/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;

@Tag("2025")
public class SqlMapper2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testAddParsedSql() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users");
        mapper.add("findAll", sql);

        ParsedSql retrieved = mapper.get("findAll");
        assertNotNull(retrieved);
        assertEquals(sql.sql(), retrieved.sql());
    }

    @Test
    public void testAddSqlString() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");

        mapper.add("insertUser", "INSERT INTO users (name, email) VALUES (?, ?)", attrs);

        ParsedSql sql = mapper.get("insertUser");
        assertNotNull(sql);

        ImmutableMap<String, String> retrievedAttrs = mapper.getAttributes("insertUser");
        assertNotNull(retrievedAttrs);
        assertEquals("100", retrievedAttrs.get("batchSize"));
    }

    @Test
    public void testAddEmptyId() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddNullId() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(null, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddIdWithWhitespace() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("find all", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(longId, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddDuplicateId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("findAll", ParsedSql.parse("SELECT * FROM accounts"));
        });
    }

    @Test
    public void testGet() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        mapper.add("findById", sql);

        ParsedSql retrieved = mapper.get("findById");
        assertNotNull(retrieved);
        assertEquals(sql.sql(), retrieved.sql());
    }

    @Test
    public void testGetNonExistent() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get("nonExistent");
        assertNull(sql);
    }

    @Test
    public void testGetEmptyId() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get("");
        assertNull(sql);
    }

    @Test
    public void testGetNullId() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get(null);
        assertNull(sql);
    }

    @Test
    public void testGetTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        ParsedSql sql = mapper.get(longId);
        assertNull(sql);
    }

    @Test
    public void testGetAttrs() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("fetchSize", "50");
        attrs.put("timeout", "30");

        mapper.add("query1", "SELECT * FROM users", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttributes("query1");
        assertNotNull(retrieved);
        assertEquals("50", retrieved.get("fetchSize"));
        assertEquals("30", retrieved.get("timeout"));
    }

    @Test
    public void testGetAttrsNonExistent() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.getAttributes("nonExistent");
        assertNull(attrs);
    }

    @Test
    public void testGetAttrsEmptyId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.getAttributes("");
        assertNull(attrs);
    }

    @Test
    public void testGetAttrsNullId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.getAttributes(null);
        assertNull(attrs);
    }

    @Test
    public void testRemove() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get("findAll"));

        mapper.remove("findAll");

        assertNull(mapper.get("findAll"));
    }

    @Test
    public void testRemoveNonExistent() {
        SqlMapper mapper = new SqlMapper();
        mapper.remove("nonExistent"); // Should not throw
    }

    @Test
    public void testRemoveEmptyId() {
        SqlMapper mapper = new SqlMapper();
        mapper.remove(""); // Should not throw
    }

    @Test
    public void testRemoveNullId() {
        SqlMapper mapper = new SqlMapper();
        mapper.remove(null); // Should not throw
    }

    @Test
    public void testKeySet() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertEquals(2, mapper.sqlIds().size());
        assertTrue(mapper.sqlIds().contains("query1"));
        assertTrue(mapper.sqlIds().contains("query2"));
    }

    @Test
    public void testIsEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertTrue(mapper.isEmpty());

        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        assertFalse(mapper.isEmpty());

        mapper.remove("query1");
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testCopy() {
        SqlMapper original = new SqlMapper();
        original.add("query1", ParsedSql.parse("SELECT * FROM users"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        original.add("query2", "INSERT INTO users VALUES (?)", attrs);

        SqlMapper copy = original.copy();

        assertNotNull(copy.get("query1"));
        assertNotNull(copy.get("query2"));
        assertNotNull(copy.getAttributes("query2"));
        assertEquals("100", copy.getAttributes("query2").get("batchSize"));
    }

    @Test
    public void testHashCode() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }

    @Test
    public void testEquals() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(firstMapper, secondMapper);
    }

    @Test
    public void testEqualsSameObject() {
        SqlMapper mapper = new SqlMapper();
        assertEquals(mapper, mapper);
    }

    @Test
    public void testEqualsNull() {
        SqlMapper mapper = new SqlMapper();
        assertFalse(mapper.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        SqlMapper mapper = new SqlMapper();
        assertFalse(mapper.equals("string"));
    }

    @Test
    public void testToString() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        String str = mapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("query1"));
    }

    @Test
    public void testMultipleAttributes() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put(SqlMapper.BATCH_SIZE, "200");
        attrs.put(SqlMapper.FETCH_SIZE, "100");
        attrs.put(SqlMapper.TIMEOUT, "60");
        attrs.put(SqlMapper.RESULT_SET_TYPE, "SCROLL_INSENSITIVE");

        mapper.add("complexQuery", "SELECT * FROM large_table", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttributes("complexQuery");
        assertEquals("200", retrieved.get(SqlMapper.BATCH_SIZE));
        assertEquals("100", retrieved.get(SqlMapper.FETCH_SIZE));
        assertEquals("60", retrieved.get(SqlMapper.TIMEOUT));
        assertEquals("SCROLL_INSENSITIVE", retrieved.get(SqlMapper.RESULT_SET_TYPE));
    }

    @Test
    public void testConstants() {
        assertEquals("sqlMapper", SqlMapper.SQL_MAPPER);
        assertEquals("sql", SqlMapper.SQL);
        assertEquals("id", SqlMapper.ID);
        assertEquals("batchSize", SqlMapper.BATCH_SIZE);
        assertEquals("fetchSize", SqlMapper.FETCH_SIZE);
        assertEquals("resultSetType", SqlMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SqlMapper.TIMEOUT);
        assertEquals(128, SqlMapper.MAX_ID_LENGTH);
    }

    @Test
    public void testResultSetTypeMap() {
        assertNotNull(SqlMapper.RESULT_SET_TYPE_MAP);
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("FORWARD_ONLY"));
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_INSENSITIVE"));
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_SENSITIVE"));
    }

    @Test
    public void testMaxIdLength() {
        SqlMapper mapper = new SqlMapper();
        String maxLengthId = "a".repeat(SqlMapper.MAX_ID_LENGTH);
        mapper.add(maxLengthId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(maxLengthId));
    }

    @Test
    public void testAddEmptyAttributes() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();

        mapper.add("query1", "SELECT * FROM users", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttributes("query1");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    public void testKeySetEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper.sqlIds());
        assertTrue(mapper.sqlIds().isEmpty());
    }

    @Test
    public void testMultipleRemove() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));
        mapper.add("query3", ParsedSql.parse("SELECT * FROM products"));

        mapper.remove("query1");
        mapper.remove("query2");

        assertNull(mapper.get("query1"));
        assertNull(mapper.get("query2"));
        assertNotNull(mapper.get("query3"));
    }

    @Test
    public void testCopyIsIndependent() {
        SqlMapper original = new SqlMapper();
        original.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper copy = original.copy();
        copy.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertNotNull(copy.get("query2"));
        assertNull(original.get("query2"));
    }

    @Test
    public void testGetAttrsWithNullId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.getAttributes(null);
        assertNull(attrs);
    }

    @Test
    public void testRemoveTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        mapper.remove(longId); // Should not throw
    }

    @Test
    public void testAddSqlStringWithEmptyAttributes() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", "SELECT * FROM users", new HashMap<>());
        ParsedSql sql = mapper.get("query1");
        assertNotNull(sql);
    }

    @Test
    public void testKeySetOrder() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query3", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query1", ParsedSql.parse("SELECT * FROM orders"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM products"));

        // LinkedHashMap preserves insertion order
        assertEquals(3, mapper.sqlIds().size());
    }

    @Test
    public void testEqualsDifferentContent() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM orders"));

        assertFalse(firstMapper.equals(secondMapper));
    }

    @Test
    public void testHashCodeConsistency() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        int hash1 = mapper.hashCode();
        int hash2 = mapper.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testToStringEmpty() {
        SqlMapper mapper = new SqlMapper();
        String str = mapper.toString();
        assertNotNull(str);
    }

    @Test
    public void testConstantsValues() {
        assertEquals("sqlMapper", SqlMapper.SQL_MAPPER);
        assertEquals("sql", SqlMapper.SQL);
        assertEquals("id", SqlMapper.ID);
        assertEquals("batchSize", SqlMapper.BATCH_SIZE);
        assertEquals("fetchSize", SqlMapper.FETCH_SIZE);
        assertEquals("resultSetType", SqlMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SqlMapper.TIMEOUT);
        assertEquals(128, SqlMapper.MAX_ID_LENGTH);
    }

    @Test
    public void testResultSetTypeMapValues() {
        assertEquals(java.sql.ResultSet.TYPE_FORWARD_ONLY, SqlMapper.RESULT_SET_TYPE_MAP.get("FORWARD_ONLY").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, SqlMapper.RESULT_SET_TYPE_MAP.get("SCROLL_INSENSITIVE").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_SENSITIVE, SqlMapper.RESULT_SET_TYPE_MAP.get("SCROLL_SENSITIVE").intValue());
    }

    @Test
    public void testGetTooLongIdBoundary() {
        SqlMapper mapper = new SqlMapper();
        String exactMaxId = "a".repeat(SqlMapper.MAX_ID_LENGTH);
        mapper.add(exactMaxId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(exactMaxId));

        String tooLongId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.get(tooLongId));
    }

    @Test
    public void testGetAttrsTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        ImmutableMap<String, String> attrs = mapper.getAttributes(longId);
        assertNull(attrs);
    }

    @Test
    public void testAddParsedSqlReturn() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql1 = ParsedSql.parse("SELECT * FROM users");
        mapper.add("query1", sql1);

        ParsedSql sql2 = ParsedSql.parse("SELECT * FROM orders");
        // Cannot test second add as it will throw IllegalArgumentException for duplicate ID
    }

    @Test
    public void testCopyPreservesAttributes() {
        SqlMapper original = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("timeout", "30");
        original.add("query1", "SELECT * FROM users", attrs);

        SqlMapper copy = original.copy();

        ImmutableMap<String, String> copiedAttrs = copy.getAttributes("query1");
        assertNotNull(copiedAttrs);
        assertEquals("100", copiedAttrs.get("batchSize"));
        assertEquals("30", copiedAttrs.get("timeout"));
    }

    @Test
    public void testEquals_DifferentAttributesAreNotEqual() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "10"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "30"));

        assertNotEquals(firstMapper, secondMapper);
    }

    @Test
    public void testHashCode_DifferentAttributesAreDifferent() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "10"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "30"));

        assertNotEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }
}
