package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Simple unit tests for DynamicSQLBuilder functionality.
 * Tests basic factory method and builder creation.
 */
public class SimpleDynamicSQLBuilderTest extends TestBase {

    @Test
    void testCreate() {
        DynamicSQLBuilder builder1 = DynamicSQLBuilder.create();
        DynamicSQLBuilder builder2 = DynamicSQLBuilder.create();
        
        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2); // Should return different instances
    }

    @Test
    void testClauseBuilders() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        
        assertNotNull(builder.select());
        assertNotNull(builder.from());
        assertNotNull(builder.where());
        assertNotNull(builder.groupBy());
        assertNotNull(builder.having());
        assertNotNull(builder.orderBy());
    }

    @Test
    void testBasicBuilding() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        
        // Test basic build - should not throw exception
        String sql = builder.build();
        assertNotNull(sql);
    }
}