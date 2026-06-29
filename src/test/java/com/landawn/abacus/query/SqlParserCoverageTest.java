package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Additional coverage for {@link SqlParser} public methods that were previously untested or only
 * lightly covered: the {@code indexOfWord} convenience overloads, {@code nextWordEnd},
 * {@code unregisterSeparator}/{@code resetSeparators}, and the SQL classification family
 * ({@code isSelectQuery}/{@code isInsertQuery}/{@code isInsertOrReplaceQuery}/{@code isReadOnlyQuery}/
 * {@code isNoUpdateQuery}).
 */
@Tag("2025")
public class SqlParserCoverageTest extends TestBase {

    /** Restore the global separator set after each test so separator-mutating tests stay isolated. */
    @AfterEach
    public void restoreSeparators() {
        SqlParser.resetSeparators();
    }

    // ----------------------------------------------------------------------------------------------
    // indexOfWord(String, String) -- 2-arg convenience overload
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfWord2Arg_findsCaseInsensitiveFromStart() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";

        assertEquals(0, SqlParser.indexOfWord(sql, "SELECT"));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfWord(sql, "FROM"));
        // Default is case-insensitive.
        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfWord(sql, "where"));
        // Composite keyword.
        assertEquals(sql.indexOf("ORDER"), SqlParser.indexOfWord(sql, "ORDER BY"));
    }

    @Test
    public void testIndexOfWord2Arg_notFoundReturnsMinusOne() {
        String sql = "SELECT * FROM users";

        assertEquals(-1, SqlParser.indexOfWord(sql, "WHERE"));
    }

    // ----------------------------------------------------------------------------------------------
    // indexOfWord(String, String, int) -- 3-arg convenience overload
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfWord3Arg_respectsFromIndex() {
        String sql = "SELECT name FROM users WHERE name = 'x' OR name = 'y'";

        int first = SqlParser.indexOfWord(sql, "name", 0);
        assertTrue(first >= 0);

        int second = SqlParser.indexOfWord(sql, "name", first + 1);
        assertTrue(second > first);

        int third = SqlParser.indexOfWord(sql, "name", second + 1);
        assertTrue(third > second);
    }

    @Test
    public void testIndexOfWord3Arg_defaultsToCaseInsensitive() {
        String sql = "select * from users";

        assertEquals(0, SqlParser.indexOfWord(sql, "SELECT", 0));
    }

    @Test
    public void testIndexOfWord3Arg_negativeFromIndexTreatedAsZero() {
        String sql = "SELECT * FROM users";

        assertEquals(0, SqlParser.indexOfWord(sql, "SELECT", -10));
    }

    // ----------------------------------------------------------------------------------------------
    // nextWordEnd(String, int)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testNextWordEnd_javadocExample() {
        String sql = "SELECT   name,   age FROM users";

        assertEquals(13, SqlParser.nextWordEnd(sql, 6)); // just past "name"
        assertEquals(14, SqlParser.nextWordEnd(sql, 13)); // just past ","
        assertEquals(20, SqlParser.nextWordEnd(sql, 14)); // just past "age"
    }

    @Test
    public void testNextWordEnd_quotedToken() {
        String sql = "SELECT 'a b' FROM t";
        int end = SqlParser.nextWordEnd(sql, 6);

        assertEquals(sql.indexOf("'a b'") + "'a b'".length(), end);
        assertEquals("'a b'", sql.substring(end - "'a b'".length(), end));
    }

    @Test
    public void testNextWordEnd_multiCharOperator() {
        String sql = "a >= b";
        int end = SqlParser.nextWordEnd(sql, 1); // scanning from the space before ">="

        assertEquals(sql.indexOf(">=") + 2, end);
    }

    @Test
    public void testNextWordEnd_skipsLeadingBlockComment() {
        String sql = "SELECT /* hidden */ name FROM users";
        int end = SqlParser.nextWordEnd(sql, 6);

        assertEquals(sql.indexOf("name") + "name".length(), end);
    }

    @Test
    public void testNextWordEnd_noFurtherTokenReturnsLength() {
        String sql = "SELECT   ";

        assertEquals(sql.length(), SqlParser.nextWordEnd(sql, 6));
    }

    @Test
    public void testNextWordEnd_onlyCommentRemainsReturnsLength() {
        String sql = "x -- trailing comment";

        // After "x" only whitespace + a line comment remain.
        assertEquals(sql.length(), SqlParser.nextWordEnd(sql, 1));
    }

    @Test
    public void testNextWordEnd_negativeFromIndexTreatedAsZero() {
        String sql = "SELECT * FROM users";

        assertEquals("SELECT".length(), SqlParser.nextWordEnd(sql, -5));
    }

    @Test
    public void testNextWordEnd_consistentWithNextWord() {
        assertNextWordConsistency("SELECT   name,   age FROM users");
        assertNextWordConsistency("WHERE a >= b AND c != d");
        assertNextWordConsistency("SELECT 'a b', \"q\" FROM t -- tail\n");
        assertNextWordConsistency("SELECT /* c */ x FROM #tmp");
        assertNextWordConsistency("a||b->>c");
        assertNextWordConsistency("");
        assertNextWordConsistency("   ");
    }

    /**
     * Walks {@code sql} with {@link SqlParser#nextWord} and {@link SqlParser#nextWordEnd} in lockstep
     * and asserts that the end index reported for each token bounds exactly the token text returned.
     */
    private static void assertNextWordConsistency(final String sql) {
        int idx = 0;

        while (idx <= sql.length()) {
            final String word = SqlParser.nextWord(sql, idx);
            final int end = SqlParser.nextWordEnd(sql, idx);

            if (word.isEmpty()) {
                assertEquals(sql.length(), end, "empty token => end at length for [" + sql + "] idx=" + idx);
                break;
            }

            final int start = end - word.length();
            assertTrue(start >= idx, "token start >= scan position for [" + sql + "] idx=" + idx);
            assertEquals(word, sql.substring(start, end), "token text bounded by nextWordEnd for [" + sql + "] idx=" + idx);

            if (end <= idx) {
                break; // safety against an accidental infinite loop in a failing build
            }

            idx = end;
        }
    }

    // ----------------------------------------------------------------------------------------------
    // unregisterSeparator(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testUnregisterSeparator_multiChar() {
        SqlParser.registerSeparator("::");
        assertEquals(Arrays.asList("a", "::", "b"), SqlParser.parse("a::b"));

        SqlParser.unregisterSeparator("::");
        // No longer a separator: "a::b" is a single token now.
        assertEquals(Arrays.asList("a::b"), SqlParser.parse("a::b"));
    }

    @Test
    public void testUnregisterSeparator_singleCharRemovesCharForm() {
        SqlParser.registerSeparator("$");
        assertEquals(Arrays.asList("a", "$", "b"), SqlParser.parse("a$b"));

        SqlParser.unregisterSeparator("$");
        assertEquals(Arrays.asList("a$b"), SqlParser.parse("a$b"));
    }

    @Test
    public void testUnregisterSeparator_removesBuiltInDefault() {
        // "##" is a built-in default separator.
        assertTrue(SqlParser.parse("1 ## 2").contains("##"));

        SqlParser.unregisterSeparator("##");
        assertFalse(SqlParser.parse("1##2").contains("##"));

        // resetSeparators() (via @AfterEach) restores it.
    }

    @Test
    public void testUnregisterSeparator_unknownIsNoOp() {
        // Unregistering something never registered must not throw and must not corrupt parsing.
        SqlParser.unregisterSeparator("%%%%%%%");

        assertEquals(Arrays.asList("SELECT", " ", "*", " ", "FROM", " ", "t"), SqlParser.parse("SELECT * FROM t"));
    }

    @Test
    public void testUnregisterSeparator_nullOrEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlParser.unregisterSeparator(null));
        assertThrows(IllegalArgumentException.class, () -> SqlParser.unregisterSeparator(""));
    }

    @Test
    public void testUnregisterSeparator_longMultiCharRemoval() {
        // '$' is not a built-in separator, so only the registered 6-char string can split here.
        SqlParser.registerSeparator("$$$$$$"); // length 6, longer than any default
        assertEquals(Arrays.asList("a", "$$$$$$", "b"), SqlParser.parse("a$$$$$$b"));

        SqlParser.unregisterSeparator("$$$$$$");
        // After removal the 6-char run is no longer a separator and stays a single token.
        assertEquals(Arrays.asList("a$$$$$$b"), SqlParser.parse("a$$$$$$b"));
    }

    // ----------------------------------------------------------------------------------------------
    // resetSeparators()
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testResetSeparators_dropsCustomAndRestoresDefaults() {
        SqlParser.registerSeparator("::");
        SqlParser.registerSeparator('§');
        SqlParser.unregisterSeparator("##"); // remove a default too

        SqlParser.resetSeparators();

        // Custom separators are gone.
        assertEquals(Arrays.asList("a::b"), SqlParser.parse("a::b"));
        assertEquals(Arrays.asList("a§b"), SqlParser.parse("a§b"));
        // The previously-removed default is back.
        assertTrue(SqlParser.parse("1 ## 2").contains("##"));
    }

    // ----------------------------------------------------------------------------------------------
    // isSelectQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsSelectQuery_basic() {
        assertTrue(SqlParser.isSelectQuery("SELECT * FROM users"));
        assertTrue(SqlParser.isSelectQuery("select id, name from products"));
        assertTrue(SqlParser.isSelectQuery("   SELECT count(*) FROM orders"));
        assertFalse(SqlParser.isSelectQuery("UPDATE users SET name = 'John'"));
        assertFalse(SqlParser.isSelectQuery("INSERT INTO users VALUES (1, 'John')"));
        assertFalse(SqlParser.isSelectQuery("DELETE FROM users"));
    }

    @Test
    public void testIsSelectQuery_nullAndEmpty() {
        assertFalse(SqlParser.isSelectQuery(null));
        assertFalse(SqlParser.isSelectQuery(""));
        assertFalse(SqlParser.isSelectQuery("    "));
    }

    @Test
    public void testIsSelectQuery_leadingComments() {
        assertTrue(SqlParser.isSelectQuery("-- a leading comment\nSELECT 1"));
        assertTrue(SqlParser.isSelectQuery("/* block */ SELECT 1"));
        assertTrue(SqlParser.isSelectQuery("# hash comment\nSELECT 1"));
    }

    @Test
    public void testIsSelectQuery_leadingParentheses() {
        assertTrue(SqlParser.isSelectQuery("(SELECT 1)"));
        assertTrue(SqlParser.isSelectQuery("((SELECT 1))"));
        assertTrue(SqlParser.isSelectQuery("(SELECT a FROM t1) UNION ALL (SELECT a FROM t2)"));
    }

    @Test
    public void testIsSelectQuery_withCte() {
        assertTrue(SqlParser.isSelectQuery("WITH t AS (SELECT 1) SELECT * FROM t"));
        assertTrue(SqlParser.isSelectQuery("WITH RECURSIVE t AS (SELECT 1) SELECT * FROM t"));
        assertTrue(SqlParser.isSelectQuery("WITH a AS (SELECT 1), b AS (SELECT 2) SELECT * FROM a, b"));
        // Data-modifying CTE body, but the top-level statement is still SELECT.
        assertTrue(SqlParser.isSelectQuery("WITH t AS (DELETE FROM x RETURNING *) SELECT * FROM t"));
        // CTE followed by DELETE is NOT a select.
        assertFalse(SqlParser.isSelectQuery("WITH t AS (SELECT 1) DELETE FROM t"));
    }

    // ----------------------------------------------------------------------------------------------
    // isInsertQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsInsertQuery_basic() {
        assertTrue(SqlParser.isInsertQuery("INSERT INTO users VALUES (1, 'John')"));
        assertTrue(SqlParser.isInsertQuery("insert into products (name, price) values ('Widget', 9.99)"));
        assertTrue(SqlParser.isInsertQuery("   INSERT INTO orders (order_id) VALUES (100)"));
        assertFalse(SqlParser.isInsertQuery("SELECT * FROM users"));
        assertFalse(SqlParser.isInsertQuery("UPDATE users SET name = 'John'"));
    }

    @Test
    public void testIsInsertQuery_withCte() {
        assertTrue(SqlParser.isInsertQuery("WITH t AS (SELECT 1) INSERT INTO x SELECT * FROM t"));
    }

    @Test
    public void testIsInsertQuery_nullAndEmpty() {
        assertFalse(SqlParser.isInsertQuery(null));
        assertFalse(SqlParser.isInsertQuery(""));
    }

    // ----------------------------------------------------------------------------------------------
    // isInsertOrReplaceQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsInsertOrReplaceQuery_basic() {
        assertTrue(SqlParser.isInsertOrReplaceQuery("INSERT OR REPLACE INTO t (id) VALUES (1)"));
        assertTrue(SqlParser.isInsertOrReplaceQuery("insert or replace into t (id) values (1)"));
        assertTrue(SqlParser.isInsertOrReplaceQuery("  /* c */ INSERT OR REPLACE INTO t VALUES (1)"));
    }

    @Test
    public void testIsInsertOrReplaceQuery_negativeCases() {
        assertFalse(SqlParser.isInsertOrReplaceQuery("INSERT INTO t (id) VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("REPLACE INTO t (id) VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("INSERT OR ROLLBACK INTO t VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("SELECT * FROM t"));
    }

    @Test
    public void testIsInsertOrReplaceQuery_nullAndEmpty() {
        assertFalse(SqlParser.isInsertOrReplaceQuery(null));
        assertFalse(SqlParser.isInsertOrReplaceQuery(""));
    }

    // ----------------------------------------------------------------------------------------------
    // isReadOnlyQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsReadOnlyQuery_plainSelectsAreReadOnly() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT id FROM t WHERE x IN (SELECT max(id) FROM u)"));
    }

    @Test
    public void testIsReadOnlyQuery_keywordsInLiteralsAndCommentsIgnored() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT 'DELETE' FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t -- DELETE\nWHERE x = 1"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t /* UPDATE */ WHERE x = 1"));
    }

    @Test
    public void testIsReadOnlyQuery_mutationsAreNotReadOnly() {
        assertFalse(SqlParser.isReadOnlyQuery("UPDATE t SET x = 1"));
        assertFalse(SqlParser.isReadOnlyQuery("INSERT INTO t VALUES (1)"));
        assertFalse(SqlParser.isReadOnlyQuery("DELETE FROM t"));
        // SELECT ... INTO creates a new table.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * INTO newt FROM t"));
        // Top-level mutation after a statement separator.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; DELETE FROM t"));
        // Data-modifying CTE.
        assertFalse(SqlParser.isReadOnlyQuery("WITH t AS (DELETE FROM x RETURNING *) SELECT * FROM t"));
    }

    @Test
    public void testIsReadOnlyQuery_nullAndEmpty() {
        assertFalse(SqlParser.isReadOnlyQuery(null));
        assertFalse(SqlParser.isReadOnlyQuery(""));
    }

    // ----------------------------------------------------------------------------------------------
    // isNoUpdateQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsNoUpdateQuery_readsAndPlainInsertsAllowed() {
        assertTrue(SqlParser.isNoUpdateQuery("SELECT * FROM t"));
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1)"));
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t (a) SELECT a FROM u"));
        // ON CONFLICT ... DO NOTHING never overwrites existing rows.
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_upsertsAndMutationsRejected() {
        assertFalse(SqlParser.isNoUpdateQuery("INSERT OR REPLACE INTO t VALUES (1)"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON DUPLICATE KEY UPDATE x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO UPDATE SET x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT OVERWRITE TABLE t SELECT * FROM s"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * INTO newt FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("UPDATE t SET x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("DELETE FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; DELETE FROM t"));
    }

    @Test
    public void testIsNoUpdateQuery_nullAndEmpty() {
        assertFalse(SqlParser.isNoUpdateQuery(null));
        assertFalse(SqlParser.isNoUpdateQuery(""));
    }

    // ----------------------------------------------------------------------------------------------
    // A couple of extra parse()/isFunctionName edge paths for completeness.
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_unterminatedSingleQuoteKeepsRemainderAsOneToken() {
        // An unterminated quote swallows the rest of the input as a single token (documented behavior).
        List<String> words = SqlParser.parse("SELECT 'oops FROM t");

        assertTrue(words.contains("SELECT"));
        assertTrue(words.stream().anyMatch(w -> w.startsWith("'oops")));
    }

    @Test
    public void testParse_keepCommentsRetainsMultipleBlockComments() {
        String sql = "-- Keep comments\nSELECT /* a */ 1 /* b */ FROM t";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("/* a */"));
        assertTrue(words.contains("/* b */"));
    }

    @Test
    public void testIsFunctionName_indexAtOrBeyondEnd() {
        List<String> words = SqlParser.parse("SELECT COUNT(*)");

        // index == len-1 with no following '(' token, and index beyond range: both false, no throw.
        assertFalse(SqlParser.isFunctionName(words, words.size(), words.size() - 1));
    }
}
