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

import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.ConditionFactory.CF;

/**
 * Represents a GROUP BY clause in SQL queries.
 * The GROUP BY clause groups rows that have the same values in specified columns into summary rows.
 * It can be used with or without a sort direction for each column.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple GROUP BY
 * GroupBy groupBy = new GroupBy("department");
 * 
 * // GROUP BY multiple columns
 * GroupBy groupBy = new GroupBy("department", "location");
 * 
 * // GROUP BY with sort direction
 * GroupBy groupBy = new GroupBy("sales_amount", SortDirection.DESC);
 * 
 * // GROUP BY with mixed sort directions
 * Map<String, SortDirection> orders = new LinkedHashMap<>();
 * orders.put("department", SortDirection.ASC);
 * orders.put("salary", SortDirection.DESC);
 * GroupBy groupBy = new GroupBy(orders);
 * }</pre>
 * 
 * @see Clause
 * @see SortDirection
 * @see Having
 */
public class GroupBy extends Clause {

    // For Kryo
    GroupBy() {
    }

    /**
     * Creates a new GROUP BY clause with the specified condition.
     * 
     * @param condition the grouping condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * GroupBy groupBy = new GroupBy(CF.expr("YEAR(order_date)"));
     * }</pre>
     */
    public GroupBy(final Condition condition) {
        super(Operator.GROUP_BY, condition);
    }

    /**
     * Creates a new GROUP BY clause with the specified property names.
     * The columns will be grouped in the order provided.
     * 
     * @param propNames the property names to group by
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // GROUP BY department, location
     * GroupBy groupBy = new GroupBy("department", "location");
     * }</pre>
     */
    public GroupBy(final String... propNames) {
        this(CF.expr(OrderBy.createCondition(propNames)));
    }

    /**
     * Creates a new GROUP BY clause with a single property and sort direction.
     * 
     * @param propName the property name to group by
     * @param direction the sort direction (ASC or DESC)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // GROUP BY sales_amount DESC
     * GroupBy groupBy = new GroupBy("sales_amount", SortDirection.DESC);
     * }</pre>
     */
    public GroupBy(final String propName, final SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propName, direction)));
    }

    /**
     * Creates a new GROUP BY clause with multiple properties and a single sort direction.
     * All properties will use the same sort direction.
     * 
     * @param propNames the collection of property names to group by
     * @param direction the sort direction to apply to all properties
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("department", "location");
     * GroupBy groupBy = new GroupBy(columns, SortDirection.DESC);
     * // Results in: GROUP BY department DESC, location DESC
     * }</pre>
     */
    public GroupBy(final Collection<String> propNames, final SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propNames, direction)));
    }

    /**
     * Creates a new GROUP BY clause with custom sort directions for each property.
     * The map should maintain insertion order (use LinkedHashMap) to preserve the grouping order.
     * 
     * @param orders a map of property names to their sort directions (should be a {@code LinkedHashMap})
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("department", SortDirection.ASC);
     * orders.put("salary", SortDirection.DESC);
     * orders.put("hire_date", SortDirection.ASC);
     * GroupBy groupBy = new GroupBy(orders);
     * // Results in: GROUP BY department ASC, salary DESC, hire_date ASC
     * }</pre>
     */
    public GroupBy(final Map<String, SortDirection> orders) {
        this(OrderBy.createCondition(orders));
    }
}