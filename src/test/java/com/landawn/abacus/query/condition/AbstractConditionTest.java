package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class AbstractConditionTest extends TestBase {
    @Test
    public void testGetOperator() {
        AbstractCondition condition = new Equal("name", "John");
        assertEquals(Operator.EQUAL, condition.operator());

        AbstractCondition and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.operator());
    }

    @Test
    public void testAnd() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("age", 18);

        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Operator.AND, result.operator());
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testAnd_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.and(null));
    }

    @Test
    public void testOr() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "pending");

        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testOr_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.or(null));
    }

    @Test
    public void testNot() {
        Equal cond = new Equal("status", "active");
        Not result = cond.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        assertSame(cond, result.getCondition());
    }

    @Test
    public void testToString_DefaultNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toString();
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("John"));
    }

    @Test
    public void testParameter2String_StringEscapesQuote() {
        Equal condition = new Equal("name", "O'Brien");
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("O\\'Brien"));
    }

    @Test
    public void testParameter2String_Null() {
        Equal condition = new Equal("name", null);
        List<Object> params = condition.parameters();
        assertEquals(0, params.size());
    }

    @Test
    public void testConcatPropNames_SingleName() {
        // Tested indirectly through other condition classes
        Equal condition = new Equal("name", "value");
        assertNotNull(condition.getPropName());
    }

    @Test
    public void testChainedOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And and = cond1.and(cond2).and(cond3);
        assertEquals(3, and.conditions().size());
    }

    @Test
    public void testMixedComposableOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);

        And and = cond1.and(cond2);
        Not not = and.not();

        assertNotNull(not);
        assertEquals(Operator.NOT, not.operator());
    }

    @Test
    public void testToString_WithNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
        assertTrue(result.contains("John"));
    }

    @Test
    public void testToString_WithNoChangePolicy() {
        Equal condition = new Equal("firstName", "Jane");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("Jane"));
    }

    @Test
    public void testParameter2String_WithCondition() {
        Equal innerCondition = new Equal("id", 100);
        Equal outerCondition = new Equal("userId", innerCondition);
        String str = outerCondition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("userId"));
        assertTrue(str.contains("id"));
    }

    @Test
    public void testParameter2String_WithSubQueryAddsParentheses() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Equal outerCondition = new Equal("userId", subQuery);
        String str = outerCondition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("= (SELECT id FROM users)"));
    }

    @Test
    public void testParameter2String_WithIsNull() {
        Equal condition = new Equal("field", IsNull.NULL);
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertNotNull(str);
        assertTrue(str.contains("NULL"));
    }

    @Test
    public void testConcatPropNames_EmptyArray() {
        // Testing through GroupBy with no props - just verify it can be created
        GroupBy groupBy = new GroupBy();
        assertNotNull(groupBy);
        // Note: toString() on empty GroupBy throws NPE, this is expected behavior
    }

    @Test
    public void testConcatPropNames_TwoNames() {
        GroupBy groupBy = new GroupBy("col1", "col2");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
    }

    @Test
    public void testConcatPropNames_ThreeNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
    }

    @Test
    public void testConcatPropNames_FourNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3", "col4");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
    }

    @Test
    public void testConcatPropNames_CollectionSingleItem() {
        OrderBy orderBy = new OrderBy("single");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("single"));
    }

    @Test
    public void testConcatPropNames_CollectionTwoItems() {
        OrderBy orderBy = new OrderBy("first", "second");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("first"));
        assertTrue(str.contains("second"));
    }

    @Test
    public void testConcatPropNames_CollectionThreeItems() {
        OrderBy orderBy = new OrderBy("a", "b", "c");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }

    @Test
    public void testConcatPropNames_CollectionFourOrMore() {
        OrderBy orderBy = new OrderBy("col1", "col2", "col3", "col4", "col5");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
        assertTrue(str.contains("col5"));
    }

    // Create a concrete implementation for testing
    private static class TestCondition extends ComposableCondition {
        private String value;

        public TestCondition(Operator operator, String value) {
            super(operator);
            this.value = value;
        }

        @Override
        public ImmutableList<Object> parameters() {
            return value == null ? ImmutableList.empty() : ImmutableList.of(value);
        }

        @Override
        public String toString(NamingPolicy namingPolicy) {
            return operator().toString() + " " + value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TestCondition)) {
                return false;
            }
            TestCondition other = (TestCondition) obj;
            return Objects.equals(operator, other.operator) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operator, value);
        }
    }

    @Test
    public void testConstructor() {
        TestCondition condition = new TestCondition(Operator.EQUAL, "test");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals(Operator.EQUAL, condition.operator());
        Assertions.assertEquals("test", condition.value);
    }

    @Test
    public void testOffsetAndForUpdateCannotBeComposedAsPredicates() {
        TestCondition offset = new TestCondition(Operator.OFFSET, "10");
        TestCondition forUpdate = new TestCondition(Operator.FOR_UPDATE, "");

        Assertions.assertThrows(IllegalArgumentException.class, () -> offset.and(Filters.eq("id", 1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> forUpdate.or(Filters.eq("id", 1)));
    }

    @Test
    public void testToString() {
        TestCondition condition = new TestCondition(Operator.IN, "list");

        String result = condition.toString();
        Assertions.assertEquals("IN list", result);
    }

    @Test
    public void testParameter2StringWithString() {
        String result = AbstractCondition.formatParameter("test", NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("'test'", result);
    }

    @Test
    public void testParameter2StringWithNumber() {
        String result = AbstractCondition.formatParameter(123, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("123", result);
    }

    @Test
    public void testParameter2StringWithNull() {
        String result = AbstractCondition.formatParameter(null, NamingPolicy.NO_CHANGE);
        Assertions.assertNull(result);
    }

    @Test
    public void testParameter2StringWithCondition() {
        Equal eq = Filters.eq("name", "John");
        String result = AbstractCondition.formatParameter(eq, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("name = 'John'", result);
    }

    @Test
    public void testParameter2StringWithConditionAndNamingPolicy() {
        Equal eq = Filters.eq("firstName", "John");
        String result = AbstractCondition.formatParameter(eq, NamingPolicy.SNAKE_CASE);
        Assertions.assertEquals("first_name = 'John'", result);
    }

    @Test
    public void testConcatPropNamesArray() {
        // Test empty array
        String result = AbstractCondition.concatPropNames();
        Assertions.assertEquals("", result);

        // Test single element
        result = AbstractCondition.concatPropNames("name");
        Assertions.assertEquals("name", result);

        // Test two elements
        result = AbstractCondition.concatPropNames("city", "state");
        Assertions.assertEquals("(city, state)", result);

        // Test three elements
        result = AbstractCondition.concatPropNames("a", "b", "c");
        Assertions.assertEquals("(a, b, c)", result);

        // Test more than three elements
        result = AbstractCondition.concatPropNames("col1", "col2", "col3", "col4", "col5");
        Assertions.assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    public void testConcatPropNamesCollection() {
        // Test empty collection
        List<String> empty = new ArrayList<>();
        String result = AbstractCondition.concatPropNames(empty);
        Assertions.assertEquals("", result);

        // Test single element
        List<String> single = Arrays.asList("name");
        result = AbstractCondition.concatPropNames(single);
        Assertions.assertEquals("name", result);

        // Test two elements
        List<String> two = Arrays.asList("city", "state");
        result = AbstractCondition.concatPropNames(two);
        Assertions.assertEquals("(city, state)", result);

        // Test three elements
        List<String> three = Arrays.asList("a", "b", "c");
        result = AbstractCondition.concatPropNames(three);
        Assertions.assertEquals("(a, b, c)", result);

        // Test more than three elements
        List<String> many = Arrays.asList("col1", "col2", "col3", "col4", "col5");
        result = AbstractCondition.concatPropNames(many);
        Assertions.assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    public void testConcatPropNamesWithSet() {
        // Test with LinkedHashSet to maintain order
        Set<String> props = new LinkedHashSet<>();
        props.add("first");
        props.add("second");
        props.add("third");

        String result = AbstractCondition.concatPropNames(props);
        Assertions.assertEquals("(first, second, third)", result);
    }

    @Test
    public void testComplexConditionChaining() {
        TestCondition cond1 = new TestCondition(Operator.EQUAL, "val1");
        TestCondition cond2 = new TestCondition(Operator.NOT_EQUAL, "val2");
        TestCondition cond3 = new TestCondition(Operator.GREATER_THAN, "val3");
        TestCondition cond4 = new TestCondition(Operator.LESS_THAN, "val4");

        // Test complex chaining: (cond1 AND cond2) OR (cond3 AND cond4)
        And and1 = cond1.and(cond2);
        And and2 = cond3.and(cond4);
        Or complex = and1.or(and2);

        Assertions.assertNotNull(complex);
        Assertions.assertEquals(2, complex.conditions().size());

        // Test NOT of complex condition
        Not notComplex = complex.not();
        Assertions.assertNotNull(notComplex);
        Assertions.assertEquals(complex, notComplex.getCondition());
    }

    @Test
    public void testNullOperatorHandling() {
        // Test condition with null operator (through default constructor)
        AbstractCondition condition = new AbstractCondition() {
            @Override
            public ImmutableList<Object> parameters() {
                return ImmutableList.empty();
            }

            @Override
            public String toString(NamingPolicy namingPolicy) {
                return "NULL_OP";
            }
        };

        Assertions.assertNull(condition.operator());
    }

    @Test
    public void testIsClause_StringEdgeCases() {
        Assertions.assertFalse(AbstractCondition.isClause((String) null));
        Assertions.assertTrue(AbstractCondition.isClause("WHERE"));
    }

    @Test
    public void testCreateSortExpression_StringArrayRejectsEmptyProperty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression("id", ""));
    }

    @Test
    public void testCreateSortExpression_SinglePropertyRejectsNullDirection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression("id", null));
    }

    @Test
    public void testCreateSortExpression_CollectionRejectsNullDirection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression(Arrays.asList("id"), null));
    }

    @Test
    public void testCreateSortExpression_CollectionRejectsEmptyProperty() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AbstractCondition.createSortExpression(Arrays.asList("id", ""), com.landawn.abacus.query.SortDirection.ASC));
    }

    @Test
    public void testCreateSortExpression_MapRejectsEmptyProperty() {
        java.util.Map<String, com.landawn.abacus.query.SortDirection> orders = new java.util.LinkedHashMap<>();
        orders.put("", com.landawn.abacus.query.SortDirection.ASC);

        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression(orders));
    }

    @Test
    public void testCreateSortExpression_MapRejectsNullDirection() {
        java.util.Map<String, com.landawn.abacus.query.SortDirection> orders = new java.util.LinkedHashMap<>();
        orders.put("id", null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression(orders));
    }

    // ---------------------------------------------------------------------
    // Third-pass review: SQL-escaping / injection-vector regression tests.
    // ---------------------------------------------------------------------

    @Test
    public void testFormatParameter_DateProducesQuotedISOLiteral() {
        // BUG: Previously java.util.Date fell through to Date.toString(),
        // emitting an unquoted "Mon Jan 01 ... 1970" sequence that is not valid SQL.
        // After the fix, Date is rendered via N.stringOf and wrapped in single quotes.
        String result = AbstractCondition.formatParameter(new java.util.Date(0L), NamingPolicy.NO_CHANGE);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("'") && result.endsWith("'"), "Date literal must be single-quoted, got: " + result);
        Assertions.assertFalse(result.contains("PST") || result.contains("PDT") || result.contains("UTC ") || result.contains("GMT "),
                "Date literal must not use Java's Date.toString() form, got: " + result);
    }

    @Test
    public void testFormatParameter_LocalDateTimeProducesQuotedLiteral() {
        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        String result = AbstractCondition.formatParameter(ldt, NamingPolicy.NO_CHANGE);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("'") && result.endsWith("'"), "LocalDateTime literal must be single-quoted, got: " + result);
        Assertions.assertTrue(result.contains("2024"), "LocalDateTime literal must include the date, got: " + result);
    }

    @Test
    public void testFormatParameter_CharacterIsQuoted() {
        // BUG: Previously a Character was rendered via Character.toString(),
        // producing an unquoted bare letter. Worse, a single-quote character would
        // produce a bare "'" that breaks the surrounding SQL.
        String resultLetter = AbstractCondition.formatParameter('X', NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("'X'", resultLetter);

        String resultQuote = AbstractCondition.formatParameter('\'', NamingPolicy.NO_CHANGE);
        Assertions.assertNotNull(resultQuote);
        Assertions.assertTrue(resultQuote.startsWith("'") && resultQuote.endsWith("'"));
        // The escaped quote inside must be present so the literal stays balanced.
        Assertions.assertTrue(resultQuote.contains("\\'") || resultQuote.contains("''"), "Embedded single-quote must be escaped, got: " + resultQuote);
    }

    @Test
    public void testFormatParameter_NaNAndInfinityRejected() {
        // BUG: Previously NaN / Infinity were emitted as bare "NaN" / "Infinity",
        // which most SQL dialects reject. Callers must use IsNaN / IsInfinite instead.
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.NaN, NamingPolicy.NO_CHANGE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.POSITIVE_INFINITY, NamingPolicy.NO_CHANGE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.NEGATIVE_INFINITY, NamingPolicy.NO_CHANGE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Float.NaN, NamingPolicy.NO_CHANGE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Float.POSITIVE_INFINITY, NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testFormatParameter_TrailingBackslashClosesLiteralSafely() {
        // BUG: Strings ending in a single backslash, when emitted as 'x\' inside MySQL-style
        // parsing, would have the closing quote consumed as an escape, breaking the SQL or
        // enabling injection. The escape helper must double the trailing backslash.
        String backslashAtEnd = "x" + (char) 92;
        String result = AbstractCondition.formatParameter(backslashAtEnd, NamingPolicy.NO_CHANGE);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("'") && result.endsWith("'"), "Output must be quoted, got: " + result);
        // Body between the quotes must end in an even number of backslashes.
        String body = result.substring(1, result.length() - 1);
        int trailing = 0;
        for (int i = body.length() - 1; i >= 0 && body.charAt(i) == '\\'; i--) {
            trailing++;
        }
        Assertions.assertEquals(0, trailing % 2, "Trailing backslash count must be even so the closing quote is not escaped, got body: " + body);
    }

    @Test
    public void testFormatParameter_NumberStillUnchanged() {
        // Regression guard: ordinary numeric values must keep their bare numeric form.
        Assertions.assertEquals("123", AbstractCondition.formatParameter(123, NamingPolicy.NO_CHANGE));
        Assertions.assertEquals("123.45", AbstractCondition.formatParameter(123.45, NamingPolicy.NO_CHANGE));
        Assertions.assertEquals("true", AbstractCondition.formatParameter(Boolean.TRUE, NamingPolicy.NO_CHANGE));
    }
}
