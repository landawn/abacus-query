/*
 * Copyright (C) 2022 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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

    public static MultiSelectionBuilder multiSelectionBuilder() {
        return new MultiSelectionBuilder();
    }

    public static final class MultiSelectionBuilder {
        private final List<Selection> selections = new ArrayList<>();

        private MultiSelectionBuilder() {
            //
        }

        public MultiSelectionBuilder add(final Class<?> entityClass) {
            return add(entityClass, null, null, null);
        }

        public MultiSelectionBuilder add(final Class<?> entityClass, final Collection<String> selectPropNames) {
            return add(entityClass, null, null, selectPropNames);
        }

        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias) {
            return add(entityClass, tableAlias, classAlias, null);
        }

        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias,
                final Collection<String> selectPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, selectPropNames, false, null));

            return this;
        }

        public MultiSelectionBuilder add(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return add(entityClass, null, null, includeSubEntityProperties, excludedPropNames);
        }

        public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            selections.add(new Selection(entityClass, tableAlias, classAlias, null, includeSubEntityProperties, excludedPropNames));

            return this;
        }

        public List<Selection> build() {
            return selections;
        }
    }
}