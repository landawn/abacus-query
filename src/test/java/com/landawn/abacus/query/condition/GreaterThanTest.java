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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class GreaterThan2025Test extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThan condition = new GreaterThan("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.GREATER_THAN, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("", 25));
    }

    @Test
    public void testGetPropName() {
        GreaterThan condition = new GreaterThan("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        GreaterThan condition = new GreaterThan("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        GreaterThan condition = new GreaterThan("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        GreaterThan condition = new GreaterThan("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(Operator.GREATER_THAN, condition.operator());
    }

    @Test
    public void testGetParameters() {
        GreaterThan condition = new GreaterThan("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        GreaterThan condition = new GreaterThan("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        GreaterThan condition = new GreaterThan("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        GreaterThan condition = new GreaterThan("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        GreaterThan cond1 = new GreaterThan("age", 25);
        GreaterThan cond2 = new GreaterThan("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        GreaterThan cond1 = new GreaterThan("status", "active");
        GreaterThan cond2 = new GreaterThan("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        GreaterThan cond1 = new GreaterThan("field1", "value");
        GreaterThan cond2 = new GreaterThan("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        GreaterThan cond1 = new GreaterThan("field", "value1");
        GreaterThan cond2 = new GreaterThan("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        GreaterThan condition = new GreaterThan("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterThan cond1 = new GreaterThan("a", 1);
        GreaterThan cond2 = new GreaterThan("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThan condition = new GreaterThan("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        GreaterThan condition = new GreaterThan("age", 18);
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("18"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        GreaterThan greaterThan = new GreaterThan("field", 10);
        LessThan lessThan = new LessThan("field", 10);
        assertNotEquals(greaterThan, lessThan);
    }
}

public class GreaterThanTest extends TestBase {

    @Test
    public void testConstructor() {
        GreaterThan gt = Filters.gt("age", 18);

        Assertions.assertNotNull(gt);
        Assertions.assertEquals("age", gt.getPropName());
        Assertions.assertEquals(18, (Integer) gt.getPropValue());
        Assertions.assertEquals(Operator.GREATER_THAN, gt.operator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Double
        GreaterThan gtDouble = Filters.gt("price", 99.99);
        Assertions.assertEquals(99.99, gtDouble.getPropValue());

        // Test with Long
        GreaterThan gtLong = Filters.gt("count", 1000000L);
        Assertions.assertEquals(1000000L, (Long) gtLong.getPropValue());

        // Test with Date
        Date now = new Date();
        GreaterThan gtDate = Filters.gt("createdDate", now);
        Assertions.assertEquals(now, gtDate.getPropValue());

        // Test with String (for alphabetical comparison)
        GreaterThan gtString = Filters.gt("name", "M");
        Assertions.assertEquals("M", gtString.getPropValue());
    }

    @Test
    public void testConstructorWithNull() {
        GreaterThan gt = Filters.gt("value", null);
        Assertions.assertNotNull(gt);
        Assertions.assertNull(gt.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan("", 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterThan(null, 10);
        });
    }

    @Test
    public void testToString() {
        GreaterThan gt = Filters.gt("salary", 50000);
        String result = gt.toString();
        Assertions.assertEquals("salary > 50000", result);
    }

    @Test
    public void testToStringWithString() {
        GreaterThan gt = Filters.gt("grade", "B");
        String result = gt.toString();
        Assertions.assertEquals("grade > 'B'", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GreaterThan gt = Filters.gt("yearOfBirth", 1990);
        String result = gt.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertEquals("year_of_birth > 1990", result);
    }

    @Test
    public void testGetParameters() {
        GreaterThan gt = Filters.gt("temperature", 32.5);
        var params = gt.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(32.5, params.get(0));
    }

    @Test
    public void testEquals() {
        GreaterThan gt1 = Filters.gt("age", 21);
        GreaterThan gt2 = Filters.gt("age", 21);
        GreaterThan gt3 = Filters.gt("age", 18);
        GreaterThan gt4 = Filters.gt("height", 21);

        Assertions.assertEquals(gt1, gt1);
        Assertions.assertEquals(gt1, gt2);
        Assertions.assertNotEquals(gt1, gt3); // Different value
        Assertions.assertNotEquals(gt1, gt4); // Different property
        Assertions.assertNotEquals(gt1, null);
        Assertions.assertNotEquals(gt1, "string");
    }

    @Test
    public void testHashCode() {
        GreaterThan gt1 = Filters.gt("quantity", 100);
        GreaterThan gt2 = Filters.gt("quantity", 100);

        Assertions.assertEquals(gt1.hashCode(), gt2.hashCode());
    }

    @Test
    public void testAnd() {
        GreaterThan gt = Filters.gt("age", 18);
        LessThan lt = Filters.lt("age", 65);

        And and = gt.and(lt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.operator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(gt));
        Assertions.assertTrue(and.getConditions().contains(lt));
    }

    @Test
    public void testOr() {
        GreaterThan gt = Filters.gt("score", 90);
        Equal eq = Filters.eq("grade", "A");

        Or or = gt.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.operator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterThan gt = Filters.gt("balance", 0);

        Not not = gt.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.operator());
        Assertions.assertEquals(gt, not.getCondition());
    }

    @Test
    public void testWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT AVG(salary) FROM employees");
        GreaterThan gt = new GreaterThan("salary", subQuery);

        Assertions.assertEquals(subQuery, gt.getPropValue());

        String result = gt.toString();
        Assertions.assertTrue(result.contains("salary >"));
        Assertions.assertTrue(result.contains("SELECT AVG(salary) FROM employees"));
    }

    @Test
    public void testComplexComparison() {
        // Test chaining multiple conditions
        GreaterThan salary = Filters.gt("salary", 50000);
        GreaterThanOrEqual experience = Filters.ge("yearsExperience", 5);
        LessThan age = Filters.lt("age", 50);

        And qualified = salary.and(experience).and(age);

        Assertions.assertNotNull(qualified);
        Assertions.assertEquals(3, qualified.getConditions().size());
    }
}
