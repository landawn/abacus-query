package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("2025")
class ParserMapperNullArgumentTest {
    @Test
    void internalScannersRetainTheirNonnullSourcePrecondition() {
        // Public entry points validate SQL; these package helpers retain their direct-dereference contract.
        assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null));
        assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null, null));
        assertThrows(NullPointerException.class, () -> SqlParser.isSeparator(null, 0, 0, ' '));
    }

    @Test
    void subscriptScanningDoesNotRequireAnUnusedTokenizer() {
        assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("", null));
        assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("SELECT 1", null));
        assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT arr[?]", null));
        assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT '['", null));
    }

    @Test
    void separatorLookupOnlyDereferencesNullForCaseInsensitiveComparison() {
        final SqlParser.TokenizerConfig punctuation = SqlParser.defaultTokenizerConfig();
        assertFalse(punctuation.isConfiguredSeparator(null, true));
        assertFalse(punctuation.isConfiguredSeparator(null, false));

        final SqlParser.TokenizerConfig words = punctuation.toBuilder().withSeparator("AND").build();
        assertFalse(words.isConfiguredSeparator(null, true));
        assertThrows(NullPointerException.class, () -> words.isConfiguredSeparator(null, false));
    }

    @Test
    void defaultNamedParameterHandlerRetainsStringBuilderNullSemantics() {
        assertThrows(NullPointerException.class, () -> SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(null, "id"));
        assertThrows(NullPointerException.class, () -> SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(null, null));

        final StringBuilder sql = new StringBuilder();
        SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(sql, null);
        assertEquals(":null", sql.toString());
        SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER.accept(sql, "id");
        assertEquals(":null:id", sql.toString());
    }

    @Test
    void publicParserValidationStillRunsBeforeEmptyOrOutOfRangeReturns() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        assertThrows(IllegalArgumentException.class, () -> SqlParser.indexOfToken("", null, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> tokenizer.indexOfToken("", null, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> SqlParser.nextToken(null, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> tokenizer.nextTokenEndIndex(null, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> SqlParser.isFunctionName(null, -1));
    }

    @Test
    void nullableEnumLookupsPreserveTheStandardValueOfContract() {
        assertNull(SqlOperation.of(null));
        assertEquals(SqlOperation.UNKNOWN, SqlOperation.fromOrUnknown(null));
        assertNull(SortDirection.of(null));

        // Compiler-generated valueOf methods retain Enum's required NullPointerException contract.
        assertThrows(NullPointerException.class, () -> SqlOperation.valueOf(null));
        assertThrows(NullPointerException.class, () -> SortDirection.valueOf(null));
        assertThrows(NullPointerException.class, () -> SqlDialect.SqlPolicy.valueOf(null));
        assertThrows(NullPointerException.class, () -> SqlDialect.IdentifierQuote.valueOf(null));
    }
}
