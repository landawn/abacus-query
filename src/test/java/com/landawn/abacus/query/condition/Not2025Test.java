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
public class Not2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Equal innerCondition = new Equal("status", "active");
        Not condition = new Not(innerCondition);

        assertEquals(Operator.NOT, condition.getOperator());
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
        And innerAnd = new And(
            new Equal("a", 1),
            new Equal("b", 2)
        );
        Not condition = new Not(innerAnd);

        And retrieved = condition.getCondition();
        assertSame(innerAnd, retrieved);
    }

    @Test
    public void testGetOperator() {
        Not condition = new Not(new Equal("field", "value"));
        assertEquals(Operator.NOT, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        Equal innerCondition = new Equal("name", "John");
        Not condition = new Not(innerCondition);

        List<Object> params = condition.getParameters();
        assertEquals(1, (int)params.size());
        assertEquals("John", params.get(0));
    }

    @Test
    public void testGetParameters_ComplexCondition() {
        Between between = new Between("age", 18, 65);
        Not condition = new Not(between);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int)params.size());
        assertEquals(18, (int)params.get(0));
        assertEquals(65, (int)params.get(1));
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
        assertEquals(30, (int)copiedInner.getPropValue());
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
    public void testToString_LowerCaseWithUnderscore() {
        Equal innerCondition = new Equal("firstName", "Charlie");
        Not condition = new Not(innerCondition);

        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
        And innerAnd = new And(
            new Equal("status", "active"),
            new GreaterThan("age", 18)
        );
        Not condition = new Not(innerAnd);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int)params.size());
    }

    @Test
    public void testAnd() {
        Not cond1 = new Not(new Equal("a", 1));
        Equal cond2 = new Equal("b", 2);

        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Not cond1 = new Not(new Equal("a", 1));
        Equal cond2 = new Equal("b", 2);

        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Not condition = new Not(new Equal("a", 1));
        Not result = condition.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }
}
