package com.landawn.abacus.query;

import com.landawn.abacus.util.NamingPolicy;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Immutable configuration object used by {@link SqlBuilder.Dsl} to render generated SQL.
 *
 * <p>A {@code SqlDialect} is not a complete database grammar. It captures the rendering choices that
 * query builders need while composing SQL:</p>
 * <ul>
 *   <li>the {@link NamingPolicy} used to translate Java property names into SQL identifiers, such as
 *       {@code firstName} to {@code first_name};</li>
 *   <li>the {@link SQLPolicy} used to render values as raw literals, positional {@code ?} parameters,
 *       named {@code :name} parameters, or iBATIS/MyBatis {@code #{name}} parameters;</li>
 *   <li>the {@link IdentifierQuote} used when generated aliases or identifiers must be quoted; and</li>
 *   <li>the optional {@link ProductInfo} identifying the target database product, which query builders
 *       use to emit product-specific SQL such as pagination clauses.</li>
 * </ul>
 *
 * <p>The predefined {@link SqlBuilder} DSL constants, such as {@link SqlBuilder#PSC} and
 * {@link SqlBuilder#NSC}, are each backed by a distinct dialect. Custom DSLs can be created with
 * {@link SqlBuilder.Dsl#forDialect(SqlDialect)}:</p>
 * <pre>{@code
 * SqlBuilder.Dsl myDsl = SqlBuilder.Dsl.forDialect(SqlDialect.builder()
 *         .namingPolicy(NamingPolicy.SNAKE_CASE)
 *         .sqlPolicy(SqlDialect.SQLPolicy.PARAMETERIZED_SQL)
 *         .identifierQuote(SqlDialect.IdentifierQuote.DOUBLE_QUOTE)
 *         .build());
 * }</pre>
 *
 * <p>Fields left unset on the builder remain {@code null}. When a dialect is used by
 * {@link AbstractQueryBuilder}, unset rendering choices are resolved to these defaults:
 * {@link NamingPolicy#SNAKE_CASE}, {@link SQLPolicy#RAW_SQL}, and
 * {@link IdentifierQuote#DOUBLE_QUOTE} (or {@link IdentifierQuote#BACKTICK} when {@link #productInfo}
 * names MySQL or MariaDB). When {@link #productInfo} is set, query builders also adapt
 * product-specific SQL syntax such as pagination clauses; see {@link AbstractQueryBuilder#limit(int)}.
 * When it is {@code null} or its name is not recognized, builders generate the default
 * {@code LIMIT}/{@code OFFSET} syntax.</p>
 *
 * @see SqlBuilder.Dsl#forDialect(SqlDialect)
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
     * {@code LIMIT}/{@code OFFSET}, and a {@code null} {@link #identifierQuote} defaults to
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
     * use {@link SQLPolicy#RAW_SQL}.
     */
    private SqlDialect.SQLPolicy sqlPolicy;

    /**
     * Quote style used for generated aliases and identifiers that need quoting. When {@code null},
     * builders use {@link IdentifierQuote#DOUBLE_QUOTE}.
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
         * {@link #identifierQuote} is {@code null}.
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
    public static enum SQLPolicy {
        /**
         * Inline values directly into the SQL string as literals.
         *
         * <p>Use only for trusted values; parameterized or named policies are preferred for user input.</p>
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
     * <p>When attached to a dialect via {@link SqlDialect#productInfo}, the {@link #name} drives
     * product-specific SQL generation in query builders (for example, Oracle-style
     * {@code FETCH FIRST ... ROWS ONLY} pagination). The name is matched case-insensitively as a
     * substring, so raw JDBC values from {@code DatabaseMetaData.getDatabaseProductName()} such as
     * {@code "Microsoft SQL Server"} or {@code "Oracle Database 19c"} are recognized. The
     * {@link #version} is descriptive metadata only.</p>
     */
    @Builder
    @Value
    @Accessors(fluent = true)
    public static class ProductInfo {
        /**
         * Database product name, such as {@code "MySQL"} or {@code "PostgreSQL"}.
         */
        private String name;

        /**
         * Database product version, such as {@code "9.7"} or {@code "18"}.
         */
        private String version;

        /**
         * Creates a {@code ProductInfo} with the given product name and no version.
         *
         * @param name the database product name, such as {@code "Oracle"} or {@code "MySQL"}
         * @return a new {@code ProductInfo} with the given name and a {@code null} version
         */
        public static ProductInfo of(final String name) {
            return new ProductInfo(name, null);
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
    }
}
