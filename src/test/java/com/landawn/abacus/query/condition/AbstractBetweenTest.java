package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractBetweenTest extends TestBase {

    private static final class TestAbstractBetween extends AbstractBetween {
        TestAbstractBetween(final String propName, final Object minValue, final Object maxValue) {
            super(propName, Operator.BETWEEN, minValue, maxValue);
        }
    }

    @Test
    public void testGetPropName() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals("age", condition.getPropName());
    }

    @Test
    public void testGetMinValue() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Integer.valueOf(18), condition.getMinValue());
    }

    @Test
    public void testGetMaxValue() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Integer.valueOf(65), condition.getMaxValue());
    }

    // Verifies literal and nested-condition parameter expansion.
    @Test
    public void testGetParameters() {
        final TestAbstractBetween condition = new TestAbstractBetween("age", 18, 65);

        assertEquals(Arrays.asList(18, 65), condition.getParameters());
    }

    @Test
    public void testGetParameters_ConditionValues() {
        final SubQuery minValue = Filters.subQuery("scores", Arrays.asList("min_score"), Filters.eq("status", "ACTIVE"));
        final SubQuery maxValue = Filters.subQuery("scores", Arrays.asList("max_score"), Filters.eq("status", "INACTIVE"));
        final TestAbstractBetween condition = new TestAbstractBetween("score", minValue, maxValue);

        final List<Object> parameters = condition.getParameters();

        assertEquals(Arrays.asList("ACTIVE", "INACTIVE"), parameters);
    }

    @Test
    public void testToString() {
        final TestAbstractBetween condition = new TestAbstractBetween("userAge", 18, 65);

        final String sql = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("user_age"));
        assertTrue(sql.contains("BETWEEN"));
        assertTrue(sql.contains("18"));
        assertTrue(sql.contains("65"));
    }

    @Test
    public void testToString_NullNamingPolicy() {
        final TestAbstractBetween condition = new TestAbstractBetween("userAge", "A", "Z");

        final String sql = condition.toString(null);

        assertTrue(sql.contains("userAge"));
        assertTrue(sql.contains("'A'"));
        assertTrue(sql.contains("'Z'"));
    }

    @Test
    public void testHashCode() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 65);

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 21, 65);

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 65);

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentValues() {
        final TestAbstractBetween left = new TestAbstractBetween("age", 18, 65);
        final TestAbstractBetween right = new TestAbstractBetween("age", 18, 70);

        assertNotEquals(left, right);
    }
}
