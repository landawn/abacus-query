package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Join2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleJoin() {
        Join join = new Join("orders");
        assertNotNull(join);
        assertEquals(Operator.JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        Join join = new Join("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        Join join = new Join(Arrays.asList("orders o", "customers c"), new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testGetJoinEntities() {
        Join join = new Join(Arrays.asList("table1", "table2"), null);
        List<String> entities = join.getJoinEntities();
        assertEquals(2, (int) entities.size());
        assertTrue(entities.contains("table1"));
        assertTrue(entities.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        Join join = new Join("orders");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        Join join = new Join("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        Join join = new Join("orders o", new Equal("status", "active"));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString_Simple() {
        Join join = new Join("orders");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        Join join = new Join("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        Join join1 = new Join("orders", new Equal("a", "b"));
        Join join2 = new Join("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Join join = new Join("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        Join join1 = new Join("orders o", new Equal("a", "b"));
        Join join2 = new Join("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        Join join1 = new Join("orders");
        Join join2 = new Join("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        Join join = new Join("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", (Object) 100)));
        Join join = new Join("orders o", andCondition);
        assertEquals(2, (int) join.getParameters().size());
    }
}

public class JoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        Join join = new Join("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertEquals("products", join.getJoinEntities().get(0));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.operator());
        Assertions.assertEquals("orders o", join.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithInvalidJoinEntity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join((String) null, Filters.eq("a", "b")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("", Filters.eq("a", "b")));
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        Condition condition = Filters.eq("o.customer_id", "c.id");
        Join join = new Join(entities, condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntitiesRejectsInvalidElements() {
        Condition condition = Filters.eq("o.customer_id", "c.id");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", null), condition));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", ""), condition));
    }

    @Test
    public void testProtectedConstructors() {
        // Test protected constructors through a test subclass
        TestJoin join1 = new TestJoin(Operator.LEFT_JOIN, "table1");
        Assertions.assertEquals(Operator.LEFT_JOIN, join1.operator());
        Assertions.assertEquals("table1", join1.getJoinEntities().get(0));

        Condition condition = Filters.eq("a", "b");
        TestJoin join2 = new TestJoin(Operator.RIGHT_JOIN, "table2", condition);
        Assertions.assertEquals(Operator.RIGHT_JOIN, join2.operator());
        Assertions.assertEquals("table2", join2.getJoinEntities().get(0));
        Assertions.assertEquals(condition, join2.getCondition());

        List<String> entities = Arrays.asList("t1", "t2");
        TestJoin join3 = new TestJoin(Operator.FULL_JOIN, entities, condition);
        Assertions.assertEquals(Operator.FULL_JOIN, join3.operator());
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
        Condition condition = Filters.eq("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);

        Condition retrieved = join.getCondition();
        Assertions.assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.gt("amount", 100));
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
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("JOIN"));
        Assertions.assertTrue(result.contains("orders o"));
        Assertions.assertTrue(result.contains("customers.id"));
        Assertions.assertTrue(result.contains("orders.customer_id"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("customerId", Filters.expr("orderId"));
        Join join = new Join("orderTable", condition);
        String result = join.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("JOIN"));
        Assertions.assertTrue(result.contains("orderTable"));
        Assertions.assertTrue(result.contains("CUSTOMER_ID"));
        Assertions.assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    public void testToStringWithMultipleEntities() {
        List<String> entities = Arrays.asList("t1", "t2", "t3");
        Condition condition = Filters.eq("t1.id", "t2.t1_id");
        Join join = new Join(entities, condition);

        String result = join.toString();
        Assertions.assertTrue(result.contains("t1, t2, t3"));
    }

    @Test
    public void testHashCode() {
        Condition condition = Filters.eq("a", "b");
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
        Condition condition = Filters.eq("a", "b");
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

class Join2026Batch2Test extends TestBase {

    @Test
    public void testDefaultConstructor_EmptyState() {
        Join join = new Join();
        Join same = new Join();

        Assertions.assertTrue(join.getJoinEntities().isEmpty());
        Assertions.assertTrue(join.getParameters().isEmpty());
        Assertions.assertEquals(join, same);
        Assertions.assertEquals(join.hashCode(), same.hashCode());
    }
}
