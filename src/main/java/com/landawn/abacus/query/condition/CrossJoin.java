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
 * traditional CROSS JOIN doesn't use one. Adding a condition makes it
 * functionally equivalent to an INNER JOIN.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple CROSS JOIN - all combinations of products and colors
 * CrossJoin join = new CrossJoin("colors");
 * // Results in: CROSS JOIN colors
 * // Each product will be paired with each color
 * 
 * // CROSS JOIN multiple tables
 * CrossJoin multiJoin = new CrossJoin(Arrays.asList("sizes", "colors"));
 * // Results in all combinations of products × sizes × colors
 * 
 * // CROSS JOIN with condition (unusual but supported)
 * CrossJoin filtered = new CrossJoin("categories", CF.eq("active", true));
 * // Functionally equivalent to INNER JOIN with the condition
 * }</pre>
 * 
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
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
     * Creates a CROSS JOIN clause for the specified table/entity.
     * This creates a Cartesian product join without an ON condition,
     * combining every row from the first table with every row from the second table.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Simple cross join - all combinations
     * CrossJoin join = new CrossJoin("colors");
     * // Generates: CROSS JOIN colors
     * // If products has 10 rows and colors has 5 rows, result has 50 rows
     *
     * // Cross join with table alias
     * CrossJoin aliasJoin = new CrossJoin("available_sizes s");
     * // Generates: CROSS JOIN available_sizes s
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if joinEntity is null or empty
     */
    public CrossJoin(final String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Creates a CROSS JOIN clause with a join condition.
     * While CROSS JOINs typically don't use conditions, this form allows for non-standard usage.
     * Adding a condition makes it functionally equivalent to an INNER JOIN.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Cross join with filter (use Expression for column references)
     * CrossJoin filtered = new CrossJoin("products p",
     *     ConditionFactory.expr("p.category = 'electronics'"));
     * // Generates: CROSS JOIN products p p.category = 'electronics'
     *
     * // Complex cross join with multiple conditions
     * CrossJoin complexCross = new CrossJoin("inventory i",
     *     new And(
     *         ConditionFactory.expr("i.warehouse_id = w.id"),
     *         new Equal("i.active", true)
     *     ));
     * // Generates: CROSS JOIN inventory i ((i.warehouse_id = w.id) AND (i.active = true))
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias.
     * @param condition the join condition (typically an equality condition between columns).
     *                  Can be a complex condition using And/Or for multiple criteria.
     * @throws IllegalArgumentException if joinEntity is null or empty, or condition is null
     */
    public CrossJoin(final String joinEntity, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntity, condition);
    }

    /**
     * Creates a CROSS JOIN clause with multiple tables/entities and a join condition.
     * This allows creating Cartesian products of multiple tables in a single operation.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Join multiple tables for all combinations
     * List<String> tables = Arrays.asList("sizes s", "colors c", "styles st");
     * CrossJoin join = new CrossJoin(tables,
     *     new Equal("active", true));
     * // Generates: CROSS JOIN sizes s, colors c, styles st (active = true)
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @param condition the join condition to apply.
     * @throws IllegalArgumentException if joinEntities is null or empty
     */
    public CrossJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntities, condition);
    }
}