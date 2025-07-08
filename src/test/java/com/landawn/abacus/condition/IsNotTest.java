package com.landawn.abacus.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class IsNotTest extends TestBase {

    @Test
    public void testConstructorWithPropNameAndValue() {
        IsNot condition = new IsNot("age", null);
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertNull(condition.getPropValue());
    }

    @Test
    public void testConstructorWithExpression() {
        IsNot condition = new IsNot("status", CF.expr("ACTIVE"));
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        IsNot condition = new IsNot("name", "value");
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("value", params.get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        IsNot condition = new IsNot("name", null);
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testClearParameters() {
        IsNot condition = new IsNot("name", "value");
        condition.clearParameters();
        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testCopy() {
        IsNot original = new IsNot("age", 25);
        IsNot copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsNot condition = new IsNot("status", null);
        String result = condition.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNot condition = new IsNot("firstName", null);
        String result = condition.toString(NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertTrue(result.contains("firstName"));
        Assertions.assertTrue(result.contains("IS NOT"));
    }

    @Test
    public void testHashCode() {
        IsNot condition1 = new IsNot("name", "value");
        IsNot condition2 = new IsNot("name", "value");
        IsNot condition3 = new IsNot("other", "value");
        
        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNot condition1 = new IsNot("name", "value");
        IsNot condition2 = new IsNot("name", "value");
        IsNot condition3 = new IsNot("other", "value");
        IsNot condition4 = new IsNot("name", "other");
        
        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }
}