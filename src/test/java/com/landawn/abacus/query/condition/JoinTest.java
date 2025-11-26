package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Filters.CF;
import com.landawn.abacus.util.NamingPolicy;

public class JoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        Join join = new Join("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertEquals("products", join.getJoinEntities().get(0));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = CF.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.getOperator());
        Assertions.assertEquals("orders o", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        Condition condition = CF.eq("o.customer_id", "c.id");
        Join join = new Join(entities, condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testProtectedConstructors() {
        // Test protected constructors through a test subclass
        TestJoin join1 = new TestJoin(Operator.LEFT_JOIN, "table1");
        Assertions.assertEquals(Operator.LEFT_JOIN, join1.getOperator());
        Assertions.assertEquals("table1", join1.getJoinEntities().get(0));

        Condition condition = CF.eq("a", "b");
        TestJoin join2 = new TestJoin(Operator.RIGHT_JOIN, "table2", condition);
        Assertions.assertEquals(Operator.RIGHT_JOIN, join2.getOperator());
        Assertions.assertEquals("table2", join2.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join2.getCondition());

        List<String> entities = Arrays.asList("t1", "t2");
        TestJoin join3 = new TestJoin(Operator.FULL_JOIN, entities, condition);
        Assertions.assertEquals(Operator.FULL_JOIN, join3.getOperator());
        Assertions.assertEquals(2, join3.getJoinEntities().size());
        Assertions.assertEquals(condition, join3.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        Join join = new Join("customers c");
        List<String> entities = join.getJoinEntities();

        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("customers c", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        Condition condition = CF.eq("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);

        Condition retrieved = join.getCondition();
        Assertions.assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = CF.and(CF.eq("status", "active"), CF.gt("amount", 100));
        Join join = new Join("orders", condition);

        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
    }

    @Test
    public void testGetParametersNoCondition() {
        Join join = new Join("products");
        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Condition condition = CF.eq("status", "active");
        Join join = new Join("orders", condition);

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testClearParametersNoCondition() {
        Join join = new Join("products");

        // Should not throw exception
        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testCopy() {
        Condition condition = CF.eq("a.id", "b.a_id");
        Join original = new Join("table_b b", condition);
        Join copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.toString(), copy.toString());
    }

    @Test
    public void testCopyWithoutCondition() {
        Join original = new Join("products");
        Join copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testToString() {
        Join join = new Join("orders");
        String result = join.toString();

        Assertions.assertTrue(result.contains("JOIN"));
        Assertions.assertTrue(result.contains("orders"));
        Assertions.assertFalse(result.contains("INNER"));
        Assertions.assertFalse(result.contains("LEFT"));
    }

    @Test
    public void testToStringWithCondition() {
        Condition condition = CF.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("JOIN"));
        Assertions.assertTrue(result.contains("orders o"));
        Assertions.assertTrue(result.contains("customers.id"));
        Assertions.assertTrue(result.contains("orders.customer_id"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = CF.eq("customerId", CF.expr("orderId"));
        Join join = new Join("orderTable", condition);
        String result = join.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("JOIN"));
        Assertions.assertTrue(result.contains("orderTable"));
        Assertions.assertTrue(result.contains("CUSTOMER_ID"));
        Assertions.assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    public void testToStringWithMultipleEntities() {
        List<String> entities = Arrays.asList("t1", "t2", "t3");
        Condition condition = CF.eq("t1.id", "t2.t1_id");
        Join join = new Join(entities, condition);

        String result = join.toString();
        Assertions.assertTrue(result.contains("t1, t2, t3"));
    }

    @Test
    public void testHashCode() {
        Condition condition = CF.eq("a", "b");
        Join join1 = new Join("table", condition);
        Join join2 = new Join("table", condition);
        Join join3 = new Join("other", condition);
        Join join4 = new Join("table");

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join4.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition = CF.eq("a", "b");
        Join join1 = new Join("table", condition);
        Join join2 = new Join("table", condition);
        Join join3 = new Join("other", condition);
        Join join4 = new Join("table");

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3);
        Assertions.assertNotEquals(join1, join4);
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testEqualsWithDifferentOperators() {
        TestJoin join1 = new TestJoin(Operator.JOIN, "table");
        TestJoin join2 = new TestJoin(Operator.LEFT_JOIN, "table");

        Assertions.assertNotEquals(join1, join2);
    }

    // Test subclass to access protected constructors
    private static class TestJoin extends Join {
        public TestJoin(Operator operator, String joinEntity) {
            super(operator, joinEntity);
        }

        public TestJoin(Operator operator, String joinEntity, Condition condition) {
            super(operator, joinEntity, condition);
        }

        public TestJoin(Operator operator, Collection<String> joinEntities, Condition condition) {
            super(operator, joinEntities, condition);
        }
    }
}