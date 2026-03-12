package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Exists2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = users.id");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.EXISTS, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products");
        Exists condition = new Exists(subQuery);
        assertEquals(Operator.EXISTS, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Exists condition = new Exists(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Condition whereCondition = new Equal("status", "pending");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        Exists condition = new Exists(subQuery);

        condition.clearParameters();
        List<Object> params = condition.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE status = 'active'");
        Exists condition = new Exists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertEquals("EXISTS (SELECT 1 FROM orders WHERE status = 'active')", result);
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new Equal("active", true);
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), whereCondition);
        Exists condition = new Exists(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        Exists cond1 = new Exists(subQuery1);
        Exists cond2 = new Exists(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        Exists condition = new Exists(subQuery);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testCorrelatedSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders o WHERE o.customer_id = customers.id");
        Exists condition = new Exists(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("EXISTS"));
        assertTrue(sql.contains("customer_id"));
    }

    @Test
    public void testComplexSubQueryWithJoin() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status = 'active'");
        Exists condition = new Exists(subQuery);
        assertNotNull(condition);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testSubQueryWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("total", (Object) 100)));
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), andCondition);
        Exists condition = new Exists(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

}

public class ExistsTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        Exists exists = Filters.exists(subQuery);

        Assertions.assertNotNull(exists);
        Assertions.assertEquals(Operator.EXISTS, exists.operator());
        Assertions.assertEquals(subQuery, exists.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
        Exists exists = Filters.exists(subQuery);

        SubQuery retrieved = exists.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM subordinates WHERE manager_id = emp.id");
        Exists exists = Filters.exists(subQuery);

        String result = exists.toString();
        Assertions.assertEquals("EXISTS (SELECT 1 FROM subordinates WHERE manager_id = emp.id)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orderItems WHERE orderId = orders.id");
        Exists exists = Filters.exists(subQuery);

        String result = exists.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("EXISTS"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), Filters.and(Filters.eq("customer_id", 123), Filters.eq("status", "pending")));
        Exists exists = Filters.exists(subQuery);

        var params = exists.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains(123));
        Assertions.assertTrue(params.contains("pending"));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("id"), Filters.in("category_id", Arrays.asList(1, 2, 3)));
        Exists exists = Filters.exists(subQuery);

        Assertions.assertEquals(3, exists.getParameters().size());

        exists.clearParameters();

        Assertions.assertTrue(exists.getParameters().size() == 3 && exists.getParameters().stream().allMatch(param -> param == null));
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery3 = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = 200");

        Exists exists1 = Filters.exists(subQuery1);
        Exists exists2 = Filters.exists(subQuery2);
        Exists exists3 = Filters.exists(subQuery3);

        Assertions.assertEquals(exists1, exists1);
        Assertions.assertEquals(exists1, exists2);
        Assertions.assertNotEquals(exists1, exists3);
        Assertions.assertNotEquals(exists1, null);
        Assertions.assertNotEquals(exists1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM tags WHERE product_id = p.id");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM tags WHERE product_id = p.id");

        Exists exists1 = Filters.exists(subQuery1);
        Exists exists2 = Filters.exists(subQuery2);

        Assertions.assertEquals(exists1.hashCode(), exists2.hashCode());
    }

    @Test
    public void testNotExists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM blocked_users WHERE id = users.id");
        NotExists notExists = Filters.notExists(subQuery);

        Assertions.assertNotNull(notExists);
        Assertions.assertEquals(Operator.NOT_EXISTS, notExists.operator());
    }

    @Test
    public void testComplexExistsScenarios() {
        // Test customers with orders
        SubQuery hasOrders = Filters.subQuery("orders", Arrays.asList("1"),
                Filters.and(Filters.eq("orders.customer_id", "customers.id"), Filters.ge("order_date", "2023-01-01")));
        Exists customersWithRecentOrders = Filters.exists(hasOrders);

        Assertions.assertNotNull(customersWithRecentOrders);
        var params = customersWithRecentOrders.getParameters();
        Assertions.assertTrue(params.contains("2023-01-01"));

        // Test products with stock
        SubQuery hasStock = Filters.subQuery("inventory", Arrays.asList("1"),
                Filters.and(Filters.eq("inventory.product_id", "products.id"), Filters.gt("quantity", 0)));
        Exists productsInStock = Filters.exists(hasStock);

        Assertions.assertNotNull(productsInStock);
        params = productsInStock.getParameters();
        Assertions.assertTrue(params.contains(0));
    }

    @Test
    public void testPerformanceConsideration() {
        // EXISTS with SELECT 1 (optimal)
        SubQuery optimal = Filters.subQuery("SELECT 1 FROM large_table WHERE condition = true");
        Exists exists1 = Filters.exists(optimal);

        // EXISTS with SELECT * (works but less optimal)
        SubQuery lessOptimal = Filters.subQuery("SELECT * FROM large_table WHERE condition = true");
        Exists exists2 = Filters.exists(lessOptimal);

        // Both should work correctly
        Assertions.assertNotNull(exists1);
        Assertions.assertNotNull(exists2);

        // The difference is only in the SQL string
        Assertions.assertTrue(exists1.toString().contains("SELECT 1"));
        Assertions.assertTrue(exists2.toString().contains("SELECT *"));
    }
}
