package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class IsNotNaN2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNotNaN condition = new IsNotNaN("temperature");
        assertEquals("temperature", condition.getPropName());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(""));
    }

    @Test
    public void testGetPropName() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        assertEquals("sensorValue", condition.getPropName());
    }

    @Test
    public void testGetPropName_DifferentNames() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.getPropName(), cond2.getPropName());
    }

    @Test
    public void testGetPropValue() {
        IsNotNaN condition = new IsNotNaN("calculatedValue");
        Expression value = condition.getPropValue();
        assertNotNull(value);
    }

    @Test
    public void testGetOperator() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testGetParameters() {
        IsNotNaN condition = new IsNotNaN("temperature");
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testToString_NoChange() {
        IsNotNaN condition = new IsNotNaN("temperature");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("sensor_value"));
    }

    @Test
    public void testToString_ScreamingSnakeCase() {
        IsNotNaN condition = new IsNotNaN("fieldName");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    public void testHashCode() {
        IsNotNaN cond1 = new IsNotNaN("value");
        IsNotNaN cond2 = new IsNotNaN("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsNotNaN cond1 = new IsNotNaN("temperature");
        IsNotNaN cond2 = new IsNotNaN("temperature");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsNotNaN cond1 = new IsNotNaN("temp1");
        IsNotNaN cond2 = new IsNotNaN("temp2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNotNaN condition = new IsNotNaN("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testInheritance() {
        IsNotNaN condition = new IsNotNaN("field");
        assertTrue(condition instanceof IsNot);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testDoublePropertyName() {
        IsNotNaN condition = new IsNotNaN("calculationResult");
        assertEquals("calculationResult", condition.getPropName());
    }

    @Test
    public void testComplexPropertyName() {
        IsNotNaN condition = new IsNotNaN("user.profile.score");
        assertEquals("user.profile.score", condition.getPropName());
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("user.profile.score"));
    }

    @Test
    public void testMultipleInstances() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        IsNotNaN cond3 = new IsNotNaN("field3");

        assertNotEquals(cond1, cond2);
        assertNotEquals(cond2, cond3);
        assertNotEquals(cond1, cond3);
    }
}

public class IsNotNaNTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNotNaN condition = new IsNotNaN("calculation_result");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("calculation_result", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
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
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
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
            Assertions.assertEquals(Operator.IS_NOT, condition.operator());
            Assertions.assertEquals(IsNaN.NAN, condition.getPropValue());

            String result = condition.toString();
            Assertions.assertTrue(result.contains(propName));
            Assertions.assertTrue(result.contains("IS NOT"));
            Assertions.assertTrue(result.contains("NAN"));
        }
    }
}
