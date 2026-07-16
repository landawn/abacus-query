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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SqlParserTest extends TestBase {
    @Test
    public void testParseSimpleSelect() {
        String sql = "SELECT * FROM users";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("users"));
    }

    @Test
    public void testParseSelectWithColumns() {
        String sql = "SELECT id, name, email FROM users";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("id"));
        assertTrue(tokens.contains(","));
        assertTrue(tokens.contains("name"));
        assertTrue(tokens.contains("email"));
    }

    @Test
    public void testParseWithWhere() {
        String sql = "SELECT * FROM users WHERE age > 18";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("WHERE"));
        assertTrue(tokens.contains("age"));
        assertTrue(tokens.contains(">"));
        assertTrue(tokens.contains("18"));
    }

    @Test
    public void testParseWithJoin() {
        String sql = "SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("LEFT"));
        assertTrue(tokens.contains("JOIN"));
        assertTrue(tokens.contains("ON"));
    }

    @Test
    public void testParseWithOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND status = 'active'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains(">="));
        assertTrue(tokens.contains("AND"));
        assertTrue(tokens.contains("="));
    }

    @Test
    public void testParseWithQuotedString() {
        String sql = "SELECT * FROM users WHERE name = 'John Doe'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.stream().anyMatch(w -> w.contains("John Doe")));
    }

    @Test
    public void testParseWithDoubleQuotes() {
        String sql = "SELECT \"first name\" FROM users";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.stream().anyMatch(w -> w.contains("first name")));
    }

    @Test
    public void testParseWithParentheses() {
        String sql = "SELECT * FROM users WHERE (age > 18 AND status = 'active')";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("("));
        assertTrue(tokens.contains(")"));
    }

    @Test
    public void testParseWithGroupBy() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("GROUP"));
        assertTrue(tokens.contains("BY"));
    }

    @Test
    public void testParseWithOrderBy() {
        String sql = "SELECT * FROM users ORDER BY name ASC";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("ORDER"));
        assertTrue(tokens.contains("BY"));
    }

    @Test
    public void testParseWithMultipleOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND age <= 65";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains(">="));
        assertTrue(tokens.contains("<="));
    }

    @Test
    public void testParseWithInClause() {
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("IN"));
        assertTrue(tokens.contains("("));
        assertTrue(tokens.contains(","));
    }

    @Test
    public void testParseWithDoubleHashOperator() {
        List<String> tokens = SqlParser.parse("SELECT 1 ## 2");
        assertTrue(tokens.contains("##"));
    }

    @Test
    public void testParseWithPostgresJsonQuestionAndOperator() {
        List<String> tokens = SqlParser.parse("SELECT * FROM events WHERE payload ?& array['a']");
        assertTrue(tokens.contains("?&"), "PostgreSQL JSONB ?& operator must stay a single token: " + tokens);
        assertFalse(tokens.contains("?"), "PostgreSQL JSONB ?& operator must not be split into a JDBC placeholder: " + tokens);
    }

    @Test
    public void testParseEmptyString() {
        String sql = "";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testParseWithHaving() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("HAVING"));
    }

    @Test
    public void testParseWithSubquery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("IN"));
    }

    @Test
    public void testParseWithUnion() {
        String sql = "SELECT id FROM users UNION SELECT id FROM accounts";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("UNION"));
    }

    @Test
    public void testParseWithUnionAll() {
        String sql = "SELECT id FROM users UNION ALL SELECT id FROM accounts";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("UNION"));
        assertTrue(tokens.contains("ALL"));
    }

    @Test
    public void testParseWithNotEquals() {
        String sql = "SELECT * FROM users WHERE status != 'deleted'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("!="));
    }

    @Test
    public void testParseWithNotEqualsAlternative() {
        String sql = "SELECT * FROM users WHERE status <> 'deleted'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("<>"));
    }

    @Test
    public void testParseWithBetween() {
        String sql = "SELECT * FROM users WHERE age BETWEEN 18 AND 65";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("BETWEEN"));
        assertTrue(tokens.contains("AND"));
    }

    @Test
    public void testParseWithLike() {
        String sql = "SELECT * FROM users WHERE name LIKE 'John%'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("LIKE"));
    }

    @Test
    public void testParseWithNotLike() {
        String sql = "SELECT * FROM users WHERE name NOT LIKE 'John%'";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("NOT"));
        assertTrue(tokens.contains("LIKE"));
    }

    @Test
    public void testParseWithForUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("FOR"));
        assertTrue(tokens.contains("UPDATE"));
    }

    @Test
    public void testParseComplexQuery() {
        String sql = "SELECT u.id, u.name, COUNT(o.id) as order_count " + "FROM users u " + "LEFT JOIN orders o ON u.id = o.user_id "
                + "WHERE u.status = 'active' AND u.created_date > '2020-01-01' " + "GROUP BY u.id, u.name " + "HAVING COUNT(o.id) > 5 "
                + "ORDER BY order_count DESC " + "LIMIT 10";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.size() > 10);
    }

    @Test
    public void testParseWithArithmeticOperators() {
        String sql = "SELECT price * quantity + tax - discount FROM orders";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("*"));
        assertTrue(tokens.contains("+"));
        assertTrue(tokens.contains("-"));
    }

    @Test
    public void testParseWithModuloOperator() {
        String sql = "SELECT id % 10 FROM users";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("%"));
    }

    @Test
    public void testParseWithBitwiseOperators() {
        String sql = "SELECT value & mask FROM data";
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertTrue(tokens.contains("&"));
    }

    @Test
    public void testIndexOfToken() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfToken(sql, "WHERE", 0, false);
        assertTrue(index >= 0);
        assertEquals("WHERE", sql.substring(index, index + 5));
    }

    @Test
    public void testIndexOfTokenCaseInsensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfToken(sql, "where", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexOfTokenCaseSensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfToken(sql, "where", 0, true);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexOfTokenNotFound() {
        String sql = "SELECT * FROM users";
        int index = SqlParser.indexOfToken(sql, "WHERE", 0, false);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexOfCompositeToken() {
        String sql = "SELECT * FROM users ORDER BY name";
        int index = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexOfTokenFromPosition() {
        String sql = "SELECT * FROM users WHERE age > 18 AND status = 'active'";
        int firstAnd = SqlParser.indexOfToken(sql, "AND", 0, false);
        assertTrue(firstAnd > 0);
    }

    @Test
    public void testIndexOfTokenWithNegativeFromIndex() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SqlParser.indexOfToken(sql, "SELECT", -5, false);
        assertEquals(0, index);
    }

    @Test
    public void testNextToken() {
        String sql = "SELECT * FROM users";
        String token = SqlParser.nextToken(sql, 7);
        assertNotNull(token);
        assertEquals("*", token);
    }

    @Test
    public void testNextTokenSkipsWhitespace() {
        String sql = "SELECT    *    FROM users";
        String token = SqlParser.nextToken(sql, 6);
        assertNotNull(token);
        assertEquals("*", token);
    }

    @Test
    public void testNextTokenEmptyString() {
        String sql = "SELECT *";
        String token = SqlParser.nextToken(sql, sql.length());
        assertEquals("", token);
    }

    @Test
    public void testNextTokenWithOperator() {
        String sql = "WHERE age >= 18";
        String token = SqlParser.nextToken(sql, 10);
        assertNotNull(token);
        assertTrue(token.equals(">=") || token.equals("18"));
    }

    @Test
    public void testNextTokenWithNegativeFromIndex() {
        String sql = "SELECT * FROM users";
        String token = SqlParser.nextToken(sql, -3);
        assertEquals("SELECT", token);
    }

    @Test
    public void testTokenNavigationMethodsAreHardRenamed() throws NoSuchMethodException {
        assertEquals(int.class, SqlParser.class.getMethod("indexOfToken", String.class, String.class).getReturnType());
        assertEquals(String.class, SqlParser.class.getMethod("nextToken", String.class, int.class).getReturnType());
        assertEquals(int.class, SqlParser.class.getMethod("nextTokenEndIndex", String.class, int.class).getReturnType());

        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("indexOfWord", String.class, String.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("nextWord", String.class, int.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("nextWordEnd", String.class, int.class));
    }

    @Test
    public void testTokenizerConfigCharSeparator() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator('$').build());
        List<String> tokens = tokenizer.parse("SELECT$FROM$users");
        assertEquals(Arrays.asList("SELECT", "$", "FROM", "$", "users"), tokens);
        assertEquals(Arrays.asList("SELECT$FROM$users"), SqlParser.parse("SELECT$FROM$users"));
    }

    @Test
    public void testTokenizerConfigStringSeparator() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator(":::").build());
        List<String> tokens = tokenizer.parse("SELECT:::FROM:::users");
        assertEquals(Arrays.asList("SELECT", ":::", "FROM", ":::", "users"), tokens);
    }

    @Test
    public void testTokenizerConfigRejectsNullSeparator() {
        assertThrows(IllegalArgumentException.class, () -> SqlParser.tokenizerConfigBuilder().withSeparator((String) null));
        assertThrows(IllegalArgumentException.class, () -> SqlParser.tokenizer(null));
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
        List<String> tokens = SqlParser.parse("SELECT COUNT(*) FROM users");
        int countIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if ("COUNT".equals(tokens.get(i))) {
                countIndex = i;
                break;
            }
        }
        assertTrue(countIndex >= 0);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedBoundedIsFunctionNamePreservesUpperBound() {
        final List<String> tokens = Arrays.asList("COUNT", " ", "(", "*");

        assertFalse(SqlParser.isFunctionName(tokens, 2, 0));
        assertTrue(SqlParser.isFunctionName(tokens, 3, 0));
        assertTrue(SqlParser.isFunctionName(tokens, Integer.MAX_VALUE, 0));
    }

    @Test
    public void testIsFunctionNameNotFunction() {
        List<String> tokens = SqlParser.parse("SELECT name FROM users");
        int nameIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if ("name".equals(tokens.get(i))) {
                nameIndex = i;
                break;
            }
        }
        assertTrue(nameIndex >= 0);
    }

    @Test
    public void testIndexWordMultipleOccurrences() {
        String sql = "SELECT name FROM users WHERE name = 'John' OR name = 'Jane'";
        int firstIndex = SqlParser.indexOfToken(sql, "name", 0, false);
        int secondIndex = SqlParser.indexOfToken(sql, "name", firstIndex + 1, false);
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex > firstIndex);
    }

    @Test
    public void testNextTokenAtEnd() {
        String sql = "SELECT * FROM users";
        String token = SqlParser.nextToken(sql, sql.length() - 1);
        assertNotNull(token);
    }

    @Test
    public void testParseSimpleSQL() {
        // Test basic SELECT statement
        String sql = "SELECT * FROM users";
        List<String> tokens = SqlParser.parse(sql);

        assertNotNull(tokens);
        assertEquals(Arrays.asList("SELECT", " ", "*", " ", "FROM", " ", "users"), tokens);

        // Test with WHERE clause
        sql = "SELECT name, age FROM users WHERE age > 25";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("name"));
        assertTrue(tokens.contains(","));
        assertTrue(tokens.contains("age"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("users"));
        assertTrue(tokens.contains("WHERE"));
        assertTrue(tokens.contains(">"));
        assertTrue(tokens.contains("25"));
    }

    @Test
    public void testParseQuotedIdentifiers() {
        // Test single quotes
        String sql = "SELECT * FROM users WHERE name = 'John Doe'";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("'John Doe'"));

        // Test double quotes
        sql = "SELECT * FROM users WHERE name = \"John Doe\"";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("\"John Doe\""));

        // Test escaped quotes
        sql = "SELECT * FROM users WHERE name = 'John\\'s'";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.stream().anyMatch(w -> w.contains("John\\'s")));

        // Test SQL-standard escaped quote by doubling delimiter
        sql = "SELECT * FROM users WHERE name = 'it''s me'";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("'it''s me'"));
    }

    @Test
    public void testParseComments() {
        // Test single-line comments
        String sql = "SELECT * FROM users -- This is a comment\nWHERE id = 1";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("WHERE"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("This is a comment")));

        // Test multi-line comments
        sql = "SELECT * /* multi-line\ncomment */ FROM users";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("FROM"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("multi-line")));
    }

    @Test
    public void testParseHashComments() {
        String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?\nAND status = :status";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("AND"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(tokens.contains("?"));
    }

    @Test
    public void testParseHashCommentsWithoutSpace() {
        String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?\nAND status = :status";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("AND"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(tokens.contains("?"));
    }

    @Test
    public void testParseHashCommentAtLineStartWithoutSpace() {
        String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains(":userId"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("fake")));
        assertFalse(tokens.contains("?"));
    }

    @Test
    public void testParseHashTempTableIsNotComment() {
        String sql = "SELECT * FROM #tmp WHERE id = :id";
        List<String> tokens = SqlParser.parse(sql);
        String joined = String.join("", tokens);

        assertTrue(joined.contains("#tmp"));
        assertTrue(tokens.contains("WHERE"));
        assertTrue(tokens.contains(":id"));
    }

    @Test
    public void testParseHashJsonOperatorsAreNotComments() {
        String sql = "SELECT payload #> '{meta,status}' AS status_json FROM docs WHERE payload #>> '{meta,status}' = 'active'";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("#>"));
        assertTrue(tokens.contains("#>>"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("WHERE"));
        assertTrue(tokens.contains("'active'"));
    }

    @Test
    public void testParseOperators() {
        // Test various operators
        String sql = "SELECT * FROM users WHERE age >= 18 AND status != 'inactive' OR role IN ('admin', 'user')";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains(">="));
        assertTrue(tokens.contains("!="));
        assertTrue(tokens.contains("IN"));
        assertTrue(tokens.contains("AND"));
        assertTrue(tokens.contains("OR"));

        // Test more operators
        sql = "SELECT * WHERE a = b AND c <> d AND e <= f AND g < h AND i > j AND k >> l";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("="));
        // assertTrue(tokens.contains("<>"));
        assertTrue(tokens.contains("<="));
        assertTrue(tokens.contains("<"));
        assertTrue(tokens.contains(">"));
        assertTrue(tokens.contains(">>"));
    }

    @Test
    public void testParseComplexOperators() {
        // Test multi-character operators
        String sql = "SELECT * WHERE a != b AND c <=> d AND e || f AND g && h";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("!="));
        assertTrue(tokens.contains("<=>"));
        assertTrue(tokens.contains("||"));
        assertTrue(tokens.contains("&&"));

        // Test assignment operators
        sql = "UPDATE table SET a += 1, b -= 2, c *= 3, d /= 4";
        tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("+="));
        assertTrue(tokens.contains("-="));
        assertTrue(tokens.contains("*="));
        assertTrue(tokens.contains("/="));
    }

    @Test
    public void testParseMyBatisParameters() {
        // Test MyBatis/iBatis parameter syntax
        String sql = "SELECT * FROM users WHERE id = #{userId} AND name = #{userName}";
        List<String> tokens = SqlParser.parse(sql);

        // # should not be separated when followed by {
        assertTrue(tokens.contains("#{userId}"));
        assertTrue(tokens.contains("#{userName}"));
    }

    @Test
    public void testParseWhitespace() {
        // Test multiple spaces, tabs, and newlines
        String sql = "SELECT   *\t\tFROM\nusers\r\nWHERE\t id = 1";
        List<String> tokens = SqlParser.parse(sql);

        // Should normalize whitespace to single spaces
        assertEquals("SELECT", tokens.get(0));
        assertEquals(" ", tokens.get(1));
        assertEquals("*", tokens.get(2));
        assertEquals(" ", tokens.get(3));
        assertEquals("FROM", tokens.get(4));
        assertEquals(" ", tokens.get(5));
        assertEquals("users", tokens.get(6));
        assertEquals(" ", tokens.get(7));
        assertEquals("WHERE", tokens.get(8));
    }

    @Test
    public void testParseComplexSQL() {
        String sql = "WITH RECURSIVE cte AS (" + "SELECT id, parent_id, name FROM categories WHERE parent_id IS NULL " + "UNION ALL "
                + "SELECT c.id, c.parent_id, c.name FROM categories c " + "INNER JOIN cte ON c.parent_id = cte.id" + ") SELECT * FROM cte ORDER BY name";

        List<String> tokens = SqlParser.parse(sql);

        // Verify key SQL keywords are parsed
        assertTrue(tokens.contains("WITH"));
        assertTrue(tokens.contains("RECURSIVE"));
        assertFalse(tokens.contains("UNION ALL"));
        assertFalse(tokens.contains("INNER JOIN"));
        assertFalse(tokens.contains("ORDER BY"));
        assertFalse(tokens.contains("IS NULL"));
    }

    @Test
    public void testParseSpecialOperators() {
        // Test various special operators from the separators set
        String sql = "SELECT * WHERE a ~= b AND c ^= d AND e :: text AND f @> g";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("~="));
        assertTrue(tokens.contains("^="));
        assertTrue(tokens.contains("::"));
        assertTrue(tokens.contains("@>"));
    }

    @Test
    public void testIndexWordQuoted() {
        // Test token inside quotes
        String sql = "SELECT * FROM users WHERE name = 'SELECT something'";

        // Should find the first SELECT, not the one in quotes
        assertEquals(0, SqlParser.indexOfToken(sql, "SELECT", 0, false));

        // Should find the quoted SELECT when searching from appropriate position
        int firstSelect = SqlParser.indexOfToken(sql, "SELECT", 0, false);
        int secondSelect = SqlParser.indexOfToken(sql, "SELECT", firstSelect + 1, false);
        assertEquals(-1, secondSelect);

        // Test escaped quote in SQL-standard form (two single quotes) inside a string literal
        sql = "SELECT * FROM users WHERE note = 'it''s SELECT here'";
        assertEquals(-1, SqlParser.indexOfToken(sql, "SELECT", 1, false));
    }

    @Test
    public void testIndexWordOperators() {
        String sql = "SELECT * WHERE a = b AND c != d OR e >= f";

        // Test finding operators
        assertEquals(17, SqlParser.indexOfToken(sql, "=", 0, false));
        assertEquals(27, SqlParser.indexOfToken(sql, "!=", 0, false));
        assertEquals(sql.indexOf(">="), SqlParser.indexOfToken(sql, ">=", 0, false));
    }

    @Test
    public void testNextTokenQuoted() {
        String sql = "SELECT 'quoted string' FROM table";

        // Test getting quoted string
        assertEquals("'quoted string'", SqlParser.nextToken(sql, 6));

        // Test with double quotes
        sql = "SELECT \"quoted string\" FROM table";
        assertEquals("\"quoted string\"", SqlParser.nextToken(sql, 6));

        // Test SQL-standard escaped quote by doubling delimiter
        sql = "SELECT 'it''s done' FROM table";
        assertEquals("'it''s done'", SqlParser.nextToken(sql, 6));
    }

    @Test
    public void testNextTokenOperators() {
        String sql = "WHERE a >= b AND c != d";

        assertEquals("a", SqlParser.nextToken(sql, 5));
        assertEquals(">=", SqlParser.nextToken(sql, 7));
        assertEquals("b", SqlParser.nextToken(sql, 10));
        assertEquals("AND", SqlParser.nextToken(sql, 12));
        assertEquals("!=", SqlParser.nextToken(sql, 19));
    }

    @Test
    public void testTokenizerConfigStringWithNewLeadingChar() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("$$").build());
        List<String> tokens = tokenizer.parse("SELECT$$FROM$$users");
        assertEquals(Arrays.asList("SELECT", "$$", "FROM", "$$", "users"), tokens);
    }

    @Test
    public void testTokenizerConfigLongSeparatorString() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("~~~~").build());
        List<String> tokens = tokenizer.parse("SELECT~~~~FROM~~~~users");
        assertEquals(Arrays.asList("SELECT", "~~~~", "FROM", "~~~~", "users"), tokens);
    }

    // Cover comment-retention and hash-prefixed temp table parsing branches.
    @Test
    public void testParse_KeepCommentsDirective() {
        String sql = "-- Keep comments\nSELECT /* keep me */ name FROM users";

        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("/* keep me */"));
    }

    @Test
    public void testParse_HashPrefixedIdentifier() {
        String sql = "SELECT * FROM #temp_users WHERE id = 1";

        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("#temp_users"));
        assertTrue(tokens.contains("WHERE"));
    }

    @Test
    public void testIndexOfToken_HashPrefixedIdentifier() {
        String sql = "SELECT * FROM #temp_users WHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", 0, false));
    }

    @Test
    public void testNextToken_HashPrefixedIdentifier() {
        String sql = "FROM #temp_users";

        assertEquals("#temp_users", SqlParser.nextToken(sql, 4));
    }

    @Test
    public void testNextToken_SkipsBlockComment() {
        String sql = "SELECT /* hidden */ name FROM users";

        assertEquals("name", SqlParser.nextToken(sql, 6));
    }

    // Exercise uncovered parser branches around inline comments, quoted tokens, and backtracking.
    @Test
    public void testParse_InlineDashCommentAfterToken() {
        String sql = "SELECT col-- hidden\nFROM users";

        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("col"));
        assertTrue(tokens.contains("FROM"));
        assertFalse(tokens.stream().anyMatch(e -> e.contains("hidden")));
    }

    @Test
    public void testParse_InlineBlockCommentAfterToken() {
        String sql = "SELECT col/* hidden */FROM users";

        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("col"));
        assertTrue(tokens.contains("FROM"));
        assertEquals("SELECT col FROM users", String.join("", tokens));
        assertFalse(tokens.stream().anyMatch(e -> e.contains("hidden")));
    }

    @Test
    public void testIndexOfToken_QuotedTokenWithBackslashEscape() {
        String sql = "SELECT 'John\\'s' FROM users";

        assertEquals(sql.indexOf("'John\\'s'"), SqlParser.indexOfToken(sql, "'John\\'s'", 0, true));
    }

    @Test
    public void testIndexOfToken_DashCommentAfterMismatchedToken() {
        String sql = "value-- hidden\nWHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfToken_HashCommentAfterMismatchedToken() {
        String sql = "value# hidden\nWHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfToken_BlockCommentAfterMismatchedToken() {
        String sql = "value/* hidden */WHERE id = 1";

        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", 0, false));
    }

    @Test
    public void testIndexOfToken_FromIndexInsideQuotedLiteralSkipsToLaterToken() {
        String sql = "SELECT 'hidden WHERE token' FROM users WHERE id = 1";
        int fromIndex = sql.indexOf("WHERE token");

        assertEquals(sql.lastIndexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", fromIndex, false));
    }

    @Test
    public void testIndexOfToken_FromIndexInsideBlockCommentSkipsToLaterToken() {
        String sql = "SELECT /* hidden WHERE token */ id FROM users WHERE id = 1";
        int fromIndex = sql.indexOf("WHERE token");

        assertEquals(sql.lastIndexOf("WHERE"), SqlParser.indexOfToken(sql, "WHERE", fromIndex, false));
    }

    @Test
    public void testIndexOfToken_TrailingTokenAtEnd() {
        String sql = "SELECT FROM";

        assertEquals(sql.length() - "FROM".length(), SqlParser.indexOfToken(sql, "FROM", 0, false));
    }

    @Test
    public void testIndexOfToken_CompositeTokenAfterFalseStarts() {
        String sql = "SELECT * FROM users ORDER name ORDER age";

        assertEquals(-1, SqlParser.indexOfToken(sql, "ORDER BY", 0, false));
    }

    @Test
    public void testNextToken_QuotedTokenWithBackslashEscape() {
        String sql = "SELECT 'John\\'s' FROM users";

        assertEquals("'John\\'s'", SqlParser.nextToken(sql, 6));
    }

    @Test
    public void testNextToken_SkipsDashCommentAtStart() {
        String sql = "-- hidden\nvalue";

        assertEquals("value", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testNextToken_DashCommentAfterToken() {
        String sql = "value-- hidden\nother";

        assertEquals("value", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testNextToken_SkipsHashCommentAtStart() {
        String sql = "# hidden\nvalue";

        assertEquals("value", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testNextToken_HashCommentAfterToken() {
        String sql = "value# hidden\nother";

        assertEquals("value", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testNextToken_BlockCommentAfterToken() {
        String sql = "value/* hidden */other";

        assertEquals("value", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testNextToken_LeadingNewlineWhitespace() {
        String sql = " \n\tvalue";

        assertEquals("value", SqlParser.nextToken(sql, 0));
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
        List<String> tokens = SqlParser.parse(sql);
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("users"));
        assertTrue(tokens.contains("WHERE"));
        // Javadoc shows "ORDER" and "BY" as separate tokens in parse() output
        assertTrue(tokens.contains("ORDER"));
        assertTrue(tokens.contains("BY"));
        assertTrue(tokens.contains("name"));
    }

    @Test
    public void testSqlParser_parse() {
        List<String> tokens = SqlParser.parse("SELECT name, age FROM users WHERE age >= 18");
        assertNotNull(tokens);
        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("name"));
        assertTrue(tokens.contains(","));
        assertTrue(tokens.contains("age"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("users"));
        assertTrue(tokens.contains("WHERE"));
        assertTrue(tokens.contains(">="));
        assertTrue(tokens.contains("18"));
    }

    @Test
    public void testSqlParser_indexOfToken() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
        int index = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0, "ORDER BY should be found in the SQL");
        int whereIndex = SqlParser.indexOfToken(sql, "WHERE", 0, false);
        assertTrue(whereIndex >= 0, "WHERE should be found in the SQL");
        assertTrue(whereIndex < index, "WHERE should come before ORDER BY");
    }

    @Test
    public void testSqlParser_nextToken() {
        String sql = "SELECT   name,   age FROM users";
        String token1 = SqlParser.nextToken(sql, 6);
        assertEquals("name", token1);
        String token2 = SqlParser.nextToken(sql, 13);
        assertEquals(",", token2);
        String token3 = SqlParser.nextToken(sql, 14);
        assertEquals("age", token3);
    }

    @Test
    public void testSqlParser_tokenizerConfigChar() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator('$').build());
        List<String> tokens = tokenizer.parse("SELECT$FROM$users");
        assertNotNull(tokens);
        assertTrue(tokens.contains("$"));
    }

    @Test
    public void testSqlParser_tokenizerConfigString() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("<=>").withSeparator("::").build());
        List<String> tokens = tokenizer.parse("SELECT <=> value :: text");
        assertTrue(tokens.contains("<=>"));
        assertTrue(tokens.contains("::"));
    }

    @Test
    public void testTokenizerConfigRejectsEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> SqlParser.tokenizerConfigBuilder().withSeparator(""));
    }

    @Test
    public void testIndexOfToken_compositeKeyword_isNotNullWithBlockCommentBetweenComponentTokens() {
        String sql = "SELECT col FROM t WHERE col IS /* remark */ NOT /* remark2 */ NULL";
        int idx = SqlParser.indexOfToken(sql, "IS NOT NULL", 0, false);
        assertTrue(idx >= 0, "indexOfToken should find IS NOT NULL even when block comments appear between component tokens");
        assertEquals(sql.indexOf("IS"), idx);
    }

    @Test
    public void testIndexOfToken_compositeKeyword_orderByWithBlockComment() {
        String sql = "SELECT * FROM t ORDER /* comment */ BY id";
        int idx = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);
        assertTrue(idx >= 0, "indexOfToken should find ORDER BY even with a block comment between the keywords");
    }

    @Test
    public void testParseBackslashEscapedQuote() {
        // Backslash-escaped single quote (MySQL style) should keep the string intact.
        String sql = "SELECT * FROM t WHERE x = 'O\\'Brien'";
        List<String> tokens = SqlParser.parse(sql);
        assertTrue(tokens.stream().anyMatch(w -> w.equals("'O\\'Brien'")),
                "Expected the backslash-escaped quote to keep the literal as a single token, got: " + tokens);
    }

    @Test
    public void testParseDoubledQuoteEscape() {
        // SQL-standard doubled-quote escape.
        String sql = "SELECT 'O''Brien' FROM dual";
        List<String> tokens = SqlParser.parse(sql);
        assertTrue(tokens.contains("'O''Brien'"));
    }

    @Test
    public void testIndexOfTokenIsNotMatchingSubstring() {
        // indexOfToken must respect token boundaries: searching for "JOIN" must NOT match inside RIGHTJOIN.
        String sql = "SELECT * FROM t1 RIGHTJOIN t2 ON ...";
        int idx = SqlParser.indexOfToken(sql, "JOIN", 0, false);
        // The only token in this SQL containing JOIN is "RIGHTJOIN" itself, which is not a separate token.
        assertEquals(-1, idx, "indexOfToken('JOIN') must not match the substring inside identifier RIGHTJOIN");
    }

    @Test
    public void testParseBackslashEscapedQuoteThenClosingQuote() {
        // Regression: a backslash-escaped quote (\') immediately followed by the real closing
        // quote must terminate the string literal. Previously the doubled-quote ('') check won
        // over the pending backslash escape, so the literal stayed open and swallowed the rest
        // of the SQL (FROM/t never became separate tokens).
        String sql = "SELECT 'a\\'' FROM t";
        List<String> tokens = SqlParser.parse(sql);
        assertTrue(tokens.contains("'a\\''"), "Expected the escaped-quote-then-closing-quote literal as a single token, got: " + tokens);
        assertTrue(tokens.contains("FROM"), "FROM must be parsed as a separate token (string terminated), got: " + tokens);
        assertTrue(tokens.contains("t"), "table name must be parsed as a separate token, got: " + tokens);
    }

    @Test
    public void testNextTokenBackslashEscapedQuoteThenClosingQuote() {
        // nextToken must return the whole terminated literal '...\'' and stop at it.
        String sql = "'a\\'' rest";
        assertEquals("'a\\''", SqlParser.nextToken(sql, 0));
    }

    @Test
    public void testIndexOfTokenAfterBackslashEscapedQuoteThenClosingQuote() {
        // The escaped-then-closing quote terminates the literal, so a keyword after it is found.
        String sql = "WHERE x = 'a\\'' AND y = 1";
        int idx = SqlParser.indexOfToken(sql, "AND", 0, false);
        assertTrue(idx > 0, "AND after a terminated escaped-quote literal must be found, got index: " + idx);
    }

    // ----------------------------------------------------------------------------------------------
    // indexOfToken(String, String) -- 2-arg convenience overload
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfToken2Arg_findsCaseInsensitiveFromStart() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";

        assertEquals(0, SqlParser.indexOfToken(sql, "SELECT"));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfToken(sql, "FROM"));
        // Default is case-insensitive.
        assertEquals(sql.indexOf("WHERE"), SqlParser.indexOfToken(sql, "where"));
        // Composite keyword.
        assertEquals(sql.indexOf("ORDER"), SqlParser.indexOfToken(sql, "ORDER BY"));
    }

    @Test
    public void testIndexOfToken2Arg_notFoundReturnsMinusOne() {
        String sql = "SELECT * FROM users";

        assertEquals(-1, SqlParser.indexOfToken(sql, "WHERE"));
    }

    // ----------------------------------------------------------------------------------------------
    // indexOfToken(String, String, int) -- 3-arg convenience overload
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfToken3Arg_respectsFromIndex() {
        String sql = "SELECT name FROM users WHERE name = 'x' OR name = 'y'";

        int first = SqlParser.indexOfToken(sql, "name", 0);
        assertTrue(first >= 0);

        int second = SqlParser.indexOfToken(sql, "name", first + 1);
        assertTrue(second > first);

        int third = SqlParser.indexOfToken(sql, "name", second + 1);
        assertTrue(third > second);
    }

    @Test
    public void testIndexOfToken3Arg_defaultsToCaseInsensitive() {
        String sql = "select * from users";

        assertEquals(0, SqlParser.indexOfToken(sql, "SELECT", 0));
    }

    @Test
    public void testIndexOfToken3Arg_negativeFromIndexTreatedAsZero() {
        String sql = "SELECT * FROM users";

        assertEquals(0, SqlParser.indexOfToken(sql, "SELECT", -10));
    }

    // ----------------------------------------------------------------------------------------------
    // nextTokenEndIndex(String, int)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testNextTokenEndIndex_javadocExample() {
        String sql = "SELECT   name,   age FROM users";

        assertEquals(13, SqlParser.nextTokenEndIndex(sql, 6)); // just past "name"
        assertEquals(14, SqlParser.nextTokenEndIndex(sql, 13)); // just past ","
        assertEquals(20, SqlParser.nextTokenEndIndex(sql, 14)); // just past "age"
    }

    @Test
    public void testNextTokenEndIndex_quotedToken() {
        String sql = "SELECT 'a b' FROM t";
        int end = SqlParser.nextTokenEndIndex(sql, 6);

        assertEquals(sql.indexOf("'a b'") + "'a b'".length(), end);
        assertEquals("'a b'", sql.substring(end - "'a b'".length(), end));
    }

    @Test
    public void testNextTokenEndIndex_multiCharOperator() {
        String sql = "a >= b";
        int end = SqlParser.nextTokenEndIndex(sql, 1); // scanning from the space before ">="

        assertEquals(sql.indexOf(">=") + 2, end);
    }

    @Test
    public void testNextTokenEndIndex_skipsLeadingBlockComment() {
        String sql = "SELECT /* hidden */ name FROM users";
        int end = SqlParser.nextTokenEndIndex(sql, 6);

        assertEquals(sql.indexOf("name") + "name".length(), end);
    }

    @Test
    public void testNextTokenEndIndex_noFurtherTokenReturnsLength() {
        String sql = "SELECT   ";

        assertEquals(sql.length(), SqlParser.nextTokenEndIndex(sql, 6));
    }

    @Test
    public void testNextTokenEndIndex_onlyCommentRemainsReturnsLength() {
        String sql = "x -- trailing comment";

        // After "x" only whitespace + a line comment remain.
        assertEquals(sql.length(), SqlParser.nextTokenEndIndex(sql, 1));
    }

    @Test
    public void testNextTokenEndIndex_negativeFromIndexTreatedAsZero() {
        String sql = "SELECT * FROM users";

        assertEquals("SELECT".length(), SqlParser.nextTokenEndIndex(sql, -5));
    }

    @Test
    public void testNextTokenEndIndex_consistentWithNextToken() {
        assertNextTokenConsistency("SELECT   name,   age FROM users");
        assertNextTokenConsistency("WHERE a >= b AND c != d");
        assertNextTokenConsistency("SELECT 'a b', \"q\" FROM t -- tail\n");
        assertNextTokenConsistency("SELECT [a]]b], [--] FROM [t]");
        assertNextTokenConsistency("SELECT /* c */ x FROM #tmp");
        assertNextTokenConsistency("a||b->>c");
        assertNextTokenConsistency("");
        assertNextTokenConsistency("   ");
    }

    /**
     * Walks {@code sql} with {@link SqlParser#nextToken} and {@link SqlParser#nextTokenEndIndex} in lockstep
     * and asserts that the end index reported for each token bounds exactly the token text returned.
     */
    private static void assertNextTokenConsistency(final String sql) {
        int idx = 0;

        while (idx <= sql.length()) {
            final String token = SqlParser.nextToken(sql, idx);
            final int end = SqlParser.nextTokenEndIndex(sql, idx);

            if (token.isEmpty()) {
                assertEquals(sql.length(), end, "empty token => end at length for [" + sql + "] idx=" + idx);
                break;
            }

            final int start = end - token.length();
            assertTrue(start >= idx, "token start >= scan position for [" + sql + "] idx=" + idx);
            assertEquals(token, sql.substring(start, end), "token text bounded by nextTokenEndIndex for [" + sql + "] idx=" + idx);

            if (end <= idx) {
                break; // safety against an accidental infinite loop in a failing build
            }

            idx = end;
        }
    }

    // ----------------------------------------------------------------------------------------------
    // instance-scoped tokenizer configuration
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testTokenizerConfigCanAddAndRemoveSeparators() {
        final SqlParser.Tokenizer withCustom = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("::").withSeparator('$').build());
        final SqlParser.Tokenizer withoutDefaults = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withoutSeparator("##").build());

        assertEquals(Arrays.asList("a", "::", "b"), withCustom.parse("a::b"));
        assertEquals(Arrays.asList("a", "$", "b"), withCustom.parse("a$b"));
        assertEquals(1, withCustom.indexOfToken("a::b", "::"));
        assertEquals("::", withCustom.nextToken("a::b", 1));
        assertEquals(3, withCustom.nextTokenEndIndex("a::b", 1));
        assertFalse(withoutDefaults.parse("1##2").contains("##"));
        assertTrue(SqlParser.parse("1##2").contains("##"));
    }

    @Test
    public void testTokenizerConfigurationsAreIsolated() {
        final SqlParser.Tokenizer colonTokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("::").build());
        final SqlParser.Tokenizer dollarTokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("$$").build());

        assertTrue(colonTokenizer.parse("a::b").contains("::"));
        assertFalse(dollarTokenizer.parse("a::b").contains("::"));
        assertTrue(dollarTokenizer.parse("a$$b").contains("$$"));
        assertFalse(colonTokenizer.parse("a$$b").contains("$$"));
    }

    @Test
    public void testTokenizerConfigIsImmutable() {
        final SqlParser.TokenizerConfig.Builder builder = SqlParser.tokenizerConfigBuilder().withSeparator("::");
        final SqlParser.TokenizerConfig config = builder.build();

        assertThrows(UnsupportedOperationException.class, () -> config.separators().add("$$"));
        assertTrue(config.separators().contains("::"));

        builder.withSeparator("$$");
        assertFalse(config.separators().contains("$$"), "A built configuration must not share mutable state with its builder");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTokenizerConfigOwnsBuilderCreationAndSupportsDerivation() {
        final SqlParser.TokenizerConfig defaults = SqlParser.TokenizerConfig.builder().build();
        final SqlParser.TokenizerConfig base = SqlParser.TokenizerConfig.builder().withSeparator("::").build();
        final SqlParser.TokenizerConfig derived = base.toBuilder().withSeparator("$$").withoutSeparator("::").build();

        assertEquals(SqlParser.defaultTokenizerConfig(), defaults);
        assertEquals(defaults, SqlParser.tokenizerConfigBuilder().build(), "The deprecated parser-level factory remains compatible");
        assertTrue(base.separators().contains("::"));
        assertFalse(base.separators().contains("$$"));
        assertFalse(derived.separators().contains("::"));
        assertTrue(derived.separators().contains("$$"));
    }

    @Test
    public void testTokenizerConfigWithoutSpaceTreatsCompositeTextAsOneToken() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withoutSeparator(' ').build());

        assertEquals(Arrays.asList("ORDER BY"), tokenizer.parse("ORDER BY"));
        assertEquals(0, tokenizer.indexOfToken("ORDER BY", "ORDER BY"));
        assertEquals("ORDER BY", tokenizer.nextToken("ORDER BY", 0));
        assertEquals(8, tokenizer.nextTokenEndIndex("ORDER BY", 0));
    }

    @Test
    public void testIndexOfTokenMatchesQuotedTokensContainingSpaces() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        final String sql = "SELECT 'a b', \"First Name\", `Last Name`, [Display ]] Name] FROM users";

        assertEquals(sql.indexOf("'a b'"), tokenizer.indexOfToken(sql, "'a b'"));
        assertEquals(sql.indexOf("\"First Name\""), tokenizer.indexOfToken(sql, "\"First Name\""));
        assertEquals(sql.indexOf("`Last Name`"), tokenizer.indexOfToken(sql, "`Last Name`"));
        assertEquals(sql.indexOf("[Display ]] Name]"), tokenizer.indexOfToken(sql, "[Display ]] Name]"));

        // The static default facade has the same corrected token contract.
        assertEquals(sql.indexOf("[Display ]] Name]"), SqlParser.indexOfToken(sql, "[Display ]] Name]"));
    }

    @Test
    public void testCustomHashSeparatorIsUsedByBackwardHashContextScan() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("#!").build());
        final String sql = "SELECT #! 1 FROM #tmp WHERE id = 1";
        final int tempTableStart = sql.indexOf("#tmp");

        final List<String> tokens = tokenizer.parse(sql);
        assertTrue(tokens.contains("#!"));
        assertTrue(tokens.contains("#tmp"));
        assertTrue(tokens.contains("WHERE"));
        assertEquals(tempTableStart, tokenizer.indexOfToken(sql, "#tmp"));
        assertEquals("#tmp", tokenizer.nextToken(sql, tempTableStart));
        assertEquals(tempTableStart + 4, tokenizer.nextTokenEndIndex(sql, tempTableStart));
    }

    @Test
    public void testTokenizerConfigLongestCustomSeparatorAndSupplementaryUnicode() {
        final SqlParser.Tokenizer longest = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("::").withSeparator(":::").build());
        final SqlParser.Tokenizer withoutLongest = SqlParser
                .tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("::").withSeparator(":::").withoutSeparator(":::").build());
        final SqlParser.Tokenizer unicode = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("😀").build());

        assertEquals(Arrays.asList("a", ":::", "b"), longest.parse("a:::b"));
        assertEquals(Arrays.asList("a", "::", ":b"), withoutLongest.parse("a:::b"));
        assertEquals(Arrays.asList("a", "😀", "b"), unicode.parse("a😀b"));
    }

    @Test
    public void testGlobalSeparatorMutationApisAreRemoved() {
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("registerSeparator", char.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("registerSeparator", String.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("unregisterSeparator", char.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("unregisterSeparator", String.class));
        assertThrows(NoSuchMethodException.class, () -> SqlParser.class.getMethod("resetSeparators"));
    }

    @Test
    public void testTokenizerConfigRemovalRejectsNullOrEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> SqlParser.tokenizerConfigBuilder().withoutSeparator(null));
        assertThrows(IllegalArgumentException.class, () -> SqlParser.tokenizerConfigBuilder().withoutSeparator(""));
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
    public void testTokenizerIsReadOnlyQueryUsesConfiguredHashOperator() throws NoSuchMethodException {
        final String sql = "SELECT 1 #foo ; UPDATE t SET x=1";
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("#foo").build());

        // Under the built-in configuration, #foo starts a MySQL line comment and hides the rest
        // of this line. The custom configuration makes it an operator, exposing the later UPDATE.
        assertTrue(SqlParser.isReadOnlyQuery(sql));
        assertFalse(tokenizer.isReadOnlyQuery(sql));
        assertTrue(java.lang.reflect.Modifier.isPublic(SqlParser.Tokenizer.class.getDeclaredMethod("isReadOnlyQuery", String.class).getModifiers()));
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
    // isNonUpdateQuery(String)
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsNoUpdateQuery_readsAndPlainInsertsAllowed() {
        assertTrue(SqlParser.isNonUpdateQuery("SELECT * FROM t"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1)"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t (a) SELECT a FROM u"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t (do, update) VALUES (1, 2)"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t (duplicate, key, update) VALUES (1, 2, 3)"));
        // ON CONFLICT ... DO NOTHING never overwrites existing rows.
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO NOTHING"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (do, update) DO NOTHING"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testReadOrInsertQueryHasClearPrimaryNameAndCompatibleAlias() {
        final String read = "SELECT * FROM t";
        final String plainInsert = "INSERT INTO t VALUES (1)";
        final String upsert = "INSERT INTO t VALUES (1) ON CONFLICT (id) DO UPDATE SET value = 2";

        assertTrue(SqlParser.isReadOrInsertQuery(read));
        assertTrue(SqlParser.isReadOrInsertQuery(plainInsert));
        assertFalse(SqlParser.isReadOrInsertQuery(upsert));
        assertEquals(SqlParser.isReadOrInsertQuery(read), SqlParser.isNonUpdateQuery(read));
        assertEquals(SqlParser.isReadOrInsertQuery(plainInsert), SqlParser.isNonUpdateQuery(plainInsert));
        assertEquals(SqlParser.isReadOrInsertQuery(upsert), SqlParser.isNonUpdateQuery(upsert));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOriginalNoUpdateQueryNameRemainsCompatible() {
        final String read = "SELECT * FROM t";
        final String plainInsert = "INSERT INTO t VALUES (1)";
        final String upsert = "INSERT INTO t VALUES (1) ON CONFLICT (id) DO UPDATE SET value = 2";

        assertEquals(SqlParser.isReadOrInsertQuery(read), SqlParser.isNoUpdateQuery(read));
        assertEquals(SqlParser.isReadOrInsertQuery(plainInsert), SqlParser.isNoUpdateQuery(plainInsert));
        assertEquals(SqlParser.isReadOrInsertQuery(upsert), SqlParser.isNoUpdateQuery(upsert));
        assertEquals(SqlParser.isReadOrInsertQuery(null), SqlParser.isNoUpdateQuery(null));
    }

    @Test
    public void testProcedureCallAfterSafeStatementIsRejected() {
        final List<String> statements = Arrays.asList("SELECT 1; CALL refresh_materialized_view()", "SELECT 1; {call refresh_materialized_view()}",
                "SELECT 1; {? = call refresh_materialized_view()}", "SELECT 1; EXEC refresh_materialized_view", "SELECT 1; EXECUTE refresh_materialized_view",
                "SELECT 1; /* invoke */ { ? /* return */ = /* verb */ CALL refresh_materialized_view() }",
                "SELECT 1; -- invoke on next line\n EXEC refresh_materialized_view");

        for (final String sql : statements) {
            assertFalse(SqlParser.isReadOnlyQuery(sql), sql);
            assertFalse(SqlParser.isReadOrInsertQuery(sql), sql);
        }
    }

    @Test
    public void testCallFunctionNameInsideSelectDoesNotTriggerProcedureGuard() {
        final List<String> statements = Arrays.asList(
                "SELECT CALL(value), EXEC(value), EXECUTE(value) FROM source; "
                        + "SELECT 'CALL', '{call hidden()}', '{? = call hidden()}', [EXEC], \"EXECUTE\" /* ; EXEC hidden */",
                "SELECT 1; -- EXEC hidden\n SELECT 2", "SELECT 1; /* {call hidden()} */ SELECT execution_time FROM audit_log");

        for (final String sql : statements) {
            assertTrue(SqlParser.isReadOnlyQuery(sql), sql);
            assertTrue(SqlParser.isReadOrInsertQuery(sql), sql);
        }
    }

    @Test
    public void testIsNoUpdateQuery_upsertsAndMutationsRejected() {
        assertFalse(SqlParser.isNonUpdateQuery("INSERT OR REPLACE INTO t VALUES (1)"));
        assertFalse(SqlParser.isNonUpdateQuery("WITH seed AS (SELECT 1) INSERT OR REPLACE INTO t SELECT * FROM seed"));
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON DUPLICATE KEY UPDATE x = 1"));
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON /* c */ DUPLICATE KEY UPDATE x = 1"));
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) DO UPDATE SET x = 1"));
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) WHERE active DO UPDATE SET x = 1"));
        assertFalse(SqlParser.isNonUpdateQuery("INSERT OVERWRITE TABLE t SELECT * FROM s"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * INTO newt FROM t"));
        assertFalse(SqlParser.isNonUpdateQuery("UPDATE t SET x = 1"));
        assertFalse(SqlParser.isNonUpdateQuery("DELETE FROM t"));
        assertFalse(SqlParser.isNonUpdateQuery("MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; DELETE FROM t"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * FROM #tmp; DELETE FROM t"));
    }

    @Test
    public void testIsNoUpdateQuery_hashIdentifierAfterCommentsDoesNotHideMutation() {
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * FROM /* temp */ #tmp; DELETE FROM users"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * FROM -- temp\n#tmp; DELETE FROM users"));
    }

    @Test
    public void testIsNoUpdateQuery_nullAndEmpty() {
        // Empty/null does not lead with SELECT or INSERT, so it is not a no-update query
        // (consistent with isReadOnlyQuery/isSelectQuery/isInsertQuery).
        assertFalse(SqlParser.isNonUpdateQuery(null));
        assertFalse(SqlParser.isNonUpdateQuery(""));
    }

    // ----------------------------------------------------------------------------------------------
    // A couple of extra parse()/isFunctionName edge paths for completeness.
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testParse_unterminatedSingleQuoteKeepsRemainderAsOneToken() {
        // An unterminated quote swallows the rest of the input as a single token (documented behavior).
        List<String> tokens = SqlParser.parse("SELECT 'oops FROM t");

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.stream().anyMatch(w -> w.startsWith("'oops")));
    }

    @Test
    public void testParse_keepCommentsRetainsMultipleBlockComments() {
        String sql = "-- Keep comments\nSELECT /* a */ 1 /* b */ FROM t";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("/* a */"));
        assertTrue(tokens.contains("/* b */"));
    }

    @Test
    public void testTokenMethods_nullInputsThrow() {
        assertThrows(NullPointerException.class, () -> SqlParser.parse(null));
        assertThrows(NullPointerException.class, () -> SqlParser.indexOfToken(null, "SELECT"));
        assertThrows(NullPointerException.class, () -> SqlParser.indexOfToken("SELECT 1", null));
        assertThrows(NullPointerException.class, () -> SqlParser.nextToken(null, 0));
        assertThrows(NullPointerException.class, () -> SqlParser.nextTokenEndIndex(null, 0));
    }

    @Test
    public void testIsFunctionName_twoArgOverload() {
        List<String> tokens = SqlParser.parse("SELECT COUNT(*) FROM users");

        assertTrue(SqlParser.isFunctionName(tokens, 2)); // "COUNT"
        assertFalse(SqlParser.isFunctionName(tokens, 0)); // "SELECT"
        assertFalse(SqlParser.isFunctionName(tokens, -1));
        assertFalse(SqlParser.isFunctionName(tokens, tokens.size()));
        assertThrows(NullPointerException.class, () -> SqlParser.isFunctionName(null, 0));
    }

    @Test
    public void testIndexOfToken_compositeTokenWithDoubledInternalSpace() {
        String sql = "A ORDER BY B";
        int singleSpaced = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);

        assertEquals(2, singleSpaced);
        // A doubled internal space in the token argument must split to the same sub-tokens
        // (previously the empty middle sub-token could never match and was even cached).
        assertEquals(singleSpaced, SqlParser.indexOfToken(sql, "ORDER  BY", 0, false));
    }

    @Test
    public void testIndexOfTokenIgnoresSurroundingWhitespaceInSearchToken() {
        final String sql = "SELECT * FROM t WHERE id = 1";
        final int expected = sql.indexOf("WHERE");

        assertEquals(expected, SqlParser.indexOfToken(sql, "  WHERE\t", 0, false));
        assertEquals(expected, SqlParser.tokenizer().indexOfToken(sql, "  WHERE\t"));

        final String orderedSql = sql + " ORDER BY id";
        final int orderByIndex = orderedSql.indexOf("ORDER");
        assertEquals(orderByIndex, SqlParser.indexOfToken(orderedSql, "ORDER\tBY", 0, false));
        assertEquals(orderByIndex, SqlParser.tokenizer().indexOfToken(orderedSql, "\nORDER\r\nBY\t"));
    }

    @Test
    public void testIsFunctionName_oracleOuterJoinMarkerIsNotFunctionCall() {
        List<String> tokens = SqlParser.parse("SELECT a.id(+) FROM a");

        assertTrue(tokens.contains("(+)"));
    }

    // ----------------------------------------------------------------------------------------------
    // TokenizerConfig -- non-ASCII separator paths
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testTokenizerConfig_nonAsciiChar() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator('§').build());

        assertEquals(Arrays.asList("a", "§", "b"), tokenizer.parse("a§b"));
    }

    @Test
    public void testTokenizerConfig_nonAsciiMultiCharString() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("→→").build());

        assertEquals(Arrays.asList("a", "→→", "b"), tokenizer.parse("a→→b"));
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
        List<String> tokens = SqlParser.parse("SELECT 1 /* unterminated");

        assertTrue(tokens.contains("SELECT"));
        assertTrue(tokens.contains("1"));
    }

    @Test
    public void testParse_keepCommentsStillStripsLineComments() {
        // The "-- Keep comments" marker keeps block comments but line/hash comments are always stripped.
        String sql = "-- Keep comments\nSELECT 1 -- gone\n/* kept */ FROM t # also gone\n";
        List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("/* kept */"));
        assertFalse(tokens.stream().anyMatch(w -> w.contains("gone")));
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
    public void testParseAndNextToken_bracketQuotedIdentifierIsSingleToken() {
        final String sql = "SELECT [a]]b], [--] FROM [table]";
        final List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("[a]]b]"));
        assertTrue(tokens.contains("[--]"));
        assertEquals("[a]]b]", SqlParser.nextToken(sql, "SELECT".length()));
        assertEquals(sql.indexOf("[a]]b]") + "[a]]b]".length(), SqlParser.nextTokenEndIndex(sql, "SELECT".length()));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfToken(sql, "FROM", 0, false));
    }

    @Test
    public void testParseAndNextToken_bracketIdentifierEndingInBackslashCloses() {
        // SQL Server [bracket] identifiers do not honor backslash escaping (only ]] doubles a bracket), so a
        // bracket ending in a lone backslash must still close and not swallow the rest of the statement.
        // Regression across all four tokenizer scanners (parse / nextToken / nextTokenEndIndex / indexOfToken).
        final String sql = "SELECT [a\\] FROM t"; // the identifier is [a\]
        final List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("[a\\]"));
        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.contains("t"));
        assertEquals("[a\\]", SqlParser.nextToken(sql, "SELECT".length()));
        assertEquals(sql.indexOf("[a\\]") + "[a\\]".length(), SqlParser.nextTokenEndIndex(sql, "SELECT".length()));
        assertEquals(sql.indexOf("FROM"), SqlParser.indexOfToken(sql, "FROM", 0, false));

        // A Windows-path-like identifier that ends in a backslash likewise closes cleanly.
        final String pathSql = "SELECT [C:\\path\\] FROM t"; // the identifier is [C:\path\]
        assertTrue(SqlParser.parse(pathSql).contains("[C:\\path\\]"));
        assertEquals(pathSql.indexOf("FROM"), SqlParser.indexOfToken(pathSql, "FROM", 0, false));
    }

    // ----------------------------------------------------------------------------------------------
    // nextToken / nextTokenEndIndex -- MyBatis #{...} stays one token
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testNextToken_myBatisMarkerIsOneToken() {
        assertEquals("#{userId}", SqlParser.nextToken("WHERE id = #{userId}", 10));
        assertNextTokenConsistency("a #{x} b");
        assertNextTokenConsistency("SELECT #{p} FROM #tmp WHERE c #>> d");
    }

    @Test
    public void testNextToken_hashPrefixedIdentifierAfterComments() {
        assertEquals("#tmp", SqlParser.nextToken("FROM /* temp */ #tmp", "FROM".length()));
        assertEquals("#tmp", SqlParser.nextToken("FROM/* temp */#tmp", "FROM".length()));
        assertEquals("#tmp", SqlParser.nextToken("FROM -- temp\n#tmp", "FROM".length()));
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
    // indexOfToken -- case sensitivity on composite keywords
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIndexOfToken_caseSensitiveComposite() {
        String sql = "SELECT * FROM a LEFT JOIN b ON a.id = b.id";

        assertEquals(sql.indexOf("LEFT"), SqlParser.indexOfToken(sql, "LEFT JOIN", 0, true));
        // Wrong case must not match when caseSensitive=true.
        assertEquals(-1, SqlParser.indexOfToken(sql, "left join", 0, true));
        // ...but matches when caseSensitive=false.
        assertEquals(sql.indexOf("LEFT"), SqlParser.indexOfToken(sql, "left join", 0, false));
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
    // isNonUpdateQuery -- ON CONFLICT shapes and plain-insert variants
    // ----------------------------------------------------------------------------------------------

    @Test
    public void testIsNoUpdateQuery_onConflictNamedConstraintDoUpdateRejected() {
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT ON CONSTRAINT uq DO UPDATE SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_mutationAfterSemicolonWithCteRejected() {
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; WITH doomed AS (SELECT 1) UPDATE t SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictNoTargetDoUpdateRejected() {
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT DO UPDATE SET x = 1"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictNamedConstraintDoNothingAllowed() {
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT ON CONSTRAINT uq DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_onConflictPredicateDoTokenWithPunctuationAllowed() {
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1) ON CONFLICT (id) WHERE do = update DO NOTHING"));
    }

    @Test
    public void testIsNoUpdateQuery_plainInsertWithReturningAllowed() {
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t (a) VALUES (1) RETURNING id"));
    }

    @Test
    public void testIsNoUpdateQuery_selectIntoSubstringInIdentifierIsAllowed() {
        assertTrue(SqlParser.isNonUpdateQuery("SELECT into$ FROM t"));
        assertTrue(SqlParser.isNonUpdateQuery("SELECT $into FROM t"));
        assertTrue(SqlParser.isNonUpdateQuery("SELECT t.into FROM t"));
        assertTrue(SqlParser.isNonUpdateQuery("SELECT * FROM $into"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT a INTO new_table FROM t"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT t.from, a INTO new_table FROM t"));
        assertTrue(SqlParser.isNonUpdateQuery("SELECT t. /* c */ into FROM t"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; SELECT a INTO new_table FROM t"));
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
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES ('OVERWRITE')"));
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
        final List<String> tokens = SqlParser.parse("-- Keep comments\nSELECT /*/x*/ FROM t");

        assertTrue(tokens.contains("/*/x*/"), "the whole /*/x*/ must be kept as a single comment token: " + tokens);
    }

    @Test
    public void testNextToken_skipsBlockCommentWithSlashBody() {
        assertEquals("name", SqlParser.nextToken("/*/x*/ name", 0));
    }

    @Test
    public void testNextTokenEndIndex_skipsBlockCommentWithSlashBody() {
        final String sql = "/*/x*/ name";

        assertEquals(sql.indexOf("name") + "name".length(), SqlParser.nextTokenEndIndex(sql, 0));
    }

    @Test
    public void testIndexOfToken_doesNotMatchKeywordInsideSlashBodyComment() {
        // FROM is inside the "/*/FROM*/" comment; the only real keyword is the trailing SELECT.
        final String sql = "/*/FROM*/ SELECT 1";

        assertEquals(-1, SqlParser.indexOfToken(sql, "FROM", 0, false));
        assertEquals(sql.indexOf("SELECT"), SqlParser.indexOfToken(sql, "SELECT", 0, false));
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
        final List<String> tokens = SqlParser.parse("UPDATE #t1 SET x = 1 FROM #t2");

        assertTrue(tokens.contains("#t1"), tokens.toString());
        assertTrue(tokens.contains("#t2"), tokens.toString());
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
        final List<String> tokens = SqlParser.parse("SELECT 1 # trailing comment\nFROM t");

        assertTrue(tokens.contains("FROM"));
        assertTrue(tokens.stream().noneMatch(w -> w.contains("trailing")));
    }

    @Test
    public void testParse_hashTempTableAsImplicitDmlTargetIsKept() {
        final List<String> insertWords = SqlParser.parse("INSERT #stage (id) VALUES (1)");
        final List<String> deleteWords = SqlParser.parse("DELETE #stage WHERE id = 1");
        final List<String> mergeWords = SqlParser.parse("MERGE #stage USING source ON 1 = 1 WHEN MATCHED THEN DELETE");

        assertTrue(insertWords.contains("#stage"), insertWords.toString());
        assertTrue(insertWords.contains("VALUES"), insertWords.toString());
        assertTrue(deleteWords.contains("#stage"), deleteWords.toString());
        assertTrue(deleteWords.contains("WHERE"), deleteWords.toString());
        assertTrue(mergeWords.contains("#stage"), mergeWords.toString());
        assertTrue(mergeWords.contains("USING"), mergeWords.toString());
    }

    @Test
    public void testParse_hashTempTableAfterDmlTopClauseIsKept() {
        final List<String> insertWords = SqlParser.parse("INSERT TOP (10) #stage (id) VALUES (1)");
        final List<String> deleteWords = SqlParser.parse("DELETE TOP (25) PERCENT #stage WHERE id = 1");
        final List<String> mergeWords = SqlParser.parse("MERGE TOP 5 #stage USING source ON 1 = 1 WHEN MATCHED THEN DELETE");

        assertTrue(insertWords.contains("#stage"), insertWords.toString());
        assertTrue(deleteWords.contains("#stage"), deleteWords.toString());
        assertTrue(mergeWords.contains("#stage"), mergeWords.toString());
    }

    @Test
    public void testParse_hashCommentsOutsideDmlTargetPositionRemainComments() {
        final List<String> insertWords = SqlParser.parse("INSERT INTO stage (id) #comment\nVALUES (1)");
        final List<String> deleteWords = SqlParser.parse("DELETE FROM stage #comment\nWHERE id = 1");
        final List<String> mergeWords = SqlParser.parse("MERGE stage #comment\nUSING source ON stage.id = source.id");
        final List<String> selectWords = SqlParser.parse("SELECT INSERT, #comment\nFROM source");

        assertFalse(insertWords.contains("#comment"), insertWords.toString());
        assertTrue(insertWords.contains("VALUES"), insertWords.toString());
        assertFalse(deleteWords.contains("#comment"), deleteWords.toString());
        assertTrue(deleteWords.contains("WHERE"), deleteWords.toString());
        assertFalse(mergeWords.contains("#comment"), mergeWords.toString());
        assertTrue(mergeWords.contains("USING"), mergeWords.toString());
        assertFalse(selectWords.contains("#comment"), selectWords.toString());
        assertTrue(selectWords.contains("FROM"), selectWords.toString());
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
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * INTO newt FROM t"));
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
        assertFalse(SqlParser.isNonUpdateQuery("SELECT a FROM t WHERE x IN (SELECT id INTO #tmp FROM s)"));
    }

    @Test
    public void testSelectInto_intoInWhereSubqueryAfterFromIsReadOnly() {
        // A correlated subquery with no INTO in its select list -> read-only.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a FROM t WHERE x IN (SELECT id FROM s WHERE s.into = 1)"));
    }

    @Test
    public void testSelectInto_insertIntoSelectStaysNoUpdate() {
        // "INSERT INTO ... SELECT ..." is a plain insert; the INTO belongs to INSERT, not a SELECT INTO.
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t (a) SELECT a FROM s"));
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
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; SELECT a INTO #t FROM b"));
    }

    @Test
    public void testSelectInto_derivedTableSelectIntoIsDetected() {
        // SELECT ... INTO inside a derived table (depth 1) is still a table-creating statement.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT a FROM (SELECT b INTO #t FROM c) x"));
    }

    @Test
    public void testIsNoUpdateQuery_mergeAfterSemicolonRejected() {
        // Mirror of the isReadOnlyQuery MERGE-after-';' case for the no-update gate.
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; MERGE INTO t USING s ON (t.id = s.id) WHEN MATCHED THEN UPDATE SET t.x = s.x"));
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
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO t VALUES (1); REPLACE INTO t VALUES (2)"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; INSERT OR REPLACE INTO t VALUES (2)"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; TRUNCATE TABLE t"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; DROP TABLE t"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; ALTER TABLE t ADD COLUMN c INT"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT 1; CREATE TABLE t (c INT)"));
    }

    @Test
    public void testMultiStatementGate_replaceAndTruncateFunctionsStillReadOnly() {
        // The REPLACE(...)/TRUNCATE(...) SQL functions appear mid-statement, never at a
        // statement-start position, so they must not affect classification.
        assertTrue(SqlParser.isReadOnlyQuery("SELECT REPLACE(name, 'a', 'b') FROM t"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT TRUNCATE(1.234, 2) FROM dual"));
        assertTrue(SqlParser.isReadOnlyQuery("SELECT a, REPLACE(b, 'x', 'y') FROM t WHERE c IN (SELECT d FROM e)"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO t SELECT REPLACE(name, 'a', 'b') FROM s"));
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
        final List<String> tokens = SqlParser.parse("SELECT * FROM #t1, #t2 WHERE id = 1");

        assertTrue(tokens.contains("#t1"), tokens.toString());
        assertTrue(tokens.contains("#t2"), tokens.toString());
        assertTrue(tokens.contains("WHERE"), tokens.toString());
        assertTrue(tokens.contains("1"), tokens.toString());
    }

    @Test
    public void testParse_hashTempTableListAfterIntoKeepsTail() {
        final List<String> tokens = SqlParser.parse("SELECT a INTO #t1, #t2 FROM x");

        assertTrue(tokens.contains("#t2"), tokens.toString());
        assertTrue(tokens.contains("FROM"), tokens.toString());
        assertTrue(tokens.contains("x"), tokens.toString());
    }

    @Test
    public void testReadOnlyAndNoUpdateGates_hashTempTableListDoesNotHideLaterDelete() {
        // Pre-fix these returned true (fail-open): '#t2' was treated as a comment, hiding the
        // ';' and the DELETE statement from the mutation-keyword scan.
        assertFalse(SqlParser.isReadOnlyQuery("SELECT * FROM #t1, #t2; DELETE FROM users"));
        assertFalse(SqlParser.isNonUpdateQuery("SELECT * FROM #t1, #t2; DELETE FROM users"));
    }

    @Test
    public void testReadOnlyAndNoUpdateGates_hashTempAfterDerivedTableDoesNotHideLaterDelete() {
        final String sql = "SELECT * FROM (SELECT 1) derived, #tmp; DELETE FROM users";
        final String quotedAndCommented = "SELECT * FROM (SELECT ')' AS value /* ) */) derived, #tmp; DELETE FROM users";
        final String hashCommentWithParenthesis = "SELECT * FROM (SELECT 1 # ) inside comment\n) derived, #tmp; DELETE FROM users";
        final String hashCommentWithOpeningParenthesis = "SELECT * FROM (SELECT 1 # ( inside comment\n) derived, #tmp; DELETE FROM users";

        assertFalse(SqlParser.isReadOnlyQuery(sql));
        assertFalse(SqlParser.isNonUpdateQuery(sql));
        assertFalse(SqlParser.isReadOnlyQuery(quotedAndCommented));
        assertFalse(SqlParser.isNonUpdateQuery(quotedAndCommented));
        assertFalse(SqlParser.isReadOnlyQuery(hashCommentWithParenthesis));
        assertFalse(SqlParser.isNonUpdateQuery(hashCommentWithParenthesis));
        assertFalse(SqlParser.isReadOnlyQuery(hashCommentWithOpeningParenthesis));
        assertFalse(SqlParser.isNonUpdateQuery(hashCommentWithOpeningParenthesis));
    }

    @Test
    public void testParse_hashAfterCommaInSelectListStaysComment() {
        // SELECT is not a hash-identifier context keyword, so "SELECT a, #..." remains a MySQL
        // hash comment even though a comma precedes the '#'.
        final List<String> tokens = SqlParser.parse("SELECT a, #comment\nFROM t");

        assertFalse(tokens.contains("#comment"), tokens.toString());
        assertTrue(tokens.contains("FROM"), tokens.toString());
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
        final List<String> tokens = SqlParser.parse("SELECT * FROM #t1,\n#t2 WHERE id = 1");

        assertTrue(tokens.contains("#t2"), tokens.toString());
        assertTrue(tokens.contains("WHERE"), tokens.toString());
    }

    @Test
    public void testParse_hashTempTableJoinChainStillKept() {
        // The keyword-adjacent (comma-less) recognition is unchanged.
        final List<String> tokens = SqlParser.parse("SELECT * FROM #t1 JOIN #t2 ON a = b");

        assertTrue(tokens.contains("#t1"), tokens.toString());
        assertTrue(tokens.contains("#t2"), tokens.toString());
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
    public void testTokenizerConfig_multiCharFirstCharFastRejectIsScoped() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator("::").build());
        assertTrue(tokenizer.parse("a::b").contains("::"));
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

    @Test
    public void testIsNonUpdateQueryRejectsSelectIntoAfterLeadingInsert() {
        assertFalse(SqlParser.isNonUpdateQuery("INSERT INTO audit_log VALUES (1); SELECT value INTO snapshot FROM source"));
        assertTrue(SqlParser.isNonUpdateQuery("INSERT INTO audit_log VALUES (1); SELECT value FROM source"));
    }

    @Test
    public void testHashTempTableAfterDoubledQuoteIdentifierDoesNotHideMutation() {
        final String sql = "SELECT * FROM \"strange\"\"table\", #tmp; DELETE FROM users";
        final List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("#tmp"), tokens.toString());
        assertFalse(SqlParser.isReadOnlyQuery(sql));
        assertFalse(SqlParser.isNonUpdateQuery(sql));
    }

    @Test
    public void testHashTempTableAfterBracketIdentifierContainingOpeningBracketDoesNotHideMutation() {
        final String sql = "SELECT * FROM [strange[table], #tmp; DELETE FROM users";

        assertTrue(SqlParser.parse(sql).contains("#tmp"));
        assertFalse(SqlParser.isReadOnlyQuery(sql));
        assertFalse(SqlParser.isNonUpdateQuery(sql));
    }

    @Test
    public void testGlobalHashTempTableIsOneIdentifierWhileDoubleHashOperatorRemainsAnOperator() {
        final String sql = "SELECT * FROM ##global_temp, #local_temp";
        final List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("##global_temp"), tokens.toString());
        assertTrue(tokens.contains("#local_temp"), tokens.toString());
        assertFalse(tokens.contains("##"), tokens.toString());
        assertEquals(sql.indexOf("##global_temp"), SqlParser.indexOfToken(sql, "##global_temp"));
        assertEquals("##global_temp", SqlParser.nextToken(sql, sql.indexOf("##global_temp")));
        assertFalse(SqlParser.isSeparator(sql, sql.length(), sql.indexOf("##global_temp"), '#'));
        assertFalse(SqlParser.isSeparator(sql, sql.length(), sql.indexOf("##global_temp") + 1, '#'));

        final List<String> operatorTokens = SqlParser.parse("SELECT 1 ## 2");
        assertTrue(operatorTokens.contains("##"), operatorTokens.toString());
        assertFalse(operatorTokens.contains("## 2"), operatorTokens.toString());
    }

    @Test
    public void testGlobalHashTempTablesRemainIdentifiersInDmlTargetsAndFromLists() {
        assertTrue(SqlParser.parse("INSERT ##target VALUES (1)").contains("##target"));
        assertTrue(SqlParser.parse("DELETE ##target WHERE id = 1").contains("##target"));
        assertTrue(SqlParser.parse("MERGE TOP (1) ##target USING source ON 1 = 1").contains("##target"));
        assertTrue(SqlParser.parse("SELECT * FROM schema.table t, ##global_temp").contains("##global_temp"));
        assertTrue(SqlParser.parse("SELECT * INTO ##global_temp FROM source").contains("##global_temp"));
    }

    @Test
    public void testDoubleHashOperatorDoesNotHideFollowingHashTempTablesOrStatements() {
        final String sql = "SELECT 1 ## 2 FROM #local_temp, ##global_temp; DELETE FROM users";
        final List<String> tokens = SqlParser.parse(sql);

        assertTrue(tokens.contains("##"), tokens.toString());
        assertTrue(tokens.contains("#local_temp"), tokens.toString());
        assertTrue(tokens.contains("##global_temp"), tokens.toString());
        assertTrue(tokens.contains("DELETE"), tokens.toString());
        assertEquals(sql.indexOf("#local_temp"), SqlParser.indexOfToken(sql, "#local_temp"));
        assertEquals(sql.indexOf("##global_temp"), SqlParser.indexOfToken(sql, "##global_temp"));
        assertFalse(SqlParser.isReadOnlyQuery(sql));
        assertFalse(SqlParser.isReadOrInsertQuery(sql));

        final String trailingHashOperator = "SELECT payload ?# path FROM #local_temp; DELETE FROM users";
        final List<String> trailingHashOperatorTokens = SqlParser.parse(trailingHashOperator);
        assertTrue(trailingHashOperatorTokens.contains("?#"), trailingHashOperatorTokens.toString());
        assertTrue(trailingHashOperatorTokens.contains("#local_temp"), trailingHashOperatorTokens.toString());
        assertTrue(trailingHashOperatorTokens.contains("DELETE"), trailingHashOperatorTokens.toString());
    }

    @Test
    public void testReadSafetyGatesRejectUnknownTopLevelStatementVerbs() {
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; VACUUM"));
        assertFalse(SqlParser.isReadOnlyQuery("SELECT 1; GRANT SELECT ON users TO app_user"));
        assertFalse(SqlParser.isReadOrInsertQuery("SELECT 1; PRAGMA journal_mode = WAL"));
        assertFalse(SqlParser.isReadOrInsertQuery("INSERT INTO audit_log VALUES (1); ANALYZE users"));

        final String safeReads = "SELECT '; VACUUM' AS text /* ; ANALYZE */; -- ; DROP\n" + "WITH c AS (SELECT 2) SELECT * FROM c; ;";
        assertTrue(SqlParser.isReadOnlyQuery(safeReads));
        assertTrue(SqlParser.isReadOrInsertQuery(safeReads));
        assertTrue(SqlParser.isReadOrInsertQuery("SELECT 1; INSERT INTO audit_log VALUES (1);"));
    }
}
