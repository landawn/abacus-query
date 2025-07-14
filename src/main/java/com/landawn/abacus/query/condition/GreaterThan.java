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
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a condition where age > 18
 * GreaterThan condition = new GreaterThan("age", 18);
 * 
 * // Use in a query
 * query.where(new GreaterThan("price", 100.0));
 * 
 * // Can be used with various data types
 * GreaterThan dateCondition = new GreaterThan("createdDate", someDate);
 * GreaterThan stringCondition = new GreaterThan("name", "M"); // Alphabetical comparison
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
     * 
     * @param propName the name of the property to compare
     * @param propValue the value to compare against
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if salary is greater than 50000
     * GreaterThan condition = new GreaterThan("salary", 50000);
     * 
     * // Check if date is after a specific date
     * GreaterThan dateCondition = new GreaterThan("expiryDate", new Date());
     * }</pre>
     */
    public GreaterThan(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_THAN, propValue);
    }
}