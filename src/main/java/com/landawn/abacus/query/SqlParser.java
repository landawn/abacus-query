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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;

/**
 * A utility class for parsing SQL statements into individual words and tokens.
 * This parser recognizes SQL keywords, operators, identifiers, and various SQL-specific
 * separators while preserving the structure and meaning of the SQL statement.
 *
 * <p>Tokenization details:</p>
 * <ul>
 *   <li>Single-quoted, double-quoted, backtick-quoted and square-bracket-quoted
 *       strings/identifiers are kept as a single token. Doubled-quote escapes ({@code ''},
 *       {@code ""}, {@code ``}, {@code ]]}) are recognized inside their respective quoted
 *       regions; backslash escaping is additionally recognized inside single-, double- and
 *       backtick-quoted regions, but not inside square-bracket identifiers (where only
 *       {@code ]]} escapes a bracket).</li>
 *   <li>Comments are normally stripped: line comments ({@code -- ...}), MySQL hash comments
 *       ({@code # ...}), and block comments ({@code /* ... *}{@code /}) are discarded.
 *       Exception: block comments are retained as tokens when the SQL begins
 *       (case-insensitively) with the marker {@code "-- Keep comments"}; line and hash
 *       comments are always discarded regardless of this marker.
 *       Nested block comments and PostgreSQL dollar-quoting ({@code $$...$$}) are NOT
 *       supported.</li>
 *   <li>Runs of whitespace between emitted tokens are collapsed into a single space token
 *       ({@code " "}); leading whitespace before the very first emitted token is dropped, while
 *       trailing whitespace after the last non-space token yields a final {@code " "} token.</li>
 *   <li>Multi-character operators (e.g. {@code >=}, {@code <>}, {@code ->>}, PostgreSQL
 *       {@code #-} and {@code @?}) are emitted
 *       as single tokens; additional separators can be registered via
 *       {@link #registerSeparator(String)} / {@link #registerSeparator(char)}.</li>
 *   <li>iBatis/MyBatis {@code #{...}} markers and registered PostgreSQL hash operators are
 *       not mistaken for hash comments.</li>
 *   <li>{@code #} that opens a hash-prefixed identifier (e.g. a temp table name appearing
 *       after {@code FROM}, {@code JOIN}, {@code INTO}, {@code UPDATE} or {@code TABLE},
 *       allowing intervening whitespace or comments, including as a later element of a
 *       comma-separated list governed by one of those keywords, e.g. {@code "FROM #t1, #t2"})
 *       is treated as part of the identifier, not as a hash comment or separator.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String sql = "SELECT * FROM users WHERE age > 25 ORDER BY name";
 * List<String> words = SqlParser.parse(sql);
 * // Result: ["SELECT", " ", "*", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">", " ", "25", " ", "ORDER", " ", "BY", " ", "name"]
 * }</pre>
 *
 * <p id="query-classification"><b>Query classification:</b> the {@link #isSelectQuery(String)},
 * {@link #isInsertQuery(String)}, {@link #isUpdateQuery(String)}, {@link #isDeleteQuery(String)},
 * {@link #isInsertOrReplaceQuery(String)}, {@link #isReadOnlyQuery(String)} and
 * {@link #isNoUpdateQuery(String)} predicates classify a statement as summarized below. Each cell shows
 * whether the predicate in that column returns {@code true} (Y) or {@code false} (N) for the statement
 * kind in that row. Note every read-only statement is also a no-update statement, but not vice versa.</p>
 * <table border="1">
 * <caption>{@code SqlParser} query-classification predicates by statement kind</caption>
 * <tr>
 *   <th>Operation (leading keyword / clause)</th>
 *   <th>{@code isSelectQuery}</th>
 *   <th>{@code isInsertQuery}</th>
 *   <th>{@code isUpdateQuery}</th>
 *   <th>{@code isDeleteQuery}</th>
 *   <th>{@code isInsertOrReplaceQuery}</th>
 *   <th>{@code isReadOnlyQuery}</th>
 *   <th>{@code isNoUpdateQuery}</th>
 * </tr>
 * <tr><td>{@code SELECT}</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>Y</td><td>Y</td></tr>
 * <tr><td>{@code SELECT ... INTO}</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code INSERT}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>Y</td></tr>
 * <tr><td>{@code INSERT OR REPLACE}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>Y</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code INSERT ... ON DUPLICATE KEY UPDATE}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code INSERT ... ON CONFLICT ... DO UPDATE}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code INSERT ... ON CONFLICT ... DO NOTHING}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>Y</td></tr>
 * <tr><td>{@code INSERT OVERWRITE}</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code UPDATE}</td><td>N</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code DELETE}</td><td>N</td><td>N</td><td>N</td><td>Y</td><td>N</td><td>N</td><td>N</td></tr>
 * <tr><td>{@code MERGE} / {@code REPLACE} / {@code TRUNCATE} / {@code CREATE} / {@code ALTER} / {@code DROP}</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * </table>
 *
 */
public final class SqlParser {

    private static final String KEEP_COMMENTS = "-- Keep comments";

    private static final char TAB = '\t';

    private static final char ENTER = '\n';

    private static final char ENTER_2 = '\r';

    private static final AtomicInteger maxSeparatorLength = new AtomicInteger(1);
    private static final Set<String> hashIdentifierContextKeywords = N.asSet(SK.FROM, SK.JOIN, SK.INTO, SK.UPDATE, "TABLE");

    private static final Set<Object> separators = ConcurrentHashMap.newKeySet();

