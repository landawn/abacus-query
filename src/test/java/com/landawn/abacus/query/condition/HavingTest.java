package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class HavingTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Condition innerCondition = Filters.gt("COUNT(*)", 5);
        Having having = new Having(innerCondition);

        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.operator());
        Assertions.assertEquals(innerCondition, having.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition innerCondition = Filters.and(Filters.gt("AVG(salary)", 50000), Filters.lt("COUNT(*)", 100));
        Having having = new Having(innerCondition);

        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.operator());
        Assertions.assertEquals(innerCondition, having.getCondition());
    }

    @Test
    public void testGetParameters() {
        Condition innerCondition = Filters.gt("SUM(sales)", 10000);
        Having having = new Having(innerCondition);

        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(10000, params.get(0));
    }

    @Test
    public void testGetParametersWithMultipleValues() {
        Condition innerCondition = Filters.between("AVG(price)", 100, 500);
        Having having = new Having(innerCondition);

        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(100, params.get(0));
        Assertions.assertEquals(500, params.get(1));
    }

    @Test
    public void testClearParameters() {
        Condition innerCondition = Filters.eq("COUNT(*)", 10);
        Having having = new Having(innerCondition);

        having.clearParameters();
        List<Object> params = having.getParameters();
        Assertions.assertNotNull(params);
        // Parameters should be cleared
        Assertions.assertTrue(params.isEmpty() || params.get(0) == null);
    }

    @Test
    public void testCopy() {
        Condition innerCondition = Filters.gt("AVG(rating)", 4.5);
        Having original = new Having(innerCondition);
        Having copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.getCondition().toString(), copy.getCondition().toString());
    }

    @Test
    public void testToString() {
        Condition innerCondition = Filters.gt("COUNT(*)", 5);
        Having having = new Having(innerCondition);

        String result = having.toString();
        Assertions.assertTrue(result.contains("HAVING"));
        Assertions.assertTrue(result.contains("COUNT(*)"));
        Assertions.assertTrue(result.contains(">"));
        Assertions.assertTrue(result.contains("5"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition innerCondition = Filters.eq("totalAmount", 1000);
        Having having = new Having(innerCondition);

        String result = having.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("HAVING"));
        Assertions.assertTrue(result.contains("TOTAL_AMOUNT"));
    }

    @Test
    public void testHashCode() {
        Condition condition1 = Filters.gt("COUNT(*)", 5);
        Condition condition2 = Filters.gt("COUNT(*)", 5);
        Condition condition3 = Filters.lt("SUM(price)", 100);

        Having having1 = new Having(condition1);
        Having having2 = new Having(condition2);
        Having having3 = new Having(condition3);

        Assertions.assertEquals(having1.hashCode(), having2.hashCode());
        Assertions.assertNotEquals(having1.hashCode(), having3.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition1 = Filters.gt("COUNT(*)", 5);
        Condition condition2 = Filters.gt("COUNT(*)", 5);
        Condition condition3 = Filters.lt("SUM(price)", 100);

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
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Having(null);
        });
    }
}