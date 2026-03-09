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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.Criteria.CriteriaBuilder;

@Tag("2025")
public class Criteria2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = Criteria.builder().build();
        assertNotNull(criteria);
    }

    @Test
    public void testPreselect() {
        Criteria criteria = Criteria.builder().build();
        String selectModifier = criteria.getSelectModifier();
        // Default should be null or empty
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
        // Initially may be null
    }

    @Test
    public void testGetGroupBy() {
        Criteria criteria = Criteria.builder().build();
        Clause groupBy = criteria.getGroupBy();
        // Initially may be null
    }

    @Test
    public void testGetHaving() {
        Criteria criteria = Criteria.builder().build();
        Clause having = criteria.getHaving();
        // Initially may be null
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
        // Initially may be null
    }

    @Test
    public void testGetLimit() {
        Criteria criteria = Criteria.builder().build();
        Limit limit = criteria.getLimit();
        // Initially may be null
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
    public void testClearParameters() {
        Criteria criteria = Criteria.builder().where(Filters.equal("age", 30)).build();
        criteria.clearParameters();
        // Parameters should be cleared
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
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.join((Join[]) null));
    }

    @Test
    public void testJoinCollectionNull() {
        CriteriaBuilder builder = Criteria.builder();
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
    public void testClearParametersMultiple() {
        Criteria criteria = Criteria.builder().where(Filters.equal("name", "John")).where(Filters.equal("age", 30)).build();
        criteria.clearParameters();
        // Parameters should be cleared
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
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
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.orderBy("name")));
    }

    @Test
    public void testWhereRejectsJoinCondition() {
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(new Join("orders", Filters.on("users.id", "orders.user_id"))));
    }

    @Test
    public void testWhereRejectsOnCondition() {
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.on("users.id", "orders.user_id")));
    }

    @Test
    public void testWhereRejectsNestedCriteriaCondition() {
        CriteriaBuilder builder = Criteria.builder();
        Criteria nested = Criteria.builder().where(Filters.equal("status", "active")).build();
        assertThrows(IllegalArgumentException.class, () -> builder.where(nested));
    }

    @Test
    public void testGroupByRejectsMismatchedClauseCondition() {
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.groupBy(Filters.where(Filters.equal("status", "active"))));
    }

    @Test
    public void testHavingRejectsMismatchedClauseCondition() {
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.having(Filters.groupBy("department")));
    }

    @Test
    public void testOrderByRejectsMismatchedClauseCondition() {
        CriteriaBuilder builder = Criteria.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.orderBy(Filters.where(Filters.equal("status", "active"))));
    }

    @Test
    public void testOrderByRejectsUsingCondition() {
        CriteriaBuilder builder = Criteria.builder();
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
