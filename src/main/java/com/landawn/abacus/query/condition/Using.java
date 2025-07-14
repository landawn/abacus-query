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

import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;

/**
 * Represents a USING clause in SQL JOIN operations.
 * 
 * <p>The USING clause is shorthand for joining tables when the join column names
 * are identical in both tables. It automatically performs an equi-join on the specified 
 * columns and eliminates duplicate columns in the result set.</p>
 * 
 * <p>Key benefits of USING:</p>
 * <ul>
 *   <li>Cleaner syntax when column names match</li>
 *   <li>Automatically removes duplicate columns from the result</li>
 *   <li>More concise than ON clause for matching column names</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Single column join
 * Using using1 = new Using("department_id");
 * // Results in: JOIN table USING (department_id)
 * // Equivalent to: JOIN table ON t1.department_id = t2.department_id
 * 
 * // Multiple column join
 * Using using2 = new Using("company_id", "department_id");
 * // Results in: JOIN table USING (company_id, department_id)
 * 
 * // Using collection
 * List<String> joinColumns = Arrays.asList("customer_id", "order_date");
 * Using using3 = new Using(joinColumns);
 * // Results in: JOIN table USING (customer_id, order_date)
 * }</pre>
 */
public class Using extends Cell {

    // For Kryo
    Using() {
    }

    /**
     * Constructs a USING clause with the specified column names.
     * 
     * <p>The columns specified must exist with the same names in both tables
     * being joined.</p>
     *
     * @param columnNames variable number of column names to join on
     * @throws IllegalArgumentException if columnNames is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Join on single column
     * Using using = new Using("employee_id");
     * 
     * // Join on multiple columns
     * Using using2 = new Using("branch_id", "department_id");
     * 
     * // In a join operation:
     * // SELECT * FROM employees JOIN departments USING (department_id)
     * }</pre>
     */
    public Using(final String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Constructs a USING clause with a collection of column names.
     * 
     * <p>Useful when the column names are dynamically determined or come from
     * another data structure.</p>
     *
     * @param columnNames collection of column names to join on
     * @throws IllegalArgumentException if columnNames is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Set<String> commonColumns = new HashSet<>();
     * commonColumns.add("tenant_id");
     * commonColumns.add("workspace_id");
     * Using using = new Using(commonColumns);
     * 
     * // Results in: USING (tenant_id, workspace_id)
     * }</pre>
     */
    public Using(final Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates a USING condition from an array of column names.
     *
     * @param columnNames array of column names
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    static Condition createUsingCondition(final String... columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames can't be null or empty");
        }

        return CF.expr(concatPropNames(columnNames));
    }

    /**
     * Creates a USING condition from a collection of column names.
     *
     * @param columnNames collection of column names
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    static Condition createUsingCondition(final Collection<String> columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames " + columnNames + " must has one or more than one column name. ");
        }

        return CF.expr(concatPropNames(columnNames));
    }
}