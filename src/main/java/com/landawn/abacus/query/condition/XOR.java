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
 * Represents an XOR (exclusive OR) condition in SQL queries.
 * 
 * <p>The XOR operator returns {@code true} when exactly one of the two operands is true,
 * but not both. It's equivalent to (A AND NOT B) OR (NOT A AND B).</p>
 * 
 * <p>Note: XOR support varies by database. MySQL supports XOR natively, while
 * other databases may require the condition to be rewritten using AND/OR/NOT.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Check if either premium OR trial, but not both
 * XOR xor1 = new XOR("isPremium", true);
 * XOR xor2 = new XOR("isTrial", true);
 * // In MySQL: isPremium XOR true (used with another condition)
 * 
 * // Practical example - exactly one discount type
 * XOR studentDiscount = new XOR("hasStudentDiscount", true);
 * XOR seniorDiscount = new XOR("hasSeniorDiscount", true);
 * // Ensures customer has exactly one type of discount
 * }</pre>
 */
public class XOR extends Binary {

    // For Kryo
    XOR() {
    }

    /**
     * Constructs an XOR condition for the specified property and value.
     *
     * <p>The XOR condition is true when the property value and the provided value
     * differ in their boolean evaluation (one true, one false).</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Check if user has exactly one type of membership
     * XOR condition = new XOR("hasGoldMembership", true);
     * // Combined with another XOR for platinum membership
     * // ensures user has gold XOR platinum, but not both or neither
     * }</pre>
     *
     * @param propName the property name
     * @param propValue the value to XOR against
     */
    public XOR(final String propName, final Object propValue) {
        super(propName, Operator.XOR, propValue);
    }
}