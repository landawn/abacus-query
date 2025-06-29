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

import java.util.Collection;

/**
 * Represents an INNER JOIN clause in SQL queries.
 * An INNER JOIN returns only the rows that have matching values in both tables.
 * Rows from either table that don't have a match in the other table are excluded
 * from the result set.
 * 
 * <p>INNER JOIN is the most common type of join and is used when you want to:
 * <ul>
 *   <li>Combine related data from multiple tables</li>
 *   <li>Return only records that have corresponding data in both tables</li>
 *   <li>Ensure referential integrity in query results</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Simple inner join
 * InnerJoin join1 = new InnerJoin("orders");
 * // Generates: INNER JOIN orders
 * 
 * // Inner join with condition
 * InnerJoin join2 = new InnerJoin("orders o", 
 *     new Equal("customers.id", "o.customer_id"));
 * // Generates: INNER JOIN orders o ON customers.id = o.customer_id
 * 
 * // Join customers with their orders (only customers who have orders)
 * InnerJoin customerOrders = new InnerJoin("orders o",
 *     new And(
 *         new Equal("c.customer_id", "o.customer_id"),
 *         new Equal("o.status", "completed")
 *     ));
 * // Generates: INNER JOIN orders o ON c.customer_id = o.customer_id AND o.status = 'completed'
 * }</pre>
 * 
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see Join
 */
public class InnerJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    InnerJoin() {
    }

    /**
     * Creates an INNER JOIN clause for the specified table/entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions.
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * 
     * <p>Example:
     * <pre>{@code
     * InnerJoin join = new InnerJoin("products");
     * // Generates: INNER JOIN products
     * }</pre>
     */
    public InnerJoin(final String joinEntity) {
        super(Operator.INNER_JOIN, joinEntity);
    }

    /**
     * Creates an INNER JOIN clause with a join condition.
     * This is the most common form of INNER JOIN, specifying both the table to join
     * and the condition for matching rows.
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     * 
     * <p>Example:
     * <pre>{@code
     * // Join orders with customers
     * InnerJoin join = new InnerJoin("customers c",
     *     new Equal("orders.customer_id", "c.id"));
     * // Generates: INNER JOIN customers c ON orders.customer_id = c.id
     * 
     * // Complex join with multiple conditions
     * InnerJoin complexJoin = new InnerJoin("order_items oi",
     *     new And(
     *         new Equal("orders.id", "oi.order_id"),
     *         new GreaterThan("oi.quantity", 0),
     *         new Equal("oi.status", "active")
     *     ));
     * // Generates: INNER JOIN order_items oi ON orders.id = oi.order_id 
     * //            AND oi.quantity > 0 AND oi.status = 'active'
     * }</pre>
     */
    public InnerJoin(final String joinEntity, final Condition condition) {
        super(Operator.INNER_JOIN, joinEntity, condition);
    }

    /**
     * Creates an INNER JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single INNER JOIN operation.
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * 
     * <p>Example:
     * <pre>{@code
     * // Join multiple related tables
     * List<String> tables = Arrays.asList("orders o", "customers c");
     * InnerJoin join = new InnerJoin(tables,
     *     new Equal("o.customer_id", "c.id"));
     * }</pre>
     */
    public InnerJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.INNER_JOIN, joinEntities, condition);
    }
}