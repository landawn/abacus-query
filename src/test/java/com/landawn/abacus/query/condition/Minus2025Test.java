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

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Minus2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertNotNull(minus);
        assertEquals(Operator.MINUS, minus.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = new SubQuery("SELECT DISTINCT customer_id FROM orders");
        Minus minus = new Minus(subQuery);
        SubQuery retrieved = minus.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = new SubQuery("sales", List.of("product_id"), new Equal("region", "WEST"));
        Minus minus = new Minus(subQuery);
        List<Object> params = minus.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("WEST", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = new SubQuery("project_assignments", List.of("employee_id"), new Equal("active", "true"));
        Minus minus = new Minus(subQuery);
        assertFalse(minus.getParameters().isEmpty());
        minus.clearParameters();
        List<Object> params = minus.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        Minus original = new Minus(subQuery);
        Minus copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = new SubQuery("SELECT product_id FROM inventory");
        Minus minus = new Minus(subQuery);
        String result = minus.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("MINUS"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertEquals(minus1.hashCode(), minus2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertEquals(minus, minus);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertEquals(minus1, minus2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table2");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertNotEquals(minus1, minus2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertNotEquals(null, minus);
    }

    @Test
    public void testOracleStyle() {
        SubQuery customersWithOrders = new SubQuery("SELECT DISTINCT customer_id FROM orders");
        Minus minus = new Minus(customersWithOrders);
        assertNotNull(minus);
        assertEquals(Operator.MINUS, minus.getOperator());
    }

    @Test
    public void testFindUnsoldProducts() {
        SubQuery soldProducts = new SubQuery("SELECT product_id FROM sales");
        Minus minus = new Minus(soldProducts);
        assertEquals(Operator.MINUS, minus.getOperator());
    }

    @Test
    public void testFindUnassignedEmployees() {
        SubQuery assignedEmployees = new SubQuery("project_assignments", List.of("employee_id"), new Equal("status", "ACTIVE"));
        Minus minus = new Minus(assignedEmployees);
        assertEquals(1, (int) minus.getParameters().size());
    }
}
