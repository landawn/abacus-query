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

@Tag("2025")
public class DynamicSQLBuilder2025Test extends TestBase {

    @Test
    public void testCreate() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        assertNotNull(builder);
    }

    @Test
    public void testSelectAppendSingleColumn() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id FROM users", sql);
    }

    @Test
    public void testSelectAppendMultipleColumns() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id").append("name").append("email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendWithAlias() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("first_name", "fname").append("last_name", "lname");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT first_name AS fname, last_name AS lname FROM users", sql);
    }

    @Test
    public void testSelectAppendCollection() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append(Arrays.asList("id", "name", "email"));
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendMap() {
        Map<String, String> columns = new HashMap<>();
        columns.put("user_id", "uid");
        columns.put("user_name", "uname");

        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append(columns);
        builder.from().append("users");
        String sql = builder.build();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("user_id AS uid"));
        assertTrue(sql.contains("user_name AS uname"));
    }

    @Test
    public void testSelectAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id").appendIf(true, "name").appendIf(false, "email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name FROM users", sql);
    }

    @Test
    public void testSelectAppendIfOrElse() {
        DynamicSQLBuilder builder1 = DynamicSQLBuilder.create();
        builder1.select().appendIfOrElse(true, "full_name", "first_name");
        builder1.from().append("users");
        String sql1 = builder1.build();
        assertEquals("SELECT full_name FROM users", sql1);

        DynamicSQLBuilder builder2 = DynamicSQLBuilder.create();
        builder2.select().appendIfOrElse(false, "full_name", "first_name");
        builder2.from().append("users");
        String sql2 = builder2.build();
        assertEquals("SELECT first_name FROM users", sql2);
    }

    @Test
    public void testFromAppendTable() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testFromAppendTableWithAlias() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("u.id");
        builder.from().append("users", "u");
        String sql = builder.build();
        assertEquals("SELECT u.id FROM users u", sql);
    }

    @Test
    public void testFromAppendMultipleTables() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").append("orders", "o");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromJoin() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").join("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromInnerJoin() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").innerJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromLeftJoin() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromRightJoin() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromFullJoin() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users", "u").fullJoin("departments d", "u.dept_id = d.id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u FULL JOIN departments d ON u.dept_id = d.id", sql);
    }

    @Test
    public void testFromAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users").appendIf(true, "active_users").appendIf(false, "deleted_users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users, active_users", sql);
    }

    @Test
    public void testFromAppendIfOrElse() {
        DynamicSQLBuilder builder1 = DynamicSQLBuilder.create();
        builder1.select().append("*");
        builder1.from().appendIfOrElse(true, "active_users", "all_users");
        String sql1 = builder1.build();
        assertEquals("SELECT * FROM active_users", sql1);

        DynamicSQLBuilder builder2 = DynamicSQLBuilder.create();
        builder2.select().append("*");
        builder2.from().appendIfOrElse(false, "active_users", "all_users");
        String sql2 = builder2.build();
        assertEquals("SELECT * FROM all_users", sql2);
    }

    @Test
    public void testWhereAppend() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18", sql);
    }

    @Test
    public void testWhereAnd() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").and("status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereOr() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("role = 'admin'").or("role = 'moderator'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE role = 'admin' OR role = 'moderator'", sql);
    }

    @Test
    public void testWhereRepeatQM() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("id IN (").repeatQM(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ? )", sql);
    }

    @Test
    public void testWhereRepeatQMWithPrefixPostfix() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("status IN ").repeatQM(2, "(", ")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status IN (?, ?)", sql);
    }

    @Test
    public void testWhereAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").appendIf(true, "AND status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereAppendIfOrElse() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().appendIfOrElse(true, "status = 'active'", "status = 'inactive'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status = 'active'", sql);
    }

    @Test
    public void testGroupByAppend() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department", sql);
    }

    @Test
    public void testGroupByAppendMultiple() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("year, month, COUNT(*)");
        builder.from().append("sales");
        builder.groupBy().append("year").append("month");
        String sql = builder.build();
        assertEquals("SELECT year, month, COUNT(*) FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testGroupByAppendCollection() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Arrays.asList("year", "quarter", "region"));
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, quarter, region", sql);
    }

    @Test
    public void testGroupByAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product").appendIf(true, "region");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product, region", sql);
    }

    @Test
    public void testGroupByAppendIfOrElse() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().appendIfOrElse(true, "year, month", "year");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testHavingAppend() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        builder.having().append("COUNT(*) > 5");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5", sql);
    }

    @Test
    public void testHavingAnd() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 10").and("SUM(amount) > 1000");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY region HAVING COUNT(*) > 10 AND SUM(amount) > 1000", sql);
    }

    @Test
    public void testHavingOr() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product");
        builder.having().append("COUNT(*) > 100").or("AVG(price) > 50");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product HAVING COUNT(*) > 100 OR AVG(price) > 50", sql);
    }

    @Test
    public void testHavingAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 0").appendIf(true, "AND SUM(revenue) > 5000");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 0 AND SUM(revenue) > 5000"));
    }

    @Test
    public void testHavingAppendIfOrElse() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().appendIfOrElse(true, "COUNT(*) > 100", "COUNT(*) > 10");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 100"));
    }

    @Test
    public void testOrderByAppend() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("created_date DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testOrderByAppendMultiple() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("last_name ASC").append("first_name ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testOrderByAppendCollection() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("products");
        builder.orderBy().append(Arrays.asList("category", "price DESC", "name"));
        String sql = builder.build();
        assertEquals("SELECT * FROM products ORDER BY category, price DESC, name", sql);
    }

    @Test
    public void testOrderByAppendIf() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("name").appendIf(true, "age DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY name, age DESC", sql);
    }

    @Test
    public void testOrderByAppendIfOrElse() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().appendIfOrElse(true, "created_date DESC", "created_date ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testLimitString() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit("LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testLimitInt() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testLimitWithOffset() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(20, 10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 20, 10", sql);
    }

    @Test
    public void testLimitByRowNum() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.limitByRowNum(100);
        String sql = builder.build();
        assertEquals("SELECT * FROM users ROWNUM <= 100", sql);
    }

    @Test
    public void testOffsetRows() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS", sql);
    }

    @Test
    public void testFetchNextNRowsOnly() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        builder.fetchNextNRowsOnly(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        builder.fetchFirstNRowsOnly(50);
        String sql = builder.build();
        assertEquals("SELECT * FROM users FETCH FIRST 50 ROWS ONLY", sql);
    }

    @Test
    public void testUnion() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id, name");
        builder.from().append("active_users");
        builder.union("SELECT id, name FROM inactive_users");
        String sql = builder.build();
        assertTrue(sql.contains("UNION"));
        assertTrue(sql.contains("active_users"));
        assertTrue(sql.contains("inactive_users"));
    }

    @Test
    public void testUnionAll() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("table1");
        builder.unionAll("SELECT * FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testIntersect() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("user_id");
        builder.from().append("orders");
        builder.intersect("SELECT user_id FROM premium_members");
        String sql = builder.build();
        assertTrue(sql.contains("INTERSECT"));
    }

    @Test
    public void testExcept() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("user_id");
        builder.from().append("all_users");
        builder.except("SELECT user_id FROM blocked_users");
        String sql = builder.build();
        assertTrue(sql.contains("EXCEPT"));
    }

    @Test
    public void testMinus() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id");
        builder.from().append("table1");
        builder.minus("SELECT id FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("MINUS"));
    }

    @Test
    public void testComplexQuery() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("u.id").append("u.name").append("COUNT(o.id)", "order_count");
        builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        builder.where().append("u.status = ?").and("u.created_date > ?");
        builder.groupBy().append("u.id").append("u.name");
        builder.having().append("COUNT(o.id) > 0");
        builder.orderBy().append("order_count DESC");
        builder.limit(10);
        String sql = builder.build();

        assertTrue(sql.contains("SELECT u.id, u.name, COUNT(o.id) AS order_count"));
        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
        assertTrue(sql.contains("WHERE u.status = ? AND u.created_date > ?"));
        assertTrue(sql.contains("GROUP BY u.id, u.name"));
        assertTrue(sql.contains("HAVING COUNT(o.id) > 0"));
        assertTrue(sql.contains("ORDER BY order_count DESC"));
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testMultipleJoins() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from()
                .append("users", "u")
                .innerJoin("orders o", "u.id = o.user_id")
                .leftJoin("products p", "o.product_id = p.id")
                .rightJoin("categories c", "p.category_id = c.id");
        String sql = builder.build();

        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testConditionalBuilding() {
        boolean includeEmail = true;
        boolean filterByAge = false;
        boolean orderByName = true;

        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("id").append("name").appendIf(includeEmail, "email");
        builder.from().append("users");
        builder.where().append("status = 'active'").appendIf(filterByAge, "AND age > 18");
        builder.orderBy().appendIf(orderByName, "name ASC");
        String sql = builder.build();

        assertTrue(sql.contains("email"));
        assertTrue(!sql.contains("age > 18"));
        assertTrue(sql.contains("ORDER BY name ASC"));
    }

    @Test
    public void testEmptyQuery() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        builder.select().append("*");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testWhereRepeatQMZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            DynamicSQLBuilder builder = DynamicSQLBuilder.create();
            builder.select().append("*");
            builder.from().append("users");
            builder.where().append("id IN (").repeatQM(-1).append(")");
            builder.build();
        });
    }
}
