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
 * floating-point values are not NaN. This predicate does not exclude positive or negative
 * infinity; combine it with {@link IsNotInfinite} when finite numeric validation is required.
 * 
 * <p>NaN (Not a Number) values can severely impact data analysis and calculations because:</p>
 * <ul>
 *   <li>NaN propagates through calculations (any arithmetic operation with NaN returns NaN)</li>
 *   <li>NaN comparison behavior is database-specific, so ordinary equality and inequality
 *       operators are not a portable way to test whether a value is or is not NaN</li>
 *   <li>Aggregate functions may produce unexpected results with NaN values</li>
 *   <li>Statistical analyses require valid numeric data</li>
 * </ul>
 * 
 * <p>This condition is useful for:</p>
 * <ul>
 *   <li>Filtering out NaN values before calculations</li>
 *   <li>Ensuring numeric columns are NaN-free</li>
 *   <li>Preparing data for statistical analysis</li>
 *   <li>Validating calculation results</li>
 *   <li>Implementing business rules that must reject NaN values</li>
 * </ul>
 *
 * <p><b>SQL portability note:</b> {@code IS NOT NAN} is not standard ANSI SQL. Support for this
 * predicate is vendor-specific. On databases that do not recognize it, use a vendor-specific
 * check or filter at the application layer.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a calculation result is not NaN
 * IsNotNaN nonNaNResult = new IsNotNaN("calculation_result");
 * // SQL: calculation_result IS NOT NAN
 * 
 * // Filter for rows with NaN-free profit ratios
 * IsNotNaN nonNaNRatio = new IsNotNaN("profit_ratio");
 * // SQL: profit_ratio IS NOT NAN
 * 
 * // Ensure sensor readings are not NaN
 * IsNotNaN nonNaNReading = new IsNotNaN("temperature");
 * // SQL: temperature IS NOT NAN
 * 
 * // Combine with other validations for complete numeric validation
 * And validNumber = new And(
 *     new IsNotNaN("score"),
 *     new IsNotInfinite("score"),
 *     new Between("score", 0, 100)
 * );
 * }</pre>
 * 
 * @see Binary
 * @see Is
 * @see IsNot
 * @see IsNull
 * @see IsNotNull
 * @see IsNaN
 * @see IsInfinite
 * @see IsNotInfinite
 * @see Condition
 */
public class IsNotNaN extends IsNot {

    /**
     * Shared Expression instance representing NAN.
     * This constant mirrors {@link IsNaN#NAN} for symmetry, allowing code in this class
     * to reference its own constant rather than reaching into the positive counterpart.
     */
    static final Expression NAN = IsNaN.NAN;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNotNaN instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNotNaN() {
    }

    /**
     * Creates a new IsNotNaN condition for the specified property.
     * This condition generates an {@code IS NOT NAN} SQL clause to check if the property's
     * numeric value is NOT NaN. It does not exclude infinities; combine it with
     * {@link IsNotInfinite} when finite numeric values are required.
     *
     * <p>The generated SQL uses the explicit {@code IS NOT NAN} predicate rather than relying on
     * database-specific equality semantics. As noted above, the predicate itself is also
     * vendor-specific and must be supported by the selected database.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * IsNotNaN tempCheck = new IsNotNaN("temperature");
     * // SQL: temperature IS NOT NAN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null}, empty, or blank)
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public IsNotNaN(final String propName) {
        super(propName, NAN);
    }

}
