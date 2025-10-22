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
 * Represents a MINUS clause in SQL queries (also known as EXCEPT in some databases).
 * This class implements the set difference operation, returning rows from the first query
 * that are not present in the second query (subquery).
 * 
 * <p>Key characteristics of MINUS/EXCEPT:
 * <ul>
 *   <li>Removes duplicate rows automatically (returns DISTINCT results)</li>
 *   <li>Both queries must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names don't need to match, but order matters</li>
 *   <li>NULL values are considered equal when comparing rows</li>
 * </ul>
 * 
 * <p>Database support:
 * <ul>
 *   <li>Oracle, DB2: Use MINUS keyword</li>
 *   <li>PostgreSQL, SQL Server, SQLite: Use EXCEPT keyword</li>
 *   <li>MySQL: Does not support MINUS/EXCEPT directly (use NOT IN or LEFT JOIN)</li>
 * </ul>
 * 
 * <p>Common use cases:
 * <ul>
 *   <li>Finding records that exist in one table but not another</li>
 *   <li>Identifying missing data or gaps</li>
 *   <li>Data validation and comparison between datasets</li>
 *   <li>Finding customers without orders, products never sold, etc.</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Find customers who have never placed an order
 * SubQuery allCustomers = new SubQuery("SELECT customer_id FROM customers");
 * SubQuery customersWithOrders = new SubQuery("SELECT DISTINCT customer_id FROM orders");
 * Minus customersWithoutOrders = new Minus(customersWithOrders);
 * // When used with main query:
 * // SELECT customer_id FROM customers
 * // MINUS
 * // SELECT DISTINCT customer_id FROM orders
 * 
 * // Find products not sold in the last month
 * SubQuery allProducts = new SubQuery("SELECT product_id FROM products WHERE active = 'Y'");
 * SubQuery soldProducts = new SubQuery(
 *     "SELECT DISTINCT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
 * );
 * Minus unsoldProducts = new Minus(soldProducts);
 * 
 * // Find employees not assigned to any project
 * SubQuery allEmployees = new SubQuery("SELECT employee_id FROM employees WHERE status = 'ACTIVE'");
 * SubQuery assignedEmployees = new SubQuery("SELECT DISTINCT employee_id FROM project_assignments");
 * Minus unassignedEmployees = new Minus(assignedEmployees);
 * }</pre>
 * 
 * @see Except
 * @see Union
 * @see UnionAll
 * @see Intersect
 * @see SubQuery
 * @see Clause
 */
public class Minus extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Minus instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Minus() {
    }

    /**
     * Creates a new MINUS clause with the specified subquery.
     * The MINUS operation will return rows from the main query that are not
     * present in this subquery. The result automatically excludes duplicates.
     *
     * <p>The MINUS operator performs a set difference operation, equivalent to
     * A - B in set theory. Only rows that appear in the first query but not in
     * the second query will be returned. Both queries must have compatible
     * column structures (same number of columns with compatible types).
     *
     * <p>Example usage:
     * <pre>{@code
     * // Find products that are in inventory but have never been sold
     * SubQuery soldProducts = new SubQuery("SELECT product_id FROM sales");
     * Minus minus = new Minus(soldProducts);
     * // When used with a query selecting from inventory:
     * // SELECT product_id FROM inventory
     * // MINUS
     * // SELECT product_id FROM sales
     *
     * // Find active employees not assigned to any project
     * SubQuery assignedEmployees = new SubQuery("SELECT employee_id FROM project_assignments");
     * Minus unassigned = new Minus(assignedEmployees);
     * // SELECT employee_id FROM employees WHERE status = 'ACTIVE'
     * // MINUS
     * // SELECT employee_id FROM project_assignments
     * }</pre>
     *
     * @param condition the subquery whose results will be subtracted from the main query. Must not be null.
     * @throws IllegalArgumentException if condition is null
     * @see Except
     * @see Union
     */
    public Minus(final SubQuery condition) {
        super(Operator.MINUS, condition);
    }
}