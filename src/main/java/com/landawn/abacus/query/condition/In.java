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

/**
 * Represents an IN condition in SQL-like queries.
 * This class is used to check if a property value matches any value in a specified collection.
 * It's equivalent to multiple OR conditions but more concise and often more efficient.
 * The IN operator is one of the most commonly used SQL operators for filtering data.
 *
 * <p>The IN condition is particularly useful for:
 * <ul>
 *   <li>Checking membership in a list of values</li>
 *   <li>Filtering by multiple possible values efficiently</li>
 *   <li>Replacing multiple OR conditions with cleaner syntax</li>
 *   <li>Working with results from subqueries (see {@link InSubQuery})</li>
 *   <li>Implementing dynamic filters based on user selections</li>
 * </ul>
 *
 * <p>Performance considerations:
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
 * // Generates: status IN ('active', 'pending', 'approved')
 *
 * // Check if user_id is in a list
 * In userFilter = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
 * // Generates: user_id IN (1, 2, 3, 5, 8)
 *
 * // Filter by categories
 * Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
 * In categoryFilter = new In("category", categories);
 * // Generates: category IN ('electronics', 'computers')
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
     * Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers", "phones"));
     * In categoryFilter = new In("category", categories);
     * // Generates: category IN ('electronics', 'computers', 'phones')
     *
     * // Filter by specific IDs
     * List<Long> ids = Arrays.asList(101L, 102L, 103L);
     * In idFilter = new In("product_id", ids);
     * // Generates: product_id IN (101, 102, 103)
     *
     * // Filter by enum values
     * List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
     * In priorityFilter = new In("priority", priorities);
     * // Generates: priority IN ('HIGH', 'CRITICAL')
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param values the collection of values to check against. Must not be null or empty.
     *               The collection is copied internally to prevent external modifications.
     * @throws IllegalArgumentException if propName is null or values is null/empty
     */
    public In(final String propName, final Collection<?> values) {
        super(propName, Operator.IN, values);
    }
}
