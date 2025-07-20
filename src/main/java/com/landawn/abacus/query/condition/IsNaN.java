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
 * in floating-point columns. NaN is a special floating-point value that represents
 * undefined or unrepresentable mathematical results.
 * 
 * <p>NaN (Not a Number) is a special floating-point value that represents an undefined
 * or unrepresentable value, typically resulting from invalid mathematical operations:
 * <ul>
 *   <li>0/0 (zero divided by zero)</li>
 *   <li>∞/∞ (infinity divided by infinity)</li>
 *   <li>∞ - ∞ (infinity minus infinity)</li>
 *   <li>√(-1) (square root of negative number in real arithmetic)</li>
 *   <li>log(-1) (logarithm of negative number)</li>
 *   <li>Any operation involving an existing NaN value</li>
 * </ul>
 * 
 * <p>Important properties of NaN:
 * <ul>
 *   <li>NaN is not equal to anything, including itself (NaN == NaN is false)</li>
 *   <li>Any comparison with NaN returns false (except != which returns true)</li>
 *   <li>NaN propagates through calculations (any operation with NaN returns NaN)</li>
 *   <li>Must use IS NAN or IS NOT NAN to check for NaN values</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculation result is NaN
 * IsNaN calcCheck = new IsNaN("calculation_result");
 * // Generates: calculation_result IS NAN
 * 
 * // Find rows with invalid calculations
 * IsNaN invalidRatio = new IsNaN("profit_ratio");
 * // Generates: profit_ratio IS NAN
 * 
 * // Check for failed mathematical operations
 * IsNaN mathError = new IsNaN("sqrt_result");
 * // Generates: sqrt_result IS NAN
 * 
 * // Combine with other validations
 * Or invalidNumeric = new Or(
 *     new IsNaN("score"),
 *     new IsInfinite("score"),
 *     new IsNull("score")
 * );
 * }</pre>
 * 
 * @see IsNotNaN
 * @see IsInfinite
 * @see Is
 */
public class IsNaN extends Is {

    /**
     * Shared Expression instance representing NAN.
     * This constant is used internally to represent the NAN value in SQL.
     * It's shared across all instances to reduce memory overhead and ensure
     * consistency in SQL generation.
     */
    static final Expression NAN = CF.expr("NAN");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNaN instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNaN() {
    }

    /**
     * Creates a new IsNaN condition for the specified property.
     * This condition checks if the property's numeric value is NaN (Not a Number),
     * which indicates an invalid or undefined mathematical result. This is crucial
     * for data validation and identifying calculation errors.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Check if temperature reading is NaN
     * IsNaN tempCheck = new IsNaN("temperature");
     * // Generates: temperature IS NAN
     * 
     * // Find all records with invalid calculations
     * IsNaN calcError = new IsNaN("computed_value");
     * // Generates: computed_value IS NAN
     * 
     * // Identify division errors
     * IsNaN divError = new IsNaN("average_score");
     * // Generates: average_score IS NAN
     * 
     * // Check statistical calculations
     * IsNaN statsCheck = new IsNaN("standard_deviation");
     * // Generates: standard_deviation IS NAN
     * 
     * // Validate sensor readings
     * IsNaN sensorError = new IsNaN("pressure_reading");
     * // Generates: pressure_reading IS NAN
     * }</pre>
     *
     * @param propName the name of the numeric property to check. Must not be null.
     *                 This should be a column containing floating-point values that
     *                 might contain NaN as a result of calculations.
     * @throws IllegalArgumentException if propName is null
     */
    public IsNaN(final String propName) {
        super(propName, NAN);
    }
}