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
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a condition where age >= 18
 * GreaterEqual condition = new GreaterEqual("age", 18);
 * 
 * // Use in a query
 * query.where(new GreaterEqual("price", 100.0));
 * }</pre>
 * 
 * @see Binary
 * @see Condition
 */
public class GreaterEqual extends Binary {

    // For Kryo
    GreaterEqual() {
    }

    /**
     * Creates a new GreaterEqual condition.
     * 
     * @param propName the name of the property to compare
     * @param propValue the value to compare against
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * GreaterEqual condition = new GreaterEqual("salary", 50000);
     * }</pre>
     */
    public GreaterEqual(final String propName, final Object propValue) {
        super(propName, Operator.GREATER_EQUAL, propValue);
    }
}