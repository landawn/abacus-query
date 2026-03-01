package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class ExceptTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'inactive'");
        Except except = Filters.except(subQuery);

        Assertions.assertNotNull(except);
        Assertions.assertEquals(Operator.EXCEPT, except.getOperator());
        Assertions.assertEquals(subQuery, except.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM discontinued_products");
        Except except = Filters.except(subQuery);

        SubQuery retrieved = except.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklist");
        Except except = Filters.except(subQuery);

        String result = except.toString();
        Assertions.assertTrue(result.contains("EXCEPT"));
        Assertions.assertTrue(result.contains("SELECT id FROM blacklist"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT userId FROM bannedUsers");
        Except except = Filters.except(subQuery);

        String result = except.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id"),
                Filters.and(Filters.lt("order_date", "2023-01-01"), Filters.eq("status", "cancelled")));
        Except except = Filters.except(subQuery);

        var params = except.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("2023-01-01"));
        Assertions.assertTrue(params.contains("cancelled"));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("id"), Filters.in("category", Arrays.asList("obsolete", "discontinued")));
        Except except = Filters.except(subQuery);

        Assertions.assertEquals(2, except.getParameters().size());

        except.clearParameters();// filled with nulls

        Assertions.assertEquals(2, except.getParameters().size());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM temp_users");
        Except original = Filters.except(subQuery);

        Except copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM excluded");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM excluded");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM included");

        Except except1 = Filters.except(subQuery1);
        Except except2 = Filters.except(subQuery2);
        Except except3 = Filters.except(subQuery3);

        Assertions.assertEquals(except1, except1);
        Assertions.assertEquals(except1, except2);
        Assertions.assertNotEquals(except1, except3);
        Assertions.assertNotEquals(except1, null);
        Assertions.assertNotEquals(except1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM inactive_items");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM inactive_items");

        Except except1 = Filters.except(subQuery1);
        Except except2 = Filters.except(subQuery2);

        Assertions.assertEquals(except1.hashCode(), except2.hashCode());
    }

    @Test
    public void testClauseRestrictions() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM test");
        Except except = Filters.except(subQuery);

        // These should throw UnsupportedOperationException as per Clause class
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.and(Filters.eq("test", 1));
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.or(Filters.eq("test", 1));
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.not();
        });
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Except except = Filters.except(subQuery1);

        Assertions.assertEquals(subQuery1, except.getCondition());

        except.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, except.getCondition());
    }

    @Test
    public void testComplexExceptScenarios() {
        // Find active customers who haven't placed orders
        SubQuery customersWithOrders = Filters.subQuery("orders", Arrays.asList("DISTINCT customer_id"), Filters.ge("order_date", "2023-01-01"));
        Except activeWithoutOrders = Filters.except(customersWithOrders);

        Assertions.assertNotNull(activeWithoutOrders);
        var params = activeWithoutOrders.getParameters();
        Assertions.assertTrue(params.contains("2023-01-01"));

        // Find products not sold recently
        SubQuery recentlySold = Filters.subQuery("order_items oi JOIN orders o ON oi.order_id = o.id", Arrays.asList("oi.product_id"),
                Filters.gt("o.order_date", "2023-06-01"));
        Except notRecentlySold = Filters.except(recentlySold);

        Assertions.assertNotNull(notRecentlySold);

        // Find employees not in management
        SubQuery managers = Filters.subQuery("employees", Arrays.asList("employee_id"),
                Filters.or(Filters.eq("is_manager", true), Filters.like("title", "%Manager%"), Filters.like("title", "%Director%")));
        Except nonManagement = Filters.except(managers);

        Assertions.assertNotNull(nonManagement);
        params = nonManagement.getParameters();
        Assertions.assertEquals(3, params.size());
    }

    @Test
    public void testExceptWithCriteria() {
        // Test EXCEPT in a complete criteria
        SubQuery excludedUsers = Filters.subQuery("SELECT user_id FROM banned_users");

        Criteria criteria = Filters.criteria().where(Filters.eq("status", "active")).except(excludedUsers);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).getOperator());
    }

    @Test
    public void testExceptVsMinus() {
        // EXCEPT and MINUS are equivalent in some databases
        SubQuery subQuery = Filters.subQuery("SELECT id FROM temp");

        Except except = Filters.except(subQuery);
        Minus minus = Filters.minus(subQuery);

        // Both should work similarly
        Assertions.assertEquals(Operator.EXCEPT, except.getOperator());
        Assertions.assertEquals(Operator.MINUS, minus.getOperator());

        // Same subquery
        Assertions.assertEquals((Condition) except.getCondition(), minus.getCondition());
    }

    @Test
    public void testExceptWithComplexSubQuery() {
        // Complex subquery with multiple conditions and joins
        SubQuery complexSubQuery = Filters.subQuery("users u JOIN departments d ON u.dept_id = d.id", Arrays.asList("u.id"),
                Filters.and(Filters.eq("d.location", "Remote"), Filters.or(Filters.eq("u.type", "contractor"), Filters.lt("u.hire_date", "2020-01-01")),
                        Filters.ne("u.status", "active")));

        Except except = Filters.except(complexSubQuery);

        var params = except.getParameters();
        Assertions.assertEquals(4, params.size());
        Assertions.assertTrue(params.contains("Remote"));
        Assertions.assertTrue(params.contains("contractor"));
        Assertions.assertTrue(params.contains("2020-01-01"));
        Assertions.assertTrue(params.contains("active"));
    }
}