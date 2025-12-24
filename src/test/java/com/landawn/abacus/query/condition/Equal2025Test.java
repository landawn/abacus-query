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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Equal2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Equal condition = new Equal("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal("", 25));
    }

    @Test
    public void testGetPropName() {
        Equal condition = new Equal("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Equal condition = new Equal("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Equal condition = new Equal("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Equal condition = new Equal("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Equal condition = new Equal("field", "value");
        assertEquals(Operator.EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        Equal condition = new Equal("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        Equal condition = new Equal("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        Equal condition = new Equal("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        Equal original = new Equal("name", "John");
        Equal copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        Equal condition = new Equal("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        Equal condition = new Equal("userName", "Bob");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Equal condition = new Equal("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Equal cond1 = new Equal("field1", "value");
        Equal cond2 = new Equal("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Equal cond1 = new Equal("field", "value1");
        Equal cond2 = new Equal("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Equal condition = new Equal("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testStringValueQuoting() {
        Equal condition = new Equal("name", "John");
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("'John'"));
    }

    @Test
    public void testNumericValueNoQuoting() {
        Equal condition = new Equal("age", 25);
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("25"));
        assertFalse(str.contains("'25'"));
    }

    @Test
    public void testBooleanValue() {
        Equal condition = new Equal("active", true);
        assertEquals(true, condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        Equal condition = new Equal("field", "oldValue");
        condition.setPropValue("newValue");
        assertEquals("newValue", condition.getPropValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue_ChangeType() {
        Equal condition = new Equal("field", "stringValue");
        condition.setPropValue(123);
        assertEquals(Integer.valueOf(123), condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        Equal condition = new Equal("name", "value");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        Equal equal = new Equal("field", "value");
        NotEqual notEqual = new NotEqual("field", "value");
        assertNotEquals(equal, notEqual);
    }
}
