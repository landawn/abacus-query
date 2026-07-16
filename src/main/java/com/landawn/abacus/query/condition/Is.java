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
 * Represents an SQL {@code IS} predicate (e.g. {@code IS NULL}).
 * This class is used to create conditions that test a property using the SQL {@code IS} operator,
 * which is primarily used for special SQL values like {@code NULL}, {@code NaN}, or {@code INFINITE}
 * that cannot be tested with the regular equality ({@code =}) operator.
 *
 * <p>The {@code IS} operator differs from the equals ({@code =}) operator in that it properly handles
 * SQL three-valued logic for these special values. The most common use case is checking for
 * {@code NULL} values, though it also applies to floating-point special values in databases that
 * support them.</p>
 *
 * <p>Prefer the dedicated subclasses ({@link IsNull}, {@link IsNaN}, {@link IsInfinite}) over
 * {@code Is} directly when checking for those well-known values. Use {@code Is} only when
 * supplying a custom right-hand expression.</p>
 *
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Custom IS expressions for database-specific values such as {@code IS UNKNOWN}</li>
 *   <li>Building IS predicates programmatically when the right-hand side is dynamic</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if a property is null (prefer IsNull class)
 * Is nullCheck = new Is("email", null);
 * // SQL: email IS NULL
 *
 * // Check against a custom expression
 * SqlExpression customExpr = Filters.expr("UNKNOWN");
 * Is unknownCheck = new Is("status", customExpr);
 * // SQL: status IS UNKNOWN
 * }</pre>
 *
 * @see Binary
 * @see IsNot
 * @see IsNull
 * @see IsNotNull
 * @see IsNaN
 * @see IsNotNaN
 * @see IsInfinite
 * @see IsNotInfinite
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
     * Creates a new {@code IS} condition with the specified property name and right-hand value.
     * The generated SQL takes the form {@code propName IS propValue}, where {@code propValue} is
     * typically an {@link SqlExpression} representing a special SQL keyword such as {@code NULL}
     * or a custom expression like {@code UNKNOWN}.
     *
     * <p>If {@code propValue} is the Java {@code null} reference, the generated SQL collapses to
     * {@code propName IS NULL}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check for NULL (though IsNull is preferred)
     * Is nullCheck = new Is("phone_number", null);
     * // SQL: phone_number IS NULL
     *
     * // Custom database-specific value
     * SqlExpression unknownExpr = Filters.expr("UNKNOWN");
     * Is triStateCheck = new Is("verification_status", unknownExpr);
     * // SQL: verification_status IS UNKNOWN
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be {@code null}, empty, or blank)
     * @param propValue the right-hand value of the IS predicate; may be {@code null} (renders as
     *            {@code IS NULL}), an {@link SqlExpression} for a SQL keyword, or another non-structural
     *            value rendered by {@link Binary}; whether that value is valid with {@code IS} is dialect-specific
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if
     *                                  {@code propValue} is or contains a {@link Criteria}, SQL clause,
     *                                  JOIN, or {@code ON}/{@code USING} connector, or is/contains an
     *                                  {@link All}, {@link Any}, or {@link Some} quantified operand
     */
    public Is(final String propName, final Object propValue) {
        super(propName, Operator.IS, propValue);
    }
}
