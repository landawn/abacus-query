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
public class LeftJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        LeftJoin join = new LeftJoin("orders");
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        LeftJoin join = new LeftJoin(entities, new And(Arrays.asList(new Equal("c.id", "o.customer_id"), new Equal("o.id", "oi.order_id"))));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        LeftJoin join = new LeftJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        LeftJoin join = new LeftJoin("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        LeftJoin join = new LeftJoin("departments");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        LeftJoin join = new LeftJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("status", "active"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        LeftJoin join = new LeftJoin("orders o", new Equal("status", "pending"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        LeftJoin original = new LeftJoin("orders o", new Equal("customer_id", (Object) 456));
        LeftJoin copy = (LeftJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        LeftJoin join = new LeftJoin("orders");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        LeftJoin join1 = new LeftJoin("orders", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LeftJoin join = new LeftJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        LeftJoin join1 = new LeftJoin("orders o", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        LeftJoin join1 = new LeftJoin("orders");
        LeftJoin join2 = new LeftJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        LeftJoin join = new LeftJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("customers.id", "o.customer_id"), new Equal("o.status", "active")));
        LeftJoin join = new LeftJoin("orders o", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testWithAlias() {
        LeftJoin join = new LeftJoin("employee_departments ed");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("employee_departments ed"));
    }

    @Test
    public void testFindMissingRecords() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.customer_id", "o.customer_id"));
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testWithAdditionalFilters() {
        And multipleConditions = new And(
                Arrays.asList(new Equal("orders.id", "oi.order_id"), new Equal("oi.status", "active"), new GreaterThan("oi.created_date", "2023-01-01")));
        LeftJoin join = new LeftJoin("order_items oi", multipleConditions);
        assertEquals(3, (int) join.getParameters().size());
    }

    @Test
    public void testPreserveLeftTableRows() {
        LeftJoin join = new LeftJoin("departments d", new Equal("employees.dept_id", "d.id"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }
}
