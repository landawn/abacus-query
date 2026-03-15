package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractInTest extends TestBase {

    private static final class TestAbstractIn extends AbstractIn {
        TestAbstractIn(final String propName, final List<?> values) {
            super(propName, Operator.IN, values);
        }
    }

    @Test
    public void testGetPropName() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals("status", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(Arrays.asList("ACTIVE", "PENDING"), condition.getValues());
    }

    @Test
    public void testGetValues_Unmodifiable() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) condition.getValues()).add("CLOSED"));
    }

    // Verifies direct values and nested condition values are flattened into parameters.
    @Test
    public void testGetParameters() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(Arrays.asList("ACTIVE", "PENDING"), condition.getParameters());
    }

    @Test
    public void testGetParameters_ConditionValues() {
        final TestAbstractIn condition = new TestAbstractIn("id", Arrays.asList(Filters.eq("status", "ACTIVE"), 2));

        assertEquals(Arrays.asList("ACTIVE", 2), condition.getParameters());
    }

    @Test
    public void testToString() {
        final TestAbstractIn condition = new TestAbstractIn("orderStatus", Arrays.asList("ACTIVE", "PENDING"));

        final String sql = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("order_status"));
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("'ACTIVE'"));
    }

    @Test
    public void testToString_NullNamingPolicy() {
        final TestAbstractIn condition = new TestAbstractIn("status", Arrays.asList(1, 2, 3));

        final String sql = condition.toString(null);

        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("1"));
        assertTrue(sql.contains("3"));
    }

    @Test
    public void testHashCode() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE"));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentValues() {
        final TestAbstractIn left = new TestAbstractIn("status", Arrays.asList("ACTIVE", "PENDING"));
        final TestAbstractIn right = new TestAbstractIn("status", Arrays.asList("ACTIVE"));

        assertNotEquals(left, right);
    }
}
