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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class In2025Test extends TestBase {

    @Test
    public void testConstructor_ValidList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        assertEquals("status", condition.getPropName());
        assertEquals(3, condition.getValues().size());
        assertEquals(Operator.IN, condition.getOperator());
    }

    @Test
    public void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", null));
    }

    @Test
    public void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", Arrays.asList()));
    }

    @Test
    public void testConstructor_Set() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        assertEquals("user_id", condition.getPropName());
        assertEquals(5, condition.getValues().size());
    }

    @Test
    public void testConstructor_SingleValue() {
        In condition = new In("category", Arrays.asList("electronics"));

        assertEquals("category", condition.getPropName());
        assertEquals(1, condition.getValues().size());
    }

    @Test
    public void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        In condition = new In("grade", values);

        assertEquals(3, condition.getValues().size());
        assertNotSame(values, condition.getValues());
    }

    @Test
    public void testGetPropName() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals("status", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        In condition = new In("id", values);

        List<?> result = condition.getValues();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testSetValues() {
        In condition = new In("status", Arrays.asList("active"));
        List<String> newValues = Arrays.asList("inactive", "deleted");

        condition.setValues(newValues);
        assertEquals(2, condition.getValues().size());
    }

    @Test
    public void testSetValues_Null() {
        In condition = new In("status", Arrays.asList("active"));
        assertThrows(IllegalArgumentException.class, () -> condition.setValues(null));
    }

    @Test
    public void testSetValues_Empty() {
        In condition = new In("status", Arrays.asList("active"));
        assertThrows(IllegalArgumentException.class, () -> condition.setValues(Arrays.asList()));
    }

    @Test
    public void testGetParameters() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        List<Object> params = condition.getParameters();
        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals("pending", params.get(1));
        assertEquals("approved", params.get(2));
    }

    @Test
    public void testGetParameters_Integers() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        In condition = new In("id", values);

        List<Object> params = condition.getParameters();
        assertEquals(3, params.size());
        assertEquals(Integer.valueOf(10), params.get(0));
    }

    @Test
    public void testClearParameters() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        condition.clearParameters();

        List<?> values = condition.getValues();
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals(null, values.get(1));
    }

    @Test
    public void testCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        In original = new In("grade", values);

        In copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals(original.getValues().size(), copy.getValues().size());
    }

    @Test
    public void testCopy_DeepCopy() {
        In original = new In("status", Arrays.asList("active", "pending"));
        In copy = original.copy();

        assertNotSame(original.getValues(), copy.getValues());

        // Modify original
        original.clearParameters();

        // Copy should not be affected
        assertEquals("active", copy.getValues().get(0));
    }

    @Test
    public void testToString_NoChange() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("active"));
        assertTrue(result.contains("pending"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        In condition = new In("orderStatus", Arrays.asList("active"));
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("IN"));
    }

    @Test
    public void testToString_MultipleValues() {
        In condition = new In("id", Arrays.asList(1, 2, 3, 4, 5));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("5"));
    }

    @Test
    public void testHashCode_Equal() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("state", Arrays.asList("active"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentValues() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        In condition = new In("status", Arrays.asList("active"));
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentType() {
        In condition = new In("status", Arrays.asList("active"));
        String other = "not an In";
        assertNotEquals(condition, other);
    }

    @Test
    public void testUseCaseScenario_StatusFilter() {
        // Check if status is one of several values
        List<String> statuses = Arrays.asList("active", "pending", "approved");
        In statusCheck = new In("status", statuses);

        assertEquals(3, statusCheck.getParameters().size());
        assertTrue(statusCheck.toString(NamingPolicy.NO_CHANGE).contains("IN"));
    }

    @Test
    public void testUseCaseScenario_UserIdFilter() {
        // Check if user_id is in a list
        List<Integer> userIds = Arrays.asList(1, 2, 3, 5, 8);
        In userFilter = new In("user_id", userIds);

        assertEquals(5, userFilter.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_CategoryFilter() {
        // Filter by categories
        Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
        In categoryFilter = new In("category", categories);

        assertEquals(2, categoryFilter.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_EnumValues() {
        // Filter by enum values
        List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
        In priorityFilter = new In("priority", priorities);

        String sql = priorityFilter.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("priority"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testUseCaseScenario_LargeList() {
        // Test with larger list
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        In condition = new In("id", largeList);

        assertEquals(10, condition.getParameters().size());
    }

    @Test
    public void testAnd() {
        In cond1 = new In("status", Arrays.asList("active", "pending"));
        In cond2 = new In("type", Arrays.asList("premium", "gold"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        In cond1 = new In("status", Arrays.asList("active"));
        In cond2 = new In("status", Arrays.asList("pending"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }
}
