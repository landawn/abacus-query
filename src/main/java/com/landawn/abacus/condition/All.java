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
 * Represents the SQL ALL operator for use with subqueries.
 * The ALL operator returns true if the comparison is true for ALL values returned by the subquery.
 * 
 * <p>ALL is used with comparison operators (=, !=, >, <, >=, <=) and a subquery.
 * The condition is satisfied only if the comparison is true for every value from the subquery.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Find products more expensive than ALL products in 'Electronics' category
 * SubQuery electronicsQuery = new SubQuery(
 *     "SELECT price FROM products WHERE category = 'Electronics'"
 * );
 * All allElectronics = new All(electronicsQuery);
 * // Use with: price > ALL (subquery)
 * 
 * // Find employees earning more than ALL managers
 * SubQuery managerSalaries = new SubQuery(
 *     "SELECT salary FROM employees WHERE is_manager = true"
 * );
 * All allManagers = new All(managerSalaries);
 * // Use with: salary > ALL (subquery)
 * }</pre>
 * 
 * <p>Behavior with different operators:</p>
 * <ul>
 *   <li>> ALL: true if greater than the maximum value in subquery</li>
 *   <li>< ALL: true if less than the minimum value in subquery</li>
 *   <li>= ALL: true if equal to all values (only possible if all values are the same)</li>
 *   <li>!= ALL: true if different from all values (equivalent to NOT IN)</li>
 * </ul>
 * 
 * @see Any
 * @see SubQuery
 * @see Cell
 */
public class All extends Cell {

    // For Kryo
    All() {
    }

    /**
     * Creates a new ALL condition with the specified subquery.
     * 
     * @param condition the subquery that returns values to compare against
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Create a subquery for competitor prices
     * SubQuery competitorPrices = new SubQuery(
     *     "SELECT price FROM competitor_products WHERE product_type = 'Premium'"
     * );
     * All allCompetitors = new All(competitorPrices);
     * 
     * // Can be used to find products priced above all competitors:
     * // our_price > ALL (SELECT price FROM competitor_products WHERE product_type = 'Premium')
     * }</pre>
     */
    public All(final SubQuery condition) {
        super(Operator.ALL, condition);
    }
}