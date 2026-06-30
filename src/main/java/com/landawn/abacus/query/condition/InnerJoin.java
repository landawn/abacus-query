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
 *   <li>Result set size depends on cardinality of the matching keys (can exceed either table for many-to-many joins)</li>
 *   <li>Order of tables doesn't affect the result (commutative)</li>
 *   <li>Can be chained for multi-table joins</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple inner join
 * InnerJoin join1 = new InnerJoin("orders");
 * // SQL: INNER JOIN orders
 *
 * // Inner join with ON condition
 * InnerJoin join2 = new InnerJoin("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // SQL: INNER JOIN orders o ON customers.id = o.customer_id
 *
 * // Join customers with their orders (only customers who have orders)
 * // Note: combine plain predicates with Filters.expr(...); an And/Or that wraps an
 * // On/Using instance is rejected at construction.
 * InnerJoin customerOrders = new InnerJoin("orders o",
 *     new And(
 *         Filters.expr("c.id = o.customer_id"),
 *         Filters.equal("o.status", "completed")
 *     ));
 * // SQL: INNER JOIN orders o ON ((c.id = o.customer_id) AND (o.status = 'completed'))
 *
 * // Complex multi-condition join
 * InnerJoin complexJoin = new InnerJoin("inventory i",
 *     new And(
 *         Filters.expr("p.product_id = i.product_id"),
 *         Filters.expr("p.warehouse_id = i.warehouse_id"),
 *         Filters.greaterThan("i.quantity", 0)
 *     ));
 * // SQL: INNER JOIN inventory i ON ((p.product_id = i.product_id) AND (p.warehouse_id = i.warehouse_id) AND (i.quantity > 0))
 *
 * // Using Expression for custom join logic
 * InnerJoin exprJoin = new InnerJoin("customers c",
 *     Filters.expr("orders.customer_id = c.id"));
 * // SQL: INNER JOIN customers c ON orders.customer_id = c.id
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
     * Creates an INNER JOIN clause for the specified table or entity without a join condition.
     * Most databases require an {@code ON} or {@code USING} clause for an INNER JOIN; supply
     * the condition separately (for example by combining this with another clause) or use
     * {@link #InnerJoin(String, Condition)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join without explicit condition
     * InnerJoin join = new InnerJoin("products");
     * // SQL: INNER JOIN products
     *
     * // Join with table alias
     * InnerJoin aliasJoin = new InnerJoin("order_details od");
     * // SQL: INNER JOIN order_details od
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
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
     * // SQL: INNER JOIN customers c ON orders.customer_id = c.id
     *
     * // Join with composite key using multiple predicates
     * InnerJoin compositeJoin = new InnerJoin("order_items oi",
     *     new And(
     *         Filters.expr("orders.id = oi.order_id"),
     *         Filters.expr("orders.version = oi.order_version")
     *     ));
     * // SQL: INNER JOIN order_items oi ON ((orders.id = oi.order_id) AND (orders.version = oi.order_version))
     *
     * // Join with key comparison and additional filter conditions
     * InnerJoin filteredJoin = new InnerJoin("products p",
     *     new And(
     *         Filters.expr("order_items.product_id = p.id"),
     *         Filters.equal("p.active", true),
     *         Filters.greaterThan("p.stock", 0)
     *     ));
     * // SQL: INNER JOIN products p ON ((order_items.product_id = p.id) AND (p.active = true) AND (p.stock > 0))
     *
     * // Using Expression for custom join logic
     * InnerJoin exprJoin = new InnerJoin("customers c",
     *     Filters.expr("orders.customer_id = c.id"));
     * // SQL: INNER JOIN customers c ON orders.customer_id = c.id
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should include
     *            those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, an {@link Expression} whose text begins with {@code ON} or {@code USING},
     *                                  or an empty predicate (a blank {@link Expression} or empty {@link Junction})
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
     * // SQL: INNER JOIN (orders o, customers c) ON o.customer_id = c.id
     *
     * // Complex multi-table join with multiple predicates
     * List<String> entities = Arrays.asList("products p", "categories cat", "suppliers s");
     * InnerJoin complexMulti = new InnerJoin(entities,
     *     new And(
     *         Filters.expr("p.category_id = cat.id"),
     *         Filters.expr("p.supplier_id = s.id")
     *     ));
     * // SQL: INNER JOIN (products p, categories cat, suppliers s) ON ((p.category_id = cat.id) AND (p.supplier_id = s.id))
     *
     * // Using Expression for multiple tables
     * InnerJoin exprMulti = new InnerJoin(tables,
     *     Filters.expr("o.customer_id = c.id AND o.status = 'active'"));
     * // SQL: INNER JOIN (orders o, customers c) ON o.customer_id = c.id AND o.status = 'active'
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param cond the condition appended after the joined table list. Use {@link On} or {@link Using} when the SQL should include
     *            those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null}, empty, or blank elements,
     *                                  or if {@code cond} is a {@link Criteria}, a SQL clause, an {@link Expression} whose text begins with {@code ON} or {@code USING},
     *                                  or an empty predicate (a blank {@link Expression} or empty {@link Junction})
     */
    public InnerJoin(final Collection<String> joinEntities, final Condition cond) {
        super(Operator.INNER_JOIN, joinEntities, cond);
    }
}
