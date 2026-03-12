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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;

@Tag("2025")
class SqlMapper2025Test extends TestBase {

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
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove("nonExistent");
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.sqlIds().size());
    }

    @Test
    public void testRemoveEmptyId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove("");
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.sqlIds().size());
    }

    @Test
    public void testRemoveNullId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(null);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.sqlIds().size());
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
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(longId);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.sqlIds().size());
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
        assertEquals(sql1, mapper.get("query1"));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query1", sql2));
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

public class SqlMapperTest extends TestBase {

    @TempDir
    File tempDir;

    @Test
    public void testEmptyConstructor() {
        SqlMapper mapper = new SqlMapper();
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
        SqlMapper mapper = SqlMapper.fromFile(xmlFile.getAbsolutePath());
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
        SqlMapper mapper = SqlMapper.fromFile(paths);

        assertEquals(2, mapper.sqlIds().size());
        assertNotNull(mapper.get("findUser"));
        assertNotNull(mapper.get("findOrder"));

        // Test loading multiple files with semicolon separator
        paths = xmlFile1.getAbsolutePath() + ";" + xmlFile2.getAbsolutePath();
        mapper = SqlMapper.fromFile(paths);

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

        assertThrows(RuntimeException.class, () -> SqlMapper.fromFile(xmlFile.getAbsolutePath()));
    }

    @Test
    public void testKeySet() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM table1"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM table2"));

        Set<String> keys = mapper.sqlIds();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("query1"));
        assertTrue(keys.contains("query2"));
    }

    @Test
    public void testGet() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users");
        mapper.add("findAll", sql);

        // Test valid get
        ParsedSql retrieved = mapper.get("findAll");
        assertEquals(sql, retrieved);

        // Test get with empty id
        assertNull(mapper.get(""));
        assertNull(mapper.get(null));

        // Test get with id too long
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.get(longId));

        // Test get non-existent
        assertNull(mapper.get("nonExistent"));
    }

    @Test
    public void testGetAttrs() {
        SqlMapper mapper = new SqlMapper();
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
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.getAttributes(longId));

        // Test getAttrs non-existent
        assertNull(mapper.getAttributes("nonExistent"));
    }

    @Test
    public void testAddParsedSql() {
        SqlMapper mapper = new SqlMapper();

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
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> mapper.add(longId, sql1));
    }

    @Test
    public void testAddStringWithAttrs() {
        SqlMapper mapper = new SqlMapper();
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
        SqlMapper mapper = new SqlMapper();
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
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        mapper.remove(longId);
    }

    @Test
    public void testCopy() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("timeout", "60");

        mapper.add("query1", "SELECT * FROM users", attrs);
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        SqlMapper copy = mapper.copy();

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
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("fetchSize", "50");

        mapper.add("findUser", "SELECT * FROM users WHERE id = ?", attrs);
        mapper.add("updateUser", "UPDATE users SET name = ? WHERE id = ?", new HashMap<>());

        File outputFile = new File(tempDir, "output.xml");
        mapper.saveTo(outputFile);

        assertTrue(outputFile.exists());

        // Load saved file and verify
        SqlMapper loaded = SqlMapper.fromFile(outputFile.getAbsolutePath());
        assertEquals(mapper.sqlIds(), loaded.sqlIds());
        assertEquals(mapper.get("findUser").sql(), loaded.get("findUser").sql());
        assertEquals(mapper.get("updateUser").sql(), loaded.get("updateUser").sql());

        ImmutableMap<String, String> loadedAttrs = loaded.getAttributes("findUser");
        assertEquals("100", loadedAttrs.get("batchSize"));
        assertEquals("50", loadedAttrs.get("fetchSize"));
    }

    @Test
    public void testSaveToCreatesParentDirectories() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findUser", "SELECT 1", new HashMap<>());

        File nestedDir = new File(tempDir, "nested/dir");
        File outputFile = new File(nestedDir, "output.xml");
        assertFalse(nestedDir.exists());

        mapper.saveTo(outputFile);

        assertTrue(outputFile.exists());
        SqlMapper loaded = SqlMapper.fromFile(outputFile.getAbsolutePath());
        assertNotNull(loaded.get("findUser"));
    }

    @Test
    public void testIsEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertTrue(mapper.isEmpty());

        mapper.add("query1", ParsedSql.parse("SELECT 1"));
        assertFalse(mapper.isEmpty());

        mapper.remove("query1");
        assertTrue(mapper.isEmpty());
    }

    @Test
    public void testHashCode() {
        SqlMapper firstMapper = new SqlMapper();
        SqlMapper secondMapper = new SqlMapper();

        // Empty mappers should have same hashCode
        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());

        // Add same content
        firstMapper.add("query1", ParsedSql.parse("SELECT 1"));
        secondMapper.add("query1", ParsedSql.parse("SELECT 1"));

        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }

    @Test
    public void testEquals() {
        SqlMapper firstMapper = new SqlMapper();
        SqlMapper secondMapper = new SqlMapper();

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
        SqlMapper mapper = new SqlMapper();
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
        ImmutableMap<String, Integer> map = SqlMapper.RESULT_SET_TYPE_MAP;

        assertEquals(3, map.size());
        assertEquals(java.sql.ResultSet.TYPE_FORWARD_ONLY, map.get("FORWARD_ONLY").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, map.get("SCROLL_INSENSITIVE").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_SENSITIVE, map.get("SCROLL_SENSITIVE").intValue());
    }

    @Test
    public void testConstants() {
        // Test all public constants
        assertEquals("sqlMapper", SqlMapper.SQL_MAPPER);
        assertEquals("sql", SqlMapper.SQL);
        assertEquals("id", SqlMapper.ID);
        assertEquals("batchSize", SqlMapper.BATCH_SIZE);
        assertEquals("fetchSize", SqlMapper.FETCH_SIZE);
        assertEquals("resultSetType", SqlMapper.RESULT_SET_TYPE);
        assertEquals("timeout", SqlMapper.TIMEOUT);
        assertEquals(128, SqlMapper.MAX_ID_LENGTH);
    }
}

