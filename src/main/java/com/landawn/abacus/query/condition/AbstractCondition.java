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

package com.landawn.abacus.query.condition;

import static com.landawn.abacus.util.SK.COMMA_SPACE;
import static com.landawn.abacus.util.SK.SPACE;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.SqlParser;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for all condition implementations.
 * This class provides common functionality for conditions including operator storage,
 * utility methods for string representation, and property name formatting.
 *
 * <p>{@code AbstractCondition} serves as the foundation for the condition hierarchy, implementing
 * the {@link Condition} interface. Logical-composition helpers ({@code and()}, {@code or()},
 * {@code not()}, {@code xor()}) are provided by the {@link ComposableCondition} subclass, not by
 * this class directly; instances of {@code AbstractCondition} that do not also extend
 * {@code ComposableCondition} (e.g. clause types like {@link Where}, {@link OrderBy}) cannot be
 * combined with logical operators.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutable operator storage after construction</li>
 *   <li>Static utility helpers for parameter and property-name formatting (used by subclasses)</li>
 *   <li>Default {@link #toString()} that delegates to {@link #toString(NamingPolicy)} with
 *       {@link NamingPolicy#NO_CHANGE}</li>
 * </ul>
 *
 * @see Condition
 * @see ComposableCondition
 * @see Operator
 */
public abstract class AbstractCondition implements Condition {

    private static final ImmutableSet<Operator> clauseOperators;

    static {
        final Set<Operator> set = N.newLinkedHashSet();
        // Membership set used only by isClause(Operator) via contains(); insertion order is not behaviorally significant.
        set.add(Operator.JOIN);
        set.add(Operator.LEFT_JOIN);
        set.add(Operator.RIGHT_JOIN);
        set.add(Operator.FULL_JOIN);
        set.add(Operator.CROSS_JOIN);
        set.add(Operator.INNER_JOIN);
        set.add(Operator.NATURAL_JOIN);
        set.add(Operator.WHERE);
        set.add(Operator.GROUP_BY);
        set.add(Operator.HAVING);
        set.add(Operator.ORDER_BY);
        set.add(Operator.LIMIT);
        set.add(Operator.OFFSET);
        set.add(Operator.FOR_UPDATE);
        // Set-operation connectors.
        set.add(Operator.UNION_ALL);
        set.add(Operator.UNION);
        set.add(Operator.INTERSECT);
        set.add(Operator.EXCEPT);
        set.add(Operator.MINUS);

        clauseOperators = ImmutableSet.wrap(set);
    }

    /**
     * The operator for this condition.
     * This field is immutable once set in the constructor and defines
     * the type of operation this condition represents.
     */
    protected final Operator operator;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized AbstractCondition instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    AbstractCondition() {
        operator = null;
    }

    /**
     * Creates a new {@code AbstractCondition} with the specified operator.
     * The operator is immutable once set and defines the behavior of this condition.
     * Subclass constructors must supply a non-{@code null} operator.
     *
     * @param operator the operator for this condition (must not be {@code null})
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected AbstractCondition(final Operator operator) {
        this.operator = N.requireNonNull(operator, "operator");
    }

    /**
     * Checks if the given operator is a valid clause operator.
     * Clause operators represent major SQL query components like WHERE, JOIN, GROUP BY, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result1 = isClause(Operator.WHERE);      // true
     * boolean result2 = isClause(Operator.ORDER_BY);   // true
     * boolean result3 = isClause(Operator.LEFT_JOIN);  // true
     * boolean result4 = isClause(Operator.EQUAL);      // false
     * boolean result5 = isClause(Operator.AND);        // false
     * boolean result6 = isClause((Operator) null);     // false
     * }</pre>
     *
     * @param operator the operator to check
     * @return {@code true} if the operator is a clause operator, {@code false} otherwise
     */
    protected static boolean isClause(final Operator operator) {
        return operator != null && clauseOperators.contains(operator);
    }

    /**
     * Checks if the given operator string represents a valid clause operator.
     * This method converts the string to an Operator and checks if it's a clause.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result1 = isClause("WHERE");      // true
     * boolean result2 = isClause("ORDER BY");   // true
     * boolean result3 = isClause("GROUP BY");   // true
     * boolean result4 = isClause("=");          // false
     * boolean result5 = isClause("AND");        // false
     * }</pre>
     *
     * @param operator the operator string to check
     * @return {@code true} if the operator string represents a clause operator, {@code false} otherwise
     */
    protected static boolean isClause(final String operator) {
        if (Strings.isEmpty(operator)) {
            return false;
        }

        return isClause(Operator.of(operator));
    }

    /**
     * Checks if the given condition is a clause condition.
     * A condition is a clause if its operator is a clause operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where whereClause = new Where(Filters.equal("status", "active"));
     * boolean result1 = isClause(whereClause);   // true
     *
     * OrderBy orderByClause = new OrderBy("name");
     * boolean result2 = isClause(orderByClause);   // true
     *
     * Equal equalCond = Filters.equal("age", 25);
     * boolean result3 = isClause(equalCond);   // false
     *
     * boolean result4 = isClause((Condition) null);   // false
     * }</pre>
     *
     * @param cond the condition to check
     * @return {@code true} if the condition has a clause operator, {@code false} if null or not a clause
     */
    protected static boolean isClause(final Condition cond) {
        if (cond == null) {
            return false;
        }

        if (cond instanceof Expression) {
            final String literal = ((Expression) cond).getLiteral();

            if (Strings.isEmpty(literal)) {
                return false;
            }

            final String firstWord = SqlParser.nextWord(literal, 0);

            if (isClause(firstWord)) {
                return true;
            }

            final int secondWordStart = literal.indexOf(firstWord) + firstWord.length();
            final String secondWord = SqlParser.nextWord(literal, secondWordStart);

            return Strings.isNotEmpty(secondWord) && isClause(firstWord + SPACE + secondWord);
        }

        return isClause(cond.operator());
    }

    /**
     * Checks whether the given condition is an {@code ON} or {@code USING} join connector.
     * For an {@link Expression}, the first word of its (non-empty) literal is inspected; for any
     * other condition, its {@link Condition#operator() operator} is checked.
     *
     * @param cond the condition to check (may be {@code null})
     * @return {@code true} if {@code cond} is an {@code ON}/{@code USING} connector, {@code false}
     *         otherwise (including for a {@code null} {@code cond})
     */
    protected static boolean isOnOrUsing(final Condition cond) {
        if (cond == null) {
            return false;
        }

        if (cond instanceof Expression) {
            final String literal = ((Expression) cond).getLiteral();

            if (Strings.isEmpty(literal)) {
                return false;
            }

            return isOnOrUsing(SqlParser.nextWord(literal, 0));
        }

        return isOnOrUsing(cond.operator());
    }

    /**
     * Checks whether the given condition is a quantified-subquery operand, i.e. its operator is
     * {@code ANY}, {@code ALL}, or {@code SOME}.
     *
     * @param cond the condition to check (may be {@code null})
     * @return {@code true} if {@code cond} has an {@code ANY}/{@code ALL}/{@code SOME} operator,
     *         {@code false} otherwise (including for a {@code null} {@code cond})
     */
    protected static boolean isQuantifiedSubQueryOperand(final Condition cond) {
        return cond != null && isQuantifiedSubQueryOperator(cond.operator());
    }

    /**
     * Checks whether the given operator is a quantified-subquery operator
     * ({@code ANY}, {@code ALL}, or {@code SOME}).
     *
     * @param operator the operator to check (may be {@code null})
     * @return {@code true} if {@code operator} is {@code ALL}, {@code ANY}, or {@code SOME}; {@code false} otherwise
     */
    protected static boolean isQuantifiedSubQueryOperator(final Operator operator) {
        return operator == Operator.ALL || operator == Operator.ANY || operator == Operator.SOME;
    }

    /**
     * Checks whether the given operator is an {@code ON} or {@code USING} join connector.
     *
     * @param operator the operator to check (may be {@code null})
     * @return {@code true} if {@code operator} is {@code ON} or {@code USING}; {@code false} otherwise
     */
    protected static boolean isOnOrUsing(final Operator operator) {
        return operator == Operator.ON || operator == Operator.USING;
    }

    /**
     * Checks whether the given word matches the {@code ON} or {@code USING} keyword, ignoring case.
     *
     * @param word the word to check (may be {@code null})
     * @return {@code true} if {@code word} equals {@code "ON"} or {@code "USING"} (case-insensitive);
     *         {@code false} otherwise (including for a {@code null} {@code word})
     */
    protected static boolean isOnOrUsing(final String word) {
        return Operator.ON.toString().equalsIgnoreCase(word) || Operator.USING.toString().equalsIgnoreCase(word);
    }

    /**
     * Checks whether the given condition is, or recursively contains, an {@code ON}/{@code USING}
     * join connector. The search descends into the children of a {@link Junction} and into the
     * wrapped condition of a {@link Cell} or {@link ComposableCell}.
     *
     * @param cond the condition to check (may be {@code null})
     * @return {@code true} if {@code cond} is or contains an {@code ON}/{@code USING} connector,
     *         {@code false} otherwise (including for a {@code null} {@code cond})
     */
    protected static boolean containsOnOrUsing(final Condition cond) {
        if (cond == null) {
            return false;
        }

        if (isOnOrUsing(cond)) {
            return true;
        }

        if (cond instanceof Junction) {
            for (final Condition child : ((Junction) cond).getConditions()) {
                if (containsOnOrUsing(child)) {
                    return true;
                }
            }

            return false;
        }

        if (cond instanceof Cell) {
            return containsOnOrUsing(((Cell) cond).getCondition());
        }

        if (cond instanceof ComposableCell) {
            return containsOnOrUsing(((ComposableCell) cond).getCondition());
        }

        return false;
    }

    /**
     * Converts a parameter value to its string representation for use in condition strings.
     * Handles special cases like strings (adds quotes) and conditions (recursive toString).
     * Returns Java {@code null} when the parameter is {@code null}.
     *
     * <p>This utility method is used internally by condition implementations to format
     * parameter values consistently across the framework.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>{@code null} returns {@code null}</li>
     *   <li>Strings are wrapped in single quotes with embedded single/double quotes escaped via {@link #escapeStringLiteral(String)}
     *       (e.g., {@code John} -&gt; {@code 'John'}; {@code O'Brien} -&gt; {@code 'O\'Brien'})</li>
     *   <li>{@link Condition} values use the recursive {@code toString(namingPolicy)}; a {@link SubQuery} is additionally
     *       wrapped in parentheses, and the {@code IsNull.NULL}, {@code IsNaN.NAN}, and {@code IsInfinite.INFINITE}
     *       sentinels use their plain {@code toString()}</li>
     *   <li>{@link Number} and {@link Boolean} values use their {@code toString()} unchanged (no quoting).
     *       {@link Float#NaN}/{@link Double#NaN}/infinity values cause an {@link IllegalArgumentException} because
     *       they have no portable SQL literal form; use {@link IsNaN}/{@link IsInfinite} instead.</li>
     *   <li>Any other object (dates, characters, byte[], etc.) is converted via {@link N#stringOf(Object)} and
     *       then wrapped in single quotes with escaping applied, yielding a valid SQL string literal.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * formatParameter("John", NamingPolicy.NO_CHANGE);                // Returns: 'John'
     * formatParameter(123, NamingPolicy.NO_CHANGE);                   // Returns: 123
     * formatParameter(null, NamingPolicy.NO_CHANGE);                  // Returns: null (Java null)
     * formatParameter(subCondition, NamingPolicy.NO_CHANGE);          // Returns: subCondition.toString(policy)
     * formatParameter(new java.util.Date(0), NamingPolicy.NO_CHANGE); // Returns the date as a quoted, escaped string literal
     * }</pre>
     *
     * @param parameter the parameter value to convert; may be {@code null}
     * @param namingPolicy the naming policy to apply to property names within nested {@link Condition}s.
     *                     May be {@code null}; nested {@code Condition} implementations treat a
     *                     {@code null} policy as {@link NamingPolicy#NO_CHANGE}.
     * @return the string representation of the parameter, or {@code null} if {@code parameter} is {@code null}
     * @throws IllegalArgumentException if {@code parameter} is a {@code NaN} or infinite {@link Float}/{@link Double}
     */
    protected static String formatParameter(final Object parameter, final NamingPolicy namingPolicy) {
        if (parameter == null) {
            return null;
        }

        if (parameter instanceof String) {
            return SK._SINGLE_QUOTE + escapeStringLiteral((String) parameter) + SK._SINGLE_QUOTE;
        }

        if (parameter instanceof Condition) {
            if (parameter == IsNull.NULL || parameter == IsNaN.NAN || parameter == IsInfinite.INFINITE) { //NOSONAR
                return parameter.toString();
            } else {
                final String conditionString = ((Condition) parameter).toString(namingPolicy);

                if (parameter instanceof SubQuery) {
                    return SK.PARENTHESIS_L + conditionString + SK.PARENTHESIS_R;
                }

                return conditionString;
            }
        }

        if (parameter instanceof Number || parameter instanceof Boolean) {
            checkFiniteNumber(parameter);
            return parameter.toString();
        }

        // Date, LocalDateTime, Character, byte[], custom types, etc.: emit a quoted, escaped string literal.
        return SK._SINGLE_QUOTE + escapeStringLiteral(N.stringOf(parameter)) + SK._SINGLE_QUOTE;
    }

    /**
     * Escapes a string for safe inclusion as the body of a single-quoted SQL string literal.
     * Single quotes and double quotes are backslash-escaped via {@link com.landawn.abacus.util.Strings#quoteEscaped},
     * and a trailing-backslash guard is applied so that the closing {@code '} quote cannot be consumed
     * as an escape sequence. This is a defense-in-depth helper used by
     * {@link #formatParameter(Object, NamingPolicy)} and by {@link Expression#normalize(Object)};
     * callers must still emit the surrounding {@code '} quotes.
     *
     * <p>Note: this is dialect-tolerant escaping (backslash-style, with an extra trailing-backslash
     * guard) rather than strict SQL-standard quote doubling; the resulting literal is safe to embed
     * even when {@code standard_conforming_strings} is off (MySQL default) and remains a valid
     * literal under most dialects, but for fully portable SQL prefer parameterized builders.</p>
     *
     * @param str the raw string contents; {@code null} yields an empty string and an empty string
     *            is returned unchanged
     * @return the escaped string body, suitable for inclusion between two single quotes; never {@code null}
     */
    protected static String escapeStringLiteral(final String str) {
        if (str == null || str.isEmpty()) {
            return str == null ? Strings.EMPTY : str;
        }

        final String escaped = Strings.quoteEscaped(str);

        // Defensive guard: if the original ends in an unescaped backslash, the trailing closing
        // quote would be consumed as an escape in MySQL-style parsing. Count trailing backslashes
        // in the ESCAPED form and append one more if the count is odd.
        int trailingBackslashes = 0;
        for (int i = escaped.length() - 1; i >= 0 && escaped.charAt(i) == '\\'; i--) {
            trailingBackslashes++;
        }

        if ((trailingBackslashes & 1) == 1) {
            return escaped + '\\';
        }

        return escaped;
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code value} is a {@link Float} or {@link Double}
     * that is {@code NaN} or infinite. {@code NaN} and infinity have no portable SQL literal form;
     * callers should use {@link IsNaN}/{@link IsInfinite} conditions instead. This guard is applied
     * inside {@link #formatParameter(Object, NamingPolicy)} so a misuse fails fast at SQL-render
     * time rather than silently producing invalid SQL such as {@code WHERE x = NaN}.
     *
     * @param value the value to check; non-numeric or finite values are ignored
     */
    protected static void checkFiniteNumber(final Object value) {
        if (value instanceof Double) {
            final double d = (Double) value;
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("NaN/Infinity has no portable SQL literal; use IsNaN / IsInfinite condition instead. Got: " + d);
            }
        } else if (value instanceof Float) {
            final float f = (Float) value;
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw new IllegalArgumentException("NaN/Infinity has no portable SQL literal; use IsNaN / IsInfinite condition instead. Got: " + f);
            }
        }
    }

    /**
     * Concatenates property names into a formatted string.
     * Handles different array sizes efficiently, adding parentheses for multiple names.
     *
     * <p>This utility method is used internally for formatting multiple property names
     * in conditions like GROUP BY or ORDER BY.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>Single name: returned as-is without parentheses</li>
     *   <li>Multiple names: enclosed in parentheses and comma-separated</li>
     *   <li>Empty array: returns empty string</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * concatPropNames("name");            // Returns: name
     * concatPropNames("city", "state");   // Returns: (city, state)
     * concatPropNames("a", "b", "c");     // Returns: (a, b, c)
     * concatPropNames();                  // Returns: ""
     * }</pre>
     *
     * @param propNames the property names to concatenate (varargs, can be empty)
     * @return a formatted string of property names, empty string if no names provided
     */
    protected static String concatPropNames(final String... propNames) {
        if (N.isEmpty(propNames)) {
            return Strings.EMPTY;
        }

        final int size = propNames.length;

        switch (size) {
            case 1:
                return propNames[0];

            case 2:
                return SK.PARENTHESIS_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.PARENTHESIS_R;

            case 3:
                return SK.PARENTHESIS_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.COMMA_SPACE + propNames[2] + SK.PARENTHESIS_R;

            default:
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESIS_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(propNames[i]);
                    }

                    sb.append(SK._PARENTHESIS_R);

                    return sb.toString();

                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Concatenates property names from a collection into a formatted string.
     * Handles different collection sizes efficiently, adding parentheses for multiple names.
     *
     * <p>This utility method is used internally for formatting multiple property names
     * from collections in conditions like IN or GROUP BY.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>Single name: returned as-is without parentheses</li>
     *   <li>Multiple names: enclosed in parentheses and comma-separated</li>
     *   <li>Empty collection: returns empty string</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("city", "state", "zip");
     * concatPropNames(names);   // Returns: (city, state, zip)
     *
     * Set<String> single = Collections.singleton("id");
     * concatPropNames(single);   // Returns: id
     *
     * List<String> empty = Collections.emptyList();
     * concatPropNames(empty);   // Returns: ""
     * }</pre>
     *
     * @param propNames the collection of property names to concatenate (can be empty)
     * @return a formatted string of property names, empty string if collection is empty
     */
    protected static String concatPropNames(final Collection<String> propNames) {
        if (N.isEmpty(propNames)) {
            return Strings.EMPTY;
        }

        final Iterator<String> it = propNames.iterator();
        final int size = propNames.size();

        switch (size) {
            case 1:
                return it.next();

            case 2:
                return SK.PARENTHESIS_L + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESIS_R;

            case 3:
                return SK.PARENTHESIS_L + it.next() + SK.COMMA_SPACE + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESIS_R;

            default:

                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESIS_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(it.next());
                    }

                    sb.append(SK._PARENTHESIS_R);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Creates a comma-separated string of property names for use in ORDER BY or GROUP BY clauses.
     * This is an internal helper method used by {@link OrderBy} and {@link GroupBy} constructors.
     *
     * <p>This method is protected and not intended for direct use by application code.
     * Use the public {@link OrderBy} or {@link GroupBy} constructors instead.</p>
     *
     * @param propNames the array of property names (must not be {@code null} or empty)
     * @return a comma-separated string of property names suitable for use in a sort/grouping clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null}, empty, or contains {@code null}, empty, or blank elements
     */
    protected static String createSortExpression(final String... propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                checkPropName(propName);

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates a sort expression for a single property with the given direction,
     * for use in ORDER BY or GROUP BY clauses.
     * This is an internal helper method used by {@link OrderBy} and {@link GroupBy} constructors.
     *
     * <p>This method is protected and not intended for direct use by application code.
     * Use the public {@link OrderBy} or {@link GroupBy} constructors instead.</p>
     *
     * @param propName the property name (must not be {@code null}, empty, or blank)
     * @param direction the sort direction (must not be {@code null})
     * @return a string of the form {@code "propName direction"} suitable for a sort/grouping clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or {@code direction} is {@code null}
     */
    protected static String createSortExpression(final String propName, final SortDirection direction) {
        checkPropName(propName);

        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        return propName + SPACE + direction;
    }

    /**
     * Creates a sort expression for multiple properties, all using the same direction,
     * for use in ORDER BY or GROUP BY clauses.
     * This is an internal helper method used by {@link OrderBy} and {@link GroupBy} constructors.
     *
     * <p>This method is protected and not intended for direct use by application code.
     * Use the public {@link OrderBy} or {@link GroupBy} constructors instead.</p>
     *
     * @param propNames collection of property names (must not be {@code null} or empty)
     * @param direction the sort direction to apply to all properties (must not be {@code null})
     * @return a comma-separated string of {@code "propName direction"} entries
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty, {@code direction} is {@code null},
     *                                  or {@code propNames} contains {@code null}, empty, or blank elements
     */
    protected static String createSortExpression(final Collection<String> propNames, final SortDirection direction) {
        N.checkArgNotEmpty(propNames, "propNames");

        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                checkPropName(propName);

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
                sb.append(SPACE);
                sb.append(direction);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates a sort expression from a map of property names to their individual sort directions,
     * for use in ORDER BY or GROUP BY clauses.
     * This is an internal helper method used by {@link OrderBy} and {@link GroupBy} constructors.
     *
     * <p>This method is protected and not intended for direct use by application code.
     * Use the public {@link OrderBy} or {@link GroupBy} constructors instead.
     * Use a {@link java.util.LinkedHashMap} to preserve the desired column order.</p>
     *
     * @param orders map of property names to their sort directions (must not be {@code null} or empty)
     * @return a comma-separated string of {@code "propName direction"} entries in map iteration order
     * @throws IllegalArgumentException if {@code orders} is {@code null}/empty, or contains {@code null}, empty, or blank keys
     *                                  or {@code null} values
     */
    protected static String createSortExpression(final Map<String, SortDirection> orders) {
        N.checkArgNotEmpty(orders, "orders");

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
                final String propName = entry.getKey();
                final SortDirection direction = entry.getValue();

                checkPropName(propName);

                if (direction == null) {
                    throw new IllegalArgumentException("SortDirection in orders must not be null");
                }

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
                sb.append(SPACE);
                sb.append(direction);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Tests whether the given condition is an "empty predicate" — a condition that carries no actual
     * filtering logic and therefore cannot meaningfully participate in composition, clauses, or joins.
     * Specifically, this is a blank {@link Expression} (an empty or whitespace-only literal) or a
     * {@link Junction} that contains no sub-conditions.
     *
     * @param cond the condition to test (may be {@code null})
     * @return {@code true} if {@code cond} is a blank {@link Expression} or an empty {@link Junction};
     *         {@code false} otherwise (including for a {@code null} {@code cond})
     */
    protected static boolean isEmptyPredicate(final Condition cond) {
        if (cond instanceof Expression) {
            return Strings.isBlank(((Expression) cond).getLiteral());
        }

        if (cond instanceof Junction) {
            return N.isEmpty(((Junction) cond).getConditions());
        }

        return false;
    }

    /**
     * Validates that the given condition is a valid operand for composable operations (AND, OR, NOT, XOR).
     * Conditions that are a {@link Criteria}, a SQL clause (WHERE, ORDER BY, etc.), an {@code ON}/{@code USING}
     * connector, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, an empty predicate
     * (a blank {@link Expression} or empty {@link Junction}), or that have a {@code null} operator
     * (including a {@code null} {@code cond}) cannot participate in logical composition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid operand - comparison condition
     * Condition eq = Filters.equal("status", "active");
     * validateComposableOperand(eq, "and");   // Returns eq
     *
     * // Invalid operand - throws IllegalArgumentException
     * Condition where = new Where(eq);
     * validateComposableOperand(where, "and");   // Throws IllegalArgumentException
     * }</pre>
     *
     * @param cond the condition to validate; may be {@code null} (will trigger the exception)
     * @param methodName the name of the composable method being called (for error messages)
     * @return {@code cond} unchanged, after validation succeeds
     * @throws IllegalArgumentException if {@code cond} is {@code null}, has a {@code null} operator, or is a
     *                                  {@link Criteria}, a SQL clause, an {@code ON}/{@code USING} connector,
     *                                  an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand,
     *                                  or an empty predicate (a blank {@link Expression} or empty {@link Junction})
     */
    protected static Condition validateComposableOperand(final Condition cond, final String methodName) {
        N.checkArgNotNull(cond, "cond");

        final Operator operator = cond.operator();

        if (operator == null || cond instanceof Criteria || isClause(cond) || containsOnOrUsing(cond) || isQuantifiedSubQueryOperand(cond)
                || isEmptyPredicate(cond)) {
            throw new IllegalArgumentException("Condition with operator '" + operator + "' cannot be used in composable method '" + methodName + "'");
        }

        return cond;
    }

    /**
     * Validates that the given property name is non-{@code null}, non-empty, and not blank
     * (whitespace-only). Used by subclass constructors to reject invalid property/column names.
     *
     * @param propName the property name to validate
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    protected static void checkPropName(final String propName) {
        if (Strings.isEmpty(propName) || Strings.isBlank(propName)) {
            throw new IllegalArgumentException("Property name must not be null, empty, or blank");
        }
    }

    /**
     * Gets the operator for this condition.
     * The operator defines the type of operation (e.g., EQUAL, GREATER_THAN, AND, OR).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal eq = new Equal("status", "active");
     * Operator op1 = eq.operator();   // Operator.EQUAL
     *
     * GreaterThan gt = new GreaterThan("age", 18);
     * Operator op2 = gt.operator();   // Operator.GREATER_THAN
     * }</pre>
     *
     * @return the operator for this condition
     */
    @Override
    public Operator operator() {
        return operator;
    }

    /**
     * Returns a string representation of this condition using the default naming policy.
     * This method delegates to {@link #toString(NamingPolicy)} with {@link NamingPolicy#NO_CHANGE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal eq = new Equal("name", "John");
     * String s1 = eq.toString();   // "name = 'John'"
     *
     * // Binary rendering (inherited by Equal): a null value with = renders as IS NULL
     * Equal nullEq = new Equal("deletedAt", (Object) null);
     * String s2 = nullEq.toString();   // "deletedAt IS NULL"
     * }</pre>
     *
     * @return a string representation of this condition
     */
    @Override
    public String toString() {
        return toString(NamingPolicy.NO_CHANGE);
    }

}
