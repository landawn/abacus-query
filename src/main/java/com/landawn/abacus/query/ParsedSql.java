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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.Poolable;
import com.landawn.abacus.pool.PoolableWrapper;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Represents a parsed SQL statement with support for named parameters and parameterized queries.
 * This class handles SQL parsing to extract named parameters (e.g., :userId, #{userId}) and converts
 * them to standard JDBC parameter placeholders (?). It also supports Couchbase-style parameters.
 * 
 * <p>The class maintains an internal cache of parsed SQL statements for performance optimization.
 * Supported parameter formats include:</p>
 * <ul>
 *   <li>Named parameters: :paramName</li>
 *   <li>iBatis/MyBatis style: #{paramName}</li>
 *   <li>Couchbase style: $1, $2, etc.</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
 * String parameterized = parsed.parameterizedSql();   // "SELECT * FROM users WHERE id = ? AND status = ?"
 * List<String> params = parsed.namedParameters();   // ["userId", "status"]
 * }</pre>
 * 
 * @see SQLParser
 * @see SQLBuilder
 */
public final class ParsedSql {

    private static final int EVICT_TIME = 60 * 1000;

    private static final int LIVE_TIME = 24 * 60 * 60 * 1000;

    private static final int MAX_IDLE_TIME = 24 * 60 * 60 * 1000;

    private static final Set<String> opSqlPrefixSet = N.asSet(SK.SELECT, SK.INSERT, SK.UPDATE, SK.DELETE, SK.WITH, SK.MERGE, SK.CALL);

    private static final int factor = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    private static final KeyedObjectPool<String, PoolableWrapper<ParsedSql>> pool = PoolFactory.createKeyedObjectPool(1000 * factor, EVICT_TIME);

    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    private static final String RIGHT_OF_IBATIS_NAMED_PARAMETER = "}";

    private static final String PREFIX_OF_COUCHBASE_NAMED_PARAMETER = "$";

    private static final char _PREFIX_OF_COUCHBASE_NAMED_PARAMETER = PREFIX_OF_COUCHBASE_NAMED_PARAMETER.charAt(0);

    private final String sql;

    private final String parameterizedSql;

    private volatile String couchbaseParameterizedSql;

    private final ImmutableList<String> namedParameters;

    private volatile ImmutableList<String> couchbaseNamedParameters;

    private int parameterCount;

    private volatile int couchbaseParameterCount;

    private ParsedSql(final String sql) {
        this.sql = sql.trim();

        final List<String> words = SQLParser.parse(this.sql);

        boolean isOpSqlPrefix = false;
        for (final String word : words) {
            if (Strings.isNotEmpty(word) && !(word.equals(" ") || word.startsWith("--") || word.startsWith("/*"))) {
                isOpSqlPrefix = opSqlPrefixSet.contains(word.toUpperCase(Locale.ROOT));
                break;
            }
        }

        final List<String> namedParameterList = new ArrayList<>();
        int type = 0; // Use bit flags: 1=question mark, 2=named parameter, 4=iBatis parameter
        final int QUESTION_MARK_TYPE = 1;
        final int NAMED_PARAMETER_TYPE = 2;
        final int IBATIS_PARAMETER_TYPE = 4;

        if (isOpSqlPrefix) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                for (String word : words) {
                    if (word.equals(SK.QUESTION_MARK)) {
                        parameterCount++;
                        type |= QUESTION_MARK_TYPE;
                    } else if (word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) && word.endsWith(RIGHT_OF_IBATIS_NAMED_PARAMETER) && word.length() > 3) {
                        namedParameterList.add(word.substring(2, word.length() - 1));

                        word = SK.QUESTION_MARK;
                        parameterCount++;
                        type |= IBATIS_PARAMETER_TYPE;
                    } else if (word.length() >= 2 && word.charAt(0) == _PREFIX_OF_NAMED_PARAMETER && isValidNamedParameterChar(word.charAt(1))) {
                        namedParameterList.add(word.substring(1));

                        word = SK.QUESTION_MARK;
                        parameterCount++;
                        type |= NAMED_PARAMETER_TYPE;
                    }

                    if (Integer.bitCount(type) > 1) {
                        throw new IllegalArgumentException("Cannot mix parameter styles ('?', ':propName', '#{propName}') in the same SQL script");
                    }

                    sb.append(word);
                }

