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

import java.util.Map;

import com.landawn.abacus.query.condition.ConditionFactory.CF;

/**
 * Represents an ON clause used in SQL JOIN operations.
 * 
 * <p>The ON clause specifies the join condition between tables. Unlike USING,
 * ON allows for more complex join conditions including different column names,
 * multiple conditions, and expressions.</p>
 * 
 * <p>Common usage patterns:</p>
 * <ul>
 *   <li>Simple column equality: table1.id = table2.foreign_id</li>
 *   <li>Multiple conditions: combining with AND/OR</li>
 *   <li>Complex expressions: joins with calculations or functions</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple ON condition with custom condition
 * Condition joinCondition = new Equal("t1.id", new Expression("t2.user_id"));
 * On on1 = new On(joinCondition);
 * 
 * // Convenient method for column equality
 * On on2 = new On("employees.department_id", "departments.id");
 * // Results in: ON employees.department_id = departments.id
 * 
 * // Multiple join conditions using Map
 * Map<String, String> joinMap = new LinkedHashMap<>();
 * joinMap.put("orders.customer_id", "customers.id");
 * joinMap.put("orders.region", "customers.region");
 * On on3 = new On(joinMap);
 * // Results in: ON orders.customer_id = customers.id AND orders.region = customers.region
 * }</pre>
 */
public class On extends Cell {

    // For Kryo
    On() {
    }

    /**
     * Constructs an ON clause with a custom condition.
     * 
     * <p>This is the most flexible constructor, allowing any type of condition
     * including complex expressions, AND/OR combinations, etc.</p>
     *
     * @param condition the join condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Complex join condition
     * And complexCondition = new And(
     *     new Equal("a.id", new Expression("b.a_id")),
     *     new GreaterThan("b.created", "2024-01-01")
     * );
     * On on = new On(complexCondition);
     * }</pre>
     */
    public On(final Condition condition) {
        super(Operator.ON, condition);
    }

    /**
     * Constructs an ON clause for simple column equality between tables.
     * 
     * <p>This is a convenience constructor for the common case of joining
     * on equal column values from different tables.</p>
     *
     * @param propName the column name from the first table
     * @param anoPropName the column name from the second table
     * 
     * <p>Example:</p>
     * <pre>{@code
     * On on = new On("users.id", "posts.user_id");
     * // Results in: ON users.id = posts.user_id
     * 
     * // Can include table aliases
     * On on2 = new On("u.department_id", "d.id");
     * // Results in: ON u.department_id = d.id
     * }</pre>
     */
    public On(final String propName, final String anoPropName) {
        this(createOnCondition(propName, anoPropName));
    }

    /**
     * Constructs an ON clause with multiple column equality conditions.
     * 
     * <p>All conditions in the map are combined with AND. This is useful
     * for composite key joins or multiple join criteria.</p>
     *
     * @param propNamePair map of column pairs where key is from first table,
     *                     value is from second table
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> joinConditions = new LinkedHashMap<>();
     * joinConditions.put("orders.customer_id", "customers.id");
     * joinConditions.put("orders.store_id", "customers.preferred_store_id");
     * On on = new On(joinConditions);
     * // Results in: ON orders.customer_id = customers.id 
     * //            AND orders.store_id = customers.preferred_store_id
     * }</pre>
     */
    public On(final Map<String, String> propNamePair) {
        this(createOnCondition(propNamePair));
    }

    /**
     * Creates an ON condition for simple column equality.
     *
     * @param propName the first column name
     * @param anoPropName the second column name
     * @return an Equal condition comparing the two columns
     */
    static Condition createOnCondition(final String propName, final String anoPropName) {
        return new Equal(propName, CF.expr(anoPropName));
    }

    /**
     * Creates an ON condition from multiple column pairs.
     *
     * @param propNamePair map of column name pairs
     * @return a single Equal condition or an And condition combining multiple equalities
     */
    static Condition createOnCondition(final Map<String, String> propNamePair) {
        if (propNamePair.size() == 1) {
            final Map.Entry<String, String> entry = propNamePair.entrySet().iterator().next();

            return createOnCondition(entry.getKey(), entry.getValue());
        } else {
            final And and = CF.and();

            for (final Map.Entry<String, String> entry : propNamePair.entrySet()) {
                and.add(createOnCondition(entry.getKey(), entry.getValue()));
            }

            return and;
        }
    }
}