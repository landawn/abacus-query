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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Except2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(subQuery);
        SubQuery retrieved = except.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123");
        Except except = new Except(subQuery);
        List<Object> params = except.getParameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("employees", List.of("employee_id"), new Equal("is_manager", "true"));
        Except except = new Except(subQuery);
        assertFalse(except.getParameters().isEmpty());
        except.clearParameters();
        List<Object> params = except.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1.hashCode(), except2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(except, except);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1, except2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertNotEquals(except1, except2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(null, except);
    }

    @Test
    public void testSetDifference() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(customersWithOrders);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    public void testFindMissingRecords() {
        SubQuery soldProducts = Filters.subQuery("order_items", List.of("product_id"), new GreaterThan("order_date", "2024-01-01"));
        Except except = new Except(soldProducts);
        assertEquals(1, (int) except.getParameters().size());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toString();
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(except, "not an Except");
        assertNotEquals(except, new Union(subQuery));
    }
}

public class ExceptTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'inactive'");
        Except except = Filters.except(subQuery);

        Assertions.assertNotNull(except);
        Assertions.assertEquals(Operator.EXCEPT, except.operator());
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

        Criteria criteria = Criteria.builder().where(Filters.eq("status", "active")).except(excludedUsers).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).operator());
    }

    @Test
    public void testExceptVsMinus() {
        // EXCEPT and MINUS are equivalent in some databases
        SubQuery subQuery = Filters.subQuery("SELECT id FROM temp");

        Except except = Filters.except(subQuery);
        Minus minus = Filters.minus(subQuery);

        // Both should work similarly
        Assertions.assertEquals(Operator.EXCEPT, except.operator());
        Assertions.assertEquals(Operator.MINUS, minus.operator());

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
