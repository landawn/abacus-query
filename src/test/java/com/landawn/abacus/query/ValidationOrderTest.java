package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("2025")
class ValidationOrderTest {
    @Test
    void dynamicQueryChecksClauseStateBeforeArguments() {
        final DynamicQuery.Builder builder = DynamicQuery.builder();
        assertThrows(IllegalStateException.class, () -> builder.from().join(null, null));
        assertThrows(IllegalStateException.class, () -> builder.where().appendPlaceholders(-1, null, null));
        assertThrows(IllegalStateException.class, () -> builder.having().appendPlaceholders(-1, null, null));

        builder.select().append("id");
        builder.from().append("users");
        builder.where().append("id IN ");
        assertThrows(IllegalArgumentException.class, () -> builder.where().appendPlaceholders(0, null, null));
        builder.where().appendPlaceholders(1, "(", ")");
        assertEquals("SELECT id FROM users WHERE id IN (?)", builder.build());
        assertThrows(IllegalStateException.class, () -> builder.limit(-1));
    }

    @Test
    void dynamicQueryChecksPaginationStateBeforeArguments() {
        final DynamicQuery.Builder builder = DynamicQuery.builder().limit(5);
        assertThrows(IllegalStateException.class, () -> builder.limit(-1));
        assertThrows(IllegalStateException.class, () -> builder.fetchFirstRows(-1));
        assertEquals("LIMIT 5", builder.build());
    }

    @Test
    void filtersCheckParametersInSignatureOrder() {
        assertEquals("Property name must not be null, empty, or blank",
                assertThrows(IllegalArgumentException.class, () -> Filters.contains(" ", null)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Filters.orderBy("first", null, " ", SortDirection.ASC)).getMessage().contains("SortDirection for 'first'"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Filters.anyOfAllEqual(Collections.emptyList(), null)).getMessage().contains("entities"));
    }

    @Test
    void invalidPropertyNamesAreRejectedBeforeReadingBeanValues() {
        final CountingBean bean = new CountingBean();
        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual(bean, Arrays.asList("name", " ")));
        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual(bean, Arrays.asList("name", "missing")));
        assertThrows(IllegalArgumentException.class,
                () -> Filters.anyOfAllEqual(Arrays.asList(bean, Collections.emptyMap()), Collections.singletonList("name")));
        assertEquals(0, bean.reads);
        assertEquals("name = 'Ada'", Filters.allEqual(bean, Collections.singletonList("name")).conditions().get(0).toString());
        assertEquals(1, bean.reads);
    }

    public static class CountingBean {
        int reads;

        public String getName() {
            reads++;
            return "Ada";
        }

        public void setName(final String name) {
            // A writable property lets the bean metadata resolver recognize this fixture.
        }
    }
}
