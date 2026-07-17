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

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.N;

/**
 * Represents a GROUP BY clause in SQL queries.
 * The GROUP BY clause groups rows that have the same values in specified columns into summary rows.
 * It's typically used with aggregate functions (COUNT, MAX, MIN, SUM, AVG) to perform calculations
 * on each group of rows. This class supports grouping with optional sort directions for each column.
 * 
 * <p>Key features of GROUP BY:</p>
 * <ul>
 *   <li>Groups rows with identical values in specified columns</li>
 *   <li>Enables aggregate calculations on grouped data</li>
 *   <li>Can specify sort direction for each grouping column</li>
 *   <li>Often used with HAVING clause to filter groups</li>
 *   <li>Selected expressions generally must be grouping expressions, aggregate expressions,
 *       or otherwise permitted by the database's functional-dependency rules</li>
 * </ul>
 *
 * <p><b>&#9888;&#65039;</b> Sort directions in {@code GROUP BY} (e.g. {@code GROUP BY col ASC/DESC})
 * are non-standard SQL and not supported by all databases. Use them only when targeting
 * a database that explicitly supports this syntax.</p>
 * 
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Calculating totals by category (sales by region, count by status)</li>
 *   <li>Finding unique combinations of column values</li>
 *   <li>Statistical analysis (average salary by department)</li>
 *   <li>Data summarization and reporting</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple GROUP BY
 * GroupBy byDept = new GroupBy("department");
 * // SQL: GROUP BY department
 *
 * // GROUP BY multiple columns
 * GroupBy byLocation = new GroupBy("department", "location");
 * // SQL: GROUP BY department, location
 *
 * // GROUP BY with sort direction (non-standard; database-specific)
 * GroupBy bySales = new GroupBy("sales_amount", SortDirection.DESC);
 * // SQL: GROUP BY sales_amount DESC
 *
 * // GROUP BY with mixed sort directions (non-standard; database-specific)
 * Map<String, SortDirection> orders = new LinkedHashMap<>();
 * orders.put("department", SortDirection.ASC);
 * orders.put("salary", SortDirection.DESC);
 * GroupBy complex = new GroupBy(orders);
 * // SQL: GROUP BY department ASC, salary DESC
 * }</pre>
 * 
 * @see Clause
 * @see SortDirection
 * @see Having
 * @see OrderBy
 * @see Filters#expr(String)
 */
