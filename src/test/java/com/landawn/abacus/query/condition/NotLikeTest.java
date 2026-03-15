package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
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
class NotLike2025Test extends TestBase {

    @Test
    public void testConstructor() {
        NotLike condition = new NotLike("age", 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.NOT_LIKE, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike(null, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new NotLike("", 25));
    }

    @Test
    public void testGetPropName() {
        NotLike condition = new NotLike("userName", "John");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        NotLike condition = new NotLike("age", 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        NotLike condition = new NotLike("name", "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        NotLike condition = new NotLike("field", null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(Operator.NOT_LIKE, condition.operator());
    }

    @Test
    public void testGetParameters() {
        NotLike condition = new NotLike("status", "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        NotLike condition = new NotLike("count", 42);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(42, (int) params.get(0));
    }

    @Test
    public void testClearParameters() {
        NotLike condition = new NotLike("field", "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        NotLike condition = new NotLike("userName", "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotLike condition = new NotLike("userName", "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testHashCode() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        NotLike cond1 = new NotLike("age", 25);
        NotLike cond2 = new NotLike("age", 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotLike condition = new NotLike("field", "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotLike cond1 = new NotLike("status", "active");
        NotLike cond2 = new NotLike("status", "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotLike cond1 = new NotLike("field1", "value");
        NotLike cond2 = new NotLike("field2", "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        NotLike cond1 = new NotLike("field", "value1");
        NotLike cond2 = new NotLike("field", "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        NotLike condition = new NotLike("field", "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotLike cond1 = new NotLike("a", 1);
        NotLike cond2 = new NotLike("b", 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotLike condition = new NotLike("field", "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testToString_NoArgs() {
        NotLike condition = new NotLike("filename", "%.tmp");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("filename"));
        assertTrue(result.contains(".tmp"));
    }

    @Test
    public void testEquals_DifferentOperator() {
        NotLike notLike = new NotLike("field", "%test%");
        Like like = new Like("field", "%test%");
        assertNotEquals(notLike, like);
    }

    @Test
    public void testExcludePattern_StartsWith() {
        NotLike condition = new NotLike("code", "TEST%");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("TEST%"));
    }

    @Test
    public void testExcludePattern_EndsWith() {
        NotLike condition = new NotLike("filename", "%.tmp");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains(".tmp"));
    }

    @Test
    public void testExcludePattern_Contains() {
        NotLike condition = new NotLike("productName", "%temp%");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("temp"));
    }
}

public class NotLikeTest extends TestBase {

    @Test
    public void testConstructor() {
        NotLike notLike = Filters.notLike("name", "John%");

        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("name", notLike.getPropName());
        Assertions.assertEquals("John%", notLike.getPropValue());
        Assertions.assertEquals(Operator.NOT_LIKE, notLike.operator());
    }

    @Test
    public void testConstructorWithWildcardPercent() {
        NotLike notLike = Filters.notLike("email", "%@gmail.com");

        Assertions.assertEquals("email", notLike.getPropName());
        Assertions.assertEquals("%@gmail.com", notLike.getPropValue());
    }

    @Test
    public void testConstructorWithWildcardUnderscore() {
        NotLike notLike = Filters.notLike("code", "___");

        Assertions.assertEquals("code", notLike.getPropName());
        Assertions.assertEquals("___", notLike.getPropValue());
    }

    @Test
    public void testConstructorWithMultipleWildcards() {
        NotLike notLike = Filters.notLike("productName", "%temp%");

        Assertions.assertEquals("productName", notLike.getPropName());
        Assertions.assertEquals("%temp%", notLike.getPropValue());
    }

    @Test
    public void testGetParameters() {
        NotLike notLike = Filters.notLike("filename", "%.tmp");

        Assertions.assertEquals(1, notLike.getParameters().size());
        Assertions.assertEquals("%.tmp", notLike.getParameters().get(0));
    }

    @Test
    public void testClearParameters() {
        NotLike notLike = Filters.notLike("name", "%test%");

        notLike.clearParameters();

        Assertions.assertNull(notLike.getPropValue());
        Assertions.assertTrue(notLike.getParameters().size() == 1);
    }

    @Test
    public void testToString() {
        NotLike notLike = Filters.notLike("description", "%important%");

        String result = notLike.toString();
        Assertions.assertTrue(result.contains("description"));
        Assertions.assertTrue(result.contains("NOT LIKE"));
        Assertions.assertTrue(result.contains("%important%"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        NotLike notLike = Filters.notLike("user_name", "admin%");

        String result = notLike.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("USER_NAME"));
        Assertions.assertTrue(result.contains("NOT LIKE"));
        Assertions.assertTrue(result.contains("admin%"));
    }

    @Test
    public void testHashCode() {
        NotLike notLike1 = Filters.notLike("name", "%test%");
        NotLike notLike2 = Filters.notLike("name", "%test%");
        NotLike notLike3 = Filters.notLike("name", "%demo%");

        Assertions.assertEquals(notLike1.hashCode(), notLike2.hashCode());
        Assertions.assertNotEquals(notLike1.hashCode(), notLike3.hashCode());
    }

    @Test
    public void testEquals() {
        NotLike notLike1 = Filters.notLike("name", "%test%");
        NotLike notLike2 = Filters.notLike("name", "%test%");
        NotLike notLike3 = Filters.notLike("name", "%demo%");
        NotLike notLike4 = Filters.notLike("email", "%test%");

        Assertions.assertTrue(notLike1.equals(notLike1));
        Assertions.assertTrue(notLike1.equals(notLike2));
        Assertions.assertFalse(notLike1.equals(notLike3));
        Assertions.assertFalse(notLike1.equals(notLike4));
        Assertions.assertFalse(notLike1.equals(null));
        Assertions.assertFalse(notLike1.equals("not a NotLike"));
    }

    @Test
    public void testWithNullValue() {
        NotLike notLike = Filters.notLike("name", null);

        Assertions.assertNull(notLike.getPropValue());
        Assertions.assertTrue(notLike.getParameters().size() == 1);
    }

    @Test
    public void testComplexPatterns() {
        // Test various complex patterns
        NotLike pattern1 = Filters.notLike("path", "/temp/%/%.tmp");
        NotLike pattern2 = Filters.notLike("email", "%@%.%");
        NotLike pattern3 = Filters.notLike("code", "A_B_C%");

        Assertions.assertEquals("/temp/%/%.tmp", pattern1.getPropValue());
        Assertions.assertEquals("%@%.%", pattern2.getPropValue());
        Assertions.assertEquals("A_B_C%", pattern3.getPropValue());
    }
}
