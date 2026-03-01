/*
 * Copyright (C) 2018 HaiYang Li
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
package com.landawn.abacus.query;

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * A fluent builder for creating dynamic SQL queries programmatically.
 * This builder provides a type-safe and intuitive way to construct SQL SELECT statements
 * with support for joins, conditions, grouping, ordering, and set operations.
 * 
 * <p>The builder follows a fluent interface pattern where each method returns the builder
 * instance, allowing method chaining. The SQL components are built in a natural order:
 * SELECT → FROM → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT/OFFSET.</p>
 * 
 * <p><b>Important:</b> Always call {@link #build()} to generate the final SQL string and 
 * release resources. The builder uses object pooling internally for performance optimization.</p>
 * 
 * <h2>Example usage:</h2>
 * <pre>{@code
 * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
 * builder.select().append("id", "user_id").append("name");
 * builder.from().append("users", "u");
 * builder.where().append("u.active = ?").and("u.age > ?");
 * builder.orderBy().append("u.name ASC");
 * builder.limit(10);
 * String sql = builder.build();
 * // Result: "SELECT id AS user_id, name FROM users u WHERE u.active = ? AND u.age > ? ORDER BY u.name ASC LIMIT 10"
 * }</pre>
 */
@SuppressWarnings("java:S1192")
public class DynamicSQLBuilder {

    static final Logger logger = LoggerFactory.getLogger(DynamicSQLBuilder.class);

    private SelectClause selectClause = new SelectClause(Objectory.createStringBuilder());

    private FromClause fromClause = new FromClause(Objectory.createStringBuilder());

    private WhereClause whereClause;

    private GroupByClause groupByClause;

    private HavingClause havingClause;

    private OrderByClause orderByClause;

    private StringBuilder moreParts = null;

    private DynamicSQLBuilder() {

    }

    /**
     * Creates a new instance of DynamicSQLBuilder.
     * This is the entry point for building dynamic SQL queries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * }</pre>
     *
     * @return a new DynamicSQLBuilder instance for method chaining
     */
    public static DynamicSQLBuilder create() {
        return new DynamicSQLBuilder();
    }

    /**
     * Returns the SELECT clause builder for defining columns to retrieve.
     * Multiple calls to this method return the same Select instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.select().append("id").append("name", "user_name");
     * // Generates: SELECT id, name AS user_name
     * }</pre>
     *
     * @return the Select clause builder for method chaining
     */
    public SelectClause select() {
        return selectClause;
    }

    /**
     * Returns the FROM clause builder for defining tables and joins.
     * Multiple calls to this method return the same From instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
     * // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @return the From clause builder for method chaining
     */
    public FromClause from() {
        return fromClause;
    }

    /**
     * Returns the WHERE clause builder for defining query conditions.
     * Creates a new Where instance on first call and returns the same instance on subsequent calls.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.where().append("status = ?").and("created_date > ?");
     * // Generates: WHERE status = ? AND created_date > ?
     * }</pre>
     *
     * @return the Where clause builder for method chaining
     */
    public WhereClause where() {
        if (whereClause == null) {
            whereClause = new WhereClause(Objectory.createStringBuilder());
        }

        return whereClause;
    }

    /**
     * Returns the GROUP BY clause builder for defining grouping columns.
     * Creates a new GroupBy instance on first call and returns the same instance on subsequent calls.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.groupBy().append("department").append("year");
     * // Generates: GROUP BY department, year
     * }</pre>
     *
     * @return the GroupBy clause builder for method chaining
     */
    public GroupByClause groupBy() {
        if (groupByClause == null) {
            groupByClause = new GroupByClause(Objectory.createStringBuilder());
        }

        return groupByClause;
    }

    /**
     * Returns the HAVING clause builder for defining conditions on grouped results.
     * Creates a new Having instance on first call and returns the same instance on subsequent calls.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.having().append("COUNT(*) > ?").and("SUM(amount) < ?");
     * // Generates: HAVING COUNT(*) > ? AND SUM(amount) < ?
     * }</pre>
     *
     * @return the Having clause builder for method chaining
     */
    public HavingClause having() {
        if (havingClause == null) {
            havingClause = new HavingClause(Objectory.createStringBuilder());
        }

        return havingClause;
    }

