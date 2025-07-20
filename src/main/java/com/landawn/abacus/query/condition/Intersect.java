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
 * <p>Example usage:
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
     * the main query and this subquery. This is the primary way to find
     * common records between two result sets.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Find users who are both premium AND active in the last 30 days
     * SubQuery premiumUsers = new SubQuery("SELECT user_id FROM users WHERE plan = 'premium'");
     * SubQuery activeUsers = new SubQuery("SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30");
     * Intersect premiumActive = new Intersect(activeUsers);
     * 
     * // Find employees who work in both projects
     * SubQuery projectA = new SubQuery("SELECT employee_id FROM assignments WHERE project = 'A'");
     * SubQuery projectB = new SubQuery("SELECT employee_id FROM assignments WHERE project = 'B'");
     * Intersect bothProjects = new Intersect(projectB);
     * 
     * // Find common skills between two job positions
     * SubQuery position1Skills = new SubQuery("SELECT skill_id FROM position_skills WHERE position_id = 1");
     * SubQuery position2Skills = new SubQuery("SELECT skill_id FROM position_skills WHERE position_id = 2");
     * Intersect commonSkills = new Intersect(position2Skills);
     * }</pre>
     *
     * @param condition the subquery to intersect with. Must not be null. The subquery should
     *                  return the same number of columns with compatible types as the main query.
     * @throws IllegalArgumentException if condition is null
     */
    public Intersect(final SubQuery condition) {
        super(Operator.INTERSECT, condition);
    }
}