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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a complete query criteria that can contain multiple SQL clauses.
 * This class serves as a builder and container for constructing complex SQL queries
 * by combining various clauses like WHERE, JOIN, GROUP BY, HAVING, ORDER BY, LIMIT, etc.
 * 
 * <p>At present, supports the following clauses:</p>
 * <ul>
 *   <li>{@code Where} - Filter rows</li>
 *   <li>{@code OrderBy} - Sort results</li>
 *   <li>{@code GroupBy} - Group rows</li>
 *   <li>{@code Having} - Filter grouped results</li>
 *   <li>{@code Join} - Join tables</li>
 *   <li>{@code Limit} - Limit result count</li>
 *   <li>{@code Union/UnionAll/Intersect/Except} - Set operations</li>
 * </ul>
 * 
 * <p>Each clause is independent. A clause should not be included in another clause.
 * If there are multiple clauses, they should be composed in one {@code Criteria} condition.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * Criteria criteria = new Criteria()
 *     .join("orders", CF.eq("users.id", "orders.user_id"))
 *     .where(CF.and(
 *         CF.eq("users.status", "active"),
 *         CF.gt("orders.amount", 100)
 *     ))
 *     .groupBy("users.department")
 *     .having(CF.gt("COUNT(*)", 5))
 *     .orderBy("total_amount", SortDirection.DESC)
 *     .limit(10);
 * }</pre>
 * 
 * @see Condition
 * @see ConditionFactory
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
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * }</pre>
     */
    public Criteria() {
        super(Operator.EMPTY);
        conditionList = new ArrayList<>();
    }

    /**
     * Gets the preselect modifier (e.g., DISTINCT, DISTINCTROW).
     * 
     * @return the preselect modifier, or null if not set
     */
    public String preselect() {
        return preselect;
    }

    /**
     * Gets all JOIN clauses in this criteria.
     * 
     * @return a list of Join conditions, empty if none exist
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .join("orders", CF.eq("users.id", "orders.user_id"))
     *     .leftJoin("payments", CF.eq("orders.id", "payments.order_id"));
     * List<Join> joins = criteria.getJoins(); // Returns 2 joins
     * }</pre>
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
     * 
     * @return the Where condition, or null if not set
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria().where(CF.eq("status", "active"));
     * Cell where = criteria.getWhere();
     * }</pre>
     */
    public Cell getWhere() {
        return (Cell) find(Operator.WHERE);
    }

    /**
     * Gets the GROUP BY clause from this criteria.
     * 
     * @return the GroupBy condition, or null if not set
     */
    public Cell getGroupBy() {
        return (Cell) find(Operator.GROUP_BY);
    }

    /**
     * Gets the HAVING clause from this criteria.
     * 
     * @return the Having condition, or null if not set
     */
    public Cell getHaving() {
        return (Cell) find(Operator.HAVING);
    }

    /**
     * Gets all aggregation operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS).
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
     * 
     * @return the OrderBy condition, or null if not set
     */
    public Cell getOrderBy() {
        return (Cell) find(Operator.ORDER_BY);
    }

    /**
     * Gets the LIMIT clause from this criteria.
     * 
     * @return the Limit condition, or null if not set
     */
    public Limit getLimit() {
        return (Limit) find(Operator.LIMIT);
    }

    /**
     * Gets all conditions in this criteria.
     * 
     * @return a list of all conditions
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     * Gets all conditions with the specified operator.
     * 
     * @param operator the operator to filter by
     * @return a list of conditions with the specified operator
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<Condition> joins = criteria.get(Operator.JOIN);
     * }</pre>
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
     * 
     * <p>Example:</p>
     * <pre>{@code
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
            return parameters;
        } else {
            return N.emptyList();
        }
    }

    /**
     * Clears all parameters from all conditions in this criteria.
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : conditionList) {
            condition.clearParameters();
        }
    }

    /**
     * Sets the DISTINCT modifier for the query.
     * 
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.distinct().where(CF.eq("status", "active"));
     * // Results in: SELECT DISTINCT ... WHERE status = 'active'
     * }</pre>
     */
    public Criteria distinct() {
        preselect = SK.DISTINCT;

        return this;
    }

    /**
     * Sets the DISTINCT modifier with specific columns.
     * 
     * @param columnNames the columns to apply DISTINCT to
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.distinctBy("department, location");
     * // Results in: SELECT DISTINCT(department, location) ...
     * }</pre>
     */
    public Criteria distinctBy(final String columnNames) {
        preselect = Strings.isEmpty(columnNames) ? SK.DISTINCT : SK.DISTINCT + "(" + columnNames + ")";

        return this;
    }

    /**
     * Sets the DISTINCTROW modifier for the query.
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
     * @param columnNames the columns to apply DISTINCTROW to
     * @return this Criteria instance for method chaining
     */
    public Criteria distinctRowBy(final String columnNames) {
        preselect = Strings.isEmpty(columnNames) ? SK.DISTINCTROW : SK.DISTINCTROW + "(" + columnNames + ")";

        return this;
    }

    /**
     * Sets a custom preselect modifier.
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
     * 
     * @param joins the JOIN clauses to add
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.join(
     *     new LeftJoin("orders", CF.eq("users.id", "orders.user_id")),
     *     new InnerJoin("products", CF.eq("orders.product_id", "products.id"))
     * );
     * }</pre>
     */
    public final Criteria join(final Join... joins) {
        add(joins);

        return this;
    }

    /**
     * Adds JOIN clauses to this criteria.
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
     * 
     * @param joinEntity the table/entity to join
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.join("orders");
     * // Results in: JOIN orders
     * }</pre>
     */
    public Criteria join(final String joinEntity) {
        add(new Join(joinEntity));

        return this;
    }

    /**
     * Adds an INNER JOIN with a condition to this criteria.
     * 
     * @param joinEntity the table/entity to join
     * @param condition the join condition
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.join("orders", CF.eq("users.id", "orders.user_id"));
     * // Results in: JOIN orders ON users.id = orders.user_id
     * }</pre>
     */
    public Criteria join(final String joinEntity, final Condition condition) {
        add(new Join(joinEntity, condition));

        return this;
    }

    /**
     * Adds an INNER JOIN with multiple entities and a condition.
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
     * 
     * @param condition the WHERE condition
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.where(CF.and(
     *     CF.eq("status", "active"),
     *     CF.gt("age", 18)
     * ));
     * }</pre>
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
     * 
     * @param condition the WHERE condition as a string
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.where("age > 18 AND status = 'active'");
     * }</pre>
     */
    public Criteria where(final String condition) {
        add(new Where(CF.expr(condition)));

        return this;
    }

    /**
     * Sets or replaces the GROUP BY clause.
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
     * 
     * @param propNames the property names to group by
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.groupBy("department", "location");
     * }</pre>
     */
    public final Criteria groupBy(final String... propNames) {
        add(new GroupBy(propNames));

        return this;
    }

    /**
     * Sets the GROUP BY clause with a property and sort direction.
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
     * 
     * @param propNames the collection of property names to group by
     * @return this Criteria instance for method chaining
     */
    public Criteria groupBy(final Collection<String> propNames) {
        return groupBy(propNames, SortDirection.ASC);
    }

    /**
     * Sets the GROUP BY clause with multiple properties and sort direction.
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
     * 
     * @param condition the HAVING condition
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.groupBy("department")
     *         .having(CF.gt("COUNT(*)", 10));
     * }</pre>
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
     * 
     * @param propNames the property names to order by
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.orderByAsc("lastName", "firstName");
     * }</pre>
     */
    public Criteria orderByAsc(final String... propNames) {
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with ascending order.
     * 
     * @param propNames the collection of property names to order by
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByAsc(final Collection<String> propNames) {
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with descending order.
     * 
     * @param propNames the property names to order by
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByDesc(final String... propNames) {
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     * Sets the ORDER BY clause with descending order.
     * 
     * @param propNames the collection of property names to order by
     * @return this Criteria instance for method chaining
     */
    public Criteria orderByDesc(final Collection<String> propNames) {
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     * Sets or replaces the ORDER BY clause.
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
     * 
     * @param propNames the collection of property names to order by
     * @return this Criteria instance for method chaining
     */
    public Criteria orderBy(final Collection<String> propNames) {
        return orderBy(propNames, SortDirection.ASC);
    }

    /**
     * Sets the ORDER BY clause with multiple properties and sort direction.
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
     * 
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.limit(10);
     * // Results in: LIMIT 10
     * }</pre>
     */
    public Criteria limit(final int count) {
        add(CF.limit(count));

        return this;
    }

    /**
     * Sets the LIMIT clause with offset and count.
     * 
     * @param offset the number of rows to skip
     * @param count the maximum number of results to return
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.limit(20, 10);
     * // Results in: LIMIT 20, 10 (skip 20, take 10)
     * }</pre>
     */
    public Criteria limit(final int offset, final int count) {
        add(CF.limit(offset, count));

        return this;
    }

    /**
     * Sets the LIMIT clause using a string expression.
     * 
     * @param expr the LIMIT expression as a string
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.limit("10 OFFSET 20");
     * }</pre>
     */
    public Criteria limit(final String expr) {
        add(CF.limit(expr));

        return this;
    }

    /**
     * Adds a UNION operation with a subquery.
     * UNION combines results and removes duplicates.
     * 
     * @param subQuery the subquery to union with
     * @return this Criteria instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * criteria.where(CF.eq("status", "active"))
     *         .union(subQuery);
     * }</pre>
     */
    public Criteria union(final SubQuery subQuery) {
        add(new Union(subQuery));

        return this;
    }

    /**
     * Adds a UNION ALL operation with a subquery.
     * UNION ALL combines results and keeps duplicates.
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
     * @param subQuery the subquery to except
     * @return this Criteria instance for method chaining
     */
    public Criteria except(final SubQuery subQuery) {
        add(new Except(subQuery));

        return this;
    }

    /**
     * Adds a MINUS operation with a subquery.
     * MINUS is equivalent to EXCEPT in some databases.
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
     * All conditions are also copied.
     * 
     * @param <T> the type of condition to return
     * @return a new Criteria instance with copied values
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria original = new Criteria().where(CF.eq("status", "active"));
     * Criteria copy = original.copy();
     * // copy is independent of original
     * }</pre>
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
     * The output follows SQL clause ordering conventions.
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
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
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
