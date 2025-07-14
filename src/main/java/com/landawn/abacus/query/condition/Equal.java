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
 * <p>This is one of the most commonly used conditions in queries.
 * It supports comparison with literal values, null values, and subqueries.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple equality check
 * Equal condition = new Equal("status", "active");
 * 
 * // Use in a query
 * query.where(new Equal("userId", 12345));
 * 
 * // Compare with null (though IsNull is preferred)
 * Equal nullCheck = new Equal("deletedDate", null);
 * 
 * // Compare with subquery result
 * SubQuery maxSalary = new SubQuery("SELECT MAX(salary) FROM employees");
 * Equal maxSalaryCheck = new Equal("salary", maxSalary);
 * }</pre>
 * 
 * @see Binary
 * @see NotEqual
 * @see IsNull
 * @see Condition
 */
public class Equal extends Binary {

    // For Kryo
    Equal() {
    }

    /**
     * Creates a new Equal condition.
     * 
     * @param propName the name of the property to compare
     * @param propValue the value to compare against (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if name equals "John"
     * Equal nameCheck = new Equal("name", "John");
     * 
     * // Check if count equals 0
     * Equal countCheck = new Equal("count", 0);
     * 
     * // Check if date equals specific date
     * Equal dateCheck = new Equal("createdDate", someDate);
     * }</pre>
     */
    public Equal(final String propName, final Object propValue) {
        super(propName, Operator.EQUAL, propValue);
    }
}