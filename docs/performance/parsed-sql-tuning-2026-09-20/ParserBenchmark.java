import com.landawn.abacus.query.ParsedSql;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Comparative, single-thread benchmark. Setup, SQL generation and cache clearing are not timed. */
public final class ParserBenchmark {
    private static final int ROUNDS = 9;
    private static final ThreadMXBean ALLOCATION = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().getId();
    private static volatile long sink;
    private static long sequence = 100000000;
    private static Object pool;
    private static Method clear;

    private record Case(String shape, int size, int expected, boolean stress) {}
    private record Query(String sql, String rendered, List<String> names) {}
    private record Sample(long nanos, long bytes, int operations, int min, int max, int errors) {}

    public static void main(String[] args) throws Exception {
        String version = args[0];
        int fork = Integer.parseInt(args[1]);
        boolean probeOnly = args.length > 2 && args[2].equals("probe");
        boolean steady = args.length > 2 && args[2].equals("steady");
        Field field = ParsedSql.class.getDeclaredField("pool");
        field.setAccessible(true);
        pool = field.get(null);
        clear = pool.getClass().getMethod("clear");
        clear.setAccessible(true);
        ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        System.err.println("version=" + version + " fork=" + fork + " java=" + System.getProperty("java.runtime.version")
                + " cp=" + System.getProperty("java.class.path")
                + " processors=" + Runtime.getRuntime().availableProcessors()
                + " maxHeap=" + Runtime.getRuntime().maxMemory());

        List<Case> cases = new ArrayList<>();
        cases.add(new Case("no_parameters", 0, 0, false));
        for (String style : List.of("positional", "named", "mybatis")) {
            for (int size : new int[] {2, 16}) cases.add(new Case("where_" + style, size, size, false));
            cases.add(new Case("join_" + style, 4, 4, false));
        }
        cases.add(new Case("where_positional", 128, 128, false));
        cases.add(new Case("quoted_and_comments", 2, 2, false));
        cases.add(new Case("json_operator", 1, 1, false));
        cases.add(new Case("jdbc_escape", 1, 1, false));
        cases.add(new Case("json_constructor", 1, 1, false));
        cases.add(new Case("geometric_cast", 1, 1, false));
        for (String shape : List.of("array_named", "array_mybatis", "array_positional", "bare_concat", "deep_unary")) {
            for (int size : new int[] {256, 1024, 4096, 8192}) {
                cases.add(new Case(shape, size, shape.equals("deep_unary") ? 1 : size, true));
            }
        }

        System.out.println("version,fork,shape,size,round,operations,ns_per_op,bytes_per_op,expected,min_count,max_count,errors,semantic_match");
        List<Boolean> semanticMatches = new ArrayList<>();
        for (Case c : cases) {
            Query q = query(c, sequence++);
            boolean match = false;
            try {
                ParsedSql p = ParsedSql.parse(q.sql);
                match = p.parameterCount() == c.expected && p.namedParameters().equals(q.names) && p.parameterizedSql().equals(q.rendered);
                System.err.println("check " + c.shape + "/" + c.size + " expected=" + c.expected + " actual=" + p.parameterCount()
                        + " names=" + p.namedParameters().size() + " semantic_match=" + match);
            } catch (IllegalArgumentException ex) {
                System.err.println("check " + c.shape + "/" + c.size + " error=" + ex.getMessage());
            }
            semanticMatches.add(match);
            if (version.equals("current") && !match) throw new AssertionError("Unexpected current result: " + c);
        }
        if (probeOnly) { System.exit(0); return; }

        // Warm the exact public API, including result access, before collecting any timed samples.
        for (Case c : cases) measure(c, c.stress ? 16 : 8192);
        // Optional longer stress samples: the original 2-32 operations can be shorter than 0.1 ms.
        // Calibrate each version equally to at least 10 ms per sample, keeping slow quadratic cases
        // bounded and applying the same public-API/cache-clearing protocol.
        int[] stressOperations = new int[cases.size()];
        if (steady) {
            for (int i = 0; i < cases.size(); i++) {
                Case c = cases.get(i);
                if (!c.stress) continue;
                int pilotOperations = Math.max(2, 8192 / c.size);
                Sample pilot = measure(c, pilotOperations);
                int operations = Math.max(pilotOperations, Math.min(8192,
                        (int) Math.ceil(10_000_000.0 * pilotOperations / Math.max(1, pilot.nanos))));
                stressOperations[i] = operations;
                for (int warm = 0; warm < 4; warm++) measure(c, operations);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) order.add(i);
        Random random = new Random(965600L + fork);
        for (int round = 1; round <= ROUNDS; round++) {
            Collections.shuffle(order, random);
            for (int index : order) {
                Case c = cases.get(index);
                int operations = c.stress ? (steady ? stressOperations[index] : Math.max(2, 8192 / c.size)) : 2048;
                Sample sample = measure(c, operations);
                emit(version, fork, c.shape, c.size, round, c.expected, sample, semanticMatches.get(index));
                if (version.equals("current") && (sample.errors != 0 || sample.min != c.expected || sample.max != c.expected)) {
                    throw new AssertionError("Current benchmark failed: " + c);
                }
            }
        }
        for (String style : List.of("positional", "named")) {
            cached(version, fork, style, false);
            cached(version, fork, style, true);
        }
        System.err.println("complete sink=" + sink);
        System.exit(0); // The shared cache owns background eviction threads.
    }

    private static Sample measure(Case c, int operations) throws Exception {
        long nanos = 0, allocated = 0, sum = 0;
        int min = Integer.MAX_VALUE, max = -1, errors = 0;
        int batchLimit = c.stress ? Math.max(1, Math.min(256, 8192 / c.size)) : 256;
        for (int done = 0; done < operations;) {
            int count = Math.min(batchLimit, operations - done);
            clear.invoke(pool);
            String[] inputs = new String[count];
            for (int i = 0; i < count; i++) inputs[i] = query(c, sequence++).sql;
            long startBytes = ALLOCATION.getThreadAllocatedBytes(THREAD), start = System.nanoTime();
            for (String sql : inputs) {
                try {
                    ParsedSql parsed = ParsedSql.parse(sql);
                    int actual = parsed.parameterCount();
                    min = Math.min(min, actual);
                    max = Math.max(max, actual);
                    sum += actual + parsed.parameterizedSql().length() + parsed.namedParameters().size();
                } catch (IllegalArgumentException ex) {
                    errors++;
                }
            }
            nanos += System.nanoTime() - start;
            allocated += ALLOCATION.getThreadAllocatedBytes(THREAD) - startBytes;
            done += count;
        }
        sink = sum;
        return new Sample(nanos, allocated, operations, min == Integer.MAX_VALUE ? -1 : min, max, errors);
    }

    private static void cached(String version, int fork, String style, boolean equalObject) throws Exception {
        clear.invoke(pool);
        Query q = query(new Case("where_" + style, 2, 2, false), 100000000);
        ParsedSql expected = ParsedSql.parse(q.sql);
        if (expected.parameterCount() != 2) throw new AssertionError("Cache seed");
        String[] keys = new String[1024];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = equalObject ? new String(q.sql.toCharArray()) : q.sql;
            keys[i].hashCode(); // Exclude fresh-string hashing from the cache-hit measurement.
            if (ParsedSql.parse(keys[i]) != expected) throw new AssertionError("Cache miss during hit setup");
        }
        long sum = 0;
        for (int i = 0; i < 1000000; i++) sum += ParsedSql.parse(keys[i & 1023]).parameterCount();
        sink = sum;
        for (int round = 1; round <= ROUNDS; round++) {
            sum = 0;
            int operations = 1000000;
            long startBytes = ALLOCATION.getThreadAllocatedBytes(THREAD), start = System.nanoTime();
            for (int i = 0; i < operations; i++) sum += ParsedSql.parse(keys[i & 1023]).parameterCount();
            long nanos = System.nanoTime() - start, bytes = ALLOCATION.getThreadAllocatedBytes(THREAD) - startBytes;
            sink = sum;
            emit(version, fork, "cached_" + style + (equalObject ? "_equal" : "_same"), 2, round, 2,
                    new Sample(nanos, bytes, operations, 2, 2, 0), true);
        }
    }

