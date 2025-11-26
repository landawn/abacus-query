package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNotInfiniteTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNotInfinite condition = new IsNotInfinite("price_ratio");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("price_ratio", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "sensor_reading", "growth_rate", "calculation_result", "measurement" };

        for (String propName : propNames) {
            IsNotInfinite condition = new IsNotInfinite(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
            Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());
        }
    }

    @Test
    public void testGetParameters() {
        IsNotInfinite condition = new IsNotInfinite("ratio");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsNotInfinite condition = new IsNotInfinite("value");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testCopy() {
        IsNotInfinite original = new IsNotInfinite("calculation");
        IsNotInfinite copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsNotInfinite condition = new IsNotInfinite("calculation_result");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("calculation_result"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("GROWTH_RATE"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testHashCode() {
        IsNotInfinite condition1 = new IsNotInfinite("field1");
        IsNotInfinite condition2 = new IsNotInfinite("field1");
        IsNotInfinite condition3 = new IsNotInfinite("field2");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNotInfinite condition1 = new IsNotInfinite("field1");
        IsNotInfinite condition2 = new IsNotInfinite("field1");
        IsNotInfinite condition3 = new IsNotInfinite("field2");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testInheritedMethods() {
        IsNotInfinite condition = new IsNotInfinite("value");

        // Test methods inherited from IsNot
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testSharedINFINITEConstant() {
        IsNotInfinite condition1 = new IsNotInfinite("calc1");
        IsNotInfinite condition2 = new IsNotInfinite("calc2");

        // Both should use the same INFINITE constant from IsInfinite
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsInfinite.INFINITE, condition1.getPropValue());
        Assertions.assertEquals(IsInfinite.INFINITE, condition2.getPropValue());
    }

    @Test
    public void testCommonUseCases() {
        // Test common numeric fields that might be checked for infinity
        String[] numericFields = { "price_ratio", "growth_rate", "division_result", "interest_rate", "profit_margin", "sensor_value" };

        for (String field : numericFields) {
            IsNotInfinite condition = new IsNotInfinite(field);
            String result = condition.toString();

            Assertions.assertTrue(result.contains(field));
            Assertions.assertTrue(result.contains("IS NOT INFINITE"));
        }
    }

    @Test
    public void testINFINITEConstant() {
        // Verify the INFINITE constant is properly accessible
        Assertions.assertNotNull(IsInfinite.INFINITE);
        Assertions.assertTrue(IsInfinite.INFINITE.toString().contains("INFINITE"));
    }
}