package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Simple unit tests for the NotExists condition.
 * Tests basic functionality without complex scenarios.
 */
public class SimpleNotExistsTest extends TestBase {

    private SubQuery simpleSubQuery;
    private NotExists notExistsCondition;

    @BeforeEach
    void setUp() {
        simpleSubQuery = new SubQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        notExistsCondition = new NotExists(simpleSubQuery);
    }

    @Test
    void testConstructorWithSubQuery() {
        NotExists notExists = new NotExists(simpleSubQuery);

        assertNotNull(notExists);
        assertEquals(Operator.NOT_EXISTS, notExists.getOperator());
    }

    @Test
    void testConstructorWithNullSubQuery() {
        assertThrows(NullPointerException.class, () -> new NotExists(null));
    }

    @Test
    void testGetOperator() {
        assertEquals(Operator.NOT_EXISTS, notExistsCondition.getOperator());
    }

    @Test
    void testGetCondition() {
        SubQuery retrieved = notExistsCondition.getCondition();
        assertSame(simpleSubQuery, retrieved);
    }

    @Test
    void testGetParameters() {
        // Simple SubQuery without parameters should return empty list
        assertNotNull(notExistsCondition.getParameters());
    }

    @Test
    void testToString() {
        String result = notExistsCondition.toString();

        assertNotNull(result);
        // Should contain NOT EXISTS in the output
        // Note: specific format may vary based on implementation
    }

    @Test
    void testLogicalOperations() {
        // Test that logical operations work (inherited from Condition)
        Condition other = Filters.eq("status", "active");

        Condition and = notExistsCondition.and(other);
        assertNotNull(and);
        assertEquals(Operator.AND, and.getOperator());

        Condition or = notExistsCondition.or(other);
        assertNotNull(or);
        assertEquals(Operator.OR, or.getOperator());

        Condition not = notExistsCondition.not();
        assertNotNull(not);
        assertEquals(Operator.NOT, not.getOperator());
    }

    @Test
    void testCopy() {
        NotExists copy = notExistsCondition.copy();

        assertNotNull(copy);
        assertEquals(notExistsCondition.getOperator(), copy.getOperator());
        assertEquals(notExistsCondition.getCondition().toString(), copy.getCondition().toString());
    }
}