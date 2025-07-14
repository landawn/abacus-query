package com.landawn.abacus.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class BinaryTest extends TestBase {

    @Test
    public void testConstructor() {
        Binary binary = ConditionFactory.binary("price", Operator.GREATER_THAN, 100.0);

        Assertions.assertNotNull(binary);
        Assertions.assertEquals("price", binary.getPropName());
        Assertions.assertEquals(Operator.GREATER_THAN, binary.getOperator());
        Assertions.assertEquals(100.0, binary.getPropValue());
    }

    @Test
    public void testConstructorWithNullValue() {
        Binary binary = ConditionFactory.binary("optional", Operator.EQUAL, null);

        Assertions.assertNotNull(binary);
        Assertions.assertNull(binary.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary("", Operator.EQUAL, "value");
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary(null, Operator.EQUAL, "value");
        });
    }

    @Test
    public void testGetPropName() {
        Binary binary = ConditionFactory.binary("userName", Operator.EQUAL, "John");
        Assertions.assertEquals("userName", binary.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Binary binary = ConditionFactory.binary("age", Operator.GREATER_EQUAL, 25);
        Integer value = binary.getPropValue();
        Assertions.assertEquals(25, value);
    }

    @Test
    public void testSetPropValue() {
        Binary binary = ConditionFactory.binary("status", Operator.EQUAL, "active");
        binary.setPropValue("inactive");
        Assertions.assertEquals("inactive", binary.getPropValue());
    }

    @Test
    public void testGetParametersWithLiteralValue() {
        Binary binary = ConditionFactory.binary("score", Operator.LESS_THAN, 80.5);
        List<Object> params = binary.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(80.5, params.get(0));
    }

    @Test
    public void testGetParametersWithConditionValue() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT MAX(price) FROM products");
        Binary binary = ConditionFactory.binary("price", Operator.EQUAL, subQuery);

        List<Object> params = binary.getParameters();
        Assertions.assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testClearParametersWithLiteralValue() {
        Binary binary = ConditionFactory.binary("count", Operator.GREATER_THAN, 100);
        binary.clearParameters();

        Assertions.assertNull(binary.getPropValue());
    }

    @Test
    public void testClearParametersWithConditionValue() {
        Between between = ConditionFactory.between("value", 10, 20);
        Binary binary = ConditionFactory.binary("range", Operator.EQUAL, between);

        binary.clearParameters();

        List<Object> params = binary.getParameters();
        Assertions.assertTrue(params.size() == 2 && params.get(0) == null && params.get(1) == null);
    }

    @Test
    public void testCopy() {
        Binary original = ConditionFactory.binary("temperature", Operator.LESS_EQUAL, 32.0);
        Binary copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testCopyWithConditionValue() {
        In in = ConditionFactory.in("id", Arrays.asList(1, 2, 3));
        Binary original = ConditionFactory.binary("ids", Operator.EQUAL, in);
        Binary copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertNotSame(original.getPropValue(), copy.getPropValue());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        Binary binary = ConditionFactory.binary("name", Operator.NOT_EQUAL, "Test");
        String result = binary.toString();

        Assertions.assertEquals("name != 'Test'", result);
    }

    @Test
    public void testToStringWithNumber() {
        Binary binary = ConditionFactory.binary("amount", Operator.GREATER_THAN, 1000);
        String result = binary.toString();

        Assertions.assertEquals("amount > 1000", result);
    }

    @Test
    public void testToStringWithNull() {
        Binary binary = ConditionFactory.binary("deleted", Operator.EQUAL, null);
        String result = binary.toString();

        Assertions.assertEquals("deleted = null", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Binary binary = ConditionFactory.binary("firstName", Operator.LIKE, "John%");
        String result = binary.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        Assertions.assertEquals("first_name LIKE 'John%'", result);
    }

    @Test
    public void testToStringWithConditionValue() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM users WHERE active = true");
        Binary binary = ConditionFactory.binary("user_id", Operator.IN, subQuery);
        String result = binary.toString();

        Assertions.assertTrue(result.contains("user_id IN"));
        Assertions.assertTrue(result.contains("SELECT id FROM users WHERE active = true"));
    }

    @Test
    public void testEquals() {
        Binary binary1 = ConditionFactory.binary("age", Operator.GREATER_EQUAL, 18);
        Binary binary2 = ConditionFactory.binary("age", Operator.GREATER_EQUAL, 18);
        Binary binary3 = ConditionFactory.binary("age", Operator.GREATER_EQUAL, 21);
        Binary binary4 = ConditionFactory.binary("age", Operator.GREATER_THAN, 18);
        Binary binary5 = ConditionFactory.binary("height", Operator.GREATER_EQUAL, 18);

        Assertions.assertEquals(binary1, binary1);
        Assertions.assertEquals(binary1, binary2);
        Assertions.assertNotEquals(binary1, binary3); // Different value
        Assertions.assertNotEquals(binary1, binary4); // Different operator
        Assertions.assertNotEquals(binary1, binary5); // Different property
        Assertions.assertNotEquals(binary1, null);
        Assertions.assertNotEquals(binary1, "string");
    }

    @Test
    public void testHashCode() {
        Binary binary1 = ConditionFactory.binary("status", Operator.EQUAL, "active");
        Binary binary2 = ConditionFactory.binary("status", Operator.EQUAL, "active");

        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testHashCodeWithNull() {
        Binary binary1 = ConditionFactory.binary("optional", Operator.EQUAL, null);
        Binary binary2 = ConditionFactory.binary("optional", Operator.EQUAL, null);

        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testAnd() {
        Binary binary = ConditionFactory.binary("age", Operator.GREATER_EQUAL, 18);
        LessThan lt = ConditionFactory.lt("age", 65);

        And and = binary.and(lt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(binary));
        Assertions.assertTrue(and.getConditions().contains(lt));
    }

    @Test
    public void testOr() {
        Binary binary = ConditionFactory.binary("status", Operator.EQUAL, "premium");
        Equal eq = ConditionFactory.eq("status", "vip");

        Or or = binary.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Binary binary = ConditionFactory.binary("active", Operator.EQUAL, false);

        Not not = binary.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(binary, not.getCondition());
    }

    @Test
    public void testAllOperators() {
        // Test with various operators
        Binary eq = ConditionFactory.binary("prop", Operator.EQUAL, 1);
        Binary ne = ConditionFactory.binary("prop", Operator.NOT_EQUAL, 1);
        Binary gt = ConditionFactory.binary("prop", Operator.GREATER_THAN, 1);
        Binary ge = ConditionFactory.binary("prop", Operator.GREATER_EQUAL, 1);
        Binary lt = ConditionFactory.binary("prop", Operator.LESS_THAN, 1);
        Binary le = ConditionFactory.binary("prop", Operator.LESS_EQUAL, 1);
        Binary like = ConditionFactory.binary("prop", Operator.LIKE, "%test%");
        Binary in = ConditionFactory.binary("prop", Operator.IN, Arrays.asList(1, 2, 3));

        Assertions.assertEquals("prop = 1", eq.toString());
        Assertions.assertEquals("prop != 1", ne.toString());
        Assertions.assertEquals("prop > 1", gt.toString());
        Assertions.assertEquals("prop >= 1", ge.toString());
        Assertions.assertEquals("prop < 1", lt.toString());
        Assertions.assertEquals("prop <= 1", le.toString());
        Assertions.assertEquals("prop LIKE '%test%'", like.toString());
        Assertions.assertTrue(in.toString().contains("prop IN"));
    }
}