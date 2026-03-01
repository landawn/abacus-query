package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;

public class SQLMapperTest extends TestBase {

    @TempDir
    File tempDir;

    @Test
    public void testEmptyConstructor() {
        SQLMapper mapper = new SQLMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
        assertTrue(mapper.sqlIds().isEmpty());
    }

    @Test
    public void testFromFile() throws IOException {
        // Create test XML file
        File xmlFile = new File(tempDir, "test-sql-mapper.xml");
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<sqlMapper>\n");
            writer.write("  <sql id=\"findById\">SELECT * FROM users WHERE id = ?</sql>\n");
            writer.write("  <sql id=\"updateName\" batchSize=\"100\" fetchSize=\"50\" timeout=\"30\">UPDATE users SET name = ? WHERE id = ?</sql>\n");
            writer.write("  <sql id=\"deleteById\">DELETE FROM users WHERE id = ?</sql>\n");
            writer.write("</sqlMapper>\n");
        }

        // Test loading from file
        SQLMapper mapper = SQLMapper.fromFile(xmlFile.getAbsolutePath());
        assertNotNull(mapper);
        assertFalse(mapper.isEmpty());

        // Verify loaded SQLs
        assertEquals(3, mapper.sqlIds().size());
        assertTrue(mapper.sqlIds().contains("findById"));
        assertTrue(mapper.sqlIds().contains("updateName"));
        assertTrue(mapper.sqlIds().contains("deleteById"));

        // Check SQL content
        ParsedSql findById = mapper.get("findById");
        assertNotNull(findById);
        assertEquals("SELECT * FROM users WHERE id = ?", findById.sql());

