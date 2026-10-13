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

public class RightJoinTest extends TestBase {

    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    void testAllRightTableRows() {
        RightJoin join = new RightJoin("customers", new Equal("orders.customer_id", "customers.id"));
        assertNotNull(join);
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("orders.product_id", "products.id"), new Equal("products.active", true)));
        RightJoin join = new RightJoin("products", andCondition);
        assertEquals(2, join.parameters().size());
    }

    @Test
    void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("orders", "order_items");
        RightJoin join = new RightJoin(entities, new Equal("orders.id", "order_items.order_id"));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    void testConstructor_Simple() {
        RightJoin join = new RightJoin("departments", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    void testConstructor_WithCondition() {
        RightJoin join = new RightJoin("departments", new Equal("employees.dept_id", "departments.id"));
        assertNotNull(join);
        assertNotNull(join.condition());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    void testConstructorRejectsInvalidEntities() {
        assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin((String) null, ON_AB));
        assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin("", ON_AB));
        assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin(Arrays.asList("products", null), Filters.on("a", "b")));
        assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin(Arrays.asList("products", ""), Filters.on("a", "b")));
    }

    @Test
    void testConstructorWithComplexCondition() {
        On complexCondition = Filters.on(Filters.expr("orders.product_id = products.id").and(Filters.eq("products.active", true)));
        RightJoin join = Filters.rightJoin("products", complexCondition);

        assertEquals(complexCondition, join.condition());
        assertEquals(1, join.parameters().size());
    }

    @Test
    void testConstructorWithEntityAndCondition() {
        On onClause = Filters.on("order_items.product_id", "products.id");
        RightJoin join = Filters.rightJoin("products", onClause);

        assertEquals(Operator.RIGHT_JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertTrue(join.joinEntities().contains("products"));
        assertEquals(onClause, join.condition());
    }

    @Test
    void testConstructorWithEntityOnly() {
        RightJoin join = Filters.rightJoin("customers", ON_AB);

        assertNotNull(join);
        assertEquals(Operator.RIGHT_JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertTrue(join.joinEntities().contains("customers"));
        assertSame(ON_AB, join.condition());
    }

    @Test
    void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("categories", "subcategories");
        On joinCondition = Filters.on(Filters.expr("products.category_id = categories.id").and(Filters.expr("products.subcategory_id = subcategories.id")));
        RightJoin join = Filters.rightJoin(tables, joinCondition);

        assertEquals(Operator.RIGHT_JOIN, join.operator());
        assertEquals(2, join.joinEntities().size());
        assertTrue(join.joinEntities().containsAll(tables));
        assertEquals(joinCondition, join.condition());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedSingleArgConstructorAlwaysThrows() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new RightJoin("orders"));
        assertTrue(ex.getMessage().contains("RIGHT JOIN requires a non-null ON/USING predicate"), ex.getMessage());

        // The advertised alternatives work.
        assertEquals("RIGHT JOIN orders ON a.id = b.id", new RightJoin("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEntityValidationPrecedesPredicateCheck() {
        // A null/blank entity is reported as such, not as a missing join predicate.
        final String entityMessage = "must not be null, empty, or blank";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new RightJoin((String) null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains("requires a non-null ON/USING predicate"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new RightJoin("   ", null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
    }

    @Test
    void testEquals() {
        On condition = Filters.on("a.id", "b.a_id");

        RightJoin join1 = Filters.rightJoin("tableB", ON_AB);
        RightJoin join2 = Filters.rightJoin("tableB", ON_AB);
        RightJoin join3 = Filters.rightJoin("tableC", ON_AB);
        RightJoin join4 = Filters.rightJoin("tableB", condition);

        assertTrue(join1.equals(join1));
        assertTrue(join1.equals(join2));
        assertFalse(join1.equals(join3));
        assertFalse(join1.equals(join4));
        assertFalse(join1.equals(null));
        assertFalse(join1.equals("not a RightJoin"));
    }

    @Test
    void testEquals_DifferentEntities() {
        RightJoin join1 = new RightJoin("orders", ON_AB);
        RightJoin join2 = new RightJoin("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    void testEquals_EqualObjects() {
        RightJoin join1 = new RightJoin("orders o", new Equal("a", "b"));
        RightJoin join2 = new RightJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    void testEquals_Null() {
        RightJoin join = new RightJoin("orders", ON_AB);
        assertNotEquals(null, join);
    }

    @Test
    void testEquals_SameObject() {
        RightJoin join = new RightJoin("orders", ON_AB);
        assertEquals(join, join);
    }

    @Test
    void testFindMissingRelationships() {
        RightJoin join = new RightJoin("products p", new Equal("order_items.product_id", "p.id"));
        assertNotNull(join.condition());
        assertEquals(Operator.RIGHT_JOIN, join.operator());
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        RightJoin join = new RightJoin("table_b b", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testGetCondition_Null() {
        // A RIGHT JOIN can no longer be condition-less: the single-argument form always throws.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new RightJoin("customers"));
        assertTrue(ex.getMessage().contains("RIGHT JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new RightJoin("customers", null));
        assertThrows(IllegalArgumentException.class, () -> new RightJoin(Arrays.asList("customers", "orders"), null));
    }

    @Test
    void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        RightJoin join = new RightJoin(entities, Filters.on("table1.id", "table2.id"));
        List<String> result = join.joinEntities();
        assertEquals(2, result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    void testHashCode() {
        RightJoin join1 = new RightJoin("orders", new Equal("a", "b"));
        RightJoin join2 = new RightJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    void testMultipleTablesJoin() {
        List<String> tables = Arrays.asList("departments", "locations");
        On joinCondition = Filters.on(Filters.expr("employees.dept_id = departments.id").and(Filters.expr("departments.location_id = locations.id")));

        RightJoin join = Filters.rightJoin(tables, joinCondition);

        // Gets all departments and locations, even without employees
        assertEquals(2, join.joinEntities().size());
        assertEquals(joinCondition, join.condition());
    }

    @Test
    void testMultiTableRightJoin() {
        List<String> tables = Arrays.asList("categories", "subcategories");
        And condition = new And(Arrays.asList(new Equal("products.category_id", "categories.id"), new Equal("products.subcategory_id", "subcategories.id")));
        RightJoin join = new RightJoin(tables, condition);
        assertEquals(2, join.joinEntities().size());
    }

    @Test
    void testParameters() {
        Equal activeCondition = Filters.eq("active", true);
        RightJoin join = Filters.rightJoin("users", activeCondition);

        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        RightJoin join = new RightJoin("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    void testParameters_WithCondition() {
        RightJoin join = new RightJoin("products", new Equal("active", true));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    void testParametersNoCondition() {
        // No bound parameters when the predicate compares columns only.
        RightJoin join = Filters.rightJoin("products", ON_AB);

        List<Object> params = join.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testPracticalExample1() {
        // Get all customers, even those without orders
        On onClause = Filters.on("orders.customer_id", "customers.id");
        RightJoin join = Filters.rightJoin("customers", onClause);

        // Would result in: RIGHT JOIN customers ON orders.customer_id = customers.id
        assertEquals(Operator.RIGHT_JOIN, join.operator());
        assertEquals("customers", join.joinEntities().get(0));
    }

    @Test
    void testPracticalExample2() {
        // Get all products, including those never ordered
        On onClause = Filters.on("order_items.product_id", "products.id");
        RightJoin join = Filters.rightJoin("products", onClause);

        // Would result in: RIGHT JOIN products ON order_items.product_id = products.id
        String result = join.toString();
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("products"));
    }

    @Test
    void testPracticalExample3() {
        // Complex right join with additional conditions
        On complexCondition = Filters
                .on(Filters.expr("sales.product_id = products.id").and(Filters.eq("products.active", true)).and(Filters.gt("products.price", 0)));
        RightJoin activeProducts = Filters.rightJoin("products", complexCondition);

        // Gets all active products with price > 0, even if they have no sales
        assertEquals(2, activeProducts.parameters().size());
    }

    @Test
    void testToString() {
        RightJoin join = Filters.rightJoin("suppliers", ON_AB);

        String result = join.toString();
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("suppliers"));
    }

    @Test
    void testToString_Simple() {
        RightJoin join = new RightJoin("departments", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    void testToString_WithCondition() {
        RightJoin join = new RightJoin("products p", new Equal("order_items.product_id", "p.id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("products p"));
    }

    @Test
    void testToStringWithCondition() {
        On onClause = Filters.on("products.supplier_id", "suppliers.id");
        RightJoin join = Filters.rightJoin("suppliers", onClause);

        String result = join.toString();
        assertTrue(result.contains("RIGHT JOIN"));
        assertTrue(result.contains("suppliers"));
        assertTrue(result.contains("ON"));
    }
}
