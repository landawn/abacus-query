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
public class GreaterThan2025Test extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThan condition = new GreaterThan("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.GREATER_THAN, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("", 25));
    }

    @Test
    public void testGetPropName() {
        GreaterThan condition = new GreaterThan("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        GreaterThan condition = new GreaterThan("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        GreaterThan condition = new GreaterThan("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        GreaterThan condition = new GreaterThan("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(Operator.GREATER_THAN, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        GreaterThan condition = new GreaterThan("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        GreaterThan condition = new GreaterThan("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        GreaterThan condition = new GreaterThan("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        GreaterThan original = new GreaterThan("name", "John");
        GreaterThan copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        GreaterThan condition = new GreaterThan("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        GreaterThan condition = new GreaterThan("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        GreaterThan cond1 = new GreaterThan("status", "active");
        GreaterThan cond2 = new GreaterThan("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        GreaterThan cond1 = new GreaterThan("field1", "value");
        GreaterThan cond2 = new GreaterThan("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        GreaterThan cond1 = new GreaterThan("field", "value1");
        GreaterThan cond2 = new GreaterThan("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThan condition = new GreaterThan("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        GreaterThan condition = new GreaterThan("field", 100);
        condition.setPropValue(200);
        assertEquals(Integer.valueOf(200), condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue_ChangeType() {
        GreaterThan condition = new GreaterThan("field", 100);
        condition.setPropValue("newValue");
        assertEquals("newValue", condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        GreaterThan condition = new GreaterThan("age", 18);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("18"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        GreaterThan greaterThan = new GreaterThan("field", 10);
        LessThan lessThan = new LessThan("field", 10);
        assertNotEquals(greaterThan, lessThan);
    }
}
