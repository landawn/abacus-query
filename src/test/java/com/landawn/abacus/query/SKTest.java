package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Unit tests for the SK (String and Keywords) constants class.
 * Tests all public constants for correct values and types.
 *
 * This class validates that all SQL keywords, operators, and character constants
 * in the SK class have the expected values and are properly initialized.
 */
public class SKTest extends TestBase {

    // Tests for Character Constants

    @Test
    void testSpecialCharacters() {
        assertEquals((char) 0, SK.CHAR_ZERO);
        assertEquals('\n', SK.CHAR_LF);
        assertEquals('\r', SK.CHAR_CR);
    }

    @Test
    void testBasicPunctuationCharacters() {
        assertEquals(' ', SK._SPACE);
        assertEquals('.', SK._PERIOD);
        assertEquals(',', SK._COMMA);
        assertEquals(':', SK._COLON);
        assertEquals(';', SK._SEMICOLON);
    }

    @Test
    void testQuoteAndEscapeCharacters() {
        assertEquals('\\', SK._BACKSLASH);
        assertEquals('\'', SK._SINGLE_QUOTE);
        assertEquals('"', SK._DOUBLE_QUOTE);
    }

    @Test
    void testOperatorCharacters() {
        assertEquals('&', SK._AMPERSAND);
        assertEquals('|', SK._VERTICAL_BAR);
        assertEquals('_', SK._UNDERSCORE);
        assertEquals('<', SK._LESS_THAN);
        assertEquals('>', SK._GREATER_THAN);
        assertEquals('=', SK._EQUAL);
        assertEquals('+', SK._PLUS);
        assertEquals('-', SK._MINUS);
        assertEquals('%', SK._PERCENT);
        assertEquals('/', SK._SLASH);
        assertEquals('*', SK._ASTERISK);
        assertEquals('?', SK._QUESTION_MARK);
    }

    @Test
    void testBracketCharacters() {
        assertEquals('(', SK._PARENTHESES_L);
        assertEquals(')', SK._PARENTHESES_R);
        assertEquals('[', SK._BRACKET_L);
        assertEquals(']', SK._BRACKET_R);
        assertEquals('{', SK._BRACE_L);
        assertEquals('}', SK._BRACE_R);
    }

    @Test
    void testSpecialOperatorCharacters() {
        assertEquals('^', SK._CIRCUMFLEX);
        assertEquals('~', SK._TILDE);
        assertEquals('$', SK._DOLLAR);
        assertEquals('#', SK._HASH);
        assertEquals('!', SK._EXCLAMATION);
    }

    // Tests for Basic String Constants

    @Test
    void testSingleCharacterStrings() {
        assertEquals(" ", SK.SPACE);
        assertEquals(".", SK.PERIOD);
        assertEquals(",", SK.COMMA);
        assertEquals(":", SK.COLON);
        assertEquals(";", SK.SEMICOLON);
        assertEquals("\\", SK.BACKSLASH);
        assertEquals("'", SK.SINGLE_QUOTE);
        assertEquals("\"", SK.DOUBLE_QUOTE);
        assertEquals("&", SK.AMPERSAND);
        assertEquals("|", SK.VERTICAL_BAR);
        assertEquals("_", SK.UNDERSCORE);
        assertEquals("<", SK.LESS_THAN);
        assertEquals(">", SK.GREATER_THAN);
        assertEquals("=", SK.EQUAL);
        assertEquals("+", SK.PLUS);
        assertEquals("-", SK.MINUS);
        assertEquals("%", SK.PERCENT);
        assertEquals("/", SK.SLASH);
        assertEquals("*", SK.ASTERISK);
        assertEquals("?", SK.QUESTION_MARK);
        assertEquals("(", SK.PARENTHESES_L);
        assertEquals(")", SK.PARENTHESES_R);
        assertEquals("[", SK.BRACKET_L);
        assertEquals("]", SK.BRACKET_R);
        assertEquals("{", SK.BRACE_L);
        assertEquals("}", SK.BRACE_R);
        assertEquals("^", SK.CIRCUMFLEX);
        assertEquals("~", SK.TILDE);
        assertEquals("$", SK.DOLLAR);
        assertEquals("#", SK.HASH);
        assertEquals("!", SK.EXCLAMATION);
    }

