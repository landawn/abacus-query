/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SortDirection;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * At present, Supports
 * {@code Where, OrderBy, GroupBy, Having, Join, Limit, ForUpdate, Union, UnionAll, Intersect, Except} clause. Each
 * {@code clause} is independent. A {@code clause} should not be included in another {@code clause}. If there more than
 * one {@code clause}, they should be composed in one {@code Criteria} condition.
 *
 * @author Haiyang Li
 * @since 0.8
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
     *
     */
    public Criteria() {
        super(Operator.EMPTY);
        conditionList = new ArrayList<>();
    }

    /**
     * 
     *
     * @return 
     */
    public String preselect() {
        return this.preselect;
    }

    /**
     * Gets the joins.
     *
     * @return
     */
    public List<Join> getJoins() {
        List<Join> joins = new ArrayList<>();

        for (Condition cond : conditionList) {
            if (cond instanceof Join) {
                joins.add((Join) cond);
            }
        }

        return joins;
    }

    /**
     * Gets the where.
     *
     * @return
     */
    public Cell getWhere() {
        return (Cell) find(Operator.WHERE);
    }

    /**
     * Gets the group by.
     *
     * @return
     */
    public Cell getGroupBy() {
        return (Cell) find(Operator.GROUP_BY);
    }

    /**
     * Gets the having.
     *
     * @return
     */
    public Cell getHaving() {
        return (Cell) find(Operator.HAVING);
    }

    /**
     * Gets the aggregation.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Cell> getAggregation() {
        List<Cell> result = null;

        for (Condition cond : conditionList) {
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
     * Gets the order by.
     *
     * @return
     */
    public Cell getOrderBy() {
        return (Cell) find(Operator.ORDER_BY);
    }

    /**
     * Gets the limit.
     *
     * @return
     */
    public Limit getLimit() {
        return (Limit) find(Operator.LIMIT);
    }

    /**
     * Gets the conditions.
     *
     * @return
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     *
     * @param operator
     * @return
     */
    public List<Condition> get(Operator operator) {
        List<Condition> conditions = new ArrayList<>();

        for (Condition cond : conditionList) {
            if (cond.getOperator() == operator) {
                conditions.add(cond);
            }
        }

        return conditions;
    }

    /**
     *
     * @param conditions
     */
    void add(Condition... conditions) {
        addConditions(conditions);
    }

    /**
     *
     * @param conditions
     */
    void add(Collection<? extends Condition> conditions) {
        addConditions(conditions);
    }

    /**
     *
     * @param operator
     */
    void remove(Operator operator) {
        List<Condition> conditions = get(operator);
        remove(conditions);
    }

    /**
     *
     * @param conditions
     */
    void remove(Condition... conditions) {
        for (Condition cond : conditions) {
            conditionList.remove(cond); // NOSONAR
        }
    }

    /**
     *
     * @param conditions
     */
    void remove(Collection<? extends Condition> conditions) {
        conditionList.removeAll(conditions);
    }

    /**
     * Clear.
     */
    public void clear() {
        conditionList.clear();
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getParameters() {
        if (conditionList.size() > 0) {
            List<Object> parameters = new ArrayList<>();
            Collection<Join> joins = getJoins();

            for (Join join : joins) {
                parameters.addAll(join.getParameters());
            }

            Cell where = getWhere();

            if (where != null) {
                parameters.addAll(where.getParameters());
            }

            // group by no parameters.
            /*
             * Cell groupBy = getGroupBy();
             *
             * if (groupBy != null) { parameters.addAll(groupBy.getParameters()); }
             */
            Cell having = getHaving();

            if (having != null) {
                parameters.addAll(having.getParameters());
            }

            List<Cell> condList = getAggregation();

            for (Condition cond : condList) {
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
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        for (Condition condition : conditionList) {
            condition.clearParameters();
        }
    }

    /**
     * 
     *
     * @return 
     */
    public Criteria distinct() {
        this.preselect = WD.DISTINCT;

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public Criteria distinctBy(String columnNames) {
        this.preselect = Strings.isEmpty(columnNames) ? WD.DISTINCT : WD.DISTINCT + "(" + columnNames + ")";

        return this;
    }

    /**
     * 
     *
     * @return 
     */
    public Criteria distinctRow() {
        this.preselect = WD.DISTINCTROW;

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public Criteria distinctRowBy(String columnNames) {
        this.preselect = Strings.isEmpty(columnNames) ? WD.DISTINCTROW : WD.DISTINCTROW + "(" + columnNames + ")";

        return this;
    }

    /**
     * 
     *
     * @param preselect 
     * @return 
     */
    public Criteria preselect(final String preselect) {
        this.preselect = preselect;

        return this;
    }

    /**
     *
     * @param joins
     * @return
     */
    @SafeVarargs
    public final Criteria join(Join... joins) {
        add(joins);

        return this;
    }

    /**
     *
     * @param joins
     * @return
     */
    public Criteria join(Collection<Join> joins) {
        add(joins);

        return this;
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public Criteria join(String joinEntity) {
        add(new Join(joinEntity));

        return this;
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public Criteria join(String joinEntity, Condition condition) {
        add(new Join(joinEntity, condition));

        return this;
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public Criteria join(Collection<String> joinEntities, Condition condition) {
        add(new Join(joinEntities, condition));

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria where(Condition condition) {
        if (condition.getOperator() == Operator.WHERE) {
            add(condition);
        } else {
            add(new Where(condition));
        }

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria where(String condition) {
        add(new Where(CF.expr(condition)));

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria groupBy(Condition condition) {
        if (condition.getOperator() == Operator.GROUP_BY) {
            add(condition);
        } else {
            add(new GroupBy(condition));
        }

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    @SafeVarargs
    public final Criteria groupBy(String... propNames) {
        add(new GroupBy(propNames));

        return this;
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public Criteria groupBy(String propName, SortDirection direction) {
        add(new GroupBy(propName, direction));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria groupBy(Collection<String> propNames) {
        return groupBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public Criteria groupBy(Collection<String> propNames, SortDirection direction) {
        add(new GroupBy(propNames, direction));

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public Criteria groupBy(Map<String, SortDirection> orders) {
        add(new GroupBy(orders));

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria having(Condition condition) {
        if (condition.getOperator() == Operator.HAVING) {
            add(condition);
        } else {
            add(new Having(condition));
        }

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria having(String condition) {
        add(new Having(CF.expr(condition)));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria orderByAsc(final String... propNames) {
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria orderByAsc(final Collection<String> propNames) {
        add(CF.orderByAsc(propNames));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria orderByDesc(final String... propNames) {
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria orderByDesc(final Collection<String> propNames) {
        add(CF.orderByDesc(propNames));

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria orderBy(Condition condition) {
        if (condition.getOperator() == Operator.ORDER_BY) {
            add(condition);
        } else {
            add(new OrderBy(condition));
        }

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    @SafeVarargs
    public final Criteria orderBy(String... propNames) {
        add(new OrderBy(propNames));

        return this;
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public Criteria orderBy(String propName, SortDirection direction) {
        add(new OrderBy(propName, direction));

        return this;
    }

    /**
     *
     * @param propNames
     * @return
     */
    public Criteria orderBy(Collection<String> propNames) {
        return orderBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public Criteria orderBy(Collection<String> propNames, SortDirection direction) {
        add(new OrderBy(propNames, direction));

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public Criteria orderBy(Map<String, SortDirection> orders) {
        add(new OrderBy(orders));

        return this;
    }

    /**
     *
     * @param condition
     * @return
     */
    public Criteria limit(Limit condition) {
        add(condition);

        return this;
    }

    /**
     *
     * @param count
     * @return
     */
    public Criteria limit(final int count) {
        add(CF.limit(count));

        return this;
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public Criteria limit(final int offset, final int count) {
        add(CF.limit(offset, count));

        return this;
    }

    /**
     *
     *
     * @param expr
     * @return
     */
    public Criteria limit(final String expr) {
        add(CF.limit(expr));

        return this;
    }

    /**
     *
     * @param subQuery
     * @return
     */
    public Criteria union(SubQuery subQuery) {
        add(new Union(subQuery));

        return this;
    }

    /**
     *
     * @param subQuery
     * @return
     */
    public Criteria unionAll(SubQuery subQuery) {
        add(new UnionAll(subQuery));

        return this;
    }

    /**
     *
     * @param subQuery
     * @return
     */
    public Criteria intersect(SubQuery subQuery) {
        add(new Intersect(subQuery));

        return this;
    }

    /**
     *
     * @param subQuery
     * @return
     */
    public Criteria except(SubQuery subQuery) {
        add(new Except(subQuery));

        return this;
    }

    /**
     *
     * @param subQuery
     * @return
     */
    public Criteria minus(SubQuery subQuery) {
        add(new Minus(subQuery));

        return this;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        Criteria result = (Criteria) super.copy();

        result.conditionList = new ArrayList<>();

        for (Condition cond : conditionList) {
            result.conditionList.add(cond.copy());
        }

        return (T) result;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        String preselect = Strings.isEmpty(this.preselect) ? Strings.EMPTY_STRING : WD.SPACE + this.preselect; //NOSONAR
        String join = Strings.EMPTY_STRING;
        String where = Strings.EMPTY_STRING;
        String groupBy = Strings.EMPTY_STRING;
        String having = Strings.EMPTY_STRING;
        String orderBy = Strings.EMPTY_STRING;
        String limit = Strings.EMPTY_STRING;
        String forUpdate = Strings.EMPTY_STRING;
        String aggregate = Strings.EMPTY_STRING;

        for (Condition cond : conditionList) {
            if (cond.getOperator() == Operator.WHERE) {
                where += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.ORDER_BY) {
                orderBy += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.GROUP_BY) {
                groupBy += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.HAVING) {
                having += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond.getOperator() == Operator.LIMIT) {
                limit += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else if (cond instanceof Join) {
                join += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            } else {
                aggregate += (WD._SPACE + cond.toString(namingPolicy)); //NOSONAR
            }
        }

        return preselect + join + where + groupBy + having + aggregate + orderBy + limit + forUpdate;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = Strings.isEmpty(preselect) ? 0 : preselect.hashCode();
        h = (h * 31) + conditionList.hashCode();

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof Criteria && N.equals(((Criteria) obj).preselect, preselect) && N.equals(((Criteria) obj).conditionList, conditionList));
    }

    /**
     *
     * @param conditions
     */
    private void checkConditions(Collection<? extends Condition> conditions) {
        for (Condition cond : conditions) {
            checkCondition(cond);
        }
    }

    /**
     *
     * @param conditions
     */
    private void checkConditions(Condition... conditions) {
        for (Condition cond : conditions) {
            checkCondition(cond);
        }
    }

    /**
     *
     * @param cond
     */
    private void checkCondition(Condition cond) {
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
    private void addConditions(Collection<? extends Condition> conditions) {
        checkConditions(conditions);

        for (Condition cond : conditions) {
            addCondition(cond);
        }
    }

    /**
     * Adds the conditions.
     *
     * @param conditions
     */
    private void addConditions(Condition... conditions) {
        checkConditions(conditions);

        for (Condition cond : conditions) {
            addCondition(cond);
        }
    }

    /**
     * Adds the condition.
     *
     * @param cond
     */
    private void addCondition(Condition cond) {
        if (cond.getOperator() == Operator.WHERE || cond.getOperator() == Operator.ORDER_BY || cond.getOperator() == Operator.GROUP_BY
                || cond.getOperator() == Operator.HAVING || cond.getOperator() == Operator.LIMIT) {

            Condition cell = find(cond.getOperator());

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
    private Condition find(Operator operator) {
        Condition cond = null;

        for (int i = 0; i < conditionList.size(); i++) {
            cond = conditionList.get(i);

            if (cond.getOperator() == operator) {
                return cond;
            }
        }

        return null;
    }
}
