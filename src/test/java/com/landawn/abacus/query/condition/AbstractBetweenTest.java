package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new TestAbstractBetween("age", null, 18, 65));
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
}
