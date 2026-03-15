package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class InnerJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        InnerJoin join = new InnerJoin("orders");
        assertNotNull(join);
        assertEquals(Operator.INNER_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.INNER_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        InnerJoin join = new InnerJoin(entities, new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.INNER_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        InnerJoin join = new InnerJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        InnerJoin join = new InnerJoin("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        InnerJoin join = new InnerJoin("orders");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        InnerJoin join = new InnerJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("status", "active"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString_Simple() {
        InnerJoin join = new InnerJoin("orders");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INNER JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        InnerJoin join = new InnerJoin("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INNER JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        InnerJoin join1 = new InnerJoin("orders", new Equal("a", "b"));
        InnerJoin join2 = new InnerJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        InnerJoin join = new InnerJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        InnerJoin join1 = new InnerJoin("orders o", new Equal("a", "b"));
        InnerJoin join2 = new InnerJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        InnerJoin join1 = new InnerJoin("orders");
        InnerJoin join2 = new InnerJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        InnerJoin join = new InnerJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", (Object) 100)));
        InnerJoin join = new InnerJoin("orders o", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testWithAlias() {
        InnerJoin join = new InnerJoin("order_details od");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("order_details od"));
    }

    @Test
    public void testWithCompositeKey() {
        And compositeKey = new And(Arrays.asList(new Equal("orders.id", "oi.order_id"), new Equal("orders.version", "oi.order_version")));
        InnerJoin join = new InnerJoin("order_items oi", compositeKey);
        assertNotNull(join.getCondition());
        assertEquals(2, (int) join.getParameters().size());
    }

    @Test
    public void testWithMultipleConditions() {
        And multipleConditions = new And(
                Arrays.asList(new Equal("order_items.product_id", "p.id"), new Equal("p.active", true), new GreaterThan("p.stock", (Object) 0)));
        InnerJoin join = new InnerJoin("products p", multipleConditions);
        assertEquals(3, (int) join.getParameters().size());
    }
}

public class InnerJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        InnerJoin join = new InnerJoin("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.INNER_JOIN, join.operator());
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
        Assertions.assertEquals(Operator.INNER_JOIN, join.operator());
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
        String result = join.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

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
