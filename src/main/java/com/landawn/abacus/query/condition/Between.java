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
 * Represents a BETWEEN condition in SQL queries.
 * The BETWEEN operator selects values within a given range, inclusive of both endpoints.
 *
 * <p>The BETWEEN condition is a convenient way to filter data within a range.
 * It's equivalent to: property &gt;= minValue AND property &lt;= maxValue.
 * The condition evaluates to true if: minValue &lt;= propertyValue &lt;= maxValue.</p>
 *
 * <p>BETWEEN can be used with various data types:</p>
 * <ul>
 *   <li>Numbers: BETWEEN 1 AND 100</li>
 *   <li>Dates: BETWEEN '2023-01-01' AND '2023-12-31'</li>
 *   <li>Strings: BETWEEN 'A' AND 'M' (alphabetical range)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Numeric range
 * Between ageRange = new Between("age", 18, 65);
 * // SQL: age BETWEEN 18 AND 65
 *
 * // Date range for current year
 * Between yearRange = new Between("orderDate",
 *     LocalDate.of(2024, 1, 1),
 *     LocalDate.of(2024, 12, 31));
 * // SQL: orderDate BETWEEN '2024-01-01' AND '2024-12-31'
 *
 * // Price range with subqueries
 * SubQuery minPrice = Filters.subQuery("SELECT MIN(price) FROM products");
 * SubQuery maxPrice = Filters.subQuery("SELECT MAX(price) FROM products");
 * Between priceRange = new Between("price", minPrice, maxPrice);
 * // SQL: price BETWEEN (SELECT MIN(price)...) AND (SELECT MAX(price)...)
 *
 * // String range (alphabetical)
 * Between nameRange = new Between("lastName", "A", "M");
 * // SQL: lastName BETWEEN 'A' AND 'M'
 * }</pre>
 *
 * @see AbstractBetween
 * @see NotBetween
 * @see Condition
 */
public class Between extends AbstractBetween {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Between instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Between() {
    }

    /**
     * Creates a new BETWEEN condition.
     * The condition checks if the property value falls within the specified range, inclusive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if age is between 18 and 65 (inclusive)
     * Between ageRange = new Between("age", 18, 65);
     *
     * // Check if salary is within a range
     * Between salaryRange = new Between("salary", 50000, 100000);
     *
     * // Check if date is in current year
     * Between currentYear = new Between("createdDate",
     *     LocalDate.of(2024, 1, 1),
     *     LocalDate.of(2024, 12, 31));
     *
     * // Use with subqueries for dynamic ranges
     * SubQuery avgMinus10 = Filters.subQuery("SELECT AVG(score) - 10 FROM scores");
     * SubQuery avgPlus10 = Filters.subQuery("SELECT AVG(score) + 10 FROM scores");
     * Between nearAverage = new Between("score", avgMinus10, avgPlus10);
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param minValue the minimum value (inclusive) (can be null, literal value, or subquery)
     * @param maxValue the maximum value (inclusive) (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public Between(final String propName, final Object minValue, final Object maxValue) {
        super(propName, Operator.BETWEEN, minValue, maxValue);
    }
}
