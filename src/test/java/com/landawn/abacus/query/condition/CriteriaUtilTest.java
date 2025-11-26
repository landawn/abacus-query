package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class CriteriaUtilTest extends TestBase {

    @Test
    public void testGetClauseOperators() {
        Set<Operator> clauseOperators = CriteriaUtil.getClauseOperators();

        Assertions.assertNotNull(clauseOperators);
        Assertions.assertFalse(clauseOperators.isEmpty());

        // Verify expected clause operators
        Assertions.assertTrue(clauseOperators.contains(Operator.JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.LEFT_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.RIGHT_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.FULL_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.CROSS_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.INNER_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.NATURAL_JOIN));
        Assertions.assertTrue(clauseOperators.contains(Operator.WHERE));
        Assertions.assertTrue(clauseOperators.contains(Operator.GROUP_BY));
        Assertions.assertTrue(clauseOperators.contains(Operator.HAVING));
        Assertions.assertTrue(clauseOperators.contains(Operator.ORDER_BY));
        Assertions.assertTrue(clauseOperators.contains(Operator.LIMIT));
        Assertions.assertTrue(clauseOperators.contains(Operator.UNION));
        Assertions.assertTrue(clauseOperators.contains(Operator.UNION_ALL));
        Assertions.assertTrue(clauseOperators.contains(Operator.INTERSECT));
        Assertions.assertTrue(clauseOperators.contains(Operator.EXCEPT));
        Assertions.assertTrue(clauseOperators.contains(Operator.MINUS));

        // Verify non-clause operators are not included
        Assertions.assertFalse(clauseOperators.contains(Operator.EQUAL));
        Assertions.assertFalse(clauseOperators.contains(Operator.NOT_EQUAL));
        Assertions.assertFalse(clauseOperators.contains(Operator.GREATER_THAN));
        Assertions.assertFalse(clauseOperators.contains(Operator.AND));
        Assertions.assertFalse(clauseOperators.contains(Operator.OR));
    }

    @Test
    public void testIsClauseWithOperator() {
        // Test clause operators
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.WHERE));
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.JOIN));
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.GROUP_BY));
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.HAVING));
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.ORDER_BY));
        Assertions.assertTrue(CriteriaUtil.isClause(Operator.LIMIT));

        // Test non-clause operators
        Assertions.assertFalse(CriteriaUtil.isClause(Operator.EQUAL));
        Assertions.assertFalse(CriteriaUtil.isClause(Operator.AND));
        Assertions.assertFalse(CriteriaUtil.isClause(Operator.OR));
        Assertions.assertFalse(CriteriaUtil.isClause(Operator.NOT));

        // Test null
        Assertions.assertFalse(CriteriaUtil.isClause((Operator) null));
    }

    @Test
    public void testIsClauseWithString() {
        // Test clause operator strings
        Assertions.assertTrue(CriteriaUtil.isClause("WHERE"));
        Assertions.assertTrue(CriteriaUtil.isClause("JOIN"));
        Assertions.assertTrue(CriteriaUtil.isClause("LEFT JOIN"));
        Assertions.assertTrue(CriteriaUtil.isClause("GROUP BY"));
        Assertions.assertTrue(CriteriaUtil.isClause("HAVING"));
        Assertions.assertTrue(CriteriaUtil.isClause("ORDER BY"));
        Assertions.assertTrue(CriteriaUtil.isClause("LIMIT"));

        // Test non-clause operator strings
        Assertions.assertFalse(CriteriaUtil.isClause("="));
        Assertions.assertFalse(CriteriaUtil.isClause("AND"));
        Assertions.assertFalse(CriteriaUtil.isClause("OR"));
        Assertions.assertFalse(CriteriaUtil.isClause("NOT"));

        // Test invalid strings
        Assertions.assertFalse(CriteriaUtil.isClause("INVALID"));
        Assertions.assertFalse(CriteriaUtil.isClause(""));
    }

    @Test
    public void testIsClauseWithCondition() {
        // Test clause conditions
        Where where = Filters.where(Filters.eq("id", 1));
        Assertions.assertTrue(CriteriaUtil.isClause(where));

        Join join = Filters.join("orders");
        Assertions.assertTrue(CriteriaUtil.isClause(join));

        GroupBy groupBy = Filters.groupBy("department");
        Assertions.assertTrue(CriteriaUtil.isClause(groupBy));

        Having having = Filters.having(Filters.gt("COUNT(*)", 5));
        Assertions.assertTrue(CriteriaUtil.isClause(having));

        OrderBy orderBy = Filters.orderBy("name");
        Assertions.assertTrue(CriteriaUtil.isClause(orderBy));

        Limit limit = Filters.limit(10);
        Assertions.assertTrue(CriteriaUtil.isClause(limit));

        // Test non-clause conditions
        Equal eq = Filters.eq("status", "active");
        Assertions.assertFalse(CriteriaUtil.isClause(eq));

        And and = Filters.and(eq);
        Assertions.assertFalse(CriteriaUtil.isClause(and));

        // Test null
        Assertions.assertFalse(CriteriaUtil.isClause((Condition) null));
    }

    @Test
    public void testAddWithVarargs() {
        Criteria criteria = Filters.criteria();
        Where where = Filters.where(Filters.eq("active", true));
        OrderBy orderBy = Filters.orderBy("name");

        CriteriaUtil.add(criteria, where, orderBy);

        Assertions.assertEquals(2, criteria.getConditions().size());
        Assertions.assertNotNull(criteria.getWhere());
        Assertions.assertNotNull(criteria.getOrderBy());
    }

    @Test
    public void testAddWithCollection() {
        Criteria criteria = Filters.criteria();
        List<Condition> conditions = Arrays.asList(Filters.join("orders"), Filters.where(Filters.eq("status", "active")), Filters.limit(10));

        CriteriaUtil.add(criteria, conditions);

        Assertions.assertEquals(3, criteria.getConditions().size());
        Assertions.assertEquals(1, criteria.getJoins().size());
        Assertions.assertNotNull(criteria.getWhere());
        Assertions.assertNotNull(criteria.getLimit());
    }

    @Test
    public void testRemoveByOperator() {
        Criteria criteria = Filters.criteria().where(Filters.eq("active", true)).orderBy("name").limit(10);

        CriteriaUtil.remove(criteria, Operator.WHERE);

        Assertions.assertNull(criteria.getWhere());
        Assertions.assertNotNull(criteria.getOrderBy());
        Assertions.assertNotNull(criteria.getLimit());
    }

    @Test
    public void testRemoveWithVarargs() {
        Criteria criteria = Filters.criteria();
        Where where = Filters.where(Filters.eq("id", 1));
        OrderBy orderBy = Filters.orderBy("date");
        Limit limit = Filters.limit(5);

        CriteriaUtil.add(criteria, where, orderBy, limit);
        CriteriaUtil.remove(criteria, where, limit);

        Assertions.assertNull(criteria.getWhere());
        Assertions.assertNotNull(criteria.getOrderBy());
        Assertions.assertNull(criteria.getLimit());
    }

    @Test
    public void testRemoveWithCollection() {
        Criteria criteria = Filters.criteria();
        Join join1 = Filters.join("orders");
        Join join2 = Filters.leftJoin("products");
        Where where = Filters.where(Filters.eq("active", true));

        CriteriaUtil.add(criteria, join1, join2, where);
        CriteriaUtil.remove(criteria, Arrays.asList(join1, where));

        Assertions.assertEquals(1, criteria.getJoins().size());
        Assertions.assertEquals(join2, criteria.getJoins().get(0));
        Assertions.assertNull(criteria.getWhere());
    }

    @Test
    public void testClauseOperatorsOrder() {
        // The order should be maintained as per implementation
        Set<Operator> operators = CriteriaUtil.getClauseOperators();
        List<Operator> operatorList = new ArrayList<>(operators);

        // Verify JOIN operators come before WHERE
        int joinIndex = operatorList.indexOf(Operator.JOIN);
        int whereIndex = operatorList.indexOf(Operator.WHERE);
        Assertions.assertTrue(joinIndex < whereIndex);

        // Verify WHERE comes before GROUP BY
        int groupByIndex = operatorList.indexOf(Operator.GROUP_BY);
        Assertions.assertTrue(whereIndex < groupByIndex);

        // Verify GROUP BY comes before HAVING
        int havingIndex = operatorList.indexOf(Operator.HAVING);
        Assertions.assertTrue(groupByIndex < havingIndex);

        // Verify HAVING comes before ORDER BY
        int orderByIndex = operatorList.indexOf(Operator.ORDER_BY);
        Assertions.assertTrue(havingIndex < orderByIndex);

        // Verify ORDER BY comes before LIMIT
        int limitIndex = operatorList.indexOf(Operator.LIMIT);
        Assertions.assertTrue(orderByIndex < limitIndex);
    }

    @Test
    public void testImmutabilityOfClauseOperators() {
        Set<Operator> operators = CriteriaUtil.getClauseOperators();

        // The returned set should be immutable
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            operators.add(Operator.EQUAL);
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            operators.remove(Operator.WHERE);
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            operators.clear();
        });
    }
}