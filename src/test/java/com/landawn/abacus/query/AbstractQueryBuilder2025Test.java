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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.entity.Account;

@Tag("2025")
public class AbstractQueryBuilder2025Test extends TestBase {

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
        String sql = SQLBuilder.PSC.select("id", "firstName", "lastName").from(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testPSCWithWhere() {
        String sql = SQLBuilder.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testPSCWithMultipleConditions() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testPSCWithOrderBy() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).orderBy("firstName").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testPSCWithLimit() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).limit(10).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testPSCWithJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testPSCWithLeftJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testPSCWithInnerJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testPSCWithRightJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testPSCWithFullJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testPSCWithCrossJoin() {
        String sql = SQLBuilder.PSC.select("*").from("users").crossJoin("roles").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testPSCWithGroupBy() {
        String sql = SQLBuilder.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testPSCWithHaving() {
        String sql = SQLBuilder.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testPSCWithDistinct() {
        String sql = SQLBuilder.PSC.select("status").from(Account.class).distinct().sql();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testPSCComplexQuery() {
        String sql = SQLBuilder.PSC.select("u.id", "u.firstName", "COUNT(o.id) as order_count")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.eq("u.status", "active"))
                .groupBy("u.id", "u.firstName")
                .having(Filters.expr("COUNT(o.id) > 0"))
                .orderBy("order_count", SortDirection.DESC)
                .limit(10)
                .sql();
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
        String sql = SQLBuilder.PSC.insertInto(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdate() {
        String sql = SQLBuilder.PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = SQLBuilder.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = SQLBuilder.PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("AS"));
    }

    @Test
    public void testSelectWithMultipleTables() {
        String sql = SQLBuilder.PSC.select("*").from("users", "orders").sql();
        assertNotNull(sql);
    }

    @Test
    public void testWhereWithOr() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testOrderByAsc() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testMultipleOrderBy() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).orderBy("lastName", "firstName").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).limit(20, 10).sql();
        assertNotNull(sql);
    }

    @Test
    public void testFromWithEntityClass() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testJoinWithEntityClass() {
        String sql = SQLBuilder.PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").sql();
        assertNotNull(sql);
    }

    @Test
    public void testIntoWithTableName() {
        String sql = SQLBuilder.PSC.insert("id", "name").into("accounts").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdateWithSet() {
        String sql = SQLBuilder.PSC.update("accounts").set("status", "inactive").set("updated_at", "NOW()").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFromWithTable() {
        String sql = SQLBuilder.PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectCount() {
        String sql = SQLBuilder.PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    public void testSelectAll() {
        String sql = SQLBuilder.PSC.select(AbstractQueryBuilder.ALL).from(Account.class).sql();
        assertNotNull(sql);
    }

    @Test
    public void testChainedAndOr() {
        String sql = SQLBuilder.PSC.select("*")
                .from(Account.class)
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).or(Filters.eq("role", "admin")))
                .sql();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
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
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
    }
}
