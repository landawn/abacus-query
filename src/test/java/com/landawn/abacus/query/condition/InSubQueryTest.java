package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class InSubQueryTest extends TestBase {

    @Test
    void testAnd() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM active_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM premium_users");
        InSubQuery cond1 = new InSubQuery("userId", subQuery1);
        InSubQuery cond2 = new InSubQuery("userId", subQuery2);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor_EmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    void testConstructor_MultipleProperties() {
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, location_id FROM dept_locations WHERE active = 'Y'");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertNotNull(condition.propNames());
        assertEquals(2, condition.propNames().size());
        assertNotNull(condition.subQuery());
    }

    @Test
    void testConstructor_MultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("   ", subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("dept_id", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("dept_id", ""), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("dept_id", "   "), subQuery));
    }

    @Test
    void testConstructor_NullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery((Collection<String>) null, subQuery));
    }

    @Test
    void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("id", null));
    }

    @Test
    void testConstructor_RejectsArityMismatch() {
        SubQuery twoColumns = Filters.subQuery("users", Arrays.asList("id", "name"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("user_id", twoColumns));

        SubQuery oneColumn = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("user_id", "user_name"), oneColumn));
    }

    @Test
    void testConstructor_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", subQuery);

        assertEquals("customer_id", condition.propNames().iterator().next());
        assertEquals(1, condition.propNames().size());
        assertNotNull(condition.subQuery());
        assertEquals(Operator.IN, condition.operator());
    }

    @Test
    void testConstructorWithEmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");

        assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery(new ArrayList<>(), subQuery);
        });
    }

    @Test
    void testConstructorWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM valid_assignments");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        assertNotNull(condition);
        assertEquals(propNames, condition.propNames());
        assertEquals(Operator.IN, condition.operator());
        assertEquals(subQuery, condition.subQuery());
    }

    @Test
    void testConstructorWithMultiplePropNamesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM valid_assignments");

        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("   ", subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("department_id", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("department_id", ""), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("department_id", "   "), subQuery));
    }

    @Test
    void testConstructorWithNullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");

        assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery((Collection<String>) null, subQuery);
        });
    }

    @Test
    void testConstructorWithNullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery("field", null);
        });
    }

    @Test
    void testConstructorWithSinglePropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM employees WHERE role = 'manager'");
        InSubQuery condition = new InSubQuery("manager_id", subQuery);

        assertNotNull(condition);
        assertEquals("manager_id", condition.propNames().iterator().next());
        assertEquals(1, condition.propNames().size());
        assertEquals(Operator.IN, condition.operator());
        assertEquals(subQuery, condition.subQuery());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table");

        InSubQuery condition1 = new InSubQuery("field", subQuery1);
        InSubQuery condition2 = new InSubQuery("field", subQuery2);
        InSubQuery condition3 = new InSubQuery("other", subQuery1);

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("customer_id", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentSubQuery() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    void testGetPropNames() {
        List<String> propNames = Arrays.asList("city", "state");
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        assertEquals(propNames, condition.propNames());
    }

    @Test
    void testGetPropNames_MultipleProperties() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        Collection<String> propNames = condition.propNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    void testGetPropNames_MultipleProperties_Unmodifiable() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.propNames().add("region_id"));
    }

    @Test
    void testGetPropNames_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(1, condition.propNames().size());
        assertEquals("user_id", condition.propNames().iterator().next());
    }

    @Test
    void testGetPropNamesForSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(1, condition.propNames().size());
        assertEquals("user_id", condition.propNames().iterator().next());
    }

    @Test
    void testGetSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        SubQuery result = condition.subQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        InSubQuery condition1 = new InSubQuery("field", subQuery1);
        InSubQuery condition2 = new InSubQuery("field", subQuery2);
        InSubQuery condition3 = new InSubQuery("other", subQuery1);
        InSubQuery condition4 = new InSubQuery("field", subQuery3);

        assertEquals(condition1.hashCode(), condition2.hashCode());
        assertNotEquals(condition1.hashCode(), condition3.hashCode());
        assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    void testHashCode_Different() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testMultiplePropNamesAreDefensivelyCopiedAndUnmodifiable() {
        List<String> propNames = new ArrayList<>(Arrays.asList("city", "state"));
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        propNames.add("country");

        assertEquals(Arrays.asList("city", "state"), condition.propNames().stream().toList());
        assertThrows(UnsupportedOperationException.class, () -> condition.propNames().add("zip"));
    }

    @Test
    void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM premium_users");
        InSubQuery condition = new InSubQuery("userId", subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM active_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM pending_users");
        InSubQuery cond1 = new InSubQuery("userId", subQuery1);
        InSubQuery cond2 = new InSubQuery("userId", subQuery2);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.parameters();
        assertNotNull(params);
    }

    @Test
    void testParameters_WithSubQueryParams() {
        Equal statusCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), statusCondition);
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.parameters();
        assertNotNull(params);
    }

    @Test
    void testParametersWithMultipleValues() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM products WHERE price BETWEEN ? AND ?", Arrays.asList(10, 20));
        InSubQuery condition = new InSubQuery("product_id", subQuery);

        List<Object> params = condition.parameters();

        assertEquals(Arrays.asList(10, 20), params);
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        String result = condition.toString();

        assertTrue(result.contains("user_id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("SELECT id FROM users"));
    }

    @Test
    void testToString_MultipleProperties_NoChange() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("dept_id"));
        assertTrue(result.contains("loc_id"));
        assertTrue(result.contains("IN"));
    }

    @Test
    void testToString_SingleProperty_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("user_id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    void testToString_SnakeCase() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("userId", subQuery);

        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_id"));
    }

    @Test
    void testToStringWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("city", "state");
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM allowed_locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        String result = condition.toString();

        assertTrue(result.contains("(city, state)"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("SELECT city, state"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        InSubQuery condition = new InSubQuery("userId", subQuery);

        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("USER_ID"));
        assertTrue(result.contains("IN"));
    }

    @Test
    void testUseCaseScenario_ActiveCategories() {
        // Find all products in active categories
        SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
        InSubQuery condition = new InSubQuery("category_id", activeCategories);

        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("category_id"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    void testUseCaseScenario_ComplexSubQuery() {
        // Using structured subquery
        Condition activeCondition = new Equal("active", true);
        SubQuery subQuery = Filters.subQuery("categories", Arrays.asList("id"), activeCondition);
        InSubQuery condition = new InSubQuery("category_id", subQuery);

        assertNotNull(condition.parameters());
    }

    @Test
    void testUseCaseScenario_MultiColumnAssignments() {
        // Find employees in specific department/location combinations
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery validAssignments = Filters.subQuery("SELECT dept_id, location_id FROM allowed_assignments");
        InSubQuery multiColumn = new InSubQuery(columns, validAssignments);

        assertEquals(2, multiColumn.propNames().size());
    }

    @Test
    void testUseCaseScenario_PremiumCustomers() {
        // Find orders from premium customers
        SubQuery premiumCustomers = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);

        assertNotNull(condition.subQuery());
        assertTrue(condition.toSql(NamingPolicy.NO_CHANGE).contains("customer_id"));
    }
}
