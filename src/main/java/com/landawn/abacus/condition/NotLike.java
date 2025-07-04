/*
 * Copyright (C) 2021 HaiYang Li
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
 * Represents a NOT LIKE condition in SQL queries.
 * This condition is used to search for values that do NOT match a specified pattern.
 * 
 * <p>The NOT LIKE operator is the opposite of the LIKE operator. It uses the same wildcards:</p>
 * <ul>
 *   <li>% - Represents zero, one, or multiple characters</li>
 *   <li>_ - Represents a single character</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Exclude names starting with 'John'
 * NotLike condition1 = new NotLike("name", "John%");
 * // Results in: name NOT LIKE 'John%'
 * 
 * // Exclude emails from gmail domain
 * NotLike condition2 = new NotLike("email", "%@gmail.com");
 * // Results in: email NOT LIKE '%@gmail.com'
 * 
 * // Exclude 3-letter codes
 * NotLike condition3 = new NotLike("code", "___");
 * // Results in: code NOT LIKE '___'
 * }</pre>
 */
public class NotLike extends Binary {

    // For Kryo
    NotLike() {
    }

    /**
     * Constructs a NOT LIKE condition for the specified property and pattern.
     * 
     * @param propName the property name to check
     * @param propValue the pattern to match against (can include % and _ wildcards)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Exclude products with 'temp' in the name
     * NotLike notLike = new NotLike("productName", "%temp%");
     * 
     * // Exclude files with .tmp extension
     * NotLike notLike2 = new NotLike("filename", "%.tmp");
     * }</pre>
     */
    public NotLike(final String propName, final Object propValue) {
        super(propName, Operator.NOT_LIKE, propValue);
    }
}