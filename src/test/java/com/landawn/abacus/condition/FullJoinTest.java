package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class FullJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        FullJoin join = ConditionFactory.fullJoin("departments");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("departments"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = ConditionFactory.eq("departments.id", "employees.dept_id");
        FullJoin join = ConditionFactory.fullJoin("employees", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("employees"));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("employees", "contractors");
        Equal eq = ConditionFactory.eq("departments.id", "person.dept_id");
        FullJoin join = ConditionFactory.fullJoin(entities, eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.FULL_JOIN, join.getOperator());
        Assertions.assertEquals(2, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testToString() {
        FullJoin join = ConditionFactory.fullJoin("orders");
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("orders"));
    }

    @Test
    public void testToStringWithCondition() {
        And condition = ConditionFactory.and(ConditionFactory.eq("users.id", "orders.user_id"), ConditionFactory.ne("orders.status", "cancelled"));
        FullJoin join = ConditionFactory.fullJoin("orders", condition);
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("orders"));
        Assertions.assertTrue(result.contains("users.id = 'orders.user_id'"));
        Assertions.assertTrue(result.contains("orders.status != 'cancelled'"));
    }

    @Test
    public void testToStringWithMultipleEntitiesAndCondition() {
        List<String> entities = Arrays.asList("table1", "table2", "table3");
        GreaterThan gt = ConditionFactory.gt("amount", 0);
        FullJoin join = ConditionFactory.fullJoin(entities, gt);
        String result = join.toString();

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("table1"));
        Assertions.assertTrue(result.contains("table2"));
        Assertions.assertTrue(result.contains("table3"));
        Assertions.assertTrue(result.contains("amount > 0"));
    }

    @Test
    public void testGetParameters() {
        Between between = ConditionFactory.between("salary", 40000, 80000);
        FullJoin join = ConditionFactory.fullJoin("employees", between);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(40000, params.get(0));
        Assertions.assertEquals(80000, params.get(1));
    }

    @Test
    public void testGetParametersWithoutCondition() {
        FullJoin join = ConditionFactory.fullJoin("departments");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        In in = ConditionFactory.in("department_id", Arrays.asList(10, 20, 30));
        FullJoin join = ConditionFactory.fullJoin("departments", in);

        Assertions.assertEquals(3, join.getParameters().size());

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.size() == 3 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Like like = ConditionFactory.like("name", "%Corp%");
        List<String> entities = Arrays.asList("companies", "subsidiaries");
        FullJoin original = ConditionFactory.fullJoin(entities, like);

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
        Equal eq1 = ConditionFactory.eq("dept.id", "emp.dept_id");
        Equal eq2 = ConditionFactory.eq("dept.id", "emp.dept_id");

        FullJoin join1 = ConditionFactory.fullJoin("employees", eq1);
        FullJoin join2 = ConditionFactory.fullJoin("employees", eq2);
        FullJoin join3 = ConditionFactory.fullJoin("departments", eq1);
        FullJoin join4 = ConditionFactory.fullJoin("employees");

        Assertions.assertEquals(join1, join1);
        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3); // Different entity
        Assertions.assertNotEquals(join1, join4); // No condition vs with condition
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testHashCode() {
        NotEqual ne = ConditionFactory.ne("status", "inactive");
        FullJoin join1 = ConditionFactory.fullJoin("records", ne);
        FullJoin join2 = ConditionFactory.fullJoin("records", ne);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = ConditionFactory.eq("departmentId", CF.expr("employeeDeptId"));
        FullJoin join = ConditionFactory.fullJoin("employees", eq);

        String result = join.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("FULL JOIN"));
        Assertions.assertTrue(result.contains("employees"));
        Assertions.assertTrue(result.contains("department_id"));
        Assertions.assertTrue(result.contains("employee_dept_id"));
    }

    @Test
    public void testComplexCondition() {
        Or complexCondition = ConditionFactory.or(ConditionFactory.and(ConditionFactory.eq("dept.active", true), ConditionFactory.isNotNull("emp.id")),
                ConditionFactory.isNull("dept.closed_date"));

        FullJoin join = ConditionFactory.fullJoin("employees emp", complexCondition);

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