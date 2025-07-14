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
 * floating-point values are finite (not positive or negative infinity).
 * 
 * <p>In floating-point arithmetic, infinity values can result from operations like
 * division by zero (1.0/0.0 = Infinity) or overflow in calculations. This condition
 * helps filter out such infinite values.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculated ratio is finite
 * IsNotInfinite condition = new IsNotInfinite("price_ratio");
 * // This would generate: price_ratio IS NOT INFINITE
 * 
 * // Ensure measurement values are within finite range
 * IsNotInfinite measurement = new IsNotInfinite("sensor_reading");
 * // This would generate: sensor_reading IS NOT INFINITE
 * }</pre>
 * 
 * @see IsInfinite
 * @see IsNotNaN
 */
public class IsNotInfinite extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNotInfinite() {
    }

    /**
     * Creates a new IsNotInfinite condition for the specified property.
     * This condition checks if the property's numeric value is NOT infinite
     * (neither positive nor negative infinity).
     *
     * @param propName the name of the numeric property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Ensure calculated values are finite
     * IsNotInfinite calcCheck = new IsNotInfinite("calculation_result");
     * // Generates: calculation_result IS NOT INFINITE
     * 
     * // Filter for records with finite growth rates
     * IsNotInfinite growthCheck = new IsNotInfinite("growth_rate");
     * // Generates: growth_rate IS NOT INFINITE
     * }</pre>
     */
    public IsNotInfinite(final String propName) {
        super(propName, IsInfinite.INFINITE);
    }
}