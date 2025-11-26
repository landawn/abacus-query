package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class InnerJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        InnerJoin join = new InnerJoin("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.INNER_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertEquals("products", join.getJoinEntities().get(0));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndAlias() {
        InnerJoin join = new InnerJoin("customers c");

        Assertions.assertNotNull(join);
        Assertions.assertEquals("customers c", join.getJoinEntities().get(0));
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("orders.customer_id", "c.id");
        InnerJoin join = new InnerJoin("customers c", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.INNER_JOIN, join.getOperator());
        Assertions.assertEquals("customers c", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition condition = Filters.and(Filters.eq("orders.id", "oi.order_id"), Filters.gt("oi.quantity", 0), Filters.eq("oi.status", "active"));
        InnerJoin join = new InnerJoin("order_items oi", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals("order_items oi", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        Condition condition = Filters.eq("o.customer_id", "c.id");
        InnerJoin join = new InnerJoin(entities, condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        InnerJoin join = new InnerJoin("categories cat");
        List<String> entities = join.getJoinEntities();

        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("categories cat", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("p.category_id", "c.id");
        InnerJoin join = new InnerJoin("categories c", condition);

        Condition retrieved = join.getCondition();
        Assertions.assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = Filters.and(Filters.eq("o.customer_id", Filters.expr("c.id")), Filters.eq("o.status", "completed"), Filters.gt("o.total", 100));
        InnerJoin join = new InnerJoin("orders o", condition);

        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("completed"));
        Assertions.assertTrue(params.contains(100));
    }

    @Test
    public void testGetParametersNoCondition() {
        InnerJoin join = new InnerJoin("products");
        List<Object> params = join.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Condition condition = Filters.between("price", 10, 100);
        InnerJoin join = new InnerJoin("products", condition);

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        Condition condition = Filters.eq("a.id", "b.a_id");
        InnerJoin original = new InnerJoin("table_b b", condition);
        InnerJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.toString(), copy.toString());
    }

    @Test
    public void testToString() {
        InnerJoin join = new InnerJoin("products");
        String result = join.toString();

        Assertions.assertTrue(result.contains("INNER JOIN"));
        Assertions.assertTrue(result.contains("products"));
    }

    @Test
    public void testToStringWithCondition() {
        Condition condition = Filters.eq("orders.customer_id", "customers.id");
        InnerJoin join = new InnerJoin("customers", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("INNER JOIN"));
        Assertions.assertTrue(result.contains("customers"));
        Assertions.assertTrue(result.contains("orders.customer_id"));
        Assertions.assertTrue(result.contains("customers.id"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("productId", Filters.expr("categoryId"));
        InnerJoin join = new InnerJoin("productCategory", condition);
        String result = join.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("INNER JOIN"));
        Assertions.assertTrue(result.contains("productCategory"));
        Assertions.assertTrue(result.contains("PRODUCT_ID"));
        Assertions.assertTrue(result.contains("CATEGORY_ID"));
    }

    @Test
    public void testHashCode() {
        Condition condition = Filters.eq("a", "b");
        InnerJoin join1 = new InnerJoin("table", condition);
        InnerJoin join2 = new InnerJoin("table", condition);
        InnerJoin join3 = new InnerJoin("other", condition);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition = Filters.eq("a", "b");
        InnerJoin join1 = new InnerJoin("table", condition);
        InnerJoin join2 = new InnerJoin("table", condition);
        InnerJoin join3 = new InnerJoin("other", condition);
        InnerJoin join4 = new InnerJoin("table");

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3);
        Assertions.assertNotEquals(join1, join4);
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testTypicalUseCases() {
        // Join orders with customers (only matching records)
        InnerJoin customerOrders = new InnerJoin("customers c", Filters.eq("orders.customer_id", Filters.expr("c.id")));

        String result = customerOrders.toString();
        Assertions.assertTrue(result.contains("INNER JOIN customers c"));
        Assertions.assertTrue(result.contains("orders.customer_id = c.id"));

        // Complex join with multiple conditions
        InnerJoin complexJoin = new InnerJoin("order_items oi",
                Filters.and(Filters.eq("orders.id", "oi.order_id"), Filters.eq("oi.status", "active"), Filters.gt("oi.quantity", 0)));

        result = complexJoin.toString();
        Assertions.assertTrue(result.contains("INNER JOIN order_items oi"));
        Assertions.assertTrue(result.contains("AND"));
    }

    @Test
    public void testMultipleTableJoin() {
        // Test joining multiple tables
        List<String> tables = Arrays.asList("products p", "categories c", "suppliers s");
        Condition condition = Filters.and(Filters.eq("p.category_id", "c.id"), Filters.eq("p.supplier_id", "s.id"));

        InnerJoin multiJoin = new InnerJoin(tables, condition);

        Assertions.assertEquals(3, multiJoin.getJoinEntities().size());
        String result = multiJoin.toString();
        Assertions.assertTrue(result.contains("products p"));
        Assertions.assertTrue(result.contains("categories c"));
        Assertions.assertTrue(result.contains("suppliers s"));
    }
}