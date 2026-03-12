package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class IsNull2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNull condition = new IsNull("email");
        assertEquals("email", condition.getPropName());
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(""));
    }

    @Test
    public void testGetPropName() {
        IsNull condition = new IsNull("phone_number");
        assertEquals("phone_number", condition.getPropName());
    }

    @Test
    public void testToString_NoChange() {
        IsNull condition = new IsNull("email");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNull condition = new IsNull("assignedTo");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("assigned_to"));
    }

    @Test
    public void testEquals() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    public void testHashCode() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testUseCaseScenario_MissingEmail() {
        IsNull emailCheck = new IsNull("email");
        assertTrue(emailCheck.toString(NamingPolicy.NO_CHANGE).contains("IS NULL"));
    }

    @Test
    public void testUseCaseScenario_UnassignedTasks() {
        IsNull assigneeCheck = new IsNull("assigned_to");
        assertEquals("assigned_to", assigneeCheck.getPropName());
    }

    @Test
    public void testUseCaseScenario_OptionalFields() {
        IsNull middleNameCheck = new IsNull("middle_name");
        assertNotNull(middleNameCheck.toString(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testGetOperator() {
        IsNull condition = new IsNull("field");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testGetPropValue() {
        IsNull condition = new IsNull("field");
        Expression value = condition.getPropValue();
        assertNotNull(value);
        assertEquals(IsNull.NULL, value);
    }

    @Test
    public void testGetParameters() {
        IsNull condition = new IsNull("field");
        assertTrue(condition.getParameters().isEmpty() || condition.getParameters().size() == 1);
    }

    @Test
    public void testClearParameters() {
        IsNull condition = new IsNull("field");
        condition.clearParameters();
        assertNotNull(condition);
    }

    @Test
    public void testToString_NoArgs() {
        IsNull condition = new IsNull("email");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("email"));
    }

    @Test
    public void testAnd() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNull condition = new IsNull("email");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNull condition = new IsNull("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testEquals_Null() {
        IsNull condition = new IsNull("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_SameObject() {
        IsNull condition = new IsNull("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testHashCode_DifferentPropName() {
        IsNull c1 = new IsNull("field1");
        IsNull c2 = new IsNull("field2");
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }
}

public class IsNullTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNull condition = new IsNull("email");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
        Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "phone_number", "birth_date", "description", "assigned_to" };

        for (String propName : propNames) {
            IsNull condition = new IsNull(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS, condition.operator());
            Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
        }
    }

    @Test
    public void testGetParameters() {
        IsNull condition = new IsNull("status");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsNull condition = new IsNull("field");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testToString() {
        IsNull condition = new IsNull("email");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("email"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNull condition = new IsNull("birthDate");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("BIRTH_DATE"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testHashCode() {
        IsNull condition1 = new IsNull("field1");
        IsNull condition2 = new IsNull("field1");
        IsNull condition3 = new IsNull("field2");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNull condition1 = new IsNull("field1");
        IsNull condition2 = new IsNull("field1");
        IsNull condition3 = new IsNull("field2");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testNULLConstant() {
        Assertions.assertNotNull(IsNull.NULL);
        // Verify it's an Expression
        Assertions.assertTrue(IsNull.NULL.toString().contains("NULL"));
    }

    @Test
    public void testInheritedMethods() {
        IsNull condition = new IsNull("value");

        // Test methods inherited from Is
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testMultipleInstances() {
        IsNull condition1 = new IsNull("field1");
        IsNull condition2 = new IsNull("field2");

        // Both should share the same NULL constant
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsNull.NULL, condition1.getPropValue());
        Assertions.assertEquals(IsNull.NULL, condition2.getPropValue());
    }

    @Test
    public void testCommonUseCases() {
        // Test common fields that are often checked for NULL
        IsNull missingEmail = new IsNull("email");
        IsNull unassignedTask = new IsNull("assigned_to");
        IsNull unprocessedRecord = new IsNull("processed_date");
        IsNull noDescription = new IsNull("description");

        String result1 = missingEmail.toString();
        String result2 = unassignedTask.toString();
        String result3 = unprocessedRecord.toString();
        String result4 = noDescription.toString();

        Assertions.assertTrue(result1.contains("email IS NULL"));
        Assertions.assertTrue(result2.contains("assigned_to IS NULL"));
        Assertions.assertTrue(result3.contains("processed_date IS NULL"));
        Assertions.assertTrue(result4.contains("description IS NULL"));
    }

    @Test
    public void testOptionalFieldScenarios() {
        // Test scenarios with optional fields
        String[] optionalFields = { "middle_name", "secondary_email", "alternate_phone", "notes", "comments", "reference_number" };

        for (String field : optionalFields) {
            IsNull condition = new IsNull(field);

            Assertions.assertEquals(field, condition.getPropName());
            Assertions.assertEquals(IsNull.NULL, condition.getPropValue());

            String result = condition.toString();
            Assertions.assertTrue(result.contains(field + " IS NULL"));
        }
    }

    @Test
    public void testOuterJoinScenario() {
        // Common scenario after LEFT JOIN
        IsNull noMatchingRecord = new IsNull("right_table.id");

        Assertions.assertEquals("right_table.id", noMatchingRecord.getPropName());
        String result = noMatchingRecord.toString();
        Assertions.assertTrue(result.contains("right_table.id IS NULL"));
    }
}
