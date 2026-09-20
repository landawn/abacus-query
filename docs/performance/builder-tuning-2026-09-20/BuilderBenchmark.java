import com.landawn.abacus.query.*;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.*;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Standalone end-to-end builder timing/allocation benchmark; every result escapes. */
public final class BuilderBenchmark {
    private static final ThreadMXBean ALLOC = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().getId();
    private static volatile Object objectSink;
    private static volatile long sink;
    private record Case(String name, Dsl dsl, String shape) {}
    private record Sample(long ns, long bytes, int operations, long value) {}
    public static class Person {
        private int id = 7;
        private String firstName = "O'Brien";
        private String lastName = "Example";
        public int getId() { return id; }
        public void setId(int value) { id = value; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String value) { firstName = value; }
        public String getLastName() { return lastName; }
        public void setLastName(String value) { lastName = value; }
    }
    private static final Person PERSON = new Person();
    private static final Map<String, Object> VALUES = values();
    private static Map<String, Object> values() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", 7); result.put("firstName", "O'Brien"); result.put("lastName", "Example");
        return result;
    }
    private static Condition many(int count) {
        List<Condition> values = new ArrayList<>();
        for (int i = 0; i < count; i++) values.add(Filters.eq("columnName" + i, i));
        return Filters.and(values);
    }
    private static final Condition EQ = Filters.eq("id", 7);
    private static final Condition MANY16 = many(16), MANY128 = many(128);
    private static final Condition BETWEEN = Filters.between("u.createdAt", 1, 99);
    private static final Condition IN16 = Filters.in("id", java.util.stream.IntStream.range(0, 16).boxed().toList());
    private static final Condition IN256 = Filters.in("id", java.util.stream.IntStream.range(0, 256).boxed().toList());
    private static final Condition IN17 = Filters.in("id", java.util.stream.IntStream.range(0, 17).boxed().toList());
    private static final Condition IN1024 = Filters.in("id", java.util.stream.IntStream.range(0, 1024).boxed().toList());
    private static final Condition INEXPR = inExpression();
    private static Condition inExpression() {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < 255; i++) values.add(i);
        values.add(SqlExpression.of("42"));
        return Filters.in("id", values);
    }
    private static final Condition REPEATED = Filters.and(Collections.nCopies(128, EQ));
    private static final Condition LITERALS = Filters.and(Filters.eq("bigValue", Long.MIN_VALUE), Filters.eq("boolValue", true), Filters.eq("realValue", 1.25d));
    private static final Condition ROW_IN = Filters.in(List.of("id", "firstName"), List.of(List.of(1, "A"), List.of(2, "B")));
    private static final Condition NESTED = Filters.and(Filters.or(EQ, Filters.eq("id", 8)), Filters.not(Filters.isNull("firstName")));
    private static final Condition EMPTY = Filters.and(List.of());
    private static final Condition RAW_SUB = Filters.in("id", new SubQuery("SELECT id FROM archive WHERE status = 'active'"));
    private static final Condition STRUCTURED_SUB = Filters.in("id", new SubQuery("archive", "id", Filters.where(Filters.eq("status", "active"))));
    private static final List<Map<String, Object>> ROWS4 = Collections.nCopies(4, VALUES), ROWS64 = Collections.nCopies(64, VALUES);
    private static final List<Case> CASES = cases();
    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        Dsl[] dsls = { Dsl.PSC, Dsl.NSC, Dsl.MSC, Dsl.SCSB };
        String[] names = { "positional", "named", "mybatis", "raw" };
        String[] shapes = { "select_star", "select_columns", "select_entity", "where_eq", "where_16", "where_128", "between", "in_16", "in_17", "in_256", "in_1024", "in_expression", "repeated_names", "mixed_literals", "batch_beans", "row_in", "nested", "condition_only", "insert_values", "insert_placeholders", "insert_entity", "batch_4", "batch_64", "update_map", "update_chain_16", "delete", "join_on", "join_using", "group_order", "pagination", "union_raw", "union_builder", "derived", "subquery_raw", "subquery_structured", "raw_expression", "select_expressions", "late_modifier", "retry_rollback" };
        for (int i = 0; i < dsls.length; i++) for (String shape : shapes) cases.add(new Case(names[i] + "_" + shape, dsls[i], shape));
        for (com.landawn.abacus.util.NamingPolicy naming : com.landawn.abacus.util.NamingPolicy.values()) {
            if (naming == com.landawn.abacus.util.NamingPolicy.SNAKE_CASE) continue;
            for (int i = 0; i < dsls.length; i++) {
                cases.add(new Case(names[i] + "_naming_" + naming, Dsl.forDialect(dsls[i].sqlDialect().toBuilder().namingPolicy(naming).build()), "where_16"));
            }
        }
        for (String product : new String[] { "PostgreSQL", "MySQL", "Microsoft SQL Server", "Oracle", "DB2" }) {
            for (int i = 0; i < 2; i++) cases.add(new Case(names[i] + "_dialect_" + product.replace(' ', '_'),
                    Dsl.forDialect(dsls[i].sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of(product)).build()), "dialect_page"));
        }
        Dsl custom = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("@[").append(name).append(']')).build());
        cases.add(new Case("custom_handler_where", custom, "where_16"));
        cases.add(new Case("custom_handler_union", custom, "union_builder"));
        return cases;
    }
    private static SP result(Case c) {
        Dsl d = c.dsl;
        return switch (c.shape) {
            case "select_star" -> d.select("*").from("users").build();
            case "select_columns" -> d.select("id", "firstName", "lastName").from("users").build();
            case "select_entity" -> d.select(Person.class).from(Person.class, "p").build();
            case "where_eq" -> d.select("id", "firstName").from("users").where(EQ).build();
            case "where_16" -> d.select("id").from("users").where(MANY16).build();
            case "where_128" -> d.select("id").from("users").where(MANY128).build();
            case "between" -> d.select("id").from("users u").where(BETWEEN).build();
            case "in_16" -> d.select("id").from("users").where(IN16).build();
            case "in_17" -> d.select("id").from("users").where(IN17).build();
            case "in_1024" -> d.select("id").from("users").where(IN1024).build();
            case "in_expression" -> d.select("id").from("users").where(INEXPR).build();
            case "repeated_names" -> d.select("id").from("users").where(REPEATED).build();
            case "mixed_literals" -> d.select("id").from("users").where(LITERALS).build();
            case "batch_beans" -> d.batchInsert(Collections.nCopies(16, PERSON)).into("users").build();
            case "in_256" -> d.select("id").from("users").where(IN256).build();
            case "row_in" -> d.select("id").from("users").where(ROW_IN).build();
            case "nested" -> d.select("id").from("users").where(NESTED).build();
            case "empty" -> d.select("id").from("users").where(EMPTY).build();
            case "condition_only" -> d.renderCondition(MANY16).build();
            case "insert_values" -> d.insert(VALUES).into("users").build();
            case "insert_placeholders" -> d.insert("id", "firstName", "lastName").into("users").build();
            case "insert_entity" -> d.insert(PERSON).into("users").build();
            case "batch_4" -> d.batchInsert(ROWS4).into("users").build();
            case "batch_64" -> d.batchInsert(ROWS64).into("users").build();
            case "update_map" -> d.update("users").set(VALUES).where(EQ).build();
            case "update_chain_16" -> {
                SqlBuilder b = d.update("users");
                for (int i = 0; i < 16; i++) b.set("columnName" + i, i);
                yield b.where(EQ).build();
            }
            case "delete" -> d.deleteFrom("users").where(EQ).build();
            case "join_on" -> d.select("u.id").from("users u").join("archive a").on("u.id = a.id").where(EQ).build();
            case "join_using" -> d.select("id").from("users").join("archive").using("id").where(EQ).build();
            case "group_order" -> d.select("firstName", "COUNT(*)").from("users").where(EQ).groupBy("firstName").having("COUNT(*) > 1").orderBy("firstName").build();
            case "dialect_page" -> d.select("id").from("users").where(EQ).orderBy("id").limit(5, 20).build();
            case "pagination" -> d.select("id").from("users").where(EQ).orderBy("id").limit(20).offset(5).build();
            case "union_raw" -> d.select("id").from("users").union("SELECT id FROM archive").build();
            case "union_builder" -> d.select("id").from("users").where(EQ).union(d.select("id").from("archive").where(EQ)).build();
            case "derived" -> d.select("x.id").from(d.select("id").from("users").where(EQ), "x").where(EQ).build();
            case "subquery_raw" -> d.select("id").from("users").where(RAW_SUB).build();
            case "subquery_structured" -> d.select("id").from("users").where(STRUCTURED_SUB).build();
            case "raw_expression" -> d.select("id").from("users").where("firstName = 'Some Name' AND scoreValue > 1").build();
            case "select_expressions" -> d.select("COALESCE(firstName, 'N.A') AS displayName", "COUNT(*) AS totalCount").from("users").build();
            case "late_modifier" -> d.select("id").from("users").distinct().where(EQ).build();
            case "retry_rollback" -> {
                SqlBuilder b = d.update("users").set("firstName", "initial");
                Map<String, Object> bad = new LinkedHashMap<>(); bad.put("lastName", "new"); bad.put("bad--column", 2);
                try { b.set(bad); throw new AssertionError("unsafe column accepted"); }
                catch (IllegalArgumentException expected) { }
                yield b.set("lastName", "ok").where(EQ).build();
            }
            default -> throw new AssertionError(c.shape);
        };
    }
    private static Sample sample(Case c, int count) {
        long bytes = ALLOC.getThreadAllocatedBytes(THREAD), start = System.nanoTime(), sum = 0;
        for (int i = 0; i < count; i++) { SP r = result(c); objectSink = r; sum += r.query().length() + r.parameters().size(); }
        long ns = System.nanoTime() - start, allocated = ALLOC.getThreadAllocatedBytes(THREAD) - bytes;
        sink = sum; return new Sample(ns, allocated, count, sum);
    }
    public static void main(String[] args) throws Exception {
        java.util.logging.LogManager.getLogManager().reset(); ALLOC.setThreadAllocatedMemoryEnabled(true);
        String version = args[0]; int fork = Integer.parseInt(args[1]); Path reference = Path.of(args[2]); String mode = args.length > 3 ? args[3] : "steady";
        List<String> signatures = new ArrayList<>();
        for (Case c : CASES) { SP r = result(c); signatures.add(c.name + "\t" + Base64.getEncoder().encodeToString((r.query() + "\n" + r.parameters()).getBytes(StandardCharsets.UTF_8))); }
        if (mode.equals("probe")) { Files.write(reference, signatures, StandardCharsets.UTF_8); System.out.println("cases=" + CASES.size()); System.exit(0); }
        List<String> expected = Files.readAllLines(reference); boolean[] equivalent = new boolean[CASES.size()];
        for (int i = 0; i < CASES.size(); i++) { equivalent[i] = signatures.get(i).equals(expected.get(i)); if ((version.equals("before") || version.equals("current")) && !equivalent[i]) throw new AssertionError("Behavior changed: " + CASES.get(i).name); }
        int[] operations = new int[CASES.size()];
        for (int i = 0; i < CASES.size(); i++) {
            Case c = CASES.get(i); int n = 1; Sample s = sample(c, n);
            while (s.ns < 2_000_000 && n < 65536) { n *= 2; s = sample(c, n); }
            long warmEnd = System.nanoTime() + (mode.equals("steady") ? 250_000_000L : 20_000_000L);
            do { s = sample(c, n); } while (System.nanoTime() < warmEnd);
            // A single paused warmup sample can otherwise leave only a handful of timed
            // operations. Calibrate from medians after warmup, with a representative batch.
            n = Math.max(64, n);
            long[] calibration = new long[5];
            while (true) {
                for (int j = 0; j < calibration.length; j++) calibration[j] = sample(c, n).ns;
                Arrays.sort(calibration);
                if (calibration[2] >= 2_000_000L || n >= 65536) break;
                n *= 2;
            }
            operations[i] = Math.max(64, Math.min(262144, (int) Math.ceil((mode.equals("steady") ? 20_000_000.0 : 5_000_000.0) * n / calibration[2])));
        }
        System.out.println("version,fork,case,round,operations,ns_per_op,bytes_per_op,equivalent");
        List<Integer> order = new ArrayList<>(); for (int i = 0; i < CASES.size(); i++) order.add(i); Random random = new Random(965604986L + fork);
        for (int round = 1; round <= 9; round++) { Collections.shuffle(order, random); for (int i : order) { Sample s = sample(CASES.get(i), operations[i]); System.out.printf(Locale.ROOT, "%s,%d,%s,%d,%d,%.3f,%.3f,%b%n", version, fork, CASES.get(i).name, round, s.operations, (double) s.ns / s.operations, (double) s.bytes / s.operations, equivalent[i]); } }
        System.err.println("complete cases=" + CASES.size() + " sink=" + sink); System.exit(0);
    }
}
