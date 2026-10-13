package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class LessThanTest extends TestBase {

    @Test
    void testAnd() {
        LessThan cond1 = new LessThan("a", 1);
        LessThan cond2 = new LessThan("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor() {
        LessThan condition = new LessThan("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.LESS_THAN, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThan("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThan(null, 25));
    }

    @Test
    void testConstructorWithDate() {
        Date date = new Date();
        LessThan condition = new LessThan("created_date", date);

        assertNotNull(condition);
        assertEquals("created_date", condition.propName());
        assertEquals(date, condition.propValue());
    }

    @Test
    void testConstructorWithDouble() {
        LessThan condition = new LessThan("price", 99.99);

        assertNotNull(condition);
        assertEquals("price", condition.propName());
        assertEquals(99.99, condition.propValue());
    }

    @Test
    void testConstructorWithNumber() {
        LessThan condition = new LessThan("age", 18);

        assertNotNull(condition);
        assertEquals("age", condition.propName());
        assertEquals(Operator.LESS_THAN, condition.operator());
        assertEquals(18, (Integer) condition.propValue());
    }

    @Test
    void testConstructorWithString() {
        LessThan condition = new LessThan("name", "Z");

        assertNotNull(condition);
        assertEquals("name", condition.propName());
        assertEquals("Z", condition.propValue());
    }

    @Test
    void testEquals() {
        LessThan condition1 = new LessThan("age", 30);
        LessThan condition2 = new LessThan("age", 30);
        LessThan condition3 = new LessThan("age", 40);
        LessThan condition4 = new LessThan("weight", 30);

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, condition4);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        LessThan condition = new LessThan("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        LessThan lessThan = new LessThan("field", 30);
        GreaterThan greaterThan = new GreaterThan("field", 30);
        assertNotEquals(lessThan, greaterThan);
    }

    @Test
    void testEquals_DifferentPropName() {
        LessThan cond1 = new LessThan("field1", "value");
        LessThan cond2 = new LessThan("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        LessThan cond1 = new LessThan("field", "value1");
        LessThan cond2 = new LessThan("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        LessThan cond1 = new LessThan("status", "active");
        LessThan cond2 = new LessThan("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        LessThan condition = new LessThan("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        LessThan condition = new LessThan("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        LessThan condition = new LessThan("field", "value");
        assertEquals(Operator.LESS_THAN, condition.operator());
    }

    @Test
    void testGetPropName() {
        LessThan condition = new LessThan("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testHashCode() {
        LessThan cond1 = new LessThan("age", 25);
        LessThan cond2 = new LessThan("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        LessThan cond1 = new LessThan("age", 25);
        LessThan cond2 = new LessThan("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        LessThan condition = new LessThan("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        LessThan cond1 = new LessThan("a", 1);
        LessThan cond2 = new LessThan("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        LessThan condition = new LessThan("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        LessThan condition = new LessThan("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        LessThan condition = new LessThan("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        LessThan condition = new LessThan("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        LessThan condition = new LessThan("amount", 1000);
        String result = condition.toString();

        assertTrue(result.contains("amount"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("1000"));
    }

    @Test
    void testToString_NoArgs() {
        LessThan condition = new LessThan("temperature", 100);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("100"));
    }

    @Test
    void testToString_NoChange() {
        LessThan condition = new LessThan("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        LessThan condition = new LessThan("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        LessThan condition = new LessThan("totalAmount", 5000);
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("TOTAL_AMOUNT"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("5000"));
    }

    @Test
    void testWithNullValue() {
        // `field < null` is no longer renderable: the constructor rejects the null RHS with a pointed message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LessThan("field", null));
        assertEquals("propValue must not be null for <; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }
}
