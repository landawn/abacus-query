package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class NotTest extends TestBase {

    @Test
    public void testConstructorWithLikeCondition() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notCondition = Filters.not(likeCondition);

        Assertions.assertNotNull(notCondition);
        Assertions.assertEquals(Operator.NOT, notCondition.getOperator());
        Assertions.assertEquals(likeCondition, notCondition.getCondition());
    }

    @Test
    public void testConstructorWithInCondition() {
        In inCondition = Filters.in("status", Arrays.asList("active", "pending"));
        Not notIn = Filters.not(inCondition);

        Assertions.assertEquals(Operator.NOT, notIn.getOperator());
        Assertions.assertEquals(inCondition, notIn.getCondition());
    }

    @Test
    public void testConstructorWithBetweenCondition() {
        Between between = Filters.between("age", 18, 65);
        Not notBetween = Filters.not(between);

        Assertions.assertEquals(Operator.NOT, notBetween.getOperator());
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

        Assertions.assertEquals(Operator.NOT, notCondition.getOperator());
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
    public void testCopy() {
        Between between = Filters.between("salary", 30000, 80000);
        Not original = Filters.not(between);

        Not copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
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
        Assertions.assertEquals(Operator.NOT, doubleNot.getOperator());
    }
}