    /**
     * Returns the ORDER BY clause builder for defining result ordering.
     * Creates a new OrderBy instance on first call and returns the same instance on subsequent calls.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.orderBy().append("created_date DESC").append("name ASC");
     * // Generates: ORDER BY created_date DESC, name ASC
     * }</pre>
     *
     * @return the OrderBy clause builder for method chaining
     */
    public OrderByClause orderBy() {
        if (orderByClause == null) {
            orderByClause = new OrderByClause(Objectory.createStringBuilder());
        }

        return orderByClause;
    }

    /**
     * Appends a custom LIMIT clause to the SQL query.
     * This method allows for database-specific limit syntax.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.limit("LIMIT 10 OFFSET 20");
     * // Or for custom database syntax
     * builder.limit("TOP 10");
     * }</pre>
     *
     * @param limitCond the complete limit condition including the LIMIT keyword (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder limit(final String limitCond) {
        N.checkArgNotNull(limitCond, "limitCond");

        getStringBuilderForMoreParts().append(" ").append(limitCond);

        return this;
    }

    /**
     * Adds a LIMIT clause to restrict the number of rows returned.
     * Generates standard SQL: {@code LIMIT n}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.select().append("*");
     * builder.from().append("users");
     * builder.limit(10);
     * // Generates: LIMIT 10
     * }</pre>
     *
     * @param count the maximum number of rows to return (must not be negative)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder limit(final int count) {
        N.checkArgNotNegative(count, "count");

        getStringBuilderForMoreParts().append(" LIMIT ").append(count);

        return this;
    }

    /**
     * Adds a LIMIT clause with count and offset for pagination.
     * Generates SQL standard syntax: {@code LIMIT count OFFSET offset}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.select().append("*");
     * builder.from().append("users");
     * builder.limit(10, 20);  // count=10, offset=20
     * // Generates: SELECT * FROM users LIMIT 10 OFFSET 20 (skip 20 rows, return next 10)
     * }</pre>
     *
     * @param count the maximum number of rows to return (must not be negative)
     * @param offset the number of rows to skip (must not be negative)
     * @return this builder instance for method chaining
     * @see #offsetRows(int)
     * @see #fetchNextRows(int)
     * @see #fetchFirstRows(int)
     */
    public DynamicSQLBuilder limit(final int count, final int offset) {
        N.checkArgNotNegative(count, "count");
        N.checkArgNotNegative(offset, "offset");

        getStringBuilderForMoreParts().append(" LIMIT ").append(count).append(" OFFSET ").append(offset);

        return this;
    }

