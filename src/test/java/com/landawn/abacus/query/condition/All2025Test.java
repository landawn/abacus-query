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
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class All2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = new SubQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ALL, condition.getOperator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("department", "Sales");
        SubQuery subQuery = new SubQuery("employees", Arrays.asList("salary"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = new SubQuery("SELECT score FROM tests");
        All condition = new All(subQuery);
        assertEquals(Operator.ALL, condition.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = new SubQuery("SELECT price FROM products WHERE in_stock = true");
        All condition = new All(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = new SubQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("category", "Premium");
        SubQuery subQuery = new SubQuery("products", Arrays.asList("price"), whereCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int)params.size());
        assertEquals("Premium", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new Equal("active", true);
        SubQuery subQuery = new SubQuery("items", Arrays.asList("cost"), whereCondition);
        All condition = new All(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT salary FROM employees WHERE is_manager = true");
        All original = new All(subQuery);
        All copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = new SubQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("rating", (Object) 4);
        SubQuery subQuery = new SubQuery("reviews", Arrays.asList("score"), whereCondition);
        All condition = new All(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("reviews"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = new SubQuery("SELECT value FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT value FROM table1");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT value FROM table1");
        SubQuery subQuery2 = new SubQuery("SELECT value FROM table2");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = new SubQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = new SubQuery("SELECT salary FROM employees WHERE active = true");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT price FROM products");
        SubQuery subQuery2 = new SubQuery("SELECT cost FROM items");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = new SubQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        SubQuery subQuery1 = new SubQuery("SELECT price FROM products");
        SubQuery subQuery2 = new SubQuery("SELECT cost FROM items");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        SubQuery subQuery1 = new SubQuery("SELECT price FROM products");
        SubQuery subQuery2 = new SubQuery("SELECT cost FROM items");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = new SubQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = new SubQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertTrue(condition instanceof Cell);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanAllScenario() {
        // price > ALL (SELECT price FROM competitor_products)
        SubQuery subQuery = new SubQuery(
            "SELECT price FROM competitor_products WHERE product_type = 'Premium'"
        );
        All condition = new All(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
        assertTrue(sql.contains("competitor_products"));
    }

    @Test
    public void testLessThanAllScenario() {
        // cost < ALL (SELECT budget FROM departments)
        SubQuery subQuery = new SubQuery("SELECT budget FROM departments WHERE region = 'West'");
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testSalaryComparison() {
        // Find employees earning more than ALL managers
        SubQuery subQuery = new SubQuery(
            "SELECT salary FROM employees WHERE is_manager = true"
        );
        All condition = new All(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(
            Arrays.asList(
                new Equal("active", true),
                new Equal("category", "Electronics")
            )
        );
        SubQuery subQuery = new SubQuery("products", Arrays.asList("price"), andCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int)params.size());
    }

    @Test
    public void testScoreComparison() {
        // score >= ALL (SELECT avg_score FROM class_statistics)
        Condition whereCondition = new Equal("year", (Object) 2024);
        SubQuery subQuery = new SubQuery("class_statistics", Arrays.asList("avg_score"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }
}
