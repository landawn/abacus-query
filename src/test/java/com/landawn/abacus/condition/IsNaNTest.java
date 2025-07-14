package com.landawn.abacus.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNaNTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNaN condition = new IsNaN("calculation_result");
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("calculation_result", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropName() {
        IsNaN condition = new IsNaN("temperature");
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("temperature", condition.getPropName());
        Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        IsNaN condition = new IsNaN("profit_ratio");
        List<Object> params = condition.getParameters();
        
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsNaN condition = new IsNaN("computed_value");
        condition.clearParameters();
        
        // The parameter should be cleared but the structure remains
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testCopy() {
        IsNaN original = new IsNaN("measurement");
        IsNaN copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsNaN condition = new IsNaN("calculation_result");
        String result = condition.toString();
        
        Assertions.assertTrue(result.contains("calculation_result"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("NAN"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNaN condition = new IsNaN("computedValue");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        
        Assertions.assertTrue(result.contains("COMPUTED_VALUE"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("NAN"));
    }

    @Test
    public void testHashCode() {
        IsNaN condition1 = new IsNaN("field1");
        IsNaN condition2 = new IsNaN("field1");
        IsNaN condition3 = new IsNaN("field2");
        
        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNaN condition1 = new IsNaN("field1");
        IsNaN condition2 = new IsNaN("field1");
        IsNaN condition3 = new IsNaN("field2");
        
        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testNANConstant() {
        Assertions.assertNotNull(IsNaN.NAN);
        // Verify it's an Expression
        Assertions.assertTrue(IsNaN.NAN.toString().contains("NAN"));
    }

    @Test
    public void testInheritedMethods() {
        IsNaN condition = new IsNaN("value");
        
        // Test methods inherited from Is
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testMultipleInstances() {
        IsNaN condition1 = new IsNaN("calc1");
        IsNaN condition2 = new IsNaN("calc2");
        
        // Both should share the same NAN constant
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsNaN.NAN, condition1.getPropValue());
        Assertions.assertEquals(IsNaN.NAN, condition2.getPropValue());
    }
}