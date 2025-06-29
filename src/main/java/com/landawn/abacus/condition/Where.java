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

package com.landawn.abacus.condition;

/**
 * Represents a WHERE clause in SQL queries.
 * This class is used to specify conditions that filter records in a query result.
 * 
 * <p>The WHERE clause is used to extract only those records that fulfill a specified condition.
 * It can contain simple conditions or complex combinations of conditions using AND, OR, and other operators.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple condition
 * Condition condition = new Equal("status", "active");
 * Where where = new Where(condition);
 * // Results in: WHERE status = 'active'
 * 
 * // Complex condition
 * And and = new And(new Equal("age", 25), new GreaterThan("salary", 50000));
 * Where where2 = new Where(and);
 * // Results in: WHERE age = 25 AND salary > 50000
 * }</pre>
 */
public class Where extends Clause {

    // For Kryo
    Where() {
    }

    /**
     * Constructs a WHERE clause with the specified condition.
     * 
     * @param condition the condition to be used in the WHERE clause
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = new Like("name", "%John%");
     * Where where = new Where(condition);
     * // Results in: WHERE name LIKE '%John%'
     * }</pre>
     */
    public Where(final Condition condition) {
        super(Operator.WHERE, condition);
    }
}