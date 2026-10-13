package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class UsingTest extends TestBase {

    @Test
    void testCompositeKeyJoin() {
        Using using = new Using("company_id", "department_id", "team_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testConstructor_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("tenant_id", "workspace_id"));
        Using using = new Using(columns);
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testConstructor_MultipleColumns() {
        Using using = new Using("company_id", "branch_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testConstructor_SingleColumn() {
        Using using = new Using("department_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testConstructor_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(new String[0]);
        });
    }

    @Test
    void testConstructor_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(Arrays.asList());
        });
    }

    @Test
    void testConstructor_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((String[]) null);
        });
    }

    @Test
    void testConstructor_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((List<String>) null);
        });
    }

    @Test
    void testConstructorRejectsBlankColumnNames() {
        assertThrows(IllegalArgumentException.class, () -> Filters.using("   "));
        assertThrows(IllegalArgumentException.class, () -> Filters.using(Arrays.asList("id", "   ")));
    }

    @Test
    void testConstructorRejectsMultiColumnOrParenthesizedNames() {
        // A name like "a, b" would render as USING (a, b) while columnNames() reported the
        // single element ["a, b"], desyncing the structured accessor from the generated SQL.
        assertThrows(IllegalArgumentException.class, () -> new Using("a, b"));
        assertThrows(IllegalArgumentException.class, () -> new Using("a,b"));
        assertThrows(IllegalArgumentException.class, () -> new Using("(id)"));
        assertThrows(IllegalArgumentException.class, () -> new Using("id)"));
        assertThrows(IllegalArgumentException.class, () -> new Using(Arrays.asList("id", "a, b")));
        assertThrows(IllegalArgumentException.class, () -> Filters.using("a, b"));

        // Plain unqualified names are still accepted, and the structured accessor matches the SQL.
        Using using = new Using("company_id", "branch_id");
        assertEquals(Arrays.asList("company_id", "branch_id"), using.columnNames());
        assertEquals("USING (company_id, branch_id)", using.toString());
    }

    @Test
    void testConstructorRejectsQualifiedColumnName() {
        // The condition's strict dot check also rejects quoted dots; callers need the offending name
        // and the builder guidance to distinguish that case from an actually qualified column.
        for (final String columnName : List.of("orders.customer_id", "\"quoted.dot\"", "`quoted.dot`", "[quoted.dot]")) {
            final IllegalArgumentException varargsFailure = assertThrows(IllegalArgumentException.class, () -> Filters.using(columnName));
            final IllegalArgumentException collectionFailure = assertThrows(IllegalArgumentException.class,
                    () -> Filters.using(Arrays.asList("id", columnName)));

            for (final IllegalArgumentException failure : List.of(varargsFailure, collectionFailure)) {
                final String message = failure.getMessage();
                assertTrue(message.contains(columnName), message);
                assertTrue(message.contains("quoted identifier that contains a dot"), message);
                assertTrue(message.contains("query builder's using(...)"), message);
                assertTrue(message.contains("rendered column name"), message);
            }
        }
    }

    @Test
    void testConstructorSnapshotsCollectionBeforeRendering() {
        final AtomicInteger iteratorCount = new AtomicInteger();
        final AbstractCollection<String> liveColumns = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                final String value = iteratorCount.getAndIncrement() == 0 ? "employee_id" : "orders.employee_id";
                return Collections.singleton(value).iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        final Using using = new Using(liveColumns);

        assertEquals(Collections.singletonList("employee_id"), using.columnNames());
        assertEquals("USING (employee_id)", using.toString());
        assertEquals(1, iteratorCount.get(), "The input must be snapshotted in one validated pass");

        final AbstractCollection<String> disappearsDuringSnapshot = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> new Using(disappearsDuringSnapshot));
    }

    @Test
    void testConstructorWithCollection() {
        List<String> columns = Arrays.asList("customer_id", "order_date");
        Using using = Filters.using(columns);

        assertEquals(Operator.USING, using.operator());
        String result = using.toString();
        assertTrue(result.contains("customer_id"));
        assertTrue(result.contains("order_date"));
    }

    @Test
    void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Filters.using(emptyList);
        });
    }

    @Test
    void testConstructorWithEmptyVarArgs() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.using();
        });
    }

    @Test
    void testConstructorWithMultipleColumns() {
        Using using = Filters.using("company_id", "department_id");

        assertEquals(Operator.USING, using.operator());
        String result = using.toString();
        assertTrue(result.contains("company_id"));
        assertTrue(result.contains("department_id"));
    }

    @Test
    void testConstructorWithNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.using((List<String>) null);
        });
    }

    @Test
    void testConstructorWithNullVarArgs() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.using((String[]) null);
        });
    }

    @Test
    void testConstructorWithSet() {
        Set<String> columns = new HashSet<>();
        columns.add("tenant_id");
        columns.add("workspace_id");
        Using using = Filters.using(columns);

        assertEquals(Operator.USING, using.operator());
        assertNotNull(using.condition());
    }

    @Test
    void testConstructorWithVarArgs() {
        Using using = Filters.using("department_id");

        assertNotNull(using);
        assertEquals(Operator.USING, using.operator());
        assertNotNull(using.condition());
    }

    @Test
    void testCreateUsingCondition_Array() {
        Condition condition = Using.createUsingCondition("customer_id", "order_date");
        assertNotNull(condition);
    }

    @Test
    void testCreateUsingCondition_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("company_id", "branch_id"));
        Condition condition = Using.createUsingCondition(columns);
        assertNotNull(condition);
    }

    @Test
    void testCreateUsingCondition_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(new String[0]);
        });
    }

    @Test
    void testCreateUsingCondition_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(Arrays.asList());
        });
    }

    @Test
    void testCreateUsingCondition_ThrowsOnEmptyElementInArray() {
        assertThrows(IllegalArgumentException.class, () -> Using.createUsingCondition("id", ""));
    }

    @Test
    void testCreateUsingCondition_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((String[]) null);
        });
    }

    @Test
    void testCreateUsingCondition_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((List<String>) null);
        });
    }

    @Test
    void testCreateUsingCondition_ThrowsOnNullElementInCollection() {
        assertThrows(IllegalArgumentException.class, () -> Using.createUsingCondition(Arrays.asList("id", null)));
    }

    @Test
    void testCreateUsingCondition_ThrowsOnQualifiedColumnName() {
        assertThrows(IllegalArgumentException.class, () -> Using.createUsingCondition("orders.customer_id"));
    }

    @Test
    void testCreateUsingConditionWithCollection() {
        List<String> columns = Arrays.asList("id", "name");
        Using condition = Filters.using(columns);

        assertNotNull(condition);
    }

    @Test
    void testCreateUsingConditionWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(emptyList);
        });
    }

    @Test
    void testCreateUsingConditionWithEmptyVarArgs() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition();
        });
    }

    @Test
    void testCreateUsingConditionWithVarArgs() {
        Using condition = Filters.using("col1", "col2", "col3");

        assertNotNull(condition);
    }

    @Test
    void testDynamicColumnList() {
        List<String> sharedColumns = Arrays.asList("tenant_id", "organization_id");
        Using using = new Using(sharedColumns);
        assertNotNull(using);
    }

    @Test
    void testEquals() {
        Using using1 = Filters.using("department_id");
        Using using2 = Filters.using("department_id");
        Using using3 = Filters.using("employee_id");
        Using using4 = Filters.using("department_id", "employee_id");

        assertTrue(using1.equals(using1));
        assertTrue(using1.equals(using2));
        assertFalse(using1.equals(using3));
        assertFalse(using1.equals(using4));
        assertFalse(using1.equals(null));
        assertFalse(using1.equals("not a Using"));
    }

    @Test
    void testEquals_EqualObjects() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1, using2);
    }

    @Test
    void testEquals_Null() {
        Using using = new Using("id");
        assertNotEquals(null, using);
    }

    @Test
    void testEquals_SameObject() {
        Using using = new Using("id");
        assertEquals(using, using);
    }

    @Test
    void testGetCondition() {
        Using using = new Using("employee_id");
        Condition condition = using.condition();
        assertNotNull(condition);
    }

    @Test
    void testGetOperator() {
        Using using = Filters.using("department_id");

        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testHashCode() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1.hashCode(), using2.hashCode());
    }

    @Test
    void testOrderPreservation() {
        // Test that order is preserved with List
        List<String> orderedColumns = Arrays.asList("first", "second", "third");
        Using using = Filters.using(orderedColumns);

        String result = using.toString();
        int firstIndex = result.indexOf("first");
        int secondIndex = result.indexOf("second");
        int thirdIndex = result.indexOf("third");

        // Verify order is maintained
        assertTrue(firstIndex < secondIndex);
        assertTrue(secondIndex < thirdIndex);
    }

    @Test
    void testParameters() {
        Using using = new Using("customer_id");
        List<Object> params = using.parameters();
        assertNotNull(params);
    }

    @Test
    void testPracticalExample1() {
        // Simple single column join
        Using using = Filters.using("employee_id");

        // Would be used like: JOIN departments USING (employee_id)
        assertEquals(Operator.USING, using.operator());
    }

    @Test
    void testPracticalExample2() {
        // Multiple column join
        Using using = Filters.using("branch_id", "department_id");

        // Would be used like: JOIN other_table USING (branch_id, department_id)
        String result = using.toString();
        assertTrue(result.contains("branch_id"));
        assertTrue(result.contains("department_id"));
    }

    @Test
    void testPracticalExample3() {
        // Using with collection for dynamic column lists
        Set<String> commonColumns = new HashSet<>();
        commonColumns.add("tenant_id");
        commonColumns.add("workspace_id");
        commonColumns.add("project_id");

        Using using = Filters.using(commonColumns);

        // All columns should be present in the condition
        String result = using.toString();
        assertTrue(result.contains("tenant_id"));
        assertTrue(result.contains("workspace_id"));
        assertTrue(result.contains("project_id"));
    }

    @Test
    void testToString() {
        Using using = new Using("employee_id");
        String result = using.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("USING"));
    }

    @Test
    void testToString_SingleColumnUsesParentheses() {
        Using using = new Using("employee_id");
        String result = using.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("USING (employee_id)"));
    }

    @Test
    void testUsingRejectsCommentTokenThatSwallowsClosingParen() {
        // SqlExpression rendering strips comments; before the fix these rendered "USING (emp" / "USING (a " / "USING (id, a"
        assertThrows(IllegalArgumentException.class, () -> new Using("emp#no"));
        assertThrows(IllegalArgumentException.class, () -> new Using("a -- b"));
        assertThrows(IllegalArgumentException.class, () -> new Using(Arrays.asList("id", "a/*b")));

        // quoted identifiers keep their characters and still render a closed column list
        assertEquals("USING (\"a--b\")", new Using("\"a--b\"").toString());
        assertEquals("USING (`x#y`)", new Using("`x#y`").toString());
    }
}
