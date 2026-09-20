import com.landawn.abacus.query.SqlParser;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class SqlParserBenchmark {
 private static final ThreadMXBean ALLOC=(ThreadMXBean)ManagementFactory.getThreadMXBean();
 private static final long THREAD=Thread.currentThread().getId();
 private static volatile Object objectSink;
 private static volatile long sink;
 private static final List<SqlParser.Tokenizer> TOKENIZERS=List.of(SqlParser.tokenizer(),SqlParser.tokenizer(SqlParser.TokenizerConfig.builder().withSeparator(" +").withSeparator("π>").withSeparator("π>>").withSeparator("😀>").withSeparator("order by").build()));
 private record Case(String name,int op,String sql,String target,int from,boolean sensitive,int config){}
 private record Sample(long ns,long bytes,int operations,long value){}
 private static final List<Case> CASES=cases();
 private static List<Case> cases(){
  List<Case> c=new ArrayList<>();
  String small="SELECT id, name FROM users WHERE id = ? AND active = ? ORDER BY name";
  String large="SELECT id FROM users WHERE "+String.join(" AND ",Collections.nCopies(128,"column_name = ?"));
  String quote="'"+"long quoted text ".repeat(512)+"''escaped'";
  String comments="SELECT "+"x /* comment ? :ignored */ + ".repeat(16)+"y FROM t -- tail\n WHERE id = ?";
  String hash="SELECT * FROM "+String.join(", ",Collections.nCopies(12,"#temp"))+" WHERE id = ?";
  String rows="INSERT INTO t VALUES "+"(1) #tag\n, ".repeat(12)+"(2)";
  String cte="WITH c AS (SELECT id FROM t WHERE n = :n), d AS (SELECT id FROM c) SELECT * FROM d";
  String custom="SELECT x +value π>>y 😀>z FROM t order by x";
  for(String[] item:new String[][]{{"tiny","SELECT 1"},{"simple",small},{"large",large},{"long_quote","SELECT "+quote+" FROM t"},{"comments",comments},{"keep_comments","-- Keep comments\n"+comments},{"mybatis","SELECT "+String.join(",",Collections.nCopies(16,"#{id, jdbcType=INTEGER}"))+" FROM t"},{"hash_identifiers",hash},{"hash_rows",rows},{"unicode","SELECT 名称, \"a.b\", [c]]d], '汉字' FROM 表 WHERE 键 = :键"},{"backslashes","SELECT 'a\\\\b', 'it\\'s', [path\\] FROM t"},{"array","SELECT ARRAY["+String.join(",",Collections.nCopies(1024,"?"))+"]"}})c.add(new Case("tokenize_"+item[0],0,item[1],"",0,false,0));
  c.add(new Case("tokenize_custom",0,custom,"",0,false,1));
  for(String[] item:new String[][]{{"early",small,"SELECT"},{"late",large+" ORDER BY id","ORDER BY"},{"missing",large,"HAVING"},{"composite",small,"ORDER BY"},{"composite_comments","SELECT x ORDER /*x*/ BY y","ORDER BY"},{"near_matches","SELECT "+"ORDER x ".repeat(256)+"ORDER BY y","ORDER BY"},{"quoted","SELECT "+quote+" FROM t",quote},{"hash",hash,"WHERE"},{"from_inside_quote","SELECT 'WHERE inside' FROM t WHERE x = 1","WHERE"}})c.add(new Case("index_"+item[0],1,item[1],item[2],item[0].equals("from_inside_quote")?10:0,false,0));
  c.add(new Case("index_sensitive",1,small,"WHERE",0,true,0));
  c.add(new Case("index_whitespace",1,small," ",0,false,0));
  c.add(new Case("index_custom_separator",1,custom," +",0,false,1));
  c.add(new Case("index_custom_composite",1,"SELECT x ORDER BY y order by z","ORDER BY",0,false,1));
  for(int op:new int[]{2,3}){
   String prefix=op==2?"next_":"end_";
   for(String[] item:new String[][]{{"word",small},{"quote",quote+" FROM t"},{"trivia"," /* comment */ -- line\n WHERE x = 1"},{"empty"," /* done */ -- tail"},{"hash","FROM #temp WHERE id = ?"}})c.add(new Case(prefix+item[0],op,item[1],"",item[0].equals("hash")?4:0,false,0));
   c.add(new Case(prefix+"custom",op," +value","",0,false,1));
  }
  for(int op:new int[]{4,5})for(String[] item:new String[][]{{"simple",small},{"large",large},{"comments",comments},{"hash",hash}})c.add(new Case((op==4?"walk_bounds_":"walk_tokens_")+item[0],op,item[1],"",0,false,0));
  for(String[] item:new String[][]{{"simple",small},{"cte",cte},{"quotes","SELECT 'x', [a], :b FROM t"},{"ambiguous","SELECT 'a\\'; DELETE FROM t; -- x'"},{"parenthesized","(((SELECT id FROM t)))"}})c.add(new Case("select_"+item[0],6,item[1],"",0,false,0));
  for(int op:new int[]{7,8})for(String[] item:new String[][]{{"simple",small},{"cte",cte},{"large",large},{"literal","SELECT 'update;delete', \"into\", :call FROM t"},{"quoted_large","SELECT "+quote+" FROM t"},{"comments",comments},{"hash",hash},{"multiple","SELECT 1; SELECT 2"},{"mutation","SELECT 1; DELETE FROM t"},{"upsert","INSERT INTO t VALUES (?) ON CONFLICT (id) DO UPDATE SET id = ?"},{"insert","INSERT INTO t VALUES (?)"},{"executable_comment","SELECT 1 /*! DELETE FROM t */"},{"mysql_dash","SELECT 1--x; DELETE FROM t"}})c.add(new Case((op==7?"read_":"read_insert_")+item[0],op,item[1],"",0,false,0));
  c.add(new Case("classify_insert",9,"INSERT INTO t VALUES (?)","",0,false,0));
  c.add(new Case("classify_update",10,"UPDATE t SET id = ?","",0,false,0));
  c.add(new Case("classify_delete",11,"DELETE FROM t WHERE id = ?","",0,false,0));
  c.add(new Case("classify_replace",12,"/* c */ INSERT OR REPLACE INTO t VALUES (?)","",0,false,0));
  c.add(new Case("config_default",13,"","",0,false,0));
  c.add(new Case("config_custom",13,"","",0,false,1));
  return c;
 }
 private static Object result(Case c,String sql){
  var t=TOKENIZERS.get(c.config);
  return switch(c.op){
   case 0->t.tokenize(sql);case 1->t.indexOfToken(sql,c.target,c.from,c.sensitive);case 2->t.nextToken(sql,c.from);case 3->t.nextTokenEndIndex(sql,c.from);
   case 4,5->{long sum=0;for(int at=0;at<sql.length();){if(c.op==5)sum+=t.nextToken(sql,at).length();int end=t.nextTokenEndIndex(sql,at);if(end<=at)throw new AssertionError("No advance");sum+=end;at=end;}yield sum;}
   case 6->SqlParser.isSelectQuery(sql);case 7->SqlParser.isReadOnlyQuery(sql);case 8->SqlParser.isReadOrInsertQuery(sql);case 9->SqlParser.isInsertQuery(sql);case 10->SqlParser.isUpdateQuery(sql);case 11->SqlParser.isDeleteQuery(sql);case 12->SqlParser.isInsertOrReplaceQuery(sql);
   case 13->c.config==0?SqlParser.TokenizerConfig.builder().build():SqlParser.TokenizerConfig.builder().withSeparator(" +").withSeparator("π>>").withSeparator("order by").build();
   default->throw new AssertionError();
  };
 }
 private static long invoke(Case c,String sql){
  // Primitive-return APIs are measured without boxing; reference results escape to a volatile sink.
  var t=TOKENIZERS.get(c.config);
  switch(c.op){
   case 0:{var x=t.tokenize(sql);objectSink=x;return x.size();}
   case 1:return t.indexOfToken(sql,c.target,c.from,c.sensitive);
   case 2:{var x=t.nextToken(sql,c.from);objectSink=x;return x.length();}
   case 3:return t.nextTokenEndIndex(sql,c.from);
   case 4:case 5:{long sum=0;for(int at=0;at<sql.length();){if(c.op==5){var x=t.nextToken(sql,at);objectSink=x;sum+=x.length();}int end=t.nextTokenEndIndex(sql,at);if(end<=at)throw new AssertionError("No advance");sum+=end;at=end;}return sum;}
   case 6:return SqlParser.isSelectQuery(sql)?1:0;case 7:return SqlParser.isReadOnlyQuery(sql)?1:0;case 8:return SqlParser.isReadOrInsertQuery(sql)?1:0;
   case 9:return SqlParser.isInsertQuery(sql)?1:0;case 10:return SqlParser.isUpdateQuery(sql)?1:0;case 11:return SqlParser.isDeleteQuery(sql)?1:0;case 12:return SqlParser.isInsertOrReplaceQuery(sql)?1:0;
   case 13:{var x=c.config==0?SqlParser.TokenizerConfig.builder().build():SqlParser.TokenizerConfig.builder().withSeparator(" +").withSeparator("π>>").withSeparator("order by").build();objectSink=x;return x.separators().size();}
   default:throw new AssertionError();
  }
 }
 private static String signature(Case c){Object x=result(c,c.sql);if(x instanceof SqlParser.TokenizerConfig cfg)return new TreeSet<>(cfg.separators()).toString();return String.valueOf(x);}
 private static Sample sample(Case c,String[] inputs,int count){long b=ALLOC.getThreadAllocatedBytes(THREAD),start=System.nanoTime(),sum=0;for(int i=0;i<count;i++)sum+=invoke(c,inputs[i&31]);long ns=System.nanoTime()-start,bytes=ALLOC.getThreadAllocatedBytes(THREAD)-b;sink=sum;return new Sample(ns,bytes,count,sum);}
 public static void main(String[] args)throws Exception{
  java.util.logging.LogManager.getLogManager().reset();ALLOC.setThreadAllocatedMemoryEnabled(true);
  String version=args[0];int fork=Integer.parseInt(args[1]);Path reference=Path.of(args[2]);boolean probe=args.length>3&&args[3].equals("probe");boolean steady=args.length>3&&args[3].equals("steady");
  List<String> signatures=new ArrayList<>();for(Case c:CASES)signatures.add(c.name+"\t"+Base64.getEncoder().encodeToString(signature(c).getBytes(StandardCharsets.UTF_8)));
  if(probe){Files.write(reference,signatures,StandardCharsets.UTF_8);System.out.println("cases="+CASES.size());System.exit(0);}
  List<String> expected=Files.readAllLines(reference,StandardCharsets.UTF_8);boolean[] equivalent=new boolean[CASES.size()];
  for(int i=0;i<CASES.size();i++){equivalent[i]=signatures.get(i).equals(expected.get(i));if((version.equals("before")||version.equals("current"))&&!equivalent[i])throw new AssertionError("Behavior changed: "+CASES.get(i).name+"\n"+signatures.get(i)+"\n"+expected.get(i));}
  String[][] inputs=new String[CASES.size()][32];int[] operations=new int[CASES.size()];
  for(int i=0;i<CASES.size();i++){Case c=CASES.get(i);for(int k=0;k<32;k++)inputs[i][k]=new String(c.sql.toCharArray());int n=1;Sample s=sample(c,inputs[i],n);while(s.ns<2_000_000&&n<65536){n*=2;s=sample(c,inputs[i],n);}if(steady){long warmEnd=System.nanoTime()+250_000_000L;do{s=sample(c,inputs[i],n);}while(System.nanoTime()<warmEnd);}else{for(int w=0;w<4;w++)s=sample(c,inputs[i],n);}operations[i]=Math.max(1,Math.min(262144,(int)Math.ceil((steady?20_000_000.0:5_000_000.0)*n/Math.max(1,s.ns))));}
  System.out.println("version,fork,case,round,operations,ns_per_op,bytes_per_op,equivalent");
  List<Integer> order=new ArrayList<>();for(int i=0;i<CASES.size();i++)order.add(i);Random random=new Random(965604986L+fork);
  for(int round=1;round<=9;round++){Collections.shuffle(order,random);for(int i:order){Sample s=sample(CASES.get(i),inputs[i],operations[i]);System.out.printf(Locale.ROOT,"%s,%d,%s,%d,%d,%.3f,%.3f,%b%n",version,fork,CASES.get(i).name,round,s.operations,(double)s.ns/s.operations,(double)s.bytes/s.operations,equivalent[i]);}}
  System.err.println("complete cases="+CASES.size()+" sink="+sink);System.exit(0);
 }
}
