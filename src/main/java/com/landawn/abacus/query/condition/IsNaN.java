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

import com.landawn.abacus.query.condition.ConditionFactory.CF;

/**
 * Represents a condition that checks if a numeric property value is NaN (Not a Number).
 * This class extends {@link Is} to provide a specialized condition for checking NaN values
 * in floating-point columns.
 * 
 * <p>NaN (Not a Number) is a special floating-point value that represents an undefined
 * or unrepresentable value, typically resulting from operations like 0/0 or sqrt(-1).
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculation result is NaN
 * IsNaN condition = new IsNaN("calculation_result");
 * // This would generate: calculation_result IS NAN
 * 
 * // Useful for finding rows with invalid calculations
 * IsNaN ratioCheck = new IsNaN("profit_ratio");
 * // This would generate: profit_ratio IS NAN
 * }</pre>
 * 
 * @see IsNotNaN
 * @see IsInfinite
 */
public class IsNaN extends Is {

    /**
     * Shared Expression instance representing NAN.
     * This constant is used internally to represent the NAN value in SQL.
     */
    static final Expression NAN = CF.expr("NAN");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNaN() {
    }

    /**
     * Creates a new IsNaN condition for the specified property.
     * This condition checks if the property's numeric value is NaN (Not a Number).
     *
     * @param propName the name of the numeric property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Check if temperature reading is NaN
     * IsNaN tempCheck = new IsNaN("temperature");
     * // Generates: temperature IS NAN
     * 
     * // Find all records with invalid calculations
     * IsNaN calcCheck = new IsNaN("computed_value");
     * // Generates: computed_value IS NAN
     * }</pre>
     */
    public IsNaN(final String propName) {
        super(propName, NAN);
    }
}