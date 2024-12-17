/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.condition;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 */
public final class NamedProperty {

    private static final Map<String, NamedProperty> instancePool = new ConcurrentHashMap<>();

    // for Kryo
    final String propName;

    /**
     *
     *
     * @param propName
     */
    public NamedProperty(final String propName) {
        this.propName = N.requireNonNull(propName);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static NamedProperty of(final String propName) {
        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("the property name can't be null or empty string.");
        }

        return instancePool.computeIfAbsent(propName, NamedProperty::new);
    }

    /**
     *
     * @param values
     * @return
     */
    public Equal eq(final Object values) {
        return CF.eq(propName, values);
    }

    /**
     *
     * @param values
     * @return
     */
    @SafeVarargs
    public final Or eqOr(final Object... values) {
        final Or or = CF.or();

        for (final Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param values
     * @return
     */
    public Or eqOr(final Collection<?> values) {
        final Or or = CF.or();

        for (final Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param values
     * @return
     */
    public NotEqual ne(final Object values) {
        return CF.ne(propName, values);
    }

    /**
     *
     * @param value
     * @return
     */
    public GreaterThan gt(final Object value) {
        return CF.gt(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public GreaterEqual ge(final Object value) {
        return CF.ge(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public LessThan lt(final Object value) {
        return CF.lt(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public LessEqual le(final Object value) {
        return CF.le(propName, value);
    }

    /**
     * Checks if is null.
     *
     * @return
     */
    public IsNull isNull() {
        return CF.isNull(propName);
    }

    /**
     * Checks if is not null.
     *
     * @return
     */
    public IsNotNull isNotNull() {
        return CF.isNotNull(propName);
    }

    /**
     *
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    public Between between(final Object minValue, final Object maxValue) {
        return CF.between(propName, minValue, maxValue);
    }

    /**
     *
     * @param minValue
     * @param maxValue
     * @return
     * @deprecated please use {@link #between(Object, Object)}
     */
    @Deprecated
    public Between bt(final Object minValue, final Object maxValue) {
        return CF.between(propName, minValue, maxValue);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like like(final Object value) {
        return CF.like(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public NotLike notLike(final Object value) {
        return CF.notLike(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like startsWith(final Object value) {
        return CF.startsWith(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like endsWith(final Object value) {
        return CF.endsWith(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like contains(final Object value) {
        return CF.contains(propName, value);
    }

    /**
     *
     * @param values
     * @return
     */
    @SafeVarargs
    public final In in(final Object... values) {
        return CF.in(propName, values);
    }

    /**
     *
     * @param values
     * @return
     */
    public In in(final Collection<?> values) {
        return CF.in(propName, values);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return propName.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof NamedProperty && N.equals(((NamedProperty) obj).propName, propName));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return propName;
    }
}
