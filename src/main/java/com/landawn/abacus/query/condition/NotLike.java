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

package com.landawn.abacus.query.condition;

/**
 * Represents a NOT LIKE condition in SQL queries.
 * This condition is used to search for values that do NOT match a specified pattern.
 * 
 * <p>The NOT LIKE operator is the opposite of the LIKE operator. It returns {@code true} when
 * the value does not match the specified pattern. This is particularly useful for
 * excluding records based on pattern matching.</p>
 * 
 * <p>Pattern matching wildcards:</p>
 * <ul>
 *   <li>% - Represents zero, one, or multiple characters</li>
 *   <li>_ - Represents a single character</li>
 *   <li>Use escape characters for literal % or _ (database-specific)</li>
 * </ul>
 * 
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Exclude records with specific prefixes or suffixes</li>
 *   <li>Filter out temporary or test data</li>
 *   <li>Remove records matching unwanted patterns</li>
 *   <li>Exclude specific file types or formats</li>
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
 * 
 * @see Like
 */
public class NotLike extends Binary {

    // For Kryo
    NotLike() {
    }

    /**
     * Constructs a NOT LIKE condition for the specified property and pattern.
     * The condition evaluates to true when the property value does not match the given pattern.
     * 
     * <p>The pattern can include SQL wildcards (% and _) for flexible matching.
     * This operator is case-sensitive in most databases, though behavior may vary
     * based on database configuration and collation settings.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Exclude products with 'temp' in the name
     * NotLike notLike = new NotLike("productName", "%temp%");
     * 
     * // Exclude files with .tmp extension
     * NotLike notLike2 = new NotLike("filename", "%.tmp");
     * }</pre>
     * 
     * @param propName the property name to check
     * @param propValue the pattern to match against (can include % and _ wildcards)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public NotLike(final String propName, final Object propValue) {
        super(propName, Operator.NOT_LIKE, propValue);
    }
}