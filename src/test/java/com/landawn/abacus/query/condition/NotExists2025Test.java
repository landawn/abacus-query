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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class NotExists2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = users.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.NOT_EXISTS, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("status", "inactive");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products");
        NotExists condition = new NotExists(subQuery);
        assertEquals(Operator.NOT_EXISTS, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = false");
        NotExists condition = new NotExists(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "cancelled");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("cancelled", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new Equal("type", "temporary");
        SubQuery subQuery = Filters.subQuery("sessions", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM reviews WHERE rating < 3");
        NotExists original = new NotExists(subQuery);
        NotExists copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.operator(), copy.operator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE status = 'cancelled'");
        NotExists condition = new NotExists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertEquals("NOT EXISTS (SELECT 1 FROM orders WHERE status = 'cancelled')", result);
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new Equal("deleted", true);
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NOT EXISTS"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE deleted = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users WHERE deleted = true");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM invoices");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testFindCustomersWithoutOrders() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        NotExists condition = new NotExists(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("customer_id"));
    }

    @Test
    public void testFindProductsWithoutReviews() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
    }

    @Test
    public void testSubQueryWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("cancelled", true), new LessThan("amount", (Object) 10)));
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), andCondition);
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testCopyIndependence() {
        Condition whereCondition = new Equal("deleted", true);
        SubQuery subQuery = Filters.subQuery("records", Arrays.asList("id"), whereCondition);
        NotExists original = new NotExists(subQuery);
        NotExists copy = original.copy();

        copy.clearParameters();
        assertFalse(original.getParameters().isEmpty());
        List<Object> copyParams = copy.getParameters();
        assertTrue(copyParams.size() == 1 && copyParams.stream().allMatch(param -> param == null));
    }

    @Test
    public void testOrphanedRecordsCheck() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM parent_table WHERE parent_table.id = child_table.parent_id");
        NotExists condition = new NotExists(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("parent_id"));
    }

    @Test
    public void testEmployeesWithoutProjects() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM project_assignments WHERE project_assignments.employee_id = employees.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
    }
}
