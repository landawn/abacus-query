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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for SubQuery.
 * Tests all public methods including constructors, getters, parameters, copying, and SQL generation.
 */
@Tag("2025")
public class SubQueryTest extends TestBase {
    @Test
    public void testConstructorWithRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        assertNotNull(subQuery);
        assertEquals(sql, subQuery.rawSql());
        assertNull(subQuery.condition());
        assertNull(subQuery.selectPropNames());
    }

    @Test
    public void testConstructorWithEntityNameAndSQL() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.entityName());
        assertEquals(sql, subQuery.rawSql());
    }

    @Test
    public void testConstructorWithEmptySQLThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SubQuery("");
        });
    }

    @Test
    public void testConstructorWithNullSQLThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SubQuery((String) null);
        });
    }

    @Test
    public void testConstructorWithEntityNamePropertiesCondition() {
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
    public void testConstructorWithEntityNameAndEmptyPropertiesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList(), (Condition) null));
    }

    @Test
    public void testConstructorWithEntityClassPropertiesCondition() {
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
    public void testConstructorWithEntityClassAndEmptyPropertiesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList(), (Condition) null));
    }

    @Test
    public void testConstructorWrapsNonClauseConditionInWhere() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.gt("price", 100);

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        // Condition should be wrapped in WHERE
        assertNotNull(subQuery.condition());
        assertEquals(Operator.WHERE, subQuery.condition().operator());
    }

    @Test
    public void testConstructorRejectsOnCondition() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("products", Arrays.asList("id"), Filters.on("a", "b")));
    }

    @Test
    public void testConstructorRejectsUsingCondition() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("products", Arrays.asList("id"), Filters.using("id")));
    }

    @Test
    public void testConstructorDoesNotWrapWhereCondition() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.where(Filters.gt("price", 100));

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        assertNotNull(subQuery.condition());
        assertEquals(Operator.WHERE, subQuery.condition().operator());
    }

    @Test
    public void testConstructorDoesNotDoubleWrapWhereExpression() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.expr("WHERE active = true"));

        assertEquals("SELECT id FROM users WHERE active = true", subQuery.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testConstructorWithNullCondition() {
        String entityName = "users";
        Collection<String> props = Arrays.asList("id");

        SubQuery subQuery = Filters.subQuery(entityName, props, (Condition) null);

        assertNotNull(subQuery);
        assertNull(subQuery.condition());
    }

    @Test
    public void testConstructorWithBlankStringConditionTreatsAsNoCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), "   ");
        assertNull(subQuery.condition());
    }

    @Test
    public void testGetSql() {
        String sql = "SELECT * FROM users";
        SubQuery subQuery = Filters.subQuery(sql);

        assertEquals(sql, subQuery.rawSql());
    }

    @Test
    public void testGetEntityName() {
        String entityName = "orders";
        SubQuery subQuery = Filters.subQuery(entityName, Arrays.asList("id"), (Condition) null);

        assertEquals(entityName, subQuery.entityName());
    }

    @Test
    public void testGetEntityClass() {
        SubQuery subQuery = Filters.subQuery(TestEntity.class, Arrays.asList("id"), (Condition) null);

        assertEquals(TestEntity.class, subQuery.entityClass());
    }

    @Test
    public void testGetSelectPropNames() {
        Collection<String> props = Arrays.asList("id", "name", "email");
        SubQuery subQuery = Filters.subQuery("users", props, (Condition) null);

        assertEquals(props, subQuery.selectPropNames());
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        assertNotNull(subQuery.condition());
    }

    @Test
    public void testParametersFromCondition() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.between("age", 18, 65));

        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        List<Object> params = subQuery.parameters();

        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals(18, params.get(1));
        assertEquals(65, params.get(2));
    }

    @Test
    public void testParametersEmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.parameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testParametersEmptyForNullCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);

        List<Object> params = subQuery.parameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testToStringRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        String result = subQuery.toSql(NamingPolicy.NO_CHANGE);

        assertEquals(sql, result);
    }

    @Test
    public void testToStringStructuredSubQuery() {
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
    public void testToStringStructuredWithoutCondition() {
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
    public void testToStringWithNamingPolicy() {
        Collection<String> props = Arrays.asList("firstName", "lastName");
        Condition condition = Filters.where(Filters.eq("isActive", true));

        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("first_name") || result.contains("firstName"));
    }

    @Test
    public void testHashCode() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    public void testHashCodeStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.eq("active", true);

        SubQuery sq1 = Filters.subQuery("users", props, condition);
        SubQuery sq2 = Filters.subQuery("users", props, condition);

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq1, sq2);
    }

    @Test
    public void testEqualsSameInstance() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertEquals(sq, sq);
    }

    @Test
    public void testEqualsNull() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertNotEquals(sq, null);
    }

    @Test
    public void testEqualsDifferentType() {
        SubQuery sq = Filters.subQuery("SELECT * FROM users");

        assertNotEquals(sq, "not a subquery");
    }

    @Test
    public void testEqualsDifferentSQL() {
        SubQuery sq1 = Filters.subQuery("SELECT * FROM users");
        SubQuery sq2 = Filters.subQuery("SELECT * FROM orders");

        assertNotEquals(sq1, sq2);
    }

    @Test
    public void testEqualsStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.eq("active", true);

        SubQuery sq1 = Filters.subQuery("users", props, condition);
        SubQuery sq2 = Filters.subQuery("users", props, condition);

        assertEquals(sq1, sq2);
    }

    @Test
    public void testEqualsStructuredDifferentEntity() {
        Collection<String> props = Arrays.asList("id");

        SubQuery sq1 = Filters.subQuery("users", props, (Condition) null);
        SubQuery sq2 = Filters.subQuery("orders", props, (Condition) null);

        assertNotEquals(sq1, sq2);
    }

    @Test
    public void testComplexSubQueryWithMultipleProperties() {
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
    public void testSubQueryWithExpression() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.expr("custom_function(column) > 100");

        SubQuery subQuery = Filters.subQuery("table", props, condition);

        assertNotNull(subQuery.condition());
    }

    @Test
    public void testToStringDefault() {
        String sql = "SELECT * FROM users WHERE active = true";
        SubQuery subQuery = Filters.subQuery(sql);

        String result = subQuery.toString();

        assertEquals(sql, result);
    }

    @Test
    public void testGetOperatorReturnsEmptyOrNull() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        // SubQuery doesn't have a specific operator, verify it returns empty or null
        Operator op = subQuery.operator();
        assertTrue(op == null || op.toString().isEmpty());
    }

    @Test
    public void testConstructorWithEntityNameAndEntityClass() {
        Collection<String> props = Arrays.asList("id", "name");

        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, (Condition) null);

        assertEquals(TestEntity.class, subQuery.entityClass());
        assertEquals("TestEntity", subQuery.entityName());
    }

    // Helper test class
    private static class TestEntity {
        @SuppressWarnings("unused")
        private Long id;
    }

    @Test
    public void testConstructorWithRawSql() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        Assertions.assertNotNull(subQuery);
        Assertions.assertEquals(sql, subQuery.rawSql());
        Assertions.assertEquals("", subQuery.entityName());
        Assertions.assertNull(subQuery.entityClass());
        Assertions.assertNull(subQuery.selectPropNames());
        Assertions.assertNull(subQuery.condition());
        Assertions.assertEquals(Operator.EMPTY, subQuery.operator());
    }

    @Test
    public void testConstructorWithEntityAndSql() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        Assertions.assertEquals(entityName, subQuery.entityName());
        Assertions.assertEquals(sql, subQuery.rawSql());
    }

    @Test
    public void testConstructorWithEmptySql() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.subQuery("");
        });
    }

    @Test
    public void testConstructorWithNullSql() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.subQuery((String) null);
        });
    }

    @Test
    public void testConstructorWithEntityPropsAndCondition() {
        List<String> props = Arrays.asList("id", "email");
        Equal condition = Filters.eq("active", true);
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        Assertions.assertEquals("users", subQuery.entityName());
        Assertions.assertEquals(props, subQuery.selectPropNames());
        Assertions.assertNotNull(subQuery.condition());
        Assertions.assertNull(subQuery.rawSql());
    }

    @Test
    public void testConstructorWithEntityClassPropsAndCondition() {
        List<String> props = Arrays.asList("id", "categoryId");
        GreaterThan condition = Filters.gt("price", 100);
        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, condition);

        Assertions.assertEquals("TestEntity", subQuery.entityName());
        Assertions.assertEquals(TestEntity.class, subQuery.entityClass());
        Assertions.assertEquals(props, subQuery.selectPropNames());
        Assertions.assertNotNull(subQuery.condition());
    }

    @Test
    public void testConstructorRejectsInvalidSelectPropNames() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", null), (Condition) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", ""), (Condition) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList("id", null), (Condition) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList("id", ""), (Condition) null));
    }

    @Test
    public void testSelectPropNamesAreDefensivelyCopiedAndUnmodifiable() {
        List<String> props = new ArrayList<>(Arrays.asList("id", "name"));
        SubQuery subQuery = Filters.subQuery("users", props, (Condition) null);

        props.set(0, "mutated");

        Collection<String> selectProps = subQuery.selectPropNames();
        Assertions.assertEquals(Arrays.asList("id", "name"), selectProps);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> selectProps.add("email"));
        Assertions.assertEquals(Arrays.asList("id", "name"), subQuery.selectPropNames());
    }

    @Test
    public void testConditionWrappingInWhere() {
        List<String> props = Arrays.asList("id");
        Equal eq = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", props, eq);

        // The condition should be wrapped in WHERE
        Assertions.assertNotNull(subQuery.condition());
        String result = subQuery.toString();
        Assertions.assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testConditionNotWrappedForClause() {
        List<String> props = Arrays.asList("id");
        And and = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, and);

        // AND is already a clause, should not be wrapped
        Assertions.assertNotEquals(and, subQuery.condition());
    }

    @Test
    public void testCriteriaConditionNotDoubleWrappedInWhere() {
        List<String> props = Arrays.asList("id");
        Criteria criteria = Criteria.builder().where(Filters.eq("active", true)).build();
        SubQuery subQuery = Filters.subQuery("users", props, criteria);

        String result = subQuery.toString();
        Assertions.assertFalse(result.contains("WHERE  WHERE"));
        Assertions.assertFalse(result.contains("WHERE WHERE"));
        Assertions.assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testEmptyCriteriaIsNormalizedToNoCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Criteria.builder().build());

        Assertions.assertNull(subQuery.condition());
        Assertions.assertEquals("SELECT id FROM users", subQuery.toString());
    }

    @Test
    public void testConstructorRejectsCriteriaConditionWithSelectModifier() {
        Criteria criteria = Criteria.builder().distinct().where(Filters.eq("active", true)).build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id"), criteria));
    }

    @Test
    public void testConstructorWithNullEntityNameAndRawSqlUsesEmptyEntityName() {
        SubQuery subQuery = new SubQuery((String) null, "SELECT id FROM users");
        Assertions.assertEquals("", subQuery.entityName());
    }

    @Test
    public void testExpressionConditionWrappedInWhere() {
        List<String> props = Arrays.asList("id");
        SqlExpression expression = Filters.expr("status = 'active'");
        SubQuery subQuery = Filters.subQuery("users", props, expression);

        Assertions.assertTrue(subQuery.toString().contains("WHERE status = 'active'"));
    }

    @Test
    public void testParameters() {
        List<String> props = Arrays.asList("id", "name");
        And condition = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        List<Object> params = subQuery.parameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains(true));
        Assertions.assertTrue(params.contains(18));
    }

    @Test
    public void testParametersWithNoCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.parameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testToStringWithRawSql() {
        String sql = "SELECT MAX(salary) FROM employees";
        SubQuery subQuery = Filters.subQuery(sql);

        Assertions.assertEquals(sql, subQuery.toString());
    }

    @Test
    public void testToStringWithStructuredQuery() {
        List<String> props = Arrays.asList("id", "name");
        Equal condition = Filters.eq("active", true);
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toString();
        Assertions.assertTrue(result.contains("SELECT"));
        Assertions.assertTrue(result.contains("id, name"));
        Assertions.assertTrue(result.contains("FROM"));
        Assertions.assertTrue(result.contains("users"));
        Assertions.assertTrue(result.contains("WHERE"));
        Assertions.assertTrue(result.contains("active"));
    }

    @Test
    public void testToStringWithNamingPolicyConvertsSelectPropsAndEntity() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("userAccount", props, Filters.eq("isActive", true));

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("first_name, last_name"));
        Assertions.assertTrue(result.contains("FROM user_account"));
        Assertions.assertTrue(result.contains("is_active"));
    }

    @Test
    public void testToStringForEntityClassUsesTableMetadata() {
        SubQuery subQuery = Filters.subQuery(AuditEventEntity.class, Arrays.asList("eventId"), (Condition) null);

        String result = subQuery.toSql(NamingPolicy.SNAKE_CASE);

        Assertions.assertTrue(result.contains("SELECT event_id"));
        Assertions.assertTrue(result.contains("FROM audit_log_entries ale"));
        Assertions.assertFalse(result.contains("FROM audit_event_entity"));
    }

    @Test
    public void testEqualsAndHashCodeIncludeEntityClass() {
        List<String> props = Arrays.asList("id");
        SubQuery classBased = Filters.subQuery(TestEntity.class, props, Filters.eq("active", true));
        SubQuery nameBased = Filters.subQuery("TestEntity", props, Filters.eq("active", true));

        Assertions.assertNotEquals(classBased, nameBased);
        Assertions.assertNotEquals(classBased.hashCode(), nameBased.hashCode());
    }

    @Test
    public void testComplexSubQuery() {
        List<String> props = Arrays.asList("id", "email", "created");
        And complexCondition = Filters.and(Filters.eq("active", true), Filters.gt("created", "2023-01-01"), Filters.like("email", "%@company.com"));

        SubQuery subQuery = Filters.subQuery("users", props, complexCondition);

        String result = subQuery.toString();
        Assertions.assertTrue(result.contains("SELECT"));
        Assertions.assertTrue(result.contains("id, email, created"));
        Assertions.assertTrue(result.contains("FROM users"));
        Assertions.assertEquals(3, subQuery.parameters().size());
    }

    @Test
    public void testConstructorRejectsWhitespaceOnlyRawSql() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", "   "));
    }

    @Test
    public void testConstructorRejectsBlankEntityAndPropertyNames() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("   ", Arrays.asList("id"), (Condition) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id", "   "), (Condition) null));
    }

    @Test
    public void testConstructorRejectsRawOnUsingExpressions() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Filters.subQuery("users", Arrays.asList("id"), Filters.expr("ON users.id = orders.user_id")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.subQuery("users", Arrays.asList("id"), Filters.expr("USING (id)")));
    }

    @Test
    public void testConstructorRejectsCollectionThatBecomesEmptyWhileSnapshotting() {
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

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", unstablePropNames, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SubQuery(TestEntity.class, unstablePropNames, null));
    }

    @Test
    public void testConstructorRejectsStandaloneNonPredicateOperands() {
        SubQuery nested = new SubQuery("SELECT id FROM accounts");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", Arrays.asList("id"), nested));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SubQuery("users", Arrays.asList("id"), new Some(nested)));

        // EXISTS is a complete predicate and remains valid even though it owns a subquery.
        SubQuery valid = new SubQuery("users", Arrays.asList("id"), new NotExists(nested));
        Assertions.assertEquals("SELECT id FROM users WHERE NOT EXISTS (SELECT id FROM accounts)", valid.toString());
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

    // The default constructor exists for serialization and should only expose empty state safely.
    @Test
    public void testDefaultConstructor_EmptyState() {
        SubQuery subQuery = new SubQuery();

        Assertions.assertNull(subQuery.entityName());
        Assertions.assertNull(subQuery.entityClass());
        Assertions.assertNull(subQuery.rawSql());
        Assertions.assertTrue(subQuery.parameters().isEmpty());
    }

    @Test
    public void testDefaultConstructorToStringDoesNotEmitFromNull() {
        // Bug fix: previously rendered "SELECT * FROM null" when the default Kryo constructor
        // left entityName/entityClass null. That is invalid SQL containing the literal "null"
        // for the table name. The FROM clause should be omitted entirely in that case.
        SubQuery subQuery = new SubQuery();

        String rendered = subQuery.toSql(NamingPolicy.NO_CHANGE);
        Assertions.assertFalse(rendered.contains("FROM null"), "toString() must not include 'FROM null'; got: " + rendered);
        Assertions.assertFalse(rendered.contains("from null"), "toString() must not include 'from null'; got: " + rendered);
        // The rendered string should look like a benign placeholder, e.g. just "SELECT *".
        Assertions.assertTrue(rendered.startsWith("SELECT"), "Should still start with SELECT");
    }

    @Test
    public void testStructuredSubQueryUnaffectedByFix() {
        // Sanity check: well-formed structured subqueries still render their FROM clause.
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "name"), null);
        String rendered = subQuery.toSql(NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(rendered.contains("FROM users"), "Structured subquery must still emit FROM users; got: " + rendered);
    }

    /**
     * Second-pass locking test: selectPropNames returns an unmodifiable view of the
     * underlying list (callers cannot mutate the SubQuery's internal state).
     */
    @Test
    public void testGetSelectPropNamesIsUnmodifiable() {
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "name"), null);

        java.util.Collection<String> props = subQuery.selectPropNames();

        Assertions.assertEquals(2, props.size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> props.add("extra"));
    }

    @Test
    public void testSinglePropertyStructuredConstructorsAndRawSqlAccessor() throws Exception {
        SubQuery byName = new SubQuery("users", "id", Filters.eq("active", true));
        Assertions.assertEquals(Arrays.asList("id"), byName.selectPropNames());
        Assertions.assertNull(byName.rawSql());

        SubQuery byClass = new SubQuery(SinglePropEntity.class, "id", Filters.eq("active", true));
        Assertions.assertEquals(Arrays.asList("id"), byClass.selectPropNames());

        SubQuery raw = new SubQuery("SELECT id FROM users");
        Assertions.assertEquals("SELECT id FROM users", raw.rawSql());
        Assertions.assertEquals(raw.rawSql(), raw.rawSql());
        Assertions.assertTrue(SubQuery.class.getConstructor(String.class, String.class).isAnnotationPresent(Deprecated.class));
    }

    private static final class SinglePropEntity {
        private long id;
    }
}
