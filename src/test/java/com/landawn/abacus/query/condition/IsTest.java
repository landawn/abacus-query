package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
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
class Is2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Is condition = new Is("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Is(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Is("", 25));
    }

    @Test
    public void testGetPropName() {
        Is condition = new Is("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Is condition = new Is("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Is condition = new Is("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Is condition = new Is("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Is condition = new Is("field", "value");
        assertEquals(Operator.IS, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Is condition = new Is("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        Is condition = new Is("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        Is condition = new Is("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        Is condition = new Is("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        Is cond1 = new Is("age", 25);
        Is cond2 = new Is("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Is cond1 = new Is("age", 25);
        Is cond2 = new Is("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Is condition = new Is("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Is cond1 = new Is("status", "active");
        Is cond2 = new Is("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Is cond1 = new Is("field1", "value");
        Is cond2 = new Is("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Is cond1 = new Is("field", "value1");
        Is cond2 = new Is("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Is condition = new Is("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Is condition = new Is("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Is cond1 = new Is("a", 1);
        Is cond2 = new Is("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Is cond1 = new Is("a", 1);
        Is cond2 = new Is("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Is condition = new Is("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class IsTest extends TestBase {

    @Test
    public void testConstructorWithPropNameAndValue() {
        Is condition = new Is("age", null);
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
        Assertions.assertNull(condition.getPropValue());
    }

    @Test
    public void testConstructorWithExpression() {
        Is condition = new Is("status", Filters.expr("ACTIVE"));
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IS, condition.operator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        Is condition = new Is("name", "value");
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("value", params.get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        Is condition = new Is("name", null);
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testToString() {
        Is condition = new Is("status", null);
        String result = condition.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("IS"));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Is condition = new Is("firstName", null);
        String result = condition.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertTrue(result.contains("firstName"));
        Assertions.assertTrue(result.contains("IS"));
    }

    @Test
    public void testHashCode() {
        Is condition1 = new Is("name", "value");
        Is condition2 = new Is("name", "value");
        Is condition3 = new Is("other", "value");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        Is condition1 = new Is("name", "value");
        Is condition2 = new Is("name", "value");
        Is condition3 = new Is("other", "value");
        Is condition4 = new Is("name", "other");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }
}
