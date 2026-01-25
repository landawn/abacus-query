package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotInSubQueryTest extends TestBase {

    @Test
    public void testConstructorWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        Assertions.assertEquals("userId", condition.getPropName());
        Assertions.assertNull(condition.getPropNames());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithSinglePropertyNullSubQuery() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("userId", (SubQuery) null);
        });
    }

    @Test
    public void testConstructorWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        Assertions.assertNull(condition.getPropName());
        Assertions.assertEquals(propNames, condition.getPropNames());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithEmptyPropNames() {
        List<String> propNames = Arrays.asList();
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn(propNames, subQuery);
        });
    }

    @Test
    public void testConstructorWithMultiplePropertiesNullSubQuery() {
        List<String> propNames = Arrays.asList("firstName", "lastName");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn(propNames, (SubQuery) null);
        });
    }

    @Test
    public void testSetSubQuery() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM active_users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery1);

        condition.setSubQuery(subQuery2);
        Assertions.assertEquals(subQuery2, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        List<Object> params = condition.getParameters();
        Assertions.assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        condition.clearParameters();
        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().isEmpty() || subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        NotInSubQuery original = Filters.notIn("userId", subQuery);

        NotInSubQuery copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertNotSame(original.getSubQuery(), copy.getSubQuery());
        Assertions.assertEquals(original.getSubQuery(), copy.getSubQuery());
    }

    @Test
    public void testHashCodeWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition1 = Filters.notIn("userId", subQuery);
        NotInSubQuery condition2 = Filters.notIn("userId", subQuery);

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    public void testHashCodeWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition1 = Filters.notIn(propNames, subQuery);
        NotInSubQuery condition2 = Filters.notIn(propNames, subQuery);

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    public void testEqualsWithSameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        Assertions.assertTrue(condition.equals(condition));
    }

    @Test
    public void testEqualsWithNull() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        Assertions.assertFalse(condition.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        Assertions.assertFalse(condition.equals("not a NotInSubQuery"));
    }

    @Test
    public void testEqualsWithSingleProperty() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");

        NotInSubQuery condition1 = Filters.notIn("userId", subQuery1);
        NotInSubQuery condition2 = Filters.notIn("userId", subQuery2);
        NotInSubQuery condition3 = Filters.notIn("customerId", subQuery1);

        Assertions.assertTrue(condition1.equals(condition2));
        Assertions.assertFalse(condition1.equals(condition3));
    }

    @Test
    public void testEqualsWithMultipleProperties() {
        List<String> propNames1 = Arrays.asList("firstName", "lastName");
        List<String> propNames2 = Arrays.asList("firstName", "lastName");
        List<String> propNames3 = Arrays.asList("firstName", "middleName");

        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");

        NotInSubQuery condition1 = Filters.notIn(propNames1, subQuery);
        NotInSubQuery condition2 = Filters.notIn(propNames2, subQuery);
        NotInSubQuery condition3 = Filters.notIn(propNames3, subQuery);

        Assertions.assertTrue(condition1.equals(condition2));
        Assertions.assertFalse(condition1.equals(condition3));
    }

    @Test
    public void testToStringWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        String result = condition.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertEquals("userId NOT IN (SELECT id FROM inactive_users)", result);
    }

    @Test
    public void testToStringWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        String result = condition.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertEquals("(firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("user_id", subQuery);

        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertEquals("USER_ID NOT IN (SELECT id FROM users)", result);
    }
}