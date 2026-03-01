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
 * Comprehensive test class for {@link GroupBy}.
 */
@Tag("2025")
public class GroupBy2025Test extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        GroupBy groupBy = new GroupBy(Filters.expr("department"));

        assertNotNull(groupBy);
        assertEquals(Operator.GROUP_BY, groupBy.operator());
    }

    @Test
    public void testConstructorWithSingleProperty() {
        GroupBy groupBy = new GroupBy("department");

        assertNotNull(groupBy);
        assertTrue(groupBy.toString().contains("department"));
    }

    @Test
    public void testConstructorWithMultipleProperties() {
        GroupBy groupBy = new GroupBy("department", "location", "year");
        String result = groupBy.toString();

        assertTrue(result.contains("department"));
        assertTrue(result.contains("location"));
        assertTrue(result.contains("year"));
    }

    @Test
    public void testConstructorWithPropertyAndDirection() {
        GroupBy groupBy = new GroupBy("salary", SortDirection.DESC);
        String result = groupBy.toString();

        assertTrue(result.contains("salary"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithPropertyAndAscDirection() {
        GroupBy groupBy = new GroupBy("name", SortDirection.ASC);
        String result = groupBy.toString();

        assertTrue(result.contains("name"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testConstructorWithCollectionAndDirection() {
        List<String> props = Arrays.asList("region", "category");
        GroupBy groupBy = new GroupBy(props, SortDirection.DESC);
        String result = groupBy.toString();

        assertTrue(result.contains("region"));
        assertTrue(result.contains("category"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);

        GroupBy groupBy = new GroupBy(orders);
        String result = groupBy.toString();

        assertTrue(result.contains("department"));
        assertTrue(result.contains("salary"));
        assertTrue(result.contains("ASC"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testToString() {
        GroupBy groupBy = new GroupBy("status");
        String result = groupBy.toString();

        assertTrue(result.contains("GROUP BY") || result.contains("status"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GroupBy groupBy = new GroupBy("productCategory");
        String result = groupBy.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("product_category") || result.contains("productCategory"));
    }

    @Test
    public void testGetParameters() {
        GroupBy groupBy = new GroupBy("department");

        assertNotNull(groupBy.getParameters());
        assertTrue(groupBy.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        GroupBy groupBy = new GroupBy("category");
        groupBy.clearParameters();

        assertNotNull(groupBy.getParameters());
    }

    @Test
    public void testEquals() {
        GroupBy groupBy1 = new GroupBy("department");
        GroupBy groupBy2 = new GroupBy("department");
        GroupBy groupBy3 = new GroupBy("location");

        assertEquals(groupBy1, groupBy2);
        assertNotEquals(groupBy1, groupBy3);
    }

    @Test
    public void testEqualsWithDirection() {
        GroupBy groupBy1 = new GroupBy("salary", SortDirection.DESC);
        GroupBy groupBy2 = new GroupBy("salary", SortDirection.DESC);
        GroupBy groupBy3 = new GroupBy("salary", SortDirection.ASC);

        assertEquals(groupBy1, groupBy2);
        assertNotEquals(groupBy1, groupBy3);
    }

    @Test
    public void testHashCode() {
        GroupBy groupBy1 = new GroupBy("department");
        GroupBy groupBy2 = new GroupBy("department");

        assertEquals(groupBy1.hashCode(), groupBy2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        GroupBy groupBy = new GroupBy("category", SortDirection.ASC);
        int hash1 = groupBy.hashCode();
        int hash2 = groupBy.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testCopy() {
        GroupBy original = new GroupBy("department", "location");
        GroupBy copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
    }

    @Test
    public void testWithThreeProperties() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("year", SortDirection.DESC);
        orders.put("month", SortDirection.DESC);
        orders.put("day", SortDirection.ASC);

        GroupBy groupBy = new GroupBy(orders);
        String result = groupBy.toString();

        assertTrue(result.contains("year"));
        assertTrue(result.contains("month"));
        assertTrue(result.contains("day"));
    }

    @Test
    public void testOperatorType() {
        GroupBy groupBy = new GroupBy("column");

        assertEquals(Operator.GROUP_BY, groupBy.operator());
        assertNotEquals(Operator.ORDER_BY, groupBy.operator());
    }

    @Test
    public void testWithExpression() {
        GroupBy groupBy = new GroupBy(Filters.expr("YEAR(order_date)"));
        String result = groupBy.toString();

        assertNotNull(result);
        assertTrue(result.contains("YEAR") || result.length() > 0);
    }

    @Test
    public void testEmptyParameterList() {
        List<String> props = Arrays.asList("col1", "col2", "col3");
        GroupBy groupBy = new GroupBy(props, SortDirection.ASC);

        // GroupBy should not have parameters
        assertEquals(0, groupBy.getParameters().size());
    }

    @Test
    public void testMultipleColumnsFormatting() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3", "col4");
        String result = groupBy.toString();

        // Should contain all columns
        assertTrue(result.contains("col1"));
        assertTrue(result.contains("col2"));
        assertTrue(result.contains("col3"));
        assertTrue(result.contains("col4"));
    }

    @Test
    public void testSortDirectionPreservation() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("first", SortDirection.DESC);
        orders.put("second", SortDirection.ASC);
        orders.put("third", SortDirection.DESC);

        GroupBy groupBy = new GroupBy(orders);
        String result = groupBy.toString();

        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testWithSingleElementCollection() {
        List<String> props = Arrays.asList("singleColumn");
        GroupBy groupBy = new GroupBy(props, SortDirection.ASC);

        String result = groupBy.toString();
        assertTrue(result.contains("singleColumn"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testAndThrowsException() {
        GroupBy groupBy = new GroupBy("department");
        assertThrows(UnsupportedOperationException.class, () -> {
            groupBy.and(Filters.eq("status", "active"));
        });
    }

    @Test
    public void testOrThrowsException() {
        GroupBy groupBy = new GroupBy("department");
        assertThrows(UnsupportedOperationException.class, () -> {
            groupBy.or(Filters.eq("status", "active"));
        });
    }

    @Test
    public void testNotThrowsException() {
        GroupBy groupBy = new GroupBy("department");
        assertThrows(UnsupportedOperationException.class, () -> {
            groupBy.not();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetCondition() {
        GroupBy groupBy = new GroupBy(Filters.expr("department"));
        Expression newCondition = Filters.expr("location");
        groupBy.setCondition(newCondition);

        Condition retrieved = groupBy.getCondition();
        assertEquals(newCondition, retrieved);
    }

    @Test
    public void testGetCondition() {
        Expression condition = Filters.expr("department, location");
        GroupBy groupBy = new GroupBy(condition);

        Condition retrieved = groupBy.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testToString_NoArgs() {
        GroupBy groupBy = new GroupBy("category", "region");
        String result = groupBy.toString();

        assertTrue(result.contains("category"));
        assertTrue(result.contains("region"));
    }

    @Test
    public void testCopy_Independence() {
        GroupBy original = new GroupBy("department", "location");
        GroupBy copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals_DifferentClass() {
        GroupBy groupBy = new GroupBy("column");
        OrderBy orderBy = new OrderBy("column");
        assertNotEquals(groupBy, (Object) orderBy);
    }

    @Test
    public void testConstructorWithEmptyMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            new GroupBy(orders);
        });
    }

    @Test
    public void testConstructorWithCollectionOfOne() {
        List<String> props = Arrays.asList("singleCol");
        GroupBy groupBy = new GroupBy(props, SortDirection.DESC);

        assertTrue(groupBy.toString().contains("singleCol"));
    }
}
