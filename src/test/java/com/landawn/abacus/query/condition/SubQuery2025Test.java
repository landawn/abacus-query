package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for SubQuery.
 * Tests all public methods including constructors, getters, parameters, copying, and SQL generation.
 */
@Tag("2025")
public class SubQuery2025Test extends TestBase {

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
    public void testConstructorWrapsNonClauseConditionInWhere() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.gt("price", 100);

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        // Condition should be wrapped in WHERE
        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().getOperator());
    }

    @Test
    public void testConstructorDoesNotWrapWhereCondition() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.where(Filters.gt("price", 100));

        SubQuery subQuery = Filters.subQuery(entityName, props, condition);

        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().getOperator());
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
    public void testClearParametersForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM users");

        // Should not throw exception
        assertDoesNotThrow(() -> subQuery.clearParameters());
    }

    @Test
    public void testClearParametersWithCondition() {
        Condition condition = Filters.eq("status", "active");
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), condition);

        assertFalse(subQuery.getParameters().isEmpty());

        subQuery.clearParameters();

        List<Object> params = subQuery.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Condition condition = Filters.eq("active", true);
        SubQuery original = Filters.subQuery("users", Arrays.asList("id", "name"), condition);

        SubQuery copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getEntityName(), copy.getEntityName());
        assertEquals(original.getSelectPropNames().size(), copy.getSelectPropNames().size());
        assertNotSame(original.getSelectPropNames(), copy.getSelectPropNames());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyRawSQL() {
        SubQuery original = Filters.subQuery("SELECT * FROM users");

        SubQuery copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.sql(), copy.sql());
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
        Operator op = subQuery.getOperator();
        assertTrue(op == null || op.toString().isEmpty());
    }

    @Test
    public void testCopyWithEntityClass() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = Filters.eq("active", true);

        SubQuery original = Filters.subQuery(TestEntity.class, props, condition);
        SubQuery copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getEntityClass(), copy.getEntityClass());
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
