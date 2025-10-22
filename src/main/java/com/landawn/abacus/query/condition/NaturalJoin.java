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
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple natural join
 * NaturalJoin join1 = new NaturalJoin("employees");
 * // Results in: NATURAL JOIN employees
 * 
 * // Natural join with additional condition
 * Condition activeOnly = new Equal("status", "active");
 * NaturalJoin join2 = new NaturalJoin("departments", activeOnly);
 * // Results in: NATURAL JOIN departments WHERE status = 'active'
 * 
 * // Multiple tables natural join
 * List<String> tables = Arrays.asList("employees", "departments");
 * NaturalJoin join3 = new NaturalJoin(tables, condition);
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
     * <p>Example usage:
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
     * <p>Example usage:
     * <pre>{@code
     * // Natural join filtered by date
     * Condition recentOnly = new GreaterThan("orderDate", "2024-01-01");
     * NaturalJoin join = new NaturalJoin("orders", recentOnly);
     * // Generates: NATURAL JOIN orders (orderDate > '2024-01-01')
     * // Natural join on matching columns, then filter by order date
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     *                  Can be a complex condition using And/Or for multiple criteria.
     * @throws IllegalArgumentException if joinEntity is null or empty, or condition is null
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
     * <p>Example usage:
     * <pre>{@code
     * // Join customers, orders, and products naturally
     * List<String> tables = Arrays.asList("customers", "orders", "products");
     * Condition highValue = new GreaterThan("totalAmount", 1000);
     * NaturalJoin join = new NaturalJoin(tables, highValue);
     * // Generates: NATURAL JOIN customers, orders, products (totalAmount > 1000)
     * // Natural join across all tables on matching columns, filtered by amount
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public NaturalJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.NATURAL_JOIN, joinEntities, condition);
    }
}