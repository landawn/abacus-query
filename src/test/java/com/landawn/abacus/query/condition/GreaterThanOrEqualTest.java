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
class GreaterThanOrEqual2025Test extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    public void testGetParameters() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("status", "active");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field1", "value");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field", "value1");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("score", 60);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("score"));
        assertTrue(result.contains("60"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 20);
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 20);
        assertNotEquals(greaterThanOrEqual, lessThanOrEqual);
    }
}

public class GreaterThanOrEqualTest extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThanOrEqual ge = Filters.ge("age", 18);
        Assertions.assertNotNull(ge);
        Assertions.assertEquals("age", ge.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) ge.getPropValue());
        Assertions.assertEquals(Operator.GREATER_THAN_OR_EQUAL, ge.operator());
    }

    @Test
    public void testConstructorWithNullValue() {
        GreaterThanOrEqual ge = Filters.ge("name", null);
        Assertions.assertNotNull(ge);
        Assertions.assertEquals("name", ge.getPropName());
        Assertions.assertNull(ge.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThanOrEqual("", 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThanOrEqual(null, 10);
        });
    }

    @Test
    public void testToString() {
        GreaterThanOrEqual ge = Filters.ge("salary", 50000);
        String result = ge.toString();
        Assertions.assertEquals("salary >= 50000", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GreaterThanOrEqual ge = Filters.ge("firstName", "John");
        String result = ge.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertEquals("first_name >= 'John'", result);
    }

    @Test
    public void testEquals() {
        GreaterThanOrEqual ge1 = Filters.ge("age", 25);
        GreaterThanOrEqual ge2 = Filters.ge("age", 25);
        GreaterThanOrEqual ge3 = Filters.ge("age", 30);
        GreaterThanOrEqual ge4 = Filters.ge("name", 25);

        Assertions.assertEquals(ge1, ge2);
        Assertions.assertNotEquals(ge1, ge3);
        Assertions.assertNotEquals(ge1, ge4);
        Assertions.assertNotEquals(ge1, null);
        Assertions.assertNotEquals(ge1, "string");
    }

    @Test
    public void testHashCode() {
        GreaterThanOrEqual ge1 = Filters.ge("age", 25);
        GreaterThanOrEqual ge2 = Filters.ge("age", 25);

        Assertions.assertEquals(ge1.hashCode(), ge2.hashCode());
    }

    @Test
    public void testGetParameters() {
        GreaterThanOrEqual ge = Filters.ge("price", 100.0);
        var params = ge.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(100.0, params.get(0));
    }

    @Test
    public void testAnd() {
        GreaterThanOrEqual ge = Filters.ge("age", 18);
        LessThan lt = Filters.lt("age", 65);
        And and = ge.and(lt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.operator());
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterThanOrEqual ge = Filters.ge("score", 90);
        Equal eq = Filters.eq("grade", "A");
        Or or = ge.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.operator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThanOrEqual ge = Filters.ge("temperature", 0);
        Not not = ge.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.operator());
        Assertions.assertEquals(ge, not.getCondition());
    }
}
