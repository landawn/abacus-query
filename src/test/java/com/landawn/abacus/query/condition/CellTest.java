package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    public void testConstructor_NullOperatorThrowsIllegalArgumentException() {
        // A null operator is an IllegalArgumentException (previously threw NullPointerException).
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new TestCell(null, Filters.eq("status", "ACTIVE")));
    }

    @Test
    public void testGetCondition() {
        final Equal wrapped = Filters.eq("status", "ACTIVE");
        final TestCell cell = new TestCell(Operator.WHERE, wrapped);

        assertEquals(wrapped, cell.condition());
    }

    @Test
    public void testParameters() {
        final TestCell cell = new TestCell(Operator.WHERE, Filters.between("age", 18, 65));

        assertEquals(Arrays.asList(18, 65), cell.parameters());
    }

    @Test
    public void testToString() {
        final TestCell cell = new TestCell(Operator.WHERE, Filters.eq("userName", "ACTIVE"));

        final String sql = cell.toSql(NamingPolicy.SNAKE_CASE);

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
    public void testHashCodeTracksMutableValueInWrappedCondition() {
        // The wrapped Binary snapshots the array at construction, so external mutation must not leak into
        // the cell's rendering, equality, or hash code.
        final byte[] value = { 1 };
        final TestCell condition = new TestCell(Operator.WHERE, new Binary("payload", Operator.EQUAL, value));

        final int originalHash = condition.hashCode();
        value[0] = 2;

        assertEquals("WHERE payload = '[1]'", condition.toString());

        final TestCell equalToSnapshot = new TestCell(Operator.WHERE, new Binary("payload", Operator.EQUAL, new byte[] { 1 }));
        assertEquals(equalToSnapshot, condition);
        assertEquals(equalToSnapshot.hashCode(), condition.hashCode());
        assertEquals(originalHash, condition.hashCode());

        final TestCell builtFromMutatedValue = new TestCell(Operator.WHERE, new Binary("payload", Operator.EQUAL, new byte[] { 2 }));
        assertNotEquals(builtFromMutatedValue, condition);
    }

    @Test
    public void testParametersArrayCopyIsNotSharedAcrossCalls() {
        // Cell.parameters() is not memoized: each call must hand out the wrapped condition's fresh defensive copy.
        final TestCell cell = new TestCell(Operator.WHERE, Filters.eq("payload", new byte[] { 1 }));

        final byte[] first = (byte[]) cell.parameters().get(0);
        first[0] = 9;

        assertTrue(Arrays.equals(new byte[] { 1 }, (byte[]) cell.parameters().get(0)));
        assertEquals("WHERE payload = '[1]'", cell.toString());
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

    private static final class EmptyTestCell extends Cell {
        EmptyTestCell() {
            super();
        }
    }

    @Test
    public void testDefaultConstructorToString() {
        final EmptyTestCell cell = new EmptyTestCell();

        assertNotNull(cell.toSql(NamingPolicy.NO_CHANGE));
        assertNotNull(cell.toString());
        assertNotNull(cell.toSql(null));
    }
}
