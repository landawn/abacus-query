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

    // For Kryo
    FullJoin() {
    }

    /**
     * Creates a new FULL JOIN with the specified table/entity.
     * This creates a simple FULL JOIN without any condition, which will produce
     * a Cartesian product filtered by WHERE conditions if any.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * FullJoin join = new FullJoin("departments");
     * // SQL: FULL JOIN departments
     * 
     * // Typically used with a WHERE clause later
     * criteria.fullJoin("departments")
     *         .where(CF.eq("employees.dept_id", "departments.id"));
     * }</pre>
     * 
     * @param joinEntity the table or entity name to join
     */
    public FullJoin(final String joinEntity) {
        super(Operator.FULL_JOIN, joinEntity);
    }

    /**
     * Creates a new FULL JOIN with the specified table/entity and join condition.
     * This is the most common form of FULL JOIN, specifying how the tables relate.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Full join employees with departments
     * FullJoin join = new FullJoin("employees", 
     *     CF.eq("departments.id", "employees.dept_id"));
     * // Returns all departments and all employees:
     * // - Matched: departments with their employees
     * // - Unmatched: departments with no employees (NULL employee fields)
     * // - Unmatched: employees with no department (NULL department fields)
     * 
     * // Full join to reconcile two data sources
     * FullJoin reconcile = new FullJoin("external_users",
     *     CF.eq("internal_users.email", "external_users.email"));
     * // Shows all users from both systems for comparison
     * }</pre>
     * 
     * @param joinEntity the table or entity name to join
     * @param condition the join condition
     */
    public FullJoin(final String joinEntity, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntity, condition);
    }

    /**
     * Creates a new FULL JOIN with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single FULL JOIN operation.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Full join multiple related tables
     * List<String> tables = Arrays.asList("employees", "contractors");
     * FullJoin join = new FullJoin(tables, 
     *     CF.eq("departments.id", "person.dept_id"));
     * // Full joins both employees and contractors tables with departments
     * 
     * // Compare multiple data sources
     * Collection<String> sources = Arrays.asList("system_a_data", "system_b_data");
     * FullJoin multiSource = new FullJoin(sources,
     *     CF.eq("master_data.record_id", "source.record_id"));
     * // Shows all records from all systems
     * }</pre>
     * 
     * @param joinEntities the collection of table or entity names to join
     * @param condition the join condition
     */
    public FullJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntities, condition);
    }
}