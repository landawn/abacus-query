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
 * Represents a UNION clause in SQL queries.
 * This class is used to combine the results of two or more SELECT statements,
 * removing duplicate rows from the result set.
 * 
 * <p>The UNION operator selects only distinct values by default. To allow
 * duplicate values, use {@link UnionAll} instead.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * SubQuery query1 = new SubQuery("SELECT id, name FROM customers WHERE city='NY'");
 * SubQuery query2 = new SubQuery("SELECT id, name FROM customers WHERE city='LA'");
 * Union union = new Union(query2);
 * // Results in: SELECT id, name FROM customers WHERE city='NY' UNION SELECT id, name FROM customers WHERE city='LA'
 * }</pre>
 */
public class Union extends Clause {

    // For Kryo
    Union() {
    }

    /**
     * Constructs a UNION clause with the specified subquery.
     *
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery activeUsers = new SubQuery("SELECT user_id FROM active_users");
     * Union union = new Union(activeUsers);
     * }</pre>
     *
     * @param condition the subquery to be combined using UNION
     */
    public Union(final SubQuery condition) {
        super(Operator.UNION, condition);
    }
}