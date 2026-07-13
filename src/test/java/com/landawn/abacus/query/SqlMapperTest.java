package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;

@Tag("2025")
public class SqlMapperTest extends TestBase {
    @Test
    public void testLoadMissingFilePathThrowsPointedIae() {
        // Regression (2026-07-03): a path that resolves to no file used to surface as a bare NPE
        // from PropertiesUtil.formatPath(null) instead of the documented IllegalArgumentException.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom("no/such/sql-mapper-file-xyz.xml"));
        assertTrue(e.getMessage().contains("no/such/sql-mapper-file-xyz.xml"));
    }

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
        assertEquals(sql.originalSql(), retrieved.originalSql());
    }

    @Test
    public void testAddSqlString() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");

        mapper.add("insertUser", "INSERT INTO users (name, email) VALUES (?, ?)", attrs);

        ParsedSql sql = mapper.get("insertUser");
        assertNotNull(sql);

        ImmutableMap<String, String> retrievedAttrs = mapper.attributes("insertUser");
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
        assertEquals(sql.originalSql(), retrieved.originalSql());
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

        ImmutableMap<String, String> retrieved = mapper.attributes("query1");
        assertNotNull(retrieved);
        assertEquals("50", retrieved.get("fetchSize"));
        assertEquals("30", retrieved.get("timeout"));
    }

    @Test
    public void testGetAttrsNonExistent() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.attributes("nonExistent");
        assertNull(attrs);
    }

    @Test
    public void testGetAttrsEmptyId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.attributes("");
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
        assertEquals(1, mapper.ids().size());
    }

    @Test
    public void testRemoveEmptyId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove("");
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    public void testRemoveNullId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(null);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    public void testKeySet() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertEquals(2, mapper.ids().size());
        assertTrue(mapper.ids().contains("query1"));
        assertTrue(mapper.ids().contains("query2"));
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
        assertNotNull(copy.attributes("query2"));
        assertEquals("100", copy.attributes("query2").get("batchSize"));
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

        ImmutableMap<String, String> retrieved = mapper.attributes("complexQuery");
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

        ImmutableMap<String, String> retrieved = mapper.attributes("query1");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    public void testKeySetEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper.ids());
        assertTrue(mapper.ids().isEmpty());
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
        ImmutableMap<String, String> attrs = mapper.attributes(null);
        assertNull(attrs);
    }

    @Test
    public void testRemoveTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(longId);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
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
        assertEquals(3, mapper.ids().size());
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
        ImmutableMap<String, String> attrs = mapper.attributes(longId);
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

        ImmutableMap<String, String> copiedAttrs = copy.attributes("query1");
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

    @TempDir
    File tempDir;

    @Test
    public void testEmptyConstructor() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
        assertTrue(mapper.ids().isEmpty());
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
        SqlMapper mapper = SqlMapper.loadFrom(xmlFile.getAbsolutePath());
        assertNotNull(mapper);
        assertFalse(mapper.isEmpty());

        // Verify loaded SQLs
        assertEquals(3, mapper.ids().size());
        assertTrue(mapper.ids().contains("findById"));
        assertTrue(mapper.ids().contains("updateName"));
        assertTrue(mapper.ids().contains("deleteById"));

        // Check SQL content
        ParsedSql findById = mapper.get("findById");
        assertNotNull(findById);
        assertEquals("SELECT * FROM users WHERE id = ?", findById.originalSql());

        // Check attributes
        ImmutableMap<String, String> attrs = mapper.attributes("updateName");
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
        SqlMapper mapper = SqlMapper.loadFrom(paths);

        assertEquals(2, mapper.ids().size());
        assertNotNull(mapper.get("findUser"));
        assertNotNull(mapper.get("findOrder"));

        // Test loading multiple files with semicolon separator
        paths = xmlFile1.getAbsolutePath() + ";" + xmlFile2.getAbsolutePath();
        mapper = SqlMapper.loadFrom(paths);

        assertEquals(2, mapper.ids().size());
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

        assertThrows(RuntimeException.class, () -> SqlMapper.loadFrom(xmlFile.getAbsolutePath()));
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
        assertEquals("SELECT * FROM users", sql.originalSql());

        ImmutableMap<String, String> retrievedAttrs = mapper.attributes("query1");
        assertEquals("50", retrievedAttrs.get("batchSize"));

        // Test duplicate id
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query1", "SELECT * FROM orders", new HashMap<>()));
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
        SqlMapper loaded = SqlMapper.loadFrom(outputFile.getAbsolutePath());
        assertEquals(mapper.ids(), loaded.ids());
        assertEquals(mapper.get("findUser").originalSql(), loaded.get("findUser").originalSql());
        assertEquals(mapper.get("updateUser").originalSql(), loaded.get("updateUser").originalSql());

        ImmutableMap<String, String> loadedAttrs = loaded.attributes("findUser");
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
        SqlMapper loaded = SqlMapper.loadFrom(outputFile.getAbsolutePath());
        assertNotNull(loaded.get("findUser"));
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

        ImmutableMap<String, String> retrievedAttrs = mapper.attributes("insertUser");
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
    public void testSqlMapper_ids() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findUser", "select * from users where id = ?", null);
        mapper.add("updateUser", "update users set name = ? where id = ?", null);
        Set<String> ids = mapper.ids();
        assertTrue(ids.contains("findUser"));
        assertTrue(ids.contains("updateUser"));
        assertEquals(2, ids.size());
    }

    @Test
    public void testLoad_EmptySegmentsAfterSplit() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(" , ; , "));
    }

    @Test
    public void testLoad_MissingFileWrapsIOException() {
        File directoryPath = tempDir;

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> SqlMapper.loadFrom(directoryPath.getAbsolutePath()));
    }

    @Test
    public void testLoad_MalformedXmlWrapsParsingException() throws IOException {
        File malformed = new File(tempDir, "malformed-sql-mapper.xml");

        try (FileWriter writer = new FileWriter(malformed)) {
            writer.write("<sqlMapper><sql id=\"findUser\">SELECT 1</sqlMapper>");
        }

        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(malformed.getAbsolutePath()));
    }

    @Test
    public void testSaveTo_TargetIsDirectory() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> mapper.saveTo(tempDir));
    }

    @Test
    public void testSaveTo_ParentDirectoryCreationFails() throws IOException {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        File blocker = new File(tempDir, "blocker");
        assertTrue(blocker.createNewFile());

        File target = new File(blocker, "nested/output.xml");

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> mapper.saveTo(target));
    }

    @Test
    public void testSaveTo_RelativeFileWithoutParent() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        File output = new File("sql-mapper-relative-output.xml");

        try {
            mapper.saveTo(output);
            assertTrue(output.exists());
        } finally {
            output.delete();
        }
    }

    @Test
    public void testSaveTo_AttrsIdEntryDoesNotOverrideCanonicalId() throws IOException {
        // Bug fix: a stray "id" entry in the attrs map must never overwrite
        // the canonical mapping id during XML serialization.
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "evil"); // pretend the user passed a conflicting id
        attrs.put("batchSize", "50");
        mapper.add("realId", "SELECT 1", attrs);

        File output = new File(tempDir, "attrs-id-roundtrip.xml");
        mapper.saveTo(output);

        SqlMapper reloaded = SqlMapper.loadFrom(output.getAbsolutePath());
        assertNotNull(reloaded.get("realId"));
        assertNull(reloaded.get("evil"));
        assertEquals("50", reloaded.attributes("realId").get("batchSize"));
    }

    @Test
    public void testSaveTo_RoundTripSpecialXmlCharacters() throws IOException {
        // SQL containing <, >, & must round-trip correctly through XML escaping.
        SqlMapper mapper = new SqlMapper();
        mapper.add("findGreater", "SELECT * FROM t WHERE a < 10 AND b > 5 AND c = 'a&b'", null);

        File output = new File(tempDir, "special-chars-roundtrip.xml");
        mapper.saveTo(output);

        SqlMapper reloaded = SqlMapper.loadFrom(output.getAbsolutePath());
        assertEquals("SELECT * FROM t WHERE a < 10 AND b > 5 AND c = 'a&b'", reloaded.get("findGreater").originalSql());
    }

    // ----- containsId -----

    @Test
    public void testContainsId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findById", ParsedSql.parse("SELECT 1"));

        assertTrue(mapper.containsId("findById"));
        assertFalse(mapper.containsId("nonExistent"));
    }

    @Test
    public void testContainsId_invalidIdsReturnFalseWithoutThrowing() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findById", ParsedSql.parse("SELECT 1"));

        assertFalse(mapper.containsId(null));
        assertFalse(mapper.containsId(""));

        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertFalse(mapper.containsId(longId));
    }

    // ----- size -----

    @Test
    public void testSize() {
        SqlMapper mapper = new SqlMapper();
        assertEquals(0, mapper.size());

        mapper.add("query1", ParsedSql.parse("SELECT 1"));
        assertEquals(1, mapper.size());

        mapper.add("query2", ParsedSql.parse("SELECT 2"));
        assertEquals(2, mapper.size());

        mapper.remove("query1");
        assertEquals(1, mapper.size());
    }

    // ----- ids snapshot semantics -----

    @Test
    public void testIds_isImmutableSnapshot() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        ImmutableSet<String> snapshot = mapper.ids();
        assertEquals(1, snapshot.size());
        assertTrue(snapshot.contains("query1"));

        // Mutating the mapper after taking the snapshot must NOT affect the snapshot.
        mapper.add("query2", ParsedSql.parse("SELECT 2"));
        assertEquals(1, snapshot.size());
        assertFalse(snapshot.contains("query2"));

        // But a freshly requested snapshot reflects the new state.
        assertEquals(2, mapper.ids().size());
    }

    @Test
    public void testIds_isUnmodifiable() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        ImmutableSet<String> snapshot = mapper.ids();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("query2"));
    }

    // ----- add(String, ParsedSql, Map) -----

    @Test
    public void testAddParsedSqlWithAttrs() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");

        mapper.add("insertUser", ParsedSql.parse("INSERT INTO users VALUES (?)"), attrs);

        assertNotNull(mapper.get("insertUser"));
        assertEquals("100", mapper.attributes("insertUser").get("batchSize"));
    }

    @Test
    public void testAddParsedSqlWithNullAttrs() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"), null);

        ImmutableMap<String, String> attrs = mapper.attributes("query1");
        assertNotNull(attrs);
        assertTrue(attrs.isEmpty());
    }

    @Test
    public void testAddParsedSqlWithAttrs_validation() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.add("id", (ParsedSql) null, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapper.add(null, ParsedSql.parse("SELECT 1"), new HashMap<>()));

        mapper.add("dup", ParsedSql.parse("SELECT 1"), new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> mapper.add("dup", ParsedSql.parse("SELECT 2"), new HashMap<>()));
    }

    // ----- add(String, String) -----

    @Test
    public void testAddSqlStringNoAttrs() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findAll", "SELECT * FROM users");

        ParsedSql sql = mapper.get("findAll");
        assertNotNull(sql);
        assertEquals("SELECT * FROM users", sql.originalSql());

        ImmutableMap<String, String> attrs = mapper.attributes("findAll");
        assertNotNull(attrs);
        assertTrue(attrs.isEmpty());
    }

    @Test
    public void testAddSqlStringNoAttrs_validation() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.add("id", (String) null));
        assertThrows(IllegalArgumentException.class, () -> mapper.add(null, "SELECT 1"));

        mapper.add("dup", "SELECT 1");
        assertThrows(IllegalArgumentException.class, () -> mapper.add("dup", "SELECT 2"));
    }

    @Test
    public void testAdd_InvalidAttributesDoNotPartiallyMutateMapper() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("timeout", null);

        assertThrows(IllegalArgumentException.class, () -> mapper.add("queryWithInvalidAttrs", "SELECT 1", attrs));
        assertTrue(mapper.isEmpty());
        assertFalse(mapper.containsId("queryWithInvalidAttrs"));

        attrs.clear();
        attrs.put("", "10");
        assertThrows(IllegalArgumentException.class, () -> mapper.add("queryWithEmptyAttrName", ParsedSql.parse("SELECT 2"), attrs));
        assertTrue(mapper.isEmpty());
    }

    // ----- loadFrom(File...) -----

    @Test
    public void testLoadFromFiles() throws IOException {
        File f1 = new File(tempDir, "users.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findUser\">SELECT * FROM users</sql>\n</sqlMapper>\n");
        }
        File f2 = new File(tempDir, "orders.xml");
        try (FileWriter w = new FileWriter(f2)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findOrder\">SELECT * FROM orders</sql>\n</sqlMapper>\n");
        }

        SqlMapper mapper = SqlMapper.loadFrom(f1, f2);
        assertEquals(2, mapper.size());
        assertTrue(mapper.containsId("findUser"));
        assertTrue(mapper.containsId("findOrder"));
    }

    @Test
    public void testLoadFromSingleFile() throws IOException {
        File f1 = new File(tempDir, "single.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findUser\" fetchSize=\"10\">SELECT * FROM users</sql>\n</sqlMapper>\n");
        }

        SqlMapper mapper = SqlMapper.loadFrom(f1);
        assertEquals(1, mapper.size());
        assertEquals("10", mapper.attributes("findUser").get("fetchSize"));
    }

    @Test
    public void testLoadFromFiles_emptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(new File[0]));
    }

    @Test
    public void testLoadFromFiles_nullElementThrows() throws IOException {
        File f1 = new File(tempDir, "users2.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findUser\">SELECT 1</sql>\n</sqlMapper>\n");
        }
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(f1, (File) null));
    }

    @Test
    public void testLoadFromFiles_duplicateIdAcrossFilesThrows() throws IOException {
        File f1 = new File(tempDir, "a.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"dup\">SELECT 1</sql>\n</sqlMapper>\n");
        }
        File f2 = new File(tempDir, "b.xml");
        try (FileWriter w = new FileWriter(f2)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"dup\">SELECT 2</sql>\n</sqlMapper>\n");
        }
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(f1, f2));
    }

    // ----- loadFrom(InputStream) -----

    @Test
    public void testLoadFromInputStream() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sqlMapper><sql id=\"findUser\" timeout=\"5\">SELECT * FROM users</sql></sqlMapper>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        SqlMapper mapper = SqlMapper.loadFrom(is);
        assertEquals(1, mapper.size());
        assertEquals("SELECT * FROM users", mapper.get("findUser").originalSql());
        assertEquals("5", mapper.attributes("findUser").get("timeout"));
    }

    @Test
    public void testLoadFromInputStream_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom((InputStream) null));
    }

    @Test
    public void testLoadFromInputStream_noSqlMapperElementThrows() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(is));
    }

    @Test
    public void testLoadFromInputStream_nestedSqlMapperIsNotAcceptedAsRoot() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><sqlMapper><sql id=\"q\">SELECT 1</sql></sqlMapper></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(is));
    }

    @Test
    public void testLoadFromInputStream_callerCanCloseInTryWithResources() throws IOException {
        // The caller opens the stream and closes it via try-with-resources; loadFrom() must not
        // throw or interfere with that (the underlying parser may consume/close the stream).
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sqlMapper><sql id=\"q\">SELECT 1</sql></sqlMapper>";
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            SqlMapper mapper = SqlMapper.loadFrom(is);
            assertEquals(1, mapper.size());
            assertEquals("SELECT 1", mapper.get("q").originalSql());
        }
    }

    // ----- saveTo(OutputStream) -----

    @Test
    public void testSaveToFile_nullThrowsIllegalArgumentException() {
        SqlMapper mapper = new SqlMapper();

        assertThrows(IllegalArgumentException.class, () -> mapper.saveTo((File) null));
    }

    @Test
    public void testSaveToOutputStream_nullThrowsIllegalArgumentException() {
        SqlMapper mapper = new SqlMapper();

        assertThrows(IllegalArgumentException.class, () -> mapper.saveTo((OutputStream) null));
    }

    @Test
    public void testSaveToOutputStream_roundTrip() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        mapper.add("findUser", "SELECT * FROM users WHERE id = ?", attrs);
        mapper.add("updateUser", "UPDATE users SET name = ? WHERE id = ?");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mapper.saveTo(baos);

        SqlMapper loaded = SqlMapper.loadFrom(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(mapper.ids(), loaded.ids());
        assertEquals("SELECT * FROM users WHERE id = ?", loaded.get("findUser").originalSql());
        assertEquals("100", loaded.attributes("findUser").get("batchSize"));
    }

    @Test
    public void testSaveToOutputStream_doesNotCloseStream() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", ParsedSql.parse("SELECT 1"));

        final boolean[] closed = { false };
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        mapper.saveTo(baos);
        assertFalse(closed[0]);
    }

    @Test
    public void testSaveToFileStillWorksViaDelegation() throws IOException {
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1");

        File out = new File(tempDir, "delegated.xml");
        mapper.saveTo(out);
        assertTrue(out.exists());

        try (InputStream is = new FileInputStream(out)) {
            SqlMapper loaded = SqlMapper.loadFrom(is);
            assertEquals("SELECT 1", loaded.get("q").originalSql());
        }
    }

    @Test
    public void testStringPathSaveAndStructuredPathLoad() {
        SqlMapper first = new SqlMapper();
        first.add("q1", "SELECT 1");
        File firstFile = new File(tempDir, "first.xml");
        first.saveTo(firstFile.getAbsolutePath());

        SqlMapper second = new SqlMapper();
        second.add("q2", "SELECT 2");
        File secondFile = new File(tempDir, "second.xml");
        second.saveTo(secondFile.getAbsolutePath());

        SqlMapper loaded = SqlMapper.loadFrom(firstFile.getAbsolutePath(), secondFile.getAbsolutePath());
        assertEquals(2, loaded.size());
        assertEquals("SELECT 1", loaded.get("q1").originalSql());
        assertEquals("SELECT 2", loaded.get("q2").originalSql());
    }
}
