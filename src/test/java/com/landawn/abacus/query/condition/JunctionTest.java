package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class JunctionTest extends TestBase {

    @Test
    public void testConstructorWithOperatorAndConditions() {
        Condition cond1 = Filters.eq("status", "active");
        Condition cond2 = Filters.gt("age", 18);

        Junction junction = new Junction(Operator.AND, cond1, cond2);

        Assertions.assertNotNull(junction);
        Assertions.assertEquals(Operator.AND, junction.operator());
        Assertions.assertEquals(2, junction.getConditions().size());
        Assertions.assertTrue(junction.getConditions().contains(cond1));
        Assertions.assertTrue(junction.getConditions().contains(cond2));
    }

    @Test
    public void testConstructorWithOperatorAndCollection() {
        List<Condition> conditions = Arrays.asList(Filters.eq("name", "John"), Filters.lt("salary", 50000), Filters.isNotNull("email"));

        Junction junction = new Junction(Operator.OR, conditions);

        Assertions.assertNotNull(junction);
        Assertions.assertEquals(Operator.OR, junction.operator());
        Assertions.assertEquals(3, junction.getConditions().size());
    }

    @Test
    public void testConstructorWithEmptyConditions() {
        Junction junction = new Junction(Operator.AND);

        Assertions.assertNotNull(junction);
        Assertions.assertEquals(0, junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Junction junction = new Junction(Operator.AND, Filters.eq("x", 1));
        List<Condition> conditions = junction.getConditions();

        Assertions.assertNotNull(conditions);
        Assertions.assertEquals(1, conditions.size());
    }

    @Test
    public void testAdd() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddWithCollection() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove() {
        // Commented out: Junction.remove(...) APIs are currently commented out.
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemoveWithCollection() {
        // Commented out: Junction.remove(...) APIs are currently commented out.
    }

    @Test
    public void testClear() {
        // Commented out: Junction.clear() API is currently commented out.
    }

    @Test
    public void testGetParameters() {
        Junction junction = new Junction(Operator.AND, Filters.eq("name", "John"), Filters.between("age", 20, 30),
                Filters.in("status", Arrays.asList("A", "B")));

        List<Object> params = junction.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(5, params.size()); // "John", 20, 30, "A", "B"
        Assertions.assertTrue(params.contains("John"));
        Assertions.assertTrue(params.contains(20));
        Assertions.assertTrue(params.contains(30));
    }

    @Test
    public void testClearParameters() {
        Junction junction = new Junction(Operator.OR, Filters.eq("x", 10), Filters.gt("y", 20));

        junction.clearParameters();

        List<Object> params = junction.getParameters();
        // Parameters should be cleared
        for (Object param : params) {
            Assertions.assertNull(param);
        }
    }

    @Test
    public void testToString() {
        Junction junction = new Junction(Operator.AND, Filters.eq("active", true), Filters.isNotNull("email"));

        String result = junction.toString();

        Assertions.assertTrue(result.contains("AND"));
        Assertions.assertTrue(result.contains("active"));
        Assertions.assertTrue(result.contains("email"));
        Assertions.assertTrue(result.startsWith("("));
        Assertions.assertTrue(result.endsWith(")"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Junction junction = new Junction(Operator.OR, Filters.eq("firstName", "John"), Filters.eq("lastName", "Doe"));

        String result = junction.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("FIRST_NAME"));
        Assertions.assertTrue(result.contains("LAST_NAME"));
        Assertions.assertTrue(result.contains("OR"));
    }

    @Test
    public void testToStringWithEmptyConditions() {
        Junction junction = new Junction(Operator.AND);
        String result = junction.toString();

        Assertions.assertEquals("", result);
    }

    @Test
    public void testHashCode() {
        Junction junction1 = new Junction(Operator.AND, Filters.eq("x", 1));
        Junction junction2 = new Junction(Operator.AND, Filters.eq("x", 1));
        Junction junction3 = new Junction(Operator.OR, Filters.eq("x", 1));

        Assertions.assertEquals(junction1.hashCode(), junction2.hashCode());
        Assertions.assertNotEquals(junction1.hashCode(), junction3.hashCode());
    }

    @Test
    public void testEquals() {
        Junction junction1 = new Junction(Operator.AND, Filters.eq("x", 1));
        Junction junction2 = new Junction(Operator.AND, Filters.eq("x", 1));
        Junction junction3 = new Junction(Operator.OR, Filters.eq("x", 1));
        Junction junction4 = new Junction(Operator.AND, Filters.eq("y", 1));

        Assertions.assertEquals(junction1, junction1);
        Assertions.assertEquals(junction1, junction2);
        Assertions.assertNotEquals(junction1, junction3);
        Assertions.assertNotEquals(junction1, junction4);
        Assertions.assertNotEquals(junction1, null);
        Assertions.assertNotEquals(junction1, "string");
    }

    @Test
    public void testNestedJunctions() {
        Junction inner1 = new Junction(Operator.AND, Filters.eq("a", 1), Filters.eq("b", 2));
        Junction inner2 = new Junction(Operator.AND, Filters.eq("c", 3), Filters.eq("d", 4));
        Junction outer = new Junction(Operator.OR, inner1, inner2);

        String result = outer.toString();

        Assertions.assertTrue(result.contains("OR"));
        Assertions.assertTrue(result.contains("AND"));
        Assertions.assertTrue(result.contains("a"));
        Assertions.assertTrue(result.contains("b"));
        Assertions.assertTrue(result.contains("c"));
        Assertions.assertTrue(result.contains("d"));
    }
}