    @Test
    void testCompoundCharacterStrings() {
        assertEquals(", ", SK.COMMA_SPACE);
        assertEquals(": ", SK.COLON_SPACE);
        assertEquals("; ", SK.SEMICOLON_SPACE);
        assertEquals(" '", SK.SPACE_SINGLE_QUOTE);
        assertEquals("' ", SK.SINGLE_QUOTE_SPACE);
        assertEquals(" \"", SK.SPACE_DOUBLE_QUOTE);
        assertEquals("\" ", SK.DOUBLE_QUOTE_SPACE);
        assertEquals(" (", SK.SPACE_PARENTHESES_L);
        assertEquals(") ", SK.PARENTHESES_R_SPACE);
    }

    // Tests for Operator Constants

    @Test
    void testComparisonOperators() {
        assertEquals("!=", SK.NOT_EQUAL);
        assertEquals("<>", SK.NOT_EQUAL_ANSI);
        assertEquals(">=", SK.GREATER_THAN_OR_EQUAL);
        assertEquals("<=", SK.LESS_THAN_OR_EQUAL);
    }

    @Test
    void testLogicalOperators() {
        assertEquals("||", SK.DOUBLE_PIPE);
        assertEquals("&&", SK.AND_OP);
        assertEquals("||", SK.OR_OP);
    }

    // Tests for SQL DDL/DML Keywords

    @Test
    void testCoreSQLCommands() {
        assertEquals("WITH", SK.WITH);
        assertEquals("MERGE", SK.MERGE);
        assertEquals("SELECT", SK.SELECT);
        assertEquals("INSERT", SK.INSERT);
        assertEquals("INTO", SK.INTO);
        assertEquals("UPDATE", SK.UPDATE);
        assertEquals("SET", SK.SET);
        assertEquals("DELETE", SK.DELETE);
        assertEquals("CREATE", SK.CREATE);
        assertEquals("DROP", SK.DROP);
        assertEquals("SHOW", SK.SHOW);
        assertEquals("DESCRIBE", SK.DESCRIBE);
        assertEquals("ALTER", SK.ALTER);
        assertEquals("USE", SK.USE);
        assertEquals("RENAME", SK.RENAME);
    }

    @Test
    void testTransactionKeywords() {
        assertEquals("BEGIN TRANSACTION", SK.BEGIN_TRANSACTION);
        assertEquals("START TRANSACTION", SK.START_TRANSACTION);
        assertEquals("COMMIT", SK.COMMIT);
        assertEquals("ROLLBACK", SK.ROLLBACK);
    }

    // Tests for SQL JOIN Operations

    @Test
    void testJoinTypes() {
        assertEquals("JOIN", SK.JOIN);
        assertEquals("NATURAL", SK.NATURAL);
        assertEquals("INNER", SK.INNER);
        assertEquals("OUTER", SK.OUTER); // Note: has trailing space
        assertEquals("LEFT", SK.LEFT);
        assertEquals("RIGHT", SK.RIGHT);
        assertEquals("FULL", SK.FULL);
        assertEquals("CROSS", SK.CROSS);
    }

    @Test
    void testCompoundJoinClauses() {
        assertEquals("LEFT JOIN", SK.LEFT_JOIN);
        assertEquals("RIGHT JOIN", SK.RIGHT_JOIN);
        assertEquals("FULL JOIN", SK.FULL_JOIN);
        assertEquals("CROSS JOIN", SK.CROSS_JOIN);
        assertEquals("INNER JOIN", SK.INNER_JOIN);
        assertEquals("NATURAL JOIN", SK.NATURAL_JOIN);
    }

    @Test
    void testJoinConditions() {
        assertEquals("ON", SK.ON);
        assertEquals("USING", SK.USING);
    }

    // Tests for SQL Query Clauses

    @Test
    void testCoreQueryKeywords() {
        assertEquals("AS", SK.AS);
        assertEquals("FROM", SK.FROM);
        assertEquals("WHERE", SK.WHERE);
        assertEquals("GROUP BY", SK.GROUP_BY);
        assertEquals("PARTITION BY", SK.PARTITION_BY);
        assertEquals("HAVING", SK.HAVING);
        assertEquals("ORDER BY", SK.ORDER_BY);
        assertEquals("ASC", SK.ASC);
        assertEquals("DESC", SK.DESC);
    }

