package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class IsNotNullTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNotNull condition = new IsNotNull("email");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "customer_name", "phone_number", "address", "description" };

        for (String propName : propNames) {
            IsNotNull condition = new IsNotNull(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
            Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
        }
    }

    @Test
    public void testGetParameters() {
        IsNotNull condition = new IsNotNull("status");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        IsNotNull condition = new IsNotNull("field");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
    }

    @Test
    public void testCopy() {
        IsNotNull original = new IsNotNull("username");
        IsNotNull copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("email"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNotNull condition = new IsNotNull("firstName");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("FIRST_NAME"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToStringWithCamelCase() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toString(NamingPolicy.LOWER_CAMEL_CASE);

        Assertions.assertTrue(result.contains("phoneNumber"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testHashCode() {
        IsNotNull condition1 = new IsNotNull("field1");
        IsNotNull condition2 = new IsNotNull("field1");
        IsNotNull condition3 = new IsNotNull("field2");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNotNull condition1 = new IsNotNull("field1");
        IsNotNull condition2 = new IsNotNull("field1");
        IsNotNull condition3 = new IsNotNull("field2");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testInheritedMethods() {
        IsNotNull condition = new IsNotNull("value");

        // Test methods inherited from IsNot
        Assertions.assertEquals("value", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.getOperator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testSharedNULLConstant() {
        IsNotNull condition1 = new IsNotNull("field1");
        IsNotNull condition2 = new IsNotNull("field2");

        // Both should use the same NULL constant from IsNull
        Assertions.assertSame(condition1.getPropValue(), condition2.getPropValue());
        Assertions.assertEquals(IsNull.NULL, condition1.getPropValue());
        Assertions.assertEquals(IsNull.NULL, condition2.getPropValue());
    }

    @Test
    public void testCommonUseCases() {
        // Test common field names that are often checked for NOT NULL
        String[] commonFields = { "email", "customer_name", "phone_number", "user_id", "created_date", "status", "password", "address" };

        for (String field : commonFields) {
            IsNotNull condition = new IsNotNull(field);
            String result = condition.toString();

            Assertions.assertTrue(result.contains(field));
            Assertions.assertTrue(result.contains("IS NOT NULL"));
        }
    }
}