/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

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

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * the sql scripts are configured in xml file and mapped to short ids referenced in program. for example: <br>
 * {@code <sqlMapper>} <br>
 * {@code <sql id="findAccountById">select * from account where id=1</sql>} <br>
 * {@code <sql id="updateAccountNameById">update account set name=? where id=?</sql>} <br>
 * {@code </sqlMapper>}
 *
 */
public final class SQLMapper {

    public static final String SQL_MAPPER = "sqlMapper";

    public static final String SQL = "sql";

    public static final String ID = "id";

    public static final String BATCH_SIZE = "batchSize";

    public static final String FETCH_SIZE = "fetchSize";

    public static final String RESULT_SET_TYPE = "resultSetType";

    public static final ImmutableMap<String, Integer> RESULT_SET_TYPE_MAP = ImmutableMap.of("FORWARD_ONLY", ResultSet.TYPE_FORWARD_ONLY, "SCROLL_INSENSITIVE",
            ResultSet.TYPE_SCROLL_INSENSITIVE, "SCROLL_SENSITIVE", ResultSet.TYPE_SCROLL_SENSITIVE);

    public static final String TIMEOUT = "timeout";

    public static final int MAX_ID_LENGTH = 128;

    private final Map<String, ParsedSql> sqlMap = new LinkedHashMap<>();

    private final Map<String, ImmutableMap<String, String>> attrsMap = new HashMap<>();

    public SQLMapper() {
        // empty constructor
    }

    /**
     *
     * @param filePath it could be multiple file paths separated by ',' or ';'
     * @return
     */
    public static SQLMapper fromFile(final String filePath) {
        String[] filePaths = Splitter.with(WD.COMMA).trimResults().splitToArray(filePath);

        if (filePaths.length == 1) {
            filePaths = Splitter.with(WD.SEMICOLON).trimResults().splitToArray(filePath);
        }

        final SQLMapper sqlMapper = new SQLMapper();

        for (final String subFilePath : filePaths) {
            final File file = Configuration.formatPath(Configuration.findFile(subFilePath));

            try (InputStream is = new FileInputStream(file)) {

                final Document doc = XmlUtil.createDOMParser(true, true).parse(is);
                final NodeList sqlMapperEle = doc.getElementsByTagName(SQLMapper.SQL_MAPPER);

                if (0 == sqlMapperEle.getLength()) {
                    throw new RuntimeException("There is no 'sqlMapper' element. ");
                }

                final List<Element> sqlElementList = XmlUtil.getElementsByTagName((Element) sqlMapperEle.item(0), SQL);

                for (final Element sqlElement : sqlElementList) {
                    final Map<String, String> attrMap = XmlUtil.readAttributes(sqlElement);

                    sqlMapper.add(attrMap.remove(ID), XmlUtil.getTextContent(sqlElement), attrMap);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } catch (final SAXException e) {
                throw new ParseException(e);
            }
        }

        return sqlMapper;
    }

    /**
     *
     *
     * @return
     */
    public Set<String> keySet() {
        return sqlMap.keySet();
    }

    /**
     *
     * @param id
     * @return
     */
    public ParsedSql get(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return null;
        }

        return sqlMap.get(id);
    }

    /**
     * Gets the attrs.
     *
     * @param id
     * @return
     */
    public ImmutableMap<String, String> getAttrs(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return null; // NOSONAR
        }

        return attrsMap.get(id);
    }

    /**
     *
     * @param id
     * @param sql
     * @return
     */
    public ParsedSql add(final String id, final ParsedSql sql) {
        checkId(id);

        return sqlMap.put(id, sql);
    }

    /**
     *
     * @param id
     * @param sql
     * @param attrs
     */
    public void add(final String id, final String sql, final Map<String, String> attrs) {
        checkId(id);

        sqlMap.put(id, ParsedSql.parse(sql));
        attrsMap.put(id, ImmutableMap.copyOf(attrs));
    }

    /**
     *
     * @param id
     */
    private void checkId(final String id) {
        N.checkArgNotEmpty(id, "id");

        if (Strings.containsWhitespace(id)) {
            throw new IllegalArgumentException("Sql id: " + id + " contains whitespace characters");
        }

        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Sql id: " + id + " is too long. The maximum length for id is: " + MAX_ID_LENGTH);
        }

        if (sqlMap.containsKey(id)) {
            throw new IllegalArgumentException(id + " already exists with sql: " + sqlMap.get(id));
        }
    }

    /**
     *
     * @param id
     */
    public void remove(final String id) {
        if (Strings.isEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return;
        }

        sqlMap.remove(id);
    }

    /**
     *
     *
     * @return
     */
    public SQLMapper copy() {
        final SQLMapper copy = new SQLMapper();

        copy.sqlMap.putAll(sqlMap);
        copy.attrsMap.putAll(attrsMap);

        return copy;
    }

    /**
     *
     * @param file
     */
    public void saveTo(final File file) {

        try (OutputStream os = new FileOutputStream(file)) {
            final Document doc = XmlUtil.createDOMParser(true, true).newDocument();
            final Element sqlMapperNode = doc.createElement(SQLMapper.SQL_MAPPER);

            for (final Map.Entry<String, ParsedSql> sqlEntry : sqlMap.entrySet()) {
                final Element sqlNode = doc.createElement(SQL);
                sqlNode.setAttribute(ID, sqlEntry.getKey());

                if (!N.isEmpty(attrsMap.get(sqlEntry.getKey()))) {
                    final Map<String, String> attrs = attrsMap.get(sqlEntry.getKey());

                    for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                        sqlNode.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                final Text sqlText = doc.createTextNode(sqlEntry.getValue().sql());
                sqlNode.appendChild(sqlText);
                sqlMapperNode.appendChild(sqlNode);
            }

            doc.appendChild(sqlMapperNode);

            if (!file.exists()) {
                file.createNewFile(); //NOSONAR
            }

            XmlUtil.transform(doc, os);

            os.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @return
     */
    public boolean isEmpty() {
        return sqlMap.isEmpty();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return sqlMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof SQLMapper && N.equals(((SQLMapper) obj).sqlMap, sqlMap));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return sqlMap.toString();
    }
}
