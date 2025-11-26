package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Filters.CF;
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
        SubQuery subQuery = new SubQuery(sql);

        assertNotNull(subQuery);
        assertEquals(sql, subQuery.getSql());
        assertNull(subQuery.getCondition());
        assertNull(subQuery.getSelectPropNames());
    }

    @Test
    public void testConstructorWithEntityNameAndSQL() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = new SubQuery(entityName, sql);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.getEntityName());
        assertEquals(sql, subQuery.getSql());
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
        Condition condition = CF.eq("active", true);

        SubQuery subQuery = new SubQuery(entityName, props, condition);

        assertNotNull(subQuery);
        assertEquals(entityName, subQuery.getEntityName());
        assertEquals(props, subQuery.getSelectPropNames());
        assertNotNull(subQuery.getCondition());
        assertNull(subQuery.getSql());
    }

    @Test
    public void testConstructorWithEntityClassPropertiesCondition() {
        Collection<String> props = Arrays.asList("id", "name");
        Condition condition = CF.like("name", "%test%");

        SubQuery subQuery = new SubQuery(TestEntity.class, props, condition);

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
        Condition condition = CF.gt("price", 100);

        SubQuery subQuery = new SubQuery(entityName, props, condition);

        // Condition should be wrapped in WHERE
        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().getOperator());
    }

    @Test
    public void testConstructorDoesNotWrapWhereCondition() {
        String entityName = "products";
        Collection<String> props = Arrays.asList("id");
        Condition condition = CF.where(CF.gt("price", 100));

        SubQuery subQuery = new SubQuery(entityName, props, condition);

        assertNotNull(subQuery.getCondition());
        assertEquals(Operator.WHERE, subQuery.getCondition().getOperator());
    }

    @Test
    public void testConstructorWithNullCondition() {
        String entityName = "users";
        Collection<String> props = Arrays.asList("id");

        SubQuery subQuery = new SubQuery(entityName, props, null);

        assertNotNull(subQuery);
        assertNull(subQuery.getCondition());
    }

    @Test
    public void testGetSql() {
        String sql = "SELECT * FROM users";
        SubQuery subQuery = new SubQuery(sql);

        assertEquals(sql, subQuery.getSql());
    }

    @Test
    public void testGetEntityName() {
        String entityName = "orders";
        SubQuery subQuery = new SubQuery(entityName, Arrays.asList("id"), null);

        assertEquals(entityName, subQuery.getEntityName());
    }

    @Test
    public void testGetEntityClass() {
        SubQuery subQuery = new SubQuery(TestEntity.class, Arrays.asList("id"), null);

        assertEquals(TestEntity.class, subQuery.getEntityClass());
    }

    @Test
    public void testGetSelectPropNames() {
        Collection<String> props = Arrays.asList("id", "name", "email");
        SubQuery subQuery = new SubQuery("users", props, null);

        assertEquals(props, subQuery.getSelectPropNames());
    }

    @Test
    public void testGetCondition() {
        Condition condition = CF.eq("status", "active");
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), condition);

        assertNotNull(subQuery.getCondition());
    }

    @Test
    public void testGetParametersFromCondition() {
        Condition condition = CF.and(CF.eq("status", "active"), CF.between("age", 18, 65));

        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), condition);

        List<Object> params = subQuery.getParameters();

        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals(18, params.get(1));
        assertEquals(65, params.get(2));
    }

    @Test
    public void testGetParametersEmptyForRawSQL() {
        SubQuery subQuery = new SubQuery("SELECT * FROM users");

        List<Object> params = subQuery.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testGetParametersEmptyForNullCondition() {
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), null);

        List<Object> params = subQuery.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testClearParametersForRawSQL() {
        SubQuery subQuery = new SubQuery("SELECT * FROM users");

        // Should not throw exception
        assertDoesNotThrow(() -> subQuery.clearParameters());
    }

    @Test
    public void testClearParametersWithCondition() {
        Condition condition = CF.eq("status", "active");
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), condition);

        assertFalse(subQuery.getParameters().isEmpty());

        subQuery.clearParameters();

        List<Object> params = subQuery.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Condition condition = CF.eq("active", true);
        SubQuery original = new SubQuery("users", Arrays.asList("id", "name"), condition);

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
        SubQuery original = new SubQuery("SELECT * FROM users");

        SubQuery copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getSql(), copy.getSql());
    }

    @Test
    public void testToStringRawSQL() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = new SubQuery(sql);

        String result = subQuery.toString(NamingPolicy.NO_CHANGE);

        assertEquals(sql, result);
    }

    @Test
    public void testToStringStructuredSubQuery() {
        Collection<String> props = Arrays.asList("id", "name");
        Condition condition = CF.where(CF.eq("status", "active"));

        SubQuery subQuery = new SubQuery("users", props, condition);

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

        SubQuery subQuery = new SubQuery("products", props, null);

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
        Condition condition = CF.where(CF.eq("isActive", true));

        SubQuery subQuery = new SubQuery("users", props, condition);

        String result = subQuery.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertTrue(result.contains("first_name") || result.contains("firstName"));
    }

    @Test
    public void testHashCode() {
        SubQuery sq1 = new SubQuery("SELECT * FROM users");
        SubQuery sq2 = new SubQuery("SELECT * FROM users");

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    public void testHashCodeStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = CF.eq("active", true);

        SubQuery sq1 = new SubQuery("users", props, condition);
        SubQuery sq2 = new SubQuery("users", props, condition);

        assertEquals(sq1.hashCode(), sq2.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery sq1 = new SubQuery("SELECT * FROM users");
        SubQuery sq2 = new SubQuery("SELECT * FROM users");

        assertEquals(sq1, sq2);
    }

    @Test
    public void testEqualsSameInstance() {
        SubQuery sq = new SubQuery("SELECT * FROM users");

        assertEquals(sq, sq);
    }

    @Test
    public void testEqualsNull() {
        SubQuery sq = new SubQuery("SELECT * FROM users");

        assertNotEquals(sq, null);
    }

    @Test
    public void testEqualsDifferentType() {
        SubQuery sq = new SubQuery("SELECT * FROM users");

        assertNotEquals(sq, "not a subquery");
    }

    @Test
    public void testEqualsDifferentSQL() {
        SubQuery sq1 = new SubQuery("SELECT * FROM users");
        SubQuery sq2 = new SubQuery("SELECT * FROM orders");

        assertNotEquals(sq1, sq2);
    }

    @Test
    public void testEqualsStructured() {
        Collection<String> props = Arrays.asList("id");
        Condition condition = CF.eq("active", true);

        SubQuery sq1 = new SubQuery("users", props, condition);
        SubQuery sq2 = new SubQuery("users", props, condition);

        assertEquals(sq1, sq2);
    }

    @Test
    public void testEqualsStructuredDifferentEntity() {
        Collection<String> props = Arrays.asList("id");

        SubQuery sq1 = new SubQuery("users", props, null);
        SubQuery sq2 = new SubQuery("orders", props, null);

        assertNotEquals(sq1, sq2);
    }

    @Test
    public void testComplexSubQueryWithMultipleProperties() {
        Collection<String> props = Arrays.asList("id", "name", "email", "age");
        Condition condition = CF.and(CF.eq("status", "active"), CF.between("age", 18, 65));

        SubQuery subQuery = new SubQuery("users", props, condition);

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
        Condition condition = CF.expr("custom_function(column) > 100");

        SubQuery subQuery = new SubQuery("table", props, condition);

        assertNotNull(subQuery.getCondition());
    }

    // Helper test class
    private static class TestEntity {
        @SuppressWarnings("unused")
        private Long id;
    }
}
