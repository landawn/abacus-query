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

import static com.landawn.abacus.query.SK.COMMA_SPACE;
import static com.landawn.abacus.query.SK.SPACE;

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.Objectory;

/**
 * Represents an ORDER BY clause in SQL queries.
 * This class is used to specify the sort order of query results.
 * 
 * <p>The ORDER BY clause sorts the result set by one or more columns in ascending (ASC)
 * or descending (DESC) order. By default, sorting is in ascending order if not specified.
 * The order of columns in the ORDER BY clause determines the priority of sorting.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Single or multiple column sorting</li>
 *   <li>Ascending (ASC) or descending (DESC) order</li>
 *   <li>Mixed sort directions for different columns</li>
 *   <li>Support for expressions and custom conditions</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple ordering by single column (default ASC)
 * OrderBy orderBy1 = new OrderBy("lastName");
 * // Results in: ORDER BY lastName
 * 
 * // Multiple columns
 * OrderBy orderBy2 = new OrderBy("lastName", "firstName");
 * // Results in: ORDER BY lastName, firstName
 * 
 * // With sort direction
 * OrderBy orderBy3 = new OrderBy("salary", SortDirection.DESC);
 * // Results in: ORDER BY salary DESC
 * 
 * // Multiple columns with same direction
 * OrderBy orderBy4 = new OrderBy(Arrays.asList("created", "modified"), SortDirection.DESC);
 * // Results in: ORDER BY created, modified DESC
 * 
 * // Mixed directions using LinkedHashMap
 * Map<String, SortDirection> orders = new LinkedHashMap<>();
 * orders.put("priority", SortDirection.DESC);
 * orders.put("created", SortDirection.ASC);
 * OrderBy orderBy5 = new OrderBy(orders);
 * // Results in: ORDER BY priority DESC, created ASC
 * }</pre>
 * 
 * @see SortDirection
 */
public class OrderBy extends Clause {

    // For Kryo
    OrderBy() {
    }

    /**
     * Constructs an ORDER BY clause with a custom condition.
     * This allows for complex ordering expressions beyond simple column names.
     * 
     * <p>Use this constructor when you need to order by calculated values,
     * case expressions, or other complex SQL expressions.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Expression expr = new Expression("CASE WHEN status='urgent' THEN 1 ELSE 2 END");
     * OrderBy orderBy = new OrderBy(expr);
     * }</pre>
     * 
     * @param condition the ordering condition
     */
    public OrderBy(final Condition condition) {
        super(Operator.ORDER_BY, condition);
    }

    /**
     * Constructs an ORDER BY clause with multiple property names.
     * All properties will be sorted in ascending order by default.
     * 
     * <p>The order of properties in the parameter list determines the sort priority.
     * The first property has the highest priority, followed by subsequent properties.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * OrderBy orderBy = new OrderBy("country", "state", "city");
     * // Results in: ORDER BY country, state, city
     * }</pre>
     * 
     * @param propNames variable number of property names to sort by
     * @throws IllegalArgumentException if propNames is null or empty
     */
    public OrderBy(final String... propNames) {
        this(CF.expr(createCondition(propNames)));
    }

    /**
     * Constructs an ORDER BY clause with a single property and sort direction.
     * This is the most common use case for ordering query results.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * OrderBy orderBy = new OrderBy("price", SortDirection.DESC);
     * // Results in: ORDER BY price DESC
     * }</pre>
     * 
     * @param propName the property name to sort by
     * @param direction the sort direction (ASC or DESC)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public OrderBy(final String propName, final SortDirection direction) {
        this(CF.expr(createCondition(propName, direction)));
    }

    /**
     * Constructs an ORDER BY clause with multiple properties and a single sort direction.
     * All properties will use the same sort direction.
     * 
     * <p>This is useful when you want to sort by multiple columns in the same direction,
     * such as sorting multiple date fields in descending order.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> dateFields = Arrays.asList("created", "updated", "published");
     * OrderBy orderBy = new OrderBy(dateFields, SortDirection.DESC);
     * // Results in: ORDER BY created, updated, published DESC
     * }</pre>
     * 
     * @param propNames collection of property names to sort by
     * @param direction the sort direction to apply to all properties
     * @throws IllegalArgumentException if propNames is null or empty
     */
    public OrderBy(final Collection<String> propNames, final SortDirection direction) {
        this(CF.expr(createCondition(propNames, direction)));
    }

    /**
     * Constructs an ORDER BY clause with properties having different sort directions.
     * This provides maximum flexibility for complex sorting requirements.
     * 
     * <p>The map should maintain insertion order (use LinkedHashMap) to ensure
     * predictable sort priority. Each entry maps a property name to its sort direction.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("isActive", SortDirection.DESC);  // Active records first
     * orders.put("priority", SortDirection.DESC);  // High priority first
     * orders.put("created", SortDirection.ASC);    // Oldest first
     * OrderBy orderBy = new OrderBy(orders);
     * // Results in: ORDER BY isActive DESC, priority DESC, created ASC
     * }</pre>
     * 
     * @param orders should be a {@code LinkedHashMap} to preserve insertion order.
     *               Maps property names to their respective sort directions.
     * @throws IllegalArgumentException if orders is null or empty
     */
    public OrderBy(final Map<String, SortDirection> orders) {
        this(createCondition(orders));
    }

    /**
     * Creates a comma-separated string of property names for ordering.
     * This static helper method formats property names for the ORDER BY clause.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String condition = OrderBy.createCondition("name", "age", "city");
     * // Returns: "name, age, city"
     * }</pre>
     *
     * @param propNames array of property names
     * @return formatted string for ORDER BY clause
     */
    static String createCondition(final String... propNames) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates an ordering condition for a single property with direction.
     * This static helper method formats a property name with its sort direction.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String condition = OrderBy.createCondition("price", SortDirection.DESC);
     * // Returns: "price DESC"
     * }</pre>
     *
     * @param propName the property name
     * @param direction the sort direction
     * @return formatted string for ORDER BY clause
     */
    static String createCondition(final String propName, final SortDirection direction) {
        return propName + SPACE + direction.toString();
    }

    /**
     * Creates an ordering condition for multiple properties with the same direction.
     * This static helper method formats multiple property names with a single sort direction.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> props = Arrays.asList("created", "modified");
     * String condition = OrderBy.createCondition(props, SortDirection.DESC);
     * // Returns: "created, modified DESC"
     * }</pre>
     *
     * @param propNames collection of property names
     * @param direction the sort direction
     * @return formatted string for ORDER BY clause
     */
    static String createCondition(final Collection<String> propNames, final SortDirection direction) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            sb.append(SPACE);
            sb.append(direction.toString());

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates an ordering condition from a map of properties and their directions.
     * This static helper method formats multiple property-direction pairs for complex ordering.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("priority", SortDirection.DESC);
     * orders.put("created", SortDirection.ASC);
     * String condition = OrderBy.createCondition(orders);
     * // Returns: "priority DESC, created ASC"
     * }</pre>
     *
     * @param orders map of property names to sort directions
     * @return formatted string for ORDER BY clause
     */
    static String createCondition(final Map<String, SortDirection> orders) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(entry.getKey());
                sb.append(SPACE);
                sb.append(entry.getValue().toString());
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }
}