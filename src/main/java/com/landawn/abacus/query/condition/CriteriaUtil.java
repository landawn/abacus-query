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
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;

/**
 * Utility class for working with Criteria and clause operators.
 * This class provides helper methods to identify and manipulate SQL clause operators
 * and conditions within the criteria framework.
 * 
 * <p>CriteriaUtil maintains the proper SQL clause ordering and provides methods to
 * check if operators or conditions are clauses. It also provides protected access
 * to Criteria's add and remove methods for framework use.</p>
 * 
 * <p>The class maintains a set of valid clause operators in their proper SQL order:</p>
 * <ol>
 *   <li>JOIN operations (JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN, CROSS_JOIN, INNER_JOIN, NATURAL_JOIN)</li>
 *   <li>WHERE</li>
 *   <li>GROUP_BY</li>
 *   <li>HAVING</li>
 *   <li>ORDER_BY</li>
 *   <li>LIMIT</li>
 *   <li>Set operations (UNION_ALL, UNION, INTERSECT, EXCEPT, MINUS)</li>
 * </ol>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if an operator is a clause operator
 * boolean isClause = CriteriaUtil.isClause(Operator.WHERE);   // true
 * boolean notClause = CriteriaUtil.isClause(Operator.EQUAL);   // false
 * 
 * // Get all clause operators
 * Set<Operator> clauses = CriteriaUtil.getClauseOperators();
 * }</pre>
 * 
 * @see Criteria
 * @see Operator
 * @see Clause
 */
@Internal
public final class CriteriaUtil {

    private static final ImmutableSet<Operator> clauseOperators;

    static {
        final Set<Operator> set = N.newLinkedHashSet();
        // it has order, don't change the order.
        set.add(Operator.JOIN);
        set.add(Operator.LEFT_JOIN);
        set.add(Operator.RIGHT_JOIN);
        set.add(Operator.FULL_JOIN);
        set.add(Operator.CROSS_JOIN);
        set.add(Operator.INNER_JOIN);
        set.add(Operator.NATURAL_JOIN);
        set.add(Operator.WHERE);
        set.add(Operator.GROUP_BY);
        set.add(Operator.HAVING);
        set.add(Operator.ORDER_BY);
        set.add(Operator.LIMIT);
        // clauseOperators.add(Operator.FOR_UPDATE);
        // Notice: If there are several connection operators,
        // this is their order.
        set.add(Operator.UNION_ALL);
        set.add(Operator.UNION);
        set.add(Operator.INTERSECT);
        set.add(Operator.EXCEPT);
        set.add(Operator.MINUS);

        clauseOperators = ImmutableSet.wrap(set);
    }

    private CriteriaUtil() {
        // singleton
    }

    /**
     * Gets the set of all valid clause operators.
     * The set maintains the proper SQL clause ordering and is immutable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Operator> clauseOps = CriteriaUtil.getClauseOperators();
     * // Returns an immutable set containing: JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN,
     * // CROSS_JOIN, INNER_JOIN, NATURAL_JOIN, WHERE, GROUP_BY, HAVING, ORDER_BY,
     * // LIMIT, UNION_ALL, UNION, INTERSECT, EXCEPT, MINUS
     *
     * for (Operator op : clauseOps) {
     *     System.out.println(op.getName());
     * }
     * }</pre>
     *
     * @return an immutable set of clause operators in proper SQL order
     */
    public static Set<Operator> getClauseOperators() {
        return clauseOperators;
    }

    /**
     * Checks if the given operator is a valid clause operator.
     * Clause operators represent major SQL query components like WHERE, JOIN, GROUP BY, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result1 = CriteriaUtil.isClause(Operator.WHERE);      // true
     * boolean result2 = CriteriaUtil.isClause(Operator.ORDER_BY);   // true
     * boolean result3 = CriteriaUtil.isClause(Operator.LEFT_JOIN);  // true
     * boolean result4 = CriteriaUtil.isClause(Operator.EQUAL);      // false
     * boolean result5 = CriteriaUtil.isClause(Operator.AND);        // false
     * boolean result6 = CriteriaUtil.isClause((Operator) null);     // false
     * }</pre>
     *
     * @param operator the operator to check
     * @return {@code true} if the operator is a clause operator, {@code false} otherwise
     */
    public static boolean isClause(final Operator operator) {
        return operator != null && clauseOperators.contains(operator);
    }

    /**
     * Checks if the given operator string represents a valid clause operator.
     * This method converts the string to an Operator and checks if it's a clause.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result1 = CriteriaUtil.isClause("WHERE");      // true
     * boolean result2 = CriteriaUtil.isClause("ORDER BY");   // true
     * boolean result3 = CriteriaUtil.isClause("GROUP BY");   // true
     * boolean result4 = CriteriaUtil.isClause("=");           // false
     * boolean result5 = CriteriaUtil.isClause("AND");         // false
     * }</pre>
     *
     * @param operator the operator string to check
     * @return {@code true} if the operator string represents a clause operator, {@code false} otherwise
     */
    public static boolean isClause(final String operator) {
        return isClause(Operator.of(operator));
    }

