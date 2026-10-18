package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.N;

public class SubQuerySnapshotTest extends TestBase {

    @Test
    void testBuilderSnapshotRetainsBindingStructureAfterCallerMutation() {
        final List<Integer> ids = new java.util.ArrayList<>(List.of(1, 2));
        final SubQuery subquery = Dsl.PSC.select("id").from("users").where(Filters.in("id", ids)).toSubQuery();
        ids.clear();

        assertEquals(List.of(1, 2), subquery.parameters());
        assertThrows(UnsupportedOperationException.class, () -> subquery.parameters().clear());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutableBindings")
    void testMutableBindingsAreIsolatedAtCaptureAndOnEveryRead(final String scenario, final Supplier<Object> newValue, final Consumer<Object> mutate) {
        final Object callerValue = newValue.get();
        final SubQuerySnapshot value = snapshot(Arrays.asList(callerValue, null, 7));
        final SubQuerySnapshot expected = snapshot(Arrays.asList(newValue.get(), null, 7));
        final int originalHash = value.hashCode();
        final Set<SubQuery> keys = new HashSet<>(List.of(value));

        // Neither the original binding nor an element returned to a caller may alter a reusable snapshot's value or hash.
        mutate.accept(callerValue);
        assertEquals(expected, value, scenario);
        final List<Object> firstRead = value.parameters();
        final List<Object> secondRead = value.parameters();
        assertNotSame(firstRead, secondRead, scenario);
        assertThrows(UnsupportedOperationException.class, firstRead::clear, scenario);
        assertNotSame(firstRead.get(0), secondRead.get(0), scenario);
        mutate.accept(firstRead.get(0));

        assertTrue(N.deepEquals(newValue.get(), secondRead.get(0)), scenario);
        assertEquals(expected, value, scenario);
        assertEquals(originalHash, value.hashCode(), scenario);
        assertTrue(keys.contains(value), scenario);
        assertTrue(keys.contains(expected), scenario);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutableBindings")
    void testParentBindingsCannotMutateSnapshotOrOtherParents(final String scenario, final Supplier<Object> newValue, final Consumer<Object> mutate) {
        for (final Dsl dsl : List.of(Dsl.PSC, Dsl.NSC, Dsl.MSC)) {
            final SubQuery value = dsl.select("id").from("blob_store").where(Filters.eq("payload", newValue.get())).toSubQuery();
            final SubQuery expected = dsl.select("id").from("blob_store").where(Filters.eq("payload", newValue.get())).toSubQuery();
            final int originalHash = value.hashCode();
            final List<Object> first = dsl.select("id").from("x").where(Filters.in("id", value)).build().parameters();
            final List<Object> second = dsl.select("id").from("y").where(Filters.in("id", value)).build().parameters();

            // Parent composition formerly copied the list references directly, bypassing defensive getter copies.
            mutate.accept(first.get(0));
            assertTrue(N.deepEquals(newValue.get(), second.get(0)), scenario);
            final List<Object> later = dsl.select("id").from("z").where(Filters.in("id", value)).build().parameters();
            assertTrue(N.deepEquals(newValue.get(), later.get(0)), scenario);
            assertEquals(expected, value, scenario);
            assertEquals(originalHash, value.hashCode(), scenario);

            // Reusing the snapshot twice in one parent must give each occurrence independent mutable bindings too.
            final List<Object> repeated = dsl.select("id")
                    .from("x")
                    .where(Filters.and(Filters.in("id", value), Filters.in("other_id", value)))
                    .build()
                    .parameters();
            mutate.accept(repeated.get(0));
            assertTrue(N.deepEquals(newValue.get(), repeated.get(1)), scenario);
            assertEquals(expected, value, scenario);
        }
    }

    @Test
    void testScalarBindingsKeepStableListWithoutCopyingOpaqueObjects() {
        final Object opaque = new StringBuilder("application value");
        final List<Object> callerParameters = new ArrayList<>(Arrays.asList(1, null, "active", opaque));
        final SubQuerySnapshot value = snapshot(callerParameters);
        callerParameters.clear();

        assertEquals(Arrays.asList(1, null, "active", opaque), value.parameters());
        assertSame(value.parameters(), value.parameters());
        // All other values, including mutable JDK types such as StringBuilder, remain shared by reference.
        assertSame(opaque, value.parameters().get(3));
        assertEquals(List.of(), snapshot(null).parameters());
        assertEquals(List.of(), snapshot(List.of()).parameters());
    }

    @Test
    void testSnapshotRejectsDirectObjectArrayCycle() {
        final Object[] cycle = new Object[1];
        cycle[0] = cycle;

        // Exercise the constructor's documented rejection, not only the shared value-copying helper.
        assertThrows(IllegalArgumentException.class, () -> snapshot(List.of((Object) cycle)));
    }

    @Test
    void testSnapshotRejectsIndirectObjectArrayCycle() {
        final Object[] first = new Object[1];
        final Object[] second = new Object[] { first };
        first[0] = second;

        assertThrows(IllegalArgumentException.class, () -> snapshot(List.of((Object) first)));
    }

    @Test
    void testEqualsAndHashCodeCompareNestedArrayBindingsByContent() {
        final SubQuerySnapshot first = snapshot(Arrays.asList(new byte[] { 1, 2 }, new Object[] { new int[] { 3 }, null }));
        final SubQuerySnapshot second = snapshot(Arrays.asList(new byte[] { 1, 2 }, new Object[] { new int[] { 3 }, null }));

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, snapshot(Arrays.asList(new byte[] { 1, 3 }, new Object[] { new int[] { 3 }, null })));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("differentBindings")
    void testEqualsDistinguishesBindingCountOrderAndNulls(final String scenario, final List<Object> parameters) {
        final SubQuerySnapshot first = snapshot(Arrays.asList(1, null));
        final SubQuerySnapshot second = snapshot(parameters);

        assertNotEquals(first, second, scenario);
        assertNotEquals(second, first, scenario);
    }

    @Test
    void testEqualsRejectsDifferentSqlAndOtherSubqueryKinds() {
        final SubQuerySnapshot value = snapshot(List.of());

        assertNotEquals(null, value);
        assertNotEquals("SELECT id FROM users", value);
        assertNotEquals(new SubQuery("SELECT id FROM users"), value);
        assertNotEquals(value, new SubQuery("SELECT id FROM users"));
        assertNotEquals(value, new SubQuerySnapshot("SELECT id FROM archive", List.of(), SqlPolicy.NAMED_SQL, false, false, Map.of(), Set.of(), Map.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("differentCompositionMetadata")
    void testEqualsRetainsMetadataNeededForParentComposition(final String scenario, final SubQuerySnapshot other) {
        assertNotEquals(snapshot(List.of()), other, scenario);
        assertNotEquals(other, snapshot(List.of()), scenario);
    }

    private static Stream<Arguments> mutableBindings() {
        return Stream.of(Arguments.of("byte array", (Supplier<Object>) () -> new byte[] { 1, 2, 3 }, (Consumer<Object>) value -> ((byte[]) value)[0] = 99),
                Arguments.of("int array", (Supplier<Object>) () -> new int[] { 1, 2, 3 }, (Consumer<Object>) value -> ((int[]) value)[0] = 99),
                Arguments.of("date", (Supplier<Object>) () -> new Date(1000), (Consumer<Object>) value -> ((Date) value).setTime(5000)),
                Arguments.of("calendar", (Supplier<Object>) SubQuerySnapshotTest::bindingCalendar,
                        (Consumer<Object>) value -> ((Calendar) value).setTimeInMillis(5000)),
                Arguments.of("timestamp nanos", (Supplier<Object>) () -> Timestamp.valueOf("2026-10-17 12:30:00.123456789"),
                        (Consumer<Object>) value -> ((Timestamp) value).setNanos(987654321)),
                Arguments.of("nested arrays and mutable elements",
                        (Supplier<Object>) () -> new Object[] { new byte[] { 1, 2 }, new Object[] { new Date(1000), bindingCalendar() } },
                        (Consumer<Object>) value -> {
                            final Object[] array = (Object[]) value;
                            ((byte[]) array[0])[0] = 99;
                            final Object[] nested = (Object[]) array[1];
                            ((Date) nested[0]).setTime(5000);
                            ((Calendar) nested[1]).setTimeInMillis(5000);
                        }));
    }

    private static Calendar bindingCalendar() {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1000);
        return calendar;
    }

    private static Stream<Arguments> differentBindings() {
        return Stream.of(Arguments.of("empty", List.of()), Arguments.of("shorter", List.of(1)), Arguments.of("longer", Arrays.asList(1, null, 2)),
                Arguments.of("different order", Arrays.asList(null, 1)), Arguments.of("different value", Arrays.asList(2, null)));
    }

    private static Stream<Arguments> differentCompositionMetadata() {
        return Stream.of(
                Arguments.of("SQL policy",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.PARAMETERIZED_SQL, false, false, Map.of(), Set.of(), Map.of())),
                Arguments.of("generated placeholder",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, true, false, Map.of(), Set.of(), Map.of())),
                Arguments.of("set operation isolation",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, true, Map.of(), Set.of(), Map.of())),
                Arguments.of("occurrence counts",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false, Map.of("id", 1), Set.of(), Map.of())),
                Arguments.of("generated names",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false, Map.of(), Set.of("id"), Map.of())),
                Arguments.of("rendered tokens",
                        new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false, Map.of(), Set.of(), Map.of("id", ":id"))));
    }

    private static SubQuerySnapshot snapshot(final List<Object> parameters) {
        return new SubQuerySnapshot("SELECT id FROM users", parameters, SqlPolicy.NAMED_SQL, false, false, Map.of(), Set.of(), Map.of());
    }
}