        // Check attributes
        ImmutableMap<String, String> attrs = mapper.getAttributes("updateName");
        assertNotNull(attrs);
        assertEquals("100", attrs.get("batchSize"));
        assertEquals("50", attrs.get("fetchSize"));
        assertEquals("30", attrs.get("timeout"));
    }

    @Test
    public void testFromFileMultiple() throws IOException {
        // Create first XML file
        File xmlFile1 = new File(tempDir, "users.xml");
        try (FileWriter writer = new FileWriter(xmlFile1)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<sqlMapper>\n");
            writer.write("  <sql id=\"findUser\">SELECT * FROM users WHERE id = ?</sql>\n");
            writer.write("</sqlMapper>\n");
        }

        // Create second XML file
        File xmlFile2 = new File(tempDir, "orders.xml");
        try (FileWriter writer = new FileWriter(xmlFile2)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<sqlMapper>\n");
            writer.write("  <sql id=\"findOrder\">SELECT * FROM orders WHERE id = ?</sql>\n");
            writer.write("</sqlMapper>\n");
        }

        // Test loading multiple files with comma separator
        String paths = xmlFile1.getAbsolutePath() + "," + xmlFile2.getAbsolutePath();
        SQLMapper mapper = SQLMapper.fromFile(paths);

        assertEquals(2, mapper.sqlIds().size());
        assertNotNull(mapper.get("findUser"));
        assertNotNull(mapper.get("findOrder"));

        // Test loading multiple files with semicolon separator
        paths = xmlFile1.getAbsolutePath() + ";" + xmlFile2.getAbsolutePath();
        mapper = SQLMapper.fromFile(paths);

        assertEquals(2, mapper.sqlIds().size());
        assertNotNull(mapper.get("findUser"));
        assertNotNull(mapper.get("findOrder"));
    }

    @Test
    public void testFromFileNoSqlMapperElement() throws IOException {
        File xmlFile = new File(tempDir, "invalid.xml");
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<root></root>\n");
        }

        assertThrows(RuntimeException.class, () -> SQLMapper.fromFile(xmlFile.getAbsolutePath()));
    }

    @Test
    public void testKeySet() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM table1"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM table2"));

        Set<String> keys = mapper.sqlIds();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("query1"));
        assertTrue(keys.contains("query2"));
    }

    @Test
    public void testGet() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users");
        mapper.add("findAll", sql);

        // Test valid get
        ParsedSql retrieved = mapper.get("findAll");
        assertEquals(sql, retrieved);

        // Test get with empty id
        assertNull(mapper.get(""));
        assertNull(mapper.get(null));

        // Test get with id too long
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.get(longId));

        // Test get non-existent
        assertNull(mapper.get("nonExistent"));
    }

    @Test
    public void testGetAttrs() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("timeout", "30");

        mapper.add("query1", "SELECT * FROM users", attrs);

        // Test valid getAttrs
        ImmutableMap<String, String> retrieved = mapper.getAttributes("query1");
        assertNotNull(retrieved);
        assertEquals("100", retrieved.get("batchSize"));
        assertEquals("30", retrieved.get("timeout"));

        // Test getAttrs with empty id
        assertNull(mapper.getAttributes(""));
        assertNull(mapper.getAttributes(null));

        // Test getAttrs with id too long
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.getAttributes(longId));

        // Test getAttrs non-existent
        assertNull(mapper.getAttributes("nonExistent"));
    }

    @Test
    public void testAddParsedSql() {
        SQLMapper mapper = new SQLMapper();

        // Test normal add
        ParsedSql sql1 = ParsedSql.parse("SELECT * FROM users");
        mapper.add("query1", sql1);

        // Test replace
        ParsedSql sql2 = ParsedSql.parse("SELECT id, name FROM users");
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query1", sql2));

        // Test add with empty id
        assertThrows(IllegalArgumentException.class, () -> mapper.add("", sql1));

        // Test add with null id
        assertThrows(IllegalArgumentException.class, () -> mapper.add(null, sql1));

        // Test add with whitespace in id
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query with space", sql1));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query\ttab", sql1));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query\nnewline", sql1));

        // Test add with id too long
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> mapper.add(longId, sql1));
    }

    @Test
    public void testAddStringWithAttrs() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "50");

        // Test normal add
        mapper.add("query1", "SELECT * FROM users", attrs);

        ParsedSql sql = mapper.get("query1");
        assertNotNull(sql);
        assertEquals("SELECT * FROM users", sql.sql());

        ImmutableMap<String, String> retrievedAttrs = mapper.getAttributes("query1");
        assertEquals("50", retrievedAttrs.get("batchSize"));

        // Test duplicate id
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query1", "SELECT * FROM orders", new HashMap<>()));
    }

    @Test
    public void testRemove() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        // Test valid remove
        assertTrue(mapper.sqlIds().contains("query1"));
        mapper.remove("query1");
        assertFalse(mapper.sqlIds().contains("query1"));

        // Test remove non-existent (should not throw)
        mapper.remove("nonExistent");

        // Test remove with empty id (should not throw)
        mapper.remove("");
        mapper.remove(null);

        // Test remove with id too long (should not throw)
        String longId = "a".repeat(SQLMapper.MAX_ID_LENGTH + 1);
        mapper.remove(longId);
    }

    @Test
    public void testCopy() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("timeout", "60");

        mapper.add("query1", "SELECT * FROM users", attrs);
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        SQLMapper copy = mapper.copy();

        // Verify copy has same content
        assertEquals(mapper.sqlIds(), copy.sqlIds());
        assertEquals(mapper.get("query1"), copy.get("query1"));
        assertEquals(mapper.get("query2"), copy.get("query2"));
        assertEquals(mapper.getAttributes("query1"), copy.getAttributes("query1"));

        // Verify copy is independent
        copy.add("query3", ParsedSql.parse("SELECT * FROM products"));
        assertFalse(mapper.sqlIds().contains("query3"));
        assertTrue(copy.sqlIds().contains("query3"));
    }

    @Test
    public void testSaveTo() throws IOException {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("fetchSize", "50");

        mapper.add("findUser", "SELECT * FROM users WHERE id = ?", attrs);
        mapper.add("updateUser", "UPDATE users SET name = ? WHERE id = ?", new HashMap<>());

        File outputFile = new File(tempDir, "output.xml");
        mapper.saveTo(outputFile);

        assertTrue(outputFile.exists());

        // Load saved file and verify
        SQLMapper loaded = SQLMapper.fromFile(outputFile.getAbsolutePath());
        assertEquals(mapper.sqlIds(), loaded.sqlIds());
        assertEquals(mapper.get("findUser").sql(), loaded.get("findUser").sql());
        assertEquals(mapper.get("updateUser").sql(), loaded.get("updateUser").sql());

        ImmutableMap<String, String> loadedAttrs = loaded.getAttributes("findUser");
        assertEquals("100", loadedAttrs.get("batchSize"));
        assertEquals("50", loadedAttrs.get("fetchSize"));
    }

    @Test
    public void testSaveToCreatesParentDirectories() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("findUser", "SELECT 1", new HashMap<>());

        File nestedDir = new File(tempDir, "nested/dir");
        File outputFile = new File(nestedDir, "output.xml");
        assertFalse(nestedDir.exists());

        mapper.saveTo(outputFile);

        assertTrue(outputFile.exists());
        SQLMapper loaded = SQLMapper.fromFile(outputFile.getAbsolutePath());
        assertNotNull(loaded.get("findUser"));
    }

    @Test
    public void testIsEmpty() {
        SQLMapper mapper = new SQLMapper();
        assertTrue(mapper.isEmpty());

        mapper.add("query1", ParsedSql.parse("SELECT 1"));
        assertFalse(mapper.isEmpty());

        mapper.remove("query1");
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testHashCode() {
        SQLMapper firstMapper = new SQLMapper();
        SQLMapper secondMapper = new SQLMapper();

        // Empty mappers should have same hashCode
        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());

        // Add same content
        firstMapper.add("query1", ParsedSql.parse("SELECT 1"));
        secondMapper.add("query1", ParsedSql.parse("SELECT 1"));

        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }

    @Test
    public void testEquals() {
        SQLMapper firstMapper = new SQLMapper();
        SQLMapper secondMapper = new SQLMapper();

        // Test equals with same instance
        assertEquals(firstMapper, firstMapper);

        // Test equals with null
        assertNotEquals(firstMapper, null);

        // Test equals with different type
        assertNotEquals(firstMapper, "string");

        // Test equals with empty mappers
        assertEquals(firstMapper, secondMapper);

        // Add same content
        firstMapper.add("query1", ParsedSql.parse("SELECT 1"));
        secondMapper.add("query1", ParsedSql.parse("SELECT 1"));
        assertEquals(firstMapper, secondMapper);

        // Add different content
        secondMapper.add("query2", ParsedSql.parse("SELECT 2"));
        assertNotEquals(firstMapper, secondMapper);
    }

    @Test
    public void testToString() {
        SQLMapper mapper = new SQLMapper();
        String str = mapper.toString();
        assertNotNull(str);
        assertEquals("{}", str);

        mapper.add("query1", ParsedSql.parse("SELECT 1"));
        str = mapper.toString();
        assertTrue(str.contains("query1"));
        assertTrue(str.contains("SELECT 1"));
    }

    @Test
    public void testResultSetTypeMap() {
        // Test the constant RESULT_SET_TYPE_MAP
        ImmutableMap<String, Integer> map = SQLMapper.RESULT_SET_TYPE_MAP;

        assertEquals(3, map.size());
        assertEquals(java.sql.ResultSet.TYPE_FORWARD_ONLY, map.get("FORWARD_ONLY").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, map.get("SCROLL_INSENSITIVE").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_SENSITIVE, map.get("SCROLL_SENSITIVE").intValue());
    }

    @Test
    public void testConstants() {
        // Test all public constants
        assertEquals("sqlMapper", SQLMapper.SQL_MAPPER);
        assertEquals("sql", SQLMapper.SQL);
        assertEquals("id", SQLMapper.ID);
        assertEquals("batchSize", SQLMapper.BATCH_SIZE);
        assertEquals("fetchSize", SQLMapper.FETCH_SIZE);
        assertEquals("resultSetType", SQLMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SQLMapper.TIMEOUT);
        assertEquals(128, SQLMapper.MAX_ID_LENGTH);
    }
}
