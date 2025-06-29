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

package com.landawn.abacus.condition;

import static com.landawn.abacus.util.WD._SPACE;

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
 * and handles the common functionality of specifying join tables and conditions.
 * 
 * <p>A JOIN clause combines rows from two or more tables based on a related column between them.
 * This class supports:
 * <ul>
 *   <li>Simple joins without conditions</li>
 *   <li>Joins with ON conditions</li>
 *   <li>Joins with multiple tables</li>
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
 * <p>Example usage:
 * <pre>{@code
 * // Basic join (usually through subclasses)
 * Join join = new Join("orders");
 * // Generates: JOIN orders
 * 
 * // Join with condition
 * Join joinWithCondition = new Join("orders o", 
 *     new Equal("customers.id", "o.customer_id"));
 * // Generates: JOIN orders o ON customers.id = o.customer_id
 * 
 * // Join multiple tables
 * Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
 *     new Equal("o.id", "oi.order_id"));
 * // Generates: JOIN orders o, order_items oi ON o.id = oi.order_id
 * }</pre>
 * 
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 */
public class Join extends AbstractCondition {

    private List<String> joinEntities;

    private Condition condition;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Join() {
    }

    /**
     * Creates a simple JOIN clause for the specified table/entity.
     * Uses the default JOIN operator without any ON condition.
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * 
     * <p>Example:
     * <pre>{@code
     * Join join = new Join("products");
     * // Generates: JOIN products
     * }</pre>
     */
    public Join(final String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    /**
     * Creates a JOIN clause with the specified operator and table/entity.
     * This protected constructor is used by subclasses to specify the join type.
     *
     * @param operator the join operator (INNER_JOIN, LEFT_JOIN, etc.)
     * @param joinEntity the table or entity to join with
     */
    protected Join(final Operator operator, final String joinEntity) {
        this(operator, joinEntity, null);
    }

    /**
     * Creates a JOIN clause with a condition.
     * Uses the default JOIN operator with an ON condition.
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition, typically comparing columns from both tables.
     * 
     * <p>Example:
     * <pre>{@code
     * Join join = new Join("orders o",
     *     new Equal("customers.id", "o.customer_id"));
     * // Generates: JOIN orders o ON customers.id = o.customer_id
     * }</pre>
     */
    public Join(final String joinEntity, final Condition condition) {
        this(Operator.JOIN, joinEntity, condition);
    }

    /**
     * Creates a JOIN clause with the specified operator, table/entity, and condition.
     * This protected constructor is used by subclasses.
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
     * Uses the default JOIN operator.
     *
     * @param joinEntities the collection of tables or entities to join with
     * @param condition the join condition
     * 
     * <p>Example:
     * <pre>{@code
     * Join join = new Join(Arrays.asList("orders o", "customers c"),
     *     new Equal("o.customer_id", "c.id"));
     * }</pre>
     */
    public Join(final Collection<String> joinEntities, final Condition condition) {
        this(Operator.JOIN, joinEntities, condition);
    }

    /**
     * Creates a JOIN clause with the specified operator, multiple tables/entities, and condition.
     * This protected constructor is used by subclasses.
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
     *
     * @return the list of join entities
     */
    public List<String> getJoinEntities() {
        return joinEntities;
    }

    /**
     * Gets the join condition.
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
     *
     * @return the list of parameters from the condition, or an empty list if no condition
     */
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears all parameters from the join condition.
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this JOIN clause.
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
     *
     * @param namingPolicy the naming policy to apply
     * @return the string representation, e.g., "INNER JOIN orders o ON customers.id = o.customer_id"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return getOperator().toString() + _SPACE + concatPropNames(joinEntities)
                + ((condition == null) ? Strings.EMPTY : (_SPACE + getCondition().toString(namingPolicy)));
    }

    /**
     * Computes the hash code for this JOIN clause.
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
     *
     * @param obj the object to compare with
     * @return true if the object is a Join with the same operator, entities, and condition
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