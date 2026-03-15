package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Some2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("category", "Budget");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        Some condition = new Some(subQuery);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("approved", true);
        SubQuery subQuery = Filters.subQuery("requests", Arrays.asList("amount"), whereCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("experience", (Object) 5);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Some condition = new Some(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertTrue(condition instanceof ComposableCell);
        assertTrue(condition instanceof ComposableCondition);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanSomeScenario() {
        // salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    public void testLessThanSomeScenario() {
        // price < SOME (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testSomeVsAnyEquivalence() {
        // SOME and ANY are functionally equivalent
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");

        Some someCondition = new Some(subQuery1);
        Any anyCondition = new Any(subQuery2);

        // Both should work the same way
        assertNotNull(someCondition);
        assertNotNull(anyCondition);
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("available", true), new GreaterThan("stock", (Object) 10)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testProjectCostComparison() {
        // project_cost < SOME (SELECT budget FROM departments)
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
    }

    @Test
    public void testManagerSalaryCheck() {
        // Find employees earning more than SOME managers
        Condition whereCondition = new Equal("is_manager", true);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

}

public class SomeTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = Filters.some(subQuery);

        Assertions.assertNotNull(some);
        Assertions.assertEquals(Operator.SOME, some.operator());
        Assertions.assertEquals(subQuery, some.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        Some some = Filters.some(subQuery);

        Assertions.assertEquals(subQuery, some.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM test");
        Some some = Filters.some(subQuery);

        Assertions.assertEquals(Operator.SOME, some.operator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), Filters.and(Filters.eq("active", true), Filters.gt("year", 2023)));
        Some some = Filters.some(subQuery);

        Assertions.assertEquals(subQuery, some.getCondition());
        Assertions.assertEquals(2, some.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.eq("category", "electronics"));
        Some some = Filters.some(subQuery);

        Assertions.assertEquals(subQuery.getParameters(), some.getParameters());
        Assertions.assertEquals(1, some.getParameters().size());
        Assertions.assertEquals("electronics", some.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM config");
        Some some = Filters.some(subQuery);

        Assertions.assertTrue(some.getParameters().isEmpty());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT level FROM requirements");
        Some some = Filters.some(subQuery);

        String result = some.toString();
        Assertions.assertEquals("SOME (SELECT level FROM requirements)", result);
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        Some some1 = Filters.some(subQuery1);
        Some some2 = Filters.some(subQuery2);
        Some some3 = Filters.some(subQuery3);

        Assertions.assertEquals(some1.hashCode(), some2.hashCode());
        Assertions.assertNotEquals(some1.hashCode(), some3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        Some some1 = Filters.some(subQuery1);
        Some some2 = Filters.some(subQuery2);
        Some some3 = Filters.some(subQuery3);

        Assertions.assertTrue(some1.equals(some1));
        Assertions.assertTrue(some1.equals(some2));
        Assertions.assertFalse(some1.equals(some3));
        Assertions.assertFalse(some1.equals(null));
        Assertions.assertFalse(some1.equals("not a Some"));
    }

    @Test
    public void testPracticalExample1() {
        // Find employees earning more than SOME managers
        SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = Filters.some(managerSalaries);

        // Would be used like: salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        Assertions.assertEquals(Operator.SOME, some.operator());
        Assertions.assertEquals(managerSalaries, some.getCondition());
    }

    @Test
    public void testPracticalExample2() {
        // Find products cheaper than SOME competitor products
        SubQuery competitorPrices = Filters.subQuery("competitor_products", Arrays.asList("price"), Filters.eq("category", "electronics"));
        Some some = Filters.some(competitorPrices);

        // Would be used like: price < SOME (SELECT price FROM competitor_products WHERE category = 'electronics')
        Assertions.assertEquals(1, some.getParameters().size());
        Assertions.assertEquals("electronics", some.getParameters().get(0));
    }

    @Test
    public void testPracticalExample3() {
        // Find projects that cost less than SOME department's budget
        SubQuery deptBudgets = Filters.subQuery("departments", Arrays.asList("budget"),
                Filters.and(Filters.eq("active", true), Filters.gt("fiscal_year", 2023)));
        Some some = Filters.some(deptBudgets);

        // Would be used like: project_cost < SOME (SELECT budget FROM departments WHERE active = true AND fiscal_year > 2023)
        Assertions.assertEquals(2, some.getParameters().size());
    }
}
