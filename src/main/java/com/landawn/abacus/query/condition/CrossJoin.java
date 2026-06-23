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
 * Represents a CROSS JOIN operation in SQL queries.
 * A CROSS JOIN returns the Cartesian product of rows from the joined tables,
 * combining each row from the first table with each row from the second table.
 * 
 * <p>CROSS JOIN creates a result set where the number of rows equals the product
 * of the number of rows in each table. For example, if Table A has 5 rows and
 * Table B has 3 rows, the CROSS JOIN will produce 15 rows (5 × 3).</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>No join condition is typically used (no ON clause)</li>
 *   <li>Every row from the first table is paired with every row from the second table</li>
 *   <li>Result set size = rows in table1 × rows in table2</li>
 *   <li>Useful for generating combinations or test data</li>
 * </ul>
 * 
 * <p>Note: While this implementation allows a join condition for flexibility,
 * standard SQL CROSS JOIN does not accept an {@code ON} clause. The supplied condition is
 * appended after an {@code ON} keyword unless the condition is an {@link On} or {@link Using},
 * so the rendered SQL may not be valid in all dialects. For a filtered Cartesian product,
 * prefer adding the predicate via a separate {@link Where} clause (which is semantically
 * equivalent to {@link InnerJoin} with the same condition).</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple CROSS JOIN - all combinations of products and colors
 * CrossJoin join = new CrossJoin("colors");
 * // SQL: CROSS JOIN colors
 * // Each product will be paired with each color
 *
 * // CROSS JOIN multiple tables
 * List<String> tables = Arrays.asList("sizes", "colors");
 * CrossJoin multiJoin = new CrossJoin(tables, null);
 * // SQL: CROSS JOIN (sizes, colors)
 * // Results in all combinations of products × sizes × colors
 *
 * // CROSS JOIN with condition (unusual but supported)
 * CrossJoin filtered = new CrossJoin("categories",
 *     Filters.equal("active", true));
 * // SQL: CROSS JOIN categories ON active = true
 * // Note: Functionally equivalent to INNER JOIN with the condition
 *
 * // CROSS JOIN with Expression
 * CrossJoin exprJoin = new CrossJoin("inventory",
 *     Filters.expr("quantity > 0"));
 * // SQL: CROSS JOIN inventory ON quantity > 0
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see NaturalJoin
 * @see On
 * @see Using
 */
public class CrossJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized CrossJoin instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    CrossJoin() {
    }

    /**
     * Creates a CROSS JOIN clause for the specified table or entity.
     * This creates a Cartesian product join without an ON condition,
     * combining every row from the first table with every row from the second table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple cross join - all combinations
     * CrossJoin join = new CrossJoin("colors");
     * // SQL: CROSS JOIN colors
     * // If products has 10 rows and colors has 5 rows, result has 50 rows
     *
     * // Cross join with table alias
     * CrossJoin aliasJoin = new CrossJoin("available_sizes s");
     * // SQL: CROSS JOIN available_sizes s
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty
     */
    public CrossJoin(final String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Creates a CROSS JOIN clause with a join condition.
     * While CROSS JOINs typically don't use conditions, this form allows for non-standard usage.
     * Adding a condition makes it functionally equivalent to an INNER JOIN.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Cross join with filter using Expression
     * CrossJoin filtered = new CrossJoin("products p",
     *     Filters.expr("p.category = 'electronics'"));
     * // SQL: CROSS JOIN products p ON p.category = 'electronics'
     *
     * // Cross join with ON condition (unusual usage)
     * CrossJoin withOn = new CrossJoin("inventory i",
     *     new On("w.id", "i.warehouse_id"));
     * // SQL: CROSS JOIN inventory i ON w.id = i.warehouse_id
     * // Note: This is functionally the same as INNER JOIN
     *
     * // Complex cross join with multiple conditions
     * CrossJoin complexCross = new CrossJoin("inventory i",
     *     new And(
     *         Filters.expr("i.warehouse_id = w.id"),
     *         Filters.equal("i.active", true)
     *     ));
     * // SQL: CROSS JOIN inventory i ON ((i.warehouse_id = w.id) AND (i.active = true))
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param cond the condition appended after the join target. Supplying any condition (including an
     *            {@link On}) produces non-standard CROSS JOIN SQL, since a standard CROSS JOIN takes no
     *            {@code ON} clause. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null} or empty, or if {@code cond} is a
     *                                  {@link Criteria}, a SQL clause, an {@link Expression} whose text begins with {@code ON} or {@code USING},
     *                                  or an empty predicate (a blank {@link Expression} or empty {@link Junction})
     */
    public CrossJoin(final String joinEntity, final Condition cond) {
        super(Operator.CROSS_JOIN, joinEntity, cond);
    }

    /**
     * Creates a CROSS JOIN clause with multiple tables/entities and a join condition.
     * This allows creating Cartesian products of multiple tables in a single operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join multiple tables for all combinations with filter
     * List<String> tables = Arrays.asList("sizes s", "colors c", "styles st");
     * CrossJoin join = new CrossJoin(tables,
     *     Filters.equal("active", true));
     * // SQL: CROSS JOIN (sizes s, colors c, styles st) ON active = true
     *
     * // Using ON conditions (makes it similar to INNER JOIN)
     * List<String> relatedTables = Arrays.asList("table1 t1", "table2 t2");
     * CrossJoin withOn = new CrossJoin(relatedTables,
     *     new On("t1.id", "t2.t1_id"));
     * // SQL: CROSS JOIN (table1 t1, table2 t2) ON t1.id = t2.t1_id
     *
     * // Using Expression for complex conditions
     * CrossJoin exprJoin = new CrossJoin(tables,
     *     Filters.expr("active = true AND archived = false"));
     * // SQL: CROSS JOIN (sizes s, colors c, styles st) ON active = true AND archived = false
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param cond the condition appended after the joined table list. Supplying any condition (including an
     *            {@link On}) produces non-standard CROSS JOIN SQL, since a standard CROSS JOIN takes no
     *            {@code ON} clause. Any non-clause {@link Condition} is allowed and can be {@code null}.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null} or empty elements,
     *                                  or if {@code cond} is a {@link Criteria}, a SQL clause, an {@link Expression} whose text begins with {@code ON} or {@code USING},
     *                                  or an empty predicate (a blank {@link Expression} or empty {@link Junction})
     */
    public CrossJoin(final Collection<String> joinEntities, final Condition cond) {
        super(Operator.CROSS_JOIN, joinEntities, cond);
    }
}
