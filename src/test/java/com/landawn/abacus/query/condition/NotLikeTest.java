package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotLikeTest extends TestBase {

    @Test
    void testAnd() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testComplexPatterns() {
        // Test various complex patterns
        NotLike pattern1 = Filters.notLike("path", "/temp/%/%.tmp");
        NotLike pattern2 = Filters.notLike("email", "%@%.%");
        NotLike pattern3 = Filters.notLike("code", "A_B_C%");

        assertEquals("/temp/%/%.tmp", pattern1.propValue());
        assertEquals("%@%.%", pattern2.propValue());
        assertEquals("A_B_C%", pattern3.propValue());
    }

    @Test
    void testConstructor() {
        NotLike condition = new NotLike("age", 25);
        assertEquals("age", condition.propName());
        assertEquals(25, (int) condition.propValue());
        assertEquals(Operator.NOT_LIKE, condition.operator());
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike("", 25));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike(null, 25));
    }

    @Test
    void testConstructorWithMultipleWildcards() {
        NotLike notLike = Filters.notLike("productName", "%temp%");

        assertEquals("productName", notLike.propName());
        assertEquals("%temp%", notLike.propValue());
    }

    @Test
    void testConstructorWithWildcardPercent() {
        NotLike notLike = Filters.notLike("email", "%@gmail.com");

        assertEquals("email", notLike.propName());
        assertEquals("%@gmail.com", notLike.propValue());
    }

    @Test
    void testConstructorWithWildcardUnderscore() {
        NotLike notLike = Filters.notLike("code", "___");

        assertEquals("code", notLike.propName());
        assertEquals("___", notLike.propValue());
    }

    @Test
    void testEquals() {
        NotLike notLike1 = Filters.notLike("name", "%test%");
        NotLike notLike2 = Filters.notLike("name", "%test%");
        NotLike notLike3 = Filters.notLike("name", "%demo%");
        NotLike notLike4 = Filters.notLike("email", "%test%");

        assertTrue(notLike1.equals(notLike1));
        assertTrue(notLike1.equals(notLike2));
        assertFalse(notLike1.equals(notLike3));
        assertFalse(notLike1.equals(notLike4));
        assertFalse(notLike1.equals(null));
        assertFalse(notLike1.equals("not a NotLike"));
    }

    @Test
    void testEquals_DifferentClass() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentOperator() {
        NotLike notLike = new NotLike("field", "%test%");
        Like like = new Like("field", "%test%");
        assertNotEquals(notLike, like);
    }

    @Test
    void testEquals_DifferentPropName() {
        NotLike cond1 = new NotLike("field1", "value");
        NotLike cond2 = new NotLike("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropValue() {
        NotLike cond1 = new NotLike("field", "value1");
        NotLike cond2 = new NotLike("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        NotLike cond1 = new NotLike("status", "active");
        NotLike cond2 = new NotLike("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    void testExcludePattern_Contains() {
        NotLike condition = new NotLike("productName", "%temp%");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("temp"));
    }

    @Test
    void testExcludePattern_EndsWith() {
        NotLike condition = new NotLike("filename", "%.tmp");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains(".tmp"));
    }

    @Test
    void testExcludePattern_StartsWith() {
        NotLike condition = new NotLike("code", "TEST%");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("TEST%"));
    }

    @Test
    void testGetOperator() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(Operator.NOT_LIKE, condition.operator());
    }

    @Test
    void testGetPropName() {
        NotLike condition = new NotLike("userName", "John");
        assertEquals("userName", condition.propName());
    }

    @Test
    void testGetPropValue_Null() {
        // A null pattern is rejected at construction: `x NOT LIKE NULL` can never be true under three-valued logic.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new NotLike("field", null));
        assertEquals("propValue must not be null for NOT LIKE; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }

    @Test
    void testHashCode() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        NotLike condition = new NotLike("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        NotLike condition = new NotLike("status", "active");
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        NotLike condition = new NotLike("count", 42);
        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    void testPropValueAs() {
        NotLike condition = new NotLike("age", 30);
        Integer value = condition.propValueAs(Integer.class);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    void testPropValueAs_String() {
        NotLike condition = new NotLike("name", "Alice");
        String value = condition.propValueAs(String.class);
        assertEquals("Alice", value);
    }

    @Test
    void testToString() {
        NotLike notLike = Filters.notLike("description", "%important%");

        String result = notLike.toString();
        assertTrue(result.contains("description"));
        assertTrue(result.contains("NOT LIKE"));
        assertTrue(result.contains("%important%"));
    }

    @Test
    void testToString_NoArgs() {
        NotLike condition = new NotLike("filename", "%.tmp");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("filename"));
        assertTrue(result.contains(".tmp"));
    }

    @Test
    void testToString_NoChange() {
        NotLike condition = new NotLike("userName", "Alice");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testToString_SnakeCase() {
        NotLike condition = new NotLike("userName", "Bob");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        NotLike notLike = Filters.notLike("user_name", "admin%");

        String result = notLike.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("USER_NAME"));
        assertTrue(result.contains("NOT LIKE"));
        assertTrue(result.contains("admin%"));
    }

    @Test
    void testWithNullValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Filters.notLike("name", null));
        assertEquals("propValue must not be null for NOT LIKE; use an IS NULL/IS NOT NULL condition instead", ex.getMessage());
    }
}
