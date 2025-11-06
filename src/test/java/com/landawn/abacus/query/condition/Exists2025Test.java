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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Exists2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders WHERE customer_id = users.id");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.EXISTS, condition.getOperator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = new SubQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM products");
        Exists condition = new Exists(subQuery);
        assertEquals(Operator.EXISTS, condition.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE active = true");
        Exists condition = new Exists(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = new SubQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new Equal("status", "pending");
        SubQuery subQuery = new SubQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = new SubQuery("SELECT id FROM products WHERE price > 100");
        Exists original = new Exists(subQuery);
        Exists copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders WHERE status = 'active'");
        Exists condition = new Exists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new Equal("active", true);
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "name"), whereCondition);
        Exists condition = new Exists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = new SubQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = new SubQuery("SELECT 1 FROM orders");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = new SubQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = new SubQuery("SELECT id FROM users WHERE active = true");
        SubQuery subQuery2 = new SubQuery("SELECT id FROM users WHERE active = true");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = new SubQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = new SubQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        SubQuery subQuery1 = new SubQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = new SubQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        SubQuery subQuery1 = new SubQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = new SubQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertTrue(condition instanceof Cell);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testCorrelatedSubQuery() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM orders o WHERE o.customer_id = customers.id");
        Exists condition = new Exists(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("EXISTS"));
        assertTrue(sql.contains("customer_id"));
    }

    @Test
    public void testComplexSubQueryWithJoin() {
        SubQuery subQuery = new SubQuery("SELECT 1 FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status = 'active'");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testSubQueryWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("total", (Object) 100)));
        SubQuery subQuery = new SubQuery("orders", Arrays.asList("id"), andCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testCopyIndependence() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = new SubQuery("orders", Arrays.asList("id"), whereCondition);
        Exists original = new Exists(subQuery);
        Exists copy = original.copy();

        copy.clearParameters();
        assertFalse(original.getParameters().isEmpty());
        List<Object> copyParams = copy.getParameters();
        assertTrue(copyParams.size() == 1 && copyParams.stream().allMatch(param -> param == null));
    }
}
