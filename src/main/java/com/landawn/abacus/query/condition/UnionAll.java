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
 * Represents a UNION ALL clause in SQL queries.
 * 
 * <p>The UNION ALL operator is used to combine the result sets of two or more SELECT statements.
 * Unlike UNION, UNION ALL includes all rows from all queries, including duplicates.
 * This makes UNION ALL faster than UNION as it doesn't need to remove duplicates.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Preserves all rows from all queries, including duplicates</li>
 *   <li>Faster performance than UNION (no duplicate elimination)</li>
 *   <li>Maintains the order of rows from each query</li>
 *   <li>Requires same number of columns with compatible types</li>
 *   <li>Column names from the first query are used in the result</li>
 * </ul>
 * 
 * <p>When to use UNION ALL vs UNION:</p>
 * <ul>
 *   <li>Use UNION ALL when you want all results or know there are no duplicates</li>
 *   <li>Use UNION ALL for better performance when duplicates don't matter</li>
 *   <li>Use UNION ALL when combining data from different sources that won't overlap</li>
 *   <li>Use UNION when you need distinct results</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get all transactions from both tables, including duplicates
 * SubQuery currentTransactions = new SubQuery("SELECT * FROM transactions WHERE year = 2024");
 * SubQuery archivedTransactions = new SubQuery("SELECT * FROM archived_transactions WHERE year = 2024");
 * UnionAll unionAll = new UnionAll(archivedTransactions);
 * // Results in: SELECT * FROM transactions WHERE year = 2024 
 * //             UNION ALL 
 * //             SELECT * FROM archived_transactions WHERE year = 2024
 * 
 * // Combine active and inactive records
 * SubQuery activeUsers = new SubQuery("SELECT id, name, 'active' as status FROM active_users");
 * SubQuery inactiveUsers = new SubQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
 * UnionAll allUsers = new UnionAll(inactiveUsers);
 * }</pre>
 * 
 * @see Union
 * @see Intersect
 * @see Except
 * @see Minus
 * @see SubQuery
 * @see Clause
 */
public class UnionAll extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized UnionAll instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    UnionAll() {
    }

    /**
     * Constructs a UNION ALL clause with the specified subquery.
     * The subquery will be combined with the main query, keeping all rows including duplicates.
     * 
     * <p>The subquery must return the same number of columns as the main query,
     * and the data types must be compatible. The column names from the first query
     * in the UNION ALL operation will be used for the final result set.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Combine orders from multiple regions
     * SubQuery eastRegion = new SubQuery("SELECT * FROM orders WHERE region = 'EAST'");
     * SubQuery westRegion = new SubQuery("SELECT * FROM orders WHERE region = 'WEST'");
     * UnionAll allOrders = new UnionAll(westRegion);
     * // Keeps all orders, even if some might appear in both regions
     * }</pre>
     *
     * @param condition the subquery to combine with UNION ALL
     * @throws IllegalArgumentException if condition is null
     */
    public UnionAll(final SubQuery condition) {
        super(Operator.UNION_ALL, condition);
    }
}