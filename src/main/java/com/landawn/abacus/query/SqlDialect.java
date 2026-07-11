/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query;

import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Immutable configuration object used by {@link Dsl} to render generated SQL.
 *
 * <p>A {@code SqlDialect} is not a complete database grammar. It captures the rendering choices that
 * query builders need while composing SQL:</p>
 * <ul>
 *   <li>the {@link NamingPolicy} used to translate Java property names into SQL identifiers, such as
 *       {@code firstName} to {@code first_name};</li>
 *   <li>the {@link SqlPolicy} used to render values as raw literals, positional {@code ?} parameters,
 *       named {@code :name} parameters, or iBATIS/MyBatis {@code #{name}} parameters;</li>
 *   <li>the {@link IdentifierQuote} used when generated aliases or identifiers must be quoted; and</li>
 *   <li>the optional {@link ProductInfo} identifying the target database product, which query builders
 *       use to emit product-specific SQL such as pagination clauses.</li>
 * </ul>
 *
 * <p>The predefined {@link Dsl} constants, such as {@link Dsl#PSC} and
 * {@link Dsl#NSC}, are each backed by a distinct dialect. Custom DSLs can be created with
 * {@link Dsl#forDialect(SqlDialect)}:</p>
 * <pre>{@code
 * Dsl myDsl = Dsl.forDialect(SqlDialect.builder()
 *         .namingPolicy(NamingPolicy.SNAKE_CASE)
 *         .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
 *         .identifierQuote(SqlDialect.IdentifierQuote.DOUBLE_QUOTE)
 *         .build());
 * }</pre>
 *
 * <p>Fields left unset on the builder remain {@code null}. When a dialect is used by
 * {@link AbstractQueryBuilder}, unset rendering choices are resolved to these defaults:
 * {@link NamingPolicy#SNAKE_CASE}, {@link SqlPolicy#RAW_SQL}, and
 * {@link IdentifierQuote#DOUBLE_QUOTE} (or {@link IdentifierQuote#BACKTICK} when {@code productInfo}
 * names MySQL or MariaDB). When {@code productInfo} is set, query builders also adapt
 * product-specific SQL syntax such as pagination clauses; see {@link AbstractQueryBuilder#limit(int)}.
 * When it is {@code null} or its name is not recognized, builders generate the default
 * {@code LIMIT}/{@code OFFSET} syntax.</p>
 *
 * @see Dsl#forDialect(SqlDialect)
 * @see AbstractQueryBuilder
 */
@Builder
@Value
@Accessors(fluent = true)
public class SqlDialect {

    /**
     * Optional descriptor of the target database product. When set, query builders branch on
     * {@link ProductInfo#name()} to emit product-specific SQL: Oracle, DB2 and SQL Server dialects
     * render pagination with {@code OFFSET ... ROWS} / {@code FETCH ... ROWS ONLY} instead of
     * {@code LIMIT}/{@code OFFSET}, and a {@code null} {@code identifierQuote} defaults to
     * {@link IdentifierQuote#BACKTICK} for MySQL/MariaDB. When {@code null}, builders use the default
     * SQL syntax.
     */
    private ProductInfo productInfo;

    /**
     * Naming policy used to translate Java property names into generated SQL identifiers. For example,
     * {@link NamingPolicy#SNAKE_CASE} renders {@code firstName} as {@code first_name}. When {@code null},
     * builders use {@link NamingPolicy#SNAKE_CASE}.
     */
    private NamingPolicy namingPolicy;

    /**
     * Parameter rendering policy for values supplied to builder operations. When {@code null}, builders
     * use {@link SqlPolicy#RAW_SQL}.
     */
    private SqlPolicy sqlPolicy;

    /**
     * Quote style used for generated aliases and identifiers that need quoting. When {@code null},
     * builders use {@link IdentifierQuote#DOUBLE_QUOTE}, except for MySQL/MariaDB product metadata,
     * which defaults to {@link IdentifierQuote#BACKTICK}.
     */
    private IdentifierQuote identifierQuote;

    /**
     * Identifier quoting style used by SQL builders.
     *
     * <p>The enum currently distinguishes the two quote characters supported by this builder:
     * ANSI double quotes and MySQL-style backticks.</p>
     */
    public static enum IdentifierQuote {
        /**
         * ANSI/standard SQL double quote ({@code "}). This is the effective default when
         * {@code identifierQuote} is {@code null}, except when {@code productInfo} names MySQL or
         * MariaDB, in which case {@link #BACKTICK} is the default.
         */
        DOUBLE_QUOTE,

        /**
         * MySQL/MariaDB-style backtick ({@code `}).
         */
        BACKTICK;
    }

    /**
     * Defines how values supplied to query builders are represented in generated SQL.
     *
     * <p>This setting controls value placeholders only. It does not change table/column naming,
     * identifier quoting, or database-specific SQL syntax.</p>
     */
    public static enum SqlPolicy {
        /**
         * Inline values directly into the SQL string as literals.
         *
         * <p><b>&#9888;&#65039;</b> Use only for trusted values; parameterized or named policies are preferred for user input.</p>
         */
        RAW_SQL,

        /**
         * Render each value as a positional {@code ?} placeholder and collect parameter values in order.
         */
        PARAMETERIZED_SQL,

        /**
         * Render values as named placeholders, such as {@code :id} or {@code :firstName}.
         */
        NAMED_SQL,

        /**
         * Render values as iBATIS/MyBatis-style named placeholders, such as {@code #{id}}.
         */
        IBATIS_SQL
    }

    /**
     * Immutable descriptor of a database product, holding the product name and version separately.
     *
     * <p>When attached to a dialect via {@code SqlDialect.productInfo}, the {@link #name()} drives
     * product-specific SQL generation in query builders (for example, Oracle-style
     * {@code FETCH FIRST ... ROWS ONLY} pagination). The name is matched case-insensitively as a
     * substring, so raw JDBC values from {@code DatabaseMetaData.getDatabaseProductName()} such as
     * {@code "Microsoft SQL Server"} or {@code "Oracle Database 19c"} are recognized. The
     * {@link #version()} can be compared numerically via {@link #isVersionAtLeast(String)} and
     * {@link #isVersionAtMost(String)}.</p>
     *
     * @param name the database product name, such as {@code "MySQL"} or {@code "PostgreSQL"}
     * @param version the database product version, such as {@code "9.7"} or {@code "18"}; a {@code null}
     *        version is normalized to an empty string, so {@link #version()} never returns {@code null}.
     *        Comparable via {@link #isVersionAtLeast(String)} / {@link #isVersionAtMost(String)}
     */
    public record ProductInfo(String name, String version) {

        /**
         * Canonical constructor that normalizes a {@code null} {@code version} to an empty string, so
         * {@link #version()} never returns {@code null} and an absent version has a single canonical
         * representation. This keeps {@code equals}/{@code hashCode} consistent whether the version was
         * omitted (via {@link #of(String)}) or passed as {@code null}.
         */
        public ProductInfo {
            version = version == null ? "" : version;
        }

        /**
         * Creates a {@code ProductInfo} with the given product name and no version (an empty {@link #version()}).
         *
         * @param name the database product name, such as {@code "Oracle"} or {@code "MySQL"}
         * @return a new {@code ProductInfo} with the given name and an empty ({@code ""}) version
         */
        public static ProductInfo of(final String name) {
            return new ProductInfo(name, "");
        }

        /**
         * Creates a {@code ProductInfo} with the given product name and version.
         *
         * @param name the database product name, such as {@code "Oracle"} or {@code "MySQL"}
         * @param version the database product version, such as {@code "19c"} or {@code "9.7"}
         * @return a new {@code ProductInfo} with the given name and version
         */
        public static ProductInfo of(final String name, final String version) {
            return new ProductInfo(name, version);
        }

        /**
         * Returns whether this descriptor names MySQL. The match is a case-insensitive substring test
         * against {@link #name()}. This is distinct from {@link #isMariaDB()}, although both share the
         * same {@code LIMIT}/{@code OFFSET} pagination and backtick-quoting behavior in query builders.
         * Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "mysql"} (case-insensitively)
         */
        public boolean isMySQL() {
            return Strings.containsIgnoreCase(name, "mysql");
        }

        /**
         * Returns whether this descriptor names MariaDB. The match is a case-insensitive substring test
         * against {@link #name()}. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "mariadb"} (case-insensitively)
         */
        public boolean isMariaDB() {
            return Strings.containsIgnoreCase(name, "mariadb");
        }

        /**
         * Returns whether this descriptor names PostgreSQL. The match is a case-insensitive substring
         * test against {@link #name()} (the substring {@code "postgres"} also matches {@code "PostgreSQL"}).
         * Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "postgres"} (case-insensitively)
         */
        public boolean isPostgreSQL() {
            return Strings.containsIgnoreCase(name, "postgres");
        }

        /**
         * Returns whether this descriptor names Microsoft SQL Server. The match is a case-insensitive
         * substring test against {@link #name()}, so raw JDBC values such as {@code "Microsoft SQL Server"}
         * are recognized. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "sql server"} or {@code "sqlserver"} (case-insensitively)
         */
        public boolean isSQLServer() {
            return Strings.containsIgnoreCase(name, "sql server") || Strings.containsIgnoreCase(name, "sqlserver");
        }

        /**
         * Returns whether this descriptor names Oracle Database. The match is a case-insensitive
         * substring test against {@link #name()}, so raw JDBC values from
         * {@code DatabaseMetaData.getDatabaseProductName()} such as {@code "Oracle Database 19c"}
         * are recognized. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "oracle"} (case-insensitively)
         */
        public boolean isOracle() {
            return Strings.containsIgnoreCase(name, "oracle");
        }

        /**
         * Returns whether this descriptor names IBM DB2. The match is a case-insensitive substring
         * test against {@link #name()}. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "db2"} (case-insensitively)
         */
        public boolean isDB2() {
            return Strings.containsIgnoreCase(name, "db2");
        }

        /**
         * Returns whether this descriptor names H2 Database. The match is a case-insensitive substring
         * test against {@link #name()}. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "h2"} (case-insensitively)
         */
        public boolean isH2() {
            return Strings.containsIgnoreCase(name, "h2");
        }

        /**
         * Returns whether this descriptor names SQLite. The match is a case-insensitive substring test
         * against {@link #name()}. Returns {@code false} when {@link #name()} is {@code null}.
         *
         * @return {@code true} if {@link #name()} contains {@code "sqlite"} (case-insensitively)
         */
        public boolean isSQLite() {
            return Strings.containsIgnoreCase(name, "sqlite");
        }

        /**
         * Returns whether this product's {@link #version()} is greater than or equal to the given version.
         *
         * <p>Versions are compared by their leading dot-separated run of integer components: for example
         * {@code "8.0.32"} parses to {@code [8, 0, 32]} and {@code "19c"} to {@code [19]} (parsing stops at
         * the first character that is neither a digit nor a dot). Missing trailing components are treated as
         * {@code 0}, so {@code "8.0"} equals {@code "8.0.0"} and is less than {@code "8.1"}.</p>
         *
         * <p>Returns {@code false} (not comparable) when either this {@link #version()} or {@code minVersion}
         * is {@code null}, blank, or does not begin with an integer component. This makes the method safe for
         * feature-gating: an unknown or unparseable version simply fails the check.</p>
         *
         * @param minVersion the minimum version to compare against, such as {@code "8.0"} or {@code "19"}
         * @return {@code true} if this product's version parses and is greater than or equal to {@code minVersion};
         *         {@code false} otherwise, including when either version is not comparable
         * @see #isVersionAtMost(String)
         */
        public boolean isVersionAtLeast(final String minVersion) {
            final long[] mine = parseVersionComponents(version());
            final long[] other = parseVersionComponents(minVersion);
            return mine != null && other != null && compareVersionComponents(mine, other) >= 0;
        }

        /**
         * Returns whether this product's {@link #version()} is less than or equal to the given version.
         *
         * <p>Versions are compared by their leading dot-separated run of integer components; see
         * {@link #isVersionAtLeast(String)} for the parsing and padding rules.</p>
         *
         * <p>Returns {@code false} (not comparable) when either this {@link #version()} or {@code maxVersion}
         * is {@code null}, blank, or does not begin with an integer component.</p>
         *
         * @param maxVersion the maximum version to compare against, such as {@code "8.0"} or {@code "19"}
         * @return {@code true} if this product's version parses and is less than or equal to {@code maxVersion};
         *         {@code false} otherwise, including when either version is not comparable
         * @see #isVersionAtLeast(String)
         */
        public boolean isVersionAtMost(final String maxVersion) {
            final long[] mine = parseVersionComponents(version());
            final long[] other = parseVersionComponents(maxVersion);
            return mine != null && other != null && compareVersionComponents(mine, other) <= 0;
        }

        /**
         * Parses the leading dot-separated run of integer components of a version string (for example
         * {@code "8.0.32"} to {@code [8, 0, 32]} and {@code "19c"} to {@code [19]}), or {@code null} when the
         * string is {@code null}, blank, does not begin with a digit, or has an unparseable component.
         */
        private static long[] parseVersionComponents(final String version) {
            if (Strings.isBlank(version)) {
                return null;
            }

            final String trimmed = version.trim();

            if (!Character.isDigit(trimmed.charAt(0))) {
                return null;
            }

            int end = 0;

            while (end < trimmed.length() && (Character.isDigit(trimmed.charAt(end)) || trimmed.charAt(end) == '.')) {
                end++;
            }

            final String[] parts = trimmed.substring(0, end).split("\\.");
            final long[] components = new long[parts.length];

            try {
                for (int i = 0; i < parts.length; i++) {
                    components[i] = Long.parseLong(parts[i]);
                }
            } catch (final NumberFormatException e) {
                return null;
            }

            return components;
        }

        /**
         * Compares two version-component arrays element by element, treating missing trailing components as
         * {@code 0}. Returns a negative, zero, or positive value when {@code a} is respectively less than,
         * equal to, or greater than {@code b}.
         */
        private static int compareVersionComponents(final long[] a, final long[] b) {
            final int len = Math.max(a.length, b.length);

            for (int i = 0; i < len; i++) {
                final long av = i < a.length ? a[i] : 0L;
                final long bv = i < b.length ? b[i] : 0L;

                if (av != bv) {
                    return av < bv ? -1 : 1;
                }
            }

            return 0;
        }
    }
}
