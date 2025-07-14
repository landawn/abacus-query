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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;

/**
 * Represents a logical AND condition that combines multiple conditions.
 * All conditions within an AND must evaluate to true for the AND condition to be true.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create an AND condition with multiple sub-conditions
 * And and = new And(
 *     CF.eq("status", "active"),
 *     CF.gt("age", 18),
 *     CF.lt("age", 65)
 * );
 * 
 * // Chain additional conditions
 * And newAnd = and.and(CF.eq("country", "USA"));
 * }</pre>
 * 
 * @see Junction
 * @see Or
 * @see Condition
 */
public class And extends Junction {

    // For Kryo
    And() {
    }

    /**
     * Creates a new AND condition with the specified conditions.
     * 
     * @param conditions the conditions to combine with AND logic
     * 
     * <p>Example:</p>
     * <pre>{@code
     * And and = new And(
     *     CF.eq("department", "Sales"),
     *     CF.gte("salary", 50000)
     * );
     * }</pre>
     */
    public And(final Condition... conditions) {
        super(Operator.AND, conditions);
    }

    /**
     * Creates a new AND condition with the specified collection of conditions.
     * 
     * @param conditions the collection of conditions to combine with AND logic 
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     CF.eq("status", "active"),
     *     CF.notNull("email")
     * );
     * And and = new And(conditions);
     * }</pre>
     */
    public And(final Collection<? extends Condition> conditions) {
        super(Operator.AND, conditions);
    }

    /**
     * Creates a new AND condition by adding another condition to this AND.
     * This method returns a new AND instance containing all existing conditions plus the new one.
     * 
     * @param condition the condition to add to this AND
     * @return a new AND condition containing all conditions 
     * 
     * <p>Example:</p>
     * <pre>{@code
     * And and = new And(CF.eq("status", "active"));
     * And extended = and.and(CF.gt("score", 80));
     * // extended now contains both conditions
     * }</pre>
     */
    @Override
    public And and(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        final List<Condition> condList = new ArrayList<>(conditionList.size() + 1);

        condList.addAll(conditionList);
        condList.add(condition);

        return new And(condList);
    }
}