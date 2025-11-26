package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class NaturalJoinTest extends TestBase {

    @Test
    public void testConstructorWithEntityOnly() {
        NaturalJoin join = Filters.naturalJoin("employees");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("employees"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithEntityAndCondition() {
        Condition activeOnly = Filters.eq("status", "active");
        NaturalJoin join = Filters.naturalJoin("departments", activeOnly);

        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("departments"));
        Assertions.assertEquals(activeOnly, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("employees", "departments");
        Condition condition = Filters.gt("salary", 50000);
        NaturalJoin join = Filters.naturalJoin(tables, condition);

        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        NaturalJoin join = Filters.naturalJoin("orders");

        List<String> entities = join.getJoinEntities();
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("orders", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        GreaterThan recentOnly = Filters.gt("orderDate", "2023-01-01");
        NaturalJoin join = Filters.naturalJoin("orders", recentOnly);

        Assertions.assertEquals(recentOnly, join.getCondition());
    }

    @Test
    public void testGetParameters() {
        Equal condition = Filters.eq("active", true);
        NaturalJoin join = Filters.naturalJoin("users", condition);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(true, params.get(0));
    }

    @Test
    public void testGetParametersNoCondition() {
        NaturalJoin join = Filters.naturalJoin("employees");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Equal condition = Filters.eq("department", "IT");
        NaturalJoin join = Filters.naturalJoin("employees", condition);

        join.clearParameters();

        // Verify condition parameters are cleared
        Assertions.assertTrue(condition.getParameters().isEmpty() || condition.getParameters().get(0) == null);
    }

    @Test
    public void testClearParametersNoCondition() {
        NaturalJoin join = Filters.naturalJoin("employees");

        // Should not throw exception
        join.clearParameters();
    }

    @Test
    public void testToString() {
        NaturalJoin join = Filters.naturalJoin("departments");

        String result = join.toString();
        Assertions.assertTrue(result.contains("NATURAL JOIN"));
        Assertions.assertTrue(result.contains("departments"));
    }

    @Test
    public void testToStringWithCondition() {
        Equal condition = Filters.eq("active", true);
        NaturalJoin join = Filters.naturalJoin("users", condition);

        String result = join.toString();
        Assertions.assertTrue(result.contains("NATURAL JOIN"));
        Assertions.assertTrue(result.contains("users"));
        Assertions.assertTrue(result.contains("active"));
    }

    @Test
    public void testCopy() {
        Equal condition = Filters.eq("status", "active");
        NaturalJoin original = Filters.naturalJoin("departments", condition);

        NaturalJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithoutCondition() {
        NaturalJoin original = Filters.naturalJoin("employees");

        NaturalJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testHashCode() {
        NaturalJoin join1 = Filters.naturalJoin("employees");
        NaturalJoin join2 = Filters.naturalJoin("employees");
        NaturalJoin join3 = Filters.naturalJoin("departments");

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Equal condition = Filters.eq("active", true);

        NaturalJoin join1 = Filters.naturalJoin("employees");
        NaturalJoin join2 = Filters.naturalJoin("employees");
        NaturalJoin join3 = Filters.naturalJoin("departments");
        NaturalJoin join4 = Filters.naturalJoin("employees", condition);

        Assertions.assertTrue(join1.equals(join1));
        Assertions.assertTrue(join1.equals(join2));
        Assertions.assertFalse(join1.equals(join3));
        Assertions.assertFalse(join1.equals(join4));
        Assertions.assertFalse(join1.equals(null));
        Assertions.assertFalse(join1.equals("not a NaturalJoin"));
    }

    @Test
    public void testComplexCondition() {
        Condition complexCondition = Filters.and(Filters.eq("department", "Sales"), Filters.gt("experience", 5), Filters.like("skills", "%leadership%"));

        NaturalJoin join = Filters.naturalJoin("employees", complexCondition);

        Assertions.assertEquals(complexCondition, join.getCondition());
        Assertions.assertEquals(3, join.getParameters().size());
    }

    @Test
    public void testMultipleTablesComplexJoin() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        Condition highValue = Filters.gt("totalAmount", 1000);

        NaturalJoin join = Filters.naturalJoin(tables, highValue);

        Assertions.assertEquals(3, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(highValue, join.getCondition());
    }
}