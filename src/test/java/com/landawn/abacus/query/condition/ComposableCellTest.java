package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComposableCellTest extends TestBase {

    private static final class TestComposableCell extends ComposableCell {
        TestComposableCell(final Operator operator, final Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testGetCondition() {
        final Equal wrapped = Filters.eq("status", "ACTIVE");
        final TestComposableCell cell = new TestComposableCell(Operator.NOT, wrapped);

        assertEquals(wrapped, cell.getCondition());
    }

    @Test
    public void testGetParameters() {
        final TestComposableCell cell = new TestComposableCell(Operator.NOT, Filters.between("age", 18, 65));

        assertEquals(Arrays.asList(18, 65), cell.getParameters());
    }

    @Test
    public void testClearParameters() {
        final TestComposableCell cell = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));

        cell.clearParameters();

        assertEquals(Arrays.asList((Object) null), cell.getParameters());
    }

    @Test
    public void testToString() {
        final TestComposableCell cell = new TestComposableCell(Operator.NOT, Filters.eq("userName", "ACTIVE"));

        final String sql = cell.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("NOT"));
        assertTrue(sql.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        final TestComposableCell left = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));
        final TestComposableCell right = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentCondition() {
        final TestComposableCell left = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));
        final TestComposableCell right = new TestComposableCell(Operator.NOT, Filters.eq("status", "INACTIVE"));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestComposableCell left = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));
        final TestComposableCell right = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentCondition() {
        final TestComposableCell left = new TestComposableCell(Operator.NOT, Filters.eq("status", "ACTIVE"));
        final TestComposableCell right = new TestComposableCell(Operator.NOT, Filters.eq("status", "INACTIVE"));

        assertNotEquals(left, right);
    }
}
