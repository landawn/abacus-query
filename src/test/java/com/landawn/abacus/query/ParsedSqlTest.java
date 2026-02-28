/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ParsedSqlTest extends TestBase {

    @Test
    public void testParse_simpleSelect() {
        String sql = "SELECT * FROM users";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users", parsed.sql());
        Assertions.assertEquals("SELECT * FROM users", parsed.getParameterizedSql());
        Assertions.assertTrue(parsed.getNamedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.getParameterCount());
    }

    @Test
    public void testParse_withQuestionMarkParameters() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.sql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.getParameterizedSql());
        Assertions.assertTrue(parsed.getNamedParameters().isEmpty());
        Assertions.assertEquals(2, parsed.getParameterCount());
    }

    @Test
    public void testParse_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :userStatus";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId AND status = :userStatus", parsed.sql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.getParameterizedSql());

        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("userStatus", namedParams.get(1));
        Assertions.assertEquals(2, parsed.getParameterCount());
    }

    @Test
    public void testParse_withIBatisStyleParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})", parsed.sql());
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.getParameterizedSql());

        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userName", namedParams.get(0));
        Assertions.assertEquals("userEmail", namedParams.get(1));
        Assertions.assertEquals(2, parsed.getParameterCount());
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

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId;", parsed.sql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.getParameterizedSql());
    }

    @Test
    public void testParse_withWhitespace() {
        String sql = "  SELECT * FROM users WHERE id = :userId  ";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId", parsed.sql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.getParameterizedSql());
    }

    @Test
    public void testParse_withComments() {
        String sql = "-- This is a comment\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withMultilineComments() {
        String sql = "/* This is a\nmultiline comment */\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashComments() {
        String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentsWithoutSpace() {
        String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentAtLineStartWithoutSpace() {
        String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_nonOperationSql() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SET @variable = :value", parsed.sql());
        Assertions.assertEquals("SET @variable = :value", parsed.getParameterizedSql());
        Assertions.assertTrue(parsed.getNamedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.getParameterCount());
    }

    @Test
    public void testParse_withClause() {
        String sql = "WITH cte AS (SELECT * FROM users WHERE status = :status) SELECT * FROM cte WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("status", namedParams.get(0));
        Assertions.assertEquals("id", namedParams.get(1));
        Assertions.assertEquals(2, parsed.getParameterCount());
    }

    @Test
    public void testParse_updateStatement() {
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("UPDATE users SET name = ?, email = ? WHERE id = ?", parsed.getParameterizedSql());
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(3, namedParams.size());
        Assertions.assertEquals("name", namedParams.get(0));
        Assertions.assertEquals("email", namedParams.get(1));
        Assertions.assertEquals("id", namedParams.get(2));
    }

    @Test
    public void testParse_deleteStatement() {
        String sql = "DELETE FROM users WHERE id = :id AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("DELETE FROM users WHERE id = ? AND status = ?", parsed.getParameterizedSql());
        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("status", namedParams.get(1));
    }

    @Test
    public void testParse_mergeStatement() {
        String sql = "MERGE INTO users USING temp ON users.id = :id WHEN MATCHED THEN UPDATE SET name = :name";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.getNamedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("name", namedParams.get(1));
    }

    @Test
    public void testParse_callStatement() {
        String sql = "CALL refresh_user(:userId, :mode)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("CALL refresh_user(?, ?)", parsed.getParameterizedSql());
        List<String> namedParams = parsed.getNamedParameters();
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

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId", parsed.sql());
    }

    @Test
    public void testGetParameterizedSql_forCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        String couchbaseSql = parsed.getParameterizedSql(true);
        Assertions.assertEquals("SELECT * FROM users WHERE id = $1 AND status = $2", couchbaseSql);
    }

    @Test
    public void testGetParameterizedSql_forCouchbase_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        String couchbaseSql = parsed.getParameterizedSql(true);
        Assertions.assertEquals("SELECT * FROM users WHERE id = $1 AND status = $2", couchbaseSql);
    }

    @Test
    public void testGetParameterizedSql_notForCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        String standardSql = parsed.getParameterizedSql(false);
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", standardSql);
    }

    @Test
    public void testGetNamedParameters() {
        String sql = "SELECT * FROM users WHERE age > :minAge AND age < :maxAge";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.getNamedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("minAge", params.get(0));
        Assertions.assertEquals("maxAge", params.get(1));
    }

    @Test
    public void testGetNamedParameters_forCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> couchbaseParams = parsed.getNamedParameters(true);
        Assertions.assertEquals(2, couchbaseParams.size());
        Assertions.assertEquals("userId", couchbaseParams.get(0));
        Assertions.assertEquals("status", couchbaseParams.get(1));
    }

    @Test
    public void testGetNamedParameters_forCouchbase_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> couchbaseParams = parsed.getNamedParameters(true);
        Assertions.assertTrue(couchbaseParams.isEmpty());
    }

    @Test
    public void testGetNamedParameters_forCouchbase_withDollarParameters() {
        String sql = "SELECT * FROM users WHERE id = $1 AND status = $2";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> couchbaseParams = parsed.getNamedParameters(true);
        Assertions.assertTrue(couchbaseParams.isEmpty());
    }

    @Test
    public void testGetNamedParameters_notForCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> standardParams = parsed.getNamedParameters(false);
        Assertions.assertEquals(1, standardParams.size());
        Assertions.assertEquals("userId", standardParams.get(0));
    }

    @Test
    public void testGetParameterCount() {
        String sql = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(3, parsed.getParameterCount());
    }

    @Test
    public void testGetParameterCount_withNamedParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (:name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(2, parsed.getParameterCount());
    }

    @Test
    public void testGetParameterCount_forCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        int couchbaseCount = parsed.getParameterCount(true);
        Assertions.assertEquals(2, couchbaseCount);
    }

    @Test
    public void testGetParameterCount_notForCouchbase() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        int standardCount = parsed.getParameterCount(false);
        Assertions.assertEquals(1, standardCount);
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

        List<String> params = parsed.getNamedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("user_id", params.get(0));
        Assertions.assertEquals("user_name123", params.get(1));
    }

    @Test
    public void testParameterWithSpecialCharacters() {
        // Test that colons not followed by valid parameter characters are not treated as parameters
        String sql = "SELECT * FROM users WHERE time > '10:30:00' AND id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.getNamedParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("userId", params.get(0));
        Assertions.assertEquals(1, parsed.getParameterCount());
    }

    @Test
    public void testMultipleSameNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.getNamedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("id", params.get(0));
        Assertions.assertEquals("id", params.get(1));
        Assertions.assertEquals(2, parsed.getParameterCount());
    }

    @Test
    public void testCouchbaseParameterizedSql_cacheReuse() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        // First call should trigger parsing
        String couchbaseSql1 = parsed.getParameterizedSql(true);
        // Second call should use cached result
        String couchbaseSql2 = parsed.getParameterizedSql(true);

        Assertions.assertEquals(couchbaseSql1, couchbaseSql2);
    }

    @Test
    public void testNonOperationSql_forCouchbase() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        String couchbaseSql = parsed.getParameterizedSql(true);
        Assertions.assertEquals("SET @variable = :value", couchbaseSql);
        Assertions.assertTrue(parsed.getNamedParameters(true).isEmpty());
        Assertions.assertEquals(0, parsed.getParameterCount(true));
    }

    @Test
    public void testInsertStatement() {
        String sql = "INSERT INTO users (id, name, email) VALUES (:id, :name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("INSERT INTO users (id, name, email) VALUES (?, ?, ?)", parsed.getParameterizedSql());
        List<String> params = parsed.getNamedParameters();
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
    public void testCouchbaseNumericParameters() {
        String sql = "SELECT * FROM users WHERE id = $1 AND status = $2";
        ParsedSql parsed = ParsedSql.parse(sql);

        // For Couchbase, numeric parameters should be detected
        List<String> couchbaseParams = parsed.getNamedParameters(true);
        Assertions.assertTrue(couchbaseParams.isEmpty());
    }
}
