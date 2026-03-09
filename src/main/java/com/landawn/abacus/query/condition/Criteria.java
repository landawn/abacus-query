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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * An immutable container representing a complete SQL query structure composed of multiple clauses
 * ({@link Join}, {@link Where}, {@link GroupBy}, {@link Having}, {@link OrderBy}, {@link Limit},
 * and set operations like {@link Union}/{@link Intersect}/{@link Except}).
 *
 * <p>Instances are created via {@link #builder()}. Each clause is independent and should not
 * be nested inside another clause; compose multiple clauses within a single {@code Criteria}.</p>
 *
 * <pre>{@code
 * Criteria criteria = Criteria.builder()
 *     .join("orders", new On("users.id", "orders.user_id"))
 *     .where(Filters.and(
 *         Filters.equal("users.status", "active"),
 *         Filters.greaterThan("orders.amount", 100)
 *     ))
 *     .groupBy("users.department")
 *     .having(Filters.greaterThan("COUNT(*)", 5))
 *     .orderBy("COUNT(*)", SortDirection.DESC)
 *     .limit(10)
 *     .build();
 * }</pre>
 *
 * @see Condition
 * @see Filters
 * @see Clause
 */
public class Criteria extends AbstractCondition {

    private static final Set<Operator> SET_OPERATORS = N.newHashSet();

    static {
        SET_OPERATORS.add(Operator.UNION_ALL);
        SET_OPERATORS.add(Operator.UNION);
        SET_OPERATORS.add(Operator.INTERSECT);
        SET_OPERATORS.add(Operator.EXCEPT);
        SET_OPERATORS.add(Operator.MINUS);
    }

    private String selectModifier = null;

    private final List<Condition> conditions;

    /**
     * Creates a new Criteria instance with the specified select modifier and condition list.
     *
     * @param selectModifier the SELECT modifier (e.g., DISTINCT), or {@code null} for none
     * @param conditions the list of conditions representing the query clauses
     */
    Criteria(String selectModifier, List<Condition> conditions) {
        super(Operator.EMPTY);
        this.selectModifier = selectModifier;
        this.conditions = conditions;
    }

    /**
     * Returns the SELECT modifier (e.g., {@code DISTINCT}, {@code DISTINCTROW}),
     * or {@code null} if none was set.
     *
     * @return the SELECT modifier, or {@code null} if not set
     */
    public String getSelectModifier() {
        return selectModifier;
    }

    /**
     * Returns all JOIN clauses (INNER, LEFT, RIGHT, FULL, CROSS, NATURAL) in the order they were added.
     *
     * @return an unmodifiable list of {@link Join} conditions; empty if none exist
     */
    public List<Join> getJoins() {
        final List<Join> joins = new ArrayList<>();

        for (final Condition cond : this.conditions) {
            if (cond instanceof Join) {
                joins.add((Join) cond);
            }
        }

        return Collections.unmodifiableList(joins);
    }

    /**
     * Returns the WHERE clause, or {@code null} if none was set.
     *
     * @return the {@link Where} clause, or {@code null}
     */
    public Clause getWhere() {
        return (Clause) find(Operator.WHERE);
    }

    /**
     * Returns the GROUP BY clause, or {@code null} if none was set.
     *
     * @return the {@link GroupBy} clause, or {@code null}
     */
    public Clause getGroupBy() {
        return (Clause) find(Operator.GROUP_BY);
    }

    /**
     * Returns the HAVING clause, or {@code null} if none was set.
     *
     * @return the {@link Having} clause, or {@code null}
     */
    public Clause getHaving() {
        return (Clause) find(Operator.HAVING);
    }

    /**
     * Returns all set operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS) in the order they were added.
     *
     * @return an unmodifiable list of set operation clauses; empty if none exist
     */
    public List<Clause> getSetOperations() {
        List<Clause> result = null;

        for (final Condition cond : this.conditions) {
            if (SET_OPERATORS.contains(cond.operator())) {
                if (result == null) {
                    result = new ArrayList<>();
                }

                result.add((Clause) cond);
            }
        }

        return result == null ? N.emptyList() : Collections.unmodifiableList(result);
    }

    /**
     * Returns the ORDER BY clause, or {@code null} if none was set.
     *
     * @return the {@link OrderBy} clause, or {@code null}
     */
    public Clause getOrderBy() {
        return (Clause) find(Operator.ORDER_BY);
    }

    /**
     * Returns the LIMIT clause, or {@code null} if none was set.
     *
     * @return the {@link Limit} clause, or {@code null}
     */
    public Limit getLimit() {
        return (Limit) find(Operator.LIMIT);
    }

    /**
     * Returns all conditions (clauses) in this criteria in the order they were added.
     *
     * @return an unmodifiable list of all conditions
     */
    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * Returns all conditions whose {@link Condition#operator()} matches the given operator.
     *
     * @param operator the operator to match
     * @return an unmodifiable list of matching conditions; empty if none found
     */
    public List<Condition> findConditions(final Operator operator) {
        final List<Condition> result = new ArrayList<>();

        for (final Condition cond : this.conditions) {
            if (cond.operator() == operator) {
                result.add(cond);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Collects parameters from all conditions in SQL clause order:
     * JOIN, WHERE, GROUP BY, HAVING, set operations, ORDER BY, LIMIT.
     *
     * @return an immutable list of all parameters
     */
    @Override
    public ImmutableList<Object> getParameters() {
        if (this.conditions.size() > 0) {
            final List<Object> parameters = new ArrayList<>();
            final Collection<Join> joins = getJoins();

            for (final Join join : joins) {
                parameters.addAll(join.getParameters());
            }

            final Clause where = getWhere();

            if (where != null) {
                parameters.addAll(where.getParameters());
            }

            final Clause groupBy = getGroupBy();

            if (groupBy != null) {
                parameters.addAll(groupBy.getParameters());
            }
            final Clause having = getHaving();

            if (having != null) {
                parameters.addAll(having.getParameters());
            }

            final List<Clause> setOperations = getSetOperations();

            for (final Condition cond : setOperations) {
                parameters.addAll(cond.getParameters());
            }

            final Clause orderBy = getOrderBy();

            if (orderBy != null) {
                parameters.addAll(orderBy.getParameters());
            }

            final Clause limit = getLimit();

            if (limit != null) {
                parameters.addAll(limit.getParameters());
            }

            return ImmutableList.wrap(parameters);
        } else {
            return ImmutableList.empty();
        }
    }

    /**
     * Clears parameter values from all conditions to release memory.
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : this.conditions) {
            condition.clearParameters();
        }
    }

    /**
     * Returns a string representation of this Criteria using the specified naming policy.
     * The output follows SQL clause ordering conventions and includes all conditions.
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this Criteria
     */
    @SuppressWarnings("StringConcatenationInLoop")
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final String selectModifier = Strings.isEmpty(this.selectModifier) ? Strings.EMPTY : SK.SPACE + this.selectModifier; //NOSONAR
        String join = Strings.EMPTY;
        String where = Strings.EMPTY;
        String groupBy = Strings.EMPTY;
        String having = Strings.EMPTY;
        String orderBy = Strings.EMPTY;
        String limit = Strings.EMPTY;
        String setOps = Strings.EMPTY;

        for (final Condition cond : this.conditions) {
            if (cond.operator() == Operator.WHERE) {
                where += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.operator() == Operator.ORDER_BY) {
                orderBy += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.operator() == Operator.GROUP_BY) {
                groupBy += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.operator() == Operator.HAVING) {
                having += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.operator() == Operator.LIMIT) {
                limit += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond instanceof Join) {
                join += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else {
                setOps += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            }
        }

        return selectModifier + join + where + groupBy + having + setOps + orderBy + limit;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (Strings.isEmpty(selectModifier) ? 0 : selectModifier.hashCode());
        return (h * 31) + conditions.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj instanceof Criteria && N.equals(((Criteria) obj).selectModifier, selectModifier) && N.equals(((Criteria) obj).conditions, conditions));
    }

    private Condition find(final Operator operator) {
        return findConditionByOperator(conditions, operator);
    }

    private static Condition findConditionByOperator(final List<Condition> conds, final Operator operator) {
        for (final Condition cond : conds) {
            if (cond.operator() == operator) {
                return cond;
            }
        }

        return null;
    }

    /**
     * Creates a new {@link Builder} pre-populated with this criteria's select modifier and conditions.
     *
     * @return a new mutable Builder initialized from this criteria
     */
    public Builder toBuilder() {
        final Builder builder = new Builder();

        builder.selectModifier(this.selectModifier);
        builder.add(this.conditions);

        return builder;
    }

    /**
     * Creates a new Criteria builder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = Criteria.builder()
     *     .where(Filters.equal("status", "active"))
     *     .orderBy("name")
     *     .limit(50)
     *     .build();
     * }</pre>
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A mutable builder for constructing {@link Criteria} instances with a fluent API.
     *
     * <pre>{@code
     * Criteria criteria = Criteria.builder()
     *     .where(Filters.equal("status", "active"))
     *     .orderBy("name")
     *     .limit(50)
     *     .build();
     * }</pre>
     *
     * <p>For single-clause clauses (WHERE, GROUP BY, HAVING, ORDER BY, LIMIT),
     * calling the same method again replaces the previous clause.
     * For JOINs and set operations, multiple calls accumulate.</p>
     */
    @Beta
    public static final class Builder {

        private String selectModifier = null;

        private final List<Condition> conditions = new ArrayList<>();

        Builder() {
            // utility/builder class
        }

        /**
         * Sets the DISTINCT modifier for the query.
         * DISTINCT removes duplicate rows from the result set.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .distinct()
         *     .where(Filters.equal("status", "active"));
         * // Results in: SELECT DISTINCT ... WHERE status = 'active'
         * }</pre>
         * 
         * @return this Builder instance for method chaining
         */
        public Builder distinct() {
            selectModifier = SK.DISTINCT;

            return this;
        }

        /**
         * Sets the DISTINCT modifier with specific columns.
         * Only the specified columns are considered for duplicate removal.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .distinctBy("department, location");
         * // Results in: SELECT DISTINCT(department, location) ...
         * 
         * // Or with column list
         * criteria.distinctBy("city");
         * // Results in: SELECT DISTINCT(city) ...
         * }</pre>
         * 
         * @param columnNames the columns to apply DISTINCT to
         * @return this Builder instance for method chaining
         */
        public Builder distinctBy(final String columnNames) {
            selectModifier = Strings.isEmpty(columnNames) ? SK.DISTINCT : SK.DISTINCT + "(" + columnNames + ")";

            return this;
        }

        /**
         * Sets the DISTINCTROW modifier for the query.
         * DISTINCTROW is similar to DISTINCT but may have database-specific behavior.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .distinctRow()
         *     .where(Filters.equal("active", true));
         * // Results in: SELECT DISTINCTROW ... WHERE active = true
         * }</pre>
         * 
         * @return this Builder instance for method chaining
         */
        public Builder distinctRow() {
            selectModifier = SK.DISTINCTROW;

            return this;
        }

        /**
         * Sets the DISTINCTROW modifier with specific columns.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .distinctRowBy("category, subcategory");
         * // Results in: SELECT DISTINCTROW(category, subcategory) ...
         * }</pre>
         * 
         * @param columnNames the columns to apply DISTINCTROW to
         * @return this Builder instance for method chaining
         */
        public Builder distinctRowBy(final String columnNames) {
            selectModifier = Strings.isEmpty(columnNames) ? SK.DISTINCTROW : SK.DISTINCTROW + "(" + columnNames + ")";

            return this;
        }

        /**
         * Sets a custom SELECT modifier.
         * This allows for database-specific modifiers not covered by other methods.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .selectModifier("SQL_CALC_FOUND_ROWS")
         *     .where(Filters.equal("active", true));
         * // Results in: SELECT SQL_CALC_FOUND_ROWS ... WHERE active = true
         *
         * // MySQL-specific modifier
         * criteria.selectModifier("SQL_NO_CACHE");
         * }</pre>
         *
         * @param selectModifier the custom SELECT modifier
         * @return this Builder instance for method chaining
         */
        public Builder selectModifier(final String selectModifier) {
            this.selectModifier = selectModifier;

            return this;
        }

        /**
         * Adds JOIN clauses to this criteria.
         * Multiple joins can be added in a single call.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .join(
         *         new LeftJoin("orders", new On("users.id", "orders.user_id")),
         *         new InnerJoin("products", new On("orders.product_id", "products.id"))
         *     );
         * }</pre>
         * 
         * @param joins the JOIN clauses to add
         * @return this Builder instance for method chaining
         */
        public final Builder join(final Join... joins) {
            add(joins);

            return this;
        }

        /**
         * Adds JOIN clauses to this criteria.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Join> joins = Arrays.asList(
         *     new LeftJoin("orders", condition1),
         *     new RightJoin("payments", condition2)
         * );
         * criteria.join(joins);
         * }</pre>
         * 
         * @param joins the collection of JOIN clauses to add
         * @return this Builder instance for method chaining
         */
        public Builder join(final Collection<Join> joins) {
            add(joins);

            return this;
        }

        /**
         * Adds a simple INNER JOIN to this criteria.
         * This creates a join without an explicit condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .join("orders")
         *     .where(Filters.equal("users.id", "orders.user_id"));
         * // Results in: JOIN orders WHERE users.id = orders.user_id
         * }</pre>
         * 
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder join(final String joinEntity) {
            add(new Join(joinEntity));

            return this;
        }

        /**
         * Adds an INNER JOIN with a condition to this criteria.
         * This is the most common form of join.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .join("orders", new On("users.id", "orders.user_id"))
         *     .where(Filters.equal("users.status", "active"));
         * // Results in: JOIN orders ON users.id = orders.user_id WHERE users.status = 'active'
         * }</pre>
         * 
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder join(final String joinEntity, final Condition cond) {
            add(new Join(joinEntity, cond));

            return this;
        }

        /**
         * Adds an INNER JOIN with multiple entities and a condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Collection<String> tables = Arrays.asList("orders", "order_items");
         * Criteria criteria = Criteria.builder()
         *     .join(tables, new On("id", "order_id"));
         * }</pre>
         * 
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder join(final Collection<String> joinEntities, final Condition cond) {
            add(new Join(joinEntities, cond));

            return this;
        }

        /**
         * Adds an INNER JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .innerJoin("orders")
         *     .where(Filters.eq("users.id", "orders.user_id"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder innerJoin(final String joinEntity) {
            add(new InnerJoin(joinEntity));

            return this;
        }

        /**
         * Adds an INNER JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .innerJoin("orders", Filters.on("users.id", "orders.user_id"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder innerJoin(final String joinEntity, final Condition cond) {
            add(new InnerJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds an INNER JOIN with multiple entities and a condition.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .innerJoin(Arrays.asList("orders", "order_items"), Filters.on("id", "order_id"));
         * }</pre>
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder innerJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new InnerJoin(joinEntities, cond));

            return this;
        }

        /**
         * Adds a LEFT JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .leftJoin("orders");
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder leftJoin(final String joinEntity) {
            add(new LeftJoin(joinEntity));

            return this;
        }

        /**
         * Adds a LEFT JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .leftJoin("orders", Filters.on("users.id", "orders.user_id"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder leftJoin(final String joinEntity, final Condition cond) {
            add(new LeftJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds a LEFT JOIN with multiple entities and a condition.
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder leftJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new LeftJoin(joinEntities, cond));

            return this;
        }

        /**
         * Adds a RIGHT JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .rightJoin("orders");
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder rightJoin(final String joinEntity) {
            add(new RightJoin(joinEntity));

            return this;
        }

        /**
         * Adds a RIGHT JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .rightJoin("orders", Filters.on("users.id", "orders.user_id"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder rightJoin(final String joinEntity, final Condition cond) {
            add(new RightJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds a RIGHT JOIN with multiple entities and a condition.
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder rightJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new RightJoin(joinEntities, cond));

            return this;
        }

        /**
         * Adds a FULL JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .fullJoin("orders");
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder fullJoin(final String joinEntity) {
            add(new FullJoin(joinEntity));

            return this;
        }

        /**
         * Adds a FULL JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .fullJoin("orders", Filters.on("users.id", "orders.user_id"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder fullJoin(final String joinEntity, final Condition cond) {
            add(new FullJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds a FULL JOIN with multiple entities and a condition.
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder fullJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new FullJoin(joinEntities, cond));

            return this;
        }

        /**
         * Adds a CROSS JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .crossJoin("colors");
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder crossJoin(final String joinEntity) {
            add(new CrossJoin(joinEntity));

            return this;
        }

        /**
         * Adds a CROSS JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .crossJoin("colors", Filters.eq("active", true));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder crossJoin(final String joinEntity, final Condition cond) {
            add(new CrossJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds a CROSS JOIN with multiple entities and a condition.
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder crossJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new CrossJoin(joinEntities, cond));

            return this;
        }

        /**
         * Adds a NATURAL JOIN to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .naturalJoin("employees");
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @return this Builder instance for method chaining
         */
        public Builder naturalJoin(final String joinEntity) {
            add(new NaturalJoin(joinEntity));

            return this;
        }

        /**
         * Adds a NATURAL JOIN with a condition to this criteria.
         *
         * <pre>{@code
         * Criteria criteria = Filters.criteria()
         *     .naturalJoin("employees", Filters.eq("status", "active"));
         * }</pre>
         *
         * @param joinEntity the table or entity to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder naturalJoin(final String joinEntity, final Condition cond) {
            add(new NaturalJoin(joinEntity, cond));

            return this;
        }

        /**
         * Adds a NATURAL JOIN with multiple entities and a condition.
         *
         * @param joinEntities the collection of tables/entities to join
         * @param cond the join condition
         * @return this Builder instance for method chaining
         */
        public Builder naturalJoin(final Collection<String> joinEntities, final Condition cond) {
            add(new NaturalJoin(joinEntities, cond));

            return this;
        }

        /**
         * Sets or replaces the WHERE clause.
         * If a WHERE clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.and(
         *         Filters.equal("status", "active"),
         *         Filters.greaterThan("age", 18),
         *         Filters.like("email", "%@company.com")
         *     ));
         * }</pre>
         * 
         * @param cond the WHERE condition
         * @return this Builder instance for method chaining
         */
        public Builder where(final Condition cond) {
            if (cond == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }

            validateClauseCondition(cond, Operator.WHERE, "where");

            if (cond.operator() == Operator.WHERE) {
                add(cond);
            } else {
                add(new Where(cond));
            }

            return this;
        }

        /**
         * Sets or replaces the WHERE clause using a string expression.
         * Useful for complex conditions that are easier to express as SQL.
         * If a WHERE clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .where("age > 18 AND status = 'active'");
         * 
         * // Complex expression
         * criteria.where("YEAR(created_date) = 2024 OR special_flag = true");
         * }</pre>
         * 
         * @param expr the WHERE condition as a string
         * @return this Builder instance for method chaining
         */
        public Builder where(final String expr) {
            N.checkArgNotEmpty(expr, "expr");

            add(new Where(Filters.expr(expr)));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy(Filters.expr("YEAR(order_date), MONTH(order_date)"));
         * }</pre>
         * 
         * @param cond the GROUP BY condition
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final Condition cond) {
            if (cond == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }

            validateClauseCondition(cond, Operator.GROUP_BY, "groupBy");

            if (cond.operator() == Operator.GROUP_BY) {
                add(cond);
            } else {
                add(new GroupBy(cond));
            }

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with property names.
         * Groups results by the specified columns in ascending order.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("department", "location", "role");
         * // Results in: GROUP BY department, location, role
         * }</pre>
         * 
         * @param propNames the property names to group by
         * @return this Builder instance for method chaining
         */
        public final Builder groupBy(final String... propNames) {
            add(new GroupBy(propNames));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with a property and sort direction.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("total_sales", SortDirection.DESC);
         * // Results in: GROUP BY total_sales DESC
         * }</pre>
         * 
         * @param propName the property name to group by
         * @param direction the sort direction
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final String propName, final SortDirection direction) {
            add(new GroupBy(propName, direction));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with two properties and their sort directions.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
         * // Results in: GROUP BY year DESC, month ASC
         * }</pre>
         * 
         * @param propName the first property name to group by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to group by
         * @param direction2 the sort direction for the second property
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2) {
            groupBy(N.asMap(propName, direction, propName2, direction2));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with three properties and their sort directions.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("country", SortDirection.ASC, "state", SortDirection.ASC, "city", SortDirection.DESC);
         * // Results in: GROUP BY country ASC, state ASC, city DESC
         * }</pre>
         * 
         * @param propName the first property name to group by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to group by
         * @param direction2 the sort direction for the second property
         * @param propName3 the third property name to group by
         * @param direction3 the sort direction for the third property
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
                final String propName3, final SortDirection direction3) {
            groupBy(N.asMap(propName, direction, propName2, direction2, propName3, direction3));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with multiple properties.
         * All properties will be sorted in ascending order.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> groupCols = Arrays.asList("region", "product_type");
         * Criteria criteria = Criteria.builder()
         *     .groupBy(groupCols);
         * // Results in: GROUP BY region, product_type
         * }</pre>
         * 
         * @param propNames the collection of property names to group by
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final Collection<String> propNames) {
            return groupBy(propNames, SortDirection.ASC);
        }

        /**
         * Sets or replaces the GROUP BY clause with multiple properties and sort direction.
         * All properties will use the same sort direction.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> groupCols = new HashSet<>(Arrays.asList("category", "brand"));
         * Criteria criteria = Criteria.builder()
         *     .groupBy(groupCols, SortDirection.DESC);
         * // Results in: GROUP BY category DESC, brand DESC
         * }</pre>
         * 
         * @param propNames the collection of property names to group by
         * @param direction the sort direction for all properties
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final Collection<String> propNames, final SortDirection direction) {
            add(new GroupBy(propNames, direction));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with custom sort directions per property.
         * The map should be a LinkedHashMap to preserve order.
         * If a GROUP BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, SortDirection> grouping = new LinkedHashMap<>();
         * grouping.put("department", SortDirection.ASC);
         * grouping.put("salary_range", SortDirection.DESC);
         * grouping.put("years_experience", SortDirection.DESC);
         * Criteria criteria = Criteria.builder()
         *     .groupBy(grouping);
         * }</pre>
         * 
         * @param orders a map of property names to sort directions
         * @return this Builder instance for method chaining
         */
        public Builder groupBy(final Map<String, SortDirection> orders) {
            add(new GroupBy(orders));

            return this;
        }

        /**
         * Sets or replaces the HAVING clause.
         * HAVING is used to filter grouped results after GROUP BY.
         * If a HAVING clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("department")
         *     .having(Filters.and(
         *         Filters.greaterThan("COUNT(*)", 10),
         *         Filters.lessThan("AVG(salary)", 100000)
         *     ));
         * }</pre>
         * 
         * @param cond the HAVING condition
         * @return this Builder instance for method chaining
         */
        public Builder having(final Condition cond) {
            if (cond == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }

            validateClauseCondition(cond, Operator.HAVING, "having");

            if (cond.operator() == Operator.HAVING) {
                add(cond);
            } else {
                add(new Having(cond));
            }

            return this;
        }

        /**
         * Sets or replaces the HAVING clause using a string expression.
         * Useful for aggregate function conditions.
         * If a HAVING clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .groupBy("product_category")
         *     .having("SUM(revenue) > 10000 AND COUNT(*) > 5");
         * }</pre>
         * 
         * @param expr the HAVING condition as a string
         * @return this Builder instance for method chaining
         */
        public Builder having(final String expr) {
            N.checkArgNotEmpty(expr, "expr");

            add(new Having(Filters.expr(expr)));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with ascending order.
         * Convenience method that sorts all specified columns in ascending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderByAsc("lastName", "firstName", "middleName");
         * // Results in: ORDER BY lastName ASC, firstName ASC, middleName ASC
         * }</pre>
         * 
         * @param propNames the property names to order by ascending
         * @return this Builder instance for method chaining
         */
        public Builder orderByAsc(final String... propNames) {
            add(Filters.orderByAsc(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with ascending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> sortCols = Arrays.asList("country", "state", "city");
         * Criteria criteria = Criteria.builder()
         *     .orderByAsc(sortCols);
         * }</pre>
         * 
         * @param propNames the collection of property names to order by ascending
         * @return this Builder instance for method chaining
         */
        public Builder orderByAsc(final Collection<String> propNames) {
            add(Filters.orderByAsc(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with descending order.
         * Convenience method that sorts all specified columns in descending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderByDesc("score", "createdDate");
         * // Results in: ORDER BY score DESC, createdDate DESC
         * }</pre>
         * 
         * @param propNames the property names to order by descending
         * @return this Builder instance for method chaining
         */
        public Builder orderByDesc(final String... propNames) {
            add(Filters.orderByDesc(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with descending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> sortCols = new HashSet<>(Arrays.asList("revenue", "profit"));
         * Criteria criteria = Criteria.builder()
         *     .orderByDesc(sortCols);
         * }</pre>
         * 
         * @param propNames the collection of property names to order by descending
         * @return this Builder instance for method chaining
         */
        public Builder orderByDesc(final Collection<String> propNames) {
            add(Filters.orderByDesc(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Complex ordering expression
         * Criteria criteria = Criteria.builder()
         *     .orderBy(Filters.expr("CASE WHEN priority = 'HIGH' THEN 1 ELSE 2 END, created_date DESC"));
         * }</pre>
         * 
         * @param cond the ORDER BY condition
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final Condition cond) {
            if (cond == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }

            validateClauseCondition(cond, Operator.ORDER_BY, "orderBy");

            if (cond.operator() == Operator.ORDER_BY) {
                add(cond);
            } else {
                add(new OrderBy(cond));
            }

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with property names.
         * Orders by the specified columns in ascending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderBy("department", "lastName", "firstName");
         * // Results in: ORDER BY department, lastName, firstName
         * }</pre>
         * 
         * @param propNames the property names to order by
         * @return this Builder instance for method chaining
         */
        public final Builder orderBy(final String... propNames) {
            add(new OrderBy(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with a property and sort direction.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderBy("createdDate", SortDirection.DESC);
         * // Results in: ORDER BY createdDate DESC
         * }</pre>
         * 
         * @param propName the property name to order by
         * @param direction the sort direction
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final String propName, final SortDirection direction) {
            add(new OrderBy(propName, direction));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with two properties and their sort directions.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderBy("priority", SortDirection.DESC, "createdDate", SortDirection.ASC);
         * // Results in: ORDER BY priority DESC, createdDate ASC
         * }</pre>
         * 
         * @param propName the first property name to order by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to order by
         * @param direction2 the sort direction for the second property
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2) {
            orderBy(N.asMap(propName, direction, propName2, direction2));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with three properties and their sort directions.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .orderBy("category", SortDirection.ASC, "price", SortDirection.DESC, "name", SortDirection.ASC);
         * // Results in: ORDER BY category ASC, price DESC, name ASC
         * }</pre>
         * 
         * @param propName the first property name to order by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to order by
         * @param direction2 the sort direction for the second property
         * @param propName3 the third property name to order by
         * @param direction3 the sort direction for the third property
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
                final String propName3, final SortDirection direction3) {
            orderBy(N.asMap(propName, direction, propName2, direction2, propName3, direction3));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with multiple properties.
         * All properties will be sorted in ascending order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> sortCols = Arrays.asList("country", "state", "city");
         * Criteria criteria = Criteria.builder()
         *     .orderBy(sortCols);
         * // Results in: ORDER BY country, state, city
         * }</pre>
         * 
         * @param propNames the collection of property names to order by
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final Collection<String> propNames) {
            return orderBy(propNames, SortDirection.ASC);
        }

        /**
         * Sets or replaces the ORDER BY clause with multiple properties and sort direction.
         * All properties will use the same sort direction.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> sortCols = new HashSet<>(Arrays.asList("score", "rating"));
         * Criteria criteria = Criteria.builder()
         *     .orderBy(sortCols, SortDirection.DESC);
         * // Results in: ORDER BY score DESC, rating DESC
         * }</pre>
         * 
         * @param propNames the collection of property names to order by
         * @param direction the sort direction for all properties
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final Collection<String> propNames, final SortDirection direction) {
            add(new OrderBy(propNames, direction));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with custom sort directions per property.
         * The map should be a LinkedHashMap to preserve order.
         * If an ORDER BY clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, SortDirection> ordering = new LinkedHashMap<>();
         * ordering.put("priority", SortDirection.DESC);
         * ordering.put("createdDate", SortDirection.DESC);
         * ordering.put("name", SortDirection.ASC);
         * Criteria criteria = Criteria.builder()
         *     .orderBy(ordering);
         * }</pre>
         * 
         * @param orders a map of property names to sort directions
         * @return this Builder instance for method chaining
         */
        public Builder orderBy(final Map<String, SortDirection> orders) {
            add(new OrderBy(orders));

            return this;
        }

        /**
         * Sets or replaces the LIMIT clause.
         * If a LIMIT clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Limit customLimit = Filters.limit(100);
         * Criteria criteria = Criteria.builder()
         *     .limit(customLimit);
         * }</pre>
         * 
         * @param condition the LIMIT condition
         * @return this Builder instance for method chaining
         */
        public Builder limit(final Limit condition) {
            add(condition);

            return this;
        }

        /**
         * Sets or replaces the LIMIT clause with a count.
         * Limits the number of rows returned by the query.
         * If a LIMIT clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("status", "active"))
         *     .limit(10);
         * // Results in: WHERE status = 'active' LIMIT 10
         * }</pre>
         * 
         * @param count the maximum number of results to return
         * @return this Builder instance for method chaining
         */
        public Builder limit(final int count) {
            add(Filters.limit(count));

            return this;
        }

        /**
         * Sets or replaces the LIMIT clause with count and offset.
         * Used for pagination - returns up to 'count' rows, skipping 'offset' rows.
         * If a LIMIT clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Page 3 with 20 items per page (take 20, skip 40)
         * Criteria criteria = Criteria.builder()
         *     .orderBy("id")
         *     .limit(20, 40);
         * // Results in: ORDER BY id LIMIT 20 OFFSET 40
         * }</pre>
         *
         * @param count the maximum number of results to return
         * @param offset the number of rows to skip
         * @return this Builder instance for method chaining
         */
        public Builder limit(final int count, final int offset) {
            add(Filters.limit(count, offset));

            return this;
        }

        /**
         * Sets or replaces the LIMIT clause using a string expression.
         * Allows for database-specific limit syntax.
         * If a LIMIT clause already exists, it will be replaced.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .limit("10 OFFSET 20");
         * 
         * // Or with parameters
         * criteria.limit("? OFFSET ?");
         * }</pre>
         * 
         * @param expr the LIMIT expression as a string
         * @return this Builder instance for method chaining
         */
        public Builder limit(final String expr) {
            add(Filters.limit(expr));

            return this;
        }

        /**
         * Adds a UNION operation with a subquery.
         * UNION combines results and removes duplicates.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SubQuery archivedUsers = Filters.subQuery("SELECT * FROM archived_users WHERE active = true");
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("status", "active"))
         *     .union(archivedUsers);
         * // Returns active users from both current and archived tables
         * }</pre>
         * 
         * @param subQuery the subquery to union with
         * @return this Builder instance for method chaining
         */
        public Builder union(final SubQuery subQuery) {
            add(new Union(subQuery));

            return this;
        }

        /**
         * Adds a UNION ALL operation with a subquery.
         * UNION ALL combines results and keeps duplicates.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SubQuery pendingOrders = Filters.subQuery("SELECT * FROM pending_orders");
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("status", "completed"))
         *     .unionAll(pendingOrders);
         * // Returns all orders, including duplicates if any exist
         * }</pre>
         * 
         * @param subQuery the subquery to union with
         * @return this Builder instance for method chaining
         */
        public Builder unionAll(final SubQuery subQuery) {
            add(new UnionAll(subQuery));

            return this;
        }

        /**
         * Adds an INTERSECT operation with a subquery.
         * INTERSECT returns only rows that appear in both result sets.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SubQuery premiumUsers = Filters.subQuery("SELECT user_id FROM premium_members");
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("active", true))
         *     .intersect(premiumUsers);
         * // Returns only active users who are also premium members
         * }</pre>
         * 
         * @param subQuery the subquery to intersect with
         * @return this Builder instance for method chaining
         */
        public Builder intersect(final SubQuery subQuery) {
            add(new Intersect(subQuery));

            return this;
        }

        /**
         * Adds an EXCEPT operation with a subquery.
         * EXCEPT returns rows from the first query that don't appear in the second.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SubQuery excludedUsers = Filters.subQuery("SELECT user_id FROM blacklist");
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("status", "active"))
         *     .except(excludedUsers);
         * // Returns active users who are not in the blacklist
         * }</pre>
         * 
         * @param subQuery the subquery to except
         * @return this Builder instance for method chaining
         */
        public Builder except(final SubQuery subQuery) {
            add(new Except(subQuery));

            return this;
        }

        /**
         * Adds a MINUS operation with a subquery.
         * MINUS is equivalent to EXCEPT in some databases (like Oracle).
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SubQuery inactiveUsers = Filters.subQuery("SELECT user_id FROM inactive_users");
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("registered", true))
         *     .minus(inactiveUsers);
         * // Returns registered users minus inactive ones
         * }</pre>
         * 
         * @param subQuery the subquery to minus
         * @return this Builder instance for method chaining
         */
        public Builder minus(final SubQuery subQuery) {
            add(new Minus(subQuery));

            return this;
        }

        private void validateClauseCondition(final Condition cond, final Operator expectedOperator, final String methodName) {
            if (cond instanceof Criteria) {
                throw new IllegalArgumentException("Invalid condition for " + methodName + ": nested Criteria is not supported");
            }

            if (cond.operator() == Operator.ON || cond.operator() == Operator.USING) {
                throw new IllegalArgumentException("Invalid condition for " + methodName + ": ON/USING conditions are not supported");
            }

            if (isClause(cond.operator())) {
                if (cond.operator() != expectedOperator) {
                    throw new IllegalArgumentException(
                            "Invalid condition for " + methodName + ": expected " + expectedOperator + " or non-clause condition, but got " + cond.operator());
                }

                if (!(cond instanceof Clause)) {
                    throw new IllegalArgumentException("Invalid condition for " + methodName + ": operator " + expectedOperator + " requires a Clause");
                }
            }
        }

        /**
         *
         * @param conditions the conditions to validate
         */
        private void checkConditions(final Collection<? extends Condition> conditions) {
            N.checkArgNotNull(conditions, "conditions");

            for (final Condition cond : conditions) {
                checkCondition(cond);
            }
        }

        /**
         *
         * @param conditions the conditions to validate
         */
        private void checkConditions(final Condition... conditions) {
            N.checkArgNotNull(conditions, "conditions");

            for (final Condition cond : conditions) {
                checkCondition(cond);
            }
        }

        private void checkCondition(final Condition cond) {
            if (cond == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }

            if (!isClause(cond.operator())) {
                throw new IllegalArgumentException(
                        "Invalid operator '" + cond.operator() + "' for Criteria. Expected clause operators: WHERE, GROUP_BY, HAVING, ORDER_BY, LIMIT, etc.");
            }

            if ((cond.operator() == Operator.WHERE || cond.operator() == Operator.GROUP_BY || cond.operator() == Operator.HAVING
                    || cond.operator() == Operator.ORDER_BY || cond.operator() == Operator.LIMIT || cond.operator() == Operator.UNION
                    || cond.operator() == Operator.UNION_ALL || cond.operator() == Operator.INTERSECT || cond.operator() == Operator.EXCEPT
                    || cond.operator() == Operator.MINUS) && !(cond instanceof Clause)) {
                throw new IllegalArgumentException("Condition with operator '" + cond.operator() + "' must be an instance of Clause");
            }

            if ((cond.operator() == Operator.JOIN || cond.operator() == Operator.LEFT_JOIN || cond.operator() == Operator.RIGHT_JOIN
                    || cond.operator() == Operator.FULL_JOIN || cond.operator() == Operator.CROSS_JOIN || cond.operator() == Operator.INNER_JOIN
                    || cond.operator() == Operator.NATURAL_JOIN) && !(cond instanceof Join)) {
                throw new IllegalArgumentException("Condition with operator '" + cond.operator() + "' must be an instance of Join");
            }
        }

        private void addCondition(final Condition cond) {
            if (cond.operator() == Operator.WHERE || cond.operator() == Operator.ORDER_BY || cond.operator() == Operator.GROUP_BY
                    || cond.operator() == Operator.HAVING || cond.operator() == Operator.LIMIT) {

                final Condition clause = findConditionByOperator(this.conditions, cond.operator());

                if (clause != null) {
                    conditions.remove(clause); // NOSONAR
                }
            }

            conditions.add(cond);
        }

        private void add(final Condition... conditions) {
            checkConditions(conditions);

            for (final Condition cond : conditions) {
                addCondition(cond);
            }
        }

        private void add(final Collection<? extends Condition> conditions) {
            checkConditions(conditions);

            for (final Condition cond : conditions) {
                addCondition(cond);
            }
        }

        /**
         * Builds and returns the Criteria instance from the configured conditions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .where(Filters.equal("active", true))
         *     .orderBy("createdDate", SortDirection.DESC)
         *     .limit(100)
         *     .build();
         * }</pre>
         *
         * @return a new Criteria instance
         */
        public Criteria build() {
            return new Criteria(this.selectModifier, new ArrayList<>(conditions));
        }

        //    /**
        //     * Backward-compatible alias type for {@link Builder}.
        //     *
        //     * <p>This nested type is retained for source compatibility with existing
        //     * code that references {@code Filters.Builder.CB} explicitly.</p>
        //     */
        //    public static final class CB extends Builder {
        //
        //        private CB() {
        //            // utility class.
        //        }
        //    }
    }
}
