package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.RightJoin;
import com.landawn.abacus.query.condition.ConditionFactory.CF;

public class RightJoinTest extends TestBase {

    @Test
    public void testConstructorWithEntityOnly() {
        RightJoin join = CF.rightJoin("customers");
        
        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("customers"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithEntityAndCondition() {
        On onClause = CF.on("order_items.product_id", "products.id");
        RightJoin join = CF.rightJoin("products", onClause);
        
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
        Assertions.assertEquals(onClause, join.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        And complexCondition = CF.and(
            CF.on("orders.product_id", "products.id"),
            CF.eq("products.active", true)
        );
        RightJoin join = CF.rightJoin("products", complexCondition);
        
        Assertions.assertEquals(complexCondition, join.getCondition());
        Assertions.assertEquals(1, join.getParameters().size());
    }

    @Test
    public void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("categories", "subcategories");
        And joinCondition = CF.and(
            CF.on("products.category_id", "categories.id"),
            CF.on("products.subcategory_id", "subcategories.id")
        );
        RightJoin join = CF.rightJoin(tables, joinCondition);
        
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(tables));
        Assertions.assertEquals(joinCondition, join.getCondition());
    }

    @Test
    public void testGetJoinEntities() {
        RightJoin join = CF.rightJoin("departments");
        
        List<String> entities = join.getJoinEntities();
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("departments", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        On onClause = CF.on("employees.dept_id", "departments.id");
        RightJoin join = CF.rightJoin("departments", onClause);
        
        Assertions.assertEquals(onClause, join.getCondition());
    }

    @Test
    public void testGetParameters() {
        Equal activeCondition = CF.eq("active", true);
        RightJoin join = CF.rightJoin("users", activeCondition);
        
        List<Object> params = join.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(true, params.get(0));
    }

    @Test
    public void testGetParametersNoCondition() {
        RightJoin join = CF.rightJoin("products");
        
        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Equal condition = CF.eq("region", "WEST");
        RightJoin join = CF.rightJoin("stores", condition);
        
        join.clearParameters();
        
        // Verify condition parameters are cleared
        Assertions.assertTrue(condition.getParameters().isEmpty() || 
                           condition.getParameters().get(0) == null);
    }

    @Test
    public void testClearParametersNoCondition() {
        RightJoin join = CF.rightJoin("customers");
        
        // Should not throw exception
        join.clearParameters();
    }

    @Test
    public void testToString() {
        RightJoin join = CF.rightJoin("suppliers");
        
        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("suppliers"));
    }

    @Test
    public void testToStringWithCondition() {
        On onClause = CF.on("products.supplier_id", "suppliers.id");
        RightJoin join = CF.rightJoin("suppliers", onClause);
        
        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("suppliers"));
        Assertions.assertTrue(result.contains("ON"));
    }

    @Test
    public void testCopy() {
        On condition = CF.on("orders.customer_id", "customers.id");
        RightJoin original = CF.rightJoin("customers", condition);
        
        RightJoin copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testCopyWithoutCondition() {
        RightJoin original = CF.rightJoin("categories");
        
        RightJoin copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNull(copy.getCondition());
    }

    @Test
    public void testHashCode() {
        RightJoin join1 = CF.rightJoin("products");
        RightJoin join2 = CF.rightJoin("products");
        RightJoin join3 = CF.rightJoin("categories");
        
        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        On condition = CF.on("a.id", "b.a_id");
        
        RightJoin join1 = CF.rightJoin("tableB");
        RightJoin join2 = CF.rightJoin("tableB");
        RightJoin join3 = CF.rightJoin("tableC");
        RightJoin join4 = CF.rightJoin("tableB", condition);
        
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
        On onClause = CF.on("orders.customer_id", "customers.id");
        RightJoin join = CF.rightJoin("customers", onClause);
        
        // Would result in: RIGHT JOIN customers ON orders.customer_id = customers.id
        Assertions.assertEquals(Operator.RIGHT_JOIN, join.getOperator());
        Assertions.assertEquals("customers", join.getJoinEntities().get(0));
    }

    @Test
    public void testPracticalExample2() {
        // Get all products, including those never ordered
        On onClause = CF.on("order_items.product_id", "products.id");
        RightJoin join = CF.rightJoin("products", onClause);
        
        // Would result in: RIGHT JOIN products ON order_items.product_id = products.id
        String result = join.toString();
        Assertions.assertTrue(result.contains("RIGHT JOIN"));
        Assertions.assertTrue(result.contains("products"));
    }

    @Test
    public void testPracticalExample3() {
        // Complex right join with additional conditions
        And complexCondition = CF.and(
            CF.on("sales.product_id", "products.id"),
            CF.eq("products.active", true),
            CF.gt("products.price", 0)
        );
        RightJoin activeProducts = CF.rightJoin("products", complexCondition);
        
        // Gets all active products with price > 0, even if they have no sales
        Assertions.assertEquals(2, activeProducts.getParameters().size());
    }

    @Test
    public void testMultipleTablesJoin() {
        List<String> tables = Arrays.asList("departments", "locations");
        And joinCondition = CF.and(
            CF.on("employees.dept_id", "departments.id"),
            CF.on("departments.location_id", "locations.id")
        );
        
        RightJoin join = CF.rightJoin(tables, joinCondition);
        
        // Gets all departments and locations, even without employees
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertEquals(joinCondition, join.getCondition());
    }
}