package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class IsNaN2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNaN condition = new IsNaN("value");
        assertNotNull(condition);
        assertEquals("value", condition.getPropName());
    }

    @Test
    public void testConstructor_WithDifferentPropertyNames() {
        IsNaN cond1 = new IsNaN("temperature");
        assertEquals("temperature", cond1.getPropName());

        IsNaN cond2 = new IsNaN("calculation_result");
        assertEquals("calculation_result", cond2.getPropName());
    }

    @Test
    public void testGetOperator() {
        IsNaN condition = new IsNaN("value");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testGetPropertyName() {
        IsNaN condition = new IsNaN("score");
        assertEquals("score", condition.getPropName());
    }

    @Test
    public void testGetParameters() {
        IsNaN condition = new IsNaN("value");
        assertTrue(condition.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        IsNaN condition = new IsNaN("value");
        condition.clearParameters();
        assertTrue(condition.getParameters().isEmpty());
    }

    @Test
    public void testToString_NoChange() {
        IsNaN condition = new IsNaN("value");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("value"));
        assertTrue(result.contains("IS NAN") || result.contains("IS") && result.contains("NAN"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNaN condition = new IsNaN("myValue");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("my_value"));
    }

    @Test
    public void testToString_ScreamingSnakeCase() {
        IsNaN condition = new IsNaN("myValue");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("MY_VALUE"));
    }

    @Test
    public void testHashCode() {
        IsNaN cond1 = new IsNaN("value");
        IsNaN cond2 = new IsNaN("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentProperties() {
        IsNaN cond1 = new IsNaN("value1");
        IsNaN cond2 = new IsNaN("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsNaN condition = new IsNaN("value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsNaN cond1 = new IsNaN("value");
        IsNaN cond2 = new IsNaN("value");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropertyNames() {
        IsNaN cond1 = new IsNaN("value1");
        IsNaN cond2 = new IsNaN("value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsNaN condition = new IsNaN("value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNaN condition = new IsNaN("value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsNaN condition = new IsNaN("value");
        Condition otherCondition = new Equal("name", "test");
        Condition result = condition.and(otherCondition);
        assertNotNull(result);
        assertTrue(result instanceof Junction);
    }

    @Test
    public void testOr() {
        IsNaN condition = new IsNaN("value");
        Condition otherCondition = new Equal("name", "test");
        Condition result = condition.or(otherCondition);
        assertNotNull(result);
        assertTrue(result instanceof Junction);
    }

    @Test
    public void testNot() {
        IsNaN condition = new IsNaN("value");
        Condition result = condition.not();
        assertNotNull(result);
        assertTrue(result instanceof Not);
    }

    @Test
    public void testUsageExample_CheckTemperature() {
        IsNaN tempCheck = new IsNaN("temperature");
        assertEquals("temperature", tempCheck.getPropName());
        String sql = tempCheck.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("temperature"));
    }

    @Test
    public void testUsageExample_FindCalculationErrors() {
        IsNaN calcError = new IsNaN("computed_value");
        assertNotNull(calcError);
        assertEquals("computed_value", calcError.getPropName());
    }

    @Test
    public void testUsageExample_ValidateSensorReadings() {
        IsNaN sensorError = new IsNaN("pressure_reading");
        assertNotNull(sensorError);
        assertTrue(sensorError.getParameters().isEmpty());
    }

    @Test
    public void testUsageExample_CombinedValidation() {
        IsNaN nanCheck = new IsNaN("score");
        IsNull nullCheck = new IsNull("score");
        Condition combined = nanCheck.or(nullCheck);
        assertNotNull(combined);
        assertTrue(combined instanceof Junction);
    }
}

public class IsNaNTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNaN condition = new IsNaN("calculation_result");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("calculation_result", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
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
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

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
        Assertions.assertEquals(Operator.IS, condition.operator());
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
