package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class LikeTest extends TestBase {

    @Test
    void testAnd() {
        Like cond1 = new Like("a", 1);
        Like cond2 = new Like("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testCaseSensitivePattern() {
        // Note: LIKE is typically case-insensitive in many databases,
        // but the pattern itself is preserved as-is
        Like upperCase = new Like("name", "JOHN%");
        Like lowerCase = new Like("name", "john%");
        Like mixedCase = new Like("name", "JoHn%");

        assertEquals("JOHN%", upperCase.propValue());
        assertEquals("john%", lowerCase.propValue());
        assertEquals("JoHn%", mixedCase.propValue());
    }

    @Test
    void testCommonPatterns() {
        // Test various common LIKE patterns
        Like startsWith = new Like("title", "The%");
        Like endsWith = new Like("filename", "%.pdf");
        Like contains = new Like("description", "%important%");
        Like exactLength = new Like("code", "___");
        Like complexPattern = new Like("product_code", "P_%_2023");

        assertEquals("The%", startsWith.propValue());
        assertEquals("%.pdf", endsWith.propValue());
        assertEquals("%important%", contains.propValue());
        assertEquals("___", exactLength.propValue());
        assertEquals("P_%_2023", complexPattern.propValue());
    }

    @Test
    void testConstructor() {
        Like condition = new Like("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.LIKE, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Like("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Like(null, 25));
    }

    @Test
    void testConstructorContains() {
        Like condition = new Like("product_name", "%phone%");

        assertNotNull(condition);
        assertEquals("product_name", condition.propName());
        assertEquals("%phone%", condition.propValue());
    }

    @Test
    void testConstructorEndsWith() {
        Like condition = new Like("email", "%@example.com");

        assertNotNull(condition);
        assertEquals("email", condition.propName());
        assertEquals("%@example.com", condition.propValue());
    }

    @Test
    void testConstructorStartsWith() {
        Like condition = new Like("name", "John%");

        assertNotNull(condition);
        assertEquals("name", condition.propName());
        assertEquals(Operator.LIKE, condition.operator());
        assertEquals("John%", condition.propValue());
    }

    @Test
    void testConstructorWithEscapedCharacters() {
        Like condition = new Like("path", "%\\_%");

        assertNotNull(condition);
        assertEquals("path", condition.propName());
        assertEquals("%\\_%", condition.propValue());
    }

    @Test
    void testConstructorWithUnderscore() {
        Like condition = new Like("word", "A___E");

        assertNotNull(condition);
        assertEquals("word", condition.propName());
        assertEquals("A___E", condition.propValue());
    }

    @Test
    void testEmptyPattern() {
        Like condition = new Like("field", "");

        assertEquals("", condition.propValue());
        String result = condition.toString();
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("''"));
    }

    @Test
    void testEquals() {
        Like condition1 = new Like("name", "John%");
        Like condition2 = new Like("name", "John%");
        Like condition3 = new Like("name", "Jane%");
        Like condition4 = new Like("title", "John%");

        assertEquals(condition1, condition1);
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, condition4);
        assertNotEquals(condition1, null);
        assertNotEquals(condition1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        Like condition = new Like("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        Like like = new Like("field", "%pattern%");
        NotLike notLike = new NotLike("field", "%pattern%");
        assertNotEquals(like, notLike);
    }

    @Test
    void testEquals_DifferentPropName() {
        Like cond1 = new Like("field1", "value");
        Like cond2 = new Like("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        Like cond1 = new Like("field", "value1");
        Like cond2 = new Like("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        Like cond1 = new Like("status", "active");
        Like cond2 = new Like("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        Like condition = new Like("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        Like condition = new Like("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testGetOperator() {
        Like condition = new Like("field", "value");
        assertEquals(Operator.LIKE, condition.operator());
    }

    @Test
    void testGetPropName() {
        Like condition = new Like("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testHashCode() {
        Like cond1 = new Like("age", 25);
        Like cond2 = new Like("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        Like cond1 = new Like("age", 25);
        Like cond2 = new Like("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testMultipleWildcards() {
        Like condition = new Like("address", "%Street%Apt%");

        assertEquals("%Street%Apt%", condition.propValue());
        String result = condition.toString();
        assertTrue(result.contains("address LIKE '%Street%Apt%'"));
    }

    @Test
    void testNot() {
        Like condition = new Like("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testNullPattern() {
        // `field LIKE null` is no longer renderable: the constructor rejects the null pattern with a pointed message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Like("field", null));
        assertEquals("propValue must not be null for LIKE; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testOr() {
        Like cond1 = new Like("a", 1);
        Like cond2 = new Like("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        Like condition = new Like("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        Like condition = new Like("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPattercountMatchBetweening_Contains() {
        Like condition = new Like("description", "%important%");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("important"));
    }

    @Test
    void testPattercountMatchBetweening_EndsWith() {
        Like condition = new Like("email", "%@example.com");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("@example.com"));
    }

    @Test
    void testPattercountMatchBetweening_StartsWith() {
        Like condition = new Like("name", "John%");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("John%"));
    }

    @Test
    void testPropValueAs() {
        Like condition = new Like("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        Like condition = new Like("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        Like condition = new Like("code", "A_B_C");
        String result = condition.toString();

        assertTrue(result.contains("code"));
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("A_B_C"));
    }

    @Test
    void testToString_NoArgs() {
        Like condition = new Like("name", "%John%");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("John"));
    }

    @Test
    void testToString_NoChange() {
        Like condition = new Like("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        Like condition = new Like("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Like condition = new Like("firstName", "J%");
        String result = condition.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("FIRST_NAME"));
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("J%"));
    }
}
