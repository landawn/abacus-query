package com.landawn.abacus.query.condition;

import static com.landawn.abacus.query.Dsl.PSC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class BinaryTest extends TestBase {
    @Test
    public void testConstructor() {
        Binary condition = new Binary("age", Operator.EQUAL, 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary(null, Operator.EQUAL, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary("", Operator.EQUAL, 25));
    }

    @Test
    public void testConstructorRejectsQueryStructuralValueConditions() {
        final Where where = new Where(Filters.eq("y", 1));
        final Criteria criteria = Criteria.builder().where(Filters.eq("y", 1)).build();

        assertThrows(IllegalArgumentException.class, () -> new Equal("x", where));
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", criteria));
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", new Join("t", Filters.expr("1 = 1"))));
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", new On("a", "b")));
        assertThrows(IllegalArgumentException.class, () -> new Binary("x", Operator.IN, new OrderBy("y")));
        assertThrows(IllegalArgumentException.class, () -> new Binary("x", Operator.IN, Arrays.asList(1, criteria)));
        assertThrows(IllegalArgumentException.class, () -> new Binary("x", Operator.NOT_IN, new Object[] { 1, new Using("id") }));

        final ComposableCell wrappedClause = new ComposableCell(Operator.NOT, where) {
            // Verifies recursive structural validation for custom wrappers.
        };
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", wrappedClause));
    }

    @Test
    public void testConstructorAcceptsOnlyExplicitScalarConditionValues() {
        final SubQuery subQuery = Filters.subQuery("SELECT score FROM results");
        final SqlExpression expression = Filters.expr("CURRENT_TIMESTAMP");
        final All quantified = new All(subQuery);
        final Exists booleanExpression = new Exists(Filters.subQuery("SELECT 1"));

        assertEquals(expression, new Equal("createdAt", expression).propValue());
        assertEquals(subQuery, new Equal("score", subQuery).propValue());
        assertEquals(quantified, new GreaterThan("score", quantified).propValue());
        assertEquals(IsNull.NULL, new Is("deletedAt", IsNull.NULL).propValue());

        assertThrows(IllegalArgumentException.class, () -> new Equal("flag", booleanExpression));
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("x", Filters.eq("y", 1)));
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", Filters.and(Filters.eq("y", 1), Filters.eq("z", 2))));
    }

    @Test
    public void testQuantifiedOperandsRequireDirectCompatibleComparisonRhs() {
        final SubQuery subQuery = Filters.subQuery("SELECT score FROM results");
        final Any any = new Any(subQuery);

        final Operator[] compatibleOperators = { Operator.EQUAL, Operator.NOT_EQUAL, Operator.NOT_EQUAL_ANSI, Operator.GREATER_THAN,
                Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL };

        for (final Operator operator : compatibleOperators) {
            assertEquals(any, new Binary("score", operator, any).propValue());
        }

        assertEquals(new All(subQuery), new GreaterThan("score", new All(subQuery)).propValue());
        assertEquals(new Some(subQuery), new LessThan("score", new Some(subQuery)).propValue());

        final Operator[] incompatibleOperators = { Operator.LIKE, Operator.NOT_LIKE, Operator.IS, Operator.IS_NOT, Operator.IN, Operator.NOT_IN };

        for (final Operator operator : incompatibleOperators) {
            assertThrows(IllegalArgumentException.class, () -> new Binary("score", operator, any));
        }

        assertThrows(IllegalArgumentException.class, () -> new Binary("score", Operator.IN, Arrays.asList(1, new All(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new Binary("score", Operator.NOT_IN, new Object[] { 1, new Some(subQuery) }));

        final ComposableCell wrappedQuantifier = new ComposableCell(Operator.NOT, new All(subQuery)) {
            // A quantified operand is valid only when it is the direct comparison RHS.
        };
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("score", wrappedQuantifier));
    }

    @Test
    public void testGetPropName() {
        Binary condition = new Binary("userName", Operator.LIKE, "%test%");
        assertEquals("userName", condition.propName());
    }

    @Test
    public void testPropValueAs() {
        Binary condition = new Binary("age", Operator.GREATER_THAN, 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testPropValueAs_String() {
        Binary condition = new Binary("name", Operator.EQUAL, "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Binary condition = new Binary("field", Operator.IS, null);
        assertNull(condition.propValue());
    }

    @Test
    public void testSafeValueAccessors() {
        Binary condition = new Binary("age", Operator.EQUAL, 30);
        assertEquals(30, condition.propValue());
        assertEquals(Integer.valueOf(30), condition.propValueAs(Integer.class));
        assertThrows(ClassCastException.class, () -> condition.propValueAs(String.class));
        assertThrows(IllegalArgumentException.class, () -> condition.propValueAs(null));
        assertNull(new Binary("age", Operator.EQUAL, null).propValueAs(Integer.class));
    }

    @Test
    public void testGetOperator() {
        Binary condition = new Binary("field", Operator.NOT_EQUAL, "value");
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    public void testParameters() {
        Binary condition = new Binary("status", Operator.EQUAL, "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testParameters_WithCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Binary condition = new Binary("userId", Operator.IN, subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
    }

    @Test
    public void testToString_NoChange() {
        Binary condition = new Binary("userName", Operator.EQUAL, "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    public void testToString_SnakeCase() {
        Binary condition = new Binary("userName", Operator.GREATER_THAN, "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testToStringWithNullAndEqualOperator() {
        Binary condition = new Binary("deletedAt", Operator.EQUAL, null);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("deletedAt IS NULL", result);
    }

    @Test
    public void testToStringWithNullAndNotEqualOperator() {
        Binary condition = new Binary("deletedAt", Operator.NOT_EQUAL, null);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("deletedAt IS NOT NULL", result);
    }

    @Test
    public void testToString_WithSubQueryAddsParentheses() {
        Binary condition = new Binary("userId", Operator.EQUAL, Filters.subQuery("SELECT id FROM users"));
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("= (SELECT id FROM users)"));
    }

    @Test
    public void testHashCode() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_ArrayValueMatchesEquals() {
        Binary cond1 = new Binary("payload", Operator.EQUAL, new byte[] { 1, 2 });
        Binary cond2 = new Binary("payload", Operator.EQUAL, new byte[] { 1, 2 });

        assertEquals(cond1, cond2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_InMembershipArrayUsesContentEquality() {
        Binary cond1 = new Binary("payload", Operator.IN, java.util.List.of(new byte[] { 1, 2 }));
        Binary cond2 = new Binary("payload", Operator.IN, java.util.List.of(new byte[] { 1, 2 }));

        assertEquals(cond1, cond2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_InMembershipArrayDifferentContent() {
        Binary cond1 = new Binary("payload", Operator.IN, java.util.List.of(new byte[] { 1, 2 }));
        Binary cond2 = new Binary("payload", Operator.IN, java.util.List.of(new byte[] { 9, 2 }));

        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testMutableArrayValueIsSnapshottedAndDefensivelyExposed() {
        final byte[] value = { 1, 2 };
        final Binary condition = new Binary("payload", Operator.EQUAL, value);
        final int hash = condition.hashCode();

        value[0] = 9;
        ((byte[]) condition.propValue())[1] = 9;
        ((byte[]) condition.parameters().get(0))[0] = 9;

        final Binary originalSnapshot = new Binary("payload", Operator.EQUAL, new byte[] { 1, 2 });
        assertEquals(originalSnapshot, condition);
        assertEquals(hash, condition.hashCode());
        assertEquals(1, ((byte[]) condition.propValue())[0]);
        assertEquals(2, ((byte[]) condition.parameters().get(0))[1]);
    }

    @Test
    public void testMutableDateValueIsSnapshottedAndDefensivelyExposed() {
        final java.util.Date value = new java.util.Date(1_000L);
        final Binary condition = new Binary("createdAt", Operator.EQUAL, value);
        final int hash = condition.hashCode();

        value.setTime(2_000L);
        ((java.util.Date) condition.propValue()).setTime(3_000L);
        ((java.util.Date) condition.parameters().get(0)).setTime(4_000L);

        assertEquals(new Binary("createdAt", Operator.EQUAL, new java.util.Date(1_000L)), condition);
        assertEquals(hash, condition.hashCode());
        assertEquals(1_000L, ((java.util.Date) condition.propValue()).getTime());
        assertEquals(1_000L, ((java.util.Date) condition.parameters().get(0)).getTime());
    }

    @Test
    public void testMutableCalendarValueIsSnapshottedAndDefensivelyExposed() {
        final java.util.Calendar value = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        value.setTimeInMillis(1_000L);
        final Binary condition = new Binary("createdAt", Operator.EQUAL, value);

        value.setTimeInMillis(2_000L);
        ((java.util.Calendar) condition.propValue()).setTimeInMillis(3_000L);
        ((java.util.Calendar) condition.parameters().get(0)).setTimeInMillis(4_000L);

        assertEquals(1_000L, ((java.util.Calendar) condition.propValue()).getTimeInMillis());
        assertEquals(1_000L, ((java.util.Calendar) condition.parameters().get(0)).getTimeInMillis());
    }

    @Test
    public void testApplicationDefinedMutableValueRetainsBindingIdentity() {
        final StringBuilder customValue = new StringBuilder("value");
        final Binary condition = new Binary("custom", Operator.EQUAL, customValue);

        assertSame(customValue, condition.propValue());
        assertSame(customValue, condition.parameters().get(0));
    }

    @Test
    public void testCyclicObjectArrayIsRejectedInsteadOfRecursingIndefinitely() {
        final Object[] cyclic = new Object[1];
        cyclic[0] = cyclic;

        assertThrows(IllegalArgumentException.class, () -> new Binary("payload", Operator.EQUAL, cyclic));
        assertThrows(IllegalArgumentException.class, () -> new Binary("payload", Operator.IN, Collections.singletonList(cyclic)));
    }

    @Test
    public void testHashCode_DifferentValues() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Binary cond1 = new Binary("status", Operator.EQUAL, "active");
        Binary cond2 = new Binary("status", Operator.EQUAL, "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Binary cond1 = new Binary("field1", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field2", Operator.EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentOperator() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field", Operator.NOT_EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value1");
        Binary cond2 = new Binary("field", Operator.EQUAL, "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testEquals_ExactClassNotSubtype() {
        // getClass()-based equals: a raw Binary is NOT equal to a typed subtype (Equal) with identical fields.
        Binary rawEqual = new Binary("a", Operator.EQUAL, 1);
        Equal equal = new Equal("a", 1);

        assertNotEquals(rawEqual, equal);
        assertNotEquals(equal, rawEqual);

        // Same-type equality is unaffected.
        assertEquals(equal, new Equal("a", 1));
    }

    @Test
    public void testAnd() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testOr() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testAndRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.and(Filters.where(Filters.equal("b", 2))));
    }

    @Test
    public void testOrRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.or(Filters.orderBy("b")));
    }

    @Test
    public void testXorRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.xor(Filters.limit(1)));
    }

    @Test
    public void testNot() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testDifferentOperators() {
        Binary eq = new Binary("a", Operator.EQUAL, 1);
        Binary gt = new Binary("b", Operator.GREATER_THAN, 2);
        Binary lt = new Binary("c", Operator.LESS_THAN, 3);
        Binary like = new Binary("d", Operator.LIKE, "%test%");

        assertEquals(Operator.EQUAL, eq.operator());
        assertEquals(Operator.GREATER_THAN, gt.operator());
        assertEquals(Operator.LESS_THAN, lt.operator());
        assertEquals(Operator.LIKE, like.operator());
    }

    @Test
    public void testConstructorWithNullValue() {
        Binary binary = Filters.binary("optional", Operator.EQUAL, null);

        Assertions.assertNotNull(binary);
        Assertions.assertNull(binary.propValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary("", Operator.EQUAL, "value");
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary(null, Operator.EQUAL, "value");
        });
    }

    @Test
    public void testParametersWithLiteralValue() {
        Binary binary = Filters.binary("score", Operator.LESS_THAN, 80.5);
        List<Object> params = binary.parameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(80.5, params.get(0));
    }

    @Test
    public void testParametersWithConditionValue() {
        SubQuery subQuery = Filters.subQuery("SELECT MAX(price) FROM products");
        Binary binary = Filters.binary("price", Operator.EQUAL, subQuery);

        List<Object> params = binary.parameters();
        Assertions.assertEquals(subQuery.parameters(), params);
    }

    @Test
    public void testToString() {
        Binary binary = Filters.binary("name", Operator.NOT_EQUAL, "Test");
        String result = binary.toString();

        Assertions.assertEquals("name != 'Test'", result);
    }

    @Test
    public void testToStringWithNumber() {
        Binary binary = Filters.binary("amount", Operator.GREATER_THAN, 1000);
        String result = binary.toString();

        Assertions.assertEquals("amount > 1000", result);
    }

    @Test
    public void testToStringWithNull() {
        Binary binary = Filters.binary("deleted", Operator.EQUAL, null);
        String result = binary.toString();

        Assertions.assertEquals("deleted IS NULL", result);
    }

    @Test
    public void testToStringWithNullAndNotEqual() {
        Binary binary = Filters.binary("deleted", Operator.NOT_EQUAL, null);
        String result = binary.toString();

        Assertions.assertEquals("deleted IS NOT NULL", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Binary binary = Filters.binary("firstName", Operator.LIKE, "John%");
        String result = binary.toSql(NamingPolicy.SNAKE_CASE);

        Assertions.assertEquals("first_name LIKE 'John%'", result);
    }

    @Test
    public void testToStringWithConditionValue() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Binary binary = Filters.binary("user_id", Operator.IN, subQuery);
        String result = binary.toString();

        Assertions.assertTrue(result.contains("user_id IN"));
        Assertions.assertTrue(result.contains("SELECT id FROM users WHERE active = true"));
    }

    @Test
    public void testHashCodeWithNull() {
        Binary binary1 = Filters.binary("optional", Operator.EQUAL, null);
        Binary binary2 = Filters.binary("optional", Operator.EQUAL, null);

        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testHashCodeArrayValueMatchesEquals() {
        Binary binary1 = Filters.binary("payload", Operator.EQUAL, new byte[] { 1, 2 });
        Binary binary2 = Filters.binary("payload", Operator.EQUAL, new byte[] { 1, 2 });

        Assertions.assertEquals(binary1, binary2);
        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testEquals() {
        Binary binary1 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 18);
        Binary binary2 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 18);
        Binary binary3 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 21);
        Binary binary4 = Filters.binary("age", Operator.GREATER_THAN, 18);
        Binary binary5 = Filters.binary("height", Operator.GREATER_THAN_OR_EQUAL, 18);

        Assertions.assertEquals(binary1, binary1);
        Assertions.assertEquals(binary1, binary2);
        Assertions.assertNotEquals(binary1, binary3); // Different value
        Assertions.assertNotEquals(binary1, binary4); // Different operator
        Assertions.assertNotEquals(binary1, binary5); // Different property
        Assertions.assertNotEquals(binary1, null);
        Assertions.assertNotEquals(binary1, "string");
    }

    @Test
    public void testAllOperators() {
        // Test with various operators
        Binary eq = Filters.binary("prop", Operator.EQUAL, 1);
        Binary ne = Filters.binary("prop", Operator.NOT_EQUAL, 1);
        Binary gt = Filters.binary("prop", Operator.GREATER_THAN, 1);
        Binary ge = Filters.binary("prop", Operator.GREATER_THAN_OR_EQUAL, 1);
        Binary lt = Filters.binary("prop", Operator.LESS_THAN, 1);
        Binary le = Filters.binary("prop", Operator.LESS_THAN_OR_EQUAL, 1);
        Binary like = Filters.binary("prop", Operator.LIKE, "%test%");
        Binary in = Filters.binary("prop", Operator.IN, Arrays.asList(1, 2, 3));

        Assertions.assertEquals("prop = 1", eq.toString());
        Assertions.assertEquals("prop != 1", ne.toString());
        Assertions.assertEquals("prop > 1", gt.toString());
        Assertions.assertEquals("prop >= 1", ge.toString());
        Assertions.assertEquals("prop < 1", lt.toString());
        Assertions.assertEquals("prop <= 1", le.toString());
        Assertions.assertEquals("prop LIKE '%test%'", like.toString());
        Assertions.assertTrue(in.toString().contains("prop IN"));
    }

    @Test
    public void testInCollectionValueRendersAsListAndFlattensParameters() {
        Binary in = Filters.binary("prop", Operator.IN, Arrays.asList(1, 2, 3));
        Binary notIn = Filters.binary("prop", Operator.NOT_IN, Arrays.asList("a", "b"));

        Assertions.assertEquals("prop IN (1, 2, 3)", in.toString());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), in.parameters());
        Assertions.assertEquals("prop NOT IN ('a', 'b')", notIn.toString());
        Assertions.assertEquals(Arrays.asList("a", "b"), notIn.parameters());
    }

    @Test
    public void testInCollectionValueDefensivelyCopied() {
        final List<Integer> values = new ArrayList<>(Arrays.asList(1));
        final Binary in = Filters.binary("id", Operator.IN, values);
        final int hash = in.hashCode();

        values.add(2);
        values.clear();

        assertEquals("id IN (1)", in.toString());
        assertEquals(Arrays.asList(1), in.parameters());
        assertEquals(hash, in.hashCode());
    }

    @Test
    public void testInCollectionSnapshotsMutableElementsAndDefensivelyExposesThem() {
        final byte[] member = { 1, 2 };
        final Binary in = Filters.binary("payload", Operator.IN, Arrays.asList(member));
        final int hash = in.hashCode();

        member[0] = 9;
        ((byte[]) ((List<?>) in.propValue()).get(0))[1] = 9;
        ((byte[]) in.parameters().get(0))[0] = 9;

        assertEquals(new Binary("payload", Operator.IN, Arrays.asList(new byte[] { 1, 2 })), in);
        assertEquals(hash, in.hashCode());
        assertEquals(1, ((byte[]) ((List<?>) in.propValue()).get(0))[0]);
        assertEquals(2, ((byte[]) in.parameters().get(0))[1]);
    }

    @Test
    public void testInCollectionValidatesTheDefensiveSnapshotIsNonEmpty() {
        final AbstractCollection<Integer> liveValues = new AbstractCollection<>() {
            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.IN, liveValues));
    }

    @Test
    public void testInArrayValueRendersAsListAndRejectsInvalidValues() {
        final Binary in = Filters.binary("id", Operator.IN, new int[] { 1, 2 });

        assertEquals("id IN (1, 2)", in.toString());
        assertEquals(Arrays.asList(1, 2), in.parameters());

        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.IN, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.IN, 1));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.NOT_IN, null));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.IN, Arrays.asList(1, null)));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.NOT_IN, new Object[] { 1, null }));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("id", Operator.IN, Arrays.asList(1, Filters.eq("x", 2))));
    }

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        Binary binary = new Binary();
        Binary same = new Binary();

        Assertions.assertNull(binary.propName());
        Assertions.assertNull(binary.propValue());
        Assertions.assertEquals(binary, same);
        Assertions.assertEquals(binary.hashCode(), same.hashCode());
    }

    @Test
    public void testToStringWithNullAndNotEqualAnsi_Batch2() {
        Binary binary = new Binary("deleted", Operator.NOT_EQUAL_ANSI, null);

        Assertions.assertEquals("deleted IS NOT NULL", binary.toSql(null));
    }

    @Test
    public void testEqualNullUsesIsNullWithoutParameters() {
        Equal condition = Filters.eq("deletedAt", null);

        assertEquals("deletedAt IS NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testNotEqualNullUsesIsNotNullWithoutParameters() {
        NotEqual condition = Filters.ne("deletedAt", null);

        assertEquals("deletedAt IS NOT NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testIsNullUsesKeywordFormWithoutParameters() {
        Is condition = Filters.is("deletedAt", null);

        assertEquals("deletedAt IS NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testIsNotNullUsesKeywordFormWithoutParameters() {
        IsNot condition = Filters.isNot("deletedAt", null);

        assertEquals("deletedAt IS NOT NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testCriteriaParametersSkipNullOnlyComparisons() {
        Criteria criteria = Criteria.builder()
                .where(Filters.and(Filters.eq("deletedAt", null), Filters.is("archivedAt", null), Filters.eq("status", "ACTIVE")))
                .build();

        List<Object> parameters = criteria.parameters();

        assertEquals(1, parameters.size());
        assertEquals("ACTIVE", parameters.get(0));
    }

    @Test
    public void testDefaultConstructorToString() {
        Binary binary = new Binary();

        Assertions.assertNotNull(binary.toString());
        Assertions.assertNotNull(binary.toSql(NamingPolicy.NO_CHANGE));
        Assertions.assertNotNull(binary.toSql(null));
    }

    @Test
    public void testDefaultConstructorParameters() {
        Binary binary = new Binary();

        Assertions.assertNotNull(binary.parameters());
        Assertions.assertTrue(binary.parameters().isEmpty());
    }

    @Test
    public void testNullIsRejectedForNonNullAwareOperators() {
        final Operator[] operators = { Operator.GREATER_THAN, Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL,
                Operator.LIKE, Operator.NOT_LIKE };

        for (final Operator operator : operators) {
            assertThrows(IllegalArgumentException.class, () -> new Binary("col", operator, null));
        }
    }

    @Test
    public void testIsOperatorsRestrictRightHandValues() {
        // Booleans are normalized to the SQL truth-value keywords (never bound as parameters).
        assertEquals("flag IS TRUE", new Binary("flag", Operator.IS, true).toString());
        assertEquals("flag IS NOT FALSE", new Binary("flag", Operator.IS_NOT, false).toString());
        assertEquals("flag IS UNKNOWN", new Binary("flag", Operator.IS, Filters.expr("UNKNOWN")).toString());

        assertThrows(IllegalArgumentException.class, () -> new Binary("flag", Operator.IS, 1));
        assertThrows(IllegalArgumentException.class, () -> new Binary("flag", Operator.IS_NOT, "UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> new Binary("flag", Operator.IS, Filters.eq("other", true)));
        assertThrows(IllegalArgumentException.class, () -> new Binary("flag", Operator.IS_NOT, Filters.subQuery("SELECT flag FROM t")));
    }

    /**
     * Second-pass locking test: when propValue is a SubQuery, parameters() must delegate
     * to the SubQuery's parameters() (not return the SubQuery as a single parameter).
     */
    @Test
    public void testParametersDelegatesToSubQueryPropValue() {
        SubQuery sub = new SubQuery("users", java.util.Arrays.asList("id"), Filters.eq("active", true));
        Binary binary = new Binary("userId", Operator.EQUAL, sub);

        List<Object> params = binary.parameters();

        // SubQuery's condition has one parameter (true). Binary must surface that, not the SubQuery itself.
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    /**
     * Operator.IN with a Condition propValue is supported: the subquery renders parenthesized
     * after IN, and parameters() delegates to the subquery's own parameters (mirrors
     * testParametersDelegatesToSubQueryPropValue, which covers EQUAL).
     */
    @Test
    public void testInOperatorWithSubQueryPropValue() {
        Binary rendered = Filters.binary("id", Operator.IN, Filters.subQuery("SELECT id FROM x"));

        assertEquals("id IN (SELECT id FROM x)", rendered.toSql(NamingPolicy.NO_CHANGE));

        SubQuery sub = new SubQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        Binary binary = Filters.binary("userId", Operator.IN, sub);

        List<Object> params = binary.parameters();

        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    /**
     * Second-pass locking test: when propValue is a SubQuery, toString() must wrap the
     * subquery in parentheses so the rendered SQL is valid.
     */
    @Test
    public void testToStringWrapsSubQueryPropValueInParens() {
        SubQuery sub = new SubQuery("users", java.util.Arrays.asList("id"), Filters.eq("active", true));
        Binary binary = new Binary("userId", Operator.EQUAL, sub);

        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        Assertions.assertTrue(result.contains("(SELECT"), "subquery must be wrapped in parens: " + result);
        Assertions.assertTrue(result.endsWith(")"), "subquery should end with closing paren: " + result);
    }

    // ---------------------------------------------------------------------
    // Third-pass review (SQL-escaping): Binary.toString() must produce safe SQL.
    // ---------------------------------------------------------------------

    /**
     * Pass-3 regression: a {@link java.util.Date} value used in a Binary condition must render
     * as a quoted SQL date literal, not Java's {@code Date.toString()} form.
     */
    @Test
    public void testToString_DateValueIsQuotedISOLiteral_Pass3() {
        Binary binary = new Binary("orderDate", Operator.EQUAL, new java.util.Date(0L));
        String result = binary.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("'"), "Date value must be wrapped in quotes, got: " + result);
        assertTrue(!result.contains("Wed Dec") && !result.contains("Thu Jan") && !result.contains("PST") && !result.contains("PDT"),
                "Output must not contain Java's Date.toString() form, got: " + result);
    }

    /**
     * Pass-3 regression: NaN / Infinity must not be silently emitted as a bare {@code NaN} /
     * {@code Infinity} token; callers should use {@link IsNaN} / {@link IsInfinite} instead.
     */
    @Test
    public void testToString_NaNValueIsRejected_Pass3() {
        Binary binary = new Binary("v", Operator.EQUAL, Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> binary.toSql(NamingPolicy.NO_CHANGE));

        Binary binary2 = new Binary("v", Operator.EQUAL, Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> binary2.toSql(NamingPolicy.NO_CHANGE));
    }

    /**
     * A SQL-standard string literal preserves a backslash rather than introducing a second,
     * value-changing backslash. Database modes with non-standard backslash escapes require a
     * dialect-aware renderer or, preferably, a bound parameter.
     */
    @Test
    public void testToString_TrailingBackslashIsPreserved_Pass3() {
        Binary binary = new Binary("name", Operator.EQUAL, "x" + (char) 92);
        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        assertEquals("name = 'x\\'", result);
    }

    /**
     * Pass-3 regression: a {@link Character} value must be quoted; in particular a single-quote
     * character must be escaped, otherwise it would terminate the surrounding literal.
     */
    @Test
    public void testToString_CharacterValueIsQuoted_Pass3() {
        Binary binary = new Binary("c", Operator.EQUAL, '\'');
        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        assertEquals("c = ''''", result);
    }

    /**
     * Rendering the same instance with alternating naming policies must always return the value for the
     * requested policy. (Originally guarded the since-removed single-slot toString cache; kept because the
     * per-policy rendering contract must hold regardless of any internal memoization.)
     */
    @Test
    public void testToStringReturnsValuePerNamingPolicy() {
        Binary binary = Filters.binary("firstName", Operator.LIKE, "John%");

        for (int i = 0; i < 100; i++) {
            assertEquals("firstName LIKE 'John%'", binary.toSql(NamingPolicy.NO_CHANGE));
            assertEquals("first_name LIKE 'John%'", binary.toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("FIRST_NAME LIKE 'John%'", binary.toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        }
    }

    /**
     * Array-valued conditions compare by content (N.deepEquals) and hash by content (N.deepHashCode), so
     * equals/hashCode stay consistent even for nested arrays and for the same array instance reused across
     * two conditions (each constructor takes its own snapshot). Fails on a shallow, identity-based equals.
     */
    @Test
    public void testEqualsAndHashCode_ArrayValuesUseDeepContentEquality() {
        final byte[] shared = { 1, 2 };
        final Binary fromShared1 = new Binary("payload", Operator.EQUAL, shared);
        final Binary fromShared2 = new Binary("payload", Operator.EQUAL, shared);
        assertEquals(fromShared1, fromShared2);
        assertEquals(fromShared1.hashCode(), fromShared2.hashCode());

        final Binary nested1 = new Binary("payload", Operator.EQUAL, new Object[] { new byte[] { 1 }, "x" });
        final Binary nested2 = new Binary("payload", Operator.EQUAL, new Object[] { new byte[] { 1 }, "x" });
        assertEquals(nested1, nested2);
        assertEquals(nested1.hashCode(), nested2.hashCode());
        assertNotEquals(nested1, new Binary("payload", Operator.EQUAL, new Object[] { new byte[] { 2 }, "x" }));

        final Binary in1 = new Binary("payload", Operator.IN, Arrays.asList(new byte[] { 1 }, new byte[] { 2 }));
        final Binary in2 = new Binary("payload", Operator.IN, Arrays.asList(new byte[] { 1 }, new byte[] { 2 }));
        assertEquals(in1, in2);
        assertEquals(in1.hashCode(), in2.hashCode());
        assertNotEquals(in1, new Binary("payload", Operator.IN, Arrays.asList(new byte[] { 1 }, new byte[] { 3 })));

        // Different array component types with the same numeric content are not equal.
        assertNotEquals(new Binary("payload", Operator.EQUAL, new byte[] { 1 }), new Binary("payload", Operator.EQUAL, new int[] { 1 }));
    }

    /**
     * A Boolean IS/IS NOT operand is normalized to the SQL truth-value keyword so that no rendering path
     * ever emits {@code x IS ?}; the result is indistinguishable from {@code Filters.isTrue/isFalse}.
     */
    @Test
    public void testIsBooleanOperandIsNormalizedToKeywordAndNeverBound() {
        final Binary isTrue = new Binary("active", Operator.IS, true);
        assertEquals("active IS TRUE", isTrue.toSql(NamingPolicy.NO_CHANGE));
        assertTrue(isTrue.parameters().isEmpty());
        assertEquals(SqlExpression.of("TRUE"), isTrue.propValue());

        // propValue() exposes the normalized keyword expression, not the original Boolean, so
        // propValueAs(Boolean.class) throws; propValueAs(SqlExpression.class) / propValue() are the accessors.
        final Is is = new Is("x", true);
        assertTrue(is.propValue() instanceof SqlExpression);
        assertEquals("TRUE", ((SqlExpression) is.propValue()).literal());
        assertEquals("TRUE", is.propValueAs(SqlExpression.class).literal());
        assertThrows(ClassCastException.class, () -> is.propValueAs(Boolean.class));
        assertEquals("FALSE", ((SqlExpression) new IsNot("x", false).propValue()).literal());
        assertThrows(ClassCastException.class, () -> new IsNot("x", false).propValueAs(Boolean.class));

        final Binary isNotFalse = Filters.binary("active", Operator.IS_NOT, false);
        assertEquals("active IS NOT FALSE", isNotFalse.toSql(NamingPolicy.NO_CHANGE));
        assertTrue(isNotFalse.parameters().isEmpty());

        assertEquals(Filters.isTrue("active"), Filters.is("active", true));
        assertEquals(Filters.isTrue("active").hashCode(), Filters.is("active", true).hashCode());
        assertEquals(Filters.isFalse("active"), Filters.is("active", false));
        assertNotEquals(Filters.is("active", true), Filters.is("active", false));

        // Booleans are still ordinary bind values for every other comparison operator.
        assertEquals(List.of(true), new Binary("active", Operator.EQUAL, true).parameters());
    }

    /**
     * A blank SqlExpression in value position would render a truncated comparison such as {@code a = };
     * it is rejected at construction with the shared blank-expression wording.
     */
    @Test
    public void testBlankSqlExpressionValueOperandIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Binary("a", Operator.EQUAL, Filters.expr("")));
        assertEquals("propValue must not be a blank SqlExpression", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new Equal("a", Filters.expr("   ")));
        assertThrows(IllegalArgumentException.class, () -> Filters.gt("a", Filters.expr(" ")));
        assertThrows(IllegalArgumentException.class, () -> new Binary("a", Operator.IS, Filters.expr("")));
        assertThrows(IllegalArgumentException.class, () -> new Binary("a", Operator.IN, Arrays.asList(1, Filters.expr(""))));
        assertThrows(IllegalArgumentException.class, () -> new Binary("a", Operator.IN, Filters.expr("  ")));

        // Non-blank expressions remain the supported escape hatch.
        assertEquals("a = CURRENT_DATE", new Binary("a", Operator.EQUAL, Filters.expr("CURRENT_DATE")).toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testToSqlPreservesLeadingAndTrailingUnderscoreRuns() {
        // Binary.toSql must convert the property name through QueryUtil.convertIdentifier (like SqlExpression
        // and the builders do) so leading/trailing '_' runs survive instead of being stripped by NamingPolicy.
        assertEquals("_id = 1", Filters.eq("_id", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id != 1", Filters.ne("_id", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name_ > 1", Filters.gt("firstName_", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("__v LIKE 'a%'", Filters.like("__v", "a%").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id IS NULL", Filters.isNull("_id").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id IS NOT NULL", Filters.isNotNull("_id").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id IS NULL", new Binary("_id", Operator.EQUAL, null).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id IS NOT NULL", new Binary("_id", Operator.NOT_EQUAL, null).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_id IN (1, 2)", new Binary("_id", Operator.IN, Arrays.asList(1, 2)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ = 1", Filters.eq("_", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("__ = 1", Filters.eq("__", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_1 = 1", Filters.eq("_1", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_1 IS NULL", Filters.isNull("_1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.__v = 1", Filters.eq("t.__v", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_first_name = 1", Filters.eq("_firstName", 1).toSql(NamingPolicy.SNAKE_CASE));

        assertEquals("_ID = 1", Filters.eq("_id", 1).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("FIRST_NAME_ > 1", Filters.gt("firstName_", 1).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("__V LIKE 'a%'", Filters.like("__v", "a%").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("_ID IS NULL", Filters.isNull("_id").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("_ = 1", Filters.eq("_", 1).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("_1 = 1", Filters.eq("_1", 1).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("T.__V = 1", Filters.eq("t.__v", 1).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));

        // ON conditions render through Binary as well
        assertEquals("ON _id = o._user_id", Filters.on("_id", "o._userId").toSql(NamingPolicy.SNAKE_CASE));

        // parity with the builder path (bind-free condition so the two strings compare directly)
        final Condition isNull = Filters.isNull("_id");
        assertEquals("SELECT * FROM t WHERE " + isNull.toSql(NamingPolicy.SNAKE_CASE), PSC.select("*").from("t").where(isNull).build().query());
    }

    @Test
    public void testParametersOfSubQueryOperandAreNotSharedAcrossCalls() {
        // `= (subquery)`: the outer Binary must not memoize the inner Binary's per-call array copies, otherwise a
        // mutation through the outer parameters() would leak into every later call.
        final SubQuery subQuery = Filters.subQuery("files", Arrays.asList("id"), Filters.eq("blob", new byte[] { 1, 2 }));
        final Binary eqSub = new Equal("fileId", subQuery);

        final byte[] first = (byte[]) eqSub.parameters().get(0);
        assertEquals(2, first[1]);
        first[1] = 9;
        assertEquals(2, ((byte[]) eqSub.parameters().get(0))[1]);
        assertNotSame(eqSub.parameters(), eqSub.parameters());

        // `= ANY (subquery)`: same guarantee through a quantified operand.
        final Binary eqAny = new Equal("fileId", Filters.any(subQuery));
        ((byte[]) eqAny.parameters().get(0))[0] = 7;
        assertEquals(1, ((byte[]) eqAny.parameters().get(0))[0]);
        assertNotSame(eqAny.parameters(), eqAny.parameters());

        // IN with a nested sub-query element rebuilds as well.
        final Binary inSub = new Binary("fileId", Operator.IN, Arrays.asList(subQuery, 3));
        ((byte[]) inSub.parameters().get(0))[0] = 7;
        assertEquals(1, ((byte[]) inSub.parameters().get(0))[0]);
        assertEquals(3, inSub.parameters().get(1));

        // All-scalar operands keep the O(1) memoized instance.
        final Binary scalar = new Equal("age", 25);
        assertSame(scalar.parameters(), scalar.parameters());
        final Binary scalarIn = new Binary("id", Operator.IN, Arrays.asList(1, 2));
        assertSame(scalarIn.parameters(), scalarIn.parameters());
    }

    @Test
    public void testScalarCollectionValueRetainsItsTypeAndBindingIdentity() {
        final java.util.Set<Integer> value = new java.util.LinkedHashSet<>(Arrays.asList(1, 2));
        final Equal condition = new Equal("payload", value);

        assertSame(value, condition.propValue());
        assertSame(value, condition.propValueAs(java.util.Set.class));
        assertSame(value, condition.parameters().get(0));
        assertSame(value, PSC.select("id").from("records").where(condition).build().parameters().get(0));

        final ArrayList<Integer> listValue = new ArrayList<>(Arrays.asList(1, 2));
        assertSame(listValue, new NotEqual("payload", listValue).propValueAs(ArrayList.class));
    }
}
