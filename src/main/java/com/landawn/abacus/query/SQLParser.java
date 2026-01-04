/*
 * Copyright (C) 2015 HaiYang Li
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;

/**
 * A utility class for parsing SQL statements into individual words and tokens.
 * This parser recognizes SQL keywords, operators, identifiers, and various SQL-specific
 * separators while preserving the structure and meaning of the SQL statement.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String sql = "SELECT * FROM users WHERE age > 25 ORDER BY name";
 * List<String> words = SQLParser.parse(sql);
 * // Result: ["SELECT", " ", "*", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">", " ", "25", " ", "ORDER", " ", "BY", " ", "name"]
 * }</pre>
 * 
 */
public final class SQLParser {

    private static final String KEEP_COMMENTS = "-- Keep comments";

    private static final char TAB = '\t';

    private static final char ENTER = '\n';

    private static final char ENTER_2 = '\r';

    private static final Set<Object> separators = N.newHashSet();

    static {
        separators.add(TAB);
        separators.add(ENTER);
        separators.add(ENTER_2);
        separators.add(' ');
        separators.add('?');
        separators.add(',');
        separators.add('~');
        separators.add('!');
        separators.add('@');
        separators.add('^');
        separators.add('#');
        separators.add("!!");
        separators.add(';');
        separators.add('(');
        separators.add(')');
        separators.add('=');
        separators.add("==");
        separators.add(":=");
        separators.add("^=");
        separators.add("~=");
        separators.add("+=");
        separators.add("-=");
        separators.add("*=");
        separators.add("/=");
        separators.add("%=");
        separators.add("&=");
        separators.add("|=");
        separators.add("!=");
        separators.add("<>");
        separators.add("!<");
        separators.add("!>");
        separators.add('>');
        separators.add(">>");
        separators.add(">=");
        separators.add("@>");
        separators.add("&>");
        separators.add(">^");
        separators.add('<');
        separators.add("<<");
        separators.add("<=");
        separators.add("<@");
        separators.add("&<");
        separators.add("<^");
        separators.add('+');
        separators.add('-');
        separators.add('%');
        separators.add('/');
        separators.add('*');
        separators.add('&');
        separators.add("&&");
        separators.add('|');
        separators.add("||");
        separators.add("|/");
        separators.add("||/");
        separators.add("->");
        separators.add("##");
        separators.add("@@");
        separators.add("@-@");
        separators.add("@@@");
        separators.add("->>");
        separators.add("<->");
        separators.add("<=>");
        separators.add(">>=");
        separators.add("<<=");
        separators.add("<<|");
        separators.add("&<|");
        separators.add("|&>");
        separators.add("|>>");
        separators.add("(+)");
        separators.add("?#");
        separators.add("?-");
        separators.add("?|");
        separators.add("?-|");
        separators.add("?||");
        separators.add("~*");
        separators.add("!~");
        separators.add("!~*");
        separators.add("^-=");
        separators.add("|*=");
    }

    private static final Map<String, String[]> compositeWords = new ObjectPool<>(64);

