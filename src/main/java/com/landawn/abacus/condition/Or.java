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

/**
 * Represents an OR logical operator that combines multiple conditions.
 * The OR condition evaluates to true if at least one of its child conditions evaluates to true.
 * 
 * <p>This class extends Junction and provides a fluent API for building complex OR conditions.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create OR with multiple conditions
 * Or or = new Or(
 *     new Equal("status", "active"),
 *     new Equal("status", "pending"),
 *     new Equal("status", "review")
 * );
 * // Results in: status = 'active' OR status = 'pending' OR status = 'review'
 * 
 * // Build OR condition fluently
 * Or or2 = new Or(new GreaterThan("age", 65))
 *     .or(new LessThan("age", 18));
 * // Results in: age > 65 OR age < 18
 * }</pre>
 */
public class Or extends Junction {

    // For Kryo
    Or() {
    }

    /**
     * Constructs an OR condition with the specified conditions.
     * 
     * @param condition variable number of conditions to be combined with OR
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Or or = new Or(
     *     new Equal("city", "New York"),
     *     new Equal("city", "Los Angeles"),
     *     new Equal("city", "Chicago")
     * );
     * }</pre>
     */
    public Or(final Condition... condition) {
        super(Operator.OR, condition);
    }

    /**
     * Constructs an OR condition with a collection of conditions.
     * 
     * @param conditions collection of conditions to be combined with OR
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     new Like("name", "John%"),
     *     new Like("name", "Jane%")
     * );
     * Or or = new Or(conditions);
     * }</pre>
     */
    public Or(final Collection<? extends Condition> conditions) {
        super(Operator.OR, conditions);
    }

    /**
     * Adds another condition to this OR clause using the OR operator.
     * Creates a new OR instance containing all existing conditions plus the new one.
     * 
     * @param condition the condition to add with OR
     * @return a new OR instance with the additional condition
     * @throws UnsupportedOperationException if the operation is not supported
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Or or = new Or(new Equal("type", "A"))
     *     .or(new Equal("type", "B"))
     *     .or(new Equal("type", "C"));
     * // Results in: type = 'A' OR type = 'B' OR type = 'C'
     * }</pre>
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        final List<Condition> condList = new ArrayList<>(conditionList.size() + 1);

        condList.addAll(conditionList);
        condList.add(condition);

        return new Or(condList);
    }
}