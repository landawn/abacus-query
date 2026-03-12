package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class IsNot2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNot condition = new IsNot("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNot(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNot("", 25));
    }

    @Test
    public void testGetPropName() {
        IsNot condition = new IsNot("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        IsNot condition = new IsNot("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        IsNot condition = new IsNot("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        IsNot condition = new IsNot("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        IsNot condition = new IsNot("field", "value");
        assertEquals(Operator.IS_NOT, condition.operator());
    }

    @Test
    public void testGetParameters() {
        IsNot condition = new IsNot("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        IsNot condition = new IsNot("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        IsNot condition = new IsNot("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        IsNot condition = new IsNot("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNot condition = new IsNot("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        IsNot cond1 = new IsNot("age", 25);
        IsNot cond2 = new IsNot("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        IsNot cond1 = new IsNot("age", 25);
        IsNot cond2 = new IsNot("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsNot condition = new IsNot("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsNot cond1 = new IsNot("status", "active");
        IsNot cond2 = new IsNot("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNot cond1 = new IsNot("field1", "value");
        IsNot cond2 = new IsNot("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        IsNot cond1 = new IsNot("field", "value1");
        IsNot cond2 = new IsNot("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsNot condition = new IsNot("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNot condition = new IsNot("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsNot cond1 = new IsNot("a", 1);
        IsNot cond2 = new IsNot("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNot cond1 = new IsNot("a", 1);
        IsNot cond2 = new IsNot("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNot condition = new IsNot("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class IsNotTest extends TestBase {

    @Test
    public void testConstructorWithPropNameAndValue() {
        IsNot condition = new IsNot("age", null);
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
        Assertions.assertNull(condition.getPropValue());
    }

    @Test
    public void testConstructorWithExpression() {
        IsNot condition = new IsNot("status", Filters.expr("ACTIVE"));
        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IS_NOT, condition.operator());
        Assertions.assertNotNull(condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        IsNot condition = new IsNot("name", "value");
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("value", params.get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        IsNot condition = new IsNot("name", null);
        List<Object> params = condition.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testClearParameters() {
        IsNot condition = new IsNot("name", "value");
        condition.clearParameters();
        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testToString() {
        IsNot condition = new IsNot("status", null);
        String result = condition.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("IS NOT"));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        IsNot condition = new IsNot("firstName", null);
        String result = condition.toString(NamingPolicy.CAMEL_CASE);
        Assertions.assertTrue(result.contains("firstName"));
        Assertions.assertTrue(result.contains("IS NOT"));
    }

    @Test
    public void testHashCode() {
        IsNot condition1 = new IsNot("name", "value");
        IsNot condition2 = new IsNot("name", "value");
        IsNot condition3 = new IsNot("other", "value");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
    }

    @Test
    public void testEquals() {
        IsNot condition1 = new IsNot("name", "value");
        IsNot condition2 = new IsNot("name", "value");
        IsNot condition3 = new IsNot("other", "value");
        IsNot condition4 = new IsNot("name", "other");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }
}
