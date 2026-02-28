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
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SK;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a complete query criteria that can contain multiple SQL clauses.
 * This class serves as a builder and container for constructing complex SQL queries
 * by combining various clauses like WHERE, JOIN, GROUP BY, HAVING, ORDER BY, LIMIT, etc.
 * 
 * <p>Criteria provides a fluent API for building queries in a type-safe manner.
 * It maintains the proper SQL clause ordering and ensures that clauses are
 * combined correctly. Each method returns the Criteria instance for method chaining.</p>
 * 
 * <p>Supported clauses include:</p>
 * <ul>
 *   <li>{@code Join} - Various JOIN operations (INNER, LEFT, RIGHT, FULL, etc.)</li>
 *   <li>{@code Where} - Filter rows based on conditions</li>
 *   <li>{@code GroupBy} - Group rows by columns</li>
 *   <li>{@code Having} - Filter grouped results</li>
 *   <li>{@code OrderBy} - Sort results</li>
 *   <li>{@code Limit} - Limit result count</li>
 *   <li>{@code Union/UnionAll/Intersect/Except} - Set operations between queries</li>
 * </ul>
 * 
 * <p>Each clause is independent. A clause should not be included in another clause.
 * If there are multiple clauses, they should be composed in one {@code Criteria} condition.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Build a complex query with multiple clauses
 * Criteria criteria = new Criteria()
 *     .join("orders", new On("users.id", "orders.user_id"))
 *     .where(Filters.and(
 *         Filters.eq("users.status", "active"),
 *         Filters.gt("orders.amount", 100)
 *     ))
 *     .groupBy("users.department")
 *     .having(Filters.gt("COUNT(*)", 5))
 *     .orderBy("COUNT(*)", SortDirection.DESC)
 *     .limit(10);
 * 
 * // Using distinct
 * Criteria distinctUsers = new Criteria()
 *     .distinct()
 *     .where(Filters.eq("active", true))
 *     .orderBy("name");
 * }</pre>
 * 
 * @see Condition
 * @see Filters
 * @see Clause
 */
public class Criteria extends AbstractCondition {

    private static final Set<Operator> aggregationOperators = N.newHashSet();

    static {
        aggregationOperators.add(Operator.UNION_ALL);
        aggregationOperators.add(Operator.UNION);
        aggregationOperators.add(Operator.INTERSECT);
        aggregationOperators.add(Operator.EXCEPT);
        aggregationOperators.add(Operator.MINUS);
    }

    private String preselect = null;

    private List<Condition> conditionList;

    /**
     * Creates a new empty Criteria instance.
     * The Criteria starts with no conditions and can be built up using the fluent API.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * criteria.where(Filters.eq("status", "active"))
     *         .orderBy("created_date", SortDirection.DESC)
     *         .limit(10);
     * }</pre>
     */
    public Criteria() {
        super(Operator.EMPTY);
        conditionList = new ArrayList<>();
    }

    /**
     * Gets the preselect modifier (e.g., DISTINCT, DISTINCTROW).
     * The preselect modifier appears before the SELECT columns in SQL.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria().distinct();
     * String modifier = criteria.preselect();   // Returns "DISTINCT"
     * }</pre>
     * 
     * @return the preselect modifier, or null if not set
     */
    public String preselect() {
        return preselect;
    }

    /**
     * Gets all JOIN clauses in this criteria.
     * Returns all types of joins (INNER, LEFT, RIGHT, FULL, CROSS, NATURAL) in the order they were added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", new On("users.id", "orders.user_id"))
     *     .join("payments", new On("orders.id", "payments.order_id"));
     *
     * List<Join> joins = criteria.getJoins();
     * // Returns a list of 2 Join conditions
     * System.out.println(joins.size());   // 2
     *
     * Criteria noJoins = new Criteria().where(Filters.eq("status", "active"));
     * List<Join> empty = noJoins.getJoins();
     * // Returns an empty list
     * }</pre>
     *
     * @return a list of Join conditions, empty list if none exist
     */
    public List<Join> getJoins() {
        final List<Join> joins = new ArrayList<>();

        for (final Condition cond : conditionList) {
            if (cond instanceof Join) {
                joins.add((Join) cond);
            }
        }

        return joins;
    }

