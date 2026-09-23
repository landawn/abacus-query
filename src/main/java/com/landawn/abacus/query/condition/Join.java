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

import static com.landawn.abacus.util.SK._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.query.cs;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Base class for SQL JOIN operations.
 * This class provides the foundation for different types of joins (INNER, LEFT, RIGHT, FULL, CROSS, NATURAL)
 * and handles the common functionality of specifying join tables and conditions. JOIN operations
 * are fundamental to relational databases, allowing you to combine rows from multiple tables
 * based on related columns.
 * 
 * <p>A JOIN clause combines rows from two or more tables based on a related column between them.
 * This class supports:
 * <ul>
 *   <li>Conditionless joins ({@link CrossJoin} and {@link NaturalJoin} only; every other join type requires an ON/USING predicate)</li>
 *   <li>Joins with ON conditions specifying how tables relate</li>
 *   <li>Joins with multiple tables in a single operation</li>
 *   <li>Complex join conditions using AND/OR logic</li>
 * </ul>
 * 
 * <p>This class is typically not used directly. Instead, use one of its subclasses:
 * <ul>
 *   <li>{@link InnerJoin} - Returns only matching rows from both tables</li>
 *   <li>{@link LeftJoin} - Returns all rows from the left table and matching rows from the right</li>
 *   <li>{@link RightJoin} - Returns all rows from the right table and matching rows from the left</li>
 *   <li>{@link FullJoin} - Returns all rows from both tables</li>
 *   <li>{@link CrossJoin} - Returns the Cartesian product of rows from both tables</li>
 *   <li>{@link NaturalJoin} - Automatically joins on all columns with identical names</li>
 * </ul>
 * 
 * <p>Join performance considerations:
 * <ul>
 *   <li>Ensure join columns are indexed for better performance</li>
 *   <li>Join order can affect query performance</li>
 *   <li>Use appropriate join types to avoid unnecessary data retrieval</li>
 *   <li>Consider denormalization for frequently joined tables</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Unconditional join: only CrossJoin and NaturalJoin can be built without a predicate
 * Join cross = new CrossJoin("orders");
 * // SQL: CROSS JOIN orders
 *
 * // Join with ON condition using On class
 * Join joinWithCondition = new Join("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // SQL: JOIN orders o ON customers.id = o.customer_id
 *
 * // Join with SqlExpression for custom conditions
 * Join exprJoin = new Join("orders o",
 *     Filters.expr("customers.id = o.customer_id"));
 * // SQL: JOIN orders o ON customers.id = o.customer_id
 *
 * // Join multiple tables
 * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
 *     new On("o.id", "oi.order_id"));
 * // SQL: JOIN (orders o CROSS JOIN order_items oi) ON o.id = oi.order_id
 * }</pre>
 * 
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see On
 * @see Using
 * @see AbstractCondition
 */
public class Join extends AbstractCondition {

    private List<String> joinEntities;

    private Condition condition;

    /** Lazily memoized immutable view of {@link #joinEntities} (performance only). */
    private transient ImmutableList<String> cachedJoinEntitiesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Join instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Join() {
    }

