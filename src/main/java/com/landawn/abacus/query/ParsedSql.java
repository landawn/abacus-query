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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
 *       e.g. {@code #{ paramName }}; as in MyBatis, the name ends at the first comma or colon, and the
 *       {@code :jdbcType} shorthand and attributes after it are discarded, so {@code #{ id, jdbcType=BIGINT }}
 *       and {@code #{id:BIGINT}} both bind {@code id}; a marker left without a name, such as {@code #{:id}}, is
 *       rejected)</li>
 *   <li>Standard JDBC placeholders: {@code ?}</li>
 * </ul>
 *
 * <p>A colon-style parameter name follows Unicode identifier grammar. Each dot-separated segment
 * starts with {@code _} or a Unicode identifier-start code point and continues with {@code _} or
 * Unicode identifier-part code points. Digits are therefore allowed after the first code point but
 * not at the start of a segment. Unicode whitespace and punctuation are delimiters rather than name
 * characters, and an unpaired UTF-16 surrogate in, or immediately before, a prospective parameter name
 * is rejected. A colon glued to a closing quote, parenthesis or bracket is not a marker: it separates the
 * key and value of a compact {@code JSON_OBJECT('key':value)}, or bounds a slice ({@code arr[f(x):n]}).</p>
 *
 * <p>Parameter detection and conversion is only performed when the SQL is recognized as a
 * data operation statement (one whose first non-comment / non-parenthesis token is
 * {@code SELECT}, {@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code WITH}, {@code MERGE},
 * {@code CALL}, {@code VALUES}, {@code TABLE}, {@code EXPLAIN} or {@code REPLACE}, also when a quoted name or
 * bracket group is glued to it, as in {@code SELECT"a"}; a leading byte-order mark
 * {@code U+FEFF} is ignored for this purpose and kept in the SQL text). JDBC call escapes
 * (<code>{call ...}</code> and <code>{? = call ...}</code>) are recognized as {@code CALL}, including
 * when the tokenizer emits a glued <code>{call</code> opener token; the return-value slot may also be a named or
 * MyBatis marker (<code>{:result = call f(:param)}</code>,
 * <code>{#{result, mode=OUT, jdbcType=INTEGER} = call f(#{param})}</code>). For an {@code EXPLAIN}
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
 * <p>Markers inside quoted literals, quoted identifiers, and SQL comments are not parameters (PostgreSQL
 * dollar-quoted strings such as {@code $$...$$} and {@code $tag$...$tag$} are not recognized as quoted text:
 * markers and quote characters inside them are processed as ordinary SQL, so {@code x = $$ :b $$} binds
 * {@code b}); this also holds for a literal element inside a subscript ({@code ARRAY[':x']}), while a marker next to
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
 * below. The chain root must be an unquoted identifier: after a quoted identifier the chain stops, and a
 * following group is classified by the standalone rules instead of by the rules of the first group. So
 * {@code "q"[?]['a', ?]} and {@code t."q"[?]['a', ?]} count only the first {@code ?} (the trailing
 * {@code ['a', ?]} is a standalone bracket-quoted identifier), while {@code "q"[?][?]} still counts both,
 * a standalone {@code [?]} being a positional subscript. A backtick-quoted ({@code `q`[?]['a', ?]}) or
 * bracket-quoted ({@code [q][?]['a', ?]}) root behaves exactly like the double-quoted one.
 * A leading or embedded PostgreSQL-style subscript whose first non-blank content is a {@code :name}
 * binding ({@code [:name]}, {@code [ :name ]}, {@code arr[:name]}) is the bracket-specific exception:
 * it is treated as a named binding rather than as a bracket-quoted identifier (a standalone
 * {@code [#{name}]} remains a bracket-quoted identifier). A bracket immediately after a qualification dot, such as {@code table.[:name]},
 * remains a quoted identifier. As a consequence, a bracket-quoted identifier whose first character
 * is {@code ':'} (for example the SQL Server column reference {@code SELECT [:identifier] FROM t})
 * is parameterized rather than preserved; qualify it ({@code t.[:identifier]}) to keep it literal.
 * Likewise a subscript whose content is a positional placeholder ({@code ARRAY[?]}, {@code arr[?, ?]},
 * or a standalone {@code [?]}) counts its {@code ?} markers as JDBC parameters. Standalone groups
 * recognized by either a leading named or positional marker inspect all parameter styles, so
 * {@code [:id, ?]} and {@code [?, #{id}]} are rejected as mixed styles. A {@code ?} standing between two
 * operands inside such a group is a PostgreSQL JSON existence operator ({@code ?}, {@code ?|}, {@code ?&})
 * rather than a placeholder, and is preserved and left uncounted exactly as it is outside brackets, so
 * {@code ARRAY [:payload ? 'key']} binds one named parameter. Operand boundaries respect SQL operators,
 * parentheses, quoted values, and intervening block or dash-line comments; a positional operand such as
 * the last marker in {@code ARRAY[doc ? ?]} is still counted. A marker that opens or closes a subscript
 * element has no operand on one side and remains a placeholder unless it opens an explicitly typed unary
 * geometric expression as described below. A bracket-quoted
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
 * <p>The pgJDBC escape {@code ??} represents an operator question mark, including in {@code ??|},
 * {@code ??&}, and {@code @??}, and contributes no binding. Escapes are preserved verbatim for the driver. Only
 * adjacent question marks form an escape: {@code doc ?? ?} has one positional parameter, while
 * {@code ? ?? ?} has two; whitespace or comments between the question marks prevent pairing.</p>
 *
 * <p>Compact positional expressions such as {@code ?||?||?} and {@code ?-1} retain every binding.
 * PostgreSQL's unary {@code ?-} and {@code ?|} operators are preserved when the operand explicitly
 * identifies a {@code line} or {@code lseg} literal, constructor or cast (for example
 * {@code ?- lseg '(0,0),(1,0)'} or {@code ?| ?::line}). Cast type names may be separated by whitespace
 * or comments, double-quoted, or qualified with {@code pg_catalog}; quoted names retain their case
 * sensitivity (for example {@code ?- CAST(? AS "line")}). Chained casts use their final type, so
 * {@code ?- CAST(? AS text)::line} contains only the operand's binding. Qualified typed literals may be
 * adjacent to their quoted value, as in {@code ?-pg_catalog.line'(0,0),(1,0)'}. Without type information, {@code ?-column}
 * is ambiguous with a placeholder followed by subtraction and is treated as a binding. SQL/JSON clauses
 * {@code NULL ON NULL}, {@code ABSENT ON NULL}, {@code FORMAT JSON} and
 * {@code WITH}/{@code WITHOUT UNIQUE [KEYS]} are recognized in constructor
 * value-argument context, not in the query body of {@code JSON_ARRAY(SELECT ...)} or
 * {@code JSON_ARRAY(WITH ... SELECT ...)}, including a query beginning with a parenthesized term.
 * A scalar subquery followed by a comma still belongs to a constructor value list.
 * Nested constructors establish their own value context.
 * A genuine JSON operator may still take {@code NULL}, an identifier named {@code format} or {@code value}, or
 * a call to {@code format(...)} as its right operand. In a {@code JSON_OBJECT} or {@code JSON_OBJECTAGG} argument
 * list, a {@code KEY} that opens an entry is the key/value keyword, so the {@code ?} after it is the key's placeholder
 * ({@code KEY ? VALUE ?}, {@code KEY ?||'_x' VALUE ?}).</p>
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

    private static final int FACTOR = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    /** The parse cache, or {@code null} if it could not be created because the JVM was already shutting down. */
    private static final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> pool = createCache(
            () -> PoolFactory.createKeyedObjectPool(1000 * FACTOR, EVICT_TIME));

    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    /** Returned by {@link #findIbatisClosingBraceIndex(String, int)} for a MyBatis marker that can never be closed. */
    private static final int MALFORMED_IBATIS_MARKER = -2;

    /** Bit flag recording that a positional {@code ?} placeholder was found. */
    private static final int QUESTION_MARK_TYPE = 1;

    /** Bit flag recording that a {@code :propName} placeholder was found. */
    private static final int NAMED_PARAMETER_TYPE = 2;

    /** Bit flag recording that a {@code #{propName}} placeholder was found. */
    private static final int IBATIS_PARAMETER_TYPE = 4;

    /** Default SQL separators grouped by their first character, longest first, for scanning bracket interiors. */
    private static final String[][] SUBSCRIPT_SEPARATORS = subscriptSeparators();

    /**
     * Configured separators spelled with a leading {@code '?'} ({@code ?-}, {@code ?|}, {@code ?&},
     * {@code ?#}, {@code ?||}, {@code ?-|}). Longest-match tokenization glues a placeholder written
     * against one of them into a single token, so {@code ?-1} and {@code ?||'x'} arrive here as the words
     * {@code ?-} and {@code ?||} rather than as a marker followed by an operator.
     */
    private static final Set<String> QUESTION_MARK_LEADING_SEPARATORS = questionMarkLeadingSeparators();

    private final String sql;

    private final String parameterizedSql;

    private final ImmutableList<String> namedParameters;

    private final int parameterCount;

    /** Whether the SQL is a recognized data operation statement, the only kind whose markers are detected. */
    private final boolean dataOperation;

    /**
     * Character offsets, in {@link #originalSql()}, of exactly the positional {@code '?'} markers counted by
     * {@link #parameterCount()} (ascending), or {@code null} if the token stream could not be aligned back onto
     * the original text. Empty when no positional marker was counted.
     */
    private final int[] positionalParameterOffsets;

    /** Cached hash code. This object is immutable, so {@code sql.hashCode()} is computed once. */
    private final int hashCode;

    /**
     * Parses a nonblank SQL string whose outer argument validation has already completed.
     *
     * @param sql the nonblank SQL string
     * @throws IllegalArgumentException if a recognized data-operation statement mixes parameter styles,
     *         contains an iBatis/MyBatis marker without a closing brace or with an empty property name
     *         ({@code #{:id}}), or contains an unpaired UTF-16
     *         surrogate in a prospective colon-style name, its preceding boundary, or immediately after
     *         a positional marker opening a standalone bracket group
     */
    private ParsedSql(final String sql) {
        this.sql = sql.trim();
        hashCode = this.sql.hashCode();

        final List<String> tokens = SqlParser.tokenize(this.sql);
        // The tokenizer can split an adjacent question mark and MyBatis binding as ?#, {name}.
        // Repair only that boundary before either scanner runs; ordinary SQL needs no extra token pass.
        final List<String> words = this.sql.indexOf("?#{") >= 0 ? restoreEscapedIbatisOpeners(tokens) : tokens;
        final String firstOpWord = resolveFirstOpWord(words);
        final boolean isOpSqlPrefix = Strings.isNotEmpty(firstOpWord) && isOpSqlPrefixWord(firstOpWord);
        dataOperation = isOpSqlPrefix;

        List<String> namedParameterList = null;
        // Ordinary bindings record their token once; bracket groups retain the scanner's owned
        // offset array. This avoids a token-index entry per array element and preserves source
        // positions for raw sub-query rewriting when operators/quoted '?' require token alignment.
        IntList questionMarkTokenIndexes = null;
        int[][] subscriptParameterOffsets = null;
        int paramCount = 0;
        int type = 0; // Bit flags: QUESTION_MARK_TYPE, NAMED_PARAMETER_TYPE, IBATIS_PARAMETER_TYPE
        // Remembers where a positional marker came from so a mixed-style rejection can explain the
        // distinction between a bracket group's placeholders and JSON operators.
        boolean questionMarkFromSubscript = false;

        // A bracket group that continues a subscript rooted in an identifier ("x[?][?]",
        // "x['a', :b]['c', :d]", "x[?] ['c', ?]") is a chained subscript: the tokenizer emits it as a
        // standalone "[...]" token, but it is inspected exactly like that first group. Computed up front in
        // one pass so the per-token lookup below stays O(1).
        // Without an opening bracket there cannot be a chain. Avoid its scan and per-token array
        // for ordinary SQL; quoted/commented brackets may cause harmless extra work, never a bypass.
        final boolean[] chainedSubscripts = isOpSqlPrefix && this.sql.indexOf('[') >= 0 ? markChainedSubscriptTokens(words) : null;
        // Classify the original tokens once, before named/MyBatis conversion changes their text or joins
        // split bindings. The same forward state machine handles bracket interiors below.
        final int[] positionalTokenIndexes = isOpSqlPrefix && this.sql.indexOf('?') >= 0 ? new QuestionMarkClassifier(words, null, chainedSubscripts).classify()
                : N.EMPTY_INT_ARRAY;
        int positionalTokenCursor = 0;
        boolean lastSourceTokenEndsWithQuestionMark = false;
        // Source offset of every token, aligned lazily, once, when two string literals are separated by whitespace and
        // the SQL contains a line break somewhere.
        int[] tokenSourceOffsets = null;
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0, size = words.size(); i < size; i++) {
                String word = words.get(i);
                // A MyBatis binding may join following tokens and advance i; offset 0 of word stays at this token.
                final int tokenIndex = i;

                if (isOpSqlPrefix) {
                    final boolean chainedSubscript = chainedSubscripts != null && chainedSubscripts[i];

                    if (word.indexOf('?') >= 0 && (chainedSubscript || isParameterSubscriptToken(word))) {
                        // Positional placeholders embedded in a subscript-shaped token ("ARRAY[?]", "arr[?, ?]",
                        // standalone "[?]"): the tokenizer keeps the bracket region glued to the preceding
                        // identifier, so the "?" never surfaces as its own token. Kept independent of the
                        // marker chain below so a token that also carries a "#{...}" or ":name" marker still
                        // reaches the mixed-style guard.
                        final int[] embedded = findSubscriptPositionalParameterIndexes(word, word.indexOf('[') + 1);

                        if (embedded.length > 0) {
                            if (subscriptParameterOffsets == null) {
                                subscriptParameterOffsets = new int[size][];
                            }
                            subscriptParameterOffsets[i] = embedded;
                            paramCount += embedded.length;
                            type |= QUESTION_MARK_TYPE;
                            questionMarkFromSubscript = true;
                        }
                    }

                    while (positionalTokenCursor < positionalTokenIndexes.length && positionalTokenIndexes[positionalTokenCursor] < i) {
                        positionalTokenCursor++; // A MyBatis binding may have joined several original tokens.
                    }

                    if (positionalTokenCursor < positionalTokenIndexes.length && positionalTokenIndexes[positionalTokenCursor] == i) {
                        positionalTokenCursor++;
                        if (questionMarkTokenIndexes == null) {
                            questionMarkTokenIndexes = new IntList();
                        }
                        questionMarkTokenIndexes.add(i);
                        paramCount++;
                        type |= QUESTION_MARK_TYPE;
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

                            appendAfterConvertedMarker(rebuilt, word, copiedFrom, markerStartIndex);

                            // The '}' is searched outside quoted regions and comments: a quote balanced inside the marker
                            // belongs to its property path or attributes ("#{map['k']}", "ARRAY[#{a, 'x'}]"), while a '}'
                            // inside a literal cannot close it ("ARRAY[#{a, '}']" must not become "ARRAY[?']"). A quote left
                            // open, or a second "#{" before the '}', makes the marker malformed.
                            int closingIndex = findIbatisClosingBraceIndex(word, markerStartIndex + 2);

                            if (closingIndex == MALFORMED_IBATIS_MARKER) {
                                throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: missing closing '}' in: " + this.sql);
                            }

                            if (closingIndex < 0) {
                                // A bracket token ends at its closing ']', so an opener inside an open group ("ARRAY[#{a]")
                                // whose '}' is not in the token cannot be closed by a later token either: joining would
                                // swallow the SQL after the group into the name. An opener outside every group, before it
                                // ("#{list[0]" + "}") or after a closed one ("#{a[0]}#{b" + " }"), continues in the next token.
                                if (isInsideOpenBracketGroup(word, markerStartIndex)) {
                                    throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: missing closing '}' in: " + this.sql);
                                }

                                // The '}' is in a following token: join tokens until it appears, and continue
                                // in that joined text, whose marker now starts at 0. The text taken from this token
                                // may still hold a '}' inside a balanced literal, so only the joined tokens are searched.
                                final StringBuilder ibatisTokenBuilder = new StringBuilder();
                                ibatisTokenBuilder.append(word, markerStartIndex, word.length());

                                while (closingIndex < 0 && i < size - 1) {
                                    final String nextWord = words.get(++i);

                                    // Each joined token is searched like the opening one: a quote balanced within a property
                                    // path belongs to the binding ("#{ map['k'] }"), while a quoted literal or identifier token
                                    // never does ("#{ x AND d = '}'" must not leave an unbalanced quote), and a new "#{" before
                                    // the '}' means this marker was never closed (no swallowing the SQL up to a later "#{y}").
                                    final int braceIndex = findIbatisContinuationClosingBraceIndex(nextWord);

                                    if (braceIndex == MALFORMED_IBATIS_MARKER) {
                                        throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: missing closing '}' in: " + this.sql);
                                    }

                                    if (braceIndex >= 0) {
                                        closingIndex = ibatisTokenBuilder.length() + braceIndex;
                                    }

                                    ibatisTokenBuilder.append(nextWord);
                                }

                                if (closingIndex < 0) {
                                    throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: missing closing '}' in: " + this.sql);
                                }

                                word = ibatisTokenBuilder.toString();
                                markerStartIndex = 0;
                                markerIndexes = findUnquotedIbatisMarkerIndexes(word);
                                markerCursor = 0; // the marker being handled is skipped by the copiedFrom guard
                            }

                            // Content between "#{" and "}"; empty for the literal "#{}".
                            final String content = word.substring(markerStartIndex + 2, closingIndex);
                            final String namedParameter = extractIbatisNamedParameter(content);

                            if (namedParameter.isEmpty() && !Strings.isBlank(content)) {
                                // "#{:id}", "#{, mode=IN}": the name ends at the first ':' or ',', so nothing is left to bind.
                                // Keeping the text verbatim would let the named-marker scan below convert the ":id" inside it.
                                throw new IllegalArgumentException("Malformed iBatis/MyBatis parameter: empty property name in: " + this.sql);
                            }

                            if (!namedParameter.isEmpty()) {
                                if (namedParameterList == null) {
                                    namedParameterList = new ArrayList<>();
                                }
                                namedParameterList.add(namedParameter);
                                appendConvertedMarker(rebuilt, sb);
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

                        appendAfterConvertedMarker(rebuilt, word, copiedFrom, word.length());
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
                            final boolean gluedToClosedValue = followsClosedValueToken(words, tokenIndex, null);
                            int copiedFrom = 0;
                            int searchFrom = 0;

                            for (final int parameterStartIndex : markerIndexes) {
                                // A ':' the previous parameter's name swallowed is not a marker of its own,
                                // and a boundary check rejects casts and qualified names.
                                if (parameterStartIndex < searchFrom || !isNamedParameterStart(word, parameterStartIndex, searchFrom, gluedToClosedValue)) {
                                    continue;
                                }

                                appendAfterConvertedMarker(rebuilt, word, copiedFrom, parameterStartIndex);

                                final int parameterEndIndex = findNamedParameterEndIndex(word, parameterStartIndex + 1);
                                if (namedParameterList == null) {
                                    namedParameterList = new ArrayList<>();
                                }
                                namedParameterList.add(word.substring(parameterStartIndex + 1, parameterEndIndex));
                                appendConvertedMarker(rebuilt, sb);
                                paramCount++;
                                type |= NAMED_PARAMETER_TYPE;

                                copiedFrom = parameterEndIndex;
                                searchFrom = parameterEndIndex;
                            }

                            appendAfterConvertedMarker(rebuilt, word, copiedFrom, word.length());
                            word = rebuilt.toString();
                        }
                    }

                    if (Integer.bitCount(type) > 1) {
                        throw new IllegalArgumentException(mixedParameterStyleMessage(type, questionMarkFromSubscript));
                    }

                    // A '?' that ends the output but not the source token was converted from a marker
                    // ("#{a}?|x"). Keep it apart from a following source '?' so the pair is not read as
                    // a pgJDBC "??" escape; two adjacent source question marks stay verbatim.
                    if (!lastSourceTokenEndsWithQuestionMark && word.startsWith(SK.QUESTION_MARK) && sb.length() > 0 && sb.charAt(sb.length() - 1) == '?') {
                        sb.append(' ');
                    }

                    lastSourceTokenEndsWithQuestionMark = words.get(i).endsWith(SK.QUESTION_MARK);

                    // The tokenizer collapses a whitespace run, line breaks and line comments included, into " ".
                    // Between two string literals that changes the SQL: the standard (and PostgreSQL) concatenate
                    // adjacent literals only when a line break separates them ('a'\n'b' is 'ab', 'a' 'b' is a syntax
                    // error), so keep a line break that the original gap contained.
                    if (SK.SPACE.equals(word) && i > 0 && i + 1 < size && sb.length() > 0 && sb.charAt(sb.length() - 1) == '\''
                            && words.get(i - 1).endsWith("'") && words.get(i + 1).startsWith("'")) {
                        if (tokenSourceOffsets == null) {
                            // Empty when the SQL has no line break at all (or, defensively, cannot be aligned).
                            final int[] aligned = containsLineBreak(this.sql, 0, this.sql.length()) ? alignTokenSourceOffsets(this.sql, words) : null;
                            tokenSourceOffsets = aligned == null ? N.EMPTY_INT_ARRAY : aligned;
                        }

                        if (tokenSourceOffsets.length > 0
                                && containsLineBreak(this.sql, tokenSourceOffsets[i - 1] + words.get(i - 1).length(), tokenSourceOffsets[i + 1])) {
                            word = "\n";
                        }
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
            namedParameters = namedParameterList == null ? ImmutableList.empty() : ImmutableList.wrap(namedParameterList);
            if ((type & QUESTION_MARK_TYPE) == 0) {
                positionalParameterOffsets = N.EMPTY_INT_ARRAY;
            } else {
                // Bracket scanners already supply every inner offset. Align their containing tokens
                // directly; rediscovering their markers character by character would duplicate that work.
                final int[] directOffsets = subscriptParameterOffsets == null ? allQuestionMarkOffsets(this.sql, paramCount) : null;
                positionalParameterOffsets = directOffsets != null ? directOffsets
                        : resolvePositionalParameterOffsets(this.sql, words, questionMarkTokenIndexes, null, subscriptParameterOffsets, paramCount);
            }
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Appends the {@code ?} that replaces a named or iBatis marker. A {@code ?} emitted directly after
     * an unpaired one ({@code payload?:key}, {@code @?:path}, {@code :a:b}, {@code #{a}#{b}}) would form
     * {@code ??}, which pgJDBC and {@link #parse(String)} read as an escaped operator rather than a binding,
     * so a space separates them. After complete {@code ??} escapes ({@code doc ??#{key}}) the new {@code ?}
     * cannot pair and is appended directly.
     *
     * @param rebuilt the token text rebuilt so far
     * @param preceding the SQL emitted before the token, continuing the run when {@code rebuilt} is all {@code ?}
     */
    private static void appendConvertedMarker(final StringBuilder rebuilt, final StringBuilder preceding) {
        int run = trailingQuestionMarkCount(rebuilt);

        if (run == rebuilt.length()) {
            run += trailingQuestionMarkCount(preceding);
        }

        // Escapes pair adjacent question marks from the left, so an odd run ends in an unpaired '?'.
        if ((run & 1) != 0) {
            rebuilt.append(' ');
        }

        rebuilt.append(SK.QUESTION_MARK);
    }

    private static int trailingQuestionMarkCount(final StringBuilder text) {
        int index = text.length();

        while (index > 0 && text.charAt(index - 1) == '?') {
            index--;
        }

        return text.length() - index;
    }

    /**
     * Copies source text that follows a converted marker, separating a leading {@code ?} from the
     * marker's {@code ?} so the pair does not form a pgJDBC {@code ??} escape. Before the first marker
     * {@code rebuilt} is empty and the text is copied unchanged.
     */
    private static void appendAfterConvertedMarker(final StringBuilder rebuilt, final String word, final int from, final int to) {
        if (from < to && word.charAt(from) == '?' && rebuilt.length() > 0 && rebuilt.charAt(rebuilt.length() - 1) == '?') {
            rebuilt.append(' ');
        }

        rebuilt.append(word, from, to);
    }

    /**
     * Builds the message for a script that mixes parameter styles. It names the styles that were actually
     * found, rather than only listing the three supported spellings. Conflicts involving positional markers
     * explain their SQL expression context, with an additional bracket-group note when a counted marker came
     * from a subscript. Recognized JSON and typed unary geometric operators are distinct from placeholders;
     * neighboring words alone do not distinguish them, and compact expressions may still begin with a binding.
     *
     * @param type the detected style bit flags
     * @param questionMarkFromSubscript whether a positional marker was read from inside a bracket group
     * @return the message for the {@code IllegalArgumentException} reporting the conflict
     */
    private String mixedParameterStyleMessage(final int type, final boolean questionMarkFromSubscript) {
        final List<String> found = new ArrayList<>(3);

        if ((type & QUESTION_MARK_TYPE) != 0) {
            found.add("positional '?'");
        }

        if ((type & NAMED_PARAMETER_TYPE) != 0) {
            found.add("named ':propName'");
        }

        if ((type & IBATIS_PARAMETER_TYPE) != 0) {
            found.add("iBatis/MyBatis '#{propName}'");
        }

        final StringBuilder msg = new StringBuilder(256);

        msg.append("Cannot mix parameter styles ('?', ':propName', '#{propName}') in the same SQL script; found ")
                .append(String.join(" and ", found))
                .append('.');

        if (questionMarkFromSubscript) {
            msg.append(" Positional placeholders inside a bracket group are counted in their SQL expression context.");
        }

        // These rules also apply outside brackets. Use the detected styles so named/MyBatis-only
        // conflicts avoid irrelevant positional guidance, without collecting any extra classifier state.
        if ((type & QUESTION_MARK_TYPE) != 0) {
            msg.append(" Recognized PostgreSQL JSON existence operators ('?', '?|', '?&') and typed unary geometric operators ('?-', '?|')")
                    .append(" are not parameters. Value placeholders before SQL/JSON constructor clauses")
                    .append(" (NULL ON NULL, ABSENT ON NULL, FORMAT JSON, WITH/WITHOUT UNIQUE KEYS) are still parameters.")
                    .append(" In compact expressions such as ?-1 and ?||'x', the leading '?' is a positional parameter.");
        }

        msg.append(" SQL: ").append(sql);

        return msg.toString();
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
     *       whitespace inside the braces is tolerated, e.g. {@code #{ userName }}; the name ends at the
     *       first comma or colon, and the MyBatis {@code :jdbcType} shorthand and attributes after it are
     *       discarded, so {@code #{ id, jdbcType=BIGINT }} and {@code #{id:BIGINT}} bind {@code id})</li>
     *   <li>Standard JDBC placeholders ({@code ?})</li>
     * </ul>
     *
     * <p>Mixing detected parameter styles in the same SQL statement results in an
     * {@code IllegalArgumentException}. The class-level documentation describes the supported
     * operator contexts and the treatment of ambiguous bracket quoting.</p>
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
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank (including SQL made up only of
     *         characters that {@link String#trim()} removes, such as control characters); or if parameter detection
     *         in a recognized data-operation statement finds mixed styles ({@code ?}, {@code :propName},
     *         {@code #{propName}}), an iBatis/MyBatis parameter missing its closing brace or with an empty property
     *         name before its {@code ':'} or {@code ','} ({@code #{:id}}), an unpaired UTF-16
     *         surrogate in or immediately before a prospective colon-style name, or an unpaired surrogate
     *         directly after a {@code '?'} opening the content of a standalone bracket group
     */
    public static ParsedSql parse(final String sql) {
        N.checkArgument(!Strings.isBlank(sql), "sql must not be null, empty, or blank");

        final String normalizedSql = sql.trim();
        // String.trim() also strips control characters (U+0000..U+001F) that Strings.isBlank does not treat as blank.
        N.checkArgument(!normalizedSql.isEmpty(), "sql must not be null, empty, or blank");

        return parse(normalizedSql, pool);
    }

    /**
     * Creates the parse cache, or returns {@code null} if it cannot be created, as happens once the JVM has begun
     * shutting down. Creating a pool registers JVM shutdown hooks and schedules its evictor on a shared executor that
     * the JVM stops at shutdown, so a pool created during shutdown fails in several ways: an
     * {@code ExceptionInInitializerError} (or later {@code NoClassDefFoundError}) when the pool classes are first
     * initialized and the runtime refuses their shutdown hook, or a {@code RejectedExecutionException} when they are
     * already initialized and the executor has terminated. Letting any of these escape the static initializer would make
     * this class unusable for the rest of the JVM's life ({@code NoClassDefFoundError}) when its first use happens
     * inside a shutdown hook. The cache is only an optimization, so whatever {@code factory} throws leaves it absent
     * and every parse runs uncached instead.
     *
     * @param factory creates the pool
     * @return the new pool, or {@code null} if {@code factory} threw
     */
    static KeyedObjectPool<String, PoolableAdapter<ParsedSql>> createCache(final Supplier<KeyedObjectPool<String, PoolableAdapter<ParsedSql>>> factory) {
        try {
            return factory.get();
        } catch (final Throwable e) { // NOSONAR - no failure to build an optional cache may fail this class's static initialization
            return null;
        }
    }

    /**
     * Returns the instance cached in {@code cache} for an already validated and trimmed SQL string, parsing and
     * caching it on a miss.
     *
     * <p>A closed cache is a permanent miss rather than an error. The pool registers its own JVM shutdown hook
     * that closes it, after which its {@code get}/{@code put} throw {@code IllegalStateException}; shutdown hooks
     * run concurrently, so code that still parses SQL while the JVM stops (another shutdown hook, a
     * {@code @PreDestroy} flush, a graceful request drain) must keep working, merely without caching. A cache
     * that could not be created at all, typically because shutdown had already begun when this class was initialized
     * ({@code cache} is {@code null}, see {@link #createCache(Supplier)}), is a permanent miss as well.</p>
     *
     * @param normalizedSql the nonblank SQL, already trimmed by {@link String#trim()}
     * @param cache the cache to consult and fill, or {@code null} to parse without caching
     * @return the cached instance, or a newly parsed one
     * @throws IllegalArgumentException if the SQL is rejected by parameter detection (see {@link #parse(String)})
     */
    static ParsedSql parse(final String normalizedSql, final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache) {
        if (cache == null) {
            return new ParsedSql(normalizedSql);
        }

        ParsedSql result = cachedValue(normalizedSql, cache);

        if (result != null) {
            return result;
        }

        // Tokenization is expensive, so construct outside the lock to avoid serializing concurrent
        // first-touch parses of unrelated SQL strings. ParsedSql is immutable and value-equal, so a
        // racing thread may build a duplicate; the pooled winner is returned in that case and the
        // loser's instance is simply discarded. Its own exceptions propagate: only cache access is guarded.
        final ParsedSql parsed = new ParsedSql(normalizedSql);

        if (cache.isClosed()) {
            return parsed;
        }

        synchronized (cache) {
            result = cachedValue(normalizedSql, cache);

            if (result != null) {
                return result;
            }

            try {
                cache.put(normalizedSql, Poolable.wrap(parsed, LIVE_TIME, MAX_IDLE_TIME));
            } catch (final IllegalStateException e) {
                // Closed concurrently (JVM shutdown): return the parsed instance uncached.
            }
        }

        return parsed;
    }

    /** Returns the live cached instance, or {@code null} on a miss, including a cache closed at JVM shutdown. */
    private static ParsedSql cachedValue(final String normalizedSql, final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache) {
        if (cache.isClosed()) {
            return null;
        }

        try {
            final PoolableAdapter<ParsedSql> w = cache.get(normalizedSql);
            return w == null ? null : w.value();
        } catch (final IllegalStateException e) {
            return null; // closed concurrently (JVM shutdown): treat as a miss
        }
    }

    /**
     * Returns the original SQL string (trimmed of leading and trailing whitespace as by {@link String#trim()}),
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
     * for recognized data-operation statements. Such a statement is rebuilt from its tokens, so its
     * comments are removed (see the class-level documentation) and each run of whitespace between
     * tokens is collapsed to a single space (quoted text and bracket groups keep theirs), except that a gap
     * containing a line break between a string literal and a following unprefixed {@code '...'} continuation
     * becomes one line break, since SQL concatenates adjacent literals only across a line break
     * ({@code 'a'\n'b'}). A {@code ?} converted
     * from a named or iBatis marker that would otherwise pair with an adjacent {@code ?} is separated from it by
     * one space ({@code payload?:key} becomes {@code payload? ?}, {@code :a:b} becomes {@code ? ?},
     * {@code #{a}?|x} becomes {@code ? ?|x}), so the output never forms a pgJDBC {@code ??} escape absent from
     * the source. For any other SQL, no parameter conversion is
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
     * Repeated names are retained once per binding occurrence, so {@code :id OR :id} contributes
     * two entries named {@code id}, corresponding to the two generated JDBC placeholders.
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
     * but excludes tokens recognized as PostgreSQL JSON-existence operators or explicitly typed unary
     * geometric operators ({@code ?-}, {@code ?|}). A JSON-existence operator's right operand may be a
     * literal, placeholder, column, or function expression. Adjacent question marks in the pgJDBC
     * operator escape {@code ??} (also {@code ??|}, {@code ??&}, and {@code @??}) contribute no parameters;
     * any following placeholder is still counted, so {@code doc ?? ?} has one parameter.
     * SQL ordering/pagination and window-frame offset placeholders ({@code ROWS|RANGE|GROUPS ? PRECEDING}) remain
     * ordinary JDBC parameters, and so does a {@code ?}
     * that directly follows a SQL operator or a value-taking keyword such as {@code INTERVAL},
     * {@code ILIKE}, {@code SIMILAR TO} or {@code ESCAPE} (for example {@code INTERVAL ? DAY}).
     * Parameters are only counted for recognized data operation statements (see the class-level
     * documentation); for other SQL this returns {@code 0}.
     *
     * <p>A value placeholder in a SQL/JSON constructor remains counted before {@code NULL ON NULL},
     * {@code ABSENT ON NULL}, {@code FORMAT JSON}, or {@code WITH}/{@code WITHOUT UNIQUE [KEYS]}. For example,
     * {@code SELECT JSON_OBJECT('k' VALUE ? NULL ON NULL)} contains one parameter, whereas
     * {@code SELECT ?- lseg '(0,0),(1,0)'} contains none. See the class-level documentation for the
     * supported operator contexts and the treatment of ambiguous bracket quoting.
     * Constructor option words inside a recognized {@code JSON_ARRAY} query body
     * retain their ordinary SQL meaning; {@code SELECT JSON_ARRAY(SELECT payload ? format JSON FROM t)}
     * contains no parameters.</p>
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
     *         {@link #parameterizedSql()} when an operator or quoted text such as {@code [what?]} contains a question mark
     */
    public int parameterCount() {
        return parameterCount;
    }

    /**
     * Returns whether the SQL is recognized as a data operation statement (see the class-level documentation),
     * the only kind of statement whose parameter markers are detected and converted. For any other SQL,
     * {@link #parameterCount()} is {@code 0} and {@link #namedParameters()} is empty without the text having been
     * inspected, so a {@code ?} in it may still be read as a JDBC placeholder by a driver.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsedSql.parse("SELECT * FROM users WHERE id = ?").isDataOperation();   // true
     * ParsedSql.parse("TABLE users").isDataOperation();                        // true
     * ParsedSql.parse("SET search_path TO app").isDataOperation();             // false
     * }</pre>
     *
     * @return {@code true} if parameter detection was applied to this SQL
     */
    public boolean isDataOperation() {
        return dataOperation;
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
     * If classification counted every question mark in the source, their literal offsets are already
     * the answer. Any extra mark (operator, quoted text, metadata or comment) forces token alignment.
     * This is a consequence of classification, not a competing rule for deciding what binds.
     */
    private static int[] allQuestionMarkOffsets(final String sql, final int count) {
        final int[] offsets = new int[count];
        // Dense binding lists benefit from one character loop instead of many tiny indexOf calls.
        if (count > sql.length() / 4) {
            int marker = 0;
            for (int index = 0, len = sql.length(); index < len; index++) {
                if (sql.charAt(index) == '?') {
                    if (marker == count) {
                        return null;
                    }
                    offsets[marker++] = index;
                }
            }
            return marker == count ? offsets : null;
        }
        int index = -1;
        for (int i = 0; i < count; i++) {
            index = sql.indexOf('?', index + 1);
            if (index < 0) {
                return null;
            }
            offsets[i] = index;
        }
        return sql.indexOf('?', index + 1) < 0 ? offsets : null;
    }

    /**
     * Maps each counted positional marker, recorded as (token index, offset within the token), to its offset in
     * {@code sql}. The tokenizer drops comments and collapses whitespace runs, so the token stream is walked
     * alongside the original text: whitespace and comments are skipped, then each non-blank token must be found
     * verbatim at the cursor. A null token-offset list denotes zero offsets (ordinary markers).
     * Returns {@code null} if a token cannot be located (not expected for tokenizer output, guarded defensively).
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
        return resolvePositionalParameterOffsets(sql, words, questionMarkTokenIndexes, questionMarkTokenOffsets, null, questionMarkTokenIndexes.size());
    }

    /** Aligns ordinary token positions and sparse bracket-offset arrays in the same source walk. */
    private static int[] resolvePositionalParameterOffsets(final String sql, final List<String> words, final IntList questionMarkTokenIndexes,
            final IntList questionMarkTokenOffsets, final int[][] subscriptOffsets, final int markerCount) {
        final int ordinaryCount = questionMarkTokenIndexes == null ? 0 : questionMarkTokenIndexes.size();
        int ordinary = 0;
        final int[] offsets = new int[markerCount];
        int cursor = 0;
        int marker = 0;

        for (int i = 0, size = words.size(); i < size && marker < markerCount; i++) {
            final String word = words.get(i);

            // Only the tokenizer's own whitespace token (a collapsed run rendered as one space) is skipped
            // here, and only the characters the tokenizer treats as whitespace are skipped in the source: a
            // Java-whitespace character it does not recognize (vertical tab, U+2000-200A, ...) stays inside
            // its token and must be matched verbatim, or the token would never be found at the cursor.
            if (word.isEmpty() || " ".equals(word)) {
                continue;
            }

            cursor = locateToken(sql, word, cursor);

            if (cursor < 0) {
                return null; // NOSONAR - documented sentinel, checked by positionalParameterOffsets()
            }

            if (subscriptOffsets != null && subscriptOffsets[i] != null) {
                for (final int offset : subscriptOffsets[i]) {
                    offsets[marker++] = cursor + offset;
                }
            }
            while (ordinary < ordinaryCount && questionMarkTokenIndexes.get(ordinary) == i) {
                offsets[marker++] = cursor + (questionMarkTokenOffsets == null ? 0 : questionMarkTokenOffsets.get(ordinary));
                ordinary++;
            }

            cursor += word.length();
        }

        return marker == markerCount ? offsets : null;
    }

    /**
     * Finds where the non-blank token {@code word} starts in {@code sql} at or after {@code cursor}, skipping the
     * whitespace and the discarded comments the tokenizer dropped before it (see
     * {@link #resolvePositionalParameterOffsets(String, List, IntList, IntList)}).
     *
     * @return the token's offset in {@code sql}, or {@code -1} if it cannot be located
     */
    private static int locateToken(final String sql, final String word, int cursor) {
        final int len = sql.length();

        while (true) {
            // A custom tokenizer separator may itself start with whitespace (" AND") and is emitted verbatim,
            // whitespace included: stop skipping where the token starts, or it can never match at the cursor.
            while (cursor < len && SqlParser.isTokenWhitespace(sql.charAt(cursor)) && !sql.startsWith(word, cursor)) {
                cursor++;
            }

            // A token may only be matched at the cursor when the cursor is not the start of a comment the
            // tokenizer discarded: "/" and "-" are prefixes of the "/*" and "--" openers, so matching them
            // first would skip the comment skipping below and resolve the markers inside the comment. A
            // comment kept as a token starts with an opener itself, so it still matches here.
            if (sql.startsWith(word, cursor) && (!startsWithCommentOpener(sql, cursor) || startsWithCommentOpener(word, 0))) {
                return cursor;
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
                return sql.indexOf(word, cursor);
            }
        }
    }

    /**
     * Returns the offset in {@code sql} of every non-blank token ({@code -1} for the collapsed whitespace
     * tokens), or {@code null} if the token stream cannot be aligned with the text (not expected).
     */
    private static int[] alignTokenSourceOffsets(final String sql, final List<String> words) {
        final int[] offsets = new int[words.size()];
        int cursor = 0;

        for (int i = 0, size = words.size(); i < size; i++) {
            final String word = words.get(i);

            if (word.isEmpty() || " ".equals(word)) {
                offsets[i] = -1;
                continue;
            }

            cursor = locateToken(sql, word, cursor);

            if (cursor < 0) {
                return null; // NOSONAR - the caller then keeps the collapsed form
            }

            offsets[i] = cursor;
            cursor += word.length();
        }

        return offsets;
    }

    private static boolean containsLineBreak(final String text, final int fromIndex, final int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            final char ch = text.charAt(i);

            if (ch == '\n' || ch == '\r') {
                return true;
            }
        }

        return false;
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

    /** Recognizes SQL statement prefixes without a temporary upper-cased string or a set iterator. */
    private static boolean isOpSqlPrefixWord(final String word) {
        return switch (word.length()) {
            case 4 -> "WITH".equalsIgnoreCase(word) || "CALL".equalsIgnoreCase(word);
            case 5 -> "MERGE".equalsIgnoreCase(word) || "TABLE".equalsIgnoreCase(word); // TABLE t: the SQL-standard query shorthand
            case 6 -> "SELECT".equalsIgnoreCase(word) || "INSERT".equalsIgnoreCase(word) || "UPDATE".equalsIgnoreCase(word) || "DELETE".equalsIgnoreCase(word)
                    || "VALUES".equalsIgnoreCase(word);
            case 7 -> "EXPLAIN".equalsIgnoreCase(word) || "REPLACE".equalsIgnoreCase(word);
            default -> false;
        };
    }

    private static boolean isByteOrderMarkRun(final String word) {
        return !word.isEmpty() && stripLeadingByteOrderMarks(word).isEmpty();
    }

    private static String stripLeadingByteOrderMarks(final String word) {
        int index = 0;

        while (index < word.length() && word.charAt(index) == '\uFEFF') {
            index++;
        }

        return index == 0 ? word : word.substring(index);
    }

    private static String resolveFirstOpWord(final List<String> words) {
        int firstIndex = nextNonCommentWord(words, 0);

        // A leading byte-order mark (U+FEFF, e.g. SQL read from a UTF-8 file) is not SQL text, but neither String.trim()
        // nor the tokenizer treats it as whitespace: it is a token of its own or glued to the first word. Classify the
        // statement by the word it precedes; the mark itself stays in the SQL text.
        while (firstIndex >= 0 && isByteOrderMarkRun(words.get(firstIndex))) {
            firstIndex = nextNonCommentWord(words, firstIndex + 1);
        }

        if (firstIndex < 0) {
            return null;
        }

        String opWord = stripLeadingByteOrderMarks(words.get(firstIndex));
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

        // The tokenizer keeps a quoted name or bracket group glued to the word before it, so "SELECT\"a\"",
        // "SELECT[a]" or "UPDATE`t`" arrive as one token whose verb must still be recognized.
        opWord = keywordPart(opWord);

        if ("EXPLAIN".equalsIgnoreCase(opWord)) {
            int explainedIndex = nextNonCommentWord(words, nextIndex);

            while (explainedIndex >= 0) {
                final String explainedOpWord = keywordPart(words.get(explainedIndex));

                if (Strings.isNotEmpty(explainedOpWord) && isOpSqlPrefixWord(explainedOpWord) && !"EXPLAIN".equalsIgnoreCase(explainedOpWord)) {
                    return explainedOpWord;
                }

                explainedIndex = nextNonCommentWord(words, explainedIndex + 1);
            }
        }

        return opWord;
    }

    /**
     * Returns the keyword part of a token: the text before its first quote character ({@code '}, {@code "},
     * {@code `}) or {@code '['} when that character is not the first one, otherwise the token itself. Same rule as
     * the condition classes' clause detection, so {@code SELECT"a"} reads as {@code SELECT} while a token that
     * starts quoted ({@code "SELECT"}, {@code [SELECT]}) is a quoted name, never a keyword.
     */
    private static String keywordPart(final String token) {
        for (int i = 0, len = token.length(); i < len; i++) {
            final char ch = token.charAt(i);

            if (ch == '\'' || ch == '"' || ch == '`' || ch == '[') {
                return i > 0 ? token.substring(0, i) : token;
            }
        }

        return token;
    }

    /**
     * Returns {@code "CALL"} when {@code opWord} (at the current statement head) introduces a JDBC
     * call escape, otherwise {@code null}. Recognizes a glued <code>{call</code> token and the
     * multi-token forms <code>{ call ...}</code> and <code>{&lt;slot&gt; = call ...}</code>, whose return-value
     * slot is a single marker: {@code ?}, {@code :name} or a MyBatis binding
     * (<code>{#{result, mode=OUT, jdbcType=INTEGER} = call f(#{param})}</code>), separate from or glued to the
     * opening brace.
     */
    private static String resolveJdbcCallOpWord(final String opWord, final List<String> words, final int nextIndex) {
        if (Strings.isEmpty(opWord) || opWord.charAt(0) != '{') {
            return null;
        }

        int idx;

        if (opWord.length() > 1) {
            // Glued form: the tokenizer emits "{call" / "{CALL", or a return slot glued to the brace
            // ("{:result", "{#{result,"), as one word.
            if ("CALL".equalsIgnoreCase(keywordPart(opWord.substring(1)))) {
                return "CALL";
            }

            idx = jdbcCallReturnSlotEnd(opWord.substring(1), words, nextIndex - 1);
        } else {
            idx = nextNonCommentWord(words, nextIndex);

            if (idx < 0) {
                return null;
            }

            if ("CALL".equalsIgnoreCase(keywordPart(words.get(idx)))) {
                return "CALL";
            }

            idx = jdbcCallReturnSlotEnd(words.get(idx), words, idx);
        }

        // Return-parameter form: {<slot> = call ...}
        idx = idx < 0 ? -1 : nextNonCommentWord(words, idx + 1);

        if (idx < 0 || !"=".equals(words.get(idx))) {
            return null;
        }

        idx = nextNonCommentWord(words, idx + 1);

        return idx >= 0 && "CALL".equalsIgnoreCase(keywordPart(words.get(idx))) ? "CALL" : null;
    }

    /**
     * Returns the index of the last token of the return-value marker of a JDBC call escape, or {@code -1} if
     * {@code slot} (the text of the token at {@code index}, without a glued opening brace) does not start one.
     * A MyBatis binding may span tokens up to the one holding its {@code '}'}, which must end that token.
     */
    private static int jdbcCallReturnSlotEnd(final String slot, final List<String> words, final int index) {
        if (SK.QUESTION_MARK.equals(slot)) {
            return index;
        }

        if (slot.length() > 1 && slot.charAt(0) == _PREFIX_OF_NAMED_PARAMETER) {
            // Only the head is checked here: the constructor's own scan decides what the marker binds.
            return isNamedParameterIdentifierStart(slot.codePointAt(1)) ? index : -1;
        }

        if (!slot.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER)) {
            return -1;
        }

        String token = slot;

        for (int i = index;;) {
            final int closing = token.indexOf('}');

            if (closing >= 0) {
                return closing == token.length() - 1 ? i : -1;
            }

            if (++i >= words.size()) {
                return -1;
            }

            token = words.get(i);
        }
    }

    /**
     * Finds the end of a colon-style name, including its dot-separated segments.
     *
     * @param token the token containing the name
     * @param fromIndex the first character after the colon
     * @return the exclusive end of the name
     * @throws IllegalArgumentException if an unpaired UTF-16 surrogate is encountered while scanning
     *         the name or the first character after a segment separator
     */
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
     * @param sql the SQL text to inspect; must not be {@code null}
     * @return sorted bracket-opening offsets
     * @throws NullPointerException if {@code sql} is {@code null}
     * @throws IllegalArgumentException if a standalone bracket group in {@code sql} starts with a {@code ':'} or
     *         {@code '?'} marker followed by an unpaired UTF-16 surrogate
     * @throws IllegalStateException if the token stream cannot be aligned with the original SQL text
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
     * @param sql the original SQL text to inspect; must not be {@code null}
     * @param tokenizer the tokenizer configured for that SQL; unused and may be {@code null} when
     *        {@code sql} contains no {@code '['} character
     * @return sorted original-text offsets, or an empty array when no subscript is recognized
     * @throws NullPointerException if {@code sql} is {@code null}, or if {@code tokenizer} is {@code null} and
     *         {@code sql} contains a {@code '['}
     * @throws IllegalArgumentException if a standalone bracket group in {@code sql} starts with a {@code ':'} or
     *         {@code '?'} marker followed by an unpaired UTF-16 surrogate
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

            // A recognized parameter subscript is exactly what isQuotedToken excludes -- for a standalone
            // group it is defined as !isParameterSubscriptToken, and for a glued or dot-qualified bracket
            // the two are complements -- so !isQuotedToken already covers it and needs no separate test.
            if (word.indexOf('[') < 0 || isCommentOrSpaceToken(word) || !(chainedSubscripts[i] || !isQuotedToken(word))) {
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

    /**
     * Checks whether a token can contain a colon-style binding.
     *
     * @throws IllegalArgumentException if a non-chained standalone bracket token containing a colon has
     *         {@code ':'} or {@code '?'} as its first non-whitespace content, followed by an unpaired UTF-16 surrogate
     */
    private static boolean mayContainNamedParameter(final String token, final boolean chainedSubscript) {
        return token.length() >= 2 && token.indexOf(_PREFIX_OF_NAMED_PARAMETER) >= 0
                && (chainedSubscript || !isQuotedToken(token) || hasQuotedCastSuffix(token)) && !isCommentOrSpaceToken(token);
    }

    /**
     * Keeps the MyBatis opener intact when longest-match tokenization borrows its '#' for a preceding
     * question mark ({@code ?#{name}}, {@code ??#{name}}, {@code @??#{name}}). Moving that character to the next token preserves the exact SQL,
     * so source offsets still follow from the resulting token lengths. The copy is lazy and the scan is linear.
     * Legitimate {@code ?#} operators without an immediately adjacent '{' are left unchanged.
     *
     * @param tokens the original tokenizer output, including whitespace and comments
     * @return the original list, or a corrected copy if a MyBatis opener crossed that token boundary
     */
    private static List<String> restoreEscapedIbatisOpeners(final List<String> tokens) {
        List<String> result = tokens;

        for (int i = 0, size = tokens.size(); i + 1 < size; i++) {
            if (tokens.get(i).equals("?#") && tokens.get(i + 1).startsWith("{")) {
                if (result == tokens) {
                    result = new ArrayList<>(tokens);
                }

                result.set(i, SK.QUESTION_MARK);
                result.set(i + 1, "#" + tokens.get(i + 1));
            }
        }

        return result;
    }

    /**
     * Checks whether a token can contain an iBatis/MyBatis binding.
     *
     * @throws IllegalArgumentException if a non-chained standalone bracket token containing an iBatis/MyBatis opener
     *         has {@code ':'} or {@code '?'} as its first non-whitespace content, followed by an unpaired UTF-16 surrogate
     */
    private static boolean mayContainIbatisParameter(final String token, final boolean chainedSubscript) {
        // The minimum length of 2 admits the standalone "#{" token the tokenizer emits when
        // whitespace immediately follows the opener (e.g. "#{ id }"); the marker-assembly loop
        // in the constructor then joins subsequent tokens until the closing '}' is found. Any
        // 2-char token containing LEFT_OF_IBATIS_NAMED_PARAMETER is exactly "#{".
        // A chained subscript ("x[#{a}]['c', #{b}]") bypasses the bracket-quoted-identifier check:
        // the marker scanner skips quoted regions itself, so literal elements stay literal.
        return token.length() >= 2 && token.indexOf(LEFT_OF_IBATIS_NAMED_PARAMETER) >= 0
                && (chainedSubscript || !isQuotedToken(token) || hasQuotedCastSuffix(token)) && !isCommentOrSpaceToken(token);
    }

    /** A quoted cast type does not quote its preceding binding; the marker scanners still skip the type's quoted contents. */
    private static boolean hasQuotedCastSuffix(final String token) {
        final int cast = token.indexOf("::");
        return cast >= 0 && token.indexOf('"') > cast && token.indexOf('[') < 0 && !precededByQuote(token, cast, '\'') && !precededByQuote(token, cast, '`');
    }

    /**
     * Distinguishes quoted SQL tokens from parameter-bearing subscripts.
     *
     * @throws IllegalArgumentException if a standalone bracket token has {@code ':'} or {@code '?'} as its first
     *         non-whitespace content, followed by an unpaired UTF-16 surrogate
     */
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

        // A token that starts with '[' is a SQL Server bracket-quoted identifier unless its first
        // non-whitespace content is a named or positional binding. Whitespace before an array bracket
        // makes the tokenizer emit the group as its own token ("array [:ids]" or "array [?, :id]").
        // Inspect every parameter style in a recognized subscript, including the mixed-style guard.
        // The caller identifies chained groups separately with markChainedSubscriptTokens.
        return !isParameterSubscriptToken(token);
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

        int count = 0;

        for (final int index : hashIndexes) {
            if (index + 1 < token.length() && token.charAt(index + 1) == '{') {
                // The scanner owns this fresh array. Compact it in place instead of allocating
                // another growable list, especially for a large ARRAY of MyBatis bindings.
                hashIndexes[count++] = index;
            }
        }

        return count == hashIndexes.length ? hashIndexes : count == 0 ? N.EMPTY_INT_ARRAY : Arrays.copyOf(hashIndexes, count);
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
     * <p>When scanning markers inside bracket literals, the two readings can disagree on where a
     * literal such as {@code 'a\''} ends. {@link #findUnambiguousUnquotedMarkerIndexes(String, int, char)}
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
        // Both quote interpretations are identical without a backslash. Keep the second reading
        // for every potentially ambiguous token, but avoid another scan/list for ordinary markers.
        if (token.indexOf('\\', fromIndex) < 0) {
            return withBackslashEscapes;
        }
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
     * Returns {@code true} if the token is a PostgreSQL-style subscript that may hold
     * parameters: an identifier glued to a bracket region ({@code "arr[?]"}, {@code "ARRAY[?, ?]"}) or a
     * standalone bracket region whose content starts with a named binding or a lone {@code '?'}
     * ({@code "[:id]"}, {@code "[?]"}, {@code "[?, ?]"}). All marker styles must be inspected in such
     * a region so mixing positional and named markers cannot bypass validation.
     * A bracket that sits inside a quoted literal or quoted identifier ({@code "'$.items[*] ? (...)'"},
     * {@code "N'a[?]'"}, {@code "\"col[?]\""}), one that follows a qualification dot ({@code "t.[what?]"}),
     * and a standalone bracket-quoted identifier that merely contains {@code '?'} ({@code "[what?]"},
     * {@code "[?foo]"}) do not qualify.
     *
     * @throws IllegalArgumentException if a standalone bracket token has {@code ':'} or {@code '?'} as its first
     *         non-whitespace content, followed by an unpaired UTF-16 surrogate
     */
    private static boolean isParameterSubscriptToken(final String token) {
        final int bracketIndex = token.indexOf('[');

        if (bracketIndex < 0 || isCommentOrSpaceToken(token) || precededByQuote(token, bracketIndex, '\'') || precededByQuote(token, bracketIndex, '"')
                || precededByQuote(token, bracketIndex, '`')) {
            return false;
        }

        if (bracketIndex > 0) {
            return token.charAt(bracketIndex - 1) != '.';
        }

        if (isNamedParameterSubscript(token, bracketIndex)) {
            return true;
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

    private static boolean precededByQuote(final String token, final int boundaryIndex, final char quote) {
        // Only the prefix can affect the boundary's role. Searching the full token repeatedly
        // rescans arbitrarily large array contents even when the prefix is just "ARRAY".
        for (int index = 0; index < boundaryIndex; index++) {
            if (token.charAt(index) == quote) {
                return true;
            }
        }
        return false;
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
     *
     * @throws IllegalArgumentException if the first non-whitespace character after the bracket is a colon followed by
     *         an unpaired UTF-16 surrogate
     */
    private static boolean isNamedParameterSubscript(final String token, final int bracketIndex) {
        final int len = token.length();
        int index = bracketIndex + 1;

        // Mirror isParameterSubscriptToken: "[ :ids ]" binds exactly like "[ ? ]" does.
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
     * starts a parameter is then decided per marker by {@link #isNamedParameterStart(String, int, int, boolean)},
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
     *
     * @param tokenFollowsClosedValue whether the token directly continues a previous token that ends with a closing
     *        quote, parenthesis or bracket (see {@link #followsClosedValueToken(List, int, IntList)}), which then
     *        rejects a marker at index 0 like one glued to such a character inside the token
     * @throws IllegalArgumentException if the character after the prospective colon or the preceding name-boundary
     *         character is an unpaired UTF-16 surrogate
     */
    private static boolean isNamedParameterStart(final String token, final int index, final int fromIndex, final boolean tokenFollowsClosedValue) {
        return index + 1 < token.length() && isNamedParameterIdentifierStart(namedParameterCodePointAt(token, index + 1))
                && isNamedParameterStartBoundary(token, index, fromIndex, tokenFollowsClosedValue);
    }

    /**
     * Checks the character immediately before a prospective colon-style marker.
     *
     * <p>A colon glued to a closing quote, parenthesis or bracket is never a parameter position: it is the
     * key/value separator of a compact SQL Server 2022 / PostgreSQL 16 {@code JSON_OBJECT('key':value)}, or a
     * slice bound ({@code arr[f(x):n]}). The tokenizer ends a token at a closing quote, so in {@code 'k':col} the
     * {@code :col} starts a new token and the preceding character is checked through
     * {@code tokenFollowsClosedValue} instead.</p>
     *
     * @throws IllegalArgumentException if the marker is not at the scan boundary and its preceding character is an
     *         unpaired UTF-16 surrogate
     */
    private static boolean isNamedParameterStartBoundary(final String token, final int parameterStartIndex, final int fromIndex,
            final boolean tokenFollowsClosedValue) {
        if (parameterStartIndex == 0) {
            return !tokenFollowsClosedValue;
        }

        if (parameterStartIndex == fromIndex) {
            return true;
        }

        final int previousCodePoint = namedParameterCodePointBefore(token, parameterStartIndex);

        return previousCodePoint != _PREFIX_OF_NAMED_PARAMETER && previousCodePoint != '.' && !isClosedValueEnd(previousCodePoint)
                && !isNamedParameterIdentifierPart(previousCodePoint);
    }

    /** Returns {@code true} for a character that closes a quoted value or a parenthesized/bracketed group. */
    private static boolean isClosedValueEnd(final int ch) {
        return ch == '\'' || ch == '"' || ch == '`' || ch == ')' || ch == ']';
    }

    /**
     * Returns {@code true} if the token at {@code index} directly continues the previous token, with no whitespace
     * or comment between them, and that token ends with a closing quote, parenthesis or bracket. In the
     * tokenizer's output a gap is a token of its own; bracket-interior words carry no gap tokens, so their
     * {@code wordOffsets} decide adjacency instead.
     *
     * @param words the tokens or bracket-interior words
     * @param index the index of the token to check
     * @param wordOffsets the source offsets of bracket-interior {@code words}, or {@code null} for tokenizer output
     */
    private static boolean followsClosedValueToken(final List<String> words, final int index, final IntList wordOffsets) {
        if (index <= 0) {
            return false;
        }

        final String previous = words.get(index - 1);

        return !isCommentOrSpaceToken(previous) && isClosedValueEnd(previous.charAt(previous.length() - 1))
                && (wordOffsets == null || wordOffsets.get(index) == wordOffsets.get(index - 1) + previous.length());
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

    /**
     * Finds positional markers in a bracket token, excluding JSON operators and other multi-character SQL
     * operators. Both escape readings must agree on the unquoted marker positions and on their classification;
     * otherwise the token remains verbatim, like the other subscript marker scanners.
     *
     * <p>Lexical words and their source offsets are collected in a forward pass, then adjacent operands are
     * inspected once. No marker rescans a prefix or suffix of the expression, so compact sums such as
     * {@code ARRAY[?+?+...]} take linear time as well as comma-separated arrays. The preliminary marker
     * agreement check and the two classification readings are needed only when backslashes can change
     * quote boundaries. Plain comma-separated bindings need neither tokenization nor operand checks.</p>
     *
     * @throws IllegalArgumentException if operand classification encounters an unpaired UTF-16 surrogate immediately
     *         after a prospective colon-style marker
     */
    private static int[] findSubscriptPositionalParameterIndexes(final String token, final int fromIndex) {
        final int[] commaSeparated = commaSeparatedSubscriptBindings(token, fromIndex);
        if (commaSeparated != null) {
            return commaSeparated;
        }
        // The lexer itself skips quotes, comments and complete MyBatis bindings. If backslashes
        // cannot change any quote boundary, one classification also replaces the agreement probe.
        if (token.indexOf('\\', fromIndex) < 0) {
            return collectSubscriptPositionalParameterIndexes(token, fromIndex, true);
        }

        if (findUnquotedQuestionMarkIndexes(token, fromIndex).length == 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] withBackslashEscapes = collectSubscriptPositionalParameterIndexes(token, fromIndex, true);
        final int[] doubledQuoteOnly = collectSubscriptPositionalParameterIndexes(token, fromIndex, false);

        return Arrays.equals(withBackslashEscapes, doubledQuoteOnly) ? withBackslashEscapes : N.EMPTY_INT_ARRAY;
    }

    /**
     * A subscript containing only comma-separated '?' values has no operator or quoting ambiguity.
     * Stop at the first other character and let the shared classifier handle the entire expression.
     * In particular, never treat adjacent '??', nested groups or a trailing comma as this simple form.
     */
    private static int[] commaSeparatedSubscriptBindings(final String token, final int fromIndex) {
        int count = 0;
        boolean expectsValue = true;
        for (int index = fromIndex, len = token.length(); index < len; index++) {
            final char ch = token.charAt(index);
            if (expectsValue && ch == '?') {
                count++;
                expectsValue = false;
            } else if (!expectsValue && ch == ',') {
                expectsValue = true;
            } else if (!expectsValue && ch == ']' && index == len - 1) {
                // Validate before allocating: the successful scan gives an exact capacity and
                // proves that every '?' in the bracket interior is a binding.
                final int[] offsets = new int[count];
                for (int cursor = fromIndex, marker = 0; cursor < index; cursor++) {
                    if (token.charAt(cursor) == '?') {
                        offsets[marker++] = cursor;
                    }
                }
                return offsets;
            } else if (!Character.isWhitespace(ch)) {
                return null;
            }
        }
        return null;
    }

    /**
     * Scans a bracket interior using SqlParser's default separators and this class's shared quote rules.
     * SqlParser itself keeps brackets opaque and uses a single escape reading, so invoking its next-token
     * methods would neither expose these operands nor preserve the subscript ambiguity contract.
     *
     * @throws IllegalArgumentException if operand classification encounters an unpaired UTF-16 surrogate immediately
     *         after a prospective colon-style marker
     */
    private static int[] collectSubscriptPositionalParameterIndexes(final String token, final int fromIndex, final boolean backslashEscapes) {
        final List<String> words = new ArrayList<>();
        final IntList wordOffsets = new IntList();
        final int length = token.length();

        for (int index = fromIndex; index < length;) {
            final char ch = token.charAt(index);

            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            } else if (ch == '/' && index + 1 < length && token.charAt(index + 1) == '*') {
                final int end = token.indexOf("*/", index + 2);
                index = end < 0 ? length : end + 2;
                continue;
            } else if (ch == '-' && index + 1 < length && token.charAt(index + 1) == '-') {
                while (index < length && token.charAt(index) != '\n' && token.charAt(index) != '\r') {
                    index++;
                }
                continue;
            }

            final int start = index;

            if (isQuoteChar(ch)) {
                index = Math.min(length, skipQuotedRegion(token, index, backslashEscapes) + 1);
            } else if (ch == '#' && index + 1 < length && token.charAt(index + 1) == '{') {
                // MyBatis options may contain commas and spaces; the complete binding is one operand.
                final int end = token.indexOf('}', index + 2);
                index = end < 0 ? length : end + 1;
            } else if (ch == '[' || ch == ']') {
                index++;
            } else {
                // The # in ?#{...} starts a binding, not the ?# operator. Keep its opener intact
                // so metadata is consumed as one operand; otherwise option question marks/parentheses
                // leak into SQL classification, including after a pgJDBC ?? escape.
                final int separatorLength = ch == '?' && index + 2 < length && token.charAt(index + 1) == '#' && token.charAt(index + 2) == '{' ? 1
                        : subscriptSeparatorLength(token, index);

                if (separatorLength > 0) {
                    index += separatorLength;
                } else {
                    do {
                        index++;
                    } while (index < length && !Character.isWhitespace(token.charAt(index)) && !isQuoteChar(token.charAt(index)) && token.charAt(index) != '['
                            && token.charAt(index) != ']' && subscriptSeparatorLength(token, index) == 0);
                }
            }

            words.add(token.substring(start, index));
            wordOffsets.add(start);
        }

        final int[] indexes = new QuestionMarkClassifier(words, wordOffsets, null).classify();

        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = wordOffsets.get(indexes[i]);
        }

        return indexes;
    }

    /**
     * Classifies ordinary tokens and bracket-interior words with one forward operand state. A compact
     * {@code ?||} ends in an operator even when its first character is a binding; inspecting only its
     * spelling as the previous token loses the middle binding in {@code ?||?||?}.
     *
     * <p>Lookahead follows operand grouping and consecutive cast suffixes. Matching parentheses for explicitly typed
     * geometric operands and cast modifiers are indexed lazily, once, instead of searching a suffix for each marker. JSON
     * constructor scopes use an amortized constant-time stack. Scanning is linear in the SQL length and
     * auxiliary storage is linear in the token count; the ordinary no-question-mark path never creates this classifier.</p>
     */
    private static final class QuestionMarkClassifier {
        private final List<String> words;
        private final IntList wordOffsets;
        private int[] closingParentheses;

        /** Memoized {@link #containsNamedMarker()} result; {@code null} until first asked. */
        private Boolean namedMarkerPresent;
        /** The constructor's chained-subscript flags for {@link #words} (may be {@code null}), for marker detection. */
        private final boolean[] chainedSubscripts;

        private QuestionMarkClassifier(final List<String> words, final IntList wordOffsets, final boolean[] chainedSubscripts) {
            this.words = words;
            this.wordOffsets = wordOffsets;
            this.chainedSubscripts = chainedSubscripts;
        }

        /**
         * Returns the token at {@code index} (empty for {@code -1}), rejoining a Unicode-escape string literal
         * {@code U&'...'}, which the tokenizer splits into {@code U}, {@code &} and the quoted part.
         */
        private String operandWordAt(final int index) {
            if (index < 0) {
                return Strings.EMPTY;
            }

            final String word = words.get(index);

            if ("U".equalsIgnoreCase(word) && index + 2 < words.size() && words.get(index + 1).equals("&") && words.get(index + 2).startsWith("'")) {
                return word + '&' + words.get(index + 2);
            }

            return word;
        }

        /**
         * Returns {@code true} if the statement contains a named ({@code :name}) or MyBatis ({@code #{...}}) marker
         * anywhere, including inside a subscript ({@code keys[:idx]}); detected with the same token scanners the
         * constructor converts markers with. A statement cannot mix those with positional {@code ?} bindings, so where
         * a {@code ?} could be either a binding or a JSON operator, their presence settles it as the operator.
         * Computed once, only for such an ambiguous {@code ?}.
         *
         * @throws IllegalArgumentException if a prospective colon-style marker is followed by an unpaired UTF-16 surrogate
         */
        private boolean containsNamedMarker() {
            if (namedMarkerPresent == null) {
                boolean found = false;

                for (int i = 0, size = words.size(); i < size && !found; i++) {
                    found = containsNamedOrIbatisMarker(words.get(i), chainedSubscripts != null && chainedSubscripts[i],
                            followsClosedValueToken(words, i, wordOffsets));
                }

                namedMarkerPresent = found;
            }

            return namedMarkerPresent;
        }

        /**
         * Classifies positional markers while tracking SQL operand and constructor context.
         *
         * @throws IllegalArgumentException if a prospective colon-style operand marker, or the first non-whitespace
         *         {@code ':'} or {@code '?'} in a standalone bracket group, is followed by an unpaired UTF-16 surrogate
         */
        private int[] classify() {
            IntList indexes = null;
            boolean previousIsOperand = false;
            boolean previousOperandUnknown = false;
            String previousWord = Strings.EMPTY;
            // Only the contextual keyword check (AS OF TIMESTAMP ?, FIRST ? SKIP ?) needs the word before previousWord.
            String wordBeforePrevious = Strings.EMPTY;
            int depth = 0;
            int[] jsonDepths = null;
            // Parallel to jsonDepths: whether the constructor is JSON_OBJECT/JSON_OBJECTAGG, whose entries may open with KEY.
            boolean[] jsonObjectScopes = null;
            int jsonDepthCount = 0;

            for (int i = 0, size = words.size(); i < size; i++) {
                final String word = words.get(i);

                if (isCommentOrSpaceToken(word)) {
                    continue;
                }

                final int bindingEnd = splitIbatisBindingEnd(i);

                if (bindingEnd > i) {
                    // The outer tokenizer can split MyBatis options at spaces/operators. Their contents
                    // are metadata, so parentheses or question marks there cannot change SQL operand state.
                    i = bindingEnd;
                    previousIsOperand = true;
                    previousOperandUnknown = false;
                    wordBeforePrevious = previousWord;
                    previousWord = word;
                    continue;
                }

                if (word.equals("(") || word.equals("[")) {
                    depth++;

                    if (word.equals("(") && isSqlJsonConstructor(previousWord)) {
                        if (jsonDepths == null) {
                            jsonDepths = new int[4];
                            jsonObjectScopes = new boolean[4];
                        } else if (jsonDepthCount == jsonDepths.length) {
                            jsonDepths = Arrays.copyOf(jsonDepths, jsonDepthCount * 2);
                            jsonObjectScopes = Arrays.copyOf(jsonObjectScopes, jsonDepthCount * 2);
                        }

                        // A negative depth marks JSON_ARRAY's query form. FORMAT JSON in its SELECT
                        // list can be an operand and alias, while a nested constructor gets a new scope.
                        final boolean query = "JSON_ARRAY".equalsIgnoreCase(previousWord) && startsJsonArrayQuery(i);
                        jsonObjectScopes[jsonDepthCount] = "JSON_OBJECT".equalsIgnoreCase(previousWord) || "JSON_OBJECTAGG".equalsIgnoreCase(previousWord);
                        jsonDepths[jsonDepthCount++] = query ? -depth : depth;
                    }
                } else if (word.equals(")") || word.equals("]")) {
                    if (jsonDepthCount > 0 && Math.abs(jsonDepths[jsonDepthCount - 1]) == depth) {
                        jsonDepthCount--;
                    }

                    depth = Math.max(0, depth - 1);
                }

                final boolean bareMarker = word.equals(SK.QUESTION_MARK);
                final boolean compactMarker = isCompactQuestionMarkOperator(word);

                // pgJDBC escapes an operator question mark as ??. The first token can end in @? and
                // the second can include a suffix (?|), so consume the pair and expect an operand.
                // Ordinary tokens retain gaps; bracket words omit them and must use their source offsets
                // to keep ? ? and ?/* comment */? distinct from an adjacent escape. No rescan is needed.
                if ((bareMarker || word.equals("@?")) && i + 1 < size && words.get(i + 1).startsWith(SK.QUESTION_MARK)
                        && (wordOffsets == null || wordOffsets.get(i + 1) == wordOffsets.get(i) + word.length())) {
                    wordBeforePrevious = previousWord;
                    previousWord = words.get(++i);
                    previousIsOperand = false;
                    previousOperandUnknown = false;
                    continue;
                }

                if (bareMarker || compactMarker) {
                    // Only a question-mark token needs its predecessor's operand classification.
                    // Defer the keyword checks across ordinary SQL words, and reject complete
                    // separators first (e.g. '=' before a binding). Explicit binding/escape state
                    // above remains authoritative; comments and whitespace never replace it.
                    final int next = nextNonCommentWord(words, i + 1);

                    if (previousOperandUnknown) {
                        final boolean jsonObjectScope = jsonDepthCount > 0 && jsonDepths[jsonDepthCount - 1] == depth && jsonObjectScopes[jsonDepthCount - 1];
                        previousIsOperand = (previousWord.equals(")") || previousWord.equals("]")
                                || subscriptSeparatorLength(previousWord, 0) != previousWord.length()) && canPrecedeJsonQuestionOperator(previousWord)
                                && !isContextualPlaceholderKeyword(wordBeforePrevious, previousWord, operandWordAt(next), this::containsNamedMarker)
                                && !leadsMarkerAsKeyword(wordBeforePrevious, previousWord, word, i, next, jsonObjectScope);
                        previousOperandUnknown = false;
                    }
                    final boolean jsonClause = jsonDepthCount > 0 && jsonDepths[jsonDepthCount - 1] == depth && startsSqlJsonClause(next);
                    final boolean binaryOperator = previousIsOperand && next >= 0 && !jsonClause && canFollowJsonQuestionOperator(words.get(next));
                    final boolean unaryOperator = !previousIsOperand && (word.equals("?-") || word.equals("?|")) && isExplicitGeometricOperand(next);
                    final boolean placeholder = !binaryOperator && !unaryOperator;

                    if (placeholder) {
                        if (indexes == null) {
                            indexes = new IntList();
                        }

                        indexes.add(i);
                    }

                    // A bare binding finishes an operand. Every operator, including the operator suffix
                    // of a compact binding, expects another operand. This also distinguishes "? ? ?".
                    previousIsOperand = bareMarker && placeholder;
                } else {
                    previousOperandUnknown = true;
                }

                wordBeforePrevious = previousWord;
                previousWord = word;
            }

            return indexes == null ? N.EMPTY_INT_ARRAY : indexes.toArray();
        }

        /**
         * Distinguishes a query expression from a value list whose first value is a scalar subquery.
         *
         * @throws IllegalArgumentException if parenthesis indexing encounters a split MyBatis token whose standalone
         *         bracket group starts with a colon or question mark followed by an unpaired UTF-16 surrogate
         */
        private boolean startsJsonArrayQuery(final int opening) {
            int first = nextNonCommentWord(words, opening + 1);

            if (first >= 0 && words.get(first).equals("(")) {
                final int close = closingParenthesis(first);
                final int after = close > first ? nextNonCommentWord(words, close + 1) : -1;

                // A comma, arithmetic operator, or value-constructor option after the scalar subquery
                // keeps the enclosing constructor in its value form. Set operations extend the query.
                if (after < 0 || !(words.get(after).equals(")") || "UNION".equalsIgnoreCase(words.get(after)) || "INTERSECT".equalsIgnoreCase(words.get(after))
                        || "EXCEPT".equalsIgnoreCase(words.get(after)) || "ORDER".equalsIgnoreCase(words.get(after))
                        || "LIMIT".equalsIgnoreCase(words.get(after)) || "OFFSET".equalsIgnoreCase(words.get(after))
                        || "FETCH".equalsIgnoreCase(words.get(after)) || "RETURNING".equalsIgnoreCase(words.get(after)))) {
                    return false;
                }

                do {
                    first = nextNonCommentWord(words, first + 1);
                } while (first >= 0 && words.get(first).equals("("));
            }

            return first >= 0 && ("SELECT".equalsIgnoreCase(words.get(first)) || "WITH".equalsIgnoreCase(words.get(first))
                    || "VALUES".equalsIgnoreCase(words.get(first)) || "TABLE".equalsIgnoreCase(words.get(first)));
        }

        /** SQL/JSON phrases are clauses only in a constructor's value scope, never its query body or a global keyword ban. */
        private boolean startsSqlJsonClause(final int start) {
            if (start < 0) {
                return false;
            }

            final String word = words.get(start);

            // The key of the standard KEY ? VALUE ... form is recognized from its KEY (see leadsMarkerAsKeyword), not from
            // the VALUE after it: "doc ? value" in a constructor tests a column named value.
            if ("FORMAT".equalsIgnoreCase(word)) {
                final int next = nextNonCommentWord(words, start + 1);
                return next >= 0 && "JSON".equalsIgnoreCase(words.get(next));
            }

            if ("NULL".equalsIgnoreCase(word) || "ABSENT".equalsIgnoreCase(word)) {
                final int on = nextNonCommentWord(words, start + 1);
                final int last = on >= 0 && "ON".equalsIgnoreCase(words.get(on)) ? nextNonCommentWord(words, on + 1) : -1;
                return last >= 0 && "NULL".equalsIgnoreCase(words.get(last));
            }

            if ("WITH".equalsIgnoreCase(word) || "WITHOUT".equalsIgnoreCase(word)) {
                final int next = nextNonCommentWord(words, start + 1);
                return next >= 0 && "UNIQUE".equalsIgnoreCase(words.get(next)); // KEYS is optional.
            }

            return false;
        }

        /**
         * Returns {@code true} if {@code previousWord}, a keyword that is also a plausible column name, acts as the
         * keyword that leads the question-mark token {@code marker}, which is then a placeholder rather than an
         * operator applied to a column of that name. Unlike {@code isContextualPlaceholderKeyword}, these cases
         * depend on the marker's spelling, the words after it, or the constructor scope:
         * <ul>
         *   <li>The window-frame units {@code RANGE} and {@code GROUPS} lead a frame offset when the expression after
         *       the marker ends in {@code PRECEDING} or {@code FOLLOWING} ({@code RANGE ? PRECEDING},
         *       {@code GROUPS ?-1 FOLLOWING}), while {@code groups ? 'admin'} and {@code range ?& array['a']} test
         *       jsonb columns of those names.</li>
         *   <li>{@code VALUES} leads a marker glued to a non-JSON operator ({@code VALUES ?-1}, {@code VALUES ?||'x'}),
         *       which no column test can explain. The JSON operators {@code ?}, {@code ?|} and {@code ?&} after it test a
         *       column named {@code values} only where an expression operand stands, after a word such as {@code SELECT},
         *       {@code WHERE}, {@code ,} or {@code =}, after a qualification dot, after {@code IS [NOT] DISTINCT FROM},
         *       or in a JSON object's key/value entry (see {@code introducesColumnOperand}): {@code SELECT values ? format JSON}
         *       tests the key in column {@code format} and names the result {@code JSON}. Other contexts are read as
         *       the row constructor leading the marker ({@code VALUES ? FORMAT JSON}, {@code UNION ALL VALUES ?},
         *       {@code EXPLAIN VALUES ?}, {@code OVERRIDING SYSTEM VALUE VALUES ?}). After a {@code (}, which may open
         *       either, it leads a marker only before a postfix clause that no JSON operator takes as its operand
         *       ({@code (VALUES ? FORMAT JSON)}, {@code COLLATE}, {@code AT TIME ZONE}).</li>
         *   <li>A {@code KEY} that opens an entry (after the opening parenthesis or a comma) of a {@code JSON_OBJECT} or
         *       {@code JSON_OBJECTAGG} argument list leads the entry's key ({@code KEY ? VALUE ?},
         *       {@code KEY ?||'_x' VALUE ?}).</li>
         * </ul>
         *
         * @param wordBeforePrevious the non-comment word before {@code previousWord}
         * @param previousWord the non-comment word directly before the marker
         * @param marker the question-mark token ({@code ?} or a compact spelling such as {@code ?-})
         * @param markerIndex the index of {@code marker} in the words
         * @param next the index of the non-comment word after the marker, or {@code -1}
         * @param jsonObjectScope whether the marker sits directly in a {@code JSON_OBJECT}/{@code JSON_OBJECTAGG} argument list
         */
        private boolean leadsMarkerAsKeyword(final String wordBeforePrevious, final String previousWord, final String marker, final int markerIndex,
                final int next, final boolean jsonObjectScope) {
            if ("RANGE".equalsIgnoreCase(previousWord) || "GROUPS".equalsIgnoreCase(previousWord)) {
                return endsInFrameBoundDirection(next);
            }

            if ("VALUES".equalsIgnoreCase(previousWord)) {
                // Longest-match tokenization glues the operator after a bare VALUES row value ("VALUES ?-1") to the '?'.
                if (!marker.equals(SK.QUESTION_MARK) && !marker.equals("?|") && !marker.equals("?&")) {
                    return true;
                }

                // After '(' both readings occur: (VALUES ? FORMAT JSON) and WHERE (values ? 'k').
                return wordBeforePrevious.equals("(") ? startsValuePostfixClause(next)
                        : !introducesColumnOperand(wordBeforePrevious, markerIndex, jsonObjectScope);
            }

            return jsonObjectScope && "KEY".equalsIgnoreCase(previousWord) && (wordBeforePrevious.equals("(") || wordBeforePrevious.equals(","));
        }

        /**
         * Whether {@code wordBeforeValues}, the word before the {@code VALUES} that precedes the marker at
         * {@code markerIndex}, makes {@code values} an expression operand, a column: a {@code ,}, a {@code [}, an
         * operator, a qualification dot, a keyword that takes an expression after it ({@code SELECT}, {@code WHERE},
         * {@code AND}, {@code WHEN}, {@code BY}, ...), {@code IS [NOT] DISTINCT FROM}, or a {@code DISTINCT} or
         * {@code ALL} after {@code SELECT} or a {@code (}. Within a JSON object argument list, {@code KEY}, {@code VALUE}
         * and {@code :} also introduce operands. Restricting that reading to the constructor's scope preserves the row
         * constructor in {@code INSERT INTO t OVERRIDING SYSTEM VALUE VALUES ? FORMAT JSON}. Other contexts retain
         * the row-constructor reading, including a statement start, {@code UNION ALL VALUES}, {@code EXPLAIN} and
         * {@code INSERT INTO t}.
         */
        private boolean introducesColumnOperand(final String wordBeforeValues, final int markerIndex, final boolean jsonObjectScope) {
            if (wordBeforeValues.isEmpty()) {
                return false;
            }

            // A qualification dot may be its own token or glued to the qualifier: t . values and t. values.
            if (wordBeforeValues.equals(",") || wordBeforeValues.equals("[") || wordBeforeValues.endsWith(".") || isOperatorWord(wordBeforeValues)) {
                return true;
            }

            if (jsonObjectScope && (wordBeforeValues.equals(":") || "VALUE".equalsIgnoreCase(wordBeforeValues) || "KEY".equalsIgnoreCase(wordBeforeValues))) {
                return true;
            }

            if ("DISTINCT".equalsIgnoreCase(wordBeforeValues) || "ALL".equalsIgnoreCase(wordBeforeValues) || "FROM".equalsIgnoreCase(wordBeforeValues)) {
                // SELECT DISTINCT values ? 'k' and count(ALL values ? 'k') test a column; UNION ALL VALUES ? does not.
                final int values = previousNonCommentWord(words, markerIndex - 1);
                final int preceding = values > 0 ? previousNonCommentWord(words, values - 1) : -1;
                int before = preceding > 0 ? previousNonCommentWord(words, preceding - 1) : -1;

                if ("FROM".equalsIgnoreCase(wordBeforeValues)) {
                    // Only the predicate's FROM introduces an operand; FROM VALUES may introduce a row source.
                    if (before < 0 || !"DISTINCT".equalsIgnoreCase(words.get(before))) {
                        return false;
                    }

                    before = previousNonCommentWord(words, before - 1);

                    if (before >= 0 && "NOT".equalsIgnoreCase(words.get(before))) {
                        before = previousNonCommentWord(words, before - 1);
                    }

                    return before >= 0 && "IS".equalsIgnoreCase(words.get(before));
                }

                return before >= 0 && ("SELECT".equalsIgnoreCase(words.get(before)) || words.get(before).equals("("));
            }

            return switch (wordBeforeValues.length()) {
                case 2 -> "OR".equalsIgnoreCase(wordBeforeValues) || "ON".equalsIgnoreCase(wordBeforeValues) || "BY".equalsIgnoreCase(wordBeforeValues);
                case 3 -> "AND".equalsIgnoreCase(wordBeforeValues) || "NOT".equalsIgnoreCase(wordBeforeValues);
                case 4 -> "WHEN".equalsIgnoreCase(wordBeforeValues) || "THEN".equalsIgnoreCase(wordBeforeValues) || "ELSE".equalsIgnoreCase(wordBeforeValues)
                        || "CASE".equalsIgnoreCase(wordBeforeValues) || "LIKE".equalsIgnoreCase(wordBeforeValues);
                case 5 -> "WHERE".equalsIgnoreCase(wordBeforeValues) || "ILIKE".equalsIgnoreCase(wordBeforeValues);
                case 6 -> "SELECT".equalsIgnoreCase(wordBeforeValues) || "HAVING".equalsIgnoreCase(wordBeforeValues);
                case 7 -> "BETWEEN".equalsIgnoreCase(wordBeforeValues);
                case 9 -> "RETURNING".equalsIgnoreCase(wordBeforeValues);
                default -> false;
            };
        }

        /** Whether {@code word} consists of operator characters only ({@code =}, {@code <>}, {@code ||}, {@code @>}, ...). */
        private static boolean isOperatorWord(final String word) {
            for (int i = 0, len = word.length(); i < len; i++) {
                if ("=<>!+-*/%^&|~@".indexOf(word.charAt(i)) < 0) {
                    return false;
                }
            }

            return !word.isEmpty();
        }

        /**
         * Whether the words from {@code start} open a postfix clause of the value before them ({@code FORMAT JSON},
         * {@code COLLATE}, {@code AT TIME ZONE}), which is no operand of a JSON operator.
         */
        private boolean startsValuePostfixClause(final int start) {
            if (start < 0) {
                return false;
            }

            final String word = words.get(start);

            if ("COLLATE".equalsIgnoreCase(word)) {
                return true;
            }

            final int next = nextNonCommentWord(words, start + 1);

            return next >= 0 && ("FORMAT".equalsIgnoreCase(word) && "JSON".equalsIgnoreCase(words.get(next))
                    || "AT".equalsIgnoreCase(word) && "TIME".equalsIgnoreCase(words.get(next)));
        }

        /**
         * Returns {@code true} if the expression starting at {@code start} ends in {@code PRECEDING} or {@code FOLLOWING},
         * the shape of a window-frame offset ({@code ?-1 PRECEDING}, {@code ? * 2 FOLLOWING}). The scan stays inside the
         * expression: it gives up at a comma, a semicolon, the parenthesis or bracket closing the enclosing group, or a
         * boundary keyword ({@code AND}, {@code ORDER}, {@code ROWS}, ...) at the starting depth. It also gives up at any
         * further {@code RANGE}/{@code GROUPS}, where the next scan would begin, so the scans never overlap and their total
         * cost stays linear.
         */
        private boolean endsInFrameBoundDirection(final int start) {
            int nesting = 0;

            for (int i = start; i >= 0; i = nextNonCommentWord(words, i + 1)) {
                final String word = words.get(i);

                if ("RANGE".equalsIgnoreCase(word) || "GROUPS".equalsIgnoreCase(word)) {
                    return false;
                }

                if (word.equals("(") || word.equals("[")) {
                    nesting++;
                } else if (word.equals(")") || word.equals("]")) {
                    if (nesting-- == 0) {
                        return false;
                    }
                } else if (nesting == 0) {
                    if ("PRECEDING".equalsIgnoreCase(word) || "FOLLOWING".equalsIgnoreCase(word)) {
                        return true;
                    }

                    if (word.equals(",") || word.equals(";") || isSqlExpressionBoundaryWord(word)) {
                        return false;
                    }
                }
            }

            return false;
        }

        /**
         * Recognizes the unary geometric operators only when the operand explicitly identifies line/lseg
         * syntax. Untyped {@code ?-column} is ambiguous with a JDBC binding minus a column and stays a
         * binding; a type literal, constructor or cast resolves that ambiguity without schema inspection.
         *
         * @throws IllegalArgumentException if inspecting a split MyBatis binding encounters a standalone bracket
         *         group whose first non-whitespace {@code ':'} or {@code '?'} is followed by an unpaired UTF-16 surrogate
         */
        private boolean isExplicitGeometricOperand(int start) {
            int end = words.size();

            while (start >= 0 && start < end) {
                final String word = words.get(start);
                final int bindingEnd = splitIbatisBindingEnd(start);

                if (bindingEnd > start) {
                    final int after = nextNonCommentWord(words, bindingEnd + 1);
                    return isGeometricCast(bindingEnd, end) || (after >= 0 && after < end && isGeometricCast(after, end));
                }

                final int next = nextNonCommentWord(words, start + 1);

                // The bracket lexer splits a string prefix from its literal (E'...' into E and '...'), and SqlParser
                // splits U&'...' into U, & and the quoted part. Continue at the literal so a prefixed literal with a
                // cast (?- E'(0,0),(1,0)'::line) classifies alike inside and outside brackets.
                final int prefixedLiteral = splitStringPrefixLiteral(start, next, end);

                if (prefixedLiteral > start) {
                    start = prefixedLiteral;
                    continue;
                }

                if (word.equals("(")) {
                    final int close = closingParenthesis(start);

                    if (close <= start || close >= end) {
                        return false;
                    }

                    final int after = nextNonCommentWord(words, close + 1);

                    if (after >= 0 && after < end && words.get(after).startsWith("::")) {
                        return isGeometricCast(after, end);
                    }

                    end = close;
                    start = next;
                    continue; // Iterative unwrapping also handles deeply parenthesized operands safely.
                }

                if ("CAST".equalsIgnoreCase(word) && next >= 0 && next < end && words.get(next).equals("(")) {
                    final int close = closingParenthesis(next);
                    final int after = close > next && close < end ? nextNonCommentWord(words, close + 1) : -1;

                    if (after >= 0 && after < end && words.get(after).startsWith("::")) {
                        return isGeometricCast(after, end);
                    }

                    int type = close > next && close < end ? previousNonCommentWord(words, close - 1) : -1;

                    // A qualified type occupies at most three significant tokens: schema, dot, name.
                    // Bound the backward search so repeated CAST operands never rescan expression bodies.
                    for (int parts = 0; parts < 3 && type > next; parts++) {
                        final int as = previousNonCommentWord(words, type - 1);

                        if (as > next && "AS".equalsIgnoreCase(words.get(as))) {
                            final int typeEnd = geometricTypeEnd(type, 0, close);
                            return typeEnd >= 0 && nextNonCommentWord(words, typeEnd) == close;
                        }

                        type = as;
                    }

                    return false;
                }

                final int typeEnd = geometricTypeEnd(start, 0, end, true);

                if (typeEnd >= 0) {
                    if (words.get(typeEnd - 1).indexOf('\'') >= 0) {
                        // The lexer can keep an adjacent typed literal in the type token.
                        return hasGeometricFinalType(typeEnd - 1, end);
                    }

                    final int followingIndex = nextNonCommentWord(words, typeEnd);

                    if (followingIndex < 0 || followingIndex >= end) {
                        return false;
                    }

                    final String following = words.get(followingIndex);

                    if (isSqlStringLiteral(following)) {
                        return hasGeometricFinalType(followingIndex, end);
                    }

                    if (following.equals("(")) {
                        final int close = closingParenthesis(followingIndex);
                        return close > followingIndex && close < end && hasGeometricFinalType(close, end);
                    }

                    // The bracket lexer exposes a string prefix separately; SqlParser glues it to the
                    // quoted token. Accept the same typed literal in either representation.
                    final int literal = nextNonCommentWord(words, followingIndex + 1);

                    if ("E".equalsIgnoreCase(following) || "N".equalsIgnoreCase(following)) {
                        return literal >= 0 && literal < end && words.get(literal).startsWith("'") && hasGeometricFinalType(literal, end);
                    }

                    if ("U".equalsIgnoreCase(following) && literal >= 0 && literal < end && words.get(literal).equals("&")) {
                        final int quoted = nextNonCommentWord(words, literal + 1);
                        return quoted >= 0 && quoted < end && words.get(quoted).startsWith("'") && hasGeometricFinalType(quoted, end);
                    }

                    return false;
                }

                if (isGeometricCast(start, end)) {
                    return true;
                }

                if (next >= 0 && next < end) {
                    if (isGeometricCast(next, end)) {
                        return true;
                    }

                    if (words.get(next).equals("(")) {
                        final int close = closingParenthesis(next);
                        final int after = close > next && close < end ? nextNonCommentWord(words, close + 1) : -1;
                        return after >= 0 && after < end && isGeometricCast(after, end);
                    }
                }

                return false;
            }

            return false;
        }

        /**
         * Returns the index of the quoted part of a prefixed string literal ({@code E'...'}, {@code N'...'},
         * {@code U&'...'}) that a lexer split into words starting at {@code start}, or {@code -1}.
         * Like the outer operand checks, which find the literal after a separate prefix word with
         * {@code nextNonCommentWord}, this does not require the words to be adjacent.
         */
        private int splitStringPrefixLiteral(final int start, final int next, final int end) {
            final String word = words.get(start);
            int literal = next;

            if ("U".equalsIgnoreCase(word) && next >= 0 && next < end && words.get(next).equals("&")) {
                literal = nextNonCommentWord(words, next + 1);
            } else if (!"E".equalsIgnoreCase(word) && !"N".equalsIgnoreCase(word)) {
                return -1;
            }

            return literal >= 0 && literal < end && words.get(literal).startsWith("'") ? literal : -1;
        }

        /**
         * A geometric literal or constructor keeps its type unless an explicit trailing cast changes it.
         *
         * @throws IllegalArgumentException if inspecting a cast modifier encounters a split MyBatis token whose
         *         standalone bracket group starts with a colon or question mark followed by an unpaired UTF-16 surrogate
         */
        private boolean hasGeometricFinalType(final int operandEnd, final int end) {
            if (lastUnquotedCast(operandEnd) >= 0) {
                return isGeometricCast(operandEnd, end);
            }

            final int next = nextNonCommentWord(words, operandEnd + 1);
            return next < 0 || next >= end || !words.get(next).startsWith("::") || isGeometricCast(next, end);
        }

        /**
         * Recognizes the final cast type even when trivia, quoting, type modifiers, or standard multiword type names
         * split a chain across tokens.
         *
         * @throws IllegalArgumentException if indexing a cast modifier's parentheses encounters a split MyBatis token
         *         whose standalone bracket group starts with a colon or question mark followed by an unpaired UTF-16 surrogate
         */
        private boolean isGeometricCast(int index, final int end) {
            while (index >= 0 && index < end) {
                final int cast = lastUnquotedCast(index);

                if (cast < 0) {
                    return false;
                }

                final int geometricEnd = geometricTypeEnd(index, cast + 2, end);
                final int typeEnd = geometricEnd >= 0 ? geometricEnd : castTypeEnd(index, cast + 2, end);
                final int next = typeEnd >= 0 ? nextCastAfterType(typeEnd, end) : -1;

                if (next < 0) {
                    final int suffix = geometricEnd >= 0 ? nextNonCommentWord(words, geometricEnd) : -1;
                    return geometricEnd >= 0 && !(suffix >= 0 && suffix < end && (words.get(suffix).startsWith("[") || isArrayTypeKeyword(words.get(suffix))));
                }

                index = next; // The final cast, including casts split by trivia, determines the type.
            }

            return false;
        }

        /** Quoted type names and string literals can contain colons that are not cast delimiters. */
        private int lastUnquotedCast(final int index) {
            final String word = words.get(index);
            final int previous = word.startsWith("'") ? previousNonCommentWord(words, index - 1) : -1;
            // The bracket lexer separates E from its literal; SqlParser retains the prefix in the token.
            final boolean backslashEscapes = previous >= 0 && "E".equalsIgnoreCase(words.get(previous));
            int cast = -1;

            for (int offset = 0; offset < word.length(); offset++) {
                if (isQuoteChar(word.charAt(offset))) {
                    offset = skipQuotedRegion(word, offset, backslashEscapes);
                } else if (word.startsWith("::", offset)) {
                    cast = offset++;
                }
            }

            return cast;
        }

        /**
         * Locates the next cast after an identifier type, optional standard type words, type modifiers, and array dimensions.
         *
         * @throws IllegalArgumentException if indexing a type modifier's parentheses encounters a split MyBatis token
         *         whose standalone bracket group starts with a colon or question mark followed by an unpaired UTF-16 surrogate
         */
        private int nextCastAfterType(final int typeEnd, final int end) {
            final String type = words.get(typeEnd - 1);
            int next = nextNonCommentWord(words, typeEnd);
            final String continuation = typeNameEndsWith(type, "double") ? "precision"
                    : typeNameEndsWith(type, "character") || typeNameEndsWith(type, "char") || typeNameEndsWith(type, "bit") ? "varying" : null;

            if (next >= 0 && next < end && continuation != null && typeWordMatches(words.get(next), continuation)) {
                if (words.get(next).length() > continuation.length()) {
                    return next; // e.g. character varying::line, with the cast glued to the final type word
                }

                next = nextNonCommentWord(words, next + 1);
            }

            if (next >= 0 && next < end && words.get(next).equals("(")) {
                final int close = closingParenthesis(next);
                next = close > next && close < end ? nextNonCommentWord(words, close + 1) : -1;
            }

            if (next >= 0 && next < end && (typeNameEndsWith(type, "time") || typeNameEndsWith(type, "timestamp"))
                    && ("WITH".equalsIgnoreCase(words.get(next)) || "WITHOUT".equalsIgnoreCase(words.get(next)))) {
                final int time = nextNonCommentWord(words, next + 1);
                final int zone = time >= 0 && time < end && "TIME".equalsIgnoreCase(words.get(time)) ? nextNonCommentWord(words, time + 1) : -1;

                if (zone < 0 || zone >= end || !typeWordMatches(words.get(zone), "zone")) {
                    return -1;
                }

                if (words.get(zone).length() > "zone".length()) {
                    return zone;
                }

                next = nextNonCommentWord(words, zone + 1);
            }

            // The SQL-standard ARRAY keyword optionally introduces a dimension instead of [] syntax.
            if (next >= 0 && next < end && isArrayTypeKeyword(words.get(next))) {
                final String word = words.get(next);
                final int suffixEnd = arrayTypeSuffixEnd(word, "ARRAY".length());

                if (suffixEnd < word.length()) {
                    return word.startsWith("::", suffixEnd) ? next : -1;
                }

                next = nextNonCommentWord(words, next + 1);
            }

            // SqlParser keeps bracket groups in one token; the bracket-interior lexer exposes
            // their delimiters separately. Both forms are type suffixes before a following cast.
            while (next >= 0 && next < end && words.get(next).startsWith("[")) {
                final String word = words.get(next);

                if (word.equals("[")) {
                    int close = nextNonCommentWord(words, next + 1);

                    if (close >= 0 && close < end && isArrayDimension(words.get(close))) {
                        close = nextNonCommentWord(words, close + 1);
                    }

                    if (close < 0 || close >= end || !words.get(close).equals("]")) {
                        return -1;
                    }

                    next = nextNonCommentWord(words, close + 1);
                } else {
                    final int suffixEnd = arrayTypeSuffixEnd(word, 0);

                    if (suffixEnd == 0) {
                        return -1;
                    }

                    if (suffixEnd < word.length()) {
                        return word.startsWith("::", suffixEnd) ? next : -1;
                    }

                    next = nextNonCommentWord(words, next + 1);
                }
            }

            return next >= 0 && next < end && words.get(next).startsWith("::") ? next : -1;
        }

        /** Recognizes the unquoted ARRAY type keyword, including glued dimensions or a following cast. */
        private static boolean isArrayTypeKeyword(final String word) {
            return typeWordMatches(word, "ARRAY") || word.regionMatches(true, 0, "ARRAY[", 0, "ARRAY[".length());
        }

        /** Skips empty or integer array dimensions glued to a type token, leaving any cast suffix visible. */
        private static int arrayTypeSuffixEnd(final String word, int offset) {
            while (offset < word.length() && word.charAt(offset) == '[') {
                int close = offset + 1;

                while (close < word.length() && Character.isWhitespace(word.charAt(close))) {
                    close++;
                }

                while (close < word.length() && word.charAt(close) >= '0' && word.charAt(close) <= '9') {
                    close++;
                }

                while (close < word.length() && Character.isWhitespace(word.charAt(close))) {
                    close++;
                }

                if (close >= word.length() || word.charAt(close) != ']') {
                    break;
                }

                offset = close + 1;
            }

            return offset;
        }

        /** Array type bounds are integer constants, not arbitrary subscript expressions. */
        private static boolean isArrayDimension(final String word) {
            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) < '0' || word.charAt(i) > '9') {
                    return false;
                }
            }

            return !word.isEmpty();
        }

        /** Matches a standard multiword type's final keyword, optionally followed by a glued cast suffix. */
        private static boolean typeWordMatches(final String word, final String name) {
            return word.regionMatches(true, 0, name, 0, name.length()) && (word.length() == name.length() || word.startsWith("::", name.length()));
        }

        /** Recognizes the final identifier of a possibly qualified cast type without matching longer identifiers. */
        private static boolean typeNameEndsWith(final String word, final String name) {
            final int start = word.length() - name.length();
            return start >= 0 && word.regionMatches(true, start, name, 0, name.length())
                    && (start == 0 || word.charAt(start - 1) == '.' || word.charAt(start - 1) == ':');
        }

        /** Skips a non-geometric cast's qualified identifier so a following cast remains visible. */
        private int castTypeEnd(int index, int offset, final int end) {
            boolean expectsIdentifier = true;

            while (index >= 0 && index < end) {
                final String word = words.get(index);

                if (offset == word.length()) {
                    index = nextNonCommentWord(words, index + 1);
                    offset = 0;
                    continue;
                }

                if (expectsIdentifier) {
                    if (word.charAt(offset) == '"') {
                        final int close = skipQuotedRegion(word, offset, false);

                        if (close >= word.length()) {
                            return -1;
                        }

                        offset = close + 1;
                    } else {
                        if (!isNamedParameterIdentifierStart(word.codePointAt(offset))) {
                            return -1;
                        }

                        do {
                            offset += Character.charCount(word.codePointAt(offset));
                        } while (offset < word.length() && isNamedParameterIdentifierPart(word.codePointAt(offset)));
                    }

                    expectsIdentifier = false;
                }

                if (offset < word.length()) {
                    if (word.charAt(offset) == '[' && arrayTypeSuffixEnd(word, offset) == word.length()) {
                        return index + 1;
                    }

                    if (word.charAt(offset) != '.') {
                        return -1;
                    }

                    offset++;
                    expectsIdentifier = true;
                } else {
                    final int next = nextNonCommentWord(words, index + 1);

                    if (next < 0 || next >= end || !words.get(next).startsWith(".")) {
                        return index + 1;
                    }

                    index = next;
                    offset = 1;
                    expectsIdentifier = true;
                }
            }

            return -1;
        }

        /**
         * Returns the token after a complete line/lseg type, or {@code -1}. Reads at most a schema,
         * dot and type across token boundaries without joining strings or retokenizing SQL. Both the
         * ordinary and bracket lexers can glue a cast prefix or qualification dot to either name.
         */
        private int geometricTypeEnd(int index, int offset, final int end) {
            return geometricTypeEnd(index, offset, end, false);
        }

        /** Optionally accepts an adjacent single-quoted literal after the complete type name. */
        private int geometricTypeEnd(int index, int offset, final int end, final boolean allowLiteral) {
            if (offset == words.get(index).length()) {
                index = nextNonCommentWord(words, index + 1);
                offset = 0;
            }

            if (index < 0 || index >= end) {
                return -1;
            }

            String word = words.get(index);
            final int schemaEnd = geometricIdentifierEnd(word, offset, "pg_catalog");

            if (schemaEnd >= 0) {
                offset = schemaEnd;

                if (offset == word.length()) {
                    index = nextNonCommentWord(words, index + 1);
                    offset = 0;

                    if (index < 0 || index >= end) {
                        return -1;
                    }

                    word = words.get(index);
                }

                if (word.charAt(offset) != '.') {
                    return -1;
                }

                if (++offset == word.length()) {
                    index = nextNonCommentWord(words, index + 1);
                    offset = 0;

                    if (index < 0 || index >= end) {
                        return -1;
                    }

                    word = words.get(index);
                }
            }

            final int nameEnd = Math.max(geometricIdentifierEnd(word, offset, "line"), geometricIdentifierEnd(word, offset, "lseg"));
            return nameEnd == word.length() || allowLiteral && nameEnd >= 0 && word.charAt(nameEnd) == '\'' ? index + 1 : -1;
        }

        /** Unquoted PostgreSQL names fold to lowercase; quoted names must match the built-in name exactly. */
        private static int geometricIdentifierEnd(final String word, final int offset, final String name) {
            if (offset >= word.length()) {
                return -1;
            }

            final boolean quoted = word.charAt(offset) == '"';
            final int nameStart = offset + (quoted ? 1 : 0);
            final int nameEnd = nameStart + name.length();

            if (!word.regionMatches(!quoted, nameStart, name, 0, name.length()) || (quoted && (nameEnd >= word.length() || word.charAt(nameEnd) != '"'))) {
                return -1;
            }

            final int identifierEnd = nameEnd + (quoted ? 1 : 0);
            return identifierEnd == word.length() || word.charAt(identifierEnd) == '.' || word.charAt(identifierEnd) == '\'' ? identifierEnd : -1;
        }

        /**
         * Builds matching-parenthesis indexes at most once; never rescans nested expression suffixes.
         *
         * @throws IllegalArgumentException if scanning a split MyBatis binding encounters a standalone bracket
         *         group whose first non-whitespace {@code ':'} or {@code '?'} is followed by an unpaired UTF-16 surrogate
         */
        private int closingParenthesis(final int opening) {
            if (closingParentheses == null) {
                final int size = words.size();
                closingParentheses = new int[size];
                int[] stack = new int[8];
                int count = 0;

                for (int i = 0; i < size; i++) {
                    if (isCommentOrSpaceToken(words.get(i))) {
                        continue;
                    }

                    final int bindingEnd = splitIbatisBindingEnd(i);

                    if (bindingEnd > i) {
                        i = bindingEnd;
                        continue;
                    }

                    final String word = words.get(i);

                    if (word.equals("(")) {
                        if (count == stack.length) {
                            stack = Arrays.copyOf(stack, count * 2);
                        }

                        stack[count++] = i;
                    } else if (word.equals(")") && count > 0) {
                        closingParentheses[stack[--count]] = i;
                    }
                }
            }

            return closingParentheses[opening];
        }

        /**
         * Returns the last token of a split MyBatis binding, or the input index for an ordinary token.
         *
         * @throws IllegalArgumentException if an incomplete MyBatis token is a standalone bracket group whose
         *         first non-whitespace {@code ':'} or {@code '?'} is followed by an unpaired UTF-16 surrogate
         */
        private int splitIbatisBindingEnd(final int start) {
            final String word = words.get(start);

            if (word.indexOf(LEFT_OF_IBATIS_NAMED_PARAMETER) < 0 || isQuotedToken(word) || !endsInUnclosedIbatisMarker(word)) {
                return start;
            }

            for (int i = start + 1, size = words.size(); i < size; i++) {
                final String next = words.get(i);

                // Mirrors the constructor's join: the first '}' outside quoted text closes the binding.
                final int braceIndex = findIbatisContinuationClosingBraceIndex(next);

                if (braceIndex == MALFORMED_IBATIS_MARKER) {
                    return size - 1; // the constructor rejects it
                }

                if (braceIndex >= 0) {
                    return i;
                }
            }

            return words.size() - 1; // The constructor reports the malformed binding itself.
        }
    }

    private static boolean isSqlJsonConstructor(final String word) {
        return "JSON_OBJECT".equalsIgnoreCase(word) || "JSON_ARRAY".equalsIgnoreCase(word) || "JSON_OBJECTAGG".equalsIgnoreCase(word)
                || "JSON_ARRAYAGG".equalsIgnoreCase(word) || "JSON".equalsIgnoreCase(word) || "JSON_SCALAR".equalsIgnoreCase(word);
    }

    private static boolean isSqlStringLiteral(final String word) {
        return word.startsWith("'") || word.regionMatches(true, 0, "E'", 0, 2) || word.regionMatches(true, 0, "N'", 0, 2)
                || word.regionMatches(true, 0, "U&'", 0, 3);
    }

    /** Collects the configured separators whose spelling starts with {@code '?'}, for the compact-operator rule. */
    private static Set<String> questionMarkLeadingSeparators() {
        final Set<String> result = new LinkedHashSet<>();

        for (final String separator : SqlParser.defaultTokenizerConfig().separators()) {
            if (separator.length() > 1 && separator.charAt(0) == '?') {
                result.add(separator);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns whether {@code word} is a separator spelled with a leading {@code '?'} whose marker must be
     * counted unless it stands in operator position.
     *
     * <p>{@code ?-}, {@code ?|}, {@code ?&}, {@code ?#}, {@code ?||} and {@code ?-|} are PostgreSQL
     * geometric and jsonb operators, and longest-match tokenization claims them before a placeholder
     * written against one of them can surface. A JDBC driver sees the leading {@code '?'} of
     * {@code ?-1} or {@code ?||'x'} as a placeholder, so leaving it uncounted makes
     * {@link #parameterCount()} disagree with what the statement will bind. The operand test that decides
     * this uses forward operand state, so {@code a ?- b} keeps its binary operator. Explicit line/lseg
     * literals, constructors and casts also identify unary geometric operators without confusing
     * {@code ?-1} or {@code ?-column} with them.</p>
     *
     * @param word the token or subscript word to classify
     * @return {@code true} if {@code word} is a {@code '?'}-leading compact operator
     */
    private static boolean isCompactQuestionMarkOperator(final String word) {
        return word.length() > 1 && word.charAt(0) == '?' && QUESTION_MARK_LEADING_SEPARATORS.contains(word);
    }

    /** Builds fixed operator buckets once; each per-character lookup examines only a bounded set of SQL separators. */
    private static String[][] subscriptSeparators() {
        final List<String> separators = new ArrayList<>(SqlParser.defaultTokenizerConfig().separators());
        separators.sort(Comparator.comparingInt(String::length).reversed());
        final String[][] result = new String[128][];

        for (int ch = 0; ch < result.length; ch++) {
            final List<String> bucket = new ArrayList<>();

            for (final String separator : separators) {
                if (separator.charAt(0) == ch) {
                    bucket.add(separator);
                }
            }

            result[ch] = bucket.toArray(new String[0]);
        }

        return result;
    }

    private static int subscriptSeparatorLength(final String token, final int index) {
        final char first = token.charAt(index);

        if (first < SUBSCRIPT_SEPARATORS.length) {
            for (final String separator : SUBSCRIPT_SEPARATORS[first]) {
                if (token.startsWith(separator, index)) {
                    // Match SqlParser's comment precedence: the '/' of ||/* and the first '-'
                    // of ?-- belong to a comment opener, not to a longer operator. Consuming
                    // them here would expose question marks inside the comment as bindings.
                    final int after = index + separator.length();
                    final char last = separator.charAt(separator.length() - 1);
                    if (separator.length() > 1 && after < token.length()
                            && (last == '-' && token.charAt(after) == '-' || last == '/' && token.charAt(after) == '*')) {
                        continue;
                    }
                    return separator.length();
                }
            }
        }

        return 0;
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
        // Punctuation-led operands cannot be SQL keywords. Only word operands need the keyword
        // checks; quoted values and closing groups are common around JSON existence operators.
        // A complete bracket-group token is an operand as well: a chained or standalone subscript
        // ("payload['a']['b']", "(payload)['a']") or a bracket-quoted identifier. The bracket-interior lexer
        // emits a lone "[" instead, so the "?" opening "ARRAY[?" stays a placeholder. The END closing a CASE
        // expression is an operand although it is a boundary word elsewhere.
        return first == ')' || first == ']' || first == '?' || first == '_' || first == '"' || first == '`' || first == '\'' || first == '$' || first == '.'
                || first == _PREFIX_OF_NAMED_PARAMETER || word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) || (first == '[' && word.length() > 1)
                || "END".equalsIgnoreCase(word)
                || (startsWithSqlExpressionWord(word) && !isSqlExpressionBoundaryWord(word) && !isPlaceholderLeadingKeyword(word));
    }

    /**
     * Returns {@code true} for keywords after which a {@code ?} is always a positional placeholder and never
     * the left operand of a JSON existence operator: clause openers, CASE parts, and operator-like keywords
     * that take a value on their right ({@code INTERVAL ? DAY}, {@code ILIKE ? ESCAPE '!'},
     * {@code SIMILAR TO ? ...}; keywords that are also plausible column names, such as the {@code OF} of
     * {@code AS OF ?}, the {@code ZONE} of {@code AT TIME ZONE ?} and the {@code RANGE}/{@code GROUPS} of a window
     * frame, are handled by {@code isContextualPlaceholderKeyword}).
     * Without this list such a {@code ?} followed by an identifier-like word ({@code DAY}, {@code ESCAPE}, an alias) would be dropped from the parameter count.
     * {@code VALUES} is not listed: its rows are parenthesized, and where a bare {@code VALUES ?} is accepted
     * (DB2) no operand can follow the bare {@code ?}, which therefore stays a placeholder, while {@code values ? 'k'}
     * tests a column named {@code values}. A {@code ?} glued to a non-JSON operator after it ({@code VALUES ?-1},
     * {@code VALUES ?||'x'}) is handled by the classifier's {@code leadsMarkerAsKeyword}.
     */
    private static boolean isPlaceholderLeadingKeyword(final String word) {
        // Length dispatch avoids testing every keyword for identifiers, punctuation and quoted text.
        return switch (word.length()) {
            case 2 -> "ON".equalsIgnoreCase(word) || "OR".equalsIgnoreCase(word) || "IN".equalsIgnoreCase(word) || "TO".equalsIgnoreCase(word);
            case 3 -> "AND".equalsIgnoreCase(word) || "NOT".equalsIgnoreCase(word) || "SET".equalsIgnoreCase(word) || "DIV".equalsIgnoreCase(word)
                    || "MOD".equalsIgnoreCase(word);
            case 4 -> "THEN".equalsIgnoreCase(word) || "ELSE".equalsIgnoreCase(word) || "WHEN".equalsIgnoreCase(word) || "CASE".equalsIgnoreCase(word);
            case 5 -> "WHERE".equalsIgnoreCase(word) || "ILIKE".equalsIgnoreCase(word) || "RLIKE".equalsIgnoreCase(word);
            case 6 -> "SELECT".equalsIgnoreCase(word) || "HAVING".equalsIgnoreCase(word) || "REGEXP".equalsIgnoreCase(word) || "ESCAPE".equalsIgnoreCase(word);
            case 8 -> "INTERVAL".equalsIgnoreCase(word);
            default -> false;
        };
    }

    /**
     * Returns {@code true} for value-taking keywords that are also plausible column names, and so lead a
     * placeholder only in the position where they act as keywords: the {@code OF} of a temporal/flashback
     * {@code AS OF ?} ({@code FOR SYSTEM_TIME AS OF ?}), the Oracle flashback point types in
     * {@code AS OF TIMESTAMP ?} and {@code AS OF SCN ?}, and the Firebird/Informix row-skip count after a
     * {@code FIRST} count ({@code SELECT FIRST ? SKIP ? id}) or right after {@code SELECT}. Elsewhere,
     * {@code of ? 'key'}, {@code timestamp ? 'key'} and {@code skip ? 'key'} stay JSON existence tests on such
     * columns. {@code SELECT skip ? x} is ambiguous: the {@code ?} is read as a JSON operator when a string literal
     * (including a prefixed one such as {@code E'key'} or {@code N'key'}) follows it ({@code SELECT SKIP ? 'constant'}
     * is rarely written), or when the statement contains a named or MyBatis marker anywhere
     * ({@code SELECT skip ? :key}, {@code SELECT skip ? lower(:key)}), since such a statement cannot also carry
     * positional bindings; otherwise ({@code SELECT SKIP ? id}) it is the Firebird row-skip binding.
     * Likewise {@code ZONE} leads a placeholder only in {@code AT TIME ZONE ?}; {@code zone ? 'k'} tests a jsonb column
     * of that name. The window-frame units {@code RANGE} and {@code GROUPS}, {@code VALUES} and the {@code KEY} of a
     * JSON object entry need the token stream and are handled by the classifier ({@code leadsMarkerAsKeyword}).
     *
     * @param wordBeforePrevious the non-comment word before {@code previousWord}
     * @param previousWord the non-comment word directly before the {@code ?}
     * @param nextWord the non-comment word directly after the {@code ?} (empty if none)
     * @param namedMarkerPresent reports whether the statement contains a named or MyBatis marker; consulted only for
     *            the ambiguous {@code SELECT skip ?} position
     */
    private static boolean isContextualPlaceholderKeyword(final String wordBeforePrevious, final String previousWord, final String nextWord,
            final BooleanSupplier namedMarkerPresent) {
        if ("SKIP".equalsIgnoreCase(previousWord)) {
            if ("SELECT".equalsIgnoreCase(wordBeforePrevious)) {
                return !isStringLiteralToken(nextWord) && !namedMarkerPresent.getAsBoolean();
            }

            return wordBeforePrevious.equals(SK.QUESTION_MARK) || wordBeforePrevious.equals(")")
                    || (!wordBeforePrevious.isEmpty() && Character.isDigit(wordBeforePrevious.charAt(0)));
        }

        if ("OF".equalsIgnoreCase(previousWord)) {
            return "AS".equalsIgnoreCase(wordBeforePrevious);
        }

        if ("ZONE".equalsIgnoreCase(previousWord)) {
            return "TIME".equalsIgnoreCase(wordBeforePrevious);
        }

        return "OF".equalsIgnoreCase(wordBeforePrevious) && ("TIMESTAMP".equalsIgnoreCase(previousWord) || "SCN".equalsIgnoreCase(previousWord));
    }

    /**
     * Returns {@code true} if the token holds a named ({@code :name}) or MyBatis ({@code #{...}}) marker that the
     * constructor would convert: the same pre-filters and unquoted-marker scanners, so quoted text, casts
     * ({@code ::int}) and qualified names are not mistaken for markers.
     *
     * @param token a source token
     * @param chainedSubscript whether the token continues an identifier-rooted subscript chain
     * @param followsClosedValue whether the token directly continues a token ending with a closing quote,
     *        parenthesis or bracket, as {@link #followsClosedValueToken(List, int, IntList)} reports
     * @throws IllegalArgumentException if a prospective colon-style marker is followed by an unpaired UTF-16 surrogate
     */
    private static boolean containsNamedOrIbatisMarker(final String token, final boolean chainedSubscript, final boolean followsClosedValue) {
        if (mayContainIbatisParameter(token, chainedSubscript) && findUnquotedIbatisMarkerIndexes(token).length > 0) {
            return true;
        }

        if (mayContainNamedParameter(token, chainedSubscript)) {
            // The constructor's extraction loop starts with searchFrom = 0 and only advances it past an accepted
            // marker, so the first accepted candidate is found with the same boundary test.
            for (final int markerIndex : findUnquotedNamedParameterMarkerIndexes(token)) {
                if (isNamedParameterStart(token, markerIndex, 0, followsClosedValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if {@code word} is a string literal, optionally with a prefix made of letters, digits,
     * {@code _} and {@code &} ({@code 'a'}, {@code E'a'}, {@code N'a'}, {@code U&'a'}, {@code _utf8mb4'a'}).
     */
    private static boolean isStringLiteralToken(final String word) {
        final int quote = word.indexOf('\'');

        if (quote < 0) {
            return false;
        }

        for (int k = 0; k < quote; k++) {
            final char ch = word.charAt(k);

            if (!(Character.isLetterOrDigit(ch) || ch == '_' || (ch == '&' && k > 0))) {
                return false;
            }
        }

        return quote == 0 || !Character.isDigit(word.charAt(0));
    }

    /**
     * Checks whether a token can be the right operand of a JSON question-mark operator.
     *
     * @throws IllegalArgumentException if the colon at the start of {@code word} is immediately followed by
     *         an unpaired UTF-16 surrogate
     */
    private static boolean canFollowJsonQuestionOperator(final String word) {
        if (Strings.isEmpty(word)) {
            return false;
        }

        final char firstChar = word.charAt(0);

        // PostgreSQL's JSON existence operator accepts any text-valued expression on its right,
        // including column references and function calls such as "payload ? lower(:key)". Requiring
        // a literal/placeholder here misclassifies the operator as a JDBC placeholder and then
        // falsely reports mixed parameter styles when the expression contains a named parameter.
        return word.equals(SK.QUESTION_MARK) || isCompactQuestionMarkOperator(word) || firstChar == '\'' || firstChar == '"' || firstChar == '`'
                || firstChar == '(' || firstChar == '['
                || (firstChar == _PREFIX_OF_NAMED_PARAMETER && word.length() >= 2 && isNamedParameterIdentifierStart(namedParameterCodePointAt(word, 1)))
                || word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) || (startsWithSqlExpressionWord(word) && !isSqlExpressionBoundaryWord(word));
    }

    private static boolean isSqlExpressionBoundaryWord(final String word) {
        // Length dispatch avoids testing every keyword for identifiers, punctuation and quoted text.
        return switch (word.length()) {
            case 2 -> "OR".equalsIgnoreCase(word) || "ON".equalsIgnoreCase(word) || "AS".equalsIgnoreCase(word) || "IS".equalsIgnoreCase(word)
                    || "IN".equalsIgnoreCase(word) || "BY".equalsIgnoreCase(word);
            case 3 -> "AND".equalsIgnoreCase(word) || "FOR".equalsIgnoreCase(word) || "END".equalsIgnoreCase(word) || "NOT".equalsIgnoreCase(word)
                    || "ASC".equalsIgnoreCase(word) || "ROW".equalsIgnoreCase(word) || "TOP".equalsIgnoreCase(word) || "ALL".equalsIgnoreCase(word)
                    || "ANY".equalsIgnoreCase(word);
            case 4 -> "FROM".equalsIgnoreCase(word) || "JOIN".equalsIgnoreCase(word) || "WHEN".equalsIgnoreCase(word) || "THEN".equalsIgnoreCase(word)
                    || "ELSE".equalsIgnoreCase(word) || "LIKE".equalsIgnoreCase(word) || "DESC".equalsIgnoreCase(word) || "LAST".equalsIgnoreCase(word)
                    || "ROWS".equalsIgnoreCase(word) || "ONLY".equalsIgnoreCase(word) || "NEXT".equalsIgnoreCase(word) || "SOME".equalsIgnoreCase(word);
            case 5 -> "WHERE".equalsIgnoreCase(word) || "GROUP".equalsIgnoreCase(word) || "ORDER".equalsIgnoreCase(word) || "LIMIT".equalsIgnoreCase(word)
                    || "FETCH".equalsIgnoreCase(word) || "UNION".equalsIgnoreCase(word) || "MINUS".equalsIgnoreCase(word) || "USING".equalsIgnoreCase(word)
                    || "NULLS".equalsIgnoreCase(word) || "FIRST".equalsIgnoreCase(word);
            case 6 -> "HAVING".equalsIgnoreCase(word) || "OFFSET".equalsIgnoreCase(word) || "EXCEPT".equalsIgnoreCase(word);
            case 7 -> "BETWEEN".equalsIgnoreCase(word);
            case 8 -> "DISTINCT".equalsIgnoreCase(word);
            case 9 -> "RETURNING".equalsIgnoreCase(word) || "INTERSECT".equalsIgnoreCase(word);
            default -> false;
        };
    }

    private static boolean isCommentOrSpaceToken(final String word) {
        return Strings.isEmpty(word) || word.equals(SK.SPACE) || word.startsWith("--") || word.startsWith("/*");
    }

    /**
     * Returns the index of the {@code '}'} in {@code token} that closes a MyBatis marker whose content (or, for a
     * token that continues a split marker, whose remaining content) starts at {@code fromIndex}; {@code -1} if the
     * token holds none; or {@link #MALFORMED_IBATIS_MARKER} if the marker cannot be closed: a quoted region in it is
     * not closed within the token (or the backslash-escape readings disagree on where it ends), or another
     * {@code "#{"} opens before the {@code '}'}. Quoted regions ({@code '}, {@code "}, {@code `}) and block and
     * dash-line comments are skipped, so a {@code '}'} inside them never closes the marker.
     *
     * @param token the token holding the marker or continuing it
     * @param fromIndex the index right after the marker's {@code "#{"}, or {@code 0} for a continuation token
     * @return the closing brace index, {@code -1}, or {@link #MALFORMED_IBATIS_MARKER}
     */
    private static int findIbatisClosingBraceIndex(final String token, final int fromIndex) {
        for (int k = fromIndex, len = token.length(); k < len; k++) {
            final char ch = token.charAt(k);

            if (ch == '}') {
                return k;
            } else if (ch == '#' && k + 1 < len && token.charAt(k + 1) == '{') {
                return MALFORMED_IBATIS_MARKER;
            } else if (isQuoteChar(ch)) {
                final int quoteEnd = skipQuotedRegion(token, k, true);

                if (quoteEnd >= len || quoteEnd != skipQuotedRegion(token, k, false)) {
                    return MALFORMED_IBATIS_MARKER;
                }

                k = quoteEnd;
            } else if (ch == '/' && k + 1 < len && token.charAt(k + 1) == '*') {
                final int commentEnd = token.indexOf("*/", k + 2);
                k = commentEnd < 0 ? len : commentEnd + 1;
            } else if (ch == '-' && k + 1 < len && token.charAt(k + 1) == '-') {
                while (k + 1 < len && token.charAt(k + 1) != '\n' && token.charAt(k + 1) != '\r') {
                    k++;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the index of the {@code '}'} that closes a split MyBatis marker in {@code token}, a token that continues
     * it, as {@link #findIbatisClosingBraceIndex(String, int)} does from the token's start. A comment or whitespace token
     * never closes the marker ({@code -1}). A token that starts with a quote is a standalone literal or quoted
     * identifier, never part of a property path (unlike the {@code ['k']} of {@code map['k']}), so it makes the marker
     * malformed ({@link #MALFORMED_IBATIS_MARKER}).
     */
    private static int findIbatisContinuationClosingBraceIndex(final String token) {
        if (isCommentOrSpaceToken(token)) {
            return -1;
        }

        return isQuoteChar(token.charAt(0)) ? MALFORMED_IBATIS_MARKER : findIbatisClosingBraceIndex(token, 0);
    }

    /**
     * Returns {@code true} if the constructor would continue a MyBatis marker of {@code token} into the following
     * tokens: walking its unquoted markers in order, as the constructor does, one of them has no closing {@code '}'}
     * in the token (<code>#{ name</code>, <code>#{a}#{ b</code>). A token whose markers all close, or one that holds a malformed
     * marker the constructor rejects, returns {@code false}.
     */
    private static boolean endsInUnclosedIbatisMarker(final String token) {
        int from = 0;

        for (final int marker : findUnquotedIbatisMarkerIndexes(token)) {
            if (marker < from) {
                continue;
            }

            final int closingIndex = findIbatisClosingBraceIndex(token, marker + 2);

            if (closingIndex < 0) {
                return closingIndex == -1;
            }

            from = closingIndex + 1;
        }

        return false;
    }

    /**
     * Returns {@code true} if {@code index} lies inside a bracket group of {@code token} that opens before it and is
     * still open there. Brackets inside quoted regions and block or dash-line comments are ignored, and a group closed
     * earlier in the token ({@code #{a[0]}#{b }}) does not count.
     */
    private static boolean isInsideOpenBracketGroup(final String token, final int index) {
        int depth = 0;

        for (int k = 0; k < index; k++) {
            final char ch = token.charAt(k);

            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth = Math.max(0, depth - 1);
            } else if (isQuoteChar(ch)) {
                k = skipQuotedRegion(token, k, true);
            } else if (ch == '/' && k + 1 < index && token.charAt(k + 1) == '*') {
                final int commentEnd = token.indexOf("*/", k + 2);
                k = commentEnd < 0 ? index : commentEnd + 1;
            } else if (ch == '-' && k + 1 < index && token.charAt(k + 1) == '-') {
                while (k + 1 < index && token.charAt(k + 1) != '\n' && token.charAt(k + 1) != '\r') {
                    k++;
                }
            }
        }

        return depth > 0;
    }

    /**
     * Extracts the property name of a MyBatis inline parameter, {@code propertyName [':' jdbcType] [',' attributes]}:
     * like MyBatis's own parser, the name ends at the first comma or colon, so {@code #{id:BIGINT}} and
     * {@code #{name:VARCHAR,javaType=String}} bind {@code id} and {@code name}.
     */
    private static String extractIbatisNamedParameter(final String content) {
        final String trimmed = Strings.stripToEmpty(content);

        if (Strings.isEmpty(trimmed)) {
            return Strings.EMPTY;
        }

        for (int index = 0, len = trimmed.length(); index < len; index++) {
            final char ch = trimmed.charAt(index);

            if (ch == SK._COMMA || ch == _PREFIX_OF_NAMED_PARAMETER) {
                return trimmed.substring(0, index).trim();
            }
        }

        return trimmed;
    }

    private static boolean isNamedParameterIdentifierStart(final int codePoint) {
        return codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint);
    }

    private static boolean isNamedParameterIdentifierPart(final int codePoint) {
        return codePoint == '_' || Character.isUnicodeIdentifierPart(codePoint);
    }

    /**
     * Reads a complete UTF-16 code point at a prospective parameter-name position.
     *
     * @throws IllegalArgumentException if the character at {@code index} is a low surrogate or a high surrogate
     *         without a following low surrogate
     */
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

    /**
     * Reads a complete UTF-16 code point immediately before a parameter marker.
     *
     * @throws IllegalArgumentException if the character before {@code index} is a high surrogate or a low surrogate
     *         without a preceding high surrogate
     */
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
