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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

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
 * <p><b>Deprecation status:</b> both public {@code Using} constructors (and the corresponding
 * {@code Filters.using(...)} factory methods) are deprecated. Prefer an {@link On} condition created via
 * {@link Filters#on(java.util.Map)}, which spells out fully qualified column pairs and is more portable.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single column join - joining employees and departments on department_id
 * Using using1 = new Using("department_id");
 * InnerJoin join1 = new InnerJoin("departments", using1);
 * // SQL: INNER JOIN departments USING (department_id)
 * // Equivalent to: INNER JOIN departments ON employees.department_id = departments.department_id
 * // But returns only one department_id column instead of two
 *
 * // Multiple column join - composite key join
 * Using using2 = new Using("company_id", "branch_id");
 * LeftJoin join2 = new LeftJoin("branches", using2);
 * // SQL: LEFT JOIN branches USING (company_id, branch_id)
 * // Equivalent to: LEFT JOIN branches ON companies.company_id = branches.company_id
 * //                                   AND companies.branch_id = branches.branch_id
 *
 * // Using collection for dynamic column lists
 * Set<String> commonColumns = new LinkedHashSet<>(Arrays.asList("tenant_id", "workspace_id"));
 * Using using3 = new Using(commonColumns);
 * RightJoin join3 = new RightJoin("workspaces", using3);
 * // SQL: RIGHT JOIN workspaces USING (tenant_id, workspace_id)
 *
 * // Triple column join
 * Using using4 = new Using("org_id", "dept_id", "team_id");
 * InnerJoin join4 = new InnerJoin("assignments", using4);
 * // SQL: INNER JOIN assignments USING (org_id, dept_id, team_id)
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
     * The validated, unqualified column names this USING clause joins on. Stored separately from the
     * wrapped {@link Expression} so the original column list is recoverable via {@link #getColumnNames()}.
     * May be {@code null} for uninitialized instances produced by the package-private default constructor
     * (e.g., during Kryo deserialization). Not part of {@link #equals(Object)}/{@link #hashCode()}, which
     * already account for these columns through the wrapped condition.
     */
    private List<String> columnNames;

    /** Lazily memoized immutable view returned by {@link #getColumnNames()} (performance only). */
    private transient ImmutableList<String> cachedColumnNamesView;

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
     * // SQL: INNER JOIN employees USING (employee_id)
     * // In query: SELECT * FROM orders INNER JOIN employees USING (employee_id)
     *
     * // Composite key join with three columns
     * Using multiColumn = new Using("company_id", "department_id", "team_id");
     * LeftJoin leftJoin = new LeftJoin("assignments", multiColumn);
     * // SQL: LEFT JOIN assignments USING (company_id, department_id, team_id)
     * // In query: SELECT * FROM projects LEFT JOIN assignments USING (company_id, department_id, team_id)
     *
     * // Two-column join
     * Using twoCol = new Using("user_id", "account_id");
     * RightJoin rightJoin = new RightJoin("accounts", twoCol);
     * // SQL: RIGHT JOIN accounts USING (user_id, account_id)
     * }</pre>
     *
     * @param columnNames variable number of column names to join on.
     *                    All columns must exist in both tables with identical names. Must not be {@code null} or empty,
     *                    and individual names must not be {@code null}, empty, or blank. Names must be unqualified (cannot contain a {@code .}).
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank entry,
     *                                  or contains a qualified (dotted) column name
     * @deprecated It's recommended to use {@link Filters#on(java.util.Map)} instead of {@code Using} for better
     *             portability and clarity. Replace {@code new Using("col1", "col2")} with explicit
     *             {@code Filters.on(N.asMap("table1.col1", "table2.col1", "table1.col2", "table2.col2"))}.
     */
    @Deprecated
    public Using(final String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));

        this.columnNames = copyColumnNames(columnNames);
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
     * // SQL: INNER JOIN customers USING (customer_id, order_date)
     *
     * // Multi-tenant join pattern
     * Set<String> tenantColumns = new LinkedHashSet<>();
     * tenantColumns.add("tenant_id");
     * tenantColumns.add("organization_id");
     * Using tenantUsing = new Using(tenantColumns);
     * LeftJoin leftJoin = new LeftJoin("organizations", tenantUsing);
     * // SQL: LEFT JOIN organizations USING (tenant_id, organization_id)
     *
     * // List-based column specification
     * List<String> joinCols = Arrays.asList("region_id", "country_id", "state_id");
     * Using locationUsing = new Using(joinCols);
     * RightJoin rightJoin = new RightJoin("locations", locationUsing);
     * // SQL: RIGHT JOIN locations USING (region_id, country_id, state_id)
     * }</pre>
     *
     * @param columnNames collection of column names to join on. Must not be {@code null} or empty, and individual
     *                    names must not be {@code null}, empty, or blank. Names must be unqualified (cannot contain a {@code .}).
     *                    Order matters for some databases; use a {@code LinkedHashSet} or {@code List} to preserve insertion order.
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank entry,
     *                                  or contains a qualified (dotted) column name
     * @deprecated It's recommended to use {@link Filters#on(java.util.Map)} instead of {@code Using} for better
     *             portability and clarity. Replace {@code new Using(columnList)} with an explicit
     *             {@code Filters.on(Map)} condition that specifies the full column names with table prefixes.
     */
    @Deprecated
    public Using(final Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));

        this.columnNames = copyColumnNames(columnNames);
    }

    /**
     * Gets the validated column names this USING clause joins on, in the order they were supplied.
     * The returned list contains the unqualified column names (no table prefixes) that were passed to
     * the constructor, after validation. This is a convenient structured alternative to inspecting the
     * rendered {@code USING (...)} expression returned by {@link #getCondition()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Using using = new Using("company_id", "branch_id");
     * List<String> cols = using.getColumnNames();
     * // cols = ["company_id", "branch_id"]
     *
     * // Edge: the returned list is immutable
     * cols.add("extra");   // throws UnsupportedOperationException
     *
     * // Edge: an uninitialized instance (created only via the package-private default
     * // constructor, e.g. during Kryo deserialization) returns an empty list
     * }</pre>
     *
     * @return an immutable list of the unqualified column names in supplied order, or an empty immutable
     *         list for an uninitialized instance produced by the package-private default constructor
     */
    public ImmutableList<String> getColumnNames() {
        if (columnNames == null) {
            return ImmutableList.empty();
        }

        // Memoized like Join.getJoinEntities(); the underlying list never changes after construction.
        ImmutableList<String> view = cachedColumnNamesView;

        if (view == null) {
            view = ImmutableList.wrap(columnNames);
            cachedColumnNamesView = view;
        }

        return view;
    }

    private static List<String> copyColumnNames(final String... columnNames) {
        final List<String> copy = new ArrayList<>(columnNames.length);

        for (final String columnName : columnNames) {
            copy.add(columnName);
        }

        return copy;
    }

    private static List<String> copyColumnNames(final Collection<String> columnNames) {
        return new ArrayList<>(columnNames);
    }

    /**
     * Creates a condition expression for the USING clause from an array of column names.
     * This static factory method is used internally (package-private helper) and constructs
     * the appropriate condition expression for the USING clause from the provided column names.
     *
     * <p><b>Internal Usage Example:</b></p>
     * <pre>{@code
     * // Internal helper — not part of the public API
     * // Single column - always parenthesized
     * Condition singleCol = Using.createUsingCondition("customer_id");
     * // Creates condition for: USING (customer_id)
     *
     * // Multiple columns
     * Condition multiCol = Using.createUsingCondition("customer_id", "order_date");
     * // Creates condition for: USING (customer_id, order_date)
     * }</pre>
     *
     * @param columnNames array of column names. Must not be null or empty, and names must not be blank or qualified (cannot contain a {@code .}).
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank entry,
     *                                  or contains a qualified (dotted) column name
     */
    static Condition createUsingCondition(final String... columnNames) {
        N.checkArgNotEmpty(columnNames, "columnNames");

        for (final String columnName : columnNames) {
            validateColumnName(columnName);
        }

        return Filters.expr(parenthesizeColumnNames(concatPropNames(columnNames)));
    }

    /**
     * Creates a condition expression for the USING clause from a collection of column names.
     * This static factory method is used internally (package-private helper) and constructs
     * the appropriate condition expression for the USING clause from the provided column collection.
     *
     * <p><b>Internal Usage Example:</b></p>
     * <pre>{@code
     * // Internal helper — not part of the public API
     * // Single column collection - always parenthesized
     * List<String> singleCol = Collections.singletonList("tenant_id");
     * Condition singleCondition = Using.createUsingCondition(singleCol);
     * // Creates condition for: USING (tenant_id)
     *
     * // Multiple columns collection
     * List<String> multiCols = Arrays.asList("tenant_id", "user_id");
     * Condition multiCondition = Using.createUsingCondition(multiCols);
     * // Creates condition for: USING (tenant_id, user_id)
     * }</pre>
     *
     * @param columnNames collection of column names. Must not be null or empty, and names must not be blank or qualified (cannot contain a {@code .}).
     * @return a condition representing the USING clause
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank entry,
     *                                  or contains a qualified (dotted) column name
     */
    static Condition createUsingCondition(final Collection<String> columnNames) {
        N.checkArgNotEmpty(columnNames, "columnNames");

        for (final String columnName : columnNames) {
            validateColumnName(columnName);
        }

        return Filters.expr(parenthesizeColumnNames(concatPropNames(columnNames)));
    }

    private static void validateColumnName(final String columnName) {
        if (Strings.isBlank(columnName)) {
            throw new IllegalArgumentException("columnName in columnNames must not be null, empty, or blank");
        }

        if (columnName.indexOf('.') >= 0) {
            throw new IllegalArgumentException("USING column names must be unqualified: " + columnName);
        }
    }

    private static String parenthesizeColumnNames(final String columnNamesExpr) {
        if (columnNamesExpr.startsWith("(") && columnNamesExpr.endsWith(")")) {
            return columnNamesExpr;
        }

        return "(" + columnNamesExpr + ")";
    }
}
