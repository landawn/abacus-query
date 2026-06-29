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

    @Test
    public void testIsSelectQuery_keywordPrefixIsNotAKeyword() {
        assertFalse(SqlParser.isSelectQuery("SELECT1 * FROM t"));
        assertFalse(SqlParser.isSelectQuery("select_value FROM t"));
        assertFalse(SqlParser.isSelectQuery("WITH t AS (SELECT 1) SELECT1 FROM t"));
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
    public void testIsInsertQuery_keywordPrefixIsNotAKeyword() {
        assertFalse(SqlParser.isInsertQuery("INSERT1 INTO t VALUES (1)"));
        assertFalse(SqlParser.isInsertQuery("insert_value INTO t VALUES (1)"));
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
        assertTrue(SqlParser.isInsertOrReplaceQuery("WITH seed AS (SELECT 1) INSERT OR REPLACE INTO t SELECT * FROM seed"));
    }

    @Test
    public void testIsInsertOrReplaceQuery_negativeCases() {
        assertFalse(SqlParser.isInsertOrReplaceQuery("INSERT INTO t (id) VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("REPLACE INTO t (id) VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("INSERT OR ROLLBACK INTO t VALUES (1)"));
        assertFalse(SqlParser.isInsertOrReplaceQuery("INSERT OR REPLACE1 INTO t VALUES (1)"));
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
    public void testIsReadOnlyQuery_hashTokensDoNotHideFollowingMutation() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * FROM #tmp; DELETE FROM users"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT payload #>> '{meta,status}' FROM docs; DELETE FROM docs"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM #tmp WHERE note = 'DELETE'"));
    }

    @Test
    public void testIsReadOnlyQuery_hashIdentifierAfterCommentsDoesNotHideMutation() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * FROM /* temp */ #tmp; DELETE FROM users"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * FROM -- temp\n#tmp; DELETE FROM users"));
    }

    @Test
    public void testIsReadOnlyQuery_identifierKeywordPrefixesIgnored() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT update_time FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT merge_value FROM t"));
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
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t (do, update) VALUES (1, 2)"));
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t (duplicate, key, update) VALUES (1, 2, 3)"));
        // ON CONFLICT ... DO NOTHING never overwrites existing rows.
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO NOTHING"));
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (do, update) DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_upsertsAndMutationsRejected() {
        assertFalse(SqlParser.isNoUpdateQuery("INSERT OR REPLACE INTO t VALUES (1)"));
        assertFalse(SqlParser.isNoUpdateQuery("WITH seed AS (SELECT 1) INSERT OR REPLACE INTO t SELECT * FROM seed"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON DUPLICATE KEY UPDATE x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON /* c */ DUPLICATE KEY UPDATE x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO UPDATE SET x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) WHERE active DO UPDATE SET x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT OVERWRITE TABLE t SELECT * FROM s"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * INTO newt FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("UPDATE t SET x = 1"));
        assertFalse(SqlParser.isNoUpdateQuery("DELETE FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; DELETE FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * FROM #tmp; DELETE FROM t"));
    }

    @Test
    public void testIsNoUpdateQuery_hashIdentifierAfterCommentsDoesNotHideMutation() {
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * FROM /* temp */ #tmp; DELETE FROM users"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * FROM -- temp\n#tmp; DELETE FROM users"));
    }

    @Test
    public void testIsNoUpdateQuery_nullAndEmpty() {
        // Empty/null does not lead with SELECT or INSERT, so it is not a no-update query
        // (consistent with isReadOnlyQuery/isSelectQuery/isInsertQuery).
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
    public void testTokenMethods_nullInputsThrow() {
        assertThrows(NullPointerException.class, () -> SqlParser.parse(null));
        assertThrows(NullPointerException.class, () -> SqlParser.indexOfWord(null, "SELECT"));
        assertThrows(NullPointerException.class, () -> SqlParser.indexOfWord("SELECT 1", null));
        assertThrows(NullPointerException.class, () -> SqlParser.nextWord(null, 0));
        assertThrows(NullPointerException.class, () -> SqlParser.nextWordEnd(null, 0));
    }

    @Test
    public void testIsFunctionName_indexAtOrBeyondEnd() {
        List<String> words = SqlParser.parse("SELECT COUNT(*)");

        // index == len-1 with no following '(' token, and index beyond range: both false, no throw.
        assertFalse(SqlParser.isFunctionName(words, words.size(), words.size() - 1));
        assertFalse(SqlParser.isFunctionName(words, words.size(), words.size()));
        assertFalse(SqlParser.isFunctionName(words, words.size(), -1));
        assertFalse(SqlParser.isFunctionName(words, 1, 2));
        assertTrue(SqlParser.isFunctionName(words, words.size() + 10, 2));
        assertThrows(NullPointerException.class, () -> SqlParser.isFunctionName(null, 0, 0));
    }

    @Test
    public void testIsFunctionName_oracleOuterJoinMarkerIsNotFunctionCall() {
        List<String> words = SqlParser.parse("SELECT a.id(+) FROM a");
        int idIndex = words.indexOf("id");

        assertTrue(words.contains("(+)"));
        assertFalse(SqlParser.isFunctionName(words, words.size(), idIndex));
    }

    // ----------------------------------------------------------------------------------------------
    // registerSeparator -- non-ASCII char path (char >= 128 skips the ASCII fast-table)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testRegisterSeparator_nonAsciiChar() {
        SqlParser.registerSeparator('§'); // 0xA7, >= 128 -> falls back to the Set, not the ASCII table

        assertEquals(Arrays.asList("a", "§", "b"), SqlParser.parse("a§b"));
        assertTrue(SqlParser.isSeparator("a§b", 3, 1, '§'));
    }

    @Test
    public void testRegisterSeparator_nonAsciiMultiCharString() {
        SqlParser.registerSeparator("→→"); // multi-char, non-ASCII

        assertEquals(Arrays.asList("a", "→→", "b"), SqlParser.parse("a→→b"));
    }

    // ----------------------------------------------------------------------------------------------
    // parse -- multi-character operator longest-match-first
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_longestMultiCharOperatorWins() {
        assertTrue(SqlParser.parse("a >>= b").contains(">>="));
        assertTrue(SqlParser.parse("a >> b").contains(">>"));
        assertTrue(SqlParser.parse("a ->> b").contains("->>"));
        assertTrue(SqlParser.parse("a #>> b").contains("#>>"));
        // Oracle outer-join marker is a registered 3-char separator.
        assertTrue(SqlParser.parse("WHERE a (+) = b").contains("(+)"));
    }

    @Test
    public void testParse_unterminatedBlockCommentDoesNotThrow() {
        List<String> words = SqlParser.parse("SELECT 1 /* unterminated");

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("1"));
    }

    @Test
    public void testParse_keepCommentsStillStripsLineComments() {
        // The "-- Keep comments" marker keeps block comments but line/hash comments are always stripped.
        String sql = "-- Keep comments\nSELECT 1 -- gone\n/* kept */ FROM t # also gone\n";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("/* kept */"));
        assertFalse(words.stream().anyMatch(w -> w.contains("gone")));
    }

    @Test
    public void testParse_hashPrefixedIdentifierAfterComments() {
        List<String> blockCommentWords = SqlParser.parse("SELECT * FROM /* temp */ #tmp WHERE id = 1");
        List<String> tightBlockCommentWords = SqlParser.parse("SELECT * FROM/* temp */#tmp WHERE id = 1");
        List<String> lineCommentWords = SqlParser.parse("SELECT * FROM -- temp\n#tmp WHERE id = 1");

        assertTrue(blockCommentWords.contains("#tmp"));
        assertTrue(blockCommentWords.contains("WHERE"));
        assertTrue(tightBlockCommentWords.contains("#tmp"));
        assertTrue(tightBlockCommentWords.contains("WHERE"));
        assertTrue(lineCommentWords.contains("#tmp"));
        assertTrue(lineCommentWords.contains("WHERE"));
    }

    // ----------------------------------------------------------------------------------------------
    // nextWord / nextWordEnd -- MyBatis #{...} stays one token
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testNextWord_myBatisMarkerIsOneToken() {
        assertEquals("#{userId}", SqlParser.nextWord("WHERE id = #{userId}", 10));
        assertNextWordConsistency("a #{x} b");
        assertNextWordConsistency("SELECT #{p} FROM #tmp WHERE c #>> d");
    }

    @Test
    public void testNextWord_hashPrefixedIdentifierAfterComments() {
        assertEquals("#tmp", SqlParser.nextWord("FROM /* temp */ #tmp", "FROM".length()));
        assertEquals("#tmp", SqlParser.nextWord("FROM/* temp */#tmp", "FROM".length()));
        assertEquals("#tmp", SqlParser.nextWord("FROM -- temp\n#tmp", "FROM".length()));
    }

    @Test
    public void testIsSeparator_hashPrefixedIdentifierAfterComments() {
        String sql = "FROM /* temp */ #tmp";
        int hashIndex = sql.indexOf('#');

        assertFalse(SqlParser.isSeparator(sql, sql.length(), hashIndex, '#'));

        sql = "FROM -- temp\n#tmp";
        hashIndex = sql.indexOf('#');

        assertFalse(SqlParser.isSeparator(sql, sql.length(), hashIndex, '#'));
    }

    // ----------------------------------------------------------------------------------------------
    // indexOfWord -- case sensitivity on composite keywords
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfWord_caseSensitiveComposite() {
        String sql = "SELECT * FROM a LEFT JOIN b ON a.id = b.id";

        assertEquals(sql.indexOf("LEFT"), SqlParser.indexOfWord(sql, "LEFT JOIN", 0, true));
        // Wrong case must not match when caseSensitive=true.
        assertEquals(-1, SqlParser.indexOfWord(sql, "left join", 0, true));
        // ...but matches when caseSensitive=false.
        assertEquals(sql.indexOf("LEFT"), SqlParser.indexOfWord(sql, "left join", 0, false));
    }

    // ----------------------------------------------------------------------------------------------
    // isReadOnlyQuery -- keyword hidden by quote escapes / locking reads
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsReadOnlyQuery_keywordHiddenByDoubledQuoteEscape() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t WHERE x = 'can''t DELETE this'"));
    }

    @Test
    public void testIsReadOnlyQuery_keywordHiddenByBackslashEscape() {
        // Java string -> SQL: SELECT * FROM t WHERE x = 'a\' DELETE'
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t WHERE x = 'a\\' DELETE'"));
    }

    @Test
    public void testIsReadOnlyQuery_forUpdateIsTreatedAsReadOnly() {
        // Documents current behavior: the UPDATE in "FOR UPDATE" is not a top-level statement verb
        // (it is not preceded by ';' or a CTE boundary), so a locking read is reported as read-only.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM t WHERE id = 1 FOR UPDATE"));
    }

    @Test
    public void testIsReadOnlyQuery_topLevelMergeAfterSemicolonRejected() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
    }

    @Test
    public void testIsReadOnlyQuery_mutationAfterSemicolonWithCteRejected() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; WITH doomed AS (SELECT 1) DELETE FROM t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; WITH changed AS (UPDATE t SET x = 1 RETURNING *) SELECT * FROM changed"));
    }

    @Test
    public void testIsReadOnlyQuery_parenthesizedMutationAfterSemicolonRejected() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; (DELETE FROM t)"));
    }

    @Test
    public void testIsReadOnlyQuery_identifierContainingKeywordIsReadOnly() {
        // readKeyword reads whole identifiers, so these column/table names are NOT mutation keywords.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM update_log"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT delete_flag, merge_count FROM t"));
    }

    // ----------------------------------------------------------------------------------------------
    // isNoUpdateQuery -- ON CONFLICT shapes and plain-insert variants
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsNoUpdateQuery_onConflictNamedConstraintDoUpdateRejected() {
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT ON CONSTRAINT uq DO UPDATE SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_mutationAfterSemicolonWithCteRejected() {
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; WITH doomed AS (SELECT 1) UPDATE t SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictNoTargetDoUpdateRejected() {
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT DO UPDATE SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictNamedConstraintDoNothingAllowed() {
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT ON CONSTRAINT uq DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictPredicateDoTokenWithPunctuationAllowed() {
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) WHERE do = update DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_plainInsertWithReturningAllowed() {
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t (a) VALUES (1) RETURNING id"));
    }

    // ----------------------------------------------------------------------------------------------
    // isInsertOrReplaceQuery -- leading line/hash comments
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsInsertOrReplaceQuery_leadingLineAndHashComments() {
        assertTrue(SqlParser.isInsertOrReplaceQuery("-- c\nINSERT OR REPLACE INTO t VALUES (1)"));
        assertTrue(SqlParser.isInsertOrReplaceQuery("# c\nINSERT OR REPLACE INTO t VALUES (1)"));
    }

    // ----------------------------------------------------------------------------------------------
    // isSelectQuery -- mixed leading comments and parentheses
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsSelectQuery_mixedLeadingCommentsAndParens() {
        assertTrue(SqlParser.isSelectQuery("/* a */ -- b\n ( /* c */ SELECT 1 )"));
        assertTrue(SqlParser.isSelectQuery("  (  ( select 1 )  )  "));
    }

    @Test
    public void testIsSelectQuery_selectIntoBracketTargetIsNotReadOnly() {
        // SQL Server SELECT ... INTO with a bracket-quoted target table.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a, b INTO [new table] FROM t"));
    }

    // ----------------------------------------------------------------------------------------------
    // keyword detection ignores bracket-quoted identifiers, quoted literals, and finds post-CTE
    // mutations after a statement separator (exercises the skip-bracket / token-sequence / WITH
    // look-ahead branches of containsQueryKeyword / containsTokenSequence)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsReadOnlyQuery_mutationKeywordInBracketIdentifierIsIgnored() {
        // SQL Server bracket-quoted identifiers named like keywords must not count as mutations.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT [delete], [update] FROM [merge]"));
    }

    @Test
    public void testIsNoUpdateQuery_overwriteInsideStringLiteralIsAllowed() {
        // "OVERWRITE" appears only inside a string literal, so it is not an INSERT OVERWRITE clause.
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES ('OVERWRITE')"));
    }

    @Test
    public void testIsReadOnlyQuery_postCteInsertAfterSemicolonRejected() {
        // The WITH look-ahead must detect a post-CTE INSERT that follows a leading SELECT + ';'.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; WITH x AS (SELECT 1) INSERT INTO t VALUES (1)"));
    }
}
