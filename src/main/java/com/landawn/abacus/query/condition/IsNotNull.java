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
 * Represents a condition that checks if a property value is NOT NULL.
 * This class extends {@link IsNot} to provide a specialized condition for non-null checks,
 * which is one of the most common conditions in database queries. The IS NOT NULL condition
 * is essential for data quality checks, validation, and ensuring that required fields contain values.
 * 
 * <p>In SQL, NULL represents the absence of a value, and the IS NOT NULL condition is used to:
 * <ul>
 *   <li>Filter out records with missing data</li>
 *   <li>Ensure data integrity by checking required fields</li>
 *   <li>Validate that joins will produce meaningful results</li>
 *   <li>Implement business rules that require certain fields to be populated</li>
 * </ul>
 * 
 * <p>This condition is crucial for data validation and ensuring that required fields
 * contain values before processing or joining with other data.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if email is not null
 * IsNotNull emailCheck = new IsNotNull("email");
 * // SQL: email IS NOT NULL
 * 
 * // Ensure required fields are populated
 * IsNotNull nameCheck = new IsNotNull("customer_name");
 * // SQL: customer_name IS NOT NULL
 * 
 * // Validate multiple required fields
 * And requiredFields = new And(
 *     new IsNotNull("first_name"),
 *     new IsNotNull("last_name"),
 *     new IsNotNull("email")
 * );
 * }</pre>
 * 
 * @see IsNull
 * @see IsNot
 */
public class IsNotNull extends IsNot {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized IsNotNull instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    IsNotNull() {
    }

    /**
     * Creates a new IsNotNull condition for the specified property.
     * This condition generates an "IS NOT NULL" SQL clause to check if the property value
     * is not NULL, effectively filtering for records that have values in the specified field.
     * This is essential for data validation and ensuring required fields are populated.
     *
     * <p>The generated SQL uses the IS NOT NULL operator (not != NULL) because NULL comparisons
     * have special semantics in SQL where NULL != NULL evaluates to UNKNOWN (which behaves as false
     * in WHERE clauses), but NULL IS NOT NULL evaluates to false (correctly identifying non-NULL values).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Ensure user has an email address
     * IsNotNull emailCheck = new IsNotNull("email");
     * // Generates SQL: email IS NOT NULL
     *
     * // Filter for customers with phone numbers
     * IsNotNull phoneCheck = new IsNotNull("phone_number");
     * // Generates SQL: phone_number IS NOT NULL
     *
     * // Find all orders with shipping addresses
     * IsNotNull addressCheck = new IsNotNull("shipping_address");
     * // Generates SQL: shipping_address IS NOT NULL
     *
     * // Validate that a date field is populated
     * IsNotNull dateCheck = new IsNotNull("registration_date");
     * // Generates SQL: registration_date IS NOT NULL
     *
     * // Use in query builders
     * List<User> usersWithEmail = queryExecutor
     *     .prepareQuery(User.class)
     *     .where(new IsNotNull("email"))
     *     .list();
     * }</pre>
     *
     * @param propName the name of the property/column to check (must not be null or empty)
     * @throws IllegalArgumentException if propName is null or empty (validation performed by superclass {@link Binary})
     */
    public IsNotNull(final String propName) {
        super(propName, IsNull.NULL);
    }
}