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
import java.util.List;

import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.cs;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for BETWEEN and NOT BETWEEN conditions in SQL queries.
 * This class provides all shared implementation for range-based conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link Between} and {@link NotBetween} is the operator
 * ({@code BETWEEN} vs {@code NOT BETWEEN}). All fields, getters, and methods
 * for parameters, string rendering, hashing, and equality are identical.</p>
 *
 * <p>Arrays, {@link java.util.Date} values, and {@link java.util.Calendar} values are snapshotted
 * on construction and defensively copied when exposed. Other application-defined mutable bounds
 * are retained by reference; callers must not mutate them while the condition is in use.</p>
 *
 * @see Between
 * @see NotBetween
 * @see ComposableCondition
 */
public abstract class AbstractBetween extends ComposableCondition {

    /**
     * The property/column name checked by this condition.
     * It is {@code null} only for an uninitialized instance created by a serialization
     * framework like Kryo.
     */
    final String propName;

    /**
     * The lower bound of the range; inclusive for {@code BETWEEN}, and the lower edge of the
     * excluded range for {@code NOT_BETWEEN}. May be a non-null literal value, a
     * {@link SqlExpression}, or a scalar {@link SubQuery}.
     */
    private Object minValue;

    /**
     * The upper bound of the range; inclusive for {@code BETWEEN}, and the upper edge of the
     * excluded range for {@code NOT_BETWEEN}. May be a non-null literal value, a
     * {@link SqlExpression}, or a scalar {@link SubQuery}.
     */
    private Object maxValue;

    /**
     * Whether {@link #parameters()} must rebuild its result on every call instead of memoizing it: {@code true}
     * when either bound is an array, {@code Date}, {@code Calendar} or a nested {@link Condition} (a
     * {@link SubQuery} or {@link SqlExpression}). Computed once at construction because the bounds are
     * immutable afterwards. A nested condition counts as mutable because its own {@code parameters()} may hand
     * out fresh defensive copies that a memoized outer list would otherwise share across calls.
     */
    private final boolean rebuildParametersPerCall;

    /** Lazily memoized parameters (performance only; unused when {@link #rebuildParametersPerCall} is {@code true}). */
    private transient ImmutableList<Object> cachedParameters;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractBetween() {
        propName = null;
        rebuildParametersPerCall = false;
    }

    /**
     * Creates a new BETWEEN or NOT BETWEEN condition.
     * A structured subquery bound with a known, non-wildcard projection must select exactly one column;
     * raw SQL and wildcard projection arity are left to the database.
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the operator ({@link Operator#BETWEEN} or {@link Operator#NOT_BETWEEN})
     * @param minValue the lower bound of the range; inclusive for {@code BETWEEN}, and the lower edge of the
     *                 excluded range for {@code NOT_BETWEEN} (values strictly below it match); may be a
     *                 non-null literal value, an explicit {@link SqlExpression}, or a scalar
     *                 {@link SubQuery} whose parameters will be spliced into {@link #parameters()}
     * @param maxValue the upper bound of the range; inclusive for {@code BETWEEN}, and the upper edge of the
     *                 excluded range for {@code NOT_BETWEEN} (values strictly above it match); may be a
     *                 non-null literal value, an explicit {@link SqlExpression}, or a scalar
     *                 {@link SubQuery} whose parameters will be spliced into {@link #parameters()}
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code operator} is neither {@link Operator#BETWEEN} nor {@link Operator#NOT_BETWEEN},
     *                                  if {@code propName} is {@code null}, empty, or blank,
     *                                  if either bound is {@code null}, an ordinary predicate or query clause,
     *                                  a blank {@link SqlExpression}, an {@link All}, {@link Any}, or {@link Some}
     *                                  quantified operand, or a scalar {@link SubQuery} with a known, non-wildcard
     *                                  projection containing multiple columns, or if either bound is a cyclic object array
     */
    protected AbstractBetween(final String propName, final Operator operator, final Object minValue, final Object maxValue) {
        super(validateOperator(operator));

        checkPropName(propName);
        N.checkArgNotNull(minValue, cs.minValue);
        N.checkArgNotNull(maxValue, cs.maxValue);
        validateNonQuantifiedValueOperand(minValue, cs.minValue);
        validateNonQuantifiedValueOperand(maxValue, cs.maxValue);

        this.propName = propName;
        this.minValue = snapshotMutableValue(minValue);
        this.maxValue = snapshotMutableValue(maxValue);
        this.rebuildParametersPerCall = requiresPerCallParameters(this.minValue) || requiresPerCallParameters(this.maxValue);
    }

