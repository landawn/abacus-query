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
     * The set maintains the proper SQL clause ordering.
     * 
     * @return an immutable set of clause operators
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Set<Operator> clauses = CriteriaUtil.getClauseOperators();
     * // Contains: JOIN, LEFT_JOIN, WHERE, GROUP_BY, HAVING, etc.
     * }</pre>
     */
    public static Set<Operator> getClauseOperators() {
        return clauseOperators;
    }

    /**
     * Checks if the given operator is a valid clause operator.
     * 
     * @param operator the operator to check
     * @return true if the operator is a clause operator, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean isClause = CriteriaUtil.isClause(Operator.WHERE); // Returns true
     * boolean isClause = CriteriaUtil.isClause(Operator.EQUAL); // Returns false
     * }</pre>
     */
    public static boolean isClause(final Operator operator) {
        return operator != null && clauseOperators.contains(operator);
    }

    /**
     * Checks if the given operator string represents a valid clause operator.
     * 
     * @param operator the operator string to check
     * @return true if the operator string represents a clause operator, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean isClause = CriteriaUtil.isClause("WHERE"); // Returns true
     * boolean isClause = CriteriaUtil.isClause("="); // Returns false
     * }</pre>
     */
    public static boolean isClause(final String operator) {
        return isClause(Operator.getOperator(operator));
    }

    /**
     * Checks if the given condition is a clause condition.
     * 
     * @param condition the condition to check
     * @return true if the condition has a clause operator, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition where = new Where(CF.eq("status", "active"));
     * boolean isClause = CriteriaUtil.isClause(where); // Returns true
     * 
     * Condition eq = CF.eq("status", "active");
     * boolean isClause = CriteriaUtil.isClause(eq); // Returns false
     * }</pre>
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
     * This method provides access to the protected add method of Criteria.
     * 
     * @param criteria the criteria to add conditions to
     * @param conditions the conditions to add
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Criteria criteria = new Criteria();
     * CriteriaUtil.add(criteria, new Where(CF.eq("status", "active")));
     * }</pre>
     */
    public static void add(final Criteria criteria, final Condition... conditions) {
        criteria.add(conditions);
    }

    /**
     * Adds a collection of conditions to the specified criteria.
     * This method provides access to the protected add method of Criteria.
     * 
     * @param criteria the criteria to add conditions to
     * @param conditions the collection of conditions to add
     */
    public static void add(final Criteria criteria, final Collection<Condition> conditions) {
        criteria.add(conditions);
    }

    /**
     * Removes all conditions with the specified operator from the criteria.
     * This method provides access to the protected remove method of Criteria.
     * 
     * @param criteria the criteria to remove conditions from
     * @param operator the operator of conditions to remove
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CriteriaUtil.remove(criteria, Operator.WHERE);
     * // Removes the WHERE clause
     * }</pre>
     */
    public static void remove(final Criteria criteria, final Operator operator) {
        criteria.remove(operator);
    }

    /**
     * Removes specific conditions from the criteria.
     * This method provides access to the protected remove method of Criteria.
     * 
     * @param criteria the criteria to remove conditions from
     * @param conditions the conditions to remove
     */
    public static void remove(final Criteria criteria, final Condition... conditions) {
        criteria.remove(conditions);
    }

    /**
     * Removes a collection of conditions from the criteria.
     * This method provides access to the protected remove method of Criteria.
     * 
     * @param criteria the criteria to remove conditions from
     * @param conditions the collection of conditions to remove
     */
    public static void remove(final Criteria criteria, final Collection<Condition> conditions) {
        criteria.remove(conditions);
    }
}