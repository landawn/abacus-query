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

import java.util.Collection;

/**
 * Represents a NATURAL JOIN clause in SQL queries.
 * 
 * <p>A NATURAL JOIN automatically joins tables based on all columns with the same names 
 * in both tables. It's a special type of equi-join where the join predicate arises 
 * implicitly by comparing all columns in both tables that have the same column names.</p>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>No explicit join condition is needed when columns have matching names</li>
 *   <li>If an explicit condition is provided, it acts as an additional filter</li>
 *   <li>Be cautious as table schema changes can affect natural join behavior</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple natural join
 * NaturalJoin join1 = new NaturalJoin("employees");
 * // Results in: NATURAL JOIN employees
 * 
 * // Natural join with additional condition
 * Condition activeOnly = new Equal("status", "active");
 * NaturalJoin join2 = new NaturalJoin("departments", activeOnly);
 * // Results in: NATURAL JOIN departments WHERE status = 'active'
 * 
 * // Multiple tables natural join
 * List<String> tables = Arrays.asList("employees", "departments");
 * NaturalJoin join3 = new NaturalJoin(tables, condition);
 * }</pre>
 */
public class NaturalJoin extends Join {

    // For Kryo
    NaturalJoin() {
    }

    /**
     * Constructs a NATURAL JOIN with the specified entity/table.
     * The join will automatically use all columns with matching names.
     *
     * @param joinEntity the name of the entity/table to join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NaturalJoin join = new NaturalJoin("orders");
     * // Joins on all columns with same names in both tables
     * }</pre>
     */
    public NaturalJoin(final String joinEntity) {
        super(Operator.NATURAL_JOIN, joinEntity);
    }

    /**
     * Constructs a NATURAL JOIN with the specified entity/table and an additional condition.
     * The natural join occurs on matching column names, with the condition as an extra filter.
     *
     * @param joinEntity the name of the entity/table to join
     * @param condition additional condition to apply after the natural join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition recentOnly = new GreaterThan("orderDate", "2023-01-01");
     * NaturalJoin join = new NaturalJoin("orders", recentOnly);
     * // Natural join on matching columns, filtered by order date
     * }</pre>
     */
    public NaturalJoin(final String joinEntity, final Condition condition) {
        super(Operator.NATURAL_JOIN, joinEntity, condition);
    }

    /**
     * Constructs a NATURAL JOIN with multiple entities/tables and a condition.
     * Useful for joining multiple tables in a single natural join operation.
     *
     * @param joinEntities collection of entity/table names to join
     * @param condition additional condition to apply after the natural join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> tables = Arrays.asList("customers", "orders", "products");
     * Condition highValue = new GreaterThan("totalAmount", 1000);
     * NaturalJoin join = new NaturalJoin(tables, highValue);
     * // Natural join across all tables on matching columns
     * }</pre>
     */
    public NaturalJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.NATURAL_JOIN, joinEntities, condition);
    }
}