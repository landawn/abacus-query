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

/**
 * Represents the SQL EXISTS operator for use with subqueries.
 * The EXISTS operator returns true if the subquery returns at least one row, false otherwise.
 * 
 * <p>EXISTS is particularly useful for checking the existence of related records without
 * actually retrieving them. It's often more efficient than using IN with large result sets
 * because it stops processing once it finds the first matching row.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns true if subquery returns any rows, false if no rows</li>
 *   <li>More efficient than IN for large datasets - stops at first match</li>
 *   <li>The SELECT clause in the subquery doesn't matter (SELECT 1, SELECT *, etc.)</li>
 *   <li>Commonly used with correlated subqueries</li>
 *   <li>Can be negated with NOT EXISTS</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find customers who have placed at least one order
 * SubQuery orderExists = CF.subQuery(
 *     "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
 * );
 * Exists hasOrders = new Exists(orderExists);
 * 
 * // Find products that have been reviewed
 * SubQuery reviewExists = CF.subQuery(
 *     "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
 * );
 * Exists hasReviews = new Exists(reviewExists);
 * 
 * // Find departments with employees
 * SubQuery employeeExists = CF.subQuery(
 *     "SELECT 1 FROM employees WHERE employees.dept_id = departments.id"
 * );
 * Exists hasEmployees = new Exists(employeeExists);
 * }</pre>
 * 
 * <p>Performance tip: The SELECT clause in the EXISTS subquery doesn't affect performance
 * (SELECT 1, SELECT *, SELECT column_name are all equivalent) because EXISTS only checks
 * for row existence, not the actual values.</p>
 * 
 * @see NotExists
 * @see SubQuery
 * @see Cell
 * @see In
 */
public class Exists extends Cell {

    // For Kryo
    Exists() {
    }

    /**
     * Creates a new EXISTS condition with the specified subquery.
     * The condition evaluates to true if the subquery returns at least one row.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if employee has any subordinates
     * SubQuery subordinatesQuery = CF.subQuery(
     *     "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id"
     * );
     * Exists hasSubordinates = new Exists(subordinatesQuery);
     * 
     * // Use in a query to find all managers
     * Criteria criteria = CF.criteria()
     *     .where(hasSubordinates);
     * 
     * // Check if product is in any active order
     * SubQuery activeOrderQuery = CF.subQuery(
     *     "SELECT 1 FROM order_items oi " +
     *     "JOIN orders o ON oi.order_id = o.id " +
     *     "WHERE oi.product_id = products.id " +
     *     "AND o.status = 'active'"
     * );
     * Exists inActiveOrder = new Exists(activeOrderQuery);
     * 
     * // More complex: Find users with specific permissions
     * SubQuery permissionQuery = CF.subQuery(
     *     "SELECT 1 FROM user_permissions up " +
     *     "WHERE up.user_id = users.id " +
     *     "AND up.permission = 'admin'"
     * );
     * Exists isAdmin = new Exists(permissionQuery);
     * }</pre>
     * 
     * @param condition the subquery to check for existence of rows
     */
    public Exists(final SubQuery condition) {
        super(Operator.EXISTS, condition);
    }
}