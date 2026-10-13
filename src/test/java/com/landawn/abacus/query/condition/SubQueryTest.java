package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for SubQuery.
 * Tests all public methods including constructors, getters, parameters, copying, and SQL generation.
 */
public class SubQueryTest extends TestBase {

    @Test
    void testComplexSubQuery() {
        List<String> props = Arrays.asList("id", "email", "created");
        And complexCondition = Filters.and(Filters.eq("active", true), Filters.gt("created", "2023-01-01"), Filters.like("email", "%@company.com"));

        SubQuery subQuery = Filters.subQuery("users", props, complexCondition);

        String result = subQuery.toString();
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id, email, created"));
        assertTrue(result.contains("FROM users"));
        assertEquals(3, subQuery.parameters().size());
    }

    @Test
    void testComplexSubQueryWithMultipleProperties() {
        Collection<String> props = Arrays.asList("id", "name", "email", "age");
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.between("age", 18, 65));

        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String sql = subQuery.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("name"));
        assertTrue(sql.contains("email"));
        assertTrue(sql.contains("age"));
    }

    @Test
    void testConditionNotWrappedForClause() {
        List<String> props = Arrays.asList("id");
        And and = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, and);

        // AND is already a clause, should not be wrapped
        assertNotEquals(and, subQuery.condition());
    }

    @Test
    void testConditionWrappingInWhere() {
        List<String> props = Arrays.asList("id");
        Equal eq = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", props, eq);

        // The condition should be wrapped in WHERE
        assertNotNull(subQuery.condition());
        String result = subQuery.toString();
        assertTrue(result.contains("WHERE"));
    }

    @Test
    void testConstructorDoesNotDoubleWrapWhereExpression() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.expr("WHERE active = true"));

        assertEquals("SELECT id FROM users WHERE active = true", subQuery.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testConstructorDoesNotWrapWhereCondition() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.where(Filters.gt("price", 100));

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        assertNotNull(subQuery.condition());
        assertEquals(Operator.WHERE, subQuery.condition().operator());
    }

    @Test
    void testConstructorRejectsBlankEntityAndPropertyNames() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("   ", Arrays.asList("id"), (Condition) null));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", "   "), (Condition) null));
    }

    @Test
    void testConstructorRejectsCollectionThatBecomesEmptyWhileSnapshotting() {
        Collection<String> unstablePropNames = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", unstablePropNames, null));
        assertThrows(IllegalArgumentException.class, () -> new SubQuery(TestEntity.class, unstablePropNames, null));
    }

    @Test
    void testConstructorRejectsCriteriaConditionWithSelectModifier() {
        Criteria criteria = Criteria.builder().distinct().where(Filters.eq("active", true)).build();

        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id"), criteria));
    }

    @Test
    void testConstructorRejectsInvalidSelectPropNames() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", null), (Condition) null));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", ""), (Condition) null));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList("id", null), (Condition) null));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList("id", ""), (Condition) null));
    }

    @Test
    void testConstructorRejectsOnCondition() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("products", Arrays.asList("id"), Filters.on("a", "b")));
    }

    @Test
    void testConstructorRejectsRawOnUsingExpressions() {
        assertThrows(IllegalArgumentException.class,
                () -> Filters.subQuery("users", Arrays.asList("id"), Filters.expr("ON users.id = orders.user_id")));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id"), Filters.expr("USING (id)")));
    }

    @Test
    void testConstructorRejectsStandaloneNonPredicateOperands() {
        SubQuery nested = new SubQuery("SELECT id FROM accounts");

        assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", Arrays.asList("id"), nested));
        assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", Arrays.asList("id"), new Some(nested)));

        // EXISTS is a complete predicate and remains valid even though it owns a subquery.
        SubQuery valid = new SubQuery("users", Arrays.asList("id"), new NotExists(nested));
        assertEquals("SELECT id FROM users WHERE NOT EXISTS (SELECT id FROM accounts)", valid.toString());
    }

    @Test
    void testConstructorRejectsUsingCondition() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("products", Arrays.asList("id"), Filters.using("id")));
    }

    @Test
    void testConstructorRejectsWhitespaceOnlyRawSql() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("   "));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", "   "));
    }

    @Test
    void testConstructorWithBlankStringConditionTreatsAsNoCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), "   ");
        assertNull(subQuery.condition());
    }

    @Test
    void testConstructorWithEmptySql() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.subQuery("");
        });
    }

    @Test
    void testConstructorWithEmptySQLThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SubQuery("");
        });
    }

    @Test
    void testConstructorWithEntityAndSql() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        assertEquals(entityName, subQuery.entityName());
        assertEquals(sql, subQuery.rawSql());
    }

    @Test
    void testConstructorWithEntityClassAndEmptyPropertiesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList(), (Condition) null));
    }

    @Test
    void testConstructorWithEntityClassPropertiesCondition() {
        Collection<String> props = Arrays.asList("id", "name");
        Condition condition = Filters.like("name", "%test%");

        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, condition);

        assertNotNull(subQuery);
        assertEquals("TestEntity", subQuery.entityName());
        assertEquals(TestEntity.class, subQuery.entityClass());
        assertEquals(props, subQuery.selectPropNames());
        assertNotNull(subQuery.condition());
    }

    @Test
    void testConstructorWithEntityClassPropsAndCondition() {
        List<String> props = Arrays.asList("id", "categoryId");
        GreaterThan condition = Filters.gt("price", 100);
        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, condition);

        assertEquals("TestEntity", subQuery.entityName());
        assertEquals(TestEntity.class, subQuery.entityClass());
        assertEquals(props, subQuery.selectPropNames());
        assertNotNull(subQuery.condition());
    }

    @Test
    void testConstructorWithEntityNameAndEmptyPropertiesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList(), (Condition) null));
    }

    @Test
    void testConstructorWithEntityNameAndEntityClass() {
        Collection<String> props = Arrays.asList("id", "name");

        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, (Condition) null);

        assertEquals(TestEntity.class, subQuery.entityClass());
        assertEquals("TestEntity", subQuery.entityName());
    }

    @Test
    void testConstructorWithEntityNameAndSQL() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.entityName());
        assertEquals(sql, subQuery.rawSql());
    }

    @Test
    void testConstructorWithEntityNamePropertiesCondition() {
        String entityName = "users";
        Collection<String> props = Arrays.asList("id", "email");
        Condition condition = Filters.eq("active", true);

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.entityName());
        assertEquals(props, subQuery.selectPropNames());
        assertNotNull(subQuery.condition());
        assertNull(subQuery.rawSql());
    }

    @Test
    void testConstructorWithEntityPropsAndCondition() {
        List<String> props = Arrays.asList("id", "email");
        Equal condition = Filters.eq("active", true);
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        assertEquals("users", subQuery.entityName());
        assertEquals(props, subQuery.selectPropNames());
        assertNotNull(subQuery.condition());
        assertNull(subQuery.rawSql());
    }

    @Test
    void testConstructorWithNullCondition() {
        String entityName = "users";
        Collection<String> props = Arrays.asList("id");

        SubQuery subQuery = Filters.subQuery(entityName, props, (Condition) null);

        assertNotNull(subQuery);
        assertNull(subQuery.condition());
    }

    @Test
    void testConstructorWithNullEntityNameAndRawSqlUsesEmptyEntityName() {
        SubQuery subQuery = new SubQuery((String) null, "SELECT id FROM users");
        assertEquals("", subQuery.entityName());
    }

    @Test
    void testConstructorWithNullSql() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.subQuery((String) null);
        });
    }

    @Test
    void testConstructorWithNullSQLThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SubQuery((String) null);
        });
    }

    @Test
    void testConstructorWithRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        assertNotNull(subQuery);
        assertEquals(sql, subQuery.rawSql());
        assertNull(subQuery.condition());
        assertNull(subQuery.selectPropNames());
    }

    @Test
    void testConstructorWithRawSql() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        assertNotNull(subQuery);
        assertEquals(sql, subQuery.rawSql());
        assertEquals("", subQuery.entityName());
        assertNull(subQuery.entityClass());
        assertNull(subQuery.selectPropNames());
        assertNull(subQuery.condition());
        assertEquals(Operator.EMPTY, subQuery.operator());
    }

    @Test
    void testConstructorWrapsNonClauseConditionInWhere() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.gt("price", 100);

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        // Condition should be wrapped in WHERE
        assertNotNull(subQuery.condition());
        assertEquals(Operator.WHERE, subQuery.condition().operator());
    }

    // toSql() appended its separator space unconditionally, but a Criteria already emits a leading space
    // before each of its clauses, so the two stacked into a doubled space after the table name.
    @Test
    void testCriteriaConditionIsSeparatedByExactlyOneSpace() {
        final Criteria criteria = Criteria.builder().where(Filters.eq("a", 1)).orderBy("b").build();

        assertEquals("SELECT id FROM users WHERE a = 1 ORDER BY b", new SubQuery("users", Arrays.asList("id"), criteria).toString());
        assertEquals("SELECT id FROM users CROSS JOIN orders",
                new SubQuery("users", Arrays.asList("id"), Criteria.builder().crossJoin("orders").build()).toString());

        // The single-clause and plain-predicate forms were already correct and must stay that way.
        assertEquals("SELECT id FROM users WHERE a = 1", new SubQuery("users", Arrays.asList("id"), new Where(Filters.eq("a", 1))).toString());
        assertEquals("SELECT id FROM users WHERE a = 1", new SubQuery("users", Arrays.asList("id"), Filters.eq("a", 1)).toString());
    }

    @Test
    void testCriteriaConditionNotDoubleWrappedInWhere() {
        List<String> props = Arrays.asList("id");
        Criteria criteria = Criteria.builder().where(Filters.eq("active", true)).build();
        SubQuery subQuery = Filters.subQuery("users", props, criteria);

        String result = subQuery.toString();
        assertFalse(result.contains("WHERE  WHERE"));
        assertFalse(result.contains("WHERE WHERE"));
        assertTrue(result.contains("WHERE"));
    }

    // The default constructor exists for serialization and should only expose empty state safely.
    @Test
    void testDefaultConstructor_EmptyState() {
        SubQuery subQuery = new SubQuery();

        assertNull(subQuery.entityName());
        assertNull(subQuery.entityClass());
        assertNull(subQuery.rawSql());
        assertTrue(subQuery.parameters().isEmpty());
    }

    @Test
    void testDefaultConstructorToStringDoesNotEmitFromNull() {
        // Bug fix: previously rendered "SELECT * FROM null" when the default Kryo constructor
        // left entityName/entityClass null. That is invalid SQL containing the literal "null"
        // for the table name. The FROM clause should be omitted entirely in that case.
        SubQuery subQuery = new SubQuery();

        String rendered = subQuery.toSql(NamingPolicy.NO_CHANGE);
        assertFalse(rendered.contains("FROM null"), "toString() must not include 'FROM null'; got: " + rendered);
        assertFalse(rendered.contains("from null"), "toString() must not include 'from null'; got: " + rendered);
        // The rendered string should look like a benign placeholder, e.g. just "SELECT *".
        assertTrue(rendered.startsWith("SELECT"), "Should still start with SELECT");
    }

    @Test
    void testEmbeddingRawSubqueryTerminatesTrailingLineComments() {
        for (final String comment : List.of("-- trailing comment", "# trailing comment")) {
            final String rawSql = "SELECT id FROM users " + comment;
            final SubQuery subQuery = new SubQuery(rawSql);
            final String parenthesized = "(" + rawSql + "\n)";

            assertEquals(rawSql, subQuery.rawSql());
            assertEquals(rawSql, subQuery.toSql(NamingPolicy.NO_CHANGE));
            assertEquals("id = " + parenthesized, new Equal("id", subQuery).toString());
            assertEquals("id IN " + parenthesized, new InSubQuery("id", subQuery).toString());
            assertEquals("EXISTS " + parenthesized, new Exists(subQuery).toString());
            assertEquals("ALL " + parenthesized, new All(subQuery).toString());
            assertEquals(parenthesized, SqlExpression.renderValue(subQuery));
        }
    }

    @Test
    void testEmptyCriteriaIsNormalizedToNoCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Criteria.builder().build());

        assertNull(subQuery.condition());
        assertEquals("SELECT id FROM users", subQuery.toString());
    }

    @Test
    void testEmptyJunctionsArePreservedAsBooleanIdentityPredicates() {
        SubQuery allRows = Filters.subQuery("users", Arrays.asList("id"), Filters.and());
        SubQuery noRows = Filters.subQuery("users", Arrays.asList("id"), Filters.or());

        assertTrue(allRows.condition() instanceof Where);
        assertTrue(noRows.condition() instanceof Where);
        assertEquals("SELECT id FROM users WHERE 1 = 1", allRows.toSql(NamingPolicy.NO_CHANGE));
        assertEquals("SELECT id FROM users WHERE 1 = 0", noRows.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testEquals() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq1, sq2);
    }

    @Test
    void testEqualsAndHashCodeIncludeEntityClass() {
        List<String> props = Arrays.asList("id");
        SubQuery classBased = Filters.subQuery(TestEntity.class, props, Filters.eq("active", true));
        SubQuery nameBased = Filters.subQuery("TestEntity", props, Filters.eq("active", true));

        assertNotEquals(classBased, nameBased);
        assertNotEquals(classBased.hashCode(), nameBased.hashCode());
    }

    @Test
    void testEqualsDifferentSQL() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM orders");

        assertNotEquals(sq1, sq2);
    }

    @Test
    void testEqualsDifferentType() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertNotEquals(sq, "not a subquery");
    }

    @Test
    void testEqualsNull() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertNotEquals(sq, null);
    }

    @Test
    void testEqualsSameInstance() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq, sq);
    }

    @Test
    void testEqualsStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.eq("active", true);

        SubQuery sq1 = Filters.subQuery("users", props, condition);
        SubQuery sq2 = Filters.subQuery("users", props, condition);

        assertEquals(sq1, sq2);
    }

    @Test
    void testEqualsStructuredDifferentEntity() {
        Collection<String> props = Arrays.asList("id");

        SubQuery sq1 = Filters.subQuery("users", props, (Condition) null);
        SubQuery sq2 = Filters.subQuery("orders", props, (Condition) null);

        assertNotEquals(sq1, sq2);
    }

    @Test
    void testExpressionConditionWrappedInWhere() {
        List<String> props = Arrays.asList("id");
        SqlExpression expression = Filters.expr("status = 'active'");
        SubQuery subQuery = Filters.subQuery("users", props, expression);

        assertTrue(subQuery.toString().contains("WHERE status = 'active'"));
    }

    @Test
    void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        assertNotNull(subQuery.condition());
    }

    @Test
    void testGetEntityClass() {
        SubQuery subQuery = Filters.subQuery(TestEntity.class, Arrays.asList("id"), (Condition) null);

        assertEquals(TestEntity.class, subQuery.entityClass());
    }

    @Test
    void testGetEntityName() {
        String entityName = "orders";
        SubQuery subQuery = Filters.subQuery(entityName, Arrays.asList("id"), (Condition) null);

        assertEquals(entityName, subQuery.entityName());
    }

    @Test
    void testGetOperatorReturnsEmptyOrNull() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        // SubQuery doesn't have a specific operator, verify it returns empty or null
        Operator op = subQuery.operator();
        assertTrue(op == null || op.toString().isEmpty());
    }

    @Test
    void testGetSelectPropNames() {
        Collection<String> props = Arrays.asList("id", "name", "email");
        SubQuery subQuery = Filters.subQuery("users", props, (Condition) null);

        assertEquals(props, subQuery.selectPropNames());
    }

    /**
     * Second-pass locking test: selectPropNames returns an unmodifiable view of the
     * underlying list (callers cannot mutate the SubQuery's internal state).
     */
    @Test
    void testGetSelectPropNamesIsUnmodifiable() {
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "name"), null);

        java.util.Collection<String> props = subQuery.selectPropNames();

        assertEquals(2, props.size());
        assertThrows(UnsupportedOperationException.class, () -> props.add("extra"));
    }

    @Test
    void testGetSql() {
        String sql = "SELECT * FROM users";
        SubQuery subQuery = Filters.subQuery(sql);

        assertEquals(sql, subQuery.rawSql());
    }

    @Test
    void testHashCode() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    void testHashCodeStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.eq("active", true);

        SubQuery sq1 = Filters.subQuery("users", props, condition);
        SubQuery sq2 = Filters.subQuery("users", props, condition);

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    void testParameters() {
        List<String> props = Arrays.asList("id", "name");
        And condition = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        List<Object> params = subQuery.parameters();
        assertEquals(2, params.size());
        assertTrue(params.contains(true));
        assertTrue(params.contains(18));
    }

    @Test
    void testParametersArrayCopyIsNotSharedAcrossCalls() {
        // SubQuery.parameters() is not memoized: each call must hand out the condition's fresh defensive copy.
        final SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), Filters.eq("payload", new byte[] { 1 }));

        final byte[] first = (byte[]) subQuery.parameters().get(0);
        first[0] = 9;

        assertTrue(Arrays.equals(new byte[] { 1 }, (byte[]) subQuery.parameters().get(0)));
        assertEquals("SELECT id FROM users WHERE payload = '[1]'", subQuery.toString());
    }

    @Test
    void testParametersEmptyForNullCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);

        List<Object> params = subQuery.parameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    void testParametersEmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.parameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    void testParametersFromCondition() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.between("age", 18, 65));

        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        List<Object> params = subQuery.parameters();

        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals(18, params.get(1));
        assertEquals(65, params.get(2));
    }

    @Test
    void testParametersWithNoCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.parameters();
        assertTrue(params.isEmpty());
    }

    @Test
    void testRawBindingArraysUseContentEqualityAndConsistentHashCode() {
        // Raw bindings are snapshotted at construction and compared by content: distinct-but-equal arrays make equal subqueries.
        final SubQuery a = new SubQuery("SELECT id FROM users WHERE tags = ?", List.of(new int[] { 1 }));
        final SubQuery b = new SubQuery("SELECT id FROM users WHERE tags = ?", List.of(new int[] { 1 }));

        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());

        // Different array content -> unequal.
        final SubQuery c = new SubQuery("SELECT id FROM users WHERE tags = ?", List.of(new int[] { 2 }));
        assertNotEquals(a, c);

        // Nested arrays are compared element-wise too.
        final SubQuery d = new SubQuery("SELECT id FROM users WHERE tags = ?", List.of((Object) new Object[] { new int[] { 1 }, "x" }));
        final SubQuery e = new SubQuery("SELECT id FROM users WHERE tags = ?", List.of((Object) new Object[] { new int[] { 1 }, "x" }));
        assertEquals(d, e);
        assertEquals(d.hashCode(), e.hashCode());

        // Non-array bindings still use their own equals (null included).
        final SubQuery f = new SubQuery("SELECT id FROM users WHERE a = ? AND b = ?", Arrays.asList("v", null));
        final SubQuery g = new SubQuery("SELECT id FROM users WHERE a = ? AND b = ?", Arrays.asList("v", null));
        assertEquals(f, g);
        assertEquals(f.hashCode(), g.hashCode());
        assertNotEquals(f, new SubQuery("SELECT id FROM users WHERE a = ? AND b = ?", Arrays.asList("v", "w")));
    }

    @Test
    void testRawBindingConditionsThatCannotBeInlinedAreRejected() {
        final String sql = "SELECT id FROM t WHERE a = ?";

        assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, Arrays.asList(Filters.eq("b", 1))));
        assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, Arrays.asList(new SubQuery("users", Arrays.asList("id"), (Condition) null))));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(sql, Arrays.asList(Filters.QME)));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(sql, Arrays.asList(SqlExpression.of("COALESCE(?, 0)"))));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(sql, Arrays.asList(SqlExpression.of(" "))));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(sql, Arrays.asList(SqlExpression.of("/* x */"))));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, Arrays.asList(Filters.eq("b", 1))));
        assertTrue(e.getMessage().contains("parameters[0]"), e.getMessage());
    }

    @Test
    void testRawBindingsAreSnapshottedAndHashStableUnderCallerMutation() {
        // Raw array/Date bindings are snapshotted at construction (like Binary), so a caller mutating its own
        // originals afterwards must not change hashCode()/equals() or what parameters() exposes.
        final byte[] callerArray = new byte[] { 1, 2 };
        final java.util.Date callerDate = new java.util.Date(1_000_000L);
        final long originalTime = callerDate.getTime();

        final SubQuery subQuery = new SubQuery("SELECT id FROM users WHERE payload = ? AND created > ?", Arrays.asList(callerArray, callerDate));
        final int hashBefore = subQuery.hashCode();
        final SubQuery twin = new SubQuery("SELECT id FROM users WHERE payload = ? AND created > ?",
                Arrays.asList(new byte[] { 1, 2 }, new java.util.Date(originalTime)));

        final java.util.Set<SubQuery> set = new java.util.HashSet<>();
        set.add(subQuery);

        callerArray[0] = 9;
        callerDate.setTime(originalTime + 60_000L);

        assertEquals(hashBefore, subQuery.hashCode());
        assertTrue(set.contains(subQuery));
        assertEquals(twin, subQuery);
        assertEquals(twin.hashCode(), subQuery.hashCode());

        final List<Object> params = subQuery.parameters();
        assertTrue(Arrays.equals(new byte[] { 1, 2 }, (byte[]) params.get(0)));
        assertEquals(originalTime, ((java.util.Date) params.get(1)).getTime());

        // parameters() hands out defensive copies: neither the caller's originals nor the same reference twice.
        assertFalse(params.get(0) == callerArray);
        assertFalse(params.get(1) == callerDate);
        assertFalse(params.get(0) == subQuery.parameters().get(0));
        assertFalse(params.get(1) == subQuery.parameters().get(1));

        // Mutating a returned copy does not leak into a later call either.
        ((byte[]) params.get(0))[1] = 7;
        assertTrue(Arrays.equals(new byte[] { 1, 2 }, (byte[]) subQuery.parameters().get(0)));

        // Scalar-only raw bindings still return the plain immutable list (same instance each call).
        final SubQuery scalars = new SubQuery("SELECT id FROM users WHERE a = ?", List.of("v"));
        assertTrue(scalars.parameters() == scalars.parameters());
    }

    @Test
    void testRawSqlAllowsNullBindingAndIncludesBindingsInIdentity() {
        SubQuery left = Filters.subQuery("SELECT id FROM users WHERE tenant_id = ?", Arrays.asList((Object) null));
        SubQuery equal = Filters.subQuery("SELECT id FROM users WHERE tenant_id = ?", Arrays.asList((Object) null));
        SubQuery different = Filters.subQuery("SELECT id FROM users WHERE tenant_id = ?", Arrays.asList(7));

        assertEquals(Arrays.asList((Object) null), left.parameters());
        assertEquals(left, equal);
        assertEquals(left.hashCode(), equal.hashCode());
        assertNotEquals(left, different);
    }

    @Test
    void testRawSqlBindingValidation() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("SELECT id FROM users WHERE status = ?"));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("SELECT id FROM users WHERE status = ?", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("SELECT id FROM users", Collections.singletonList("extra")));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("SELECT id FROM users WHERE status = :status", Collections.singletonList("OPEN")));
        assertThrows(IllegalArgumentException.class,
                () -> Filters.subQuery("SELECT id FROM users WHERE status = #{status}", Collections.singletonList("OPEN")));
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("SELECT id FROM users", (Collection<?>) null));
    }

    @Test
    void testRawSqlCapturesPositionalBindingsDefensively() {
        List<Object> bindings = new ArrayList<>(Arrays.asList("OPEN", 100));
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM orders WHERE status = ? AND total > ?", bindings);

        bindings.set(0, "MUTATED");
        bindings.clear();

        assertEquals(Arrays.asList("OPEN", 100), subQuery.parameters());
        assertThrows(UnsupportedOperationException.class, () -> subQuery.parameters().add("extra"));
        assertEquals("SELECT user_id FROM orders WHERE status = ? AND total > ?", subQuery.rawSql());
    }

    @Test
    void testRawSqlExpressionBindingIsInlinedAtConstruction() {
        final SubQuery sq = new SubQuery("SELECT id FROM orders WHERE created < ? AND total > ?", Arrays.asList(SqlExpression.of("CURRENT_DATE"), 100));

        assertEquals("SELECT id FROM orders WHERE created < CURRENT_DATE AND total > ?", sq.rawSql());
        assertEquals(Arrays.asList(100), sq.parameters());
        assertEquals(sq.rawSql(), sq.toSql(NamingPolicy.SNAKE_CASE));

        // Only SQL-valued bindings are inlined; the text is written verbatim (no naming policy), and a glued
        // placeholder, a negative expression after '-' and a trailing line comment are kept apart from their neighbours.
        assertEquals("SELECT a FROM t WHERE b=NOW() AND c = ?",
                new SubQuery("SELECT a FROM t WHERE b=? AND c = ?", Arrays.asList(SqlExpression.of("NOW()"), "x")).rawSql());
        assertEquals("SELECT a FROM t WHERE b=firstName AND c = 1",
                new SubQuery("SELECT a FROM t WHERE b=?AND c = 1", Arrays.asList(SqlExpression.of("firstName"))).rawSql());
        assertEquals("SELECT a FROM t WHERE b = 10- -1", new SubQuery("SELECT a FROM t WHERE b = 10-?", Arrays.asList(SqlExpression.of("-1"))).rawSql());
        assertEquals("SELECT a FROM t WHERE b = x -- c\n AND c = 2",
                new SubQuery("SELECT a FROM t WHERE b = ? AND c = 2", Arrays.asList(SqlExpression.of("x -- c"))).rawSql());

        // A '?' inside a quoted literal is not a placeholder and is left alone.
        assertEquals("SELECT a FROM t WHERE q = '?' AND b = NOW()",
                new SubQuery("SELECT a FROM t WHERE q = '?' AND b = ?", Arrays.asList(SqlExpression.of("NOW()"))).rawSql());

        assertEquals(sq, Filters.subQuery("SELECT id FROM orders WHERE created < ? AND total > ?", Arrays.asList(SqlExpression.of("CURRENT_DATE"), 100)));
    }

    @Test
    void testRawSqlPlaceholderScannerIgnoresQuotedMarkersAndJsonQuestionOperator() {
        SubQuery quoted = Filters.subQuery("SELECT '?' AS marker, ':name' AS named_marker");
        SubQuery json = Filters.subQuery("SELECT payload ? 'key' FROM documents");

        assertTrue(quoted.parameters().isEmpty());
        assertTrue(json.parameters().isEmpty());
    }

    @Test
    void testRawSubQueryBindingIsInlinedWithItsOwnBindings() {
        final SubQuery inner = new SubQuery("SELECT MAX(total) FROM orders WHERE status = ?", Arrays.asList("OPEN"));
        final SubQuery outer = new SubQuery("SELECT id FROM orders WHERE region = ? AND total = ? AND created > ?", Arrays.asList("EU", inner, 5));

        assertEquals("SELECT id FROM orders WHERE region = ? AND total = (SELECT MAX(total) FROM orders WHERE status = ?) AND created > ?", outer.rawSql());
        assertEquals(Arrays.asList("EU", "OPEN", 5), outer.parameters());

        // A nested SqlExpression binding was already inlined by the inner subquery.
        final SubQuery innerExpr = new SubQuery("SELECT MAX(d) FROM t WHERE d < ?", Arrays.asList(SqlExpression.of("CURRENT_DATE")));
        assertEquals("SELECT id FROM t WHERE d = (SELECT MAX(d) FROM t WHERE d < CURRENT_DATE)",
                new SubQuery("SELECT id FROM t WHERE d = ?", Arrays.asList(innerExpr)).rawSql());
    }

    // Regression: placeholders were only counted after SELECT/WITH/...; TABLE (and BOM-prefixed) text counted 0, rejecting its bindings and accepting an unbound '?'.
    @Test
    void testRawSubQueryPlaceholdersOutsideSelectPrefixAreValidated() {
        final SubQuery table = new SubQuery("TABLE t ORDER BY c LIMIT ?", Arrays.asList(5));
        assertEquals("TABLE t ORDER BY c LIMIT ?", table.rawSql());
        assertEquals(Arrays.asList(5), table.parameters());
        assertEquals(Arrays.asList(5), new SubQuery("TABLE t UNION SELECT a FROM u WHERE b = ?", Arrays.asList(5)).parameters());
        assertThrows(IllegalArgumentException.class, () -> new SubQuery("TABLE t ORDER BY c LIMIT ?"));

        assertThrows(IllegalArgumentException.class, () -> new SubQuery("\uFEFFSELECT id FROM t WHERE a = ?"));
        assertEquals(Arrays.asList(1), new SubQuery("\uFEFFSELECT id FROM t WHERE a = ?", Arrays.asList(1)).parameters());

        // Text that is not a recognized query cannot carry a '?' whose binding could be verified.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SubQuery("FROM t SELECT a WHERE b = ?"));
        assertTrue(e.getMessage().contains("not a recognized query"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new SubQuery("FROM t SELECT a WHERE b = ?", Arrays.asList(1)));

        // A '?' inside quoted text or a comment is not a placeholder.
        assertEquals("FROM t SELECT a WHERE b = '?' /* ? */", new SubQuery("FROM t SELECT a WHERE b = '?' /* ? */").rawSql());
        assertEquals("FROM t SELECT \"a?\" WHERE b = N'?'", new SubQuery("FROM t SELECT \"a?\" WHERE b = N'?'").rawSql());
    }

    @Test
    void testRawSubQueryTableVariantsCommentOnlyPlaceholdersAndFailClosedTradeOffs() {
        // Covers TABLE statements (parenthesized, lowercase, BOM-prefixed) with bindings, a '?' only inside a comment of
        // unrecognized text, and the deliberate fail-closed rejections of unrecognized text with an unverifiable '?'.
        assertEquals(Arrays.asList(1), new SubQuery("(TABLE t LIMIT ?)", Arrays.asList(1)).parameters());
        assertEquals(Arrays.asList(1), new SubQuery("table t limit ?", Arrays.asList(1)).parameters());
        assertEquals(Arrays.asList(1), new SubQuery("﻿TABLE t LIMIT ?", Arrays.asList(1)).parameters());
        assertEquals(Arrays.asList(1), Filters.subQuery("TABLE t LIMIT ?", Arrays.asList(1)).parameters());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SubQuery("(TABLE t LIMIT ?)"));
        assertTrue(e.getMessage().contains("placeholder count (1)"), e.getMessage());

        // A '?' that is only inside a comment needs no binding, and accepts none.
        assertEquals("FROM t SELECT a -- ?\n", new SubQuery("FROM t SELECT a -- ?\n").rawSql());
        assertEquals("FROM t SELECT a # ?\n", new SubQuery("FROM t SELECT a # ?\n").rawSql());
        assertThrows(IllegalArgumentException.class, () -> new SubQuery("FROM t SELECT a -- ?\n", Arrays.asList(1)));

        // Trade-off: in unrecognized text a '?' in a bracket identifier or a JSON operator cannot be told from a placeholder,
        // so it is rejected; in a recognized query the JSON operator is not a placeholder.
        e = assertThrows(IllegalArgumentException.class, () -> new SubQuery("FROM [t?] SELECT a"));
        assertTrue(e.getMessage().contains("not a recognized query"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> new SubQuery("FROM t SELECT j ?| array['a']"));
        assertTrue(e.getMessage().contains("not a recognized query"), e.getMessage());
        assertTrue(new SubQuery("SELECT j ?| array['a'] FROM t").parameters().isEmpty());
    }

    // Regression: a raw subquery whose verb is glued to a quoted name or bracket group (SELECT"a") was rejected as "not a recognized query".
    @Test
    void testRawSubQueryVerbGluedToQuotedNameIsRecognized() {
        for (final String sql : Arrays.asList("SELECT\"a\" FROM t WHERE b = ?", "SELECT[a] FROM t WHERE b = ?", "SELECT`a` FROM t WHERE b = ?",
                "SELECT'x' FROM t WHERE b = ?", "WITH\"x\" AS (SELECT 1) SELECT * FROM x WHERE b = ?")) {
            final SubQuery subQuery = new SubQuery(sql, Arrays.asList(1));
            assertEquals(sql, subQuery.rawSql(), sql);
            assertEquals(Arrays.asList(1), subQuery.parameters(), sql);

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql), sql);
            assertTrue(e.getMessage().contains("placeholder count"), e.getMessage());
        }
    }

    @Test
    void testSelectPropNamesAreDefensivelyCopiedAndUnmodifiable() {
        List<String> props = new ArrayList<>(Arrays.asList("id", "name"));
        SubQuery subQuery = Filters.subQuery("users", props, (Condition) null);

        props.set(0, "mutated");

        Collection<String> selectProps = subQuery.selectPropNames();
        assertEquals(Arrays.asList("id", "name"), selectProps);
        assertThrows(UnsupportedOperationException.class, () -> selectProps.add("email"));
        assertEquals(Arrays.asList("id", "name"), subQuery.selectPropNames());
    }

    @Test
    void testSinglePropertyStructuredConstructorsAndRawSqlAccessor() throws Exception {
        SubQuery byName = new SubQuery("users", "id", Filters.eq("active", true));
        assertEquals(Arrays.asList("id"), byName.selectPropNames());
        assertNull(byName.rawSql());

        SubQuery byClass = new SubQuery(SinglePropEntity.class, "id", Filters.eq("active", true));
        assertEquals(Arrays.asList("id"), byClass.selectPropNames());

        SubQuery raw = new SubQuery("SELECT id FROM users");
        assertEquals("SELECT id FROM users", raw.rawSql());
        assertEquals(raw.rawSql(), raw.rawSql());
        assertTrue(SubQuery.class.getConstructor(String.class, String.class).isAnnotationPresent(Deprecated.class));
    }

    @Test
    void testStructuredConditionSnapshotsMutableArrayValue() {
        final byte[] value = { 1 };
        final SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("payload", value));

        subQuery.hashCode();
        value[0] = 2;

        final SubQuery equalToSnapshot = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("payload", new byte[] { 1 }));
        assertEquals(subQuery, equalToSnapshot);
        assertEquals(subQuery.hashCode(), equalToSnapshot.hashCode());
    }

    @Test
    void testStructuredSubqueryTerminatesProjectionLineComments() {
        assertEquals("SELECT id -- trailing comment\n, name FROM users", new SubQuery("users", List.of("id -- trailing comment", "name"), null).toString());
        assertEquals("SELECT id -- trailing comment\n FROM users", new SubQuery("users", "id -- trailing comment", null).toString());
        assertEquals("SELECT \"id--suffix\" FROM \"users--archive\"", new SubQuery("\"users--archive\"", "\"id--suffix\"", null).toString());
    }

    @Test
    void testStructuredSubqueryTerminatesTableLineCommentBeforeWhere() {
        final SubQuery subQuery = new SubQuery("users -- trailing comment", "id", new Equal("active", true));

        assertEquals("SELECT id FROM users -- trailing comment\n WHERE active = true", subQuery.toString());
    }

    @Test
    void testStructuredSubQueryUnaffectedByFix() {
        // Sanity check: well-formed structured subqueries still render their FROM clause.
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "name"), null);
        String rendered = subQuery.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(rendered.contains("FROM users"), "Structured subquery must still emit FROM users; got: " + rendered);
    }

    @Test
    void testSubQueryWithExpression() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.expr("custom_function(column) > 100");

        SubQuery subQuery = Filters.subQuery("table", props, condition);

        assertNotNull(subQuery.condition());
    }

    @Test
    void testToSqlKeepsQuotedIdentifiersVerbatim() {
        final SubQuery quoted = new SubQuery("\"UserAccount\"", "\"firstName\"", Filters.equal("\"isActive\"", true));
        assertEquals("SELECT \"firstName\" FROM \"UserAccount\" WHERE \"isActive\" = true", quoted.toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("SELECT [FirstName] FROM `UserAccount`", new SubQuery("`UserAccount`", "[FirstName]", (Condition) null).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("\"UserAccount\" = 1", Filters.equal("\"UserAccount\"", 1).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("`FirstName` BETWEEN 1 AND 2", Filters.between("`FirstName`", 1, 2).toSql(NamingPolicy.SNAKE_CASE));
        // unquoted names are still converted
        assertEquals("SELECT first_name FROM user_account", new SubQuery("userAccount", "firstName", (Condition) null).toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testToStringDefault() {
        String sql = "SELECT * FROM users WHERE active = true";
        SubQuery subQuery = Filters.subQuery(sql);

        String result = subQuery.toString();

        assertEquals(sql, result);
    }

    @Test
    void testToStringForEntityClassUsesTableMetadata() {
        SubQuery subQuery = Filters.subQuery(AuditEventEntity.class, Arrays.asList("eventId"), (Condition) null);

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("SELECT event_id"));
        assertTrue(result.contains("FROM audit_log_entries ale"));
        assertFalse(result.contains("FROM audit_event_entity"));
    }

    @Test
    void testToStringRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        String result = subQuery.toSql(NamingPolicy.NO_CHANGE);

        assertEquals(sql, result);
    }

    @Test
    void testToStringStructuredSubQuery() {
        Collection<String> props = Arrays.asList("id", "name");
        Condition condition = Filters.where(Filters.eq("status", "active"));

        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("FROM"));
        assertTrue(result.contains("users"));
        assertTrue(result.contains("WHERE"));
    }

    @Test
    void testToStringStructuredWithoutCondition() {
        Collection<String> props = Arrays.asList("id");

        SubQuery subQuery = Filters.subQuery("products", props, (Condition) null);

        String result = subQuery.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("FROM"));
        assertTrue(result.contains("products"));
        assertFalse(result.contains("WHERE"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Collection<String> props = Arrays.asList("firstName", "lastName");
        Condition condition = Filters.where(Filters.eq("isActive", true));

        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("first_name") || result.contains("firstName"));
    }

    @Test
    void testToStringWithNamingPolicyConvertsSelectPropsAndEntity() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("userAccount", props, Filters.eq("isActive", true));

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("first_name, last_name"));
        assertTrue(result.contains("FROM user_account"));
        assertTrue(result.contains("is_active"));
    }

    @Test
    void testToStringWithRawSql() {
        String sql = "SELECT MAX(salary) FROM employees";
        SubQuery subQuery = Filters.subQuery(sql);

        assertEquals(sql, subQuery.toString());
    }

    @Test
    void testToStringWithStructuredQuery() {
        List<String> props = Arrays.asList("id", "name");
        Equal condition = Filters.eq("active", true);
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toString();
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id, name"));
        assertTrue(result.contains("FROM"));
        assertTrue(result.contains("users"));
        assertTrue(result.contains("WHERE"));
        assertTrue(result.contains("active"));
    }

    // Regression: a JSON operator on a column named values must not require an extra raw-subquery binding.
    @Test
    void testValuesColumnExpressionsAcceptTheirActualBindings() {
        for (final String sql : new String[] { "SELECT JSON_OBJECT('present' VALUE values ? 'k') FROM t", "SELECT JSON_OBJECT('present': values ? 'k') FROM t",
                "SELECT t. values ? 'k' FROM t", "SELECT true IS DISTINCT FROM values ? 'k' FROM t" }) {
            assertTrue(new SubQuery(sql, List.of()).parameters().isEmpty(), sql);
            assertEquals(List.of(7), new SubQuery(sql + " WHERE id = ?", List.of(7)).parameters(), sql);
        }
    }

    // Regression: the binding in H2's "VALUES ? FORMAT JSON" was read as a JSON operator, so the subquery rejected it.
    @Test
    void testValuesMarkerBeforeFormatJsonAcceptsItsBinding() {
        final SubQuery subQuery = new SubQuery("VALUES ? FORMAT JSON", List.of("[1,2]"));
        assertEquals(List.of("[1,2]"), subQuery.parameters());

        assertEquals(List.of("[2]"), new SubQuery("SELECT CAST('[1]' AS JSON) UNION ALL VALUES ? FORMAT JSON", List.of("[2]")).parameters());

        // A JSON existence test on a column named values takes no binding.
        assertTrue(new SubQuery("SELECT values ? format JSON FROM t", List.of()).parameters().isEmpty());
    }

    // Helper test class
    private static class TestEntity {
        @SuppressWarnings("unused")
        private Long id;
    }

    @Table(name = "audit_log_entries", alias = "ale")
    static class AuditEventEntity {
        private Long eventId;

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(final Long eventId) {
            this.eventId = eventId;
        }
    }

    private static final class SinglePropEntity {
        private long id;
    }
}
