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
public class NotIn2025Test extends TestBase {

    @Test
    public void testConstructor_ValidList() {
        List<String> values = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", values);

        assertEquals("status", condition.getPropName());
        assertEquals(3, condition.getValues().size());
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    public void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", null));
    }

    @Test
    public void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", Arrays.asList()));
    }

    @Test
    public void testConstructor_Set() {
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn condition = new NotIn("department_id", excludedDepts);

        assertEquals("department_id", condition.getPropName());
        assertEquals(3, condition.getValues().size());
    }

    @Test
    public void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn condition = new NotIn("status", values);

        assertNotSame(values, condition.getValues());
    }

    @Test
    public void testGetPropName() {
        NotIn condition = new NotIn("status", Arrays.asList("inactive"));
        assertEquals("status", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        NotIn condition = new NotIn("id", values);

        List<?> result = condition.getValues();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testSetValues() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        List<String> newValues = Arrays.asList("inactive", "archived");

        condition.setValues(newValues);
        assertEquals(2, condition.getValues().size());
    }

    @Test
    public void testGetParameters() {
        List<String> values = Arrays.asList("deleted", "archived");
        NotIn condition = new NotIn("status", values);

        List<Object> params = condition.getParameters();
        assertEquals(2, params.size());
        assertEquals("deleted", params.get(0));
        assertEquals("archived", params.get(1));
    }

    @Test
    public void testClearParameters() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        condition.clearParameters();

        List<?> values = condition.getValues();
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals(null, values.get(1));
    }

    @Test
    public void testCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn original = new NotIn("status", values);

        NotIn copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals(original.getValues().size(), copy.getValues().size());
    }

    @Test
    public void testCopy_DeepCopy() {
        NotIn original = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn copy = original.copy();

        assertNotSame(original.getValues(), copy.getValues());

        // Modify original
        original.clearParameters();

        // Copy should not be affected
        assertEquals("deleted", copy.getValues().get(0));
    }

    @Test
    public void testToString_NoChange() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("NOT IN"));
        assertTrue(result.contains("deleted"));
        assertTrue(result.contains("archived"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotIn condition = new NotIn("orderStatus", Arrays.asList("deleted"));
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testHashCode_Equal() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted", "archived"));

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("state", Arrays.asList("deleted"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentValues() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_ExcludeInactiveStatuses() {
        // Exclude inactive statuses
        List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", inactiveStatuses);

        assertEquals(3, condition.getParameters().size());
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    public void testUseCaseScenario_ExcludeDepartments() {
        // Exclude specific department IDs
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn deptCondition = new NotIn("department_id", excludedDepts);

        assertEquals(3, deptCondition.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_ExcludeTestUsers() {
        // Exclude test users
        List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
        NotIn emailCondition = new NotIn("email", testEmails);

        assertEquals(2, emailCondition.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_ExcludeCategories() {
        // Exclude specific product categories
        List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
        NotIn notIn = new NotIn("category", excludedCategories);

        assertEquals(3, notIn.getParameters().size());
        assertTrue(notIn.toString(NamingPolicy.NO_CHANGE).contains("category"));
    }

    @Test
    public void testSetValues_Null() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertThrows(IllegalArgumentException.class, () -> condition.setValues(null));
    }

    @Test
    public void testSetValues_Empty() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertThrows(IllegalArgumentException.class, () -> condition.setValues(Arrays.asList()));
    }

    @Test
    public void testEquals_DifferentType() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        String other = "not a NotIn";
        assertNotEquals(condition, other);
    }

    @Test
    public void testConstructor_SingleValue() {
        NotIn condition = new NotIn("type", Arrays.asList("test"));

        assertEquals("type", condition.getPropName());
        assertEquals(1, condition.getValues().size());
    }

    @Test
    public void testAnd() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn cond2 = new NotIn("type", Arrays.asList("test", "demo"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn cond2 = new NotIn("status", Arrays.asList("archived"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}
