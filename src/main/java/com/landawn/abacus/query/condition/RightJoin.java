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

/**
 * Represents a RIGHT JOIN clause in SQL queries.
 * 
 * <p>A RIGHT JOIN (or RIGHT OUTER JOIN) returns all records from the right table (table2),
 * and the matched records from the left table (table1). If there is no match, NULL values
 * are returned for columns from the left table. This is essentially the opposite of a LEFT JOIN.</p>
 * 
 * <p>RIGHT JOIN behavior:</p>
 * <ul>
 *   <li>All rows from the right table are included in the result</li>
 *   <li>Matching rows from the left table are included with their values</li>
 *   <li>Non-matching rows from right table have NULL for left table columns</li>
 *   <li>Rows from left table without matches in right table are excluded</li>
 *   <li>The join condition determines which rows match between tables</li>
 * </ul>
 * 
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Finding all records in a reference table, with optional related data</li>
 *   <li>Listing all products even if they have no orders</li>
 *   <li>Showing all departments including those without employees</li>
 *   <li>Identifying missing relationships from the right table perspective</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple right join
 * RightJoin join1 = new RightJoin("departments");
 * // Generates: RIGHT JOIN departments
 *
 * // Right join with ON condition
 * RightJoin join2 = new RightJoin("departments",
 *     new On("employees.dept_id", "departments.id"));
 * // Generates: RIGHT JOIN departments ON employees.dept_id = departments.id
 * // This returns all departments, even those with no employees
 *
 * // Complex right join with multiple conditions
 * RightJoin complexJoin = new RightJoin("products p",
 *     new And(
 *         new On("order_items.product_id", "p.id"),
 *         new Equal("p.active", true)
 *     ));
 * // Generates: RIGHT JOIN products p (ON order_items.product_id = p.id) AND (p.active = true)
 *
 * // Using Expression for custom join logic
 * RightJoin exprJoin = new RightJoin("departments",
 *     ConditionFactory.expr("employees.dept_id = departments.id"));
 * // Generates: RIGHT JOIN departments employees.dept_id = departments.id
 * }</pre>
 * 
 * @see LeftJoin
 * @see InnerJoin
 * @see FullJoin
 */
public class RightJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized RightJoin instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    RightJoin() {
    }

    /**
     * Creates a RIGHT JOIN clause for the specified table/entity.
     * This creates a join without an ON condition, which may need to be
     * specified separately or will use implicit join conditions based on
     * foreign key relationships (if supported by the database).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple right join without condition
     * RightJoin join = new RightJoin("departments");
     * // Generates: RIGHT JOIN departments
     *
     * // Right join with table alias
     * RightJoin aliasJoin = new RightJoin("all_customers c");
     * // Generates: RIGHT JOIN all_customers c
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public RightJoin(final String joinEntity) {
        super(Operator.RIGHT_JOIN, joinEntity);
    }

    /**
     * Creates a RIGHT JOIN clause with a join condition.
     * This is the most common form of RIGHT JOIN, specifying both the table to join
     * and the condition for matching rows. All rows from the right table are preserved,
     * with NULL values for non-matching rows from the left table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join orders with all products using ON
     * RightJoin allProducts = new RightJoin("products p",
     *     new On("order_items.product_id", "p.id"));
     * // Generates: RIGHT JOIN products p ON order_items.product_id = p.id
     *
     * // Find all departments including those with no employees
     * RightJoin allDepts = new RightJoin("departments d",
     *     new On("employees.dept_id", "d.id"));
     * // Generates: RIGHT JOIN departments d ON employees.dept_id = d.id
     *
     * // Complex join with ON condition and filtering
     * RightJoin activeCategories = new RightJoin("categories c",
     *     new And(
     *         new On("products.category_id", "c.id"),
     *         new Equal("c.active", true),
     *         new GreaterThan("c.created_date", "2023-01-01")
     *     ));
     * // Generates: RIGHT JOIN categories c (ON products.category_id = c.id) AND (c.active = true) AND (c.created_date > '2023-01-01')
     *
     * // Using Expression for custom join logic
     * RightJoin exprJoin = new RightJoin("products p",
     *     ConditionFactory.expr("order_items.product_id = p.id AND p.stock > 0"));
     * // Generates: RIGHT JOIN products p order_items.product_id = p.id AND p.stock > 0
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an On condition for column equality).
     *                  Can be a complex condition using And/Or for multiple criteria.
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public RightJoin(final String joinEntity, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntity, condition);
    }

    /**
     * Creates a RIGHT JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single RIGHT JOIN operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables with ON conditions
     * List<String> tables = Arrays.asList("categories c", "subcategories sc");
     * RightJoin join = new RightJoin(tables,
     *     new And(
     *         new On("p.category_id", "c.id"),
     *         new On("p.subcategory_id", "sc.id")
     *     ));
     * // Generates: RIGHT JOIN categories c, subcategories sc (ON p.category_id = c.id) AND (ON p.subcategory_id = sc.id)
     *
     * // Using Expression for multiple tables
     * RightJoin exprJoin = new RightJoin(tables,
     *     ConditionFactory.expr("p.category_id = c.id AND p.subcategory_id = sc.id"));
     * // Generates: RIGHT JOIN categories c, subcategories sc p.category_id = c.id AND p.subcategory_id = sc.id
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public RightJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntities, condition);
    }
}