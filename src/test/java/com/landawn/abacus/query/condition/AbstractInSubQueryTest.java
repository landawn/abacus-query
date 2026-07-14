package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AbstractInSubQueryTest extends TestBase {

    private static final class NominallyNonEmptyCollection<E> extends AbstractCollection<E> {
        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    private static final class TestAbstractInSubQuery extends AbstractInSubQuery {
        TestAbstractInSubQuery() {
            super();
        }

        TestAbstractInSubQuery(final String propName, final SubQuery subQuery) {
            super(propName, Operator.IN, subQuery);
        }

        TestAbstractInSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
            super(propNames, Operator.IN, subQuery);
        }

        TestAbstractInSubQuery(final String propName, final Operator operator, final SubQuery subQuery) {
            super(propName, operator, subQuery);
        }

        TestAbstractInSubQuery(final Collection<String> propNames, final Operator operator, final SubQuery subQuery) {
            super(propNames, operator, subQuery);
        }
    }

    @Test
    public void testRejectsUnsupportedOperator() {
        SubQuery one = Filters.subQuery("SELECT id FROM users");
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractInSubQuery("id", Operator.EQUAL, one));
        assertThrows(IllegalArgumentException.class,
                () -> new TestAbstractInSubQuery(Arrays.asList("a", "b"), Operator.EQUAL, Filters.subQuery("SELECT a, b FROM t")));
        assertThrows(NullPointerException.class, () -> new TestAbstractInSubQuery("id", null, one));
    }

    @Test
    public void testGetPropNames() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals("user_id", condition.propName());
        assertEquals(Arrays.asList("user_id"), condition.propNames().stream().toList());
    }

    @Test
    public void testGetPropNames_MultipleProperties() {
        final SubQuery subQuery = Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"), subQuery);

        assertEquals("leftId", condition.propName());
        assertEquals(Arrays.asList("leftId", "rightId"), condition.propNames().stream().toList());
    }

    @Test
    public void testGetPropNames_Unmodifiable() {
        final SubQuery subQuery = Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"), subQuery);

        assertThrows(UnsupportedOperationException.class, () -> condition.propNames().add("tenantId"));
    }

    @Test
    public void testGetSubQuery() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals(subQuery, condition.subQuery());
    }

    @Test
    public void testParameters() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE"));
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("user_id", subQuery);

        assertEquals(Arrays.asList("ACTIVE"), condition.parameters());
    }

    @Test
    public void testHashCode() {
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE"));
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id", subQuery);
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQuery() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "INACTIVE")));

        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void testEquals() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        assertEquals(left, right);
    }

    @Test
    public void testEquals_DifferentSubQuery() {
        final TestAbstractInSubQuery left = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));
        final TestAbstractInSubQuery right = new TestAbstractInSubQuery("user_id",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "INACTIVE")));

        assertNotEquals(left, right);
    }

    // Verifies both single-column and multi-column SQL rendering.
    @Test
    public void testToString() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery("userId",
                Filters.subQuery("users", Arrays.asList("id"), Filters.eq("status", "ACTIVE")));

        final String sql = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    public void testToString_MultipleProperties() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"),
                Filters.subQuery("pairs", Arrays.asList("left_id", "right_id"), Filters.eq("status", "ACTIVE")));

        final String sql = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(sql.contains("(left_id, right_id)"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testDefaultConstructor_EmptyState() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery();

        assertEquals(null, condition.propName());
        assertTrue(condition.propNames().isEmpty());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testConstructor_RejectsSubQueryArityMismatch() {
        final SubQuery subQuery = Filters.subQuery("pairs", Arrays.asList("left_id"), Filters.eq("status", "ACTIVE"));

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractInSubQuery(Arrays.asList("leftId", "rightId"), subQuery));
    }

    @Test
    public void testConstructorValidatesDefensivePropertySnapshotIsNonEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestAbstractInSubQuery(new NominallyNonEmptyCollection<>(), Filters.subQuery("SELECT id FROM users")));
    }

    @Test
    public void testConstructor_AllowsStructuredWildcardProjectionWithUnknownArity() {
        SubQuery wildcard = new SubQuery("users", Arrays.asList("*"), null);
        InSubQuery condition = new InSubQuery(Arrays.asList("id", "name"), wildcard);

        assertEquals("(id, name) IN (SELECT * FROM users)", condition.toSql(NamingPolicy.NO_CHANGE));

        SubQuery qualifiedWildcard = new SubQuery("users u", Arrays.asList("u.*"), null);
        assertDoesNotThrow(() -> new InSubQuery(Arrays.asList("id", "name"), qualifiedWildcard));
    }

    @Test
    @Tag("2025")
    public void testConstructor_RejectsBlankPropertyNames() {
        final SubQuery subQuery = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new TestAbstractInSubQuery("   ", subQuery));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractInSubQuery(Arrays.asList("id", "   "), subQuery));
    }

    @Test
    public void testDefaultConstructorToString() {
        final TestAbstractInSubQuery condition = new TestAbstractInSubQuery();

        final String sql = condition.toSql(NamingPolicy.NO_CHANGE);

        assertNotNull(sql);
    }
}
