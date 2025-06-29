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

import com.landawn.abacus.annotation.NotNull;

/**
 * Represents a condition that checks if a property value is NOT NULL.
 * This class extends {@link IsNot} to provide a specialized condition for non-null checks,
 * which is one of the most common conditions in database queries.
 * 
 * <p>This condition is essential for data validation and ensuring that required fields
 * contain values before processing or joining with other data.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if email is not null
 * IsNotNull condition = new IsNotNull("email");
 * // This would generate: email IS NOT NULL
 * 
 * // Ensure required fields are populated
 * IsNotNull nameCheck = new IsNotNull("customer_name");
 * // This would generate: customer_name IS NOT NULL
 * }</pre>
 * 
 * @see IsNull
 * @see NotNull (alias)
 */
public class IsNotNull extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNotNull() {
    }

    /**
     * Creates a new IsNotNull condition for the specified property.
     * This condition checks if the property value is not null.
     *
     * @param propName the name of the property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Ensure user has an email address
     * IsNotNull emailCheck = new IsNotNull("email");
     * // Generates: email IS NOT NULL
     * 
     * // Filter for customers with phone numbers
     * IsNotNull phoneCheck = new IsNotNull("phone_number");
     * // Generates: phone_number IS NOT NULL
     * }</pre>
     */
    public IsNotNull(final String propName) {
        super(propName, IsNull.NULL);
    }
}