package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class ExceptTest extends TestBase {

    @Test
    void testComplexExceptScenarios() {
        // Find active customers who haven't placed orders
        SubQuery customersWithOrders = Filters.subQuery("orders", Arrays.asList("DISTINCT customer_id"), Filters.ge("order_date", "2023-01-01"));
        Except activeWithoutOrders = Filters.except(customersWithOrders);

        assertNotNull(activeWithoutOrders);
        var params = activeWithoutOrders.parameters();
        assertTrue(params.contains("2023-01-01"));

        // Find products not sold recently
        SubQuery recentlySold = Filters.subQuery("order_items oi JOIN orders o ON oi.order_id = o.id", Arrays.asList("oi.product_id"),
                Filters.gt("o.order_date", "2023-06-01"));
        Except notRecentlySold = Filters.except(recentlySold);

        assertNotNull(notRecentlySold);

        // Find employees not in management
        SubQuery managers = Filters.subQuery("employees", Arrays.asList("employee_id"),
                Filters.or(Filters.eq("is_manager", true), Filters.like("title", "%Manager%"), Filters.like("title", "%Director%")));
        Except nonManagement = Filters.except(managers);

        assertNotNull(nonManagement);
        params = nonManagement.parameters();
        assertEquals(3, params.size());
    }

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM excluded");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM excluded");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM included");

        Except except1 = Filters.except(subQuery1);
        Except except2 = Filters.except(subQuery2);
        Except except3 = Filters.except(subQuery3);

        assertEquals(except1, except1);
        assertEquals(except1, except2);
        assertNotEquals(except1, except3);
        assertNotEquals(except1, null);
        assertNotEquals(except1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(except, "not an Except");
        assertNotEquals(except, new Union(subQuery));
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertNotEquals(except1, except2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1, except2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertNotEquals(null, except);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(except, except);
    }

    @Test
    void testExceptVsMinus() {
        // EXCEPT and MINUS are equivalent in some databases
        SubQuery subQuery = Filters.subQuery("SELECT id FROM temp");

        Except except = Filters.except(subQuery);
        Minus minus = Filters.minus(subQuery);

        // Both should work similarly
        assertEquals(Operator.EXCEPT, except.operator());
        assertEquals(Operator.MINUS, minus.operator());

        // Same subquery
        assertEquals(except.condition(), minus.condition());
    }

    @Test
    void testExceptWithComplexSubQuery() {
        // Complex subquery with multiple conditions and joins
        SubQuery complexSubQuery = Filters.subQuery("users u JOIN departments d ON u.dept_id = d.id", Arrays.asList("u.id"),
                Filters.and(Filters.eq("d.location", "Remote"), Filters.or(Filters.eq("u.type", "contractor"), Filters.lt("u.hire_date", "2020-01-01")),
                        Filters.ne("u.status", "active")));

        Except except = Filters.except(complexSubQuery);

        var params = except.parameters();
        assertEquals(4, params.size());
        assertTrue(params.contains("Remote"));
        assertTrue(params.contains("contractor"));
        assertTrue(params.contains("2020-01-01"));
        assertTrue(params.contains("active"));
    }

    @Test
    void testExceptWithCriteria() {
        // Test EXCEPT in a complete criteria
        SubQuery excludedUsers = Filters.subQuery("SELECT user_id FROM banned_users");

        Criteria criteria = Criteria.builder().where(Filters.eq("status", "active")).except(excludedUsers).build();

        List<Clause> aggregations = criteria.setOperations();
        assertEquals(1, aggregations.size());
        assertEquals(Operator.EXCEPT, aggregations.get(0).operator());
    }

    @Test
    void testFindMissingRecords() {
        SubQuery soldProducts = Filters.subQuery("order_items", List.of("product_id"), new GreaterThan("order_date", "2024-01-01"));
        Except except = new Except(soldProducts);
        assertEquals(1, except.parameters().size());
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(subQuery);
        SubQuery retrieved = (SubQuery) except.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Except except1 = new Except(subQuery1);
        Except except2 = new Except(subQuery2);
        assertEquals(except1.hashCode(), except2.hashCode());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123");
        Except except = new Except(subQuery);
        List<Object> params = except.parameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, params.size());
    }

    @Test
    void testSetDifference() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except except = new Except(customersWithOrders);
        assertNotNull(except);
        assertEquals(Operator.EXCEPT, except.operator());
    }

    @Test
    void testSubQueryAccessor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Except except = new Except(subQuery);
        assertSame(subQuery, except.subQuery());
    }

    @Test
    void testSubQueryAccessor_DefaultConstructor_ReturnsNull() {
        // Uninitialized (Kryo) instance: subQuery() must return null without a ClassCastException.
        Except except = new Except();
        assertNull(except.subQuery());
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Except except = new Except(subQuery);
        String result = except.toString();
        assertTrue(result.contains("EXCEPT"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT userId FROM bannedUsers");
        Except except = Filters.except(subQuery);

        String result = except.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("EXCEPT"));
    }
}
