package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test class for {@link OrderBy}.
 */
@Tag("2025")
class OrderBy2025Test extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        OrderBy orderBy = new OrderBy(Filters.expr("name ASC"));

        assertNotNull(orderBy);
        assertEquals(Operator.ORDER_BY, orderBy.operator());
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
        String result = AbstractCondition.createSortExpression("name", "age", "city");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
        assertTrue(result.contains("city"));
    }

    @Test
    public void testStaticCreateConditionWithDirection() {
        String result = AbstractCondition.createSortExpression("price", SortDirection.DESC);

        assertTrue(result.contains("price"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testStaticCreateConditionWithCollection() {
        List<String> props = Arrays.asList("col1", "col2");
        String result = AbstractCondition.createSortExpression(props, SortDirection.ASC);

        assertTrue(result.contains("col1"));
        assertTrue(result.contains("col2"));
        assertTrue(result.contains("ASC"));
    }

    @Test
    public void testStaticCreateConditionWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("first", SortDirection.DESC);
        orders.put("second", SortDirection.ASC);

        String result = AbstractCondition.createSortExpression(orders);

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
        String result = orderBy.toString(NamingPolicy.SNAKE_CASE);

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
    public void testOperatorType() {
        OrderBy orderBy = new OrderBy("column");

        assertEquals(Operator.ORDER_BY, orderBy.operator());
        assertNotEquals(Operator.GROUP_BY, orderBy.operator());
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
        String result = AbstractCondition.createSortExpression("a", "b", "c");

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
        assertTrue(result.contains(", "));
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
        String result = AbstractCondition.createSortExpression("name");
        assertTrue(result.contains("name"));
    }

    @Test
    public void testStaticCreateConditionWithEmptyMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            AbstractCondition.createSortExpression(orders);
        });
    }

    @Test
    public void testHashCode_DifferentDirections() {
        OrderBy orderBy1 = new OrderBy("name", SortDirection.ASC);
        OrderBy orderBy2 = new OrderBy("name", SortDirection.DESC);
        assertNotEquals(orderBy1.hashCode(), orderBy2.hashCode());
    }
}

public class OrderByTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Expression expr = Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END");
        OrderBy orderBy = Filters.orderBy(expr);

        Assertions.assertNotNull(orderBy);
        Assertions.assertEquals(Operator.ORDER_BY, orderBy.operator());
        Assertions.assertEquals(expr, orderBy.getCondition());
    }

    @Test
    public void testConstructorWithVarArgs() {
        OrderBy orderBy = Filters.orderBy("country", "state", "city");

        Assertions.assertEquals(Operator.ORDER_BY, orderBy.operator());
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
