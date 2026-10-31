package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.condition.Or;

class QueryUtilityTuningTest {
    @Test
    void testBothOverloadsSnapshotExternalRowsOnce() {
        final SnapshotCollection<Object> inferredRows = new SnapshotCollection<>(Arrays.asList(null, new Row(7), null));
        final SnapshotCollection<Object> explicitRows = new SnapshotCollection<>(Arrays.asList(null, new Row(7), null));
        final SnapshotCollection<String> names = new SnapshotCollection<>(List.of("id"));

        assertEquals(List.of(7), Filters.anyOfAllEqual(inferredRows).parameters());
        assertEquals(List.of(7), Filters.anyOfAllEqual(explicitRows, names).parameters());
        assertEquals(1, inferredRows.snapshots);
        assertEquals(1, explicitRows.snapshots);
        assertEquals(1, names.snapshots);
    }

    @Test
    void testCommentReadingsIgnoreAnIrrelevantTailAfterTheLastOpener() {
        final String tail = "x".repeat(65_536);
        final String prefix = "/* \\ \r # [ --x $$x$$ */ /* done */ ";
        assertFalse(QueryUtil.hasUnterminatedBlockComment(prefix + tail));
        assertTrue(QueryUtil.hasUnterminatedBlockComment(prefix + tail + " /* unfinished"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("/* complete */ " + tail + " ' [ # -- $$"));
    }

    @Test
    void testEntityAndPropertyValidationRetainTheirPrecedence() {
        assertEquals("All specified entities/maps are null.",
                assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(null, null))).getMessage());
        assertEquals("All specified entities are null.",
                assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(null, null), null)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(List.of(), null)).getMessage().contains("entities"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(List.of(new Row(1), Map.of("id", 2)), null)).getMessage()
                .contains("maps require the map overload"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(List.of(new NoProperties(), Map.of("id", 2)))).getMessage()
                .contains("NoProperties"));
    }

    @Test
    void testEveryRowsMetadataIsCheckedBeforeAnyGetter() {
        final AtomicInteger reads = new AtomicInteger();
        final Row first = new Row(7);
        first.onRead = reads::incrementAndGet;
        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(null, first, null, new MissingId()), List.of("id")));
        assertEquals(0, reads.get());
        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(null, first, null, Map.of("id", 9))));
        assertEquals(0, reads.get());
        assertEquals(List.of(7), Filters.anyOfAllEqual(List.of(first), List.of("id")).parameters());
        assertEquals(1, reads.get());
    }

    @Test
    void testGetterCallbacksCannotChangeOwnedRowAndNameSnapshots() {
        final List<Object> rows = new ArrayList<>();
        final List<String> names = new ArrayList<>(List.of("id"));
        final Row first = new Row(7);
        first.onRead = () -> {
            rows.clear();
            rows.add(Map.of("invalid", 1));
            names.clear();
            names.add("missing");
        };
        rows.add(first);
        rows.add(null);
        rows.add(new OtherRow(9));

        assertEquals(List.of(7, 9), Filters.anyOfAllEqual(rows, names).parameters());
        assertEquals(List.of("missing"), names);
    }

    @Test
    void testLastOpenerInsideQuotedRegionsPreservesTheExistingReadingUnion() {
        assertTrue(QueryUtil.hasUnterminatedBlockComment("'a\\'/*'"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("$$'$$ /* x '"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("$$'a /* x '"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("/* a */ \"/*\""));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("[a/*b]"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("a # it's\n/* x 'y'"));
        // This optimization does not add dialect readings absent from the existing scanner.
        assertFalse(QueryUtil.hasUnterminatedBlockComment("a // it's\n/* x 'y'"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("n = q'[it's]' /* x '"));
    }

    @Test
    void testSkipsStillReadPastTheLastOpenerToFindClosers() {
        final String tail = "x".repeat(4096);
        assertFalse(QueryUtil.hasUnterminatedBlockComment("'/*" + tail + "'"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("'/*" + tail));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("\"/*" + tail + "\""));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("/* outer /* inner " + tail + " */ */"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("/* outer /* inner " + tail + " */"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("-- /*" + tail));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("--x /*" + tail));
    }

    @Test
    void testSortDiagnosticsRetainExactMessagesAndPairOrder() {
        assertEquals("SortDirection for 'first' must not be null",
                assertThrows(IllegalArgumentException.class, () -> Filters.orderBy("first", null, " ", SortDirection.ASC)).getMessage());
        assertEquals("Property name must not be null, empty, or blank",
                assertThrows(IllegalArgumentException.class, () -> Filters.groupBy(" ", null, "last", SortDirection.ASC)).getMessage());
        assertEquals("SortDirection for 'last' must not be null",
                assertThrows(IllegalArgumentException.class, () -> Filters.groupBy("first", SortDirection.ASC, "last", null)).getMessage());
    }

    @Test
    void testSparseEntityBatchesPreserveOrderAndMixedRuntimeClasses() {
        final List<Object> rows = new ArrayList<>(Collections.nCopies(65_536, null));
        rows.set(17, new Row(7));
        rows.set(65_535, new OtherRow(9));

        final Or inferred = Filters.anyOfAllEqual(rows);
        final Or explicit = Filters.anyOfAllEqual(rows, List.of("id"));
        assertEquals(List.of(7, 9), inferred.parameters());
        assertEquals(inferred.parameters(), explicit.parameters());
        assertEquals(inferred.toString(), explicit.toString());
        assertNull(rows.get(0));
    }

    public static class Row {
        private int value;
        private Runnable onRead;

        public Row() {
        }

        Row(final int id) {
            value = id;
        }

        public int getId() {
            if (onRead != null) {
                onRead.run();
            }
            return value;
        }

        public void setId(final int id) {
            value = id;
        }
    }

    public static class OtherRow {
        private int id;

        public OtherRow() {
        }

        OtherRow(final int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    public static class MissingId {
        public int getOther() {
            return 1;
        }

        public void setOther(final int value) {
        }
    }

    public static class NoProperties {
    }

    private static final class SnapshotCollection<T> extends AbstractCollection<T> {
        private final List<T> values;
        private int snapshots;

        SnapshotCollection(final Collection<T> values) {
            this.values = new ArrayList<>(values);
        }

        @Override
        public Object[] toArray() {
            if (++snapshots != 1) {
                throw new AssertionError("External collection snapshotted more than once");
            }
            return values.toArray();
        }

        @Override
        public Iterator<T> iterator() {
            throw new AssertionError("Read the owned snapshot rather than revisiting the external collection");
        }

        @Override
        public int size() {
            return values.size();
        }
    }
}
