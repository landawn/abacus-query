package com.landawn.abacus.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class AnyTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any any = ConditionFactory.any(subQuery);
        
        Assertions.assertNotNull(any);
        Assertions.assertEquals(Operator.ANY, any.getOperator());
        Assertions.assertEquals(subQuery, any.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Any any = ConditionFactory.any(subQuery);
        
        SubQuery retrieved = any.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT budget FROM departments");
        Any any = ConditionFactory.any(subQuery);
        
        String result = any.toString();
        Assertions.assertTrue(result.contains("ANY"));
        Assertions.assertTrue(result.contains("SELECT budget FROM departments"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT departmentId FROM employees");
        Any any = ConditionFactory.any(subQuery);
        
        // Naming policy should be applied to the subquery
        String result = any.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("ANY"));
    }

    @Test
    public void testGetParameters() {
        // Create a subquery with parameters
        SubQuery subQuery = ConditionFactory.subQuery("products", 
            Arrays.asList("price"), 
            ConditionFactory.eq("category", "Electronics"));
        Any any = ConditionFactory.any(subQuery);
        
        var params = any.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("Electronics", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("products", 
            Arrays.asList("price"), 
            ConditionFactory.between("price", 100, 500));
        Any any = ConditionFactory.any(subQuery);
        
        Assertions.assertEquals(2, any.getParameters().size());
        
        any.clearParameters();
        
        Assertions.assertTrue(any.getParameters().isEmpty());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM users WHERE active = true");
        Any original = ConditionFactory.any(subQuery);
        
        Any copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT price FROM products");
        SubQuery subQuery3 = ConditionFactory.subQuery("SELECT cost FROM products");
        
        Any any1 = ConditionFactory.any(subQuery1);
        Any any2 = ConditionFactory.any(subQuery2);
        Any any3 = ConditionFactory.any(subQuery3);
        
        Assertions.assertEquals(any1, any1);
        Assertions.assertEquals(any1, any2);
        Assertions.assertNotEquals(any1, any3);
        Assertions.assertNotEquals(any1, null);
        Assertions.assertNotEquals(any1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT level FROM grades");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT level FROM grades");
        
        Any any1 = ConditionFactory.any(subQuery1);
        Any any2 = ConditionFactory.any(subQuery2);
        
        Assertions.assertEquals(any1.hashCode(), any2.hashCode());
    }

    @Test
    public void testAnd() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT min_salary FROM positions");
        Any any = ConditionFactory.any(subQuery);
        Equal eq = ConditionFactory.eq("department", "Sales");
        
        And and = any.and(eq);
        
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(any));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT threshold FROM limits");
        Any any = ConditionFactory.any(subQuery);
        GreaterThan gt = ConditionFactory.gt("priority", 5);
        
        Or or = any.or(gt);
        
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT restricted_id FROM blacklist");
        Any any = ConditionFactory.any(subQuery);
        
        Not not = any.not();
        
        Assertions.assertNotNull(not);
        Assertions.assertEquals(any, not.getCondition());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT id FROM table2");
        Any any = ConditionFactory.any(subQuery1);
        
        Assertions.assertEquals(subQuery1, any.getCondition());
        
        any.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, any.getCondition());
    }

    @Test
    public void testComplexSubQuery() {
        // Test with a complex subquery
        SubQuery complexSubQuery = ConditionFactory.subQuery("departments", 
            Arrays.asList("budget"), 
            ConditionFactory.and(
                ConditionFactory.eq("region", "West"),
                ConditionFactory.gt("employee_count", 50),
                ConditionFactory.ne("status", "inactive")
            )
        );
        
        Any any = ConditionFactory.any(complexSubQuery);
        
        var params = any.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("West"));
        Assertions.assertTrue(params.contains(50));
        Assertions.assertTrue(params.contains("inactive"));
    }

    @Test
    public void testUsageScenarios() {
        // Test = ANY (equivalent to IN)
        SubQuery managerIds = ConditionFactory.subQuery("SELECT id FROM employees WHERE is_manager = true");
        Any anyManager = ConditionFactory.any(managerIds);
        
        // This would be used like: employee_id = ANY (subquery)
        Assertions.assertNotNull(anyManager);
        
        // Test > ANY (greater than at least one)
        SubQuery juniorSalaries = ConditionFactory.subQuery("SELECT salary FROM employees WHERE level = 'junior'");
        Any anyJuniorSalary = ConditionFactory.any(juniorSalaries);
        
        // This would be used like: salary > ANY (subquery)
        Assertions.assertNotNull(anyJuniorSalary);
    }
}