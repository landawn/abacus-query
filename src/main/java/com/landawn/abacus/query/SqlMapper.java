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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.PropertiesUtil;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

/**
 * A utility class for managing SQL scripts stored in XML files and mapping them to short identifiers.
 * This class provides a centralized way to manage SQL queries outside of application code,
 * making it easier to maintain and modify SQL statements without recompiling.
 * 
 * <p>SQL scripts are configured in XML format with the following structure:</p>
 * <pre>{@code
 * <sqlMapper>
 *     <sql id="findAccountById">select * from account where id = ?</sql>
 *     <sql id="updateAccountNameById">update account set name = ? where id = ?</sql>
 *     <sql id="batchInsertAccounts" batchSize="100" fetchSize="50" timeout="30">
 *         insert into account (id, name, email) values (?, ?, ?)
 *     </sql>
 * </sqlMapper>
 * }</pre>
 * 
 * <p>Recognized XML attributes on {@code <sql>} elements are copied after basic structural validation
 * (attribute names must be nonempty and values non-null). This class interprets only the {@code id}
 * contract; remaining attributes are stored verbatim for downstream callers such as JDBC executors:</p>
 * <ul>
 *   <li>{@code id} - unique identifier for the SQL (required, max {@value #MAX_ID_LENGTH} characters,
 *       must not contain whitespace)</li>
 *   <li>{@code batchSize} - default batch size for batch operations</li>
 *   <li>{@code fetchSize} - JDBC fetch size</li>
 *   <li>{@code resultSetType} - one of {@code FORWARD_ONLY}, {@code SCROLL_INSENSITIVE},
 *       {@code SCROLL_SENSITIVE} (see {@link #RESULT_SET_TYPE_MAP})</li>
 *   <li>{@code timeout} - query timeout in seconds</li>
 * </ul>
 *
 * <p>Instances are mutable and are not thread-safe. Complete configuration before sharing a mapper,
 * or provide external synchronization when reads and writes may occur concurrently.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Load from single file
 * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
 * 
 * // Load from multiple files
 * SqlMapper mapper = SqlMapper.loadFrom("sql/users.xml,sql/orders.xml");
 * 
 * // Get parsed SQL
 * ParsedSql sql = mapper.get("findAccountById");
 * 
 * // Get SQL attributes
 * ImmutableMap<String, String> attrs = mapper.attributes("batchInsertAccounts");
 * String batchSize = attrs.get("batchSize");
 * }</pre>
 */
public final class SqlMapper {

    static final Logger logger = LoggerFactory.getLogger(SqlMapper.class);

    /**
     * XML element name for the root sqlMapper element.
     */
    public static final String SQL_MAPPER = "sqlMapper";

    /**
     * XML element name for individual sql elements.
     */
    public static final String SQL = "sql";

    /**
     * XML attribute name for the SQL identifier.
     */
    public static final String ID = "id";

    /**
     * XML attribute name for batch size configuration.
     */
    public static final String BATCH_SIZE = "batchSize";

    /**
     * XML attribute name for JDBC fetch size configuration.
     */
    public static final String FETCH_SIZE = "fetchSize";

    /**
     * XML attribute name for JDBC result set type configuration.
     */
    public static final String RESULT_SET_TYPE = "resultSetType";

    /**
     * Mapping of result set type names to their JDBC constant values.
     * Supported types: FORWARD_ONLY, SCROLL_INSENSITIVE, SCROLL_SENSITIVE.
     */
    public static final ImmutableMap<String, Integer> RESULT_SET_TYPE_MAP = ImmutableMap.of("FORWARD_ONLY", ResultSet.TYPE_FORWARD_ONLY, "SCROLL_INSENSITIVE",
            ResultSet.TYPE_SCROLL_INSENSITIVE, "SCROLL_SENSITIVE", ResultSet.TYPE_SCROLL_SENSITIVE);

    /**
     * XML attribute name for query timeout configuration.
     */
    public static final String TIMEOUT = "timeout";

    /**
     * Maximum allowed length for SQL identifiers ({@value}).
     */
    public static final int MAX_ID_LENGTH = 128;

