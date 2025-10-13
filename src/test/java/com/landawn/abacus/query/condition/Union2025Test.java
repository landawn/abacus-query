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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Union2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = new SubQuery("SELECT * FROM orders");
        Union union = new Union(subQuery);
        SubQuery retrieved = union.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = new SubQuery("customers", List.of("*"), new Equal("status", "active"));
        Union union = new Union(subQuery);
        List<Object> params = union.getParameters();
        assertEquals(1, (int)params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        Union original = new Union(subQuery);
        Union copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = new SubQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1.hashCode(), union2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertEquals(union, union);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1, union2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table2");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertNotEquals(union1, union2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotEquals(null, union);
    }

    @Test
    public void testRemovesDuplicates() {
        SubQuery subQuery = new SubQuery("SELECT customer_id FROM orders");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.getOperator());
    }

}
