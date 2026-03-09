package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SqlBuilder.NSC;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;

public class CriteriaTest extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = Criteria.builder().build();

        Assertions.assertNotNull(criteria);
        Assertions.assertEquals(Operator.EMPTY, criteria.operator());
        Assertions.assertNotNull(criteria.getConditions());
        Assertions.assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    public void testPreselect() {
        Criteria criteria = Criteria.builder().build();
        Assertions.assertNull(criteria.getSelectModifier());
    }

    @Test
    public void testDistinct() {
        Criteria criteria = Criteria.builder().distinct().build();
        Assertions.assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctBy() {
        Criteria criteria = Criteria.builder().distinctBy("department, location").build();
        Assertions.assertEquals("DISTINCT(department, location)", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctByEmpty() {
        Criteria criteria = Criteria.builder().distinctBy("").build();
        Assertions.assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRow() {
        Criteria criteria = Criteria.builder().distinctRow().build();
        Assertions.assertEquals("DISTINCTROW", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRowBy() {
        Criteria criteria = Criteria.builder().distinctRowBy("id, name").build();
        Assertions.assertEquals("DISTINCTROW(id, name)", criteria.getSelectModifier());
    }

    @Test
    public void testPreselectCustom() {
        Criteria criteria = Criteria.builder().selectModifier("CUSTOM").build();
        Assertions.assertEquals("CUSTOM", criteria.getSelectModifier());
    }

    @Test
    public void testJoinSingle() {
        Criteria criteria = Criteria.builder().join("orders").build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(Operator.JOIN, joins.get(0).operator());
    }

    @Test
    public void testJoinWithCondition() {
        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria = Criteria.builder().join("orders", eq).build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(eq, joins.get(0).getCondition());
    }

    @Test
    public void testJoinMultiple() {
        List<String> tables = Arrays.asList("orders", "order_items");
        Equal eq = Filters.eq("orders.id", "order_items.order_id");
        Criteria criteria = Criteria.builder().join(tables, eq).build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(1, joins.size());
        Assertions.assertEquals(2, joins.get(0).getJoinEntities().size());
    }

    @Test
    public void testJoinArray() {
        Join join1 = Filters.join("orders");
        Join join2 = Filters.leftJoin("products");
        Criteria criteria = Criteria.builder().join(join1, join2).build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testJoinCollection() {
        List<Join> joinList = Arrays.asList(Filters.join("orders"), Filters.rightJoin("customers"));
        Criteria criteria = Criteria.builder().join(joinList).build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
    }

    @Test
    public void testWhere() {
        Equal eq = Filters.eq("status", "active");
        Criteria criteria = Criteria.builder().where(eq).build();

        Clause where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertEquals(eq, where.getCondition());
    }

    @Test
    public void testWhereString() {
        Criteria criteria = Criteria.builder().where("age > 18").build();

        Clause where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertTrue(where.getCondition() instanceof Expression);
    }

    @Test
    public void testWhereStringWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().where((String) null));
    }

    @Test
    public void testWhereWithWhereCondition() {
        Where whereCondition = Filters.where(Filters.eq("id", 1));
        Criteria criteria = Criteria.builder().where(whereCondition).build();

        Clause where = criteria.getWhere();
        Assertions.assertNotNull(where);
        Assertions.assertEquals(whereCondition, where);
    }

    @Test
    public void testWhereWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().where((Condition) null));
    }

    @Test
    public void testGroupBy() {
        Criteria criteria = Criteria.builder().groupBy("department", "location").build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(Operator.GROUP_BY, groupBy.operator());
    }

    @Test
    public void testGroupByWithDirection() {
        Criteria criteria = Criteria.builder().groupBy("salary", SortDirection.DESC).build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCollection() {
        List<String> props = Arrays.asList("dept", "team");
        Criteria criteria = Criteria.builder().groupBy(props).build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);

        Criteria criteria = Criteria.builder().groupBy(orders).build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCondition() {
        Expression expr = Filters.expr("YEAR(date)");
        Criteria criteria = Criteria.builder().groupBy(expr).build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(expr, groupBy.getCondition());
    }

    @Test
    public void testGroupByWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().groupBy((Condition) null));
    }

    @Test
    public void testHaving() {
        GreaterThan gt = Filters.gt("COUNT(*)", 5);
        Criteria criteria = Criteria.builder().having(gt).build();

        Clause having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertEquals(Operator.HAVING, having.operator());
        Assertions.assertEquals(gt, having.getCondition());
    }

    @Test
    public void testHavingString() {
        Criteria criteria = Criteria.builder().having("SUM(amount) > 1000").build();

        Clause having = criteria.getHaving();
        Assertions.assertNotNull(having);
        Assertions.assertTrue(having.getCondition() instanceof Expression);
    }

    @Test
    public void testHavingStringWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().having((String) null));
    }

    @Test
    public void testHavingWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().having((Condition) null));
    }

    @Test
    public void testOrderByAsc() {
        Criteria criteria = Criteria.builder().orderByAsc("lastName", "firstName").build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
        Assertions.assertEquals(Operator.ORDER_BY, orderBy.operator());
    }

    @Test
    public void testOrderByDesc() {
        Criteria criteria = Criteria.builder().orderByDesc("createdDate", "id").build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderBy() {
        Criteria criteria = Criteria.builder().orderBy("name", SortDirection.ASC).build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMultiple() {
        Criteria criteria = Criteria.builder().orderBy("dept", SortDirection.ASC, "salary", SortDirection.DESC).build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);

        Criteria criteria = Criteria.builder().orderBy(orders).build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    @Test
    public void testOrderByWithNullCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().orderBy((Condition) null));
    }

    @Test
    public void testLimit() {
        Criteria criteria = Criteria.builder().limit(10).build();

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitWithOffset() {
        Criteria criteria = Criteria.builder().limit(10, 20).build();

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
        Assertions.assertEquals(20, limit.getOffset());
        Assertions.assertEquals(10, limit.getCount());
    }

    @Test
    public void testLimitString() {
        Criteria criteria = Criteria.builder().limit("10 OFFSET 20").build();

        Limit limit = criteria.getLimit();
        Assertions.assertNotNull(limit);
    }

    @Test
    public void testUnion() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_orders");
        Criteria criteria = Criteria.builder().union(subQuery).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION, aggregations.get(0).operator());
    }

    @Test
    public void testUnionAll() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM temp_orders");
        Criteria criteria = Criteria.builder().unionAll(subQuery).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.UNION_ALL, aggregations.get(0).operator());
    }

    @Test
    public void testIntersect() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM active_users");
        Criteria criteria = Criteria.builder().intersect(subQuery).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.INTERSECT, aggregations.get(0).operator());
    }

    @Test
    public void testExcept() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM blocked_users");
        Criteria criteria = Criteria.builder().except(subQuery).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.EXCEPT, aggregations.get(0).operator());
    }

    @Test
    public void testMinus() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        Criteria criteria = Criteria.builder().minus(subQuery).build();

        List<Clause> aggregations = criteria.getSetOperations();
        Assertions.assertEquals(1, aggregations.size());
        Assertions.assertEquals(Operator.MINUS, aggregations.get(0).operator());
    }

    @Test
    public void testGetParameters() {
        Criteria criteria = Criteria.builder()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .where(Filters.and(Filters.eq("status", "active"), Filters.gt("amount", 100)))
                .having(Filters.gt("COUNT(*)", 5))
                .build();

        List<Object> params = criteria.getParameters();

        // Should contain parameters from join, where, and having
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
        Assertions.assertTrue(params.contains(5));
    }

    @Test
    public void testClearParameters() {
        Criteria criteria = Criteria.builder().where(Filters.in("id", Arrays.asList(1, 2, 3))).having(Filters.between("count", 10, 100)).build();

        criteria.clearParameters();

        List<Object> params = criteria.getParameters();
        Assertions.assertNull(params.get(0)); // In condition should clear parameters));
    }

    @Test
    public void testGet() {
        Criteria criteria = Criteria.builder().join("orders").join("products").where(Filters.eq("status", "active")).build();

        List<Condition> joins = criteria.findConditions(Operator.JOIN);
        Assertions.assertEquals(2, joins.size());

        List<Condition> wheres = criteria.findConditions(Operator.WHERE);
        Assertions.assertEquals(1, wheres.size());
    }

    @Test
    public void testToString() {
        Criteria criteria = Criteria.builder()
                .distinct()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .where(Filters.eq("status", "active"))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("department")
                .limit(10)
                .build();

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
        Criteria criteria = Criteria.builder().where(Filters.eq("firstName", "John")).orderBy("lastName").build();

        String result = criteria.toString(NamingPolicy.SNAKE_CASE);

        Assertions.assertTrue(result.contains("first_name = 'John'"));
        Assertions.assertTrue(result.contains("last_name"));

        String sql = NSC.selectFrom(Account.class).where(Filters.eq("firstName", "John")).orderBy("lastName").build().sql();
        Assertions.assertTrue(sql.contains("first_name"));
        Assertions.assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testEquals() {
        Criteria criteria1 = Criteria.builder().distinct().where(Filters.eq("id", 1)).build();

        Criteria criteria2 = Criteria.builder().distinct().where(Filters.eq("id", 1)).build();

        Criteria criteria3 = Criteria.builder().where(Filters.eq("id", 1)).build();

        Criteria criteria4 = Criteria.builder().distinct().where(Filters.eq("id", 2)).build();

        Assertions.assertEquals(criteria1, criteria1);
        Assertions.assertEquals(criteria1, criteria2);
        Assertions.assertNotEquals(criteria1, criteria3); // Different selectModifier
        Assertions.assertNotEquals(criteria1, criteria4); // Different condition
        Assertions.assertNotEquals(criteria1, null);
        Assertions.assertNotEquals(criteria1, "string");
    }

    @Test
    public void testHashCode() {
        Criteria criteria1 = Criteria.builder().distinctBy("name").where(Filters.eq("active", true)).limit(10).build();

        Criteria criteria2 = Criteria.builder().distinctBy("name").where(Filters.eq("active", true)).limit(10).build();

        Assertions.assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }

    @Test
    public void testComplexCriteria() {
        Criteria criteria = Criteria.builder()
                .distinct()
                .join("orders", Filters.eq("users.id", "orders.user_id"))
                .join("payments", Filters.eq("orders.id", "payments.order_id"))
                .where(Filters.and(Filters.eq("users.status", "active"), Filters.gt("orders.amount", 100),
                        Filters.between("orders.date", "2023-01-01", "2023-12-31")))
                .groupBy("users.id", "users.name")
                .having(Filters.and(Filters.gt("COUNT(*)", 5), Filters.lt("SUM(orders.amount)", 10000)))
                .orderBy("total_amount", SortDirection.DESC, "user_name", SortDirection.ASC)
                .limit(100, 20)
                .build();

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
        Criteria criteria = Criteria.builder().where(Filters.eq("id", 1)).where(Filters.eq("id", 2)).build(); // Should replace the first where

        Clause where = criteria.getWhere();
        Equal eq = where.getCondition();
        Assertions.assertEquals(2, (Integer) eq.getPropValue());
    }

    @Test
    public void testMultipleGroupBy() {
        Criteria criteria = Criteria.builder().groupBy("dept", SortDirection.ASC, "team", SortDirection.DESC, "member", SortDirection.ASC).build();

        Clause groupBy = criteria.getGroupBy();
        Assertions.assertNotNull(groupBy);
    }

    @Test
    public void testMultipleOrderBy() {
        Criteria criteria = Criteria.builder().orderBy("priority", SortDirection.DESC, "date", SortDirection.ASC, "id", SortDirection.ASC).build();

        Clause orderBy = criteria.getOrderBy();
        Assertions.assertNotNull(orderBy);
    }

    // ---- Convenience join method tests ----

    @Test
    public void testInnerJoin() {
        // entity-only overload
        Criteria criteria1 = Criteria.builder().innerJoin("orders").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.INNER_JOIN, joins1.get(0).operator());

        // entity + condition overload
        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria2 = Criteria.builder().innerJoin("orders", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.INNER_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        // multi-entity overload
        List<String> tables = Arrays.asList("orders", "order_items");
        Criteria criteria3 = Criteria.builder().innerJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.INNER_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testLeftJoin() {
        Criteria criteria1 = Criteria.builder().leftJoin("orders").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.LEFT_JOIN, joins1.get(0).operator());

        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria2 = Criteria.builder().leftJoin("orders", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.LEFT_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        List<String> tables = Arrays.asList("orders", "order_items");
        Criteria criteria3 = Criteria.builder().leftJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.LEFT_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testRightJoin() {
        Criteria criteria1 = Criteria.builder().rightJoin("orders").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.RIGHT_JOIN, joins1.get(0).operator());

        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria2 = Criteria.builder().rightJoin("orders", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.RIGHT_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        List<String> tables = Arrays.asList("orders", "order_items");
        Criteria criteria3 = Criteria.builder().rightJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.RIGHT_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testFullJoin() {
        Criteria criteria1 = Criteria.builder().fullJoin("orders").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.FULL_JOIN, joins1.get(0).operator());

        Equal eq = Filters.eq("users.id", "orders.user_id");
        Criteria criteria2 = Criteria.builder().fullJoin("orders", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.FULL_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        List<String> tables = Arrays.asList("orders", "order_items");
        Criteria criteria3 = Criteria.builder().fullJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.FULL_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testCrossJoin() {
        Criteria criteria1 = Criteria.builder().crossJoin("colors").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.CROSS_JOIN, joins1.get(0).operator());

        Equal eq = Filters.eq("active", true);
        Criteria criteria2 = Criteria.builder().crossJoin("colors", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.CROSS_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        List<String> tables = Arrays.asList("sizes", "colors");
        Criteria criteria3 = Criteria.builder().crossJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.CROSS_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testNaturalJoin() {
        Criteria criteria1 = Criteria.builder().naturalJoin("employees").build();
        List<Join> joins1 = criteria1.getJoins();
        Assertions.assertEquals(1, joins1.size());
        Assertions.assertEquals(Operator.NATURAL_JOIN, joins1.get(0).operator());

        Equal eq = Filters.eq("status", "active");
        Criteria criteria2 = Criteria.builder().naturalJoin("employees", eq).build();
        List<Join> joins2 = criteria2.getJoins();
        Assertions.assertEquals(1, joins2.size());
        Assertions.assertEquals(Operator.NATURAL_JOIN, joins2.get(0).operator());
        Assertions.assertEquals(eq, joins2.get(0).getCondition());

        List<String> tables = Arrays.asList("employees", "departments");
        Criteria criteria3 = Criteria.builder().naturalJoin(tables, eq).build();
        List<Join> joins3 = criteria3.getJoins();
        Assertions.assertEquals(1, joins3.size());
        Assertions.assertEquals(Operator.NATURAL_JOIN, joins3.get(0).operator());
        Assertions.assertEquals(2, joins3.get(0).getJoinEntities().size());
    }

    @Test
    public void testMixedJoinChaining() {
        Criteria criteria = Criteria.builder()
                .innerJoin("orders", Filters.eq("users.id", "orders.user_id"))
                .leftJoin("payments", Filters.eq("orders.id", "payments.order_id"))
                .where(Filters.eq("users.status", "active"))
                .build();

        List<Join> joins = criteria.getJoins();
        Assertions.assertEquals(2, joins.size());
        Assertions.assertEquals(Operator.INNER_JOIN, joins.get(0).operator());
        Assertions.assertEquals(Operator.LEFT_JOIN, joins.get(1).operator());
        Assertions.assertNotNull(criteria.getWhere());
    }
}
