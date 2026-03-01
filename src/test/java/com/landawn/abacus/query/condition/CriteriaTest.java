package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SQLBuilder.NSC;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;

public class CriteriaTest extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = Filters.criteria();

        Assertions.assertNotNull(criteria);
        Assertions.assertEquals(Operator.EMPTY, criteria.getOperator());
        Assertions.assertNotNull(criteria.getConditions());
        Assertions.assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    public void testPreselect() {
        Criteria criteria = Filters.criteria();
        Assertions.assertNull(criteria.getSelectModifier());
    }

    @Test
    public void testDistinct() {
        Criteria criteria = Filters.criteria().distinct();
        Assertions.assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctBy() {
        Criteria criteria = Filters.criteria().distinctBy("department, location");
        Assertions.assertEquals("DISTINCT(department, location)", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctByEmpty() {
        Criteria criteria = Filters.criteria().distinctBy("");
        Assertions.assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRow() {
        Criteria criteria = Filters.criteria().distinctRow();
        Assertions.assertEquals("DISTINCTROW", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRowBy() {
        Criteria criteria = Filters.criteria().distinctRowBy("id, name");
        Assertions.assertEquals("DISTINCTROW(id, name)", criteria.getSelectModifier());
    }

    @Test
    public void testPreselectCustom() {
        Criteria criteria = Filters.criteria().selectModifier("CUSTOM");
        Assertions.assertEquals("CUSTOM", criteria.getSelectModifier());
    }

    @Test
    public void testJoinSingle() {
        Criteria criteria = Filters.criteria().join("orders");

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(Operator.JOIN, joins.get(0).getOperator());
    }

    @Test
    public void testJoinWithCondition() {
        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria = Filters.criteria().join("orders", eq);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(eq, joins.get(0).getCondition());
    }

    @Test
    public void testJoinMultiple() {
        List<String> tables = Arrays.asList("orders", "order_items");
        Equal eq = Filters.eq("orders.id", "order_items.order_id");
        Criteria criteria = Filters.criteria().join(tables, eq);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(2, joins.get(0).getJoinEntities().size());
    }

    @Test
    public void testJoinArray() {
        Join join1 = Filters.join("orders");
        Join join2 = Filters.leftJoin("products");
        Criteria criteria = Filters.criteria().join(join1, join2);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testJoinCollection() {
        List<Join> joinList = Arrays.asList(Filters.join("orders"), Filters.rightJoin("customers"));
        Criteria criteria = Filters.criteria().join(joinList);

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testWhere() {
        Equal eq = Filters.eq("status", "active");
        Criteria criteria = Filters.criteria().where(eq);

        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(eq, where.getCondition());
    }

    @Test
    public void testWhereString() {
        Criteria criteria = Filters.criteria().where("age > 18");

        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertTrue(where.getCondition() instanceof Expression);
    }

    @Test
    public void testWhereStringWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().where((String) null));
    }

    @Test
    public void testWhereWithWhereCondition() {
        Where whereCondition = Filters.where(Filters.eq("id", 1));
        Criteria criteria = Filters.criteria().where(whereCondition);

        Cell where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(whereCondition, where);
    }

    @Test
    public void testWhereWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().where((Condition) null));
    }

    @Test
    public void testGroupBy() {
        Criteria criteria = Filters.criteria().groupBy("department", "location");

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(Operator.GROUP_BY, groupBy.getOperator());
    }

    @Test
    public void testGroupByWithDirection() {
        Criteria criteria = Filters.criteria().groupBy("salary", SortDirection.DESC);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCollection() {
        List<String> props = Arrays.asList("dept", "team");
        Criteria criteria = Filters.criteria().groupBy(props);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);

        Criteria criteria = Filters.criteria().groupBy(orders);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCondition() {
        Expression expr = Filters.expr("YEAR(date)");
        Criteria criteria = Filters.criteria().groupBy(expr);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(expr, groupBy.getCondition());
    }

    @Test
    public void testGroupByWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().groupBy((Condition) null));
    }

    @Test
    public void testHaving() {
        GreaterThan gt = Filters.gt("COUNT(*)", 5);
        Criteria criteria = Filters.criteria().having(gt);

        Cell having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.getOperator());
        Assertions.assertEquals(gt, having.getCondition());
    }

    @Test
    public void testHavingString() {
        Criteria criteria = Filters.criteria().having("SUM(amount) > 1000");

        Cell having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertTrue(having.getCondition() instanceof Expression);
    }

    @Test
    public void testHavingStringWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().having((String) null));
    }

    @Test
    public void testHavingWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().having((Condition) null));
    }

    @Test
    public void testOrderByAsc() {
        Criteria criteria = Filters.criteria().orderByAsc("lastName", "firstName");

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
        Assertions.assertEquals(Operator.ORDER_BY, orderBy.getOperator());
    }

    @Test
    public void testOrderByDesc() {
        Criteria criteria = Filters.criteria().orderByDesc("createdDate", "id");

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderBy() {
        Criteria criteria = Filters.criteria().orderBy("name", SortDirection.ASC);

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMultiple() {
        Criteria criteria = Filters.criteria().orderBy("dept", SortDirection.ASC, "salary", SortDirection.DESC);

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);

        Criteria criteria = Filters.criteria().orderBy(orders);

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.criteria().orderBy((Condition) null));
    }

    @Test
    public void testLimit() {
        Criteria criteria = Filters.criteria().limit(10);

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitWithOffset() {
        Criteria criteria = Filters.criteria().limit(10, 20);

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(20, limit.getOffset());
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitString() {
        Criteria criteria = Filters.criteria().limit("10 OFFSET 20");

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
    }

    @Test
    public void testUnion() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_orders");
        Criteria criteria = Filters.criteria().union(subQuery);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION, aggregations.get(0).getOperator());
    }

    @Test
    public void testUnionAll() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM temp_orders");
        Criteria criteria = Filters.criteria().unionAll(subQuery);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION_ALL, aggregations.get(0).getOperator());
    }

    @Test
    public void testIntersect() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM active_users");
        Criteria criteria = Filters.criteria().intersect(subQuery);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.INTERSECT, aggregations.get(0).getOperator());
    }

    @Test
    public void testExcept() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM blocked_users");
        Criteria criteria = Filters.criteria().except(subQuery);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).getOperator());
    }

    @Test
    public void testMinus() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        Criteria criteria = Filters.criteria().minus(subQuery);

        List<Cell> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.MINUS, aggregations.get(0).getOperator());
    }

    @Test
    public void testGetParameters() {
        Criteria criteria = Filters.criteria()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .where(Filters.and(Filters.eq("status", "active"), Filters.gt("amount", 100)))
                .having(Filters.gt("COUNT(*)", 5));

        List<Object> params = criteria.getParameters();

        // Should contain parameters from join, where, and having
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
        Assertions.assertTrue(params.contains(5));
    }

    @Test
    public void testClearParameters() {
        Criteria criteria = Filters.criteria().where(Filters.in("id", Arrays.asList(1, 2, 3))).having(Filters.between("count", 10, 100));

        criteria.clearParameters();

        List<Object> params = criteria.getParameters();
        Assertions.assertNull(params.get(0)); // In condition should clear parameters));
    }

    @Test
    public void testClear() {
        Criteria criteria = Filters.criteria()
                .distinct()
                .join("orders")
                .where(Filters.eq("active", true))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
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
        Assertions.assertNull(criteria.getSelectModifier());
    }

    @Test
    public void testGet() {
        Criteria criteria = Filters.criteria().join("orders").join("products").where(Filters.eq("status", "active"));

        List<Condition> joins = criteria.get(Operator.JOIN);
        Assertions.assertEquals(2, joins.size());

        List<Condition> wheres = criteria.get(Operator.WHERE);
        Assertions.assertEquals(1, wheres.size());
    }

    @Test
    public void testCopy() {
        Criteria original = Filters.criteria()
                .distinct()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .where(Filters.eq("active", true))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("name")
                .limit(10);

        Criteria copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getSelectModifier(), copy.getSelectModifier());
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());

        // Verify deep copy
        Assertions.assertNotNull(copy.getWhere());
        Assertions.assertNotSame(original.getWhere(), copy.getWhere());
    }

    @Test
    public void testToString() {
        Criteria criteria = Filters.criteria()
                .distinct()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .where(Filters.eq("status", "active"))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
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
        Criteria criteria = Filters.criteria().where(Filters.eq("firstName", "John")).orderBy("lastName");

        String result = criteria.toString(NamingPolicy.SNAKE_CASE);

        Assertions.assertTrue(result.contains("first_name = 'John'"));
        Assertions.assertTrue(result.contains("last_name"));

        String sql = NSC.selectFrom(Account.class).where(Filters.eq("firstName", "John")).orderBy("lastName").sql();
        Assertions.assertTrue(sql.contains("first_name"));
        Assertions.assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testEquals() {
        Criteria criteria1 = Filters.criteria().distinct().where(Filters.eq("id", 1));

        Criteria criteria2 = Filters.criteria().distinct().where(Filters.eq("id", 1));

        Criteria criteria3 = Filters.criteria().where(Filters.eq("id", 1));

        Criteria criteria4 = Filters.criteria().distinct().where(Filters.eq("id", 2));

        Assertions.assertEquals(criteria1, criteria1);
        Assertions.assertEquals(criteria1, criteria2);
        Assertions.assertNotEquals(criteria1, criteria3); // Different selectModifier
        Assertions.assertNotEquals(criteria1, criteria4); // Different condition
        Assertions.assertNotEquals(criteria1, null);
        Assertions.assertNotEquals(criteria1, "string");
    }

    @Test
    public void testHashCode() {
        Criteria criteria1 = Filters.criteria().distinctBy("name").where(Filters.eq("active", true)).limit(10);

        Criteria criteria2 = Filters.criteria().distinctBy("name").where(Filters.eq("active", true)).limit(10);

        Assertions.assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }

    @Test
    public void testComplexCriteria() {
        Criteria criteria = Filters.criteria()
                .distinct()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .join("payments", Filters.eq("orders.id", "payments.order_id"))
                .where(Filters.and(Filters.eq("users.status", "active"), Filters.gt("orders.amount", 100),
                        Filters.between("orders.date", "2023-01-01", "2023-12-31")))
                .groupBy("users.id", "users.name")
                .having(Filters.and(Filters.gt("COUNT(*)", 5), Filters.lt("SUM(orders.amount)", 10000)))
                .orderBy("total_amount", SortDirection.DESC, "user_name", SortDirection.ASC)
                .limit(100, 20);

        // Verify all components are present
        Assertions.assertEquals("DISTINCT", criteria.getSelectModifier());
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
        Criteria criteria = Filters.criteria().where(Filters.eq("id", 1)).where(Filters.eq("id", 2)); // Should replace the first where

        Cell where = criteria.getWhere();
        Equal eq = where.getCondition();
        Assertions.assertEquals(2, (Integer) eq.getPropValue());
    }

    @Test
    public void testMultipleGroupBy() {
        Criteria criteria = Filters.criteria().groupBy("dept", SortDirection.ASC, "team", SortDirection.DESC, "member", SortDirection.ASC);

        Cell groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testMultipleOrderBy() {
        Criteria criteria = Filters.criteria().orderBy("priority", SortDirection.DESC, "date", SortDirection.ASC, "id", SortDirection.ASC);

        Cell orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testAnd() {
        Criteria criteria = Filters.criteria().where(Filters.eq("id", 1));

        And and = criteria.and(Filters.eq("status", "active"));
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        Criteria criteria = Filters.criteria().where(Filters.eq("type", "A"));

        Or or = criteria.or(Filters.eq("type", "B"));
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Criteria criteria = Filters.criteria().where(Filters.eq("active", true));

        Not not = criteria.not();
        Assertions.assertNotNull(not);
        Assertions.assertEquals(criteria, not.getCondition());
    }
}
