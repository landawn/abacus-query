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
 * floating-point values are valid numbers (not NaN). This is essential for ensuring
 * data quality and preventing NaN values from propagating through calculations.
 * 
 * <p>NaN (Not a Number) values can severely impact data analysis and calculations because:
 * <ul>
 *   <li>NaN propagates through calculations (any operation with NaN returns NaN)</li>
 *   <li>NaN comparisons always return false (except != which returns true)</li>
 *   <li>Aggregate functions may produce unexpected results with NaN values</li>
 *   <li>Statistical analyses require valid numeric data</li>
 * </ul>
 * 
 * <p>This condition is useful for:
 * <ul>
 *   <li>Filtering out invalid numeric values before calculations</li>
 *   <li>Ensuring data quality in numeric columns</li>
 *   <li>Preparing data for statistical analysis</li>
 *   <li>Validating calculation results</li>
 *   <li>Implementing business rules that require valid numbers</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculation result is a valid number
 * IsNotNaN validResult = new IsNotNaN("calculation_result");
 * // Generates: calculation_result IS NOT NAN
 * 
 * // Filter for rows with valid profit ratios
 * IsNotNaN validRatio = new IsNotNaN("profit_ratio");
 * // Generates: profit_ratio IS NOT NAN
 * 
 * // Ensure sensor readings are valid
 * IsNotNaN validReading = new IsNotNaN("temperature");
 * // Generates: temperature IS NOT NAN
 * 
 * // Combine with other validations for complete numeric validation
 * And validNumber = new And(
 *     new IsNotNaN("score"),
 *     new IsNotInfinite("score"),
 *     new Between("score", 0, 100)
 * );
 * }</pre>
 * 
 * @see IsNaN
 * @see IsNotInfinite
 * @see IsNot
 */
public class IsNotNaN extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNotNaN instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNotNaN() {
    }

    /**
     * Creates a new IsNotNaN condition for the specified property.
     * This condition checks if the property's numeric value is NOT NaN (is a valid number),
     * which is essential for ensuring data quality and preventing calculation errors.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Ensure temperature readings are valid numbers
     * IsNotNaN tempCheck = new IsNotNaN("temperature");
     * // Generates: temperature IS NOT NAN
     * 
     * // Find all records with valid calculations
     * IsNotNaN calcCheck = new IsNotNaN("computed_value");
     * // Generates: computed_value IS NOT NAN
     * 
     * // Validate statistical measures
     * IsNotNaN statsCheck = new IsNotNaN("standard_deviation");
     * // Generates: standard_deviation IS NOT NAN
     * 
     * // Filter for valid financial metrics
     * IsNotNaN financeCheck = new IsNotNaN("return_on_investment");
     * // Generates: return_on_investment IS NOT NAN
     * 
     * // Ensure scientific measurements are valid
     * IsNotNaN measurementCheck = new IsNotNaN("ph_level");
     * // Generates: ph_level IS NOT NAN
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public IsNotNaN(final String propName) {
        super(propName, IsNaN.NAN);
    }
}