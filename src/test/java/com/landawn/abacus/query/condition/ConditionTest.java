package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Unit tests for the Condition interface contract.
 * Tests the core behavior and contracts that all Condition implementations should follow.
 * 
 * This test focuses on the interface contract rather than implementation-specific behavior,
 * which is covered in individual implementation test classes.
 */
public class ConditionTest extends TestBase {

    private Condition simpleCondition;
    private Condition complexCondition;

    @BeforeEach
    void setUp() {
        simpleCondition = Filters.eq("name", "John");
        complexCondition = Filters.and(Filters.gt("age", 18), Filters.eq("status", "active"));
    }

    // Tests for operator() method

    @Test
    void testGetOperatorReturnsCorrectOperator() {
        assertEquals(Operator.EQUAL, simpleCondition.operator());
        assertEquals(Operator.AND, complexCondition.operator());
    }

    @Test
    void testGetOperatorNeverReturnsNull() {
        assertNotNull(simpleCondition.operator());
        assertNotNull(complexCondition.operator());

        // Test with various condition types
        assertNotNull(Filters.gt("age", 25).operator());
        assertNotNull(Filters.like("name", "%john%").operator());
        assertNotNull(Filters.isNull("description").operator());
        assertNotNull(Filters.in("status", Arrays.asList("active", "pending")).operator());
    }

    @Test
    void testGetOperatorConsistency() {
        // Same condition type should always return same operator
        Condition eq1 = Filters.eq("field1", "value1");
        Condition eq2 = Filters.eq("field2", "value2");
        assertEquals(eq1.operator(), eq2.operator());

        Condition gt1 = Filters.gt("field1", 10);
        Condition gt2 = Filters.gt("field2", 20);
        assertEquals(gt1.operator(), gt2.operator());
    }

    // Tests for and() method

    @Test
    void testAndCreatesAndCondition() {
        Condition other = Filters.lt("age", 65);
        Condition result = simpleCondition.and(other);

        assertNotNull(result);
        assertEquals(Operator.AND, result.operator());
        assertTrue(result instanceof And);
    }

    @Test
    void testAndReturnsNewInstance() {
        Condition other = Filters.ne("status", "inactive");
        Condition result = simpleCondition.and(other);

        assertNotSame(simpleCondition, result);
        assertNotSame(other, result);
    }

    @Test
    void testAndWithSelf() {
        Condition result = simpleCondition.and(simpleCondition);

        assertNotNull(result);
        assertEquals(Operator.AND, result.operator());
        assertNotSame(simpleCondition, result);
    }

    @Test
    void testAndChaining() {
        Condition condition1 = Filters.eq("field1", "value1");
        Condition condition2 = Filters.eq("field2", "value2");
        Condition condition3 = Filters.eq("field3", "value3");

        Condition result = condition1.and(condition2).and(condition3);

        assertNotNull(result);
        assertEquals(Operator.AND, result.operator());
    }

    // Tests for or() method

    @Test
    void testOrCreatesOrCondition() {
        Condition other = Filters.eq("type", "admin");
        Condition result = simpleCondition.or(other);

        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
        assertTrue(result instanceof Or);
    }

    @Test
    void testOrReturnsNewInstance() {
        Condition other = Filters.isNull("deleted_at");
        Condition result = simpleCondition.or(other);

        assertNotSame(simpleCondition, result);
        assertNotSame(other, result);
    }

    @Test
    void testOrWithSelf() {
        Condition result = simpleCondition.or(simpleCondition);

        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
        assertNotSame(simpleCondition, result);
    }

    @Test
    void testOrChaining() {
        Condition condition1 = Filters.eq("status", "active");
        Condition condition2 = Filters.eq("status", "pending");
        Condition condition3 = Filters.eq("status", "processing");

        Condition result = condition1.or(condition2).or(condition3);

        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
    }

