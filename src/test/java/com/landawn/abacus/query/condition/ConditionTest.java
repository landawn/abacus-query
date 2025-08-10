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
import com.landawn.abacus.query.condition.ConditionFactory.CF;
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
        simpleCondition = CF.eq("name", "John");
        complexCondition = CF.and(CF.gt("age", 18), CF.eq("status", "active"));
    }

    // Tests for getOperator() method

    @Test
    void testGetOperatorReturnsCorrectOperator() {
        assertEquals(Operator.EQUAL, simpleCondition.getOperator());
        assertEquals(Operator.AND, complexCondition.getOperator());
    }

    @Test
    void testGetOperatorNeverReturnsNull() {
        assertNotNull(simpleCondition.getOperator());
        assertNotNull(complexCondition.getOperator());

        // Test with various condition types
        assertNotNull(CF.gt("age", 25).getOperator());
        assertNotNull(CF.like("name", "%john%").getOperator());
        assertNotNull(CF.isNull("description").getOperator());
        assertNotNull(CF.in("status", Arrays.asList("active", "pending")).getOperator());
    }

    @Test
    void testGetOperatorConsistency() {
        // Same condition type should always return same operator
        Condition eq1 = CF.eq("field1", "value1");
        Condition eq2 = CF.eq("field2", "value2");
        assertEquals(eq1.getOperator(), eq2.getOperator());

        Condition gt1 = CF.gt("field1", 10);
        Condition gt2 = CF.gt("field2", 20);
        assertEquals(gt1.getOperator(), gt2.getOperator());
    }

    // Tests for and() method

    @Test
    void testAndCreatesAndCondition() {
        Condition other = CF.lt("age", 65);
        Condition result = simpleCondition.and(other);

        assertNotNull(result);
        assertEquals(Operator.AND, result.getOperator());
        assertTrue(result instanceof And);
    }

    @Test
    void testAndReturnsNewInstance() {
        Condition other = CF.ne("status", "inactive");
        Condition result = simpleCondition.and(other);

        assertNotSame(simpleCondition, result);
        assertNotSame(other, result);
    }

    @Test
    void testAndWithSelf() {
        Condition result = simpleCondition.and(simpleCondition);

        assertNotNull(result);
        assertEquals(Operator.AND, result.getOperator());
        assertNotSame(simpleCondition, result);
    }

    @Test
    void testAndChaining() {
        Condition condition1 = CF.eq("field1", "value1");
        Condition condition2 = CF.eq("field2", "value2");
        Condition condition3 = CF.eq("field3", "value3");

        Condition result = condition1.and(condition2).and(condition3);

        assertNotNull(result);
        assertEquals(Operator.AND, result.getOperator());
    }

    // Tests for or() method

    @Test
    void testOrCreatesOrCondition() {
        Condition other = CF.eq("type", "admin");
        Condition result = simpleCondition.or(other);

        assertNotNull(result);
        assertEquals(Operator.OR, result.getOperator());
        assertTrue(result instanceof Or);
    }

    @Test
    void testOrReturnsNewInstance() {
        Condition other = CF.isNull("deleted_at");
        Condition result = simpleCondition.or(other);

        assertNotSame(simpleCondition, result);
        assertNotSame(other, result);
    }

    @Test
    void testOrWithSelf() {
        Condition result = simpleCondition.or(simpleCondition);

        assertNotNull(result);
        assertEquals(Operator.OR, result.getOperator());
        assertNotSame(simpleCondition, result);
    }

    @Test
    void testOrChaining() {
        Condition condition1 = CF.eq("status", "active");
        Condition condition2 = CF.eq("status", "pending");
        Condition condition3 = CF.eq("status", "processing");

        Condition result = condition1.or(condition2).or(condition3);

        assertNotNull(result);
        assertEquals(Operator.OR, result.getOperator());
    }

    @Test
    void testOrWithNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> simpleCondition.or(null));
    }

    // Tests for not() method

    @Test
    void testNotCreatesNotCondition() {
        Condition result = simpleCondition.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
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
        assertEquals(Operator.NOT, doubleNegated.getOperator());
        assertNotSame(negated, doubleNegated);
    }

    @Test
    void testNotWithComplexCondition() {
        Condition result = complexCondition.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    // Tests for copy() method

    @Test
    void testCopyCreatesNewInstance() {
        Condition copy = simpleCondition.copy();

        assertNotNull(copy);
        assertNotSame(simpleCondition, copy);
        assertEquals(simpleCondition.getOperator(), copy.getOperator());
    }

    @Test
    void testCopyPreservesOperator() {
        Condition copy = simpleCondition.copy();
        assertEquals(simpleCondition.getOperator(), copy.getOperator());

        Condition complexCopy = complexCondition.copy();
        assertEquals(complexCondition.getOperator(), complexCopy.getOperator());
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
        Condition complex = CF.and(CF.or(CF.eq("status", "active"), CF.eq("status", "pending")), CF.gt("created_date", "2023-01-01"));

        Condition copy = complex.copy();

        assertNotNull(copy);
        assertNotSame(complex, copy);
        assertEquals(complex.getOperator(), copy.getOperator());
    }

    // Tests for getParameters() method

    @Test
    void testGetParametersNeverReturnsNull() {
        assertNotNull(simpleCondition.getParameters());
        assertNotNull(complexCondition.getParameters());

        // Test with condition that has no parameters
        Condition noParamCondition = CF.isNull("field");
        assertNotNull(noParamCondition.getParameters());
    }

    @Test
    void testGetParametersReturnsCorrectParameters() {
        List<Object> params = simpleCondition.getParameters();
        assertTrue(params.contains("John"));

        // Test with multiple parameters
        Condition betweenCondition = CF.between("age", 18, 65);
        List<Object> betweenParams = betweenCondition.getParameters();
        assertEquals(2, betweenParams.size());
        assertTrue(betweenParams.contains(18));
        assertTrue(betweenParams.contains(65));
    }

    @Test
    void testGetParametersWithInCondition() {
        List<String> values = Arrays.asList("active", "pending", "processing");
        Condition inCondition = CF.in("status", values);
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
        Condition noParamCondition = CF.isNull("field");

        // Should not throw exception even if no parameters to clear
        noParamCondition.clearParameters();
        assertTrue(noParamCondition.getParameters().isEmpty());
    }

    // Tests for toString(NamingPolicy) method

    @Test
    void testToStringWithNamingPolicy() {
        String result = simpleCondition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("name"));
    }

    @Test
    void testToStringWithDifferentNamingPolicies() {
        Condition camelCaseCondition = CF.eq("firstName", "John");

        String snakeCase = camelCaseCondition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        String upperCase = camelCaseCondition.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        String noChange = camelCaseCondition.toString(NamingPolicy.NO_CHANGE);

        assertNotNull(snakeCase);
        assertNotNull(upperCase);
        assertNotNull(noChange);

        // Results should be different based on naming policy
        assertFalse(snakeCase.equals(upperCase));
        assertFalse(snakeCase.equals(noChange));
    }

    @Test
    void testToStringWithNullNamingPolicy() {
        assertThrows(NullPointerException.class, () -> simpleCondition.toString((NamingPolicy) null));
    }

    @Test
    void testToStringWithComplexCondition() {
        String result = complexCondition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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
        Condition condition1 = CF.eq("status", "active");
        Condition condition2 = CF.gt("age", 18);
        Condition condition3 = CF.lt("age", 65);

        // Test complex logical combination
        Condition result = condition1.and(condition2.or(condition3)).not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    void testCopyAndLogicalOperations() {
        Condition copy = simpleCondition.copy();
        Condition combined = copy.and(CF.gt("age", 21));

        assertNotNull(combined);
        assertEquals(Operator.AND, combined.getOperator());

        // Original and copy should be independent
        assertNotSame(simpleCondition, copy);
        assertNotSame(copy, combined);
    }

    @Test
    void testParameterManagementWithLogicalOperations() {
        Condition condition1 = CF.eq("name", "John");
        Condition condition2 = CF.gt("age", 25);
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
        Condition nullCondition = CF.eq("field", null);

        assertNotNull(nullCondition);
        assertEquals(Operator.EQUAL, nullCondition.getOperator());

        List<Object> params = nullCondition.getParameters();
        assertEquals(1, params.size());
        assertNull(params.get(0));
    }

    @Test
    void testConditionWithEmptyStringParameters() {
        Condition emptyCondition = CF.eq("field", "");

        assertNotNull(emptyCondition);
        List<Object> params = emptyCondition.getParameters();
        assertEquals(1, params.size());
        assertEquals("", params.get(0));
    }

    @Test
    void testConditionWithLargeParameterList() {
        // Create condition with many parameters
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Condition inCondition = CF.in("id", largeList);

        List<Object> params = inCondition.getParameters();
        assertEquals(largeList.size(), params.size());

        // Test copy with large parameter list
        Condition copy = inCondition.copy();
        assertEquals(params.size(), copy.getParameters().size());
    }

    @Test
    void testConditionChainPerformance() {
        // Test performance with long condition chains
        Condition chain = CF.eq("field1", "value1");

        for (int i = 2; i <= 100; i++) {
            chain = chain.and(CF.eq("field" + i, "value" + i));
        }

        assertNotNull(chain);
        assertEquals(Operator.AND, chain.getOperator());

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
        Operator originalOperator = simpleCondition.getOperator();

        // Logical operations should not modify original
        simpleCondition.and(CF.eq("other", "value"));
        simpleCondition.or(CF.eq("another", "value"));
        simpleCondition.not();

        // Original should remain unchanged
        assertEquals(originalParams, simpleCondition.getParameters());
        assertEquals(originalOperator, simpleCondition.getOperator());
    }

    @Test
    void testThreadSafetyContract() {
        // Basic thread safety test - conditions should be immutable and thread-safe
        final Condition condition = CF.eq("field", "value");

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                condition.and(CF.gt("age", i));
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                condition.or(CF.lt("score", i));
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
        assertEquals(Operator.EQUAL, condition.getOperator());
    }
}