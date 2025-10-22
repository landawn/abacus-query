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
 * Represents a greater-than (>) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is greater than
 * a specified value. The greater-than operator is fundamental for implementing lower bounds,
 * range queries, and various filtering scenarios where you need to exclude values at or
 * below a certain threshold.
 *
 * <p>This condition is commonly used for:
 * <ul>
 *   <li>Setting exclusive lower bounds on numeric values</li>
 *   <li>Date comparisons (after a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing exclusive range queries with LessThan</li>
 *   <li>Age requirements, threshold checks, and minimum validations</li>
 * </ul>
 *
 * <p>The GreaterThan operator works with various data types:
 * <ul>
 *   <li>Numbers: Natural numeric comparison</li>
 *   <li>Dates/Times: Chronological comparison</li>
 *   <li>Strings: Lexicographical (dictionary) order</li>
 *   <li>Any Comparable type supported by the database</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Check if age is greater than 18
 * GreaterThan adults = new GreaterThan("age", 18);
 * // SQL: age > 18
 *
 * // Check if price is greater than 99.99
 * GreaterThan premium = new GreaterThan("price", 99.99);
 * // SQL: price > 99.99
 *
 * // Check if date is after a start date
 * GreaterThan afterStart = new GreaterThan("start_date", "2023-01-01");
 * // SQL: start_date > '2023-01-01'
 *
 * // Combine with LessThan for exclusive range
 * And priceRange = new And(
 *     new GreaterThan("price", 10.00),
 *     new LessThan("price", 100.00)
 * );
 * // SQL: (price > 10.00) AND (price < 100.00)
 * }</pre>
 *
 * @see GreaterEqual
 * @see LessThan
 * @see Between
 * @see Binary
 */
public class GreaterThan extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized GreaterThan instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    GreaterThan() {
    }

    /**
     * Creates a new GreaterThan condition.
     * The condition evaluates to true when the property value is strictly greater than the specified value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if salary is above 50000
     * GreaterThan salaryCondition = new GreaterThan("salary", 50000);
     * 
     * // Check if temperature exceeds threshold
     * GreaterThan tempCondition = new GreaterThan("temperature", 100);
     * 
     * // Check if date is after a specific date
     * GreaterThan dateCondition = new GreaterThan("expiryDate", LocalDate.of(2024, 12, 31));
     * 
     * // Use with subquery - find products priced above average
     * SubQuery avgPrice = CF.subQuery("SELECT AVG(price) FROM products");
     * GreaterThan aboveAverage = new GreaterThan("price", avgPrice);
     * }</pre>
     * 
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public GreaterThan(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_THAN, propValue);
    }
}