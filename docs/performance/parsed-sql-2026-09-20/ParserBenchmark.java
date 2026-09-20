import com.landawn.abacus.query.ParsedSql;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.util.*;

/** Isolated local benchmark; source generation and cache clearing are outside timing. */
public class ParserBenchmark {
    static volatile long sink;
    static long sequence;
    static Object pool;
    static Method clear;
    static final ThreadMXBean allocation = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    static final long thread = Thread.currentThread().threadId();
    static final int SAMPLES = 9;
    record Case(String shape, int size, int expected, int batch) {}
    record Sample(double nanos, double bytes, int min, int max, int errors) {}
    public static void main(String[] args) throws Exception {
        String version = args[0]; int fork = Integer.parseInt(args[1]);
        Field f = ParsedSql.class.getDeclaredField("pool"); f.setAccessible(true); pool = f.get(null);
        clear = pool.getClass().getMethod("clear"); clear.setAccessible(true);
        allocation.setThreadAllocatedMemoryEnabled(true);
        System.err.println("version=" + version + " fork=" + fork + " java=" + System.getProperty("java.version")
            + " source=" + ParsedSql.class.getProtectionDomain().getCodeSource().getLocation());
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("short_no_parameters",1,0,1000));
        cases.add(new Case("short_named",2,2,1000));
        cases.add(new Case("short_positional",2,2,1000));
        cases.add(new Case("where_positional",16,16,250));
        cases.add(new Case("json_null",1,0,1000));
        cases.add(new Case("json_format_named",1,1,1000));
        for (String shape: List.of("bare_concat","array_concat","array_plus","unary_cast","deep_unary"))
            for (int size: new int[]{2000,4000,8000,16000,32000})
                cases.add(new Case(shape,size,shape.equals("deep_unary")?1:size,Math.max(1,16000/size)));
        for (Case c: cases) {
            int parses = c.size < 100 ? 10000 : 30;
            for(int done=0;done<parses;) {int n=Math.min(c.batch,parses-done);run(c,n);done+=n;}
        }
        List<List<Sample>> results = new ArrayList<>(); List<Integer> order=new ArrayList<>();
        for(int i=0;i<cases.size();i++){results.add(new ArrayList<>());order.add(i);}
        Random random = new Random(91473+fork);
        for(int s=0;s<SAMPLES;s++) {
            Collections.shuffle(order,random);
            for(int i:order) results.get(i).add(run(cases.get(i),cases.get(i).batch));
        }
        System.out.println("version,fork,shape,size,batch,samples,median_ns,median_bytes,expected,min_count,max_count,errors");
        for(int i=0;i<cases.size();i++) {
            Case c=cases.get(i);List<Sample> rs=results.get(i);
            double[] ns=rs.stream().mapToDouble(Sample::nanos).sorted().toArray();
            double[] bytes=rs.stream().mapToDouble(Sample::bytes).sorted().toArray();
            System.out.printf(Locale.ROOT,"%s,%d,%s,%d,%d,%d,%.3f,%.3f,%d,%d,%d,%d%n",version,fork,c.shape,c.size,c.batch,SAMPLES,
                ns[SAMPLES/2],bytes[SAMPLES/2],c.expected,rs.stream().mapToInt(Sample::min).min().orElse(-1),
                rs.stream().mapToInt(Sample::max).max().orElse(-1),rs.stream().mapToInt(Sample::errors).sum());
        }
        cached(version,fork); System.err.println("complete sink="+sink);System.exit(0);
    }
    static Sample run(Case c,int batch) throws Exception {
        clear.invoke(pool);String[] queries=new String[batch];
        for(int i=0;i<batch;i++) queries[i]=sql(c,sequence++);
        int min=Integer.MAX_VALUE,max=-1,errors=0;long sum=0;
        long bytes=allocation.getThreadAllocatedBytes(thread),start=System.nanoTime();
        for(String sql:queries) {
            try{ParsedSql p=ParsedSql.parse(sql);int count=p.parameterCount();min=Math.min(min,count);max=Math.max(max,count);sum+=count+p.parameterizedSql().length();}
            catch(IllegalArgumentException ex){errors++;}
        }
        long elapsed=System.nanoTime()-start;bytes=allocation.getThreadAllocatedBytes(thread)-bytes;sink=sum;
        return new Sample((double)elapsed/batch,(double)bytes/batch,min==Integer.MAX_VALUE?-1:min,max,errors);
    }
    static String sql(Case c,long id) {
        String a=" AS result_"+id;
        return switch(c.shape) {
            case "short_no_parameters" -> "SELECT id, name FROM users u"+id+" WHERE active = 1 ORDER BY id";
            case "short_named" -> "SELECT id, name FROM users u"+id+" WHERE id = :id AND status = :status";
            case "short_positional" -> "SELECT id, name FROM users u"+id+" WHERE id = ? AND status = ?";
            case "where_positional" -> "SELECT id FROM users u"+id+" WHERE "+String.join(" AND ",Collections.nCopies(c.size,"id = ?"));
            case "json_null" -> "SELECT payload ? NULL"+a+" FROM t";
            case "json_format_named" -> "SELECT payload ? format('%s', :key)"+a+" FROM t";
            case "bare_concat" -> "SELECT "+"?||".repeat(c.size-1)+"?"+a;
            case "array_concat" -> "SELECT ARRAY["+"?||".repeat(c.size-1)+"?]"+a;
            case "array_plus" -> "SELECT ARRAY["+"?+".repeat(c.size-1)+"?]"+a;
            case "unary_cast" -> "SELECT ARRAY["+"?- CAST(? AS line),".repeat(c.size-1)+"?- CAST(? AS line)]"+a;
            case "deep_unary" -> "SELECT ?- "+"(".repeat(c.size)+"?::line"+")".repeat(c.size)+a;
            default -> throw new IllegalArgumentException(c.shape);
        };
    }
    static void cached(String version,int fork) throws Exception {
        clear.invoke(pool);String sql="SELECT id FROM users WHERE id = ? AND status = ?";
        if (ParsedSql.parse(sql).parameterCount() != 2) throw new AssertionError("Cached-query parameter count");
        for(int i=0;i<100000;i++)sink=ParsedSql.parse(sql).parameterCount();
        int batch=500000;double[] ns=new double[SAMPLES],bytes=new double[SAMPLES];
        for(int s=0;s<SAMPLES;s++) {
            long sum=0,b=allocation.getThreadAllocatedBytes(thread),start=System.nanoTime();
            for(int i=0;i<batch;i++)sum+=ParsedSql.parse(sql).parameterCount();
            ns[s]=(double)(System.nanoTime()-start)/batch;bytes[s]=(double)(allocation.getThreadAllocatedBytes(thread)-b)/batch;sink=sum;
        }
        Arrays.sort(ns);Arrays.sort(bytes);
        System.out.printf(Locale.ROOT,"%s,%d,cached_short,2,%d,%d,%.3f,%.3f,2,2,2,0%n",version,fork,batch,SAMPLES,ns[SAMPLES/2],bytes[SAMPLES/2]);
    }
}
