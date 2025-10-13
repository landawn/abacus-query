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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class InSubQuery2025Test extends TestBase {

    @Test
    public void testConstructor_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", subQuery);

        assertEquals("customer_id", condition.getPropName());
        assertNull(condition.getPropNames());
        assertNotNull(condition.getSubQuery());
        assertEquals(Operator.IN, condition.getOperator());
    }

    @Test
    public void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("id", null));
    }

    @Test
    public void testConstructor_MultipleProperties() {
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = new SubQuery("SELECT dept_id, location_id FROM dept_locations WHERE active = 'Y'");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertNull(condition.getPropName());
        assertNotNull(condition.getPropNames());
        assertEquals(2, condition.getPropNames().size());
        assertNotNull(condition.getSubQuery());
    }

    @Test
    public void testConstructor_EmptyPropNames() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    public void testConstructor_NullPropNames() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery((Collection<String>)null, subQuery));
    }

    @Test
    public void testGetPropName_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals("user_id", condition.getPropName());
    }

    @Test
    public void testGetPropName_MultipleProperties() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = new SubQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertNull(condition.getPropName());
    }

    @Test
    public void testGetPropNames_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertNull(condition.getPropNames());
    }

    @Test
    public void testGetPropNames_MultipleProperties() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = new SubQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        Collection<String> propNames = condition.getPropNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    public void testGetSubQuery() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        SubQuery result = condition.getSubQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    public void testSetSubQuery() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM users WHERE status = 'active'");
        InSubQuery condition = new InSubQuery("user_id", subQuery1);

        SubQuery subQuery2 = new SubQuery("SELECT id FROM users WHERE status = 'inactive'");
        condition.setSubQuery(subQuery2);

        assertEquals(subQuery2, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_WithSubQueryParams() {
        Equal statusCondition = new Equal("status", "active");
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), statusCondition);
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Equal statusCondition = new Equal("status", "active");
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), statusCondition);
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        condition.clearParameters();
        // SubQuery parameters should be cleared
        assertNotNull(condition.getSubQuery());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
        InSubQuery original = new InSubQuery("user_id", subQuery);

        InSubQuery copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getSubQuery(), copy.getSubQuery());
    }

    @Test
    public void testCopy_MultipleProperties() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = new SubQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery original = new InSubQuery(columns, subQuery);

        InSubQuery copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getSubQuery(), copy.getSubQuery());
    }

    @Test
    public void testToString_SingleProperty_NoChange() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("user_id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_MultipleProperties_NoChange() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = new SubQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("dept_id"));
        assertTrue(result.contains("loc_id"));
        assertTrue(result.contains("IN"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("userId", subQuery);

        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_id"));
    }

    @Test
    public void testHashCode_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM users");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("customer_id", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentSubQuery() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM users");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_PremiumCustomers() {
        // Find orders from premium customers
        SubQuery premiumCustomers = new SubQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);

        assertNotNull(condition.getSubQuery());
        assertTrue(condition.toString(NamingPolicy.NO_CHANGE).contains("customer_id"));
    }

    @Test
    public void testUseCaseScenario_MultiColumnAssignments() {
        // Find employees in specific department/location combinations
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery validAssignments = new SubQuery("SELECT dept_id, location_id FROM allowed_assignments");
        InSubQuery multiColumn = new InSubQuery(columns, validAssignments);

        assertEquals(2, multiColumn.getPropNames().size());
    }

    @Test
    public void testUseCaseScenario_ActiveCategories() {
        // Find all products in active categories
        SubQuery activeCategories = new SubQuery("SELECT category_id FROM categories WHERE active = true");
        InSubQuery condition = new InSubQuery("category_id", activeCategories);

        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("category_id"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testUseCaseScenario_ComplexSubQuery() {
        // Using structured subquery
        Condition activeCondition = new Equal("active", true);
        SubQuery subQuery = new SubQuery("categories", Arrays.asList("id"), activeCondition);
        InSubQuery condition = new InSubQuery("category_id", subQuery);

        assertNotNull(condition.getParameters());
    }
}
