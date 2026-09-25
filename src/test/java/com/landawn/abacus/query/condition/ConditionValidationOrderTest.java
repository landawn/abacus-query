package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.SortDirection;

@Tag("2025")
class ConditionValidationOrderTest {

    @Test
    void compositionChecksReceiverBeforeArgument() {
        final SqlExpression invalidReceiver = SqlExpression.of("");

        assertTrue(assertThrows(IllegalArgumentException.class, () -> invalidReceiver.and((Condition) null)).getMessage().contains("composable method 'and'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> invalidReceiver.or((Condition) null)).getMessage().contains("composable method 'or'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> invalidReceiver.xor(null)).getMessage().contains("composable method 'xor'"));
    }

    @Test
    void rangeChecksEntireMinimumBeforeMaximum() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Between("age", SqlExpression.of(""), null)).getMessage()
                .contains("minValue must not be a blank SqlExpression"));

        final Object[] cyclicMinimum = new Object[1];
        cyclicMinimum[0] = cyclicMinimum;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new NotBetween("age", cyclicMinimum, null)).getMessage().contains("Cyclic object arrays"));
    }

    @Test
    void criteriaChecksSortPairsInSignatureOrder() {
        final String firstDirectionError = "SortDirection for 'firstName' in the sort map must not be null";

        assertEquals(firstDirectionError, assertThrows(IllegalArgumentException.class,
                () -> Criteria.builder().orderBy("firstName", null, "", SortDirection.ASC)).getMessage());
        assertEquals(firstDirectionError, assertThrows(IllegalArgumentException.class,
                () -> Criteria.builder().groupBy("firstName", null, "firstName", SortDirection.DESC, "thirdName", SortDirection.ASC)).getMessage());

        assertEquals(" ORDER BY firstName ASC, lastName DESC",
                Criteria.builder().orderBy("firstName", SortDirection.ASC, "lastName", SortDirection.DESC).build().toString());
    }

    @Test
    void sortMapValidationKeepsOneReadOfEachEntry() {
        final Map<String, SortDirection> source = new LinkedHashMap<>();
        source.put("firstName", SortDirection.ASC);
        source.put("lastName", SortDirection.DESC);
        final Map<String, SortDirection> oneReadMap = new AbstractMap<>() {
            private boolean read;

            @Override
            public int size() {
                return source.size();
            }

            @Override
            public Set<Entry<String, SortDirection>> entrySet() {
                if (read) {
                    throw new IllegalStateException("entrySet was read twice");
                }
                read = true;
                return source.entrySet();
            }
        };

        assertEquals("ORDER BY firstName ASC, lastName DESC", new OrderBy(oneReadMap).toString());
    }

    @Test
    void rowValueConstructionPropagatesGetterFailure() {
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
