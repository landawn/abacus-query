package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class LimitTest extends TestBase {

    @Test
    public void testConstructorWithCount() {
        Limit limit = Filters.limit(10);
        Assertions.assertEquals(10, limit.getCount());
        Assertions.assertEquals(0, limit.getOffset());
        Assertions.assertNull(limit.getExpr());
    }

    @Test
    public void testConstructorWithOffsetAndCount() {
        Limit limit = Filters.limit(20, 50);
        Assertions.assertEquals(50, limit.getCount());
        Assertions.assertEquals(20, limit.getOffset());
        Assertions.assertNull(limit.getExpr());
    }

    @Test
    public void testConstructorWithExpression() {
        String expr = "10 OFFSET 20";
        Limit limit = Filters.limit(expr);
        Assertions.assertEquals(expr, limit.getExpr());
        Assertions.assertEquals(Integer.MAX_VALUE, limit.getCount());
        Assertions.assertEquals(0, limit.getOffset());
    }

    @Test
    public void testGetParameters() {
        Limit limit = Filters.limit(10);
        List<Object> params = limit.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Limit limit = Filters.limit(10);
        // Should not throw exception
        limit.clearParameters();
    }

    @Test
    public void testAndThrowsException() {
        Limit limit = Filters.limit(10);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            limit.and(Filters.eq("id", 1));
        });
    }

    @Test
    public void testOrThrowsException() {
        Limit limit = Filters.limit(10);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            limit.or(Filters.eq("id", 1));
        });
    }

    @Test
    public void testNotThrowsException() {
        Limit limit = Filters.limit(10);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            limit.not();
        });
    }

    @Test
    public void testToStringWithCountOnly() {
        Limit limit = Filters.limit(10);
        String result = limit.toString(NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertEquals("LIMIT 10", result);
    }

    @Test
    public void testToStringWithOffsetAndCount() {
        Limit limit = Filters.limit(20, 50);
        String result = limit.toString(NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertEquals("LIMIT 50 OFFSET 20", result);
    }

    @Test
    public void testHashCodeWithExpression() {
        String expr = "10 OFFSET 20";
        Limit limit1 = Filters.limit(expr);
        Limit limit2 = Filters.limit(expr);
        Assertions.assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeWithoutExpression() {
        Limit limit1 = Filters.limit(20, 50);
        Limit limit2 = Filters.limit(20, 50);
        Assertions.assertEquals(limit1.hashCode(), limit2.hashCode());

        Limit limit3 = Filters.limit(10, 50);
        Assertions.assertNotEquals(limit1.hashCode(), limit3.hashCode());
    }

    @Test
    public void testEqualsWithSameObject() {
        Limit limit = Filters.limit(10);
        Assertions.assertTrue(limit.equals(limit));
    }

    @Test
    public void testEqualsWithNull() {
        Limit limit = Filters.limit(10);
        Assertions.assertFalse(limit.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Limit limit = Filters.limit(10);
        Assertions.assertFalse(limit.equals("not a limit"));
    }

    @Test
    public void testEqualsWithExpression() {
        String expr = "10 OFFSET 20";
        Limit limit1 = Filters.limit(expr);
        Limit limit2 = Filters.limit(expr);
        Limit limit3 = Filters.limit("different expr");

        Assertions.assertTrue(limit1.equals(limit2));
        Assertions.assertFalse(limit1.equals(limit3));
    }

    @Test
    public void testEqualsWithoutExpression() {
        Limit limit1 = Filters.limit(20, 50);
        Limit limit2 = Filters.limit(20, 50);
        Limit limit3 = Filters.limit(20, 60);
        Limit limit4 = Filters.limit(30, 50);

        Assertions.assertTrue(limit1.equals(limit2));
        Assertions.assertFalse(limit1.equals(limit3));
        Assertions.assertFalse(limit1.equals(limit4));
    }
}