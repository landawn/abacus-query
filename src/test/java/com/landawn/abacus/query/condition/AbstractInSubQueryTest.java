package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractInSubQueryTest extends TestBase {

    private static final class TestAbstractInSubQuery extends AbstractInSubQuery {
        TestAbstractInSubQuery(final String propName, final SubQuery subQuery) {
            super(propName, Operator.IN, subQuery);
        }

        TestAbstractInSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
            super(propNames, Operator.IN, subQuery);
        }
    }

    @Test
    public void testGetPropNames() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals(Arrays.asList("user_id"), condition.getPropNames().stream().toList());
    }

    @Test
    public void testGetPropNames_MultipleProperties() {
        final SubQuery subQuery = Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"), subQuery);

        assertEquals(Arrays.asList("leftId", "rightId"), condition.getPropNames().stream().toList());
    }

    @Test
    public void testGetPropNames_Unmodifiable() {
        final SubQuery subQuery = Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"), subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.getPropNames().add("tenantId"));
    }

    @Test
    public void testGetSubQuery() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals(subQuery, condition.getSubQuery());
    }

    @Test
    public void testGetParameters() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE"));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals(Arrays.asList("ACTIVE"), condition.getParameters());
    }

    @Test
    public void testClearParameters() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE"));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        condition.clearParameters();

        assertEquals(Arrays.asList((Object) null), condition.getParameters());
    }

    @Test
    public void testHashCode() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE"));
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id", subQuery);
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQuery() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "INACTIVE")));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentSubQuery() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "INACTIVE")));

        assertNotEquals(left, right);
    }

    // Verifies both single-column and multi-column SQL rendering.
    @Test
    public void testToString() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("userId", Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        final String sql = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    public void testToString_MultipleProperties() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"),
                Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("status", "ACTIVE")));

        final String sql = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("(left_id, right_id)"));
        assertTrue(sql.contains("IN"));
    }
}