    private final Map<String, ParsedSql> sqlMap = new LinkedHashMap<>();

    private final Map<String, ImmutableMap<String, String>> attrsMap = new HashMap<>();

    /**
     * Creates an empty SqlMapper instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * boolean empty = mapper.isEmpty();   // true
     *
     * // Populate it programmatically
     * mapper.add("findUserById", ParsedSql.parse("select * from users where id = ?"));
     * boolean nowEmpty = mapper.isEmpty();   // false
     * }</pre>
     *
     */
    public SqlMapper() {
        // empty constructor
    }

    /**
     * Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
     * Multiple file paths can be specified separated by comma (,) or semicolon (;).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single file
     * SqlMapper mapper = SqlMapper.loadFrom("config/sql-mapper.xml");
     * 
     * // Multiple files
     * SqlMapper mapper = SqlMapper.loadFrom("sql/users.xml,sql/orders.xml,sql/products.xml");
     * // or
     * SqlMapper mapper = SqlMapper.loadFrom("sql/users.xml;sql/orders.xml;sql/products.xml");
     * }</pre>
     *
     * @param filePaths one or more file paths separated by ',' or ';' (must not be {@code null} or empty).
     *        Each path is resolved against the literal location first; when no file exists there, the common
     *        configuration directories are searched as a fallback
     * @return a new SqlMapper instance loaded with SQL definitions from the specified files
     * @throws IllegalArgumentException if {@code filePaths} is {@code null}, empty, or resolves to no non-empty paths
     *         after splitting, if no file can be found for one of the paths, or if a loaded {@code <sql>} element has
     *         an invalid id (empty, containing whitespace, exceeding {@link #MAX_ID_LENGTH} characters, or duplicated)
     *         or a blank SQL body
     * @throws UncheckedIOException if an I/O error occurs reading the files
     * @throws ParsingException if the XML content is invalid, or if any loaded document does not have {@code <sqlMapper>} as its root element
     */
    public static SqlMapper loadFrom(final String filePaths) {
        N.checkArgNotEmpty(filePaths, "filePaths");
        final String[] rawFilePaths = Splitter.with(SK.COMMA).trimResults().splitToArray(filePaths.replace(SK.SEMICOLON, SK.COMMA));
        final List<String> parsedFilePaths = N.newArrayList(rawFilePaths.length);

        for (final String subFilePath : rawFilePaths) {
            if (!Strings.isEmpty(subFilePath)) {
                parsedFilePaths.add(subFilePath);
            }
        }

        if (parsedFilePaths.isEmpty()) {
            throw new IllegalArgumentException("File path is empty after splitting: " + filePaths);
        }

        final SqlMapper sqlMapper = new SqlMapper();

        for (final String subFilePath : parsedFilePaths) {
            final File foundFile = PropertiesUtil.findFile(subFilePath);

            // findFile returns null when the path exists neither literally nor in the common
            // configuration directories; without this check formatPath would throw a bare NPE.
            if (foundFile == null) {
                throw new IllegalArgumentException("No file found for path: " + subFilePath);
            }

            loadFile(sqlMapper, PropertiesUtil.formatPath(foundFile));
        }

        return sqlMapper;
    }

    /**
     * Creates a SqlMapper by loading separately supplied file paths.
     *
     * @param firstFilePath the first XML mapper path; must not be {@code null} or empty
     * @param additionalFilePaths additional XML mapper paths; no element may be {@code null} or empty
     * @return a new mapper containing definitions from every supplied path
     * @throws IllegalArgumentException if either argument is {@code null}, if the first path or any additional path is empty,
     *         if a path cannot be found, or if a loaded SQL definition is invalid or duplicated
     * @throws UncheckedIOException if an I/O error occurs while reading a file
     * @throws ParsingException if any XML document is invalid or does not have {@code <sqlMapper>} as its root element
     */
    public static SqlMapper loadFrom(final String firstFilePath, final String... additionalFilePaths) {
        N.checkArgNotEmpty(firstFilePath, "firstFilePath");
        N.checkArgNotNull(additionalFilePaths, "additionalFilePaths");

        final SqlMapper sqlMapper = new SqlMapper();
        loadPath(sqlMapper, firstFilePath);

        for (final String filePath : additionalFilePaths) {
            N.checkArgNotEmpty(filePath, "additionalFilePath");
            loadPath(sqlMapper, filePath);
        }

        return sqlMapper;
    }

