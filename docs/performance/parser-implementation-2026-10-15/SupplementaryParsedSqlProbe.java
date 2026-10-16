package com.landawn.abacus.query;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

/** Diagnostic allocation/timing controls for named-parameter storage; not a JMH benchmark. */
public final class SupplementaryParsedSqlProbe {
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
        // Match ParserReviewProbe's mixed-path preparation and per-case measurement protocol.
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
        for (int n : new int[] { 1, 32, 1024 }) {
            addNames(cases, "named_distinct", n, ":", "", i -> "p" + i);
            addNames(cases, "mybatis_repeated", n, "#{", "}", i -> "id");
            addNames(cases, "mybatis_distinct", n, "#{", "}", i -> "p" + i);
            // At n == 1 this is a singleton control; larger inputs repeat only their first two names.
            addNames(cases, "named_early_repeat_unique", n, ":", "", i -> i < 2 ? "id" : "p" + i);
        }
        return cases;
    }

    private static void addNames(List<Case> cases, String name, int n, String prefix, String suffix, IntFunction<String> nameAt) {
        StringBuilder builder = new StringBuilder("SELECT ");
        List<String> expectedNames = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (i > 0) { builder.append(','); }
            String parameter = nameAt.apply(i);
            expectedNames.add(parameter);
            builder.append(prefix).append(parameter).append(suffix);
        }
        String sql = builder.toString();
        // Verify every name and generated marker outside the timed/allocation measurement windows.
        ParsedSql checked = ParsedSql.parse(sql, null);
        String expectedSql = "SELECT " + "?,".repeat(n - 1) + "?";
        if (checked.parameterCount() != n || !expectedNames.equals(checked.namedParameters())
                || !expectedSql.equals(checked.parameterizedSql())) {
            throw new AssertionError(name + " incorrect parameter extraction at scale " + n);
        }
        cases.add(new Case(name, n, sql, () -> {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            objectSink = parsed;
            return ((long) parsed.parameterCount() << 32) + parsed.parameterizedSql().length();
        }));
    }
}
