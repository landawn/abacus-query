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
 * <p>A CROSS JOIN does not accept an explicit condition. For a filtered Cartesian product,
 * add the predicate through a separate {@link Where} clause.</p>
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
 * CrossJoin multiJoin = new CrossJoin(tables);
 * // SQL: CROSS JOIN (sizes, colors)
 * // Results in all combinations of products × sizes × colors
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
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public CrossJoin(final String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Creates a CROSS JOIN clause with multiple tables/entities and no join condition.
     * This produces the Cartesian product of the listed tables. The rendered SQL is
     * {@code CROSS JOIN (t1, t2, ...)}. This is a convenience equivalent to
     * {@code new CrossJoin(joinEntities)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // All combinations of sizes, colors, and styles
     * List<String> tables = Arrays.asList("sizes s", "colors c", "styles st");
     * CrossJoin join = new CrossJoin(tables);
     * // SQL: CROSS JOIN (sizes s, colors c, styles st)
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null}, empty, or blank elements
     */
    public CrossJoin(final Collection<String> joinEntities) {
        super(Operator.CROSS_JOIN, joinEntities, null);
    }
}
