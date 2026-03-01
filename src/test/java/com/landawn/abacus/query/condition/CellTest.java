package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class CellTest extends TestBase {

    @Test
    public void testConstructor() {
        Equal eq = Filters.eq("status", "active");
        Cell cell = new Cell(Operator.NOT, eq);

        Assertions.assertNotNull(cell);
        Assertions.assertEquals(Operator.NOT, cell.operator());
        Assertions.assertEquals(eq, cell.getCondition());
    }

    @Test
    public void testGetCondition() {
        Equal eq = Filters.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, eq);

        Equal retrieved = cell.getCondition();
        Assertions.assertEquals(eq, retrieved);
    }

    @Test
    public void testGetParametersWithCondition() {
        Between between = Filters.between("age", 18, 65);
        Cell cell = new Cell(Operator.NOT, between);

        List<Object> params = cell.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(18, (Integer) params.get(0));
        Assertions.assertEquals(65, params.get(1));
    }

    @Test
    public void testClearParametersWithCondition() {
        In in = Filters.in("id", Arrays.asList(1, 2, 3));
        Cell cell = new Cell(Operator.NOT, in);

        cell.clearParameters();

        List<Object> params = cell.getParameters();
        Assertions.assertTrue(params.size() == 3 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testClearParametersWithNullCondition() {
        Assertions.assertThrows(NullPointerException.class, () -> new Cell(Operator.EXISTS, null));
    }

    @Test
    public void testCopy() {
        Equal eq = Filters.eq("status", "active");
        Cell original = new Cell(Operator.NOT, eq);

        Cell copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        Equal eq = Filters.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, eq);

        String result = cell.toString();
        Assertions.assertTrue(result.startsWith("NOT"));
        Assertions.assertTrue(result.contains("name = 'John'"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("userName", "test");
        Cell cell = new Cell(Operator.NOT, eq);

        String result = cell.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("user_name = 'test'"));
    }

    @Test
    public void testEquals() {
        Equal eq1 = Filters.eq("id", 1);
        Equal eq2 = Filters.eq("id", 1);

        Cell cell1 = new Cell(Operator.NOT, eq1);
        Cell cell2 = new Cell(Operator.NOT, eq2);
        Cell cell3 = new Cell(Operator.EXISTS, eq1);

        Assertions.assertEquals(cell1, cell2);
        Assertions.assertNotEquals(cell1, cell3);
        Assertions.assertNotEquals(cell1, "string");
        Assertions.assertEquals(cell1, cell1);
    }

    @Test
    public void testHashCode() {
        Equal eq1 = Filters.eq("id", 1);
        Equal eq2 = Filters.eq("id", 1);

        Cell cell1 = new Cell(Operator.NOT, eq1);
        Cell cell2 = new Cell(Operator.NOT, eq2);

        Assertions.assertEquals(cell1.hashCode(), cell2.hashCode());
    }

    @Test
    public void testAnd() {
        Cell cell = new Cell(Operator.NOT, Filters.eq("active", true));
        Equal eq = Filters.eq("status", "published");

        And and = cell.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        Cell cell = new Cell(Operator.EXISTS, Filters.subQuery("SELECT 1"));
        GreaterThan gt = Filters.gt("count", 0);

        Or or = cell.or(gt);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Cell cell = new Cell(Operator.EXISTS, Filters.subQuery("SELECT id FROM users"));

        Not not = cell.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(cell, not.getCondition());
    }
}