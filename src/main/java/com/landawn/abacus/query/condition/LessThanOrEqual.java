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
 * Represents a less-than-or-equal-to (&lt;=) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is less than
 * or equal to a specified value. The less-than-or-equal operator is fundamental for
 * implementing inclusive upper bounds, inclusive ranges, and various filtering scenarios.
 *
 * <p>Common use cases include:
 * <ul>
 *   <li>Setting inclusive upper bounds on numeric values (maximum limits)</li>
 *   <li>Date comparisons (on or before a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing inclusive range queries when combined with GreaterThanOrEqual</li>
 *   <li>Price caps, age limits, and deadline checks</li>
 * </ul>
 *
 * <p>The operator works with various data types:
 * <ul>
 *   <li>Numbers: Natural numeric comparison</li>
 *   <li>Dates/Times: Chronological comparison</li>
 *   <li>Strings: Lexicographical (dictionary) order</li>
 *   <li>Any Comparable type supported by the database</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if age is 18 or younger
 * LessThanOrEqual ageLimit = new LessThanOrEqual("age", 18);
 * // SQL: age <= 18
 * 
 * // Check if price is at most $99.99
 * LessThanOrEqual priceLimit = new LessThanOrEqual("price", 99.99);
 * // SQL: price <= 99.99
 * 
 * // Check if date is on or before a deadline
 * LessThanOrEqual deadline = new LessThanOrEqual("submit_date", "2023-12-31");
 * // SQL: submit_date <= '2023-12-31'
 * 
 * // Combine with GreaterThanOrEqual for inclusive range
 * And priceRange = new And(
 *     new GreaterThanOrEqual("price", 10.00),
 *     new LessThanOrEqual("price", 100.00)
 * );
 * // SQL: (price >= 10.00) AND (price <= 100.00)
 * }</pre>
 * 
 * @see Binary
 * @see LessThan
 * @see GreaterThanOrEqual
 * @see Condition
 */
public class LessThanOrEqual extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized LessThanOrEqual instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    LessThanOrEqual() {
    }

    /**
     * Creates a new LessThanOrEqual condition.
     * This condition checks if the property value is less than or equal to the specified value,
     * providing an inclusive upper bound check.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit quantity to maximum stock
     * LessThanOrEqual stockLimit = new LessThanOrEqual("quantity", 100);
     * // SQL: quantity <= 100
     *
     * // Find all orders on or before today
     * LessThanOrEqual todayOrEarlier = new LessThanOrEqual("order_date", LocalDate.now());
     * // SQL: order_date <= '2023-10-15' (assuming today's date)
     *
     * // Set maximum allowed discount
     * LessThanOrEqual maxDiscount = new LessThanOrEqual("discount_percent", 50);
     * // SQL: discount_percent <= 50
     *
     * // Check temperature threshold
     * LessThanOrEqual tempThreshold = new LessThanOrEqual("temperature", 25.5);
     * // SQL: temperature <= 25.5
     *
     * // Use with subquery - find products priced at or below average
     * SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products");
     * LessThanOrEqual atOrBelowAverage = new LessThanOrEqual("price", avgPrice);
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public LessThanOrEqual(final String propName, final Object propValue) {
        super(propName, Operator.LESS_THAN_OR__EQUAL, propValue);
    }
}