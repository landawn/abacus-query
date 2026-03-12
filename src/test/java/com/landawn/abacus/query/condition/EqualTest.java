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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Equal2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Equal condition = new Equal("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Equal("", 25));
    }

    @Test
    public void testGetPropName() {
        Equal condition = new Equal("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Equal condition = new Equal("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Equal condition = new Equal("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Equal condition = new Equal("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Equal condition = new Equal("field", "value");
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Equal condition = new Equal("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        Equal condition = new Equal("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        Equal condition = new Equal("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Equal condition = new Equal("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    public void testToString_SnakeCase() {
        Equal condition = new Equal("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Equal cond1 = new Equal("age", 25);
        Equal cond2 = new Equal("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Equal condition = new Equal("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Equal cond1 = new Equal("field1", "value");
        Equal cond2 = new Equal("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Equal cond1 = new Equal("field", "value1");
        Equal cond2 = new Equal("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Equal condition = new Equal("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Equal condition = new Equal("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testStringValueQuoting() {
        Equal condition = new Equal("name", "John");
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("'John'"));
    }

    @Test
    public void testNumericValueNoQuoting() {
        Equal condition = new Equal("age", 25);
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("25"));
        assertFalse(str.contains("'25'"));
    }

    @Test
    public void testBooleanValue() {
        Equal condition = new Equal("active", true);
        assertEquals(true, condition.getPropValue());
    }

    @Test
    public void testToString_NoArgs() {
        Equal condition = new Equal("name", "value");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        Equal equal = new Equal("field", "value");
        NotEqual notEqual = new NotEqual("field", "value");
        assertNotEquals(equal, notEqual);
    }
}

public class EqualTest extends TestBase {

    @Test
    public void testConstructor() {
        Equal eq = Filters.eq("status", "active");

        Assertions.assertNotNull(eq);
        Assertions.assertEquals("status", eq.getPropName());
        Assertions.assertEquals("active", eq.getPropValue());
        Assertions.assertEquals(Operator.EQUAL, eq.operator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Integer
        Equal eqInt = Filters.eq("count", 100);
        Assertions.assertEquals(100, (Integer) eqInt.getPropValue());

        // Test with Boolean
        Equal eqBool = Filters.eq("active", true);
        Assertions.assertEquals(true, eqBool.getPropValue());

        // Test with Double
        Equal eqDouble = Filters.eq("price", 99.99);
        Assertions.assertEquals(99.99, eqDouble.getPropValue());

        // Test with Date
        Date now = new Date();
        Equal eqDate = Filters.eq("createdDate", now);
        Assertions.assertEquals(now, eqDate.getPropValue());

        // Test with null
        Equal eqNull = Filters.eq("deletedDate", null);
        Assertions.assertNull(eqNull.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Equal("", "value");
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Equal(null, "value");
        });
    }

    @Test
    public void testToString() {
        Equal eq = Filters.eq("name", "John");
        String result = eq.toString();
        Assertions.assertEquals("name = 'John'", result);
    }

    @Test
    public void testToStringWithNumber() {
        Equal eq = Filters.eq("age", 25);
        String result = eq.toString();
        Assertions.assertEquals("age = 25", result);
    }

    @Test
    public void testToStringWithBoolean() {
        Equal eq = Filters.eq("isActive", true);
        String result = eq.toString();
        Assertions.assertEquals("isActive = true", result);
    }

    @Test
    public void testToStringWithNull() {
        Equal eq = Filters.eq("middleName", null);
        String result = eq.toString();
        Assertions.assertEquals("middleName IS NULL", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = Filters.eq("firstName", "Jane");
        String result = eq.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertEquals("first_name = 'Jane'", result);
    }

    @Test
    public void testGetParameters() {
        Equal eq = Filters.eq("email", "test@example.com");
        var params = eq.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("test@example.com", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Equal eq = Filters.eq("id", 12345);
        Assertions.assertEquals(12345, (Integer) eq.getPropValue());

        eq.clearParameters();
        Assertions.assertNull(eq.getPropValue());
    }

    @Test
    public void testEquals() {
        Equal eq1 = Filters.eq("status", "active");
        Equal eq2 = Filters.eq("status", "active");
        Equal eq3 = Filters.eq("status", "inactive");
        Equal eq4 = Filters.eq("type", "active");

        Assertions.assertEquals(eq1, eq1);
        Assertions.assertEquals(eq1, eq2);
        Assertions.assertNotEquals(eq1, eq3); // Different value
        Assertions.assertNotEquals(eq1, eq4); // Different property
        Assertions.assertNotEquals(eq1, null);
        Assertions.assertNotEquals(eq1, "string");
    }

    @Test
    public void testHashCode() {
        Equal eq1 = Filters.eq("code", "ABC123");
        Equal eq2 = Filters.eq("code", "ABC123");

        Assertions.assertEquals(eq1.hashCode(), eq2.hashCode());
    }

    @Test
    public void testAnd() {
        Equal eq1 = Filters.eq("status", "active");
        Equal eq2 = Filters.eq("type", "premium");

        And and = eq1.and(eq2);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.operator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(eq1));
        Assertions.assertTrue(and.getConditions().contains(eq2));
    }

    @Test
    public void testOr() {
        Equal eq1 = Filters.eq("department", "Sales");
        Equal eq2 = Filters.eq("department", "Marketing");

        Or or = eq1.or(eq2);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.operator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Equal eq = Filters.eq("deleted", false);

        Not not = eq.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.operator());
        Assertions.assertEquals(eq, not.getCondition());
    }

    @Test
    public void testWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
        Equal eq = new Equal("salary", subQuery);

        Assertions.assertEquals(subQuery, eq.getPropValue());

        String result = eq.toString();
        Assertions.assertTrue(result.contains("salary ="));
        Assertions.assertTrue(result.contains("SELECT MAX(salary) FROM employees"));
    }

    @Test
    public void testWithExpression() {
        Expression expr = Expression.of("CURRENT_TIMESTAMP");
        Equal eq = new Equal("lastModified", expr);

        Assertions.assertEquals(expr, eq.getPropValue());

        String result = eq.toString();
        Assertions.assertEquals("lastModified = CURRENT_TIMESTAMP", result);
    }

    @Test
    public void testEqualWithQuestionMark() {
        Equal eq = Filters.equal("name");

        Assertions.assertEquals(Filters.QME, eq.getPropValue());
        String result = eq.toString();
        Assertions.assertEquals("name = ?", result);
    }

    @Test
    public void testEqWithQuestionMark() {
        Equal eq = Filters.eq("id");

        Assertions.assertEquals(Filters.QME, eq.getPropValue());
        String result = eq.toString();
        Assertions.assertEquals("id = ?", result);
    }

    @Test
    public void testComplexEqualityChain() {
        Equal status = Filters.eq("status", "active");
        GreaterThan age = Filters.gt("age", 18);
        LessThanOrEqual salary = Filters.le("salary", 100000);
        NotEqual type = Filters.ne("type", "temporary");

        And complex = status.and(age).and(salary).and(type);

        Assertions.assertNotNull(complex);
        Assertions.assertEquals(4, complex.getConditions().size());
    }
}
