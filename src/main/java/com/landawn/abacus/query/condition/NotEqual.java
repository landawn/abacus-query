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
 * Represents a NOT EQUAL (!= or &lt;&gt;) condition in SQL queries.
 * This condition checks if a property value is not equal to a specified value.
 *
 * <p>The NOT EQUAL operator can be represented as != or &lt;&gt; in SQL, depending on the database.
 * This implementation uses the standard != operator. The condition evaluates to true when
 * the property value differs from the specified value.</p>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>NULL comparisons: In SQL, NULL != value returns NULL, not true. Use {@link IsNotNull} for null checks</li>
 *   <li>Type compatibility: Ensure the property and value types are compatible for comparison</li>
 *   <li>Case sensitivity: String comparisons may be case-sensitive depending on the database</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple not equal comparison
 * NotEqual condition1 = new NotEqual("status", "deleted");
 * // SQL: status != 'deleted'
 *
 * // Numeric comparison
 * NotEqual condition2 = new NotEqual("quantity", 0);
 * // SQL: quantity != 0
 *
 * // With null values (note: use IsNotNull for null checks)
 * NotEqual condition3 = new NotEqual("assignee", "admin");
 * // SQL: assignee != 'admin'
 *
 * // Date comparison
 * NotEqual condition4 = new NotEqual("created", "2024-01-01");
 * // SQL: created != '2024-01-01'
 * }</pre>
 * 
 * @see Binary
 * @see Equal
 * @see Condition
 */
public class NotEqual extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotEqual instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotEqual() {
    }

    /**
     * Constructs a NOT EQUAL condition for the specified property and value.
     * This condition will evaluate to true when the property value is not equal to the specified value.
     *
     * <p>The NOT EQUAL operator is commonly used to exclude specific values from query results.
     * It's particularly useful for filtering out deleted records, excluding default values,
     * or finding records that don't match a specific criteria.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude specific user
     * NotEqual notAdmin = new NotEqual("username", "admin");
     * // SQL: username != 'admin'
     *
     * // Exclude default values
     * NotEqual notDefault = new NotEqual("configuration", "default");
     * // SQL: configuration != 'default'
     *
     * // Filter out zero values
     * NotEqual notZero = new NotEqual("balance", 0);
     * // SQL: balance != 0
     * }</pre>
     *
     * <p>Note: For null comparisons, consider using {@link IsNull} or {@link IsNotNull}
     * as SQL NULL comparisons behave differently (NULL != value returns NULL, not true).</p>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public NotEqual(final String propName, final Object propValue) {
        super(propName, Operator.NOT_EQUAL, propValue);
    }
}