    /**
     * Adds an Oracle-style ROWNUM condition to limit results.
     * Generates: {@code ROWNUM <= n}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.limitByRowNum(10);
     * // Generates: ROWNUM <= 10
     * }</pre>
     *
     * @param count the maximum number of rows to return (must not be negative)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder limitByRowNum(final int count) {
        N.checkArgNotNegative(count, "count");

        final String rowNumCondition = "ROWNUM <= " + count;

        if (whereClause == null || whereClause.sb.isEmpty()) {
            where().append(rowNumCondition);
        } else {
            whereClause.and(rowNumCondition);
        }

        return this;
    }

    /**
     * Adds an OFFSET clause for SQL:2008 standard pagination.
     * Typically used with {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.offsetRows(20).fetchNextRows(10);
     * // Generates: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     *
     * @param offset the number of rows to skip (must not be negative)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder offsetRows(final int offset) {
        N.checkArgNotNegative(offset, "offset");

        getStringBuilderForMoreParts().append(" OFFSET ").append(offset).append(" ROWS");

        return this;
    }

    /**
     * Adds a FETCH NEXT clause for SQL:2008 standard result limiting.
     * Typically used after {@link #offsetRows(int)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.offsetRows(100).fetchNextRows(25);
     * // Generates: OFFSET 100 ROWS FETCH NEXT 25 ROWS ONLY
     * }</pre>
     *
     * @param count the number of rows to fetch (must not be negative)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder fetchNextRows(final int count) {
        N.checkArgNotNegative(count, "count");

        getStringBuilderForMoreParts().append(" FETCH NEXT ").append(count).append(" ROWS ONLY");

        return this;
    }

    /**
     * Adds a FETCH FIRST clause for SQL:2008 standard result limiting.
     * This is an alternative to FETCH NEXT with the same functionality.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.fetchFirstRows(10);
     * // Generates: FETCH FIRST 10 ROWS ONLY
     * }</pre>
     *
     * @param count the number of rows to fetch (must not be negative)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder fetchFirstRows(final int count) {
        N.checkArgNotNegative(count, "count");

        getStringBuilderForMoreParts().append(" FETCH FIRST ").append(count).append(" ROWS ONLY");

        return this;
    }

    private StringBuilder getStringBuilderForMoreParts() {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        return moreParts;
    }

    /**
     * Adds a UNION operator to combine results with another query.
     * UNION removes duplicate rows from the combined result set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.union("SELECT id, name FROM archived_users");
     * }</pre>
     *
     * @param query the complete SQL query to union with (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder union(final String query) {
        N.checkArgNotNull(query, "query");

        getStringBuilderForMoreParts().append(" UNION ").append(query);

        return this;
    }

    /**
     * Adds a UNION ALL operator to combine results with another query.
     * UNION ALL keeps all rows including duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.unionAll("SELECT id, name FROM temp_users");
     * }</pre>
     *
     * @param query the complete SQL query to union with (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder unionAll(final String query) {
        N.checkArgNotNull(query, "query");

        getStringBuilderForMoreParts().append(" UNION ALL ").append(query);

        return this;
    }

    /**
     * Adds an INTERSECT operator to find common rows between queries.
     * Returns only rows that appear in both result sets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.intersect("SELECT user_id FROM premium_users");
     * }</pre>
     *
     * @param query the complete SQL query to intersect with (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder intersect(final String query) {
        N.checkArgNotNull(query, "query");

        getStringBuilderForMoreParts().append(" INTERSECT ").append(query);

        return this;
    }

    /**
     * Adds an EXCEPT operator to find rows in the first query but not in the second.
     * This is the SQL standard operator (used by PostgreSQL, SQL Server).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.except("SELECT user_id FROM blocked_users");
     * }</pre>
     *
     * @param query the complete SQL query to exclude results from (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder except(final String query) {
        N.checkArgNotNull(query, "query");

        getStringBuilderForMoreParts().append(" EXCEPT ").append(query);

        return this;
    }

    /**
     * Adds a MINUS operator to find rows in the first query but not in the second.
     * This is Oracle's equivalent of EXCEPT.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * builder.minus("SELECT user_id FROM inactive_users");
     * }</pre>
     *
     * @param query the complete SQL query to exclude results from (must not be null)
     * @return this builder instance for method chaining
     */
    public DynamicSQLBuilder minus(final String query) {
        N.checkArgNotNull(query, "query");

        getStringBuilderForMoreParts().append(" MINUS ").append(query);

        return this;
    }

    /**
     * Builds the final SQL string from all the components and releases resources.
     * This method MUST be called to get the SQL and clean up internal resources.
     * After calling build(), this builder instance should not be reused.
     *
     * <p>The method combines all SQL components in the correct order and returns
     * the complete SQL statement. Internal StringBuilder objects are recycled
     * to the object pool for performance optimization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSQLBuilder builder = DynamicSQLBuilder.create();
     * builder.select().append("*");
     * builder.from().append("users");
     * builder.where().append("active = true");
     * String sql = builder.build();
     * // Returns: "SELECT * FROM users WHERE active = true"
     * }</pre>
     *
     * @return the complete SQL query string
     */
    public String build() {
        try {
            if (selectClause == null) {
                throw new IllegalStateException("This DynamicSQLBuilder has already been closed after build() was called");
            }

            if (fromClause != null && !fromClause.sb.isEmpty()) {
                if (!selectClause.sb.isEmpty()) {
                    selectClause.sb.append(" ");
                }

                selectClause.sb.append(fromClause.sb);
            }

            if (whereClause != null && !whereClause.sb.isEmpty()) {
                if (!selectClause.sb.isEmpty()) {
                    selectClause.sb.append(" ");
                }

                selectClause.sb.append(whereClause.sb);
            }

            if (groupByClause != null && !groupByClause.sb.isEmpty()) {
                if (!selectClause.sb.isEmpty()) {
                    selectClause.sb.append(" ");
                }

                selectClause.sb.append(groupByClause.sb);
            }

            if (havingClause != null && !havingClause.sb.isEmpty()) {
                if (!selectClause.sb.isEmpty()) {
                    selectClause.sb.append(" ");
                }

                selectClause.sb.append(havingClause.sb);
            }

            if (orderByClause != null && !orderByClause.sb.isEmpty()) {
                if (!selectClause.sb.isEmpty()) {
                    selectClause.sb.append(" ");
                }

                selectClause.sb.append(orderByClause.sb);
            }

            if (moreParts != null) {
                selectClause.sb.append(moreParts);
            }

            return selectClause.sb.toString();
        } finally {
            if (fromClause != null) {
                Objectory.recycle(fromClause.sb);
                fromClause = null;
            }

            if (whereClause != null) {
                Objectory.recycle(whereClause.sb);
                whereClause = null;
            }

            if (groupByClause != null) {
                Objectory.recycle(groupByClause.sb);
                groupByClause = null;
            }

            if (havingClause != null) {
                Objectory.recycle(havingClause.sb);
                havingClause = null;
            }

            if (orderByClause != null) {
                Objectory.recycle(orderByClause.sb);
                orderByClause = null;
            }

            if (moreParts != null) {
                Objectory.recycle(moreParts);
                moreParts = null;
            }

            if (selectClause != null) {
                Objectory.recycle(selectClause.sb);
                selectClause = null;
            }
        }
    }

