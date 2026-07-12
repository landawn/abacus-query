package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SqlParserTest extends TestBase {
    @Test
    public void testParseSimpleSelect() {
        String sql = "SELECT * FROM users";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
    }

    @Test
    public void testParseSelectWithColumns() {
        String sql = "SELECT id, name, email FROM users";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("id"));
        assertTrue(words.contains(","));
        assertTrue(words.contains("name"));
        assertTrue(words.contains("email"));
    }

    @Test
    public void testParseWithWhere() {
        String sql = "SELECT * FROM users WHERE age > 18";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains("age"));
        assertTrue(words.contains(">"));
        assertTrue(words.contains("18"));
    }

    @Test
    public void testParseWithJoin() {
        String sql = "SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("LEFT"));
        assertTrue(words.contains("JOIN"));
        assertTrue(words.contains("ON"));
    }

    @Test
    public void testParseWithOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND status = 'active'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains(">="));
        assertTrue(words.contains("AND"));
        assertTrue(words.contains("="));
    }

    @Test
    public void testParseWithQuotedString() {
        String sql = "SELECT * FROM users WHERE name = 'John Doe'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.stream().anyMatch(w -> w.contains("John Doe")));
    }

    @Test
    public void testParseWithDoubleQuotes() {
        String sql = "SELECT \"first name\" FROM users";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.stream().anyMatch(w -> w.contains("first name")));
    }

    @Test
    public void testParseWithParentheses() {
        String sql = "SELECT * FROM users WHERE (age > 18 AND status = 'active')";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("("));
        assertTrue(words.contains(")"));
    }

    @Test
    public void testParseWithGroupBy() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("GROUP"));
        assertTrue(words.contains("BY"));
    }

    @Test
    public void testParseWithOrderBy() {
        String sql = "SELECT * FROM users ORDER BY name ASC";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("ORDER"));
        assertTrue(words.contains("BY"));
    }

    @Test
    public void testParseWithMultipleOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND age <= 65";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains(">="));
        assertTrue(words.contains("<="));
    }

    @Test
    public void testParseWithInClause() {
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("IN"));
        assertTrue(words.contains("("));
        assertTrue(words.contains(","));
    }

    @Test
    public void testParseWithDoubleHashOperator() {
        List<String> words = SqlParser.parse("SELECT 1 ## 2");
        assertTrue(words.contains("##"));
    }

    @Test
    public void testParseWithPostgresJsonQuestionAndOperator() {
        List<String> words = SqlParser.parse("SELECT * FROM events WHERE payload ?& array['a']");
        assertTrue(words.contains("?&"), "PostgreSQL JSONB ?& operator must stay a single token: " + words);
        assertFalse(words.contains("?"), "PostgreSQL JSONB ?& operator must not be split into a JDBC placeholder: " + words);
    }

    @Test
    public void testParseEmptyString() {
        String sql = "";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.isEmpty());
    }

    @Test
    public void testParseWithHaving() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("HAVING"));
    }

    @Test
    public void testParseWithSubquery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("IN"));
    }

    @Test
    public void testParseWithUnion() {
        String sql = "SELECT id FROM users UNION SELECT id FROM accounts";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("UNION"));
    }

    @Test
    public void testParseWithUnionAll() {
        String sql = "SELECT id FROM users UNION ALL SELECT id FROM accounts";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("UNION"));
        assertTrue(words.contains("ALL"));
    }

    @Test
    public void testParseWithNotEquals() {
        String sql = "SELECT * FROM users WHERE status != 'deleted'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("!="));
    }

    @Test
    public void testParseWithNotEqualsAlternative() {
        String sql = "SELECT * FROM users WHERE status <> 'deleted'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("<>"));
    }

    @Test
    public void testParseWithBetween() {
        String sql = "SELECT * FROM users WHERE age BETWEEN 18 AND 65";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("BETWEEN"));
        assertTrue(words.contains("AND"));
    }

    @Test
    public void testParseWithLike() {
        String sql = "SELECT * FROM users WHERE name LIKE 'John%'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("LIKE"));
    }

    @Test
    public void testParseWithNotLike() {
        String sql = "SELECT * FROM users WHERE name NOT LIKE 'John%'";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("NOT"));
        assertTrue(words.contains("LIKE"));
    }

    @Test
    public void testParseWithForUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("FOR"));
        assertTrue(words.contains("UPDATE"));
    }

    @Test
    public void testParseComplexQuery() {
        String sql = "SELECT u.id, u.name, COUNT(o.id) as order_count " + "FROM users u " + "LEFT JOIN orders o ON u.id = o.user_id "
                + "WHERE u.status = 'active' AND u.created_date > '2020-01-01' " + "GROUP BY u.id, u.name " + "HAVING COUNT(o.id) > 5 "
                + "ORDER BY order_count DESC " + "LIMIT 10";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.size() > 10);
    }

    @Test
    public void testParseWithArithmeticOperators() {
        String sql = "SELECT price * quantity + tax - discount FROM orders";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("*"));
        assertTrue(words.contains("+"));
        assertTrue(words.contains("-"));
    }

    @Test
    public void testParseWithModuloOperator() {
        String sql = "SELECT id % 10 FROM users";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("%"));
    }

    @Test
    public void testParseWithBitwiseOperators() {
        String sql = "SELECT value & mask FROM data";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("&"));
    }

    @Test
    public void testIndexWord() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfWord(sql, "WHERE", 0, false);
        assertTrue(index >= 0);
        assertEquals("WHERE", sql.substring(index, index + 5));
    }

    @Test
    public void testIndexWordCaseInsensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfWord(sql, "where", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexWordCaseSensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfWord(sql, "where", 0, true);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexWordNotFound() {
        String sql = "SELECT * FROM users";
        int index = SqlParser.indexOfWord(sql, "WHERE", 0, false);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexWordComposite() {
        String sql = "SELECT * FROM users ORDER BY name";
        int index = SqlParser.indexOfWord(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexWordFromPosition() {
        String sql = "SELECT * FROM users WHERE age > 18 AND status = 'active'";
        int firstAnd = SqlParser.indexOfWord(sql, "AND", 0, false);
        assertTrue(firstAnd > 0);
    }

    @Test
    public void testIndexWordWithNegativeFromIndex() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfWord(sql, "SELECT", -5, false);
        assertEquals(0, index);
    }

    @Test
    public void testNextWord() {
        String sql = "SELECT * FROM users";
        String word = SqlParser.nextWord(sql, 7);
        assertNotNull(word);
        assertEquals("*", word);
    }

    @Test
    public void testNextWordSkipsWhitespace() {
        String sql = "SELECT    *    FROM users";
        String word = SqlParser.nextWord(sql, 6);
        assertNotNull(word);
        assertEquals("*", word);
    }

    @Test
    public void testNextWordEmptyString() {
        String sql = "SELECT *";
        String word = SqlParser.nextWord(sql, sql.length());
        assertEquals("", word);
    }

    @Test
    public void testNextWordWithOperator() {
        String sql = "WHERE age >= 18";
        String word = SqlParser.nextWord(sql, 10);
        assertNotNull(word);
        assertTrue(word.equals(">=") || word.equals("18"));
    }

    @Test
    public void testNextWordWithNegativeFromIndex() {
        String sql = "SELECT * FROM users";
        String word = SqlParser.nextWord(sql, -3);
        assertEquals("SELECT", word);
    }

    @Test
    public void testRegisterSeparatorChar() {
        SqlParser.registerSeparator('$');
        List<String> words = SqlParser.parse("SELECT$FROM$users");
        assertEquals(Arrays.asList("SELECT", "$", "FROM", "$", "users"), words);
    }

    @Test
    public void testRegisterSeparatorString() {
        SqlParser.registerSeparator(":::");
        List<String> words = SqlParser.parse("SELECT:::FROM:::users");
        assertEquals(Arrays.asList("SELECT", ":::", "FROM", ":::", "users"), words);
    }

    @Test
    public void testRegisterSeparatorNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlParser.registerSeparator((String) null);
        });
    }

    @Test
    public void testIsSeparator() {
        String sql = "SELECT * FROM users";
        assertTrue(SqlParser.isSeparator(sql, sql.length(), 6, ' '));
        assertTrue(SqlParser.isSeparator(sql, sql.length(), 7, '*'));
        assertFalse(SqlParser.isSeparator(sql, sql.length(), 0, 'S'));
    }

    @Test
    public void testIsSeparatorHash() {
        String sql = "SELECT * FROM users WHERE id = #{userId}";
        assertTrue(SqlParser.isSeparator(sql, sql.length(), 35, '#'));
    }

    @Test
    public void testIsSeparatorHashForTempTable() {
        String sql = "SELECT * FROM #tmp";
        int hashIndex = sql.indexOf('#');
        assertFalse(SqlParser.isSeparator(sql, sql.length(), hashIndex, '#'));
    }

    @Test
    public void testIsFunctionName() {
        List<String> words = SqlParser.parse("SELECT COUNT(*) FROM users");
        int countIndex = -1;
        for (int i = 0; i < words.size(); i++) {
            if ("COUNT".equals(words.get(i))) {
                countIndex = i;
                break;
            }
        }
        assertTrue(countIndex >= 0);
        assertTrue(SqlParser.isFunctionName(words, words.size(), countIndex));
    }

    @Test
    public void testIsFunctionNameNotFunction() {
        List<String> words = SqlParser.parse("SELECT name FROM users");
        int nameIndex = -1;
        for (int i = 0; i < words.size(); i++) {
            if ("name".equals(words.get(i))) {
                nameIndex = i;
                break;
            }
        }
        assertTrue(nameIndex >= 0);
        assertFalse(SqlParser.isFunctionName(words, words.size(), nameIndex));
    }

    @Test
    public void testIndexWordMultipleOccurrences() {
        String sql = "SELECT name FROM users WHERE name = 'John' OR name = 'Jane'";
        int firstIndex = SqlParser.indexOfWord(sql, "name", 0, false);
        int secondIndex = SqlParser.indexOfWord(sql, "name", firstIndex + 1, false);
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex > firstIndex);
    }

    @Test
    public void testNextWordAtEnd() {
        String sql = "SELECT * FROM users";
        String word = SqlParser.nextWord(sql, sql.length() - 1);
        assertNotNull(word);
    }

    @Test
    public void testParseSimpleSQL() {
        // Test basic SELECT statement
        String sql = "SELECT * FROM users";
        List<String> words = SqlParser.parse(sql);

        assertNotNull(words);
        assertEquals(Arrays.asList("SELECT", " ", "*", " ", "FROM", " ", "users"), words);

        // Test with WHERE clause
        sql = "SELECT name, age FROM users WHERE age > 25";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("name"));
        assertTrue(words.contains(","));
        assertTrue(words.contains("age"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains(">"));
        assertTrue(words.contains("25"));
    }

    @Test
    public void testParseQuotedIdentifiers() {
        // Test single quotes
        String sql = "SELECT * FROM users WHERE name = 'John Doe'";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("'John Doe'"));

        // Test double quotes
        sql = "SELECT * FROM users WHERE name = \"John Doe\"";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("\"John Doe\""));

        // Test escaped quotes
        sql = "SELECT * FROM users WHERE name = 'John\\'s'";
        words = SqlParser.parse(sql);

        assertTrue(words.stream().anyMatch(w -> w.contains("John\\'s")));

        // Test SQL-standard escaped quote by doubling delimiter
        sql = "SELECT * FROM users WHERE name = 'it''s me'";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("'it''s me'"));
    }

    @Test
    public void testParseComments() {
        // Test single-line comments
        String sql = "SELECT * FROM users -- This is a comment\nWHERE id = 1";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("WHERE"));
        assertFalse(words.stream().anyMatch(w -> w.contains("This is a comment")));

        // Test multi-line comments
        sql = "SELECT * /* multi-line\ncomment */ FROM users";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertFalse(words.stream().anyMatch(w -> w.contains("multi-line")));
    }

    @Test
    public void testParseHashComments() {
        String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?\nAND status = :status";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("AND"));
        assertFalse(words.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(words.contains("?"));
    }

    @Test
    public void testParseHashCommentsWithoutSpace() {
        String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?\nAND status = :status";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("AND"));
        assertFalse(words.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(words.contains("?"));
    }

    @Test
    public void testParseHashCommentAtLineStartWithoutSpace() {
        String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains(":userId"));
        assertFalse(words.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(words.contains("?"));
    }

    @Test
    public void testParseHashTempTableIsNotComment() {
        String sql = "SELECT * FROM #tmp WHERE id = :id";
        List<String> words = SqlParser.parse(sql);
        String joined = String.join("", words);

        assertTrue(joined.contains("#tmp"));
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains(":id"));
    }

    @Test
    public void testParseHashJsonOperatorsAreNotComments() {
        String sql = "SELECT payload #> '{meta,status}' AS status_json FROM docs WHERE payload #>> '{meta,status}' = 'active'";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("#>"));
        assertTrue(words.contains("#>>"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains("'active'"));
    }

    @Test
    public void testParseOperators() {
        // Test various operators
        String sql = "SELECT * FROM users WHERE age >= 18 AND status != 'inactive' OR role IN ('admin', 'user')";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains(">="));
        assertTrue(words.contains("!="));
        assertTrue(words.contains("IN"));
        assertTrue(words.contains("AND"));
        assertTrue(words.contains("OR"));

        // Test more operators
        sql = "SELECT * WHERE a = b AND c <> d AND e <= f AND g < h AND i > j AND k >> l";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("="));
        // assertTrue(words.contains("<>"));
        assertTrue(words.contains("<="));
        assertTrue(words.contains("<"));
        assertTrue(words.contains(">"));
        assertTrue(words.contains(">>"));
    }

    @Test
    public void testParseComplexOperators() {
        // Test multi-character operators
        String sql = "SELECT * WHERE a != b AND c <=> d AND e || f AND g && h";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("!="));
        assertTrue(words.contains("<=>"));
        assertTrue(words.contains("||"));
        assertTrue(words.contains("&&"));

        // Test assignment operators
        sql = "UPDATE table SET a += 1, b -= 2, c *= 3, d /= 4";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("+="));
        assertTrue(words.contains("-="));
        assertTrue(words.contains("*="));
        assertTrue(words.contains("/="));
    }

    @Test
    public void testParseMyBatisParameters() {
        // Test MyBatis/iBatis parameter syntax
        String sql = "SELECT * FROM users WHERE id = #{userId} AND name = #{userName}";
        List<String> words = SqlParser.parse(sql);

        // # should not be separated when followed by {
        assertTrue(words.contains("#{userId}"));
        assertTrue(words.contains("#{userName}"));
    }

    @Test
    public void testParseWhitespace() {
        // Test multiple spaces, tabs, and newlines
        String sql = "SELECT   *\t\tFROM\nusers\r\nWHERE\t id = 1";
        List<String> words = SqlParser.parse(sql);

        // Should normalize whitespace to single spaces
        assertEquals("SELECT", words.get(0));
        assertEquals(" ", words.get(1));
        assertEquals("*", words.get(2));
        assertEquals(" ", words.get(3));
        assertEquals("FROM", words.get(4));
        assertEquals(" ", words.get(5));
        assertEquals("users", words.get(6));
        assertEquals(" ", words.get(7));
        assertEquals("WHERE", words.get(8));
    }

    @Test
    public void testParseComplexSQL() {
        String sql = "WITH RECURSIVE cte AS (" + "SELECT id, parent_id, name FROM categories WHERE parent_id IS NULL " + "UNION ALL "
                + "SELECT c.id, c.parent_id, c.name FROM categories c " + "INNER JOIN cte ON c.parent_id = cte.id" + ") SELECT * FROM cte ORDER BY name";

        List<String> words = SqlParser.parse(sql);

        // Verify key SQL keywords are parsed
        assertTrue(words.contains("WITH"));
        assertTrue(words.contains("RECURSIVE"));
        assertFalse(words.contains("UNION ALL"));
        assertFalse(words.contains("INNER JOIN"));
        assertFalse(words.contains("ORDER BY"));
        assertFalse(words.contains("IS NULL"));
    }

    @Test
    public void testParseSpecialOperators() {
        // Test various special operators from the separators set
        String sql = "SELECT * WHERE a ~= b AND c ^= d AND e :: text AND f @> g";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("~="));
        assertTrue(words.contains("^="));
        assertTrue(words.contains("::"));
        assertTrue(words.contains("@>"));
    }

    @Test
    public void testIndexWordQuoted() {
        // Test word inside quotes
        String sql = "SELECT * FROM users WHERE name = 'SELECT something'";

        // Should find the first SELECT, not the one in quotes
        assertEquals(0, SqlParser.indexOfWord(sql, "SELECT", 0, false));

        // Should find the quoted SELECT when searching from appropriate position
        int firstSelect = SqlParser.indexOfWord(sql, "SELECT", 0, false);
        int secondSelect = SqlParser.indexOfWord(sql, "SELECT", firstSelect + 1, false);
        assertEquals(-1, secondSelect);

        // Test escaped quote in SQL-standard form (two single quotes) inside a string literal
        sql = "SELECT * FROM users WHERE note = 'it''s SELECT here'";
        assertEquals(-1, SqlParser.indexOfWord(sql, "SELECT", 1, false));
    }

    @Test
    public void testIndexWordOperators() {
        String sql = "SELECT * WHERE a = b AND c != d OR e >= f";

        // Test finding operators
        assertEquals(17, SqlParser.indexOfWord(sql, "=", 0, false));
        assertEquals(27, SqlParser.indexOfWord(sql, "!=", 0, false));
        assertEquals(sql.indexOf(">="), SqlParser.indexOfWord(sql, ">=", 0, false));
    }

    @Test
    public void testNextWordQuoted() {
        String sql = "SELECT 'quoted string' FROM table";

        // Test getting quoted string
        assertEquals("'quoted string'", SqlParser.nextWord(sql, 6));

        // Test with double quotes
        sql = "SELECT \"quoted string\" FROM table";
        assertEquals("\"quoted string\"", SqlParser.nextWord(sql, 6));

        // Test SQL-standard escaped quote by doubling delimiter
        sql = "SELECT 'it''s done' FROM table";
        assertEquals("'it''s done'", SqlParser.nextWord(sql, 6));
    }

    @Test
    public void testNextWordOperators() {
        String sql = "WHERE a >= b AND c != d";

        assertEquals("a", SqlParser.nextWord(sql, 5));
        assertEquals(">=", SqlParser.nextWord(sql, 7));
        assertEquals("b", SqlParser.nextWord(sql, 10));
        assertEquals("AND", SqlParser.nextWord(sql, 12));
        assertEquals("!=", SqlParser.nextWord(sql, 19));
    }

    @Test
    public void testRegisterSeparatorStringWithNewLeadingChar() {
        SqlParser.registerSeparator("$$");

        List<String> words = SqlParser.parse("SELECT$$FROM$$users");
        assertEquals(Arrays.asList("SELECT", "$$", "FROM", "$$", "users"), words);
    }

    @Test
    public void testRegisterLongSeparatorString() {
        SqlParser.registerSeparator("~~~~");

        List<String> words = SqlParser.parse("SELECT~~~~FROM~~~~users");
        assertEquals(Arrays.asList("SELECT", "~~~~", "FROM", "~~~~", "users"), words);
    }

    @Test
    public void testIsFunctionNameWithSpaces() {
        List<String> words = SqlParser.parse("SELECT COUNT ( * ), SUM  (  amount  ) FROM sales");

        // COUNT is still a function even with spaces before (
        int countIndex = words.indexOf("COUNT");
        assertTrue(SqlParser.isFunctionName(words, words.size(), countIndex));

        // SUM is still a function even with multiple spaces
        int sumIndex = words.indexOf("SUM");
        assertTrue(SqlParser.isFunctionName(words, words.size(), sumIndex));
    }

    // Cover comment-retention and hash-prefixed temp table parsing branches.
    @Test
    public void testParse_KeepCommentsDirective() {
        String sql = "-- Keep comments\nSELECT /* keep me */ name FROM users";

        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("/* keep me */"));
    }

    @Test
    public void testParse_HashPrefixedIdentifier() {
        String sql = "SELECT * FROM #temp_users WHERE id = 1";

        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("#temp_users"));
        assertTrue(words.contains("WHERE"));
    }

    @Test
    public void testIndexOfWord_HashPrefixedIdentifier() {
        String sql = "SELECT * FROM #temp_users WHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", 0, false));
    }

    @Test
    public void testNextWord_HashPrefixedIdentifier() {
        String sql = "FROM #temp_users";

        assertEquals("#temp_users", SqlParser.nextWord(sql, 4));
    }

    @Test
    public void testNextWord_SkipsBlockComment() {
        String sql = "SELECT /* hidden */ name FROM users";

        assertEquals("name", SqlParser.nextWord(sql, 6));
    }

    // Exercise uncovered parser branches around inline comments, quoted tokens, and backtracking.
    @Test
    public void testParse_InlineDashCommentAfterToken() {
        String sql = "SELECT col-- hidden\nFROM users";

        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("col"));
        assertTrue(words.contains("FROM"));
        assertFalse(words.stream().anyMatch(e -> e.contains("hidden")));
    }

    @Test
    public void testParse_InlineBlockCommentAfterToken() {
        String sql = "SELECT col/* hidden */FROM users";

        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("col"));
        assertTrue(words.contains("FROM"));
        assertEquals("SELECT col FROM users", String.join("", words));
        assertFalse(words.stream().anyMatch(e -> e.contains("hidden")));
    }

    @Test
    public void testIndexOfWord_QuotedTokenWithBackslashEscape() {
        String sql = "SELECT 'John\\'s' FROM users";

        assertEquals(sql.indexOf("'John\\'s'"), SqlParser.indexOfWord(sql, "'John\\'s'", 0, true));
    }

    @Test
    public void testIndexOfWord_DashCommentAfterMismatchedToken() {
        String sql = "value-- hidden\nWHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfWord_HashCommentAfterMismatchedToken() {
        String sql = "value# hidden\nWHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfWord_BlockCommentAfterMismatchedToken() {
        String sql = "value/* hidden */WHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfWord_FromIndexInsideQuotedLiteralSkipsToLaterToken() {
        String sql = "SELECT 'hidden WHERE token' FROM users WHERE id = 1";
        int fromIndex = sql.indexOf("WHERE token");

        assertEquals(sql.lastIndexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", fromIndex, false));
    }

    @Test
    public void testIndexOfWord_FromIndexInsideBlockCommentSkipsToLaterToken() {
        String sql = "SELECT /* hidden WHERE token */ id FROM users WHERE id = 1";
        int fromIndex = sql.indexOf("WHERE token");

        assertEquals(sql.lastIndexOf("WHERE"), SqlParser.indexOfWord(sql, "WHERE", fromIndex, false));
    }

    @Test
    public void testIndexOfWord_TrailingWordAtEnd() {
        String sql = "SELECT FROM";

        assertEquals(sql.length() - "FROM".length(), SqlParser.indexOfWord(sql, "FROM", 0, false));
    }

    @Test
    public void testIndexOfWord_CompositeWordAfterFalseStarts() {
        String sql = "SELECT * FROM users ORDER name ORDER age";

        assertEquals(-1, SqlParser.indexOfWord(sql, "ORDER BY", 0, false));
    }

    @Test
    public void testNextWord_QuotedTokenWithBackslashEscape() {
        String sql = "SELECT 'John\\'s' FROM users";

        assertEquals("'John\\'s'", SqlParser.nextWord(sql, 6));
    }

    @Test
    public void testNextWord_SkipsDashCommentAtStart() {
        String sql = "-- hidden\nvalue";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testNextWord_DashCommentAfterToken() {
        String sql = "value-- hidden\nother";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testNextWord_SkipsHashCommentAtStart() {
        String sql = "# hidden\nvalue";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testNextWord_HashCommentAfterToken() {
        String sql = "value# hidden\nother";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testNextWord_BlockCommentAfterToken() {
        String sql = "value/* hidden */other";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testNextWord_LeadingNewlineWhitespace() {
        String sql = " \n\tvalue";

        assertEquals("value", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testIsSeparator_HashAtEnd() {
        String sql = "SELECT #";
        int index = sql.length() - 1;

        assertTrue(SqlParser.isSeparator(sql, sql.length(), index, '#'));
    }

    @Test
    public void testIsSeparator_HashAfterNonIdentifierContext() {
        String sql = "SELECT (#tmp)";
        int index = sql.indexOf('#');

        assertTrue(SqlParser.isSeparator(sql, sql.length(), index, '#'));
    }

    @Test
    public void testSqlParser_classLevelExample() {
        String sql = "SELECT * FROM users WHERE age > 25 ORDER BY name";
        List<String> words = SqlParser.parse(sql);
        assertNotNull(words);
        assertFalse(words.isEmpty());
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
        assertTrue(words.contains("WHERE"));
        // Javadoc shows "ORDER" and "BY" as separate tokens in parse() output
        assertTrue(words.contains("ORDER"));
        assertTrue(words.contains("BY"));
        assertTrue(words.contains("name"));
    }

    @Test
    public void testSqlParser_parse() {
        List<String> words = SqlParser.parse("SELECT name, age FROM users WHERE age >= 18");
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("name"));
        assertTrue(words.contains(","));
        assertTrue(words.contains("age"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains(">="));
        assertTrue(words.contains("18"));
    }

    @Test
    public void testSqlParser_indexOfWord() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
        int index = SqlParser.indexOfWord(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0, "ORDER BY should be found in the SQL");
        int whereIndex = SqlParser.indexOfWord(sql, "WHERE", 0, false);
        assertTrue(whereIndex >= 0, "WHERE should be found in the SQL");
        assertTrue(whereIndex < index, "WHERE should come before ORDER BY");
    }

    @Test
    public void testSqlParser_nextWord() {
        String sql = "SELECT   name,   age FROM users";
        String word1 = SqlParser.nextWord(sql, 6);
        assertEquals("name", word1);
        String word2 = SqlParser.nextWord(sql, 13);
        assertEquals(",", word2);
        String word3 = SqlParser.nextWord(sql, 14);
        assertEquals("age", word3);
    }

    @Test
    public void testSqlParser_registerSeparatorChar() {
        SqlParser.registerSeparator('$');
        List<String> words = SqlParser.parse("SELECT$FROM$users");
        assertNotNull(words);
        assertTrue(words.contains("$"));
    }

    @Test
    public void testSqlParser_registerSeparatorString() {
        SqlParser.registerSeparator("<=>");
        SqlParser.registerSeparator("::");
        List<String> words = SqlParser.parse("SELECT <=> value :: text");
        assertTrue(words.contains("<=>"));
        assertTrue(words.contains("::"));
    }

    @Test
    public void testRegisterSeparatorRejectsEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> SqlParser.registerSeparator(""));
    }

    @Test
    public void testIndexOfWord_compositeKeyword_isNotNullWithBlockCommentBetweenSubwords() {
        String sql = "SELECT col FROM t WHERE col IS /* remark */ NOT /* remark2 */ NULL";
        int idx = SqlParser.indexOfWord(sql, "IS NOT NULL", 0, false);
        assertTrue(idx >= 0, "indexOfWord should find IS NOT NULL even when block comments appear between subwords");
        assertEquals(sql.indexOf("IS"), idx);
    }

    @Test
    public void testIndexOfWord_compositeKeyword_orderByWithBlockComment() {
        String sql = "SELECT * FROM t ORDER /* comment */ BY id";
        int idx = SqlParser.indexOfWord(sql, "ORDER BY", 0, false);
        assertTrue(idx >= 0, "indexOfWord should find ORDER BY even with a block comment between the keywords");
    }

    @Test
    public void testParseBackslashEscapedQuote() {
        // Backslash-escaped single quote (MySQL style) should keep the string intact.
        String sql = "SELECT * FROM t WHERE x = 'O\\'Brien'";
        List<String> words = SqlParser.parse(sql);
        assertTrue(words.stream().anyMatch(w -> w.equals("'O\\'Brien'")),
                "Expected the backslash-escaped quote to keep the literal as a single token, got: " + words);
    }

    @Test
    public void testParseDoubledQuoteEscape() {
        // SQL-standard doubled-quote escape.
        String sql = "SELECT 'O''Brien' FROM dual";
        List<String> words = SqlParser.parse(sql);
        assertTrue(words.contains("'O''Brien'"));
    }

    @Test
    public void testIndexOfWordIsNotMatchingSubstring() {
        // indexOfWord must respect word boundaries: searching for "JOIN" must NOT match inside RIGHTJOIN.
        String sql = "SELECT * FROM t1 RIGHTJOIN t2 ON ...";
        int idx = SqlParser.indexOfWord(sql, "JOIN", 0, false);
        // The only token in this SQL containing JOIN is "RIGHTJOIN" itself, which is not a separate word.
        assertEquals(-1, idx, "indexOfWord('JOIN') must not match the substring inside identifier RIGHTJOIN");
    }

    @Test
    public void testParseBackslashEscapedQuoteThenClosingQuote() {
        // Regression: a backslash-escaped quote (\') immediately followed by the real closing
        // quote must terminate the string literal. Previously the doubled-quote ('') check won
        // over the pending backslash escape, so the literal stayed open and swallowed the rest
        // of the SQL (FROM/t never became separate tokens).
        String sql = "SELECT 'a\\'' FROM t";
        List<String> words = SqlParser.parse(sql);
        assertTrue(words.contains("'a\\''"), "Expected the escaped-quote-then-closing-quote literal as a single token, got: " + words);
        assertTrue(words.contains("FROM"), "FROM must be parsed as a separate token (string terminated), got: " + words);
        assertTrue(words.contains("t"), "table name must be parsed as a separate token, got: " + words);
    }

    @Test
    public void testNextWordBackslashEscapedQuoteThenClosingQuote() {
        // nextWord must return the whole terminated literal '...\'' and stop at it.
        String sql = "'a\\'' rest";
        assertEquals("'a\\''", SqlParser.nextWord(sql, 0));
    }

    @Test
    public void testIndexOfWordAfterBackslashEscapedQuoteThenClosingQuote() {
        // The escaped-then-closing quote terminates the literal, so a keyword after it is found.
        String sql = "WHERE x = 'a\\'' AND y = 1";
        int idx = SqlParser.indexOfWord(sql, "AND", 0, false);
        assertTrue(idx > 0, "AND after a terminated escaped-quote literal must be found, got index: " + idx);
    }

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
        assertNextWordConsistency("SELECT [a]]b], [--] FROM [t]");
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
    // unregisterSeparator(char)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testUnregisterSeparatorChar_removesCharRegisteredSeparator() {
        SqlParser.registerSeparator('$');
        assertEquals(Arrays.asList("a", "$", "b"), SqlParser.parse("a$b"));

        SqlParser.unregisterSeparator('$');
        assertEquals(Arrays.asList("a$b"), SqlParser.parse("a$b"));
    }

    @Test
    public void testUnregisterSeparatorChar_removesStringRegisteredSingleChar() {
        // registerSeparator(String) dual-registers single-char separators; the char overload must undo both forms.
        SqlParser.registerSeparator("$");
        assertEquals(Arrays.asList("a", "$", "b"), SqlParser.parse("a$b"));

        SqlParser.unregisterSeparator('$');
        assertEquals(Arrays.asList("a$b"), SqlParser.parse("a$b"));
    }

    @Test
    public void testUnregisterSeparatorChar_unknownIsNoOp() {
        SqlParser.unregisterSeparator('¤');

        assertEquals(Arrays.asList("SELECT", " ", "*", " ", "FROM", " ", "t"), SqlParser.parse("SELECT * FROM t"));
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
    // isUpdateQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsUpdateQuery_basic() {
        assertTrue(SqlParser.isUpdateQuery("UPDATE users SET name = 'John'"));
        assertTrue(SqlParser.isUpdateQuery("update products set price = 9.99 where id = 1"));
        assertTrue(SqlParser.isUpdateQuery("   UPDATE orders SET status = 'shipped'"));
        assertFalse(SqlParser.isUpdateQuery("SELECT * FROM users"));
        assertFalse(SqlParser.isUpdateQuery("INSERT INTO users VALUES (1, 'John')"));
        assertFalse(SqlParser.isUpdateQuery("DELETE FROM users"));
    }

    @Test
    public void testIsUpdateQuery_leadingCommentsAndParens() {
        assertTrue(SqlParser.isUpdateQuery("-- a leading comment\nUPDATE t SET a = 1"));
        assertTrue(SqlParser.isUpdateQuery("/* block */ UPDATE t SET a = 1"));
        assertTrue(SqlParser.isUpdateQuery("# hash comment\nUPDATE t SET a = 1"));
    }

    @Test
    public void testIsUpdateQuery_withCte() {
        assertTrue(SqlParser.isUpdateQuery("WITH t AS (SELECT 1) UPDATE x SET a = (SELECT 1 FROM t)"));
        assertFalse(SqlParser.isUpdateQuery("WITH t AS (SELECT 1) SELECT * FROM t"));
    }

    @Test
    public void testIsUpdateQuery_keywordPrefixIsNotAKeyword() {
        assertFalse(SqlParser.isUpdateQuery("UPDATE1 t SET a = 1"));
        assertFalse(SqlParser.isUpdateQuery("update_log t SET a = 1"));
    }

    @Test
    public void testIsUpdateQuery_nullAndEmpty() {
        assertFalse(SqlParser.isUpdateQuery(null));
        assertFalse(SqlParser.isUpdateQuery(""));
        assertFalse(SqlParser.isUpdateQuery("    "));
    }

    // ----------------------------------------------------------------------------------------------
    // isDeleteQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsDeleteQuery_basic() {
        assertTrue(SqlParser.isDeleteQuery("DELETE FROM users WHERE id = 1"));
        assertTrue(SqlParser.isDeleteQuery("delete from products where price < 1"));
        assertTrue(SqlParser.isDeleteQuery("   DELETE FROM orders"));
        assertFalse(SqlParser.isDeleteQuery("SELECT * FROM users"));
        assertFalse(SqlParser.isDeleteQuery("INSERT INTO users VALUES (1, 'John')"));
        assertFalse(SqlParser.isDeleteQuery("UPDATE users SET name = 'John'"));
    }

    @Test
    public void testIsDeleteQuery_leadingCommentsAndParens() {
        assertTrue(SqlParser.isDeleteQuery("-- a leading comment\nDELETE FROM t"));
        assertTrue(SqlParser.isDeleteQuery("/* block */ DELETE FROM t"));
        assertTrue(SqlParser.isDeleteQuery("# hash comment\nDELETE FROM t"));
    }

    @Test
    public void testIsDeleteQuery_withCte() {
        assertTrue(SqlParser.isDeleteQuery("WITH t AS (SELECT 1) DELETE FROM x WHERE id IN (SELECT 1 FROM t)"));
        assertFalse(SqlParser.isDeleteQuery("WITH t AS (SELECT 1) SELECT * FROM t"));
    }

    @Test
    public void testIsDeleteQuery_keywordPrefixIsNotAKeyword() {
        assertFalse(SqlParser.isDeleteQuery("DELETE1 FROM t"));
        assertFalse(SqlParser.isDeleteQuery("delete_log FROM t"));
    }

    @Test
    public void testIsDeleteQuery_nullAndEmpty() {
        assertFalse(SqlParser.isDeleteQuery(null));
        assertFalse(SqlParser.isDeleteQuery(""));
        assertFalse(SqlParser.isDeleteQuery("    "));
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
    public void testIsFunctionName_spaceBeforeParenthesisIsNotFunctionName() {
        List<String> words = SqlParser.parse("SELECT (1)");

        assertFalse(SqlParser.isFunctionName(words, words.size(), words.indexOf(" ")));
    }

    @Test
    public void testIsFunctionName_twoArgOverload() {
        List<String> words = SqlParser.parse("SELECT COUNT(*) FROM users");

        assertTrue(SqlParser.isFunctionName(words, 2)); // "COUNT"
        assertFalse(SqlParser.isFunctionName(words, 0)); // "SELECT"
        assertFalse(SqlParser.isFunctionName(words, -1));
        assertFalse(SqlParser.isFunctionName(words, words.size()));
        assertThrows(NullPointerException.class, () -> SqlParser.isFunctionName(null, 0));
    }

    @Test
    public void testIndexOfWord_compositeWordWithDoubledInternalSpace() {
        String sql = "A ORDER BY B";
        int singleSpaced = SqlParser.indexOfWord(sql, "ORDER BY", 0, false);

        assertEquals(2, singleSpaced);
        // A doubled internal space in the word argument must split to the same sub-words
        // (previously the empty middle sub-word could never match and was even cached).
        assertEquals(singleSpaced, SqlParser.indexOfWord(sql, "ORDER  BY", 0, false));
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

    @Test
    public void testParse_hashCommentAfterHashLikeTokenIsStripped() {
        List<String> afterMyBatisWords = SqlParser.parse("SELECT * FROM #{table} #comment\nWHERE id = 1");
        List<String> afterTempTableWords = SqlParser.parse("SELECT * FROM #tmp\n#comment\nWHERE id = 1");

        assertFalse(afterMyBatisWords.contains("#comment"));
        assertTrue(afterMyBatisWords.contains("WHERE"));
        assertFalse(afterTempTableWords.contains("#comment"));
        assertTrue(afterTempTableWords.contains("WHERE"));
    }

    @Test
    public void testParse_hashTempTableAfterQuotedCommentMarkers() {
        assertTrue(SqlParser.parse("SELECT '--' FROM #tmp WHERE id = 1").contains("#tmp"));
        assertTrue(SqlParser.parse("SELECT '# not comment' FROM #tmp WHERE id = 1").contains("#tmp"));
        assertTrue(SqlParser.parse("SELECT [--] FROM #tmp WHERE id = 1").contains("#tmp"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT '--' FROM #tmp; DELETE FROM users"));
    }

    @Test
    public void testParseAndNextWord_bracketQuotedIdentifierIsSingleToken() {
        final String sql = "SELECT [a]]b], [--] FROM [table]";
        final List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("[a]]b]"));
        assertTrue(words.contains("[--]"));
        assertEquals("[a]]b]", SqlParser.nextWord(sql, "SELECT".length()));
        assertEquals(sql.indexOf("[a]]b]") + "[a]]b]".length(), SqlParser.nextWordEnd(sql, "SELECT".length()));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfWord(sql, "FROM", 0, false));
    }

    @Test
    public void testParseAndNextWord_bracketIdentifierEndingInBackslashCloses() {
        // SQL Server [bracket] identifiers do not honor backslash escaping (only ]] doubles a bracket), so a
        // bracket ending in a lone backslash must still close and not swallow the rest of the statement.
        // Regression across all four tokenizer scanners (parse / nextWord / nextWordEnd / indexOfWord).
        final String sql = "SELECT [a\\] FROM t"; // the identifier is [a\]
        final List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("[a\\]"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("t"));
        assertEquals("[a\\]", SqlParser.nextWord(sql, "SELECT".length()));
        assertEquals(sql.indexOf("[a\\]") + "[a\\]".length(), SqlParser.nextWordEnd(sql, "SELECT".length()));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfWord(sql, "FROM", 0, false));

        // A Windows-path-like identifier that ends in a backslash likewise closes cleanly.
        final String pathSql = "SELECT [C:\\path\\] FROM t"; // the identifier is [C:\path\]
        assertTrue(SqlParser.parse(pathSql).contains("[C:\\path\\]"));
        assertEquals(pathSql.indexOf("FROM"), SqlParser.indexOfWord(pathSql, "FROM", 0, false));
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

    @Test
    public void testIsReadOnlyQuery_selectIntoSubstringInIdentifierIsReadOnly() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT into$ FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT $into FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT [into] FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT t.into FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT * FROM $into"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a INTO new_table FROM t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT t.from, a INTO new_table FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT t. /* c */ into FROM t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; SELECT a INTO new_table FROM t"));
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

    @Test
    public void testIsNoUpdateQuery_selectIntoSubstringInIdentifierIsAllowed() {
        assertTrue(SqlParser.isNoUpdateQuery("SELECT into$ FROM t"));
        assertTrue(SqlParser.isNoUpdateQuery("SELECT $into FROM t"));
        assertTrue(SqlParser.isNoUpdateQuery("SELECT t.into FROM t"));
        assertTrue(SqlParser.isNoUpdateQuery("SELECT * FROM $into"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT a INTO new_table FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT t.from, a INTO new_table FROM t"));
        assertTrue(SqlParser.isNoUpdateQuery("SELECT t. /* c */ into FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; SELECT a INTO new_table FROM t"));
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

    // ----------------------------------------------------------------------------------------------
    // Block comment whose body starts with '/' (e.g. "/*/x*/") must NOT end one char early: the
    // opening '*' must not be reused as the closing '*'. Regression for all four char scanners.
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_blockCommentBodyStartingWithSlashIsFullyConsumed() {
        // The whole "/*/x*/" is a single comment -> nothing is emitted.
        assertTrue(SqlParser.parse("/*/x*/").isEmpty());
        // "b" is inside an unterminated comment (opened by "/*/ b"), so only "a" + space remain.
        assertEquals(Arrays.asList("a", " "), SqlParser.parse("a /*/ b"));
        // The comment is stripped and the surrounding tokens survive (no stray "path", "*", "/").
        assertEquals("SELECT 1 FROM t", String.join("", SqlParser.parse("SELECT 1 /*/path*/ FROM t")));
    }

    @Test
    public void testParse_keepCommentsRetainsSlashBodyBlockCommentAsOneToken() {
        final List<String> words = SqlParser.parse("-- Keep comments\nSELECT /*/x*/ FROM t");

        assertTrue(words.contains("/*/x*/"), "the whole /*/x*/ must be kept as a single comment token: " + words);
    }

    @Test
    public void testNextWord_skipsBlockCommentWithSlashBody() {
        assertEquals("name", SqlParser.nextWord("/*/x*/ name", 0));
    }

    @Test
    public void testNextWordEnd_skipsBlockCommentWithSlashBody() {
        final String sql = "/*/x*/ name";

        assertEquals(sql.indexOf("name") + "name".length(), SqlParser.nextWordEnd(sql, 0));
    }

    @Test
    public void testIndexOfWord_doesNotMatchKeywordInsideSlashBodyComment() {
        // FROM is inside the "/*/FROM*/" comment; the only real keyword is the trailing SELECT.
        final String sql = "/*/FROM*/ SELECT 1";

        assertEquals(-1, SqlParser.indexOfWord(sql, "FROM", 0, false));
        assertEquals(sql.indexOf("SELECT"), SqlParser.indexOfWord(sql, "SELECT", 0, false));
    }

    // ----------------------------------------------------------------------------------------------
    // A non-comment '#' earlier on the line (MyBatis #{...}, a #>/#>> operator, or an earlier
    // #temp identifier) must NOT cause a later hash-prefixed temp table to be dropped as a comment.
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_myBatisMarkerBeforeHashTempTableKeepsTempTable() {
        assertTrue(SqlParser.parse("SELECT #{x} FROM #tmp").contains("#tmp"));
    }

    @Test
    public void testParse_jsonOperatorBeforeHashTempTableKeepsTempTable() {
        assertTrue(SqlParser.parse("SELECT a #> b FROM #tmp").contains("#tmp"));
    }

    @Test
    public void testParse_earlierHashTempTableDoesNotHideLaterHashTempTable() {
        final List<String> words = SqlParser.parse("UPDATE #t1 SET x = 1 FROM #t2");

        assertTrue(words.contains("#t1"), words.toString());
        assertTrue(words.contains("#t2"), words.toString());
    }

    @Test
    public void testParse_hashTempTableAfterBlockCommentFollowingKeyword() {
        // The keyword may be separated from the #temp identifier by a block comment.
        assertTrue(SqlParser.parse("SELECT * FROM /* c */ #tmp").contains("#tmp"));
    }

    @Test
    public void testParse_hashTempTableAfterLineCommentFollowingKeyword() {
        // ...or by a line comment ending in a newline.
        assertTrue(SqlParser.parse("SELECT * FROM -- pick one\n#tmp").contains("#tmp"));
    }

    @Test
    public void testParse_hashTempTableAfterBlockCommentContainingHashBeforeKeyword() {
        // A block comment containing '#' (or '--') BEFORE the FROM/JOIN keyword on the same line
        // must not derail the hash-prefixed-identifier heuristic: the '#' inside the block comment
        // is not a line-comment start, so the following '#tmp' temp-table identifier is kept.
        assertTrue(SqlParser.parse("/* # */ FROM #tmp WHERE id = 1").contains("#tmp"));
        assertTrue(SqlParser.parse("SELECT /* # */ FROM #tmp WHERE id = 1").contains("#tmp"));
        assertTrue(SqlParser.parse("/* -- */ FROM #tmp WHERE id = 1").contains("#tmp"));
        assertTrue(SqlParser.parse("FROM x /* # */ JOIN #tmp WHERE id = 1").contains("#tmp"));
        // The trailing WHERE clause (same line, after #tmp) must survive too.
        assertTrue(SqlParser.parse("/* # */ FROM #tmp WHERE id = 1").contains("WHERE"));
    }

    @Test
    public void testParse_realHashCommentStillStripped() {
        // A genuine MySQL '#' comment must still be stripped (the fix must not over-correct).
        final List<String> words = SqlParser.parse("SELECT 1 # trailing comment\nFROM t");

        assertTrue(words.contains("FROM"));
        assertTrue(words.stream().noneMatch(w -> w.contains("trailing")));
    }

    // ----------------------------------------------------------------------------------------------
    // SELECT ... INTO detection: only an INTO in the SELECT list (before that query's FROM) counts.
    // Table names after FROM, qualified names such as t.into, and identifiers merely containing
    // "into" do not. ('$' is an identifier char and must not spin the INTO scan.)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testSelectInto_realSelectIntoIsNotReadOnly() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * INTO newt FROM t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a, b INTO newt FROM t"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * INTO newt FROM t"));
        // Outer SELECT ... INTO even when a derived table follows.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a INTO #t1 FROM (SELECT b FROM s) x"));
    }

    @Test
    public void testSelectInto_intoAfterFromIsReadOnly() {
        // A table named like the keyword, appearing after FROM, is not a SELECT ... INTO.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a FROM into_table"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a FROM t JOIN into2 ON t.id = into2.id"));
    }

    @Test
    public void testSelectInto_qualifiedAndContainingIdentifiersAreReadOnly() {
        assertTrue(SqlParser.isReadOnlyQuery("SELECT t.into FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT into_col FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT into$ FROM t"));
    }

    @Test
    public void testSelectInto_inSubquerySelectListIsDetected() {
        // SELECT ... INTO inside a subquery still creates a table -> not read-only / not no-update.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a FROM t WHERE x IN (SELECT id INTO #tmp FROM s)"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT a FROM t WHERE x IN (SELECT id INTO #tmp FROM s)"));
    }

    @Test
    public void testSelectInto_intoInWhereSubqueryAfterFromIsReadOnly() {
        // A correlated subquery with no INTO in its select list -> read-only.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a FROM t WHERE x IN (SELECT id FROM s WHERE s.into = 1)"));
    }

    @Test
    public void testSelectInto_insertIntoSelectStaysNoUpdate() {
        // "INSERT INTO ... SELECT ..." is a plain insert; the INTO belongs to INSERT, not a SELECT INTO.
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t (a) SELECT a FROM s"));
    }

    @Test
    public void testSelectInto_qualifierDotSeparatedByCommentsOrWhitespaceIsReadOnly() {
        // A qualifier dot may be separated from the identifier by whitespace or a comment; such a
        // qualified column (a.into / into.x) must not be read as the SELECT ... INTO keyword.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a . into FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a./* x */into FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT into/* */.x FROM t"));
    }

    @Test
    public void testSelectInto_qualifiedColumnNamedFromIsReadOnly() {
        // A qualified column named like the FROM keyword must not end the SELECT-list scan early.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a.from FROM t"));
    }

    @Test
    public void testSelectInto_dollarIdentifierDoesNotHang() {
        // Regression guard: '$' is an identifier char; the INTO scan must advance past it, not spin.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertTrue(SqlParser.isReadOnlyQuery("SELECT a$ FROM t"));
            assertTrue(SqlParser.isReadOnlyQuery("SELECT $ FROM t"));
            assertTrue(SqlParser.isReadOnlyQuery("SELECT a$b, c$ FROM t$x WHERE y$ = 1"));
        });
    }

    @Test
    public void testSelectInto_scalarSubqueryInSelectListThenIntoIsDetected() {
        // A scalar subquery in the select list closes its own depth; the outer INTO (before the
        // outer FROM) must still be detected at depth 0.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT (SELECT max(x) FROM y) INTO #t FROM z"));
    }

    @Test
    public void testSelectInto_secondStatementSelectIntoAfterSemicolonIsDetected() {
        // ';' resets the depth-0 select-list state; a SELECT ... INTO in the next statement counts.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; SELECT a INTO #t FROM b"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; SELECT a INTO #t FROM b"));
    }

    @Test
    public void testSelectInto_derivedTableSelectIntoIsDetected() {
        // SELECT ... INTO inside a derived table (depth 1) is still a table-creating statement.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a FROM (SELECT b INTO #t FROM c) x"));
    }

    @Test
    public void testIsNoUpdateQuery_mergeAfterSemicolonRejected() {
        // Mirror of the isReadOnlyQuery MERGE-after-';' case for the no-update gate.
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
    }

    @Test
    public void testMultiStatementGate_replaceTruncateAndDdlRejected() {
        // Regression (2026-07-03): the multi-statement sweeps only covered INSERT/UPDATE/DELETE/MERGE,
        // so a later REPLACE INTO / TRUNCATE / DDL statement slipped through both gates.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; REPLACE INTO t VALUES (1)"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; TRUNCATE TABLE t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; DROP TABLE t"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; ALTER TABLE t ADD COLUMN c INT"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; CREATE TABLE t (c INT)"));
        assertFalse(SqlParser.isNoUpdateQuery("INSERT INTO t VALUES (1); REPLACE INTO t VALUES (2)"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; TRUNCATE TABLE t"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT 1; DROP TABLE t"));
    }

    @Test
    public void testMultiStatementGate_replaceAndTruncateFunctionsStillReadOnly() {
        // The REPLACE(...)/TRUNCATE(...) SQL functions appear mid-statement, never at a
        // statement-start position, so they must not affect classification.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT REPLACE(name, 'a', 'b') FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT TRUNCATE(1.234, 2) FROM dual"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a, REPLACE(b, 'x', 'y') FROM t WHERE c IN (SELECT d FROM e)"));
        assertTrue(SqlParser.isNoUpdateQuery("INSERT INTO t SELECT REPLACE(name, 'a', 'b') FROM s"));
    }

    // ----------------------------------------------------------------------------------------------
    // Hash-prefixed identifiers in comma-separated lists (2026-07-10): a '#' element after a comma
    // in a list governed by FROM/JOIN/INTO/UPDATE/TABLE is an identifier, not a MySQL hash comment.
    // Outside those contexts (e.g. a SELECT list) a '#' after a comma is still a comment.
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_hashTempTableAfterCommaInFromListIsKept() {
        // Regression: "#t2 WHERE id = 1" used to be swallowed as a MySQL hash comment because the
        // backward scan from '#t2' landed on ',' instead of the FROM keyword.
        final List<String> words = SqlParser.parse("SELECT * FROM #t1, #t2 WHERE id = 1");

        assertTrue(words.contains("#t1"), words.toString());
        assertTrue(words.contains("#t2"), words.toString());
        assertTrue(words.contains("WHERE"), words.toString());
        assertTrue(words.contains("1"), words.toString());
    }

    @Test
    public void testParse_hashTempTableListAfterIntoKeepsTail() {
        final List<String> words = SqlParser.parse("SELECT a INTO #t1, #t2 FROM x");

        assertTrue(words.contains("#t2"), words.toString());
        assertTrue(words.contains("FROM"), words.toString());
        assertTrue(words.contains("x"), words.toString());
    }

    @Test
    public void testReadOnlyAndNoUpdateGates_hashTempTableListDoesNotHideLaterDelete() {
        // Pre-fix these returned true (fail-open): '#t2' was treated as a comment, hiding the
        // ';' and the DELETE statement from the mutation-keyword scan.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * FROM #t1, #t2; DELETE FROM users"));
        assertFalse(SqlParser.isNoUpdateQuery("SELECT * FROM #t1, #t2; DELETE FROM users"));
    }

    @Test
    public void testParse_hashAfterCommaInSelectListStaysComment() {
        // SELECT is not a hash-identifier context keyword, so "SELECT a, #..." remains a MySQL
        // hash comment even though a comma precedes the '#'.
        final List<String> words = SqlParser.parse("SELECT a, #comment\nFROM t");

        assertFalse(words.contains("#comment"), words.toString());
        assertTrue(words.contains("FROM"), words.toString());
    }

    @Test
    public void testParse_hashTempTableListElementsWithAliasesAreKept() {
        // The list element before the comma may carry an alias ("#t1 a1" / "#t1 AS a1"), and the
        // first element may be a plain (non-'#') table name.
        assertTrue(SqlParser.parse("SELECT * FROM #t1 a1, #t2 WHERE id = 1").contains("#t2"));
        assertTrue(SqlParser.parse("SELECT * FROM #t1 AS a1, #t2 WHERE id = 1").contains("#t2"));
        assertTrue(SqlParser.parse("SELECT * FROM t1, #t2 WHERE id = 1").contains("#t2"));
    }

    @Test
    public void testParse_hashTempTableListAcrossLinesIsKept() {
        // The backward walk may cross a newline between the comma and the earlier list elements.
        final List<String> words = SqlParser.parse("SELECT * FROM #t1,\n#t2 WHERE id = 1");

        assertTrue(words.contains("#t2"), words.toString());
        assertTrue(words.contains("WHERE"), words.toString());
    }

    @Test
    public void testParse_hashTempTableJoinChainStillKept() {
        // The keyword-adjacent (comma-less) recognition is unchanged.
        final List<String> words = SqlParser.parse("SELECT * FROM #t1 JOIN #t2 ON a = b");

        assertTrue(words.contains("#t1"), words.toString());
        assertTrue(words.contains("#t2"), words.toString());
    }

    @Test
    public void testParse_hashAfterCommaOutsideListContextsStaysComment() {
        // A comma inside SET assignments, IN (...) value lists or GROUP BY lists does not make a
        // following '#' an identifier: the walk stops at '=', '(' or the non-context keyword.
        assertFalse(SqlParser.parse("UPDATE t SET a = 1, #note\nb = 2").contains("#note"));
        assertFalse(SqlParser.parse("SELECT * FROM t WHERE x IN (1, #c\n)").contains("#c"));
        assertFalse(SqlParser.parse("GROUP BY a, b, #c\nHAVING x = 1").contains("#c"));
    }

    @Test
    public void testParse_manyHashTempTablesOnOneLineCompletesQuickly() {
        // Perf regression guard (2026-07-10): each '#' on a single long line pays a backward scan;
        // pre-fix the scan ran twice per '#' plus a redundant re-scan (~0.36s/parse for this 25KB
        // statement; ~0.1s after). The generous bound only guards against gross (worse-than-
        // quadratic) regressions without being flaky on slow machines.
        final StringBuilder sb = new StringBuilder("SELECT * FROM #t0");

        for (int i = 1; i <= 800; i++) {
            sb.append(" JOIN #t").append(i).append(" ON t").append(i - 1).append(".id = t").append(i).append(".id");
        }

        final String sql = sb.toString();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (int i = 0; i < 3; i++) {
                assertTrue(SqlParser.parse(sql).contains("#t800"));
            }
        });
    }

    @Test
    public void testRegisterSeparator_multiCharFirstCharFastRejectIsRebuilt() {
        // ':' is not the first char of any default multi-char separator; registering "::" must be
        // picked up (exercising the first-char fast-reject rebuild) and resetting must drop it.
        SqlParser.registerSeparator("::");
        assertTrue(SqlParser.parse("a::b").contains("::"));

        SqlParser.resetSeparators();
        assertFalse(SqlParser.parse("a::b").contains("::"));
    }

    @Test
    public void testPostgreSqlJsonPathOperatorsAreNotCommentsOrParameters() {
        final String removePath = "SELECT payload #- '{address,city}' FROM events WHERE id = ?";
        final List<String> removePathWords = SqlParser.parse(removePath);

        assertTrue(removePathWords.contains("#-"), removePathWords.toString());
        assertTrue(removePathWords.contains("WHERE"), removePathWords.toString());
        assertEquals(1, ParsedSql.parse(removePath).parameterCount());

        final String pathPredicate = "SELECT payload @? '$.items[*] ? (@.price > 10)' FROM events WHERE id = ?";
        final List<String> pathPredicateWords = SqlParser.parse(pathPredicate);

        assertTrue(pathPredicateWords.contains("@?"), pathPredicateWords.toString());
        assertEquals(1, ParsedSql.parse(pathPredicate).parameterCount());
    }
}
