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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class InnerJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        InnerJoin join = new InnerJoin("orders");
        assertNotNull(join);
        assertEquals(Operator.INNER_JOIN, join.getOperator());
    }

    @Test
    public void testConstructor_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.INNER_JOIN, join.getOperator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        InnerJoin join = new InnerJoin(entities, new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.INNER_JOIN, join.getOperator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        InnerJoin join = new InnerJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        InnerJoin join = new InnerJoin("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        InnerJoin join = new InnerJoin("orders");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        InnerJoin join = new InnerJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("status", "active"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        InnerJoin join = new InnerJoin("orders o", new Equal("status", "pending"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        InnerJoin original = new InnerJoin("orders o", new Equal("customer_id", (Object) 123));
        InnerJoin copy = (InnerJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        InnerJoin join = new InnerJoin("orders");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INNER JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INNER JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        InnerJoin join1 = new InnerJoin("orders", new Equal("a", "b"));
        InnerJoin join2 = new InnerJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        InnerJoin join = new InnerJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        InnerJoin join1 = new InnerJoin("orders o", new Equal("a", "b"));
        InnerJoin join2 = new InnerJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        InnerJoin join1 = new InnerJoin("orders");
        InnerJoin join2 = new InnerJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        InnerJoin join = new InnerJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testAnd() {
        InnerJoin join1 = new InnerJoin("orders");
        InnerJoin join2 = new InnerJoin("products");
        And result = join1.and(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        InnerJoin join1 = new InnerJoin("orders");
        InnerJoin join2 = new InnerJoin("products");
        Or result = join1.or(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        InnerJoin join = new InnerJoin("orders");
        Not result = join.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", (Object) 100)));
        InnerJoin join = new InnerJoin("orders o", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testWithAlias() {
        InnerJoin join = new InnerJoin("order_details od");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("order_details od"));
    }

    @Test
    public void testWithCompositeKey() {
        And compositeKey = new And(Arrays.asList(new Equal("orders.id", "oi.order_id"), new Equal("orders.version", "oi.order_version")));
        InnerJoin join = new InnerJoin("order_items oi", compositeKey);
        assertNotNull(join.getCondition());
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testWithMultipleConditions() {
        And multipleConditions = new And(
                Arrays.asList(new Equal("order_items.product_id", "p.id"), new Equal("p.active", true), new GreaterThan("p.stock", (Object) 0)));
        InnerJoin join = new InnerJoin("products p", multipleConditions);
        assertEquals(3, (int) join.getParameters().size());
    }
}
