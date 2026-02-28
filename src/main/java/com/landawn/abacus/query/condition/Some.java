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

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Represents the SQL SOME operator for use with subqueries.
 * The SOME operator returns {@code true} if the comparison is true for at least one
 * value returned by the subquery. It's functionally equivalent to the ANY operator
 * in SQL. The operator is used with a comparison operator (=, !=, &gt;, &lt;, &gt;=, &lt;=)
 * to compare a value against a set of values from a subquery.
 *
 * <p>Comparison behavior:</p>
 * <ul>
 *   <li>column = SOME (subquery) - true if column equals any value from subquery</li>
 *   <li>column &gt; SOME (subquery) - true if column is greater than at least one value</li>
 *   <li>column &lt; SOME (subquery) - true if column is less than at least one value</li>
 *   <li>column != SOME (subquery) - true if column differs from at least one value</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find employees earning more than SOME managers
 * SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
 * Some someCondition = new Some(managerSalaries);
 * // Used with: salary > SOME (SELECT salary FROM employees WHERE role = 'manager')
 *
 * // Find products cheaper than SOME competitor products
 * SubQuery competitorPrices = Filters.subQuery("SELECT price FROM competitor_products");
 * Some somePrice = new Some(competitorPrices);
 * // Used with: price < SOME (SELECT price FROM competitor_products)
 * }</pre>
 *
 * <p>Relationship to ANY and ALL:</p>
 * <ul>
 *   <li>SOME and ANY are functionally equivalent - both return true if condition matches at least one value</li>
 *   <li>ALL requires the condition to be true for all values (more restrictive)</li>
 *   <li>SOME/ANY are less restrictive than ALL</li>
 * </ul>
 *
 * @see Any
 * @see All
 * @see SubQuery
 * @see Cell
 */
public class Some extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Some instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Some() {
    }

    /**
     * Creates a new SOME condition with the specified subquery.
     * The SOME operator must be used with a comparison operator in the containing condition.
     *
     * <p>The SOME operator is typically used in scenarios where you want to find records
     * that meet a criteria compared to at least one value from another dataset. It's
     * particularly useful for finding records that exceed minimum thresholds or fall
     * below maximum limits from a dynamic set of values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Subquery to get department budgets
     * SubQuery deptBudgets = Filters.subQuery("SELECT budget FROM departments");
     * Some someCondition = new Some(deptBudgets);
     * // Used with: WHERE project_cost < SOME (SELECT budget FROM departments)
     * // Returns projects that cost less than at least one department's budget
     *
     * // Find employees earning more than some manager salaries
     * SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
     * Some someManagerSalary = new Some(managerSalaries);
     * // Used with: WHERE salary > SOME (SELECT salary FROM employees WHERE is_manager = true)
     * // Returns employees earning more than at least one manager
     *
     * // Find orders with amount equal to some threshold
     * SubQuery thresholds = Filters.subQuery("SELECT threshold FROM order_levels");
     * Some someThreshold = new Some(thresholds);
     * // Used with: WHERE order_amount = SOME (SELECT threshold FROM order_levels)
     * // Returns orders matching at least one threshold value
     * }</pre>
     *
     * @param subQuery the subquery that returns values to compare against. Must not be null.
     * @throws NullPointerException if subQuery is null
     */
    public Some(final SubQuery subQuery) {
        super(Operator.SOME, subQuery);
    }

    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Condition condition = getCondition();
        final String conditionString = condition == null ? "" : condition.toString(effectiveNamingPolicy);
        return getOperator().toString() + SK._SPACE + SK._PARENTHESES_L + conditionString + SK._PARENTHESES_R;
    }
}
