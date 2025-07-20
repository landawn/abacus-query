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
 * Represents a greater than (>) condition in a query.
 * This condition checks if a property value is strictly greater than a specified value.
 * 
 * <p>The GreaterThan condition is used for range queries where you need to find records
 * with values exceeding a threshold. It supports various data types including numbers,
 * dates, strings (lexicographical comparison), and can also work with subqueries.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a condition where age > 18
 * GreaterThan ageCondition = new GreaterThan("age", 18);
 * 
 * // Use in a query for high-value orders
 * query.where(new GreaterThan("orderTotal", 1000.0));
 * 
 * // Date comparison - find future events
 * GreaterThan futureEvents = new GreaterThan("eventDate", LocalDate.now());
 * 
 * // String comparison (alphabetical)
 * GreaterThan afterM = new GreaterThan("lastName", "M");
 * }</pre>
 * 
 * @see Binary
 * @see GreaterEqual
 * @see LessThan
 * @see Condition
 */
public class GreaterThan extends Binary {

    // For Kryo
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
     * @param propName the name of the property to compare
     * @param propValue the value to compare against
     * @throws IllegalArgumentException if propName is null or empty
     */
    public GreaterThan(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_THAN, propValue);
    }
}