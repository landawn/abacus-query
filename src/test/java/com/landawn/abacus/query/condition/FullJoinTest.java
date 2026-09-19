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
public class FullJoinTest extends TestBase {
    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    public void testConstructor_Simple() {
        FullJoin join = new FullJoin("departments", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        assertNotNull(join);
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        FullJoin join = new FullJoin(entities, new Equal("departments.id", "person.dept_id"));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        FullJoin join = new FullJoin(entities, Filters.on("table1.id", "table2.id"));
        List<String> result = join.joinEntities();
        assertEquals(2, result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("users.id", "orders.user_id");
        FullJoin join = new FullJoin("orders", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCondition_Null() {
        // A FULL JOIN can no longer be condition-less: the single-argument form always throws.
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin("departments"));
        assertTrue(ex.getMessage().contains("FULL JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin("departments", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin(Arrays.asList("departments", "teams"), null));
    }

    @Test
    public void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        FullJoin join = new FullJoin("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    public void testParameters_WithCondition() {
        FullJoin join = new FullJoin("products", new Equal("active", true));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testToString_Simple() {
        FullJoin join = new FullJoin("departments", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("FULL JOIN"));
        assertTrue(result.contains("departments"));
    }

    @Test
    public void testToString_WithCondition() {
        FullJoin join = new FullJoin("employees", new Equal("departments.id", "employees.dept_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
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
        FullJoin join = new FullJoin("orders", ON_AB);
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
        FullJoin join1 = new FullJoin("orders", ON_AB);
        FullJoin join2 = new FullJoin("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        FullJoin join = new FullJoin("orders", ON_AB);
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
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testMultiTableFullJoin() {
        List<String> tables = Arrays.asList("system_a_data", "system_b_data");
        FullJoin join = new FullJoin(tables, new Equal("master_data.record_id", "source.record_id"));
        assertEquals(2, join.joinEntities().size());
    }

    @Test
    public void testFindDataMismatches() {
        FullJoin join = new FullJoin("warehouse_inventory", new Equal("online_inventory.product_id", "warehouse_inventory.product_id"));
        assertNotNull(join.condition());
        assertEquals(Operator.FULL_JOIN, join.operator());
    }

    @Test
    public void testConstructorWithJoinEntity() {
        FullJoin join = Filters.fullJoin("departments", ON_AB);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(1, join.joinEntities().size());
        Assertions.assertTrue(join.joinEntities().contains("departments"));
        Assertions.assertSame(ON_AB, join.condition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = Filters.eq("departments.id", "employees.dept_id");
        FullJoin join = Filters.fullJoin("employees", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(1, join.joinEntities().size());
        Assertions.assertTrue(join.joinEntities().contains("employees"));
        Assertions.assertEquals(eq, join.condition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        Equal eq = Filters.eq("departments.id", "person.dept_id");
        FullJoin join = Filters.fullJoin(entities, eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.operator());
        Assertions.assertEquals(2, join.joinEntities().size());
        Assertions.assertTrue(join.joinEntities().containsAll(entities));
        Assertions.assertEquals(eq, join.condition());
    }

    @Test
    public void testToString() {
        FullJoin join = Filters.fullJoin("orders", ON_AB);
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
    public void testParameters() {
        Between between = Filters.between("salary", 40000, 80000);
        FullJoin join = Filters.fullJoin("employees", between);

        List<Object> params = join.parameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(40000, params.get(0));
        Assertions.assertEquals(80000, params.get(1));
    }

    @Test
    public void testParametersWithoutCondition() {
        // No bound parameters when the predicate compares columns only.
        FullJoin join = Filters.fullJoin("departments", ON_AB);

        List<Object> params = join.parameters();
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
        FullJoin join4 = Filters.fullJoin("employees", Filters.eq("dept.id", "other"));

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3); // Different entity
        Assertions.assertNotEquals(join1, join4); // Different condition
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("departmentId", Filters.expr("employeeDeptId"));
        FullJoin join = Filters.fullJoin("employees", eq);

        String result = join.toSql(NamingPolicy.SNAKE_CASE);

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

    @Test
    @SuppressWarnings("deprecation")
    public void testEntityValidationPrecedesPredicateCheck() {
        // A null/blank entity is reported as such, not as a missing join predicate.
        final String entityMessage = "must not be null, empty, or blank";

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin((String) null));
        Assertions.assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        Assertions.assertFalse(ex.getMessage().contains("requires a non-null ON/USING predicate"), ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin("   ", null));
        Assertions.assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedSingleArgConstructorAlwaysThrows() {
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> new FullJoin("orders"));
        Assertions.assertTrue(ex.getMessage().contains("FULL JOIN requires a non-null ON/USING predicate"), ex.getMessage());

        // The advertised alternatives work.
        Assertions.assertEquals("FULL JOIN orders ON a.id = b.id", new FullJoin("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        Assertions.assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }
}
