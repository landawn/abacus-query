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
 * Represents a NATURAL JOIN clause in SQL queries.
 * 
 * <p>A NATURAL JOIN automatically joins tables based on all columns with the same names 
 * in both tables. It's a special type of equi-join where the join predicate arises 
 * implicitly by comparing all columns in both tables that have the same column names.
 * The result set contains only one column for each pair of equally named columns.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Automatically identifies common column names</li>
 *   <li>Performs equality comparison on all matching columns</li>
 *   <li>Eliminates duplicate columns in the result</li>
 *   <li>No explicit join condition needed for matching columns</li>
 *   <li>Additional conditions act as filters after the natural join</li>
 * </ul>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>Schema changes can silently affect query behavior</li>
 *   <li>Adding columns with common names changes join semantics</li>
 *   <li>Less explicit than other join types</li>
 *   <li>May join on unintended columns if naming conventions overlap</li>
 *   <li>Best used when table relationships are well-defined and stable</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple natural join - automatically joins on common column names
 * NaturalJoin join1 = new NaturalJoin("employees");
 * // Generates: NATURAL JOIN employees
 * // Automatically joins on any columns with identical names
 *
 * // Natural join with additional filter condition
 * NaturalJoin join2 = new NaturalJoin("departments",
 *     Filters.equal("status", "active"));
 * // Generates: NATURAL JOIN departments ON status = 'active'
 * // Natural join on matching columns, then apply status filter
 *
 * // Natural join with Expression condition
 * NaturalJoin join3 = new NaturalJoin("orders",
 *     Filters.expr("order_date > '2024-01-01'"));
 * // Generates: NATURAL JOIN orders ON order_date > '2024-01-01'
 *
 * // Multiple tables natural join
 * List<String> tables = Arrays.asList("employees", "departments");
 * NaturalJoin multiJoin = new NaturalJoin(tables,
 *     Filters.equal("active", true));
 * // Generates: NATURAL JOIN (employees, departments) ON active = true
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see On
 * @see Using
 */
public class NaturalJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NaturalJoin instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NaturalJoin() {
    }

    /**
     * Creates a NATURAL JOIN clause for the specified table or entity.
     * The join will automatically use all columns with matching names between the tables.
     *
     * <p>This constructor creates a pure natural join without additional conditions.
     * The database engine will identify all columns with identical names in both tables
     * and create an implicit equality condition for each pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // If 'orders' and 'customers' both have 'customer_id' column
     * NaturalJoin join = new NaturalJoin("customers");
     * // Generates: NATURAL JOIN customers
     * // Automatically joins on orders.customer_id = customers.customer_id
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty
     */
    public NaturalJoin(final String joinEntity) {
        super(Operator.NATURAL_JOIN, joinEntity);
    }

    /**
     * Creates a NATURAL JOIN clause with an additional condition.
     * The natural join occurs on matching column names, with the condition as an extra filter.
     *
     * <p>The condition parameter acts as an additional filter after the natural join
     * is performed. This is useful when you want to combine the automatic column matching
     * of natural join with specific filtering criteria.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Natural join filtered by date using Filters.greaterThan()
     * NaturalJoin join1 = new NaturalJoin("orders",
     *     Filters.greaterThan("orderDate", "2024-01-01"));
     * // Generates: NATURAL JOIN orders ON orderDate > '2024-01-01'
     * // Natural join on matching columns, then filter by order date
     *
     * // Natural join with Expression condition
     * NaturalJoin join2 = new NaturalJoin("products",
     *     Filters.expr("price > 100 AND stock > 0"));
     * // Generates: NATURAL JOIN products ON price > 100 AND stock > 0
     *
     * // Natural join with complex And condition
     * NaturalJoin join3 = new NaturalJoin("employees",
     *     new And(
     *         Filters.equal("status", "active"),
     *         Filters.greaterThan("hire_date", "2020-01-01")
     *     ));
     * // Generates: NATURAL JOIN employees ON ((status = 'active') AND (hire_date > '2020-01-01'))
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond an additional condition appended after the natural join fragment. Use {@link On} or {@link Using} when the SQL should include those keywords; any other non-{@code null} condition is automatically prefixed with {@code ON}. Any non-clause {@link Condition} is allowed
     *            and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public NaturalJoin(final String joinEntity, final Condition cond) {
        super(Operator.NATURAL_JOIN, joinEntity, cond);
    }

    /**
     * Creates a NATURAL JOIN clause with multiple tables/entities and a condition.
     * The rendered SQL is {@code NATURAL JOIN (t1, t2, ...)} followed by the optional condition;
     * because most databases do not accept a comma-separated list after {@code NATURAL JOIN}, this
     * form is rarely directly executable and is provided mainly for symmetry with the other join
     * subclasses. Prefer chaining individual {@link NaturalJoin} clauses for portable SQL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join customers, orders, and products naturally with filter
     * List<String> tables = Arrays.asList("customers", "orders", "products");
     * NaturalJoin join1 = new NaturalJoin(tables,
     *     Filters.greaterThan("totalAmount", 1000));
     * // Generates: NATURAL JOIN (customers, orders, products) ON totalAmount > 1000
     * // Natural join across all tables on matching columns, filtered by amount
     *
     * // Natural join with Expression condition
     * NaturalJoin join2 = new NaturalJoin(tables,
     *     Filters.expr("status = 'active' AND verified = true"));
     * // Generates: NATURAL JOIN (customers, orders, products) ON status = 'active' AND verified = true
     *
     * // Natural join with complex conditions
     * NaturalJoin join3 = new NaturalJoin(tables,
     *     new And(
     *         Filters.equal("region", "US"),
     *         Filters.greaterThan("created_date", "2024-01-01")
     *     ));
     * // Generates: NATURAL JOIN (customers, orders, products) ON ((region = 'US') AND (created_date > '2024-01-01'))
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param cond an additional condition appended after the natural join fragment. Use {@link On} or {@link Using} when the SQL should include those keywords; any other non-{@code null} condition is automatically prefixed with {@code ON}. Any non-clause {@link Condition} is allowed
     *            and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null} or empty elements,
     *                                  or if {@code cond} is a {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public NaturalJoin(final Collection<String> joinEntities, final Condition cond) {
        super(Operator.NATURAL_JOIN, joinEntities, cond);
    }
}