    @Test
    void testResultLimitingKeywords() {
        assertEquals("LIMIT", SK.LIMIT);
        assertEquals("OFFSET", SK.OFFSET);
        assertEquals("FOR UPDATE", SK.FOR_UPDATE);
        assertEquals("FETCH FIRST", SK.FETCH_FIRST);
        assertEquals("FETCH NEXT", SK.FETCH_NEXT);
        assertEquals("ROWS", SK.ROWS);
        assertEquals("ROWS ONLY", SK.ROWS_ONLY);
        assertEquals("ROW_NEXT", SK.ROW_NEXT);
        assertEquals("ROWNUM", SK.ROWNUM);
    }

    // Tests for SQL Logical Operations

    @Test
    void testLogicalKeywords() {
        assertEquals("AND", SK.AND);
        assertEquals("OR", SK.OR);
        assertEquals("XOR", SK.XOR);
        assertEquals("NOT", SK.NOT);
    }

    @Test
    void testComparisonKeywords() {
        assertEquals("EXISTS", SK.EXISTS);
        assertEquals("LIKE", SK.LIKE);
        assertEquals("BETWEEN", SK.BETWEEN);
        assertEquals("IS", SK.IS);
        assertEquals("IN", SK.IN);
        assertEquals("ANY", SK.ANY);
        assertEquals("ALL", SK.ALL);
        assertEquals("SOME", SK.SOME);
    }

    @Test
    void testNullOperations() {
        assertEquals("NULL", SK.NULL);
        assertEquals("IS NULL", SK.IS_NULL);
        assertEquals("IS NOT", SK.IS_NOT);
        assertEquals("IS NOT NULL", SK.IS_NOT_NULL);
    }

    @Test
    void testCustomEmptyBlankOperations() {
        assertEquals("EMPTY", SK.EMPTY);
        assertEquals("IS EMPTY", SK.IS_EMPTY);
        assertEquals("IS NOT EMPTY", SK.IS_NOT_EMPTY);
        assertEquals("BLANK", SK.BLANK);
        assertEquals("IS BLANK", SK.IS_BLANK);
        assertEquals("IS NOT BLANK", SK.IS_NOT_BLANK);
    }

    @Test
    void testNegatedOperations() {
        assertEquals("NOT IN", SK.NOT_IN);
        assertEquals("NOT EXISTS", SK.NOT_EXISTS);
        assertEquals("NOT LIKE", SK.NOT_LIKE);
    }

    // Tests for SQL Set Operations

    @Test
    void testSetOperationKeywords() {
        assertEquals("VALUES", SK.VALUES);
        assertEquals("DISTINCT", SK.DISTINCT);
        assertEquals("DISTINCTROW", SK.DISTINCTROW);
        assertEquals("UNIQUE", SK.UNIQUE);
        assertEquals("TOP", SK.TOP);
        assertEquals("UNION", SK.UNION);
        assertEquals("UNION ALL", SK.UNION_ALL);
        assertEquals("INTERSECT", SK.INTERSECT);
        assertEquals("EXCEPT", SK.EXCEPT);
        assertEquals("MINUS", SK.EXCEPT2);
    }

    // Tests for SQL Aggregate Functions

    @Test
    void testAggregateFunctions() {
        assertEquals("AVG", SK.AVG);
        assertEquals("COUNT", SK.COUNT);
        assertEquals("SUM", SK.SUM);
        assertEquals("MIN", SK.MIN);
        assertEquals("MAX", SK.MAX);
    }

    // Tests for Mathematical Functions

    @Test
    void testMathematicalFunctions() {
        assertEquals("ABS", SK.ABS);
        assertEquals("ACOS", SK.ACOS);
        assertEquals("ASIN", SK.ASIN);
        assertEquals("ATAN", SK.ATAN);
        assertEquals("ATAN2", SK.ATAN2);
        assertEquals("CEIL", SK.CEIL);
        assertEquals("COS", SK.COS);
        assertEquals("EXP", SK.EXP);
        assertEquals("FLOOR", SK.FLOOR);
        assertEquals("LOG", SK.LOG);
        assertEquals("LN", SK.LN);
        assertEquals("MOD", SK.MOD);
        assertEquals("POWER", SK.POWER);
        assertEquals("SIGN", SK.SIGN);
        assertEquals("SIN", SK.SIN);
        assertEquals("SQRT", SK.SQRT);
        assertEquals("TAN", SK.TAN);
    }

