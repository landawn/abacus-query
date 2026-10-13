package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNullTest extends TestBase {

    @Test
    void testAnd() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testCommonUseCases() {
        // Test common fields that are often checked for NULL
        IsNull missingEmail = new IsNull("email");
        IsNull unassignedTask = new IsNull("assigned_to");
        IsNull unprocessedRecord = new IsNull("processed_date");
        IsNull noDescription = new IsNull("description");

        String result1 = missingEmail.toString();
        String result2 = unassignedTask.toString();
        String result3 = unprocessedRecord.toString();
        String result4 = noDescription.toString();

        assertTrue(result1.contains("email IS NULL"));
        assertTrue(result2.contains("assigned_to IS NULL"));
        assertTrue(result3.contains("processed_date IS NULL"));
        assertTrue(result4.contains("description IS NULL"));
    }

    @Test
    void testConstructor() {
        IsNull condition = new IsNull("email");
        assertEquals("email", condition.propName());
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(""));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(null));
    }

    @Test
    void testConstructorWithDifferentPropNames() {
        String[] propNames = { "phone_number", "birth_date", "description", "assigned_to" };

        for (String propName : propNames) {
            IsNull condition = new IsNull(propName);

            assertEquals(propName, condition.propName());
            assertEquals(Operator.IS, condition.operator());
            assertEquals(IsNull.NULL, condition.propValue());
        }
    }

    @Test
    void testConstructorWithPropName() {
        IsNull condition = new IsNull("email");

        assertNotNull(condition);
        assertEquals("email", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertEquals(IsNull.NULL, condition.propValue());
    }

    @Test
    void testEquals() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentClass() {
        IsNull condition = new IsNull("field");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentPropName() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        IsNull condition = new IsNull("field");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsNull condition = new IsNull("field");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsNull condition = new IsNull("field");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    void testGetPropName() {
        IsNull condition = new IsNull("phone_number");
        assertEquals("phone_number", condition.propName());
    }

    @Test
    void testHashCode() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_DifferentPropName() {
        IsNull c1 = new IsNull("field1");
        IsNull c2 = new IsNull("field2");
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testInheritedMethods() {
        IsNull condition = new IsNull("value");

        // Test methods inherited from Is
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testMultipleInstances() {
        IsNull condition1 = new IsNull("field1");
        IsNull condition2 = new IsNull("field2");

        // Both should share the same NULL constant
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsNull.NULL, condition1.propValue());
        assertEquals(IsNull.NULL, condition2.propValue());
    }

    @Test
    void testNot() {
        IsNull condition = new IsNull("email");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testNULLConstant() {
        assertNotNull(IsNull.NULL);
        // Verify it's an SqlExpression
        assertTrue(IsNull.NULL.toString().contains("NULL"));
    }

    @Test
    void testOptionalFieldScenarios() {
        // Test scenarios with optional fields
        String[] optionalFields = { "middle_name", "secondary_email", "alternate_phone", "notes", "comments", "reference_number" };

        for (String field : optionalFields) {
            IsNull condition = new IsNull(field);

            assertEquals(field, condition.propName());
            assertEquals(IsNull.NULL, condition.propValue());

            String result = condition.toString();
            assertTrue(result.contains(field + " IS NULL"));
        }
    }

    @Test
    void testOr() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testOuterJoinScenario() {
        // Common scenario after LEFT JOIN
        IsNull noMatchingRecord = new IsNull("right_table.id");

        assertEquals("right_table.id", noMatchingRecord.propName());
        String result = noMatchingRecord.toString();
        assertTrue(result.contains("right_table.id IS NULL"));
    }

    @Test
    void testParameters() {
        IsNull condition = new IsNull("field");
        assertTrue(condition.parameters().isEmpty() || condition.parameters().size() == 1);
    }

    @Test
    void testPropValueAs() {
        IsNull condition = new IsNull("field");
        SqlExpression value = condition.propValueAs(SqlExpression.class);
        assertNotNull(value);
        assertEquals(IsNull.NULL, value);
    }

    @Test
    void testToString() {
        IsNull condition = new IsNull("email");
        String result = condition.toString();

        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testToString_NoArgs() {
        IsNull condition = new IsNull("email");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("email"));
    }

    @Test
    void testToString_NoChange() {
        IsNull condition = new IsNull("email");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testToString_SnakeCase() {
        IsNull condition = new IsNull("assignedTo");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("assigned_to"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsNull condition = new IsNull("birthDate");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("BIRTH_DATE"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testUseCaseScenario_MissingEmail() {
        IsNull emailCheck = new IsNull("email");
        assertTrue(emailCheck.toSql(NamingPolicy.NO_CHANGE).contains("IS NULL"));
    }

    @Test
    void testUseCaseScenario_OptionalFields() {
        IsNull middleNameCheck = new IsNull("middle_name");
        assertNotNull(middleNameCheck.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testUseCaseScenario_UnassignedTasks() {
        IsNull assigneeCheck = new IsNull("assigned_to");
        assertEquals("assigned_to", assigneeCheck.propName());
    }
}
