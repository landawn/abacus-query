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
 * Represents the SQL ANY operator for use with subqueries.
 * The ANY operator returns true if the comparison is true for ANY of the values returned by the subquery.
 * 
 * <p>ANY is typically used with comparison operators (=, !=, >, <, >=, <=) and a subquery.
 * The condition is satisfied if the comparison is true for at least one value from the subquery.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find products with price greater than ANY product in category 'Electronics'
 * SubQuery electronicsQuery = new SubQuery("SELECT price FROM products WHERE category = 'Electronics'");
 * Any anyPrice = new Any(electronicsQuery);
 * // Use with: price > ANY (subquery)
 * 
 * // Find employees whose salary equals ANY manager salary
 * SubQuery managerSalaries = new SubQuery("SELECT salary FROM employees WHERE is_manager = true");
 * Any anyManagerSalary = new Any(managerSalaries);
 * // Use with: salary = ANY (subquery)
 * }</pre>
 * 
 * <p>Note: ANY is equivalent to SOME in SQL. The behavior with different operators:</p>
 * <ul>
 *   <li>= ANY: true if equal to any value in the subquery (equivalent to IN)</li>
 *   <li>> ANY: true if greater than at least one value</li>
 *   <li>< ANY: true if less than at least one value</li>
 * </ul>
 * 
 * @see All
 * @see SubQuery
 * @see Cell
 */
public class Any extends Cell {

    // For Kryo
    Any() {
    }

    /**
     * Creates a new ANY condition with the specified subquery.
     * 
     * @param condition the subquery that returns values to compare against
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Create a subquery for department budgets
     * SubQuery budgetQuery = new SubQuery(
     *     "SELECT budget FROM departments WHERE region = 'West'"
     * );
     * Any anyBudget = new Any(budgetQuery);
     * 
     * // Can be used in conditions like:
     * // expense > ANY (SELECT budget FROM departments WHERE region = 'West')
     * }</pre>
     */
    public Any(final SubQuery condition) {
        super(Operator.ANY, condition);
    }
}