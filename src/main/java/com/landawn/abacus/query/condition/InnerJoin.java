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
 * Represents an INNER JOIN clause in SQL queries.
 * An INNER JOIN returns only the rows that have matching values in both tables.
 * Rows from either table that don't have a match in the other table are excluded
 * from the result set. This is the most restrictive type of join and ensures that
 * all returned rows have complete data from both tables.
 * 
 * <p>INNER JOIN is the most common type of join and is used when you want to:
 * <ul>
 *   <li>Combine related data from multiple tables</li>
 *   <li>Return only records that have corresponding data in both tables</li>
 *   <li>Ensure referential integrity in query results</li>
 *   <li>Eliminate orphaned records from results</li>
 *   <li>Perform equi-joins based on matching key values</li>
 * </ul>
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li>Returns only matching rows from both tables</li>
 *   <li>Non-matching rows are completely excluded</li>
 *   <li>Result set size is at most the size of the smaller table</li>
 *   <li>Order of tables doesn't affect the result (commutative)</li>
 *   <li>Can be chained for multi-table joins</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
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
 * 
 * // Complex multi-condition join
 * InnerJoin complexJoin = new InnerJoin("inventory i",
 *     new And(
 *         new Equal("p.product_id", "i.product_id"),
 *         new Equal("p.warehouse_id", "i.warehouse_id"),
 *         new GreaterThan("i.quantity", 0)
 *     ));
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
     * This constructor creates an uninitialized InnerJoin instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    InnerJoin() {
    }

    /**
     * Creates an INNER JOIN clause for the specified table/entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions based on
     * foreign key relationships (if supported by the database).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join without explicit condition
     * InnerJoin join = new InnerJoin("products");
     * // Generates: INNER JOIN products
     *
     * // Join with table alias
     * InnerJoin aliasJoin = new InnerJoin("order_details od");
     * // Generates: INNER JOIN order_details od
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public InnerJoin(final String joinEntity) {
        super(Operator.INNER_JOIN, joinEntity);
    }

    /**
     * Creates an INNER JOIN clause with a join condition.
     * This is the most common form of INNER JOIN, specifying both the table to join
     * and the condition for matching rows. The condition typically compares key columns
     * between the tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join orders with customers (use Expression for column references)
     * InnerJoin customerOrders = new InnerJoin("customers c",
     *     ConditionFactory.expr("orders.customer_id = c.id"));
     * // Generates: INNER JOIN customers c orders.customer_id = c.id
     *
     * // Join with composite key
     * InnerJoin compositeJoin = new InnerJoin("order_items oi",
     *     new And(
     *         ConditionFactory.expr("orders.id = oi.order_id"),
     *         ConditionFactory.expr("orders.version = oi.order_version")
     *     ));
     * // Generates: INNER JOIN order_items oi ((orders.id = oi.order_id) AND (orders.version = oi.order_version))
     *
     * // Join with additional filter conditions
     * InnerJoin filteredJoin = new InnerJoin("products p",
     *     new And(
     *         ConditionFactory.expr("order_items.product_id = p.id"),
     *         new Equal("p.active", true),
     *         new GreaterThan("p.stock", 0)
     *     ));
     * // Generates: INNER JOIN products p ((order_items.product_id = p.id) AND (p.active = true) AND (p.stock > 0))
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     *                  Can be a complex condition using And/Or for multiple criteria.
     * @throws IllegalArgumentException if joinEntity is null or empty, or condition is null
     */
    public InnerJoin(final String joinEntity, final Condition condition) {
        super(Operator.INNER_JOIN, joinEntity, condition);
    }

    /**
     * Creates an INNER JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single INNER JOIN operation, though
     * this syntax is less common than chaining individual joins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables
     * List<String> tables = Arrays.asList("orders o", "customers c");
     * InnerJoin multiJoin = new InnerJoin(tables,
     *     ConditionFactory.expr("o.customer_id = c.id"));
     * // Generates: INNER JOIN orders o, customers c o.customer_id = c.id
     *
     * // Complex multi-table join
     * List<String> entities = Arrays.asList("products p", "categories cat", "suppliers s");
     * InnerJoin complexMulti = new InnerJoin(entities,
     *     new And(
     *         ConditionFactory.expr("p.category_id = cat.id"),
     *         ConditionFactory.expr("p.supplier_id = s.id")
     *     ));
     * // Generates: INNER JOIN products p, categories cat, suppliers s ((p.category_id = cat.id) AND (p.supplier_id = s.id))
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply across all tables.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public InnerJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.INNER_JOIN, joinEntities, condition);
    }
}