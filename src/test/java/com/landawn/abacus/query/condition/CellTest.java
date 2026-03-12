package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class CellTest extends TestBase {

    private static final class TestCell extends Cell {
        TestCell(final Operator operator, final Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testGetCondition() {
        final Equal wrapped = Filters.eq("status", "ACTIVE");
        final TestCell cell = new TestCell(Operator.WHERE, wrapped);

        assertEquals(wrapped, cell.getCondition());
    }

    @Test
    public void testGetParameters() {
        final TestCell cell = new TestCell(Operator.WHERE, Filters.between("age", 18, 65));

        assertEquals(Arrays.asList(18, 65), cell.getParameters());
    }

    @Test
    public void testClearParameters() {
        final TestCell cell = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));

        cell.clearParameters();

        assertEquals(Arrays.asList((Object) null), cell.getParameters());
    }

    @Test
    public void testToString() {
        final TestCell cell = new TestCell(Operator.WHERE, Filters.eq("userName", "ACTIVE"));

        final String sql = cell.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        final TestCell left = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));
        final TestCell right = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentCondition() {
        final TestCell left = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));
        final TestCell right = new TestCell(Operator.WHERE, Filters.eq("status", "INACTIVE"));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestCell left = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));
        final TestCell right = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentCondition() {
        final TestCell left = new TestCell(Operator.WHERE, Filters.eq("status", "ACTIVE"));
        final TestCell right = new TestCell(Operator.WHERE, Filters.eq("status", "INACTIVE"));

        assertNotEquals(left, right);
    }
}
