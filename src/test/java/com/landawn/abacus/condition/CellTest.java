package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class CellTest extends TestBase {

    @Test
    public void testConstructor() {
        Equal eq = ConditionFactory.eq("status", "active");
        Cell cell = new Cell(Operator.NOT, eq);
        
        Assertions.assertNotNull(cell);
        Assertions.assertEquals(Operator.NOT, cell.getOperator());
        Assertions.assertEquals(eq, cell.getCondition());
    }

    @Test
    public void testConstructorWithNull() {
        Cell cell = new Cell(Operator.EXISTS, null);
        
        Assertions.assertNotNull(cell);
        Assertions.assertNull(cell.getCondition());
    }

    @Test
    public void testGetCondition() {
        Equal eq = ConditionFactory.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, eq);
        
        Equal retrieved = cell.getCondition();
        Assertions.assertEquals(eq, retrieved);
    }

    @Test
    public void testSetCondition() {
        Cell cell = new Cell(Operator.NOT, null);
        Equal eq = ConditionFactory.eq("id", 1);
        
        cell.setCondition(eq);
        Assertions.assertEquals(eq, cell.getCondition());
    }

    @Test
    public void testGetParametersWithCondition() {
        Between between = ConditionFactory.between("age", 18, 65);
        Cell cell = new Cell(Operator.NOT, between);
        
        List<Object> params = cell.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(18, (Integer)params.get(0));
        Assertions.assertEquals(65, params.get(1));
    }

    @Test
    public void testGetParametersWithNullCondition() {
        Cell cell = new Cell(Operator.EXISTS, null);
        
        List<Object> params = cell.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParametersWithCondition() {
        In in = ConditionFactory.in("id", Arrays.asList(1, 2, 3));
        Cell cell = new Cell(Operator.NOT, in);
        
        cell.clearParameters();
        
        List<Object> params = cell.getParameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParametersWithNullCondition() {
        Cell cell = new Cell(Operator.EXISTS, null);
        
        // Should not throw exception
        cell.clearParameters();
    }

    @Test
    public void testCopy() {
        Equal eq = ConditionFactory.eq("status", "active");
        Cell original = new Cell(Operator.NOT, eq);
        
        Cell copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithNullCondition() {
        Cell original = new Cell(Operator.EXISTS, null);

        Cell copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testToString() {
        Equal eq = ConditionFactory.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, eq);
        
        String result = cell.toString();
        Assertions.assertTrue(result.startsWith("NOT"));
        Assertions.assertTrue(result.contains("name = 'John'"));
    }

    @Test
    public void testToStringWithNullCondition() {
        Cell cell = new Cell(Operator.EXISTS, null);
        
        String result = cell.toString();
        Assertions.assertEquals("EXISTS", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = ConditionFactory.eq("userName", "test");
        Cell cell = new Cell(Operator.NOT, eq);
        
        String result = cell.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("user_name = 'test'"));
    }

    @Test
    public void testEquals() {
        Equal eq1 = ConditionFactory.eq("id", 1);
        Equal eq2 = ConditionFactory.eq("id", 1);
        
        Cell cell1 = new Cell(Operator.NOT, eq1);
        Cell cell2 = new Cell(Operator.NOT, eq2);
        Cell cell3 = new Cell(Operator.EXISTS, eq1);
        Cell cell4 = new Cell(Operator.NOT, null);
        
        Assertions.assertEquals(cell1, cell2);
        Assertions.assertNotEquals(cell1, cell3);
        Assertions.assertNotEquals(cell1, cell4);
        Assertions.assertNotEquals(cell1, null);
        Assertions.assertNotEquals(cell1, "string");
        Assertions.assertEquals(cell1, cell1);
    }

    @Test
    public void testHashCode() {
        Equal eq1 = ConditionFactory.eq("id", 1);
        Equal eq2 = ConditionFactory.eq("id", 1);

        Cell cell1 = new Cell(Operator.NOT, eq1);
        Cell cell2 = new Cell(Operator.NOT, eq2);

        Assertions.assertEquals(cell1.hashCode(), cell2.hashCode());
    }

    @Test
    public void testHashCodeWithNullCondition() {
        Cell cell1 = new Cell(Operator.EXISTS, null);
        Cell cell2 = new Cell(Operator.EXISTS, null);
        
        Assertions.assertEquals(cell1.hashCode(), cell2.hashCode());
    }

    @Test
    public void testAnd() {
        Cell cell = new Cell(Operator.NOT, ConditionFactory.eq("active", true));
        Equal eq = ConditionFactory.eq("status", "published");

        And and = cell.and(eq);
        
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        Cell cell = new Cell(Operator.EXISTS, ConditionFactory.subQuery("SELECT 1"));
        GreaterThan gt = ConditionFactory.gt("count", 0);

        Or or = cell.or(gt);
        
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Cell cell = new Cell(Operator.EXISTS, ConditionFactory.subQuery("SELECT id FROM users"));

        Not not = cell.not();
        
        Assertions.assertNotNull(not);
        Assertions.assertEquals(cell, not.getCondition());
    }
}