package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link Having}.
 * Tests all public methods including construction, toString, equals, hashCode, and inherited methods.
 */
public class HavingTest extends TestBase {

    @Test
    void testConstructorWithComplexCondition() {
        Condition innerCondition = Filters.and(Filters.gt("AVG(salary)", 50000), Filters.lt("COUNT(*)", 100));
        Having having = new Having(innerCondition);

        assertNotNull(having);
        assertEquals(Operator.HAVING, having.operator());
        assertEquals(innerCondition, having.condition());
    }

    @Test
    void testConstructorWithCondition() {
        Equal condition = Filters.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        assertNotNull(having);
        assertEquals(Operator.HAVING, having.operator());
    }

    @Test
    void testConstructorWithNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Having(null);
        });
    }

    @Test
    void testEmptyJunctionRendersBooleanIdentity() {
        // An initialized empty junction is a complete predicate, so HAVING accepts it and renders its identity.
        assertEquals("HAVING 1 = 0", new Having(Filters.or()).toString());
        assertEquals("HAVING 1 = 1", Filters.having(Filters.and()).toString());
        assertTrue(new Having(Filters.and()).parameters().isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new Having(Filters.expr("  ")));
        assertThrows(IllegalArgumentException.class, () -> new Having(new Or()));
    }

    @Test
    void testEquals() {
        Having having1 = new Having(Filters.gt("COUNT(*)", 5));
        Having having2 = new Having(Filters.gt("COUNT(*)", 5));
        Having having3 = new Having(Filters.gt("COUNT(*)", 10));

        assertEquals(having1, having2);
        assertNotEquals(having1, having3);
        assertEquals(having1, having1);
    }

    @Test
    void testEqualsWithDifferentClass() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));
        Where where = new Where(Filters.gt("COUNT(*)", 5));

        assertNotEquals(having, where);
    }

    @Test
    void testEqualsWithNull() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));

        assertNotEquals(having, null);
    }

    @Test
    void testGetOperator() {
        Having having = new Having(Filters.gt("COUNT(*)", 0));
        assertEquals(Operator.HAVING, having.operator());
    }

    @Test
    void testHashCode() {
        Having having1 = new Having(Filters.gt("COUNT(*)", 5));
        Having having2 = new Having(Filters.gt("COUNT(*)", 5));

        assertEquals(having1.hashCode(), having2.hashCode());
    }

    @Test
    void testHashCodeConsistency() {
        Having having = new Having(Filters.gt("SUM(total)", 1000));
        int hash1 = having.hashCode();
        int hash2 = having.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    void testInheritedGetConditionMethod() {
        Equal condition = Filters.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        Condition retrievedCondition = having.condition();
        assertEquals(condition, retrievedCondition);
    }

    @Test
    void testMultipleAggregatesWithAnd() {
        Condition condition = Filters.and(Filters.gt("COUNT(*)", 5), Filters.gt("SUM(amount)", 10000), Filters.lt("AVG(price)", 500));
        Having having = new Having(condition);

        assertEquals(3, having.parameters().size());
        String result = having.toString();
        assertTrue(result.contains("COUNT"));
        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("AVG"));
    }

    @Test
    void testOperatorType() {
        Having having = new Having(Filters.gt("COUNT(*)", 0));

        assertEquals(Operator.HAVING, having.operator());
        assertNotEquals(Operator.WHERE, having.operator());
        assertNotEquals(Operator.GROUP_BY, having.operator());
    }

    @Test
    void testParameters() {
        Having having = new Having(Filters.gt("SUM(amount)", 1000));

        assertNotNull(having.parameters());
        assertEquals(1, having.parameters().size());
        assertEquals(1000, having.parameters().get(0));
    }

    @Test
    void testParametersWithMultipleConditions() {
        Condition condition = Filters.and(Filters.gt("COUNT(*)", 5), Filters.lt("AVG(price)", 100));
        Having having = new Having(condition);

        assertEquals(2, having.parameters().size());
    }

    @Test
    void testParametersWithMultipleValues() {
        Condition innerCondition = Filters.between("AVG(price)", 100, 500);
        Having having = new Having(innerCondition);

        List<Object> params = having.parameters();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals(100, params.get(0));
        assertEquals(500, params.get(1));
    }

    @Test
    void testStringRepresentationFormat() {
        Having having = new Having(Filters.ge("COUNT(*)", 1));
        String result = having.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains(">="));
    }

    @Test
    void testToString() {
        Having having = new Having(Filters.gt("COUNT(*)", 10));
        String result = having.toString();

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("COUNT(*)"));
    }

    @Test
    void testToString_NoArgs() {
        Having having = new Having(Filters.gt("SUM(sales)", 50000));
        String result = having.toString();

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("SUM"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Having having = new Having(Filters.gt("COUNT(*)", 5));
        String result = having.toSql(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    void testWithAggregateFunction() {
        Having having = new Having(Filters.gt("MAX(salary)", 100000));
        String result = having.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("max") || result.contains("MAX"));
    }

    @Test
    void testWithBetweenCondition() {
        Having having = new Having(Filters.between("AVG(age)", 25, 40));

        assertEquals(2, having.parameters().size());
        assertEquals(25, having.parameters().get(0));
        assertEquals(40, having.parameters().get(1));
    }

    @Test
    void testWithComplexCondition() {
        Condition complex = Filters.and(Filters.gt("COUNT(*)", 10), Filters.between("AVG(salary)", 50000, 100000));
        Having having = new Having(complex);

        assertNotNull(having);
        assertEquals(3, having.parameters().size());
    }

    @Test
    void testWithExpression() {
        Having having = new Having(Filters.expr("COUNT(DISTINCT customer_id) > 100"));

        assertNotNull(having);
        String result = having.toString();
        assertTrue(result.contains("HAVING"));
    }

    @Test
    void testWithIsNull() {
        Having having = new Having(Filters.isNull("MAX(deletedAt)"));
        assertEquals(0, having.parameters().size());
    }

    @Test
    void testWithMinFunction() {
        Having having = new Having(Filters.lt("MIN(age)", 18));

        assertNotNull(having);
        assertEquals(1, having.parameters().size());
        assertEquals(18, having.parameters().get(0));
    }

    @Test
    void testWithNullSafeCondition() {
        Having having = new Having(Filters.isNotNull("COUNT(*)"));

        assertNotNull(having);
        assertEquals(0, having.parameters().size());
    }

    @Test
    void testWithOrCondition() {
        Condition orCondition = Filters.or(Filters.gt("SUM(revenue)", 1000000), Filters.eq("COUNT(*)", 0));
        Having having = new Having(orCondition);

        String result = having.toString();
        assertTrue(result.contains("OR"));
    }
}
