package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class SomeTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = Filters.some(subQuery);

        Assertions.assertNotNull(some);
        Assertions.assertEquals(Operator.SOME, some.getOperator());
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

        Assertions.assertEquals(Operator.SOME, some.getOperator());
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
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("scores", Arrays.asList("value"), Filters.between("date", "2023-01-01", "2023-12-31"));
        Some some = Filters.some(subQuery);

        some.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT level FROM requirements");
        Some some = Filters.some(subQuery);

        String result = some.toString();
        Assertions.assertTrue(result.contains("SOME"));
        Assertions.assertTrue(result.contains("SELECT level FROM requirements"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("salaries", Arrays.asList("amount"), Filters.eq("department", "IT"));
        Some original = Filters.some(subQuery);

        Some copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
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
        Assertions.assertEquals(Operator.SOME, some.getOperator());
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