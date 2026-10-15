package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link OrderBy}.
 */
public class OrderByTest extends TestBase {

    @Test
    void testCommentOnlyPropertyNameRejected() {
        // A comment-only name would be stripped at render time, leaving "ORDER BY ASC" / "ORDER BY , name".
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("/* x */", SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("/* x */", "name"));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("name", "-- x"));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(Arrays.asList("name", "/* x */")));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(Arrays.asList("name", "# x"), SortDirection.DESC));

        final Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("name", SortDirection.ASC);
        orders.put("/* x */", SortDirection.DESC);
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(orders));

        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy("/* x */", SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().orderBy("/* x */", SortDirection.ASC, "name", SortDirection.DESC));
    }

    @Test
    void testComplexOrdering() {
        // Test complex ordering with mixed directions
        Map<String, SortDirection> complexOrder = new LinkedHashMap<>();
        complexOrder.put("status", SortDirection.DESC);
        complexOrder.put("priority", SortDirection.DESC);
        complexOrder.put("created_date", SortDirection.ASC);
        complexOrder.put("id", SortDirection.ASC);

        OrderBy orderBy = Filters.orderBy(complexOrder);

        String result = orderBy.toString();
        assertTrue(result.contains("status DESC"));
        assertTrue(result.contains("priority DESC"));
        assertTrue(result.contains("created_date ASC"));
        assertTrue(result.contains("id ASC"));
    }

    @Test
    void testConstructorRejectsBlankPropertyNames() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("   ", SortDirection.ASC);

        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy("   "));
        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy("   ", SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy(Arrays.asList("name", "   "), SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy(orders));
    }

    @Test
    void testConstructorWithCollectionAndDirection() {
        List<String> props = Arrays.asList("priority", "created");
        OrderBy orderBy = new OrderBy(props, SortDirection.DESC);
        String result = orderBy.toString();

        assertEquals("ORDER BY priority DESC, created DESC", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testConstructorWithCondition() {
        OrderBy orderBy = new OrderBy(Filters.expr("name ASC"));

        assertNotNull(orderBy);
        assertEquals(Operator.ORDER_BY, orderBy.operator());
    }

    @Test
    void testConstructorWithEmptyMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderBy(orders);
        });
    }

    @Test
    void testConstructorWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);

        OrderBy orderBy = new OrderBy(orders);
        String result = orderBy.toString();

        assertTrue(result.contains("priority"));
        assertTrue(result.contains("created"));
        assertTrue(result.contains("DESC"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    void testConstructorWithMultipleProperties() {
        OrderBy orderBy = new OrderBy("lastName", "firstName", "middleName");
        String result = orderBy.toString();

        assertTrue(result.contains("lastName"));
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("middleName"));
    }

    @Test
    void testConstructorWithPropertyAndAscDirection() {
        OrderBy orderBy = new OrderBy("created", SortDirection.ASC);
        String result = orderBy.toString();

        assertEquals("ORDER BY created ASC", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testConstructorWithPropertyAndDirection() {
        OrderBy orderBy = new OrderBy("salary", SortDirection.DESC);
        String result = orderBy.toString();

        assertEquals("ORDER BY salary DESC", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testConstructorWithSingleProperty() {
        OrderBy orderBy = new OrderBy("lastName");

        assertEquals("ORDER BY lastName", orderBy.toString());
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testConstructorWithVarArgs() {
        OrderBy orderBy = Filters.orderBy("country", "state", "city");

        assertEquals(Operator.ORDER_BY, orderBy.operator());
        String result = orderBy.toString();
        assertTrue(result.contains("country, state, city"));
    }

    @Test
    void testEmptyParameterList() {
        OrderBy orderBy = new OrderBy("col1", "col2");

        assertEquals(0, orderBy.parameters().size());
    }

    @Test
    void testEquals() {
        OrderBy orderBy1 = new OrderBy("name");
        OrderBy orderBy2 = new OrderBy("name");
        OrderBy orderBy3 = new OrderBy("age");

        assertEquals(orderBy1, orderBy2);
        assertNotEquals(orderBy1, orderBy3);
    }

    @Test
    void testEquals_DifferentClass() {
        OrderBy orderBy = new OrderBy("column");
        GroupBy groupBy = new GroupBy("column");
        assertNotEquals(orderBy, groupBy);
    }

    @Test
    void testEqualsWithDirection() {
        OrderBy orderBy1 = new OrderBy("price", SortDirection.DESC);
        OrderBy orderBy2 = new OrderBy("price", SortDirection.DESC);
        OrderBy orderBy3 = new OrderBy("price", SortDirection.ASC);

        assertEquals(orderBy1, orderBy2);
        assertNotEquals(orderBy1, orderBy3);
    }

    @Test
    void testGetCondition() {
        SqlExpression condition = Filters.expr("name ASC, age DESC");
        OrderBy orderBy = new OrderBy(condition);

        Condition retrieved = orderBy.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    void testHashCode() {
        OrderBy orderBy1 = new OrderBy("name");
        OrderBy orderBy2 = new OrderBy("name");

        assertEquals(orderBy1.hashCode(), orderBy2.hashCode());
    }

    @Test
    void testHashCode_DifferentDirections() {
        OrderBy orderBy1 = new OrderBy("name", SortDirection.ASC);
        OrderBy orderBy2 = new OrderBy("name", SortDirection.DESC);
        assertNotEquals(orderBy1.hashCode(), orderBy2.hashCode());
    }

    @Test
    void testHashCodeConsistency() {
        OrderBy orderBy = new OrderBy("column", SortDirection.DESC);
        int hash1 = orderBy.hashCode();
        int hash2 = orderBy.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    void testMapConstructor_nullOrEmptyMap_messageNamesOrders() {
        // twin of GroupByTest#testMapConstructor_nullOrEmptyMap_messageNamesGroupings: this path keeps its own
        // parameter name ('orders'), while the shared null-direction message names the offending key
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> new OrderBy((Map<String, SortDirection>) null));
        assertTrue(e1.getMessage().contains("orders"), e1.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> new OrderBy(new LinkedHashMap<>()));
        assertTrue(e2.getMessage().contains("orders"), e2.getMessage());

        Map<String, SortDirection> nullDirection = new LinkedHashMap<>();
        nullDirection.put("department", null);
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> new OrderBy(nullDirection));
        assertTrue(e3.getMessage().contains("department"), e3.getMessage());

        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);
        assertEquals("ORDER BY department ASC, salary DESC", new OrderBy(orders).toString());
    }

    @Test
    void testMixedDirections() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);
        orders.put("name", SortDirection.ASC);

        OrderBy orderBy = new OrderBy(orders);
        String result = orderBy.toString();

        assertEquals("ORDER BY priority DESC, created ASC, name ASC", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testMultipleColumnsOrdering() {
        OrderBy orderBy = new OrderBy("country", "state", "city", "street");
        String result = orderBy.toString();

        assertTrue(result.contains("country"));
        assertTrue(result.contains("state"));
        assertTrue(result.contains("city"));
        assertTrue(result.contains("street"));
    }

    @Test
    void testNullDirectionMessageDoesNotMentionSortMap() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Criteria.builder().orderBy("a", SortDirection.ASC, "b", null));
        assertTrue(e.getMessage().contains("'b'"), e.getMessage());
        assertFalse(e.getMessage().contains("sort map"), e.getMessage());
    }

    @Test
    void testOperatorType() {
        OrderBy orderBy = new OrderBy("column");

        assertEquals(Operator.ORDER_BY, orderBy.operator());
        assertNotEquals(Operator.GROUP_BY, orderBy.operator());
    }

    @Test
    void testParameters() {
        OrderBy orderBy = new OrderBy("column");

        assertNotNull(orderBy.parameters());
        assertTrue(orderBy.parameters().isEmpty());
    }

    @Test
    void testPracticalExamples() {
        // Simple ascending order (default)
        OrderBy orderBy1 = Filters.orderBy("lastName", "firstName");
        assertTrue(orderBy1.toString().contains("lastName, firstName"));

        // Descending order
        OrderBy orderBy2 = Filters.orderBy("salary", SortDirection.DESC);
        assertTrue(orderBy2.toString().contains("salary DESC"));

        // Multiple columns with same direction
        OrderBy orderBy3 = Filters.orderBy(Arrays.asList("created", "modified"), SortDirection.DESC);
        assertTrue(orderBy3.toString().contains("created DESC, modified DESC"));
    }

    @Test
    void testPropertyNameWithCommentAndRealTokenAccepted() {
        // Comments are stripped at render time, but the real token keeps the entry well-formed.
        assertEquals("ORDER BY name ASC", new OrderBy("name /* primary */", SortDirection.ASC).toString());
        assertEquals("ORDER BY name, age", new OrderBy("/* primary */ name", "age").toString());
        // A #name (possible SQL Server temporary identifier) is not treated as comment-only by the constructor.
        assertNotNull(new OrderBy("#tmp", SortDirection.ASC));
    }

    @Test
    void testSingleColumnAscending() {
        OrderBy orderBy = new OrderBy("id", SortDirection.ASC);
        String result = orderBy.toString();

        assertTrue(result.contains("id"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    void testSingleColumnDescending() {
        OrderBy orderBy = new OrderBy("timestamp", SortDirection.DESC);
        String result = orderBy.toString();

        assertTrue(result.contains("timestamp"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    void testSortKeyBlockCommentLeftOpenUnderAnyLexicalReadingIsRejected() {
        // Regression: the unclosed-"/*" check stopped at a stray quote and ended "--" comments only at '\n', so keys whose
        // "/*" the renderer still sees (after a lone '\r', or past a quote inside a "#" comment or a [..] identifier)
        // silently lost their direction and every later key ("ORDER BY a " instead of an error).
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a -- c\r/* x", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a -- it's\r/* x", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("[it's] /* x", "b"));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a # it's\n/* x", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("\"x\" # it's\n /* y", SortDirection.DESC));

        // The Map constructor validates every key the same way.
        final Map<String, SortDirection> single = new LinkedHashMap<>();
        single.put("a /* x", SortDirection.DESC);
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(single));

        final Map<String, SortDirection> loneCarriageReturn = new LinkedHashMap<>();
        loneCarriageReturn.put("a -- c\r/* x", SortDirection.DESC);
        loneCarriageReturn.put("b", SortDirection.DESC);
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new OrderBy(loneCarriageReturn));
        assertTrue(e.getMessage().contains("unterminated block comment"), e.getMessage());

        // Both string-literal readings count: with backslash escapes the "/*" below is outside the literal.
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("'a\\'/*'", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("E'\\'' /*", SortDirection.DESC));

        // A "/*" inside quoted text under every reading, or a closed comment, is accepted and keeps the direction.
        assertEquals("ORDER BY \"a/*b\" DESC", new OrderBy("\"a/*b\"", SortDirection.DESC).toString());
        assertEquals("ORDER BY `a/*b` DESC", new OrderBy("`a/*b`", SortDirection.DESC).toString());
        assertEquals("ORDER BY 'it''s /*' DESC", new OrderBy("'it''s /*'", SortDirection.DESC).toString());
        assertEquals("ORDER BY a DESC", new OrderBy("a -- c\r/* x */", SortDirection.DESC).toString());
        assertEquals("ORDER BY a DESC", new OrderBy("a -- c\n/* x */", SortDirection.DESC).toString());

        // Deliberate fail-closed trade-off: the check knows neither [..] identifiers nor "#" comments, so these keys, whose
        // "/*" one dialect would treat as inert, are rejected too (the builder's own orderBy still accepts "[a/*b]").
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("[a/*b]", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a # note /*", SortDirection.DESC));
        assertEquals("SELECT a FROM t ORDER BY [a/*b] DESC",
                com.landawn.abacus.query.Dsl.PSC.select("a").from("t").orderBy("[a/*b]", SortDirection.DESC).build().query());
    }

    // Regression: PostgreSQL, SQL Server and H2 nest block comments, so "a /* x /* y */" is still open there.
    @Test
    void testSortKeyNestedBlockCommentLeftOpenIsRejected() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new OrderBy("a /* x /* y */", SortDirection.DESC));
        assertTrue(e.getMessage().contains("unterminated block comment"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a /* x /* y */", "b"));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().orderBy("a /* x /* y */", SortDirection.DESC));
    }

    @Test
    void testToString() {
        OrderBy orderBy = new OrderBy("status");
        String result = orderBy.toString();

        assertEquals("ORDER BY status", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testToString_NoArgs() {
        OrderBy orderBy = new OrderBy("name", "age");
        String result = orderBy.toString();

        assertEquals("ORDER BY name, age", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testToStringWithNamingPolicy() {
        OrderBy orderBy = new OrderBy("userName");
        String result = orderBy.toSql(NamingPolicy.SNAKE_CASE);

        assertEquals("ORDER BY user_name", result);
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testWithCaseExpression() {
        SqlExpression expr = Filters.expr("CASE WHEN priority=1 THEN 0 ELSE 1 END");
        OrderBy orderBy = new OrderBy(expr);

        assertEquals("ORDER BY CASE WHEN priority=1 THEN 0 ELSE 1 END", orderBy.toString());
        assertEquals(List.of(), orderBy.parameters());
    }

    @Test
    void testWithExpression() {
        OrderBy orderBy = new OrderBy(Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END"));
        String result = orderBy.toString();

        assertNotNull(result);
        assertTrue(result.contains("CASE") || result.length() > 0);
    }

    @Nested
    class Validation {
        @Test
        void testSortMapValidationKeepsOneReadOfEachEntry() {
            final Map<String, SortDirection> source = new LinkedHashMap<>();
            source.put("firstName", SortDirection.ASC);
            source.put("lastName", SortDirection.DESC);
            final Map<String, SortDirection> oneReadMap = new AbstractMap<>() {
                private boolean read;

                @Override
                public int size() {
                    return source.size();
                }

                @Override
                public Set<Entry<String, SortDirection>> entrySet() {
                    if (read) {
                        throw new IllegalStateException("entrySet was read twice");
                    }
                    read = true;
                    return source.entrySet();
                }
            };

            assertEquals("ORDER BY firstName ASC, lastName DESC", new OrderBy(oneReadMap).toString());
        }
    }
}
