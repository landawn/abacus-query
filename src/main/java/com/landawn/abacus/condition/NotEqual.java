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

/**
 * Represents a NOT EQUAL (!=) condition in SQL queries.
 * This condition checks if a property value is not equal to a specified value.
 * 
 * <p>The NOT EQUAL operator can be represented as != or <> in SQL, depending on the database.
 * This implementation uses the standard != operator.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple not equal comparison
 * NotEqual condition1 = new NotEqual("status", "deleted");
 * // Results in: status != 'deleted'
 * 
 * // Numeric comparison
 * NotEqual condition2 = new NotEqual("quantity", 0);
 * // Results in: quantity != 0
 * 
 * // With null values (note: use IsNotNull for null checks)
 * NotEqual condition3 = new NotEqual("assignee", "admin");
 * // Results in: assignee != 'admin'
 * 
 * // Date comparison
 * NotEqual condition4 = new NotEqual("created", "2024-01-01");
 * // Results in: created != '2024-01-01'
 * }</pre>
 */
public class NotEqual extends Binary {

    // For Kryo
    NotEqual() {
    }

    /**
     * Constructs a NOT EQUAL condition for the specified property and value.
     * 
     * <p>Note: For null comparisons, consider using {@link IsNull} or {@link IsNotNull}
     * as SQL NULL comparisons behave differently (NULL != value returns NULL, not true).</p>
     *
     * @param propName the property name to compare
     * @param propValue the value that the property should not equal
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Exclude specific user
     * NotEqual notAdmin = new NotEqual("username", "admin");
     * 
     * // Exclude default values
     * NotEqual notDefault = new NotEqual("configuration", "default");
     * 
     * // Filter out zero values
     * NotEqual notZero = new NotEqual("balance", 0);
     * }</pre>
     */
    public NotEqual(final String propName, final Object propValue) {
        super(propName, Operator.NOT_EQUAL, propValue);
    }
}