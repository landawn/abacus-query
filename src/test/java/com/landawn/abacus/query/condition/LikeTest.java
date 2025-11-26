package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class LikeTest extends TestBase {

    @Test
    public void testConstructorStartsWith() {
        Like condition = new Like("name", "John%");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals(Operator.LIKE, condition.getOperator());
        Assertions.assertEquals("John%", condition.getPropValue());
    }

    @Test
    public void testConstructorEndsWith() {
        Like condition = new Like("email", "%@example.com");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals("%@example.com", condition.getPropValue());
    }

    @Test
    public void testConstructorContains() {
        Like condition = new Like("product_name", "%phone%");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("product_name", condition.getPropName());
        Assertions.assertEquals("%phone%", condition.getPropValue());
    }

    @Test
    public void testConstructorWithUnderscore() {
        Like condition = new Like("word", "A___E");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("word", condition.getPropName());
        Assertions.assertEquals("A___E", condition.getPropValue());
    }

    @Test
    public void testConstructorWithEscapedCharacters() {
        Like condition = new Like("path", "%\\_%");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("path", condition.getPropName());
        Assertions.assertEquals("%\\_%", condition.getPropValue());
    }

    @Test
    public void testGetParameters() {
        Like condition = new Like("title", "The%");
        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("The%", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Like condition = new Like("filename", "%.pdf");
        condition.clearParameters();

        List<Object> params = condition.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertNull(params.get(0));
    }

    @Test
    public void testCopy() {
        Like original = new Like("description", "%important%");
        Like copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
    }

    @Test
    public void testToString() {
        Like condition = new Like("code", "A_B_C");
        String result = condition.toString();

        Assertions.assertTrue(result.contains("code"));
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("A_B_C"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Like condition = new Like("firstName", "J%");
        String result = condition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("FIRST_NAME"));
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("J%"));
    }

    @Test
    public void testHashCode() {
        Like condition1 = new Like("name", "John%");
        Like condition2 = new Like("name", "John%");
        Like condition3 = new Like("name", "Jane%");
        Like condition4 = new Like("title", "John%");

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        Like condition1 = new Like("name", "John%");
        Like condition2 = new Like("name", "John%");
        Like condition3 = new Like("name", "Jane%");
        Like condition4 = new Like("title", "John%");

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testCommonPatterns() {
        // Test various common LIKE patterns
        Like startsWith = new Like("title", "The%");
        Like endsWith = new Like("filename", "%.pdf");
        Like contains = new Like("description", "%important%");
        Like exactLength = new Like("code", "___");
        Like complexPattern = new Like("product_code", "P_%_2023");

        Assertions.assertEquals("The%", startsWith.getPropValue());
        Assertions.assertEquals("%.pdf", endsWith.getPropValue());
        Assertions.assertEquals("%important%", contains.getPropValue());
        Assertions.assertEquals("___", exactLength.getPropValue());
        Assertions.assertEquals("P_%_2023", complexPattern.getPropValue());
    }

    @Test
    public void testEmptyPattern() {
        Like condition = new Like("field", "");

        Assertions.assertEquals("", condition.getPropValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("''"));
    }

    @Test
    public void testNullPattern() {
        Like condition = new Like("field", null);

        Assertions.assertNull(condition.getPropValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testMultipleWildcards() {
        Like condition = new Like("address", "%Street%Apt%");

        Assertions.assertEquals("%Street%Apt%", condition.getPropValue());
        String result = condition.toString();
        Assertions.assertTrue(result.contains("address LIKE '%Street%Apt%'"));
    }

    @Test
    public void testCaseSensitivePattern() {
        // Note: LIKE is typically case-insensitive in many databases,
        // but the pattern itself is preserved as-is
        Like upperCase = new Like("name", "JOHN%");
        Like lowerCase = new Like("name", "john%");
        Like mixedCase = new Like("name", "JoHn%");

        Assertions.assertEquals("JOHN%", upperCase.getPropValue());
        Assertions.assertEquals("john%", lowerCase.getPropValue());
        Assertions.assertEquals("JoHn%", mixedCase.getPropValue());
    }
}