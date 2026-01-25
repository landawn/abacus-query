package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNotNaNTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNotNaN condition = new IsNotNaN("calculation_result");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("calculation_result", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropName() {
        IsNotNaN condition = new IsNotNaN("temperature");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("temperature", condition.getPropName());
        Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        IsNotNaN condition = new IsNotNaN("profit_ratio");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsNotNaN condition = new IsNotNaN("computed_value");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testCopy() {
        IsNotNaN original = new IsNotNaN("measurement");
        IsNotNaN copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsNotNaN condition = new IsNotNaN("calculation_result");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("calculation_result"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NAN"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNotNaN condition = new IsNotNaN("computedValue");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("COMPUTED_VALUE"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NAN"));
    }

    @Test
    public void testHashCode() {
        IsNotNaN condition1 = new IsNotNaN("field1");
        IsNotNaN condition2 = new IsNotNaN("field1");
        IsNotNaN condition3 = new IsNotNaN("field2");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNotNaN condition1 = new IsNotNaN("field1");
        IsNotNaN condition2 = new IsNotNaN("field1");
        IsNotNaN condition3 = new IsNotNaN("field2");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testInheritedMethods() {
        IsNotNaN condition = new IsNotNaN("value");

        // Test methods inherited from IsNot
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testSharedNANConstant() {
        IsNotNaN condition1 = new IsNotNaN("calc1");
        IsNotNaN condition2 = new IsNotNaN("calc2");

        // Both should use the same NAN constant from IsNaN
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsNaN.NAN, condition1.getPropValue());
        Assertions.assertEquals(IsNaN.NAN, condition2.getPropValue());
    }

    @Test
    public void testWithVariousPropNames() {
        String[] propNames = { "temperature", "ratio", "calculated_value", "sensor_reading", "percentage" };

        for (String propName : propNames) {
            IsNotNaN condition = new IsNotNaN(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
            Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());

            String result = condition.toString();
            Assertions.assertTrue(result.contains(propName));
            Assertions.assertTrue(result.contains("IS NOT"));
            Assertions.assertTrue(result.contains("NAN"));
        }
    }
}