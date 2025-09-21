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

import com.landawn.abacus.query.SK;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
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
 * <p>Usage example:</p>
 * <pre>{@code
 * // Build a complex query with multiple clauses
 * Criteria criteria = new Criteria()
 *     .join("orders", CF.eq("users.id", "orders.user_id"))
 *     .where(CF.and(
 *         CF.eq("users.status", "active"),
 *         CF.gt("orders.amount", 100)
 *     ))
 *     .groupBy("users.department")
 *     .having(CF.gt("COUNT(*)", 5))
 *     .orderBy("COUNT(*)", SortDirection.DESC)
 *     .limit(10);
 * 
 * // Using distinct
 * Criteria distinctUsers = new Criteria()
 *     .distinct()
 *     .where(CF.eq("active", true))
 *     .orderBy("name");
 * }</pre>
 * 
 * @see Condition
 * @see ConditionFactory
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * criteria.where(CF.eq("status", "active"))
     *         .orderBy("created_date", DESC)
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().distinct();
     * String modifier = criteria.preselect(); // Returns "DISTINCT"
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", CF.eq("users.id", "orders.user_id"))
     *     .leftJoin("payments", CF.eq("orders.id", "payments.order_id"));
     * List<Join> joins = criteria.getJoins(); // Returns 2 joins
     * }</pre>
     * 
     * @return a list of Join conditions, empty if none exist
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().where(CF.eq("status", "active"));
     * Cell where = criteria.getWhere(); // Returns the WHERE clause
     * Condition condition = where.getCondition(); // Gets the wrapped condition
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().groupBy("department", "role");
     * Cell groupBy = criteria.getGroupBy(); // Returns the GROUP BY clause
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("department")
     *     .having(CF.gt("COUNT(*)", 10));
     * Cell having = criteria.getHaving(); // Returns the HAVING clause
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
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery subQuery = CF.subQuery("SELECT id FROM archived_users");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("active", true))
     *     .union(subQuery);
     * List<Cell> aggregations = criteria.getAggregation(); // Returns the UNION
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().orderBy("name", DESC);
     * Cell orderBy = criteria.getOrderBy(); // Returns the ORDER BY clause
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().limit(10);
     * Limit limit = criteria.getLimit(); // Returns the LIMIT clause
     * int count = limit.getCount(); // Returns 10
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("active", true))
     *     .orderBy("name");
     * List<Condition> conditions = criteria.getConditions(); // Returns all conditions
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", condition1)
     *     .leftJoin("payments", condition2);
     * List<Condition> allJoins = criteria.get(Operator.JOIN);
     * List<Condition> leftJoins = criteria.get(Operator.LEFT_JOIN);
     * }</pre>
     * 
     * @param operator the operator to filter by
     * @return a list of conditions with the specified operator
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
     * 
     * @param conditions the conditions to add
     */
    void add(final Condition... conditions) {
        addConditions(conditions);
    }

    /**
     * Adds conditions to this criteria.
     * 
     * @param conditions the collection of conditions to add
     */
    void add(final Collection<? extends Condition> conditions) {
        addConditions(conditions);
    }

    /**
     * Removes all conditions with the specified operator.
     * 
     * @param operator the operator of conditions to remove
     */
    void remove(final Operator operator) {
        final List<Condition> conditions = get(operator);
        remove(conditions);
    }

    /**
     * Removes specific conditions from this criteria.
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("active", true))
     *     .orderBy("name");
     * criteria.clear();
     * // All conditions are removed
     * }</pre>
     */
    public void clear() {
        conditionList.clear();
    }

    /**
     * Gets all parameters from all conditions in the proper order.
     * The order follows SQL clause precedence: JOIN, WHERE, HAVING, aggregations.
     * Parameters from GROUP BY, ORDER BY, and LIMIT are not included as they typically don't have parameters.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("status", "active"))
     *     .having(CF.gt("COUNT(*)", 5));
     * List<Object> params = criteria.getParameters(); // Returns ["active", 5]
     * }</pre>
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
     * Clears all parameters from all conditions in this criteria.
     * This is useful for releasing resources when the criteria contains large parameter lists.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.in("id", largeIdList));
     * // Use the criteria...
     * criteria.clearParameters(); // Releases the large list
     * }</pre>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinct()
     *     .where(CF.eq("status", "active"));
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
     * <p>Example:</p>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinctRow()
     *     .where(CF.eq("active", true));
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
     * <p>Example:</p>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .preselect("SQL_CALC_FOUND_ROWS")
     *     .where(CF.eq("active", true));
     * // Results in: SELECT SQL_CALC_FOUND_ROWS ... WHERE active = true
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join(
     *         new LeftJoin("orders", CF.eq("users.id", "orders.user_id")),
     *         new InnerJoin("products", CF.eq("orders.product_id", "products.id"))
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
     * <p>Example:</p>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders")
     *     .where(CF.eq("users.id", "orders.user_id"));
     * // Results in: JOIN orders WHERE users.id = orders.user_id
     * }</pre>
     * 
     * @param joinEntity the table/entity to join
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", CF.eq("users.id", "orders.user_id"))
     *     .where(CF.eq("users.status", "active"));
     * // Results in: JOIN orders ON users.id = orders.user_id WHERE users.status = 'active'
     * }</pre>
     * 
     * @param joinEntity the table/entity to join
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
     * <p>Example:</p>
     * <pre>{@code
     * Collection<String> tables = Arrays.asList("orders", "order_items");
     * Criteria criteria = new Criteria()
     *     .join(tables, CF.eq("id", "order_id"));
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.and(
     *         CF.eq("status", "active"),
     *         CF.gt("age", 18),
     *         CF.like("email", "%@company.com")
     *     ));
     * }</pre>
     * 
     * @param condition the WHERE condition
     * @return this Criteria instance for method chaining
     */
    public Criteria where(final Condition condition) {
        if (condition.getOperator() == Operator.WHERE) {
            add(condition);
        } else {
            add(new Where(condition));
        }

        return this;
    }

    /**
     * Sets the WHERE clause using a string expression.
     * Useful for complex conditions that are easier to express as SQL.
     * 
     * <p>Example:</p>
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
        add(new Where(CF.expr(condition)));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause.
     * If a GROUP BY clause already exists, it will be replaced.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy(CF.expr("YEAR(order_date), MONTH(order_date)"));
     * }</pre>
     * 
     * @param condition the GROUP BY condition
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Condition condition) {
        if (condition.getOperator() == Operator.GROUP_BY) {
            add(condition);
        } else {
            add(new GroupBy(condition));
        }

        return this;
    }

    /**
     * Sets the GROUP BY clause with property names.
     * Groups results by the specified columns in ascending order.
     * 
     * <p>Example:</p>
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
     * Sets the GROUP BY clause with a property and sort direction.
     * 
     * <p>Example:</p>
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
     * Sets the GROUP BY clause with two properties and their sort directions.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("year", DESC, "month", ASC);
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
     * Sets the GROUP BY clause with three properties and their sort directions.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("country", ASC, "state", ASC, "city", DESC);
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
     * Sets the GROUP BY clause with multiple properties.
     * All properties will be sorted in ascending order.
     * 
     * <p>Example:</p>
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
     * Sets the GROUP BY clause with multiple properties and sort direction.
     * All properties will use the same sort direction.
     * 
     * <p>Example:</p>
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
     * Sets the GROUP BY clause with custom sort directions per property.
     * The map should be a LinkedHashMap to preserve order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, SortDirection> grouping = new LinkedHashMap<>();
     * grouping.put("department", ASC);
     * grouping.put("salary_range", DESC);
     * grouping.put("years_experience", DESC);
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .groupBy("department")
     *     .having(CF.and(
     *         CF.gt("COUNT(*)", 10),
     *         CF.lt("AVG(salary)", 100000)
     *     ));
     * }</pre>
     * 
     * @param condition the HAVING condition
     * @return this Criteria instance for method chaining
     */
    public Criteria having(final Condition condition) {
        if (condition.getOperator() == Operator.HAVING) {
            add(condition);
        } else {
            add(new Having(condition));
        }

        return this;
    }

    /**
     * Sets the HAVING clause using a string expression.
     * Useful for aggregate function conditions.
     * 
     * <p>Example:</p>
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
        add(new Having(CF.expr(condition)));

        return this;
    }

    /**
     * Sets the ORDER BY clause with ascending order.
     * Convenience method that sorts all specified columns in ascending order.
     * 
     * <p>Example:</p>
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
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with ascending order.
     * 
     * <p>Example:</p>
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
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with descending order.
     * Convenience method that sorts all specified columns in descending order.
     * 
     * <p>Example:</p>
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
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with descending order.
     * 
     * <p>Example:</p>
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
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause.
     * If an ORDER BY clause already exists, it will be replaced.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Complex ordering expression
     * Criteria criteria = new Criteria()
     *     .orderBy(CF.expr("CASE WHEN priority = 'HIGH' THEN 1 ELSE 2 END, created_date DESC"));
     * }</pre>
     * 
     * @param condition the ORDER BY condition
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Condition condition) {
        if (condition.getOperator() == Operator.ORDER_BY) {
            add(condition);
        } else {
            add(new OrderBy(condition));
        }

        return this;
    }

    /**
     * Sets the ORDER BY clause with property names.
     * Orders by the specified columns in ascending order.
     * 
     * <p>Example:</p>
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
     * Sets the ORDER BY clause with a property and sort direction.
     * 
     * <p>Example:</p>
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
     * Sets the ORDER BY clause with two properties and their sort directions.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderBy("priority", DESC, "createdDate", ASC);
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
     * Sets the ORDER BY clause with three properties and their sort directions.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .orderBy("category", ASC, "price", DESC, "name", ASC);
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
     * Sets the ORDER BY clause with multiple properties.
     * All properties will be sorted in ascending order.
     * 
     * <p>Example:</p>
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
     * Sets the ORDER BY clause with multiple properties and sort direction.
     * All properties will use the same sort direction.
     * 
     * <p>Example:</p>
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
     * Sets the ORDER BY clause with custom sort directions per property.
     * The map should be a LinkedHashMap to preserve order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, SortDirection> ordering = new LinkedHashMap<>();
     * ordering.put("priority", DESC);
     * ordering.put("createdDate", DESC);
     * ordering.put("name", ASC);
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
     * <p>Example:</p>
     * <pre>{@code
     * Limit customLimit = CF.limit(100);
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
     * Sets the LIMIT clause with a count.
     * Limits the number of rows returned by the query.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("status", "active"))
     *     .limit(10);
     * // Results in: WHERE status = 'active' LIMIT 10
     * }</pre>
     * 
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final int count) {
        add(CF.limit(count));

        return this;
    }

    /**
     * Sets the LIMIT clause with offset and count.
     * Used for pagination - skips 'offset' rows and returns up to 'count' rows.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Page 3 with 20 items per page (skip 40, take 20)
     * Criteria criteria = new Criteria()
     *     .orderBy("id")
     *     .limit(40, 20);
     * // Results in: ORDER BY id LIMIT 40, 20
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     */
    public Criteria limit(final int offset, final int count) {
        add(CF.limit(offset, count));

        return this;
    }

    /**
     * Sets the LIMIT clause using a string expression.
     * Allows for database-specific limit syntax.
     * 
     * <p>Example:</p>
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
        add(CF.limit(expr));

        return this;
    }

    /**
     * Adds a UNION operation with a subquery.
     * UNION combines results and removes duplicates.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery archivedUsers = CF.subQuery("SELECT * FROM archived_users WHERE active = true");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("status", "active"))
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
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery pendingOrders = CF.subQuery("SELECT * FROM pending_orders");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("status", "completed"))
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
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery premiumUsers = CF.subQuery("SELECT user_id FROM premium_members");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("active", true))
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
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery excludedUsers = CF.subQuery("SELECT user_id FROM blacklist");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("status", "active"))
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
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery inactiveUsers = CF.subQuery("SELECT user_id FROM inactive_users");
     * Criteria criteria = new Criteria()
     *     .where(CF.eq("registered", true))
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria original = new Criteria()
     *     .where(CF.eq("status", "active"))
     *     .orderBy("name");
     * Criteria copy = original.copy();
     * // copy is independent of original
     * copy.limit(10); // Doesn't affect original
     * }</pre>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .distinct()
     *     .where(CF.eq("isActive", true))
     *     .orderBy("createdDate", DESC);
     * String sql = criteria.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: " DISTINCT WHERE is_active = true ORDER BY created_date DESC"
     * }</pre>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria c1 = new Criteria().where(CF.eq("status", "active"));
     * Criteria c2 = new Criteria().where(CF.eq("status", "active"));
     * boolean sameHash = c1.hashCode() == c2.hashCode(); // true
     * }</pre>
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
     * <p>Example:</p>
     * <pre>{@code
     * Criteria c1 = new Criteria()
     *     .distinct()
     *     .where(CF.eq("status", "active"));
     * Criteria c2 = new Criteria()
     *     .distinct()
     *     .where(CF.eq("status", "active"));
     * boolean isEqual = c1.equals(c2); // Returns true
     * }</pre>
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
     * @param conditions
     */
    private void checkConditions(final Collection<? extends Condition> conditions) {
        for (final Condition cond : conditions) {
            checkCondition(cond);
        }
    }

    /**
     *
     * @param conditions
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
        if (!CriteriaUtil.isClause(cond.getOperator())) {
            throw new IllegalArgumentException(
                    "Criteria only accepts condition: " + CriteriaUtil.getClauseOperators() + ". Doesn't accept[" + cond.getOperator() + "]. ");
        }
    }

    /**
     * Adds the conditions.
     *
     * @param conditions
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
     * @param conditions
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