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

public class FullJoinTest extends TestBase {

    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    void testAllRowsFromBothTables() {
        FullJoin join = new FullJoin("orders", new Equal("users.id", "orders.user_id"));
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testComplexCondition() {
        Or complexCondition = Filters.or(Filters.and(Filters.eq("dept.active", true), Filters.isNotNull("emp.id")), Filters.isNull("dept.closed_date"));

        FullJoin join = Filters.fullJoin("employees emp", complexCondition);

        String result = join.toString();
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("employees emp"));
        assertTrue(result.contains("dept.active = true"));
        assertTrue(result.contains("emp.id IS NOT NULL"));
        assertTrue(result.contains("dept.closed_date IS NULL"));
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("AND"));
    }

    @Test
    void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        FullJoin join = new FullJoin(entities, new Equal("departments.id", "person.dept_id"));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testConstructor_Simple() {
        FullJoin join = new FullJoin("departments", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testConstructor_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        assertNotNull(join);
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testConstructorWithJoinEntity() {
        FullJoin join = Filters.fullJoin("departments", ON_AB);

        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertTrue(join.joinEntities().contains("departments"));
        assertSame(ON_AB, join.condition());
    }

    @Test
    void testConstructorWithJoinEntityAndCondition() {
        Equal eq = Filters.eq("departments.id", "employees.dept_id");
        FullJoin join = Filters.fullJoin("employees", eq);

        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertTrue(join.joinEntities().contains("employees"));
        assertEquals(eq, join.condition());
    }

    @Test
    void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        Equal eq = Filters.eq("departments.id", "person.dept_id");
        FullJoin join = Filters.fullJoin(entities, eq);

        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
        assertEquals(2, join.joinEntities().size());
        assertTrue(join.joinEntities().containsAll(entities));
        assertEquals(eq, join.condition());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedSingleArgConstructorAlwaysThrows() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FullJoin("orders"));
        assertTrue(ex.getMessage().contains("FULL JOIN requires a non-null ON/USING predicate"), ex.getMessage());

        // The advertised alternatives work.
        assertEquals("FULL JOIN orders ON a.id = b.id", new FullJoin("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEntityValidationPrecedesPredicateCheck() {
        // A null/blank entity is reported as such, not as a missing join predicate.
        final String entityMessage = "must not be null, empty, or blank";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FullJoin((String) null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains("requires a non-null ON/USING predicate"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new FullJoin("   ", null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
    }

    @Test
    void testEquals() {
        Equal eq1 = Filters.eq("dept.id", "emp.dept_id");
        Equal eq2 = Filters.eq("dept.id", "emp.dept_id");

        FullJoin join1 = Filters.fullJoin("employees", eq1);
        FullJoin join2 = Filters.fullJoin("employees", eq2);
        FullJoin join3 = Filters.fullJoin("departments", eq1);
        FullJoin join4 = Filters.fullJoin("employees", Filters.eq("dept.id", "other"));

        assertEquals(join1, join1);
        assertEquals(join1, join2);
        assertNotEquals(join1, join3); // Different entity
        assertNotEquals(join1, join4); // Different condition
        assertNotEquals(join1, null);
        assertNotEquals(join1, "string");
    }

    @Test
    void testEquals_DifferentEntities() {
        FullJoin join1 = new FullJoin("orders", ON_AB);
        FullJoin join2 = new FullJoin("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    void testEquals_EqualObjects() {
        FullJoin join1 = new FullJoin("orders o", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    void testEquals_Null() {
        FullJoin join = new FullJoin("orders", ON_AB);
        assertNotEquals(null, join);
    }

    @Test
    void testEquals_SameObject() {
        FullJoin join = new FullJoin("orders", ON_AB);
        assertEquals(join, join);
    }

    @Test
    void testFindDataMismatches() {
        FullJoin join = new FullJoin("warehouse_inventory", new Equal("online_inventory.product_id", "warehouse_inventory.product_id"));
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("users.id", "orders.user_id");
        FullJoin join = new FullJoin("orders", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testGetCondition_Null() {
        // A FULL JOIN can no longer be condition-less: the single-argument form always throws.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FullJoin("departments"));
        assertTrue(ex.getMessage().contains("FULL JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new FullJoin("departments", null));
        assertThrows(IllegalArgumentException.class, () -> new FullJoin(Arrays.asList("departments", "teams"), null));
    }

    @Test
    void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        FullJoin join = new FullJoin(entities, Filters.on("table1.id", "table2.id"));
        List<String> result = join.joinEntities();
        assertEquals(2, result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    void testHashCode() {
        FullJoin join1 = new FullJoin("orders", new Equal("a", "b"));
        FullJoin join2 = new FullJoin("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    void testMultiTableFullJoin() {
        List<String> tables = Arrays.asList("system_a_data", "system_b_data");
        FullJoin join = new FullJoin(tables, new Equal("master_data.record_id", "source.record_id"));
        assertEquals(2, join.joinEntities().size());
    }

    @Test
    void testParameters() {
        Between between = Filters.between("salary", 40000, 80000);
        FullJoin join = Filters.fullJoin("employees", between);

        List<Object> params = join.parameters();
        assertEquals(2, params.size());
        assertEquals(40000, params.get(0));
        assertEquals(80000, params.get(1));
    }

    @Test
    void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        FullJoin join = new FullJoin("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    void testParameters_WithCondition() {
        FullJoin join = new FullJoin("products", new Equal("active", true));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    void testParametersWithoutCondition() {
        // No bound parameters when the predicate compares columns only.
        FullJoin join = Filters.fullJoin("departments", ON_AB);

        List<Object> params = join.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testReconcileTwoDataSources() {
        FullJoin join = new FullJoin("external_users", new Equal("internal_users.email", "external_users.email"));
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    void testToString() {
        FullJoin join = Filters.fullJoin("orders", ON_AB);
        String result = join.toString();

        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    void testToString_Simple() {
        FullJoin join = new FullJoin("departments", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    void testToString_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("employees"));
    }

    @Test
    void testToStringWithCondition() {
        And condition = Filters.and(Filters.eq("users.id", "orders.user_id"), Filters.ne("orders.status", "cancelled"));
        FullJoin join = Filters.fullJoin("orders", condition);
        String result = join.toString();

        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("orders"));
        assertTrue(result.contains("users.id = 'orders.user_id'"));
        assertTrue(result.contains("orders.status != 'cancelled'"));
    }

    @Test
    void testToStringWithMultipleEntitiesAndCondition() {
        List<String> entities = Arrays.asList("table1", "table2", "table3");
        GreaterThan gt = Filters.gt("amount", 0);
        FullJoin join = Filters.fullJoin(entities, gt);
        String result = join.toString();

        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
        assertTrue(result.contains("table3"));
        assertTrue(result.contains("amount > 0"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("departmentId", Filters.expr("employeeDeptId"));
        FullJoin join = Filters.fullJoin("employees", eq);

        String result = join.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("employees"));
        assertTrue(result.contains("department_id"));
        assertTrue(result.contains("employee_dept_id"));
    }
}
