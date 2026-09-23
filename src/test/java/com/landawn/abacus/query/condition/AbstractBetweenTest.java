package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AbstractBetweenTest extends TestBase {

    private static final class TestAbstractBetween extends AbstractBetween {
        TestAbstractBetween(final String propName, final Object minValue, final Object maxValue) {
            super(propName, Operator.BETWEEN, minValue, maxValue);
        }

        TestAbstractBetween(final String propName, final Operator operator, final Object minValue, final Object maxValue) {
            super(propName, operator, minValue, maxValue);
        }
    }

    @Test
    public void testRejectsUnsupportedOperator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("age", Operator.EQUAL, 18, 65));
        // A null operator is an IllegalArgumentException (previously threw NullPointerException).
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("age", null, 18, 65));
    }

    @Test
    public void testRejectsQueryStructuralBounds() {
        final Where where = new Where(Filters.eq("y", 1));
        final Criteria criteria = Criteria.builder().where(Filters.eq("y", 1)).build();

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", where, 10));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", 1, criteria));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", new On("a", "b"), 10));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", 1, new OrderBy("y")));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", Filters.eq("y", 1), 10));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", 1, Filters.exists(Filters.subQuery("SELECT 1"))));
    }

    @Test
    public void testRejectsQuantifiedBounds() {
        final SubQuery subQuery = Filters.subQuery("SELECT score FROM results");

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", new All(subQuery), 10));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", 1, new Any(subQuery)));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("x", new Some(subQuery), 10));
    }

    private static final class EmptyAbstractBetween extends AbstractBetween {
        EmptyAbstractBetween() {
            super();
        }
    }

    @Test
    public void testGetPropName() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals("age", condition.propName());
    }

    @Test
    public void testGetMinValue() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Integer.valueOf(18), condition.minValue());
    }

    @Test
    public void testGetMaxValue() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Integer.valueOf(65), condition.maxValue());
    }

    // Verifies literal and nested-condition parameter expansion.
    @Test
    public void testParameters() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Arrays.asList(18, 65), condition.parameters());
    }

    @Test
    public void testParameters_ConditionValues() {
        final SubQuery minValue = Filters.subQuery("scores", Arrays.asList("min_score"), Filters.eq("status", "ACTIVE"));
        final SubQuery maxValue = Filters.subQuery("scores", Arrays.asList("max_score"), Filters.eq("status", "INACTIVE"));
        final TestAbstractBetween condition = new TestAbstractBetween("score", minValue, maxValue);

        final List<Object> parameters = condition.parameters();

        assertEquals(Arrays.asList("ACTIVE", "INACTIVE"), parameters);
    }

    @Test
    public void testToString() {
        final TestAbstractBetween condition = new TestAbstractBetween("userAge", 18, 65);

        final String sql = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("user_age"));
        assertTrue(sql.contains("BETWEEN"));
        assertTrue(sql.contains("18"));
        assertTrue(sql.contains("65"));
    }

    @Test
    public void testToString_NullNamingPolicy() {
        final TestAbstractBetween condition = new TestAbstractBetween("userAge", "A", "Z");

        final String sql = condition.toSql(null);

        assertTrue(sql.contains("userAge"));
        assertTrue(sql.contains("'A'"));
        assertTrue(sql.contains("'Z'"));
    }

    @Test
    public void testHashCode() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 65);

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    @Tag("2025")
    public void testHashCode_ArrayBoundsMatchEquals() {
        final TestAbstractBetween left = new TestAbstractBetween("payload", new byte[] { 1 }, new byte[] { 2 });
        final TestAbstractBetween right = new TestAbstractBetween("payload", new byte[] { 1 }, new byte[] { 2 });

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    @Tag("2025")
    public void testMutableArrayBoundsAreSnapshottedAndDefensivelyExposed() {
        final byte[] min = { 1 };
        final byte[] max = { 2 };
        final TestAbstractBetween condition = new TestAbstractBetween("payload", min, max);
        final int hash = condition.hashCode();

        min[0] = 3;
        max[0] = 4;
        ((byte[]) condition.minValue())[0] = 5;
        ((byte[]) condition.maxValue())[0] = 6;
        ((byte[]) condition.parameters().get(0))[0] = 7;
        ((byte[]) condition.parameters().get(1))[0] = 8;

        final TestAbstractBetween originalSnapshot = new TestAbstractBetween("payload", new byte[] { 1 }, new byte[] { 2 });
        assertEquals(originalSnapshot, condition);
        assertEquals(hash, condition.hashCode());
        assertEquals(1, ((byte[]) condition.minValue())[0]);
        assertEquals(2, ((byte[]) condition.maxValue())[0]);
        assertEquals(1, ((byte[]) condition.parameters().get(0))[0]);
        assertEquals(2, ((byte[]) condition.parameters().get(1))[0]);
    }

    @Test
    public void testHashCode_DifferentValues() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 21, 65);

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 65);

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentValues() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 70);

        assertNotEquals(left, right);
    }

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        final EmptyAbstractBetween left = new EmptyAbstractBetween();
        final EmptyAbstractBetween right = new EmptyAbstractBetween();

        assertNull(left.propName());
        assertNull(left.minValue());
        assertNull(left.maxValue());
        assertEquals(Arrays.asList(null, null), left.parameters());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testToString_WithSubQueryBounds_Batch2() {
        final TestAbstractBetween condition = new TestAbstractBetween("score", Filters.subQuery("SELECT MIN(score) FROM results"),
                Filters.subQuery("SELECT MAX(score) FROM results"));

        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("score BETWEEN"));
        assertTrue(sql.contains("(SELECT MIN(score) FROM results)"));
        assertTrue(sql.contains("(SELECT MAX(score) FROM results)"));
    }

    @Test
    public void testConstructorRejectsNullBounds() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("score", null, 100));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("score", 0, null));
    }

    @Test
    public void testMutableDateBoundsAreSnapshottedAndDefensivelyExposed() {
        final java.util.Date min = new java.util.Date(0L);
        final java.util.Date max = new java.util.Date(1_000L);
        final TestAbstractBetween condition = new TestAbstractBetween("createdAt", min, max);
        final int hash = condition.hashCode();

        min.setTime(2_000L);
        max.setTime(3_000L);
        ((java.util.Date) condition.minValue()).setTime(4_000L);
        ((java.util.Date) condition.parameters().get(1)).setTime(5_000L);

        assertEquals(new TestAbstractBetween("createdAt", new java.util.Date(0L), new java.util.Date(1_000L)), condition);
        assertEquals(hash, condition.hashCode());
        assertEquals(0L, ((java.util.Date) condition.minValue()).getTime());
        assertEquals(1_000L, ((java.util.Date) condition.parameters().get(1)).getTime());
    }

    @Test
    public void testDefaultConstructorToString() {
        final EmptyAbstractBetween condition = new EmptyAbstractBetween();

        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);

        assertNotNull(sql);
    }

    /**
     * Pass-3 regression: BETWEEN with {@link java.util.Date} bounds must render them as
     * quoted SQL date literals, not Java's {@code Date.toString()} form.
     */
    @Test
    public void testToString_DateBoundsAreQuoted_Pass3() {
        final TestAbstractBetween condition = new TestAbstractBetween("orderDate", new java.util.Date(0L), new java.util.Date(86400000L));
        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("'"), "Date bounds must be quoted, got: " + sql);
        assertTrue(!sql.contains("Wed Dec") && !sql.contains("Thu Jan") && !sql.contains("PST"),
                "Output must not contain Java's Date.toString() form, got: " + sql);
    }

    /**
     * Pass-3 regression: NaN / Infinity must be rejected from BETWEEN bounds rather than
     * silently emitted as bare {@code NaN} / {@code Infinity} tokens.
     */
    @Test
    public void testToString_NaNBoundIsRejected_Pass3() {
        final TestAbstractBetween condition = new TestAbstractBetween("v", Double.NaN, 100.0);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> condition.toSql(NamingPolicy.NO_CHANGE));
    }

    /**
     * Array bounds compare by content (N.deepEquals), consistent with the N.deepHashCode-based hash. Each
     * constructor snapshots its bounds, so even the same array instance reused across two conditions must
     * still compare equal. Fails on a shallow, identity-based equals.
     */
    @Test
    @Tag("2025")
    public void testEquals_SameArrayInstanceReusedAcrossConditions() {
        final byte[] shared = { 1 };
        final TestAbstractBetween a = new TestAbstractBetween("p", shared, shared);
        final TestAbstractBetween b = new TestAbstractBetween("p", shared, shared);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(new TestAbstractBetween("p", new byte[] { 1 }, new byte[] { 2 }), new TestAbstractBetween("p", new byte[] { 1 }, new byte[] { 3 }));

        final TestAbstractBetween nested1 = new TestAbstractBetween("p", new Object[] { new int[] { 1 } }, new Object[] { new int[] { 2 } });
        final TestAbstractBetween nested2 = new TestAbstractBetween("p", new Object[] { new int[] { 1 } }, new Object[] { new int[] { 2 } });
        assertEquals(nested1, nested2);
        assertEquals(nested1.hashCode(), nested2.hashCode());
    }

    /**
     * A blank SqlExpression bound would render a truncated {@code p BETWEEN  AND 5}; it is rejected at construction.
     */
    @Test
    @Tag("2025")
    public void testBlankSqlExpressionBoundIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("p", Filters.expr(""), 5));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractBetween("p", 1, Filters.expr("   ")));
        assertThrows(IllegalArgumentException.class, () -> Filters.between("p", Filters.expr(" "), 5));

        assertEquals("p BETWEEN CURRENT_DATE AND 5", new TestAbstractBetween("p", Filters.expr("CURRENT_DATE"), 5).toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    @Tag("2025")
    public void testToSqlPreservesLeadingAndTrailingUnderscoreRuns() {
        // AbstractBetween.toSql must convert the property name through QueryUtil.convertIdentifier
        // so leading/trailing '_' runs survive instead of being stripped by NamingPolicy.
        assertEquals("_id BETWEEN 1 AND 2", Filters.between("_id", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name_ NOT BETWEEN 1 AND 2", Filters.notBetween("firstName_", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ BETWEEN 1 AND 2", Filters.between("_", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_1 BETWEEN 1 AND 2", Filters.between("_1", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.__v BETWEEN 1 AND 2", Filters.between("t.__v", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ID BETWEEN 1 AND 2", Filters.between("_id", 1, 2).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("FIRST_NAME_ NOT BETWEEN 1 AND 2", Filters.notBetween("firstName_", 1, 2).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("T.__V BETWEEN 1 AND 2", Filters.between("t.__v", 1, 2).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("__v BETWEEN 1 AND 2", new TestAbstractBetween("__v", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testParametersOfSubQueryBoundAreNotSharedAcrossCalls() {
        // A sub-query bound splices the inner Binary's per-call array copies into parameters(); the outer condition
        // must not memoize them, otherwise a mutation through the outer parameters() would leak into later calls.
        final SubQuery subQuery = Filters.subQuery("config", Arrays.asList("minAge"), Filters.eq("blob", new byte[] { 1, 2 }));
        final TestAbstractBetween lower = new TestAbstractBetween("age", subQuery, 65);

        final byte[] first = (byte[]) lower.parameters().get(0);
        assertEquals(2, first[1]);
        first[1] = 9;
        assertEquals(2, ((byte[]) lower.parameters().get(0))[1]);
        assertEquals(65, lower.parameters().get(1));
        assertNotSame(lower.parameters(), lower.parameters());

        // Same guarantee when the sub-query is the upper bound.
        final TestAbstractBetween upper = new TestAbstractBetween("age", 18, subQuery);
        ((byte[]) upper.parameters().get(1))[0] = 7;
        assertEquals(1, ((byte[]) upper.parameters().get(1))[0]);
        assertEquals(18, upper.parameters().get(0));

        // All-scalar bounds keep the O(1) memoized instance.
        final TestAbstractBetween scalars = new TestAbstractBetween("age", 18, 65);
        assertSame(scalars.parameters(), scalars.parameters());
    }
}
