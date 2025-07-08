package com.landawn.abacus.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class NotLikeTest extends TestBase {

    @Test
    public void testConstructor() {
        NotLike notLike = CF.notLike("name", "John%");
        
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("name", notLike.getPropName());
        Assertions.assertEquals("John%", notLike.getPropValue());
        Assertions.assertEquals(Operator.NOT_LIKE, notLike.getOperator());
    }

    @Test
    public void testConstructorWithWildcardPercent() {
        NotLike notLike = CF.notLike("email", "%@gmail.com");
        
        Assertions.assertEquals("email", notLike.getPropName());
        Assertions.assertEquals("%@gmail.com", notLike.getPropValue());
    }

    @Test
    public void testConstructorWithWildcardUnderscore() {
        NotLike notLike = CF.notLike("code", "___");
        
        Assertions.assertEquals("code", notLike.getPropName());
        Assertions.assertEquals("___", notLike.getPropValue());
    }

    @Test
    public void testConstructorWithMultipleWildcards() {
        NotLike notLike = CF.notLike("productName", "%temp%");
        
        Assertions.assertEquals("productName", notLike.getPropName());
        Assertions.assertEquals("%temp%", notLike.getPropValue());
    }

    @Test
    public void testGetParameters() {
        NotLike notLike = CF.notLike("filename", "%.tmp");
        
        Assertions.assertEquals(1, notLike.getParameters().size());
        Assertions.assertEquals("%.tmp", notLike.getParameters().get(0));
    }

    @Test
    public void testClearParameters() {
        NotLike notLike = CF.notLike("name", "%test%");
        
        notLike.clearParameters();
        
        Assertions.assertNull(notLike.getPropValue());
        Assertions.assertTrue(notLike.getParameters().isEmpty());
    }

    @Test
    public void testToString() {
        NotLike notLike = CF.notLike("description", "%important%");
        
        String result = notLike.toString();
        Assertions.assertTrue(result.contains("description"));
        Assertions.assertTrue(result.contains("NOT LIKE"));
        Assertions.assertTrue(result.contains("%important%"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        NotLike notLike = CF.notLike("user_name", "admin%");
        
        String result = notLike.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("USER_NAME"));
        Assertions.assertTrue(result.contains("NOT LIKE"));
        Assertions.assertTrue(result.contains("admin%"));
    }

    @Test
    public void testCopy() {
        NotLike original = CF.notLike("email", "%@temp.com");
        
        NotLike copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testHashCode() {
        NotLike notLike1 = CF.notLike("name", "%test%");
        NotLike notLike2 = CF.notLike("name", "%test%");
        NotLike notLike3 = CF.notLike("name", "%demo%");
        
        Assertions.assertEquals(notLike1.hashCode(), notLike2.hashCode());
        Assertions.assertNotEquals(notLike1.hashCode(), notLike3.hashCode());
    }

    @Test
    public void testEquals() {
        NotLike notLike1 = CF.notLike("name", "%test%");
        NotLike notLike2 = CF.notLike("name", "%test%");
        NotLike notLike3 = CF.notLike("name", "%demo%");
        NotLike notLike4 = CF.notLike("email", "%test%");
        
        Assertions.assertTrue(notLike1.equals(notLike1));
        Assertions.assertTrue(notLike1.equals(notLike2));
        Assertions.assertFalse(notLike1.equals(notLike3));
        Assertions.assertFalse(notLike1.equals(notLike4));
        Assertions.assertFalse(notLike1.equals(null));
        Assertions.assertFalse(notLike1.equals("not a NotLike"));
    }

    @Test
    public void testWithNullValue() {
        NotLike notLike = CF.notLike("name", null);
        
        Assertions.assertNull(notLike.getPropValue());
        Assertions.assertTrue(notLike.getParameters().isEmpty());
    }

    @Test
    public void testComplexPatterns() {
        // Test various complex patterns
        NotLike pattern1 = CF.notLike("path", "/temp/%/%.tmp");
        NotLike pattern2 = CF.notLike("email", "%@%.%");
        NotLike pattern3 = CF.notLike("code", "A_B_C%");
        
        Assertions.assertEquals("/temp/%/%.tmp", pattern1.getPropValue());
        Assertions.assertEquals("%@%.%", pattern2.getPropValue());
        Assertions.assertEquals("A_B_C%", pattern3.getPropValue());
    }
}