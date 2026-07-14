package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

public class ComposableConditionTest extends TestBase {

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

    @Test
    public void testNot() {
        final TestComposableCondition condition = new TestComposableCondition("status", "ACTIVE");

        final Not result = condition.not();

        assertEquals(Operator.NOT, result.operator());
        assertEquals(condition, result.condition());
    }

    @Test
    public void testAnd() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final TestComposableCondition right = new TestComposableCondition("type", "ADMIN");

        final And result = left.and(right);

        assertEquals(Operator.AND, result.operator());
        assertEquals(2, result.conditions().size());
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
        assertEquals(2, result.conditions().size());
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
        assertEquals(2, result.conditions().size());
        assertTrue(result.conditions().get(0) instanceof And);
        assertTrue(result.conditions().get(1) instanceof And);
    }

    @Test
    public void testXor_NullCondition() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");

        assertThrows(IllegalArgumentException.class, () -> left.xor(null));
    }

    @Test
    public void testCompositionRejectsWrappedNonPredicateComponents() {
        final TestComposableCondition left = new TestComposableCondition("status", "ACTIVE");
        final SubQuery subQuery = new SubQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> left.and(new TestComposableWrapper(new OrderBy("name"))));
        assertThrows(IllegalArgumentException.class, () -> left.or(new TestComposableWrapper(new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> left.xor(new TestComposableWrapper(new SqlExpression("   "))));
        assertDoesNotThrow(() -> left.and(new TestComposableWrapper(new Equal("id", 1))));
    }
}
