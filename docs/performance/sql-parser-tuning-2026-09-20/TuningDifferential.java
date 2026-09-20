import com.landawn.abacus.query.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public class TuningDifferential {
 static Method offsets;
 static java.io.BufferedWriter out;
 static int probes;
 static void emit(String kind,String sql,Object result) throws Exception {
  out.write(kind+"\t"+Base64.getEncoder().encodeToString(sql.getBytes(StandardCharsets.UTF_8))+"\t"+Base64.getEncoder().encodeToString(String.valueOf(result).getBytes(StandardCharsets.UTF_8)));out.newLine();probes++;
 }
 static void parse(String sql) throws Exception {
  try {var p=ParsedSql.parse(sql);emit("parse",sql,p.parameterCount()+"|"+p.namedParameters()+"|"+p.parameterizedSql()+"|"+Arrays.toString((int[])offsets.invoke(p)));}
  catch(IllegalArgumentException e){emit("parse",sql,e.getClass().getName()+"|"+e.getMessage());}
 }
 public static void main(String[] args) throws Exception {
  java.util.logging.LogManager.getLogManager().reset();
  offsets=ParsedSql.class.getDeclaredMethod("positionalParameterOffsets");offsets.setAccessible(true);
  out=Files.newBufferedWriter(Path.of(args[0]),StandardCharsets.UTF_8);
  var configs=List.of(SqlParser.TokenizerConfig.builder().build(),SqlParser.TokenizerConfig.builder().withSeparator("~>").withSeparator("~>>").withSeparator("~>>>").withSeparator("π>").withSeparator("π>>").withSeparator("😀>").withSeparator(" +").withSeparator("N'").build(),SqlParser.TokenizerConfig.builder().withSeparator("order by").withSeparator("N").withSeparator("N'").withSeparator("a[").withSeparator("\t=>").build());
  Random r=new Random(965600417L);
  String[] atoms={"x","?","??",":id","#{a}","'","\"",Character.toString(96),"[","]","\\"," ","\t","\n",",",";","~>","~>>","~>>>","π>","π>>","😀>","/*x*/","--x\n","#x\n","ORDER BY","order by","N'","ARRAY[?]","''","\"\"",Character.toString(96).repeat(2),"]]","=","+","?#{a}"};
  for(int c=0;c<configs.size();c++){
   var t=SqlParser.tokenizer(configs.get(c));
   for(int i=0;i<8000;i++){
    StringBuilder b=new StringBuilder();int n=1+r.nextInt(18);for(int k=0;k<n;k++)b.append(atoms[r.nextInt(atoms.length)]);String sql=b.toString();
    var tokens=t.tokenize(sql);var result=new StringBuilder(tokens.toString());
    for(int index:new int[]{0,sql.length()/2,sql.length()})result.append('|').append(index).append(':').append(t.nextToken(sql,index)).append(':').append(t.nextTokenEndIndex(sql,index));
    for(String token:tokens.subList(0,Math.min(5,tokens.size())))result.append('|').append(token).append(':').append(t.indexOfToken(sql,token,0,true)).append(':').append(t.indexOfToken(sql,token,0,false)).append(':').append(t.indexOfToken(sql,token,sql.length()/2,false));
    emit("tokens"+c,sql,result);
   }
  }
  String[] values={"?","?, ?","?+?","?||'x'","doc ? 'k'","doc ?? 'k'","?- line '{1,0,0}'","?- ?:: line","?- CAST(? AS \"lseg\")","JSON_OBJECT('k' VALUE ? NULL ON NULL)","JSON_ARRAY(SELECT payload ? format JSON FROM t)",":a",":a:b","#{a}","#{ a, typeHandler=handler(?) }","doc ??#{key}","'?'","'it''s ?'","'a\\\\'","E'it\\'s ?'","[x?]","arr[?, ?]","COALESCE(?, 2)","? /* ? */ + ?","? -- ?\n + ?","doc ? :key","doc ? #{key}","? AS id","(doc) ? 'k'","?::text"};
  for(int i=0;i<18000;i++){
   int n=1+r.nextInt(4);List<String> parts=new ArrayList<>();for(int k=0;k<n;k++)parts.add(values[r.nextInt(values.length)]);String exp=String.join(i%2==0?", ":" + ",parts);
   if(i%3==0)exp="ARRAY["+exp+"]";else if(i%3==1)exp="arr["+exp+"][?]";
   parse((i%7==0?"/* ? */ ":"")+"SELECT "+exp+" FROM t"+(i%5==0?" WHERE id = ?":""));
  }
  out.close();System.out.println("probes="+probes);System.exit(0);
 }
}
