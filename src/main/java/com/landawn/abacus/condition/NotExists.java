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
 * Represents the SQL NOT EXISTS operator for use with subqueries.
 * The NOT EXISTS operator returns true if the subquery returns no rows, false otherwise.
 *
 * <p>NOT EXISTS is useful for checking the absence of related records without
 * retrieving them. It can be more efficient than using NOT IN with large result sets
 * because it stops processing once it finds the first matching row.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find customers who have not placed any orders
 * SubQuery orderNotExists = new SubQuery(
 *     "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
 * );
 * NotExists noOrders = new NotExists(orderNotExists);
 *
 * // Find products that have not been reviewed
 * SubQuery reviewNotExists = new SubQuery(
 *     "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
 * );
 * NotExists noReviews = new NotExists(reviewNotExists);
 *
 * @see Exists
 * @see SubQuery
 */
public class NotExists extends Cell {

    // For Kryo
    NotExists() {
    }


    /**
     * Creates a new NOT EXISTS condition with the specified subquery.
     *
     * @param condition the subquery to check for non-existence of rows
     */
    public NotExists(final SubQuery condition) {
        super(Operator.NOT_EXISTS, condition);
    }
}