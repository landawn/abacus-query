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
 * Represents an IS NOT condition in SQL-like queries.
 * This class is used to create conditions that check if a property is not equal to a specific value,
 * typically used for special SQL values like NULL, NaN, or INFINITE.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a property is not null
 * IsNot condition = new IsNot("age", null);
 * // This would generate: age IS NOT NULL
 * }</pre>
 * 
 * @see IsNotNull
 * @see IsNotNaN
 * @see IsNotInfinite
 */
public class IsNot extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNot() {
    }

    /**
     * Creates a new IS NOT condition with the specified property name and value.
     * This condition checks if the property is not equal to the specified value using SQL IS NOT operator.
     *
     * @param propName the name of the property to check. Must not be null.
     * @param propValue the value to compare against. Can be null or special Expression values.
     * 
     * <p>Example:
     * <pre>{@code
     * IsNot condition = new IsNot("status", someExpression);
     * // Generates: status IS NOT someExpression
     * }</pre>
     */
    public IsNot(final String propName, final Object propValue) {
        super(propName, Operator.IS_NOT, propValue);
    }
}