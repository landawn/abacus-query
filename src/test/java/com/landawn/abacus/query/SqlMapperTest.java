package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;

public class SqlMapperTest extends TestBase {

    @TempDir
    File tempDir;

    @Test
    void testAdd_InvalidAttributesDoNotPartiallyMutateMapper() {
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

    @Test
    void testAddDuplicateId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("findAll", ParsedSql.parse("SELECT * FROM accounts"));
        });
    }

    @Test
    void testAddEmptyAttributes() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();

        mapper.add("query1", "SELECT * FROM users", attrs);

        ImmutableMap<String, String> retrieved = mapper.attributes("query1");
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void testAddEmptyId() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    void testAddIdWithWhitespace() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add("find all", ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    void testAddNullId() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(null, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    void testAddParsedSql() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users");
        mapper.add("findAll", sql);

        ParsedSql retrieved = mapper.get("findAll");
        assertNotNull(retrieved);
        assertEquals(sql.originalSql(), retrieved.originalSql());
    }

    @Test
    void testAddParsedSqlReturn() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql1 = ParsedSql.parse("SELECT * FROM users");
        mapper.add("query1", sql1);

        ParsedSql sql2 = ParsedSql.parse("SELECT * FROM orders");
        assertEquals(sql1, mapper.get("query1"));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query1", sql2));
    }

    // ----- add(String, ParsedSql, Map) -----

    @Test
    void testAddParsedSqlWithAttrs() {
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");

        mapper.add("insertUser", ParsedSql.parse("INSERT INTO users VALUES (?)"), attrs);

        assertNotNull(mapper.get("insertUser"));
        assertEquals("100", mapper.attributes("insertUser").get("batchSize"));
    }

    @Test
    void testAddParsedSqlWithAttrs_validation() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.add("id", (ParsedSql) null, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapper.add(null, ParsedSql.parse("SELECT 1"), new HashMap<>()));

        mapper.add("dup", ParsedSql.parse("SELECT 1"), new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> mapper.add("dup", ParsedSql.parse("SELECT 2"), new HashMap<>()));
    }

    @Test
    void testAddParsedSqlWithNullAttrs() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"), null);

        ImmutableMap<String, String> attrs = mapper.attributes("query1");
        assertNotNull(attrs);
        assertTrue(attrs.isEmpty());
    }

    @Test
    void testAddSqlString() {
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

    // ----- add(String, String) -----

    @Test
    void testAddSqlStringNoAttrs() {
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
    void testAddSqlStringNoAttrs_validation() {
        SqlMapper mapper = new SqlMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.add("id", (String) null));
        assertThrows(IllegalArgumentException.class, () -> mapper.add(null, "SELECT 1"));

        mapper.add("dup", "SELECT 1");
        assertThrows(IllegalArgumentException.class, () -> mapper.add("dup", "SELECT 2"));
    }

    @Test
    void testAddSqlStringWithEmptyAttributes() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", "SELECT * FROM users", new HashMap<>());
        ParsedSql sql = mapper.get("query1");
        assertNotNull(sql);
    }

    @Test
    void testAddStringWithAttrs() {
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
    void testAddTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.add(longId, ParsedSql.parse("SELECT * FROM users"));
        });
    }

    @Test
    void testConstants() {
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
    void testConstructor() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
    }

    // ----- containsId -----

    @Test
    void testContainsId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findById", ParsedSql.parse("SELECT 1"));

        assertTrue(mapper.containsId("findById"));
        assertFalse(mapper.containsId("nonExistent"));
    }

    @Test
    void testContainsId_invalidIdsReturnFalseWithoutThrowing() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findById", ParsedSql.parse("SELECT 1"));

        assertFalse(mapper.containsId(null));
        assertFalse(mapper.containsId(""));

        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertFalse(mapper.containsId(longId));
    }

    @Test
    void testCopy() {
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
    void testCopyIsIndependent() {
        SqlMapper original = new SqlMapper();
        original.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper copy = original.copy();
        copy.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertNotNull(copy.get("query2"));
        assertNull(original.get("query2"));
    }

    @Test
    void testCopyPreservesAttributes() {
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
    void testEmptyConstructor() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper);
        assertTrue(mapper.isEmpty());
        assertTrue(mapper.ids().isEmpty());
    }

    @Test
    void testEquals() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(firstMapper, secondMapper);
    }

    @Test
    void testEquals_DifferentAttributesAreNotEqual() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "10"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "30"));

        assertNotEquals(firstMapper, secondMapper);
    }

    @Test
    void testEqualsDifferentClass() {
        SqlMapper mapper = new SqlMapper();
        assertFalse(mapper.equals("string"));
    }

    @Test
    void testEqualsDifferentContent() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM orders"));

        assertFalse(firstMapper.equals(secondMapper));
    }

    @Test
    void testEqualsNull() {
        SqlMapper mapper = new SqlMapper();
        assertFalse(mapper.equals(null));
    }

    @Test
    void testEqualsSameObject() {
        SqlMapper mapper = new SqlMapper();
        assertEquals(mapper, mapper);
    }

    @Test
    void testFromFile() throws IOException {
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
    void testFromFileMultiple() throws IOException {
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
    void testFromFileNoSqlMapperElement() throws IOException {
        File xmlFile = new File(tempDir, "invalid.xml");
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<root></root>\n");
        }

        assertThrows(RuntimeException.class, () -> SqlMapper.loadFrom(xmlFile.getAbsolutePath()));
    }

    @Test
    void testGet() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        mapper.add("findById", sql);

        ParsedSql retrieved = mapper.get("findById");
        assertNotNull(retrieved);
        assertEquals(sql.originalSql(), retrieved.originalSql());
    }

    @Test
    void testGetAttrs() {
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
    void testGetAttrsEmptyId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.attributes("");
        assertNull(attrs);
    }

    @Test
    void testGetAttrsNonExistent() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.attributes("nonExistent");
        assertNull(attrs);
    }

    @Test
    void testGetAttrsTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        ImmutableMap<String, String> attrs = mapper.attributes(longId);
        assertNull(attrs);
    }

    @Test
    void testGetAttrsWithNullId() {
        SqlMapper mapper = new SqlMapper();
        ImmutableMap<String, String> attrs = mapper.attributes(null);
        assertNull(attrs);
    }

    @Test
    void testGetEmptyId() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get("");
        assertNull(sql);
    }

    @Test
    void testGetNonExistent() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get("nonExistent");
        assertNull(sql);
    }

    @Test
    void testGetNullId() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql sql = mapper.get(null);
        assertNull(sql);
    }

    @Test
    void testGetTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        ParsedSql sql = mapper.get(longId);
        assertNull(sql);
    }

    @Test
    void testGetTooLongIdBoundary() {
        SqlMapper mapper = new SqlMapper();
        String exactMaxId = "a".repeat(SqlMapper.MAX_ID_LENGTH);
        mapper.add(exactMaxId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(exactMaxId));

        String tooLongId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        assertNull(mapper.get(tooLongId));
    }

    @Test
    void testHashCode() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        assertEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }

    @Test
    void testHashCode_DifferentAttributesAreDifferent() {
        SqlMapper firstMapper = new SqlMapper();
        firstMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "10"));

        SqlMapper secondMapper = new SqlMapper();
        secondMapper.add("query1", "SELECT * FROM users", Map.of("timeout", "30"));

        assertNotEquals(firstMapper.hashCode(), secondMapper.hashCode());
    }

    @Test
    void testHashCodeConsistency() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        int hash1 = mapper.hashCode();
        int hash2 = mapper.hashCode();
        assertEquals(hash1, hash2);
    }

    // ----- ids snapshot semantics -----

    @Test
    void testIds_isImmutableSnapshot() {
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
    void testIds_isUnmodifiable() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        ImmutableSet<String> snapshot = mapper.ids();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("query2"));
    }

    @Test
    void testInvalidXmlAttributeNameIsRejectedWithoutMutation() {
        SqlMapper mapper = new SqlMapper();

        assertThrows(IllegalArgumentException.class, () -> mapper.add("query", "SELECT 1", Map.of("not an xml name", "10")));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query", "SELECT 1", Map.of("p:timeout", "10")));
        assertThrows(IllegalArgumentException.class, () -> mapper.add("query", "SELECT 1", Map.of("xmlns", "urn:test")));
        assertTrue(mapper.isEmpty());
        assertFalse(mapper.containsId("query"));
    }

    // R13-02: attribute names are validated through one shared DOM document instead of a parser per call, and the
    // loader skips that validation for names the XML parser already accepted; every rejection and the stored
    // content/order must be unchanged.
    @Test
    void testAttributeValidationAndOrderSurviveSharedValidatorAndLoader() {
        SqlMapper mapper = new SqlMapper();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> mapper.add("query", ParsedSql.parse("SELECT 1"), Map.of("not an xml name", "10")));
        assertEquals("Invalid XML attribute name: not an xml name", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> mapper.add("query", "SELECT 1", Map.of("1abc", "10")));
        assertEquals("Invalid XML attribute name: 1abc", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> mapper.add("query", "SELECT 1", Map.of("p:timeout", "10")));
        assertEquals("Namespace-qualified XML attributes are not supported: p:timeout", e.getMessage());
        assertTrue(mapper.isEmpty());

        // Repeated valid adds reuse the shared validator; insertion order of the supplied map is kept.
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put(SqlMapper.TIMEOUT, "5");
        attrs.put(SqlMapper.BATCH_SIZE, "100");
        attrs.put(SqlMapper.FETCH_SIZE, "10");
        attrs.put(SqlMapper.RESULT_SET_TYPE, "FORWARD_ONLY");

        mapper.add("parsed", ParsedSql.parse("SELECT 1"), attrs);
        mapper.add("text", "SELECT 2", attrs);
        attrs.put("later", "ignored"); // the stored map is a snapshot

        for (String id : Arrays.asList("parsed", "text")) {
            assertEquals(Arrays.asList(SqlMapper.TIMEOUT, SqlMapper.BATCH_SIZE, SqlMapper.FETCH_SIZE, SqlMapper.RESULT_SET_TYPE),
                    new ArrayList<>(mapper.attributes(id).keySet()), id);
            assertEquals("5", mapper.attributes(id).get(SqlMapper.TIMEOUT));
            assertEquals("FORWARD_ONLY", mapper.attributes(id).get(SqlMapper.RESULT_SET_TYPE));
            assertThrows(UnsupportedOperationException.class, () -> mapper.attributes(id).put("x", "y"));
        }

        // Loaded attributes keep the parsed document's content and order; a prefixed attribute is still rejected.
        String xml = "<sqlMapper><sql id=\"q\" batchSize=\"100\" fetchSize=\"10\" resultSetType=\"FORWARD_ONLY\" timeout=\"5\">select 1</sql></sqlMapper>";
        SqlMapper loaded = SqlMapper.loadFrom(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(Arrays.asList(SqlMapper.BATCH_SIZE, SqlMapper.FETCH_SIZE, SqlMapper.RESULT_SET_TYPE, SqlMapper.TIMEOUT),
                new ArrayList<>(loaded.attributes("q").keySet()));
        assertEquals(mapper.attributes("text"), loaded.attributes("q"));

        String prefixed = "<sqlMapper><sql id=\"q\" p:timeout=\"5\" xmlns:p=\"urn:p\">select 1</sql></sqlMapper>";
        e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(new ByteArrayInputStream(prefixed.getBytes(StandardCharsets.UTF_8))));
        assertEquals("Invalid <sql> definition in input stream: Namespace-qualified XML attributes are not supported: p:timeout", e.getMessage());

        // saveTo -> loadFrom round trip of programmatic attributes.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.saveTo(out);
        SqlMapper reloaded = SqlMapper.loadFrom(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(mapper, reloaded);
        assertEquals(mapper.attributes("parsed"), reloaded.attributes("parsed"));
    }

    @Test
    void testLoadFromInputStreamPreservesReadFailureCause() {
        final IOException failure = new IOException("source disconnected");
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        };
        final var error = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> SqlMapper.loadFrom(input));
        assertSame(failure, error.getCause());
        assertTrue(error.getMessage().contains("input stream"));
    }

    @Test
    void testSaveToOutputStreamPreservesNonIoFailure() {
        final SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1");
        final IllegalStateException failure = new IllegalStateException("destination unavailable");
        final OutputStream output = new OutputStream() {
            @Override
            public void write(final int value) {
                throw failure;
            }
        };
        final var error = assertThrows(com.landawn.abacus.exception.UncheckedException.class, () -> mapper.saveTo(output));
        assertFalse(error instanceof com.landawn.abacus.exception.UncheckedIOException);
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertSame(failure, cause);
    }

    // Only an XML 1.0 document lets the loader skip attribute-name validation: XML 1.1 Names are wider than what saveTo
    // (an XML 1.0 serializer) can write, so a 1.1-only name is still rejected at load time instead of breaking saveTo later.
    @Test
    void testXml11DocumentAttributeNamesAreStillValidatedOnLoad() {
        for (String name : Arrays.asList("a\u2070", "\u2C00x", "\uD840\uDC00x")) {
            String xml11 = "<?xml version=\"1.1\"?><sqlMapper><sql id=\"q\" " + name + "=\"1\">select 1</sql></sqlMapper>";
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> SqlMapper.loadFrom(new ByteArrayInputStream(xml11.getBytes(StandardCharsets.UTF_8))));
            assertEquals("Invalid <sql> definition in input stream: Invalid XML attribute name: " + name, e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> new SqlMapper().add("q", "select 1", Map.of(name, "1")));
            assertEquals("Invalid XML attribute name: " + name, e.getMessage());
        }

        // XML 1.1 and XML 1.0 documents with ordinary names still load and round-trip through saveTo.
        for (String version : Arrays.asList("1.1", "1.0")) {
            String xml = "<?xml version=\"" + version + "\"?><sqlMapper><sql id=\"q\" fetchSize=\"1\">select 1</sql></sqlMapper>";
            SqlMapper loaded = SqlMapper.loadFrom(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            assertEquals("1", loaded.attributes("q").get(SqlMapper.FETCH_SIZE), version);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            loaded.saveTo(out);
            SqlMapper reloaded = SqlMapper.loadFrom(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(loaded, reloaded, version);
            assertEquals(loaded.attributes("q"), reloaded.attributes("q"), version);
        }
    }

    @Test
    void testIsEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertTrue(mapper.isEmpty());

        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        assertFalse(mapper.isEmpty());

        mapper.remove("query1");
        assertTrue(mapper.isEmpty());
    }

    @Test
    void testKeySet() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM orders"));

        assertEquals(2, mapper.ids().size());
        assertTrue(mapper.ids().contains("query1"));
        assertTrue(mapper.ids().contains("query2"));
    }

    @Test
    void testKeySetEmpty() {
        SqlMapper mapper = new SqlMapper();
        assertNotNull(mapper.ids());
        assertTrue(mapper.ids().isEmpty());
    }

    @Test
    void testKeySetOrder() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query3", ParsedSql.parse("SELECT * FROM users"));
        mapper.add("query1", ParsedSql.parse("SELECT * FROM orders"));
        mapper.add("query2", ParsedSql.parse("SELECT * FROM products"));

        // LinkedHashMap preserves insertion order
        assertEquals(Arrays.asList("query3", "query1", "query2"), new ArrayList<>(mapper.ids()));
    }

    @Test
    void testLoad_EmptySegmentsAfterSplit() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(" , ; , "));
    }

    @Test
    void testLoad_MalformedXmlWrapsParsingException() throws IOException {
        File malformed = new File(tempDir, "malformed-sql-mapper.xml");

        try (FileWriter writer = new FileWriter(malformed)) {
            writer.write("<sqlMapper><sql id=\"findUser\">SELECT 1</sqlMapper>");
        }

        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(malformed.getAbsolutePath()));
    }

    @Test
    void testLoad_MissingFileWrapsIOException() {
        File directoryPath = tempDir;

        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(directoryPath.getAbsolutePath()));
    }

    @Test
    void testLoadFrom_invalidSqlIdNamesOffendingSourceInMessage() throws IOException {
        // A <sql> element with an invalid id must fail with an IAE whose message names the
        // offending source file, so multi-file loadFrom(...) failures are diagnosable.
        File badIdFile = new File(tempDir, "bad-id-mapper.xml");

        try (FileWriter writer = new FileWriter(badIdFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<sqlMapper>\n");
            writer.write("  <sql id=\"bad id\">SELECT 1</sql>\n");
            writer.write("</sqlMapper>\n");
        }

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(badIdFile.getAbsolutePath()));
        assertTrue(e.getMessage().contains("bad-id-mapper.xml"), "message should name the offending source file but was: " + e.getMessage());
        assertNotNull(e.getCause());
    }

    @Test
    void testLoadFrom_missingSqlIdNamesOffendingSourceInMessage() {
        // The same source-labeling applies to the stream path ("input stream" label) when the
        // id attribute is missing entirely.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sqlMapper><sql>SELECT 1</sql></sqlMapper>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(is));
        assertTrue(e.getMessage().contains("input stream"), "message should name the offending source but was: " + e.getMessage());
    }

    // ----- loadFrom(File...) -----

    @Test
    void testLoadFromFiles() throws IOException {
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
    void testLoadFromFiles_duplicateIdAcrossFilesThrows() throws IOException {
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

    @Test
    void testLoadFromFiles_emptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(new File[0]));
    }

    @Test
    void testLoadFromFiles_nullElementThrows() throws IOException {
        File f1 = new File(tempDir, "users2.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findUser\">SELECT 1</sql>\n</sqlMapper>\n");
        }
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(f1, (File) null));
    }

    // ----- loadFrom(InputStream) -----

    @Test
    void testLoadFromInputStream() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sqlMapper><sql id=\"findUser\" timeout=\"5\">SELECT * FROM users</sql></sqlMapper>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        SqlMapper mapper = SqlMapper.loadFrom(is);
        assertEquals(1, mapper.size());
        assertEquals("SELECT * FROM users", mapper.get("findUser").originalSql());
        assertEquals("5", mapper.attributes("findUser").get("timeout"));
    }

    @Test
    void testLoadFromInputStream_callerCanCloseInTryWithResources() throws IOException {
        // The caller opens the stream and closes it via try-with-resources; loadFrom() must not
        // throw or interfere with that (the underlying parser may consume/close the stream).
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sqlMapper><sql id=\"q\">SELECT 1</sql></sqlMapper>";
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            SqlMapper mapper = SqlMapper.loadFrom(is);
            assertEquals(1, mapper.size());
            assertEquals("SELECT 1", mapper.get("q").originalSql());
        }
    }

    @Test
    void testLoadFromInputStream_nestedSqlMapperIsNotAcceptedAsRoot() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><sqlMapper><sql id=\"q\">SELECT 1</sql></sqlMapper></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(is));
    }

    @Test
    void testLoadFromInputStream_noSqlMapperElementThrows() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> SqlMapper.loadFrom(is));
    }

    @Test
    void testLoadFromInputStream_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom((InputStream) null));
    }

    @Test
    void testLoadFromSingleFile() throws IOException {
        File f1 = new File(tempDir, "single.xml");
        try (FileWriter w = new FileWriter(f1)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sqlMapper>\n  <sql id=\"findUser\" fetchSize=\"10\">SELECT * FROM users</sql>\n</sqlMapper>\n");
        }

        SqlMapper mapper = SqlMapper.loadFrom(f1);
        assertEquals(1, mapper.size());
        assertEquals("10", mapper.attributes("findUser").get("fetchSize"));
    }

    @Test
    void testLoadFromVarargs_DirectoryPathThrowsIae() {
        // A path that resolves to a directory is not a loadable file; the varargs overload must
        // reject it with IllegalArgumentException just like loadFrom(String) does.
        File directoryPath = tempDir;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom(directoryPath.getAbsolutePath(), new String[0]));
        assertTrue(e.getMessage().contains(directoryPath.getAbsolutePath()));
    }

    @Test
    void testLoadMissingFilePathThrowsPointedIae() {
        // Regression (2026-07-03): a path that resolves to no file used to surface as a bare NPE
        // from PropertiesUtil.formatPath(null) instead of the documented IllegalArgumentException.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SqlMapper.loadFrom("no/such/sql-mapper-file-xyz.xml"));
        assertTrue(e.getMessage().contains("no/such/sql-mapper-file-xyz.xml"));
    }

    @Test
    void testMaxIdLength() {
        SqlMapper mapper = new SqlMapper();
        String maxLengthId = "a".repeat(SqlMapper.MAX_ID_LENGTH);
        mapper.add(maxLengthId, ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get(maxLengthId));
    }

    @Test
    void testMultipleAttributes() {
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
    void testMultipleRemove() {
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
    void testRemove() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));

        assertNotNull(mapper.get("findAll"));

        mapper.remove("findAll");

        assertNull(mapper.get("findAll"));
    }

    @Test
    void testRemoveEmptyId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove("");
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    void testRemoveNonExistent() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove("nonExistent");
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    void testRemoveNullId() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(null);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    void testRemoveTooLongId() {
        SqlMapper mapper = new SqlMapper();
        String longId = "a".repeat(SqlMapper.MAX_ID_LENGTH + 1);
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));
        mapper.remove(longId);
        assertNotNull(mapper.get("query1"));
        assertEquals(1, mapper.ids().size());
    }

    @Test
    void testResultSetTypeMap() {
        assertNotNull(SqlMapper.RESULT_SET_TYPE_MAP);
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("FORWARD_ONLY"));
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_INSENSITIVE"));
        assertTrue(SqlMapper.RESULT_SET_TYPE_MAP.containsKey("SCROLL_SENSITIVE"));
    }

    @Test
    void testResultSetTypeMapValues() {
        assertEquals(java.sql.ResultSet.TYPE_FORWARD_ONLY, SqlMapper.RESULT_SET_TYPE_MAP.get("FORWARD_ONLY").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, SqlMapper.RESULT_SET_TYPE_MAP.get("SCROLL_INSENSITIVE").intValue());
        assertEquals(java.sql.ResultSet.TYPE_SCROLL_SENSITIVE, SqlMapper.RESULT_SET_TYPE_MAP.get("SCROLL_SENSITIVE").intValue());
    }

    @Test
    void testSaveTo() throws IOException {
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
    void testSaveTo_AttrsIdEntryDoesNotOverrideCanonicalId() throws IOException {
        // Bug fix: a stray "id" entry in the attrs map must never overwrite
        // the canonical mapping id during XML serialization.
        SqlMapper mapper = new SqlMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "evil"); // pretend the user passed a conflicting id
        attrs.put("batchSize", "50");
        mapper.add("realId", "SELECT 1", attrs);
        assertEquals("evil", mapper.attributes("realId").get("id"), "Programmatic attributes retain their pre-existing API-visible value");

        File output = new File(tempDir, "attrs-id-roundtrip.xml");
        mapper.saveTo(output);

        SqlMapper reloaded = SqlMapper.loadFrom(output.getAbsolutePath());
        assertNotNull(reloaded.get("realId"));
        assertNull(reloaded.get("evil"));
        assertEquals("50", reloaded.attributes("realId").get("batchSize"));
    }

    @Test
    void testSaveTo_controlCharacterAndNoncharacterBoundaries() throws IOException {
        // XML 1.0 excludes C0 controls other than tab/LF/CR, unpaired surrogates, and U+FFFE/U+FFFF.
        // DEL, C1 controls, and other noncharacters remain legal XML text.
        for (char rejected : new char[] { '\u0000', '\u0001', '\u000B', '\u000C', '\u001F', '\uFFFE', '\uFFFF' }) {
            SqlMapper mapper = new SqlMapper();
            mapper.add("q", "SELECT '" + rejected + "'");
            assertThrows(IllegalStateException.class, () -> mapper.saveTo(new ByteArrayOutputStream()));
        }

        for (char accepted : new char[] { '\t', '\n', '\r', '\u007F', '\u0080', '\u0085', '\u009F', '\uFDD0' }) {
            SqlMapper mapper = new SqlMapper();
            mapper.add("q", "SELECT '" + accepted + "'", Map.of("note", "a" + accepted + "b", "id", "ignored\uD800"));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mapper.saveTo(bos);

            try (InputStream is = new ByteArrayInputStream(bos.toByteArray())) {
                final SqlMapper loaded = SqlMapper.loadFrom(is);
                assertEquals("SELECT '" + accepted + "'", loaded.get("q").originalSql());
                assertEquals("a" + accepted + "b", loaded.attributes("q").get("note"));
            }
        }

    }

    @Test
    void testSaveTo_illegalXmlCharInAttributeValueThrowsIllegalStateException() {
        // Validate stored state before the serializer can write a partial document.
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1", Map.of("k", "a\u0001b"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> mapper.saveTo(new ByteArrayOutputStream()));
        assertTrue(e.getMessage().contains("Attribute 'k' for 'q'"));
    }

    @Test
    void testSaveTo_ParentDirectoryCreationFails() throws IOException {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        File blocker = new File(tempDir, "blocker");
        assertTrue(blocker.createNewFile());

        File target = new File(blocker, "nested/output.xml");

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> mapper.saveTo(target));
    }

    @Test
    void testSaveTo_RelativeFileWithoutParent() {
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
    void testSaveTo_RoundTripSpecialXmlCharacters() throws IOException {
        // SQL containing <, >, & must round-trip correctly through XML escaping.
        SqlMapper mapper = new SqlMapper();
        mapper.add("findGreater", "SELECT * FROM t WHERE a < 10 AND b > 5 AND c = 'a&b'", null);

        File output = new File(tempDir, "special-chars-roundtrip.xml");
        mapper.saveTo(output);

        SqlMapper reloaded = SqlMapper.loadFrom(output.getAbsolutePath());
        assertEquals("SELECT * FROM t WHERE a < 10 AND b > 5 AND c = 'a&b'", reloaded.get("findGreater").originalSql());
    }

    @Test
    void testSaveTo_TargetIsDirectory() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT 1"));

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> mapper.saveTo(tempDir));
    }

    @Test
    void testSaveTo_unpairedSurrogateMatrix() throws IOException {
        // Every unpaired surrogate is rejected before serialization, including a trailing high surrogate
        // that the underlying serializer otherwise silently drops.
        SqlMapper midHigh = new SqlMapper();
        midHigh.add("q", "SELECT '\uD800x'");
        assertThrows(IllegalStateException.class, () -> midHigh.saveTo(new ByteArrayOutputStream()));

        SqlMapper midLow = new SqlMapper();
        midLow.add("q", "SELECT '\uDC00x'");
        assertThrows(IllegalStateException.class, () -> midLow.saveTo(new ByteArrayOutputStream()));

        SqlMapper trailingLow = new SqlMapper();
        trailingLow.add("q", "SELECT '\uDC00");
        assertThrows(IllegalStateException.class, () -> trailingLow.saveTo(new ByteArrayOutputStream()));

        // the id and an attribute value go through the same serializer path as the body
        SqlMapper idMidHigh = new SqlMapper();
        idMidHigh.add("q\uD800x", "SELECT 1");
        assertThrows(IllegalStateException.class, () -> idMidHigh.saveTo(new ByteArrayOutputStream()));

        SqlMapper attrMidLow = new SqlMapper();
        attrMidLow.add("q", "SELECT 1", Map.of("k", "a\uDC00b"));
        assertThrows(IllegalStateException.class, () -> attrMidLow.saveTo(new ByteArrayOutputStream()));

        // A trailing lone high surrogate must not silently disappear from a stored definition.
        SqlMapper trailingHigh = new SqlMapper();
        trailingHigh.add("q", "SELECT '\uD800");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertThrows(IllegalStateException.class, () -> trailingHigh.saveTo(bos));
        assertEquals(0, bos.size());

        // a well-formed surrogate pair is written and reloads unchanged
        SqlMapper pair = new SqlMapper();
        pair.add("q", "SELECT '\uD800\uDC00'");
        ByteArrayOutputStream pairOut = new ByteArrayOutputStream();
        pair.saveTo(pairOut);

        try (InputStream is = new ByteArrayInputStream(pairOut.toByteArray())) {
            assertEquals("SELECT '\uD800\uDC00'", SqlMapper.loadFrom(is).get("q").originalSql());
        }
    }

    @Test
    void testSaveToCreatesParentDirectories() {
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

    // ----- saveTo(OutputStream) -----

    @Test
    void testSaveToFile_nullThrowsIllegalArgumentException() {
        SqlMapper mapper = new SqlMapper();

        assertThrows(IllegalArgumentException.class, () -> mapper.saveTo((File) null));
    }

    @Test
    void testSaveToFileStillWorksViaDelegation() throws IOException {
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
    void testSaveToOutputStream_doesNotCloseStream() {
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
    void testSaveToOutputStream_nullThrowsIllegalArgumentException() {
        SqlMapper mapper = new SqlMapper();

        assertThrows(IllegalArgumentException.class, () -> mapper.saveTo((OutputStream) null));
    }

    @Test
    void testSaveToOutputStream_preWrappedUncheckedIOExceptionKeepsItsOwnMessage() {
        // Regression (2026-09-20): UncheckedIOException extends UncheckedException, so an already-wrapped
        // UncheckedIOException was caught and rebuilt from its inner IOException, discarding its own message
        // ("custom wrapper message" surfaced as "java.io.IOException: inner io").
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1");

        final IOException inner = new IOException("inner io");

        // (a) raised inside the transformer: UncheckedException <- TransformerException <- UncheckedIOException
        OutputStream failingWrite = new OutputStream() {
            @Override
            public void write(int b) {
                throw new com.landawn.abacus.exception.UncheckedIOException("custom wrapper message", inner);
            }
        };

        com.landawn.abacus.exception.UncheckedIOException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> mapper.saveTo(failingWrite));
        assertEquals("custom wrapper message", e.getMessage());
        assertSame(inner, e.getCause());

        // (b) raised outside the transformer, at flush time: caught directly as the UncheckedException subclass
        OutputStream failingFlush = new OutputStream() {
            @Override
            public void write(int b) {
                // accepts everything
            }

            @Override
            public void flush() {
                throw new com.landawn.abacus.exception.UncheckedIOException("flush wrapper message", inner);
            }
        };

        com.landawn.abacus.exception.UncheckedIOException e2 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> mapper.saveTo(failingFlush));
        assertEquals("flush wrapper message", e2.getMessage());
        assertSame(inner, e2.getCause());
    }

    @Test
    void testSaveToOutputStream_roundTrip() {
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
    void testSaveToOutputStream_wrappedIOExceptionIsStillUncheckedIOException() {
        // The IOException may itself carry a cause: the FIRST IOException in the chain is what counts, not the root.
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1");

        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("write failed", new IllegalArgumentException("root"));
            }
        };

        com.landawn.abacus.exception.UncheckedIOException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> mapper.saveTo(failing));
        assertEquals("write failed", e.getCause().getMessage());
        assertTrue(e.getCause().getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testSaveToOutputStream_writeFailureIsUncheckedIOException() {
        // Regression (2026-09-20): XmlUtil.transform wraps a serializer failure as
        // UncheckedException(TransformerException <- SAXException <- IOException), so an I/O failure on the target
        // stream used to escape as UncheckedException instead of the documented UncheckedIOException.
        SqlMapper mapper = new SqlMapper();
        mapper.add("q", "SELECT 1");

        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("disk full");
            }
        };

        com.landawn.abacus.exception.UncheckedIOException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> mapper.saveTo(failing));
        assertEquals("disk full", e.getCause().getMessage());
    }

    @Test
    void testSaveToRejectsInvalidXmlBeforeTouchingDestination(@TempDir final File directory) throws IOException {
        for (final String invalid : new String[] { "\u0001", "\uD800", "\uDC00", "\uFFFE", "\uFFFF" }) {
            for (int location = 0; location < 3; location++) {
                final SqlMapper mapper = new SqlMapper();
                mapper.add("valid", "SELECT 1");
                mapper.add(location == 0 ? "id" + invalid : "bad", location == 1 ? "SELECT '" + invalid + "'" : "SELECT 2",
                        location == 2 ? Map.of("note", "text" + invalid) : Map.of());

                final ByteArrayOutputStream output = new ByteArrayOutputStream();
                output.write("existing".getBytes(StandardCharsets.UTF_8));
                assertThrows(IllegalStateException.class, () -> mapper.saveTo(output));
                assertEquals("existing", output.toString(StandardCharsets.UTF_8));

                final File file = new File(directory, "existing.xml");
                java.nio.file.Files.writeString(file.toPath(), "existing", StandardCharsets.UTF_8);
                assertThrows(IllegalStateException.class, () -> mapper.saveTo(file));
                assertEquals("existing", java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8));

                final File newFile = new File(directory, "not-created/output.xml");
                assertThrows(IllegalStateException.class, () -> mapper.saveTo(newFile.getAbsolutePath()));
                assertFalse(newFile.getParentFile().exists());
            }
        }
    }

    // ----- size -----

    @Test
    void testSize() {
        SqlMapper mapper = new SqlMapper();
        assertEquals(0, mapper.size());

        mapper.add("query1", ParsedSql.parse("SELECT 1"));
        assertEquals(1, mapper.size());

        mapper.add("query2", ParsedSql.parse("SELECT 2"));
        assertEquals(2, mapper.size());

        mapper.remove("query1");
        assertEquals(1, mapper.size());
    }

    @Test
    void testSqlMapper_addAndGet() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
        mapper.add("findUserById", parsedSql);

        ParsedSql retrieved = mapper.get("findUserById");
        assertNotNull(retrieved);
        assertEquals("select * from users where id = ?", retrieved.parameterizedSql());
    }

    @Test
    void testSqlMapper_addWithAttrs() {
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
    void testSqlMapper_copy() {
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
    void testSqlMapper_getReturnsNull() {
        SqlMapper mapper = new SqlMapper();
        ParsedSql unknown = mapper.get("nonExistentId");
        assertNull(unknown);
    }

    @Test
    void testSqlMapper_ids() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("findUser", "select * from users where id = ?", null);
        mapper.add("updateUser", "update users set name = ? where id = ?", null);
        Set<String> ids = mapper.ids();
        assertTrue(ids.contains("findUser"));
        assertTrue(ids.contains("updateUser"));
        assertEquals(2, ids.size());
    }

    @Test
    void testSqlMapper_isEmpty() {
        SqlMapper emptyMapper = new SqlMapper();
        boolean empty = emptyMapper.isEmpty();
        assertTrue(empty);
    }

    @Test
    void testSqlMapper_remove() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("deprecatedQuery", "select 1", null);
        assertNotNull(mapper.get("deprecatedQuery"));
        mapper.remove("deprecatedQuery");
        boolean removed = mapper.get("deprecatedQuery") == null;
        assertTrue(removed);
    }

    @Test
    void testStringPathSaveAndStructuredPathLoad() {
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

    @Test
    void testToString() {
        SqlMapper mapper = new SqlMapper();
        mapper.add("query1", ParsedSql.parse("SELECT * FROM users"));

        String str = mapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("query1"));
    }

    @Test
    void testToStringEmpty() {
        SqlMapper mapper = new SqlMapper();
        String str = mapper.toString();
        assertNotNull(str);
    }
}
