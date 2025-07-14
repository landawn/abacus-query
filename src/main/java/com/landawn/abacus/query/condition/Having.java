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
 * Represents a HAVING clause in SQL queries.
 * The HAVING clause is used to filter grouped results based on conditions applied to aggregated data.
 * It is typically used with GROUP BY clauses to filter groups based on aggregate functions.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a HAVING clause to filter groups where COUNT(*) > 5
 * Having having = new Having(CF.gt("COUNT(*)", 5));
 * 
 * // Use in a query with GROUP BY
 * query.groupBy("department")
 *      .having(new Having(CF.gt("AVG(salary)", 50000)));
 * }</pre>
 * 
 * @see Clause
 * @see Condition
 * @see GroupBy
 */
public class Having extends Clause {
    // For Kryo
    Having() {
    }

    /**
     * Creates a new HAVING clause with the specified condition.
     * 
     * @param condition the condition to apply in the HAVING clause
     * @throws NullPointerException if condition is null
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Filter groups where the sum of sales is greater than 10000
     * Having having = new Having(CF.gt("SUM(sales)", 10000));
     * }</pre>
     */
    public Having(final Condition condition) {
        super(Operator.HAVING, condition);
    }
}