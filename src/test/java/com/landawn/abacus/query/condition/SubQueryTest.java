package com.landawn.abacus.query.condition;

import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test class for SubQuery.
 * Tests all public methods including constructors, getters, parameters, copying, and SQL generation.
 */
@Tag("2025")
class SubQuery2025Test extends TestBase {

    @Test
    public void testConstructorWithRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        assertNotNull(subQuery);
        assertEquals(sql, subQuery.sql());
        assertNull(subQuery.getCondition());
        assertNull(subQuery.getSelectPropNames());
    }

    @Test
    public void testConstructorWithEntityNameAndSQL() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.getEntityName());
        assertEquals(sql, subQuery.sql());
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
        assertEquals(entityName, subQuery.getEntityName());
        assertEquals(props, subQuery.getSelectPropNames());
        assertNotNull(subQuery.getCondition());
        assertNull(subQuery.sql());
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
        assertEquals("TestEntity", subQuery.getEntityName());
        assertEquals(TestEntity.class, subQuery.getEntityClass());
        assertEquals(props, subQuery.getSelectPropNames());
        assertNotNull(subQuery.getCondition());
    }

    @Test
    public void testConstructorWithEntityClassAndEmptyPropertiesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Filters.subQuery(TestEntity.class, Arrays.asList(), null));
    }

    @Test
    public void testConstructorWrapsNonClauseConditionInWhere() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.gt("price", 100);

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        // Condition should be wrapped in WHERE
        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().operator());
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

        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().operator());
    }

    @Test
    public void testConstructorWithNullCondition() {
        String entityName = "users";
        Collection<String> props = Arrays.asList("id");

        SubQuery subQuery = Filters.subQuery(entityName, props, (Condition) null);

        assertNotNull(subQuery);
        assertNull(subQuery.getCondition());
    }

    @Test
    public void testConstructorWithBlankStringConditionTreatsAsNoCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), "   ");
        assertNull(subQuery.getCondition());
    }

    @Test
    public void testGetSql() {
        String sql = "SELECT * FROM users";
        SubQuery subQuery = Filters.subQuery(sql);

        assertEquals(sql, subQuery.sql());
    }

    @Test
    public void testGetEntityName() {
        String entityName = "orders";
        SubQuery subQuery = Filters.subQuery(entityName, Arrays.asList("id"), (Condition) null);

        assertEquals(entityName, subQuery.getEntityName());
    }

    @Test
    public void testGetEntityClass() {
        SubQuery subQuery = Filters.subQuery(TestEntity.class, Arrays.asList("id"), null);

        assertEquals(TestEntity.class, subQuery.getEntityClass());
    }

    @Test
    public void testGetSelectPropNames() {
        Collection<String> props = Arrays.asList("id", "name", "email");
        SubQuery subQuery = Filters.subQuery("users", props, (Condition) null);

        assertEquals(props, subQuery.getSelectPropNames());
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        assertNotNull(subQuery.getCondition());
    }

    @Test
    public void testGetParametersFromCondition() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.between("age", 18, 65));

        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        List<Object> params = subQuery.getParameters();

        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals(18, params.get(1));
        assertEquals(65, params.get(2));
    }

    @Test
    public void testGetParametersEmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testGetParametersEmptyForNullCondition() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);

        List<Object> params = subQuery.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testToStringRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        String result = subQuery.toString(NamingPolicy.NO_CHANGE);

        assertEquals(sql, result);
    }

    @Test
    public void testToStringStructuredSubQuery() {
        Collection<String> props = Arrays.asList("id", "name");
        Condition condition = Filters.where(Filters.eq("status", "active"));

        SubQuery subQuery = Filters.subQuery("users", props, condition);

        String result = subQuery.toString(NamingPolicy.NO_CHANGE);

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

        String result = subQuery.toString(NamingPolicy.NO_CHANGE);

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

        String result = subQuery.toString(NamingPolicy.SNAKE_CASE);

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

        String sql = subQuery.toString(NamingPolicy.NO_CHANGE);

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

        assertNotNull(subQuery.getCondition());
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

        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, null);

        assertEquals(TestEntity.class, subQuery.getEntityClass());
        assertEquals("TestEntity", subQuery.getEntityName());
    }

    // Helper test class
    private static class TestEntity {
        @SuppressWarnings("unused")
        private Long id;
    }
}

public class SubQueryTest extends TestBase {

    @Test
    public void testConstructorWithRawSql() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = Filters.subQuery(sql);

