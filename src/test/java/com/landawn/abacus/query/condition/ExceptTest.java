package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Cell;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.ConditionFactory;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Except;
import com.landawn.abacus.query.condition.Minus;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.NamingPolicy;

public class ExceptTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT customer_id FROM customers WHERE status = 'inactive'");
        Except except = ConditionFactory.except(subQuery);
        
        Assertions.assertNotNull(except);
        Assertions.assertEquals(Operator.EXCEPT, except.getOperator());
        Assertions.assertEquals(subQuery, except.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT product_id FROM discontinued_products");
        Except except = ConditionFactory.except(subQuery);
        
        SubQuery retrieved = except.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM blacklist");
        Except except = ConditionFactory.except(subQuery);
        
        String result = except.toString();
        Assertions.assertTrue(result.contains("EXCEPT"));
        Assertions.assertTrue(result.contains("SELECT id FROM blacklist"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT userId FROM bannedUsers");
        Except except = ConditionFactory.except(subQuery);
        
        String result = except.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("EXCEPT"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("orders", 
            Arrays.asList("customer_id"), 
            ConditionFactory.and(
                ConditionFactory.lt("order_date", "2023-01-01"),
                ConditionFactory.eq("status", "cancelled")
            )
        );
        Except except = ConditionFactory.except(subQuery);
        
        var params = except.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("2023-01-01"));
        Assertions.assertTrue(params.contains("cancelled"));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("products", 
            Arrays.asList("id"), 
            ConditionFactory.in("category", Arrays.asList("obsolete", "discontinued"))
        );
        Except except = ConditionFactory.except(subQuery);
        
        Assertions.assertEquals(2, except.getParameters().size());
        
        except.clearParameters();// filled with nulls

        Assertions.assertEquals(2, except.getParameters().size());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM temp_users");
        Except original = ConditionFactory.except(subQuery);
        
        Except copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT id FROM excluded");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT id FROM excluded");
        SubQuery subQuery3 = ConditionFactory.subQuery("SELECT id FROM included");
        
        Except except1 = ConditionFactory.except(subQuery1);
        Except except2 = ConditionFactory.except(subQuery2);
        Except except3 = ConditionFactory.except(subQuery3);
        
        Assertions.assertEquals(except1, except1);
        Assertions.assertEquals(except1, except2);
        Assertions.assertNotEquals(except1, except3);
        Assertions.assertNotEquals(except1, null);
        Assertions.assertNotEquals(except1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT id FROM inactive_items");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT id FROM inactive_items");
        
        Except except1 = ConditionFactory.except(subQuery1);
        Except except2 = ConditionFactory.except(subQuery2);
        
        Assertions.assertEquals(except1.hashCode(), except2.hashCode());
    }

    @Test
    public void testClauseRestrictions() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM test");
        Except except = ConditionFactory.except(subQuery);
        
        // These should throw UnsupportedOperationException as per Clause class
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.and(ConditionFactory.eq("test", 1));
        });
        
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.or(ConditionFactory.eq("test", 1));
        });
        
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            except.not();
        });
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT id FROM table2");
        Except except = ConditionFactory.except(subQuery1);
        
        Assertions.assertEquals(subQuery1, except.getCondition());
        
        except.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, except.getCondition());
    }

    @Test
    public void testComplexExceptScenarios() {
        // Find active customers who haven't placed orders
        SubQuery customersWithOrders = ConditionFactory.subQuery(
            "orders",
            Arrays.asList("DISTINCT customer_id"),
            ConditionFactory.ge("order_date", "2023-01-01")
        );
        Except activeWithoutOrders = ConditionFactory.except(customersWithOrders);
        
        Assertions.assertNotNull(activeWithoutOrders);
        var params = activeWithoutOrders.getParameters();
        Assertions.assertTrue(params.contains("2023-01-01"));
        
        // Find products not sold recently
        SubQuery recentlySold = ConditionFactory.subQuery(
            "order_items oi JOIN orders o ON oi.order_id = o.id",
            Arrays.asList("oi.product_id"),
            ConditionFactory.gt("o.order_date", "2023-06-01")
        );
        Except notRecentlySold = ConditionFactory.except(recentlySold);
        
        Assertions.assertNotNull(notRecentlySold);
        
        // Find employees not in management
        SubQuery managers = ConditionFactory.subQuery(
            "employees",
            Arrays.asList("employee_id"),
            ConditionFactory.or(
                ConditionFactory.eq("is_manager", true),
                ConditionFactory.like("title", "%Manager%"),
                ConditionFactory.like("title", "%Director%")
            )
        );
        Except nonManagement = ConditionFactory.except(managers);
        
        Assertions.assertNotNull(nonManagement);
        params = nonManagement.getParameters();
        Assertions.assertEquals(3, params.size());
    }

    @Test
    public void testExceptWithCriteria() {
        // Test EXCEPT in a complete criteria
        SubQuery excludedUsers = ConditionFactory.subQuery("SELECT user_id FROM banned_users");
        
        Criteria criteria = ConditionFactory.criteria()
            .where(ConditionFactory.eq("status", "active"))
            .except(excludedUsers);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).getOperator());
    }

    @Test
    public void testExceptVsMinus() {
        // EXCEPT and MINUS are equivalent in some databases
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM temp");
        
        Except except = ConditionFactory.except(subQuery);
        Minus minus = ConditionFactory.minus(subQuery);
        
        // Both should work similarly
        Assertions.assertEquals(Operator.EXCEPT, except.getOperator());
        Assertions.assertEquals(Operator.MINUS, minus.getOperator());
        
        // Same subquery
        Assertions.assertEquals((Condition) except.getCondition(), minus.getCondition());
    }

    @Test
    public void testExceptWithComplexSubQuery() {
        // Complex subquery with multiple conditions and joins
        SubQuery complexSubQuery = ConditionFactory.subQuery(
            "users u JOIN departments d ON u.dept_id = d.id",
            Arrays.asList("u.id"),
            ConditionFactory.and(
                ConditionFactory.eq("d.location", "Remote"),
                ConditionFactory.or(
                    ConditionFactory.eq("u.type", "contractor"),
                    ConditionFactory.lt("u.hire_date", "2020-01-01")
                ),
                ConditionFactory.ne("u.status", "active")
            )
        );
        
        Except except = ConditionFactory.except(complexSubQuery);
        
        var params = except.getParameters();
        Assertions.assertEquals(4, params.size());
        Assertions.assertTrue(params.contains("Remote"));
        Assertions.assertTrue(params.contains("contractor"));
        Assertions.assertTrue(params.contains("2020-01-01"));
        Assertions.assertTrue(params.contains("active"));
    }
}