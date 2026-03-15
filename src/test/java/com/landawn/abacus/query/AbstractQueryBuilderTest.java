package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String sql = SqlBuilder.PSC.select("id", "firstName", "lastName").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testToSql() {
        String sql = SqlBuilder.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testBuild() {
        AbstractQueryBuilder.SP sqlPair = SqlBuilder.PSC.select("id").from(Account.class).where(Filters.eq("id", 1)).build();
        assertNotNull(sqlPair);
        assertTrue(sqlPair.query().contains("WHERE"));
        assertEquals(1, sqlPair.parameters().size());
    }

    @Test
    public void testPSCWithWhere() {
        String sql = SqlBuilder.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testPSCWithMultipleConditions() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testPSCWithOrderBy() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).orderBy("firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testPSCWithLimit() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testPSCWithJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testPSCWithLeftJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testPSCWithInnerJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testPSCWithRightJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testPSCWithFullJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testPSCWithCrossJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").crossJoin("roles").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testPSCWithGroupBy() {
        String sql = SqlBuilder.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testPSCWithHaving() {
        String sql = SqlBuilder.PSC.select("department", "COUNT(*)")
                .from("employees")
                .groupBy("department")
                .having(Filters.expr("COUNT(*) > 5"))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testPSCWithDistinct() {
        String sql = SqlBuilder.PSC.select("status").from(Account.class).distinct().build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testPSCComplexQuery() {
        String sql = SqlBuilder.PSC.select("u.id", "u.firstName", "COUNT(o.id) as order_count")
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
        String sql = SqlBuilder.PSC.insertInto(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdate() {
        String sql = SqlBuilder.PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = SqlBuilder.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = SqlBuilder.PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AS"));
    }

    @Test
    public void testSelectWithMultipleTables() {
        String sql = SqlBuilder.PSC.select("*").from("users", "orders").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testWhereWithOr() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleOrderBy() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).orderBy("lastName", "firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByRejectsCommentToken() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").orderBy("id--").build().query());
    }

    @Test
    public void testOrderByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").orderBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").orderBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testOrderByAsc() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).limit(20, 10).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testFromWithEntityClass() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testJoinWithEntityClass() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testIntoWithTableName() {
        String sql = SqlBuilder.PSC.insert("id", "name").into("accounts").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testIntoRejectsEmptyTableName() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.insert("id").into("").build().query());
    }

    @Test
    public void testUpdateWithSet() {
        String sql = SqlBuilder.PSC.update("accounts").set("status", "inactive").set("updated_at", "NOW()").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFromWithTable() {
        String sql = SqlBuilder.PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectCount() {
        String sql = SqlBuilder.PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    public void testSelectAll() {
        String sql = SqlBuilder.PSC.select(AbstractQueryBuilder.ALL).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testChainedAndOr() {
        String sql = SqlBuilder.PSC.select("*")
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
        String sql = SqlBuilder.PSC.select("*")
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
        assertThrows(IllegalStateException.class, () -> SqlBuilder.PSC.select("*").from("users").limit(10, 5).offset(2));
    }

    @Test
    public void testAppendLimitConditionWithExpression() {
        String sql = SqlBuilder.PSC.select("*").from("users").append(new Limit("10 OFFSET 20")).build().query();
        assertTrue(sql.endsWith("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testAppendConditionAfterWhereThrowsDuplicateWhere() {
        assertThrows(IllegalStateException.class,
                () -> SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.eq("name", "Alice")).build().query());
    }

    @Test
    public void testAppendWhereClauseAfterWhereThrows() {
        assertThrows(IllegalStateException.class,
                () -> SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.where(Filters.eq("name", "Alice"))).build().query());
    }

    @Test
    public void testAppendCriteriaAfterWhereThrowsWhenCriteriaHasWhere() {
        Criteria criteria = Criteria.builder().where(Filters.eq("name", "Alice")).build();

        assertThrows(IllegalStateException.class, () -> SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(criteria).build().query());
    }

    @Test
    public void testAppendLimitExpressionAfterLimitThrows() {
        assertThrows(IllegalStateException.class, () -> SqlBuilder.PSC.select("*").from("users").limit(10).append(new Limit("5")).build().query());
    }

    @Test
    public void testSelectAllowsHashJsonOperators() {
        String sql = SqlBuilder.PSC.select("payload #>> '{meta,status}'").from("docs").build().query();
        assertTrue(sql.contains("#>>"));
    }

    @Test
    public void testSelectAllowsCommentLikeTokenInsideQuotedLiteral() {
        String sql = SqlBuilder.PSC.select("CASE WHEN note = '--literal' THEN 1 ELSE 0 END").from("docs").build().query();
        assertTrue(sql.contains("'--literal'"));
    }

    @Test
    public void testUpdateAllowsIbatisPlaceholderExpression() {
        String sql = SqlBuilder.PSC.update("users").set("name = #{name}").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("#{name}"));
    }

    @Test
    public void testGroupByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").groupBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").groupBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testWhereRejectsNullStringExpression() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select("*").from("users").where((String) null));
    }
}

class AbstractQueryBuilder2026Test extends TestBase {