    /**
     * Gets the WHERE clause from this criteria.
     * Returns null if no WHERE clause has been set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"));
     *
     * Cell whereClause = criteria.getWhere();
     * // Returns the Where condition wrapping: status = 'active'
     *
     * Criteria noWhere = new Criteria().orderBy("name");
     * Cell result = noWhere.getWhere();   // Returns null
     * }</pre>
     *
     * @return the Where condition, or null if not set
     */
    public Cell getWhere() {
        return (Cell) find(Operator.WHERE);
    }

    /**
     * Gets the GROUP BY clause from this criteria.
     * Returns null if no GROUP BY clause has been set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("department", "location");
     *
     * Cell groupByClause = criteria.getGroupBy();
     * // Returns the GroupBy condition for: department, location
     *
     * Criteria noGroupBy = new Criteria().where(Filters.eq("active", true));
     * Cell result = noGroupBy.getGroupBy();   // Returns null
     * }</pre>
     *
     * @return the GroupBy condition, or null if not set
     */
    public Cell getGroupBy() {
        return (Cell) find(Operator.GROUP_BY);
    }

    /**
     * Gets the HAVING clause from this criteria.
     * Returns null if no HAVING clause has been set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("department")
     *     .having("COUNT(*) > 5");
     *
     * Cell havingClause = criteria.getHaving();
     * // Returns the Having condition wrapping: COUNT(*) > 5
     *
     * Criteria noHaving = new Criteria().groupBy("category");
     * Cell result = noHaving.getHaving();   // Returns null
     * }</pre>
     *
     * @return the Having condition, or null if not set
     */
    public Cell getHaving() {
        return (Cell) find(Operator.HAVING);
    }

    /**
     * Gets all aggregation operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS).
     * These are set operations that combine results from multiple queries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery archivedUsers = Filters.subQuery("SELECT * FROM archived_users");
     * SubQuery tempUsers = Filters.subQuery("SELECT * FROM temp_users");
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("active", true))
     *     .union(archivedUsers)
     *     .unionAll(tempUsers);
     *
     * List<Cell> aggregations = criteria.getAggregation();
     * // Returns a list of 2 aggregation conditions (UNION and UNION ALL)
     *
     * Criteria noAgg = new Criteria().where(Filters.eq("status", "active"));
     * List<Cell> empty = noAgg.getAggregation();
     * // Returns an empty list
     * }</pre>
     *
     * @return a list of aggregation conditions, empty if none exist
     */
    public List<Cell> getAggregation() {
        List<Cell> result = null;

        for (final Condition cond : conditionList) {
            if (aggregationOperators.contains(cond.getOperator())) {
                if (result == null) {
                    result = new ArrayList<>();
                }

                result.add((Cell) cond);
            }
        }

        if (result == null) {
            result = N.emptyList();
        }

        return result;
    }

    /**
     * Gets the ORDER BY clause from this criteria.
     * Returns null if no ORDER BY clause has been set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderBy("name", SortDirection.ASC);
     *
     * Cell orderByClause = criteria.getOrderBy();
     * // Returns the OrderBy condition for: name ASC
     *
     * Criteria noOrderBy = new Criteria().where(Filters.eq("active", true));
     * Cell result = noOrderBy.getOrderBy();   // Returns null
     * }</pre>
     *
     * @return the OrderBy condition, or null if not set
     */
    public Cell getOrderBy() {
        return (Cell) find(Operator.ORDER_BY);
    }

    /**
     * Gets the LIMIT clause from this criteria.
     * Returns null if no LIMIT clause has been set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("active", true))
     *     .limit(50);
     *
     * Limit limitClause = criteria.getLimit();
     * // Returns the Limit condition for: LIMIT 50
     *
     * Criteria noLimit = new Criteria().where(Filters.eq("status", "active"));
     * Limit result = noLimit.getLimit();   // Returns null
     * }</pre>
     *
     * @return the Limit condition, or null if not set
     */
    public Limit getLimit() {
        return (Limit) find(Operator.LIMIT);
    }

