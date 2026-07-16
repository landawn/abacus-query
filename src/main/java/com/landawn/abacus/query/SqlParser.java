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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * A utility class for parsing SQL statements into lexical SQL tokens.
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
 *   <li>With the built-in separator configuration, runs of whitespace between emitted tokens are collapsed into a single space token
 *       ({@code " "}); leading whitespace before the very first emitted token is dropped, while
 *       trailing whitespace after the last non-space token yields a final {@code " "} token.</li>
 *   <li>Multi-character operators (e.g. {@code >=}, {@code <>}, {@code ->>}, PostgreSQL
 *       {@code #-} and {@code @?}) are emitted
 *       as single tokens. Additional separators can be configured without global mutation by using
 *       {@link TokenizerConfig#builder()} and an instance-scoped {@link Tokenizer}.</li>
 *   <li>iBatis/MyBatis {@code #{...}} markers and configured PostgreSQL hash operators are
 *       not mistaken for hash comments.</li>
 *   <li>{@code #} or {@code ##} that opens a hash-prefixed identifier (e.g. a SQL Server
 *       local or global temp-table name appearing
 *       after {@code FROM}, {@code JOIN}, {@code INTO}, {@code UPDATE} or {@code TABLE}, or in the
 *       target position after {@code INSERT}, {@code DELETE} or {@code MERGE} and an optional
 *       {@code TOP} clause,
 *       allowing intervening whitespace or comments, including as a later element of a
 *       comma-separated list governed by one of those keywords, e.g. {@code "FROM #t1, ##t2"})
 *       is treated as part of the identifier, not as a hash comment or separator.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String sql = "SELECT * FROM users WHERE age > 25 ORDER BY name";
 * List<String> tokens = SqlParser.parse(sql);
 * // Result: ["SELECT", " ", "*", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">", " ", "25", " ", "ORDER", " ", "BY", " ", "name"]
 *
 * SqlParser.Tokenizer postgresTokenizer = SqlParser.tokenizer(
 *         SqlParser.TokenizerConfig.builder().withSeparator("::").build());
 * List<String> postgresTokens = postgresTokenizer.parse("payload::jsonb");
 * // Result: ["payload", "::", "jsonb"]
 * }</pre>
 *
 * <p id="query-classification"><b>Query classification:</b> the {@link #isSelectQuery(String)},
 * {@link #isInsertQuery(String)}, {@link #isUpdateQuery(String)}, {@link #isDeleteQuery(String)},
 * {@link #isInsertOrReplaceQuery(String)}, {@link #isReadOnlyQuery(String)} and
 * {@link #isReadOrInsertQuery(String)} predicates classify a statement as summarized below. Each cell shows
 * whether the predicate in that column returns {@code true} (Y) or {@code false} (N) for the statement
 * kind in that row. Every read-only statement also qualifies as read-or-insert, but not vice versa.</p>
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
 *   <th>{@code isReadOrInsertQuery}</th>
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
 * <tr><td>{@code CALL} / JDBC {@code {call ...}} / {@code EXEC} / {@code EXECUTE}</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr>
 * </table>
 *
 */
public final class SqlParser {

    private static final String KEEP_COMMENTS = "-- Keep comments";

    private static final char TAB = '\t';

    private static final char ENTER = '\n';

    private static final char ENTER_2 = '\r';

    private static final Set<String> hashIdentifierContextKeywords = N.asSet(SK.FROM, SK.JOIN, SK.INTO, SK.UPDATE, "TABLE");
    private static final Set<String> hashIdentifierDmlTargetKeywords = N.asSet("INSERT", "DELETE", "MERGE");

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static Set<String> defaultSeparators() {
        final Set<String> separators = new LinkedHashSet<>();

        separators.add(String.valueOf(TAB));
        separators.add(String.valueOf(ENTER));
        separators.add(String.valueOf(ENTER_2));
        separators.add(" ");
        separators.add("?");
        separators.add(",");
        separators.add("~");
        separators.add("!");
        separators.add("@");
        separators.add("^");
        // Registered so isSeparator('#') stays true, but a bare '#' is in practice always consumed
        // as a MySQL hash comment (or merged into a hash-prefixed identifier / #{...} marker) by the
        // scanners before the separator branch, so '#' is never emitted as its own token.
        separators.add("#");
        separators.add("!!");
        separators.add(";");
        separators.add("(");
        separators.add(")");
        separators.add("=");
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
        separators.add(">");
        separators.add(">>");
        separators.add(">=");
        separators.add("@>");
        separators.add("&>");
        separators.add(">^");
        separators.add("<");
        separators.add("<<");
        separators.add("<=");
        separators.add("<@");
        separators.add("&<");
        separators.add("<^");
        separators.add("+");
        separators.add("-");
        separators.add("%");
        separators.add("/");
        separators.add("*");
        separators.add("&");
        separators.add("&&");
        separators.add("|");
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

        return separators;
    }

    private static final Map<String, String[]> compositeTokens = new ConcurrentHashMap<>(64);

    static {
        compositeTokens.put(SK.LEFT_JOIN, new String[] { "LEFT", "JOIN" });
        compositeTokens.put(SK.RIGHT_JOIN, new String[] { "RIGHT", "JOIN" });
        compositeTokens.put(SK.FULL_JOIN, new String[] { "FULL", "JOIN" });
        compositeTokens.put(SK.CROSS_JOIN, new String[] { "CROSS", "JOIN" });
        compositeTokens.put(SK.NATURAL_JOIN, new String[] { "NATURAL", "JOIN" });
        compositeTokens.put(SK.INNER_JOIN, new String[] { "INNER", "JOIN" });
        compositeTokens.put(SK.GROUP_BY, new String[] { "GROUP", "BY" });
        compositeTokens.put(SK.ORDER_BY, new String[] { "ORDER", "BY" });
        compositeTokens.put(SK.FOR_UPDATE, new String[] { "FOR", "UPDATE" });
        compositeTokens.put(SK.FETCH_FIRST, new String[] { "FETCH", "FIRST" });
        compositeTokens.put(SK.FETCH_NEXT, new String[] { "FETCH", "NEXT" });
        compositeTokens.put(SK.ROWS_ONLY, new String[] { "ROWS", "ONLY" });
        compositeTokens.put(SK.UNION_ALL, new String[] { "UNION", "ALL" });
        compositeTokens.put(SK.IS_NOT, new String[] { "IS", "NOT" });
        compositeTokens.put(SK.IS_NULL, new String[] { "IS", "NULL" });
        compositeTokens.put(SK.IS_NOT_NULL, new String[] { "IS", "NOT", "NULL" });
        compositeTokens.put(SK.IS_EMPTY, new String[] { "IS", "EMPTY" });
        compositeTokens.put(SK.IS_NOT_EMPTY, new String[] { "IS", "NOT", "EMPTY" });
        compositeTokens.put(SK.IS_BLANK, new String[] { "IS", "BLANK" });
        compositeTokens.put(SK.IS_NOT_BLANK, new String[] { "IS", "NOT", "BLANK" });
        compositeTokens.put(SK.NOT_IN, new String[] { "NOT", "IN" });
        compositeTokens.put(SK.NOT_EXISTS, new String[] { "NOT", "EXISTS" });
        compositeTokens.put(SK.NOT_LIKE, new String[] { "NOT", "LIKE" });
        compositeTokens.put(SK.PARTITION_BY, new String[] { "PARTITION", "BY" });

        final List<String> list = new ArrayList<>(compositeTokens.keySet());

        for (String e : list) {
            e = e.toLowerCase(Locale.ROOT);

            if (!compositeTokens.containsKey(e)) {
                compositeTokens.put(e, splitTokenComponents(e));
            }

            e = e.toUpperCase(Locale.ROOT);

            if (!compositeTokens.containsKey(e)) {
                compositeTokens.put(e, splitTokenComponents(e));
            }
        }
    }

    private static final TokenizerConfig DEFAULT_TOKENIZER_CONFIG = new TokenizerConfig(defaultSeparators());

    private static final Tokenizer DEFAULT_TOKENIZER = new Tokenizer(DEFAULT_TOKENIZER_CONFIG);

    private SqlParser() {
    }

    /**
     * Splits a search token on every Java/SQL whitespace character rather than only a literal
     * space. Empty components are omitted so differently formatted composite keywords such as
     * {@code "ORDER  BY"}, {@code "ORDER\tBY"}, and {@code "ORDER\nBY"} are equivalent.
     */
    private static String[] splitTokenComponents(final String token) {
        final List<String> components = new ArrayList<>();
        final int length = token.length();
        int start = 0;

        while (start < length) {
            while (start < length && Character.isWhitespace(token.charAt(start))) {
                start++;
            }

            if (start >= length) {
                break;
            }

            int end = start + 1;

            while (end < length && !Character.isWhitespace(token.charAt(end))) {
                end++;
            }

            components.add(token.substring(start, end));
            start = end;
        }

        return components.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Starts an immutable tokenizer configuration from the built-in SQL separators.
     *
     * @return a builder initialized with the default separators
     * @deprecated use {@link TokenizerConfig#builder()}; builder creation belongs to the type being built
     */
    @Deprecated
    public static TokenizerConfig.Builder tokenizerConfigBuilder() {
        return TokenizerConfig.builder();
    }

    /**
     * Returns the immutable configuration used by the static parsing methods.
     *
     * @return the default tokenizer configuration
     */
    public static TokenizerConfig defaultTokenizerConfig() {
        return DEFAULT_TOKENIZER_CONFIG;
    }

    /**
     * Returns the default tokenizer using the built-in separator configuration.
     *
     * @return a tokenizer using {@link #defaultTokenizerConfig()}
     */
    public static Tokenizer tokenizer() {
        return DEFAULT_TOKENIZER;
    }

    /**
     * Creates an instance-scoped tokenizer. The supplied immutable configuration is retained and
     * never changes process-wide parsing behavior.
     *
     * @param tokenizerConfig the tokenizer configuration; must not be {@code null}
     * @return a tokenizer bound to the supplied configuration
     * @throws IllegalArgumentException if {@code tokenizerConfig} is {@code null}
     */
    public static Tokenizer tokenizer(final TokenizerConfig tokenizerConfig) {
        N.checkArgNotNull(tokenizerConfig, "tokenizerConfig");
        return new Tokenizer(tokenizerConfig);
    }

    /**
     * Immutable separator configuration for {@link Tokenizer}. Instances are created with
     * {@link #builder()} or derived with {@link #toBuilder()}, and can safely be shared between threads.
     *
     * <p>SQL lexical structure takes precedence over configured separators: quoted regions remain
     * whole tokens, comments are skipped, {@code #{...}} remains a MyBatis parameter marker, and a
     * contextually recognized hash-prefixed identifier remains an identifier. Separators are
     * recognized outside those constructs, with the longest configured match winning.</p>
     */
    public static final class TokenizerConfig {
        private final Set<String> separators;
        private final int maxSeparatorLength;
        private final boolean[] asciiSeparators;
        private final Set<Character> nonAsciiSingleCharSeparators;
        private final String[][] multiCharSeparatorsByLength;
        private final boolean[] multiCharSeparatorFirstChars;
        private final boolean hasNonAsciiMultiCharFirstChar;

        private TokenizerConfig(final Set<String> configuredSeparators) {
            separators = Collections.unmodifiableSet(new LinkedHashSet<>(configuredSeparators));

            int maxLength = 1;
            final boolean[] ascii = new boolean[128];
            final Set<Character> nonAscii = new LinkedHashSet<>();

            for (final String separator : separators) {
                maxLength = Math.max(maxLength, separator.length());

                if (separator.length() == 1) {
                    final char ch = separator.charAt(0);

                    if (ch < 128) {
                        ascii[ch] = true;
                    } else {
                        nonAscii.add(ch);
                    }
                }
            }

            maxSeparatorLength = maxLength;
            asciiSeparators = ascii;
            nonAsciiSingleCharSeparators = Collections.unmodifiableSet(nonAscii);

            @SuppressWarnings("unchecked")
            final List<String>[] buckets = new List[maxLength + 1];
            final boolean[] firstChars = new boolean[128];
            boolean nonAsciiFirstChar = false;

            for (final String separator : separators) {
                if (separator.length() > 1) {
                    final int length = separator.length();

                    if (buckets[length] == null) {
                        buckets[length] = new ArrayList<>();
                    }

                    buckets[length].add(separator);

                    final char first = separator.charAt(0);

                    if (first < 128) {
                        firstChars[first] = true;
                    } else {
                        nonAsciiFirstChar = true;
                    }
                }
            }

            multiCharSeparatorsByLength = new String[maxLength + 1][];

            for (int length = 0; length <= maxLength; length++) {
                multiCharSeparatorsByLength[length] = buckets[length] == null ? EMPTY_STRING_ARRAY : buckets[length].toArray(new String[0]);
            }

            multiCharSeparatorFirstChars = firstChars;
            hasNonAsciiMultiCharFirstChar = nonAsciiFirstChar;
        }

        /**
         * Starts a configuration builder initialized with the built-in SQL separators.
         * Changes made through the builder are instance-scoped and do not affect the static parser.
         *
         * @return a new builder initialized with the default separators
         */
        public static Builder builder() {
            return new Builder(DEFAULT_TOKENIZER_CONFIG);
        }

        /**
         * Creates a builder initialized with this configuration's separators.
         * The builder owns a defensive copy, so subsequent changes do not mutate this configuration.
         *
         * @return a new builder initialized from this configuration
         */
        public Builder toBuilder() {
            return new Builder(this);
        }

        /**
         * Returns the configured separators as an immutable set.
         *
         * @return the configured separators
         */
        public Set<String> separators() {
            return separators;
        }

        private boolean isSingleCharSeparator(final char ch) {
            return ch < 128 ? asciiSeparators[ch] : nonAsciiSingleCharSeparators.contains(ch);
        }

        @Override
        public int hashCode() {
            return separators.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof final TokenizerConfig other && separators.equals(other.separators);
        }

        @Override
        public String toString() {
            return "TokenizerConfig{" + "separators=" + separators + '}';
        }

        /**
         * Builder for immutable {@link TokenizerConfig} instances. Use {@link TokenizerConfig#builder()}
         * for the defaults or {@link TokenizerConfig#toBuilder()} to derive a modified configuration.
         */
        public static final class Builder {
            private final Set<String> separators;

            private Builder(final TokenizerConfig base) {
                separators = new LinkedHashSet<>(base.separators);
            }

            /**
             * Adds a single-character separator to this configuration.
             *
             * @param separator the separator to add
             * @return this builder
             */
            public Builder withSeparator(final char separator) {
                separators.add(String.valueOf(separator));
                return this;
            }

            /**
             * Adds a single- or multi-character separator to this configuration.
             *
             * @param separator the separator to add; must not be {@code null} or empty
             * @return this builder
             * @throws IllegalArgumentException if {@code separator} is {@code null} or empty
             */
            public Builder withSeparator(final String separator) {
                N.checkArgNotEmpty(separator, "separator");
                separators.add(separator);
                return this;
            }

            /**
             * Removes a single-character separator from this configuration.
             *
             * @param separator the separator to remove
             * @return this builder
             */
            public Builder withoutSeparator(final char separator) {
                separators.remove(String.valueOf(separator));
                return this;
            }

            /**
             * Removes a single- or multi-character separator from this configuration.
             *
             * @param separator the separator to remove; must not be {@code null} or empty
             * @return this builder
             * @throws IllegalArgumentException if {@code separator} is {@code null} or empty
             */
            public Builder withoutSeparator(final String separator) {
                N.checkArgNotEmpty(separator, "separator");
                separators.remove(separator);
                return this;
            }

            /**
             * Builds the immutable configuration.
             *
             * @return a new tokenizer configuration
             */
            public TokenizerConfig build() {
                return new TokenizerConfig(separators);
            }
        }
    }

    /**
     * Instance-scoped, thread-safe SQL tokenizer. Its immutable {@link TokenizerConfig} replaces the
     * former process-wide separator registry.
     */
    public static final class Tokenizer {
        private final TokenizerConfig tokenizerConfig;

        private Tokenizer(final TokenizerConfig tokenizerConfig) {
            this.tokenizerConfig = tokenizerConfig;
        }

        /**
         * Returns the immutable configuration used by this tokenizer.
         *
         * @return this tokenizer's configuration
         */
        public TokenizerConfig tokenizerConfig() {
            return tokenizerConfig;
        }

        /**
         * Parses a SQL statement with this tokenizer's separator configuration.
         *
         * @param sql the SQL statement to parse; must not be {@code null}
         * @return the lexical SQL tokens
         * @throws NullPointerException if {@code sql} is {@code null}
         * @see SqlParser#parse(String)
         */
        public List<String> parse(final String sql) {
            return SqlParser.parse(sql, tokenizerConfig);
        }

        /**
         * Finds a token from the beginning using case-insensitive matching.
         *
         * @param sql the SQL statement to search; must not be {@code null}
         * @param token the token to find; must not be {@code null}
         * @return the token's character index, or {@code -1}
         * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
         * @see SqlParser#indexOfToken(String, String)
         */
        public int indexOfToken(final String sql, final String token) {
            return SqlParser.indexOfToken(sql, token, 0, false, tokenizerConfig);
        }

        /**
         * Finds a token from the supplied character index using case-insensitive matching.
         *
         * @param sql the SQL statement to search; must not be {@code null}
         * @param token the token to find; must not be {@code null}
         * @param fromIndex the earliest character index to return
         * @return the token's character index, or {@code -1}
         * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
         * @see SqlParser#indexOfToken(String, String, int)
         */
        public int indexOfToken(final String sql, final String token, final int fromIndex) {
            return SqlParser.indexOfToken(sql, token, fromIndex, false, tokenizerConfig);
        }

        /**
         * Finds a token using this tokenizer's configured separators.
         *
         * @param sql the SQL statement to search; must not be {@code null}
         * @param token the token to find; must not be {@code null}
         * @param fromIndex the earliest character index to return
         * @param caseSensitive whether matching is case-sensitive
         * @return the token's character index, or {@code -1}
         * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
         * @see SqlParser#indexOfToken(String, String, int, boolean)
         */
        public int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive) {
            return SqlParser.indexOfToken(sql, token, fromIndex, caseSensitive, tokenizerConfig);
        }

        /**
         * Returns the next token at or after a character index.
         *
         * @param sql the SQL statement to scan; must not be {@code null}
         * @param fromIndex the starting character index
         * @return the next token, or an empty string if none remains
         * @throws NullPointerException if {@code sql} is {@code null}
         * @see SqlParser#nextToken(String, int)
         */
        public String nextToken(final String sql, final int fromIndex) {
            return SqlParser.nextToken(sql, fromIndex, tokenizerConfig);
        }

        /**
         * Returns the index immediately after the next token.
         *
         * @param sql the SQL statement to scan; must not be {@code null}
         * @param fromIndex the starting character index
         * @return the next token's exclusive end index, or {@code sql.length()} if none remains
         * @throws NullPointerException if {@code sql} is {@code null}
         * @see SqlParser#nextTokenEndIndex(String, int)
         */
        public int nextTokenEndIndex(final String sql, final int fromIndex) {
            return SqlParser.nextTokenEndIndex(sql, fromIndex, tokenizerConfig);
        }

        /**
         * Checks whether the statement is a read-only SELECT using this tokenizer's configured
         * separator rules. In particular, configured hash-prefixed operators are not mistaken for
         * MySQL hash comments while scanning later statements.
         *
         * @param sql the SQL statement to classify; may be empty or {@code null}
         * @return {@code true} if the SQL is a read-only SELECT query, {@code false} otherwise
         * @see SqlParser#isReadOnlyQuery(String)
         */
        public boolean isReadOnlyQuery(final String sql) {
            return SqlParser.isReadOnlyQuery(sql, tokenizerConfig);
        }
    }

    /**
     * Parses a SQL statement into a list of lexical SQL tokens.
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
     * List<String> tokens = SqlParser.parse("SELECT name, age FROM users WHERE age >= 18");
     * // Result: ["SELECT", " ", "name", ",", " ", "age", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">=", " ", "18"]
     * }</pre>
     *
     * @param sql the SQL statement to parse (must not be {@code null})
     * @return a list of tokens representing the parsed SQL statement
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    public static List<String> parse(final String sql) {
        return parse(sql, DEFAULT_TOKENIZER_CONFIG);
    }

    private static List<String> parse(final String sql, final TokenizerConfig tokenizerConfig) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            final List<String> tokens = new ArrayList<>(Math.max(16, sqlLength / 4));

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
                            tokens.add(sb.toString());
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
                        tokens.add(sb.toString());
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
                    } else if (isLikelyHashPrefixedIdentifier(sql, sqlLength, index, tokenizerConfig)) {
                        // Hash-prefixed identifier (e.g. a temp table after a table-context keyword or
                        // in an INSERT/DELETE/MERGE target position):
                        // '#' is part of the token.
                        sb.append(ch);
                    } else if ((temp = matchMultiCharSeparator(sql, sqlLength, index, tokenizerConfig)) != null) {
                        // '#'-leading operator such as #>, #>> or ##.
                        if (!sb.isEmpty()) {
                            tokens.add(sb.toString());
                            sb.setLength(0);
                        }

                        tokens.add(temp);
                        index += temp.length() - 1;
                    } else { // MySQL hash line comment: always discarded. Skip to the end of the line.
                        if (!sb.isEmpty()) {
                            tokens.add(sb.toString());
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
                        tokens.add(sb.toString());
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

                                tokens.add(sb.toString());
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

                        appendSpaceAfterSkippedBlockCommentIfNeeded(sql, sqlLength, index, tokens, tokenizerConfig);
                    }
                } else if ((temp = matchMultiCharSeparator(sql, sqlLength, index, tokenizerConfig)) != null) {
                    // Multi-character operator (e.g. >=, <>, ->>, :=). Matched before the single-character
                    // separator lookup (same effective precedence as before, when isSeparator matched it
                    // and this branch re-matched it) so the match is computed only once.
                    if (!sb.isEmpty()) {
                        tokens.add(sb.toString());
                        sb.setLength(0);
                    }

                    tokens.add(temp);
                    index += temp.length() - 1;
                } else if (tokenizerConfig.isSingleCharSeparator(ch)) {
                    if (!sb.isEmpty()) {
                        tokens.add(sb.toString());
                        sb.setLength(0);
                    }

                    if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                        if (!tokens.isEmpty() && !tokens.get(tokens.size() - 1).equals(SK.SPACE)) {
                            tokens.add(SK.SPACE);
                        }
                    } else {
                        tokens.add(String.valueOf(ch));
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
                tokens.add(sb.toString());
                sb.setLength(0);
            }

            return tokens;
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Finds the index of a specific token within a SQL statement, searching from the beginning
     * using case-insensitive matching. This is a convenience overload of
     * {@link #indexOfToken(String, String, int, boolean)} equivalent to
     * {@code indexOfToken(sql, token, 0, false)}.
     *
     * <p>Like the four-argument form, this method only reports positions where {@code token}
     * appears as a complete SQL token (or composite token such as {@code "LEFT JOIN"}), not where
     * it occurs as a substring of another identifier, and it skips matches inside line, hash and
     * block comments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int index = SqlParser.indexOfToken(sql, "ORDER BY");   // 40
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param token the token or composite keyword to find (must not be {@code null})
     * @return the index of the token if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
     * @see #indexOfToken(String, String, int, boolean)
     */
    public static int indexOfToken(final String sql, final String token) {
        return indexOfToken(sql, token, 0, false);
    }

    /**
     * Finds the index of a specific token within a SQL statement, searching from the given position
     * using case-insensitive matching. This is a convenience overload of
     * {@link #indexOfToken(String, String, int, boolean)} equivalent to
     * {@code indexOfToken(sql, token, fromIndex, false)}.
     *
     * <p>Like the four-argument form, this method only reports positions where {@code token}
     * appears as a complete SQL token (or composite token such as {@code "LEFT JOIN"}), not where
     * it occurs as a substring of another identifier, and it skips matches inside line, hash and
     * block comments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int whereIndex = SqlParser.indexOfToken(sql, "WHERE", 0);   // 20
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param token the token or composite keyword to find (must not be {@code null})
     * @param fromIndex the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
     * @return the index of the token if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
     * @see #indexOfToken(String, String, int, boolean)
     */
    public static int indexOfToken(final String sql, final String token, final int fromIndex) {
        return indexOfToken(sql, token, fromIndex, false);
    }

    /**
     * Finds the index of a specific token within a SQL statement starting from a given position.
     * This method is capable of finding both simple tokens and composite keywords (like "LEFT JOIN").
     * It respects SQL syntax rules, including quoted identifiers and case sensitivity options.
     *
     * <p>The method handles:</p>
     * <ul>
     *   <li>Simple tokens and operators</li>
     *   <li>Composite keywords (e.g., "GROUP BY", "LEFT JOIN"). Sub-tokens may be separated by
     *       any amount of whitespace and/or comments in the source SQL.</li>
     *   <li>Case-sensitive and case-insensitive matching</li>
     *   <li>Quoted identifiers (the entire quoted region is matched as a single token)</li>
     *   <li>Multi-character operators</li>
     * </ul>
     *
     * <p>Unlike {@link String#indexOf(String, int)}, this method only returns a position where
     * {@code token} appears as a <em>complete</em> SQL token (or composite token), not where it
     * occurs as a substring of another identifier; matches that fall inside line/hash/block
     * comments are skipped. Leading and trailing whitespace in {@code token} is ignored, and runs
     * of whitespace inside a composite token are treated as a separator between its components.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int index = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);
     * // Returns: 40 (the position where "ORDER BY" starts)
     *
     * int whereIndex = SqlParser.indexOfToken(sql, "WHERE", 0, false);
     * // Returns: 20 (the position where "WHERE" starts)
     * }</pre>
     *
     * @param sql the SQL statement to search within (must not be {@code null})
     * @param token the token or composite keyword to find (must not be {@code null})
     * @param fromIndex the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
     * @param caseSensitive whether the search should be case-sensitive
     * @return the index of the token if found, or {@code -1} if not found
     * @throws NullPointerException if {@code sql} or {@code token} is {@code null}
     */
    public static int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive) {
        return indexOfToken(sql, token, fromIndex, caseSensitive, DEFAULT_TOKENIZER_CONFIG);
    }

    private static int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive,
            final TokenizerConfig tokenizerConfig) {
        final String trimmedToken = token.trim();
        String[] componentTokens = null;

        // A search argument that is one token under the active configuration must be matched as a
        // unit. This covers quoted tokens containing spaces and configurations that deliberately
        // remove space from the separator set. Only composite targets are space-split below.
        if (!trimmedToken.isEmpty() && trimmedToken.equals(nextToken(trimmedToken, 0, tokenizerConfig))
                && nextTokenEndIndex(trimmedToken, 0, tokenizerConfig) == trimmedToken.length()) {
            componentTokens = new String[] { trimmedToken };
        } else {
            componentTokens = compositeTokens.get(token);

            if (componentTokens == null) {
                componentTokens = splitTokenComponents(token);
            }
        }

        // Component splitting trims surrounding whitespace. Use the normalized single component for comparisons
        // as well; otherwise a padded target such as "  WHERE  " is classified as one token above
        // but can never match because the scanner produces "WHERE" without the padding.
        final String searchToken = N.len(componentTokens) == 1 ? componentTokens[0] : token;

        //noinspection IfStatementWithIdenticalBranches
        if (N.len(componentTokens) <= 1) {
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

                                final int matchStart = index - searchToken.length() + 1;

                                if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
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
                            final int matchStart = index - searchToken.length();

                            if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
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
                    } else if (isHashCommentStart(sql, sqlLength, index, tokenizerConfig)) {
                        // Skip MySQL single-line comment (# ...)
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - searchToken.length();

                            if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
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
                            final int matchStart = index - searchToken.length();

                            if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
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
                    } else if (isSeparator(sql, sqlLength, index, ch, tokenizerConfig)) {
                        if (!sb.isEmpty()) {
                            temp = sb.toString();
                            final int matchStart = index - searchToken.length();

                            if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
                                result = matchStart;

                                break;
                            }

                            sb.setLength(0);
                        } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                            // skip white char
                            continue;
                        }

                        temp = matchMultiCharSeparator(sql, sqlLength, index, tokenizerConfig);

                        if (temp != null) {
                            if (index >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
                                result = index;

                                break;
                            }

                            index += temp.length() - 1;
                        } else if (index >= startIndex
                                && (searchToken.equals(String.valueOf(ch)) || (!caseSensitive && searchToken.equalsIgnoreCase(String.valueOf(ch))))) {
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
                    final int matchStart = sqlLength - searchToken.length();

                    if (matchStart >= startIndex && (searchToken.equals(temp) || (!caseSensitive && searchToken.equalsIgnoreCase(temp)))) {
                        result = matchStart;
                    }
                }

                return result;
            } finally {
                Objectory.recycle(sb);
            }
        } else {
            int result = indexOfToken(sql, componentTokens[0], fromIndex, caseSensitive, tokenizerConfig);

            while (result >= 0) {
                int tmpIndex = result + componentTokens[0].length();
                boolean matched = true;

                for (int i = 1; i < componentTokens.length; i++) {
                    final String nextToken = nextToken(sql, tmpIndex, tokenizerConfig);

                    if (Strings.isNotEmpty(nextToken)
                            && (nextToken.equals(componentTokens[i]) || (!caseSensitive && nextToken.equalsIgnoreCase(componentTokens[i])))) {
                        // Use indexOfToken to skip whitespace and block/line comments between component tokens.
                        final int componentTokenPos = indexOfToken(sql, componentTokens[i], tmpIndex, caseSensitive, tokenizerConfig);

                        if (componentTokenPos < 0) {
                            matched = false;
                            break;
                        }

                        tmpIndex = componentTokenPos + componentTokens[i].length();
                    } else {
                        matched = false;

                        break;
                    }
                }

                if (matched) {
                    return result;
                }

                // The first component matched but a later one did not; continue after the current match.
                result = indexOfToken(sql, componentTokens[0], result + componentTokens[0].length(), caseSensitive, tokenizerConfig);
            }

            return result;
        }
    }

    /**
     * Extracts the next token from a SQL statement starting at the specified index.
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
     * String token1 = SqlParser.nextToken(sql, 6);    // Returns: "name" (skips spaces after SELECT)
     * String token2 = SqlParser.nextToken(sql, 13);   // Returns: ","
     * String token3 = SqlParser.nextToken(sql, 14);   // Returns: "age" (skips spaces after comma)
     * }</pre>
     *
     * @param sql the SQL statement to extract the token from (must not be {@code null})
     * @param fromIndex the starting position for extraction (0-based); negative values are treated as {@code 0}
     * @return the next token found, or an empty string if no more tokens exist
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    public static String nextToken(final String sql, final int fromIndex) {
        return nextToken(sql, fromIndex, DEFAULT_TOKENIZER_CONFIG);
    }

    private static String nextToken(final String sql, final int fromIndex, final TokenizerConfig tokenizerConfig) {
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
                } else if (isHashCommentStart(sql, sqlLength, index, tokenizerConfig)) {
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
                } else if (isSeparator(sql, sqlLength, index, ch, tokenizerConfig)) {
                    if (!sb.isEmpty()) {
                        break;
                    } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                        // skip white char
                        continue;
                    }

                    temp = matchMultiCharSeparator(sql, sqlLength, index, tokenizerConfig);

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
     * Returns the position just past the next token in a SQL statement, scanning from
     * the specified index. This is the position-returning companion to
     * {@link #nextToken(String, int)}: it applies the identical scanning rules (skipping leading
     * whitespace and comments, treating a quoted region or multi-character operator as one token)
     * but returns the end index of that token instead of its text. A caller can therefore obtain
     * the next token together with its bounds without re-scanning the string with
     * {@link String#indexOf(int)}.
     *
     * <p>The returned index is the offset of the first character <em>after</em> the next token,
     * suitable for passing back as the {@code fromIndex} of a subsequent call to advance through
     * the SQL. Equivalently, {@code sql.substring(returnedStart, nextTokenEndIndex(sql, fromIndex))}
     * spans the same token returned by {@link #nextToken(String, int)} (where {@code returnedStart}
     * is the index of that token's first character).</p>
     *
     * <p>If no further token exists (only trailing whitespace and/or comments remain), the length
     * of {@code sql} is returned, consistent with {@link #nextToken(String, int)} returning an empty
     * string in that case.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT   name,   age FROM users";
     * int end1 = SqlParser.nextTokenEndIndex(sql, 6);    // 13 (just past "name")
     * int end2 = SqlParser.nextTokenEndIndex(sql, 13);   // 14 (just past ",")
     * int end3 = SqlParser.nextTokenEndIndex(sql, 14);   // 20 (just past "age")
     * }</pre>
     *
     * @param sql the SQL statement to scan (must not be {@code null})
     * @param fromIndex the starting position for scanning (0-based); negative values are treated as {@code 0}
     * @return the index immediately after the next token, or the length of {@code sql} if no further token exists
     * @throws NullPointerException if {@code sql} is {@code null}
     * @see #nextToken(String, int)
     */
    public static int nextTokenEndIndex(final String sql, final int fromIndex) {
        return nextTokenEndIndex(sql, fromIndex, DEFAULT_TOKENIZER_CONFIG);
    }

    private static int nextTokenEndIndex(final String sql, final int fromIndex, final TokenizerConfig tokenizerConfig) {
        final int sqlLength = sql.length();

        // Mirrors nextToken's scan. `started` tracks whether any token character has been
        // accumulated yet (the analogue of nextToken's `!sb.isEmpty()` guard).
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
            } else if (isHashCommentStart(sql, sqlLength, index, tokenizerConfig)) {
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
            } else if (isSeparator(sql, sqlLength, index, ch, tokenizerConfig)) {
                if (started) {
                    return index;
                } else if (ch == SK._SPACE || ch == TAB || ch == ENTER || ch == ENTER_2) {
                    // skip white char
                    continue;
                }

                final String temp = matchMultiCharSeparator(sql, sqlLength, index, tokenizerConfig);

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
     * Checks if a character at a specific position in a SQL string is a separator.
     * This method performs context-aware checking, handling special cases like
     * MyBatis/iBatis parameter markers (#{...}).
     *
     * <p>Special handling:</p>
     * <ul>
     *   <li>{@code #} followed by <code>{</code> is not considered a separator (MyBatis/iBatis {@code #{...}} syntax)</li>
     *   <li>{@code #} or {@code ##} that starts a hash-prefixed identifier (e.g. a SQL Server
     *       local or global temp-table name appearing
     *       after {@code FROM}, {@code JOIN}, {@code INTO}, {@code UPDATE} or {@code TABLE}, or as
     *       an {@code INSERT}, {@code DELETE} or {@code MERGE} target after an optional {@code TOP} clause,
     *       with optional whitespace/comments in between, including as a later element of a
     *       comma-separated list such as {@code "FROM #t1, #t2"}) is not considered a separator</li>
     *   <li>All configured single-character and multi-character separators are checked</li>
     * </ul>
     *
     * <p>Behavior (internal helper): for the SQL {@code "SELECT * FROM users"}, the {@code '*'} at index 7
     * and the space at index 6 are both separators, while the {@code '#'} in a {@code #{...}} marker is not.</p>
     *
     * <p>The method may inspect characters surrounding {@code index} (notably the next char to
     * disambiguate {@code #{...}}, and previous characters to detect a hash-prefixed identifier
     * context), in addition to checking {@code ch} against the configured separator set.</p>
     *
     * @param str the SQL string being parsed
     * @param len the exclusive upper bound for scanning (normally the length of {@code str})
     * @param index the current position in the string (0-based)
     * @param ch the character to check; expected to equal {@code str.charAt(index)}
     * @return {@code true} if the character is a separator in this context, {@code false} otherwise
     */
    static boolean isSeparator(final String str, final int len, final int index, final char ch) {
        return isSeparator(str, len, index, ch, DEFAULT_TOKENIZER_CONFIG);
    }

    private static boolean isSeparator(final String str, final int len, final int index, final char ch, final TokenizerConfig tokenizerConfig) {
        // for Ibatis/Mybatis
        if (ch == '#' && index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        if (ch == '#' && isLikelyHashPrefixedIdentifier(str, len, index, tokenizerConfig)) {
            return false;
        }

        if (tokenizerConfig.isSingleCharSeparator(ch)) {
            return true;
        }

        return matchMultiCharSeparator(str, len, index, tokenizerConfig) != null;
    }

    private static void appendSpaceAfterSkippedBlockCommentIfNeeded(final String sql, final int sqlLength, final int commentEndIndex, final List<String> tokens,
            final TokenizerConfig tokenizerConfig) {
        final int nextIndex = commentEndIndex + 1;

        if (nextIndex >= sqlLength || tokens.isEmpty() || SK.SPACE.equals(tokens.get(tokens.size() - 1))) {
            return;
        }

        final char nextChar = sql.charAt(nextIndex);

        if (!Character.isWhitespace(nextChar) && !isSeparator(sql, sqlLength, nextIndex, nextChar, tokenizerConfig)) {
            tokens.add(SK.SPACE);
        }
    }

    private static boolean isHashCommentStart(final String str, final int len, final int index, final TokenizerConfig tokenizerConfig) {
        if (str.charAt(index) != '#') {
            return false;
        }

        // for Ibatis/Mybatis
        if (index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        if (isInsideMultiCharSeparator(str, len, index, tokenizerConfig)) {
            return false;
        }

        final String multiCharSeparator = matchMultiCharSeparator(str, len, index, tokenizerConfig);
        if (multiCharSeparator != null && multiCharSeparator.length() > 1) {
            return false;
        }

        return !isLikelyHashPrefixedIdentifier(str, len, index, tokenizerConfig);
    }

    /**
     * Decides whether the {@code '#'} at {@code index} starts, or is the second character of, a
     * hash-prefixed identifier (e.g. a SQL Server local {@code #temp} or global {@code ##temp}
     * table) rather than a MySQL hash comment or a configured hash operator. The character following
     * the one- or two-character hash prefix must be an identifier character, and scanning backward
     * (skipping whitespace and comments) must reach one of the
     * {@link #hashIdentifierContextKeywords} ({@code FROM},
     * {@code JOIN}, {@code INTO}, {@code UPDATE}, {@code TABLE}), or the hash must be exactly where
     * an {@code INSERT}, {@code DELETE} or {@code MERGE} target is expected, including after an
     * optional {@code TOP} clause.
     *
     * <p>A comma is treated as a list continuation: for {@code "FROM #t1, #t2"} the backward walk
     * skips the comma plus the single list element before it (a possibly qualified, quoted,
     * '#'-prefixed, or parenthesized derived table with an optional alias token) and re-checks from
     * there, so every element of a comma-separated list governed by a context keyword is recognized. Anything else anchors
     * the decision: a context keyword means identifier, any other token or character (e.g.
     * {@code SELECT}, {@code '('}, {@code '='}) means comment, so {@code "SELECT a, #comment"}
     * remains a MySQL comment. Ambiguous shapes deliberately fall back to the comment
     * classification.</p>
     */
    private static boolean isLikelyHashPrefixedIdentifier(final String str, final int len, final int index, final TokenizerConfig tokenizerConfig) {
        if (index >= len - 1) {
            return false;
        }

        int prefixStart = index;
        int identifierStart = index + 1;

        if (str.charAt(identifierStart) == '#') {
            // SQL Server global temp tables use a two-character prefix (##name). The prefix is an
            // identifier only in the same table/target contexts as #name; elsewhere the configured
            // ## operator must retain precedence.
            identifierStart++;
        } else if (index > 0 && str.charAt(index - 1) == '#') {
            // The forward scanners visit both '#' characters separately after the first one has
            // been classified as identifier text. Re-evaluate the pair from its real start so the
            // second '#' is appended instead of being mistaken for a new hash comment.
            if (index > 1 && str.charAt(index - 2) == '#') {
                return false;
            }

            prefixStart--;
        }

        if (identifierStart >= len || !isIdentifierChar(str.charAt(identifierStart))) {
            return false;
        }

        int left = skipBackwardWhitespaceAndComments(str, prefixStart - 1, tokenizerConfig);

        if (isHashIdentifierDmlTargetContext(str, left, true, tokenizerConfig)) {
            return true;
        }

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
                final int beforeElement = skipBackwardListElement(str, beforeComma, tokenizerConfig);

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

            int tokenStart = left;

            while (tokenStart >= 0 && isIdentifierChar(str.charAt(tokenStart))) {
                tokenStart--;
            }

            final String prevWord = str.substring(tokenStart + 1, left + 1).toUpperCase(Locale.ROOT);
            return hashIdentifierContextKeywords.contains(prevWord);
        }

        return false;
    }

    /**
     * Returns whether {@code left} is immediately after an INSERT/DELETE/MERGE target introducer.
     * Unlike {@link #hashIdentifierContextKeywords}, these operation tokens are deliberately not
     * general identifier anchors: they are accepted only directly before the target, optionally
     * with a SQL Server {@code TOP [ ( expression ) ] [ PERCENT ]} clause between them.
     */
    private static boolean isHashIdentifierDmlTargetContext(final String str, int left, final boolean skipLineComments, final TokenizerConfig tokenizerConfig) {
        left = skipBackwardHashContextTrivia(str, left, skipLineComments, tokenizerConfig);

        if (left < 0) {
            return false;
        }

        int tokenStart = identifierWordStart(str, left);

        if (tokenStart >= 0 && hashIdentifierDmlTargetKeywords.contains(str.substring(tokenStart, left + 1).toUpperCase(Locale.ROOT))) {
            return true;
        }

        // TOP may end with PERCENT. Strip it before locating the parenthesized (or legacy bare)
        // expression that follows TOP.
        if (tokenStart >= 0 && "PERCENT".equalsIgnoreCase(str.substring(tokenStart, left + 1))) {
            left = skipBackwardHashContextTrivia(str, tokenStart - 1, skipLineComments, tokenizerConfig);
        }

        if (left < 0) {
            return false;
        }

        if (str.charAt(left) == ')') {
            final int openingParenthesis = findMatchingOpeningParenthesis(str, left, tokenizerConfig);

            if (openingParenthesis < 0) {
                return false;
            }

            left = skipBackwardHashContextTrivia(str, openingParenthesis - 1, skipLineComments, tokenizerConfig);
        } else {
            // SQL Server documents parentheses for data-modification TOP expressions, but accepts
            // the long-standing bare numeric form too (for example, "MERGE TOP 5 #stage").
            tokenStart = identifierWordStart(str, left);

            if (tokenStart < 0) {
                return false;
            }

            left = skipBackwardHashContextTrivia(str, tokenStart - 1, skipLineComments, tokenizerConfig);
        }

        tokenStart = identifierWordStart(str, left);

        if (tokenStart < 0 || !"TOP".equalsIgnoreCase(str.substring(tokenStart, left + 1))) {
            return false;
        }

        left = skipBackwardHashContextTrivia(str, tokenStart - 1, skipLineComments, tokenizerConfig);
        tokenStart = identifierWordStart(str, left);

        return tokenStart >= 0 && hashIdentifierDmlTargetKeywords.contains(str.substring(tokenStart, left + 1).toUpperCase(Locale.ROOT));
    }

    private static int skipBackwardHashContextTrivia(final String str, final int left, final boolean skipLineComments, final TokenizerConfig tokenizerConfig) {
        return skipLineComments ? skipBackwardWhitespaceAndComments(str, left, tokenizerConfig) : skipBackwardWhitespaceAndBlockComments(str, left);
    }

    private static int identifierWordStart(final String str, final int end) {
        if (end < 0 || !isIdentifierChar(str.charAt(end))) {
            return -1;
        }

        int start = end;

        while (start > 0 && isIdentifierChar(str.charAt(start - 1))) {
            start--;
        }

        return start;
    }

    /**
     * Consumes one comma-separated list element backward, starting at {@code start} (which must
     * already be positioned on a non-whitespace character), for the list walk in
     * {@link #isLikelyHashPrefixedIdentifier}. An element is at most two whitespace-separated
     * units (a name plus an optional trailing alias, with a free {@code AS} between them) where
     * each unit is a dot-qualified chain of segments and each segment is an identifier token
     * (optionally prefixed with {@code #} or {@code ##}), a quoted/bracket-quoted identifier, or a
     * balanced parenthesized derived-table expression.
     *
     * <p>Returns the index of the first character before the consumed element (possibly
     * {@code -1}), or {@code start} unchanged if no element was recognized there. A hash-identifier
     * context keyword is never consumed: it is left in place for the caller to classify, so
     * {@code "FROM t1, #t2"} stops in front of {@code FROM} after consuming {@code t1}.</p>
     */
    private static int skipBackwardListElement(final String str, final int start, final TokenizerConfig tokenizerConfig) {
        int left = start;
        int units = 0;

        outer: while (left >= 0 && units < 2) {
            final int unitStart = left;

            // Consume one unit: dot-joined segments, scanned backward.
            while (true) {
                final char ch = str.charAt(left);

                if (ch == ')') {
                    // A derived table may be the element before the comma: "FROM (SELECT ...) d,
                    // #tmp". Consume its balanced parenthesized body as the name unit after the
                    // alias; otherwise the walk stops at ')' and misclassifies #tmp as a comment,
                    // potentially hiding a later statement from the read-only safety checks.
                    final int openingParenthesis = findMatchingOpeningParenthesis(str, left, tokenizerConfig);

                    if (openingParenthesis < 0) {
                        break outer;
                    }

                    left = openingParenthesis - 1;
                } else if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK || ch == ']') {
                    // Quoted / bracket-quoted identifier segment: skip back to the opening quote.
                    final char openChar = ch == ']' ? '[' : ch;
                    final int quoteStart = ch == ']' ? findOpeningBracketQuotedIdentifier(str, left, tokenizerConfig)
                            : findOpeningQuoteBackward(str, left, openChar);

                    if (quoteStart < 0) {
                        break outer; // unbalanced quote -> not a recognizable element
                    }

                    left = quoteStart - 1;
                } else if (isIdentifierChar(ch)) {
                    int tokenStart = left;

                    while (tokenStart >= 0 && isIdentifierChar(str.charAt(tokenStart))) {
                        tokenStart--;
                    }

                    final String token = str.substring(tokenStart + 1, left + 1).toUpperCase(Locale.ROOT);

                    if (hashIdentifierContextKeywords.contains(token)) {
                        break outer; // the anchor keyword; leave it for the caller to classify
                    }

                    if ("AS".equals(token) && left == unitStart) {
                        // "name AS alias": the AS keyword does not count against the unit budget.
                        left = skipBackwardWhitespaceAndBlockComments(str, tokenStart);
                        continue outer;
                    }

                    left = tokenStart;

                    if (left >= 0 && str.charAt(left) == '#') {
                        left--; // '#'-prefixed segment: the '#' belongs to the identifier

                        if (left >= 0 && str.charAt(left) == '#') {
                            left--; // SQL Server global-temp prefix (##)
                        }
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

            left = beforeGap; // a second unit (the name before an alias token) may follow
        }

        return left;
    }

    /**
     * Finds the opening quote paired with {@code closingQuote} while scanning a quoted identifier
     * backward. Doubled SQL quotes and backslash-escaped quotes are content, not opening delimiters.
     */
    private static int findOpeningQuoteBackward(final String str, final int closingQuote, final char quoteChar) {
        int index = closingQuote - 1;

        while (index >= 0) {
            if (str.charAt(index) != quoteChar) {
                index--;
                continue;
            }

            int backslashCount = 0;
            int beforeQuote = index - 1;

            while (beforeQuote >= 0 && str.charAt(beforeQuote) == '\\') {
                backslashCount++;
                beforeQuote--;
            }

            if ((backslashCount & 1) != 0) {
                index = beforeQuote;
            } else if (index > 0 && str.charAt(index - 1) == quoteChar) {
                index -= 2;
            } else {
                return index;
            }
        }

        return -1;
    }

    /**
     * Finds the opening bracket paired with {@code closingBracket}. A forward lexical scan is
     * necessary because an unescaped {@code '['} is legal content inside a bracket-quoted SQL
     * Server identifier, while doubled {@code "]]"} represents a literal closing bracket.
     */
    private static int findOpeningBracketQuotedIdentifier(final String str, final int closingBracket, final TokenizerConfig tokenizerConfig) {
        int openingBracket = -1;
        char quoteChar = 0;
        boolean backslashEscaped = false;

        for (int index = 0; index <= closingBracket; index++) {
            final char ch = str.charAt(index);

            if (quoteChar != 0) {
                if (ch == quoteChar) {
                    if (backslashEscaped) {
                        backslashEscaped = false;
                    } else if (index < closingBracket && str.charAt(index + 1) == quoteChar) {
                        index++;
                    } else {
                        quoteChar = 0;
                    }
                } else if (ch == '\\') {
                    backslashEscaped = !backslashEscaped;
                } else {
                    backslashEscaped = false;
                }

                continue;
            }

            if (openingBracket >= 0) {
                if (ch == ']') {
                    if (index < closingBracket && str.charAt(index + 1) == ']') {
                        index++;
                    } else if (index == closingBracket) {
                        return openingBracket;
                    } else {
                        openingBracket = -1;
                    }
                }

                continue;
            }

            if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK) {
                quoteChar = ch;
                backslashEscaped = false;
            } else if (ch == '[') {
                openingBracket = index;
            } else if (ch == '-' && index < closingBracket && str.charAt(index + 1) == '-') {
                index += 2;

                while (index <= closingBracket && str.charAt(index) != ENTER && str.charAt(index) != ENTER_2) {
                    index++;
                }
            } else if (ch == '#' && isHashCommentStart(str, str.length(), index, tokenizerConfig)) {
                index++;

                while (index <= closingBracket && str.charAt(index) != ENTER && str.charAt(index) != ENTER_2) {
                    index++;
                }
            } else if (ch == '/' && index < closingBracket && str.charAt(index + 1) == '*') {
                index += 2;

                while (index < closingBracket && !(str.charAt(index) == '*' && str.charAt(index + 1) == '/')) {
                    index++;
                }

                if (index < closingBracket) {
                    index++;
                }
            }
        }

        return -1;
    }

    /**
     * Finds the opening parenthesis paired with {@code closingParenthesis}, ignoring parentheses
     * inside quoted text, bracket-quoted identifiers, and SQL line/block/hash comments.
     */
    private static int findMatchingOpeningParenthesis(final String str, final int closingParenthesis, final TokenizerConfig tokenizerConfig) {
        final List<Integer> openings = new ArrayList<>(4);
        char quoteChar = 0;
        boolean backslashEscaped = false;
        boolean inBracketQuotedIdentifier = false;

        for (int i = 0; i <= closingParenthesis; i++) {
            final char ch = str.charAt(i);

            if (quoteChar != 0) {
                if (ch == quoteChar) {
                    if (backslashEscaped) {
                        backslashEscaped = false;
                    } else if (i < closingParenthesis && str.charAt(i + 1) == quoteChar) {
                        i++;
                    } else {
                        quoteChar = 0;
                    }
                } else if (ch == '\\') {
                    backslashEscaped = !backslashEscaped;
                } else {
                    backslashEscaped = false;
                }

                continue;
            }

            if (inBracketQuotedIdentifier) {
                if (ch == ']') {
                    if (i < closingParenthesis && str.charAt(i + 1) == ']') {
                        i++;
                    } else {
                        inBracketQuotedIdentifier = false;
                    }
                }

                continue;
            }

            if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK) {
                quoteChar = ch;
                backslashEscaped = false;
            } else if (ch == '[') {
                inBracketQuotedIdentifier = true;
            } else if (ch == '-' && i < closingParenthesis && str.charAt(i + 1) == '-') {
                i += 2;

                while (i <= closingParenthesis && str.charAt(i) != ENTER && str.charAt(i) != ENTER_2) {
                    i++;
                }
            } else if (ch == '#' && isHashCommentStart(str, str.length(), i, tokenizerConfig)) {
                i++;

                while (i <= closingParenthesis && str.charAt(i) != ENTER && str.charAt(i) != ENTER_2) {
                    i++;
                }
            } else if (ch == '/' && i < closingParenthesis && str.charAt(i + 1) == '*') {
                i += 2;

                while (i < closingParenthesis && !(str.charAt(i) == '*' && str.charAt(i + 1) == '/')) {
                    i++;
                }

                if (i < closingParenthesis) {
                    i++;
                }
            } else if (ch == '(') {
                openings.add(i);
            } else if (ch == ')') {
                if (openings.isEmpty()) {
                    return -1;
                }

                final int opening = openings.remove(openings.size() - 1);

                if (i == closingParenthesis) {
                    return opening;
                }
            }
        }

        return -1;
    }

    private static int skipBackwardWhitespaceAndComments(final String str, int left, final TokenizerConfig tokenizerConfig) {
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
                final int commentIndex = lastLineCommentStart(str, lineStart, left, tokenizerConfig);

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

    private static int lastLineCommentStart(final String str, final int fromIndex, final int toIndex, final TokenizerConfig tokenizerConfig) {
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
            } else if (ch == '#' && isHashLineCommentStartForBackwardScan(str, i, tokenizerConfig)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isHashLineCommentStartForBackwardScan(final String str, final int index, final TokenizerConfig tokenizerConfig) {
        if (index < str.length() - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        // The forward scanner consumes a configured multi-character separator as a unit, but this
        // backward-context scan visits every character. Do not reinterpret a later '#' within an
        // operator such as ## or ?# as the start of a line comment; doing so can hide the FROM/TABLE
        // context of a following #temp identifier (and everything later on the same line).
        if (isInsideMultiCharSeparator(str, str.length(), index, tokenizerConfig)) {
            return false;
        }

        final String multiCharSeparator = matchMultiCharSeparator(str, str.length(), index, tokenizerConfig);
        if (multiCharSeparator != null && multiCharSeparator.length() > 1) {
            return false;
        }

        return !isLikelyHashPrefixedIdentifierAfterWhitespaceAndBlockComments(str, str.length(), index, tokenizerConfig);
    }

    private static boolean isInsideMultiCharSeparator(final String str, final int len, final int index, final TokenizerConfig tokenizerConfig) {
        final int earliestSeparatorStart = Math.max(0, index - tokenizerConfig.maxSeparatorLength + 1);

        for (int separatorStart = earliestSeparatorStart; separatorStart < index; separatorStart++) {
            final String separator = matchMultiCharSeparator(str, len, separatorStart, tokenizerConfig);

            if (separator != null && separatorStart + separator.length() > index) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLikelyHashPrefixedIdentifierAfterWhitespaceAndBlockComments(final String str, final int len, final int index,
            final TokenizerConfig tokenizerConfig) {
        if (index >= len - 1) {
            return false;
        }

        int prefixStart = index;
        int identifierStart = index + 1;

        if (str.charAt(identifierStart) == '#') {
            identifierStart++;
        } else if (index > 0 && str.charAt(index - 1) == '#') {
            if (index > 1 && str.charAt(index - 2) == '#') {
                return false;
            }

            prefixStart--;
        }

        if (identifierStart >= len || !isIdentifierChar(str.charAt(identifierStart))) {
            return false;
        }

        int left = skipBackwardWhitespaceAndBlockComments(str, prefixStart - 1);

        if (left < 0) {
            return false;
        }

        if (isHashIdentifierDmlTargetContext(str, left, false, tokenizerConfig)) {
            return true;
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

    private static String matchMultiCharSeparator(final String str, final int len, final int index, final TokenizerConfig tokenizerConfig) {
        if (index < len) {
            final char first = str.charAt(index);

            // Fast reject: no configured multi-character separator starts with this character.
            if (first < 128 ? !tokenizerConfig.multiCharSeparatorFirstChars[first] : !tokenizerConfig.hasNonAsciiMultiCharFirstChar) {
                return null;
            }
        }

        final String[][] byLen = tokenizerConfig.multiCharSeparatorsByLength;
        int maxLen = Math.min(tokenizerConfig.maxSeparatorLength, len - index);

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
     * Determines if a token at a specific position in a parsed token list represents a function name.
     * A token is considered a function name if it is followed by the opening parenthesis token,
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
     * @param index the index of the token to check; invalid indices return {@code false}
     * @return {@code true} if the token at {@code index} is followed (after zero or more space tokens)
     *         by the {@code "("} token; {@code false} otherwise
     * @throws NullPointerException if {@code tokens} is {@code null}
     */
    public static boolean isFunctionName(final List<String> tokens, final int index) {
        final int size = tokens.size();

        if (index < 0 || index >= size) {
            return false;
        }

        if (SK.SPACE.equals(tokens.get(index))) {
            return false;
        }

        for (int i = index + 1; i < size; i++) {
            String token = tokens.get(i);
            if (SK.PARENTHESIS_L.equals(token)) {
                return true;
            } else if (!SK.SPACE.equals(token)) {
                return false;
            }
        }

        return false;
    }

    /**
     * Determines whether a token is a function name while examining only tokens below the supplied
     * exclusive upper bound. This compatibility overload preserves the historical bounded-scan
     * behavior; values above {@code tokens.size()} are capped at the list size.
     *
     * @param tokens the parsed SQL tokens (typically the result of {@link #parse(String)})
     * @param len the exclusive upper bound for the scan; values above {@code tokens.size()} are capped
     * @param index the index of the candidate function-name token
     * @return {@code true} if an opening-parenthesis token occurs after {@code index}, with only space
     *         tokens between them, and before the effective upper bound; {@code false} otherwise
     * @throws NullPointerException if {@code tokens} is {@code null}
     * @deprecated use {@link #isFunctionName(List, int)} when the complete token list should be examined
     */
    @Deprecated
    public static boolean isFunctionName(final List<String> tokens, final int len, final int index) {
        final int upperBound = Math.min(len, tokens.size());

        if (index < 0 || index >= upperBound || SK.SPACE.equals(tokens.get(index))) {
            return false;
        }

        for (int i = index + 1; i < upperBound; i++) {
            final String token = tokens.get(i);

            if (SK.PARENTHESIS_L.equals(token)) {
                return true;
            } else if (!SK.SPACE.equals(token)) {
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
     * @see #isReadOrInsertQuery(String)
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
     * (see {@link #isSelectQuery(String)}) <i>and</i> it contains no top-level mutation, DDL, or procedure-invocation keyword
     * ({@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code MERGE}, {@code REPLACE}, {@code TRUNCATE},
     * {@code CREATE}, {@code ALTER} or {@code DROP}), no procedure invocation ({@code CALL}, JDBC
     * {@code {call ...}} / {@code {? = call ...}}, {@code EXEC} or {@code EXECUTE}), and no standalone
     * {@code SELECT ... INTO ...} clause. The {@code INTO} check is limited to the SELECT list
     * before that SELECT's {@code FROM}; table names after {@code FROM} and qualified identifiers
     * such as {@code t.into} do not count as {@code SELECT ... INTO}. Keyword matching ignores
     * occurrences inside quoted string literals, quoted identifiers, SQL comments and larger
     * identifier tokens, so a SELECT that merely returns the literal text {@code 'DELETE'} or a
     * column named {@code into$} is still treated as read-only, whereas a data-changing CTE such as
     * {@code WITH t AS (...) DELETE ...} is not. For multi-statement SQL, a later statement is
     * permitted only when it also resolves to a {@code SELECT}; a later statement with any other
     * leading verb (including an unrecognized or vendor-specific command) makes the SQL non-read-only.
     * This includes statements that start with a {@code WITH} clause or leading parentheses. The
     * mutation-keyword scan matches only statement-start positions, so the
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
     * @see #isReadOrInsertQuery(String)
     */
    public static boolean isReadOnlyQuery(final String sql) {
        return isReadOnlyQuery(sql, DEFAULT_TOKENIZER_CONFIG);
    }

    private static boolean isReadOnlyQuery(final String sql, final TokenizerConfig tokenizerConfig) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        return isSelectQuery(sql, tokenizerConfig) && hasOnlyAllowedTopLevelStatements(sql, tokenizerConfig, false)
                && !containsMutationQueryKeyword(sql, tokenizerConfig) && !containsSelectIntoClause(sql, tokenizerConfig);
    }

    private static boolean isSelectQuery(final String sql, final TokenizerConfig tokenizerConfig) {
        return !Strings.isEmpty(sql) && "SELECT".equalsIgnoreCase(getLeadingQueryKeyword(sql, tokenizerConfig));
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
     * @see #isReadOrInsertQuery(String)
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
     * @see #isReadOrInsertQuery(String)
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
     * @see #isReadOrInsertQuery(String)
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
     * @see #isReadOrInsertQuery(String)
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
     * Checks whether the given SQL statement is a read or a plain/safe insert. This method permits
     * reads and inserts of new rows but rejects statements that can mutate existing data.
     * <p>
     * A statement qualifies as read-or-insert only if its leading keyword is {@code SELECT} or
     * {@code INSERT} <i>and</i> it contains none of the following (matching outside of quoted string
     * literals and SQL comments):
     * </p>
     * <ul>
     *   <li>a top-level {@code UPDATE}, {@code DELETE}, {@code MERGE}, {@code REPLACE}, {@code TRUNCATE},
     *       {@code CREATE}, {@code ALTER} or {@code DROP} keyword (matched only at statement-start
     *       positions, so e.g. {@code SELECT ... FOR UPDATE} is still accepted);</li>
     *   <li>a procedure invocation introduced by {@code CALL}, JDBC {@code {call ...}} /
     *       {@code {? = call ...}}, {@code EXEC} or {@code EXECUTE}; or</li>
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
     * {@code DELETE}, {@code MERGE}, {@code REPLACE}, {@code TRUNCATE}, {@code CREATE}, {@code DROP},
     * {@code ALTER}, {@code CALL}, JDBC {@code {call ...}} / {@code {? = call ...}}, {@code EXEC} or
     * {@code EXECUTE} statement makes this method return {@code false}. More generally, every
     * top-level statement must resolve to either {@code SELECT} or {@code INSERT}; an unrecognized
     * or vendor-specific command is rejected rather than assumed to be safe. Procedure calls are
     * conservatively rejected because their effects cannot be determined from the SQL text; the keyword scan matches
     * only statement-start positions, so the {@code REPLACE(...)}/{@code TRUNCATE(...)} SQL
     * <i>functions</i> do not affect the classification.
     * </p>
     *
     * <p><b>Comparison with related methods:</b> see the
     * <a href="#query-classification">query-classification table</a> in the class documentation for how
     * this predicate relates to the other {@code is...Query} methods.</p>
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return {@code true} for an accepted read or plain/safe insert; {@code false} otherwise,
     *         including for a {@code null} or empty statement
     * @see #isSelectQuery(String)
     * @see #isInsertQuery(String)
     * @see #isUpdateQuery(String)
     * @see #isDeleteQuery(String)
     * @see #isInsertOrReplaceQuery(String)
     * @see #isReadOnlyQuery(String)
     * @see #isNonUpdateQuery(String)
     */
    public static boolean isReadOrInsertQuery(final String sql) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        if (!(isSelectQuery(sql) || isInsertQuery(sql))) {
            return false;
        }

        // See the note in containsMutationQueryKeyword: statement-start-only matching keeps the
        // REPLACE(...)/TRUNCATE(...) functions from false-positiving.
        return hasOnlyAllowedTopLevelStatements(sql, DEFAULT_TOKENIZER_CONFIG, true) && !containsQueryKeyword(sql, "UPDATE")
                && !containsQueryKeyword(sql, "DELETE") && !containsQueryKeyword(sql, "MERGE") && !containsQueryKeyword(sql, "REPLACE")
                && !containsQueryKeyword(sql, "TRUNCATE") && !containsQueryKeyword(sql, "DROP") && !containsQueryKeyword(sql, "ALTER")
                && !containsQueryKeyword(sql, "CREATE") && !containsProcedureInvocation(sql, DEFAULT_TOKENIZER_CONFIG) && !containsInsertUpdateClause(sql)
                && !containsSelectIntoClause(sql) && !containsTokenSequence(sql, "INSERT", "OVERWRITE");
    }

    /**
     * Compatibility alias for the historical {@code NoUpdateDao} terminology. Despite its name,
     * this predicate accepts both reads and plain/safe inserts.
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return the result of {@link #isReadOrInsertQuery(String)}
     * @deprecated use {@link #isReadOrInsertQuery(String)}, whose name describes the accepted statement kinds
     */
    @Deprecated
    public static boolean isNonUpdateQuery(final String sql) {
        return isReadOrInsertQuery(sql);
    }

    /**
     * Compatibility alias for the original public API name. Despite its historical name, this
     * predicate accepts both reads and plain/safe inserts.
     *
     * @param sql the SQL statement to check; may be empty or {@code null}
     * @return the result of {@link #isReadOrInsertQuery(String)}
     * @deprecated use {@link #isReadOrInsertQuery(String)}, whose name describes the accepted statement kinds
     */
    @Deprecated
    public static boolean isNoUpdateQuery(final String sql) {
        return isReadOrInsertQuery(sql);
    }

    /**
     * Verifies the leading verb of every semicolon-delimited top-level statement. This is an
     * allowlist complement to the more detailed mutation/clause scanners: an unknown command must
     * not become "safe" merely because its verb is absent from their finite mutation keyword list.
     * Semicolons inside quoted regions, comments, bracket identifiers, or parentheses do not split
     * a statement.
     */
    private static boolean hasOnlyAllowedTopLevelStatements(final String sql, final TokenizerConfig tokenizerConfig, final boolean allowInsert) {
        final int sqlLength = sql.length();
        int statementStart = 0;
        int depth = 0;
        int index = 0;

        while (index < sqlLength) {
            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                continue;
            } else if (ch == '-' && index + 1 < sqlLength && sql.charAt(index + 1) == '-') {
                index += 2;

                while (index < sqlLength && sql.charAt(index) != ENTER && sql.charAt(index) != ENTER_2) {
                    index++;
                }

                continue;
            } else if (ch == '#' && isHashCommentStart(sql, sqlLength, index, tokenizerConfig)) {
                do {
                    index++;
                } while (index < sqlLength && sql.charAt(index) != ENTER && sql.charAt(index) != ENTER_2);

                continue;
            } else if (ch == '/' && index + 1 < sqlLength && sql.charAt(index + 1) == '*') {
                index += 2;

                while (index + 1 < sqlLength && !(sql.charAt(index) == '*' && sql.charAt(index + 1) == '/')) {
                    index++;
                }

                index = Math.min(index + 2, sqlLength);
                continue;
            }

            if (ch == '(') {
                depth++;
            } else if (ch == ')' && depth > 0) {
                depth--;
            } else if (ch == ';' && depth == 0) {
                if (!isAllowedTopLevelStatement(sql, statementStart, index, tokenizerConfig, allowInsert)) {
                    return false;
                }

                statementStart = index + 1;
            }

            index++;
        }

        return isAllowedTopLevelStatement(sql, statementStart, sqlLength, tokenizerConfig, allowInsert);
    }

    private static boolean isAllowedTopLevelStatement(final String sql, final int fromIndex, final int toIndex, final TokenizerConfig tokenizerConfig,
            final boolean allowInsert) {
        final String statement = sql.substring(fromIndex, toIndex);

        if (skipLeadingWhitespaceAndComments(statement, 0, tokenizerConfig) >= statement.length()) {
            return true; // Empty statement (including comments only), e.g. a trailing semicolon.
        }

        final String keyword = getLeadingQueryKeyword(statement, tokenizerConfig);
        return "SELECT".equalsIgnoreCase(keyword) || allowInsert && "INSERT".equalsIgnoreCase(keyword);
    }

    private static boolean containsMutationQueryKeyword(final String sql, final TokenizerConfig tokenizerConfig) {
        // containsQueryKeyword matches only at statement-start positions (start of SQL, after ';', or a CTE
        // body's "AS ("), so the REPLACE(...)/TRUNCATE(...) string/numeric FUNCTIONS -- which always appear
        // mid-statement -- cannot false-positive here.
        return containsQueryKeyword(sql, "INSERT", tokenizerConfig) || containsQueryKeyword(sql, "UPDATE", tokenizerConfig)
                || containsQueryKeyword(sql, "DELETE", tokenizerConfig) || containsQueryKeyword(sql, "MERGE", tokenizerConfig)
                || containsQueryKeyword(sql, "REPLACE", tokenizerConfig) || containsQueryKeyword(sql, "TRUNCATE", tokenizerConfig)
                || containsQueryKeyword(sql, "DROP", tokenizerConfig) || containsQueryKeyword(sql, "ALTER", tokenizerConfig)
                || containsQueryKeyword(sql, "CREATE", tokenizerConfig) || containsProcedureInvocation(sql, tokenizerConfig);
    }

    /**
     * Detects procedure invocations only where a statement verb may begin. Besides standard
     * {@code CALL}, this recognizes the JDBC call escapes {@code {call ...}} and
     * {@code {? = call ...}}, plus SQL Server's {@code EXEC}/{@code EXECUTE}. Quoted text,
     * quoted identifiers, comments, larger identifier tokens, and function-like tokens appearing
     * after a statement's leading verb are ignored.
     */
    private static boolean containsProcedureInvocation(final String sql, final TokenizerConfig tokenizerConfig) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        int index = 0;
        boolean canStartStatement = true;

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index, tokenizerConfig);

            if (index >= sql.length()) {
                break;
            }

            final char ch = sql.charAt(index);

            if (ch == '\'' || ch == '"' || ch == '`') {
                index = skipQuotedLiteral(sql, index, ch);
                canStartStatement = false;
                continue;
            } else if (ch == '[') {
                index = skipBracketQuotedIdentifier(sql, index);
                canStartStatement = false;
                continue;
            }

            if (canStartStatement) {
                if (ch == '(') {
                    index++;
                    continue;
                }

                if (ch == '{') {
                    if (isJdbcCallEscape(sql, index, tokenizerConfig)) {
                        return true;
                    }

                    canStartStatement = false;
                    index++;
                    continue;
                }

                if (Character.isLetter(ch)) {
                    final String token = readKeyword(sql, index, tokenizerConfig);

                    if ("CALL".equalsIgnoreCase(token) || "EXEC".equalsIgnoreCase(token) || "EXECUTE".equalsIgnoreCase(token)) {
                        return true;
                    }

                    canStartStatement = false;
                    index += token.length();
                    continue;
                }
            }

            if (ch == ';') {
                canStartStatement = true;
            } else if (!Character.isWhitespace(ch)) {
                canStartStatement = false;
            }

            index++;
        }

        return false;
    }

    private static boolean isJdbcCallEscape(final String sql, final int openingBraceIndex, final TokenizerConfig tokenizerConfig) {
        int index = skipLeadingWhitespaceAndComments(sql, openingBraceIndex + 1, tokenizerConfig);

        if (index < sql.length() && sql.charAt(index) == '?') {
            index = skipLeadingWhitespaceAndComments(sql, index + 1, tokenizerConfig);

            if (index >= sql.length() || sql.charAt(index) != '=') {
                return false;
            }

            index = skipLeadingWhitespaceAndComments(sql, index + 1, tokenizerConfig);
        }

        return "CALL".equalsIgnoreCase(readKeyword(sql, index, tokenizerConfig));
    }

    private static boolean containsInsertUpdateClause(final String sql) {
        return containsTokenSequence(sql, "INSERT", "OR", "REPLACE") || containsTokenSequence(sql, "ON", "DUPLICATE", "KEY", "UPDATE")
                || containsOnConflictDoUpdateClause(sql);
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
        return containsSelectIntoClause(sql, DEFAULT_TOKENIZER_CONFIG);
    }

    private static boolean containsSelectIntoClause(final String sql, final TokenizerConfig tokenizerConfig) {
        // Scan every SELECT in the SQL, not only when the first statement is a SELECT. The
        // no-update gate also permits a leading INSERT, and a later statement such as
        // "INSERT ...; SELECT value INTO new_table ..." must not be allowed to create a table.
        // containsSelectListIntoToken already distinguishes INSERT INTO from SELECT-list INTO.
        return containsSelectListIntoToken(sql, tokenizerConfig);
    }

    private static boolean containsSelectListIntoToken(final String sql, final TokenizerConfig tokenizerConfig) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        final List<Boolean> selectBeforeFromByDepth = new ArrayList<>(4);
        int index = 0;
        int depth = 0;

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index, tokenizerConfig);

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
                } else if ("FROM".equalsIgnoreCase(token) && !isDotQualifiedToken(sql, index, index + token.length(), tokenizerConfig)) {
                    if (isSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth)) {
                        setSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth, false);
                    }
                } else if ("INTO".equalsIgnoreCase(token) && isSelectBeforeFromAtDepth(selectBeforeFromByDepth, depth)
                        && !isDotQualifiedToken(sql, index, index + token.length(), tokenizerConfig)) {
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

    private static boolean isDotQualifiedToken(final String sql, final int startIndex, final int endIndex, final TokenizerConfig tokenizerConfig) {
        final int previousIndex = skipBackwardWhitespaceAndComments(sql, startIndex - 1, tokenizerConfig);

        if (previousIndex >= 0 && sql.charAt(previousIndex) == '.') {
            return true;
        }

        final int nextIndex = skipLeadingWhitespaceAndComments(sql, endIndex, tokenizerConfig);

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
        return containsQueryKeyword(sql, keywordToFind, DEFAULT_TOKENIZER_CONFIG);
    }

    private static boolean containsQueryKeyword(final String sql, final String keywordToFind, final TokenizerConfig tokenizerConfig) {
        if (Strings.isEmpty(sql)) {
            return false;
        }

        int index = 0;
        boolean canStartQueryKeyword = true;
        String previousKeyword = "";

        while (index < sql.length()) {
            index = skipLeadingWhitespaceAndComments(sql, index, tokenizerConfig);

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
                final String token = readKeyword(sql, index, tokenizerConfig);

                if (canStartQueryKeyword && "WITH".equalsIgnoreCase(token)) {
                    final int queryKeywordIndex = findKeywordIndexAfterWithClause(sql, index + token.length(), tokenizerConfig);

                    if (queryKeywordIndex >= 0 && keywordToFind.equalsIgnoreCase(readKeyword(sql, queryKeywordIndex, tokenizerConfig))) {
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
        return getLeadingQueryKeyword(sql, DEFAULT_TOKENIZER_CONFIG);
    }

    private static String getLeadingQueryKeyword(final String sql, final TokenizerConfig tokenizerConfig) {
        final int index = getLeadingQueryKeywordIndex(sql, tokenizerConfig);
        return index >= 0 ? readKeyword(sql, index, tokenizerConfig) : "";
    }

    private static int getLeadingQueryKeywordIndex(final String sql) {
        return getLeadingQueryKeywordIndex(sql, DEFAULT_TOKENIZER_CONFIG);
    }

    private static int getLeadingQueryKeywordIndex(final String sql, final TokenizerConfig tokenizerConfig) {
        if (Strings.isEmpty(sql)) {
            return -1;
        }

        int index = skipLeadingWhitespaceAndComments(sql, 0, tokenizerConfig);

        // A query may be wrapped in one or more leading parentheses, e.g. "(SELECT 1)" or
        // "(SELECT a FROM t1) UNION ALL (SELECT a FROM t2)". Skip past those so the leading verb
        // (SELECT/INSERT/...) is still recognized instead of being classified as no leading keyword.
        while (index < sql.length() && sql.charAt(index) == '(') {
            index = skipLeadingWhitespaceAndComments(sql, index + 1, tokenizerConfig);
        }

        if (index >= sql.length()) {
            return -1;
        }

        String keyword = readKeyword(sql, index, tokenizerConfig);

        if (Strings.isEmpty(keyword)) {
            return -1;
        }

        if (!"WITH".equalsIgnoreCase(keyword)) {
            return index;
        }

        index += keyword.length();
        index = skipLeadingWhitespaceAndComments(sql, index, tokenizerConfig);

        keyword = readKeyword(sql, index, tokenizerConfig);

        if ("RECURSIVE".equalsIgnoreCase(keyword)) {
            index += keyword.length();
        }

        return findKeywordIndexAfterWithClause(sql, index, tokenizerConfig);
    }

    private static int findKeywordIndexAfterWithClause(final String sql, int fromIndex, final TokenizerConfig tokenizerConfig) {
        int depth = 0;

        while (fromIndex < sql.length()) {
            fromIndex = skipLeadingWhitespaceAndComments(sql, fromIndex, tokenizerConfig);

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
                final String token = readKeyword(sql, fromIndex, tokenizerConfig);

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
        return skipLeadingWhitespaceAndComments(sql, fromIndex, DEFAULT_TOKENIZER_CONFIG);
    }

    private static int skipLeadingWhitespaceAndComments(final String sql, int fromIndex, final TokenizerConfig tokenizerConfig) {
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

            if (sql.charAt(fromIndex) == '#' && isHashCommentStart(sql, sql.length(), fromIndex, tokenizerConfig)) {
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
        return readKeyword(sql, fromIndex, DEFAULT_TOKENIZER_CONFIG);
    }

    private static String readKeyword(final String sql, int fromIndex, final TokenizerConfig tokenizerConfig) {
        fromIndex = skipLeadingWhitespaceAndComments(sql, fromIndex, tokenizerConfig);

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
