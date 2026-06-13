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

import static com.landawn.abacus.util.SK._PARENTHESIS_L;
import static com.landawn.abacus.util.SK._PARENTHESIS_R;
import static com.landawn.abacus.util.SK._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Base class for composable junction conditions that combine multiple conditions.
 * This class is used to join multiple conditions using composable operators like AND or OR.
 * It provides the foundation for creating complex query conditions by combining simpler ones.
 * Junctions can be nested to create arbitrarily complex composable expressions.
 * 
 * <p>Important: Junction is intended for composable boolean conditions. It is not meant to
 * contain clause conditions (WHERE, ORDER BY, etc.); those are handled by {@link Criteria}.
 * The public constructors enforce this contract via {@code validateConstructorOperand} and
 * reject {@link Criteria} and clause-operator conditions by throwing
 * {@link IllegalArgumentException}. Only the package-private marker constructor (used for
 * trusted, already-validated fluent chaining) skips this validation.
 * 
 * <p>This class serves as the parent for specific junction types:
 * <ul>
 *   <li>{@link And} - combines conditions with logical AND (all must be true)</li>
 *   <li>{@link Or} - combines conditions with logical OR (at least one must be true)</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Fixed after construction — no conditions can be added or removed after creation</li>
 *   <li>Automatic parentheses handling for correct precedence in generated SQL</li>
 *   <li>Parameter collection from all nested conditions</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an AND junction
 * Junction and = new Junction(Operator.AND, 
 *     new Equal("status", "active"),
 *     new GreaterThan("age", 18),
 *     new LessThan("age", 65)
 * );
 * // SQL: ((status = 'active') AND (age > 18) AND (age < 65))
 * 
 * // Create an OR junction
 * Junction or = new Junction(Operator.OR,
 *     new Equal("city", "New York"),
 *     new Equal("city", "Los Angeles"),
 *     new Equal("city", "Chicago")
 * );
 * // SQL: ((city = 'New York') OR (city = 'Los Angeles') OR (city = 'Chicago'))
 * 
 * // Nested junctions for complex logic
 * Junction complex = new Junction(Operator.AND,
 *     new Equal("type", "premium"),
 *     new Junction(Operator.OR,
 *         new GreaterThan("balance", 10000),
 *         new Equal("vip_status", true)
 *     )
 * );
 * }</pre>
 * 
 * @see And
 * @see Or
 * @see Criteria
 * @see ComposableCondition
 */
public class Junction extends ComposableCondition {

