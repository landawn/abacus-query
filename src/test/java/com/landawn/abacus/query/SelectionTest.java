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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.entity.Account;

public class SelectionTest extends TestBase {

    // ------------------------------------------------------------------------
    // No-arg constructor + fluent accessors
    // ------------------------------------------------------------------------

    @Test
    public void testNoArgConstructorDefaults() {
        Selection s = new Selection();

        assertNull(s.entityClass());
        assertNull(s.tableAlias());
        assertNull(s.classAlias());
        assertNull(s.includedPropNames());
        assertNull(s.excludedPropNames());
        assertFalse(s.includeSubEntityProperties());
    }

    @Test
    public void testFluentSettersAndGetters() {
        Selection s = new Selection().entityClass(Account.class).tableAlias("a").classAlias("account").includeSubEntityProperties(true);

        assertEquals(Account.class, s.entityClass());
        assertEquals("a", s.tableAlias());
        assertEquals("account", s.classAlias());
        assertTrue(s.includeSubEntityProperties());
    }

    @Test
    public void testFluentSettersReturnSameInstanceForChaining() {
        Selection s = new Selection();
        // @Accessors(fluent = true) implies chain = true, so each setter returns the same instance.
        assertTrue(s == s.entityClass(Account.class));
        assertTrue(s == s.tableAlias("a"));
        assertTrue(s == s.classAlias("account"));
        assertTrue(s == s.includeSubEntityProperties(true));
        assertTrue(s == s.includedPropNames(List.of("id")));
        assertTrue(s == s.excludedPropNames(Set.of("status")));
    }

    // ------------------------------------------------------------------------
    // includedPropNames: defensive copy on write, immutable view on read
    // ------------------------------------------------------------------------

    @Test
    public void testIncludedPropNamesStoresContent() {
        Selection s = new Selection().includedPropNames(Arrays.asList("id", "firstName"));

        assertEquals(2, s.includedPropNames().size());
        assertTrue(s.includedPropNames().contains("id"));
        assertTrue(s.includedPropNames().contains("firstName"));
    }

    @Test
    public void testIncludedPropNamesDefensiveCopyOnWrite() {
        List<String> input = new ArrayList<>(Arrays.asList("id", "firstName"));
        Selection s = new Selection().includedPropNames(input);

        // Mutating the caller's collection after the set must not affect the Selection.
        input.add("lastName");
        input.clear();

        assertEquals(2, s.includedPropNames().size());
        assertTrue(s.includedPropNames().contains("id"));
        assertTrue(s.includedPropNames().contains("firstName"));
    }

    @Test
    public void testIncludedPropNamesReturnsImmutableView() {
        Selection s = new Selection().includedPropNames(Arrays.asList("id", "firstName"));

        assertThrows(UnsupportedOperationException.class, () -> s.includedPropNames().add("lastName"));
    }

    @Test
    public void testIncludedPropNamesNullClearsSelection() {
        Selection s = new Selection().includedPropNames(Arrays.asList("id"));
        assertNotNull(s.includedPropNames());

        s.includedPropNames(null);
        assertNull(s.includedPropNames());
    }

    // ------------------------------------------------------------------------
    // excludedPropNames: defensive copy on write, immutable view on read
    // ------------------------------------------------------------------------

    @Test
    public void testExcludedPropNamesStoresContent() {
        Selection s = new Selection().excludedPropNames(new LinkedHashSet<>(Arrays.asList("status", "createTime")));

        assertEquals(2, s.excludedPropNames().size());
        assertTrue(s.excludedPropNames().contains("status"));
        assertTrue(s.excludedPropNames().contains("createTime"));
    }

    @Test
    public void testExcludedPropNamesDefensiveCopyOnWrite() {
        Set<String> input = new LinkedHashSet<>(Arrays.asList("status"));
        Selection s = new Selection().excludedPropNames(input);

        input.add("createTime");
        input.clear();

        assertEquals(1, s.excludedPropNames().size());
        assertTrue(s.excludedPropNames().contains("status"));
    }

    @Test
    public void testExcludedPropNamesReturnsImmutableView() {
        Selection s = new Selection().excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")));

        assertThrows(UnsupportedOperationException.class, () -> s.excludedPropNames().add("createTime"));
    }

    @Test
    public void testExcludedPropNamesNullClearsExclusion() {
        Selection s = new Selection().excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")));
        assertNotNull(s.excludedPropNames());

        s.excludedPropNames(null);
        assertNull(s.excludedPropNames());
    }

    // ------------------------------------------------------------------------
    // Package-private all-args constructor (used by SqlBuilder/Dsl and the builder)
    // ------------------------------------------------------------------------

    @Test
    public void testAllArgsConstructor() {
        Selection s = new Selection(Account.class, "a", "account", Arrays.asList("id", "firstName"), true, new LinkedHashSet<>(Arrays.asList("status")));

        assertEquals(Account.class, s.entityClass());
        assertEquals("a", s.tableAlias());
        assertEquals("account", s.classAlias());
        assertEquals(2, s.includedPropNames().size());
        assertTrue(s.includeSubEntityProperties());
        assertEquals(1, s.excludedPropNames().size());
    }

