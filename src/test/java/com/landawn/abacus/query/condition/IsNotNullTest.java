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

public class IsNotNullTest extends TestBase {

    @Test
    void testAnd() {
        IsNotNull cond1 = new IsNotNull("email");
        IsNotNull cond2 = new IsNotNull("phone");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testCommonUseCases() {
        // Test common field names that are often checked for NOT NULL
        String[] commonFields = { "email", "customer_name", "phone_number", "user_id", "created_date", "status", "password", "address" };

        for (String field : commonFields) {
            IsNotNull condition = new IsNotNull(field);
            String result = condition.toString();

            assertTrue(result.contains(field));
            assertTrue(result.contains("IS NOT NULL"));
        }
    }

    @Test
    void testConstructor() {
        IsNotNull condition = new IsNotNull("email");
        assertEquals("email", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNull(""));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNull(null));
    }

    @Test
    void testConstructorWithDifferentPropNames() {
        String[] propNames = { "customer_name", "phone_number", "address", "description" };

        for (String propName : propNames) {
            IsNotNull condition = new IsNotNull(propName);

            assertEquals(propName, condition.propName());
            assertEquals(Operator.IS_NOT, condition.operator());
            assertEquals(IsNull.NULL, condition.propValue());
        }
    }

    @Test
    void testConstructorWithPropName() {
        IsNotNull condition = new IsNotNull("email");

        assertNotNull(condition);
        assertEquals("email", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertEquals(IsNull.NULL, condition.propValue());
    }

    @Test
    void testEquals() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentClass() {
        IsNotNull condition = new IsNotNull("field");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperatorIsNull() {
        IsNotNull isNotNull = new IsNotNull("field");
        IsNull isNull = new IsNull("field");
        assertNotEquals(isNotNull, isNull);
    }

    @Test
    void testEquals_DifferentPropName() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        IsNotNull condition = new IsNotNull("field");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        IsNotNull condition = new IsNotNull("field");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        IsNotNull condition = new IsNotNull("field");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    void testGetPropName() {
        IsNotNull condition = new IsNotNull("customer_name");
        assertEquals("customer_name", condition.propName());
    }

    @Test
    void testHashCode() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_DifferentPropName() {
        IsNotNull c1 = new IsNotNull("field1");
        IsNotNull c2 = new IsNotNull("field2");
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testInheritedMethods() {
        IsNotNull condition = new IsNotNull("value");

        // Test methods inherited from IsNot
        assertEquals("value", condition.propName());
        assertEquals(Operator.IS_NOT, condition.operator());
        assertNotNull(condition.propValue());
    }

    @Test
    void testNot() {
        IsNotNull condition = new IsNotNull("email");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        IsNotNull cond1 = new IsNotNull("email");
        IsNotNull cond2 = new IsNotNull("phone");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        IsNotNull condition = new IsNotNull("field");
        assertTrue(condition.parameters().isEmpty() || condition.parameters().size() == 1);
    }

    @Test
    void testPropValueAs() {
        IsNotNull condition = new IsNotNull("field");
        SqlExpression value = condition.propValueAs(SqlExpression.class);
        assertNotNull(value);
        assertEquals(IsNull.NULL, value);
    }

    @Test
    void testSharedNULLConstant() {
        IsNotNull condition1 = new IsNotNull("field1");
        IsNotNull condition2 = new IsNotNull("field2");

        // Both should use the same NULL constant from IsNull
        assertSame(condition1.propValue(), condition2.propValue());
        assertEquals(IsNull.NULL, condition1.propValue());
        assertEquals(IsNull.NULL, condition2.propValue());
    }

    @Test
    void testToString() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString();

        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testToString_NoArgs() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("email"));
    }

    @Test
    void testToString_NoChange() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testToString_SnakeCase() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("phone_number"));
    }

    @Test
    void testToStringWithCamelCase() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toSql(NamingPolicy.CAMEL_CASE);

        assertTrue(result.contains("phoneNumber"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        IsNotNull condition = new IsNotNull("firstName");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("FIRST_NAME"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    void testUseCaseScenario_RequiredEmail() {
        IsNotNull emailCheck = new IsNotNull("email");
        assertTrue(emailCheck.toSql(NamingPolicy.NO_CHANGE).contains("IS NOT NULL"));
    }

}
