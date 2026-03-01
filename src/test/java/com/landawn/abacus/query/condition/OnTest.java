package com.landawn.abacus.query.condition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class OnTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Condition joinCondition = Filters.eq("t1.id", Filters.expr("t2.user_id"));
        On on = Filters.on(joinCondition);

        Assertions.assertNotNull(on);
        Assertions.assertEquals(Operator.ON, on.operator());
        Assertions.assertEquals(joinCondition, on.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        And complexCondition = Filters.and(Filters.eq("a.id", Filters.expr("b.a_id")), Filters.gt("b.created", "2024-01-01"));
        On on = Filters.on(complexCondition);

        Assertions.assertEquals(complexCondition, on.getCondition());
    }

    @Test
    public void testConstructorWithTwoProperties() {
        On on = Filters.on("users.id", "posts.user_id");

        Assertions.assertEquals(Operator.ON, on.operator());
        Assertions.assertNotNull(on.getCondition());
        Assertions.assertTrue(on.getCondition() instanceof Equal);
    }

    @Test
    public void testConstructorWithTableAliases() {
        On on = Filters.on("u.department_id", "d.id");

        Equal condition = (Equal) on.getCondition();
        Assertions.assertEquals("u.department_id", condition.getPropName());
        // The second property becomes an expression
        Assertions.assertTrue(condition.getPropValue() instanceof Expression);
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, String> joinConditions = new LinkedHashMap<>();
        joinConditions.put("orders.customer_id", "customers.id");
        joinConditions.put("orders.store_id", "customers.preferred_store_id");

        On on = Filters.on(joinConditions);

        Assertions.assertEquals(Operator.ON, on.operator());
        Assertions.assertNotNull(on.getCondition());
    }

    @Test
    public void testConstructorWithSingleEntryMap() {
        Map<String, String> joinCondition = new LinkedHashMap<>();
        joinCondition.put("products.category_id", "categories.id");

        On on = Filters.on(joinCondition);

        // Should create a single Equal condition
        Assertions.assertTrue(on.getCondition() instanceof Equal);
    }

    @Test
    public void testConstructorWithEmptyMap() {
        Map<String, String> emptyMap = new LinkedHashMap<>();

        // Empty map should throw IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.on(emptyMap));
    }

    @Test
    public void testCreateOnConditionWithTwoStrings() {
        Condition condition = On.createOnCondition("table1.col1", "table2.col2");

        Assertions.assertTrue(condition instanceof Equal);
        Equal equal = (Equal) condition;
        Assertions.assertEquals("table1.col1", equal.getPropName());
        Assertions.assertTrue(equal.getPropValue() instanceof Expression);
    }

    @Test
    public void testCreateOnConditionWithMap() {
        Map<String, String> joinPairs = new LinkedHashMap<>();
        joinPairs.put("a.id", "b.a_id");
        joinPairs.put("a.type", "b.type");

        Condition condition = On.createOnCondition(joinPairs);

        Assertions.assertTrue(condition instanceof And);
        And and = (And) condition;
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testCreateOnConditionWithSingleEntryMap() {
        Map<String, String> singlePair = new LinkedHashMap<>();
        singlePair.put("users.id", "profiles.user_id");

        Condition condition = On.createOnCondition(singlePair);

        Assertions.assertTrue(condition instanceof Equal);
    }

    @Test
    public void testGetCondition() {
        Equal joinCondition = Filters.eq("t1.id", Filters.expr("t2.id"));
        On on = Filters.on(joinCondition);

        Assertions.assertEquals(joinCondition, on.getCondition());
    }

    @Test
    public void testGetOperator() {
        On on = Filters.on("a.id", "b.a_id");

        Assertions.assertEquals(Operator.ON, on.operator());
    }

    @Test
    public void testToString() {
        On on = Filters.on("employees.dept_id", "departments.id");

        String result = on.toString();
        Assertions.assertTrue(result.contains("ON"));
        Assertions.assertTrue(result.contains("employees.dept_id"));
        Assertions.assertTrue(result.contains("departments.id"));
    }

    @Test
    public void testCopy() {
        On original = Filters.on("t1.id", "t2.foreign_id");

        On copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        On on1 = Filters.on("a.id", "b.a_id");
        On on2 = Filters.on("a.id", "b.a_id");
        On on3 = Filters.on("x.id", "y.x_id");

        Assertions.assertEquals(on1.hashCode(), on2.hashCode());
        Assertions.assertNotEquals(on1.hashCode(), on3.hashCode());
    }

    @Test
    public void testEquals() {
        On on1 = Filters.on("a.id", "b.a_id");
        On on2 = Filters.on("a.id", "b.a_id");
        On on3 = Filters.on("x.id", "y.x_id");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.a_id");
        On on4 = Filters.on(map);

        Assertions.assertTrue(on1.equals(on1));
        Assertions.assertTrue(on1.equals(on2));
        Assertions.assertFalse(on1.equals(on3));
        Assertions.assertTrue(on1.equals(on4)); // Same condition, different construction
        Assertions.assertFalse(on1.equals(null));
        Assertions.assertFalse(on1.equals("not an On"));
    }

    @Test
    public void testPracticalExample1() {
        // Simple column equality
        On on = Filters.on("employees.department_id", "departments.id");

        // Would be used like: JOIN departments ON employees.department_id = departments.id
        String result = on.toString();
        Assertions.assertTrue(result.contains("employees.department_id"));
    }

    @Test
    public void testPracticalExample2() {
        // Multiple join conditions
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("orders.customer_id", "customers.id");
        joinMap.put("orders.region", "customers.region");

        On on = Filters.on(joinMap);

        // Would create: ON orders.customer_id = customers.id AND orders.region = customers.region
        Assertions.assertTrue(on.getCondition() instanceof And);
    }

    @Test
    public void testPracticalExample3() {
        // Complex join with additional conditions
        And complexJoin = Filters.and(Filters.eq("products.category_id", Filters.expr("categories.id")), Filters.eq("products.active", true),
                Filters.gt("products.stock", 0));

        On on = Filters.on(complexJoin);

        Assertions.assertEquals(complexJoin, on.getCondition());
        Assertions.assertEquals(2, on.getCondition().getParameters().size());
    }
}