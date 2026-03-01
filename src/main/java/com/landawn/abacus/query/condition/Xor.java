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
 * Represents an XOR (exclusive OR) logical operator in SQL queries.
 * The XOR condition evaluates to true if exactly one of the two operands is true,
 * but not both. This is useful for enforcing mutually exclusive conditions.
 *
 * <p>The XOR operator is a binary logical operator that returns true when the operands
 * have different boolean values. It follows this truth table:
 * <ul>
 *   <li>TRUE XOR TRUE = FALSE</li>
 *   <li>TRUE XOR FALSE = TRUE</li>
 *   <li>FALSE XOR TRUE = TRUE</li>
 *   <li>FALSE XOR FALSE = FALSE</li>
 * </ul>
 *
 * <p>In SQL terms, XOR is equivalent to: (A AND NOT B) OR (NOT A AND B)</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns true when operands differ in boolean evaluation</li>
 *   <li>Returns false when both operands are true or both are false</li>
 *   <li>Useful for enforcing mutually exclusive business rules</li>
 *   <li>Database support varies - MySQL has native XOR, others may need expansion</li>
 * </ul>
 *
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Enforcing exactly one of two flags is set</li>
 *   <li>Mutually exclusive discount or promotion types</li>
 *   <li>Either-or membership or subscription models</li>
 *   <li>Validation rules where only one option should be selected</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic XOR usage (MySQL syntax)
 * Xor xor = new Xor("usePasswordAuth", true);
 * // SQL: usePasswordAuth XOR true
 *
 * // XOR with column and value
 * Xor flagCheck = new Xor("isActive", 1);
 * // SQL: isActive XOR 1
 * // True when isActive is 0 or false, false when isActive is non-zero
 *
 * // For portable mutually exclusive conditions, use AND/OR/NOT:
 * // Instead of: XOR for checking "exactly one of A or B"
 * // Use: (A AND NOT B) OR (NOT A AND B)
 * Or exclusiveOr = new Or(
 *     new And(Filters.equal("hasPasswordAuth", true), Filters.equal("hasBiometricAuth", false)),
 *     new And(Filters.equal("hasPasswordAuth", false), Filters.equal("hasBiometricAuth", true))
 * );
 * // This ensures exactly one authentication method is active
 * }</pre>
 *
 * <p>Database compatibility notes:</p>
 * <ul>
 *   <li>MySQL: Native XOR operator support</li>
 *   <li>PostgreSQL: May require expansion to (A AND NOT B) OR (NOT A AND B)</li>
 *   <li>Oracle: May require expansion to equivalent AND/OR/NOT expression</li>
 *   <li>SQL Server: May require expansion to equivalent AND/OR/NOT expression</li>
 * </ul>
 *
 * <p>Relationship to other logical operators:</p>
 * <ul>
 *   <li>XOR requires exactly one of two conditions to be true (exclusive or)</li>
 *   <li>OR requires at least one condition to be true (inclusive or)</li>
 *   <li>AND requires all conditions to be true</li>
 *   <li>NOT negates a condition</li>
 * </ul>
 *
 * @see And
 * @see Or
 * @see Not
 * @see Binary
 */
public class Xor extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Xor instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Xor() {
    }

    /**
     * Creates a new XOR condition that evaluates to true when exactly one of the operands is true.
     * The condition provides exclusive-or logic for mutually exclusive conditions.
     *
     * <p>The XOR condition is true when the property value and the provided value
     * differ in their boolean evaluation (one true, one false). This is useful for
     * enforcing business rules where only one of two options should be active.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Note: XOR is a binary operator in SQL, typically used with boolean expressions
     * // The exact SQL syntax varies by database
     *
     * // MySQL-style XOR - ensure exactly one condition is true
     * Xor exclusiveAuth = new Xor("usePasswordAuth", true);
     * // SQL: usePasswordAuth XOR true
     * // This evaluates to true when usePasswordAuth != true
     *
     * // XOR with numeric values (treated as boolean: 0=false, non-zero=true)
     * Xor xorCheck = new Xor("flagA", 1);
     * // SQL: flagA XOR 1
     *
     * // Database compatibility note:
     * // MySQL: Native XOR operator support
     * // Other databases: May need to be expanded to (A AND NOT B) OR (NOT A AND B)
     * // Consider using AND/OR/NOT combinations for better portability:
     * // Or portableXor = new Or(
     * //     new And(condition1, new Not(condition2)),
     * //     new And(new Not(condition1), condition2)
     * // );
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param propValue the value to compare against. Can be null, a literal value, Expression, or SubQuery.
     * @throws IllegalArgumentException if propName is null or empty
     */
    public Xor(final String propName, final Object propValue) {
        super(propName, Operator.XOR, propValue);
    }
}