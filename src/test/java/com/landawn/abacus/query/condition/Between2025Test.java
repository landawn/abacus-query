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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Between2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Between condition = new Between("age", 18, 65);
        assertEquals("age", condition.getPropName());
        assertEquals(Integer.valueOf(18), condition.getMinValue());
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between(null, 10, 20));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between("", 10, 20));
    }

    @Test
    public void testGetPropName() {
        Between condition = new Between("salary", 50000, 100000);
        assertEquals("salary", condition.getPropName());
    }

    @Test
    public void testGetMinValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double min = condition.getMinValue();
        assertEquals(10.0, min);
    }

    @Test
    public void testGetMaxValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double max = condition.getMaxValue();
        assertEquals(50.0, max);
    }

    @Test
    public void testGetOperator() {
        Between condition = new Between("field", 1, 10);
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Between condition = new Between("age", 18, 65);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    public void testGetParameters_WithNullValues() {
        Between condition = new Between("field", null, null);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertNull(params.get(0));
        assertNull(params.get(1));
    }

    @Test
    public void testClearParameters() {
        Between condition = new Between("age", 18, 65);
        condition.clearParameters();
        assertNull(condition.getMinValue());
        assertNull(condition.getMaxValue());
    }

    @Test
    public void testCopy() {
        Between original = new Between("age", 18, 65);
        Between copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getMinValue(), (Object) copy.getMinValue());
        assertEquals((Object) original.getMaxValue(), (Object) copy.getMaxValue());
    }

    @Test
    public void testToString_NoChange() {
        Between condition = new Between("age", 18, 65);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testToString_SnakeCase() {
        Between condition = new Between("userAge", 18, 65);
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_age"));
    }

    @Test
    public void testHashCode() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Between condition = new Between("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 18, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentMinValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentMaxValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 70);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Between cond1 = new Between("age", 18, 30);
        Between cond2 = new Between("age", 50, 65);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Between condition = new Between("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testStringValues() {
        Between condition = new Between("name", "A", "M");
        assertEquals("A", condition.getMinValue());
        assertEquals("M", condition.getMaxValue());
    }

    @Test
    public void testNumericValues() {
        Between condition = new Between("score", 0, 100);
        assertEquals(Integer.valueOf(0), condition.getMinValue());
        assertEquals(Integer.valueOf(100), condition.getMaxValue());
    }

    @Test
    public void testSetMinValue() {
        Between condition = new Between("age", 18, 65);
        condition.setMinValue(21);
        assertEquals(Integer.valueOf(21), condition.getMinValue());
    }

    @Test
    public void testSetMaxValue() {
        Between condition = new Between("age", 18, 65);
        condition.setMaxValue(70);
        assertEquals(Integer.valueOf(70), condition.getMaxValue());
    }

    @Test
    public void testCopy_DeepCopy() {
        Between original = new Between("price", 10.0, 50.0);
        Between copy = original.copy();

        assertNotSame(original, copy);

        // Modify original
        original.clearParameters();

        // Copy should not be affected
        assertEquals(10.0, copy.getMinValue());
        assertEquals(50.0, copy.getMaxValue());
    }

    @Test
    public void testToString_WithNullValues() {
        Between condition = new Between("value", null, null);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("value"));
        assertTrue(result.contains("BETWEEN"));
    }

    @Test
    public void testDoubleValues() {
        Between condition = new Between("price", 9.99, 99.99);
        assertEquals(9.99, (Double) condition.getMinValue());
        assertEquals(99.99, (Double) condition.getMaxValue());
    }
}
