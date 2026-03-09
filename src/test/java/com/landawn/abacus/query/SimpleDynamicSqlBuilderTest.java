package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Simple unit tests for DynamicSqlBuilder functionality.
 * Tests basic factory method and builder creation.
 */
public class SimpleDynamicSqlBuilderTest extends TestBase {

    @Test
    void testCreate() {
        DynamicSqlBuilder builder1 = DynamicSqlBuilder.create();
        DynamicSqlBuilder builder2 = DynamicSqlBuilder.create();

        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2); // Should return different instances
    }

    @Test
    void testClauseBuilders() {
        DynamicSqlBuilder builder = DynamicSqlBuilder.create();

        assertNotNull(builder.select());
        assertNotNull(builder.from());
        assertNotNull(builder.where());
        assertNotNull(builder.groupBy());
        assertNotNull(builder.having());
        assertNotNull(builder.orderBy());
    }

    @Test
    void testBasicBuilding() {
        DynamicSqlBuilder builder = DynamicSqlBuilder.create();

        // Test basic build - should not throw exception
        String sql = builder.build();
        assertNotNull(sql);
    }
}