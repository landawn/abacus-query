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
 * of two queries, effectively finding the intersection of two query results.
 * 
 * <p>The INTERSECT operator removes duplicate rows from the final result set.
 * Both queries must have the same number of columns with compatible data types.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create a subquery for active customers
 * SubQuery activeCustomers = new SubQuery("SELECT customer_id FROM customers WHERE status = 'active'");
 * 
 * // Create another subquery for customers with recent orders
 * SubQuery recentOrders = new SubQuery("SELECT customer_id FROM orders WHERE order_date > '2023-01-01'");
 * 
 * // Find customers who are both active AND have recent orders
 * Intersect intersect = new Intersect(recentOrders);
 * // When combined with the first query, generates:
 * // SELECT customer_id FROM customers WHERE status = 'active'
 * // INTERSECT
 * // SELECT customer_id FROM orders WHERE order_date > '2023-01-01'
 * }</pre>
 * 
 * @see Union
 * @see Except
 * @see Minus
 */
public class Intersect extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Intersect() {
    }

    /**
     * Creates a new INTERSECT clause with the specified subquery.
     * The INTERSECT operation will return only rows that appear in both
     * the main query and this subquery.
     *
     * @param condition the subquery to intersect with. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * SubQuery premiumUsers = new SubQuery("SELECT user_id FROM users WHERE plan = 'premium'");
     * Intersect intersect = new Intersect(premiumUsers);
     * }</pre>
     */
    public Intersect(final SubQuery condition) {
        super(Operator.INTERSECT, condition);
    }
}