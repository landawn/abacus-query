package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.IsInfinite;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.util.NamingPolicy;

public class IsInfiniteTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsInfinite condition = new IsInfinite("growth_rate");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("growth_rate", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "calculated_ratio", "interest_rate", "computed_value", "division_result" };

        for (String propName : propNames) {
            IsInfinite condition = new IsInfinite(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS, condition.getOperator());
            Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());
        }
    }

    @Test
    public void testGetParameters() {
        IsInfinite condition = new IsInfinite("ratio");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsInfinite condition = new IsInfinite("value");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testCopy() {
        IsInfinite original = new IsInfinite("measurement");
        IsInfinite copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsInfinite condition = new IsInfinite("calculated_ratio");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("calculated_ratio"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsInfinite condition = new IsInfinite("interestRate");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("INTEREST_RATE"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testHashCode() {
        IsInfinite condition1 = new IsInfinite("field1");
        IsInfinite condition2 = new IsInfinite("field1");
        IsInfinite condition3 = new IsInfinite("field2");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsInfinite condition1 = new IsInfinite("field1");
        IsInfinite condition2 = new IsInfinite("field1");
        IsInfinite condition3 = new IsInfinite("field2");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testINFINITEConstant() {
        Assertions.assertNotNull(IsInfinite.INFINITE);
        // Verify it's an Expression
        Assertions.assertTrue(IsInfinite.INFINITE.toString().contains("INFINITE"));
    }

    @Test
    public void testInheritedMethods() {
        IsInfinite condition = new IsInfinite("value");

        // Test methods inherited from Is
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testMultipleInstances() {
        IsInfinite condition1 = new IsInfinite("calc1");
        IsInfinite condition2 = new IsInfinite("calc2");

        // Both should share the same INFINITE constant
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsInfinite.INFINITE, condition1.getPropValue());
        Assertions.assertEquals(IsInfinite.INFINITE, condition2.getPropValue());
    }

    @Test
    public void testCommonInfinityScenarios() {
        // Test common scenarios that might produce infinity
        IsInfinite divisionByZero = new IsInfinite("division_result");
        IsInfinite overflow = new IsInfinite("overflow_value");
        IsInfinite logarithm = new IsInfinite("log_result");

        String result1 = divisionByZero.toString();
        String result2 = overflow.toString();
        String result3 = logarithm.toString();

        Assertions.assertTrue(result1.contains("division_result IS INFINITE"));
        Assertions.assertTrue(result2.contains("overflow_value IS INFINITE"));
        Assertions.assertTrue(result3.contains("log_result IS INFINITE"));
    }

    @Test
    public void testNumericFieldNames() {
        // Test with various numeric field names
        String[] numericFields = { "price_ratio", "growth_rate", "interest_rate", "profit_margin", "conversion_rate", "performance_metric" };

        for (String field : numericFields) {
            IsInfinite condition = new IsInfinite(field);

            Assertions.assertEquals(field, condition.getPropName());
            Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());

            String result = condition.toString();
            Assertions.assertTrue(result.contains(field + " IS INFINITE"));
        }
    }
}