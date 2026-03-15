package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Limit2025Test extends TestBase {

    @Test
    public void testConstructorWithCount() {
        Limit limit = new Limit(10);

        assertNotNull(limit);
        assertEquals(10, limit.getCount());
        assertEquals(0, limit.getOffset());
        assertNull(limit.getExpression());
    }

    @Test
    public void testConstructorWithOffsetAndCount() {
        Limit limit = new Limit(10, 20);

        assertNotNull(limit);
        assertEquals(10, limit.getCount());
        assertEquals(20, limit.getOffset());
        assertNull(limit.getExpression());
    }

    @Test
    public void testConstructorWithExpression() {
        Limit limit = new Limit("10 OFFSET 20");

        assertNotNull(limit);
        assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.getExpression());
        assertEquals(Integer.MAX_VALUE, limit.getCount());
        assertEquals(0, limit.getOffset());
    }

    @Test
    public void testGetCount() {
        Limit limit = new Limit(50);

        assertEquals(50, limit.getCount());
    }

    @Test
    public void testGetOffset() {
        Limit limit = new Limit(25, 100);

        assertEquals(100, limit.getOffset());
        assertEquals(25, limit.getCount());
    }

    @Test
    public void testGetExpr() {
        Limit limit = new Limit("FIRST 10 ROWS");

        assertEquals("FIRST 10 ROWS", limit.getExpression());
    }

    @Test
    public void testGetExprWithoutExpression() {
        Limit limit = new Limit(10);

        assertNull(limit.getExpression());
    }

    @Test
    public void testGetParameters() {
        Limit limit = new Limit(10);

        assertNotNull(limit.getParameters());
        assertTrue(limit.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        Limit limit = new Limit(15);
        limit.clearParameters();

        // Should do nothing, but not throw exception
        assertNotNull(limit.getParameters());
        assertTrue(limit.getParameters().isEmpty());
    }

    @Test
    public void testToStringWithCountOnly() {
        Limit limit = new Limit(10);
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertFalse(result.contains("OFFSET"));
    }

    @Test
    public void testToStringWithOffsetAndCount() {
        Limit limit = new Limit(10, 20);
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("OFFSET"));
        assertTrue(result.contains("20"));
    }

    @Test
    public void testToStringWithExpression() {
        Limit limit = new Limit("FIRST 5 ROWS");
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertEquals("FIRST 5 ROWS", result);
    }

    @Test
    public void testEquals() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);
        Limit limit3 = new Limit(20);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
        assertEquals(limit1, limit1);
    }

    @Test
    public void testEqualsWithOffset() {
        Limit limit1 = new Limit(5, 10);
        Limit limit2 = new Limit(5, 10);
        Limit limit3 = new Limit(5, 20);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    public void testEqualsWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");
        Limit limit3 = new Limit("LIMIT 20");

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    public void testEqualsWithNull() {
        Limit limit = new Limit(10);

        assertNotEquals(limit, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Limit limit = new Limit(10);
        OrderBy orderBy = new OrderBy("id");

        assertNotEquals(limit, (Object) orderBy);
    }

    @Test
    public void testHashCode() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeWithOffset() {
        Limit limit1 = new Limit(10, 20);
        Limit limit2 = new Limit(10, 20);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Limit limit = new Limit(10, 50);
        int hash1 = limit.hashCode();
        int hash2 = limit.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testOperatorType() {
        Limit limit = new Limit(10);

        assertEquals(Operator.LIMIT, limit.operator());
        assertNotEquals(Operator.WHERE, limit.operator());
    }

    @Test
    public void testZeroOffset() {
        Limit limit = new Limit(10, 0);

        assertEquals(0, limit.getOffset());
        assertEquals(10, limit.getCount());
    }

    @Test
    public void testLargeOffset() {
        Limit limit = new Limit(50, 1000000);

        assertEquals(1000000, limit.getOffset());
        assertEquals(50, limit.getCount());
    }

    @Test
    public void testLargeCount() {
        Limit limit = new Limit(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, limit.getCount());
    }

    @Test
    public void testToStringFormat() {
        Limit limit = new Limit(100);
        String result = limit.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("100"));
    }

    @Test
    public void testPagination() {
        // Page 1: First 10 records
        Limit page1 = new Limit(10, 0);
        assertEquals(0, page1.getOffset());
        assertEquals(10, page1.getCount());

        // Page 2: Next 10 records
        Limit page2 = new Limit(10, 10);
        assertEquals(10, page2.getOffset());
        assertEquals(10, page2.getCount());

        // Page 3: Next 10 records
        Limit page3 = new Limit(10, 20);
        assertEquals(20, page3.getOffset());
        assertEquals(10, page3.getCount());
    }

    @Test
    public void testCustomExpressionFormats() {
        Limit mysqlStyle = new Limit("10, 20");
        assertEquals(SK.LIMIT + SK.SPACE + "10, 20", mysqlStyle.getExpression());

        Limit standardStyle = new Limit("20 OFFSET 10");
        assertEquals(SK.LIMIT + SK.SPACE + "20 OFFSET 10", standardStyle.getExpression());
    }

    @Test
    public void testToString_NoArgsWithCountOnly() {
        Limit limit = new Limit(15);
        String result = limit.toString();

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("15"));
    }

    @Test
    public void testToString_NoArgsWithOffset() {
        Limit limit = new Limit(20, 10);
        String result = limit.toString();

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("OFFSET"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testToString_NoArgsWithExpression() {
        Limit limit = new Limit("FIRST 5 ROWS");
        String result = limit.toString();

        assertEquals("FIRST 5 ROWS", result);
    }

    @Test
    public void testEquals_SameValues() {
        Limit limit1 = new Limit(50, 100);
        Limit limit2 = new Limit(50, 100);

        assertEquals(limit1, limit2);
        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testToStringWithDifferentNamingPolicy() {
        Limit limit = new Limit(10);
        String result1 = limit.toString(NamingPolicy.NO_CHANGE);
        String result2 = limit.toString(NamingPolicy.SNAKE_CASE);

        // Naming policy shouldn't affect LIMIT clause output significantly
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("10"));
        assertTrue(result2.contains("10"));
    }

    @Test
    public void testEdgeCaseZeroCount() {
        Limit limit = new Limit(0);
        assertEquals(0, limit.getCount());
        assertEquals(0, limit.getOffset());
    }

    @Test
    public void testEdgeCaseLargeValues() {
        Limit limit = new Limit(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, limit.getOffset());
        assertEquals(Integer.MAX_VALUE - 1, limit.getCount());
    }

    @Test
    public void testEqualsWithMixedExpressionAndNumeric() {
        Limit limit1 = new Limit("10");
        Limit limit2 = new Limit(10);

        // These should not be equal as one uses expression and one uses numeric
        assertNotEquals(limit1, limit2);
    }
}

public class LimitTest extends TestBase {

    @Test
    public void testConstructorWithCount() {
        Limit limit = Filters.limit(10);
        Assertions.assertEquals(10, limit.getCount());
        Assertions.assertEquals(0, limit.getOffset());
        Assertions.assertNull(limit.getExpression());
    }

    @Test
    public void testConstructorWithOffsetAndCount() {
        Limit limit = Filters.limit(50, 20);
        Assertions.assertEquals(50, limit.getCount());
        Assertions.assertEquals(20, limit.getOffset());
        Assertions.assertNull(limit.getExpression());
    }

    @Test
    public void testConstructorWithExpression() {
        String expr = "10 OFFSET 20";
        Limit limit = Filters.limit(expr);
        Assertions.assertEquals(SK.LIMIT + SK.SPACE + expr, limit.getExpression());
        Assertions.assertEquals(Integer.MAX_VALUE, limit.getCount());
        Assertions.assertEquals(0, limit.getOffset());
    }

    @Test
    public void testConstructorWithExpressionTrims() {
        String expr = "  10 OFFSET 20  ";
        Limit limit = Filters.limit(expr);
        Assertions.assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.getExpression());
    }

    @Test
    public void testConstructorWithPlaceholderExpression() {
        Limit limit = Filters.limit("? OFFSET ?");
        Assertions.assertEquals(SK.LIMIT + SK.SPACE + "? OFFSET ?", limit.getExpression());
    }

    @Test
    public void testConstructorWithWhitespaceExpressionThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.limit("   "));
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
        limit.clearParameters();
        Assertions.assertEquals(10, limit.getCount());
        Assertions.assertEquals(0, limit.getOffset());
        Assertions.assertTrue(limit.getParameters().isEmpty());
    }

    @Test
    public void testToStringWithCountOnly() {
        Limit limit = Filters.limit(10);
        String result = limit.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertEquals("LIMIT 10", result);
    }

    @Test
    public void testToStringWithOffsetAndCount() {
        Limit limit = Filters.limit(50, 20);
        String result = limit.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertEquals("LIMIT 50 OFFSET 20", result);
    }

    @Test
    public void testConditionReflectsExpressionValue() {
        Limit limit = Filters.limit("10 OFFSET 20");
        Assertions.assertEquals("10 OFFSET 20", limit.getCondition().toString());
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
        Limit limit1 = Filters.limit(50, 20);
        Limit limit2 = Filters.limit(50, 20);
        Assertions.assertEquals(limit1.hashCode(), limit2.hashCode());

        Limit limit3 = Filters.limit(50, 10);
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
        Limit limit1 = Filters.limit(50, 20);
        Limit limit2 = Filters.limit(50, 20);
        Limit limit3 = Filters.limit(60, 20);
        Limit limit4 = Filters.limit(50, 30);

        Assertions.assertTrue(limit1.equals(limit2));
        Assertions.assertFalse(limit1.equals(limit3));
        Assertions.assertFalse(limit1.equals(limit4));
    }
}
