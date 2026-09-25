package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
class BuilderValidationOrderTest {
    @Test
    void closedBuilderStatePrecedesInvalidClauseArguments() {
        assertClosedFirst(builder -> builder.from((String) null));
        assertClosedFirst(builder -> builder.from((String[]) null));
        assertClosedFirst(builder -> builder.from((Collection<String>) null));
        assertClosedFirst(builder -> builder.from((Class<?>) null));
        assertClosedFirst(builder -> builder.join((Class<?>) null));
        assertClosedFirst(builder -> builder.on((String) null));
        assertClosedFirst(builder -> builder.on((Condition) null));
        assertClosedFirst(builder -> builder.using((String[]) null));
        assertClosedFirst(builder -> builder.where((Condition) null));
        assertClosedFirst(builder -> builder.having((String) null));
        assertClosedFirst(builder -> builder.groupBy((Map<String, SortDirection>) null));
        assertClosedFirst(builder -> builder.groupByAsc((String[]) null));
        assertClosedFirst(builder -> builder.orderBy((Collection<String>) null));
        assertClosedFirst(builder -> builder.orderByDesc((String[]) null));
        assertClosedFirst(builder -> builder.append((Condition) null));
    }

    @Test
    void closedBuilderStatePrecedesNullTerminalCallbacks() {
        assertClosedFirst(builder -> builder.apply((Throwables.Function<SP, Object, RuntimeException>) null));
        assertClosedFirst(builder -> builder.apply((Throwables.BiFunction<String, java.util.List<Object>, Object, RuntimeException>) null));
        assertClosedFirst(builder -> builder.accept((Throwables.Consumer<SP, RuntimeException>) null));
        assertClosedFirst(builder -> builder.accept((Throwables.BiConsumer<String, java.util.List<Object>, RuntimeException>) null));
    }

    @Test
    void nullCallbackDoesNotConsumeAnOpenBuilder() {
        final SqlBuilder builder = Dsl.PSC.select("id").from("account");
        assertThrows(IllegalArgumentException.class, () -> builder.apply((Throwables.Function<SP, Object, RuntimeException>) null));
        assertEquals("SELECT id FROM account", builder.build().query());
    }

    @Test
    void missingJoinStatePrecedesConnectorArgumentsAndLeavesQueryReusable() {
        final SqlBuilder builder = Dsl.PSC.select("id").from("account");
        assertThrows(IllegalStateException.class, () -> builder.on((String) null));
        assertThrows(IllegalStateException.class, () -> builder.using((String[]) null));
        assertEquals("SELECT id FROM account", builder.build().query());
    }

    @Test
    void unselectedConditionalArgumentsRemainLazy() {
        final SqlBuilder builder = Dsl.PSC.select("id").from("account");
        builder.appendIf(false, (Condition) null).appendIf(false, (String) null);
        builder.appendIfOrElse(true, "WHERE id = 1", (String) null);
        assertEquals("SELECT id FROM account WHERE id = 1", builder.build().query());
    }

    private static void assertClosedFirst(final Consumer<SqlBuilder> operation) {
        final SqlBuilder builder = Dsl.PSC.select("id").from("account");
        builder.build();
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> operation.accept(builder));
        assertTrue(exception.getMessage().contains("closed"));
    }
}
