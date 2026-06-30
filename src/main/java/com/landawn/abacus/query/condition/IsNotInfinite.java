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
 * Represents a condition that checks if a numeric property value is NOT infinite.
 * This class extends {@link IsNot} to provide a specialized condition for checking that
 * floating-point values are non-infinite (not positive or negative infinity). This predicate
 * does not exclude NaN; combine it with {@link IsNotNaN} when finite numeric validation is required.
 * 
 * <p>In floating-point arithmetic, infinity values can result from various operations:</p>
 * <ul>
 *   <li>Division by zero with non-zero numerator (e.g., 1.0/0.0 = Infinity, -1.0/0.0 = -Infinity)</li>
 *   <li>Operations that exceed the maximum representable value (overflow)</li>
 *   <li>Mathematical operations like log(0) or tan(π/2)</li>
 *   <li>Accumulation of rounding errors in iterative calculations</li>
 * </ul>
 *
 * <p>This condition helps filter out such infinite values to:</p>
 * <ul>
 *   <li>Exclude positive and negative infinity from numeric data</li>
 *   <li>Prevent propagation of infinity in calculations</li>
 *   <li>Maintain meaningful numeric ranges</li>
 *   <li>Support finite-value validation when combined with a NaN check</li>
 * </ul>
 *
 * <p><b>SQL portability note:</b> {@code IS NOT INFINITE} is not standard ANSI SQL. Support for
 * this predicate is vendor-specific. On databases that do not recognize it, use a vendor-specific
 * check or filter at the application layer.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a calculated ratio is not infinite
 * IsNotInfinite nonInfiniteRatio = new IsNotInfinite("price_ratio");
 * // SQL: price_ratio IS NOT INFINITE
 * 
 * // Ensure measurement values are not positive or negative infinity
 * IsNotInfinite nonInfiniteMeasurement = new IsNotInfinite("sensor_reading");
 * // SQL: sensor_reading IS NOT INFINITE
 * 
 * // Filter for non-infinite growth rates
 * IsNotInfinite nonInfiniteGrowth = new IsNotInfinite("growth_rate");
 * // SQL: growth_rate IS NOT INFINITE
 * 
 * // Combine with other conditions for complete validation
 * And validNumeric = new And(
 *     new IsNotInfinite("calculated_value"),
 *     new IsNotNaN("calculated_value"),
 *     new GreaterThan("calculated_value", 0)
 * );
 * }</pre>
 * 
 * @see Binary
 * @see Is
 * @see IsNot
 * @see IsNull
 * @see IsNotNull
 * @see IsNaN
 * @see IsNotNaN
 * @see IsInfinite
 * @see Condition
 */
public class IsNotInfinite extends IsNot {

    /**
     * Shared Expression instance representing INFINITE.
     * This constant mirrors {@link IsInfinite#INFINITE} for symmetry, allowing code in this class
     * to reference its own constant rather than reaching into the positive counterpart.
     */
    static final Expression INFINITE = IsInfinite.INFINITE;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNotInfinite instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNotInfinite() {
    }

    /**
     * Creates a new IsNotInfinite condition for the specified property.
     * This condition generates an {@code IS NOT INFINITE} SQL clause to check if the property's
     * numeric value is NOT infinite (neither positive infinity nor negative infinity).
     * It does not exclude NaN; combine it with {@link IsNotNaN} before relying on a value as finite.
     *
     * <p>The generated SQL uses the {@code IS NOT INFINITE} operator to properly verify non-infinite
     * values. Standard comparison operators cannot reliably test for the absence of
     * infinity because infinity has special arithmetic properties. {@code IS NOT INFINITE} is
     * the correct way to filter out infinite numeric values.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * IsNotInfinite calcCheck = new IsNotInfinite("calculation_result");
     * // SQL: calculation_result IS NOT INFINITE
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null}, empty, or blank)
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public IsNotInfinite(final String propName) {
        super(propName, INFINITE);
    }

}