    static {
        compositeWords.put(SK.LEFT_JOIN, new String[] { "LEFT", "JOIN" });
        compositeWords.put(SK.RIGHT_JOIN, new String[] { "RIGHT", "JOIN" });
        compositeWords.put(SK.FULL_JOIN, new String[] { "FULL", "JOIN" });
        compositeWords.put(SK.CROSS_JOIN, new String[] { "CROSS", "JOIN" });
        compositeWords.put(SK.NATURAL_JOIN, new String[] { "NATURAL", "JOIN" });
        compositeWords.put(SK.INNER_JOIN, new String[] { "INNER", "JOIN" });
        compositeWords.put(SK.GROUP_BY, new String[] { "GROUP", "BY" });
        compositeWords.put(SK.ORDER_BY, new String[] { "ORDER", "BY" });
        compositeWords.put(SK.FOR_UPDATE, new String[] { "FOR", "UPDATE" });
        compositeWords.put(SK.FETCH_FIRST, new String[] { "FETCH", "FIRST" });
        compositeWords.put(SK.FETCH_NEXT, new String[] { "FETCH", "NEXT" });
        compositeWords.put(SK.ROWS_ONLY, new String[] { "ROWS", "ONLY" });
        compositeWords.put(SK.UNION_ALL, new String[] { "UNION", "ALL" });
        compositeWords.put(SK.IS_NOT, new String[] { "IS", "NOT" });
        compositeWords.put(SK.IS_NULL, new String[] { "IS", "NULL" });
        compositeWords.put(SK.IS_NOT_NULL, new String[] { "IS", "NOT", "NULL" });
        compositeWords.put(SK.IS_EMPTY, new String[] { "IS", "EMPTY" });
        compositeWords.put(SK.IS_NOT_EMPTY, new String[] { "IS", "NOT", "EMPTY" });
        compositeWords.put(SK.IS_BLANK, new String[] { "IS", "BLANK" });
        compositeWords.put(SK.IS_NOT_BLANK, new String[] { "IS", "NOT", "BLANK" });
        compositeWords.put(SK.NOT_IN, new String[] { "NOT", "IN" });
        compositeWords.put(SK.NOT_EXISTS, new String[] { "NOT", "EXISTS" });
        compositeWords.put(SK.NOT_LIKE, new String[] { "NOT", "LIKE" });
        compositeWords.put(SK.PARTITION_BY, new String[] { "PARTITION", "BY" });

        final List<String> list = new ArrayList<>(compositeWords.keySet());

        for (String e : list) {
            e = e.toLowerCase();

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, Splitter.with(SK.SPACE).trimResults().splitToArray(e));
            }

