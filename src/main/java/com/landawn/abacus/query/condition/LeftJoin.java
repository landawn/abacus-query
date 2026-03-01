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
 * A LEFT JOIN (also called LEFT OUTER JOIN) returns all rows from the left table (first table),
 * and the matched rows from the right table. If there is no match, NULL values are returned 
 * for columns from the right table. This ensures that no data from the left table is lost,
 * even when there are no corresponding matches in the right table.
 * 
 * <p>LEFT JOIN is commonly used when you want to:
 * <ul>
 *   <li>Include all records from the primary table regardless of matches</li>
 *   <li>Find records in one table that don't have corresponding records in another</li>
 *   <li>Preserve all data from the main table while adding optional related data</li>
 *   <li>Implement "find missing" queries (WHERE right_table.id IS NULL)</li>
 *   <li>Create comprehensive reports that show all entities even without related data</li>
 * </ul>
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li>Returns ALL rows from the left table</li>
 *   <li>Returns matching rows from the right table</li>
 *   <li>Returns NULL for right table columns when no match exists</li>
 *   <li>Result set size is at least the size of the left table</li>
 *   <li>Order matters: LEFT JOIN is not commutative</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple left join
 * LeftJoin join1 = new LeftJoin("orders");
 * // Generates: LEFT JOIN orders
 *
 * // Left join with ON condition
 * LeftJoin customerOrders = new LeftJoin("orders o",
 *     new On("customers.id", "o.customer_id"));
 * // Generates: LEFT JOIN orders o ON customers.id = o.customer_id
 *
 * // Find customers without orders using LEFT JOIN
 * LeftJoin noOrders = new LeftJoin("orders o",
 *     new On("c.id", "o.customer_id"));
 * // Use with WHERE o.customer_id IS NULL to find customers without orders
 *
 * // Complex left join with multiple ON conditions and filters
 * LeftJoin complexJoin = new LeftJoin("order_items oi",
 *     new And(
 *         new On("o.id", "oi.order_id"),
 *         Filters.equal("oi.status", "active"),
 *         Filters.greaterThan("oi.quantity", 0)
 *     ));
 * // Generates: LEFT JOIN order_items oi (ON o.id = oi.order_id) AND (oi.status = 'active') AND (oi.quantity > 0)
 *
 * // Using Expression for custom join logic
 * LeftJoin exprJoin = new LeftJoin("orders o",
 *     Filters.expr("customers.id = o.customer_id"));
 * // Generates: LEFT JOIN orders o customers.id = o.customer_id
 * // Note: Expression conditions don't add ON keyword
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see On
 * @see Using
 */
public class LeftJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized LeftJoin instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    LeftJoin() {
    }

    /**
     * Creates a LEFT JOIN clause for the specified table or entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions based on
     * foreign key relationships (if supported by the database).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple left join without condition
     * LeftJoin join = new LeftJoin("departments");
     * // Generates: LEFT JOIN departments
     * 
     * // Left join with table alias
     * LeftJoin aliasJoin = new LeftJoin("employee_departments ed");
     * // Generates: LEFT JOIN employee_departments ed
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public LeftJoin(final String joinEntity) {
        super(Operator.LEFT_JOIN, joinEntity);
    }

    /**
     * Creates a LEFT JOIN clause with a join condition.
     * This is the most common form of LEFT JOIN, specifying both the table to join
     * and the condition for matching rows. All rows from the left table are preserved,
     * with NULL values for non-matching rows from the right table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join customers with their orders using ON
     * LeftJoin customerOrders = new LeftJoin("orders o",
     *     new On("customers.id", "o.customer_id"));
     * // Generates: LEFT JOIN orders o ON customers.id = o.customer_id
     *
     * // Find all employees with their departments
     * LeftJoin empDept = new LeftJoin("departments d",
     *     new On("employees.dept_id", "d.id"));
     * // Generates: LEFT JOIN departments d ON employees.dept_id = d.id
     *
     * // Complex join with ON condition and filtering
     * LeftJoin activeItems = new LeftJoin("order_items oi",
     *     new And(
     *         new On("orders.id", "oi.order_id"),
     *         Filters.equal("oi.status", "active"),
     *         Filters.greaterThan("oi.created_date", "2023-01-01")
     *     ));
     * // Generates: LEFT JOIN order_items oi (ON orders.id = oi.order_id) AND (oi.status = 'active') AND (oi.created_date > '2023-01-01')
     *
     * // Using Expression for custom join logic
     * LeftJoin exprJoin = new LeftJoin("orders o",
     *     Filters.expr("customers.id = o.customer_id AND o.amount > 100"));
     * // Generates: LEFT JOIN orders o customers.id = o.customer_id AND o.amount > 100
     * // Note: Expression conditions don't add ON keyword
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition, typically an {@link On} condition for column equality;
     *            any {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public LeftJoin(final String joinEntity, final Condition condition) {
        super(Operator.LEFT_JOIN, joinEntity, condition);
    }

    /**
     * Creates a LEFT JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single LEFT JOIN operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables with ON conditions
     * List<String> tables = Arrays.asList("orders o", "order_items oi");
     * LeftJoin join = new LeftJoin(tables,
     *     new And(
     *         new On("c.id", "o.customer_id"),
     *         new On("o.id", "oi.order_id")
     *     ));
     * // Generates: LEFT JOIN (orders o, order_items oi) (ON c.id = o.customer_id) AND (ON o.id = oi.order_id)
     *
     * // Using Expression for multiple tables
     * LeftJoin exprJoin = new LeftJoin(tables,
     *     Filters.expr("c.id = o.customer_id AND o.id = oi.order_id"));
     * // Generates: LEFT JOIN (orders o, order_items oi) c.id = o.customer_id AND o.id = oi.order_id
     * // Note: Expression conditions don't add ON keyword
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition, typically an {@link On} condition for column equality;
     *            any {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public LeftJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.LEFT_JOIN, joinEntities, condition);
    }
}
