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
 * Entry point for fluently creating dynamic SQL queries programmatically.
 * This utility class exposes {@link #builder()}, which returns a {@link DynamicSqlBuilder} that provides
 * a fluent and lightweight way to construct SQL SELECT statements with support for joins,
 * conditions, grouping, ordering, and set operations.
 *
 * <p>The {@link DynamicSqlBuilder} follows a fluent interface pattern where each method returns the builder
 * instance, allowing method chaining. The SQL components are built in a natural order:
 * SELECT → FROM → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT/OFFSET.</p>
 *
 * <p><b>Important:</b> Always call {@link DynamicSqlBuilder#build()} to generate the final SQL string and
 * release resources. The builder uses object pooling internally for performance optimization.</p>
 *
 * <h2>Example usage:</h2>
 * <pre>{@code
 * DynamicSqlBuilder builder = DynamicQuery.builder();
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
public final class DynamicQuery {

    static final Logger logger = LoggerFactory.getLogger(DynamicQuery.class);

    private DynamicQuery() {
        // utility/wrapper class.
    }

    /**
     * Creates a new {@link DynamicSqlBuilder} instance for constructing a dynamic SQL query.
     *
     * <p>Each call returns a fresh, independent {@link DynamicSqlBuilder}. Always finish with
     * {@link DynamicSqlBuilder#build()} to obtain the SQL and release pooled resources.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DynamicSqlBuilder builder = DynamicQuery.builder();
     * builder.select().append("*");
     * builder.from().append("users");
     * builder.where().append("active = true");
     * String sql = builder.build();
     * // Returns: "SELECT * FROM users WHERE active = true"
     *
     * // Each call returns a new, independent builder
     * DynamicSqlBuilder another = DynamicQuery.builder();   // not the same instance as 'builder'
     * }</pre>
     *
     * @return a new {@link DynamicSqlBuilder} instance
     */
    public static DynamicSqlBuilder builder() {
        return new DynamicSqlBuilder();
    }

    private static void checkSqlFragmentNotBlank(final String value, final String argName) {
        if (Strings.isBlank(value)) {
            throw new IllegalArgumentException(argName + " must not be null, empty, or blank");
        }
    }

    private static void checkSqlFragmentsNotBlank(final Collection<String> values, final String argName) {
        N.checkArgNotNull(values, argName);

        for (final String value : values) {
            checkSqlFragmentNotBlank(value, "Element in " + argName);
        }
    }

    private static void checkSqlFragmentMapNotBlank(final Map<String, String> values, final String argName) {
        N.checkArgNotNull(values, argName);

        for (final Map.Entry<String, String> entry : values.entrySet()) {
            checkSqlFragmentNotBlank(entry.getKey(), "Key in " + argName);
            checkSqlFragmentNotBlank(entry.getValue(), "Value in " + argName);
        }
    }

    /**
     * Emits the leading token for a clause fragment: the clause {@code keyword} when the buffer is
     * still empty (the first fragment), otherwise the inter-fragment {@code separator}. Shared by all
     * clause builders to keep the "first vs subsequent fragment" decision in one place.
     *
     * @param sb the clause buffer
     * @param keyword the clause keyword to start with (e.g. {@code "SELECT "})
     * @param separator the separator placed between fragments (e.g. {@code ", "})
     */
    private static void startClauseFragment(final StringBuilder sb, final String keyword, final String separator) {
        if (sb.isEmpty()) {
            sb.append(keyword);
        } else {
            sb.append(separator);
        }
    }

    private static void startClauseFragment(final ClauseBuilder clause, final String keyword, final String separator) {
        clause.checkOpen();
        startClauseFragment(clause.sb, keyword, separator);
    }

    /**
     * Builder for constructing dynamic SQL queries clause by clause.
     */
    public static class DynamicSqlBuilder {

        /** The {@link SelectClause} for this builder. */
        private SelectClause selectClause = new SelectClause(Objectory.createStringBuilder());

        /** The {@link FromClause} for this builder. */
        private FromClause fromClause = new FromClause(Objectory.createStringBuilder());

        /** The {@link WhereClause} for this builder. */
        private WhereClause whereClause;

        /** The {@link GroupByClause} for this builder. */
        private GroupByClause groupByClause;

        /** The {@link HavingClause} for this builder. */
        private HavingClause havingClause;

        /** The {@link OrderByClause} for this builder. */
        private OrderByClause orderByClause;

        /** The {@link StringBuilder} for additional SQL parts. */
        private StringBuilder moreParts = null;

        private DynamicSqlBuilder() {

        }

        /**
         * Returns the {@link SelectClause} builder for defining columns to retrieve.
         * Multiple calls to this method return the same instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.select().append("id").append("name", "user_name");
         * // Generates: SELECT id, name AS user_name
         * }</pre>
         *
         * @return the {@link SelectClause} builder for method chaining
         */
        public SelectClause select() {
            return selectClause;
        }

        /**
         * Returns the {@link FromClause} builder for defining tables and joins.
         * Multiple calls to this method return the same instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
         * // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @return the {@link FromClause} builder for method chaining
         */
        public FromClause from() {
            return fromClause;
        }

        /**
         * Returns the {@link WhereClause} builder for defining query conditions.
         * Creates a new instance on the first call and returns the same instance on subsequent calls.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.where().append("status = ?").and("created_date > ?");
         * // Generates: WHERE status = ? AND created_date > ?
         * }</pre>
         *
         * @return the {@link WhereClause} builder for method chaining
         */
        public WhereClause where() {
            if (whereClause == null) {
                whereClause = new WhereClause(Objectory.createStringBuilder());
            }

            return whereClause;
        }

        /**
         * Returns the {@link GroupByClause} builder for defining grouping columns.
         * Creates a new instance on the first call and returns the same instance on subsequent calls.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.groupBy().append("department").append("year");
         * // Generates: GROUP BY department, year
         * }</pre>
         *
         * @return the {@link GroupByClause} builder for method chaining
         */
        public GroupByClause groupBy() {
            if (groupByClause == null) {
                groupByClause = new GroupByClause(Objectory.createStringBuilder());
            }

            return groupByClause;
        }

        /**
         * Returns the {@link HavingClause} builder for defining conditions on grouped results.
         * Creates a new instance on the first call and returns the same instance on subsequent calls.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.having().append("COUNT(*) > ?").and("SUM(amount) < ?");
         * // Generates: HAVING COUNT(*) > ? AND SUM(amount) < ?
         * }</pre>
         *
         * @return the {@link HavingClause} builder for method chaining
         */
        public HavingClause having() {
            if (havingClause == null) {
                havingClause = new HavingClause(Objectory.createStringBuilder());
            }

            return havingClause;
        }

        /**
         * Returns the {@link OrderByClause} builder for defining result ordering.
         * Creates a new instance on the first call and returns the same instance on subsequent calls.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.orderBy().append("created_date DESC").append("name ASC");
         * // Generates: ORDER BY created_date DESC, name ASC
         * }</pre>
         *
         * @return the {@link OrderByClause} builder for method chaining
         */
        public OrderByClause orderBy() {
            if (orderByClause == null) {
                orderByClause = new OrderByClause(Objectory.createStringBuilder());
            }

            return orderByClause;
        }

        /**
         * Adds a {@code LIMIT} clause to restrict the number of rows returned.
         * Generates standard SQL: {@code LIMIT n}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.select().append("*");
         * builder.from().append("users");
         * builder.limit(10);
         * // Generates: LIMIT 10
         * }</pre>
         *
         * @param count the maximum number of rows to return (must not be negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code count} is negative
         */
        public DynamicSqlBuilder limit(final int count) {
            N.checkArgNotNegative(count, "count");

            getStringBuilderForMoreParts().append(" LIMIT ").append(count);

            return this;
        }

        /**
         * Adds a {@code LIMIT} clause with count and offset for pagination.
         * Generates SQL standard syntax: {@code LIMIT count OFFSET offset}; the {@code OFFSET} portion is
         * omitted when {@code offset} is {@code 0}, emitting just {@code LIMIT count}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.select().append("*");
         * builder.from().append("users");
         * builder.limit(10, 20);  // count=10, offset=20
         * // Generates: SELECT * FROM users LIMIT 10 OFFSET 20 (skip 20 rows, return next 10)
         * }</pre>
         *
         * @param count the maximum number of rows to return (must not be negative)
         * @param offset the number of rows to skip (must not be negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
         * @see #offsetRows(int)
         * @see #fetchNextRows(int)
         * @see #fetchFirstRows(int)
         */
        public DynamicSqlBuilder limit(final int count, final int offset) {
            N.checkArgNotNegative(count, "count");
            N.checkArgNotNegative(offset, "offset");

            if (offset > 0) {
                getStringBuilderForMoreParts().append(" LIMIT ").append(count).append(" OFFSET ").append(offset);
            } else {
                getStringBuilderForMoreParts().append(" LIMIT ").append(count);
            }

            return this;
        }

        /**
         * Adds a plain {@code OFFSET} clause to skip the given number of leading rows.
         * Generates: {@code OFFSET n} (without the trailing {@code ROWS} keyword). When {@code offset}
         * is {@code 0}, nothing is appended.
         *
         * <p>Use {@link #offsetRows(int)} instead when you need the SQL:2008 {@code OFFSET n ROWS}
         * form (typically paired with {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)}).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.limit(10).offset(20);
         * // Generates: LIMIT 10 OFFSET 20
         * }</pre>
         *
         * @param offset the number of rows to skip (must not be negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code offset} is negative
         * @see #offsetRows(int)
         */
        public DynamicSqlBuilder offset(final int offset) {
            N.checkArgNotNegative(offset, "offset");

            if (offset > 0) {
                getStringBuilderForMoreParts().append(" OFFSET ").append(offset);
            }

            return this;
        }

        /**
         * Adds an {@code OFFSET} clause for SQL:2008 standard pagination.
         * Typically used with {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)}.
         * When {@code offset} is {@code 0}, nothing is appended.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.offsetRows(20).fetchNextRows(10);
         * // Generates: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
         * }</pre>
         *
         * @param offset the number of rows to skip (must not be negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code offset} is negative
         */
        public DynamicSqlBuilder offsetRows(final int offset) {
            N.checkArgNotNegative(offset, "offset");

            if (offset > 0) {
                getStringBuilderForMoreParts().append(" OFFSET ").append(offset).append(" ROWS");
            }

            return this;
        }

        /**
         * Adds a {@code FETCH NEXT} clause for SQL:2008 standard result limiting.
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
         * @throws IllegalArgumentException if {@code count} is negative
         */
        public DynamicSqlBuilder fetchNextRows(final int count) {
            N.checkArgNotNegative(count, "count");

            getStringBuilderForMoreParts().append(" FETCH NEXT ").append(count).append(" ROWS ONLY");

            return this;
        }

        /**
         * Adds a {@code FETCH FIRST} clause for SQL:2008 standard result limiting.
         * This is an alternative to {@code FETCH NEXT} with the same functionality.
         * Typically used after {@link #offsetRows(int)}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.fetchFirstRows(10);
         * // Generates: FETCH FIRST 10 ROWS ONLY
         * }</pre>
         *
         * @param count the number of rows to fetch (must not be negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code count} is negative
         * @see #offsetRows(int)
         */
        public DynamicSqlBuilder fetchFirstRows(final int count) {
            N.checkArgNotNegative(count, "count");

            getStringBuilderForMoreParts().append(" FETCH FIRST ").append(count).append(" ROWS ONLY");

            return this;
        }

        /**
         * Adds a {@code UNION} operator to combine results with another query.
         * {@code UNION} removes duplicate rows from the combined result set.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.union("SELECT id, name FROM archived_users");
         * // Appends: UNION SELECT id, name FROM archived_users
         * }</pre>
         *
         * @param query the complete SQL query to union with (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code query} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder union(final String query) {
            checkSqlFragmentNotBlank(query, "query");

            getStringBuilderForMoreParts().append(" UNION ").append(query);

            return this;
        }

        /**
         * Adds a {@code UNION ALL} operator to combine results with another query.
         * {@code UNION ALL} keeps all rows including duplicates.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.unionAll("SELECT id, name FROM temp_users");
         * // Appends: UNION ALL SELECT id, name FROM temp_users
         * }</pre>
         *
         * @param query the complete SQL query to union with (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code query} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder unionAll(final String query) {
            checkSqlFragmentNotBlank(query, "query");

            getStringBuilderForMoreParts().append(" UNION ALL ").append(query);

            return this;
        }

        /**
         * Adds an {@code INTERSECT} operator to find common rows between queries.
         * Returns only rows that appear in both result sets.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.intersect("SELECT user_id FROM premium_users");
         * // Appends: INTERSECT SELECT user_id FROM premium_users
         * }</pre>
         *
         * @param query the complete SQL query to intersect with (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code query} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder intersect(final String query) {
            checkSqlFragmentNotBlank(query, "query");

            getStringBuilderForMoreParts().append(" INTERSECT ").append(query);

            return this;
        }

        /**
         * Adds an {@code EXCEPT} operator to find rows in the first query but not in the second.
         * This is the SQL standard operator (used by PostgreSQL, SQL Server).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.except("SELECT user_id FROM blocked_users");
         * // Appends: EXCEPT SELECT user_id FROM blocked_users
         * }</pre>
         *
         * @param query the complete SQL query whose result rows are subtracted from the current result set (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code query} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder except(final String query) {
            checkSqlFragmentNotBlank(query, "query");

            getStringBuilderForMoreParts().append(" EXCEPT ").append(query);

            return this;
        }

        /**
         * Adds a {@code MINUS} operator to find rows in the first query but not in the second.
         * This is Oracle's equivalent of {@code EXCEPT}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.minus("SELECT user_id FROM inactive_users");
         * // Appends: MINUS SELECT user_id FROM inactive_users
         * }</pre>
         *
         * @param query the complete SQL query whose result rows are subtracted from the current result set (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code query} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder minus(final String query) {
            checkSqlFragmentNotBlank(query, "query");

            getStringBuilderForMoreParts().append(" MINUS ").append(query);

            return this;
        }

        /**
         * Appends a raw, database-specific SQL clause or fragment verbatim to the end of the query.
         * The supplied text is emitted unchanged (preceded by a single space) and is <em>not</em>
         * validated, escaped, or interpreted in any way — whatever you pass becomes the literal tail
         * of the generated SQL. Use it for any trailing clause that has no typed builder method, such
         * as pagination/row-limiting syntax (for example {@code "LIMIT 10 OFFSET 20"} or a SQL:2008
         * {@code FETCH FIRST ... ROWS ONLY} clause), locking hints, or other vendor-specific suffixes.
         *
         * <p>Prefer the typed clause methods (such as {@link #limit(int)}, {@link #limit(int, int)},
         * {@link #offset(int)}, {@link #offsetRows(int)}, {@link #fetchFirstRows(int)}, or
         * {@link #fetchNextRows(int)}) whenever they can express the desired clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.append("LIMIT 10 OFFSET 20");
         * // Or any trailing clause that has no typed overload (e.g. a SQL:2008 row-limiting clause)
         * builder.append("FETCH FIRST 10 ROWS ONLY");
         * }</pre>
         *
         * @param rawClause the complete raw SQL clause to append verbatim (e.g., {@code "LIMIT 10 OFFSET 20"}) (must not be {@code null}, empty, or blank)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code rawClause} is {@code null}, empty, or blank
         */
        public DynamicSqlBuilder append(final String rawClause) {
            checkSqlFragmentNotBlank(rawClause, "rawClause");

            getStringBuilderForMoreParts().append(" ").append(rawClause);

            return this;
        }

        /**
         * Conditionally appends a raw SQL clause or fragment verbatim to the end of the query.
         * When {@code condition} is {@code true} this behaves exactly like {@link #append(String)}
         * (the text is emitted unchanged, preceded by a single space, with no validation, escaping,
         * or interpretation); when {@code condition} is {@code false} the builder is left unchanged
         * and {@code rawClause} is not inspected.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.appendIf(paged, "LIMIT 10 OFFSET 20")
         *        .appendIf(locked, "FOR UPDATE");
         * }</pre>
         *
         * @param condition the condition to check
         * @param rawClause the raw SQL clause to append verbatim if {@code condition} is {@code true}
         *                  (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code rawClause} is {@code null}, empty, or blank
         * @see #append(String)
         */
        public DynamicSqlBuilder appendIf(final boolean condition, final String rawClause) {
            if (condition) {
                checkSqlFragmentNotBlank(rawClause, "rawClause");

                getStringBuilderForMoreParts().append(" ").append(rawClause);
            }

            return this;
        }

        private StringBuilder getStringBuilderForMoreParts() {
            if (moreParts == null) {
                moreParts = Objectory.createStringBuilder();
            }

            return moreParts;
        }

        /**
         * Builds the final SQL string from all the components and releases resources.
         * This method MUST be called to get the SQL and clean up internal resources.
         * After calling {@code build()}, this builder instance should not be reused.
         *
         * <p>The method combines all SQL components in the correct order and returns
         * the complete SQL statement. Internal {@link StringBuilder} objects are recycled
         * to the object pool for performance optimization.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DynamicSqlBuilder builder = DynamicQuery.builder();
         * builder.select().append("*");
         * builder.from().append("users");
         * builder.where().append("active = true");
         * String sql = builder.build();
         * // Returns: "SELECT * FROM users WHERE active = true"
         * }</pre>
         *
         * @return the complete SQL query string
         * @throws IllegalStateException if the builder has already been built/closed
         */
        public String build() {
            try {
                if (selectClause == null) {
                    throw new IllegalStateException("This DynamicSqlBuilder has already been closed after build() was called");
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

                final String sql = selectClause.sb.toString();

                if (logger.isDebugEnabled()) {
                    logger.debug("Built dynamic SQL: {}", sql);
                }

                return sql;
            } finally {
                if (fromClause != null) {
                    fromClause.close();
                    fromClause = null;
                }

                if (whereClause != null) {
                    whereClause.close();
                    whereClause = null;
                }

                if (groupByClause != null) {
                    groupByClause.close();
                    groupByClause = null;
                }

                if (havingClause != null) {
                    havingClause.close();
                    havingClause = null;
                }

                if (orderByClause != null) {
                    orderByClause.close();
                    orderByClause = null;
                }

                if (moreParts != null) {
                    Objectory.recycle(moreParts);
                    moreParts = null;
                }

                if (selectClause != null) {
                    selectClause.close();
                    selectClause = null;
                }
            }
        }

    }

    static abstract class ClauseBuilder {
        final StringBuilder sb;

        private boolean closed;

        ClauseBuilder(final StringBuilder sb) {
            this.sb = sb;
        }

        final void checkOpen() {
            if (closed) {
                throw new IllegalStateException("Clause builder has already been closed by build()");
            }
        }

        final void close() {
            closed = true;
            Objectory.recycle(sb);
        }
    }

    /**
     * Builder class for constructing the {@code SELECT} clause of a SQL query.
     * Provides methods to add columns with optional aliases and conditional inclusion.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#select()}
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
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class SelectClause extends ClauseBuilder {

        SelectClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a single column to the {@code SELECT} clause.
         * Automatically adds "SELECT " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append("user_id").append("username");
         * // Generates: SELECT user_id, username
         * }</pre>
         *
         * @param column the column name to select (must not be {@code null}, empty, or blank)
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if {@code column} is {@code null}, empty, or blank
         */
        public SelectClause append(final String column) {
            checkSqlFragmentNotBlank(column, "column");

            startClauseFragment(this, "SELECT ", ", ");

            sb.append(column);

            return this;
        }

        /**
         * Appends a column with an alias to the {@code SELECT} clause.
         * Generates: {@code column AS alias}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append("first_name", "fname").append("last_name", "lname");
         * // Generates: SELECT first_name AS fname, last_name AS lname
         * }</pre>
         *
         * @param column the column name to select (must not be {@code null}, empty, or blank)
         * @param alias the alias for the column (must not be {@code null}, empty, or blank)
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if {@code column} or {@code alias} is {@code null}, empty, or blank
         */
        public SelectClause append(final String column, final String alias) {
            checkSqlFragmentNotBlank(column, "column");
            checkSqlFragmentNotBlank(alias, "alias");

            startClauseFragment(this, "SELECT ", ", ");

            sb.append(column).append(" AS ").append(alias);

            return this;
        }

        /**
         * Appends multiple columns to the {@code SELECT} clause.
         * Columns are separated by commas. If the collection is empty, this method does nothing.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.append(Arrays.asList("id", "name", "email"));
         * // Generates: SELECT id, name, email
         * }</pre>
         *
         * @param columns collection of column names to select (may be {@code null} or empty;
         *                individual elements must not be {@code null}, empty, or blank)
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if any element in {@code columns} is {@code null}, empty, or blank
         */
        public SelectClause append(final Collection<String> columns) {
            if (N.isEmpty(columns)) {
                return this;
            }

            checkSqlFragmentsNotBlank(columns, "columns");

            startClauseFragment(this, "SELECT ", ", ");

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Appends multiple columns with their aliases to the {@code SELECT} clause.
         * Each entry in the map represents a column-alias pair. If the map is empty, this method does nothing.
         * Columns are emitted in the map's iteration order, so use a {@link java.util.LinkedHashMap}
         * if a stable column order matters.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> cols = new LinkedHashMap<>();
         * cols.put("first_name", "fname");
         * cols.put("last_name", "lname");
         * select.append(cols);
         * // Generates: SELECT first_name AS fname, last_name AS lname
         * }</pre>
         *
         * @param columnsAndAliasMap map where keys are column names and values are aliases (may be {@code null} or empty;
         *        individual keys and values must not be {@code null}, empty, or blank)
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if any key or value in the map is {@code null}, empty, or blank
         */
        public SelectClause append(final Map<String, String> columnsAndAliasMap) {
            if (N.isEmpty(columnsAndAliasMap)) {
                return this;
            }

            checkSqlFragmentMapNotBlank(columnsAndAliasMap, "columnsAndAliasMap");

            startClauseFragment(this, "SELECT ", ", ");

            sb.append(Strings.joinEntries(columnsAndAliasMap, ", ", " AS "));

            return this;
        }

        /**
         * Conditionally appends a string to the {@code SELECT} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * select.appendIf(includeSalary, "salary")
         *       .appendIf(includeBonus, "bonus");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public SelectClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "SELECT ", ", ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code SELECT} clause based on a boolean condition.
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
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link SelectClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public SelectClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "SELECT ", ", ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the {@code FROM} clause of a SQL query.
     * Supports adding tables, aliases, and various types of joins.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#from()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.from()
     *     .append("users", "u")
     *     .leftJoin("orders o", "u.id = o.user_id")
     *     .innerJoin("products p", "o.product_id = p.id");
     * }</pre>
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class FromClause extends ClauseBuilder {

        FromClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a table to the {@code FROM} clause.
         * Multiple tables are separated by commas (creating a cross join).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users").append("departments");
         * // Generates: FROM users, departments
         * }</pre>
         *
         * @param table the table name to add (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} is {@code null}, empty, or blank
         */
        public FromClause append(final String table) {
            checkSqlFragmentNotBlank(table, "table");

            startClauseFragment(this, "FROM ", ", ");

            sb.append(table);

            return this;
        }

        /**
         * Appends a table with an alias to the {@code FROM} clause.
         * The alias can be used to reference the table in other clauses.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users", "u").append("orders", "o");
         * // Generates: FROM users u, orders o
         * }</pre>
         *
         * @param table the table name to add (must not be {@code null}, empty, or blank)
         * @param alias the alias for the table (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code alias} is {@code null}, empty, or blank
         */
        public FromClause append(final String table, final String alias) {
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(alias, "alias");

            startClauseFragment(this, "FROM ", ", ");

            sb.append(table).append(" ").append(alias);

            return this;
        }

        /**
         * Adds a {@code JOIN} clause (implicit {@code INNER JOIN}) with the specified table and join condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").join("orders o", "u.id = o.user_id");
         * // Generates: FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be {@code null}, empty, or blank)
         * @param on the join condition (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code on} is {@code null}, empty, or blank
         * @throws IllegalStateException if the {@code FROM} clause has not been initialized by a prior call that actually appended a table
         *         (e.g. {@code append(...)}, {@code appendIf(...)} with a {@code true} condition, or {@code appendIfOrElse(...)})
         */
        public FromClause join(final String table, final String on) {
            checkOpen();
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(on, "on");
            requireFromInitialized();
            sb.append(" JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds an {@code INNER JOIN} clause with the specified table and join condition.
         * Returns only rows that have matching values in both tables.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").innerJoin("orders o", "u.id = o.user_id");
         * // Generates: FROM users u INNER JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be {@code null}, empty, or blank)
         * @param on the join condition (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code on} is {@code null}, empty, or blank
         * @throws IllegalStateException if the {@code FROM} clause has not been initialized by a prior call that actually appended a table
         *         (e.g. {@code append(...)}, {@code appendIf(...)} with a {@code true} condition, or {@code appendIfOrElse(...)})
         */
        public FromClause innerJoin(final String table, final String on) {
            checkOpen();
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(on, "on");
            requireFromInitialized();
            sb.append(" INNER JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a {@code LEFT JOIN} clause with the specified table and join condition.
         * Returns all rows from the left table and matched rows from the right table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("users u").leftJoin("orders o", "u.id = o.user_id");
         * // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be {@code null}, empty, or blank)
         * @param on the join condition (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code on} is {@code null}, empty, or blank
         * @throws IllegalStateException if the {@code FROM} clause has not been initialized by a prior call that actually appended a table
         *         (e.g. {@code append(...)}, {@code appendIf(...)} with a {@code true} condition, or {@code appendIfOrElse(...)})
         */
        public FromClause leftJoin(final String table, final String on) {
            checkOpen();
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(on, "on");
            requireFromInitialized();
            sb.append(" LEFT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a {@code RIGHT JOIN} clause with the specified table and join condition.
         * Returns all rows from the right table and matched rows from the left table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("orders o").rightJoin("users u", "o.user_id = u.id");
         * // Generates: FROM orders o RIGHT JOIN users u ON o.user_id = u.id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be {@code null}, empty, or blank)
         * @param on the join condition (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code on} is {@code null}, empty, or blank
         * @throws IllegalStateException if the {@code FROM} clause has not been initialized by a prior call that actually appended a table
         *         (e.g. {@code append(...)}, {@code appendIf(...)} with a {@code true} condition, or {@code appendIfOrElse(...)})
         */
        public FromClause rightJoin(final String table, final String on) {
            checkOpen();
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(on, "on");
            requireFromInitialized();
            sb.append(" RIGHT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         * Adds a {@code FULL JOIN} clause with the specified table and join condition.
         * Returns all rows from both tables, with {@code NULL}s on either side where there
         * is no matching row in the other table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.append("employees e").fullJoin("departments d", "e.dept_id = d.id");
         * // Generates: FROM employees e FULL JOIN departments d ON e.dept_id = d.id
         * }</pre>
         *
         * @param table the table to join (can include alias; must not be {@code null}, empty, or blank)
         * @param on the join condition (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code table} or {@code on} is {@code null}, empty, or blank
         * @throws IllegalStateException if the {@code FROM} clause has not been initialized by a prior call that actually appended a table
         *         (e.g. {@code append(...)}, {@code appendIf(...)} with a {@code true} condition, or {@code appendIfOrElse(...)})
         */
        public FromClause fullJoin(final String table, final String on) {
            checkOpen();
            checkSqlFragmentNotBlank(table, "table");
            checkSqlFragmentNotBlank(on, "on");
            requireFromInitialized();
            sb.append(" FULL JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        private void requireFromInitialized() {
            checkOpen();
            if (sb.isEmpty()) {
                throw new IllegalStateException("FROM clause must be initialized by append(...) before join operations");
            }
        }

        /**
         * Conditionally appends a string to the {@code FROM} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.appendIf(includeArchive, "archived_users");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public FromClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "FROM ", ", ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code FROM} clause based on a boolean condition.
         * Always appends something, choosing between two options based on the condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * from.appendIfOrElse(useArchive, "archived_users", "active_users");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link FromClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public FromClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "FROM ", ", ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the {@code WHERE} clause of a SQL query.
     * Supports adding conditions with {@code AND}/{@code OR} operators and parameter placeholders.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#where()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.where()
     *     .append("status = ?")
     *     .and("age >= ?")
     *     .or("vip = true")
     *     .and("city IN ").placeholders(3, "(", ")");
     * // Generates: WHERE status = ? AND age >= ? OR vip = true AND city IN (?, ?, ?)
     * }</pre>
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class WhereClause extends ClauseBuilder {

        WhereClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a condition to the {@code WHERE} clause.
         * Automatically adds "WHERE " prefix on first call.
         *
         * <p>Unlike {@link #and(String)}/{@link #or(String)}, this method does <em>not</em> insert a logical
         * connective — the caller must include any required {@code AND}/{@code OR} in the argument.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true").append("AND deleted = false");
         * // Generates: WHERE active = true AND deleted = false
         * }</pre>
         *
         * @param cond the condition to append (must not be {@code null}, empty, or blank)
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public WhereClause append(final String cond) {
            checkSqlFragmentNotBlank(cond, "cond");

            startClauseFragment(this, "WHERE ", " ");

            sb.append(cond);

            return this;
        }

        /**
         * Appends question mark placeholders for parameterized queries.
         * Useful for {@code IN} clauses or multiple parameters.
         *
         * <p>Note: this method writes only the {@code ?} characters separated by {@code ", "} into the
         * underlying buffer; it does not insert any surrounding whitespace or parentheses. Use the
         * {@link #placeholders(int, String, String)} overload when you also need a prefix/postfix
         * (e.g. parentheses for an {@code IN} list).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("id IN (").placeholders(3).append(")");
         * // sb contents so far: "WHERE id IN (?, ?, ? )"
         * // Note: placeholders(int) appends the markers with no leading space, while append(String)
         * // inserts a single space before each fragment, so a manual close paren is preceded by a space.
         * // Prefer placeholders(int, String, String) when you want the parentheses tightly attached.
         * }</pre>
         *
         * @param placeholderCount the number of question marks to append (must not be negative)
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code placeholderCount} is negative
         */
        public WhereClause placeholders(final int placeholderCount) {
            checkOpen();
            N.checkArgNotNegative(placeholderCount, "placeholderCount");

            appendPlaceholders(placeholderCount);

            return this;
        }

        /**
         * Appends question mark placeholders surrounded by prefix and postfix.
         * Commonly used for {@code IN} clauses with automatic parentheses.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("status IN ").placeholders(3, "(", ")");
         * // Generates: status IN (?, ?, ?)
         * }</pre>
         *
         * <p>If {@code placeholderCount} is {@code 0}, neither {@code prefix} nor {@code postfix} is appended.</p>
         *
         * @param placeholderCount the number of question marks to append (must not be negative)
         * @param prefix the string to add before the question marks (must not be {@code null})
         * @param postfix the string to add after the question marks (must not be {@code null})
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code placeholderCount} is negative, or if {@code prefix} or {@code postfix} is {@code null}
         */
        public WhereClause placeholders(final int placeholderCount, final String prefix, final String postfix) {
            checkOpen();
            N.checkArgNotNegative(placeholderCount, "placeholderCount");
            N.checkArgNotNull(prefix, "prefix");
            N.checkArgNotNull(postfix, "postfix");

            if (placeholderCount > 0) {
                sb.append(prefix);
                appendPlaceholders(placeholderCount);
                sb.append(postfix);
            }

            return this;
        }

        /**
         * Appends {@code placeholderCount} comma-separated {@code ?} placeholders, with no surrounding
         * prefix/postfix. Shared by the {@code placeholders(...)} overloads.
         *
         * @param placeholderCount the number of {@code ?} placeholders to append
         */
        private void appendPlaceholders(final int placeholderCount) {
            for (int i = 0; i < placeholderCount; i++) {
                if (i > 0) {
                    sb.append(", ?");
                } else {
                    sb.append('?');
                }
            }
        }

        /**
         * Adds an {@code AND} condition to the {@code WHERE} clause.
         * If called before any {@link #append(String)}, this acts as the first condition and emits
         * {@code WHERE cond} with no leading {@code AND}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true").and("age >= 18").and("country = ?");
         * // Generates: WHERE active = true AND age >= 18 AND country = ?
         * }</pre>
         *
         * @param cond the condition to add with {@code AND} (must not be {@code null}, empty, or blank)
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public WhereClause and(final String cond) {
            checkOpen();
            checkSqlFragmentNotBlank(cond, "cond");

            if (sb.isEmpty()) {
                sb.append("WHERE ");
            } else {
                sb.append(" AND ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Adds an {@code OR} condition to the {@code WHERE} clause.
         * If called before any {@link #append(String)}, this acts as the first condition and emits
         * {@code WHERE cond} with no leading {@code OR}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("role = 'admin'").or("role = 'moderator'");
         * // Generates: WHERE role = 'admin' OR role = 'moderator'
         * }</pre>
         *
         * @param cond the condition to add with {@code OR} (must not be {@code null}, empty, or blank)
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public WhereClause or(final String cond) {
            checkOpen();
            checkSqlFragmentNotBlank(cond, "cond");

            if (sb.isEmpty()) {
                sb.append("WHERE ");
            } else {
                sb.append(" OR ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Conditionally appends a string to the {@code WHERE} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * where.append("active = true")
         *      .appendIf(filterByDate, "AND created_date > ?");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public WhereClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "WHERE ", " ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code WHERE} clause based on a boolean condition.
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
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link WhereClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public WhereClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "WHERE ", " ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the {@code GROUP BY} clause of a SQL query.
     * Supports adding single or multiple grouping columns.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#groupBy()}
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
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class GroupByClause extends ClauseBuilder {

        GroupByClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a column to the {@code GROUP BY} clause.
         * Automatically adds "GROUP BY " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append("category").append("subcategory");
         * // Generates: GROUP BY category, subcategory
         * }</pre>
         *
         * @param column the column name to group by (must not be {@code null}, empty, or blank)
         * @return this {@link GroupByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code column} is {@code null}, empty, or blank
         */
        public GroupByClause append(final String column) {
            checkSqlFragmentNotBlank(column, "column");

            startClauseFragment(this, "GROUP BY ", ", ");

            sb.append(column);

            return this;
        }

        /**
         * Appends multiple columns to the {@code GROUP BY} clause.
         * Columns are separated by commas. If the collection is empty, this method does nothing.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append(Arrays.asList("year", "quarter", "region"));
         * // Generates: GROUP BY year, quarter, region
         * }</pre>
         *
         * @param columns collection of column names to group by (may be {@code null} or empty;
         *                individual elements must not be {@code null}, empty, or blank)
         * @return this {@link GroupByClause} instance for method chaining
         * @throws IllegalArgumentException if any element in {@code columns} is {@code null}, empty, or blank
         */
        public GroupByClause append(final Collection<String> columns) {
            if (N.isEmpty(columns)) {
                return this;
            }

            checkSqlFragmentsNotBlank(columns, "columns");

            startClauseFragment(this, "GROUP BY ", ", ");

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Conditionally appends a string to the {@code GROUP BY} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * groupBy.append("product_id")
         *        .appendIf(groupByRegion, "region_id");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link GroupByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public GroupByClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "GROUP BY ", ", ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code GROUP BY} clause based on a boolean condition.
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
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link GroupByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public GroupByClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "GROUP BY ", ", ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the {@code HAVING} clause of a SQL query.
     * Used to filter grouped results based on aggregate conditions.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#having()}
     * to get an instance.</p>
     *
     * <h2>Example usage:</h2>
     * <pre>{@code
     * builder.groupBy().append("department");
     * builder.having().append("COUNT(*) > ?")
     *                 .and("AVG(salary) > ?");
     * // Generates: GROUP BY department HAVING COUNT(*) > ? AND AVG(salary) > ?
     * }</pre>
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class HavingClause extends ClauseBuilder {

        HavingClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a condition to the {@code HAVING} clause.
         * Automatically adds "HAVING " prefix on first call.
         *
         * <p>Unlike {@link #and(String)}/{@link #or(String)}, this method does <em>not</em> insert a logical
         * connective — the caller must include any required {@code AND}/{@code OR} in the argument.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("SUM(amount) > 1000");
         * // Generates: HAVING SUM(amount) > 1000
         * }</pre>
         *
         * @param cond the condition to append (must not be {@code null}, empty, or blank)
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public HavingClause append(final String cond) {
            checkSqlFragmentNotBlank(cond, "cond");

            startClauseFragment(this, "HAVING ", " ");

            sb.append(cond);

            return this;
        }

        /**
         * Appends question mark placeholders for parameterized queries.
         * Useful for {@code IN} clauses or multiple parameters in aggregate conditions.
         *
         * <p>Note: this method writes only the {@code ?} characters separated by {@code ", "} into the
         * underlying buffer; it does not insert any surrounding whitespace or parentheses. Use the
         * {@link #placeholders(int, String, String)} overload when you also need a prefix/postfix
         * (e.g. parentheses for an {@code IN} list).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("MAX(score) IN (").placeholders(3).append(")");
         * // sb contents so far: "HAVING MAX(score) IN (?, ?, ? )"
         * // Note: placeholders(int) appends the markers with no leading space, while append(String)
         * // inserts a single space before each fragment, so a manual close paren is preceded by a space.
         * // Prefer placeholders(int, String, String) when you want the parentheses tightly attached.
         * }</pre>
         *
         * @param placeholderCount the number of question marks to append (must not be negative)
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code placeholderCount} is negative
         */
        public HavingClause placeholders(final int placeholderCount) {
            checkOpen();
            N.checkArgNotNegative(placeholderCount, "placeholderCount");

            appendPlaceholders(placeholderCount);

            return this;
        }

        /**
         * Appends question mark placeholders surrounded by prefix and postfix.
         * Commonly used for {@code IN} clauses with automatic parentheses.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("MAX(score) IN ").placeholders(3, "(", ")");
         * // Generates: MAX(score) IN (?, ?, ?)
         * }</pre>
         *
         * <p>If {@code placeholderCount} is {@code 0}, neither {@code prefix} nor {@code postfix} is appended.</p>
         *
         * @param placeholderCount the number of question marks to append (must not be negative)
         * @param prefix the string to add before the question marks (must not be {@code null})
         * @param postfix the string to add after the question marks (must not be {@code null})
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code placeholderCount} is negative, or if {@code prefix} or {@code postfix} is {@code null}
         */
        public HavingClause placeholders(final int placeholderCount, final String prefix, final String postfix) {
            checkOpen();
            N.checkArgNotNegative(placeholderCount, "placeholderCount");
            N.checkArgNotNull(prefix, "prefix");
            N.checkArgNotNull(postfix, "postfix");

            if (placeholderCount > 0) {
                sb.append(prefix);
                appendPlaceholders(placeholderCount);
                sb.append(postfix);
            }

            return this;
        }

        /**
         * Appends {@code placeholderCount} comma-separated {@code ?} placeholders, with no surrounding
         * prefix/postfix. Shared by the {@code placeholders(...)} overloads.
         *
         * @param placeholderCount the number of {@code ?} placeholders to append
         */
        private void appendPlaceholders(final int placeholderCount) {
            for (int i = 0; i < placeholderCount; i++) {
                if (i > 0) {
                    sb.append(", ?");
                } else {
                    sb.append('?');
                }
            }
        }

        /**
         * Adds an {@code AND} condition to the {@code HAVING} clause.
         * If called before any {@link #append(String)}, this acts as the first condition and emits
         * {@code HAVING cond} with no leading {@code AND}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("COUNT(*) > 5").and("MAX(price) < 1000");
         * // Generates: HAVING COUNT(*) > 5 AND MAX(price) < 1000
         * }</pre>
         *
         * @param cond the condition to add with {@code AND} (must not be {@code null}, empty, or blank)
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public HavingClause and(final String cond) {
            checkOpen();
            checkSqlFragmentNotBlank(cond, "cond");

            if (sb.isEmpty()) {
                sb.append("HAVING ");
            } else {
                sb.append(" AND ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Adds an {@code OR} condition to the {@code HAVING} clause.
         * If called before any {@link #append(String)}, this acts as the first condition and emits
         * {@code HAVING cond} with no leading {@code OR}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("MIN(score) > 80").or("AVG(score) > 90");
         * // Generates: HAVING MIN(score) > 80 OR AVG(score) > 90
         * }</pre>
         *
         * @param cond the condition to add with {@code OR} (must not be {@code null}, empty, or blank)
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, empty, or blank
         */
        public HavingClause or(final String cond) {
            checkOpen();
            checkSqlFragmentNotBlank(cond, "cond");

            if (sb.isEmpty()) {
                sb.append("HAVING ");
            } else {
                sb.append(" OR ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Conditionally appends a string to the {@code HAVING} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * having.append("COUNT(*) > 0")
         *       .appendIf(checkRevenue, "AND SUM(revenue) > ?");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public HavingClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "HAVING ", " ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code HAVING} clause based on a boolean condition.
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
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link HavingClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public HavingClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "HAVING ", " ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }

    /**
     * Builder class for constructing the {@code ORDER BY} clause of a SQL query.
     * Supports adding single or multiple columns with sort directions.
     *
     * <p>This class is not meant to be instantiated directly. Use {@link DynamicSqlBuilder#orderBy()}
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
     *
     * <p>Once the owning {@link DynamicSqlBuilder#build()} completes, this clause is closed: its internal buffer is
     * released and any subsequent attempt to append to it throws {@link IllegalStateException}.</p>
     */
    public static class OrderByClause extends ClauseBuilder {

        OrderByClause(final StringBuilder sb) {
            super(sb);
        }

        /**
         * Appends a column (with optional sort direction) to the {@code ORDER BY} clause.
         * Automatically adds "ORDER BY " prefix on first call and comma separators for subsequent columns.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append("created_date DESC").append("name ASC");
         * // Generates: ORDER BY created_date DESC, name ASC
         * }</pre>
         *
         * @param column the column name with optional {@code ASC}/{@code DESC} (must not be {@code null}, empty, or blank)
         * @return this {@link OrderByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code column} is {@code null}, empty, or blank
         */
        public OrderByClause append(final String column) {
            checkSqlFragmentNotBlank(column, "column");

            startClauseFragment(this, "ORDER BY ", ", ");

            sb.append(column);

            return this;
        }

        /**
         * Appends multiple columns to the {@code ORDER BY} clause.
         * Columns are separated by commas. Sort direction can be included with each column.
         * If the collection is empty, this method does nothing.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append(Arrays.asList("year DESC", "month DESC", "day DESC"));
         * // Generates: ORDER BY year DESC, month DESC, day DESC
         * }</pre>
         *
         * @param columns collection of column names with optional sort directions (may be {@code null} or empty;
         *                individual elements must not be {@code null}, empty, or blank)
         * @return this {@link OrderByClause} instance for method chaining
         * @throws IllegalArgumentException if any element in {@code columns} is {@code null}, empty, or blank
         */
        public OrderByClause append(final Collection<String> columns) {
            if (N.isEmpty(columns)) {
                return this;
            }

            checkSqlFragmentsNotBlank(columns, "columns");

            startClauseFragment(this, "ORDER BY ", ", ");

            sb.append(Strings.join(columns, ", "));

            return this;
        }

        /**
         * Conditionally appends a string to the {@code ORDER BY} clause based on a boolean condition.
         * The string is only appended if the condition is true.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * orderBy.append("priority DESC")
         *        .appendIf(sortByDate, "created_date DESC");
         * }</pre>
         *
         * @param condition the condition to check
         * @param textToAppend the string to append if condition is true (must not be {@code null}, empty, or blank when {@code condition} is {@code true})
         * @return this {@link OrderByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code true} and {@code textToAppend} is {@code null}, empty, or blank
         */
        public OrderByClause appendIf(final boolean condition, final String textToAppend) {
            if (condition) {
                checkSqlFragmentNotBlank(textToAppend, "textToAppend");

                startClauseFragment(this, "ORDER BY ", ", ");

                sb.append(textToAppend);
            }

            return this;
        }

        /**
         * Appends one of two strings to the {@code ORDER BY} clause based on a boolean condition.
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
         * @param textToAppendWhenTrue the string to append if condition is true (must not be {@code null}, empty, or blank)
         * @param textToAppendWhenFalse the string to append if condition is false (must not be {@code null}, empty, or blank)
         * @return this {@link OrderByClause} instance for method chaining
         * @throws IllegalArgumentException if {@code textToAppendWhenTrue} or {@code textToAppendWhenFalse} is {@code null}, empty, or blank
         */
        public OrderByClause appendIfOrElse(final boolean condition, final String textToAppendWhenTrue, final String textToAppendWhenFalse) {
            checkSqlFragmentNotBlank(textToAppendWhenTrue, "textToAppendWhenTrue");
            checkSqlFragmentNotBlank(textToAppendWhenFalse, "textToAppendWhenFalse");

            startClauseFragment(this, "ORDER BY ", ", ");

            if (condition) {
                sb.append(textToAppendWhenTrue);
            } else {
                sb.append(textToAppendWhenFalse);
            }

            return this;
        }
    }
}
