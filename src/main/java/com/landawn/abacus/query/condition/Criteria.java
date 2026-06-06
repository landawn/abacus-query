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
 * A container representing a complete SQL query structure composed of multiple clauses
 * ({@link Join}, {@link Where}, {@link GroupBy}, {@link Having}, {@link OrderBy}, {@link Limit},
 * and set operations like {@link Union}/{@link Intersect}/{@link Except}).
 *
 * <p>Instances are effectively immutable once built: the constituent conditions list is final
 * and never mutated post-construction, and all collection accessors ({@link #getConditions()},
 * {@link #getJoins()}, {@link #getSetOperations()}, {@link #findConditions(Operator)}) return
 * unmodifiable lists.</p>
 *
 * <p>Instances are created via {@link #builder()}. Each clause is independent and should not
 * be nested inside another clause; compose multiple clauses within a single {@code Criteria}.</p>
 *
 * <p><b>Usage Examples:</b></p>
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
 * @see #builder()
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

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Single-slot toString cache: last naming policy and its rendered string (performance only). */
    private transient NamingPolicy cachedTostringNamingPolicy;

    private transient String cachedTostring;

    /** Lazily memoized unmodifiable JOIN view (performance only). */
    private transient List<Join> cachedJoinsView;

    /** Lazily memoized unmodifiable set-operations view (performance only). */
    private transient List<Clause> cachedSetOperationsView;

    /**
     * Creates a new Criteria instance with the specified select modifier and condition list.
     * This constructor is package-private; use {@link #builder()} to construct instances.
     * The supplied {@code conditions} list is stored by reference (no defensive copy), so
     * callers must pass a freshly created list that they will not retain or mutate afterwards.
     *
     * @param selectModifier the SELECT modifier (e.g., {@code DISTINCT}), or {@code null} for none
     * @param conditions the list of conditions representing the query clauses; stored by reference
     */
    Criteria(String selectModifier, List<Condition> conditions) {
        super(Operator.EMPTY);
        this.selectModifier = selectModifier;
        this.conditions = conditions;
    }

    /**
     * Returns the SELECT modifier (e.g., {@code DISTINCT}, {@code DISTINCTROW},
     * {@code DISTINCT(col1, col2)}, or any custom modifier set via
     * {@link Builder#selectModifier(String)}), or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getSelectModifier();                    // returns null
     * Criteria.builder().distinct().build().getSelectModifier();         // returns "DISTINCT"
     * Criteria.builder().distinctBy("a, b").build().getSelectModifier(); // returns "DISTINCT(a, b)"
     * }</pre>
     *
     * @return the SELECT modifier, or {@code null} if not set
     * @see Builder#distinct()
     * @see Builder#distinctBy(String)
     * @see Builder#distinctRow()
     * @see Builder#distinctRowBy(String)
     * @see Builder#selectModifier(String)
     */
    public String getSelectModifier() {
        return selectModifier;
    }

    /**
     * Returns all JOIN clauses (JOIN, INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, CROSS JOIN, NATURAL JOIN)
     * in the order they were added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getJoins();   // returns [] (empty list)
     *
     * Criteria c = Criteria.builder().join("orders").innerJoin("items").build();
     * c.getJoins().size();                     // returns 2
     * c.getJoins().add(null);                  // throws UnsupportedOperationException (unmodifiable view)
     * }</pre>
     *
     * @return an unmodifiable list of {@link Join} conditions; empty if none exist
     */
    public List<Join> getJoins() {
        List<Join> view = cachedJoinsView;

        if (view == null) {
            List<Join> joins = null;

            for (final Condition cond : this.conditions) {
                if (cond instanceof Join) {
                    if (joins == null) {
                        joins = new ArrayList<>();
                    }

                    joins.add((Join) cond);
                }
            }

            view = joins == null ? N.emptyList() : Collections.unmodifiableList(joins);
            cachedJoinsView = view;
        }

        return view;
    }

    /**
     * Returns the WHERE clause, or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getWhere();   // returns null
     *
     * Criteria c = Criteria.builder().where(Filters.eq("a", 1)).build();
     * c.getWhere().operator();                 // returns Operator.WHERE
     * }</pre>
     *
     * @return the {@link Where} clause as a {@link Clause}, or {@code null}
     */
    public Clause getWhere() {
        return (Clause) find(Operator.WHERE);
    }

    /**
     * Returns the GROUP BY clause, or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getGroupBy();   // returns null
     *
     * Criteria c = Criteria.builder().groupBy("dept").build();
     * c.getGroupBy().operator();                 // returns Operator.GROUP_BY
     * }</pre>
     *
     * @return the {@link GroupBy} clause as a {@link Clause}, or {@code null}
     */
    public Clause getGroupBy() {
        return (Clause) find(Operator.GROUP_BY);
    }

    /**
     * Returns the HAVING clause, or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getHaving();   // returns null
     *
     * Criteria c = Criteria.builder().having(Filters.greaterThan("COUNT(*)", 5)).build();
     * c.getHaving().operator();                 // returns Operator.HAVING
     * }</pre>
     *
     * @return the {@link Having} clause as a {@link Clause}, or {@code null}
     */
    public Clause getHaving() {
        return (Clause) find(Operator.HAVING);
    }

    /**
     * Returns all set operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS) in the order they were added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getSetOperations();   // returns [] (empty list)
     *
     * Criteria c = Criteria.builder().union(Filters.subQuery("SELECT id FROM t")).build();
     * c.getSetOperations().size();                     // returns 1
     * }</pre>
     *
     * @return an unmodifiable list of set operation clauses; empty if none exist
     */
    public List<Clause> getSetOperations() {
        List<Clause> view = cachedSetOperationsView;

        if (view == null) {
            List<Clause> result = null;

            for (final Condition cond : this.conditions) {
                if (SET_OPERATORS.contains(cond.operator())) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    result.add((Clause) cond);
                }
            }

            view = result == null ? N.emptyList() : Collections.unmodifiableList(result);
            cachedSetOperationsView = view;
        }

        return view;
    }

    /**
     * Returns the ORDER BY clause, or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getOrderBy();   // returns null
     *
     * Criteria c = Criteria.builder().orderBy("name").build();
     * c.getOrderBy().operator();                 // returns Operator.ORDER_BY
     * }</pre>
     *
     * @return the {@link OrderBy} clause as a {@link Clause}, or {@code null}
     */
    public Clause getOrderBy() {
        return (Clause) find(Operator.ORDER_BY);
    }

    /**
     * Returns the LIMIT clause, or {@code null} if none was set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getLimit();   // returns null
     *
     * Criteria c = Criteria.builder().limit(10).build();
     * c.getLimit().toString(NamingPolicy.NO_CHANGE);   // returns "LIMIT 10"
     * }</pre>
     *
     * @return the {@link Limit} clause, or {@code null}
     */
    public Limit getLimit() {
        return (Limit) find(Operator.LIMIT);
    }

    /**
     * Returns all conditions (clauses) in this criteria in the order they were added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getConditions();   // returns [] (empty list)
     *
     * Criteria c = Criteria.builder().where(Filters.eq("a", 1)).orderBy("b").build();
     * c.getConditions().size();                      // returns 2
     * c.getConditions().clear();                     // throws UnsupportedOperationException (unmodifiable view)
     * }</pre>
     *
     * @return an unmodifiable list of all conditions
     */
    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * Returns all conditions whose {@link Condition#operator()} equals the given operator,
     * in the order they were added. This includes any duplicate clauses that were added directly
     * (the Builder normally replaces single-instance clauses like WHERE/ORDER BY, but multiple
     * JOINs and set operations such as UNION accumulate).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria c = Criteria.builder().join("o1").join("o2").where(Filters.eq("a", 1)).build();
     * c.findConditions(Operator.JOIN).size();    // returns 2 (JOINs accumulate)
     * c.findConditions(Operator.WHERE).size();   // returns 1
     * c.findConditions(Operator.HAVING);         // returns [] (no HAVING present)
     * c.findConditions(null);                    // returns [] (null never matches an operator)
     * }</pre>
     *
     * @param operator the operator to match (may be {@code null}, in which case this returns an
     *                 empty list since {@link AbstractCondition} disallows null operators)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria.builder().build().getParameters();   // returns [] (empty list)
     *
     * Criteria c = Criteria.builder().where(Filters.eq("status", "active")).limit(10).build();
     * c.getParameters();   // returns ["active"] (the literal LIMIT count carries no parameter)
     * }</pre>
     *
     * @return an immutable list of all parameters collected from the constituent clauses;
     *         empty if this criteria has no conditions or if none of the conditions carry parameters
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    private ImmutableList<Object> computeParameters() {
        if (this.conditions.isEmpty()) {
            return ImmutableList.empty();
        }

        // Single pass: bucket conditions by clause while preserving the exact ordering used by
        // the previous 7 independent scans (JOINs in order, then first WHERE, first GROUP BY,
        // first HAVING, set operations in order, first ORDER BY, first LIMIT).
        List<Join> joins = null;
        Condition where = null;
        Condition groupBy = null;
        Condition having = null;
        List<Condition> setOperations = null;
        Condition orderBy = null;
        Condition limit = null;

        for (final Condition cond : this.conditions) {
            final Operator op = cond.operator();

            if (cond instanceof Join) {
                if (joins == null) {
                    joins = new ArrayList<>();
                }
                joins.add((Join) cond);
            } else if (op == Operator.WHERE) {
                if (where == null) {
                    where = cond;
                }
            } else if (op == Operator.GROUP_BY) {
                if (groupBy == null) {
                    groupBy = cond;
                }
            } else if (op == Operator.HAVING) {
                if (having == null) {
                    having = cond;
                }
            } else if (op == Operator.ORDER_BY) {
                if (orderBy == null) {
                    orderBy = cond;
                }
            } else if (op == Operator.LIMIT) {
                if (limit == null) {
                    limit = cond;
                }
            } else if (SET_OPERATORS.contains(op)) {
                if (setOperations == null) {
                    setOperations = new ArrayList<>();
                }
                setOperations.add(cond);
            }
        }

        final List<Object> parameters = new ArrayList<>();

        if (joins != null) {
            for (final Join join : joins) {
                parameters.addAll(join.getParameters());
            }
        }

        if (where != null) {
            parameters.addAll(where.getParameters());
        }

        if (groupBy != null) {
            parameters.addAll(groupBy.getParameters());
        }

        if (having != null) {
            parameters.addAll(having.getParameters());
        }

        if (setOperations != null) {
            for (final Condition cond : setOperations) {
                parameters.addAll(cond.getParameters());
            }
        }

        if (orderBy != null) {
            parameters.addAll(orderBy.getParameters());
        }

        if (limit != null) {
            parameters.addAll(limit.getParameters());
        }

        return ImmutableList.wrap(parameters);
    }

    /**
     * Returns a string representation of this Criteria using the specified naming policy.
     * Clauses are emitted in SQL order: select modifier, JOINs, WHERE, GROUP BY, HAVING,
     * set operations (UNION/INTERSECT/EXCEPT), ORDER BY, LIMIT. Each clause is prefixed by a
     * leading space, so a non-empty result starts with a space.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria c = Criteria.builder().where(Filters.eq("firstName", "John")).build();
     * c.toString(NamingPolicy.NO_CHANGE);    // returns " WHERE firstName = 'John'"
     * c.toString(NamingPolicy.SNAKE_CASE);   // returns " WHERE first_name = 'John'"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to property names within each clause
     * @return a string representation of this Criteria
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (cachedTostring != null && cachedTostringNamingPolicy == namingPolicy) {
            return cachedTostring;
        }

        final String result = doToString(namingPolicy);

        cachedTostring = result;
        cachedTostringNamingPolicy = namingPolicy;

        return result;
    }

    private String doToString(final NamingPolicy namingPolicy) {
        // Single pass into per-clause buffers, then assembled in SQL order
        // (selectModifier + join + where + groupBy + having + setOps + orderBy + limit).
        // Output is byte-identical to the previous O(n^2) string-concatenation version.
        final StringBuilder join = new StringBuilder();
        final StringBuilder where = new StringBuilder();
        final StringBuilder groupBy = new StringBuilder();
        final StringBuilder having = new StringBuilder();
        final StringBuilder orderBy = new StringBuilder();
        final StringBuilder limit = new StringBuilder();
        final StringBuilder setOps = new StringBuilder();

        for (final Condition cond : this.conditions) {
            final StringBuilder target;
            final Operator op = cond.operator();

            if (op == Operator.WHERE) {
                target = where;
            } else if (op == Operator.ORDER_BY) {
                target = orderBy;
            } else if (op == Operator.GROUP_BY) {
                target = groupBy;
            } else if (op == Operator.HAVING) {
                target = having;
            } else if (op == Operator.LIMIT) {
                target = limit;
            } else if (cond instanceof Join) {
                target = join;
            } else {
                target = setOps;
            }

            target.append(SK._SPACE).append(cond.toString(namingPolicy));
        }

        final int modifierLen = Strings.isEmpty(this.selectModifier) ? 0 : 1 + this.selectModifier.length();
        final StringBuilder sb = new StringBuilder(
                modifierLen + join.length() + where.length() + groupBy.length() + having.length() + setOps.length() + orderBy.length() + limit.length());

        if (modifierLen > 0) {
            sb.append(SK.SPACE).append(this.selectModifier);
        }

        sb.append(join).append(where).append(groupBy).append(having).append(setOps).append(orderBy).append(limit);

        return sb.toString();
    }

    /**
     * Returns the hash code of this Criteria, based on its select modifier and conditions list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria c1 = Criteria.builder().where(Filters.eq("a", 1)).build();
     * Criteria c2 = Criteria.builder().where(Filters.eq("a", 1)).build();
     * c1.hashCode() == c2.hashCode();   // returns true (equal criteria share a hash code)
     * }</pre>
     *
     * @return hash code based on the select modifier and the ordered conditions list
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + (Strings.isEmpty(selectModifier) ? 0 : selectModifier.hashCode());
            h = (h * 31) + conditions.hashCode();

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks whether this Criteria is equal to another object.
     * Two {@code Criteria} instances are equal if they have the same select modifier
     * and the same ordered list of conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria c1 = Criteria.builder().where(Filters.eq("a", 1)).build();
     * Criteria c2 = Criteria.builder().where(Filters.eq("a", 1)).build();
     * c1.equals(c2);               // returns true
     * c1.equals(c1);               // returns true (reflexive)
     * c1.equals(null);             // returns false
     * c1.equals("not a Criteria"); // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria original = Criteria.builder().distinct().where(Filters.eq("a", 1)).build();
     * Criteria copy = original.toBuilder().build();
     * copy.equals(original);   // returns true (a faithful copy)
     * }</pre>
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
         * DISTINCT removes duplicate rows from the result set when the surrounding
         * {@code SqlBuilder} renders the {@code SELECT} clause.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .distinct()
         *     .where(Filters.equal("status", "active"))
         *     .build();
         * c.getSelectModifier();                 // returns "DISTINCT"
         * c.toString(NamingPolicy.NO_CHANGE);    // returns " DISTINCT WHERE status = 'active'"
         * // Combined with a SqlBuilder SELECT, renders: SELECT DISTINCT ... WHERE status = 'active'
         * }</pre>
         *
         * @return this Builder instance for method chaining
         * @see #distinctBy(String)
         */
        public Builder distinct() {
            selectModifier = SK.DISTINCT;

            return this;
        }

        /**
         * Sets the DISTINCT modifier with specific columns.
         * Only the specified columns are considered for duplicate removal.
         * If {@code columnNames} is {@code null} or empty, a plain {@code DISTINCT}
         * modifier (without parentheses) is used.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.builder().distinctBy("department, location").build().getSelectModifier();
         * // returns "DISTINCT(department, location)"
         *
         * Criteria.builder().distinctBy("city").build().getSelectModifier();   // returns "DISTINCT(city)"
         *
         * // null or empty falls back to a plain DISTINCT (no parentheses).
         * Criteria.builder().distinctBy(null).build().getSelectModifier();     // returns "DISTINCT"
         * Criteria.builder().distinctBy("").build().getSelectModifier();       // returns "DISTINCT"
         * }</pre>
         *
         * @param columnNames the columns to apply DISTINCT to; if {@code null} or empty, plain {@code DISTINCT} is used
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
         * Criteria c = Criteria.builder()
         *     .distinctRow()
         *     .where(Filters.equal("active", true))
         *     .build();
         * c.getSelectModifier();   // returns "DISTINCTROW"
         * // Combined with a SqlBuilder SELECT, renders: SELECT DISTINCTROW ... WHERE active = true
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
         * Only the specified columns are considered for duplicate removal.
         * If {@code columnNames} is {@code null} or empty, a plain {@code DISTINCTROW}
         * modifier (without parentheses) is used.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.builder().distinctRowBy("category, subcategory").build().getSelectModifier();
         * // returns "DISTINCTROW(category, subcategory)"
         *
         * // null or empty falls back to a plain DISTINCTROW (no parentheses).
         * Criteria.builder().distinctRowBy(null).build().getSelectModifier();   // returns "DISTINCTROW"
         * }</pre>
         *
         * @param columnNames the columns to apply DISTINCTROW to; if {@code null} or empty, plain {@code DISTINCTROW} is used
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
         * Criteria.builder().selectModifier("SQL_CALC_FOUND_ROWS").build().getSelectModifier();
         * // returns "SQL_CALC_FOUND_ROWS"
         *
         * // The value is stored verbatim; passing null clears any previously set modifier.
         * Criteria.builder().selectModifier(null).build().getSelectModifier();   // returns null
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
         * Criteria c = Criteria.builder()
         *     .join(
         *         new LeftJoin("orders", new On("users.id", "orders.user_id")),
         *         new InnerJoin("products", new On("orders.product_id", "products.id"))
         *     )
         *     .build();
         * c.getJoins().size();   // returns 2
         *
         * // Passing no joins is a no-op.
         * Criteria empty = Criteria.builder().join(new Join[0]).build();
         * empty.getJoins();      // returns [] (empty list)
         * }</pre>
         *
         * @param joins the JOIN clauses to add
         * @return this Builder instance for method chaining
         */
        public Builder join(final Join... joins) {
            add(joins);

            return this;
        }

        /**
         * Adds JOIN clauses to this criteria.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Join> joins = Arrays.asList(
         *     new LeftJoin("orders", new On("users.id", "orders.user_id")),
         *     new RightJoin("payments", new On("orders.id", "payments.order_id"))
         * );
         * Criteria c = Criteria.builder().join(joins).build();
         * c.getJoins().size();   // returns 2
         *
         * // An empty collection is a no-op.
         * Criteria empty = Criteria.builder().join(new ArrayList<Join>()).build();
         * empty.getJoins();      // returns [] (empty list)
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
         * Adds a plain JOIN (no explicit type keyword) to this criteria, without an explicit condition.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .join("orders")
         *     .where(Filters.expr("users.id = orders.user_id"))
         *     .build();
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
         * Adds a plain JOIN (no explicit type keyword) with a condition to this criteria.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = Criteria.builder()
         *     .join("orders", new On("users.id", "orders.user_id"))
         *     .where(Filters.equal("users.status", "active"))
         *     .build();
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
         * Adds a plain JOIN (no explicit type keyword) with multiple entities and a condition to this criteria.
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Collection<String> tables = Arrays.asList("orders", "order_items");
         * Criteria c = Criteria.builder()
         *     .join(tables, new On("id", "order_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " JOIN (orders, order_items) ON id = order_id"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().join(Arrays.asList("orders"), new On("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " JOIN orders ON a = b"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .innerJoin("orders")
         *     .where(Filters.expr("users.id = orders.user_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders WHERE users.id = orders.user_id"
         *
         * Criteria bare = Criteria.builder().innerJoin("orders").build();
         * bare.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .innerJoin("orders", Filters.on("users.id", "orders.user_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON users.id = orders.user_id"
         *
         * // The ON value is treated as a property reference, so it is rendered unquoted.
         * Criteria c2 = Criteria.builder().innerJoin("orders", Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON a = b"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .innerJoin(Arrays.asList("orders", "order_items"), Filters.on("id", "order_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN (orders, order_items) ON id = order_id"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().innerJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON a = b"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder().leftJoin("orders").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders"
         *
         * Criteria c2 = Criteria.builder().leftJoin("orders").where(Filters.eq("a", 1)).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders WHERE a = 1"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .leftJoin("orders", Filters.on("users.id", "orders.user_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON users.id = orders.user_id"
         *
         * Criteria c2 = Criteria.builder().leftJoin("orders", Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON a = b"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .leftJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN (orders, items) ON a = b"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().leftJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON a = b"
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder().rightJoin("orders").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders"
         *
         * Criteria c2 = Criteria.builder().rightJoin("orders").where(Filters.eq("a", 1)).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders WHERE a = 1"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .rightJoin("orders", Filters.on("users.id", "orders.user_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON users.id = orders.user_id"
         *
         * Criteria c2 = Criteria.builder().rightJoin("orders", Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON a = b"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .rightJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN (orders, items) ON a = b"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().rightJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON a = b"
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder().fullJoin("orders").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders"
         *
         * Criteria c2 = Criteria.builder().fullJoin("orders").where(Filters.eq("a", 1)).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders WHERE a = 1"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .fullJoin("orders", Filters.on("users.id", "orders.user_id"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON users.id = orders.user_id"
         *
         * Criteria c2 = Criteria.builder().fullJoin("orders", Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON a = b"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .fullJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN (orders, items) ON a = b"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().fullJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON a = b"
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder().crossJoin("colors").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors"
         *
         * Criteria c2 = Criteria.builder().crossJoin("colors").where(Filters.eq("a", 1)).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors WHERE a = 1"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // A non-On/Using condition (e.g. Equal) has the ON keyword prepended by the join.
         * Criteria c = Criteria.builder()
         *     .crossJoin("colors", Filters.eq("active", true))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors ON active = true"
         *
         * // An On condition renders with the ON keyword.
         * Criteria c2 = Criteria.builder().crossJoin("colors", Filters.on("a", "b")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors ON a = b"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .crossJoin(Arrays.asList("a", "b"), Filters.on("x", "y"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN (a, b) ON x = y"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().crossJoin(Arrays.asList("a"), Filters.on("x", "y")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN a ON x = y"
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder().naturalJoin("employees").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees"
         *
         * Criteria c2 = Criteria.builder().naturalJoin("employees").where(Filters.eq("a", 1)).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees WHERE a = 1"
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // On's second argument is treated as a property reference, so it renders unquoted.
         * Criteria c = Criteria.builder()
         *     .naturalJoin("employees", Filters.on("status", "active"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees ON status = active"
         *
         * // A non-On/Using condition (e.g. Equal) has the ON keyword prepended by the join.
         * Criteria c2 = Criteria.builder().naturalJoin("employees", Filters.eq("status", "active")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees ON status = 'active'"
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
         * Multiple entities are rendered as a parenthesized, comma-separated list; a single entity is rendered bare.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .naturalJoin(Arrays.asList("a", "b"), Filters.on("x", "y"))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN (a, b) ON x = y"
         *
         * // A single-element collection is rendered without parentheses.
         * Criteria c2 = Criteria.builder().naturalJoin(Arrays.asList("a"), Filters.on("x", "y")).build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN a ON x = y"
         * }</pre>
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.and(Filters.equal("status", "active"), Filters.greaterThan("age", 18)))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " WHERE ((status = 'active') AND (age > 18))"
         *
         * // Edge cases:
         * Criteria.builder().where((Condition) null);   // throws IllegalArgumentException
         * Criteria.builder().where(new GroupBy("x"));   // throws IllegalArgumentException (wrong clause)
         * }</pre>
         *
         * @param cond the WHERE condition (must not be {@code null}); if its operator is already
         *             {@link Operator#WHERE} it is added directly, otherwise it is wrapped in a {@link Where}
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, is a {@link Criteria},
         *                                  uses {@code ON}/{@code USING}, or is a clause condition
         *                                  with an operator other than {@code WHERE}
         */
        public Builder where(final Condition cond) {
            N.checkArgNotNull(cond, "cond");

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
         * Criteria c = Criteria.builder().where("age > 18 AND status = 'active'").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " WHERE age > 18 AND status = 'active'"
         *
         * // Edge cases:
         * Criteria.builder().where((String) null);   // throws IllegalArgumentException
         * Criteria.builder().where("");              // throws IllegalArgumentException
         * }</pre>
         *
         * @param expr the WHERE condition as a string (must not be {@code null} or empty)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code expr} is {@code null} or empty
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
         * Criteria c = Criteria.builder().groupBy(Filters.expr("YEAR(order_date)")).build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " GROUP BY YEAR(order_date)"
         *
         * Criteria.builder().groupBy((Condition) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param cond the GROUP BY condition (must not be {@code null}); if its operator is already
         *             {@link Operator#GROUP_BY} it is added directly, otherwise it is wrapped in a {@link GroupBy}
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, is a {@link Criteria},
         *                                  uses {@code ON}/{@code USING}, or is a clause condition
         *                                  with an operator other than {@code GROUP_BY}
         */
        public Builder groupBy(final Condition cond) {
            N.checkArgNotNull(cond, "cond");

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
         * Groups results by the specified columns (no explicit sort direction is emitted).
         * If a GROUP BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.Builder builder = Criteria.builder()
         *     .groupBy("department", "location", "role");
         * // Results in: GROUP BY department, location, role
         * }</pre>
         *
         * @param propNames the property names to group by
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or contains a {@code null} or empty element
         */
        public Builder groupBy(final String... propNames) {
            add(new GroupBy(propNames));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with a property and sort direction.
         * If a GROUP BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.Builder builder = Criteria.builder()
         *     .groupBy("total_sales", SortDirection.DESC);
         * // Results in: GROUP BY total_sales DESC
         * }</pre>
         *
         * @param propName the property name to group by
         * @param direction the sort direction
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propName} is {@code null} or empty, or if {@code direction} is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
         *     .groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
         * // Results in: GROUP BY year DESC, month ASC
         * }</pre>
         *
         * @param propName the first property name to group by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to group by
         * @param direction2 the sort direction for the second property
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if any property name is {@code null} or empty, or if any sort direction is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
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
         * @throws IllegalArgumentException if any property name is {@code null} or empty, or if any sort direction is {@code null}
         */
        public Builder groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
                final String propName3, final SortDirection direction3) {
            groupBy(N.asMap(propName, direction, propName2, direction2, propName3, direction3));

            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with multiple properties.
         * Groups results by the specified columns (no explicit sort direction is emitted).
         * If a GROUP BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> groupCols = Arrays.asList("region", "product_type");
         * Criteria c = Criteria.builder().groupBy(groupCols).build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " GROUP BY region, product_type"
         *
         * Criteria.builder().groupBy((Collection<String>) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param propNames the collection of property names to group by
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order; must not be {@code null} or empty)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or contains a {@code null} or empty element
         */
        public Builder groupBy(final Collection<String> propNames) {
            N.checkArgNotEmpty(propNames, "propNames");

            add(new GroupBy(propNames.toArray(new String[0])));
            return this;
        }

        /**
         * Sets or replaces the GROUP BY clause with multiple properties and sort direction.
         * All properties will use the same sort direction.
         * If a GROUP BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> groupCols = Arrays.asList("category", "brand");
         * Criteria.Builder builder = Criteria.builder()
         *     .groupBy(groupCols, SortDirection.DESC);
         * // Results in: GROUP BY category DESC, brand DESC
         * }</pre>
         *
         * @param propNames the collection of property names to group by
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order)
         * @param direction the sort direction for all properties
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements,
         *                                  or if {@code direction} is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
         *     .groupBy(grouping);
         * // Results in: GROUP BY department ASC, salary_range DESC, years_experience DESC
         * }</pre>
         *
         * @param groupings a map of property names to sort directions
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code groupings} is {@code null}, empty, or contains {@code null} or empty keys
         *                                  or {@code null} values
         */
        public Builder groupBy(final Map<String, SortDirection> groupings) {
            add(new GroupBy(groupings));

            return this;
        }

        /**
         * Sets or replaces the HAVING clause.
         * HAVING is used to filter grouped results after GROUP BY.
         * If a HAVING clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria c = Criteria.builder()
         *     .groupBy("department")
         *     .having(Filters.and(Filters.greaterThan("COUNT(*)", 10), Filters.lessThan("AVG(salary)", 100000)))
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " GROUP BY department HAVING ((COUNT(*) > 10) AND (AVG(salary) < 100000))"
         *
         * Criteria.builder().having((Condition) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param cond the HAVING condition (must not be {@code null}); if its operator is already
         *             {@link Operator#HAVING} it is added directly, otherwise it is wrapped in a {@link Having}
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, is a {@link Criteria},
         *                                  uses {@code ON}/{@code USING}, or is a clause condition
         *                                  with an operator other than {@code HAVING}
         */
        public Builder having(final Condition cond) {
            N.checkArgNotNull(cond, "cond");

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
         * Criteria c = Criteria.builder()
         *     .groupBy("product_category")
         *     .having("SUM(revenue) > 10000 AND COUNT(*) > 5")
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " GROUP BY product_category HAVING SUM(revenue) > 10000 AND COUNT(*) > 5"
         *
         * Criteria.builder().having("");   // throws IllegalArgumentException
         * }</pre>
         *
         * @param expr the HAVING condition as a string (must not be {@code null} or empty)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code expr} is {@code null} or empty
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
         * Criteria.Builder builder = Criteria.builder()
         *     .orderByAsc("lastName", "firstName", "middleName");
         * // Results in: ORDER BY lastName ASC, firstName ASC, middleName ASC
         * }</pre>
         *
         * @param propNames the property names to order by ascending
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements
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
         * Criteria.Builder builder = Criteria.builder()
         *     .orderByAsc(sortCols);
         * // Results in: ORDER BY country ASC, state ASC, city ASC
         * }</pre>
         *
         * @param propNames the collection of property names to order by ascending
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements
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
         * Criteria.Builder builder = Criteria.builder()
         *     .orderByDesc("score", "createdDate");
         * // Results in: ORDER BY score DESC, createdDate DESC
         * }</pre>
         *
         * @param propNames the property names to order by descending
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements
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
         * List<String> sortCols = Arrays.asList("revenue", "profit");
         * Criteria.Builder builder = Criteria.builder()
         *     .orderByDesc(sortCols);
         * // Results in: ORDER BY revenue DESC, profit DESC
         * }</pre>
         *
         * @param propNames the collection of property names to order by descending
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements
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
         * Criteria c = Criteria.builder().orderBy(Filters.expr("created_date DESC")).build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " ORDER BY created_date DESC"
         *
         * Criteria.builder().orderBy((Condition) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param cond the ORDER BY condition (must not be {@code null}); if its operator is already
         *             {@link Operator#ORDER_BY} it is added directly, otherwise it is wrapped in an {@link OrderBy}
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code cond} is {@code null}, is a {@link Criteria},
         *                                  uses {@code ON}/{@code USING}, or is a clause condition
         *                                  with an operator other than {@code ORDER_BY}
         */
        public Builder orderBy(final Condition cond) {
            N.checkArgNotNull(cond, "cond");

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
         * Orders by the specified columns using the default (ascending) direction; no explicit
         * {@code ASC} keyword is emitted.
         * If an ORDER BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.Builder builder = Criteria.builder()
         *     .orderBy("department", "lastName", "firstName");
         * // Results in: ORDER BY department, lastName, firstName
         * }</pre>
         *
         * @param propNames the property names to order by
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or contains a {@code null} or empty element
         */
        public Builder orderBy(final String... propNames) {
            add(new OrderBy(propNames));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with a property and sort direction.
         * If an ORDER BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria.Builder builder = Criteria.builder()
         *     .orderBy("createdDate", SortDirection.DESC);
         * // Results in: ORDER BY createdDate DESC
         * }</pre>
         *
         * @param propName the property name to order by
         * @param direction the sort direction
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propName} is {@code null} or empty, or if {@code direction} is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
         *     .orderBy("priority", SortDirection.DESC, "createdDate", SortDirection.ASC);
         * // Results in: ORDER BY priority DESC, createdDate ASC
         * }</pre>
         *
         * @param propName the first property name to order by
         * @param direction the sort direction for the first property
         * @param propName2 the second property name to order by
         * @param direction2 the sort direction for the second property
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if any property name is {@code null} or empty, or if any sort direction is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
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
         * @throws IllegalArgumentException if any property name is {@code null} or empty, or if any sort direction is {@code null}
         */
        public Builder orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
                final String propName3, final SortDirection direction3) {
            orderBy(N.asMap(propName, direction, propName2, direction2, propName3, direction3));

            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with multiple properties.
         * Orders by the specified columns using the default (ascending) direction; no explicit
         * {@code ASC} keyword is emitted.
         * If an ORDER BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> sortCols = Arrays.asList("country", "state", "city");
         * Criteria c = Criteria.builder().orderBy(sortCols).build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " ORDER BY country, state, city"
         *
         * Criteria.builder().orderBy((Collection<String>) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param propNames the collection of property names to order by
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order; must not be {@code null} or empty)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or contains a {@code null} or empty element
         */
        public Builder orderBy(final Collection<String> propNames) {
            N.checkArgNotEmpty(propNames, "propNames");

            add(new OrderBy(propNames.toArray(new String[0])));
            return this;
        }

        /**
         * Sets or replaces the ORDER BY clause with multiple properties and sort direction.
         * All properties will use the same sort direction.
         * If an ORDER BY clause already exists, it will be replaced.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> sortCols = Arrays.asList("score", "rating");
         * Criteria.Builder builder = Criteria.builder()
         *     .orderBy(sortCols, SortDirection.DESC);
         * // Results in: ORDER BY score DESC, rating DESC
         * }</pre>
         *
         * @param propNames the collection of property names to order by
         *                  (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet}
         *                  to preserve the column order)
         * @param direction the sort direction for all properties
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null} or empty elements,
         *                                  or if {@code direction} is {@code null}
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
         * Criteria.Builder builder = Criteria.builder()
         *     .orderBy(ordering);
         * // Results in: ORDER BY priority DESC, createdDate DESC, name ASC
         * }</pre>
         *
         * @param orders a map of property names to sort directions
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code orders} is {@code null}, empty, or contains {@code null} or empty keys
         *                                  or {@code null} values
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
         * Criteria c = Criteria.builder().limit(Filters.limit(100)).build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " LIMIT 100"
         *
         * Criteria.builder().limit((Limit) null);   // throws IllegalArgumentException
         * }</pre>
         *
         * @param condition the LIMIT condition (must not be {@code null}); its operator must be
         *                  {@link Operator#LIMIT}, which is guaranteed for any {@link Limit} instance
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code condition} is {@code null}
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
         *     .limit(10)
         *     .build();
         * // Results in: WHERE status = 'active' LIMIT 10
         * }</pre>
         *
         * @param count the maximum number of results to return
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code count} is negative
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
         *     .limit(20, 40)
         *     .build();
         * // Results in: ORDER BY id LIMIT 20 OFFSET 40
         * }</pre>
         *
         * @param count the maximum number of results to return
         * @param offset the number of rows to skip
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
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
         * Criteria c = Criteria.builder().limit("10 OFFSET 20").build();
         * c.toString(NamingPolicy.NO_CHANGE);   // returns " LIMIT 10 OFFSET 20"
         *
         * // Placeholder form for parameterized queries.
         * Criteria c2 = Criteria.builder().limit("? OFFSET ?").build();
         * c2.toString(NamingPolicy.NO_CHANGE);   // returns " LIMIT ? OFFSET ?"
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.equal("status", "active"))
         *     .union(archivedUsers)
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " WHERE status = 'active' UNION SELECT * FROM archived_users WHERE active = true"
         *
         * // Multiple set operations accumulate in order.
         * c.getSetOperations().size();   // returns 1
         * }</pre>
         *
         * @param subQuery the subquery to union with (must not be {@code null})
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code subQuery} is {@code null}
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.equal("status", "completed"))
         *     .unionAll(pendingOrders)
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " WHERE status = 'completed' UNION ALL SELECT * FROM pending_orders"
         * }</pre>
         *
         * @param subQuery the subquery to union with (must not be {@code null})
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code subQuery} is {@code null}
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.equal("active", true))
         *     .intersect(premiumUsers)
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " WHERE active = true INTERSECT SELECT user_id FROM premium_members"
         * }</pre>
         *
         * @param subQuery the subquery to intersect with (must not be {@code null})
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code subQuery} is {@code null}
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.equal("status", "active"))
         *     .except(excludedUsers)
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " WHERE status = 'active' EXCEPT SELECT user_id FROM blacklist"
         * }</pre>
         *
         * @param subQuery the subquery to except (must not be {@code null})
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code subQuery} is {@code null}
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
         * Criteria c = Criteria.builder()
         *     .where(Filters.equal("registered", true))
         *     .minus(inactiveUsers)
         *     .build();
         * c.toString(NamingPolicy.NO_CHANGE);
         * // returns " WHERE registered = true MINUS SELECT user_id FROM inactive_users"
         * }</pre>
         *
         * @param subQuery the subquery to minus (must not be {@code null})
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if {@code subQuery} is {@code null}
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
         * Validates that the given collection of conditions is not {@code null} and that each element
         * is a valid clause-level condition (has a clause operator and the correct implementation type).
         *
         * @param conditions the conditions to validate; must not be {@code null}
         * @throws IllegalArgumentException if {@code conditions} is {@code null} or contains an invalid condition
         */
        private void checkConditions(final Collection<? extends Condition> conditions) {
            N.checkArgNotNull(conditions, "conditions");

            for (final Condition cond : conditions) {
                checkCondition(cond);
            }
        }

        /**
         * Validates that the given array of conditions is not {@code null} and that each element
         * is a valid clause-level condition (has a clause operator and the correct implementation type).
         *
         * @param conditions the conditions to validate; must not be {@code null}
         * @throws IllegalArgumentException if {@code conditions} is {@code null} or contains an invalid condition
         */
        private void checkConditions(final Condition... conditions) {
            N.checkArgNotNull(conditions, "conditions");

            for (final Condition cond : conditions) {
                checkCondition(cond);
            }
        }

        private void checkCondition(final Condition cond) {
            N.checkArgNotNull(cond, "cond");

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