    /**
     * Checks if the given condition is a clause condition.
     * A condition is a clause if its operator is a clause operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where whereClause = new Where(Filters.eq("status", "active"));
     * boolean result1 = CriteriaUtil.isClause(whereClause);   // true
     *
     * OrderBy orderByClause = new OrderBy("name");
     * boolean result2 = CriteriaUtil.isClause(orderByClause);   // true
     *
     * Equal equalCond = Filters.eq("age", 25);
     * boolean result3 = CriteriaUtil.isClause(equalCond);   // false
     *
     * boolean result4 = CriteriaUtil.isClause((Condition) null);   // false
     * }</pre>
     *
     * @param condition the condition to check
     * @return {@code true} if the condition has a clause operator, {@code false} if null or not a clause
     */
    public static boolean isClause(final Condition condition) {
        //        if (condition == null) {
        //            return false;
        //        }
        //
        //        if (condition instanceof Expression) {
        //            Expression exp = (Expression) condition;
        //
        //            if (N.isEmpty(exp.getLiteral())) {
        //                return false;
        //            } else {
        //                SQLParser sqlParser = SQLParser.valueOf(exp.getLiteral());
        //                String word = sqlParser.nextWord();
        //
        //                return isClause(word) || isClause(word + D._SPACE + sqlParser.nextWord());
        //            }
        //        } else {
        //            return isClause(condition.getOperator());
        //        }
        return condition != null && isClause(condition.getOperator());
    }

    /**
     * Adds conditions to the specified criteria.
     * This method provides access to the protected add method of Criteria for framework use.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * CriteriaUtil.add(criteria,
     *     new Where(Filters.eq("status", "active")),
     *     new OrderBy("name", SortDirection.ASC)
     * );
     * // criteria now contains WHERE status = 'active' ORDER BY name ASC
     * }</pre>
     *
     * @param criteria the criteria to add conditions to
     * @param conditions the conditions to add
     */
    public static void add(final Criteria criteria, final Condition... conditions) {
        criteria.add(conditions);
    }

    /**
     * Adds a collection of conditions to the specified criteria.
     * This method provides access to the protected add method of Criteria for framework use.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * List<Condition> conditions = Arrays.asList(
     *     new Where(Filters.eq("active", true)),
     *     new Limit(10)
     * );
     * CriteriaUtil.add(criteria, conditions);
     * // criteria now contains WHERE active = true LIMIT 10
     * }</pre>
     *
     * @param criteria the criteria to add conditions to
     * @param conditions the collection of conditions to add
     */
    public static void add(final Criteria criteria, final Collection<Condition> conditions) {
        criteria.add(conditions);
    }

    /**
     * Removes all conditions with the specified operator from the criteria.
     * This method provides access to the protected remove method of Criteria for framework use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = new Criteria()
     *     .where(Filters.eq("status", "active"))
     *     .orderBy("name", SortDirection.ASC);
     * CriteriaUtil.remove(criteria, Operator.WHERE);
     * // Removes the WHERE clause from the criteria
     * 
     * CriteriaUtil.remove(criteria, Operator.ORDER_BY);
     * // Removes all ORDER BY clauses
     * }</pre>
     * 
     * @param criteria the criteria to remove conditions from
     * @param operator the operator of conditions to remove
     */
    public static void remove(final Criteria criteria, final Operator operator) {
        criteria.remove(operator);
    }

    /**
     * Removes specific conditions from the criteria.
     * This method provides access to the protected remove method of Criteria for framework use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where whereClause = new Where(Filters.eq("status", "active"));
     * OrderBy orderByClause = new OrderBy("name");
     * Criteria criteria = new Criteria()
     *     .where(whereClause)
     *     .orderBy(orderByClause);
     * 
     * CriteriaUtil.remove(criteria, whereClause, orderByClause);
     * // Removes both specific conditions
     * }</pre>
     * 
     * @param criteria the criteria to remove conditions from
     * @param conditions the conditions to remove
     */
    public static void remove(final Criteria criteria, final Condition... conditions) {
        criteria.remove(conditions);
    }

    /**
     * Removes a collection of conditions from the criteria.
     * This method provides access to the protected remove method of Criteria for framework use.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where whereClause = new Where(Filters.eq("status", "active"));
     * Limit limitClause = new Limit(10);
     * List<Condition> conditionsToRemove = new ArrayList<>();
     * conditionsToRemove.add(whereClause);
     * conditionsToRemove.add(limitClause);
     * Criteria criteria = new Criteria()
     *     .where(whereClause)
     *     .limit(10);
     *
     * CriteriaUtil.remove(criteria, conditionsToRemove);
     * // Removes all conditions in the list
     * }</pre>
     *
     * @param criteria the criteria to remove conditions from
     * @param conditions the collection of conditions to remove
     */
    public static void remove(final Criteria criteria, final Collection<Condition> conditions) {
        criteria.remove(conditions);
    }
}
