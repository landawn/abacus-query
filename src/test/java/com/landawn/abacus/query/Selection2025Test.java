/*
 * Copyright (C) 2025 HaiYang Li
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Selection2025Test extends TestBase {

    @Test
    public void testNoArgsConstructor() {
        Selection selection = new Selection();
        assertNotNull(selection);
        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertFalse(selection.includeSubEntityProperties());
        assertNull(selection.excludedPropNames());
    }

    @Test
    public void testAllArgsConstructor() {
        List<String> propNames = Arrays.asList("id", "name");
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        Selection selection = new Selection(String.class, "t", "alias", propNames, true, excluded);

        assertEquals(String.class, selection.entityClass());
        assertEquals("t", selection.tableAlias());
        assertEquals("alias", selection.classAlias());
        assertEquals(propNames, selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excluded, selection.excludedPropNames());
    }

    @Test
    public void testFluentSetters() {
        List<String> propNames = Arrays.asList("id", "name");
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        Selection selection = new Selection().entityClass(String.class)
                .tableAlias("t")
                .classAlias("alias")
                .selectPropNames(propNames)
                .includeSubEntityProperties(true)
                .excludedPropNames(excluded);

        assertEquals(String.class, selection.entityClass());
        assertEquals("t", selection.tableAlias());
        assertEquals("alias", selection.classAlias());
        assertEquals(propNames, selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excluded, selection.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_Create() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        assertNotNull(builder);
    }

    @Test
    public void testMultiSelectionBuilder_AddSimple() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithPropNames() {
        List<String> propNames = Arrays.asList("id", "name");

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, propNames).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals(propNames, selections.get(0).selectPropNames());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithAliases() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias").build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithFull() {
        List<String> propNames = Arrays.asList("id", "name");

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", propNames).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertEquals(propNames, selections.get(0).selectPropNames());
        assertFalse(selections.get(0).includeSubEntityProperties());
        assertNull(selections.get(0).excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithSubEntity() {
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, true, excluded).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertEquals(excluded, selections.get(0).excludedPropNames());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithAllOptions() {
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", true, excluded).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertEquals(excluded, selections.get(0).excludedPropNames());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_MultipleAdds() {
        List<String> propNames1 = Arrays.asList("id", "name");
        List<String> propNames2 = Arrays.asList("orderId", "orderDate");

        List<Selection> selections = Selection.multiSelectionBuilder()
                .add(String.class, "u", "user", propNames1)
                .add(Integer.class, "o", "order", propNames2)
                .add(Double.class)
                .build();

        assertEquals(3, selections.size());

        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("u", selections.get(0).tableAlias());
        assertEquals("user", selections.get(0).classAlias());
        assertEquals(propNames1, selections.get(0).selectPropNames());

        assertEquals(Integer.class, selections.get(1).entityClass());
        assertEquals("o", selections.get(1).tableAlias());
        assertEquals("order", selections.get(1).classAlias());
        assertEquals(propNames2, selections.get(1).selectPropNames());

        assertEquals(Double.class, selections.get(2).entityClass());
        assertNull(selections.get(2).tableAlias());
    }

    @Test
    public void testMultiSelectionBuilder_Build() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class).add(Integer.class).build();

        assertNotNull(selections);
        assertEquals(2, selections.size());
    }

    @Test
    public void testMultiSelectionBuilder_EmptyBuild() {
        List<Selection> selections = Selection.multiSelectionBuilder().build();

        assertNotNull(selections);
        assertEquals(0, selections.size());
    }

    @Test
    public void testMultiSelectionBuilder_Chaining() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        Selection.MultiSelectionBuilder result = builder.add(String.class);

        assertEquals(builder, result);
    }

    @Test
    public void testIncludeSubEntityPropertiesDefault() {
        Selection selection = new Selection();
        assertFalse(selection.includeSubEntityProperties());
    }

    @Test
    public void testIncludeSubEntityPropertiesTrue() {
        Selection selection = new Selection();
        selection.includeSubEntityProperties(true);
        assertTrue(selection.includeSubEntityProperties());
    }

    @Test
    public void testIncludeSubEntityPropertiesFalse() {
        Selection selection = new Selection();
        selection.includeSubEntityProperties(false);
        assertFalse(selection.includeSubEntityProperties());
    }

    @Test
    public void testSettersReturnSelf() {
        Selection selection = new Selection();
        Selection result;

        result = selection.entityClass(String.class);
        assertEquals(selection, result);

        result = selection.tableAlias("t");
        assertEquals(selection, result);

        result = selection.classAlias("alias");
        assertEquals(selection, result);

        result = selection.selectPropNames(Arrays.asList("id"));
        assertEquals(selection, result);

        result = selection.includeSubEntityProperties(true);
        assertEquals(selection, result);

        result = selection.excludedPropNames(new HashSet<>());
        assertEquals(selection, result);
    }

    @Test
    public void testNullValues() {
        Selection selection = new Selection().entityClass(null).tableAlias(null).classAlias(null).selectPropNames(null).excludedPropNames(null);

        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertNull(selection.excludedPropNames());
    }
}
