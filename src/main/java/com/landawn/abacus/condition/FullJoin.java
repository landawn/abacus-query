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
 * Represents a FULL OUTER JOIN operation in SQL queries.
 * A FULL JOIN returns all rows when there is a match in either table.
 * It combines the results of both LEFT JOIN and RIGHT JOIN.
 * 
 * <p>When no match is found:</p>
 * <ul>
 *   <li>NULL values are returned for columns from the left table when there's no match</li>
 *   <li>NULL values are returned for columns from the right table when there's no match</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Full join with condition
 * FullJoin join = new FullJoin("orders", CF.eq("users.id", "orders.user_id"));
 * // Results in: FULL JOIN orders ON users.id = orders.user_id
 * 
 * // This returns all users and all orders, with NULLs where no match exists
 * 
 * // Full join multiple tables
 * FullJoin join = new FullJoin(
 *     Arrays.asList("orders", "order_items"), 
 *     CF.eq("orders.id", "order_items.order_id")
 * );
 * }</pre>
 * 
 * @see Join
 * @see LeftJoin
 * @see RightJoin
 * @see InnerJoin
 */
public class FullJoin extends Join {

    // For Kryo
    FullJoin() {
    }

    /**
     * Creates a new FULL JOIN with the specified table/entity.
     * This creates a simple FULL JOIN without any condition.
     * 
     * @param joinEntity the table or entity name to join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * FullJoin join = new FullJoin("departments");
     * // SQL: FULL JOIN departments
     * }</pre>
     */
    public FullJoin(final String joinEntity) {
        super(Operator.FULL_JOIN, joinEntity);
    }

    /**
     * Creates a new FULL JOIN with the specified table/entity and join condition.
     * 
     * @param joinEntity the table or entity name to join
     * @param condition the join condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * FullJoin join = new FullJoin("employees", 
     *     CF.eq("departments.id", "employees.dept_id"));
     * // Returns all departments and all employees, 
     * // with NULLs where no match exists
     * }</pre>
     */
    public FullJoin(final String joinEntity, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntity, condition);
    }

    /**
     * Creates a new FULL JOIN with multiple tables/entities and a join condition.
     * 
     * @param joinEntities the collection of table or entity names to join
     * @param condition the join condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> tables = Arrays.asList("employees", "contractors");
     * FullJoin join = new FullJoin(tables, 
     *     CF.eq("departments.id", "person.dept_id"));
     * // Full joins both employees and contractors tables
     * }</pre>
     */
    public FullJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.FULL_JOIN, joinEntities, condition);
    }
}