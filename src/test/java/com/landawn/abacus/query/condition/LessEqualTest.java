package com.landawn.abacus.query.condition;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.LessEqual;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.util.NamingPolicy;

public class LessEqualTest extends TestBase {

    @Test
    public void testConstructorWithNumber() {
        LessEqual condition = new LessEqual("age", 18);
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.LESS_EQUAL, condition.getOperator());
        Assertions.assertEquals(18, (Integer)(Integer) condition.getPropValue());
    }

    @Test
    public void testConstructorWithDouble() {
        LessEqual condition = new LessEqual("price", 99.99);
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("price", condition.getPropName());
        Assertions.assertEquals(99.99, condition.getPropValue());
    }

    @Test
    public void testConstructorWithString() {
        LessEqual condition = new LessEqual("name", "Z");
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals("Z", condition.getPropValue());
    }

    @Test
    public void testConstructorWithDate() {
        Date date = new Date();
        LessEqual condition = new LessEqual("submit_date", date);
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("submit_date", condition.getPropName());
        Assertions.assertEquals(date, condition.getPropValue());
    }

    @Test
    public void testConstructorWithLocalDate() {
        LocalDate date = LocalDate.now();
        LessEqual condition = new LessEqual("order_date", date);
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("order_date", condition.getPropName());
        Assertions.assertEquals(date, condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        LessEqual condition = new LessEqual("quantity", 100);
        List<Object> params = condition.getParameters();
        
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(100, params.get(0));
    }

    @Test
    public void testClearParameters() {
        LessEqual condition = new LessEqual("stock", 50);
        condition.clearParameters();
        
        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testCopy() {
        LessEqual original = new LessEqual("score", 85.5);
        LessEqual copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        LessEqual condition = new LessEqual("amount", 1000);
        String result = condition.toString();
        
        Assertions.assertTrue(result.contains("amount"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("1000"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        LessEqual condition = new LessEqual("totalAmount", 5000);
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        
        Assertions.assertTrue(result.contains("TOTAL_AMOUNT"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("5000"));
    }

    @Test
    public void testHashCode() {
        LessEqual condition1 = new LessEqual("age", 30);
        LessEqual condition2 = new LessEqual("age", 30);
        LessEqual condition3 = new LessEqual("age", 40);
        LessEqual condition4 = new LessEqual("weight", 30);
        
        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        LessEqual condition1 = new LessEqual("age", 30);
        LessEqual condition2 = new LessEqual("age", 30);
        LessEqual condition3 = new LessEqual("age", 40);
        LessEqual condition4 = new LessEqual("weight", 30);
        
        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testWithNullValue() {
        LessEqual condition = new LessEqual("field", null);
        
        Assertions.assertNotNull(condition);
        Assertions.assertNull(condition.getPropValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("field"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testUpperBoundScenarios() {
        // Test common upper bound scenarios
        LessEqual maxStock = new LessEqual("quantity", 100);
        LessEqual maxPrice = new LessEqual("price", 999.99);
        LessEqual deadline = new LessEqual("submit_date", "2023-12-31");
        
        Assertions.assertEquals(100, (Integer) maxStock.getPropValue());
        Assertions.assertEquals(999.99, maxPrice.getPropValue());
        Assertions.assertEquals("2023-12-31", deadline.getPropValue());
    }

    @Test
    public void testRangeQueryScenario() {
        // Common scenario: using LessEqual as part of a range query
        LessEqual upperBound = new LessEqual("salary", 100000);
        
        Assertions.assertEquals("salary", upperBound.getPropName());
        Assertions.assertEquals(100000, (Integer) upperBound.getPropValue());
        Assertions.assertEquals(Operator.LESS_EQUAL, upperBound.getOperator());
        
        String result = upperBound.toString();
        Assertions.assertTrue(result.contains("salary <= 100000"));
    }
}