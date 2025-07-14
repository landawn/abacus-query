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

import java.util.Collection;

/**
 * Represents a LEFT JOIN clause in SQL queries.
 * A LEFT JOIN returns all rows from the left table (first table), and the matched rows
 * from the right table. If there is no match, NULL values are returned for columns
 * from the right table.
 * 
 * <p>LEFT JOIN is commonly used when you want to:
 * <ul>
 *   <li>Include all records from the primary table regardless of matches</li>
 *   <li>Find records in one table that don't have corresponding records in another</li>
 *   <li>Preserve all data from the main table while adding optional related data</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Simple left join
 * LeftJoin join1 = new LeftJoin("orders");
 * // Generates: LEFT JOIN orders
 * 
 * // Left join with condition
 * LeftJoin join2 = new LeftJoin("orders", 
 *     new Equal("customers.id", "orders.customer_id"));
 * // Generates: LEFT JOIN orders ON customers.id = orders.customer_id
 * 
 * // Find all customers, including those without orders
 * LeftJoin customerOrders = new LeftJoin("orders o",
 *     new Equal("c.customer_id", "o.customer_id"));
 * // Generates: LEFT JOIN orders o ON c.customer_id = o.customer_id
 * }</pre>
 * 
 * @see InnerJoin
 * @see RightJoin
 * @see FullJoin
 * @see Join
 */
public class LeftJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    LeftJoin() {
    }

    /**
     * Creates a LEFT JOIN clause for the specified table/entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions.
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * 
     * <p>Example:
     * <pre>{@code
     * LeftJoin join = new LeftJoin("departments");
     * // Generates: LEFT JOIN departments
     * }</pre>
     */
    public LeftJoin(final String joinEntity) {
        super(Operator.LEFT_JOIN, joinEntity);
    }

    /**
     * Creates a LEFT JOIN clause with a join condition.
     * This is the most common form of LEFT JOIN, specifying both the table to join
     * and the condition for matching rows.
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     * 
     * <p>Example:
     * <pre>{@code
     * // Join customers with their orders (including customers with no orders)
     * LeftJoin join = new LeftJoin("orders o",
     *     new Equal("customers.id", "o.customer_id"));
     * // Generates: LEFT JOIN orders o ON customers.id = o.customer_id
     * 
     * // Complex join condition
     * LeftJoin complexJoin = new LeftJoin("order_items oi",
     *     new And(
     *         new Equal("o.id", "oi.order_id"),
     *         new Equal("oi.status", "active")
     *     ));
     * // Generates: LEFT JOIN order_items oi ON o.id = oi.order_id AND oi.status = 'active'
     * }</pre>
     */
    public LeftJoin(final String joinEntity, final Condition condition) {
        super(Operator.LEFT_JOIN, joinEntity, condition);
    }

    /**
     * Creates a LEFT JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single LEFT JOIN operation.
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * 
     * <p>Example:
     * <pre>{@code
     * // Join multiple related tables
     * List<String> tables = Arrays.asList("orders o", "order_items oi");
     * LeftJoin join = new LeftJoin(tables,
     *     new And(
     *         new Equal("c.id", "o.customer_id"),
     *         new Equal("o.id", "oi.order_id")
     *     ));
     * }</pre>
     */
    public LeftJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.LEFT_JOIN, joinEntities, condition);
    }
}