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
 * The USING clause provides a concise way to join tables when they share columns with identical names.
 * It automatically performs an equi-join on the specified columns and eliminates duplicate columns
 * from the result set, unlike the ON clause.
 * 
 * <p>Key advantages of USING over ON:
 * <ul>
 *   <li>Cleaner, more readable syntax for common column names</li>
 *   <li>Automatically removes duplicate join columns from the result</li>
 *   <li>Reduces redundancy when joining on identically named columns</li>
 *   <li>Particularly useful for natural key joins and standardized schemas</li>
 * </ul>
 * 
 * <p>Limitations:
 * <ul>
 *   <li>Can only be used when column names are identical in both tables</li>
 *   <li>Cannot specify table qualifiers with column names</li>
 *   <li>Less flexible than ON for complex join conditions</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Single column join - joining employees and departments on department_id
 * Using using1 = new Using("department_id");
 * // Results in: JOIN departments USING (department_id)
 * // Equivalent to: JOIN departments ON employees.department_id = departments.department_id
 * // But returns only one department_id column instead of two
 * 
 * // Multiple column join - composite key join
 * Using using2 = new Using("company_id", "branch_id");
 * // Results in: JOIN branches USING (company_id, branch_id)
 * 
 * // Using collection for dynamic column lists
 * Set<String> commonColumns = new HashSet<>(Arrays.asList("tenant_id", "workspace_id"));
 * Using using3 = new Using(commonColumns);
 * // Results in: JOIN workspaces USING (tenant_id, workspace_id)
 * }</pre>
 * 
 * @see On
 * @see Join
 */
public class Using extends Cell {

    // For Kryo
    Using() {
    }

    /**
     * Constructs a USING clause with the specified column names.
     * The columns must exist with identical names in both tables being joined.
     * The join will match rows where all specified columns have equal values.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Simple join on employee_id
     * Using using = new Using("employee_id");
     * // In query: SELECT * FROM orders JOIN employees USING (employee_id)
     * 
     * // Composite key join
     * Using multiColumn = new Using("company_id", "department_id", "team_id");
     * // In query: SELECT * FROM projects JOIN assignments USING (company_id, department_id, team_id)
     * }</pre>
     *
     * @param columnNames variable number of column names to join on. 
     *                    All columns must exist in both tables with identical names.
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    public Using(final String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Constructs a USING clause with a collection of column names.
     * This constructor is useful when column names are determined dynamically
     * or retrieved from metadata/configuration.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Dynamic column list from metadata
     * List<String> sharedColumns = metadata.getSharedColumns("orders", "customers");
     * Using using = new Using(sharedColumns);
     * 
     * // Multi-tenant join pattern
     * Set<String> tenantColumns = new LinkedHashSet<>();
     * tenantColumns.add("tenant_id");
     * tenantColumns.add("organization_id");
     * Using tenantUsing = new Using(tenantColumns);
     * // Results in: USING (tenant_id, organization_id)
     * }</pre>
     *
     * @param columnNames collection of column names to join on.
     *                    Order matters for some databases.
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    public Using(final Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates a USING condition from an array of column names.
     * This static factory method constructs the appropriate condition expression
     * for the USING clause from the provided column names.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Condition usingCondition = Using.createUsingCondition("customer_id", "order_date");
     * // Creates condition for: USING (customer_id, order_date)
     * }</pre>
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
     * This static factory method constructs the appropriate condition expression
     * for the USING clause from the provided column collection.
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