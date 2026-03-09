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
 * Represents a NOT IN condition in SQL queries.
 * This condition checks if a property value is NOT contained in a specified collection of values.
 * It's the logical opposite of the IN operator and is useful for exclusion-based filtering.
 *
 * <p>The NOT IN operator is particularly useful for:
 * <ul>
 *   <li>Excluding records with specific status values</li>
 *   <li>Filtering out test or system data</li>
 *   <li>Implementing blacklist-based filtering</li>
 *   <li>Finding records that don't match any value in a list</li>
 * </ul>
 *
 * <p>Important considerations:
 * <ul>
 *   <li>NULL handling: If the list contains NULL or the column has NULL values,
 *       NOT IN may return unexpected results. In SQL, comparing any value with NULL using NOT IN
 *       evaluates to UNKNOWN, which behaves as false in WHERE clauses</li>
 *   <li>Performance: For large lists, consider using NOT EXISTS or LEFT JOIN instead</li>
 *   <li>The values list is copied during construction to ensure immutability</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude inactive statuses
 * List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
 * NotIn condition = new NotIn("status", inactiveStatuses);
 * // Results in: status NOT IN ('deleted', 'archived', 'suspended')
 *
 * // Exclude specific department IDs
 * Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
 * NotIn deptCondition = new NotIn("department_id", excludedDepts);
 * // Results in: department_id NOT IN (10, 20, 30)
 *
 * // Exclude test users
 * List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
 * NotIn emailCondition = new NotIn("email", testEmails);
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
     * // Generates: category NOT IN ('discontinued', 'internal', 'test')
     *
     * // Exclude test users by ID
     * Set<Integer> testUserIds = new HashSet<>(Arrays.asList(1, 2, 999));
     * NotIn excludeUsers = new NotIn("user_id", testUserIds);
     * // Generates: user_id NOT IN (1, 2, 999)
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param values the collection of values that the property should NOT match.
     *               The collection is copied internally to ensure immutability.
     * @throws IllegalArgumentException if propName is null or values is null/empty
     */
    public NotIn(final String propName, final Collection<?> values) {
        super(propName, Operator.NOT_IN, values);
    }
}
