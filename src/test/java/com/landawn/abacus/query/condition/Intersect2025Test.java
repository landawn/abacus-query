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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Intersect2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders");
        Intersect intersect = new Intersect(subQuery);
        SubQuery retrieved = intersect.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(subQuery);
        List<Object> params = intersect.getParameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("activity", List.of("user_id"), new GreaterThan("last_login", "2024-01-01"));
        Intersect intersect = new Intersect(subQuery);
        assertFalse(intersect.getParameters().isEmpty());
        intersect.clearParameters();
        List<Object> params = intersect.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Intersect original = new Intersect(subQuery);
        Intersect copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1.hashCode(), intersect2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(intersect, intersect);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertNotEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(null, intersect);
    }

    @Test
    public void testFindCommonElements() {
        SubQuery activeUsers = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > ?", "2023-01-01");
        Intersect intersect = new Intersect(activeUsers);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.getOperator());
    }

    @Test
    public void testSetIntersection() {
        SubQuery onSale = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(onSale);
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) intersect.getParameters().size());
    }

    @Test
    public void testAnd_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            intersect.and(otherCondition);
        });
    }

    @Test
    public void testOr_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            intersect.or(otherCondition);
        });
    }

    @Test
    public void testNot_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);

        assertThrows(UnsupportedOperationException.class, () -> {
            intersect.not();
        });
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(Operator.INTERSECT, intersect.getOperator());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Intersect intersect = new Intersect(subQuery1);
        intersect.setCondition(subQuery2);
        assertEquals(subQuery2, intersect.getCondition());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString();
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    public void testCopy_DeepCopy() {
        SubQuery subQuery = Filters.subQuery("activity", List.of("user_id"), new GreaterThan("last_login", "2024-01-01"));
        Intersect original = new Intersect(subQuery);
        Intersect copy = original.copy();

        // Modify original's subquery
        original.clearParameters();

        // Copy should not be affected
        List<Object> copyParams = copy.getParameters();
        assertEquals("2024-01-01", copyParams.get(0));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(intersect, "not an Intersect");
        assertNotEquals(intersect, new Union(subQuery));
    }
}
