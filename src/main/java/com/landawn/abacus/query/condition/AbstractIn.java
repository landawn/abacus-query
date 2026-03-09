/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Abstract base class for IN and NOT IN conditions in SQL queries.
 * This class provides all shared implementation for collection membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link In} and {@link NotIn} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, and methods
 * for parameters, copying, string rendering, hashing, and equality are identical.</p>
 *
 * @see In
 * @see NotIn
 * @see AbstractCondition
 */
public abstract class AbstractIn extends ComposableCondition {

    // For Kryo
    final String propName;

    private List<?> values;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractIn() {
        propName = null;
    }

    /**
     * Creates a new IN or NOT IN condition.
     *
     * @param propName the property/column name (must not be null or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param values the collection of values to check against (must not be null or empty)
     * @throws IllegalArgumentException if propName is null/empty or values is null/empty
     */
    protected AbstractIn(final String propName, final Operator operator, final Collection<?> values) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotEmpty(values, "values");

        this.propName = propName;
        this.values = new ArrayList<>(values);
    }

    /**
     * Gets the property name being checked in this IN or NOT IN condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * String prop = inCond.getPropName();   // "status"
     * }</pre>
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the values used by this IN or NOT IN condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * List<?> values = inCond.getValues();   // ["active", "pending"]
     * }</pre>
     *
     * @return an unmodifiable view of the values list, or {@code null} for an uninitialized instance
     */
    public List<?> getValues() { //NOSONAR
        return values == null ? null : Collections.unmodifiableList(values);
    }

    /**
     * Gets the parameter values for this condition.
     *
     * @return an immutable list of parameter values, or an empty immutable list if no values are set
     */
    @Override
    public ImmutableList<Object> getParameters() {
        if (values == null) {
            return ImmutableList.empty();
        }

        final List<Object> parameters = new ArrayList<>(values.size());

        for (final Object value : values) {
            if (value instanceof Condition) {
                parameters.addAll(((Condition) value).getParameters());
            } else {
                parameters.add(value);
            }
        }

        return ImmutableList.wrap(parameters);
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void clearParameters() {
        if (N.notEmpty(values)) {
            for (int i = 0, size = values.size(); i < size; i++) {
                final Object value = values.get(i);

                if (value instanceof Condition) {
                    ((Condition) value).clearParameters();
                } else {
                    ((List<Object>) values).set(i, null);
                }
            }
        }
    }

    /**
     * Converts this condition to its string representation.
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return the string representation, e.g., "status IN ('active', 'pending')"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final StringBuilder sb = new StringBuilder();
        sb.append(effectiveNamingPolicy.convert(propName)).append(SK._SPACE).append(operator().toString()).append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(SK.COMMA_SPACE);
                }
                sb.append(formatParameter(values.get(i), effectiveNamingPolicy));
            }
        }

        sb.append(SK._PARENTHESIS_R);
        return sb.toString();
    }

    /**
     * Generates the hash code for this condition.
     *
     * @return the hash code based on property name, operator, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the same property name,
     * operator, and values list.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final AbstractIn other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}
