/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class IsNotInfinite2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNotInfinite condition = new IsNotInfinite("priceRatio");
        assertEquals("priceRatio", condition.getPropName());
        assertEquals(Operator.IS_NOT, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotInfinite(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotInfinite(""));
    }

    @Test
    public void testGetPropName() {
        IsNotInfinite condition = new IsNotInfinite("sensorReading");
        assertEquals("sensorReading", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        IsNotInfinite condition = new IsNotInfinite("value");
        Expression value = condition.getPropValue();
        assertNotNull(value);
        assertTrue(value.toString().contains("INFINITE"));
    }

    @Test
    public void testGetOperator() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertEquals(Operator.IS_NOT, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        IsNotInfinite condition = new IsNotInfinite("value");
        condition.clearParameters();
        assertNotNull(condition);
    }

    @Test
    public void testCopy() {
        IsNotInfinite original = new IsNotInfinite("calculatedValue");
        IsNotInfinite copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        IsNotInfinite condition = new IsNotInfinite("validValue");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("validValue"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("INFINITE"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        IsNotInfinite condition = new IsNotInfinite("growthRate");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("growth_rate"));
    }

    @Test
    public void testToString_UpperCaseWithUnderscore() {
        IsNotInfinite condition = new IsNotInfinite("fieldName");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    public void testHashCode() {
        IsNotInfinite cond1 = new IsNotInfinite("value");
        IsNotInfinite cond2 = new IsNotInfinite("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        IsNotInfinite cond1 = new IsNotInfinite("value1");
        IsNotInfinite cond2 = new IsNotInfinite("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsNotInfinite cond1 = new IsNotInfinite("measurement");
        IsNotInfinite cond2 = new IsNotInfinite("measurement");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNotInfinite cond1 = new IsNotInfinite("field1");
        IsNotInfinite cond2 = new IsNotInfinite("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsNotInfinite cond1 = new IsNotInfinite("val1");
        IsNotInfinite cond2 = new IsNotInfinite("val2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNotInfinite cond1 = new IsNotInfinite("value1");
        IsNotInfinite cond2 = new IsNotInfinite("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNotInfinite condition = new IsNotInfinite("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testInheritance() {
        IsNotInfinite condition = new IsNotInfinite("field");
        assertTrue(condition instanceof IsNot);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testSharedInfiniteExpression() {
        IsNotInfinite cond1 = new IsNotInfinite("field1");
        IsNotInfinite cond2 = new IsNotInfinite("field2");

        // Both should use the same INFINITE expression
        assertEquals(cond1.getPropValue().toString(), cond2.getPropValue().toString());
    }

    @Test
    public void testComplexPropertyName() {
        IsNotInfinite condition = new IsNotInfinite("stats.calculation.result");
        assertEquals("stats.calculation.result", condition.getPropName());
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("stats.calculation.result"));
    }

    @Test
    public void testCopyIndependence() {
        IsNotInfinite original = new IsNotInfinite("value");
        IsNotInfinite copy = original.copy();

        copy.clearParameters();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
    }

    @Test
    public void testValidationScenario() {
        // Represents ensuring a value is finite before calculations
        IsNotInfinite condition = new IsNotInfinite("averageScore");
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("averageScore"));
        assertTrue(sql.contains("IS NOT INFINITE"));
    }

    @Test
    public void testCombinedWithNotNaN() {
        IsNotInfinite infinite = new IsNotInfinite("value");
        IsNotNaN nan = new IsNotNaN("value");

        And combined = infinite.and(nan);
        assertEquals(Integer.valueOf(2), combined.getConditions().size());
    }
}