    @Test
    public void testSetHandlerForNamedParameter() {
        AbstractQueryBuilder.setHandlerForNamedParameter((sb, propName) -> sb.append("#{").append(propName).append("}"));

        try {
            final String sql = SqlBuilder.NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("#{id}"));
        } finally {
            AbstractQueryBuilder.resetHandlerForNamedParameter();
        }
    }

    @Test
    public void testResetHandlerForNamedParameter() {
        AbstractQueryBuilder.setHandlerForNamedParameter((sb, propName) -> sb.append("#{").append(propName).append("}"));
        AbstractQueryBuilder.resetHandlerForNamedParameter();

        final String sql = SqlBuilder.NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains(":id"));
    }

    @Test
    public void testSelectModifier() {
        final String sql = SqlBuilder.PSC.select("*").selectModifier("TOP 5").from("users").build().query();

        assertTrue(sql.contains("SELECT TOP 5"));
    }

    @Test
    public void testNaturalJoin_String() {
        final String sql = SqlBuilder.PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertTrue(sql.contains("NATURAL JOIN orders"));
    }

    @Test
    public void testNaturalJoin_EntityClass() {
        final String sql = SqlBuilder.PSC.select("*").from(Account.class).naturalJoin(Account.class).build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.toLowerCase().contains("account"));
    }

    @Test
    public void testNaturalJoin_EntityClassAlias() {
        final String sql = SqlBuilder.PSC.select("*").from(Account.class, "a").naturalJoin(Account.class, "b").build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.contains(" b"));
    }

    @Test
    public void testUsing() {
        final String sql = SqlBuilder.PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testOffsetRows() {
        final String sql = SqlBuilder.PSC.select("*").from("users").orderBy("id").offsetRows(20).build().query();

        assertTrue(sql.contains("OFFSET 20 ROWS"));
    }

    @Test
    public void testFetchNextRows() {
        final String sql = SqlBuilder.PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testFetchFirstRows() {
        final String sql = SqlBuilder.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testAppendIf_Condition() {
        final String withCondition = SqlBuilder.PSC.select("*").from("users").appendIf(true, Filters.eq("status", "ACTIVE")).build().query();
        final String withoutCondition = SqlBuilder.PSC.select("*").from("users").appendIf(false, Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(withCondition.contains("status"));
        assertTrue(!withoutCondition.contains("status"));
    }

    @Test
    public void testAppendIf_String() {
        final String withExpression = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(true, " FOR UPDATE").build().query();
        final String withoutExpression = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(false, " FOR UPDATE").build().query();

        assertTrue(withExpression.contains("FOR UPDATE"));
        assertTrue(!withoutExpression.contains("FOR UPDATE"));
    }

    @Test
    public void testAppendIf_Consumer() {
        final String sql = SqlBuilder.PSC.select("*")
                .from("users")
                .appendIf(true, builder -> builder.where(Filters.eq("status", "ACTIVE")).orderBy("name"))
                .build()
                .query();

        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testAppendIfOrElse_Condition() {
        final AbstractQueryBuilder.SP trueBranch = SqlBuilder.PSC.select("*")
                .from("users")
                .appendIfOrElse(true, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();
        final AbstractQueryBuilder.SP falseBranch = SqlBuilder.PSC.select("*")
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
        final String asc = SqlBuilder.PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        final String desc = SqlBuilder.PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();

        assertTrue(asc.contains("ORDER BY name ASC"));
        assertTrue(desc.contains("ORDER BY name DESC"));
    }

    @Test
    public void testUnion_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .union(SqlBuilder.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnion_Query() {
        final String sql = SqlBuilder.PSC.select("id").from("users").union("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnion_Columns() {
        final String sql = SqlBuilder.PSC.select("id").from("users").union("id").from("admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnion_Collection() {
        final String sql = SqlBuilder.PSC.select("id").from("users").union(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .unionAll(SqlBuilder.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION ALL"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnionAll_Query() {
        final String sql = SqlBuilder.PSC.select("id").from("users").unionAll("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_Columns() {
        final String sql = SqlBuilder.PSC.select("id").from("users").unionAll("id").from("admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_Collection() {
        final String sql = SqlBuilder.PSC.select("id").from("users").unionAll(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .intersect(SqlBuilder.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("INTERSECT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testIntersect_Query() {
        final String sql = SqlBuilder.PSC.select("id").from("users").intersect("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_Columns() {
        final String sql = SqlBuilder.PSC.select("id").from("users").intersect("id").from("admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_Collection() {
        final String sql = SqlBuilder.PSC.select("id").from("users").intersect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .except(SqlBuilder.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("EXCEPT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testExcept_Query() {
        final String sql = SqlBuilder.PSC.select("id").from("users").except("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_Columns() {
        final String sql = SqlBuilder.PSC.select("id").from("users").except("id").from("admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_Collection() {
        final String sql = SqlBuilder.PSC.select("id").from("users").except(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .minus(SqlBuilder.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("MINUS"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testMinus_Query() {
        final String sql = SqlBuilder.PSC.select("id").from("users").minus("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testMinus_Columns() {
        final String sql = SqlBuilder.PSC.select("id").from("users").minus("id").from("admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testMinus_Collection() {
        final String sql = SqlBuilder.PSC.select("id").from("users").minus(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testForUpdate() {
        final String sql = SqlBuilder.PSC.select("*").from("users").forUpdate().build().query();

        assertTrue(sql.contains("FOR UPDATE"));
    }

    @Test
    public void testApply_SPFunction() throws Exception {
        final List<Object> result = SqlBuilder.PSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .apply(sp -> Arrays.asList(sp.query(), sp.parameters().size()));

        assertTrue(result.get(0).toString().contains("WHERE"));
        assertEquals(1, result.get(1));
    }

    @Test
    public void testApply_SqlAndParams() throws Exception {
        final String result = SqlBuilder.PSC.select("id").from("users").where(Filters.eq("id", 1)).apply((sql, params) -> sql + " / " + params.size());

        assertTrue(result.contains("WHERE"));
        assertTrue(result.endsWith("/ 1"));
    }

    @Test
    public void testAccept_SPConsumer() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        SqlBuilder.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept(sp -> {
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

        SqlBuilder.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept((sql, params) -> {
            sqlHolder[0] = sql;
            paramCount[0] = params.size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    @Test
    public void testPrintln() {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            SqlBuilder.PSC.select("id").from("users").println();
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
