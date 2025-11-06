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
 *     new Equal("status", "active"));
 * // Generates: NATURAL JOIN departments (status = 'active')
 * // Natural join on matching columns, then apply status filter
 *
 * // Natural join with Expression condition
 * NaturalJoin join3 = new NaturalJoin("orders",
 *     ConditionFactory.expr("order_date > '2024-01-01'"));
 * // Generates: NATURAL JOIN orders order_date > '2024-01-01'
 *
 * // Multiple tables natural join
 * List<String> tables = Arrays.asList("employees", "departments");
 * NaturalJoin multiJoin = new NaturalJoin(tables,
 *     new Equal("active", true));
 * // Generates: NATURAL JOIN employees, departments (active = true)
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
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
     * Creates a NATURAL JOIN clause for the specified table/entity.
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
     * @throws IllegalArgumentException if joinEntity is null or empty
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
     * // Natural join filtered by date using GreaterThan condition
     * NaturalJoin join1 = new NaturalJoin("orders",
     *     new GreaterThan("orderDate", "2024-01-01"));
     * // Generates: NATURAL JOIN orders (orderDate > '2024-01-01')
     * // Natural join on matching columns, then filter by order date
     *
     * // Natural join with Expression condition
     * NaturalJoin join2 = new NaturalJoin("products",
     *     ConditionFactory.expr("price > 100 AND stock > 0"));
     * // Generates: NATURAL JOIN products price > 100 AND stock > 0
     *
     * // Natural join with complex And condition
     * NaturalJoin join3 = new NaturalJoin("employees",
     *     new And(
     *         new Equal("status", "active"),
     *         new GreaterThan("hire_date", "2020-01-01")
     *     ));
     * // Generates: NATURAL JOIN employees (status = 'active') AND (hire_date > '2020-01-01')
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the additional filter condition applied after the natural join.
     *                  Can be Expression, Equal, GreaterThan, or any complex condition using And/Or.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public NaturalJoin(final String joinEntity, final Condition condition) {
        super(Operator.NATURAL_JOIN, joinEntity, condition);
    }

    /**
     * Creates a NATURAL JOIN clause with multiple tables/entities and a condition.
     * This allows joining multiple tables in a single natural join operation.
     *
     * <p>When joining multiple tables, the natural join is performed sequentially.
     * Each table is joined based on columns with matching names. Care must be taken
     * to ensure the intended columns are matched, especially with multiple tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join customers, orders, and products naturally with filter
     * List<String> tables = Arrays.asList("customers", "orders", "products");
     * NaturalJoin join1 = new NaturalJoin(tables,
     *     new GreaterThan("totalAmount", 1000));
     * // Generates: NATURAL JOIN customers, orders, products (totalAmount > 1000)
     * // Natural join across all tables on matching columns, filtered by amount
     *
     * // Natural join with Expression condition
     * NaturalJoin join2 = new NaturalJoin(tables,
     *     ConditionFactory.expr("status = 'active' AND verified = true"));
     * // Generates: NATURAL JOIN customers, orders, products status = 'active' AND verified = true
     *
     * // Natural join with complex conditions
     * NaturalJoin join3 = new NaturalJoin(tables,
     *     new And(
     *         new Equal("region", "US"),
     *         new GreaterThan("created_date", "2024-01-01")
     *     ));
     * // Generates: NATURAL JOIN customers, orders, products (region = 'US') AND (created_date > '2024-01-01')
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the additional filter condition to apply after the natural join.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public NaturalJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.NATURAL_JOIN, joinEntities, condition);
    }
}