    /**
     * Creates a plain {@code JOIN} clause for the specified table or entity without a join condition.
     *
     * <p>This constructor <b>always throws</b>: a qualified join requires a non-{@code null}
     * {@code ON}/{@code USING} predicate, so there is no way for it to succeed. It is retained only
     * for source compatibility.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with an ON predicate
     * Join join = new Join("products p", Filters.on("categories.id", "p.category_id"));
     * // SQL: JOIN products p ON categories.id = p.category_id
     *
     * // Unconditional join
     * Join cross = new CrossJoin("products");
     * // SQL: CROSS JOIN products
     *
     * // Edge: the single-argument form cannot succeed
     * new Join("products");   // throws IllegalArgumentException
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException always: if {@code joinEntity} is {@code null}, empty, or blank that is reported first;
     *                                  otherwise because a qualified {@code JOIN} requires a non-{@code null} {@code ON}/{@code USING} predicate
     * @deprecated always throws {@link IllegalArgumentException} because a qualified join requires an {@code ON}/{@code USING}
     *             predicate; use {@link #Join(String, Condition)} instead, or {@link CrossJoin} for an unconditional join
     */
    @Deprecated
    public Join(final String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    /**
     * Creates a JOIN clause with the specified operator and table or entity, without a join condition.
     * This protected constructor is used by subclasses to specify the join type while reusing the
     * common join logic. Only {@link Operator#CROSS_JOIN} and {@link Operator#NATURAL_JOIN} can be
     * built without a predicate; every other JOIN operator rejects the missing condition.
     *
     * @param operator the join operator; only {@link Operator#CROSS_JOIN} or {@link Operator#NATURAL_JOIN} succeeds here
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code operator} is {@code null} or is not a JOIN operator; if {@code joinEntity} is
     *                                  {@code null}, empty, or blank; or if {@code operator} is any JOIN operator other than
     *                                  {@link Operator#CROSS_JOIN} or {@link Operator#NATURAL_JOIN} (those require a non-{@code null}
     *                                  {@code ON}/{@code USING} predicate)
     */
    protected Join(final Operator operator, final String joinEntity) {
        this(operator, joinEntity, null);
    }

    /**
     * Creates a JOIN clause with a condition.
     * Uses the default JOIN operator with a join condition. This specifies how
     * the tables are related and which rows should be combined.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with ON condition
     * Join orderJoin = new Join("orders o",
     *     new On("customers.id", "o.customer_id"));
     * // SQL: JOIN orders o ON customers.id = o.customer_id
     *
     * // Join with SqlExpression for custom condition
     * Join exprJoin = new Join("orders o",
     *     Filters.expr("customers.id = o.customer_id"));
     * // SQL: JOIN orders o ON customers.id = o.customer_id
     *
     * // Join with complex condition using And
     * // Note: an And/Or that wraps an On/Using instance is rejected; combine plain
     * // predicates with Filters.expr(...) instead.
     * Join complexJoin = new Join("products p",
     *     new And(
     *         Filters.expr("categories.id = p.category_id"),
     *         Filters.equal("p.active", true)
     *     ));
     * // SQL: JOIN products p ON ((categories.id = p.category_id) AND (p.active = true))
     * // Note: And wraps each child in parentheses and the whole junction in outer parentheses
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @param joinCondition the join condition; must not be {@code null}. A plain predicate is rendered with an {@code ON} prefix; an explicit
     *            {@link On} or {@code @Beta} {@link Using} supplies its own keyword.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank; if {@code joinCondition} is {@code null};
     *                                  or if {@code joinCondition} is or contains a
     *                                  {@link Criteria}, a null operator, a SQL clause, an {@link SqlExpression} whose text begins with
     *                                  {@code ON} or {@code USING}, a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand (a comparison wrapping one, such as {@code a = ANY (subquery)}, is accepted), a standalone {@link SubQuery}, or a blank {@link SqlExpression}
     */
    public Join(final String joinEntity, final Condition joinCondition) {
        this(Operator.JOIN, joinEntity, joinCondition);
    }

    /**
     * Creates a JOIN clause with the specified operator, table or entity, and condition.
     * This protected constructor is used by subclasses to create specific join types
     * with conditions.
     * 
     * <p><b>Usage Example (subclass pattern):</b></p>
     * <pre>{@code
     * // Used internally by LeftJoin
     * public class LeftJoin extends Join {
     *     public LeftJoin(String joinEntity, Condition joinCondition) {
     *         super(Operator.LEFT_JOIN, joinEntity, joinCondition);
     *     }
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @param joinCondition the join condition. A plain predicate is rendered with an {@code ON} prefix; an explicit
     *            {@link On} or {@code @Beta} {@link Using} supplies its own keyword. Must be {@code null} for
     *            {@link Operator#CROSS_JOIN}/{@link Operator#NATURAL_JOIN} and non-{@code null} for every other JOIN operator.
     * @throws IllegalArgumentException if {@code operator} is {@code null} or is not a JOIN operator; if {@code joinEntity} is {@code null}, empty, or blank;
     *                                  if {@code operator} is {@link Operator#CROSS_JOIN} or {@link Operator#NATURAL_JOIN} and
     *                                  {@code joinCondition} is non-{@code null}; if {@code operator} is any other JOIN operator and
     *                                  {@code joinCondition} is {@code null}; or if {@code joinCondition} is or contains a
     *                                  {@link Criteria}, a null operator, a SQL clause, an {@link SqlExpression} whose text begins with
     *                                  {@code ON} or {@code USING}, a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand (a comparison wrapping one, such as {@code a = ANY (subquery)}, is accepted), a standalone {@link SubQuery}, or a blank {@link SqlExpression}
     */
    protected Join(final Operator operator, final String joinEntity, final Condition joinCondition) {
        this(operator, Collections.singletonList(joinEntity), joinCondition);
    }

    /**
     * Creates a JOIN clause with multiple tables or entities and a condition.
     * Uses the default JOIN operator. This form allows joining multiple tables
     * in a single join clause, though chaining individual joins is often clearer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple tables with ON condition
     * List<String> tables = Arrays.asList("orders o", "customers c");
     * Join multiJoin = new Join(tables,
     *     new On("o.customer_id", "c.id"));
     * // SQL: JOIN (orders o CROSS JOIN customers c) ON o.customer_id = c.id
     *
     * // Join multiple tables with a compound condition built from SqlExpressions
     * Join exprMultiJoin = new Join(tables,
     *     new And(
     *         Filters.expr("o.customer_id = c.id"),
     *         Filters.expr("o.status = 'active'")
     *     ));
     * // SQL: JOIN (orders o CROSS JOIN customers c) ON ((o.customer_id = c.id) AND (o.status = 'active'))
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param joinCondition the join condition; must not be {@code null}. A plain predicate is rendered with an {@code ON} prefix; an explicit
     *            {@link On} or {@code @Beta} {@link Using} supplies its own keyword.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null}, empty, or blank elements;
     *                                  if {@code joinCondition} is {@code null};
     *                                  or if {@code joinCondition} is or contains a {@link Criteria}, a null operator, a SQL clause,
     *                                  an {@link SqlExpression} whose text begins
     *                                  with {@code ON} or {@code USING}, a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand (a comparison wrapping one, such as {@code a = ANY (subquery)}, is accepted), a standalone {@link SubQuery}, or a blank {@link SqlExpression}
     */
    public Join(final Collection<String> joinEntities, final Condition joinCondition) {
        this(Operator.JOIN, joinEntities, joinCondition);
    }

    /**
     * Creates a JOIN clause with the specified operator, multiple tables or entities, and condition.
     * This protected constructor provides the base implementation for all join operations,
     * allowing subclasses to specify their join type while reusing the common logic.
     * 
     * <p><b>Usage Example (subclass pattern):</b></p>
     * <pre>{@code
     * // Used internally by subclasses
     * public class RightJoin extends Join {
     *     public RightJoin(Collection<String> joinEntities, Condition joinCondition) {
     *         super(Operator.RIGHT_JOIN, joinEntities, joinCondition);
     *     }
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntities the collection of tables or entities to join with.
     * @param joinCondition the join condition. A plain predicate is rendered with an {@code ON} prefix; an explicit
     *            {@link On} or {@code @Beta} {@link Using} supplies its own keyword. Must be {@code null} for
     *            {@link Operator#CROSS_JOIN}/{@link Operator#NATURAL_JOIN} and non-{@code null} for every other JOIN operator.
     * @throws IllegalArgumentException if {@code operator} is {@code null} or is not a JOIN operator; if {@code joinEntities} is
     *                                  {@code null} or empty, or contains {@code null}, empty, or blank elements;
     *                                  if {@code operator} is {@link Operator#CROSS_JOIN} or {@link Operator#NATURAL_JOIN} and
     *                                  {@code joinCondition} is non-{@code null}; if {@code operator} is any other JOIN operator and
     *                                  {@code joinCondition} is {@code null}; or
     *                                  if {@code joinCondition} is or contains a {@link Criteria}, a null operator,
     *                                  a SQL clause, an {@link SqlExpression} whose text begins with {@code ON} or {@code USING},
     *                                  a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery
     *                                  operand, a standalone {@link SubQuery}, or a blank {@link SqlExpression}
     */
    protected Join(final Operator operator, final Collection<String> joinEntities, final Condition joinCondition) {
        super(operator);

        if (!isJoinOperator(operator)) {
            throw new IllegalArgumentException(
                    "Join operator must be JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, CROSS JOIN, INNER JOIN, or NATURAL JOIN, but was: " + operator);
        }

        // Validate the entities before the condition policy so that a null/blank entity is reported as such
        // rather than as a missing (or unexpected) join predicate.
        this.joinEntities = copyAndValidateJoinEntities(joinEntities);

        if ((operator == Operator.CROSS_JOIN || operator == Operator.NATURAL_JOIN) && joinCondition != null) {
            throw new IllegalArgumentException(operator + " derives its row combinations without an explicit join condition");
        }

        if (operator != Operator.CROSS_JOIN && operator != Operator.NATURAL_JOIN && joinCondition == null) {
            throw new IllegalArgumentException(operator + " requires a non-null ON/USING predicate; use CROSS JOIN for an unconditional join");
        }

        this.condition = validateJoinCondition(joinCondition);
    }

    /**
     * Copies {@code joinEntities} into a new list, rejecting a {@code null} or empty collection and any
     * {@code null}, empty, or blank element.
     *
     * @param joinEntities the tables or entities to join with
     * @return a mutable copy of the validated join entities
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains a
     *         {@code null}, empty, or blank element
     */
    private static List<String> copyAndValidateJoinEntities(final Collection<String> joinEntities) {
        N.checkArgNotEmpty(joinEntities, cs.joinEntities);

        final List<String> copy = new ArrayList<>(joinEntities.size());

        for (final String joinEntity : joinEntities) {
            if (Strings.isBlank(joinEntity)) {
                throw new IllegalArgumentException("joinEntity in joinEntities must not be null, empty, or blank");
            }

            copy.add(joinEntity);
        }

        // Validate the owned snapshot as well as the source. A live or otherwise inconsistent
        // collection can report a non-zero size and still yield no elements while being copied.
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("joinEntities must not yield an empty join-entity list");
        }

        return copy;
    }

