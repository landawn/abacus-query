package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Simple unit tests for AbstractQueryBuilder functionality.
 * Tests basic constants and naming policy functionality.
 */
public class SimpleAbstractQueryBuilderTest extends TestBase {

    @BeforeEach
    void setUp() {
        // No setup needed for constant testing
    }

    @Test
    void testPublicConstants() {
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("TOP", AbstractQueryBuilder.TOP);
        assertEquals("UNIQUE", AbstractQueryBuilder.UNIQUE);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("DISTINCTROW", AbstractQueryBuilder.DISTINCTROW);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testConstantsAreNotNull() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.TOP);
        assertNotNull(AbstractQueryBuilder.UNIQUE);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.DISTINCTROW);
        assertNotNull(AbstractQueryBuilder.ASTERISK);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testNamingPolicyEnum() {
        // Test that NamingPolicy enum values exist and are accessible
        assertNotNull(NamingPolicy.NO_CHANGE);
        assertNotNull(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        assertNotNull(NamingPolicy.LOWER_CAMEL_CASE);
    }
}