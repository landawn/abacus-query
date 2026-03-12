
package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.DynamicQuery.Builder;

@Tag("2025")
public class DynamicQueryTest extends TestBase {

    @Test
    public void testCreate() {
        Builder builder = DynamicQuery.builder();
        assertNotNull(builder);
    }

    @Test
    public void testSelectAppendSingleColumn() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id FROM users", sql);
    }

    @Test
    public void testSelectAppendMultipleColumns() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("id").append("name").append("email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendWithAlias() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("first_name", "fname").append("last_name", "lname");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT first_name AS fname, last_name AS lname FROM users", sql);
    }

    @Test
    public void testSelectAppendCollection() {
        Builder builder = DynamicQuery.builder();
        builder.select().append(Arrays.asList("id", "name", "email"));
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendEmptyCollectionDoesNotCreateSkeleton() {
        Builder builder = DynamicQuery.builder();
        builder.select().append(Arrays.asList());
        builder.from().append("users");
        String sql = builder.build();
        assertFalse(sql.contains("SELECT  FROM"));
    }

    @Test
    public void testSelectAppendMap() {
        Map<String, String> columns = new HashMap<>();
        columns.put("user_id", "uid");
        columns.put("user_name", "uname");

        Builder builder = DynamicQuery.builder();
        builder.select().append(columns);
        builder.from().append("users");
        String sql = builder.build();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("user_id AS uid"));
        assertTrue(sql.contains("user_name AS uname"));
    }

    @Test
    public void testSelectAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("id").appendIf(true, "name").appendIf(false, "email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name FROM users", sql);
    }

    @Test
    public void testSelectAppendIfOrElse() {
        Builder builder1 = DynamicQuery.builder();
        builder1.select().appendIfOrElse(true, "full_name", "first_name");
        builder1.from().append("users");
        String sql1 = builder1.build();
        assertEquals("SELECT full_name FROM users", sql1);

        Builder builder2 = DynamicQuery.builder();
        builder2.select().appendIfOrElse(false, "full_name", "first_name");
        builder2.from().append("users");
        String sql2 = builder2.build();
        assertEquals("SELECT first_name FROM users", sql2);
    }

    @Test
    public void testFromAppendTable() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testFromAppendTableWithAlias() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("u.id");
        builder.from().append("users", "u");
        String sql = builder.build();
        assertEquals("SELECT u.id FROM users u", sql);
    }

    @Test
    public void testFromAppendMultipleTables() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").append("orders", "o");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").join("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromJoin_ThrowsWhenFromNotInitialized() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().join("orders o", "u.id = o.user_id"));
    }

    @Test
    public void testFromInnerJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").innerJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromLeftJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromRightJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromFullJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").fullJoin("departments d", "u.dept_id = d.id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u FULL JOIN departments d ON u.dept_id = d.id", sql);
    }

    @Test
    public void testFromAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users").appendIf(true, "active_users").appendIf(false, "deleted_users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users, active_users", sql);
    }

    @Test
    public void testFromAppendIfOrElse() {
        Builder builder1 = DynamicQuery.builder();
        builder1.select().append("*");
        builder1.from().appendIfOrElse(true, "active_users", "all_users");
        String sql1 = builder1.build();
        assertEquals("SELECT * FROM active_users", sql1);

        Builder builder2 = DynamicQuery.builder();
        builder2.select().append("*");
        builder2.from().appendIfOrElse(false, "active_users", "all_users");
        String sql2 = builder2.build();
        assertEquals("SELECT * FROM all_users", sql2);
    }

    @Test
    public void testWhereAppend() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18", sql);
    }

    @Test
    public void testWhereAnd() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").and("status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereOr() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("role = 'admin'").or("role = 'moderator'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE role = 'admin' OR role = 'moderator'", sql);
    }

    @Test
    public void testWhereRepeatQuestionMark() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("id IN (").placeholders(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ? )", sql);
    }

    @Test
    public void testWhereRepeatQuestionMarkWithPrefixPostfix() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("status IN ").placeholders(2, "(", ")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status IN (?, ?)", sql);
    }

    @Test
    public void testWhereAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").appendIf(true, "AND status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereAppendIfOrElse() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().appendIfOrElse(true, "status = 'active'", "status = 'inactive'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status = 'active'", sql);
    }

    @Test
    public void testGroupByAppend() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department", sql);
    }

    @Test
    public void testGroupByAppendMultiple() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("year, month, COUNT(*)");
        builder.from().append("sales");
        builder.groupBy().append("year").append("month");
        String sql = builder.build();
        assertEquals("SELECT year, month, COUNT(*) FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testGroupByAppendCollection() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Arrays.asList("year", "quarter", "region"));
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, quarter, region", sql);
    }

    @Test
    public void testGroupByAppendEmptyCollectionNoOp() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Collections.emptyList());
        String sql = builder.build();
        assertEquals("SELECT * FROM sales", sql);
    }

    @Test
    public void testGroupByAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product").appendIf(true, "region");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product, region", sql);
    }

    @Test
    public void testGroupByAppendIfOrElse() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().appendIfOrElse(true, "year, month", "year");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testHavingAppend() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        builder.having().append("COUNT(*) > 5");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5", sql);
    }

    @Test
    public void testHavingAnd() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 10").and("SUM(amount) > 1000");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY region HAVING COUNT(*) > 10 AND SUM(amount) > 1000", sql);
    }

    @Test
    public void testHavingOr() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product");
        builder.having().append("COUNT(*) > 100").or("AVG(price) > 50");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product HAVING COUNT(*) > 100 OR AVG(price) > 50", sql);
    }

    @Test
    public void testHavingAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 0").appendIf(true, "AND SUM(revenue) > 5000");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 0 AND SUM(revenue) > 5000"));
    }

    @Test
    public void testHavingAppendIfOrElse() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().appendIfOrElse(true, "COUNT(*) > 100", "COUNT(*) > 10");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 100"));
    }

    @Test
    public void testOrderByAppend() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("created_date DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testOrderByAppendMultiple() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("last_name ASC").append("first_name ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testOrderByAppendCollection() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("products");
        builder.orderBy().append(Arrays.asList("category", "price DESC", "name"));
        String sql = builder.build();
        assertEquals("SELECT * FROM products ORDER BY category, price DESC, name", sql);
    }

    @Test
    public void testOrderByAppendEmptyCollectionNoOp() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("products");
        builder.orderBy().append(Collections.emptyList());
        String sql = builder.build();
        assertEquals("SELECT * FROM products", sql);
    }

    @Test
    public void testOrderByAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("name").appendIf(true, "age DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY name, age DESC", sql);
    }

    @Test
    public void testOrderByAppendIfOrElse() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().appendIfOrElse(true, "created_date DESC", "created_date ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testLimitString() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit("LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testLimitInt() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testLimitWithOffset() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10, 20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testLimitByRowNum() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.whereRowNumAtMost(100);
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE ROWNUM <= 100", sql);
    }

    @Test
    public void testLimitByRowNumWithWhere() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("active = 1");
        builder.whereRowNumAtMost(100);
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE active = 1 AND ROWNUM <= 100", sql);
    }

    @Test
    public void testOffsetRows() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS", sql);
    }

    @Test
    public void testFetchNextNRowsOnly() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        builder.fetchNextRows(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.fetchFirstRows(50);
        String sql = builder.build();
        assertEquals("SELECT * FROM users FETCH FIRST 50 ROWS ONLY", sql);
    }

    @Test
    public void testUnion() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("table1");
        builder.unionAll("SELECT * FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testIntersect() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("user_id");
        builder.from().append("orders");
        builder.intersect("SELECT user_id FROM premium_members");
        String sql = builder.build();
        assertTrue(sql.contains("INTERSECT"));
    }

    @Test
    public void testExcept() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("user_id");
        builder.from().append("all_users");
        builder.except("SELECT user_id FROM blocked_users");
        String sql = builder.build();
        assertTrue(sql.contains("EXCEPT"));
    }

    @Test
    public void testMinus() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append("table1");
        builder.minus("SELECT id FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("MINUS"));
    }

    @Test
    public void testComplexQuery() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
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

        Builder builder = DynamicQuery.builder();
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
    public void testWhereRepeatQuestionMarkZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            Builder builder = DynamicQuery.builder();
            builder.select().append("*");
            builder.from().append("users");
            builder.where().append("id IN (").placeholders(-1).append(")");
            builder.build();
        });
    }
}

