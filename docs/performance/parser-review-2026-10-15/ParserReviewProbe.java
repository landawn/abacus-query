package com.landawn.abacus.query;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Diagnostic scaling/allocation probe for the unchanged parser sources; not a JMH benchmark. */
public final class ParserReviewProbe {
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
        ALLOC.setThreadAllocatedMemoryEnabled(true);
        String fork = args.length == 0 ? "1" : args[0];
        String filter = args.length < 2 ? "" : args[1];
        List<Case> cases = cases();
        // Expose mixed parser paths before measuring any individual family.
        for (int pass = 0; pass < 2; pass++) {
            for (Case c : cases) {
                if (filter.isEmpty() || c.name.contains(filter)) {
                    for (int repeat = 0; repeat < 3; repeat++) {
                        valueSink = c.action.run();
                    }
                }
            }
        }
        System.out.println("fork,case,scale,sql_chars,round,operations,ns_per_op,bytes_per_op,result");
        for (Case c : cases) {
            if (!filter.isEmpty() && !c.name.contains(filter)) {
                continue;
            }
            long expected = c.action.run();
            int batch = 1;
            long warmStart = System.nanoTime();
            do {
                long start = System.nanoTime();
                run(c, batch, expected);
                long elapsed = System.nanoTime() - start;
                if (elapsed < 5_000_000 && batch < 32768) {
                    batch *= 2;
                }
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
            if (value != expected) {
                throw new AssertionError(c.name + " unstable output: " + value + " != " + expected);
            }
            sum += value;
        }
        valueSink = sum;
    }

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (int n : new int[] { 64, 128, 256, 512, 1024 }) {
            StringBuilder hash = new StringBuilder("SELECT * FROM ");
            StringBuilder aliases = new StringBuilder("SELECT ");
            for (int i = 0; i < n; i++) {
                if (i > 0) { hash.append(", "); aliases.append(", "); }
                hash.append("#t").append(i);
                aliases.append("[order-id").append(i).append("] [alias-id").append(i).append(']');
            }
            addWalk(cases, "hash", n, hash.toString());
            addWalk(cases, "bracket_alias", n, aliases.toString());
            addWalk(cases, "subscript_chain", n, "SELECT x[0]" + " [1]".repeat(n));
        }
        for (int n : new int[] { 0, 1024, 65536, 1048576 }) {
            for (String prefix : new String[] { "FROM #temp ", "FROM /*x*/ #temp " }) {
                String sql = prefix + "x".repeat(n);
                String name = prefix.contains("/*") ? "early_hash_block_comment" : "early_hash";
                cases.add(new Case(name, n, sql, () -> SqlParser.nextTokenEndIndex(sql, 4)));
            }
        }
        for (int n : new int[] { 512, 1024, 2048, 4096, 8192, 16384 }) {
            addRead(cases, "multiline_markers_no_close", n, "SELECT 1\n" + "#{\n".repeat(n));
            addRead(cases, "multiline_markers_final_close", n, "SELECT 1\n" + "#{\n".repeat(n) + "}");
            if (n <= 8192) {
                addRead(cases, "raw_bracket_shared_suffix", n,
                        "SELECT '" + "[/*".repeat(n) + "*/ UPDATE " + "x".repeat(n) + "]'");
            }
        }
        for (int n : new int[] { 16, 256, 4096, 65536, 1048576 }) {
            addRead(cases, "quoted_literal", n, "SELECT '" + "x".repeat(n) + "'");
            addParse(cases, "unrecognized_ddl", n, "CREATE TABLE t (x VARCHAR DEFAULT '" + "x".repeat(n) + "')");
        }
        for (int n : new int[] { 16, 128, 512, 1024 }) {
            addTokens(cases, "comment_boundaries", n, "SELECT " + "1/**/+".repeat(n) + "1");
            addTokens(cases, "comment_free_control", n, "SELECT " + "1+".repeat(n) + "1");
            addRead(cases, "ordinary_parentheses", n, "SELECT " + "COALESCE(a,b),".repeat(n) + "1 FROM t");
            String ordinary = "SELECT " + "a,".repeat(n) + "1 FROM t";
            addRead(cases, "mode_plain", n, ordinary + " /* plain */");
            addRead(cases, "mode_inert_features", n, ordinary + " /* \\ ' \" ` E' $ [ -- */");
        }
        for (int n : new int[] { 1, 2, 16, 128, 1024 }) {
            addParse(cases, "positional", n, "SELECT " + "?,".repeat(n - 1) + "?");
            addParse(cases, "named_repeated", n, "SELECT " + ":id,".repeat(n - 1) + ":id");
        }
        return cases;
    }

    private static void addWalk(List<Case> cases, String name, int n, String sql) {
        // Validate the public walkers' token text outside every measurement window.
        List<String> expected = SqlParser.tokenize(sql).stream().filter(s -> !s.equals(" ")).toList();
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < sql.length();) {
            String word = SqlParser.nextToken(sql, i);
            int next = SqlParser.nextTokenEndIndex(sql, i);
            if (next <= i) { throw new AssertionError("No progress"); }
            if (!word.isEmpty()) { actual.add(word); }
            i = next;
        }
        if (!expected.equals(actual)) { throw new AssertionError(name + " disagrees with tokenize"); }
        cases.add(new Case("walk_" + name, n, sql, () -> {
            int count = 0;
            for (int i = 0; i < sql.length();) {
                i = SqlParser.nextTokenEndIndex(sql, i);
                count++;
            }
            return count;
        }));
        addTokens(cases, "tokenize_" + name, n, sql);
    }

    private static void addRead(List<Case> cases, String name, int n, String sql) {
        cases.add(new Case(name, n, sql, () -> SqlParser.isSyntacticallyReadQuery(sql) ? 1 : 0));
    }

    private static void addTokens(List<Case> cases, String name, int n, String sql) {
        cases.add(new Case(name, n, sql, () -> {
            List<String> tokens = SqlParser.tokenize(sql);
            objectSink = tokens;
            return tokens.size();
        }));
    }

    private static void addParse(List<Case> cases, String name, int n, String sql) {
        cases.add(new Case(name, n, sql, () -> {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            objectSink = parsed;
            return ((long) parsed.parameterCount() << 32) + parsed.parameterizedSql().length();
        }));
    }
}
