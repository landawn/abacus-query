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
 * Represents a WHERE clause in SQL queries.
 * This class is used to specify conditions that filter records in a query result.
 * 
 * <p>The WHERE clause is one of the most fundamental SQL clauses, used to extract only those
 * records that fulfill a specified condition. It supports simple comparisons, complex logical
 * combinations, pattern matching, null checks, and subqueries. The WHERE clause is evaluated
 * for each row before any grouping occurs.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Filters individual rows before grouping</li>
 *   <li>Can contain any valid SQL condition</li>
 *   <li>Supports nested conditions with AND/OR</li>
 *   <li>Cannot contain aggregate functions (use HAVING for those)</li>
 *   <li>Evaluated before GROUP BY, HAVING, and ORDER BY</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple condition
 * Condition condition = Filters.eq("status", "active");
 * Where where = new Where(condition);
 * // SQL: WHERE status = 'active'
 *
 * // Complex condition
 * Condition and = Filters.and(Filters.eq("age", 25), Filters.gt("salary", 50000));
 * Where where2 = new Where(and);
 * // SQL: WHERE age = 25 AND salary > 50000
 * }</pre>
 * 
 * @see Clause
 * @see Having
 * @see Condition
 */
public class Where extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Where instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Where() {
    }

    /**
     * Creates a WHERE clause with the specified condition.
     * The condition can be any valid SQL condition including simple comparisons,
     * logical combinations (AND/OR), or complex expressions.
     *
     * <p>The WHERE clause is essential for filtering query results. It's evaluated
     * row by row, and only rows where the condition evaluates to true are included
     * in the result set. NULL comparisons require special handling with IS NULL
     * or IS NOT NULL operators.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple WHERE clause
     * Condition condition = Filters.like("name", "%John%");
     * Where where = new Where(condition);
     * // SQL: WHERE name LIKE '%John%'
     *
     * // Complex WHERE with multiple conditions
     * Condition complexCondition = Filters.or(
     *     Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000)),
     *     Filters.eq("vip", true)
     * );
     * Where complexWhere = new Where(complexCondition);
     * // SQL: WHERE ((status = 'active') AND (balance > 1000)) OR (vip = true)
     *
     * // WHERE with subquery
     * SubQuery activeUsers = Filters.subQuery("SELECT id FROM users WHERE active = true");
     * Where whereIn = new Where(Filters.in("user_id", activeUsers));
     * // SQL: WHERE user_id IN (SELECT id FROM users WHERE active = true)
     * }</pre>
     *
     * @param condition the condition to be used in the WHERE clause. Must not be null.
     * @throws NullPointerException if condition is null
     */
    public Where(final Condition condition) {
        super(Operator.WHERE, condition);
    }
}