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

    // For Kryo
    CrossJoin() {
    }

    /**
     * Creates a new CROSS JOIN with the specified table/entity.
     * This creates a simple CROSS JOIN without any condition, producing
     * a Cartesian product of the tables.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Create all combinations of products and colors
     * CrossJoin join = new CrossJoin("colors");
     * // SQL: CROSS JOIN colors
     * // If products has 10 rows and colors has 5 rows, result has 50 rows
     * 
     * // Generate all possible date-time slot combinations
     * CrossJoin timeSlots = new CrossJoin("available_times");
     * // Each date will be combined with each available time
     * }</pre>
     * 
     * @param joinEntity the table or entity name to join
     */
    public CrossJoin(final String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Creates a new CROSS JOIN with the specified table/entity and condition.
     * While CROSS JOINs typically don't have conditions, this allows for non-standard usage.
     * Adding a condition makes it functionally equivalent to an INNER JOIN.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // CROSS JOIN with filter (unusual usage)
     * CrossJoin join = new CrossJoin("products", CF.eq("available", true));
     * // Only crosses with available products
     * 
     * // More typical approach would be:
     * // 1. Use CROSS JOIN without condition for Cartesian product
     * // 2. Add WHERE clause to filter results
     * CrossJoin pure = new CrossJoin("products");
     * // Then: WHERE available = true
     * }</pre>
     * 
     * @param joinEntity the table or entity name to join
     * @param condition the join condition (optional for CROSS JOIN)
     */
    public CrossJoin(final String joinEntity, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntity, condition);
    }

    /**
     * Creates a new CROSS JOIN with multiple tables/entities and a condition.
     * This allows creating Cartesian products of multiple tables in a single operation.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Create all combinations of sizes, colors, and styles
     * List<String> tables = Arrays.asList("sizes", "colors", "styles");
     * CrossJoin join = new CrossJoin(tables, CF.eq("active", true));
     * // Creates a cross join of all three tables where active = true
     * 
     * // Generate test data combinations
     * Collection<String> testTables = Arrays.asList("test_users", "test_permissions");
     * CrossJoin testData = new CrossJoin(testTables, null);
     * // Every test user gets every test permission
     * }</pre>
     * 
     * @param joinEntities the collection of table or entity names to join
     * @param condition the join condition (optional for CROSS JOIN)
     */
    public CrossJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntities, condition);
    }
}