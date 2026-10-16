package com.landawn.abacus.query;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Targeted follow-up allocation/scaling diagnostics; not a JMH benchmark. */
public final class FollowupProbe {
    private static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().threadId();
    private static volatile Object objectSink;
    private static volatile long valueSink;

    @FunctionalInterface
    private interface Action { long run(); }
    private record Case(String name, int scale, String sql, Action action) { }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        java.util.logging.LogManager.getLogManager().reset();
        ALLOC.setThreadAllocatedMemoryEnabled(true);
        String fork = args.length == 0 ? "1" : args[0];
        boolean allocationReference = args.length > 1 && args[1].equals("allocation-reference");
        List<Case> cases = cases();
        if (allocationReference) {
            cases.removeIf(c -> !c.name.startsWith("index_") && !c.name.startsWith("matcher_"));
        }
        // Exercise every measured path before per-case warmup, without repeatedly running slow cases.
        for (Case c : cases) valueSink = c.action.run();
        System.out.println("fork,case,scale,sql_chars,round,operations,ns_per_op,bytes_per_op,result");
        for (Case c : cases) {
            long expected = c.action.run();
            int batch = 1;
            long warmStart = System.nanoTime();
            do {
                long start = System.nanoTime();
                run(c, batch, expected);
                long elapsed = System.nanoTime() - start;
                if (elapsed < 5_000_000 && batch < 32768) batch *= 2;
            } while (System.nanoTime() - warmStart < 300_000_000);
            for (int round = 1; round <= 7; round++) {
                long allocated = ALLOC.getThreadAllocatedBytes(THREAD);
                long start = System.nanoTime();
                run(c, batch, expected);
                long elapsed = System.nanoTime() - start;
                long bytes = ALLOC.getThreadAllocatedBytes(THREAD) - allocated;
                System.out.printf("%s,%s,%d,%d,%d,%d,%.3f,%.3f,%d%n", fork, c.name, c.scale,
                        c.sql.length(), round, batch, elapsed / (double) batch, bytes / (double) batch, expected);
            }
        }
        System.exit(0); // ParsedSql's shared pool owns background eviction threads.
    }

    private static void run(Case c, int batch, long expected) {
        long sum = 0;
        for (int i = 0; i < batch; i++) {
            long value = c.action.run();
            if (value != expected) throw new AssertionError(c.name + " unstable output: " + value + " != " + expected);
            sum += value;
        }
        valueSink = sum;
    }

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (int n : new int[] { 128, 512, 1024 }) {
            StringBuilder hash = new StringBuilder("SELECT * FROM ");
            StringBuilder aliases = new StringBuilder("SELECT ");
            for (int i = 0; i < n; i++) {
                if (i > 0) { hash.append(", "); aliases.append(", "); }
                hash.append("#t").append(i);
                // Hyphens exercise bracket/alias context. Plain [x0] [alias] does not need these tables.
                aliases.append("[order-id").append(i).append("] [alias-id").append(i).append(']');
            }
            String hashes = hash.toString();
            String brackets = aliases.toString();
            addIndex(cases, "index_dense_hash", n, hashes, "missing");
            addIndex(cases, "index_dense_bracket", n, brackets, "missing");
            addMatcher(cases, "matcher_dense_hash", n, hashes, false);
            if (n == 1024) {
                addIndex(cases, "index_first_hash", n, hashes, "#t0");
                addIndex(cases, "index_quarter_hash", n, hashes, "#t256");
                addIndex(cases, "index_middle_hash", n, hashes, "#t512");
                addMatcher(cases, "matcher_first_hash", n, hashes, true);
                cases.add(new Case("tokenize_dense_hash", n, hashes, () -> {
                    List<String> tokens = SqlParser.tokenize(hashes);
                    objectSink = tokens;
                    return tokens.size();
                }));
            }
        }
        for (int n : new int[] { 20000, 1048576 }) {
            String sql = "SELECT * FROM #t " + "x".repeat(n);
            addIndex(cases, "index_sparse_hash", n, sql, "missing");
            addMatcher(cases, "matcher_sparse_hash", n, sql, false);
        }
        String ordinary = "SELECT " + "COALESCE(a,b),".repeat(1024) + "1 FROM t";
        addIndex(cases, "index_no_hash_control", 1024, ordinary, "missing");
        addRead(cases, "read_ordinary_control", 1024, ordinary);
        addRead(cases, "read_disjoint_bracket_names", 1024, "SELECT " + "[update date],".repeat(1024) + "1 FROM t");
        addRead(cases, "read_shared_first_word", 8192,
                "SELECT '" + "[/*".repeat(8192) + "*/ UPDATE " + "x".repeat(8192) + "]'");
        for (int n : new int[] { 1000, 2000, 4000, 8000 }) {
            addRead(cases, "read_distinct_overlapping_regions", n,
                    "SELECT '" + "[/*".repeat(n) + "*/ UPDATE x ".repeat(n) + "]'");
        }
        for (int n : new int[] { 1, 32, 1024 }) {
            addParse(cases, "parse_named_repeated", n, "SELECT " + ":id,".repeat(n - 1) + ":id");
        }
        StringBuilder unique = new StringBuilder("SELECT ");
        for (int i = 0; i < 1024; i++) {
            if (i > 0) unique.append(',');
            unique.append(":name").append(i);
        }
        addParse(cases, "parse_named_unique", 1024, unique.toString());
        addParse(cases, "parse_positional_control", 1024, "SELECT " + "?,".repeat(1023) + "?");
        return cases;
    }

    private static void addIndex(List<Case> cases, String name, int n, String sql, String token) {
        cases.add(new Case(name, n, sql, () -> SqlParser.indexOfToken(sql, token)));
    }

    private static void addMatcher(List<Case> cases, String name, int n, String sql, boolean firstOnly) {
        cases.add(new Case(name, n, sql, () -> {
            var matcher = SqlParser.hashPrefixedIdentifierMatcher(sql, SqlParser.defaultTokenizerConfig());
            long count = 0;
            for (int at = sql.indexOf('#'); at >= 0; at = sql.indexOf('#', at + 1)) {
                if (matcher.test(at)) count++;
                if (firstOnly) break;
            }
            return count;
        }));
    }

    private static void addRead(List<Case> cases, String name, int n, String sql) {
        cases.add(new Case(name, n, sql, () -> SqlParser.isSyntacticallyReadQuery(sql) ? 1 : 0));
    }

    private static void addParse(List<Case> cases, String name, int n, String sql) {
        cases.add(new Case(name, n, sql, () -> {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            objectSink = parsed;
            return ((long) parsed.parameterCount() << 32) + parsed.parameterizedSql().length();
        }));
    }
}
