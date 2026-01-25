package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class IsTest extends TestBase {

    @Test
    public void testConstructorWithPropNameAndValue() {
        Is condition = new Is("age", null);
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertNull(condition.getPropValue());
    }

    @Test
    public void testConstructorWithExpression() {
        Is condition = new Is("status", Filters.expr("ACTIVE"));
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        Is condition = new Is("name", "value");
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("value", params.get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        Is condition = new Is("name", null);
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testClearParameters() {
        Is condition = new Is("name", "value");
        condition.clearParameters();
        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testCopy() {
        Is original = new Is("age", 25);
        Is copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        Is condition = new Is("status", null);
        String result = condition.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Is condition = new Is("firstName", null);
        String result = condition.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertTrue(result.contains("firstName"));
        Assertions.assertTrue(result.contains("IS"));
    }

    @Test
    public void testHashCode() {
        Is condition1 = new Is("name", "value");
        Is condition2 = new Is("name", "value");
        Is condition3 = new Is("other", "value");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        Is condition1 = new Is("name", "value");
        Is condition2 = new Is("name", "value");
        Is condition3 = new Is("other", "value");
        Is condition4 = new Is("name", "other");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }
}