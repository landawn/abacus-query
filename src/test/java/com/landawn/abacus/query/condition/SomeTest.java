package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Some;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Filters.CF;

public class SomeTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = CF.some(subQuery);

        Assertions.assertNotNull(some);
        Assertions.assertEquals(Operator.SOME, some.getOperator());
        Assertions.assertEquals(subQuery, some.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = CF.subQuery("SELECT price FROM competitor_products");
        Some some = CF.some(subQuery);

        Assertions.assertEquals(subQuery, some.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM test");
        Some some = CF.some(subQuery);

        Assertions.assertEquals(Operator.SOME, some.getOperator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = CF.subQuery("departments", Arrays.asList("budget"), CF.and(CF.eq("active", true), CF.gt("year", 2023)));
        Some some = CF.some(subQuery);

        Assertions.assertEquals(subQuery, some.getCondition());
        Assertions.assertEquals(2, some.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = CF.subQuery("products", Arrays.asList("price"), CF.eq("category", "electronics"));
        Some some = CF.some(subQuery);

        Assertions.assertEquals(subQuery.getParameters(), some.getParameters());
        Assertions.assertEquals(1, some.getParameters().size());
        Assertions.assertEquals("electronics", some.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT value FROM config");
        Some some = CF.some(subQuery);

        Assertions.assertTrue(some.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = CF.subQuery("scores", Arrays.asList("value"), CF.between("date", "2023-01-01", "2023-12-31"));
        Some some = CF.some(subQuery);

        some.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = CF.subQuery("SELECT level FROM requirements");
        Some some = CF.some(subQuery);

        String result = some.toString();
        Assertions.assertTrue(result.contains("SOME"));
        Assertions.assertTrue(result.contains("SELECT level FROM requirements"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = CF.subQuery("salaries", Arrays.asList("amount"), CF.eq("department", "IT"));
        Some original = CF.some(subQuery);

        Some copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM other");

        Some some1 = CF.some(subQuery1);
        Some some2 = CF.some(subQuery2);
        Some some3 = CF.some(subQuery3);

        Assertions.assertEquals(some1.hashCode(), some2.hashCode());
        Assertions.assertNotEquals(some1.hashCode(), some3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM other");

        Some some1 = CF.some(subQuery1);
        Some some2 = CF.some(subQuery2);
        Some some3 = CF.some(subQuery3);

        Assertions.assertTrue(some1.equals(some1));
        Assertions.assertTrue(some1.equals(some2));
        Assertions.assertFalse(some1.equals(some3));
        Assertions.assertFalse(some1.equals(null));
        Assertions.assertFalse(some1.equals("not a Some"));
    }

    @Test
    public void testPracticalExample1() {
        // Find employees earning more than SOME managers
        SubQuery managerSalaries = CF.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some some = CF.some(managerSalaries);

        // Would be used like: salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        Assertions.assertEquals(Operator.SOME, some.getOperator());
        Assertions.assertEquals(managerSalaries, some.getCondition());
    }

    @Test
    public void testPracticalExample2() {
        // Find products cheaper than SOME competitor products
        SubQuery competitorPrices = CF.subQuery("competitor_products", Arrays.asList("price"), CF.eq("category", "electronics"));
        Some some = CF.some(competitorPrices);

        // Would be used like: price < SOME (SELECT price FROM competitor_products WHERE category = 'electronics')
        Assertions.assertEquals(1, some.getParameters().size());
        Assertions.assertEquals("electronics", some.getParameters().get(0));
    }

    @Test
    public void testPracticalExample3() {
        // Find projects that cost less than SOME department's budget
        SubQuery deptBudgets = CF.subQuery("departments", Arrays.asList("budget"), CF.and(CF.eq("active", true), CF.gt("fiscal_year", 2023)));
        Some some = CF.some(deptBudgets);

        // Would be used like: project_cost < SOME (SELECT budget FROM departments WHERE active = true AND fiscal_year > 2023)
        Assertions.assertEquals(2, some.getParameters().size());
    }
}