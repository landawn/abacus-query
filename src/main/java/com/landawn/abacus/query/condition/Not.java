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
 * This class negates the result of any given condition, reversing its boolean logic.
 * It's a fundamental logical operator used to invert query conditions.
 * 
 * <p>The NOT operator is particularly useful for:
 * <ul>
 *   <li>Negating complex conditions without rewriting them</li>
 *   <li>Creating more readable queries by expressing what you don't want</li>
 *   <li>Inverting existing conditions dynamically</li>
 *   <li>Building flexible query builders that can toggle conditions</li>
 * </ul>
 * 
 * <p>Truth table:
 * <ul>
 *   <li>NOT TRUE = FALSE</li>
 *   <li>NOT FALSE = TRUE</li>
 *   <li>NOT NULL = NULL (in SQL three-valued logic)</li>
 * </ul>
 *
 * <p>Relationship to other logical operators:</p>
 * <ul>
 *   <li>NOT negates a condition (reverses its boolean value)</li>
 *   <li>AND requires all conditions to be true</li>
 *   <li>OR requires at least one condition to be true</li>
 *   <li>XOR requires exactly one of two conditions to be true</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // NOT with LIKE - find names that don't contain "test"
 * Like likeCondition = new Like("name", "%test%");
 * Not notLike = new Not(likeCondition);
 * // Results in: NOT name LIKE '%test%'
 *
 * // NOT with IN - exclude specific departments
 * In deptCondition = new In("department_id", Arrays.asList(10, 20, 30));
 * Not notInDepts = new Not(deptCondition);
 * // Results in: NOT department_id IN (10, 20, 30)
 *
 * // NOT with complex condition - find orders that are NOT (high priority AND urgent)
 * And complexCondition = new And(
 *     new Equal("priority", "HIGH"),
 *     new Equal("status", "URGENT")
 * );
 * Not notUrgentHigh = new Not(complexCondition);
 * // Results in: NOT ((priority = 'HIGH') AND (status = 'URGENT'))
 *
 * // NOT with EXISTS subquery
 * SubQuery hasOrders = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
 * Exists existsCondition = new Exists(hasOrders);
 * Not noOrders = new Not(existsCondition);
 * // Results in: NOT EXISTS SELECT 1 FROM orders WHERE orders.customer_id = customers.id
 * }</pre>
 * 
 * @see And
 * @see Or
 * @see NotIn
 * @see NotBetween
 */
public class Not extends Cell {
    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Not instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Not() {
    }

    /**
     * Creates a new NOT condition that negates the specified condition.
     * The resulting condition will be true when the input condition is false,
     * and false when the input condition is true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple negation
     * Equal isActive = new Equal("active", true);
     * Not isInactive = new Not(isActive);
     * // Results in: NOT active = true
     *
     * // Negating BETWEEN
     * Between ageRange = new Between("age", 18, 65);
     * Not outsideRange = new Not(ageRange);
     * // Results in: NOT age BETWEEN (18, 65)
     *
     * // Negating OR condition
     * Or multiStatus = new Or(
     *     new Equal("status", "PENDING"),
     *     new Equal("status", "PROCESSING")
     * );
     * Not notPendingOrProcessing = new Not(multiStatus);
     * // Results in: NOT ((status = 'PENDING') OR (status = 'PROCESSING'))
     * }</pre>
     *
     * @param cond the condition to be negated. Can be any type of condition
     *                  including simple comparisons, complex logical conditions,
     *                  or subquery conditions. Must not be null.
     */
    public Not(final Condition cond) {
        super(Operator.NOT, cond);
    }
}