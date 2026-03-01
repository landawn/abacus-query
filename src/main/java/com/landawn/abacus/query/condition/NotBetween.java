/*
 * Copyright (C) 2021 HaiYang Li
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
 * Represents a NOT BETWEEN condition in SQL queries.
 * This condition checks if a value is NOT within a specified range.
 * It's the logical opposite of the BETWEEN operator and is useful for excluding ranges of values.
 *
 * <p>The NOT BETWEEN condition is equivalent to: property &lt; minValue OR property &gt; maxValue.
 * The condition evaluates to true if the property value falls outside the specified range,
 * excluding the boundaries themselves.</p>
 *
 * <p>Common use cases include:
 * <ul>
 *   <li>Excluding values within a specific range</li>
 *   <li>Finding outliers or extreme values</li>
 *   <li>Filtering out normal operating ranges to find anomalies</li>
 *   <li>Implementing "outside business hours" logic</li>
 * </ul>
 *
 * <p>Important notes:</p>
 * <ul>
 *   <li>The range is inclusive in BETWEEN, so NOT BETWEEN excludes both boundaries</li>
 *   <li>Works with numbers, strings, dates, and other comparable types</li>
 *   <li>Can use expressions or subqueries as range boundaries</li>
 *   <li>NULL values: if the column value or either boundary is NULL, the result is NULL (not true)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude normal temperature range
 * NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
 * // Renders as: temperature NOT BETWEEN 36.0 AND 37.5
 *
 * // Find orders outside business hours (before 9 AM or after 5 PM)
 * NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
 * // Renders as: order_hour NOT BETWEEN 9 AND 17
 *
 * // Exclude mid-range salaries
 * NotBetween salaryRange = new NotBetween("salary", 50000, 100000);
 * // Renders as: salary NOT BETWEEN 50000 AND 100000
 *
 * // Using with date strings
 * NotBetween dateRange = new NotBetween("order_date", "2024-01-01", "2024-12-31");
 * // Renders as: order_date NOT BETWEEN '2024-01-01' AND '2024-12-31'
 * }</pre>
 *
 * @see AbstractBetween
 * @see Between
 * @see Condition
 */
public class NotBetween extends AbstractBetween {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotBetween instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotBetween() {
    }

    /**
     * Creates a NOT BETWEEN condition for the specified property and range.
     * The condition will match values that are less than minValue OR greater than maxValue.
     * Both boundaries are excluded from the match (opposite of BETWEEN's inclusive behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find products with extreme prices (very cheap or very expensive)
     * NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
     * // Renders as: price NOT BETWEEN 10.0 AND 1000.0
     * // Matches: price < 10.0 OR price > 1000.0
     *
     * // Find events outside regular working days
     * NotBetween workdays = new NotBetween("day_of_week", 2, 6);   // Monday = 2, Friday = 6
     * // Renders as: day_of_week NOT BETWEEN 2 AND 6
     * // Matches: Sunday (1) and Saturday (7)
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param minValue the minimum value of the range to exclude (can be null, literal value, or subquery)
     * @param maxValue the maximum value of the range to exclude (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public NotBetween(final String propName, final Object minValue, final Object maxValue) {
        super(propName, Operator.NOT_BETWEEN, minValue, maxValue);
    }
}
