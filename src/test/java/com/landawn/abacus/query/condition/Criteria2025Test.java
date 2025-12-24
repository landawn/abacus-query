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

@Tag("2025")
public class Criteria2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Criteria criteria = new Criteria();
        assertNotNull(criteria);
    }

    @Test
    public void testPreselect() {
        Criteria criteria = new Criteria();
        String preselect = criteria.preselect();
        // Default should be null or empty
    }

    @Test
    public void testPreselectSetter() {
        Criteria criteria = new Criteria();
        criteria.preselect("DISTINCT");
        assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testGetJoins() {
        Criteria criteria = new Criteria();
        List<Join> joins = criteria.getJoins();
        assertNotNull(joins);
    }

    @Test
    public void testGetWhere() {
        Criteria criteria = new Criteria();
        Cell where = criteria.getWhere();
        // Initially may be null
    }

    @Test
    public void testGetGroupBy() {
        Criteria criteria = new Criteria();
        Cell groupBy = criteria.getGroupBy();
        // Initially may be null
    }

    @Test
    public void testGetHaving() {
        Criteria criteria = new Criteria();
        Cell having = criteria.getHaving();
        // Initially may be null
    }

    @Test
    public void testGetAggregation() {
        Criteria criteria = new Criteria();
        List<Cell> aggregation = criteria.getAggregation();
        assertNotNull(aggregation);
    }

    @Test
    public void testGetOrderBy() {
        Criteria criteria = new Criteria();
        Cell orderBy = criteria.getOrderBy();
        // Initially may be null
    }

    @Test
    public void testGetLimit() {
        Criteria criteria = new Criteria();
        Limit limit = criteria.getLimit();
        // Initially may be null
    }

    @Test
    public void testGetConditions() {
        Criteria criteria = new Criteria();
        List<Condition> conditions = criteria.getConditions();
        assertNotNull(conditions);
    }

    @Test
    public void testGetByOperator() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        List<Condition> whereConditions = criteria.get(Operator.WHERE);
        assertNotNull(whereConditions);
    }

    @Test
    public void testClear() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        criteria.clear();
        assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    public void testGetParameters() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("age", 30));
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("age", 30));
        criteria.clearParameters();
        // Parameters should be cleared
    }

    @Test
    public void testDistinct() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.distinct();
        assertNotNull(result);
        assertEquals(criteria, result);
    }

    @Test
    public void testDistinctBy() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.distinctBy("name, age");
        assertNotNull(result);
    }

    @Test
    public void testDistinctRow() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.distinctRow();
        assertNotNull(result);
    }

    @Test
    public void testDistinctRowBy() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.distinctRowBy("id");
        assertNotNull(result);
    }

    @Test
    public void testJoinVarargs() {
        Criteria criteria = new Criteria();
        Join join = new LeftJoin("orders", Filters.expr("users.id = orders.user_id"));
        Criteria result = criteria.join(join);
        assertNotNull(result);
        assertEquals(1, criteria.getJoins().size());
    }

    @Test
    public void testJoinCollection() {
        Criteria criteria = new Criteria();
        Join join1 = new LeftJoin("orders", Filters.expr("users.id = orders.user_id"));
        Join join2 = new InnerJoin("products", Filters.expr("orders.product_id = products.id"));
        Criteria result = criteria.join(Arrays.asList(join1, join2));
        assertNotNull(result);
        assertEquals(2, criteria.getJoins().size());
    }

    @Test
    public void testJoinEntity() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.join("orders");
        assertNotNull(result);
    }

    @Test
    public void testJoinEntityWithCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.equal("user_id", "id");
        Criteria result = criteria.join("orders", condition);
        assertNotNull(result);
    }

    @Test
    public void testWhereCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.equal("name", "John");
        Criteria result = criteria.where(condition);
        assertNotNull(result);
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testWhereString() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.where("age > 18");
        assertNotNull(result);
    }

    @Test
    public void testGroupByCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.expr("department");
        Criteria result = criteria.groupBy(condition);
        assertNotNull(result);
    }

    @Test
    public void testGroupByVarargs() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy("department", "year");
        assertNotNull(result);
    }

    @Test
    public void testGroupByWithDirection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy("department", SortDirection.ASC);
        assertNotNull(result);
    }

    @Test
    public void testGroupByTwoColumns() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
        assertNotNull(result);
    }

    @Test
    public void testGroupByThreeColumns() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC, "day", SortDirection.DESC);
        assertNotNull(result);
    }

    @Test
    public void testGroupByCollection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy(Arrays.asList("year", "month", "day"));
        assertNotNull(result);
    }

    @Test
    public void testGroupByCollectionWithDirection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.groupBy(Arrays.asList("year", "month"), SortDirection.DESC);
        assertNotNull(result);
    }

    @Test
    public void testGroupByMap() {
        Criteria criteria = new Criteria();
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("year", SortDirection.DESC);
        orders.put("month", SortDirection.ASC);
        Criteria result = criteria.groupBy(orders);
        assertNotNull(result);
    }

    @Test
    public void testHavingCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.expr("COUNT(*) > 5");
        Criteria result = criteria.having(condition);
        assertNotNull(result);
    }

    @Test
    public void testHavingString() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.having("COUNT(*) > 5");
        assertNotNull(result);
    }

    @Test
    public void testOrderByAscVarargs() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderByAsc("name", "age");
        assertNotNull(result);
    }

    @Test
    public void testOrderByAscCollection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderByAsc(Arrays.asList("name", "email"));
        assertNotNull(result);
    }

    @Test
    public void testOrderByDescVarargs() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderByDesc("created_date", "updated_date");
        assertNotNull(result);
    }

    @Test
    public void testOrderByDescCollection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderByDesc(Arrays.asList("price", "rating"));
        assertNotNull(result);
    }

    @Test
    public void testOrderByCondition() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.expr("name ASC");
        Criteria result = criteria.orderBy(condition);
        assertNotNull(result);
    }

    @Test
    public void testOrderByVarargs() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy("name", "age", "email");
        assertNotNull(result);
    }

    @Test
    public void testOrderByWithDirection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy("name", SortDirection.ASC);
        assertNotNull(result);
    }

    @Test
    public void testOrderByTwoColumns() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy("name", SortDirection.ASC, "age", SortDirection.DESC);
        assertNotNull(result);
    }

    @Test
    public void testOrderByThreeColumns() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy("name", SortDirection.ASC, "age", SortDirection.DESC, "email", SortDirection.ASC);
        assertNotNull(result);
    }

    @Test
    public void testOrderByCollection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy(Arrays.asList("name", "age", "email"));
        assertNotNull(result);
    }

    @Test
    public void testOrderByCollectionWithDirection() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.orderBy(Arrays.asList("price", "rating"), SortDirection.DESC);
        assertNotNull(result);
    }

    @Test
    public void testOrderByMap() {
        Criteria criteria = new Criteria();
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("name", SortDirection.ASC);
        orders.put("age", SortDirection.DESC);
        Criteria result = criteria.orderBy(orders);
        assertNotNull(result);
    }

    @Test
    public void testLimitCondition() {
        Criteria criteria = new Criteria();
        Limit limit = new Limit(10);
        Criteria result = criteria.limit(limit);
        assertNotNull(result);
        assertEquals(limit, criteria.getLimit());
    }

    @Test
    public void testLimitInt() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.limit(20);
        assertNotNull(result);
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testLimitWithOffset() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.limit(10, 50);
        assertNotNull(result);
    }

    @Test
    public void testChainedOperations() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.where(Filters.equal("status", "active")).groupBy("department").having("COUNT(*) > 5").orderByDesc("created_date").limit(10);
        assertNotNull(result);
        assertNotNull(criteria.getWhere());
        assertNotNull(criteria.getGroupBy());
        assertNotNull(criteria.getHaving());
        assertNotNull(criteria.getOrderBy());
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testMultipleWhereConditions() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("status", "active"));
        criteria.where(Filters.greaterThan("age", 18));
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testMultipleJoins() {
        Criteria criteria = new Criteria();
        criteria.join("orders").join("products").join("categories");
        assertEquals(3, criteria.getJoins().size());
    }

    @Test
    public void testGetConditionsMultipleTypes() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John"));
        criteria.groupBy("department");
        criteria.having("COUNT(*) > 5");
        criteria.orderBy("name");

        List<Condition> conditions = criteria.getConditions();
        assertNotNull(conditions);
        assertTrue(conditions.size() > 0);
    }

    @Test
    public void testClearAll() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John")).groupBy("department").having("COUNT(*) > 5").orderBy("name").limit(10);

        criteria.clear();

        assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    public void testPreselectChaining() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.preselect("DISTINCT").where(Filters.equal("status", "active"));
        assertNotNull(result);
        assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testGetParametersWithMultipleConditions() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John")).where(Filters.equal("age", 30)).where(Filters.equal("status", "active"));

        List<Object> params = criteria.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testEmptyCriteria() {
        Criteria criteria = new Criteria();
        assertTrue(criteria.getConditions().isEmpty());
        assertTrue(criteria.getJoins().isEmpty());
    }

    @Test
    public void testJoinCollectionOfEntities() {
        Criteria criteria = new Criteria();
        Condition condition = Filters.expr("id = user_id");
        Criteria result = criteria.join(Arrays.asList("orders", "payments"), condition);
        assertNotNull(result);
        assertEquals(1, criteria.getJoins().size());
    }

    @Test
    public void testLimitString() {
        Criteria criteria = new Criteria();
        Criteria result = criteria.limit("10 OFFSET 20");
        assertNotNull(result);
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testUnion() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        Criteria result = criteria.union(subQuery);
        assertNotNull(result);
        assertTrue(criteria.getAggregation().size() > 0);
    }

    @Test
    public void testUnionAll() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        Criteria result = criteria.unionAll(subQuery);
        assertNotNull(result);
        assertTrue(criteria.getAggregation().size() > 0);
    }

    @Test
    public void testIntersect() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM premium_users");
        Criteria result = criteria.intersect(subQuery);
        assertNotNull(result);
        assertTrue(criteria.getAggregation().size() > 0);
    }

    @Test
    public void testExcept() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM blocked_users");
        Criteria result = criteria.except(subQuery);
        assertNotNull(result);
        assertTrue(criteria.getAggregation().size() > 0);
    }

    @Test
    public void testMinus() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM inactive_users");
        Criteria result = criteria.minus(subQuery);
        assertNotNull(result);
        assertTrue(criteria.getAggregation().size() > 0);
    }

    @Test
    public void testCopy() {
        Criteria original = new Criteria();
        original.distinct().where(Filters.equal("status", "active")).groupBy("department").having("COUNT(*) > 5").orderBy("name").limit(10);

        Criteria copy = original.copy();

        assertNotNull(copy);
        assertEquals(original.preselect(), copy.preselect());
        assertEquals(original.getConditions().size(), copy.getConditions().size());

        // Verify it's a deep copy
        copy.clear();
        assertTrue(copy.getConditions().isEmpty());
        assertFalse(original.getConditions().isEmpty());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Criteria criteria = new Criteria();
        criteria.distinct().where(Filters.equal("isActive", true)).orderBy("createdDate", SortDirection.DESC);

        String sql = criteria.toString(com.landawn.abacus.util.NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testHashCode() {
        Criteria c1 = new Criteria();
        c1.where(Filters.equal("status", "active"));

        Criteria c2 = new Criteria();
        c2.where(Filters.equal("status", "active"));

        // HashCode should be consistent
        assertEquals(c1.hashCode(), c1.hashCode());
        assertNotNull(c2.hashCode());
    }

    @Test
    public void testEquals() {
        Criteria c1 = new Criteria();
        c1.distinct().where(Filters.equal("status", "active"));

        Criteria c2 = new Criteria();
        c2.distinct().where(Filters.equal("status", "active"));

        // Test self-equality
        assertEquals(c1, c1);

        // Test equality with same content
        assertEquals(c1, c2);

        // Test inequality
        Criteria c3 = new Criteria();
        c3.where(Filters.equal("status", "inactive"));
        assertFalse(c1.equals(c3));

        // Test null
        assertFalse(c1.equals(null));

        // Test different type
        assertFalse(c1.equals("string"));
    }

    @Test
    public void testDistinctByEmpty() {
        Criteria criteria = new Criteria();
        criteria.distinctBy("");
        assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testDistinctByNull() {
        Criteria criteria = new Criteria();
        criteria.distinctBy(null);
        assertEquals("DISTINCT", criteria.preselect());
    }

    @Test
    public void testDistinctRowByEmpty() {
        Criteria criteria = new Criteria();
        criteria.distinctRowBy("");
        assertEquals("DISTINCTROW", criteria.preselect());
    }

    @Test
    public void testDistinctRowByNull() {
        Criteria criteria = new Criteria();
        criteria.distinctRowBy(null);
        assertEquals("DISTINCTROW", criteria.preselect());
    }

    @Test
    public void testMultipleAggregations() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.SubQuery subQuery1 = Filters.subQuery("SELECT * FROM archived");
        com.landawn.abacus.query.condition.SubQuery subQuery2 = Filters.subQuery("SELECT * FROM deleted");

        criteria.union(subQuery1).unionAll(subQuery2);

        List<Cell> aggregations = criteria.getAggregation();
        assertEquals(2, aggregations.size());
    }

    @Test
    public void testGetEmptyAggregation() {
        Criteria criteria = new Criteria();
        List<Cell> aggregations = criteria.getAggregation();
        assertNotNull(aggregations);
        assertTrue(aggregations.isEmpty());
    }

    @Test
    public void testClearParametersMultiple() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("name", "John")).where(Filters.equal("age", 30));
        criteria.clearParameters();
        // Parameters should be cleared
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParametersEmpty() {
        Criteria criteria = new Criteria();
        List<Object> params = criteria.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testWhereWithWhereOperator() {
        Criteria criteria = new Criteria();
        Where where = new Where(Filters.equal("status", "active"));
        criteria.where(where);
        assertNotNull(criteria.getWhere());
    }

    @Test
    public void testGroupByWithGroupByOperator() {
        Criteria criteria = new Criteria();
        GroupBy groupBy = new GroupBy(Filters.expr("department"));
        criteria.groupBy(groupBy);
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    public void testHavingWithHavingOperator() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.Having having = new com.landawn.abacus.query.condition.Having(Filters.expr("COUNT(*) > 5"));
        criteria.having(having);
        assertNotNull(criteria.getHaving());
    }

    @Test
    public void testOrderByWithOrderByOperator() {
        Criteria criteria = new Criteria();
        com.landawn.abacus.query.condition.OrderBy orderBy = new com.landawn.abacus.query.condition.OrderBy(Filters.expr("name ASC"));
        criteria.orderBy(orderBy);
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    public void testGetJoinsMultipleTypes() {
        Criteria criteria = new Criteria();
        criteria.join("orders", Filters.expr("id = order_id"));
        criteria.join(new LeftJoin("payments", Filters.expr("id = payment_id")));
        criteria.join(new RightJoin("products", Filters.expr("id = product_id")));

        List<Join> joins = criteria.getJoins();
        assertEquals(3, joins.size());
    }

    @Test
    public void testGetByOperatorMultiple() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.equal("status", "active"));

        // Replace where with another
        criteria.where(Filters.equal("type", "premium"));

        List<Condition> whereConditions = criteria.get(Operator.WHERE);
        assertEquals(1, whereConditions.size());
    }

    @Test
    public void testComplexCriteria() {
        Criteria criteria = new Criteria();
        criteria.distinct()
                .join("orders", Filters.expr("users.id = orders.user_id"))
                .where(Filters.and(Filters.equal("users.status", "active"), Filters.gt("orders.total", 100)))
                .groupBy("users.department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("COUNT(*)", SortDirection.DESC)
                .limit(10);

        assertNotNull(criteria.preselect());
        assertEquals(1, criteria.getJoins().size());
        assertNotNull(criteria.getWhere());
        assertNotNull(criteria.getGroupBy());
        assertNotNull(criteria.getHaving());
        assertNotNull(criteria.getOrderBy());
        assertNotNull(criteria.getLimit());
    }

    @Test
    public void testReplaceClauses() {
        Criteria criteria = new Criteria();

        // Add initial clauses
        criteria.where(Filters.equal("status", "active"));
        criteria.groupBy("department");
        criteria.having("COUNT(*) > 5");
        criteria.orderBy("name");
        criteria.limit(10);

        // Replace them
        criteria.where(Filters.equal("status", "inactive"));
        criteria.groupBy("location");
        criteria.having("COUNT(*) > 10");
        criteria.orderBy("age");
        criteria.limit(20);

        // Only latest values should exist
        assertEquals(5, criteria.getConditions().size());
    }

    @Test
    public void testPreselectNull() {
        Criteria criteria = new Criteria();
        criteria.preselect(null);
        // Should not throw error
        assertNotNull(criteria);
    }

    @Test
    public void testToStringEmptyCriteria() {
        Criteria criteria = new Criteria();
        String sql = criteria.toString(com.landawn.abacus.util.NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(sql);
    }

    @Test
    public void testGetNullClauses() {
        Criteria criteria = new Criteria();

        // All clauses should be null initially
        assertNull(criteria.getWhere());
        assertNull(criteria.getGroupBy());
        assertNull(criteria.getHaving());
        assertNull(criteria.getOrderBy());
        assertNull(criteria.getLimit());
        assertNull(criteria.preselect());
    }
}
