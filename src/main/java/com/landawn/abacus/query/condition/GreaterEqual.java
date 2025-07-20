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
 * Represents a greater than or equal to (>=) condition in a query.
 * This condition checks if a property value is greater than or equal to a specified value.
 * 
 * <p>The GreaterEqual condition is commonly used for range queries, filtering records
 * where a field meets a minimum threshold. It supports comparison with various data types
 * including numbers, dates, strings (lexicographical comparison), and even subqueries.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a condition where age >= 18
 * GreaterEqual ageCondition = new GreaterEqual("age", 18);
 * 
 * // Use in a query for salary threshold
 * query.where(new GreaterEqual("salary", 50000));
 * 
 * // Date comparison
 * GreaterEqual dateCondition = new GreaterEqual("createdDate", someDate);
 * 
 * // With subquery
 * SubQuery avgSalary = CF.subQuery("SELECT AVG(salary) FROM employees");
 * GreaterEqual aboveAvg = new GreaterEqual("salary", avgSalary);
 * }</pre>
 * 
 * @see Binary
 * @see GreaterThan
 * @see LessEqual
 * @see Condition
 */
public class GreaterEqual extends Binary {

    // For Kryo
    GreaterEqual() {
    }

    /**
     * Creates a new GreaterEqual condition.
     * The condition evaluates to true when the property value is greater than or equal to the specified value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if salary is at least 50000
     * GreaterEqual salaryCondition = new GreaterEqual("salary", 50000);
     * 
     * // Check if score meets minimum requirement
     * GreaterEqual scoreCondition = new GreaterEqual("score", 60);
     * 
     * // Check if date is on or after a specific date
     * GreaterEqual dateCondition = new GreaterEqual("expiryDate", LocalDate.now());
     * }</pre>
     * 
     * @param propName the name of the property to compare
     * @param propValue the value to compare against
     * @throws IllegalArgumentException if propName is null or empty
     */
    public GreaterEqual(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_EQUAL, propValue);
    }
}