package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class Any2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM exams");
        Any condition = new Any(subQuery);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Any condition = new Any(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "published");
        SubQuery subQuery = Filters.subQuery("articles", Arrays.asList("views"), whereCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("published", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new GreaterThan("rating", (Object) 3);
        SubQuery subQuery = Filters.subQuery("reviews", Arrays.asList("score"), whereCondition);
        Any condition = new Any(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new LessThan("age", (Object) 30);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Any condition = new Any(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Any condition = new Any(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertTrue(condition instanceof Cell);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanAnyScenario() {
        // salary > ANY (SELECT salary FROM employees WHERE department = 'Sales')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE department = 'Sales'");
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    public void testEqualsAnyScenario() {
        // id = ANY (subquery) is equivalent to IN
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_sessions");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testLessThanAnyScenario() {
        // price < ANY (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products WHERE available = true");
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("active", true), new GreaterThan("stock", (Object) 0)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testBudgetComparison() {
        // expense > ANY (SELECT budget FROM departments WHERE region = 'West')
        Condition whereCondition = new Equal("region", "West");
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), whereCondition);
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testPassingScoreCheck() {
        // student_score > ANY (SELECT passing_score FROM exams WHERE subject = 'Math')
        Condition whereCondition = new Equal("subject", "Math");
        SubQuery subQuery = Filters.subQuery("exams", Arrays.asList("passing_score"), whereCondition);
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }
}

public class AnyTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any any = Filters.any(subQuery);

        Assertions.assertNotNull(any);
        Assertions.assertEquals(Operator.ANY, any.operator());
        Assertions.assertEquals(subQuery, any.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Any any = Filters.any(subQuery);

        SubQuery retrieved = any.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Any any = Filters.any(subQuery);

        String result = any.toString();
        Assertions.assertEquals("ANY (SELECT budget FROM departments)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT departmentId FROM employees");
        Any any = Filters.any(subQuery);

        // Naming policy should be applied to the subquery
        String result = any.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("ANY"));
    }

    @Test
    public void testGetParameters() {
        // Create a subquery with parameters
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.eq("category", "Electronics"));
        Any any = Filters.any(subQuery);

        var params = any.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("Electronics", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.between("price", 100, 500));
        Any any = Filters.any(subQuery);

        Assertions.assertEquals(2, any.getParameters().size());

        any.clearParameters();

        Assertions.assertEquals(2, any.getParameters().size());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery3 = Filters.subQuery("SELECT cost FROM products");

        Any any1 = Filters.any(subQuery1);
        Any any2 = Filters.any(subQuery2);
        Any any3 = Filters.any(subQuery3);

        Assertions.assertEquals(any1, any1);
        Assertions.assertEquals(any1, any2);
        Assertions.assertNotEquals(any1, any3);
        Assertions.assertNotEquals(any1, null);
        Assertions.assertNotEquals(any1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT level FROM grades");
        SubQuery subQuery2 = Filters.subQuery("SELECT level FROM grades");

        Any any1 = Filters.any(subQuery1);
        Any any2 = Filters.any(subQuery2);

        Assertions.assertEquals(any1.hashCode(), any2.hashCode());
    }

    @Test
    public void testComplexSubQuery() {
        // Test with a complex subquery
        SubQuery complexSubQuery = Filters.subQuery("departments", Arrays.asList("budget"),
                Filters.and(Filters.eq("region", "West"), Filters.gt("employee_count", 50), Filters.ne("status", "inactive")));

        Any any = Filters.any(complexSubQuery);

        var params = any.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("West"));
        Assertions.assertTrue(params.contains(50));
        Assertions.assertTrue(params.contains("inactive"));
    }

    @Test
    public void testUsageScenarios() {
        // Test = ANY (equivalent to IN)
        SubQuery managerIds = Filters.subQuery("SELECT id FROM employees WHERE is_manager = true");
        Any anyManager = Filters.any(managerIds);

        // This would be used like: employee_id = ANY (subquery)
        Assertions.assertNotNull(anyManager);

        // Test > ANY (greater than at least one)
        SubQuery juniorSalaries = Filters.subQuery("SELECT salary FROM employees WHERE level = 'junior'");
        Any anyJuniorSalary = Filters.any(juniorSalaries);

        // This would be used like: salary > ANY (subquery)
        Assertions.assertNotNull(anyJuniorSalary);
    }
}
