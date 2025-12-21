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
 * Represents a logical AND condition that combines multiple conditions.
 * All conditions within an AND must evaluate to true for the AND condition to be true.
 * 
 * <p>The AND condition is one of the fundamental logical operations in query building,
 * allowing you to combine multiple criteria where all must be satisfied. It follows
 * standard boolean algebra rules where the result is true only when all operands are true.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Short-circuit evaluation in most databases</li>
 *   <li>Can combine any types of conditions</li>
 *   <li>Supports chaining for readability</li>
 *   <li>Maintains order of conditions for predictable SQL generation</li>
 * </ul>
 *
 * <p>Relationship to other logical operators:</p>
 * <ul>
 *   <li>AND requires all conditions to be true</li>
 *   <li>OR requires at least one condition to be true</li>
 *   <li>NOT negates a condition</li>
 *   <li>XOR requires exactly one of two conditions to be true</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an AND condition with multiple sub-conditions
 * And and = new And(
 *     Filters.eq("status", "active"),
 *     Filters.gt("age", 18),
 *     Filters.lt("age", 65)
 * );
 * // Results in: ((status = 'active') AND (age > 18) AND (age < 65))
 *
 * // Chain additional conditions
 * And extended = and.and(Filters.eq("country", "USA"));
 * // Results in: ((status = 'active') AND (age > 18) AND (age < 65) AND (country = 'USA'))
 *
 * // Create from a collection
 * List<Condition> conditions = Arrays.asList(
 *     Filters.isNotNull("email"),
 *     Filters.eq("verified", true)
 * );
 * And fromList = new And(conditions);
 * }</pre>
 * 
 * @see Junction
 * @see Or
 * @see Not
 * @see Condition
 */
public class And extends Junction {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized And instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    And() {
    }

    /**
     * Creates a new AND condition with the specified conditions.
     * All provided conditions must be true for this AND condition to evaluate to true.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple AND with two conditions
     * And and = new And(
     *     Filters.eq("department", "Sales"),
     *     Filters.ge("salary", 50000)
     * );
     * // Results in: ((department = 'Sales') AND (salary >= 50000))
     *
     * // Complex AND with multiple conditions
     * And complex = new And(
     *     Filters.eq("status", "active"),
     *     Filters.between("age", 25, 65),
     *     Filters.in("role", Arrays.asList("Manager", "Director")),
     *     Filters.isNotNull("email")
     * );
     * }</pre>
     *
     * @param conditions the conditions to combine with AND logic
     * @throws IllegalArgumentException if conditions array contains null elements
     */
    public And(final Condition... conditions) {
        super(Operator.AND, conditions);
    }

    /**
     * Creates a new AND condition with the specified collection of conditions.
     * All conditions in the collection must be true for this AND condition to evaluate to true.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Build conditions dynamically
     * List<Condition> conditions = new ArrayList<>();
     * conditions.add(Filters.eq("status", "active"));
     * conditions.add(Filters.isNotNull("email"));
     * if (includeAgeFilter) {
     *     conditions.add(Filters.gt("age", 21));
     * }
     *
     * And and = new And(conditions);
     * // Results in dynamic AND condition based on the list
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with AND logic
     * @throws IllegalArgumentException if conditions contains null elements
     */
    public And(final Collection<? extends Condition> conditions) {
        super(Operator.AND, conditions);
    }

    /**
     * Creates a new AND condition by adding another condition to this AND.
     * This method returns a new AND instance containing all existing conditions plus the new one.
     * The original AND condition remains unchanged (immutable).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Start with a basic AND
     * And and = new And(Filters.eq("status", "active"));
     *
     * // Add more conditions through chaining
     * And extended = and
     *     .and(Filters.gt("score", 80))
     *     .and(Filters.lt("attempts", 3))
     *     .and(Filters.eq("verified", true));
     * // Results in: ((status = 'active') AND (score > 80) AND (attempts < 3) AND (verified = true))
     *
     * // Original 'and' is unchanged
     * // extended is a new instance with all conditions
     * }</pre>
     *
     * @param condition the condition to add to this AND. Must not be null.
     * @return a new AND condition containing all existing conditions plus the new one
     * @throws IllegalArgumentException if condition is null
     */
    @Override
    public And and(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        final List<Condition> conditions = new ArrayList<>(conditionList.size() + 1);

        conditions.addAll(conditionList);
        conditions.add(condition);

        return new And(conditions);
    }
}
