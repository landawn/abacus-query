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

public class EqualTest extends TestBase {

    @Test
    void testAnd() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testBooleanValue() {
        Equal condition = new Equal("active", true);
        assertEquals(true, condition.propValue());
    }

    @Test
    void testComplexEqualityChain() {
        Equal status = Filters.eq("status", "active");
        GreaterThan age = Filters.gt("age", 18);
        LessThanOrEqual salary = Filters.le("salary", 100000);
        NotEqual type = Filters.ne("type", "temporary");

        And complex = status.and(age).and(salary).and(type);

        assertNotNull(complex);
        assertEquals(4, complex.conditions().size());
    }

    @Test
    void testConstructor() {
        Equal condition = new Equal("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal(null, 25));
    }

    @Test
    void testConstructorWithDifferentTypes() {
        // Test with Integer
        Equal eqInt = Filters.eq("count", 100);
        assertEquals(100, (Integer) eqInt.propValue());

        // Test with Boolean
        Equal eqBool = Filters.eq("active", true);
        assertEquals(true, eqBool.propValue());

        // Test with Double
        Equal eqDouble = Filters.eq("price", 99.99);
        assertEquals(99.99, eqDouble.propValue());

        // Test with Date
        Date createdAt = new Date(1_700_000_000_000L);
        Equal eqDate = Filters.eq("createdDate", createdAt);
        assertEquals(createdAt, eqDate.propValue());

        // Test with null
        Equal eqNull = Filters.eq("deletedDate", null);
        assertNull(eqNull.propValue());
    }

    @Test
    void testConstructorWithEmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Equal("", "value");
        });
    }

    @Test
    void testConstructorWithNullPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Equal(null, "value");
        });
    }

    @Test
    void testEquals() {
        Equal eq1 = Filters.eq("status", "active");
        Equal eq2 = Filters.eq("status", "active");
        Equal eq3 = Filters.eq("status", "inactive");
        Equal eq4 = Filters.eq("type", "active");

        assertEquals(eq1, eq1);
        assertEquals(eq1, eq2);
        assertNotEquals(eq1, eq3); // Different value
        assertNotEquals(eq1, eq4); // Different property
        assertNotEquals(eq1, null);
        assertNotEquals(eq1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        Equal equal = new Equal("field", "value");
        NotEqual notEqual = new NotEqual("field", "value");
        assertNotEquals(equal, notEqual);
    }

    @Test
    void testEquals_DifferentPropName() {
        Equal cond1 = new Equal("field1", "value");
        Equal cond2 = new Equal("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        Equal cond1 = new Equal("field", "value1");
        Equal cond2 = new Equal("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        Equal condition = new Equal("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testEqualWithQuestionMark() {
        Equal eq = Filters.equal("name");

        assertEquals(Filters.QME, eq.propValue());
        String result = eq.toString();
        assertEquals("name = ?", result);
    }

    @Test
    void testEqWithQuestionMark() {
        Equal eq = Filters.eq("id");

        assertEquals(Filters.QME, eq.propValue());
        String result = eq.toString();
        assertEquals("id = ?", result);
    }

    @Test
    void testGetOperator() {
        Equal condition = new Equal("field", "value");
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    void testGetPropName() {
        Equal condition = new Equal("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testGetPropValue_Null() {
        Equal condition = new Equal("field", null);
        assertNull(condition.propValue());
    }

    @Test
    void testHashCode() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        Equal condition = new Equal("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testNumericValueNoQuoting() {
        Equal condition = new Equal("age", 25);
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("25"));
        assertFalse(str.contains("'25'"));
    }

    @Test
    void testOr() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        Equal condition = new Equal("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        Equal condition = new Equal("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        Equal condition = new Equal("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        Equal condition = new Equal("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testStringValueQuoting() {
        Equal condition = new Equal("name", "John");
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("'John'"));
    }

    @Test
    void testToString() {
        Equal eq = Filters.eq("name", "John");
        String result = eq.toString();
        assertEquals("name = 'John'", result);
    }

    @Test
    void testToString_NoArgs() {
        Equal condition = new Equal("name", "value");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    void testToString_NoChange() {
        Equal condition = new Equal("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    void testToString_SnakeCase() {
        Equal condition = new Equal("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithBoolean() {
        Equal eq = Filters.eq("isActive", true);
        String result = eq.toString();
        assertEquals("isActive = true", result);
    }

    @Test
    void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("firstName", "Jane");
        String result = eq.toSql(NamingPolicy.SNAKE_CASE);
        assertEquals("first_name = 'Jane'", result);
    }

    @Test
    void testToStringWithNull() {
        Equal eq = Filters.eq("middleName", null);
        String result = eq.toString();
        assertEquals("middleName IS NULL", result);
    }

    @Test
    void testToStringWithNumber() {
        Equal eq = Filters.eq("age", 25);
        String result = eq.toString();
        assertEquals("age = 25", result);
    }

    @Test
    void testWithExpression() {
        SqlExpression expr = SqlExpression.of("CURRENT_TIMESTAMP");
        Equal eq = new Equal("lastModified", expr);

        assertEquals(expr, eq.propValue());

        String result = eq.toString();
        assertEquals("lastModified = CURRENT_TIMESTAMP", result);
    }

    @Test
    void testWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
        Equal eq = new Equal("salary", subQuery);

        assertEquals(subQuery, eq.propValue());

        String result = eq.toString();
        assertTrue(result.contains("salary ="));
        assertTrue(result.contains("SELECT MAX(salary) FROM employees"));
    }
}
