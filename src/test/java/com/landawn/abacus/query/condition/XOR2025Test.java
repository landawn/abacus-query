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
public class XOR2025Test extends TestBase {

    @Test
    public void testConstructor() {
        XOR condition = new XOR("field", true);
        assertEquals("field", condition.getPropName());
        assertEquals(true, condition.getPropValue());
        assertEquals(Operator.XOR, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new XOR(null, true));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new XOR("", true));
    }

    @Test
    public void testGetPropName() {
        XOR condition = new XOR("isPremium", true);
        assertEquals("isPremium", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        XOR condition = new XOR("hasDiscount", false);
        Boolean value = condition.getPropValue();
        assertEquals(false, value);
    }

    @Test
    public void testGetOperator() {
        XOR condition = new XOR("field", "value");
        assertEquals(Operator.XOR, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        XOR condition = new XOR("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        XOR condition = new XOR("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testCopy() {
        XOR original = new XOR("name", "John");
        XOR copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        XOR condition = new XOR("hasGoldMembership", true);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("hasGoldMembership"));
        assertTrue(result.contains("XOR"));
    }

    @Test
    public void testHashCode() {
        XOR cond1 = new XOR("field", true);
        XOR cond2 = new XOR("field", true);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        XOR condition = new XOR("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        XOR cond1 = new XOR("status", "active");
        XOR cond2 = new XOR("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        XOR cond1 = new XOR("field1", "value");
        XOR cond2 = new XOR("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        XOR condition = new XOR("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testAnd() {
        XOR cond1 = new XOR("a", 1);
        XOR cond2 = new XOR("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        XOR cond1 = new XOR("a", 1);
        XOR cond2 = new XOR("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        XOR condition = new XOR("field", "value");
        Not result = condition.not();
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetPropValue() {
        XOR condition = new XOR("flag", true);
        assertEquals(true, condition.getPropValue());

        condition.setPropValue(false);
        assertEquals(false, condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        XOR condition = new XOR("isActive", true);
        String result = condition.toString();

        assertTrue(result.contains("isActive"));
        assertTrue(result.contains("XOR"));
    }

    @Test
    public void testEquals_DifferentPropValue() {
        XOR cond1 = new XOR("field", "value1");
        XOR cond2 = new XOR("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentClass() {
        XOR condition = new XOR("field", "value");
        Equal equal = new Equal("field", "value");
        assertNotEquals(condition, (Object) equal);
    }

    @Test
    public void testGetParameters_WithNestedCondition() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        XOR condition = new XOR("userId", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testCopy_DeepCopy() {
        XOR original = new XOR("value", 100);
        XOR copy = original.copy();

        original.clearParameters();
        assertNull(original.getPropValue());
        assertEquals(100, (int) copy.getPropValue());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        XOR condition = new XOR("userName", "john");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertTrue(result.contains("user_name") || result.contains("userName"));
    }

    @Test
    public void testHashCode_DifferentValues() {
        XOR cond1 = new XOR("field", "value1");
        XOR cond2 = new XOR("field", "value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }
}
