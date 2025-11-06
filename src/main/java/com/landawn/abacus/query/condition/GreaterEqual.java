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
 * Represents a greater-than-or-equal-to (>=) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is greater than
 * or equal to a specified value. The greater-than-or-equal operator is fundamental for
 * implementing lower bounds, inclusive ranges, and various filtering scenarios.
 *
 * <p>This condition is commonly used for:
 * <ul>
 *   <li>Setting lower bounds on numeric values (minimum thresholds)</li>
 *   <li>Date comparisons (on or after a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing inclusive range queries with LessEqual</li>
 *   <li>Minimum requirements, start dates, and eligibility checks</li>
 * </ul>
 *
 * <p>The GreaterEqual operator works with various data types:
 * <ul>
 *   <li>Numbers: Natural numeric comparison</li>
 *   <li>Dates/Times: Chronological comparison</li>
 *   <li>Strings: Lexicographical (dictionary) order</li>
 *   <li>Any Comparable type supported by the database</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if age is 18 or older
 * GreaterEqual ageLimit = new GreaterEqual("age", 18);
 * // SQL: age >= 18
 *
 * // Check if price is at least $99.99
 * GreaterEqual priceMin = new GreaterEqual("price", 99.99);
 * // SQL: price >= 99.99
 *
 * // Check if date is on or after a start date
 * GreaterEqual startDate = new GreaterEqual("start_date", "2023-01-01");
 * // SQL: start_date >= '2023-01-01'
 *
 * // Combine with LessEqual for inclusive range
 * And priceRange = new And(
 *     new GreaterEqual("price", 10.00),
 *     new LessEqual("price", 100.00)
 * );
 * // SQL: (price >= 10.00) AND (price <= 100.00)
 * }</pre>
 *
 * @see Binary
 * @see GreaterThan
 * @see LessEqual
 * @see Condition
 */
public class GreaterEqual extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized GreaterEqual instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    GreaterEqual() {
    }

    /**
     * Creates a new GreaterEqual condition.
     * The condition evaluates to true when the property value is greater than or equal to the specified value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if salary is at least 50000
     * GreaterEqual salaryCondition = new GreaterEqual("salary", 50000);
     * 
     * // Check if score meets minimum requirement
     * GreaterEqual scoreCondition = new GreaterEqual("score", 60);
     * 
     * // Check if date is on or after a specific date
     * GreaterEqual dateCondition = new GreaterEqual("expiryDate", LocalDate.now());
     * }</pre>
     * 
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public GreaterEqual(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_EQUAL, propValue);
    }
}