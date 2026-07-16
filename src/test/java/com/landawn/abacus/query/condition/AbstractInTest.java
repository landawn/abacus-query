package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AbstractInTest extends TestBase {

    private static final class TestAbstractIn extends AbstractIn {
        TestAbstractIn(final String propName, final Collection<?> values) {
            super(propName, Operator.IN, values);
        }

        TestAbstractIn(final String propName, final Operator operator, final Collection<?> values) {
            super(propName, operator, values);
        }
    }

    private static final class TestRowAbstractIn extends AbstractIn {
        TestRowAbstractIn(final Operator operator) {
            super(Arrays.asList("a", "b"), operator, Arrays.asList(Arrays.asList(1, 2)));
        }

        TestRowAbstractIn(final Collection<String> propNames, final Collection<?> valueRows) {
            super(propNames, Operator.IN, valueRows);
        }
    }

    private static final class NominallyNonEmptyCollection<E> extends AbstractCollection<E> {
        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @Test
    public void testRejectsUnsupportedOperator() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("status", Operator.EQUAL, Arrays.asList("A")));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(Operator.EQUAL));
        assertThrows(NullPointerException.class, () -> new TestAbstractIn("status", null, Arrays.asList("A")));
    }

    @Test
    public void testRejectsQueryStructuralValueElements() {
        final Criteria criteria = Criteria.builder().where(Filters.eq("y", 1)).build();

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("x", Arrays.asList(1, new Where(Filters.eq("y", 1)))));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("x", Arrays.asList(1, criteria)));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, new On("a", "b")))));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(new OrderBy("y"), 2))));
    }

    @Test
    public void testRejectsQuantifiedValueElements() {
        final SubQuery subQuery = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("x", Arrays.asList(1, new All(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("x", Arrays.asList(new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, new Some(subQuery)))));
    }

    @Test
    public void testConstructorsValidateDefensiveSnapshotsAreNonEmpty() {
        final NominallyNonEmptyCollection<Object> emptySnapshot = new NominallyNonEmptyCollection<>();

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("status", emptySnapshot));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(new NominallyNonEmptyCollection<>(), Arrays.asList(Arrays.asList(1))));
        assertThrows(IllegalArgumentException.class, () -> new TestRowAbstractIn(Arrays.asList("id"), emptySnapshot));
    }

    private static final class EmptyAbstractIn extends AbstractIn {
        EmptyAbstractIn() {
            super();
        }
    }

    @Test
    public void testGetPropName() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals("status", condition.propName());
    }

    @Test
    public void testGetValues() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(Arrays.asList("ACTIVE", "PENDING"), condition.values());
    }

    @Test
    public void testGetValues_Unmodifiable() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) condition.values()).add("CLOSED"));
    }

    @Test
    public void testUsesRowValueConstructorIsAHardRename() throws NoSuchMethodException {
        final TestAbstractIn scalar = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestRowAbstractIn rowValue = new TestRowAbstractIn(Operator.IN);

        assertFalse(scalar.usesRowValueConstructor());
        assertTrue(rowValue.usesRowValueConstructor());
        assertEquals(boolean.class, AbstractIn.class.getMethod("usesRowValueConstructor").getReturnType());
        assertThrows(NoSuchMethodException.class, () -> AbstractIn.class.getMethod("rowValueConstructor"));
    }

    // Verifies direct values and nested condition values are flattened into parameters.
    @Test
    public void testParameters() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(Arrays.asList("ACTIVE", "PENDING"), condition.parameters());
    }

    @Test
    public void testParameters_ConditionValues() {
        final TestAbstractIn condition = new TestAbstractIn("id", Arrays.asList(Filters.eq("status", "ACTIVE"), 2));

        assertEquals(Arrays.asList("ACTIVE", 2), condition.parameters());
    }

    @Test
    public void testToString() {
        final TestAbstractIn condition = new TestAbstractIn("orderStatus", Arrays.asList("ACTIVE", "PENDING"));

        final String sql = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("order_status"));
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("'ACTIVE'"));
    }

    @Test
    public void testToString_NullNamingPolicy() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList(1, 2, 3));

        final String sql = condition.toSql(null);

        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("1"));
        assertTrue(sql.contains("3"));
    }

    @Test
    public void testHashCode() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE"));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentValues() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE"));

        assertNotEquals(left, right);
    }

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        final EmptyAbstractIn left = new EmptyAbstractIn();
        final EmptyAbstractIn right = new EmptyAbstractIn();

        assertNull(left.propName());
        assertTrue(left.values().isEmpty());
        assertTrue(left.parameters().isEmpty());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testToString_WithSubQueryValue_Batch2() {
        final TestAbstractIn condition = new TestAbstractIn("userId", Arrays.asList(Filters.subQuery("SELECT id FROM users")));

        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("userId IN"));
        assertTrue(sql.contains("(SELECT id FROM users)"));
    }

    @Test
    public void testDefaultConstructorToString() {
        final EmptyAbstractIn condition = new EmptyAbstractIn();

        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);

        assertNotNull(sql);
    }

    /**
     * Pass-3 regression: IN with {@link java.util.Date} values must render them as quoted
     * SQL date literals, not Java's {@code Date.toString()} form.
     */
    @Test
    public void testToString_DateValuesAreQuoted_Pass3() {
        final TestAbstractIn condition = new TestAbstractIn("orderDate", Arrays.asList(new java.util.Date(0L), new java.util.Date(86400000L)));
        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("'"), "Date values must be quoted, got: " + sql);
        assertTrue(!sql.contains("Wed Dec") && !sql.contains("Thu Jan") && !sql.contains("PST"),
                "Output must not contain Java's Date.toString() form, got: " + sql);
    }

    /**
     * Pass-3 regression: NaN must be rejected from IN values rather than silently emitted as
     * bare {@code NaN} tokens.
     */
    @Test
    public void testToString_NaNValueIsRejected_Pass3() {
        final TestAbstractIn condition = new TestAbstractIn("v", Arrays.asList(1.0, Double.NaN, 2.0));
        assertThrows(IllegalArgumentException.class, () -> condition.toSql(NamingPolicy.NO_CHANGE));
    }
}
