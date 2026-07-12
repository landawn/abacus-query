package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.DynamicQuery.DynamicSqlBuilder;
import com.landawn.abacus.util.Objectory;

@Tag("2025")
public class DynamicQueryTest extends TestBase {
    @Test
    public void testCreate() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        assertNotNull(builder);
    }

    @Test
    public void testSelectAppendSingleColumn() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id FROM users", sql);
    }

    @Test
    public void testSelectAppendMultipleColumns() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id").append("name").append("email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendWithAlias() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("first_name", "fname").append("last_name", "lname");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT first_name AS fname, last_name AS lname FROM users", sql);
    }

    @Test
    public void testSelectAppendCollection() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append(Arrays.asList("id", "name", "email"));
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testSelectAppendEmptyCollectionDoesNotCreateSkeleton() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
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

        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append(columns);
        builder.from().append("users");
        String sql = builder.build();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("user_id AS uid"));
        assertTrue(sql.contains("user_name AS uname"));
    }

    @Test
    public void testSelectAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id").appendIf(true, "name").appendIf(false, "email");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT id, name FROM users", sql);
    }

    @Test
    public void testSelectAppendIfOrElse() {
        DynamicSqlBuilder builder1 = DynamicQuery.builder();
        builder1.select().appendIfOrElse(true, "full_name", "first_name");
        builder1.from().append("users");
        String sql1 = builder1.build();
        assertEquals("SELECT full_name FROM users", sql1);

        DynamicSqlBuilder builder2 = DynamicQuery.builder();
        builder2.select().appendIfOrElse(false, "full_name", "first_name");
        builder2.from().append("users");
        String sql2 = builder2.build();
        assertEquals("SELECT first_name FROM users", sql2);
    }

    @Test
    public void testFromAppendTable() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testFromAppendTableWithAlias() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("u.id");
        builder.from().append("users", "u");
        String sql = builder.build();
        assertEquals("SELECT u.id FROM users u", sql);
    }

    @Test
    public void testFromAppendMultipleTables() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").append("orders", "o");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromAppendCollection() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append(Arrays.asList("users", "departments"));
        String sql = builder.build();
        assertEquals("SELECT * FROM users, departments", sql);
    }

    @Test
    public void testFromAppendEmptyCollectionDoesNotCreateSkeleton() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append(Arrays.asList()).append("users");
        String sql = builder.build();
        assertEquals("SELECT id FROM users", sql);
    }

    @Test
    public void testFromAppendCollectionBlankElementThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.from().append(Arrays.asList("users", "   ")));
    }

    @Test
    public void testFromJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").join("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromJoin_ThrowsWhenFromNotInitialized() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().join("orders o", "u.id = o.user_id"));
    }

    @Test
    public void testFromInnerJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").innerJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromLeftJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromRightJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFromFullJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").fullJoin("departments d", "u.dept_id = d.id");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u FULL JOIN departments d ON u.dept_id = d.id", sql);
    }

    @Test
    public void testFromJoinWithoutOn() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").join("orders o USING (user_id)");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u JOIN orders o USING (user_id)", sql);
    }

    @Test
    public void testFromJoinWithoutOn_Validation() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().join("orders o"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().join("   "));
    }

    @Test
    public void testFromTypedJoinsWithoutOn() {
        DynamicSqlBuilder inner = DynamicQuery.builder();
        inner.select().append("*");
        inner.from().append("users", "u").innerJoin("orders o USING (user_id)");
        assertEquals("SELECT * FROM users u INNER JOIN orders o USING (user_id)", inner.build());

        DynamicSqlBuilder left = DynamicQuery.builder();
        left.select().append("*");
        left.from().append("users", "u").leftJoin("orders o USING (user_id)");
        assertEquals("SELECT * FROM users u LEFT JOIN orders o USING (user_id)", left.build());

        DynamicSqlBuilder right = DynamicQuery.builder();
        right.select().append("*");
        right.from().append("orders", "o").rightJoin("users u USING (user_id)");
        assertEquals("SELECT * FROM orders o RIGHT JOIN users u USING (user_id)", right.build());

        DynamicSqlBuilder full = DynamicQuery.builder();
        full.select().append("*");
        full.from().append("employees", "e").fullJoin("departments d USING (dept_id)");
        assertEquals("SELECT * FROM employees e FULL JOIN departments d USING (dept_id)", full.build());
    }

    @Test
    public void testFromTypedJoinsWithoutOn_Validation() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().innerJoin("orders o"));
        assertThrows(IllegalStateException.class, () -> builder.from().leftJoin("orders o"));
        assertThrows(IllegalStateException.class, () -> builder.from().rightJoin("orders o"));
        assertThrows(IllegalStateException.class, () -> builder.from().fullJoin("orders o"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().innerJoin("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.from().leftJoin("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.from().rightJoin("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.from().fullJoin("   "));
    }

    @Test
    public void testFromCrossJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").crossJoin("colors c");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u CROSS JOIN colors c", sql);
    }

    @Test
    public void testFromCrossJoin_Validation() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().crossJoin("colors c"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().crossJoin("   "));
    }

    @Test
    public void testFromNaturalJoin() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").naturalJoin("user_profiles");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u NATURAL JOIN user_profiles", sql);
    }

    @Test
    public void testFromNaturalJoin_Validation() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().naturalJoin("user_profiles"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().naturalJoin("   "));
    }

    @Test
    public void testFromAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users").appendIf(true, "active_users").appendIf(false, "deleted_users");
        String sql = builder.build();
        assertEquals("SELECT * FROM users, active_users", sql);
    }

    @Test
    public void testFromAppendIfOrElse() {
        DynamicSqlBuilder builder1 = DynamicQuery.builder();
        builder1.select().append("*");
        builder1.from().appendIfOrElse(true, "active_users", "all_users");
        String sql1 = builder1.build();
        assertEquals("SELECT * FROM active_users", sql1);

        DynamicSqlBuilder builder2 = DynamicQuery.builder();
        builder2.select().append("*");
        builder2.from().appendIfOrElse(false, "active_users", "all_users");
        String sql2 = builder2.build();
        assertEquals("SELECT * FROM all_users", sql2);
    }

    @Test
    public void testWhereAppend() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18", sql);
    }

    @Test
    public void testWhereAnd() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").and("status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereOr() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("role = 'admin'").or("role = 'moderator'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE role = 'admin' OR role = 'moderator'", sql);
    }

    @Test
    public void testWhereRepeatQuestionMark() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("id IN (").placeholders(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ? )", sql);
    }

    @Test
    public void testWhereRepeatQuestionMarkWithPrefixPostfix() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("status IN ").placeholders(2, "(", ")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status IN (?, ?)", sql);
    }

    @Test
    public void testWhereAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("age > 18").appendIf(true, "AND status = 'active'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'active'", sql);
    }

    @Test
    public void testWhereAppendIfOrElse() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().appendIfOrElse(true, "status = 'active'", "status = 'inactive'");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE status = 'active'", sql);
    }

    @Test
    public void testGroupByAppend() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department", sql);
    }

    @Test
    public void testGroupByAppendMultiple() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("year, month, COUNT(*)");
        builder.from().append("sales");
        builder.groupBy().append("year").append("month");
        String sql = builder.build();
        assertEquals("SELECT year, month, COUNT(*) FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testGroupByAppendCollection() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Arrays.asList("year", "quarter", "region"));
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, quarter, region", sql);
    }

    @Test
    public void testGroupByAppendEmptyCollectionNoOp() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Collections.emptyList());
        String sql = builder.build();
        assertEquals("SELECT * FROM sales", sql);
    }

    @Test
    public void testGroupByAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product").appendIf(true, "region");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product, region", sql);
    }

    @Test
    public void testGroupByAppendIfOrElse() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().appendIfOrElse(true, "year, month", "year");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY year, month", sql);
    }

    @Test
    public void testHavingAppend() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("department, COUNT(*)");
        builder.from().append("employees");
        builder.groupBy().append("department");
        builder.having().append("COUNT(*) > 5");
        String sql = builder.build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5", sql);
    }

    @Test
    public void testHavingAnd() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 10").and("SUM(amount) > 1000");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY region HAVING COUNT(*) > 10 AND SUM(amount) > 1000", sql);
    }

    @Test
    public void testHavingOr() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("product");
        builder.having().append("COUNT(*) > 100").or("AVG(price) > 50");
        String sql = builder.build();
        assertEquals("SELECT * FROM sales GROUP BY product HAVING COUNT(*) > 100 OR AVG(price) > 50", sql);
    }

    @Test
    public void testHavingAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) > 0").appendIf(true, "AND SUM(revenue) > 5000");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 0 AND SUM(revenue) > 5000"));
    }

    @Test
    public void testHavingAppendIfOrElse() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().appendIfOrElse(true, "COUNT(*) > 100", "COUNT(*) > 10");
        String sql = builder.build();
        assertTrue(sql.contains("HAVING COUNT(*) > 100"));
    }

    @Test
    public void testHavingPlaceholders() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) IN (").placeholders(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?, ? )", sql);
    }

    @Test
    public void testHavingPlaceholdersWithPrefixPostfix() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) IN ").placeholders(2, "(", ")");
        String sql = builder.build();
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?)", sql);
    }

    @Test
    public void testHavingPlaceholdersZeroEmitsNothing() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.having().append("x").placeholders(0, "(", ")");
        String sql = builder.build();
        assertEquals("HAVING x", sql);
    }

    @Test
    public void testHavingPlaceholdersNegativeThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        DynamicQuery.HavingClause having = builder.having();
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(-1));
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(-1, "(", ")"));
    }

    @Test
    public void testHavingPlaceholdersNullPrefixOrPostfixThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        DynamicQuery.HavingClause having = builder.having();
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(3, null, ")"));
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(3, "(", null));
    }

    @Test
    public void testOrderByAppend() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("created_date DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testOrderByAppendMultiple() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("last_name ASC").append("first_name ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testOrderByAppendCollection() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("products");
        builder.orderBy().append(Arrays.asList("category", "price DESC", "name"));
        String sql = builder.build();
        assertEquals("SELECT * FROM products ORDER BY category, price DESC, name", sql);
    }

    @Test
    public void testOrderByAppendEmptyCollectionNoOp() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("products");
        builder.orderBy().append(Collections.emptyList());
        String sql = builder.build();
        assertEquals("SELECT * FROM products", sql);
    }

    @Test
    public void testOrderByAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append("name").appendIf(true, "age DESC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY name, age DESC", sql);
    }

    @Test
    public void testOrderByAppendIfOrElse() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().appendIfOrElse(true, "created_date DESC", "created_date ASC");
        String sql = builder.build();
        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);
    }

    @Test
    public void testAppendRawClause() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append("LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testAppendRawClauseSpacingIsIdempotent() {
        // A leading space in the argument does not produce a doubled separating space.
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append(" LIMIT 10");
        assertEquals("SELECT * FROM users LIMIT 10", builder.build());

        // Consecutive appends are each separated by exactly one space, whether or not they carry one.
        builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append("FOR UPDATE").append(" OF users");
        assertEquals("SELECT * FROM users FOR UPDATE OF users", builder.build());
    }

    @Test
    public void testLimitInt() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testLimitWithOffset() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10, 20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffsetRows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS", sql);
    }

    @Test
    public void testOffset() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10).offset(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffsetNoRowsKeyword() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offset(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20", sql);
    }

    @Test
    public void testOffsetNegativeThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.offset(-1));
    }

    @Test
    public void testAppendRawFetchClause() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append("FETCH FIRST 10 ROWS ONLY");
        String sql = builder.build();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testAppendBlankThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.append("   "));
    }

    @Test
    public void testAppendIfTrueAppends() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(true, "LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testAppendIfFalseSkips() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(false, "LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testAppendIfReturnsSameBuilderForChaining() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(true, "FOR UPDATE").appendIf(false, "LIMIT 5");
        String sql = builder.build();
        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    }

    @Test
    public void testAppendIfTrueBlankThrows() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, null));
    }

    @Test
    public void testAppendIfFalseBlankDoesNotThrow() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        // When the condition is false the fragment is never inspected (consistent with the clause-level appendIf).
        builder.appendIf(false, "   ");
        builder.appendIf(false, null);
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testAppendIfOrElseTrueAppendsFirst() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(true, "LIMIT 10", "LIMIT 100");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testAppendIfOrElseFalseAppendsSecond() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(false, "LIMIT 10", "LIMIT 100");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 100", sql);
    }

    @Test
    public void testAppendIfOrElseReturnsSameBuilderForChaining() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(false, "LIMIT 10", "LIMIT 100").appendIf(true, "FOR UPDATE");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 100 FOR UPDATE", sql);
    }

    @Test
    public void testAppendIfOrElseBlankThrowsRegardlessOfCondition() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        // Both fragments are validated up front, matching the clause-level appendIfOrElse contract.
        assertThrows(IllegalArgumentException.class, () -> builder.appendIfOrElse(true, "LIMIT 10", "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.appendIfOrElse(false, null, "LIMIT 100"));
    }

    @Test
    public void testFetchNextNRowsOnly() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(20);
        builder.fetchNextRows(10);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.fetchFirstRows(50);
        String sql = builder.build();
        assertEquals("SELECT * FROM users FETCH FIRST 50 ROWS ONLY", sql);
    }

    @Test
    public void testUnion() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
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
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("table1");
        builder.unionAll("SELECT * FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testIntersect() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("user_id");
        builder.from().append("orders");
        builder.intersect("SELECT user_id FROM premium_members");
        String sql = builder.build();
        assertTrue(sql.contains("INTERSECT"));
    }

    @Test
    public void testExcept() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("user_id");
        builder.from().append("all_users");
        builder.except("SELECT user_id FROM blocked_users");
        String sql = builder.build();
        assertTrue(sql.contains("EXCEPT"));
    }

    @Test
    public void testMinus() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append("table1");
        builder.minus("SELECT id FROM table2");
        String sql = builder.build();
        assertTrue(sql.contains("MINUS"));
    }

    @Test
    public void testComplexQuery() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
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
        DynamicSqlBuilder builder = DynamicQuery.builder();
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

        DynamicSqlBuilder builder = DynamicQuery.builder();
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
            DynamicSqlBuilder builder = DynamicQuery.builder();
            builder.select().append("*");
            builder.from().append("users");
            builder.where().append("id IN (").placeholders(-1).append(")");
            builder.build();
        });
    }

    @Test
    public void testStringInputsRejectNullInsteadOfRenderingLiteralNull() {
        DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.select().append((String) null));
        assertThrows(IllegalArgumentException.class, () -> builder.select().append("id", null));
        assertThrows(IllegalArgumentException.class, () -> builder.from().append((String) null));
        assertThrows(IllegalArgumentException.class, () -> builder.from().append("users", null));

        builder.select().append("*");
        builder.from().append("users");

        assertThrows(IllegalArgumentException.class, () -> builder.from().join(null, "users.id = orders.user_id"));
        assertThrows(IllegalArgumentException.class, () -> builder.from().join("orders", null));
        assertThrows(IllegalArgumentException.class, () -> builder.where().append(null));
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendIf(true, null));
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendIfOrElse(true, null, "status = 'inactive'"));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().append((String) null));
        assertThrows(IllegalArgumentException.class, () -> builder.having().append(null));
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().append((String) null));
    }

    @Test
    void testClauseBuilders() {
        DynamicSqlBuilder builder = DynamicQuery.builder();

        assertNotNull(builder.select());
        assertNotNull(builder.from());
        assertNotNull(builder.where());
        assertNotNull(builder.groupBy());
        assertNotNull(builder.having());
        assertNotNull(builder.orderBy());
    }

    @Test
    void testBasicBuilding() {
        DynamicSqlBuilder builder = DynamicQuery.builder();

        // Test basic build - should not throw exception
        String sql = builder.build();
        assertNotNull(sql);
    }

    @Test
    public void testDynamicQueryBuilder_classLevelExample() {
        DynamicSqlBuilder b = DynamicQuery.builder();
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
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("id").append("name", "user_name");
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name AS user_name FROM users", sql);
    }

    @Test
    public void testDynamicQueryBuilder_fromWithJoin() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_where() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("status = ?").and("created_date > ?");
        String sql = b.build();
        assertTrue(sql.contains("WHERE status = ? AND created_date > ?"));
    }

    @Test
    public void testDynamicQueryBuilder_groupBy() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        String sql = b.build();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testDynamicQueryBuilder_having() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        b.having().append("COUNT(*) > ?");
        String sql = b.build();
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
    }

    @Test
    public void testDynamicQueryBuilder_orderBy() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.orderBy().append("created_date DESC").append("name ASC");
        String sql = b.build();
        assertTrue(sql.contains("ORDER BY created_date DESC, name ASC"));
    }

    @Test
    public void testDynamicQueryBuilder_limitIntInt() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.limit(10, 20);
        String sql = b.build();
        assertTrue(sql.contains("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testDynamicQueryBuilder_offsetAndFetch() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.offsetRows(20).fetchNextRows(10);
        String sql = b.build();
        assertTrue(sql.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicQueryBuilder_fetchFirst() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.fetchFirstRows(10);
        String sql = b.build();
        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicQueryBuilder_union() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("id").append("name");
        b.from().append("active_users");
        b.union("SELECT id, name FROM archived_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION SELECT id, name FROM archived_users"));
    }

    @Test
    public void testDynamicQueryBuilder_unionAll() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("id").append("name");
        b.from().append("users");
        b.unionAll("SELECT id, name FROM temp_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION ALL SELECT id, name FROM temp_users"));
    }

    @Test
    public void testDynamicQueryBuilder_build() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("active = true");
        String sql = b.build();
        assertEquals("SELECT * FROM users WHERE active = true", sql);
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendCollection() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append(Arrays.asList("id", "name", "email"));
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendIf() {
        boolean includeSalary = true;
        boolean includeBonus = false;
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("id").appendIf(includeSalary, "salary").appendIf(includeBonus, "bonus");
        b.from().append("employees");
        String sql = b.build();
        assertTrue(sql.contains("salary"));
        assertFalse(sql.contains("bonus"));
    }

    @Test
    public void testDynamicQueryBuilder_intersect() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.intersect("SELECT user_id FROM premium_users");
        String sql = b.build();
        assertTrue(sql.contains("INTERSECT SELECT user_id FROM premium_users"));
    }

    @Test
    public void testDynamicQueryBuilder_except() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.except("SELECT user_id FROM blocked_users");
        String sql = b.build();
        assertTrue(sql.contains("EXCEPT SELECT user_id FROM blocked_users"));
    }

    @Test
    public void testDynamicQueryBuilder_minus() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("user_id");
        b.from().append("all_users");
        b.minus("SELECT user_id FROM inactive_users");
        String sql = b.build();
        assertTrue(sql.contains("MINUS SELECT user_id FROM inactive_users"));
    }

    @Test
    public void testDynamicQueryBuilder_fromAppendWithAlias() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u");
        String sql = b.build();
        assertEquals("SELECT * FROM users u", sql);
    }

    @Test
    public void testDynamicQueryBuilder_fromInnerJoin() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("u.id").append("p.name");
        b.from().append("users", "u").innerJoin("products p", "u.id = p.user_id");
        String sql = b.build();
        assertTrue(sql.contains("INNER JOIN products p ON u.id = p.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_fromRightJoin() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("RIGHT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_fromFullJoin() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users", "u").fullJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("FULL JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicQueryBuilder_selectAppendIfOrElse() {
        DynamicSqlBuilder b1 = DynamicQuery.builder();
        b1.select().appendIfOrElse(true, "first_name || ' ' || last_name AS full_name", "first_name");
        b1.from().append("users");
        String sql1 = b1.build();
        assertTrue(sql1.contains("first_name || ' ' || last_name AS full_name"));

        DynamicSqlBuilder b2 = DynamicQuery.builder();
        b2.select().appendIfOrElse(false, "first_name || ' ' || last_name AS full_name", "first_name");
        b2.from().append("users");
        String sql2 = b2.build();
        assertTrue(sql2.contains("SELECT first_name FROM"));
        assertFalse(sql2.contains("full_name"));
    }

    @Test
    public void testDynamicQueryBuilder_whereOr() {
        DynamicSqlBuilder b = DynamicQuery.builder();
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
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("users");
        b.where().append("1 = 1").appendIf(filterByStatus, "AND status = 'active'").appendIf(filterByRole, "AND role = 'admin'");
        String sql = b.build();
        assertTrue(sql.contains("status = 'active'"));
        assertFalse(sql.contains("role = 'admin'"));
    }

    // Covers clause builders when the conditional branch is the first text appended.
    @Test
    public void testConditionalClauseBuilders_EmptyInitialState() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().appendIfOrElse(false, "id", "user_id");
        builder.from().appendIfOrElse(false, "archived_users", "active_users");
        builder.where().appendIfOrElse(false, "status = 'archived'", "status = 'active'");
        builder.groupBy().appendIfOrElse(false, "archived_region", "active_region");
        builder.having().appendIfOrElse(false, "COUNT(*) > 100", "COUNT(*) > 0");
        builder.orderBy().appendIfOrElse(false, "created_at DESC", "user_id ASC");

        String sql = builder.build();

        assertTrue(sql.contains("SELECT user_id"));
        assertTrue(sql.contains("FROM active_users"));
        assertTrue(sql.contains("WHERE status = 'active'"));
        assertTrue(sql.contains("GROUP BY active_region"));
        assertTrue(sql.contains("HAVING COUNT(*) > 0"));
        assertTrue(sql.contains("ORDER BY user_id ASC"));
    }

    @Test
    public void testSelectClause_appendMapAndCollectionAfterExistingContent() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("id").append(Map.of("first_name", "firstName", "last_name", "lastName")).append(Arrays.asList("email", "status"));
        builder.from().append("users");

        String sql = builder.build();

        assertTrue(sql.contains("SELECT id, "));
        assertTrue(sql.contains("first_name AS firstName"));
        assertTrue(sql.contains("last_name AS lastName"));
        assertTrue(sql.contains("email, status"));
    }

    @Test
    public void testWhereClause_andOrFromEmpty() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().and("status = 'active'").or("role = 'admin'");

        String sql = builder.build();

        assertEquals("SELECT * FROM users WHERE status = 'active' OR role = 'admin'", sql);
    }

    @Test
    public void testWhereClause_appendIf_FalseOnEmpty() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().appendIf(false, "status = 'active'");

        assertEquals("SELECT * FROM users", builder.build());
    }

    @Test
    public void testGroupByClause_appendCollectionThenAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Arrays.asList("year", "quarter")).appendIf(true, "region");

        String sql = builder.build();

        assertTrue(sql.contains("GROUP BY year, quarter, region"));
    }

    @Test
    public void testHavingClause_andOrFromEmpty() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().and("COUNT(*) > 1").or("SUM(amount) > 0");

        String sql = builder.build();

        assertTrue(sql.contains("HAVING COUNT(*) > 1 OR SUM(amount) > 0"));
    }

    @Test
    public void testOrderByClause_appendCollectionThenAppendIf() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.orderBy().append(Arrays.asList("created_at DESC", "id ASC")).appendIf(true, "name ASC");

        String sql = builder.build();

        assertTrue(sql.contains("ORDER BY created_at DESC, id ASC, name ASC"));
    }

    @Test
    public void testOrderByClause_StandaloneBuilderBranches_Batch2() {
        StringBuilder sb = new StringBuilder();
        DynamicQuery.OrderByClause clause = new DynamicQuery.OrderByClause(sb);

        clause.append(java.util.Collections.emptyList());
        clause.append("name ASC").appendIf(false, "ignored DESC").appendIf(true, "created_at DESC").appendIfOrElse(false, "priority DESC", "id ASC");

        assertEquals("ORDER BY name ASC, created_at DESC, id ASC", sb.toString());
    }

    @Test
    public void selectClause_rejectsEmptyAlias() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.select().append("id", ""));
        assertThrows(IllegalArgumentException.class, () -> builder.select().append("id", "   "));
    }

    @Test
    public void selectClause_rejectsBlankConditionalAndCollectionFragments() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();
        final Map<String, String> columnsAndAliasMap = new LinkedHashMap<>();
        columnsAndAliasMap.put("id", "   ");

        assertThrows(IllegalArgumentException.class, () -> builder.select().append(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> builder.select().append(columnsAndAliasMap));
        assertThrows(IllegalArgumentException.class, () -> builder.select().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.select().appendIfOrElse(true, "   ", "name"));
        assertThrows(IllegalArgumentException.class, () -> builder.select().appendIfOrElse(false, "name", "   "));
    }

    @Test
    public void fromClause_rejectsEmptyAliasAndBlankJoinFragments() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.from().append("users", ""));
        assertThrows(IllegalArgumentException.class, () -> builder.from().append("users", "   "));

        builder.from().append("users");

        assertThrows(IllegalArgumentException.class, () -> builder.from().join("   ", "users.id = orders.user_id"));
        assertThrows(IllegalArgumentException.class, () -> builder.from().join("orders", "   "));
    }

    @Test
    public void builder_rejectsBlankSetOperationAndLimitFragments() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.union("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.unionAll("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.intersect("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.except("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.minus("   "));
    }

    @Test
    public void whereClause_rejectsBlankConditions() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.where().append(""));
        assertThrows(IllegalArgumentException.class, () -> builder.where().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.where().and(""));
        assertThrows(IllegalArgumentException.class, () -> builder.where().or("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendIfOrElse(true, "   ", "id = 1"));
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendIfOrElse(false, "id = 1", "   "));
    }

    @Test
    public void groupByClause_rejectsBlankColumns() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().append(Arrays.asList("year", "   ")));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIfOrElse(true, "   ", "year"));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIfOrElse(false, "year", "   "));
    }

    @Test
    public void havingClause_rejectsBlankConditions() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.having().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().and("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().or("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIfOrElse(true, "   ", "COUNT(*) > 0"));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIfOrElse(false, "COUNT(*) > 0", "   "));
    }

    @Test
    public void orderByClause_rejectsBlankColumns() {
        final DynamicSqlBuilder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().append(Arrays.asList("id ASC", "   ")));
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().appendIfOrElse(true, "   ", "id ASC"));
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy().appendIfOrElse(false, "id ASC", "   "));
    }

    // --- 2nd-pass review verification tests ---

    @Test
    public void test2ndPass_buildClauseOrder_isSelectFromWhereGroupByHavingOrderByLimit() {
        // Order must be: SELECT -> FROM -> WHERE -> GROUP BY -> HAVING -> ORDER BY -> LIMIT
        // Even if the builder methods are called in a different order.
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.limit(5); // LIMIT first (in user code order)
        b.orderBy().append("name"); // ORDER BY second
        b.having().append("COUNT(*) > 0");
        b.groupBy().append("dept");
        b.where().append("active = true");
        b.from().append("users");
        b.select().append("*");

        String sql = b.build();

        int selectIdx = sql.indexOf("SELECT");
        int fromIdx = sql.indexOf("FROM");
        int whereIdx = sql.indexOf("WHERE");
        int groupIdx = sql.indexOf("GROUP BY");
        int havingIdx = sql.indexOf("HAVING");
        int orderIdx = sql.indexOf("ORDER BY");
        int limitIdx = sql.indexOf("LIMIT");

        assertTrue(selectIdx >= 0 && selectIdx < fromIdx, "SELECT must come before FROM");
        assertTrue(fromIdx < whereIdx, "FROM must come before WHERE");
        assertTrue(whereIdx < groupIdx, "WHERE must come before GROUP BY");
        assertTrue(groupIdx < havingIdx, "GROUP BY must come before HAVING");
        assertTrue(havingIdx < orderIdx, "HAVING must come before ORDER BY");
        assertTrue(orderIdx < limitIdx, "ORDER BY must come before LIMIT");
    }

    @Test
    public void test2ndPass_whereCalledTwice_returnsSameInstance_andAppends() {
        // Repeated .where() should return the same WhereClause, and successive .append() calls accumulate.
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");

        var w1 = b.where();
        var w2 = b.where();
        assertSame(w1, w2, "where() must return same instance on repeated calls");

        b.where().append("a = 1");
        b.where().append("AND b = 2"); // .append uses space, not AND/OR
        String sql = b.build();
        assertTrue(sql.contains("WHERE a = 1 AND b = 2"), "Successive appends should accumulate: " + sql);
    }

    @Test
    public void test2ndPass_orderByCalledTwice_returnsSameInstance() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        var o1 = b.orderBy();
        var o2 = b.orderBy();
        assertSame(o1, o2);
    }

    @Test
    public void test2ndPass_placeholders_nZero_emitsNothing() {
        // placeholders(0) should write nothing to the buffer.
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN (").placeholders(0); // adds nothing after "("
        // Then we'd usually close with ")" via raw append
        b.where().append(")");
        String sql = b.build();
        assertTrue(sql.contains("id IN ( )"), "0 placeholders should add nothing between '(' and ')' but got: " + sql);
    }

    @Test
    public void test2ndPass_placeholders_nOne_emitsSingleQuestionMark_noTrailingComma() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN ").placeholders(1, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE id IN (?)", sql);
    }

    @Test
    public void test2ndPass_placeholders_nThree_emitsCorrectSeparators() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN ").placeholders(3, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE id IN (?, ?, ?)", sql);
    }

    @Test
    public void test2ndPass_placeholdersWithPrefixPostfix_nZero_emitsNeitherPrefixNorPostfix() {
        // Documented: "If placeholderCount is 0, neither prefix nor postfix is appended."
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("x").placeholders(0, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE x", sql);
    }

    @Test
    public void test2ndPass_buildTwice_secondCallThrowsISE() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.build();
        assertThrows(IllegalStateException.class, b::build);
    }

    @Test
    public void testBuild_RawOnlyQueryHasNoSyntheticLeadingSpace() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.append("FOR UPDATE");

        assertEquals("FOR UPDATE", builder.build());
    }

    @Test
    public void testClauseBuildersRejectMutationAfterBuild() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        DynamicQuery.SelectClause select = builder.select();
        DynamicQuery.FromClause from = builder.from();
        DynamicQuery.WhereClause where = builder.where();
        DynamicQuery.GroupByClause groupBy = builder.groupBy();
        DynamicQuery.HavingClause having = builder.having();
        DynamicQuery.OrderByClause orderBy = builder.orderBy();

        select.append("*");
        from.append("users");
        where.append("active = true");
        groupBy.append("region");
        having.append("COUNT(*) > 0");
        orderBy.append("id ASC");

        builder.build();

        assertThrows(IllegalStateException.class, () -> select.append("name"));
        assertThrows(IllegalStateException.class, () -> from.append("orders"));
        assertThrows(IllegalStateException.class, () -> where.and("id = 1"));
        assertThrows(IllegalStateException.class, () -> groupBy.append("department"));
        assertThrows(IllegalStateException.class, () -> having.or("COUNT(*) > 10"));
        assertThrows(IllegalStateException.class, () -> orderBy.append("name ASC"));
    }

    @Test
    public void test2ndPass_placeholdersNegative_throwsIAE() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        var w = b.where().append("x");
        assertThrows(IllegalArgumentException.class, () -> w.placeholders(-1));
        assertThrows(IllegalArgumentException.class, () -> w.placeholders(-1, "(", ")"));
    }

    @Test
    public void test2ndPass_placeholdersNullPrefixOrPostfix_throws() {
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        var w = b.where().append("x");
        assertThrows(IllegalArgumentException.class, () -> w.placeholders(3, null, ")"));
        assertThrows(IllegalArgumentException.class, () -> w.placeholders(3, "(", null));
    }

    @Test
    public void test2ndPass_joinBeforeFromAppend_throwsIllegalState() {
        // requireFromInitialized: join methods must be preceded by from().append(...)
        DynamicSqlBuilder b = DynamicQuery.builder();
        b.select().append("*");
        var f = b.from();
        assertThrows(IllegalStateException.class, () -> f.leftJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.innerJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.rightJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.fullJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.join("orders o", "u.id = o.uid"));
    }

    @Test
    public void testBuilderMethodsRejectUseAfterBuild() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.build();

        // Clause accessors must not return null or resurrect fresh clause buffers.
        assertThrows(IllegalStateException.class, builder::select);
        assertThrows(IllegalStateException.class, builder::from);
        assertThrows(IllegalStateException.class, builder::where);
        assertThrows(IllegalStateException.class, builder::groupBy);
        assertThrows(IllegalStateException.class, builder::having);
        assertThrows(IllegalStateException.class, builder::orderBy);

        // Pagination methods must not silently write into a fresh, leaked moreParts buffer.
        assertThrows(IllegalStateException.class, () -> builder.limit(10));
        assertThrows(IllegalStateException.class, () -> builder.limit(10, 20));
        assertThrows(IllegalStateException.class, () -> builder.offset(20));
        assertThrows(IllegalStateException.class, () -> builder.offsetRows(20));
        assertThrows(IllegalStateException.class, () -> builder.fetchNextRows(10));
        assertThrows(IllegalStateException.class, () -> builder.fetchFirstRows(10));

        // Set operations.
        assertThrows(IllegalStateException.class, () -> builder.union("SELECT id FROM archived_users"));
        assertThrows(IllegalStateException.class, () -> builder.unionAll("SELECT id FROM temp_users"));
        assertThrows(IllegalStateException.class, () -> builder.intersect("SELECT id FROM premium_users"));
        assertThrows(IllegalStateException.class, () -> builder.except("SELECT id FROM blocked_users"));
        assertThrows(IllegalStateException.class, () -> builder.minus("SELECT id FROM inactive_users"));

        // Raw appends (appendIf throws even when the condition is false: reuse itself is the misuse).
        assertThrows(IllegalStateException.class, () -> builder.append("FOR UPDATE"));
        assertThrows(IllegalStateException.class, () -> builder.appendIf(true, "FOR UPDATE"));
        assertThrows(IllegalStateException.class, () -> builder.appendIf(false, "FOR UPDATE"));
        assertThrows(IllegalStateException.class, () -> builder.appendIfOrElse(true, "LIMIT 10", "LIMIT 100"));

        // And build() itself still rejects a second call.
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void testRetainedClauseNoOpMethodsRejectUseAfterBuild() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        DynamicQuery.SelectClause select = builder.select().append("region");
        DynamicQuery.FromClause from = builder.from().append("sales");
        DynamicQuery.WhereClause where = builder.where().append("active = true");
        DynamicQuery.GroupByClause groupBy = builder.groupBy().append("region");
        DynamicQuery.HavingClause having = builder.having().append("COUNT(*) > 0");
        DynamicQuery.OrderByClause orderBy = builder.orderBy().append("region");

        builder.build();

        assertThrows(IllegalStateException.class, () -> select.append(Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> select.append(Collections.emptyMap()));
        assertThrows(IllegalStateException.class, () -> select.appendIf(false, null));
        assertThrows(IllegalStateException.class, () -> from.append(Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> from.appendIf(false, null));
        assertThrows(IllegalStateException.class, () -> where.appendIf(false, null));
        assertThrows(IllegalStateException.class, () -> groupBy.append(Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> groupBy.appendIf(false, null));
        assertThrows(IllegalStateException.class, () -> having.appendIf(false, null));
        assertThrows(IllegalStateException.class, () -> orderBy.append(Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> orderBy.appendIf(false, null));

        // Lifecycle errors take precedence over argument validation after the owning builder is closed.
        assertThrows(IllegalStateException.class, () -> select.append((String) null));
        assertThrows(IllegalStateException.class, () -> from.append((String) null));
        assertThrows(IllegalStateException.class, () -> where.append(null));
        assertThrows(IllegalStateException.class, () -> groupBy.append((String) null));
        assertThrows(IllegalStateException.class, () -> having.append(null));
        assertThrows(IllegalStateException.class, () -> orderBy.append((String) null));
    }

    @Test
    public void testWherePlaceholdersRequireInitializedClause() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        DynamicQuery.WhereClause where = builder.where();

        // Without a prior append/and/or, placeholders would emit "... FROM users ?, ?, ?".
        assertThrows(IllegalStateException.class, () -> where.placeholders(3));
        assertThrows(IllegalStateException.class, () -> where.placeholders(3, "(", ")"));

        // Argument validation still comes first (matches the FromClause join ordering).
        assertThrows(IllegalArgumentException.class, () -> where.placeholders(-1));
        assertThrows(IllegalArgumentException.class, () -> where.placeholders(3, null, ")"));

        // Once initialized, placeholders work as before.
        where.append("id IN ").placeholders(2, "(", ")");
        assertEquals("SELECT * FROM users WHERE id IN (?, ?)", builder.build());
    }

    @Test
    public void testHavingPlaceholdersRequireInitializedClause() {
        DynamicSqlBuilder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        DynamicQuery.HavingClause having = builder.having();

        // Without a prior append/and/or, placeholders would emit "... GROUP BY region ?, ?".
        assertThrows(IllegalStateException.class, () -> having.placeholders(2));
        assertThrows(IllegalStateException.class, () -> having.placeholders(2, "(", ")"));

        // Argument validation still comes first (matches the FromClause join ordering).
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(-1));
        assertThrows(IllegalArgumentException.class, () -> having.placeholders(2, "(", null));

        // Once initialized, placeholders work as before.
        having.append("COUNT(*) IN ").placeholders(2, "(", ")");
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?)", builder.build());
    }

    @Test
    public void testSetOperationRendersAfterOrderByRegardlessOfCallOrder() {
        // Documented behavior: set operations live in the trailing buffer, which build() appends
        // after every clause builder — orderBy() therefore always renders before the set operator.
        DynamicSqlBuilder b1 = DynamicQuery.builder();
        b1.select().append("id");
        b1.from().append("t1");
        b1.union("SELECT id FROM t2");
        b1.orderBy().append("id");
        assertEquals("SELECT id FROM t1 ORDER BY id UNION SELECT id FROM t2", b1.build());

        // Recommended pattern: order the combined result with append(...) after the set operation.
        DynamicSqlBuilder b2 = DynamicQuery.builder();
        b2.select().append("id");
        b2.from().append("t1");
        b2.union("SELECT id FROM t2");
        b2.append("ORDER BY id");
        assertEquals("SELECT id FROM t1 UNION SELECT id FROM t2 ORDER BY id", b2.build());
    }

    @Test
    public void testClauseBuilderCloseIsIdempotent() {
        DynamicQuery.OrderByClause clause = new DynamicQuery.OrderByClause(Objectory.createStringBuilder());
        clause.append("id ASC");

        clause.close();
        clause.close(); // second close must be a no-op (no double recycle into the buffer pool)

        assertThrows(IllegalStateException.class, () -> clause.append("name ASC"));
    }
}
