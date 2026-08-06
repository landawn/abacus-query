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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;

/**
 * Executable coverage for every row in {@code scripts/sqlbuilder-supported-sql.txt}.
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
                Dsl.PSC.select("department").distinctOn("department").from("employees").build());

        // 5. MySQL-style DISTINCTROW.
        assertSp("SELECT DISTINCTROW department FROM employees", List.of(), Dsl.PSC.select("department").distinctRow().from("employees").build());

        // 6. Custom SELECT modifier.
        assertSp("SELECT TOP 10 * FROM users", List.of(), Dsl.PSC.select("*").selectModifier("TOP 10").from("users").build());

        // 7. Multiple-table FROM.
        assertSp("SELECT * FROM users u, orders o", List.of(), Dsl.PSC.select("*").from("users u", "orders o").build());

        // 8. Derived table / subquery in FROM.
        assertSp("SELECT * FROM (SELECT id FROM users) u", List.of(), Dsl.PSC.select("*").from(Dsl.PSC.select("id").from("users"), "u").build());

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
                Dsl.PSC.update("users").set(Arrays.asList("name", "status")).where(Filters.eq("id", 7)).build());

        // 16. Value-bearing UPDATE.
        assertSp("UPDATE users SET status = ? WHERE id = ?", List.of("INACTIVE", 7),
                Dsl.PSC.update("users").set("status", "INACTIVE").where(Filters.eq("id", 7)).build());

        // 17. Expression assignment in UPDATE.
        assertSp("UPDATE users SET login_count = login_count + 1 WHERE id = ?", List.of(7),
                Dsl.PSC.update("users").set("loginCount", SqlExpression.of("login_count + 1")).where(Filters.eq("id", 7)).build());

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
        assertSp("SELECT * FROM users WHERE active IS true", List.of(), Dsl.PSC.select("*").from("users").where(Filters.isTrue("active")).build());

        // 26. Null/empty/zero convenience predicate.
        assertSp("SELECT * FROM users WHERE (name IS NULL) OR (name = ?)", List.of(""),
                Dsl.PSC.select("*").from("users").where(Filters.isNullOrEmpty("name")).build());

        // 27. NaN / infinity predicate.
        assertSp("SELECT * FROM metrics WHERE score IS INFINITE", List.of(), Dsl.PSC.select("*").from("metrics").where(Filters.isInfinite("score")).build());

        // 28. IN / NOT IN value list.
        assertSp("SELECT * FROM users WHERE id IN (?, ?, ?)", List.of(1, 2, 3), Dsl.PSC.select("*").from("users").where(Filters.in("id", 1, 2, 3)).build());

        // 29. Row-value / multi-column IN.
        assertSp("SELECT * FROM users WHERE (id, type) IN ((?, ?), (?, ?))", List.of(1, "A", 2, "B"),
                Dsl.PSC.select("*").from("users").where(Filters.in(List.of("id", "type"), List.of(List.of(1, "A"), List.of(2, "B")))).build());

        // 30. Boolean AND / OR composition.
        assertSp("SELECT * FROM users WHERE (status = ?) AND ((age > ?) OR (vip = ?))", List.of("ACTIVE", 21, true),
                Dsl.PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.gt("age", 21).or(Filters.eq("vip", true)))).build());

        // 31. Boolean NOT.
        assertSp("SELECT * FROM users WHERE NOT (status = ?)", List.of("ACTIVE"),
                Dsl.PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").not()).build());

        // 32. Raw predicate expression.
        assertSp("SELECT * FROM users WHERE score > average_score", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.expr("score > average_score")).build());
    }

    @Test
    public void testSubqueries() {
        // 33. Scalar subquery comparison.
        assertSp("SELECT * FROM users WHERE id = (SELECT MAX(user_id) FROM orders)", List.of(),
                Dsl.PSC.select("*").from("users").where(Filters.eq("id", Filters.subQuery(Dsl.PSC.select("MAX(user_id)").from("orders")))).build());

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
                        .where(Filters.gt("salary", Filters.all(Filters.subQuery(Dsl.PSC.select("salary").from("managers")))))
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
        assertSp("SELECT * FROM users ORDER BY created_at DESC", List.of(), Dsl.PSC.select("*").from("users").orderByDesc("createdAt").build());

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
        assertSp("SELECT id FROM users UNION SELECT id FROM admins", List.of(), Dsl.PSC.select("id").from("users").union(List.of("id")).from("admins").build());

        // 55. UNION ALL.
        assertSp("SELECT id FROM users UNION ALL SELECT id FROM admins", List.of(),
                Dsl.PSC.select("id").from("users").unionAll(List.of("id")).from("admins").build());

        // 56. INTERSECT.
        assertSp("SELECT id FROM users INTERSECT SELECT id FROM subscribers", List.of(),
                Dsl.PSC.select("id").from("users").intersect(List.of("id")).from("subscribers").build());

        // 57. EXCEPT.
        assertSp("SELECT id FROM users EXCEPT SELECT id FROM blocked_users", List.of(),
                Dsl.PSC.select("id").from("users").except(List.of("id")).from("blocked_users").build());

        // 58. Oracle-style MINUS.
        assertSp("SELECT id FROM users MINUS SELECT id FROM blocked_users", List.of(),
                Dsl.PSC.select("id").from("users").minus(List.of("id")).from("blocked_users").build());
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

    @Test
    public void testNewConvenienceApiEdgeCases() {
        assertSp("SELECT DISTINCT status FROM users", List.of(), Dsl.PSC.select("status").distinctOn(" ").from("users").build());
        assertSp("SELECT * FROM users WHERE active IS false", List.of(), Dsl.PSC.select("*").from("users").where(Filters.isFalse("active")).build());
        assertSp("UPDATE users SET status = ? WHERE id = ?", Arrays.asList(null, 7),
                Dsl.PSC.update("users").set("status", (Object) null).where(Filters.eq("id", 7)).build());
        assertSp("UPDATE users SET roles = ? WHERE id = ?", List.of(Set.of("ADMIN", "EDITOR"), 7),
                Dsl.PSC.update("users").set("roles", (Object) Set.of("ADMIN", "EDITOR")).where(Filters.eq("id", 7)).build());
    }

    @Test
    public void testSetAssignmentOverloads() {
        assertSp("UPDATE users SET first_name = ?, status = ? WHERE id = ?", List.of("Ada", "ACTIVE", 7),
                Dsl.PSC.update("users").set("firstName", "Ada", "status", "ACTIVE").where(Filters.eq("id", 7)).build());

        assertSp("UPDATE users SET first_name = :firstName, status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id", Arrays.asList("Ada", null, 7),
                Dsl.NSC.update("users")
                        .set("firstName", "Ada", "status", null, "updatedAt", SqlExpression.of("CURRENT_TIMESTAMP"))
                        .where(Filters.eq("id", 7))
                        .build());
    }

    @Test
    public void testBuilderBackedDerivedTableMergesParametersAndNamedPlaceholders() {
        assertSp("SELECT u.id FROM (SELECT id FROM users WHERE status = ?) u WHERE u.id > ?", List.of("ACTIVE", 10),
                Dsl.PSC.select("u.id")
                        .from(Dsl.PSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")), "u")
                        .where(Filters.gt("u.id", 10))
                        .build());

        assertSp("SELECT * FROM (SELECT id FROM users WHERE status = :status) u WHERE status = :status_2", List.of("INNER", "OUTER"),
                Dsl.NSC.select("*")
                        .from(Dsl.NSC.select("id").from("users").where(Filters.eq("status", "INNER")), "u")
                        .where(Filters.eq("status", "OUTER"))
                        .build());
    }

    @Test
    public void testProtectedFromCompatibilityBridge() throws NoSuchMethodException {
        assertEquals(true, Modifier.isProtected(AbstractQueryBuilder.class.getDeclaredMethod("from", String.class, String.class).getModifiers()));

        // Legacy subclass/helper shape: primary table followed by the complete FROM body.
        assertSp("SELECT * FROM users u, orders o", List.of(), Dsl.PSC.select("*").from("users", "users u, orders o").build());

        // Ordinary two-table calls still take the public multi-table behavior.
        assertSp("SELECT * FROM users, orders", List.of(), Dsl.PSC.select("*").from("users", "orders").build());
    }

    @Test
    public void testBuilderBackedConditionSubQueryMergesParametersAndNamedPlaceholders() {
        final SubQuery positionalChild = Filters.subQuery(Dsl.PSC.select("user_id").from("orders").where(Filters.gt("total", 100)));
        assertSp("SELECT * FROM users WHERE (status = ?) AND (id IN (SELECT user_id FROM orders WHERE total > ?)) AND (age > ?)", List.of("ACTIVE", 100, 18),
                Dsl.PSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", positionalChild), Filters.gt("age", 18)))
                        .build());

        final SubQuery namedChild = Filters.subQuery(Dsl.NSC.select("user_id").from("orders").where(Filters.eq("status", "SHIPPED")));
        assertSp("SELECT * FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2)) AND (status = :status_3)",
                List.of("ACTIVE", "SHIPPED", "PENDING"),
                Dsl.NSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", namedChild), Filters.eq("status", "PENDING")))
                        .build());
    }

    @Test
    public void testBuilderBackedSubQueryCanBeReused() {
        final SubQuery reusable = Filters.subQuery(Dsl.NSC.select("user_id").from("orders").where(Filters.eq("status", "OPEN")));

        assertSp(
                "SELECT * FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2))"
                        + " AND (manager_id IN (SELECT user_id FROM orders WHERE status = :status_3)) AND (status = :status_4)",
                List.of("ACTIVE", "OPEN", "OPEN", "PENDING"),
                Dsl.NSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", reusable), Filters.in("managerId", reusable),
                                Filters.eq("status", "PENDING")))
                        .build());
    }

    @Test
    public void testBuilderBackedSubQuerySupportsIbatisAndCustomNamedHandlers() {
        final SubQuery ibatisSub = Filters.subQuery(Dsl.MSC.select("user_id").from("orders").where(Filters.eq("status", "OPEN")));
        assertSp(
                "SELECT * FROM users WHERE (status = #{status}) AND (id IN (SELECT user_id FROM orders WHERE status = #{status_2}))"
                        + " AND (status = #{status_3})",
                List.of("ACTIVE", "OPEN", "PENDING"),
                Dsl.MSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", ibatisSub), Filters.eq("status", "PENDING")))
                        .build());

        final Dsl childDsl = Dsl.forDialect(
                Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("CAST(:").append(name).append(" AS varchar)")).build());
        final Dsl parentDsl = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final SubQuery customSub = Filters.subQuery(childDsl.select("user_id").from("orders").where(Filters.eq("status", "OPEN")));

        assertSp("SELECT * FROM users WHERE (status = ${status}) AND (id IN (SELECT user_id FROM orders WHERE status = ${status_2}))",
                List.of("ACTIVE", "OPEN"),
                parentDsl.select("*").from("users").where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", customSub))).build());
    }

    @Test
    public void testBuilderBackedSubQuerySupportsCriteriaSetOperationsAndNullParameters() {
        final SubQuery criteriaSub = Filters.subQuery(Dsl.NSC.select("id").from("archive").where(Filters.eq("status", "OLD")));
        assertSp("SELECT id FROM users WHERE status = :status UNION SELECT id FROM archive WHERE status = :status_2", List.of("ACTIVE", "OLD"),
                Dsl.NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")).append(Criteria.builder().union(criteriaSub).build()).build());

        final SubQuery nullSub = Filters.subQuery(Dsl.PSC.select("id").from("items").where(Filters.in("code", Arrays.asList((Object) null))));
        assertSp("SELECT * FROM orders WHERE item_id IN (SELECT id FROM items WHERE code IN (?))", Arrays.asList((Object) null),
                Dsl.PSC.select("*").from("orders").where(Filters.in("itemId", nullSub)).build());
    }

    @Test
    public void testBuilderBackedSubQueryTakesOwnershipOfPlaceholderMetadata() {
        final SqlBuilder source = Dsl.NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE"));
        final Map<String, Integer> occurrences = source._namedParameterNameOccurrences;
        final Set<String> generatedNames = source._generatedNamedParameterNames;
        final Map<String, String> renderedTokens = source._renderedNamedParameterTokens;

        final SubQuerySnapshot snapshot = (SubQuerySnapshot) Filters.subQuery(source);

        assertSame(occurrences, snapshot.namedParameterNameOccurrences);
        assertSame(generatedNames, snapshot.generatedNamedParameterNames);
        assertSame(renderedTokens, snapshot.renderedNamedParameterTokens);
        assertEquals(List.of("ACTIVE"), snapshot.parameters());
    }

    @Test
    public void testBuilderBackedSubQueryValidationIsAtomic() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(Dsl.PSC.update("users").set("status", "INACTIVE")));

        final SqlBuilder derivedParent = Dsl.PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> derivedParent.from(Dsl.PSC.update("users").set("status", "INACTIVE"), "u"));
        assertSp("SELECT * FROM users", List.of(), derivedParent.from("users").build());

        final SqlBuilder namedChild = Dsl.NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE"));
        final SqlBuilder positionalParent = Dsl.PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> positionalParent.from(namedChild, "u"));
        assertSp("SELECT id FROM users WHERE status = :status", List.of("ACTIVE"), namedChild.build());
        assertSp("SELECT * FROM users", List.of(), positionalParent.from("users").build());

        final SubQuery namedSubQuery = Filters.subQuery(Dsl.NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")));
        final SqlBuilder outer = Dsl.PSC.select("*").from("users");
        assertThrows(IllegalArgumentException.class, () -> outer.where(Filters.in("id", namedSubQuery)));
        assertSp("SELECT * FROM users", List.of(), outer.build());

        final SqlBuilder self = Dsl.PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> self.from(self, "u"));
        assertSp("SELECT * FROM users", List.of(), self.from("users").build());
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
