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
 * Represents a less-than (&lt;) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is less than 
 * a specified value. The less-than operator is fundamental for implementing upper bounds,
 * range queries, and various filtering scenarios where you need to exclude values at or
 * above a certain threshold.
 * 
 * <p>This condition is commonly used for:
 * <ul>
 *   <li>Setting exclusive upper bounds on numeric values</li>
 *   <li>Date comparisons (before a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing exclusive range queries with GreaterThan</li>
 *   <li>Age restrictions, expiration checks, and limit validations</li>
 * </ul>
 * 
 * <p>The LessThan operator works with various data types:
 * <ul>
 *   <li>Numbers: Natural numeric comparison</li>
 *   <li>Dates/Times: Chronological comparison</li>
 *   <li>Strings: Lexicographical (dictionary) order</li>
 *   <li>Any Comparable type supported by the database</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if age is less than 18
 * LessThan underAge = new LessThan("age", 18);
 * // SQL: age < 18
 * 
 * // Check if price is less than 99.99
 * LessThan priceLimit = new LessThan("price", 99.99);
 * // SQL: price < 99.99
 * 
 * // Check if date is before a deadline
 * LessThan beforeDeadline = new LessThan("submit_date", "2023-12-31");
 * // SQL: submit_date < '2023-12-31'
 * 
 * // Combine with GreaterThan for exclusive range
 * And priceRange = new And(
 *     new GreaterThan("price", 10.00),
 *     new LessThan("price", 100.00)
 * );
 * // SQL: (price > 10.00) AND (price < 100.00)
 * }</pre>
 * 
 * @see Binary
 * @see LessEqual
 * @see GreaterThan
 * @see Condition
 */
public class LessThan extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized LessThan instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    LessThan() {
    }

    /**
     * Creates a new less-than condition with the specified property name and value.
     * This condition checks if the property value is less than the specified value,
     * providing an exclusive upper bound check. The comparison excludes the boundary value itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for minors (under 18)
     * LessThan minorCheck = new LessThan("age", 18);
     * // SQL: age < 18
     *
     * // Ensure salary is below threshold
     * LessThan salaryLimit = new LessThan("salary", 50000);
     * // SQL: salary < 50000
     *
     * // Find items expiring before tomorrow
     * LessThan expiringItems = new LessThan("expiry_date", LocalDate.now().plusDays(1));
     * // Generates: expiry_date < '2023-10-16' (assuming tomorrow's date)
     *
     * // Temperature below freezing
     * LessThan freezing = new LessThan("temperature", 0);
     * // SQL: temperature < 0
     *
     * // Use with subquery - find products priced below average
     * SubQuery avgPrice = CF.subQuery("SELECT AVG(price) FROM products");
     * LessThan belowAverage = new LessThan("price", avgPrice);
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public LessThan(final String propName, final Object propValue) {
        super(propName, Operator.LESS_THAN, propValue);
    }
}