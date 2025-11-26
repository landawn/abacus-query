package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class InSubQueryTest extends TestBase {

    @Test
    public void testConstructorWithSinglePropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM employees WHERE role = 'manager'");
        InSubQuery condition = new InSubQuery("manager_id", subQuery);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("manager_id", condition.getPropName());
        Assertions.assertNull(condition.getPropNames());
        Assertions.assertEquals(Operator.IN, condition.getOperator());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("department_id", "location_id");
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM valid_assignments");
        InSubQuery condition = new InSubQuery(propNames, subQuery);

        Assertions.assertNotNull(condition);
        Assertions.assertNull(condition.getPropName());
        Assertions.assertEquals(propNames, condition.getPropNames());
        Assertions.assertEquals(Operator.IN, condition.getOperator());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testGetPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        InSubQuery condition = new InSubQuery("user_id", subQuery);

        Assertions.assertEquals("user_id", condition.getPropName());
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
    @SuppressWarnings("deprecation")
    public void testSetSubQuery() {
        SubQuery originalSubQuery = Filters.subQuery("SELECT id FROM table1");
        SubQuery newSubQuery = Filters.subQuery("SELECT id FROM table2");
        InSubQuery condition = new InSubQuery("id", originalSubQuery);

        condition.setSubQuery(newSubQuery);

        Assertions.assertEquals(newSubQuery, condition.getSubQuery());
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
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table WHERE value = ?");
        InSubQuery condition = new InSubQuery("id", subQuery);

        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = ?");
        InSubQuery original = new InSubQuery("user_id", subQuery);
        InSubQuery copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getSubQuery(), copy.getSubQuery());
        Assertions.assertEquals(original.getSubQuery().toString(), copy.getSubQuery().toString());
    }

    @Test
    public void testCopyWithMultiplePropNames() {
        List<String> propNames = Arrays.asList("col1", "col2");
        SubQuery subQuery = Filters.subQuery("SELECT a, b FROM table");
        InSubQuery original = new InSubQuery(propNames, subQuery);
        InSubQuery copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropNames(), copy.getPropNames());
        Assertions.assertNotSame(original.getSubQuery(), copy.getSubQuery());
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

        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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