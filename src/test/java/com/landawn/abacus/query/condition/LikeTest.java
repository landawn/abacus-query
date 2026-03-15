package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;
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
class Like2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Like condition = new Like("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.LIKE, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Like(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Like("", 25));
    }

    @Test
    public void testGetPropName() {
        Like condition = new Like("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Like condition = new Like("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Like condition = new Like("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Like condition = new Like("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Like condition = new Like("field", "value");
        assertEquals(Operator.LIKE, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Like condition = new Like("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        Like condition = new Like("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        Like condition = new Like("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        Like condition = new Like("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        Like cond1 = new Like("age", 25);
        Like cond2 = new Like("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Like cond1 = new Like("age", 25);
        Like cond2 = new Like("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Like condition = new Like("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Like cond1 = new Like("status", "active");
        Like cond2 = new Like("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Like cond1 = new Like("field1", "value");
        Like cond2 = new Like("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Like cond1 = new Like("field", "value1");
        Like cond2 = new Like("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Like condition = new Like("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Like condition = new Like("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Like cond1 = new Like("a", 1);
        Like cond2 = new Like("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Like cond1 = new Like("a", 1);
        Like cond2 = new Like("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Like condition = new Like("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        Like condition = new Like("name", "%John%");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("John"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        Like like = new Like("field", "%pattern%");
        NotLike notLike = new NotLike("field", "%pattern%");
        assertNotEquals(like, notLike);
    }

    @Test
    public void testPattercountMatchBetweening_StartsWith() {
        Like condition = new Like("name", "John%");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("John%"));
    }

    @Test
    public void testPattercountMatchBetweening_EndsWith() {
        Like condition = new Like("email", "%@example.com");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("@example.com"));
    }

    @Test
    public void testPattercountMatchBetweening_Contains() {
        Like condition = new Like("description", "%important%");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("important"));
    }
}

public class LikeTest extends TestBase {

    @Test
    public void testConstructorStartsWith() {
        Like condition = new Like("name", "John%");

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals(Operator.LIKE, condition.operator());
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
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

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