    private static void loadPath(final SqlMapper sqlMapper, final String filePath) {
        final File foundFile = PropertiesUtil.findFile(filePath);

        if (foundFile == null) {
            throw new IllegalArgumentException("No file found for path: " + filePath);
        }

        loadFile(sqlMapper, PropertiesUtil.formatPath(foundFile));
    }

    /**
     * Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
     * Each file must contain a {@code <sqlMapper>} root element; definitions from all files are merged
     * into a single mapper. Duplicate ids across files are rejected, just as within a single file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = SqlMapper.loadFrom(new File("sql/users.xml"), new File("sql/orders.xml"));
     * }</pre>
     *
     * @param files one or more XML files to load (must not be {@code null} or empty, and no element may be {@code null})
     * @return a new SqlMapper instance loaded with SQL definitions from the specified files
     * @throws IllegalArgumentException if {@code files} is {@code null} or empty, if any element of {@code files} is
     *         {@code null}, or if a loaded {@code <sql>} element has an invalid id (empty, containing whitespace,
     *         exceeding {@link #MAX_ID_LENGTH} characters, or duplicated) or a blank SQL body
     * @throws UncheckedIOException if an I/O error occurs reading the files
     * @throws ParsingException if the XML content is invalid, or if any loaded document does not have {@code <sqlMapper>} as its root element
     */
    public static SqlMapper loadFrom(final File... files) {
        N.checkArgNotEmpty(files, "files");

        final SqlMapper sqlMapper = new SqlMapper();

        for (final File file : files) {
            N.checkArgNotNull(file, "file");
            loadFile(sqlMapper, file);
        }

        return sqlMapper;
    }

    /**
     * Creates a SqlMapper instance by loading SQL definitions from the supplied input stream.
     * The stream content must contain a {@code <sqlMapper>} root element. The caller opens the stream
     * and remains responsible for closing it (typically via try-with-resources); this method does not
     * call {@code close()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("sql/queries.xml")) {
     *     SqlMapper mapper = SqlMapper.loadFrom(is);
     * }
     * }</pre>
     *
     * @param inputStream the input stream to read the XML SQL definitions from (must not be {@code null})
     * @return a new SqlMapper instance loaded with SQL definitions from the stream
     * @throws IllegalArgumentException if {@code inputStream} is {@code null}, or if a loaded {@code <sql>} element has an invalid
     *         id (empty, containing whitespace, exceeding {@link #MAX_ID_LENGTH} characters, or duplicated) or a blank SQL body
     * @throws UncheckedIOException if an I/O error occurs reading the stream
     * @throws ParsingException if the XML content is invalid, or does not have {@code <sqlMapper>} as its root element
     */
    public static SqlMapper loadFrom(final InputStream inputStream) {
        N.checkArgNotNull(inputStream, "inputStream");

        final SqlMapper sqlMapper = new SqlMapper();
        loadStream(sqlMapper, inputStream, "input stream");

        return sqlMapper;
    }