public class GroupBy extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized GroupBy instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    GroupBy() {
    }

    /**
     * Creates a new GROUP BY clause with the specified condition.
     * This constructor allows for custom grouping expressions beyond simple column names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by year extracted from date
     * GroupBy byYear = new GroupBy(Filters.expr("YEAR(order_date)"));
     * // SQL: GROUP BY YEAR(order_date)
     *
     * // Group by calculated expression
     * GroupBy byRange = new GroupBy(Filters.expr("CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END"));
     * // SQL: GROUP BY CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END
     * }</pre>
     *
     * @param condition the grouping condition or expression. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code condition} is {@code null}, has a null operator, is or contains a
     *             {@link Criteria}, is a standalone {@link SubQuery} or another clause, contains an
     *             {@code ON}/{@code USING} condition or an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery
     *             operand, or is an empty predicate (a blank {@link SqlExpression} or empty {@link Junction}) — none of
     *             which can be nested inside a clause
     * @see Filters#expr(String)
     */
    public GroupBy(final Condition condition) {
        super(Operator.GROUP_BY, condition);
    }

    /**
     * Creates a new GROUP BY clause with the specified property names.
     * The columns will be grouped in the order provided. This is the most common
     * way to create GROUP BY clauses for simple column-based grouping.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by single column
     * GroupBy byStatus = new GroupBy("status");
     * // SQL: GROUP BY status
     *
     * // Group by department and location
     * GroupBy byDeptLoc = new GroupBy("department", "location");
     * // SQL: GROUP BY department, location
     *
     * // Group by multiple dimensions for analysis
     * GroupBy byDimensions = new GroupBy("region", "product_category", "year");
     * // SQL: GROUP BY region, product_category, year
     * }</pre>
     *
     * @param propNames the property names to group by, in order. Must not be {@code null} or empty and must not contain {@code null}, empty, or blank elements.
     * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null}, empty, or blank elements
     */
    public GroupBy(final String... propNames) {
        this(Filters.expr(AbstractCondition.createSortExpression(propNames)));
    }

    /**
     * Creates a new GROUP BY clause with the property names supplied as a collection.
     * The columns will be grouped in iteration order, with no explicit sort direction,
     * matching the behavior of {@link #GroupBy(String...)}.
     *
     * <p>Use an order-preserving collection (such as {@code List} or {@code LinkedHashSet})
     * to guarantee predictable generated SQL. Group-key order can affect performance but does
     * not by itself order the result rows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by department and location
     * GroupBy byDeptLoc = new GroupBy(Arrays.asList("department", "location"));
     * // SQL: GROUP BY department, location
     *
     * // Group by multiple dimensions for analysis
     * List<String> dims = Arrays.asList("region", "product_category", "year");
     * GroupBy byDimensions = new GroupBy(dims);
     * // SQL: GROUP BY region, product_category, year
     * }</pre>
     *
     * @param propNames the collection of property names to group by, in iteration order. Must not be {@code null} or empty and must not contain {@code null},
     *                  empty, or blank elements.
     * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null}, empty, or blank elements
     */
    public GroupBy(final Collection<String> propNames) {
        this(N.checkArgNotEmpty(propNames, "propNames").toArray(new String[0]));
    }

    /**
     * Creates a new GROUP BY clause with a single property and sort direction.
     * This emits a direction token after a single grouping column for dialects that support
     * that non-standard syntax. It does not by itself guarantee result-row ordering; use
     * {@link OrderBy} when ordering is required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by sales amount in descending order
     * GroupBy topSales = new GroupBy("sales_amount", SortDirection.DESC);
     * // SQL: GROUP BY sales_amount DESC
     *
     * // Group by date in ascending order
     * GroupBy byDate = new GroupBy("order_date", SortDirection.ASC);
     * // SQL: GROUP BY order_date ASC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to group by. Must not be {@code null}, empty, or blank.
     * @param direction the sort direction (ASC or DESC). Must not be {@code null}.
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     */
    public GroupBy(final String propOrColumnName, final SortDirection direction) {
        this(Filters.expr(AbstractCondition.createSortExpression(propOrColumnName, direction)));
    }

    /**
     * Creates a new GROUP BY clause with multiple properties and a single sort direction.
     * All properties will use the same direction token. This does not guarantee result-row
     * ordering; use {@link OrderBy} when ordering is required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by multiple columns, all descending
     * List<String> columns = Arrays.asList("department", "location", "year");
     * GroupBy allDesc = new GroupBy(columns, SortDirection.DESC);
     * // SQL: GROUP BY department DESC, location DESC, year DESC
     *
     * // Group by categories in ascending order (use LinkedHashSet to preserve order)
     * Set<String> categories = new LinkedHashSet<>(Arrays.asList("type", "subtype"));
     * GroupBy byCategory = new GroupBy(categories, SortDirection.ASC);
     * // SQL: GROUP BY type ASC, subtype ASC (order preserved)
     * }</pre>
     *
     * @param propNames the collection of property names to group by. Must not be {@code null} or empty and must not contain
     *                  {@code null}, empty, or blank elements.
     * @param direction the sort direction to apply to all properties. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null}, empty, or blank elements,
     *                                  or if {@code direction} is {@code null}
     */
    public GroupBy(final Collection<String> propNames, final SortDirection direction) {
        this(Filters.expr(AbstractCondition.createSortExpression(propNames, direction)));
    }

    /**
     * Creates a new GROUP BY clause with custom sort directions for each property.
     * This provides maximum flexibility by allowing different sort directions for each
     * grouping column. The map should maintain insertion order (use LinkedHashMap) to
     * preserve the generated grouping order. Group-key order can affect performance but does not
     * by itself order the result rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complex grouping with mixed sort directions
     * Map<String, SortDirection> groupings = new LinkedHashMap<>();
     * groupings.put("department", SortDirection.ASC);
     * groupings.put("salary_range", SortDirection.DESC);
     * groupings.put("hire_year", SortDirection.ASC);
     * GroupBy complex = new GroupBy(groupings);
     * // SQL: GROUP BY department ASC, salary_range DESC, hire_year ASC
     *
     * // Grouping for sales analysis
     * Map<String, SortDirection> salesGroup = new LinkedHashMap<>();
     * salesGroup.put("region", SortDirection.ASC);
     * salesGroup.put("total_sales", SortDirection.DESC);
     * GroupBy salesAnalysis = new GroupBy(salesGroup);
     * // SQL: GROUP BY region ASC, total_sales DESC
     * }</pre>
     *
     * @param groupings a map of property names to their sort directions. Should be a {@code LinkedHashMap}
     *                  to maintain order. Must not be {@code null} or empty; keys must not be {@code null}, empty, or blank and values must not be
     *                  {@code null}.
     * @throws IllegalArgumentException if {@code groupings} is {@code null}, empty, or contains {@code null}, empty, or blank keys
     *                                  or {@code null} values
     */
    public GroupBy(final Map<String, SortDirection> groupings) {
        this(Filters.expr(AbstractCondition.createSortExpression(groupings)));
    }
}
