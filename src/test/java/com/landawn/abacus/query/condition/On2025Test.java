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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class On2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleEquality() {
        On on = new On("orders.customer_id", "customers.id");
        assertNotNull(on);
        assertEquals(Operator.ON, on.getOperator());
    }

    @Test
    public void testConstructor_WithCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        On on = new On(condition);
        assertNotNull(on);
        assertNotNull(on.getCondition());
        assertEquals(Operator.ON, on.getOperator());
    }

    @Test
    public void testConstructor_WithMap() {
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("emp.department_id", "dept.id");
        joinMap.put("emp.location_id", "dept.location_id");
        On on = new On(joinMap);
        assertNotNull(on);
        assertEquals(Operator.ON, on.getOperator());
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("users.id", "posts.user_id");
        On on = new On(condition);
        Condition retrieved = on.getCondition();
        assertNotNull(retrieved);
    }

    @Test
    public void testGetParameters_Empty() {
        On on = new On("a.id", "b.id");
        List<Object> params = on.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_WithValues() {
        And complexCondition = new And(
            new Equal("orders.customer_id", "customers.id"),
            new Equal("orders.status", "active")
        );
        On on = new On(complexCondition);
        List<Object> params = on.getParameters();
        assertEquals(2, (int)params.size());
        assertEquals("customers.id", params.get(0));
        assertEquals("active", params.get(1));
    }

    @Test
    public void testClearParameters() {
        And condition = new And(
            new Equal("a.id", "b.id"),
            new Equal("a.status", "pending")
        );
        On on = new On(condition);
        assertFalse(on.getParameters().isEmpty());
        on.clearParameters();
        List<Object> params = on.getParameters();
        assertTrue(params.size() == 2 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        On original = new On("orders.customer_id", "customers.id");
        On copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString() {
        On on = new On("orders.customer_id", "customers.id");
        String result = on.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ON"));
    }

    @Test
    public void testHashCode() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1.hashCode(), on2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        On on = new On("a.id", "b.id");
        assertEquals(on, on);
    }

    @Test
    public void testEquals_EqualObjects() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1, on2);
    }

    @Test
    public void testEquals_DifferentColumns() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("c.id", "d.id");
        assertNotEquals(on1, on2);
    }

    @Test
    public void testEquals_Null() {
        On on = new On("a.id", "b.id");
        assertNotEquals(null, on);
    }

    @Test
    public void testCreateOnCondition_Simple() {
        Condition condition = On.createOnCondition("users.id", "posts.user_id");
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    public void testCreateOnCondition_Map_SingleEntry() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    public void testCreateOnCondition_Map_MultipleEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        map.put("a.code", "b.code");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof And);
    }

    @Test
    public void testComplexCondition() {
        And complexCondition = new And(
            new Equal("orders.customer_id", "customers.id"),
            new GreaterThan("orders.order_date", "customers.registration_date")
        );
        On on = new On(complexCondition);
        assertNotNull(on.getCondition());
    }

    @Test
    public void testCompositeKey() {
        Map<String, String> compositeKey = new LinkedHashMap<>();
        compositeKey.put("order_items.order_id", "orders.id");
        compositeKey.put("order_items.customer_id", "orders.customer_id");
        On on = new On(compositeKey);
        assertNotNull(on.getCondition());
    }

    @Test
    public void testWithAdditionalFilters() {
        And filteredJoin = new And(
            new Equal("products.category_id", "categories.id"),
            new Equal("categories.active", true)
        );
        On on = new On(filteredJoin);
        assertEquals(2, (int)on.getParameters().size());
    }
}
