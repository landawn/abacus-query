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
    final ImmutableList<String> propNames;

    private SubQuery subQuery;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractInSubQuery() {
        propNames = ImmutableList.empty();
    }

    /**
     * Creates a condition for a single property.
     *
     * <p><b>&#9888;&#65039;</b> If the subquery is a structured subquery with an explicit projection (i.e. it exposes
     * selected property names via {@link SubQuery#selectPropNames()} and none is {@code *} or
     * {@code qualifier.*}), it must select exactly one column. Raw SQL and wildcard projections
     * are not validated for column arity.</p>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be {@code null})
     * @throws IllegalArgumentException if {@code operator} is neither {@link Operator#IN} nor {@link Operator#NOT_IN},
     *             if {@code propName} is {@code null}, empty, or blank, if {@code subQuery} is
     *             {@code null}, or if the subquery has an explicit structured projection with a number of columns other than 1
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected AbstractInSubQuery(final String propName, final Operator operator, final SubQuery subQuery) {
        super(validateOperator(operator));

        checkPropName(propName);
        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = ImmutableList.wrap(Collections.singletonList(propName));
        validateSubQuerySelectArity(this.propNames.size(), subQuery);
        this.subQuery = subQuery;
    }

    /**
     * Creates a condition for multiple properties.
     *
     * <p><b>&#9888;&#65039;</b> If the subquery is a structured subquery with an explicit projection (i.e. it
     * exposes selected property names via {@link SubQuery#selectPropNames()} and none is {@code *} or
     * {@code qualifier.*}), the number of selected columns must match {@code propNames.size()}.
     * Raw SQL and wildcard projections cannot be validated for column arity and are left to the database.</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain {@code null}, empty, or blank elements)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param subQuery the subquery (must not be {@code null})
     * @throws IllegalArgumentException if {@code operator} is neither {@link Operator#IN} nor {@link Operator#NOT_IN},
     *             if {@code propNames} is {@code null}/empty, if any element is
     *             {@code null}, empty, or blank, if {@code subQuery} is {@code null}, or if the subquery has an explicit
     *             structured projection whose number of selected columns does not match {@code propNames.size()}
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected AbstractInSubQuery(final Collection<String> propNames, final Operator operator, final SubQuery subQuery) {
        super(validateOperator(operator));

        this.propNames = copyAndValidatePropNames(propNames);
        N.checkArgNotNull(subQuery, "subQuery");
        validateSubQuerySelectArity(this.propNames.size(), subQuery);
        this.subQuery = subQuery;
    }

    private static Operator validateOperator(final Operator operator) {
        if (operator == null) {
            throw new NullPointerException("operator");
        }

        if (operator != Operator.IN && operator != Operator.NOT_IN) {
            throw new IllegalArgumentException("Only IN and NOT_IN are supported: " + operator);
        }

        return operator;
    }

    /**
     * Returns the property name being checked in this IN or NOT IN condition. For a multi-column
     * condition this returns the first property name; prefer {@link #propNames()} in that case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("dept_id", subQuery);
     * String prop = inSub.propName();   // "dept_id"
     * }</pre>
     *
     * @return the (first) property name, or {@code null} for an uninitialized instance
     */
    public String propName() {
        return N.firstOrNullIfEmpty(propNames);
    }

    /**
     * Returns the property names for this IN or NOT IN subquery condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("dept_id", subQuery);
     * Collection<String> props = inSub.propNames();   // ["dept_id"]
     * }</pre>
     *
     * @return non-null immutable collection of property names
     */
    public ImmutableList<String> propNames() {
        return propNames;
    }

    private static ImmutableList<String> copyAndValidatePropNames(final Collection<String> propNames) {
        N.checkArgNotNull(propNames, "propNames");

        final List<String> copy = new ArrayList<>(propNames);
        N.checkArgNotEmpty(copy, "propNames");

        for (final String propName : copy) {
            checkPropName(propName);
        }

        return ImmutableList.wrap(copy);
    }

    /**
     * Returns the subquery used in this IN or NOT IN subquery condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("dept_id", subQuery);
     * SubQuery sq = inSub.subQuery();   // the subquery instance
     * }</pre>
     *
     * @return the subquery, or {@code null} for an uninitialized instance
     */
    public SubQuery subQuery() {
        return subQuery;
    }

    /**
     * Validates the arity of a structured subquery when its projection is known. Raw SQL and
     * wildcard projections are deliberately left unchecked because their result shape cannot be
     * determined reliably here. This helper is also used by quantified {@code ALL}/{@code ANY}/
     * {@code SOME} operands, all of which require a one-column subquery.
     *
     * @param expectedArity the number of columns required by the enclosing SQL construct
     * @param subQuery the subquery whose explicit structured projection is inspected
     * @throws IllegalArgumentException if a known projection has a different number of columns
     */
    static void validateSubQuerySelectArity(final int expectedArity, final SubQuery subQuery) {
        final Collection<String> subQuerySelectPropNames = subQuery.selectPropNames();

        if (subQuerySelectPropNames != null && !hasWildcardProjection(subQuerySelectPropNames) && subQuerySelectPropNames.size() != expectedArity) {
            throw new IllegalArgumentException("The number of selected properties in subQuery (" + subQuerySelectPropNames.size()
                    + ") must match the required arity (" + expectedArity + ")");
        }
    }

    private static boolean hasWildcardProjection(final Collection<String> selectPropNames) {
        for (final String selectPropName : selectPropNames) {
            final String trimmed = selectPropName.trim();

            if ("*".equals(trimmed) || trimmed.endsWith(".*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the list of parameters from the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Raw SQL subquery has no bind parameters -> empty list
     * SubQuery raw = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("deptId", raw);
     * List<Object> p1 = inSub.parameters();   // [] (empty)
     *
     * // Structured subquery with a parameterized condition -> subquery's params
     * SubQuery structured = Filters.subQuery("departments", Arrays.asList("id"), Filters.eq("active", true));
     * InSubQuery inSub2 = new InSubQuery("deptId", structured);
     * List<Object> p2 = inSub2.parameters();   // [true]
     * }</pre>
     *
     * @return an immutable list of parameter values from the subquery; an empty immutable list
     *         if the subquery is {@code null} (only possible for an uninitialized instance)
     */
    @Override
    public ImmutableList<Object> parameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = subQuery == null ? ImmutableList.empty() : subQuery.parameters();
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Generates the hash code for this condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments");
     * InSubQuery a = new InSubQuery("deptId", subQuery);
     * InSubQuery b = new InSubQuery("deptId", subQuery);
     * boolean same = a.hashCode() == b.hashCode();   // true
     *
     * // Different property -> different hash codes
     * InSubQuery c = new InSubQuery("teamId", subQuery);
     * boolean diff = a.hashCode() == c.hashCode();   // false
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments");
     * InSubQuery a = new InSubQuery("deptId", subQuery);
     * InSubQuery b = new InSubQuery("deptId", subQuery);
     * boolean eq = a.equals(b);   // true
     *
     * // Different property -> not equal
     * boolean neProp = a.equals(new InSubQuery("teamId", subQuery));   // false
     *
     * // Different operator (IN vs NOT IN) -> not equal
     * boolean neOp = a.equals(new NotInSubQuery("deptId", subQuery));   // false
     *
     * // Non-AbstractInSubQuery object -> not equal
     * boolean neType = a.equals("deptId");   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final AbstractInSubQuery other = (AbstractInSubQuery) obj;
        return N.equals(propNames, other.propNames) && N.equals(operator, other.operator) && N.equals(subQuery, other.subQuery);
    }

    /**
     * Converts this condition to its SQL representation.
     *
     * <p>For a single property: {@code propName IN (subQuery)}</p>
     * <p>For multiple properties: {@code (prop1, prop2, ...) IN (subQuery)}</p>
     * <p>The rendered operator ({@code IN} or {@code NOT IN}) reflects this condition's
     * {@link Operator}; {@code IN} above is shown only as an example.</p>
     * <p>If {@code propNames} is empty (only possible for an uninitialized instance), only
     * {@code OPERATOR (subQuery)} is rendered, and the operator falls back to the literal
     * {@code "null"} when {@code operator} is also {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single property -> propName IN (subQuery)
     * SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
     * InSubQuery inSub = new InSubQuery("deptId", subQuery);
     * String s1 = inSub.toSql(NamingPolicy.NO_CHANGE);
     * // "deptId IN (SELECT id FROM departments WHERE active = true)"
     *
     * // NotInSubQuery uses the NOT IN operator
     * NotInSubQuery notInSub = new NotInSubQuery("deptId", subQuery);
     * String s2 = notInSub.toSql(NamingPolicy.NO_CHANGE);
     * // "deptId NOT IN (SELECT id FROM departments WHERE active = true)"
     *
     * // Multiple properties -> (prop1, prop2) IN (subQuery)
     * SubQuery multi = new SubQuery("SELECT firstName, lastName FROM employees");
     * InSubQuery inMulti = new InSubQuery(Arrays.asList("firstName", "lastName"), multi);
     * String s3 = inMulti.toSql(null);   // null naming policy uses NO_CHANGE
     * // "(firstName, lastName) IN (SELECT firstName, lastName FROM employees)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to property names;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return the SQL representation of the condition
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final String subQueryString = subQuery == null ? Strings.EMPTY : subQuery.toSql(effectiveNamingPolicy);
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        if (N.notEmpty(propNames)) {
            final int size = propNames.size();

            if (size == 1) {
                final String singleProp = propNames.iterator().next();

                return effectiveNamingPolicy.convert(singleProp) + SK._SPACE + opStr + SK.SPACE_PARENTHESIS_L + subQueryString + SK.PARENTHESIS_R;
            }

            final StringBuilder sb = new StringBuilder(16 + (size << 4) + subQueryString.length());
            sb.append(SK._PARENTHESIS_L);

            int i = 0;
            for (final String propName : propNames) {
                if (i++ > 0) {
                    sb.append(SK.COMMA_SPACE);
                }

                sb.append(effectiveNamingPolicy.convert(propName));
            }

            sb.append(SK._PARENTHESIS_R).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L).append(subQueryString).append(SK.PARENTHESIS_R);

            return sb.toString();
        }

        return opStr + SK.SPACE_PARENTHESIS_L + subQueryString + SK.PARENTHESIS_R;
    }
}
