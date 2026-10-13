package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class NotInSubQueryTest extends TestBase {

    @Test
    void testAnd() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM deleted_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM suspended_users");
        NotInSubQuery cond1 = new NotInSubQuery("userId", subQuery1);
        NotInSubQuery cond2 = new NotInSubQuery("userId", subQuery2);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor_EmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    void testConstructor_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        assertNotNull(condition.propNames());
        assertEquals(2, condition.propNames().size());
    }

    @Test
    void testConstructor_MultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("   ", subQuery));
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", ""), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", "   "), subQuery));
    }

    @Test
    void testConstructor_NullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery((Collection<String>) null, subQuery));
    }

    @Test
    void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("id", null));
    }

    @Test
    void testConstructor_RejectsArityMismatch() {
        SubQuery twoColumns = Filters.subQuery("users", Arrays.asList("id", "name"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("userId", twoColumns));

        SubQuery oneColumn = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", "lastName"), oneColumn));
    }

    @Test
    void testConstructor_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals("userId", condition.propNames().iterator().next());
        assertEquals(1, condition.propNames().size());
        assertNotNull(condition.subQuery());
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    void testConstructorWithEmptyPropNames() {
        List<String> propNames = Arrays.asList();
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn(propNames, subQuery);
        });
    }

    @Test
    void testConstructorWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        assertEquals(propNames, condition.propNames());
        assertEquals(subQuery, condition.subQuery());
    }

    @Test
    void testConstructorWithMultiplePropertiesDefensiveCopy() {
        List<String> propNames = new ArrayList<>(Arrays.asList("firstName", "lastName"));
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        propNames.add("middleName");

        assertEquals(Arrays.asList("firstName", "lastName"), condition.propNames().stream().toList());
    }

    @Test
    void testConstructorWithMultiplePropertiesNullSubQuery() {
        List<String> propNames = Arrays.asList("firstName", "lastName");

        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn(propNames, (SubQuery) null);
        });
    }

    @Test
    void testConstructorWithMultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");

        assertThrows(IllegalArgumentException.class, () -> Filters.notIn("   ", subQuery));
        assertThrows(IllegalArgumentException.class, () -> Filters.notIn(Arrays.asList("firstName", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> Filters.notIn(Arrays.asList("firstName", ""), subQuery));
        assertThrows(IllegalArgumentException.class, () -> Filters.notIn(Arrays.asList("firstName", "   "), subQuery));
    }

    @Test
    void testConstructorWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        assertEquals("userId", condition.propNames().iterator().next());
        assertEquals(1, condition.propNames().size());
        assertEquals(subQuery, condition.subQuery());
    }

    @Test
    void testConstructorWithSinglePropertyNullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notIn("userId", (SubQuery) null);
        });
    }

    @Test
    void testEquals_DifferentPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("customerId", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    void testEqualsWithDifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        assertFalse(condition.equals("not a NotInSubQuery"));
    }

    @Test
    void testEqualsWithMultipleProperties() {
        List<String> propNames1 = Arrays.asList("firstName", "lastName");
        List<String> propNames2 = Arrays.asList("firstName", "lastName");
        List<String> propNames3 = Arrays.asList("firstName", "middleName");

        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");

        NotInSubQuery condition1 = Filters.notIn(propNames1, subQuery);
        NotInSubQuery condition2 = Filters.notIn(propNames2, subQuery);
        NotInSubQuery condition3 = Filters.notIn(propNames3, subQuery);

        assertTrue(condition1.equals(condition2));
        assertFalse(condition1.equals(condition3));
    }

    @Test
    void testEqualsWithNull() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        assertFalse(condition.equals(null));
    }

    @Test
    void testEqualsWithSameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        assertTrue(condition.equals(condition));
    }

    @Test
    void testEqualsWithSingleProperty() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");

        NotInSubQuery condition1 = Filters.notIn("userId", subQuery1);
        NotInSubQuery condition2 = Filters.notIn("userId", subQuery2);
        NotInSubQuery condition3 = Filters.notIn("customerId", subQuery1);

        assertTrue(condition1.equals(condition2));
        assertFalse(condition1.equals(condition3));
    }

    @Test
    void testGetPropNames_MultipleProperties() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery subQuery = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        Collection<String> propNames = condition.propNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    void testGetPropNames_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", subQuery);

        assertEquals(1, condition.propNames().size());
        assertEquals("itemId", condition.propNames().iterator().next());
    }

    @Test
    void testGetPropNames_Unmodifiable() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery subQuery = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.propNames().add("zip"));
    }

    @Test
    void testGetPropNamesIsUnmodifiable() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.propNames().add("middleName"));
    }

    @Test
    void testGetSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        SubQuery result = condition.subQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    void testHashCode_Equal() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCodeWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition1 = Filters.notIn(propNames, subQuery);
        NotInSubQuery condition2 = Filters.notIn(propNames, subQuery);

        assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    void testHashCodeWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition1 = Filters.notIn("userId", subQuery);
        NotInSubQuery condition2 = Filters.notIn("userId", subQuery);

        assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    @Test
    void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM deleted_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM archived_users");
        NotInSubQuery cond1 = new NotInSubQuery("userId", subQuery1);
        NotInSubQuery cond2 = new NotInSubQuery("userId", subQuery2);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        List<Object> params = condition.parameters();
        assertNotNull(params);
    }

    @Test
    void testToString_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("lastName"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    void testToString_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userId"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    void testToStringWithMultipleProperties() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        String result = condition.toSql(NamingPolicy.CAMEL_CASE);
        assertEquals("(firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)", result);
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("user_id", subQuery);

        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertEquals("USER_ID NOT IN (SELECT id FROM users)", result);
    }

    @Test
    void testToStringWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        String result = condition.toSql(NamingPolicy.CAMEL_CASE);
        assertEquals("userId NOT IN (SELECT id FROM inactive_users)", result);
    }

    @Test
    void testUseCaseScenario_ExcludeDeletedItems() {
        SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);

        assertTrue(condition.toSql(NamingPolicy.NO_CHANGE).contains("itemId"));
    }

    @Test
    void testUseCaseScenario_ExcludeRestrictedLocations() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, restricted);

        assertEquals(2, condition.propNames().size());
    }
}
