package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class SubQueryTest extends TestBase {

    @Test
    public void testConstructorWithRawSql() {
        String sql = "SELECT id FROM users WHERE status = 'active'";
        SubQuery subQuery = CF.subQuery(sql);
        
        Assertions.assertNotNull(subQuery);
        Assertions.assertEquals(sql, subQuery.getSql());
        Assertions.assertEquals("", subQuery.getEntityName());
        Assertions.assertNull(subQuery.getEntityClass());
        Assertions.assertNull(subQuery.getSelectPropNames());
        Assertions.assertNull(subQuery.getCondition());
        Assertions.assertEquals(Operator.EMPTY, subQuery.getOperator());
    }

    @Test
    public void testConstructorWithEntityAndSql() {
        String entityName = "orders";
        String sql = "SELECT order_id FROM orders WHERE total > 1000";
        SubQuery subQuery = CF.subQuery(entityName, sql);
        
        Assertions.assertEquals(entityName, subQuery.getEntityName());
        Assertions.assertEquals(sql, subQuery.getSql());
    }

    @Test
    public void testConstructorWithEmptySql() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.subQuery("");
        });
    }

    @Test
    public void testConstructorWithNullSql() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.subQuery((String) null);
        });
    }

    @Test
    public void testConstructorWithEntityPropsAndCondition() {
        List<String> props = Arrays.asList("id", "email");
        Equal condition = CF.eq("active", true);
        SubQuery subQuery = CF.subQuery("users", props, condition);
        
        Assertions.assertEquals("users", subQuery.getEntityName());
        Assertions.assertEquals(props, subQuery.getSelectPropNames());
        Assertions.assertNotNull(subQuery.getCondition());
        Assertions.assertNull(subQuery.getSql());
    }

    @Test
    public void testConstructorWithEntityClassPropsAndCondition() {
        List<String> props = Arrays.asList("id", "categoryId");
        GreaterThan condition = CF.gt("price", 100);
        SubQuery subQuery = CF.subQuery(TestEntity.class, props, condition);
        
        Assertions.assertEquals("TestEntity", subQuery.getEntityName());
        Assertions.assertEquals(TestEntity.class, subQuery.getEntityClass());
        Assertions.assertEquals(props, subQuery.getSelectPropNames());
        Assertions.assertNotNull(subQuery.getCondition());
    }

    @Test
    public void testConditionWrappingInWhere() {
        List<String> props = Arrays.asList("id");
        Equal eq = CF.eq("status", "active");
        SubQuery subQuery = CF.subQuery("users", props, eq);
        
        // The condition should be wrapped in WHERE
        Assertions.assertNotNull(subQuery.getCondition());
        String result = subQuery.toString();
        Assertions.assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testConditionNotWrappedForClause() {
        List<String> props = Arrays.asList("id");
        And and = CF.and(CF.eq("active", true), CF.gt("age", 18));
        SubQuery subQuery = CF.subQuery("users", props, and);
        
        // AND is already a clause, should not be wrapped
        Assertions.assertNotEquals(and, subQuery.getCondition());
    }

    @Test
    public void testGetParameters() {
        List<String> props = Arrays.asList("id", "name");
        And condition = CF.and(CF.eq("active", true), CF.gt("age", 18));
        SubQuery subQuery = CF.subQuery("users", props, condition);
        
        List<Object> params = subQuery.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains(true));
        Assertions.assertTrue(params.contains(18));
    }

    @Test
    public void testGetParametersWithNoCondition() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM users");
        
        List<Object> params = subQuery.getParameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        List<String> props = Arrays.asList("id");
        Equal condition = CF.eq("status", "active");
        SubQuery subQuery = CF.subQuery("users", props, condition);
        
        subQuery.clearParameters();
        
        // Verify condition parameters are cleared
        Assertions.assertTrue(condition.getParameters().isEmpty() || 
                           condition.getParameters().get(0) == null);
    }

    @Test
    public void testClearParametersWithNoCondition() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM users");
        
        // Should not throw exception
        subQuery.clearParameters();
    }

    @Test
    public void testCopy() {
        List<String> props = Arrays.asList("id", "name");
        Equal condition = CF.eq("active", true);
        SubQuery original = CF.subQuery("users", props, condition);
        
        SubQuery copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getEntityName(), copy.getEntityName());
        Assertions.assertNotSame(original.getSelectPropNames(), copy.getSelectPropNames());
        Assertions.assertEquals(original.getSelectPropNames(), copy.getSelectPropNames());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithRawSql() {
        SubQuery original = CF.subQuery("SELECT id FROM users");
        
        SubQuery copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getSql(), copy.getSql());
    }

    @Test
    public void testToStringWithRawSql() {
        String sql = "SELECT MAX(salary) FROM employees";
        SubQuery subQuery = CF.subQuery(sql);
        
        Assertions.assertEquals(sql, subQuery.toString());
    }

    @Test
    public void testToStringWithStructuredQuery() {
        List<String> props = Arrays.asList("id", "name");
        Equal condition = CF.eq("active", true);
        SubQuery subQuery = CF.subQuery("users", props, condition);
        
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
        Equal condition = CF.eq("is_active", true);
        SubQuery subQuery = CF.subQuery("user_table", props, condition);
        
        String result = subQuery.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("user_id, user_name"));
        Assertions.assertTrue(result.contains("user_table"));
        Assertions.assertTrue(result.contains("IS_ACTIVE"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM customers");
        
        Assertions.assertEquals(subQuery1.hashCode(), subQuery2.hashCode());
        Assertions.assertNotEquals(subQuery1.hashCode(), subQuery3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM customers");
        
        List<String> props = Arrays.asList("id");
        SubQuery subQuery4 = CF.subQuery("users", props, CF.eq("active", true));
        SubQuery subQuery5 = CF.subQuery("users", props, CF.eq("active", true));
        
        Assertions.assertTrue(subQuery1.equals(subQuery1));
        Assertions.assertTrue(subQuery1.equals(subQuery2));
        Assertions.assertFalse(subQuery1.equals(subQuery3));
        Assertions.assertTrue(subQuery4.equals(subQuery5));
        Assertions.assertFalse(subQuery1.equals(subQuery4));
        Assertions.assertFalse(subQuery1.equals(null));
        Assertions.assertFalse(subQuery1.equals("not a SubQuery"));
    }

    @Test
    public void testComplexSubQuery() {
        List<String> props = Arrays.asList("id", "email", "created");
        And complexCondition = CF.and(
            CF.eq("active", true),
            CF.gt("created", "2023-01-01"),
            CF.like("email", "%@company.com")
        );
        
        SubQuery subQuery = CF.subQuery("users", props, complexCondition);
        
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
}