    /**
     * Detects whether returning a memoized parameter list would expose a known mutable value: an array,
     * {@code Date} or {@code Calendar} bound, or a nested {@link Condition} whose spliced-in parameters may
     * themselves be per-call defensive copies. Evaluated once at construction into
     * {@link #rebuildParametersPerCall}.
     */
    private static boolean requiresPerCallParameters(final Object bound) {
        return isSnapshotMutableValue(bound) || bound instanceof Condition;
    }

    private static Operator validateOperator(final Operator operator) {
        if (operator == null) {
            throw new NullPointerException("operator");
        }

        if (operator != Operator.BETWEEN && operator != Operator.NOT_BETWEEN) {
            throw new IllegalArgumentException("Only BETWEEN and NOT_BETWEEN are supported: " + operator);
        }

        return operator;
    }

    /**
     * Returns the property name being checked in this BETWEEN or NOT BETWEEN condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between between = new Between("age", 18, 65);
     * String prop = between.propName();   // "age"
     * }</pre>
     *
     * @return the property name, or {@code null} for an uninitialized serialization-framework instance
     */
    public String propName() {
        return propName;
    }

    /**
     * Returns the lower bound of the range. For {@link Between} this is the inclusive lower bound;
     * for {@link NotBetween} this is the lower bound of the excluded range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between between = new Between("age", 18, 65);
     * Integer min = between.minValue();   // 18
     * }</pre>
     *
     * @param <T> the expected type of the minimum value (caller-supplied; an unchecked cast is
     *            performed internally and a {@link ClassCastException} may be thrown at the call site)
     * @return the configured minimum value, which may be a literal, a {@link SqlExpression}, or a
     *         scalar {@link SubQuery}; known mutable JDK values are returned as defensive copies;
     *         {@code null} is returned only for an uninitialized serialization-framework instance
     */
    @SuppressWarnings("unchecked")
    public <T> T minValue() {
        return (T) snapshotMutableValue(minValue);
    }

    /**
     * Returns the upper bound of the range. For {@link Between} this is the inclusive upper bound;
     * for {@link NotBetween} this is the upper bound of the excluded range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between between = new Between("age", 18, 65);
     * Integer max = between.maxValue();   // 65
     * }</pre>
     *
     * @param <T> the expected type of the maximum value (caller-supplied; an unchecked cast is
     *            performed internally and a {@link ClassCastException} may be thrown at the call site)
     * @return the configured maximum value, which may be a literal, a {@link SqlExpression}, or a
     *         scalar {@link SubQuery}; known mutable JDK values are returned as defensive copies;
     *         {@code null} is returned only for an uninitialized serialization-framework instance
     */
    @SuppressWarnings("unchecked")
    public <T> T maxValue() {
        return (T) snapshotMutableValue(maxValue);
    }