    @Test
    void testOrWithNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> simpleCondition.or(null));
    }

    // Tests for xor() method

    @Test
    void testXorCreatesOrCondition() {
        Condition other = Filters.eq("type", "admin");
        Condition result = simpleCondition.xor(other);

        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
        assertTrue(result instanceof Or);
    }

    @Test
    void testXorReturnsNewInstance() {
        Condition other = Filters.eq("type", "admin");
        Condition result = simpleCondition.xor(other);

        assertNotSame(simpleCondition, result);
        assertNotSame(other, result);
    }

    @Test
    void testXorWithNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> simpleCondition.xor(null));
    }

    @Test
    void testXorSemantics() {
        // XOR should produce (A AND NOT B) OR (NOT A AND B)
        Condition a = Filters.eq("x", 1);
        Condition b = Filters.eq("y", 2);
        Or result = a.xor(b);

        // The result should be an Or with 2 conditions (each an And)
        assertEquals(2, result.getConditions().size());
        assertTrue(result.getConditions().get(0) instanceof And);
        assertTrue(result.getConditions().get(1) instanceof And);
    }

    // Tests for not() method

    @Test
    void testNotCreatesNotCondition() {
        Condition result = simpleCondition.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        assertTrue(result instanceof Not);
    }

    @Test
    void testNotReturnsNewInstance() {
        Condition result = simpleCondition.not();
        assertNotSame(simpleCondition, result);
    }

    @Test
    void testDoubleNegation() {
        Condition negated = simpleCondition.not();
        Condition doubleNegated = negated.not();

        assertNotNull(doubleNegated);
        assertEquals(Operator.NOT, doubleNegated.operator());
        assertNotSame(negated, doubleNegated);
    }

    @Test
    void testNotWithComplexCondition() {
        Condition result = complexCondition.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    // Tests for copy() method

    @Test
    void testCopyCreatesNewInstance() {
        Condition copy = simpleCondition.copy();

        assertNotNull(copy);
        assertNotSame(simpleCondition, copy);
        assertEquals(simpleCondition.operator(), copy.operator());
    }

    @Test
    void testCopyPreservesOperator() {
        Condition copy = simpleCondition.copy();
        assertEquals(simpleCondition.operator(), copy.operator());

        Condition complexCopy = complexCondition.copy();
        assertEquals(complexCondition.operator(), complexCopy.operator());
    }

    @Test
    void testCopyPreservesParameters() {
        List<Object> originalParams = simpleCondition.getParameters();
        Condition copy = simpleCondition.copy();
        List<Object> copyParams = copy.getParameters();

        assertEquals(originalParams, copyParams);

        // Verify it's a deep copy by checking independence
        assertNotSame(originalParams, copyParams);
    }

    @Test
    void testCopyIndependence() {
        Condition copy = simpleCondition.copy();

        // Modify original shouldn't affect copy
        copy.clearParameters();

        // Original should still have parameters
        assertFalse(simpleCondition.getParameters().isEmpty());
    }

    @Test
    void testCopyWithComplexConditions() {
        // Test copying conditions with nested structures
        Condition complex = Filters.and(Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending")), Filters.gt("created_date", "2023-01-01"));

        Condition copy = complex.copy();

        assertNotNull(copy);
        assertNotSame(complex, copy);
        assertEquals(complex.operator(), copy.operator());
    }

    // Tests for getParameters() method

    @Test
    void testGetParametersNeverReturnsNull() {
        assertNotNull(simpleCondition.getParameters());
        assertNotNull(complexCondition.getParameters());

        // Test with condition that has no parameters
        Condition noParamCondition = Filters.isNull("field");
        assertNotNull(noParamCondition.getParameters());
    }

    @Test
    void testGetParametersReturnsCorrectParameters() {
        List<Object> params = simpleCondition.getParameters();
        assertTrue(params.contains("John"));

        // Test with multiple parameters
        Condition betweenCondition = Filters.between("age", 18, 65);
        List<Object> betweenParams = betweenCondition.getParameters();
        assertEquals(2, betweenParams.size());
        assertTrue(betweenParams.contains(18));
        assertTrue(betweenParams.contains(65));
    }

    @Test
    void testGetParametersWithInCondition() {
        List<String> values = Arrays.asList("active", "pending", "processing");
        Condition inCondition = Filters.in("status", values);
        List<Object> params = inCondition.getParameters();

        assertEquals(values.size(), params.size());
        for (String value : values) {
            assertTrue(params.contains(value));
        }
    }

    @Test
    void testGetParametersWithComplexCondition() {
        List<Object> params = complexCondition.getParameters();

        // Complex condition should contain parameters from all sub-conditions
        assertTrue(params.contains(18));
        assertTrue(params.contains("active"));
    }

    @Test
    void testGetParametersReturnsImmutableList() {
        List<Object> params = simpleCondition.getParameters();

        // Should not be able to modify the returned list
        assertThrows(UnsupportedOperationException.class, () -> params.add("newValue"));
        assertThrows(UnsupportedOperationException.class, () -> params.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> params.clear());
    }

    @Test
    void testClearParametersWithNoParameters() {
        Condition noParamCondition = Filters.isNull("field");

        // Should not throw exception even if no parameters to clear
        noParamCondition.clearParameters();
        assertTrue(noParamCondition.getParameters().isEmpty());
    }

    // Tests for toString(NamingPolicy) method

    @Test
    void testToStringWithNamingPolicy() {
        String result = simpleCondition.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("name"));
    }

    @Test
    void testToStringWithDifferentNamingPolicies() {
        Condition camelCaseCondition = Filters.eq("firstName", "John");

        String snakeCase = camelCaseCondition.toString(NamingPolicy.SNAKE_CASE);
        String upperCase = camelCaseCondition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        String noChange = camelCaseCondition.toString(NamingPolicy.NO_CHANGE);

        assertNotNull(snakeCase);
        assertNotNull(upperCase);
        assertNotNull(noChange);

        // Results should be different based on naming policy
        assertFalse(snakeCase.equals(upperCase));
        assertFalse(snakeCase.equals(noChange));
    }

    @Test
    void testToStringWithComplexCondition() {
        String result = complexCondition.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Should contain elements from all sub-conditions
        assertTrue(result.contains("age"));
        assertTrue(result.contains("status"));
        assertTrue(result.contains("AND"));
    }

    // Integration Tests

    @Test
    void testLogicalOperationsCombination() {
        Condition condition1 = Filters.eq("status", "active");
        Condition condition2 = Filters.gt("age", 18);
        Condition condition3 = Filters.lt("age", 65);

        // Test complex logical combination
        Condition result = condition1.and(condition2.or(condition3)).not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testCopyAndLogicalOperations() {
        Condition copy = simpleCondition.copy();
        Condition combined = copy.and(Filters.gt("age", 21));

        assertNotNull(combined);
        assertEquals(Operator.AND, combined.operator());

        // Original and copy should be independent
        assertNotSame(simpleCondition, copy);
        assertNotSame(copy, combined);
    }

    @Test
    void testParameterManagementWithLogicalOperations() {
        Condition condition1 = Filters.eq("name", "John");
        Condition condition2 = Filters.gt("age", 25);
        Condition combined = condition1.and(condition2);

        List<Object> params = combined.getParameters();
        assertEquals(2, params.size());
        assertTrue(params.contains("John"));
        assertTrue(params.contains(25));

        // Clear parameters
        combined.clearParameters();
        assertTrue(combined.getParameters().stream().allMatch(param -> param == null));
    }

    // Edge Cases and Error Conditions

    @Test
    void testConditionWithNullParameters() {
        Condition nullCondition = Filters.eq("field", null);

        assertNotNull(nullCondition);
        assertEquals(Operator.EQUAL, nullCondition.operator());

        List<Object> params = nullCondition.getParameters();
        assertEquals(1, params.size());
        assertNull(params.get(0));
    }

    @Test
    void testConditionWithEmptyStringParameters() {
        Condition emptyCondition = Filters.eq("field", "");

        assertNotNull(emptyCondition);
        List<Object> params = emptyCondition.getParameters();
        assertEquals(1, params.size());
        assertEquals("", params.get(0));
    }

    @Test
    void testConditionWithLargeParameterList() {
        // Create condition with many parameters
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Condition inCondition = Filters.in("id", largeList);

        List<Object> params = inCondition.getParameters();
        assertEquals(largeList.size(), params.size());

        // Test copy with large parameter list
        Condition copy = inCondition.copy();
        assertEquals(params.size(), copy.getParameters().size());
    }

    @Test
    void testConditionChainPerformance() {
        // Test performance with long condition chains
        Condition chain = Filters.eq("field1", "value1");

        for (int i = 2; i <= 100; i++) {
            chain = chain.and(Filters.eq("field" + i, "value" + i));
        }

        assertNotNull(chain);
        assertEquals(Operator.AND, chain.operator());

        // Should still be able to get parameters and copy
        List<Object> params = chain.getParameters();
        assertEquals(100, params.size());

        Condition copy = chain.copy();
        assertEquals(chain.getParameters().size(), copy.getParameters().size());
    }

    // Contract Validation Tests

    @Test
    void testImmutabilityContract() {
        // Test that conditions are immutable (except for clearParameters)
        List<Object> originalParams = simpleCondition.getParameters();
        Operator originalOperator = simpleCondition.operator();

        // Logical operations should not modify original
        simpleCondition.and(Filters.eq("other", "value"));
        simpleCondition.or(Filters.eq("another", "value"));
        simpleCondition.not();

        // Original should remain unchanged
        assertEquals(originalParams, simpleCondition.getParameters());
        assertEquals(originalOperator, simpleCondition.operator());
    }

    @Test
    void testThreadSafetyContract() {
        // Basic thread safety test - conditions should be immutable and thread-safe
        final Condition condition = Filters.eq("field", "value");

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                condition.and(Filters.gt("age", i));
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                condition.or(Filters.lt("score", i));
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Original condition should remain unchanged
        assertNotNull(condition.getParameters());
        assertEquals(Operator.EQUAL, condition.operator());
    }
}