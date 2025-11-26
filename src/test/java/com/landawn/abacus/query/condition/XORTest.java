package com.landawn.abacus.query.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class XORTest extends TestBase {

    @Test
    public void testConstructorWithStringAndObject() {
        // Test with boolean value
        XOR xor1 = Filters.xor("isPremium", true);
        Assertions.assertNotNull(xor1);
        Assertions.assertEquals("isPremium", xor1.getPropName());
        Assertions.assertEquals(Operator.XOR, xor1.getOperator());
        Assertions.assertEquals(true, xor1.getPropValue());

        // Test with string value
        XOR xor2 = Filters.xor("status", "active");
        Assertions.assertNotNull(xor2);
        Assertions.assertEquals("status", xor2.getPropName());
        Assertions.assertEquals(Operator.XOR, xor2.getOperator());
        Assertions.assertEquals("active", xor2.getPropValue());

        // Test with null value
        XOR xor3 = Filters.xor("hasDiscount", null);
        Assertions.assertNotNull(xor3);
        Assertions.assertEquals("hasDiscount", xor3.getPropName());
        Assertions.assertEquals(Operator.XOR, xor3.getOperator());
        Assertions.assertNull(xor3.getPropValue());

        // Test with numeric value
        XOR xor4 = Filters.xor("level", 5);
        Assertions.assertNotNull(xor4);
        Assertions.assertEquals("level", xor4.getPropName());
        Assertions.assertEquals(Operator.XOR, xor4.getOperator());
        Assertions.assertEquals(5, (Integer) xor4.getPropValue());
    }

    @Test
    public void testToString() {
        XOR xor = Filters.xor("hasGoldMembership", true);
        String str = xor.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("hasGoldMembership"));
        Assertions.assertTrue(str.contains("XOR"));
        Assertions.assertTrue(str.contains("true"));
    }

    @Test
    public void testEquals() {
        XOR xor1 = Filters.xor("isPremium", true);
        XOR xor2 = Filters.xor("isPremium", true);
        XOR xor3 = Filters.xor("isPremium", false);
        XOR xor4 = Filters.xor("isTrial", true);

        Assertions.assertEquals(xor1, xor2);
        Assertions.assertNotEquals(xor1, xor3);
        Assertions.assertNotEquals(xor1, xor4);
        Assertions.assertNotEquals(xor1, null);
        Assertions.assertNotEquals(xor1, "string");
    }

    @Test
    public void testHashCode() {
        XOR xor1 = Filters.xor("isPremium", true);
        XOR xor2 = Filters.xor("isPremium", true);
        XOR xor3 = Filters.xor("isPremium", false);

        Assertions.assertEquals(xor1.hashCode(), xor2.hashCode());
        // Different values might have different hash codes (not guaranteed but likely)
        // We just check that hashCode() doesn't throw exception
        Assertions.assertDoesNotThrow(() -> xor3.hashCode());
    }

    @Test
    public void testCombinationWithOtherConditions() {
        // Test XOR in combination with AND
        XOR xor1 = Filters.xor("hasStudentDiscount", true);
        XOR xor2 = Filters.xor("hasSeniorDiscount", true);

        var and = xor1.and(xor2);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());

        // Test XOR in combination with OR
        var or = xor1.or(xor2);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }
}