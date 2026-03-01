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
 * // Inner join with ON condition
 * InnerJoin join2 = new InnerJoin("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // Generates: INNER JOIN orders o ON customers.id = o.customer_id
 *
 * // Join customers with their orders (only customers who have orders)
 * InnerJoin customerOrders = new InnerJoin("orders o",
 *     new And(
 *         new On("c.id", "o.customer_id"),
 *         Filters.equal("o.status", "completed")
 *     ));
 * // Generates: INNER JOIN orders o (ON c.id = o.customer_id) AND (o.status = 'completed')
 *
 * // Complex multi-condition join
 * InnerJoin complexJoin = new InnerJoin("inventory i",
 *     new And(
 *         new On("p.product_id", "i.product_id"),
 *         new On("p.warehouse_id", "i.warehouse_id"),
 *         Filters.greaterThan("i.quantity", 0)
 *     ));
 * // Generates: INNER JOIN inventory i (ON p.product_id = i.product_id) AND (ON p.warehouse_id = i.warehouse_id) AND (i.quantity > 0)
 *
 * // Using Expression for custom join logic
 * InnerJoin exprJoin = new InnerJoin("customers c",
 *     Filters.expr("orders.customer_id = c.id"));
 * // Generates: INNER JOIN customers c orders.customer_id = c.id
 * // Note: Expression conditions don't add ON keyword
 * }</pre>
 * 
 * @see Join
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see On
 * @see Using
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
     * Creates an INNER JOIN clause for the specified table or entity.
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
     * // Join orders with customers using ON
     * InnerJoin customerOrders = new InnerJoin("customers c",
     *     new On("orders.customer_id", "c.id"));
     * // Generates: INNER JOIN customers c ON orders.customer_id = c.id
     *
     * // Join with composite key using multiple ON conditions
     * InnerJoin compositeJoin = new InnerJoin("order_items oi",
     *     new And(
     *         new On("orders.id", "oi.order_id"),
     *         new On("orders.version", "oi.order_version")
     *     ));
     * // Generates: INNER JOIN order_items oi (ON orders.id = oi.order_id) AND (ON orders.version = oi.order_version)
     *
     * // Join with ON condition and additional filter conditions
     * InnerJoin filteredJoin = new InnerJoin("products p",
     *     new And(
     *         new On("order_items.product_id", "p.id"),
     *         Filters.equal("p.active", true),
     *         Filters.greaterThan("p.stock", 0)
     *     ));
     * // Generates: INNER JOIN products p (ON order_items.product_id = p.id) AND (p.active = true) AND (p.stock > 0)
     *
     * // Using Expression for custom join logic
     * InnerJoin exprJoin = new InnerJoin("customers c",
     *     Filters.expr("orders.customer_id = c.id"));
     * // Generates: INNER JOIN customers c orders.customer_id = c.id
     * // Note: Expression conditions don't add ON keyword
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond the join condition, typically an {@link On} condition for column equality;
     *            any {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public InnerJoin(final String joinEntity, final Condition cond) {
        super(Operator.INNER_JOIN, joinEntity, cond);
    }

    /**
     * Creates an INNER JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single INNER JOIN operation, though
     * this syntax is less common than chaining individual joins.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables with ON condition
     * List<String> tables = Arrays.asList("orders o", "customers c");
     * InnerJoin multiJoin = new InnerJoin(tables,
     *     new On("o.customer_id", "c.id"));
     * // Generates: INNER JOIN (orders o, customers c) ON o.customer_id = c.id
     *
     * // Complex multi-table join with multiple ON conditions
     * List<String> entities = Arrays.asList("products p", "categories cat", "suppliers s");
     * InnerJoin complexMulti = new InnerJoin(entities,
     *     new And(
     *         new On("p.category_id", "cat.id"),
     *         new On("p.supplier_id", "s.id")
     *     ));
     * // Generates: INNER JOIN (products p, categories cat, suppliers s) (ON p.category_id = cat.id) AND (ON p.supplier_id = s.id)
     *
     * // Using Expression for multiple tables
     * InnerJoin exprMulti = new InnerJoin(tables,
     *     Filters.expr("o.customer_id = c.id AND o.status = 'active'"));
     * // Generates: INNER JOIN (orders o, customers c) o.customer_id = c.id AND o.status = 'active'
     * // Note: Expression conditions don't add ON keyword
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param cond the join condition, typically an {@link On} condition for column equality;
     *            any {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public InnerJoin(final Collection<String> joinEntities, final Condition cond) {
        super(Operator.INNER_JOIN, joinEntities, cond);
    }
}
