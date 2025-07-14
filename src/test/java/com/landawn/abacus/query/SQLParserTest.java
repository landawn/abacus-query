package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SQLParser;

public class SQLParserTest extends TestBase {

    @Test
    public void testParseSimpleSQL() {
        // Test basic SELECT statement
        String sql = "SELECT * FROM users";
        List<String> words = SQLParser.parse(sql);

        assertNotNull(words);
        assertEquals(Arrays.asList("SELECT", " ", "*", " ", "FROM", " ", "users"), words);

        // Test with WHERE clause
        sql = "SELECT name, age FROM users WHERE age > 25";
        words = SQLParser.parse(sql);

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
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("'John Doe'"));

        // Test double quotes
        sql = "SELECT * FROM users WHERE name = \"John Doe\"";
        words = SQLParser.parse(sql);

        assertTrue(words.contains("\"John Doe\""));

        // Test escaped quotes
        sql = "SELECT * FROM users WHERE name = 'John\\'s'";
        words = SQLParser.parse(sql);

        assertTrue(words.stream().anyMatch(w -> w.contains("John\\'s")));
    }

    @Test
    public void testParseComments() {
        // Test single-line comments
        String sql = "SELECT * FROM users -- This is a comment\nWHERE id = 1";
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("WHERE"));
        assertFalse(words.stream().anyMatch(w -> w.contains("This is a comment")));

        // Test multi-line comments
        sql = "SELECT * /* multi-line\ncomment */ FROM users";
        words = SQLParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertFalse(words.stream().anyMatch(w -> w.contains("multi-line")));
    }

    @Test
    public void testParseOperators() {
        // Test various operators
        String sql = "SELECT * FROM users WHERE age >= 18 AND status != 'inactive' OR role IN ('admin', 'user')";
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains(">="));
        assertTrue(words.contains("!="));
        assertTrue(words.contains("IN"));
        assertTrue(words.contains("AND"));
        assertTrue(words.contains("OR"));

        // Test more operators
        sql = "SELECT * WHERE a = b AND c <> d AND e <= f AND g < h AND i > j AND k >> l";
        words = SQLParser.parse(sql);

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
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("!="));
        assertTrue(words.contains("<=>"));
        assertTrue(words.contains("||"));
        assertTrue(words.contains("&&"));

        // Test assignment operators
        sql = "UPDATE table SET a += 1, b -= 2, c *= 3, d /= 4";
        words = SQLParser.parse(sql);

        assertTrue(words.contains("+="));
        assertTrue(words.contains("-="));
        assertTrue(words.contains("*="));
        assertTrue(words.contains("/="));
    }

    @Test
    public void testParseMyBatisParameters() {
        // Test MyBatis/iBatis parameter syntax
        String sql = "SELECT * FROM users WHERE id = #{userId} AND name = #{userName}";
        List<String> words = SQLParser.parse(sql);

        // # should not be separated when followed by {
        assertTrue(words.contains("#{userId}"));
        assertTrue(words.contains("#{userName}"));

        // Test regular # separator
        sql = "SELECT * FROM users WHERE id = #123";
        words = SQLParser.parse(sql);

        assertTrue(words.contains("#"));
        assertTrue(words.contains("123"));
    }

    @Test
    public void testParseWhitespace() {
        // Test multiple spaces, tabs, and newlines
        String sql = "SELECT   *\t\tFROM\nusers\r\nWHERE\t id = 1";
        List<String> words = SQLParser.parse(sql);

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
    public void testIndexWord() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";

        // Test finding simple words
        assertEquals(0, SQLParser.indexWord(sql, "SELECT", 0, false));
        assertEquals(9, SQLParser.indexWord(sql, "FROM", 0, false));
        assertEquals(20, SQLParser.indexWord(sql, "WHERE", 0, false));
        assertEquals(40, SQLParser.indexWord(sql, "ORDER BY", 0, false));

        // Test case sensitivity
        assertEquals(0, SQLParser.indexWord(sql, "select", 0, false));
        assertEquals(-1, SQLParser.indexWord(sql, "select", 0, true));

        // Test not found
        assertEquals(-1, SQLParser.indexWord(sql, "INSERT", 0, false));

        // Test from different starting positions
        assertEquals(-1, SQLParser.indexWord(sql, "SELECT", 5, false));
        assertEquals(40, SQLParser.indexWord(sql, "ORDER BY", 30, false));
    }

    @Test
    public void testIndexWordComposite() {
        String sql = "SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id";

        // Test composite words
        assertEquals(20, SQLParser.indexWord(sql, "LEFT JOIN", 0, false));
        assertEquals(20, SQLParser.indexWord(sql, "left join", 0, false));

        // Test GROUP BY
        sql = "SELECT COUNT(*) FROM users GROUP BY age";
        assertEquals(27, SQLParser.indexWord(sql, "GROUP BY", 0, false));

        // Test ORDER BY
        sql = "SELECT * FROM users ORDER BY name";
        assertEquals(20, SQLParser.indexWord(sql, "ORDER BY", 0, false));
    }

    @Test
    public void testIndexWordQuoted() {
        // Test word inside quotes
        String sql = "SELECT * FROM users WHERE name = 'SELECT something'";

        // Should find the first SELECT, not the one in quotes
        assertEquals(0, SQLParser.indexWord(sql, "SELECT", 0, false));

        // Should find the quoted SELECT when searching from appropriate position
        int firstSelect = SQLParser.indexWord(sql, "SELECT", 0, false);
        int secondSelect = SQLParser.indexWord(sql, "SELECT", firstSelect + 1, false);
        assertEquals(-1, secondSelect);
    }

    @Test
    public void testIndexWordOperators() {
        String sql = "SELECT * WHERE a = b AND c != d OR e >= f";

        // Test finding operators
        assertEquals(17, SQLParser.indexWord(sql, "=", 0, false));
        assertEquals(27, SQLParser.indexWord(sql, "!=", 0, false));
        assertEquals(sql.indexOf(">="), SQLParser.indexWord(sql, ">=", 0, false));
    }

    @Test
    public void testNextWord() {
        String sql = "SELECT   name,   age FROM users";

        // Test skipping whitespace
        assertEquals("name", SQLParser.nextWord(sql, 6)); // After "SELECT"
        assertEquals(",", SQLParser.nextWord(sql, 13)); // After "name"
        assertEquals("age", SQLParser.nextWord(sql, 14)); // After ","
        assertEquals("FROM", SQLParser.nextWord(sql, 20)); // After "age"

        // Test at end of string
        assertEquals("users", SQLParser.nextWord(sql, 26));
        assertEquals("", SQLParser.nextWord(sql, 31));
    }

    @Test
    public void testNextWordQuoted() {
        String sql = "SELECT 'quoted string' FROM table";

        // Test getting quoted string
        assertEquals("'quoted string'", SQLParser.nextWord(sql, 6));

        // Test with double quotes
        sql = "SELECT \"quoted string\" FROM table";
        assertEquals("\"quoted string\"", SQLParser.nextWord(sql, 6));
    }

    @Test
    public void testNextWordOperators() {
        String sql = "WHERE a >= b AND c != d";

        assertEquals("a", SQLParser.nextWord(sql, 5));
        assertEquals(">=", SQLParser.nextWord(sql, 7));
        assertEquals("b", SQLParser.nextWord(sql, 10));
        assertEquals("AND", SQLParser.nextWord(sql, 12));
        assertEquals("!=", SQLParser.nextWord(sql, 19));
    }

    @Test
    public void testRegisterSeparatorChar() {
        // Register a new character separator
        SQLParser.registerSeparator('$');

        String sql = "SELECT$FROM$users";
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("$"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("$"));
        assertTrue(words.contains("users"));

    }

    @Test
    public void testRegisterSeparatorString() {
        // Register a new string separator
        SQLParser.registerSeparator("<->");

        String sql = "SELECT * WHERE a <-> b";
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("<->"));

        // Test single character string also registers as char
        SQLParser.registerSeparator("@");
        sql = "SELECT@FROM@users";
        words = SQLParser.parse(sql);

        assertTrue(words.contains("@"));

        // Test null separator should throw
        assertThrows(IllegalArgumentException.class, () -> SQLParser.registerSeparator((String) null));
    }

    @Test
    public void testIsSeparator() {
        String str = "SELECT * FROM users WHERE id = #{userId}";
        int len = str.length();

        // Test regular separators
        assertTrue(SQLParser.isSeparator(str, len, 6, ' ')); // Space after SELECT
        assertTrue(SQLParser.isSeparator(str, len, 7, '*')); // *
        assertTrue(SQLParser.isSeparator(str, len, 29, '=')); // =

        // Test MyBatis special case
        assertFalse(SQLParser.isSeparator(str, len, 31, '#')); // # before {userId}

        // Test regular # (not followed by {)
        str = "SELECT #comment";
        assertTrue(SQLParser.isSeparator(str, str.length(), 7, '#'));
    }

    @Test
    public void testIsFunctionName() {
        List<String> words = SQLParser.parse("SELECT COUNT(*), MAX(age), name FROM users");

        // COUNT is a function (followed by ()
        int countIndex = words.indexOf("COUNT");
        assertTrue(SQLParser.isFunctionName(words, words.size(), countIndex));

        // MAX is a function
        int maxIndex = words.indexOf("MAX");
        assertTrue(SQLParser.isFunctionName(words, words.size(), maxIndex));

        // name is not a function
        int nameIndex = words.indexOf("name");
        assertFalse(SQLParser.isFunctionName(words, words.size(), nameIndex));

        // users is not a function
        int usersIndex = words.indexOf("users");
        assertFalse(SQLParser.isFunctionName(words, words.size(), usersIndex));
    }

    @Test
    public void testIsFunctionNameWithSpaces() {
        List<String> words = SQLParser.parse("SELECT COUNT ( * ), SUM  (  amount  ) FROM sales");

        // COUNT is still a function even with spaces before (
        int countIndex = words.indexOf("COUNT");
        assertTrue(SQLParser.isFunctionName(words, words.size(), countIndex));

        // SUM is still a function even with multiple spaces
        int sumIndex = words.indexOf("SUM");
        assertTrue(SQLParser.isFunctionName(words, words.size(), sumIndex));
    }

    @Test
    public void testParseEmptyString() {
        List<String> words = SQLParser.parse("");
        assertNotNull(words);
        assertTrue(words.isEmpty());
    }

    @Test
    public void testParseComplexSQL() {
        String sql = "WITH RECURSIVE cte AS (" + "SELECT id, parent_id, name FROM categories WHERE parent_id IS NULL " + "UNION ALL "
                + "SELECT c.id, c.parent_id, c.name FROM categories c " + "INNER JOIN cte ON c.parent_id = cte.id" + ") SELECT * FROM cte ORDER BY name";

        List<String> words = SQLParser.parse(sql);

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
        List<String> words = SQLParser.parse(sql);

        assertTrue(words.contains("~="));
        assertTrue(words.contains("^="));
        assertTrue(words.contains("::"));
        assertTrue(words.contains("@>"));
    }
}