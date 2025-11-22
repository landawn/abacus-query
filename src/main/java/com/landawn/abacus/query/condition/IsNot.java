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
 * Represents an IS NOT condition in SQL-like queries.
 * This class is used to create conditions that check if a property is not equal to a specific value
 * using the SQL IS NOT operator, which is primarily used for special SQL values like NULL, NaN, or INFINITE.
 * 
 * <p>The IS NOT operator is the negation of the IS operator and is essential for:
 * <ul>
 *   <li>Checking if a value is not NULL (most common use case)</li>
 *   <li>Checking if a numeric value is not NaN</li>
 *   <li>Checking if a numeric value is not INFINITE</li>
 *   <li>Comparing against other special SQL values</li>
 * </ul>
 * 
 * <p>This class serves as the base for more specific IS NOT conditions like IsNotNull,
 * IsNotNaN, and IsNotInfinite, but can also be used directly for custom IS NOT expressions.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a property is not null (prefer IsNotNull class)
 * IsNot notNull = new IsNot("email", null);
 * // Generates: email IS NOT NULL
 * 
 * // Check if not a specific expression value
 * Expression unknownExpr = CF.expr("UNKNOWN");
 * IsNot notUnknown = new IsNot("status", unknownExpr);
 * // Generates: status IS NOT UNKNOWN
 * }</pre>
 * 
 * @see Binary
 * @see Is
 * @see Condition
 */
public class IsNot extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNot instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNot() {
    }

    /**
     * Creates a new IS NOT condition with the specified property name and value.
     * This condition checks if the property is not equal to the specified value using
     * the SQL IS NOT operator. This operator is essential for negating comparisons
     * with special SQL values that have no direct inequality semantics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for NOT NULL (though IsNotNull is preferred)
     * IsNot notNull = new IsNot("phone_number", null);
     * // Generates: phone_number IS NOT NULL
     * 
     * // Check if not NaN
     * Expression nanExpr = CF.expr("NAN");
     * IsNot notNaN = new IsNot("temperature", nanExpr);
     * // Generates: temperature IS NOT NAN
     * 
     * // Check if not a custom value
     * Expression pendingExpr = CF.expr("PENDING");
     * IsNot notPending = new IsNot("order_status", pendingExpr);
     * // Generates: order_status IS NOT PENDING
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public IsNot(final String propName, final Object propValue) {
        super(propName, Operator.IS_NOT, propValue);
    }
}