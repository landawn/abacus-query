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

public class IsNotInfiniteTest extends TestBase {

    @Test
    void testAnd() {
        IsNotInfinite cond1 = new IsNotInfinite("val1");
        IsNotInfinite cond2 = new IsNotInfinite("val2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testCombinedWithNotNaN() {
        IsNotInfinite infinite = new IsNotInfinite("value");
        IsNotNaN nan = new IsNotNaN("value");

        And combined = infinite.and(nan);
        assertEquals(Integer.valueOf(2), combined.conditions().size());
    }

    @Test
    void testCommonUseCases() {
        // Test common numeric fields that might be checked for infinity
        String[] numericFields = { "price_ratio", "growth_rate", "division_result", "interest_rate", "profit_margin", "sensor_value" };

        for (String field : numericFields) {
            IsNotInfinite condition = new IsNotInfinite(field);
            String result = condition.toString();

            assertTrue(result.contains(field));
            assertTrue(result.contains("IS NOT INFINITE"));
        }
    }

    @Test
    void testComplexPropertyName() {
        IsNotInfinite condition = new IsNotInfinite("stats.calculation.result");
        assertEquals("stats.calculation.result", condition.propName());
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("stats.calculation.result"));
    }

    @Test
    void testConstructor() {
        IsNotInfinite condition = new IsNotInfinite("priceRatio");
        assertEquals("priceRatio", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotInfinite(""));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotInfinite(null));
    }

    @Test
    void testConstructorWithDifferentPropNames() {
        String[] propNames = { "sensor_reading", "growth_rate", "calculation_result", "measurement" };

        for (String propName : propNames) {
            IsNotInfinite condition = new IsNotInfinite(propName);

            assertEquals(propName, condition.propName());
            assertEquals(Operator.IS_NOT, condition.operator());
            assertEquals(IsInfinite.INFINITE, condition.propValue());
        }
    }

    @Test
    void testConstructorWithPropName() {
        IsNotInfinite condition = new IsNotInfinite("price_ratio");

        assertNotNull(condition);
        assertEquals("price_ratio", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertEquals(IsInfinite.INFINITE, condition.propValue());
    }

    @Test
    void testEquals() {
        IsNotInfinite condition1 = new IsNotInfinite("field1");
        IsNotInfinite condition2 = new IsNotInfinite("field1");
        IsNotInfinite condition3 = new IsNotInfinite("field2");

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        IsNotInfinite cond1 = new IsNotInfinite("field1");
        IsNotInfinite cond2 = new IsNotInfinite("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        IsNotInfinite cond1 = new IsNotInfinite("measurement");
        IsNotInfinite cond2 = new IsNotInfinite("measurement");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testGetPropName() {
        IsNotInfinite condition = new IsNotInfinite("sensorReading");
        assertEquals("sensorReading", condition.propName());
    }

    @Test
    void testHashCode() {
        IsNotInfinite cond1 = new IsNotInfinite("value");
        IsNotInfinite cond2 = new IsNotInfinite("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        IsNotInfinite cond1 = new IsNotInfinite("value1");
        IsNotInfinite cond2 = new IsNotInfinite("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertTrue(condition instanceof IsNot);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testInheritedMethods() {
        IsNotInfinite condition = new IsNotInfinite("value");

        // Test methods inherited from IsNot
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testNot() {
        IsNotInfinite condition = new IsNotInfinite("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        IsNotInfinite cond1 = new IsNotInfinite("value1");
        IsNotInfinite cond2 = new IsNotInfinite("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testPropValueAs() {
        IsNotInfinite condition = new IsNotInfinite("value");
        SqlExpression value = condition.propValueAs(SqlExpression.class);
        assertNotNull(value);
        assertTrue(value.toString().contains("INFINITE"));
    }

    @Test
    void testSharedINFINITEConstant() {
        IsNotInfinite condition1 = new IsNotInfinite("calc1");
        IsNotInfinite condition2 = new IsNotInfinite("calc2");

        // Both should use the same INFINITE constant from IsInfinite
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsInfinite.INFINITE, condition1.propValue());
        assertEquals(IsInfinite.INFINITE, condition2.propValue());
    }

    @Test
    void testSharedInfiniteExpression() {
        IsNotInfinite cond1 = new IsNotInfinite("field1");
        IsNotInfinite cond2 = new IsNotInfinite("field2");

        // Both should use the same INFINITE expression
        assertEquals(cond1.propValue().toString(), cond2.propValue().toString());
    }

    @Test
    void testToString() {
        IsNotInfinite condition = new IsNotInfinite("calculation_result");
        String result = condition.toString();

        assertTrue(result.contains("calculation_result"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    void testToString_NoChange() {
        IsNotInfinite condition = new IsNotInfinite("validValue");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("validValue"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    void testToString_ScreamingSnakeCase() {
        IsNotInfinite condition = new IsNotInfinite("fieldName");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    void testToString_SnakeCase() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("growth_rate"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("GROWTH_RATE"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    void testValidationScenario() {
        // Represents ensuring a value is finite before calculations
        IsNotInfinite condition = new IsNotInfinite("averageScore");
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("averageScore"));
        assertTrue(sql.contains("IS NOT INFINITE"));
    }
}
