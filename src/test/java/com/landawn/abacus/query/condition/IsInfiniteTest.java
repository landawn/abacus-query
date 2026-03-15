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
class IsInfinite2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsInfinite condition = new IsInfinite("growthRate");
        assertEquals("growthRate", condition.getPropName());
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsInfinite(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsInfinite(""));
    }

    @Test
    public void testGetPropName() {
        IsInfinite condition = new IsInfinite("calculatedRatio");
        assertEquals("calculatedRatio", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        IsInfinite condition = new IsInfinite("value");
        Expression value = condition.getPropValue();
        assertNotNull(value);
        assertTrue(value.toString().contains("INFINITE"));
    }

    @Test
    public void testGetOperator() {
        IsInfinite condition = new IsInfinite("field");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testGetParameters() {
        IsInfinite condition = new IsInfinite("divisionResult");
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testToString_NoChange() {
        IsInfinite condition = new IsInfinite("growthRate");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("growthRate"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsInfinite condition = new IsInfinite("growthRate");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("growth_rate"));
    }

    @Test
    public void testToString_ScreamingSnakeCase() {
        IsInfinite condition = new IsInfinite("fieldName");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    public void testHashCode() {
        IsInfinite cond1 = new IsInfinite("value");
        IsInfinite cond2 = new IsInfinite("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        IsInfinite cond1 = new IsInfinite("value1");
        IsInfinite cond2 = new IsInfinite("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsInfinite condition = new IsInfinite("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsInfinite cond1 = new IsInfinite("ratio");
        IsInfinite cond2 = new IsInfinite("ratio");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsInfinite cond1 = new IsInfinite("field1");
        IsInfinite cond2 = new IsInfinite("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsInfinite condition = new IsInfinite("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsInfinite condition = new IsInfinite("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsInfinite cond1 = new IsInfinite("val1");
        IsInfinite cond2 = new IsInfinite("val2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsInfinite cond1 = new IsInfinite("value1");
        IsInfinite cond2 = new IsInfinite("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsInfinite condition = new IsInfinite("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testInheritance() {
        IsInfinite condition = new IsInfinite("field");
        assertTrue(condition instanceof Is);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testStaticInfiniteExpression() {
        IsInfinite cond1 = new IsInfinite("field1");
        IsInfinite cond2 = new IsInfinite("field2");

        // Both should use the same INFINITE expression
        assertEquals(cond1.getPropValue().toString(), cond2.getPropValue().toString());
    }

    @Test
    public void testComplexPropertyName() {
        IsInfinite condition = new IsInfinite("calculation.result.value");
        assertEquals("calculation.result.value", condition.getPropName());
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("calculation.result.value"));
    }

    @Test
    public void testDivisionByZeroScenario() {
        // Represents checking if division resulted in infinity
        IsInfinite condition = new IsInfinite("averagePerUnit");
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("averagePerUnit"));
        assertTrue(sql.contains("IS INFINITE"));
    }

    @Test
    public void testOverflowCheckScenario() {
        IsInfinite condition = new IsInfinite("exponentialGrowth");
        assertNotNull(condition.getPropName());
        assertNotNull(condition.getPropValue());
    }
}

public class IsInfiniteTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsInfinite condition = new IsInfinite("growth_rate");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("growth_rate", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
        Assertions.assertEquals(IsInfinite.INFINITE, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "calculated_ratio", "interest_rate", "computed_value", "division_result" };

        for (String propName : propNames) {
            IsInfinite condition = new IsInfinite(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS, condition.operator());
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
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

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
        Assertions.assertEquals(Operator.IS, condition.operator());
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
