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
 * which is the negation of the {@code IS} operator and is used for {@code NULL}, Boolean truth
 * values (including the standard {@code UNKNOWN}), and explicit database-specific SQL expressions such as
 * {@code NAN} or {@code INFINITE}.
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
 * use {@code IsNot} directly only when supplying a Boolean or custom right-hand expression.
 * Arbitrary strings and numbers are rejected because forms such as {@code column IS NOT 'value'}
 * and {@code column IS NOT 5} are not portable SQL predicates.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a property is not null (prefer IsNotNull class)
 * IsNot notNull = new IsNot("email", null);
 * // SQL: email IS NOT NULL
 *
 * // Check if not a specific expression value
 * SqlExpression unknownExpr = Filters.expr("UNKNOWN");
 * IsNot notUnknown = new IsNot("status", unknownExpr);
 * // SQL: status IS NOT UNKNOWN
 * }</pre>
 *
 * @see Binary
 * @see Is
 * @see IsNull
 * @see IsNotNull
 * @see IsNaN
 * @see IsNotNaN
 * @see IsInfinite
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
     * is {@code null}, a Boolean, or an {@link SqlExpression} representing a special SQL keyword
     * such as {@code NULL}, {@code FALSE}, or {@code UNKNOWN}.
     *
     * <p>If {@code propValue} is the Java {@code null} reference, the generated SQL collapses to
     * {@code propName IS NOT NULL}. A Boolean is normalized at construction to the SQL keyword expression
     * {@code TRUE}/{@code FALSE}, so it is rendered inline ({@code propName IS NOT FALSE}) by every rendering
     * path and contributes no bind parameter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for NOT NULL (though IsNotNull is preferred)
     * IsNot notNull = new IsNot("phone_number", null);
     * // SQL: phone_number IS NOT NULL
     *
     * // Boolean truth value: rendered as a keyword, never bound as a parameter
     * IsNot notFalse = new IsNot("active", false);
     * // SQL: active IS NOT FALSE   (parameters: [])
     *
     * // Check if not a custom value
     * SqlExpression unknownExpr = Filters.expr("UNKNOWN");
     * IsNot notUnknown = new IsNot("verification_status", unknownExpr);
     * // SQL: verification_status IS NOT UNKNOWN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null}, empty, or blank)
     * @param propValue the right-hand value of the IS NOT predicate; must be {@code null} (renders as
     *            {@code IS NOT NULL}), a Boolean (normalized to the {@code TRUE}/{@code FALSE} keyword), or an
     *            explicit non-blank {@link SqlExpression} for a SQL keyword
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if
     *                                  {@code propValue} is not {@code null}, a Boolean, or an
     *                                  {@link SqlExpression}, or is a blank {@link SqlExpression}
     */
    public IsNot(final String propName, final Object propValue) {
        super(propName, Operator.IS_NOT, propValue);
    }
}
