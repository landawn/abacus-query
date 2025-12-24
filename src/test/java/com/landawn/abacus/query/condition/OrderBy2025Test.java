/*
 * Copyright (C) 2025 HaiYang Li
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link OrderBy}.
 */
@Tag("2025")
public class OrderBy2025Test extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        OrderBy orderBy = new OrderBy(Filters.expr("name ASC"));

        assertNotNull(orderBy);
        assertEquals(Operator.ORDER_BY, orderBy.getOperator());
    }

    @Test
    public void testConstructorWithSingleProperty() {
        OrderBy orderBy = new OrderBy("lastName");

        assertNotNull(orderBy);
        assertTrue(orderBy.toString().contains("lastName"));
    }

    @Test
    public void testConstructorWithMultipleProperties() {
        OrderBy orderBy = new OrderBy("lastName", "firstName", "middleName");
        String result = orderBy.toString();

        assertTrue(result.contains("lastName"));
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("middleName"));
    }

    @Test
    public void testConstructorWithPropertyAndDirection() {
        OrderBy orderBy = new OrderBy("salary", SortDirection.DESC);
        String result = orderBy.toString();

        assertTrue(result.contains("salary"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithPropertyAndAscDirection() {
        OrderBy orderBy = new OrderBy("created", SortDirection.ASC);
        String result = orderBy.toString();

        assertTrue(result.contains("created"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testConstructorWithCollectionAndDirection() {
        List<String> props = Arrays.asList("priority", "created");
        OrderBy orderBy = new OrderBy(props, SortDirection.DESC);
        String result = orderBy.toString();

        assertTrue(result.contains("priority"));
        assertTrue(result.contains("created"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);

        OrderBy orderBy = new OrderBy(orders);
        String result = orderBy.toString();

        assertTrue(result.contains("priority"));
        assertTrue(result.contains("created"));
        assertTrue(result.contains("DESC"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testStaticCreateConditionWithStrings() {
        String result = OrderBy.createCondition("name", "age", "city");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
        assertTrue(result.contains("city"));
    }

    @Test
    public void testStaticCreateConditionWithDirection() {
        String result = OrderBy.createCondition("price", SortDirection.DESC);

        assertTrue(result.contains("price"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testStaticCreateConditionWithCollection() {
        List<String> props = Arrays.asList("col1", "col2");
        String result = OrderBy.createCondition(props, SortDirection.ASC);

        assertTrue(result.contains("col1"));
        assertTrue(result.contains("col2"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testStaticCreateConditionWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("first", SortDirection.DESC);
        orders.put("second", SortDirection.ASC);

        String result = OrderBy.createCondition(orders);

        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
        assertTrue(result.contains("DESC"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testToString() {
        OrderBy orderBy = new OrderBy("status");
        String result = orderBy.toString();

        assertTrue(result.contains("ORDER BY") || result.contains("status"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        OrderBy orderBy = new OrderBy("userName");
        String result = orderBy.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertNotNull(result);
        assertTrue(result.contains("user_name") || result.contains("userName"));
    }

    @Test
    public void testGetParameters() {
        OrderBy orderBy = new OrderBy("column");

        assertNotNull(orderBy.getParameters());
        assertTrue(orderBy.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        OrderBy orderBy = new OrderBy("name", SortDirection.ASC);
        orderBy.clearParameters();

        assertNotNull(orderBy.getParameters());
    }

    @Test
    public void testEquals() {
        OrderBy orderBy1 = new OrderBy("name");
        OrderBy orderBy2 = new OrderBy("name");
        OrderBy orderBy3 = new OrderBy("age");

        assertEquals(orderBy1, orderBy2);
        assertNotEquals(orderBy1, orderBy3);
    }

    @Test
    public void testEqualsWithDirection() {
        OrderBy orderBy1 = new OrderBy("price", SortDirection.DESC);
        OrderBy orderBy2 = new OrderBy("price", SortDirection.DESC);
        OrderBy orderBy3 = new OrderBy("price", SortDirection.ASC);

        assertEquals(orderBy1, orderBy2);
        assertNotEquals(orderBy1, orderBy3);
    }

    @Test
    public void testHashCode() {
        OrderBy orderBy1 = new OrderBy("name");
        OrderBy orderBy2 = new OrderBy("name");

        assertEquals(orderBy1.hashCode(), orderBy2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        OrderBy orderBy = new OrderBy("column", SortDirection.DESC);
        int hash1 = orderBy.hashCode();
        int hash2 = orderBy.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testCopy() {
        OrderBy original = new OrderBy("name", "age");
        OrderBy copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
    }

    @Test
    public void testOperatorType() {
        OrderBy orderBy = new OrderBy("column");

        assertEquals(Operator.ORDER_BY, orderBy.getOperator());
        assertNotEquals(Operator.GROUP_BY, orderBy.getOperator());
    }

    @Test
    public void testWithExpression() {
        OrderBy orderBy = new OrderBy(Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END"));
        String result = orderBy.toString();

        assertNotNull(result);
        assertTrue(result.contains("CASE") || result.length() > 0);
    }

    @Test
    public void testMixedDirections() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);
        orders.put("name", SortDirection.ASC);

        OrderBy orderBy = new OrderBy(orders);
        String result = orderBy.toString();

        assertNotNull(result);
        assertTrue(result.contains("priority"));
        assertTrue(result.contains("created"));
        assertTrue(result.contains("name"));
    }

    @Test
    public void testEmptyParameterList() {
        OrderBy orderBy = new OrderBy("col1", "col2");

        assertEquals(0, orderBy.getParameters().size());
    }

    @Test
    public void testMultipleColumnsOrdering() {
        OrderBy orderBy = new OrderBy("country", "state", "city", "street");
        String result = orderBy.toString();

        assertTrue(result.contains("country"));
        assertTrue(result.contains("state"));
        assertTrue(result.contains("city"));
        assertTrue(result.contains("street"));
    }

    @Test
    public void testSingleColumnAscending() {
        OrderBy orderBy = new OrderBy("id", SortDirection.ASC);
        String result = orderBy.toString();

        assertTrue(result.contains("id"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testSingleColumnDescending() {
        OrderBy orderBy = new OrderBy("timestamp", SortDirection.DESC);
        String result = orderBy.toString();

        assertTrue(result.contains("timestamp"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testWithCaseExpression() {
        Expression expr = Filters.expr("CASE WHEN priority=1 THEN 0 ELSE 1 END");
        OrderBy orderBy = new OrderBy(expr);

        assertNotNull(orderBy);
        assertTrue(orderBy.toString().length() > 0);
    }

    @Test
    public void testCreateConditionStaticMethod() {
        String result = OrderBy.createCondition("a", "b", "c");

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
        assertTrue(result.contains(", "));
    }

    @Test
    public void testAndThrowsException() {
        OrderBy orderBy = new OrderBy("name");
        assertThrows(UnsupportedOperationException.class, () -> {
            orderBy.and(Filters.eq("status", "active"));
        });
    }

    @Test
    public void testOrThrowsException() {
        OrderBy orderBy = new OrderBy("name");
        assertThrows(UnsupportedOperationException.class, () -> {
            orderBy.or(Filters.eq("status", "active"));
        });
    }

    @Test
    public void testNotThrowsException() {
        OrderBy orderBy = new OrderBy("name");
        assertThrows(UnsupportedOperationException.class, () -> {
            orderBy.not();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetCondition() {
        OrderBy orderBy = new OrderBy(Filters.expr("name ASC"));
        Expression newCondition = Filters.expr("age DESC");
        orderBy.setCondition(newCondition);

        Condition retrieved = orderBy.getCondition();
        assertEquals(newCondition, retrieved);
    }

    @Test
    public void testGetCondition() {
        Expression condition = Filters.expr("name ASC, age DESC");
        OrderBy orderBy = new OrderBy(condition);

        Condition retrieved = orderBy.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testToString_NoArgs() {
        OrderBy orderBy = new OrderBy("name", "age");
        String result = orderBy.toString();

        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
    }

    @Test
    public void testCopy_Independence() {
        OrderBy original = new OrderBy("name", "age");
        OrderBy copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals_DifferentClass() {
        OrderBy orderBy = new OrderBy("column");
        GroupBy groupBy = new GroupBy("column");
        assertNotEquals(orderBy, (Object) groupBy);
    }

    @Test
    public void testConstructorWithEmptyMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderBy(orders);
        });
    }

    @Test
    public void testStaticCreateConditionWithSingleProperty() {
        String result = OrderBy.createCondition("name");
        assertTrue(result.contains("name"));
    }

    @Test
    public void testStaticCreateConditionWithEmptyMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            OrderBy.createCondition(orders);
        });
    }

    @Test
    public void testHashCode_DifferentDirections() {
        OrderBy orderBy1 = new OrderBy("name", SortDirection.ASC);
        OrderBy orderBy2 = new OrderBy("name", SortDirection.DESC);
        assertNotEquals(orderBy1.hashCode(), orderBy2.hashCode());
    }
}
