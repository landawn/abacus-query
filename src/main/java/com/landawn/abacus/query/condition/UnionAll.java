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
 * <p>Key differences from UNION:</p>
 * <ul>
 *   <li>UNION removes duplicate rows, UNION ALL keeps all rows</li>
 *   <li>UNION ALL is generally faster as it doesn't perform duplicate elimination</li>
 *   <li>Use UNION ALL when you want all results or know there are no duplicates</li>
 * </ul>
 * 
 * <p>Example usage:</p>
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
 */
public class UnionAll extends Clause {

    // For Kryo
    UnionAll() {
    }

    /**
     * Constructs a UNION ALL clause with the specified subquery.
     * 
     * <p>The subquery should return the same number and compatible types of columns
     * as the main query to ensure valid SQL.</p>
     *
     * @param condition the subquery to combine with UNION ALL
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Combine orders from multiple regions
     * SubQuery eastRegion = new SubQuery("SELECT * FROM orders WHERE region = 'EAST'");
     * SubQuery westRegion = new SubQuery("SELECT * FROM orders WHERE region = 'WEST'");
     * UnionAll allOrders = new UnionAll(westRegion);
     * // Keeps all orders, even if some might appear in both regions
     * }</pre>
     */
    public UnionAll(final SubQuery condition) {
        super(Operator.UNION_ALL, condition);
    }
}