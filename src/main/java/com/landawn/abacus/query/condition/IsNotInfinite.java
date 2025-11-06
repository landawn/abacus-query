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
 * floating-point values are finite (not positive or negative infinity). This condition
 * is essential for data validation and ensuring numeric calculations remain within bounds.
 * 
 * <p>In floating-point arithmetic, infinity values can result from various operations:
 * <ul>
 *   <li>Division by zero with non-zero numerator (e.g., 1.0/0.0 = Infinity, -1.0/0.0 = -Infinity)</li>
 *   <li>Operations that exceed the maximum representable value (overflow)</li>
 *   <li>Mathematical operations like log(0) or tan(π/2)</li>
 *   <li>Accumulation of rounding errors in iterative calculations</li>
 * </ul>
 * 
 * <p>This condition helps filter out such infinite values to:
 * <ul>
 *   <li>Ensure data quality and validity</li>
 *   <li>Prevent propagation of infinity in calculations</li>
 *   <li>Maintain meaningful numeric ranges</li>
 *   <li>Support statistical analysis that requires finite values</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a calculated ratio is finite
 * IsNotInfinite finiteRatio = new IsNotInfinite("price_ratio");
 * // Generates: price_ratio IS NOT INFINITE
 * 
 * // Ensure measurement values are within finite range
 * IsNotInfinite validMeasurement = new IsNotInfinite("sensor_reading");
 * // Generates: sensor_reading IS NOT INFINITE
 * 
 * // Filter for valid growth rates
 * IsNotInfinite validGrowth = new IsNotInfinite("growth_rate");
 * // Generates: growth_rate IS NOT INFINITE
 * 
 * // Combine with other conditions for complete validation
 * And validNumeric = new And(
 *     new IsNotInfinite("calculated_value"),
 *     new IsNotNaN("calculated_value"),
 *     new GreaterThan("calculated_value", 0)
 * );
 * }</pre>
 * 
 * @see IsInfinite
 * @see IsNotNaN
 * @see IsNot
 */
public class IsNotInfinite extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNotInfinite instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNotInfinite() {
    }

    /**
     * Creates a new IsNotInfinite condition for the specified property.
     * This condition generates an "IS NOT INFINITE" SQL clause to check if the property's
     * numeric value is NOT infinite (neither positive infinity nor negative infinity).
     * This ensures that values are within the finite range of floating-point numbers
     * and is essential for data validation before performing calculations or analysis.
     *
     * <p>The generated SQL uses the IS NOT INFINITE operator to properly verify finite
     * values. Standard comparison operators cannot reliably test for the absence of
     * infinity because infinity has special arithmetic properties. IS NOT INFINITE is
     * the correct way to filter for finite numeric values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Ensure calculated values are finite
     * IsNotInfinite calcCheck = new IsNotInfinite("calculation_result");
     * // Generates SQL: calculation_result IS NOT INFINITE
     *
     * // Filter for records with finite growth rates
     * IsNotInfinite growthCheck = new IsNotInfinite("year_over_year_growth");
     * // Generates SQL: year_over_year_growth IS NOT INFINITE
     *
     * // Validate division results
     * IsNotInfinite divisionCheck = new IsNotInfinite("average_score");
     * // Generates SQL: average_score IS NOT INFINITE
     *
     * // Check scientific calculations
     * IsNotInfinite scientificCheck = new IsNotInfinite("exponential_result");
     * // Generates SQL: exponential_result IS NOT INFINITE
     *
     * // Use in query builders to filter finite values
     * List<Metric> finiteMetrics = queryExecutor
     *     .prepareQuery(Metric.class)
     *     .where(new IsNotInfinite("metric_value"))
     *     .list();
     * }</pre>
     *
     * @param propName the property/column name to check for non-infinity (must not be null or empty)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public IsNotInfinite(final String propName) {
        super(propName, IsInfinite.INFINITE);
    }
}