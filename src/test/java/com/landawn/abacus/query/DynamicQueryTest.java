package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.DynamicQuery.Builder;
import com.landawn.abacus.util.Objectory;

@Tag("2025")
public class DynamicQueryTest extends TestBase {
    @Test
    public void testCreate() {
        Builder builder = DynamicQuery.builder();
        assertNotNull(builder);
    }

    @Test
    public void testAppendPlaceholdersIsAHardRename() throws NoSuchMethodException {
        assertEquals(DynamicQuery.WhereClause.class, DynamicQuery.WhereClause.class.getMethod("appendPlaceholders", int.class).getReturnType());
        assertEquals(DynamicQuery.HavingClause.class, DynamicQuery.HavingClause.class.getMethod("appendPlaceholders", int.class).getReturnType());

        assertThrows(NoSuchMethodException.class, () -> DynamicQuery.WhereClause.class.getMethod("placeholders", int.class));
        assertThrows(NoSuchMethodException.class, () -> DynamicQuery.HavingClause.class.getMethod("placeholders", int.class));
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
    public void testSelectAppendCollectionValidatesAndRendersOneSnapshot() {
        Collection<String> changingCollection = new AbstractCollection<String>() {
            private int iteratorCalls;

            @Override
            public java.util.Iterator<String> iterator() {
                return Collections.singleton(iteratorCalls++ == 0 ? "id" : "   ").iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        Builder builder = DynamicQuery.builder();
        builder.select().append(changingCollection);

        assertEquals("SELECT id", builder.build());
    }

    @Test
    public void testSelectAppendCollectionRejectsSnapshotThatBecomesEmpty() {
        Collection<String> disappearingCollection = new AbstractCollection<String>() {
            @Override
            public java.util.Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.select().append(disappearingCollection));
        assertEquals("", builder.build());
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
    public void testSelectAppendMapValidatesAndRendersOneSnapshot() {
        Map<String, String> changingMap = new AbstractMap<String, String>() {
            private int entrySetCalls;

            @Override
            public Set<Entry<String, String>> entrySet() {
                String alias = entrySetCalls++ == 0 ? "user_id" : "   ";
                return Collections.singleton(new SimpleImmutableEntry<>("id", alias));
            }

            @Override
            public int size() {
                return 1;
            }
        };

        Builder builder = DynamicQuery.builder();
        builder.select().append(changingMap);

        assertEquals("SELECT id AS user_id", builder.build());
    }

    @Test
    public void testSelectAppendMapRejectsSnapshotThatBecomesEmpty() {
        Map<String, String> disappearingMap = new AbstractMap<String, String>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.select().append(disappearingMap));
        assertEquals("", builder.build());
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
    public void testFromAppendCollection() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append(Arrays.asList("users", "departments"));
        String sql = builder.build();
        assertEquals("SELECT * FROM users, departments", sql);
    }

    @Test
    public void testFromAppendEmptyCollectionDoesNotCreateSkeleton() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("id");
        builder.from().append(Arrays.asList()).append("users");
        String sql = builder.build();
        assertEquals("SELECT id FROM users", sql);
    }

    @Test
    public void testFromAppendCollectionBlankElementThrows() {
        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.from().append(Arrays.asList("users", "   ")));
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
    public void testFromJoinWithoutOn() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").join("orders o USING (user_id)");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u JOIN orders o USING (user_id)", sql);
    }

