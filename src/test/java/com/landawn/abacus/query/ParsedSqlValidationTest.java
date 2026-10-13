package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ParsedSqlValidationTest extends TestBase {

    @Nested
    class Validation {
        @Test
        void testInternalScannersRetainTheirNonnullSourcePrecondition() {
            // Public entry points validate SQL; these package helpers retain their direct-dereference contract.
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null, null));
        }

        @Test
        void testSubscriptScanningDoesNotRequireAnUnusedTokenizer() {
            assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("", null));
            assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("SELECT 1", null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT arr[?]", null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT '['", null));
        }
    }
}
