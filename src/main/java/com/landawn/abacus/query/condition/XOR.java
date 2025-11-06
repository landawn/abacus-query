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
 * // Ensure exactly one authentication method is enabled
 * XOR authCheck = new XOR("usePasswordAuth", true);
 * // Can be combined with another XOR for biometric auth
 * // Ensures user has password XOR biometric, but not both
 *
 * // Validate mutually exclusive discount types
 * XOR studentDiscount = new XOR("hasStudentDiscount", true);
 * XOR seniorDiscount = new XOR("hasSeniorDiscount", true);
 * // Business rule: customer can have one discount type, not both
 *
 * // Membership tiers - exactly one active
 * XOR basicMember = new XOR("isBasicMember", true);
 * XOR premiumMember = new XOR("isPremiumMember", true);
 * // Ensures member has exactly one tier active
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
 * @see And
 * @see Or
 * @see Not
 * @see Binary
 */
public class XOR extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized XOR instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    XOR() {
    }

    /**
     * Creates a new XOR condition.
     * The condition evaluates to true when exactly one of the operands is true,
     * providing exclusive-or logic for mutually exclusive conditions.
     *
     * <p>The XOR condition is true when the property value and the provided value
     * differ in their boolean evaluation (one true, one false). This is useful for
     * enforcing business rules where only one of two options should be active.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Enforce exactly one authentication method
     * XOR passwordAuth = new XOR("usePassword", true);
     * // This should be paired with XOR for alternative auth
     * // to ensure exactly one method is enabled
     *
     * // Mutually exclusive payment methods
     * XOR creditCard = new XOR("useCreditCard", true);
     * XOR paypal = new XOR("usePaypal", true);
     * // Ensures customer selects one payment method, not both
     *
     * // Subscription model - exactly one tier active
     * XOR freeTier = new XOR("hasFreeTier", true);
     * XOR paidTier = new XOR("hasPaidTier", true);
     * // User should be on free XOR paid, not both
     *
     * // Validation for exclusive features
     * XOR feature1 = new XOR("hasFeatureA", true);
     * XOR feature2 = new XOR("hasFeatureB", true);
     * // Features A and B are mutually exclusive
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param propValue the value to compare against. Can be null, a literal value, Expression, or SubQuery.
     * @throws IllegalArgumentException if propName is null or empty
     */
    public XOR(final String propName, final Object propValue) {
        super(propName, Operator.XOR, propValue);
    }
}