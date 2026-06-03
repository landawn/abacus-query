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
 * Represents a condition that checks if a numeric property value is infinite.
 * This class extends {@link Is} to provide a specialized condition for checking
 * infinity values in floating-point columns (both positive and negative infinity).
 * This condition is useful for identifying numeric overflow conditions and special
 * calculation results.
 * 
 * <p>In floating-point arithmetic, infinity can result from various operations:</p>
 * <ul>
 *   <li>Division by zero with non-zero numerator (e.g., 1.0/0.0 = Infinity, -1.0/0.0 = -Infinity)</li>
 *   <li>Operations that exceed the maximum representable value (overflow)</li>
 *   <li>Mathematical operations like log(0), exp(very_large_number)</li>
 *   <li>Accumulation in iterative calculations that grow without bound</li>
 * </ul>
 * 
 * <p>Common scenarios where checking for infinity is important:</p>
 * <ul>
 *   <li>Data quality validation after calculations</li>
 *   <li>Identifying division by zero errors</li>
 *   <li>Finding overflow conditions in numeric computations</li>
 *   <li>Debugging mathematical algorithms</li>
 *   <li>Ensuring data integrity before statistical analysis</li>
 * </ul>
 *
 * <p><b>SQL portability note:</b> {@code IS INFINITE} is not standard ANSI SQL. Support for this
 * predicate is vendor-specific. On databases that do not recognize {@code IS INFINITE}, use a
 * vendor-specific check (for example, comparison against the column's representation of infinity)
 * or filter at the application layer.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a calculation resulted in infinity
 * IsInfinite overflowCheck = new IsInfinite("growth_rate");
 * // Generates: growth_rate IS INFINITE
 * 
 * // Find records where division might have produced infinity
 * IsInfinite divisionCheck = new IsInfinite("calculated_ratio");
 * // Generates: calculated_ratio IS INFINITE
 * 
 * // Identify problematic calculations
 * IsInfinite calcError = new IsInfinite("risk_score");
 * // Generates: risk_score IS INFINITE
 * 
 * // Combine with other checks for comprehensive validation
 * Or invalidValue = new Or(
 *     new IsInfinite("metric_value"),
 *     new IsNaN("metric_value")
 * );
 * }</pre>
 * 
 * @see IsNotInfinite
 * @see IsNaN
 * @see Is
 */
public class IsInfinite extends Is {

    /**
     * Shared Expression instance representing INFINITE.
     * This constant is used internally to represent the INFINITE value in SQL.
     * It's shared across all instances to reduce memory overhead and ensure
     * consistency in SQL generation.
     */
    static final Expression INFINITE = Filters.expr("INFINITE");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsInfinite instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsInfinite() {
    }

    /**
     * Creates a new IsInfinite condition for the specified property.
     * This condition generates an {@code IS INFINITE} SQL clause to check if the property's
     * numeric value is infinite (either positive infinity or negative infinity).
     * This is particularly useful for identifying numeric overflow conditions,
     * division by zero results, and other exceptional calculation outcomes.
     *
     * <p>The generated SQL uses the {@code IS INFINITE} operator to properly detect both
     * positive and negative infinity values in a single check. Plain comparison operators
     * are not a portable way to test for infinity: there is no standard SQL literal that
     * matches "either positive or negative infinity", and infinity participates in special
     * arithmetic (for example, {@code infinity - infinity} is NaN), so {@code IS INFINITE}
     * is the reliable way to identify such values.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * IsInfinite rateCheck = new IsInfinite("interest_rate");
     * // Generates SQL: interest_rate IS INFINITE
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null} or empty)
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty
     */
    public IsInfinite(final String propName) {
        super(propName, INFINITE);
    }

}
