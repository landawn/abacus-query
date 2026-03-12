package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class On2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleEquality() {
        On on = new On("orders.customer_id", "customers.id");
        assertNotNull(on);
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        On on = new On(condition);
        assertNotNull(on);
        assertNotNull(on.getCondition());
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    public void testConstructor_WithMap() {
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("emp.department_id", "dept.id");
        joinMap.put("emp.location_id", "dept.location_id");
        On on = new On(joinMap);
        assertNotNull(on);
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("users.id", "posts.user_id");
        On on = new On(condition);
        Condition retrieved = on.getCondition();
        assertNotNull(retrieved);
    }

    @Test
    public void testGetParameters_Empty() {
        On on = new On("a.id", "b.id");
        List<Object> params = on.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_WithValues() {
        And complexCondition = new And(new Equal("orders.customer_id", "customers.id"), new Equal("orders.status", "active"));
        On on = new On(complexCondition);
        List<Object> params = on.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals("customers.id", params.get(0));
        assertEquals("active", params.get(1));
    }

    @Test
    public void testClearParameters() {
        And condition = new And(new Equal("a.id", "b.id"), new Equal("a.status", "pending"));
        On on = new On(condition);
        assertFalse(on.getParameters().isEmpty());
        on.clearParameters();
        List<Object> params = on.getParameters();
        assertTrue(params.size() == 2 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString() {
        On on = new On("orders.customer_id", "customers.id");
        String result = on.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ON"));
    }

    @Test
    public void testHashCode() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1.hashCode(), on2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        On on = new On("a.id", "b.id");
        assertEquals(on, on);
    }

    @Test
    public void testEquals_EqualObjects() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1, on2);
    }

    @Test
    public void testEquals_DifferentColumns() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("c.id", "d.id");
        assertNotEquals(on1, on2);
    }

    @Test
    public void testEquals_Null() {
        On on = new On("a.id", "b.id");
        assertNotEquals(null, on);
    }

    @Test
    public void testCreateOnCondition_Simple() {
        Condition condition = On.createOnCondition("users.id", "posts.user_id");
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    public void testCreateOnCondition_ThrowsOnEmptyRightPropName() {
        assertThrows(IllegalArgumentException.class, () -> On.createOnCondition("users.id", ""));
    }

    @Test
    public void testCreateOnCondition_Map_SingleEntry() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    public void testCreateOnCondition_Map_MultipleEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        map.put("a.code", "b.code");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof And);
    }

    @Test
    public void testCreateOnCondition_Map_ThrowsOnEmptyRightValue() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "");
        assertThrows(IllegalArgumentException.class, () -> On.createOnCondition(map));
    }

    @Test
    public void testComplexCondition() {
        And complexCondition = new And(new Equal("orders.customer_id", "customers.id"), new GreaterThan("orders.order_date", "customers.registration_date"));
        On on = new On(complexCondition);
        assertNotNull(on.getCondition());
    }

    @Test
    public void testCompositeKey() {
        Map<String, String> compositeKey = new LinkedHashMap<>();
        compositeKey.put("order_items.order_id", "orders.id");
        compositeKey.put("order_items.customer_id", "orders.customer_id");
        On on = new On(compositeKey);
        assertNotNull(on.getCondition());
    }

    @Test
    public void testWithAdditionalFilters() {
        And filteredJoin = new And(new Equal("products.category_id", "categories.id"), new Equal("categories.active", true));
        On on = new On(filteredJoin);
        assertEquals(2, (int) on.getParameters().size());
    }
}

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
