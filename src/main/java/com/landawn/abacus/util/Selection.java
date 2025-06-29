/*
 * Copyright (C) 2022 HaiYang Li
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
package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.SQLBuilder.NSC;
import com.landawn.abacus.util.SQLBuilder.PSC;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Represents a selection specification for SQL queries, particularly useful for complex multi-table selections.
 * This class encapsulates information about which entity fields to select, table aliases, and property filtering.
 * 
 * <p>The Selection class is designed to work with SQLBuilder to generate SELECT clauses with support for:</p>
 * <ul>
 *   <li>Entity class mapping</li>
 *   <li>Table aliasing</li>
 *   <li>Class aliasing for result mapping</li>
 *   <li>Selective property inclusion</li>
 *   <li>Sub-entity property handling</li>
 *   <li>Property exclusion</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple selection
 * Selection userSelection = new Selection()
 *     .entityClass(User.class)
 *     .selectPropNames(Arrays.asList("id", "name", "email"));
 * 
 * // Selection with aliases
 * Selection orderSelection = new Selection()
 *     .entityClass(Order.class)
 *     .tableAlias("o")
 *     .classAlias("order")
 *     .includeSubEntityProperties(true)
 *     .excludedPropNames(Set.of("internalNotes"));
 * 
 * // Multi-table selection using builder
 * List<Selection> selections = Selection.multiSelectionBuilder()
 *     .add(User.class, "u", "user", Arrays.asList("id", "name"))
 *     .add(Order.class, "o", "order")
 *     .add(Product.class, "p", "product", true, Set.of("cost"))
 *     .build();
 * }</pre>
 */
@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public final class Selection {
    private Class<?> entityClass;
    private String tableAlias;
    private String classAlias;
    private Collection<String> selectPropNames;
    boolean includeSubEntityProperties;
    private Set<String> excludedPropNames;

    /**
     * Creates a new MultiSelectionBuilder for building complex multi-table selections.
     * This builder provides a fluent API for constructing multiple Selection objects.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * SQLBuilder sqlBuilder = Selection.multiSelectionBuilder()
     *     .add(User.class, "u", "user")
     *     .add(Order.class, "o", "order", Arrays.asList("id", "orderDate", "total"))
     *     .apply(PSC::selectFrom);
     * }</pre>
     *
     * @return a new MultiSelectionBuilder instance
     */
    public static MultiSelectionBuilder multiSelectionBuilder() {
        return new MultiSelectionBuilder();
    }

    /**
     * Builder class for creating multiple Selection objects in a fluent manner.
     * This builder is particularly useful when constructing complex queries involving multiple tables.
     * 
     * <p>The builder supports various overloaded add() methods for different selection scenarios
     * and can be directly applied to SQLBuilder methods using the apply() method.</p>
     */
    public static final class MultiSelectionBuilder {
        private final List<Selection> selections = new ArrayList<>();

        private MultiSelectionBuilder() {
            //
        }

        /**
         * Adds a simple selection for the specified entity class with default settings.
         * All properties will be selected without any aliases.
         *
         * @param entityClass the entity class to select from
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass) {
            return add(entityClass, null, null, null);
        }

        /**
         * Adds a selection for the specified entity class with specific properties to select.
         *
         * @param entityClass the entity class to select from
         * @param selectPropNames the property names to include in the selection
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final Collection<String> selectPropNames) {
            return add(entityClass, null, null, selectPropNames);
        }

        /**
         * Adds a selection for the specified entity class with table and class aliases.
         * All properties will be selected.
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL
         * @param classAlias the alias to use for result mapping
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias) {
            return add(entityClass, tableAlias, classAlias, null);
        }

        /**
         * Adds a selection for the specified entity class with full configuration options.
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be null)
         * @param classAlias the alias to use for result mapping (can be null)
         * @param selectPropNames the property names to include in the selection (null means all)
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias,
                final Collection<String> selectPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, selectPropNames, false, null));

            return this;
        }

        /**
         * Adds a selection with sub-entity property control and exclusion options.
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames property names to exclude from the selection
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return add(entityClass, null, null, includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Adds a selection with full configuration including sub-entity and exclusion options.
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be null)
         * @param classAlias the alias to use for result mapping (can be null)
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames property names to exclude from the selection
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, null, includeSubEntityProperties, excludedPropNames));

            return this;
        }

        /**
         * Builds and returns the list of Selection objects configured in this builder.
         *
         * @return an unmodifiable list of Selection objects
         */
        public List<Selection> build() {
            return selections;
        }

        /**
         * Applies the built selections to a SQLBuilder function and returns the resulting SQLBuilder.
         * This method provides a convenient way to integrate the selections with SQLBuilder methods.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * SQLBuilder query = Selection.multiSelectionBuilder()
         *     .add(User.class, "u", "user")
         *     .add(Order.class, "o", "order")
         *     .apply(PSC::selectFrom);
         * }</pre>
         *
         * @param func the function to apply the selections to (e.g., PSC::select, NSC::selectFrom)
         * @return the SQLBuilder instance returned by the function
         * @see PSC#select(List)
         * @see PSC#selectFrom(List)
         * @see NSC#select(List)
         * @see NSC#selectFrom(List)
         */
        @Beta
        public SQLBuilder apply(final Function<? super List<Selection>, SQLBuilder> func) {
            return func.apply(build());
        }
    }
}