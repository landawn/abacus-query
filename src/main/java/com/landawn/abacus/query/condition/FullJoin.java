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
 * Represents a FULL JOIN (a.k.a. FULL OUTER JOIN) operation in SQL queries.
 * A FULL JOIN returns all rows from both tables: matched rows are combined,
 * and unmatched rows from either side are returned with NULLs filled in for
 * the columns of the other table. It is equivalent to the union of the
 * results of a LEFT JOIN and a RIGHT JOIN on the same condition.
 * 
 * <p>FULL JOIN is useful when you need to see all records from both tables,
 * regardless of whether they have matching values. It's particularly valuable
 * for finding mismatches or gaps in data between related tables.</p>
 * 
 * <p>When no match is found:</p>
 * <ul>
 *   <li>Right-side columns are NULL for left-side rows with no matching right-side row</li>
 *   <li>Left-side columns are NULL for right-side rows with no matching left-side row</li>
 *   <li>All rows from both tables are included in the result set</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Full join to see all users and all orders
 * FullJoin join = new FullJoin("orders",
 *     new On("users.id", "orders.user_id"));
 * // Generates: FULL JOIN orders ON users.id = orders.user_id
 * // Returns:
 * // - Users with orders (matched records)
 * // - Users without orders (NULLs for order columns)
 * // - Orders without users (NULLs for user columns)
 *
 * // Full join to compare two inventory systems
 * FullJoin inventoryJoin = new FullJoin("warehouse_inventory",
 *     new On("online_inventory.product_id", "warehouse_inventory.product_id"));
 * // Shows all products from both systems, highlighting discrepancies
 *
 * // Complex full join with filtering
 * FullJoin complexJoin = new FullJoin("external_data e",
 *     new And(
 *         new On("internal_data.id", "e.id"),
 *         Filters.greaterThan("e.updated_date", "2024-01-01")
 *     ));
 * // Generates: FULL JOIN external_data e ON ((ON internal_data.id = e.id) AND (e.updated_date > '2024-01-01'))
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see On
 * @see Using
 */
public class FullJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized FullJoin instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    FullJoin() {
    }

    /**
     * Creates a FULL JOIN clause for the specified table or entity without a join condition.
     * Most databases require an {@code ON} or {@code USING} clause for a FULL JOIN; supply
     * the condition separately (for example by combining this with another clause) or use
     * {@link #FullJoin(String, Condition)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple full join without condition
     * FullJoin join = new FullJoin("departments");
     * // Generates: FULL JOIN departments
     *
     * // Full join with table alias
     * FullJoin aliasJoin = new FullJoin("employee_departments ed");
     * // Generates: FULL JOIN employee_departments ed
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty
     */
    public FullJoin(final String joinEntity) {
        super(Operator.FULL_JOIN, joinEntity);
    }

    /**
     * Creates a FULL JOIN clause with a join condition.
     * This is the most common form of FULL JOIN, specifying both the table to join
     * and the condition for matching rows. All rows from both tables are preserved,
     * with NULL values for non-matching rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join employees with departments using ON
     * FullJoin empDept = new FullJoin("departments d",
     *     new On("employees.dept_id", "d.id"));
     * // Generates: FULL JOIN departments d ON employees.dept_id = d.id
     *
     * // Find all users and orders, showing orphaned records
     * FullJoin allData = new FullJoin("orders o",
     *     new On("users.id", "o.user_id"));
     * // Generates: FULL JOIN orders o ON users.id = o.user_id
     *
     * // Complex join with ON condition and filtering
     * FullJoin reconcileData = new FullJoin("external_inventory ei",
     *     new And(
     *         new On("internal_inventory.product_id", "ei.product_id"),
     *         Filters.equal("ei.active", true),
     *         Filters.greaterThan("ei.updated_date", "2023-01-01")
     *     ));
     * // Generates: FULL JOIN external_inventory ei ON ((ON internal_inventory.product_id = ei.product_id) AND (ei.active = true) AND (ei.updated_date > '2023-01-01'))
     *
     * // Using Expression for custom join logic
     * FullJoin exprJoin = new FullJoin("departments d",
     *     Filters.expr("employees.dept_id = d.id AND d.active = true"));
     * // Generates: FULL JOIN departments d ON employees.dept_id = d.id AND d.active = true
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond the condition appended after the join target. Use {@link On} or {@link Using} when the SQL should include
     *            those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public FullJoin(final String joinEntity, final Condition cond) {
        super(Operator.FULL_JOIN, joinEntity, cond);
    }

    /**
     * Creates a FULL JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single FULL JOIN operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables with ON conditions
     * List<String> tables = Arrays.asList("employees e", "contractors c");
     * FullJoin join = new FullJoin(tables,
     *     new And(
     *         new On("d.id", "e.dept_id"),
     *         new On("d.id", "c.dept_id")
     *     ));
     * // Generates: FULL JOIN (employees e, contractors c) ON ((ON d.id = e.dept_id) AND (ON d.id = c.dept_id))
     *
     * // Using Expression for multiple tables
     * FullJoin exprJoin = new FullJoin(tables,
     *     Filters.expr("d.id = e.dept_id AND d.id = c.dept_id"));
     * // Generates: FULL JOIN (employees e, contractors c) ON d.id = e.dept_id AND d.id = c.dept_id
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param cond the condition appended after the joined table list. Use {@link On} or {@link Using} when the SQL should include
     *            those keywords. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null} or empty elements,
     *                                  or if {@code cond} is a {@link Criteria}, a SQL clause, or an {@link Expression} whose text begins with {@code ON} or {@code USING}
     */
    public FullJoin(final Collection<String> joinEntities, final Condition cond) {
        super(Operator.FULL_JOIN, joinEntities, cond);
    }
}
