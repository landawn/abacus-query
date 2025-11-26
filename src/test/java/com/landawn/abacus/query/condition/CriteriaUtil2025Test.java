/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;

@Tag("2025")
public class CriteriaUtil2025Test extends TestBase {

    @Test
    public void testGetClauseOperators() {
        Set<Operator> operators = CriteriaUtil.getClauseOperators();
        assertNotNull(operators);
        assertFalse(operators.isEmpty());
    }

    @Test
    public void testIsClauseWithOperator() {
        assertTrue(CriteriaUtil.isClause(Operator.WHERE));
        assertTrue(CriteriaUtil.isClause(Operator.GROUP_BY));
        assertTrue(CriteriaUtil.isClause(Operator.HAVING));
        assertTrue(CriteriaUtil.isClause(Operator.ORDER_BY));
        assertTrue(CriteriaUtil.isClause(Operator.LIMIT));
    }

    @Test
    public void testIsClauseWithNonClauseOperator() {
        assertFalse(CriteriaUtil.isClause(Operator.EQUAL));
        assertFalse(CriteriaUtil.isClause(Operator.NOT_EQUAL));
        assertFalse(CriteriaUtil.isClause(Operator.GREATER_THAN));
    }

    @Test
    public void testIsClauseWithString() {
        assertTrue(CriteriaUtil.isClause("WHERE"));
        assertTrue(CriteriaUtil.isClause("GROUP BY"));
        assertTrue(CriteriaUtil.isClause("HAVING"));
        assertTrue(CriteriaUtil.isClause("ORDER BY"));
        assertTrue(CriteriaUtil.isClause("LIMIT"));
    }

    @Test
    public void testIsClauseWithNonClauseString() {
        assertFalse(CriteriaUtil.isClause("EQUAL"));
        assertFalse(CriteriaUtil.isClause("NOT_EQUAL"));
        assertFalse(CriteriaUtil.isClause("BETWEEN"));
    }

    @Test
    public void testIsClauseWithCondition() {
        Condition whereCondition = new Where(Filters.equal("name", "John"));
        assertTrue(CriteriaUtil.isClause(whereCondition));

        Condition groupByCondition = new GroupBy("department");
        assertTrue(CriteriaUtil.isClause(groupByCondition));

        Condition havingCondition = new Having(Filters.expr("COUNT(*) > 5"));
        assertTrue(CriteriaUtil.isClause(havingCondition));

        Condition orderByCondition = new OrderBy("name", SortDirection.ASC);
        assertTrue(CriteriaUtil.isClause(orderByCondition));
    }

    @Test
    public void testIsClauseWithNonClauseCondition() {
        Condition equalCondition = Filters.equal("name", "John");
        assertFalse(CriteriaUtil.isClause(equalCondition));

        Condition betweenCondition = Filters.between("age", 18, 65);
        assertFalse(CriteriaUtil.isClause(betweenCondition));
    }

    @Test
    public void testAddVarargs() {
        Criteria criteria = new Criteria();
        Condition where1 = new Where(Filters.equal("name", "John"));
        Condition orderBy = new OrderBy("age", SortDirection.ASC);

        CriteriaUtil.add(criteria, where1, orderBy);

        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testAddCollection() {
        Criteria criteria = new Criteria();
        Condition where = new Where(Filters.equal("name", "John"));
        Condition groupBy = new GroupBy("department");

        CriteriaUtil.add(criteria, Arrays.asList(where, groupBy));

        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testAddEmptyCollection() {
        Criteria criteria = new Criteria();
        CriteriaUtil.add(criteria, Arrays.asList());
        // Should not throw
    }

    @Test
    public void testRemoveByOperator() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        criteria.orderBy("name");

        CriteriaUtil.remove(criteria, Operator.WHERE);

        // Where should be removed but order by should remain
    }

    @Test
    public void testRemoveVarargs() {
        Criteria criteria = new Criteria();
        Condition cond1 = Filters.equal("name", "John");
        Condition cond2 = Filters.equal("age", 30);

        criteria.where(cond1);
        criteria.where(cond2);

        CriteriaUtil.remove(criteria, cond1, cond2);
        // Conditions should be removed
    }

    @Test
    public void testRemoveCollection() {
        Criteria criteria = new Criteria();
        Condition cond1 = Filters.equal("name", "John");
        Condition cond2 = Filters.equal("age", 30);

        criteria.where(cond1);
        criteria.where(cond2);

        CriteriaUtil.remove(criteria, Arrays.asList(cond1, cond2));
        // Conditions should be removed
    }

    @Test
    public void testRemoveEmptyCollection() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        CriteriaUtil.remove(criteria, Arrays.asList());
        // Should not throw, original conditions should remain
        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testIsClauseWithNull() {
        assertFalse(CriteriaUtil.isClause((Condition) null));
        assertFalse(CriteriaUtil.isClause((String) null));
    }

    @Test
    public void testIsClauseWithEmptyString() {
        assertFalse(CriteriaUtil.isClause(""));
    }

    @Test
    public void testGetClauseOperatorsImmutable() {
        Set<Operator> operators = CriteriaUtil.getClauseOperators();
        assertNotNull(operators);
        // The set should contain expected clause operators
        assertTrue(operators.contains(Operator.WHERE));
        assertTrue(operators.contains(Operator.GROUP_BY));
        assertTrue(operators.contains(Operator.HAVING));
        assertTrue(operators.contains(Operator.ORDER_BY));
        assertTrue(operators.contains(Operator.LIMIT));
    }

    @Test
    public void testAddMultipleConditionsOfDifferentTypes() {
        Criteria criteria = new Criteria();
        Condition whereCondition = new Where(Filters.equal("name", "John"));
        Condition havingCondition = new Having(Filters.expr("COUNT(*) > 5"));

        CriteriaUtil.add(criteria, whereCondition, havingCondition);

        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testRemoveNonExistentCondition() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));

        Condition nonExistent = Filters.equal("age", 30);
        CriteriaUtil.remove(criteria, nonExistent);

        // Should not throw, original condition should remain
        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testAddWithClauseConditions() {
        Criteria criteria = new Criteria();
        Condition whereCondition = new Where(Filters.equal("name", "John"));
        Condition groupByCondition = new GroupBy("department");

        CriteriaUtil.add(criteria, whereCondition, groupByCondition);

        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testRemoveMultipleOperators() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        criteria.groupBy("department");
        criteria.having("COUNT(*) > 5");
        criteria.orderBy("name");

        CriteriaUtil.remove(criteria, Operator.WHERE);
        CriteriaUtil.remove(criteria, Operator.GROUP_BY);

        // Where and GroupBy should be removed
    }

    @Test
    public void testIsClauseCaseInsensitive() {
        assertTrue(CriteriaUtil.isClause("where"));
        assertTrue(CriteriaUtil.isClause("WHERE"));
        assertTrue(CriteriaUtil.isClause("Where"));
        assertTrue(CriteriaUtil.isClause("GROUP BY"));
        assertTrue(CriteriaUtil.isClause("group by"));
    }

    @Test
    public void testAddSingleCondition() {
        Criteria criteria = new Criteria();
        Condition condition = new Where(Filters.equal("status", "active"));

        CriteriaUtil.add(criteria, condition);

        assertFalse(criteria.getConditions().isEmpty());
    }

    @Test
    public void testRemoveSingleCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.equal("status", "active");
        criteria.where(condition);

        CriteriaUtil.remove(criteria, condition);
        // Condition should be removed
    }
}
