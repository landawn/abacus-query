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

import static com.landawn.abacus.query.SK._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Array;
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
 *   <li>Simple joins without conditions (natural joins or cross joins)</li>
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
 * // Generates: JOIN orders
 *
 * // Join with ON condition using On class
 * Join joinWithCondition = new Join("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // Generates: JOIN orders o ON customers.id = o.customer_id
 *
 * // Join with Expression for custom conditions
 * Join exprJoin = new Join("orders o",
 *     Filters.expr("customers.id = o.customer_id"));
 * // Generates: JOIN orders o customers.id = o.customer_id
 * // Note: Expression conditions don't add ON keyword
 *
 * // Join multiple tables
 * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
 *     new On("o.id", "oi.order_id"));
 * // Generates: JOIN orders o, order_items oi ON o.id = oi.order_id
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

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Join instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Join() {
    }

    /**
     * Creates a simple JOIN clause for the specified table/entity.
     * Uses the default JOIN operator without any ON condition. This form is rarely used
     * in practice as it relies on implicit join conditions or results in a cross join.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join (rarely used directly)
     * Join join = new Join("products");
     * // Generates: JOIN products
     * 
     * // With alias
     * Join aliasJoin = new Join("product_categories pc");
     * // Generates: JOIN product_categories pc
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public Join(final String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    /**
     * Creates a JOIN clause with the specified operator and table/entity.
     * This protected constructor is used by subclasses to specify the join type
     * (INNER, LEFT, RIGHT, FULL) while reusing the common join logic.
     * 
     * <p>Example usage in subclasses:
     * <pre>{@code
     * // Used internally by InnerJoin
     * protected InnerJoin(String joinEntity) {
     *     super(Operator.INNER_JOIN, joinEntity);
     * }
     * }</pre>
     *
     * @param operator the join operator (INNER_JOIN, LEFT_JOIN, etc.)
     * @param joinEntity the table or entity to join with
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
     * // Generates: JOIN orders o ON customers.id = o.customer_id
     *
     * // Join with Expression for custom condition
     * Join exprJoin = new Join("orders o",
     *     Filters.expr("customers.id = o.customer_id"));
     * // Generates: JOIN orders o customers.id = o.customer_id
     * // Note: Expression conditions don't add ON keyword
     *
     * // Join with complex condition using And
     * Join complexJoin = new Join("products p",
     *     new And(
     *         new On("categories.id", "p.category_id"),
     *         Filters.eq("p.active", true)
     *     ));
     * // Generates: JOIN products p (ON categories.id = p.category_id) AND (p.active = true)
     * // Note: And wraps each condition in parentheses
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition, typically an On condition or Expression comparing columns from both tables.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public Join(final String joinEntity, final Condition condition) {
        this(Operator.JOIN, joinEntity, condition);
    }

    /**
     * Creates a JOIN clause with the specified operator, table/entity, and condition.
     * This protected constructor is used by subclasses to create specific join types
     * with conditions.
     * 
     * <p>Example usage in subclasses:
     * <pre>{@code
     * // Used internally by LeftJoin
     * protected LeftJoin(String joinEntity, Condition condition) {
     *     super(Operator.LEFT_JOIN, joinEntity, condition);
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntity the table or entity to join with
     * @param condition the join condition (can be null)
     */
    protected Join(final Operator operator, final String joinEntity, final Condition condition) {
        this(operator, Array.asList(joinEntity), condition);
    }

    /**
     * Creates a JOIN clause with multiple tables/entities and a condition.
     * Uses the default JOIN operator. This form allows joining multiple tables
     * in a single join clause, though chaining individual joins is often clearer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple tables with ON condition
     * List<String> tables = Arrays.asList("orders o", "customers c");
     * Join multiJoin = new Join(tables,
     *     new And(
     *         new On("o.customer_id", "c.id"),
     *         new On("c.address_id", "a.id")
     *     ));
     * // Generates: JOIN orders o, customers c (ON o.customer_id = c.id) AND (ON c.address_id = a.id)
     *
     * // Join multiple tables with Expression
     * Join exprMultiJoin = new Join(tables,
     *     new And(
     *         Filters.expr("o.customer_id = c.id"),
     *         Filters.expr("o.status = 'active'")
     *     ));
     * // Generates: JOIN orders o, customers c (o.customer_id = c.id) AND (o.status = 'active')
     * // Note: Expression conditions don't add ON keyword
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with
     * @param condition the join condition, typically On conditions or Expressions
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public Join(final Collection<String> joinEntities, final Condition condition) {
        this(Operator.JOIN, joinEntities, condition);
    }

    /**
     * Creates a JOIN clause with the specified operator, multiple tables/entities, and condition.
     * This protected constructor provides the base implementation for all join operations,
     * allowing subclasses to specify their join type while reusing the common logic.
     * 
     * <p>Example usage in subclasses:
     * <pre>{@code
     * // Used internally by subclasses
     * protected RightJoin(Collection<String> joinEntities, Condition condition) {
     *     super(Operator.RIGHT_JOIN, joinEntities, condition);
     * }
     * }</pre>
     *
     * @param operator the join operator
     * @param joinEntities the collection of tables or entities to join with
     * @param condition the join condition (can be null)
     */
    protected Join(final Operator operator, final Collection<String> joinEntities, final Condition condition) {
        super(operator);
        this.joinEntities = new ArrayList<>(joinEntities);
        this.condition = condition;
    }

    /**
     * Gets the list of tables/entities involved in this join.
     * Returns the tables that are being joined, including any aliases.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = new Join(Arrays.asList("orders o", "customers c"), condition);
     * List<String> entities = join.getJoinEntities(); // Returns ["orders o", "customers c"]
     * }</pre>
     *
     * @return the list of join entities
     */
    public List<String> getJoinEntities() {
        return joinEntities;
    }

    /**
     * Gets the join condition.
     * Returns the condition that specifies how the tables are related.
     * May return null if no condition was specified (natural join or cross join).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create join with ON condition
     * On onCondition = new On("a.id", "b.a_id");
     * Join join = new Join("table_b b", onCondition);
     * Condition retrieved = join.getCondition(); // Returns the On condition
     *
     * // Create join with Expression
     * Condition exprCondition = Filters.expr("a.id = b.a_id");
     * Join exprJoin = new Join("table_b b", exprCondition);
     * Condition exprRetrieved = exprJoin.getCondition(); // Returns the Expression
     * }</pre>
     *
     * @param <T> the type of the condition
     * @return the join condition, or null if no condition is specified
     */
    @SuppressWarnings("unchecked")
    public <T extends Condition> T getCondition() {
        return (T) condition;
    }

    /**
     * Gets all parameters from the join condition.
     * Returns any bound parameters used in the join condition. Returns an empty
     * list if there's no condition or the condition has no parameters.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = new Join("orders o",
     *     new And(
     *         new On("c.id", "o.customer_id"),
     *         Filters.gt("o.amount", 1000)
     *     ));
     * List<Object> params = join.getParameters(); // Returns [1000]
     * }</pre>
     *
     * @return the list of parameters from the condition, or an empty list if no condition
     */
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * 
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> parameters = condition.getParameters(); // e.g., [1, 2, 3, 4, 5]
     * condition.clearParameters(); // All parameters become null
     * List<Object> updatedParameters = condition.getParameters(); // Returns [null, null, null, null, null]
     * }</pre>
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this JOIN clause.
     * The copy includes copies of all join entities and the condition,
     * ensuring that modifications to the copy don't affect the original.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join original = new Join("orders o", new On("c.id", "o.customer_id"));
     * Join copy = original.copy();
     * // copy is independent of original
     * }</pre>
     *
     * @param <T> the type of the condition
     * @return a new Join instance with copies of all entities and condition
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final Join copy = super.copy();

        if (joinEntities != null) {
            copy.joinEntities = new ArrayList<>(joinEntities);
        }

        if (condition != null) {
            copy.condition = condition.copy();
        }

        return (T) copy;
    }

    /**
     * Converts this JOIN clause to its string representation according to the specified naming policy.
     * The output format includes the join operator, tables, and optional join condition.
     * The condition's string representation depends on its type (On, Using, Expression, etc.).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join
     * Join j1 = new Join("orders");
     * j1.toString(policy); // "JOIN orders"
     *
     * // Join with ON condition
     * Join j2 = new Join("orders o", new On("c.id", "o.customer_id"));
     * j2.toString(policy); // "JOIN orders o ON c.id = o.customer_id"
     *
     * // Join with Expression condition
     * Join j3 = new Join("orders o", Filters.expr("c.id = o.customer_id"));
     * j3.toString(policy); // "JOIN orders o c.id = o.customer_id"
     *
     * // Multiple tables
     * Join j4 = new Join(Arrays.asList("t1", "t2"), new On("t1.id", "t2.t1_id"));
     * j4.toString(policy); // "JOIN t1, t2 ON t1.id = t2.t1_id"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply
     * @return the string representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return getOperator().toString() + _SPACE + concatPropNames(joinEntities)
                + ((condition == null) ? Strings.EMPTY : (_SPACE + getCondition().toString(namingPolicy)));
    }

    /**
     * Computes the hash code for this JOIN clause.
     * The hash code is based on the operator, join entities, and condition,
     * ensuring consistent hashing for equivalent joins.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition expr = Filters.expr("c.id = o.customer_id");
     * Join j1 = new Join("orders o", expr);
     * Join j2 = new Join("orders o", expr);
     * assert j1.hashCode() == j2.hashCode();
     * }</pre>
     *
     * @return the hash code based on operator, join entities, and condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + joinEntities.hashCode();

        if (condition != null) {
            h = (h * 31) + condition.hashCode();
        }

        return h;
    }

    /**
     * Checks if this JOIN clause is equal to another object.
     * Two Join instances are equal if they have the same operator, join entities,
     * and condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join j1 = new Join("orders o", new On("c.id", "o.customer_id"));
     * Join j2 = new Join("orders o", new On("c.id", "o.customer_id"));
     * assert j1.equals(j2); // true
     *
     * Join j3 = new Join("products p", new On("c.id", "p.category_id"));
     * assert !j1.equals(j3); // false
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