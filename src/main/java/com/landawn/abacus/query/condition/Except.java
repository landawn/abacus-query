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
 * Represents an EXCEPT set operation in SQL queries.
 * The EXCEPT operator returns all distinct rows from the first query that are not
 * present in the second query (set difference).
 * 
 * <p>EXCEPT is a powerful set operation that allows you to find records that exist
 * in one result set but not in another. It automatically removes duplicates from the
 * result, similar to using DISTINCT.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns only distinct rows (duplicates are removed)</li>
 *   <li>Both queries must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names from the first query are used in the result</li>
 *   <li>Order of rows is not guaranteed unless ORDER BY is used</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find customers who haven't placed any orders
 * SubQuery customersWithOrders = CF.subQuery(
 *     "SELECT DISTINCT customer_id FROM orders"
 * );
 * 
 * // Main query returns all customers
 * Criteria criteria = CF.criteria()
 *     .where(CF.eq("status", "active"))
 *     .except(customersWithOrders);
 * // Results: active customers who have no orders
 * 
 * // Find products not sold in the last month
 * SubQuery recentlySold = CF.subQuery(
 *     "SELECT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
 * );
 * Criteria allProducts = CF.criteria();
 * allProducts.except(recentlySold);
 * // Results: all products except those sold recently
 * }</pre>
 * 
 * <p>Note: EXCEPT is equivalent to MINUS in some databases (e.g., Oracle).
 * The functionality is the same - returning rows from the first query
 * that don't appear in the second query.</p>
 * 
 * @see Union
 * @see UnionAll
 * @see Intersect
 * @see Minus
 * @see SubQuery
 * @see Clause
 */
public class Except extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Except instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Except() {
    }

    /**
     * Creates a new EXCEPT clause with the specified subquery.
     * The result will contain all rows from the main query that are not present
     * in the subquery results.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Find employees who are not managers
     * SubQuery managers = CF.subQuery(
     *     "SELECT employee_id FROM employees WHERE is_manager = true"
     * );
     * Except notManagers = new Except(managers);
     * 
     * // Use in criteria
     * Criteria criteria = CF.criteria()
     *     .where(CF.eq("department", "Sales"))
     *     .except(managers);
     * // Returns all Sales employees who are not managers
     * 
     * // Find skills not required for a specific job
     * SubQuery requiredSkills = CF.subQuery(
     *     "SELECT skill_id FROM job_requirements WHERE job_id = 123"
     * );
     * Except otherSkills = new Except(requiredSkills);
     * // Use to find all skills except the required ones
     * }</pre>
     * 
     * @param condition the subquery whose results will be excluded from the main query
     * @throws IllegalArgumentException if condition is null
     */
    public Except(final SubQuery condition) {
        super(Operator.EXCEPT, condition);
    }
}