    @Test
    public void testFromJoinWithoutOn_Validation() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().join("orders o"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().join("   "));
    }

    @Test
    public void testFromTypedJoinsWithoutOn() {
        Builder inner = DynamicQuery.builder();
        inner.select().append("*");
        inner.from().append("users", "u").innerJoin("orders o USING (user_id)");
        assertEquals("SELECT * FROM users u INNER JOIN orders o USING (user_id)", inner.build());

        Builder left = DynamicQuery.builder();
        left.select().append("*");
        left.from().append("users", "u").leftJoin("orders o USING (user_id)");
        assertEquals("SELECT * FROM users u LEFT JOIN orders o USING (user_id)", left.build());

        Builder right = DynamicQuery.builder();
        right.select().append("*");
        right.from().append("orders", "o").rightJoin("users u USING (user_id)");
        assertEquals("SELECT * FROM orders o RIGHT JOIN users u USING (user_id)", right.build());

        Builder full = DynamicQuery.builder();
        full.select().append("*");
        full.from().append("employees", "e").fullJoin("departments d USING (dept_id)");
        assertEquals("SELECT * FROM employees e FULL JOIN departments d USING (dept_id)", full.build());
    }

    @Test
    public void testFromTypedJoinsWithoutOn_Validation() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").crossJoin("colors c");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u CROSS JOIN colors c", sql);
    }

    @Test
    public void testFromCrossJoin_Validation() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().crossJoin("colors c"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().crossJoin("   "));
    }

    @Test
    public void testFromNaturalJoin() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users", "u").naturalJoin("user_profiles");
        String sql = builder.build();
        assertEquals("SELECT * FROM users u NATURAL JOIN user_profiles", sql);
    }

    @Test
    public void testFromNaturalJoin_Validation() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        assertThrows(IllegalStateException.class, () -> builder.from().naturalJoin("user_profiles"));
        builder.from().append("users");
        assertThrows(IllegalArgumentException.class, () -> builder.from().naturalJoin("   "));
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
        builder.where().append("id IN (").appendPlaceholders(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ? )", sql);
    }

    @Test
    public void testWhereRepeatQuestionMarkWithPrefixPostfix() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().append("status IN ").appendPlaceholders(2, "(", ")");
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
    public void testHavingAppendPlaceholders() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) IN (").appendPlaceholders(3).append(")");
        String sql = builder.build();
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?, ? )", sql);
    }

    @Test
    public void testHavingAppendPlaceholdersWithPrefixPostfix() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().append("COUNT(*) IN ").appendPlaceholders(2, "(", ")");
        String sql = builder.build();
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?)", sql);
    }

    @Test
    public void testHavingAppendPlaceholdersZeroEmitsNothing() {
        Builder builder = DynamicQuery.builder();
        builder.having().append("x").appendPlaceholders(0, "(", ")");
        String sql = builder.build();
        assertEquals("HAVING x", sql);
    }

    @Test
    public void testHavingAppendPlaceholdersNegativeThrows() {
        Builder builder = DynamicQuery.builder();
        DynamicQuery.HavingClause having = builder.having().append("COUNT(*) IN ");
        assertThrows(IllegalArgumentException.class, () -> having.appendPlaceholders(-1));
        assertThrows(IllegalArgumentException.class, () -> having.appendPlaceholders(-1, "(", ")"));
    }

    @Test
    public void testHavingAppendPlaceholdersNullPrefixOrPostfixThrows() {
        Builder builder = DynamicQuery.builder();
        DynamicQuery.HavingClause having = builder.having().append("COUNT(*) IN ");
        assertThrows(IllegalArgumentException.class, () -> having.appendPlaceholders(3, null, ")"));
        assertThrows(IllegalArgumentException.class, () -> having.appendPlaceholders(3, "(", null));
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
    public void testAppendRawClause() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append("LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testAppendRawClauseSpacingIsIdempotent() {
        // A leading space in the argument does not produce a doubled separating space.
        Builder builder = DynamicQuery.builder();
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
    public void testLimitWithZeroOffsetOmitsOffset() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10, 0);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
        assertFalse(sql.contains("OFFSET"));
    }

    @Test
    public void testLimitWithZeroOffsetReservesPlainOffsetSlot() {
        // limit(count, 0) renders like limit(count) but still occupies the plain-offset slot,
        // exactly like offset(0) does, so a later offset(...) is a duplicate.
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10, 0);
        assertThrows(IllegalStateException.class, () -> builder.offset(20));
        assertEquals("SELECT * FROM users LIMIT 10", builder.build());

        // limit(count) alone leaves the offset slot open.
        Builder open = DynamicQuery.builder();
        open.select().append("*");
        open.from().append("users");
        open.limit(10).offset(20);
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", open.build());
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
    public void testOffset() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.limit(10).offset(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffsetNoRowsKeyword() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offset(20);
        String sql = builder.build();
        assertEquals("SELECT * FROM users OFFSET 20", sql);
    }

    @Test
    public void testOffsetNegativeThrows() {
        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.offset(-1));
    }

    @Test
    public void testPlainOffsetZeroIsOmitted() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offset(0);
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testOffsetRowsZeroIsRendered() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.offsetRows(0).fetchNextRows(10);

        assertEquals("SELECT * FROM users OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", builder.build());
    }

    @Test
    public void testTypedPaginationUsesGrammarOrderRegardlessOfCallOrder() {
        Builder limitBuilder = DynamicQuery.builder();
        limitBuilder.select().append("*");
        limitBuilder.from().append("users");
        limitBuilder.offset(20).limit(10);
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", limitBuilder.build());

        Builder fetchBuilder = DynamicQuery.builder();
        fetchBuilder.select().append("*");
        fetchBuilder.from().append("users");
        fetchBuilder.fetchNextRows(10).offsetRows(20);
        assertEquals("SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", fetchBuilder.build());
    }

    @Test
    public void testTypedPaginationRejectsDuplicateAndMixedClauses() {
        Builder duplicateLimit = DynamicQuery.builder();
        duplicateLimit.limit(10);
        assertThrows(IllegalStateException.class, () -> duplicateLimit.limit(20));

        Builder duplicateOffset = DynamicQuery.builder();
        duplicateOffset.offset(10);
        assertThrows(IllegalStateException.class, () -> duplicateOffset.offset(20));

        Builder duplicateFetch = DynamicQuery.builder();
        duplicateFetch.fetchFirstRows(10);
        assertThrows(IllegalStateException.class, () -> duplicateFetch.fetchNextRows(20));

        Builder limitThenFetch = DynamicQuery.builder();
        limitThenFetch.limit(10);
        assertThrows(IllegalStateException.class, () -> limitThenFetch.offsetRows(20));

        Builder fetchThenLimit = DynamicQuery.builder();
        fetchThenLimit.offsetRows(20);
        assertThrows(IllegalStateException.class, () -> fetchThenLimit.offset(10));
    }

    @Test
    public void testPaginationStatePrecedesInvalidArgumentsWithoutChangingValues() {
        // Invalid arguments must not mask a duplicate/incompatible clause, or consume an unset slot.
        final Builder limit = DynamicQuery.builder().limit(5, 2);
        assertThrows(IllegalStateException.class, () -> limit.limit(-1));
        assertThrows(IllegalStateException.class, () -> limit.limit(-1, -1));
        assertThrows(IllegalStateException.class, () -> limit.offset(-1));
        assertThrows(IllegalStateException.class, () -> limit.offsetRows(-1));
        assertThrows(IllegalStateException.class, () -> limit.fetchNextRows(-1));
        assertThrows(IllegalStateException.class, () -> limit.fetchFirstRows(-1));
        assertEquals("LIMIT 5 OFFSET 2", limit.build());

        final Builder fetch = DynamicQuery.builder().offsetRows(2).fetchFirstRows(5);
        assertThrows(IllegalStateException.class, () -> fetch.limit(-1));
        assertThrows(IllegalStateException.class, () -> fetch.limit(-1, -1));
        assertThrows(IllegalStateException.class, () -> fetch.offset(-1));
        assertThrows(IllegalStateException.class, () -> fetch.offsetRows(-1));
        assertThrows(IllegalStateException.class, () -> fetch.fetchNextRows(-1));
        assertThrows(IllegalStateException.class, () -> fetch.fetchFirstRows(-1));
        assertEquals("OFFSET 2 ROWS FETCH FIRST 5 ROWS ONLY", fetch.build());

        final Builder untouched = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> untouched.limit(5, -1));
        assertThrows(IllegalArgumentException.class, () -> untouched.fetchNextRows(-1));
        assertEquals("LIMIT 3", untouched.limit(3).build());
    }

    @Test
    public void testTypedPaginationPrecedesRawTailRegardlessOfCallOrder() {
        Builder builder = DynamicQuery.builder();
        builder.append("FOR UPDATE");
        builder.limit(10);
        builder.select().append("*");
        builder.from().append("users");

        assertEquals("SELECT * FROM users LIMIT 10 FOR UPDATE", builder.build());
    }

    @Test
    public void testAppendRawFetchClause() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.append("FETCH FIRST 10 ROWS ONLY");
        String sql = builder.build();
        assertEquals("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testAppendBlankThrows() {
        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.append("   "));
    }

    @Test
    public void testAppendIfTrueAppends() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(true, "LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testAppendIfFalseSkips() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(false, "LIMIT 10 OFFSET 20");
        String sql = builder.build();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testAppendIfReturnsSameBuilderForChaining() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIf(true, "FOR UPDATE").appendIf(false, "LIMIT 5");
        String sql = builder.build();
        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    }

    @Test
    public void testAppendIfTrueBlankThrows() {
        Builder builder = DynamicQuery.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, null));
    }

    @Test
    public void testAppendIfFalseBlankDoesNotThrow() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(true, "LIMIT 10", "LIMIT 100");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    public void testAppendIfOrElseFalseAppendsSecond() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(false, "LIMIT 10", "LIMIT 100");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 100", sql);
    }

    @Test
    public void testAppendIfOrElseReturnsSameBuilderForChaining() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.appendIfOrElse(false, "LIMIT 10", "LIMIT 100").appendIf(true, "FOR UPDATE");
        String sql = builder.build();
        assertEquals("SELECT * FROM users LIMIT 100 FOR UPDATE", sql);
    }

    @Test
    public void testAppendIfOrElseBlankThrowsRegardlessOfCondition() {
        Builder builder = DynamicQuery.builder();
        // Both fragments are validated up front, matching the clause-level appendIfOrElse contract.
        assertThrows(IllegalArgumentException.class, () -> builder.appendIfOrElse(true, "LIMIT 10", "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.appendIfOrElse(false, null, "LIMIT 100"));
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
    public void testSetOperationsOnlyBuild() {
        assertEquals("UNION SELECT 1", DynamicQuery.builder().union("SELECT 1").build());
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
            builder.where().append("id IN (").appendPlaceholders(-1).append(")");
            builder.build();
        });
    }

    @Test
    public void testStringInputsRejectNullInsteadOfRenderingLiteralNull() {
        Builder builder = DynamicQuery.builder();

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

    // Covers clause builders when the conditional branch is the first text appended.
    @Test
    public void testConditionalClauseBuilders_EmptyInitialState() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().and("status = 'active'").or("role = 'admin'");

        String sql = builder.build();

        assertEquals("SELECT * FROM users WHERE status = 'active' OR role = 'admin'", sql);
    }

    @Test
    public void testWhereClause_appendIf_FalseOnEmpty() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        builder.where().appendIf(false, "status = 'active'");

        assertEquals("SELECT * FROM users", builder.build());
    }

    @Test
    public void testGroupByClause_appendCollectionThenAppendIf() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append(Arrays.asList("year", "quarter")).appendIf(true, "region");

        String sql = builder.build();

        assertTrue(sql.contains("GROUP BY year, quarter, region"));
    }

    @Test
    public void testHavingClause_andOrFromEmpty() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("sales");
        builder.groupBy().append("region");
        builder.having().and("COUNT(*) > 1").or("SUM(amount) > 0");

        String sql = builder.build();

        assertTrue(sql.contains("HAVING COUNT(*) > 1 OR SUM(amount) > 0"));
    }

    @Test
    public void testOrderByClause_appendCollectionThenAppendIf() {
        Builder builder = DynamicQuery.builder();
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
        final Builder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.select().append("id", ""));
        assertThrows(IllegalArgumentException.class, () -> builder.select().append("id", "   "));
    }

    @Test
    public void selectClause_rejectsBlankConditionalAndCollectionFragments() {
        final Builder builder = DynamicQuery.builder();
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
        final Builder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.from().append("users", ""));
        assertThrows(IllegalArgumentException.class, () -> builder.from().append("users", "   "));

        builder.from().append("users");

        assertThrows(IllegalArgumentException.class, () -> builder.from().join("   ", "users.id = orders.user_id"));
        assertThrows(IllegalArgumentException.class, () -> builder.from().join("orders", "   "));
    }

    @Test
    public void builder_rejectsBlankSetOperationAndLimitFragments() {
        final Builder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.union("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.unionAll("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.intersect("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.except("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.minus("   "));
    }

    @Test
    public void whereClause_rejectsBlankConditions() {
        final Builder builder = DynamicQuery.builder();

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
        final Builder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().append(Arrays.asList("year", "   ")));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIfOrElse(true, "   ", "year"));
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy().appendIfOrElse(false, "year", "   "));
    }

    @Test
    public void havingClause_rejectsBlankConditions() {
        final Builder builder = DynamicQuery.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.having().append("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().and("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().or("   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIf(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIfOrElse(true, "   ", "COUNT(*) > 0"));
        assertThrows(IllegalArgumentException.class, () -> builder.having().appendIfOrElse(false, "COUNT(*) > 0", "   "));
    }

    @Test
    public void orderByClause_rejectsBlankColumns() {
        final Builder builder = DynamicQuery.builder();

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
        Builder b = DynamicQuery.builder();
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
        Builder b = DynamicQuery.builder();
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
        Builder b = DynamicQuery.builder();
        var o1 = b.orderBy();
        var o2 = b.orderBy();
        assertSame(o1, o2);
    }

    @Test
    public void test2ndPass_appendPlaceholders_nZero_emitsNothing() {
        // appendPlaceholders(0) should write nothing to the buffer.
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN (").appendPlaceholders(0); // adds nothing after "("
        // Then we'd usually close with ")" via raw append
        b.where().append(")");
        String sql = b.build();
        assertTrue(sql.contains("id IN ( )"), "0 placeholders should add nothing between '(' and ')' but got: " + sql);
    }

    @Test
    public void test2ndPass_appendPlaceholders_nOne_emitsSingleQuestionMark_noTrailingComma() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN ").appendPlaceholders(1, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE id IN (?)", sql);
    }

    @Test
    public void test2ndPass_appendPlaceholders_nThree_emitsCorrectSeparators() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("id IN ").appendPlaceholders(3, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE id IN (?, ?, ?)", sql);
    }

    @Test
    public void test2ndPass_appendPlaceholdersWithPrefixPostfix_nZero_emitsNeitherPrefixNorPostfix() {
        // Documented: "If placeholderCount is 0, neither prefix nor postfix is appended."
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.where().append("x").appendPlaceholders(0, "(", ")");
        String sql = b.build();
        assertEquals("SELECT * FROM t WHERE x", sql);
    }

    @Test
    public void test2ndPass_buildTwice_secondCallThrowsISE() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        b.build();
        assertThrows(IllegalStateException.class, b::build);
    }

    @Test
    public void testBuild_RawOnlyQueryHasNoSyntheticLeadingSpace() {
        Builder builder = DynamicQuery.builder();
        builder.append("FOR UPDATE");

        assertEquals("FOR UPDATE", builder.build());
    }

    @Test
    public void testClauseBuildersRejectMutationAfterBuild() {
        Builder builder = DynamicQuery.builder();
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
    public void test2ndPass_appendPlaceholdersNegative_throwsIAE() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        var w = b.where().append("x");
        assertThrows(IllegalArgumentException.class, () -> w.appendPlaceholders(-1));
        assertThrows(IllegalArgumentException.class, () -> w.appendPlaceholders(-1, "(", ")"));
    }

    @Test
    public void test2ndPass_appendPlaceholdersNullPrefixOrPostfix_throws() {
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        b.from().append("t");
        var w = b.where().append("x");
        assertThrows(IllegalArgumentException.class, () -> w.appendPlaceholders(3, null, ")"));
        assertThrows(IllegalArgumentException.class, () -> w.appendPlaceholders(3, "(", null));
    }

    @Test
    public void test2ndPass_joinBeforeFromAppend_throwsIllegalState() {
        // requireFromInitialized: join methods must be preceded by from().append(...)
        Builder b = DynamicQuery.builder();
        b.select().append("*");
        var f = b.from();
        assertThrows(IllegalStateException.class, () -> f.leftJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.innerJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.rightJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.fullJoin("orders o", "u.id = o.uid"));
        assertThrows(IllegalStateException.class, () -> f.join("orders o", "u.id = o.uid"));
    }

    @Test
    public void testEveryJoinChecksClauseStateBeforeNullArguments() {
        final Builder builder = DynamicQuery.builder();
        final DynamicQuery.FromClause from = builder.from();
        assertThrows(IllegalStateException.class, () -> from.join(null, null));
        assertThrows(IllegalStateException.class, () -> from.innerJoin(null, null));
        assertThrows(IllegalStateException.class, () -> from.leftJoin(null, null));
        assertThrows(IllegalStateException.class, () -> from.rightJoin(null, null));
        assertThrows(IllegalStateException.class, () -> from.fullJoin(null, null));
        assertThrows(IllegalStateException.class, () -> from.join(null));
        assertThrows(IllegalStateException.class, () -> from.innerJoin(null));
        assertThrows(IllegalStateException.class, () -> from.leftJoin(null));
        assertThrows(IllegalStateException.class, () -> from.rightJoin(null));
        assertThrows(IllegalStateException.class, () -> from.fullJoin(null));
        assertThrows(IllegalStateException.class, () -> from.crossJoin(null));
        assertThrows(IllegalStateException.class, () -> from.naturalJoin(null));

        // Rejections leave the same clause usable once its prerequisite has been supplied.
        from.append("users u");
        assertThrows(IllegalArgumentException.class, () -> from.join("orders o", null));
        from.join("orders o", "u.id = o.user_id");
        assertEquals("FROM users u JOIN orders o ON u.id = o.user_id", builder.build());
        assertThrows(IllegalStateException.class, () -> from.join(null, null));
    }

    @Test
    public void testBuilderMethodsRejectUseAfterBuild() {
        Builder builder = DynamicQuery.builder();
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
        Builder builder = DynamicQuery.builder();
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
    public void testWhereAppendPlaceholdersRequireInitializedClause() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("*");
        builder.from().append("users");
        DynamicQuery.WhereClause where = builder.where();

        // Without a prior append/and/or, placeholders would emit "... FROM users ?, ?, ?".
        assertThrows(IllegalStateException.class, () -> where.appendPlaceholders(3));
        assertThrows(IllegalStateException.class, () -> where.appendPlaceholders(3, "(", ")"));

        // Clause state is checked before argument validation.
        assertThrows(IllegalStateException.class, () -> where.appendPlaceholders(-1));
        assertThrows(IllegalStateException.class, () -> where.appendPlaceholders(3, null, ")"));

        // Once initialized, placeholders work as before.
        where.append("id IN ").appendPlaceholders(2, "(", ")");
        assertEquals("SELECT * FROM users WHERE id IN (?, ?)", builder.build());
    }

    @Test
    public void testHavingAppendPlaceholdersRequireInitializedClause() {
        Builder builder = DynamicQuery.builder();
        builder.select().append("region");
        builder.from().append("sales");
        builder.groupBy().append("region");
        DynamicQuery.HavingClause having = builder.having();

        // Without a prior append/and/or, placeholders would emit "... GROUP BY region ?, ?".
        assertThrows(IllegalStateException.class, () -> having.appendPlaceholders(2));
        assertThrows(IllegalStateException.class, () -> having.appendPlaceholders(2, "(", ")"));

        // Clause state is checked before argument validation.
        assertThrows(IllegalStateException.class, () -> having.appendPlaceholders(-1));
        assertThrows(IllegalStateException.class, () -> having.appendPlaceholders(2, "(", null));

        // Once initialized, placeholders work as before.
        having.append("COUNT(*) IN ").appendPlaceholders(2, "(", ")");
        assertEquals("SELECT region FROM sales GROUP BY region HAVING COUNT(*) IN (?, ?)", builder.build());
    }

    @Test
    public void testSetOperationRendersBeforeFinalOrderByAndPaginationRegardlessOfCallOrder() {
        // Regression: set operations used to share the trailing-fragment buffer, so orderBy()
        // rendered first and produced invalid "ORDER BY ... UNION ..." SQL.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("id");
        b1.from().append("t1");
        b1.union("SELECT id FROM t2");
        b1.orderBy().append("id");
        assertEquals("SELECT id FROM t1 UNION SELECT id FROM t2 ORDER BY id", b1.build());

        // Raw trailing clauses still follow the combined query.
        Builder b2 = DynamicQuery.builder();
        b2.select().append("id");
        b2.from().append("t1");
        b2.union("SELECT id FROM t2");
        b2.append("ORDER BY id");
        assertEquals("SELECT id FROM t1 UNION SELECT id FROM t2 ORDER BY id", b2.build());

        // Typed pagination is also rendered after the set operation even when invoked first.
        Builder b3 = DynamicQuery.builder();
        b3.select().append("id");
        b3.from().append("t1");
        b3.limit(10);
        b3.unionAll("SELECT id FROM t2");
        assertEquals("SELECT id FROM t1 UNION ALL SELECT id FROM t2 LIMIT 10", b3.build());
    }

    @Test
    public void testClauseBuilderCloseIsIdempotent() {
        DynamicQuery.OrderByClause clause = new DynamicQuery.OrderByClause(Objectory.createStringBuilder());
        clause.append("id ASC");

        clause.close();
        clause.close(); // second close must be a no-op (no double recycle into the buffer pool)

        assertThrows(IllegalStateException.class, () -> clause.append("name ASC"));
    }

    @Test
    public void testTrailingLineCommentInFragmentDoesNotHideFollowingClauses() {
        // Regression: a fragment ending in a "--" line comment swallowed every clause rendered after it
        // (the WHERE filter, AND predicates, ORDER BY, LIMIT), silently changing the query.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("users u -- main table");
        b1.where().append("u.deleted = 0");
        assertEquals("SELECT * FROM users u -- main table\n WHERE u.deleted = 0", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().append("users u").leftJoin("orders o", "u.id = o.user_id -- fk");
        b2.where().append("tenant_id = ? -- tenant filter").and("owner_id = ?");
        b2.orderBy().append("name -- primary sort");
        b2.limit(10);
        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id -- fk\n WHERE tenant_id = ? -- tenant filter\n AND owner_id = ?"
                + " ORDER BY name -- primary sort\n LIMIT 10", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append(Arrays.asList("id -- pk", "name")).append("email # mysql comment", "mail");
        b3.from().append("users");
        b3.union("SELECT id, name, email FROM archived -- archived");
        b3.orderBy().append("id");
        assertEquals("SELECT id -- pk\n, name, email # mysql comment\n AS mail FROM users UNION SELECT id, name, email FROM archived -- archived\n ORDER BY id",
                b3.build());

        // Fragments without a trailing line comment (including "--" inside a literal or block comment) are unchanged.
        Builder b4 = DynamicQuery.builder();
        b4.select().append("*");
        b4.from().append("t");
        b4.where().append("a = '--x'").and("b = 1 /* -- */");
        b4.append("FOR UPDATE");
        assertEquals("SELECT * FROM t WHERE a = '--x' AND b = 1 /* -- */ FOR UPDATE", b4.build());
    }

    @Test
    public void testTrailingLineCommentInPlaceholderPrefixAndPostfixIsTerminated() {
        // Covers the appendPlaceholders prefix/postfix path of the trailing line-comment fix (WHERE and HAVING).
        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("t");
        b1.where().append("id IN").appendPlaceholders(2, "( -- ids", ") -- end");
        b1.orderBy().append("id");
        assertEquals("SELECT * FROM t WHERE id IN( -- ids\n?, ?) -- end\n ORDER BY id", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("g");
        b2.from().append("t");
        b2.groupBy().append("g");
        b2.having().append("COUNT(*) IN").appendPlaceholders(2, "( -- n", ") -- end");
        b2.orderBy().append("g");
        assertEquals("SELECT g FROM t GROUP BY g HAVING COUNT(*) IN( -- n\n?, ?) -- end\n ORDER BY g", b2.build());
    }

    @Test
    public void testTrailingLineCommentInFromAndSelectAliasFragmentsIsTerminated() {
        // Covers the table/alias, table-list, column-alias map and alias-side paths of the trailing line-comment fix.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("users -- t", "u -- alias");
        b1.where().append("x = 1");
        assertEquals("SELECT * FROM users -- t\n u -- alias\n WHERE x = 1", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().append(Arrays.asList("a -- x", "b"));
        b2.where().append("x = 1");
        assertEquals("SELECT * FROM a -- x\n, b WHERE x = 1", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append(Collections.singletonMap("a -- k", "x -- v"));
        b3.from().append("t");
        assertEquals("SELECT a -- k\n AS x -- v\n FROM t", b3.build());

        Builder b4 = DynamicQuery.builder();
        b4.select().append("a", "b -- alias");
        b4.from().append("t");
        assertEquals("SELECT a AS b -- alias\n FROM t", b4.build());
    }

    @Test
    public void testTrailingLineCommentInJoinFragmentsIsTerminated() {
        // Covers the single-argument join methods (including CROSS and NATURAL JOIN) and both sides of the two-argument joins.
        final String[] expectedJoins = { "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "NATURAL JOIN" };

        for (int i = 0; i < expectedJoins.length; i++) {
            Builder b = DynamicQuery.builder();
            b.select().append("*");
            DynamicQuery.FromClause from = b.from().append("t");

            switch (i) {
                case 0 -> from.join("u -- j");
                case 1 -> from.innerJoin("u -- j");
                case 2 -> from.leftJoin("u -- j");
                case 3 -> from.rightJoin("u -- j");
                case 4 -> from.fullJoin("u -- j");
                case 5 -> from.crossJoin("u -- j");
                default -> from.naturalJoin("u -- j");
            }

            b.where().append("x = 1");
            assertEquals("SELECT * FROM t " + expectedJoins[i] + " u -- j\n WHERE x = 1", b.build());
        }

        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("t").join("u -- j", "t.id = u.id");
        b1.where().append("x = 1");
        assertEquals("SELECT * FROM t JOIN u -- j\n ON t.id = u.id WHERE x = 1", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().append("t").innerJoin("u", "t.id = u.id -- on").rightJoin("v", "u.id = v.id -- on").fullJoin("w", "v.id = w.id -- on");
        b2.where().append("x = 1");
        assertEquals("SELECT * FROM t INNER JOIN u ON t.id = u.id -- on\n RIGHT JOIN v ON u.id = v.id -- on\n FULL JOIN w ON v.id = w.id -- on\n WHERE x = 1",
                b2.build());
    }

    @Test
    public void testTrailingLineCommentInGroupByHavingAndSetOperationFragmentsIsTerminated() {
        // Covers GROUP BY items, HAVING/WHERE and()/or() fragments and every set operation of the trailing line-comment fix.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("g");
        b1.from().append("t");
        b1.groupBy().append("g -- grp").append("h");
        b1.having().append("COUNT(*) > 1");
        assertEquals("SELECT g FROM t GROUP BY g -- grp\n, h HAVING COUNT(*) > 1", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("g");
        b2.from().append("t");
        b2.groupBy().append(Arrays.asList("g -- grp", "h"));
        b2.orderBy().append("g");
        assertEquals("SELECT g FROM t GROUP BY g -- grp\n, h ORDER BY g", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append("g");
        b3.from().append("t");
        b3.groupBy().append("g");
        b3.having().append("COUNT(*) > 1 -- c1").and("SUM(x) > 2 -- c2").or("MAX(x) > 3");
        b3.orderBy().append("g");
        assertEquals("SELECT g FROM t GROUP BY g HAVING COUNT(*) > 1 -- c1\n AND SUM(x) > 2 -- c2\n OR MAX(x) > 3 ORDER BY g", b3.build());

        Builder b4 = DynamicQuery.builder();
        b4.select().append("*");
        b4.from().append("t");
        b4.where().append("a = 1").and("b = 2 -- c2").or("c = 3 -- c3");
        b4.orderBy().append("a");
        assertEquals("SELECT * FROM t WHERE a = 1 AND b = 2 -- c2\n OR c = 3 -- c3\n ORDER BY a", b4.build());

        final String[] setOps = { "INTERSECT", "EXCEPT", "MINUS" };

        for (int i = 0; i < setOps.length; i++) {
            Builder b = DynamicQuery.builder();
            b.select().append("id");
            b.from().append("t1");

            switch (i) {
                case 0 -> b.intersect("SELECT id FROM t2 -- s");
                case 1 -> b.except("SELECT id FROM t2 -- s");
                default -> b.minus("SELECT id FROM t2 -- s");
            }

            b.orderBy().append("id");
            assertEquals("SELECT id FROM t1 " + setOps[i] + " SELECT id FROM t2 -- s\n ORDER BY id", b.build());
        }

        Builder b5 = DynamicQuery.builder();
        b5.select().append("id");
        b5.from().append("t1");
        b5.unionAll("SELECT id FROM t2 -- s");
        b5.limit(3);
        assertEquals("SELECT id FROM t1 UNION ALL SELECT id FROM t2 -- s\n LIMIT 3", b5.build());
    }

    @Test
    public void testTrailingLineCommentInAppendIfVariantsIsTerminated() {
        // Covers every appendIf/appendIfOrElse variant (both branches) of the trailing line-comment fix.
        Builder b1 = DynamicQuery.builder();
        b1.select().appendIf(true, "a -- c").appendIfOrElse(true, "b -- c", "z").appendIfOrElse(false, "z", "c -- c").append("d");
        b1.from().append("t");
        assertEquals("SELECT a -- c\n, b -- c\n, c -- c\n, d FROM t", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().appendIf(true, "t -- c").appendIfOrElse(false, "z", "u -- c");
        b2.where().append("x = 1");
        assertEquals("SELECT * FROM t -- c\n, u -- c\n WHERE x = 1", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append("*");
        b3.from().append("t");
        b3.where().appendIfOrElse(true, "a = 1 -- c", "z").appendIf(true, "AND b = 2 -- c").and("c = 3");
        assertEquals("SELECT * FROM t WHERE a = 1 -- c\n AND b = 2 -- c\n AND c = 3", b3.build());

        Builder b4 = DynamicQuery.builder();
        b4.select().append("g");
        b4.from().append("t");
        b4.groupBy().appendIf(true, "g -- c").appendIfOrElse(false, "z", "h -- c");
        b4.having().appendIfOrElse(false, "z", "COUNT(*) > 1 -- c").appendIf(true, "AND SUM(x) > 2 -- c");
        b4.orderBy().appendIf(true, "g -- c").appendIfOrElse(false, "z", "h -- c");
        b4.limit(2);
        assertEquals("SELECT g FROM t GROUP BY g -- c\n, h -- c\n HAVING COUNT(*) > 1 -- c\n AND SUM(x) > 2 -- c\n ORDER BY g -- c\n, h -- c\n LIMIT 2",
                b4.build());

        Builder b5 = DynamicQuery.builder();
        b5.select().append("*");
        b5.from().append("t");
        b5.appendIf(true, "FOR UPDATE -- lock").appendIfOrElse(false, "z", "NOWAIT -- now").append("SKIP LOCKED");
        assertEquals("SELECT * FROM t FOR UPDATE -- lock\n NOWAIT -- now\n SKIP LOCKED", b5.build());
    }

    @Test
    public void testTrailingLineCommentInRawTailsAndLineEndings() {
        // Covers raw tails, raw tail plus typed pagination, lone CR / LF / CRLF endings, and a query whose last fragment is a comment.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("t");
        b1.append("FOR UPDATE -- lock").append("NOWAIT");
        assertEquals("SELECT * FROM t FOR UPDATE -- lock\n NOWAIT", b1.build());

        // Typed pagination renders before the raw tail regardless of call order; the trailing comment still ends with a newline.
        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().append("t");
        b2.append("-- only");
        b2.limit(5);
        assertEquals("SELECT * FROM t LIMIT 5 -- only\n", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append("*");
        b3.from().append("t");
        b3.orderBy().append("a -- c");
        b3.offsetRows(5).fetchNextRows(3);
        assertEquals("SELECT * FROM t ORDER BY a -- c\n OFFSET 5 ROWS FETCH NEXT 3 ROWS ONLY", b3.build());

        // A lone '\r' gets a '\n' (some lexers end a line comment only at '\n'); already-terminated comments are unchanged.
        Builder b4 = DynamicQuery.builder();
        b4.select().append("a -- c\r").append("b");
        b4.from().append("t -- c\r");
        b4.where().append("x = 1");
        assertEquals("SELECT a -- c\r\n, b FROM t -- c\r\n WHERE x = 1", b4.build());

        Builder b5 = DynamicQuery.builder();
        b5.select().append("*");
        b5.from().append("t -- c\n");
        b5.where().append("x = 1");
        assertEquals("SELECT * FROM t -- c\n WHERE x = 1", b5.build());

        Builder b6 = DynamicQuery.builder();
        b6.select().append("*");
        b6.from().append("t -- c\r\n");
        b6.where().append("x = 1");
        assertEquals("SELECT * FROM t -- c\r\n WHERE x = 1", b6.build());

        // A query whose last fragment ends in a line comment ends with the terminating newline; a block comment does not.
        Builder b7 = DynamicQuery.builder();
        b7.select().append("*");
        b7.from().append("t");
        b7.where().append("x = 1 -- c");
        assertEquals("SELECT * FROM t WHERE x = 1 -- c\n", b7.build());

        Builder b8 = DynamicQuery.builder();
        b8.select().append("*");
        b8.from().append("t");
        b8.where().append("x = 1 /* c */");
        assertEquals("SELECT * FROM t WHERE x = 1 /* c */", b8.build());
    }

    @Test
    public void testFragmentTrailingCommentCheckFailsClosedAcrossDialectReadings() {
        // Regression: a fragment carries no dialect, but its trailing-comment check used one fixed reading: a PostgreSQL
        // array literal "['a]b']" read as a bracket identifier hid the trailing "--" comment, and a fragment starting with
        // "#word" was read as a SQL Server temp table although MySQL reads it as a comment, so the next clause was swallowed.
        Builder b1 = DynamicQuery.builder();
        b1.select().append("*");
        b1.from().append("t");
        b1.where().append("tags = ARRAY['a]b'] -- c").and("tenant_id = ?");
        assertEquals("SELECT * FROM t WHERE tags = ARRAY['a]b'] -- c\n AND tenant_id = ?", b1.build());

        Builder b2 = DynamicQuery.builder();
        b2.select().append("*");
        b2.from().append("t");
        b2.where().append("x = 1").append("#tenant filter").and("y = 2");
        assertEquals("SELECT * FROM t WHERE x = 1 #tenant filter\n AND y = 2", b2.build());

        Builder b3 = DynamicQuery.builder();
        b3.select().append("*");
        b3.from().append("t");
        b3.orderBy().append("#sort");
        b3.limit(3);
        assertEquals("SELECT * FROM t ORDER BY #sort\n LIMIT 3", b3.build());

        assertTrue(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("doc['a]'] = 1 -- c"));
        assertTrue(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("#tenant filter"));
        assertTrue(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("x = 1 -- c"));

        // No comment under any reading: MyBatis markers, bracket identifiers, and hash operators inside quotes stay unchanged.
        assertFalse(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("x = #{x}"));
        assertFalse(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("[a] = 1"));
        assertFalse(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("a = '#b'"));
        assertFalse(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading("x = 1 -- c\n"));

        Builder b4 = DynamicQuery.builder();
        b4.select().append("[a]");
        b4.from().append("[t]");
        b4.where().append("x = #{x}").and("y = 2");
        assertEquals("SELECT [a] FROM [t] WHERE x = #{x} AND y = 2", b4.build());
    }

    @Test
    public void testFragmentTrailingCommentCheckIncludesSqlServerTempTableReading() {
        // Regression: the fail-closed check had no SQL Server reading. There "#tmp.note" is a temporary-table reference and
        // the "--" after the multi-line literal is a real comment, while the other readings take "#tmp.note ..." for a hash
        // comment, mis-pair the quotes, and missed it: SQL Server then commented out the ORDER BY.
        final String fragment = "x = #tmp.note AND y = 'a\nb' -- c";
        assertTrue(AbstractQueryBuilder.endsInsideLineCommentUnderAnyReading(fragment));

        Builder b = DynamicQuery.builder();
        b.select().append("x");
        b.from().append("t");
        b.where().append(fragment);
        b.orderBy().append("x");
        assertEquals("SELECT x FROM t WHERE x = #tmp.note AND y = 'a\nb' -- c\n ORDER BY x", b.build());
    }
}
