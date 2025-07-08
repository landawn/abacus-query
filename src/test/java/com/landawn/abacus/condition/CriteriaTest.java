package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SortDirection;

public class CriteriaTest extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = ConditionFactory.criteria();
        
        Assertions.assertNotNull(criteria);
        Assertions.assertEquals(Operator.EMPTY, criteria.getOperator());
        Assertions.assertNotNull(criteria.getConditions());
        Assertions.assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    public void testPreselect() {
        Criteria criteria = ConditionFactory.criteria();
        Assertions.assertNull(criteria.preselect());
    }

    @Test
    public void testDistinct() {
        Criteria criteria = ConditionFactory.criteria().distinct();
        Assertions.assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testDistinctBy() {
        Criteria criteria = ConditionFactory.criteria().distinctBy("department, location");
        Assertions.assertEquals("DISTINCT(department, location)", criteria.preselect());
    }

    @Test
    public void testDistinctByEmpty() {
        Criteria criteria = ConditionFactory.criteria().distinctBy("");
        Assertions.assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testDistinctRow() {
        Criteria criteria = ConditionFactory.criteria().distinctRow();
        Assertions.assertEquals("DISTINCTROW", criteria.preselect());
    }

    @Test
    public void testDistinctRowBy() {
        Criteria criteria = ConditionFactory.criteria().distinctRowBy("id, name");
        Assertions.assertEquals("DISTINCTROW(id, name)", criteria.preselect());
    }

    @Test
    public void testPreselectCustom() {
        Criteria criteria = ConditionFactory.criteria().preselect("CUSTOM");
        Assertions.assertEquals("CUSTOM", criteria.preselect());
    }

    @Test
    public void testJoinSingle() {
        Criteria criteria = ConditionFactory.criteria().join("orders");
        
        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(Operator.JOIN, joins.get(0).getOperator());
    }

    @Test
    public void testJoinWithCondition() {
        Equal eq = ConditionFactory.eq("users.id", "orders.user_id");
        Criteria criteria = ConditionFactory.criteria().join("orders", eq);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(eq, joins.get(0).getCondition());
    }

    @Test
    public void testJoinMultiple() {
        List<String> tables = Arrays.asList("orders", "order_items");
        Equal eq = ConditionFactory.eq("orders.id", "order_items.order_id");
        Criteria criteria = ConditionFactory.criteria().join(tables, eq);
        
        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(2, joins.get(0).getJoinEntities().size());
    }

    @Test
    public void testJoinArray() {
        Join join1 = ConditionFactory.join("orders");
        Join join2 = ConditionFactory.leftJoin("products");
        Criteria criteria = ConditionFactory.criteria().join(join1, join2);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testJoinCollection() {
        List<Join> joinList = Arrays.asList(ConditionFactory.join("orders"), ConditionFactory.rightJoin("customers"));
        Criteria criteria = ConditionFactory.criteria().join(joinList);
        
        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testWhere() {
        Equal eq = ConditionFactory.eq("status", "active");
        Criteria criteria = ConditionFactory.criteria().where(eq);
        
        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(eq, where.getCondition());
    }

    @Test
    public void testWhereString() {
        Criteria criteria = ConditionFactory.criteria().where("age > 18");
        
        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertTrue(where.getCondition() instanceof Expression);
    }

    @Test
    public void testWhereWithWhereCondition() {
        Where whereCondition = ConditionFactory.where(ConditionFactory.eq("id", 1));
        Criteria criteria = ConditionFactory.criteria().where(whereCondition);
        
        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(whereCondition, where);
    }

    @Test
    public void testGroupBy() {
        Criteria criteria = ConditionFactory.criteria().groupBy("department", "location");
        
        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(Operator.GROUP_BY, groupBy.getOperator());
    }

    @Test
    public void testGroupByWithDirection() {
        Criteria criteria = ConditionFactory.criteria().groupBy("salary", SortDirection.DESC);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCollection() {
        List<String> props = Arrays.asList("dept", "team");
        Criteria criteria = ConditionFactory.criteria().groupBy(props);
        
        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);
        
        Criteria criteria = ConditionFactory.criteria().groupBy(orders);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCondition() {
        Expression expr = ConditionFactory.expr("YEAR(date)");
        Criteria criteria = ConditionFactory.criteria().groupBy(expr);
        
        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(expr, groupBy.getCondition());
    }

    @Test
    public void testHaving() {
        GreaterThan gt = ConditionFactory.gt("COUNT(*)", 5);
        Criteria criteria = ConditionFactory.criteria().having(gt);
        
        Cell having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.getOperator());
        Assertions.assertEquals(gt, having.getCondition());
    }

    @Test
    public void testHavingString() {
        Criteria criteria = ConditionFactory.criteria().having("SUM(amount) > 1000");
        
        Cell having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertTrue(having.getCondition() instanceof Expression);
    }

    @Test
    public void testOrderByAsc() {
        Criteria criteria = ConditionFactory.criteria().orderByAsc("lastName", "firstName");
        
        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
        Assertions.assertEquals(Operator.ORDER_BY, orderBy.getOperator());
    }

    @Test
    public void testOrderByDesc() {
        Criteria criteria = ConditionFactory.criteria().orderByDesc("createdDate", "id");

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderBy() {
        Criteria criteria = ConditionFactory.criteria().orderBy("name", SortDirection.ASC);
        
        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMultiple() {
        Criteria criteria = ConditionFactory.criteria().orderBy("dept", SortDirection.ASC, "salary", SortDirection.DESC);
        
        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);

        Criteria criteria = ConditionFactory.criteria().orderBy(orders);
        
        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testLimit() {
        Criteria criteria = ConditionFactory.criteria().limit(10);
        
        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitWithOffset() {
        Criteria criteria = ConditionFactory.criteria().limit(20, 10);
        
        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(20, limit.getOffset());
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitString() {
        Criteria criteria = ConditionFactory.criteria().limit("10 OFFSET 20");
        
        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
    }

    @Test
    public void testUnion() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT * FROM archived_orders");
        Criteria criteria = ConditionFactory.criteria().union(subQuery);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION, aggregations.get(0).getOperator());
    }

    @Test
    public void testUnionAll() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT * FROM temp_orders");
        Criteria criteria = ConditionFactory.criteria().unionAll(subQuery);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION_ALL, aggregations.get(0).getOperator());
    }

    @Test
    public void testIntersect() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM active_users");
        Criteria criteria = ConditionFactory.criteria().intersect(subQuery);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.INTERSECT, aggregations.get(0).getOperator());
    }

    @Test
    public void testExcept() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM blocked_users");
        Criteria criteria = ConditionFactory.criteria().except(subQuery);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).getOperator());
    }

    @Test
    public void testMinus() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM inactive_users");
        Criteria criteria = ConditionFactory.criteria().minus(subQuery);
        
        List<Cell> aggregations = criteria.getAggregation();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.MINUS, aggregations.get(0).getOperator());
    }

    @Test
    public void testGetParameters() {
        Criteria criteria = ConditionFactory.criteria()
                .join("orders", ConditionFactory.eq("users.id", "orders.user_id"))
            .where(ConditionFactory.and(
                ConditionFactory.eq("status", "active"),
                ConditionFactory.gt("amount", 100)
            ))
            .having(ConditionFactory.gt("COUNT(*)", 5));
        
        List<Object> params = criteria.getParameters();

        // Should contain parameters from join, where, and having
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
        Assertions.assertTrue(params.contains(5));
    }

    @Test
    public void testClearParameters() {
        Criteria criteria = ConditionFactory.criteria()
                .where(ConditionFactory.in("id", Arrays.asList(1, 2, 3)))
                .having(ConditionFactory.between("count", 10, 100));
        
        criteria.clearParameters();
        
        List<Object> params = criteria.getParameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClear() {
        Criteria criteria = ConditionFactory.criteria()
                .join("orders")
                .where(ConditionFactory.eq("active", true))
            .groupBy("department")
                .having(ConditionFactory.gt("COUNT(*)", 5))
                .orderBy("name")
                .limit(10);
        
        criteria.clear();
        
        Assertions.assertTrue(criteria.getConditions().isEmpty());
        Assertions.assertNull(criteria.getWhere());
        Assertions.assertNull(criteria.getGroupBy());
        Assertions.assertNull(criteria.getHaving());
        Assertions.assertNull(criteria.getOrderBy());
        Assertions.assertNull(criteria.getLimit());
        Assertions.assertTrue(criteria.getJoins().isEmpty());
    }

    @Test
    public void testGet() {
        Criteria criteria = ConditionFactory.criteria().join("orders").join("products").where(ConditionFactory.eq("status", "active"));

        List<Condition> joins = criteria.get(Operator.JOIN);
        Assertions.assertEquals(2, joins.size());

        List<Condition> wheres = criteria.get(Operator.WHERE);
        Assertions.assertEquals(1, wheres.size());
    }

    @Test
    public void testCopy() {
        Criteria original = ConditionFactory.criteria()
            .distinct()
                .join("orders", ConditionFactory.eq("users.id", "orders.user_id"))
                .where(ConditionFactory.eq("active", true))
            .groupBy("department")
            .having(ConditionFactory.gt("COUNT(*)", 5))
                .orderBy("name")
            .limit(10);
        
        Criteria copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.preselect(), copy.preselect());
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());
        
        // Verify deep copy
        Assertions.assertNotNull(copy.getWhere());
        Assertions.assertNotSame(original.getWhere(), copy.getWhere());
    }

    @Test
    public void testToString() {
        Criteria criteria = ConditionFactory.criteria()
                .distinct()
                .join("orders", ConditionFactory.eq("users.id", "orders.user_id"))
            .where(ConditionFactory.eq("status", "active"))
            .groupBy("department")
            .having(ConditionFactory.gt("COUNT(*)", 5))
                .orderBy("department")
            .limit(10);
        
        String result = criteria.toString();
        
        // Verify order of clauses
        Assertions.assertTrue(result.indexOf("DISTINCT") < result.indexOf("JOIN"));
        Assertions.assertTrue(result.indexOf("JOIN") < result.indexOf("WHERE"));
        Assertions.assertTrue(result.indexOf("WHERE") < result.indexOf("GROUP BY"));
        Assertions.assertTrue(result.indexOf("GROUP BY") < result.indexOf("HAVING"));
        Assertions.assertTrue(result.indexOf("HAVING") < result.indexOf("ORDER BY"));
        Assertions.assertTrue(result.indexOf("ORDER BY") < result.indexOf("LIMIT"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Criteria criteria = ConditionFactory.criteria()
            .where(ConditionFactory.eq("firstName", "John"))
            .orderBy("lastName");
        
        String result = criteria.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        
        Assertions.assertTrue(result.contains("first_name = 'John'"));
        Assertions.assertTrue(result.contains("last_name"));
    }

    @Test
    public void testEquals() {
        Criteria criteria1 = ConditionFactory.criteria()
            .distinct()
                .where(ConditionFactory.eq("id", 1));
        
        Criteria criteria2 = ConditionFactory.criteria().distinct().where(ConditionFactory.eq("id", 1));
        
        Criteria criteria3 = ConditionFactory.criteria().where(ConditionFactory.eq("id", 1));
        
        Criteria criteria4 = ConditionFactory.criteria().distinct().where(ConditionFactory.eq("id", 2));
        
        Assertions.assertEquals(criteria1, criteria1);
        Assertions.assertEquals(criteria1, criteria2);
        Assertions.assertNotEquals(criteria1, criteria3); // Different preselect
        Assertions.assertNotEquals(criteria1, criteria4); // Different condition
        Assertions.assertNotEquals(criteria1, null);
        Assertions.assertNotEquals(criteria1, "string");
    }

    @Test
    public void testHashCode() {
        Criteria criteria1 = ConditionFactory.criteria().distinctBy("name").where(ConditionFactory.eq("active", true)).limit(10);

        Criteria criteria2 = ConditionFactory.criteria().distinctBy("name").where(ConditionFactory.eq("active", true)).limit(10);
        
        Assertions.assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }

    @Test
    public void testComplexCriteria() {
        Criteria criteria = ConditionFactory.criteria()
            .distinct()
                .join("orders", ConditionFactory.eq("users.id", "orders.user_id"))
                .join("payments", ConditionFactory.eq("orders.id", "payments.order_id"))
            .where(ConditionFactory.and(
                ConditionFactory.eq("users.status", "active"),
                ConditionFactory.gt("orders.amount", 100),
                ConditionFactory.between("orders.date", "2023-01-01", "2023-12-31")
            ))
                .groupBy("users.id", "users.name")
            .having(ConditionFactory.and(
                ConditionFactory.gt("COUNT(*)", 5),
                        ConditionFactory.lt("SUM(orders.amount)", 10000)
            ))
                .orderBy("total_amount", SortDirection.DESC, "user_name", SortDirection.ASC)
                .limit(20, 100);
        
        // Verify all components are present
        Assertions.assertEquals("DISTINCT", criteria.preselect());
        Assertions.assertEquals(2, criteria.getJoins().size());
        Assertions.assertNotNull(criteria.getWhere());
        Assertions.assertNotNull(criteria.getGroupBy());
        Assertions.assertNotNull(criteria.getHaving());
        Assertions.assertNotNull(criteria.getOrderBy());
        Assertions.assertNotNull(criteria.getLimit());
        
        // Verify parameters
        List<Object> params = criteria.getParameters();
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
        Assertions.assertTrue(params.contains("2023-01-01"));
        Assertions.assertTrue(params.contains("2023-12-31"));
        Assertions.assertTrue(params.contains(5));
        Assertions.assertTrue(params.contains(10000));
    }

    @Test
    public void testReplaceClause() {
        Criteria criteria = ConditionFactory.criteria().where(ConditionFactory.eq("id", 1)).where(ConditionFactory.eq("id", 2)); // Should replace the first where
        
        Cell where = criteria.getWhere();
        Equal eq = where.getCondition();
        Assertions.assertEquals(2, (Integer) eq.getPropValue());
    }

    @Test
    public void testMultipleGroupBy() {
        Criteria criteria = ConditionFactory.criteria().groupBy("dept", SortDirection.ASC, "team", SortDirection.DESC, "member", SortDirection.ASC);
        
        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testMultipleOrderBy() {
        Criteria criteria = ConditionFactory.criteria().orderBy("priority", SortDirection.DESC, "date", SortDirection.ASC, "id", SortDirection.ASC);
        
        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testAnd() {
        Criteria criteria = ConditionFactory.criteria().where(ConditionFactory.eq("id", 1));
        
        And and = criteria.and(ConditionFactory.eq("status", "active"));
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        Criteria criteria = ConditionFactory.criteria().where(ConditionFactory.eq("type", "A"));
        
        Or or = criteria.or(ConditionFactory.eq("type", "B"));
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Criteria criteria = ConditionFactory.criteria().where(ConditionFactory.eq("active", true));

        Not not = criteria.not();
        Assertions.assertNotNull(not);
        Assertions.assertEquals(criteria, not.getCondition());
    }
}