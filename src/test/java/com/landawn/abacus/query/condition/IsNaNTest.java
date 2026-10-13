package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNaNTest extends TestBase {

    @Test
    void testAnd() {
        IsNaN condition = new IsNaN("value");
        Condition otherCondition = new Equal("name", "test");
        Condition result = condition.and(otherCondition);
        assertNotNull(result);
        assertTrue(result instanceof Junction);
    }

    @Test
    void testConstructor() {
        IsNaN condition = new IsNaN("value");
        assertNotNull(condition);
        assertEquals("value", condition.propName());
    }

    @Test
    void testConstructor_WithDifferentPropertyNames() {
        IsNaN cond1 = new IsNaN("temperature");
        assertEquals("temperature", cond1.propName());

        IsNaN cond2 = new IsNaN("calculation_result");
        assertEquals("calculation_result", cond2.propName());
    }

    @Test
    void testConstructorWithDifferentPropName() {
        IsNaN condition = new IsNaN("temperature");

        assertNotNull(condition);
        assertEquals("temperature", condition.propName());
        assertEquals(IsNaN.NAN, condition.propValue());
    }

    @Test
    void testConstructorWithPropName() {
        IsNaN condition = new IsNaN("calculation_result");

        assertNotNull(condition);
        assertEquals("calculation_result", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertEquals(IsNaN.NAN, condition.propValue());
    }

    @Test
    void testEquals() {
        IsNaN condition1 = new IsNaN("field1");
        IsNaN condition2 = new IsNaN("field1");
        IsNaN condition3 = new IsNaN("field2");

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        IsNaN condition = new IsNaN("value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentPropertyNames() {
        IsNaN cond1 = new IsNaN("value1");
        IsNaN cond2 = new IsNaN("value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        IsNaN cond1 = new IsNaN("value");
        IsNaN cond2 = new IsNaN("value");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        IsNaN condition = new IsNaN("value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsNaN condition = new IsNaN("value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsNaN condition = new IsNaN("value");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    void testGetPropertyName() {
        IsNaN condition = new IsNaN("score");
        assertEquals("score", condition.propName());
    }

    @Test
    void testHashCode() {
        IsNaN cond1 = new IsNaN("value");
        IsNaN cond2 = new IsNaN("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentProperties() {
        IsNaN cond1 = new IsNaN("value1");
        IsNaN cond2 = new IsNaN("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritedMethods() {
        IsNaN condition = new IsNaN("value");

        // Test methods inherited from Is
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testMultipleInstances() {
        IsNaN condition1 = new IsNaN("calc1");
        IsNaN condition2 = new IsNaN("calc2");

        // Both should share the same NAN constant
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsNaN.NAN, condition1.propValue());
        assertEquals(IsNaN.NAN, condition2.propValue());
    }

    @Test
    void testNANConstant() {
        assertNotNull(IsNaN.NAN);
        // Verify it's an SqlExpression
        assertTrue(IsNaN.NAN.toString().contains("NAN"));
    }

    @Test
    void testNot() {
        IsNaN condition = new IsNaN("value");
        Condition result = condition.not();
        assertNotNull(result);
        assertTrue(result instanceof Not);
    }

    @Test
    void testOr() {
        IsNaN condition = new IsNaN("value");
        Condition otherCondition = new Equal("name", "test");
        Condition result = condition.or(otherCondition);
        assertNotNull(result);
        assertTrue(result instanceof Junction);
    }

    @Test
    void testParameters() {
        IsNaN condition = new IsNaN("value");
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    void testToString() {
        IsNaN condition = new IsNaN("calculation_result");
        String result = condition.toString();

        assertTrue(result.contains("calculation_result"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    void testToString_NoChange() {
        IsNaN condition = new IsNaN("value");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("value"));
        assertTrue(result.contains("IS NAN") || result.contains("IS") && result.contains("NAN"));
    }

    @Test
    void testToString_ScreamingSnakeCase() {
        IsNaN condition = new IsNaN("myValue");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("MY_VALUE"));
    }

    @Test
    void testToString_SnakeCase() {
        IsNaN condition = new IsNaN("myValue");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("my_value"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsNaN condition = new IsNaN("computedValue");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("COMPUTED_VALUE"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    void testUsageExample_CheckTemperature() {
        IsNaN tempCheck = new IsNaN("temperature");
        assertEquals("temperature", tempCheck.propName());
        String sql = tempCheck.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("temperature"));
    }

    @Test
    void testUsageExample_CombinedValidation() {
        IsNaN nanCheck = new IsNaN("score");
        IsNull nullCheck = new IsNull("score");
        Condition combined = nanCheck.or(nullCheck);
        assertNotNull(combined);
        assertTrue(combined instanceof Junction);
    }

    @Test
    void testUsageExample_FindCalculationErrors() {
        IsNaN calcError = new IsNaN("computed_value");
        assertNotNull(calcError);
        assertEquals("computed_value", calcError.propName());
    }

    @Test
    void testUsageExample_ValidateSensorReadings() {
        IsNaN sensorError = new IsNaN("pressure_reading");
        assertNotNull(sensorError);
        assertTrue(sensorError.parameters().isEmpty());
    }
}