    /**
     * Loads the SQL definitions from {@code file} into {@code sqlMapper}, opening and closing the file stream.
     *
     * @param sqlMapper the mapper to populate
     * @param file the XML file to read
     */
    private static void loadFile(final SqlMapper sqlMapper, final File file) {
        if (logger.isInfoEnabled()) {
            logger.info("Loading SQL mapper from file: {}", file.getAbsolutePath());
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            loadStream(sqlMapper, inputStream, file.getAbsolutePath());
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read SQL mapper file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Parses the XML from {@code inputStream} and merges its {@code <sql>} definitions into {@code sqlMapper}.
     * The stream is not closed by this method.
     *
     * @param sqlMapper the mapper to populate
     * @param inputStream the input stream to read
     * @param sourceLabel a human-readable label identifying the source, used in log and error messages
     */
    private static void loadStream(final SqlMapper sqlMapper, final InputStream inputStream, final String sourceLabel) {
        try {
            final Document doc = XmlUtil.createDOMParser(true, true).parse(inputStream);
            final Element sqlMapperElement = doc.getDocumentElement();

            if (sqlMapperElement == null || !SqlMapper.SQL_MAPPER.equals(sqlMapperElement.getTagName())) {
                throw new ParsingException("Root element must be '" + SqlMapper.SQL_MAPPER + "' in: " + sourceLabel);
            }

            final List<Element> sqlElementList = XmlUtil.getElementsByTagName(sqlMapperElement, SQL);

            for (final Element sqlElement : sqlElementList) {
                final Map<String, String> attrMap = XmlUtil.readAttributes(sqlElement);

                sqlMapper.add(attrMap.remove(ID), XmlUtil.getTextContent(sqlElement), attrMap);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Loaded {} SQL statements from: {}", sqlElementList.size(), sourceLabel);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read SQL mapper XML from: " + sourceLabel, e);
        } catch (final SAXException e) {
            throw new ParsingException("Failed to parse SQL mapper XML from: " + sourceLabel, e);
        }
    }

    /**
     * Returns an immutable snapshot of all SQL identifiers registered in this mapper.
     * The returned set is a copy taken at call time; subsequent modifications to this mapper
     * (via {@link #add}, {@link #remove}, etc.) are <i>not</i> reflected in a previously returned set.
     * The snapshot maintains the insertion order of SQL definitions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
     * ImmutableSet<String> ids = mapper.ids();
     * ids.forEach(id -> System.out.println("Available SQL: " + id));
     * }</pre>
     *
     * @return an immutable snapshot of all SQL identifiers in this mapper, maintaining insertion order
     */
    public ImmutableSet<String> ids() {
        return ImmutableSet.copyOf(new LinkedHashSet<>(sqlMap.keySet()));
    }

    /**
     * Retrieves the parsed SQL associated with the specified identifier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
     *
     * ParsedSql sql = mapper.get("findAccountById");
     * if (sql != null) {
     *     String parameterizedSql = sql.parameterizedSql();
     *     // Use with PreparedStatement
     *     PreparedStatement stmt = connection.prepareStatement(parameterizedSql);
     * }
     *
     * // Returns null for unknown ids
     * ParsedSql unknown = mapper.get("nonExistentId");
     * // unknown is null
     * }</pre>
     *
     * @param id the SQL identifier to look up
     * @return the {@link ParsedSql} object, or {@code null} if the id is {@code null}, empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
     */
    public ParsedSql get(final String id) {
        if (!isValidLookupId(id)) {
            return null;
        }

        return sqlMap.get(id);
    }

    /**
     * Returns {@code true} if this mapper contains an SQL registered under the specified identifier.
     * The id is validated using the same rules as {@link #get(String)}: an id that is {@code null},
     * empty, or exceeds {@link #MAX_ID_LENGTH} characters can never be present and therefore yields
     * {@code false} without throwing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
     * boolean present = mapper.containsId("findAccountById");
     * boolean absent = mapper.containsId("nonExistentId");   // false
     * }</pre>
     *
     * @param id the SQL identifier to test
     * @return {@code true} if a matching SQL is registered; {@code false} if the id is {@code null}, empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
     * @see #get(String)
     */
    public boolean containsId(final String id) {
        return isValidLookupId(id) && sqlMap.containsKey(id);
    }

    /**
     * Retrieves the attributes associated with the specified SQL identifier.
     * Attributes may include batchSize, fetchSize, resultSetType, timeout, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given XML: <sql id="batchInsert" batchSize="100" timeout="30">...</sql>
     * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
     *
     * ImmutableMap<String, String> attrs = mapper.attributes("batchInsert");
     * if (attrs != null) {
     *     String batchSize = attrs.get("batchSize");   // "100"
     *     String timeout = attrs.get("timeout");       // "30"
     * }
     *
     * // Returns null for unknown ids
     * ImmutableMap<String, String> unknown = mapper.attributes("nonExistentId");
     * // unknown is null
     * }</pre>
     *
     * @param id the SQL identifier to look up
     * @return an immutable map of attribute names to values, or {@code null} if the id is {@code null}, empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
     */
    public ImmutableMap<String, String> attributes(final String id) {
        if (!isValidLookupId(id)) {
            return null; // NOSONAR
        }

        return attrsMap.get(id);
    }

    /**
     * Adds a parsed SQL with the specified identifier.
     * This method validates the ID and throws an exception if an SQL with the same ID already exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
     * mapper.add("findUserById", parsedSql);
     *
     * // Later, retrieve the SQL
     * ParsedSql retrieved = mapper.get("findUserById");   // returns the same parsedSql instance just added
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the parsed SQL to associate with the identifier (must not be {@code null})
     * @throws IllegalArgumentException if {@code sql} is {@code null}, or if the id is {@code null}/empty, contains whitespace,
     *                                  exceeds {@link #MAX_ID_LENGTH} characters, or already exists
     */
    public void add(final String id, final ParsedSql sql) {
        N.checkArgNotNull(sql, "sql");
        checkId(id);

        sqlMap.put(id, sql);
        attrsMap.put(id, ImmutableMap.empty());

        if (logger.isDebugEnabled()) {
            logger.debug("Added SQL mapping: id='{}'", id);
        }
    }

    /**
     * Adds a parsed SQL with the specified identifier and attributes.
     * This method validates the ID and throws an exception if an SQL with the same ID already exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * ParsedSql parsedSql = ParsedSql.parse("insert into users (id, name) values (?, ?)");
     * Map<String, String> attrs = new HashMap<>();
     * attrs.put("batchSize", "100");
     * mapper.add("insertUser", parsedSql, attrs);
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the parsed SQL to associate with the identifier (must not be {@code null})
     * @param attributes additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout);
     *              may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
     * @throws IllegalArgumentException if {@code sql} is {@code null}; if the id is {@code null}/empty, contains whitespace,
     *                                  exceeds {@link #MAX_ID_LENGTH} characters, or already exists; or if {@code attributes}
     *                                  contains a {@code null}/empty/invalid or namespace-qualified XML attribute name, or a {@code null} value
     */
    public void add(final String id, final ParsedSql sql, final Map<String, String> attributes) {
        N.checkArgNotNull(sql, "sql");
        checkId(id);

        final ImmutableMap<String, String> immutableAttrs = copyAttributes(attributes);
        sqlMap.put(id, sql);
        attrsMap.put(id, immutableAttrs);

        if (logger.isDebugEnabled()) {
            logger.debug("Added SQL mapping: id='{}'", id);
        }
    }

    /**
     * Adds a SQL string with the specified identifier and no attributes.
     * The SQL string will be parsed using {@link ParsedSql#parse(String)} before storing.
     * This is a convenience overload of {@link #add(String, String, Map)} with an empty attribute map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * mapper.add("findAll", "select * from users");
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the SQL string to parse and store (must not be {@code null} or blank)
     * @throws IllegalArgumentException if {@code sql} is {@code null} or blank (blank is rejected by {@link ParsedSql#parse(String)}),
     *                                  or if the id is {@code null}/empty, contains whitespace, exceeds {@link #MAX_ID_LENGTH}
     *                                  characters, or already exists
     */
    public void add(final String id, final String sql) {
        add(id, sql, null);
    }

    /**
     * Adds a SQL string with the specified identifier and attributes.
     * The SQL string will be parsed using {@link ParsedSql#parse(String)} before storing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * Map<String, String> attrs = new HashMap<>();
     * attrs.put("batchSize", "100");
     * attrs.put("timeout", "30");
     * mapper.add("insertUser", "insert into users (id, name) values (?, ?)", attrs);
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the SQL string to parse and store (must not be {@code null} or blank)
     * @param attributes additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout);
     *              may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
     * @throws IllegalArgumentException if {@code sql} is {@code null} or blank (blank is rejected by {@link ParsedSql#parse(String)});
     *                                  if the id is {@code null}/empty, contains whitespace, exceeds {@link #MAX_ID_LENGTH}
     *                                  characters, or already exists; or if {@code attributes} contains a {@code null}/empty key
     *                                  or a {@code null} value; or an invalid/namespace-qualified XML attribute name
     */
    public void add(final String id, final String sql, final Map<String, String> attributes) {
        N.checkArgNotNull(sql, "sql");
        checkId(id);

        final ParsedSql parsedSql = ParsedSql.parse(sql);
        final ImmutableMap<String, String> immutableAttrs = copyAttributes(attributes);
        sqlMap.put(id, parsedSql);
        attrsMap.put(id, immutableAttrs);

        if (logger.isDebugEnabled()) {
            logger.debug("Added SQL mapping: id='{}'", id);
        }
    }

    /**
     * Validates the SQL identifier according to the following rules:
     * <ul>
     *   <li>Must not be empty</li>
     *   <li>Must not contain whitespace</li>
     *   <li>Must not exceed {@value #MAX_ID_LENGTH} characters</li>
     *   <li>Must not already exist in the mapper</li>
     * </ul>
     *
     * @param id the identifier to validate
     * @throws IllegalArgumentException if any validation rule is violated
     */
    private void checkId(final String id) {
        N.checkArgNotEmpty(id, "id");

        if (Strings.containsWhitespace(id)) {
            throw new IllegalArgumentException("SQL id '" + id + "' contains whitespace characters");
        }

        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("SQL id '" + id + "' exceeds maximum length of " + MAX_ID_LENGTH + " characters");
        }

        if (sqlMap.containsKey(id)) {
            throw new IllegalArgumentException("SQL id '" + id + "' already exists. Use a unique identifier");
        }
    }

    private static ImmutableMap<String, String> copyAttributes(final Map<String, String> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return ImmutableMap.empty();
        }

        // Snapshot first so validation and copying see one stable set of entries.
        final Map<String, String> snapshot = new LinkedHashMap<>(attrs);

        final Document validationDocument = XmlUtil.createDOMParser(true, true).newDocument();

        for (final Map.Entry<String, String> entry : snapshot.entrySet()) {
            N.checkArgNotEmpty(entry.getKey(), "attribute name");
            N.checkArgNotNull(entry.getValue(), "attribute value for '" + entry.getKey() + "'");

            if (entry.getKey().indexOf(':') >= 0 || "xmlns".equals(entry.getKey())) {
                throw new IllegalArgumentException("Namespace-qualified XML attributes are not supported: " + entry.getKey());
            }

            try {
                // Delegate XML Name validation to the same DOM implementation used by saveTo(...), rather
                // than maintaining an incomplete ASCII-only approximation of the XML Name grammar.
                validationDocument.createAttribute(entry.getKey());
            } catch (final DOMException e) {
                throw new IllegalArgumentException("Invalid XML attribute name: " + entry.getKey(), e);
            }
        }

        return ImmutableMap.copyOf(snapshot);
    }

