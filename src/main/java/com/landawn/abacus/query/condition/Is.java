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
 * Represents an IS condition in SQL-like queries.
 * This class is used to create conditions that check if a property is equal to a specific value
 * using the SQL IS operator, which is primarily used for special SQL values like NULL, NaN, or INFINITE.
 * 
 * <p>The IS operator differs from the equals (=) operator in that it can properly compare
 * special SQL values that have no direct equality semantics. The most common use case is
 * checking for NULL values, though it also applies to floating-point special values in
 * databases that support them.
 * 
 * <p>Common use cases:
 * <ul>
 *   <li>Checking if a value is NULL (though IsNull class is preferred)</li>
 *   <li>Checking if a numeric value is NaN (though IsNaN class is preferred)</li>
 *   <li>Checking if a numeric value is INFINITE (though IsInfinite class is preferred)</li>
 *   <li>Custom IS expressions for database-specific values</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a property is null (prefer IsNull class)
 * Is nullCheck = new Is("email", null);
 * // Generates: email IS NULL
 * 
 * // Check against a custom expression
 * Expression customExpr = Filters.expr("UNKNOWN");
 * Is unknownCheck = new Is("status", customExpr);
 * // Generates: status IS UNKNOWN
 * }</pre>
 * 
 * @see Binary
 * @see IsNot
 * @see Condition
 */
public class Is extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Is instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Is() {
    }

    /**
     * Creates a new IS condition with the specified property name and value.
     * This condition checks if the property is equal to the specified value using the SQL IS operator.
     * The IS operator is essential for comparing special SQL values that cannot be compared
     * using the standard equals operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for NULL (though IsNull is preferred)
     * Is nullCheck = new Is("phone_number", null);
     * // Generates: phone_number IS NULL
     * 
     * // Check against a special expression
     * Expression nanExpr = Filters.expr("NAN");
     * Is nanCheck = new Is("temperature", nanExpr);
     * // Generates: temperature IS NAN
     * 
     * // Custom database-specific value
     * Expression unknownExpr = Filters.expr("UNKNOWN");
     * Is triStateCheck = new Is("verification_status", unknownExpr);
     * // Generates: verification_status IS UNKNOWN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty (validation performed by superclass {@link Binary})
     */
    public Is(final String propName, final Object propValue) {
        super(propName, Operator.IS, propValue);
    }
}