    /**
     * Validates that {@code joinCondition} is usable as a join predicate: an {@link On} or {@link Using}
     * connector is unwrapped to the condition it carries, and the result must not be or contain a
     * non-predicate component (a {@link Criteria}, a SQL clause, a nested ON/USING connector, an
     * {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand (a comparison wrapping one, such as {@code a = ANY (subquery)}, is accepted), a standalone {@link SubQuery}, or a
     * blank {@link SqlExpression}).
     *
     * @param joinCondition the join condition; may be {@code null} (CROSS/NATURAL joins)
     * @return the validated join condition, or {@code null} when none was supplied
     * @throws IllegalArgumentException if the condition is or contains a non-predicate component
     */
    private static Condition validateJoinCondition(final Condition joinCondition) {
        if (joinCondition != null) {
            final Condition predicate = (joinCondition instanceof On || joinCondition instanceof Using) ? ((Cell) joinCondition).condition() : joinCondition;

            if (containsNonPredicateComponent(predicate)) {
                throw new IllegalArgumentException("Join condition type " + joinCondition.getClass().getName()
                        + " is not allowed: use a predicate without clause, quantified, ON, or USING operators (a blank expression is not a predicate)");
            }
        }

        return joinCondition;
    }

    /**
     * Returns the list of tables or entities involved in this join.
     * Returns an immutable list of the tables that are being joined, including any aliases.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single table join
     * Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
     * List<String> entities = join.joinEntities();
     * // entities = ["orders o"]
     *
     * // Multi-table join
     * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
     *     new On("o.id", "oi.order_id"));
     * List<String> multiEntities = multiJoin.joinEntities();
     * // multiEntities = ["orders o", "order_items oi"]
     *
     * // Edge: the returned list is immutable
     * entities.add("more");   // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an immutable list of join entities
     */
    public ImmutableList<String> joinEntities() {
        if (joinEntities == null) {
            return ImmutableList.empty();
        }

        ImmutableList<String> view = cachedJoinEntitiesView;

        if (view == null) {
            view = ImmutableList.wrap(joinEntities);
            cachedJoinEntitiesView = view;
        }

        return view;
    }