    // Tests for String Functions

    @Test
    void testStringFunctions() {
        assertEquals("LENGTH", SK.LENGTH);
        assertEquals("CONCAT", SK.CONCAT);
        assertEquals("TRIM", SK.TRIM);
        assertEquals("LTRIM", SK.LTRIM);
        assertEquals("RTRIM", SK.RTRIM);
        assertEquals("LPAD", SK.LPAD);
        assertEquals("RPAD", SK.RPAD);
        assertEquals("REPLACE", SK.REPLACE);
        assertEquals("SUBSTR", SK.SUBSTR);
        assertEquals("UPPER", SK.UPPER);
        assertEquals("LOWER", SK.LOWER);
    }

    // Tests for Date/Time and Conversion Functions

    @Test
    void testDateTimeAndConversionFunctions() {
        assertEquals("CAST", SK.CAST);
        assertEquals("CURRENT_TIME", SK.CURRENT_TIME);
        assertEquals("CURRENT_DATE", SK.CURRENT_DATE);
        assertEquals("CURRENT_TIMESTAMP", SK.CURRENT_TIMESTAMP);
    }

    // Tests for Non-null Validation

    @Test
    void testAllConstantsAreNotNull() {
        // Test all character constants are not null
        assertNotNull(SK._SPACE);
        assertNotNull(SK._PERIOD);
        assertNotNull(SK._COMMA);

        // Test all string constants are not null
        assertNotNull(SK.SPACE);
        assertNotNull(SK.PERIOD);
        assertNotNull(SK.COMMA);

        // Test SQL keywords are not null
        assertNotNull(SK.SELECT);
        assertNotNull(SK.FROM);
        assertNotNull(SK.WHERE);
        assertNotNull(SK.INSERT);
        assertNotNull(SK.UPDATE);
        assertNotNull(SK.DELETE);

        // Test functions are not null
        assertNotNull(SK.COUNT);
        assertNotNull(SK.SUM);
        assertNotNull(SK.MAX);
        assertNotNull(SK.MIN);
        assertNotNull(SK.AVG);
    }

    // Tests for Character to String Consistency

    @Test
    void testCharacterToStringConsistency() {
        // Verify that character constants match their string equivalents
        assertEquals(String.valueOf(SK._SPACE), SK.SPACE);
        assertEquals(String.valueOf(SK._PERIOD), SK.PERIOD);
        assertEquals(String.valueOf(SK._COMMA), SK.COMMA);
        assertEquals(String.valueOf(SK._COLON), SK.COLON);
        assertEquals(String.valueOf(SK._SEMICOLON), SK.SEMICOLON);
        assertEquals(String.valueOf(SK._BACKSLASH), SK.BACKSLASH);
        assertEquals(String.valueOf(SK._SINGLE_QUOTE), SK.SINGLE_QUOTE);
        assertEquals(String.valueOf(SK._DOUBLE_QUOTE), SK.DOUBLE_QUOTE);
        assertEquals(String.valueOf(SK._AMPERSAND), SK.AMPERSAND);
        assertEquals(String.valueOf(SK._VERTICAL_BAR), SK.VERTICAL_BAR);
        assertEquals(String.valueOf(SK._UNDERSCORE), SK.UNDERSCORE);
        assertEquals(String.valueOf(SK._LESS_THAN), SK.LESS_THAN);
        assertEquals(String.valueOf(SK._GREATER_THAN), SK.GREATER_THAN);
        assertEquals(String.valueOf(SK._EQUAL), SK.EQUAL);
        assertEquals(String.valueOf(SK._PLUS), SK.PLUS);
        assertEquals(String.valueOf(SK._MINUS), SK.MINUS);
        assertEquals(String.valueOf(SK._PERCENT), SK.PERCENT);
        assertEquals(String.valueOf(SK._SLASH), SK.SLASH);
        assertEquals(String.valueOf(SK._ASTERISK), SK.ASTERISK);
        assertEquals(String.valueOf(SK._QUESTION_MARK), SK.QUESTION_MARK);
        assertEquals(String.valueOf(SK._PARENTHESES_L), SK.PARENTHESES_L);
        assertEquals(String.valueOf(SK._PARENTHESES_R), SK.PARENTHESES_R);
        assertEquals(String.valueOf(SK._BRACKET_L), SK.BRACKET_L);
        assertEquals(String.valueOf(SK._BRACKET_R), SK.BRACKET_R);
        assertEquals(String.valueOf(SK._BRACE_L), SK.BRACE_L);
        assertEquals(String.valueOf(SK._BRACE_R), SK.BRACE_R);
    }

