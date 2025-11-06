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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class FullJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        FullJoin join = new FullJoin("departments");
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }

    @Test
    public void testConstructor_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        FullJoin join = new FullJoin(entities, new Equal("departments.id", "person.dept_id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        FullJoin join = new FullJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("users.id", "orders.user_id");
        FullJoin join = new FullJoin("orders", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        FullJoin join = new FullJoin("departments");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        FullJoin join = new FullJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        FullJoin join = new FullJoin("products", new Equal("active", true));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testClearParameters() {
        FullJoin join = new FullJoin("products", new Equal("status", "available"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        FullJoin original = new FullJoin("products", new Equal("category_id", (Object) 111));
        FullJoin copy = (FullJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        FullJoin join = new FullJoin("departments");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    public void testToString_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        FullJoin join1 = new FullJoin("orders", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        FullJoin join = new FullJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        FullJoin join1 = new FullJoin("orders o", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        FullJoin join1 = new FullJoin("orders");
        FullJoin join2 = new FullJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        FullJoin join = new FullJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testAnd() {
        FullJoin join1 = new FullJoin("orders");
        FullJoin join2 = new FullJoin("products");
        And result = join1.and(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        FullJoin join1 = new FullJoin("orders");
        FullJoin join2 = new FullJoin("products");
        Or result = join1.or(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testAllRowsFromBothTables() {
        FullJoin join = new FullJoin("orders", new Equal("users.id", "orders.user_id"));
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }

    @Test
    public void testReconcileTwoDataSources() {
        FullJoin join = new FullJoin("external_users", new Equal("internal_users.email", "external_users.email"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }

    @Test
    public void testMultiTableFullJoin() {
        List<String> tables = Arrays.asList("system_a_data", "system_b_data");
        FullJoin join = new FullJoin(tables, new Equal("master_data.record_id", "source.record_id"));
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testFindDataMismatches() {
        FullJoin join = new FullJoin("warehouse_inventory", new Equal("online_inventory.product_id", "warehouse_inventory.product_id"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.getOperator());
    }
}
