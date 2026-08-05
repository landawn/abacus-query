/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SqlExpression;

/**
 * Executable coverage for every row in {@code sqlbuilder-supported-sql.txt}.
 */
@SuppressWarnings("deprecation")
public class SqlBuilderSupportedSqlTest extends TestBase {

    @Test
    public void testSelectFormsAndModifiers() {
        // 1. Basic SELECT projection.
        assertSp("SELECT id, first_name FROM users", List.of(), Dsl.PSC.select("id", "first_name").from("users").build());

        // 2. Column alias.
        assertSp("SELECT first_name AS \"fname\" FROM users", List.of(), Dsl.PSC.select(Map.of("firstName", "fname")).from("users").build());

        // 3. SELECT DISTINCT.
        assertSp("SELECT DISTINCT status FROM users", List.of(), Dsl.PSC.select("status").distinct().from("users").build());

        // 4. PostgreSQL-style DISTINCT ON.
        assertSp("SELECT DISTINCT ON (department) department FROM employees", List.of(),
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinctOn("department").build()).build());

        // 5. MySQL-style DISTINCTROW.
        assertSp("SELECT DISTINCTROW department FROM employees", List.of(),
                Dsl.PSC.select("department").from("employees").append(Criteria.builder().distinctRow().build()).build());

        // 6. Custom SELECT modifier.
        assertSp("SELECT TOP 10 * FROM users", List.of(), Dsl.PSC.select("*").selectModifier("TOP 10").from("users").build());

        // 7. Multiple-table FROM.
        assertSp("SELECT * FROM users u, orders o", List.of(), Dsl.PSC.select("*").from(List.of("users u", "orders o")).build());

        // 8. Derived table / subquery in FROM.
        assertSp("SELECT * FROM (SELECT id FROM users) u", List.of(), Dsl.PSC.select("*").from("(SELECT id FROM users) u").build());

        // 9. Entity-derived SELECT and table.
        assertSp("SELECT id AS \"id\" FROM account WHERE id = ?", List.of(7),
                Dsl.PSC.selectFrom(Selection.builder(Account.class).includedPropNames(List.of("id")).build()).where(Filters.eq("id", 7)).build());

