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
public class Any2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("region", "West");
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), whereCondition);
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM exams");
        Any condition = new Any(subQuery);
        assertEquals(Operator.ANY, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Any condition = new Any(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM items");
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "published");
        SubQuery subQuery = Filters.subQuery("articles", Arrays.asList("views"), whereCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("published", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new GreaterThan("rating", (Object) 3);
        SubQuery subQuery = Filters.subQuery("reviews", Arrays.asList("score"), whereCondition);
        Any condition = new Any(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE in_stock = true");
        Any original = new Any(subQuery);
        Any copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.operator(), copy.operator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        Any condition = new Any(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new LessThan("age", (Object) 30);
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        Any condition = new Any(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ANY"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        Any condition = new Any(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        Any cond1 = new Any(subQuery1);
        Any cond2 = new Any(subQuery2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        Any condition = new Any(subQuery);
        assertTrue(condition instanceof Cell);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanAnyScenario() {
        // salary > ANY (SELECT salary FROM employees WHERE department = 'Sales')
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE department = 'Sales'");
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
        assertTrue(sql.contains("employees"));
    }

    @Test
    public void testEqualsAnyScenario() {
        // id = ANY (subquery) is equivalent to IN
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_sessions");
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testLessThanAnyScenario() {
        // price < ANY (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products WHERE available = true");
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("active", true), new GreaterThan("stock", (Object) 0)));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        Any condition = new Any(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testBudgetComparison() {
        // expense > ANY (SELECT budget FROM departments WHERE region = 'West')
        Condition whereCondition = new Equal("region", "West");
        SubQuery subQuery = Filters.subQuery("departments", Arrays.asList("budget"), whereCondition);
        Any condition = new Any(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testPassingScoreCheck() {
        // student_score > ANY (SELECT passing_score FROM exams WHERE subject = 'Math')
        Condition whereCondition = new Equal("subject", "Math");
        SubQuery subQuery = Filters.subQuery("exams", Arrays.asList("passing_score"), whereCondition);
        Any condition = new Any(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ANY"));
    }
}
