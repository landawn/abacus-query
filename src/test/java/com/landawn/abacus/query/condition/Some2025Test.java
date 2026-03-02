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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Some2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("category", "Budget");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        Some condition = new Some(subQuery);
        assertEquals(Operator.SOME, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("approved", true);
        SubQuery subQuery = Filters.subQuery("requests", Arrays.asList("amount"), whereCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new LessThan("priority", (Object) 5);
        SubQuery subQuery = Filters.subQuery("tasks", Arrays.asList("estimated_hours"), whereCondition);
        Some condition = new Some(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        Some original = new Some(subQuery);
        Some copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.operator(), copy.operator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("experience", (Object) 5);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("SOME"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Some condition = new Some(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Some cond1 = new Some(subQuery1);
        Some cond2 = new Some(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Some condition = new Some(subQuery);
        assertTrue(condition instanceof Cell);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanSomeScenario() {
        // salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some condition = new Some(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    public void testLessThanSomeScenario() {
        // price < SOME (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testSomeVsAnyEquivalence() {
        // SOME and ANY are functionally equivalent
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");

        Some someCondition = new Some(subQuery1);
        Any anyCondition = new Any(subQuery2);

        // Both should work the same way
        assertNotNull(someCondition);
        assertNotNull(anyCondition);
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("available", true), new GreaterThan("stock", (Object) 10)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Some condition = new Some(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testProjectCostComparison() {
        // project_cost < SOME (SELECT budget FROM departments)
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments");
        Some condition = new Some(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("SOME"));
    }

    @Test
    public void testManagerSalaryCheck() {
        // Find employees earning more than SOME managers
        Condition whereCondition = new Equal("is_manager", true);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Some condition = new Some(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testCopyIndependence() {
        Condition whereCondition = new Equal("category", "Premium");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        Some original = new Some(subQuery);
        Some copy = original.copy();

        copy.clearParameters();
        assertEquals(1, (int) original.getParameters().size());
        List<Object> copyParams = copy.getParameters();
        assertTrue(copyParams.size() == 1 && copyParams.stream().allMatch(param -> param == null));
    }
}
