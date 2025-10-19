/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SQLParser2025Test extends TestBase {

    @Test
    public void testParseSimpleSelect() {
        String sql = "SELECT * FROM users";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
    }

    @Test
    public void testParseSelectWithColumns() {
        String sql = "SELECT id, name, email FROM users";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("id"));
        assertTrue(words.contains(","));
        assertTrue(words.contains("name"));
        assertTrue(words.contains("email"));
    }

    @Test
    public void testParseWithWhere() {
        String sql = "SELECT * FROM users WHERE age > 18";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains("age"));
        assertTrue(words.contains(">"));
        assertTrue(words.contains("18"));
    }

    @Test
    public void testParseWithJoin() {
        String sql = "SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("LEFT"));
        assertTrue(words.contains("JOIN"));
        assertTrue(words.contains("ON"));
    }

    @Test
    public void testParseWithOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND status = 'active'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains(">="));
        assertTrue(words.contains("AND"));
        assertTrue(words.contains("="));
    }

    @Test
    public void testParseWithQuotedString() {
        String sql = "SELECT * FROM users WHERE name = 'John Doe'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.stream().anyMatch(w -> w.contains("John Doe")));
    }

    @Test
    public void testParseWithDoubleQuotes() {
        String sql = "SELECT \"first name\" FROM users";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.stream().anyMatch(w -> w.contains("first name")));
    }

    @Test
    public void testParseWithParentheses() {
        String sql = "SELECT * FROM users WHERE (age > 18 AND status = 'active')";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("("));
        assertTrue(words.contains(")"));
    }

    @Test
    public void testParseWithGroupBy() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("GROUP"));
        assertTrue(words.contains("BY"));
    }

    @Test
    public void testParseWithOrderBy() {
        String sql = "SELECT * FROM users ORDER BY name ASC";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("ORDER"));
        assertTrue(words.contains("BY"));
    }

    @Test
    public void testParseWithMultipleOperators() {
        String sql = "SELECT * FROM users WHERE age >= 18 AND age <= 65";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains(">="));
        assertTrue(words.contains("<="));
    }

    @Test
    public void testParseWithInClause() {
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("IN"));
        assertTrue(words.contains("("));
        assertTrue(words.contains(","));
    }

    @Test
    public void testIndexWord() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SQLParser.indexWord(sql, "WHERE", 0, false);
        assertTrue(index >= 0);
        assertEquals("WHERE", sql.substring(index, index + 5));
    }

    @Test
    public void testIndexWordCaseInsensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SQLParser.indexWord(sql, "where", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexWordCaseSensitive() {
        String sql = "SELECT * FROM users WHERE age > 18";
        int index = SQLParser.indexWord(sql, "where", 0, true);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexWordNotFound() {
        String sql = "SELECT * FROM users";
        int index = SQLParser.indexWord(sql, "WHERE", 0, false);
        assertEquals(-1, index);
    }

    @Test
    public void testIndexWordComposite() {
        String sql = "SELECT * FROM users ORDER BY name";
        int index = SQLParser.indexWord(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0);
    }

    @Test
    public void testIndexWordFromPosition() {
        String sql = "SELECT * FROM users WHERE age > 18 AND status = 'active'";
        int firstAnd = SQLParser.indexWord(sql, "AND", 0, false);
        assertTrue(firstAnd > 0);
    }

    @Test
    public void testNextWord() {
        String sql = "SELECT * FROM users";
        String word = SQLParser.nextWord(sql, 7);
        assertNotNull(word);
        assertEquals("*", word);
    }

    @Test
    public void testNextWordSkipsWhitespace() {
        String sql = "SELECT    *    FROM users";
        String word = SQLParser.nextWord(sql, 6);
        assertNotNull(word);
        assertEquals("*", word);
    }

    @Test
    public void testNextWordEmptyString() {
        String sql = "SELECT *";
        String word = SQLParser.nextWord(sql, sql.length());
        assertEquals("", word);
    }

    @Test
    public void testNextWordWithOperator() {
        String sql = "WHERE age >= 18";
        String word = SQLParser.nextWord(sql, 10);
        assertNotNull(word);
        assertTrue(word.equals(">=") || word.equals("18"));
    }

    @Test
    public void testRegisterSeparatorChar() {
        SQLParser.registerSeparator('$');
        // Test that it doesn't throw
    }

    @Test
    public void testRegisterSeparatorString() {
        SQLParser.registerSeparator(":::");
        // Test that it doesn't throw
    }

    @Test
    public void testRegisterSeparatorNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            SQLParser.registerSeparator((String) null);
        });
    }

    @Test
    public void testIsSeparator() {
        String sql = "SELECT * FROM users";
        assertTrue(SQLParser.isSeparator(sql, sql.length(), 6, ' '));
        assertTrue(SQLParser.isSeparator(sql, sql.length(), 7, '*'));
        assertFalse(SQLParser.isSeparator(sql, sql.length(), 0, 'S'));
    }

    @Test
    public void testIsSeparatorHash() {
        String sql = "SELECT * FROM users WHERE id = #{userId}";
        assertTrue(SQLParser.isSeparator(sql, sql.length(), 35, '#'));
    }

    @Test
    public void testIsFunctionName() {
        List<String> words = SQLParser.parse("SELECT COUNT(*) FROM users");
        int countIndex = -1;
        for (int i = 0; i < words.size(); i++) {
            if ("COUNT".equals(words.get(i))) {
                countIndex = i;
                break;
            }
        }
        assertTrue(countIndex >= 0);
        assertTrue(SQLParser.isFunctionName(words, words.size(), countIndex));
    }

    @Test
    public void testIsFunctionNameNotFunction() {
        List<String> words = SQLParser.parse("SELECT name FROM users");
        int nameIndex = -1;
        for (int i = 0; i < words.size(); i++) {
            if ("name".equals(words.get(i))) {
                nameIndex = i;
                break;
            }
        }
        assertTrue(nameIndex >= 0);
        assertFalse(SQLParser.isFunctionName(words, words.size(), nameIndex));
    }

    @Test
    public void testParseEmptyString() {
        String sql = "";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.isEmpty());
    }

    @Test
    public void testParseWithHaving() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("HAVING"));
    }

    @Test
    public void testParseWithSubquery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("IN"));
    }

    @Test
    public void testParseWithUnion() {
        String sql = "SELECT id FROM users UNION SELECT id FROM accounts";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("UNION"));
    }

    @Test
    public void testParseWithUnionAll() {
        String sql = "SELECT id FROM users UNION ALL SELECT id FROM accounts";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("UNION"));
        assertTrue(words.contains("ALL"));
    }

    @Test
    public void testParseWithNotEquals() {
        String sql = "SELECT * FROM users WHERE status != 'deleted'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("!="));
    }

    @Test
    public void testParseWithNotEqualsAlternative() {
        String sql = "SELECT * FROM users WHERE status <> 'deleted'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("<>"));
    }

    @Test
    public void testParseWithBetween() {
        String sql = "SELECT * FROM users WHERE age BETWEEN 18 AND 65";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("BETWEEN"));
        assertTrue(words.contains("AND"));
    }

    @Test
    public void testParseWithLike() {
        String sql = "SELECT * FROM users WHERE name LIKE 'John%'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("LIKE"));
    }

    @Test
    public void testParseWithNotLike() {
        String sql = "SELECT * FROM users WHERE name NOT LIKE 'John%'";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("NOT"));
        assertTrue(words.contains("LIKE"));
    }

    @Test
    public void testParseWithForUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("FOR"));
        assertTrue(words.contains("UPDATE"));
    }

    @Test
    public void testParseComplexQuery() {
        String sql = "SELECT u.id, u.name, COUNT(o.id) as order_count " + "FROM users u " + "LEFT JOIN orders o ON u.id = o.user_id "
                + "WHERE u.status = 'active' AND u.created_date > '2020-01-01' " + "GROUP BY u.id, u.name " + "HAVING COUNT(o.id) > 5 "
                + "ORDER BY order_count DESC " + "LIMIT 10";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.size() > 10);
    }

    @Test
    public void testIndexWordMultipleOccurrences() {
        String sql = "SELECT name FROM users WHERE name = 'John' OR name = 'Jane'";
        int firstIndex = SQLParser.indexWord(sql, "name", 0, false);
        int secondIndex = SQLParser.indexWord(sql, "name", firstIndex + 1, false);
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex > firstIndex);
    }

    @Test
    public void testNextWordAtEnd() {
        String sql = "SELECT * FROM users";
        String word = SQLParser.nextWord(sql, sql.length() - 1);
        assertNotNull(word);
    }

    @Test
    public void testParseWithArithmeticOperators() {
        String sql = "SELECT price * quantity + tax - discount FROM orders";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("*"));
        assertTrue(words.contains("+"));
        assertTrue(words.contains("-"));
    }

    @Test
    public void testParseWithModuloOperator() {
        String sql = "SELECT id % 10 FROM users";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("%"));
    }

    @Test
    public void testParseWithBitwiseOperators() {
        String sql = "SELECT value & mask FROM data";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertTrue(words.contains("&"));
    }
}
