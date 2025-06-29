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

package com.landawn.abacus.condition;

import com.landawn.abacus.condition.ConditionFactory.CF;

/**
 * Represents a condition that checks if a numeric property value is infinite.
 * This class extends {@link Is} to provide a specialized condition for checking
 * infinity values in floating-point columns (both positive and negative infinity).
 * 
 * <p>In floating-point arithmetic, infinity can result from:
 * <ul>
 *   <li>Division by zero with non-zero numerator (e.g., 1.0/0.0 = Infinity)</li>
 *   <li>Operations that exceed the maximum representable value</li>
 *   <li>Mathematical operations like log(0)</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if a calculation resulted in infinity
 * IsInfinite condition = new IsInfinite("growth_rate");
 * // This would generate: growth_rate IS INFINITE
 * 
 * // Find records where division might have produced infinity
 * IsInfinite divisionCheck = new IsInfinite("calculated_ratio");
 * // This would generate: calculated_ratio IS INFINITE
 * }</pre>
 * 
 * @see IsNotInfinite
 * @see IsNaN
 */
public class IsInfinite extends Is {

    /**
     * Shared Expression instance representing INFINITE.
     * This constant is used internally to represent the INFINITE value in SQL.
     */
    static final Expression INFINITE = CF.expr("INFINITE");

    /**
     * Creates a new IsInfinite condition for the specified property.
     * This condition checks if the property's numeric value is infinite
     * (either positive or negative infinity).
     *
     * @param propName the name of the numeric property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Check for infinite values in calculations
     * IsInfinite rateCheck = new IsInfinite("interest_rate");
     * // Generates: interest_rate IS INFINITE
     * 
     * // Find records with overflow in computations
     * IsInfinite overflowCheck = new IsInfinite("computed_value");
     * // Generates: computed_value IS INFINITE
     * }</pre>
     */
    public IsInfinite(final String propName) {
        super(propName, INFINITE);
    }
}