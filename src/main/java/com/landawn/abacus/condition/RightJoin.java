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
 * Represents a RIGHT JOIN clause in SQL queries.
 * 
 * <p>A RIGHT JOIN (or RIGHT OUTER JOIN) returns all records from the right table (table2),
 * and the matched records from the left table (table1). If there is no match, NULL values
 * are returned for columns from the left table.</p>
 * 
 * <p>RIGHT JOIN behavior:</p>
 * <ul>
 *   <li>All rows from the right table are included</li>
 *   <li>Matching rows from the left table are included</li>
 *   <li>Non-matching rows from right table have NULL for left table columns</li>
 *   <li>Rows from left table without matches are excluded</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple right join
 * RightJoin join1 = new RightJoin("departments");
 * // Results in: RIGHT JOIN departments
 * 
 * // Right join with condition
 * On onCondition = new On("employees.dept_id", "departments.id");
 * RightJoin join2 = new RightJoin("departments", onCondition);
 * // Results in: RIGHT JOIN departments ON employees.dept_id = departments.id
 * // This returns all departments, even those with no employees
 * 
 * // Multiple table right join
 * List<String> tables = Arrays.asList("orders", "order_items");
 * Condition condition = new Equal("orders.id", new Expression("order_items.order_id"));
 * RightJoin join3 = new RightJoin(tables, condition);
 * }</pre>
 */
public class RightJoin extends Join {

    // For Kryo
    RightJoin() {
    }

    /**
     * Constructs a RIGHT JOIN with the specified entity/table.
     * 
     * <p>Creates a basic right join without an explicit ON condition.
     * The join condition should be specified separately or will use natural join behavior.</p>
     *
     * @param joinEntity the name of the entity/table to right join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * RightJoin join = new RightJoin("customers");
     * // Use when you want all customers, even those without orders
     * // SELECT * FROM orders RIGHT JOIN customers
     * }</pre>
     */
    public RightJoin(final String joinEntity) {
        super(Operator.RIGHT_JOIN, joinEntity);
    }

    /**
     * Constructs a RIGHT JOIN with the specified entity/table and join condition.
     * 
     * <p>This is the most common form, specifying both the table to join and how to join it.</p>
     *
     * @param joinEntity the name of the entity/table to right join
     * @param condition the join condition (typically an ON or USING clause)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Get all products, even those never ordered
     * On onClause = new On("order_items.product_id", "products.id");
     * RightJoin join = new RightJoin("products", onClause);
     * // Results in: RIGHT JOIN products ON order_items.product_id = products.id
     * 
     * // With additional conditions
     * And complexCondition = new And(
     *     new On("orders.product_id", "products.id"),
     *     new Equal("products.active", true)
     * );
     * RightJoin activeProducts = new RightJoin("products", complexCondition);
     * }</pre>
     */
    public RightJoin(final String joinEntity, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntity, condition);
    }

    /**
     * Constructs a RIGHT JOIN with multiple entities/tables and a condition.
     * 
     * <p>Useful for joining multiple tables in a single right join operation.</p>
     *
     * @param joinEntities collection of entity/table names to right join
     * @param condition the join condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Right join multiple related tables
     * List<String> tables = Arrays.asList("categories", "subcategories");
     * Condition joinCondition = new And(
     *     new On("products.category_id", "categories.id"),
     *     new On("products.subcategory_id", "subcategories.id")
     * );
     * RightJoin join = new RightJoin(tables, joinCondition);
     * // Gets all categories and subcategories, even those with no products
     * }</pre>
     */
    public RightJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntities, condition);
    }
}