    /**
     * Returns {@code true} if {@code id} could plausibly be a stored key: non-empty and no longer
     * than {@link #MAX_ID_LENGTH}. Used by the silent lookup/remove methods to skip ids that can
     * never be present (as opposed to {@link #checkId(String)}, which throws on invalid input).
     *
     * @param id the identifier to test
     * @return {@code true} if the id is non-null, non-empty, and does not exceed {@link #MAX_ID_LENGTH} characters
     */
    private static boolean isValidLookupId(final String id) {
        return Strings.isNotEmpty(id) && id.length() <= MAX_ID_LENGTH;
    }

    /**
     * Removes the SQL and its attributes associated with the specified identifier.
     * If the id is {@code null}, empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found, this method does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
     * mapper.remove("deprecatedQuery");
     * // Verify removal
     * boolean removed = mapper.get("deprecatedQuery") == null;
     * }</pre>
     *
     * @param id the SQL identifier to remove
     */
    public void remove(final String id) {
        if (!isValidLookupId(id)) {
            return;
        }

        final boolean removedSql = sqlMap.remove(id) != null;
        final boolean removedAttrs = attrsMap.remove(id) != null;

        if ((removedSql || removedAttrs) && logger.isDebugEnabled()) {
            logger.debug("Removed SQL mapping: id='{}'", id);
        }
    }

