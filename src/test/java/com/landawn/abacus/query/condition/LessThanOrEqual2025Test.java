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
public class LessThanOrEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        LessThanOrEqual condition = new LessThanOrEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        LessThanOrEqual condition = new LessThanOrEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        LessThanOrEqual condition = new LessThanOrEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        LessThanOrEqual condition = new LessThanOrEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        LessThanOrEqual original = new LessThanOrEqual("name", "John");
        LessThanOrEqual copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        LessThanOrEqual cond1 = new LessThanOrEqual("status", "active");
        LessThanOrEqual cond2 = new LessThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field1", "value");
        LessThanOrEqual cond2 = new LessThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field", "value1");
        LessThanOrEqual cond2 = new LessThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        LessThanOrEqual condition = new LessThanOrEqual("field", 80);
        condition.setPropValue(90);
        assertEquals(Integer.valueOf(90), condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue_ChangeType() {
        LessThanOrEqual condition = new LessThanOrEqual("field", 80);
        condition.setPropValue("modifiedValue");
        assertEquals("modifiedValue", condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        LessThanOrEqual condition = new LessThanOrEqual("quantity", 100);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 40);
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 40);
        assertNotEquals(lessThanOrEqual, greaterThanOrEqual);
    }
}