    /**
     * Gets all conditions in this criteria.
     * Returns all conditions in the order they were added, including all clauses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .orderBy("name")
     *     .limit(10);
     *
     * List<Condition> conditions = criteria.getConditions();
     * // Returns a list of 3 conditions: Where, OrderBy, Limit
     * System.out.println(conditions.size());   // 3
     *
     * Criteria empty = new Criteria();
     * List<Condition> none = empty.getConditions();
     * // Returns an empty list
     * }</pre>
     *
     * @return a list of all conditions
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     * Gets all conditions with the specified operator.
     * Useful for retrieving all conditions of a specific type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", new On("users.id", "orders.user_id"))
     *     .join("payments", new On("orders.id", "payments.order_id"))
     *     .where(Filters.eq("status", "active"));
     *
     * List<Condition> joins = criteria.get(Operator.JOIN);
     * // Returns a list of 2 Join conditions
     *
     * List<Condition> wheres = criteria.get(Operator.WHERE);
     * // Returns a list of 1 Where condition
     *
     * List<Condition> limits = criteria.get(Operator.LIMIT);
     * // Returns an empty list (no LIMIT clause set)
     * }</pre>
     *
     * @param operator the operator to filter by (must not be null)
     * @return a list of conditions with the specified operator, empty list if none found
     */
    public List<Condition> get(final Operator operator) {
        final List<Condition> conditions = new ArrayList<>();

        for (final Condition cond : conditionList) {
            if (cond.getOperator() == operator) {
                conditions.add(cond);
            }
        }

        return conditions;
    }

    /**
     * Adds conditions to this criteria.
     * Package-private method used internally and by CriteriaUtil for framework operations.
     *
     * @param conditions the conditions to add
     */
    void add(final Condition... conditions) {
        addConditions(conditions);
    }

    /**
     * Adds conditions to this criteria.
     * Package-private method used internally and by CriteriaUtil for framework operations.
     *
     * @param conditions the collection of conditions to add
     */
    void add(final Collection<? extends Condition> conditions) {
        addConditions(conditions);
    }

    /**
     * Removes all conditions with the specified operator.
     * Package-private method used internally and by CriteriaUtil for framework operations.
     *
     * @param operator the operator of conditions to remove
     */
    void remove(final Operator operator) {
        final List<Condition> conditions = get(operator);
        remove(conditions);
    }

    /**
     * Removes specific conditions from this criteria.
     * Package-private method used internally and by CriteriaUtil for framework operations.
     *
     * @param conditions the conditions to remove
     */
    void remove(final Condition... conditions) {
        for (final Condition cond : conditions) {
            conditionList.remove(cond); // NOSONAR
        }
    }

    /**
     * Removes specific conditions from this criteria.
     * Package-private method used internally and by CriteriaUtil for framework operations.
     *
     * @param conditions the collection of conditions to remove
     */
    void remove(final Collection<? extends Condition> conditions) {
        conditionList.removeAll(conditions);
    }

    /**
     * Clears all conditions from this criteria.
     * After calling this method, the criteria will be empty and can be rebuilt.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .orderBy("name")
     *     .limit(10);
     *
     * criteria.clear();
     * // criteria is now empty; preselect is also reset to null
     *
     * // Rebuild the criteria with new conditions
     * criteria.where(Filters.gt("age", 21))
     *         .limit(20);
     * }</pre>
     *
     */
    public void clear() {
        preselect = null;
        conditionList.clear();
    }

