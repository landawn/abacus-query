package com.landawn.abacus.query;

import com.landawn.abacus.query.AbstractQueryBuilder.SQLPolicy;
import com.landawn.abacus.util.NamingPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Configuration that defines a SQL "dialect" for a {@link SqlBuilder.Dsl}. A dialect captures how
 * generated SQL should look through three rendering choices:
 * <ul>
 *   <li>the {@link NamingPolicy} used to translate property names into column names
 *       (e.g. {@code firstName} &rarr; {@code first_name});</li>
 *   <li>the {@link SQLPolicy} that controls the parameter style: raw literals, positional {@code ?},
 *       named {@code :name}, or iBATIS {@code #{name}}; and</li>
 *   <li>the {@link IdentifierQuote} used when an identifier (such as a column name) has to be quoted.</li>
 * </ul>
 *
 * <p>It also carries an optional {@link #productVersion} string identifying the target database product
 * and version (e.g. {@code "MySQL 9.7"}) as descriptive metadata.</p>
 *
 * <p>Each predefined {@link SqlBuilder} constant (e.g. {@link SqlBuilder#PSC}, {@link SqlBuilder#NSC})
 * is backed by a distinct {@code SqlDialect}. Instances are created with the generated {@code builder()}:</p>
 * <pre>{@code
 * SqlDialect dialect = SqlDialect.builder()
 *         .namingPolicy(NamingPolicy.SNAKE_CASE)
 *         .sqlPolicy(SQLPolicy.PARAMETERIZED_SQL)
 *         .build();
 * }</pre>
 *
 * <p>Any property left unset on the builder is {@code null}, and is resolved to a default when the
 * dialect is used to build SQL: a {@code null} naming policy defaults to {@link NamingPolicy#SNAKE_CASE},
 * a {@code null} SQL policy defaults to {@link SQLPolicy#RAW_SQL}, and a {@code null} identifier quote
 * defaults to {@link IdentifierQuote#DOUBLE_QUOTE}.</p>
 *
 * @see SqlBuilder.Dsl#forDialect(SqlDialect)
 */
@Builder
@Data
@AllArgsConstructor
@Accessors(fluent = true)
public class SqlDialect {

    /**
     * Identifies the target database product and version &mdash; for example {@code "MySQL 9.7"} or
     * {@code "PostgreSQL 18"}. Optional metadata describing the database the SQL is generated for;
     * may be {@code null}.
     */
    private String productVersion;

    /**
     * The naming policy used to translate entity property names into SQL column names
     * (e.g. {@link NamingPolicy#SNAKE_CASE} renders {@code firstName} as {@code first_name}).
     * When {@code null}, builders fall back to {@link NamingPolicy#SNAKE_CASE}.
     */
    private NamingPolicy namingPolicy;

    /**
     * The parameter style of the generated SQL: raw literals, positional {@code ?}, named {@code :name},
     * or iBATIS {@code #{name}}. When {@code null}, builders fall back to {@link SQLPolicy#RAW_SQL}.
     */
    private SQLPolicy sqlPolicy;

    /**
     * The quote character used when an identifier (such as a column name) has to be quoted in the
     * generated SQL. When {@code null}, builders fall back to {@link IdentifierQuote#DOUBLE_QUOTE}.
     */
    private IdentifierQuote identifierQuote;

    /**
     * The character used to quote identifiers (such as column names) in generated SQL.
     */
    public static enum IdentifierQuote {
        /** ANSI/standard SQL double quote ({@code "}); the effective default when no quote is specified. */
        DOUBLE_QUOTE,

        /** MySQL-style backtick ({@code `}). */
        BACKTICK;
    }
}
