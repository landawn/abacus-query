package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertThrows(IllegalArgumentException.class, () -> new Equal("x", new Join("t")));
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
    public void testConstructorPreservesNonStructuralConditionValues() {
        final SubQuery subQuery = Filters.subQuery("SELECT score FROM results");
        final SqlExpression expression = Filters.expr("CURRENT_TIMESTAMP");
        final All quantified = new All(subQuery);
        final Exists booleanExpression = new Exists(Filters.subQuery("SELECT 1"));

        assertEquals(expression, new Equal("createdAt", expression).propValue());
        assertEquals(subQuery, new Equal("score", subQuery).propValue());
        assertEquals(quantified, new GreaterThan("score", quantified).propValue());
        assertEquals(booleanExpression, new Equal("flag", booleanExpression).propValue());
        assertEquals(IsNull.NULL, new Is("deletedAt", IsNull.NULL).propValue());
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
    public void testGetPropValue() {
        Binary condition = new Binary("age", Operator.GREATER_THAN, 30);
        Integer value = condition.propValue(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Binary condition = new Binary("name", Operator.EQUAL, "Alice");
        String value = condition.propValue(String.class);
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
        assertEquals(Integer.valueOf(30), condition.propValue(Integer.class));
        assertThrows(ClassCastException.class, () -> condition.propValue(String.class));
        assertThrows(IllegalArgumentException.class, () -> condition.propValue(null));
        assertNull(new Binary("age", Operator.EQUAL, null).propValue(Integer.class));
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
    public void testToStringWithNullValueAndNonNullOperator() {
        Binary binary = new Binary("col", Operator.GREATER_THAN, null);

        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("col"));
        Assertions.assertTrue(result.contains(">"));
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
     * Pass-3 regression: a String value that ends in a single backslash must not produce
     * {@code WHERE name = 'x\'} (where MySQL-style parsing would consume the closing quote
     * as an escape, breaking the SQL or enabling injection).
     */
    @Test
    public void testToString_TrailingBackslashKeepsLiteralBalanced_Pass3() {
        Binary binary = new Binary("name", Operator.EQUAL, "x" + (char) 92);
        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        // Locate the closing quote of the literal: count quotes to ensure exactly two
        // (the opening and closing of the string literal). A broken literal would have an
        // unbalanced backslash before the closing quote.
        int openQuote = result.indexOf('\'');
        int closeQuote = result.lastIndexOf('\'');
        assertTrue(openQuote >= 0 && closeQuote > openQuote, "Expected a literal pair in: " + result);

        // The body between the quotes must end in an even number of backslashes so the
        // closing quote is not consumed as an escape.
        String body = result.substring(openQuote + 1, closeQuote);
        int trailing = 0;
        for (int i = body.length() - 1; i >= 0 && body.charAt(i) == '\\'; i--) {
            trailing++;
        }
        assertEquals(0, trailing % 2, "Trailing backslash count must be even, got body: " + body);
    }

    /**
     * Pass-3 regression: a {@link Character} value must be quoted; in particular a single-quote
     * character must be escaped, otherwise it would terminate the surrounding literal.
     */
    @Test
    public void testToString_CharacterValueIsQuoted_Pass3() {
        Binary binary = new Binary("c", Operator.EQUAL, '\'');
        String result = binary.toSql(NamingPolicy.NO_CHANGE);

        // Body of the literal must contain an escaped quote (either \' or '').
        assertTrue(result.contains("\\'") || result.contains("''"), "Single-quote Character must be escaped, got: " + result);
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
}
