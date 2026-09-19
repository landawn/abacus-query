package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
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

    @Test
    public void testRejectsNullScalarAndTupleMembers() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractIn("status", Arrays.asList("A", null, "B")));
        assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("id", "tenant"), Arrays.asList(Arrays.asList(1, null))));
    }

    @Test
    public void testMapRowsDistinguishMissingKeysFromExplicitNullValues() {
        final Map<String, Object> missing = new LinkedHashMap<>();
        missing.put("id", 1);

        final IllegalArgumentException missingError = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("id", "tenant"), Arrays.asList(missing)));
        assertTrue(missingError.getMessage().contains("missing required property key: tenant"));

        final Map<String, Object> explicitNull = new LinkedHashMap<>();
        explicitNull.put("id", 1);
        explicitNull.put("tenant", null);

        final IllegalArgumentException nullError = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("id", "tenant"), Arrays.asList(explicitNull)));
        assertTrue(nullError.getMessage().contains("null found at index 1"));
    }

    @Test
    public void testOversizedIterableConsumesOnlyArityPlusOneElements() {
        final int[] nextCalls = { 0 };
        final Iterable<Integer> unboundedRow = () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return nextCalls[0]++;
            }
        };

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(unboundedRow)));

        assertEquals(3, nextCalls[0]);
        assertTrue(error.getMessage().contains("found at least 3"));
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

    // Verifies direct values are exposed as parameters in encounter order.
    @Test
    public void testParameters() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(Arrays.asList("ACTIVE", "PENDING"), condition.parameters());
    }

    @Test
    public void testRejectsPredicateValuedMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestAbstractIn("id", Arrays.asList(Filters.eq("status", "ACTIVE"), 2)));
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
    public void testHashCodeTracksRetainedMutableValueElement() {
        final List<Integer> mutableValue = new ArrayList<>(List.of(1));
        final TestAbstractIn condition = new TestAbstractIn("payload", List.of(mutableValue));

        condition.hashCode();
        mutableValue.add(2);

        final TestAbstractIn equalAfterMutation = new TestAbstractIn("payload", List.of(List.of(1, 2)));
        assertEquals(condition, equalAfterMutation);
        assertEquals(condition.hashCode(), equalAfterMutation.hashCode());
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
    public void testEqualsAndHashCode_ArrayMembershipUsesContentEquality() {
        final TestAbstractIn left = new TestAbstractIn("payload", Arrays.asList(new byte[] { 1, 2 }));
        final TestAbstractIn right = new TestAbstractIn("payload", Arrays.asList(new byte[] { 1, 2 }));

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals_ArrayMembershipDifferentContent() {
        final TestAbstractIn left = new TestAbstractIn("payload", Arrays.asList(new byte[] { 1, 2 }));
        final TestAbstractIn right = new TestAbstractIn("payload", Arrays.asList(new byte[] { 1, 3 }));

        assertNotEquals(left, right);
    }

    @Test
    public void testEqualsAndHashCode_RowValueArrayMembershipUsesContentEquality() {
        final TestRowAbstractIn left = new TestRowAbstractIn(Arrays.asList("a", "b"),
                Arrays.asList(Arrays.asList(new byte[] { 1 }, new byte[] { 2 })));
        final TestRowAbstractIn right = new TestRowAbstractIn(Arrays.asList("a", "b"),
                Arrays.asList(Arrays.asList(new byte[] { 1 }, new byte[] { 2 })));

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
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

    /**
     * Mirror of {@code BinaryTest.testInCollectionSnapshotsMutableElementsAndDefensivelyExposesThem}: array
     * elements are snapshotted at construction and exposed as defensive copies by values()/parameters().
     */
    @Test
    public void testInCollectionSnapshotsMutableElementsAndDefensivelyExposesThem() {
        final byte[] member = { 1, 2 };
        final TestAbstractIn condition = new TestAbstractIn("payload", Arrays.asList(member));
        final TestAbstractIn originalSnapshot = new TestAbstractIn("payload", Arrays.asList(new byte[] { 1, 2 }));
        final int hashBeforeMutation = condition.hashCode();

        // Mutating the caller's array must not leak into the condition.
        member[0] = 9;
        assertEquals("payload IN ('[1, 2]')", condition.toSql(NamingPolicy.NO_CHANGE));
        assertEquals(hashBeforeMutation, condition.hashCode());

        // The exposed elements are copies, never the internal snapshot.
        final byte[] exposedValue = (byte[]) condition.values().get(0);
        assertNotSame(member, exposedValue);
        exposedValue[0] = 7;
        assertNotSame(exposedValue, condition.values().get(0));

        final byte[] exposedParameter = (byte[]) condition.parameters().get(0);
        exposedParameter[1] = 8;
        assertNotSame(exposedParameter, condition.parameters().get(0));

        assertEquals("payload IN ('[1, 2]')", condition.toSql(NamingPolicy.NO_CHANGE));
        assertTrue(Arrays.equals(new byte[] { 1, 2 }, (byte[]) condition.parameters().get(0)));
        assertEquals(originalSnapshot, condition);
        assertEquals(originalSnapshot.hashCode(), condition.hashCode());
    }

    @Test
    public void testRowValueTuplesSnapshotMutableElementsAndDefensivelyExposeThem() {
        final byte[] first = { 1 };
        final java.util.Date second = new java.util.Date(1000L);
        final TestRowAbstractIn condition = new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(first, second)));
        final TestRowAbstractIn originalSnapshot = new TestRowAbstractIn(Arrays.asList("a", "b"),
                Arrays.asList(Arrays.asList(new byte[] { 1 }, new java.util.Date(1000L))));
        final int hashBeforeMutation = condition.hashCode();

        first[0] = 9;
        second.setTime(2000L);

        assertEquals(hashBeforeMutation, condition.hashCode());
        assertEquals(originalSnapshot, condition);

        final List<?> tuple = (List<?>) condition.values().get(0);
        assertNotSame(first, tuple.get(0));
        assertEquals(1, ((byte[]) tuple.get(0))[0]);
        assertNotSame(second, tuple.get(1));
        assertEquals(1000L, ((java.util.Date) tuple.get(1)).getTime());

        ((byte[]) condition.parameters().get(0))[0] = 5;
        assertEquals(1, ((byte[]) condition.parameters().get(0))[0]);
        assertEquals(1000L, ((java.util.Date) condition.parameters().get(1)).getTime());
    }

    @Test
    public void testNonMutableValuesKeepMemoizedViews() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("A", "B"));

        assertSame(condition.values(), condition.values());
        assertSame(condition.parameters(), condition.parameters());
    }

    @Test
    public void testRowWidthMessageIsExactForObjectArrayRows() {
        final IllegalArgumentException oversizedArray = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList((Object) new Object[] { 1, 2, 3 })));
        assertTrue(oversizedArray.getMessage().endsWith("but found 3"), oversizedArray.getMessage());

        final IllegalArgumentException undersizedList = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1))));
        assertTrue(undersizedList.getMessage().endsWith("but found 1"), undersizedList.getMessage());

        // A Collection row knows its exact size, so the message reports it (no "at least" lower bound).
        final IllegalArgumentException oversizedList = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2, 3, 4, 5))));
        assertTrue(oversizedList.getMessage().endsWith("but found 5"), oversizedList.getMessage());
        assertFalse(oversizedList.getMessage().contains("at least"), oversizedList.getMessage());

        // Only a non-Collection Iterable is read one element past the width, so its count stays a lower bound.
        final Iterable<Integer> oversizedIterable = () -> Arrays.asList(1, 2, 3, 4, 5).iterator();
        final IllegalArgumentException oversizedIter = assertThrows(IllegalArgumentException.class,
                () -> new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(oversizedIterable)));
        assertTrue(oversizedIter.getMessage().endsWith("but found at least 3"), oversizedIter.getMessage());
    }

    @Test
    public void testLargeScalarInReturnsMemoizedInstancesWhileArrayBearingInDoesNot() {
        final List<Integer> many = new ArrayList<>(10_000);

        for (int i = 0; i < 10_000; i++) {
            many.add(i);
        }

        // All-scalar membership list: the mutable-element flag is computed once at construction, so both
        // accessors hand out the same memoized instance on every call.
        final TestAbstractIn scalars = new TestAbstractIn("id", many);
        assertSame(scalars.values(), scalars.values());
        assertSame(scalars.parameters(), scalars.parameters());
        assertEquals(10_000, scalars.values().size());

        // An array element forces a fresh defensively-copied list on every call.
        final TestAbstractIn arrays = new TestAbstractIn("hash", Arrays.asList(new byte[] { 1 }, "x"));
        assertNotSame(arrays.values(), arrays.values());
        assertNotSame(arrays.parameters(), arrays.parameters());

        final TestRowAbstractIn rowArrays = new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(new byte[] { 1 }, 2)));
        assertNotSame(rowArrays.values(), rowArrays.values());
        assertNotSame(rowArrays.parameters(), rowArrays.parameters());

        final TestRowAbstractIn rowScalars = new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        assertSame(rowScalars.values(), rowScalars.values());
        assertSame(rowScalars.parameters(), rowScalars.parameters());
    }

    @Test
    @Tag("2025")
    public void testToSqlPreservesLeadingAndTrailingUnderscoreRuns() {
        // AbstractIn / AbstractInSubQuery / SubQuery must convert names through QueryUtil.convertIdentifier
        // so leading/trailing '_' runs survive instead of being stripped by NamingPolicy.
        assertEquals("_id IN (1, 2)", Filters.in("_id", Arrays.asList(1, 2)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name_ NOT IN (1, 2)", Filters.notIn("firstName_", Arrays.asList(1, 2)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ IN (1)", Filters.in("_", Arrays.asList(1)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_1 IN (1)", Filters.in("_1", Arrays.asList(1)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.__v IN (1)", Filters.in("t.__v", Arrays.asList(1)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ID IN (1, 2)", Filters.in("_id", Arrays.asList(1, 2)).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("FIRST_NAME_ NOT IN (1, 2)", Filters.notIn("firstName_", Arrays.asList(1, 2)).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));

        // multi-column (row-value) IN
        assertEquals("(_id, first_name_) IN ((1, 2))",
                Filters.in(Arrays.asList("_id", "firstName_"), Arrays.asList(Arrays.asList(1, 2))).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("(_ID, FIRST_NAME_) IN ((1, 2))",
                Filters.in(Arrays.asList("_id", "firstName_"), Arrays.asList(Arrays.asList(1, 2))).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));

        // InSubQuery: single and multi-column
        assertEquals("_id IN (SELECT 1)", Filters.in("_id", Filters.subQuery("SELECT 1")).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_ID IN (SELECT 1)", Filters.in("_id", Filters.subQuery("SELECT 1")).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("(_id, __v) IN (SELECT 1, 2)",
                Filters.in(Arrays.asList("_id", "__v"), Filters.subQuery("SELECT 1, 2")).toSql(NamingPolicy.SNAKE_CASE));

        // structured SubQuery: selected prop names AND the table name keep their runs
        assertEquals("SELECT _id, first_name_ FROM _tbl WHERE _x = 1",
                Filters.subQuery("_tbl", Arrays.asList("_id", "firstName_"), Filters.eq("_x", 1)).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("SELECT _ID, FIRST_NAME_ FROM _TBL WHERE _X = 1",
                Filters.subQuery("_tbl", Arrays.asList("_id", "firstName_"), Filters.eq("_x", 1)).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
    }

    @Test
    public void testParametersOfSubQueryElementAreNotSharedAcrossCalls() {
        // Filters.in(String, SubQuery) routes to InSubQuery (which delegates to SubQuery.parameters() on every call);
        // AbstractIn itself accepts a scalar SubQuery / SqlExpression as a membership element, and must not memoize
        // the inner Binary's per-call array copies, otherwise a mutation through the outer parameters() would leak.
        final SubQuery subQuery = Filters.subQuery("files", Arrays.asList("id"), Filters.eq("blob", new byte[] { 1, 2 }));
        final TestAbstractIn condition = new TestAbstractIn("fileId", Arrays.asList(subQuery, 3));

        final byte[] first = (byte[]) condition.parameters().get(0);
        assertEquals(2, first[1]);
        first[1] = 9;
        assertEquals(2, ((byte[]) condition.parameters().get(0))[1]);
        assertEquals(3, condition.parameters().get(1));
        assertNotSame(condition.parameters(), condition.parameters());

        // Row-value form with a sub-query tuple element.
        final TestRowAbstractIn rows = new TestRowAbstractIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(subQuery, 3)));
        ((byte[]) rows.parameters().get(0))[0] = 7;
        assertEquals(1, ((byte[]) rows.parameters().get(0))[0]);
        assertNotSame(rows.parameters(), rows.parameters());

        // All-scalar membership lists keep the O(1) memoized instances.
        final TestAbstractIn scalars = new TestAbstractIn("id", Arrays.asList(1, 2));
        assertSame(scalars.parameters(), scalars.parameters());
        assertSame(scalars.values(), scalars.values());
    }
}