    /**
     * Primitive lookup for single-character ASCII separators, derived from the single-character
     * entries of {@link #separators} to avoid Character autoboxing + Set lookup on the per-char
     * hot path. Rebuilt whenever a single-character separator is registered so it can't drift.
     * Non-ASCII / multi-char entries still fall back to {@link #separators}.
     */
    private static volatile boolean[] ASCII_SEPARATOR = new boolean[128];

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Multi-character separators grouped by length: {@code multiCharSeparatorsByLen[L]} holds all
     * registered separators of length {@code L} (index 0 and 1 are unused/empty). Derived from the
     * {@code String} entries of {@link #separators} so it cannot drift. Used by
     * {@link #matchMultiCharSeparator} to compare characters directly against candidates instead of
     * allocating a {@code substring} per probe. The returned separator instance and chosen length
     * are identical to a {@code separators}-backed substring lookup (longest match first).
     */
    private static volatile String[][] multiCharSeparatorsByLen = new String[1][0];

    /**
     * Fast-reject lookup for {@link #matchMultiCharSeparator}: {@code true} at index {@code c}
     * if some registered multi-character separator starts with the ASCII character {@code c}.
     * Rebuilt together with {@link #multiCharSeparatorsByLen} (in
     * {@link #rebuildMultiCharSeparatorTable()}) so it cannot drift. Most characters are not the
     * first character of any multi-character separator, so this avoids probing every candidate
     * bucket for every ordinary character on the parse hot path.
     */
    private static volatile boolean[] MULTI_CHAR_SEPARATOR_FIRST_CHAR = new boolean[128];

    /**
     * {@code true} if any registered multi-character separator starts with a non-ASCII character
     * (which {@link #MULTI_CHAR_SEPARATOR_FIRST_CHAR} cannot represent); such characters then skip
     * the fast-reject and fall through to the full candidate probe.
     */
    private static volatile boolean multiCharSeparatorNonAsciiFirstChar = false;

    static {
        loadDefaultSeparators();
    }

    /**
     * Clears the current separator set and repopulates it with the built-in default
     * separators, then rebuilds all derived lookup tables ({@link #maxSeparatorLength},
     * {@link #ASCII_SEPARATOR}, {@link #multiCharSeparatorsByLen},
     * {@link #MULTI_CHAR_SEPARATOR_FIRST_CHAR}, and {@link #multiCharSeparatorNonAsciiFirstChar}). Shared by the static
     * initializer and {@link #resetSeparators()}.
     */
    private static void loadDefaultSeparators() {
        separators.clear();
        maxSeparatorLength.set(1);

        separators.add(TAB);
        separators.add(ENTER);
        separators.add(ENTER_2);
        separators.add(' ');
        separators.add('?');
        separators.add(',');
        separators.add('~');
        separators.add('!');
        separators.add('@');
        separators.add('^');
        // Registered so isSeparator('#') stays true, but a bare '#' is in practice always consumed
        // as a MySQL hash comment (or merged into a hash-prefixed identifier / #{...} marker) by the
        // scanners before the separator branch, so '#' is never emitted as its own token.
        separators.add('#');
        separators.add("!!");
        separators.add(';');
        separators.add('(');
        separators.add(')');
        separators.add('=');
        separators.add("==");
        separators.add(":=");
        separators.add("^=");
        separators.add("~=");
        separators.add("+=");
        separators.add("-=");
        separators.add("*=");
        separators.add("/=");
        separators.add("%=");
        separators.add("&=");
        separators.add("|=");
        separators.add("!=");
        separators.add("<>");
        separators.add("!<");
        separators.add("!>");
        separators.add('>');
        separators.add(">>");
        separators.add(">=");
        separators.add("@>");
        separators.add("&>");
        separators.add(">^");
        separators.add('<');
        separators.add("<<");
        separators.add("<=");
        separators.add("<@");
        separators.add("&<");
        separators.add("<^");
        separators.add('+');
        separators.add('-');
        separators.add('%');
        separators.add('/');
        separators.add('*');
        separators.add('&');
        separators.add("&&");
        separators.add('|');
        separators.add("||");
        separators.add("|/");
        separators.add("||/");
        separators.add("->");
        separators.add("#>");
        separators.add("#>>");
        separators.add("#-");
        separators.add("##");
        separators.add("@?");
        separators.add("@@");
        separators.add("@-@");
        separators.add("@@@");
        separators.add("->>");
        separators.add("<->");
        separators.add("<=>");
        separators.add(">>=");
        separators.add("<<=");
        separators.add("<<|");
        separators.add("&<|");
        separators.add("|&>");
        separators.add("|>>");
        separators.add("(+)");
        separators.add("?#");
        separators.add("?-");
        separators.add("?&");
        separators.add("?|");
        separators.add("?-|");
        separators.add("?||");
        separators.add("~*");
        separators.add("!~");
        separators.add("!~*");
        separators.add("^-=");
        separators.add("|*=");

        for (final Object separator : separators) {
            if (separator instanceof final String separatorStr && separatorStr.length() > maxSeparatorLength.get()) {
                maxSeparatorLength.set(separatorStr.length());
            }
        }

        rebuildAsciiSeparatorTable();
        rebuildMultiCharSeparatorTable();
    }

    /**
     * Recomputes {@link #maxSeparatorLength} from the current contents of {@link #separators}.
     * Unlike the incremental update in {@link #registerSeparator(String)} (which only ever
     * grows the value), this scans every registered separator and resets the maximum to the
     * longest one currently present, so it can shrink after separators are removed.
     */
    private static void recomputeMaxSeparatorLength() {
        int max = 1;

        for (final Object separator : separators) {
            if (separator instanceof final String separatorStr && separatorStr.length() > max) {
                max = separatorStr.length();
            }
        }

        maxSeparatorLength.set(max);
    }

    private static void rebuildAsciiSeparatorTable() {
        final boolean[] table = new boolean[128];

        for (final Object separator : separators) {
            if (separator instanceof final Character ch) {
                final char c = ch;

                if (c < 128) {
                    table[c] = true;
                }
            }
        }

        ASCII_SEPARATOR = table;
    }

    private static void rebuildMultiCharSeparatorTable() {
        int maxLen = 1;

        for (final Object separator : separators) {
            if (separator instanceof final String s && s.length() > maxLen) {
                maxLen = s.length();
            }
        }

        @SuppressWarnings("unchecked")
        final List<String>[] buckets = new List[maxLen + 1];

        for (final Object separator : separators) {
            if (separator instanceof final String s && s.length() > 1) {
                final int l = s.length();

                if (buckets[l] == null) {
                    buckets[l] = new ArrayList<>();
                }

                buckets[l].add(s);
            }
        }

        final String[][] table = new String[maxLen + 1][];

        for (int l = 0; l <= maxLen; l++) {
            table[l] = buckets[l] == null ? EMPTY_STRING_ARRAY : buckets[l].toArray(new String[0]);
        }

        final boolean[] firstCharTable = new boolean[128];
        boolean nonAsciiFirstChar = false;

        for (final Object separator : separators) {
            if (separator instanceof final String s && s.length() > 1) {
                final char first = s.charAt(0);

                if (first < 128) {
                    firstCharTable[first] = true;
                } else {
                    nonAsciiFirstChar = true;
                }
            }
        }

        multiCharSeparatorsByLen = table;
        MULTI_CHAR_SEPARATOR_FIRST_CHAR = firstCharTable;
        multiCharSeparatorNonAsciiFirstChar = nonAsciiFirstChar;
    }

    private static final Map<String, String[]> compositeWords = new ConcurrentHashMap<>(64);

    /**
     * Reused space-splitter (immutable config) instead of constructing one per split call.
     * Empty sub-words are omitted so a composite word containing consecutive spaces (e.g.
     * {@code "ORDER  BY"}) splits to the same sub-words as its single-spaced form instead of
     * producing an empty, never-matching middle sub-word.
     */
    private static final Splitter WORD_SPLITTER = Splitter.with(SK.SPACE).trimResults().omitEmptyStrings();

    static {
        compositeWords.put(SK.LEFT_JOIN, new String[] { "LEFT", "JOIN" });
        compositeWords.put(SK.RIGHT_JOIN, new String[] { "RIGHT", "JOIN" });
        compositeWords.put(SK.FULL_JOIN, new String[] { "FULL", "JOIN" });
        compositeWords.put(SK.CROSS_JOIN, new String[] { "CROSS", "JOIN" });
        compositeWords.put(SK.NATURAL_JOIN, new String[] { "NATURAL", "JOIN" });
        compositeWords.put(SK.INNER_JOIN, new String[] { "INNER", "JOIN" });
        compositeWords.put(SK.GROUP_BY, new String[] { "GROUP", "BY" });
        compositeWords.put(SK.ORDER_BY, new String[] { "ORDER", "BY" });
        compositeWords.put(SK.FOR_UPDATE, new String[] { "FOR", "UPDATE" });
        compositeWords.put(SK.FETCH_FIRST, new String[] { "FETCH", "FIRST" });
        compositeWords.put(SK.FETCH_NEXT, new String[] { "FETCH", "NEXT" });
        compositeWords.put(SK.ROWS_ONLY, new String[] { "ROWS", "ONLY" });
        compositeWords.put(SK.UNION_ALL, new String[] { "UNION", "ALL" });
        compositeWords.put(SK.IS_NOT, new String[] { "IS", "NOT" });
        compositeWords.put(SK.IS_NULL, new String[] { "IS", "NULL" });
        compositeWords.put(SK.IS_NOT_NULL, new String[] { "IS", "NOT", "NULL" });
        compositeWords.put(SK.IS_EMPTY, new String[] { "IS", "EMPTY" });
        compositeWords.put(SK.IS_NOT_EMPTY, new String[] { "IS", "NOT", "EMPTY" });
        compositeWords.put(SK.IS_BLANK, new String[] { "IS", "BLANK" });
        compositeWords.put(SK.IS_NOT_BLANK, new String[] { "IS", "NOT", "BLANK" });
        compositeWords.put(SK.NOT_IN, new String[] { "NOT", "IN" });
        compositeWords.put(SK.NOT_EXISTS, new String[] { "NOT", "EXISTS" });
        compositeWords.put(SK.NOT_LIKE, new String[] { "NOT", "LIKE" });
        compositeWords.put(SK.PARTITION_BY, new String[] { "PARTITION", "BY" });

        final List<String> list = new ArrayList<>(compositeWords.keySet());

        for (String e : list) {
            e = e.toLowerCase(Locale.ROOT);

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, WORD_SPLITTER.splitToArray(e));
            }

            e = e.toUpperCase(Locale.ROOT);

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, WORD_SPLITTER.splitToArray(e));
            }
        }
    }

    private SqlParser() {
    }

    /**
     * Parses a SQL statement into a list of individual words and tokens.
     * This method tokenizes the SQL string while preserving the semantic meaning
     * of SQL constructs such as keywords, operators, identifiers, and literals.
     *
     * <p>Comments are stripped (line and hash comments are always stripped; block comments are
     * stripped unless the SQL begins case-insensitively with {@code "-- Keep comments"}, in which
     * case each block comment is emitted as its own token) and runs of whitespace are collapsed
     * into a single space token (see the class-level documentation for full tokenization rules).
     * Composite keywords are <em>not</em> merged: e.g. {@code "ORDER BY"} is returned as the
     * separate tokens {@code "ORDER"}, {@code " "}, {@code "BY"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = SqlParser.parse("SELECT name, age FROM users WHERE age >= 18");
     * // Result: ["SELECT", " ", "name", ",", " ", "age", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">=", " ", "18"]
     * }</pre>
     *
     * @param sql the SQL statement to parse (must not be {@code null})
     * @return a list of tokens representing the parsed SQL statement
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    public static List<String> parse(final String sql) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            final List<String> words = new ArrayList<>(Math.max(16, sqlLength / 4));

            String temp = "";
            char quoteChar = 0;
            int keepComments = -1;
            // Forward-running backslash parity: true if the char at the current `index` is
            // immediately preceded by an ODD number of consecutive backslashes. Maintained while
            // consuming a quoted region so the closing-quote escape decision is identical to the
            // previous O(n) backward backslash scan, without the O(n^2) worst case.
            boolean bsEscaped = false;

            for (int index = 0; index < sqlLength; index++) {
                char ch = sql.charAt(index);

                if (quoteChar != 0) {
                    // is it in a quoted identifier?
                    sb.append(ch);

                    // end in quote.
                    if (ch == quoteChar) {
                        if (bsEscaped) {
                            // Escaped closing quote: stays in the string. The quote char itself
                            // is not a backslash, so the run parity resets to even. Checked before
                            // the doubled-quote case so an escaped quote immediately followed by
                            // another quote is not mis-read as a doubled-quote pair.
                            bsEscaped = false;
                        } else if (index < sqlLength - 1 && sql.charAt(index + 1) == quoteChar) {
                            sb.append(sql.charAt(++index));
                            // Two quote chars consumed (non-backslash) -> run parity is even.
                            bsEscaped = false;
                        } else {
                            // Even count (including 0) of preceding backslashes -> quote NOT escaped.
                            words.add(sb.toString());
                            sb.setLength(0);

                            quoteChar = 0;
                            bsEscaped = false;
                        }
                    } else if (ch == '\\' && quoteChar != ']') {
                        // Backslash escaping does not apply inside SQL Server [bracket] identifiers (only ]] does).
                        bsEscaped = !bsEscaped;
                    } else {
                        bsEscaped = false;
                    }
                } else if (ch == '-' && index < sqlLength - 1 && sql.charAt(index + 1) == '-') {
                    // Line comment (-- ...): always discarded (unlike block comments, the "Keep
                    // comments" marker does not preserve these). Skip to the end of the line.
                    if (!sb.isEmpty()) {
                        words.add(sb.toString());
                        sb.setLength(0);
                    }

                    while (++index < sqlLength) {
                        ch = sql.charAt(index);

                        if (ch == ENTER || ch == ENTER_2) {
                            index--; // back up so the newline is reprocessed by the outer loop as whitespace
                            break;
                        }
                    }
                } else if (ch == '#') {
                    // Classify the '#' ONCE per position. Previously the (potentially expensive,
                    // backward-scanning) hash-prefixed-identifier check ran twice for the same index:
                    // once via isHashCommentStart and again via isSeparator. The sub-checks below are
                    // ordered exactly as the isHashCommentStart + isSeparator pair resolved them.
                    if (index < sqlLength - 1 && sql.charAt(index + 1) == '{') {
                        // iBatis/MyBatis #{...} parameter marker: '#' is part of the token.
                        sb.append(ch);
                    } else if (isLikelyHashPrefixedIdentifier(sql, sqlLength, index)) {
                        // Hash-prefixed identifier (e.g. a temp table after FROM/JOIN/INTO/UPDATE/TABLE):
                        // '#' is part of the token.
                        sb.append(ch);
                    } else if ((temp = matchMultiCharSeparator(sql, sqlLength, index)) != null) {
                        // '#'-leading operator such as #>, #>> or ##.
                        if (!sb.isEmpty()) {
                            words.add(sb.toString());
                            sb.setLength(0);
                        }

                        words.add(temp);
                        index += temp.length() - 1;
                    } else { // MySQL hash line comment: always discarded. Skip to the end of the line.
                        if (!sb.isEmpty()) {
                            words.add(sb.toString());
                            sb.setLength(0);
                        }

                        while (++index < sqlLength) {
                            ch = sql.charAt(index);

                            if (ch == ENTER || ch == ENTER_2) {
                                index--; // back up so the newline is reprocessed by the outer loop as whitespace
                                break;
                            }
                        }
                    }
                } else if (ch == '/' && index < sqlLength - 1 && sql.charAt(index + 1) == '*') {
                    if (!sb.isEmpty()) {
                        words.add(sb.toString());
                        sb.setLength(0);
                    }

                    if (keepComments == -1) {
                        keepComments = Strings.startsWithIgnoreCase(sql, KEEP_COMMENTS) ? 1 : 0;
                    }

                    if (keepComments == 1) {
                        sb.append(ch);
                        // Consume the opening '*' so a comment body that starts with '/'
                        // (e.g. "/*/path*/") is not mistaken for the closing "*/".
                        sb.append(sql.charAt(++index));

                        while (++index < sqlLength) {
                            ch = sql.charAt(index);
                            sb.append(ch);

                            if (ch == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                                sb.append(sql.charAt(++index));

                                words.add(sb.toString());
                                sb.setLength(0);

                                break;
                            }
                        }
                    } else {
                        // Consume the opening '*' so a comment body that starts with '/'
                        // (e.g. "/*/path*/") is not mistaken for the closing "*/".
                        index++;

                        while (++index < sqlLength) {
                            ch = sql.charAt(index);

                            if (ch == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                                index++;
                                break;
                            }
                        }

                        appendSpaceAfterSkippedBlockCommentIfNeeded(sql, sqlLength, index, words);
                    }
                } else if ((temp = matchMultiCharSeparator(sql, sqlLength, index)) != null) {
                    // Multi-character operator (e.g. >=, <>, ->>, :=). Matched before the single-character
                    // separator lookup (same effective precedence as before, when isSeparator matched it
                    // and this branch re-matched it) so the match is computed only once.
                    if (!sb.isEmpty()) {
                        words.add(sb.toString());
                        sb.setLength(0);
                    }

                    words.add(temp);
                    index += temp.length() - 1;
                } else if (ch < 128 ? ASCII_SEPARATOR[ch] : separators.contains(ch)) {
                    if (!sb.isEmpty()) {
                        words.add(sb.toString());
                        sb.setLength(0);
                    }

                    if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                        if (!words.isEmpty() && !words.get(words.size() - 1).equals(SK.SPACE)) {
                            words.add(SK.SPACE);
                        }
                    } else {
                        words.add(String.valueOf(ch));
                    }
                } else {
                    sb.append(ch);

                    if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == '[') {
                        quoteChar = ch == '[' ? ']' : ch;
                        // Opening quote char is non-backslash -> first content char has even parity.
                        bsEscaped = false;
                    }
                }
            }

            if (!sb.isEmpty()) {
                words.add(sb.toString());
                sb.setLength(0);
            }

            return words;
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Finds the index of a specific word within a SQL statement, searching from the beginning
     * using case-insensitive matching. This is a convenience overload of
     * {@link #indexOfWord(String, String, int, boolean)} equivalent to
     * {@code indexOfWord(sql, word, 0, false)}.
     *
     * <p>Like the four-argument form, this method only reports positions where {@code word}
     * appears as a complete SQL token (or composite token such as {@code "LEFT JOIN"}), not where
     * it occurs as a substring of another identifier, and it skips matches inside line, hash and
     * block comments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int index = SqlParser.indexOfWord(sql, "ORDER BY");   // 40
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param word the word or composite keyword to find (must not be {@code null})
     * @return the index of the word if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code word} is {@code null}
     * @see #indexOfWord(String, String, int, boolean)
     */
    public static int indexOfWord(final String sql, final String word) {
        return indexOfWord(sql, word, 0, false);
    }

    /**
     * Finds the index of a specific word within a SQL statement, searching from the given position
     * using case-insensitive matching. This is a convenience overload of
     * {@link #indexOfWord(String, String, int, boolean)} equivalent to
     * {@code indexOfWord(sql, word, fromIndex, false)}.
     *
     * <p>Like the four-argument form, this method only reports positions where {@code word}
     * appears as a complete SQL token (or composite token such as {@code "LEFT JOIN"}), not where
     * it occurs as a substring of another identifier, and it skips matches inside line, hash and
     * block comments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int whereIndex = SqlParser.indexOfWord(sql, "WHERE", 0);   // 20
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param word the word or composite keyword to find (must not be {@code null})
     * @param fromIndex the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
     * @return the index of the word if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code word} is {@code null}
     * @see #indexOfWord(String, String, int, boolean)
     */
    public static int indexOfWord(final String sql, final String word, final int fromIndex) {
        return indexOfWord(sql, word, fromIndex, false);
    }

    /**
     * Finds the index of a specific word within a SQL statement starting from a given position.
     * This method is capable of finding both simple words and composite keywords (like "LEFT JOIN").
     * It respects SQL syntax rules, including quoted identifiers and case sensitivity options.
     *
     * <p>The method handles:</p>
     * <ul>
     *   <li>Simple words and operators</li>
     *   <li>Composite keywords (e.g., "GROUP BY", "LEFT JOIN"). Sub-words may be separated by
     *       any amount of whitespace and/or comments in the source SQL.</li>
     *   <li>Case-sensitive and case-insensitive matching</li>
     *   <li>Quoted identifiers (the entire quoted region is matched as a single token)</li>
     *   <li>Multi-character operators</li>
     * </ul>
     *
     * <p>Unlike {@link String#indexOf(String, int)}, this method only returns a position where
     * {@code word} appears as a <em>complete</em> SQL token (or composite token), not where it
     * occurs as a substring of another identifier; matches that fall inside line/hash/block
     * comments are skipped.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int index = SqlParser.indexOfWord(sql, "ORDER BY", 0, false);
     * // Returns: 40 (the position where "ORDER BY" starts)
     *
     * int whereIndex = SqlParser.indexOfWord(sql, "WHERE", 0, false);
     * // Returns: 20 (the position where "WHERE" starts)
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param word the word or composite keyword to find (must not be {@code null})
     * @param fromIndex the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
     * @param caseSensitive whether the search should be case-sensitive
     * @return the index of the word if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code word} is {@code null}
     */
    public static int indexOfWord(final String sql, final String word, final int fromIndex, final boolean caseSensitive) {
        String[] subWords = compositeWords.get(word);

        if (subWords == null) {
            subWords = WORD_SPLITTER.splitToArray(word);
        }

        //noinspection IfStatementWithIdenticalBranches
        if (N.len(subWords) <= 1) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                int result = N.INDEX_NOT_FOUND;
                final int sqlLength = sql.length();
                final int startIndex = Math.max(0, fromIndex);
                String temp = "";
                char quoteChar = 0;
                // Forward-running backslash parity (see parse()): true if the char at the current
                // `index` is preceded by an ODD number of consecutive backslashes.
                boolean bsEscaped = false;

                for (int index = 0; index < sqlLength; index++) {
                    final char ch = sql.charAt(index);

                    // is it in a quoted identifier?
                    if (quoteChar != 0) {
                        sb.append(ch);

                        // end in quote.
                        if (ch == quoteChar) {
                            if (bsEscaped) {
                                // Escaped closing quote: stays in the string. Checked before the
                                // doubled-quote case so an escaped quote immediately followed by
                                // another quote is not mis-read as a doubled-quote pair.
                                bsEscaped = false;
                            } else if (index < sqlLength - 1 && sql.charAt(index + 1) == quoteChar) {
                                sb.append(sql.charAt(++index));
                                bsEscaped = false;
                            } else {
                                // Even count (including 0) of preceding backslashes -> NOT escaped.
                                temp = sb.toString();

                                final int matchStart = index - word.length() + 1;

                                if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                    result = matchStart;

                                    break;
                                }

                                sb.setLength(0);
                                quoteChar = 0;
                                bsEscaped = false;
                            }
                        } else if (ch == '\\' && quoteChar != ']') {
                            // Backslash escaping does not apply inside SQL Server [bracket] identifiers (only ]] does).
                            bsEscaped = !bsEscaped;
                        } else {
                            bsEscaped = false;
                        }
                    } else if (ch == '-' && index < sqlLength - 1 && sql.charAt(index + 1) == '-') {
                        // Skip single-line comment (-- ...)
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - word.length();

                            if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                result = matchStart;
                                break;
                            }
                            sb.setLength(0);
                        }
                        while (++index < sqlLength) {
                            final char cc = sql.charAt(index);
                            if (cc == ENTER || cc == ENTER_2) {
                                break;
                            }
                        }
                    } else if (isHashCommentStart(sql, sqlLength, index)) {
                        // Skip MySQL single-line comment (# ...)
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - word.length();

                            if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                result = matchStart;
                                break;
                            }
                            sb.setLength(0);
                        }
                        while (++index < sqlLength) {
                            final char cc = sql.charAt(index);
                            if (cc == ENTER || cc == ENTER_2) {
                                break;
                            }
                        }
                    } else if (ch == '/' && index < sqlLength - 1 && sql.charAt(index + 1) == '*') {
                        // Skip block comment (/* ... */)
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - word.length();

                            if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                result = matchStart;
                                break;
                            }
                            sb.setLength(0);
                        }
                        // Consume the opening '*' so a comment body that starts with '/'
                        // (e.g. "/*/x*/") is not mistaken for the closing "*/".
                        index++;
                        while (++index < sqlLength) {
                            final char cc = sql.charAt(index);
                            if (cc == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                                index++;
                                break;
                            }
                        }
                    } else if (isSeparator(sql, sqlLength, index, ch)) {
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - word.length();

                            if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                result = matchStart;

                                break;
                            }

                            sb.setLength(0);
                        } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                            // skip white char
                            continue;
                        }

                        temp = matchMultiCharSeparator(sql, sqlLength, index);

                        if (temp != null) {
                            if (index >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                                result = index;

                                break;
                            }

                            index += temp.length() - 1;
                        } else if (index >= startIndex && (word.equals(String.valueOf(ch)) || (!caseSensitive && word.equalsIgnoreCase(String.valueOf(ch))))) {
                            result = index;

                            break;
                        }
                    } else {
                        sb.append(ch);

                        if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == '[') {
                            quoteChar = ch == '[' ? ']' : ch;
                            bsEscaped = false;
                        }
                    }
                }

                if (result < 0 && !sb.isEmpty()) {
                    temp = sb.toString();
                    final int matchStart = sqlLength - word.length();

                    if (matchStart >= startIndex && (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp)))) {
                        result = matchStart;
                    }
                }

                return result;
            } finally {
                Objectory.recycle(sb);
            }
        } else {
            int result = indexOfWord(sql, subWords[0], fromIndex, caseSensitive);

            while (result >= 0) {
                int tmpIndex = result + subWords[0].length();
                boolean matched = true;

                for (int i = 1; i < subWords.length; i++) {
                    final String nextWord = nextWord(sql, tmpIndex);

                    if (Strings.isNotEmpty(nextWord) && (nextWord.equals(subWords[i]) || (!caseSensitive && nextWord.equalsIgnoreCase(subWords[i])))) {
                        // Use indexOfWord to skip whitespace AND block/line comments between subwords
                        final int subWordPos = indexOfWord(sql, subWords[i], tmpIndex, caseSensitive);

                        if (subWordPos < 0) {
                            matched = false;
                            break;
                        }

                        tmpIndex = subWordPos + subWords[i].length();
                    } else {
                        matched = false;

                        break;
                    }
                }

                if (matched) {
                    return result;
                }

                // First sub-word matched but subsequent words didn't; continue searching from after the current match
                result = indexOfWord(sql, subWords[0], result + subWords[0].length(), caseSensitive);
            }

            return result;
        }
    }

    /**
     * Extracts the next word or token from a SQL statement starting at the specified index.
     * This method skips leading whitespace and returns the next meaningful token,
     * which could be a keyword, identifier, operator, or separator.
     *
     * <p>The method handles:</p>
     * <ul>
     *   <li>Whitespace skipping (only between tokens; once a token has begun accumulating,
     *       a comment or whitespace ends it).</li>
     *   <li>Comment skipping when encountered before any token character (line {@code -- ...},
     *       MySQL hash {@code # ...}, and block {@code /* ... *}{@code /} comments are all skipped).</li>
     *   <li>Quoted identifiers (the entire quoted string, including the quotes, is returned).</li>
     *   <li>Multi-character operators (e.g., {@code >=}, {@code !=}).</li>
     *   <li>Single-character tokens.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT   name,   age FROM users";
     * String word1 = SqlParser.nextWord(sql, 6);    // Returns: "name" (skips spaces after SELECT)
     * String word2 = SqlParser.nextWord(sql, 13);   // Returns: ","
     * String word3 = SqlParser.nextWord(sql, 14);   // Returns: "age" (skips spaces after comma)
     * }</pre>
     *
     * @param sql the SQL statement to extract the word from (must not be {@code null})
     * @param fromIndex the starting position for extraction (0-based); negative values are treated as {@code 0}
     * @return the next word or token found, or an empty string if no more tokens exist
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    public static String nextWord(final String sql, final int fromIndex) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            String temp = "";
            char quoteChar = 0;
            // Forward-running backslash parity (see parse()): true if the char at the current
            // `index` is preceded by an ODD number of consecutive backslashes.
            boolean bsEscaped = false;

            for (int index = Math.max(0, fromIndex); index < sqlLength; index++) {
                final char ch = sql.charAt(index);

                // is it in a quoted identifier?
                if (quoteChar != 0) {
                    sb.append(ch);

                    // end in quote.
                    if (ch == quoteChar) {
                        if (bsEscaped) {
                            // Escaped closing quote: stays in the string. Checked before the
                            // doubled-quote case so an escaped quote immediately followed by
                            // another quote is not mis-read as a doubled-quote pair.
                            bsEscaped = false;
                        } else if (index < sqlLength - 1 && sql.charAt(index + 1) == quoteChar) {
                            sb.append(sql.charAt(++index));
                            bsEscaped = false;
                        } else {
                            // Even count (including 0) of preceding backslashes -> NOT escaped.
                            break;
                        }
                    } else if (ch == '\\' && quoteChar != ']') {
                        // Backslash escaping does not apply inside SQL Server [bracket] identifiers (only ]] does).
                        bsEscaped = !bsEscaped;
                    } else {
                        bsEscaped = false;
                    }
                } else if (ch == '-' && index < sqlLength - 1 && sql.charAt(index + 1) == '-') {
                    // Skip single-line comment (-- ...)
                    if (!sb.isEmpty()) {
                        break;
                    }
                    while (++index < sqlLength) {
                        final char cc = sql.charAt(index);
                        if (cc == ENTER || cc == ENTER_2) {
                            break;
                        }
                    }
                } else if (isHashCommentStart(sql, sqlLength, index)) {
                    // Skip MySQL single-line comment (# ...)
                    if (!sb.isEmpty()) {
                        break;
                    }
                    while (++index < sqlLength) {
                        final char cc = sql.charAt(index);
                        if (cc == ENTER || cc == ENTER_2) {
                            break;
                        }
                    }
                } else if (ch == '/' && index < sqlLength - 1 && sql.charAt(index + 1) == '*') {
                    // Skip block comment (/* ... */)
                    if (!sb.isEmpty()) {
                        break;
                    }
                    // Consume the opening '*' so a comment body that starts with '/'
                    // (e.g. "/*/x*/") is not mistaken for the closing "*/".
                    index++;
                    while (++index < sqlLength) {
                        final char cc = sql.charAt(index);
                        if (cc == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                            index++;
                            break;
                        }
                    }
                } else if (isSeparator(sql, sqlLength, index, ch)) {
                    if (!sb.isEmpty()) {
                        break;
                    } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                        // skip white char
                        continue;
                    }

                    temp = matchMultiCharSeparator(sql, sqlLength, index);

                    if (temp != null) {
                        sb.append(temp);
                    } else {
                        sb.append(ch);
                    }

                    break;
                } else {
                    sb.append(ch);

                    if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == '[') {
                        quoteChar = ch == '[' ? ']' : ch;
                        bsEscaped = false;
                    }
                }
            }

            return (sb.isEmpty()) ? "" : sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Returns the position just past the next word or token in a SQL statement, scanning from
     * the specified index. This is the position-returning companion to
     * {@link #nextWord(String, int)}: it applies the identical scanning rules (skipping leading
     * whitespace and comments, treating a quoted region or multi-character operator as one token)
     * but returns the end index of that token instead of its text. A caller can therefore obtain
     * the next token together with its bounds without re-scanning the string with
     * {@link String#indexOf(int)}.
     *
     * <p>The returned index is the offset of the first character <em>after</em> the next token,
     * suitable for passing back as the {@code fromIndex} of a subsequent call to advance through
     * the SQL. Equivalently, {@code sql.substring(returnedStart, nextWordEnd(sql, fromIndex))}
     * spans the same token returned by {@link #nextWord(String, int)} (where {@code returnedStart}
     * is the index of that token's first character).</p>
     *
     * <p>If no further token exists (only trailing whitespace and/or comments remain), the length
     * of {@code sql} is returned, consistent with {@link #nextWord(String, int)} returning an empty
     * string in that case.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT   name,   age FROM users";
     * int end1 = SqlParser.nextWordEnd(sql, 6);    // 13 (just past "name")
     * int end2 = SqlParser.nextWordEnd(sql, 13);   // 14 (just past ",")
     * int end3 = SqlParser.nextWordEnd(sql, 14);   // 20 (just past "age")
     * }</pre>
     *
     * @param sql the SQL statement to scan (must not be {@code null})
     * @param fromIndex the starting position for scanning (0-based); negative values are treated as {@code 0}
     * @return the index immediately after the next word or token, or the length of {@code sql} if no further token exists
     * @throws NullPointerException if {@code sql} is {@code null}
     * @see #nextWord(String, int)
     */
    public static int nextWordEnd(final String sql, final int fromIndex) {
        final int sqlLength = sql.length();

        // Mirrors nextWord's scan. `started` tracks whether any token character has been
        // accumulated yet (the analogue of nextWord's `!sb.isEmpty()` guard).
        boolean started = false;
        char quoteChar = 0;
        // Forward-running backslash parity (see parse()): true if the char at the current
        // `index` is preceded by an ODD number of consecutive backslashes.
        boolean bsEscaped = false;

        for (int index = Math.max(0, fromIndex); index < sqlLength; index++) {
            final char ch = sql.charAt(index);

            // is it in a quoted identifier?
            if (quoteChar != 0) {
                // end in quote.
                if (ch == quoteChar) {
                    if (bsEscaped) {
                        bsEscaped = false;
                    } else if (index < sqlLength - 1 && sql.charAt(index + 1) == quoteChar) {
                        index++;
                        bsEscaped = false;
                    } else {
                        return index + 1;
                    }
                } else if (ch == '\\' && quoteChar != ']') {
                    // Backslash escaping does not apply inside SQL Server [bracket] identifiers (only ]] does).
                    bsEscaped = !bsEscaped;
                } else {
                    bsEscaped = false;
                }
            } else if (ch == '-' && index < sqlLength - 1 && sql.charAt(index + 1) == '-') {
                // Skip single-line comment (-- ...)
                if (started) {
                    return index;
                }
                while (++index < sqlLength) {
                    final char cc = sql.charAt(index);
                    if (cc == ENTER || cc == ENTER_2) {
                        break;
                    }
                }
            } else if (isHashCommentStart(sql, sqlLength, index)) {
                // Skip MySQL single-line comment (# ...)
                if (started) {
                    return index;
                }
                while (++index < sqlLength) {
                    final char cc = sql.charAt(index);
                    if (cc == ENTER || cc == ENTER_2) {
                        break;
                    }
                }
            } else if (ch == '/' && index < sqlLength - 1 && sql.charAt(index + 1) == '*') {
                // Skip block comment (/* ... */)
                if (started) {
                    return index;
                }
                // Consume the opening '*' so a comment body that starts with '/'
                // (e.g. "/*/x*/") is not mistaken for the closing "*/".
                index++;
                while (++index < sqlLength) {
                    final char cc = sql.charAt(index);
                    if (cc == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                        index++;
                        break;
                    }
                }
            } else if (isSeparator(sql, sqlLength, index, ch)) {
                if (started) {
                    return index;
                } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                    // skip white char
                    continue;
                }

                final String temp = matchMultiCharSeparator(sql, sqlLength, index);

                return temp != null ? index + temp.length() : index + 1;
            } else {
                started = true;

                if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == '[') {
                    quoteChar = ch == '[' ? ']' : ch;
                    bsEscaped = false;
                }
            }
        }

        return sqlLength;
    }

    /**
     * Registers a single character as a SQL separator.
     * Once registered, this character will be recognized as a token separator
     * during SQL parsing operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlParser.registerSeparator('$');   // Register $ as a separator
     * List<String> words = SqlParser.parse("SELECT$FROM$users");
     * // Result: ["SELECT", "$", "FROM", "$", "users"]
     * }</pre>
     *
     * @param separator the character to register as a separator
     * @see #registerSeparator(String)
     * @see #unregisterSeparator(char)
     * @see #resetSeparators()
     */
    public static void registerSeparator(final char separator) {
        separators.add(separator);

        if (separator < 128) {
            rebuildAsciiSeparatorTable();
        }
    }

    /**
     * Registers a string as a SQL separator.
     * This can be used to register multi-character operators or separators
     * that should be recognized as single tokens during parsing.
     *
     * <p>If the separator is a single character, it will also be registered
     * as a character separator for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlParser.registerSeparator("<=>");   // Register the NULL-safe equal operator
     * SqlParser.registerSeparator("::");    // Register PostgreSQL cast operator
     * }</pre>
     *
     * @param separator the string to register as a separator (must not be {@code null} or empty)
     * @throws IllegalArgumentException if {@code separator} is {@code null} or empty
     * @see #registerSeparator(char)
     * @see #unregisterSeparator(String)
     * @see #resetSeparators()
     */
    public static void registerSeparator(final String separator) {
        N.checkArgNotEmpty(separator, "separator");

        separators.add(separator);

        if (separator.length() == 1) {
            separators.add(separator.charAt(0));

            if (separator.charAt(0) < 128) {
                rebuildAsciiSeparatorTable();
            }
        } else {
            rebuildMultiCharSeparatorTable();
        }

        if (separator.length() > maxSeparatorLength.get()) {
            int currentMax = maxSeparatorLength.get();

            while (separator.length() > currentMax && !maxSeparatorLength.compareAndSet(currentMax, separator.length())) {
                currentMax = maxSeparatorLength.get();
            }
        }
    }

    /**
     * Unregisters a previously registered separator character, removing it from the
     * recognized separator set. This is the inverse of {@link #registerSeparator(char)};
     * both the character form and the equivalent single-character {@code String} form are
     * removed, and all derived lookup tables are rebuilt.
     *
     * <p>Unregistering a separator that is not currently registered has no effect.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlParser.registerSeparator('$');     // Register custom separator
     * SqlParser.unregisterSeparator('$');   // ...then remove it again
     * }</pre>
     *
     * @param separator the character to unregister as a separator
     * @see #unregisterSeparator(String)
     */
    public static void unregisterSeparator(final char separator) {
        unregisterSeparator(String.valueOf(separator));
    }

    /**
     * Unregisters a previously registered separator, removing it from the recognized
     * separator set. This is the inverse of {@link #registerSeparator(String)} and
     * {@link #registerSeparator(char)} and can be used to undo a registration so the
     * given token is no longer treated as a separator during parsing.
     *
     * <p>If {@code separator} is a single character, both its {@code String} and character
     * forms are removed (mirroring the dual registration performed by
     * {@link #registerSeparator(String)} for single-character separators). After removal,
     * all derived lookup tables ({@link #maxSeparatorLength}, the ASCII lookup and the
     * multi-character tables) are rebuilt so they cannot drift.</p>
     *
     * <p><b>Note:</b> This method does not distinguish between built-in default separators
     * and user-registered ones; unregistering a default separator removes it just the same.
     * To restore the original built-in set, use {@link #resetSeparators()}.</p>
     *
     * <p>Unregistering a separator that is not currently registered has no effect.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlParser.registerSeparator("::");     // Register PostgreSQL cast operator
     * SqlParser.unregisterSeparator("::");   // ...then remove it again
     * }</pre>
     *
     * @param separator the separator to unregister (must not be {@code null} or empty)
     * @throws IllegalArgumentException if {@code separator} is {@code null} or empty
     */
    public static void unregisterSeparator(final String separator) {
        N.checkArgNotEmpty(separator, "separator");

        separators.remove(separator);

        if (separator.length() == 1) {
            separators.remove(separator.charAt(0));
        }

        recomputeMaxSeparatorLength();
        rebuildAsciiSeparatorTable();
        rebuildMultiCharSeparatorTable();
    }

    /**
     * Restores the separator set to the built-in defaults, discarding every separator added
     * via {@link #registerSeparator(char)} / {@link #registerSeparator(String)} and re-adding
     * any default separator that was removed via {@link #unregisterSeparator(String)}.
     *
     * <p>This rebuilds the internal separator set and all derived lookup tables
     * ({@link #maxSeparatorLength}, the ASCII lookup and the multi-character tables) to exactly
     * the state established when the class was first loaded. It is primarily useful for
     * undoing process-global separator customizations (for example, to isolate tests that
     * register custom separators).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlParser.registerSeparator("::");   // customize parsing globally
     * // ... do work ...
     * SqlParser.resetSeparators();         // restore the original built-in separators
     * }</pre>
     */
    public static void resetSeparators() {
        loadDefaultSeparators();
    }

    /**
     * Checks if a character at a specific position in a SQL string is a separator.
     * This method performs context-aware checking, handling special cases like
     * MyBatis/iBatis parameter markers (#{...}).
     *
     * <p>Special handling:</p>
     * <ul>
     *   <li>{@code #} followed by <code>{</code> is not considered a separator (MyBatis/iBatis {@code #{...}} syntax)</li>
     *   <li>{@code #} that starts a hash-prefixed identifier (e.g. a temp table name appearing
     *       after {@code FROM}, {@code JOIN}, {@code INTO}, {@code UPDATE} or {@code TABLE},
     *       with optional whitespace/comments in between, including as a later element of a
     *       comma-separated list such as {@code "FROM #t1, #t2"}) is not considered a separator</li>
     *   <li>All registered single-character and multi-character separators are checked</li>
     * </ul>
     *
     * <p>Behavior (internal helper): for the SQL {@code "SELECT * FROM users"}, the {@code '*'} at index 7
     * and the space at index 6 are both separators, while the {@code '#'} in a {@code #{...}} marker is not.</p>
     *
     * <p>The method may inspect characters surrounding {@code index} (notably the next char to
     * disambiguate {@code #{...}}, and previous characters to detect a hash-prefixed identifier
     * context), in addition to checking {@code ch} against the registered separator set.</p>
     *
     * @param str the SQL string being parsed
     * @param len the exclusive upper bound for scanning (normally the length of {@code str})
     * @param index the current position in the string (0-based)
     * @param ch the character to check; expected to equal {@code str.charAt(index)}
     * @return {@code true} if the character is a separator in this context, {@code false} otherwise
     */
    static boolean isSeparator(final String str, final int len, final int index, final char ch) {
        // for Ibatis/Mybatis
        if (ch == '#' && index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        if (ch == '#' && isLikelyHashPrefixedIdentifier(str, len, index)) {
            return false;
        }

        if (ch < 128 ? ASCII_SEPARATOR[ch] : separators.contains(ch)) {
            return true;
        }

        return matchMultiCharSeparator(str, len, index) != null;
    }

    private static void appendSpaceAfterSkippedBlockCommentIfNeeded(final String sql, final int sqlLength, final int commentEndIndex,
            final List<String> words) {
        final int nextIndex = commentEndIndex + 1;

        if (nextIndex >= sqlLength || words.isEmpty() || SK.SPACE.equals(words.get(words.size() - 1))) {
            return;
        }

        final char nextChar = sql.charAt(nextIndex);

        if (!Character.isWhitespace(nextChar) && !isSeparator(sql, sqlLength, nextIndex, nextChar)) {
            words.add(SK.SPACE);
        }
    }

    private static boolean isHashCommentStart(final String str, final int len, final int index) {
        if (str.charAt(index) != '#') {
            return false;
        }

        // for Ibatis/Mybatis
        if (index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        final String multiCharSeparator = matchMultiCharSeparator(str, len, index);
        if (multiCharSeparator != null && multiCharSeparator.length() > 1) {
            return false;
        }

        return !isLikelyHashPrefixedIdentifier(str, len, index);
    }

    /**
     * Decides whether the {@code '#'} at {@code index} starts a hash-prefixed identifier (e.g. a
     * SQL Server temp table) rather than a MySQL hash comment. The character following the
     * {@code '#'} must be an identifier character, and scanning backward (skipping whitespace and
     * comments) must reach one of the {@link #hashIdentifierContextKeywords} ({@code FROM},
     * {@code JOIN}, {@code INTO}, {@code UPDATE}, {@code TABLE}).
     *
     * <p>A comma is treated as a list continuation: for {@code "FROM #t1, #t2"} the backward walk
     * skips the comma plus the single list element before it (a possibly qualified, quoted or
     * '#'-prefixed name with an optional alias word) and re-checks from there, so every element of
     * a comma-separated list governed by a context keyword is recognized. Anything else anchors
     * the decision: a context keyword means identifier, any other word or character (e.g.
     * {@code SELECT}, {@code '('}, {@code '='}) means comment, so {@code "SELECT a, #comment"}
     * remains a MySQL comment. Ambiguous shapes deliberately fall back to the comment
     * classification.</p>
     */
    private static boolean isLikelyHashPrefixedIdentifier(final String str, final int len, final int index) {
        if (index >= len - 1) {
            return false;
        }

        final char next = str.charAt(index + 1);

        if (!isIdentifierChar(next)) {
            return false;
        }

        int left = skipBackwardWhitespaceAndComments(str, index - 1);

        // Walk backward over ","-separated list elements until something other than a list
        // continuation anchors the decision. Only the first skip above is line-comment-aware;
        // the per-element steps skip whitespace and block comments only (a line comment between
        // list elements conservatively yields the comment classification, as before this walk
        // existed). Each iteration consumes at least the comma (progress is checked), so the
        // loop terminates.
        while (left >= 0) {
            final char ch = str.charAt(left);

            if (ch == ',') {
                final int beforeComma = skipBackwardWhitespaceAndBlockComments(str, left - 1);
                final int beforeElement = skipBackwardListElement(str, beforeComma);

                if (beforeElement >= beforeComma) {
                    // No recognizable list element before the ',' -> not an identifier list.
                    return false;
                }

                left = skipBackwardWhitespaceAndBlockComments(str, beforeElement);
                continue;
            }

            if (!isIdentifierChar(ch)) {
                return false;
            }

            int wordStart = left;

            while (wordStart >= 0 && isIdentifierChar(str.charAt(wordStart))) {
                wordStart--;
            }

            final String prevWord = str.substring(wordStart + 1, left + 1).toUpperCase(Locale.ROOT);
            return hashIdentifierContextKeywords.contains(prevWord);
        }

        return false;
    }

    /**
     * Consumes one comma-separated list element backward, starting at {@code start} (which must
     * already be positioned on a non-whitespace character), for the list walk in
     * {@link #isLikelyHashPrefixedIdentifier}. An element is at most two whitespace-separated
     * units (a name plus an optional trailing alias, with a free {@code AS} between them) where
     * each unit is a dot-qualified chain of segments and each segment is an identifier word
     * (optionally '#'-prefixed) or a quoted/bracket-quoted identifier.
     *
     * <p>Returns the index of the first character before the consumed element (possibly
     * {@code -1}), or {@code start} unchanged if no element was recognized there. A hash-identifier
     * context keyword is never consumed: it is left in place for the caller to classify, so
     * {@code "FROM t1, #t2"} stops in front of {@code FROM} after consuming {@code t1}.</p>
     */
    private static int skipBackwardListElement(final String str, final int start) {
        int left = start;
        int units = 0;

        outer: while (left >= 0 && units < 2) {
            final int unitStart = left;

            // Consume one unit: dot-joined segments, scanned backward.
            while (true) {
                final char ch = str.charAt(left);

                if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == ']') {
                    // Quoted / bracket-quoted identifier segment: skip back to the opening quote.
                    // (Escaped/doubled closing quotes are not un-escaped here; a mismatch makes the
                    // walk stop early, which conservatively yields the comment classification.)
                    final char openChar = ch == ']' ? '[' : ch;
                    int quoteStart = left - 1;

                    while (quoteStart >= 0 && str.charAt(quoteStart) != openChar) {
                        quoteStart--;
                    }

                    if (quoteStart < 0) {
                        break outer; // unbalanced quote -> not a recognizable element
                    }

                    left = quoteStart - 1;
                } else if (isIdentifierChar(ch)) {
                    int wordStart = left;

                    while (wordStart >= 0 && isIdentifierChar(str.charAt(wordStart))) {
                        wordStart--;
                    }

                    final String word = str.substring(wordStart + 1, left + 1).toUpperCase(Locale.ROOT);

                    if (hashIdentifierContextKeywords.contains(word)) {
                        break outer; // the anchor keyword; leave it for the caller to classify
                    }

                    if ("AS".equals(word) && left == unitStart) {
                        // "name AS alias": the AS keyword does not count against the unit budget.
                        left = skipBackwardWhitespaceAndBlockComments(str, wordStart);
                        continue outer;
                    }

                    left = wordStart;

                    if (left >= 0 && str.charAt(left) == '#') {
                        left--; // '#'-prefixed segment: the '#' belongs to the identifier
                    }
                } else {
                    break outer; // e.g. '(' or '=' -> not part of a list element
                }

                if (left >= 0 && str.charAt(left) == '.') {
                    left--; // dot-qualified name: the qualifier segment belongs to the same unit
                    continue;
                }

                break; // unit complete
            }

            units++;

            final int beforeGap = skipBackwardWhitespaceAndBlockComments(str, left);

            if (beforeGap < 0 || str.charAt(beforeGap) == ',') {
                break; // element complete (next list continuation or start of input reached)
            }

            left = beforeGap; // a second unit (the name before an alias word) may follow
        }

        return left;
    }

    private static int skipBackwardWhitespaceAndComments(final String str, int left) {
        boolean skipped;
        int lastLineScanPosition = Integer.MIN_VALUE;

        do {
            skipped = false;

            final int beforeWhitespaceAndBlockComments = left;
            left = skipBackwardWhitespaceAndBlockComments(str, left);

            if (left != beforeWhitespaceAndBlockComments) {
                skipped = true;
            }

            // The line-comment scan below is the expensive part (it re-walks the current line from
            // its start). It is a pure function of (str, left), so re-running it at an unchanged
            // position cannot find anything new: only run it when `left` moved since the last scan.
            // (For left < 0 it trivially finds nothing, so it is skipped as well.)
            if (left >= 0 && left != lastLineScanPosition) {
                lastLineScanPosition = left;

                final int lineStart = lastLineStart(str, left);
                final int commentIndex = lastLineCommentStart(str, lineStart, left);

                if (commentIndex >= 0) {
                    left = commentIndex - 1;
                    skipped = true;
                }
            }
        } while (skipped);

        return left;
    }

    private static int skipBackwardWhitespaceAndBlockComments(final String str, int left) {
        boolean skipped;

        do {
            skipped = false;

            while (left >= 0 && Character.isWhitespace(str.charAt(left))) {
                left--;
                skipped = true;
            }

            if (left >= 1 && str.charAt(left) == '/' && str.charAt(left - 1) == '*') {
                left -= 2;

                while (left >= 1 && !(str.charAt(left) == '*' && str.charAt(left - 1) == '/')) {
                    left--;
                }

                left = left >= 1 ? left - 2 : -1;
                skipped = true;
            }
        } while (skipped);

        return left;
    }

    private static int lastLineStart(final String str, final int fromIndex) {
        int index = fromIndex;

        while (index >= 0) {
            final char ch = str.charAt(index);

            if (ch == ENTER || ch == ENTER_2) {
                return index + 1;
            }

            index--;
        }

        return 0;
    }

    private static int lastLineCommentStart(final String str, final int fromIndex, final int toIndex) {
        char quoteChar = 0;
        boolean bsEscaped = false;
        boolean inBracketQuotedIdentifier = false;

        for (int i = fromIndex; i <= toIndex; i++) {
            final char ch = str.charAt(i);

            if (quoteChar != 0) {
                if (ch == quoteChar) {
                    if (bsEscaped) {
                        bsEscaped = false;
                    } else if (i < toIndex && str.charAt(i + 1) == quoteChar) {
                        i++;
                        bsEscaped = false;
                    } else {
                        quoteChar = 0;
                    }
                } else if (ch == '\\') {
                    bsEscaped = !bsEscaped;
                } else {
                    bsEscaped = false;
                }

                continue;
            }

            if (inBracketQuotedIdentifier) {
                if (ch == ']') {
                    if (i < toIndex && str.charAt(i + 1) == ']') {
                        i++;
                    } else {
                        inBracketQuotedIdentifier = false;
                    }
                }

                continue;
            }

            if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK) {
                quoteChar = ch;
                bsEscaped = false;
            } else if (ch == '[') {
                inBracketQuotedIdentifier = true;
            } else if (ch == '/' && i < toIndex && str.charAt(i + 1) == '*') {
                // Skip the entire block comment so a '#' or '--' appearing inside it is not
                // mistaken for a line-comment start during the backward scan (which would
                // derail the hash-prefixed-identifier heuristic and swallow the identifier).
                i += 2; // past the opening "/*"
                while (i < toIndex && !(str.charAt(i) == '*' && str.charAt(i + 1) == '/')) {
                    i++;
                }
                // i now rests on the '*' of the closing "*/" (or at toIndex if unclosed);
                // the for-loop's i++ advances past it.
            } else if (ch == '-' && i < toIndex && str.charAt(i + 1) == '-') {
                return i;
            } else if (ch == '#' && isHashLineCommentStartForBackwardScan(str, i)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isHashLineCommentStartForBackwardScan(final String str, final int index) {
        if (index < str.length() - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        final String multiCharSeparator = matchMultiCharSeparator(str, str.length(), index);
        if (multiCharSeparator != null && multiCharSeparator.length() > 1) {
            return false;
        }

        return !isLikelyHashPrefixedIdentifierAfterWhitespaceAndBlockComments(str, str.length(), index);
    }

    private static boolean isLikelyHashPrefixedIdentifierAfterWhitespaceAndBlockComments(final String str, final int len, final int index) {
        if (index >= len - 1) {
            return false;
        }

        final char next = str.charAt(index + 1);

        if (!isIdentifierChar(next)) {
            return false;
        }

        int left = skipBackwardWhitespaceAndBlockComments(str, index - 1);

        if (left < 0) {
            return false;
        }

        int end = left;

        while (left >= 0 && isIdentifierChar(str.charAt(left))) {
            left--;
        }

        if (end < left + 1) {
            return false;
        }

        final String prevWord = str.substring(left + 1, end + 1).toUpperCase(Locale.ROOT);
        return hashIdentifierContextKeywords.contains(prevWord);
    }

    private static boolean isIdentifierChar(final char ch) {
        return ch == '_' || ch == '$' || Character.isLetterOrDigit(ch);
    }

    private static String matchMultiCharSeparator(final String str, final int len, final int index) {
        if (index < len) {
            final char first = str.charAt(index);

            // Fast reject: no registered multi-character separator starts with this character.
            if (first < 128 ? !MULTI_CHAR_SEPARATOR_FIRST_CHAR[first] : !multiCharSeparatorNonAsciiFirstChar) {
                return null;
            }
        }

        final String[][] byLen = multiCharSeparatorsByLen;
        // Same cap as before: longest registered separator vs. remaining input length.
        int maxLen = Math.min(maxSeparatorLength.get(), len - index);

        if (maxLen > byLen.length - 1) {
            maxLen = byLen.length - 1;
        }

        // Longest match first, identical to the previous substring + Set.contains probe order,
        // but compares characters directly so no String is allocated per probe.
        for (int sepLen = maxLen; sepLen > 1; sepLen--) {
            final String[] candidates = byLen[sepLen];

            outer: for (int ci = 0, cn = candidates.length; ci < cn; ci++) {
                final String candidate = candidates[ci];

                for (int k = 0; k < sepLen; k++) {
                    if (str.charAt(index + k) != candidate.charAt(k)) {
                        continue outer;
                    }
                }

                return candidate;
            }
        }

        return null;
    }

    /**
     * Determines if a word at a specific position in a parsed word list represents a function name.
     * A word is considered a function name if it is followed by the opening parenthesis token,
     * either immediately or after whitespace. Multi-character separators that merely start with
     * {@code '('}, such as Oracle's outer-join marker {@code (+)}, are not function-call markers.
     * Space tokens and invalid indices are never considered function names.
     *
     * <p>This method is useful for identifying SQL function calls during parsing or analysis.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> tokens = SqlParser.parse("SELECT COUNT(*) FROM users");
     * boolean isFunc = SqlParser.isFunctionName(tokens, 2);   // true for "COUNT"
     * boolean notFunc = SqlParser.isFunctionName(tokens, 0);  // false for "SELECT"
     * }</pre>
     *
     * @param tokens the parsed SQL tokens (typically the result of {@link #parse(String)})
     * @param index the index of the word to check; invalid indices return {@code false}
     * @return {@code true} if the word at {@code index} is followed (after zero or more space tokens)
     *         by the {@code "("} token; {@code false} otherwise
     * @throws NullPointerException if {@code words} is {@code null}
     */
    public static boolean isFunctionName(final List<String> tokens, final int index) {
        return isFunctionName(tokens, tokens.size(), index);
    }

    /**
     * Determines if a word at a specific position in a parsed word list represents a function name,
     * examining only the tokens below the given exclusive upper bound.
     *
     * @param tokens the parsed SQL tokens (typically the result of {@link #parse(String)})
     * @param len the exclusive upper bound to search within {@code words} (usually {@code words.size()};
     *            indices {@code >= len} are not examined; values above {@code words.size()} are capped
     *            at {@code words.size()})
     * @param index the index of the word to check; invalid indices return {@code false}
     * @return {@code true} if the word at {@code index} is followed (after zero or more space tokens)
     *         by the {@code "("} token; {@code false} otherwise
     * @throws NullPointerException if {@code words} is {@code null}
     * @deprecated use {@link #isFunctionName(List, int)}
     */
    @Deprecated
    public static boolean isFunctionName(final List<String> tokens, final int len, final int index) {
        final int upperBound = Math.min(len, tokens.size());

        if (index < 0 || index >= upperBound) {
            return false;
        }

        if (SK.SPACE.equals(tokens.get(index))) {
            return false;
        }

        for (int i = index + 1; i < upperBound; i++) {
            String word = tokens.get(i);
            if (SK.PARENTHESIS_L.equals(word)) {
                return true;
            } else if (!SK.SPACE.equals(word)) {
                return false;
            }
        }

        return false;
    }

    /**
     * Checks if the given SQL statement is a SELECT query.
     * <p>
     * This method performs a case-insensitive check on the leading SQL keyword (after skipping
     * any leading whitespace, line comments {@code --}/{@code #} and block comments
     * {@code /}{@code * ... *}{@code /}). Any leading parentheses are skipped as well, so a
     * parenthesized query such as {@code (SELECT ...) UNION ALL (SELECT ...)} is still recognized
     * as a SELECT. For statements that start with a {@code WITH} (CTE) clause, the keyword that
     * follows the CTE definitions is examined instead.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid SELECT queries
     * boolean result1 = SqlParser.isSelectQuery("SELECT * FROM users");
     * // result1 = true
     *
     * boolean result2 = SqlParser.isSelectQuery("select id, name from products");
     * // result2 = true
     *
     * boolean result3 = SqlParser.isSelectQuery("  SELECT count(*) FROM orders");
     * // result3 = true
     *
     * // Non-SELECT queries
     * boolean result4 = SqlParser.isSelectQuery("UPDATE users SET name = 'John'");
     * // result4 = false
     *
     * boolean result5 = SqlParser.isSelectQuery("INSERT INTO users VALUES (1, 'John')");
     * // result5 = false
     * }</pre>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL is a SELECT query, {@code false} otherwise
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isSelectQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return "SELECT".equalsIgnoreCase(getLeadingQueryKeyword(sql));
    }

    /**
     * Checks whether the given SQL statement is read-only, i.e. a SELECT that performs no data
     * mutation. This is the kind of gate a read-only DAO can use to reject any statement that could
     * modify data.
     * <p>
     * A statement is considered read-only only if its leading keyword is {@code SELECT}
     * (see {@link #isSelectQuery(String)}) <i>and</i> it contains no top-level mutation or DDL keyword
     * ({@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code MERGE}, {@code REPLACE}, {@code TRUNCATE},
     * {@code CREATE}, {@code ALTER} or {@code DROP}) and no standalone
     * {@code SELECT ... INTO ...} clause. The {@code INTO} check is limited to the SELECT list
     * before that SELECT's {@code FROM}; table names after {@code FROM} and qualified identifiers
     * such as {@code t.into} do not count as {@code SELECT ... INTO}. Keyword matching ignores
     * occurrences inside quoted string literals, quoted identifiers, SQL comments and larger
     * identifier tokens, so a SELECT that merely returns the literal text {@code 'DELETE'} or a
     * column named {@code into$} is still treated as read-only, whereas a data-changing CTE such as
     * {@code WITH t AS (...) DELETE ...} is not. For multi-statement SQL, a later statement that
     * starts with one of the mutation or DDL keywords listed above also makes the SQL
     * non-read-only, including when that later statement starts with a {@code WITH} clause or
     * leading parentheses. The keyword scan matches only statement-start positions, so the
     * {@code REPLACE(...)}/{@code TRUNCATE(...)} SQL <i>functions</i> inside a SELECT do not
     * affect the classification.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL is a read-only SELECT query, {@code false} otherwise
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isReadOnlyQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return isSelectQuery(sql) && !containsMutationQueryKeyword(sql) && !containsSelectIntoClause(sql);
    }

    /**
     * Checks if the given SQL statement is an INSERT query.
     * <p>
     * This method performs a case-insensitive check on the leading SQL keyword (after skipping
     * any leading whitespace, line comments {@code --}/{@code #} and block comments
     * {@code /}{@code * ... *}{@code /}). Any leading parentheses are skipped as well. For
     * statements that start with a {@code WITH} (CTE) clause, the keyword that follows the CTE
     * definitions is examined instead.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid INSERT queries
     * boolean result1 = SqlParser.isInsertQuery("INSERT INTO users VALUES (1, 'John')");
     * // result1 = true
     *
     * boolean result2 = SqlParser.isInsertQuery("insert into products (name, price) values ('Widget', 9.99)");
     * // result2 = true
     *
     * boolean result3 = SqlParser.isInsertQuery("  INSERT INTO orders (order_id) VALUES (100)");
     * // result3 = true
     *
     * // Non-INSERT queries
     * boolean result4 = SqlParser.isInsertQuery("UPDATE users SET name = 'John'");
     * // result4 = false
     *
     * boolean result5 = SqlParser.isInsertQuery("SELECT * FROM users");
     * // result5 = false
     * }</pre>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL is an INSERT query, {@code false} otherwise
     * @see #isSelectQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isInsertQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return "INSERT".equalsIgnoreCase(getLeadingQueryKeyword(sql));
    }

    /**
     * Checks if the given SQL statement is an UPDATE query.
     * <p>
     * This method performs a case-insensitive check on the leading SQL keyword (after skipping
     * any leading whitespace, line comments {@code --}/{@code #} and block comments
     * {@code /}{@code * ... *}{@code /}). Any leading parentheses are skipped as well. For
     * statements that start with a {@code WITH} (CTE) clause, the keyword that follows the CTE
     * definitions is examined instead.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid UPDATE queries
     * boolean result1 = SqlParser.isUpdateQuery("UPDATE users SET name = 'John'");
     * // result1 = true
     *
     * boolean result2 = SqlParser.isUpdateQuery("update products set price = 9.99 where id = 1");
     * // result2 = true
     *
     * // Non-UPDATE queries
     * boolean result3 = SqlParser.isUpdateQuery("SELECT * FROM users");
     * // result3 = false
     *
     * boolean result4 = SqlParser.isUpdateQuery("DELETE FROM users WHERE id = 1");
     * // result4 = false
     * }</pre>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL is an UPDATE query, {@code false} otherwise
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isUpdateQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return "UPDATE".equalsIgnoreCase(getLeadingQueryKeyword(sql));
    }

    /**
     * Checks if the given SQL statement is a DELETE query.
     * <p>
     * This method performs a case-insensitive check on the leading SQL keyword (after skipping
     * any leading whitespace, line comments {@code --}/{@code #} and block comments
     * {@code /}{@code * ... *}{@code /}). Any leading parentheses are skipped as well. For
     * statements that start with a {@code WITH} (CTE) clause, the keyword that follows the CTE
     * definitions is examined instead.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid DELETE queries
     * boolean result1 = SqlParser.isDeleteQuery("DELETE FROM users WHERE id = 1");
     * // result1 = true
     *
     * boolean result2 = SqlParser.isDeleteQuery("delete from products where price < 1");
     * // result2 = true
     *
     * // Non-DELETE queries
     * boolean result3 = SqlParser.isDeleteQuery("SELECT * FROM users");
     * // result3 = false
     *
     * boolean result4 = SqlParser.isDeleteQuery("UPDATE users SET name = 'John'");
     * // result4 = false
     * }</pre>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL is a DELETE query, {@code false} otherwise
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isDeleteQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return "DELETE".equalsIgnoreCase(getLeadingQueryKeyword(sql));
    }

    /**
     * Checks whether the given SQL statement begins with an {@code INSERT OR REPLACE} clause
     * (the SQLite upsert form that overwrites an existing row when a uniqueness constraint is
     * violated).
     * <p>
     * Only the leading keywords are examined, after skipping any leading whitespace, line comments
     * ({@code --}/{@code #}), block comments ({@code /}{@code * ... *}{@code /}), any leading
     * parentheses and any leading {@code WITH} clause; the three keywords {@code INSERT}, {@code OR}
     * and {@code REPLACE} must
     * appear (case-insensitively) in that order at the start of the actual statement. A plain
     * {@code INSERT}, a SQL Server / Oracle standalone {@code REPLACE}, or any other leading
     * keyword returns {@code false}.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result1 = SqlParser.isInsertOrReplaceQuery("INSERT OR REPLACE INTO t (id) VALUES (1)");
     * // result1 = true
     *
     * boolean result2 = SqlParser.isInsertOrReplaceQuery("INSERT INTO t (id) VALUES (1)");
     * // result2 = false
     * }</pre>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL begins with {@code INSERT OR REPLACE}, {@code false} otherwise
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNoUpdateQuery(String)
     */
    public static boolean isInsertOrReplaceQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        int index = getLeadingQueryKeywordIndex(sql);

        if (index < 0) {
            return false;
        }

        String keyword = readKeyword(sql, index);

        if (!"INSERT".equalsIgnoreCase(keyword)) {
            return false;
        }

        index = skipLeadingWhitespaceAndComments(sql, index + keyword.length());
        keyword = readKeyword(sql, index);

        if (!"OR".equalsIgnoreCase(keyword)) {
            return false;
        }

        index = skipLeadingWhitespaceAndComments(sql, index + keyword.length());

        return "REPLACE".equalsIgnoreCase(readKeyword(sql, index));
    }

    /**
     * Checks whether the given SQL statement neither updates nor deletes existing rows. The name
     * deliberately mirrors the {@code NoUpdateDao} gate in abacus-jdbc, which uses exactly this
     * check: it permits reads and plain inserts of new rows but forbids statements that mutate
     * existing data.
     * <p>
     * A statement qualifies as "no-update" only if its leading keyword is {@code SELECT} or
     * {@code INSERT} <i>and</i> it contains none of the following (matching outside of quoted string
     * literals and SQL comments):
     * </p>
     * <ul>
     *   <li>a top-level {@code UPDATE}, {@code DELETE} or {@code MERGE} keyword (matched only at
     *       statement-start positions, so e.g. {@code SELECT ... FOR UPDATE} is still accepted); or</li>
     *   <li>an upsert clause that can modify existing rows, namely {@code INSERT OR REPLACE},
     *       {@code ON DUPLICATE KEY UPDATE} (MySQL) or {@code ON CONFLICT ... DO UPDATE}
     *       (PostgreSQL/SQLite). These clauses are recognized outside quoted literals,
     *       quoted identifiers and comments; or</li>
     *   <li>a table-creating or table-overwriting clause, namely a standalone
     *       {@code SELECT ... INTO ...} (a {@code SELECT} whose select list contains the
     *       {@code INTO} keyword) or an {@code INSERT OVERWRITE} (Hive).</li>
     * </ul>
     * <p>
     * A plain {@code INSERT}, and an {@code INSERT ... ON CONFLICT ... DO NOTHING}, are therefore
     * accepted, since they never overwrite existing rows. Clause and keyword scans use token
     * boundaries, so identifiers such as {@code into$}, qualified names such as {@code t.into},
     * {@code update_time} or bracket/quoted identifiers named like keywords are ignored. A
     * {@code null} or empty statement does not lead with {@code SELECT} or {@code INSERT}, so it
     * returns {@code false}. For multi-statement SQL, a later top-level {@code UPDATE},
     * {@code DELETE}, {@code MERGE}, {@code REPLACE}, {@code TRUNCATE}, {@code DROP} or
     * {@code ALTER} statement makes this method return {@code false}; the keyword scan matches
     * only statement-start positions, so the {@code REPLACE(...)}/{@code TRUNCATE(...)} SQL
     * <i>functions</i> do not affect the classification.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} if the SQL neither updates nor deletes existing rows, {@code false} otherwise
     *         (including for a {@code null} or empty statement)
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     */
    public static boolean isNoUpdateQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        if (!(isSelectQuery(sql) || isInsertQuery(sql))) {
            return false;
        }

        // See the note in containsMutationQueryKeyword: statement-start-only matching keeps the
        // REPLACE(...)/TRUNCATE(...) functions from false-positiving.
        return !containsQueryKeyword(sql, "UPDATE") && !containsQueryKeyword(sql, "DELETE") && !containsQueryKeyword(sql, "MERGE")
                && !containsQueryKeyword(sql, "REPLACE") && !containsQueryKeyword(sql, "TRUNCATE") && !containsQueryKeyword(sql, "DROP")
                && !containsQueryKeyword(sql, "ALTER") && !containsInsertUpdateClause(sql) && !containsSelectIntoClause(sql)
                && !containsTokenSequence(sql, "INSERT", "OVERWRITE");
    }

    private static boolean containsMutationQueryKeyword(final String sql) {
        // containsQueryKeyword matches only at statement-start positions (start of SQL, after ';', or a CTE
        // body's "AS ("), so the REPLACE(...)/TRUNCATE(...) string/numeric FUNCTIONS -- which always appear
        // mid-statement -- cannot false-positive here.
        return containsQueryKeyword(sql, "INSERT") || containsQueryKeyword(sql, "UPDATE") || containsQueryKeyword(sql, "DELETE")
                || containsQueryKeyword(sql, "MERGE") || containsQueryKeyword(sql, "REPLACE") || containsQueryKeyword(sql, "TRUNCATE")
                || containsQueryKeyword(sql, "DROP") || containsQueryKeyword(sql, "ALTER") || containsQueryKeyword(sql, "CREATE");
    }

    private static boolean containsInsertUpdateClause(final String sql) {
        return isInsertOrReplaceQuery(sql) || containsTokenSequence(sql, "ON", "DUPLICATE", "KEY", "UPDATE") || containsOnConflictDoUpdateClause(sql);
    }

    private static boolean containsOnConflictDoUpdateClause(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        int index = 0;
        int matched = 0; // 0: seek ON, 1: expect CONFLICT, 2: seek DO, 3: expect UPDATE.
        int conflictClauseDepth = 0;

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index);

            if (index >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                continue;
            } else if (ch == ';') {
                matched = 0;
                conflictClauseDepth = 0;
                index++;
                continue;
            }

            if (Character.isLetter(ch)) {
                final String token = readKeyword(sql, index);

                if (matched == 0) {
                    matched = "ON".equalsIgnoreCase(token) ? 1 : 0;
                } else if (matched == 1) {
                    if ("CONFLICT".equalsIgnoreCase(token)) {
                        matched = 2;
                    } else {
                        matched = "ON".equalsIgnoreCase(token) ? 1 : 0;
                    }

                    conflictClauseDepth = 0;
                } else if (matched == 2) {
                    if (conflictClauseDepth == 0 && "DO".equalsIgnoreCase(token)) {
                        matched = 3;
                    }
                } else if (matched == 3) {
                    if ("UPDATE".equalsIgnoreCase(token)) {
                        return true;
                    }

                    matched = "ON".equalsIgnoreCase(token) ? 1 : 0;
                    conflictClauseDepth = 0;
                }

                index += token.length();
                continue;
            }

            if (matched == 2) {
                if (ch == '(') {
                    conflictClauseDepth++;
                } else if (ch == ')' && conflictClauseDepth > 0) {
                    conflictClauseDepth--;
                }
            }

            if (matched == 1) {
                matched = 0;
                conflictClauseDepth = 0;
            } else if (matched == 3) {
                matched = 0;
                conflictClauseDepth = 0;
            }

            index++;
        }

        return false;
    }

    private static boolean containsSelectIntoClause(final String sql) {
        return isSelectQuery(sql) && containsSelectListIntoToken(sql);
    }

    private static boolean containsSelectListIntoToken(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        final List<Boolean> selectBeforeFromByDepth = new ArrayList<>(4);
        int index = 0;
        int depth = 0;

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index);

            if (index >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                continue;
            } else if (ch == '(') {
                depth++;
                index++;
                continue;
            } else if (ch == ')') {
                clearSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth);

                if (depth > 0) {
                    depth--;
                }

                index++;
                continue;
            } else if (ch == ';') {
                if (depth == 0) {
                    selectBeforeFromByDepth.clear();
                }

                index++;
                continue;
            }

            if (isIdentifierChar(ch)) {
                final String token = readIdentifierToken(sql, index);

                if ("SELECT".equalsIgnoreCase(token)) {
                    setSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth, true);
                } else if ("FROM".equalsIgnoreCase(token) && !isDotQualifiedToken(sql, index, index + token.length())) {
                    if (isSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth)) {
                        setSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth, false);
                    }
                } else if ("INTO".equalsIgnoreCase(token) && isSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth)
                        && !isDotQualifiedToken(sql, index, index + token.length())) {
                    return true;
                }

                index += token.length();
                continue;
            }

            index++;
        }

        return false;
    }

    private static boolean isSelectBeforeFromAtDepth(final List<Boolean> selectBeforeFromByDepth, final int depth) {
        return depth < selectBeforeFromByDepth.size() && Boolean.TRUE.equals(selectBeforeFromByDepth.get(depth));
    }

    private static void setSelectBeforeFromAtDepth(final List<Boolean> selectBeforeFromByDepth, final int depth, final boolean value) {
        while (selectBeforeFromByDepth.size() <= depth) {
            selectBeforeFromByDepth.add(Boolean.FALSE);
        }

        selectBeforeFromByDepth.set(depth, value);
    }

    private static void clearSelectBeforeFromAtDepth(final List<Boolean> selectBeforeFromByDepth, final int depth) {
        if (depth < selectBeforeFromByDepth.size()) {
            selectBeforeFromByDepth.set(depth, Boolean.FALSE);
        }
    }

    private static boolean isDotQualifiedToken(final String sql, final int startIndex, final int endIndex) {
        final int previousIndex = skipBackwardWhitespaceAndComments(sql, startIndex - 1);

        if (previousIndex >= 0 && sql.charAt(previousIndex) == '.') {
            return true;
        }

        final int nextIndex = skipLeadingWhitespaceAndComments(sql, endIndex);

        return nextIndex < sql.length() && sql.charAt(nextIndex) == '.';
    }

    private static boolean containsTokenSequence(final String sql, final String... tokens) {
        if (Strings.isEmpty(sql) || tokens.length == 0) {
            return false;
        }

        int index = 0;
        int matched = 0;

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index);

            if (index >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                matched = 0;
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                matched = 0;
                continue;
            }

            if (Character.isLetter(ch)) {
                final String token = readKeyword(sql, index);

                if (tokens[matched].equalsIgnoreCase(token)) {
                    matched++;

                    if (matched == tokens.length) {
                        return true;
                    }
                } else {
                    matched = tokens[0].equalsIgnoreCase(token) ? 1 : 0;
                }

                index += token.length();
                continue;
            }

            matched = 0;
            index++;
        }

        return false;
    }

    private static boolean containsQueryKeyword(final String sql, final String keywordToFind) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        int index = 0;
        boolean canStartQueryKeyword = true;
        String previousKeyword = "";

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index);

            if (index >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                canStartQueryKeyword = false;
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                canStartQueryKeyword = false;
                continue;
            }

            if (Character.isLetter(ch)) {
                final String token = readKeyword(sql, index);

                if (canStartQueryKeyword && "WITH".equalsIgnoreCase(token)) {
                    final int queryKeywordIndex = findKeywordIndexAfterWithClause(sql, index + token.length());

                    if (queryKeywordIndex >= 0 && keywordToFind.equalsIgnoreCase(readKeyword(sql, queryKeywordIndex))) {
                        return true;
                    }
                }

                if (canStartQueryKeyword && keywordToFind.equalsIgnoreCase(token)) {
                    return true;
                }

                previousKeyword = token;
                canStartQueryKeyword = false;
                index += token.length();
                continue;
            }

            if (ch == ';') {
                canStartQueryKeyword = true;
            } else if (ch == '(') {
                canStartQueryKeyword = canStartQueryKeyword || "AS".equalsIgnoreCase(previousKeyword) || "MATERIALIZED".equalsIgnoreCase(previousKeyword);
            } else if (!Character.isWhitespace(ch)) {
                canStartQueryKeyword = false;
            }

            index++;
        }

        return false;
    }

    private static String getLeadingQueryKeyword(final String sql) {
        final int index = getLeadingQueryKeywordIndex(sql);
        return index >= 0 ? readKeyword(sql, index) : "";
    }

    private static int getLeadingQueryKeywordIndex(final String sql) {
        if (Strings.isEmpty(sql)) {
            return -1;
        }

        int index = skipLeadingWhitespaceAndComments(sql, 0);

        // A query may be wrapped in one or more leading parentheses, e.g. "(SELECT 1)" or
        // "(SELECT a FROM t1) UNION ALL (SELECT a FROM t2)". Skip past those so the leading verb
        // (SELECT/INSERT/...) is still recognized instead of being classified as no leading keyword.
        while (index < sql.length() && sql.charAt(index) == '(') {
            index = skipLeadingWhitespaceAndComments(sql, index + 1);
        }

        if (index >= sql.length()) {
            return -1;
        }

        String keyword = readKeyword(sql, index);

        if (Strings.isEmpty(keyword)) {
            return -1;
        }

        if (!"WITH".equalsIgnoreCase(keyword)) {
            return index;
        }

        index += keyword.length();
        index = skipLeadingWhitespaceAndComments(sql, index);

        keyword = readKeyword(sql, index);

        if ("RECURSIVE".equalsIgnoreCase(keyword)) {
            index += keyword.length();
        }

        return findKeywordIndexAfterWithClause(sql, index);
    }

    private static int findKeywordIndexAfterWithClause(final String sql, int fromIndex) {
        int depth = 0;

        while (fromIndex < sql.length()) {
            fromIndex = skipLeadingWhitespaceAndComments(sql, fromIndex);

            if (fromIndex >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(fromIndex);

            if (ch == '\'' || ch == '"' || ch == '`') {
                fromIndex = skipQuotedLiteral(sql, fromIndex, ch);
                continue;
            } else if (ch == '[') {
                fromIndex = skipBracketQuotedIdentifier(sql, fromIndex);
                continue;
            }

            if (ch == '(') {
                depth++;
                fromIndex++;
                continue;
            }

            if (ch == ')') {
                if (depth > 0) {
                    depth--;
                }

                fromIndex++;
                continue;
            }

            if (Character.isLetter(ch)) {
                final String token = readKeyword(sql, fromIndex);

                if (depth == 0 && isQueryKeyword(token)) {
                    return fromIndex;
                }

                fromIndex += token.length();
                continue;
            }

            fromIndex++;
        }

        return -1;
    }

    private static boolean isQueryKeyword(final String token) {
        return "SELECT".equalsIgnoreCase(token) || "INSERT".equalsIgnoreCase(token) || "UPDATE".equalsIgnoreCase(token) || "DELETE".equalsIgnoreCase(token)
                || "MERGE".equalsIgnoreCase(token);
    }

    private static int skipLeadingWhitespaceAndComments(final String sql, int fromIndex) {
        while (fromIndex < sql.length()) {
            while (fromIndex < sql.length() && Character.isWhitespace(sql.charAt(fromIndex))) {
                fromIndex++;
            }

            if (fromIndex >= sql.length()) {
                break;
            }

            if ((fromIndex + 1 < sql.length()) && sql.charAt(fromIndex) == '-' && sql.charAt(fromIndex + 1) == '-') {
                fromIndex += 2;

                while (fromIndex < sql.length() && sql.charAt(fromIndex) != '\n' && sql.charAt(fromIndex) != '\r') {
                    fromIndex++;
                }

                continue;
            }

            if ((fromIndex + 1 < sql.length()) && sql.charAt(fromIndex) == '/' && sql.charAt(fromIndex + 1) == '*') {
                fromIndex += 2;

                while ((fromIndex + 1 < sql.length()) && !(sql.charAt(fromIndex) == '*' && sql.charAt(fromIndex + 1) == '/')) {
                    fromIndex++;
                }

                fromIndex = Math.min(fromIndex + 2, sql.length());
                continue;
            }

            if (sql.charAt(fromIndex) == '#' && isHashCommentStart(sql, sql.length(), fromIndex)) {
                do {
                    fromIndex++;
                } while (fromIndex < sql.length() && sql.charAt(fromIndex) != '\n' && sql.charAt(fromIndex) != '\r');

                continue;
            }

            break;
        }

        return fromIndex;
    }

    private static int skipQuotedLiteral(final String sql, int fromIndex, final char quoteChar) {
        fromIndex++;

        while (fromIndex < sql.length()) {
            final char ch = sql.charAt(fromIndex);

            if (ch == '\\') {
                // Skip backslash-escaped character (e.g., \' in MySQL)
                fromIndex += 2;
                if (fromIndex >= sql.length()) {
                    break;
                }
            } else if (ch == quoteChar) {
                if ((fromIndex + 1 < sql.length()) && sql.charAt(fromIndex + 1) == quoteChar) {
                    // Doubled quote escape (SQL standard)
                    fromIndex += 2;
                } else {
                    fromIndex++;
                    break;
                }
            } else {
                fromIndex++;
            }
        }

        return fromIndex;
    }

    private static int skipBracketQuotedIdentifier(final String sql, int fromIndex) {
        fromIndex++;

        while (fromIndex < sql.length()) {
            if (sql.charAt(fromIndex) == ']') {
                if ((fromIndex + 1 < sql.length()) && sql.charAt(fromIndex + 1) == ']') {
                    fromIndex += 2;
                } else {
                    fromIndex++;
                    break;
                }
            } else {
                fromIndex++;
            }
        }

        return fromIndex;
    }

    private static String readKeyword(final String sql, int fromIndex) {
        fromIndex = skipLeadingWhitespaceAndComments(sql, fromIndex);

        if (fromIndex >= sql.length() || !Character.isLetter(sql.charAt(fromIndex))) {
            return "";
        }

        final int startIndex = fromIndex;

        while (fromIndex < sql.length() && isIdentifierChar(sql.charAt(fromIndex))) {
            fromIndex++;
        }

        return fromIndex > startIndex ? sql.substring(startIndex, fromIndex) : "";
    }

    private static String readIdentifierToken(final String sql, int fromIndex) {
        fromIndex = skipLeadingWhitespaceAndComments(sql, fromIndex);

        final int startIndex = fromIndex;

        while (fromIndex < sql.length()) {
            final char ch = sql.charAt(fromIndex);

            if (!isIdentifierChar(ch)) {
                break;
            }

            fromIndex++;
        }

        return fromIndex > startIndex ? sql.substring(startIndex, fromIndex) : "";
    }
}
