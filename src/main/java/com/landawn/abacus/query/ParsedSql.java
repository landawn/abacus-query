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
import com.landawn.abacus.pool.PoolableAdapter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a parsed SQL statement with support for named parameters and parameterized queries.
 * This class handles SQL parsing to extract named parameters (e.g., {@code :userId}, {@code #{userId}}) and converts
 * them to standard JDBC parameter placeholders ({@code ?}).
 *
 * <p>The class maintains an internal cache of parsed SQL statements for performance optimization.
 * Supported parameter formats include:</p>
 * <ul>
 *   <li>Named parameters: {@code :paramName}</li>
 *   <li>iBatis/MyBatis style: {@code #{paramName}}</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
 * String parameterized = parsed.parameterizedSql();   // "SELECT * FROM users WHERE id = ? AND status = ?"
 * List<String> params = parsed.namedParameters();   // ["userId", "status"]
 * }</pre>
 *
 * @see SqlParser
 * @see SqlBuilder
 */
public final class ParsedSql {

    private static final int EVICT_TIME = 60 * 1000;

    private static final int LIVE_TIME = 24 * 60 * 60 * 1000;

    private static final int MAX_IDLE_TIME = 24 * 60 * 60 * 1000;

    private static final Set<String> opSqlPrefixSet = Set.of(SK.SELECT, SK.INSERT, SK.UPDATE, SK.DELETE, SK.WITH, SK.MERGE, SK.CALL, SK.VALUES, "EXPLAIN",
            "REPLACE");

    private static final int factor = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    private static final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> pool = PoolFactory.createKeyedObjectPool(1000 * factor, EVICT_TIME);

    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    private static final String RIGHT_OF_IBATIS_NAMED_PARAMETER = "}";

    private final String sql;

    private final String parameterizedSql;

    private final ImmutableList<String> namedParameters;

    private int parameterCount;

    private ParsedSql(final String sql) {
        this.sql = sql.trim();

        final List<String> words = SqlParser.parse(this.sql);
        final String firstOpWord = resolveFirstOpWord(words);
        final boolean isOpSqlPrefix = Strings.isNotEmpty(firstOpWord) && opSqlPrefixSet.contains(firstOpWord.toUpperCase(Locale.ROOT));

        final List<String> namedParameterList = new ArrayList<>();
        int type = 0; // Use bit flags: 1=question mark, 2=named parameter, 4=iBatis parameter
        final int QUESTION_MARK_TYPE = 1;
        final int NAMED_PARAMETER_TYPE = 2;
        final int IBATIS_PARAMETER_TYPE = 4;

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0, size = words.size(); i < size; i++) {
                String word = words.get(i);

                if (isOpSqlPrefix) {
                    if (word.equals(SK.QUESTION_MARK)) {
                        parameterCount++;
                        type |= QUESTION_MARK_TYPE;
                    } else if (word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER)) {
                        final StringBuilder ibatisTokenBuilder = new StringBuilder(word);

                        while (ibatisTokenBuilder.indexOf(RIGHT_OF_IBATIS_NAMED_PARAMETER) < 0 && i < size - 1) {
                            ibatisTokenBuilder.append(words.get(++i));
                        }

                        final String ibatisToken = ibatisTokenBuilder.toString();
                        final int rightBracketIndex = ibatisToken.indexOf(RIGHT_OF_IBATIS_NAMED_PARAMETER);

                        if (rightBracketIndex < 0) {
                            throw new IllegalArgumentException(
                                    "Malformed iBatis/MyBatis parameter: missing closing '}' for token starting with '#{' in SQL: " + sql);
                        }

                        if (rightBracketIndex > 2) {
                            final String namedParameter = extractIbatisNamedParameter(ibatisToken.substring(2, rightBracketIndex));

                            if (Strings.isNotEmpty(namedParameter)) {
                                namedParameterList.add(namedParameter);
                                final String suffix = rightBracketIndex + 1 < ibatisToken.length() ? ibatisToken.substring(rightBracketIndex + 1)
                                        : Strings.EMPTY;

                                word = SK.QUESTION_MARK + suffix;
                                parameterCount++;
                                type |= IBATIS_PARAMETER_TYPE;
                            } else {
                                word = ibatisToken;
                            }
                        } else {
                            word = ibatisToken;
                        }
                    } else if (word.length() >= 2 && word.charAt(0) == _PREFIX_OF_NAMED_PARAMETER && isValidNamedParameterChar(word.charAt(1))) {
                        final int parameterEndIndex = findNamedParameterEndIndex(word, 1);
                        namedParameterList.add(word.substring(1, parameterEndIndex));

                        word = SK.QUESTION_MARK + word.substring(parameterEndIndex);
                        parameterCount++;
                        type |= NAMED_PARAMETER_TYPE;
                    }

                    if (Integer.bitCount(type) > 1) {
                        throw new IllegalArgumentException("Cannot mix parameter styles ('?', ':propName', '#{propName}') in the same SQL script");
                    }
                }

                sb.append(word);
            }

            final String tmpSql = Strings.stripToEmpty(isOpSqlPrefix ? sb.toString() : this.sql);
            parameterizedSql = tmpSql.endsWith(";") ? tmpSql.substring(0, tmpSql.length() - 1) : tmpSql;
            namedParameters = isOpSqlPrefix ? ImmutableList.wrap(namedParameterList) : ImmutableList.empty();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Parses the given SQL string and returns a {@code ParsedSql} instance.
     * This method uses an internal cache to avoid re-parsing the same SQL statements.
     * The SQL is analyzed to extract named parameters and convert them to standard JDBC placeholders.
     *
     * <p>The parser automatically detects and converts different parameter styles:</p>
     * <ul>
     *   <li>Named parameters starting with {@code ':'} (e.g., {@code :userId})</li>
     *   <li>iBatis/MyBatis style parameters enclosed in {@code #{}} (e.g., {@code #{userName}})</li>
     *   <li>Standard JDBC placeholders ({@code ?})</li>
     * </ul>
     *
     * <p>Note: Mixing different parameter styles in the same SQL statement will result in an {@code IllegalArgumentException}.</p>
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
     * @param sql the SQL string to parse (must not be {@code null} or empty)
     * @return a {@code ParsedSql} instance containing the parsed information
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or mixes different parameter styles
     */
    public static ParsedSql parse(final String sql) {
        N.checkArgNotEmpty(sql, "sql");

        ParsedSql result = null;
        PoolableAdapter<ParsedSql> w = pool.get(sql);

        if (w != null) {
            result = w.value();
        }

        if (result == null) {
            synchronized (pool) {
                w = pool.get(sql);
                if (w != null) {
                    result = w.value();
                }
                if (result == null) {
                    result = new ParsedSql(sql);
                    pool.put(sql, Poolable.wrap(result, LIVE_TIME, MAX_IDLE_TIME));
                }
            }
        }

        return result;
    }

    /**
     * Returns the original SQL string (trimmed of leading and trailing whitespace),
     * before any parameter conversion or processing.
     *
     * <p>Use {@link #parameterizedSql()} to obtain the SQL with named parameters
     * replaced by JDBC {@code ?} placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users WHERE id = :userId  ");
     * String sql = parsed.originalSql();   // Returns: "SELECT * FROM users WHERE id = :userId"
     * }</pre>
     *
     * @return the trimmed original SQL string
     */
    public String originalSql() {
        return sql;
    }

    /**
     * Gets the parameterized SQL with all named parameters replaced by JDBC placeholders ({@code ?}).
     * This SQL can be used directly with JDBC {@code PreparedStatement}.
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
     * @return the parameterized SQL string with {@code ?} placeholders
     */
    public String parameterizedSql() {
        return parameterizedSql;
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
     * Gets the total number of parameters (named or positional) in the SQL.
     * This count includes all occurrences of {@code ?}, {@code :paramName}, or {@code #{paramName}}.
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

    private static String resolveFirstOpWord(final List<String> words) {
        final int firstIndex = nextNonCommentWord(words, 0);

        if (firstIndex < 0) {
            return null;
        }

        String opWord = words.get(firstIndex);
        int nextIndex = firstIndex + 1;

        while (SK.PARENTHESIS_L.equals(opWord)) {
            final int nestedIndex = nextNonCommentWord(words, nextIndex);

            if (nestedIndex < 0) {
                return null;
            }

            opWord = words.get(nestedIndex);
            nextIndex = nestedIndex + 1;
        }

        if ("EXPLAIN".equalsIgnoreCase(opWord)) {
            int explainedIndex = nextNonCommentWord(words, nextIndex);

            while (explainedIndex >= 0) {
                final String explainedOpWord = words.get(explainedIndex);

                if (Strings.isNotEmpty(explainedOpWord) && opSqlPrefixSet.contains(explainedOpWord.toUpperCase(Locale.ROOT))
                        && !"EXPLAIN".equalsIgnoreCase(explainedOpWord)) {
                    return explainedOpWord;
                }

                explainedIndex = nextNonCommentWord(words, explainedIndex + 1);
            }
        }

        return opWord;
    }

    private static int findNamedParameterEndIndex(final String token, final int fromIndex) {
        int index = fromIndex;

        while (index < token.length() && isValidNamedParameterChar(token.charAt(index))) {
            index++;
        }

        return index;
    }

    private static int nextNonCommentWord(final List<String> words, final int fromIndex) {
        for (int i = fromIndex, size = words.size(); i < size; i++) {
            if (!isCommentOrSpaceToken(words.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isCommentOrSpaceToken(final String word) {
        return Strings.isEmpty(word) || word.equals(SK.SPACE) || word.startsWith("--") || word.startsWith("/*");
    }

    private static String extractIbatisNamedParameter(final String content) {
        final String trimmed = Strings.stripToEmpty(content);

        if (Strings.isEmpty(trimmed)) {
            return Strings.EMPTY;
        }

        final int commaIndex = trimmed.indexOf(SK._COMMA);
        return (commaIndex >= 0 ? trimmed.substring(0, commaIndex) : trimmed).trim();
    }

    private static boolean isValidNamedParameterChar(final char ch) {
        // https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
        return ch == '_' || ch == '.' || !(ch < '0' || (ch > '9' && ch < 'A') || (ch > 'Z' && ch < 'a') || (ch > 'z' && ch < 128));
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