            e = e.toUpperCase();

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, Splitter.with(SK.SPACE).trimResults().splitToArray(e));
            }
        }
    }

    private SQLParser() {
    }

    /**
     * Parses a SQL statement into a list of individual words and tokens.
     * This method tokenizes the SQL string while preserving the semantic meaning
     * of SQL constructs such as keywords, operators, identifiers, and literals.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = SQLParser.parse("SELECT name, age FROM users WHERE age >= 18");
     * // Result: ["SELECT", " ", "name", ",", " ", "age", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">=", " ", "18"]
     * }</pre>
     * 
     * @param sql the SQL statement to parse
     * @return a list of tokens representing the parsed SQL statement
     */
    public static List<String> parse(final String sql) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();
        final List<String> words = new ArrayList<>();

        String temp = "";
        char quoteChar = 0;
        int keepComments = -1;

        for (int index = 0; index < sqlLength; index++) {
            // TODO [performance improvement]. will it improve performance if
            // change to char array?
            // char c = sqlCharArray[charIndex];
            char c = sql.charAt(index);

            if (quoteChar != 0) {
                // is it in a quoted identifier?
                sb.append(c);

                // end in quote.
                if (c == quoteChar && (index == 0 || sql.charAt(index - 1) != '\\')) {
                    words.add(sb.toString());
                    sb.setLength(0);

                    quoteChar = 0;
                }
            } else if (c == '-' && index < sqlLength - 1 && sql.charAt(index + 1) == '-') {
                if (!sb.isEmpty()) {
                    words.add(sb.toString());
                    sb.setLength(0);
                }

                //    if (keepComments == -1) {
                //        keepComments = Strings.startsWithIgnoreCase(sql, "-- Keep comments") ? 1 : 0;
                //    }
                //
                //    if (keepComments == 1) {
                //        sb.append(c);
                //
                //        while (++index < sqlLength) {
                //            c = sql.charAt(index);
                //            sb.append(c);
                //
                //            if (c == ENTER || c == ENTER_2) {
                //                final String tmp = sb.toString();
                //
                //                if (!Strings.startsWithIgnoreCase(tmp, "-- Keep comments")) {
                //                    words.add(sb.toString());
                //                }
                //
                //                sb.setLength(0);
                //
                //                break;
                //            }
                //        }
                //    } else {
                //        while (++index < sqlLength) {
                //            c = sql.charAt(index);
                //
                //            if (c == ENTER || c == ENTER_2) {
                //                break;
                //            }
                //        }
                //    }

                while (++index < sqlLength) {
                    c = sql.charAt(index);

                    if (c == ENTER || c == ENTER_2) {
                        break;
                    }
                }
            } else if (c == '/' && index < sqlLength - 1 && sql.charAt(index + 1) == '*') {
                if (!sb.isEmpty()) {
                    words.add(sb.toString());
                    sb.setLength(0);
                }

                if (keepComments == -1) {
                    keepComments = Strings.startsWithIgnoreCase(sql, KEEP_COMMENTS) ? 1 : 0;
                }

                if (keepComments == 1) {
                    sb.append(c);

                    while (++index < sqlLength) {
                        c = sql.charAt(index);
                        sb.append(c);

                        if (c == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                            sb.append(sql.charAt(++index));

                            words.add(sb.toString());
                            sb.setLength(0);

                            break;
                        }
                    }
                } else {
                    while (++index < sqlLength) {
                        c = sql.charAt(index);

                        if (c == '*' && index < sqlLength - 1 && sql.charAt(index + 1) == '/') {
                            index++;
                            break;
                        }
                    }
                }
            } else if (isSeparator(sql, sqlLength, index, c)) {
                if (!sb.isEmpty()) {
                    words.add(sb.toString());
                    sb.setLength(0);
                }

                if ((index < (sqlLength - 2)) && separators.contains(temp = sql.substring(index, index + 3))) {
                    words.add(temp);
                    index += 2;
                } else if ((index < (sqlLength - 1)) && separators.contains(temp = sql.substring(index, index + 2))) {
                    words.add(temp);
                    index += 1;
                } else if (c == SK._SPACE || c == TAB || c == ENTER || c == ENTER_2) {
                    if (!words.isEmpty() && !words.get(words.size() - 1).equals(SK.SPACE)) {
                        words.add(SK.SPACE);
                    }
                } else {
                    words.add(String.valueOf(c));
                }
            } else {
                sb.append(c);

                if ((c == SK._QUOTATION_S) || (c == SK._QUOTATION_D)) {
                    quoteChar = c;
                }
            }
        }

        if (!sb.isEmpty()) {
            words.add(sb.toString());
            sb.setLength(0);
        }

        Objectory.recycle(sb);

        return words;
    }

    /**
     * Finds the index of a specific word within a SQL statement starting from a given position.
     * This method is capable of finding both simple words and composite keywords (like "LEFT JOIN").
     * It respects SQL syntax rules, including quoted identifiers and case sensitivity options.
     * 
     * <p>The method handles:</p>
     * <ul>
     *   <li>Simple words and operators</li>
     *   <li>Composite keywords (e.g., "GROUP BY", "LEFT JOIN")</li>
     *   <li>Case-sensitive and case-insensitive matching</li>
     *   <li>Quoted identifiers (preserving exact matches within quotes)</li>
     *   <li>Multi-character operators</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
     * int index = SQLParser.indexWord(sql, "ORDER BY", 0, false);
     * // Returns: 40 (the position where "ORDER BY" starts)
     *
     * int whereIndex = SQLParser.indexWord(sql, "WHERE", 0, false);
     * // Returns: 20 (the position where "WHERE" starts)
     * }</pre>
     * 
     * @param sql the SQL statement to search within
     * @param word the word or composite keyword to find
     * @param fromIndex the starting position for the search (0-based)
     * @param caseSensitive whether the search should be case-sensitive
     * @return the index of the word if found, or -1 if not found
     */
    public static int indexWord(final String sql, final String word, final int fromIndex, final boolean caseSensitive) {
        String[] subWords = compositeWords.get(word);

        if (subWords == null) {
            subWords = Splitter.with(SK.SPACE).trimResults().splitToArray(word);
            compositeWords.put(word, subWords);
        }

        //noinspection IfStatementWithIdenticalBranches
        if ((subWords == null) || (subWords.length <= 1)) {
            int result = N.INDEX_NOT_FOUND;

            final StringBuilder sb = Objectory.createStringBuilder();
            final int sqlLength = sql.length();
            String temp = "";
            char quoteChar = 0;

            for (int index = fromIndex; index < sqlLength; index++) {
                final char c = sql.charAt(index);

                // is it in a quoted identifier?
                if (quoteChar != 0) {
                    sb.append(c);

                    // end in quote.
                    if (c == quoteChar) {
                        temp = sb.toString();

                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index - word.length() + 1;

                            break;
                        }

                        sb.setLength(0);
                        quoteChar = 0;
                    }
                } else if (isSeparator(sql, sqlLength, index, c)) {
                    if (!sb.isEmpty()) {
                        temp = sb.toString();

                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index - word.length();

                            break;
                        }

                        sb.setLength(0);
                    } else if (c == SK._SPACE || c == TAB || c == ENTER || c == ENTER_2) {
                        // skip white char
                        continue;
                    }

                    if ((index < (sqlLength - 2)) && separators.contains(temp = sql.substring(index, index + 3))) {
                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index;

                            break;
                        }

                        index += 2;
                    } else if ((index < (sqlLength - 1)) && separators.contains(temp = sql.substring(index, index + 2))) {
                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index;

                            break;
                        }

                        index += 1;
                    } else if (word.equals(String.valueOf(c)) || (!caseSensitive && word.equalsIgnoreCase(String.valueOf(c)))) {
                        result = index;

                        break;
                    }
                } else {
                    sb.append(c);

                    if ((c == SK._QUOTATION_S) || (c == SK._QUOTATION_D)) {
                        quoteChar = c;
                    }
                }
            }

            if (result < 0 && !sb.isEmpty()) {
                temp = sb.toString();

                if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                    result = sqlLength - word.length();
                }
            }

            Objectory.recycle(sb);

            return result;
        } else {
            int result = indexWord(sql, subWords[0], fromIndex, caseSensitive);

            if (result >= 0) {
                int tmpIndex = result + subWords[0].length();
                String nextWord = null;

                for (int i = 1; i < subWords.length; i++) {
                    nextWord = nextWord(sql, tmpIndex);

                    if (Strings.isNotEmpty(nextWord) && (nextWord.equals(subWords[i]) || (!caseSensitive && nextWord.equalsIgnoreCase(subWords[i])))) {
                        tmpIndex += (subWords[i].length() + 1);
                    } else {
                        result = -1;

                        break;
                    }
                }
            }

            return result;
        }
    }

    /**
     * Extracts the next word or token from a SQL statement starting at the specified index.
     * This method skips leading whitespace and returns the next meaningful token,
     * which could be a keyword, identifier, operator, or separator.
     * 
     * <p>The method handles:</p>
     * <ul>
     *   <li>Whitespace skipping</li>
     *   <li>Quoted identifiers (returns the entire quoted string)</li>
     *   <li>Multi-character operators (e.g., >=, !=)</li>
     *   <li>Single-character tokens</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = "SELECT   name,   age FROM users";
     * String word1 = SQLParser.nextWord(sql, 6);    // Returns: "name" (skips spaces after SELECT)
     * String word2 = SQLParser.nextWord(sql, 13);   // Returns: ","
     * String word3 = SQLParser.nextWord(sql, 14);   // Returns: "age" (skips spaces after comma)
     * }</pre>
     * 
     * @param sql the SQL statement to extract the word from
     * @param fromIndex the starting position for extraction (0-based)
     * @return the next word or token found, or an empty string if no more tokens exist
     */
    public static String nextWord(final String sql, final int fromIndex) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();

        String temp = "";
        char quoteChar = 0;

        for (int index = fromIndex; index < sqlLength; index++) {
            final char c = sql.charAt(index);

            // is it in a quoted identifier?
            if (quoteChar != 0) {
                sb.append(c);

                // end in quote.
                if (c == quoteChar && sql.charAt(index - 1) != '\\') {
                    break;
                }
            } else if (isSeparator(sql, sqlLength, index, c)) {
                if (!sb.isEmpty()) {
                    break;
                } else if (c == SK._SPACE || c == TAB || c == ENTER || c == ENTER_2) {
                    // skip white char
                    continue;
                }

                if (((index < (sqlLength - 2)) && separators.contains(temp = sql.substring(index, index + 3)))
                        || ((index < (sqlLength - 1)) && separators.contains(temp = sql.substring(index, index + 2)))) {
                    sb.append(temp);
                } else {
                    sb.append(c);
                }

                break;
            } else {
                sb.append(c);

                if ((c == SK._QUOTATION_S) || (c == SK._QUOTATION_D)) {
                    quoteChar = c;
                }
            }
        }

        final String st = (sb.isEmpty()) ? "" : sb.toString();
        Objectory.recycle(sb);

        return st;
    }

    /**
     * Registers a single character as a SQL separator.
     * Once registered, this character will be recognized as a token separator
     * during SQL parsing operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLParser.registerSeparator('$');   // Register $ as a separator
     * List<String> words = SQLParser.parse("SELECT$FROM$users");
     * // Result: ["SELECT", "$", "FROM", "$", "users"]
     * }</pre>
     * 
     * @param separator the character to register as a separator 
     */
    public static void registerSeparator(final char separator) {
        separators.add(separator);
    }

    /**
     * Registers a string as a SQL separator.
     * This can be used to register multi-character operators or separators
     * that should be recognized as single tokens during parsing.
     *
     * <p>If the separator is a single character, it will also be registered
     * as a character separator for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLParser.registerSeparator("<=>");   // Register the NULL-safe equal operator
     * SQLParser.registerSeparator("::");    // Register PostgreSQL cast operator
     * }</pre>
     *
     * @param separator the string to register as a separator (must not be null)
     * @throws IllegalArgumentException if separator is null
     */
    public static void registerSeparator(final String separator) {
        N.checkArgNotNull(separator, "separator");

        separators.add(separator);

        if (separator.length() == 1) {
            separators.add(separator.charAt(0));
        }
    }

    /**
     * Checks if a character at a specific position in a SQL string is a separator.
     * This method performs context-aware checking, handling special cases like
     * MyBatis/iBatis parameter markers (#{...}).
     * 
     * <p>Special handling:</p>
     * <ul>
     *   <li># followed by { is not considered a separator (MyBatis/iBatis syntax)</li>
     *   <li>All registered separators are checked</li>
     * </ul>
     * 
     * @param str the SQL string being parsed
     * @param len the length of the SQL string
     * @param index the current position in the string
     * @param ch the character to check
     * @return {@code true} if the character is a separator in this context, {@code false} otherwise
     */
    public static boolean isSeparator(final String str, final int len, final int index, final char ch) {
        // for Ibatis/Mybatis
        if (ch == '#' && index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        return separators.contains(ch);
    }

    /**
     * Determines if a word at a specific position in a parsed word list represents a function name.
     * A word is considered a function name if it is followed by an opening parenthesis,
     * either immediately or after whitespace.
     * 
     * <p>This method is useful for identifying SQL function calls during parsing or analysis.</p>
     * 
     * @param words the list of parsed SQL words/tokens
     * @param len the total length of the words list
     * @param index the index of the word to check
     * @return {@code true} if the word at the specified index is a function name, {@code false} otherwise
     */
    public static boolean isFunctionName(final List<String> words, final int len, final int index) {
        //    return (i < len - 1 && words.get(i + 1).charAt(0) == SK._PARENTHESES_L)
        //            || (i < len - 2 && SK.SPACE.equals(words.get(i + 1)) && words.get(i + 2).charAt(0) == SK._PARENTHESES_L);

        if (index < len - 1) {
            String nextWord = words.get(index + 1);
            if (!nextWord.isEmpty() && nextWord.charAt(0) == SK._PARENTHESES_L) {
                return true;
            }
        }

        for (int i = index + 1; i < len; i++) {
            String word = words.get(i);
            if (!word.isEmpty() && word.charAt(0) == SK._PARENTHESES_L) {
                return true;
            } else if (!SK.SPACE.equals(word)) {
                return false;
            }
        }

        return false;
    }
}