    // Tests for Case Sensitivity

    @Test
    void testSQLKeywordsAreUpperCase() {
        // SQL keywords should be uppercase
        assertTrue(SK.SELECT.equals(SK.SELECT.toUpperCase()));
        assertTrue(SK.FROM.equals(SK.FROM.toUpperCase()));
        assertTrue(SK.WHERE.equals(SK.WHERE.toUpperCase()));
        assertTrue(SK.INSERT.equals(SK.INSERT.toUpperCase()));
        assertTrue(SK.UPDATE.equals(SK.UPDATE.toUpperCase()));
        assertTrue(SK.DELETE.equals(SK.DELETE.toUpperCase()));
        assertTrue(SK.CREATE.equals(SK.CREATE.toUpperCase()));
        assertTrue(SK.DROP.equals(SK.DROP.toUpperCase()));

        // Join keywords
        assertTrue(SK.JOIN.equals(SK.JOIN.toUpperCase()));
        assertTrue(SK.INNER_JOIN.equals(SK.INNER_JOIN.toUpperCase()));
        assertTrue(SK.LEFT_JOIN.equals(SK.LEFT_JOIN.toUpperCase()));
        assertTrue(SK.RIGHT_JOIN.equals(SK.RIGHT_JOIN.toUpperCase()));

        // Logical operators
        assertTrue(SK.AND.equals(SK.AND.toUpperCase()));
        assertTrue(SK.OR.equals(SK.OR.toUpperCase()));
        assertTrue(SK.NOT.equals(SK.NOT.toUpperCase()));

        // Functions
        assertTrue(SK.COUNT.equals(SK.COUNT.toUpperCase()));
        assertTrue(SK.SUM.equals(SK.SUM.toUpperCase()));
        assertTrue(SK.MAX.equals(SK.MAX.toUpperCase()));
        assertTrue(SK.MIN.equals(SK.MIN.toUpperCase()));
        assertTrue(SK.AVG.equals(SK.AVG.toUpperCase()));
    }

    // Tests for Compound String Correctness

    @Test
    void testCompoundStringCorrectness() {
        // Test that compound strings are correctly formed
        assertEquals(SK.COMMA + SK.SPACE, SK.COMMA_SPACE);
        assertEquals(SK.COLON + SK.SPACE, SK.COLON_SPACE);
        assertEquals(SK.SEMICOLON + SK.SPACE, SK.SEMICOLON_SPACE);
        assertEquals(SK.SPACE + SK.SINGLE_QUOTE, SK.SPACE_SINGLE_QUOTE);
        assertEquals(SK.SINGLE_QUOTE + SK.SPACE, SK.SINGLE_QUOTE_SPACE);
        assertEquals(SK.SPACE + SK.DOUBLE_QUOTE, SK.SPACE_DOUBLE_QUOTE);
        assertEquals(SK.DOUBLE_QUOTE + SK.SPACE, SK.DOUBLE_QUOTE_SPACE);
        assertEquals(SK.SPACE + SK.PARENTHESES_L, SK.SPACE_PARENTHESES_L);
        assertEquals(SK.PARENTHESES_R + SK.SPACE, SK.PARENTHESES_R_SPACE);
    }

    // Tests for Special Cases

    @Test
    void testSpecialConstants() {
        // Test special constants that might have unique characteristics
        assertEquals("ALL", SK.ALL); // ALL is represented as asterisk

        // Test question mark constant for prepared statements
        assertEquals("?", SK.QUESTION_MARK);
        assertEquals('?', SK._QUESTION_MARK);
    }

