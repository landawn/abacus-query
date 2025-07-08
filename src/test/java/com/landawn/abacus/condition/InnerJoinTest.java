package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
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
        Condition condition = CF.eq("orders.customer_id", "c.id");
        InnerJoin join = new InnerJoin("customers c", condition);
        
        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.INNER_JOIN, join.getOperator());
        Assertions.assertEquals("customers c", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition condition = CF.and(
            CF.eq("orders.id", "oi.order_id"),
            CF.gt("oi.quantity", 0),
            CF.eq("oi.status", "active")
        );
        InnerJoin join = new InnerJoin("order_items oi", condition);
        
        Assertions.assertNotNull(join);
        Assertions.assertEquals("order_items oi", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        Condition condition = CF.eq("o.customer_id", "c.id");
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
        Condition condition = CF.eq("p.category_id", "c.id");
        InnerJoin join = new InnerJoin("categories c", condition);
        
        Condition retrieved = join.getCondition();
        Assertions.assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = CF.and(
            CF.eq("o.customer_id", "c.id"),
            CF.eq("o.status", "completed"),
            CF.gt("o.total", 100)
        );
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
        Condition condition = CF.between("price", 10, 100);
        InnerJoin join = new InnerJoin("products", condition);
        
        join.clearParameters();
        
        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        Condition condition = CF.eq("a.id", "b.a_id");
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
        Condition condition = CF.eq("orders.customer_id", "customers.id");
        InnerJoin join = new InnerJoin("customers", condition);
        String result = join.toString();
        
        Assertions.assertTrue(result.contains("INNER JOIN"));
        Assertions.assertTrue(result.contains("customers"));
        Assertions.assertTrue(result.contains("orders.customer_id"));
        Assertions.assertTrue(result.contains("customers.id"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = CF.eq("productId", "categoryId");
        InnerJoin join = new InnerJoin("productCategory", condition);
        String result = join.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        
        Assertions.assertTrue(result.contains("INNER JOIN"));
        Assertions.assertTrue(result.contains("productCategory"));
        Assertions.assertTrue(result.contains("PRODUCT_ID"));
        Assertions.assertTrue(result.contains("CATEGORY_ID"));
    }

    @Test
    public void testHashCode() {
        Condition condition = CF.eq("a", "b");
        InnerJoin join1 = new InnerJoin("table", condition);
        InnerJoin join2 = new InnerJoin("table", condition);
        InnerJoin join3 = new InnerJoin("other", condition);
        
        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Condition condition = CF.eq("a", "b");
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
        InnerJoin customerOrders = new InnerJoin("customers c",
            CF.eq("orders.customer_id", "c.id"));
        
        String result = customerOrders.toString();
        Assertions.assertTrue(result.contains("INNER JOIN customers c"));
        Assertions.assertTrue(result.contains("orders.customer_id = c.id"));
        
        // Complex join with multiple conditions
        InnerJoin complexJoin = new InnerJoin("order_items oi",
            CF.and(
                CF.eq("orders.id", "oi.order_id"),
                CF.eq("oi.status", "active"),
                CF.gt("oi.quantity", 0)
            ));
        
        result = complexJoin.toString();
        Assertions.assertTrue(result.contains("INNER JOIN order_items oi"));
        Assertions.assertTrue(result.contains("AND"));
    }

    @Test
    public void testMultipleTableJoin() {
        // Test joining multiple tables
        List<String> tables = Arrays.asList("products p", "categories c", "suppliers s");
        Condition condition = CF.and(
            CF.eq("p.category_id", "c.id"),
            CF.eq("p.supplier_id", "s.id")
        );
        
        InnerJoin multiJoin = new InnerJoin(tables, condition);
        
        Assertions.assertEquals(3, multiJoin.getJoinEntities().size());
        String result = multiJoin.toString();
        Assertions.assertTrue(result.contains("products p"));
        Assertions.assertTrue(result.contains("categories c"));
        Assertions.assertTrue(result.contains("suppliers s"));
    }
}