package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.SqlBuilder.NSC;
import com.landawn.abacus.query.condition.Criteria.Builder;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Criteria2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = Criteria.builder().build();
        assertNotNull(criteria);
    }

    @Test
    public void testPreselect() {
        Criteria criteria = Criteria.builder().build();
        String selectModifier = criteria.getSelectModifier();
        assertNull(selectModifier);
    }

    @Test
    public void testPreselectSetter() {
        Criteria criteria = Criteria.builder().selectModifier("DISTINCT").build();
        assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testGetJoins() {
        Criteria criteria = Criteria.builder().build();
        List<Join> joins = criteria.getJoins();
        assertNotNull(joins);
    }

    @Test
    public void testGetWhere() {
        Criteria criteria = Criteria.builder().build();
        Clause where = criteria.getWhere();
        assertNull(where);
    }

    @Test
    public void testGetGroupBy() {
        Criteria criteria = Criteria.builder().build();
        Clause groupBy = criteria.getGroupBy();
        assertNull(groupBy);
    }

    @Test
    public void testGetHaving() {
        Criteria criteria = Criteria.builder().build();
        Clause having = criteria.getHaving();
        assertNull(having);
    }

    @Test
    public void testGetAggregation() {
        Criteria criteria = Criteria.builder().build();
        List<Clause> aggregation = criteria.getSetOperations();
        assertNotNull(aggregation);
    }

    @Test
    public void testGetOrderBy() {
        Criteria criteria = Criteria.builder().build();
        Clause orderBy = criteria.getOrderBy();
        assertNull(orderBy);
    }

    @Test
    public void testGetLimit() {
        Criteria criteria = Criteria.builder().build();
        Limit limit = criteria.getLimit();
        assertNull(limit);
    }

    @Test
    public void testGetConditions() {
        Criteria criteria = Criteria.builder().build();
        List<Condition> conditions = criteria.getConditions();
        assertNotNull(conditions);
    }

    @Test
    public void testGetByOperator() {
        Criteria criteria = Criteria.builder().where(Filters.equal("name", "John")).build();
        List<Condition> whereConditions = criteria.findConditions(Operator.WHERE);
        assertNotNull(whereConditions);
    }

    // testClear removed - Criteria is now immutable

    @Test
    public void testGetParameters() {
        Criteria criteria = Criteria.builder().where(Filters.equal("age", 30)).build();
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testDistinct() {
        Criteria criteria = Criteria.builder().distinct().build();
        assertNotNull(criteria);
        assertNotNull(criteria.getSelectModifier());
    }

    @Test
    public void testDistinctBy() {
        Criteria criteria = Criteria.builder().distinctBy("name, age").build();
        assertNotNull(criteria);
    }

    @Test
    public void testDistinctRow() {
        Criteria criteria = Criteria.builder().distinctRow().build();
        assertNotNull(criteria);
    }

    @Test
    public void testDistinctRowBy() {
        Criteria criteria = Criteria.builder().distinctRowBy("id").build();
        assertNotNull(criteria);
    }

    @Test
    public void testJoinVarargs() {
        Join join = new LeftJoin("orders", Filters.expr("users.id = orders.user_id"));
        Criteria criteria = Criteria.builder().join(join).build();
        assertNotNull(criteria);
        assertEquals(1, criteria.getJoins().size());
    }

    @Test
    public void testJoinCollection() {
        Join join1 = new LeftJoin("orders", Filters.expr("users.id = orders.user_id"));
        Join join2 = new InnerJoin("products", Filters.expr("orders.product_id = products.id"));
        Criteria criteria = Criteria.builder().join(Arrays.asList(join1, join2)).build();
        assertNotNull(criteria);
        assertEquals(2, criteria.getJoins().size());
    }

    @Test
    public void testJoinVarargsNullArray() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.join((Join[]) null));
    }

    @Test
    public void testJoinCollectionNull() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.join((java.util.Collection<Join>) null));
    }

    @Test
    public void testJoinEntity() {
        Criteria criteria = Criteria.builder().join("orders").build();
        assertNotNull(criteria);
    }

    @Test
    public void testJoinEntityWithCondition() {
        Condition condition = Filters.equal("user_id", "id");
        Criteria criteria = Criteria.builder().join("orders", condition).build();
        assertNotNull(criteria);
    }

    @Test
    public void testWhereCondition() {
        Condition condition = Filters.equal("name", "John");
        Criteria criteria = Criteria.builder().where(condition).build();
        assertNotNull(criteria);
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testWhereString() {
        Criteria criteria = Criteria.builder().where("age > 18").build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByCondition() {
        Condition condition = Filters.expr("department");
        Criteria criteria = Criteria.builder().groupBy(condition).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByVarargs() {
        Criteria criteria = Criteria.builder().groupBy("department", "year").build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByWithDirection() {
        Criteria criteria = Criteria.builder().groupBy("department", SortDirection.ASC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByTwoColumns() {
        Criteria criteria = Criteria.builder().groupBy("year", SortDirection.DESC, "month", SortDirection.ASC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByThreeColumns() {
        Criteria criteria = Criteria.builder().groupBy("year", SortDirection.DESC, "month", SortDirection.ASC, "day", SortDirection.DESC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByCollection() {
        Criteria criteria = Criteria.builder().groupBy(Arrays.asList("year", "month", "day")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByCollectionWithDirection() {
        Criteria criteria = Criteria.builder().groupBy(Arrays.asList("year", "month"), SortDirection.DESC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("year", SortDirection.DESC);
        orders.put("month", SortDirection.ASC);
        Criteria criteria = Criteria.builder().groupBy(orders).build();
        assertNotNull(criteria);
    }

    @Test
    public void testHavingCondition() {
        Condition condition = Filters.expr("COUNT(*) > 5");
        Criteria criteria = Criteria.builder().having(condition).build();
        assertNotNull(criteria);
    }

    @Test
    public void testHavingString() {
        Criteria criteria = Criteria.builder().having("COUNT(*) > 5").build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByAscVarargs() {
        Criteria criteria = Criteria.builder().orderByAsc("name", "age").build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByAscCollection() {
        Criteria criteria = Criteria.builder().orderByAsc(Arrays.asList("name", "email")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByDescVarargs() {
        Criteria criteria = Criteria.builder().orderByDesc("created_date", "updated_date").build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByDescCollection() {
        Criteria criteria = Criteria.builder().orderByDesc(Arrays.asList("price", "rating")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByCondition() {
        Condition condition = Filters.expr("name ASC");
        Criteria criteria = Criteria.builder().orderBy(condition).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByVarargs() {
        Criteria criteria = Criteria.builder().orderBy("name", "age", "email").build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByWithDirection() {
        Criteria criteria = Criteria.builder().orderBy("name", SortDirection.ASC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByTwoColumns() {
        Criteria criteria = Criteria.builder().orderBy("name", SortDirection.ASC, "age", SortDirection.DESC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByThreeColumns() {
        Criteria criteria = Criteria.builder().orderBy("name", SortDirection.ASC, "age", SortDirection.DESC, "email", SortDirection.ASC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByCollection() {
        Criteria criteria = Criteria.builder().orderBy(Arrays.asList("name", "age", "email")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByCollectionWithDirection() {
        Criteria criteria = Criteria.builder().orderBy(Arrays.asList("price", "rating"), SortDirection.DESC).build();
        assertNotNull(criteria);
    }

    @Test
    public void testOrderByMap() {
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("name", SortDirection.ASC);
        orders.put("age", SortDirection.DESC);
        Criteria criteria = Criteria.builder().orderBy(orders).build();
        assertNotNull(criteria);
    }

    @Test
    public void testLimitCondition() {
        Limit limit = new Limit(10);
        Criteria criteria = Criteria.builder().limit(limit).build();
        assertNotNull(criteria);
        assertEquals(limit, criteria.getLimit());
    }

    @Test
    public void testLimitInt() {
        Criteria criteria = Criteria.builder().limit(20).build();
        assertNotNull(criteria);
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testLimitWithOffset() {
        Criteria criteria = Criteria.builder().limit(50, 10).build();
        assertNotNull(criteria);
    }

    @Test
    public void testChainedOperations() {
        Criteria criteria = Criteria.builder()
                .where(Filters.equal("status", "active"))
                .groupBy("department")
                .having("COUNT(*) > 5")
                .orderByDesc("created_date")
                .limit(10)
                .build();
        assertNotNull(criteria);
        assertNotNull(criteria.getWhere());
        assertNotNull(criteria.getGroupBy());
        assertNotNull(criteria.getHaving());
        assertNotNull(criteria.getOrderBy());
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testMultipleWhereConditions() {
        Criteria criteria = Criteria.builder().where(Filters.equal("status", "active")).where(Filters.greaterThan("age", 18)).build();
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testMultipleJoins() {
        Criteria criteria = Criteria.builder().join("orders").join("products").join("categories").build();
        assertEquals(3, criteria.getJoins().size());
    }

    @Test
    public void testGetConditionsMultipleTypes() {
        Criteria criteria = Criteria.builder().where(Filters.equal("name", "John")).groupBy("department").having("COUNT(*) > 5").orderBy("name").build();

        List<Condition> conditions = criteria.getConditions();
        assertNotNull(conditions);
        assertTrue(conditions.size() > 0);
    }

    // testClearAll removed - Criteria is now immutable

    @Test
    public void testPreselectChaining() {
        Criteria criteria = Criteria.builder().selectModifier("DISTINCT").where(Filters.equal("status", "active")).build();
        assertNotNull(criteria);
        assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testGetParametersWithMultipleConditions() {
        Criteria criteria = Criteria.builder()
                .where(Filters.equal("name", "John"))
                .where(Filters.equal("age", 30))
                .where(Filters.equal("status", "active"))
                .build();

        List<Object> params = criteria.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testEmptyCriteria() {
        Criteria criteria = Criteria.builder().build();
        assertTrue(criteria.getConditions().isEmpty());
        assertTrue(criteria.getJoins().isEmpty());
    }

    @Test
    public void testJoinCollectionOfEntities() {
        Condition condition = Filters.expr("id = user_id");
        Criteria criteria = Criteria.builder().join(Arrays.asList("orders", "payments"), condition).build();
        assertNotNull(criteria);
        assertEquals(1, criteria.getJoins().size());
    }

    @Test
    public void testLimitString() {
        Criteria criteria = Criteria.builder().limit("10 OFFSET 20").build();
        assertNotNull(criteria);
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testUnion() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        Criteria criteria = Criteria.builder().union(subQuery).build();
        assertNotNull(criteria);
        assertTrue(criteria.getSetOperations().size() > 0);
    }

    @Test
    public void testUnionAll() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        Criteria criteria = Criteria.builder().unionAll(subQuery).build();
        assertNotNull(criteria);
        assertTrue(criteria.getSetOperations().size() > 0);
    }

    @Test
    public void testIntersect() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM premium_users");
        Criteria criteria = Criteria.builder().intersect(subQuery).build();
        assertNotNull(criteria);
        assertTrue(criteria.getSetOperations().size() > 0);
    }

    @Test
    public void testExcept() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM blocked_users");
        Criteria criteria = Criteria.builder().except(subQuery).build();
        assertNotNull(criteria);
        assertTrue(criteria.getSetOperations().size() > 0);
    }

    @Test
    public void testMinus() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM inactive_users");
        Criteria criteria = Criteria.builder().minus(subQuery).build();
        assertNotNull(criteria);
        assertTrue(criteria.getSetOperations().size() > 0);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Criteria criteria = Criteria.builder().distinct().where(Filters.equal("isActive", true)).orderBy("createdDate", SortDirection.DESC).build();

        String sql = criteria.toString(com.landawn.abacus.util.NamingPolicy.SNAKE_CASE);
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testHashCode() {
        Criteria c1 = Criteria.builder().where(Filters.equal("status", "active")).build();
        Criteria c2 = Criteria.builder().where(Filters.equal("status", "active")).build();

        // HashCode should be consistent
        assertEquals(c1.hashCode(), c1.hashCode());
        assertNotNull(c2.hashCode());
    }

    @Test
    public void testEquals() {
        Criteria c1 = Criteria.builder().distinct().where(Filters.equal("status", "active")).build();
        Criteria c2 = Criteria.builder().distinct().where(Filters.equal("status", "active")).build();

        // Test self-equality
        assertEquals(c1, c1);

        // Test equality with same content
        assertEquals(c1, c2);

        // Test inequality
        Criteria c3 = Criteria.builder().where(Filters.equal("status", "inactive")).build();
        assertFalse(c1.equals(c3));

        // Test null
        assertFalse(c1.equals(null));

        // Test different type
        assertFalse(c1.equals("string"));
    }

    @Test
    public void testDistinctByEmpty() {
        Criteria criteria = Criteria.builder().distinctBy("").build();
        assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctByNull() {
        Criteria criteria = Criteria.builder().distinctBy(null).build();
        assertEquals("DISTINCT", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRowByEmpty() {
        Criteria criteria = Criteria.builder().distinctRowBy("").build();
        assertEquals("DISTINCTROW", criteria.getSelectModifier());
    }

    @Test
    public void testDistinctRowByNull() {
        Criteria criteria = Criteria.builder().distinctRowBy(null).build();
        assertEquals("DISTINCTROW", criteria.getSelectModifier());
    }

    @Test
    public void testMultipleAggregations() {
        com.landawn.abacus.query.condition.SubQuery subQuery1 = Filters.subQuery("SELECT * FROM archived");
        com.landawn.abacus.query.condition.SubQuery subQuery2 = Filters.subQuery("SELECT * FROM deleted");

        Criteria criteria = Criteria.builder().union(subQuery1).unionAll(subQuery2).build();

        List<Clause> aggregations = criteria.getSetOperations();
        assertEquals(2, aggregations.size());
    }

    @Test
    public void testGetEmptyAggregation() {
        Criteria criteria = Criteria.builder().build();
        List<Clause> aggregations = criteria.getSetOperations();
        assertNotNull(aggregations);
        assertTrue(aggregations.isEmpty());
    }

    @Test
    public void testGetParametersEmpty() {
        Criteria criteria = Criteria.builder().build();
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParametersIncludesGroupByAndOrderByConditions() {
        Criteria criteria = Criteria.builder().groupBy(Filters.equal("department", "Engineering")).orderBy(Filters.equal("priority", "HIGH")).build();

        List<Object> params = criteria.getParameters();
        assertEquals(Arrays.asList("Engineering", "HIGH"), params);
    }

    @Test
    public void testWhereWithWhereOperator() {
        Where where = new Where(Filters.equal("status", "active"));
        Criteria criteria = Criteria.builder().where(where).build();
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testGroupByWithGroupByOperator() {
        GroupBy groupBy = new GroupBy(Filters.expr("department"));
        Criteria criteria = Criteria.builder().groupBy(groupBy).build();
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    public void testHavingWithHavingOperator() {
        com.landawn.abacus.query.condition.Having having = new com.landawn.abacus.query.condition.Having(Filters.expr("COUNT(*) > 5"));
        Criteria criteria = Criteria.builder().having(having).build();
        assertNotNull(criteria.getHaving());
    }

    @Test
    public void testOrderByWithOrderByOperator() {
        com.landawn.abacus.query.condition.OrderBy orderBy = new com.landawn.abacus.query.condition.OrderBy(Filters.expr("name ASC"));
        Criteria criteria = Criteria.builder().orderBy(orderBy).build();
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    public void testWhereRejectsMismatchedClauseCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.orderBy("name")));
    }

    @Test
    public void testWhereRejectsJoinCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(new Join("orders", Filters.on("users.id", "orders.user_id"))));
    }

    @Test
    public void testWhereRejectsOnCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.on("users.id", "orders.user_id")));
    }

    @Test
    public void testWhereRejectsNestedCriteriaCondition() {
        Builder builder = Criteria.builder();
        Criteria nested = Criteria.builder().where(Filters.equal("status", "active")).build();
        assertThrows(IllegalArgumentException.class, () -> builder.where(nested));
    }

    @Test
    public void testGroupByRejectsMismatchedClauseCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy(Filters.where(Filters.equal("status", "active"))));
    }

    @Test
    public void testHavingRejectsMismatchedClauseCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.having(Filters.groupBy("department")));
    }

    @Test
    public void testOrderByRejectsMismatchedClauseCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy(Filters.where(Filters.equal("status", "active"))));
    }

    @Test
    public void testOrderByRejectsUsingCondition() {
        Builder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy(Filters.using("id")));
    }

    @Test
    public void testGetJoinsMultipleTypes() {
        Criteria criteria = Criteria.builder()
                .join("orders", Filters.expr("id = order_id"))
                .join(new LeftJoin("payments", Filters.expr("id = payment_id")))
                .join(new RightJoin("products", Filters.expr("id = product_id")))
                .build();

        List<Join> joins = criteria.getJoins();
        assertEquals(3, joins.size());
    }

    @Test
    public void testGetByOperatorMultiple() {
        // The builder replaces where when called multiple times
        Criteria criteria = Criteria.builder().where(Filters.equal("status", "active")).where(Filters.equal("type", "premium")).build();

        List<Condition> whereConditions = criteria.findConditions(Operator.WHERE);
        assertEquals(1, whereConditions.size());
    }

    @Test
    public void testComplexCriteria() {
        Criteria criteria = Criteria.builder()
                .distinct()
                .join("orders", Filters.expr("users.id = orders.user_id"))
                .where(Filters.and(Filters.equal("users.status", "active"), Filters.gt("orders.total", 100)))
                .groupBy("users.department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("COUNT(*)", SortDirection.DESC)
                .limit(10)
                .build();

        assertNotNull(criteria.getSelectModifier());
        assertEquals(1, criteria.getJoins().size());
        assertNotNull(criteria.getWhere());
        assertNotNull(criteria.getGroupBy());
        assertNotNull(criteria.getHaving());
        assertNotNull(criteria.getOrderBy());
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testReplaceClauses() {
        // Build with the final (replaced) values
        Criteria criteria = Criteria.builder()
                .where(Filters.equal("status", "active"))
                .groupBy("department")
                .having("COUNT(*) > 5")
                .orderBy("name")
                .limit(10)
                .where(Filters.equal("status", "inactive"))
                .groupBy("location")
                .having("COUNT(*) > 10")
                .orderBy("age")
                .limit(20)
                .build();

        // Only latest values should exist
        assertEquals(5, criteria.getConditions().size());
    }

    @Test
    public void testPreselectNull() {
        Criteria criteria = Criteria.builder().selectModifier(null).build();
        // Should not throw error
        assertNotNull(criteria);
    }

    @Test
    public void testToStringEmptyCriteria() {
        Criteria criteria = Criteria.builder().build();
        String sql = criteria.toString(com.landawn.abacus.util.NamingPolicy.SNAKE_CASE);
        assertNotNull(sql);
    }

    @Test
    public void testGetNullClauses() {
        Criteria criteria = Criteria.builder().build();

        // All clauses should be null initially
        assertNull(criteria.getWhere());
        assertNull(criteria.getGroupBy());
        assertNull(criteria.getHaving());
        assertNull(criteria.getOrderBy());
        assertNull(criteria.getLimit());
        assertNull(criteria.getSelectModifier());
    }
}

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

        String sql = NSC.selectFrom(Account.class).where(Filters.eq("firstName", "John")).orderBy("lastName").build().query();
        Assertions.assertTrue(sql.contains("first_name"));
        Assertions.assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testHashCode() {
        Criteria criteria1 = Criteria.builder().distinctBy("name").where(Filters.eq("active", true)).limit(10).build();

        Criteria criteria2 = Criteria.builder().distinctBy("name").where(Filters.eq("active", true)).limit(10).build();

        Assertions.assertEquals(criteria1.hashCode(), criteria2.hashCode());
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
    public void testGet() {
        Criteria criteria = Criteria.builder().join("orders").join("products").where(Filters.eq("status", "active")).build();

        List<Condition> joins = criteria.findConditions(Operator.JOIN);
        Assertions.assertEquals(2, joins.size());

        List<Condition> wheres = criteria.findConditions(Operator.WHERE);
        Assertions.assertEquals(1, wheres.size());
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

class Criteria2026Test extends TestBase {

    @Test
    public void testToBuilder() {
        final Criteria original = Criteria.builder().selectModifier("DISTINCT").orderBy("name").limit(10).build();

        final Criteria rebuilt = original.toBuilder().where(Filters.eq("status", "ACTIVE")).build();

        assertEquals("DISTINCT", rebuilt.getSelectModifier());
        assertEquals(0, original.getParameters().size());
        assertEquals(1, rebuilt.getParameters().size());
        assertTrue(original.toString(NamingPolicy.NO_CHANGE).contains("ORDER BY"));
        assertFalse(original.toString(NamingPolicy.NO_CHANGE).contains("status"));
        assertTrue(rebuilt.toString(NamingPolicy.NO_CHANGE).contains("ORDER BY"));
        assertTrue(rebuilt.toString(NamingPolicy.NO_CHANGE).contains("status"));
    }
}

class CriteriaFromFiltersTest extends TestBase {

    @Test
    public void testCriteria() {
        Criteria criteria = Criteria.builder().build();
        Assertions.assertNotNull(criteria);
    }
}

class Criteria2026BatchTest extends TestBase {

    private static final class FakeClauseCondition extends AbstractCondition {
        FakeClauseCondition(final Operator operator) {
            super(operator);
        }

        @Override
        public com.landawn.abacus.util.ImmutableList<Object> getParameters() {
            return com.landawn.abacus.util.ImmutableList.empty();
        }

        @Override
        public String toString(final NamingPolicy namingPolicy) {
            return operator().toString();
        }
    }

    private static void invokeCheckCondition(final Criteria.Builder builder, final Condition condition) {
        try {
            java.lang.reflect.Method method = Criteria.Builder.class.getDeclaredMethod("checkCondition", Condition.class);
            method.setAccessible(true);
            method.invoke(builder, condition);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testWhere_RejectsNestedCriteria() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Criteria.builder().where(Criteria.builder().build()));
    }

    @Test
    public void testCheckCondition_RejectsNonClauseOperator() {
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> invokeCheckCondition(Criteria.builder(), Filters.eq("id", 1)));

        Assertions.assertTrue(error.getMessage().contains("Invalid operator"));
    }

    @Test
    public void testCheckCondition_RejectsClauseOperatorWithoutClauseType() {
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> invokeCheckCondition(Criteria.builder(), new FakeClauseCondition(Operator.WHERE)));

        Assertions.assertTrue(error.getMessage().contains("must be an instance of Clause"));
    }

    @Test
    public void testCheckCondition_RejectsJoinOperatorWithoutJoinType() {
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> invokeCheckCondition(Criteria.builder(), new FakeClauseCondition(Operator.JOIN)));

        Assertions.assertTrue(error.getMessage().contains("must be an instance of Join"));
    }
}

class Criteria2026Batch2Test extends TestBase {

    @Test
    public void testEmptyCriteria_CollectionsAndEquality() {
        Criteria left = Criteria.builder().build();
        Criteria right = Criteria.builder().build();
        Criteria distinct = Criteria.builder().selectModifier("DISTINCT").build();

        Assertions.assertTrue(left.getJoins().isEmpty());
        Assertions.assertTrue(left.getSetOperations().isEmpty());
        Assertions.assertTrue(left.findConditions(Operator.WHERE).isEmpty());
        Assertions.assertTrue(left.getParameters().isEmpty());
        Assertions.assertEquals(left, right);
        Assertions.assertEquals(left.hashCode(), right.hashCode());
        Assertions.assertNotEquals(left, distinct);
    }
}
