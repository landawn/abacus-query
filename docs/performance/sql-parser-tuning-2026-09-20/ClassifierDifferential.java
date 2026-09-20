import com.landawn.abacus.query.SqlParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class ClassifierDifferential {
 public static void main(String[] args)throws Exception{
  java.util.logging.LogManager.getLogManager().reset();
  var r=new Random(49869656L);int cases=0;
  String[] prefixes={"SELECT ","INSERT INTO t VALUES ","UPDATE t SET x = ","DELETE FROM t WHERE x = ","WITH c AS (SELECT 1) SELECT ","WITH delete AS (SELECT 1) (SELECT ","--x\nSELECT ","-- x\nSELECT ","/* c */ SELECT ","SELECT * FROM #t WHERE ","SELECT * FROM t # comment\nWHERE ","WITH c AS (DELETE FROM t RETURNING *) SELECT ","(((SELECT "};
  String[] atoms={"1","'x'","'a\\'; DELETE FROM t; -- '" ,"'a\\'","'it''s'","[a]]b]","[a\\]","\"a\\\"b\"",":into",":call",":select.id","#{insert}","#{call, option='--'}","'--x'","'/*'","/* c */","/*/c*/","/* unfinished","/*! SELECT 1 */","--x\n","-- x\n","# x\n","FROM #t","FROM a, #t", "ON CONFLICT (id) DO UPDATE SET id=1","INTO OUTFILE 'f'","; GRANT x", "; SELECT 1", "; CALL p()", "; { ? = call f()}","AS", "(", ")","?",",","FROM","INTO","[FROM]"," :字段","E'\\\\'","\"INTO\"","--\u0001","--\u007f","'\n'","\t","RECURSIVE"};
  try(var out=Files.newBufferedWriter(Path.of(args[0]),StandardCharsets.UTF_8)){
   for(int i=0;i<24000;i++){
    StringBuilder b=new StringBuilder(prefixes[i%prefixes.length]);int n=1+r.nextInt(7);for(int j=0;j<n;j++)b.append(atoms[r.nextInt(atoms.length)]).append(' ');String sql=b.toString();
    String v=SqlParser.isSelectQuery(sql)+"|"+SqlParser.isReadOnlyQuery(sql)+"|"+SqlParser.isReadOrInsertQuery(sql)+"|"+SqlParser.isInsertQuery(sql)+"|"+SqlParser.isUpdateQuery(sql)+"|"+SqlParser.isDeleteQuery(sql)+"|"+SqlParser.isInsertOrReplaceQuery(sql);
    out.write(Base64.getEncoder().encodeToString(sql.getBytes(StandardCharsets.UTF_8))+"\t"+v);out.newLine();cases++;
   }
  }
  System.out.println("cases="+cases);System.exit(0);
 }
}
