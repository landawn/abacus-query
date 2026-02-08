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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.PropertiesUtil;
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
 * <p>Supported attributes for SQL elements:</p>
 * <ul>
 *   <li>id - unique identifier for the SQL (required, max 128 characters)</li>
 *   <li>batchSize - default batch size for batch operations</li>
 *   <li>fetchSize - JDBC fetch size</li>
 *   <li>resultSetType - one of FORWARD_ONLY, SCROLL_INSENSITIVE, SCROLL_SENSITIVE</li>
 *   <li>timeout - query timeout in seconds</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Load from single file
 * SQLMapper mapper = SQLMapper.fromFile("sql/queries.xml");
 * 
 * // Load from multiple files
 * SQLMapper mapper = SQLMapper.fromFile("sql/users.xml,sql/orders.xml");
 * 
 * // Get parsed SQL
 * ParsedSql sql = mapper.get("findAccountById");
 * 
 * // Get SQL attributes
 * ImmutableMap<String, String> attrs = mapper.getAttrs("batchInsertAccounts");
 * String batchSize = attrs.get("batchSize");
 * }</pre>
 */
public final class SQLMapper {

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
     * Maximum allowed length for SQL identifiers.
     */
    public static final int MAX_ID_LENGTH = 128;

    private final Map<String, ParsedSql> sqlMap = new LinkedHashMap<>();

    private final Map<String, ImmutableMap<String, String>> attrsMap = new HashMap<>();

    /**
     * Creates an empty SQLMapper instance.
     */
    public SQLMapper() {
        // empty constructor
    }

    /**
     * Creates a SQLMapper instance by loading SQL definitions from one or more XML files.
     * Multiple file paths can be specified separated by comma (,) or semicolon (;).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single file
     * SQLMapper mapper = SQLMapper.fromFile("config/sql-mapper.xml");
     * 
     * // Multiple files
     * SQLMapper mapper = SQLMapper.fromFile("sql/users.xml,sql/orders.xml,sql/products.xml");
     * // or
     * SQLMapper mapper = SQLMapper.fromFile("sql/users.xml;sql/orders.xml;sql/products.xml");
     * }</pre>
     *
     * @param filePath one or more file paths separated by ',' or ';'
     * @return a new SQLMapper instance loaded with SQL definitions from the specified files
     * @throws UncheckedIOException if an I/O error occurs reading the files
     * @throws ParseException if the XML content is invalid
     * @throws RuntimeException if no 'sqlMapper' element is found in any file
     */
    public static SQLMapper fromFile(final String filePath) {
        String[] filePaths = Splitter.with(SK.COMMA).trimResults().splitToArray(filePath);

        if (filePaths.length == 1) {
            filePaths = Splitter.with(SK.SEMICOLON).trimResults().splitToArray(filePath);
        }

        final SQLMapper sqlMapper = new SQLMapper();

        for (final String subFilePath : filePaths) {
            final File file = PropertiesUtil.formatPath(PropertiesUtil.findFile(subFilePath));

            try (InputStream is = new FileInputStream(file)) {

                final Document doc = XmlUtil.createDOMParser(true, true).parse(is);
                final NodeList sqlMapperEle = doc.getElementsByTagName(SQLMapper.SQL_MAPPER);

                if (0 == sqlMapperEle.getLength()) {
                    throw new RuntimeException("No 'sqlMapper' element found in the configuration");
                }

                final List<Element> sqlElementList = XmlUtil.getElementsByTagName((Element) sqlMapperEle.item(0), SQL);

                for (final Element sqlElement : sqlElementList) {
                    final Map<String, String> attrMap = XmlUtil.readAttributes(sqlElement);

                    sqlMapper.add(attrMap.remove(ID), XmlUtil.getTextContent(sqlElement), attrMap);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } catch (final SAXException e) {
                throw new ParsingException(e);
            }
        }

        return sqlMapper;
    }

    /**
     * Returns a set of all SQL identifiers registered in this mapper.
     * The returned set maintains the insertion order of SQL definitions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLMapper mapper = SQLMapper.fromFile("sql/queries.xml");
     * Set<String> sqlIds = mapper.keySet();
     * sqlIds.forEach(id -> System.out.println("Available SQL: " + id));
     * }</pre>
     *
     * @return a set view of all SQL identifiers in this mapper, maintaining insertion order
     */
    public Set<String> keySet() {
        return sqlMap.keySet();
    }

    /**
     * Retrieves the parsed SQL associated with the specified identifier.
     *
     * @param id the SQL identifier to look up
     * @return the ParsedSql object, or {@code null} if the id is empty, exceeds {@link #MAX_ID_LENGTH}, or not found
     */
    public ParsedSql get(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return null;
        }

        return sqlMap.get(id);
    }

