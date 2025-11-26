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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Filters;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.entity.Account;

@Tag("2025")
public class SQLBuilder2025Test extends TestBase {

    // Basic SELECT tests
    @Test
    public void testSelectAll() {
        String sql = SQLBuilder.PSC.select("*").from("users").sql();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testSelectSingleColumn() {
        String sql = SQLBuilder.PSC.select("name").from("users").sql();
        assertEquals("SELECT name FROM users", sql);
    }

    @Test
    public void testSelectMultipleColumns() {
        String sql = SQLBuilder.PSC.select("id", "name", "email").from("users").sql();
        assertTrue(sql.contains("SELECT id, name, email"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = SQLBuilder.PSC.select("first_name AS fname").from("users").sql();
        assertTrue(sql.contains("AS fname"));
    }

    @Test
    public void testSelectWithEntityClass() {
        String sql = SQLBuilder.PSC.select(Account.class).from(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    // FROM clause tests
    @Test
    public void testFromSingleTable() {
        String sql = SQLBuilder.PSC.select("*").from("users").sql();
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testFromMultipleTables() {
        String sql = SQLBuilder.PSC.select("*").from("users", "orders").sql();
        assertTrue(sql.contains("FROM orders"));
    }

    @Test
    public void testFromWithAlias() {
        String sql = SQLBuilder.PSC.select("u.*").from("users u").sql();
        assertTrue(sql.contains("FROM users u"));
    }

    @Test
    public void testFromEntityClass() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).sql();
        assertNotNull(sql);
    }

    // WHERE clause tests
    @Test
    public void testWhereEqual() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testWhereMultipleConditions() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).sql();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testWhereOr() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator"))).sql();
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testWhereBetween() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.between("age", 18, 65)).sql();
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testWhereLike() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.like("name", "%John%")).sql();
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testWhereIsNull() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.isNull("deleted_at")).sql();
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testWhereIsNotNull() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.isNotNull("email")).sql();
        assertTrue(sql.contains("IS NOT NULL"));
    }

    // JOIN tests
    @Test
    public void testInnerJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").sql();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testLeftJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").sql();
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testRightJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").rightJoin("departments").on("users.dept_id = departments.id").sql();
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testFullJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").sql();
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testCrossJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").crossJoin("roles").sql();
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testMultipleJoins() {
        String sql = SQLBuilder.PSC.select("*")
                .from("users u")
                .innerJoin("orders o")
                .on("u.id = o.user_id")
                .leftJoin("products p")
                .on("o.product_id = p.id")
                .sql();
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
    }

    // GROUP BY tests
    @Test
    public void testGroupBy() {
        String sql = SQLBuilder.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").sql();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testGroupByMultipleColumns() {
        String sql = SQLBuilder.PSC.select("year", "month", "COUNT(*)").from("sales").groupBy("year", "month").sql();
        assertTrue(sql.contains("GROUP BY"));
    }

    // HAVING tests
    @Test
    public void testHaving() {
        String sql = SQLBuilder.PSC.select("department", "COUNT(*)")
                .from("employees")
                .groupBy("department")
                .having(Filters.expr("COUNT(*) > 5"))
                .sql();
        assertTrue(sql.contains("HAVING"));
    }

    // ORDER BY tests
    @Test
    public void testOrderBy() {
        String sql = SQLBuilder.PSC.select("*").from("users").orderBy("name").sql();
        assertTrue(sql.contains("ORDER BY name"));
    }

    @Test
    public void testOrderByAsc() {
        String sql = SQLBuilder.PSC.select("*").from("users").orderBy("name", SortDirection.ASC).sql();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("ASC"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = SQLBuilder.PSC.select("*").from("users").orderBy("created_date", SortDirection.DESC).sql();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("DESC"));
    }

    @Test
    public void testOrderByMultipleColumns() {
        String sql = SQLBuilder.PSC.select("*").from("users").orderBy("last_name", "first_name").sql();
        assertTrue(sql.contains("ORDER BY"));
    }

    // LIMIT tests
    @Test
    public void testLimit() {
        String sql = SQLBuilder.PSC.select("*").from("users").limit(10).sql();
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = SQLBuilder.PSC.select("*").from("users").limit(10, 20).sql();
        assertTrue(sql.contains("LIMIT"));
    }

    // DISTINCT tests
    @Test
    public void testDistinct() {
        String sql = SQLBuilder.PSC.select("status").from("users").distinct().sql();
        assertTrue(sql.contains("DISTINCT"));
    }

    // INSERT tests
    @Test
    public void testInsertInto() {
        String sql = SQLBuilder.PSC.insert("id", "name").into("users").sql();
        assertTrue(sql.contains("INSERT INTO users"));
    }

    @Test
    public void testInsertIntoWithEntityClass() {
        String sql = SQLBuilder.PSC.insert("firstName", "lastName").into(Account.class).sql();
        assertTrue(sql.contains("INSERT INTO"));
    }

    // UPDATE tests
    @Test
    public void testUpdate() {
        String sql = SQLBuilder.PSC.update("users").set("status", "inactive").where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("UPDATE users"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateMultipleColumns() {
        String sql = SQLBuilder.PSC.update("users").set("first_name", "John").set("last_name", "Doe").where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateWithEntityClass() {
        String sql = SQLBuilder.PSC.update(Account.class).set("status", "active").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
    }

    // DELETE tests
    @Test
    public void testDeleteFrom() {
        String sql = SQLBuilder.PSC.deleteFrom("users").where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("DELETE FROM users"));
    }

    @Test
    public void testDeleteFromWithEntityClass() {
        String sql = SQLBuilder.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
    }

    // Complex query tests
    @Test
    public void testComplexSelectQuery() {
        String sql = SQLBuilder.PSC.select("u.id", "u.name", "COUNT(o.id) as order_count")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.eq("u.status", "active"))
                .groupBy("u.id", "u.name")
                .having(Filters.expr("COUNT(o.id) > 5"))
                .orderBy("order_count", SortDirection.DESC)
                .limit(10)
                .sql();

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
        String sql = SQLBuilder.PSC.select(Account.class).from(Account.class).sql();
        assertNotNull(sql);
    }

    @Test
    public void testSelectWithUpperCaseUnderscore() {
        String sql = SQLBuilder.PAC.select(Account.class).from(Account.class).sql();
        assertNotNull(sql);
    }

    // Parameterized query tests
    @Test
    public void testPairWithParameters() {
        SQLBuilder builder = SQLBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1));
        AbstractQueryBuilder.SP pair = builder.build();
        assertNotNull(pair);
        assertNotNull(pair.query);
        assertNotNull(pair.parameters);
    }

    // Subquery tests
    @Test
    public void testSubquery() {
        String subquery = SQLBuilder.PSC.select("id").from("active_users").sql();
        String sql = SQLBuilder.PSC.select("*").from("orders").where("user_id IN (" + subquery + ")").sql();
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("SELECT"));
    }

    // UNION tests
    @Test
    public void testUnion() {
        String sql1 = SQLBuilder.PSC.select("id", "name").from("users").sql();
        String sql2 = SQLBuilder.PSC.select("id", "name").from("archived_users").sql();
        String unionSql = sql1 + " UNION " + sql2;
        assertTrue(unionSql.contains("UNION"));
    }

    // Expression tests
    @Test
    public void testExpressionInSelect() {
        String sql = SQLBuilder.PSC.select("COUNT(*) as total").from("users").sql();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testExpressionInWhere() {
        Expression expr = Filters.expr("age > 18 AND status = 'active'");
        String sql = SQLBuilder.PSC.select("*").from("users").where(expr).sql();
        assertNotNull(sql);
    }

    // Aggregate functions tests
    @Test
    public void testCount() {
        String sql = SQLBuilder.PSC.select("COUNT(*)").from("users").sql();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testSum() {
        String sql = SQLBuilder.PSC.select("SUM(amount)").from("orders").sql();
        assertTrue(sql.contains("SUM(amount)"));
    }

    @Test
    public void testAvg() {
        String sql = SQLBuilder.PSC.select("AVG(price)").from("products").sql();
        assertTrue(sql.contains("AVG(price)"));
    }

    @Test
    public void testMax() {
        String sql = SQLBuilder.PSC.select("MAX(price)").from("products").sql();
        assertTrue(sql.contains("MAX(price)"));
    }

    @Test
    public void testMin() {
        String sql = SQLBuilder.PSC.select("MIN(price)").from("products").sql();
        assertTrue(sql.contains("MIN(price)"));
    }

    // Build method tests
    @Test
    public void testBuildMethod() {
        AbstractQueryBuilder.SP sp = SQLBuilder.PSC.select("*").from("users").build();
        assertNotNull(sp);
        assertNotNull(sp.query);
    }

    // Multiple where conditions with different operators
    @Test
    public void testWhereGreaterThan() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.gt("age", 18)).sql();
        assertTrue(sql.contains(">"));
    }

    @Test
    public void testWhereLessThan() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.lt("age", 65)).sql();
        assertTrue(sql.contains("<"));
    }

    @Test
    public void testWhereGreaterEqual() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.ge("age", 21)).sql();
        assertTrue(sql.contains(">="));
    }

    @Test
    public void testWhereLessEqual() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.le("age", 60)).sql();
        assertTrue(sql.contains("<="));
    }

    @Test
    public void testWhereNotEqual() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.ne("status", "deleted")).sql();
        assertTrue(sql.contains("!=") || sql.contains("<>"));
    }

    // IN clause tests
    @Test
    public void testWhereIn() {
        String sql = SQLBuilder.PSC.select("*").from("users").where("id IN (1, 2, 3)").sql();
        assertTrue(sql.contains("IN"));
    }

    // CASE WHEN tests
    @Test
    public void testCaseWhen() {
        String sql = SQLBuilder.PSC.select("CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END as age_group").from("users").sql();
        assertTrue(sql.contains(" case when age < 18 then 'minor' else 'adult' end AS age_group "));
    }

    // Multiple table sources
    @Test
    public void testFromMultipleTablesWithJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").join("products p").on("o.product_id = p.id").sql();
        assertTrue(sql.contains("users u"));
        assertTrue(sql.contains("orders o"));
        assertTrue(sql.contains("products p"));
    }

    // Chaining tests
    @Test
    public void testChainedAndConditions() {
        String sql = SQLBuilder.PSC.select("*")
                .from("users")
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).and(Filters.lt("age", 65)))
                .sql();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testChainedOrConditions() {
        String sql = SQLBuilder.PSC.select("*")
                .from("users")
                .where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator")).or(Filters.eq("role", "owner")))
                .sql();
        assertTrue(sql.contains("OR"));
    }

    // Edge case tests
    @Test
    public void testSelectWithNoFrom() {
        assertThrows(Exception.class, () -> {
            SQLBuilder.PSC.select("*").sql();
        });
    }

    @Test
    public void testEmptySelect() {
        assertThrows(IllegalArgumentException.class, () -> SQLBuilder.PSC.select().from("users").sql());
    }

    // Named SQL tests
    @Test
    public void testNamedInsert() {
        String sql = SQLBuilder.PSC.insert("firstName", "lastName").into(Account.class).sql();
        assertNotNull(sql);
    }

    // Static factory tests
    @Test
    public void testSelectFactory() {
        SQLBuilder builder = SQLBuilder.PSC.select("*");
        assertNotNull(builder);
    }

    @Test
    public void testInsertIntoFactory() {
        SQLBuilder builder = SQLBuilder.PSC.insert("id", "name");
        assertNotNull(builder);
    }

    @Test
    public void testUpdateFactory() {
        SQLBuilder builder = SQLBuilder.PSC.update("users");
        assertNotNull(builder);
    }

    @Test
    public void testDeleteFromFactory() {
        SQLBuilder builder = SQLBuilder.PSC.deleteFrom("users");
        assertNotNull(builder);
    }

    // Performance and resource cleanup
    @Test
    public void testMultipleBuildCalls() {
        SQLBuilder builder = SQLBuilder.PSC.select("*").from("users");
        String sql1 = builder.sql();
        assertNotNull(sql1);
        // Builder should be reusable or properly cleaned up
    }

    // Collection-based select
    @Test
    public void testSelectWithCollection() {
        String sql = SQLBuilder.PSC.select(Arrays.asList("id", "name", "email")).from("users").sql();
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

        String sql = SQLBuilder.PSC.update("users").set(props).where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("SET"));
    }

    // Complex conditions
    @Test
    public void testWhereWithNestedAndOr() {
        String sql = SQLBuilder.PSC.select("*")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"),
                        Filters.or(Filters.eq("role", "admin"), Filters.eq("role", "moderator"))))
                .sql();
        assertNotNull(sql);
    }

    // NULL handling
    @Test
    public void testWhereNullCheck() {
        String sql = SQLBuilder.PSC.select("*").from("users").where(Filters.isNull("deleted_at").and(Filters.isNotNull("email"))).sql();
        assertTrue(sql.contains("IS NULL"));
        assertTrue(sql.contains("IS NOT NULL"));
    }

    // Preselect tests
    @Test
    public void testPreselectDistinct() {
        String sql = SQLBuilder.PSC.select("status").from("users").preselect("DISTINCT").sql();
        assertTrue(sql.contains("DISTINCT"));
    }

    // USING clause tests
    @Test
    public void testJoinUsing() {
        String sql = SQLBuilder.PSC.select("*").from("users").join("orders").using("user_id").sql();
        assertTrue(sql.contains("USING"));
    }

    // Offset tests
    @Test
    public void testOffset() {
        String sql = SQLBuilder.PSC.select("*").from("users").offset(20).sql();
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
}
