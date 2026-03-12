package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

public class ComposableConditionTest extends TestBase {

    private static final class TestComposableCondition extends ComposableCondition {
        private final Object value;

        TestComposableCondition(final String propName, final Object value) {
            super(Operator.EQUAL);
            this.value = value;
            this.propName = propName;
        }

        private final String propName;

        @Override
        public ImmutableList<Object> getParameters() {
            return ImmutableList.of(value);
        }

        @Override
        public void clearParameters() {
            // no-op for test helper
        }

        @Override
        public String toString(final NamingPolicy namingPolicy) {
            return (namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy).convert(propName) + " = " + value;
        }
    }

    @Test
    public void testNot() {
        final TestComposableCondition condition = new TestComposableCondition("status", "ACTIVE");

        final Not result = condition.not();

        assertEquals(Operator.NOT, result.operator());
        assertEquals(condition, result.getCondition());
    }

    @Test
    public void testAnd() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final And result = left.and(right);

        assertEquals(Operator.AND, result.operator());
        assertEquals(2, result.getConditions().size());
    }

    @Test
    public void testAnd_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.and(null));
    }

    @Test
    public void testOr() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final Or result = left.or(right);

        assertEquals(Operator.OR, result.operator());
        assertEquals(2, result.getConditions().size());
    }

    @Test
    public void testOr_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.or(null));
    }

    // Verifies XOR expands to the documented OR-of-ANDs structure.
    @Test
    public void testXor() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final Or result = left.xor(right);

        assertEquals(Operator.OR, result.operator());
        assertEquals(2, result.getConditions().size());
        assertTrue(result.getConditions().get(0) instanceof And);
        assertTrue(result.getConditions().get(1) instanceof And);
    }

    @Test
    public void testXor_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.xor(null));
    }
}
