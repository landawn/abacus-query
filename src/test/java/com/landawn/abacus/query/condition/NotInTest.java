package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class NotIn2025Test extends TestBase {

    @Test
    public void testConstructor_ValidList() {
        List<String> values = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", values);

        assertEquals("status", condition.getPropName());
        assertEquals(3, condition.getValues().size());
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    public void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", null));
    }

    @Test
    public void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", Arrays.asList()));
    }

    @Test
    public void testConstructor_Set() {
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn condition = new NotIn("department_id", excludedDepts);

        assertEquals("department_id", condition.getPropName());
        assertEquals(3, condition.getValues().size());
    }

    @Test
    public void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn condition = new NotIn("status", values);

        assertNotSame(values, condition.getValues());
    }

    @Test
    public void testGetPropName() {
        NotIn condition = new NotIn("status", Arrays.asList("inactive"));
        assertEquals("status", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        NotIn condition = new NotIn("id", values);

        List<?> result = condition.getValues();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetParameters() {
        List<String> values = Arrays.asList("deleted", "archived");
        NotIn condition = new NotIn("status", values);

        List<Object> params = condition.getParameters();
        assertEquals(2, params.size());
        assertEquals("deleted", params.get(0));
        assertEquals("archived", params.get(1));
    }

    @Test
    public void testClearParameters() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        condition.clearParameters();

        List<?> values = condition.getValues();
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals(null, values.get(1));
    }

    @Test
    public void testToString_NoChange() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("NOT IN"));
        assertTrue(result.contains("deleted"));
        assertTrue(result.contains("archived"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotIn condition = new NotIn("orderStatus", Arrays.asList("deleted"));
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testHashCode_Equal() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted", "archived"));

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("state", Arrays.asList("deleted"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentValues() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_ExcludeInactiveStatuses() {
        // Exclude inactive statuses
        List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", inactiveStatuses);

        assertEquals(3, condition.getParameters().size());
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    public void testUseCaseScenario_ExcludeDepartments() {
        // Exclude specific department IDs
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn deptCondition = new NotIn("department_id", excludedDepts);

        assertEquals(3, deptCondition.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_ExcludeTestUsers() {
        // Exclude test users
        List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
        NotIn emailCondition = new NotIn("email", testEmails);

        assertEquals(2, emailCondition.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_ExcludeCategories() {
        // Exclude specific product categories
        List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
        NotIn notIn = new NotIn("category", excludedCategories);

        assertEquals(3, notIn.getParameters().size());
        assertTrue(notIn.toString(NamingPolicy.NO_CHANGE).contains("category"));
    }

    @Test
    public void testEquals_DifferentType() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        String other = "not a NotIn";
        assertNotEquals(condition, other);
    }

    @Test
    public void testConstructor_SingleValue() {
        NotIn condition = new NotIn("type", Arrays.asList("test"));

        assertEquals("type", condition.getPropName());
        assertEquals(1, condition.getValues().size());
    }

    @Test
    public void testAnd() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn cond2 = new NotIn("type", Arrays.asList("test", "demo"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn cond2 = new NotIn("status", Arrays.asList("archived"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class NotInTest extends TestBase {

    @Test
    public void testConstructorWithList() {
        List<String> values = Arrays.asList("deleted", "archived", "inactive");
        NotIn notIn = Filters.notIn("status", values);

        Assertions.assertNotNull(notIn);
        Assertions.assertEquals("status", notIn.getPropName());
        Assertions.assertEquals(values, notIn.getValues());
        Assertions.assertEquals(Operator.NOT_IN, notIn.operator());
    }

    @Test
    public void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        NotIn notIn = Filters.notIn("userId", values);

        Assertions.assertEquals("userId", notIn.getPropName());
        Assertions.assertEquals(5, notIn.getValues().size());
        Assertions.assertTrue(notIn.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("status", emptyList);
        });
    }

    @Test
    public void testConstructorWithNullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("status", (List<String>) null);
        });
    }

    @Test
    public void testGetParameters() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        NotIn notIn = Filters.notIn("amount", values);

        List<Object> params = notIn.getParameters();
        Assertions.assertEquals(values, params);
    }

    @Test
    public void testClearParameters() {
        List<String> values = new ArrayList<>(Arrays.asList("a", "b", "c"));
        NotIn notIn = Filters.notIn("type", values);

        notIn.clearParameters();

        // Values should be filled with null
        Assertions.assertTrue(notIn.getValues().stream().allMatch(v -> v == null));
    }

    @Test
    public void testToString() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn notIn = Filters.notIn("category", values);

        String result = notIn.toString();
        Assertions.assertTrue(result.contains("category"));
        Assertions.assertTrue(result.contains("NOT IN"));
        Assertions.assertTrue(result.contains("category NOT IN ('A', 'B', 'C')"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        List<String> values = Arrays.asList("active", "pending");
        NotIn notIn = Filters.notIn("user_status", values);

        String result = notIn.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("USER_STATUS"));
        Assertions.assertTrue(result.contains("NOT IN"));
        Assertions.assertTrue(result.contains("USER_STATUS NOT IN ('active', 'pending')"));
    }

    @Test
    public void testHashCode() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");

        NotIn notIn1 = Filters.notIn("type", values1);
        NotIn notIn2 = Filters.notIn("type", values2);
        NotIn notIn3 = Filters.notIn("type", values3);
        NotIn notIn4 = Filters.notIn("category", values1);

        Assertions.assertEquals(notIn1.hashCode(), notIn2.hashCode());
        Assertions.assertNotEquals(notIn1.hashCode(), notIn3.hashCode());
        Assertions.assertNotEquals(notIn1.hashCode(), notIn4.hashCode());
    }

    @Test
    public void testEquals() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");

        NotIn notIn1 = Filters.notIn("type", values1);
        NotIn notIn2 = Filters.notIn("type", values2);
        NotIn notIn3 = Filters.notIn("type", values3);
        NotIn notIn4 = Filters.notIn("category", values1);

        Assertions.assertTrue(notIn1.equals(notIn1));
        Assertions.assertTrue(notIn1.equals(notIn2));
        Assertions.assertFalse(notIn1.equals(notIn3));
        Assertions.assertFalse(notIn1.equals(notIn4));
        Assertions.assertFalse(notIn1.equals(null));
        Assertions.assertFalse(notIn1.equals("not a NotIn"));
    }

    @Test
    public void testWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        NotIn notIn = Filters.notIn("mixedField", values);

        Assertions.assertEquals(4, notIn.getValues().size());
        Assertions.assertTrue(notIn.getValues().contains(1));
        Assertions.assertTrue(notIn.getValues().contains("two"));
        Assertions.assertTrue(notIn.getValues().contains(3.0));
        Assertions.assertTrue(notIn.getValues().contains(true));
    }

    @Test
    public void testLargeValueSet() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        NotIn notIn = Filters.notIn("id", values);

        Assertions.assertEquals(1000, notIn.getValues().size());
        Assertions.assertEquals(1000, notIn.getParameters().size());
    }
}
