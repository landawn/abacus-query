package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class NotInSubQueryTest extends TestBase {

    @Test
    public void testConstructorWithSingleProperty() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        Assertions.assertEquals("userId", condition.getPropName());
        Assertions.assertNull(condition.getPropNames());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithSinglePropertyNullSubQuery() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.notIn("userId", (SubQuery) null);
        });
    }

    @Test
    public void testConstructorWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = CF.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = CF.notIn(propNames, subQuery);
        
        Assertions.assertNull(condition.getPropName());
        Assertions.assertEquals(propNames, condition.getPropNames());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithEmptyPropNames() {
        List<String> propNames = Arrays.asList();
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.notIn(propNames, subQuery);
        });
    }

    @Test
    public void testConstructorWithMultiplePropertiesNullSubQuery() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.notIn(propNames, (SubQuery) null);
        });
    }

    @Test
    public void testSetSubQuery() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM active_users");
        NotInSubQuery condition = CF.notIn("userId", subQuery1);
        
        condition.setSubQuery(subQuery2);
        Assertions.assertEquals(subQuery2, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = CF.subQuery("users", Arrays.asList("id"), CF.eq("active", true));
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        List<Object> params = condition.getParameters();
        Assertions.assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = CF.subQuery("users", Arrays.asList("id"), CF.eq("active", true));
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        condition.clearParameters();
        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().isEmpty() || subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = CF.subQuery("users", Arrays.asList("id"), CF.eq("active", true));
        NotInSubQuery original = CF.notIn("userId", subQuery);
        
        NotInSubQuery copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertNotSame(original.getSubQuery(), copy.getSubQuery());
        Assertions.assertEquals(original.getSubQuery(), copy.getSubQuery());
    }

    @Test
    public void testHashCodeWithSingleProperty() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition1 = CF.notIn("userId", subQuery);
        NotInSubQuery condition2 = CF.notIn("userId", subQuery);
        
        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    public void testHashCodeWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = CF.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition1 = CF.notIn(propNames, subQuery);
        NotInSubQuery condition2 = CF.notIn(propNames, subQuery);
        
        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    public void testEqualsWithSameObject() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        Assertions.assertTrue(condition.equals(condition));
    }

    @Test
    public void testEqualsWithNull() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        Assertions.assertFalse(condition.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        Assertions.assertFalse(condition.equals("not a NotInSubQuery"));
    }

    @Test
    public void testEqualsWithSingleProperty() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM users");
        
        NotInSubQuery condition1 = CF.notIn("userId", subQuery1);
        NotInSubQuery condition2 = CF.notIn("userId", subQuery2);
        NotInSubQuery condition3 = CF.notIn("customerId", subQuery1);
        
        Assertions.assertTrue(condition1.equals(condition2));
        Assertions.assertFalse(condition1.equals(condition3));
    }

    @Test
    public void testEqualsWithMultipleProperties() {
        List<String> propNames1 = Arrays.asList("firstName", "lastName");
        List<String> propNames2 = Arrays.asList("firstName", "lastName");
        List<String> propNames3 = Arrays.asList("firstName", "middleName");
        
        SubQuery subQuery = CF.subQuery("SELECT fname, lname FROM blacklist");
        
        NotInSubQuery condition1 = CF.notIn(propNames1, subQuery);
        NotInSubQuery condition2 = CF.notIn(propNames2, subQuery);
        NotInSubQuery condition3 = CF.notIn(propNames3, subQuery);
        
        Assertions.assertTrue(condition1.equals(condition2));
        Assertions.assertFalse(condition1.equals(condition3));
    }

    @Test
    public void testToStringWithSingleProperty() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = CF.notIn("userId", subQuery);
        
        String result = condition.toString(NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertEquals("userId NOT IN (SELECT id FROM inactive_users)", result);
    }

    @Test
    public void testToStringWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = CF.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = CF.notIn(propNames, subQuery);
        
        String result = condition.toString(NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertEquals("(firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        NotInSubQuery condition = CF.notIn("user_id", subQuery);
        
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("USER_ID NOT IN (SELECT id FROM users)", result);
    }
}