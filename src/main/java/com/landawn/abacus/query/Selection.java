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
package com.landawn.abacus.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Represents a selection specification for SQL queries, particularly useful for complex multi-table selections.
 * This class encapsulates information about which entity fields to select, table aliases, and property filtering.
 * 
 * <p>The {@code Selection} class is designed to work with {@link SqlBuilder} to generate SELECT clauses with support for:</p>
 * <ul>
 *   <li>Entity class mapping</li>
 *   <li>Table aliasing</li>
 *   <li>Class aliasing for result mapping</li>
 *   <li>Selective property inclusion</li>
 *   <li>Sub-entity property handling</li>
 *   <li>Property exclusion</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
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
 * List<Selection> selections = Selection.builder()
 *     .add(User.class, "u", "user", Arrays.asList("id", "name"))
 *     .add(Order.class, "o", "order")
 *     .add(Product.class, "p", "product", true, Set.of("cost"))
 *     .build();
 * }</pre>
 */
@Data
@Accessors(fluent = true)
public final class Selection {

    /**
     * Creates a new empty Selection instance.
     * Use the fluent setter methods to configure the selection properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Selection selection = new Selection()
     *     .entityClass(User.class)
     *     .tableAlias("u")
     *     .selectPropNames(Arrays.asList("id", "name"));
     * }</pre>
     */
    public Selection() {
        // Default constructor for fluent API usage
    }

    private Class<?> entityClass;
    private String tableAlias;
    private String classAlias;
    private Collection<String> selectPropNames;
    private boolean includeSubEntityProperties;
    private Set<String> excludedPropNames;

    /**
     * Creates a fully populated {@code Selection} from the given values.
     *
     * <p>This all-args constructor is intentionally package-private and defensively copies the
     * {@code selectPropNames} and {@code excludedPropNames} collections so that the {@code Selection}
     * does not retain a reference to (or share mutable state with) the caller-supplied collections.
     * External code should use the no-arg constructor with the fluent setters, or
     * {@link #builder()}.</p>
     *
     * @param entityClass the entity class to select from
     * @param tableAlias the alias to use for the table in SQL (can be {@code null})
     * @param classAlias the alias to use for result mapping (can be {@code null})
     * @param selectPropNames the property names to include; copied defensively, {@code null} is preserved as {@code null}
     * @param includeSubEntityProperties whether to include properties from sub-entities
     * @param excludedPropNames the property names to exclude; copied defensively, {@code null} is preserved as {@code null}
     */
    Selection(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> selectPropNames,
            final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        this.entityClass = entityClass;
        this.tableAlias = tableAlias;
        this.classAlias = classAlias;
        this.selectPropNames = selectPropNames == null ? null : new ArrayList<>(selectPropNames);
        this.includeSubEntityProperties = includeSubEntityProperties;
        this.excludedPropNames = excludedPropNames == null ? null : new LinkedHashSet<>(excludedPropNames);
    }

    /**
     * Sets the property names to include in this selection.
     *
     * <p>The supplied collection is copied defensively, so subsequent mutations of the argument do not
     * affect this {@code Selection}. Passing {@code null} clears the selection (meaning "all properties").</p>
     *
     * @param selectPropNames the property names to include; {@code null} means all properties
     * @return this {@code Selection} instance for method chaining
     */
    public Selection selectPropNames(final Collection<String> selectPropNames) {
        this.selectPropNames = selectPropNames == null ? null : new ArrayList<>(selectPropNames);
        return this;
    }

    /**
     * Returns the property names to include in this selection.
     *
     * <p>The returned collection is an immutable view; attempts to modify it throw
     * {@link UnsupportedOperationException}. Returns {@code null} when no specific properties have been set.</p>
     *
     * @return an immutable view of the selected property names, or {@code null} if none was set
     */
    public Collection<String> selectPropNames() {
        return selectPropNames == null ? null : ImmutableList.wrap((List<String>) selectPropNames);
    }

    /**
     * Sets the property names to exclude from this selection.
     *
     * <p>The supplied set is copied defensively, so subsequent mutations of the argument do not affect
     * this {@code Selection}. Passing {@code null} clears the exclusion set.</p>
     *
     * @param excludedPropNames the property names to exclude; can be {@code null}
     * @return this {@code Selection} instance for method chaining
     */
    public Selection excludedPropNames(final Set<String> excludedPropNames) {
        this.excludedPropNames = excludedPropNames == null ? null : new LinkedHashSet<>(excludedPropNames);
        return this;
    }

    /**
     * Returns the property names to exclude from this selection.
     *
     * <p>The returned set is an immutable view; attempts to modify it throw
     * {@link UnsupportedOperationException}. Returns {@code null} when no exclusions have been set.</p>
     *
     * @return an immutable view of the excluded property names, or {@code null} if none was set
     */
    public Set<String> excludedPropNames() {
        return excludedPropNames == null ? null : ImmutableSet.wrap(excludedPropNames);
    }

