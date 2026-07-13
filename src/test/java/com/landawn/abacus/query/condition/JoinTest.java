package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class JoinTest extends TestBase {
    private static final class TestComposableWrapper extends ComposableCell {
        TestComposableWrapper(final Condition condition) {
            super(Operator.NOT, condition);
        }
    }

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
        assertNotNull(join.condition());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        Join join = new Join(Arrays.asList("orders o", "customers c"), new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
    }

    @Test
    public void testGetJoinEntities() {
        Join join = new Join(Arrays.asList("table1", "table2"), null);
        List<String> entities = join.joinEntities();
        assertEquals(2, entities.size());
        assertTrue(entities.contains("table1"));
        assertTrue(entities.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        Join join = new Join("orders");
        assertNull(join.condition());
    }

    @Test
    public void testParameters_Empty() {
        Join join = new Join("orders");
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    public void testParameters_WithCondition() {
        Join join = new Join("orders o", new Equal("status", "active"));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString_Simple() {
        Join join = new Join("orders");
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        Join join = new Join("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
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
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", 100)));
        Join join = new Join("orders o", andCondition);
        assertEquals(2, join.parameters().size());
    }

    @Test
    public void testConstructorRejectsRawOnUsingExpressionsButAllowsExplicitConnectors() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("ON users.id = orders.user_id")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("USING (id)")));

        Assertions.assertDoesNotThrow(() -> new Join("orders", Filters.on("users.id", "orders.user_id")));
        Assertions.assertDoesNotThrow(() -> new Join("orders", Filters.using("id")));
    }

    @Test
    public void testConstructorRejectsNestedOnConnectorCondition() {
        Condition nestedOn = new Junction(Operator.AND, Arrays.asList(Filters.on("sales.product_id", "products.id"), Filters.eq("products.active", true)),
                true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("products", nestedOn));
    }

    @Test
    public void testConstructorRejectsWrappedNonPredicateComponents() {
        final SubQuery subQuery = new SubQuery("SELECT id FROM users");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new OrderBy("name"))));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new Any(subQuery))));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new Expression(" "))));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", new Equal()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", new On()));
        Assertions.assertDoesNotThrow(() -> new Join("orders", new TestComposableWrapper(new Equal("customers.id", "orders.customer_id"))));
    }

    @Test
    public void testConstructorWithJoinEntity() {
        Join join = new Join("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.operator());
        Assertions.assertEquals(1, join.joinEntities().size());
        Assertions.assertEquals("products", join.joinEntities().get(0));
        Assertions.assertNull(join.condition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.JOIN, join.operator());
        Assertions.assertEquals("orders o", join.joinEntities().get(0));
        Assertions.assertEquals(condition, join.condition());
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
        Assertions.assertEquals(2, join.joinEntities().size());
        Assertions.assertTrue(join.joinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.condition());
    }

    @Test
    public void testConstructorWithMultipleEntitiesRejectsInvalidElements() {
        Condition condition = Filters.eq("o.customer_id", "c.id");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", null), condition));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", ""), condition));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", "   "), condition));
    }

    @Test
    public void testConstructorRejectsBlankJoinEntitiesAndBlankExpressionCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("   ")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders", "   "), Filters.eq("a", "b")));
    }

    @Test
    public void testProtectedConstructors() {
        // Test protected constructors through a test subclass
        TestJoin join1 = new TestJoin(Operator.LEFT_JOIN, "table1");
        Assertions.assertEquals(Operator.LEFT_JOIN, join1.operator());
        Assertions.assertEquals("table1", join1.joinEntities().get(0));

        Condition condition = Filters.eq("a", "b");
        TestJoin join2 = new TestJoin(Operator.RIGHT_JOIN, "table2", condition);
        Assertions.assertEquals(Operator.RIGHT_JOIN, join2.operator());
        Assertions.assertEquals("table2", join2.joinEntities().get(0));
        Assertions.assertEquals(condition, join2.condition());

        List<String> entities = Arrays.asList("t1", "t2");
        TestJoin join3 = new TestJoin(Operator.FULL_JOIN, entities, condition);
        Assertions.assertEquals(Operator.FULL_JOIN, join3.operator());
        Assertions.assertEquals(2, join3.joinEntities().size());
        Assertions.assertEquals(condition, join3.condition());
    }

    @Test
    public void testParameters() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.gt("amount", 100));
        Join join = new Join("orders", condition);

        List<Object> params = join.parameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(100));
    }

    @Test
    public void testParametersNoCondition() {
        Join join = new Join("products");
        List<Object> params = join.parameters();

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
    public void testToStringAddsOnForPlainPredicateCondition() {
        Join plainPredicate = new Join("orders", Filters.expr("customers.id = orders.customer_id"));
        Join explicitOn = new Join("orders", Filters.on("customers.id", "orders.customer_id"));

        Assertions.assertEquals("JOIN orders ON customers.id = orders.customer_id", plainPredicate.toSql(NamingPolicy.NO_CHANGE));
        Assertions.assertEquals("JOIN orders ON customers.id = orders.customer_id", explicitOn.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testConstructorRejectsClauseCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.where(Filters.eq("a", 1))));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("customerId", Filters.expr("orderId"));
        Join join = new Join("orderTable", condition);
        String result = join.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

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

    @Test
    public void testDefaultConstructor_EmptyState() {
        Join join = new Join();
        Join same = new Join();

        Assertions.assertTrue(join.joinEntities().isEmpty());
        Assertions.assertTrue(join.parameters().isEmpty());
        Assertions.assertEquals(join, same);
        Assertions.assertEquals(join.hashCode(), same.hashCode());
    }

    @Test
    public void testDefaultConstructorToString() {
        Join join = new Join();

        Assertions.assertNotNull(join.toSql(NamingPolicy.NO_CHANGE));
        Assertions.assertNotNull(join.toString());
        Assertions.assertNotNull(join.toSql(null));
    }

    @Test
    public void testDefaultConstructorToStringHasNoTrailingSpace() {
        // Bug fix: previously rendered "null " (literal null + trailing space) when both operator
        // and join entities were null. The trailing space combined with the "null" placeholder
        // produced invalid SQL fragments. Now should render as plain "null" with no trailing space.
        Join join = new Join();

        String rendered = join.toSql(NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("null", rendered);
        Assertions.assertFalse(rendered.endsWith(" "), "toString() must not end with a trailing space");
    }

    @Test
    public void testToStringOperatorOnlyHasNoTrailingSpace() throws Exception {
        // Bug fix: when joinEntities is null/empty but operator is set, the output should not have
        // a trailing space (which would yield "INNER JOIN " instead of "INNER JOIN").
        Join join = new Join();
        java.lang.reflect.Field operatorField = AbstractCondition.class.getDeclaredField("operator");
        operatorField.setAccessible(true);
        operatorField.set(join, Operator.INNER_JOIN);

        String rendered = join.toSql(NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("INNER JOIN", rendered);
        Assertions.assertFalse(rendered.endsWith(" "), "toString() must not end with a trailing space");
    }

    @Test
    public void testRegularJoinsUnaffectedByFix() {
        // Sanity check that the fix does not regress normal join rendering.
        Join j1 = new Join("orders");
        Assertions.assertEquals("JOIN orders", j1.toSql(NamingPolicy.NO_CHANGE));

        Join j2 = new Join("orders o", new Equal("c.id", "o.cid"));
        String r2 = j2.toSql(NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(r2.startsWith("JOIN orders o"));
        Assertions.assertTrue(r2.contains("c.id"));

        InnerJoin ij = new InnerJoin("orders o");
        Assertions.assertEquals("INNER JOIN orders o", ij.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testConstructorRejectsQuantifiedSubQueryOperand() {
        // ANY/ALL/SOME are quantified-subquery operands (valid only on the RHS of a comparison);
        // "JOIN t1 ON ANY (SELECT ...)" is invalid SQL in every dialect, so the constructor must
        // reject them, consistent with Clause and the composable and()/or()/not() operations.
        SubQuery sub = Filters.subQuery("SELECT id FROM users");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.any(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.all(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.some(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("t1", "t2"), Filters.any(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.join("t1", Filters.any(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.innerJoin("t1", Filters.all(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.leftJoin("t1", Filters.some(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin("t1", Filters.any(sub)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.fullJoin("t1", Filters.all(sub)));

        // On/Using instances (and a null condition) are still accepted.
        assertNotNull(new Join("t1", new On("a.id", "b.id")));
        assertNotNull(new Join("t1", new Using("id")));
        assertNotNull(new Join("t1", null));
    }

    @Test
    public void testConstructorRejectsNonJoinOperator() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Join(Operator.EQUAL, "t1", null));
    }
}
