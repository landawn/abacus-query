package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class LeftJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        LeftJoin join = new LeftJoin("orders");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.LEFT_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertEquals("orders", join.getJoinEntities().get(0));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndAlias() {
        LeftJoin join = new LeftJoin("orders o");

        Assertions.assertNotNull(join);
        Assertions.assertEquals("orders o", join.getJoinEntities().get(0));
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        LeftJoin join = new LeftJoin("orders", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.LEFT_JOIN, join.operator());
        Assertions.assertEquals("orders", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition condition = Filters.and(Filters.eq("c.customer_id", "o.customer_id"), Filters.eq("o.status", "active"));
        LeftJoin join = new LeftJoin("orders o", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals("orders o", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        Condition condition = Filters.and(Filters.eq("c.id", "o.customer_id"), Filters.eq("o.id", "oi.order_id"));
        LeftJoin join = new LeftJoin(entities, condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        LeftJoin join = new LeftJoin("products p");
        List<String> entities = join.getJoinEntities();

        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("products p", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("a.id", "b.a_id");
        LeftJoin join = new LeftJoin("table_b b", condition);

        Condition retrieved = join.getCondition();
        Assertions.assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = Filters.and(Filters.eq("o.customer_id", Filters.expr("c.id")), Filters.eq("o.status", "completed"));
        LeftJoin join = new LeftJoin("orders o", condition);

        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("completed", params.get(0));
    }

    @Test
    public void testGetParametersNoCondition() {
        LeftJoin join = new LeftJoin("orders");
        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Condition condition = Filters.eq("status", "active");
        LeftJoin join = new LeftJoin("orders", condition);

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        Condition condition = Filters.eq("a.id", "b.a_id");
        LeftJoin original = new LeftJoin("table_b b", condition);
        LeftJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.toString(), copy.toString());
    }

    @Test
    public void testToString() {
        LeftJoin join = new LeftJoin("orders");
        String result = join.toString();

        Assertions.assertTrue(result.contains("LEFT JOIN"));
        Assertions.assertTrue(result.contains("orders"));
    }

    @Test
    public void testToStringWithCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        LeftJoin join = new LeftJoin("orders o", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("LEFT JOIN"));
        Assertions.assertTrue(result.contains("orders o"));
        Assertions.assertTrue(result.contains("customers.id"));
        Assertions.assertTrue(result.contains("orders.customer_id"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("customerId", Filters.expr("orderId"));
        LeftJoin join = new LeftJoin("orderTable", condition);
        String result = join.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("LEFT JOIN"));
        Assertions.assertTrue(result.contains("orderTable"));
        Assertions.assertTrue(result.contains("CUSTOMER_ID"));
        Assertions.assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    public void testHashCode() {
        Condition condition = Filters.eq("a", "b");
        LeftJoin join1 = new LeftJoin("table", condition);
        LeftJoin join2 = new LeftJoin("table", condition);
        LeftJoin join3 = new LeftJoin("other", condition);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition = Filters.eq("a", "b");
        LeftJoin join1 = new LeftJoin("table", condition);
        LeftJoin join2 = new LeftJoin("table", condition);
        LeftJoin join3 = new LeftJoin("other", condition);
        LeftJoin join4 = new LeftJoin("table");

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3);
        Assertions.assertNotEquals(join1, join4);
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testTypicalUseCases() {
        // Find all customers, including those without orders
        LeftJoin customerOrders = new LeftJoin("orders o", Filters.eq("c.customer_id", Filters.expr("o.customer_id")));

        String result = customerOrders.toString();
        Assertions.assertTrue(result.contains("LEFT JOIN orders o"));
        Assertions.assertTrue(result.contains("c.customer_id = o.customer_id"));

        // Include optional data
        LeftJoin optionalData = new LeftJoin("customer_preferences cp", Filters.eq("c.id", "cp.customer_id"));

        result = optionalData.toString();
        Assertions.assertTrue(result.contains("LEFT JOIN customer_preferences cp"));
    }
}