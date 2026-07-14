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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;

/**
 * Immutable selection specification for SQL queries, particularly useful for complex multi-table selections.
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
 * Selection userSelection = Selection.builder(User.class)
 *     .includedPropNames(Arrays.asList("id", "name", "email"))
 *     .build();
 *
 * // Selection with aliases
 * Selection orderSelection = Selection.builder(Order.class)
 *     .tableAlias("o")
 *     .classAlias("order")
 *     .includeSubEntityProperties(true)
 *     .excludedPropNames(Set.of("internalNotes"))
 *     .build();
 *
 * // Multi-table selection: build one Selection per table and collect them into a List
 * List<Selection> selections = List.of(
 *     Selection.builder(User.class).tableAlias("u").classAlias("user")
 *         .includedPropNames(Arrays.asList("id", "name")).build(),
 *     Selection.builder(Order.class).tableAlias("o").classAlias("order").build());
 * // ... then pass to Dsl.PSC.select(selections) / selectFrom(selections)
 * }</pre>
 *
 * <p>The entity class is required when the builder is created, so every built instance is complete.
 * Property collections are defensively copied during construction and exposed as immutable collections.
 * Instances are therefore safe to share between threads.</p>
 *
 * @see Dsl#select(java.util.List)
 * @see Dsl#selectFrom(java.util.List)
 * @see SqlBuilder
 */
public final class Selection {

    private final Class<?> entityClass;
    private final String tableAlias;
    private final String classAlias;
    private final ImmutableList<String> includedPropNames;
    private final boolean includeSubEntityProperties;
    private final ImmutableSet<String> excludedPropNames;

    private Selection(final SelectionBuilder builder) {
        entityClass = builder.entityClass;
        tableAlias = builder.tableAlias;
        classAlias = builder.classAlias;
        includedPropNames = builder.includedPropNames == null ? null : ImmutableList.copyOf(builder.includedPropNames);
        includeSubEntityProperties = builder.includeSubEntityProperties;
        excludedPropNames = builder.excludedPropNames == null ? null : ImmutableSet.copyOf(builder.excludedPropNames);
    }

    /**
     * Returns the entity class to select.
     *
     * @return the entity class, never {@code null}
     */
    public Class<?> entityClass() {
        return entityClass;
    }

    /**
     * Returns the table alias.
     *
     * @return the table alias, or {@code null} if none was specified
     */
    public String tableAlias() {
        return tableAlias;
    }

    /**
     * Returns the result class alias.
     *
     * @return the class alias, or {@code null} if none was specified
     */
    public String classAlias() {
        return classAlias;
    }

    /**
     * Returns the property names to include in this selection.
     *
     * @return the immutable included property names, or {@code null} if all properties are included
     */
    public Collection<String> includedPropNames() {
        return includedPropNames;
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
     * Returns the property names to exclude from this selection.
     *
     * @return the immutable excluded property names, or {@code null} if no properties are excluded
     */
    public Set<String> excludedPropNames() {
        return excludedPropNames;
    }

    /**
     * Creates a builder with the required entity class.
     *
     * @param entityClass the entity class to select; must not be {@code null}
     * @return a new selection builder
     * @throws NullPointerException if {@code entityClass} is {@code null}
     */
    public static SelectionBuilder builder(final Class<?> entityClass) {
        return new SelectionBuilder(Objects.requireNonNull(entityClass, "entityClass"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, tableAlias, classAlias, includedPropNames, includeSubEntityProperties, excludedPropNames);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof final Selection other)) {
            return false;
        }

        return includeSubEntityProperties == other.includeSubEntityProperties && Objects.equals(entityClass, other.entityClass)
                && Objects.equals(tableAlias, other.tableAlias) && Objects.equals(classAlias, other.classAlias)
                && Objects.equals(includedPropNames, other.includedPropNames) && Objects.equals(excludedPropNames, other.excludedPropNames);
    }

    @Override
    public String toString() {
        return "Selection(entityClass=" + entityClass + ", tableAlias=" + tableAlias + ", classAlias=" + classAlias + ", includedPropNames=" + includedPropNames
                + ", includeSubEntityProperties=" + includeSubEntityProperties + ", excludedPropNames=" + excludedPropNames + ')';
    }

    /**
     * Builder for immutable {@link Selection} instances.
     */
    public static final class SelectionBuilder {
        private final Class<?> entityClass;
        private String tableAlias;
        private String classAlias;
        private Collection<String> includedPropNames;
        private boolean includeSubEntityProperties;
        private Set<String> excludedPropNames;

        private SelectionBuilder(final Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        /**
         * Sets the table alias.
         *
         * @param tableAlias the table alias
         * @return this builder
         */
        public SelectionBuilder tableAlias(final String tableAlias) {
            this.tableAlias = tableAlias;
            return this;
        }

        /**
         * Sets the result class alias.
         *
         * @param classAlias the class alias
         * @return this builder
         */
        public SelectionBuilder classAlias(final String classAlias) {
            this.classAlias = classAlias;
            return this;
        }

        /**
         * Sets the property names to include. Passing {@code null} means all properties.
         *
         * @param includedPropNames the included property names
         * @return this builder
         */
        public SelectionBuilder includedPropNames(final Collection<String> includedPropNames) {
            this.includedPropNames = includedPropNames;
            return this;
        }

        /**
         * Sets whether properties from sub-entities are included.
         *
         * @param includeSubEntityProperties the inclusion flag
         * @return this builder
         */
        public SelectionBuilder includeSubEntityProperties(final boolean includeSubEntityProperties) {
            this.includeSubEntityProperties = includeSubEntityProperties;
            return this;
        }

        /**
         * Sets the property names to exclude. Passing {@code null} means no exclusions.
         *
         * @param excludedPropNames the excluded property names
         * @return this builder
         */
        public SelectionBuilder excludedPropNames(final Set<String> excludedPropNames) {
            this.excludedPropNames = excludedPropNames;
            return this;
        }

        /**
         * Builds an immutable selection, defensively copying its property collections.
         *
         * @return the new selection
         */
        public Selection build() {
            return new Selection(this);
        }
    }
}
