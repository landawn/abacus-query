package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class Not2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Equal innerCondition = new Equal("status", "active");
        Not condition = new Not(innerCondition);

        assertEquals(Operator.NOT, condition.operator());
        assertSame(innerCondition, condition.getCondition());
    }

    @Test
    public void testConstructor_NullCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Not(null));
    }

    @Test
    public void testGetCondition() {
        Equal innerCondition = new Equal("age", 25);
        Not condition = new Not(innerCondition);

        Equal retrieved = condition.getCondition();
        assertSame(innerCondition, retrieved);
    }

    @Test
    public void testGetCondition_ComplexCondition() {
        And innerAnd = new And(new Equal("a", 1), new Equal("b", 2));
        Not condition = new Not(innerAnd);

        And retrieved = condition.getCondition();
        assertSame(innerAnd, retrieved);
    }

    @Test
    public void testGetOperator() {
        Not condition = new Not(new Equal("field", "value"));
        assertEquals(Operator.NOT, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Equal innerCondition = new Equal("name", "John");
        Not condition = new Not(innerCondition);

        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("John", params.get(0));
    }

    @Test
    public void testGetParameters_ComplexCondition() {
        Between between = new Between("age", 18, 65);
        Not condition = new Not(between);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    public void testClearParameters() {
        Equal innerCondition = new Equal("field", "value");
        Not condition = new Not(innerCondition);

        condition.clearParameters();
        assertNull(innerCondition.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Equal innerCondition = new Equal("userName", "Bob");
        Not condition = new Not(innerCondition);

        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("userName"));
    }

    @Test
    public void testToString_SnakeCase() {
        Equal innerCondition = new Equal("firstName", "Charlie");
        Not condition = new Not(innerCondition);

        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("first_name"));
    }

    @Test
    public void testHashCode() {
        Equal inner = new Equal("field", "value");
        Not cond1 = new Not(inner);
        Not cond2 = new Not(new Equal("field", "value"));

        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Not condition = new Not(new Equal("a", 1));
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("a", 1));
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentInnerConditions() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("b", 2));
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(condition, "string");
    }

    @Test
    public void testNestedCondition() {
        And innerAnd = new And(new Equal("status", "active"), new GreaterThan("age", 18));
        Not condition = new Not(innerAnd);

        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testToString_NoArgs() {
        Not condition = new Not(new Equal("status", "active"));
        String result = condition.toString();

        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("status"));
    }

    @Test
    public void testGetParameters_NullCondition() {
        Not condition = new Not(new Equal("field", null));
        List<Object> params = condition.getParameters();

        assertEquals(1, (int) params.size());
        assertNull(params.get(0));
    }

}

public class NotTest extends TestBase {

    @Test
    public void testConstructorWithLikeCondition() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notCondition = Filters.not(likeCondition);

        Assertions.assertNotNull(notCondition);
        Assertions.assertEquals(Operator.NOT, notCondition.operator());
        Assertions.assertEquals(likeCondition, notCondition.getCondition());
    }

    @Test
    public void testConstructorWithInCondition() {
        In inCondition = Filters.in("status", Arrays.asList("active", "pending"));
        Not notIn = Filters.not(inCondition);

        Assertions.assertEquals(Operator.NOT, notIn.operator());
        Assertions.assertEquals(inCondition, notIn.getCondition());
    }

    @Test
    public void testConstructorWithBetweenCondition() {
        Between between = Filters.between("age", 18, 65);
        Not notBetween = Filters.not(between);

        Assertions.assertEquals(Operator.NOT, notBetween.operator());
        Assertions.assertEquals(between, notBetween.getCondition());
    }

    @Test
    public void testGetCondition() {
        Like likeCondition = Filters.like("email", "%@example.com");
        Not notCondition = Filters.not(likeCondition);

        Assertions.assertEquals(likeCondition, notCondition.getCondition());
    }

    @Test
    public void testGetOperator() {
        Like likeCondition = Filters.like("name", "John%");
        Not notCondition = Filters.not(likeCondition);

        Assertions.assertEquals(Operator.NOT, notCondition.operator());
    }

    @Test
    public void testGetParameters() {
        In inCondition = Filters.in("id", Arrays.asList(1, 2, 3));
        Not notCondition = Filters.not(inCondition);

        Assertions.assertEquals(inCondition.getParameters(), notCondition.getParameters());
        Assertions.assertEquals(3, notCondition.getParameters().size());
    }

    @Test
    public void testClearParameters() {
        In inCondition = Filters.in("id", Arrays.asList(1, 2, 3));
        Not notCondition = Filters.not(inCondition);

        notCondition.clearParameters();
        // Verify the inner condition's parameters are cleared
        Assertions.assertTrue(inCondition.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notCondition = Filters.not(likeCondition);

        String result = notCondition.toString();
        Assertions.assertTrue(result.contains("NOT"));
        Assertions.assertTrue(result.contains("name"));
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("%test%"));
    }

    @Test
    public void testHashCode() {
        Like likeCondition1 = Filters.like("name", "%test%");
        Like likeCondition2 = Filters.like("name", "%test%");

        Not not1 = Filters.not(likeCondition1);
        Not not2 = Filters.not(likeCondition2);

        Assertions.assertEquals(not1.hashCode(), not2.hashCode());
    }

    @Test
    public void testEquals() {
        Like likeCondition1 = Filters.like("name", "%test%");
        Like likeCondition2 = Filters.like("name", "%test%");
        Like likeCondition3 = Filters.like("name", "%demo%");

        Not not1 = Filters.not(likeCondition1);
        Not not2 = Filters.not(likeCondition2);
        Not not3 = Filters.not(likeCondition3);

        Assertions.assertTrue(not1.equals(not1));
        Assertions.assertTrue(not1.equals(not2));
        Assertions.assertFalse(not1.equals(not3));
        Assertions.assertFalse(not1.equals(null));
        Assertions.assertFalse(not1.equals("not a Not"));
    }

    @Test
    public void testComplexNestedConditions() {
        // Test NOT with complex nested conditions
        Not notAndCondition = Filters.not(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.like("email", "%@company.com")));

        Assertions.assertNotNull(notAndCondition);
        Assertions.assertEquals(3, notAndCondition.getParameters().size());
    }

    @Test
    public void testDoubleNegation() {
        Like likeCondition = Filters.like("name", "John%");
        Not notCondition = Filters.not(likeCondition);
        Not doubleNot = Filters.not(notCondition);

        Assertions.assertEquals(notCondition, doubleNot.getCondition());
        Assertions.assertEquals(Operator.NOT, doubleNot.operator());
    }
}
