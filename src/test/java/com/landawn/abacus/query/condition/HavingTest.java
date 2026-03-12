package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link Having}.
 * Tests all public methods including construction, toString, equals, hashCode, and inherited methods.
 */
@Tag("2025")
class Having2025Test extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Equal condition = Filters.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        assertNotNull(having);
        assertEquals(Operator.HAVING, having.operator());
    }

    @Test
    public void testToString() {
        Having having = new Having(Filters.gt("COUNT(*)", 10));
        String result = having.toString();

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("COUNT(*)"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));
        String result = having.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testGetParameters() {
        Having having = new Having(Filters.gt("SUM(amount)", 1000));

        assertNotNull(having.getParameters());
        assertEquals(1, having.getParameters().size());
        assertEquals(1000, having.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithMultipleConditions() {
        Condition condition = Filters.and(Filters.gt("COUNT(*)", 5), Filters.lt("AVG(price)", 100));
        Having having = new Having(condition);

        assertEquals(2, having.getParameters().size());
    }

    @Test
    public void testClearParameters() {
        Having having = new Having(Filters.gt("COUNT(*)", 10));
        assertEquals(1, having.getParameters().size());

        having.clearParameters();
        // Parameters should still exist but values should be cleared
        assertNotNull(having.getParameters());
    }

    @Test
    public void testEquals() {
        Having having1 = new Having(Filters.gt("COUNT(*)", 5));
        Having having2 = new Having(Filters.gt("COUNT(*)", 5));
        Having having3 = new Having(Filters.gt("COUNT(*)", 10));

        assertEquals(having1, having2);
        assertNotEquals(having1, having3);
        assertEquals(having1, having1);
    }

    @Test
    public void testEqualsWithNull() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));

        assertNotEquals(having, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));
        Where where = new Where(Filters.gt("COUNT(*)", 5));

        assertNotEquals(having, (Object) where);
    }

    @Test
    public void testHashCode() {
        Having having1 = new Having(Filters.gt("COUNT(*)", 5));
        Having having2 = new Having(Filters.gt("COUNT(*)", 5));

        assertEquals(having1.hashCode(), having2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Having having = new Having(Filters.gt("SUM(total)", 1000));
        int hash1 = having.hashCode();
        int hash2 = having.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testWithAggregateFunction() {
        Having having = new Having(Filters.gt("MAX(salary)", 100000));
        String result = having.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("max") || result.contains("MAX"));
    }

    @Test
    public void testWithMinFunction() {
        Having having = new Having(Filters.lt("MIN(age)", 18));

        assertNotNull(having);
        assertEquals(1, having.getParameters().size());
        assertEquals(18, having.getParameters().get(0));
    }

    @Test
    public void testWithComplexCondition() {
        Condition complex = Filters.and(Filters.gt("COUNT(*)", 10), Filters.between("AVG(salary)", 50000, 100000));
        Having having = new Having(complex);

        assertNotNull(having);
        assertEquals(3, having.getParameters().size());
    }

    @Test
    public void testWithOrCondition() {
        Condition orCondition = Filters.or(Filters.gt("SUM(revenue)", 1000000), Filters.eq("COUNT(*)", 0));
        Having having = new Having(orCondition);

        String result = having.toString();
        assertTrue(result.contains("OR"));
    }

    @Test
    public void testWithExpression() {
        Having having = new Having(Filters.expr("COUNT(DISTINCT customer_id) > 100"));

        assertNotNull(having);
        String result = having.toString();
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testInheritedGetConditionMethod() {
        Equal condition = Filters.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        Condition retrievedCondition = having.getCondition();
        assertEquals(condition, retrievedCondition);
    }

    @Test
    public void testOperatorType() {
        Having having = new Having(Filters.gt("COUNT(*)", 0));

        assertEquals(Operator.HAVING, having.operator());
        assertNotEquals(Operator.WHERE, having.operator());
        assertNotEquals(Operator.GROUP_BY, having.operator());
    }

    @Test
    public void testWithBetweenCondition() {
        Having having = new Having(Filters.between("AVG(age)", 25, 40));

        assertEquals(2, having.getParameters().size());
        assertEquals(25, having.getParameters().get(0));
        assertEquals(40, having.getParameters().get(1));
    }

    @Test
    public void testMultipleAggregatesWithAnd() {
        Condition condition = Filters.and(Filters.gt("COUNT(*)", 5), Filters.gt("SUM(amount)", 10000), Filters.lt("AVG(price)", 500));
        Having having = new Having(condition);

        assertEquals(3, having.getParameters().size());
        String result = having.toString();
        assertTrue(result.contains("COUNT"));
        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("AVG"));
    }

    @Test
    public void testWithNullSafeCondition() {
        Having having = new Having(Filters.isNotNull("COUNT(*)"));

        assertNotNull(having);
        assertEquals(0, having.getParameters().size());
    }

    @Test
    public void testStringRepresentationFormat() {
        Having having = new Having(Filters.ge("COUNT(*)", 1));
        String result = having.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains(">="));
    }

    @Test
    public void testToString_NoArgs() {
        Having having = new Having(Filters.gt("SUM(sales)", 50000));
        String result = having.toString();

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("SUM"));
    }

    @Test
    public void testGetOperator() {
        Having having = new Having(Filters.gt("COUNT(*)", 0));
        assertEquals(Operator.HAVING, having.operator());
    }

    @Test
    public void testWithIsNull() {
        Having having = new Having(Filters.isNull("MAX(deletedAt)"));
        assertEquals(0, having.getParameters().size());
    }
}

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
