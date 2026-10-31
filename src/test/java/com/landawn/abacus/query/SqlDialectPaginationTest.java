package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.PSC;
import static com.landawn.abacus.query.QueryTestSupport.assertSql;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Per-product detection predicates on {@link ProductInfo} ({@code isMySQL()}, {@code isOracle()}, ...)
 * and their consistency with {@link AbstractQueryBuilder#resolveDialectFamily(ProductInfo)}.
 */
public class SqlDialectPaginationTest extends TestBase {

    @Test
    void testAbsentVersionIsNormalizedToEmptyAndConsistent() {
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
    void testControlOnlyVersionsAreNotComparable() {
        for (final String version : List.of("\u0000", "\u0001\u0002", " \u0000\t", "\u001b")) {
            final ProductInfo invalid = ProductInfo.of("PostgreSQL", version);
            assertFalse(invalid.isVersionAtLeast("1"));
            assertFalse(invalid.isVersionAtMost("1"));

            final ProductInfo valid = ProductInfo.of("PostgreSQL", "1");
            assertFalse(valid.isVersionAtLeast(version));
            assertFalse(valid.isVersionAtMost(version));
        }
    }

    @Test
    void testDb2UsesFetchPagination() {
        String sql = dslFor("DB2").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("DB2/LINUXX8664").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testDialectToBuilderAndProductNameValidation() {
        SqlDialect base = SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).build();
        SqlDialect variant = base.toBuilder().identifierQuote(SqlDialect.IdentifierQuote.BACKTICK).build();
        assertEquals(NamingPolicy.SNAKE_CASE, variant.namingPolicy());
        assertEquals(SqlDialect.IdentifierQuote.BACKTICK, variant.identifierQuote());

        assertThrows(IllegalArgumentException.class, () -> new ProductInfo(null, "1"));
        assertThrows(IllegalArgumentException.class, () -> ProductInfo.of(" "));
    }

    @Test
    void testExplicitIdentifierQuoteWinsOverProductInfo() {
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
    void testFetchStyleStandaloneOffset() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").offset(20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS", sql);
    }

    @Test
    void testFetchSyntaxRetainsLimitsLargerThanIntegerRange() {
        for (final String product : List.of("Oracle", "DB2", "Microsoft SQL Server")) {
            final Dsl dsl = dslFor(product);
            final String first = product.equals("Microsoft SQL Server") ? "OFFSET 0 ROWS FETCH NEXT" : "FETCH FIRST";
            assertSql("SELECT id FROM users ORDER BY id " + first + " 2147483648 ROWS ONLY", List.of(),
                    dsl.select("id").from("users").orderBy("id").append(new Limit("2147483648")).build());

            for (final String expression : List.of("2147483648 OFFSET 4294967296", "4294967296, 2147483648")) {
                assertSql("SELECT id FROM users ORDER BY id OFFSET 4294967296 ROWS FETCH NEXT 2147483648 ROWS ONLY", List.of(),
                        dsl.select("id").from("users").orderBy("id").append(new Limit(expression)).build());
            }
        }
    }

    @Test
    void testFetchSyntaxRetainsUnresolvedFetchExpressions() {
        for (final String product : List.of("Oracle", "DB2", "Microsoft SQL Server")) {
            final Dsl dsl = dslFor(product);
            final String first = product.equals("Microsoft SQL Server") ? "OFFSET 0 ROWS FETCH NEXT" : "FETCH FIRST";
            assertSql("SELECT id FROM users ORDER BY id " + first + " 2147483648 ROWS ONLY", List.of(),
                    dsl.select("id").from("users").orderBy("id").append(new Limit("FETCH FIRST 2147483648 ROWS ONLY")).build());
            assertSql("SELECT id FROM users ORDER BY id OFFSET 4294967296 ROWS FETCH NEXT 2147483648 ROWS ONLY", List.of(),
                    dsl.select("id").from("users").orderBy("id").append(new Limit("OFFSET 4294967296 ROWS FETCH NEXT 2147483648 ROWS ONLY")).build());
        }
    }

    @Test
    void testGenericExpressionLimitRerenderedOnFetchDialects() {
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit("10")).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit("10 OFFSET 20")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").append(Filters.limit("10")).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        // On LIMIT-style dialects the parsed count/offset render back to the same LIMIT/OFFSET text.
        sql = dslFor("MySQL").select("*").from("users").append(Filters.limit("10 OFFSET 20")).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    void testInvalidNamesAreRejectedAndUnknownProductReturnsFalse() {
        assertThrows(IllegalArgumentException.class, () -> ProductInfo.of(null));
        assertThrows(IllegalArgumentException.class, () -> ProductInfo.of(""));
        assertThrows(IllegalArgumentException.class, () -> ProductInfo.of(" "));

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
    void testLimitConditionUsesDialectPagination() {
        // The Limit condition path (appendLimit) delegates to limit(int) / limit(int, int).
        String sql = dslFor("Oracle").select("*").from("users").append(Filters.limit(10)).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        sql = dslFor("Oracle").select("*").from("users").orderBy("id").append(Filters.limit(10, 20)).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testLimitStyleProductsKeepLimitSyntax() {
        for (final String product : new String[] { "PostgreSQL", "SQLite", "H2" }) {
            final String sql = dslFor(product).select("*").from("users").limit(10).build().query();
            assertEquals("SELECT * FROM users LIMIT 10", sql, product);
        }
    }

    @Test
    void testMySqlAndMariaDbAreDistinctProducts() {
        final ProductInfo mysql = ProductInfo.of("MySQL");
        assertTrue(mysql.isMySQL());
        assertFalse(mysql.isMariaDB());

        final ProductInfo mariadb = ProductInfo.of("MariaDB");
        assertTrue(mariadb.isMariaDB());
        assertFalse(mariadb.isMySQL());
    }

    @Test
    void testMySqlAndMariaDbKeepLimitSyntax() {
        String sql = dslFor("MySQL").select("*").from("users").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);

        sql = dslFor("MariaDB").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    void testMySqlProductInfoDefaultsToBacktickQuote() {
        final Dsl mysqlDsl = dslFor("MySQL");

        final String sql = mysqlDsl.selectFrom(QdpUser.class).build().query();
        assertEquals("SELECT id AS `id`, first_name AS `firstName` FROM qdp_user", sql);
    }

    @Test
    void testOracleLimit() {
        String sql = dslFor("Oracle").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);

        // Raw JDBC product names are matched as case-insensitive substrings.
        sql = dslFor("Oracle Database 19c Enterprise Edition").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    void testOracleLimitAndFetchRowsAreMutuallyExclusive() {
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
    void testOracleLimitThenOffsetThrows() {
        // OFFSET must precede FETCH on FETCH-style dialects, so limit() consumes the OFFSET slot.
        final SqlBuilder builder = dslFor("Oracle").select("*").from("users").limit(10);

        try {
            assertThrows(IllegalStateException.class, () -> builder.offset(20));
        } finally {
            builder.build();
        }
    }

    @Test
    void testOracleLimitWithOffset() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testOracleOffsetThenLimit() {
        final String sql = dslFor("Oracle").select("*").from("users").orderBy("id").offset(20).limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    void testParsedExpressionLimitRerenderedPerDialect() {
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
    void testPlaceholderExpressionLimitIsRejectedBeforeDialectRendering() {
        assertThrows(IllegalArgumentException.class, () -> Filters.limit("? OFFSET ?"));
        assertThrows(IllegalArgumentException.class, () -> Filters.limit(":count"));
        assertThrows(IllegalArgumentException.class, () -> Filters.limit("#{count}"));
    }

    @Test
    void testPredicatesAreCaseInsensitiveSubstringMatches() {
        // Raw DatabaseMetaData.getDatabaseProductName() values are recognized.
        assertTrue(ProductInfo.of("Oracle Database 19c").isOracle());
        assertTrue(ProductInfo.of("Microsoft SQL Server").isSQLServer());
        assertTrue(ProductInfo.of("sqlserver").isSQLServer());
        assertTrue(ProductInfo.of("MYSQL").isMySQL());
        assertTrue(ProductInfo.of("PostgreSQL 16.1").isPostgreSQL());
    }

    @Test
    void testPredicatesMatchTheirProduct() {
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
    void testProductInfoFactories() {
        final ProductInfo nameOnly = ProductInfo.of("Oracle");
        assertEquals("Oracle", nameOnly.name());
        assertEquals("", nameOnly.version()); // absent version is normalized to "" (never null)

        final ProductInfo nameAndVersion = ProductInfo.of("Oracle", "19c");
        assertEquals("Oracle", nameAndVersion.name());
        assertEquals("19c", nameAndVersion.version());
    }

    @Test
    void testResolvedFetchLimitAfterOffsetRowsOnLimitStyleDialect() {
        final Dsl postgres = dslFor("PostgreSQL");
        assertEquals("SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY",
                postgres.select("*").from("users").offsetRows(5).fetchNextRows(10).build().query());
        assertEquals("SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 99999999999 ROWS ONLY",
                postgres.select("*").from("users").offsetRows(5).append(new Limit("FETCH NEXT 99999999999 ROWS ONLY")).build().query());
        assertEquals("SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY",
                postgres.select("*").from("users").offsetRows(5).append(new Limit("FETCH NEXT 10 ROWS ONLY")).build().query());
        assertEquals("SELECT * FROM users OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY",
                postgres.select("*").from("users").offsetRows(5).append(Criteria.builder().limit("FETCH NEXT 10 ROWS ONLY").build()).build().query());
        assertEquals("SELECT * FROM users LIMIT 10", postgres.select("*").from("users").append(new Limit("FETCH NEXT 10 ROWS ONLY")).build().query());
        assertThrows(IllegalStateException.class, () -> postgres.select("*").from("users").offset(5).append(new Limit("FETCH NEXT 10 ROWS ONLY")));
    }

    @Test
    void testResolveDialectFamilyStaysConsistentWithPredicates() {
        assertEquals(AbstractQueryBuilder.DialectFamily.ORACLE, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("Oracle Database 19c")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DB2, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("DB2/LINUXX8664")));
        assertEquals(AbstractQueryBuilder.DialectFamily.SQL_SERVER, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("Microsoft SQL Server")));
        assertEquals(AbstractQueryBuilder.DialectFamily.MYSQL, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("MySQL")));
        assertEquals(AbstractQueryBuilder.DialectFamily.MYSQL, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("MariaDB")));
        assertEquals(AbstractQueryBuilder.DialectFamily.LIMIT_STYLE, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("PostgreSQL")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DEFAULT, AbstractQueryBuilder.resolveDialectFamily(ProductInfo.of("FancyNewDB")));
        assertEquals(AbstractQueryBuilder.DialectFamily.DEFAULT, AbstractQueryBuilder.resolveDialectFamily(null));
    }

    @Test
    void testSqlServerAppendCriteriaOrderByWithLimit() {
        // The criteria's own ORDER BY renders before its LIMIT, so it satisfies SQL Server's
        // OFFSET/FETCH ORDER BY prerequisite during the side-effect-free pre-validation.
        String sql = dslFor("Microsoft SQL Server").select("id").from("users").append(Criteria.builder().orderBy("id").limit(10).build()).build().query();
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        sql = dslFor("Microsoft SQL Server").select("id").from("users").append(Criteria.builder().orderBy("id").limit(10, 20).build()).build().query();
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);

        // A criteria LIMIT without any ORDER BY (neither on the builder nor in the criteria) is still rejected.
        final SqlBuilder noOrderBy = dslFor("Microsoft SQL Server").select("id").from("users");
        assertThrows(IllegalStateException.class, () -> noOrderBy.append(Criteria.builder().limit(10).build()));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", noOrderBy.orderBy("id").limit(10).build().query());
    }

    @Test
    void testSqlServerLimit() {
        // SQL Server has no FETCH FIRST without OFFSET, so a lone limit() emits OFFSET 0 ROWS.
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testSqlServerLimitWithOffset() {
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").limit(10, 20).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testSqlServerOffsetThenLimitOmitsOffsetZero() {
        final String sql = dslFor("Microsoft SQL Server").select("*").from("users").orderBy("id").offset(20).limit(10).build().query();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    void testUnrecognizedOrMissingProductInfoUsesDefaultSyntax() {
        String sql = dslFor("CockroachDB").select("*").from("users").limit(10).build().query();
        assertEquals("SELECT * FROM users LIMIT 10", sql);

        // Predefined DSL constants carry no product info and are unchanged.
        sql = PSC.select("*").from("users").limit(10).offset(20).build().query();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);

        assertThrows(IllegalArgumentException.class, () -> dslFor(" "));
    }

    @Test
    void testExplicitFetchRejectedOnUpdateAndDeleteWithoutChangingBuilder() {
        for (final String product : List.of("MySQL", "MariaDB", "SQLite")) {
            final Dsl dsl = dslFor(product);
            final SqlBuilder delete = dsl.deleteFrom("t");
            assertThrows(IllegalStateException.class, () -> delete.fetchFirstRows(5), product);
            assertEquals("DELETE FROM t LIMIT 5", delete.limit(5).build().query(), product);

            final SqlBuilder update = dsl.update("t").set("a");
            assertThrows(IllegalStateException.class, () -> update.fetchFirstRows(5), product);
            assertEquals("UPDATE t SET a = ? LIMIT 5", update.limit(5).build().query(), product);
        }
    }

    @Test
    void testResolvedFetchAndLargeNativeLimitsOnUpdateAndDelete() {
        for (final String product : List.of("MySQL", "MariaDB", "SQLite")) {
            final Dsl dsl = dslFor(product);
            // These controls distinguish explicit FETCH from a Limit expression that should convert to LIMIT.
            for (final String count : List.of("10", "2147483647")) {
                assertSql("UPDATE t SET a = ? LIMIT " + count, List.of(1),
                        dsl.update("t").set("a", 1).append(new Limit("FETCH FIRST " + count + " ROWS ONLY")).build());
                assertSql("DELETE FROM t LIMIT " + count, List.of(), dsl.deleteFrom("t").append(new Limit("FETCH NEXT " + count + " ROWS ONLY")).build());
            }
            assertSql("UPDATE t SET a = ? LIMIT 99999999999", List.of(1), dsl.update("t").set("a", 1).append(new Limit("LIMIT 99999999999")).build());
            assertSql("DELETE FROM t LIMIT 99999999999", List.of(), dsl.deleteFrom("t").append(new Limit("LIMIT 99999999999")).build());
        }
    }

    // Intended behavior for KNOWN OPEN ISSUES #14: crossing the int boundary must not change FETCH-to-LIMIT conversion.
    // Keep the desired SQL here instead of treating the current rejection as the contract.
    @Disabled("Known open issue #14 in AbstractQueryBuilder: large FETCH limits are rejected before conversion on UPDATE/DELETE")
    @ParameterizedTest
    @CsvSource({ "MySQL, 2147483648", "MySQL, 99999999999", "MariaDB, 2147483648", "MariaDB, 99999999999", "SQLite, 2147483648", "SQLite, 99999999999" })
    void testKnownOpenIssueLargeFetchLimitOnUpdateAndDelete(final String product, final String count) {
        final Dsl dsl = dslFor(product);
        final String first = "FETCH FIRST " + count + " ROWS ONLY";
        final String next = "FETCH NEXT " + count + " ROWS ONLY";

        assertAll(product + ": " + count,
                () -> assertSql("UPDATE t SET a = ? LIMIT " + count, List.of(1), dsl.update("t").set("a", 1).append(new Limit(first)).build()),
                () -> assertSql("DELETE FROM t LIMIT " + count, List.of(), dsl.deleteFrom("t").append(new Limit(next)).build()),
                () -> assertSql("UPDATE t SET a = ? WHERE b = ? LIMIT " + count, List.of(1, 2),
                        dsl.update("t").set("a", 1).append(Criteria.builder().where(Filters.eq("b", 2)).limit(next).build()).build()),
                () -> assertSql("DELETE FROM t WHERE b = ? LIMIT " + count, List.of(2),
                        dsl.deleteFrom("t").append(Criteria.builder().where(Filters.eq("b", 2)).limit(first).build()).build()));
    }

    // Regression: an out-of-int-range FETCH-form Limit was emitted verbatim on MySQL and SQLite, which have no FETCH clause
    // (a resolved one, and the comma form, were already rendered as LIMIT there).
    @Test
    void testUnresolvedFetchLimitRendersLimitSyntaxOnMySqlAndSqlite() {
        for (final String product : List.of("MySQL", "MariaDB", "SQLite")) {
            final Dsl dsl = dslFor(product);
            assertEquals("SELECT * FROM t LIMIT 99999999999", dsl.select("*").from("t").append(new Limit("FETCH FIRST 99999999999 ROWS ONLY")).build().query(),
                    product);
            assertEquals("SELECT * FROM t LIMIT 99999999999 OFFSET 5",
                    dsl.select("*").from("t").append(new Limit("OFFSET 5 ROWS FETCH NEXT 99999999999 ROWS ONLY")).build().query(), product);
            assertEquals("SELECT * FROM t LIMIT 10", dsl.select("*").from("t").append(new Limit("FETCH FIRST 10 ROWS ONLY")).build().query(), product);
        }

        // PostgreSQL supports the SQL-standard FETCH clause, so the expression is still emitted as written there.
        assertEquals("SELECT * FROM t FETCH FIRST 99999999999 ROWS ONLY",
                dslFor("PostgreSQL").select("*").from("t").append(new Limit("FETCH FIRST 99999999999 ROWS ONLY")).build().query());
    }

    @Test
    void testVersionAtLeastAndAtMostAreInclusive() {
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
    void testVersionComparisonPadsMissingComponentsWithZero() {
        final ProductInfo v = ProductInfo.of("MySQL", "8.0");
        assertTrue(v.isVersionAtLeast("8.0.0")); // 8.0 == 8.0.0
        assertTrue(v.isVersionAtMost("8.0.0"));
        assertFalse(v.isVersionAtLeast("8.0.1"));
    }

    @Test
    void testVersionComparisonParsesLeadingNumericRun() {
        final ProductInfo oracle = ProductInfo.of("Oracle", "19c");
        assertTrue(oracle.isVersionAtLeast("18"));
        assertTrue(oracle.isVersionAtLeast("19"));
        assertFalse(oracle.isVersionAtLeast("21"));
        assertTrue(oracle.isVersionAtMost("21"));
    }

    @Test
    void testVersionComparisonReturnsFalseWhenNotComparable() {
        assertFalse(ProductInfo.of("MySQL").isVersionAtLeast("8.0")); // absent version ("") -> not comparable
        assertFalse(ProductInfo.of("MySQL", "unknown").isVersionAtLeast("8")); // not leading-numeric
        assertFalse(ProductInfo.of("MySQL", "8.").isVersionAtLeast("8")); // empty trailing component
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtLeast(null)); // null argument
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtLeast("abc")); // unparseable argument
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtLeast("8.")); // malformed comparison version
        assertFalse(ProductInfo.of("MySQL", "8.0").isVersionAtMost(null));
    }

    private static Dsl dslFor(final String productName) {
        return Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                .productInfo(ProductInfo.of(productName))
                .build());
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
