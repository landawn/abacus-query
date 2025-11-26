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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
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
}
