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

import com.landawn.abacus.query.Filters;

/**
 * Represents a condition that checks if a numeric property value is NaN (Not a Number).
 * This class extends {@link Is} to provide a specialized condition for checking NaN values
 * in floating-point columns. NaN is a special floating-point value that represents
 * undefined or unrepresentable mathematical results.
 * 
 * <p>NaN (Not a Number) is a special floating-point value that represents an undefined
 * or unrepresentable value, typically resulting from invalid mathematical operations:</p>
 * <ul>
 *   <li>0/0 (zero divided by zero)</li>
 *   <li>∞/∞ (infinity divided by infinity)</li>
 *   <li>∞ - ∞ (infinity minus infinity)</li>
 *   <li>√(-1) (square root of negative number in real arithmetic)</li>
 *   <li>log(-1) (logarithm of negative number)</li>
 *   <li>Any operation involving an existing NaN value</li>
 * </ul>
 * 
 * <p>Important properties of NaN:</p>
 * <ul>
 *   <li>NaN is not equal to anything, including itself (under IEEE 754 {@code NaN == NaN} is {@code false})</li>
 *   <li>Standard SQL comparisons against NaN evaluate to UNKNOWN (and behave as {@code false} in WHERE clauses)</li>
 *   <li>NaN propagates through calculations (any arithmetic operation with NaN returns NaN)</li>
 *   <li>Must use {@code IS NAN} or {@code IS NOT NAN} to reliably test for NaN values</li>
 * </ul>
 *
 * <p><b>SQL portability note:</b> {@code IS NAN} is not standard ANSI SQL. Support for this
 * predicate is vendor-specific. On databases that do not recognize it, use a vendor-specific
 * check (for example PostgreSQL's {@code column = 'NaN'::float8}) or filter at the application layer.</p>
 * 
 * <p><b>Usage Examples:</b></p>
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
 * @see IsNull
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
    static final Expression NAN = Filters.expr("NAN");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNaN instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNaN() {
    }

    /**
     * Creates a new IsNaN condition for the specified property.
     * This condition generates an {@code IS NAN} SQL clause to check if the property's numeric
     * value is NaN (Not a Number), which represents an invalid or undefined mathematical
     * result. This is crucial for data validation, quality checks, and identifying
     * calculation errors in floating-point operations.
     *
     * <p>The generated SQL uses the {@code IS NAN} operator because NaN has special comparison
     * semantics in SQL: any comparison with NaN (including {@code = NAN}) evaluates to UNKNOWN
     * and therefore behaves as {@code false} in WHERE clauses. {@code IS NAN} is the only
     * reliable way to test for NaN values.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * IsNaN tempCheck = new IsNaN("temperature");
     * // Generates SQL: temperature IS NAN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null} or empty)
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty
     */
    public IsNaN(final String propName) {
        super(propName, NAN);
    }

}
