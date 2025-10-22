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
 * Represents a FULL OUTER JOIN operation in SQL queries.
 * A FULL JOIN returns all rows when there is a match in either table.
 * It combines the results of both LEFT JOIN and RIGHT JOIN.
 * 
 * <p>FULL JOIN is useful when you need to see all records from both tables,
 * regardless of whether they have matching values. It's particularly valuable
 * for finding mismatches or gaps in data between related tables.</p>
 * 
 * <p>When no match is found:</p>
 * <ul>
 *   <li>NULL values are returned for columns from the left table when there's no match</li>
 *   <li>NULL values are returned for columns from the right table when there's no match</li>
 *   <li>All rows from both tables are included in the result set</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Full join to see all users and all orders
 * FullJoin join = new FullJoin("orders", CF.eq("users.id", "orders.user_id"));
 * // Results in: FULL JOIN orders ON users.id = orders.user_id
 * // Returns:
 * // - Users with orders (matched records)
 * // - Users without orders (NULLs for order columns)
 * // - Orders without users (NULLs for user columns)
 * 
 * // Full join to compare two inventory systems
 * FullJoin inventoryJoin = new FullJoin("warehouse_inventory", 
 *     CF.eq("online_inventory.product_id", "warehouse_inventory.product_id"));
 * // Shows all products from both systems, highlighting discrepancies
 * }</pre>
 * 
 * @see Join
 * @see LeftJoin
 * @see RightJoin
 * @see InnerJoin
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
     * Creates a FULL JOIN clause for the specified table/entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions based on
     * foreign key relationships (if supported by the database).
     *
     * <p>Example usage:
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
     * @throws IllegalArgumentException if joinEntity is null or empty
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
     * <p>Example usage:
     * <pre>{@code
     * // Join employees with departments (use Expression for column references)
     * FullJoin empDept = new FullJoin("departments d",
     *     ConditionFactory.expr("employees.dept_id = d.id"));
     * // Generates: FULL JOIN departments d employees.dept_id = d.id
     *
     * // Find all users and orders, showing orphaned records
     * FullJoin allData = new FullJoin("orders o",
     *     ConditionFactory.expr("users.id = o.user_id"));
     * // Generates: FULL JOIN orders o users.id = o.user_id
     *
     * // Complex join with filtering in the join condition
     * FullJoin reconcileData = new FullJoin("external_inventory ei",
     *     new And(
     *         ConditionFactory.expr("internal_inventory.product_id = ei.product_id"),
     *         new Equal("ei.active", true),
     *         new GreaterThan("ei.updated_date", "2023-01-01")
     *     ));
     * // Generates: FULL JOIN external_inventory ei ((internal_inventory.product_id = ei.product_id) AND (ei.active = true) AND (ei.updated_date > '2023-01-01'))
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     *                  Can be a complex condition using And/Or for multiple criteria.
     * @throws IllegalArgumentException if joinEntity is null or empty, or condition is null
     */
    public FullJoin(final String joinEntity, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntity, condition);
    }

    /**
     * Creates a FULL JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single FULL JOIN operation.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Join multiple related tables
     * List<String> tables = Arrays.asList("employees e", "contractors c");
     * FullJoin join = new FullJoin(tables,
     *     new And(
     *         ConditionFactory.expr("d.id = e.dept_id"),
     *         ConditionFactory.expr("d.id = c.dept_id")
     *     ));
     * // Generates: FULL JOIN employees e, contractors c ((d.id = e.dept_id) AND (d.id = c.dept_id))
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public FullJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntities, condition);
    }
}