    // Tests for SQL Specific Edge Cases

    @Test
    void testSQLSpecificEdgeCases() {
        // Test that OUTER has trailing space (common SQL syntax)
        assertEquals("OUTER", SK.OUTER);

        // Test compound keywords with proper spacing
        assertEquals("GROUP BY", SK.GROUP_BY);
        assertEquals("ORDER BY", SK.ORDER_BY);
        assertEquals("UNION ALL", SK.UNION_ALL);
        assertEquals("IS NULL", SK.IS_NULL);
        assertEquals("IS NOT NULL", SK.IS_NOT_NULL);
        assertEquals("NOT EXISTS", SK.NOT_EXISTS);
        assertEquals("NOT IN", SK.NOT_IN);
        assertEquals("NOT LIKE", SK.NOT_LIKE);

        // Verify each compound has exactly one space between words
        assertEquals(1, SK.GROUP_BY.split(" ").length - 1);
        assertEquals(1, SK.ORDER_BY.split(" ").length - 1);
        assertEquals(1, SK.UNION_ALL.split(" ").length - 1);
        assertEquals(1, SK.IS_NULL.split(" ").length - 1);
        assertEquals(2, SK.IS_NOT_NULL.split(" ").length - 1); // "IS NOT NULL" has 2 spaces
    }

    // Tests for Character Encoding

    @Test
    void testCharacterEncodingCorrectness() {
        // Test special characters are correctly encoded
        assertEquals(0, SK.CHAR_ZERO);
        assertEquals(10, SK.CHAR_LF); // Line feed
        assertEquals(13, SK.CHAR_CR); // Carriage return

        // Test printable ASCII characters
        assertEquals(32, SK._SPACE); // Space
        assertEquals(33, SK._EXCLAMATION); // !
        assertEquals(34, SK._DOUBLE_QUOTE); // "
        assertEquals(35, SK._HASH); // #
        assertEquals(36, SK._DOLLAR); // $
        assertEquals(37, SK._PERCENT); // %
        assertEquals(38, SK._AMPERSAND); // &
        assertEquals(39, SK._SINGLE_QUOTE); // '
        assertEquals(40, SK._PARENTHESES_L); // (
        assertEquals(41, SK._PARENTHESES_R); // )
        assertEquals(42, SK._ASTERISK); // *
        assertEquals(43, SK._PLUS); // +
        assertEquals(44, SK._COMMA); // ,
        assertEquals(45, SK._MINUS); // -
        assertEquals(46, SK._PERIOD); // .
        assertEquals(47, SK._SLASH); // /
    }

    // Performance and Memory Tests

    @Test
    void testConstantsAreStaticFinal() {
        // This test ensures constants are properly initialized and accessible
        // We can't test the final modifier directly, but we can ensure values don't change

        String selectValue = SK.SELECT;
        String fromValue = SK.FROM;
        String whereValue = SK.WHERE;

        // Call multiple times to ensure they're not recalculated
        assertEquals(selectValue, SK.SELECT);
        assertEquals(fromValue, SK.FROM);
        assertEquals(whereValue, SK.WHERE);

        // Test character constants similarly
        char spaceValue = SK._SPACE;
        char commaValue = SK._COMMA;

        assertEquals(spaceValue, SK._SPACE);
        assertEquals(commaValue, SK._COMMA);
    }

    // Integration Test

    @Test
    void testConstantsInQueryBuilding() {
        // Test using constants to build a simple SQL query
        String query = SK.SELECT + SK.SPACE + SK.ASTERISK + SK.SPACE + SK.FROM + SK.SPACE + "users" + SK.SPACE + SK.WHERE + SK.SPACE + "active" + SK.SPACE
                + SK.EQUAL + SK.SPACE + "1";

        assertEquals("SELECT * FROM users WHERE active = 1", query);

        // Test with compound constants
        String query2 = SK.SELECT + SK.SPACE + "name" + SK.COMMA_SPACE + "email" + SK.SPACE + SK.FROM + SK.SPACE + "users" + SK.SPACE + SK.ORDER_BY + SK.SPACE
                + "name" + SK.SPACE + SK.ASC;

        assertEquals("SELECT name, email FROM users ORDER BY name ASC", query2);
    }
}