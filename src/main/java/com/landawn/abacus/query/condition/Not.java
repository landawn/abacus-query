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
 * Represents a NOT logical operator in SQL conditions.
 * This class negates the result of a given condition.
 * 
 * <p>The NOT operator reverses the logical state of its operand. If a condition is TRUE,
 * then NOT condition will be FALSE, and vice versa.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // NOT with LIKE condition
 * Like likeCondition = new Like("name", "%test%");
 * Not notCondition = new Not(likeCondition);
 * // Results in: NOT (name LIKE '%test%')
 * 
 * // NOT with IN condition
 * In inCondition = new In("status", Arrays.asList("active", "pending"));
 * Not notIn = new Not(inCondition);
 * // Results in: NOT (status IN ('active', 'pending'))
 * }</pre>
 */
public class Not extends Cell {
    // For Kryo
    Not() {
    }

    /**
     * Constructs a NOT condition that negates the specified condition.
     *
     * @param condition the condition to be negated
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between between = new Between("age", 18, 65);
     * Not notBetween = new Not(between);
     * // Results in: NOT (age BETWEEN 18 AND 65)
     * }</pre>
     */
    public Not(final Condition condition) {
        super(Operator.NOT, condition);
    }
}