/**
 * Simple unit tests for Builder functionality.
 * Tests basic factory method and builder creation.
 */
class SimpleDynamicQueryBuilderTest extends TestBase {

    @Test
    void testCreate() {
        Builder builder1 = DynamicQuery.builder();
        Builder builder2 = DynamicQuery.builder();

        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2); // Should return different instances
    }

    @Test
    void testClauseBuilders() {
        Builder builder = DynamicQuery.builder();

        assertNotNull(builder.select());
        assertNotNull(builder.from());
        assertNotNull(builder.where());
        assertNotNull(builder.groupBy());
        assertNotNull(builder.having());
        assertNotNull(builder.orderBy());
    }

    @Test
    void testBasicBuilding() {
        Builder builder = DynamicQuery.builder();

        // Test basic build - should not throw exception
        String sql = builder.build();
        assertNotNull(sql);
    }
}

/**
 * Javadoc usage examples for the DynamicQuery builder live here because DynamicQuery exposes the builder entry point.
 */
class DynamicQueryJavadocExamples extends TestBase {

    @Test
    public void testDynamicQueryBuilder_classLevelExample() {
        Builder b = DynamicQuery.builder();
        b.select().append("id", "user_id").append("name");
        b.from().append("users", "u");
        b.where().append("u.active = ?").and("u.age > ?");
        b.orderBy().append("u.name ASC");
        b.limit(10);
        String sql = b.build();
        assertEquals("SELECT id AS user_id, name FROM users u WHERE u.active = ? AND u.age > ? ORDER BY u.name ASC LIMIT 10", sql);
    }

