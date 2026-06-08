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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Base class for SQL JOIN operations.
 * This class provides the foundation for different types of joins (INNER, LEFT, RIGHT, FULL)
 * and handles the common functionality of specifying join tables and conditions. JOIN operations
 * are fundamental to relational databases, allowing you to combine rows from multiple tables
 * based on related columns.
 * 
 * <p>A JOIN clause combines rows from two or more tables based on a related column between them.
 * This class supports:
 * <ul>
 *   <li>Simple joins without an explicit condition (e.g., for {@link CrossJoin} or {@link NaturalJoin})</li>
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
 * // Basic join (usually through subclasses)
 * Join join = new Join("orders");
 * // SQL: JOIN orders
 *
 * // Join with ON condition using On class
 * Join joinWithCondition = new Join("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // SQL: JOIN orders o ON customers.id = o.customer_id
 *
 * // Join with Expression for custom conditions
 * Join exprJoin = new Join("orders o",
 *     Filters.expr("customers.id = o.customer_id"));
 * // SQL: JOIN orders o ON customers.id = o.customer_id
 *
 * // Join multiple tables
 * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
 *     new On("o.id", "oi.order_id"));
 * // SQL: JOIN (orders o, order_items oi) ON o.id = oi.order_id
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

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Single-slot toString cache pairing a naming policy with its rendered string (performance only). */
    private transient volatile CachedToString cachedTostring;

    /** Lazily memoized unmodifiable view of {@link #joinEntities} (performance only). */
    private transient List<String> cachedJoinEntitiesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Join instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Join() {
    }

    /**
     * Creates a simple JOIN clause for the specified table or entity.
     * Uses the default {@link Operator#JOIN} operator without any join condition. This form is rarely used
     * directly; most databases require an explicit {@code ON} or {@code USING} clause for a plain
     * {@code JOIN}, so this constructor is typically used to build a join fragment incrementally
     * and combine it with a separately specified condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join (rarely used directly)
     * Join join = new Join("products");
     * // SQL: JOIN products
     *
     * // With alias
     * Join aliasJoin = new Join("product_categories pc");
     * // SQL: JOIN product_categories pc
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty
     */
    public Join(final String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    /**
     * Creates a JOIN clause with the specified operator and table or entity.
     * This protected constructor is used by subclasses to specify the join type
     * (INNER, LEFT, RIGHT, FULL, CROSS, NATURAL) while reusing the common join logic.
     *
     * @param operator the join operator (e.g. {@code INNER_JOIN}, {@code LEFT_JOIN})
     * @param joinEntity the table or entity to join with
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty
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
     * // Join with Expression for custom condition
     * Join exprJoin = new Join("orders o",
     *     Filters.expr("customers.id = o.customer_id"));
     * // SQL: JOIN orders o ON customers.id = o.customer_id
     *
     * // Join with complex condition using And
     * Join complexJoin = new Join("products p",
     *     new And(
     *         new On("categories.id", "p.category_id"),
     *         Filters.equal("p.active", true)
     *     ));
     * // SQL: JOIN products p ON ((ON categories.id = p.category_id) AND (p.active = true))
     * // Note: And wraps each child in parentheses and the whole junction in outer parentheses
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should
     *            include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public Join(final String joinEntity, final Condition cond) {
        this(Operator.JOIN, joinEntity, cond);
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
     *     public LeftJoin(String joinEntity, Condition cond) {
     *         super(Operator.LEFT_JOIN, joinEntity, cond);
     *     }
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntity the table or entity to join with
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should
     *            include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    protected Join(final Operator operator, final String joinEntity, final Condition cond) {
        this(operator, Collections.singletonList(joinEntity), cond);
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
     * // SQL: JOIN (orders o, customers c) ON o.customer_id = c.id
     *
     * // Join multiple tables with Expression
     * Join exprMultiJoin = new Join(tables,
     *     new And(
     *         Filters.expr("o.customer_id = c.id"),
     *         Filters.expr("o.status = 'active'")
     *     ));
     * // SQL: JOIN (orders o, customers c) ON ((o.customer_id = c.id) AND (o.status = 'active'))
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should
     *            include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null} or empty elements,
     *                                  or if {@code cond} is a {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public Join(final Collection<String> joinEntities, final Condition cond) {
        this(Operator.JOIN, joinEntities, cond);
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
     *     public RightJoin(Collection<String> joinEntities, Condition cond) {
     *         super(Operator.RIGHT_JOIN, joinEntities, cond);
     *     }
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntities the collection of tables or entities to join with
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should
     *            include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains
     *                                  {@code null} or empty elements, or if {@code cond} is a {@link Criteria},
     *                                  a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    protected Join(final Operator operator, final Collection<String> joinEntities, final Condition cond) {
        super(operator);

        this.joinEntities = copyAndValidateJoinEntities(joinEntities);
        this.condition = validateJoinCondition(cond);
    }

    private static List<String> copyAndValidateJoinEntities(final Collection<String> joinEntities) {
        N.checkArgNotEmpty(joinEntities, "joinEntities");

        final List<String> copy = new ArrayList<>(joinEntities.size());

        for (final String joinEntity : joinEntities) {
            N.checkArgNotEmpty(joinEntity, "joinEntity in joinEntities");
            copy.add(joinEntity);
        }

        return copy;
    }

    private static Condition validateJoinCondition(final Condition cond) {
        if (cond != null && (cond instanceof Criteria || isClause(cond) || (cond instanceof Expression && isOnOrUsing(cond)))) {
            throw new IllegalArgumentException("Join condition cannot be a SQL clause: " + cond.operator());
        }

        return cond;
    }

    /**
     * Gets the list of tables or entities involved in this join.
     * Returns an unmodifiable view of the tables that are being joined, including any aliases.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single table join
     * Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
     * List<String> entities = join.getJoinEntities();
     * // entities = ["orders o"]
     *
     * // Multi-table join
     * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
     *     new On("o.id", "oi.order_id"));
     * List<String> multiEntities = multiJoin.getJoinEntities();
     * // multiEntities = ["orders o", "order_items oi"]
     *
     * // Edge: the returned view is unmodifiable
     * entities.add("more");   // throws UnsupportedOperationException
     * }</pre>
     *
     * @return an unmodifiable view of the list of join entities
     */
    public List<String> getJoinEntities() {
        if (joinEntities == null) {
            return N.emptyList();
        }

        List<String> view = cachedJoinEntitiesView;

        if (view == null) {
            view = Collections.unmodifiableList(joinEntities);
            cachedJoinEntitiesView = view;
        }

        return view;
    }

    /**
     * Gets the join condition.
     * Returns the condition that specifies how the tables are related, or {@code null} if no
     * condition was supplied at construction time. Callers that need a more specific subtype
     * must cast explicitly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with ON condition
     * On onCondition = new On("customers.id", "o.customer_id");
     * Join join = new Join("orders o", onCondition);
     * On condition = (On) join.getCondition();
     * // condition == onCondition (the same On instance is returned)
     *
     * // Join without condition
     * Join simpleJoin = new Join("products");
     * Condition noCondition = simpleJoin.getCondition();
     * // noCondition == null
     *
     * // Edge: the condition is returned as-is; an incompatible cast fails
     * Using bad = (Using) join.getCondition();   // throws ClassCastException
     * }</pre>
     *
     * @return the join condition, or {@code null} if no condition was specified
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Gets all parameters from the join condition.
     * Returns any bound parameters used in the join condition. Returns an empty
     * list if there's no condition or the condition has no parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Condition with a bound value
     * Join valueJoin = new Join("products p", Filters.equal("p.active", true));
     * valueJoin.getParameters();    // returns [true]
     *
     * // Edge: an ON condition compares columns and has no bound parameters
     * Join onJoin = new Join("orders o", new On("customers.id", "o.customer_id"));
     * onJoin.getParameters();       // returns [] (empty, immutable)
     *
     * // Edge: no condition at all -> empty list
     * new Join("products").getParameters();   // returns []
     * }</pre>
     *
     * @return an immutable list of parameters from the condition, or an empty immutable list if no condition
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
     * Converts this JOIN clause to its string representation, propagating the specified naming policy
     * to the join condition. The output format includes the join operator, the joined entities, and
     * the optional join condition; the join operator keyword and entity strings themselves are emitted
     * verbatim. The condition's string representation depends on its type (On, Using, Expression, etc.).
     * A single join entity is rendered bare while multiple entities are wrapped in parentheses
     * (e.g. {@code "JOIN (orders o, customers c) ..."}). A non-{@code On}/{@code Using} condition is
     * prepended with the {@code ON} keyword before being appended.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
     * join.toString(NamingPolicy.NO_CHANGE);
     * // returns "JOIN orders o ON customers.id = o.customer_id"
     *
     * // Edge: naming policy rewrites property names within the condition
     * Join snake = new Join("orders o", Filters.equal("firstName", "John"));
     * snake.toString(NamingPolicy.SNAKE_CASE);
     * // returns "JOIN orders o ON first_name = 'John'"
     *
     * // Edge: no condition -> just the operator and entity
     * new Join("products").toString(NamingPolicy.NO_CHANGE);   // returns "JOIN products"
     * }</pre>
     *
     * @param namingPolicy the naming policy passed through to the join condition's {@code toString}
     * @return the string representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final CachedToString cache = cachedTostring;

        if (cache != null && cache.namingPolicy == namingPolicy) {
            return cache.value;
        }

        final String result = doToString(namingPolicy);

        cachedTostring = new CachedToString(namingPolicy, result);

        return result;
    }

    private String doToString(final NamingPolicy namingPolicy) {
        final Operator op = operator();
        final String entities = (joinEntities == null || joinEntities.isEmpty()) ? Strings.EMPTY : concatPropNames(joinEntities);

        if (op == null && entities.isEmpty()) {
            // Default (Kryo) state: avoid emitting "null " with a trailing space.
            return condition == null ? Strings.NULL : Strings.NULL + _SPACE + condition.toString(namingPolicy);
        }

        final String opStr = (op == null) ? Strings.NULL : op.toString();
        final String entityPart = entities.isEmpty() ? Strings.EMPTY : _SPACE + entities;
        String condPart = Strings.EMPTY;

        if (condition != null) {
            final String conditionString = condition.toString(namingPolicy);
            condPart = _SPACE + (condition.operator() == Operator.ON || condition.operator() == Operator.USING ? conditionString
                    : Operator.ON.toString() + _SPACE + conditionString);
        }

        return opStr + entityPart + condPart;
    }

    /**
     * Computes the hash code for this JOIN clause.
     * The hash code is based on the operator, join entities, and condition,
     * ensuring consistent hashing for equivalent joins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join a = new Join("orders o", new On("a.id", "b.id"));
     * Join b = new Join("orders o", new On("a.id", "b.id"));
     * a.hashCode() == b.hashCode();   // true (same operator, entities, and condition)
     *
     * // Edge: a different join entity produces a different hash code
     * Join c = new Join("customers c", new On("a.id", "b.id"));
     * a.hashCode() == c.hashCode();   // (typically) false
     * }</pre>
     *
     * @return hash code based on operator, join entities, and condition
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + ((joinEntities == null) ? 0 : joinEntities.hashCode());
            h = (h * 31) + ((condition == null) ? 0 : condition.hashCode());

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this JOIN clause is equal to another object.
     * Two Join instances are equal if they have the same operator, join entities,
     * and condition. Because the operator participates in the comparison, a {@code Join}
     * and a subclass such as {@link LeftJoin} are never equal even with identical
     * entities and condition.
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
     * @return {@code true} if the object is a Join with the same operator, entities, and condition
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Join other) {
            return N.equals(operator, other.operator) && N.equals(joinEntities, other.joinEntities) && N.equals(condition, other.condition);
        }

        return false;
    }
}
