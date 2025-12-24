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
public class Except2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(subQuery);
        SubQuery retrieved = except.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123");
        Except except = new Except(subQuery);
        List<Object> params = except.getParameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("employees", List.of("employee_id"), new Equal("is_manager", "true"));
        Except except = new Except(subQuery);
        assertFalse(except.getParameters().isEmpty());
        except.clearParameters();
        List<Object> params = except.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Except original = new Except(subQuery);
        Except copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1.hashCode(), except2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(except, except);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1, except2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertNotEquals(except1, except2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(null, except);
    }

    @Test
    public void testSetDifference() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(customersWithOrders);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.getOperator());
    }

    @Test
    public void testFindMissingRecords() {
        SubQuery soldProducts = Filters.subQuery("order_items", List.of("product_id"), new GreaterThan("order_date", "2024-01-01"));
        Except except = new Except(soldProducts);
        assertEquals(1, (int) except.getParameters().size());
    }

    @Test
    public void testAnd_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            except.and(otherCondition);
        });
    }

    @Test
    public void testOr_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            except.or(otherCondition);
        });
    }

    @Test
    public void testNot_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);

        assertThrows(UnsupportedOperationException.class, () -> {
            except.not();
        });
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(Operator.EXCEPT, except.getOperator());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Except except = new Except(subQuery1);
        except.setCondition(subQuery2);
        assertEquals(subQuery2, except.getCondition());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toString();
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testCopy_DeepCopy() {
        SubQuery subQuery = Filters.subQuery("employees", List.of("employee_id"), new Equal("is_manager", "true"));
        Except original = new Except(subQuery);
        Except copy = original.copy();

        // Modify original's subquery
        original.clearParameters();

        // Copy should not be affected
        List<Object> copyParams = copy.getParameters();
        assertEquals("true", copyParams.get(0));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(except, "not an Except");
        assertNotEquals(except, new Union(subQuery));
    }
}
