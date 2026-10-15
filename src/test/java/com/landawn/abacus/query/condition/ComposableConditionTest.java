package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

public class ComposableConditionTest extends TestBase {

    @Test
    void testAnd() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final And result = left.and(right);

        assertEquals(Operator.AND, result.operator());
        assertEquals(2, result.conditions().size());
    }

    @Test
    void testAnd_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.and(null));
    }

    @Test
    void testCompositionRejectsWrappedNonPredicateComponents() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final SubQuery subQuery = new SubQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> left.and(new TestComposableWrapper(new OrderBy("name"))));
        assertThrows(IllegalArgumentException.class, () -> left.or(new TestComposableWrapper(new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> left.xor(new TestComposableWrapper(new SqlExpression("   "))));
        assertDoesNotThrow(() -> left.and(new TestComposableWrapper(new Equal("id", 1))));
    }

    @Test
    void testNot() {
        final TestComposableCondition condition = new TestComposableCondition("status", "ACTIVE");

        final Not result = condition.not();

        assertEquals(Operator.NOT, result.operator());
        assertEquals(condition, result.condition());
    }

    @Test
    void testOr() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final Or result = left.or(right);

        assertEquals(Operator.OR, result.operator());
        assertEquals(2, result.conditions().size());
    }

    @Test
    void testOr_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.or(null));
    }

    // Verifies XOR expands to the documented OR-of-ANDs structure.
    @Test
    void testXor() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final Or result = left.xor(right);

        assertEquals(Operator.OR, result.operator());
        assertEquals(2, result.conditions().size());
        assertTrue(result.conditions().get(0) instanceof And);
        assertTrue(result.conditions().get(1) instanceof And);
    }

    @Test
    void testXor_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.xor(null));
    }

    @Nested
    class Validation {
        @Test
        void testCompositionChecksReceiverBeforeArgument() {
            final SqlExpression invalidReceiver = SqlExpression.of("");

            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> invalidReceiver.and((Condition) null)).getMessage().contains("composable method 'and'"));
            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> invalidReceiver.or((Condition) null)).getMessage().contains("composable method 'or'"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> invalidReceiver.xor(null)).getMessage().contains("composable method 'xor'"));
        }

        @Test
        void testCompositionRejectsClauseArgumentAndAcceptsNestedJunctionReceiver() {
            final Equal eq = new Equal("a", 1);

            assertThrows(IllegalArgumentException.class, () -> eq.and(new Where(new Equal("b", 2))));
            assertThrows(IllegalArgumentException.class, () -> eq.or(new Where(new Equal("b", 2))));
            assertThrows(IllegalArgumentException.class, () -> eq.xor(new Where(new Equal("b", 2))));

            final Or nested = new Or(new Equal("b", 2)).or(new Equal("c", 3));

            assertDoesNotThrow(() -> nested.and(eq));
            assertDoesNotThrow(() -> eq.and(nested).or(nested).xor(eq));
            assertThrows(IllegalArgumentException.class, () -> nested.and(new Where(eq)));
            assertThrows(IllegalArgumentException.class, () -> new And(nested).or(new SqlExpression("")));
        }
    }

    private static final class TestComposableWrapper extends ComposableCell {
        TestComposableWrapper(final Condition condition) {
            super(Operator.NOT, condition);
        }
    }

    private static final class TestComposableCondition extends ComposableCondition {
        private final Object value;

        TestComposableCondition(final String propName, final Object value) {
            super(Operator.EQUAL);
            this.value = value;
            this.propName = propName;
        }

        private final String propName;

        @Override
        public ImmutableList<Object> parameters() {
            return ImmutableList.of(value);
        }

        @Override
        public String toSql(final NamingPolicy namingPolicy) {
            return (namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy).convert(propName) + " = " + value;
        }
    }
}
