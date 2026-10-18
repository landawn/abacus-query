package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

public class InTest extends TestBase {

    // Characterisation (R16-02): an all-scalar membership list exposes the membership values themselves as the
    // memoized, unmodifiable parameter list; a Date element still forces a fresh defensive copy on every call.
    @Test
    void testAllScalarParametersAreMemoizedUnmodifiableAndEqualToValues() {
        final In condition = new In("id", Arrays.asList(1, 2, 3));

        final List<Object> parameters = condition.parameters();
        assertEquals(Arrays.asList(1, 2, 3), parameters);
        assertEquals(condition.values(), parameters);
        assertSame(parameters, condition.parameters());
        assertThrows(UnsupportedOperationException.class, () -> parameters.add(4));
        assertThrows(UnsupportedOperationException.class, () -> parameters.set(0, 9));
        assertEquals(Arrays.asList(1, 2, 3), condition.parameters());
        assertEquals("id IN (1, 2, 3)", condition.toString());

        final java.util.Date date = new java.util.Date(1_000L);
        final In dated = new In("createdOn", Arrays.asList(date, 2));
        assertNotSame(dated.parameters(), dated.parameters());
        assertNotSame(date, dated.parameters().get(0));
        assertNotSame(dated.parameters().get(0), dated.parameters().get(0));
        ((java.util.Date) dated.parameters().get(0)).setTime(4_000L);
        assertEquals(1_000L, ((java.util.Date) dated.parameters().get(0)).getTime());
    }

    @Test
    void testAnd() {
        In cond1 = new In("status", Arrays.asList("active", "pending"));
        In cond2 = new In("type", Arrays.asList("premium", "gold"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    // Regression: In snapshots array elements at construction (like Binary.IN), so mutating the caller's
    // array afterwards changes neither toString nor hashCode, and values() exposes a defensive copy.
    @Test
    void testArrayElementIsSnapshottedAtConstruction() {
        final int[] member = { 1, 2 };
        final In condition = new In("payload", Arrays.asList(member));
        final String sqlBeforeMutation = condition.toString();
        final int hashBeforeMutation = condition.hashCode();

        member[0] = 9;

        assertEquals("payload IN ('[1, 2]')", sqlBeforeMutation);
        assertEquals(sqlBeforeMutation, condition.toString());
        assertEquals(hashBeforeMutation, condition.hashCode());
        assertEquals(new In("payload", Arrays.asList(new int[] { 1, 2 })), condition);

        final int[] exposed = (int[]) condition.values().get(0);
        assertNotSame(member, exposed);
        assertNotSame(exposed, condition.values().get(0));
        assertTrue(Arrays.equals(new int[] { 1, 2 }, exposed));
        assertNotSame(condition.parameters().get(0), condition.values().get(0));
    }

    @Test
    void testConstructor_BlankPropName() {
        assertThrows(IllegalArgumentException.class, () -> new In("   ", Arrays.asList("active")));
    }

    @Test
    void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        In condition = new In("grade", values);

        assertEquals(3, condition.values().size());
        assertNotSame(values, condition.values());
    }

    @Test
    void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", Arrays.asList()));
    }

    @Test
    void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", null));
    }

