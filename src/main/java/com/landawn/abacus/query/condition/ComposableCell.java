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
 * A composable variant of {@link Cell} that supports logical composition via AND/OR/NOT operations.
 * Like Cell, it wraps another condition with an operator, but extends {@link ComposableCondition}
 * instead of {@link AbstractCondition}, enabling chaining with other conditions.
 *
 * <p>Concrete subclasses include {@link Not}, {@link Exists}, {@link NotExists}, {@link All}, {@link Any}, and {@link Some}.</p>
 *
 * @see Cell
 * @see ComposableCondition
 * @see Not
 * @see Exists
 * @see NotExists
 */
public abstract class ComposableCell extends ComposableCondition {

    /**
     * The wrapped condition. This field is immutable once set in the constructor.
     * Accessed by subclasses for direct rendering when they override {@code toString(NamingPolicy)}.
     */
    protected final Condition condition;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Single-slot toString cache: last naming policy and its rendered string (performance only). */
    private transient NamingPolicy cachedTostringNamingPolicy;

    private transient String cachedTostring;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * Creates an uninitialized ComposableCell instance; not for direct application use.
     */
    ComposableCell() {
        this.condition = null;
    }

    /**
     * Creates a new {@code ComposableCell} with the specified operator and condition.
     *
     * <p>This constructor is typically invoked by subclass constructors such as
     * {@link Not}, {@link Exists}, {@link NotExists}, {@link All}, {@link Any}, and {@link Some}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Subclasses pass a fixed operator; the rendered form is "OPERATOR (condition)"
     * ComposableCell not = new Not(Filters.eq("status", "active"));
     * // not.toString() returns "NOT (status = 'active')"
     *
     * ComposableCell exists = new Exists(Filters.subQuery("SELECT id FROM orders"));
     * // exists.toString() returns "EXISTS (SELECT id FROM orders)"
     *
     * // Edge: a null condition is rejected
     * ComposableCell bad = new Not((Condition) null);   // throws IllegalArgumentException
     *
     * // Edge: an uninitialized instance (null operator) renders the literal "null" for the operator
     * }</pre>
     *
     * @param operator the operator to apply to the condition (must not be {@code null})
     * @param cond the condition to wrap (must not be {@code null})
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code cond} is {@code null}
     */
    public ComposableCell(final Operator operator, final Condition cond) {
        super(operator);
        this.condition = N.checkArgNotNull(cond, "cond");
    }

    /**
     * Gets the wrapped condition.
     * Callers that need a more specific subtype must cast explicitly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("status", "active");
     * Not notCond = new Not(eq);
     * Equal inner = (Equal) notCond.getCondition();
     * // inner.toString() returns "status = 'active'"
     *
     * SubQuery sub = Filters.subQuery("SELECT id FROM orders");
     * Exists exists = new Exists(sub);
     * Condition c = exists.getCondition();
     * // c == sub (the same wrapped SubQuery is returned)
     *
     * // Edge: the wrapped condition is returned as-is; an incompatible cast fails
     * Like bad = (Like) notCond.getCondition();   // throws ClassCastException
     * }</pre>
     *
     * @return the wrapped condition; never {@code null} for instances created via the public
     *         constructor, but may be {@code null} for uninitialized instances produced by the
     *         package-private default constructor (e.g., during Kryo deserialization)
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Gets the parameters from the wrapped condition.
     * This method delegates to the wrapped condition's getParameters method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Not not = new Not(Filters.between("age", 18, 65));
     * not.getParameters();          // returns [18, 65]
     *
     * Not single = new Not(Filters.eq("active", true));
     * single.getParameters();       // returns [true]
     *
     * // Edge: a wrapped SubQuery with no bound parameters yields an empty list
     * Exists exists = new Exists(Filters.subQuery("SELECT 1"));
     * exists.getParameters();       // returns [] (empty, immutable)
     *
     * // Edge: the returned list reflects the wrapped condition only and is immutable
     * }</pre>
     *
     * @return an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = (condition == null) ? ImmutableList.empty() : condition.getParameters();
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Converts this {@code ComposableCell} to its string representation using the specified naming policy.
     * The output format is: {@code OPERATOR (condition_string)}
     * (the inner condition is always enclosed in parentheses).
     * If the operator is {@code null} (only possible for an uninitialized instance), the literal
     * {@code "null"} is rendered in place of the operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Not not = new Not(Filters.eq("status", "active"));
     * not.toString(NamingPolicy.NO_CHANGE);   // returns "NOT (status = 'active')"
     *
     * Exists exists = new Exists(Filters.subQuery("SELECT 1"));
     * exists.toString(NamingPolicy.NO_CHANGE); // returns "EXISTS (SELECT 1)"
     *
     * // Edge: naming policy rewrites property names in the wrapped condition
     * Not w = new Not(Filters.eq("firstName", "John"));
     * w.toString(NamingPolicy.SNAKE_CASE);    // returns "NOT (first_name = 'John')"
     *
     * // Edge: a null naming policy falls back to NO_CHANGE; an uninitialized
     * // instance (null operator) renders the literal "null" for the operator
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to property names within the wrapped condition;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a string representation of this ComposableCell
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
        final Condition condition = getCondition();
        final String conditionString = condition == null ? "" : condition.toString(effectiveNamingPolicy);
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();
        return opStr + SK._SPACE + SK._PARENTHESIS_L + conditionString + SK._PARENTHESIS_R;
    }

    /**
     * Returns the hash code of this ComposableCell, based on the operator and wrapped condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Not a = new Not(Filters.eq("status", "active"));
     * Not b = new Not(Filters.eq("status", "active"));
     * a.hashCode() == b.hashCode();   // true (same operator and condition)
     *
     * // Edge: a different wrapped condition produces a different hash code
     * Not c = new Not(Filters.eq("status", "inactive"));
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
     * Checks if this ComposableCell is equal to another object.
     * Two ComposableCells are equal if they are of the same runtime class and have the same
     * operator and wrapped condition. Different concrete subclasses of {@code ComposableCell}
     * are never equal, even when their operator and wrapped condition are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Not a = new Not(Filters.eq("status", "active"));
     * Not b = new Not(Filters.eq("status", "active"));
     * a.equals(b);    // returns true
     *
     * // Edge: different wrapped condition -> not equal
     * Not c = new Not(Filters.eq("status", "inactive"));
     * a.equals(c);    // returns false
     *
     * // Edge: different concrete subclass -> never equal, even with equal operator/condition
     * SubQuery sub = Filters.subQuery("SELECT 1");
     * Exists exists = new Exists(sub);
     * NotExists notExists = new NotExists(sub);
     * exists.equals(notExists);   // returns false
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

        final ComposableCell other = (ComposableCell) obj;
        return N.equals(operator, other.operator) && N.equals(condition, other.condition);
    }
}
