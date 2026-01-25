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
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Binary2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Binary condition = new Binary("age", Operator.EQUAL, 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.EQUAL, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary(null, Operator.EQUAL, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary("", Operator.EQUAL, 25));
    }

    @Test
    public void testGetPropName() {
        Binary condition = new Binary("userName", Operator.LIKE, "%test%");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Binary condition = new Binary("age", Operator.GREATER_THAN, 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Binary condition = new Binary("name", Operator.EQUAL, "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Binary condition = new Binary("field", Operator.IS, null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Binary condition = new Binary("field", Operator.NOT_EQUAL, "value");
        assertEquals(Operator.NOT_EQUAL, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        Binary condition = new Binary("status", Operator.EQUAL, "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_WithCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Binary condition = new Binary("userId", Operator.IN, subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testClearParameters_WithCondition() {
        Equal innerCond = new Equal("id", 100);
        Binary condition = new Binary("userId", Operator.EQUAL, innerCond);
        condition.clearParameters();
        assertNull(innerCond.getPropValue());
    }

    @Test
    public void testCopy() {
        Binary original = new Binary("name", Operator.EQUAL, "John");
        Binary copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testCopy_WithCondition() {
        Equal innerCond = new Equal("id", 100);
        Binary original = new Binary("userId", Operator.EQUAL, innerCond);
        Binary copy = original.copy();

        assertNotSame(original, copy);
        assertNotSame(innerCond, copy.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Binary condition = new Binary("userName", Operator.EQUAL, "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    public void testToString_SnakeCase() {
        Binary condition = new Binary("userName", Operator.GREATER_THAN, "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Binary cond1 = new Binary("status", Operator.EQUAL, "active");
        Binary cond2 = new Binary("status", Operator.EQUAL, "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Binary cond1 = new Binary("field1", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field2", Operator.EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentOperator() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field", Operator.NOT_EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value1");
        Binary cond2 = new Binary("field", Operator.EQUAL, "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testDifferentOperators() {
        Binary eq = new Binary("a", Operator.EQUAL, 1);
        Binary gt = new Binary("b", Operator.GREATER_THAN, 2);
        Binary lt = new Binary("c", Operator.LESS_THAN, 3);
        Binary like = new Binary("d", Operator.LIKE, "%test%");

        assertEquals(Operator.EQUAL, eq.getOperator());
        assertEquals(Operator.GREATER_THAN, gt.getOperator());
        assertEquals(Operator.LESS_THAN, lt.getOperator());
        assertEquals(Operator.LIKE, like.getOperator());
    }

    @Test
    public void testSetPropValue() {
        Binary condition = new Binary("status", Operator.EQUAL, "active");
        assertEquals("active", (String) condition.getPropValue());

        condition.setPropValue("inactive");
        assertEquals("inactive", (String) condition.getPropValue());
    }

    @Test
    public void testSetPropValue_Null() {
        Binary condition = new Binary("field", Operator.EQUAL, "initial");
        condition.setPropValue(null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testSetPropValue_DifferentType() {
        Binary condition = new Binary("field", Operator.EQUAL, "string");
        condition.setPropValue(123);
        assertEquals(Integer.valueOf(123), condition.getPropValue());
    }

    @Test
    public void testSetPropValue_WithCondition() {
        Equal initialCond = new Equal("id", 1);
        Binary condition = new Binary("field", Operator.EQUAL, initialCond);

        Equal newCond = new Equal("id", 2);
        condition.setPropValue(newCond);

        Equal retrieved = condition.getPropValue();
        assertEquals(newCond, retrieved);
    }
}
