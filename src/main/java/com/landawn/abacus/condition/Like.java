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
 * Represents a LIKE condition in SQL queries for pattern matching.
 * This class is used to create conditions that perform pattern matching on string values
 * using SQL LIKE syntax with wildcards.
 * 
 * <p>The LIKE operator supports two wildcards:
 * <ul>
 *   <li>% (percent sign): Matches any sequence of zero or more characters</li>
 *   <li>_ (underscore): Matches any single character</li>
 * </ul>
 * 
 * <p>Common use cases include:
 * <ul>
 *   <li>Searching for partial matches in text fields</li>
 *   <li>Finding records that start with, end with, or contain specific patterns</li>
 *   <li>Implementing flexible search functionality</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Find all names starting with "John"
 * Like condition1 = new Like("name", "John%");
 * // Generates: name LIKE 'John%'
 * 
 * // Find all emails ending with "@example.com"
 * Like condition2 = new Like("email", "%@example.com");
 * // Generates: email LIKE '%@example.com'
 * 
 * // Find all products containing "phone" anywhere in the name
 * Like condition3 = new Like("product_name", "%phone%");
 * // Generates: product_name LIKE '%phone%'
 * 
 * // Find all 5-letter words starting with 'A' and ending with 'E'
 * Like condition4 = new Like("word", "A___E");
 * // Generates: word LIKE 'A___E'
 * }</pre>
 * 
 * @see NotLike
 * @see ILike (case-insensitive LIKE in some databases)
 */
public class Like extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Like() {
    }

    /**
     * Creates a new LIKE condition with the specified property name and pattern.
     * The pattern should include SQL wildcards (% or _) for pattern matching.
     *
     * @param propName the name of the property to match against. Must not be null.
     * @param propValue the pattern to match, including wildcards. Must not be null.
     * 
     * <p>Example patterns:
     * <pre>{@code
     * // Starts with pattern
     * Like startsWith = new Like("title", "The%");
     * 
     * // Ends with pattern
     * Like endsWith = new Like("filename", "%.pdf");
     * 
     * // Contains pattern
     * Like contains = new Like("description", "%important%");
     * 
     * // Specific character positions
     * Like pattern = new Like("code", "A_B_C");  // Matches: A1B2C, AXBYC, etc.
     * 
     * // Escape special characters if needed
     * Like escaped = new Like("path", "%\\_%");  // To match literal underscore
     * }</pre>
     */
    public Like(final String propName, final Object propValue) {
        super(propName, Operator.LIKE, propValue);
    }
}