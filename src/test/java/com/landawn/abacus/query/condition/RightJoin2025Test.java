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
public class RightJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        RightJoin join = new RightJoin("departments");
        assertNotNull(join);
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        RightJoin join = new RightJoin("departments", new Equal("employees.dept_id", "departments.id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders", "order_items");
        RightJoin join = new RightJoin(entities, new Equal("orders.id", "order_items.order_id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        RightJoin join = new RightJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        RightJoin join = new RightJoin("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        RightJoin join = new RightJoin("customers");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        RightJoin join = new RightJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        RightJoin join = new RightJoin("products", new Equal("active", true));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testClearParameters() {
        RightJoin join = new RightJoin("products", new Equal("status", "available"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        RightJoin original = new RightJoin("products", new Equal("category_id", (Object) 789));
        RightJoin copy = (RightJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        RightJoin join = new RightJoin("departments");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    public void testToString_WithCondition() {
        RightJoin join = new RightJoin("products p", new Equal("order_items.product_id", "p.id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("products p"));
    }

    @Test
    public void testHashCode() {
        RightJoin join1 = new RightJoin("orders", new Equal("a", "b"));
        RightJoin join2 = new RightJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        RightJoin join = new RightJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        RightJoin join1 = new RightJoin("orders o", new Equal("a", "b"));
        RightJoin join2 = new RightJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        RightJoin join1 = new RightJoin("orders");
        RightJoin join2 = new RightJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        RightJoin join = new RightJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("orders.product_id", "products.id"), new Equal("products.active", true)));
        RightJoin join = new RightJoin("products", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testAllRightTableRows() {
        RightJoin join = new RightJoin("customers", new Equal("orders.customer_id", "customers.id"));
        assertNotNull(join);
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    public void testMultiTableRightJoin() {
        List<String> tables = Arrays.asList("categories", "subcategories");
        And condition = new And(Arrays.asList(new Equal("products.category_id", "categories.id"), new Equal("products.subcategory_id", "subcategories.id")));
        RightJoin join = new RightJoin(tables, condition);
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testFindMissingRelationships() {
        RightJoin join = new RightJoin("products p", new Equal("order_items.product_id", "p.id"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }
}