    @Test
    public void testAllArgsConstructorDefensiveCopy() {
        List<String> props = new ArrayList<>(Arrays.asList("id", "firstName"));
        Set<String> excluded = new LinkedHashSet<>(Arrays.asList("status"));

        Selection s = new Selection(Account.class, "a", "account", props, false, excluded);

        props.add("lastName");
        excluded.add("createTime");

        assertEquals(2, s.includedPropNames().size());
        assertEquals(1, s.excludedPropNames().size());
    }

    @Test
    public void testAllArgsConstructorPreservesNullCollections() {
        Selection s = new Selection(Account.class, null, null, null, false, null);

        assertEquals(Account.class, s.entityClass());
        assertNull(s.tableAlias());
        assertNull(s.classAlias());
        assertNull(s.includedPropNames());
        assertNull(s.excludedPropNames());
        assertFalse(s.includeSubEntityProperties());
    }

    // ------------------------------------------------------------------------
    // Lombok @Builder
    // ------------------------------------------------------------------------

    @Test
    public void testBuilderFullyPopulated() {
        Selection s = Selection.builder()
                .entityClass(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .includeSubEntityProperties(true)
                .excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")))
                .build();

        assertEquals(Account.class, s.entityClass());
        assertEquals("a", s.tableAlias());
        assertEquals("account", s.classAlias());
        assertEquals(2, s.includedPropNames().size());
        assertTrue(s.includeSubEntityProperties());
        assertEquals(1, s.excludedPropNames().size());
    }

    @Test
    public void testBuilderEmptyUsesDefaults() {
        Selection s = Selection.builder().build();

        assertNull(s.entityClass());
        assertNull(s.tableAlias());
        assertNull(s.classAlias());
        assertNull(s.includedPropNames());
        assertNull(s.excludedPropNames());
        assertFalse(s.includeSubEntityProperties());
    }

    @Test
    public void testBuilderDefensiveCopyThroughBuild() {
        List<String> props = new ArrayList<>(Arrays.asList("id", "firstName"));
        Selection s = Selection.builder().entityClass(Account.class).includedPropNames(props).build();

        // build() delegates to the all-args constructor, which copies defensively.
        props.add("lastName");

        assertEquals(2, s.includedPropNames().size());
        assertThrows(UnsupportedOperationException.class, () -> s.includedPropNames().add("x"));
    }

    // ------------------------------------------------------------------------
    // @Data: equals / hashCode / toString
    // ------------------------------------------------------------------------

    @Test
    public void testEqualsAndHashCode_sameViaFluentAndBuilder() {
        Selection viaFluent = new Selection().entityClass(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")));

        Selection viaBuilder = Selection.builder()
                .entityClass(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")))
                .build();

        assertEquals(viaFluent, viaBuilder);
        assertEquals(viaFluent.hashCode(), viaBuilder.hashCode());
    }

    @Test
    public void testEquals_reflexiveAndNullAndOtherType() {
        Selection s = new Selection().entityClass(Account.class).tableAlias("a");

        assertEquals(s, s);
        assertNotEquals(s, null);
        assertNotEquals(s, "not a selection");
    }

    @Test
    public void testEquals_differingFields() {
        Selection base = new Selection().entityClass(Account.class).tableAlias("a").classAlias("account");

        assertNotEquals(base, new Selection().entityClass(Account.class).tableAlias("b").classAlias("account"));
        assertNotEquals(base, new Selection().entityClass(Account.class).tableAlias("a").classAlias("acct"));
        assertNotEquals(base, new Selection().entityClass(Account.class).tableAlias("a").classAlias("account").includeSubEntityProperties(true));
    }

    @Test
    public void testToStringContainsFieldValues() {
        String str = new Selection().entityClass(Account.class).tableAlias("a").classAlias("account").toString();

        assertNotNull(str);
        assertTrue(str.contains("Selection("));
        assertTrue(str.contains("tableAlias=a"));
        assertTrue(str.contains("classAlias=account"));
    }

    // ------------------------------------------------------------------------
    // Integration: Selection -> Dsl SQL generation
    // ------------------------------------------------------------------------

    @Test
    public void testDslSelectFromSingleSelection() {
        String sql = Dsl.PSC.selectFrom(new Selection().entityClass(Account.class).tableAlias("a").classAlias("account")).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    public void testDslSelectSingleSelectionThenFrom() {
        String sql = Dsl.PSC.select(new Selection().entityClass(Account.class).tableAlias("a").classAlias("account")).from("account a").build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
    }

    @Test
    public void testDslSelectFromMultipleSelections() {
        List<Selection> selections = List.of(new Selection().entityClass(Account.class).tableAlias("a").classAlias("account"),
                new Selection().entityClass(Account.class).tableAlias("b").classAlias("account2"));

        String sql = Dsl.PSC.selectFrom(selections).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    public void testDslSelectNullSelectionThrows() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select((Selection) null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.selectFrom((Selection) null));
    }

    @Test
    public void testRequiredEntityBuilderEntryPoint() {
        Selection selection = Selection.builder(Account.class).tableAlias("a").build();
        assertEquals(Account.class, selection.entityClass());
        assertThrows(NullPointerException.class, () -> Selection.builder(null));
    }
}
