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
 * <p>Key characteristics of MINUS/EXCEPT:</p>
 * <ul>
 *   <li>Removes duplicate rows automatically (returns DISTINCT results)</li>
 *   <li>Both queries must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names don't need to match, but order matters</li>
 *   <li>NULL values are considered equal when comparing rows</li>
 * </ul>
 *
 * <p>Database support:</p>
 * <ul>
 *   <li>Oracle, DB2: Use MINUS keyword</li>
 *   <li>PostgreSQL, SQL Server, SQLite: Use EXCEPT keyword</li>
 *   <li>MySQL: Does not support MINUS/EXCEPT directly (use NOT IN or LEFT JOIN)</li>
 * </ul>
 *
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Finding records that exist in one table but not another</li>
 *   <li>Identifying missing data or gaps</li>
 *   <li>Data validation and comparison between datasets</li>
 *   <li>Finding customers without orders, products never sold, etc.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
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
 * <p>Relationship to other set operations:</p>
 * <ul>
 *   <li>MINUS and EXCEPT are functionally equivalent (MINUS in Oracle, EXCEPT in PostgreSQL/SQL Server)</li>
 *   <li>UNION combines rows from both queries, removing duplicates</li>
 *   <li>UNION ALL combines rows from both queries, keeping duplicates</li>
 *   <li>INTERSECT returns only rows that appear in both queries</li>
 *   <li>MINUS/EXCEPT returns rows from first query that don't appear in second query</li>
 * </ul>
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
     * The MINUS operation returns rows from the main query that are not present in this subquery,
     * performing a set difference operation (A - B). The result automatically excludes duplicates.
     *
     * <p>The MINUS operator performs a set difference operation, equivalent to A - B in set theory.
     * Only rows that appear in the first query but not in the second query will be returned.
     * Both queries must have compatible column structures (same number of columns with compatible types).
     * MINUS is functionally equivalent to EXCEPT and is primarily used in Oracle and DB2 databases.</p>
     *
     * <p>MINUS is useful for identifying gaps, finding missing data, or determining records
     * that exist in one dataset but not another. Common use cases include finding customers
     * without orders, products never sold, or employees not assigned to projects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find products that are in inventory but have never been sold
     * SubQuery soldProducts = new SubQuery("SELECT product_id FROM sales");
     * Minus unsoldProducts = new Minus(soldProducts);
     * // When used with a query selecting from inventory:
     * // SELECT product_id FROM inventory
     * // MINUS
     * // SELECT product_id FROM sales
     * // Returns product_id values in inventory but not in sales
     *
     * // Find active employees not assigned to any project
     * SubQuery assignedEmployees = new SubQuery("SELECT employee_id FROM project_assignments");
     * Minus unassigned = new Minus(assignedEmployees);
     * // SELECT employee_id FROM employees WHERE status = 'ACTIVE'
     * // MINUS
     * // SELECT employee_id FROM project_assignments
     * // Returns active employees with no project assignments
     *
     * // Find customers who have never placed an order
     * SubQuery customersWithOrders = new SubQuery("SELECT DISTINCT customer_id FROM orders");
     * Minus customersWithoutOrders = new Minus(customersWithOrders);
     * // SELECT customer_id FROM customers
     * // MINUS
     * // SELECT DISTINCT customer_id FROM orders
     * // Returns customers who have never ordered
     *
     * // Find skills not required for a position
     * SubQuery requiredSkills = new SubQuery("SELECT skill_id FROM position_requirements WHERE position_id = 5");
     * Minus optionalSkills = new Minus(requiredSkills);
     * // SELECT skill_id FROM all_skills
     * // MINUS
     * // SELECT skill_id FROM position_requirements WHERE position_id = 5
     * }</pre>
     *
     * @param condition the subquery to perform the MINUS operation with. Must not be null.
     *                  The subquery must have the same number of columns with compatible types as the main query.
     * @throws IllegalArgumentException if condition is null
     * @see Except
     * @see Union
     * @see UnionAll
     * @see Intersect
     */
    public Minus(final SubQuery condition) {
        super(Operator.MINUS, condition);
    }
}