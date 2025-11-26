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
 * Represents a HAVING clause in SQL queries.
 * The HAVING clause is used to filter grouped results based on conditions applied to aggregated data.
 * It is typically used with GROUP BY clauses to filter groups based on aggregate functions.
 * Unlike WHERE, which filters individual rows before grouping, HAVING filters entire groups after
 * aggregation has been performed.
 *
 * <p>Key differences between WHERE and HAVING:
 * <ul>
 *   <li>WHERE filters rows before grouping; HAVING filters groups after aggregation</li>
 *   <li>WHERE cannot use aggregate functions; HAVING is designed for aggregate function conditions</li>
 *   <li>WHERE is processed earlier in query execution; HAVING is processed after GROUP BY</li>
 *   <li>WHERE is more efficient for filtering individual rows; HAVING for filtering aggregated results</li>
 * </ul>
 *
 * <p>Common use cases for HAVING:
 * <ul>
 *   <li>Finding groups with counts above/below a threshold</li>
 *   <li>Filtering based on sum, average, min, or max values</li>
 *   <li>Identifying outlier groups in statistical analysis</li>
 *   <li>Enforcing business rules on aggregated data</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Find departments with more than 5 employees
 * Having moreThan5 = new Having(Filters.gt("COUNT(*)", 5));
 * // SQL: HAVING COUNT(*) > 5
 *
 * // Find categories with average price above 100
 * Having avgPriceHigh = new Having(Filters.gt("AVG(price)", 100));
 * // SQL: HAVING AVG(price) > 100
 *
 * // Find customers with total orders over 10000
 * Having bigSpenders = new Having(Filters.gt("SUM(order_total)", 10000));
 * // SQL: HAVING SUM(order_total) > 10000
 *
 * // Complex HAVING with multiple conditions
 * Having complex = new Having(
 *     Filters.and(
 *         Filters.gt("COUNT(*)", 10),
 *         Filters.lt("AVG(age)", 40),
 *         Filters.ge("SUM(revenue)", 50000)
 *     )
 * );
 * // SQL: HAVING COUNT(*) > 10 AND AVG(age) < 40 AND SUM(revenue) >= 50000
 *
 * // Complete query example
 * query.select("department", "COUNT(*) as emp_count", "AVG(salary) as avg_salary")
 *      .from("employees")
 *      .groupBy("department")
 *      .having(new Having(Filters.and(
 *          Filters.gt("COUNT(*)", 5),
 *          Filters.gt("AVG(salary)", 50000)
 *      )));
 * // SQL: SELECT department, COUNT(*) as emp_count, AVG(salary) as avg_salary
 * //      FROM employees
 * //      GROUP BY department
 * //      HAVING COUNT(*) > 5 AND AVG(salary) > 50000
 * }</pre>
 *
 * @see Clause
 * @see Condition
 * @see GroupBy
 * @see Where
 */
public class Having extends Clause {
    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Having instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Having() {
    }

    /**
     * Creates a new HAVING clause with the specified condition.
     * The condition is applied to the aggregated groups after GROUP BY processing.
     * This allows filtering based on aggregate function results such as COUNT, SUM,
     * AVG, MIN, and MAX.
     *
     * <p>The condition typically involves aggregate functions and comparison operators.
     * Multiple conditions can be combined using And/Or to create complex filtering logic.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter groups where the sum of sales exceeds 10000
     * Having highSales = new Having(Filters.gt("SUM(sales)", 10000));
     * // SQL: HAVING SUM(sales) > 10000
     *
     * // Filter groups with at least 3 members
     * Having minCount = new Having(Filters.ge("COUNT(*)", 3));
     * // SQL: HAVING COUNT(*) >= 3
     *
     * // Filter groups with average value in range
     * Having avgRange = new Having(
     *     Filters.and(
     *         Filters.ge("AVG(score)", 60),
     *         Filters.le("AVG(score)", 90)
     *     )
     * );
     * // SQL: HAVING AVG(score) >= 60 AND AVG(score) <= 90
     *
     * // Filter groups with maximum value check
     * Having maxCheck = new Having(Filters.lt("MAX(temperature)", 100));
     * // SQL: HAVING MAX(temperature) < 100
     *
     * // Multiple aggregate conditions
     * Having multipleAggs = new Having(
     *     Filters.and(
     *         Filters.gt("COUNT(DISTINCT customer_id)", 10),
     *         Filters.ge("SUM(amount)", 5000),
     *         Filters.between("AVG(rating)", 3.0, 5.0)
     *     )
     * );
     * // SQL: HAVING COUNT(DISTINCT customer_id) > 10
     * //      AND SUM(amount) >= 5000
     * //      AND AVG(rating) BETWEEN 3.0 AND 5.0
     * }</pre>
     *
     * @param condition the condition to apply in the HAVING clause. Must not be null.
     *                  Typically contains aggregate function expressions that operate on
     *                  grouped data. Can use comparison operators (gt, lt, eq, etc.) and
     *                  logical operators (and, or, not) to build complex filtering logic.
     * @throws IllegalArgumentException if condition is null (validated by parent constructor)
     */
    public Having(final Condition condition) {
        super(Operator.HAVING, condition);
    }
}