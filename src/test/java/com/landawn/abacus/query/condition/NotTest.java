package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Filters.CF;

public class NotTest extends TestBase {

    @Test
    public void testConstructorWithLikeCondition() {
        Like likeCondition = CF.like("name", "%test%");
        Not notCondition = CF.not(likeCondition);

        Assertions.assertNotNull(notCondition);
        Assertions.assertEquals(Operator.NOT, notCondition.getOperator());
        Assertions.assertEquals(likeCondition, notCondition.getCondition());
    }

    @Test
    public void testConstructorWithInCondition() {
        In inCondition = CF.in("status", Arrays.asList("active", "pending"));
        Not notIn = CF.not(inCondition);

        Assertions.assertEquals(Operator.NOT, notIn.getOperator());
        Assertions.assertEquals(inCondition, notIn.getCondition());
    }

    @Test
    public void testConstructorWithBetweenCondition() {
        Between between = CF.between("age", 18, 65);
        Not notBetween = CF.not(between);

        Assertions.assertEquals(Operator.NOT, notBetween.getOperator());
        Assertions.assertEquals(between, notBetween.getCondition());
    }

    @Test
    public void testGetCondition() {
        Like likeCondition = CF.like("email", "%@example.com");
        Not notCondition = CF.not(likeCondition);

        Assertions.assertEquals(likeCondition, notCondition.getCondition());
    }

    @Test
    public void testGetOperator() {
        Like likeCondition = CF.like("name", "John%");
        Not notCondition = CF.not(likeCondition);

        Assertions.assertEquals(Operator.NOT, notCondition.getOperator());
    }

    @Test
    public void testGetParameters() {
        In inCondition = CF.in("id", Arrays.asList(1, 2, 3));
        Not notCondition = CF.not(inCondition);

        Assertions.assertEquals(inCondition.getParameters(), notCondition.getParameters());
        Assertions.assertEquals(3, notCondition.getParameters().size());
    }

    @Test
    public void testClearParameters() {
        In inCondition = CF.in("id", Arrays.asList(1, 2, 3));
        Not notCondition = CF.not(inCondition);

        notCondition.clearParameters();
        // Verify the inner condition's parameters are cleared
        Assertions.assertTrue(inCondition.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        Like likeCondition = CF.like("name", "%test%");
        Not notCondition = CF.not(likeCondition);

        String result = notCondition.toString();
        Assertions.assertTrue(result.contains("NOT"));
        Assertions.assertTrue(result.contains("name"));
        Assertions.assertTrue(result.contains("LIKE"));
        Assertions.assertTrue(result.contains("%test%"));
    }

    @Test
    public void testCopy() {
        Between between = CF.between("salary", 30000, 80000);
        Not original = CF.not(between);

        Not copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        Like likeCondition1 = CF.like("name", "%test%");
        Like likeCondition2 = CF.like("name", "%test%");

        Not not1 = CF.not(likeCondition1);
        Not not2 = CF.not(likeCondition2);

        Assertions.assertEquals(not1.hashCode(), not2.hashCode());
    }

    @Test
    public void testEquals() {
        Like likeCondition1 = CF.like("name", "%test%");
        Like likeCondition2 = CF.like("name", "%test%");
        Like likeCondition3 = CF.like("name", "%demo%");

        Not not1 = CF.not(likeCondition1);
        Not not2 = CF.not(likeCondition2);
        Not not3 = CF.not(likeCondition3);

        Assertions.assertTrue(not1.equals(not1));
        Assertions.assertTrue(not1.equals(not2));
        Assertions.assertFalse(not1.equals(not3));
        Assertions.assertFalse(not1.equals(null));
        Assertions.assertFalse(not1.equals("not a Not"));
    }

    @Test
    public void testComplexNestedConditions() {
        // Test NOT with complex nested conditions
        Not notAndCondition = CF.not(CF.and(CF.eq("status", "active"), CF.gt("age", 18), CF.like("email", "%@company.com")));

        Assertions.assertNotNull(notAndCondition);
        Assertions.assertEquals(3, notAndCondition.getParameters().size());
    }

    @Test
    public void testDoubleNegation() {
        Like likeCondition = CF.like("name", "John%");
        Not notCondition = CF.not(likeCondition);
        Not doubleNot = CF.not(notCondition);

        Assertions.assertEquals(notCondition, doubleNot.getCondition());
        Assertions.assertEquals(Operator.NOT, doubleNot.getOperator());
    }
}