class SqlMapperJavadocExamples extends TestBase {

    @Test
    public void testSqlMapper_addAndGet() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
        mapper.add("findUserById", parsedSql);

        ParsedSql retrieved = mapper.get("findUserById");
        assertNotNull(retrieved);
        assertEquals("select * from users where id = ?", retrieved.parameterizedSql());
    }

    @Test
    public void testSqlMapper_addWithAttrs() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("timeout", "30");
        mapper.add("insertUser", "insert into users (id, name) values (?, ?)", attrs);

        ParsedSql sql = mapper.get("insertUser");
        assertNotNull(sql);

        ImmutableMap<String, String> retrievedAttrs = mapper.getAttributes("insertUser");
        assertNotNull(retrievedAttrs);
        assertEquals("100", retrievedAttrs.get("batchSize"));
        assertEquals("30", retrievedAttrs.get("timeout"));
    }

    @Test
    public void testSqlMapper_getReturnsNull() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql unknown = mapper.get("nonExistentId");
        assertNull(unknown);
    }

    @Test
    public void testSqlMapper_remove() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("deprecatedQuery", "select 1", null);
        assertNotNull(mapper.get("deprecatedQuery"));
        mapper.remove("deprecatedQuery");
        boolean removed = mapper.get("deprecatedQuery") == null;
        assertTrue(removed);
    }

    @Test
    public void testSqlMapper_copy() {
        SqlMapper original = new SqlMapper();
        original.add("query1", "select 1", null);
        SqlMapper copy = original.copy();
        copy.add("newQuery", ParsedSql.parse("SELECT 1"));
        boolean originalHasIt = original.get("newQuery") != null;
        boolean copyHasIt = copy.get("newQuery") != null;
        assertFalse(originalHasIt);
        assertTrue(copyHasIt);
    }

    @Test
    public void testSqlMapper_isEmpty() {
        SqlMapper emptyMapper = new SqlMapper();
        boolean empty = emptyMapper.isEmpty();
        assertTrue(empty);
    }

    @Test
    public void testSqlMapper_sqlIds() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findUser", "select * from users where id = ?", null);
        mapper.add("updateUser", "update users set name = ? where id = ?", null);
        Set<String> sqlIds = mapper.sqlIds();
        assertTrue(sqlIds.contains("findUser"));
        assertTrue(sqlIds.contains("updateUser"));
        assertEquals(2, sqlIds.size());
    }
}
