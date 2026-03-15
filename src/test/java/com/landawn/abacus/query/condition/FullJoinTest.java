package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class FullJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        FullJoin join = new FullJoin("departments");
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        FullJoin join = new FullJoin(entities, new Equal("departments.id", "person.dept_id"));
        assertNotNull(join);
        assertEquals(2, (int) join.getJoinEntities().size());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        FullJoin join = new FullJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("users.id", "orders.user_id");
        FullJoin join = new FullJoin("orders", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        FullJoin join = new FullJoin("departments");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        FullJoin join = new FullJoin("orders");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        FullJoin join = new FullJoin("products", new Equal("active", true));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testToString_Simple() {
        FullJoin join = new FullJoin("departments");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    public void testToString_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testHashCode() {
        FullJoin join1 = new FullJoin("orders", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        FullJoin join = new FullJoin("orders");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        FullJoin join1 = new FullJoin("orders o", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        FullJoin join1 = new FullJoin("orders");
        FullJoin join2 = new FullJoin("products");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        FullJoin join = new FullJoin("orders");
        assertNotEquals(null, join);
    }

    @Test
    public void testAllRowsFromBothTables() {
        FullJoin join = new FullJoin("orders", new Equal("users.id", "orders.user_id"));
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testReconcileTwoDataSources() {
        FullJoin join = new FullJoin("external_users", new Equal("internal_users.email", "external_users.email"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testMultiTableFullJoin() {
        List<String> tables = Arrays.asList("system_a_data", "system_b_data");
        FullJoin join = new FullJoin(tables, new Equal("master_data.record_id", "source.record_id"));
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testFindDataMismatches() {
        FullJoin join = new FullJoin("warehouse_inventory", new Equal("online_inventory.product_id", "warehouse_inventory.product_id"));
        assertNotNull(join.getCondition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }
}

public class FullJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        FullJoin join = Filters.fullJoin("departments");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("departments"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = Filters.eq("departments.id", "employees.dept_id");
        FullJoin join = Filters.fullJoin("employees", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("employees"));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        Equal eq = Filters.eq("departments.id", "person.dept_id");
        FullJoin join = Filters.fullJoin(entities, eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testToString() {
        FullJoin join = Filters.fullJoin("orders");
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("orders"));
    }

    @Test
    public void testToStringWithCondition() {
        And condition = Filters.and(Filters.eq("users.id", "orders.user_id"), Filters.ne("orders.status", "cancelled"));
        FullJoin join = Filters.fullJoin("orders", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("orders"));
        Assertions.assertTrue(result.contains("users.id = 'orders.user_id'"));
        Assertions.assertTrue(result.contains("orders.status != 'cancelled'"));
    }

    @Test
    public void testToStringWithMultipleEntitiesAndCondition() {
        List<String> entities = Arrays.asList("table1", "table2", "table3");
        GreaterThan gt = Filters.gt("amount", 0);
        FullJoin join = Filters.fullJoin(entities, gt);
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("table1"));
        Assertions.assertTrue(result.contains("table2"));
        Assertions.assertTrue(result.contains("table3"));
        Assertions.assertTrue(result.contains("amount > 0"));
    }

    @Test
    public void testGetParameters() {
        Between between = Filters.between("salary", 40000, 80000);
        FullJoin join = Filters.fullJoin("employees", between);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(40000, params.get(0));
        Assertions.assertEquals(80000, params.get(1));
    }

    @Test
    public void testGetParametersWithoutCondition() {
        FullJoin join = Filters.fullJoin("departments");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testEquals() {
        Equal eq1 = Filters.eq("dept.id", "emp.dept_id");
        Equal eq2 = Filters.eq("dept.id", "emp.dept_id");

        FullJoin join1 = Filters.fullJoin("employees", eq1);
        FullJoin join2 = Filters.fullJoin("employees", eq2);
        FullJoin join3 = Filters.fullJoin("departments", eq1);
        FullJoin join4 = Filters.fullJoin("employees");

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3); // Different entity
        Assertions.assertNotEquals(join1, join4); // No condition vs with condition
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testHashCode() {
        NotEqual ne = Filters.ne("status", "inactive");
        FullJoin join1 = Filters.fullJoin("records", ne);
        FullJoin join2 = Filters.fullJoin("records", ne);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("departmentId", Filters.expr("employeeDeptId"));
        FullJoin join = Filters.fullJoin("employees", eq);

        String result = join.toString(NamingPolicy.SNAKE_CASE);

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("employees"));
        Assertions.assertTrue(result.contains("department_id"));
        Assertions.assertTrue(result.contains("employee_dept_id"));
    }

    @Test
    public void testComplexCondition() {
        Or complexCondition = Filters.or(Filters.and(Filters.eq("dept.active", true), Filters.isNotNull("emp.id")), Filters.isNull("dept.closed_date"));

        FullJoin join = Filters.fullJoin("employees emp", complexCondition);

        String result = join.toString();
        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("employees emp"));
        Assertions.assertTrue(result.contains("dept.active = true"));
        Assertions.assertTrue(result.contains("emp.id IS NOT NULL"));
        Assertions.assertTrue(result.contains("dept.closed_date IS NULL"));
        Assertions.assertTrue(result.contains("OR"));
        Assertions.assertTrue(result.contains("AND"));
    }
}
