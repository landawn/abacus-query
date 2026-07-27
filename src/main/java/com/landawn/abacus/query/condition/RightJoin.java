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
 * A RIGHT JOIN (also called RIGHT OUTER JOIN) returns all rows from the right table,
 * and the matched rows from the left table. If there is no match, NULL values are returned
 * for columns from the left table. This is essentially the opposite of a LEFT JOIN, with
 * the roles of the two tables swapped.
 * 
 * <p>RIGHT JOIN is commonly used when you want to:
 * <ul>
 *   <li>Include all records from the right (reference) table regardless of matches</li>
 *   <li>List all products even if they have no orders</li>
 *   <li>Show all departments including those without employees</li>
 *   <li>Identify missing relationships from the right table perspective</li>
 * </ul>
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li>Returns ALL rows from the right table</li>
 *   <li>Returns matching rows from the left table</li>
 *   <li>Returns NULL for left table columns when no match exists</li>
 *   <li>Rows from the left table without matches in the right table are excluded</li>
 *   <li>Result set size is at least the size of the right table (more if a right-table row matches multiple left-table rows)</li>
 *   <li>Order matters: RIGHT JOIN is not commutative</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple right join
 * RightJoin join1 = new RightJoin("departments");
 * // SQL: RIGHT JOIN departments
 *
 * // Right join with ON condition
 * RightJoin join2 = new RightJoin("departments",
 *     new On("employees.dept_id", "departments.id"));
 * // SQL: RIGHT JOIN departments ON employees.dept_id = departments.id
 * // This returns all departments, even those with no employees
 *
 * // Complex right join with multiple conditions
 * RightJoin complexJoin = new RightJoin("products p",
 *     new And(
 *         Filters.expr("order_items.product_id = p.id"),
 *         Filters.equal("p.active", true)
 *     ));
 * // SQL: RIGHT JOIN products p ON ((order_items.product_id = p.id) AND (p.active = true))
 *
 * // Using SqlExpression for custom join logic
 * RightJoin exprJoin = new RightJoin("departments",
 *     Filters.expr("employees.dept_id = departments.id"));
 * // SQL: RIGHT JOIN departments ON employees.dept_id = departments.id
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see On
 * @see Using
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
     * Creates a RIGHT JOIN clause for the specified table or entity without a join condition.
     * Most databases require an {@code ON} or {@code USING} clause for a RIGHT JOIN; use
     * {@link #RightJoin(String, Condition)} when a condition is required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple right join without condition
     * RightJoin join = new RightJoin("departments");
     * // SQL: RIGHT JOIN departments
     *
     * // Right join with table alias
     * RightJoin aliasJoin = new RightJoin("all_customers c");
     * // SQL: RIGHT JOIN all_customers c
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
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
     * // SQL: RIGHT JOIN products p ON order_items.product_id = p.id
     *
     * // Find all departments including those with no employees
     * RightJoin allDepts = new RightJoin("departments d",
     *     new On("employees.dept_id", "d.id"));
     * // SQL: RIGHT JOIN departments d ON employees.dept_id = d.id
     *
     * // Complex join with key comparison and filtering
     * RightJoin activeCategories = new RightJoin("categories c",
     *     new And(
     *         Filters.expr("products.category_id = c.id"),
     *         Filters.equal("c.active", true),
     *         Filters.greaterThan("c.created_date", "2023-01-01")
     *     ));
     * // SQL: RIGHT JOIN categories c ON ((products.category_id = c.id) AND (c.active = true) AND (c.created_date > '2023-01-01'))
     *
     * // Using SqlExpression for custom join logic
     * RightJoin exprJoin = new RightJoin("products p",
     *     Filters.expr("order_items.product_id = p.id AND p.stock > 0"));
     * // SQL: RIGHT JOIN products p ON order_items.product_id = p.id AND p.stock > 0
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param joinCondition the join condition. A plain non-empty predicate is automatically prefixed with
     *            {@code ON}; an explicit {@link On} (or deprecated {@link Using}) renders its own keyword.
     *            May be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank, or if {@code joinCondition} is or contains a
     *                                  {@link Criteria}, a null operator, a SQL clause, an {@link SqlExpression} whose text begins with {@code ON} or {@code USING},
     *                                  a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand,
     *                                  a standalone {@link SubQuery}, or an empty predicate (a blank {@link SqlExpression} or empty {@link Junction})
     */
    public RightJoin(final String joinEntity, final Condition joinCondition) {
        super(Operator.RIGHT_JOIN, joinEntity, joinCondition);
    }

    /**
     * Creates a RIGHT JOIN clause with multiple tables/entities and a join condition.
     * This allows joining multiple tables in a single RIGHT JOIN operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple related tables with predicates
     * List<String> tables = Arrays.asList("categories c", "subcategories sc");
     * RightJoin join = new RightJoin(tables,
     *     new And(
     *         Filters.expr("p.category_id = c.id"),
     *         Filters.expr("p.subcategory_id = sc.id")
     *     ));
     * // SQL: RIGHT JOIN (categories c, subcategories sc) ON ((p.category_id = c.id) AND (p.subcategory_id = sc.id))
     *
     * // Using SqlExpression for multiple tables
     * RightJoin exprJoin = new RightJoin(tables,
     *     Filters.expr("p.category_id = c.id AND p.subcategory_id = sc.id"));
     * // SQL: RIGHT JOIN (categories c, subcategories sc) ON p.category_id = c.id AND p.subcategory_id = sc.id
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param joinCondition the join condition. A plain non-empty predicate is automatically prefixed with
     *            {@code ON}; an explicit {@link On} (or deprecated {@link Using}) renders its own keyword.
     *            May be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null}, empty, or blank elements,
     *                                  or if {@code joinCondition} is or contains a {@link Criteria}, a null operator, a SQL clause,
     *                                  an {@link SqlExpression} whose text begins with {@code ON} or {@code USING},
     *                                  a nested ON/USING connector, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand,
     *                                  a standalone {@link SubQuery}, or an empty predicate (a blank {@link SqlExpression} or empty {@link Junction})
     */
    public RightJoin(final Collection<String> joinEntities, final Condition joinCondition) {
        super(Operator.RIGHT_JOIN, joinEntities, joinCondition);
    }
}
