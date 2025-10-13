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
public class Intersect2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = new SubQuery("SELECT customer_id FROM orders");
        Intersect intersect = new Intersect(subQuery);
        SubQuery retrieved = intersect.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = new SubQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(subQuery);
        List<Object> params = intersect.getParameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int)params.size());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = new SubQuery("activity", List.of("user_id"), new GreaterThan("last_login", "2024-01-01"));
        Intersect intersect = new Intersect(subQuery);
        assertFalse(intersect.getParameters().isEmpty());
        intersect.clearParameters();
        List<Object> params = intersect.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        Intersect original = new Intersect(subQuery);
        Intersect copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = new SubQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1.hashCode(), intersect2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(intersect, intersect);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM table2");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertNotEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(null, intersect);
    }

    @Test
    public void testFindCommonElements() {
        SubQuery activeUsers = new SubQuery("SELECT user_id FROM activity WHERE last_login > ?", "2023-01-01");
        Intersect intersect = new Intersect(activeUsers);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.getOperator());
    }

    @Test
    public void testSetIntersection() {
        SubQuery onSale = new SubQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(onSale);
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int)intersect.getParameters().size());
    }
}
