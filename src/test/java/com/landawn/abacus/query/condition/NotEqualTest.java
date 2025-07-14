package com.landawn.abacus.query.condition;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class NotEqualTest extends TestBase {

    @Test
    public void testConstructor() {
        NotEqual notEqual = CF.ne("status", "deleted");
        
        Assertions.assertNotNull(notEqual);
        Assertions.assertEquals("status", notEqual.getPropName());
        Assertions.assertEquals("deleted", notEqual.getPropValue());
        Assertions.assertEquals(Operator.NOT_EQUAL, notEqual.getOperator());
    }

    @Test
    public void testConstructorWithNumericValue() {
        NotEqual notEqual = CF.ne("quantity", 0);
        
        Assertions.assertEquals("quantity", notEqual.getPropName());
        Assertions.assertEquals(0, (Integer) notEqual.getPropValue());
    }

    @Test
    public void testConstructorWithNullValue() {
        NotEqual notEqual = CF.ne("assignee", null);
        
        Assertions.assertEquals("assignee", notEqual.getPropName());
        Assertions.assertNull(notEqual.getPropValue());
    }

    @Test
    public void testConstructorWithDateString() {
        NotEqual notEqual = CF.ne("created", "2024-01-01");
        
        Assertions.assertEquals("created", notEqual.getPropName());
        Assertions.assertEquals("2024-01-01", notEqual.getPropValue());
    }

    @Test
    public void testGetParameters() {
        NotEqual notEqual = CF.ne("username", "admin");
        
        Assertions.assertEquals(1, notEqual.getParameters().size());
        Assertions.assertEquals("admin", notEqual.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithNull() {
        NotEqual notEqual = CF.ne("value", null);
        
        Assertions.assertNull(notEqual.getParameters().get(0));
    }

    @Test
    public void testClearParameters() {
        NotEqual notEqual = CF.ne("type", "default");
        
        notEqual.clearParameters();
        
        Assertions.assertNull(notEqual.getPropValue());
        Assertions.assertNull(notEqual.getParameters().get(0));
    }

    @Test
    public void testToString() {
        NotEqual notEqual = CF.ne("status", "inactive");
        
        String result = notEqual.toString();
        Assertions.assertTrue(result.contains("status"));
        Assertions.assertTrue(result.contains("!="));
        Assertions.assertTrue(result.contains("inactive"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        NotEqual notEqual = CF.ne("user_status", "banned");
        
        String result = notEqual.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("USER_STATUS"));
        Assertions.assertTrue(result.contains("!="));
        Assertions.assertTrue(result.contains("banned"));
    }

    @Test
    public void testCopy() {
        NotEqual original = CF.ne("role", "guest");
        
        NotEqual copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testHashCode() {
        NotEqual notEqual1 = CF.ne("status", "deleted");
        NotEqual notEqual2 = CF.ne("status", "deleted");
        NotEqual notEqual3 = CF.ne("status", "active");
        NotEqual notEqual4 = CF.ne("type", "deleted");

        Assertions.assertEquals(notEqual1.hashCode(), notEqual2.hashCode());
        Assertions.assertNotEquals(notEqual1.hashCode(), notEqual3.hashCode());
        Assertions.assertNotEquals(notEqual1.hashCode(), notEqual4.hashCode());
    }

    @Test
    public void testEquals() {
        NotEqual notEqual1 = CF.ne("status", "deleted");
        NotEqual notEqual2 = CF.ne("status", "deleted");
        NotEqual notEqual3 = CF.ne("status", "active");
        NotEqual notEqual4 = CF.ne("type", "deleted");

        Assertions.assertTrue(notEqual1.equals(notEqual1));
        Assertions.assertTrue(notEqual1.equals(notEqual2));
        Assertions.assertFalse(notEqual1.equals(notEqual3));
        Assertions.assertFalse(notEqual1.equals(notEqual4));
        Assertions.assertFalse(notEqual1.equals(null));
        Assertions.assertFalse(notEqual1.equals("not a NotEqual"));
    }

    @Test
    public void testWithDifferentDataTypes() {
        // String
        NotEqual stringNe = CF.ne("name", "test");
        Assertions.assertEquals("test", stringNe.getPropValue());

        // Integer
        NotEqual intNe = CF.ne("count", 42);
        Assertions.assertEquals(42, (Integer) intNe.getPropValue());

        // Double
        NotEqual doubleNe = CF.ne("price", 99.99);
        Assertions.assertEquals(99.99, doubleNe.getPropValue());

        // Boolean
        NotEqual boolNe = CF.ne("active", true);
        Assertions.assertEquals(true, boolNe.getPropValue());

        // Date
        Date now = new Date();
        NotEqual dateNe = CF.ne("created", now);
        Assertions.assertEquals(now, dateNe.getPropValue());
    }

    @Test
    public void testPracticalExamples() {
        // Exclude specific user
        NotEqual notAdmin = CF.ne("username", "admin");
        Assertions.assertEquals("username", notAdmin.getPropName());
        Assertions.assertEquals("admin", notAdmin.getPropValue());

        // Exclude default values
        NotEqual notDefault = CF.ne("configuration", "default");
        Assertions.assertEquals("configuration", notDefault.getPropName());
        Assertions.assertEquals("default", notDefault.getPropValue());

        // Filter out zero values
        NotEqual notZero = CF.ne("balance", 0);
        Assertions.assertEquals("balance", notZero.getPropName());
        Assertions.assertEquals(0, (Integer) notZero.getPropValue());
    }

    @Test
    public void testChainedConditions() {
        // Can be used in AND/OR chains
        NotEqual ne1 = CF.ne("status", "deleted");
        NotEqual ne2 = CF.ne("status", "archived");

        // Both conditions are independent and valid
        Assertions.assertNotNull(ne1);
        Assertions.assertNotNull(ne2);
        Assertions.assertNotEquals(ne1, ne2);
    }

    @Test
    public void testWithComplexObjects() {
        // Test with complex object (though typically you'd use primitive values)
        Object complexObject = new Object() {
            @Override
            public String toString() {
                return "ComplexObject";
            }
        };

        NotEqual notEqual = CF.ne("data", complexObject);
        Assertions.assertEquals(complexObject, notEqual.getPropValue());
    }
}