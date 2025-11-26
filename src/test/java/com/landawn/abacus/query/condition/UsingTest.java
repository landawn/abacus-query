package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Filters.CF;

public class UsingTest extends TestBase {

    @Test
    public void testConstructorWithVarArgs() {
        Using using = CF.using("department_id");

        Assertions.assertNotNull(using);
        Assertions.assertEquals(Operator.USING, using.getOperator());
        Assertions.assertNotNull(using.getCondition());
    }

    @Test
    public void testConstructorWithMultipleColumns() {
        Using using = CF.using("company_id", "department_id");

        Assertions.assertEquals(Operator.USING, using.getOperator());
        String result = using.toString();
        Assertions.assertTrue(result.contains("company_id"));
        Assertions.assertTrue(result.contains("department_id"));
    }

    @Test
    public void testConstructorWithEmptyVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.using();
        });
    }

    @Test
    public void testConstructorWithNullVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.using((String[]) null);
        });
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> columns = Arrays.asList("customer_id", "order_date");
        Using using = CF.using(columns);

        Assertions.assertEquals(Operator.USING, using.getOperator());
        String result = using.toString();
        Assertions.assertTrue(result.contains("customer_id"));
        Assertions.assertTrue(result.contains("order_date"));
    }

    @Test
    public void testConstructorWithSet() {
        Set<String> columns = new HashSet<>();
        columns.add("tenant_id");
        columns.add("workspace_id");
        Using using = CF.using(columns);

        Assertions.assertEquals(Operator.USING, using.getOperator());
        Assertions.assertNotNull(using.getCondition());
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.using(emptyList);
        });
    }

    @Test
    public void testConstructorWithNullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.using((List<String>) null);
        });
    }

    @Test
    public void testCreateUsingConditionWithVarArgs() {
        Using condition = CF.using("col1", "col2", "col3");

        Assertions.assertNotNull(condition);
    }

    @Test
    public void testCreateUsingConditionWithEmptyVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition();
        });
    }

    @Test
    public void testCreateUsingConditionWithNullVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((String[]) null);
        });
    }

    @Test
    public void testCreateUsingConditionWithCollection() {
        List<String> columns = Arrays.asList("id", "name");
        Using condition = CF.using(columns);

        Assertions.assertNotNull(condition);
    }

    @Test
    public void testCreateUsingConditionWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(emptyList);
        });
    }

    @Test
    public void testCreateUsingConditionWithNullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((List<String>) null);
        });
    }

    @Test
    public void testGetCondition() {
        Using using = CF.using("employee_id");

        Assertions.assertNotNull(using.getCondition());
        Assertions.assertTrue(using.getCondition() instanceof Expression);
    }

    @Test
    public void testGetOperator() {
        Using using = CF.using("department_id");

        Assertions.assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testToString() {
        Using using = CF.using("branch_id", "department_id");

        String result = using.toString();
        Assertions.assertTrue(result.contains("USING"));
        Assertions.assertTrue(result.contains("branch_id"));
        Assertions.assertTrue(result.contains("department_id"));
    }

    @Test
    public void testCopy() {
        Using original = CF.using("customer_id", "region_id");

        Using copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        Using using1 = CF.using("department_id");
        Using using2 = CF.using("department_id");
        Using using3 = CF.using("employee_id");

        Assertions.assertEquals(using1.hashCode(), using2.hashCode());
        Assertions.assertNotEquals(using1.hashCode(), using3.hashCode());
    }

    @Test
    public void testEquals() {
        Using using1 = CF.using("department_id");
        Using using2 = CF.using("department_id");
        Using using3 = CF.using("employee_id");
        Using using4 = CF.using("department_id", "employee_id");

        Assertions.assertTrue(using1.equals(using1));
        Assertions.assertTrue(using1.equals(using2));
        Assertions.assertFalse(using1.equals(using3));
        Assertions.assertFalse(using1.equals(using4));
        Assertions.assertFalse(using1.equals(null));
        Assertions.assertFalse(using1.equals("not a Using"));
    }

    @Test
    public void testPracticalExample1() {
        // Simple single column join
        Using using = CF.using("employee_id");

        // Would be used like: JOIN departments USING (employee_id)
        Assertions.assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testPracticalExample2() {
        // Multiple column join
        Using using = CF.using("branch_id", "department_id");

        // Would be used like: JOIN other_table USING (branch_id, department_id)
        String result = using.toString();
        Assertions.assertTrue(result.contains("branch_id"));
        Assertions.assertTrue(result.contains("department_id"));
    }

    @Test
    public void testPracticalExample3() {
        // Using with collection for dynamic column lists
        Set<String> commonColumns = new HashSet<>();
        commonColumns.add("tenant_id");
        commonColumns.add("workspace_id");
        commonColumns.add("project_id");

        Using using = CF.using(commonColumns);

        // All columns should be present in the condition
        String result = using.toString();
        Assertions.assertTrue(result.contains("tenant_id"));
        Assertions.assertTrue(result.contains("workspace_id"));
        Assertions.assertTrue(result.contains("project_id"));
    }

    @Test
    public void testOrderPreservation() {
        // Test that order is preserved with List
        List<String> orderedColumns = Arrays.asList("first", "second", "third");
        Using using = CF.using(orderedColumns);

        String result = using.toString();
        int firstIndex = result.indexOf("first");
        int secondIndex = result.indexOf("second");
        int thirdIndex = result.indexOf("third");

        // Verify order is maintained
        Assertions.assertTrue(firstIndex < secondIndex);
        Assertions.assertTrue(secondIndex < thirdIndex);
    }
}