package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class ExistsTest extends TestBase {

    @Test
    void testComplexExistsScenarios() {
        // Test customers with orders
        SubQuery hasOrders = Filters.subQuery("orders", Arrays.asList("1"),
                Filters.and(Filters.eq("orders.customer_id", "customers.id"), Filters.ge("order_date", "2023-01-01")));
        Exists customersWithRecentOrders = Filters.exists(hasOrders);

        assertNotNull(customersWithRecentOrders);
        var params = customersWithRecentOrders.parameters();
        assertTrue(params.contains("2023-01-01"));

        // Test products with stock
        SubQuery hasStock = Filters.subQuery("inventory", Arrays.asList("1"),
                Filters.and(Filters.eq("inventory.product_id", "products.id"), Filters.gt("quantity", 0)));
        Exists productsInStock = Filters.exists(hasStock);

        assertNotNull(productsInStock);
        params = productsInStock.parameters();
        assertTrue(params.contains(0));
    }

    @Test
    void testComplexSubQueryWithJoin() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status = 'active'");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        Exists exists = Filters.exists(subQuery);

        assertNotNull(exists);
        assertEquals(Operator.EXISTS, exists.operator());
        assertEquals(subQuery, exists.condition());
    }

    @Test
    void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = users.id");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.EXISTS, condition.operator());
    }

    @Test
    void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testCorrelatedSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders o WHERE o.customer_id = customers.id");
        Exists condition = new Exists(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("EXISTS"));
        assertTrue(sql.contains("customer_id"));
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery3 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 200");

        Exists exists1 = Filters.exists(subQuery1);
        Exists exists2 = Filters.exists(subQuery2);
        Exists exists3 = Filters.exists(subQuery3);

        assertEquals(exists1, exists1);
        assertEquals(exists1, exists2);
        assertNotEquals(exists1, exists3);
        assertNotEquals(exists1, null);
        assertNotEquals(exists1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Exists condition = new Exists(subQuery);
        SubQuery retrieved = (SubQuery) condition.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products");
        Exists condition = new Exists(subQuery);
        assertEquals(Operator.EXISTS, condition.operator());
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testNotExists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM blocked_users WHERE id = users.id");
        NotExists notExists = Filters.notExists(subQuery);

        assertNotNull(notExists);
        assertEquals(Operator.NOT_EXISTS, notExists.operator());
    }

    @Test
    void testNullSubQueryMessageNamesSubQuery() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Exists(null));
        assertTrue(e.getMessage().contains("subQuery"), e.getMessage());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), Filters.and(Filters.eq("customer_id", 123), Filters.eq("status", "pending")));
        Exists exists = Filters.exists(subQuery);

        var params = exists.parameters();
        assertEquals(2, params.size());
        assertTrue(params.contains(123));
        assertTrue(params.contains("pending"));
    }

    @Test
    void testParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testPerformanceConsideration() {
        // EXISTS with SELECT 1 (optimal)
        SubQuery optimal = Filters.subQuery("SELECT 1 FROM large_table WHERE condition = true");
        Exists exists1 = Filters.exists(optimal);

        // EXISTS with SELECT * (works but less optimal)
        SubQuery lessOptimal = Filters.subQuery("SELECT * FROM large_table WHERE condition = true");
        Exists exists2 = Filters.exists(lessOptimal);

        // Both should work correctly
        assertNotNull(exists1);
        assertNotNull(exists2);

        // The difference is only in the SQL string
        assertTrue(exists1.toString().contains("SELECT 1"));
        assertTrue(exists2.toString().contains("SELECT *"));
    }

    @Test
    void testSubQueryWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("total", 100)));
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), andCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM subordinates WHERE manager_id = emp.id");
        Exists exists = Filters.exists(subQuery);

        String result = exists.toString();
        assertEquals("EXISTS (SELECT 1 FROM subordinates WHERE manager_id = emp.id)", result);
    }

    @Test
    void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE status = 'active'");
        Exists condition = new Exists(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("EXISTS (SELECT 1 FROM orders WHERE status = 'active')", result);
    }

    @Test
    void testToString_WithStructuredQuery() {
        Condition whereCondition = new Equal("active", true);
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), whereCondition);
        Exists condition = new Exists(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("users"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orderItems WHERE orderId = orders.id");
        Exists exists = Filters.exists(subQuery);

        String result = exists.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("EXISTS"));
    }
}
