package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class LessThanOrEqualTest extends TestBase {

    @Test
    void testAnd() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual(null, 25));
    }

    @Test
    void testConstructorWithDate() {
        Date date = new Date(1_700_000_000_000L);
        LessThanOrEqual condition = new LessThanOrEqual("submit_date", date);

        assertNotNull(condition);
        assertEquals("submit_date", condition.propName());
        assertEquals(date, condition.propValue());
    }

    @Test
    void testConstructorWithDouble() {
        LessThanOrEqual condition = new LessThanOrEqual("price", 99.99);

        assertNotNull(condition);
        assertEquals("price", condition.propName());
        assertEquals(99.99, condition.propValue());
    }

    @Test
    void testConstructorWithLocalDate() {
        LocalDate date = LocalDate.now();
        LessThanOrEqual condition = new LessThanOrEqual("order_date", date);

        assertNotNull(condition);
        assertEquals("order_date", condition.propName());
        assertEquals(date, condition.propValue());
    }

    @Test
    void testConstructorWithNumber() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 18);

        assertNotNull(condition);
        assertEquals("age", condition.propName());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
        assertEquals(18, (Integer) condition.propValue());
    }

    @Test
    void testConstructorWithString() {
        LessThanOrEqual condition = new LessThanOrEqual("name", "Z");

        assertNotNull(condition);
        assertEquals("name", condition.propName());
        assertEquals("Z", condition.propValue());
    }

    @Test
    void testEquals() {
        LessThanOrEqual condition1 = new LessThanOrEqual("age", 30);
        LessThanOrEqual condition2 = new LessThanOrEqual("age", 30);
        LessThanOrEqual condition3 = new LessThanOrEqual("age", 40);
        LessThanOrEqual condition4 = new LessThanOrEqual("weight", 30);

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, condition4);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        LessThanOrEqual lessThanOrEqual = new LessThanOrEqual("field", 40);
        GreaterThanOrEqual greaterThanOrEqual = new GreaterThanOrEqual("field", 40);
        assertNotEquals(lessThanOrEqual, greaterThanOrEqual);
    }

    @Test
    void testEquals_DifferentPropName() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field1", "value");
        LessThanOrEqual cond2 = new LessThanOrEqual("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        LessThanOrEqual cond1 = new LessThanOrEqual("field", "value1");
        LessThanOrEqual cond2 = new LessThanOrEqual("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        LessThanOrEqual cond1 = new LessThanOrEqual("status", "active");
        LessThanOrEqual cond2 = new LessThanOrEqual("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        assertEquals(Operator.LESS_THAN_OR_EQUAL, condition.operator());
    }

    @Test
    void testGetPropName() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testHashCode() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        LessThanOrEqual cond1 = new LessThanOrEqual("age", 25);
        LessThanOrEqual cond2 = new LessThanOrEqual("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        LessThanOrEqual condition = new LessThanOrEqual("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        LessThanOrEqual cond1 = new LessThanOrEqual("a", 1);
        LessThanOrEqual cond2 = new LessThanOrEqual("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        LessThanOrEqual condition = new LessThanOrEqual("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        LessThanOrEqual condition = new LessThanOrEqual("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        LessThanOrEqual condition = new LessThanOrEqual("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        LessThanOrEqual condition = new LessThanOrEqual("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testRangeQueryScenario() {
        // Common scenario: using LessThanOrEqual as part of a range query
        LessThanOrEqual upperBound = new LessThanOrEqual("salary", 100000);

        assertEquals("salary", upperBound.propName());
        assertEquals(100000, (Integer) upperBound.propValue());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, upperBound.operator());

        String result = upperBound.toString();
        assertTrue(result.contains("salary <= 100000"));
    }

    @Test
    void testToString() {
        LessThanOrEqual condition = new LessThanOrEqual("amount", 1000);
        String result = condition.toString();

        assertTrue(result.contains("amount"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("1000"));
    }

    @Test
    void testToString_NoArgs() {
        LessThanOrEqual condition = new LessThanOrEqual("quantity", 100);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("100"));
    }

    @Test
    void testToString_NoChange() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        LessThanOrEqual condition = new LessThanOrEqual("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        LessThanOrEqual condition = new LessThanOrEqual("totalAmount", 5000);
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("TOTAL_AMOUNT"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("5000"));
    }

    @Test
    void testUpperBoundScenarios() {
        // Test common upper bound scenarios
        LessThanOrEqual maxStock = new LessThanOrEqual("quantity", 100);
        LessThanOrEqual maxPrice = new LessThanOrEqual("price", 999.99);
        LessThanOrEqual deadline = new LessThanOrEqual("submit_date", "2023-12-31");

        assertEquals(100, (Integer) maxStock.propValue());
        assertEquals(999.99, maxPrice.propValue());
        assertEquals("2023-12-31", deadline.propValue());
    }

    @Test
    void testWithNullValue() {
        // `field <= null` is no longer renderable: the constructor rejects the null RHS with a pointed message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LessThanOrEqual("field", null));
        assertEquals("propValue must not be null for <=; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }
}
