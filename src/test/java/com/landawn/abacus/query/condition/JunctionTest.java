package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Filters.CF;
import com.landawn.abacus.util.NamingPolicy;

public class JunctionTest extends TestBase {

    @Test
    public void testConstructorWithOperatorAndConditions() {
        Condition cond1 = CF.eq("status", "active");
        Condition cond2 = CF.gt("age", 18);

        Junction junction = new Junction(Operator.AND, cond1, cond2);

        Assertions.assertNotNull(junction);
        Assertions.assertEquals(Operator.AND, junction.getOperator());
        Assertions.assertEquals(2, junction.getConditions().size());
        Assertions.assertTrue(junction.getConditions().contains(cond1));
        Assertions.assertTrue(junction.getConditions().contains(cond2));
    }

    @Test
    public void testConstructorWithOperatorAndCollection() {
        List<Condition> conditions = Arrays.asList(CF.eq("name", "John"), CF.lt("salary", 50000), CF.isNotNull("email"));

        Junction junction = new Junction(Operator.OR, conditions);

        Assertions.assertNotNull(junction);
        Assertions.assertEquals(Operator.OR, junction.getOperator());
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
        Junction junction = new Junction(Operator.AND, CF.eq("x", 1));
        List<Condition> conditions = junction.getConditions();

        Assertions.assertNotNull(conditions);
        Assertions.assertEquals(1, conditions.size());
    }

    @Test
    public void testSet() {
        Junction junction = new Junction(Operator.AND, CF.eq("old", 1));

        junction.set(CF.eq("new1", 2), CF.eq("new2", 3));

        Assertions.assertEquals(2, junction.getConditions().size());
        String result = junction.toString();
        Assertions.assertTrue(result.contains("new1"));
        Assertions.assertTrue(result.contains("new2"));
        Assertions.assertFalse(result.contains("old"));
    }

    @Test
    public void testSetWithCollection() {
        Junction junction = new Junction(Operator.OR);
        List<Condition> newConditions = Arrays.asList(CF.gt("price", 100), CF.lt("price", 500));

        junction.set(newConditions);

        Assertions.assertEquals(2, junction.getConditions().size());
    }

    @Test
    public void testAdd() {
        Junction junction = new Junction(Operator.AND);

        junction.add(CF.eq("status", "active"));
        Assertions.assertEquals(1, junction.getConditions().size());

        junction.add(CF.gt("score", 80), CF.lt("score", 100));
        Assertions.assertEquals(3, junction.getConditions().size());
    }

    @Test
    public void testAddWithCollection() {
        Junction junction = new Junction(Operator.OR);
        List<Condition> conditions = Arrays.asList(CF.eq("type", "A"), CF.eq("type", "B"));

        junction.add(conditions);

        Assertions.assertEquals(2, junction.getConditions().size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove() {
        Condition cond1 = CF.eq("x", 1);
        Condition cond2 = CF.eq("y", 2);
        Junction junction = new Junction(Operator.AND, cond1, cond2);

        junction.remove(cond1);

        Assertions.assertEquals(1, junction.getConditions().size());
        Assertions.assertFalse(junction.getConditions().contains(cond1));
        Assertions.assertTrue(junction.getConditions().contains(cond2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemoveWithCollection() {
        Condition cond1 = CF.eq("a", 1);
        Condition cond2 = CF.eq("b", 2);
        Condition cond3 = CF.eq("c", 3);
        Junction junction = new Junction(Operator.OR, cond1, cond2, cond3);

        junction.remove(Arrays.asList(cond1, cond3));

        Assertions.assertEquals(1, junction.getConditions().size());
        Assertions.assertTrue(junction.getConditions().contains(cond2));
    }

    @Test
    public void testClear() {
        Junction junction = new Junction(Operator.AND, CF.eq("x", 1), CF.eq("y", 2));

        junction.clear();

        Assertions.assertEquals(0, junction.getConditions().size());
    }

    @Test
    public void testGetParameters() {
        Junction junction = new Junction(Operator.AND, CF.eq("name", "John"), CF.between("age", 20, 30), CF.in("status", Arrays.asList("A", "B")));

        List<Object> params = junction.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(5, params.size()); // "John", 20, 30, "A", "B"
        Assertions.assertTrue(params.contains("John"));
        Assertions.assertTrue(params.contains(20));
        Assertions.assertTrue(params.contains(30));
    }

    @Test
    public void testClearParameters() {
        Junction junction = new Junction(Operator.OR, CF.eq("x", 10), CF.gt("y", 20));

        junction.clearParameters();

        List<Object> params = junction.getParameters();
        // Parameters should be cleared
        for (Object param : params) {
            Assertions.assertNull(param);
        }
    }

    @Test
    public void testCopy() {
        Junction original = new Junction(Operator.AND, CF.eq("status", "active"), CF.gt("age", 21));

        Junction copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());

        // Verify deep copy of conditions
        for (int i = 0; i < original.getConditions().size(); i++) {
            Assertions.assertNotSame(original.getConditions().get(i), copy.getConditions().get(i));
        }
    }

    @Test
    public void testToString() {
        Junction junction = new Junction(Operator.AND, CF.eq("active", true), CF.isNotNull("email"));

        String result = junction.toString();

        Assertions.assertTrue(result.contains("AND"));
        Assertions.assertTrue(result.contains("active"));
        Assertions.assertTrue(result.contains("email"));
        Assertions.assertTrue(result.startsWith("("));
        Assertions.assertTrue(result.endsWith(")"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Junction junction = new Junction(Operator.OR, CF.eq("firstName", "John"), CF.eq("lastName", "Doe"));

        String result = junction.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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
        Junction junction1 = new Junction(Operator.AND, CF.eq("x", 1));
        Junction junction2 = new Junction(Operator.AND, CF.eq("x", 1));
        Junction junction3 = new Junction(Operator.OR, CF.eq("x", 1));

        Assertions.assertEquals(junction1.hashCode(), junction2.hashCode());
        Assertions.assertNotEquals(junction1.hashCode(), junction3.hashCode());
    }

    @Test
    public void testEquals() {
        Junction junction1 = new Junction(Operator.AND, CF.eq("x", 1));
        Junction junction2 = new Junction(Operator.AND, CF.eq("x", 1));
        Junction junction3 = new Junction(Operator.OR, CF.eq("x", 1));
        Junction junction4 = new Junction(Operator.AND, CF.eq("y", 1));

        Assertions.assertEquals(junction1, junction1);
        Assertions.assertEquals(junction1, junction2);
        Assertions.assertNotEquals(junction1, junction3);
        Assertions.assertNotEquals(junction1, junction4);
        Assertions.assertNotEquals(junction1, null);
        Assertions.assertNotEquals(junction1, "string");
    }

    @Test
    public void testNestedJunctions() {
        Junction inner1 = new Junction(Operator.AND, CF.eq("a", 1), CF.eq("b", 2));
        Junction inner2 = new Junction(Operator.AND, CF.eq("c", 3), CF.eq("d", 4));
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