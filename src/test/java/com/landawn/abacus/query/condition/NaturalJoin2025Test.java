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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class NaturalJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNotNull(join);
        assertEquals(Operator.NATURAL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        NaturalJoin join = new NaturalJoin("departments", new Equal("status", "active"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.NATURAL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("customers", "orders", "products");
        NaturalJoin join = new NaturalJoin(entities, new GreaterThan("totalAmount", (Object) 1000));
        assertNotNull(join);
        assertEquals(3, (int) join.getJoinEntities().size());
        assertEquals(Operator.NATURAL_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        NaturalJoin join = new NaturalJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("status", "active");
        NaturalJoin join = new NaturalJoin("departments", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        NaturalJoin join = new NaturalJoin("employees");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        NaturalJoin join = new NaturalJoin("orders", new Equal("status", "completed"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("completed", params.get(0));
    }

    @Test
    public void testClearParameters() {
        NaturalJoin join = new NaturalJoin("orders", new GreaterThan("orderDate", "2024-01-01"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        NaturalJoin original = new NaturalJoin("customers", new Equal("type", "VIP"));
        NaturalJoin copy = (NaturalJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        NaturalJoin join = new NaturalJoin("employees");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NATURAL JOIN"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testToString_WithCondition() {
        NaturalJoin join = new NaturalJoin("departments", new Equal("active", true));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NATURAL JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    public void testHashCode() {
        NaturalJoin join1 = new NaturalJoin("employees", new Equal("a", "b"));
        NaturalJoin join2 = new NaturalJoin("employees", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NaturalJoin join = new NaturalJoin("employees");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        NaturalJoin join1 = new NaturalJoin("employees", new Equal("a", "b"));
        NaturalJoin join2 = new NaturalJoin("employees", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("departments");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNotEquals(null, join);
    }

    @Test
    public void testAnd() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("departments");
        assertThrows(UnsupportedOperationException.class, () -> join1.and(join2));
    }

    @Test
    public void testOr() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("departments");
        assertThrows(UnsupportedOperationException.class, () -> join1.or(join2));
    }

    @Test
    public void testNot() {
        NaturalJoin join = new NaturalJoin("employees");
        assertThrows(UnsupportedOperationException.class, () -> join.not());
    }

    @Test
    public void testAutomaticColumcountMatchBetweening() {
        NaturalJoin join = new NaturalJoin("customers");
        assertNotNull(join);
        assertNull(join.getCondition());
    }

    @Test
    public void testWithAdditionalFilter() {
        NaturalJoin join = new NaturalJoin("orders", new GreaterThan("orderDate", "2024-01-01"));
        assertNotNull(join.getCondition());
        assertEquals(1, (int) join.getParameters().size());
    }
}
