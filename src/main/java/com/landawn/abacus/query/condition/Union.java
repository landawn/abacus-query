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
 * Represents a UNION clause in SQL queries.
 * This class is used to combine the results of two or more SELECT statements,
 * automatically removing duplicate rows from the result set.
 *
 * <p>The UNION operator is a set operation that merges results from multiple queries into a single
 * result set. Unlike UNION ALL, it performs duplicate elimination, which can impact performance
 * but ensures that each row in the result set is unique.</p>
 *
 * <p>Key characteristics of UNION:
 * <ul>
 *   <li>Automatically removes duplicate rows from the combined result set</li>
 *   <li>All SELECT statements must have the same number of columns</li>
 *   <li>Corresponding columns must have compatible data types</li>
 *   <li>Column names from the first SELECT are used in the result</li>
 *   <li>Slower than UNION ALL due to duplicate elimination overhead</li>
 *   <li>Result order is not guaranteed unless ORDER BY is specified</li>
 * </ul>
 *
 * <p>When to use UNION vs UNION ALL:
 * <ul>
 *   <li>Use UNION when you need to eliminate duplicates from combined results</li>
 *   <li>Use UNION when merging data from overlapping sources</li>
 *   <li>Use UNION ALL for better performance when duplicates are acceptable or impossible</li>
 *   <li>Use UNION ALL when combining data from distinct, non-overlapping sources</li>
 * </ul>
 *
 * <p>Common use cases for UNION:
 * <ul>
 *   <li>Combining similar data from different tables (e.g., active and archived records)</li>
 *   <li>Merging results from different conditions that might overlap</li>
 *   <li>Creating unified views of partitioned data</li>
 *   <li>Consolidating data from multiple sources where uniqueness matters</li>
 * </ul>
 *
 * <p>Performance considerations:
 * <ul>
 *   <li>UNION performs an implicit DISTINCT operation, which requires sorting or hashing</li>
 *   <li>For large result sets, UNION can be significantly slower than UNION ALL</li>
 *   <li>If you know there are no duplicates, use UNION ALL for better performance</li>
 *   <li>Consider adding indexes on columns used in UNION queries</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Combine customers from different cities, removing duplicates
 * SubQuery nyCustomers = new SubQuery("SELECT id, name FROM customers WHERE city='NY'");
 * SubQuery laCustomers = new SubQuery("SELECT id, name FROM customers WHERE city='LA'");
 * Union union = new Union(laCustomers);
 * // Results in: SELECT id, name FROM customers WHERE city='NY'
 * //             UNION
 * //             SELECT id, name FROM customers WHERE city='LA'
 * // If a customer appears in both result sets, they will appear only once
 *
 * // Combine active and inactive users, ensuring no duplicates
 * SubQuery activeUsers = new SubQuery("SELECT user_id, email FROM active_users");
 * SubQuery inactiveUsers = new SubQuery("SELECT user_id, email FROM inactive_users");
 * Union allUsers = new Union(inactiveUsers);
 * // If a user appears in both tables, only one instance is returned
 *
 * // Merge current and historical data
 * SubQuery currentOrders = new SubQuery("SELECT order_id, customer_id FROM orders WHERE year = 2024");
 * SubQuery pastOrders = new SubQuery("SELECT order_id, customer_id FROM orders WHERE year = 2023");
 * Union allOrders = new Union(pastOrders);
 * // Duplicates are removed if an order appears in both years
 * }</pre>
 *
 * @see UnionAll
 * @see Intersect
 * @see Except
 * @see SubQuery
 * @see Clause
 */
public class Union extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Union instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Union() {
    }

    /**
     * Constructs a UNION clause with the specified subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery activeUsers = new SubQuery("SELECT user_id FROM active_users");
     * Union union = new Union(activeUsers);
     * }</pre>
     *
     * @param condition the subquery to be combined using UNION. Must not be null.
     * @throws IllegalArgumentException if condition is null
     */
    public Union(final SubQuery condition) {
        super(Operator.UNION, condition);
    }
}