    /**
     * Retrieves the attributes associated with the specified SQL identifier.
     * Attributes may include batchSize, fetchSize, resultSetType, timeout, etc.
     *
     * @param id the SQL identifier to look up
     * @return an immutable map of attribute names to values, or {@code null} if the id is empty, exceeds {@link #MAX_ID_LENGTH}, or not found
     */
    public ImmutableMap<String, String> getAttrs(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
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
     * SQLMapper mapper = new SQLMapper();
     * ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
     * mapper.add("findUserById", parsedSql);
     *
     * // Later, retrieve the SQL
     * ParsedSql retrieved = mapper.get("findUserById");
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the parsed SQL to associate with the identifier
     * @return the previous ParsedSql associated with the id, or {@code null} if there was no mapping
     * @throws IllegalArgumentException if the id is empty, contains whitespace, exceeds {@link #MAX_ID_LENGTH} characters, or already exists
     */
    public ParsedSql add(final String id, final ParsedSql sql) {
        checkId(id);

        return sqlMap.put(id, sql);
    }

    /**
     * Adds a SQL string with the specified identifier and attributes.
     * The SQL string will be parsed using {@link ParsedSql#parse(String)} before storing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLMapper mapper = new SQLMapper();
     * Map<String, String> attrs = new HashMap<>();
     * attrs.put("batchSize", "100");
     * attrs.put("timeout", "30");
     * mapper.add("insertUser", "insert into users (id, name) values (?, ?)", attrs);
     * }</pre>
     *
     * @param id the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
     * @param sql the SQL string to parse and store
     * @param attrs additional attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be empty but not null
     * @throws IllegalArgumentException if the id is empty, contains whitespace, exceeds {@link #MAX_ID_LENGTH} characters, or already exists
     */
    public void add(final String id, final String sql, final Map<String, String> attrs) {
        checkId(id);

        sqlMap.put(id, ParsedSql.parse(sql));
        attrsMap.put(id, ImmutableMap.copyOf(attrs));
    }

    /**
     * Validates the SQL identifier according to the following rules:
     * - Must not be empty
     * - Must not contain whitespace
     * - Must not exceed MAX_ID_LENGTH (128) characters
     * - Must not already exist in the mapper
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

    /**
     * Removes the SQL and its attributes associated with the specified identifier.
     * If the id is empty, exceeds {@link #MAX_ID_LENGTH}, or not found, this method does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLMapper mapper = SQLMapper.fromFile("sql/queries.xml");
     * mapper.remove("deprecatedQuery");
     * // Verify removal
     * boolean removed = mapper.get("deprecatedQuery") == null;
     * }</pre>
     *
     * @param id the SQL identifier to remove
     */
    public void remove(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return;
        }

        sqlMap.remove(id);
        attrsMap.remove(id);
    }

    /**
     * Creates a shallow copy of this SQLMapper instance.
     * The copy contains references to the same ParsedSql objects and attribute maps from the original.
     * Modifications to one mapper (adding/removing entries) will not affect the other.
     *
     * @return a new SQLMapper instance with the same SQL definitions and attributes
     */
    public SQLMapper copy() {
        final SQLMapper copy = new SQLMapper();

        copy.sqlMap.putAll(sqlMap);
        copy.attrsMap.putAll(attrsMap);

        return copy;
    }

    /**
     * Saves all SQL definitions in this mapper to an XML file.
     * The output format matches the expected input format for {@link #fromFile(String)}.
     * If the file already exists, it will be overwritten.
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
     * SQLMapper mapper = new SQLMapper();
     * mapper.add("findUser", "select * from users where id = ?", Collections.emptyMap());
     * mapper.saveTo(new File("sql/queries.xml"));
     * }</pre>
     *
     * @param file the file to write to (will be created if it doesn't exist; parent directories must exist)
     * @throws UncheckedIOException if an I/O error occurs while creating or writing to the file
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void saveTo(final File file) {
        final File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new UncheckedIOException(new IOException("Failed to create parent directories for file: " + file.getAbsolutePath()));
        }

        try (OutputStream os = new FileOutputStream(file)) {
            final Document doc = XmlUtil.createDOMParser(true, true).newDocument();
            final Element sqlMapperNode = doc.createElement(SQLMapper.SQL_MAPPER);

            for (final Map.Entry<String, ParsedSql> sqlEntry : sqlMap.entrySet()) {
                final Element sqlNode = doc.createElement(SQL);
                sqlNode.setAttribute(ID, sqlEntry.getKey());

                final Map<String, String> attrs = attrsMap.get(sqlEntry.getKey());
                if (!N.isEmpty(attrs)) {

                    for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                        sqlNode.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                final Text sqlText = doc.createTextNode(sqlEntry.getValue().sql());
                sqlNode.appendChild(sqlText);
                sqlMapperNode.appendChild(sqlNode);
            }

            doc.appendChild(sqlMapperNode);

            XmlUtil.transform(doc, os);

            os.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Checks if this mapper contains no SQL definitions.
     *
     * @return {@code true} if the mapper contains no SQL definitions, {@code false} otherwise
     */
    public boolean isEmpty() {
        return sqlMap.isEmpty();
    }

    /**
     * Returns the hash code value for this SQLMapper.
     * The hash code is based on the internal SQL map.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return sqlMap.hashCode();
    }

    /**
     * Compares this SQLMapper to another object for equality.
     * Two SQLMappers are considered equal if they contain the same SQL definitions.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof SQLMapper && N.equals(((SQLMapper) obj).sqlMap, sqlMap));
    }

    /**
     * Returns a string representation of this SQLMapper.
     * The string contains all SQL definitions in the mapper.
     *
     * @return a string representation of the SQL map
     */
    @Override
    public String toString() {
        return sqlMap.toString();
    }
}
