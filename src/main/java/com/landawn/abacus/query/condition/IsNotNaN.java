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
 * Represents a condition that checks if a numeric property value is NOT NaN (Not a Number).
 * This class extends {@link IsNot} to provide a specialized condition for checking that
 * floating-point values are valid numbers (not NaN).
 * 
 * <p>This condition is useful for filtering out invalid numeric values and ensuring
 * that calculations or numeric comparisons can be performed safely.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculation result is a valid number
 * IsNotNaN condition = new IsNotNaN("calculation_result");
 * // This would generate: calculation_result IS NOT NAN
 * 
 * // Filter for rows with valid profit ratios
 * IsNotNaN validRatio = new IsNotNaN("profit_ratio");
 * // This would generate: profit_ratio IS NOT NAN
 * }</pre>
 * 
 * @see IsNaN
 * @see IsNotInfinite
 */
public class IsNotNaN extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNotNaN() {
    }

    /**
     * Creates a new IsNotNaN condition for the specified property.
     * This condition checks if the property's numeric value is NOT NaN (is a valid number).
     *
     * @param propName the name of the numeric property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Ensure temperature readings are valid numbers
     * IsNotNaN tempCheck = new IsNotNaN("temperature");
     * // Generates: temperature IS NOT NAN
     * 
     * // Find all records with valid calculations
     * IsNotNaN calcCheck = new IsNotNaN("computed_value");
     * // Generates: computed_value IS NOT NAN
     * }</pre>
     */
    public IsNotNaN(final String propName) {
        super(propName, IsNaN.NAN);
    }
}