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
 * // Generates: JOIN (orders o, order_items oi) ON o.id = oi.order_id
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
     * <p><b>Usage Examples:</b></p>
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
     * // Generates: JOIN (orders o, customers c) (ON o.customer_id = c.id) AND (ON c.address_id = a.id)
     *
     * // Join multiple tables with Expression
     * Join exprMultiJoin = new Join(tables,
     *     new And(
     *         Filters.expr("o.customer_id = c.id"),
     *         Filters.expr("o.status = 'active'")
     *     ));
     * // Generates: JOIN (orders o, customers c) (o.customer_id = c.id) AND (o.status = 'active')
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
     * <p><b>Usage Examples:</b></p>
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

        N.checkArgNotEmpty(joinEntities, "joinEntities");

        this.joinEntities = new ArrayList<>(joinEntities);
        this.condition = condition;
    }

    /**
     * Gets the list of tables/entities involved in this join.
     * Returns a defensive copy of the tables that are being joined, including any aliases.
     *
     * @return a copy of the list of join entities
     */
    public List<String> getJoinEntities() {
        return joinEntities == null ? N.emptyList() : new ArrayList<>(joinEntities);
    }

    /**
     * Gets the join condition.
     * Returns the condition that specifies how the tables are related.
     * May return null if no condition was specified (natural join or cross join).
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
     * @param namingPolicy the naming policy to apply
     * @return the string representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return getOperator().toString() + _SPACE + concatPropNames(joinEntities)
                + ((condition == null) ? Strings.EMPTY : (_SPACE + condition.toString(namingPolicy)));
    }

    /**
     * Computes the hash code for this JOIN clause.
     * The hash code is based on the operator, join entities, and condition,
     * ensuring consistent hashing for equivalent joins.
     * 
     * @return the hash code based on operator, join entities, and condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + ((joinEntities == null) ? 0 : joinEntities.hashCode());

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
