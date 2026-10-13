package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.SubQuery;

public class SubQuerySnapshotTest extends TestBase {

    @Test
    void testBuilderSnapshotRetainsBindingStructureAfterCallerMutation() {
        final List<Integer> ids = new java.util.ArrayList<>(List.of(1, 2));
        final SubQuery subquery = Dsl.PSC.select("id").from("users").where(Filters.in("id", ids)).toSubQuery();
        ids.clear();

        assertEquals(List.of(1, 2), subquery.parameters());
        assertThrows(UnsupportedOperationException.class, () -> subquery.parameters().clear());
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
        assertNotEquals(value, new SubQuerySnapshot("SELECT id FROM archive", List.of(), SqlPolicy.NAMED_SQL, false, false,
                Map.of(), Set.of(), Map.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("differentCompositionMetadata")
    void testEqualsRetainsMetadataNeededForParentComposition(final String scenario, final SubQuerySnapshot other) {
        assertNotEquals(snapshot(List.of()), other, scenario);
        assertNotEquals(other, snapshot(List.of()), scenario);
    }

    private static Stream<Arguments> differentBindings() {
        return Stream.of(Arguments.of("empty", List.of()), Arguments.of("shorter", List.of(1)),
                Arguments.of("longer", Arrays.asList(1, null, 2)), Arguments.of("different order", Arrays.asList(null, 1)),
                Arguments.of("different value", Arrays.asList(2, null)));
    }

    private static Stream<Arguments> differentCompositionMetadata() {
        return Stream.of(
                Arguments.of("SQL policy", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.PARAMETERIZED_SQL, false, false,
                        Map.of(), Set.of(), Map.of())),
                Arguments.of("generated placeholder", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, true, false,
                        Map.of(), Set.of(), Map.of())),
                Arguments.of("set operation isolation", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, true,
                        Map.of(), Set.of(), Map.of())),
                Arguments.of("occurrence counts", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false,
                        Map.of("id", 1), Set.of(), Map.of())),
                Arguments.of("generated names", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false,
                        Map.of(), Set.of("id"), Map.of())),
                Arguments.of("rendered tokens", new SubQuerySnapshot("SELECT id FROM users", List.of(), SqlPolicy.NAMED_SQL, false, false,
                        Map.of(), Set.of(), Map.of("id", ":id"))));
    }

    private static SubQuerySnapshot snapshot(final List<Object> parameters) {
        return new SubQuerySnapshot("SELECT id FROM users", parameters, SqlPolicy.NAMED_SQL, false, false, Map.of(), Set.of(), Map.of());
    }
}
