package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
public class AbstractQueryBuilderTest extends TestBase {

    @Test
    public void testConstants() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.TOP);
        assertNotNull(AbstractQueryBuilder.UNIQUE);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.DISTINCTROW);
        assertNotNull(AbstractQueryBuilder.ASTERISK);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    public void testPSCSelectFrom() {
        String sql = Dsl.PSC.select("id", "firstName", "lastName").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testToSql() {
        String sql = Dsl.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testBuild() {
        AbstractQueryBuilder.SP sqlPair = Dsl.PSC.select("id").from(Account.class).where(Filters.eq("id", 1)).build();
        assertNotNull(sqlPair);
        assertTrue(sqlPair.query().contains("WHERE"));
        assertEquals(1, sqlPair.parameters().size());
    }

    @Test
    public void testPSCWithWhere() {
        String sql = Dsl.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testPSCWithMultipleConditions() {
        String sql = Dsl.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testPSCWithOrderBy() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testPSCWithLimit() {
        String sql = Dsl.PSC.select("*").from(Account.class).limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testPSCWithJoin() {
        String sql = Dsl.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testPSCWithLeftJoin() {
        String sql = Dsl.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testPSCWithInnerJoin() {
        String sql = Dsl.PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testPSCWithRightJoin() {
        String sql = Dsl.PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testPSCWithFullJoin() {
        String sql = Dsl.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testPSCWithCrossJoin() {
        String sql = Dsl.PSC.select("*").from("users").crossJoin("roles").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testPSCWithGroupBy() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testPSCWithHaving() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testPSCWithDistinct() {
        String sql = Dsl.PSC.select("status").from(Account.class).distinct().build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testPSCComplexQuery() {
        String sql = Dsl.PSC.select("u.id", "u.firstName", "COUNT(o.id) as order_count")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.eq("u.status", "active"))
                .groupBy("u.id", "u.firstName")
                .having(Filters.expr("COUNT(o.id) > 0"))
                .orderBy("order_count", SortDirection.DESC)
                .limit(10)
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("HAVING"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testInsertInto() {
        String sql = Dsl.PSC.insertInto(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdate() {
        String sql = Dsl.PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = Dsl.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = Dsl.PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AS"));
    }

    @Test
    public void testSelectWithMultipleTables() {
        String sql = Dsl.PSC.select("*").from("users", "orders").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testWhereWithOr() {
        String sql = Dsl.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleOrderBy() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("lastName", "firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByRejectsCommentToken() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id--").build().query());
    }

    @Test
    public void testCommentTokenAfterBackslashTerminatedQuotedIdentifierIsRejected() {
        // A backslash before the closing quote of a double-quoted or backtick-quoted identifier does NOT escape
        // that quote (backslash escaping applies only inside single-quoted string literals), so the closing quote
        // terminates the identifier and the trailing "--" is a real SQL comment token that must be rejected.
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("\"a\\\" -- x"));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("`a\\` -- x"));

        // A comment-like token that is genuinely inside a quoted identifier is still allowed.
        final String sql = Dsl.PSC.select("*").from("users").join("orders").using("\"a--b\"").build().query();
        assertTrue(sql.contains("a--b"));
    }

    @Test
    public void testCommentTokenAfterBackslashEscapedQuoteInsideSingleQuotedLiteralIsRejected() {
        // Inside a single-quoted string literal a backslash DOES escape the following quote (\'), so the next
        // quote closes the string and a trailing comment token (-- or /* */) must be rejected.
        // Regression: the scanner previously misread "\''" as a doubled-quote ('') escape and stayed "inside"
        // the string, hiding the trailing comment token from the guard.
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("note = '\\'' -- x").from("docs").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("note = '\\'' /* x */").from("docs").build().query());

        // The backslash-escaped-quote literal on its own (no trailing comment) is still accepted.
        final String okSql = Dsl.PSC.select("note = '\\''").from("docs").build().query();
        assertNotNull(okSql);
    }

    @Test
    public void testOrderByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testOrderByAsc() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = Dsl.PSC.select("*").from(Account.class).limit(20, 10).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testFromWithEntityClass() {
        String sql = Dsl.PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testJoinWithEntityClass() {
        String sql = Dsl.PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testIntoWithTableName() {
        String sql = Dsl.PSC.insert("id", "name").into("accounts").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testIntoRejectsEmptyTableName() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("id").into("").build().query());
    }

    @Test
    public void testUpdateWithSet() {
        String sql = Dsl.PSC.update("accounts").set("status", "inactive").set("updated_at", "NOW()").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFromWithTable() {
        String sql = Dsl.PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectCount() {
        String sql = Dsl.PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    public void testSelectAll() {
        String sql = Dsl.PSC.select(AbstractQueryBuilder.ALL).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testChainedAndOr() {
        String sql = Dsl.PSC.select("*")
                .from(Account.class)
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).or(Filters.eq("role", "admin")))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleJoins() {
        String sql = Dsl.PSC.select("*")
                .from("users u")
                .innerJoin("orders o")
                .on("u.id = o.user_id")
                .leftJoin("products p")
                .on("o.product_id = p.id")
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testLimitWithOffsetRejectsSecondOffset() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").limit(10, 5).offset(2));
    }

    @Test
    public void testAppendLimitConditionWithExpression() {
        String sql = Dsl.PSC.select("*").from("users").append(new Limit("10 OFFSET 20")).build().query();
        assertTrue(sql.endsWith("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testAppendConditionAfterWhereThrowsDuplicateWhere() {
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.eq("name", "Alice")).build().query());
    }

    @Test
    public void testAppendWhereClauseAfterWhereThrows() {
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.where(Filters.eq("name", "Alice"))).build().query());
    }

    @Test
    public void testAppendCriteriaAfterWhereThrowsWhenCriteriaHasWhere() {
        Criteria criteria = Criteria.builder().where(Filters.eq("name", "Alice")).build();

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(criteria).build().query());
    }

    @Test
    public void testAppendLimitExpressionAfterLimitThrows() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").limit(10).append(new Limit("5")).build().query());
    }

    @Test
    public void testSelectAllowsHashJsonOperators() {
        String sql = Dsl.PSC.select("payload #>> '{meta,status}'").from("docs").build().query();
        assertTrue(sql.contains("#>>"));
    }

    @Test
    public void testSelectAllowsCommentLikeTokenInsideQuotedLiteral() {
        String sql = Dsl.PSC.select("CASE WHEN note = '--literal' THEN 1 ELSE 0 END").from("docs").build().query();
        assertTrue(sql.contains("'--literal'"));
    }

    @Test
    public void testUpdateAllowsIbatisPlaceholderExpression() {
        String sql = Dsl.PSC.update("users").set("name = #{name}").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("#{name}"));
    }

    @Test
    public void testGroupByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testWhereRejectsNullStringExpression() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where((String) null));
    }

    @Test
    public void testClauseBuildersRejectBlankStringFragments() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").on("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id").having("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").append("   "));
    }

    @Test
    public void testInsertAndUpdateRejectBlankTableAndSetFragments() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("id").into("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Arrays.asList("name", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Collections.emptyMap()));
    }

    @Test
    public void testSetOperationsAndDirectionsRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").unionAll("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").intersect("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").except("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").minus("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id", null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id", null));
    }

    @Test
    public void testFetchNextRowsAndFetchFirstRowsAreMutuallyExclusive() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id").fetchNextRows(10).fetchFirstRows(5).build().query());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).fetchNextRows(5).build().query());
    }

    @Test
    public void testRowLimitApisRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").limit(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").limit(10, -1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").offset(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").offsetRows(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").fetchNextRows(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").fetchFirstRows(-1));
    }

    @Test
    public void testChainedSetCollectionCallsIncludeComma() {
        String sql = Dsl.PSC.update("users").set("firstName").set("lastName").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("first_name = ?"), "first_name assignment missing: " + sql);
        assertTrue(sql.contains("last_name = ?"), "last_name assignment missing: " + sql);
        int firstIdx = sql.indexOf("first_name");
        int commaIdx = sql.indexOf(',', firstIdx);
        int secondIdx = sql.indexOf("last_name");
        assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate the two SET assignments: " + sql);
    }

    @Test
    public void testChainedSetMapCallsIncludeComma() {
        java.util.Map<String, Object> m1 = java.util.Collections.singletonMap("firstName", "John");
        java.util.Map<String, Object> m2 = java.util.Collections.singletonMap("lastName", "Doe");
        String sql = Dsl.PSC.update("users").set(m1).set(m2).where(Filters.eq("id", 1)).build().query();
        int firstIdx = sql.indexOf("first_name");
        int commaIdx = sql.indexOf(',', firstIdx);
        int secondIdx = sql.indexOf("last_name");
        assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate map-based SET assignments: " + sql);
    }

    @Test
    public void testIsDefaultIdPropValueFractionalNumberNotTreatedAsZero() {
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(null));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0L));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(java.math.BigDecimal.ZERO));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0f));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.9")),
                "BigDecimal 0.9 has longValue()=0 but must not be treated as default ID");
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.1")));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.5), "double 0.5 must not be treated as default ID");
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.1f), "float 0.1 must not be treated as default ID");
    }

    @Test
    public void testDoubleHashNotTreatedAsSqlCommentInExpressions() {
        // ## is a whitelisted two-char token; the second # must not be re-examined as lone #
        String sql = Dsl.PSC.select("*").from("users").where(Filters.expr("status = '##ACTIVE##'")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("##ACTIVE##"), "## inside value must not be rejected as SQL comment");
    }
}

