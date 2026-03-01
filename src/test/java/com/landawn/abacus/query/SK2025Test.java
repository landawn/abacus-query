/*
 * Copyright (C) 2025 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SK2025Test extends TestBase {

    @Test
    public void testSpecialCharacters() {
        assertEquals((char) 0, SK.CHAR_ZERO);
        assertEquals('\n', SK.CHAR_LF);
        assertEquals('\r', SK.CHAR_CR);
        assertEquals(' ', SK._SPACE);
        assertEquals(" ", SK.SPACE);
        assertEquals('.', SK._PERIOD);
        assertEquals(".", SK.PERIOD);
        assertEquals(',', SK._COMMA);
        assertEquals(",", SK.COMMA);
        assertEquals(", ", SK.COMMA_SPACE);
        assertEquals(':', SK._COLON);
        assertEquals(":", SK.COLON);
        assertEquals(": ", SK.COLON_SPACE);
        assertEquals(';', SK._SEMICOLON);
        assertEquals(";", SK.SEMICOLON);
        assertEquals("; ", SK.SEMICOLON_SPACE);
    }

    @Test
    public void testBackslashAndQuotes() {
        assertEquals('\\', SK._BACKSLASH);
        assertEquals("\\", SK.BACKSLASH);
        assertEquals('\'', SK._SINGLE_QUOTE);
        assertEquals("'", SK.SINGLE_QUOTE);
        assertEquals(" '", SK.SPACE_SINGLE_QUOTE);
        assertEquals("' ", SK.SINGLE_QUOTE_SPACE);
        assertEquals('"', SK._DOUBLE_QUOTE);
        assertEquals("\"", SK.DOUBLE_QUOTE);
        assertEquals(" \"", SK.SPACE_DOUBLE_QUOTE);
        assertEquals("\" ", SK.DOUBLE_QUOTE_SPACE);
    }

    @Test
    public void testOperatorCharacters() {
        assertEquals('&', SK._AMPERSAND);
        assertEquals("&", SK.AMPERSAND);
        assertEquals('|', SK._VERTICAL_BAR);
        assertEquals("|", SK.VERTICAL_BAR);
        assertEquals("||", SK.DOUBLE_PIPE);
        assertEquals('_', SK._UNDERSCORE);
        assertEquals("_", SK.UNDERSCORE);
        assertEquals('<', SK._LESS_THAN);
        assertEquals("<", SK.LESS_THAN);
        assertEquals('>', SK._GREATER_THAN);
        assertEquals(">", SK.GREATER_THAN);
        assertEquals('=', SK._EQUAL);
        assertEquals("=", SK.EQUAL);
        assertEquals('+', SK._PLUS);
        assertEquals("+", SK.PLUS);
        assertEquals('-', SK._MINUS);
        assertEquals("-", SK.MINUS);
        assertEquals('%', SK._PERCENT);
        assertEquals("%", SK.PERCENT);
        assertEquals('/', SK._SLASH);
        assertEquals("/", SK.SLASH);
        assertEquals('*', SK._ASTERISK);
        assertEquals("*", SK.ASTERISK);
    }

    @Test
    public void testComparisonOperators() {
        assertEquals("!=", SK.NOT_EQUAL);
        assertEquals("<>", SK.NOT_EQUAL_ANSI);
        assertEquals(">=", SK.GREATER_THAN_OR_EQUAL);
        assertEquals("<=", SK.LESS_THAN_OR_EQUAL);
    }

    @Test
    public void testBrackets() {
        assertEquals('?', SK._QUESTION_MARK);
        assertEquals("?", SK.QUESTION_MARK);
        assertEquals('(', SK._PARENTHESIS_L);
        assertEquals("(", SK.PARENTHESIS_L);
        assertEquals(" (", SK.SPACE_PARENTHESIS_L);
        assertEquals(')', SK._PARENTHESIS_R);
        assertEquals(")", SK.PARENTHESIS_R);
        assertEquals(") ", SK.PARENTHESIS_R_SPACE);
        assertEquals('[', SK._BRACKET_L);
        assertEquals("[", SK.BRACKET_L);
        assertEquals(']', SK._BRACKET_R);
        assertEquals("]", SK.BRACKET_R);
        assertEquals('{', SK._BRACE_L);
        assertEquals("{", SK.BRACE_L);
        assertEquals('}', SK._BRACE_R);
        assertEquals("}", SK.BRACE_R);
    }

    @Test
    public void testMiscCharacters() {
        assertEquals('^', SK._CIRCUMFLEX);
        assertEquals("^", SK.CIRCUMFLEX);
        assertEquals('~', SK._TILDE);
        assertEquals("~", SK.TILDE);
        assertEquals('$', SK._DOLLAR);
        assertEquals("$", SK.DOLLAR);
        assertEquals('#', SK._HASH);
        assertEquals("#", SK.HASH);
        assertEquals('!', SK._EXCLAMATION);
        assertEquals("!", SK.EXCLAMATION);
    }

    @Test
    public void testSQLKeywords_DML() {
        assertEquals("WITH", SK.WITH);
        assertEquals("MERGE", SK.MERGE);
        assertEquals("SELECT", SK.SELECT);
        assertEquals("INSERT", SK.INSERT);
        assertEquals("INTO", SK.INTO);
        assertEquals("UPDATE", SK.UPDATE);
        assertEquals("SET", SK.SET);
        assertEquals("DELETE", SK.DELETE);
    }

    @Test
    public void testSQLKeywords_DDL() {
        assertEquals("CREATE", SK.CREATE);
        assertEquals("DROP", SK.DROP);
        assertEquals("SHOW", SK.SHOW);
        assertEquals("DESCRIBE", SK.DESCRIBE);
        assertEquals("ALTER", SK.ALTER);
        assertEquals("USE", SK.USE);
        assertEquals("RENAME", SK.RENAME);
    }

    @Test
    public void testSQLKeywords_Transaction() {
        assertEquals("BEGIN TRANSACTION", SK.BEGIN_TRANSACTION);
        assertEquals("START TRANSACTION", SK.START_TRANSACTION);
        assertEquals("COMMIT", SK.COMMIT);
        assertEquals("ROLLBACK", SK.ROLLBACK);
    }

    @Test
    public void testSQLKeywords_Join() {
        assertEquals("AS", SK.AS);
        assertEquals("JOIN", SK.JOIN);
        assertEquals("NATURAL", SK.NATURAL);
        assertEquals("INNER", SK.INNER);
        assertEquals("OUTER", SK.OUTER);
        assertEquals("LEFT JOIN", SK.LEFT_JOIN);
        assertEquals("LEFT", SK.LEFT);
        assertEquals("RIGHT JOIN", SK.RIGHT_JOIN);
        assertEquals("RIGHT", SK.RIGHT);
        assertEquals("FULL JOIN", SK.FULL_JOIN);
        assertEquals("FULL", SK.FULL);
        assertEquals("CROSS JOIN", SK.CROSS_JOIN);
        assertEquals("INNER JOIN", SK.INNER_JOIN);
        assertEquals("NATURAL JOIN", SK.NATURAL_JOIN);
        assertEquals("CROSS", SK.CROSS);
        assertEquals("ON", SK.ON);
        assertEquals("USING", SK.USING);
    }

    @Test
    public void testSQLKeywords_Clauses() {
        assertEquals("WHERE", SK.WHERE);
        assertEquals("GROUP BY", SK.GROUP_BY);
        assertEquals("PARTITION BY", SK.PARTITION_BY);
        assertEquals("HAVING", SK.HAVING);
        assertEquals("ORDER BY", SK.ORDER_BY);
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

    @Test
    public void testSQLKeywords_Conditions() {
        assertEquals("EXISTS", SK.EXISTS);
        assertEquals("LIKE", SK.LIKE);
        assertEquals("AND", SK.AND);
        assertEquals("&&", SK.AND_OP);
        assertEquals("OR", SK.OR);
        assertEquals("||", SK.OR_OP);
        assertEquals("XOR", SK.XOR);
        assertEquals("NOT", SK.NOT);
        assertEquals("BETWEEN", SK.BETWEEN);
        assertEquals("IS", SK.IS);
        assertEquals("IS NOT", SK.IS_NOT);
        assertEquals("NULL", SK.NULL);
        assertEquals("IS NULL", SK.IS_NULL);
        assertEquals("IS NOT NULL", SK.IS_NOT_NULL);
        assertEquals("EMPTY", SK.EMPTY);
        assertEquals("IS EMPTY", SK.IS_EMPTY);
        assertEquals("IS NOT EMPTY", SK.IS_NOT_EMPTY);
        assertEquals("BLANK", SK.BLANK);
        assertEquals("IS BLANK", SK.IS_BLANK);
        assertEquals("IS NOT BLANK", SK.IS_NOT_BLANK);
        assertEquals("NOT IN", SK.NOT_IN);
        assertEquals("NOT EXISTS", SK.NOT_EXISTS);
        assertEquals("NOT LIKE", SK.NOT_LIKE);
    }

    @Test
    public void testSQLKeywords_Misc() {
        assertEquals("FROM", SK.FROM);
        assertEquals("ASC", SK.ASC);
        assertEquals("DESC", SK.DESC);
        assertEquals("VALUES", SK.VALUES);
        assertEquals("DISTINCT", SK.DISTINCT);
        assertEquals("DISTINCTROW", SK.DISTINCTROW);
        assertEquals("UNIQUE", SK.UNIQUE);
        assertEquals("TOP", SK.TOP);
        assertEquals("IN", SK.IN);
        assertEquals("ANY", SK.ANY);
        assertEquals("ALL", SK.ALL);
        assertEquals("SOME", SK.SOME);
        assertEquals("UNION", SK.UNION);
        assertEquals("UNION ALL", SK.UNION_ALL);
        assertEquals("INTERSECT", SK.INTERSECT);
        assertEquals("EXCEPT", SK.EXCEPT);
        assertEquals("MINUS", SK.EXCEPT_MINUS);
    }

    @Test
    public void testSQLFunctions_Aggregate() {
        assertEquals("AVG", SK.AVG);
        assertEquals("COUNT", SK.COUNT);
        assertEquals("SUM", SK.SUM);
        assertEquals("MIN", SK.MIN);
        assertEquals("MAX", SK.MAX);
    }

    @Test
    public void testSQLFunctions_Mathematical() {
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

    @Test
    public void testSQLFunctions_String() {
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

    @Test
    public void testSQLFunctions_DateTime() {
        assertEquals("CAST", SK.CAST);
        assertEquals("CURRENT_TIME", SK.CURRENT_TIME);
        assertEquals("CURRENT_DATE", SK.CURRENT_DATE);
        assertEquals("CURRENT_TIMESTAMP", SK.CURRENT_TIMESTAMP);
    }

    @Test
    public void testConstantsNotNull() {
        assertNotNull(SK.SPACE);
        assertNotNull(SK.SELECT);
        assertNotNull(SK.FROM);
        assertNotNull(SK.WHERE);
        assertNotNull(SK.ORDER_BY);
        assertNotNull(SK.GROUP_BY);
        assertNotNull(SK.HAVING);
        assertNotNull(SK.JOIN);
        assertNotNull(SK.LEFT_JOIN);
        assertNotNull(SK.RIGHT_JOIN);
        assertNotNull(SK.INNER_JOIN);
        assertNotNull(SK.COMMA);
        assertNotNull(SK.PERIOD);
        assertNotNull(SK.EQUAL);
        assertNotNull(SK.AND);
        assertNotNull(SK.OR);
        assertNotNull(SK.NULL);
    }
}