                final String tmpSql = Strings.stripToEmpty(sb.toString());
                parameterizedSql = tmpSql.endsWith(";") ? tmpSql.substring(0, tmpSql.length() - 1) : tmpSql;
                namedParameters = ImmutableList.wrap(namedParameterList);
            } finally {
                Objectory.recycle(sb);
            }
        } else {
            final String tmpSql = Strings.stripToEmpty(this.sql);
            parameterizedSql = tmpSql.endsWith(";") ? tmpSql.substring(0, tmpSql.length() - 1) : tmpSql;
            namedParameters = ImmutableList.empty();
        }
    }

    /**
     * Parses the given SQL string and returns a ParsedSql instance.
     * This method uses an internal cache to avoid re-parsing the same SQL statements.
     * The SQL is analyzed to extract named parameters and convert them to standard JDBC placeholders.
     *
     * <p>The parser automatically detects and converts different parameter styles:</p>
     * <ul>
     *   <li>Named parameters starting with ':' (e.g., :userId)</li>
     *   <li>iBatis/MyBatis style parameters enclosed in #{} (e.g., #{userName})</li>
     *   <li>Standard JDBC placeholders (?)</li>
     * </ul>
     *
     * <p>Note: Mixing different parameter styles in the same SQL statement will result in an IllegalArgumentException.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using named parameters
     * ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
     * System.out.println(ps1.parameterizedSql());   // "SELECT * FROM users WHERE id = ?"
     *
     * // Using iBatis/MyBatis style
     * ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
     * System.out.println(ps2.namedParameters());   // ["name", "email"]
     *
     * // Using standard JDBC placeholders
     * ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
     * System.out.println(ps3.parameterCount());   // 2
     * }</pre>
     *
     * @param sql the SQL string to parse (must not be null or empty)
     * @return a ParsedSql instance containing the parsed information
     * @throws IllegalArgumentException if sql is null, empty, or mixes different parameter styles
     */
    public static ParsedSql parse(final String sql) {
        N.checkArgNotEmpty(sql, "sql");

        ParsedSql result = null;
        PoolableWrapper<ParsedSql> w = pool.get(sql);

        if ((w == null) || (w.value() == null)) {
            synchronized (pool) {
                w = pool.get(sql);
                if ((w == null) || (w.value() == null)) {
                    result = new ParsedSql(sql);
                    pool.put(sql, Poolable.wrap(result, LIVE_TIME, MAX_IDLE_TIME));
                } else {
                    result = w.value();
                }
            }
        } else {
            result = w.value();
        }

        return result;
    }

    /**
     * Gets the SQL string (trimmed of leading and trailing whitespace).
     * This is the SQL before any parameter conversion or processing, but after trimming.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users WHERE id = :userId  ");
     * String sql = parsed.sql();   // Returns: "SELECT * FROM users WHERE id = :userId"
     * }</pre>
     *
     * @return the trimmed SQL string
     */
    public String sql() {
        return sql;
    }

    /**
     * Gets the parameterized SQL with all named parameters replaced by JDBC placeholders (?).
     * This SQL can be used directly with JDBC PreparedStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
     * String sql = parsed.parameterizedSql();
     * // Returns: "SELECT * FROM users WHERE id = ? AND status = ?"
     *
     * // Use with PreparedStatement
     * PreparedStatement stmt = connection.prepareStatement(parsed.parameterizedSql());
     * stmt.setLong(1, userId);
     * stmt.setString(2, status);
     * }</pre>
     *
     * @return the parameterized SQL string with ? placeholders
     */
    public String parameterizedSql() {
        return parameterizedSql;
    }

    /**
     * Gets the parameterized SQL formatted for the specified database system.
     * When isForCouchbase is true, JDBC placeholders (?) are converted to Couchbase positional parameters ($1, $2, etc.).
     * When false, returns standard JDBC SQL with ? placeholders.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
     *
     * // Standard JDBC format
     * String jdbcSql = parsed.parameterizedSql(false);
     * // Returns: "SELECT * FROM users WHERE id = ? AND name = ?"
     *
     * // Couchbase N1QL format
     * String couchbaseSql = parsed.parameterizedSql(true);
     * // Returns: "SELECT * FROM users WHERE id = $1 AND name = $2"
     * }</pre>
     *
     * @param isForCouchbase {@code true} to get Couchbase-formatted SQL with $n parameters, {@code false} for standard JDBC format with ? placeholders
     * @return the parameterized SQL string in the requested format
     */
    public String parameterizedSql(final boolean isForCouchbase) {
        if (isForCouchbase) {
            if (Strings.isEmpty(couchbaseParameterizedSql)) {
                synchronized (this) {
                    if (Strings.isEmpty(couchbaseParameterizedSql)) {
                        parseForCouchbase();
                    }
                }
            }

            return couchbaseParameterizedSql;
        } else {
            return parameterizedSql;
        }
    }

    /**
     * Gets the list of named parameters extracted from the SQL in order of appearance.
     * For SQL with no named parameters, returns an empty list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
     * ImmutableList<String> params = parsed.namedParameters();
     * // Returns: ["name", "minAge"]
     *
     * // SQL with no named parameters returns empty list
     * ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
     * ImmutableList<String> params2 = parsed2.namedParameters();
     * // Returns: []
     * }</pre>
     *
     * @return an immutable list of parameter names
     */
    public ImmutableList<String> namedParameters() {
        return namedParameters;
    }

    /**
     * Gets the list of named parameters formatted for the specified database system.
     * When isForCouchbase is true, returns parameter names suitable for Couchbase N1QL positional binding.
     * For SQL with positional parameters only (using ?), Couchbase format returns an empty list since
     * parameters are bound by position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
     *
     * // Standard format
     * ImmutableList<String> params = parsed.namedParameters(false);
     * // Returns: ["name", "minAge"]
     *
     * // Couchbase format
     * ImmutableList<String> cbParams = parsed.namedParameters(true);
     * // Returns: ["name", "minAge"]
     *
     * // Positional parameters return empty list for Couchbase
     * ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
     * ImmutableList<String> cbParams2 = parsed2.namedParameters(true);
     * // Returns: []
     * }</pre>
     *
     * @param isForCouchbase {@code true} to get Couchbase-formatted parameter names, {@code false} for standard format
     * @return an immutable list of parameter names
     */
    public ImmutableList<String> namedParameters(final boolean isForCouchbase) {
        if (isForCouchbase) {
            if (Strings.isEmpty(couchbaseParameterizedSql)) {
                synchronized (this) {
                    if (Strings.isEmpty(couchbaseParameterizedSql)) {
                        parseForCouchbase();
                    }
                }
            }

            return couchbaseNamedParameters;
        } else {
            return namedParameters;
        }
    }

    /**
     * Gets the total number of parameters (named or positional) in the SQL.
     * This count includes all occurrences of ?, :paramName, or #{paramName}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
     * int count = parsed.parameterCount();
     * // Returns: 3
     *
     * ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
     * int count2 = parsed2.parameterCount();
     * // Returns: 0
     * }</pre>
     *
     * @return the number of parameters in the SQL
     */
    public int parameterCount() {
        return parameterCount;
    }

    /**
     * Gets the parameter count formatted for the specified database system.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
     *
     * // Standard parameter count
     * int count = parsed.parameterCount(false);
     * // Returns: 2
     *
     * // Couchbase parameter count
     * int cbCount = parsed.parameterCount(true);
     * // Returns: 2
     * }</pre>
     *
     * @param isForCouchbase {@code true} to get Couchbase parameter count, {@code false} for standard count
     * @return the number of parameters
     */
    public int parameterCount(final boolean isForCouchbase) {
        if (isForCouchbase) {
            if (Strings.isEmpty(couchbaseParameterizedSql)) {
                synchronized (this) {
                    if (Strings.isEmpty(couchbaseParameterizedSql)) {
                        parseForCouchbase();
                    }
                }
            }

            return couchbaseParameterCount;
        } else {
            return parameterCount;
        }
    }

    private void parseForCouchbase() {
        final List<String> couchbaseNamedParameterList = new ArrayList<>();

        final List<String> words = SQLParser.parse(sql);

        boolean isOpSqlPrefix = false;
        for (final String word : words) {
            if (Strings.isNotEmpty(word) && !(word.equals(" ") || word.startsWith("--") || word.startsWith("/*"))) {
                isOpSqlPrefix = opSqlPrefixSet.contains(word.toUpperCase(Locale.ROOT));
                break;
            }
        }

        if (isOpSqlPrefix) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                int countOfParameter = 0;
                int type = 0;
                final int QUESTION_MARK_TYPE = 1;
                final int NAMED_PARAMETER_TYPE = 2;
                final int IBATIS_PARAMETER_TYPE = 4;

                for (String word : words) {
                    if (word.equals(SK.QUESTION_MARK)) {
                        type |= QUESTION_MARK_TYPE;
                        countOfParameter++;
                        word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                    } else if (word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) && word.endsWith(RIGHT_OF_IBATIS_NAMED_PARAMETER) && word.length() > 3) {
                        couchbaseNamedParameterList.add(word.substring(2, word.length() - 1));

                        type |= IBATIS_PARAMETER_TYPE;
                        countOfParameter++;
                        word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                    } else if (word.length() >= 2 && (word.charAt(0) == _PREFIX_OF_NAMED_PARAMETER || word.charAt(0) == _PREFIX_OF_COUCHBASE_NAMED_PARAMETER)
                            && isValidNamedParameterChar(word.charAt(1))) {
                        couchbaseNamedParameterList.add(word.substring(1));

                        type |= NAMED_PARAMETER_TYPE;
                        countOfParameter++;
                        word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                    }

                    if (Integer.bitCount(type) > 1) {
                        throw new IllegalArgumentException("Cannot mix parameter styles ('?', ':propName', '#{propName}') in the same SQL script");
                    }

                    sb.append(word);
                }

                boolean isNamedParametersByNum = true;

                for (int i = 0; i < countOfParameter && i < couchbaseNamedParameterList.size(); i++) {
                    try {
                        if (Numbers.toInt(couchbaseNamedParameterList.get(i)) != i + 1) {
                            isNamedParametersByNum = false;
                            break;
                        }
                    } catch (final Exception e) {
                        // ignore;
                        isNamedParametersByNum = false;
                        break;
                    }
                }

                if (isNamedParametersByNum && couchbaseNamedParameterList.size() == countOfParameter) {
                    couchbaseNamedParameterList.clear();
                }

                couchbaseNamedParameters = ImmutableList.wrap(couchbaseNamedParameterList);
                couchbaseParameterCount = countOfParameter;

                final String tmpCouchbaseSql = Strings.stripToEmpty(sb.toString());
                couchbaseParameterizedSql = tmpCouchbaseSql.endsWith(";") ? tmpCouchbaseSql.substring(0, tmpCouchbaseSql.length() - 1) : tmpCouchbaseSql;
            } finally {
                Objectory.recycle(sb);
            }
        } else {
            couchbaseNamedParameters = ImmutableList.empty();
            couchbaseParameterCount = 0;

            final String tmpCouchbaseSql = Strings.stripToEmpty(this.sql);
            couchbaseParameterizedSql = tmpCouchbaseSql.endsWith(";") ? tmpCouchbaseSql.substring(0, tmpCouchbaseSql.length() - 1) : tmpCouchbaseSql;
        }
    }

    private static boolean isValidNamedParameterChar(final char ch) {
        // https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
        return ch == '_' || !(ch < '0' || (ch > '9' && ch < 'A') || (ch > 'Z' && ch < 'a') || (ch > 'z' && ch < 128));
    }

    /**
     * Returns the hash code value for this ParsedSql.
     * The hash code is based on the original SQL string.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(sql);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two ParsedSql objects are equal if they have the same original SQL string.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object equals the obj argument; false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final ParsedSql other) {
            return N.equals(sql, other.sql);
        }

        return false;
    }

    /**
     * Returns a string representation of this ParsedSql.
     * The string contains both the original SQL and the parameterized SQL.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "{sql=" + sql + ", parameterizedSql=" + parameterizedSql + "}";
    }
}
