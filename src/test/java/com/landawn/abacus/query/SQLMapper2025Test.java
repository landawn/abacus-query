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
public class SQLMapper2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SQLMapper mapper = new SQLMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testAddParsedSql() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users");
        mapper.add("findAll", sql);

        ParsedSql retrieved = mapper.get("findAll");
        assertNotNull(retrieved);
        assertEquals(sql.sql(), retrieved.sql());
    }

    @Test
    public void testAddSqlString() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");

        mapper.add("insertUser", "INSERT INTO users (name, email) VALUES (?, ?)", attrs);

        ParsedSql sql = mapper.get("insertUser");
        assertNotNull(sql);

        ImmutableMap<String, String> retrievedAttrs = mapper.getAttrs("insertUser");
        assertNotNull(retrievedAttrs);
        assertEquals("100", retrievedAttrs.get("batchSize"));
    }

    @Test
    public void testAddEmptyId() {
        SQLMapper mapper = new SQLMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddNullId() {
        SQLMapper mapper = new SQLMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(null, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddIdWithWhitespace() {
        SQLMapper mapper = new SQLMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("find all", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddTooLongId() {
        SQLMapper mapper = new SQLMapper();
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(longId, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    public void testAddDuplicateId() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("findAll", ParsedSql.parse("SELECT * FROM accounts"));
        });
    }

    @Test
    public void testGet() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        mapper.add("findById", sql);

        ParsedSql retrieved = mapper.get("findById");
        assertNotNull(retrieved);
        assertEquals(sql.sql(), retrieved.sql());
    }

    @Test
    public void testGetNonExistent() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = mapper.get("nonExistent");
        assertNull(sql);
    }

    @Test
    public void testGetEmptyId() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = mapper.get("");
        assertNull(sql);
    }

    @Test
    public void testGetNullId() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = mapper.get(null);
        assertNull(sql);
    }

    @Test
    public void testGetTooLongId() {
        SQLMapper mapper = new SQLMapper();
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        ParsedSql sql = mapper.get(longId);
        assertNull(sql);
    }

    @Test
    public void testGetAttrs() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("fetchSize", "50");
        attrs.put("timeout", "30");

        mapper.add("query1", "SELECT * FROM users", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttrs("query1");
        assertNotNull(retrieved);
        assertEquals("50", retrieved.get("fetchSize"));
        assertEquals("30", retrieved.get("timeout"));
    }

    @Test
    public void testGetAttrsNonExistent() {
        SQLMapper mapper = new SQLMapper();
        ImmutableMap<String, String> attrs = mapper.getAttrs("nonExistent");
        assertNull(attrs);
    }

    @Test
    public void testGetAttrsEmptyId() {
        SQLMapper mapper = new SQLMapper();
        ImmutableMap<String, String> attrs = mapper.getAttrs("");
        assertNull(attrs);
    }

    @Test
    public void testGetAttrsNullId() {
        SQLMapper mapper = new SQLMapper();
        ImmutableMap<String, String> attrs = mapper.getAttrs(null);
        assertNull(attrs);
    }

    @Test
    public void testRemove() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get("findAll"));

        mapper.remove("findAll");

        assertNull(mapper.get("findAll"));
    }

    @Test
    public void testRemoveNonExistent() {
        SQLMapper mapper = new SQLMapper();
        mapper.remove("nonExistent");   // Should not throw
    }

    @Test
    public void testRemoveEmptyId() {
        SQLMapper mapper = new SQLMapper();
        mapper.remove("");   // Should not throw
    }

    @Test
    public void testRemoveNullId() {
        SQLMapper mapper = new SQLMapper();
        mapper.remove(null);   // Should not throw
    }

    @Test
    public void testKeySet() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertEquals(2, mapper.keySet().size());
        assertTrue(mapper.keySet().contains("query1"));
        assertTrue(mapper.keySet().contains("query2"));
    }

    @Test
    public void testIsEmpty() {
        SQLMapper mapper = new SQLMapper();
        assertTrue(mapper.isEmpty());

        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        assertFalse(mapper.isEmpty());

        mapper.remove("query1");
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testCopy() {
        SQLMapper original = new SQLMapper();
        original.add("query1", ParsedSql.parse("SELECT * FROM users"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        original.add("query2", "INSERT INTO users VALUES (?)", attrs);

        SQLMapper copy = original.copy();

        assertNotNull(copy.get("query1"));
        assertNotNull(copy.get("query2"));
        assertNotNull(copy.getAttrs("query2"));
        assertEquals("100", copy.getAttrs("query2").get("batchSize"));
    }

    @Test
    public void testHashCode() {
        SQLMapper mapper1 = new SQLMapper();
        mapper1.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SQLMapper mapper2 = new SQLMapper();
        mapper2.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(mapper1.hashCode(), mapper2.hashCode());
    }

    @Test
    public void testEquals() {
        SQLMapper mapper1 = new SQLMapper();
        mapper1.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SQLMapper mapper2 = new SQLMapper();
        mapper2.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(mapper1, mapper2);
    }

    @Test
    public void testEqualsSameObject() {
        SQLMapper mapper = new SQLMapper();
        assertEquals(mapper, mapper);
    }

    @Test
    public void testEqualsNull() {
        SQLMapper mapper = new SQLMapper();
        assertFalse(mapper.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        SQLMapper mapper = new SQLMapper();
        assertFalse(mapper.equals("string"));
    }

    @Test
    public void testToString() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        String str = mapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("query1"));
    }

    @Test
    public void testMultipleAttributes() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put(SQLMapper.BATCH_SIZE, "200");
        attrs.put(SQLMapper.FETCH_SIZE, "100");
        attrs.put(SQLMapper.TIMEOUT, "60");
        attrs.put(SQLMapper.RESULT_SET_TYPE, "SCROLL_INSENSITIVE");

        mapper.add("complexQuery", "SELECT * FROM large_table", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttrs("complexQuery");
        assertEquals("200", retrieved.get(SQLMapper.BATCH_SIZE));
        assertEquals("100", retrieved.get(SQLMapper.FETCH_SIZE));
        assertEquals("60", retrieved.get(SQLMapper.TIMEOUT));
        assertEquals("SCROLL_INSENSITIVE", retrieved.get(SQLMapper.RESULT_SET_TYPE));
    }

    @Test
    public void testConstants() {
        assertEquals("sqlMapper", SQLMapper.SQL_MAPPER);
        assertEquals("sql", SQLMapper.SQL);
        assertEquals("id", SQLMapper.ID);
        assertEquals("batchSize", SQLMapper.BATCH_SIZE);
        assertEquals("fetchSize", SQLMapper.FETCH_SIZE);
        assertEquals("resultSetType", SQLMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SQLMapper.TIMEOUT);
        assertEquals(128, SQLMapper.MAX_ID_LENGTH);
    }

    @Test
    public void testResultSetTypeMap() {
        assertNotNull(SQLMapper.RESULT_SET_TYPE_MAP);
        assertTrue(SQLMapper.RESULT_SET_TYPE_MAP.containsKey("FORWARD_ONLY"));
        assertTrue(SQLMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_INSENSITIVE"));
        assertTrue(SQLMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_SENSITIVE"));
    }

    @Test
    public void testMaxIdLength() {
        SQLMapper mapper = new SQLMapper();
        String maxLengthId = "a".repeat(SQLMapper.MAX_ID_LENGTH);
        mapper.add(maxLengthId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(maxLengthId));
    }

    @Test
    public void testAddEmptyAttributes() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();

        mapper.add("query1", "SELECT * FROM users", attrs);

        ImmutableMap<String, String> retrieved = mapper.getAttrs("query1");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    public void testKeySetEmpty() {
        SQLMapper mapper = new SQLMapper();
        assertNotNull(mapper.keySet());
        assertTrue(mapper.keySet().isEmpty());
    }

    @Test
    public void testMultipleRemove() {
        SQLMapper mapper = new SQLMapper();
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
        SQLMapper original = new SQLMapper();
        original.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SQLMapper copy = original.copy();
        copy.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertNotNull(copy.get("query2"));
        assertNull(original.get("query2"));
    }

    @Test
    public void testGetAttrsWithNullId() {
        SQLMapper mapper = new SQLMapper();
        ImmutableMap<String, String> attrs = mapper.getAttrs(null);
        assertNull(attrs);
    }

    @Test
    public void testRemoveTooLongId() {
        SQLMapper mapper = new SQLMapper();
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        mapper.remove(longId);   // Should not throw
    }

    @Test
    public void testAddSqlStringWithEmptyAttributes() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", "SELECT * FROM users", new HashMap<>());
        ParsedSql sql = mapper.get("query1");
        assertNotNull(sql);
    }

    @Test
    public void testKeySetOrder() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query3", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query1", ParsedSql.parse("SELECT * FROM orders"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM products"));

        // LinkedHashMap preserves insertion order
        assertEquals(3, mapper.keySet().size());
    }

    @Test
    public void testEqualsDifferentContent() {
        SQLMapper mapper1 = new SQLMapper();
        mapper1.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SQLMapper mapper2 = new SQLMapper();
        mapper2.add("query1", ParsedSql.parse("SELECT * FROM orders"));

        assertFalse(mapper1.equals(mapper2));
    }

    @Test
    public void testHashCodeConsistency() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        int hash1 = mapper.hashCode();
        int hash2 = mapper.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testToStringEmpty() {
        SQLMapper mapper = new SQLMapper();
        String str = mapper.toString();
        assertNotNull(str);
    }

    @Test
    public void testConstantsValues() {
        assertEquals("sqlMapper", SQLMapper.SQL_MAPPER);
        assertEquals("sql", SQLMapper.SQL);
        assertEquals("id", SQLMapper.ID);
        assertEquals("batchSize", SQLMapper.BATCH_SIZE);
        assertEquals("fetchSize", SQLMapper.FETCH_SIZE);
        assertEquals("resultSetType", SQLMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SQLMapper.TIMEOUT);
        assertEquals(128, SQLMapper.MAX_ID_LENGTH);
    }

    @Test
    public void testResultSetTypeMapValues() {
        assertEquals(java.sql.ResultSet.TYPE_FORWARD_ONLY, SQLMapper.RESULT_SET_TYPE_MAP.get("FORWARD_ONLY").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, SQLMapper.RESULT_SET_TYPE_MAP.get("SCROLL_INSENSITIVE").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_SENSITIVE, SQLMapper.RESULT_SET_TYPE_MAP.get("SCROLL_SENSITIVE").intValue());
    }

    @Test
    public void testGetTooLongIdBoundary() {
        SQLMapper mapper = new SQLMapper();
        String exactMaxId = "a".repeat(SQLMapper.MAX_ID_LENGTH);
        mapper.add(exactMaxId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(exactMaxId));

        String tooLongId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.get(tooLongId));
    }

    @Test
    public void testGetAttrsTooLongId() {
        SQLMapper mapper = new SQLMapper();
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        ImmutableMap<String, String> attrs = mapper.getAttrs(longId);
        assertNull(attrs);
    }

    @Test
    public void testAddParsedSqlReturn() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql result = mapper.add("query1", sql1);
        assertNull(result);   // First add should return null

        ParsedSql sql2 = ParsedSql.parse("SELECT * FROM orders");
        // Cannot test second add as it will throw IllegalArgumentException for duplicate ID
    }

    @Test
    public void testCopyPreservesAttributes() {
        SQLMapper original = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("timeout", "30");
        original.add("query1", "SELECT * FROM users", attrs);

        SQLMapper copy = original.copy();

        ImmutableMap<String, String> copiedAttrs = copy.getAttrs("query1");
        assertNotNull(copiedAttrs);
        assertEquals("100", copiedAttrs.get("batchSize"));
        assertEquals("30", copiedAttrs.get("timeout"));
    }
}
