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
 * Represents a condition that checks if a numeric property value is infinite.
 * This class extends {@link Is} to provide a specialized condition for checking
 * infinity values in floating-point columns (both positive and negative infinity).
 * This condition is useful for identifying numeric overflow conditions and special
 * calculation results.
 * 
 * <p>In floating-point arithmetic, infinity can result from various operations:
 * <ul>
 *   <li>Division by zero with non-zero numerator (e.g., 1.0/0.0 = Infinity, -1.0/0.0 = -Infinity)</li>
 *   <li>Operations that exceed the maximum representable value (overflow)</li>
 *   <li>Mathematical operations like log(0), exp(very_large_number)</li>
 *   <li>Accumulation in iterative calculations that grow without bound</li>
 * </ul>
 * 
 * <p>Common scenarios where checking for infinity is important:
 * <ul>
 *   <li>Data quality validation after calculations</li>
 *   <li>Identifying division by zero errors</li>
 *   <li>Finding overflow conditions in numeric computations</li>
 *   <li>Debugging mathematical algorithms</li>
 *   <li>Ensuring data integrity before statistical analysis</li>
 * </ul>
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
    static final Expression INFINITE = CF.expr("INFINITE");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsInfinite instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsInfinite() {
    }

    /**
     * Creates a new IsInfinite condition for the specified property.
     * This condition generates an "IS INFINITE" SQL clause to check if the property's
     * numeric value is infinite (either positive infinity or negative infinity).
     * This is particularly useful for identifying numeric overflow conditions,
     * division by zero results, and other exceptional calculation outcomes.
     *
     * <p>The generated SQL uses the IS INFINITE operator to properly detect both
     * positive and negative infinity values. Standard comparison operators cannot
     * reliably test for infinity because infinity has special arithmetic properties
     * (e.g., infinity == infinity is true, but infinity - infinity is NaN).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for infinite values in calculations
     * IsInfinite rateCheck = new IsInfinite("interest_rate");
     * // Generates SQL: interest_rate IS INFINITE
     *
     * // Find records with overflow in computations
     * IsInfinite overflowCheck = new IsInfinite("computed_value");
     * // Generates SQL: computed_value IS INFINITE
     *
     * // Identify division by zero results (non-zero / 0 = infinity)
     * IsInfinite divByZero = new IsInfinite("average_per_unit");
     * // Generates SQL: average_per_unit IS INFINITE
     *
     * // Check exponential calculation results
     * IsInfinite expCheck = new IsInfinite("exponential_growth");
     * // Generates SQL: exponential_growth IS INFINITE
     *
     * // Use in query builders to identify problematic calculations
     * List<Calculation> infiniteResults = queryExecutor
     *     .prepareQuery(Calculation.class)
     *     .where(new IsInfinite("result_value"))
     *     .list();
     * }</pre>
     *
     * @param propName the property/column name to check for infinity (must not be null or empty)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public IsInfinite(final String propName) {
        super(propName, INFINITE);
    }
}