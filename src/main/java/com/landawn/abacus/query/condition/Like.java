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
 * Represents a LIKE condition in SQL queries for pattern matching.
 * This class is used to create conditions that perform pattern matching on string values
 * using SQL LIKE syntax with wildcards. The LIKE operator provides flexible string matching
 * capabilities that go beyond simple equality checks, making it essential for search
 * functionality and text filtering.
 * 
 * <p>The LIKE operator supports two standard SQL wildcards:
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
 *   <li>Pattern validation (e.g., email format, phone number patterns)</li>
 *   <li>Autocomplete and type-ahead features</li>
 * </ul>
 * 
 * <p>Performance considerations:
 * <ul>
 *   <li>Patterns starting with % prevent index usage (full table scan)</li>
 *   <li>Patterns starting with literal characters can use indexes efficiently</li>
 *   <li>Consider full-text search for complex text searching needs</li>
 *   <li>Case sensitivity depends on database collation settings</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find all names starting with "John"
 * Like startsWithJohn = new Like("name", "John%");
 * // SQL: name LIKE 'John%'
 * // Matches: "John", "Johnson", "Johnny", etc.
 * 
 * // Find all emails ending with "@example.com"
 * Like exampleEmails = new Like("email", "%@example.com");
 * // Generates: email LIKE '%@example.com'
 * // Matches: "user@example.com", "admin@example.com", etc.
 * 
 * // Find all products containing "phone" anywhere in the name
 * Like phoneProducts = new Like("product_name", "%phone%");
 * // Generates: product_name LIKE '%phone%'
 * // Matches: "iPhone", "Smartphone", "Headphones", etc.
 * 
 * // Find all 5-letter words starting with 'A' and ending with 'E'
 * Like pattern = new Like("word", "A___E");
 * // Generates: word LIKE 'A___E'
 * // Matches: "APPLE", "ANKLE", "ANGLE", etc.
 * 
 * // Complex pattern matching
 * Like complexPattern = new Like("code", "PRD-20__-___");
 * // Generates: code LIKE 'PRD-20__-___'
 * // Matches: "PRD-2023-001", "PRD-2024-ABC", etc.
 * }</pre>
 * 
 * @see Binary
 * @see NotLike
 * @see Condition
 */
public class Like extends Binary {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Like instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Like() {
    }

    /**
     * Creates a new LIKE condition with the specified property name and pattern.
     * The pattern should include SQL wildcards (% or _) for pattern matching.
     * If special characters need to be matched literally, they should be escaped
     * according to your database's escape syntax.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Starts with pattern - uses index efficiently
     * Like startsWith = new Like("title", "The%");
     * // Matches: "The Great Gatsby", "The Lord of the Rings", "The", etc.
     * 
     * // Ends with pattern - may require full table scan
     * Like endsWith = new Like("filename", "%.pdf");
     * // Matches: "report.pdf", "invoice_2023.pdf", ".pdf", etc.
     * 
     * // Contains pattern - requires full table scan
     * Like contains = new Like("description", "%important%");
     * // Matches: "This is important", "unimportant details", "important", etc.
     * 
     * // Specific character positions using underscore
     * Like pattern = new Like("code", "A_B_C");
     * // Matches: "A1B2C", "AXBYC", "A-B-C", etc.
     * 
     * // Mixed wildcards for complex patterns
     * Like mixed = new Like("serial", "SN-%_____");
     * // Matches: "SN-12345", "SN-ABCDE", etc.
     * 
     * // Email domain pattern
     * Like emailDomain = new Like("email", "%@%.com");
     * // Matches any .com email address
     * 
     * // Phone number pattern (specific format)
     * Like phonePattern = new Like("phone", "(___) ___-____");
     * // Matches: "(555) 123-4567" format
     * 
     * // Escape special characters if needed (syntax varies by database)
     * Like escaped = new Like("path", "%\\_%"); // To match literal underscore
     * // Check your database documentation for escape syntax
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param propValue the pattern to match, including wildcards. Must not be null.
     *                  Use % for any characters, _ for single character.
     * @throws IllegalArgumentException if propName is null or empty
     */
    public Like(final String propName, final Object propValue) {
        super(propName, Operator.LIKE, propValue);
    }
}