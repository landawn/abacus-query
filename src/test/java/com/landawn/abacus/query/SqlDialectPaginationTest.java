package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Per-product detection predicates on {@link ProductInfo} ({@code isMySQL()}, {@code isOracle()}, ...)
 * and their consistency with {@link AbstractQueryBuilder#resolveDialectFamily(ProductInfo)}.
 */
@Tag("2025")
public class SqlDialectPaginationTest extends TestBase {
    @Test
    public void testPredicatesMatchTheirProduct() {
        assertTrue(ProductInfo.of("Oracle").isOracle());
        assertTrue(ProductInfo.of("DB2").isDB2());
        assertTrue(ProductInfo.of("SQL Server").isSQLServer());
        assertTrue(ProductInfo.of("MySQL").isMySQL());
        assertTrue(ProductInfo.of("MariaDB").isMariaDB());
        assertTrue(ProductInfo.of("PostgreSQL").isPostgreSQL());
        assertTrue(ProductInfo.of("SQLite").isSQLite());
        assertTrue(ProductInfo.of("H2").isH2());
    }

    @Test
    public void testPredicatesAreCaseInsensitiveSubstringMatches() {
        // Raw DatabaseMetaData.getDatabaseProductName() values are recognized.
        assertTrue(ProductInfo.of("Oracle Database 19c").isOracle());
        assertTrue(ProductInfo.of("Microsoft SQL Server").isSQLServer());
        assertTrue(ProductInfo.of("sqlserver").isSQLServer());
        assertTrue(ProductInfo.of("MYSQL").isMySQL());
        assertTrue(ProductInfo.of("PostgreSQL 16.1").isPostgreSQL());
    }

    @Test
    public void testMySqlAndMariaDbAreDistinctProducts() {
        final ProductInfo mysql = ProductInfo.of("MySQL");
        assertTrue(mysql.isMySQL());
        assertFalse(mysql.isMariaDB());

        final ProductInfo mariadb = ProductInfo.of("MariaDB");
        assertTrue(mariadb.isMariaDB());
        assertFalse(mariadb.isMySQL());
    }

    @Test
    public void testNullNameAndUnknownProductReturnFalse() {
        final ProductInfo nullName = ProductInfo.of(null);
        assertFalse(nullName.isOracle());
        assertFalse(nullName.isMySQL());
        assertFalse(nullName.isSQLServer());

        final ProductInfo unknown = ProductInfo.of("FancyNewDB");
        assertFalse(unknown.isOracle());
        assertFalse(unknown.isDB2());
        assertFalse(unknown.isSQLServer());
        assertFalse(unknown.isMySQL());
        assertFalse(unknown.isMariaDB());
        assertFalse(unknown.isPostgreSQL());
        assertFalse(unknown.isSQLite());
        assertFalse(unknown.isH2());
    }

    @Test
    public void testVersionAtLeastAndAtMostAreInclusive() {
        final ProductInfo mysql = ProductInfo.of("MySQL", "8.0.32");
        assertTrue(mysql.isVersionAtLeast("8.0"));
        assertTrue(mysql.isVersionAtLeast("8.0.32")); // equal -> inclusive
        assertFalse(mysql.isVersionAtLeast("8.1"));
        assertFalse(mysql.isVersionAtLeast("9"));

        assertTrue(mysql.isVersionAtMost("8.1"));
        assertTrue(mysql.isVersionAtMost("8.0.32")); // equal -> inclusive
        assertFalse(mysql.isVersionAtMost("8.0"));
    }

    @Test
    public void testVersionComparisonPadsMissingComponentsWithZero() {
        final ProductInfo v = ProductInfo.of("MySQL", "8.0");
        assertTrue(v.isVersionAtLeast("8.0.0")); // 8.0 == 8.0.0
        assertTrue(v.isVersionAtMost("8.0.0"));
        assertFalse(v.isVersionAtLeast("8.0.1"));
    }

    @Test
    public void testVersionComparisonParsesLeadingNumericRun() {
        final ProductInfo oracle = ProductInfo.of("Oracle", "19c");
        assertTrue(oracle.isVersionAtLeast("18"));
        assertTrue(oracle.isVersionAtLeast("19"));
        assertFalse(oracle.isVersionAtLeast("21"));
        assertTrue(oracle.isVersionAtMost("21"));
    }

    @Test
    public void testVersionComparisonReturnsFalseWhenNotComparable() {
        assertFalse(ProductInfo.of("MySQL").isVersionAtLeast("8.0")); // absent version ("") -> not comparable
        assertFalse(ProductInfo.of("MySQL", "unknown").isVersionAtLeast("8")); // not leading-numeric
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtLeast(null)); // null argument
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtLeast("abc")); // unparseable argument
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtMost(null));
    }

    @Test
    public void testAbsentVersionIsNormalizedToEmptyAndConsistent() {
        // Absent/null version normalizes to "" across all construction paths, so version() is never null
        // and equals/hashCode stay consistent regardless of how "no version" was expressed.
        assertEquals("", ProductInfo.of("Oracle").version());
        assertEquals("", ProductInfo.of("Oracle", null).version());
        assertEquals("", new ProductInfo("Oracle", null).version());

        assertEquals(ProductInfo.of("Oracle"), ProductInfo.of("Oracle", null));
        assertEquals(ProductInfo.of("Oracle"), new ProductInfo("Oracle", null));
        assertEquals(ProductInfo.of("Oracle").hashCode(), new ProductInfo("Oracle", null).hashCode());
    }

    @Test
    public void testResolveDialectFamilyStaysConsistentWithPredicates() {
        assertEquals(AbstractQueryBuilder.DialectFamily.ORACLE, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("Oracle Database 19c")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DB2, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("DB2/LINUXX8664")));
        assertEquals(AbstractQueryBuilder.DialectFamily.SQL_SERVER, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("Microsoft SQL Server")));
        assertEquals(AbstractQueryBuilder.DialectFamily.MYSQL, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("MySQL")));
        assertEquals(AbstractQueryBuilder.DialectFamily.MYSQL, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("MariaDB")));
        assertEquals(AbstractQueryBuilder.DialectFamily.LIMIT_STYLE, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("PostgreSQL")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DEFAULT, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("FancyNewDB")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DEFAULT, AbstractQueryBuilder.resolveDialectFamily(null));
    }

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

        // On LIMIT-style dialects the parsed count/offset render back to the same LIMIT/OFFSET text.
        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("10 OFFSET 20")).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOpaqueExpressionLimitStaysVerbatim() {
        // A truly opaque expression — here a placeholder form on a LIMIT-style dialect — is not parsed
        // into count/offset and is emitted verbatim.
        String sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("? OFFSET ?")).build().query();
        assertEquals("SELECT * FROM users LIMIT ? OFFSET ?", sql);
    }

    @Test
    public void testParsedExpressionLimitRerenderedPerDialect() {
        // MySQL's comma form (offset, count) is now parsed and re-rendered in the target dialect's syntax.
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("20, 10")).build().query();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("20, 10")).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);

        // A SQL:2008 OFFSET/FETCH form is parsed too: Oracle renders it equivalently, MySQL as LIMIT/OFFSET.
        sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY")).build().query();
        assertEquals("SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", sql);

        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY")).build().query();
        assertEquals("SELECT * FROM users LIMIT 20 OFFSET 5", sql);

        // FETCH FIRST count ROWS ONLY (no offset).
        sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("FETCH FIRST 5 ROWS ONLY")).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 5 ROWS ONLY", sql);

        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("FETCH FIRST 5 ROWS ONLY")).build().query();
        assertEquals("SELECT * FROM users LIMIT 5", sql);
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
        assertEquals("", nameOnly.version()); // absent version is normalized to "" (never null)

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
