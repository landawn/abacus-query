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
 * <p>Note: CROSS JOIN typically doesn't use a join condition (ON clause),
 * but this implementation allows it for flexibility.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple CROSS JOIN
 * CrossJoin join = new CrossJoin("products");
 * // Results in: CROSS JOIN products
 * 
 * // CROSS JOIN with condition (unusual but supported)
 * CrossJoin join = new CrossJoin("products", CF.eq("category_id", 1));
 * 
 * // CROSS JOIN multiple tables
 * CrossJoin join = new CrossJoin(Arrays.asList("products", "categories"), 
 *                                CF.eq("active", true));
 * }</pre>
 * 
 * @see Join
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see InnerJoin
 */
public class CrossJoin extends Join {

    // For Kryo
    CrossJoin() {
    }

    /**
     * Creates a new CROSS JOIN with the specified table/entity.
     * This creates a simple CROSS JOIN without any condition.
     * 
     * @param joinEntity the table or entity name to join
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CrossJoin join = new CrossJoin("colors");
     * // SQL: CROSS JOIN colors
     * }</pre>
     */
    public CrossJoin(final String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Creates a new CROSS JOIN with the specified table/entity and condition.
     * While CROSS JOINs typically don't have conditions, this allows for non-standard usage.
     * 
     * @param joinEntity the table or entity name to join
     * @param condition the join condition (optional for CROSS JOIN)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Unusual but supported: CROSS JOIN with condition
     * CrossJoin join = new CrossJoin("products", CF.eq("available", true));
     * }</pre>
     */
    public CrossJoin(final String joinEntity, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntity, condition);
    }

    /**
     * Creates a new CROSS JOIN with multiple tables/entities and a condition.
     * This allows joining multiple tables in a single CROSS JOIN operation.
     * 
     * @param joinEntities the collection of table or entity names to join
     * @param condition the join condition (optional for CROSS JOIN)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> tables = Arrays.asList("sizes", "colors", "styles");
     * CrossJoin join = new CrossJoin(tables, CF.eq("active", true));
     * // Creates a cross join of all three tables
     * }</pre>
     */
    public CrossJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.CROSS_JOIN, joinEntities, condition);
    }
}