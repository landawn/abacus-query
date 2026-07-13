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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
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
 *     .includedPropNames(Arrays.asList("id", "name", "email"));
 *
 * // Selection with aliases
 * Selection orderSelection = new Selection()
 *     .entityClass(Order.class)
 *     .tableAlias("o")
 *     .classAlias("order")
 *     .includeSubEntityProperties(true)
 *     .excludedPropNames(Set.of("internalNotes"));
 *
 * // Single selection via the generated builder
 * Selection productSelection = Selection.builder()
 *     .entityClass(Product.class)
 *     .tableAlias("p")
 *     .classAlias("product")
 *     .includeSubEntityProperties(true)
 *     .excludedPropNames(Set.of("cost"))
 *     .build();
 *
 * // Multi-table selection: build one Selection per table and collect them into a List
 * List<Selection> selections = List.of(
 *     Selection.builder().entityClass(User.class).tableAlias("u").classAlias("user")
 *         .includedPropNames(Arrays.asList("id", "name")).build(),
 *     Selection.builder().entityClass(Order.class).tableAlias("o").classAlias("order").build());
 * // ... then pass to Dsl.PSC.select(selections) / selectFrom(selections)
 * }</pre>
 *
 * <p>Note: {@code entityClass}, {@code tableAlias}, and {@code classAlias} use Lombok-generated fluent accessors.
 * The sub-entity flag uses the predicate getter {@link #includesSubEntityProperties()} and the Lombok-generated
 * fluent setter {@code includeSubEntityProperties(boolean)}. The {@code includedPropNames} and
 * {@code excludedPropNames} properties have hand-written accessors that defensively copy on write
 * and return immutable views on read.</p>
 *
 * @see Dsl#select(List)
 * @see Dsl#selectFrom(List)
 * @see SqlBuilder
 */
@Data
@Accessors(fluent = true)
public final class Selection {

    /**
     * Creates a new empty selection that can be configured through the fluent setters.
     */
    public Selection() {
        // Default constructor for fluent API usage.
    }

    private Class<?> entityClass;
    private String tableAlias;
    private String classAlias;
    private Collection<String> includedPropNames;
    @Getter(AccessLevel.NONE)
    private boolean includeSubEntityProperties;
    private Set<String> excludedPropNames;

    /**
     * Creates a fully populated {@code Selection} from the given values.
     *
     * <p>This all-args constructor is intentionally package-private and defensively copies the
     * {@code includedPropNames} and {@code excludedPropNames} collections so that the {@code Selection}
     * does not retain a reference to (or share mutable state with) the caller-supplied collections.
     * External code should use the no-arg constructor with the fluent setters, or
     * {@code builder()}.</p>
     *
     * @param entityClass the entity class to select from
     * @param tableAlias the alias to use for the table in SQL (can be {@code null})
     * @param classAlias the alias to use for result mapping (can be {@code null})
     * @param includedPropNames the property names to include; copied defensively, {@code null} is preserved as {@code null}
     * @param includeSubEntityProperties whether to include properties from sub-entities
     * @param excludedPropNames the property names to exclude; copied defensively, {@code null} is preserved as {@code null}
     */
    Selection(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> includedPropNames,
            final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        this.entityClass = entityClass;
        this.tableAlias = tableAlias;
        this.classAlias = classAlias;
        this.includedPropNames = includedPropNames == null ? null : new ArrayList<>(includedPropNames);
        this.includeSubEntityProperties = includeSubEntityProperties;
        this.excludedPropNames = excludedPropNames == null ? null : new LinkedHashSet<>(excludedPropNames);
    }

    /**
     * Returns whether properties from sub-entities are included.
     *
     * @return {@code true} if sub-entity properties are included
     */
    public boolean includesSubEntityProperties() {
        return includeSubEntityProperties;
    }

    /**
     * Sets the property names to include in this selection.
     *
     * <p>The supplied collection is copied defensively, so subsequent mutations of the argument do not
     * affect this {@code Selection}. Passing {@code null} clears the selection (meaning "all properties").</p>
     *
     * @param includedPropNames the property names to include; {@code null} means all properties
     * @return this {@code Selection} instance for method chaining
     */
    public Selection includedPropNames(final Collection<String> includedPropNames) {
        this.includedPropNames = includedPropNames == null ? null : new ArrayList<>(includedPropNames);
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
    public Collection<String> includedPropNames() {
        return includedPropNames == null ? null : ImmutableList.wrap((List<String>) includedPropNames);
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
     * Creates a builder with the required entity class already set.
     *
     * @param entityClass the entity class to select; must not be {@code null}
     * @return a new selection builder
     */
    public static SelectionBuilder builder(final Class<?> entityClass) {
        return builder().entityClass(java.util.Objects.requireNonNull(entityClass, "entityClass"));
    }

    /**
     * Creates an unconstrained selection builder for compatibility.
     *
     * @return a new selection builder
     */
    public static SelectionBuilder builder() {
        return new SelectionBuilder();
    }

    /**
     * Builder for {@link Selection} instances.
     *
     * <p>This builder is declared explicitly so it is visible consistently to Java compilation,
     * IDEs, and Javadoc generation.</p>
     */
    public static final class SelectionBuilder {
        private Class<?> entityClass;
        private String tableAlias;
        private String classAlias;
        private Collection<String> includedPropNames;
        private boolean includeSubEntityProperties;
        private Set<String> excludedPropNames;

        private SelectionBuilder() {
            // Created through Selection.builder(...).
        }

        /**
         * Sets the entity class.
         * @param entityClass the entity class
         * @return this builder
         */
        public SelectionBuilder entityClass(final Class<?> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        /**
         * Sets the table alias.
         * @param tableAlias the table alias
         * @return this builder
         */
        public SelectionBuilder tableAlias(final String tableAlias) {
            this.tableAlias = tableAlias;
            return this;
        }

        /**
         * Sets the result class alias.
         * @param classAlias the class alias
         * @return this builder
         */
        public SelectionBuilder classAlias(final String classAlias) {
            this.classAlias = classAlias;
            return this;
        }

        /**
         * Sets included property names.
         * @param includedPropNames the included names
         * @return this builder
         */
        public SelectionBuilder includedPropNames(final Collection<String> includedPropNames) {
            this.includedPropNames = includedPropNames;
            return this;
        }

        /**
         * Sets whether sub-entity properties are included.
         * @param includeSubEntityProperties the inclusion flag
         * @return this builder
         */
        public SelectionBuilder includeSubEntityProperties(final boolean includeSubEntityProperties) {
            this.includeSubEntityProperties = includeSubEntityProperties;
            return this;
        }

        /**
         * Sets excluded property names.
         * @param excludedPropNames the excluded names
         * @return this builder
         */
        public SelectionBuilder excludedPropNames(final Set<String> excludedPropNames) {
            this.excludedPropNames = excludedPropNames;
            return this;
        }

        /**
         * Builds a selection, defensively copying its property collections.
         *
         * @return the new selection
         */
        public Selection build() {
            return new Selection(entityClass, tableAlias, classAlias, includedPropNames, includeSubEntityProperties, excludedPropNames);
        }
    }
}
