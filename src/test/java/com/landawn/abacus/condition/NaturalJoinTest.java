package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;

public class NaturalJoinTest extends TestBase {

    @Test
    public void testConstructorWithEntityOnly() {
        NaturalJoin join = CF.naturalJoin("employees");
        
        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("employees"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithEntityAndCondition() {
        Condition activeOnly = CF.eq("status", "active");
        NaturalJoin join = CF.naturalJoin("departments", activeOnly);
        
        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("departments"));
        Assertions.assertEquals(activeOnly, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("employees", "departments");
        Condition condition = CF.gt("salary", 50000);
        NaturalJoin join = CF.naturalJoin(tables, condition);
        
        Assertions.assertEquals(Operator.NATURAL_JOIN, join.getOperator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        NaturalJoin join = CF.naturalJoin("orders");
        
        List<String> entities = join.getJoinEntities();
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("orders", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        GreaterThan recentOnly = CF.gt("orderDate", "2023-01-01");
        NaturalJoin join = CF.naturalJoin("orders", recentOnly);
        
        Assertions.assertEquals(recentOnly, join.getCondition());
    }

    @Test
    public void testGetParameters() {
        Equal condition = CF.eq("active", true);
        NaturalJoin join = CF.naturalJoin("users", condition);
        
        List<Object> params = join.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(true, params.get(0));
    }

    @Test
    public void testGetParametersNoCondition() {
        NaturalJoin join = CF.naturalJoin("employees");
        
        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Equal condition = CF.eq("department", "IT");
        NaturalJoin join = CF.naturalJoin("employees", condition);
        
        join.clearParameters();
        
        // Verify condition parameters are cleared
        Assertions.assertTrue(condition.getParameters().isEmpty() || 
                           condition.getParameters().get(0) == null);
    }

    @Test
    public void testClearParametersNoCondition() {
        NaturalJoin join = CF.naturalJoin("employees");
        
        // Should not throw exception
        join.clearParameters();
    }

    @Test
    public void testToString() {
        NaturalJoin join = CF.naturalJoin("departments");
        
        String result = join.toString();
        Assertions.assertTrue(result.contains("NATURAL JOIN"));
        Assertions.assertTrue(result.contains("departments"));
    }

    @Test
    public void testToStringWithCondition() {
        Equal condition = CF.eq("active", true);
        NaturalJoin join = CF.naturalJoin("users", condition);
        
        String result = join.toString();
        Assertions.assertTrue(result.contains("NATURAL JOIN"));
        Assertions.assertTrue(result.contains("users"));
        Assertions.assertTrue(result.contains("active"));
    }

    @Test
    public void testCopy() {
        Equal condition = CF.eq("status", "active");
        NaturalJoin original = CF.naturalJoin("departments", condition);
        
        NaturalJoin copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithoutCondition() {
        NaturalJoin original = CF.naturalJoin("employees");
        
        NaturalJoin copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testHashCode() {
        NaturalJoin join1 = CF.naturalJoin("employees");
        NaturalJoin join2 = CF.naturalJoin("employees");
        NaturalJoin join3 = CF.naturalJoin("departments");
        
        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Equal condition = CF.eq("active", true);
        
        NaturalJoin join1 = CF.naturalJoin("employees");
        NaturalJoin join2 = CF.naturalJoin("employees");
        NaturalJoin join3 = CF.naturalJoin("departments");
        NaturalJoin join4 = CF.naturalJoin("employees", condition);
        
        Assertions.assertTrue(join1.equals(join1));
        Assertions.assertTrue(join1.equals(join2));
        Assertions.assertFalse(join1.equals(join3));
        Assertions.assertFalse(join1.equals(join4));
        Assertions.assertFalse(join1.equals(null));
        Assertions.assertFalse(join1.equals("not a NaturalJoin"));
    }

    @Test
    public void testComplexCondition() {
        Condition complexCondition = CF.and(
            CF.eq("department", "Sales"),
            CF.gt("experience", 5),
            CF.like("skills", "%leadership%")
        );
        
        NaturalJoin join = CF.naturalJoin("employees", complexCondition);
        
        Assertions.assertEquals(complexCondition, join.getCondition());
        Assertions.assertEquals(3, join.getParameters().size());
    }

    @Test
    public void testMultipleTablesComplexJoin() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        Condition highValue = CF.gt("totalAmount", 1000);
        
        NaturalJoin join = CF.naturalJoin(tables, highValue);
        
        Assertions.assertEquals(3, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(highValue, join.getCondition());
    }
}