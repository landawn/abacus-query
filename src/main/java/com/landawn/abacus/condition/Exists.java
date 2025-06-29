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
 * Represents the SQL EXISTS operator for use with subqueries.
 * The EXISTS operator returns true if the subquery returns at least one row, false otherwise.
 * 
 * <p>EXISTS is particularly useful for checking the existence of related records without
 * actually retrieving them. It's often more efficient than using IN with large result sets
 * because it stops processing once it finds the first matching row.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find customers who have placed at least one order
 * SubQuery orderExists = new SubQuery(
 *     "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
 * );
 * Exists hasOrders = new Exists(orderExists);
 * 
 * // Find products that have been reviewed
 * SubQuery reviewExists = new SubQuery(
 *     "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
 * );
 * Exists hasReviews = new Exists(reviewExists);
 * 
 * // Can be negated: NOT EXISTS
 * Not noReviews = hasReviews.not();
 * }</pre>
 * 
 * <p>Performance tip: The SELECT clause in the EXISTS subquery doesn't matter
 * (SELECT 1, SELECT *, etc.) because EXISTS only checks for row existence.</p>
 * 
 * @see NotExists
 * @see SubQuery
 * @see Cell
 */
public class Exists extends Cell {

    // For Kryo
    Exists() {
    }

    /**
     * Creates a new EXISTS condition with the specified subquery.
     * 
     * @param condition the subquery to check for existence of rows
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if employee has any subordinates
     * SubQuery subordinatesQuery = new SubQuery(
     *     "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id"
     * );
     * Exists hasSubordinates = new Exists(subordinatesQuery);
     * 
     * // Use in a query to find all managers
     * query.where(hasSubordinates);
     * }</pre>
     */
    public Exists(final SubQuery condition) {
        super(Operator.EXISTS, condition);
    }
}