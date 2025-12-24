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
public class LessEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        LessEqual condition = new LessEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.LESS_EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        LessEqual condition = new LessEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        LessEqual condition = new LessEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        LessEqual condition = new LessEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        LessEqual condition = new LessEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        LessEqual condition = new LessEqual("field", "value");
        assertEquals(Operator.LESS_EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        LessEqual condition = new LessEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        LessEqual condition = new LessEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        LessEqual condition = new LessEqual("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        LessEqual original = new LessEqual("name", "John");
        LessEqual copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        LessEqual condition = new LessEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        LessEqual condition = new LessEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        LessEqual cond1 = new LessEqual("age", 25);
        LessEqual cond2 = new LessEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        LessEqual cond1 = new LessEqual("age", 25);
        LessEqual cond2 = new LessEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LessEqual condition = new LessEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        LessEqual cond1 = new LessEqual("status", "active");
        LessEqual cond2 = new LessEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        LessEqual cond1 = new LessEqual("field1", "value");
        LessEqual cond2 = new LessEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        LessEqual cond1 = new LessEqual("field", "value1");
        LessEqual cond2 = new LessEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        LessEqual condition = new LessEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        LessEqual condition = new LessEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        LessEqual cond1 = new LessEqual("a", 1);
        LessEqual cond2 = new LessEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        LessEqual cond1 = new LessEqual("a", 1);
        LessEqual cond2 = new LessEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        LessEqual condition = new LessEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        LessEqual condition = new LessEqual("field", 80);
        condition.setPropValue(90);
        assertEquals(Integer.valueOf(90), condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue_ChangeType() {
        LessEqual condition = new LessEqual("field", 80);
        condition.setPropValue("modifiedValue");
        assertEquals("modifiedValue", condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        LessEqual condition = new LessEqual("quantity", 100);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        LessEqual lessEqual = new LessEqual("field", 40);
        GreaterEqual greaterEqual = new GreaterEqual("field", 40);
        assertNotEquals(lessEqual, greaterEqual);
    }
}
