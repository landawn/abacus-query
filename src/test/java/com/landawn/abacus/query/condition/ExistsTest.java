package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

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
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM items WHERE active = true");
        Exists original = Filters.exists(subQuery);

        Exists copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
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
    public void testAnd() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM permissions WHERE user_id = u.id");
        Exists exists = Filters.exists(subQuery);
        Equal eq = Filters.eq("active", true);

        And and = exists.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(exists));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM admins WHERE user_id = u.id");
        Exists exists = Filters.exists(subQuery);
        Equal eq = Filters.eq("superuser", true);

        Or or = exists.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM blacklist WHERE user_id = u.id");
        Exists exists = Filters.exists(subQuery);

        Not not = exists.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(exists, not.getCondition());
    }

    @Test
    public void testNotExists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM blocked_users WHERE id = users.id");
        NotExists notExists = Filters.notExists(subQuery);

        Assertions.assertNotNull(notExists);
        Assertions.assertEquals(Operator.NOT_EXISTS, notExists.operator());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM table2");
        Exists exists = Filters.exists(subQuery1);

        Assertions.assertEquals(subQuery1, exists.getCondition());

        exists.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, exists.getCondition());
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
