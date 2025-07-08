package com.landawn.abacus.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;

public class MinusTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT product_id FROM sales");
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertNotNull(minus);
        Assertions.assertEquals(Operator.MINUS, minus.getOperator());
        Assertions.assertEquals(subQuery, minus.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = CF.subQuery("SELECT customer_id FROM inactive_customers");
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertEquals(subQuery, minus.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM test");
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertEquals(Operator.MINUS, minus.getOperator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = CF.subQuery("orders", 
            Arrays.asList("customer_id"), 
            CF.and(CF.eq("status", "cancelled"), CF.gt("date", "2023-01-01")));
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertEquals(subQuery, minus.getCondition());
        Assertions.assertEquals(2, minus.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = CF.subQuery("products", 
            Arrays.asList("id"), 
            CF.eq("discontinued", true));
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertEquals(subQuery.getParameters(), minus.getParameters());
        Assertions.assertEquals(1, minus.getParameters().size());
        Assertions.assertEquals(true, minus.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM archived_records");
        Minus minus = CF.minus(subQuery);
        
        Assertions.assertTrue(minus.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = CF.subQuery("users", 
            Arrays.asList("id"), 
            CF.in("status", Arrays.asList("deleted", "banned")));
        Minus minus = CF.minus(subQuery);
        
        minus.clearParameters();
        
        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM inactive_users");
        Minus minus = CF.minus(subQuery);
        
        String result = minus.toString();
        Assertions.assertTrue(result.contains("MINUS"));
        Assertions.assertTrue(result.contains("SELECT id FROM inactive_users"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = CF.subQuery("orders", 
            Arrays.asList("product_id"), 
            CF.eq("returned", true));
        Minus original = CF.minus(subQuery);
        
        Minus copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM other");
        
        Minus minus1 = CF.minus(subQuery1);
        Minus minus2 = CF.minus(subQuery2);
        Minus minus3 = CF.minus(subQuery3);
        
        Assertions.assertEquals(minus1.hashCode(), minus2.hashCode());
        Assertions.assertNotEquals(minus1.hashCode(), minus3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM other");
        
        Minus minus1 = CF.minus(subQuery1);
        Minus minus2 = CF.minus(subQuery2);
        Minus minus3 = CF.minus(subQuery3);
        
        Assertions.assertTrue(minus1.equals(minus1));
        Assertions.assertTrue(minus1.equals(minus2));
        Assertions.assertFalse(minus1.equals(minus3));
        Assertions.assertFalse(minus1.equals(null));
        Assertions.assertFalse(minus1.equals("not a Minus"));
    }

    @Test
    public void testPracticalExample() {
        // Find products that are in inventory but have never been sold
        SubQuery soldProducts = CF.subQuery("SELECT DISTINCT product_id FROM sales");
        Minus minus = CF.minus(soldProducts);
        
        // This would be used with: SELECT product_id FROM inventory MINUS ...
        Assertions.assertEquals(Operator.MINUS, minus.getOperator());
        Assertions.assertEquals(soldProducts, minus.getCondition());
    }

    @Test
    public void testWithParameterizedSubQuery() {
        // Find customers who haven't ordered in the last year
        SubQuery recentCustomers = CF.subQuery("orders", 
            Arrays.asList("customer_id"), 
            CF.gt("order_date", "2023-01-01"));
        Minus minus = CF.minus(recentCustomers);
        
        Assertions.assertEquals(1, minus.getParameters().size());
        Assertions.assertEquals("2023-01-01", minus.getParameters().get(0));
    }
}