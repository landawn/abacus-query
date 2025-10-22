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
 * Represents the SQL ANY operator for use with subqueries.
 * The ANY operator returns {@code true} if the comparison is true for ANY of the values returned by the subquery.
 * 
 * <p>ANY is typically used with comparison operators (=, !=, >, <, >=, <=) and a subquery.
 * The condition is satisfied if the comparison is true for at least one value from the subquery.
 * This provides a powerful way to compare a value against a set of values returned by a subquery.</p>
 * 
 * <p>Common usage patterns:</p>
 * <ul>
 *   <li>salary > ANY (subquery) - true if salary is greater than at least one value from subquery</li>
 *   <li>price <= ANY (subquery) - true if price is less than or equal to at least one value</li>
 *   <li>id = ANY (subquery) - equivalent to id IN (subquery)</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find products with price greater than ANY product in category 'Electronics'
 * SubQuery electronicsQuery = CF.subQuery(
 *     "SELECT price FROM products WHERE category = 'Electronics'"
 * );
 * Any anyPrice = new Any(electronicsQuery);
 * // Use with: WHERE price > ANY (SELECT price FROM products WHERE category = 'Electronics')
 * 
 * // Find employees whose salary equals ANY manager salary
 * SubQuery managerSalaries = CF.subQuery(
 *     "SELECT salary FROM employees WHERE is_manager = true"
 * );
 * Any anyManagerSalary = new Any(managerSalaries);
 * // Use with: WHERE salary = ANY (SELECT salary FROM employees WHERE is_manager = true)
 * }</pre>
 * 
 * <p>Note: ANY is equivalent to SOME in SQL. The behavior with different operators:</p>
 * <ul>
 *   <li>= ANY: true if equal to any value in the subquery (equivalent to IN)</li>
 *   <li>> ANY: true if greater than at least one value (greater than the minimum)</li>
 *   <li>< ANY: true if less than at least one value (less than the maximum)</li>
 *   <li>!= ANY: true if different from at least one value</li>
 * </ul>
 * 
 * @see All
 * @see Some
 * @see SubQuery
 * @see Cell
 */
public class Any extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Any instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Any() {
    }

    /**
     * Creates a new ANY condition with the specified subquery.
     * The ANY operator is used in conjunction with comparison operators to test
     * if the comparison is true for any value returned by the subquery.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Create a subquery for department budgets
     * SubQuery budgetQuery = CF.subQuery(
     *     "SELECT budget FROM departments WHERE region = 'West'"
     * );
     * Any anyBudget = new Any(budgetQuery);
     *
     * // Can be used in conditions like:
     * // expense > ANY (SELECT budget FROM departments WHERE region = 'West')
     * // This finds expenses greater than at least one department budget in West region
     *
     * // Another example: find students with score higher than any passing score
     * SubQuery passingScores = CF.subQuery(
     *     "SELECT passing_score FROM exams WHERE subject = 'Math'"
     * );
     * Any anyPassingScore = new Any(passingScores);
     * // Use with: WHERE student_score > ANY (...)
     * }</pre>
     *
     * @param condition the subquery that returns values to compare against. Must not be null.
     * @throws IllegalArgumentException if condition is null
     */
    public Any(final SubQuery condition) {
        super(Operator.ANY, condition);
    }
}