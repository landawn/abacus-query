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
 * <p>When to use EXCEPT vs other approaches:
 * <ul>
 *   <li>Use EXCEPT for clean, declarative set difference operations</li>
 *   <li>Use EXCEPT when comparing entire row structures (multiple columns)</li>
 *   <li>Consider NOT EXISTS for better performance in some cases</li>
 *   <li>Consider LEFT JOIN with NULL check for single-table differences</li>
 *   <li>Use NOT IN for simple single-column exclusions (but beware of NULL values)</li>
 * </ul>
 *
 * <p>Performance considerations:
 * <ul>
 *   <li>EXCEPT requires duplicate elimination, which involves sorting or hashing</li>
 *   <li>Performance depends on result set sizes and database optimization</li>
 *   <li>Indexes on columns used in both queries can significantly improve performance</li>
 *   <li>For large datasets, NOT EXISTS may outperform EXCEPT in some databases</li>
 *   <li>Consider the cardinality of both result sets when choosing an approach</li>
 * </ul>
 *
 * <p>Database support:
 * <ul>
 *   <li>Supported by: PostgreSQL, SQL Server, SQLite, DB2</li>
 *   <li>NOT supported by MySQL or Oracle (Oracle uses MINUS instead)</li>
 *   <li>Part of SQL standard but not universally implemented</li>
 *   <li>Functionally equivalent to Oracle's MINUS operator</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find all customers except those who have placed orders
 * SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
 * Except customersWithoutOrders = new Except(customersWithOrders);
 * // When combined with all customers query:
 * // SELECT customer_id FROM customers WHERE status = 'active'
 * // EXCEPT
 * // SELECT DISTINCT customer_id FROM orders
 * // Results: active customers who have no orders
 *
 * // Find products not sold in the last month
 * SubQuery recentlySold = Filters.subQuery(
 *     "SELECT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
 * );
 * Except unsoldProducts = new Except(recentlySold);
 * // SELECT product_id FROM products
 * // EXCEPT
 * // SELECT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)
 * // Results: all products except those sold recently
 * }</pre>
 *
 * <p>Relationship to other set operations:</p>
 * <ul>
 *   <li>EXCEPT and MINUS are functionally equivalent (EXCEPT in PostgreSQL/SQL Server, MINUS in Oracle)</li>
 *   <li>UNION combines rows from both queries, removing duplicates</li>
 *   <li>UNION ALL combines rows from both queries, keeping duplicates</li>
 *   <li>INTERSECT returns only rows that appear in both queries</li>
 *   <li>EXCEPT returns rows from first query that don't appear in second query</li>
 * </ul>
 *
 * @see Minus
 * @see Union
 * @see UnionAll
 * @see Intersect
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
     * The EXCEPT operation returns all distinct rows from the main query that are not
     * present in the subquery results, performing a set difference operation (A - B).
     * Duplicates are automatically removed from the final result.
     *
     * <p>EXCEPT is useful for finding records that exist in one dataset but not in another,
     * such as customers without orders, products never sold, or employees not assigned to projects.
     * Both queries must have the same number of columns with compatible data types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find employees who are not managers
     * SubQuery managers = Filters.subQuery("SELECT employee_id FROM employees WHERE is_manager = true");
     * Except notManagers = new Except(managers);
     * // When combined with all employees query:
     * // SELECT employee_id FROM employees WHERE department = 'Sales'
     * // EXCEPT
     * // SELECT employee_id FROM employees WHERE is_manager = true
     * // Returns Sales employees who are not managers
     *
     * // Find customers who haven't placed orders
     * SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
     * Except customersWithoutOrders = new Except(customersWithOrders);
     * // SELECT customer_id FROM customers
     * // EXCEPT
     * // SELECT DISTINCT customer_id FROM orders
     * // Returns customers with no orders
     *
     * // Find skills not required for a specific job
     * SubQuery requiredSkills = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123");
     * Except otherSkills = new Except(requiredSkills);
     * // SELECT skill_id FROM skills
     * // EXCEPT
     * // SELECT skill_id FROM job_requirements WHERE job_id = 123
     * // Returns all skills except those required for job 123
     *
     * // Find products in inventory but never sold
     * SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales");
     * Except unsoldProducts = new Except(soldProducts);
     * // SELECT product_id FROM inventory
     * // EXCEPT
     * // SELECT product_id FROM sales
     * }</pre>
     *
     * @param condition the subquery to perform the EXCEPT operation with. Must not be null.
     *                  The subquery must have the same number of columns with compatible types as the main query.
     * @throws NullPointerException if condition is null
     * @see Minus
     * @see Union
     * @see UnionAll
     * @see Intersect
     */
    public Except(final SubQuery condition) {
        super(Operator.EXCEPT, condition);
    }
}