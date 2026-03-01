/*
 * Copyright (C) 2025 HaiYang Li
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParsedSql2025Test extends TestBase {

    @Test
    public void testParse_SimpleSelect() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertNotNull(parsed);
        assertEquals("SELECT * FROM users", parsed.sql());
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
        assertEquals("SELECT * FROM users", parsed.sql());
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
    public void testGetParameterizedSql_Couchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        String couchbaseSql = parsed.parameterizedSqlForCouchbase();
        assertNotNull(couchbaseSql);
        assertTrue(couchbaseSql.contains("$1"));
    }

    @Test
    public void testGetParameterizedSql_CouchbaseWithNamedParams() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :userName");
        String couchbaseSql = parsed.parameterizedSqlForCouchbase();
        assertNotNull(couchbaseSql);
        assertTrue(couchbaseSql.contains("$1"));
        assertTrue(couchbaseSql.contains("$2"));
    }

    @Test
    public void testGetParameterizedSql_NotCouchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ?", sql);
    }

    @Test
    public void testGetNamedParameters_Couchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        parsed.parameterizedSqlForCouchbase();
        assertNotNull(parsed.namedParametersForCouchbase());
    }

    @Test
    public void testGetParameterCount_Couchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = ?");
        int count = parsed.parameterCountForCouchbase();
        assertEquals(2, count);
    }

    @Test
    public void testSql() {
        String originalSql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(originalSql);
        assertEquals(originalSql, parsed.sql());
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
    public void testParse_ComplexNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :user_id_123");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("user_id_123", parsed.namedParameters().get(0));
    }
}
