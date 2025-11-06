package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for Cell.
 * Tests all public methods including constructors, getters/setters, parameters, copying, and string representation.
 */
@Tag("2025")
public class Cell2025Test extends TestBase {

    @Test
    public void testConstructorWithOperatorAndCondition() {
        Condition condition = CF.eq("status", "active");
        Cell cell = new Cell(Operator.NOT, condition);

        assertNotNull(cell);
        assertEquals(Operator.NOT, cell.getOperator());
        assertEquals(condition, cell.getCondition());
    }

    @Test
    public void testConstructorWithNullConditionThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Cell(Operator.NOT, null);
        });
    }

    @Test
    public void testGetCondition() {
        Equal equal = CF.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, equal);

        Equal retrieved = cell.getCondition();

        assertEquals(equal, retrieved);
    }

    @Test
    public void testGetConditionWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM users");
        Cell cell = new Cell(Operator.EXISTS, subQuery);

        SubQuery retrieved = cell.getCondition();

        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testSetCondition() {
        Cell cell = new Cell(Operator.NOT, CF.eq("old", "value"));

        Condition newCondition = CF.eq("new", "value");
        cell.setCondition(newCondition);

        assertEquals(newCondition, cell.getCondition());
    }

    @Test
    public void testGetParameters() {
        Condition condition = CF.eq("name", "John");
        Cell cell = new Cell(Operator.NOT, condition);

        List<Object> params = cell.getParameters();

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("John", params.get(0));
    }

    @Test
    public void testGetParametersWithMultipleParams() {
        Condition condition = CF.between("age", 18, 65);
        Cell cell = new Cell(Operator.NOT, condition);

        List<Object> params = cell.getParameters();

        assertEquals(2, params.size());
        assertEquals(18, params.get(0));
        assertEquals(65, params.get(1));
    }

    @Test
    public void testGetParametersEmpty() {
        Condition condition = CF.isNull("email");
        Cell cell = new Cell(Operator.NOT, condition);

        List<Object> params = cell.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testGetParametersNullCondition() {
        Cell cell = new Cell(Operator.NOT, CF.isNull("test"));
        // Set condition to null via deprecated method
        cell.setCondition(null);

        List<Object> params = cell.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        Condition condition = CF.eq("status", "active");
        Cell cell = new Cell(Operator.NOT, condition);

        assertFalse(cell.getParameters().isEmpty());

        cell.clearParameters();

        List<Object> params = cell.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testClearParametersWithNullCondition() {
        Cell cell = new Cell(Operator.NOT, CF.isNull("test"));
        cell.setCondition(null);

        // Should not throw exception
        assertDoesNotThrow(() -> cell.clearParameters());
    }

    @Test
    public void testCopy() {
        Condition condition = CF.eq("status", "active");
        Cell original = new Cell(Operator.NOT, condition);

        Cell copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithNullCondition() {
        Cell original = new Cell(Operator.NOT, CF.isNull("test"));
        original.setCondition(null);

        Cell copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertNull(copy.getCondition());
    }

    @Test
    public void testToString() {
        Condition condition = CF.eq("status", "active");
        Cell cell = new Cell(Operator.NOT, condition);

        String result = cell.toString(NamingPolicy.NO_CHANGE);

        assertNotNull(result);
        assertTrue(result.startsWith("NOT"));
        assertTrue(result.contains("status"));
    }

    @Test
    public void testToStringWithExistsOperator() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM users");
        Cell cell = new Cell(Operator.EXISTS, subQuery);

        String result = cell.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.startsWith("EXISTS"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToStringWithNullCondition() {
        Cell cell = new Cell(Operator.NOT, CF.isNull("test"));
        cell.setCondition(null);

        String result = cell.toString(NamingPolicy.NO_CHANGE);

        assertEquals("NOT", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = CF.eq("userName", "John");
        Cell cell = new Cell(Operator.NOT, condition);

        String result = cell.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertTrue(result.contains("user_name") || result.contains("userName"));
    }

    @Test
    public void testHashCode() {
        Cell cell1 = new Cell(Operator.NOT, CF.eq("status", "active"));
        Cell cell2 = new Cell(Operator.NOT, CF.eq("status", "active"));

        assertEquals(cell1.hashCode(), cell2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        Cell cell1 = new Cell(Operator.NOT, CF.eq("status", "active"));
        Cell cell2 = new Cell(Operator.EXISTS, CF.eq("status", "active"));

        assertNotEquals(cell1.hashCode(), cell2.hashCode());
    }

    @Test
    public void testEquals() {
        Cell cell1 = new Cell(Operator.NOT, CF.eq("status", "active"));
        Cell cell2 = new Cell(Operator.NOT, CF.eq("status", "active"));

        assertEquals(cell1, cell2);
    }

    @Test
    public void testEqualsSameInstance() {
        Cell cell = new Cell(Operator.NOT, CF.eq("a", 1));

        assertEquals(cell, cell);
    }

    @Test
    public void testEqualsNull() {
        Cell cell = new Cell(Operator.NOT, CF.eq("a", 1));

        assertNotEquals(cell, null);
    }

    @Test
    public void testEqualsDifferentType() {
        Cell cell = new Cell(Operator.NOT, CF.eq("a", 1));

        assertNotEquals(cell, "not a cell");
    }

    @Test
    public void testEqualsDifferentOperator() {
        Cell cell1 = new Cell(Operator.NOT, CF.eq("a", 1));
        Cell cell2 = new Cell(Operator.EXISTS, CF.eq("a", 1));

        assertNotEquals(cell1, cell2);
    }

    @Test
    public void testEqualsDifferentCondition() {
        Cell cell1 = new Cell(Operator.NOT, CF.eq("a", 1));
        Cell cell2 = new Cell(Operator.NOT, CF.eq("b", 2));

        assertNotEquals(cell1, cell2);
    }

    @Test
    public void testNotCellWithIsNull() {
        Cell cell = new Cell(Operator.NOT, CF.isNull("email"));

        String sql = cell.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("NOT"));
        assertTrue(sql.contains("email"));
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testExistsCellWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM orders WHERE user_id = users.id");
        Cell cell = new Cell(Operator.EXISTS, subQuery);

        String sql = cell.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.startsWith("EXISTS"));
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    public void testNotExistsCellWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM blacklist WHERE user_id = users.id");
        Cell notExistsCell = new Cell(Operator.NOT_EXISTS, subQuery);

        String sql = notExistsCell.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.startsWith("NOT EXISTS"));
    }

    @Test
    public void testCellWithComplexCondition() {
        Condition complex = CF.and(CF.eq("status", "active"), CF.gt("age", 18), CF.isNotNull("email"));

        Cell cell = new Cell(Operator.NOT, complex);

        List<Object> params = cell.getParameters();
        assertEquals(2, params.size());
    }

    @Test
    public void testCellCopyPreservesValues() {
        Cell original = new Cell(Operator.NOT, CF.eq("test", "value"));

        Cell copy = original.copy();
        copy.clearParameters();

        // Original should still have parameters
        assertFalse(original.getParameters().isEmpty());
    }
}
