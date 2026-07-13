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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a condition cell that wraps another condition with an operator.
 * This class serves as a container for a condition paired with a specific operator,
 * typically used for unary operations or clauses that modify other conditions.
 * 
 * <p>A Cell is commonly used to wrap conditions with operators like {@code ON} and
 * {@code USING}, or to create clause conditions like {@code WHERE}, {@code HAVING}, etc.
 * It acts as a decorator that adds an operator context to an existing condition.</p>
 *
 * <p>Direct subclasses include {@link On}, {@link Using}, and the abstract {@link Clause}
 * (along with its concrete subclasses such as {@link Where} and {@link Having}). For
 * composable wrappers like {@link Not}, {@link Exists}, and {@link NotExists}, see
 * {@link ComposableCell}.</p>
 *
 * @see AbstractCondition
 * @see ComposableCell
 * @see Clause
 * @see Operator
 */
public abstract class Cell extends AbstractCondition {

    private final Condition condition;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Cell instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Cell() {
        condition = null;
    }

    /**
     * Creates a new {@code Cell} with the specified operator and condition.
     * The Cell wraps the given condition and applies the specified operator to it.
     *
     * <p>This constructor is typically invoked by subclass constructors such as
     * {@link Clause} subclasses, {@link On}, and {@link Using}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Subclasses pass a fixed operator; the rendered form is "OPERATOR condition"
     * Cell where = new Where(Filters.eq("active", true));
     * // where.toString() returns "WHERE active = true"
     *
     * Cell having = new Having(Filters.gt("count", 5));
     * // having.toString() returns "HAVING count > 5"
     *
     * // Edge: a null condition is rejected
     * Cell bad = new Where((Condition) null);     // throws IllegalArgumentException
     *
     * // Edge: an uninitialized Cell (created only via the package-private no-arg constructor)
     * // has a null operator and renders the literal "null".
     * }</pre>
     *
     * @param operator the operator to apply to the condition (must not be {@code null})
     * @param condition the condition to wrap (must not be {@code null})
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code condition} is {@code null}
     */
    protected Cell(final Operator operator, final Condition condition) {
        super(operator);
        this.condition = N.checkArgNotNull(condition, "condition");
    }

    /**
     * Returns the wrapped condition.
     * Callers that need a more specific subtype must cast explicitly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On onCond = new On("a.id", "b.id");
     * Equal inner = (Equal) onCond.condition();
     * // inner.toString() returns "a.id = b.id"
     *
     * Where where = new Where(Filters.eq("active", true));
     * Condition c = where.condition();
     * // c.toString() returns "active = true"
     *
     * // Edge: the wrapped condition is returned as-is; cast to the concrete type to access subtype API
     * Equal eq = (Equal) where.condition();   // ok
     * Like bad = (Like) where.condition();    // throws ClassCastException
     * }</pre>
     *
     * @return the wrapped condition; never {@code null} for instances created via the protected
     *         constructor, but may be {@code null} for uninitialized instances produced by the
     *         package-private default constructor (e.g., during Kryo deserialization)
     */
    public Condition condition() {
        return condition;
    }

    /**
     * Returns the parameters from the wrapped condition.
     * This method delegates to the wrapped condition's parameters method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = new Where(Filters.between("age", 18, 65));
     * where.parameters();          // returns [18, 65]
     *
     * Where single = new Where(Filters.eq("active", true));
     * single.parameters();         // returns [true]
     *
     * // Edge: wrapping a parameter-free condition yields an empty list
     * On onCond = new On("a.id", "b.id");
     * onCond.parameters();         // returns [] (empty, immutable)
     *
     * // Edge: the returned list reflects the wrapped condition only and is immutable
     * }</pre>
     *
     * @return an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
     */
    @Override
    public ImmutableList<Object> parameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = (condition == null) ? ImmutableList.empty() : condition.parameters();
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Converts this {@code Cell} condition to its string representation using the specified naming policy.
     * The output format is {@code OPERATOR condition_string} (separated by a single space), or just
     * {@code OPERATOR} if the wrapped condition is {@code null}. Unlike {@link ComposableCell#toString(NamingPolicy)},
     * the inner condition is <em>not</em> enclosed in parentheses. If the operator is {@code null}
     * (only possible for an uninitialized instance), the literal {@code "null"} is rendered in place of the operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = new Where(Filters.eq("active", true));
     * where.toString(NamingPolicy.NO_CHANGE);   // returns "WHERE active = true"
     *
     * On onCond = new On("a.id", "b.id");
     * onCond.toString(NamingPolicy.NO_CHANGE);  // returns "ON a.id = b.id" (inner NOT parenthesized)
     *
     * // Edge: naming policy rewrites property names in the wrapped condition
     * Where w = new Where(Filters.eq("firstName", "John"));
     * w.toString(NamingPolicy.SNAKE_CASE);      // returns "WHERE first_name = 'John'"
     *
     * // Edge: an uninitialized Cell (null operator) renders the literal "null" for the operator
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to property names within the wrapped condition;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a string representation of this Cell
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();

        // Note: unlike ComposableCell, the wrapped condition is NOT parenthesized; a null naming
        // policy is normalized to NamingPolicy.NO_CHANGE before being applied to the wrapped condition.
        return (op == null ? Strings.NULL : op.toString()) + ((condition == null) ? Strings.EMPTY : SK._SPACE + condition.toString(effectiveNamingPolicy));
    }

    /**
     * Returns the hash code of this Cell.
     * The hash code is computed based on the operator and wrapped condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where a = new Where(Filters.eq("active", true));
     * Where b = new Where(Filters.eq("active", true));
     * a.hashCode() == b.hashCode();   // true (same operator and condition)
     *
     * // Edge: a different wrapped condition produces a different hash code
     * Where c = new Where(Filters.eq("active", false));
     * a.hashCode() == c.hashCode();   // (typically) false
     * }</pre>
     *
     * @return hash code based on operator and wrapped condition
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + ((condition == null) ? 0 : condition.hashCode());

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this Cell is equal to another object.
     * Two Cells are equal if they are of the same runtime class and have the same operator
     * and wrapped condition. Different concrete subclasses of {@code Cell} are never equal,
     * even when their operator and wrapped condition are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where a = new Where(Filters.eq("active", true));
     * Where b = new Where(Filters.eq("active", true));
     * a.equals(b);    // returns true
     *
     * // Edge: different wrapped condition -> not equal
     * Where c = new Where(Filters.eq("active", false));
     * a.equals(c);    // returns false
     *
     * // Edge: different concrete subclass -> never equal, even with equal operator/condition
     * Where w = new Where(Filters.eq("x", 1));
     * Having h = new Having(Filters.eq("x", 1));
     * w.equals(h);    // returns false
     *
     * a.equals(null); // returns false
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

        final Cell other = (Cell) obj;
        return N.equals(operator, other.operator) && N.equals(condition, other.condition);
    }
}
