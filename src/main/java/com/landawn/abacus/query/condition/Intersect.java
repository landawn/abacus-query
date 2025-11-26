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
 * Represents an INTERSECT clause in SQL queries.
 * This class is used to create conditions that return only the rows that appear in both result sets
 * of two queries, effectively finding the intersection of two query results. The INTERSECT operator
 * is a set operation that combines results from multiple SELECT statements.
 * 
 * <p>Key characteristics of INTERSECT:
 * <ul>
 *   <li>Returns only rows that exist in both query results</li>
 *   <li>Automatically removes duplicate rows from the final result</li>
 *   <li>Both queries must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names from the first query are used in the result</li>
 * </ul>
 * 
 * <p>The INTERSECT operator is useful for:
 * <ul>
 *   <li>Finding common elements between two datasets</li>
 *   <li>Identifying records that meet multiple complex criteria</li>
 *   <li>Data validation and quality checks</li>
 *   <li>Set-based analysis and reporting</li>
 * </ul>
 *
 * <p>When to use INTERSECT vs other approaches:
 * <ul>
 *   <li>Use INTERSECT when you need rows that satisfy multiple independent conditions</li>
 *   <li>Use INTERSECT for readable, declarative set-based logic</li>
 *   <li>Consider INNER JOIN when you need columns from both tables</li>
 *   <li>Consider EXISTS when you only need to check presence without column matching</li>
 *   <li>Use IN clause for simple single-column membership tests</li>
 * </ul>
 *
 * <p>Performance considerations:
 * <ul>
 *   <li>INTERSECT requires duplicate elimination, similar to UNION</li>
 *   <li>Performance depends on result set size and database optimization</li>
 *   <li>May use sorting or hashing for duplicate removal</li>
 *   <li>Indexes on columns used in both queries can improve performance</li>
 *   <li>For large datasets, consider alternative approaches like INNER JOIN with EXISTS</li>
 * </ul>
 *
 * <p>Database support:
 * <ul>
 *   <li>Supported by: PostgreSQL, Oracle, SQL Server, SQLite, DB2</li>
 *   <li>NOT supported by MySQL (use IN with subquery or INNER JOIN instead)</li>
 *   <li>Part of SQL standard but not universally implemented</li>
 *   <li>Performance and optimization strategies vary by database</li>
 * </ul>
 *
 * <p>Relationship to other set operations:</p>
 * <ul>
 *   <li>INTERSECT returns only rows that appear in both queries</li>
 *   <li>UNION combines rows from both queries, removing duplicates</li>
 *   <li>UNION ALL combines rows from both queries, keeping duplicates</li>
 *   <li>EXCEPT/MINUS returns rows from first query that don't appear in second query</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find customers who are both active AND have recent orders
 * SubQuery activeCustomers = new SubQuery("SELECT customer_id FROM customers WHERE status = 'active'");
 * SubQuery recentOrders = new SubQuery("SELECT customer_id FROM orders WHERE order_date > '2023-01-01'");
 * Intersect intersect = new Intersect(recentOrders);
 * // When combined with the first query:
 * // SELECT customer_id FROM customers WHERE status = 'active'
 * // INTERSECT
 * // SELECT customer_id FROM orders WHERE order_date > '2023-01-01'
 * 
 * // Find products that are both in stock AND on sale
 * SubQuery inStock = new SubQuery("SELECT product_id FROM inventory WHERE quantity > 0");
 * SubQuery onSale = new SubQuery("SELECT product_id FROM promotions WHERE discount > 0");
 * Intersect commonProducts = new Intersect(onSale);
 * }</pre>
 * 
 * @see Union
 * @see UnionAll
 * @see Except
 * @see Minus
 * @see SubQuery
 * @see Clause
 */
public class Intersect extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Intersect instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Intersect() {
    }

    /**
     * Creates a new INTERSECT clause with the specified subquery.
     * The INTERSECT operation will return only rows that appear in both
     * the main query and this subquery, automatically removing duplicates.
     * This is the primary way to find common records between two result sets.
     *
     * <p>INTERSECT is useful for finding the intersection of two datasets,
     * such as customers who meet multiple criteria, products available in
     * multiple locations, or users with overlapping permissions. Both queries
     * must have the same number of columns with compatible data types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find users who are both premium AND active in the last 30 days
     * SubQuery activeUsers = new SubQuery("SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30");
     * Intersect premiumActive = new Intersect(activeUsers);
     * // When combined with premium users query:
     * // SELECT user_id FROM users WHERE plan = 'premium'
     * // INTERSECT
     * // SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30
     * // Returns only user_id values present in both result sets
     *
     * // Find employees who work in both projects
     * SubQuery projectB = new SubQuery("SELECT employee_id FROM assignments WHERE project = 'B'");
     * Intersect bothProjects = new Intersect(projectB);
     * // Use with project A query to find employees assigned to both projects
     *
     * // Find common skills between two job positions
     * SubQuery position2Skills = new SubQuery("SELECT skill_id FROM position_skills WHERE position_id = 2");
     * Intersect commonSkills = new Intersect(position2Skills);
     * // Identifies skills required by both positions
     *
     * // Find products in stock AND on promotion
     * SubQuery onPromotion = new SubQuery("SELECT product_id FROM promotions WHERE active = true");
     * Intersect availablePromotions = new Intersect(onPromotion);
     * // SELECT product_id FROM inventory WHERE quantity > 0
     * // INTERSECT
     * // SELECT product_id FROM promotions WHERE active = true
     * }</pre>
     *
     * @param condition the subquery to perform the INTERSECT operation with. Must not be null. The subquery should
     *                  return the same number of columns with compatible types as the main query.
     * @throws IllegalArgumentException if condition is null
     * @see Union
     * @see UnionAll
     * @see Except
     * @see Minus
     */
    public Intersect(final SubQuery condition) {
        super(Operator.INTERSECT, condition);
    }
}