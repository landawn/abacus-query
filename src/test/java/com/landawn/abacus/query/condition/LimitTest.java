package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

public class LimitTest extends TestBase {

    @Test
    void testConditionReflectsExpressionValue() {
        Limit limit = Filters.limit("10 OFFSET 20");
        assertEquals("10 OFFSET 20", limit.condition().toString());
    }

    @Test
    void testConstructorWithCount() {
        Limit limit = new Limit(10);

        assertNotNull(limit);
        assertEquals(10, limit.count());
        assertEquals(0, limit.offset());
        assertNull(limit.expression());
    }

    @Test
    void testConstructorWithExpression() {
        Limit limit = new Limit("10 OFFSET 20");

        assertNotNull(limit);
        assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.expression());
        // The recognized integer form is parsed into concrete count/offset (literal is still retained).
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
    }

    @Test
    void testConstructorWithExpression_001() throws Exception {
        String literal = "OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY";
        final Limit limit = Filters.limit(literal);

        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", limit.expression());
        assertEquals(5, limit.offset());
        assertEquals(20, limit.count());
    }

    @Test
    void testConstructorWithExpression_CaseInsensitiveUpcasesKeywords() {
        Limit limit = new Limit("limit 10 offset 20");
        assertEquals("LIMIT 10 OFFSET 20", limit.expression());
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());

        Limit fetch = new Limit("offset 5 rows fetch next 20 rows only");
        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", fetch.expression());
        assertEquals(20, fetch.count());
        assertEquals(5, fetch.offset());
    }

    @Test
    void testConstructorWithExpression_CollapsesWhitespace() {
        // Leading/trailing trimmed; internal runs collapsed to a single space.
        assertEquals("LIMIT 10 OFFSET 20", new Limit("   10    OFFSET    20   ").expression());
        assertEquals("FETCH FIRST 5 ROWS ONLY", new Limit("FETCH   FIRST  5   ROWS   ONLY").expression());
    }

    @Test
    void testConstructorWithExpression_FetchNextWithoutOffset() {
        Limit limit = new Limit("FETCH NEXT 8 ROWS ONLY");
        assertEquals("FETCH NEXT 8 ROWS ONLY", limit.expression());
        assertEquals(8, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    void testConstructorWithExpression_FloatThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 1.0"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("1.0"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 10 OFFSET 2.5"));
    }

    @Test
    void testConstructorWithExpression_IntegerOverflowStaysOpaque() {
        // A well-formed LIMIT whose number overflows int is accepted but left opaque (no exception).
        Limit limit = new Limit("LIMIT 9999999999");
        assertEquals("LIMIT 9999999999", limit.expression());
        assertEquals(Integer.MAX_VALUE, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    void testConstructorWithExpression_InvalidFormatThrows() {
        // OFF is a typo of OFFSET.
        assertThrows(IllegalArgumentException.class, () -> new Limit("OFF 5 ROWS FETCH NEXT 20 ROWS ONLY"));
        // NEXT without a preceding FETCH.
        assertThrows(IllegalArgumentException.class, () -> new Limit("OFFSET 5 ROWS NEXT 20 ROWS ONLY"));
        // Missing trailing ONLY.
        assertThrows(IllegalArgumentException.class, () -> new Limit("FETCH FIRST 5 ROWS"));
        // Not a limit expression at all.
        assertThrows(IllegalArgumentException.class, () -> new Limit("SAMPLE(5)"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("FIRST 10 ROWS"));
    }

    @Test
    void testConstructorWithExpression_MybatisPlaceholder() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("#{limit} OFFSET #{offset}"));
    }

    @Test
    void testConstructorWithExpression_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT -1"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("-1"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 10 OFFSET -5"));
    }

    @Test
    void testConstructorWithExpression_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> new Limit((String) null));
    }

    @Test
    void testConstructorWithExpression_ParsesFetchForms() {
        Limit offsetFetch = new Limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY");
        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", offsetFetch.expression());
        assertEquals(20, offsetFetch.count());
        assertEquals(5, offsetFetch.offset());

        Limit fetchFirst = new Limit("FETCH FIRST 15 ROWS ONLY");
        assertEquals("FETCH FIRST 15 ROWS ONLY", fetchFirst.expression());
        assertEquals(15, fetchFirst.count());
        assertEquals(0, fetchFirst.offset());
    }

    @Test
    void testConstructorWithExpression_ParsesMySqlCommaForm() {
        Limit limit = new Limit("20, 10");

        // MySQL "LIMIT offset, count" -> offset=20, count=10; literal kept verbatim.
        assertEquals(SK.LIMIT + SK.SPACE + "20, 10", limit.expression());
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
    }

    @Test
    void testConstructorWithExpression_RejectsUnboundPlaceholders() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("?"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("? OFFSET ?"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("?, ?"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY"));
        assertThrows(IllegalArgumentException.class, () -> new Limit(":count"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("#{count}"));
    }

    @Test
    void testConstructorWithExpression_RowSingular() {
        Limit fetchOnly = new Limit("FETCH NEXT 5 ROW ONLY");
        assertEquals("FETCH NEXT 5 ROW ONLY", fetchOnly.expression());
        assertEquals(5, fetchOnly.count());
        assertEquals(0, fetchOnly.offset());

        Limit offsetFetch = new Limit("OFFSET 3 ROW FETCH FIRST 7 ROW ONLY");
        assertEquals(7, offsetFetch.count());
        assertEquals(3, offsetFetch.offset());
    }

    @Test
    void testConstructorWithExpressionTrims() {
        String expr = "  10 OFFSET 20  ";
        Limit limit = Filters.limit(expr);
        assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.expression());
    }

    @Test
    void testConstructorWithOffsetAndCount() {
        Limit limit = new Limit(10, 20);

        assertNotNull(limit);
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
        assertNull(limit.expression());
    }

    @Test
    void testConstructorWithPlaceholderExpression() {
        assertThrows(IllegalArgumentException.class, () -> Filters.limit("? OFFSET ?"));
    }

    @Test
    void testConstructorWithWhitespaceExpressionThrows() {
        assertThrows(IllegalArgumentException.class, () -> Filters.limit("   "));
    }

    @Test
    void testCustomExpressionFormats() {
        Limit mysqlStyle = new Limit("10, 20");
        assertEquals(SK.LIMIT + SK.SPACE + "10, 20", mysqlStyle.expression());

        Limit standardStyle = new Limit("20 OFFSET 10");
        assertEquals(SK.LIMIT + SK.SPACE + "20 OFFSET 10", standardStyle.expression());
    }

    @Test
    void testDefaultConstructor_EmptyState() {
        Limit limit = new Limit();
        Limit same = new Limit();

        assertNull(limit.expression());
        assertEquals(0, limit.count());
        assertEquals(0, limit.offset());
        assertTrue(limit.parameters().isEmpty());
        assertEquals(limit, same);
        assertEquals(limit.hashCode(), same.hashCode());
    }

    @Test
    void testDefaultConstructor_ToStringDoesNotPretendToBeLimitZero() {
        // Regression: previously toString() on a default-constructed (Kryo) Limit returned
        // "LIMIT 0", which falsely implies a fully-formed "limit zero rows" clause despite the
        // operator being null. It should now render the "null" marker, consistent with Cell.toString.
        Limit limit = new Limit();

        assertNull(limit.operator());
        String rendered = limit.toSql(NamingPolicy.NO_CHANGE);
        assertFalse(rendered.contains("LIMIT"), "Default-constructed Limit toString must not advertise a LIMIT clause; was: " + rendered);
        assertEquals("null", rendered);

        // A properly initialised LIMIT 0 still renders correctly.
        Limit zeroRows = new Limit(0);
        assertEquals("LIMIT 0", zeroRows.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testDefaultConstructorNotEqualToInitializedLimitZero() {
        Limit emptyState = new Limit();
        Limit zeroRows = new Limit(0);

        assertNotEquals(emptyState, zeroRows);
        assertNotEquals(emptyState.hashCode(), zeroRows.hashCode());
    }

    @Test
    void testEdgeCaseLargeValues() {
        Limit limit = new Limit(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, limit.offset());
        assertEquals(Integer.MAX_VALUE - 1, limit.count());
    }

    @Test
    void testEdgeCaseZeroCount() {
        Limit limit = new Limit(0);
        assertEquals(0, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    void testEquals() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);
        Limit limit3 = new Limit(20);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
        assertEquals(limit1, limit1);
    }

    @Test
    void testEquals_SameValues() {
        Limit limit1 = new Limit(50, 100);
        Limit limit2 = new Limit(50, 100);

        assertEquals(limit1, limit2);
        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    void testEqualsWithDifferentClass() {
        Limit limit = new Limit(10);
        OrderBy orderBy = new OrderBy("id");

        assertNotEquals(limit, orderBy);
    }

    @Test
    void testEqualsWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");
        Limit limit3 = new Limit("LIMIT 20");

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    void testEqualsWithMixedExpressionAndNumeric() {
        Limit limit1 = new Limit("10");
        Limit limit2 = new Limit(10);

        // These should not be equal as one uses expression and one uses numeric
        assertNotEquals(limit1, limit2);
    }

    @Test
    void testEqualsWithNull() {
        Limit limit = new Limit(10);

        assertNotEquals(limit, null);
    }

    @Test
    void testEqualsWithOffset() {
        Limit limit1 = new Limit(5, 10);
        Limit limit2 = new Limit(5, 10);
        Limit limit3 = new Limit(5, 20);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    void testEqualsWithoutExpression() {
        Limit limit1 = Filters.limit(50, 20);
        Limit limit2 = Filters.limit(50, 20);
        Limit limit3 = Filters.limit(60, 20);
        Limit limit4 = Filters.limit(50, 30);

        assertTrue(limit1.equals(limit2));
        assertFalse(limit1.equals(limit3));
        assertFalse(limit1.equals(limit4));
    }

    @Test
    void testEqualsWithSameObject() {
        Limit limit = Filters.limit(10);
        assertTrue(limit.equals(limit));
    }

    @Test
    void testGetCount() {
        Limit limit = new Limit(50);

        assertEquals(50, limit.count());
    }

    @Test
    void testGetOffset() {
        Limit limit = new Limit(25, 100);

        assertEquals(100, limit.offset());
        assertEquals(25, limit.count());
    }

    @Test
    void testHashCode() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    void testHashCodeConsistency() {
        Limit limit = new Limit(10, 50);
        int hash1 = limit.hashCode();
        int hash2 = limit.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    void testHashCodeWithOffset() {
        Limit limit1 = new Limit(10, 20);
        Limit limit2 = new Limit(10, 20);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    void testHashCodeWithoutExpression() {
        Limit limit1 = Filters.limit(50, 20);
        Limit limit2 = Filters.limit(50, 20);
        assertEquals(limit1.hashCode(), limit2.hashCode());

        Limit limit3 = Filters.limit(50, 10);
        assertNotEquals(limit1.hashCode(), limit3.hashCode());
    }

    @Test
    void testHasLiteral() {
        // String-expression constructor retains a validated numeric expression.
        assertTrue(new Limit("10 OFFSET 20").hasExpression());
        assertThrows(IllegalArgumentException.class, () -> new Limit("? OFFSET ?"));

        // Numeric constructors: no literal.
        assertFalse(new Limit(10).hasExpression());
        assertFalse(new Limit(10, 20).hasExpression());
    }

    @Test
    void testIsResolvedDoesNotUseSentinelInference() {
        Limit numeric = new Limit(Integer.MAX_VALUE);
        assertTrue(numeric.isResolved());
        assertEquals(Integer.MAX_VALUE, numeric.resolvedCount().orElseThrow());
        assertEquals(0, numeric.resolvedOffset().orElseThrow());

        Limit parsed = new Limit(String.valueOf(Integer.MAX_VALUE));
        assertTrue(parsed.isResolved());
        assertEquals(Integer.MAX_VALUE, parsed.resolvedCount().orElseThrow());

        Limit opaque = new Limit("2147483648 OFFSET 2147483649");
        assertFalse(opaque.isResolved());
        assertTrue(opaque.resolvedCount().isEmpty());
        assertTrue(opaque.resolvedOffset().isEmpty());
    }

    @Test
    void testIsResolvedIsAHardRename() throws NoSuchMethodException {
        assertEquals(boolean.class, Limit.class.getMethod("isResolved").getReturnType());
        assertThrows(NoSuchMethodException.class, () -> Limit.class.getMethod("resolved"));
    }

    @Test
    void testLargeCount() {
        Limit limit = new Limit(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, limit.count());
    }

    @Test
    void testLargeOffset() {
        Limit limit = new Limit(50, 1000000);

        assertEquals(1000000, limit.offset());
        assertEquals(50, limit.count());
    }

    @Test
    void testLiteral() {
        Limit limit = new Limit("FETCH FIRST 10 ROWS ONLY");

        assertEquals("FETCH FIRST 10 ROWS ONLY", limit.expression());
    }

    @Test
    void testLiteralWithoutExpression() {
        Limit limit = new Limit(10);

        assertNull(limit.expression());
    }

    @Test
    void testNumericConstructorInstancesEqualAndRenderIdentically() {
        // Regression: the numeric constructors must not intern their derived "count OFFSET offset"
        // expression through SqlExpression.of(...) — that cache is unbounded, so every distinct
        // (count, offset) pair from dynamic pagination would be retained forever. Fresh instances
        // must still compare equal and render identically without any shared interned instance.
        Limit limit1 = new Limit(10, 20);
        Limit limit2 = new Limit(10, 20);

        assertEquals(limit1, limit2);
        assertEquals(limit1.hashCode(), limit2.hashCode());
        assertEquals("LIMIT 10 OFFSET 20", limit1.toSql(NamingPolicy.NO_CHANGE));
        assertEquals(limit1.toSql(NamingPolicy.NO_CHANGE), limit2.toSql(NamingPolicy.NO_CHANGE));

        // Wrapped conditions are structurally equal but deliberately not the same interned object.
        assertEquals(limit1.condition(), limit2.condition());
        assertNotSame(limit1.condition(), limit2.condition());
    }

    @Test
    void testOperatorType() {
        Limit limit = new Limit(10);

        assertEquals(Operator.LIMIT, limit.operator());
        assertNotEquals(Operator.WHERE, limit.operator());
    }

    @Test
    void testPagination() {
        // Page 1: First 10 records
        Limit page1 = new Limit(10, 0);
        assertEquals(0, page1.offset());
        assertEquals(10, page1.count());

        // Page 2: Next 10 records
        Limit page2 = new Limit(10, 10);
        assertEquals(10, page2.offset());
        assertEquals(10, page2.count());

        // Page 3: Next 10 records
        Limit page3 = new Limit(10, 20);
        assertEquals(20, page3.offset());
        assertEquals(10, page3.count());
    }

    @Test
    void testParameters() {
        Limit limit = new Limit(10);

        assertNotNull(limit.parameters());
        assertTrue(limit.parameters().isEmpty());
    }

    @Test
    void testToString_NoArgsWithCountOnly() {
        Limit limit = new Limit(15);
        String result = limit.toString();

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("15"));
    }

    @Test
    void testToString_NoArgsWithExpression() {
        Limit limit = new Limit("FETCH FIRST 5 ROWS ONLY");
        String result = limit.toString();

        assertEquals("FETCH FIRST 5 ROWS ONLY", result);
    }

    @Test
    void testToString_NoArgsWithOffset() {
        Limit limit = new Limit(20, 10);
        String result = limit.toString();

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("OFFSET"));
        assertTrue(result.contains("10"));
    }

    @Test
    void testToStringFormat() {
        Limit limit = new Limit(100);
        String result = limit.toSql(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("100"));
    }

    @Test
    void testToStringWithCountOnly() {
        Limit limit = new Limit(10);
        String result = limit.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertFalse(result.contains("OFFSET"));
    }

    @Test
    void testToStringWithDifferentNamingPolicy() {
        Limit limit = new Limit(10);
        String result1 = limit.toSql(NamingPolicy.NO_CHANGE);
        String result2 = limit.toSql(NamingPolicy.SNAKE_CASE);

        // Naming policy shouldn't affect LIMIT clause output significantly
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("10"));
        assertTrue(result2.contains("10"));
    }

    @Test
    void testToStringWithExpression() {
        Limit limit = new Limit("FETCH FIRST 5 ROWS ONLY");
        String result = limit.toSql(NamingPolicy.NO_CHANGE);

        assertEquals("FETCH FIRST 5 ROWS ONLY", result);
    }

    @Test
    void testToStringWithOffsetAndCount() {
        Limit limit = new Limit(10, 20);
        String result = limit.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("OFFSET"));
        assertTrue(result.contains("20"));
    }

    @Test
    void testZeroOffset() {
        Limit limit = new Limit(10, 0);

        assertEquals(0, limit.offset());
        assertEquals(10, limit.count());
    }
}
