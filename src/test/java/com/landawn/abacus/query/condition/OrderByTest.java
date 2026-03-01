package com.landawn.abacus.query.condition;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;

public class OrderByTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Expression expr = Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END");
        OrderBy orderBy = Filters.orderBy(expr);

        Assertions.assertNotNull(orderBy);
        Assertions.assertEquals(Operator.ORDER_BY, orderBy.getOperator());
        Assertions.assertEquals(expr, orderBy.getCondition());
    }

    @Test
    public void testConstructorWithVarArgs() {
        OrderBy orderBy = Filters.orderBy("country", "state", "city");

        Assertions.assertEquals(Operator.ORDER_BY, orderBy.getOperator());
        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("country, state, city"));
    }

    @Test
    public void testConstructorWithSingleProperty() {
        OrderBy orderBy = Filters.orderBy("lastName");

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("lastName"));
    }

    @Test
    public void testConstructorWithPropertyAndDirection() {
        OrderBy orderBy = Filters.orderBy("price", SortDirection.DESC);

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("price DESC"));
    }

    @Test
    public void testConstructorWithCollectionAndDirection() {
        List<String> dateFields = Arrays.asList("created", "updated", "published");
        OrderBy orderBy = Filters.orderBy(dateFields, SortDirection.DESC);

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("created DESC, updated DESC, published DESC"));
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("isActive", SortDirection.DESC);
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);

        OrderBy orderBy = Filters.orderBy(orders);

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("isActive DESC"));
        Assertions.assertTrue(result.contains("priority DESC"));
        Assertions.assertTrue(result.contains("created ASC"));
    }

    @Test
    public void testCreateConditionWithVarArgs() {
        String result = AbstractCondition.createSortExpression("col1", "col2", "col3");
        Assertions.assertEquals("col1, col2, col3", result);
    }

    @Test
    public void testCreateConditionWithEmptyVarArgs() {
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression());
    }

    @Test
    public void testCreateConditionWithSinglePropertyAndDirection() {
        String result = AbstractCondition.createSortExpression("salary", SortDirection.DESC);
        Assertions.assertEquals("salary DESC", result);
    }

    @Test
    public void testCreateConditionWithCollectionAndDirection() {
        List<String> props = Arrays.asList("year", "month", "day");
        String result = AbstractCondition.createSortExpression(props, SortDirection.ASC);
        Assertions.assertEquals("year ASC, month ASC, day ASC", result);
    }

    @Test
    public void testCreateConditionWithEmptyCollection() {
        List<String> emptyList = Arrays.asList();
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression(emptyList, SortDirection.ASC));
    }

    @Test
    public void testCreateConditionWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("col1", SortDirection.ASC);
        orders.put("col2", SortDirection.DESC);

        String result = AbstractCondition.createSortExpression(orders);
        Assertions.assertEquals("col1 ASC, col2 DESC", result);
    }

    @Test
    public void testCreateConditionWithEmptyMap() {
        Map<String, SortDirection> emptyMap = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortExpression(emptyMap));
    }

    @Test
    public void testGetCondition() {
        Expression expr = Filters.expr("custom expression");
        OrderBy orderBy = Filters.orderBy(expr);

        Assertions.assertEquals(expr, orderBy.getCondition());
    }

    @Test
    public void testToString() {
        OrderBy orderBy = Filters.orderBy("name", "age");

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("ORDER BY"));
        Assertions.assertTrue(result.contains("name, age"));
    }

    @Test
    public void testCopy() {
        OrderBy original = Filters.orderBy("created", SortDirection.DESC);

        OrderBy copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        OrderBy orderBy1 = Filters.orderBy("name", "age");
        OrderBy orderBy2 = Filters.orderBy("name", "age");
        OrderBy orderBy3 = Filters.orderBy("age", "name");

        Assertions.assertEquals(orderBy1.hashCode(), orderBy2.hashCode());
        Assertions.assertNotEquals(orderBy1.hashCode(), orderBy3.hashCode());
    }

    @Test
    public void testEquals() {
        OrderBy orderBy1 = Filters.orderBy("name", "age");
        OrderBy orderBy2 = Filters.orderBy("name", "age");
        OrderBy orderBy3 = Filters.orderBy("age", "name");

        Assertions.assertTrue(orderBy1.equals(orderBy1));
        Assertions.assertTrue(orderBy1.equals(orderBy2));
        Assertions.assertFalse(orderBy1.equals(orderBy3));
        Assertions.assertFalse(orderBy1.equals(null));
        Assertions.assertFalse(orderBy1.equals("not an OrderBy"));
    }

    @Test
    public void testComplexOrdering() {
        // Test complex ordering with mixed directions
        Map<String, SortDirection> complexOrder = new LinkedHashMap<>();
        complexOrder.put("status", SortDirection.DESC);
        complexOrder.put("priority", SortDirection.DESC);
        complexOrder.put("created_date", SortDirection.ASC);
        complexOrder.put("id", SortDirection.ASC);

        OrderBy orderBy = Filters.orderBy(complexOrder);

        String result = orderBy.toString();
        Assertions.assertTrue(result.contains("status DESC"));
        Assertions.assertTrue(result.contains("priority DESC"));
        Assertions.assertTrue(result.contains("created_date ASC"));
        Assertions.assertTrue(result.contains("id ASC"));
    }

    @Test
    public void testPracticalExamples() {
        // Simple ascending order (default)
        OrderBy orderBy1 = Filters.orderBy("lastName", "firstName");
        Assertions.assertTrue(orderBy1.toString().contains("lastName, firstName"));

        // Descending order
        OrderBy orderBy2 = Filters.orderBy("salary", SortDirection.DESC);
        Assertions.assertTrue(orderBy2.toString().contains("salary DESC"));

        // Multiple columns with same direction
        OrderBy orderBy3 = Filters.orderBy(Arrays.asList("created", "modified"), SortDirection.DESC);
        Assertions.assertTrue(orderBy3.toString().contains("created DESC, modified DESC"));
    }
}
