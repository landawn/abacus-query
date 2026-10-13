package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AnyTest extends TestBase {

    @Test
    void testBudgetComparison() {
        // expense > ANY (SELECT budget FROM departments WHERE region = 'West')
        Condition whereCondition = new Equal("region", "West");
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), whereCondition);
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testComplexSubQuery() {
        // Test with a complex subquery
        SubQuery complexSubQuery = Filters.subQuery("departments", Arrays.asList("budget"),
                Filters.and(Filters.eq("region", "West"), Filters.gt("employee_count", 50), Filters.ne("status", "inactive")));

        Any any = Filters.any(complexSubQuery);

        var params = any.parameters();
        assertEquals(3, params.size());
        assertTrue(params.contains("West"));
        assertTrue(params.contains(50));
        assertTrue(params.contains("inactive"));
    }

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any any = Filters.any(subQuery);

        assertNotNull(any);
        assertEquals(Operator.ANY, any.operator());
        assertEquals(subQuery, any.condition());
    }

    @Test
    void testConstructor_RejectsKnownMultiColumnProjection() {
        final SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price", "tax"), (Condition) null);

        assertThrows(IllegalArgumentException.class, () -> new Any(subQuery));
    }

    @Test
    void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery3 = Filters.subQuery("SELECT cost FROM products");

        Any any1 = Filters.any(subQuery1);
        Any any2 = Filters.any(subQuery2);
        Any any3 = Filters.any(subQuery3);

        assertEquals(any1, any1);
        assertEquals(any1, any2);
        assertNotEquals(any1, any3);
        assertNotEquals(any1, null);
        assertNotEquals(any1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Any condition = new Any(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    void testEqualsAnyScenario() {
        // id = ANY (subquery) is equivalent to IN
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_sessions");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Any condition = new Any(subQuery);
        SubQuery retrieved = (SubQuery) condition.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM exams");
        Any condition = new Any(subQuery);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    void testGreaterThanAnyScenario() {
        // salary > ANY (SELECT salary FROM employees WHERE department = 'Sales')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE department = 'Sales'");
        Any condition = new Any(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertTrue(condition instanceof ComposableCell);
        assertTrue(condition instanceof ComposableCondition);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testLessThanAnyScenario() {
        // price < ANY (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products WHERE available = true");
        Any condition = new Any(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }

    @Test
    void testNullSubQueryMessageNamesSubQuery() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Any(null));
        assertTrue(e.getMessage().contains("subQuery"), e.getMessage());
    }

    @Test
    void testParameters() {
        // Create a subquery with parameters
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.eq("category", "Electronics"));
        Any any = Filters.any(subQuery);

        var params = any.parameters();
        assertEquals(1, params.size());
        assertEquals("Electronics", params.get(0));
    }

    @Test
    void testParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Any condition = new Any(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "published");
        SubQuery subQuery = Filters.subQuery("articles", Arrays.asList("views"), whereCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("published", params.get(0));
    }

    @Test
    void testPassingScoreCheck() {
        // student_score > ANY (SELECT passing_score FROM exams WHERE subject = 'Math')
        Condition whereCondition = new Equal("subject", "Math");
        SubQuery subQuery = Filters.subQuery("exams", Arrays.asList("passing_score"), whereCondition);
        Any condition = new Any(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }

    @Test
    void testRejectsStandalonePredicateUse() {
        Any condition = new Any(Filters.subQuery("SELECT score FROM exams"));

        assertThrows(IllegalArgumentException.class, () -> new Where(condition));
        assertThrows(IllegalArgumentException.class, () -> condition.and(Filters.eq("active", true)));
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Any any = Filters.any(subQuery);

        String result = any.toString();
        assertEquals("ANY (SELECT budget FROM departments)", result);
    }

    @Test
    void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    void testToString_WithStructuredQuery() {
        Condition whereCondition = new LessThan("age", 30);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Any condition = new Any(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT departmentId FROM employees");
        Any any = Filters.any(subQuery);

        // Naming policy should be applied to the subquery
        String result = any.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("ANY"));
    }

    @Test
    void testUsageScenarios() {
        // Test = ANY (equivalent to IN)
        SubQuery managerIds = Filters.subQuery("SELECT id FROM employees WHERE is_manager = true");
        Any anyManager = Filters.any(managerIds);

        // This would be used like: employee_id = ANY (subquery)
        assertNotNull(anyManager);

        // Test > ANY (greater than at least one)
        SubQuery juniorSalaries = Filters.subQuery("SELECT salary FROM employees WHERE level = 'junior'");
        Any anyJuniorSalary = Filters.any(juniorSalaries);

        // This would be used like: salary > ANY (subquery)
        assertNotNull(anyJuniorSalary);
    }

    @Test
    void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("active", true), new GreaterThan("stock", 0)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }
}
