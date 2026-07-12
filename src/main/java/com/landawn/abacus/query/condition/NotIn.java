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
import java.util.Map;

/**
 * Represents a NOT IN condition in SQL queries.
 * This condition checks if a property value is NOT contained in a specified collection of values.
 * It's the logical opposite of the IN operator and is useful for exclusion-based filtering.
 *
 * <p>The NOT IN operator is particularly useful for:</p>
 * <ul>
 *   <li>Excluding records with specific status values</li>
 *   <li>Filtering out test or system data</li>
 *   <li>Implementing blacklist-based filtering</li>
 *   <li>Finding records that don't match any value in a list</li>
 * </ul>
 *
 * <p><b>&#9888;&#65039;</b> Important considerations:</p>
 * <ul>
 *   <li>NULL handling: if the list contains a NULL value, a value equal to a non-null list member
 *       still evaluates to false, while any otherwise nonmatching value evaluates to unknown.
 *       Neither result matches a {@code WHERE} clause. If the column itself is NULL, the comparison
 *       is also unknown and that row is excluded</li>
 *   <li>Performance: for large lists, consider using NOT EXISTS or a LEFT JOIN / IS NULL pattern</li>
 *   <li>The values collection is copied during construction to ensure immutability</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude inactive statuses
 * List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
 * NotIn condition = new NotIn("status", inactiveStatuses);
 * // SQL: status NOT IN ('deleted', 'archived', 'suspended')
 *
 * // Exclude specific department IDs
 * Set<Integer> excludedDepts = new LinkedHashSet<>(Arrays.asList(10, 20, 30));
 * NotIn deptCondition = new NotIn("department_id", excludedDepts);
 * // SQL: department_id NOT IN (10, 20, 30)
 *
 * // Exclude test users
 * List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
 * NotIn emailCondition = new NotIn("email", testEmails);
 * // SQL: email NOT IN ('test@example.com', 'demo@example.com')
 * }</pre>
 *
 * @see AbstractIn
 * @see In
 * @see NotInSubQuery
 */
public class NotIn extends AbstractIn {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotIn instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotIn() {
    }

    /**
     * Creates a NOT IN condition for the specified property and collection of values.
     * The condition will match records where the property value is not equal to any of the
     * provided values. A defensive copy of the values collection is made to ensure immutability.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude specific product categories
     * List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
     * NotIn notIn = new NotIn("category", excludedCategories);
     * // SQL: category NOT IN ('discontinued', 'internal', 'test')
     *
     * // Exclude test users by ID
     * Set<Integer> testUserIds = new LinkedHashSet<>(Arrays.asList(1, 2, 999));
     * NotIn excludeUsers = new NotIn("user_id", testUserIds);
     * // SQL: user_id NOT IN (1, 2, 999)
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param values the collection of values that the property should NOT match
     *               (must not be {@code null} or empty); the collection is copied internally to ensure immutability
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty/blank, or if {@code values} is {@code null}/empty
     */
    public NotIn(final String propName, final Collection<?> values) {
        super(propName, Operator.NOT_IN, values);
    }

    /**
     * Creates a new row value constructor NOT IN condition. The condition matches records
     * whose tuple of property values does not match any of the supplied value rows. Each element of
     * {@code valueRows} must resolve to exactly {@code propNames.size()} values. A row may be supplied as a
     * {@link Collection} or other {@link Iterable}, an object array, a {@link Map} (looked up by property
     * name) or a bean (read by property name).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude specific (first_name, last_name) pairs supplied as lists
     * NotIn nameFilter = new NotIn(Arrays.asList("first_name", "last_name"),
     *         Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * // SQL: (first_name, last_name) NOT IN (('John', 'Doe'), ('Jane', 'Roe'))
     *
     * // Explicit one-column row value form
     * NotIn idFilter = new NotIn(Arrays.asList("id"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
     * // SQL: (id) NOT IN ((1), (2))
     * }</pre>
     *
     * <p><b>&#9888;&#65039;</b> The row value-list form is supported by MySQL, PostgreSQL,
     * Oracle and DB2, but <i>not</i> by SQL Server (use {@link NotInSubQuery} there).</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain {@code null}, empty, or blank names)
     * @param valueRows the collection of value rows (must not be {@code null} or empty); each row must be
     *               non-{@code null} and resolve to exactly {@code propNames.size()} values. A row may be a
     *               {@link Collection}, {@link Iterable}, object array, {@link Map} or bean
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty or contains any {@code null}, empty, or blank name,
     *                                  if {@code valueRows} is {@code null}/empty, if any row is {@code null} or of an
     *                                  unsupported type, if a positional row's width does not match {@code propNames.size()},
     *                                  or if a bean row does not expose a requested property
     */
    public NotIn(final Collection<String> propNames, final Collection<?> valueRows) {
        super(propNames, Operator.NOT_IN, valueRows);
    }
}
