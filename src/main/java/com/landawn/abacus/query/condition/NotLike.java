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
 * <p>Performance considerations:</p>
 * <ul>
 *   <li>Patterns starting with % prevent index usage (full table scan)</li>
 *   <li>Patterns starting with literal characters can use indexes more efficiently</li>
 *   <li>Consider alternative approaches for complex exclusion patterns</li>
 *   <li>Case sensitivity depends on database collation settings</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude names starting with 'John'
 * NotLike condition1 = new NotLike("name", "John%");
 * // SQL: name NOT LIKE 'John%'
 * 
 * // Exclude emails from gmail domain
 * NotLike condition2 = new NotLike("email", "%@gmail.com");
 * // SQL: email NOT LIKE '%@gmail.com'
 * 
 * // Exclude 3-letter codes
 * NotLike condition3 = new NotLike("code", "___");
 * // SQL: code NOT LIKE '___'
 * }</pre>
 * 
 * @see Binary
 * @see Like
 * @see Condition
 */
public class NotLike extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotLike instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude products with 'temp' in the name
     * NotLike notLike = new NotLike("productName", "%temp%");
     * // SQL: productName NOT LIKE '%temp%'
     *
     * // Exclude files with .tmp extension
     * NotLike notLike2 = new NotLike("filename", "%.tmp");
     * // SQL: filename NOT LIKE '%.tmp'
     *
     * // Exclude codes starting with 'TEST'
     * NotLike testExclude = new NotLike("code", "TEST%");
     * // SQL: code NOT LIKE 'TEST%'
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param propValue the pattern to match against. Can include % and _ wildcards.
     *                  Can be a String, Expression, or SubQuery.
     * @throws IllegalArgumentException if propName is null or empty
     */
    public NotLike(final String propName, final Object propValue) {
        super(propName, Operator.NOT_LIKE, propValue);
    }
}