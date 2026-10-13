package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SqlDialectTest extends TestBase {

    @Nested
    class Validation {
        @Test
        void testDefaultNamedParameterHandlerRetainsStringBuilderNullSemantics() {
            assertThrows(NullPointerException.class, () -> SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(null, "id"));
            assertThrows(NullPointerException.class, () -> SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(null, null));

            final StringBuilder sql = new StringBuilder();
            SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(sql, null);
            assertEquals(":null", sql.toString());
            SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(sql, "id");
            assertEquals(":null:id", sql.toString());
        }

        @Test
        void testNullableEnumLookupsPreserveTheStandardValueOfContract() {

            // Compiler-generated valueOf methods retain Enum's required NullPointerException contract.
            assertThrows(NullPointerException.class, () -> SqlDialect.SqlPolicy.valueOf(null));
            assertThrows(NullPointerException.class, () -> SqlDialect.IdentifierQuote.valueOf(null));
        }
    }
}
