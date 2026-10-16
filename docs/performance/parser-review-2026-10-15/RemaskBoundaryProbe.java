import java.lang.reflect.Method;

import com.landawn.abacus.query.SqlParser;

/** Structural assertion only; no timing or production changes. */
public final class RemaskBoundaryProbe {
    public static void main(String[] args) throws Exception {
        Class<?> memoType = Class.forName("com.landawn.abacus.query.SqlParser$HashScanMemo");
        Method mask = SqlParser.class.getDeclaredMethod("maskQuotedRegionsForClassification",
                String.class, SqlParser.TokenizerConfig.class, int.class, boolean.class, memoType, boolean.class);
        mask.setAccessible(true);

        String sql = "SELECT :x$tag$ abc x$tag$";
        int dollarQuoteAxis = 16;
        String first = (String) mask.invoke(null, sql, SqlParser.defaultTokenizerConfig(),
                dollarQuoteAxis, false, null, false);
        String second = (String) mask.invoke(null, first, SqlParser.defaultTokenizerConfig(),
                dollarQuoteAxis, false, null, false);

        if (sql.indexOf('#') >= 0 || !"SELECT : $tag$ abc x$tag$".equals(first)
                || second == null || first.equals(second) || first.length() != second.length()) {
            throw new AssertionError("Expected a no-hash parameter mask to expose a dollar quote: "
                    + "first=[" + first + "], second=[" + second + "]");
        }
        System.out.println("PASS: no-hash remasking changes dollar-quote boundaries");
        System.out.println("first =[" + first + "]");
        System.out.println("second=[" + second + "]");
    }
}
