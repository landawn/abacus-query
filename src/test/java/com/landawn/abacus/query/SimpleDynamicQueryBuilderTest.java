package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.DynamicQuery.Builder;

/**
 * Simple unit tests for Builder functionality.
 * Tests basic factory method and builder creation.
 */
public class SimpleDynamicQueryBuilderTest extends TestBase {

    @Test
    void testCreate() {
        Builder builder1 = DynamicQuery.builder();
        Builder builder2 = DynamicQuery.builder();

        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2); // Should return different instances
    }

    @Test
    void testClauseBuilders() {
        Builder builder = DynamicQuery.builder();

        assertNotNull(builder.select());
        assertNotNull(builder.from());
        assertNotNull(builder.where());
        assertNotNull(builder.groupBy());
        assertNotNull(builder.having());
        assertNotNull(builder.orderBy());
    }

    @Test
    void testBasicBuilding() {
        Builder builder = DynamicQuery.builder();

        // Test basic build - should not throw exception
        String sql = builder.build();
        assertNotNull(sql);
    }
}