    /**
     * Builder class for constructing the SELECT clause of a SQL query.
     * Provides methods to add columns with optional aliases and conditional inclusion.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#select()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.select()
     *     .append("id")
     *     .append("first_name", "fname")
     *     .append(Arrays.asList("email", "phone"))
     *     .appendIf(includeAge, "age");
     * }</pre>
     */
    public static class SelectClause {

        final StringBuilder sb;

        SelectClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a single column to the SELECT clause.
         * Automatically adds "SELECT " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append("user_id").append("username");
         * // Generates: SELECT user_id, username
         * }</pre>
         *
         * @param column the column name to select (must not be null)
         * @return this Select instance for method chaining
         */
        public SelectClause append(final String column) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(column);

            return this;
        }

        /**
         * Appends a column with an alias to the SELECT clause.
         * Generates: {@code column AS alias}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append("first_name", "fname").append("last_name", "lname");
         * // Generates: SELECT first_name AS fname, last_name AS lname
         * }</pre>
         *
         * @param column the column name to select (must not be null)
         * @param alias the alias for the column (must not be null)
         * @return this Select instance for method chaining
         */
        public SelectClause append(final String column, final String alias) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(column).append(" AS ").append(alias);

            return this;
        }

        /**
         * Appends multiple columns to the SELECT clause.
         * Columns are separated by commas.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append(Arrays.asList("id", "name", "email"));
         * // Generates: SELECT id, name, email
         * }</pre>
         *
         * @param columns collection of column names to select (must not be null)
         * @return this Select instance for method chaining
         */
        public SelectClause append(final Collection<String> columns) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Appends multiple columns with their aliases to the SELECT clause.
         * Each entry in the map represents a column-alias pair.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> cols = new HashMap<>();
         * cols.put("first_name", "fname");
         * cols.put("last_name", "lname");
         * select.append(cols);
         * // Generates: SELECT first_name AS fname, last_name AS lname
         * }</pre>
         *
         * @param columnsAndAliasMap map where keys are column names and values are aliases (must not be null)
         * @return this Select instance for method chaining
         */
        public SelectClause append(final Map<String, String> columnsAndAliasMap) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(Strings.joinEntries(columnsAndAliasMap, ", ", " AS "));

            return this;
        }

        /**
         * Conditionally appends a string to the SELECT clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.appendIf(includeSalary, "salary")
         *       .appendIf(includeBonus, "bonus");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this Select instance for method chaining
         */
        public SelectClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                } else {
                    sb.append("SELECT ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the SELECT clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.appendIfOrElse(showFullName,
         *                      "first_name || ' ' || last_name AS full_name",
         *                      "first_name");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this Select instance for method chaining
         */
        public SelectClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the FROM clause of a SQL query.
     * Supports adding tables, aliases, and various types of joins.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#from()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.from()
     *     .append("users", "u")
     *     .leftJoin("orders o", "u.id = o.user_id")
     *     .innerJoin("products p", "o.product_id = p.id");
     * }</pre>
     */
    public static class FromClause {

        final StringBuilder sb;

        FromClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a table to the FROM clause.
         * Multiple tables are separated by commas (creating a cross join).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users").append("departments");
         * // Generates: FROM users, departments
         * }</pre>
         *
         * @param table the table name to add (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause append(final String table) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("FROM ");
            }

            sb.append(table);

            return this;
        }

        /**
         * Appends a table with an alias to the FROM clause.
         * The alias can be used to reference the table in other clauses.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users", "u").append("orders", "o");
         * // Generates: FROM users u, orders o
         * }</pre>
         *
         * @param table the table name to add (must not be null)
         * @param alias the alias for the table (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause append(final String table, final String alias) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("FROM ");
            }

            sb.append(table).append(" ").append(alias);

            return this;
        }

        /**
         * Adds a JOIN clause (implicit INNER JOIN) with the specified table and join condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").join("orders o", "u.id = o.user_id");
         * // Generates: FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be null)
         * @param on the join condition (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause join(final String table, final String on) {
            sb.append(" JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds an INNER JOIN clause with the specified table and join condition.
         * Returns only rows that have matching values in both tables.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").innerJoin("orders o", "u.id = o.user_id");
         * // Generates: FROM users u INNER JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be null)
         * @param on the join condition (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause innerJoin(final String table, final String on) {
            sb.append(" INNER JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a LEFT JOIN clause with the specified table and join condition.
         * Returns all rows from the left table and matched rows from the right table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").leftJoin("orders o", "u.id = o.user_id");
         * // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be null)
         * @param on the join condition (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause leftJoin(final String table, final String on) {
            sb.append(" LEFT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a RIGHT JOIN clause with the specified table and join condition.
         * Returns all rows from the right table and matched rows from the left table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("orders o").rightJoin("users u", "o.user_id = u.id");
         * // Generates: FROM orders o RIGHT JOIN users u ON o.user_id = u.id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be null)
         * @param on the join condition (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause rightJoin(final String table, final String on) {
            sb.append(" RIGHT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a FULL JOIN clause with the specified table and join condition.
         * Returns all rows when there is a match in either table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("employees e").fullJoin("departments d", "e.dept_id = d.id");
         * // Generates: FROM employees e FULL JOIN departments d ON e.dept_id = d.id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be null)
         * @param on the join condition (must not be null)
         * @return this From instance for method chaining
         */
        public FromClause fullJoin(final String table, final String on) {
            sb.append(" FULL JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Conditionally appends a string to the FROM clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.appendIf(includeArchive, "archived_users");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this From instance for method chaining
         */
        public FromClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                } else {
                    sb.append("FROM ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the FROM clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.appendIfOrElse(useArchive, "archived_users", "active_users");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this From instance for method chaining
         */
        public FromClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("FROM ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the WHERE clause of a SQL query.
     * Supports adding conditions with AND/OR operators and parameter placeholders.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#where()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.where()
     *     .append("status = ?")
     *     .and("age >= ?")
     *     .or("vip = true")
     *     .and("city IN ").repeatQuestionMark(3, "(", ")");
     * // Generates: WHERE status = ? AND age >= ? OR vip = true AND city IN (?, ?, ?)
     * }</pre>
     */
    public static class WhereClause {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new where.
         *
         * @param sb
         */
        WhereClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a condition to the WHERE clause.
         * Automatically adds "WHERE " prefix on first call.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true").append("AND deleted = false");
         * // Generates: WHERE active = true AND deleted = false
         * }</pre>
         *
         * @param cond the condition to append (must not be null)
         * @return this Where instance for method chaining
         */
        public WhereClause append(final String cond) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            } else {
                sb.append("WHERE ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Appends question mark placeholders for parameterized queries.
         * Useful for IN clauses or multiple parameters.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("id IN (").repeatQuestionMark(3).append(")");
         * // Generates: id IN (?, ?, ?)
         * }</pre>
         *
         * @param placeholderCount the number of question marks to append
         * @return this Where instance for method chaining
         * @throws IllegalArgumentException if placeholderCount is negative
         */
        public WhereClause repeatQuestionMark(final int placeholderCount) {
            N.checkArgNotNegative(placeholderCount, "placeholderCount");

            for (int i = 0; i < placeholderCount; i++) {
                if (i > 0) {
                    sb.append(", ?");
                } else {
                    sb.append('?');
                }
            }

            return this;
        }

        /**
         * Appends question mark placeholders surrounded by prefix and postfix.
         * Commonly used for IN clauses with automatic parentheses.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("status IN ").repeatQuestionMark(3, "(", ")");
         * // Generates: status IN (?, ?, ?)
         * }</pre>
         *
         * @param placeholderCount the number of question marks to append
         * @param prefix the string to add before the question marks
         * @param postfix the string to add after the question marks
         * @return this Where instance for method chaining
         * @throws IllegalArgumentException if placeholderCount is negative
         */
        public WhereClause repeatQuestionMark(final int placeholderCount, final String prefix, final String postfix) {
            N.checkArgNotNegative(placeholderCount, "placeholderCount");

            if (placeholderCount > 0) {
                sb.append(prefix);

                for (int i = 0; i < placeholderCount; i++) {
                    if (i > 0) {
                        sb.append(", ?");
                    } else {
                        sb.append('?');
                    }
                }

                sb.append(postfix);
            }

            return this;
        }

        /**
         * Adds an AND condition to the WHERE clause.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true").and("age >= 18").and("country = ?");
         * // Generates: WHERE active = true AND age >= 18 AND country = ?
         * }</pre>
         *
         * @param cond the condition to add with AND (must not be null)
         * @return this Where instance for method chaining
         */
        public WhereClause and(final String cond) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            } else {
                sb.append(" AND ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Adds an OR condition to the WHERE clause.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("role = 'admin'").or("role = 'moderator'");
         * // Generates: WHERE role = 'admin' OR role = 'moderator'
         * }</pre>
         *
         * @param cond the condition to add with OR (must not be null)
         * @return this Where instance for method chaining
         */
        public WhereClause or(final String cond) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            } else {
                sb.append(" OR ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Conditionally appends a string to the WHERE clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true")
         *      .appendIf(filterByDate, "AND created_date > ?");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this Where instance for method chaining
         */
        public WhereClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                } else {
                    sb.append("WHERE ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the WHERE clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.appendIfOrElse(includeDeleted, 
         *                      "status IN ('active', 'deleted')",
         *                      "status = 'active'");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this Where instance for method chaining
         */
        public WhereClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            } else {
                sb.append("WHERE ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the GROUP BY clause of a SQL query.
     * Supports adding single or multiple grouping columns.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#groupBy()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.groupBy()
     *     .append("department")
     *     .append("year")
     *     .append(Arrays.asList("month", "region"));
     * // Generates: GROUP BY department, year, month, region
     * }</pre>
     */
    public static class GroupByClause {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new group by.
         *
         * @param sb
         */
        GroupByClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a column to the GROUP BY clause.
         * Automatically adds "GROUP BY " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append("category").append("subcategory");
         * // Generates: GROUP BY category, subcategory
         * }</pre>
         *
         * @param column the column name to group by (must not be null)
         * @return this GroupBy instance for method chaining
         */
        public GroupByClause append(final String column) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("GROUP BY ");
            }

            sb.append(column);

            return this;
        }

        /**
         * Appends multiple columns to the GROUP BY clause.
         * Columns are separated by commas.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append(Arrays.asList("year", "quarter", "region"));
         * // Generates: GROUP BY year, quarter, region
         * }</pre>
         *
         * @param columns collection of column names to group by (must not be null)
         * @return this GroupBy instance for method chaining
         */
        public GroupByClause append(final Collection<String> columns) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("GROUP BY ");
            }

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Conditionally appends a string to the GROUP BY clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append("product_id")
         *        .appendIf(groupByRegion, "region_id");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this GroupBy instance for method chaining
         */
        public GroupByClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                } else {
                    sb.append("GROUP BY ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the GROUP BY clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.appendIfOrElse(detailedReport,
         *                        "year, month, day",
         *                        "year");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this GroupBy instance for method chaining
         */
        public GroupByClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("GROUP BY ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the HAVING clause of a SQL query.
     * Used to filter grouped results based on aggregate conditions.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#having()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.groupBy().append("department");
     * builder.having().append("COUNT(*) > ?")
     *                 .and("AVG(salary) > ?");
     * // Generates: GROUP BY department HAVING COUNT(*) > ? AND AVG(salary) > ?
     * }</pre>
     */
    public static class HavingClause {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new having.
         *
         * @param sb
         */
        HavingClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a condition to the HAVING clause.
         * Automatically adds "HAVING " prefix on first call.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("SUM(amount) > 1000");
         * // Generates: HAVING SUM(amount) > 1000
         * }</pre>
         *
         * @param cond the condition to append (must not be null)
         * @return this Having instance for method chaining
         */
        public HavingClause append(final String cond) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            } else {
                sb.append("HAVING ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Adds an AND condition to the HAVING clause.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("COUNT(*) > 5").and("MAX(price) < 1000");
         * // Generates: HAVING COUNT(*) > 5 AND MAX(price) < 1000
         * }</pre>
         *
         * @param cond the condition to add with AND (must not be null)
         * @return this Having instance for method chaining
         */
        public HavingClause and(final String cond) {
            if (sb.isEmpty()) {
                sb.append("HAVING ");
            } else {
                sb.append(" AND ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Adds an OR condition to the HAVING clause.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("MIN(score) > 80").or("AVG(score) > 90");
         * // Generates: HAVING MIN(score) > 80 OR AVG(score) > 90
         * }</pre>
         *
         * @param cond the condition to add with OR (must not be null)
         * @return this Having instance for method chaining
         */
        public HavingClause or(final String cond) {
            if (sb.isEmpty()) {
                sb.append("HAVING ");
            } else {
                sb.append(" OR ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Conditionally appends a string to the HAVING clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("COUNT(*) > 0")
         *       .appendIf(checkRevenue, "AND SUM(revenue) > ?");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this Having instance for method chaining
         */
        public HavingClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                } else {
                    sb.append("HAVING ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the HAVING clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.appendIfOrElse(strictFilter,
         *                       "COUNT(*) > 100",
         *                       "COUNT(*) > 10");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this Having instance for method chaining
         */
        public HavingClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            } else {
                sb.append("HAVING ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the ORDER BY clause of a SQL query.
     * Supports adding single or multiple columns with sort directions.
     * 
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSQLBuilder#orderBy()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.orderBy()
     *     .append("created_date DESC")
     *     .append("priority ASC")
     *     .append(Arrays.asList("category", "name"));
     * // Generates: ORDER BY created_date DESC, priority ASC, category, name
     * }</pre>
     */
    public static class OrderByClause {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new order by.
         *
         * @param sb
         */
        OrderByClause(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         * Appends a column (with optional sort direction) to the ORDER BY clause.
         * Automatically adds "ORDER BY " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append("created_date DESC").append("name ASC");
         * // Generates: ORDER BY created_date DESC, name ASC
         * }</pre>
         *
         * @param column the column name with optional ASC/DESC (must not be null)
         * @return this OrderBy instance for method chaining
         */
        public OrderByClause append(final String column) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("ORDER BY ");
            }

            sb.append(column);

            return this;
        }

        /**
         * Appends multiple columns to the ORDER BY clause.
         * Columns are separated by commas. Sort direction can be included with each column.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append(Arrays.asList("year DESC", "month DESC", "day DESC"));
         * // Generates: ORDER BY year DESC, month DESC, day DESC
         * }</pre>
         *
         * @param columns collection of column names with optional sort directions (must not be null)
         * @return this OrderBy instance for method chaining
         */
        public OrderByClause append(final Collection<String> columns) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("ORDER BY ");
            }

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Conditionally appends a string to the ORDER BY clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append("priority DESC")
         *        .appendIf(sortByDate, "created_date DESC");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true
         * @return this OrderBy instance for method chaining
         */
        public OrderByClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                } else {
                    sb.append("ORDER BY ");
                }

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the ORDER BY clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.appendIfOrElse(newestFirst,
         *                        "created_date DESC",
         *                        "created_date ASC");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true
         * @param textToAppendWhenFalse the string to append if condition is false
         * @return this OrderBy instance for method chaining
         */
        public OrderByClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            } else {
                sb.append("ORDER BY ");
            }

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * A convenience subclass of DynamicSQLBuilder with a shorter name.
     * Functionality is identical to the parent class.
     *
     * <p>This class exists purely for brevity when the full class name would be too verbose.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * DSB builder = DSB.create();
     * builder.select().append("*");
     * builder.from().append("users");
     * String sql = builder.build();
     * }</pre>
     */
    public static final class DSB extends DynamicSQLBuilder {

        /**
         * Instantiates a new dsb.
         */
        private DSB() {
        }

        /**
         * Creates a new instance of DSB.
         * This is a shorthand for DynamicSQLBuilder.create() with a shorter class name.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DSB builder = DSB.create();
         * builder.select().append("*");
         * builder.from().append("users");
         * String sql = builder.build();
         * }</pre>
         *
         * @return a new DSB instance for method chaining
         */
        public static DSB create() {
            return new DSB();
        }
    }
}
