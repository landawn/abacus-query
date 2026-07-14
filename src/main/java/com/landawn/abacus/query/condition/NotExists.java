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
 * Represents the SQL NOT EXISTS operator for use with subqueries.
 * The NOT EXISTS operator returns {@code true} if the subquery returns no rows, {@code false} otherwise.
 *
 * <p>NOT EXISTS is particularly useful for checking the absence of related records without
 * retrieving them. Optimizers may stop once a matching row is established. Relative performance
 * versus NOT IN is plan-dependent, while NOT EXISTS avoids NOT IN's NULL-sensitive membership semantics.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns {@code true} when subquery returns zero rows</li>
 *   <li>Returns {@code false} when subquery returns one or more rows</li>
 *   <li>Avoids the NULL-sensitive membership semantics of NOT IN</li>
 *   <li>Relative performance versus NOT IN depends on the database and execution plan</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find customers who have not placed any orders
 * SubQuery orderNotExists = Filters.subQuery(
 *     "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
 * );
 * NotExists noOrders = new NotExists(orderNotExists);
 * 
 * // Find products that have not been reviewed
 * SubQuery reviewNotExists = Filters.subQuery(
 *     "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
 * );
 * NotExists noReviews = new NotExists(reviewNotExists);
 * }</pre>
 *
 * @see Exists
 * @see SubQuery
 * @see NotInSubQuery
 * @see ComposableCell
 */
public class NotExists extends ComposableCell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotExists instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotExists() {
    }

    /**
     * Creates a new NOT EXISTS condition with the specified subquery.
     * The condition evaluates to true when the subquery returns no rows.
     *
     * <p>The subquery typically contains a correlated reference to the outer query,
     * allowing it to check for the absence of related records. Common patterns include
     * checking for missing relationships, finding orphaned records, or identifying
     * entities without certain attributes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find employees without any assigned projects (correlated subquery)
     * SubQuery projectCheck = Filters.subQuery(
     *     "SELECT 1 FROM project_assignments " +
     *     "WHERE project_assignments.employee_id = employees.id"
     * );
     * NotExists noProjects = new NotExists(projectCheck);
     * noProjects.toString();
     * // returns "NOT EXISTS (SELECT 1 FROM project_assignments WHERE project_assignments.employee_id = employees.id)"
     *
     * // A null subquery is rejected
     * NotExists bad = new NotExists((SubQuery) null);
     * // throws IllegalArgumentException
     * }</pre>
     *
     * @param subQuery the subquery to check for non-existence of rows (must not be {@code null})
     * @throws IllegalArgumentException if {@code subQuery} is {@code null}
     */
    public NotExists(final SubQuery subQuery) {
        super(Operator.NOT_EXISTS, subQuery);
    }

    /**
     * Returns the subquery used by this NOT EXISTS condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retrieve the wrapped subquery
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
     * NotExists notExists = new NotExists(subQuery);
     * SubQuery retrieved = notExists.subQuery();
     * // returns the subquery passed to the constructor
     *
     * // The wrapped subquery is also what condition() returns
     * boolean sameAsCondition = notExists.subQuery() == notExists.condition();
     * // returns true
     * }</pre>
     *
     * @return the {@link SubQuery} supplied at construction time, or {@code null} for an uninitialized
     *         serialization-framework instance
     */
    public SubQuery subQuery() {
        return (SubQuery) condition;
    }
}
