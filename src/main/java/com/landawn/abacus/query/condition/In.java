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

import java.util.Collection;
import java.util.Map;

/**
 * Represents an IN condition in SQL-like queries.
 * This class is used to check if a property value matches any value in a specified collection.
 * It's equivalent to multiple OR conditions but more concise and often more efficient.
 * The IN operator is one of the most commonly used SQL operators for filtering data.
 *
 * <p>The IN condition is particularly useful for:</p>
 * <ul>
 *   <li>Checking membership in a list of values</li>
 *   <li>Filtering by multiple possible values efficiently</li>
 *   <li>Replacing multiple OR conditions with cleaner syntax</li>
 *   <li>Working with results from subqueries (see {@link InSubQuery})</li>
 *   <li>Implementing dynamic filters based on user selections</li>
 * </ul>
 *
 * <p><b>&#9888;&#65039;</b> SQL {@code NULL} follows three-valued logic. A {@code NULL} column value does
 * not match, and a nonmatching value compared with a list that contains {@code NULL} evaluates
 * to unknown rather than false.</p>
 *
 * <p>Performance considerations:</p>
 * <ul>
 *   <li>Most databases optimize IN conditions well for small to medium lists</li>
 *   <li>For very large lists, consider using a temporary table and JOIN</li>
 *   <li>Some databases have limits on the number of values in an IN clause</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if status is one of several values
 * In statusCheck = new In("status", Arrays.asList("active", "pending", "approved"));
 * // SQL: status IN ('active', 'pending', 'approved')
 *
 * // Check if user_id is in a list
 * In userFilter = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
 * // SQL: user_id IN (1, 2, 3, 5, 8)
 *
 * // Filter by categories
 * Set<String> categories = new LinkedHashSet<>(Arrays.asList("electronics", "computers"));
 * In categoryFilter = new In("category", categories);
 * // SQL: category IN ('electronics', 'computers')
 * }</pre>
 *
 * @see AbstractIn
 * @see NotIn
 * @see InSubQuery
 */
public class In extends AbstractIn {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized In instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    In() {
    }

    /**
     * Creates a new IN condition with the specified property name and collection of values.
     * The condition checks if the property value matches any value in the collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter by multiple categories
     * Set<String> categories = new LinkedHashSet<>(Arrays.asList("electronics", "computers", "phones"));
     * In categoryFilter = new In("category", categories);
     * // SQL: category IN ('electronics', 'computers', 'phones')
     *
     * // Filter by specific IDs
     * List<Long> ids = Arrays.asList(101L, 102L, 103L);
     * In idFilter = new In("product_id", ids);
     * // SQL: product_id IN (101, 102, 103)
     *
     * // Filter by enum values
     * List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
     * In priorityFilter = new In("priority", priorities);
     * // SQL: priority IN ('HIGH', 'CRITICAL')
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param values the collection of values to check against (must not be {@code null} or empty);
     *               the collection is copied internally to prevent external modifications. A
     *               condition-valued element must not be query-structural or quantified
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty/blank, if {@code values}
     *                                  is {@code null}/empty, or if a condition-valued element is or contains
     *                                  a {@link Criteria}, SQL clause, JOIN, or {@code ON}/{@code USING} connector,
     *                                  or is/contains an {@link All}, {@link Any}, or {@link Some} operand
     */
    public In(final String propName, final Collection<?> values) {
        super(propName, Operator.IN, values);
    }

    /**
     * Creates a new row value constructor IN condition. The condition checks whether the
     * tuple of property values matches any of the supplied value rows. Each element of {@code valueRows}
     * must resolve to exactly {@code propNames.size()} values. A row may be supplied as a {@link Collection}
     * or other {@link Iterable}, an object array, a {@link Map} (looked up by property name, with a
     * missing key represented as {@code null}) or a bean
     * (read by property name).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Match (first_name, last_name) pairs supplied as lists
     * In nameFilter = new In(Arrays.asList("first_name", "last_name"),
     *         Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * // SQL: (first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))
     *
     * // Explicit one-column row value form
     * In idFilter = new In(Arrays.asList("id"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
     * // SQL: (id) IN ((1), (2))
     *
     * // The same rows supplied as maps keyed by property name
     * In fromMaps = new In(Arrays.asList("first_name", "last_name"),
     *         Arrays.asList(N.asMap("first_name", "John", "last_name", "Doe")));
     * }</pre>
     *
     * <p><b>&#9888;&#65039;</b> The row value-list form is supported by MySQL, PostgreSQL,
     * Oracle and DB2, but <i>not</i> by SQL Server (rewrite the composite comparison with
     * {@code EXISTS} or a join there).</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain {@code null}, empty, or blank names)
     * @param valueRows the collection of value rows (must not be {@code null} or empty); each row must be
     *               non-{@code null} and resolve to exactly {@code propNames.size()} values. A row may be a
     *               {@link Collection}, {@link Iterable}, object array, {@link Map} or bean. A missing
     *               property-name key in a map contributes {@code null}; condition-valued elements
     *               must not be query-structural or quantified
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty or contains any {@code null}, empty, or blank name,
     *                                  if {@code valueRows} is {@code null}/empty, if any row is {@code null} or of an
     *                                  unsupported type, if a positional row's width does not match {@code propNames.size()},
     *                                  or if a bean row does not expose a requested property, or if a condition-valued
     *                                  row element is or contains a {@link Criteria}, SQL clause, JOIN, or
     *                                  {@code ON}/{@code USING} connector, or is/contains an {@link All},
     *                                  {@link Any}, or {@link Some} quantified operand
     */
    public In(final Collection<String> propNames, final Collection<?> valueRows) {
        super(propNames, Operator.IN, valueRows);
    }
}
