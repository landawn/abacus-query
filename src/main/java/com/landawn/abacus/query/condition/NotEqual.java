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
 * This implementation uses the {@code !=} operator (note: {@code <>} is the ISO SQL standard,
 * but {@code !=} is widely supported across databases). The condition evaluates to true when
 * the property value differs from the specified value.</p>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>Java {@code null}: passing {@code null} as the value is rewritten by {@link Binary#toString} to
 *       {@code propName IS NOT NULL}. Prefer {@link IsNotNull} for clarity</li>
 *   <li>SQL NULL semantics: {@code col != some_value} where {@code col} is NULL evaluates to UNKNOWN
 *       (treated as false in WHERE clauses), so such rows are not returned</li>
 *   <li>Type compatibility: ensure the property and value types are compatible for comparison</li>
 *   <li>Case sensitivity: string comparisons may be case-sensitive depending on the database collation</li>
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
 * // Null argument is rewritten to IS NOT NULL (prefer IsNotNull for clarity)
 * NotEqual condition3 = new NotEqual("deletedDate", null);
 * // SQL: deletedDate IS NOT NULL
 * }</pre>
 * 
 * @see Binary
 * @see Equal
 * @see IsNotNull
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
     * Creates a new NotEqual condition.
     * This condition will evaluate to true when the property value is not equal to the specified value.
     *
     * <p>The NOT EQUAL operator is commonly used to exclude specific values from query results.
     * It's particularly useful for filtering out deleted records, excluding default values,
     * or finding records that don't match a specific criterion.</p>
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
     * @param propName the property/column name (must not be {@code null} or empty)
     * @param propValue the value to compare against; may be {@code null} (renders as {@code IS NOT NULL}),
     *                  a literal value, or a {@link SubQuery}
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty
     */
    public NotEqual(final String propName, final Object propValue) {
        super(propName, Operator.NOT_EQUAL, propValue);
    }
}