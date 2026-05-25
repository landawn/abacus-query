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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for IN and NOT IN conditions in SQL queries.
 * This class provides all shared implementation for collection membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link In} and {@link NotIn} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, and methods
 * for parameters, string rendering, hashing, and equality are identical.</p>
 *
 * @see In
 * @see NotIn
 * @see ComposableCondition
 */
public abstract class AbstractIn extends ComposableCondition {

    // For Kryo
    final String propName;

    private List<?> values;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Single-slot toString cache: last naming policy and its rendered string (performance only). */
    private transient NamingPolicy cachedTostringNamingPolicy;

    private transient String cachedTostring;

    /** Lazily memoized unmodifiable view of {@link #values} (performance only). */
    private transient List<?> cachedValuesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractIn() {
        propName = null;
    }

    /**
     * Creates a new IN or NOT IN condition. The given values are copied into an internal
     * {@link ArrayList}, so later mutations to the supplied collection do not affect this
     * condition. Individual elements may be literal values or {@link Condition} instances; the
     * latter have their parameters spliced into {@link #getParameters()}.
     *
     * @param propName the property/column name (must not be {@code null} or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param values the collection of values to check membership against (must not be {@code null} or empty);
     *               elements may be {@code null}
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty or {@code values} is {@code null}/empty
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
        if (values == null) {
            return null;
        }

        List<?> view = cachedValuesView;

        if (view == null) {
            view = Collections.unmodifiableList(values);
            cachedValuesView = view;
        }

        return view;
    }

    /**
     * Gets the parameter values for this condition. Any element of {@link #getValues()} that is
     * itself a {@link Condition} has its parameters spliced into the result in place of the
     * element; non-{@code Condition} elements are included as-is.
     *
     * @return an immutable list of parameter values, or an empty immutable list if no values are set
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    private ImmutableList<Object> computeParameters() {
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
     * Converts this condition to its string representation.
     * The format is: {@code propName IN (value1, value2, ...)}
     * (or {@code NOT IN} for {@link NotIn}). If the operator is {@code null}
     * (only possible for an uninitialized instance), the literal {@code "null"} is rendered
     * in place of the operator.
     *
     * @param namingPolicy the naming policy to apply to the property name;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return the string representation, e.g., {@code "status IN ('active', 'pending')"}
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (cachedTostring != null && cachedTostringNamingPolicy == namingPolicy) {
            return cachedTostring;
        }

        final String result = doToString(namingPolicy);

        cachedTostring = result;
        cachedTostringNamingPolicy = namingPolicy;

        return result;
    }

    private String doToString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        final int size = values == null ? 0 : values.size();
        final StringBuilder sb = new StringBuilder(16 + (size << 3));
        sb.append(effectiveNamingPolicy.convert(propName)).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            for (int i = 0; i < size; i++) {
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
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + ((values == null) ? 0 : values.hashCode());

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
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
