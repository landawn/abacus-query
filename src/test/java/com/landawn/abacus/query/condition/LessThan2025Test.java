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
public class LessThan2025Test extends TestBase {

    @Test
    public void testConstructor() {
        LessThan condition = new LessThan("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int)condition.getPropValue());
        assertEquals(Operator.LESS_THAN, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThan(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThan("", 25));
    }

    @Test
    public void testGetPropName() {
        LessThan condition = new LessThan("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        LessThan condition = new LessThan("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        LessThan condition = new LessThan("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        LessThan condition = new LessThan("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        LessThan condition = new LessThan("field", "value");
        assertEquals(Operator.LESS_THAN, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        LessThan condition = new LessThan("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int)params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        LessThan condition = new LessThan("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int)params.size());
        assertEquals(42, (int)params.get(0));
    }

    @Test
    public void testClearParameters() {
        LessThan condition = new LessThan("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        LessThan original = new LessThan("name", "John");
        LessThan copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object)original.getPropValue(), (Object)copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        LessThan condition = new LessThan("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        LessThan condition = new LessThan("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        LessThan cond1 = new LessThan("age", 25);
        LessThan cond2 = new LessThan("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        LessThan cond1 = new LessThan("age", 25);
        LessThan cond2 = new LessThan("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LessThan condition = new LessThan("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        LessThan cond1 = new LessThan("status", "active");
        LessThan cond2 = new LessThan("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        LessThan cond1 = new LessThan("field1", "value");
        LessThan cond2 = new LessThan("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        LessThan cond1 = new LessThan("field", "value1");
        LessThan cond2 = new LessThan("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        LessThan condition = new LessThan("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        LessThan condition = new LessThan("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        LessThan cond1 = new LessThan("a", 1);
        LessThan cond2 = new LessThan("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        LessThan cond1 = new LessThan("a", 1);
        LessThan cond2 = new LessThan("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        LessThan condition = new LessThan("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }
}
