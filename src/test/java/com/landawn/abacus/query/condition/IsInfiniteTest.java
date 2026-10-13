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

public class IsInfiniteTest extends TestBase {

    @Test
    void testAnd() {
        IsInfinite cond1 = new IsInfinite("val1");
        IsInfinite cond2 = new IsInfinite("val2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testCommonInfinityScenarios() {
        // Test common scenarios that might produce infinity
        IsInfinite divisionByZero = new IsInfinite("division_result");
        IsInfinite overflow = new IsInfinite("overflow_value");
        IsInfinite logarithm = new IsInfinite("log_result");

        String result1 = divisionByZero.toString();
        String result2 = overflow.toString();
        String result3 = logarithm.toString();

        assertTrue(result1.contains("division_result IS INFINITE"));
        assertTrue(result2.contains("overflow_value IS INFINITE"));
        assertTrue(result3.contains("log_result IS INFINITE"));
    }

    @Test
    void testComplexPropertyName() {
        IsInfinite condition = new IsInfinite("calculation.result.value");
        assertEquals("calculation.result.value", condition.propName());
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("calculation.result.value"));
    }

    @Test
    void testConstructor() {
        IsInfinite condition = new IsInfinite("growthRate");
        assertEquals("growthRate", condition.propName());
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsInfinite(""));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsInfinite(null));
    }

    @Test
    void testConstructorWithDifferentPropNames() {
        String[] propNames = { "calculated_ratio", "interest_rate", "computed_value", "division_result" };

        for (String propName : propNames) {
            IsInfinite condition = new IsInfinite(propName);

            assertEquals(propName, condition.propName());
            assertEquals(Operator.IS, condition.operator());
            assertEquals(IsInfinite.INFINITE, condition.propValue());
        }
    }

    @Test
    void testConstructorWithPropName() {
        IsInfinite condition = new IsInfinite("growth_rate");

        assertNotNull(condition);
        assertEquals("growth_rate", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertEquals(IsInfinite.INFINITE, condition.propValue());
    }

    @Test
    void testDivisionByZeroScenario() {
        // Represents checking if division resulted in infinity
        IsInfinite condition = new IsInfinite("averagePerUnit");
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("averagePerUnit"));
        assertTrue(sql.contains("IS INFINITE"));
    }

    @Test
    void testEquals() {
        IsInfinite condition1 = new IsInfinite("field1");
        IsInfinite condition2 = new IsInfinite("field1");
        IsInfinite condition3 = new IsInfinite("field2");

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        IsInfinite condition = new IsInfinite("field");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        IsInfinite cond1 = new IsInfinite("field1");
        IsInfinite cond2 = new IsInfinite("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        IsInfinite cond1 = new IsInfinite("ratio");
        IsInfinite cond2 = new IsInfinite("ratio");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        IsInfinite condition = new IsInfinite("field");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsInfinite condition = new IsInfinite("field");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsInfinite condition = new IsInfinite("field");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    void testGetPropName() {
        IsInfinite condition = new IsInfinite("calculatedRatio");
        assertEquals("calculatedRatio", condition.propName());
    }

    @Test
    void testHashCode() {
        IsInfinite cond1 = new IsInfinite("value");
        IsInfinite cond2 = new IsInfinite("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        IsInfinite cond1 = new IsInfinite("value1");
        IsInfinite cond2 = new IsInfinite("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testINFINITEConstant() {
        assertNotNull(IsInfinite.INFINITE);
        // Verify it's an SqlExpression
        assertTrue(IsInfinite.INFINITE.toString().contains("INFINITE"));
    }

    @Test
    void testInheritance() {
        IsInfinite condition = new IsInfinite("field");
        assertTrue(condition instanceof Is);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testInheritedMethods() {
        IsInfinite condition = new IsInfinite("value");

        // Test methods inherited from Is
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testMultipleInstances() {
        IsInfinite condition1 = new IsInfinite("calc1");
        IsInfinite condition2 = new IsInfinite("calc2");

        // Both should share the same INFINITE constant
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsInfinite.INFINITE, condition1.propValue());
        assertEquals(IsInfinite.INFINITE, condition2.propValue());
    }

    @Test
    void testNot() {
        IsInfinite condition = new IsInfinite("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testNumericFieldNames() {
        // Test with various numeric field names
        String[] numericFields = { "price_ratio", "growth_rate", "interest_rate", "profit_margin", "conversion_rate", "performance_metric" };

        for (String field : numericFields) {
            IsInfinite condition = new IsInfinite(field);

            assertEquals(field, condition.propName());
            assertEquals(IsInfinite.INFINITE, condition.propValue());

            String result = condition.toString();
            assertTrue(result.contains(field + " IS INFINITE"));
        }
    }

    @Test
    void testOr() {
        IsInfinite cond1 = new IsInfinite("value1");
        IsInfinite cond2 = new IsInfinite("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testOverflowCheckScenario() {
        IsInfinite condition = new IsInfinite("exponentialGrowth");
        assertNotNull(condition.propName());
        assertNotNull(condition.propValue());
    }

    @Test
    void testParameters() {
        IsInfinite condition = new IsInfinite("divisionResult");
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testPropValueAs() {
        IsInfinite condition = new IsInfinite("value");
        SqlExpression value = condition.propValueAs(SqlExpression.class);
        assertNotNull(value);
        assertTrue(value.toString().contains("INFINITE"));
    }

    @Test
    void testStaticInfiniteExpression() {
        IsInfinite cond1 = new IsInfinite("field1");
        IsInfinite cond2 = new IsInfinite("field2");

        // Both should use the same INFINITE expression
        assertEquals(cond1.propValue().toString(), cond2.propValue().toString());
    }

    @Test
    void testToString() {
        IsInfinite condition = new IsInfinite("calculated_ratio");
        String result = condition.toString();

        assertTrue(result.contains("calculated_ratio"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    void testToString_NoChange() {
        IsInfinite condition = new IsInfinite("growthRate");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("growthRate"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    void testToString_ScreamingSnakeCase() {
        IsInfinite condition = new IsInfinite("fieldName");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    void testToString_SnakeCase() {
        IsInfinite condition = new IsInfinite("growthRate");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("growth_rate"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsInfinite condition = new IsInfinite("interestRate");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("INTEREST_RATE"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("INFINITE"));
    }
}
