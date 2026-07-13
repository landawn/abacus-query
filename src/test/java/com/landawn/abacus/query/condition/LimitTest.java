package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

@Tag("2025")
public class LimitTest extends TestBase {
    @Test
    public void testConstructorWithCount() {
        Limit limit = new Limit(10);

        assertNotNull(limit);
        assertEquals(10, limit.count());
        assertEquals(0, limit.offset());
        assertNull(limit.literal());
    }

    @Test
    public void testConstructorWithOffsetAndCount() {
        Limit limit = new Limit(10, 20);

        assertNotNull(limit);
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
        assertNull(limit.literal());
    }

    @Test
    public void testConstructorWithExpression() {
        Limit limit = new Limit("10 OFFSET 20");

        assertNotNull(limit);
        assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.literal());
        // The recognized integer form is parsed into concrete count/offset (literal is still retained).
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
    }

    @Test
    public void testHasLiteral() {
        // String-expression constructor: literal mode, even when the expression is parsed or opaque.
        assertTrue(new Limit("10 OFFSET 20").hasLiteral());
        assertTrue(new Limit("? OFFSET ?").hasLiteral());

        // Numeric constructors: no literal.
        assertFalse(new Limit(10).hasLiteral());
        assertFalse(new Limit(10, 20).hasLiteral());
    }

    @Test
    public void testConstructorWithExpression_ParsesMySqlCommaForm() {
        Limit limit = new Limit("20, 10");

        // MySQL "LIMIT offset, count" -> offset=20, count=10; literal kept verbatim.
        assertEquals(SK.LIMIT + SK.SPACE + "20, 10", limit.literal());
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());
    }

    @Test
    public void testConstructorWithExpression_ParsesFetchForms() {
        Limit offsetFetch = new Limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY");
        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", offsetFetch.literal());
        assertEquals(20, offsetFetch.count());
        assertEquals(5, offsetFetch.offset());

        Limit fetchFirst = new Limit("FETCH FIRST 15 ROWS ONLY");
        assertEquals("FETCH FIRST 15 ROWS ONLY", fetchFirst.literal());
        assertEquals(15, fetchFirst.count());
        assertEquals(0, fetchFirst.offset());
    }

    @Test
    public void testConstructorWithExpression_PlaceholderStaysOpaque() {
        Limit limit = new Limit("? OFFSET ?");

        // A placeholder-bearing expression cannot be parsed to integers: it stays opaque.
        assertEquals(SK.LIMIT + SK.SPACE + "? OFFSET ?", limit.literal());
        assertEquals(Integer.MAX_VALUE, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    public void testConstructorWithExpression_CaseInsensitiveUpcasesKeywords() {
        Limit limit = new Limit("limit 10 offset 20");
        assertEquals("LIMIT 10 OFFSET 20", limit.literal());
        assertEquals(10, limit.count());
        assertEquals(20, limit.offset());

        Limit fetch = new Limit("offset 5 rows fetch next 20 rows only");
        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", fetch.literal());
        assertEquals(20, fetch.count());
        assertEquals(5, fetch.offset());
    }

    @Test
    public void testConstructorWithExpression_CollapsesWhitespace() {
        // Leading/trailing trimmed; internal runs collapsed to a single space.
        assertEquals("LIMIT 10 OFFSET 20", new Limit("   10    OFFSET    20   ").literal());
        assertEquals("FETCH FIRST 5 ROWS ONLY", new Limit("FETCH   FIRST  5   ROWS   ONLY").literal());
    }

    @Test
    public void testConstructorWithExpression_PreservesParameterNameCase() {
        // Keywords are upper-cased, but named-parameter tokens keep their original case.
        Limit mybatis = new Limit("#{maxRows} offset #{startRow}");
        assertEquals("LIMIT #{maxRows} OFFSET #{startRow}", mybatis.literal());
        assertEquals(Integer.MAX_VALUE, mybatis.count());

        Limit named = new Limit(":Cnt");
        assertEquals("LIMIT :Cnt", named.literal());
    }

    @Test
    public void testConstructorWithExpression_PreservesKeywordNamedPlaceholderWithSpaces() {
        // A #{...} body may contain internal spaces and be spelled like a keyword. Its parameter name must
        // survive verbatim (not whitespace-collapsed and not upper-cased), while the leading LIMIT keyword
        // is still normalized. Regression for the placeholder-corruption bug.
        Limit spacedOffset = new Limit("LIMIT #{ offset }");
        assertEquals("LIMIT #{ offset }", spacedOffset.literal());
        assertEquals(Integer.MAX_VALUE, spacedOffset.count());
        assertEquals(0, spacedOffset.offset());

        Limit spacedPair = new Limit("#{ maxRows } offset #{ startRow }");
        assertEquals("LIMIT #{ maxRows } OFFSET #{ startRow }", spacedPair.literal());

        // A :name placeholder spelled like a keyword must also be preserved (whole-token, so :offset != OFFSET).
        Limit namedOffset = new Limit("LIMIT :offset");
        assertEquals("LIMIT :offset", namedOffset.literal());
    }

    @Test
    public void testConstructorWithExpression_RowSingular() {
        Limit fetchOnly = new Limit("FETCH NEXT 5 ROW ONLY");
        assertEquals("FETCH NEXT 5 ROW ONLY", fetchOnly.literal());
        assertEquals(5, fetchOnly.count());
        assertEquals(0, fetchOnly.offset());

        Limit offsetFetch = new Limit("OFFSET 3 ROW FETCH FIRST 7 ROW ONLY");
        assertEquals(7, offsetFetch.count());
        assertEquals(3, offsetFetch.offset());
    }

    @Test
    public void testConstructorWithExpression_FetchNextWithoutOffset() {
        Limit limit = new Limit("FETCH NEXT 8 ROWS ONLY");
        assertEquals("FETCH NEXT 8 ROWS ONLY", limit.literal());
        assertEquals(8, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    public void testConstructorWithExpression_IntegerOverflowStaysOpaque() {
        // A well-formed LIMIT whose number overflows int is accepted but left opaque (no exception).
        Limit limit = new Limit("LIMIT 9999999999");
        assertEquals("LIMIT 9999999999", limit.literal());
        assertEquals(Integer.MAX_VALUE, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    public void testConstructorWithExpression_FloatThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 1.0"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("1.0"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 10 OFFSET 2.5"));
    }

    @Test
    public void testConstructorWithExpression_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT -1"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("-1"));
        assertThrows(IllegalArgumentException.class, () -> new Limit("LIMIT 10 OFFSET -5"));
    }

    @Test
    public void testConstructorWithExpression_InvalidFormatThrows() {
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
    public void testGetCount() {
        Limit limit = new Limit(50);

        assertEquals(50, limit.count());
    }

    @Test
    public void testGetOffset() {
        Limit limit = new Limit(25, 100);

        assertEquals(100, limit.offset());
        assertEquals(25, limit.count());
    }

    @Test
    public void testLiteral() {
        Limit limit = new Limit("FETCH FIRST 10 ROWS ONLY");

        assertEquals("FETCH FIRST 10 ROWS ONLY", limit.literal());
    }

    @Test
    public void testLiteralWithoutExpression() {
        Limit limit = new Limit(10);

        assertNull(limit.literal());
    }

    @Test
    public void testParameters() {
        Limit limit = new Limit(10);

        assertNotNull(limit.parameters());
        assertTrue(limit.parameters().isEmpty());
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
        Limit limit = new Limit("FETCH FIRST 5 ROWS ONLY");
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertEquals("FETCH FIRST 5 ROWS ONLY", result);
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

        assertNotEquals(limit, orderBy);
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

        assertEquals(0, limit.offset());
        assertEquals(10, limit.count());
    }

    @Test
    public void testLargeOffset() {
        Limit limit = new Limit(50, 1000000);

        assertEquals(1000000, limit.offset());
        assertEquals(50, limit.count());
    }

    @Test
    public void testLargeCount() {
        Limit limit = new Limit(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, limit.count());
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
    public void testCustomExpressionFormats() {
        Limit mysqlStyle = new Limit("10, 20");
        assertEquals(SK.LIMIT + SK.SPACE + "10, 20", mysqlStyle.literal());

        Limit standardStyle = new Limit("20 OFFSET 10");
        assertEquals(SK.LIMIT + SK.SPACE + "20 OFFSET 10", standardStyle.literal());
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
        Limit limit = new Limit("FETCH FIRST 5 ROWS ONLY");
        String result = limit.toString();

        assertEquals("FETCH FIRST 5 ROWS ONLY", result);
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
        assertEquals(0, limit.count());
        assertEquals(0, limit.offset());
    }

    @Test
    public void testEdgeCaseLargeValues() {
        Limit limit = new Limit(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, limit.offset());
        assertEquals(Integer.MAX_VALUE - 1, limit.count());
    }

    @Test
    public void testEqualsWithMixedExpressionAndNumeric() {
        Limit limit1 = new Limit("10");
        Limit limit2 = new Limit(10);

        // These should not be equal as one uses expression and one uses numeric
        assertNotEquals(limit1, limit2);
    }

    @Test
    public void testConstructorWithExpressionTrims() {
        String expr = "  10 OFFSET 20  ";
        Limit limit = Filters.limit(expr);
        Assertions.assertEquals(SK.LIMIT + SK.SPACE + "10 OFFSET 20", limit.literal());
    }

    @Test
    public void testConstructorWithPlaceholderExpression() {
        Limit limit = Filters.limit("? OFFSET ?");
        Assertions.assertEquals(SK.LIMIT + SK.SPACE + "? OFFSET ?", limit.literal());
    }

    @Test
    public void testConstructorWithWhitespaceExpressionThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.limit("   "));
    }

    @Test
    public void testConstructorWithExpression_001() throws Exception {
        String literal = "OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY";
        final Limit limit = Filters.limit(literal);

        assertEquals("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY", limit.literal());
        assertEquals(5, limit.offset());
        assertEquals(20, limit.count());
    }

    @Test
    public void testConditionReflectsExpressionValue() {
        Limit limit = Filters.limit("10 OFFSET 20");
        Assertions.assertEquals("10 OFFSET 20", limit.condition().toString());
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
    public void testEqualsWithoutExpression() {
        Limit limit1 = Filters.limit(50, 20);
        Limit limit2 = Filters.limit(50, 20);
        Limit limit3 = Filters.limit(60, 20);
        Limit limit4 = Filters.limit(50, 30);

        Assertions.assertTrue(limit1.equals(limit2));
        Assertions.assertFalse(limit1.equals(limit3));
        Assertions.assertFalse(limit1.equals(limit4));
    }

    @Test
    public void testDefaultConstructor_EmptyState() {
        Limit limit = new Limit();
        Limit same = new Limit();

        Assertions.assertNull(limit.literal());
        Assertions.assertEquals(0, limit.count());
        Assertions.assertEquals(0, limit.offset());
        Assertions.assertTrue(limit.parameters().isEmpty());
        Assertions.assertEquals(limit, same);
        Assertions.assertEquals(limit.hashCode(), same.hashCode());
    }

    @Test
    public void testConstructorWithExpression_NullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit((String) null));
    }

    @Test
    public void testConstructorWithExpression_MybatisPlaceholder() {
        Limit limit = new Limit("#{limit} OFFSET #{offset}");

        Assertions.assertEquals("LIMIT #{limit} OFFSET #{offset}", limit.literal());
        Assertions.assertEquals("#{limit} OFFSET #{offset}", limit.condition().toString());
    }

    @Test
    public void testDefaultConstructor_ToStringDoesNotPretendToBeLimitZero() {
        // Regression: previously toString() on a default-constructed (Kryo) Limit returned
        // "LIMIT 0", which falsely implies a fully-formed "limit zero rows" clause despite the
        // operator being null. It should now render the "null" marker, consistent with Cell.toString.
        Limit limit = new Limit();

        Assertions.assertNull(limit.operator());
        String rendered = limit.toString(NamingPolicy.NO_CHANGE);
        Assertions.assertFalse(rendered.contains("LIMIT"), "Default-constructed Limit toString must not advertise a LIMIT clause; was: " + rendered);
        Assertions.assertEquals("null", rendered);

        // A properly initialised LIMIT 0 still renders correctly.
        Limit zeroRows = new Limit(0);
        Assertions.assertEquals("LIMIT 0", zeroRows.toString(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testDefaultConstructorNotEqualToInitializedLimitZero() {
        Limit emptyState = new Limit();
        Limit zeroRows = new Limit(0);

        Assertions.assertNotEquals(emptyState, zeroRows);
        Assertions.assertNotEquals(emptyState.hashCode(), zeroRows.hashCode());
    }

    @Test
    public void testResolvedStateDoesNotUseSentinelInference() {
        Limit numeric = new Limit(Integer.MAX_VALUE);
        Assertions.assertTrue(numeric.resolved());
        Assertions.assertEquals(Integer.MAX_VALUE, numeric.resolvedCount().orElseThrow());
        Assertions.assertEquals(0, numeric.resolvedOffset().orElseThrow());

        Limit parsed = new Limit(String.valueOf(Integer.MAX_VALUE));
        Assertions.assertTrue(parsed.resolved());
        Assertions.assertEquals(Integer.MAX_VALUE, parsed.resolvedCount().orElseThrow());

        Limit opaque = new Limit("? OFFSET ?");
        Assertions.assertFalse(opaque.resolved());
        Assertions.assertTrue(opaque.resolvedCount().isEmpty());
        Assertions.assertTrue(opaque.resolvedOffset().isEmpty());
    }
}
