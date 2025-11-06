package com.landawn.abacus.query.condition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.ConditionFactory.CF;

public class OnTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Condition joinCondition = CF.eq("t1.id", CF.expr("t2.user_id"));
        On on = CF.on(joinCondition);

        Assertions.assertNotNull(on);
        Assertions.assertEquals(Operator.ON, on.getOperator());
        Assertions.assertEquals(joinCondition, on.getCondition());
    }

    @Test
    public void testConstructorWithComplexCondition() {
        And complexCondition = CF.and(CF.eq("a.id", CF.expr("b.a_id")), CF.gt("b.created", "2024-01-01"));
        On on = CF.on(complexCondition);

        Assertions.assertEquals(complexCondition, on.getCondition());
    }

    @Test
    public void testConstructorWithTwoProperties() {
        On on = CF.on("users.id", "posts.user_id");

        Assertions.assertEquals(Operator.ON, on.getOperator());
        Assertions.assertNotNull(on.getCondition());
        Assertions.assertTrue(on.getCondition() instanceof Equal);
    }

    @Test
    public void testConstructorWithTableAliases() {
        On on = CF.on("u.department_id", "d.id");

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

        On on = CF.on(joinConditions);

        Assertions.assertEquals(Operator.ON, on.getOperator());
        Assertions.assertNotNull(on.getCondition());
    }

    @Test
    public void testConstructorWithSingleEntryMap() {
        Map<String, String> joinCondition = new LinkedHashMap<>();
        joinCondition.put("products.category_id", "categories.id");

        On on = CF.on(joinCondition);

        // Should create a single Equal condition
        Assertions.assertTrue(on.getCondition() instanceof Equal);
    }

    @Test
    public void testConstructorWithEmptyMap() {
        Map<String, String> emptyMap = new LinkedHashMap<>();

        On on = CF.on(emptyMap);

        // Empty map should still create an On with an And condition
        Assertions.assertNotNull(on.getCondition());
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
        Equal joinCondition = CF.eq("t1.id", CF.expr("t2.id"));
        On on = CF.on(joinCondition);

        Assertions.assertEquals(joinCondition, on.getCondition());
    }

    @Test
    public void testGetOperator() {
        On on = CF.on("a.id", "b.a_id");

        Assertions.assertEquals(Operator.ON, on.getOperator());
    }

    @Test
    public void testToString() {
        On on = CF.on("employees.dept_id", "departments.id");

        String result = on.toString();
        Assertions.assertTrue(result.contains("ON"));
        Assertions.assertTrue(result.contains("employees.dept_id"));
        Assertions.assertTrue(result.contains("departments.id"));
    }

    @Test
    public void testCopy() {
        On original = CF.on("t1.id", "t2.foreign_id");

        On copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        On on1 = CF.on("a.id", "b.a_id");
        On on2 = CF.on("a.id", "b.a_id");
        On on3 = CF.on("x.id", "y.x_id");

        Assertions.assertEquals(on1.hashCode(), on2.hashCode());
        Assertions.assertNotEquals(on1.hashCode(), on3.hashCode());
    }

    @Test
    public void testEquals() {
        On on1 = CF.on("a.id", "b.a_id");
        On on2 = CF.on("a.id", "b.a_id");
        On on3 = CF.on("x.id", "y.x_id");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.a_id");
        On on4 = CF.on(map);

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
        On on = CF.on("employees.department_id", "departments.id");

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

        On on = CF.on(joinMap);

        // Would create: ON orders.customer_id = customers.id AND orders.region = customers.region
        Assertions.assertTrue(on.getCondition() instanceof And);
    }

    @Test
    public void testPracticalExample3() {
        // Complex join with additional conditions
        And complexJoin = CF.and(CF.eq("products.category_id", CF.expr("categories.id")), CF.eq("products.active", true), CF.gt("products.stock", 0));

        On on = CF.on(complexJoin);

        Assertions.assertEquals(complexJoin, on.getCondition());
        Assertions.assertEquals(2, on.getCondition().getParameters().size());
    }
}