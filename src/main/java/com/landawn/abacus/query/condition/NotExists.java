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

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Represents the SQL NOT EXISTS operator for use with subqueries.
 * The NOT EXISTS operator returns {@code true} if the subquery returns no rows, {@code false} otherwise.
 *
 * <p>NOT EXISTS is particularly useful for checking the absence of related records without
 * retrieving them. It can be more efficient than using NOT IN with large result sets
 * because it stops processing once it finds the first matching row, making it ideal for
 * existence checks.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns true when subquery returns zero rows</li>
 *   <li>Returns false when subquery returns one or more rows</li>
 *   <li>Often more efficient than NOT IN for large datasets</li>
 *   <li>Handles NULL values more predictably than NOT IN</li>
 *   <li>Short-circuits evaluation on first row found</li>
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
 * @see NotIn
 * @see Cell
 */
public class NotExists extends Cell {

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
     * // Find employees without any assigned projects
     * SubQuery projectCheck = Filters.subQuery(
     *     "SELECT 1 FROM project_assignments " +
     *     "WHERE project_assignments.employee_id = employees.id"
     * );
     * NotExists noProjects = new NotExists(projectCheck);
     * // Generates: NOT EXISTS (SELECT 1 FROM project_assignments WHERE project_assignments.employee_id = employees.id)
     *
     * // Find customers who have never placed an order
     * SubQuery orderCheck = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
     * NotExists noOrders = new NotExists(orderCheck);
     * // Generates: NOT EXISTS (SELECT 1 FROM orders WHERE orders.customer_id = customers.id)
     *
     * // Find products with no reviews
     * SubQuery reviewCheck = Filters.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
     * NotExists noReviews = new NotExists(reviewCheck);
     * // Generates: NOT EXISTS (SELECT 1 FROM reviews WHERE reviews.product_id = products.id)
     *
     * // Find departments without employees
     * SubQuery empCheck = Filters.subQuery("SELECT 1 FROM employees WHERE employees.dept_id = departments.id");
     * NotExists emptyDept = new NotExists(empCheck);
     * // Generates: NOT EXISTS (SELECT 1 FROM employees WHERE employees.dept_id = departments.id)
     * }</pre>
     *
     * @param subQuery the subquery to check for non-existence of rows (must not be null)
     * @throws NullPointerException if subQuery is null
     */
    public NotExists(final SubQuery subQuery) {
        super(Operator.NOT_EXISTS, subQuery);
    }

    /**
     * Returns this condition as SQL text using the specified naming policy.
     *
     * @param namingPolicy the naming policy for property/column names. If {@code null}, {@link NamingPolicy#NO_CHANGE} is applied
     * @return SQL in the form {@code NOT EXISTS (...)}
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Condition condition = getCondition();
        final String conditionString = condition == null ? "" : condition.toString(effectiveNamingPolicy);
        return operator().toString() + SK._SPACE + SK._PARENTHESIS_L + conditionString + SK._PARENTHESIS_R;
    }
}
