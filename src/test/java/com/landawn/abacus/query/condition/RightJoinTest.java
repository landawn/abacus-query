package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class RightJoinTest extends TestBase {

    @Test
    public void testConstructorWithEntityOnly() {
        RightJoin join = Filters.rightJoin("customers");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("customers"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithEntityAndCondition() {
        On onClause = Filters.on("order_items.product_id", "products.id");
        RightJoin join = Filters.rightJoin("products", onClause);

        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
        Assertions.assertEquals(onClause, join.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        And complexCondition = Filters.and(Filters.on("orders.product_id", "products.id"), Filters.eq("products.active", true));
        RightJoin join = Filters.rightJoin("products", complexCondition);

        Assertions.assertEquals(complexCondition, join.getCondition());
        Assertions.assertEquals(1, join.getParameters().size());
    }

    @Test
    public void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("categories", "subcategories");
        And joinCondition = Filters.and(Filters.on("products.category_id", "categories.id"), Filters.on("products.subcategory_id", "subcategories.id"));
        RightJoin join = Filters.rightJoin(tables, joinCondition);

        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(joinCondition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        RightJoin join = Filters.rightJoin("departments");

        List<String> entities = join.getJoinEntities();
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("departments", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        On onClause = Filters.on("employees.dept_id", "departments.id");
        RightJoin join = Filters.rightJoin("departments", onClause);

        Assertions.assertEquals(onClause, join.getCondition());
    }

    @Test
    public void testGetParameters() {
        Equal activeCondition = Filters.eq("active", true);
        RightJoin join = Filters.rightJoin("users", activeCondition);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(true, params.get(0));
    }

    @Test
    public void testGetParametersNoCondition() {
        RightJoin join = Filters.rightJoin("products");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Equal condition = Filters.eq("region", "WEST");
        RightJoin join = Filters.rightJoin("stores", condition);

        join.clearParameters();

        // Verify condition parameters are cleared
        Assertions.assertTrue(condition.getParameters().isEmpty() || condition.getParameters().get(0) == null);
    }

    @Test
    public void testClearParametersNoCondition() {
        RightJoin join = Filters.rightJoin("customers");

        // Should not throw exception
        join.clearParameters();
    }

    @Test
    public void testToString() {
        RightJoin join = Filters.rightJoin("suppliers");

        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("suppliers"));
    }

    @Test
    public void testToStringWithCondition() {
        On onClause = Filters.on("products.supplier_id", "suppliers.id");
        RightJoin join = Filters.rightJoin("suppliers", onClause);

        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("suppliers"));
        Assertions.assertTrue(result.contains("ON"));
    }

    @Test
    public void testCopy() {
        On condition = Filters.on("orders.customer_id", "customers.id");
        RightJoin original = Filters.rightJoin("customers", condition);

        RightJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithoutCondition() {
        RightJoin original = Filters.rightJoin("categories");

        RightJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testHashCode() {
        RightJoin join1 = Filters.rightJoin("products");
        RightJoin join2 = Filters.rightJoin("products");
        RightJoin join3 = Filters.rightJoin("categories");

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        On condition = Filters.on("a.id", "b.a_id");

        RightJoin join1 = Filters.rightJoin("tableB");
        RightJoin join2 = Filters.rightJoin("tableB");
        RightJoin join3 = Filters.rightJoin("tableC");
        RightJoin join4 = Filters.rightJoin("tableB", condition);

        Assertions.assertTrue(join1.equals(join1));
        Assertions.assertTrue(join1.equals(join2));
        Assertions.assertFalse(join1.equals(join3));
        Assertions.assertFalse(join1.equals(join4));
        Assertions.assertFalse(join1.equals(null));
        Assertions.assertFalse(join1.equals("not a RightJoin"));
    }

    @Test
    public void testPracticalExample1() {
        // Get all customers, even those without orders
        On onClause = Filters.on("orders.customer_id", "customers.id");
        RightJoin join = Filters.rightJoin("customers", onClause);

        // Would result in: RIGHT JOIN customers ON orders.customer_id = customers.id
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals("customers", join.getJoinEntities().get(0));
    }

    @Test
    public void testPracticalExample2() {
        // Get all products, including those never ordered
        On onClause = Filters.on("order_items.product_id", "products.id");
        RightJoin join = Filters.rightJoin("products", onClause);

        // Would result in: RIGHT JOIN products ON order_items.product_id = products.id
        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("products"));
    }

    @Test
    public void testPracticalExample3() {
        // Complex right join with additional conditions
        And complexCondition = Filters.and(Filters.on("sales.product_id", "products.id"), Filters.eq("products.active", true), Filters.gt("products.price", 0));
        RightJoin activeProducts = Filters.rightJoin("products", complexCondition);

        // Gets all active products with price > 0, even if they have no sales
        Assertions.assertEquals(2, activeProducts.getParameters().size());
    }

    @Test
    public void testMultipleTablesJoin() {
        List<String> tables = Arrays.asList("departments", "locations");
        And joinCondition = Filters.and(Filters.on("employees.dept_id", "departments.id"), Filters.on("departments.location_id", "locations.id"));

        RightJoin join = Filters.rightJoin(tables, joinCondition);

        // Gets all departments and locations, even without employees
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertEquals(joinCondition, join.getCondition());
    }
}