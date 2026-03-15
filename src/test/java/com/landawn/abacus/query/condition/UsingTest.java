package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Using2025Test extends TestBase {

    @Test
    public void testConstructor_SingleColumn() {
        Using using = new Using("department_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testConstructor_MultipleColumns() {
        Using using = new Using("company_id", "branch_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testConstructor_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("tenant_id", "workspace_id"));
        Using using = new Using(columns);
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testConstructor_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((String[]) null);
        });
    }

    @Test
    public void testConstructor_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(new String[0]);
        });
    }

    @Test
    public void testConstructor_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((List<String>) null);
        });
    }

    @Test
    public void testConstructor_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(Arrays.asList());
        });
    }

    @Test
    public void testGetCondition() {
        Using using = new Using("employee_id");
        Condition condition = using.getCondition();
        assertNotNull(condition);
    }

    @Test
    public void testGetParameters() {
        Using using = new Using("customer_id");
        List<Object> params = using.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Using using = new Using("order_id");
        using.clearParameters();
        assertTrue(using.getParameters().isEmpty());
    }

    @Test
    public void testToString() {
        Using using = new Using("employee_id");
        String result = using.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("USING"));
    }

    @Test
    public void testToString_SingleColumnUsesParentheses() {
        Using using = new Using("employee_id");
        String result = using.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("USING (employee_id)"));
    }

    @Test
    public void testHashCode() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1.hashCode(), using2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Using using = new Using("id");
        assertEquals(using, using);
    }

    @Test
    public void testEquals_EqualObjects() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1, using2);
    }

    @Test
    public void testEquals_Null() {
        Using using = new Using("id");
        assertNotEquals(null, using);
    }

    @Test
    public void testCreateUsingCondition_Array() {
        Condition condition = Using.createUsingCondition("customer_id", "order_date");
        assertNotNull(condition);
    }

    @Test
    public void testCreateUsingCondition_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("company_id", "branch_id"));
        Condition condition = Using.createUsingCondition(columns);
        assertNotNull(condition);
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((String[]) null);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(new String[0]);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnEmptyElementInArray() {
        assertThrows(IllegalArgumentException.class, () -> Using.createUsingCondition("id", ""));
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((List<String>) null);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(Arrays.asList());
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnNullElementInCollection() {
        assertThrows(IllegalArgumentException.class, () -> Using.createUsingCondition(Arrays.asList("id", null)));
    }

    @Test
    public void testCompositeKeyJoin() {
        Using using = new Using("company_id", "department_id", "team_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testDynamicColumnList() {
        List<String> sharedColumns = Arrays.asList("tenant_id", "organization_id");
        Using using = new Using(sharedColumns);
        assertNotNull(using);
    }
}

public class UsingTest extends TestBase {

    @Test
    public void testConstructorWithVarArgs() {
        Using using = Filters.using("department_id");

        Assertions.assertNotNull(using);
        Assertions.assertEquals(Operator.USING, using.operator());
        Assertions.assertNotNull(using.getCondition());
    }

    @Test
    public void testConstructorWithMultipleColumns() {
        Using using = Filters.using("company_id", "department_id");

        Assertions.assertEquals(Operator.USING, using.operator());
        String result = using.toString();
        Assertions.assertTrue(result.contains("company_id"));
        Assertions.assertTrue(result.contains("department_id"));
    }

    @Test
    public void testConstructorWithEmptyVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.using();
        });
    }

    @Test
    public void testConstructorWithNullVarArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.using((String[]) null);
        });
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> columns = Arrays.asList("customer_id", "order_date");
        Using using = Filters.using(columns);

        Assertions.assertEquals(Operator.USING, using.operator());
        String result = using.toString();
        Assertions.assertTrue(result.contains("customer_id"));
        Assertions.assertTrue(result.contains("order_date"));
    }

    @Test
    public void testConstructorWithSet() {
        Set<String> columns = new HashSet<>();
        columns.add("tenant_id");
        columns.add("workspace_id");
        Using using = Filters.using(columns);

        Assertions.assertEquals(Operator.USING, using.operator());
        Assertions.assertNotNull(using.getCondition());
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.using(emptyList);
        });
    }

    @Test
    public void testConstructorWithNullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.using((List<String>) null);
        });
    }

    @Test
    public void testCreateUsingConditionWithVarArgs() {
        Using condition = Filters.using("col1", "col2", "col3");

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
        Using condition = Filters.using(columns);

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
        Using using = Filters.using("employee_id");

        Assertions.assertNotNull(using.getCondition());
        Assertions.assertTrue(using.getCondition() instanceof Expression);
    }

    @Test
    public void testGetOperator() {
        Using using = Filters.using("department_id");

        Assertions.assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testToString() {
        Using using = Filters.using("branch_id", "department_id");

        String result = using.toString();
        Assertions.assertTrue(result.contains("USING"));
        Assertions.assertTrue(result.contains("branch_id"));
        Assertions.assertTrue(result.contains("department_id"));
    }

    @Test
    public void testHashCode() {
        Using using1 = Filters.using("department_id");
        Using using2 = Filters.using("department_id");
        Using using3 = Filters.using("employee_id");

        Assertions.assertEquals(using1.hashCode(), using2.hashCode());
        Assertions.assertNotEquals(using1.hashCode(), using3.hashCode());
    }

    @Test
    public void testEquals() {
        Using using1 = Filters.using("department_id");
        Using using2 = Filters.using("department_id");
        Using using3 = Filters.using("employee_id");
        Using using4 = Filters.using("department_id", "employee_id");

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
        Using using = Filters.using("employee_id");

        // Would be used like: JOIN departments USING (employee_id)
        Assertions.assertEquals(Operator.USING, using.operator());
    }

    @Test
    public void testPracticalExample2() {
        // Multiple column join
        Using using = Filters.using("branch_id", "department_id");

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

        Using using = Filters.using(commonColumns);

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
        Using using = Filters.using(orderedColumns);

        String result = using.toString();
        int firstIndex = result.indexOf("first");
        int secondIndex = result.indexOf("second");
        int thirdIndex = result.indexOf("third");

        // Verify order is maintained
        Assertions.assertTrue(firstIndex < secondIndex);
        Assertions.assertTrue(secondIndex < thirdIndex);
    }
}