class AbstractQueryBuilder2026Test extends TestBase {

    @Test
    public void testSetHandlerForNamedParameter() {
        AbstractQueryBuilder.setHandlerForNamedParameter((sb, propName) -> sb.append("#{").append(propName).append("}"));

        try {
            final String sql = Dsl.NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("#{id}"));
        } finally {
            AbstractQueryBuilder.resetHandlerForNamedParameter();
        }
    }

    @Test
    public void testResetHandlerForNamedParameter() {
        AbstractQueryBuilder.setHandlerForNamedParameter((sb, propName) -> sb.append("#{").append(propName).append("}"));
        AbstractQueryBuilder.resetHandlerForNamedParameter();

        final String sql = Dsl.NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains(":id"));
    }

    @Test
    public void testSelectModifier() {
        final String sql = Dsl.PSC.select("*").selectModifier("TOP 5").from("users").build().query();

        assertTrue(sql.contains("SELECT TOP 5"));
    }

    @Test
    public void testNaturalJoin_String() {
        final String sql = Dsl.PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertTrue(sql.contains("NATURAL JOIN orders"));
    }

    @Test
    public void testNaturalJoin_EntityClass() {
        final String sql = Dsl.PSC.select("*").from(Account.class).naturalJoin(Account.class).build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.toLowerCase().contains("account"));
    }

    @Test
    public void testNaturalJoin_EntityClassAlias() {
        final String sql = Dsl.PSC.select("*").from(Account.class, "a").naturalJoin(Account.class, "b").build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.contains(" b"));
    }

    @Test
    public void testUsing() {
        final String sql = Dsl.PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testOffsetRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).build().query();

        assertTrue(sql.contains("OFFSET 20 ROWS"));
    }

    @Test
    public void testFetchNextRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testFetchFirstRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testAppendIf_Condition() {
        final String withCondition = Dsl.PSC.select("*").from("users").appendIf(true, Filters.eq("status", "ACTIVE")).build().query();
        final String withoutCondition = Dsl.PSC.select("*").from("users").appendIf(false, Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(withCondition.contains("status"));
        assertTrue(!withoutCondition.contains("status"));
    }

    @Test
    public void testAppendIf_String() {
        final String withExpression = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(true, " FOR UPDATE").build().query();
        final String withoutExpression = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(false, " FOR UPDATE").build().query();

        assertTrue(withExpression.contains("FOR UPDATE"));
        assertTrue(!withoutExpression.contains("FOR UPDATE"));
    }

    @Test
    public void testAppendIf_Consumer() {
        final String sql = Dsl.PSC.select("*")
                .from("users")
                .appendIf(true, builder -> builder.where(Filters.eq("status", "ACTIVE")).orderBy("name"))
                .build()
                .query();

        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testAppendIfOrElse_Condition() {
        final AbstractQueryBuilder.SP trueBranch = Dsl.PSC.select("*")
                .from("users")
                .appendIfOrElse(true, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();
        final AbstractQueryBuilder.SP falseBranch = Dsl.PSC.select("*")
                .from("users")
                .appendIfOrElse(false, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();

        assertTrue(trueBranch.query().contains("WHERE"));
        assertTrue(falseBranch.query().contains("WHERE"));
        assertEquals(Arrays.asList("ACTIVE"), trueBranch.parameters());
        assertEquals(Arrays.asList("INACTIVE"), falseBranch.parameters());
    }

    @Test
    public void testAppendIfOrElse_String() {
        final String asc = Dsl.PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        final String desc = Dsl.PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();

        assertTrue(asc.contains("ORDER BY name ASC"));
        assertTrue(desc.contains("ORDER BY name DESC"));
    }

    @Test
    public void testUnion_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .union(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnion_Query() {
        final String sql = Dsl.PSC.select("id").from("users").union("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnion_SingleNonSubQueryStringRejected() {
        // union(String) is reserved for a complete SELECT sub-query; a bare column name is rejected up front
        // instead of silently becoming a column list that fails later with an unrelated "from() must be called" error.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnion_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").union(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .unionAll(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION ALL"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnionAll_Query() {
        final String sql = Dsl.PSC.select("id").from("users").unionAll("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").unionAll("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnionAll_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").unionAll(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .intersect(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("INTERSECT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testIntersect_Query() {
        final String sql = Dsl.PSC.select("id").from("users").intersect("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").intersect("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testIntersect_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").intersect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .except(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("EXCEPT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testExcept_Query() {
        final String sql = Dsl.PSC.select("id").from("users").except("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").except("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testExcept_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").except(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .minus(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("MINUS"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testMinus_Query() {
        final String sql = Dsl.PSC.select("id").from("users").minus("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").minus("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testMinus_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").minus(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testForUpdate() {
        final String sql = Dsl.PSC.select("*").from("users").forUpdate().build().query();

        assertTrue(sql.contains("FOR UPDATE"));
    }

    @Test
    public void testForUpdate_idempotencyGuard() {
        // Calling forUpdate() twice must not produce "FOR UPDATE FOR UPDATE".
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").forUpdate().forUpdate());
    }

    @Test
    public void testSetEntity_equivalentToDeprecatedSet() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final String viaSetEntity = Dsl.PSC.update("account").setEntity(a).where(Filters.eq("id", 1)).build().query();
        final String viaSet = Dsl.PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

        assertEquals(viaSet, viaSetEntity);
        assertTrue(viaSetEntity.contains("first_name = ?"));
    }

    @Test
    public void testSetEntity_excludedPropNames() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final Set<String> excluded = java.util.Set.of("lastName");
        final String sql = Dsl.PSC.update("account").setEntity(a, excluded).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertFalse(sql.contains("last_name = ?"));
    }

    @Test
    public void testSetEntity_rejectsCollection() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").setEntity(Arrays.asList("firstName", "lastName")));
    }

    @Test
    public void testSetEntity_rejectsArray() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").setEntity(new String[] { "firstName", "lastName" }));
    }

    @Test
    public void testIntoClass_alwaysSetsEntityClass() {
        // into(Class) should map property names to columns (entity class always set), matching from(...).
        final String sql = Dsl.PSC.insert("firstName", "lastName").into(Account.class).build().query();

        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testUsing_varargs() {
        final String sql = Dsl.PSC.select("*").from("orders").join("order_items").using("order_id", "tenant_id").build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testUsing_collection() {
        final String sql = Dsl.PSC.select("*").from("orders").join("order_items").using(Arrays.asList("order_id", "tenant_id")).build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testOn_varargsComposite() {
        final String sql = Dsl.PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id", "u.tenant_id = o.tenant_id").build().query();

        assertTrue(sql.contains("ON u.id = o.user_id AND u.tenant_id = o.tenant_id"), sql);
    }

    @Test
    public void testGroupBy_mapIterationOrder() {
        final Map<String, SortDirection> groupings = new LinkedHashMap<>();
        groupings.put("category", SortDirection.ASC);
        groupings.put("brand", SortDirection.DESC);

        final String sql = Dsl.PSC.select("category", "brand").from("products").groupBy(groupings).build().query();

        assertTrue(sql.indexOf("category ASC") < sql.indexOf("brand DESC"), sql);
    }

    @Test
    public void testOrderBy_mapIterationOrder() {
        final Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("lastName", SortDirection.ASC);
        orders.put("firstName", SortDirection.DESC);

        final String sql = Dsl.PSC.select("*").from("users").orderBy(orders).build().query();

        assertTrue(sql.indexOf("ASC") < sql.indexOf("DESC"), sql);
    }

    @Test
    public void testApply_SPFunction() throws Exception {
        final List<Object> result = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .apply(sp -> Arrays.asList(sp.query(), sp.parameters().size()));

        assertTrue(result.get(0).toString().contains("WHERE"));
        assertEquals(1, result.get(1));
    }

    @Test
    public void testApply_SqlAndParams() throws Exception {
        final String result = Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).apply((sql, params) -> sql + " / " + params.size());

        assertTrue(result.contains("WHERE"));
        assertTrue(result.endsWith("/ 1"));
    }

    @Test
    public void testAccept_SPConsumer() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept(sp -> {
            sqlHolder[0] = sp.query();
            paramCount[0] = sp.parameters().size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    @Test
    public void testAccept_SqlAndParams() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept((sql, params) -> {
            sqlHolder[0] = sql;
            paramCount[0] = params.size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    // Cover select-into and entity-class join overloads that inject aliases directly.
    @Test
    public void testSelectIntoFromEntityClass() {
        final String sql = Dsl.PSC.select("id", "firstName").into("account_archive").from(Account.class).build().query();

        assertTrue(sql.startsWith("INSERT INTO account_archive"));
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account acc"));
    }

    @Test
    public void testEntityJoinOverloadsWithAlias() {
        final String innerJoinSql = Dsl.PSC.select("*").from(Account.class, "a").innerJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String leftJoinSql = Dsl.PSC.select("*").from(Account.class, "a").leftJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String rightJoinSql = Dsl.PSC.select("*").from(Account.class, "a").rightJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String fullJoinSql = Dsl.PSC.select("*").from(Account.class, "a").fullJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String crossJoinSql = Dsl.PSC.select("*").from(Account.class, "a").crossJoin(Account.class, "a2").build().query();

        assertTrue(innerJoinSql.contains("INNER JOIN account a2"));
        assertTrue(leftJoinSql.contains("LEFT JOIN account a2"));
        assertTrue(rightJoinSql.contains("RIGHT JOIN account a2"));
        assertTrue(fullJoinSql.contains("FULL JOIN account a2"));
        assertTrue(crossJoinSql.contains("CROSS JOIN account a2"));
    }

    @Test
    public void testOrderByRejectsBlockAndHashCommentTokens() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id/*comment*/").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id#comment").build().query());
    }

    @Test
    public void testPrintln() {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            Dsl.PSC.select("id").from("users").debugPrint();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("SELECT id FROM users"));
    }
}

/**
 * Simple unit tests for AbstractQueryBuilder functionality.
 * Tests basic constants and naming policy functionality.
 */
class SimpleAbstractQueryBuilderTest extends TestBase {

    @BeforeEach
    void setUp() {
        // No setup needed for constant testing
    }

    @Test
    void testPublicConstants() {
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("TOP", AbstractQueryBuilder.TOP);
        assertEquals("UNIQUE", AbstractQueryBuilder.UNIQUE);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("DISTINCTROW", AbstractQueryBuilder.DISTINCTROW);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testConstantsAreNotNull() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.TOP);
        assertNotNull(AbstractQueryBuilder.UNIQUE);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.DISTINCTROW);
        assertNotNull(AbstractQueryBuilder.ASTERISK);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testNamingPolicyEnum() {
        // Test that NamingPolicy enum values exist and are accessible
        assertNotNull(NamingPolicy.NO_CHANGE);
        assertNotNull(NamingPolicy.SNAKE_CASE);
        assertNotNull(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(NamingPolicy.CAMEL_CASE);
    }
}

class AbstractQueryBuilder2026BatchTest extends TestBase {

    @Test
    public void testSet_ObjectStringDelegatesToColumnSet() {
        String sql = Dsl.PSC.update("account").set((Object) "firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("SET"));
        assertTrue(sql.contains("first_name = ?"));
    }

    @Test
    public void testSet_ObjectMapHonorsExcludedProperties() {
        java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
        props.put("firstName", "John");
        props.put("lastName", "Doe");

        String sql = Dsl.PSC.update("account").set(props, Collections.singleton("lastName")).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertTrue(!sql.contains("last_name = ?"));
    }

    @Test
    public void testInsertEntity_SkipsZeroIdAndNullProperties() {
        Account account = new Account();
        account.setId(0);
        account.setFirstName("John");
        account.setLastName(null);

        String sql = Dsl.PSC.insert(account).into("account").build().query();

        assertTrue(sql.contains("first_name"));
        assertTrue(!sql.contains("last_name"));
        assertTrue(!sql.contains("id"));
    }

    @com.landawn.abacus.annotation.Table(name = "frac_tbl")
    public static class FractionalIdEntity {
        @com.landawn.abacus.annotation.Id
        private java.math.BigDecimal myKey;
        private String myName;

        public java.math.BigDecimal getMyKey() {
            return myKey;
        }

        public FractionalIdEntity setMyKey(java.math.BigDecimal myKey) {
            this.myKey = myKey;
            return this;
        }

        public String getMyName() {
            return myName;
        }

        public FractionalIdEntity setMyName(String myName) {
            this.myName = myName;
            return this;
        }
    }

    @com.landawn.abacus.annotation.Table(name = "composite_id_tbl")
    public static class CompositeIdEntity {
        @com.landawn.abacus.annotation.Id
        private long tenantId;

        @com.landawn.abacus.annotation.Id
        private long localId;

        private String name;

        public long getTenantId() {
            return tenantId;
        }

        public CompositeIdEntity setTenantId(long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public long getLocalId() {
            return localId;
        }

        public CompositeIdEntity setLocalId(long localId) {
            this.localId = localId;
            return this;
        }

        public String getName() {
            return name;
        }

        public CompositeIdEntity setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Test
    public void testFix_insertEntity_doesNotSkipFractionalBigDecimalIdAsZero() {
        // BigDecimal("0.5").longValue() == 0 (truncation), so the buggy check would
        // wrongly treat 0.5 as a default/unset ID and omit it from the INSERT.
        FractionalIdEntity entity = new FractionalIdEntity();
        entity.setMyKey(new java.math.BigDecimal("0.5"));
        entity.setMyName("Alice");

        String sql = Dsl.PSC.insert(entity).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal id 0.5 must not be skipped as default: " + sql);
    }

    @Test
    public void testFix_insertEntity_keepsDefaultCompositeIdPartWhenAnotherIdAssigned() {
        CompositeIdEntity entity = new CompositeIdEntity();
        entity.setTenantId(7);
        entity.setLocalId(0);
        entity.setName("Alice");

        AbstractQueryBuilder.SP sp = Dsl.PSC.insert(entity).into("composite_id_tbl").build();
        String sql = sp.query();

        assertTrue(sql.contains("tenant_id"), "assigned id should be included: " + sql);
        assertTrue(sql.contains("local_id"), "default-valued composite id part should be included: " + sql);
        assertTrue(sql.contains("name"), "regular non-null property should be included: " + sql);
        assertEquals(Arrays.asList(7L, 0L, "Alice"), sp.parameters());
    }

    @Test
    public void testFix_setColumnNamesNamedSqlDeduplicatesPlaceholders() {
        AbstractQueryBuilder.SP sp = Dsl.NSC.update("users").set("status").where(Filters.eq("status", "OLD")).build();

        assertEquals("UPDATE users SET status = :status WHERE status = :status_2", sp.query());
        assertEquals(Arrays.asList("OLD"), sp.parameters());
    }

    @Test
    public void testFix_setColumnNamesIbatisSqlSanitizesAliasAndDeduplicatesPlaceholders() {
        AbstractQueryBuilder.SP sp = Dsl.MSC.update("users").set("u.firstName").where(Filters.eq("u.firstName", "John")).build();
        String sql = sp.query();

        assertTrue(sql.contains("#{firstName}"), "SET placeholder should be sanitized: " + sql);
        assertTrue(sql.contains("#{firstName_2}"), "WHERE placeholder should be de-duplicated: " + sql);
        assertFalse(sql.contains("#{u.firstName}"), "Raw dotted placeholder is invalid: " + sql);
        assertEquals(Arrays.asList("John"), sp.parameters());
    }

    @Test
    public void testFix_batchInsertEntities_doesNotSkipFractionalBigDecimalIdAsZero() {
        // Same defect as above, but for the batch-insert code path that builds props from a collection.
        FractionalIdEntity e1 = new FractionalIdEntity();
        e1.setMyKey(new java.math.BigDecimal("0.5"));
        e1.setMyName("Alice");

        FractionalIdEntity e2 = new FractionalIdEntity();
        e2.setMyKey(new java.math.BigDecimal("0.7"));
        e2.setMyName("Bob");

        String sql = Dsl.PSC.batchInsert(java.util.Arrays.asList(e1, e2)).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal ids must not be removed as all-zero: " + sql);
    }

    // Bug fix: set(Object entity), insert(entity), and batchInsert(entities) used HashMap internally,
    // which made the resulting SET / INSERT column order depend on hash codes rather than the
    // declared property order. The fix swaps to LinkedHashMap so column order is deterministic.

    @Test
    public void testFix_setEntity_preservesPropertyOrder() {
        Account a = new Account();
        a.setGUI("g");
        a.setEmailAddress("e@e.com");
        a.setFirstName("F");
        a.setMiddleName("M");
        a.setLastName("L");
        a.setStatus(1);

        String sql = Dsl.PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

        // Property order in Account.java is: gui, emailAddress, firstName, middleName, lastName, status, ...
        // The SET clause must therefore list those columns in that order.
        int gui = sql.indexOf("gui = ?");
        int email = sql.indexOf("email_address = ?");
        int first = sql.indexOf("first_name = ?");
        int middle = sql.indexOf("middle_name = ?");
        int last = sql.indexOf("last_name = ?");
        int status = sql.indexOf("status = ?");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    @Test
    public void testFix_insertEntity_preservesPropertyOrder() {
        Account a = new Account();
        a.setGUI("g");
        a.setEmailAddress("e@e.com");
        a.setFirstName("F");
        a.setMiddleName("M");
        a.setLastName("L");
        a.setStatus(1);

        String sql = Dsl.PSC.insert(a).into("account").build().query();

        int gui = sql.indexOf("gui");
        int email = sql.indexOf("email_address");
        int first = sql.indexOf("first_name");
        int middle = sql.indexOf("middle_name");
        int last = sql.indexOf("last_name");
        int status = sql.indexOf("status");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    @Test
    public void testFix_batchInsertEntities_preservesPropertyOrder() {
        Account a1 = new Account();
        a1.setGUI("g1");
        a1.setEmailAddress("e1@e.com");
        a1.setFirstName("F1");
        a1.setMiddleName("M1");
        a1.setLastName("L1");
        a1.setStatus(1);

        Account a2 = new Account();
        a2.setGUI("g2");
        a2.setEmailAddress("e2@e.com");
        a2.setFirstName("F2");
        a2.setMiddleName("M2");
        a2.setLastName("L2");
        a2.setStatus(2);

        String sql = Dsl.PSC.batchInsert(java.util.Arrays.asList(a1, a2)).into("account").build().query();

        int gui = sql.indexOf("gui");
        int email = sql.indexOf("email_address");
        int first = sql.indexOf("first_name");
        int middle = sql.indexOf("middle_name");
        int last = sql.indexOf("last_name");
        int status = sql.indexOf("status");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    /**
     * Regression test: calling {@code set(Object, Set)} with a {@code null} entity must
     * fail fast with a descriptive {@link IllegalArgumentException} rather than throwing
     * a raw {@link NullPointerException} from {@code entity.getClass()}.
     */
    @Test
    public void testSetEntityNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").set((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").set((Object) null, null));
    }

    /**
     * Regression test: {@link AbstractQueryBuilder#sanitizeNamedParameterName(String)}
     * strips a table-alias prefix so the returned identifier can be used as a named
     * parameter in JDBC / MyBatis / Spring named SQL.
     */
    @Test
    public void testSanitizeNamedParameterName_stripsTableAliasPrefix() {
        // Simple names are unchanged.
        assertEquals("id", AbstractQueryBuilder.sanitizeNamedParameterName("id"));
        assertEquals("firstName", AbstractQueryBuilder.sanitizeNamedParameterName("firstName"));
        // Aliased names are stripped to the suffix.
        assertEquals("id", AbstractQueryBuilder.sanitizeNamedParameterName("u.id"));
        assertEquals("orderDate", AbstractQueryBuilder.sanitizeNamedParameterName("ord.orderDate"));
        // Multi-level prefixes collapse to the last segment.
        assertEquals("c", AbstractQueryBuilder.sanitizeNamedParameterName("a.b.c"));
        // Function/expression names are reduced to legal placeholder identifiers.
        assertEquals("COUNT", AbstractQueryBuilder.sanitizeNamedParameterName("COUNT(*)"));
        assertEquals("COUNT", AbstractQueryBuilder.sanitizeNamedParameterName("COUNT(o.id)"));
        // Edge cases.
        assertEquals("", AbstractQueryBuilder.sanitizeNamedParameterName(""));
        assertEquals(null, AbstractQueryBuilder.sanitizeNamedParameterName(null));
        assertEquals("ord", AbstractQueryBuilder.sanitizeNamedParameterName("ord."));
    }

    /**
     * Regression test: the {@code insert(String...).into(...)} VALUES placeholders for named
     * and iBATIS SQL must be routed through {@code nextNamedParameterName(...)} — exactly like
     * every other named-parameter site ({@code set(...)}, {@code appendInsertProps(...)}). Before
     * the fix this INSERT column path emitted the raw column name verbatim, so duplicate column
     * names produced colliding placeholders (e.g. {@code :id, :id}) instead of the de-duplicated
     * {@code :id, :id_2}. The common case of clean, distinct property names is unchanged.
     */
    @Test
    public void testInsertNamedPlaceholdersAreSanitizedAndDeduplicated() {
        // Clean, distinct names: unchanged (no regression).
        String named = Dsl.NLC.insert("firstName", "lastName").into("account").build().query();
        assertTrue(named.contains("VALUES (:firstName, :lastName)"), "Unexpected named INSERT SQL: " + named);

        // Duplicate column names: placeholders must be de-duplicated via the occurrence counter.
        String dup = Dsl.NLC.insert("id", "id").into("account").build().query();
        assertTrue(dup.contains("VALUES (:id, :id_2)"), "Duplicate named placeholders not de-duplicated: " + dup);

        // iBATIS (#{...}) path: same routing through nextNamedParameterName.
        String ibatisClean = Dsl.MLC.insert("firstName").into("account").build().query();
        assertTrue(ibatisClean.contains("VALUES (#{firstName})"), "Unexpected iBATIS INSERT SQL: " + ibatisClean);

        String ibatisDup = Dsl.MLC.insert("id", "id").into("account").build().query();
        assertTrue(ibatisDup.contains("VALUES (#{id}, #{id_2})"), "Duplicate iBATIS placeholders not de-duplicated: " + ibatisDup);
    }

    /**
     * Regression test (Pass 2): {@code Account} declares sub-entity properties
     * ({@code contact} and {@code devices}). When a SELECT-with-sub-entities is
     * built, every code path that iterates {@link com.landawn.abacus.parser.ParserUtil.BeanInfo#subEntityPropNameList}
     * and calls {@code getPropInfo(name)} must tolerate the rare null return
     * (defensive guard added in {@code getSelectTableNames} and {@code getFromClause}).
     * The normal happy path should still produce SQL with the sub-entity tables.
     */
    @Test
    public void testSelectWithSubEntities_DoesNotThrow_Pass2() {
        // Force the include-sub-entities select path. Using PSC.selectFrom(Class, boolean)
        // exercises both helpers that previously dereferenced propInfo without a null check.
        String sql = Dsl.PSC.selectFrom(Account.class, true).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"), "Should produce a SELECT");
        assertTrue(sql.contains("FROM"), "Should produce a FROM");
    }

    /**
     * Regression test (Pass 2): the constructor-time builder-leak warning logic must log
     * at the highest applicable severity. Previously the {@code else if (> 1024)} branch
     * was unreachable when warn was enabled, because the prior {@code if (> 512 && warn)}
     * branch consumed all matching cases. The fix swaps the order so that an over-1024
     * count produces the ERROR-level log instead of the warning.
     *
     * <p>Functional verification is indirect: we exercise the constructor + build() lifecycle
     * heavily enough that the warning branch would be reachable, and confirm that the
     * builder still produces valid SQL without throwing. (Asserting on the log output itself
     * would require a logger mock and is out of scope.)
     */
    @Test
    public void testManyBuildersDoNotLeakOrThrow_Pass2() {
        for (int i = 0; i < 32; i++) {
            String sql = Dsl.PSC.select("id").from(Account.class).where(Filters.eq("id", i)).build().query();
            assertNotNull(sql);
        }
    }

    /**
     * Regression test: a {@code null} {@link Condition} passed to {@code where(Condition)},
     * {@code having(Condition)}, {@code on(Condition)}, or {@code append(Condition)} must
     * fail fast with {@link IllegalArgumentException}. Previously these methods silently
     * fell through to {@code appendCondition(null)}, producing malformed SQL containing
     * a bare {@code WHERE}/{@code HAVING}/{@code ON} keyword followed by no expression.
     */
    @Test
    public void testWhereHavingOnAppendRejectNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id").having((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users u").join("orders o").on((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").append((Condition) null).build().query());
    }
}
