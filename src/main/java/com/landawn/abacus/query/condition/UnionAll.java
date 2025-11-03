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
     * Creates a new UNION ALL clause with the specified subquery.
     * The UNION ALL operation will combine results from the main query and this subquery,
     * keeping all rows including duplicates. This provides better performance than UNION
     * since it skips the duplicate elimination step.
     *
     * <p>The subquery must return the same number of columns as the main query,
     * and the data types must be compatible. The column names from the first query
     * in the UNION ALL operation will be used for the final result set.</p>
     *
     * <p>Use UNION ALL when you know there are no duplicates in the combined result,
     * or when you want to preserve all rows regardless of duplication. This is
     * significantly faster than UNION for large datasets.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Combine orders from multiple regions
     * SubQuery eastOrders = new SubQuery("SELECT order_id, amount FROM orders WHERE region = 'EAST'");
     * UnionAll allOrders = new UnionAll(eastOrders);
     * // When combined with West region query:
     * // SELECT order_id, amount FROM orders WHERE region = 'WEST'
     * // UNION ALL
     * // SELECT order_id, amount FROM orders WHERE region = 'EAST'
     * // Keeps all orders, including any duplicates
     *
     * // Merge current and archived transactions
     * SubQuery archivedTxns = new SubQuery("SELECT txn_id, date, amount FROM archived_transactions");
     * UnionAll allTxns = new UnionAll(archivedTxns);
     * // Combines with current transactions, preserving all records
     *
     * // Combine data from partitioned tables
     * SubQuery q1Data = new SubQuery("SELECT * FROM sales_q1");
     * SubQuery q2Data = new SubQuery("SELECT * FROM sales_q2");
     * UnionAll allSales = new UnionAll(q2Data);
     * // Efficiently combines quarterly data without duplicate check
     * }</pre>
     *
     * @param condition the subquery to combine with UNION ALL. Must not be null. The subquery
     *                  must have the same number of columns with compatible types as the main query.
     * @throws IllegalArgumentException if condition is null
     * @see Union
     * @see Intersect
     */
    public UnionAll(final SubQuery condition) {
        super(Operator.UNION_ALL, condition);
    }
}