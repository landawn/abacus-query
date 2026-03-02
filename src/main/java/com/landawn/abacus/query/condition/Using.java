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

import com.landawn.abacus.query.Filters;
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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single column join - joining employees and departments on department_id
 * Using using1 = new Using("department_id");
 * InnerJoin join1 = new InnerJoin("departments", using1);
 * // Generates: INNER JOIN departments USING department_id
 * // Equivalent to: INNER JOIN departments ON employees.department_id = departments.department_id
 * // But returns only one department_id column instead of two
 *
 * // Multiple column join - composite key join
 * Using using2 = new Using("company_id", "branch_id");
 * LeftJoin join2 = new LeftJoin("branches", using2);
 * // Generates: LEFT JOIN branches USING (company_id, branch_id)
 * // Equivalent to: LEFT JOIN branches ON companies.company_id = branches.company_id
 * //                                   AND companies.branch_id = branches.branch_id
 *
 * // Using collection for dynamic column lists
 * Set<String> commonColumns = new LinkedHashSet<>(Arrays.asList("tenant_id", "workspace_id"));
 * Using using3 = new Using(commonColumns);
 * RightJoin join3 = new RightJoin("workspaces", using3);
 * // Generates: RIGHT JOIN workspaces USING (tenant_id, workspace_id)
 *
 * // Triple column join
 * Using using4 = new Using("org_id", "dept_id", "team_id");
 * InnerJoin join4 = new InnerJoin("assignments", using4);
 * // Generates: INNER JOIN assignments USING (org_id, dept_id, team_id)
 * }</pre>
 * 
 * @see On
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see Cell
 */
public class Using extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Using instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Using() {
    }

    /**
     * Creates a USING clause with the specified column names.
     * The columns must exist with identical names in both tables being joined.
     * The join will match rows where all specified columns have equal values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple join on employee_id
     * Using using = new Using("employee_id");
     * InnerJoin join = new InnerJoin("employees", using);
     * // Generates: INNER JOIN employees USING employee_id
     * // In query: SELECT * FROM orders INNER JOIN employees USING employee_id
     *
     * // Composite key join with three columns
     * Using multiColumn = new Using("company_id", "department_id", "team_id");
     * LeftJoin leftJoin = new LeftJoin("assignments", multiColumn);
     * // Generates: LEFT JOIN assignments USING (company_id, department_id, team_id)
     * // In query: SELECT * FROM projects LEFT JOIN assignments USING (company_id, department_id, team_id)
     *
     * // Two-column join
     * Using twoCol = new Using("user_id", "account_id");
     * RightJoin rightJoin = new RightJoin("accounts", twoCol);
     * // Generates: RIGHT JOIN accounts USING (user_id, account_id)
     * }</pre>
     *
     * @param columnNames variable number of column names to join on.
     *                    All columns must exist in both tables with identical names. Must not be null or empty.
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    public Using(final String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates a USING clause with a collection of column names.
     * This constructor is useful when column names are determined dynamically
     * or retrieved from metadata/configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Dynamic column list from metadata (multiple columns)
     * List<String> sharedColumns = Arrays.asList("customer_id", "order_date");
     * Using using = new Using(sharedColumns);
     * InnerJoin join = new InnerJoin("customers", using);
     * // Generates: INNER JOIN customers USING (customer_id, order_date)
     *
     * // Multi-tenant join pattern
     * Set<String> tenantColumns = new LinkedHashSet<>();
     * tenantColumns.add("tenant_id");
     * tenantColumns.add("organization_id");
     * Using tenantUsing = new Using(tenantColumns);
     * LeftJoin leftJoin = new LeftJoin("organizations", tenantUsing);
     * // Generates: LEFT JOIN organizations USING (tenant_id, organization_id)
     *
     * // List-based column specification
     * List<String> joinCols = Arrays.asList("region_id", "country_id", "state_id");
     * Using locationUsing = new Using(joinCols);
     * RightJoin rightJoin = new RightJoin("locations", locationUsing);
     * // Generates: RIGHT JOIN locations USING (region_id, country_id, state_id)
     * }</pre>
     *
     * @param columnNames collection of column names to join on. Must not be null or empty.
     *                    Order matters for some databases. Use LinkedHashSet or List to preserve order.
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    public Using(final Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates a condition expression for the USING clause from an array of column names.
     * This static factory method constructs the appropriate condition expression
     * for the USING clause from the provided column names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single column - no parentheses in output
     * Condition singleCol = Using.createUsingCondition("customer_id");
     * // Creates condition for: USING customer_id
     *
     * // Multiple columns - parentheses in output
     * Condition multiCol = Using.createUsingCondition("customer_id", "order_date");
     * // Creates condition for: USING (customer_id, order_date)
     * }</pre>
     *
     * @param columnNames array of column names. Must not be null or empty.
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    static Condition createUsingCondition(final String... columnNames) {
        N.checkArgNotEmpty(columnNames, "columnNames");

        for (final String columnName : columnNames) {
            N.checkArgNotEmpty(columnName, "columnName in columnNames");
        }

        return Filters.expr(concatPropNames(columnNames));
    }

    /**
     * Creates a condition expression for the USING clause from a collection of column names.
     * This static factory method constructs the appropriate condition expression
     * for the USING clause from the provided column collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single column collection - no parentheses in output
     * List<String> singleCol = Collections.singletonList("tenant_id");
     * Condition singleCondition = Using.createUsingCondition(singleCol);
     * // Creates condition for: USING tenant_id
     *
     * // Multiple columns collection - parentheses in output
     * List<String> multiCols = Arrays.asList("tenant_id", "user_id");
     * Condition multiCondition = Using.createUsingCondition(multiCols);
     * // Creates condition for: USING (tenant_id, user_id)
     * }</pre>
     *
     * @param columnNames collection of column names. Must not be null or empty.
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if columnNames is null or empty
     */
    static Condition createUsingCondition(final Collection<String> columnNames) {
        N.checkArgNotEmpty(columnNames, "columnNames");

        for (final String columnName : columnNames) {
            N.checkArgNotEmpty(columnName, "columnName in columnNames");
        }

        return Filters.expr(concatPropNames(columnNames));
    }
}