    /**
     * Returns the join condition.
     * Returns the condition that specifies how the tables are related, or {@code null} if no
     * condition was supplied at construction time. Callers that need a more specific subtype
     * must cast explicitly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with ON condition
     * On onCondition = new On("customers.id", "o.customer_id");
     * Join join = new Join("orders o", onCondition);
     * On condition = (On) join.condition();
     * // condition == onCondition (the same On instance is returned)
     *
     * // Conditionless join type (only CrossJoin/NaturalJoin can be built without a predicate)
     * Join crossJoin = new CrossJoin("products");
     * Condition noCondition = crossJoin.condition();
     * // noCondition == null
     *
     * // Edge: the condition is returned as-is; an incompatible cast fails
     * Using bad = (Using) join.condition();   // throws ClassCastException
     * }</pre>
     *
     * @return the join condition, or {@code null} if no condition was specified
     */
    public Condition condition() {
        return condition;
    }

    /**
     * Returns whether this join has a condition.
     * Returns {@code true} if a condition was supplied at construction time, {@code false}
     * otherwise. Unlike {@link #condition()}, which requires a {@code null} check on its
     * result, this method gives a direct boolean answer &mdash; useful for the conditionless
     * {@link CrossJoin} and {@link NaturalJoin}, the only join types that can be built without
     * a predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with ON condition
     * Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
     * join.hasCondition();    // returns true
     *
     * // Conditionless join type
     * Join crossJoin = new CrossJoin("products");
     * crossJoin.hasCondition();   // returns false
     *
     * // Typical use: guard before unwrapping the condition
     * if (join.hasCondition()) {
     *     Condition c = join.condition();   // never null here
     * }
     * }</pre>
     *
     * @return {@code true} if this join has a condition, {@code false} otherwise
     */
    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Returns all parameters from the join condition.
     * Returns any bound parameters used in the join condition. Returns an empty
     * list if there's no condition or the condition has no parameters.
     * This join does not memoize the list itself; it returns the join condition's own
     * {@link Condition#parameters()} result, which that condition may memoize when all of its values are
     * plain scalars. Mutable parameter values such as arrays or {@code Date}s come from the join
     * condition's own per-call defensive copies and are never shared between callers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Condition with a bound value
     * Join valueJoin = new Join("products p", Filters.equal("p.active", true));
     * valueJoin.parameters();    // returns [true]
     *
     * // Edge: an ON condition compares columns and has no bound parameters
     * Join onJoin = new Join("orders o", new On("customers.id", "o.customer_id"));
     * onJoin.parameters();       // returns [] (empty, immutable)
     *
     * // Edge: conditionless join type -> empty list
     * new CrossJoin("products").parameters();   // returns []
     * }</pre>
     *
     * @return an immutable list of parameters from the condition, or an empty immutable list if no condition
     */
    @Override
    public ImmutableList<Object> parameters() {
        return (condition == null) ? ImmutableList.empty() : condition.parameters();
    }

