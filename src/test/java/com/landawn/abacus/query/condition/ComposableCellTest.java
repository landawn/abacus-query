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

public class ComposableCellTest extends TestBase {

    private static final class TestComposableCell extends ComposableCell {
        TestComposableCell(final Operator operator, final Condition condition) {
            super(operator, condition);
        }
    }

    private static final class EmptyComposableCell extends ComposableCell {
        EmptyComposableCell() {
            super();
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

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        final EmptyComposableCell left = new EmptyComposableCell();
        final EmptyComposableCell right = new EmptyComposableCell();

        assertTrue(left.getCondition() == null);
        assertTrue(left.getParameters().isEmpty());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testDefaultConstructorToString_NullOperator() {
        // A ComposableCell created via the package-private default constructor has a
        // null operator (mirrors Kryo deserialization of partially-populated state).
        // Calling toString() must not NPE.
        final EmptyComposableCell cell = new EmptyComposableCell();

        final String result = cell.toString(NamingPolicy.NO_CHANGE);
        assertNotNull(result);
        // The operator should render as the literal "null" placeholder.
        assertTrue(result.contains("null"));

        // Also verify the no-arg toString() variant and a null NamingPolicy both work.
        assertNotNull(cell.toString());
        assertNotNull(cell.toString(null));
    }
}
