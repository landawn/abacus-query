package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class SqlBuilderTest extends TestBase {
    // Basic SELECT tests
    @Test
    public void testSelectAll() {
        String sql = Dsl.PSC.select("*").from("users").build().query();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testSelectSingleColumn() {
        String sql = Dsl.PSC.select("name").from("users").build().query();
        assertEquals("SELECT name FROM users", sql);
    }

    @Test
    public void testSelectMultipleColumns() {
        String sql = Dsl.PSC.select("id", "name", "email").from("users").build().query();
        assertTrue(sql.contains("SELECT id, name, email"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = Dsl.PSC.select("first_name AS fname").from("users").build().query();
        assertTrue(sql.contains("AS fname"));
    }

    @Test
    public void testSelectWithEntityClass() {
        String sql = Dsl.PSC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    // FROM clause tests
    @Test
    public void testFromSingleTable() {
        String sql = Dsl.PSC.select("*").from("users").build().query();
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testFromMultipleTables() {
        String sql = Dsl.PSC.select("*").from("users", "orders").build().query();
        assertTrue(sql.contains("FROM orders"));
    }

    @Test
    public void testFromWithAlias() {
        String sql = Dsl.PSC.select("u.*").from("users u").build().query();
        assertTrue(sql.contains("FROM users u"));
    }

    @Test
    public void testFromEntityClass() {
        String sql = Dsl.PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
    }

    // WHERE clause tests
    @Test
    public void testWhereEqual() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testWhereMultipleConditions() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testWhereOr() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator"))).build().query();
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testWhereBetween() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testWhereLike() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testWhereIsNull() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNull("deleted_at")).build().query();
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testWhereIsNotNull() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();
        assertTrue(sql.contains("IS NOT NULL"));
    }

    @Test
    public void testWhereIsWithNullValue() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.is("deleted_at", null)).build().query();
        assertTrue(sql.contains("IS NULL"));
        assertFalse(sql.contains("IS ?"));
    }

    @Test
    public void testWhereIsNotWithNullValue() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNot("deleted_at", null)).build().query();
        assertTrue(sql.contains("IS NOT NULL"));
        assertFalse(sql.contains("IS NOT ?"));
    }

    // JOIN tests
    @Test
    public void testInnerJoin() {
        String sql = Dsl.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testLeftJoin() {
        String sql = Dsl.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testRightJoin() {
        String sql = Dsl.PSC.select("*").from("users").rightJoin("departments").on("users.dept_id = departments.id").build().query();
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testFullJoin() {
        String sql = Dsl.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testCrossJoin() {
        String sql = Dsl.PSC.select("*").from("users").crossJoin("roles").build().query();
        assertTrue(sql.contains("CROSS JOIN"));
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
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
    }

    // GROUP BY tests
    @Test
    public void testGroupBy() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testGroupByMultipleColumns() {
        String sql = Dsl.PSC.select("year", "month", "COUNT(*)").from("sales").groupBy("year", "month").build().query();
        assertTrue(sql.contains("GROUP BY"));
    }

    // HAVING tests
    @Test
    public void testHaving() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
        assertTrue(sql.contains("HAVING"));
    }

    // ORDER BY tests
    @Test
    public void testOrderBy() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("name").build().query();
        assertTrue(sql.contains("ORDER BY name"));
    }

    @Test
    public void testOrderByAsc() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("name", SortDirection.ASC).build().query();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("ASC"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("created_date", SortDirection.DESC).build().query();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("DESC"));
    }

    @Test
    public void testOrderByMultipleColumns() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("last_name", "first_name").build().query();
        assertTrue(sql.contains("ORDER BY"));
    }

    // LIMIT tests
    @Test
    public void testLimit() {
        String sql = Dsl.PSC.select("*").from("users").limit(10).build().query();
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = Dsl.PSC.select("*").from("users").limit(20, 10).build().query();
        assertTrue(sql.contains("LIMIT"));
    }

    // DISTINCT tests
    @Test
    public void testDistinct() {
        String sql = Dsl.PSC.select("status").from("users").distinct().build().query();
        assertTrue(sql.contains("DISTINCT"));
    }

    // INSERT tests
    @Test
    public void testInsertInto() {
        String sql = Dsl.PSC.insert("id", "name").into("users").build().query();
        assertTrue(sql.contains("INSERT INTO users"));
    }

    @Test
    public void testInsertIntoWithEntityClass() {
        String sql = Dsl.PSC.insert("firstName", "lastName").into(Account.class).build().query();
        assertTrue(sql.contains("INSERT INTO"));
    }

    // UPDATE tests
    @Test
    public void testUpdate() {
        String sql = Dsl.PSC.update("users").set("status", "inactive").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("UPDATE users"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateMultipleColumns() {
        String sql = Dsl.PSC.update("users").set("first_name", "John").set("last_name", "Doe").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateWithEntityClass() {
        String sql = Dsl.PSC.update(Account.class).set("status", "active").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
    }

    // DELETE tests
    @Test
    public void testDeleteFrom() {
        String sql = Dsl.PSC.deleteFrom("users").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("DELETE FROM users"));
    }

    @Test
    public void testDeleteFromWithEntityClass() {
        String sql = Dsl.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = Dsl.PSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder_NamedSnakeCase() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = Dsl.NSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder_NamedLowerCamelCase() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = Dsl.NLC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (firstName, lastName) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithNullMapRows() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(null);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = Dsl.PSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    // Complex query tests
    @Test
    public void testComplexSelectQuery() {
        String sql = Dsl.PSC.select("u.id", "u.name", "COUNT(o.id) as order_count")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.eq("u.status", "active"))
                .groupBy("u.id", "u.name")
                .having(Filters.expr("COUNT(o.id) > 5"))
                .orderBy("order_count", SortDirection.DESC)
                .limit(10)
                .build()
                .query();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("HAVING"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT"));
    }

    // Naming policy tests
    @Test
    public void testSelectWithLowerCaseUnderscore() {
        String sql = Dsl.PSC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testSelectWithUpperCaseUnderscore() {
        String sql = Dsl.PAC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
    }

    // Parameterized query tests
    @Test
    public void testPairWithParameters() {
        SqlBuilder builder = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1));
        AbstractQueryBuilder.SP sp = builder.build();
        assertNotNull(sp);
        assertNotNull(sp.query());
        assertNotNull(sp.parameters());
    }

    // Subquery tests
    @Test
    public void testSubquery() {
        String subquery = Dsl.PSC.select("id").from("active_users").build().query();
        String sql = Dsl.PSC.select("*").from("orders").where("user_id IN (" + subquery + ")").build().query();
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("SELECT"));
    }

    // UNION tests
    @Test
    public void testUnion() {
        String sql1 = Dsl.PSC.select("id", "name").from("users").build().query();
        String sql2 = Dsl.PSC.select("id", "name").from("archived_users").build().query();
        String unionSql = sql1 + " UNION " + sql2;
        assertTrue(unionSql.contains("UNION"));
    }

    // SqlExpression tests
    @Test
    public void testExpressionInSelect() {
        String sql = Dsl.PSC.select("COUNT(*) as total").from("users").build().query();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testExpressionInWhere() {
        SqlExpression expr = Filters.expr("age > 18 AND status = 'active'");
        String sql = Dsl.PSC.select("*").from("users").where(expr).build().query();
        assertNotNull(sql);
    }

    // Aggregate functions tests
    @Test
    public void testCount() {
        String sql = Dsl.PSC.select("COUNT(*)").from("users").build().query();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testSum() {
        String sql = Dsl.PSC.select("SUM(amount)").from("orders").build().query();
        assertTrue(sql.contains("SUM(amount)"));
    }

    @Test
    public void testAvg() {
        String sql = Dsl.PSC.select("AVG(price)").from("products").build().query();
        assertTrue(sql.contains("AVG(price)"));
    }

    @Test
    public void testMax() {
        String sql = Dsl.PSC.select("MAX(price)").from("products").build().query();
        assertTrue(sql.contains("MAX(price)"));
    }

    @Test
    public void testMin() {
        String sql = Dsl.PSC.select("MIN(price)").from("products").build().query();
        assertTrue(sql.contains("MIN(price)"));
    }

    // Build method tests
    @Test
    public void testBuildMethod() {
        AbstractQueryBuilder.SP sp = Dsl.PSC.select("*").from("users").build();
        assertNotNull(sp);
        assertNotNull(sp.query());
    }

    // Multiple where conditions with different operators
    @Test
    public void testWhereGreaterThan() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();
        assertTrue(sql.contains(">"));
    }

    @Test
    public void testWhereLessThan() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.lt("age", 65)).build().query();
        assertTrue(sql.contains("<"));
    }

    @Test
    public void testWhereGreaterThanOrEqual() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.ge("age", 21)).build().query();
        assertTrue(sql.contains(">="));
    }

    @Test
    public void testWhereLessThanOrEqual() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.le("age", 60)).build().query();
        assertTrue(sql.contains("<="));
    }

    @Test
    public void testWhereNotEqual() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.ne("status", "deleted")).build().query();
        assertTrue(sql.contains("!=") || sql.contains("<>"));
    }

    // IN clause tests
    @Test
    public void testWhereIn() {
        String sql = Dsl.PSC.select("*").from("users").where("id IN (1, 2, 3)").build().query();
        assertTrue(sql.contains("IN"));
    }

    // CASE WHEN tests
    @Test
    public void testCaseWhen() {
        String sql = Dsl.PSC.select("CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END as age_group").from("users").build().query();
        assertTrue(sql.contains(" case when age < 18 then 'minor' else 'adult' end AS age_group "));
    }

    // Multiple table sources
    @Test
    public void testFromMultipleTablesWithJoin() {
        String sql = Dsl.PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").join("products p").on("o.product_id = p.id").build().query();
        assertTrue(sql.contains("users u"));
        assertTrue(sql.contains("orders o"));
        assertTrue(sql.contains("products p"));
    }

    // Chaining tests
    @Test
    public void testChainedAndConditions() {
        String sql = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).and(Filters.lt("age", 65)))
                .build()
                .query();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testChainedOrConditions() {
        String sql = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator")).or(Filters.eq("role", "owner")))
                .build()
                .query();
        assertTrue(sql.contains("OR"));
    }

    // Edge case tests
    @Test
    public void testSelectWithNoFrom() {
        assertThrows(Exception.class, () -> {
            Dsl.PSC.select("*").build().query();
        });
    }

    @Test
    public void testEmptySelect() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select().from("users").build().query());
    }

    // Named SQL tests
    @Test
    public void testNamedInsert() {
        String sql = Dsl.PSC.insert("firstName", "lastName").into(Account.class).build().query();
        assertNotNull(sql);
    }

    // Static factory tests
    @Test
    public void testSelectFactory() {
        SqlBuilder builder = Dsl.PSC.select("*");
        assertNotNull(builder);
    }

    @Test
    public void testInsertIntoFactory() {
        SqlBuilder builder = Dsl.PSC.insert("id", "name");
        assertNotNull(builder);
    }

    @Test
    public void testUpdateFactory() {
        SqlBuilder builder = Dsl.PSC.update("users");
        assertNotNull(builder);
    }

    @Test
    public void testDeleteFromFactory() {
        SqlBuilder builder = Dsl.PSC.deleteFrom("users");
        assertNotNull(builder);
    }

    // Performance and resource cleanup
    @Test
    public void testMultipleBuildCalls() {
        SqlBuilder builder = Dsl.PSC.select("*").from("users");
        String sql1 = builder.build().query();
        assertNotNull(sql1);
        // Builder should be reusable or properly cleaned up
    }

    // Collection-based select
    @Test
    public void testSelectWithCollection() {
        String sql = Dsl.PSC.select(Arrays.asList("id", "name", "email")).from("users").build().query();
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("name"));
        assertTrue(sql.contains("email"));
    }

    // Map-based operations
    @Test
    public void testUpdateWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("first_name", "John");
        props.put("last_name", "Doe");

        String sql = Dsl.PSC.update("users").set(props).where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("SET"));
    }

    // Complex conditions
    @Test
    public void testWhereWithNestedAndOr() {
        String sql = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.eq("role", "admin"), Filters.eq("role", "moderator"))))
                .build()
                .query();
        assertNotNull(sql);
    }

    // NULL handling
    @Test
    public void testWhereNullCheck() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNull("deleted_at").and(Filters.isNotNull("email"))).build().query();
        assertTrue(sql.contains("IS NULL"));
        assertTrue(sql.contains("IS NOT NULL"));
    }

    // Preselect tests
    @Test
    public void testPreselectDistinct() {
        String sql = Dsl.PSC.select("status").from("users").selectModifier("DISTINCT").build().query();
        assertTrue(sql.contains("DISTINCT"));
    }

    // USING clause tests
    @Test
    public void testJoinUsing() {
        String sql = Dsl.PSC.select("*").from("users").join("orders").using("user_id").build().query();
        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testBinaryWithSubQueryRhsIsParenthesized() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.equal("id", Filters.subQuery("SELECT MAX(user_id) FROM orders"))).build().query();
        assertTrue(sql.contains("id = (SELECT MAX(user_id) FROM orders)"));
    }

    // Offset tests
    @Test
    public void testOffset() {
        String sql = Dsl.PSC.select("*").from("users").offset(20).build().query();
        assertTrue(sql.contains("OFFSET") || sql.contains("20"));
    }

    // Constants verification
    @Test
    public void testConstants() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    // ==== Regression tests for the 2026-07-03 review fixes ====

    @Test
    public void testSinglePropRowValueInRendersTupleRows() {
        // A single-prop row-value In previously fell into the scalar IN path, binding each tuple List
        // whole as one parameter ("id IN (?, ?)" with params [[1],[2]]; raw mode "IN ('[1]', '[2]')").
        SP sp = Dsl.PSC.select("*").from("users").where(Filters.in(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
        assertEquals("SELECT * FROM users WHERE (id) IN ((?), (?))", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());

        SP named = Dsl.NSC.select("*").from("users").where(Filters.in(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
        assertEquals("SELECT * FROM users WHERE (id) IN ((:id1), (:id2))", named.query());
        assertEquals(Arrays.asList(1, 2), named.parameters());

        SP notIn = Dsl.PSC.select("*").from("users").where(Filters.notIn(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
        assertEquals("SELECT * FROM users WHERE (id) NOT IN ((?), (?))", notIn.query());
        assertEquals(Arrays.asList(1, 2), notIn.parameters());
    }

    @Test
    public void testCriteriaJoinWithRawConditionRendersOnKeyword() {
        // A raw (non-On/Using) join condition previously rendered with no ON keyword and no separator,
        // fusing the table name and the condition: "JOIN orderst1.id = ...".
        Criteria exprJoin = Criteria.builder().join("orders", Filters.expr("users.id = orders.user_id")).build();
        assertEquals("SELECT * FROM users JOIN orders ON users.id = orders.user_id", Dsl.PSC.select("*").from("users").append(exprJoin).build().query());

        Criteria binaryJoin = Criteria.builder().join("orders", Filters.equal("orders.user_id", 5)).build();
        SP sp = Dsl.PSC.select("*").from("users").append(binaryJoin).build();
        assertEquals("SELECT * FROM users JOIN orders ON orders.user_id = ?", sp.query());
        assertEquals(Arrays.asList(5), sp.parameters());

        // An On condition renders its own ON keyword -- no double "ON ON".
        Criteria onJoin = Criteria.builder().join("orders", Filters.on("users.id", "orders.user_id")).build();
        String onSql = Dsl.PSC.select("*").from("users").append(onJoin).build().query();
        assertTrue(onSql.contains("JOIN orders ON ("));
        assertFalse(onSql.contains("ON ON"));
    }

    @Test
    public void testSetOperationCrossBaseNamedParameterCollision() {
        // Parent's literal property "id_2" used to collide with the child's generated "id" + "_2" suffix,
        // binding the same :id_2 placeholder to two different values.
        SqlBuilder child = Dsl.NSC.select("id").from("t2").where(Filters.and(Filters.equal("id", 1), Filters.equal("id", 2)));
        SP sp = Dsl.NSC.select("id").from("t1").where(Filters.equal("id_2", 100)).union(child).build();

        List<String> names = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(":([A-Za-z0-9_]+)").matcher(sp.query());
        while (m.find()) {
            names.add(m.group(1));
        }

        assertEquals(3, names.size());
        assertEquals(3, new HashSet<>(names).size()); // every placeholder unique across the compound query
        assertEquals(Arrays.asList(100, 1, 2), sp.parameters());
    }

    @Test
    public void testIncompleteSetOperationSegmentThrowsInsteadOfTruncating() {
        // Previously built "SELECT id FROM t UNION " with the staged columns silently dropped.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("t").union(Arrays.asList("id", "name")).build());

        // A non-SELECT sibling builder is rejected up front instead of being staged as a "column list".
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("t").union(Dsl.PSC.update("t2").set("a")));

        // Completing the segment still works.
        String sql = Dsl.PSC.select("id").from("t").union(Arrays.asList("id", "name")).from("t2").build().query();
        assertTrue(sql.startsWith("SELECT id FROM t UNION SELECT "));
        assertTrue(sql.endsWith("FROM t2"));
    }

    @Test
    public void testSelectModifierEmittedVerbatim() {
        // The modifier used to be routed through column-name normalization, camelCasing any
        // non-keyword modifier under PLC (SQL_CALC_FOUND_ROWS -> sqlCalcFoundRows).
        assertEquals("SELECT SQL_CALC_FOUND_ROWS * FROM account", Dsl.PLC.select("*").selectModifier("SQL_CALC_FOUND_ROWS").from("account").build().query());

        // The late-insert path (selectModifier after from) must be verbatim too.
        assertEquals("SELECT SQL_CALC_FOUND_ROWS * FROM account", Dsl.PLC.select("*").from("account").selectModifier("SQL_CALC_FOUND_ROWS").build().query());
    }

    @Test
    public void testFromAndIntoReentryGuards() {
        // Double from() used to emit two fused SELECT fragments.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("t1").from("t2"));

        // into() after from() used to concatenate a second INSERT statement with no separator.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("firstName").from("account").into("backup"));

        // A second into() is rejected as well.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.insert("firstName").into("t1").into("t2"));

        // An INSERT ... SELECT left without its from() now fails at build instead of emitting a fragment.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("firstName").into("backup").build());

        // The documented INSERT ... SELECT flow still works.
        assertEquals("INSERT INTO account_backup (first_name) SELECT first_name AS \"firstName\" FROM account",
                Dsl.PSC.select("firstName").into("account_backup").from("account").build().query());
    }

    @Test
    public void testVerbatimLimitLiteralConsumesOffsetSlot() {
        // "LIMIT ? OFFSET ?" followed by offset(5) used to emit a second OFFSET clause.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("t").append(Filters.limit("LIMIT ? OFFSET ?")).offset(5));

        // A literal without OFFSET leaves the offset slot available.
        assertEquals("SELECT id FROM t LIMIT ? OFFSET 5", Dsl.PSC.select("id").from("t").append(Filters.limit("LIMIT ?")).offset(5).build().query());
    }

    @Test
    public void testLimitLiteralPlaceholderNameContainingOffsetKeepsOffsetSlot() {
        // The raw substring test used to treat the placeholder name as an OFFSET clause,
        // making the follow-up offset(20) fail with "'OFFSET' has already been set".
        assertEquals("SELECT id FROM t LIMIT :rowOFFSETCount OFFSET 20",
                Dsl.NSC.select("id").from("t").append(Filters.limit("LIMIT :rowOFFSETCount")).offset(20).build().query());

        // Literals that really carry the OFFSET keyword still consume the slot.
        assertThrows(IllegalStateException.class, () -> Dsl.NSC.select("id").from("t").append(Filters.limit("LIMIT :count OFFSET :offset")).offset(20));
        assertThrows(IllegalStateException.class, () -> Dsl.NSC.select("id").from("t").append(Filters.limit("LIMIT #{count} OFFSET #{offset}")).offset(20));
    }

    @Test
    public void testDanglingIntoSegmentThrowsForAliasesAndMultiSelects() {
        // select(Map).into() without the follow-up from() used to emit a truncated
        // "INSERT INTO account (first_name)" with no SELECT or VALUES part.
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("firstName", "fn");
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select(aliases).into("account").build());

        // Same for select(List<Selection>).into().
        List<Selection> selections = Arrays.asList(Selection.builder(Account.class).build());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select(selections).into("account").build());

        // Completing the INSERT ... SELECT still works.
        assertEquals("INSERT INTO backup (first_name) SELECT first_name AS \"fn\" FROM account",
                Dsl.PSC.select(aliases).into("backup").from("account").build().query());
    }

    @Test
    public void testStaleMultiSelectsDoNotLeakAcrossSetOperation() {
        // The multi-select column list used to survive a verbatim union and be re-emitted by the
        // next from(), producing two juxtaposed SELECTs ("... UNION SELECT id FROM t2 SELECT ... FROM t3").
        List<Selection> selections = Arrays.asList(Selection.builder(Account.class).build());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select(selections).from("account").union("SELECT id FROM t2").from("t3"));

        // The verbatim-union compound itself still builds.
        String sql = Dsl.PSC.select(selections).from("account").union("SELECT id FROM t2").build().query();
        assertTrue(sql.endsWith(" FROM account UNION SELECT id FROM t2"));
    }

    @Test
    public void testSelectModifierAfterVerbatimSetOperationThrows() {
        // DISTINCT staged after union("SELECT ...") used to be dropped silently: there is no SELECT
        // keyword to attach it to and no from() can legally follow the verbatim sub-query.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("t1").union("SELECT id FROM t2").distinct().build());

        // A modifier staged before a column-list set operation is still rendered by the follow-up from().
        assertEquals("SELECT id FROM t1 UNION SELECT DISTINCT id, name FROM t2",
                Dsl.PSC.select("id").from("t1").union(Arrays.asList("id", "name")).distinct().from("t2").build().query());
    }

    @Test
    public void testSelectSingleExpressionAppliesNamingPolicy() {
        // Documented behavior of Dsl.select(String): identifiers inside the expression ARE converted
        // by the naming policy; function names, keywords and the AS alias are preserved.
        assertEquals("SELECT first_name || ' ' || last_name AS fullName FROM account",
                Dsl.PSC.select("firstName || ' ' || lastName AS fullName").from("account").build().query());
    }

    @Test
    public void testEntityClassSelectAliasesEveryColumnBackToItsPropertyName() {
        // Pins the Dsl Javadoc examples for select(Class), selectFrom(Class) and selectFrom(Class, alias):
        // with a naming policy other than NO_CHANGE, EVERY selected column of an entity-class SELECT is
        // aliased back to its property name - including columns like id/email whose rendered column name
        // already equals the property name. (The string-based select(String...) overload, by contrast,
        // only aliases names the naming policy actually changed.)
        assertEquals("SELECT id AS \"id\", first_name AS \"firstName\", last_name AS \"lastName\", email AS \"email\" FROM account",
                Dsl.PSC.select(JavadocAccount.class).from("account").build().query());

        assertEquals("SELECT id AS \"id\", first_name AS \"firstName\", last_name AS \"lastName\", email AS \"email\" FROM account WHERE email = ?",
                Dsl.PSC.selectFrom(JavadocAccount.class).where(Filters.eq("email", "john.doe@example.com")).build().query());

        assertEquals(
                "SELECT a.id AS \"id\", a.first_name AS \"firstName\", a.last_name AS \"lastName\", a.email AS \"email\" FROM account a WHERE a.email = ?",
                Dsl.PSC.selectFrom(JavadocAccount.class, "a").where(Filters.eq("a.email", "john.doe@example.com")).build().query());
    }

    /** Minimal entity mirroring the Account shape used by the Dsl entity-class SELECT Javadoc examples. */
    @Table(name = "account")
    public static class JavadocAccount {
        private long id;
        private String firstName;
        private String lastName;
        private String email;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Test entity classes
    @Table(name = "test_account")
    public static class Account {
        @Id
        private long id;
        private String firstName;
        private String lastName;
        private String email;
        @NonUpdatable
        private Date createdDate;
        @ReadOnly
        private Date lastModifiedDate;
        @Column("account_status")
        private String status;
        private int age;
        private BigDecimal balance;

        // Getters and setters
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

        public Date getLastModifiedDate() {
            return lastModifiedDate;
        }

        public void setLastModifiedDate(Date lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }

    @Table(name = "user_order", alias = "o")
    public static class Order {
        private long id;
        private long userId;
        private String orderNumber;
        private BigDecimal amount;
        private Date orderDate;

        // Getters and setters
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }
    }

    // Static method tests

    @Test
    public void testGetTableName() {
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.CAMEL_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.NO_CHANGE));

        assertEquals("user_order", AbstractQueryBuilder.getTableName(Order.class, NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testGetTableAlias() {
        assertEquals("", AbstractQueryBuilder.tableAlias(Account.class));
        assertEquals("o", AbstractQueryBuilder.tableAlias(Order.class));
    }

    @Test
    public void testGetTableAliasWithSpecifiedAlias() {
        assertEquals("a", AbstractQueryBuilder.tableAlias("a", Account.class));
        assertEquals("", AbstractQueryBuilder.tableAlias("", Account.class));
        assertEquals("", AbstractQueryBuilder.tableAlias(null, Account.class));
    }

    @Test
    public void testGetTableAliasOrName() {
        assertEquals("test_account", AbstractQueryBuilder.tableAliasOrName(Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("o", AbstractQueryBuilder.tableAliasOrName(Order.class, NamingPolicy.SNAKE_CASE));

        assertEquals("custom", AbstractQueryBuilder.tableAliasOrName("custom", Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("o", AbstractQueryBuilder.tableAliasOrName("", Order.class, NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testIsDefaultIdPropValue() {
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(null));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0L));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(BigInteger.ZERO));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(new BigDecimal(0)));

        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(1));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(-1));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue("0"));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(""));
    }

    @Test
    public void testLoadPropNamesByClass() {
        Set<String>[] propNames = AbstractQueryBuilder.loadPropNamesByClass(Account.class);

        assertNotNull(propNames);
        assertEquals(5, propNames.length);

        // propNames[0] - for select, including sub entity properties
        assertTrue(propNames[0].contains("firstName"));
        assertTrue(propNames[0].contains("lastName"));
        assertTrue(propNames[0].contains("email"));
        assertTrue(propNames[0].contains("status"));

        // propNames[1] - for select, no sub entity properties
        assertTrue(propNames[1].contains("firstName"));

        // propNames[2] - for insert with id
        assertTrue(propNames[2].contains("id"));
        assertTrue(propNames[2].contains("createdDate")); // @NonUpdatable
        assertFalse(propNames[2].contains("lastModifiedDate")); // @ReadOnly

        // propNames[3] - for insert without id
        assertFalse(propNames[3].contains("id")); // ID should be excluded

        // propNames[4] - for update
        assertFalse(propNames[4].contains("createdDate")); // @NonUpdatable
        assertFalse(propNames[4].contains("lastModifiedDate")); // @ReadOnly
    }

    @Test
    public void testGetSubEntityPropNames() {
        ImmutableSet<String> subProps = AbstractQueryBuilder.getSubEntityPropNames(Account.class);
        assertNotNull(subProps);
        assertTrue(subProps.isEmpty()); // Account has no sub-entities
    }

    @Test
    public void testNamed() {
        Map<String, SqlExpression> result = AbstractQueryBuilder.namedPlaceholders("firstName", "lastName");
        assertEquals(2, result.size());
        assertEquals(Filters.QME, result.get("firstName"));
        assertEquals(Filters.QME, result.get("lastName"));

        List<String> propList = Arrays.asList("email", "status");
        result = AbstractQueryBuilder.namedPlaceholders(propList);
        assertEquals(2, result.size());
        assertEquals(Filters.QME, result.get("email"));
        assertEquals(Filters.QME, result.get("status"));
    }

    @Test
    public void testNamedParameterHandlerIsScopedToDialect() {
        final BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("${").append(propName).append("}");
        final Dsl customDsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler(customHandler).build());

        String sql = customDsl.select("name").from("users").where(Filters.eq("id", 1)).build().query();
        assertEquals("SELECT name FROM users WHERE id = ${id}", sql);

        sql = customDsl.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("${firstName}"));
        assertTrue(sql.contains("${id}"));

        // The predefined DSL remains unchanged.
        sql = Dsl.NSC.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains(":firstName"));
        assertTrue(sql.contains(":id"));
    }

    @Test
    public void testNullNamedParameterHandlerUsesDefault() {
        final Dsl dsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler(null).build());
        assertEquals("SELECT name FROM users WHERE id = :id", dsl.select("name").from("users").where(Filters.eq("id", 1)).build().query());
    }

    @Test
    public void testNamedParameterHandlerMustEmitToken() {
        final Dsl dsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
            // Deliberately emits nothing.
        }).build());

        assertThrows(IllegalStateException.class, () -> dsl.select("name").from("users").where(Filters.eq("id", 1)).build());
    }

    @Test
    public void testNamedParameterHandlerPropagatesIntoStructuredSubQuery() {
        final Dsl dsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("userId"), Filters.eq("status", "OPEN"));
        final SP sp = dsl.select("id").from("users").where(Filters.in("id", subQuery)).build();

        assertEquals("SELECT id FROM users WHERE id IN (SELECT user_id AS \"userId\" FROM orders WHERE status = ${status})", sp.query());
        assertEquals(Arrays.asList("OPEN"), sp.parameters());
    }

    // Instance method tests using PSC (Parameterized SQL with snake_case)

    @Test
    public void testInto() {
        String sql = Dsl.PSC.insert("firstName", "lastName").into("account").build().query();

        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?)", sql);

        // Test with entity class
        sql = Dsl.PSC.insert("firstName", "lastName").into(Account.class).build().query();

        assertEquals("INSERT INTO test_account (first_name, last_name) VALUES (?, ?)", sql);
    }

    @Test
    public void testIntoWithEntityAndTableName() {
        String sql = Dsl.PSC.insert("firstName", "lastName").into("custom_table", Account.class).build().query();

        assertEquals("INSERT INTO custom_table (first_name, last_name) VALUES (?, ?)", sql);
    }

    //    @Test
    //    public void testIntoWithInvalidOperation() {
    //        SqlBuilder builder = PSC.select("*").from("account");
    //        assertThrows(RuntimeException.class, () -> builder.into("account"));
    //    }

    @Test
    public void testIntoWithoutColumns() {
        assertThrows(RuntimeException.class, () -> Dsl.PSC.update("account").into("account"));
    }

    @Test
    public void testPreselect() {
        String sql = Dsl.PSC.select("*").selectModifier("TOP 10").from("account").build().query();

        assertEquals("SELECT TOP 10 * FROM account", sql);

        // Test duplicate selectModifier
        SqlBuilder builder = Dsl.PSC.select("*").selectModifier("TOP 10");
        assertThrows(IllegalStateException.class, () -> builder.selectModifier("DISTINCT"));
    }

    @Test
    public void testFrom() {
        // Single table
        String sql = Dsl.PSC.select("*").from("users").build().query();
        assertEquals("SELECT * FROM users", sql);

        // Multiple tables
        sql = Dsl.PSC.select("*").from(Array.of("users", "orders")).build().query();
        assertEquals("SELECT * FROM users, orders", sql);

        // Collection of tables
        sql = Dsl.PSC.select("*").from(Arrays.asList("users", "orders", "products")).build().query();
        assertEquals("SELECT * FROM users, orders, products", sql);

        // With alias
        sql = Dsl.PSC.select("*").from("users u").build().query();
        assertEquals("SELECT * FROM users u", sql);

        // With entity class
        sql = Dsl.PSC.select("*").from(Account.class).build().query();
        assertEquals("SELECT * FROM test_account", sql);

        // With entity class and alias
        sql = Dsl.PSC.select("*").from(Account.class, "a").build().query();
        assertEquals("SELECT * FROM test_account a", sql);
    }

    @Test
    public void testFromRejectsNullOrBlankTableNames() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from(new String[] { "users", null }));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from(new String[] { "users", "   " }));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from(Arrays.asList("users", null)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from(Arrays.asList("users", "  ")));
    }

    @Test
    public void testUpdateAndDeleteRejectBlankTableNames() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("   ", Account.class));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.deleteFrom("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.deleteFrom("   ", Account.class));
    }

    @Test
    public void testFromWithExpression() {
        String sql = Dsl.PSC.select("*").from("(SELECT * FROM users) t").build().query();
        assertEquals("SELECT * FROM (SELECT * FROM users) t", sql);

        sql = Dsl.PSC.select("*").from("users u, orders o").build().query();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromWithAsAliasAndEntityClass() {
        String sql = Dsl.PSC.select("firstName").from("test_account AS a", Account.class).build().query();
        assertTrue(sql.contains("a.first_name"));
        assertTrue(sql.contains("FROM test_account AS a"));
    }

    @Test
    public void testFromWithoutSelect() {
        assertThrows(RuntimeException.class, () -> Dsl.PSC.update("account").from("account"));
    }

    @Test
    public void testJoin() {
        String sql = Dsl.PSC.select("*").from("users u").join("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With entity class
        sql = Dsl.PSC.select("*").from(Account.class, "a").join(Order.class, "o").on("a.id = o.user_id").build().query();

        assertEquals("SELECT * FROM test_account a JOIN user_order o ON a.id = o.user_id", sql);
    }

    @Test
    public void testNaturalJoin() {
        String sql = Dsl.PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertEquals("SELECT * FROM users NATURAL JOIN orders", sql);
    }

    @Test
    public void testOn() {
        String sql = Dsl.PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With condition
        sql = Dsl.PSC.select("*").from("users u").join("orders o").on(Filters.eq("u.id", "o.user_id")).build().query();

        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testUsing() {
        String sql = Dsl.PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertEquals("SELECT * FROM users JOIN orders USING (user_id)", sql);
    }

    @Test
    public void testWhere() {
        String sql = Dsl.PSC.select("*").from("users").where("age > 18").build().query();

        assertEquals("SELECT * FROM users WHERE age > 18", sql);

        // With condition
        sql = Dsl.PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();

        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    }

    @Tag("2025")
    @Test
    public void testBinaryWithInOperatorRendersAsInClause() {
        // A Binary built via Filters.binary(prop, Operator.IN, collection) must render as a real IN clause
        // (col IN (?, ?, ...)) -- identical to In/NotIn and Binary.toString() -- rather than binding the whole
        // collection as a single parameter (regression: previously produced "status IN ?" bound to the list).
        final SP binaryIn = Dsl.PSC.select("*").from("users").where(Filters.binary("status", Operator.IN, N.asList("A", "B", "C"))).build();
        final SP realIn = Dsl.PSC.select("*").from("users").where(Filters.in("status", N.asList("A", "B", "C"))).build();

        assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", binaryIn.query());
        assertEquals(realIn.query(), binaryIn.query());
        assertEquals(Arrays.asList("A", "B", "C"), binaryIn.parameters());

        // NOT IN behaves the same way.
        final SP binaryNotIn = Dsl.PSC.select("*").from("users").where(Filters.binary("status", Operator.NOT_IN, N.asList("A", "B"))).build();

        assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", binaryNotIn.query());
        assertEquals(Arrays.asList("A", "B"), binaryNotIn.parameters());
    }

    @Test
    public void testGroupByAsc() {
        String sql = Dsl.PSC.select("category", "COUNT(*)").from("products").groupByAsc("category").build().query();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category ASC", sql);

        sql = Dsl.PSC.select("category", "brand", "COUNT(*)").from("products").groupByAsc("category", "brand").build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);

        sql = Dsl.PSC.select("category", "brand", "COUNT(*)").from("products").groupByAsc(Arrays.asList("category", "brand")).build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);
    }

    @Test
    public void testGroupByDesc() {
        String sql = Dsl.PSC.select("category", "COUNT(*)").from("products").groupByDesc("category").build().query();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category DESC", sql);

        sql = Dsl.PSC.select("category", "brand", "COUNT(*)").from("products").groupByDesc("category", "brand").build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);

        sql = Dsl.PSC.select("category", "brand", "COUNT(*)").from("products").groupByDesc(Arrays.asList("category", "brand")).build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);
    }

    @Test
    public void testOffsetRows() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchNextNRowsOnly() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testExplicitFetchCannotBeCombinedWithAnotherRowLimitOrTrailingOffset() {
        SqlBuilder fetchFirst = Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10);
        assertThrows(IllegalStateException.class, () -> fetchFirst.limit(5));
        assertThrows(IllegalStateException.class, () -> fetchFirst.offset(20));
        assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", fetchFirst.build().query());

        SqlBuilder limit = Dsl.PSC.select("*").from("users").orderBy("id").limit(10);
        assertThrows(IllegalStateException.class, () -> limit.fetchNextRows(5));
        assertEquals("SELECT * FROM users ORDER BY id LIMIT 10", limit.build().query());

        SqlBuilder limitStyleOffset = Dsl.PSC.select("*").from("users").orderBy("id").offset(20);
        assertThrows(IllegalStateException.class, () -> limitStyleOffset.fetchNextRows(10));
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20", limitStyleOffset.build().query());

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
                Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build().query());
    }

    @Test
    public void testRejectedCombinedLimitDoesNotReserveLimitSlot() {
        final SqlBuilder builder = Dsl.PSC.select("*").from("users").offset(20);

        assertThrows(IllegalStateException.class, () -> builder.limit(10, 30));
        assertFalse(builder.calledOpSet.contains("LIMIT"));
        assertEquals("SELECT * FROM users OFFSET 20", builder.build().query());
    }

    @Test
    public void testUnresolvedCommaLimitConsumesOffsetSlot() {
        final SqlBuilder builder = Dsl.PSC.select("*").from("users").append(new Limit("?, ?"));

        assertThrows(IllegalStateException.class, () -> builder.offset(20));
        assertEquals("SELECT * FROM users LIMIT ?, ?", builder.build().query());
    }

    @Test
    public void testUnresolvedFetchLimitClosesTrailingOffsetSlot() {
        final SqlBuilder builder = Dsl.PSC.select("*").from("users").append(new Limit("FETCH FIRST ? ROWS ONLY"));

        assertThrows(IllegalStateException.class, () -> builder.offset(20));
        assertEquals("SELECT * FROM users FETCH FIRST ? ROWS ONLY", builder.build().query());
    }

    @Test
    public void testUnresolvedFetchLimitCanFollowOffsetRows() {
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT ? ROWS ONLY",
                Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).append(new Limit("FETCH NEXT ? ROWS ONLY")).build().query());

        final Criteria criteria = Criteria.builder().limit("FETCH NEXT ? ROWS ONLY").build();
        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT ? ROWS ONLY",
                Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).append(criteria).build().query());
    }

    @Test
    public void testUnresolvedLimitPlaceholderNamesDoNotConsumePaginationSlots() {
        assertEquals("SELECT * FROM users LIMIT :OFFSET OFFSET 20", Dsl.PSC.select("*").from("users").append(new Limit(":OFFSET")).offset(20).build().query());
        assertEquals("SELECT * FROM users LIMIT :FETCH OFFSET 20", Dsl.PSC.select("*").from("users").append(new Limit(":FETCH")).offset(20).build().query());
    }

    @Test
    public void testAppend() {
        // With condition
        String sql = Dsl.PSC.select("*").from("users").append(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65))).build().query();

        assertEquals("SELECT * FROM users WHERE (age > ?) AND (age < ?)", sql);

        // With string (leading space in the argument)
        sql = Dsl.PSC.select("*").from("users").append(" FOR UPDATE").build().query();

        assertEquals("SELECT * FROM users FOR UPDATE", sql);

        // Without a leading space: a separating space is inserted automatically.
        sql = Dsl.PSC.select("*").from("users").append("FOR UPDATE").build().query();

        assertEquals("SELECT * FROM users FOR UPDATE", sql);

        // Chained appends never produce a doubled space regardless of leading spaces.
        sql = Dsl.PSC.select("*").from("users").append("WHERE 1=1").append(" AND status = 'x'").append("ORDER BY name").build().query();

        assertEquals("SELECT * FROM users WHERE 1=1 AND status = 'x' ORDER BY name", sql);
    }

    @Test
    public void testUnionAll() {
        SqlBuilder query1 = Dsl.PSC.select("id", "name").from("users");
        SqlBuilder query2 = Dsl.PSC.select("id", "name").from("customers");

        String sql = query1.unionAll(query2).build().query();
        assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);

        SqlBuilder self = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.unionAll(self));
    }

    @Test
    public void testIntersect() {
        SqlBuilder query1 = Dsl.PSC.select("id", "name").from("users");
        SqlBuilder query2 = Dsl.PSC.select("id", "name").from("customers");

        String sql = query1.intersect(query2).build().query();
        assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);

        SqlBuilder self = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.intersect(self));
    }

    @Test
    public void testExcept() {
        SqlBuilder query1 = Dsl.PSC.select("id", "name").from("users");
        SqlBuilder query2 = Dsl.PSC.select("id", "name").from("customers");

        String sql = query1.except(query2).build().query();
        assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);

        SqlBuilder self = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.except(self));
    }

    @Test
    public void testMinus() {
        SqlBuilder query1 = Dsl.PSC.select("id", "name").from("users");
        SqlBuilder query2 = Dsl.PSC.select("id", "name").from("customers");

        String sql = query1.minus(query2).build().query();
        assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);

        SqlBuilder self = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.minus(self));
    }

    @Test
    @Tag("2025")
    public void testSetOperationBuildersKeepNamedParameterNamesUnique() {
        assertEquals("SELECT id FROM users WHERE status = :status UNION SELECT id FROM archived_users WHERE status = :status_2",
                Dsl.NSC.select("id")
                        .from("users")
                        .where(Filters.eq("status", "ACTIVE"))
                        .union(Dsl.NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                        .build()
                        .query());

        assertEquals("SELECT id FROM users WHERE status = :status UNION ALL SELECT id FROM archived_users WHERE status = :status_2",
                Dsl.NSC.select("id")
                        .from("users")
                        .where(Filters.eq("status", "ACTIVE"))
                        .unionAll(Dsl.NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                        .build()
                        .query());

        assertEquals("SELECT id FROM users WHERE status = :status INTERSECT SELECT id FROM archived_users WHERE status = :status_2",
                Dsl.NSC.select("id")
                        .from("users")
                        .where(Filters.eq("status", "ACTIVE"))
                        .intersect(Dsl.NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                        .build()
                        .query());

        assertEquals("SELECT id FROM users WHERE status = :status EXCEPT SELECT id FROM archived_users WHERE status = :status_2",
                Dsl.NSC.select("id")
                        .from("users")
                        .where(Filters.eq("status", "ACTIVE"))
                        .except(Dsl.NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                        .build()
                        .query());

        assertEquals("SELECT id FROM users WHERE status = :status MINUS SELECT id FROM archived_users WHERE status = :status_2",
                Dsl.NSC.select("id")
                        .from("users")
                        .where(Filters.eq("status", "ACTIVE"))
                        .minus(Dsl.NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                        .build()
                        .query());
    }

    @Test
    public void testSetOperationRenamesCustomNamedParameterTokensWithoutCorruptingWrapperText() {
        final Dsl customDsl = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("CAST(:").append(name).append(" AS uuid)")).build());
        final SP sp = customDsl.select("id")
                .from("users")
                .where(Filters.equal("id", 1))
                .union(customDsl.select("id").from("archived_users").where(Filters.equal("id", 2)))
                .build();

        assertEquals("SELECT id FROM users WHERE id = CAST(:id AS uuid) UNION SELECT id FROM archived_users WHERE id = CAST(:id_2 AS uuid)", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    @Test
    public void testSetOperationNormalizesChildNamedParametersToParentHandler() {
        final Dsl parentDsl = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final SP sp = parentDsl.select("id")
                .from("users")
                .where(Filters.equal("id", 1))
                .union(Dsl.NSC.select("id").from("archived_users").where(Filters.equal("id", 2)))
                .build();

        assertEquals("SELECT id FROM users WHERE id = ${id} UNION SELECT id FROM archived_users WHERE id = ${id_2}", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    @Test
    public void testSetOperationRejectsDifferentGeneratedNamedParameterPolicies() {
        final SqlBuilder parent = Dsl.NSC.select("id").from("users").where(Filters.equal("id", 1));
        final SqlBuilder child = Dsl.MSC.select("id").from("archived_users").where(Filters.equal("id", 2));

        assertThrows(IllegalArgumentException.class, () -> parent.union(child));
    }

    @Test
    public void testUnionAll_StringOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").unionAll("SELECT id, name FROM customers").build().query();
        assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);
    }

    @Test
    public void testUnionAll_CollectionOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").unionAll(Arrays.asList("id", "name")).from("customers").build().query();
        assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);
    }

    @Test
    public void testIntersect_StringOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").intersect("SELECT id, name FROM customers").build().query();
        assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testIntersect_CollectionOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").intersect(Arrays.asList("id", "name")).from("customers").build().query();
        assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testExcept_StringOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").except("SELECT id, name FROM customers").build().query();
        assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testExcept_CollectionOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").except(Arrays.asList("id", "name")).from("customers").build().query();
        assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testMinus_StringOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").minus("SELECT id, name FROM customers").build().query();
        assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);
    }

    @Test
    public void testMinus_CollectionOverload() {
        String sql = Dsl.PSC.select("id", "name").from("users").minus(Arrays.asList("id", "name")).from("customers").build().query();
        assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);
    }

    @Test
    public void testGroupBy_CollectionWithDirection() {
        String sql = Dsl.PSC.select("category", "brand", "COUNT(*)")
                .from("products")
                .groupBy(Arrays.asList("category", "brand"), SortDirection.DESC)
                .build()
                .query();
        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);
    }

    @Test
    public void testGroupBy_CollectionWithAscDirection() {
        String sql = Dsl.PSC.select("category", "brand", "COUNT(*)")
                .from("products")
                .groupBy(Arrays.asList("category", "brand"), SortDirection.ASC)
                .build()
                .query();
        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);
    }

    @Test
    public void testOrderBy_CollectionWithDirection() {
        String sql = Dsl.PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName"), SortDirection.DESC).build().query();
        assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);
    }

    @Test
    public void testOrderBy_CollectionWithAscDirection() {
        String sql = Dsl.PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName"), SortDirection.ASC).build().query();
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testAppendIf_ConditionTrue() {
        String sql = Dsl.PSC.select("*").from("users").appendIf(true, Filters.gt("age", 18)).build().query();
        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    }

    @Test
    public void testAppendIf_ConditionFalse() {
        String sql = Dsl.PSC.select("*").from("users").appendIf(false, Filters.gt("age", 18)).build().query();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testAppendIf_StringTrue() {
        String sql = Dsl.PSC.select("*").from("users").appendIf(true, " FOR UPDATE").build().query();
        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    }

    @Test
    public void testAppendIf_StringFalse() {
        String sql = Dsl.PSC.select("*").from("users").appendIf(false, " FOR UPDATE").build().query();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testAppendIfRejectsClosedBuilderForBothBranchesAndOverloads() {
        SqlBuilder conditionBuilder = Dsl.PSC.select("*").from("users");
        conditionBuilder.build();

        assertThrows(IllegalStateException.class, () -> conditionBuilder.appendIf(false, Filters.eq("id", 1)));
        assertThrows(IllegalStateException.class, () -> conditionBuilder.appendIf(true, Filters.eq("id", 1)));

        SqlBuilder expressionBuilder = Dsl.PSC.select("*").from("users");
        expressionBuilder.build();

        assertThrows(IllegalStateException.class, () -> expressionBuilder.appendIf(false, "FOR UPDATE"));
        assertThrows(IllegalStateException.class, () -> expressionBuilder.appendIf(true, "FOR UPDATE"));
    }

    @Test
    public void testAppendIfOrElse_ConditionTrue() {
        String sql = Dsl.PSC.select("*").from("users").appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build().query();
        assertEquals("SELECT * FROM users WHERE status = ?", sql);
        assertEquals("active",
                Dsl.PSC.select("*")
                        .from("users")
                        .appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive"))
                        .build()
                        .parameters()
                        .get(0));
    }

    @Test
    public void testAppendIfOrElse_ConditionFalse() {
        SP sp = Dsl.PSC.select("*").from("users").appendIfOrElse(false, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build();
        assertEquals("SELECT * FROM users WHERE status = ?", sp.query());
        assertEquals("inactive", sp.parameters().get(0));
    }

    @Test
    public void testAppendIfOrElse_StringTrue() {
        String sql = Dsl.PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);
    }

    @Test
    public void testAppendIfOrElse_StringFalse() {
        String sql = Dsl.PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        assertEquals("SELECT * FROM users ORDER BY name DESC", sql);
    }

    @Test
    public void testAccept_Consumer() {
        final List<String> captured = new ArrayList<>();
        Dsl.PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).accept(sp -> {
            captured.add(sp.query());
            captured.add(sp.parameters().toString());
        });
        assertEquals(2, captured.size());
        assertEquals("SELECT * FROM account WHERE status = ?", captured.get(0));
        assertEquals("[ACTIVE]", captured.get(1));
    }

    @Test
    public void testAccept_BiConsumer() {
        final List<String> captured = new ArrayList<>();
        Dsl.PSC.select("*").from("account").where(Filters.eq("id", 1)).accept((sql, params) -> {
            captured.add(sql);
            captured.add(String.valueOf(params.size()));
        });
        assertEquals(2, captured.size());
        assertEquals("SELECT * FROM account WHERE id = ?", captured.get(0));
        assertEquals("1", captured.get(1));
    }

    @Test
    public void testForUpdate() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

        assertEquals("SELECT * FROM users WHERE id = ? FOR UPDATE", sql);
    }

    @Test
    public void testSet() {
        // With expression
        String sql = Dsl.PSC.update("users").set("name = 'John'").where(Filters.eq("id", 1)).build().query();

        assertEquals("UPDATE users SET name = 'John' WHERE id = ?", sql);

        // With columns
        sql = Dsl.PSC.update("users").set("firstName", "lastName", "email").where(Filters.eq("id", 1)).build().query();

        assertEquals("UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?", sql);

        // With collection
        sql = Dsl.PSC.update("users").set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).build().query();

        assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

        // With map
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");

        sql = Dsl.PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

        assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

        // With entity
        Account account = new Account();
        account.setFirstName("John");
        account.setLastName("Doe");

        sql = Dsl.PSC.update("account").set(account).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("WHERE id = ?"));

        // With entity class
        sql = Dsl.PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("WHERE id = ?"));

        // With excluded properties
        Set<String> excluded = N.asSet("lastModifiedDate");
        sql = Dsl.PSC.update("account").set(account, excluded).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertFalse(sql.contains("last_modified_date"));

        sql = Dsl.PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testSql() {
        String sql = Dsl.PSC.select("id", "name").from("account").where(Filters.gt("age", 18)).build().query();

        assertEquals("SELECT id, name FROM account WHERE age > ?", sql);

        // Test double call throws exception
        SqlBuilder builder = Dsl.PSC.select("*").from("users");
        builder.build().query();
        assertThrows(RuntimeException.class, () -> builder.build().query());
    }

    @Test
    public void testParameters() {
        SqlBuilder builder = Dsl.PSC.select("*").from("account").where(Filters.eq("name", "John").and(Filters.gt("age", 25)));

        SP sp = builder.build();
        List<Object> params = sp.parameters();

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testPair() {
        SqlBuilder.SP sp = Dsl.PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).build();

        assertEquals("SELECT * FROM account WHERE status = ?", sp.query());
        assertEquals(1, sp.parameters().size());
        assertEquals("ACTIVE", sp.parameters().get(0));
    }

    @Test
    public void testApplyFunction() throws Exception {
        List<String> result = Dsl.PSC.select("*")
                .from("account")
                .where(Filters.eq("status", "ACTIVE"))
                .apply(sp -> Arrays.asList(sp.query(), sp.parameters().toString()));

        assertEquals(2, result.size());
        assertEquals("SELECT * FROM account WHERE status = ?", result.get(0));
        assertEquals("[ACTIVE]", result.get(1));
    }

    @Test
    public void testApplyBiFunction() throws Exception {
        String result = Dsl.PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).apply((sql, params) -> sql + " - " + params.size());

        assertEquals("SELECT * FROM account WHERE status = ? - 1", result);
    }

    @Test
    public void testDebugPrint() {
        SqlBuilder builder = Dsl.PSC.select("*").from("account").where(Filters.between("age", 18, 65));
        builder.debugPrint();
        assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
    }

    @Test
    public void testToString() {
        String sql = Dsl.PSC.select("*").from("account").where(Filters.eq("id", 1)).toString();

        assertNotEquals("SELECT * FROM account WHERE id = ?", sql);
    }

    @Test
    public void testIsNamedSql() {
        assertFalse(Dsl.PSC.select("*").from("account").isNamedSql());
        assertTrue(Dsl.NSC.select("*").from("account").isNamedSql());
    }

    @Test
    public void testComplexQuery() {
        String sql = Dsl.PSC.select("u.id", "u.name", "o.order_number")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.gt("u.age", 18).and(Filters.eq("u.status", "ACTIVE")))
                .groupBy("u.id", "u.name", "o.order_number")
                .having(Filters.gt("COUNT(*)", 1))
                .orderBy("u.name", SortDirection.ASC)
                .limit(20, 10)
                .build()
                .query();

        assertTrue(sql.contains("SELECT u.id, u.name, o.order_number"));
        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o"));
        assertTrue(sql.contains("ON u.id = o.user_id"));
        assertTrue(sql.contains("WHERE (u.age > ?) AND (u.status = ?)"));
        assertTrue(sql.contains("GROUP BY u.id, u.name, o.order_number"));
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
        assertTrue(sql.contains("ORDER BY u.name ASC"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    // Test different naming policies
    @Test
    public void testNamingPolicies() {
        // Snake case (PSC)
        String sql = Dsl.PSC.select("firstName", "lastName").from("userAccount").build().query();
        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
        assertTrue(sql.contains("userAccount"));

        // Upper case (PAC)
        sql = Dsl.PAC.select("firstName", "lastName").from("userAccount").build().query();
        assertTrue(sql.contains("FIRST_NAME"));
        assertTrue(sql.contains("LAST_NAME"));
        assertTrue(sql.contains("userAccount"));

        // Lower camel case (PLC)
        sql = Dsl.PLC.select("first_name", "last_name").from("user_account").build().query();
        assertTrue(sql.contains("firstName"));
        assertTrue(sql.contains("lastName"));
        assertTrue(sql.contains("user_account"));
    }

    // Test SQL policies
    @Test
    public void testSQLPolicies() {
        // Parameterized SQL
        String sql = Dsl.PSC.update("account").set("name").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("id = ?"));

        // Named SQL
        sql = Dsl.NSC.update("account").set("name").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("name = :name"));
        assertTrue(sql.contains("id = :id"));
    }

    @Test
    public void testJoinWithEntityClasses() {
        String sql = Dsl.PSC.select("*").from(Account.class, "a").join(Order.class, "o").on(Filters.eq("a.id", "o.userId")).build().query();

        assertTrue(sql.contains("FROM test_account a"));
        assertTrue(sql.contains("JOIN user_order o"));
    }

    @Test
    public void testConditionsWithAnd() {
        String sql = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65), Filters.eq("status", "ACTIVE")))
                .build()
                .query();

        assertTrue(sql.contains("WHERE (age > ?) AND (age < ?) AND (status = ?)"));
    }

    @Test
    public void testConditionsWithOr() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.or(Filters.eq("status", "ACTIVE"), Filters.eq("status", "PENDING"))).build().query();

        assertTrue(sql.contains("WHERE (status = ?) OR (status = ?)"));
    }

    @Test
    public void testBetweenCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

        assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", sql);
    }

    @Test
    public void testNotBetweenCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.notBetween("age", 18, 65)).build().query();

        assertEquals("SELECT * FROM users WHERE age NOT BETWEEN ? AND ?", sql);
    }

    /**
     * Regression test: when a table-aliased property like "ord.orderDate" is used with
     * BETWEEN under a named-SQL builder (NSC/NSB/NAC/NLC), the synthesized "min"/"max"
     * parameter names previously contained an embedded dot (":minOrd.orderDate"), which
     * most named-parameter parsers (Spring's NamedParameterJdbcTemplate, MyBatis, etc.)
     * reject as invalid identifiers. The fix strips the alias prefix so the placeholder
     * becomes ":minOrderDate" / ":maxOrderDate".
     */
    @Test
    public void testNamedBetweenWithTableAliasedColumn_noDotInParameterName() {
        // NSC: snake_case + ":name" placeholders.
        String sql = Dsl.NSC.select("*").from("orders ord").where(Filters.between("ord.orderDate", "2023-01-01", "2023-12-31")).build().query();
        assertTrue(sql.contains(":minOrderDate"), sql);
        assertTrue(sql.contains(":maxOrderDate"), sql);
        // The buggy form with a dot inside the parameter name must not be present.
        assertFalse(sql.contains(":minOrd.orderDate"), sql);
        assertFalse(sql.contains(":maxOrd.orderDate"), sql);

        // NotBetween must follow the same rule.
        String sql2 = Dsl.NSC.select("*").from("orders ord").where(Filters.notBetween("ord.orderDate", "2023-01-01", "2023-12-31")).build().query();
        assertTrue(sql2.contains(":minOrderDate"), sql2);
        assertTrue(sql2.contains(":maxOrderDate"), sql2);
        assertFalse(sql2.contains(":minOrd.orderDate"), sql2);

        // Same rule for iBATIS-style placeholders generated by MSC.
        String sql3 = Dsl.MSC.select("*").from("orders ord").where(Filters.between("ord.orderDate", "a", "b")).build().query();
        assertTrue(sql3.contains("#{minOrderDate}"), sql3);
        assertTrue(sql3.contains("#{maxOrderDate}"), sql3);
        assertFalse(sql3.contains("#{minOrd.orderDate}"), sql3);
    }

    /**
     * Regression test: a table-aliased property used with a simple equality / LIKE under a
     * named- or iBATIS-style SQL builder must produce a valid placeholder identifier (i.e.
     * the alias prefix must be stripped). Previously, {@code Filters.eq("u.id", ...)}
     * generated {@code :u.id} which is not a valid named parameter.
     */
    @Test
    public void testNamedEqualWithTableAliasedColumn_noDotInParameterName() {
        // NSC (named SQL, snake_case): the placeholder must be ":id", not ":u.id".
        String sql = Dsl.NSC.select("*").from("users u").where(Filters.eq("u.id", 1L)).build().query();
        assertTrue(sql.contains(":id"), sql);
        assertFalse(sql.contains(":u.id"), sql);

        // MSC (iBATIS SQL, snake_case): the placeholder must be "#{id}", not "#{u.id}".
        String sql2 = Dsl.MSC.select("*").from("users u").where(Filters.eq("u.id", 1L)).build().query();
        assertTrue(sql2.contains("#{id}"), sql2);
        assertFalse(sql2.contains("#{u.id}"), sql2);
    }

    /**
     * Regression test: a table-aliased property used with IN/NOT IN under a named-style
     * SQL builder must produce valid placeholder identifiers (no embedded dots).
     */
    @Test
    public void testNamedInWithTableAliasedColumn_noDotInParameterName() {
        String sql = Dsl.NSC.select("*").from("users u").where(Filters.in("u.status", Arrays.asList("A", "B"))).build().query();
        // Each IN element must produce ":status1", ":status2" — never ":u.status1".
        assertFalse(sql.contains(":u.status"), sql);
        assertTrue(sql.contains(":status1"), sql);
        assertTrue(sql.contains(":status2"), sql);

        String sql2 = Dsl.NSC.select("*").from("users u").where(Filters.notIn("u.status", Arrays.asList("X", "Y"))).build().query();
        assertFalse(sql2.contains(":u.status"), sql2);
        assertTrue(sql2.contains(":status1"), sql2);
        assertTrue(sql2.contains(":status2"), sql2);
    }

    @Test
    public void testInCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING", "APPROVED"))).build().query();

        assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", sql);
    }

    @Test
    public void testNotInCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.notIn("status", Arrays.asList("DELETED", "BANNED"))).build().query();

        assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", sql);
    }

    @Test
    public void testMultiColumnInCondition_Parameterized() {
        AbstractQueryBuilder.SP sp = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe"))))
                .build();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?), (?, ?))", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), sp.parameters());
    }

    @Test
    public void testMultiColumnNotInCondition_Parameterized() {
        String sql = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.notIn(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"))))
                .build()
                .query();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) NOT IN ((?, ?))", sql);
    }

    @Test
    public void testMultiColumnInCondition_Named() {
        String sql = Dsl.NSC.select("*")
                .from("users")
                .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe"))))
                .build()
                .query();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((:firstName1, :lastName1), (:firstName2, :lastName2))", sql);
    }

    @Test
    public void testMultiColumnInCondition_Raw() {
        String sql = Dsl.SCSB.select("*")
                .from("users")
                .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"))))
                .build()
                .query();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN (('John', 'Doe'))", sql);
    }

    @Test
    public void testMultiColumnInCondition_MapRows() {
        AbstractQueryBuilder.SP sp = Dsl.PSC.select("*")
                .from("users")
                .where(Filters.in(Arrays.asList("firstName", "lastName"),
                        Arrays.asList(N.asMap("firstName", "John", "lastName", "Doe"), N.asMap("firstName", "Jane", "lastName", "Roe"))))
                .build();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?), (?, ?))", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), sp.parameters());
    }

    @Test
    public void testMultiColumnInCondition_BeanRows() {
        Account row = new Account();
        row.setFirstName("John");
        row.setLastName("Doe");

        AbstractQueryBuilder.SP sp = Dsl.PSC.select("*").from("users").where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(row))).build();

        assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?))", sp.query());
        assertEquals(Arrays.asList("John", "Doe"), sp.parameters());
    }

    @Test
    public void testIsNullCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNull("deletedDate")).build().query();

        assertEquals("SELECT * FROM users WHERE deleted_date IS NULL", sql);
    }

    @Test
    public void testIsNotNullCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();

        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", sql);
    }

    @Test
    public void testLikeCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();

        assertEquals("SELECT * FROM users WHERE name LIKE ?", sql);
    }

    @Test
    public void testNotLikeCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.notLike("email", "%@temp.com")).build().query();

        assertEquals("SELECT * FROM users WHERE email NOT LIKE ?", sql);
    }

    @Test
    public void testCriteriaCondition() {
        Criteria criteria = Criteria.builder()
                .where(Filters.gt("age", 18))
                .groupBy("status")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("status")
                .limit(10)
                .build();

        String sql = Dsl.PSC.select("status", "COUNT(*)").from("users").append(criteria).build().query();

        assertTrue(sql.contains("WHERE age > ?"));
        assertTrue(sql.contains("GROUP BY status"));
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
        assertTrue(sql.contains("ORDER BY status"));
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testCriteriaSelectModifiersAreAppliedToCurrentSelect() {
        assertEquals("SELECT DISTINCT department FROM employees",
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinct().build()).build().query());
        assertEquals("SELECT DISTINCT ON (department) department FROM employees",
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinctOn("department").build()).build().query());
        assertEquals("SELECT DISTINCTROW department FROM employees",
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinctRow().build()).build().query());
        assertEquals("SELECT DISTINCTROW(department) department FROM employees",
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinctRowBy("department").build()).build().query());
        assertEquals("SELECT SQL_CALC_FOUND_ROWS department FROM employees",
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().selectModifier("SQL_CALC_FOUND_ROWS").build()).build().query());
    }

    @Test
    public void testCriteriaSelectModifierConflictsWithExistingBuilderModifier() {
        Criteria criteria = Criteria.builder().distinct().build();

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("department").distinct().from("employees").append(criteria));
    }

    @Test
    public void testCriteriaSelectModifierRequiresCurrentSelectSegment() {
        Criteria criteria = Criteria.builder().distinct().build();

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("employees").set("department = 'sales'").append(criteria));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.deleteFrom("employees").append(criteria));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.renderCondition(Filters.equal("active", true)).append(criteria));
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("department").from("employees").union("SELECT department FROM archived_employees").append(criteria));
    }

    @Test
    public void testRejectedCriteriaDoesNotLeaveSelectModifierBehind() {
        SqlBuilder builder = Dsl.PSC.select("department").from("employees").where("department IS NULL");
        Criteria criteria = Criteria.builder().distinct().where("department IS NOT NULL").build();

        assertThrows(IllegalStateException.class, () -> builder.append(criteria));
        assertEquals("SELECT department FROM employees WHERE department IS NULL", builder.build().query());
    }

    @Test
    public void testRejectedCriteriaWithLaterClauseConflictDoesNotAppendEarlierClauses() {
        final SqlBuilder builder = Dsl.PSC.select("department").from("employees").orderBy("department");
        final Criteria criteria = Criteria.builder().where(Filters.equal("active", true)).orderBy("id").build();

        assertThrows(IllegalStateException.class, () -> builder.append(criteria));

        final SP result = builder.build();
        assertEquals("SELECT department FROM employees ORDER BY department", result.query());
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    public void testExpressionCondition() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.expr("age > 18 AND status = 'ACTIVE'")).build().query();

        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'ACTIVE'", sql);
    }

    @Test
    public void testSelectWithAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("firstName", "name");
        aliases.put("lastName", "surname");

        String sql = Dsl.PSC.select(aliases).from("users").build().query();

        assertTrue(sql.contains("first_name AS \"name\""));
        assertTrue(sql.contains("last_name AS \"surname\""));
    }

    @Test
    public void testInsertWithMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");
        values.put("email", "john@example.com");

        String sql = Dsl.PSC.insert(values).into("users").build().query();

        assertEquals("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", sql);
    }

    @Test
    public void testInsertWithEntity() {
        Account account = new Account();
        account.setId(1);
        account.setFirstName("John");
        account.setLastName("Doe");

        String sql = Dsl.PSC.insert(account).into("account").build().query();

        assertTrue(sql.contains("INSERT INTO account"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    public void testBatchInsert() {
        List<Map<String, Object>> propsList = new ArrayList<>();
        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("firstName", "John");
        map1.put("lastName", "Doe");

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("firstName", "Jane");
        map2.put("lastName", "Smith");

        propsList.add(map1);
        propsList.add(map2);

        String sql = Dsl.PSC.batchInsert(propsList).into("users").build().query();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sql);
    }

    @Test
    public void testUpdateWithoutSet() {
        // Used to emit "UPDATE users SET  WHERE id = ?" with an empty SET list; now fails fast.
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("users").where(Filters.eq("id", 1)));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("users").build());

        // update(Class) pre-stages the updatable columns, so no explicit set() is required.
        String sql = Dsl.PSC.update(Account.class).where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.startsWith("UPDATE test_account SET "));
        assertTrue(sql.contains("first_name = ?"));
        assertTrue(sql.endsWith(" WHERE id = ?"));
    }

    @Test
    public void testNamingPolicyConversion() {
        // Test normalize column name
        assertEquals("first_name", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.SNAKE_CASE));
        assertEquals("FIRST_NAME", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("firstName", AbstractQueryBuilder.normalizeColumnName("first_name", NamingPolicy.CAMEL_CASE));
        assertEquals("firstName", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.NO_CHANGE));

        // SQL keywords should not be converted
        assertEquals("SELECT", AbstractQueryBuilder.normalizeColumnName("SELECT", NamingPolicy.SNAKE_CASE));
        assertEquals("FROM", AbstractQueryBuilder.normalizeColumnName("FROM", NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testPropToColumnInfoMap() {
        ImmutableMap<String, QueryUtil.ColumnInfo> map = AbstractQueryBuilder.propToColumnInfoMap(Account.class, NamingPolicy.SNAKE_CASE);

        assertNotNull(map);
        assertTrue(map.containsKey("firstName"));
        assertTrue(map.containsKey("status"));

        assertEquals("first_name", map.get("firstName").columnName());
        assertEquals("account_status", map.get("status").columnName()); // Should use @Column annotation
    }

    @Test
    public void testNamedSQLWithParameters() {
        String sql = Dsl.NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).build().query();

        assertTrue(sql.contains("first_name = :firstName"));
        assertTrue(sql.contains("age > :age"));

        List<Object> params = Dsl.NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).parameters();

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testNamedSQLWithIn() {
        String sql = Dsl.NSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING"))).build().query();

        assertTrue(sql.contains("status IN (:status1, :status2)"));
    }

    @Test
    public void testNamedSQLWithBetween() {
        String sql = Dsl.NSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

        assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
    }

    @Test
    public void testNamedSQLWithRepeatedPropertyUsesUniqueParameterNames() {
        String sql = Dsl.NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).build().query();

        assertTrue(sql.contains("status = :status"));
        assertTrue(sql.contains("status != :status_2"));

        List<Object> params = Dsl.NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).parameters();
        assertEquals(Arrays.asList("ACTIVE", "DELETED"), params);
    }

    @Test
    public void testComplexJoinConditions() {
        String sql = Dsl.PSC.select("*")
                .from("users u")
                .leftJoin("orders o")
                .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                .build()
                .query();

        assertTrue(sql.contains("LEFT JOIN orders o ON"));
        assertTrue(sql.contains("u.id = ?"));
        assertTrue(sql.contains("o.status = ?"));
    }

    @Test
    public void testIbatisSqlPolicy() {
        SP sp = Dsl.MSC.update("users").set(Map.of("name", "Alice")).where(Filters.eq("id", 1)).build();

        assertEquals("UPDATE users SET name = #{name} WHERE id = #{id}", sp.query());
        assertEquals(Arrays.asList("Alice", 1), sp.parameters());
    }

    @Test
    public void testMultipleWhereConditions() {

        assertThrows(IllegalStateException.class, () -> {
            Dsl.PSC.select("*")
                    .from("users")
                    .where(Filters.gt("age", 18))
                    .where(Filters.eq("status", "ACTIVE"))
                    .where(Filters.like("email", "%@company.com"))
                    .build()
                    .query();
        });

    }

    @Test
    public void testUnionWithSubQuery() {
        String subQuery = "(SELECT id, name FROM archived_users WHERE status = 'INACTIVE')";

        String sql = Dsl.PSC.select("id", "name").from("users").where(Filters.eq("status", "ACTIVE")).union(subQuery).build().query();

        assertTrue(sql.contains("SELECT id, name FROM users WHERE status = ?"));
        assertTrue(sql.contains("UNION"));
        assertTrue(sql.contains(subQuery));
    }

    @Test
    public void testColumnNameWithSpecialCharacters() {
        String sql = Dsl.PSC.select("user.name", "COUNT(*) as total").from("users").groupBy("user.name").build().query();

        assertTrue(sql.contains("user.name"));
        assertTrue(sql.contains(" COUNT(*) AS total"));
        assertTrue(sql.contains("GROUP BY user.name"));
    }

    @Test
    public void testMultipleGroupByWithDifferentSortDirections() {
        String sql = Dsl.PSC.select("category", "brand", "COUNT(*)")
                .from("products")
                .groupBy(N.asMap("brand", SortDirection.DESC, "category", SortDirection.ASC))
                .build()
                .query();

        // Note: This test shows that each groupBy call replaces the previous one
        // The SQL will only contain the last GROUP BY clause
        assertTrue(sql.contains("GROUP BY brand DESC"));
    }

    @Test
    public void testLimitWithLargeNumbers() {
        String sql = Dsl.PSC.select("*").from("users").limit(50, 1000000).build().query();

        assertEquals("SELECT * FROM users LIMIT 50 OFFSET 1000000", sql);
    }

    @Test
    public void testComplexUpdateWithMultipleConditions() {
        Map<String, Object> updateValues = new LinkedHashMap<>();
        updateValues.put("status", "INACTIVE");
        updateValues.put("lastModifiedDate", new Date());

        String sql = Dsl.PSC.update("users")
                .set(updateValues)
                .where(Filters.and(Filters.lt("lastLoginDate", new Date()), Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "ACTIVE"))))
                .build()
                .query();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("status = ?"));
        assertTrue(sql.contains("last_modified_date = ?"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testExpressionWithComplexSQL() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.expr("DATEDIFF(day, created_date, GETDATE()) > 30")).build().query();

        assertEquals("SELECT * FROM users WHERE DATEDIFF(day, created_date, GETDATE()) > 30", sql);
    }

    @Test
    public void testJoinUsingMultipleColumns() {
        String sql = Dsl.PSC.select("*").from("table1").join("table2").using("(col1, col2)").build().query();

        assertEquals("SELECT * FROM table1 JOIN table2 USING (col1, col2)", sql);
    }

    @Test
    public void testSelectWithFunctions() {
        String sql = Dsl.PSC.select("MAX(age)", "MIN(age)", "AVG(age)", "COUNT(*)").from("users").groupBy("status").build().query();

        assertTrue(sql.contains("MAX(age)"));
        assertTrue(sql.contains("MIN(age)"));
        assertTrue(sql.contains("AVG(age)"));
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testHavingWithMultipleConditions() {
        String sql = Dsl.PSC.select("status", "COUNT(*) as count")
                .from("users")
                .groupBy("status")
                .having(Filters.and(Filters.gt("COUNT(*)", 10), Filters.lt("COUNT(*)", 100)))
                .build()
                .query();

        assertTrue(sql.contains("GROUP BY status"));
        assertTrue(sql.contains("HAVING (COUNT(*) > ?) AND (COUNT(*) < ?)"));
    }

    @Test
    public void testAppendWithCriteria() {
        Criteria criteria = Criteria.builder()
                .where(Filters.eq("status", "ACTIVE"))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("department")
                .limit(20, 10)
                .build();

        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").append(criteria).build().query();

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("GROUP BY department"));
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
        assertTrue(sql.contains("ORDER BY department"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    public void testWhereClause() {
        Where where = Filters.where("age > 18 AND status = 'ACTIVE'");

        String sql = Dsl.PSC.select("*").from("users").append(where).build().query();

        assertTrue(sql.contains("WHERE age > 18 AND status = 'ACTIVE'"));
    }

    @Test
    public void testHavingClause() {
        Having having = Filters.having("COUNT(*) > 10");

        String sql = Dsl.PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).build().query();

        assertTrue(sql.contains("HAVING COUNT(*) > 10"));
    }

    @Test
    public void testOrderByWithExpression() {
        String sql = Dsl.PSC.select("*").from("users").orderBy("CASE WHEN status = 'VIP' THEN 0 ELSE 1 END, name").build().query();

        assertTrue(sql.contains(" ORDER BY case when status = 'VIP' then 0 else 1 end, name"));
    }

    @Test
    public void testInsertWithExcludedProperties() {
        Account account = new Account();
        account.setId(1);
        account.setFirstName("John");
        account.setLastName("Doe");
        account.setCreatedDate(new Date());
        account.setLastModifiedDate(new Date());

        Set<String> excluded = N.asSet("createdDate", "lastModifiedDate");

        String sql = Dsl.PSC.insert(account, excluded).into("account").build().query();

        assertTrue(sql.contains("INSERT INTO account"));
        assertFalse(sql.contains("created_date"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testBatchInsertWithEntities() {
        List<Account> accounts = new ArrayList<>();

        Account account1 = new Account();
        account1.setFirstName("John");
        account1.setLastName("Doe");

        Account account2 = new Account();
        account2.setFirstName("Jane");
        account2.setLastName("Smith");

        accounts.add(account1);
        accounts.add(account2);

        String sql = Dsl.PSC.batchInsert(accounts).into("account").build().query();

        assertTrue(sql.contains("INSERT INTO account"));
        assertTrue(sql.contains("VALUES"));
        assertTrue(sql.contains("), ("));
    }

    @Test
    public void testBatchInsertWithMixedEntityTypesThrows() {
        List<Object> entities = new ArrayList<>();
        entities.add(new Account());
        entities.add(new Order());

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(entities));
    }

    @Test
    public void testBatchInsertRejectsDifferentBeanClassesWithSameProperties() {
        final BatchEntityWithId first = new BatchEntityWithId();
        first.setName("first");
        final BatchEntityWithOtherId second = new BatchEntityWithOtherId();
        second.setName("second");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(Arrays.asList(first, null, second)));

        assertTrue(ex.getMessage().contains("same runtime class"), ex.getMessage());
        assertTrue(ex.getMessage().contains(BatchEntityWithId.class.getName()), ex.getMessage());
        assertTrue(ex.getMessage().contains(BatchEntityWithOtherId.class.getName()), ex.getMessage());
    }

    @Test
    public void testBatchInsertAllowsSameBeanClassWithNullRows() {
        final BatchEntityWithId first = new BatchEntityWithId();
        first.setOtherId(10);
        first.setName("first");
        final BatchEntityWithId second = new BatchEntityWithId();
        second.setOtherId(20);
        second.setName("second");

        final SP sp = Dsl.PSC.batchInsert(Arrays.asList(first, null, second)).into("batch_entity").build();

        assertTrue(sp.query().startsWith("INSERT INTO batch_entity"), sp.query());
        assertTrue(sp.query().contains("), ("), sp.query());
        assertEquals(4, sp.parameters().size());
    }

    @Test
    public void testDeleteFromWithMultipleConditions() {
        String sql = Dsl.PSC.deleteFrom("users").where(Filters.and(Filters.eq("status", "DELETED"), Filters.lt("deletedDate", new Date()))).build().query();

        assertTrue(sql.contains("DELETE FROM users"));
        assertTrue(sql.contains("WHERE (status = ?) AND (deleted_date < ?)"));
    }

    @Test
    public void testSelectFromWithExcludedProperties() {
        String sql = Dsl.PSC.selectFrom(Account.class, N.asSet("createdDate", "lastModifiedDate")).build().query();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM test_account"));
        assertFalse(sql.contains("created_date"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testComplexQueryWithAllFeatures() {
        // Create a complex query using all major features
        String sql = Dsl.PSC.select("u.id", "u.name", "COUNT(o.id) as order_count", "SUM(o.amount) as total_amount")
                .from("users u")
                .leftJoin("orders o")
                .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                .where(Filters.and(Filters.gt("u.created_date", new Date()), Filters.in("u.status", Arrays.asList("ACTIVE", "VIP"))))
                .groupBy("u.id", "u.name")
                .having(Filters.gt("COUNT(o.id)", 5))
                .orderBy("total_amount", SortDirection.DESC)
                .limit(20, 10)
                .build()
                .query();

        // Verify all parts are present
        N.println(sql);
        assertTrue(sql.contains("SELECT u.id, u.name, COUNT(o.id) AS order_count, SUM(o.amount) AS total_amount"));
        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o"));
        assertTrue(sql.contains("ON"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY u.id, u.name"));
        assertTrue(sql.contains("HAVING COUNT(o.id) > ?"));
        assertTrue(sql.contains("ORDER BY total_amount DESC"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    public void testSqlBuilderWithDifferentNamingPolicies() {
        // Test that different SqlBuilder implementations use correct naming policies

        // Snake case
        assertTrue(Dsl.PSC.select("firstName").from("userAccount").build().query().contains("first_name"));
        assertTrue(Dsl.NSC.select("firstName").from("userAccount").build().query().contains("first_name"));

        // Upper case
        assertTrue(Dsl.PAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));
        assertTrue(Dsl.NAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));

        // Lower camel case
        assertTrue(Dsl.PLC.select("first_name").from("user_account").build().query().contains("firstName"));
        assertTrue(Dsl.NLC.select("first_name").from("user_account").build().query().contains("firstName"));
    }

    @Test
    public void testAppendWithClause() {
        // Test appending a Clause condition
        String sql = Dsl.PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(sql.contains("status = ?"));
    }

    @Test
    public void testMultipleAppendsWithConditions() {
        String sql = Dsl.PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).append(" AND age > 18").build().query();

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("AND age > 18"));
    }

    @Test
    public void testUpdateAllProperties() {
        String sql = Dsl.PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("first_name = ?"));
        assertTrue(sql.contains("last_name = ?"));
        assertFalse(sql.contains("created_date")); // @NonUpdatable
        assertFalse(sql.contains("last_modified_date")); // @ReadOnly
    }

    @Test
    public void testFormatColumnNameEdgeCases() {
        // Test edge cases for column name formatting
        assertEquals("id", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SNAKE_CASE));
        assertEquals("ID", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("user_name123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SNAKE_CASE));
        assertEquals("USER_NAME123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SCREAMING_SNAKE_CASE));
    }

    @Test
    public void testEmptyConditions() {
        // Test handling of empty conditions
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*")
                .from("users")
                .where(Filters.and()) // Empty AND
                .build()
                .query());
    }

    @Test
    public void testNullParameters() {
        String sql = Dsl.PSC.select("*").from("users").where(Filters.eq("deletedBy", null)).build().query();

        assertEquals("SELECT * FROM users WHERE deleted_by IS NULL", sql);

        SqlBuilder builder = Dsl.PSC.select("*").from("users").where(Filters.eq("deletedBy", null));
        builder.build().query();

        List<Object> params = builder.parameters();
        assertTrue(params.isEmpty());

        String sql2 = Dsl.PSC.select("*").from("users").where(Filters.ne("deletedBy", null)).build().query();
        assertEquals("SELECT * FROM users WHERE deleted_by IS NOT NULL", sql2);

        SqlBuilder builder2 = Dsl.PSC.select("*").from("users").where(Filters.ne("deletedBy", null));
        builder2.build().query();
        assertTrue(builder2.parameters().isEmpty());
    }

    @Test
    public void testCaseInsensitiveKeywords() {
        // Test that SQL keywords are preserved regardless of case
        String sql = Dsl.PSC.select("*").from("users").where(Filters.expr("select = 1 AND from = 2")).build().query();

        assertTrue(sql.contains("select = 1 AND from = 2"));
    }

    @Test
    public void testVeryLongColumnList() {
        // Test with many columns
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            columns.add("col" + i);
        }

        String sql = Dsl.PSC.select(columns).from("big_table").build().query();

        assertTrue(sql.startsWith("SELECT col0, col1, col2"));
        assertTrue(sql.contains("col49"));
        assertTrue(sql.contains("FROM big_table"));
    }

    @Test
    public void testSpecialCharactersInValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "O'Brien");
        values.put("comment", "Test \"quote\" handling");

        String sql = Dsl.PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("comment = ?"));

        SqlBuilder builder = Dsl.PSC.update("users").set(values).where(Filters.eq("id", 1));
        builder.build().query();

        List<Object> params = builder.parameters();
        assertEquals("O'Brien", params.get(0));
        assertEquals("Test \"quote\" handling", params.get(1));
    }

    @Test
    public void testAliaspropColumnNameMap() {
        // Test query with table aliases and property column name mapping
        String sql = Dsl.PSC.select("a.firstName", "o.orderNumber").from(Account.class, "a").join(Order.class, "o").on("a.id = o.userId").build().query();

        assertTrue(sql.contains("a.first_name AS \"a.firstName\""));
        assertTrue(sql.contains("o.order_number AS \"o.orderNumber\""));
    }

    @Test
    public void testSelectConstants() {
        // Test selecting constants
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("TOP", AbstractQueryBuilder.TOP);

        String sql = Dsl.PSC.select(AbstractQueryBuilder.COUNT_ALL).from("users").build().query();

        assertEquals("SELECT count(*) FROM users", sql);
    }

    @Test
    public void testEntityWithNoTableAnnotation() {
        // Test with a simple class without @Table annotation
        class SimpleEntity {
            private Long id;
            private String name;

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        String tableName = AbstractQueryBuilder.getTableName(SimpleEntity.class, NamingPolicy.SNAKE_CASE);
        assertEquals("simple_entity", tableName);
    }

    @Test
    public void testQMEAsparameter() {
        // Test using QME (Question Mark SqlExpression) as parameter
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", Filters.QME);
        values.put("age", 25);

        String sql = Dsl.PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("age = ?"));
    }

    @Test
    public void testClosedBuilderException() {
        // Test that using a closed builder throws exception
        SqlBuilder builder = Dsl.PSC.select("*").from("users");
        builder.build().query(); // This closes the builder

        // Any operation after sql() should throw exception
        assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
        assertThrows(RuntimeException.class, () -> builder.build().query());
    }

    @Test
    public void testActiveStringBuilderLimit() {
        List<SqlBuilder> builders = new ArrayList<>();
        int activeBefore = AbstractQueryBuilder.activeStringBuilderCounter.get();

        // Building each query should increment the active StringBuilder counter until build() releases it.
        for (int i = 0; i < 10; i++) {
            builders.add(Dsl.PSC.select("*").from("users"));
        }

        assertEquals(activeBefore + builders.size(), AbstractQueryBuilder.activeStringBuilderCounter.get());

        for (SqlBuilder builder : builders) {
            builder.build();
        }

        assertEquals(activeBefore, AbstractQueryBuilder.activeStringBuilderCounter.get());
    }

    @Test
    public void testAllPublicConstants() {
        // Verify all public constants
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("TOP", AbstractQueryBuilder.TOP);
        assertEquals("UNIQUE", AbstractQueryBuilder.UNIQUE);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("DISTINCTROW", AbstractQueryBuilder.DISTINCTROW);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    /**
     * Regression test for ClassCastException bug in SqlBuilder.appendCondition().
     * Where and Having extend Clause (not Cell), so casting them to Cell would throw ClassCastException.
     * This test verifies that Where/Having conditions are correctly handled when nested inside a Junction.
     */
    @Test
    public void testWhereAndHavingNestedInJunction() {
        // Where nested inside an And junction - this triggers SqlBuilder.appendCondition() with a Where instance
        Where where = Filters.where("age > 18");
        Having having = Filters.having("COUNT(*) > 5");

        // Using Where as a standalone Clause via append()
        String sql = Dsl.PSC.select("*").from("users").append(where).build().query();
        assertTrue(sql.contains("WHERE age > 18"));

        // Using Having as a standalone Clause via append()
        sql = Dsl.PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).build().query();
        assertTrue(sql.contains("HAVING COUNT(*) > 5"));

        // Where inside a Criteria (exercises AbstractQueryBuilder.append -> appendCondition path)
        Criteria criteria = Criteria.builder().where(Filters.expr("status = 'ACTIVE'")).groupBy("status").having(Filters.expr("COUNT(*) > 10")).build();
        sql = Dsl.PSC.select("status", "COUNT(*)").from("users").append(criteria).build().query();
        assertTrue(sql.contains("WHERE status = 'ACTIVE'"));
        assertTrue(sql.contains("HAVING COUNT(*) > 10"));
    }

    @Test
    public void testNamedParameterHandlerIsCarriedBySqlDialect() {
        final BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");
        final SqlDialect dialect = Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler(customHandler).build();

        assertEquals(customHandler, dialect.namedParameterHandler());
        assertEquals("SELECT name FROM users WHERE id = #{id}",
                Dsl.forDialect(dialect).select("name").from("users").where(Filters.eq("id", 1)).build().query());
    }

    @Test
    public void testGlobalNamedParameterHandlerMethodsAreRemoved() {
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getMethod("setHandlerForNamedParameter", BiConsumer.class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getMethod("resetHandlerForNamedParameter"));
    }

    public static class TestEntity {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Integer age;
        private String status;
        private Date createdDate;
        private Date modifiedDate;

        public TestEntity() {
        }

        public TestEntity(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

        public Date getModifiedDate() {
            return modifiedDate;
        }

        public void setModifiedDate(Date modifiedDate) {
            this.modifiedDate = modifiedDate;
        }
    }

    public static class TestOrder {
        private Long orderId;
        private Long userId;
        private String orderNumber;
        private Date orderDate;
        private Double totalAmount;

        // Getters and setters
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }

        public Double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    @Nested
    public class SCSBTest {

        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private String status;
            private Date createdDate;

            public Account() {
            }

            public Account(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            public Account(String firstName, String email, String status) {
                this.firstName = firstName;
                this.email = email;
                this.status = status;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.SCSB.insert("name");
            Assertions.assertNotNull(sb);
            // Verify the SQL builder is created properly
            Assertions.assertDoesNotThrow(() -> sb.into("account").build().query());
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.SCSB.insert("name", "email", "status");
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            List<String> columns = Arrays.asList("name", "email", "status");
            SqlBuilder sb = Dsl.SCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 25);

            SqlBuilder sb = Dsl.SCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntity() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            SqlBuilder sb = Dsl.SCSB.insert(account);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder sb = Dsl.SCSB.insert(account, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = Dsl.SCSB.insert(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "id"));
            SqlBuilder sb = Dsl.SCSB.insert(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = Dsl.SCSB.insertInto(Account.class);
            Assertions.assertNotNull(sb);

            // This should already have the table name set
            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = Dsl.SCSB.insertInto(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "john@email.com", "ACTIVE"), new Account("Jane", "jane@email.com", "ACTIVE"));

            SqlBuilder sb = Dsl.SCSB.batchInsert(accounts);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = Dsl.SCSB.update("account");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.SCSB.update("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.SCSB.update(Account.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder sb = Dsl.SCSB.update(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom("account");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.SCSB.select("COUNT(*)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.SCSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectCollectionOfColumns() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.SCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder sb = Dsl.SCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "salt"));
            SqlBuilder sb = Dsl.SCSB.select(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.select(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.SCSB.select(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = Dsl.SCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = Dsl.SCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = Dsl.SCSB.count("account");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = Dsl.SCSB.count(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            SqlBuilder sb = Dsl.SCSB.renderCondition(cond, Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            // Should contain the condition SQL
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testFromCondition_singleArg_equivalentToNullEntityClass() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            String viaSingleArg = Dsl.SCSB.renderCondition(cond).build().query();
            String viaNullClass = Dsl.SCSB.renderCondition(Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000)), null).build().query();

            Assertions.assertEquals(viaNullClass, viaSingleArg);
            Assertions.assertTrue(viaSingleArg.contains("AND"));
        }

        @Test
        public void testFromCondition_singleArg_nullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.renderCondition(null));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.renderCondition(null, Account.class));
        }

        @Test
        public void testSelectEmptyColumns() {
            // Empty column names should throw exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(new String[0]));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(Collections.emptyMap()));
        }

        @Test
        public void testSelectEmptyString() {
            // Empty select part should throw exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(""));
        }

        @Test
        public void testComplexQueryWithConditions() {
            SqlBuilder sb = Dsl.SCSB.select("firstName", "lastName")
                    .from("account")
                    .where(Filters.eq("status", "'ACTIVE'").and(Filters.gt("age", 18)))
                    .orderBy("lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }
    }

    @Nested
    public class ACSBTest {

        public static class User {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private String status;
            private Date createdDate;

            public User() {
            }

            public User(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.ACSB.insert("name");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.ACSB.insert("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.ACSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("age", 30);

            SqlBuilder sb = Dsl.ACSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            User user = new User("John", "Doe");
            user.setEmail("john@example.com");

            SqlBuilder sb = Dsl.ACSB.insert(user);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            User user = new User("John", "Doe");
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));

            SqlBuilder sb = Dsl.ACSB.insert(user, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = Dsl.ACSB.insert(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = Dsl.ACSB.insert(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = Dsl.ACSB.insertInto(User.class);
            Assertions.assertNotNull(sb);

            // Should be able to get SQL directly as table name is already set
            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = Dsl.ACSB.insertInto(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe"), new User("Jane", "Smith"));

            SqlBuilder sb = Dsl.ACSB.batchInsert(users);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> user1 = new HashMap<>();
            user1.put("firstName", "John");
            user1.put("lastName", "Doe");
            propsList.add(user1);

            Map<String, Object> user2 = new HashMap<>();
            user2.put("firstName", "Jane");
            user2.put("lastName", "Smith");
            propsList.add(user2);

            SqlBuilder sb = Dsl.ACSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = Dsl.ACSB.update("users");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.ACSB.update("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.ACSB.update(User.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = Dsl.ACSB.update(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom("users");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.ACSB.select("COUNT(DISTINCT userId)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("orders").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(DISTINCT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.ACSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.ACSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("email", "emailAddress");

            SqlBuilder sb = Dsl.ACSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = Dsl.ACSB.select(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.ACSB.select(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
            SqlBuilder sb = Dsl.ACSB.select(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.select(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.ACSB.select(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = Dsl.ACSB.select(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u1", "user1", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = Dsl.ACSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u1", "user1", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = Dsl.ACSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = Dsl.ACSB.count("users");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = Dsl.ACSB.count(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.or(Filters.eq("status", "'ACTIVE'"), Filters.eq("status", "'PENDING'"));

            SqlBuilder sb = Dsl.ACSB.renderCondition(cond, User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.renderCondition(null, User.class));
        }

        @Test
        public void testEmptySelectParts() {
            // Test empty string
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(""));

            // Test empty array
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(new String[0]));

            // Test empty map
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(Collections.emptyMap()));
        }

        @Test
        public void testComplexQuery() {
            SqlBuilder sb = Dsl.ACSB.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(Filters.eq("u.status", "'ACTIVE'"))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("u.lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }
    }

    @Nested
    public class LCSBTest extends TestBase {

        public static class Customer {
            private Long customerId;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private String phoneNumber;
            private String status;
            private Date registrationDate;

            public Customer() {
            }

            public Customer(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            public Customer(String firstName, String lastName, String email) {
                this.firstName = firstName;
                this.lastName = lastName;
                this.emailAddress = email;
            }

            // Getters and setters
            public Long getCustomerId() {
                return customerId;
            }

            public void setCustomerId(Long customerId) {
                this.customerId = customerId;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public String getPhoneNumber() {
                return phoneNumber;
            }

            public void setPhoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public Date getRegistrationDate() {
                return registrationDate;
            }

            public void setRegistrationDate(Date registrationDate) {
                this.registrationDate = registrationDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.LCSB.insert("customerName");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.LCSB.insert("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "emailAddress", "phoneNumber");
            SqlBuilder sb = Dsl.LCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "Alice");
            props.put("lastName", "Johnson");
            props.put("emailAddress", "alice@example.com");

            SqlBuilder sb = Dsl.LCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            Customer customer = new Customer("Bob", "Smith", "bob@example.com");
            customer.setPhoneNumber("123-456-7890");

            SqlBuilder sb = Dsl.LCSB.insert(customer);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Customer customer = new Customer("Carol", "Davis");
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));

            SqlBuilder sb = Dsl.LCSB.insert(customer, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = Dsl.LCSB.insert(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.insert(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = Dsl.LCSB.insertInto(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId"));
            SqlBuilder sb = Dsl.LCSB.insertInto(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<Customer> customers = Arrays.asList(new Customer("Dave", "Wilson", "dave@example.com"), new Customer("Eve", "Brown", "eve@example.com"));

            SqlBuilder sb = Dsl.LCSB.batchInsert(customers);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();

            Map<String, Object> customer1 = new HashMap<>();
            customer1.put("firstName", "Frank");
            customer1.put("lastName", "Miller");
            propsList.add(customer1);

            Map<String, Object> customer2 = new HashMap<>();
            customer2.put("firstName", "Grace");
            customer2.put("lastName", "Lee");
            propsList.add(customer2);

            SqlBuilder sb = Dsl.LCSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = Dsl.LCSB.update("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'PREMIUM'").where(Filters.gt("totalPurchases", 1000)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.LCSB.update("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("customerId", 100)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.LCSB.update(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.update(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.lt("lastLoginDate", "2020-01-01")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.isNull("emailAddress")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.LCSB.select("COUNT(*) as totalCount");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("totalCount"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.LCSB.select("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("customerId", "firstName", "lastName");
            SqlBuilder sb = Dsl.LCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");

            SqlBuilder sb = Dsl.LCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder sb = Dsl.LCSB.select(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Customer.class, "c1", "customer1", null, false, null),
                    selection(Customer.class, "c2", "customer2", null, false, null), selection(Customer.class, "c3", "customer3", null, false, null));

            SqlBuilder sb = Dsl.LCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2, customers c3").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Customer.class, "c1", "customer1", null, false, null),
                    selection(Customer.class, "c2", "customer2", null, false, null));

            SqlBuilder sb = Dsl.LCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = Dsl.LCSB.count("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = Dsl.LCSB.count(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.between("registrationDate", "2020-01-01", "2023-12-31"));

            SqlBuilder sb = Dsl.LCSB.renderCondition(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        public void testParseComplexCondition() {
            Condition cond = Filters.or(Filters.and(Filters.eq("status", "'PREMIUM'"), Filters.gt("totalPurchases", 5000)),
                    Filters.and(Filters.eq("status", "'GOLD'"), Filters.gt("totalPurchases", 3000)));

            SqlBuilder sb = Dsl.LCSB.renderCondition(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.renderCondition(null, Customer.class));
        }

        @Test
        public void testEmptySelectParts() {
            // Test empty string
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(""));

            // Test null string
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select((String) null));

            // Test empty array
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(new String[0]));

            // Test empty collection
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(Collections.<String> emptyList()));

            // Test empty map
            Assertions.assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(Collections.emptyMap()));
        }

        @Test
        public void testEmptyMultiSelects() {
            // Test null selections list
            Assertions.assertThrows(RuntimeException.class, () -> Dsl.LCSB.select((List<Selection>) null));

            // Test empty selections list
            Assertions.assertThrows(RuntimeException.class, () -> Dsl.LCSB.select(Collections.<String> emptyList()));
        }

        @Test
        public void testComplexQueryWithJoins() {
            SqlBuilder sb = Dsl.LCSB.select("c.firstName", "c.lastName", "SUM(o.amount)")
                    .from("customers c")
                    .innerJoin("orders o")
                    .on("c.customerId = o.customerId")
                    .where(Filters.eq("c.status", "'ACTIVE'"))
                    .groupBy("c.customerId", "c.firstName", "c.lastName")
                    .having(Filters.gt("SUM(o.amount)", 10000))
                    .orderBy("SUM(o.amount) DESC");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INNER JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        public void testQueryWithSubquery() {
            SubQuery subquery = Filters.subQuery("orders", N.asList("customerId"), Filters.gt("amount", 1000));

            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.in("customerId", subquery));

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("IN"));
        }

        @Test
        public void testUpdateWithMultipleSet() {
            SqlBuilder sb = Dsl.LCSB.update("customers")
                    .set("status", "'INACTIVE'")
                    .set("lastModified", "CURRENT_TIMESTAMP")
                    .where(Filters.lt("lastLoginDate", "2020-01-01"));

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("SET"));
        }

        @Test
        public void testSelectDistinct() {
            SqlBuilder sb = Dsl.LCSB.select("DISTINCT status").from("customers").orderBy("status");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testQueryWithLimit() {
            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("registrationDate DESC").limit(10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testQueryWithLimitAndOffset() {
            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("customerId").limit(20, 10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionQuery() {
            SqlBuilder query1 = Dsl.LCSB.select("firstName", "lastName").from("customers").where(Filters.eq("status", "'ACTIVE'"));

            SqlBuilder query2 = Dsl.LCSB.select("firstName", "lastName").from("employees").where(Filters.eq("department", "'SALES'"));

            SqlBuilder sb = query1.union(query2);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UNION"));
        }

        @Test
        public void testCaseWhenExpression() {
            String caseExpr = "CASE WHEN status = 'PREMIUM' THEN 'VIP' " + "WHEN status = 'GOLD' THEN 'Important' " + "ELSE 'Regular' END AS customerType";

            SqlBuilder sb = Dsl.LCSB.select("firstName", "lastName", caseExpr).from("customers");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("case when"));
        }

        @Test
        public void testAggregateFunctions() {
            SqlBuilder sb = Dsl.LCSB.select("status", "COUNT(*) as count", "AVG(totalPurchases) as avgPurchases", "MAX(lastLoginDate) as lastActive")
                    .from("customers")
                    .groupBy("status")
                    .having(Filters.gt("COUNT(*)", 10));

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT"));
            Assertions.assertTrue(sql.contains("AVG"));
            Assertions.assertTrue(sql.contains("MAX"));
        }

        @Test
        public void testCreateInstance() {
            // Each factory call returns a new SqlBuilder instance
            SqlBuilder instance1 = Dsl.LCSB.select("*");
            SqlBuilder instance2 = Dsl.LCSB.select("*");
            Assertions.assertNotNull(instance1);
            Assertions.assertNotSame(instance1, instance2);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }
    }

    @Nested
    public class PSBTest {

        @Table("test_users")
        public static class User {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String nonUpdatableField;
            @Transient
            private String transientField;

            public User() {
            }

            public User(String firstName, String lastName, String email) {
                this.firstName = firstName;
                this.lastName = lastName;
                this.email = email;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getNonUpdatableField() {
                return nonUpdatableField;
            }

            public void setNonUpdatableField(String nonUpdatableField) {
                this.nonUpdatableField = nonUpdatableField;
            }

            public String getTransientField() {
                return transientField;
            }

            public void setTransientField(String transientField) {
                this.transientField = transientField;
            }
        }

        @Test
        public void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PSB.insert("name");
            assertNotNull(builder);

            // Test with empty string - should throw exception
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(""));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PSB.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new String[0]));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = Dsl.PSB.insert(columns);
            assertNotNull(builder);

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Collection<String>) null));

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new ArrayList<>()));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = Dsl.PSB.insert(props);
            assertNotNull(builder);

            // Test with null map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Map<String, Object>) null));

            // Test with empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new HashMap<>()));
        }

        @Test
        public void testInsert_Entity() {
            User user = new User("John", "Doe", "john@example.com");
            SqlBuilder builder = Dsl.PSB.insert(user);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Object) null));
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            User user = new User("John", "Doe", "john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SqlBuilder builder = Dsl.PSB.insert(user, excludedProps);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = Dsl.PSB.insert(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Class<?>) null));
        }

        @Test
        public void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = Dsl.PSB.insert(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PSB.insertInto(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insertInto((Class<?>) null));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = Dsl.PSB.insertInto(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insertInto(null, excludedProps));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe", "john@example.com"), new User("Jane", "Smith", "jane@example.com"));

            SqlBuilder builder = Dsl.PSB.batchInsert(users);
            assertNotNull(builder);

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.batchInsert(new ArrayList<>()));

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.batchInsert(null));

            // Test with maps
            List<Map<String, Object>> maps = new ArrayList<>();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("firstName", "John");
            maps.add(map1);

            SqlBuilder mapBuilder = Dsl.PSB.batchInsert(maps);
            assertNotNull(mapBuilder);
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PSB.update("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(""));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSB.update("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update("users", null));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PSB.update(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update((Class<?>) null));
        }

        @Test
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = Dsl.PSB.update(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(null, excludedProps));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PSB.deleteFrom("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom(""));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSB.deleteFrom("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom("users", null));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSB.deleteFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom((Class<?>) null));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PSB.select("firstName");
            assertNotNull(builder);

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(""));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PSB.select("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new String[0]));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            SqlBuilder builder = Dsl.PSB.select(columns);
            assertNotNull(builder);

            // Test with null/empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new ArrayList<Selection>()));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PSB.select(aliases);
            assertNotNull(builder);

            // Test with null/empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new HashMap<>()));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PSB.select(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Class<?>) null));
        }

        @Test
        public void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = Dsl.PSB.select(User.class, true);
            assertNotNull(builder);

            // Test without sub-entities
            SqlBuilder builder2 = Dsl.PSB.select(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.select(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null excluded props
            SqlBuilder builder2 = Dsl.PSB.select(User.class, null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.select(User.class, true, excludedProps);
            assertNotNull(builder);

            // Test all combinations
            assertNotNull(Dsl.PSB.select(User.class, false, null));
            assertNotNull(Dsl.PSB.select(User.class, true, null));
            assertNotNull(Dsl.PSB.select(User.class, false, excludedProps));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom((Class<?>) null));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u");
            assertNotNull(builder);

            // Test with null alias
            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, (String) null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, true);
            assertNotNull(builder);

            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndSubEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", true);
            assertNotNull(builder);

            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, "u", false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntitiesAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, true, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", true, excludedProps);
            assertNotNull(builder);

            // Test various combinations
            assertNotNull(Dsl.PSB.selectFrom(User.class, "u", false, null));
            assertNotNull(Dsl.PSB.selectFrom(User.class, null, true, excludedProps));
        }

        @Test
        public void testSelect_TwoEntities() {
            SqlBuilder builder = Dsl.PSB.select(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelect_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PSB.select(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);

            // Test with null exclusions
            SqlBuilder builder2 = Dsl.PSB.select(User.class, "u", "user", null, User.class, "u2", "user2", null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = Dsl.PSB.select(selections);
            assertNotNull(builder);

            // Test with null/empty list
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new ArrayList<Selection>()));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = Dsl.PSB.selectFrom(selections);
            assertNotNull(builder);

            // Test with invalid selections
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom(new ArrayList<>()));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = Dsl.PSB.count("users");
            assertNotNull(builder);

            // Test with null/empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.count((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.count(""));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PSB.count(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.count((Class<?>) null));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.eq("firstName", "John");
            SqlBuilder builder = Dsl.PSB.renderCondition(cond, User.class);
            assertNotNull(builder);

            // Test with null condition
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.renderCondition(null, User.class));

            // Test with complex condition
            Condition complexCond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));
            SqlBuilder complexBuilder = Dsl.PSB.renderCondition(complexCond, User.class);
            assertNotNull(complexBuilder);
        }

        @Test
        public void testCreateInstance() {
            // Each factory call returns a new SqlBuilder instance
            SqlBuilder instance1 = Dsl.PSB.select("*");
            SqlBuilder instance2 = Dsl.PSB.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }
    }

    @Nested
    public class PSCTest {

        @Table("account")
        public static class Account {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String status;
            @Transient
            private String transientField;

            public Account() {
            }

            public Account(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getTransientField() {
                return transientField;
            }

            public void setTransientField(String transientField) {
                this.transientField = transientField;
            }
        }

        @Test
        public void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PSC.insert("firstName");
            assertNotNull(builder);

            // Test SQL generation
            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PSC.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = Dsl.PSC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = Dsl.PSC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertEquals(2, sp.parameters().size());
        }

        @Test
        public void testInsert_Entity() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");

            SqlBuilder builder = Dsl.PSC.insert(account);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertTrue(sp.query().contains("email"));
            assertFalse(sp.query().contains("created_date")); // ReadOnly field should be excluded
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SqlBuilder builder = Dsl.PSC.insert(account, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertFalse(sp.query().contains("email"));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = Dsl.PSC.insert(Account.class);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("created_date")); // ReadOnly
            assertFalse(sql.contains("transient_field")); // Transient
        }

        @Test
        public void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SqlBuilder builder = Dsl.PSC.insert(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PSC.insertInto(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = Dsl.PSC.insertInto(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertFalse(sql.contains(" id "));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "Doe"), new Account("Jane", "Smith"));

            SqlBuilder builder = Dsl.PSC.batchInsert(accounts);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
            assertEquals(4, sp.parameters().size()); // 2 accounts * 2 fields each
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PSC.update("account");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSC.update("account", Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PSC.update(Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
            // Should include updatable fields
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
            // Should not include non-updatable fields
            assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));
            SqlBuilder builder = Dsl.PSC.update(Account.class, excludedProps);
            assertNotNull(builder);

            // Get the generated column names
            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PSC.deleteFrom("account");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSC.deleteFrom("account", Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("firstName", "John")).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSC.deleteFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PSC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PSC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("id", "firstName", "lastName");
            SqlBuilder builder = Dsl.PSC.select(columns);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PSC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("first_name AS \"fname\""));
            assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PSC.select(Account.class);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("transient_field"));
        }

        @Test
        public void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = Dsl.PSC.select(Account.class, true);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SqlBuilder builder = Dsl.PSC.select(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSC.selectFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PSC.selectFrom(Account.class, "a");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM account a"));
            assertTrue(sql.contains("a.first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PSC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertTrue(sql.contains("a2.first_name AS \"account2.firstName\""));
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", Arrays.asList("id", "firstName"), false, null),
                    selection(Account.class, "a2", "account2", null, false, new HashSet<>(Arrays.asList("email"))));

            SqlBuilder builder = Dsl.PSC.select(selections);
            assertNotNull(builder);

            String sql = builder.from("account a, account a2").build().query();
            assertTrue(sql.contains("a.id AS \"account.id\""));
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertFalse(sql.contains("account2.email"));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = Dsl.PSC.count("account");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PSC.count(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.like("email", "%@example.com"));

            SqlBuilder builder = Dsl.PSC.renderCondition(cond, Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("first_name = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("email LIKE ?"));
        }

        @Test
        public void testCreateInstance() {
            SqlBuilder instance1 = Dsl.PSC.select("*");
            SqlBuilder instance2 = Dsl.PSC.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PSC uses snake_case naming
            SqlBuilder builder = Dsl.PSC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("account").build().query();

            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }
    }

    @Nested
    public class PACTest {

        @Table("USER_ACCOUNT")
        public static class UserAccount {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String accountStatus;
            @Transient
            private String tempData;

            public UserAccount() {
            }

            public UserAccount(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getAccountStatus() {
                return accountStatus;
            }

            public void setAccountStatus(String accountStatus) {
                this.accountStatus = accountStatus;
            }

            public String getTempData() {
                return tempData;
            }

            public void setTempData(String tempData) {
                this.tempData = tempData;
            }
        }

        @Test
        public void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PAC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PAC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "emailAddress");
            SqlBuilder builder = Dsl.PAC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = Dsl.PAC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertEquals(2, sp.parameters().size());
        }

        @Test
        public void testInsert_Entity() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");

            SqlBuilder builder = Dsl.PAC.insert(account);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertTrue(sp.query().contains("EMAIL_ADDRESS"));
            assertFalse(sp.query().contains("CREATED_DATE")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = Dsl.PAC.insert(account, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertFalse(sp.query().contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = Dsl.PAC.insert(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("CREATED_DATE")); // ReadOnly
            assertFalse(sql.contains("TEMP_DATA")); // Transient
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PAC.insertInto(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testBatchInsert() {
            List<UserAccount> accounts = Arrays.asList(new UserAccount("John", "Doe"), new UserAccount("Jane", "Smith"));

            SqlBuilder builder = Dsl.PAC.batchInsert(accounts);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PAC.update("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PAC.update(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PAC.deleteFrom("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PAC.deleteFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PAC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PAC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PAC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PAC.select(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            assertFalse(sql.contains("TEMP_DATA"));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class, "u");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM USER_ACCOUNT u"));
            assertTrue(sql.contains("u.FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class, "u1", "user1", UserAccount.class, "u2", "user2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("u1.FIRST_NAME AS \"user1.firstName\""));
            assertTrue(sql.contains("u2.FIRST_NAME AS \"user2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = Dsl.PAC.count("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PAC.count(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));

            SqlBuilder builder = Dsl.PAC.renderCondition(cond, UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FIRST_NAME = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("ID > ?"));
        }

        @Test
        public void testCreateInstance() {
            SqlBuilder instance1 = Dsl.PAC.select("*");
            SqlBuilder instance2 = Dsl.PAC.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PAC uses UPPER_CASE naming
            SqlBuilder builder = Dsl.PAC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("USER_ACCOUNT").build().query();

            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
        }

        @Test
        public void testComplexQueries() {
            // Test complex query with joins
            Set<String> excludeUser1 = new HashSet<>(Arrays.asList("tempData"));
            Set<String> excludeUser2 = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PAC.select(UserAccount.class, "u1", "user1", excludeUser1, UserAccount.class, "u2", "user2", excludeUser2);

            String sql = builder.from("USER_ACCOUNT u1, USER_ACCOUNT u2").where(Filters.eq("u1.id", "u2.id")).build().query();

            assertTrue(sql.contains("u1.ID AS \"user1.id\""));
            assertTrue(sql.contains("u2.ID AS \"user2.id\""));
            assertFalse(sql.contains("user1.tempData"));
            assertFalse(sql.contains("user2.createdDate"));
        }

        @Test
        public void testAllMethodsWithNullParameters() {
            // Test all methods handle null appropriately
            // assertThrows(IllegalArgumentException.class, () -> PAC.insert((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Map<String, Object>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Object) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insertInto((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.batchInsert(null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.update((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.update((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.deleteFrom((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.deleteFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.count((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.count((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.renderCondition(null, UserAccount.class));
        }
    }

    @Nested
    public class PLCTest {

        @Table("userProfile")
        public static class UserProfile {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Boolean isActive;
            @ReadOnly
            private Date lastLoginDate;
            @NonUpdatable
            private String accountType;
            @Transient
            private String sessionData;

            public UserProfile() {
            }

            public UserProfile(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Boolean getIsActive() {
                return isActive;
            }

            public void setIsActive(Boolean isActive) {
                this.isActive = isActive;
            }

            public Date getLastLoginDate() {
                return lastLoginDate;
            }

            public void setLastLoginDate(Date lastLoginDate) {
                this.lastLoginDate = lastLoginDate;
            }

            public String getAccountType() {
                return accountType;
            }

            public void setAccountType(String accountType) {
                this.accountType = accountType;
            }

            public String getSessionData() {
                return sessionData;
            }

            public void setSessionData(String sessionData) {
                this.sessionData = sessionData;
            }
        }

        @Test
        public void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PLC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PLC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "isActive");
            SqlBuilder builder = Dsl.PLC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("isActive"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);

            SqlBuilder builder = Dsl.PLC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("isActive"));
            assertEquals(3, sp.parameters().size());
        }

        @Test
        public void testInsert_Entity() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            profile.setIsActive(true);

            SqlBuilder builder = Dsl.PLC.insert(profile);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("emailAddress"));
            assertTrue(sp.query().contains("isActive"));
            assertFalse(sp.query().contains("lastLoginDate")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = Dsl.PLC.insert(profile, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertFalse(sp.query().contains("emailAddress"));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = Dsl.PLC.insert(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("lastLoginDate")); // ReadOnly
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PLC.insertInto(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testBatchInsert() {
            List<UserProfile> profiles = Arrays.asList(new UserProfile("John", "Doe"), new UserProfile("Jane", "Smith"));

            SqlBuilder builder = Dsl.PLC.batchInsert(profiles);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PLC.update("userProfile");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PLC.update(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PLC.deleteFrom("userProfile");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PLC.deleteFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PLC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PLC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PLC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PLC.select(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class, "p");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM userProfile p"));
            assertTrue(sql.contains("p.firstName"));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class, "p1", "profile1", UserProfile.class, "p2", "profile2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertTrue(sql.contains("p2.firstName AS \"profile2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = Dsl.PLC.count("userProfile");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PLC.count(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.eq("isActive", true));

            SqlBuilder builder = Dsl.PLC.renderCondition(cond, UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("firstName = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("isActive = ?"));
        }

        @Test
        public void testCreateInstance() {
            SqlBuilder instance1 = Dsl.PLC.select("*");
            SqlBuilder instance2 = Dsl.PLC.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PLC uses camelCase naming (no transformation)
            SqlBuilder builder = Dsl.PLC.select("firstName", "lastName", "emailAddress", "isActive");
            String sql = builder.from("userProfile").build().query();

            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));

            // Verify no snake_case or UPPER_CASE transformation
            assertFalse(sql.contains("first_name"));
            assertFalse(sql.contains("FIRST_NAME"));
            assertFalse(sql.contains("email_address"));
            assertFalse(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("is_active"));
            assertFalse(sql.contains("IS_ACTIVE"));
        }

        @Test
        public void testComplexQueries() {
            // Test complex query with multiple selections
            List<Selection> selections = Arrays.asList(selection(UserProfile.class, "p1", "profile1", Arrays.asList("id", "firstName"), false, null),
                    selection(UserProfile.class, "p2", "profile2", null, false, new HashSet<>(Arrays.asList("sessionData", "lastLoginDate"))));

            SqlBuilder builder = Dsl.PLC.select(selections);
            String sql = builder.from("userProfile p1, userProfile p2").build().query();

            assertTrue(sql.contains("p1.id AS \"profile1.id\""));
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertFalse(sql.contains("profile2.sessionData"));
            assertFalse(sql.contains("profile2.lastLoginDate"));
        }

        @Test
        public void testAllMethodOverloads() {
            // Test all overloaded methods
            assertNotNull(Dsl.PLC.insert(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.insertInto(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.update("userProfile", UserProfile.class));
            assertNotNull(Dsl.PLC.update(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.deleteFrom("userProfile", UserProfile.class));
            assertNotNull(Dsl.PLC.select(UserProfile.class, true));
            assertNotNull(Dsl.PLC.select(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.select(UserProfile.class, true, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, true));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", true));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, true, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", true, new HashSet<>()));
        }

        @Test
        public void testErrorCases() {
            // Test error cases
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new HashMap<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.batchInsert(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.update(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.deleteFrom(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new HashMap<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.selectFrom(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.count(""));
        }
    }

    @Nested
    public class NSBTest {

        @Table("test_user")
        public static class User {
            private long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String password;
            @Transient
            private String tempData;

            // Getters and setters
            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getTempData() {
                return tempData;
            }

            public void setTempData(String tempData) {
                this.tempData = tempData;
            }
        }

        @Test
        public void testInsertWithSingleExpression() {
            String sql = Dsl.NSB.insert("user_name").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("user_name"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains(":user_name"));
        }

        @Test
        public void testInsertWithMultipleColumns() {
            String sql = Dsl.NSB.insert("first_name", "last_name", "email").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":first_name"));
            Assertions.assertTrue(sql.contains(":last_name"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "created_date");
            String sql = Dsl.NSB.insert(columns).into("products").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO products"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("created_date"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("username", "john_doe");
            data.put("age", 25);
            String sql = Dsl.NSB.insert(data).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("username"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains(":username"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");
            user.setCreatedDate(new Date());
            user.setTempData("temp");

            String sql = Dsl.NSB.insert(user).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            // Should include regular fields
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            Assertions.assertFalse(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");

            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.insert(user, exclude).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.NSB.insert(User.class).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            // Should include insertable fields
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            Assertions.assertFalse(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = N.asSet("id", "password");
            String sql = Dsl.NSB.insert(User.class, exclude).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertInto() {
            String sql = Dsl.NSB.insertInto(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO test_user"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> exclude = N.asSet("id");
            String sql = Dsl.NSB.insertInto(User.class, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO test_user"));
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = new ArrayList<>();
            User user1 = new User();
            user1.setFirstName("John");
            user1.setLastName("Doe");

            User user2 = new User();
            user2.setFirstName("Jane");
            user2.setLastName("Smith");

            users.add(user1);
            users.add(user2);

            String sql = Dsl.NSB.batchInsert(users).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testUpdate() {
            String sql = Dsl.NSB.update("users").set("last_login", "status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("last_login"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains(":id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = Dsl.NSB.update("user_accounts", User.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE user_accounts"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.NSB.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = Dsl.NSB.update(User.class, exclude).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            // Should be able to update non-excluded fields
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = Dsl.NSB.deleteFrom("users").where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains(":status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = Dsl.NSB.deleteFrom("user_accounts", User.class).where(Filters.lt("lastLogin", new Date())).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM user_accounts"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("lastLogin"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.NSB.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = Dsl.NSB.select("COUNT(*) AS total").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.NSB.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = Dsl.NSB.select(columns).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("u.first_name", "firstName");
            aliases.put("u.last_name", "lastName");
            aliases.put("COUNT(o.id)", "orderCount");

            String sql = Dsl.NSB.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.NSB.select(User.class).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            // Should include all non-transient fields
            Assertions.assertTrue(sql.contains("createdDate"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @Transient fields
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NSB.select(User.class, true).from("users u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = Dsl.NSB.select(User.class, exclude).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.select(User.class, true, exclude).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = Dsl.NSB.selectFrom(User.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = Dsl.NSB.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM test_user u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, "u", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, "u", exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("test_user u"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, "u", true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.NSB.select(User.class, "u", "user_", User.class, "u2", "user2_")
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("u.id AS \"user_.id\""));
            Assertions.assertTrue(sql.contains("u2.id AS \"user2_.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeUser = N.asSet("password");
            Set<String> excludeUser2 = N.asSet("email");

            String sql = Dsl.NSB.select(User.class, "u", "user_", excludeUser, User.class, "u2", "user2_", excludeUser2)
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, N.asSet("password")));

            String sql = Dsl.NSB.select(selections).from("users u").join("users m").on("u.manager_id = m.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = N.asSet("password");
            Set<String> excludeM = N.asSet("email", "password");

            String sql = Dsl.NSB.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, null));

            String sql = Dsl.NSB.selectFrom(selections).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = Dsl.NSB.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.NSB.count(User.class).where(Filters.eq("status", "active")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = Dsl.NSB.renderCondition(cond, User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains(":age"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testEmptyCollectionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.insert(new ArrayList<>()).into("users").build().query();
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select(new ArrayList<Selection>()).from("users").build().query();
            });
        }

        @Test
        public void testNullArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.renderCondition(null, User.class);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select((String) null).from("users");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select((Collection<String>) null).from("users");
            });
        }

        @Test
        public void testJoinOperations() {
            // Test INNER JOIN
            String sql = Dsl.NSB.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            // Test LEFT JOIN
            sql = Dsl.NSB.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            // Test RIGHT JOIN
            sql = Dsl.NSB.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            // Test FULL JOIN
            sql = Dsl.NSB.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            // Test CROSS JOIN
            sql = Dsl.NSB.select("*").from("users").crossJoin("departments").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            // Test NATURAL JOIN
            sql = Dsl.NSB.select("*").from("users").naturalJoin("user_profiles").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testMultipleJoins() {
            String sql = Dsl.NSB.select("u.name", "o.id", "p.name")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .leftJoin("products p")
                    .on("o.product_id = p.id")
                    .where(Filters.eq("u.active", true))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN orders"));
            Assertions.assertTrue(sql.contains("LEFT JOIN products"));
        }

        @Test
        public void testJoinWithCondition() {
            String sql = Dsl.NSB.select("*")
                    .from("users u")
                    .leftJoin("orders o")
                    .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.gt("o.amount", 100)))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testUpdateOperations() {
            // Test simple update
            String sql = Dsl.NSB.update("users").set("status", "active").where(Filters.eq("id", 1)).build().query();

            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));

            // Test update with multiple sets
            sql = Dsl.NSB.update("users").set("firstName", "John").set("lastName", "Doe").set(Map.of("age", 30)).where(Filters.eq("id", 1)).build().query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("age"));

            // Test update with Map
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("age", 25);

            sql = Dsl.NSB.update("users").set(updates).where(Filters.eq("id", 2)).build().query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("age"));
        }

        @Test
        public void testWhereOperations() {
            // Test single where
            String sql = Dsl.NSB.select("*").from("users").where(Filters.eq("status", "active")).build().query();

            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));

            // Test multiple where (should be AND'ed)
            sql = Dsl.NSB.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();

            Assertions.assertTrue(sql.contains("AND"));

            // Test where with OR
            sql = Dsl.NSB.select("*").from("users").where(Filters.or(Filters.eq("status", "active"), Filters.eq("status", "premium"))).build().query();

            Assertions.assertTrue(sql.contains("OR"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = Dsl.NSB.select("department", "COUNT(*) as count")
                    .from("users")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));

            // Test multiple group by
            sql = Dsl.NSB.select("department", "location", "COUNT(*)").from("users").groupBy("department", "location").build().query();

            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("department"));
            Assertions.assertTrue(sql.contains("location"));
        }

        @Test
        public void testOrderBy() {
            // Test simple order by
            String sql = Dsl.NSB.select("*").from("users").orderBy("name").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY name"));

            // Test order by with direction
            sql = Dsl.NSB.select("*").from("users").orderBy("name ASC", "age DESC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("name ASC"));
            Assertions.assertTrue(sql.contains("age DESC"));
        }

        @Test
        public void testLimitOffset() {
            // Test limit only
            String sql = Dsl.NSB.select("*").from("users").limit(10).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));

            // Test limit with offset
            sql = Dsl.NSB.select("*").from("users").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with count and offset
            sql = Dsl.NSB.select("*").from("users").limit(10, 5).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testForUpdate() {
            String sql = Dsl.NSB.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testUnion() {
            SqlBuilder builder1 = Dsl.NSB.select("name").from("users");
            SqlBuilder builder2 = Dsl.NSB.select("name").from("customers");

            String sql1 = builder1.build().query();
            String sql2 = builder2.build().query();

            // Union would be manual construction
            String unionSql = sql1 + " UNION " + sql2;
            Assertions.assertTrue(unionSql.contains("UNION"));
        }

        @Test
        public void testAppend() {
            String sql = Dsl.NSB.select("*").from("users").append(" WHERE custom_condition = true").build().query();

            Assertions.assertTrue(sql.contains("custom_condition"));

            // Test multiple appends
            sql = Dsl.NSB.select("*").from("users").append(" WHERE 1=1").append(" AND status = 'active'").append(" ORDER BY name").build().query();

            Assertions.assertTrue(sql.contains("1=1"));
            Assertions.assertTrue(sql.contains("status = 'active'"));
            Assertions.assertTrue(sql.contains("ORDER BY name"));
        }

        @Test
        public void testComplexConditions() {
            // Test nested AND/OR
            String sql = Dsl.NSB.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.lt("age", 65)),
                            Filters.and(Filters.eq("status", "premium"), Filters.isNotNull("subscription_id"))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NSB.select("*").from("users").where(Filters.isNull("email")).build().query();
            Assertions.assertTrue(sql.contains("IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NSB.select("*").from("users").where(Filters.isNotNull("email")).build().query();
            Assertions.assertTrue(sql.contains("IS NOT NULL"));

            // Test IN
            sql = Dsl.NSB.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).build().query();
            Assertions.assertTrue(sql.contains("IN"));

            // Test NOT IN
            sql = Dsl.NSB.select("*").from("users").where(Filters.notIn("status", Arrays.asList("inactive", "banned"))).build().query();
            Assertions.assertTrue(sql.contains("NOT IN"));

            // Test BETWEEN
            sql = Dsl.NSB.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("BETWEEN"));

            // Test LIKE
            sql = Dsl.NSB.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
            Assertions.assertTrue(sql.contains("LIKE"));

            // Test NOT LIKE
            sql = Dsl.NSB.select("*").from("users").where(Filters.notLike("email", "%spam%")).build().query();
            Assertions.assertTrue(sql.contains("NOT LIKE"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = Dsl.NSB.insert("firstName", "lastName").into("users").append(" RETURNING id").build().query();

            Assertions.assertTrue(sql.contains("RETURNING id"));
        }

        @Test
        public void testCaseExpression() {
            String sql = Dsl.NSB.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("CASE"));
            Assertions.assertTrue(sql.contains("WHEN"));
            Assertions.assertTrue(sql.contains("THEN"));
            Assertions.assertTrue(sql.contains("ELSE"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = Dsl.NSB.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employees")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("ROW_NUMBER()"));
            Assertions.assertTrue(sql.contains("OVER"));
            Assertions.assertTrue(sql.contains("PARTITION BY"));
        }

        @Test
        public void testDistinct() {
            String sql = Dsl.NSB.select("DISTINCT department").from("users").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));

            // Test with multiple columns
            sql = Dsl.NSB.select("DISTINCT department", "location").from("users").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = Dsl.NSB
                    .select("COUNT(*) as total", "AVG(salary) as avg_salary", "MAX(salary) as max_salary", "MIN(salary) as min_salary",
                            "SUM(salary) as total_salary")
                    .from("employees")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("COUNT(*)"));
            Assertions.assertTrue(sql.contains("AVG(salary)"));
            Assertions.assertTrue(sql.contains("MAX(salary)"));
            Assertions.assertTrue(sql.contains("MIN(salary)"));
            Assertions.assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        public void testNamedParametersConsistency() {
            // Ensure named parameters use consistent format
            String sql = Dsl.NSB.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.eq("lastName", "Doe"), Filters.gt("age", 18)))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@example.com");

            String sql = Dsl.NSB.insert(user).into("test_user").build().query();

            // Should use property names as parameter names
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testComplexMultiTableQuery() {
            String sql = Dsl.NSB.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
                    .from("users u")
                    .join("departments d")
                    .on("u.department_id = d.id")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.and(Filters.eq("u.active", true), Filters.between("o.created_date", "2023-01-01", "2023-12-31")))
                    .groupBy("u.id", "u.name", "d.id", "d.name")
                    .having(Filters.gt("COUNT(o.id)", 0))
                    .orderBy("totalRevenue DESC")
                    .limit(20)
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("JOIN"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }
    }

    @Nested
    public class NSCTest {

        @Table("users")
        public static class User {
            private long id;
            @Column("first_name")
            private String firstName;
            @Column("last_name")
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String status;
            @Transient
            private String tempField;

            // Getters and setters
            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getTempField() {
                return tempField;
            }

            public void setTempField(String tempField) {
                this.tempField = tempField;
            }
        }

        @Test
        public void testInsertSingleExpression() {
            String sql = Dsl.NSC.insert("name").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES (:name)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.NSC.insert("firstName", "lastName", "email").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = Dsl.NSC.insert(columns).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains(":firstName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = Dsl.NSC.insert(props).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");
            user.setCreatedDate(new Date()); // Should be excluded (ReadOnly)
            user.setTempField("temp"); // Should be excluded (Transient)

            String sql = Dsl.NSC.insert(user).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");

            Set<String> excluded = Set.of("email");
            String sql = Dsl.NSC.insert(user, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.NSC.insert(User.class).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "status");
            String sql = Dsl.NSC.insert(User.class, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto() {
            String sql = Dsl.NSC.insertInto(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = Dsl.NSC.insertInto(User.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            final User john = new User();
            john.setFirstName("John");
            john.setLastName("Doe");
            final User jane = new User();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<User> users = Arrays.asList(john, jane);

            String sql = Dsl.NSC.batchInsert(users).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple parameter sets for batch insert
        }

        @Test
        public void testUpdate() {
            String sql = Dsl.NSC.update("users").set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = Dsl.NSC.update("users", User.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.NSC.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = Dsl.NSC.update(User.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = Dsl.NSC.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = Dsl.NSC.deleteFrom("users", User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.NSC.deleteFrom(User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = Dsl.NSC.select("COUNT(*)").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.NSC.select("firstName", "lastName", "email").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NSC.select(columns).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = Dsl.NSC.select(aliases).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.NSC.select(User.class).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date AS \"createdDate\""));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NSC.select(User.class, true).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = Dsl.NSC.select(User.class, excluded).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = Dsl.NSC.select(User.class, true, excluded).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFrom() {
            String sql = Dsl.NSC.selectFrom(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = Dsl.NSC.selectFrom(User.class, "u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM users u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = Dsl.NSC.selectFrom(User.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NSC.selectFrom(User.class, "u", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = Dsl.NSC.selectFrom(User.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = Dsl.NSC.selectFrom(User.class, "u", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("users u"));
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = Dsl.NSC.selectFrom(User.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = Dsl.NSC.selectFrom(User.class, "u", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.NSC.select(User.class, "u", "user", User.class, "m", "manager")
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.id AS \"user.id\""));
            Assertions.assertTrue(sql.contains("m.id AS \"manager.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = Dsl.NSC.select(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM)
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, Set.of("status")));

            String sql = Dsl.NSC.select(selections).from("users u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.NSC.selectFrom(User.class, "u", "user", User.class, "m", "manager").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = Dsl.NSC.selectFrom(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, null));

            String sql = Dsl.NSC.selectFrom(selections).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = Dsl.NSC.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains(":active"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.NSC.count(User.class).where(Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18))).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18), Filters.like("email", "%@example.com"));
            String sql = Dsl.NSC.renderCondition(cond, User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
            Assertions.assertTrue(sql.contains("email LIKE :email"));
        }

        @Test
        public void testNamingPolicySnakeCase() {
            // NSC uses SNAKE_CASE naming policy
            String sql = Dsl.NSC.select("firstName", "lastName").from("userAccounts").build().query();

            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = Dsl.NSC.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.eq("u.active", true))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"u.firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"u.lastName\""));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = Dsl.NSC.update("users").set(updates).where(Filters.eq("id", 1)).build().query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("last_name = :lastName"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = Dsl.NSC.batchInsert(data).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            // Ensure named parameters follow the correct pattern
            String sql = Dsl.NSC.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains("status IN (:status1, :status2)"));
            Assertions.assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNameTransformation() {
            // Test that camelCase property names are converted to snake_case
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = Dsl.NSC.insert(user).into("users").build().query();

            // Column names should be snake_case
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));

            // Parameter names should remain camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSC.select(new ArrayList<Selection>());
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSC.select(new HashMap<>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSC.renderCondition(null, User.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            // Test all join variations
            String sql;

            sql = Dsl.NSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = Dsl.NSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = Dsl.NSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = Dsl.NSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = Dsl.NSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = Dsl.NSC.select("*").from("users").crossJoin("departments").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = Dsl.NSC.select("*").from("users").naturalJoin("user_profiles").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = Dsl.NSC.select("*").from("users").append(" WHERE MATCH(name) AGAINST (:search IN BOOLEAN MODE)").build().query();

            Assertions.assertTrue(sql.contains("MATCH"));
            Assertions.assertTrue(sql.contains("AGAINST"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = Dsl.NSC.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "ACTIVE"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "PREMIUM"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = Dsl.NSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = Dsl.NSC.select("*").from("users").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = Dsl.NSC.select("*").from("users").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }
    }

    @Nested
    public class NACTest {

        @Table("ACCOUNT")
        public static class Account {
            private long id;
            @Column("FIRST_NAME")
            private String firstName;
            @Column("LAST_NAME")
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdTime;
            @NonUpdatable
            private String password;
            @Transient
            private String sessionId;

            // Getters and setters
            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedTime() {
                return createdTime;
            }

            public void setCreatedTime(Date createdTime) {
                this.createdTime = createdTime;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getSessionId() {
                return sessionId;
            }

            public void setSessionId(String sessionId) {
                this.sessionId = sessionId;
            }
        }

        @Test
        public void testInsertSingleExpression() {
            String sql = Dsl.NAC.insert("FIRST_NAME").into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES (:FIRST_NAME)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.NAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NAC.insert(columns).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = Dsl.NAC.insert(props).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date()); // Should be excluded
            account.setSessionId("123"); // Should be excluded

            String sql = Dsl.NAC.insert(account).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertFalse(sql.contains("CREATED_TIME"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = Dsl.NAC.insert(account, exclude).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.NAC.insert(Account.class).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("PASSWORD"));
            Assertions.assertFalse(sql.contains("CREATED_TIME"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = Dsl.NAC.insert(Account.class, excluded).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testInsertInto() {
            String sql = Dsl.NAC.insertInto(Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = Dsl.NAC.insertInto(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testBatchInsert() {
            final Account john = new Account();
            john.setFirstName("John");
            john.setLastName("Doe");
            final Account jane = new Account();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<Account> accounts = Arrays.asList(john, jane);

            String sql = Dsl.NAC.batchInsert(accounts).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = Dsl.NAC.update("ACCOUNT").set("STATUS", "ACTIVE").where(Filters.eq("ID", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = Dsl.NAC.update("ACCOUNT", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.NAC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = Dsl.NAC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = Dsl.NAC.deleteFrom("ACCOUNT").where(Filters.eq("STATUS", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = Dsl.NAC.deleteFrom("ACCOUNT", Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.NAC.deleteFrom(Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = Dsl.NAC.select("COUNT(*)").from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.NAC.select("firstName", "lastName", "email").from("ACCOUNT").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NAC.select(columns).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = Dsl.NAC.select(aliases).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.NAC.select(Account.class).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("CREATED_TIME AS \"createdTime\""));
            Assertions.assertTrue(sql.contains("PASSWORD"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NAC.select(Account.class, true).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.select(Account.class, excluded).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.select(Account.class, true, excluded).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFrom() {
            String sql = Dsl.NAC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, "a", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("ACCOUNT a"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, "a", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.NAC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.ID AS \"account.id\""));
            Assertions.assertTrue(sql.contains("o.ID AS \"order.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NAC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NAC.select(selections).from("ACCOUNT a").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a", "account", Account.class, "o", "order")
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NAC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NAC.selectFrom(selections).where(Filters.eq("a.ID", "o.ACCOUNT_ID")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = Dsl.NAC.count("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.NAC.count(Account.class).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :status"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.NAC.renderCondition(cond, Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("STATUS = :status"));
            Assertions.assertTrue(sql.contains("BALANCE > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyUpperCase() {
            // NAC uses SCREAMING_SNAKE_CASE naming policy
            String sql = Dsl.NAC.select("firstName", "lastName").from("userAccounts").build().query();

            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = Dsl.NAC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.ID", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.ID)", 5))
                    .orderBy("COUNT(o.ID) DESC")
                    .limit(10)
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.FIRST_NAME AS \"a.firstName\""));
            Assertions.assertTrue(sql.contains("a.LAST_NAME AS \"a.lastName\""));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = Dsl.NAC.update("ACCOUNT").set(updates).where(Filters.eq("ID", 1)).build().query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
            Assertions.assertTrue(sql.contains("LAST_NAME = :lastName"));
            Assertions.assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = Dsl.NAC.batchInsert(data).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains("AGE BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNameTransformation() {
            // Test that camelCase property names are converted to SCREAMING_SNAKE_CASE
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = Dsl.NAC.insert(account).into("ACCOUNT").build().query();

            // Column names should be UPPER_CASE
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));

            // Parameter names should remain camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NAC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NAC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NAC.select(new ArrayList<Selection>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NAC.renderCondition(null, Account.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            String sql;

            sql = Dsl.NAC.select("*").from("ACCOUNT a").join("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").innerJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").leftJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").rightJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").fullJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT").crossJoin("DEPARTMENT").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT").naturalJoin("ACCOUNT_PROFILE").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").append(" WHERE CUSTOM_FUNCTION(NAME) = TRUE").build().query();

            Assertions.assertTrue(sql.contains("CUSTOM_FUNCTION"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.or(Filters.and(Filters.eq("STATUS", "ACTIVE"), Filters.or(Filters.lt("AGE", 18), Filters.gt("AGE", 65))),
                            Filters.and(Filters.eq("STATUS", "PREMIUM"), Filters.between("AGE", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":STATUS"));
            Assertions.assertTrue(sql.contains(":AGE"));
        }

        @Test
        public void testForUpdate() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.eq("ID", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = Dsl.NAC.select("*").from("ACCOUNT").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testInsertWithUpperCaseColumns() {
            // Test that already uppercase columns remain uppercase
            String sql = Dsl.NAC.insert("FIRST_NAME", "LAST_NAME").into("ACCOUNT").build().query();

            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":FIRST_NAME"));
            Assertions.assertTrue(sql.contains(":LAST_NAME"));
        }

        @Test
        public void testTableNameTransformation() {
            // Test table name is converted to uppercase
            String sql = Dsl.NAC.select("*").from("userAccounts").build().query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on(Filters.and(Filters.eq("a.ID", "o.ACCOUNT_ID"), Filters.gt("o.AMOUNT", 100)))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = Dsl.NAC.select("DEPARTMENT", "COUNT(*) AS CNT")
                    .from("EMPLOYEE")
                    .groupBy("DEPARTMENT")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("GROUP BY DEPARTMENT"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").orderBy("LAST_NAME ASC", "FIRST_NAME ASC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LAST_NAME ASC"));
            Assertions.assertTrue(sql.contains("FIRST_NAME ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.isNull("EMAIL")).build().query();
            Assertions.assertTrue(sql.contains("EMAIL IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.isNotNull("EMAIL")).build().query();
            Assertions.assertTrue(sql.contains("EMAIL IS NOT NULL"));

            // Test IN
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.in("STATUS", Arrays.asList("ACTIVE", "PREMIUM"))).build().query();
            Assertions.assertTrue(sql.contains("STATUS IN"));

            // Test BETWEEN
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.between("AGE", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("AGE BETWEEN"));

            // Test LIKE
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.like("NAME", "%JOHN%")).build().query();
            Assertions.assertTrue(sql.contains("NAME LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
        }

    }

    @Nested
    public class NLCTest extends TestBase {

        @Table("account")
        public static class Account {
            private long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdTime;
            @NonUpdatable
            private String password;
            @Transient
            private String sessionToken;

            // Getters and setters
            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedTime() {
                return createdTime;
            }

            public void setCreatedTime(Date createdTime) {
                this.createdTime = createdTime;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getSessionToken() {
                return sessionToken;
            }

            public void setSessionToken(String sessionToken) {
                this.sessionToken = sessionToken;
            }
        }

        @Test
        public void testInsertSingleExpression() {
            String sql = Dsl.NLC.insert("firstName").into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES (:firstName)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.NLC.insert("firstName", "lastName", "email").into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NLC.insert(columns).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = Dsl.NLC.insert(props).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date()); // Should be excluded
            account.setSessionToken("123"); // Should be excluded

            String sql = Dsl.NLC.insert(account).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = Dsl.NLC.insert(account, exclude).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.NLC.insert(Account.class).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = Dsl.NLC.insert(Account.class, excluded).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertInto() {
            String sql = Dsl.NLC.insertInto(Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = Dsl.NLC.insertInto(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            final Account john = new Account();
            john.setFirstName("John");
            john.setLastName("Doe");
            final Account jane = new Account();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<Account> accounts = Arrays.asList(john, jane);

            String sql = Dsl.NLC.batchInsert(accounts).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = Dsl.NLC.update("account").set("status", "active").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = Dsl.NLC.update("account", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.NLC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = Dsl.NLC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = Dsl.NLC.deleteFrom("account").where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = Dsl.NLC.deleteFrom("account", Account.class).where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.NLC.deleteFrom(Account.class).where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = Dsl.NLC.select("COUNT(*)").from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.NLC.select("firstName", "lastName", "email").from("account").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("active = :active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NLC.select(columns).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = Dsl.NLC.select(aliases).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.NLC.select(Account.class).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("createdTime"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NLC.select(Account.class, true).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "createdTime");
            String sql = Dsl.NLC.select(Account.class, excluded).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.select(Account.class, true, excluded).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = Dsl.NLC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("active = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM account a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, "a", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, "a", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.NLC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.id AS \"account.id\""));
            Assertions.assertTrue(sql.contains("o.id AS \"order.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NLC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NLC.select(selections).from("account a").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a", "account", Account.class, "o", "order")
                    .where(Filters.eq("a.id", "o.accountId"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NLC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.id", "o.accountId"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NLC.selectFrom(selections).where(Filters.eq("a.id", "o.accountId")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = Dsl.NLC.count("account").where(Filters.eq("status", "active")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.NLC.count(Account.class).where(Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000))).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000));
            String sql = Dsl.NLC.renderCondition(cond, Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyCamelCase() {
            // NLC uses CAMEL_CASE naming policy
            String sql = Dsl.NLC.select("firstName", "lastName").from("userAccounts").build().query();

            // Should preserve camelCase
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = Dsl.NLC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("account a")
                    .leftJoin("order o")
                    .on("a.id = o.accountId")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.id", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.firstName"));
            Assertions.assertTrue(sql.contains("a.lastName"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = Dsl.NLC.update("account").set(updates).where(Filters.eq("id", 1)).build().query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName = :firstName"));
            Assertions.assertTrue(sql.contains("lastName = :lastName"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = Dsl.NLC.batchInsert(data).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("active", "premium")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains("status IN (:status1, :status2)"));
            Assertions.assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNamePreservation() {
            // Test that camelCase property names are preserved
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = Dsl.NLC.insert(account).into("account").build().query();

            // Column names should remain camelCase
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));

            // Parameter names should also be camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NLC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NLC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NLC.select(new ArrayList<Selection>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NLC.renderCondition(null, Account.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            String sql;

            sql = Dsl.NLC.select("*").from("account a").join("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = Dsl.NLC.select("*").from("account a").innerJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = Dsl.NLC.select("*").from("account a").leftJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = Dsl.NLC.select("*").from("account a").rightJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = Dsl.NLC.select("*").from("account a").fullJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = Dsl.NLC.select("*").from("account").crossJoin("department").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = Dsl.NLC.select("*").from("account").naturalJoin("accountProfile").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = Dsl.NLC.select("*").from("account").append(" WHERE customFunction(name) = true").build().query();

            Assertions.assertTrue(sql.contains("customFunction"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "premium"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = Dsl.NLC.select("*").from("account").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = Dsl.NLC.select("*").from("account").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = Dsl.NLC.select("*").from("account").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testTableNamePreservation() {
            // Test table name preserves camelCase
            String sql = Dsl.NLC.select("*").from("userAccounts").build().query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account a")
                    .leftJoin("order o")
                    .on(Filters.and(Filters.eq("a.id", "o.accountId"), Filters.gt("o.amount", 100)))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = Dsl.NLC.select("department", "COUNT(*) as cnt")
                    .from("employee")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = Dsl.NLC.select("*").from("account").orderBy("lastName ASC", "firstName ASC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("lastName ASC"));
            Assertions.assertTrue(sql.contains("firstName ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NLC.select("*").from("account").where(Filters.isNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NLC.select("*").from("account").where(Filters.isNotNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = Dsl.NLC.select("*").from("account").where(Filters.in("status", Arrays.asList("active", "premium"))).build().query();
            Assertions.assertTrue(sql.contains("status IN"));

            // Test BETWEEN
            sql = Dsl.NLC.select("*").from("account").where(Filters.between("age", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("age BETWEEN"));

            // Test LIKE
            sql = Dsl.NLC.select("*").from("account").where(Filters.like("name", "%John%")).build().query();
            Assertions.assertTrue(sql.contains("name LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testDistinct() {
            String sql = Dsl.NLC.select("DISTINCT department").from("employee").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = Dsl.NLC
                    .select("COUNT(*) as total", "AVG(salary) as avgSalary", "MAX(salary) as maxSalary", "MIN(salary) as minSalary",
                            "SUM(salary) as totalSalary")
                    .from("employee")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("COUNT(*)"));
            Assertions.assertTrue(sql.contains("AVG(salary)"));
            Assertions.assertTrue(sql.contains("MAX(salary)"));
            Assertions.assertTrue(sql.contains("MIN(salary)"));
            Assertions.assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = Dsl.NLC.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employee")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("OVER (PARTITION BY department ORDER BY salary DESC) AS rank"));
        }

        @Test
        public void testCaseExpression() {
            String sql = Dsl.NLC.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("case when age < 18 then 'Minor' when age < 65 then 'Adult' else 'Senior' end AS category"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = Dsl.NLC.insert("firstName", "lastName").into("users").append(" RETURNING id").build().query();

            Assertions.assertTrue(sql.contains("RETURNING id"));
        }

        @Test
        public void testUpdateMultipleColumns() {
            String sql = Dsl.NLC.update("users")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("age", 30))
                    .set("email", "john@example.com")
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testComplexMultiTableQuery() {
            String sql = Dsl.NLC.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
                    .from("users u")
                    .join("departments d")
                    .on("u.departmentId = d.id")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(Filters.and(Filters.eq("u.active", true), Filters.between("o.createdDate", "2023-01-01", "2023-12-31")))
                    .groupBy("u.id", "u.name", "d.id", "d.name")
                    .having(Filters.gt("COUNT(o.id)", 0))
                    .orderBy("totalRevenue DESC")
                    .limit(20)
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("JOIN"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionAll() {
            // Test building queries that could be used in UNION ALL
            String sql1 = Dsl.NLC.select("name", "email").from("users").build().query();
            String sql2 = Dsl.NLC.select("name", "email").from("customers").build().query();

            // Manual UNION ALL construction
            String unionAllSql = sql1 + " UNION ALL " + sql2;
            Assertions.assertTrue(unionAllSql.contains("UNION ALL"));
        }

        @Test
        public void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            Account account = new Account();
            account.setFirstName("Test");
            account.setLastName("User");
            account.setEmail("test@example.com");

            String sql = Dsl.NLC.insert(account).into("account").build().query();

            // Should use property names as both column and parameter names
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));

            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + 86400000); // 1 day later

            sql = Dsl.NLC.selectFrom(Order.class, "ord", true).where(Filters.between("ord.orderDate", startDate, endDate)).build().query();
            // Output: SELECT ord.id, ord.orderNumber, ord.amount, ord.status,
            //                acc.id AS "account.id", acc.firstName AS "account.firstName"
            //         FROM orders ord
            //         LEFT JOIN account acc ON ord.accountId = acc.id
            //         WHERE ord.orderDate BETWEEN :startDate AND :endDate
            N.println(sql);
        }
    }

    @Nested
    public class MSBTest extends TestBase {

        @Table("test_account")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            String sql = Dsl.MSB.insert("name").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.MSB.insert("firstName", "lastName", "email").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, email)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "status");
            String sql = Dsl.MSB.insert(columns).into("products").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO products"));
            Assertions.assertTrue(sql.contains("(id, name, status)"));
            Assertions.assertTrue(sql.contains("#{id}"));
            Assertions.assertTrue(sql.contains("#{name}"));
            Assertions.assertTrue(sql.contains("#{status}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 30);
            String sql = Dsl.MSB.insert(props).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains("#{name}"));
            Assertions.assertTrue(sql.contains("#{age}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = Dsl.MSB.insert(account).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient field should be excluded
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly field should be excluded
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.insert(account, excludes).into("users").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.MSB.insert(Account.class).into("test_account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSB.insert(Account.class, excludes).into("test_account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = Dsl.MSB.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.insertInto(Account.class, excludes).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testBatchInsert() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> props1 = new HashMap<>();
            props1.put("name", "John");
            props1.put("age", 30);
            Map<String, Object> props2 = new HashMap<>();
            props2.put("name", "Jane");
            props2.put("age", 25);
            propsList.add(props1);
            propsList.add(props2);

            String sql = Dsl.MSB.batchInsert(propsList).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name_0}"));
            Assertions.assertTrue(sql.contains("#{name_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = Dsl.MSB.update("users").set("status", "lastModified").where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("lastModified = #{lastModified}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = Dsl.MSB.update("user_archive", Account.class).set("firstName").where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE user_archive"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.MSB.update(Account.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
            Assertions.assertFalse(sql.contains("nonUpdatableField")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSB.update(Account.class, excludes).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = Dsl.MSB.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MSB.deleteFrom("users", Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.MSB.deleteFrom(Account.class).where(Filters.lt("createdDate", new Date())).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM test_account"));
            Assertions.assertTrue(sql.contains("createdDate < #{createdDate}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = Dsl.MSB.select("COUNT(*)").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.MSB.select("firstName", "lastName", "email").from("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, email"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "price");
            String sql = Dsl.MSB.select(columns).from("products").build().query();
            Assertions.assertTrue(sql.contains("SELECT id, name, price"));
            Assertions.assertTrue(sql.contains("FROM products"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = Dsl.MSB.select(aliases).from("users").build().query();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.MSB.select(Account.class).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MSB.select(Account.class, true).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.select(Account.class, excludes).from("users").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = Dsl.MSB.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MSB.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertTrue(sql.contains("FROM test_account a"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MSB.selectFrom(Account.class, excludes).where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.MSB.select(Account.class, "a", "account", Account.class, "b", "account2").from("test_account a, test_account b").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testCountTable() {
            String sql = Dsl.MSB.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.MSB.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            N.println(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
            String sql = Dsl.MSB.renderCondition(cond, Account.class).build().query();
            Assertions.assertTrue(sql.contains("active = #{active}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }
    }

    @Nested
    public class MSCTest extends TestBase {

        @Table("test_users")
        public static class User {
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            String sql = Dsl.MSC.insert("userName").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(user_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{userName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.MSC.insert("firstName", "lastName", "emailAddress").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name, email_address)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> props = Arrays.asList("firstName", "lastName");
            String sql = Dsl.MSC.insert(props).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("firstName", "John");
            data.put("lastName", "Doe");
            String sql = Dsl.MSC.insert(data).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = Dsl.MSC.insert(user).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmailAddress("john@example.com");

            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.insert(user, exclude).into("users").build().query();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email_address"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.MSC.insert(User.class).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email_address"));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = Dsl.MSC.insert(User.class, exclude).into("users").build().query();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = Dsl.MSC.insertInto(User.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id"));
            String sql = Dsl.MSC.insertInto(User.class, exclude).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_users"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = new ArrayList<>();
            User user1 = new User();
            user1.setFirstName("John");
            user1.setLastName("Doe");
            User user2 = new User();
            user2.setFirstName("Jane");
            user2.setLastName("Smith");
            users.add(user1);
            users.add(user2);

            String sql = Dsl.MSC.batchInsert(users).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = Dsl.MSC.update("users").set("firstName", "lastName").where(Filters.eq("userId", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = Dsl.MSC.update("users", User.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.MSC.update(User.class).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
            Assertions.assertFalse(sql.contains("non_updatable_field")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSC.update(User.class, exclude).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = Dsl.MSC.deleteFrom("users").where(Filters.eq("userId", 123)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MSC.deleteFrom("users", User.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.MSC.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM test_users"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = Dsl.MSC.select("COUNT(*)").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.MSC.select("firstName", "lastName", "emailAddress").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.MSC.select(columns).from("users").build().query();
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = Dsl.MSC.select(aliases).from("users").build().query();
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.MSC.select(User.class).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MSC.select(User.class, true).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.select(User.class, exclude).from("users").build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = Dsl.MSC.selectFrom(User.class).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MSC.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            // Table-alias prefix is stripped from the named-parameter identifier so the
            // placeholder remains a valid iBATIS expression (#{active} not #{u.active}).
            Assertions.assertTrue(sql.contains("u.active = #{active}"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MSC.selectFrom(User.class, exclude).build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, true, exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", true, exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.MSC.select(User.class, "u", "user", User.class, "u2", "user2").from("test_users u, test_users u2").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password", "emailAddress"));
            String sql = Dsl.MSC.select(User.class, "u", "user", exclude, User.class, "u2", "user2", exclude)
                    .from("test_users u, test_users u2")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectWithMultiSelects() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));
            String sql = Dsl.MSC.select(selections).from("test_users u, test_users u2").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.MSC.selectFrom(User.class, "u", "user", User.class, "u2", "user2").build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> exclude1 = new HashSet<>(Arrays.asList("password"));
            Set<String> exclude2 = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", "user", exclude1, User.class, "u2", "user2", exclude2).build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromWithMultiSelects() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));
            String sql = Dsl.MSC.selectFrom(selections).build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testCountTable() {
            String sql = Dsl.MSC.count("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.MSC.count(User.class).where(Filters.gt("age", 18)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18));
            String sql = Dsl.MSC.renderCondition(cond, User.class).build().query();
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }
    }

    @Nested
    public class MACTest extends TestBase {

        @Table("ACCOUNT")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            String sql = Dsl.MAC.insert("firstName").into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.MAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = Dsl.MAC.insert(columns).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = Dsl.MAC.insert(props).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = Dsl.MAC.insert(account).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
            Assertions.assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id"));
            String sql = Dsl.MAC.insert(account, excludes).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.MAC.insert(Account.class).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
            Assertions.assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = Dsl.MAC.insert(Account.class, excludes).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = Dsl.MAC.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MAC.insertInto(Account.class, excludes).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = Dsl.MAC.batchInsert(accounts).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
            Assertions.assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = Dsl.MAC.update("ACCOUNT")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            Assertions.assertTrue(sql.contains("LAST_NAME = #{lastName}"));
            Assertions.assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = Dsl.MAC.update("ACCOUNT", Account.class)
                    .set(Map.of("isActive", false))
                    .set(Map.of("deactivatedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("DEACTIVATED_DATE = #{deactivatedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.MAC.update(Account.class)
                    .set("status", "ACTIVE")
                    .set(Map.of("lastLoginDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE = #{lastLoginDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
            String sql = Dsl.MAC.update(Account.class, excludes)
                    .set("firstName", "John")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            Assertions.assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = Dsl.MAC.deleteFrom("ACCOUNT").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MAC.deleteFrom("ACCOUNT", Account.class)
                    .where(Filters.and(Filters.eq("isActive", false), Filters.lt("lastLoginDate", new Date())))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE < #{lastLoginDate}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.MAC.deleteFrom(Account.class).where(Filters.in("id", Arrays.asList(1, 2, 3))).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID IN"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = Dsl.MAC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("ACCOUNT").build().query();
            Assertions.assertEquals("SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM ACCOUNT", sql);
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.MAC.select("firstName", "lastName", "emailAddress").from("ACCOUNT").where(Filters.gt("createdDate", new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("CREATED_DATE > #{createdDate}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = Dsl.MAC.select(columns).from("ACCOUNT").orderBy("createdDate DESC").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("PHONE_NUMBER AS \"phoneNumber\""));
            Assertions.assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = Dsl.MAC.select(aliases).from("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
            Assertions.assertTrue(sql.contains("EMAIL_ADDRESS AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.MAC.select(Account.class).from("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"id\""));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MAC.select(Account.class, true).from("ACCOUNT").innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
            String sql = Dsl.MAC.select(Account.class, excludes).from("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("passwordHash"));
            Assertions.assertFalse(sql.contains("securityToken"));
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MAC.select(Account.class, true, excludes)
                    .from("ACCOUNT")
                    .innerJoin("PROFILE")
                    .on("ACCOUNT.PROFILE_ID = PROFILE.ID")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = Dsl.MAC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MAC.selectFrom(Account.class, "a")
                    .innerJoin("PROFILE p")
                    .on("a.PROFILE_ID = p.ID")
                    .where(Filters.eq("a.isActive", true))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
            // Table-alias prefix is stripped from the named-parameter identifier so
            // the placeholder remains a valid iBATIS expression.
            Assertions.assertTrue(sql.contains("a.IS_ACTIVE = #{isActive}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = Dsl.MAC.selectFrom(Account.class, true).innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = Dsl.MAC.selectFrom(Account.class, "acc", true).innerJoin("PROFILE p").on("acc.PROFILE_ID = p.ID").build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = Dsl.MAC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MAC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertFalse(sql.contains("password"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            Assertions.assertTrue(sql.contains("a.EMAIL_ADDRESS LIKE #{emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = Dsl.MAC.selectFrom(Account.class, true, excludes).innerJoin("ORDERS").on("ACCOUNT.ID = ORDERS.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertFalse(sql.contains("temporaryData"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = Dsl.MAC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("ORDERS o")
                    .on("acc.ID = o.ACCOUNT_ID")
                    .innerJoin("ITEMS i")
                    .on("o.ID = i.ORDER_ID")
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertFalse(sql.contains("debugInfo"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            Assertions.assertTrue(sql.contains("O.TOTAL > #{total}"));
        }

        @Test
        public void testCountTable() {
            String sql = Dsl.MAC.count("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.MAC.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("CREATED_DATE BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.MAC.renderCondition(cond, Account.class).build().query();
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("BALANCE > #{balance}"));
        }
    }

    @Nested
    public class MLCTest extends TestBase {

        @Table("account")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Boolean isActive;
            private Date createdDate;
            private Date modifiedDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

            // Getters and setters
            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Boolean getIsActive() {
                return isActive;
            }

            public void setIsActive(Boolean isActive) {
                this.isActive = isActive;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public Date getModifiedDate() {
                return modifiedDate;
            }

            public void setModifiedDate(Date modifiedDate) {
                this.modifiedDate = modifiedDate;
            }
        }

        @Test
        public void testInsertSingleColumn() {
            String sql = Dsl.MLC.insert("firstName").into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = Dsl.MLC.insert("firstName", "lastName", "emailAddress").into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, emailAddress)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = Dsl.MLC.insert(columns).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, phoneNumber)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{phoneNumber}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);
            String sql = Dsl.MLC.insert(props).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("isActive"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{isActive}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedDate(new Date());

            String sql = Dsl.MLC.insert(account).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MLC.insert(account, excludes).into("account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = Dsl.MLC.insert(Account.class).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "auditFields"));
            String sql = Dsl.MLC.insert(Account.class, excludes).into("account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = Dsl.MLC.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MLC.insertInto(Account.class, excludes).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = Dsl.MLC.batchInsert(accounts).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
            Assertions.assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = Dsl.MLC.update("account")
                    .set("firstName", "updatedName")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("modifiedDate = #{modifiedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = Dsl.MLC.update("account", Account.class).set("firstName", "John").set("lastName", "Doe").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = Dsl.MLC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
            String sql = Dsl.MLC.update(Account.class, excludes).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = Dsl.MLC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MLC.deleteFrom("account", Account.class).where(Filters.eq("emailAddress", "john@example.com")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("emailAddress = #{emailAddress}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = Dsl.MLC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = Dsl.MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("account").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total, MAX(createdDate) AS latest"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = Dsl.MLC.select("firstName", "lastName", "emailAddress").from("account").where(Filters.gt("createdDate", new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, emailAddress"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate > #{createdDate}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = getColumnsToSelect();
            String sql = Dsl.MLC.select(columns).from("account").orderBy("createdDate DESC").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        private List<String> getColumnsToSelect() {
            return Arrays.asList("firstName", "lastName", "emailAddress");
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = Dsl.MLC.select(aliases).from("account").build().query();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
            Assertions.assertTrue(sql.contains("emailAddress AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = Dsl.MLC.select(Account.class).from("account").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertTrue(sql.contains("isActive"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MLC.select(Account.class, true).from("account").innerJoin("address").on("account.addressId = address.id").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
            String sql = Dsl.MLC.select(Account.class, excludes).from("account").build().query();
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("secretKey"));
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MLC.select(Account.class, true, excludes)
                    .from("account")
                    .innerJoin("profile")
                    .on("account.profileId = profile.id")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = Dsl.MLC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("isActive = #{isActive}"));
            Assertions.assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MLC.selectFrom(Account.class, "a")
                    .innerJoin("profile p")
                    .on("a.profileId = p.id")
                    .where(Filters.like("a.emailAddress", "%@example.com"))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            Assertions.assertTrue(sql.contains("a.emailAddress LIKE #{emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = Dsl.MLC.selectFrom(Account.class, true).innerJoin("address").on("account.addressId = address.id").build().query();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = Dsl.MLC.selectFrom(Account.class, "acc", true).innerJoin("profile p").on("acc.profileId = p.id").build().query();
            Assertions.assertTrue(sql.contains("FROM account acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = Dsl.MLC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MLC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = Dsl.MLC.selectFrom(Account.class, true, excludes).innerJoin("orders").on("account.id = orders.accountId").build().query();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertFalse(sql.contains("temporaryData"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = Dsl.MLC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("orders o")
                    .on("acc.id = o.accountId")
                    .innerJoin("items i")
                    .on("o.id = i.orderId")
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM account acc"));
            Assertions.assertFalse(sql.contains("debugInfo"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            Assertions.assertTrue(sql.contains("o.total > #{total}"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = Dsl.MLC.select(Account.class, "a", "account", Account.class, "a2", "account2")
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("password"));
            Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MLC.select(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", orderExcludes)
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .where(Filters.gt("a.createdDate", new Date()))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectWithMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, true, null),
                    selection(Account.class, "a2", "account2", Arrays.asList("id", "name"), false, null));
            String sql = Dsl.MLC.select(selections).from("account a").innerJoin("account a2").on("a.id = a2.parentId").build().query();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = Dsl.MLC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2")
                    .innerJoin("a.id = a2.parentId")
                    .where(Filters.gt("a.createdDate", new Date()))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
            String sql = Dsl.MLC.selectFrom(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", null)
                    .innerJoin("a.id = a2.parentId")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("sensitiveData"));
        }

        @Test
        public void testSelectFromWithMultipleSelections() {
            List<Selection> selections = createComplexSelections();
            String sql = Dsl.MLC.selectFrom(selections)
                    .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 0)))
                    .groupBy("account.type")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY account.type"));
            Assertions.assertTrue(sql.contains("HAVING COUNT(*) > #{COUNT}"));
        }

        @Test
        public void testNamedHavingFunctionExpressionUsesValidPlaceholder() {
            AbstractQueryBuilder.SP sp = Dsl.NLC.select("department", "COUNT(*)")
                    .from("employees")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build();

            Assertions.assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > :COUNT", sp.query());
            Assertions.assertEquals(Arrays.asList(5), sp.parameters());
        }

        private List<Selection> createComplexSelections() {
            return Arrays.asList(selection(Account.class, "a", "account", null, false, null));
        }

        @Test
        public void testCountTable() {
            String sql = Dsl.MLC.count("account").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("isActive = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = Dsl.MLC.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.MLC.renderCondition(cond, Account.class).build().query();
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("balance > #{balance}"));
        }
    }

    @Test
    public void testSqlBuilder_PSC_simpleSelect() {
        String sql = Dsl.PSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_updateWithConditions() {
        String sql = Dsl.PSC.update("account").set("name", "status").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE account"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testSqlBuilder_PSC_deleteFrom() {
        String sql = Dsl.PSC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithWhere() {
        String sql = Dsl.PSC.select("id", "name").from("users").where(Filters.gt("age", 18)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithJoin() {
        String sql = Dsl.PSC.select("u.id", "u.name", "o.total").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithGroupBy() {
        String sql = Dsl.PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithOrderBy() {
        String sql = Dsl.PSC.select("id", "name").from("users").orderBy("name").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithLimit() {
        String sql = Dsl.PSC.select("id", "name").from("users").limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithBetween() {
        String sql = Dsl.PSC.select("id", "name", "age").from("users").where(Filters.between("age", 18, 65)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithIn() {
        String sql = Dsl.PSC.select("id", "name").from("users").where(Filters.in("status", Arrays.asList("active", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithLike() {
        String sql = Dsl.PSC.select("id", "name", "email").from("users").where(Filters.like("email", "%@company.com")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithIsNull() {
        String sql = Dsl.PSC.select("id", "name").from("users").where(Filters.isNull("deleted_at")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithAndOr() {
        String sql = Dsl.PSC.select("id", "name")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.gt("age", 18), Filters.eq("verified", true))))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithRightJoin() {
        String sql = Dsl.PSC.select("u.id", "o.total").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithFullJoin() {
        String sql = Dsl.PSC.select("u.id", "o.total").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithInnerJoin() {
        String sql = Dsl.PSC.select("u.id", "o.total").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithCrossJoin() {
        String sql = Dsl.PSC.select("u.id", "p.name").from("users u").crossJoin("products p").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithHaving() {
        String sql = Dsl.PSC.select("department", "COUNT(*) AS cnt")
                .from("employees")
                .groupBy("department")
                .having(Filters.expr("COUNT(*) > 5"))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithUnion() {
        String sql = Dsl.PSC.select("id", "name").from("active_users").union(Dsl.PSC.select("id", "name").from("archived_users")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithUnionAll() {
        String sql = Dsl.PSC.select("id", "name").from("active_users").unionAll(Dsl.PSC.select("id", "name").from("temp_users")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testSqlBuilder_NSC_simpleSelect() {
        String sql = Dsl.NSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_insertIntoValues() {
        String sql = Dsl.PSC.insert("id", "name", "email").into("users").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    public void testSqlBuilder_PSC_selectDistinct() {
        String sql = Dsl.PSC.select("DISTINCT department").from("employees").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testSqlBuilder_PSC_rejectsLimitAfterOffset() {
        final SqlBuilder builder = Dsl.PSC.select("id", "name").from("users").offset(10);

        assertThrows(IllegalStateException.class, () -> builder.limit(5));
        assertEquals("SELECT id, name FROM users OFFSET 10", builder.build().query());
    }

    @Test
    public void testPAC_UpdateAndDeleteWithEntityClass() {
        String updateSql = Dsl.PAC.update("USER_ACCOUNT", Account.class).set("firstName").where(Filters.eq("id", 1)).build().query();
        String deleteSql = Dsl.PAC.deleteFrom("USER_ACCOUNT", Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(updateSql.contains("UPDATE USER_ACCOUNT"));
        assertTrue(deleteSql.contains("DELETE FROM USER_ACCOUNT"));
    }

    @Test
    public void testPAC_SelectOverloadsWithExcludedProperties() {
        Set<String> excluded = Collections.singleton("lastName");
        String selectSql = Dsl.PAC.select(Account.class, excluded).from("USER_ACCOUNT").build().query();
        String selectFromSql = Dsl.PAC.selectFrom(Account.class, "ua", true, excluded).build().query();

        assertTrue(selectSql.contains("SELECT"));
        assertTrue(selectSql.contains("FROM USER_ACCOUNT"));
        assertTrue(selectFromSql.contains("FROM"));
        assertTrue(selectFromSql.contains("ua"));
    }

    @Test
    public void testPLC_SelectOverloadsWithTwoEntityClasses() {
        String sql = Dsl.PLC.select(Account.class, "a1", "account1", Account.class, "a2", "account2").from("account1 a1, account2 a2").build().query();

        assertTrue(sql.contains("FROM account1 a1, account2 a2"));
    }

    @Test
    public void testLCSB_SelectFromDefaultEntityClass() {
        String sql = Dsl.LCSB.selectFrom(Account.class).build().query();

        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testMSB_SelectFromMultiSelectionAndBatchInsert() {
        List<Selection> selections = Arrays.asList(selection(Account.class, "a1", "account1", null, false, null),
                selection(Account.class, "a2", "account2", null, false, null));

        SqlBuilder selectBuilder = Dsl.MSB.selectFrom(selections);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new LinkedHashMap<>(Map.of("firstName", "John")));
        rows.add(new LinkedHashMap<>(Map.of("firstName", "Jane")));
        SP batchInsert = Dsl.MSB.batchInsert(rows).into("users").build();

        assertTrue(selectBuilder.build().query().contains("FROM"));
        assertTrue(batchInsert.query().contains("INSERT INTO users"));
    }

    @Test
    public void testMAC_SelectOverloadsWithTableAliases() {
        Set<String> excluded = Collections.singleton("lastName");
        String selectSql = Dsl.MAC.select(Account.class, "a1", "account1", excluded, Account.class, "a2", "account2", excluded)
                .from("ACCOUNT1 a1, ACCOUNT2 a2")
                .build()
                .query();
        String selectFromSql = Dsl.MAC.selectFrom(Account.class, "a1", "account1", Account.class, "a2", "account2").build().query();

        assertTrue(selectSql.contains("FROM ACCOUNT1 a1, ACCOUNT2 a2"));
        assertTrue(selectFromSql.contains("FROM"));
    }

    private static List<Map<String, Object>> batchRows() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        return Arrays.asList(row1, row2);
    }

    private static void assertInsertBuilds(SP sp) {
        assertTrue(sp.query().contains("INSERT INTO users"));
        assertNotNull(sp.parameters());
    }

    private static void assertUpdateBuilds(SP sp, String tableName) {
        assertTrue(sp.query().contains("UPDATE " + tableName));
        assertTrue(sp.query().contains("WHERE"));
    }

    private static void assertDeleteBuilds(SP sp, String tableName) {
        assertTrue(sp.query().contains("DELETE FROM " + tableName));
        assertTrue(sp.query().contains("WHERE"));
    }

    @Test
    public void testBatchInsertVariants_Batch2() {
        List<Map<String, Object>> rows = batchRows();

        List<SP> results = Arrays.asList(Dsl.SCSB.batchInsert(rows).into("users").build(), Dsl.ACSB.batchInsert(rows).into("users").build(),
                Dsl.PSB.batchInsert(rows).into("users").build(), Dsl.PSC.batchInsert(rows).into("users").build(),
                Dsl.NSB.batchInsert(rows).into("users").build(), Dsl.NSC.batchInsert(rows).into("users").build(),
                Dsl.NAC.batchInsert(rows).into("users").build(), Dsl.NLC.batchInsert(rows).into("users").build(),
                Dsl.MSC.batchInsert(rows).into("users").build(), Dsl.MLC.batchInsert(rows).into("users").build());

        for (SP sp : results) {
            assertInsertBuilds(sp);
        }
    }

    @Test
    public void testUpdateTableWithEntityVariants_Batch2() {
        List<SP> results = Arrays.asList(Dsl.SCSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.ACSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.PSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.PSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.NSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.NSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.NAC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.NLC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.MSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                Dsl.MLC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build());

        for (SP sp : results) {
            assertUpdateBuilds(sp, "account");
        }
    }

    @Test
    public void testUpdateEntityWithExcludedPropsVariants_Batch2() {
        Set<String> excluded = Collections.singleton("lastModifiedDate");

        List<SP> results = Arrays.asList(Dsl.SCSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.ACSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.PSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.PSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.NSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.NSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.NAC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.NLC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.MSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                Dsl.MLC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build());

        for (SP sp : results) {
            assertTrue(sp.query().contains("UPDATE"));
            assertTrue(sp.query().contains("WHERE"));
        }
    }

    @Test
    public void testDeleteFromTableWithEntityVariants_Batch2() {
        List<SP> results = Arrays.asList(Dsl.SCSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.ACSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.PSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.PSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.NSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.NSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.NAC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.NLC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.MSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                Dsl.MLC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build());

        for (SP sp : results) {
            assertDeleteBuilds(sp, "account");
        }
    }

    @Test
    public void testMultiEntitySelectVariants_Batch2() {
        Set<String> excludedA = Collections.singleton("lastModifiedDate");
        Set<String> excludedB = Collections.singleton("amount");

        List<String> results = Arrays.asList(
                Dsl.SCSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.ACSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.PSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.PSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.NSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.NSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.NAC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.NLC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.MSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query(),
                Dsl.MLC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                        .from("test_account a1, user_order o1")
                        .build()
                        .query());

        for (String sql : results) {
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM test_account a1, user_order o1"));
        }
    }

    @Test
    public void testMultiEntitySelectFromVariants_Batch2() {
        Set<String> excludedA = Collections.singleton("lastModifiedDate");
        Set<String> excludedB = Collections.singleton("amount");

        List<String> results = Arrays.asList(
                Dsl.SCSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.ACSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.PSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.PSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.NSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.NSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.NAC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.NLC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.MSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                Dsl.MLC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query());

        for (String sql : results) {
            assertTrue(sql.contains("FROM"));
            assertTrue(sql.contains("a1"));
            assertTrue(sql.contains("o1"));
        }
    }

    @Test
    public void testInWithExpressionValueIsEmbeddedNotDropped_Pass3() {
        SP sp = Dsl.PSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList(SqlExpression.of("'ACTIVE'"), "PENDING"))).build();
        assertTrue(sp.query().contains("'ACTIVE'"), "SqlExpression literal must be embedded in IN list: " + sp.query());
        assertTrue(sp.query().contains("?"), "Non-SqlExpression value must still be parameterized: " + sp.query());
        assertEquals(1, sp.parameters().size(), "Only the non-SqlExpression value should be bound; got: " + sp.parameters());
        assertEquals("PENDING", sp.parameters().get(0));
    }

    @Test
    public void testNotInWithExpressionValueIsEmbeddedNotDropped_Pass3() {
        SP sp = Dsl.PSC.select("id").from("test_account").where(Filters.notIn("status", Arrays.asList(SqlExpression.of("'DELETED'"), "ARCHIVED"))).build();
        assertTrue(sp.query().contains("'DELETED'"), "SqlExpression literal must be embedded in NOT IN list: " + sp.query());
        assertTrue(sp.query().contains("NOT IN"));
        assertEquals(1, sp.parameters().size());
        assertEquals("ARCHIVED", sp.parameters().get(0));
    }

    @Test
    public void testInWithAllSimpleValues_BackwardCompatPass3() {
        SP sp = Dsl.PSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList("A", "B", "C"))).build();
        assertTrue(sp.query().endsWith("IN (?, ?, ?)"), "Simple IN should produce three placeholders: " + sp.query());
        assertEquals(3, sp.parameters().size());
        assertEquals(Arrays.asList("A", "B", "C"), sp.parameters());
    }

    @Test
    public void testNamedInWithExpressionValue_Pass3() {
        SP sp = Dsl.NSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList(SqlExpression.of("'ACTIVE'"), "PENDING"))).build();
        assertTrue(sp.query().contains("'ACTIVE'"), "SqlExpression must be embedded in named IN list: " + sp.query());
        assertTrue(sp.query().contains(":"), "Non-SqlExpression value must use named placeholder: " + sp.query());
        assertEquals(1, sp.parameters().size());
        assertEquals("PENDING", sp.parameters().get(0));
    }

    // ===== Pass-4: parameter ordering across JOINs, subqueries, nested conditions =====

    private static int countQmarks(String s) {
        return s.length() - s.replace("?", "").length();
    }

    /**
     * Scenario 1 — JOIN with parameter + WHERE with parameter via Criteria.
     * Verifies join-ON parameters appear before WHERE parameters in the final list,
     * matching the left-to-right order of {@code ?} placeholders in the SQL.
     */
    @Test
    public void testParamOrder_JoinOnWithParamThenWhere_Pass4() {
        com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders o",
                Filters.expr("u.id = o.user_id").and(Filters.eq("o.status", "active")));
        Criteria crit = Criteria.builder().join(innerJoin).where(Filters.gt("u.age", 18)).build();
        SP sp = Dsl.PSC.select("u.id").from("users u").append(crit).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("active", 18), sp.parameters(), "Expected [active, 18] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 2 — WHERE with subquery containing parameter + outer WHERE parameter.
     */
    @Test
    public void testParamOrder_InSubQueryThenAndBinary_Pass4() {
        SubQuery innerSubQuery = Filters.subQuery("departments", Arrays.asList("id"), Filters.eq("region", "EU"));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.in("department_id", innerSubQuery).and(Filters.gt("age", 21))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("EU", 21), sp.parameters(), "Expected [EU, 21] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 4 — Multiple JOINs with parameters, then WHERE with parameter.
     */
    @Test
    public void testParamOrder_MultipleJoinsWithParams_Pass4() {
        com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders o",
                Filters.expr("u.id = o.user_id").and(Filters.gt("o.amount", 100)));
        com.landawn.abacus.query.condition.LeftJoin leftJoin = Filters.leftJoin("payments p",
                Filters.expr("p.order_id = o.id").and(Filters.eq("p.method", "CC")));
        Criteria crit = Criteria.builder().join(innerJoin, leftJoin).where(Filters.eq("u.country", "US")).build();
        SP sp = Dsl.PSC.select("u.id").from("users u").append(crit).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList(100, "CC", "US"), sp.parameters(), "Expected [100, CC, US] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 5 — HAVING with parameter after GROUP BY and WHERE with parameter.
     */
    @Test
    public void testParamOrder_WhereGroupByHaving_Pass4() {
        Criteria crit = Criteria.builder().where(Filters.gt("salary", 50000)).groupBy("department").having(Filters.gt("COUNT(*)", 5)).build();
        SP sp = Dsl.PSC.select("department", "COUNT(*)").from("employees").append(crit).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList(50000, 5), sp.parameters(), "Expected [50000, 5] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 6 — Nested subqueries (subquery inside subquery).
     * Verifies parameters flatten in left-to-right SQL order.
     */
    @Test
    public void testParamOrder_NestedSubQueries_Pass4() {
        SubQuery innerInnerSQ = Filters.subQuery("regions", Arrays.asList("id"), Filters.eq("continent", "EU"));
        SubQuery middleSQ = Filters.subQuery("countries", Arrays.asList("id"), Filters.in("region_id", innerInnerSQ).and(Filters.eq("active", true)));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.in("country_id", middleSQ).and(Filters.gt("age", 30))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("EU", true, 30), sp.parameters(), "Expected [EU, true, 30] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 7 — InSubQuery + AND + Binary.
     */
    @Test
    public void testParamOrder_InSubQueryThenAndStatus_Pass4() {
        SubQuery adminSQ = Filters.subQuery("admins", Arrays.asList("id"), Filters.gt("level", 3));
        SP sp = Dsl.PSC.select("id").from("test_account").where(Filters.in("user_id", adminSQ).and(Filters.eq("status", "active"))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList(3, "active"), sp.parameters(), "Expected [3, active] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 3 — Subquery in WHERE plus outer WHERE parameter, then ensure the structured
     * subquery's WHERE condition's parameters precede the outer condition's parameter.
     * (The library does not support correlated SELECT subqueries directly; we exercise the
     *  equivalent ordering risk through WHERE-with-subquery + AND-with-binary.)
     */
    @Test
    public void testParamOrder_StructuredSubQueryConditionBeforeOuter_Pass4() {
        SubQuery sq = Filters.subQuery("orders", Arrays.asList("user_id"), Filters.eq("status", "active").and(Filters.gt("amount", 50)));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.in("id", sq).and(Filters.gt("age", 18))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("active", 50, 18), sp.parameters(), "Expected [active, 50, 18] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Scenario 8 — Criteria with WHERE containing subquery + ORDER BY + LIMIT.
     */
    @Test
    public void testParamOrder_WhereSubQueryOrderByLimit_Pass4() {
        SubQuery vipSQ = Filters.subQuery("vip_users", Arrays.asList("id"), Filters.eq("tier", "GOLD"));
        Criteria crit = Criteria.builder().where(Filters.in("user_id", vipSQ).and(Filters.gt("score", 90))).orderBy("score DESC").limit(10).build();
        SP sp = Dsl.PSC.select("id").from("test_account").append(crit).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("GOLD", 90), sp.parameters(), "Expected [GOLD, 90] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Extra: OR junction with subquery on left, binary on right — verifies the OR path
     * (different from AND) keeps parameter order matching SQL left-to-right.
     */
    @Test
    public void testParamOrder_OrJunctionWithSubQueryAndBinary_Pass4() {
        SubQuery sq = Filters.subQuery("admins", Arrays.asList("id"), Filters.eq("rank", "S"));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.in("id", sq).or(Filters.eq("vip", true))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("S", true), sp.parameters(), "Expected [S, true] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Extra: Subquery used inside HAVING after WHERE with parameter. Verifies parameters
     * from the outer WHERE precede parameters originating from HAVING's nested subquery.
     */
    @Test
    public void testParamOrder_WhereThenHavingWithSubQuery_Pass4() {
        SubQuery sq = Filters.subQuery("eligible_depts", Arrays.asList("dept_id"), Filters.eq("active", true));
        Criteria crit = Criteria.builder()
                .where(Filters.eq("status", "ACTIVE"))
                .groupBy("dept_id")
                .having(Filters.in("dept_id", sq).and(Filters.gt("COUNT(*)", 3)))
                .build();
        SP sp = Dsl.PSC.select("dept_id", "COUNT(*)").from("employees").append(crit).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("ACTIVE", true, 3), sp.parameters(), "Expected [ACTIVE, true, 3] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Extra: Three-branch AND junction interleaving Binary/SubQuery/Binary — verifies that
     * Junction iteration preserves parameter ordering when subqueries sit between binary
     * branches.
     */
    @Test
    public void testParamOrder_ThreeWayAndInterleaved_Pass4() {
        SubQuery sq = Filters.subQuery("flagged", Arrays.asList("user_id"), Filters.eq("severity", "HIGH").and(Filters.gt("count", 2)));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", sq), Filters.gt("age", 21))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("ACTIVE", "HIGH", 2, 21), sp.parameters(),
                "Expected [ACTIVE, HIGH, 2, 21] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    /**
     * Extra: BETWEEN combined with subquery, exercising Between's two parameters mixed
     * with subquery parameters.
     */
    @Test
    public void testParamOrder_BetweenMixedWithSubQuery_Pass4() {
        SubQuery sq = Filters.subQuery("blacklist", Arrays.asList("user_id"), Filters.eq("region", "EU"));
        SP sp = Dsl.PSC.select("id").from("users").where(Filters.notIn("id", sq).and(Filters.between("age", 18, 65))).build();

        assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
        assertEquals(Arrays.asList("EU", 18, 65), sp.parameters(), "Expected [EU, 18, 65] but got " + sp.parameters() + " SQL=" + sp.query());
    }

    @Test
    @Tag("2025")
    public void testNamedParameters_InSubQueryAndOuterConditionsUseUniqueNames_Pass4() {
        SubQuery innerSubQuery = Filters.subQuery("orders", Arrays.asList("user_id"), Filters.eq("status", "SHIPPED"));
        SP sp = Dsl.NSC.select("id")
                .from("users")
                .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", innerSubQuery), Filters.eq("status", "PENDING")))
                .build();

        assertEquals("SELECT id FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2)) AND (status = :status_3)",
                sp.query());
        assertEquals(Arrays.asList("ACTIVE", "SHIPPED", "PENDING"), sp.parameters());
    }

    // ===== Pass-4: DML schema validation =====

    /**
     * Bug: batchInsert(List&lt;Map&gt;) with rows that have different key sets silently produced
     * wrong SQL. The column list was taken from the first Map; for subsequent rows, missing
     * columns were filled with null and extra columns were silently dropped, causing data loss
     * and stray NULL parameters. Now an IllegalArgumentException is thrown.
     */
    @Test
    public void testBatchInsert_MapsWithMismatchedKeysThrows_Pass4() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("email", "jane@example.com"); // wrong key — would silently lose lastName + email

        List<Map<String, Object>> rows = Arrays.asList(row1, row2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(rows));
        assertTrue(ex.getMessage().contains("same key set"), "Exception should mention key set mismatch but was: " + ex.getMessage());
    }

    /**
     * Bug: batchInsert with extra columns in a later row silently dropped them.
     */
    @Test
    public void testBatchInsert_MapsWithExtraKeyInLaterRowThrows_Pass4() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");
        row2.put("email", "jane@example.com"); // extra key

        List<Map<String, Object>> rows = Arrays.asList(row1, row2);

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(rows));
    }

    /**
     * Bug: batchInsert with a missing column in a later row silently inserted NULL for that column.
     */
    @Test
    public void testBatchInsert_MapsWithMissingKeyInLaterRowThrows_Pass4() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        // missing lastName

        List<Map<String, Object>> rows = Arrays.asList(row1, row2);

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(rows));
    }

    /**
     * Bug: batchInsert with all-empty Maps would produce malformed SQL: {@code INSERT INTO t () VALUES ()}.
     */
    @Test
    public void testBatchInsert_EmptyMapThrows_Pass4() {
        List<Map<String, Object>> rows = Arrays.asList(new LinkedHashMap<>(), new LinkedHashMap<>());

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(rows));
    }

    /**
     * Regression: batchInsert with identical key sets still works correctly across multiple rows.
     */
    @Test
    public void testBatchInsert_MapsWithMatchingKeysProducesCorrectSql_Pass4() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        Map<String, Object> row3 = new LinkedHashMap<>();
        row3.put("firstName", "Bob");
        row3.put("lastName", "Johnson");

        SP sp = Dsl.PSC.batchInsert(Arrays.asList(row1, row2, row3)).into("account").build();

        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith", "Bob", "Johnson"), sp.parameters());
    }

    /**
     * Regression: batchInsert with one null element followed by valid maps continues to work
     * (null elements are skipped).
     */
    @Test
    public void testBatchInsert_NullElementsSkippedThenValidated_Pass4() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(null);
        rows.add(row1);
        rows.add(null);
        rows.add(row2);

        SP sp = Dsl.PSC.batchInsert(rows).into("account").build();
        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    public static class BfxQuoted {
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

    public static class BfxAddress {
        private long id;
        private String streetName;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(final String streetName) {
            this.streetName = streetName;
        }
    }

    public static class BfxUser {
        private long id;
        private String firstName;
        private BfxAddress address;

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

        public BfxAddress getAddress() {
            return address;
        }

        public void setAddress(final BfxAddress address) {
            this.address = address;
        }
    }

    /**
     * Bug: the static full-select-parts cache was keyed only by naming policy + entity class, so two
     * dialects sharing a naming policy but using different identifier quotes poisoned each other's
     * cached SELECT list (e.g. PSC emitting backtick-quoted aliases after a backtick dialect ran first).
     */
    @Test
    public void testFullSelectPartsCacheIsDialectQuoteAware() {
        Dsl backtickDsl = Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                .identifierQuote(SqlDialect.IdentifierQuote.BACKTICK)
                .build());

        String backtickSql = backtickDsl.selectFrom(BfxQuoted.class).build().query();
        assertEquals("SELECT id AS `id`, first_name AS `firstName` FROM bfx_quoted", backtickSql);

        String pscSql = Dsl.PSC.selectFrom(BfxQuoted.class).build().query();
        assertEquals("SELECT id AS \"id\", first_name AS \"firstName\" FROM bfx_quoted", pscSql);

        // Render both again to verify neither direction poisoned the other's cache.
        assertEquals(backtickSql, backtickDsl.selectFrom(BfxQuoted.class).build().query());
        assertEquals(pscSql, Dsl.PSC.selectFrom(BfxQuoted.class).build().query());
    }

    /**
     * Bug: {@code Dsl.selectFrom(List<Selection>)} hardcoded SNAKE_CASE for the generated FROM clause,
     * so non-snake-case DSLs (PAC/PLC/...) emitted a FROM clause inconsistent with the SELECT list.
     */
    @Test
    public void testMultiSelectFromUsesDialectNamingPolicyForFromClause() {
        String pacSql = Dsl.PAC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
        assertTrue(pacSql.endsWith(" FROM BFX_USER u, BFX_ADDRESS a"), pacSql);

        String plcSql = Dsl.PLC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
        assertTrue(plcSql.endsWith(" FROM bfxUser u, bfxAddress a"), plcSql);

        // snake_case DSL behavior is unchanged.
        String pscSql = Dsl.PSC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
        assertTrue(pscSql.endsWith(" FROM bfx_user u, bfx_address a"), pscSql);
    }

    /**
     * Bug: {@code Dsl.selectFrom(Class, alias, includeSubEntityProperties, excludedPropNames)} hardcoded
     * SNAKE_CASE for the sub-entity FROM table names, producing FROM clauses that did not match the
     * sub-entity column prefixes in the SELECT list for non-snake-case DSLs.
     */
    @Test
    public void testSelectFromWithSubEntitiesUsesDialectNamingPolicyForFromClause() {
        String pacSql = Dsl.PAC.selectFrom(BfxUser.class, true).build().query();
        assertTrue(pacSql.endsWith(" FROM BFX_USER, BFX_ADDRESS"), pacSql);

        String plcSql = Dsl.PLC.selectFrom(BfxUser.class, true).build().query();
        assertTrue(plcSql.endsWith(" FROM bfxUser, bfxAddress"), plcSql);

        String pscSql = Dsl.PSC.selectFrom(BfxUser.class, true).build().query();
        assertTrue(pscSql.endsWith(" FROM bfx_user, bfx_address"), pscSql);
    }

    /**
     * Bug: a {@code Using} join condition reached the generic Cell branch of appendCondition, which
     * wrapped its already-parenthesized column list in a second pair of parentheses:
     * {@code USING ((employee_id))} — invalid SQL.
     */
    @Test
    public void testCriteriaJoinWithUsingRendersSingleParentheses() {
        Criteria criteria = Criteria.builder().innerJoin("employees", new Using("employee_id")).build();
        String sql = Dsl.PSC.select("*").from("orders").append(criteria).build().query();
        assertEquals("SELECT * FROM orders INNER JOIN employees USING (employee_id)", sql);

        Criteria multiColumn = Criteria.builder().leftJoin("assignments", new Using("company_id", "department_id")).build();
        String sql2 = Dsl.PSC.select("*").from("projects").append(multiColumn).build().query();
        assertEquals("SELECT * FROM projects LEFT JOIN assignments USING (company_id, department_id)", sql2);
    }

    /**
     * Bug: a select modifier set on the first segment leaked into every following set-operation
     * segment (DISTINCT silently applied to the UNION ALL branch, changing query semantics).
     */
    @Test
    public void testSelectModifierDoesNotLeakIntoSetOperationSegments() {
        String sql = Dsl.PSC.select("id").distinct().from("a_tbl").unionAll(Collections.singletonList("id")).from("b_tbl").build().query();
        assertEquals("SELECT DISTINCT id FROM a_tbl UNION ALL SELECT id FROM b_tbl", sql);
    }

    /**
     * Bug: a select modifier set after a set operation was inserted into the FIRST segment's SELECT
     * (raw indexOf-based insertion) and then also emitted for the new segment.
     */
    @Test
    public void testSelectModifierAfterSetOperationAppliesToNewSegmentOnly() {
        String sql = Dsl.PSC.select("id").from("users").union(Collections.singletonList("id")).distinct().from("customers").build().query();
        assertEquals("SELECT id FROM users UNION SELECT DISTINCT id FROM customers", sql);
    }

    /**
     * Regression: a modifier set after from() is still inserted right after the current SELECT keyword.
     */
    @Test
    public void testSelectModifierAfterFromStillApplies() {
        String sql = Dsl.PSC.select("name").from("account").distinct().build().query();
        assertEquals("SELECT DISTINCT name FROM account", sql);
    }

    /**
     * Bug: in the INSERT ... SELECT flow, the modifier insertion searched for the "SELECT" substring
     * and matched inside a rendered identifier (e.g. "SELECTED_FLAG"), splicing the modifier into the
     * column list of the INSERT clause.
     */
    @Test
    public void testSelectModifierWithInsertSelectAndSelectLikeColumnName() {
        String sql = Dsl.PAC.select("selectedFlag").into("t2").distinct().from("t1").build().query();
        assertEquals("INSERT INTO t2 (SELECTED_FLAG) SELECT DISTINCT SELECTED_FLAG AS \"selectedFlag\" FROM t1", sql);
    }

    /**
     * Bug: the generated "name_N" suffix for repeated named parameters could collide with a property
     * literally named "name_N", producing two different values bound to one placeholder.
     */
    @Test
    public void testNamedParameterSuffixDoesNotCollideWithLiteralPropertyName() {
        SP sp = Dsl.NSC.select("*").from("users").where(Filters.and(Filters.eq("id", 1), Filters.eq("id", 2), Filters.eq("id_2", 3))).build();

        assertEquals("SELECT * FROM users WHERE (id = :id) AND (id = :id_2) AND (id_2 = :id_2_2)", sp.query());
        assertEquals(Arrays.asList(1, 2, 3), sp.parameters());
    }

    /**
     * Same collision with the literal "name_N" property occurring first: the second occurrence of the
     * base name must skip the taken "name_2" suffix.
     */
    @Test
    public void testNamedParameterSuffixSkipsNameTakenByLiteralProperty() {
        SP sp = Dsl.NSC.select("*").from("users").where(Filters.and(Filters.eq("id_2", 3), Filters.eq("id", 1), Filters.eq("id", 2))).build();

        assertEquals("SELECT * FROM users WHERE (id_2 = :id_2) AND (id = :id) AND (id = :id_3)", sp.query());
        assertEquals(Arrays.asList(3, 1, 2), sp.parameters());
    }

    /**
     * Bug: the union-rename logic picked "name_(parentCount+i)" blindly, colliding with a property
     * literally named "name_2" on the child side.
     */
    @Test
    public void testUnionNamedParameterRenameAvoidsChildLiteralName() {
        SP sp = Dsl.NSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .union(Dsl.NSC.select("id").from("archive").where(Filters.and(Filters.eq("id", 2), Filters.eq("id_2", 3))))
                .build();

        assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM archive WHERE (id = :id_3) AND (id_2 = :id_2)", sp.query());
        assertEquals(Arrays.asList(1, 2, 3), sp.parameters());
    }

    /**
     * Same union-rename collision on the parent side: the renamed child parameter must not collide
     * with the parent's literally-named "id_2" placeholder.
     */
    @Test
    public void testUnionNamedParameterRenameAvoidsParentLiteralName() {
        SP sp = Dsl.NSC.select("id")
                .from("users")
                .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_2", 9)))
                .union(Dsl.NSC.select("id").from("archive").where(Filters.eq("id", 2)))
                .build();

        assertEquals("SELECT id FROM users WHERE (id = :id) AND (id_2 = :id_2) UNION SELECT id FROM archive WHERE id = :id_3", sp.query());
        assertEquals(Arrays.asList(1, 9, 2), sp.parameters());
    }

    /**
     * Regression: the plain duplicated-name union rename (no literal-name collisions) is unchanged.
     */
    @Test
    public void testUnionNamedParameterRenamePlainCaseUnchanged() {
        SP sp = Dsl.NSC.select("id").from("users").where(Filters.eq("id", 1)).union(Dsl.NSC.select("id").from("archive").where(Filters.eq("id", 2))).build();

        assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM archive WHERE id = :id_2", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    @Test
    public void testUnionNamedParameterRenameSkipsQuotedLiterals() {
        SP sp = Dsl.NSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .union(Dsl.NSC.select("id").from("archive").where(Filters.eq("id", 2)).append(" AND note = ':id' AND quoted_name = \":id\""))
                .build();

        assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
        assertTrue(sp.query().contains("note = ':id'"), sp.query());
        assertTrue(sp.query().contains("quoted_name = \":id\""), sp.query());
        assertFalse(sp.query().contains("note = ':id_2'"), sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    @Test
    public void testUnionNamedParameterRenameSkipsDoubledQuotesAndBackticks() {
        SP sp = Dsl.NSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .union(Dsl.NSC.select("id").from("archive").where(Filters.eq("id", 2)).append(" AND a = \"x\"\":id\" AND b = `y``:id` AND c = ':id'"))
                .build();

        // The genuine parameter of the second query is renamed...
        assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
        // ...while colon-tokens embedded inside double-quoted, backtick-quoted, and single-quoted
        // literals (with doubled "" / `` escapes) are left intact.
        assertTrue(sp.query().contains("a = \"x\"\":id\""), sp.query());
        assertTrue(sp.query().contains("b = `y``:id`"), sp.query());
        assertTrue(sp.query().contains("c = ':id'"), sp.query());
        assertFalse(sp.query().contains(":id_2\""), sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    /**
     * Bug: during set-operation named-parameter uniquify, the token-replacement scanner treated a
     * SQL Server temporary-table identifier ({@code #tmp}) in the child operand as a MySQL hash
     * comment and skipped the rest of the line, so the child's colliding {@code :id} was silently
     * left un-renamed (both legs bound the same placeholder). On a SQL Server dialect,
     * {@code #name}/{@code ##name} are data tokens, never comments.
     */
    @Test
    public void testUnionNamedParameterRenameHandlesSqlServerTempTableInChild() {
        final Dsl mssqlNamed = Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.NO_CHANGE)
                .sqlPolicy(SqlDialect.SqlPolicy.NAMED_SQL)
                .productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server"))
                .build());

        SP sp = mssqlNamed.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(mssqlNamed.select("id").from("#tmp").where(Filters.eq("id", 2)))
                .build();

        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM #tmp WHERE id = :id_2", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());

        // Global temporary table (##name): already safe before the fix via the ##-specific carve-out,
        // pinned here so both temp-identifier forms stay covered.
        sp = mssqlNamed.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(mssqlNamed.select("id").from("##tmp").where(Filters.eq("id", 2)))
                .build();

        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM ##tmp WHERE id = :id_2", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    /**
     * Guard for the fix above: on a MySQL dialect a genuine hash comment in the child operand still
     * stops the uniquify scanner, so a {@code :id} inside the comment text is left untouched while
     * the genuine parameter before it is renamed.
     */
    @Test
    public void testUnionNamedParameterRenameStillSkipsMySqlHashComment() {
        final Dsl mysqlNamed = Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.NO_CHANGE)
                .sqlPolicy(SqlDialect.SqlPolicy.NAMED_SQL)
                .productInfo(SqlDialect.ProductInfo.of("MySQL"))
                .build());

        final SP sp = mysqlNamed.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .union(mysqlNamed.select("id").from("archive").where(Filters.eq("id", 2)).append(" # note :id"))
                .build();

        assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
        assertTrue(sp.query().endsWith("# note :id"), sp.query());
        assertFalse(sp.query().contains("# note :id_2"), sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
    }

    /**
     * Bug: {@code union("SELECT 1")} (a valid FROM-less query) was treated as a column list and
     * silently dropped, emitting "... UNION " with nothing after the keyword.
     */
    @Test
    public void testUnionWithFromLessQueryIsAppendedInline() {
        String sql = Dsl.PSC.select("id").from("users").union("SELECT 1").build().query();
        assertEquals("SELECT id FROM users UNION SELECT 1", sql);
    }

    /**
     * Regression: a full inline sub-query and a plain column list after union are unchanged.
     */
    @Test
    public void testUnionWithFullQueryAndColumnListUnchanged() {
        String sql = Dsl.PSC.select("id").from("users").union("SELECT id FROM customers").build().query();
        assertEquals("SELECT id FROM users UNION SELECT id FROM customers", sql);

        String sql2 = Dsl.PSC.select("id").from("users").union(Collections.singletonList("id")).from("customers").build().query();
        assertEquals("SELECT id FROM users UNION SELECT id FROM customers", sql2);
    }

    /**
     * Bug: chained set() calls decided the leading comma by sniffing the buffer's last character,
     * so a raw fragment with trailing whitespace swallowed the comma and produced invalid SQL.
     */
    @Test
    public void testChainedSetWithTrailingWhitespaceFragmentKeepsComma() {
        String sql = Dsl.PSC.update("users").set("a = 1 ").set("b").where(Filters.eq("id", 1)).build().query();
        // The raw fragment's trailing space is preserved verbatim; the comma is what matters here.
        assertEquals("UPDATE users SET a = 1 , b = ? WHERE id = ?", sql);
    }

    /**
     * Regression: normal chained set() behavior is unchanged.
     */
    @Test
    public void testChainedSetWithoutTrailingWhitespaceUnchanged() {
        String sql = Dsl.PSC.update("users").set("a = 1").set("b").where(Filters.eq("id", 1)).build().query();
        assertEquals("UPDATE users SET a = 1, b = ? WHERE id = ?", sql);
    }

    @Test
    public void testBuilderInputsAreDefensivelyCopiedBeforeBuild() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("firstName", "John");
        SqlBuilder insert = Dsl.PSC.insert(props);
        props.clear();

        SP insertSp = insert.into("account").build();
        assertEquals("INSERT INTO account (first_name) VALUES (?)", insertSp.query());
        assertEquals(Arrays.asList("John"), insertSp.parameters());

        Map<String, Object> entityProps = new LinkedHashMap<>();
        entityProps.put("lastName", "Doe");
        Object entity = entityProps;
        SqlBuilder insertObject = Dsl.PSC.insert(entity);
        entityProps.clear();

        SP insertObjectSp = insertObject.into("account").build();
        assertEquals("INSERT INTO account (last_name) VALUES (?)", insertObjectSp.query());
        assertEquals(Arrays.asList("Doe"), insertObjectSp.parameters());

        List<String> columns = new ArrayList<>(Arrays.asList("firstName"));
        SqlBuilder selectColumns = Dsl.PSC.select(columns);
        columns.add("lastName");
        columns.clear();
        assertEquals("SELECT first_name AS \"firstName\" FROM account", selectColumns.from("account").build().query());

        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("firstName", "fname");
        SqlBuilder selectAliases = Dsl.PSC.select(aliases);
        aliases.put("lastName", "lname");
        aliases.clear();
        assertEquals("SELECT first_name AS \"fname\" FROM account", selectAliases.from("account").build().query());
    }

    @Test
    public void testBatchInsertMapRowsAreDefensivelyCopiedBeforeBuild() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        SqlBuilder builder = Dsl.PSC.batchInsert(rows);
        row1.clear();
        row2.put("email", "jane@example.com");
        rows.clear();

        SP sp = builder.into("account").build();
        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testSelectMapRejectsUnsafeAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();

        aliases.put("firstName", "bad\"alias");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(aliases));

        aliases.clear();
        aliases.put("firstName", "bad`alias");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(aliases));

        aliases.clear();
        aliases.put("firstName", "bad--alias");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(aliases));

        aliases.clear();
        aliases.put("firstName", "bad#alias");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(aliases));

        aliases.clear();
        aliases.put("firstName", "bad'alias");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(aliases));
    }

    @Test
    public void testSelectExpressionRejectsUnsafeInlineAlias() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("firstName AS bad--alias").from("account").build());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("firstName AS bad/*alias").from("account").build());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("firstName AS bad*/alias").from("account").build());
        // '#' starts a comment in MySQL and a quote opens a string literal; both would truncate or
        // corrupt the statement if emitted unquoted after AS.
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("firstName AS x#y").from("account").build());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("firstName AS x'y").from("account").build());
    }

    @Test
    public void testSelectExpressionFindsOnlyTopLevelAsAlias() {
        assertEquals("SELECT CAST(created_at AS date) AS createdDay FROM events",
                Dsl.NSC.select("CAST(created_at AS DATE) AS createdDay").from("events").build().query());
        assertEquals("SELECT CONCAT(' AS ', name) AS label FROM events", Dsl.NSC.select("CONCAT(' AS ', name) AS label").from("events").build().query());
        assertEquals("SELECT \"created AS date\" AS label FROM events", Dsl.NSC.select("\"created AS date\" AS label").from("events").build().query());
    }

    @Test
    public void testFromAliasScannerSkipsQuotesCommentsAndNestedParentheses() {
        final String subquerySql = Dsl.PSC.select(Account.class).from("(SELECT ')' AS marker FROM account /* ) */) e").build().query();
        final String doubleQuotedTableSql = Dsl.PSC.select(Account.class).from("\"Order Details\" od").build().query();
        final String bracketQuotedTableSql = Dsl.PSC.select(Account.class).from("[Order Details] AS od").build().query();
        final String commentSeparatedAliasSql = Dsl.PSC.select(Account.class).from("account -- AS ignored\n e").build().query();
        final String trailingCommentSql = Dsl.PSC.select(Account.class).from("account /* no alias */").build().query();

        assertTrue(subquerySql.startsWith("SELECT e."), subquerySql);
        assertTrue(subquerySql.endsWith(" FROM (SELECT ')' AS marker FROM account /* ) */) e"), subquerySql);
        assertTrue(doubleQuotedTableSql.startsWith("SELECT od."), doubleQuotedTableSql);
        assertTrue(doubleQuotedTableSql.endsWith(" FROM \"Order Details\" od"), doubleQuotedTableSql);
        assertTrue(bracketQuotedTableSql.startsWith("SELECT od."), bracketQuotedTableSql);
        assertTrue(bracketQuotedTableSql.endsWith(" FROM [Order Details] AS od"), bracketQuotedTableSql);
        assertTrue(commentSeparatedAliasSql.startsWith("SELECT e."), commentSeparatedAliasSql);
        assertTrue(commentSeparatedAliasSql.endsWith(" FROM account -- AS ignored\n e"), commentSeparatedAliasSql);
        assertFalse(trailingCommentSql.startsWith("SELECT /*"), trailingCommentSql);
        assertTrue(trailingCommentSql.endsWith(" FROM account /* no alias */"), trailingCommentSql);
    }

    @Test
    public void testRawFromBodyUsesOnlyPrimaryTableAliasForEntityColumns() {
        final String inlineJoinSql = Dsl.PSC.select(Account.class).from("test_account a JOIN user_order o ON a.id = o.account_id").build().query();
        final String quotedCommaSql = Dsl.PSC.select(Account.class).from("\"accounts,archive\" a").build().query();
        final String qualifiedKeywordTableSql = Dsl.PSC.select(Account.class).from("schema.left l").build().query();
        final String straightJoinSql = Dsl.PSC.select(Account.class).from("test_account a STRAIGHT_JOIN user_order o ON a.id = o.account_id").build().query();
        final String outerApplySql = Dsl.PSC.select(Account.class).from("test_account a OUTER APPLY order_summary(a.id) o").build().query();

        assertTrue(inlineJoinSql.startsWith("SELECT a.id"), inlineJoinSql);
        assertFalse(inlineJoinSql.contains("o.account_id.id"), inlineJoinSql);
        assertTrue(inlineJoinSql.endsWith(" FROM test_account a JOIN user_order o ON a.id = o.account_id"), inlineJoinSql);
        assertTrue(quotedCommaSql.startsWith("SELECT a.id"), quotedCommaSql);
        assertTrue(quotedCommaSql.endsWith(" FROM \"accounts,archive\" a"), quotedCommaSql);
        assertTrue(qualifiedKeywordTableSql.startsWith("SELECT l.id"), qualifiedKeywordTableSql);
        assertTrue(qualifiedKeywordTableSql.endsWith(" FROM schema.left l"), qualifiedKeywordTableSql);
        assertTrue(straightJoinSql.startsWith("SELECT a.id"), straightJoinSql);
        assertTrue(straightJoinSql.endsWith(" FROM test_account a STRAIGHT_JOIN user_order o ON a.id = o.account_id"), straightJoinSql);
        assertTrue(outerApplySql.startsWith("SELECT a.id"), outerApplySql);
        assertTrue(outerApplySql.endsWith(" FROM test_account a OUTER APPLY order_summary(a.id) o"), outerApplySql);
    }

    @Test
    public void testParenthesizedUnaryConditionDoesNotAddDoubleSpace() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("*").from("users").where(Filters.not(Filters.equal("status", "ACTIVE"))).build();

        assertEquals("SELECT * FROM users WHERE NOT (status = ?)", sp.query());
        assertEquals(Arrays.asList("ACTIVE"), sp.parameters());
    }

    @Test
    public void testUnsupportedConditionMessageUsesConditionClass() {
        final Condition unsupported = unsupportedCondition();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where(unsupported).build());

        assertTrue(ex.getMessage().contains(unsupported.getClass().getName()));
    }

    /**
     * The dialect accessor follows the fluent lower-camel naming convention (was {@code SqlDialect()}).
     */
    @Test
    public void testSqlDialectAccessor() {
        SqlBuilder builder = Dsl.PSC.select("id").from("users");

        assertNotNull(builder.sqlDialect());
        assertEquals(NamingPolicy.SNAKE_CASE, builder.sqlDialect().namingPolicy());
        assertEquals(SqlDialect.SqlPolicy.PARAMETERIZED_SQL, builder.sqlDialect().sqlPolicy());

        builder.build();
    }

    /**
     * Bug: the {@code dslCache} field was declared <i>after</i> the predefined {@code Dsl} constants
     * whose initializers call {@link Dsl#forDialect(SqlDialect)}. Static field initializers run in
     * textual order, so {@code dslCache} was still {@code null} when the first constant ({@code PSB})
     * ran {@code forDialect(...)}, throwing {@code NullPointerException} -> {@code ExceptionInInitializerError}
     * during class init and leaving every later access failing with
     * {@code NoClassDefFoundError: Could not initialize class ...Dsl}. Fix: declare {@code dslCache}
     * before the constants. This test pins both the initialization and the caching contract.
     */
    @Test
    public void testPredefinedDslConstantsInitializeAndForDialectCaches() {
        // Touch every predefined constant: each must initialize (non-null) and be usable.
        for (final Dsl dsl : new Dsl[] { Dsl.PSB, Dsl.PSC, Dsl.PAC, Dsl.PLC, Dsl.NSB, Dsl.NSC, Dsl.NAC, Dsl.NLC, Dsl.SCSB, Dsl.ACSB, Dsl.LCSB, Dsl.MSB, Dsl.MSC,
                Dsl.MAC, Dsl.MLC }) {
            assertNotNull(dsl);
            assertNotNull(dsl.sqlDialect());
        }

        // forDialect returns the canonical cached instance for a dialect equal to a predefined one...
        final SqlDialect pscDialect = SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build();
        Assertions.assertSame(Dsl.PSC, Dsl.forDialect(pscDialect));

        // ...and a fresh, non-null instance for a dialect that is not predefined.
        final SqlDialect customDialect = SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                .identifierQuote(SqlDialect.IdentifierQuote.BACKTICK)
                .build();
        final Dsl custom = Dsl.forDialect(customDialect);
        assertNotNull(custom);
        assertNotSame(Dsl.PSC, custom);
    }

    @Test
    public void testDslVarargsAreSnapshottedBeforeDeferredRendering() {
        String[] selectColumns = { "firstName" };
        SqlBuilder selectBuilder = Dsl.PSC.select(selectColumns);
        selectColumns[0] = "lastName";

        String selectSql = selectBuilder.from("account").build().query();
        assertTrue(selectSql.contains("first_name"));
        assertFalse(selectSql.contains("last_name"));

        String[] insertColumns = { "firstName" };
        SqlBuilder insertBuilder = Dsl.PSC.insert(insertColumns);
        insertColumns[0] = "lastName";

        assertEquals("INSERT INTO account (first_name) VALUES (?)", insertBuilder.into("account").build().query());
    }

    @Test
    public void testSetOperationColumnsAreSnapshottedBeforeDeferredRendering() {
        List<String> unionColumns = new ArrayList<>(Arrays.asList("legacyId"));
        SqlBuilder builder = Dsl.PSC.select("id").from("current_records").union(unionColumns);
        unionColumns.set(0, "mutatedId");

        String sql = builder.from("legacy_records").build().query();
        assertTrue(sql.contains("legacy_id"));
        assertFalse(sql.contains("mutated_id"));
    }

    @Test
    public void testIncompleteInsertAndPreSetUpdateFailFast() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.insert("id").build());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.insert("id").append("RETURNING id").build());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("users").append("RETURNING id").build());
    }

    @Test
    public void testMultiSelectionsValidateInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(selection(Account.class, "a", "bad\"alias", Arrays.asList("id"), false, null)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(selection(Account.class, "a", "account", Arrays.asList("   "), false, null)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(Account.class, "a", "account", (Class<?>) null, "b", "other"));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.selectFrom(Account.class, "a", "account", (Class<?>) null, "b", "other"));
    }

    @Test
    public void testSetOperationNamedParameterTokenSafety() {
        SP combined = Dsl.NSC.select("id")
                .from("current_records")
                .where(Filters.equal("type", "current"))
                .union(Dsl.NSC.select("payload::type").from("archived_records").where(Filters.equal("type", "archived")))
                .build();

        assertTrue(combined.query().contains("payload::type"), combined.query());
        assertFalse(combined.query().contains("payload::type_2"), combined.query());
        assertTrue(combined.query().contains(":type_2"), combined.query());
        assertEquals(Arrays.asList("current", "archived"), combined.parameters());
    }

    @Test
    public void testSetOperationRejectsMutationContainingNestedSelect() {
        SqlBuilder builder = Dsl.PSC.select("id").from("current_records");

        assertThrows(IllegalArgumentException.class, () -> builder.union("UPDATE target SET value = (SELECT value FROM source) FROM target"));
        assertEquals("SELECT id FROM current_records", builder.build().query());
    }

    @Test
    public void testSetOperationPropertyVarargsOverloadsAreRemoved() {
        for (final String methodName : Arrays.asList("union", "unionAll", "intersect", "except", "minus")) {
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod(methodName, String[].class));
        }
    }

    @Test
    public void testSetOperationsRequireACompleteSelectLeftOperand() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").union("SELECT id FROM archived_records"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").intersect(Arrays.asList("id")));
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.update("current_records").set(Collections.singletonMap("active", false)).except("SELECT id FROM archived_records"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.renderCondition(Filters.equal("active", true)).minus("SELECT id FROM archived_records"));
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("id")
                        .from("current_records")
                        .union("SELECT id FROM archived_records")
                        .distinct()
                        .except("SELECT id FROM deleted_records"));
    }

    @Test
    public void testRejectedSiblingSetOperationDoesNotConsumeRightBuilder() {
        SqlBuilder right = Dsl.PSC.select("id").from("archived_records");

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").unionAll(right));
        assertEquals("SELECT id FROM archived_records", right.build().query());
    }

    @Test
    public void testSiblingSetOperationRejectsMixedSqlPoliciesWhenNamedParametersAreGenerated() {
        SqlBuilder left = Dsl.NSC.select("id").from("current_records");
        SqlBuilder right = Dsl.PSC.select("id").from("archived_records").where(Filters.equal("active", true));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> left.union(right));
        assertTrue(ex.getMessage().contains("parent's SQL policy"), ex.getMessage());
        assertEquals("SELECT id FROM current_records", left.build().query());
        assertEquals("SELECT id FROM archived_records WHERE active = ?", right.build().query());

        assertEquals("SELECT id FROM current_records WHERE active = :active UNION SELECT id FROM archived_records",
                Dsl.NSC.select("id")
                        .from("current_records")
                        .where(Filters.equal("active", true))
                        .union(Dsl.PSC.select("id").from("archived_records"))
                        .build()
                        .query());

        SqlBuilder questionMarkLeft = Dsl.NSC.select("id").from("current_records");
        SqlBuilder questionMark = Dsl.PSC.select("id").from("archived_records").where(Filters.equal("active", Filters.QME));
        assertThrows(IllegalArgumentException.class, () -> questionMarkLeft.union(questionMark));
        assertEquals("SELECT id FROM current_records", questionMarkLeft.build().query());
        assertEquals("SELECT id FROM archived_records WHERE active = ?", questionMark.build().query());
    }

    @Test
    public void testSiblingSetOperationDetectsParametersGeneratedInsideStructuredSubQuery() {
        final SqlBuilder left = Dsl.NSC.select("id").from("current_records");
        final SubQuery activeOwners = Filters.subQuery("owners", Arrays.asList("id"), Filters.equal("active", true));
        final SqlBuilder right = Dsl.PSC.select("id").from("archived_records").where(Filters.in("owner_id", activeOwners));

        assertThrows(IllegalArgumentException.class, () -> left.union(right));
        assertEquals("SELECT id FROM current_records", left.build().query());

        final SP rightSql = right.build();
        assertEquals("SELECT id FROM archived_records WHERE owner_id IN (SELECT id FROM owners WHERE active = ?)", rightSql.query());
        assertEquals(Arrays.asList(true), rightSql.parameters());
    }

    @Test
    public void testCompleteLiteralSetOperandsCanBeChained() {
        String sql = Dsl.PSC.select("id")
                .from("current_records")
                .union("SELECT id FROM archived_records")
                .except("SELECT id FROM deleted_records")
                .build()
                .query();

        assertEquals("SELECT id FROM current_records UNION SELECT id FROM archived_records EXCEPT SELECT id FROM deleted_records", sql);
    }

    @Test
    public void testSetOperationsRejectTerminalClausesOnLeftOperand() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("current_records").orderBy("id").union("SELECT id FROM archived_records"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("current_records").limit(10).intersect("SELECT id FROM archived_records"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("current_records").forUpdate().minus("SELECT id FROM archived_records"));
    }

    @Test
    public void testBatchInsertRejectsRowsWithNoRemainingColumns() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.batchInsert(Arrays.asList(new AllNullBatchEntity(), new AllNullBatchEntity())));
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
    }

    @Test
    public void testSingleBeanInsertRejectsNoRemainingValuesBeforeAllocatingBuilder() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert(new AllNullBatchEntity()));

        assertTrue(ex.getMessage().contains("No insertable values remain"), ex.getMessage());
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
    }

    @Test
    public void testMetadataFactoriesRejectEmptyColumnSetsBeforeAllocatingBuilder() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
        final Set<String> allProperties = Collections.singleton("value");
        final Selection emptySelection = Selection.builder(AllNullBatchEntity.class).excludedPropNames(allProperties).build();

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert(AllNullBatchEntity.class, allProperties));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(AllNullBatchEntity.class, allProperties));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update(AllNullBatchEntity.class, allProperties));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(emptySelection));
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "Factories that cannot produce a column must reject before allocating pooled buffers");
    }

    @Test
    public void testSelectFromRenderingFailureReleasesAllocatedBuilder() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
        final Selection unsafeSelection = Selection.builder(AllNullBatchEntity.class).includedPropNames(Collections.singletonList("value -- unsafe")).build();

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.selectFrom(unsafeSelection));
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "A SELECT FROM factory that fails during projection rendering must release its pooled buffer");
    }

    @Test
    public void testSetEntityRejectsAnEmptyUpdatableProjectionWithoutChangingMapping() {
        final SqlBuilder builder = Dsl.PSC.update("users");

        assertThrows(IllegalArgumentException.class, () -> builder.setEntity(AllNullBatchEntity.class, Collections.singleton("value")));
        assertNull(builder._entityClass);
        assertEquals("UPDATE users SET name = ?", builder.set("name").build().query());

        final SqlBuilder objectBuilder = Dsl.PSC.update("users");
        assertThrows(IllegalArgumentException.class, () -> objectBuilder.setEntity(new AllNullBatchEntity(), Collections.singleton("value")));
        assertNull(objectBuilder._entityClass);
        assertEquals("UPDATE users SET name = ?", objectBuilder.set("name").build().query());
    }

    @Test
    public void testFailedStructuredSubQueryRenderingReleasesNestedAndOuterBuilders() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
        final SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), unsupportedCondition());

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.renderCondition(Filters.in("id", subQuery)));
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "A failed condition factory must release both its outer and structured-subquery buffers");
    }

    @Test
    public void testCountRejectsBlankTableBeforeAllocatingBuilder() {
        final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.count("   "));
        assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
    }

    @Test
    public void testBatchInsertUsesOneOuterCollectionSnapshot() {
        final BatchEntityWithId beanRow = new BatchEntityWithId();
        beanRow.setName("correct");

        final Map<String, Object> laterMapRow = new LinkedHashMap<>();
        laterMapRow.put("otherId", 999L);
        laterMapRow.put("name", "wrong");

        final Collection<Object> changingRows = new java.util.AbstractCollection<>() {
            private int iteratorCalls;

            @Override
            public java.util.Iterator<Object> iterator() {
                return Collections.<Object> singleton(iteratorCalls++ == 0 ? beanRow : laterMapRow).iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        final SP result = Dsl.PSC.batchInsert(changingRows).into("batch_entity").build();

        assertEquals("INSERT INTO batch_entity (other_id, name) VALUES (?, ?)", result.query());
        assertEquals(Arrays.asList(0L, "correct"), result.parameters());
    }

    private static Selection selection(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> includedPropNames,
            final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        return Selection.builder(entityClass)
                .tableAlias(tableAlias)
                .classAlias(classAlias)
                .includedPropNames(includedPropNames)
                .includeSubEntityProperties(includeSubEntityProperties)
                .excludedPropNames(excludedPropNames)
                .build();
    }

    private static Condition unsupportedCondition() {
        return new Condition() {
            @Override
            public Operator operator() {
                return Operator.EQUAL;
            }

            @Override
            public ImmutableList<Object> parameters() {
                return ImmutableList.empty();
            }

            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                throw new AssertionError("toSql(NamingPolicy) should not be used for unsupported condition messages");
            }

            @Override
            public String toString() {
                throw new AssertionError("toString() should not be used for unsupported condition messages");
            }
        };
    }

    static final class AllNullBatchEntity {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    static final class BatchEntityWithId {
        @Id
        private long id;
        private long otherId;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public long getOtherId() {
            return otherId;
        }

        public void setOtherId(final long otherId) {
            this.otherId = otherId;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    static final class BatchEntityWithOtherId {
        private long id;
        @Id
        private long otherId;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public long getOtherId() {
            return otherId;
        }

        public void setOtherId(final long otherId) {
            this.otherId = otherId;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
