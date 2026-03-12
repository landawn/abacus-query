package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class NotEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        NotEqual condition = new NotEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        NotEqual condition = new NotEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        NotEqual condition = new NotEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        NotEqual condition = new NotEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        NotEqual condition = new NotEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    public void testGetParameters() {
        NotEqual condition = new NotEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        NotEqual condition = new NotEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        NotEqual condition = new NotEqual("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        NotEqual condition = new NotEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotEqual condition = new NotEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        NotEqual cond1 = new NotEqual("age", 25);
        NotEqual cond2 = new NotEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotEqual condition = new NotEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotEqual cond1 = new NotEqual("status", "active");
        NotEqual cond2 = new NotEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotEqual cond1 = new NotEqual("field1", "value");
        NotEqual cond2 = new NotEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        NotEqual cond1 = new NotEqual("field", "value1");
        NotEqual cond2 = new NotEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        NotEqual condition = new NotEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotEqual cond1 = new NotEqual("a", 1);
        NotEqual cond2 = new NotEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotEqual condition = new NotEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        NotEqual condition = new NotEqual("name", "value");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        NotEqual notEqual = new NotEqual("field", "value");
        Equal equal = new Equal("field", "value");
        assertNotEquals(notEqual, equal);
    }
}

public class NotEqualTest extends TestBase {

    @Test
    public void testConstructor() {
        NotEqual notEqual = Filters.ne("status", "deleted");

        Assertions.assertNotNull(notEqual);
        Assertions.assertEquals("status", notEqual.getPropName());
        Assertions.assertEquals("deleted", notEqual.getPropValue());
        Assertions.assertEquals(Operator.NOT_EQUAL, notEqual.operator());
    }

    @Test
    public void testConstructorWithNumericValue() {
        NotEqual notEqual = Filters.ne("quantity", 0);

        Assertions.assertEquals("quantity", notEqual.getPropName());
        Assertions.assertEquals(0, (Integer) notEqual.getPropValue());
    }

    @Test
    public void testConstructorWithNullValue() {
        NotEqual notEqual = Filters.ne("assignee", null);

        Assertions.assertEquals("assignee", notEqual.getPropName());
        Assertions.assertNull(notEqual.getPropValue());
    }

    @Test
    public void testConstructorWithDateString() {
        NotEqual notEqual = Filters.ne("created", "2024-01-01");

        Assertions.assertEquals("created", notEqual.getPropName());
        Assertions.assertEquals("2024-01-01", notEqual.getPropValue());
    }

    @Test
    public void testGetParameters() {
        NotEqual notEqual = Filters.ne("username", "admin");

        Assertions.assertEquals(1, notEqual.getParameters().size());
        Assertions.assertEquals("admin", notEqual.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        NotEqual notEqual = Filters.ne("value", null);

        Assertions.assertNull(notEqual.getParameters().get(0));
    }

    @Test
    public void testClearParameters() {
        NotEqual notEqual = Filters.ne("type", "default");

        notEqual.clearParameters();

        Assertions.assertNull(notEqual.getPropValue());
        Assertions.assertNull(notEqual.getParameters().get(0));
    }

    @Test
    public void testToString() {
        NotEqual notEqual = Filters.ne("status", "inactive");

        String result = notEqual.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("!="));
        Assertions.assertTrue(result.contains("inactive"));
    }

    @Test
    public void testToStringWithNull() {
        NotEqual notEqual = Filters.ne("status", null);

        String result = notEqual.toString();
        Assertions.assertEquals("status IS NOT NULL", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        NotEqual notEqual = Filters.ne("user_status", "banned");

        String result = notEqual.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("USER_STATUS"));
        Assertions.assertTrue(result.contains("!="));
        Assertions.assertTrue(result.contains("banned"));
    }

    @Test
    public void testHashCode() {
        NotEqual notEqual1 = Filters.ne("status", "deleted");
        NotEqual notEqual2 = Filters.ne("status", "deleted");
        NotEqual notEqual3 = Filters.ne("status", "active");
        NotEqual notEqual4 = Filters.ne("type", "deleted");

        Assertions.assertEquals(notEqual1.hashCode(), notEqual2.hashCode());
        Assertions.assertNotEquals(notEqual1.hashCode(), notEqual3.hashCode());
        Assertions.assertNotEquals(notEqual1.hashCode(), notEqual4.hashCode());
    }

    @Test
    public void testEquals() {
        NotEqual notEqual1 = Filters.ne("status", "deleted");
        NotEqual notEqual2 = Filters.ne("status", "deleted");
        NotEqual notEqual3 = Filters.ne("status", "active");
        NotEqual notEqual4 = Filters.ne("type", "deleted");

        Assertions.assertTrue(notEqual1.equals(notEqual1));
        Assertions.assertTrue(notEqual1.equals(notEqual2));
        Assertions.assertFalse(notEqual1.equals(notEqual3));
        Assertions.assertFalse(notEqual1.equals(notEqual4));
        Assertions.assertFalse(notEqual1.equals(null));
        Assertions.assertFalse(notEqual1.equals("not a NotEqual"));
    }

    @Test
    public void testWithDifferentDataTypes() {
        // String
        NotEqual stringNe = Filters.ne("name", "test");
        Assertions.assertEquals("test", stringNe.getPropValue());

        // Integer
        NotEqual intNe = Filters.ne("count", 42);
        Assertions.assertEquals(42, (Integer) intNe.getPropValue());

        // Double
        NotEqual doubleNe = Filters.ne("price", 99.99);
        Assertions.assertEquals(99.99, doubleNe.getPropValue());

        // Boolean
        NotEqual boolNe = Filters.ne("active", true);
        Assertions.assertEquals(true, boolNe.getPropValue());

        // Date
        Date now = new Date();
        NotEqual dateNe = Filters.ne("created", now);
        Assertions.assertEquals(now, dateNe.getPropValue());
    }

    @Test
    public void testPracticalExamples() {
        // Exclude specific user
        NotEqual notAdmin = Filters.ne("username", "admin");
        Assertions.assertEquals("username", notAdmin.getPropName());
        Assertions.assertEquals("admin", notAdmin.getPropValue());

        // Exclude default values
        NotEqual notDefault = Filters.ne("configuration", "default");
        Assertions.assertEquals("configuration", notDefault.getPropName());
        Assertions.assertEquals("default", notDefault.getPropValue());

        // Filter out zero values
        NotEqual notZero = Filters.ne("balance", 0);
        Assertions.assertEquals("balance", notZero.getPropName());
        Assertions.assertEquals(0, (Integer) notZero.getPropValue());
    }

    @Test
    public void testChainedConditions() {
        // Can be used in AND/OR chains
        NotEqual ne1 = Filters.ne("status", "deleted");
        NotEqual ne2 = Filters.ne("status", "archived");

        // Both conditions are independent and valid
        Assertions.assertNotNull(ne1);
        Assertions.assertNotNull(ne2);
        Assertions.assertNotEquals(ne1, ne2);
    }

    @Test
    public void testWithComplexObjects() {
        // Test with complex object (though typically you'd use primitive values)
        Object complexObject = new Object() {
            @Override
            public String toString() {
                return "ComplexObject";
            }
        };

        NotEqual notEqual = Filters.ne("data", complexObject);
        Assertions.assertEquals(complexObject, notEqual.getPropValue());
    }
}
