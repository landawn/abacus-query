import com.landawn.abacus.query.SqlParser;
import java.lang.management.ManagementFactory;

public class ParserReviewProbe {
    static volatile Object sink;

    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 10000; i++) sink = SqlParser.tokenize("SELECT id, name FROM users WHERE id = ? AND active = 1");
        for (String kind : new String[] { "identifier", "literal", "comment", "short", "dense", "dense-large" }) {
            String sql = switch (kind) {
                case "identifier" -> "x".repeat(8 * 1024 * 1024);
                case "literal" -> "'" + "x".repeat(8 * 1024 * 1024) + "'";
                case "comment" -> "/*" + "x".repeat(8 * 1024 * 1024) + "*/SELECT 1";
                case "dense" -> "x,".repeat(5000) + "x";
                case "dense-large" -> "x,".repeat(50000) + "x";
                default -> "SELECT id, name FROM users WHERE id = ? AND active = 1";
            };
            int count = kind.equals("short") ? 100000 : kind.equals("dense") ? 1000 : kind.equals("dense-large") ? 200 : 10;
            for (int i = 0; i < Math.min(count, 30); i++) sink = SqlParser.tokenize(sql);
            long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
            long time = System.nanoTime();
            for (int i = 0; i < count; i++) sink = SqlParser.tokenize(sql);
            System.out.printf("%s: %.1f bytes/op, %.1f us/op%n", kind,
                    (bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes) / (double) count,
                    (System.nanoTime() - time) / (1000.0 * count));
        }
        for (int count : new int[] { 1000, 2000, 4000, 8000 }) {
            String sql = "SELECT * FROM " + "#t,".repeat(count - 1) + "#t";
            long time = System.nanoTime();
            sink = SqlParser.tokenize(sql);
            System.out.printf("temp list %d: %.1f ms%n", count, (System.nanoTime() - time) / 1_000_000.0);
        }
    }
}
