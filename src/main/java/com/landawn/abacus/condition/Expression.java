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

package com.landawn.abacus.condition;

import static com.landawn.abacus.util.WD.ABS;
import static com.landawn.abacus.util.WD.ACOS;
import static com.landawn.abacus.util.WD.AMPERSAND;
import static com.landawn.abacus.util.WD.ASIN;
import static com.landawn.abacus.util.WD.ASTERISK;
import static com.landawn.abacus.util.WD.ATAN;
import static com.landawn.abacus.util.WD.AVG;
import static com.landawn.abacus.util.WD.CEIL;
import static com.landawn.abacus.util.WD.CIRCUMFLEX;
import static com.landawn.abacus.util.WD.COMMA_SPACE;
import static com.landawn.abacus.util.WD.CONCAT;
import static com.landawn.abacus.util.WD.COS;
import static com.landawn.abacus.util.WD.COUNT;
import static com.landawn.abacus.util.WD.EXP;
import static com.landawn.abacus.util.WD.FLOOR;
import static com.landawn.abacus.util.WD.LENGTH;
import static com.landawn.abacus.util.WD.LN;
import static com.landawn.abacus.util.WD.LOG;
import static com.landawn.abacus.util.WD.LOWER;
import static com.landawn.abacus.util.WD.LPAD;
import static com.landawn.abacus.util.WD.LTRIM;
import static com.landawn.abacus.util.WD.MAX;
import static com.landawn.abacus.util.WD.MIN;
import static com.landawn.abacus.util.WD.MINUS;
import static com.landawn.abacus.util.WD.MOD;
import static com.landawn.abacus.util.WD.PERCENT;
import static com.landawn.abacus.util.WD.PLUS;
import static com.landawn.abacus.util.WD.POWER;
import static com.landawn.abacus.util.WD.REPLACE;
import static com.landawn.abacus.util.WD.RPAD;
import static com.landawn.abacus.util.WD.RTRIM;
import static com.landawn.abacus.util.WD.SIGN;
import static com.landawn.abacus.util.WD.SIN;
import static com.landawn.abacus.util.WD.SLASH;
import static com.landawn.abacus.util.WD.SPACE;
import static com.landawn.abacus.util.WD.SQRT;
import static com.landawn.abacus.util.WD.SUBSTR;
import static com.landawn.abacus.util.WD.SUM;
import static com.landawn.abacus.util.WD.TAN;
import static com.landawn.abacus.util.WD.TRIM;
import static com.landawn.abacus.util.WD.UPPER;
import static com.landawn.abacus.util.WD.VERTICALBAR;
import static com.landawn.abacus.util.WD._QUOTATION_S;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SQLParser;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Represents a raw SQL expression that can be used in queries.
 * This class allows the inclusion of arbitrary SQL expressions, functions, and literals
 * in query conditions. It also provides utility methods for building SQL expressions
 * and mathematical/string functions.
 * 
 * <p>Expressions are cached for performance optimization. The same expression literal
 * will return the same Expression instance when created through {@link #of(String)}.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Simple expression
 * Expression expr = Expression.of("price * 0.9");
 * 
 * // Using in a condition
 * Condition discountPrice = CF.lt(expr, 100);
 * 
 * // SQL functions
 * String upperName = Expression.upper("name");
 * String avgPrice = Expression.average("price");
 * </pre>
 * 
 * @see AbstractCondition
 * @see ConditionFactory#expr(String)
 */
public class Expression extends AbstractCondition {

    static final String NULL_STRING = "null";

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    private static final String LEFT_SHIFT = "<<";

    private static final String RIGHT_SHIFT = ">>";

    private static final String NULL = "NULL";

    private static final String EMPTY = "BLANK";

    private static final Map<String, Expression> cachedExpression = new ConcurrentHashMap<>();

    // For Kryo
    final String literal;

    // For Kryo
    Expression() {
        literal = null;
    }

    /**
     * Constructs a new Expression with the specified SQL literal.
     * 
     * @param literal the SQL expression as a string. Can contain any valid SQL.
     * 
     * <p>Example:</p>
     * <pre>
     * Expression expr1 = new Expression("CURRENT_TIMESTAMP");
     * Expression expr2 = new Expression("price * quantity");
     * Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END");
     * </pre>
     */
    public Expression(final String literal) {
        super(Operator.EMPTY);

        this.literal = literal;
    }

    /**
     * Gets the SQL literal string of this expression.
     * 
     * @return the SQL expression string
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Creates or retrieves a cached Expression instance for the given literal.
     * This method uses caching to ensure that expressions with the same literal
     * share the same instance, improving memory efficiency.
     * 
     * @param literal the SQL expression string
     * @return a cached or new Expression instance
     * 
     * <p>Example:</p>
     * <pre>
     * Expression expr1 = Expression.of("CURRENT_DATE");
     * Expression expr2 = Expression.of("CURRENT_DATE");
     * // expr1 == expr2 (same instance due to caching)
     * </pre>
     */
    public static Expression of(final String literal) {
        Expression expr = cachedExpression.get(literal);

        if (expr == null) {
            expr = new Expression(literal);

            cachedExpression.put(literal, expr);
        }

        return expr;
    }

    /**
     * Creates an equality expression between a literal and a value.
     * 
     * @param literal the left-hand side of the equality
     * @param value the right-hand side value
     * @return a string representation of the equality expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.equal("age", 25);
     * // Returns: "age = 25"
     * </pre>
     */
    public static String equal(final String literal, final Object value) { //NOSONAR
        return link(Operator.EQUAL, literal, value);
    }

    /**
     * Creates an equality expression between a literal and a value.
     * Alias for {@link #equal(String, Object)}.
     * 
     * @param literal the left-hand side of the equality
     * @param value the right-hand side value
     * @return a string representation of the equality expression
     */
    public static String eq(final String literal, final Object value) {
        return link(Operator.EQUAL, literal, value);
    }

    /**
     * Creates a not-equal expression between a literal and a value.
     * 
     * @param literal the left-hand side of the inequality
     * @param value the right-hand side value
     * @return a string representation of the not-equal expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.notEqual("status", "INACTIVE");
     * // Returns: "status != 'INACTIVE'"
     * </pre>
     */
    public static String notEqual(final String literal, final Object value) {
        return link(Operator.NOT_EQUAL, literal, value);
    }

    /**
     * Creates a not-equal expression between a literal and a value.
     * Alias for {@link #notEqual(String, Object)}.
     * 
     * @param literal the left-hand side of the inequality
     * @param value the right-hand side value
     * @return a string representation of the not-equal expression
     */
    public static String ne(final String literal, final Object value) {
        return link(Operator.NOT_EQUAL, literal, value);
    }

    /**
     * Creates a greater-than expression between a literal and a value.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the greater-than expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.greaterThan("salary", 50000);
     * // Returns: "salary > 50000"
     * </pre>
     */
    public static String greaterThan(final String literal, final Object value) {
        return link(Operator.GREATER_THAN, literal, value);
    }

    /**
     * Creates a greater-than expression between a literal and a value.
     * Alias for {@link #greaterThan(String, Object)}.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the greater-than expression
     */
    public static String gt(final String literal, final Object value) {
        return link(Operator.GREATER_THAN, literal, value);
    }

    /**
     * Creates a greater-than-or-equal expression between a literal and a value.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the greater-than-or-equal expression
     */
    public static String greaterEqual(final String literal, final Object value) {
        return link(Operator.GREATER_EQUAL, literal, value);
    }

    /**
     * Creates a greater-than-or-equal expression between a literal and a value.
     * Alias for {@link #greaterEqual(String, Object)}.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the greater-than-or-equal expression
     */
    public static String ge(final String literal, final Object value) {
        return link(Operator.GREATER_EQUAL, literal, value);
    }

    /**
     * Creates a less-than expression between a literal and a value.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the less-than expression
     */
    public static String lessThan(final String literal, final Object value) {
        return link(Operator.LESS_THAN, literal, value);
    }

    /**
     * Creates a less-than expression between a literal and a value.
     * Alias for {@link #lessThan(String, Object)}.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the less-than expression
     */
    public static String lt(final String literal, final Object value) {
        return link(Operator.LESS_THAN, literal, value);
    }

    /**
     * Creates a less-than-or-equal expression between a literal and a value.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the less-than-or-equal expression
     */
    public static String lessEqual(final String literal, final Object value) {
        return link(Operator.LESS_EQUAL, literal, value);
    }

    /**
     * Creates a less-than-or-equal expression between a literal and a value.
     * Alias for {@link #lessEqual(String, Object)}.
     * 
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the less-than-or-equal expression
     */
    public static String le(final String literal, final Object value) {
        return link(Operator.LESS_EQUAL, literal, value);
    }

    /**
     * Creates a BETWEEN expression for a literal with min and max values.
     * 
     * @param literal the literal to test
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a string representation of the BETWEEN expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.between("age", 18, 65);
     * // Returns: "age BETWEEN (18, 65)"
     * </pre>
     */
    public static String between(final String literal, final Object min, final Object max) {
        return link(Operator.BETWEEN, literal, min, max);
    }

    /**
     * Creates a BETWEEN expression for a literal with min and max values.
     * 
     * @param literal the literal to test
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a string representation of the BETWEEN expression
     * @deprecated please use {@link #between(String, Object, Object)}
     */
    @Deprecated
    public static String bt(final String literal, final Object min, final Object max) {
        return link(Operator.BETWEEN, literal, min, max);
    }

    /**
     * Creates a LIKE expression for pattern matching.
     * 
     * @param literal the literal to match
     * @param value the pattern to match against (can include % and _ wildcards)
     * @return a string representation of the LIKE expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.like("name", "John%");
     * // Returns: "name LIKE 'John%'"
     * </pre>
     */
    public static String like(final String literal, final String value) {
        return link(Operator.LIKE, literal, value);
    }

    /**
     * Creates an IS NULL expression for the specified literal.
     * 
     * @param literal the literal to check for null
     * @return a string representation of the IS NULL expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.isNull("middleName");
     * // Returns: "middleName IS NULL"
     * </pre>
     */
    public static String isNull(final String literal) {
        return link2(Operator.IS, literal, NULL);
    }

    /**
     * Creates an IS NOT NULL expression for the specified literal.
     * 
     * @param literal the literal to check for not null
     * @return a string representation of the IS NOT NULL expression
     */
    public static String isNotNull(final String literal) {
        return link2(Operator.IS_NOT, literal, NULL);
    }

    /**
     * Creates an IS EMPTY expression for the specified literal.
     * This checks if a value is empty (blank).
     * 
     * @param literal the literal to check for emptiness
     * @return a string representation of the IS EMPTY expression
     */
    public static String isEmpty(final String literal) {
        return link2(Operator.IS, literal, EMPTY);
    }

    /**
     * Creates an IS NOT EMPTY expression for the specified literal.
     * This checks if a value is not empty (not blank).
     * 
     * @param literal the literal to check for non-emptiness
     * @return a string representation of the IS NOT EMPTY expression
     */
    public static String isNotEmpty(final String literal) {
        return link2(Operator.IS_NOT, literal, EMPTY);
    }

    /**
     * Creates an AND expression combining multiple literals.
     * 
     * @param literals the literals to combine with AND
     * @return a string representation of the AND expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.and("active = true", "age > 18", "status = 'APPROVED'");
     * // Returns: "active = true AND age > 18 AND status = 'APPROVED'"
     * </pre>
     */
    public static String and(final String... literals) {
        return link2(Operator.AND, literals);
    }

    /**
     * Creates an OR expression combining multiple literals.
     * 
     * @param literals the literals to combine with OR
     * @return a string representation of the OR expression
     */
    public static String or(final String... literals) {
        return link2(Operator.OR, literals);
    }

    /**
     * Creates an addition expression for the given objects.
     * 
     * @param objects the values to add
     * @return a string representation of the addition expression
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.plus("price", "tax", "shipping");
     * // Returns: "price + tax + shipping"
     * </pre>
     */
    public static String plus(final Object... objects) {
        return link(PLUS, objects);
    }

    /**
     * Creates a subtraction expression for the given objects.
     * 
     * @param objects the values to subtract
     * @return a string representation of the subtraction expression
     */
    public static String minus(final Object... objects) {
        return link(MINUS, objects);
    }

    /**
     * Creates a multiplication expression for the given objects.
     * 
     * @param objects the values to multiply
     * @return a string representation of the multiplication expression
     */
    public static String multi(final Object... objects) {
        return link(ASTERISK, objects);
    }

    /**
     * Creates a division expression for the given objects.
     * 
     * @param objects the values to divide
     * @return a string representation of the division expression
     */
    public static String division(final Object... objects) {
        return link(SLASH, objects);
    }

    /**
     * Creates a modulus expression for the given objects.
     * 
     * @param objects the values for modulus operation
     * @return a string representation of the modulus expression
     */
    public static String modulus(final Object... objects) {
        return link(PERCENT, objects);
    }

    /**
     * Creates a left shift expression for the given objects.
     * 
     * @param objects the values for left shift operation
     * @return a string representation of the left shift expression
     */
    public static String lShift(final Object... objects) {
        return link(LEFT_SHIFT, objects);
    }

    /**
     * Creates a right shift expression for the given objects.
     * 
     * @param objects the values for right shift operation
     * @return a string representation of the right shift expression
     */
    public static String rShift(final Object... objects) {
        return link(RIGHT_SHIFT, objects);
    }

    /**
     * Creates a bitwise AND expression for the given objects.
     * 
     * @param objects the values for bitwise AND operation
     * @return a string representation of the bitwise AND expression
     */
    public static String bitwiseAnd(final Object... objects) {
        return link(AMPERSAND, objects);
    }

    /**
     * Creates a bitwise OR expression for the given objects.
     * 
     * @param objects the values for bitwise OR operation
     * @return a string representation of the bitwise OR expression
     */
    public static String bitwiseOr(final Object... objects) {
        return link(VERTICALBAR, objects);
    }

    /**
     * Creates a bitwise XOR expression for the given objects.
     * 
     * @param objects the values for bitwise XOR operation
     * @return a string representation of the bitwise XOR expression
     */
    public static String bitwiseXOr(final Object... objects) {
        return link(CIRCUMFLEX, objects);
    }

    /**
     * Links a literal with a value using the specified operator.
     * 
     * @param operator the operator to use
     * @param literal the left-hand side literal
     * @param value the right-hand side value
     * @return a string representation of the linked expression
     */
    static String link(final Operator operator, final String literal, final Object value) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(formalize(value));

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Links a literal with min and max values using the specified operator.
     * 
     * @param operator the operator to use
     * @param literal the literal
     * @param min the minimum value
     * @param max the maximum value
     * @return a string representation of the linked expression
     */
    static String link(final Operator operator, final String literal, final Object min, final Object max) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(WD._PARENTHESES_L);
            sb.append(formalize(min));
            sb.append(WD.COMMA_SPACE);
            sb.append(formalize(max));
            sb.append(WD._PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Links a literal with an operator postfix.
     * 
     * @param operator the operator to use
     * @param literal the literal
     * @param operatorPostfix the postfix for the operator
     * @return a string representation of the linked expression
     */
    static String link2(final Operator operator, final String literal, final String operatorPostfix) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(operatorPostfix);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Links multiple literals with the specified operator.
     * 
     * @param operator the operator to use
     * @param literals the literals to link
     * @return a string representation of the linked expression
     */
    static String link2(final Operator operator, final String... literals) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < literals.length; i++) {
                if (i > 0) {
                    sb.append(WD._SPACE);
                    sb.append(operator.getName());
                    sb.append(WD._SPACE);
                }

                sb.append(literals[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Links multiple objects with the specified symbol.
     * 
     * @param linkedSymbol the symbol to use for linking
     * @param objects the objects to link
     * @return a string representation of the linked expression
     */
    static String link(String linkedSymbol, final Object... objects) {
        if (!(SPACE.equals(linkedSymbol) || COMMA_SPACE.equals(linkedSymbol))) {
            linkedSymbol = WD._SPACE + linkedSymbol + WD._SPACE;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < objects.length; i++) {
                if (i > 0) {
                    sb.append(linkedSymbol);
                }

                sb.append(formalize(objects[i]));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Converts a value to its SQL representation.
     * Strings are quoted, nulls become "null", numbers are converted to strings,
     * and expressions keep their literal form.
     * 
     * @param value the value to formalize
     * @return the SQL representation of the value
     * 
     * <p>Example:</p>
     * <pre>
     * Expression.formalize("text");     // Returns: "'text'"
     * Expression.formalize(123);        // Returns: "123"
     * Expression.formalize(null);       // Returns: "null"
     * Expression.formalize(expr);       // Returns: the expression's literal
     * </pre>
     */
    public static String formalize(final Object value) {
        if (value == null) {
            return NULL_STRING;
        }

        if (value instanceof String) {
            return (_QUOTATION_S + Strings.quoteEscaped((String) value) + _QUOTATION_S);
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Expression) {
            return ((Expression) value).getLiteral();
        } else {
            return (_QUOTATION_S + Strings.quoteEscaped(N.stringOf(value)) + _QUOTATION_S);
        }
    }

    /**
     * Creates a COUNT function expression.
     * 
     * @param expression the expression to count
     * @return a COUNT function string
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.count("*");           // Returns: "COUNT(*)"
     * String expr2 = Expression.count("DISTINCT id"); // Returns: "COUNT(DISTINCT id)"
     * </pre>
     */
    public static String count(final String expression) {
        return function(COUNT, expression);
    }

    /**
     * Creates an AVERAGE function expression.
     * 
     * @param expression the expression to average
     * @return an AVG function string
     */
    public static String average(final String expression) {
        return function(AVG, expression);
    }

    /**
     * Creates a SUM function expression.
     * 
     * @param expression the expression to sum
     * @return a SUM function string
     */
    public static String sum(final String expression) {
        return function(SUM, expression);
    }

    /**
     * Creates a MIN function expression.
     * 
     * @param expression the expression to find minimum
     * @return a MIN function string
     */
    public static String min(final String expression) {
        return function(MIN, expression);
    }

    /**
     * Creates a MAX function expression.
     * 
     * @param expression the expression to find maximum
     * @return a MAX function string
     */
    public static String max(final String expression) {
        return function(MAX, expression);
    }

    /**
     * Creates an ABS (absolute value) function expression.
     * 
     * @param expression the expression to get absolute value of
     * @return an ABS function string
     */
    public static String abs(final String expression) {
        return function(ABS, expression);
    }

    /**
     * Creates an ACOS (arc cosine) function expression.
     * 
     * @param expression the expression to calculate arc cosine of
     * @return an ACOS function string
     */
    public static String acos(final String expression) {
        return function(ACOS, expression);
    }

    /**
     * Creates an ASIN (arc sine) function expression.
     * 
     * @param expression the expression to calculate arc sine of
     * @return an ASIN function string
     */
    public static String asin(final String expression) {
        return function(ASIN, expression);
    }

    /**
     * Creates an ATAN (arc tangent) function expression.
     * 
     * @param expression the expression to calculate arc tangent of
     * @return an ATAN function string
     */
    public static String atan(final String expression) {
        return function(ATAN, expression);
    }

    /**
     * Creates a CEIL (ceiling) function expression.
     * 
     * @param expression the expression to round up
     * @return a CEIL function string
     */
    public static String ceil(final String expression) {
        return function(CEIL, expression);
    }

    /**
     * Creates a COS (cosine) function expression.
     * 
     * @param expression the expression to calculate cosine of
     * @return a COS function string
     */
    public static String cos(final String expression) {
        return function(COS, expression);
    }

    /**
     * Creates an EXP (exponential) function expression.
     * 
     * @param expression the expression to calculate exponential of
     * @return an EXP function string
     */
    public static String exp(final String expression) {
        return function(EXP, expression);
    }

    /**
     * Creates a FLOOR function expression.
     * 
     * @param expression the expression to round down
     * @return a FLOOR function string
     */
    public static String floor(final String expression) {
        return function(FLOOR, expression);
    }

    /**
     * Creates a LOG function expression with specified base.
     * 
     * @param b the logarithm base
     * @param x the value to calculate logarithm of
     * @return a LOG function string
     */
    public static String log(final String b, final String x) {
        return function(LOG, b, x);
    }

    /**
     * Creates an LN (natural logarithm) function expression.
     * 
     * @param expression the expression to calculate natural logarithm of
     * @return an LN function string
     */
    public static String ln(final String expression) {
        return function(LN, expression);
    }

    /**
     * Creates a MOD (modulo) function expression.
     * 
     * @param n1 the dividend
     * @param n2 the divisor
     * @return a MOD function string
     */
    public static String mod(final String n1, final String n2) {
        return function(MOD, n1, n2);
    }

    /**
     * Creates a POWER function expression.
     * 
     * @param n1 the base
     * @param n2 the exponent
     * @return a POWER function string
     */
    public static String power(final String n1, final String n2) {
        return function(POWER, n1, n2);
    }

    /**
     * Creates a SIGN function expression.
     * 
     * @param expression the expression to get sign of
     * @return a SIGN function string
     */
    public static String sign(final String expression) {
        return function(SIGN, expression);
    }

    /**
     * Creates a SIN (sine) function expression.
     * 
     * @param expression the expression to calculate sine of
     * @return a SIN function string
     */
    public static String sin(final String expression) {
        return function(SIN, expression);
    }

    /**
     * Creates a SQRT (square root) function expression.
     * 
     * @param expression the expression to calculate square root of
     * @return a SQRT function string
     */
    public static String sqrt(final String expression) {
        return function(SQRT, expression);
    }

    /**
     * Creates a TAN (tangent) function expression.
     * 
     * @param expression the expression to calculate tangent of
     * @return a TAN function string
     */
    public static String tan(final String expression) {
        return function(TAN, expression);
    }

    /**
     * Creates a CONCAT function expression to concatenate two strings.
     * 
     * @param st1 the first string
     * @param st2 the second string
     * @return a CONCAT function string
     * 
     * <p>Example:</p>
     * <pre>
     * String expr = Expression.concat("firstName", "' '", "lastName");
     * // Returns: "CONCAT(firstName, ' ', lastName)"
     * </pre>
     */
    public static String concat(final String st1, final String st2) {
        return function(CONCAT, st1, st2);
    }

    /**
     * Creates a REPLACE function expression.
     * 
     * @param st the string to search in
     * @param oldString the string to search for
     * @param replacement the replacement string
     * @return a REPLACE function string
     */
    public static String replace(final String st, final String oldString, final String replacement) {
        return function(REPLACE, st, oldString, replacement);
    }

    /**
     * Creates a LENGTH function expression.
     * 
     * @param st the string to get length of
     * @return a LENGTH function string
     */
    public static String stringLength(final String st) {
        return function(LENGTH, st);
    }

    /**
     * Creates a SUBSTR function expression starting from a position.
     * 
     * @param st the string to extract from
     * @param fromIndex the starting position (1-based)
     * @return a SUBSTR function string
     */
    public static String subString(final String st, final int fromIndex) {
        return function(SUBSTR, st, fromIndex);
    }

    /**
     * Creates a SUBSTR function expression with start position and length.
     * 
     * @param st the string to extract from
     * @param fromIndex the starting position (1-based)
     * @param length the number of characters to extract
     * @return a SUBSTR function string
     */
    public static String subString(final String st, final int fromIndex, final int length) {
        return function(SUBSTR, st, fromIndex, length);
    }

    /**
     * Creates a TRIM function expression.
     * 
     * @param st the string to trim
     * @return a TRIM function string
     */
    public static String trim(final String st) {
        return function(TRIM, st);
    }

    /**
     * Creates an LTRIM (left trim) function expression.
     * 
     * @param st the string to left trim
     * @return an LTRIM function string
     */
    public static String lTrim(final String st) {
        return function(LTRIM, st);
    }

    /**
     * Creates an RTRIM (right trim) function expression.
     * 
     * @param st the string to right trim
     * @return an RTRIM function string
     */
    public static String rTrim(final String st) {
        return function(RTRIM, st);
    }

    /**
     * Creates an LPAD (left pad) function expression.
     * 
     * @param st the string to pad
     * @param length the total length after padding
     * @param padStr the string to pad with
     * @return an LPAD function string
     */
    public static String lPad(final String st, final int length, final String padStr) {
        return function(LPAD, st, length, padStr);
    }

    /**
     * Creates an RPAD (right pad) function expression.
     * 
     * @param st the string to pad
     * @param length the total length after padding
     * @param padStr the string to pad with
     * @return an RPAD function string
     */
    public static String rPad(final String st, final int length, final String padStr) {
        return function(RPAD, st, length, padStr);
    }

    /**
     * Creates a LOWER function expression.
     * 
     * @param st the string to convert to lowercase
     * @return a LOWER function string
     */
    public static String lower(final String st) {
        return function(LOWER, st);
    }

    /**
     * Creates an UPPER function expression.
     * 
     * @param st the string to convert to uppercase
     * @return an UPPER function string
     */
    public static String upper(final String st) {
        return function(UPPER, st);
    }

    /**
     * Returns an empty list as expressions have no parameters.
     * 
     * @return an empty list
     */
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * Does nothing as expressions have no parameters to clear.
     */
    @Override
    public void clearParameters() {
        // TODO Auto-generated method stub
    }

    /**
     * Creates a function expression with the given name and arguments.
     * 
     * @param functionName the name of the function
     * @param args the function arguments
     * @return a function expression string
     */
    private static String function(final String functionName, final Object... args) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(functionName);

            sb.append(WD._PARENTHESES_L);

            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(N.stringOf(args[i]));
            }

            sb.append(WD._PARENTHESES_R);

            return sb.toString();

        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Returns the literal string of this expression.
     * The naming policy is ignored for expressions.
     * 
     * @param namingPolicy ignored for expressions
     * @return the literal string
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (literal.length() < 16 && CF.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher(literal).find()) {
            return namingPolicy.convert(literal);
        }

        final List<String> words = SQLParser.parse(literal);
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            String word = null;
            for (int i = 0, len = words.size(); i < len; i++) {
                word = words.get(i);

                if (!Strings.isAsciiAlpha(word.charAt(0)) || SQLParser.isFunctionName(words, len, i)) {
                    sb.append(word);
                } else {
                    sb.append(namingPolicy.convert(word));
                }
            }
            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Computes the hash code based on the literal string.
     * 
     * @return the hash code of the literal
     */
    @Override
    public int hashCode() {
        return (literal == null) ? 0 : literal.hashCode();
    }

    /**
     * Checks if this expression equals another object.
     * Two expressions are equal if they have the same literal string.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(final Object obj) {
        return (this == obj) || (obj instanceof Expression && N.equals(literal, ((Expression) obj).literal));
    }

    /**
     * A simplified alias class for {@link Expression}.
     * <p>
     * This class provides the same functionality as the parent {@code Expression} class
     * but with a more concise class name. It can be used interchangeably with {@code Expression}
     * in contexts where a shorter name is preferred for readability or brevity.
     * <p>
     * Since this class inherits all methods and behavior from {@code Expression}, it can be
     * used in all the same contexts with identical functionality.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Using the parent Expression class
     * Expression expr1 = new Expression("price * quantity");
     * 
     * // Using the Expr alias class
     * Expr expr2 = new Expr("price * quantity");
     * 
     * // Both objects function identically
     * boolean same = expr1.getLiteral().equals(expr2.getLiteral()); // true
     * }</pre>
     *
     * @see Expression
     */
    public static class Expr extends Expression {
        /**
         * Constructs a new Expr (Expression alias) with the specified SQL literal.
         * <p>
         * This constructor creates a new Expression instance using the shortened Expr alias.
         * It passes the provided literal directly to the parent Expression constructor.
         * <p>
         * Example usage:
         * <pre>{@code
         * // Creating an expression with the alias class
         * Expr expr = new Expr("price * quantity");
         * 
         * // The expression works exactly like a regular Expression
         * String result = expr.getLiteral(); // returns "price * quantity"
         * }</pre>
         *
         * @param literal the SQL expression as a string
         * @see Expression#Expression(String)
         */
        Expr(final String literal) {
            super(literal);
        }
    }
}
