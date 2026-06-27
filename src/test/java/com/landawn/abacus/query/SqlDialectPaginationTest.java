package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Dialect-aware SQL generation driven by {@link SqlDialect#productInfo()}: pagination syntax
 * ({@code LIMIT}/{@code OFFSET} vs {@code OFFSET ... ROWS} / {@code FETCH ... ROWS ONLY}) and the
 * identifier-quote fallback for MySQL/MariaDB.
 */
public class SqlDialectPaginationTest extends TestBase {

    private static Dsl dslFor(final String productName) {
        return Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                .productInfo(ProductInfo.of(productName))
                .build());
    }

    @Test
    public void testOracleLimit() {
        String sql = dslFor("Oracle").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        // Raw JDBC product names are matched as case-insensitive substrings.
        sql = dslFor("Oracle Database 19c Enterprise Edition").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testOracleLimitWithOffset() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testOracleOffsetThenLimit() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").offset(20).limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testOracleLimitThenOffsetThrows() {
        // OFFSET must precede FETCH on FETCH-style dialects, so limit() consumes the OFFSET slot.
        final SqlBuilder builder = dslFor("Oracle").select("*").from("users").limit(10);

        try {
            assertThrows(IllegalStateException.class, () -> builder.offset(20));
        } finally {
            builder.build();
        }
    }

    @Test
    public void testOracleLimitAndFetchRowsAreMutuallyExclusive() {
        final SqlBuilder limitFirst = dslFor("Oracle").select("*").from("users").limit(10);

        try {
            assertThrows(IllegalStateException.class, () -> limitFirst.fetchFirstRows(5));
        } finally {
            limitFirst.build();
        }

        final SqlBuilder fetchFirst = dslFor("Oracle").select("*").from("users").fetchFirstRows(5);

        try {
            assertThrows(IllegalStateException.class, () -> fetchFirst.limit(10));
        } finally {
            fetchFirst.build();
        }
    }

    @Test
    public void testDb2UsesFetchPagination() {
        String sql = dslFor("DB2").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("DB2/LINUXX8664").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testSqlServerLimit() {
        // SQL Server has no FETCH FIRST without OFFSET, so a lone limit() emits OFFSET 0 ROWS.
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testSqlServerLimitWithOffset() {
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testSqlServerOffsetThenLimitOmitsOffsetZero() {
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").offset(20).limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testMySqlAndMariaDbKeepLimitSyntax() {
        String sql = dslFor("MySQL").select("*").from("users").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);

        sql = dslFor("MariaDB").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testLimitStyleProductsKeepLimitSyntax() {
        for (final String product : new String[] { "PostgreSQL", "SQLite", "H2" }) {
            final String sql = dslFor(product).select("*").from("users").limit(10).build().query();
            assertEquals("SELECT * FROM users LIMIT 10", sql, product);
        }
    }

    @Test
    public void testUnrecognizedOrMissingProductInfoUsesDefaultSyntax() {
        String sql = dslFor("CockroachDB").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users LIMIT 10", sql);

        // Predefined DSL constants carry no product info and are unchanged.
        sql = Dsl.PSC.select("*").from("users").limit(10).offset(20).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);

        // Blank product name is treated the same as no product info.
        sql = dslFor(" ").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testFetchStyleStandaloneOffset() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").offset(20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS", sql);
    }

    @Test
    public void testLimitConditionUsesDialectPagination() {
        // The Limit condition path (appendLimit) delegates to limit(int) / limit(int, int).
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit(10)).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit(10, 20)).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testGenericExpressionLimitRerenderedOnFetchDialects() {
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("10")).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit("10 OFFSET 20")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        // Placeholder tokens are carried over into the FETCH syntax.
        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit("? OFFSET ?")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").append(Filters.limit(":count")).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST :count ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit("#{count} OFFSET #{offset}")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET #{offset} ROWS FETCH NEXT #{count} ROWS ONLY", sql);

        sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").append(Filters.limit("10")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        // LIMIT-style dialects keep the expression as-is.
        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("10 OFFSET 20")).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testProductSpecificExpressionLimitStaysVerbatim() {
        // A non-generic expression is deliberate product-specific syntax and is never re-rendered.
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("FETCH FIRST 5 ROWS ONLY")).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 5 ROWS ONLY", sql);

        // MySQL's comma form (offset, count) does not match the generic pattern.
        sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("20, 10")).build().query();
        assertEquals("SELECT * FROM users LIMIT 20, 10", sql);
    }

    @Test
    public void testMySqlProductInfoDefaultsToBacktickQuote() {
        final Dsl mysqlDsl = dslFor("MySQL");

        final String sql = mysqlDsl.selectFrom(QdpUser.class).build().query();
        assertEquals("SELECT id AS `id`, first_name AS `firstName` FROM qdp_user", sql);
    }

    @Test
    public void testExplicitIdentifierQuoteWinsOverProductInfo() {
        final Dsl mysqlDoubleQuoteDsl = Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                .identifierQuote(IdentifierQuote.DOUBLE_QUOTE)
                .productInfo(ProductInfo.of("MySQL"))
                .build());

        final String sql = mysqlDoubleQuoteDsl.selectFrom(QdpUser.class).build().query();
        assertEquals("SELECT id AS \"id\", first_name AS \"firstName\" FROM qdp_user", sql);
    }

    @Test
    public void testProductInfoFactories() {
        final ProductInfo nameOnly = ProductInfo.of("Oracle");
        assertEquals("Oracle", nameOnly.name());
        assertNull(nameOnly.version());

        final ProductInfo nameAndVersion = ProductInfo.of("Oracle", "19c");
        assertEquals("Oracle", nameAndVersion.name());
        assertEquals("19c", nameAndVersion.version());
    }

    public static class QdpUser {
        private long id;
        private String firstName;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }
}
