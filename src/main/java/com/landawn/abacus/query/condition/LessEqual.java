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
 * Represents a less-than-or-equal-to (≤) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is less than
 * or equal to a specified value. The less-than-or-equal operator is fundamental for
 * implementing upper bounds, inclusive ranges, and various filtering scenarios.
 * 
 * <p>This condition is commonly used for:
 * <ul>
 *   <li>Setting upper bounds on numeric values (maximum limits)</li>
 *   <li>Date comparisons (on or before a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing inclusive range queries with GreaterEqual</li>
 *   <li>Price caps, age limits, and deadline checks</li>
 * </ul>
 * 
 * <p>The LessEqual operator works with various data types:
 * <ul>
 *   <li>Numbers: Natural numeric comparison</li>
 *   <li>Dates/Times: Chronological comparison</li>
 *   <li>Strings: Lexicographical (dictionary) order</li>
 *   <li>Any Comparable type supported by the database</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if age is 18 or younger
 * LessEqual ageLimit = new LessEqual("age", 18);
 * // SQL: age <= 18
 * 
 * // Check if price is at most $99.99
 * LessEqual priceLimit = new LessEqual("price", 99.99);
 * // SQL: price <= 99.99
 * 
 * // Check if date is on or before a deadline
 * LessEqual deadline = new LessEqual("submit_date", "2023-12-31");
 * // SQL: submit_date <= '2023-12-31'
 * 
 * // Combine with GreaterEqual for inclusive range
 * And priceRange = new And(
 *     new GreaterEqual("price", 10.00),
 *     new LessEqual("price", 100.00)
 * );
 * // SQL: (price >= 10.00) AND (price <= 100.00)
 * }</pre>
 * 
 * @see LessThan
 * @see GreaterEqual
 * @see Between
 * @see Binary
 */
public class LessEqual extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized LessEqual instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    LessEqual() {
    }

    /**
     * Creates a new less-than-or-equal-to condition with the specified property name and value.
     * This condition checks if the property value is less than or equal to the specified value,
     * providing an inclusive upper bound check.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Limit quantity to maximum stock
     * LessEqual stockLimit = new LessEqual("quantity", 100);
     * // SQL: quantity <= 100
     * 
     * // Find all orders on or before today
     * LessEqual todayOrEarlier = new LessEqual("order_date", LocalDate.now());
     * // Generates: order_date <= '2023-10-15' (assuming today's date)
     * 
     * // Set maximum allowed discount
     * LessEqual maxDiscount = new LessEqual("discount_percent", 50);
     * // SQL: discount_percent <= 50
     * 
     * // Check temperature threshold
     * LessEqual tempThreshold = new LessEqual("temperature", 25.5);
     * // SQL: temperature <= 25.5
     * 
     * // String comparison (alphabetical order)
     * LessEqual alphabetical = new LessEqual("last_name", "M");
     * // SQL: last_name <= 'M'
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public LessEqual(final String propName, final Object propValue) {
        super(propName, Operator.LESS_EQUAL, propValue);
    }
}