    /**
     * Creates a new {@link MultiSelectionBuilder} for building complex multi-table selections.
     * This builder provides a fluent API for constructing multiple {@link Selection} objects that can be
     * used with {@link SqlBuilder} to create complex SELECT statements involving multiple tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Build multiple selections and apply to SqlBuilder
     * SqlBuilder sqlBuilder = Selection.builder()
     *     .add(User.class, "u", "user")
     *     .add(Order.class, "o", "order", Arrays.asList("id", "orderDate", "total"))
     *     .apply(Dsl.PSC::selectFrom);
     *
     * // Or build and use separately
     * List<Selection> selections = Selection.builder()
     *     .add(User.class, "u", "user")
     *     .add(Address.class, "a", "address")
     *     .build();
     * }</pre>
     *
     * @return a new MultiSelectionBuilder instance for constructing multi-table selections
     */
    public static MultiSelectionBuilder builder() {
        return new MultiSelectionBuilder();
    }

    /**
     * Builder class for creating multiple {@link Selection} objects in a fluent manner.
     * This builder is particularly useful when constructing complex queries involving multiple tables.
     *
     * <p>The builder supports various overloaded {@code add()} methods for different selection scenarios
     * and can be directly applied to {@link SqlBuilder} methods using the {@link #apply(Function)} method.</p>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class)
         *     .build();
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, Arrays.asList("id", "name", "email"))
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param selectPropNames the property names to include in the selection; {@code null} means all properties
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final Collection<String> selectPropNames) {
            return add(entityClass, null, null, selectPropNames);
        }

        /**
         * Adds a selection for the specified entity class with table and class aliases.
         * All properties will be selected.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, "u", "user")
         *     .add(Order.class, "o", "order")
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be {@code null})
         * @param classAlias the alias to use for result mapping (can be {@code null})
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias) {
            return add(entityClass, tableAlias, classAlias, null);
        }

        /**
         * Adds a selection for the specified entity class with full configuration options.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, "u", "user", Arrays.asList("id", "name"))
         *     .add(Order.class, "o", "order", Arrays.asList("orderId", "orderDate", "total"))
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be {@code null})
         * @param classAlias the alias to use for result mapping (can be {@code null})
         * @param selectPropNames the property names to include in the selection ({@code null} means all properties)
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias,
                final Collection<String> selectPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, selectPropNames == null ? null : N.newArrayList(selectPropNames), false, null));

            return this;
        }

        /**
         * Adds a selection with sub-entity property control and exclusion options.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, true, Set.of("password", "internalNotes"))
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames property names to exclude from the selection (may be {@code null})
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return add(entityClass, null, null, includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Adds a selection with full configuration including sub-entity and exclusion options.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, "u", "user", true, Set.of("password"))
         *     .add(Order.class, "o", "order", false, Set.of("internalNotes", "costPrice"))
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be {@code null})
         * @param classAlias the alias to use for result mapping (can be {@code null})
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames property names to exclude from the selection (may be {@code null})
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, null, includeSubEntityProperties,
                    excludedPropNames == null ? null : N.newHashSet(excludedPropNames)));

            return this;
        }

        /**
         * Adds a selection with the full set of configuration options, combining specific property
         * selection, sub-entity handling, and property exclusion in a single call.
         *
         * <p>This is the most complete {@code add} overload: it lets you set {@code selectPropNames},
         * {@code includeSubEntityProperties}, and {@code excludedPropNames} together. The supplied
         * collections are copied defensively into the resulting {@link Selection}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, "u", "user", Arrays.asList("id", "name", "address"), true, Set.of("password"))
         *     .build();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param tableAlias the alias to use for the table in SQL (can be {@code null})
         * @param classAlias the alias to use for result mapping (can be {@code null})
         * @param selectPropNames the property names to include in the selection ({@code null} means all properties)
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames property names to exclude from the selection (may be {@code null})
         * @return this builder instance for method chaining
         */
        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> selectPropNames,
                final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, selectPropNames == null ? null : N.newArrayList(selectPropNames),
                    includeSubEntityProperties, excludedPropNames == null ? null : N.newHashSet(excludedPropNames)));

            return this;
        }

        /**
         * Builds and returns the list of Selection objects configured in this builder.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Selection.builder()
         *     .add(User.class, "u", "user")
         *     .add(Order.class, "o", "order")
         *     .build();
         * }</pre>
         *
         * @return an unmodifiable list of Selection objects
         */
        public List<Selection> build() {
            return Collections.unmodifiableList(new ArrayList<>(selections));
        }

        /**
         * Applies the built selections to the provided SqlBuilder function and returns the resulting SqlBuilder.
         * This method provides a convenient way to integrate the selections directly with SqlBuilder methods
         * without needing to call build() explicitly.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Using with parameterized snake_case builder (PSC)
         * SqlBuilder query = Selection.builder()
         *     .add(User.class, "u", "user")
         *     .add(Order.class, "o", "order")
         *     .apply(Dsl.PSC::selectFrom);
         *
         * // Using with named-parameter snake_case builder (NSC)
         * SqlBuilder namedQuery = Selection.builder()
         *     .add(Product.class, "p", "product")
         *     .add(Category.class, "c", "category")
         *     .apply(Dsl.NSC::select);
         * }</pre>
         *
         * @param func the function to apply the built selections to (e.g., {@code Dsl.PSC::selectFrom}, {@code Dsl.NSC::select}); must not be {@code null}
         * @return the SqlBuilder instance returned by the function
         * @throws IllegalArgumentException if {@code func} is {@code null}
         * @see Dsl#select(List)
         * @see Dsl#selectFrom(List)
         */
        @Beta
        public SqlBuilder apply(final Function<? super List<Selection>, SqlBuilder> func) {
            N.checkArgNotNull(func, "func");
            return func.apply(build());
        }
    }
}
