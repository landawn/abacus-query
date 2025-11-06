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
public class NotInSubQuery2025Test extends TestBase {

    @Test
    public void testConstructor_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals("userId", condition.getPropName());
        assertNull(condition.getPropNames());
        assertNotNull(condition.getSubQuery());
        assertEquals(Operator.NOT_IN, condition.getOperator());
    }

    @Test
    public void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("id", null));
    }

    @Test
    public void testConstructor_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = new SubQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        assertNull(condition.getPropName());
        assertNotNull(condition.getPropNames());
        assertEquals(2, condition.getPropNames().size());
    }

    @Test
    public void testConstructor_EmptyPropNames() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    public void testConstructor_NullPropNames() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery((Collection<String>) null, subQuery));
    }

    @Test
    public void testGetPropName() {
        SubQuery subQuery = new SubQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", subQuery);

        assertEquals("itemId", condition.getPropName());
    }

    @Test
    public void testGetPropNames() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery subQuery = new SubQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        Collection<String> propNames = condition.getPropNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    public void testGetSubQuery() {
        SubQuery subQuery = new SubQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        SubQuery result = condition.getSubQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    public void testSetSubQuery() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM users WHERE active = false");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery1);

        SubQuery subQuery2 = new SubQuery("SELECT id FROM users WHERE deleted = true");
        condition.setSubQuery(subQuery2);

        assertEquals(subQuery2, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Equal statusCondition = new Equal("status", "deleted");
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), statusCondition);
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        condition.clearParameters();
        assertNotNull(condition.getSubQuery());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM inactive_users");
        NotInSubQuery original = new NotInSubQuery("userId", subQuery);

        NotInSubQuery copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getSubQuery(), copy.getSubQuery());
    }

    @Test
    public void testToString_SingleProperty() {
        SubQuery subQuery = new SubQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userId"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testToString_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = new SubQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("lastName"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testHashCode_Equal() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("customerId", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_ExcludeDeletedItems() {
        SubQuery deletedItems = new SubQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);

        assertTrue(condition.toString(NamingPolicy.NO_CHANGE).contains("itemId"));
    }

    @Test
    public void testUseCaseScenario_ExcludeRestrictedLocations() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery restricted = new SubQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, restricted);

        assertEquals(2, condition.getPropNames().size());
    }
}