    /**
     * Creates a shallow copy of this SqlMapper instance.
     * The copy contains references to the same ParsedSql objects and attribute maps from the original.
     * Modifications to one mapper (adding/removing entries) will not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper original = SqlMapper.loadFrom("sql/queries.xml");
     * SqlMapper copy = original.copy();
     *
     * // Modifications to the copy do not affect the original
     * copy.add("newQuery", ParsedSql.parse("SELECT 1"));
     * boolean originalHasIt = original.get("newQuery") != null;  // false
     * boolean copyHasIt = copy.get("newQuery") != null;          // true
     * }</pre>
     *
     * @return a new SqlMapper instance with the same SQL definitions and attributes
     */
    public SqlMapper copy() {
        final SqlMapper copy = new SqlMapper();

        copy.sqlMap.putAll(sqlMap);
        copy.attrsMap.putAll(attrsMap);

        return copy;
    }

    /**
     * Saves all SQL definitions in this mapper to an XML file.
     * The output format matches the expected input format for {@link #loadFrom(String)}.
     * If the file already exists, it will be overwritten.
     *
     * <p>The canonical SQL identifier (the registered map key) is always written as the
     * {@code id} attribute and is protected from being overridden: any stray {@code id}
     * entry in a SQL's attributes map is ignored when emitting attributes.</p>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * <sqlMapper>
     *     <sql id="findUser" fetchSize="100">select * from users where id = ?</sql>
     *     <sql id="updateUser">update users set name = ? where id = ?</sql>
     * </sqlMapper>
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * mapper.add("findUser", "select * from users where id = ?", Collections.emptyMap());
     * mapper.saveTo(new File("sql/queries.xml"));
     * }</pre>
     *
     * @param file the file to write to (will be created if it doesn't exist; parent directories will be created if needed)
     * @throws IllegalArgumentException if {@code file} is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while creating or writing to the file
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void saveTo(final File file) {
        N.checkArgNotNull(file, "file");

        final File parent = file.getParentFile();

        // mkdirs-then-isDirectory is the race-free idiom: mkdirs() returns false when another thread or
        // process created the directory between any exists() check and the call.
        if (parent != null && !(parent.mkdirs() || parent.isDirectory())) {
            throw new UncheckedIOException(new IOException("Failed to create parent directories for file: " + file.getAbsolutePath()));
        }

        try (OutputStream os = new FileOutputStream(file)) {
            saveTo(os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes this mapper to the specified file path.
     *
     * @param filePath the target file path; must not be {@code null} or empty
     * @throws IllegalArgumentException if {@code filePath} is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs while creating or writing the file
     */
    public void saveTo(final String filePath) {
        N.checkArgNotEmpty(filePath, "filePath");
        saveTo(new File(filePath));
    }

