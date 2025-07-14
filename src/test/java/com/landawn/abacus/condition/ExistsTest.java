package com.landawn.abacus.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class ExistsTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        Exists exists = ConditionFactory.exists(subQuery);

        Assertions.assertNotNull(exists);
        Assertions.assertEquals(Operator.EXISTS, exists.getOperator());
        Assertions.assertEquals(subQuery, exists.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
        Exists exists = ConditionFactory.exists(subQuery);

        SubQuery retrieved = exists.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM subordinates WHERE manager_id = emp.id");
        Exists exists = ConditionFactory.exists(subQuery);

        String result = exists.toString();
        Assertions.assertTrue(result.contains("EXISTS"));
        Assertions.assertTrue(result.contains("SELECT 1 FROM subordinates"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM orderItems WHERE orderId = orders.id");
        Exists exists = ConditionFactory.exists(subQuery);

        String result = exists.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("EXISTS"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("orders", Arrays.asList("id"),
                ConditionFactory.and(ConditionFactory.eq("customer_id", 123), ConditionFactory.eq("status", "pending")));
        Exists exists = ConditionFactory.exists(subQuery);

        var params = exists.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains(123));
        Assertions.assertTrue(params.contains("pending"));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("products", Arrays.asList("id"), ConditionFactory.in("category_id", Arrays.asList(1, 2, 3)));
        Exists exists = ConditionFactory.exists(subQuery);

        Assertions.assertEquals(3, exists.getParameters().size());

        exists.clearParameters();

        Assertions.assertTrue(exists.getParameters().size() == 3 && exists.getParameters().stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM items WHERE active = true");
        Exists original = ConditionFactory.exists(subQuery);

        Exists copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT 1 FROM orders WHERE customer_id = 100");
        SubQuery subQuery3 = ConditionFactory.subQuery("SELECT 1 FROM orders WHERE customer_id = 200");

        Exists exists1 = ConditionFactory.exists(subQuery1);
        Exists exists2 = ConditionFactory.exists(subQuery2);
        Exists exists3 = ConditionFactory.exists(subQuery3);

        Assertions.assertEquals(exists1, exists1);
        Assertions.assertEquals(exists1, exists2);
        Assertions.assertNotEquals(exists1, exists3);
        Assertions.assertNotEquals(exists1, null);
        Assertions.assertNotEquals(exists1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT 1 FROM tags WHERE product_id = p.id");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT 1 FROM tags WHERE product_id = p.id");

        Exists exists1 = ConditionFactory.exists(subQuery1);
        Exists exists2 = ConditionFactory.exists(subQuery2);

        Assertions.assertEquals(exists1.hashCode(), exists2.hashCode());
    }

    @Test
    public void testAnd() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM permissions WHERE user_id = u.id");
        Exists exists = ConditionFactory.exists(subQuery);
        Equal eq = ConditionFactory.eq("active", true);

        And and = exists.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(exists));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM admins WHERE user_id = u.id");
        Exists exists = ConditionFactory.exists(subQuery);
        Equal eq = ConditionFactory.eq("superuser", true);

        Or or = exists.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM blacklist WHERE user_id = u.id");
        Exists exists = ConditionFactory.exists(subQuery);

        Not not = exists.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(exists, not.getCondition());
    }

    @Test
    public void testNotExists() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM blocked_users WHERE id = users.id");
        NotExists notExists = ConditionFactory.notExists(subQuery);

        Assertions.assertNotNull(notExists);
        Assertions.assertEquals(Operator.NOT_EXISTS, notExists.getOperator());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT 1 FROM table1");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT 1 FROM table2");
        Exists exists = ConditionFactory.exists(subQuery1);

        Assertions.assertEquals(subQuery1, exists.getCondition());

        exists.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, exists.getCondition());
    }

    @Test
    public void testComplexExistsScenarios() {
        // Test customers with orders
        SubQuery hasOrders = ConditionFactory.subQuery("orders", Arrays.asList("1"),
                ConditionFactory.and(ConditionFactory.eq("orders.customer_id", "customers.id"), ConditionFactory.ge("order_date", "2023-01-01")));
        Exists customersWithRecentOrders = ConditionFactory.exists(hasOrders);

        Assertions.assertNotNull(customersWithRecentOrders);
        var params = customersWithRecentOrders.getParameters();
        Assertions.assertTrue(params.contains("2023-01-01"));

        // Test products with stock
        SubQuery hasStock = ConditionFactory.subQuery("inventory", Arrays.asList("1"),
                ConditionFactory.and(ConditionFactory.eq("inventory.product_id", "products.id"), ConditionFactory.gt("quantity", 0)));
        Exists productsInStock = ConditionFactory.exists(hasStock);

        Assertions.assertNotNull(productsInStock);
        params = productsInStock.getParameters();
        Assertions.assertTrue(params.contains(0));
    }

    @Test
    public void testPerformanceConsideration() {
        // EXISTS with SELECT 1 (optimal)
        SubQuery optimal = ConditionFactory.subQuery("SELECT 1 FROM large_table WHERE condition = true");
        Exists exists1 = ConditionFactory.exists(optimal);

        // EXISTS with SELECT * (works but less optimal)
        SubQuery lessOptimal = ConditionFactory.subQuery("SELECT * FROM large_table WHERE condition = true");
        Exists exists2 = ConditionFactory.exists(lessOptimal);

        // Both should work correctly
        Assertions.assertNotNull(exists1);
        Assertions.assertNotNull(exists2);

        // The difference is only in the SQL string
        Assertions.assertTrue(exists1.toString().contains("SELECT 1"));
        Assertions.assertTrue(exists2.toString().contains("SELECT *"));
    }
}