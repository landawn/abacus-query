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
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class GreaterThanTest extends TestBase {

    @Test
    void testAnd() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testComplexComparison() {
        // Test chaining multiple conditions
        GreaterThan salary = Filters.gt("salary", 50000);
        GreaterThanOrEqual experience = Filters.ge("yearsExperience", 5);
        LessThan age = Filters.lt("age", 50);

        And qualified = salary.and(experience).and(age);

        assertNotNull(qualified);
        assertEquals(3, qualified.conditions().size());
    }

    @Test
    void testConstructor() {
        GreaterThan condition = new GreaterThan("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.GREATER_THAN, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan(null, 25));
    }

    @Test
    void testConstructorWithDifferentTypes() {
        // Test with Double
        GreaterThan gtDouble = Filters.gt("price", 99.99);
        assertEquals(99.99, gtDouble.propValue());

        // Test with Long
        GreaterThan gtLong = Filters.gt("count", 1000000L);
        assertEquals(1000000L, (Long) gtLong.propValue());

        // Test with Date
        Date createdAt = new Date(1_700_000_000_000L);
        GreaterThan gtDate = Filters.gt("createdDate", createdAt);
        assertEquals(createdAt, gtDate.propValue());

        // Test with String (for alphabetical comparison)
        GreaterThan gtString = Filters.gt("name", "M");
        assertEquals("M", gtString.propValue());
    }

    @Test
    void testConstructorWithEmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan("", 10);
        });
    }

    @Test
    void testConstructorWithNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Filters.gt("value", null));
        assertEquals("propValue must not be null for >; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testConstructorWithNullPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan(null, 10);
        });
    }

    @Test
    void testEquals() {
        GreaterThan gt1 = Filters.gt("age", 21);
        GreaterThan gt2 = Filters.gt("age", 21);
        GreaterThan gt3 = Filters.gt("age", 18);
        GreaterThan gt4 = Filters.gt("height", 21);

        assertEquals(gt1, gt1);
        assertEquals(gt1, gt2);
        assertNotEquals(gt1, gt3); // Different value
        assertNotEquals(gt1, gt4); // Different property
        assertNotEquals(gt1, null);
        assertNotEquals(gt1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        GreaterThan greaterThan = new GreaterThan("field", 10);
        LessThan lessThan = new LessThan("field", 10);
        assertNotEquals(greaterThan, lessThan);
    }

    @Test
    void testEquals_DifferentPropName() {
        GreaterThan cond1 = new GreaterThan("field1", "value");
        GreaterThan cond2 = new GreaterThan("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        GreaterThan cond1 = new GreaterThan("field", "value1");
        GreaterThan cond2 = new GreaterThan("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        GreaterThan cond1 = new GreaterThan("status", "active");
        GreaterThan cond2 = new GreaterThan("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(Operator.GREATER_THAN, condition.operator());
    }

    @Test
    void testGetPropName() {
        GreaterThan condition = new GreaterThan("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testGetPropValue_Null() {
        // A null RHS is rejected at construction: `x > NULL` can never be true under three-valued logic.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new GreaterThan("field", null));
        assertEquals("propValue must not be null for >; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testHashCode() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        GreaterThan condition = new GreaterThan("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        GreaterThan condition = new GreaterThan("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        GreaterThan condition = new GreaterThan("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        GreaterThan condition = new GreaterThan("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        GreaterThan condition = new GreaterThan("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        GreaterThan gt = Filters.gt("salary", 50000);
        String result = gt.toString();
        assertEquals("salary > 50000", result);
    }

    @Test
    void testToString_NoArgs() {
        GreaterThan condition = new GreaterThan("age", 18);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("18"));
    }

    @Test
    void testToString_NoChange() {
        GreaterThan condition = new GreaterThan("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        GreaterThan condition = new GreaterThan("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        GreaterThan gt = Filters.gt("yearOfBirth", 1990);
        String result = gt.toSql(NamingPolicy.SNAKE_CASE);
        assertEquals("year_of_birth > 1990", result);
    }

    @Test
    void testToStringWithString() {
        GreaterThan gt = Filters.gt("grade", "B");
        String result = gt.toString();
        assertEquals("grade > 'B'", result);
    }

    @Test
    void testWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT AVG(salary) FROM employees");
        GreaterThan gt = new GreaterThan("salary", subQuery);

        assertEquals(subQuery, gt.propValue());

        String result = gt.toString();
        assertTrue(result.contains("salary >"));
        assertTrue(result.contains("SELECT AVG(salary) FROM employees"));
    }
}
