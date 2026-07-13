package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class LessThanOrEqualTest extends TestBase {
    @Test
    public void testConstructor() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual("", 25));
    }

    @Test
    public void testGetPropName() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    public void testGetPropValue() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 30);
        Integer value = condition.propValue(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        LessThanOrEqual condition = new LessThanOrEqual("name", "Alice");
        String value = condition.propValue(String.class);
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        LessThanOrEqual condition = new LessThanOrEqual("field", null);
        assertNull(condition.propValue());
    }

    @Test
    public void testGetOperator() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    public void testParameters() {
        LessThanOrEqual condition = new LessThanOrEqual("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testParameters_MultipleValues() {
        LessThanOrEqual condition = new LessThanOrEqual("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        LessThanOrEqual cond1 = new LessThanOrEqual("status", "active");
        LessThanOrEqual cond2 = new LessThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field1", "value");
        LessThanOrEqual cond2 = new LessThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field", "value1");
        LessThanOrEqual cond2 = new LessThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testOr() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    public void testNot() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        LessThanOrEqual condition = new LessThanOrEqual("quantity", 100);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 40);
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 40);
        assertNotEquals(lessThanOrEqual, greaterThanOrEqual);
    }

    @Test
    public void testConstructorWithNumber() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 18);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("age", condition.propName());
        Assertions.assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
        Assertions.assertEquals(18, (Integer) condition.propValue());
    }

    @Test
    public void testConstructorWithDouble() {
        LessThanOrEqual condition = new LessThanOrEqual("price", 99.99);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("price", condition.propName());
        Assertions.assertEquals(99.99, condition.propValue());
    }

    @Test
    public void testConstructorWithString() {
        LessThanOrEqual condition = new LessThanOrEqual("name", "Z");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("name", condition.propName());
        Assertions.assertEquals("Z", condition.propValue());
    }

    @Test
    public void testConstructorWithDate() {
        Date date = new Date();
        LessThanOrEqual condition = new LessThanOrEqual("submit_date", date);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("submit_date", condition.propName());
        Assertions.assertEquals(date, condition.propValue());
    }

    @Test
    public void testConstructorWithLocalDate() {
        LocalDate date = LocalDate.now();
        LessThanOrEqual condition = new LessThanOrEqual("order_date", date);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("order_date", condition.propName());
        Assertions.assertEquals(date, condition.propValue());
    }

    @Test
    public void testToString() {
        LessThanOrEqual condition = new LessThanOrEqual("amount", 1000);
        String result = condition.toString();

        Assertions.assertTrue(result.contains("amount"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("1000"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        LessThanOrEqual condition = new LessThanOrEqual("totalAmount", 5000);
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("TOTAL_AMOUNT"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("5000"));
    }

    @Test
    public void testEquals() {
        LessThanOrEqual condition1 = new LessThanOrEqual("age", 30);
        LessThanOrEqual condition2 = new LessThanOrEqual("age", 30);
        LessThanOrEqual condition3 = new LessThanOrEqual("age", 40);
        LessThanOrEqual condition4 = new LessThanOrEqual("weight", 30);

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testWithNullValue() {
        LessThanOrEqual condition = new LessThanOrEqual("field", null);

        Assertions.assertNotNull(condition);
        Assertions.assertNull(condition.propValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("field"));
        Assertions.assertTrue(result.contains("<="));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testUpperBoundScenarios() {
        // Test common upper bound scenarios
        LessThanOrEqual maxStock = new LessThanOrEqual("quantity", 100);
        LessThanOrEqual maxPrice = new LessThanOrEqual("price", 999.99);
        LessThanOrEqual deadline = new LessThanOrEqual("submit_date", "2023-12-31");

        Assertions.assertEquals(100, (Integer) maxStock.propValue());
        Assertions.assertEquals(999.99, maxPrice.propValue());
        Assertions.assertEquals("2023-12-31", deadline.propValue());
    }

    @Test
    public void testRangeQueryScenario() {
        // Common scenario: using LessThanOrEqual as part of a range query
        LessThanOrEqual upperBound = new LessThanOrEqual("salary", 100000);

        Assertions.assertEquals("salary", upperBound.propName());
        Assertions.assertEquals(100000, (Integer) upperBound.propValue());
        Assertions.assertEquals(Operator.LESS_THAN_OR_EQUAL, upperBound.operator());

        String result = upperBound.toString();
        Assertions.assertTrue(result.contains("salary <= 100000"));
    }
}
