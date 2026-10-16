package com.landawn.abacus.query;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Diagnostic cursor scaling/allocation probe. Run in separate JVMs after correctness checks. */
public final class CursorProbe {
    private static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static volatile long sink;
    private static volatile Object textSink;
    private record Case(String name, int scale, String sql, boolean text) { }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        ALLOC.setThreadAllocatedMemoryEnabled(true);
        String fork = args.length == 0 ? "1" : args[0];
        List<Case> cases = new ArrayList<>();
        for (int n : new int[] {64, 128, 256, 512, 1024, 2048, 4096, 8192}) {
            StringBuilder hashes = new StringBuilder("SELECT * FROM ");
            StringBuilder aliases = new StringBuilder("SELECT ");
            for (int i = 0; i < n; i++) {
                if (i > 0) { hashes.append(", "); aliases.append(", "); }
                hashes.append("#t").append(i);
                aliases.append("[order-id").append(i).append("] [alias-id").append(i).append(']');
            }
            cases.add(new Case("cursor_hash", n, hashes.toString(), false));
            cases.add(new Case("cursor_bracket_alias", n, aliases.toString(), false));
            cases.add(new Case("cursor_subscript_chain", n, "SELECT x[0]" + " [1]".repeat(n), false));
            if (n <= 1024) cases.add(new Case("cursor_hash_text", n, hashes.toString(), true));
        }
        for (Case c : cases) {
            long count = SqlParser.tokenize(c.sql).stream().filter(t -> !t.equals(" ")).count();
            if (one(c) != count) throw new AssertionError(c.name + " token count");
        }
        System.out.println("fork,case,scale,sql_chars,round,operations,ns_per_op,bytes_per_op,result");
        long thread = Thread.currentThread().threadId();
        for (Case c : cases) {
            int batch = 1;
            long begin = System.nanoTime();
            do {
                long start = System.nanoTime();
                run(c, batch);
                if (System.nanoTime() - start < 5_000_000 && batch < 32768) batch *= 2;
            } while (System.nanoTime() - begin < 300_000_000);
            for (int round = 1; round <= 7; round++) {
                long bytes = ALLOC.getThreadAllocatedBytes(thread);
                long start = System.nanoTime();
                long result = run(c, batch);
                long time = System.nanoTime() - start;
                bytes = ALLOC.getThreadAllocatedBytes(thread) - bytes;
                System.out.printf("%s,%s,%d,%d,%d,%d,%.3f,%.3f,%d%n", fork, c.name, c.scale,
                        c.sql.length(), round, batch, time / (double) batch, bytes / (double) batch, result / batch);
            }
        }
    }

    private static long run(Case c, int batch) {
        long total = 0;
        for (int i = 0; i < batch; i++) total += one(c);
        sink = total;
        return total;
    }

    private static long one(Case c) {
        SqlParser.TokenCursor cursor = SqlParser.cursor(c.sql);
        long count = 0;
        while (cursor.next()) {
            if (cursor.endIndex() <= cursor.startIndex()) throw new AssertionError("Empty token");
            if (c.text) textSink = cursor.token();
            count++;
        }
        return count;
    }
}