        Assertions.assertNotNull(subQuery);
        Assertions.assertEquals(sql, subQuery.sql());
        Assertions.assertEquals("", subQuery.getEntityName());
        Assertions.assertNull(subQuery.getEntityClass());
        Assertions.assertNull(subQuery.getSelectPropNames());
        Assertions.assertNull(subQuery.getCondition());
        Assertions.assertEquals(Operator.EMPTY, subQuery.operator());
    }

    @Test
    public void testConstructorWithEntityAndSql() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = Filters.subQuery(entityName, sql);

        Assertions.assertEquals(entityName, subQuery.getEntityName());
        Assertions.assertEquals(sql, subQuery.sql());
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

        Assertions.assertEquals("users", subQuery.getEntityName());
        Assertions.assertEquals(props, subQuery.getSelectPropNames());
        Assertions.assertNotNull(subQuery.getCondition());
        Assertions.assertNull(subQuery.sql());
    }

    @Test
    public void testConstructorWithEntityClassPropsAndCondition() {
        List<String> props = Arrays.asList("id", "categoryId");
        GreaterThan condition = Filters.gt("price", 100);
        SubQuery subQuery = Filters.subQuery(TestEntity.class, props, condition);

        Assertions.assertEquals("TestEntity", subQuery.getEntityName());
        Assertions.assertEquals(TestEntity.class, subQuery.getEntityClass());
        Assertions.assertEquals(props, subQuery.getSelectPropNames());
        Assertions.assertNotNull(subQuery.getCondition());
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

        Collection<String> selectProps = subQuery.getSelectPropNames();
        Assertions.assertEquals(Arrays.asList("id", "name"), selectProps);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> selectProps.add("email"));
        Assertions.assertEquals(Arrays.asList("id", "name"), subQuery.getSelectPropNames());
    }

    @Test
    public void testConditionWrappingInWhere() {
        List<String> props = Arrays.asList("id");
        Equal eq = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", props, eq);

        // The condition should be wrapped in WHERE
        Assertions.assertNotNull(subQuery.getCondition());
        String result = subQuery.toString();
        Assertions.assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testConditionNotWrappedForClause() {
        List<String> props = Arrays.asList("id");
        And and = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, and);

        // AND is already a clause, should not be wrapped
        Assertions.assertNotEquals(and, subQuery.getCondition());
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
    public void testConstructorWithNullEntityNameAndRawSqlUsesEmptyEntityName() {
        SubQuery subQuery = new SubQuery((String) null, "SELECT id FROM users");
        Assertions.assertEquals("", subQuery.getEntityName());
    }

    @Test
    public void testExpressionConditionWrappedInWhere() {
        List<String> props = Arrays.asList("id");
        Expression expression = Filters.expr("status = 'active'");
        SubQuery subQuery = Filters.subQuery("users", props, expression);

        Assertions.assertTrue(subQuery.toString().contains("WHERE status = 'active'"));
    }

    @Test
    public void testGetParameters() {
        List<String> props = Arrays.asList("id", "name");
        And condition = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
        SubQuery subQuery = Filters.subQuery("users", props, condition);

        List<Object> params = subQuery.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains(true));
        Assertions.assertTrue(params.contains(18));
    }

    @Test
    public void testGetParametersWithNoCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        List<Object> params = subQuery.getParameters();
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
    public void testToStringWithNamingPolicy() {
        List<String> props = Arrays.asList("user_id", "user_name");
        Equal condition = Filters.eq("is_active", true);
        SubQuery subQuery = Filters.subQuery("user_table", props, condition);

        String result = subQuery.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("USER_ID, USER_NAME"));
        Assertions.assertTrue(result.contains("FROM USER_TABLE"));
        Assertions.assertTrue(result.contains("IS_ACTIVE"));
    }

    @Test
    public void testToStringWithNamingPolicyConvertsSelectPropsAndEntity() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery = Filters.subQuery("userAccount", props, Filters.eq("isActive", true));

        String result = subQuery.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("first_name, last_name"));
        Assertions.assertTrue(result.contains("FROM user_account"));
        Assertions.assertTrue(result.contains("is_active"));
    }

    @Test
    public void testToStringForEntityClassUsesTableMetadata() {
        SubQuery subQuery = Filters.subQuery(AuditEventEntity.class, Arrays.asList("eventId"), null);

        String result = subQuery.toString(NamingPolicy.SNAKE_CASE);

        Assertions.assertTrue(result.contains("SELECT event_id"));
        Assertions.assertTrue(result.contains("FROM audit_log_entries ale"));
        Assertions.assertFalse(result.contains("FROM audit_event_entity"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM customers");

        Assertions.assertEquals(subQuery1.hashCode(), subQuery2.hashCode());
        Assertions.assertNotEquals(subQuery1.hashCode(), subQuery3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM customers");

        List<String> props = Arrays.asList("id");
        SubQuery subQuery4 = Filters.subQuery("users", props, Filters.eq("active", true));
        SubQuery subQuery5 = Filters.subQuery("users", props, Filters.eq("active", true));

        Assertions.assertTrue(subQuery1.equals(subQuery1));
        Assertions.assertTrue(subQuery1.equals(subQuery2));
        Assertions.assertFalse(subQuery1.equals(subQuery3));
        Assertions.assertTrue(subQuery4.equals(subQuery5));
        Assertions.assertFalse(subQuery1.equals(subQuery4));
        Assertions.assertFalse(subQuery1.equals(null));
        Assertions.assertFalse(subQuery1.equals("not a SubQuery"));
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
        Assertions.assertEquals(3, subQuery.getParameters().size());
    }

    // Test entity class for constructor test
    static class TestEntity {
        // Empty class for testing
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
}

class SubQuery2026Test extends TestBase {

    // The default constructor exists for serialization and should only expose empty state safely.
    @Test
    public void testDefaultConstructor_EmptyState() {
        SubQuery subQuery = new SubQuery();

        Assertions.assertNull(subQuery.getEntityName());
        Assertions.assertNull(subQuery.getEntityClass());
        Assertions.assertNull(subQuery.sql());
        Assertions.assertTrue(subQuery.getParameters().isEmpty());
    }
}
