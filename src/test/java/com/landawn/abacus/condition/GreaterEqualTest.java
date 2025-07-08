package com.landawn.abacus.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class GreaterEqualTest extends TestBase {

    @Test
    public void testConstructor() {
        GreaterEqual ge = ConditionFactory.ge("age", 18);
        Assertions.assertNotNull(ge);
        Assertions.assertEquals("age", ge.getPropName());
        Assertions.assertEquals(18, (Integer)(Integer) ge.getPropValue());
        Assertions.assertEquals(Operator.GREATER_EQUAL, ge.getOperator());
    }

    @Test
    public void testConstructorWithNullValue() {
        GreaterEqual ge = ConditionFactory.ge("name", null);
        Assertions.assertNotNull(ge);
        Assertions.assertEquals("name", ge.getPropName());
        Assertions.assertNull(ge.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterEqual("", 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GreaterEqual(null, 10);
        });
    }

    @Test
    public void testToString() {
        GreaterEqual ge = ConditionFactory.ge("salary", 50000);
        String result = ge.toString();
        Assertions.assertEquals("salary >= 50000", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GreaterEqual ge = ConditionFactory.ge("firstName", "John");
        String result = ge.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("first_name >= 'John'", result);
    }

    @Test
    public void testEquals() {
        GreaterEqual ge1 = ConditionFactory.ge("age", 25);
        GreaterEqual ge2 = ConditionFactory.ge("age", 25);
        GreaterEqual ge3 = ConditionFactory.ge("age", 30);
        GreaterEqual ge4 = ConditionFactory.ge("name", 25);

        Assertions.assertEquals(ge1, ge2);
        Assertions.assertNotEquals(ge1, ge3);
        Assertions.assertNotEquals(ge1, ge4);
        Assertions.assertNotEquals(ge1, null);
        Assertions.assertNotEquals(ge1, "string");
    }

    @Test
    public void testHashCode() {
        GreaterEqual ge1 = ConditionFactory.ge("age", 25);
        GreaterEqual ge2 = ConditionFactory.ge("age", 25);

        Assertions.assertEquals(ge1.hashCode(), ge2.hashCode());
    }

    @Test
    public void testCopy() {
        GreaterEqual original = ConditionFactory.ge("score", 80.5);
        GreaterEqual copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testGetParameters() {
        GreaterEqual ge = ConditionFactory.ge("price", 100.0);
        var params = ge.getParameters();
        
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(100.0, params.get(0));
    }

    @Test
    public void testClearParameters() {
        GreaterEqual ge = ConditionFactory.ge("amount", 500);
        ge.clearParameters();
        
        Assertions.assertNull(ge.getPropValue());
    }

    @Test
    public void testAnd() {
        GreaterEqual ge = ConditionFactory.ge("age", 18);
        LessThan lt = ConditionFactory.lt("age", 65);
        And and = ge.and(lt);
        
        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.getOperator());
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testOr() {
        GreaterEqual ge = ConditionFactory.ge("score", 90);
        Equal eq = ConditionFactory.eq("grade", "A");
        Or or = ge.or(eq);
        
        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        GreaterEqual ge = ConditionFactory.ge("temperature", 0);
        Not not = ge.not();
        
        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.getOperator());
        Assertions.assertEquals(ge, not.getCondition());
    }
}