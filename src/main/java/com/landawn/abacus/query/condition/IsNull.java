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
 * which is one of the most fundamental conditions in SQL queries. NULL checking
 * is essential for proper data handling as NULL represents the absence of a value,
 * distinct from empty string, zero, or false.
 * 
 * <p>NULL represents the absence of a value and is different from empty string or zero.
 * In SQL, NULL has special properties:
 * <ul>
 *   <li>NULL is not equal to anything, including itself (NULL = NULL is unknown)</li>
 *   <li>Any arithmetic operation with NULL results in NULL</li>
 *   <li>NULL values are excluded from aggregate functions (except COUNT(*))</li>
 *   <li>NULL values require special IS NULL/IS NOT NULL operators for comparison</li>
 * </ul>
 * 
 * <p>This condition is essential for:
 * <ul>
 *   <li>Finding missing or unset data</li>
 *   <li>Data quality checks and validation</li>
 *   <li>Handling optional fields in queries</li>
 *   <li>Filtering results from outer joins</li>
 *   <li>Implementing business logic for incomplete data</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if email is null
 * IsNull emailCheck = new IsNull("email");
 * // SQL: email IS NULL
 * 
 * // Find customers without phone numbers
 * IsNull phoneCheck = new IsNull("phone_number");
 * // SQL: phone_number IS NULL
 * 
 * // Find unassigned tasks
 * IsNull assigneeCheck = new IsNull("assigned_to");
 * // SQL: assigned_to IS NULL
 * 
 * // Combine with other conditions
 * And incompleteProfile = new And(
 *     new IsNull("profile_picture"),
 *     new IsNull("bio"),
 *     new IsNotNull("user_id")
 * );
 * }</pre>
 * 
 * @see IsNotNull
 * @see Null (alias)
 * @see Is
 */
public class IsNull extends Is {

    /**
     * Shared Expression instance representing NULL.
     * This constant is used internally to represent the NULL value in SQL.
     * It's shared across all instances to reduce memory overhead and ensure
     * consistency in SQL generation.
     */
    static final Expression NULL = CF.expr("NULL");

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNull instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNull() {
    }

    /**
     * Creates a new IsNull condition for the specified property.
     * This condition checks if the property value is NULL, which indicates
     * the absence of a value in the database. This is different from empty
     * string or zero values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find records with missing data
     * IsNull birthdateCheck = new IsNull("birth_date");
     * // SQL: birth_date IS NULL
     * 
     * // Find unprocessed records
     * IsNull processedCheck = new IsNull("processed_date");
     * // SQL: processed_date IS NULL
     * 
     * // Find products without descriptions
     * IsNull descCheck = new IsNull("description");
     * // SQL: description IS NULL
     * 
     * // Find employees without managers (top-level)
     * IsNull managerCheck = new IsNull("manager_id");
     * // SQL: manager_id IS NULL
     * 
     * // Check for missing optional fields
     * IsNull middleNameCheck = new IsNull("middle_name");
     * // SQL: middle_name IS NULL
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public IsNull(final String propName) {
        super(propName, NULL);
    }
}