    /**
     * Converts this JOIN clause to its SQL representation, propagating the specified naming policy
     * to the join condition. The output format includes the join operator, the joined entities, and
     * the optional join condition; the join operator keyword and entity strings themselves are emitted
     * verbatim. The condition's SQL representation depends on its type (On, Using, SqlExpression, etc.).
     * A single join entity is rendered bare while multiple entities are combined as a parenthesized
     * {@code CROSS JOIN} tree (e.g. {@code "JOIN (orders o CROSS JOIN customers c) ..."}). A non-{@code On}/{@code Using} condition is
     * prepended with the {@code ON} keyword before being appended.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
     * join.toSql(NamingPolicy.NO_CHANGE);
     * // returns "JOIN orders o ON customers.id = o.customer_id"
     *
     * // Edge: naming policy rewrites property names within the condition
     * Join snake = new Join("orders o", Filters.equal("firstName", "John"));
     * snake.toSql(NamingPolicy.SNAKE_CASE);
     * // returns "JOIN orders o ON first_name = 'John'"
     *
     * // Edge: conditionless join type -> just the operator and entity
     * new CrossJoin("products").toSql(NamingPolicy.NO_CHANGE);   // returns "CROSS JOIN products"
     * }</pre>
     *
     * @param namingPolicy the naming policy passed through to the join condition's {@link Condition#toSql(NamingPolicy)} method;
     *                     if {@code null}, it is normalized to {@link NamingPolicy#NO_CHANGE} before delegation
     * @return the SQL representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
     * @throws IllegalArgumentException if rendering the join condition rejects one of its values (for example a
     *                                  {@code NaN} or infinite {@link Float}/{@link Double}), or if a nested
     *                                  {@link SubQuery} cannot be rendered, as documented for {@link SubQuery#toSql(NamingPolicy)}
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();
        final String entities = renderJoinEntities();

        if (op == null && entities.isEmpty()) {
            // Default (Kryo) state: avoid emitting "null " with a trailing space.
            return condition == null ? Strings.NULL : Strings.NULL + _SPACE + condition.toSql(effectiveNamingPolicy);
        }

        final String opStr = (op == null) ? Strings.NULL : op.toString();
        final String entityPart = entities.isEmpty() ? Strings.EMPTY : _SPACE + entities;
        String condPart = Strings.EMPTY;

        if (condition != null) {
            final String conditionString = condition.toSql(effectiveNamingPolicy);
            condPart = _SPACE + (condition.operator() == Operator.ON || condition.operator() == Operator.USING ? conditionString
                    : Operator.ON.toString() + _SPACE + conditionString);
        }

        return opStr + entityPart + condPart;
    }

    /**
     * Renders a multi-entity operand as a standard joined table. A parenthesized comma list in this
     * position is a MySQL extension and is rejected by several otherwise-supported SQL dialects.
     */
    private String renderJoinEntities() {
        if (joinEntities == null || joinEntities.isEmpty()) {
            return Strings.EMPTY;
        }

        if (joinEntities.size() == 1) {
            return joinEntities.get(0);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('(');

        for (int i = 0, size = joinEntities.size(); i < size; i++) {
            if (i > 0) {
                sb.append(" CROSS JOIN ");
            }

            sb.append(joinEntities.get(i));
        }

        return sb.append(')').toString();
    }

    /**
     * Computes the hash code for this JOIN clause.
     * The hash code is based on the operator, join entities, and condition,
     * ensuring consistent hashing for equivalent joins.
     * It is recomputed on every call because the join condition may contain retained mutable
     * parameter values; caching would preserve a stale transitive hash.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join a = new Join("orders o", new On("a.id", "b.id"));
     * Join b = new Join("orders o", new On("a.id", "b.id"));
     * a.hashCode() == b.hashCode();   // true (same operator, entities, and condition)
     * }</pre>
     *
     * @return hash code based on operator, join entities, and condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + ((joinEntities == null) ? 0 : joinEntities.hashCode());
        h = (h * 31) + ((condition == null) ? 0 : condition.hashCode());

        return h == 0 ? 1 : h;
    }

    /**
     * Checks if this JOIN clause is equal to another object.
     * Two Join instances are equal if they have the same operator, join entities,
     * and condition. Because the runtime class participates in the comparison (via a
     * {@code getClass()} check), a {@code Join} and a subclass such as {@link LeftJoin}
     * are never equal even with identical entities and condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join a = new Join("orders o", new On("a.id", "b.id"));
     * Join b = new Join("orders o", new On("a.id", "b.id"));
     * a.equals(b);    // returns true
     *
     * // Edge: different join entity -> not equal
     * Join c = new Join("customers c", new On("a.id", "b.id"));
     * a.equals(c);    // returns false
     *
     * // Edge: a different join type (operator) -> not equal
     * Join left = new LeftJoin("orders o", new On("a.id", "b.id"));
     * a.equals(left); // returns false
     *
     * a.equals(null); // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is of the same class with the same operator, entities, and condition
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Join other = (Join) obj;
        return N.equals(operator, other.operator) && N.equals(joinEntities, other.joinEntities) && N.equals(condition, other.condition);
    }
}