    private static void emit(String version, int fork, String shape, int size, int round, int expected, Sample sample, boolean match) {
        System.out.printf(Locale.ROOT, "%s,%d,%s,%d,%d,%d,%.3f,%.3f,%d,%d,%d,%d,%b%n", version, fork, shape, size, round,
                sample.operations, (double) sample.nanos / sample.operations, (double) sample.bytes / sample.operations,
                expected, sample.min, sample.max, sample.errors, match);
    }

    private static Query query(Case c, long id) {
        String alias = " result_" + id;
        String sql;
        if (c.shape.startsWith("where_")) {
            String style = c.shape.substring(6);
            List<String> predicates = new ArrayList<>();
            for (int i = 0; i < c.size; i++) predicates.add("col" + i + " = " + marker(style, i));
            sql = "SELECT id, name FROM users" + alias + " WHERE " + String.join(" AND ", predicates);
        } else if (c.shape.startsWith("join_")) {
            String style = c.shape.substring(5);
            sql = "SELECT u.id, u.name, SUM(o.total)" + alias + " FROM users u JOIN orders o ON u.id = o.user_id"
                    + " WHERE u.tenant_id = " + marker(style, 0) + " AND u.status = " + marker(style, 1)
                    + " AND o.created_at >= " + marker(style, 2) + " GROUP BY u.id, u.name HAVING SUM(o.total) > "
                    + marker(style, 3) + " ORDER BY u.id";
        } else if (c.shape.startsWith("array_")) {
            String style = c.shape.substring(6);
            List<String> markers = new ArrayList<>();
            for (int i = 0; i < c.size; i++) markers.add(marker(style, i));
            sql = "SELECT ARRAY[" + String.join(",", markers) + "] AS" + alias;
        } else {
            sql = switch (c.shape) {
                case "no_parameters" -> "SELECT id, name FROM users" + alias + " WHERE active = 1 ORDER BY id";
                case "quoted_and_comments" -> "SELECT '? :literal #{literal}' AS" + alias
                        + " FROM users /* ignored ? :fake */ WHERE id = ? AND name = ?";
                case "json_operator" -> "SELECT payload ? 'key' AS" + alias + " FROM docs WHERE id = ?";
                case "jdbc_escape" -> "SELECT payload ?? 'key' AS" + alias + " FROM docs WHERE id = ?";
                case "json_constructor" -> "SELECT JSON_OBJECT('key' VALUE ? NULL ON NULL) AS" + alias;
                case "geometric_cast" -> "SELECT ?- ?:: line AS" + alias;
                case "bare_concat" -> "SELECT " + "?||".repeat(c.size - 1) + "? AS" + alias;
                case "deep_unary" -> "SELECT ?- " + "(".repeat(c.size) + "?::line" + ")".repeat(c.size) + " AS" + alias;
                default -> throw new IllegalArgumentException(c.shape);
            };
        }
        boolean named = c.shape.endsWith("_named"), mybatis = c.shape.endsWith("_mybatis");
        List<String> names = new ArrayList<>();
        if (named || mybatis) for (int i = 0; i < c.expected; i++) names.add("p" + i);
        String rendered = named ? sql.replaceAll(":p[0-9]+", "?") : mybatis ? sql.replaceAll("#\\{p[0-9]+, jdbcType=VARCHAR\\}", "?") : sql;
        if (c.shape.equals("quoted_and_comments")) rendered = sql.replace(" /* ignored ? :fake */", "");
        return new Query(sql, rendered, names);
    }

    private static String marker(String style, int index) {
        return switch (style) {
            case "positional" -> "?";
            case "named" -> ":p" + index;
            case "mybatis" -> "#{p" + index + ", jdbcType=VARCHAR}";
            default -> throw new IllegalArgumentException(style);
        };
    }
}
