package com.landawn.abacus.condition;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class GreaterThanTest extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThan gt = ConditionFactory.gt("age", 18);
        
        Assertions.assertNotNull(gt);
        Assertions.assertEquals("age", gt.getPropName());
        Assertions.assertEquals(18, (Integer)gt.getPropValue());
        Assertions.assertEquals(Operator.GREATER_THAN, gt.getOperator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Double
        GreaterThan gtDouble = ConditionFactory.gt("price", 99.99);
        Assertions.assertEquals(99.99, gtDouble.getPropValue());
        
        // Test with Long
        GreaterThan gtLong = ConditionFactory.gt("count", 1000000L);
        Assertions.assertEquals(1000000L, (Long) gtLong.getPropValue());
        
        // Test with Date
        Date now = new Date();
        GreaterThan gtDate = ConditionFactory.gt("createdDate", now);
        Assertions.assertEquals(now, gtDate.getPropValue());
        
        // Test with String (for alphabetical comparison)
        GreaterThan gtString = ConditionFactory.gt("name", "M");
        Assertions.assertEquals("M", gtString.getPropValue());
    }

    @Test
    public void testConstructorWithNull() {
        GreaterThan gt = ConditionFactory.gt("value", null);
        Assertions.assertNotNull(gt);
        Assertions.assertNull(gt.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan("", 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan(null, 10);
        });
    }

    @Test
    public void testToString() {
        GreaterThan gt = ConditionFactory.gt("salary", 50000);
        String result = gt.toString();
        Assertions.assertEquals("salary > 50000", result);
    }

    @Test
    public void testToStringWithString() {
        GreaterThan gt = ConditionFactory.gt("grade", "B");
        String result = gt.toString();
        Assertions.assertEquals("grade > 'B'", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GreaterThan gt = ConditionFactory.gt("yearOfBirth", 1990);
        String result = gt.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("year_of_birth > 1990", result);
    }

    @Test
    public void testGetParameters() {
        GreaterThan gt = ConditionFactory.gt("temperature", 32.5);
        var params = gt.getParameters();
        
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(32.5, params.get(0));
    }

    @Test
    public void testClearParameters() {
        GreaterThan gt = ConditionFactory.gt("score", 85);
        Assertions.assertEquals(85, (Integer) gt.getPropValue());
        
        gt.clearParameters();
        Assertions.assertNull(gt.getPropValue());
    }

    @Test
    public void testCopy() {
        GreaterThan original = ConditionFactory.gt("level", 5);
        GreaterThan copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testEquals() {
        GreaterThan gt1 = ConditionFactory.gt("age", 21);
        GreaterThan gt2 = ConditionFactory.gt("age", 21);
        GreaterThan gt3 = ConditionFactory.gt("age", 18);
        GreaterThan gt4 = ConditionFactory.gt("height", 21);
        
        Assertions.assertEquals(gt1, gt1);
        Assertions.assertEquals(gt1, gt2);
        Assertions.assertNotEquals(gt1, gt3); // Different value
        Assertions.assertNotEquals(gt1, gt4); // Different property
        Assertions.assertNotEquals(gt1, null);
        Assertions.assertNotEquals(gt1, "string");
    }

    @Test
    public void testHashCode() {
        GreaterThan gt1 = ConditionFactory.gt("quantity", 100);
        GreaterThan gt2 = ConditionFactory.gt("quantity", 100);
        
        Assertions.assertEquals(gt1.hashCode(), gt2.hashCode());
    }

    @Test
    public void testAnd() {
        GreaterThan gt = ConditionFactory.gt("age", 18);
        LessThan lt = ConditionFactory.lt("age", 65);
        
        And and = gt.and(lt);
        
        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.getOperator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(gt));
        Assertions.assertTrue(and.getConditions().contains(lt));
    }

    @Test
    public void testOr() {
        GreaterThan gt = ConditionFactory.gt("score", 90);
        Equal eq = ConditionFactory.eq("grade", "A");
        
        Or or = gt.or(eq);
        
        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThan gt = ConditionFactory.gt("balance", 0);
        
        Not not = gt.not();
        
        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.getOperator());
        Assertions.assertEquals(gt, not.getCondition());
    }

    @Test
    public void testWithSubQuery() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT AVG(salary) FROM employees");
        GreaterThan gt = new GreaterThan("salary", subQuery);
        
        Assertions.assertEquals(subQuery, gt.getPropValue());
        
        String result = gt.toString();
        Assertions.assertTrue(result.contains("salary >"));
        Assertions.assertTrue(result.contains("SELECT AVG(salary) FROM employees"));
    }

    @Test
    public void testSetPropValue() {
        GreaterThan gt = ConditionFactory.gt("price", 100);
        Assertions.assertEquals(100, (Integer) gt.getPropValue());
        
        gt.setPropValue(200);
        Assertions.assertEquals(200, (Integer) gt.getPropValue());
    }

    @Test
    public void testComplexComparison() {
        // Test chaining multiple conditions
        GreaterThan salary = ConditionFactory.gt("salary", 50000);
        GreaterEqual experience = ConditionFactory.ge("yearsExperience", 5);
        LessThan age = ConditionFactory.lt("age", 50);
        
        And qualified = salary.and(experience).and(age);
        
        Assertions.assertNotNull(qualified);
        Assertions.assertEquals(3, qualified.getConditions().size());
    }
}