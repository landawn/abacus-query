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

import static com.landawn.abacus.query.Dsl.*;
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
import com.landawn.abacus.query.condition.Condition;
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
        assertSp("SELECT id, first_name FROM users", List.of(), PSC.select("id", "first_name").from("users").build());

        // 2. Column alias.
        assertSp("SELECT first_name AS \"fname\" FROM users", List.of(), PSC.select(Map.of("firstName", "fname")).from("users").build());

        // 3. SELECT DISTINCT.
        assertSp("SELECT DISTINCT status FROM users", List.of(), PSC.select("status").distinct().from("users").build());

        // 4. PostgreSQL-style DISTINCT ON.
        assertSp("SELECT DISTINCT ON (department) department FROM employees", List.of(),
                PSC.select("department").distinctOn("department").from("employees").build());

        // 5. MySQL-style DISTINCTROW.
        assertSp("SELECT DISTINCTROW department FROM employees", List.of(), PSC.select("department").distinctRow().from("employees").build());

        // 6. Custom SELECT modifier.
        assertSp("SELECT TOP 10 * FROM users", List.of(), PSC.select("*").selectModifier("TOP 10").from("users").build());

        // 7. Multiple-table FROM.
        assertSp("SELECT * FROM users u, orders o", List.of(), PSC.select("*").from("users u", "orders o").build());

        // 8. Derived table / subquery in FROM.
        assertSp("SELECT * FROM (SELECT id FROM users) u", List.of(), PSC.select("*").from(PSC.select("id").from("users"), "u").build());

        // 9. Entity-derived SELECT and table.
        assertSp("SELECT id AS \"id\" FROM account WHERE id = ?", List.of(7),
                PSC.selectFrom(Selection.builder(Account.class).includedPropNames(List.of("id")).build()).where(Filters.eq("id", 7)).build());

        // 10. Aggregate COUNT(*) factory.
        assertSp("SELECT count(*) FROM users WHERE active = ?", List.of(true), PSC.selectCountFrom("users").where(Filters.eq("active", true)).build());
    }

    @Test
    public void testDataModificationStatements() {
        // 11. INSERT template.
        assertSp("INSERT INTO users (name, status) VALUES (?, ?)", List.of(), PSC.insert("name", "status").into("users").build());

        // 12. Value-bearing INSERT from a map.
        assertSp("INSERT INTO users (name) VALUES (?)", List.of("Ada"), PSC.insert(Map.of("name", "Ada")).into("users").build());

        // 13. Batch/multi-row INSERT.
        assertSp("INSERT INTO users (name) VALUES (?), (?)", List.of("Ada", "Linus"),
                PSC.batchInsert(List.of(Map.of("name", "Ada"), Map.of("name", "Linus"))).into("users").build());

        // 14. INSERT ... SELECT.
        assertSp("INSERT INTO archived_users (id, name) SELECT id, name FROM users", List.of(),
                PSC.select("id", "name").into("archived_users").from("users").build());

        // 15. UPDATE template. SET placeholders are intentionally unbound; the WHERE value is bound.
        assertSp("UPDATE users SET name = ?, status = ? WHERE id = ?", List.of(7),
                PSC.update("users").set(Arrays.asList("name", "status")).where(Filters.eq("id", 7)).build());

        // 16. Value-bearing UPDATE.
        assertSp("UPDATE users SET status = ? WHERE id = ?", List.of("INACTIVE", 7),
                PSC.update("users").set("status", "INACTIVE").where(Filters.eq("id", 7)).build());

        // 17. Expression assignment in UPDATE.
        assertSp("UPDATE users SET login_count = login_count + 1 WHERE id = ?", List.of(7),
                PSC.update("users").set("loginCount", SqlExpression.of("login_count + 1")).where(Filters.eq("id", 7)).build());

        // 18. DELETE.
        assertSp("DELETE FROM users WHERE id = ?", List.of(7), PSC.deleteFrom("users").where(Filters.eq("id", 7)).build());

        // 19. Standalone condition fragment.
        assertSp("status = ?", List.of("ACTIVE"), PSC.renderCondition(Filters.eq("status", "ACTIVE")).build());
    }

    @Test
    public void testPredicates() {
        // 20. Comparisons.
        assertSp("SELECT * FROM users WHERE age >= ?", List.of(18), PSC.select("*").from("users").where(Filters.ge("age", 18)).build());

        // 21. BETWEEN / NOT BETWEEN.
        assertSp("SELECT * FROM users WHERE age BETWEEN ? AND ?", List.of(18, 65), PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build());

        // 22. LIKE / NOT LIKE.
        assertSp("SELECT * FROM users WHERE name LIKE ?", List.of("A%"), PSC.select("*").from("users").where(Filters.like("name", "A%")).build());

        // 23. Pattern helper.
        assertSp("SELECT * FROM users WHERE name LIKE ?", List.of("A%"), PSC.select("*").from("users").where(Filters.startsWith("name", "A")).build());

        // 24. IS NULL / IS NOT NULL.
        assertSp("SELECT * FROM users WHERE deleted_at IS NULL", List.of(), PSC.select("*").from("users").where(Filters.isNull("deletedAt")).build());

        // 25. IS / IS NOT.
        assertSp("SELECT * FROM users WHERE active IS TRUE", List.of(), PSC.select("*").from("users").where(Filters.isTrue("active")).build());

        // 26. Null/empty/zero convenience predicate.
        assertSp("SELECT * FROM users WHERE (name IS NULL) OR (name = ?)", List.of(""),
                PSC.select("*").from("users").where(Filters.isNullOrEmpty("name")).build());

        // 27. NaN / infinity predicate.
        assertSp("SELECT * FROM metrics WHERE score IS INFINITE", List.of(), PSC.select("*").from("metrics").where(Filters.isInfinite("score")).build());

        // 28. IN / NOT IN value list.
        assertSp("SELECT * FROM users WHERE id IN (?, ?, ?)", List.of(1, 2, 3), PSC.select("*").from("users").where(Filters.in("id", 1, 2, 3)).build());

        // 29. Row-value / multi-column IN.
        assertSp("SELECT * FROM users WHERE (id, type) IN ((?, ?), (?, ?))", List.of(1, "A", 2, "B"),
                PSC.select("*").from("users").where(Filters.in(List.of("id", "type"), List.of(List.of(1, "A"), List.of(2, "B")))).build());

        // 30. Boolean AND / OR composition.
        assertSp("SELECT * FROM users WHERE (status = ?) AND ((age > ?) OR (vip = ?))", List.of("ACTIVE", 21, true),
                PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.gt("age", 21).or(Filters.eq("vip", true)))).build());

        // 31. Boolean NOT.
        assertSp("SELECT * FROM users WHERE NOT (status = ?)", List.of("ACTIVE"),
                PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").not()).build());

        // 32. Raw predicate expression.
        assertSp("SELECT * FROM users WHERE score > average_score", List.of(),
                PSC.select("*").from("users").where(Filters.expr("score > average_score")).build());
    }

    @Test
    public void testSubqueries() {
        // 33. Scalar subquery comparison.
        assertSp("SELECT * FROM users WHERE id = (SELECT MAX(user_id) FROM orders)", List.of(),
                PSC.select("*").from("users").where(Filters.eq("id", PSC.select("MAX(user_id)").from("orders").toSubQuery())).build());

        // 34. Structured IN subquery with parameters.
        assertSp("SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > ?)", List.of(100),
                PSC.select("*").from("users").where(Filters.in("id", Filters.subQuery("orders", "user_id", Filters.gt("total", 100)))).build());

        // 35. Row-value IN subquery.
        assertSp("SELECT * FROM users WHERE (id, type) IN (SELECT user_id, type FROM memberships)", List.of(),
                PSC.select("*").from("users").where(Filters.in(List.of("id", "type"), Filters.subQuery("SELECT user_id, type FROM memberships"))).build());

        // 36. EXISTS / NOT EXISTS.
        assertSp("SELECT * FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)", List.of(),
                PSC.select("*").from("users u").where(Filters.exists(Filters.subQuery("SELECT 1 FROM orders o WHERE o.user_id = u.id"))).build());

        // 37. Quantified subquery.
        assertSp("SELECT * FROM employees WHERE salary > ALL (SELECT salary FROM managers)", List.of(),
                PSC.select("*").from("employees").where(Filters.gt("salary", Filters.all(PSC.select("salary").from("managers").toSubQuery()))).build());
    }

    @Test
    public void testJoins() {
        // 38. Generic / INNER JOIN ... ON.
        assertSp("SELECT u.id FROM users u INNER JOIN orders o ON u.id = o.user_id", List.of(),
                PSC.select("u.id").from("users u").innerJoin("orders o").on("u.id = o.user_id").build());

        // 39. LEFT JOIN.
        assertSp("SELECT u.id FROM users u LEFT JOIN profiles p ON u.id = p.user_id", List.of(),
                PSC.select("u.id").from("users u").leftJoin("profiles p").on("u.id = p.user_id").build());

        // 40. RIGHT JOIN.
        assertSp("SELECT u.id FROM users u RIGHT JOIN profiles p ON u.id = p.user_id", List.of(),
                PSC.select("u.id").from("users u").rightJoin("profiles p").on("u.id = p.user_id").build());

        // 41. FULL JOIN.
        assertSp("SELECT u.id FROM users u FULL JOIN profiles p ON u.id = p.user_id", List.of(),
                PSC.select("u.id").from("users u").fullJoin("profiles p").on("u.id = p.user_id").build());

        // 42. CROSS JOIN.
        assertSp("SELECT * FROM colors CROSS JOIN sizes", List.of(), PSC.select("*").from("colors").crossJoin("sizes").build());

        // 43. NATURAL JOIN.
        assertSp("SELECT * FROM users NATURAL JOIN profiles", List.of(), PSC.select("*").from("users").naturalJoin("profiles").build());

        // 44. JOIN ... USING.
        assertSp("SELECT * FROM users JOIN profiles USING (user_id)", List.of(), PSC.select("*").from("users").join("profiles").using("userId").build());
    }

    @Test
    public void testGroupingOrderingPaginationAndLocking() {
        // 45. GROUP BY.
        assertSp("SELECT department, COUNT(*) FROM employees GROUP BY department", List.of(),
                PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build());

        // 46. HAVING.
        assertSp("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ?", List.of(5),
                PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.gt("COUNT(*)", 5)).build());

        // 47. ORDER BY with direction.
        assertSp("SELECT * FROM users ORDER BY created_at DESC", List.of(), PSC.select("*").from("users").orderByDesc("createdAt").build());

        // 48. LIMIT.
        assertSp("SELECT * FROM users LIMIT 10", List.of(), PSC.select("*").from("users").limit(10).build());

        // 49. LIMIT ... OFFSET.
        assertSp("SELECT * FROM users LIMIT 10 OFFSET 20", List.of(), PSC.select("*").from("users").limit(10, 20).build());

        // 50. Standalone OFFSET.
        assertSp("SELECT * FROM users ORDER BY id OFFSET 20", List.of(), PSC.select("*").from("users").orderBy("id").offset(20).build());

        // 51. ANSI FETCH FIRST.
        assertSp("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", List.of(), PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build());

        // 52. ANSI OFFSET ... FETCH NEXT.
        assertSp("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", List.of(),
                PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build());

        // 53. FOR UPDATE.
        assertSp("SELECT * FROM users WHERE id = ? FOR UPDATE", List.of(7), PSC.select("*").from("users").where(Filters.eq("id", 7)).forUpdate().build());
    }

    @Test
    public void testSetOperations() {
        // 54. UNION.
        assertSp("SELECT id FROM users UNION SELECT id FROM admins", List.of(),
                PSC.select("id").from("users").unionSelect(List.of("id")).from("admins").build());

        // 55. UNION ALL.
        assertSp("SELECT id FROM users UNION ALL SELECT id FROM admins", List.of(),
                PSC.select("id").from("users").unionAllSelect(List.of("id")).from("admins").build());

        // 56. INTERSECT.
        assertSp("SELECT id FROM users INTERSECT SELECT id FROM subscribers", List.of(),
                PSC.select("id").from("users").intersectSelect(List.of("id")).from("subscribers").build());

        // 57. EXCEPT.
        assertSp("SELECT id FROM users EXCEPT SELECT id FROM blocked_users", List.of(),
                PSC.select("id").from("users").exceptSelect(List.of("id")).from("blocked_users").build());

        // 58. Oracle-style MINUS.
        assertSp("SELECT id FROM users MINUS SELECT id FROM blocked_users", List.of(),
                PSC.select("id").from("users").minusSelect(List.of("id")).from("blocked_users").build());
    }

    @Test
    public void testExpressionFamilies() {
        // 59. Arithmetic expressions.
        assertSp("SELECT price + tax FROM orders", List.of(),
                PSC.select(SqlExpression.plus(SqlExpression.of("price"), SqlExpression.of("tax"))).from("orders").build());

        // 60. Bitwise and shift expressions.
        assertSp("SELECT flags & mask FROM users", List.of(),
                PSC.select(SqlExpression.bitwiseAnd(SqlExpression.of("flags"), SqlExpression.of("mask"))).from("users").build());

        // 61. Aggregate functions.
        assertSp("SELECT SUM(amount) FROM orders", List.of(), PSC.select(SqlExpression.sum("amount")).from("orders").build());

        // 62. Numeric functions.
        assertSp("SELECT SQRT(score) FROM metrics", List.of(), PSC.select(SqlExpression.sqrt("score")).from("metrics").build());

        // 63. String functions.
        assertSp("SELECT UPPER(name) FROM users", List.of(), PSC.select(SqlExpression.upper("name")).from("users").build());
    }

    @Test
    public void testParameterNamingAndCompositionPolicies() {
        // 64. Positional parameters.
        assertSp("SELECT * FROM users WHERE status = ?", List.of("ACTIVE"), PSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 65. Named parameters.
        assertSp("SELECT * FROM users WHERE status = :status", List.of("ACTIVE"), NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 66. MyBatis/iBATIS parameters.
        assertSp("SELECT * FROM users WHERE status = #{status}", List.of("ACTIVE"),
                Dsl.MSC.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 67. Inlined literal SQL.
        assertSp("SELECT * FROM users WHERE status = 'ACTIVE'", List.of(), Dsl.SCSB.select("*").from("users").where(Filters.eq("status", "ACTIVE")).build());

        // 68. Identifier naming policy.
        assertSp("SELECT FIRST_NAME AS \"firstName\" FROM account", List.of(), Dsl.PAC.select("firstName").from("account").build());

        // 69. Reusable full query shape via Criteria.
        assertSp("SELECT department FROM users WHERE active = ? GROUP BY department ORDER BY department", List.of(true),
                PSC.select("department")
                        .from("users")
                        .append(Criteria.builder().where(Filters.eq("active", true)).groupBy("department").orderBy("department").build())
                        .build());

        // 70. Conditional SQL composition.
        final boolean includeStatus = true;
        assertSp("SELECT * FROM users WHERE status = ?", List.of("ACTIVE"),
                PSC.select("*").from("users").appendIf(includeStatus, Filters.eq("status", "ACTIVE")).build());

        // 71. Trusted raw trailing fragment escape hatch.
        assertSp("SELECT * FROM users FOR SHARE", List.of(), PSC.select("*").from("users").append("FOR SHARE").build());
    }

    @Test
    public void testNewConvenienceApiEdgeCases() {
        assertSp("SELECT DISTINCT status FROM users", List.of(), PSC.select("status").distinctOn(" ").from("users").build());
        assertSp("SELECT * FROM users WHERE active IS FALSE", List.of(), PSC.select("*").from("users").where(Filters.isFalse("active")).build());
        assertSp("UPDATE users SET status = ? WHERE id = ?", Arrays.asList(null, 7),
                PSC.update("users").set("status", (Object) null).where(Filters.eq("id", 7)).build());
        assertSp("UPDATE users SET roles = ? WHERE id = ?", List.of(Set.of("ADMIN", "EDITOR"), 7),
                PSC.update("users").set("roles", (Object) Set.of("ADMIN", "EDITOR")).where(Filters.eq("id", 7)).build());
    }

    @Test
    public void testSetAssignmentOverloads() {
        assertSp("UPDATE users SET first_name = ?, status = ? WHERE id = ?", List.of("Ada", "ACTIVE", 7),
                PSC.update("users").set("firstName", "Ada", "status", "ACTIVE").where(Filters.eq("id", 7)).build());

        assertSp("UPDATE users SET first_name = :firstName, status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id", Arrays.asList("Ada", null, 7),
                NSC.update("users")
                        .set("firstName", "Ada", "status", null, "updatedAt", SqlExpression.of("CURRENT_TIMESTAMP"))
                        .where(Filters.eq("id", 7))
                        .build());
    }

    @Test
    public void testBuilderBackedDerivedTableMergesParametersAndNamedPlaceholders() {
        assertSp("SELECT u.id FROM (SELECT id FROM users WHERE status = ?) u WHERE u.id > ?", List.of("ACTIVE", 10),
                PSC.select("u.id").from(PSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")), "u").where(Filters.gt("u.id", 10)).build());

        assertSp("SELECT * FROM (SELECT id FROM users WHERE status = :status) u WHERE status = :status_2", List.of("INNER", "OUTER"),
                NSC.select("*").from(NSC.select("id").from("users").where(Filters.eq("status", "INNER")), "u").where(Filters.eq("status", "OUTER")).build());
    }

    @Test
    public void testProtectedFromCompatibilityBridge() throws NoSuchMethodException {
        assertEquals(true, Modifier.isProtected(AbstractQueryBuilder.class.getDeclaredMethod("from", String.class, String.class).getModifiers()));

        // Legacy subclass/helper shape: primary table followed by the complete FROM body.
        assertSp("SELECT * FROM users u, orders o", List.of(), PSC.select("*").from("users", "users u, orders o").build());

        // Ordinary two-table calls still take the public multi-table behavior.
        assertSp("SELECT * FROM users, orders", List.of(), PSC.select("*").from("users", "orders").build());
    }

    @Test
    public void testBuilderBackedConditionSubQueryMergesParametersAndNamedPlaceholders() {
        final SubQuery positionalChild = PSC.select("user_id").from("orders").where(Filters.gt("total", 100)).toSubQuery();
        assertSp("SELECT * FROM users WHERE (status = ?) AND (id IN (SELECT user_id FROM orders WHERE total > ?)) AND (age > ?)", List.of("ACTIVE", 100, 18),
                PSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", positionalChild), Filters.gt("age", 18)))
                        .build());

        final SubQuery namedChild = NSC.select("user_id").from("orders").where(Filters.eq("status", "SHIPPED")).toSubQuery();
        assertSp("SELECT * FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2)) AND (status = :status_3)",
                List.of("ACTIVE", "SHIPPED", "PENDING"),
                NSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", namedChild), Filters.eq("status", "PENDING")))
                        .build());
    }

    @Test
    public void testBuilderBackedSubQueryCanBeReused() {
        final SubQuery reusable = NSC.select("user_id").from("orders").where(Filters.eq("status", "OPEN")).toSubQuery();

        assertSp(
                "SELECT * FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2))"
                        + " AND (manager_id IN (SELECT user_id FROM orders WHERE status = :status_3)) AND (status = :status_4)",
                List.of("ACTIVE", "OPEN", "OPEN", "PENDING"),
                NSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", reusable), Filters.in("managerId", reusable),
                                Filters.eq("status", "PENDING")))
                        .build());
    }

    @Test
    public void testBuilderBackedSubQuerySupportsIbatisAndCustomNamedHandlers() {
        final SubQuery ibatisSub = Dsl.MSC.select("user_id").from("orders").where(Filters.eq("status", "OPEN")).toSubQuery();
        assertSp(
                "SELECT * FROM users WHERE (status = #{status}) AND (id IN (SELECT user_id FROM orders WHERE status = #{status_2}))"
                        + " AND (status = #{status_3})",
                List.of("ACTIVE", "OPEN", "PENDING"),
                Dsl.MSC.select("*")
                        .from("users")
                        .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", ibatisSub), Filters.eq("status", "PENDING")))
                        .build());

        final Dsl childDsl = Dsl
                .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("CAST(:").append(name).append(" AS varchar)")).build());
        final Dsl parentDsl = Dsl
                .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final SubQuery customSub = childDsl.select("user_id").from("orders").where(Filters.eq("status", "OPEN")).toSubQuery();

        assertSp("SELECT * FROM users WHERE (status = ${status}) AND (id IN (SELECT user_id FROM orders WHERE status = ${status_2}))",
                List.of("ACTIVE", "OPEN"),
                parentDsl.select("*").from("users").where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", customSub))).build());
    }

    @Test
    public void testBuilderBackedSubQuerySupportsCriteriaSetOperationsAndNullParameters() {
        final SubQuery criteriaSub = NSC.select("id").from("archive").where(Filters.eq("status", "OLD")).toSubQuery();
        assertSp("SELECT id FROM users WHERE status = :status UNION SELECT id FROM archive WHERE status = :status_2", List.of("ACTIVE", "OLD"),
                NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")).append(Criteria.builder().union(criteriaSub).build()).build());

        // A null IN element is rejected at construction (use an explicit IS NULL predicate); a non-null element
        // still travels through the builder-backed sub-query into the parent's parameter list.
        assertThrows(IllegalArgumentException.class, () -> Filters.in("code", Arrays.asList((Object) null)));

        final SubQuery codeSub = PSC.select("id").from("items").where(Filters.in("code", Arrays.asList("X"))).toSubQuery();
        assertSp("SELECT * FROM orders WHERE item_id IN (SELECT id FROM items WHERE code IN (?))", List.of("X"),
                PSC.select("*").from("orders").where(Filters.in("itemId", codeSub)).build());
    }

    @Test
    public void testBuilderBackedSubQueryTakesOwnershipOfPlaceholderMetadata() {
        final SqlBuilder source = NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE"));
        final Map<String, Integer> occurrences = source._namedParameterNameOccurrences;
        final Set<String> generatedNames = source._generatedNamedParameterNames;
        final Map<String, String> renderedTokens = source._renderedNamedParameterTokens;

        final SubQuerySnapshot snapshot = (SubQuerySnapshot) source.toSubQuery();

        assertSame(occurrences, snapshot.namedParameterNameOccurrences);
        assertSame(generatedNames, snapshot.generatedNamedParameterNames);
        assertSame(renderedTokens, snapshot.renderedNamedParameterTokens);
        assertEquals(List.of("ACTIVE"), snapshot.parameters());
    }

    @Test
    public void testBuilderBackedSubQueryValidationIsAtomic() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set("status", "INACTIVE").toSubQuery());

        final SqlBuilder derivedParent = PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> derivedParent.from(PSC.update("users").set("status", "INACTIVE"), "u"));
        assertSp("SELECT * FROM users", List.of(), derivedParent.from("users").build());

        final SqlBuilder namedChild = NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE"));
        final SqlBuilder positionalParent = PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> positionalParent.from(namedChild, "u"));
        assertSp("SELECT id FROM users WHERE status = :status", List.of("ACTIVE"), namedChild.build());
        assertSp("SELECT * FROM users", List.of(), positionalParent.from("users").build());

        final SubQuery namedSubQuery = NSC.select("id").from("users").where(Filters.eq("status", "ACTIVE")).toSubQuery();
        final SqlBuilder outer = PSC.select("*").from("users");
        assertThrows(IllegalArgumentException.class, () -> outer.where(Filters.in("id", namedSubQuery)));
        assertSp("SELECT * FROM users", List.of(), outer.build());

        final SqlBuilder self = PSC.select("*");
        assertThrows(IllegalArgumentException.class, () -> self.from(self, "u"));
        assertSp("SELECT * FROM users", List.of(), self.from("users").build());
    }

    // A raw SubQuery(sql, bindings) carries positional bindings that must reach the parent's parameter
    // list in placeholder order on every builder path (IN / NOT IN / EXISTS / scalar comparison / set operation).
    @Test
    public void testRawSubQueryBindingsAreMergedIntoParentParameters() {
        final SubQuery raw = new SubQuery("SELECT id FROM orders WHERE x = ?", List.of(1));

        assertSp("SELECT * FROM account WHERE (id IN (SELECT id FROM orders WHERE x = ?)) AND (status = ?)", List.of(1, 7),
                PSC.select("*").from("account").where(Filters.and(Filters.in("id", raw), Filters.eq("status", 7))).build());
        assertSp("SELECT * FROM account WHERE (status = ?) AND (id NOT IN (SELECT id FROM orders WHERE x = ?))", List.of(7, 1),
                PSC.select("*").from("account").where(Filters.and(Filters.eq("status", 7), Filters.notIn("id", raw))).build());
        assertSp("SELECT * FROM account WHERE EXISTS (SELECT id FROM orders WHERE x = ?)", List.of(1),
                PSC.select("*").from("account").where(Filters.exists(raw)).build());
        assertSp("SELECT * FROM account WHERE id = (SELECT id FROM orders WHERE x = ?)", List.of(1),
                PSC.select("*").from("account").where(Filters.eq("id", raw)).build());
        assertSp("SELECT id FROM users UNION SELECT id FROM orders WHERE x = ?", List.of(1),
                PSC.select("id").from("users").append(Criteria.builder().union(raw).build()).build());
        assertSp("id IN (SELECT id FROM orders WHERE x = ?)", List.of(1), PSC.renderCondition(Filters.in("id", raw)).build());

        final SubQuery twoBindings = new SubQuery("SELECT id FROM orders WHERE x = ? AND y = ?", List.of("a", "b"));
        assertSp("SELECT * FROM account WHERE (id IN (SELECT id FROM orders WHERE x = ? AND y = ?)) AND (status = ?)", List.of("a", "b", 7),
                PSC.select("*").from("account").where(Filters.and(Filters.in("id", twoBindings), Filters.eq("status", 7))).build());
    }

    // Under NAMED_SQL / IBATIS_SQL the "?" placeholders of a bound raw SubQuery must be rewritten to unique
    // generated names (in binding order) so the statement never mixes parameter styles; PSC keeps the "?".
    @Test
    public void testRawSubQueryBindingsAreRenamedUnderNamedAndIbatisPolicies() {
        final SubQuery raw = new SubQuery("SELECT id FROM x WHERE y = ? AND z = ?", List.of(1, 2));
        // The outer property is literally named "param" to prove the generated names stay collision-free.
        final Condition cond = Filters.and(Filters.in("id", raw), Filters.exists(raw), Filters.eq("param", 7));
        final List<Object> expectedParameters = List.of(1, 2, 1, 2, 7);

        final SP nsc = NSC.select("*").from("account").where(cond).build();
        assertEquals("SELECT * FROM account WHERE (id IN (SELECT id FROM x WHERE y = :param AND z = :param_2))"
                + " AND (EXISTS (SELECT id FROM x WHERE y = :param_3 AND z = :param_4)) AND (param = :param_5)", nsc.query());
        assertEquals(expectedParameters, nsc.parameters());
        assertEquals(-1, nsc.query().indexOf('?'));
        final ParsedSql nscParsed = ParsedSql.parse(nsc.query());
        assertEquals(5, nscParsed.parameterCount());
        assertEquals(List.of("param", "param_2", "param_3", "param_4", "param_5"), nscParsed.namedParameters());
        assertEquals(5, Set.copyOf(nscParsed.namedParameters()).size());

        final SP msc = MSC.select("*").from("account").where(cond).build();
        assertEquals("SELECT * FROM account WHERE (id IN (SELECT id FROM x WHERE y = #{param} AND z = #{param_2}))"
                + " AND (EXISTS (SELECT id FROM x WHERE y = #{param_3} AND z = #{param_4})) AND (param = #{param_5})", msc.query());
        assertEquals(expectedParameters, msc.parameters());
        assertEquals(-1, msc.query().indexOf('?'));
        final ParsedSql mscParsed = ParsedSql.parse(msc.query());
        assertEquals(5, mscParsed.parameterCount());
        assertEquals(List.of("param", "param_2", "param_3", "param_4", "param_5"), mscParsed.namedParameters());

        // "?" inside quoted text and comments is never a placeholder and must survive the rewrite verbatim.
        final SubQuery tricky = new SubQuery("SELECT id FROM x WHERE k = 'a?' AND [w?] = ? /* ? */ -- ?\n AND z = ?", List.of("p", "q"));
        final SP trickyNsc = NSC.select("*").from("account").where(Filters.in("id", tricky)).build();
        assertEquals("SELECT * FROM account WHERE id IN (SELECT id FROM x WHERE k = 'a?' AND [w?] = :param /* ? */ -- ?\n AND z = :param_2)",
                trickyNsc.query());
        assertEquals(List.of("p", "q"), trickyNsc.parameters());
        assertEquals(2, ParsedSql.parse(trickyNsc.query()).parameterCount());

        // PSC control: positional placeholders are kept as-is.
        final SP psc = PSC.select("*").from("account").where(cond).build();
        assertEquals("SELECT * FROM account WHERE (id IN (SELECT id FROM x WHERE y = ? AND z = ?))"
                + " AND (EXISTS (SELECT id FROM x WHERE y = ? AND z = ?)) AND (param = ?)", psc.query());
        assertEquals(expectedParameters, psc.parameters());

        // A PostgreSQL JSON "?" operator is not a placeholder for the SubQuery constructor, and the named
        // rewrite follows the same ParsedSql classification: the operator stays verbatim and only the real
        // binding is renamed. PSC renders everything untouched.
        final SubQuery jsonOperator = new SubQuery("SELECT id FROM x WHERE data ? 'k' AND z = ?", List.of(5));
        assertSp("SELECT * FROM account WHERE id IN (SELECT id FROM x WHERE data ? 'k' AND z = ?)", List.of(5),
                PSC.select("*").from("account").where(Filters.in("id", jsonOperator)).build());
        assertSp("SELECT * FROM account WHERE id IN (SELECT id FROM x WHERE data ? 'k' AND z = :param)", List.of(5),
                NSC.select("*").from("account").where(Filters.in("id", jsonOperator)).build());
    }

    // The "?" placeholders of a raw SubQuery are located by ParsedSql, the classifier SubQuery(String, Collection)
    // validated the binding count with, so a PostgreSQL JSON "?" operator is never rewritten and a "?" inside an
    // array subscript is; the two scans can no longer disagree.
    @Test
    public void testRawSubQueryPlaceholderPositionsFollowParsedSqlClassification() {
        final SubQuery raw = new SubQuery("SELECT arr[?] FROM t WHERE doc ? 'key'", List.of(2));

        final SP nsc = NSC.select("*").from("account").where(Filters.in("id", raw)).build();
        assertEquals("SELECT * FROM account WHERE id IN (SELECT arr[:param] FROM t WHERE doc ? 'key')", nsc.query());
        assertEquals(List.of(2), nsc.parameters());
        final ParsedSql nscParsed = ParsedSql.parse(nsc.query());
        assertEquals(List.of("param"), nscParsed.namedParameters());
        assertEquals(1, nscParsed.parameterCount());

        final SP msc = MSC.select("*").from("account").where(Filters.in("id", raw)).build();
        assertEquals("SELECT * FROM account WHERE id IN (SELECT arr[#{param}] FROM t WHERE doc ? 'key')", msc.query());
        assertEquals(List.of(2), msc.parameters());
        final ParsedSql mscParsed = ParsedSql.parse(msc.query());
        assertEquals(List.of("param"), mscParsed.namedParameters());
        assertEquals(1, mscParsed.parameterCount());

        assertSp("SELECT * FROM account WHERE id IN (SELECT arr[2] FROM t WHERE doc ? 'key')", List.of(),
                SCSB.select("*").from("account").where(Filters.in("id", raw)).build());

        // A JSON operator ahead of a real binding: the operator is untouched, the binding is rewritten.
        final SubQuery jsonOperatorFirst = new SubQuery("SELECT id FROM x WHERE data ? 'k' AND id = ?", List.of(5));
        final SP jsonNsc = NSC.select("*").from("account").where(Filters.exists(jsonOperatorFirst)).build();
        assertEquals("SELECT * FROM account WHERE EXISTS (SELECT id FROM x WHERE data ? 'k' AND id = :param)", jsonNsc.query());
        assertEquals(List.of(5), jsonNsc.parameters());
        assertEquals(List.of("param"), ParsedSql.parse(jsonNsc.query()).namedParameters());
        assertSp("SELECT * FROM account WHERE EXISTS (SELECT id FROM x WHERE data ? 'k' AND id = #{param})", List.of(5),
                MSC.select("*").from("account").where(Filters.exists(jsonOperatorFirst)).build());
        assertSp("SELECT * FROM account WHERE EXISTS (SELECT id FROM x WHERE data ? 'k' AND id = 5)", List.of(),
                SCSB.select("*").from("account").where(Filters.exists(jsonOperatorFirst)).build());

        // PSC control: nothing is rewritten.
        assertSp("SELECT * FROM account WHERE id IN (SELECT arr[?] FROM t WHERE doc ? 'key')", List.of(2),
                PSC.select("*").from("account").where(Filters.in("id", raw)).build());
        assertSp("SELECT * FROM account WHERE EXISTS (SELECT id FROM x WHERE data ? 'k' AND id = ?)", List.of(5),
                PSC.select("*").from("account").where(Filters.exists(jsonOperatorFirst)).build());

        // "?" inside quoted literals, comments and bracket-quoted identifiers is still never rewritten, and
        // leading whitespace in the raw text (ParsedSql trims it) does not shift the substituted positions.
        final SubQuery tricky = new SubQuery("  SELECT id FROM x WHERE k = 'a?' AND [w?] = ? /* ? */ -- ?\n AND z = ? ", List.of("p", 2));
        assertSp("SELECT * FROM account WHERE id IN (  SELECT id FROM x WHERE k = 'a?' AND [w?] = :param /* ? */ -- ?\n AND z = :param_2 )",
                List.of("p", 2), NSC.select("*").from("account").where(Filters.in("id", tricky)).build());
        assertSp("SELECT * FROM account WHERE id IN (  SELECT id FROM x WHERE k = 'a?' AND [w?] = 'p' /* ? */ -- ?\n AND z = 2 )", List.of(),
                SCSB.select("*").from("account").where(Filters.in("id", tricky)).build());
    }

    // Under RAW_SQL ("inline values directly into the SQL string as literals") the positional bindings of a
    // raw SubQuery are inlined in placeholder order with exactly the literal rendering a structured condition's
    // value receives on the same builder, and nothing is bound. PSC keeps the "?" + bindings.
    @Test
    public void testRawSubQueryBindingsAreInlinedAsLiteralsUnderRawSql() {
        final SqlExpression expr = SqlExpression.of("NOW()");
        final List<Object> bindings = Arrays.asList(1, "O'Brien", null, expr);
        final SubQuery raw = new SubQuery("SELECT id FROM orders WHERE x = ? AND y = ? AND z = ? AND t < ?", bindings);

        final SP scsbIn = SCSB.select("*").from("account").where(Filters.in("id", raw)).build();
        assertEquals("SELECT * FROM account WHERE id IN (SELECT id FROM orders WHERE x = 1 AND y = 'O''Brien' AND z = null AND t < NOW())", scsbIn.query());
        assertEquals(List.of(), scsbIn.parameters());
        assertEquals(-1, scsbIn.query().indexOf('?'));

        final SP scsbExists = SCSB.select("*").from("account").where(Filters.and(Filters.exists(raw), Filters.eq("status", "A"))).build();
        assertEquals("SELECT * FROM account WHERE (EXISTS (SELECT id FROM orders WHERE x = 1 AND y = 'O''Brien' AND z = null AND t < NOW()))"
                + " AND (status = 'A')", scsbExists.query());
        assertEquals(List.of(), scsbExists.parameters());

        final SP lcsb = LCSB.select("*").from("account").where(Filters.notIn("id", raw)).build();
        assertEquals("SELECT * FROM account WHERE id NOT IN (SELECT id FROM orders WHERE x = 1 AND y = 'O''Brien' AND z = null AND t < NOW())",
                lcsb.query());
        assertEquals(List.of(), lcsb.parameters());
        assertEquals(-1, lcsb.query().indexOf('?'));

        // The inlined literal text is exactly what a structured condition renders for the same value on the same builder.
        assertEquals("x = 1", SCSB.renderCondition(Filters.eq("x", 1)).build().query());
        assertEquals("y = 'O''Brien'", SCSB.renderCondition(Filters.eq("y", "O'Brien")).build().query());
        assertEquals("t < NOW()", SCSB.renderCondition(Filters.lt("t", expr)).build().query());
        assertEquals("y = 'O''Brien'", LCSB.renderCondition(Filters.eq("y", "O'Brien")).build().query());
        assertEquals("UPDATE t SET z = null", SCSB.update("t").set("z", (Object) null).build().query());

        // A "?" inside quoted text or a comment is never a placeholder and survives verbatim.
        final SubQuery tricky = new SubQuery("SELECT id FROM x WHERE k = 'a?' AND [w?] = ? /* ? */ -- ?\n AND z = ?", List.of("p", 2));
        assertSp("SELECT * FROM account WHERE id IN (SELECT id FROM x WHERE k = 'a?' AND [w?] = 'p' /* ? */ -- ?\n AND z = 2)", List.of(),
                SCSB.select("*").from("account").where(Filters.in("id", tricky)).build());

        // A PostgreSQL JSON "?" operator is not a placeholder (same ParsedSql classification as the SubQuery
        // constructor): it stays verbatim and only the real binding is inlined.
        final SubQuery jsonOperator = new SubQuery("SELECT id FROM x WHERE data ? 'k' AND z = ?", List.of(5));
        assertSp("SELECT * FROM account WHERE id IN (SELECT id FROM x WHERE data ? 'k' AND z = 5)", List.of(),
                SCSB.select("*").from("account").where(Filters.in("id", jsonOperator)).build());

        // PSC control: positional placeholders and bindings are unchanged.
        assertSp("SELECT * FROM account WHERE id IN (SELECT id FROM orders WHERE x = ? AND y = ? AND z = ? AND t < ?)", bindings,
                PSC.select("*").from("account").where(Filters.in("id", raw)).build());
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
