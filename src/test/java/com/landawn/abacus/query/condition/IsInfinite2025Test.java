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
public class IsInfinite2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsInfinite condition = new IsInfinite("growthRate");
        assertEquals("growthRate", condition.getPropName());
        assertEquals(Operator.IS, condition.getOperator());
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
        assertEquals(Operator.IS, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        IsInfinite condition = new IsInfinite("divisionResult");
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        IsInfinite condition = new IsInfinite("value");
        condition.clearParameters();
        assertNotNull(condition);
    }

    @Test
    public void testCopy() {
        IsInfinite original = new IsInfinite("overflowValue");
        IsInfinite copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals(original.getOperator(), copy.getOperator());
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
        assertEquals(Operator.NOT, result.getOperator());
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
    public void testCopyIndependence() {
        IsInfinite original = new IsInfinite("value");
        IsInfinite copy = original.copy();

        copy.clearParameters();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
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
