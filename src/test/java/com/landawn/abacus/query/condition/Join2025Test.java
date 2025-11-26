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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Join2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleJoin() {
        Join join = new Join("orders");
        assertNotNull(join);
        assertEquals(Operator.JOIN, join.getOperator());
    }

    @Test
    public void testConstructor_WithCondition() {
        Join join = new Join("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        Join join = new Join(Arrays.asList("orders o", "customers c"), new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testGetJoinEntities() {
        Join join = new Join(Arrays.asList("table1", "table2"), null);
        List<String> entities = join.getJoinEntities();
        assertEquals(2, (int) entities.size());
        assertTrue(entities.contains("table1"));
        assertTrue(entities.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        Join join = new Join("orders");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        Join join = new Join("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        Join join = new Join("orders o", new Equal("status", "active"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Join join = new Join("orders o", new Equal("status", "pending"));
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Join original = new Join("orders o", new Equal("customer_id", (Object) 123));
        Join copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        Join join = new Join("orders");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        Join join = new Join("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        Join join1 = new Join("orders", new Equal("a", "b"));
        Join join2 = new Join("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Join join = new Join("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        Join join1 = new Join("orders o", new Equal("a", "b"));
        Join join2 = new Join("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        Join join1 = new Join("orders");
        Join join2 = new Join("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        Join join = new Join("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testAnd() {
        Join join1 = new Join("orders");
        Join join2 = new Join("products");
        And result = join1.and(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Join join1 = new Join("orders");
        Join join2 = new Join("products");
        Or result = join1.or(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", (Object) 100)));
        Join join = new Join("orders o", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }
}
