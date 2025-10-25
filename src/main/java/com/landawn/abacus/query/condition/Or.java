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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;

/**
 * Represents an OR logical operator that combines multiple conditions.
 * The OR condition evaluates to true if at least one of its child conditions evaluates to true.
 * 
 * <p>This class extends Junction and provides a fluent API for building complex OR conditions.
 * The OR operator follows standard SQL logical evaluation rules where the entire expression
 * is true if any single condition is true. Evaluation typically short-circuits when a true
 * condition is found.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns true if ANY child condition is true</li>
 *   <li>Returns false only if ALL child conditions are false</li>
 *   <li>Supports unlimited number of child conditions</li>
 *   <li>Can be nested with other logical operators (AND, NOT)</li>
 *   <li>Evaluation may short-circuit for performance</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create OR with multiple conditions
 * Or or = new Or(
 *     new Equal("status", "active"),
 *     new Equal("status", "pending"),
 *     new Equal("status", "review")
 * );
 * // Results in: ((status = 'active') OR (status = 'pending') OR (status = 'review'))
 *
 * // Build OR condition fluently
 * Or or2 = new Or(new GreaterThan("age", 65))
 *     .or(new LessThan("age", 18));
 * // Results in: ((age > 65) OR (age < 18))
 * }</pre>
 * 
 * @see And
 * @see Junction
 */
public class Or extends Junction {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Or instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Or() {
    }

    /**
     * Creates a new OR condition with the specified conditions.
     * All provided conditions will be combined using the OR operator.
     *
     * <p>The conditions are evaluated left to right, and the first true condition
     * will make the entire OR expression true. This constructor accepts a variable
     * number of conditions for convenience.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find users in specific cities
     * Or or = new Or(
     *     new Equal("city", "New York"),
     *     new Equal("city", "Los Angeles"),
     *     new Equal("city", "Chicago")
     * );
     * 
     * // Complex OR with different condition types
     * Or complexOr = new Or(
     *     new Like("email", "%@gmail.com"),
     *     new Like("email", "%@yahoo.com"),
     *     new IsNull("email")
     * );
     * }</pre>
     * 
     * @param conditions the variable number of conditions to be combined with OR
     * @throws IllegalArgumentException if conditions is null
     */
    public Or(final Condition... conditions) {
        super(Operator.OR, conditions);
    }

    /**
     * Creates a new OR condition with a collection of conditions.
     * This constructor is useful when conditions are built dynamically.
     *
     * <p>All conditions in the collection will be combined using the OR operator.
     * The collection is copied internally to ensure immutability of the condition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Dynamic condition building
     * List<Condition> conditions = new ArrayList<>();
     * for (String name : searchNames) {
     *     conditions.add(new Like("name", "%" + name + "%"));
     * }
     * Or or = new Or(conditions);
     *
     * // Combining existing conditions
     * Set<Condition> statusConditions = new HashSet<>();
     * statusConditions.add(new Equal("status", "active"));
     * statusConditions.add(new Equal("status", "pending"));
     * Or statusOr = new Or(statusConditions);
     * }</pre>
     *
     * @param conditions the collection of conditions to be combined with OR
     * @throws IllegalArgumentException if conditions is null
     */
    public Or(final Collection<? extends Condition> conditions) {
        super(Operator.OR, conditions);
    }

    /**
     * Adds another condition to this OR clause using the OR operator.
     * Creates a new OR instance containing all existing conditions plus the new one.
     * 
     * <p>This method provides a fluent interface for building OR conditions incrementally.
     * Each call returns a new OR instance, preserving immutability. The new condition
     * is added to the end of the existing conditions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Build condition step by step
     * Or or = new Or(new Equal("type", "A"))
     *     .or(new Equal("type", "B"))
     *     .or(new Equal("type", "C"));
     * // Results in: type = 'A' OR type = 'B' OR type = 'C'
     * 
     * // Add conditions conditionally
     * Or baseOr = new Or(new Equal("status", "active"));
     * if (includeInactive) {
     *     baseOr = baseOr.or(new Equal("status", "inactive"));
     * }
     * if (includePending) {
     *     baseOr = baseOr.or(new Equal("status", "pending"));
     * }
     * }</pre>
     * 
     * @param condition the condition to add with OR
     * @return a new OR instance with the additional condition
     * @throws IllegalArgumentException if condition is null
     */
    @Override
    public Or or(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        final List<Condition> condList = new ArrayList<>(conditionList.size() + 1);

        condList.addAll(conditionList);
        condList.add(condition);

        return new Or(condList);
    }
}