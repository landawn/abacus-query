package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class In2025Test extends TestBase {

    @Test
    public void testConstructor_ValidList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        assertEquals("status", condition.getPropName());
        assertEquals(3, condition.getValues().size());
        assertEquals(Operator.IN, condition.operator());
    }

    @Test
    public void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", null));
    }

    @Test
    public void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new In("status", Arrays.asList()));
    }

    @Test
    public void testConstructor_Set() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        assertEquals("user_id", condition.getPropName());
        assertEquals(5, condition.getValues().size());
    }

    @Test
    public void testConstructor_SingleValue() {
        In condition = new In("category", Arrays.asList("electronics"));

        assertEquals("category", condition.getPropName());
        assertEquals(1, condition.getValues().size());
    }

    @Test
    public void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        In condition = new In("grade", values);

        assertEquals(3, condition.getValues().size());
        assertNotSame(values, condition.getValues());
    }

    @Test
    public void testGetPropName() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals("status", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        In condition = new In("id", values);

        List<?> result = condition.getValues();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetParameters() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        List<Object> params = condition.getParameters();
        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals("pending", params.get(1));
        assertEquals("approved", params.get(2));
    }

    @Test
    public void testGetParameters_Integers() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        In condition = new In("id", values);

        List<Object> params = condition.getParameters();
        assertEquals(3, params.size());
        assertEquals(Integer.valueOf(10), params.get(0));
    }

    @Test
    public void testGetParameters_WithNestedConditionValues() {
        Equal nested = Filters.equal("status", "active");
        In condition = new In("id", Arrays.asList(nested, 2));

        List<Object> params = condition.getParameters();
        assertEquals(Arrays.asList("active", 2), params);
    }

    @Test
    public void testClearParameters() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        condition.clearParameters();

        List<?> values = condition.getValues();
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals(null, values.get(1));
    }

    @Test
    public void testClearParameters_WithNestedConditionValues() {
        In condition = new In("id", Arrays.asList(Filters.equal("status", "active"), 2));
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        assertEquals(Arrays.asList(null, null), params);
        assertTrue(condition.getValues().get(0) instanceof Condition);
    }

    @Test
    public void testToString_NoChange() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("active"));
        assertTrue(result.contains("pending"));
    }

    @Test
    public void testToString_SnakeCase() {
        In condition = new In("orderStatus", Arrays.asList("active"));
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("IN"));
    }

    @Test
    public void testToString_MultipleValues() {
        In condition = new In("id", Arrays.asList(1, 2, 3, 4, 5));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("5"));
    }

    @Test
    public void testHashCode_Equal() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        In condition = new In("status", Arrays.asList("active"));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        In c1 = new In("status", Arrays.asList("active", "pending"));
        In c2 = new In("status", Arrays.asList("active", "pending"));

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("state", Arrays.asList("active"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentValues() {
        In c1 = new In("status", Arrays.asList("active"));
        In c2 = new In("status", Arrays.asList("inactive"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        In condition = new In("status", Arrays.asList("active"));
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentType() {
        In condition = new In("status", Arrays.asList("active"));
        String other = "not an In";
        assertNotEquals(condition, other);
    }

    @Test
    public void testUseCaseScenario_StatusFilter() {
        // Check if status is one of several values
        List<String> statuses = Arrays.asList("active", "pending", "approved");
        In statusCheck = new In("status", statuses);

        assertEquals(3, statusCheck.getParameters().size());
        assertTrue(statusCheck.toString(NamingPolicy.NO_CHANGE).contains("IN"));
    }

    @Test
    public void testUseCaseScenario_UserIdFilter() {
        // Check if user_id is in a list
        List<Integer> userIds = Arrays.asList(1, 2, 3, 5, 8);
        In userFilter = new In("user_id", userIds);

        assertEquals(5, userFilter.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_CategoryFilter() {
        // Filter by categories
        Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
        In categoryFilter = new In("category", categories);

        assertEquals(2, categoryFilter.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_EnumValues() {
        // Filter by enum values
        List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
        In priorityFilter = new In("priority", priorities);

        String sql = priorityFilter.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("priority"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testUseCaseScenario_LargeList() {
        // Test with larger list
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        In condition = new In("id", largeList);

        assertEquals(10, condition.getParameters().size());
    }

    @Test
    public void testAnd() {
        In cond1 = new In("status", Arrays.asList("active", "pending"));
        In cond2 = new In("type", Arrays.asList("premium", "gold"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        In cond1 = new In("status", Arrays.asList("active"));
        In cond2 = new In("status", Arrays.asList("pending"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        In condition = new In("status", Arrays.asList("active", "pending"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class InTest extends TestBase {

    @Test
    public void testConstructorWithList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IN, condition.operator());
        Assertions.assertEquals(3, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("user_id", condition.getPropName());
        Assertions.assertEquals(5, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        In condition = new In("mixed_field", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals(4, condition.getValues().size());
    }

    @Test
    public void testGetPropName() {
        In condition = new In("category", Arrays.asList("A", "B", "C"));
        Assertions.assertEquals("category", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<String> values = Arrays.asList("red", "green", "blue");
        In condition = new In("color", values);

        List<?> retrievedValues = condition.getValues();
        Assertions.assertNotNull(retrievedValues);
        Assertions.assertEquals(3, retrievedValues.size());
        Assertions.assertTrue(retrievedValues.containsAll(values));
    }

    @Test
    public void testGetParameters() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        In condition = new In("value", values);

        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals(values, params);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testClearParameters() {
        In condition = new In("id", Arrays.asList(1, 2, 3));
        condition.clearParameters();

        List values = condition.getValues();
        Assertions.assertNotNull(values);
        for (Object value : values) {
            Assertions.assertNull(value);
        }
    }

    @Test
    public void testToString() {
        In condition = new In("category", Arrays.asList("electronics", "computers", "phones"));
        String result = condition.toString();

        Assertions.assertTrue(result.contains("category"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("("));
        Assertions.assertTrue(result.contains(")"));
        Assertions.assertTrue(result.contains("electronics"));
        Assertions.assertTrue(result.contains("computers"));
        Assertions.assertTrue(result.contains("phones"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        In condition = new In("productId", Arrays.asList(101, 102, 103));
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("PRODUCT_ID"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("101"));
    }

    @Test
    public void testHashCode() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new In("field", new ArrayList<>());
        });
    }

    @Test
    public void testLargeValuesList() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        In condition = new In("id", values);

        Assertions.assertEquals(1000, condition.getValues().size());
        Assertions.assertEquals(1000, condition.getParameters().size());
    }

    @Test
    public void testSingleValue() {
        In condition = new In("type", Collections.singletonList("SINGLE"));

        Assertions.assertEquals(1, condition.getValues().size());
        Assertions.assertEquals("SINGLE", condition.getValues().get(0));

        String result = condition.toString();
        Assertions.assertTrue(result.contains("type IN ('SINGLE')"));
    }

    @Test
    public void testNullValues() {
        List<String> values = Arrays.asList("A", null, "B");
        In condition = new In("field", values);

        Assertions.assertEquals(3, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().contains(null));
    }
}
