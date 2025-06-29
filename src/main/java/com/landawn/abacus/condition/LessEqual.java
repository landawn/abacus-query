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

package com.landawn.abacus.condition;

/**
 * Represents a less-than-or-equal-to (&lt;=) comparison condition in SQL-like queries.
 * This class is used to create conditions that check if a property value is less than
 * or equal to a specified value.
 * 
 * <p>This condition is commonly used for:
 * <ul>
 *   <li>Setting upper bounds on numeric values</li>
 *   <li>Date comparisons (on or before a certain date)</li>
 *   <li>String comparisons using lexicographical ordering</li>
 *   <li>Implementing inclusive range queries</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if age is 18 or younger
 * LessEqual condition = new LessEqual("age", 18);
 * // This would generate: age <= 18
 * 
 * // Check if price is at most $99.99
 * LessEqual priceCondition = new LessEqual("price", 99.99);
 * // This would generate: price <= 99.99
 * 
 * // Check if date is on or before a deadline
 * LessEqual dateCondition = new LessEqual("submit_date", "2023-12-31");
 * // This would generate: submit_date <= '2023-12-31'
 * }</pre>
 * 
 * @see LessThan
 * @see GreaterEqual
 * @see Between
 */
public class LessEqual extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    LessEqual() {
    }

    /**
     * Creates a new less-than-or-equal-to condition with the specified property name and value.
     * This condition checks if the property value is less than or equal to the specified value.
     *
     * @param propName the name of the property to compare. Must not be null.
     * @param propValue the value to compare against. Can be any comparable value (number, string, date, etc.).
     * 
     * <p>Example:
     * <pre>{@code
     * // Limit quantity to maximum stock
     * LessEqual stockCheck = new LessEqual("quantity", 100);
     * // Generates: quantity <= 100
     * 
     * // Find all orders on or before today
     * LessEqual dateCheck = new LessEqual("order_date", LocalDate.now());
     * // Generates: order_date <= '2023-10-15' (assuming today's date)
     * }</pre>
     */
    public LessEqual(final String propName, final Object propValue) {
        super(propName, Operator.LESS_EQUAL, propValue);
    }
}