    @Test
    public void testDynamicQueryBuilder_selectAppend() {
        Builder b = DynamicQuery.builder();
        b.select().append("id").append("name", "user_name");
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name AS user_name FROM users", sql);
    }

    @Test
    public void testDynamicQueryBuilder_fromWithJoin() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_where() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("status = ?").and("created_date > ?");
        String sql = b.build();
        assertTrue(sql.contains("WHERE status = ? AND created_date > ?"));
    }

    @Test
    public void testDynamicQueryBuilder_groupBy() {
        Builder b = DynamicQuery.builder();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        String sql = b.build();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testDynamicQueryBuilder_having() {
        Builder b = DynamicQuery.builder();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        b.having().append("COUNT(*) > ?");
        String sql = b.build();
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
    }

    @Test
    public void testDynamicQueryBuilder_orderBy() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.orderBy().append("created_date DESC").append("name ASC");
        String sql = b.build();
        assertTrue(sql.contains("ORDER BY created_date DESC, name ASC"));
    }

    @Test
    public void testDynamicQueryBuilder_limitIntInt() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.limit(10, 20);
        String sql = b.build();
        assertTrue(sql.contains("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testDynamicQueryBuilder_offsetAndFetch() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.offsetRows(20).fetchNextRows(10);
        String sql = b.build();
        assertTrue(sql.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicQueryBuilder_fetchFirst() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.fetchFirstRows(10);
        String sql = b.build();
        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicQueryBuilder_union() {
        Builder b = DynamicQuery.builder();
        b.select().append("id").append("name");
        b.from().append("active_users");
        b.union("SELECT id, name FROM archived_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION SELECT id, name FROM archived_users"));
    }

    @Test
    public void testDynamicQueryBuilder_unionAll() {
        Builder b = DynamicQuery.builder();
        b.select().append("id").append("name");
        b.from().append("users");
        b.unionAll("SELECT id, name FROM temp_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION ALL SELECT id, name FROM temp_users"));
    }

    @Test
    public void testDynamicQueryBuilder_build() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("active = true");
        String sql = b.build();
        assertEquals("SELECT * FROM users WHERE active = true", sql);
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendCollection() {
        Builder b = DynamicQuery.builder();
        b.select().append(Arrays.asList("id", "name", "email"));
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendIf() {
        boolean includeSalary = true;
        boolean includeBonus = false;
        Builder b = DynamicQuery.builder();
        b.select().append("id").appendIf(includeSalary, "salary").appendIf(includeBonus, "bonus");
        b.from().append("employees");
        String sql = b.build();
        assertTrue(sql.contains("salary"));
        assertFalse(sql.contains("bonus"));
    }

    @Test
    public void testDynamicQueryBuilder_intersect() {
        Builder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.intersect("SELECT user_id FROM premium_users");
        String sql = b.build();
        assertTrue(sql.contains("INTERSECT SELECT user_id FROM premium_users"));
    }

    @Test
    public void testDynamicQueryBuilder_except() {
        Builder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.except("SELECT user_id FROM blocked_users");
        String sql = b.build();
        assertTrue(sql.contains("EXCEPT SELECT user_id FROM blocked_users"));
    }

    @Test
    public void testDynamicQueryBuilder_minus() {
        Builder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.minus("SELECT user_id FROM inactive_users");
        String sql = b.build();
        assertTrue(sql.contains("MINUS SELECT user_id FROM inactive_users"));
    }

    @Test
    public void testDynamicQueryBuilder_fromAppendWithAlias() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u");
        String sql = b.build();
        assertEquals("SELECT * FROM users u", sql);
    }

    @Test
    public void testDynamicQueryBuilder_fromInnerJoin() {
        Builder b = DynamicQuery.builder();
        b.select().append("u.id").append("p.name");
        b.from().append("users", "u").innerJoin("products p", "u.id = p.user_id");
        String sql = b.build();
        assertTrue(sql.contains("INNER JOIN products p ON u.id = p.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_fromRightJoin() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("RIGHT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_fromFullJoin() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").fullJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("FULL JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_whereRowNumAtMost() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.whereRowNumAtMost(10);
        String sql = b.build();
        assertTrue(sql.contains("ROWNUM <= 10"));
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendIfOrElse() {
        Builder b1 = DynamicQuery.builder();
        b1.select().appendIfOrElse(true, "first_name || ' ' || last_name AS full_name", "first_name");
        b1.from().append("users");
        String sql1 = b1.build();
        assertTrue(sql1.contains("first_name || ' ' || last_name AS full_name"));

        Builder b2 = DynamicQuery.builder();
        b2.select().appendIfOrElse(false, "first_name || ' ' || last_name AS full_name", "first_name");
        b2.from().append("users");
        String sql2 = b2.build();
        assertTrue(sql2.contains("SELECT first_name FROM"));
        assertFalse(sql2.contains("full_name"));
    }

    @Test
    public void testDynamicQueryBuilder_whereOr() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("status = 'active'").or("role = 'admin'");
        String sql = b.build();
        assertTrue(sql.contains("WHERE status = 'active' OR role = 'admin'"));
    }

    @Test
    public void testDynamicQueryBuilder_whereAppendIf() {
        boolean filterByStatus = true;
        boolean filterByRole = false;
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("1 = 1").appendIf(filterByStatus, "AND status = 'active'").appendIf(filterByRole, "AND role = 'admin'");
        String sql = b.build();
        assertTrue(sql.contains("status = 'active'"));
        assertFalse(sql.contains("role = 'admin'"));
    }
}