    /**
     * Writes all SQL definitions in this mapper to the supplied output stream as XML.
     * The output format matches the expected input format for {@link #loadFrom(InputStream)}.
     * The stream is flushed but <i>not</i> closed by this method; the caller retains ownership
     * and is responsible for closing it.
     *
     * <p>The canonical SQL identifier (the registered map key) is always written as the
     * {@code id} attribute and is protected from being overridden: any stray {@code id}
     * entry in a SQL's attributes map is ignored when emitting attributes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * mapper.add("findUser", "select * from users where id = ?");
     * try (OutputStream os = new FileOutputStream("sql/queries.xml")) {
     *     mapper.saveTo(os);
     * }
     * }</pre>
     *
     * @param outputStream the output stream to write to (not closed by this method)
     * @throws IllegalArgumentException if {@code outputStream} is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while writing to the stream
     */
    public void saveTo(final OutputStream outputStream) {
        N.checkArgNotNull(outputStream, "outputStream");

        try {
            final Document doc = XmlUtil.createDOMParser(true, true).newDocument();
            final Element sqlMapperNode = doc.createElement(SqlMapper.SQL_MAPPER);

            for (final Map.Entry<String, ParsedSql> sqlEntry : sqlMap.entrySet()) {
                final Element sqlNode = doc.createElement(SQL);

                final Map<String, String> attrs = attrsMap.get(sqlEntry.getKey());

                if (!N.isEmpty(attrs)) {
                    for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                        // Skip any stray "id" attribute in the attrs map so it cannot
                        // overwrite the canonical id we are about to set below.
                        if (ID.equals(entry.getKey())) {
                            continue;
                        }
                        sqlNode.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                // Set the id last to guarantee the entry key wins regardless of attrs contents.
                sqlNode.setAttribute(ID, sqlEntry.getKey());

                final Text sqlToken = doc.createTextNode(sqlEntry.getValue().originalSql());
                sqlNode.appendChild(sqlToken);
                sqlMapperNode.appendChild(sqlNode);
            }

            doc.appendChild(sqlMapperNode);

            XmlUtil.transform(doc, outputStream);

            outputStream.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the number of SQL definitions registered in this mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * int empty = mapper.size();   // 0
     * mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));
     * int one = mapper.size();     // 1
     * }</pre>
     *
     * @return the number of registered SQL definitions
     */
    public int size() {
        return sqlMap.size();
    }

