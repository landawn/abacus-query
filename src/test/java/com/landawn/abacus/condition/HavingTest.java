package com.landawn.abacus.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class HavingTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Condition innerCondition = CF.gt("COUNT(*)", 5);
        Having having = new Having(innerCondition);
        
        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.getOperator());
        Assertions.assertEquals(innerCondition, having.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition innerCondition = CF.and(
            CF.gt("AVG(salary)", 50000),
            CF.lt("COUNT(*)", 100)
        );
        Having having = new Having(innerCondition);
        
        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.getOperator());
        Assertions.assertEquals(innerCondition, having.getCondition());
    }

    @Test
    public void testGetParameters() {
        Condition innerCondition = CF.gt("SUM(sales)", 10000);
        Having having = new Having(innerCondition);
        
        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(10000, params.get(0));
    }

    @Test
    public void testGetParametersWithMultipleValues() {
        Condition innerCondition = CF.between("AVG(price)", 100, 500);
        Having having = new Having(innerCondition);
        
        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(100, params.get(0));
        Assertions.assertEquals(500, params.get(1));
    }

    @Test
    public void testClearParameters() {
        Condition innerCondition = CF.eq("COUNT(*)", 10);
        Having having = new Having(innerCondition);
        
        having.clearParameters();
        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        // Parameters should be cleared
        Assertions.assertTrue(params.isEmpty() || params.get(0) == null);
    }

    @Test
    public void testCopy() {
        Condition innerCondition = CF.gt("AVG(rating)", 4.5);
        Having original = new Having(innerCondition);
        Having copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.getCondition().toString(), copy.getCondition().toString());
    }

    @Test
    public void testToString() {
        Condition innerCondition = CF.gt("COUNT(*)", 5);
        Having having = new Having(innerCondition);
        
        String result = having.toString();
        Assertions.assertTrue(result.contains("HAVING"));
        Assertions.assertTrue(result.contains("COUNT(*)"));
        Assertions.assertTrue(result.contains(">"));
        Assertions.assertTrue(result.contains("5"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition innerCondition = CF.eq("totalAmount", 1000);
        Having having = new Having(innerCondition);
        
        String result = having.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("HAVING"));
        Assertions.assertTrue(result.contains("TOTAL_AMOUNT"));
    }

    @Test
    public void testHashCode() {
        Condition condition1 = CF.gt("COUNT(*)", 5);
        Condition condition2 = CF.gt("COUNT(*)", 5);
        Condition condition3 = CF.lt("SUM(price)", 100);
        
        Having having1 = new Having(condition1);
        Having having2 = new Having(condition2);
        Having having3 = new Having(condition3);
        
        Assertions.assertEquals(having1.hashCode(), having2.hashCode());
        Assertions.assertNotEquals(having1.hashCode(), having3.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition1 = CF.gt("COUNT(*)", 5);
        Condition condition2 = CF.gt("COUNT(*)", 5);
        Condition condition3 = CF.lt("SUM(price)", 100);
        
        Having having1 = new Having(condition1);
        Having having2 = new Having(condition2);
        Having having3 = new Having(condition3);
        
        Assertions.assertEquals(having1, having1);
        Assertions.assertEquals(having1, having2);
        Assertions.assertNotEquals(having1, having3);
        Assertions.assertNotEquals(having1, null);
        Assertions.assertNotEquals(having1, "string");
    }

    @Test
    public void testConstructorWithNullCondition() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new Having(null);
        });
    }
}