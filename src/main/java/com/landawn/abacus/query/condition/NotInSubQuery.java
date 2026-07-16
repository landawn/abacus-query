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
 * Represents a NOT IN subquery condition used in SQL WHERE clauses.
 * This condition checks if a property value (or multiple property values) is NOT contained
 * in the result set of a subquery.
 *
 * <p>The NOT IN subquery is particularly useful for excluding records based on
 * dynamic criteria from another query. It supports both single-column and multi-column
 * comparisons, making it suitable for simple exclusions as well as composite key checks.</p>
 *
 * <p><b>&#9888;&#65039;</b> Important considerations:</p>
 * <ul>
 *   <li>NULL handling: if the subquery returns a NULL, equality with a returned non-null value
 *       still makes {@code NOT IN} false, while an otherwise nonmatching value becomes unknown.
 *       Neither result matches a {@code WHERE} clause</li>
 *   <li>Performance: For large result sets, consider using NOT EXISTS instead</li>
 *   <li>Empty subquery results: If subquery returns no rows, all values pass the NOT IN check</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single property NOT IN subquery
 * SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
 * NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
 * // SQL: userId NOT IN (SELECT id FROM inactive_users)
 *
 * // Multiple properties NOT IN subquery
 * List<String> props = Arrays.asList("firstName", "lastName");
 * SubQuery subQuery2 = Filters.subQuery("SELECT fname, lname FROM blacklist");
 * NotInSubQuery condition2 = new NotInSubQuery(props, subQuery2);
 * // SQL: (firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)
 * }</pre>
 *
 * @see AbstractInSubQuery
 * @see InSubQuery
 * @see NotIn
 * @see NotExists
 * @see SubQuery
 */
public class NotInSubQuery extends AbstractInSubQuery {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotInSubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotInSubQuery() {
    }

    /**
     * Creates a NOT IN subquery condition for a single property.
     * This checks if the property value is not present in the subquery results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude deleted items
     * SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
     * NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);
     * // SQL: itemId NOT IN (SELECT id FROM deleted_items)
     *
     * // Exclude users from specific departments
     * SubQuery deptQuery = Filters.subQuery("SELECT user_id FROM dept_users WHERE dept = 'HR'");
     * NotInSubQuery notHR = new NotInSubQuery("id", deptQuery);
     * // SQL: id NOT IN (SELECT user_id FROM dept_users WHERE dept = 'HR')
     * }</pre>
     *
     * @param propName the property/column name to check against the subquery results (must not be {@code null}, empty, or blank)
     * @param subQuery the subquery that returns the values to check against (must not be {@code null});
     *            if it has an explicit structured projection without {@code *} or {@code qualifier.*}, it must select
     *            exactly one column; raw SQL and wildcard projections cannot be arity-checked here
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, if {@code subQuery} is {@code null},
     *             or if the subquery has a known, non-wildcard structured projection whose column count is not 1
     */
    public NotInSubQuery(final String propName, final SubQuery subQuery) {
        super(propName, Operator.NOT_IN, subQuery);
    }

    /**
     * Creates a NOT IN subquery condition for multiple properties.
     * Used for composite key comparisons where multiple columns need to be
     * checked against a subquery returning multiple columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude based on composite key
     * List<String> props = Arrays.asList("country", "city");
     * SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
     * NotInSubQuery condition = new NotInSubQuery(props, restricted);
     * // SQL: (country, city) NOT IN (SELECT country, city FROM restricted_locations)
     *
     * // Exclude duplicate entries
     * List<String> uniqueProps = Arrays.asList("firstName", "lastName", "email");
     * SubQuery existing = Filters.subQuery("SELECT fname, lname, email FROM existing_users");
     * NotInSubQuery noDupes = new NotInSubQuery(uniqueProps, existing);
     * // SQL: (firstName, lastName, email) NOT IN (SELECT fname, lname, email FROM existing_users)
     * }</pre>
     *
     * @param propNames collection of property names to check against the subquery results (must not be {@code null}
     *            or empty, and no element may be {@code null}, empty, or blank). Their order must match the column order in the subquery.
     * @param subQuery the subquery that returns the values to check against (must not be {@code null}).
     *            If it has an explicit structured projection without {@code *} or {@code qualifier.*}, it must select
     *            exactly {@code propNames.size()} columns. Raw SQL and wildcard projections cannot be arity-checked here.
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty, if any element is {@code null}, empty, or blank,
     *             if {@code subQuery} is {@code null}, or if the subquery has a known, non-wildcard structured projection
     *             whose number of selected columns does not match {@code propNames.size()}
     */
    public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(propNames, Operator.NOT_IN, subQuery);
    }
}
