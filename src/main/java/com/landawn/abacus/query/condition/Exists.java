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
 * Represents the SQL EXISTS operator for use with subqueries.
 * The EXISTS operator returns {@code true} if the subquery returns at least one row, {@code false} otherwise.
 * 
 * <p>EXISTS is useful for checking the existence of related records without returning values from
 * the subquery. Optimizers may implement it as a semi-join or stop once existence is established;
 * relative performance versus IN depends on the database, data, indexes, and query plan.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns {@code true} if subquery returns any rows, {@code false} if no rows</li>
 *   <li>The selected values do not affect EXISTS semantics; only row existence matters</li>
 *   <li>Commonly used with correlated subqueries</li>
 *   <li>Can be negated with NOT EXISTS</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find customers who have placed at least one order
 * SubQuery orderExists = Filters.subQuery(
 *     "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
 * );
 * Exists hasOrders = new Exists(orderExists);
 *
 * // Find products that have been reviewed
 * SubQuery reviewExists = Filters.subQuery(
 *     "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
 * );
 * Exists hasReviews = new Exists(reviewExists);
 *
 * // Find departments with employees
 * SubQuery employeeExists = Filters.subQuery(
 *     "SELECT 1 FROM employees WHERE employees.dept_id = departments.id"
 * );
 * Exists hasEmployees = new Exists(employeeExists);
 * }</pre>
 * 
 * <p>The SELECT list does not affect EXISTS truth semantics, but it may still affect parsing,
 * permissions, or optimizer choices on a particular database.</p>
 * 
 * @see NotExists
 * @see SubQuery
 * @see ComposableCell
 * @see InSubQuery
 */
public class Exists extends ComposableCell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Exists instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Exists() {
    }

    /**
     * Creates a new EXISTS condition with the specified subquery.
     * The condition evaluates to true if the subquery returns at least one row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if employee has any subordinates (correlated subquery)
     * SubQuery subordinatesQuery = Filters.subQuery(
     *     "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id"
     * );
     * Exists hasSubordinates = new Exists(subordinatesQuery);
     * hasSubordinates.toString();
     * // returns "EXISTS (SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id)"
     *
     * // A null subquery is rejected
     * Exists bad = new Exists((SubQuery) null);
     * // throws IllegalArgumentException
     * }</pre>
     *
     * @param subQuery the subquery to check for existence of rows (must not be {@code null})
     * @throws IllegalArgumentException if {@code subQuery} is {@code null}
     */
    public Exists(final SubQuery subQuery) {
        super(Operator.EXISTS, subQuery);
    }

    /**
     * Returns the subquery used by this EXISTS condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retrieve the wrapped subquery
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
     * Exists exists = new Exists(subQuery);
     * SubQuery retrieved = exists.subQuery();
     * // returns the subquery passed to the constructor
     *
     * // The wrapped subquery is also what condition() returns
     * boolean sameAsCondition = exists.subQuery() == exists.condition();
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
