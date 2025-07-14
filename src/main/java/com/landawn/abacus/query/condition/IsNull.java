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

import org.apache.commons.lang3.ObjectUtils.Null;

import com.landawn.abacus.query.condition.ConditionFactory.CF;

/**
 * Represents a condition that checks if a property value is NULL.
 * This class extends {@link Is} to provide a specialized condition for NULL checks,
 * which is one of the most fundamental conditions in SQL queries.
 * 
 * <p>NULL represents the absence of a value and is different from empty string or zero.
 * This condition is essential for:
 * <ul>
 *   <li>Finding missing or unset data</li>
 *   <li>Data quality checks</li>
 *   <li>Handling optional fields</li>
 *   <li>Outer join result filtering</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if email is null
 * IsNull condition = new IsNull("email");
 * // This would generate: email IS NULL
 * 
 * // Find customers without phone numbers
 * IsNull phoneCheck = new IsNull("phone_number");
 * // This would generate: phone_number IS NULL
 * 
 * // Find unassigned tasks
 * IsNull assigneeCheck = new IsNull("assigned_to");
 * // This would generate: assigned_to IS NULL
 * }</pre>
 * 
 * @see IsNotNull
 * @see Null (alias)
 */
public class IsNull extends Is {

    /**
     * Shared Expression instance representing NULL.
     * This constant is used internally to represent the NULL value in SQL.
     */
    static final Expression NULL = CF.expr("NULL");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    IsNull() {
    }

    /**
     * Creates a new IsNull condition for the specified property.
     * This condition checks if the property value is NULL.
     *
     * @param propName the name of the property to check. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Find records with missing data
     * IsNull birthdateCheck = new IsNull("birth_date");
     * // Generates: birth_date IS NULL
     * 
     * // Find unprocessed records
     * IsNull processedCheck = new IsNull("processed_date");
     * // Generates: processed_date IS NULL
     * 
     * // Find products without descriptions
     * IsNull descCheck = new IsNull("description");
     * // Generates: description IS NULL
     * }</pre>
     */
    public IsNull(final String propName) {
        super(propName, NULL);
    }
}