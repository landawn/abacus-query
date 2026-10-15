package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class LeftJoinTest extends TestBase {

    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("customers.id", "o.customer_id"), new Equal("o.status", "active")));
        LeftJoin join = new LeftJoin("orders o", andCondition);
        assertEquals(2, join.parameters().size());
    }

    @Test
    void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        LeftJoin join = new LeftJoin(entities, new And(Arrays.asList(new Equal("c.id", "o.customer_id"), new Equal("o.id", "oi.order_id"))));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    void testConstructor_Simple() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    void testConstructor_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.condition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    void testConstructorWithComplexCondition() {
        Condition condition = Filters.and(Filters.eq("c.customer_id", "o.customer_id"), Filters.eq("o.status", "active"));
        LeftJoin join = new LeftJoin("orders o", condition);

        assertNotNull(join);
        assertEquals("orders o", join.joinEntities().get(0));
        assertEquals(condition, join.condition());
    }

    @Test
    void testConstructorWithJoinEntity() {
        LeftJoin join = new LeftJoin("orders", ON_AB);

        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertEquals("orders", join.joinEntities().get(0));
        assertSame(ON_AB, join.condition());
    }

    @Test
    void testConstructorWithJoinEntityAndAlias() {
        LeftJoin join = new LeftJoin("orders o", ON_AB);

        assertNotNull(join);
        assertEquals("orders o", join.joinEntities().get(0));
    }

    @Test
    void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        LeftJoin join = new LeftJoin("orders", condition);

        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
        assertEquals("orders", join.joinEntities().get(0));
        assertEquals(condition, join.condition());
    }

    @Test
    void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        Condition condition = Filters.and(Filters.eq("c.id", "o.customer_id"), Filters.eq("o.id", "oi.order_id"));
        LeftJoin join = new LeftJoin(entities, condition);

        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertTrue(join.joinEntities().containsAll(entities));
        assertEquals(condition, join.condition());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedSingleArgConstructorAlwaysThrows() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LeftJoin("orders"));
        assertTrue(ex.getMessage().contains("LEFT JOIN requires a non-null ON/USING predicate"), ex.getMessage());

        // The advertised alternatives work.
        assertEquals("LEFT JOIN orders ON a.id = b.id", new LeftJoin("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEntityValidationPrecedesPredicateCheck() {
        // A null/blank entity is reported as such, not as a missing join predicate.
        final String entityMessage = "must not be null, empty, or blank";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LeftJoin((String) null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains("requires a non-null ON/USING predicate"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new LeftJoin("   ", null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
    }

    @Test
    void testEquals() {
        Condition condition = Filters.eq("a", "b");
        LeftJoin join1 = new LeftJoin("table", condition);
        LeftJoin join2 = new LeftJoin("table", condition);
        LeftJoin join3 = new LeftJoin("other", condition);
        LeftJoin join4 = new LeftJoin("table", Filters.eq("a", "c"));

        assertEquals(join1, join1);
        assertEquals(join1, join2);
        assertNotEquals(join1, join3);
        assertNotEquals(join1, join4);
        assertNotEquals(join1, null);
        assertNotEquals(join1, "string");
    }

    @Test
    void testEquals_DifferentEntities() {
        LeftJoin join1 = new LeftJoin("orders", ON_AB);
        LeftJoin join2 = new LeftJoin("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    void testEquals_EqualObjects() {
        LeftJoin join1 = new LeftJoin("orders o", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    void testEquals_Null() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertNotEquals(null, join);
    }

    @Test
    void testEquals_SameObject() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertEquals(join, join);
    }

    @Test
    void testFindMissingRecords() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.customer_id", "o.customer_id"));
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        LeftJoin join = new LeftJoin("table_b b", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testGetCondition_Null() {
        // A LEFT JOIN can no longer be condition-less: the single-argument form always throws.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LeftJoin("departments"));
        assertTrue(ex.getMessage().contains("LEFT JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new LeftJoin("departments", null));
        assertThrows(IllegalArgumentException.class, () -> new LeftJoin(Arrays.asList("departments", "teams"), null));
    }

    @Test
    void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        LeftJoin join = new LeftJoin(entities, Filters.on("table1.id", "table2.id"));
        List<String> result = join.joinEntities();
        assertEquals(2, result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    void testHashCode() {
        LeftJoin join1 = new LeftJoin("orders", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    void testParameters() {
        Condition condition = Filters.and(Filters.eq("o.customer_id", Filters.expr("c.id")), Filters.eq("o.status", "completed"));
        LeftJoin join = new LeftJoin("orders o", condition);

        List<Object> params = join.parameters();

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("completed", params.get(0));
    }

    @Test
    void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    void testParameters_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("status", "active"));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParametersNoCondition() {
        // No bound parameters when the predicate compares columns only.
        LeftJoin join = new LeftJoin("orders", ON_AB);
        List<Object> params = join.parameters();

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testPreserveLeftTableRows() {
        LeftJoin join = new LeftJoin("departments d", new Equal("employees.dept_id", "d.id"));
        assertNotNull(join.condition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    void testToString() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        String result = join.toString();

        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    void testToString_Simple() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    void testToString_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    void testToStringWithCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        LeftJoin join = new LeftJoin("orders o", condition);
        String result = join.toString();

        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders o"));
        assertTrue(result.contains("customers.id"));
        assertTrue(result.contains("orders.customer_id"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("customerId", Filters.expr("orderId"));
        LeftJoin join = new LeftJoin("orderTable", condition);
        String result = join.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orderTable"));
        assertTrue(result.contains("CUSTOMER_ID"));
        assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    void testTypicalUseCases() {
        // Find all customers, including those without orders
        LeftJoin customerOrders = new LeftJoin("orders o", Filters.eq("c.customer_id", Filters.expr("o.customer_id")));

        String result = customerOrders.toString();
        assertTrue(result.contains("LEFT JOIN orders o"));
        assertTrue(result.contains("c.customer_id = o.customer_id"));

        // Include optional data
        LeftJoin optionalData = new LeftJoin("customer_preferences cp", Filters.eq("c.id", "cp.customer_id"));

        result = optionalData.toString();
        assertTrue(result.contains("LEFT JOIN customer_preferences cp"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testUnterminatedBlockCommentInJoinEntityIsRejected() {
        // An open "/*" in the entity would comment out the ON predicate and everything rendered after the join.
        assertEquals("joinEntity must not contain an unterminated block comment: t /* x",
                assertThrows(IllegalArgumentException.class, () -> new LeftJoin("t /* x", ON_AB)).getMessage());
        assertEquals("joinEntity in joinEntities must not contain an unterminated block comment: t /* x",
                assertThrows(IllegalArgumentException.class, () -> new LeftJoin(List.of("a", "t /* x"), ON_AB)).getMessage());
        assertEquals("joinEntity must not contain an unterminated block comment: t /* x",
                assertThrows(IllegalArgumentException.class, () -> new LeftJoin("t /* x")).getMessage());
        assertEquals("LEFT JOIN t /* x */ ON a.id = b.id", new LeftJoin("t /* x */", ON_AB).toString());
    }

    @Test
    void testWithAdditionalFilters() {
        And multipleConditions = new And(
                Arrays.asList(new Equal("orders.id", "oi.order_id"), new Equal("oi.status", "active"), new GreaterThan("oi.created_date", "2023-01-01")));
        LeftJoin join = new LeftJoin("order_items oi", multipleConditions);
        assertEquals(3, join.parameters().size());
    }

    @Test
    void testWithAlias() {
        LeftJoin join = new LeftJoin("employee_departments ed", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("employee_departments ed"));
    }
}
