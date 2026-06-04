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
 * Represents an SQL {@code IS NOT} predicate (e.g. {@code IS NOT NULL}).
 * This class creates conditions that test a property using the SQL {@code IS NOT} operator,
 * which is the negation of the {@code IS} operator and is primarily used for special SQL values
 * like {@code NULL}, {@code NaN}, or {@code INFINITE}.
 *
 * <p>The {@code IS NOT} operator is essential for:</p>
 * <ul>
 *   <li>Checking if a value is not NULL (most common use case)</li>
 *   <li>Checking if a numeric value is not NaN</li>
 *   <li>Checking if a numeric value is not INFINITE</li>
 *   <li>Negating comparisons against other special SQL values</li>
 * </ul>
 *
 * <p>This class serves as the base for more specific {@code IS NOT} conditions like {@link IsNotNull},
 * {@link IsNotNaN}, and {@link IsNotInfinite}. Prefer those subclasses for the well-known values;
 * use {@code IsNot} directly only when supplying a custom right-hand expression.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a property is not null (prefer IsNotNull class)
 * IsNot notNull = new IsNot("email", null);
 * // Generates: email IS NOT NULL
 *
 * // Check if not a specific expression value
 * Expression unknownExpr = Filters.expr("UNKNOWN");
 * IsNot notUnknown = new IsNot("status", unknownExpr);
 * // Generates: status IS NOT UNKNOWN
 * }</pre>
 *
 * @see Binary
 * @see Is
 * @see IsNotNull
 * @see IsNotNaN
 * @see IsNotInfinite
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
     * Creates a new {@code IS NOT} condition with the specified property name and right-hand value.
     * The generated SQL takes the form {@code propName IS NOT propValue}, where {@code propValue}
     * is typically an {@link Expression} representing a special SQL keyword such as {@code NULL}
     * or a custom expression like {@code UNKNOWN}.
     *
     * <p>If {@code propValue} is the Java {@code null} reference, the generated SQL collapses to
     * {@code propName IS NOT NULL}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for NOT NULL (though IsNotNull is preferred)
     * IsNot notNull = new IsNot("phone_number", null);
     * // Generates: phone_number IS NOT NULL
     *
     * // Check if not a custom value
     * Expression unknownExpr = Filters.expr("UNKNOWN");
     * IsNot notUnknown = new IsNot("verification_status", unknownExpr);
     * // Generates: verification_status IS NOT UNKNOWN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null} or empty)
     * @param propValue the right-hand value of the IS NOT predicate; may be {@code null} (renders as
     *            {@code IS NOT NULL}) or an {@link Expression} for a SQL keyword
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty
     */
    public IsNot(final String propName, final Object propValue) {
        super(propName, Operator.IS_NOT, propValue);
    }
}
