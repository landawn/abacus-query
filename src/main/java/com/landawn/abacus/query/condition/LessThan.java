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
 * This class is used to create conditions that check if a property value is less than a specified value.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if age is less than 18
 * LessThan condition = new LessThan("age", 18);
 * // This would generate: age < 18
 * 
 * // Check if price is less than 99.99
 * LessThan priceCondition = new LessThan("price", 99.99);
 * // This would generate: price < 99.99
 * }</pre>
 */
public class LessThan extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    LessThan() {
    }

    /**
     * Creates a new less-than condition with the specified property name and value.
     * This condition checks if the property value is less than the specified value.
     *
     * @param propName the name of the property to compare. Must not be null.
     * @param propValue the value to compare against. Can be any comparable value (number, string, date, etc.).
     * 
     * <p>Example:
     * <pre>{@code
     * LessThan condition = new LessThan("salary", 50000);
     * // Generates: salary < 50000
     * }</pre>
     */
    public LessThan(final String propName, final Object propValue) {
        super(propName, Operator.LESS_THAN, propValue);
    }
}