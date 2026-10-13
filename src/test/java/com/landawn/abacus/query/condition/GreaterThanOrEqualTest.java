package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class GreaterThanOrEqualTest extends TestBase {

    @Test
    void testAnd() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual(null, 25));
    }

    @Test
    void testConstructorWithEmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThanOrEqual("", 10);
        });
    }

    @Test
    void testConstructorWithNullPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThanOrEqual(null, 10);
        });
    }

    @Test
    void testConstructorWithNullValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Filters.ge("name", null));
        assertEquals("propValue must not be null for >=; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testEquals() {
        GreaterThanOrEqual ge1 = Filters.ge("age", 25);
        GreaterThanOrEqual ge2 = Filters.ge("age", 25);
        GreaterThanOrEqual ge3 = Filters.ge("age", 30);
        GreaterThanOrEqual ge4 = Filters.ge("name", 25);

        assertEquals(ge1, ge2);
        assertNotEquals(ge1, ge3);
        assertNotEquals(ge1, ge4);
        assertNotEquals(ge1, null);
        assertNotEquals(ge1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 20);
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 20);
        assertNotEquals(greaterThanOrEqual, lessThanOrEqual);
    }

    @Test
    void testEquals_DifferentPropName() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field1", "value");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("field", "value1");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("status", "active");
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    void testGetPropName() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testGetPropValue_Null() {
        // A null RHS is rejected at construction: `x >= NULL` can never be true under three-valued logic.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new GreaterThanOrEqual("field", null));
        assertEquals("propValue must not be null for >=; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testHashCode() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("age", 25);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        GreaterThanOrEqual cond1 = new GreaterThanOrEqual("a", 1);
        GreaterThanOrEqual cond2 = new GreaterThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        GreaterThanOrEqual ge = Filters.ge("salary", 50000);
        String result = ge.toString();
        assertEquals("salary >= 50000", result);
    }

    @Test
    void testToString_NoArgs() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("score", 60);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("score"));
        assertTrue(result.contains("60"));
    }

    @Test
    void testToString_NoChange() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        GreaterThanOrEqual condition = new GreaterThanOrEqual("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        GreaterThanOrEqual ge = Filters.ge("firstName", "John");
        String result = ge.toSql(NamingPolicy.SNAKE_CASE);
        assertEquals("first_name >= 'John'", result);
    }
}
