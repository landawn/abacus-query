package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class InSubQuery2025Test extends TestBase {

    @Test
    public void testConstructor_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", subQuery);

        assertEquals("customer_id", condition.getPropNames().iterator().next());
        assertEquals(1, condition.getPropNames().size());
        assertNotNull(condition.getSubQuery());
        assertEquals(Operator.IN, condition.operator());
    }

    @Test
    public void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("id", null));
    }

    @Test
    public void testConstructor_MultipleProperties() {
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, location_id FROM dept_locations WHERE active = 'Y'");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertNotNull(condition.getPropNames());
        assertEquals(2, condition.getPropNames().size());
        assertNotNull(condition.getSubQuery());
    }

    @Test
    public void testConstructor_RejectsArityMismatch() {
        SubQuery twoColumns = Filters.subQuery("users", Arrays.asList("id", "name"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("user_id", twoColumns));

        SubQuery oneColumn = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("user_id", "user_name"), oneColumn));
    }

    @Test
    public void testConstructor_EmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    public void testConstructor_MultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("dept_id", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("dept_id", ""), subQuery));
    }

    @Test
    public void testConstructor_NullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new InSubQuery((Collection<String>) null, subQuery));
    }

    @Test
    public void testGetPropNames_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(1, condition.getPropNames().size());
        assertEquals("user_id", condition.getPropNames().iterator().next());
    }

    @Test
    public void testGetPropNames_MultipleProperties() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        Collection<String> propNames = condition.getPropNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    public void testGetPropNames_MultipleProperties_Unmodifiable() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.getPropNames().add("region_id"));
    }

    @Test
    public void testGetSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        SubQuery result = condition.getSubQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_WithSubQueryParams() {
        Equal statusCondition = new Equal("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), statusCondition);
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testToString_SingleProperty_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("user_id"));
        assertTrue(result.contains("IN"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_MultipleProperties_NoChange() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery subQuery = Filters.subQuery("SELECT department_id, location_id FROM assignments");
        InSubQuery condition = new InSubQuery(columns, subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("dept_id"));
        assertTrue(result.contains("loc_id"));
        assertTrue(result.contains("IN"));
    }

    @Test
    public void testToString_SnakeCase() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("userId", subQuery);

        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_id"));
    }

    @Test
    public void testHashCode_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("user_id", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery c1 = new InSubQuery("user_id", subQuery);
        InSubQuery c2 = new InSubQuery("customer_id", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentSubQuery() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM orders");
        InSubQuery c1 = new InSubQuery("user_id", subQuery1);
        InSubQuery c2 = new InSubQuery("user_id", subQuery2);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_PremiumCustomers() {
        // Find orders from premium customers
        SubQuery premiumCustomers = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);

        assertNotNull(condition.getSubQuery());
        assertTrue(condition.toString(NamingPolicy.NO_CHANGE).contains("customer_id"));
    }

    @Test
    public void testUseCaseScenario_MultiColumnAssignments() {
        // Find employees in specific department/location combinations
        List<String> columns = Arrays.asList("department_id", "location_id");
        SubQuery validAssignments = Filters.subQuery("SELECT dept_id, location_id FROM allowed_assignments");
        InSubQuery multiColumn = new InSubQuery(columns, validAssignments);

        assertEquals(2, multiColumn.getPropNames().size());
    }

    @Test
    public void testUseCaseScenario_ActiveCategories() {
        // Find all products in active categories
        SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
        InSubQuery condition = new InSubQuery("category_id", activeCategories);

        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("category_id"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testUseCaseScenario_ComplexSubQuery() {
        // Using structured subquery
        Condition activeCondition = new Equal("active", true);
        SubQuery subQuery = Filters.subQuery("categories", Arrays.asList("id"), activeCondition);
        InSubQuery condition = new InSubQuery("category_id", subQuery);

        assertNotNull(condition.getParameters());
    }

    @Test
    public void testAnd() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM active_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM premium_users");
        InSubQuery cond1 = new InSubQuery("userId", subQuery1);
        InSubQuery cond2 = new InSubQuery("userId", subQuery2);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM active_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM pending_users");
        InSubQuery cond1 = new InSubQuery("userId", subQuery1);
        InSubQuery cond2 = new InSubQuery("userId", subQuery2);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM premium_users");
        InSubQuery condition = new InSubQuery("userId", subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class InSubQueryTest extends TestBase {

    @Test
    public void testConstructorWithSinglePropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM employees WHERE role = 'manager'");
        InSubQuery condition = new InSubQuery("manager_id", subQuery);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("manager_id", condition.getPropNames().iterator().next());
        Assertions.assertEquals(1, condition.getPropNames().size());
        Assertions.assertEquals(Operator.IN, condition.operator());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM valid_assignments");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals(propNames, condition.getPropNames());
        Assertions.assertEquals(Operator.IN, condition.operator());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithMultiplePropNamesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM valid_assignments");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("department_id", null), subQuery));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InSubQuery(Arrays.asList("department_id", ""), subQuery));
    }

    @Test
    public void testMultiplePropNamesAreDefensivelyCopiedAndUnmodifiable() {
        List<String> propNames = new ArrayList<>(Arrays.asList("city", "state"));
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        propNames.add("country");

        Assertions.assertEquals(Arrays.asList("city", "state"), condition.getPropNames().stream().toList());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> condition.getPropNames().add("zip"));
    }

    @Test
    public void testGetPropNamesForSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        Assertions.assertEquals(1, condition.getPropNames().size());
        Assertions.assertEquals("user_id", condition.getPropNames().iterator().next());
    }

    @Test
    public void testGetPropNames() {
        List<String> propNames = Arrays.asList("city", "state");
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        Assertions.assertEquals(propNames, condition.getPropNames());
    }

    @Test
    public void testGetSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE status = ?", "active");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testGetParametersWithMultipleValues() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM products WHERE price BETWEEN ? AND ?");
        InSubQuery condition = new InSubQuery("product_id", subQuery);

        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        InSubQuery condition1 = new InSubQuery("field", subQuery1);
        InSubQuery condition2 = new InSubQuery("field", subQuery2);
        InSubQuery condition3 = new InSubQuery("other", subQuery1);
        InSubQuery condition4 = new InSubQuery("field", subQuery3);

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table");

        InSubQuery condition1 = new InSubQuery("field", subQuery1);
        InSubQuery condition2 = new InSubQuery("field", subQuery2);
        InSubQuery condition3 = new InSubQuery("other", subQuery1);

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        String result = condition.toString();

        Assertions.assertTrue(result.contains("user_id"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("SELECT id FROM users"));
    }

    @Test
    public void testToStringWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("city", "state");
        SubQuery subQuery = Filters.subQuery("SELECT city, state FROM allowed_locations");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        String result = condition.toString();

        Assertions.assertTrue(result.contains("(city, state)"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("SELECT city, state"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        InSubQuery condition = new InSubQuery("userId", subQuery);

        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("USER_ID"));
        Assertions.assertTrue(result.contains("IN"));
    }

    @Test
    public void testConstructorWithNullSubQuery() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery("field", null);
        });
    }

    @Test
    public void testConstructorWithEmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery(new ArrayList<String>(), subQuery);
        });
    }

    @Test
    public void testConstructorWithNullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new InSubQuery((Collection<String>) null, subQuery);
        });
    }
}