    @Test
    void testConstructor_Set() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        assertEquals("user_id", condition.propName());
        assertEquals(5, condition.values().size());
    }

    @Test
    void testConstructor_SingleValue() {
        In condition = new In("category", Arrays.asList("electronics"));

        assertEquals("category", condition.propName());
        assertEquals(1, condition.values().size());
    }

    @Test
    void testConstructor_ValidList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        assertEquals("status", condition.propName());
        assertEquals(3, condition.values().size());
        assertEquals(Operator.IN, condition.operator());
    }

    @Test
    void testConstructorWithEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new In("field", new ArrayList<>());
        });
    }

    @Test
    void testConstructorWithList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        assertNotNull(condition);
        assertEquals("status", condition.propName());
        assertEquals(Operator.IN, condition.operator());
        assertEquals(3, condition.values().size());
        assertTrue(condition.values().containsAll(values));
    }

    @Test
    void testConstructorWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        In condition = new In("mixed_field", values);

        assertNotNull(condition);
        assertEquals(4, condition.values().size());
    }

    @Test
    void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        assertNotNull(condition);
        assertEquals("user_id", condition.propName());
        assertEquals(5, condition.values().size());
        assertTrue(condition.values().containsAll(values));
    }

    @Test
    void testEquals() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, condition4);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("state", Arrays.asList("active"));

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentType() {
        In condition = new In("status", Arrays.asList("active"));
        String other = "not an In";
        assertNotEquals(condition, other);
    }

    @Test
    void testEquals_DifferentValues() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_EqualObjects() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        In condition = new In("status", Arrays.asList("active"));
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals(condition, condition);
    }

    // Regression (abacus-common 8.0.0 made N.equals shallow): two In conditions built from equal but
    // distinct int[]/byte[] elements must still be equal with equal hash codes (content equality).
    @Test
    void testEqualsAndHashCode_EqualArrayElementsUseContentEquality() {
        final In intLeft = new In("payload", Arrays.asList(new int[] { 1, 2 }, new int[] { 3 }));
        final In intRight = new In("payload", Arrays.asList(new int[] { 1, 2 }, new int[] { 3 }));
        assertEquals(intLeft, intRight);
        assertEquals(intLeft.hashCode(), intRight.hashCode());

        final In byteLeft = new In("payload", Arrays.asList(new byte[] { 1, 2 }));
        final In byteRight = new In("payload", Arrays.asList(new byte[] { 1, 2 }));
        assertEquals(byteLeft, byteRight);
        assertEquals(byteLeft.hashCode(), byteRight.hashCode());

        // Different content or a different array type is not equal.
        assertNotEquals(byteLeft, new In("payload", Arrays.asList(new byte[] { 1, 3 })));
        assertNotEquals(byteLeft, new In("payload", Arrays.asList(new int[] { 1, 2 })));
    }

    @Test
    void testGetPropName() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals("status", condition.propName());
    }

    @Test
    void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        In condition = new In("id", values);

        List<?> result = condition.values();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testHashCode() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        assertEquals(condition1.hashCode(), condition2.hashCode());
        assertNotEquals(condition1.hashCode(), condition3.hashCode());
        assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    void testHashCode_Different() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_Equal() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testLargeValuesList() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        In condition = new In("id", values);

        assertEquals(1000, condition.values().size());
        assertEquals(1000, condition.parameters().size());
    }

    @Test
    void testMultiColumn_DefensiveCopy() {
        List<List<Object>> tuples = new ArrayList<>();
        tuples.add(new ArrayList<>(Arrays.asList(1, 2)));
        In condition = new In(Arrays.asList("a", "b"), tuples);

        tuples.get(0).set(0, 99);
        tuples.clear();

        assertEquals(Arrays.asList(1, 2), condition.values().get(0));
    }

    @Test
    void testMultiColumn_EqualsAndHashCode() {
        In a = new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        In b = new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        In c = new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        assertNotEquals(a, c);

        // Single-column and multi-column are never equal even with overlapping names
        In single = new In("a", Arrays.asList(1, 2));
        assertNotEquals(single, a);
    }

    @Test
    void testMultiColumn_FromArrayRows() {
        In condition = new In(Arrays.asList("first_name", "last_name"), Arrays.asList(new Object[] { "John", "Doe" }, new Object[] { "Jane", "Roe" }));

        assertEquals(2, condition.values().size());
        assertEquals(Arrays.asList("John", "Doe"), condition.values().get(0));
        assertEquals(Arrays.asList("Jane", "Roe"), condition.values().get(1));
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), condition.parameters());
        assertEquals("(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testMultiColumn_FromBeanRows() {
        Account a1 = new Account().setFirstName("John").setLastName("Doe");
        Account a2 = new Account().setFirstName("Jane").setLastName("Roe");

        In condition = new In(Arrays.asList("firstName", "lastName"), Arrays.asList(a1, a2));

        assertEquals(2, condition.values().size());
        assertEquals(Arrays.asList("John", "Doe"), condition.values().get(0));
        assertEquals(Arrays.asList("Jane", "Roe"), condition.values().get(1));
        assertEquals("(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testMultiColumn_FromMapRows() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Roe");

        In condition = new In(Arrays.asList("firstName", "lastName"), Arrays.asList(row1, row2));

        assertEquals(2, condition.values().size());
        assertEquals(Arrays.asList("John", "Doe"), condition.values().get(0));
        assertEquals(Arrays.asList("Jane", "Roe"), condition.values().get(1));
        // Values are extracted in propNames order regardless of map iteration order.
        assertEquals("(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testMultiColumn_FromMapRows_RejectsMissingKey() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("firstName", "John");
        // "lastName" intentionally absent

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("firstName", "lastName"), Arrays.asList(row)));
        assertTrue(error.getMessage().contains("lastName"));
    }

    // --- Multi-column rows supplied as forms other than List ---

    @Test
    void testMultiColumn_FromSetRows() {
        // A Set is an Iterable/Collection; insertion order is preserved with LinkedHashSet.
        Set<String> row1 = new LinkedHashSet<>(Arrays.asList("John", "Doe"));
        In condition = new In(Arrays.asList("first_name", "last_name"), Arrays.asList(row1));

        assertEquals(1, condition.values().size());
        assertEquals(Arrays.asList("John", "Doe"), condition.values().get(0));
        assertEquals("(first_name, last_name) IN (('John', 'Doe'))", condition.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testMultiColumn_MixedRowForms() {
        Map<String, Object> mapRow = N.asMap("firstName", "Jane", "lastName", "Roe");
        Account beanRow = new Account().setFirstName("Sam").setLastName("Poe");

        In condition = new In(Arrays.asList("firstName", "lastName"),
                Arrays.asList(Arrays.asList("John", "Doe"), new Object[] { "Max", "Coe" }, mapRow, beanRow));

        assertEquals(4, condition.values().size());
        assertEquals(Arrays.asList("John", "Doe", "Max", "Coe", "Jane", "Roe", "Sam", "Poe"), condition.parameters());
    }

    @Test
    void testMultiColumn_Parameters_FlattensRowMajor() {
        In condition = new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));

        assertEquals(Arrays.asList(1, 2, 3, 4), condition.parameters());
    }

    // --- Row value constructor IN ---

    @Test
    void testMultiColumn_PropNamesAndValues() {
        In condition = new In(Arrays.asList("first_name", "last_name"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));

        assertEquals(Arrays.asList("first_name", "last_name"), new ArrayList<>(condition.propNames()));
        assertEquals("first_name", condition.propName());
        assertEquals(2, condition.values().size());
        assertEquals(Operator.IN, condition.operator());
    }

    @Test
    void testMultiColumn_RejectsArrayWidthMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Arrays.asList(new Object[] { 1, 2 }, new Object[] { 3 })));
    }

    @Test
    void testMultiColumn_RejectsEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Collections.<List<?>> emptyList()));
    }

    @Test
    void testMultiColumn_RejectsNullOrBlankPropName() {
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", null), Arrays.asList(Arrays.asList(1, 2))));
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "  "), Arrays.asList(Arrays.asList(1, 2))));
    }

    @Test
    void testMultiColumn_RejectsNullRow() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), null)));

        // The rejected row is named by its index, like the other valueRows[r] messages.
        assertEquals("valueRows[1] must not be null", ex.getMessage());
    }

    @Test
    void testMultiColumn_RejectsPrimitiveArrayRow() {
        // A primitive int[] is not a supported row form (only Collection, Iterable, Object[], Map or bean).
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Arrays.asList(new int[] { 1, 2 })));
    }

    @Test
    void testMultiColumn_RejectsTupleSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3))));
    }

    @Test
    void testMultiColumn_RejectsUnsupportedRowType() {
        // A scalar (String) is not a valid row representation for a multi-column IN.
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Arrays.asList("John")));
    }

    @Test
    void testMultiColumn_ToString() {
        In condition = new In(Arrays.asList("first_name", "last_name"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));

        assertEquals("(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testMultiColumn_ToString_SnakeCase() {
        In condition = new In(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe")));

        assertEquals("(first_name, last_name) IN (('John', 'Doe'))", condition.toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testNot() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        In cond1 = new In("status", Arrays.asList("active"));
        In cond2 = new In("status", Arrays.asList("pending"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        List<Object> params = condition.parameters();
        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals("pending", params.get(1));
        assertEquals("approved", params.get(2));
    }

    @Test
    void testParameters_Integers() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        In condition = new In("id", values);

        List<Object> params = condition.parameters();
        assertEquals(3, params.size());
        assertEquals(Integer.valueOf(10), params.get(0));
    }

    @Test
    void testRejectsNestedPredicateValues() {
        Equal nested = Filters.equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> new In("id", Arrays.asList(nested, 2)));
    }

    @Test
    void testRejectsNullValues() {
        List<String> values = Arrays.asList("A", null, "B");
        assertThrows(IllegalArgumentException.class, () -> new In("field", values));
    }

    @Test
    void testRowValueConstructor_AllowsSinglePropName() {
        In condition = new In(Arrays.asList("a"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));

        assertEquals(Arrays.asList("a"), new ArrayList<>(condition.propNames()));
        assertEquals("a", condition.propName());
        assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2)), condition.values());
        assertEquals(Arrays.asList(1, 2), condition.parameters());
        assertEquals("(a) IN ((1), (2))", condition.toSql(NamingPolicy.NO_CHANGE));

        In scalar = new In("a", Arrays.asList(1, 2));
        assertNotEquals(scalar, condition);
        assertNotEquals(scalar.hashCode(), condition.hashCode());
    }

    @Test
    void testSingleValue() {
        In condition = new In("type", Collections.singletonList("SINGLE"));

        assertEquals(1, condition.values().size());
        assertEquals("SINGLE", condition.values().get(0));

        String result = condition.toString();
        assertTrue(result.contains("type IN ('SINGLE')"));
    }

    @Test
    void testToString() {
        In condition = new In("category", Arrays.asList("electronics", "computers", "phones"));
        String result = condition.toString();

        assertTrue(result.contains("category"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("("));
        assertTrue(result.contains(")"));
        assertTrue(result.contains("electronics"));
        assertTrue(result.contains("computers"));
        assertTrue(result.contains("phones"));
    }

    @Test
    void testToString_MultipleValues() {
        In condition = new In("id", Arrays.asList(1, 2, 3, 4, 5));
        String result = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("5"));
    }

    @Test
    void testToString_NoChange() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        String result = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("active"));
        assertTrue(result.contains("pending"));
    }

    @Test
    void testToString_SnakeCase() {
        In condition = new In("orderStatus", Arrays.asList("active"));
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("IN"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        In condition = new In("productId", Arrays.asList(101, 102, 103));
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("PRODUCT_ID"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("101"));
    }

    @Test
    void testUseCaseScenario_CategoryFilter() {
        // Filter by categories
        Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
        In categoryFilter = new In("category", categories);

        assertEquals(2, categoryFilter.parameters().size());
    }

    @Test
    void testUseCaseScenario_EnumValues() {
        // Filter by enum values
        List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
        In priorityFilter = new In("priority", priorities);

        String sql = priorityFilter.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("priority"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    void testUseCaseScenario_LargeList() {
        // Test with larger list
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        In condition = new In("id", largeList);

        assertEquals(10, condition.parameters().size());
    }

    @Test
    void testUseCaseScenario_StatusFilter() {
        // Check if status is one of several values
        List<String> statuses = Arrays.asList("active", "pending", "approved");
        In statusCheck = new In("status", statuses);

        assertEquals(3, statusCheck.parameters().size());
        assertTrue(statusCheck.toSql(NamingPolicy.NO_CHANGE).contains("IN"));
    }

    @Test
    void testUseCaseScenario_UserIdFilter() {
        // Check if user_id is in a list
        List<Integer> userIds = Arrays.asList(1, 2, 3, 5, 8);
        In userFilter = new In("user_id", userIds);

        assertEquals(5, userFilter.parameters().size());
    }

    @Nested
    class Validation {
        // Characterisation (R16-01): the positional element names in the rejection messages are unchanged now
        // that they are built only for a rejected element.
        @Test
        void testRejectedElementMessagesCarryPositionalIndex() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new In("id", Arrays.asList(1, null, 3)));
            assertEquals("values must not contain null elements; null found at index 1", ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class, () -> new In("id", Arrays.asList(1, 2, Filters.eq("a", 1))));
            assertTrue(ex.getMessage().startsWith("values[2] must be a literal value, SqlExpression, scalar SubQuery"), ex.getMessage());

            final Condition quantified = Filters.any(Filters.subQuery("t", Arrays.asList("id"), Filters.eq("x", 1)));
            ex = assertThrows(IllegalArgumentException.class, () -> new In("id", Arrays.asList(1, quantified)));
            assertEquals("values[1] must not be an ALL/ANY/SOME operand; quantified operands require a direct scalar-comparison RHS", ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class, () -> new In("id", Arrays.asList(SqlExpression.of("  "), 1)));
            assertEquals("values[0] must not be a blank SqlExpression", ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class,
                    () -> new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, null))));
            assertEquals("valueRows[1] must not contain null elements; null found at index 1", ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class,
                    () -> new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(Filters.eq("a", 1), 4))));
            assertTrue(ex.getMessage().startsWith("valueRows[1][0] must be a literal value, SqlExpression, scalar SubQuery"), ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, quantified))));
            assertEquals("valueRows[0][1] must not be an ALL/ANY/SOME operand; quantified operands require a direct scalar-comparison RHS", ex.getMessage());
        }

        @Test
        void testRowValueConstructionPropagatesGetterFailure() {
            final RuntimeException failure = assertThrows(RuntimeException.class, () -> new In(List.of("id"), List.of(new ThrowingRow())));
            Throwable cause = failure;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertEquals("row getter failed", cause.getMessage());
        }

        public static class ThrowingRow {
            public int getId() {
                throw new IllegalStateException("row getter failed");
            }

            public void setId(final int id) {
                // A writable bean property makes this a supported row type.
            }
        }
    }
}
