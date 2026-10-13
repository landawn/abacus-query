package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class SomeTest extends TestBase {

    @Test
    void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("category", "Budget");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = Filters.some(subQuery);

        assertNotNull(some);
        assertEquals(Operator.SOME, some.operator());
        assertEquals(subQuery, some.condition());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        Some some1 = Filters.some(subQuery1);
        Some some2 = Filters.some(subQuery2);
        Some some3 = Filters.some(subQuery3);

        assertTrue(some1.equals(some1));
        assertTrue(some1.equals(some2));
        assertFalse(some1.equals(some3));
        assertFalse(some1.equals(null));
        assertFalse(some1.equals("not a Some"));
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Some condition = new Some(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        SubQuery retrieved = (SubQuery) condition.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        Some condition = new Some(subQuery);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    void testGreaterThanSomeScenario() {
        // salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertTrue(condition instanceof ComposableCell);
        assertTrue(condition instanceof ComposableCondition);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testLessThanSomeScenario() {
        // price < SOME (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testManagerSalaryCheck() {
        // Find employees earning more than SOME managers
        Condition whereCondition = new Equal("is_manager", true);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testNullSubQueryMessageNamesSubQuery() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Some(null));
        assertTrue(e.getMessage().contains("subQuery"), e.getMessage());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.eq("category", "electronics"));
        Some some = Filters.some(subQuery);

        assertEquals(subQuery.parameters(), some.parameters());
        assertEquals(1, some.parameters().size());
        assertEquals("electronics", some.parameters().get(0));
    }

    @Test
    void testParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Some condition = new Some(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("approved", true);
        SubQuery subQuery = Filters.subQuery("requests", Arrays.asList("amount"), whereCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    void testParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM config");
        Some some = Filters.some(subQuery);

        assertTrue(some.parameters().isEmpty());
    }

    @Test
    void testPracticalExample1() {
        // Find employees earning more than SOME managers
        SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = Filters.some(managerSalaries);

        // Would be used like: salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        assertEquals(Operator.SOME, some.operator());
        assertEquals(managerSalaries, some.condition());
    }

    @Test
    void testPracticalExample2() {
        // Find products cheaper than SOME competitor products
        SubQuery competitorPrices = Filters.subQuery("competitor_products", Arrays.asList("price"), Filters.eq("category", "electronics"));
        Some some = Filters.some(competitorPrices);

        // Would be used like: price < SOME (SELECT price FROM competitor_products WHERE category = 'electronics')
        assertEquals(1, some.parameters().size());
        assertEquals("electronics", some.parameters().get(0));
    }

    @Test
    void testPracticalExample3() {
        // Find projects that cost less than SOME department's budget
        SubQuery deptBudgets = Filters.subQuery("departments", Arrays.asList("budget"),
                Filters.and(Filters.eq("active", true), Filters.gt("fiscal_year", 2023)));
        Some some = Filters.some(deptBudgets);

        // Would be used like: project_cost < SOME (SELECT budget FROM departments WHERE active = true AND fiscal_year > 2023)
        assertEquals(2, some.parameters().size());
    }

    @Test
    void testProjectCostComparison() {
        // project_cost < SOME (SELECT budget FROM departments)
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
    }

    @Test
    void testRejectsStandalonePredicateUse() {
        Some condition = new Some(Filters.subQuery("SELECT score FROM tests"));

        assertThrows(IllegalArgumentException.class, () -> new Where(condition));
        assertThrows(IllegalArgumentException.class, () -> condition.and(Filters.eq("active", true)));
    }

    @Test
    void testSomeVsAnyEquivalence() {
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
    void testStructuredSubQueryMustProjectOneColumn() {
        SubQuery scalar = Filters.subQuery("employees", Arrays.asList("salary"), (Condition) null);
        assertEquals(scalar, new Some(scalar).subQuery());

        SubQuery rowValue = Filters.subQuery("employees", Arrays.asList("salary", "bonus"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new Some(rowValue));

        // Raw SQL projection arity is intentionally unknown and remains accepted.
        assertNotNull(new Some(Filters.subQuery("SELECT salary, bonus FROM employees")));
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT level FROM requirements");
        Some some = Filters.some(subQuery);

        String result = some.toString();
        assertEquals("SOME (SELECT level FROM requirements)", result);
    }

    @Test
    void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("experience", 5);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), Filters.and(Filters.eq("active", true), Filters.gt("year", 2023)));
        Some some = Filters.some(subQuery);

        assertEquals(subQuery, some.condition());
        assertEquals(2, some.parameters().size());
    }

    @Test
    void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("available", true), new GreaterThan("stock", 10)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }
}
