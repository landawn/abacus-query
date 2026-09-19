package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class LeftJoinTest extends TestBase {
    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    public void testConstructor_Simple() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.condition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        LeftJoin join = new LeftJoin(entities, new And(Arrays.asList(new Equal("c.id", "o.customer_id"), new Equal("o.id", "oi.order_id"))));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        LeftJoin join = new LeftJoin(entities, Filters.on("table1.id", "table2.id"));
        List<String> result = join.joinEntities();
        assertEquals(2, result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        LeftJoin join = new LeftJoin("table_b b", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCondition_Null() {
        // A LEFT JOIN can no longer be condition-less: the single-argument form always throws.
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin("departments"));
        assertTrue(ex.getMessage().contains("LEFT JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin("departments", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin(Arrays.asList("departments", "teams"), null));
    }

    @Test
    public void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    public void testParameters_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("status", "active"));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString_Simple() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    public void testToString_WithCondition() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("LEFT JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    public void testHashCode() {
        LeftJoin join1 = new LeftJoin("orders", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        LeftJoin join1 = new LeftJoin("orders o", new Equal("a", "b"));
        LeftJoin join2 = new LeftJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        LeftJoin join1 = new LeftJoin("orders", ON_AB);
        LeftJoin join2 = new LeftJoin("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
        assertNotEquals(null, join);
    }

    @Test
    public void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("customers.id", "o.customer_id"), new Equal("o.status", "active")));
        LeftJoin join = new LeftJoin("orders o", andCondition);
        assertEquals(2, join.parameters().size());
    }

    @Test
    public void testWithAlias() {
        LeftJoin join = new LeftJoin("employee_departments ed", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("employee_departments ed"));
    }

    @Test
    public void testFindMissingRecords() {
        LeftJoin join = new LeftJoin("orders o", new Equal("c.customer_id", "o.customer_id"));
        assertNotNull(join);
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testWithAdditionalFilters() {
        And multipleConditions = new And(
                Arrays.asList(new Equal("orders.id", "oi.order_id"), new Equal("oi.status", "active"), new GreaterThan("oi.created_date", "2023-01-01")));
        LeftJoin join = new LeftJoin("order_items oi", multipleConditions);
        assertEquals(3, join.parameters().size());
    }

    @Test
    public void testPreserveLeftTableRows() {
        LeftJoin join = new LeftJoin("departments d", new Equal("employees.dept_id", "d.id"));
        assertNotNull(join.condition());
        assertEquals(Operator.LEFT_JOIN, join.operator());
    }

    @Test
    public void testConstructorWithJoinEntity() {
        LeftJoin join = new LeftJoin("orders", ON_AB);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.LEFT_JOIN, join.operator());
        Assertions.assertEquals(1, join.joinEntities().size());
        Assertions.assertEquals("orders", join.joinEntities().get(0));
        Assertions.assertSame(ON_AB, join.condition());
    }

    @Test
    public void testConstructorWithJoinEntityAndAlias() {
        LeftJoin join = new LeftJoin("orders o", ON_AB);

        Assertions.assertNotNull(join);
        Assertions.assertEquals("orders o", join.joinEntities().get(0));
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        LeftJoin join = new LeftJoin("orders", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.LEFT_JOIN, join.operator());
        Assertions.assertEquals("orders", join.joinEntities().get(0));
        Assertions.assertEquals(condition, join.condition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        Condition condition = Filters.and(Filters.eq("c.customer_id", "o.customer_id"), Filters.eq("o.status", "active"));
        LeftJoin join = new LeftJoin("orders o", condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals("orders o", join.joinEntities().get(0));
        Assertions.assertEquals(condition, join.condition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "order_items oi");
        Condition condition = Filters.and(Filters.eq("c.id", "o.customer_id"), Filters.eq("o.id", "oi.order_id"));
        LeftJoin join = new LeftJoin(entities, condition);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(2, join.joinEntities().size());
        Assertions.assertTrue(join.joinEntities().containsAll(entities));
        Assertions.assertEquals(condition, join.condition());
    }

    @Test
    public void testParameters() {
        Condition condition = Filters.and(Filters.eq("o.customer_id", Filters.expr("c.id")), Filters.eq("o.status", "completed"));
        LeftJoin join = new LeftJoin("orders o", condition);

        List<Object> params = join.parameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("completed", params.get(0));
    }

    @Test
    public void testParametersNoCondition() {
        // No bound parameters when the predicate compares columns only.
        LeftJoin join = new LeftJoin("orders", ON_AB);
        List<Object> params = join.parameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testToString() {
        LeftJoin join = new LeftJoin("orders", ON_AB);
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
        String result = join.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("LEFT JOIN"));
        Assertions.assertTrue(result.contains("orderTable"));
        Assertions.assertTrue(result.contains("CUSTOMER_ID"));
        Assertions.assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    public void testEquals() {
        Condition condition = Filters.eq("a", "b");
        LeftJoin join1 = new LeftJoin("table", condition);
        LeftJoin join2 = new LeftJoin("table", condition);
        LeftJoin join3 = new LeftJoin("other", condition);
        LeftJoin join4 = new LeftJoin("table", Filters.eq("a", "c"));

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

    @Test
    @SuppressWarnings("deprecation")
    public void testEntityValidationPrecedesPredicateCheck() {
        // A null/blank entity is reported as such, not as a missing join predicate.
        final String entityMessage = "must not be null, empty, or blank";

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin((String) null));
        Assertions.assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        Assertions.assertFalse(ex.getMessage().contains("requires a non-null ON/USING predicate"), ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin("   ", null));
        Assertions.assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedSingleArgConstructorAlwaysThrows() {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new LeftJoin("orders"));
        Assertions.assertTrue(ex.getMessage().contains("LEFT JOIN requires a non-null ON/USING predicate"), ex.getMessage());

        // The advertised alternatives work.
        Assertions.assertEquals("LEFT JOIN orders ON a.id = b.id", new LeftJoin("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        Assertions.assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }
}
