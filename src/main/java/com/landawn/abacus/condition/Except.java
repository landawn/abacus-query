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

package com.landawn.abacus.condition;

/**
 * Represents an EXCEPT set operation in SQL queries.
 * The EXCEPT operator returns all distinct rows from the first query that are not
 * present in the second query (set difference).
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns only distinct rows (duplicates are removed)</li>
 *   <li>Both queries must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names from the first query are used in the result</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find customers who haven't placed any orders
 * SubQuery activeCustomers = new SubQuery(
 *     "SELECT customer_id FROM customers WHERE status = 'active'"
 * );
 * SubQuery customersWithOrders = new SubQuery(
 *     "SELECT DISTINCT customer_id FROM orders"
 * );
 * 
 * // Main query returns active customers
 * Criteria criteria = new Criteria()
 *     .where(CF.eq("status", "active"))
 *     .except(customersWithOrders);
 * 
 * // Find products not sold in the last month
 * SubQuery recentlySold = new SubQuery(
 *     "SELECT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
 * );
 * Criteria allProducts = new Criteria();
 * allProducts.except(recentlySold);
 * }</pre>
 * 
 * <p>Note: EXCEPT is equivalent to MINUS in some databases (e.g., Oracle).</p>
 * 
 * @see Union
 * @see UnionAll
 * @see Intersect
 * @see Minus
 * @see SubQuery
 * @see Clause
 */
public class Except extends Clause {

    // For Kryo
    Except() {
    }

    /**
     * Creates a new EXCEPT clause with the specified subquery.
     * 
     * @param condition the subquery whose results will be excluded from the main query
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Find employees who are not managers
     * SubQuery managers = new SubQuery(
     *     "SELECT employee_id FROM employees WHERE is_manager = true"
     * );
     * Except notManagers = new Except(managers);
     * 
     * // Use in criteria
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("department", "Sales"))
     *     .except(managers);
     * // Returns all Sales employees who are not managers
     * }</pre>
     */
    public Except(final SubQuery condition) {
        super(Operator.EXCEPT, condition);
    }
}