    /**
     * Checks if this mapper contains no SQL definitions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper emptyMapper = new SqlMapper();
     * boolean empty = emptyMapper.isEmpty();  // true
     *
     * SqlMapper loadedMapper = SqlMapper.loadFrom("sql/queries.xml");
     * boolean hasEntries = !loadedMapper.isEmpty();  // true (assuming file has SQL definitions)
     * }</pre>
     *
     * @return {@code true} if the mapper contains no SQL definitions, {@code false} otherwise
     */
    public boolean isEmpty() {
        return sqlMap.isEmpty();
    }

    /**
     * Returns the hash code value for this {@code SqlMapper}.
     * The hash code is based on the internal SQL map and the attributes map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper a = new SqlMapper();
     * a.add("q", ParsedSql.parse("SELECT 1"));
     * SqlMapper b = new SqlMapper();
     * b.add("q", ParsedSql.parse("SELECT 1"));
     * // Equal mappers produce equal hash codes
     * boolean sameHash = a.hashCode() == b.hashCode();   // true
     * }</pre>
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(sqlMap, attrsMap);
    }

    /**
     * Compares this {@code SqlMapper} to another object for equality.
     * Two {@code SqlMapper} instances are considered equal if they contain the same id-to-SQL mappings
     * and id-to-attributes mappings (order-independent, per {@link Map#equals(Object)}).
     * Note: equality ignores entry order, although {@link #ids()} and {@link #toString()} reflect insertion order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper a = new SqlMapper();
     * a.add("q", ParsedSql.parse("SELECT 1"));
     * SqlMapper b = new SqlMapper();
     * b.add("q", ParsedSql.parse("SELECT 1"));
     * SqlMapper c = new SqlMapper();
     * c.add("q", ParsedSql.parse("SELECT 2"));
     *
     * boolean eq = a.equals(b);                // true (same id-to-SQL mappings)
     * boolean ne = a.equals(c);                // false (different SQL for "q")
     * boolean notMapper = a.equals("text");    // false (not a SqlMapper)
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code SqlMapper} whose internal SQL and attribute maps
     *         are equal to this mapper's; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof SqlMapper && N.equals(((SqlMapper) obj).sqlMap, sqlMap) && N.equals(((SqlMapper) obj).attrsMap, attrsMap));
    }

    /**
     * Returns a string representation of this {@code SqlMapper}.
     * The string contains all SQL definitions in the mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlMapper mapper = new SqlMapper();
     * String empty = mapper.toString();   // "{}"
     *
     * mapper.add("findUser", ParsedSql.parse("select * from users where id = ?"));
     * String s = mapper.toString();       // contains "findUser" and the parsed SQL
     * }</pre>
     *
     * @return a string representation of this SQL mapper
     */
    @Override
    public String toString() {
        return sqlMap.toString();
    }
}
