package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class ParsedSql2025Test extends TestBase {

    @Test
    public void testParse_SimpleSelect() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertNotNull(parsed);
        assertEquals("SELECT * FROM users", parsed.originalSql());
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertEquals(0, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithQuestionMarkParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithMultipleQuestionMarks() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > ? AND status = ?");
        assertEquals("SELECT * FROM users WHERE age > ? AND status = ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("userId", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_WithMultipleNamedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > :minAge AND age < :maxAge");
        assertEquals("SELECT * FROM users WHERE age > ? AND age < ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals(2, parsed.namedParameters().size());
        assertEquals("minAge", parsed.namedParameters().get(0));
        assertEquals("maxAge", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_WithIBatisParameter() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals(2, parsed.namedParameters().size());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("email", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_WithIBatisParameterOptions() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (id) VALUES (#{id,jdbcType=INTEGER})");
        assertEquals("INSERT INTO users (id) VALUES (?)", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_MixedParametersThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = :name");
        });
    }

    @Test
    public void testParse_MixedIBatisAndNamedThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = :id AND name = #{name}");
        });
    }

    @Test
    public void testParse_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(null);
        });
    }

    @Test
    public void testParse_EmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("");
        });
    }

    @Test
    public void testParse_WithTrailingSemicolon() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users;");
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertFalse(parsed.parameterizedSql().endsWith(";"));
    }

    @Test
    public void testParse_WithWhitespace() {
        ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users  ");
        assertEquals("SELECT * FROM users", parsed.originalSql());
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
    }

    @Test
    public void testParse_Caching() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed1 = ParsedSql.parse(sql);
        ParsedSql parsed2 = ParsedSql.parse(sql);
        assertSame(parsed1, parsed2);
    }

    @Test
    public void testGetParameterizedSql_WithQuestionMarkParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        String sql = parsed.parameterizedSql();
        assertNotNull(sql);
        assertEquals("SELECT * FROM users WHERE id = ?", sql);
    }

    @Test
    public void testGetParameterizedSql_WithNamedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :userName");
        String sql = parsed.parameterizedSql();
        assertNotNull(sql);
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", sql);
    }

    @Test
    public void testGetParameterizedSql_WithSingleNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ?", sql);
    }

    @Test
    public void testGetNamedParameters_WithNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertNotNull(parsed.namedParameters());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("userId", parsed.namedParameters().get(0));
    }

    @Test
    public void testGetParameterCount_WithQuestionMarkParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = ?");
        int count = parsed.parameterCount();
        assertEquals(2, count);
    }

    @Test
    public void testSql() {
        String originalSql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(originalSql);
        assertEquals(originalSql, parsed.originalSql());
    }

    @Test
    public void testHashCode() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        assertEquals(parsed1.hashCode(), parsed2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertTrue(parsed.equals(parsed));
    }

    @Test
    public void testEquals_EqualObjects() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        assertTrue(parsed1.equals(parsed2));
    }

    @Test
    public void testEquals_DifferentObjects() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM orders");
        assertFalse(parsed1.equals(parsed2));
    }

    @Test
    public void testEquals_Null() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertFalse(parsed.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertFalse(parsed.equals("SELECT * FROM users"));
    }

    @Test
    public void testToString() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String str = parsed.toString();
        assertNotNull(str);
        assertTrue(str.contains("SELECT * FROM users WHERE id = :userId"));
        assertTrue(str.contains("SELECT * FROM users WHERE id = ?"));
    }

    @Test
    public void testParse_Update() {
        ParsedSql parsed = ParsedSql.parse("UPDATE users SET name = :name WHERE id = :id");
        assertEquals("UPDATE users SET name = ? WHERE id = ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("id", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_Delete() {
        ParsedSql parsed = ParsedSql.parse("DELETE FROM users WHERE id = :id");
        assertEquals("DELETE FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_Insert() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, age) VALUES (:name, :age)");
        assertEquals("INSERT INTO users (name, age) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("age", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_Call() {
        ParsedSql parsed = ParsedSql.parse("CALL refresh_user(:userId, :mode)");
        assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("userId", parsed.namedParameters().get(0));
        assertEquals("mode", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_ReplaceStatement() {
        ParsedSql parsed = ParsedSql.parse("REPLACE INTO users (id, name) VALUES (:id, :name)");
        assertEquals("REPLACE INTO users (id, name) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
        assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_NonQueryStatement() {
        ParsedSql parsed = ParsedSql.parse("CREATE TABLE users (id INT)");
        assertEquals("CREATE TABLE users (id INT)", parsed.parameterizedSql());
        assertEquals(0, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithComments() {
        ParsedSql parsed = ParsedSql.parse("-- Comment\nSELECT * FROM users WHERE id = :id");
        assertNotNull(parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
    }

    @Test
    public void testParse_ParenthesizedSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("(SELECT :id)");
        assertEquals("(SELECT ?)", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ExplainSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("EXPLAIN SELECT * FROM users WHERE id = :id");
        assertEquals("EXPLAIN SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ExplainAnalyzeSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("EXPLAIN ANALYZE SELECT * FROM users WHERE id = :id");
        assertEquals("EXPLAIN ANALYZE SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ValuesPrefix() {
        ParsedSql parsed = ParsedSql.parse("VALUES(:id, :name)");
        assertEquals("VALUES(?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
        assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_ComplexNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :user_id_123");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("user_id_123", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_NamedParameterWithSuffix() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id::int");
        assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_IbatisNamedParameterWithSuffix() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{id}::int");
        assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }
}

public class ParsedSqlTest extends TestBase {

    @Test
    public void testParse_simpleSelect() {
        String sql = "SELECT * FROM users";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_withQuestionMarkParameters() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :userStatus";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId AND status = :userStatus", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("userStatus", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withIBatisStyleParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})", parsed.originalSql());
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userName", namedParams.get(0));
        Assertions.assertEquals("userEmail", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withMixedParameters_throwsException() {
        String sql = "SELECT * FROM users WHERE id = ? AND name = :userName";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(sql);
        });
    }

    @Test
    public void testParse_withTrailingSemicolon() {
        String sql = "SELECT * FROM users WHERE id = :userId;";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId;", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
    }

    @Test
    public void testParse_withWhitespace() {
        String sql = "  SELECT * FROM users WHERE id = :userId  ";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
    }

    @Test
    public void testParse_withComments() {
        String sql = "-- This is a comment\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withMultilineComments() {
        String sql = "/* This is a\nmultiline comment */\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashComments() {
        String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentsWithoutSpace() {
        String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentAtLineStartWithoutSpace() {
        String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withIbatisParameterOptions() {
        String sql = "INSERT INTO users (id) VALUES (#{id,jdbcType=INTEGER})";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("INSERT INTO users (id) VALUES (?)", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
    }

    @Test
    public void testParse_nonOperationSql() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SET @variable = :value", parsed.originalSql());
        Assertions.assertEquals("SET @variable = :value", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_withClause() {
        String sql = "WITH cte AS (SELECT * FROM users WHERE status = :status) SELECT * FROM cte WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("status", namedParams.get(0));
        Assertions.assertEquals("id", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_parenthesizedSelectPrefix() {
        String sql = "(SELECT :id)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("(SELECT ?)", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_updateStatement() {
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("UPDATE users SET name = ?, email = ? WHERE id = ?", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(3, namedParams.size());
        Assertions.assertEquals("name", namedParams.get(0));
        Assertions.assertEquals("email", namedParams.get(1));
        Assertions.assertEquals("id", namedParams.get(2));
    }

    @Test
    public void testParse_deleteStatement() {
        String sql = "DELETE FROM users WHERE id = :id AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("DELETE FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("status", namedParams.get(1));
    }

    @Test
    public void testParse_mergeStatement() {
        String sql = "MERGE INTO users USING temp ON users.id = :id WHEN MATCHED THEN UPDATE SET name = :name";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("name", namedParams.get(1));
    }

    @Test
    public void testParse_callStatement() {
        String sql = "CALL refresh_user(:userId, :mode)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("mode", namedParams.get(1));
    }

    @Test
    public void testParse_nullSql_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(null);
        });
    }

    @Test
    public void testParse_emptySql_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("");
        });
    }

    @Test
    public void testParse_cacheReuse() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed1 = ParsedSql.parse(sql);
        ParsedSql parsed2 = ParsedSql.parse(sql);

        // Should return the same cached instance
        Assertions.assertSame(parsed1, parsed2);
    }

    @Test
    public void testSql() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId", parsed.originalSql());
    }

    @Test
    public void testGetParameterizedSql_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
    }

    @Test
    public void testGetParameterizedSql_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
    }

    @Test
    public void testGetParameterizedSql_withSingleNamedParameter() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parameterizedSql);
    }

    @Test
    public void testParameterizedSql_cacheReuse() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        // First call should trigger parsing
        String parameterizedSql1 = parsed.parameterizedSql();
        // Second call should use cached result
        String parameterizedSql2 = parsed.parameterizedSql();

        Assertions.assertEquals(parameterizedSql1, parameterizedSql2);
    }

    @Test
    public void testGetNamedParameters() {
        String sql = "SELECT * FROM users WHERE age > :minAge AND age < :maxAge";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("minAge", params.get(0));
        Assertions.assertEquals("maxAge", params.get(1));
    }

    @Test
    public void testGetNamedParameters_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("status", namedParams.get(1));
    }

    @Test
    public void testGetNamedParameters_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertTrue(namedParams.isEmpty());
    }

    @Test
    public void testGetNamedParameters_withSingleNamedParameter() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testGetParameterCount() {
        String sql = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(3, parsed.parameterCount());
    }

    @Test
    public void testGetParameterCount_withNamedParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (:name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testGetParameterCount_withNamedParametersInSelect() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        int parameterCount = parsed.parameterCount();
        Assertions.assertEquals(2, parameterCount);
    }

    @Test
    public void testGetParameterCount_withSingleNamedParameterInSelect() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        int parameterCount = parsed.parameterCount();
        Assertions.assertEquals(1, parameterCount);
    }

    @Test
    public void testHashCode() {
        String sql1 = "SELECT * FROM users WHERE id = :userId";
        String sql2 = "SELECT * FROM users WHERE id = :userId";
        String sql3 = "SELECT * FROM users WHERE id = :id";

        ParsedSql parsed1 = ParsedSql.parse(sql1);
        ParsedSql parsed2 = ParsedSql.parse(sql2);
        ParsedSql parsed3 = ParsedSql.parse(sql3);

        Assertions.assertEquals(parsed1.hashCode(), parsed2.hashCode());
        Assertions.assertNotEquals(parsed1.hashCode(), parsed3.hashCode());
    }

    @Test
    public void testEquals() {
        String sql1 = "SELECT * FROM users WHERE id = :userId";
        String sql2 = "SELECT * FROM users WHERE id = :userId";
        String sql3 = "SELECT * FROM users WHERE id = :id";

        ParsedSql parsed1 = ParsedSql.parse(sql1);
        ParsedSql parsed2 = ParsedSql.parse(sql2);
        ParsedSql parsed3 = ParsedSql.parse(sql3);

        // Test equals with same object
        Assertions.assertEquals(parsed1, parsed1);

        // Test equals with equivalent object
        Assertions.assertEquals(parsed1, parsed2);

        // Test not equals with different SQL
        Assertions.assertNotEquals(parsed1, parsed3);

        // Test not equals with null
        Assertions.assertNotEquals(parsed1, null);

        // Test not equals with different type
        Assertions.assertNotEquals(parsed1, "SELECT * FROM users WHERE id = :userId");
    }

    @Test
    public void testToString() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        String str = parsed.toString();
        Assertions.assertTrue(str.contains("sql=SELECT * FROM users WHERE id = :userId"));
        Assertions.assertTrue(str.contains("parameterizedSql=SELECT * FROM users WHERE id = ?"));
    }

    @Test
    public void testComplexNamedParameters() {
        String sql = "SELECT * FROM users WHERE user_id = :user_id AND user_name123 = :user_name123";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("user_id", params.get(0));
        Assertions.assertEquals("user_name123", params.get(1));
    }

    @Test
    public void testParameterWithSpecialCharacters() {
        // Test that colons not followed by valid parameter characters are not treated as parameters
        String sql = "SELECT * FROM users WHERE time > '10:30:00' AND id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("userId", params.get(0));
        Assertions.assertEquals(1, parsed.parameterCount());
    }

    @Test
    public void testMultipleSameNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("id", params.get(0));
        Assertions.assertEquals("id", params.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testNonOperationSql() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SET @variable = :value", parameterizedSql);
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testInsertStatement() {
        String sql = "INSERT INTO users (id, name, email) VALUES (:id, :name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("INSERT INTO users (id, name, email) VALUES (?, ?, ?)", parsed.parameterizedSql());
        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals("id", params.get(0));
        Assertions.assertEquals("name", params.get(1));
        Assertions.assertEquals("email", params.get(2));
    }

    @Test
    public void testMixedParameterStyles_iBatisAndNamed_throwsException() {
        String sql = "SELECT * FROM users WHERE id = #{userId} AND status = :status";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(sql);
        });
    }

    @Test
    public void testDollarNumericParameters_areNotNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = $1 AND status = $2";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertTrue(namedParams.isEmpty());
    }

    @Test
    public void testParse_malformedIbatisParameter_missingClosingBracket_throwsException() {
        // Bug fix: #{param without closing } should throw, not silently corrupt SQL
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("INSERT INTO users (id) VALUES (#{userId)");
        });
    }

    @Test
    public void testParse_malformedIbatisParameter_missingClosingBracketAtEnd_throwsException() {
        // #{param at end of SQL without closing }
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = #{userId");
        });
    }

    @Test
    public void testParse_malformedIbatisParameter_withTrailingTokensConsumed_throwsException() {
        // Bug fix: the while-loop was consuming all remaining tokens when } was missing,
        // which silently lost subsequent parameters and corrupted SQL structure
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("INSERT INTO t VALUES (#{id}, #{name)");
        });
    }

    @Test
    public void testParse_validIbatisParameter_stillWorks() {
        // Ensure the fix doesn't break valid iBatis parameters
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a, b) VALUES (#{id}, #{name})");
        Assertions.assertEquals("INSERT INTO t (a, b) VALUES (?, ?)", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
        Assertions.assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_validIbatisParameterWithOptions_stillWorks() {
        // Ensure the fix doesn't break valid iBatis parameters with options like jdbcType
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a) VALUES (#{id,jdbcType=INTEGER})");
        Assertions.assertEquals("INSERT INTO t (a) VALUES (?)", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }
}

class ParsedSqlJavadocExamples extends TestBase {

    @Test
    public void testParsedSql_classLevelExample() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String parameterized = parsed.parameterizedSql();
        List<String> params = parsed.namedParameters();

        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterized);
        assertEquals(Arrays.asList("userId", "status"), params);
    }

    @Test
    public void testParsedSql_parseNamedParameters() {
        ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertEquals("SELECT * FROM users WHERE id = ?", ps1.parameterizedSql());
    }

    @Test
    public void testParsedSql_parseIBatisStyle() {
        ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
        assertEquals(Arrays.asList("name", "email"), ps2.namedParameters());
    }

    @Test
    public void testParsedSql_parseJdbcPlaceholders() {
        ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
        assertEquals(2, ps3.parameterCount());
    }

    @Test
    public void testParsedSql_sql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String original = parsed.originalSql();
        assertEquals("SELECT * FROM users WHERE id = :userId", original);
    }

    @Test
    public void testParsedSql_parameterizedSql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", sql);
    }

    @Test
    public void testParsedSql_parameterizedSqlForJdbc() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
        String jdbcSql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", jdbcSql);
    }

    @Test
    public void testParsedSql_namedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
        ImmutableList<String> params = parsed.namedParameters();
        assertEquals(Arrays.asList("name", "minAge"), params);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        ImmutableList<String> params2 = parsed2.namedParameters();
        assertTrue(params2.isEmpty());
    }

    @Test
    public void testParsedSql_parameterCount() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
        int count = parsed.parameterCount();
        assertEquals(3, count);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        int count2 = parsed2.parameterCount();
        assertEquals(0, count2);
    }
}
