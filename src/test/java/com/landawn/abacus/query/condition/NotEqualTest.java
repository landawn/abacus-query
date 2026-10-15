package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotEqualTest extends TestBase {

    @Test
    void testAnd() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testChainedConditions() {
        // Can be used in AND/OR chains
        NotEqual ne1 = Filters.ne("status", "deleted");
        NotEqual ne2 = Filters.ne("status", "archived");

        // Both conditions are independent and valid
        assertNotNull(ne1);
        assertNotNull(ne2);
        assertNotEquals(ne1, ne2);
    }

    @Test
    void testConstructor() {
        NotEqual condition = new NotEqual("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual(null, 25));
    }

    @Test
    void testConstructorWithDateString() {
        NotEqual notEqual = Filters.ne("created", "2024-01-01");

        assertEquals("created", notEqual.propName());
        assertEquals("2024-01-01", notEqual.propValue());
    }

    @Test
    void testConstructorWithNullValue() {
        NotEqual notEqual = Filters.ne("assignee", null);

        assertEquals("assignee", notEqual.propName());
        assertNull(notEqual.propValue());
    }

    @Test
    void testConstructorWithNumericValue() {
        NotEqual notEqual = Filters.ne("quantity", 0);

        assertEquals("quantity", notEqual.propName());
        assertEquals(0, (Integer) notEqual.propValue());
    }

    @Test
    void testEquals() {
        NotEqual notEqual1 = Filters.ne("status", "deleted");
        NotEqual notEqual2 = Filters.ne("status", "deleted");
        NotEqual notEqual3 = Filters.ne("status", "active");
        NotEqual notEqual4 = Filters.ne("type", "deleted");

        assertTrue(notEqual1.equals(notEqual1));
        assertTrue(notEqual1.equals(notEqual2));
        assertFalse(notEqual1.equals(notEqual3));
        assertFalse(notEqual1.equals(notEqual4));
        assertFalse(notEqual1.equals(null));
        assertFalse(notEqual1.equals("not a NotEqual"));
    }

    @Test
    void testEquals_DifferentClass() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        NotEqual notEqual = new NotEqual("field", "value");
        Equal equal = new Equal("field", "value");
        assertNotEquals(notEqual, equal);
    }

    @Test
    void testEquals_DifferentPropName() {
        NotEqual cond1 = new NotEqual("field1", "value");
        NotEqual cond2 = new NotEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        NotEqual cond1 = new NotEqual("field", "value1");
        NotEqual cond2 = new NotEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        NotEqual cond1 = new NotEqual("status", "active");
        NotEqual cond2 = new NotEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    void testGetPropName() {
        NotEqual condition = new NotEqual("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testGetPropValue_Null() {
        NotEqual condition = new NotEqual("field", null);
        assertNull(condition.propValue());
    }

    @Test
    void testHashCode() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        NotEqual condition = new NotEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        NotEqual condition = new NotEqual("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        NotEqual condition = new NotEqual("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testParametersWithNull() {
        NotEqual notEqual = Filters.ne("value", null);

        assertTrue(notEqual.parameters().isEmpty());
    }

    @Test
    void testPracticalExamples() {
        // Exclude specific user
        NotEqual notAdmin = Filters.ne("username", "admin");
        assertEquals("username", notAdmin.propName());
        assertEquals("admin", notAdmin.propValue());

        // Exclude default values
        NotEqual notDefault = Filters.ne("configuration", "default");
        assertEquals("configuration", notDefault.propName());
        assertEquals("default", notDefault.propValue());

        // Filter out zero values
        NotEqual notZero = Filters.ne("balance", 0);
        assertEquals("balance", notZero.propName());
        assertEquals(0, (Integer) notZero.propValue());
    }

    @Test
    void testPropValueAs() {
        NotEqual condition = new NotEqual("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        NotEqual condition = new NotEqual("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        NotEqual notEqual = Filters.ne("status", "inactive");

        String result = notEqual.toString();
        assertTrue(result.contains("status"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("inactive"));
    }

    @Test
    void testToString_NoArgs() {
        NotEqual condition = new NotEqual("name", "value");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    void testToString_NoChange() {
        NotEqual condition = new NotEqual("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        NotEqual condition = new NotEqual("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        NotEqual notEqual = Filters.ne("user_status", "banned");

        String result = notEqual.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("USER_STATUS"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("banned"));
    }

    @Test
    void testToStringWithNull() {
        NotEqual notEqual = Filters.ne("status", null);

        String result = notEqual.toString();
        assertEquals("status IS NOT NULL", result);
    }

    @Test
    void testWithComplexObjects() {
        // Test with complex object (though typically you'd use primitive values)
        Object complexObject = new Object() {
            @Override
            public String toString() {
                return "ComplexObject";
            }
        };

        NotEqual notEqual = Filters.ne("data", complexObject);
        assertEquals(complexObject, notEqual.propValue());
    }

    @Test
    void testWithDifferentDataTypes() {
        // String
        NotEqual stringNe = Filters.ne("name", "test");
        assertEquals("test", stringNe.propValue());

        // Integer
        NotEqual intNe = Filters.ne("count", 42);
        assertEquals(42, (Integer) intNe.propValue());

        // Double
        NotEqual doubleNe = Filters.ne("price", 99.99);
        assertEquals(99.99, doubleNe.propValue());

        // Boolean
        NotEqual boolNe = Filters.ne("active", true);
        assertEquals(true, boolNe.propValue());

        // Date
        Date createdAt = new Date(1_700_000_000_000L);
        NotEqual dateNe = Filters.ne("created", createdAt);
        assertEquals(createdAt, dateNe.propValue());
    }
}
