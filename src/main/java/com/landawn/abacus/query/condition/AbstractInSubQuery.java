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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for IN and NOT IN subquery conditions in SQL queries.
 * This class provides all shared implementation for subquery membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link InSubQuery} and {@link NotInSubQuery} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, and methods
 * for parameters, string rendering, hashing, and equality are identical.</p>
 *
 * @see InSubQuery
 * @see NotInSubQuery
 * @see SubQuery
 * @see ComposableCondition
 */
public abstract class AbstractInSubQuery extends ComposableCondition {

    // For Kryo
    final Collection<String> propNames;

    private SubQuery subQuery;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Single-slot toString cache: last naming policy and its rendered string (performance only). */
    private transient NamingPolicy cachedTostringNamingPolicy;

    private transient String cachedTostring;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractInSubQuery() {
        propNames = Collections.emptyList();
    }

    /**
     * Creates a condition for a single property.
     *
     * <p>If the subquery is a structured subquery (i.e. it exposes selected property names via
     * {@link SubQuery#getSelectPropNames()}), it must select exactly one column. Raw SQL subqueries
     * are not validated for column arity.</p>
     *
     * @param propName the property/column name (must not be {@code null} or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be {@code null})
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty, if {@code subQuery} is
     *             {@code null}, or if the subquery is structured and selects a number of columns other than 1
     */
    protected AbstractInSubQuery(final String propName, final Operator operator, final SubQuery subQuery) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = Collections.singletonList(propName);
        validateSubQuerySelectArity(this.propNames, subQuery);
        this.subQuery = subQuery;
    }

    /**
     * Creates a condition for multiple properties.
     *
     * <p>If the subquery is a structured subquery (i.e. it exposes selected property names via
     * {@link SubQuery#getSelectPropNames()}), the number of selected columns must match
     * {@code propNames.size()}. Raw SQL subqueries are not validated for column arity.</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be {@code null})
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty, if any element is
     *             {@code null}/empty, if {@code subQuery} is {@code null}, or if the subquery is structured and
     *             its number of selected columns does not match {@code propNames.size()}
     */
    protected AbstractInSubQuery(final Collection<String> propNames, final Operator operator, final SubQuery subQuery) {
        super(operator);

        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = copyAndValidatePropNames(propNames);
        validateSubQuerySelectArity(this.propNames, subQuery);
        this.subQuery = subQuery;
    }

    /**
     * Gets the property names for this IN or NOT IN subquery condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("dept_id", subQuery);
     * Collection<String> props = inSub.getPropNames();   // ["dept_id"]
     * }</pre>
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
     * Gets the subquery used in this IN or NOT IN subquery condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("dept_id", subQuery);
     * SubQuery sq = inSub.getSubQuery();   // the subquery instance
     * }</pre>
     *
     * @return the subquery, or {@code null} for an uninitialized instance
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    private static void validateSubQuerySelectArity(final Collection<String> propNames, final SubQuery subQuery) {
        final Collection<String> subQuerySelectPropNames = subQuery.getSelectPropNames();

        if (subQuerySelectPropNames != null && subQuerySelectPropNames.size() != propNames.size()) {
            throw new IllegalArgumentException("The number of selected properties in subQuery (" + subQuerySelectPropNames.size()
                    + ") must match the number of left-hand properties (" + propNames.size() + ")");
        }
    }

    /**
     * Gets the list of parameters from the subquery.
     *
     * @return an immutable list of parameter values from the subquery; an empty immutable list
     *         if the subquery is {@code null} (only possible for an uninitialized instance)
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = subQuery == null ? ImmutableList.empty() : subQuery.getParameters();
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Generates the hash code for this condition.
     *
     * @return hash code based on property name(s), operator, and subquery
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + N.hashCode(propNames);
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the same property names,
     * operator, and subquery.
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
     * <p>For a single property: {@code propName IN (subQuery)}</p>
     * <p>For multiple properties: {@code (prop1, prop2, ...) IN (subQuery)}</p>
     * <p>The rendered operator ({@code IN} or {@code NOT IN}) reflects this condition's
     * {@link Operator}; {@code IN} above is shown only as an example.</p>
     * <p>If {@code propNames} is empty (only possible for an uninitialized instance), only
     * {@code OPERATOR (subQuery)} is rendered, and the operator falls back to the literal
     * {@code "null"} when {@code operator} is also {@code null}.</p>
     *
     * @param namingPolicy the naming policy to apply to property names;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return the string representation of the condition
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
        final String subQueryString = subQuery == null ? Strings.EMPTY : subQuery.toString(effectiveNamingPolicy);
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        if (N.notEmpty(propNames)) {
            final int size = propNames.size();

            if (size == 1) {
                final String singleProp = (propNames instanceof List) ? ((List<String>) propNames).get(0) : propNames.iterator().next();

                return effectiveNamingPolicy.convert(singleProp) + SK._SPACE + opStr + SK.SPACE_PARENTHESIS_L + subQueryString + SK.PARENTHESIS_R;
            }

            final StringBuilder sb = new StringBuilder(16 + (size << 4) + subQueryString.length());
            sb.append('(');

            int i = 0;
            for (final String propName : propNames) {
                if (i++ > 0) {
                    sb.append(", ");
                }

                sb.append(effectiveNamingPolicy.convert(propName));
            }

            sb.append(") ").append(opStr).append(SK.SPACE_PARENTHESIS_L).append(subQueryString).append(SK.PARENTHESIS_R);

            return sb.toString();
        }

        return opStr + SK.SPACE_PARENTHESIS_L + subQueryString + SK.PARENTHESIS_R;
    }
}
