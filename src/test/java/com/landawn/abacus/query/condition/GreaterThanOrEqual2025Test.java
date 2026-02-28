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
public class GreaterThanOrEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.GREATER_EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(Operator.GREATER_EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        GreaterThanOrEqual original = new GreaterThanOrEqual("name", "John");
        GreaterThanOrEqual copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("status", "active");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field1", "value");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field", "value1");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", 50);
        condition.setPropValue(75);
        assertEquals(Integer.valueOf(75), condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue_ChangeType() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", 50);
        condition.setPropValue("updatedValue");
        assertEquals("updatedValue", condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("score", 60);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("score"));
        assertTrue(result.contains("60"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 20);
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 20);
        assertNotEquals(greaterThanOrEqual, lessThanOrEqual);
    }
}
