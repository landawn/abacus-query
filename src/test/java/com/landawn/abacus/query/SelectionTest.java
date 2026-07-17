/*
 * Copyright (C) 2026 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.entity.Account;

public class SelectionTest extends TestBase {

    @Test
    public void testBuilderRequiresEntityClassAtCreation() throws NoSuchMethodException {
        assertThrows(IllegalArgumentException.class, () -> Selection.builder(null));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("builder"));
        assertEquals(0, Selection.class.getConstructors().length);
    }

    @Test
    public void testBuilderDefaults() {
        final Selection selection = Selection.builder(Account.class).build();

        assertEquals(Account.class, selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.includedPropNames());
        assertNull(selection.excludedPropNames());
        assertFalse(selection.includesSubEntityProperties());
    }

    @Test
    public void testBuilderFullyPopulated() {
        final Selection selection = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .includeSubEntityProperties(true)
                .excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")))
                .build();

        assertEquals(Account.class, selection.entityClass());
        assertEquals("a", selection.tableAlias());
        assertEquals("account", selection.classAlias());
        assertEquals(Arrays.asList("id", "firstName"), selection.includedPropNames());
        assertTrue(selection.includesSubEntityProperties());
        assertEquals(Set.of("status"), selection.excludedPropNames());
    }

    @Test
    public void testBuilderMethodsChainOnBuilder() {
        final Selection.SelectionBuilder builder = Selection.builder(Account.class);

        assertTrue(builder == builder.tableAlias("a"));
        assertTrue(builder == builder.classAlias("account"));
        assertTrue(builder == builder.includeSubEntityProperties(true));
        assertTrue(builder == builder.includedPropNames(List.of("id")));
        assertTrue(builder == builder.excludedPropNames(Set.of("status")));
    }

    @Test
    public void testSelectionDeclaresExplicitReadOnlyAccessors() throws NoSuchMethodException {
        assertEquals(Class.class, Selection.class.getMethod("entityClass").getReturnType());
        assertEquals(String.class, Selection.class.getMethod("tableAlias").getReturnType());
        assertEquals(String.class, Selection.class.getMethod("classAlias").getReturnType());
        assertEquals(boolean.class, Selection.class.getMethod("includesSubEntityProperties").getReturnType());
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("entityClass", Class.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("tableAlias", String.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("classAlias", String.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("includeSubEntityProperties", boolean.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("includedPropNames", Collection.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("excludedPropNames", Set.class));
    }

    @Test
    public void testPropertyCollectionsAreDefensivelyCopiedAndImmutable() {
        final List<String> included = new ArrayList<>(Arrays.asList("id", "firstName"));
        final Set<String> excluded = new LinkedHashSet<>(Arrays.asList("status"));
        final Selection.SelectionBuilder builder = Selection.builder(Account.class).includedPropNames(included).excludedPropNames(excluded);
        final Selection selection = builder.build();

        included.add("lastName");
        excluded.add("createTime");
        builder.includedPropNames(List.of("changed")).excludedPropNames(Set.of("changed"));

        assertEquals(Arrays.asList("id", "firstName"), selection.includedPropNames());
        assertEquals(Set.of("status"), selection.excludedPropNames());
        assertThrows(UnsupportedOperationException.class, () -> selection.includedPropNames().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> selection.excludedPropNames().add("other"));
    }

    @Test
    public void testNullPropertyCollectionsPreserveDefaultSemantics() {
        final Selection selection = Selection.builder(Account.class).includedPropNames(null).excludedPropNames(null).build();

        assertNull(selection.includedPropNames());
        assertNull(selection.excludedPropNames());
    }

    @Test
    public void testEqualsAndHashCode() {
        final Selection first = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(Set.of("status"))
                .build();
        final Selection equal = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(Set.of("status"))
                .build();

        assertEquals(first, first);
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "not a selection");
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("b").classAlias("account").build());
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("a").classAlias("acct").build());
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("a").classAlias("account").includeSubEntityProperties(true).build());
    }

    @Test
    public void testToStringContainsFieldValues() {
        final String str = Selection.builder(Account.class).tableAlias("a").classAlias("account").build().toString();

        assertNotNull(str);
        assertTrue(str.contains("Selection("));
        assertTrue(str.contains("tableAlias=a"));
        assertTrue(str.contains("classAlias=account"));
    }

    @Test
    public void testDslSelectFromSingleSelection() {
        final Selection selection = Selection.builder(Account.class).tableAlias("a").classAlias("account").build();
        final String sql = Dsl.PSC.selectFrom(selection).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    public void testDslSelectSingleSelectionThenFrom() {
        final Selection selection = Selection.builder(Account.class).tableAlias("a").classAlias("account").build();
        final String sql = Dsl.PSC.select(selection).from("account a").build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
    }

    @Test
    public void testDslSelectFromMultipleSelections() {
        final List<Selection> selections = List.of(Selection.builder(Account.class).tableAlias("a").classAlias("account").build(),
                Selection.builder(Account.class).tableAlias("b").classAlias("account2").build());
        final String sql = Dsl.PSC.selectFrom(selections).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    public void testDslSelectUsesOneListSnapshotForValidationAndRendering() {
        final Selection firstPass = Selection.builder(Account.class).tableAlias("a").classAlias("account").includedPropNames(List.of("id")).build();
        final Selection laterPass = Selection.builder(Account.class).tableAlias("a").classAlias("account").includedPropNames(List.of("firstName")).build();

        assertEquals("SELECT a.id AS \"account.id\" FROM account a",
                Dsl.PSC.select(changingSelectionList(firstPass, laterPass)).from("account a").build().query());
        assertEquals("SELECT a.id AS \"account.id\" FROM account a", Dsl.PSC.selectFrom(changingSelectionList(firstPass, laterPass)).build().query());
    }

    private static List<Selection> changingSelectionList(final Selection firstPass, final Selection laterPass) {
        return new AbstractList<>() {
            private int iteratorCount;

            @Override
            public Iterator<Selection> iterator() {
                return List.of(++iteratorCount == 1 ? firstPass : laterPass).iterator();
            }

            @Override
            public Selection get(final int index) {
                if (index != 0) {
                    throw new IndexOutOfBoundsException(index);
                }

                return firstPass;
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }

    @Test
    public void testDslSelectNullSelectionThrows() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select((Selection) null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.selectFrom((Selection) null));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedTwoEntitySelectionValidatesBothEntityClassesConsistently() {
        final IllegalArgumentException selectFirst = assertThrows(IllegalArgumentException.class,
                () -> Dsl.PSC.select(null, "a", "first", null, Account.class, "b", "second", null));
        final IllegalArgumentException selectSecond = assertThrows(IllegalArgumentException.class,
                () -> Dsl.PSC.select(Account.class, "a", "first", null, null, "b", "second", null));
        final IllegalArgumentException selectFromFirst = assertThrows(IllegalArgumentException.class,
                () -> Dsl.PSC.selectFrom(null, "a", "first", null, Account.class, "b", "second", null));
        final IllegalArgumentException selectFromSecond = assertThrows(IllegalArgumentException.class,
                () -> Dsl.PSC.selectFrom(Account.class, "a", "first", null, null, "b", "second", null));

        assertEquals(selectFirst.getMessage(), selectSecond.getMessage());
        assertEquals(selectFirst.getMessage(), selectFromFirst.getMessage());
        assertEquals(selectFirst.getMessage(), selectFromSecond.getMessage());
    }
}
