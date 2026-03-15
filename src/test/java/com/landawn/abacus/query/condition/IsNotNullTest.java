package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class IsNotNull2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNotNull condition = new IsNotNull("email");
        assertEquals("email", condition.getPropName());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNull(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNull(""));
    }

    @Test
    public void testGetPropName() {
        IsNotNull condition = new IsNotNull("customer_name");
        assertEquals("customer_name", condition.getPropName());
    }

    @Test
    public void testToString_NoChange() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("phone_number"));
    }

    @Test
    public void testEquals() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    public void testHashCode() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testUseCaseScenario_RequiredEmail() {
        IsNotNull emailCheck = new IsNotNull("email");
        assertTrue(emailCheck.toString(NamingPolicy.NO_CHANGE).contains("IS NOT NULL"));
    }

    @Test
    public void testUseCaseScenario_ValidatedFields() {
        IsNotNull nameCheck = new IsNotNull("customer_name");
        assertEquals("customer_name", nameCheck.getPropName());
    }

    @Test
    public void testGetOperator() {
        IsNotNull condition = new IsNotNull("field");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testGetPropValue() {
        IsNotNull condition = new IsNotNull("field");
        Expression value = condition.getPropValue();
        assertNotNull(value);
        assertEquals(IsNull.NULL, value);
    }

    @Test
    public void testGetParameters() {
        IsNotNull condition = new IsNotNull("field");
        assertTrue(condition.getParameters().isEmpty() || condition.getParameters().size() == 1);
    }

    @Test
    public void testClearParameters() {
        IsNotNull condition = new IsNotNull("field");
        condition.clearParameters();
        assertNotNull(condition);
    }

    @Test
    public void testToString_NoArgs() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("email"));
    }

    @Test
    public void testAnd() {
        IsNotNull cond1 = new IsNotNull("email");
        IsNotNull cond2 = new IsNotNull("phone");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNotNull cond1 = new IsNotNull("email");
        IsNotNull cond2 = new IsNotNull("phone");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNotNull condition = new IsNotNull("email");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNotNull condition = new IsNotNull("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testEquals_Null() {
        IsNotNull condition = new IsNotNull("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_SameObject() {
        IsNotNull condition = new IsNotNull("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testHashCode_DifferentPropName() {
        IsNotNull c1 = new IsNotNull("field1");
        IsNotNull c2 = new IsNotNull("field2");
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_DifferentOperatorIsNull() {
        IsNotNull isNotNull = new IsNotNull("field");
        IsNull isNull = new IsNull("field");
        assertNotEquals(isNotNull, isNull);
    }
}

public class IsNotNullTest extends TestBase {

    @Test
    public void testConstructorWithPropName() {
        IsNotNull condition = new IsNotNull("email");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
        Assertions.assertEquals(IsNull.NULL, condition.getPropValue());
    }

    @Test
    public void testConstructorWithDifferentPropNames() {
        String[] propNames = { "customer_name", "phone_number", "address", "description" };

        for (String propName : propNames) {
            IsNotNull condition = new IsNotNull(propName);

            Assertions.assertEquals(propName, condition.getPropName());
            Assertions.assertEquals(Operator.IS_NOT, condition.operator());
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
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("FIRST_NAME"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToStringWithCamelCase() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toString(NamingPolicy.CAMEL_CASE);

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
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
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
