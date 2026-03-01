/*
 * Copyright (C) 2020 HaiYang Li
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
import java.util.function.Function;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for IN and NOT IN subquery conditions in SQL queries.
 * This class provides all shared implementation for subquery membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link InSubQuery} and {@link NotInSubQuery} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, setters, and methods
 * for parameters, copying, string rendering, hashing, and equality are identical.</p>
 *
 * @see InSubQuery
 * @see NotInSubQuery
 * @see SubQuery
 * @see AbstractCondition
 */
public abstract class AbstractInSubQuery extends AbstractCondition {

    // For Kryo
    final Collection<String> propNames;

    private SubQuery subQuery;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractInSubQuery() {
        propNames = Collections.emptyList();
    }

    /**
     * Creates a condition for a single property.
     *
     * @param propName the property/column name (must not be null or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be null)
     * @throws IllegalArgumentException if propName is null/empty or subQuery is null
     */
    protected AbstractInSubQuery(final String propName, final Operator operator, final SubQuery subQuery) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = Collections.singletonList(propName);
        this.subQuery = subQuery;
    }

    /**
     * Creates a condition for multiple properties.
     *
     * @param propNames the property/column names (must not be null or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be null)
     * @throws IllegalArgumentException if propNames is null/empty, any element is null/empty, or subQuery is null
     */
    protected AbstractInSubQuery(final Collection<String> propNames, final Operator operator, final SubQuery subQuery) {
        super(operator);

        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = copyAndValidatePropNames(propNames);
        this.subQuery = subQuery;
    }

    /**
     * Gets the property names for this condition.
     *
     * @return non-null immutable collection of property names
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    private static Collection<String> copyAndValidatePropNames(final Collection<String> propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        final List<String> copy = new ArrayList<>(propNames.size());

        for (final String propName : propNames) {
            N.checkArgNotEmpty(propName, "Property name in propNames");
            copy.add(propName);
        }

        return Collections.unmodifiableList(copy);
    }

    /**
     * Gets the subquery used in this condition.
     *
     * @return the subquery
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets a new subquery for this condition.
     *
     * @param subQuery the new subquery (must not be null)
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new instance instead.
     */
    @Deprecated
    public void setSubQuery(final SubQuery subQuery) {
        N.checkArgNotNull(subQuery, "subQuery");

        this.subQuery = subQuery;
    }

    /**
     * Gets the list of parameters from the subquery.
     *
     * @return list of parameter values from the subquery
     */
    @Override
    public List<Object> getParameters() {
        return subQuery == null ? N.emptyList() : subQuery.getParameters();
    }

    /**
     * Clears parameters in the underlying subquery.
     */
    @Override
    public void clearParameters() {
        if (subQuery != null) {
            subQuery.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this condition.
     *
     * @param <T> the type of condition to return
     * @return a new instance with a deep copy of the subquery
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final AbstractInSubQuery copy = super.copy();

        if (subQuery != null) {
            copy.subQuery = subQuery.copy();
        }

        return (T) copy;
    }

    /**
     * Generates the hash code for this condition.
     *
     * @return hash code based on property name(s), operator, and subquery
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + N.hashCode(propNames);
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());
    }

    /**
     * Checks if this condition is equal to another object.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final AbstractInSubQuery other) {
            return N.equals(propNames, other.propNames) && N.equals(operator, other.operator) && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    /**
     * Converts this condition to its string representation.
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return the string representation of the condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final String subQueryString = subQuery == null ? Strings.EMPTY : subQuery.toString(effectiveNamingPolicy);

        if (N.notEmpty(propNames)) {
            if (propNames.size() == 1) {
                return effectiveNamingPolicy.convert(propNames.iterator().next()) + SK._SPACE + operator().toString() + SK.SPACE_PARENTHESIS_L + subQueryString
                        + SK.PARENTHESIS_R;
            }

            final Function<String, String> converter = effectiveNamingPolicy::convert;

            return "(" + Strings.join(N.map(propNames, converter), ", ") + ") " + operator().toString() + SK.SPACE_PARENTHESIS_L + subQueryString
                    + SK.PARENTHESIS_R;
        }

        return operator().toString() + SK.SPACE_PARENTHESIS_L + subQueryString + SK.PARENTHESIS_R;
    }
}
