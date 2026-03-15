package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class SqlParser2025Test extends TestBase {

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
}

public class SqlParserTest extends TestBase {

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
    public void testParseEmptyString() {
        List<String> words = SqlParser.parse("");
        assertNotNull(words);
        assertTrue(words.isEmpty());
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
    public void testIndexWord() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";

        // Test finding simple words
        assertEquals(0, SqlParser.indexOfWord(sql, "SELECT", 0, false));
        assertEquals(9, SqlParser.indexOfWord(sql, "FROM", 0, false));
        assertEquals(20, SqlParser.indexOfWord(sql, "WHERE", 0, false));
        assertEquals(40, SqlParser.indexOfWord(sql, "ORDER BY", 0, false));

        // Test case sensitivity
        assertEquals(0, SqlParser.indexOfWord(sql, "select", 0, false));
        assertEquals(-1, SqlParser.indexOfWord(sql, "select", 0, true));

        // Test not found
        assertEquals(-1, SqlParser.indexOfWord(sql, "INSERT", 0, false));

        // Test from different starting positions
        assertEquals(-1, SqlParser.indexOfWord(sql, "SELECT", 5, false));
        assertEquals(40, SqlParser.indexOfWord(sql, "ORDER BY", 30, false));
    }

    @Test
    public void testIndexWordComposite() {
        String sql = "SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id";

        // Test composite words
        assertEquals(20, SqlParser.indexOfWord(sql, "LEFT JOIN", 0, false));
        assertEquals(20, SqlParser.indexOfWord(sql, "left join", 0, false));

        // Test GROUP BY
        sql = "SELECT COUNT(*) FROM users GROUP BY age";
        assertEquals(27, SqlParser.indexOfWord(sql, "GROUP BY", 0, false));

        // Test ORDER BY
        sql = "SELECT * FROM users ORDER BY name";
        assertEquals(20, SqlParser.indexOfWord(sql, "ORDER BY", 0, false));
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
    public void testNextWord() {
        String sql = "SELECT   name,   age FROM users";

        // Test skipping whitespace
        assertEquals("name", SqlParser.nextWord(sql, 6)); // After "SELECT"
        assertEquals(",", SqlParser.nextWord(sql, 13)); // After "name"
        assertEquals("age", SqlParser.nextWord(sql, 14)); // After ","
        assertEquals("FROM", SqlParser.nextWord(sql, 20)); // After "age"

        // Test at end of string
        assertEquals("users", SqlParser.nextWord(sql, 26));
        assertEquals("", SqlParser.nextWord(sql, 31));
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
    public void testRegisterSeparatorChar() {
        // Register a new character separator
        SqlParser.registerSeparator('$');

        String sql = "SELECT$FROM$users";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("$"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("$"));
        assertTrue(words.contains("users"));

    }

    @Test
    public void testRegisterSeparatorString() {
        // Register a new string separator
        SqlParser.registerSeparator("<->");

        String sql = "SELECT * WHERE a <-> b";
        List<String> words = SqlParser.parse(sql);

        assertTrue(words.contains("<->"));

        // Test single character string also registers as char
        SqlParser.registerSeparator("@");
        sql = "SELECT@FROM@users";
        words = SqlParser.parse(sql);

        assertTrue(words.contains("@"));

        // Test null separator should throw
        assertThrows(IllegalArgumentException.class, () -> SqlParser.registerSeparator((String) null));
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
    public void testIsSeparator() {
        String str = "SELECT * FROM users WHERE id = #{userId}";
        int len = str.length();

        // Test regular separators
        assertTrue(SqlParser.isSeparator(str, len, 6, ' ')); // Space after SELECT
        assertTrue(SqlParser.isSeparator(str, len, 7, '*')); // *
        assertTrue(SqlParser.isSeparator(str, len, 29, '=')); // =

        // Test MyBatis special case
        assertFalse(SqlParser.isSeparator(str, len, 31, '#')); // # before {userId}

        // Test regular # (not followed by {)
        str = "SELECT #comment";
        assertTrue(SqlParser.isSeparator(str, str.length(), 7, '#'));
    }

    @Test
    public void testIsFunctionName() {
        List<String> words = SqlParser.parse("SELECT COUNT(*), MAX(age), name FROM users");

        // COUNT is a function (followed by ()
        int countIndex = words.indexOf("COUNT");
        assertTrue(SqlParser.isFunctionName(words, words.size(), countIndex));

        // MAX is a function
        int maxIndex = words.indexOf("MAX");
        assertTrue(SqlParser.isFunctionName(words, words.size(), maxIndex));

        // name is not a function
        int nameIndex = words.indexOf("name");
        assertFalse(SqlParser.isFunctionName(words, words.size(), nameIndex));

        // users is not a function
        int usersIndex = words.indexOf("users");
        assertFalse(SqlParser.isFunctionName(words, words.size(), usersIndex));
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
}

class SqlParserJavadocExamples extends TestBase {

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
}