    /**
     * Gets all parameters from all conditions in the proper order.
     * The order follows SQL clause precedence: JOIN, WHERE, HAVING, aggregations.
     * Parameters from GROUP BY, ORDER BY, and LIMIT are not included as they typically don't have parameters.
     * 
     * @return a list of all parameters from all conditions
     */
    @Override
    public List<Object> getParameters() {
        if (conditionList.size() > 0) {
            final List<Object> parameters = new ArrayList<>();
            final Collection<Join> joins = getJoins();

            for (final Join join : joins) {
                parameters.addAll(join.getParameters());
            }

            final Cell where = getWhere();

            if (where != null) {
                parameters.addAll(where.getParameters());
            }

            // group by no parameters.
            /*
             * Cell groupBy = getGroupBy();
             *
             * if (groupBy != null) { parameters.addAll(groupBy.getParameters()); }
             */
            final Cell having = getHaving();

            if (having != null) {
                parameters.addAll(having.getParameters());
            }

            final List<Cell> condList = getAggregation();

            for (final Condition cond : condList) {
                parameters.addAll(cond.getParameters());
            }

            // order by no parameters.
            /*
             * Cell orderBy = getOrderBy();
             *
             * if (orderBy != null) { parameters.addAll(orderBy.getParameters()); }
             */

            // limit no parameters.
            /*
             * Cell limit = getLimit();
             *
             * if (limit != null) { parameters.addAll(limit.getParameters()); }
             */
            return ImmutableList.wrap(parameters);
        } else {
            return N.emptyList();
        }
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * This clears parameters from all conditions in this criteria (JOIN, WHERE, HAVING, aggregations).
     *
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     *
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : conditionList) {
            condition.clearParameters();
        }
    }

    /**
     * Sets the DISTINCT modifier for the query.
     * DISTINCT removes duplicate rows from the result set.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinct()
     *     .where(Filters.eq("status", "active"));
     * // Results in: SELECT DISTINCT ... WHERE status = 'active'
     * }</pre>
     * 
     * @return this Criteria instance for method chaining
     */
    public Criteria distinct() {
        preselect = SK.DISTINCT;

        return this;
    }

    /**
     * Sets the DISTINCT modifier with specific columns.
     * Only the specified columns are considered for duplicate removal.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinctBy("department, location");
     * // Results in: SELECT DISTINCT(department, location) ...
     * 
     * // Or with column list
     * criteria.distinctBy("city");
     * // Results in: SELECT DISTINCT(city) ...
     * }</pre>
     * 
     * @param columnNames the columns to apply DISTINCT to
     * @return this Criteria instance for method chaining
     */
    public Criteria distinctBy(final String columnNames) {
        preselect = Strings.isEmpty(columnNames) ? SK.DISTINCT : SK.DISTINCT + "(" + columnNames + ")";

        return this;
    }

    /**
     * Sets the DISTINCTROW modifier for the query.
     * DISTINCTROW is similar to DISTINCT but may have database-specific behavior.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinctRow()
     *     .where(Filters.eq("active", true));
     * // Results in: SELECT DISTINCTROW ... WHERE active = true
     * }</pre>
     * 
     * @return this Criteria instance for method chaining
     */
    public Criteria distinctRow() {
        preselect = SK.DISTINCTROW;

        return this;
    }

    /**
     * Sets the DISTINCTROW modifier with specific columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinctRowBy("category, subcategory");
     * // Results in: SELECT DISTINCTROW(category, subcategory) ...
     * }</pre>
     * 
     * @param columnNames the columns to apply DISTINCTROW to
     * @return this Criteria instance for method chaining
     */
    public Criteria distinctRowBy(final String columnNames) {
        preselect = Strings.isEmpty(columnNames) ? SK.DISTINCTROW : SK.DISTINCTROW + "(" + columnNames + ")";

        return this;
    }

    /**
     * Sets a custom preselect modifier.
     * This allows for database-specific modifiers not covered by other methods.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .preselect("SQL_CALC_FOUND_ROWS")
     *     .where(Filters.eq("active", true));
     * // Results in: SELECT SQL_CALC_FOUND_ROWS ... WHERE active = true
     *
     * // MySQL-specific modifier
     * criteria.preselect("SQL_NO_CACHE");
     * }</pre>
     *
     * @param preselect the custom preselect modifier
     * @return this Criteria instance for method chaining
     */
    public Criteria preselect(final String preselect) {
        this.preselect = preselect;

        return this;
    }

    /**
     * Adds JOIN clauses to this criteria.
     * Multiple joins can be added in a single call.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join(
     *         new LeftJoin("orders", new On("users.id", "orders.user_id")),
     *         new InnerJoin("products", new On("orders.product_id", "products.id"))
     *     );
     * }</pre>
     * 
     * @param joins the JOIN clauses to add
     * @return this Criteria instance for method chaining
     */
    public final Criteria join(final Join... joins) {
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
     * @return this Criteria instance for method chaining
     */
    public Criteria join(final Collection<Join> joins) {
        add(joins);

        return this;
    }

    /**
     * Adds a simple INNER JOIN to this criteria.
     * This creates a join without an explicit condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders")
     *     .where(Filters.eq("users.id", "orders.user_id"));
     * // Results in: JOIN orders WHERE users.id = orders.user_id
     * }</pre>
     * 
     * @param joinEntity the table or entity to join
     * @return this Criteria instance for method chaining
     */
    public Criteria join(final String joinEntity) {
        add(new Join(joinEntity));

        return this;
    }

    /**
     * Adds an INNER JOIN with a condition to this criteria.
     * This is the most common form of join.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", new On("users.id", "orders.user_id"))
     *     .where(Filters.eq("users.status", "active"));
     * // Results in: JOIN orders ON users.id = orders.user_id WHERE users.status = 'active'
     * }</pre>
     * 
     * @param joinEntity the table or entity to join
     * @param condition the join condition
     * @return this Criteria instance for method chaining
     */
    public Criteria join(final String joinEntity, final Condition condition) {
        add(new Join(joinEntity, condition));

        return this;
    }

    /**
     * Adds an INNER JOIN with multiple entities and a condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> tables = Arrays.asList("orders", "order_items");
     * Criteria criteria = new Criteria()
     *     .join(tables, new On("id", "order_id"));
     * }</pre>
     * 
     * @param joinEntities the collection of tables/entities to join
     * @param condition the join condition
     * @return this Criteria instance for method chaining
     */
    public Criteria join(final Collection<String> joinEntities, final Condition condition) {
        add(new Join(joinEntities, condition));

        return this;
    }

    /**
     * Sets or replaces the WHERE clause.
     * If a WHERE clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.and(
     *         Filters.eq("status", "active"),
     *         Filters.gt("age", 18),
     *         Filters.like("email", "%@company.com")
     *     ));
     * }</pre>
     * 
     * @param condition the WHERE condition
     * @return this Criteria instance for method chaining
     */
    public Criteria where(final Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (condition.getOperator() == Operator.WHERE) {
            add(condition);
        } else {
            add(new Where(condition));
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
     * Criteria criteria = new Criteria()
     *     .where("age > 18 AND status = 'active'");
     * 
     * // Complex expression
     * criteria.where("YEAR(created_date) = 2024 OR special_flag = true");
     * }</pre>
     * 
     * @param condition the WHERE condition as a string
     * @return this Criteria instance for method chaining
     */
    public Criteria where(final String condition) {
        add(new Where(Filters.expr(condition)));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause.
     * If a GROUP BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy(Filters.expr("YEAR(order_date), MONTH(order_date)"));
     * }</pre>
     * 
     * @param condition the GROUP BY condition
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (condition.getOperator() == Operator.GROUP_BY) {
            add(condition);
        } else {
            add(new GroupBy(condition));
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
     * Criteria criteria = new Criteria()
     *     .groupBy("department", "location", "role");
     * // Results in: GROUP BY department, location, role
     * }</pre>
     * 
     * @param propNames the property names to group by
     * @return this Criteria instance for method chaining
     */
    public final Criteria groupBy(final String... propNames) {
        add(new GroupBy(propNames));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause with a property and sort direction.
     * If a GROUP BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("total_sales", SortDirection.DESC);
     * // Results in: GROUP BY total_sales DESC
     * }</pre>
     * 
     * @param propName the property name to group by
     * @param direction the sort direction
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final String propName, final SortDirection direction) {
        add(new GroupBy(propName, direction));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause with two properties and their sort directions.
     * If a GROUP BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
     * // Results in: GROUP BY year DESC, month ASC
     * }</pre>
     * 
     * @param propName the first property name to group by
     * @param direction the sort direction for the first property
     * @param propName2 the second property name to group by
     * @param direction2 the sort direction for the second property
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2) {
        groupBy(N.asLinkedHashMap(propName, direction, propName2, direction2));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause with three properties and their sort directions.
     * If a GROUP BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
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
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
            final String propName3, final SortDirection direction3) {
        groupBy(N.asLinkedHashMap(propName, direction, propName2, direction2, propName3, direction3));

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
     * Criteria criteria = new Criteria()
     *     .groupBy(groupCols);
     * // Results in: GROUP BY region, product_type
     * }</pre>
     * 
     * @param propNames the collection of property names to group by
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Collection<String> propNames) {
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
     * Criteria criteria = new Criteria()
     *     .groupBy(groupCols, SortDirection.DESC);
     * // Results in: GROUP BY category DESC, brand DESC
     * }</pre>
     * 
     * @param propNames the collection of property names to group by
     * @param direction the sort direction for all properties
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Collection<String> propNames, final SortDirection direction) {
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
     * Criteria criteria = new Criteria()
     *     .groupBy(grouping);
     * }</pre>
     * 
     * @param orders a map of property names to sort directions
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Map<String, SortDirection> orders) {
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
     * Criteria criteria = new Criteria()
     *     .groupBy("department")
     *     .having(Filters.and(
     *         Filters.gt("COUNT(*)", 10),
     *         Filters.lt("AVG(salary)", 100000)
     *     ));
     * }</pre>
     * 
     * @param condition the HAVING condition
     * @return this Criteria instance for method chaining
     */
    public Criteria having(final Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (condition.getOperator() == Operator.HAVING) {
            add(condition);
        } else {
            add(new Having(condition));
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
     * Criteria criteria = new Criteria()
     *     .groupBy("product_category")
     *     .having("SUM(revenue) > 10000 AND COUNT(*) > 5");
     * }</pre>
     * 
     * @param condition the HAVING condition as a string
     * @return this Criteria instance for method chaining
     */
    public Criteria having(final String condition) {
        add(new Having(Filters.expr(condition)));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause with ascending order.
     * Convenience method that sorts all specified columns in ascending order.
     * If an ORDER BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderByAsc("lastName", "firstName", "middleName");
     * // Results in: ORDER BY lastName ASC, firstName ASC, middleName ASC
     * }</pre>
     * 
     * @param propNames the property names to order by ascending
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByAsc(final String... propNames) {
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
     * Criteria criteria = new Criteria()
     *     .orderByAsc(sortCols);
     * }</pre>
     * 
     * @param propNames the collection of property names to order by ascending
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByAsc(final Collection<String> propNames) {
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
     * Criteria criteria = new Criteria()
     *     .orderByDesc("score", "createdDate");
     * // Results in: ORDER BY score DESC, createdDate DESC
     * }</pre>
     * 
     * @param propNames the property names to order by descending
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByDesc(final String... propNames) {
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
     * Criteria criteria = new Criteria()
     *     .orderByDesc(sortCols);
     * }</pre>
     * 
     * @param propNames the collection of property names to order by descending
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByDesc(final Collection<String> propNames) {
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
     * Criteria criteria = new Criteria()
     *     .orderBy(Filters.expr("CASE WHEN priority = 'HIGH' THEN 1 ELSE 2 END, created_date DESC"));
     * }</pre>
     * 
     * @param condition the ORDER BY condition
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (condition.getOperator() == Operator.ORDER_BY) {
            add(condition);
        } else {
            add(new OrderBy(condition));
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
     * Criteria criteria = new Criteria()
     *     .orderBy("department", "lastName", "firstName");
     * // Results in: ORDER BY department, lastName, firstName
     * }</pre>
     * 
     * @param propNames the property names to order by
     * @return this Criteria instance for method chaining
     */
    public final Criteria orderBy(final String... propNames) {
        add(new OrderBy(propNames));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause with a property and sort direction.
     * If an ORDER BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderBy("createdDate", SortDirection.DESC);
     * // Results in: ORDER BY createdDate DESC
     * }</pre>
     * 
     * @param propName the property name to order by
     * @param direction the sort direction
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final String propName, final SortDirection direction) {
        add(new OrderBy(propName, direction));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause with two properties and their sort directions.
     * If an ORDER BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderBy("priority", SortDirection.DESC, "createdDate", SortDirection.ASC);
     * // Results in: ORDER BY priority DESC, createdDate ASC
     * }</pre>
     * 
     * @param propName the first property name to order by
     * @param direction the sort direction for the first property
     * @param propName2 the second property name to order by
     * @param direction2 the sort direction for the second property
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2) {
        orderBy(N.asLinkedHashMap(propName, direction, propName2, direction2));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause with three properties and their sort directions.
     * If an ORDER BY clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
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
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2,
            final String propName3, final SortDirection direction3) {
        orderBy(N.asLinkedHashMap(propName, direction, propName2, direction2, propName3, direction3));

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
     * Criteria criteria = new Criteria()
     *     .orderBy(sortCols);
     * // Results in: ORDER BY country, state, city
     * }</pre>
     * 
     * @param propNames the collection of property names to order by
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Collection<String> propNames) {
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
     * Criteria criteria = new Criteria()
     *     .orderBy(sortCols, SortDirection.DESC);
     * // Results in: ORDER BY score DESC, rating DESC
     * }</pre>
     * 
     * @param propNames the collection of property names to order by
     * @param direction the sort direction for all properties
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Collection<String> propNames, final SortDirection direction) {
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
     * Criteria criteria = new Criteria()
     *     .orderBy(ordering);
     * }</pre>
     * 
     * @param orders a map of property names to sort directions
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Map<String, SortDirection> orders) {
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
     * Criteria criteria = new Criteria()
     *     .limit(customLimit);
     * }</pre>
     * 
     * @param condition the LIMIT condition
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final Limit condition) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .limit(10);
     * // Results in: WHERE status = 'active' LIMIT 10
     * }</pre>
     * 
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final int count) {
        add(Filters.limit(count));

        return this;
    }

    /**
     * Sets or replaces the LIMIT clause with offset and count.
     * Used for pagination - skips 'offset' rows and returns up to 'count' rows.
     * If a LIMIT clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Page 3 with 20 items per page (skip 40, take 20)
     * Criteria criteria = new Criteria()
     *     .orderBy("id")
     *     .limit(40, 20);
     * // Results in: ORDER BY id LIMIT 20 OFFSET 40
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final int offset, final int count) {
        add(Filters.limit(offset, count));

        return this;
    }

    /**
     * Sets or replaces the LIMIT clause using a string expression.
     * Allows for database-specific limit syntax.
     * If a LIMIT clause already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .limit("10 OFFSET 20");
     * 
     * // Or with parameters
     * criteria.limit("? OFFSET ?");
     * }</pre>
     * 
     * @param expr the LIMIT expression as a string
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final String expr) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .union(archivedUsers);
     * // Returns active users from both current and archived tables
     * }</pre>
     * 
     * @param subQuery the subquery to union with
     * @return this Criteria instance for method chaining
     */
    public Criteria union(final SubQuery subQuery) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "completed"))
     *     .unionAll(pendingOrders);
     * // Returns all orders, including duplicates if any exist
     * }</pre>
     * 
     * @param subQuery the subquery to union with
     * @return this Criteria instance for method chaining
     */
    public Criteria unionAll(final SubQuery subQuery) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("active", true))
     *     .intersect(premiumUsers);
     * // Returns only active users who are also premium members
     * }</pre>
     * 
     * @param subQuery the subquery to intersect with
     * @return this Criteria instance for method chaining
     */
    public Criteria intersect(final SubQuery subQuery) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .except(excludedUsers);
     * // Returns active users who are not in the blacklist
     * }</pre>
     * 
     * @param subQuery the subquery to except
     * @return this Criteria instance for method chaining
     */
    public Criteria except(final SubQuery subQuery) {
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
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("registered", true))
     *     .minus(inactiveUsers);
     * // Returns registered users minus inactive ones
     * }</pre>
     * 
     * @param subQuery the subquery to minus
     * @return this Criteria instance for method chaining
     */
    public Criteria minus(final SubQuery subQuery) {
        add(new Minus(subQuery));

        return this;
    }

    /**
     * Creates a deep copy of this Criteria.
     * All conditions are also copied, ensuring complete independence between
     * the original and the copy.
     * 
     * @param <T> the type of condition to return
     * @return a new Criteria instance with copied values
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final Criteria result = super.copy();

        result.conditionList = new ArrayList<>();

        for (final Condition cond : conditionList) {
            result.conditionList.add(cond.copy());
        }

        return (T) result;
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
        final String preselect = Strings.isEmpty(this.preselect) ? Strings.EMPTY : SK.SPACE + this.preselect; //NOSONAR
        String join = Strings.EMPTY;
        String where = Strings.EMPTY;
        String groupBy = Strings.EMPTY;
        String having = Strings.EMPTY;
        String orderBy = Strings.EMPTY;
        String limit = Strings.EMPTY;
        final String forUpdate = Strings.EMPTY;
        String aggregate = Strings.EMPTY;

        for (final Condition cond : conditionList) {
            if (cond.getOperator() == Operator.WHERE) {
                where += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.ORDER_BY) {
                orderBy += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.GROUP_BY) {
                groupBy += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.HAVING) {
                having += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.LIMIT) {
                limit += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond instanceof Join) {
                join += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else {
                aggregate += (SK._SPACE + cond.toString(namingPolicy)); //NOSONAR
            }
        }

        return preselect + join + where + groupBy + having + aggregate + orderBy + limit + forUpdate;
    }

    /**
     * Returns the hash code of this Criteria.
     * The hash code is based on the preselect modifier and all conditions.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int h = Strings.isEmpty(preselect) ? 0 : preselect.hashCode();
        return (h * 31) + conditionList.hashCode();
    }

    /**
     * Checks if this Criteria is equal to another object.
     * Two Criteria are equal if they have the same preselect modifier and conditions.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj instanceof Criteria && N.equals(((Criteria) obj).preselect, preselect) && N.equals(((Criteria) obj).conditionList, conditionList));
    }

    /**
     *
     * @param conditions the conditions to validate
     */
    private void checkConditions(final Collection<? extends Condition> conditions) {
        for (final Condition cond : conditions) {
            checkCondition(cond);
        }
    }

    /**
     *
     * @param conditions the conditions to validate
     */
    private void checkConditions(final Condition... conditions) {
        for (final Condition cond : conditions) {
            checkCondition(cond);
        }
    }

    /**
     *
     * @param cond
     */
    private void checkCondition(final Condition cond) {
        if (cond == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (!CriteriaUtil.isClause(cond.getOperator())) {
            throw new IllegalArgumentException(
                    "Invalid operator '" + cond.getOperator() + "' for Criteria. Expected clause operators: WHERE, GROUP_BY, HAVING, ORDER_BY, LIMIT, etc.");
        }
    }

    /**
     * Adds the conditions.
     *
     * @param conditions the conditions to add
     */
    private void addConditions(final Collection<? extends Condition> conditions) {
        checkConditions(conditions);

        for (final Condition cond : conditions) {
            addCondition(cond);
        }
    }

    /**
     * Adds the conditions.
     *
     * @param conditions the conditions to add
     */
    private void addConditions(final Condition... conditions) {
        checkConditions(conditions);

        for (final Condition cond : conditions) {
            addCondition(cond);
        }
    }

    /**
     * Adds the condition.
     *
     * @param cond
     */
    private void addCondition(final Condition cond) {
        if (cond.getOperator() == Operator.WHERE || cond.getOperator() == Operator.ORDER_BY || cond.getOperator() == Operator.GROUP_BY
                || cond.getOperator() == Operator.HAVING || cond.getOperator() == Operator.LIMIT) {

            final Condition cell = find(cond.getOperator());

            if (cell != null) {
                conditionList.remove(cell); // NOSONAR
            }
        }

        conditionList.add(cond);
    }

    /**
     *
     * @param operator
     * @return
     */
    private Condition find(final Operator operator) {
        Condition cond = null;

        for (final Condition element : conditionList) {
            cond = element;

            if (cond.getOperator() == operator) {
                return cond;
            }
        }

        return null;
    }
}