        // 10. Aggregate COUNT(*) factory.
        assertSp("SELECT count(*) FROM users WHERE active = ?", List.of(true), Dsl.PSC.count("users").where(Filters.eq("active", true)).build());
    }

    @Test
    public void testDataModificationStatements() {
        // 11. INSERT template.
        assertSp("INSERT INTO users (name, status) VALUES (?, ?)", List.of(), Dsl.PSC.insert("name", "status").into("users").build());

        // 12. Value-bearing INSERT from a map.
        assertSp("INSERT INTO users (name) VALUES (?)", List.of("Ada"), Dsl.PSC.insert(Map.of("name", "Ada")).into("users").build());

        // 13. Batch/multi-row INSERT.
        assertSp("INSERT INTO users (name) VALUES (?), (?)", List.of("Ada", "Linus"),
                Dsl.PSC.batchInsert(List.of(Map.of("name", "Ada"), Map.of("name", "Linus"))).into("users").build());

        // 14. INSERT ... SELECT.
        assertSp("INSERT INTO archived_users (id, name) SELECT id, name FROM users", List.of(),
                Dsl.PSC.select("id", "name").into("archived_users").from("users").build());

        // 15. UPDATE template. SET placeholders are intentionally unbound; the WHERE value is bound.
        assertSp("UPDATE users SET name = ?, status = ? WHERE id = ?", List.of(7),
                Dsl.PSC.update("users").set("name", "status").where(Filters.eq("id", 7)).build());

        // 16. Value-bearing UPDATE from a map.
        assertSp("UPDATE users SET status = ? WHERE id = ?", List.of("INACTIVE", 7),
                Dsl.PSC.update("users").set(Map.of("status", "INACTIVE")).where(Filters.eq("id", 7)).build());

        // 17. Expression assignment in UPDATE.
        assertSp("UPDATE users SET login_count = login_count + 1 WHERE id = ?", List.of(7),
                Dsl.PSC.update("users").set("login_count = login_count + 1").where(Filters.eq("id", 7)).build());

        // 18. DELETE.
        assertSp("DELETE FROM users WHERE id = ?", List.of(7), Dsl.PSC.deleteFrom("users").where(Filters.eq("id", 7)).build());

        // 19. Standalone condition fragment.
        assertSp("status = ?", List.of("ACTIVE"), Dsl.PSC.renderCondition(Filters.eq("status", "ACTIVE")).build());
    }

    @Test
    public void testPredicates() {
        // 20. Comparisons.
        assertSp("SELECT * FROM users WHERE age >= ?", List.of(18), Dsl.PSC.select("*").from("users").where(Filters.ge("age", 18)).build());

        // 21. BETWEEN / NOT BETWEEN.
        assertSp("SELECT * FROM users WHERE age BETWEEN ? AND ?", List.of(18, 65),
                Dsl.PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build());

        // 22. LIKE / NOT LIKE.
        assertSp("SELECT * FROM users WHERE name LIKE ?", List.of("A%"), Dsl.PSC.select("*").from("users").where(Filters.like("name", "A%")).build());

        // 23. Pattern helper.
        assertSp("SELECT * FROM users WHERE name LIKE ?", List.of("A%"), Dsl.PSC.select("*").from("users").where(Filters.startsWith("name", "A")).build());

        // 24. IS NULL / IS NOT NULL.
        assertSp("SELECT * FROM users WHERE deleted_at IS NULL", List.of(), Dsl.PSC.select("*").from("users").where(Filters.isNull("deletedAt")).build());

        // 25. IS / IS NOT.
        assertSp("SELECT * FROM users WHERE active IS true", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.is("active", SqlExpression.of("TRUE"))).build());

        // 26. Null/empty/zero convenience predicate.
        assertSp("SELECT * FROM users WHERE (name IS NULL) OR (name = ?)", List.of(""),
                Dsl.PSC.select("*").from("users").where(Filters.isNullOrEmpty("name")).build());

        // 27. NaN / infinity predicate.
        assertSp("SELECT * FROM metrics WHERE score IS INFINITE", List.of(), Dsl.PSC.select("*").from("metrics").where(Filters.isInfinite("score")).build());

        // 28. IN / NOT IN value list.
        assertSp("SELECT * FROM users WHERE id IN (?, ?, ?)", List.of(1, 2, 3),
                Dsl.PSC.select("*").from("users").where(Filters.in("id", List.of(1, 2, 3))).build());

        // 29. Row-value / multi-column IN.
        assertSp("SELECT * FROM users WHERE (id, type) IN ((?, ?), (?, ?))", List.of(1, "A", 2, "B"),
                Dsl.PSC.select("*").from("users").where(Filters.in(List.of("id", "type"), List.of(List.of(1, "A"), List.of(2, "B")))).build());

        // 30. Boolean AND / OR composition.
        assertSp("SELECT * FROM users WHERE (status = ?) AND ((age > ?) OR (vip = ?))", List.of("ACTIVE", 21, true),
                Dsl.PSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.or(Filters.gt("age", 21), Filters.eq("vip", true))))
                        .build());

        // 31. Boolean NOT.
        assertSp("SELECT * FROM users WHERE NOT (status = ?)", List.of("ACTIVE"),
                Dsl.PSC.select("*").from("users").where(Filters.not(Filters.eq("status", "ACTIVE"))).build());

        // 32. Raw predicate expression.
        assertSp("SELECT * FROM users WHERE score > average_score", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.expr("score > average_score")).build());
    }

    @Test
    public void testSubqueries() {
        // 33. Scalar subquery comparison.
        assertSp("SELECT * FROM users WHERE id = (SELECT MAX(user_id) FROM orders)", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.eq("id", Filters.subQuery("SELECT MAX(user_id) FROM orders"))).build());

        // 34. Structured IN subquery with parameters.
        assertSp("SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > ?)", List.of(100),
                Dsl.PSC.select("*").from("users").where(Filters.in("id", Filters.subQuery("orders", "user_id", Filters.gt("total", 100)))).build());

        // 35. Row-value IN subquery.
        assertSp("SELECT * FROM users WHERE (id, type) IN (SELECT user_id, type FROM memberships)", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.in(List.of("id", "type"), Filters.subQuery("SELECT user_id, type FROM memberships"))).build());

        // 36. EXISTS / NOT EXISTS.
        assertSp("SELECT * FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)", List.of(),
                Dsl.PSC.select("*").from("users u").where(Filters.exists(Filters.subQuery("SELECT 1 FROM orders o WHERE o.user_id = u.id"))).build());

        // 37. Quantified subquery.
        assertSp("SELECT * FROM employees WHERE salary > ALL (SELECT salary FROM managers)", List.of(),
                Dsl.PSC.select("*")
                        .from("employees")
                        .where(Filters.binary("salary", Operator.GREATER_THAN, Filters.all(Filters.subQuery("SELECT salary FROM managers"))))
                        .build());
    }

    @Test
    public void testJoins() {
        // 38. Generic / INNER JOIN ... ON.
        assertSp("SELECT u.id FROM users u INNER JOIN orders o ON u.id = o.user_id", List.of(),
                Dsl.PSC.select("u.id").from("users u").innerJoin("orders o").on("u.id = o.user_id").build());

        // 39. LEFT JOIN.
        assertSp("SELECT u.id FROM users u LEFT JOIN profiles p ON u.id = p.user_id", List.of(),
                Dsl.PSC.select("u.id").from("users u").leftJoin("profiles p").on("u.id = p.user_id").build());

        // 40. RIGHT JOIN.
        assertSp("SELECT u.id FROM users u RIGHT JOIN profiles p ON u.id = p.user_id", List.of(),
                Dsl.PSC.select("u.id").from("users u").rightJoin("profiles p").on("u.id = p.user_id").build());

        // 41. FULL JOIN.
        assertSp("SELECT u.id FROM users u FULL JOIN profiles p ON u.id = p.user_id", List.of(),
                Dsl.PSC.select("u.id").from("users u").fullJoin("profiles p").on("u.id = p.user_id").build());

        // 42. CROSS JOIN.
        assertSp("SELECT * FROM colors CROSS JOIN sizes", List.of(), Dsl.PSC.select("*").from("colors").crossJoin("sizes").build());

        // 43. NATURAL JOIN.
        assertSp("SELECT * FROM users NATURAL JOIN profiles", List.of(), Dsl.PSC.select("*").from("users").naturalJoin("profiles").build());

        // 44. JOIN ... USING.
        assertSp("SELECT * FROM users JOIN profiles USING (user_id)", List.of(), Dsl.PSC.select("*").from("users").join("profiles").using("userId").build());
    }

    @Test
    public void testGroupingOrderingPaginationAndLocking() {
        // 45. GROUP BY.
        assertSp("SELECT department, COUNT(*) FROM employees GROUP BY department", List.of(),
                Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build());

        // 46. HAVING.
        assertSp("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ?", List.of(5),
                Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.gt("COUNT(*)", 5)).build());

        // 47. ORDER BY with direction.
        assertSp("SELECT * FROM users ORDER BY created_at DESC", List.of(), Dsl.PSC.select("*").from("users").orderBy("createdAt", SortDirection.DESC).build());

        // 48. LIMIT.
        assertSp("SELECT * FROM users LIMIT 10", List.of(), Dsl.PSC.select("*").from("users").limit(10).build());

        // 49. LIMIT ... OFFSET.
        assertSp("SELECT * FROM users LIMIT 10 OFFSET 20", List.of(), Dsl.PSC.select("*").from("users").limit(10, 20).build());

        // 50. Standalone OFFSET.
        assertSp("SELECT * FROM users ORDER BY id OFFSET 20", List.of(), Dsl.PSC.select("*").from("users").orderBy("id").offset(20).build());

        // 51. ANSI FETCH FIRST.
        assertSp("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", List.of(),
                Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build());

        // 52. ANSI OFFSET ... FETCH NEXT.
        assertSp("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", List.of(),
                Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build());

        // 53. FOR UPDATE.
        assertSp("SELECT * FROM users WHERE id = ? FOR UPDATE", List.of(7), Dsl.PSC.select("*").from("users").where(Filters.eq("id", 7)).forUpdate().build());
    }

    @Test
    public void testSetOperations() {
        // 54. UNION.
        assertSp("SELECT id FROM users UNION SELECT id FROM admins", List.of(),
                Dsl.PSC.select("id").from("users").union(Dsl.PSC.select("id").from("admins")).build());

        // 55. UNION ALL.
        assertSp("SELECT id FROM users UNION ALL SELECT id FROM admins", List.of(),
                Dsl.PSC.select("id").from("users").unionAll(Dsl.PSC.select("id").from("admins")).build());

        // 56. INTERSECT.
        assertSp("SELECT id FROM users INTERSECT SELECT id FROM subscribers", List.of(),
                Dsl.PSC.select("id").from("users").intersect(Dsl.PSC.select("id").from("subscribers")).build());

        // 57. EXCEPT.
        assertSp("SELECT id FROM users EXCEPT SELECT id FROM blocked_users", List.of(),
                Dsl.PSC.select("id").from("users").except(Dsl.PSC.select("id").from("blocked_users")).build());

        // 58. Oracle-style MINUS.
        assertSp("SELECT id FROM users MINUS SELECT id FROM blocked_users", List.of(),
                Dsl.PSC.select("id").from("users").minus(Dsl.PSC.select("id").from("blocked_users")).build());
    }

    @Test
    public void testExpressionFamilies() {
        // 59. Arithmetic expressions.
        assertSp("SELECT price + tax FROM orders", List.of(),
                Dsl.PSC.select(SqlExpression.plus(SqlExpression.of("price"), SqlExpression.of("tax"))).from("orders").build());

        // 60. Bitwise and shift expressions.
        assertSp("SELECT flags & mask FROM users", List.of(),
                Dsl.PSC.select(SqlExpression.bitwiseAnd(SqlExpression.of("flags"), SqlExpression.of("mask"))).from("users").build());

        // 61. Aggregate functions.
        assertSp("SELECT SUM(amount) FROM orders", List.of(), Dsl.PSC.select(SqlExpression.sum("amount")).from("orders").build());

        // 62. Numeric functions.
        assertSp("SELECT SQRT(score) FROM metrics", List.of(), Dsl.PSC.select(SqlExpression.sqrt("score")).from("metrics").build());

        // 63. String functions.
        assertSp("SELECT UPPER(name) FROM users", List.of(), Dsl.PSC.select(SqlExpression.upper("name")).from("users").build());
    }

    @Test
    public void testParameterNamingAndCompositionPolicies() {
        // 64. Positional parameters.
        assertSp("SELECT * FROM users WHERE status = ?", List.of("ACTIVE"), Dsl.PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 65. Named parameters.
        assertSp("SELECT * FROM users WHERE status = :status", List.of("ACTIVE"),
                Dsl.NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 66. MyBatis/iBATIS parameters.
        assertSp("SELECT * FROM users WHERE status = #{status}", List.of("ACTIVE"),
                Dsl.MSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 67. Inlined literal SQL.
        assertSp("SELECT * FROM users WHERE status = 'ACTIVE'", List.of(), Dsl.SCSB.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 68. Identifier naming policy.
        assertSp("SELECT FIRST_NAME AS \"firstName\" FROM account", List.of(), Dsl.PAC.select("firstName").from("account").build());

        // 69. Reusable full query shape via Criteria.
        assertSp("SELECT department FROM users WHERE active = ? GROUP BY department ORDER BY department", List.of(true),
                Dsl.PSC.select("department")
                        .from("users")
                        .append(Criteria.builder().where(Filters.eq("active", true)).groupBy("department").orderBy("department").build())
                        .build());

        // 70. Conditional SQL composition.
        final boolean includeStatus = true;
        assertSp("SELECT * FROM users WHERE status = ?", List.of("ACTIVE"),
                Dsl.PSC.select("*").from("users").appendIf(includeStatus, Filters.eq("status", "ACTIVE")).build());

        // 71. Trusted raw trailing fragment escape hatch.
        assertSp("SELECT * FROM users FOR SHARE", List.of(), Dsl.PSC.select("*").from("users").append("FOR SHARE").build());
    }

    private static void assertSp(final String expectedSql, final List<?> expectedParameters, final SP actual) {
        assertEquals(expectedSql, actual.query());
        assertEquals(expectedParameters, actual.parameters());
    }

    @Table(name = "account")
    public static final class Account {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }
}
