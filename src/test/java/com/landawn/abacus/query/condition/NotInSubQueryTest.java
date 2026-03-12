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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class NotInSubQuery2025Test extends TestBase {

    @Test
    public void testConstructor_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals("userId", condition.getPropNames().iterator().next());
        assertEquals(1, condition.getPropNames().size());
        assertNotNull(condition.getSubQuery());
        assertEquals(Operator.NOT_IN, condition.operator());
    }

    @Test
    public void testConstructor_NullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("id", null));
    }

    @Test
    public void testConstructor_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        assertNotNull(condition.getPropNames());
        assertEquals(2, condition.getPropNames().size());
    }

    @Test
    public void testConstructor_RejectsArityMismatch() {
        SubQuery twoColumns = Filters.subQuery("users", Arrays.asList("id", "name"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("userId", twoColumns));

        SubQuery oneColumn = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", "lastName"), oneColumn));
    }

    @Test
    public void testConstructor_EmptyPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList(), subQuery));
    }

    @Test
    public void testConstructor_MultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", null), subQuery));
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(Arrays.asList("firstName", ""), subQuery));
    }

    @Test
    public void testConstructor_NullPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery((Collection<String>) null, subQuery));
    }

    @Test
    public void testGetPropNames_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", subQuery);

        assertEquals(1, condition.getPropNames().size());
        assertEquals("itemId", condition.getPropNames().iterator().next());
    }

    @Test
    public void testGetPropNames_MultipleProperties() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery subQuery = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        Collection<String> propNames = condition.getPropNames();
        assertNotNull(propNames);
        assertEquals(2, propNames.size());
    }

    @Test
    public void testGetPropNames_Unmodifiable() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery subQuery = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.getPropNames().add("zip"));
    }

    @Test
    public void testGetSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        SubQuery result = condition.getSubQuery();
        assertNotNull(result);
        assertEquals(subQuery, result);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Equal statusCondition = new Equal("status", "deleted");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), statusCondition);
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        condition.clearParameters();
        assertNotNull(condition.getSubQuery());
    }

    @Test
    public void testToString_SingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userId"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testToString_MultipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = new NotInSubQuery(props, subQuery);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("lastName"));
        assertTrue(result.contains("NOT IN"));
    }

    @Test
    public void testHashCode_Equal() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("userId", subQuery);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery c1 = new NotInSubQuery("userId", subQuery);
        NotInSubQuery c2 = new NotInSubQuery("customerId", subQuery);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);

        assertNotEquals(null, condition);
    }

    @Test
    public void testUseCaseScenario_ExcludeDeletedItems() {
        SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);

        assertTrue(condition.toString(NamingPolicy.NO_CHANGE).contains("itemId"));
    }

    @Test
    public void testUseCaseScenario_ExcludeRestrictedLocations() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, restricted);

        assertEquals(2, condition.getPropNames().size());
    }

    @Test
    public void testAnd() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM deleted_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM suspended_users");
        NotInSubQuery cond1 = new NotInSubQuery("userId", subQuery1);
        NotInSubQuery cond2 = new NotInSubQuery("userId", subQuery2);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM deleted_users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM archived_users");
        NotInSubQuery cond1 = new NotInSubQuery("userId", subQuery1);
        NotInSubQuery cond2 = new NotInSubQuery("userId", subQuery2);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class NotInSubQueryTest extends TestBase {

    @Test
    public void testConstructorWithSingleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        NotInSubQuery condition = Filters.notIn("userId", subQuery);

        Assertions.assertEquals("userId", condition.getPropNames().iterator().next());
        Assertions.assertEquals(1, condition.getPropNames().size());
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

        Assertions.assertEquals(propNames, condition.getPropNames());
        Assertions.assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testConstructorWithMultiplePropertiesDefensiveCopy() {
        List<String> propNames = new ArrayList<>(Arrays.asList("firstName", "lastName"));
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        propNames.add("middleName");

        Assertions.assertEquals(Arrays.asList("firstName", "lastName"), condition.getPropNames().stream().toList());
    }

    @Test
    public void testConstructorWithMultiplePropertiesRejectsInvalidElements() {
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.notIn(Arrays.asList("firstName", null), subQuery));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.notIn(Arrays.asList("firstName", ""), subQuery));
    }

    @Test
    public void testGetPropNamesIsUnmodifiable() {
        List<String> propNames = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition = Filters.notIn(propNames, subQuery);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> condition.getPropNames().add("middleName"));
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