    final List<Condition> conditions;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Lazily memoized unmodifiable view of {@link #conditions} (performance only). */
    private transient List<Condition> cachedConditionsView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Junction instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Junction() {
        conditions = new ArrayList<>();
    }

    /**
     * Trusted package-private constructor that adopts an already-validated, freshly-allocated and
     * non-leaked condition list without re-running per-element null checks. Used by fluent
     * {@code and()}/{@code or()} chaining in subclasses to keep chaining O(n) overall. The
     * {@code marker} parameter only serves to distinguish this constructor's signature.
     *
     * <p>The supplied list is taken over directly (not copied); callers must pass a private,
     * freshly created list that they will not retain or mutate afterwards.</p>
     *
     * @param operator the composable operator
     * @param ownedValidatedConditions a freshly created list whose elements have already been null-validated
     * @param marker disambiguation marker (ignored)
     */
    @SuppressWarnings({ "unchecked", "unused" })
    Junction(final Operator operator, final List<? extends Condition> ownedValidatedConditions, final boolean marker) {
        super(operator);
        this.conditions = (List<Condition>) ownedValidatedConditions;
    }

    /**
     * Creates a new Junction with the specified operator and conditions.
     * This constructor initializes the junction with a set of conditions that will
     * be combined using the specified composable operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an AND junction with multiple conditions
     * Junction activeAdults = new Junction(Operator.AND,
     *     new Equal("active", true),
     *     new GreaterThanOrEqual("age", 18),
     *     new IsNotNull("email")
     * );
     * // activeAdults.toString() returns
     * //   "((active = true) AND (age >= 18) AND (email IS NOT NULL))"
     *
     * // Create an OR junction for status checks
     * Junction validStatus = new Junction(Operator.OR,
     *     new Equal("status", "approved"),
     *     new Equal("status", "pending_review"),
     *     new Equal("override", true)
     * );
     *
     * // Edge: no conditions -> renders as an empty string
     * Junction none = new Junction(Operator.AND);
     * // none.toString() returns ""
     *
     * // Edge: a null element is rejected
     * Junction bad = new Junction(Operator.AND, new Equal("a", 1), (Condition) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param operator the composable operator to use (AND, OR, etc.)
     * @param conditions the conditions to combine; may be {@code null} or empty (treated as no conditions)
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if any element in {@code conditions} is {@code null}, or if any
     *             element is a {@link Criteria}, has a clause operator (WHERE, JOIN variants, ORDER_BY, etc.),
     *             is an {@code ON}/{@code USING} condition that is not an {@link On} instance, is an
     *             {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or is an empty predicate
     *             (a blank {@link Expression} or empty {@link Junction})
     */
    public Junction(final Operator operator, final Condition... conditions) {
        super(operator);
        this.conditions = new ArrayList<>();
        appendConditions(conditions);
    }

    /**
     * Creates a new Junction with the specified operator and collection of conditions.
     * This constructor is useful when conditions are already collected in a list or set.
     * The collection is copied internally so that subsequent changes to the original collection
     * do not affect this condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create conditions dynamically
     * List<Condition> conditions = new ArrayList<>();
     * conditions.add(new Equal("status", "active"));
     * conditions.add(new GreaterThan("score", 80));
     * if (includeDateCheck) {
     *     conditions.add(new LessThanOrEqual("date", today));
     * }
     *
     * Junction junction = new Junction(Operator.AND, conditions);
     * // junction.toString() returns "((status = 'active') AND (score > 80))"
     *
     * // Edge: a null collection is treated as no conditions -> renders as ""
     * Junction empty = new Junction(Operator.OR, (Collection<Condition>) null);
     * // empty.toString() returns ""
     * }</pre>
     *
     * @param operator the composable operator to use (AND, OR, etc.)
     * @param conditions the collection of conditions to combine; may be {@code null} or empty (treated as no conditions)
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if any element in {@code conditions} is {@code null}, or if any
     *             element is a {@link Criteria}, has a clause operator (WHERE, JOIN variants, ORDER_BY, etc.),
     *             is an {@code ON}/{@code USING} condition that is not an {@link On} instance, is an
     *             {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or is an empty predicate
     *             (a blank {@link Expression} or empty {@link Junction})
     */
    public Junction(final Operator operator, final Collection<? extends Condition> conditions) {
        super(operator);
        this.conditions = new ArrayList<>();
        appendConditions(conditions); // NOSONAR
    }

    /**
     * Gets the list of conditions contained in this junction.
     * The returned list is an unmodifiable view; attempts to modify it
     * will throw {@link UnsupportedOperationException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction and = new Junction(Operator.AND,
     *     new Equal("status", "active"),
     *     new GreaterThan("age", 18));
     * List<Condition> conditions = and.getConditions();
     * // conditions = [Equal("status", "active"), GreaterThan("age", 18)]
     * conditions.size();             // returns 2
     * conditions.get(0).toString();  // returns "status = 'active'"
     *
     * // Edge: the returned view is unmodifiable
     * conditions.add(new Equal("x", 1));   // throws UnsupportedOperationException
     *
     * // Edge: an empty junction returns an empty list
     * new Junction(Operator.AND).getConditions().isEmpty();   // returns true
     * }</pre>
     *
     * @return an unmodifiable view of the list of conditions in this junction
     */
    public List<Condition> getConditions() {
        List<Condition> view = cachedConditionsView;

        if (view == null) {
            view = Collections.unmodifiableList(conditions);
            cachedConditionsView = view;
        }

        return view;
    }

    private void appendConditions(final Condition... conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        for (final Condition condition : conditions) {
            validateConstructorOperand(condition);
        }

        Collections.addAll(this.conditions, conditions);
    }

    private void appendConditions(final Collection<? extends Condition> conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        for (final Condition condition : conditions) {
            validateConstructorOperand(condition);
        }

        this.conditions.addAll(conditions);
    }

    private static Condition validateConstructorOperand(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        if (condition instanceof Criteria || isClause(condition) || containsOnOrUsing(condition) || isQuantifiedSubQueryOperand(condition)
                || isEmptyPredicate(condition)) {
            throw new IllegalArgumentException("Condition with operator '" + condition.operator() + "' cannot be used in a junction constructor");
        }

        return condition;
    }

    /**
     * Gets all parameters from all conditions in this junction.
     * This method recursively collects parameters from all nested conditions,
     * including those in nested junctions. The order of parameters matches
     * the order they would appear in the generated SQL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction and = new Junction(Operator.AND,
     *     new Equal("status", "active"),
     *     new Between("age", 18, 65));
     * and.getParameters();          // returns ["active", 18, 65]
     *
     * // Edge: an empty junction has no parameters
     * Junction empty = new Junction(Operator.AND);
     * empty.getParameters();        // returns [] (empty, immutable)
     *
     * // Edge: conditions without bound values contribute nothing
     * Junction noParams = new Junction(Operator.OR, new IsNotNull("email"));
     * noParams.getParameters();     // returns []
     * }</pre>
     *
     * @return an immutable list containing all parameters from all conditions
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            final List<Object> parameters = new ArrayList<>(conditions.size());

            for (final Condition condition : conditions) {
                if (condition != null) {
                    parameters.addAll(condition.getParameters());
                }
            }

            result = ImmutableList.wrap(parameters);
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Converts this junction to its string representation according to the specified naming policy.
     * Each contained condition is wrapped in parentheses, joined by the junction operator, and the
     * entire result is itself wrapped in an outer pair of parentheses (e.g.
     * {@code "((cond1) AND (cond2) AND (cond3))"}). This ensures proper precedence in nested
     * composable expressions. Any {@code null} entries in the conditions list are skipped, and an
     * empty string is returned if the junction has no conditions or every condition is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction and = new Junction(Operator.AND,
     *     new Equal("status", "active"),
     *     new GreaterThan("age", 18));
     * and.toString(NamingPolicy.NO_CHANGE);
     * // returns "((status = 'active') AND (age > 18))"
     *
     * // Edge: a single condition is still doubly parenthesized
     * Junction single = new Junction(Operator.OR, new Equal("x", 1));
     * single.toString(NamingPolicy.NO_CHANGE);   // returns "((x = 1))"
     *
     * // Edge: naming policy rewrites property names
     * Junction snake = new Junction(Operator.AND, new Equal("firstName", "John"));
     * snake.toString(NamingPolicy.SNAKE_CASE);   // returns "((first_name = 'John'))"
     *
     * // Edge: an empty junction renders as an empty string
     * new Junction(Operator.AND).toString(NamingPolicy.NO_CHANGE);   // returns ""
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to property names within each condition
     * @return the string representation with proper parentheses and spacing, or an empty string if
     *         no non-{@code null} conditions are present
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (N.isEmpty(conditions)) {
            return Strings.EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            final Operator op = operator();
            final String opStr = op == null ? Strings.NULL : op.toString();
            boolean isFirst = true;
            for (final Condition condition : conditions) {
                if (condition == null) {
                    continue;
                }

                if (!isFirst) {
                    sb.append(_SPACE);
                    sb.append(opStr);
                    sb.append(_SPACE);
                } else {
                    sb.append(_PARENTHESIS_L);
                }

                sb.append(_PARENTHESIS_L);
                sb.append(condition.toString(namingPolicy));
                sb.append(_PARENTHESIS_R);

                isFirst = false;
            }

            if (isFirst) {
                return Strings.EMPTY;
            }

            sb.append(_PARENTHESIS_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Computes the hash code for this junction based on its operator and conditions.
     * The hash code is consistent with equals() - junctions with the same operator
     * and conditions will have the same hash code.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction a = new Junction(Operator.AND, new Equal("status", "active"));
     * Junction b = new Junction(Operator.AND, new Equal("status", "active"));
     * a.hashCode() == b.hashCode();   // true (same operator and conditions)
     *
     * // Edge: a different operator produces a different hash code
     * Junction c = new Junction(Operator.OR, new Equal("status", "active"));
     * a.hashCode() == c.hashCode();   // (typically) false
     * }</pre>
     *
     * @return hash code based on operator and condition list
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + conditions.hashCode();

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this junction is equal to another object.
     * Two junctions are considered equal if they have the same operator
     * and contain the same conditions in the same order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction a = new Junction(Operator.AND, new Equal("status", "active"));
     * Junction b = new Junction(Operator.AND, new Equal("status", "active"));
     * a.equals(b);    // returns true
     *
     * // Edge: a different operator -> not equal
     * Junction c = new Junction(Operator.OR, new Equal("status", "active"));
     * a.equals(c);    // returns false
     *
     * // Edge: same conditions in a different order -> not equal
     * Junction d = new Junction(Operator.AND, new Equal("a", 1), new Equal("b", 2));
     * Junction e = new Junction(Operator.AND, new Equal("b", 2), new Equal("a", 1));
     * d.equals(e);    // returns false
     *
     * a.equals(null); // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is a Junction with the same operator and conditions
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Junction other) {
            return N.equals(operator, other.operator) && N.equals(conditions, other.conditions);

        }

        return false;
    }
}
