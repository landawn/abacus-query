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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.Poolable;
import com.landawn.abacus.pool.PoolableAdapter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a parsed SQL statement with support for named parameters and parameterized queries.
 * This class handles SQL parsing to extract named parameters (e.g., {@code :userId}, {@code #{userId}}) and converts
 * them to standard JDBC parameter placeholders ({@code ?}).
 *
 * <p>The class maintains an internal cache (a keyed object pool) of parsed SQL statements for
 * performance optimization, so repeated calls to {@link #parse(String)} with the same SQL string
 * typically return the same cached instance (subject to pool eviction after prolonged inactivity). Supported parameter formats include:</p>
 * <ul>
 *   <li>Named parameters: {@code :paramName} or a dotted property path such as
 *       {@code :user.address.city}</li>
 *   <li>iBatis/MyBatis style: {@code #{paramName}} (whitespace inside the braces is tolerated,
 *       e.g. {@code #{ paramName }}; the text from the first comma onward is treated as MyBatis
 *       attributes and discarded, so {@code #{ id, jdbcType=BIGINT }} binds {@code id})</li>
 *   <li>Standard JDBC placeholders: {@code ?}</li>
 * </ul>
 *
 * <p>A colon-style parameter name follows Unicode identifier grammar. Each dot-separated segment
 * starts with {@code _} or a Unicode identifier-start code point and continues with {@code _} or
 * Unicode identifier-part code points. Digits are therefore allowed after the first code point but
 * not at the start of a segment. Unicode whitespace and punctuation are delimiters rather than name
 * characters, and an unpaired UTF-16 surrogate in, or immediately before, a prospective parameter name
 * is rejected.</p>
 *
 * <p>Parameter detection and conversion is only performed when the SQL is recognized as a
 * data operation statement (one whose first non-comment / non-parenthesis token is
 * {@code SELECT}, {@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code WITH}, {@code MERGE},
 * {@code CALL}, {@code VALUES}, {@code EXPLAIN} or {@code REPLACE}). JDBC call escapes
 * (<code>{call ...}</code> and <code>{? = call ...}</code>) are recognized as {@code CALL}, including
 * when the tokenizer emits a glued <code>{call</code> opener token. For an {@code EXPLAIN}
 * statement, the first recognized keyword that follows is used to classify it (for example,
 * {@code EXPLAIN SELECT ...} is treated as a {@code SELECT}); if no such keyword follows,
 * {@code EXPLAIN} itself is used. For any other SQL, no parameter substitution is performed
 * and {@link #namedParameters()} is empty.</p>
 *
 * <p>Regardless of whether parameter substitution is applied, any trailing semicolons (and any
 * surrounding whitespace) are stripped from the parameterized SQL, so
 * {@code "SELECT * FROM x;"}, {@code "SELECT * FROM x;;"} and {@code "SELECT * FROM x ; ;"}
 * all produce {@code "SELECT * FROM x"}.</p>
 *
 * <p>Markers inside quoted literals, quoted identifiers, and SQL comments are not parameters; this
 * also holds for a literal element inside a subscript ({@code ARRAY[':x']}), while a marker next to
 * such a literal ({@code ARRAY['a', :id]}, {@code ARRAY['a', #{id}]}) is still a parameter. Where a
 * literal inside a subscript ends depends on the dialect once it contains a backslash: MySQL reads {@code \'}
 * as an escaped quote, standard-conforming strings read {@code 'a\'} as a complete literal. The subscript
 * scanners therefore evaluate the token under both readings and bind a marker only when the readings agree on
 * its position ({@code ARRAY['it''s :x', :id]} and {@code ARRAY['a\\', :id]} bind {@code id};
 * {@code ARRAY[E'a\\b', ?]} counts one placeholder). A token on which they disagree is left verbatim, by
 * design and fail-safe: {@code ARRAY['a\'', :id]}, {@code ARRAY['a\'', #{id}]} and {@code ARRAY['a\'', ?]}
 * bind and count nothing, so a leftover marker fails loudly at the driver instead of a literal being silently
 * rewritten. A PostgreSQL escape string is never ambiguous, since that syntax always processes backslash
 * escapes: {@code ARRAY[E'it\'s :literal', :id]} binds {@code id} and keeps the literal intact. Ordinary
 * bindings outside an ambiguous token are unaffected ({@code ARRAY['a\'', :x] AND id = :id} binds only
 * {@code id}). A chained
 * subscript, that is a bracket group that follows a subscript glued to an identifier
 * ({@code x['a', :b]['c', :d]}, {@code x[?]['c', ?]}, {@code x[#{a}]['c', #{b}]}), is inspected under the
 * same rules as that first group, so {@code :d}, the second {@code ?} and {@code #{b}} are all parameters.
 * Whitespace and comments between the groups do not break the chain, so {@code x[?] ['c', ?]} binds both
 * placeholders exactly as {@code x[?]['c', ?]} does. A bracket group that instead follows a
 * bracket-<i>quoted identifier</i> ({@code SELECT [a] [b:c]}, {@code SELECT t.[a] [b:c]} &mdash; a SQL Server
 * column with a bracketed alias) is not chained and follows the standalone rules
 * below. A leading or embedded PostgreSQL-style subscript whose first non-blank content is a {@code :name}
 * binding ({@code [:name]}, {@code [ :name ]}, {@code arr[:name]}) is the bracket-specific exception:
 * it is treated as a named binding rather than as a bracket-quoted identifier (a standalone
 * {@code [#{name}]} remains a bracket-quoted identifier). A bracket immediately after a qualification dot, such as {@code table.[:name]},
 * remains a quoted identifier. As a consequence, a bracket-quoted identifier whose first character
 * is {@code ':'} (for example the SQL Server column reference {@code SELECT [:identifier] FROM t})
 * is parameterized rather than preserved; qualify it ({@code t.[:identifier]}) to keep it literal.
 * Likewise a subscript whose content is a positional placeholder ({@code ARRAY[?]}, {@code arr[?, ?]},
 * or a standalone {@code [?]}) counts its {@code ?} markers as JDBC parameters, while a bracket-quoted
 * identifier that merely contains {@code ?} elsewhere ({@code [what?]}, {@code t.[what?]}) is preserved.
 * Such a preserved {@code ?} is still emitted verbatim by {@link #parameterizedSql()} but is not counted by
 * {@link #parameterCount()}, so a caller that binds JDBC parameters by counting {@code ?} characters in the
 * output would over-count; bind by {@code parameterCount()} and keep {@code ?} out of bracket-quoted identifiers
 * that a JDBC driver would otherwise read as placeholders.
 * Comments are normally removed by {@link SqlParser} when parameter conversion is applied; markers
 * inside block comments retained by its keep-comments directive are still ignored. Block and dash-line
 * comments embedded in a subscript token are preserved verbatim, and their markers are ignored as well. For an
 * unrecognized operation the token stream is not used to rebuild the SQL and no parameter
 * conversion is performed, so its comments remain in the normalized parameterized SQL.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
 * String parameterized = parsed.parameterizedSql();           // "SELECT * FROM users WHERE id = ? AND status = ?"
 * ImmutableList<String> params = parsed.namedParameters();    // ["userId", "status"]
 * }</pre>
 *
 * @see SqlParser
 * @see SqlBuilder
 */
public final class ParsedSql {

    private static final int EVICT_TIME = 60 * 1000;

    private static final int LIVE_TIME = 24 * 60 * 60 * 1000;

    private static final int MAX_IDLE_TIME = 24 * 60 * 60 * 1000;

    private static final Set<String> OP_SQL_PREFIX_SET = Set.of(SK.SELECT, SK.INSERT, SK.UPDATE, SK.DELETE, SK.WITH, SK.MERGE, SK.CALL, SK.VALUES, "EXPLAIN",
            "REPLACE");

    private static final int FACTOR = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    private static final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> pool = PoolFactory.createKeyedObjectPool(1000 * FACTOR, EVICT_TIME);

    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    private static final String RIGHT_OF_IBATIS_NAMED_PARAMETER = "}";

    private final String sql;

    private final String parameterizedSql;

    private final ImmutableList<String> namedParameters;

    private final int parameterCount;

    /**
     * Character offsets, in {@link #originalSql()}, of exactly the positional {@code '?'} markers counted by
     * {@link #parameterCount()} (ascending), or {@code null} if the token stream could not be aligned back onto
     * the original text. Empty when no positional marker was counted.
     */
    private final int[] positionalParameterOffsets;

    /** Cached hash code. This object is immutable, so {@code sql.hashCode()} is computed once. */
    private final int hashCode;

    private ParsedSql(final String sql) {
        this.sql = sql.trim();
        hashCode = this.sql.hashCode();

        final List<String> words = SqlParser.tokenize(this.sql);
        final String firstOpWord = resolveFirstOpWord(words);
        final boolean isOpSqlPrefix = Strings.isNotEmpty(firstOpWord) && isOpSqlPrefixWord(firstOpWord);

        final List<String> namedParameterList = new ArrayList<>();
        // Every counted positional marker as (token index, offset within that token), resolved to
        // original-text offsets after the scan so callers that rewrite the raw text (the query builders'
        // raw sub-query renaming) substitute exactly the markers counted here and nothing else.
        final IntList questionMarkTokenIndexes = new IntList();
        final IntList questionMarkTokenOffsets = new IntList();
        int paramCount = 0;
        int type = 0; // Use bit flags: 1=question mark, 2=named parameter, 4=iBatis parameter
        final int QUESTION_MARK_TYPE = 1;
        final int NAMED_PARAMETER_TYPE = 2;
        final int IBATIS_PARAMETER_TYPE = 4;

        final StringBuilder sb = Objectory.createStringBuilder();

        // A bracket group that continues a subscript rooted in an identifier ("x[?][?]",
        // "x['a', :b]['c', :d]", "x[?] ['c', ?]") is a chained subscript: the tokenizer emits it as a
        // standalone "[...]" token, but it is inspected exactly like that first group. Computed up front in
        // one pass so the per-token lookup below stays O(1).
        final boolean[] chainedSubscripts = isOpSqlPrefix ? markChainedSubscriptTokens(words) : null;

        try {
            for (int i = 0, size = words.size(); i < size; i++) {
                String word = words.get(i);

                if (isOpSqlPrefix) {
                    final boolean chainedSubscript = chainedSubscripts[i];

                    if (word.indexOf('?') >= 0 && (chainedSubscript || isPositionalSubscriptToken(word))) {
                        // Positional placeholders embedded in a subscript-shaped token ("ARRAY[?]", "arr[?, ?]",
                        // standalone "[?]"): the tokenizer keeps the bracket region glued to the preceding
                        // identifier, so the "?" never surfaces as its own token. Kept independent of the
                        // marker chain below so a token that also carries a "#{...}" or ":name" marker still
                        // reaches the mixed-style guard.
                        final int[] embedded = findUnquotedQuestionMarkIndexes(word, word.indexOf('[') + 1);

                        if (embedded.length > 0) {
                            for (final int offsetInToken : embedded) {
                                questionMarkTokenIndexes.add(i);
                                questionMarkTokenOffsets.add(offsetInToken);
                            }

                            paramCount += embedded.length;
                            type |= QUESTION_MARK_TYPE;
                        }
                    }

                    if (word.equals(SK.QUESTION_MARK)) {
                        if (!isPostgreSqlJsonQuestionOperator(words, i)) {
                            questionMarkTokenIndexes.add(i);
                            questionMarkTokenOffsets.add(0);
                            paramCount++;
                            type |= QUESTION_MARK_TYPE;
                        }
                    } else if (mayContainIbatisParameter(word, chainedSubscript)) {
                        // A token may contain multiple iBatis markers and literal text between them
                        // (for example "#{a}x#{b}"). Scan the complete unquoted token instead of only
                        // consuming markers at its beginning, so no embedded binding is left as SQL text.
                        // The marker positions are collected once for the whole token: the quoted-region check
                        // behind them scans it under both escape readings, so asking for the next marker one at
                        // a time made a token that holds many of them ("ARRAY[#{a},#{b},...]") take quadratic
                        // time. Only a marker whose '}' lives in a following token ("#{ name }") replaces the
                        // text being scanned, and the positions are then recollected for what remains.
                        int[] markerIndexes = findUnquotedIbatisMarkerIndexes(word);
                        final StringBuilder rebuilt = new StringBuilder(word.length() + 4);
                        int copiedFrom = 0;
                        int markerCursor = 0;

                        while (markerCursor < markerIndexes.length) {
                            int markerStartIndex = markerIndexes[markerCursor++];

                            if (markerStartIndex < copiedFrom) {
                                continue; // part of a marker that was already consumed
                            }

                            rebuilt.append(word, copiedFrom, markerStartIndex);

                            int closingIndex = word.indexOf(RIGHT_OF_IBATIS_NAMED_PARAMETER, markerStartIndex);

                            if (closingIndex < 0) {
                                // The '}' is in a following token: join tokens until it appears, and continue
                                // in that joined text, whose marker now starts at 0.
                                final StringBuilder ibatisTokenBuilder = new StringBuilder();
                                ibatisTokenBuilder.append(word, markerStartIndex, word.length());

                                while (ibatisTokenBuilder.indexOf(RIGHT_OF_IBATIS_NAMED_PARAMETER) < 0 && i < size - 1) {
                                    ibatisTokenBuilder.append(words.get(++i));
                                }

                                word = ibatisTokenBuilder.toString();
                                closingIndex = word.indexOf(RIGHT_OF_IBATIS_NAMED_PARAMETER);

                                if (closingIndex < 0) {
                                    throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: missing closing '}' in: " + this.sql);
                                }

                                markerStartIndex = 0;
                                markerIndexes = findUnquotedIbatisMarkerIndexes(word);
                                markerCursor = 0; // the marker being handled is skipped by the copiedFrom guard
                            }

                            // Content between "#{" and "}"; empty for the literal "#{}".
                            final String namedParameter = closingIndex > markerStartIndex + 2
                                    ? extractIbatisNamedParameter(word.substring(markerStartIndex + 2, closingIndex))
                                    : null;

                            if (Strings.isNotEmpty(namedParameter)) {
                                namedParameterList.add(namedParameter);
                                rebuilt.append(SK.QUESTION_MARK);
                                paramCount++;
                                type |= IBATIS_PARAMETER_TYPE;
                            } else {
                                // Empty or blank "#{...}" content — keep verbatim, no parameter, and keep
                                // scanning so a subsequent marker in the same token ("#{}#{a}", "#{ }#{a}")
                                // is still extracted instead of being lost.
                                rebuilt.append(word, markerStartIndex, closingIndex + 1);
                            }

                            copiedFrom = closingIndex + 1;
                        }

                        rebuilt.append(word, copiedFrom, word.length());
                        word = rebuilt.toString();
                    }

                    // The tokenizer can keep adjacent markers in one token (for example
                    // ":id#{name}" or "#{name}:id"). Scan the rebuilt token even when it
                    // originally contained an iBatis marker so the mixed-style guard below
                    // sees both styles instead of silently leaving the :named marker in SQL.
                    if (mayContainNamedParameter(word, chainedSubscript)) {
                        // A single tokenized word may contain one or more ':named' markers because
                        // ':' is not a token separator. Extract markers at safe boundaries so
                        // constructs such as ":a:b" and "array[:ids]" are parameterized, while
                        // PostgreSQL casts such as "::int" and quoted literal tokens are preserved.
                        // The candidate positions are collected once for the whole token: the quoted-region
                        // check behind them scans the token under both escape readings, so asking for the
                        // next marker one at a time made a token that holds many of them
                        // ("ARRAY[:id0,:id1,...]") take quadratic time.
                        final int[] markerIndexes = findUnquotedNamedParameterMarkerIndexes(word);

                        if (markerIndexes.length > 0) {
                            final StringBuilder rebuilt = new StringBuilder(word.length() + 4);
                            int copiedFrom = 0;
                            int searchFrom = 0;

                            for (final int parameterStartIndex : markerIndexes) {
                                // A ':' the previous parameter's name swallowed is not a marker of its own,
                                // and a boundary check rejects casts and qualified names.
                                if (parameterStartIndex < searchFrom || !isNamedParameterStart(word, parameterStartIndex, searchFrom)) {
                                    continue;
                                }

                                rebuilt.append(word, copiedFrom, parameterStartIndex);

                                final int parameterEndIndex = findNamedParameterEndIndex(word, parameterStartIndex + 1);
                                namedParameterList.add(word.substring(parameterStartIndex + 1, parameterEndIndex));
                                rebuilt.append(SK.QUESTION_MARK);
                                paramCount++;
                                type |= NAMED_PARAMETER_TYPE;

                                copiedFrom = parameterEndIndex;
                                searchFrom = parameterEndIndex;
                            }

                            rebuilt.append(word, copiedFrom, word.length());
                            word = rebuilt.toString();
                        }
                    }

                    if (Integer.bitCount(type) > 1) {
                        throw new IllegalArgumentException("Cannot mix parameter styles ('?', ':propName', '#{propName}') in the same SQL script");
                    }
                }

                sb.append(word);
            }

            final String tmpSql = Strings.stripToEmpty(isOpSqlPrefix ? sb.toString() : this.sql);
            // Strip ALL trailing semicolons (and any whitespace between them) so SQL like
            // "SELECT * FROM x;;" or "SELECT * FROM x ; ;" both produce a clean parameterized form.
            int endIdx = tmpSql.length();
            while (endIdx > 0) {
                final char ch = tmpSql.charAt(endIdx - 1);
                if (ch == ';' || Character.isWhitespace(ch)) {
                    endIdx--;
                } else {
                    break;
                }
            }
            parameterizedSql = endIdx == tmpSql.length() ? tmpSql : tmpSql.substring(0, endIdx);
            parameterCount = paramCount;
            namedParameters = isOpSqlPrefix ? ImmutableList.wrap(namedParameterList) : ImmutableList.empty();
            positionalParameterOffsets = questionMarkTokenIndexes.isEmpty() ? N.EMPTY_INT_ARRAY
                    : resolvePositionalParameterOffsets(this.sql, words, questionMarkTokenIndexes, questionMarkTokenOffsets);
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
     *   <li>iBatis/MyBatis style parameters enclosed in {@code #{}} (e.g., {@code #{userName}};
     *       whitespace inside the braces is tolerated, e.g. {@code #{ userName }}; the text from the
     *       first comma onward is treated as MyBatis attributes and discarded, so
     *       {@code #{ id, jdbcType=BIGINT }} binds {@code id})</li>
     *   <li>Standard JDBC placeholders ({@code ?})</li>
     * </ul>
     *
     * <p>Note: Mixing different parameter styles in the same SQL statement will result in an {@code IllegalArgumentException}.</p>
     *
     * <p>Parameter conversion is only applied when the SQL is a recognized data operation statement
     * (see the class-level documentation). All trailing semicolons and surrounding whitespace are
     * stripped from the resulting {@link #parameterizedSql()}.</p>
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
     * @param sql the SQL string to parse (must not be {@code null}, empty, or blank)
     * @return a {@code ParsedSql} instance for the given SQL (typically a cached instance)
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank, if it mixes different
     *         parameter styles ({@code ?}, {@code :propName}, {@code #{propName}}), or if it contains
     *         a malformed iBatis/MyBatis parameter that is missing its closing brace, or an unpaired
     *         UTF-16 surrogate in, or immediately before, a prospective colon-style parameter name
     */
    public static ParsedSql parse(final String sql) {
        if (Strings.isBlank(sql)) {
            throw new IllegalArgumentException("sql must not be null, empty, or blank");
        }

        final String normalizedSql = sql.trim();
        PoolableAdapter<ParsedSql> w = pool.get(normalizedSql);
        ParsedSql result = w == null ? null : w.value();

        if (result != null) {
            return result;
        }

        // Tokenization is expensive, so construct outside the lock to avoid serializing concurrent
        // first-touch parses of unrelated SQL strings. ParsedSql is immutable and value-equal, so a
        // racing thread may build a duplicate; the pooled winner is returned in that case and the
        // loser's instance is simply discarded.
        final ParsedSql parsed = new ParsedSql(normalizedSql);

        synchronized (pool) {
            w = pool.get(normalizedSql);
            result = w == null ? null : w.value();

            if (result != null) {
                return result;
            }

            pool.put(normalizedSql, Poolable.wrap(parsed, LIVE_TIME, MAX_IDLE_TIME));
        }

        return parsed;
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
     * Returns the parameterized SQL with named parameters replaced by JDBC placeholders ({@code ?})
     * for recognized data-operation statements. For any other SQL, no parameter conversion is
     * performed; the statement is still normalized by trimming surrounding whitespace and removing
     * trailing semicolons.
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
     * Returns the list of named parameters extracted from the SQL in order of appearance.
     * The list is empty if the SQL has no named parameters, or if the SQL is not a
     * recognized data operation statement (see the class-level documentation), in which
     * case no parameter extraction is performed.
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
     * Returns the total number of parameters (named or positional) in the SQL.
     * This count includes parameter occurrences of {@code ?}, {@code :paramName}, or {@code #{paramName}},
     * but excludes {@code ?} tokens recognized as PostgreSQL JSON-existence operators. The
     * operator's right operand may be a literal, placeholder, column, or function expression;
     * SQL ordering/pagination placeholders remain ordinary JDBC parameters, and so does a {@code ?}
     * that directly follows a SQL operator or a value-taking keyword such as {@code INTERVAL},
     * {@code ILIKE}, {@code SIMILAR TO} or {@code ESCAPE} (for example {@code INTERVAL ? DAY}).
     * Parameters are only counted for recognized data operation statements (see the class-level
     * documentation); for other SQL this returns {@code 0}.
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
     * @return the number of parameters in the SQL; this can be smaller than the number of {@code ?} characters in
     *         {@link #parameterizedSql()} when a bracket-quoted identifier such as {@code [what?]} is preserved verbatim
     */
    public int parameterCount() {
        return parameterCount;
    }

    /**
     * Returns the character offsets, in {@link #originalSql()}, of exactly the positional {@code '?'} markers that
     * {@link #parameterCount()} counted as JDBC parameters, in ascending order. A {@code '?'} inside a quoted
     * literal, a quoted or bracket-quoted identifier ({@code [what?]}) or a comment, and a PostgreSQL JSON
     * {@code ?} operator, is never listed; a {@code '?'} inside an array subscript ({@code arr[?]}) is. This is
     * the single classification the query builders use to rewrite the placeholders of a raw sub-query, so the
     * text they substitute is, by construction, the text this parser bound.
     *
     * @return a fresh array of offsets (empty if the SQL carries no positional marker)
     * @throws IllegalStateException if the tokenized form could not be aligned back onto the original text
     */
    int[] positionalParameterOffsets() {
        if (positionalParameterOffsets == null) {
            throw new IllegalStateException("Cannot locate the positional '?' placeholders in the original text of: " + sql);
        }

        return positionalParameterOffsets.clone();
    }

    /**
     * Maps each counted positional marker, recorded as (token index, offset within the token), to its offset in
     * {@code sql}. The tokenizer drops comments and collapses whitespace runs, so the token stream is walked
     * alongside the original text: whitespace and comments are skipped, then each non-blank token must be found
     * verbatim at the cursor. Returns {@code null} if a token cannot be located (never expected, guarded anyway).
     *
     * <p>A token is accepted at the cursor only when the cursor does not open a comment the tokenizer discarded,
     * because the openers share their first character with ordinary operator tokens: without that guard the token
     * {@code "/"} matches the {@code '/'} of a following {@code "/*"} and {@code "-"} matches the {@code '-'} of
     * {@code "--"}, the comment is then never skipped, and the marker offsets land inside it ({@code "SELECT 1 /*
     * ? *}{@code / / ?"} would report the commented-out {@code '?'}). A comment that the tokenizer kept as a token
     * (the "Keep comments" marker) starts with an opener itself, so it is still matched rather than skipped.</p>
     */
    private static int[] resolvePositionalParameterOffsets(final String sql, final List<String> words, final IntList questionMarkTokenIndexes,
            final IntList questionMarkTokenOffsets) {
        final int markerCount = questionMarkTokenIndexes.size();
        final int[] offsets = new int[markerCount];
        final int len = sql.length();
        int cursor = 0;
        int marker = 0;

        for (int i = 0, size = words.size(); i < size && marker < markerCount; i++) {
            final String word = words.get(i);

            if (Strings.isBlank(word)) {
                continue;
            }

            while (true) {
                while (cursor < len && Character.isWhitespace(sql.charAt(cursor))) {
                    cursor++;
                }

                // A token may only be matched at the cursor when the cursor is not the start of a comment the
                // tokenizer discarded: "/" and "-" are prefixes of the "/*" and "--" openers, so matching them
                // first would skip the comment skipping below and resolve the markers inside the comment. A
                // comment kept as a token starts with an opener itself, so it still matches here.
                if (sql.startsWith(word, cursor) && (!startsWithCommentOpener(sql, cursor) || startsWithCommentOpener(word, 0))) {
                    break;
                }

                // Comment text the tokenizer discarded (block comments are kept as tokens only under the
                // "Keep comments" marker, in which case they matched above).
                if (sql.startsWith("--", cursor) || sql.startsWith("#", cursor)) {
                    while (cursor < len && sql.charAt(cursor) != '\n' && sql.charAt(cursor) != '\r') {
                        cursor++;
                    }
                } else if (sql.startsWith("/*", cursor)) {
                    final int end = sql.indexOf("*/", cursor + 2);
                    cursor = end < 0 ? len : end + 2;
                } else {
                    final int found = sql.indexOf(word, cursor);

                    if (found < 0) {
                        return null; // NOSONAR - documented sentinel, checked by positionalParameterOffsets()
                    }

                    cursor = found;
                    break;
                }
            }

            while (marker < markerCount && questionMarkTokenIndexes.get(marker) == i) {
                offsets[marker] = cursor + questionMarkTokenOffsets.get(marker);
                marker++;
            }

            cursor += word.length();
        }

        return marker == markerCount ? offsets : null;
    }

    /**
     * Returns {@code true} if a comment opener the tokenizer recognizes ({@code "--"}, {@code "#"} or
     * {@code "/*"}) starts at {@code index} of {@code text}. Exactly the openers
     * {@link #resolvePositionalParameterOffsets(String, List, IntList, IntList)} skips, so its token match and
     * its comment skipping cannot disagree about where a comment begins.
     */
    private static boolean startsWithCommentOpener(final String text, final int index) {
        return text.startsWith("--", index) || text.startsWith("#", index) || text.startsWith("/*", index);
    }

    /**
     * Equivalent to {@code OP_SQL_PREFIX_SET.contains(word.toUpperCase(Locale.ROOT))} but without
     * allocating a temporary upper-cased String. All entries of {@code OP_SQL_PREFIX_SET} are
     * uppercase ASCII keywords, so a case-insensitive scan yields the identical result.
     */
    private static boolean isOpSqlPrefixWord(final String word) {
        for (final String prefix : OP_SQL_PREFIX_SET) {
            if (prefix.equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
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

        // JDBC escape forms: "{call ...}" / "{? = call ...}". The tokenizer may emit a single
        // "{call" token when there is no whitespace after '{'; otherwise '{' is its own token.
        final String jdbcCallOp = resolveJdbcCallOpWord(opWord, words, nextIndex);

        if (jdbcCallOp != null) {
            return jdbcCallOp;
        }

        if ("EXPLAIN".equalsIgnoreCase(opWord)) {
            int explainedIndex = nextNonCommentWord(words, nextIndex);

            while (explainedIndex >= 0) {
                final String explainedOpWord = words.get(explainedIndex);

                if (Strings.isNotEmpty(explainedOpWord) && isOpSqlPrefixWord(explainedOpWord) && !"EXPLAIN".equalsIgnoreCase(explainedOpWord)) {
                    return explainedOpWord;
                }

                explainedIndex = nextNonCommentWord(words, explainedIndex + 1);
            }
        }

        return opWord;
    }

    /**
     * Returns {@code "CALL"} when {@code opWord} (at the current statement head) introduces a JDBC
     * call escape, otherwise {@code null}. Recognizes a glued <code>{call</code> token and the
     * multi-token forms <code>{ call ...}</code> and <code>{? = call ...}</code>.
     */
    private static String resolveJdbcCallOpWord(final String opWord, final List<String> words, final int nextIndex) {
        if (Strings.isEmpty(opWord)) {
            return null;
        }

        // Glued form: tokenizer emits "{call" / "{CALL" as one word.
        if (opWord.length() > 1 && opWord.charAt(0) == '{' && "CALL".equalsIgnoreCase(opWord.substring(1))) {
            return "CALL";
        }

        if (!"{".equals(opWord)) {
            return null;
        }

        int idx = nextNonCommentWord(words, nextIndex);

        if (idx < 0) {
            return null;
        }

        String next = words.get(idx);

        // Optional return-parameter form: {? = call ...}
        if (SK.QUESTION_MARK.equals(next)) {
            idx = nextNonCommentWord(words, idx + 1);

            if (idx < 0 || !"=".equals(words.get(idx))) {
                return null;
            }

            idx = nextNonCommentWord(words, idx + 1);

            if (idx < 0) {
                return null;
            }

            next = words.get(idx);
        }

        return "CALL".equalsIgnoreCase(next) ? "CALL" : null;
    }

    private static int findNamedParameterEndIndex(final String token, final int fromIndex) {
        int index = fromIndex;

        if (index >= token.length()) {
            return index;
        }

        int codePoint = namedParameterCodePointAt(token, index);

        if (!isNamedParameterIdentifierStart(codePoint)) {
            return index;
        }

        index += Character.charCount(codePoint);

        while (index < token.length()) {
            codePoint = namedParameterCodePointAt(token, index);

            if (codePoint == '.') {
                final int nextSegmentIndex = index + 1;

                if (nextSegmentIndex >= token.length()) {
                    break;
                }

                final int nextCodePoint = namedParameterCodePointAt(token, nextSegmentIndex);

                if (!isNamedParameterIdentifierStart(nextCodePoint)) {
                    break;
                }

                index = nextSegmentIndex + Character.charCount(nextCodePoint);
            } else if (isNamedParameterIdentifierPart(codePoint)) {
                index += Character.charCount(codePoint);
            } else {
                break;
            }
        }

        return index;
    }

    /**
     * Returns the original-text offsets of bracket openers recognized as subscripts by the default tokenizer.
     *
     * @param sql the SQL text to inspect
     * @return sorted bracket-opening offsets
     * @see #subscriptOpeningOffsets(String, SqlParser.Tokenizer)
     */
    static int[] subscriptOpeningOffsets(final String sql) {
        return subscriptOpeningOffsets(sql, SqlParser.tokenizer());
    }

    /**
     * Locates subscript brackets for builder placeholder rewriting using the same distinction between
     * subscripts and quoted identifiers as parameter parsing. Identifier-rooted and chained subscripts,
     * and standalone groups starting with a colon binding or positional marker, are recognized.
     * Every unquoted nested opener is included; comments and literal content are excluded. A token with
     * ambiguous backslash/doubled-quote readings is left opaque.
     *
     * <p>Standalone {@code [#{name}]} or custom-token groups remain bracket-quoted identifiers. Once a
     * standalone {@code [?]} has been rendered into that shape, its original subscript role cannot be
     * recovered from the resulting text alone.</p>
     *
     * @param sql the original SQL text to inspect
     * @param tokenizer the tokenizer configured for that SQL
     * @return sorted original-text offsets, or an empty array when no subscript is recognized
     * @throws IllegalStateException if the token stream cannot be aligned with the original SQL text
     */
    static int[] subscriptOpeningOffsets(final String sql, final SqlParser.Tokenizer tokenizer) {
        if (sql.indexOf('[') < 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final List<String> words = tokenizer.tokenize(sql);
        final boolean[] chainedSubscripts = markChainedSubscriptTokens(words);
        final IntList tokenIndexes = new IntList();
        final IntList tokenOffsets = new IntList();

        for (int i = 0; i < words.size(); i++) {
            final String word = words.get(i);

            if (word.indexOf('[') < 0 || isCommentOrSpaceToken(word) || !(chainedSubscripts[i] || !isQuotedToken(word) || isPositionalSubscriptToken(word))) {
                continue;
            }

            for (final int opening : unambiguousSubscriptOpenings(word)) {
                tokenIndexes.add(i);
                tokenOffsets.add(opening);
            }
        }

        if (tokenIndexes.isEmpty()) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] offsets = resolvePositionalParameterOffsets(sql, words, tokenIndexes, tokenOffsets);

        if (offsets == null) {
            throw new IllegalStateException("Cannot locate subscript brackets in the original SQL text: " + sql);
        }

        return offsets;
    }

    /** Collects bracket openers only when both supported quote readings agree throughout the token. */
    private static int[] unambiguousSubscriptOpenings(final String token) {
        final IntList openings = new IntList();

        for (int index = 0, len = token.length(); index < len; index++) {
            final char ch = token.charAt(index);

            if (isQuoteChar(ch)) {
                final int end = skipQuotedRegion(token, index, true);

                if (end != skipQuotedRegion(token, index, false)) {
                    return N.EMPTY_INT_ARRAY;
                }

                index = end;
            } else if (ch == '/' && index + 1 < len && token.charAt(index + 1) == '*') {
                final int end = token.indexOf("*/", index + 2);
                index = end < 0 ? len : end + 1;
            } else if (ch == '-' && index + 1 < len && token.charAt(index + 1) == '-') {
                while (index + 1 < len && token.charAt(index + 1) != '\r' && token.charAt(index + 1) != '\n') {
                    index++;
                }
            } else if (ch == '[') {
                openings.add(index);
            }
        }

        return openings.toArray();
    }

    /**
     * Flags every token that continues a subscript chain: a bracket group whose chain of preceding bracket
     * groups is rooted in a subscript glued to an identifier, as in {@code "x[?][?]"},
     * {@code "x['a', :b]['c', :d]"} or {@code "x[?] ['c', ?]"}, where the tokenizer emits every group after
     * the first as its own token. Such a group is inspected under the same rules as the group attached to
     * the identifier, so a marker inside it is a parameter.
     *
     * <p>Whitespace and comments between the groups do not break the chain: {@code "x[?] ['c', ?]"} is the
     * same PostgreSQL expression as {@code "x[?]['c', ?]"}, and leaving its second {@code '?'} uncounted
     * would emit a placeholder that {@link #parameterCount()} does not report. The chain root must be a
     * real subscript, so a bracket group that follows a bracket-<i>quoted identifier</i>
     * ({@code "SELECT [a] [b:c]"}, {@code "SELECT t.[a] [b:c]"} &mdash; a SQL Server column with a bracketed
     * alias) is not chained and keeps the standalone bracket-quoted-identifier reading.</p>
     *
     * @param words the tokenized SQL
     * @return one flag per token, {@code true} where the token continues a subscript chain
     */
    private static boolean[] markChainedSubscriptTokens(final List<String> words) {
        final int size = words.size();
        final boolean[] chained = new boolean[size];
        boolean chainIsOpen = false; // the last meaningful token ended a subscript chain

        // One forward pass: walking backwards per token would be quadratic on a long run of bracket groups.
        for (int i = 0; i < size; i++) {
            final String token = words.get(i);

            if (isCommentOrSpaceToken(token)) {
                continue; // whitespace and comments between the groups do not break the chain
            }

            if (chainIsOpen && isCompleteBracketGroupToken(token)) {
                chained[i] = true; // continues the chain, and leaves it open for the next group
            } else {
                chainIsOpen = isIdentifierGluedSubscriptToken(token);
            }
        }

        return chained;
    }

    /** Returns {@code true} for a standalone, closed bracket group token such as {@code "[?]"} or {@code "['c', :d]"}. */
    private static boolean isCompleteBracketGroupToken(final String token) {
        return token.length() > 1 && token.charAt(0) == '[' && token.charAt(token.length() - 1) == ']';
    }

    /**
     * Returns {@code true} if {@code token} ends with a subscript glued to an identifier ({@code "x[?]"},
     * {@code "arr[:ids]"}), which is what opens a subscript chain. A bracket that follows a qualification
     * dot ({@code "t.[a]"}) or sits inside a quoted region ({@code "N'a[?]'"}) is a quoted identifier, and a
     * token that starts with {@code '['} is a standalone group rather than a chain root.
     */
    private static boolean isIdentifierGluedSubscriptToken(final String token) {
        final int bracketIndex = token.indexOf('[');

        return bracketIndex > 0 && token.charAt(bracketIndex - 1) != '.' && token.charAt(token.length() - 1) == ']'
                && !precededByQuote(token, bracketIndex, '\'') && !precededByQuote(token, bracketIndex, '"') && !precededByQuote(token, bracketIndex, '`');
    }

    private static boolean mayContainNamedParameter(final String token, final boolean chainedSubscript) {
        return token.length() >= 2 && token.indexOf(_PREFIX_OF_NAMED_PARAMETER) >= 0 && (chainedSubscript || !isQuotedToken(token))
                && !isCommentOrSpaceToken(token);
    }

    private static boolean mayContainIbatisParameter(final String token, final boolean chainedSubscript) {
        // The minimum length of 2 admits the standalone "#{" token the tokenizer emits when
        // whitespace immediately follows the opener (e.g. "#{ id }"); the marker-assembly loop
        // in the constructor then joins subsequent tokens until the closing '}' is found. Any
        // 2-char token containing LEFT_OF_IBATIS_NAMED_PARAMETER is exactly "#{".
        // A chained subscript ("x[#{a}]['c', #{b}]") bypasses the bracket-quoted-identifier check:
        // the marker scanner skips quoted regions itself, so literal elements stay literal.
        return token.length() >= 2 && token.indexOf(LEFT_OF_IBATIS_NAMED_PARAMETER) >= 0 && (chainedSubscript || !isQuotedToken(token))
                && !isCommentOrSpaceToken(token);
    }

    private static boolean isQuotedToken(final String token) {
        final int bracketIndex = token.indexOf('[');

        if (bracketIndex < 0) {
            return token.indexOf('\'') >= 0 || token.indexOf('"') >= 0 || token.indexOf('`') >= 0;
        }

        // A quote before the bracket means the bracket sits inside a prefixed literal ("N'a[:x]'") or a
        // quoted identifier, so the whole token is opaque. A quote after the bracket belongs to a literal
        // element inside a subscript ("ARRAY['a', :id]"); the marker scanners skip quoted regions
        // themselves (like findUnquotedQuestionMarkIndexes does for '?'), so such a token still reaches them.
        if (precededByQuote(token, bracketIndex, '\'') || precededByQuote(token, bracketIndex, '"') || precededByQuote(token, bracketIndex, '`')) {
            return true;
        }

        if (bracketIndex > 0) {
            // '[' immediately after a qualification dot is SQL Server qualified quoting ("db.[col]"): a
            // subscript can never directly follow '.' in SQL. Any other glued bracket region is a
            // subscript such as "array[:ids]", which deliberately supports named bindings inside the brackets.
            return token.charAt(bracketIndex - 1) == '.';
        }

        // A token that starts with '[' is a SQL Server bracket-quoted identifier, with one exception: a
        // leading "[:name" (optionally after whitespace) is a PostgreSQL-style array subscript holding a
        // named binding. Whitespace before the bracket makes the tokenizer emit the subscript as its own
        // token (e.g. "array [:ids]" yields the standalone token "[:ids]"), and without the exception that
        // binding would silently pass through as literal SQL. (A group continuing a subscript chain is
        // recognized by the caller via isChainedSubscriptToken and never consults this rule.)
        return !isNamedParameterSubscript(token, bracketIndex);
    }

    /**
     * Returns the indexes of every {@code "#{"} in {@code token} that lies outside single-, double- or
     * backtick-quoted regions, in ascending order, so a marker inside a literal element of a subscript
     * ({@code "ARRAY['#{x}']"}) is not mistaken for a binding. A token that holds none, and one whose quoting
     * is ambiguous between the two escape readings (see
     * {@link #findUnambiguousUnquotedMarkerIndexes(String, int, char)}), yields an empty array: it is verbatim.
     *
     * <p>Collected once for the whole token, like
     * {@link #findUnquotedNamedParameterMarkerIndexes(String)}: every reported index lies outside every quoted
     * region under both readings, so the text following one of them reads identically to a fresh scan that
     * started there.</p>
     */
    private static int[] findUnquotedIbatisMarkerIndexes(final String token) {
        final int[] hashIndexes = findUnambiguousUnquotedMarkerIndexes(token, 0, '#');

        if (N.isEmpty(hashIndexes)) {
            return N.EMPTY_INT_ARRAY;
        }

        IntList markerIndexes = null;

        for (final int index : hashIndexes) {
            if (index + 1 < token.length() && token.charAt(index + 1) == '{') {
                if (markerIndexes == null) {
                    markerIndexes = new IntList();
                }

                markerIndexes.add(index);
            }
        }

        return markerIndexes == null ? N.EMPTY_INT_ARRAY : markerIndexes.toArray();
    }

    private static boolean isQuoteChar(final char ch) {
        return ch == '\'' || ch == '"' || ch == '`';
    }

    /**
     * The one quoted-region rule shared by every marker scanner that inspects the content of a subscript
     * token ({@code "ARRAY['it''s :x', :id]"}). Returns the index of the closing quote of the region opened
     * at {@code openIndex}, or {@code token.length()} if the region is never closed (the rest of the token
     * is then quoted). A doubled quote is always an escaped quote. Inside a single-quoted literal a backslash
     * additionally escapes the following character when {@code backslashEscapes} is {@code true} (MySQL /
     * PostgreSQL {@code E''} semantics: {@code 'it\'s'} and {@code 'a\\'} are single literals) and is an
     * ordinary character otherwise (standard-conforming strings: {@code 'a\'} is a complete literal); inside a
     * double- or backtick-quoted identifier only the doubled quote ever escapes. Callers resume scanning at the
     * returned index + 1.
     *
     * <p>A literal written with the PostgreSQL {@code E} prefix ({@code E'it\'s'}) is the exception: that
     * syntax exists only in PostgreSQL and always processes backslash escapes, whatever
     * {@code standard_conforming_strings} is set to, so {@code backslashEscapes} is forced for it and both
     * readings see the same literal.</p>
     *
     * <p>Otherwise no scanner calls this with a single reading: because the two readings disagree on where a
     * literal such as {@code 'a\''} ends, {@link #findUnambiguousUnquotedMarkerIndexes(String, int, char)}
     * evaluates a token under BOTH readings and commits to a marker position only when they agree; a token on
     * which they disagree is left verbatim ({@code ARRAY['a\'', :id]} binds nothing and is emitted unchanged),
     * so an unbound marker fails loudly at the driver instead of a literal being silently corrupted.</p>
     */
    private static int skipQuotedRegion(final String token, final int openIndex, final boolean backslashEscapes) {
        final char quote = token.charAt(openIndex);
        final int len = token.length();
        final boolean escapesWithBackslash = quote == '\'' && (backslashEscapes || isEscapeStringPrefixed(token, openIndex));

        for (int index = openIndex + 1; index < len; index++) {
            final char ch = token.charAt(index);

            if (ch == quote) {
                if (index + 1 < len && token.charAt(index + 1) == quote) {
                    index++;
                } else {
                    return index;
                }
            } else if (ch == '\\' && escapesWithBackslash) {
                index++;
            }
        }

        return len;
    }

    /**
     * Returns {@code true} if the single quote at {@code openIndex} opens a PostgreSQL escape-string literal,
     * that is one prefixed by a standalone {@code E} or {@code e} ({@code E'it\'s'}, {@code ARRAY[E'a\'b']}).
     * The prefix must not continue an identifier, so the {@code 'abc'} of {@code code_E'abc'} or
     * {@code NE'abc'} is not an escape string.
     */
    private static boolean isEscapeStringPrefixed(final String token, final int openIndex) {
        if (openIndex == 0) {
            return false;
        }

        final char prefix = token.charAt(openIndex - 1);

        if (prefix != 'E' && prefix != 'e') {
            return false;
        }

        return openIndex == 1 || !isNamedParameterIdentifierPart(token.codePointBefore(openIndex - 1));
    }

    /**
     * Returns the indexes at or after {@code fromIndex} of every {@code marker} character that lies outside
     * single-, double- or backtick-quoted regions of {@code token} under both escape readings of
     * {@link #skipQuotedRegion(String, int, boolean)} (backslash + doubled quote, and doubled quote only), or
     * {@code null} when the two readings disagree on that set. Agreement means the marker positions do not
     * depend on which dialect the literal is read under ({@code 'it''s :x', :id} and {@code 'a\\', :id} both
     * yield the {@code ':'} of {@code :id}); disagreement ({@code 'a\'', :id}: one literal under the backslash
     * reading, a literal plus an unclosed one under the standard reading) makes the token ambiguous, and the
     * callers then treat it as verbatim SQL, binding and counting nothing inside it. This is by design and
     * fail-safe: a leftover marker fails loudly at the driver instead of a literal being silently corrupted.
     *
     * <p>A PostgreSQL escape string carries its reading in its syntax, so it never makes a token ambiguous:
     * {@link #skipQuotedRegion(String, int, boolean)} always reads {@code E'...'} with backslash escapes and
     * {@code ARRAY[E'it\'s :literal', :id]} therefore binds {@code id} with the literal left intact.</p>
     */
    private static int[] findUnambiguousUnquotedMarkerIndexes(final String token, final int fromIndex, final char marker) {
        final int[] withBackslashEscapes = collectUnquotedMarkerIndexes(token, fromIndex, marker, true);
        final int[] doubledQuoteOnly = collectUnquotedMarkerIndexes(token, fromIndex, marker, false);

        return Arrays.equals(withBackslashEscapes, doubledQuoteOnly) ? withBackslashEscapes : null;
    }

    private static int[] collectUnquotedMarkerIndexes(final String token, final int fromIndex, final char marker, final boolean backslashEscapes) {
        IntList indexes = null;

        for (int index = fromIndex, len = token.length(); index < len; index++) {
            final char ch = token.charAt(index);

            if (isQuoteChar(ch)) {
                index = skipQuotedRegion(token, index, backslashEscapes);
            } else if (ch == '/' && index + 1 < len && token.charAt(index + 1) == '*') {
                final int commentEnd = token.indexOf("*/", index + 2);
                index = commentEnd < 0 ? len : commentEnd + 1;
            } else if (ch == '-' && index + 1 < len && token.charAt(index + 1) == '-') {
                while (index + 1 < len && token.charAt(index + 1) != '\n' && token.charAt(index + 1) != '\r') {
                    index++;
                }
            } else if (ch == marker) {
                if (indexes == null) {
                    indexes = new IntList();
                }

                indexes.add(index);
            }
        }

        return indexes == null ? N.EMPTY_INT_ARRAY : indexes.toArray();
    }

    /**
     * Returns {@code true} if the token is a PostgreSQL-style subscript that may hold positional
     * placeholders: an identifier glued to a bracket region ({@code "arr[?]"}, {@code "ARRAY[?, ?]"}) or a
     * standalone bracket region whose content starts with a lone {@code '?'} ({@code "[?]"}, {@code "[?, ?]"}).
     * A bracket that sits inside a quoted literal or quoted identifier ({@code "'$.items[*] ? (...)'"},
     * {@code "N'a[?]'"}, {@code "\"col[?]\""}), one that follows a qualification dot ({@code "t.[what?]"}),
     * and a standalone bracket-quoted identifier that merely contains {@code '?'} ({@code "[what?]"},
     * {@code "[?foo]"}) do not qualify.
     */
    private static boolean isPositionalSubscriptToken(final String token) {
        final int bracketIndex = token.indexOf('[');

        if (bracketIndex < 0 || isCommentOrSpaceToken(token) || precededByQuote(token, bracketIndex, '\'') || precededByQuote(token, bracketIndex, '"')
                || precededByQuote(token, bracketIndex, '`')) {
            return false;
        }

        if (bracketIndex > 0) {
            return token.charAt(bracketIndex - 1) != '.';
        }

        // Standalone "[...]": mirrors the "[:name" exception -- a positional subscript only when the first
        // non-blank character after '[' is a '?' that is not immediately followed by an identifier character.
        int index = bracketIndex + 1;

        while (index < token.length() && Character.isWhitespace(token.charAt(index))) {
            index++;
        }

        if (index >= token.length() || token.charAt(index) != '?') {
            return false;
        }

        final int afterMarkerIndex = index + 1;

        return afterMarkerIndex >= token.length() || !isNamedParameterIdentifierPart(namedParameterCodePointAt(token, afterMarkerIndex));
    }

    private static boolean precededByQuote(final String token, final int bracketIndex, final char quote) {
        final int quoteIndex = token.indexOf(quote);

        return quoteIndex >= 0 && quoteIndex < bracketIndex;
    }

    /**
     * Returns the indexes of the {@code '?'} characters from {@code fromIndex} that are outside single-, double-
     * or backtick-quoted regions under both escape readings (see
     * {@link #findUnambiguousUnquotedMarkerIndexes(String, int, char)}), so a {@code '?'} inside a literal
     * in a subscript ({@code "ARRAY['a?', ?]"}, {@code "ARRAY['it''s ?', ?]"}, {@code "ARRAY[E'it\'s ?', ?]"})
     * is not mistaken for a placeholder. An ambiguous token ({@code "ARRAY['a\'?', ?]"}) yields no indexes at
     * all: none of its {@code '?'} characters is counted, and it contributes nothing to
     * {@link #positionalParameterOffsets()}.
     */
    private static int[] findUnquotedQuestionMarkIndexes(final String token, final int fromIndex) {
        final int[] indexes = findUnambiguousUnquotedMarkerIndexes(token, fromIndex, '?');

        return indexes == null ? N.EMPTY_INT_ARRAY : indexes;
    }

    /**
     * Returns {@code true} if the {@code '['} at {@code bracketIndex} opens a PostgreSQL-style
     * subscript whose content starts with a named binding: the named-parameter prefix {@code ':'}
     * immediately followed by a valid parameter-name start. Shapes such as {@code "[:]"},
     * {@code "[::int]"} or {@code "[column]"} do not qualify and remain bracket-quoted identifiers.
     */
    private static boolean isNamedParameterSubscript(final String token, final int bracketIndex) {
        final int len = token.length();
        int index = bracketIndex + 1;

        // Mirror isPositionalSubscriptToken: "[ :ids ]" binds exactly like "[ ? ]" does.
        while (index < len && Character.isWhitespace(token.charAt(index))) {
            index++;
        }

        return index + 1 < len && token.charAt(index) == _PREFIX_OF_NAMED_PARAMETER
                && isNamedParameterIdentifierStart(namedParameterCodePointAt(token, index + 1));
    }

    /**
     * Returns the indexes of every {@code ':'} of {@code token} that lies outside single-, double- and
     * backtick-quoted regions, so a marker inside a literal element of a subscript ({@code "ARRAY[':x']"})
     * is not mistaken for a binding, or an empty array when the token holds none or its quoting is ambiguous
     * between the two escape readings (see {@link #findUnambiguousUnquotedMarkerIndexes(String, int, char)}),
     * in which case the token is verbatim. Collected once for the whole token: which of these indexes really
     * starts a parameter is then decided per marker by {@link #isNamedParameterStart(String, int, int)},
     * because that depends on where the name of the previous one ended.
     *
     * <p>Restricting the result to a suffix is sound: every index it reports sits outside every quoted region
     * under both readings, so a scan that resumes at one of them starts from the same unquoted state and reads
     * the remaining text identically.</p>
     */
    private static int[] findUnquotedNamedParameterMarkerIndexes(final String token) {
        final int[] indexes = findUnambiguousUnquotedMarkerIndexes(token, 0, _PREFIX_OF_NAMED_PARAMETER);

        return indexes == null ? N.EMPTY_INT_ARRAY : indexes;
    }

    /**
     * Returns {@code true} if the {@code ':'} at {@code index} of {@code token} starts a named parameter: a
     * valid parameter-name start follows it and it sits at a safe boundary, so a PostgreSQL cast
     * ({@code "::int"}), a qualified name and a {@code ':'} inside a name are not parameters.
     * {@code fromIndex} is where the scan resumed, that is the end of the previously extracted parameter, so
     * the second marker of {@code ":a:b"} is a parameter although a name character precedes it.
     */
    private static boolean isNamedParameterStart(final String token, final int index, final int fromIndex) {
        return index + 1 < token.length() && isNamedParameterIdentifierStart(namedParameterCodePointAt(token, index + 1))
                && isNamedParameterStartBoundary(token, index, fromIndex);
    }

    private static boolean isNamedParameterStartBoundary(final String token, final int parameterStartIndex, final int fromIndex) {
        if (parameterStartIndex == 0 || parameterStartIndex == fromIndex) {
            return true;
        }

        final int previousCodePoint = namedParameterCodePointBefore(token, parameterStartIndex);

        return previousCodePoint != _PREFIX_OF_NAMED_PARAMETER && previousCodePoint != '.' && !isNamedParameterIdentifierPart(previousCodePoint);
    }

    private static int nextNonCommentWord(final List<String> words, final int fromIndex) {
        for (int i = fromIndex, size = words.size(); i < size; i++) {
            if (!isCommentOrSpaceToken(words.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private static int previousNonCommentWord(final List<String> words, final int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (!isCommentOrSpaceToken(words.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isPostgreSqlJsonQuestionOperator(final List<String> words, final int questionMarkIndex) {
        final int previousIndex = previousNonCommentWord(words, questionMarkIndex - 1);
        final int nextIndex = nextNonCommentWord(words, questionMarkIndex + 1);

        return previousIndex >= 0 && nextIndex >= 0 && canPrecedeJsonQuestionOperator(words.get(previousIndex))
                && canFollowJsonQuestionOperator(words.get(nextIndex));
    }

    private static boolean canPrecedeJsonQuestionOperator(final String word) {
        if (Strings.isEmpty(word)) {
            return false;
        }

        final char first = word.charAt(0);

        // The left operand of a JSON existence operator is a value: a column reference (optionally cast,
        // "x::jsonb"), a quoted identifier or literal, a placeholder, or the ")" / "]" closing a
        // sub-expression. A token that begins with operator punctuation ("=", "<>", "+", "||", "->>",
        // "~*", "(", ",", ...) is an operator, so the "?" after it is a positional placeholder. Checking
        // the leading character covers every operator spelling instead of enumerating them.
        if (!(first == ')' || first == ']' || first == '?' || first == '_' || first == '"' || first == '`' || first == '\'' || first == '$' || first == '.'
                || first == _PREFIX_OF_NAMED_PARAMETER || word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) || startsWithSqlExpressionWord(word))) {
            return false;
        }

        return !isSqlExpressionBoundaryWord(word) && !isPlaceholderLeadingKeyword(word);
    }

    /**
     * Returns {@code true} for keywords after which a {@code ?} is always a positional placeholder and never
     * the left operand of a JSON existence operator: clause openers, CASE parts, and operator-like keywords
     * that take a value on their right ({@code INTERVAL ? DAY}, {@code ILIKE ? ESCAPE '!'},
     * {@code SIMILAR TO ? ...}, {@code AT TIME ZONE ?}). Without this list such a {@code ?} followed by an
     * identifier-like word ({@code DAY}, {@code ESCAPE}, an alias) would be dropped from the parameter count.
     */
    private static boolean isPlaceholderLeadingKeyword(final String word) {
        return "SELECT".equalsIgnoreCase(word) || "WHERE".equalsIgnoreCase(word) || "HAVING".equalsIgnoreCase(word) || "ON".equalsIgnoreCase(word)
                || "AND".equalsIgnoreCase(word) || "OR".equalsIgnoreCase(word) || "NOT".equalsIgnoreCase(word) || "IN".equalsIgnoreCase(word)
                || "VALUES".equalsIgnoreCase(word) || "SET".equalsIgnoreCase(word) || "THEN".equalsIgnoreCase(word) || "ELSE".equalsIgnoreCase(word)
                || "WHEN".equalsIgnoreCase(word) || "CASE".equalsIgnoreCase(word) || "INTERVAL".equalsIgnoreCase(word) || "ILIKE".equalsIgnoreCase(word)
                || "RLIKE".equalsIgnoreCase(word) || "REGEXP".equalsIgnoreCase(word) || "TO".equalsIgnoreCase(word) || "ESCAPE".equalsIgnoreCase(word)
                || "ZONE".equalsIgnoreCase(word) || "DIV".equalsIgnoreCase(word) || "MOD".equalsIgnoreCase(word);
    }

    private static boolean canFollowJsonQuestionOperator(final String word) {
        if (Strings.isEmpty(word) || isSqlExpressionBoundaryWord(word)) {
            return false;
        }

        final char firstChar = word.charAt(0);

        // PostgreSQL's JSON existence operator accepts any text-valued expression on its right,
        // including column references and function calls such as "payload ? lower(:key)". Requiring
        // a literal/placeholder here misclassifies the operator as a JDBC placeholder and then
        // falsely reports mixed parameter styles when the expression contains a named parameter.
        return word.equals(SK.QUESTION_MARK) || firstChar == '\'' || firstChar == '"' || firstChar == '`' || firstChar == '(' || firstChar == '['
                || (firstChar == _PREFIX_OF_NAMED_PARAMETER && word.length() >= 2 && isNamedParameterIdentifierStart(namedParameterCodePointAt(word, 1)))
                || word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) || startsWithSqlExpressionWord(word);
    }

    private static boolean isSqlExpressionBoundaryWord(final String word) {
        return "AND".equalsIgnoreCase(word) || "OR".equalsIgnoreCase(word) || "FROM".equalsIgnoreCase(word) || "WHERE".equalsIgnoreCase(word)
                || "GROUP".equalsIgnoreCase(word) || "ORDER".equalsIgnoreCase(word) || "HAVING".equalsIgnoreCase(word) || "LIMIT".equalsIgnoreCase(word)
                || "OFFSET".equalsIgnoreCase(word) || "FETCH".equalsIgnoreCase(word) || "FOR".equalsIgnoreCase(word) || "RETURNING".equalsIgnoreCase(word)
                || "UNION".equalsIgnoreCase(word) || "INTERSECT".equalsIgnoreCase(word) || "EXCEPT".equalsIgnoreCase(word) || "MINUS".equalsIgnoreCase(word)
                || "JOIN".equalsIgnoreCase(word) || "ON".equalsIgnoreCase(word) || "USING".equalsIgnoreCase(word) || "AS".equalsIgnoreCase(word)
                || "WHEN".equalsIgnoreCase(word) || "THEN".equalsIgnoreCase(word) || "ELSE".equalsIgnoreCase(word) || "END".equalsIgnoreCase(word)
                || "IS".equalsIgnoreCase(word) || "IN".equalsIgnoreCase(word) || "LIKE".equalsIgnoreCase(word) || "BETWEEN".equalsIgnoreCase(word)
                || "NOT".equalsIgnoreCase(word) || "BY".equalsIgnoreCase(word) || "ASC".equalsIgnoreCase(word) || "DESC".equalsIgnoreCase(word)
                || "NULLS".equalsIgnoreCase(word) || "FIRST".equalsIgnoreCase(word) || "LAST".equalsIgnoreCase(word) || "ROW".equalsIgnoreCase(word)
                || "ROWS".equalsIgnoreCase(word) || "ONLY".equalsIgnoreCase(word) || "TOP".equalsIgnoreCase(word) || "NEXT".equalsIgnoreCase(word)
                || "DISTINCT".equalsIgnoreCase(word) || "ALL".equalsIgnoreCase(word) || "ANY".equalsIgnoreCase(word) || "SOME".equalsIgnoreCase(word);
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

    private static boolean isNamedParameterIdentifierStart(final int codePoint) {
        return codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint);
    }

    private static boolean isNamedParameterIdentifierPart(final int codePoint) {
        return codePoint == '_' || Character.isUnicodeIdentifierPart(codePoint);
    }

    private static int namedParameterCodePointAt(final String token, final int index) {
        final char first = token.charAt(index);

        // Character.codePointAt deliberately returns an isolated surrogate as an integer. Reject it
        // explicitly so malformed UTF-16 cannot become an invisible or unbindable parameter name.
        if (Character.isHighSurrogate(first)) {
            if (index + 1 >= token.length() || !Character.isLowSurrogate(token.charAt(index + 1))) {
                throw new IllegalArgumentException("Malformed named parameter: unpaired high surrogate at token index " + index);
            }

            return Character.toCodePoint(first, token.charAt(index + 1));
        }

        if (Character.isLowSurrogate(first)) {
            throw new IllegalArgumentException("Malformed named parameter: unpaired low surrogate at token index " + index);
        }

        return first;
    }

    private static int namedParameterCodePointBefore(final String token, final int index) {
        final char last = token.charAt(index - 1);

        if (Character.isLowSurrogate(last)) {
            if (index < 2 || !Character.isHighSurrogate(token.charAt(index - 2))) {
                throw new IllegalArgumentException("Malformed named parameter boundary: unpaired low surrogate at token index " + (index - 1));
            }

            return Character.toCodePoint(token.charAt(index - 2), last);
        }

        if (Character.isHighSurrogate(last)) {
            throw new IllegalArgumentException("Malformed named parameter boundary: unpaired high surrogate at token index " + (index - 1));
        }

        return last;
    }

    private static boolean startsWithSqlExpressionWord(final String word) {
        final char first = word.charAt(0);

        if (Character.isSurrogate(first)) {
            if (!Character.isHighSurrogate(first) || word.length() < 2 || !Character.isLowSurrogate(word.charAt(1))) {
                return false;
            }

            final int codePoint = Character.toCodePoint(first, word.charAt(1));
            return Character.isUnicodeIdentifierStart(codePoint) || Character.isDigit(codePoint);
        }

        return first == '_' || first == '.' || Character.isUnicodeIdentifierStart(first) || Character.isDigit(first);
    }

    /**
     * Returns the hash code value for this {@code ParsedSql}.
     * The hash code is based on the trimmed original SQL string returned by {@link #originalSql()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql a = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
     * ParsedSql b = ParsedSql.parse("  SELECT * FROM users WHERE id = :id  ");
     * // Both trim to the same original SQL, so their hash codes match
     * boolean sameHash = a.hashCode() == b.hashCode();   // true
     * }</pre>
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code ParsedSql} objects are equal if their trimmed original SQL strings
     * (as returned by {@link #originalSql()}) are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql a = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
     * ParsedSql b = ParsedSql.parse("  SELECT * FROM users WHERE id = :id  ");
     * ParsedSql c = ParsedSql.parse("SELECT * FROM users WHERE id = :otherId");
     *
     * boolean eq = a.equals(b);                    // true (same trimmed original SQL)
     * boolean ne = a.equals(c);                    // false (different parameter name)
     * boolean notString = a.equals("SELECT ...");  // false (not a ParsedSql)
     * }</pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object equals the obj argument; {@code false} otherwise
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
     * Returns a string representation of this {@code ParsedSql}.
     * The string contains both the original SQL and the parameterized SQL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
     * String s = parsed.toString();
     * // "{sql=SELECT * FROM users WHERE id = :id, parameterizedSql=SELECT * FROM users WHERE id = ?}"
     * }</pre>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{sql=" + sql + ", parameterizedSql=" + parameterizedSql + "}";
    }
}
