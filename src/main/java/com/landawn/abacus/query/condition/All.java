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
 * Represents the SQL ALL operator for use with subqueries.
 * The ALL operator returns {@code true} if the comparison is true for ALL values returned by the subquery.
 *
 * <p>ALL is used with comparison operators (=, !=, &gt;, &lt;, &gt;=, &lt;=) and a subquery.
 * The condition is satisfied only if the comparison is true for every value from the subquery.
 * This provides a way to ensure a value meets a criteria compared to an entire set of values.</p>
 *
 * <p>Common usage patterns:</p>
 * <ul>
 *   <li>salary > ALL (subquery) - true if salary is greater than every value (greater than maximum)</li>
 *   <li>price &lt; ALL (subquery) - true if price is less than every value (less than minimum)</li>
 *   <li>score >= ALL (subquery) - true if score is greater than or equal to all values</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find products more expensive than ALL products in 'Electronics' category
 * SubQuery electronicsQuery = Filters.subQuery(
 *     "SELECT price FROM products WHERE category = 'Electronics'"
 * );
 * All allElectronics = new All(electronicsQuery);
 * // Use with: WHERE price > ALL (SELECT price FROM products WHERE category = 'Electronics')
 *
 * // Find employees earning more than ALL managers
 * SubQuery managerSalaries = Filters.subQuery(
 *     "SELECT salary FROM employees WHERE is_manager = true"
 * );
 * All allManagers = new All(managerSalaries);
 * // Use with: WHERE salary > ALL (SELECT salary FROM employees WHERE is_manager = true)
 * }</pre>
 *
 * <p>Behavior with different operators:</p>
 * <ul>
 *   <li>> ALL: true if greater than the maximum value in subquery</li>
 *   <li>&lt; ALL: true if less than the minimum value in subquery</li>
 *   <li>= ALL: true if equal to all values (only possible if all values are the same)</li>
 *   <li>!= ALL: true if different from all values (equivalent to NOT IN)</li>
 *   <li>>= ALL: true if greater than or equal to the maximum value</li>
 *   <li>&lt;= ALL: true if less than or equal to the minimum value</li>
 * </ul>
 *
 * <p>Relationship to ANY and SOME:</p>
 * <ul>
 *   <li>ALL requires the condition to be true for every value in the subquery</li>
 *   <li>ANY and SOME require the condition to be true for at least one value (they are synonyms)</li>
 *   <li>ALL is more restrictive than ANY/SOME</li>
 * </ul>
 *
 * @see Any
 * @see Some
 * @see SubQuery
 * @see Cell
 */
public class All extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized All instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    All() {
    }

    /**
     * Creates a new ALL condition with the specified subquery.
     * The ALL operator ensures that a comparison is true for every value returned by the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a subquery for competitor prices
     * SubQuery competitorPrices = Filters.subQuery(
     *     "SELECT price FROM competitor_products WHERE product_type = 'Premium'"
     * );
     * All allCompetitors = new All(competitorPrices);
     * // Used with: WHERE our_price > ALL (SELECT price FROM competitor_products WHERE product_type = 'Premium')
     *
     * // Find students who scored higher than all class averages
     * SubQuery classAverages = Filters.subQuery(
     *     "SELECT avg_score FROM class_statistics WHERE year = 2024"
     * );
     * All allAverages = new All(classAverages);
     * // Used with: WHERE student_score > ALL (SELECT avg_score FROM class_statistics WHERE year = 2024)
     *
     * // Find products cheaper than all premium items
     * SubQuery premiumPrices = Filters.subQuery("SELECT price FROM products WHERE category = 'premium'");
     * All allPremium = new All(premiumPrices);
     * // Used with: WHERE price < ALL (SELECT price FROM products WHERE category = 'premium')
     * }</pre>
     *
     * @param subQuery the subquery that returns values to compare against. Must not be null.
     * @throws NullPointerException if subQuery is null
     */
    public All(final SubQuery subQuery) {
        super(Operator.ALL, subQuery);
    }

    /**
     * Returns this condition as SQL text using the specified naming policy.
     *
     * @param namingPolicy the naming policy for property/column names. If {@code null}, {@link NamingPolicy#NO_CHANGE} is applied
     * @return SQL in the form {@code ALL (...)}
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Condition condition = getCondition();
        final String conditionString = condition == null ? "" : condition.toString(effectiveNamingPolicy);
        return operator().toString() + SK._SPACE + SK._PARENTHESIS_L + conditionString + SK._PARENTHESIS_R;
    }
}
