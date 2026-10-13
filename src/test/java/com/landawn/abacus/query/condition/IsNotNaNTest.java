package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNotNaNTest extends TestBase {

    @Test
    void testAnd() {
        IsNotNaN cond1 = new IsNotNaN("temp1");
        IsNotNaN cond2 = new IsNotNaN("temp2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testComplexPropertyName() {
        IsNotNaN condition = new IsNotNaN("user.profile.score");
        assertEquals("user.profile.score", condition.propName());
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("user.profile.score"));
    }

    @Test
    void testConstructor() {
        IsNotNaN condition = new IsNotNaN("temperature");
        assertEquals("temperature", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(""));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(null));
    }

    @Test
    void testConstructorWithDifferentPropName() {
        IsNotNaN condition = new IsNotNaN("temperature");

        assertNotNull(condition);
        assertEquals("temperature", condition.propName());
        assertEquals(IsNaN.NAN, condition.propValue());
    }

    @Test
    void testConstructorWithPropName() {
        IsNotNaN condition = new IsNotNaN("calculation_result");

        assertNotNull(condition);
        assertEquals("calculation_result", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertEquals(IsNaN.NAN, condition.propValue());
    }

    @Test
    void testDoublePropertyName() {
        IsNotNaN condition = new IsNotNaN("calculationResult");
        assertEquals("calculationResult", condition.propName());
    }

    @Test
    void testEquals() {
        IsNotNaN condition1 = new IsNotNaN("field1");
        IsNotNaN condition2 = new IsNotNaN("field1");
        IsNotNaN condition3 = new IsNotNaN("field2");

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        IsNotNaN cond1 = new IsNotNaN("temperature");
        IsNotNaN cond2 = new IsNotNaN("temperature");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testGetPropName() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        assertEquals("sensorValue", condition.propName());
    }

    @Test
    void testGetPropName_DifferentNames() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.propName(), cond2.propName());
    }

    @Test
    void testHashCode() {
        IsNotNaN cond1 = new IsNotNaN("value");
        IsNotNaN cond2 = new IsNotNaN("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        IsNotNaN condition = new IsNotNaN("field");
        assertTrue(condition instanceof IsNot);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testInheritedMethods() {
        IsNotNaN condition = new IsNotNaN("value");

        // Test methods inherited from IsNot
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testMultipleInstances() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        IsNotNaN cond3 = new IsNotNaN("field3");

        assertNotEquals(cond1, cond2);
        assertNotEquals(cond2, cond3);
        assertNotEquals(cond1, cond3);
    }

    @Test
    void testNot() {
        IsNotNaN condition = new IsNotNaN("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        IsNotNaN condition = new IsNotNaN("temperature");
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testPropValueAs() {
        IsNotNaN condition = new IsNotNaN("calculatedValue");
        SqlExpression value = condition.propValueAs(SqlExpression.class);
        assertNotNull(value);
    }

    @Test
    void testSharedNANConstant() {
        IsNotNaN condition1 = new IsNotNaN("calc1");
        IsNotNaN condition2 = new IsNotNaN("calc2");

        // Both should use the same NAN constant from IsNaN
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsNaN.NAN, condition1.propValue());
        assertEquals(IsNaN.NAN, condition2.propValue());
    }

    @Test
    void testToString() {
        IsNotNaN condition = new IsNotNaN("calculation_result");
        String result = condition.toString();

        assertTrue(result.contains("calculation_result"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    void testToString_NoChange() {
        IsNotNaN condition = new IsNotNaN("temperature");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    void testToString_ScreamingSnakeCase() {
        IsNotNaN condition = new IsNotNaN("fieldName");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    void testToString_SnakeCase() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("sensor_value"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsNotNaN condition = new IsNotNaN("computedValue");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("COMPUTED_VALUE"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    void testWithVariousPropNames() {
        String[] propNames = { "temperature", "ratio", "calculated_value", "sensor_reading", "percentage" };

        for (String propName : propNames) {
            IsNotNaN condition = new IsNotNaN(propName);

            assertEquals(propName, condition.propName());
            assertEquals(Operator.IS_NOT, condition.operator());
            assertEquals(IsNaN.NAN, condition.propValue());

            String result = condition.toString();
            assertTrue(result.contains(propName));
            assertTrue(result.contains("IS NOT"));
            assertTrue(result.contains("NAN"));
        }
    }
}
