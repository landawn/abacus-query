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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Not2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Equal innerCondition = new Equal("status", "active");
        Not condition = new Not(innerCondition);

        assertEquals(Operator.NOT, condition.operator());
        assertSame(innerCondition, condition.getCondition());
    }

    @Test
    public void testConstructor_NullCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Not(null));
    }

    @Test
    public void testGetCondition() {
        Equal innerCondition = new Equal("age", 25);
        Not condition = new Not(innerCondition);

        Equal retrieved = condition.getCondition();
        assertSame(innerCondition, retrieved);
    }

    @Test
    public void testGetCondition_ComplexCondition() {
        And innerAnd = new And(new Equal("a", 1), new Equal("b", 2));
        Not condition = new Not(innerAnd);

        And retrieved = condition.getCondition();
        assertSame(innerAnd, retrieved);
    }

    @Test
    public void testGetOperator() {
        Not condition = new Not(new Equal("field", "value"));
        assertEquals(Operator.NOT, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Equal innerCondition = new Equal("name", "John");
        Not condition = new Not(innerCondition);

        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("John", params.get(0));
    }

    @Test
    public void testGetParameters_ComplexCondition() {
        Between between = new Between("age", 18, 65);
        Not condition = new Not(between);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    public void testClearParameters() {
        Equal innerCondition = new Equal("field", "value");
        Not condition = new Not(innerCondition);

        condition.clearParameters();
        assertNull(innerCondition.getPropValue());
    }

    @Test
    public void testCopy() {
        Equal innerCondition = new Equal("name", "Alice");
        Not original = new Not(innerCondition);

        Not copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopy_PreservesValues() {
        Equal innerCondition = new Equal("age", 30);
        Not original = new Not(innerCondition);

        Not copy = original.copy();
        Equal copiedInner = copy.getCondition();
        assertEquals("age", copiedInner.getPropName());
        assertEquals(30, (int) copiedInner.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Equal innerCondition = new Equal("userName", "Bob");
        Not condition = new Not(innerCondition);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("userName"));
    }

    @Test
    public void testToString_SnakeCase() {
        Equal innerCondition = new Equal("firstName", "Charlie");
        Not condition = new Not(innerCondition);

        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("first_name"));
    }

    @Test
    public void testHashCode() {
        Equal inner = new Equal("field", "value");
        Not cond1 = new Not(inner);
        Not cond2 = new Not(new Equal("field", "value"));

        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Not condition = new Not(new Equal("a", 1));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("a", 1));
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentInnerConditions() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("b", 2));
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(condition, "string");
    }

    @Test
    public void testNestedCondition() {
        And innerAnd = new And(new Equal("status", "active"), new GreaterThan("age", 18));
        Not condition = new Not(innerAnd);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetCondition() {
        Equal originalCondition = new Equal("name", "John");
        Not condition = new Not(originalCondition);

        Equal newCondition = new Equal("age", 30);
        condition.setCondition(newCondition);

        Condition retrieved = condition.getCondition();
        assertEquals(newCondition, retrieved);
        assertEquals("age", ((Equal) retrieved).getPropName());
    }

    @Test
    public void testToString_NoArgs() {
        Not condition = new Not(new Equal("status", "active"));
        String result = condition.toString();

        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("status"));
    }

    @Test
    public void testGetParameters_NullCondition() {
        Not condition = new Not(new Equal("field", null));
        List<Object> params = condition.getParameters();

        assertEquals(1, (int) params.size());
        assertNull(params.get(0));
    }

    @Test
    public void testCopy_Independence() {
        Equal innerCondition = new Equal("name", "Alice");
        Not original = new Not(innerCondition);
        Not copy = original.copy();

        // Modify original's inner condition
        innerCondition.clearParameters();

        // Copy should still have the value
        Equal copiedInner = copy.getCondition();
        assertEquals("Alice", copiedInner.getPropValue());
    }
}
