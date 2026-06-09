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
 *   <li>optional product metadata, currently stored as {@link #productVersion}.</li>
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
 * {@link IdentifierQuote#DOUBLE_QUOTE}. The {@link #productVersion} value is descriptive metadata only
 * and does not change SQL rendering.</p>
 *
 * @see SqlBuilder.Dsl#forDialect(SqlDialect)
 * @see AbstractQueryBuilder
 */
@Builder
@Value
@Accessors(fluent = true)
public class SqlDialect {

    /**
     * Optional database product/version label, such as {@code "MySQL 9.7"} or {@code "PostgreSQL 18"}.
     * This value is metadata for callers and diagnostics; query builders do not branch on it.
     */
    private String productVersion;

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
     * Immutable database product descriptor for callers that want to keep a product name and version
     * separately.
     *
     * <p>This type is metadata only. SQL rendering is controlled by {@link SqlDialect}'s
     * {@link #namingPolicy}, {@link #sqlPolicy}, and {@link #identifierQuote} values.</p>
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
    }
}
