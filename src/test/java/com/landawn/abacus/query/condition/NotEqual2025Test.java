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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.*;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class NotEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        NotEqual condition = new NotEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.NOT_EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        NotEqual condition = new NotEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        NotEqual condition = new NotEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        NotEqual condition = new NotEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        NotEqual condition = new NotEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(Operator.NOT_EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        NotEqual condition = new NotEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        NotEqual condition = new NotEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        NotEqual condition = new NotEqual("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        NotEqual original = new NotEqual("name", "John");
        NotEqual copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        NotEqual condition = new NotEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        NotEqual condition = new NotEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotEqual cond1 = new NotEqual("status", "active");
        NotEqual cond2 = new NotEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotEqual cond1 = new NotEqual("field1", "value");
        NotEqual cond2 = new NotEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        NotEqual cond1 = new NotEqual("field", "value1");
        NotEqual cond2 = new NotEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotEqual condition = new NotEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }
}
