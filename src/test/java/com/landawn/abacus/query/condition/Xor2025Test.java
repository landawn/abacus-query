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
public class Xor2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Xor condition = new Xor("field", true);
        assertEquals("field", condition.getPropName());
        assertEquals(true, condition.getPropValue());
        assertEquals(Operator.XOR, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Xor(null, true));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Xor("", true));
    }

    @Test
    public void testGetPropName() {
        Xor condition = new Xor("isPremium", true);
        assertEquals("isPremium", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Xor condition = new Xor("hasDiscount", false);
        Boolean value = condition.getPropValue();
        assertEquals(false, value);
    }

    @Test
    public void testGetOperator() {
        Xor condition = new Xor("field", "value");
        assertEquals(Operator.XOR, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        Xor condition = new Xor("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Xor condition = new Xor("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        Xor original = new Xor("name", "John");
        Xor copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Xor condition = new Xor("hasGoldMembership", true);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("hasGoldMembership"));
        assertTrue(result.contains("XOR"));
    }

    @Test
    public void testHashCode() {
        Xor cond1 = new Xor("field", true);
        Xor cond2 = new Xor("field", true);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Xor condition = new Xor("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Xor cond1 = new Xor("status", "active");
        Xor cond2 = new Xor("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Xor cond1 = new Xor("field1", "value");
        Xor cond2 = new Xor("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Xor condition = new Xor("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testAnd() {
        Xor cond1 = new Xor("a", 1);
        Xor cond2 = new Xor("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Xor cond1 = new Xor("a", 1);
        Xor cond2 = new Xor("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Xor condition = new Xor("field", "value");
        Not result = condition.not();
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        Xor condition = new Xor("flag", true);
        assertEquals(true, condition.getPropValue());

        condition.setPropValue(false);
        assertEquals(false, condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        Xor condition = new Xor("isActive", true);
        String result = condition.toString();

        assertTrue(result.contains("isActive"));
        assertTrue(result.contains("XOR"));
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Xor cond1 = new Xor("field", "value1");
        Xor cond2 = new Xor("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentClass() {
        Xor condition = new Xor("field", "value");
        Equal equal = new Equal("field", "value");
        assertNotEquals(condition, (Object) equal);
    }

    @Test
    public void testGetParameters_WithNestedCondition() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        Xor condition = new Xor("userId", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testCopy_DeepCopy() {
        Xor original = new Xor("value", 100);
        Xor copy = original.copy();

        original.clearParameters();
        assertNull(original.getPropValue());
        assertEquals(100, (int) copy.getPropValue());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Xor condition = new Xor("userName", "john");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("user_name") || result.contains("userName"));
    }

    @Test
    public void testHashCode_DifferentValues() {
        Xor cond1 = new Xor("field", "value1");
        Xor cond2 = new Xor("field", "value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }
}