    /**
     * Returns the parameters for this condition.
     * Returns a list containing the minimum and maximum values in that order.
     * If either bound is a {@link Condition} (typically a {@link SubQuery}), its parameters are
     * spliced in place of the bound itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Literal bounds -> [min, max]
     * Between between = new Between("age", 18, 65);
     * List<Object> p1 = between.parameters();   // [18, 65]
     *
     * // A Condition bound has its parameters spliced in
     * SubQuery sub = Filters.subQuery("config", Arrays.asList("minAge"), Filters.eq("active", true));
     * Between subBound = new Between("age", sub, 65);
     * List<Object> p2 = subBound.parameters();   // [true, 65]
     * }</pre>
     *
     * <p>The result is memoized only when both bounds are plain scalars (neither an array, {@code Date},
     * {@code Calendar} nor a nested {@link Condition}); otherwise a fresh list, holding fresh defensive copies of
     * any array/{@code Date}/{@code Calendar} values (including those spliced in from a nested condition), is
     * built on every call. Mutating one of these defensive array/date/calendar copies does not affect
     * this condition or a later call; other application-defined mutable values remain shared by reference.</p>
     *
     * @return an immutable list containing {@code [minValue, maxValue]}, or their respective
     *         parameters spliced in where a bound is itself a {@link Condition}; known mutable JDK
     *         bounds are returned as defensive copies
     */
    @Override
    public ImmutableList<Object> parameters() {
        if (rebuildParametersPerCall) {
            return computeParameters();
        }

        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    private ImmutableList<Object> computeParameters() {
        final List<Object> parameters = new ArrayList<>(2);

        if (minValue instanceof Condition) {
            parameters.addAll(((Condition) minValue).parameters());
        } else {
            parameters.add(snapshotMutableValue(minValue));
        }

        if (maxValue instanceof Condition) {
            parameters.addAll(((Condition) maxValue).parameters());
        } else {
            parameters.add(snapshotMutableValue(maxValue));
        }

        return ImmutableList.wrap(parameters);
    }

    /**
     * Converts this condition to its SQL representation.
     * The format is: {@code propertyName BETWEEN minValue AND maxValue}
     * (or {@code NOT BETWEEN} for {@link NotBetween}).
     * If the operator is {@code null} (only possible for an uninitialized instance), the literal
     * {@code "null"} is rendered in place of the operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Numeric bounds are unquoted
     * Between between = new Between("age", 18, 65);
     * String s1 = between.toSql(NamingPolicy.NO_CHANGE);   // "age BETWEEN 18 AND 65"
     *
     * // NotBetween uses the NOT BETWEEN operator
     * NotBetween nb = new NotBetween("age", 18, 65);
     * String s2 = nb.toSql(NamingPolicy.NO_CHANGE);   // "age NOT BETWEEN 18 AND 65"
     *
     * // String bounds are single-quoted; a null naming policy uses NO_CHANGE
     * Between str = new Between("name", "A", "M");
     * String s3 = str.toSql(null);   // "name BETWEEN 'A' AND 'M'"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a SQL representation of this condition
     * @throws IllegalArgumentException if either bound is a {@code NaN} or infinite {@link Float}/{@link Double},
     *                                  or a {@link Number} whose text is not a valid numeric literal, or if a
     *                                  {@link SubQuery} bound cannot be rendered, as documented for
     *                                  {@link SubQuery#toSql(NamingPolicy)}
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        final StringBuilder sb = new StringBuilder();
        sb.append(QueryUtil.convertIdentifier(propName, effectiveNamingPolicy))
                .append(SK._SPACE)
                .append(opStr)
                .append(SK._SPACE)
                .append(formatParameter(minValue, effectiveNamingPolicy))
                .append(SK._SPACE)
                .append(SK.AND)
                .append(SK._SPACE)
                .append(formatParameter(maxValue, effectiveNamingPolicy));

        return sb.toString();
    }

    /**
     * Returns the hash code of this condition.
     * The value is recomputed on every call because application-defined mutable bounds are retained
     * by reference. Arrays, dates, and calendars cannot be mutated through this condition because
     * they are snapshotted and defensively copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Same property/operator/bounds -> equal hash codes
     * Between a = new Between("age", 18, 65);
     * Between b = new Between("age", 18, 65);
     * boolean same = a.hashCode() == b.hashCode();   // true
     * }</pre>
     *
     * @return hash code based on property name, operator, and range values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + N.deepHashCode(minValue);
        h = (h * 31) + N.deepHashCode(maxValue);

        return h == 0 ? 1 : h;
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the exact same runtime class, property name,
     * operator, minValue, and maxValue. Array bounds are compared by content via
     * {@link N#deepEquals(Object, Object)}, pairing with the {@link N#deepHashCode(Object)} used by
     * {@link #hashCode()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between a = new Between("age", 18, 65);
     * Between b = new Between("age", 18, 65);
     * boolean eq = a.equals(b);   // true
     *
     * // Different upper bound -> not equal
     * boolean neMax = a.equals(new Between("age", 18, 99));   // false
     *
     * // Different operator (BETWEEN vs NOT BETWEEN) -> not equal
     * boolean neOp = a.equals(new NotBetween("age", 18, 65));   // false
     *
     * // Non-AbstractBetween object -> not equal
     * boolean neType = a.equals("age");   // false
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

        final AbstractBetween other = (AbstractBetween) obj;
        return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.deepEquals(minValue, other.minValue)
                && N.deepEquals(maxValue, other.maxValue);
    }
}
