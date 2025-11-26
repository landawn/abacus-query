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

/**
 * Represents a GROUP BY clause in SQL queries.
 * The GROUP BY clause groups rows that have the same values in specified columns into summary rows.
 * It's typically used with aggregate functions (COUNT, MAX, MIN, SUM, AVG) to perform calculations
 * on each group of rows. This class supports grouping with optional sort directions for each column.
 * 
 * <p>Key features of GROUP BY:
 * <ul>
 *   <li>Groups rows with identical values in specified columns</li>
 *   <li>Enables aggregate calculations on grouped data</li>
 *   <li>Can specify sort direction for each grouping column</li>
 *   <li>Often used with HAVING clause to filter groups</li>
 *   <li>Columns in SELECT must either be in GROUP BY or be aggregate functions</li>
 * </ul>
 * 
 * <p>Common use cases:
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
 * // GROUP BY with sort direction
 * GroupBy bySales = new GroupBy("sales_amount", SortDirection.DESC);
 * // SQL: GROUP BY sales_amount DESC
 *
 * // GROUP BY with mixed sort directions
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
     * @param condition the grouping condition or expression. Must not be null.
     * @throws IllegalArgumentException if condition is null (validated by parent constructor)
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
     * @param propNames the property names to group by, in order. Must not be null or empty.
     * @throws IllegalArgumentException if propNames is null, empty, or contains null/empty elements
     */
    public GroupBy(final String... propNames) {
        this(Filters.expr(OrderBy.createCondition(propNames)));
    }

    /**
     * Creates a new GROUP BY clause with a single property and sort direction.
     * This allows grouping by a single column with explicit ordering of the groups.
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
     * @param propName the property name to group by. Must not be null or empty.
     * @param direction the sort direction (ASC or DESC). Must not be null.
     * @throws IllegalArgumentException if propName is null/empty or direction is null
     */
    public GroupBy(final String propName, final SortDirection direction) {
        this(Filters.expr(OrderBy.createCondition(propName, direction)));
    }

    /**
     * Creates a new GROUP BY clause with multiple properties and a single sort direction.
     * All properties will use the same sort direction. This is useful when you want
     * consistent ordering across all grouping columns.
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
     * @param propNames the collection of property names to group by. Must not be null or empty.
     * @param direction the sort direction to apply to all properties. Must not be null.
     * @throws IllegalArgumentException if propNames is null/empty, direction is null, or propNames contains null/empty elements
     */
    public GroupBy(final Collection<String> propNames, final SortDirection direction) {
        this(Filters.expr(OrderBy.createCondition(propNames, direction)));
    }

    /**
     * Creates a new GROUP BY clause with custom sort directions for each property.
     * This provides maximum flexibility by allowing different sort directions for each
     * grouping column. The map should maintain insertion order (use LinkedHashMap) to
     * preserve the grouping order, as the order of columns in GROUP BY can affect performance
     * and results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complex grouping with mixed sort directions
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("department", SortDirection.ASC);
     * orders.put("salary_range", SortDirection.DESC);
     * orders.put("hire_year", SortDirection.ASC);
     * GroupBy complex = new GroupBy(orders);
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
     * @param orders a map of property names to their sort directions. Should be a LinkedHashMap
     *               to maintain order. Must not be null or empty.
     * @throws IllegalArgumentException if orders is null/empty, or contains null/empty keys or null values
     */
    public GroupBy(final Map<String, SortDirection> orders) {
        this(Filters.expr(OrderBy.createCondition(orders)));
    }
}