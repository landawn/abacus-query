package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class FullJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        FullJoin join = Filters.fullJoin("departments");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("departments"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = Filters.eq("departments.id", "employees.dept_id");
        FullJoin join = Filters.fullJoin("employees", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
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
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
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
    public void testClearParameters() {
        In in = Filters.in("department_id", Arrays.asList(10, 20, 30));
        FullJoin join = Filters.fullJoin("departments", in);

        Assertions.assertEquals(3, join.getParameters().size());

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.size() == 3 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Like like = Filters.like("name", "%Corp%");
        List<String> entities = Arrays.asList("companies", "subsidiaries");
        FullJoin original = Filters.fullJoin(entities, like);

        FullJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
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

        String result = join.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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