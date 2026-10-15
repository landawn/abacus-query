package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

public class NotInTest extends TestBase {

    @Test
    void testAnd() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn cond2 = new NotIn("type", Arrays.asList("test", "demo"));
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor_BlankPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("   ", Arrays.asList("active")));
    }

    @Test
    void testConstructor_DefensiveCopy() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn condition = new NotIn("status", values);

        assertNotSame(values, condition.values());
    }

    @Test
    void testConstructor_EmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", Arrays.asList()));
    }

    @Test
    void testConstructor_NullValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", null));
    }

    @Test
    void testConstructor_RejectsNullMember() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("status", Arrays.asList("deleted", null)));
    }

    @Test
    void testConstructor_Set() {
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn condition = new NotIn("department_id", excludedDepts);

        assertEquals("department_id", condition.propName());
        assertEquals(3, condition.values().size());
    }

    @Test
    void testConstructor_SingleValue() {
        NotIn condition = new NotIn("type", Arrays.asList("test"));

        assertEquals("type", condition.propName());
        assertEquals(1, condition.values().size());
    }

    @Test
    void testConstructor_ValidList() {
        List<String> values = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", values);

        assertEquals("status", condition.propName());
        assertEquals(3, condition.values().size());
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    void testConstructors_RejectQuantifiedConditionValues() {
        final SubQuery subQuery = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new NotIn("id", Arrays.asList(new All(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new NotIn("id", Arrays.asList(1, new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new NotIn(Arrays.asList("id", "tenant"), Arrays.asList(Arrays.asList(1, new Some(subQuery)))));
    }

    @Test
    void testConstructors_RejectStructuralConditionValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn("id", Arrays.asList(new Where(Filters.eq("active", true)))));
        assertThrows(IllegalArgumentException.class,
                () -> new NotIn(Arrays.asList("id", "tenant"), Arrays.asList(Arrays.asList(1, new On("a.tenant", "b.tenant")))));
    }

    @Test
    void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("status", emptyList);
        });
    }

    @Test
    void testConstructorWithList() {
        List<String> values = Arrays.asList("deleted", "archived", "inactive");
        NotIn notIn = Filters.notIn("status", values);

        assertNotNull(notIn);
        assertEquals("status", notIn.propName());
        assertEquals(values, notIn.values());
        assertEquals(Operator.NOT_IN, notIn.operator());
    }

    @Test
    void testConstructorWithNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("status", (List<String>) null);
        });
    }

    @Test
    void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        NotIn notIn = Filters.notIn("userId", values);

        assertEquals("userId", notIn.propName());
        assertEquals(5, notIn.values().size());
        assertTrue(notIn.values().containsAll(values));
    }

    @Test
    void testEquals() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");

        NotIn notIn1 = Filters.notIn("type", values1);
        NotIn notIn2 = Filters.notIn("type", values2);
        NotIn notIn3 = Filters.notIn("type", values3);
        NotIn notIn4 = Filters.notIn("category", values1);

        assertTrue(notIn1.equals(notIn1));
        assertTrue(notIn1.equals(notIn2));
        assertFalse(notIn1.equals(notIn3));
        assertFalse(notIn1.equals(notIn4));
        assertFalse(notIn1.equals(null));
        assertFalse(notIn1.equals("not a NotIn"));
    }

    @Test
    void testEquals_DifferentPropName() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("state", Arrays.asList("deleted"));

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentType() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        String other = "not a NotIn";
        assertNotEquals(condition, other);
    }

    @Test
    void testEquals_DifferentValues() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_EqualObjects() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted", "archived"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted", "archived"));

        assertEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted"));
        assertEquals(condition, condition);
    }

    @Test
    void testGetPropName() {
        NotIn condition = new NotIn("status", Arrays.asList("inactive"));
        assertEquals("status", condition.propName());
    }

    @Test
    void testGetValues() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        NotIn condition = new NotIn("id", values);

        List<?> result = condition.values();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testHashCode() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");

        NotIn notIn1 = Filters.notIn("type", values1);
        NotIn notIn2 = Filters.notIn("type", values2);
        NotIn notIn3 = Filters.notIn("type", values3);
        NotIn notIn4 = Filters.notIn("category", values1);

        assertEquals(notIn1.hashCode(), notIn2.hashCode());
        assertNotEquals(notIn1.hashCode(), notIn3.hashCode());
        assertNotEquals(notIn1.hashCode(), notIn4.hashCode());
    }

    @Test
    void testHashCode_Different() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("archived"));

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_Equal() {
        NotIn c1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn c2 = new NotIn("status", Arrays.asList("deleted"));

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testLargeValueSet() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        NotIn notIn = Filters.notIn("id", values);

        assertEquals(1000, notIn.values().size());
        assertEquals(1000, notIn.parameters().size());
    }

    @Test
    void testMultiColumn_FromMapAndBeanRows() {
        Map<String, Object> mapRow = N.asMap("firstName", "John", "lastName", "Doe");
        Account beanRow = new Account().setFirstName("Jane").setLastName("Roe");

        NotIn condition = new NotIn(Arrays.asList("firstName", "lastName"), Arrays.asList(mapRow, beanRow));

        assertEquals("(first_name, last_name) NOT IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.SNAKE_CASE));
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), condition.parameters());
    }

    @Test
    void testMultiColumn_RejectsTupleSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1))));
    }

    @Test
    void testMultiColumn_RejectsUnsupportedRowType() {
        assertThrows(IllegalArgumentException.class, () -> new NotIn(Arrays.asList("a", "b"), Arrays.asList(42)));
    }

    // --- Row value constructor NOT IN ---

    @Test
    void testMultiColumn_ToStringAndParameters() {
        NotIn condition = new NotIn(Arrays.asList("first_name", "last_name"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));

        assertEquals("(first_name, last_name) NOT IN (('John', 'Doe'), ('Jane', 'Roe'))", condition.toSql(NamingPolicy.NO_CHANGE));
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), condition.parameters());
        assertEquals(Arrays.asList("first_name", "last_name"), new ArrayList<>(condition.propNames()));
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    void testNot() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        NotIn cond1 = new NotIn("status", Arrays.asList("deleted"));
        NotIn cond2 = new NotIn("status", Arrays.asList("archived"));
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        List<String> values = Arrays.asList("deleted", "archived");
        NotIn condition = new NotIn("status", values);

        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
        assertEquals("deleted", params.get(0));
        assertEquals("archived", params.get(1));
    }

    @Test
    void testRowValueConstructor_AllowsSinglePropName() {
        NotIn condition = new NotIn(Arrays.asList("a"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));

        assertEquals(Arrays.asList("a"), new ArrayList<>(condition.propNames()));
        assertEquals("a", condition.propName());
        assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2)), condition.values());
        assertEquals(Arrays.asList(1, 2), condition.parameters());
        assertEquals("(a) NOT IN ((1), (2))", condition.toSql(NamingPolicy.NO_CHANGE));

        NotIn scalar = new NotIn("a", Arrays.asList(1, 2));
        assertNotEquals(scalar, condition);
        assertNotEquals(scalar.hashCode(), condition.hashCode());
    }

    @Test
    void testToString() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn notIn = Filters.notIn("category", values);

        String result = notIn.toString();
        assertTrue(result.contains("category"));
        assertTrue(result.contains("NOT IN"));
        assertTrue(result.contains("category NOT IN ('A', 'B', 'C')"));
    }

    @Test
    void testToString_NoChange() {
        NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
        String result = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("status"));
        assertTrue(result.contains("NOT IN"));
        assertTrue(result.contains("deleted"));
        assertTrue(result.contains("archived"));
    }

    @Test
    void testToString_SnakeCase() {
        NotIn condition = new NotIn("orderStatus", Arrays.asList("deleted"));
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("order_status"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        List<String> values = Arrays.asList("active", "pending");
        NotIn notIn = Filters.notIn("user_status", values);

        String result = notIn.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("USER_STATUS"));
        assertTrue(result.contains("NOT IN"));
        assertTrue(result.contains("USER_STATUS NOT IN ('active', 'pending')"));
    }

    @Test
    void testUseCaseScenario_ExcludeCategories() {
        // Exclude specific product categories
        List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
        NotIn notIn = new NotIn("category", excludedCategories);

        assertEquals(3, notIn.parameters().size());
        assertTrue(notIn.toSql(NamingPolicy.NO_CHANGE).contains("category"));
    }

    @Test
    void testUseCaseScenario_ExcludeDepartments() {
        // Exclude specific department IDs
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn deptCondition = new NotIn("department_id", excludedDepts);

        assertEquals(3, deptCondition.parameters().size());
    }

    @Test
    void testUseCaseScenario_ExcludeInactiveStatuses() {
        // Exclude inactive statuses
        List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", inactiveStatuses);

        assertEquals(3, condition.parameters().size());
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    void testUseCaseScenario_ExcludeTestUsers() {
        // Exclude test users
        List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
        NotIn emailCondition = new NotIn("email", testEmails);

        assertEquals(2, emailCondition.parameters().size());
    }

    @Test
    void testWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        NotIn notIn = Filters.notIn("mixedField", values);

        assertEquals(4, notIn.values().size());
        assertTrue(notIn.values().contains(1));
        assertTrue(notIn.values().contains("two"));
        assertTrue(notIn.values().contains(3.0));
        assertTrue(notIn.values().contains(true));
    }

    // Characterisation (R16-01/R16-02): positional rejection messages are unchanged and an all-scalar NOT IN
    // list exposes its membership values as the memoized, unmodifiable parameter list.
    @Test
    void testPositionalRejectionMessagesAndScalarParameterReuse() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new NotIn("id", Arrays.asList(1, null)));
        assertEquals("values must not contain null elements; null found at index 1", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new NotIn("id", Arrays.asList(Filters.eq("a", 1), 2)));
        assertTrue(ex.getMessage().startsWith("values[0] must be a literal value, SqlExpression, scalar SubQuery"), ex.getMessage());

        final NotIn condition = new NotIn("id", Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), condition.parameters());
        assertEquals(condition.values(), condition.parameters());
        assertTrue(condition.parameters() == condition.parameters());
        assertThrows(UnsupportedOperationException.class, () -> condition.parameters().add(4));
        assertEquals("id NOT IN (1, 2, 3)", condition.toString());
    }
}
