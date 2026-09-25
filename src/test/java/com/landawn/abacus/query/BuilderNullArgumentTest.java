package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class BuilderNullArgumentTest {
    @Test
    void fromClauseRejectsNullSelectionsBeforeLookingUpEntityMetadata() {
        final Selection invalidEntity = Selection.builder(String.class).build();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AbstractQueryBuilder.getFromClause(Arrays.asList(invalidEntity, null), NamingPolicy.SNAKE_CASE));
        assertTrue(exception.getMessage().contains("selection"));
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(null, NamingPolicy.SNAKE_CASE));
        assertThrows(IllegalArgumentException.class,
                () -> AbstractQueryBuilder.getFromClause(Collections.singletonList(null), NamingPolicy.SNAKE_CASE));
    }

    @Test
    void fromClausePreservesEmptyListsAndDefaultNaming() {
        assertEquals("", AbstractQueryBuilder.getFromClause(Collections.emptyList(), null));
        final List<Selection> selections = List.of(Selection.builder(Account.class).build());
        assertEquals(AbstractQueryBuilder.getFromClause(selections, NamingPolicy.SNAKE_CASE), AbstractQueryBuilder.getFromClause(selections, null));
    }

    @Test
    void nullSnapshotIsRejectedWithoutChangingTheParent() {
        final SqlBuilder parent = Dsl.PSC.select("id").from("account");
        assertThrows(IllegalArgumentException.class, () -> parent.appendSubQuerySnapshot(null));
        assertEquals("SELECT id FROM account", parent.build().query());
        assertThrows(IllegalStateException.class, () -> parent.appendSubQuerySnapshot(null));
    }

    @Test
    void optionalNullsRetainTheirMeaning() {
        final SqlBuilder builder = Dsl.PSC.select("id").selectModifier(null).from("account", (Class<?>) null);
        builder.appendIf(false, (Condition) null);
        assertEquals("SELECT id FROM account WHERE id IS NULL", builder.where(Filters.eq("id", null)).build().query());
        assertNull(AbstractQueryBuilder.normalizeColumnName(null, NamingPolicy.SNAKE_CASE));
        assertNull(AbstractQueryBuilder.sanitizeNamedParameterName(null));
        assertEquals("", AbstractQueryBuilder.tableAlias((Class<?>) null));
    }
}
