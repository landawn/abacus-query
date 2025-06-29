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

/**
 * Represents a SOME comparison operator used with subqueries in SQL.
 * 
 * <p>The SOME operator returns true if the comparison is true for at least one
 * value returned by the subquery. It's functionally equivalent to the ANY operator
 * in SQL.</p>
 * 
 * <p>Common usage patterns:</p>
 * <ul>
 *   <li>column = SOME (subquery) - true if column equals any value from subquery</li>
 *   <li>column > SOME (subquery) - true if column is greater than at least one value</li>
 *   <li>column < SOME (subquery) - true if column is less than at least one value</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Find employees earning more than SOME managers
 * SubQuery managerSalaries = new SubQuery("SELECT salary FROM employees WHERE role = 'manager'");
 * Some someCondition = new Some(managerSalaries);
 * // Used with: salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
 * 
 * // Find products cheaper than SOME competitor products
 * SubQuery competitorPrices = new SubQuery("SELECT price FROM competitor_products");
 * Some somePrice = new Some(competitorPrices);
 * // Used with: price < SOME (SELECT price FROM competitor_products)
 * }</pre>
 */
public class Some extends Cell {

    // For Kryo
    Some() {
    }

    /**
     * Constructs a SOME condition with the specified subquery.
     * 
     * <p>The SOME operator is typically used in combination with comparison operators
     * (=, !=, >, <, >=, <=) to compare a value against the result set of a subquery.</p>
     *
     * @param condition the subquery that returns values to compare against
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Subquery to get department budgets
     * SubQuery deptBudgets = new SubQuery("SELECT budget FROM departments");
     * Some someCondition = new Some(deptBudgets);
     * 
     * // Can be used in conditions like:
     * // project_cost < SOME (SELECT budget FROM departments)
     * // This returns projects that cost less than at least one department's budget
     * }</pre>
     */
    public Some(final SubQuery condition) {
        super(Operator.SOME, condition);
    }
}