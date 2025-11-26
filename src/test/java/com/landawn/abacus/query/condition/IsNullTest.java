package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNullTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNull condition = new IsNull("email");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.getOperator());
        Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "phone_number", "birth_date", "description", "assigned_to" };

        for (String propName : propNames) {
            IsNull condition = new IsNull(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS, condition.getOperator());
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
    public void testCopy() {
        IsNull original = new IsNull("processed_date");
        IsNull copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
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
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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
        Assertions.assertEquals(Operator.IS, condition.getOperator());
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