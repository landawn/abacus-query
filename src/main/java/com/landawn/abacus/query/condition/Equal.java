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
 * Represents an equality (=) condition in a query.
 * This condition checks if a property value equals a specified value.
 * 
 * <p>The Equal condition is one of the most fundamental and commonly used conditions
 * in database queries. It performs exact matching between a column value and a
 * specified value, supporting various data types including strings, numbers, dates,
 * booleans, and even subqueries.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Exact value matching</li>
 *   <li>Null-safe comparison when comparing with null</li>
 *   <li>Support for subquery comparisons</li>
 *   <li>Case-sensitive for string comparisons (database-dependent)</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple equality check
 * Equal statusCheck = new Equal("status", "active");
 * 
 * // Numeric comparison
 * Equal idCheck = new Equal("userId", 12345);
 * 
 * // Date comparison
 * Equal dateCheck = new Equal("createdDate", LocalDate.of(2024, 1, 1));
 * 
 * // Null check (though IsNull is preferred for clarity)
 * Equal nullCheck = new Equal("deletedDate", null);
 * 
 * // Subquery comparison
 * SubQuery maxSalary = CF.subQuery("SELECT MAX(salary) FROM employees");
 * Equal maxSalaryCheck = new Equal("salary", maxSalary);
 * }</pre>
 * 
 * @see Binary
 * @see NotEqual
 * @see IsNull
 * @see In
 * @see Condition
 */
public class Equal extends Binary {

    // For Kryo
    Equal() {
    }

    /**
     * Creates a new Equal condition.
     * The condition evaluates to true when the property value exactly matches the specified value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // String equality
     * Equal nameCheck = new Equal("name", "John");
     * 
     * // Numeric equality
     * Equal countCheck = new Equal("count", 0);
     * 
     * // Boolean equality
     * Equal activeCheck = new Equal("isActive", true);
     * 
     * // Date equality
     * Equal dateCheck = new Equal("birthDate", LocalDate.of(1990, 1, 1));
     * 
     * // Subquery equality - find employees with average salary
     * SubQuery avgSalary = CF.subQuery("SELECT AVG(salary) FROM employees");
     * Equal avgCheck = new Equal("salary", avgSalary);
     * }</pre>
     * 
     * @param propName the name of the property to compare (must not be null or empty)
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public Equal(final String propName, final Object propValue) {
        super(propName, Operator.EQUAL, propValue);
    }
}