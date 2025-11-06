package com.landawn.abacus.query.condition;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.util.NamingPolicy;

public class LessThanTest extends TestBase {

    @Test
    public void testConstructorWithNumber() {
        LessThan condition = new LessThan("age", 18);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.LESS_THAN, condition.getOperator());
        Assertions.assertEquals(18, (Integer) (Integer) condition.getPropValue());
    }

    @Test
    public void testConstructorWithDouble() {
        LessThan condition = new LessThan("price", 99.99);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("price", condition.getPropName());
        Assertions.assertEquals(99.99, condition.getPropValue());
    }

    @Test
    public void testConstructorWithString() {
        LessThan condition = new LessThan("name", "Z");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals("Z", condition.getPropValue());
    }

    @Test
    public void testConstructorWithDate() {
        Date date = new Date();
        LessThan condition = new LessThan("created_date", date);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("created_date", condition.getPropName());
        Assertions.assertEquals(date, condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        LessThan condition = new LessThan("salary", 50000);
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(50000, params.get(0));
    }

    @Test
    public void testClearParameters() {
        LessThan condition = new LessThan("quantity", 100);
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testCopy() {
        LessThan original = new LessThan("score", 85.5);
        LessThan copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        LessThan condition = new LessThan("amount", 1000);
        String result = condition.toString();

        Assertions.assertTrue(result.contains("amount"));
        Assertions.assertTrue(result.contains("<"));
        Assertions.assertTrue(result.contains("1000"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        LessThan condition = new LessThan("totalAmount", 5000);
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("TOTAL_AMOUNT"));
        Assertions.assertTrue(result.contains("<"));
        Assertions.assertTrue(result.contains("5000"));
    }

    @Test
    public void testHashCode() {
        LessThan condition1 = new LessThan("age", 30);
        LessThan condition2 = new LessThan("age", 30);
        LessThan condition3 = new LessThan("age", 40);
        LessThan condition4 = new LessThan("weight", 30);

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        LessThan condition1 = new LessThan("age", 30);
        LessThan condition2 = new LessThan("age", 30);
        LessThan condition3 = new LessThan("age", 40);
        LessThan condition4 = new LessThan("weight", 30);

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testWithNullValue() {
        LessThan condition = new LessThan("field", null);

        Assertions.assertNotNull(condition);
        Assertions.assertNull(condition.getPropValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("field"));
        Assertions.assertTrue(result.contains("<"));
        Assertions.assertTrue(result.contains("null"));
    }
}