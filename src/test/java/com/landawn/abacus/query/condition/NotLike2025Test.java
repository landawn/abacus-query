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
public class NotLike2025Test extends TestBase {

    @Test
    public void testConstructor() {
        NotLike condition = new NotLike("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.NOT_LIKE, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike("", 25));
    }

    @Test
    public void testGetPropName() {
        NotLike condition = new NotLike("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        NotLike condition = new NotLike("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        NotLike condition = new NotLike("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        NotLike condition = new NotLike("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(Operator.NOT_LIKE, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        NotLike condition = new NotLike("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        NotLike condition = new NotLike("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        NotLike condition = new NotLike("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        NotLike original = new NotLike("name", "John");
        NotLike copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        NotLike condition = new NotLike("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        NotLike condition = new NotLike("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotLike cond1 = new NotLike("status", "active");
        NotLike cond2 = new NotLike("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotLike cond1 = new NotLike("field1", "value");
        NotLike cond2 = new NotLike("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        NotLike cond1 = new NotLike("field", "value1");
        NotLike cond2 = new NotLike("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotLike condition = new NotLike("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }
}
