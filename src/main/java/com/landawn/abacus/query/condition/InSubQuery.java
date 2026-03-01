/*
 * Copyright (C) 2020 HaiYang Li
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

import java.util.Collection;

/**
 * Represents an IN condition with a subquery in SQL-like queries.
 * This class is used to check if a property value (or multiple property values) exists
 * in the result set of a subquery. It's commonly used for filtering records based on
 * values from another table or complex query result.
 *
 * <p>The IN subquery condition is particularly useful when:
 * <ul>
 *   <li>Filtering based on dynamic result sets from other tables</li>
 *   <li>Avoiding joins when only checking existence</li>
 *   <li>Working with correlated or uncorrelated subqueries</li>
 *   <li>Checking multiple column combinations against subquery results</li>
 * </ul>
 *
 * <p>This condition supports two forms:
 * <ul>
 *   <li>Single column: {@code column IN (SELECT ... FROM ...)}</li>
 *   <li>Multiple columns: {@code (column1, column2) IN (SELECT col1, col2 FROM ...)}</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single column IN subquery - find orders from premium customers
 * SubQuery premiumCustomers = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
 * InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);
 * // Generates: customer_id IN (SELECT customer_id FROM customers WHERE status = 'premium')
 *
 * // Multiple columns IN subquery - find employees in specific department/location combinations
 * SubQuery validAssignments = Filters.subQuery("SELECT dept_id, location_id FROM allowed_assignments");
 * InSubQuery multiColumn = new InSubQuery(Arrays.asList("department_id", "location_id"), validAssignments);
 * // Generates: (department_id, location_id) IN (SELECT dept_id, location_id FROM allowed_assignments)
 * }</pre>
 *
 * @see AbstractInSubQuery
 * @see NotInSubQuery
 * @see In
 * @see SubQuery
 */
public class InSubQuery extends AbstractInSubQuery {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized InSubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    InSubQuery() {
    }

    /**
     * Creates an IN subquery condition for a single property.
     * Use this constructor when checking if a single column value exists in the subquery result.
     * The subquery should return a single column of compatible type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all products in active categories
     * SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
     * InSubQuery condition = new InSubQuery("category_id", activeCategories);
     * // Generates: category_id IN (SELECT category_id FROM categories WHERE active = true)
     *
     * // Find employees in departments with high budgets
     * SubQuery richDepts = Filters.subQuery("SELECT dept_id FROM departments WHERE budget > 1000000");
     * InSubQuery condition2 = new InSubQuery("department_id", richDepts);
     * // Generates: department_id IN (SELECT dept_id FROM departments WHERE budget > 1000000)
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param subQuery the subquery that returns the values to check against. Must not be null.
     * @throws IllegalArgumentException if propName is null or empty, or if subQuery is null
     */
    public InSubQuery(final String propName, final SubQuery subQuery) {
        super(propName, Operator.IN, subQuery);
    }

    /**
     * Creates an IN subquery condition for multiple properties.
     * Use this constructor for composite key checks or when multiple columns need to match
     * the subquery results. The subquery must return the same number of columns in the same order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find employees assigned to valid department/location combinations
     * List<String> columns = Arrays.asList("dept_id", "loc_id");
     * SubQuery validCombinations = Filters.subQuery(
     *     "SELECT department_id, location_id FROM dept_locations WHERE active = 'Y'"
     * );
     * InSubQuery condition = new InSubQuery(columns, validCombinations);
     * // Generates: (dept_id, loc_id) IN (SELECT department_id, location_id FROM dept_locations WHERE active = 'Y')
     * }</pre>
     *
     * @param propNames the names of the properties to check. Must not be null or empty.
     *                  The order must match the column order in the subquery.
     * @param subQuery the subquery that returns the value combinations to check against. Must not be null.
     *                 Must return the same number of columns as propNames.size().
     * @throws IllegalArgumentException if propNames is null/empty, if any element is null/empty, or if subQuery is null
     */
    public InSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(propNames, Operator.IN, subQuery);
    }
}
