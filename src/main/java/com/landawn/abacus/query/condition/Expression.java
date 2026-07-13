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

import static com.landawn.abacus.util.SK.ABS;
import static com.landawn.abacus.util.SK.ACOS;
import static com.landawn.abacus.util.SK.AMPERSAND;
import static com.landawn.abacus.util.SK.ASIN;
import static com.landawn.abacus.util.SK.ASTERISK;
import static com.landawn.abacus.util.SK.ATAN;
import static com.landawn.abacus.util.SK.AVG;
import static com.landawn.abacus.util.SK.CEIL;
import static com.landawn.abacus.util.SK.CIRCUMFLEX;
import static com.landawn.abacus.util.SK.COMMA_SPACE;
import static com.landawn.abacus.util.SK.CONCAT;
import static com.landawn.abacus.util.SK.COS;
import static com.landawn.abacus.util.SK.COUNT;
import static com.landawn.abacus.util.SK.EXP;
import static com.landawn.abacus.util.SK.FLOOR;
import static com.landawn.abacus.util.SK.LENGTH;
import static com.landawn.abacus.util.SK.LN;
import static com.landawn.abacus.util.SK.LOG;
import static com.landawn.abacus.util.SK.LOWER;
import static com.landawn.abacus.util.SK.LPAD;
import static com.landawn.abacus.util.SK.LTRIM;
import static com.landawn.abacus.util.SK.MAX;
import static com.landawn.abacus.util.SK.MIN;
import static com.landawn.abacus.util.SK.MINUS;
import static com.landawn.abacus.util.SK.MOD;
import static com.landawn.abacus.util.SK.PERCENT;
import static com.landawn.abacus.util.SK.PLUS;
import static com.landawn.abacus.util.SK.POWER;
import static com.landawn.abacus.util.SK.REPLACE;
import static com.landawn.abacus.util.SK.RPAD;
import static com.landawn.abacus.util.SK.RTRIM;
import static com.landawn.abacus.util.SK.SIGN;
import static com.landawn.abacus.util.SK.SIN;
import static com.landawn.abacus.util.SK.SLASH;
import static com.landawn.abacus.util.SK.SPACE;
import static com.landawn.abacus.util.SK.SQRT;
import static com.landawn.abacus.util.SK.SUBSTR;
import static com.landawn.abacus.util.SK.SUM;
import static com.landawn.abacus.util.SK.TAN;
import static com.landawn.abacus.util.SK.TRIM;
import static com.landawn.abacus.util.SK.UPPER;
import static com.landawn.abacus.util.SK.VERTICAL_BAR;
import static com.landawn.abacus.util.SK._SINGLE_QUOTE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.SqlParser;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a raw SQL expression that can be used in queries.
 * This class allows the inclusion of arbitrary SQL expressions, functions, and literals
 * in query conditions. It also provides utility methods for building SQL expressions
 * and mathematical/string functions.
 *
 * <p>Expressions are cached for performance optimization. The same expression literal
 * will return the same Expression instance when created through {@link #of(String)}.
 * This helps reduce memory usage and improves performance for frequently used expressions.</p>
 *
 * <p>The class provides numerous static helper methods for creating common SQL expression
 * strings, including arithmetic operations, string functions, mathematical functions, and
 * comparison operations. Most of these helpers return raw SQL fragments as {@link String}
 * (suitable for passing back into {@link #of(String)} or {@link Filters#expr(String)}),
 * not {@code Expression} instances.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple expression
 * Expression expr = Expression.of("price * 0.9");
 * 
 * // Using in a condition
 * Condition discountPrice = Filters.expr("price * 0.9 < 100");
 * 
 * // SQL functions
 * String upperName = Expression.upper("name");
 * String avgPrice = Expression.avg("price");
 * 
 * // Complex expressions (use Expression.of() for column references to avoid quoting)
 * String complex = Expression.plus(Expression.of("base_price"), Expression.of("tax"), Expression.of("shipping"));
 * // Returns: "base_price + tax + shipping"
 * }</pre>
 *
 * @see AbstractCondition
 * @see ComposableCondition
 * @see Filters#expr(String)
 * @see Operator#EMPTY
 */
public class Expression extends ComposableCondition {

    /** Lowercase {@code "null"} literal used when rendering a null value (distinct from the {@code "NULL"} SQL keyword in {@link #NULL_KEYWORD}). */
    static final String NULL_STRING = Strings.NULL;

    private static final String LEFT_SHIFT = "<<";

    private static final String RIGHT_SHIFT = ">>";

    /** SQL keyword rendered as the right-hand side of {@code IS NULL} / {@code IS NOT NULL}. */
    private static final String NULL_KEYWORD = "NULL";

    /** Framework-specific sentinel rendered as the right-hand side of {@code IS BLANK} / {@code IS NOT BLANK}. */
    private static final String BLANK_KEYWORD = "BLANK";

    private static final Map<String, Expression> cachedExpression = new ConcurrentHashMap<>();

    private static final Set<String> SQL_KEY_WORDS = N.newHashSet(1024);

    static {
        final Field[] fields = SK.class.getDeclaredFields();

        for (final Field field : fields) {
            final int modifiers = field.getModifiers();

            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType().equals(String.class)) {
                try {
                    final String value = (String) field.get(null);

                    for (final String e : Strings.split(value, ' ', true)) {
                        registerSqlKeyword(e);
                    }
                } catch (final Exception e) {
                    // ignore, should never happen.
                }
            }
        }

        registerSqlKeyword("CURRENT_DATE");
        registerSqlKeyword("CURRENT_TIME");
        registerSqlKeyword("CURRENT_TIMESTAMP");
        registerSqlKeyword("CURRENT_USER");
        registerSqlKeyword("LOCALTIME");
        registerSqlKeyword("LOCALTIMESTAMP");
        registerSqlKeyword("SESSION_USER");
        registerSqlKeyword("SYSTEM_USER");
    }

    // For Kryo
    final String literal;

    /** Lazily memoized {@link SqlParser#parse(String)} result for {@link #literal} (performance only). */
    private transient List<String> cachedParsedLiteral;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Expression instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Expression() {
        literal = null;
    }

    /**
     * Constructs a new {@code Expression} with the specified SQL literal.
     * The literal can contain any valid SQL expression, including functions, operators,
     * column references, and complex expressions.
     *
     * <p>Note: For frequently used expressions, prefer {@link #of(String)} instead,
     * which provides instance caching for better performance and memory efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr1 = new Expression("CURRENT_TIMESTAMP");
     * Expression expr2 = new Expression("price * quantity");
     * Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END");
     * Expression expr4 = new Expression("COALESCE(middle_name, '')");
     * // expr4.toString() returns: "COALESCE(middle_name, '')"
     * }</pre>
     *
     * @param literal the SQL expression as a string (must not be {@code null})
     * @throws IllegalArgumentException if {@code literal} is {@code null}
     */
    public Expression(final String literal) {
        super(Operator.EMPTY);

        if (literal == null) {
            throw new IllegalArgumentException("literal must not be null");
        }

        this.literal = literal;
    }

    /**
     * Returns the SQL literal string of this expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr = new Expression("price * quantity");
     * String literal = expr.literal();   // Returns "price * quantity"
     *
     * Expression expr2 = Expression.of("CURRENT_TIMESTAMP");
     * String literal2 = expr2.literal();   // Returns "CURRENT_TIMESTAMP"
     * }</pre>
     *
     * @return the SQL expression string; never {@code null} for instances created via the public
     *         constructor or {@link #of(String)}, but may be {@code null} for uninitialized instances
     *         produced by the package-private default constructor (e.g., during Kryo deserialization)
     */
    public String literal() {
        return literal;
    }

    /**
     * Creates or retrieves a cached Expression instance for the given literal.
     * This method uses caching to ensure that expressions with the same literal
     * share the same instance, improving memory efficiency and performance.
     *
     * <p>Note: the cache is unbounded and retains every distinct literal for the lifetime of the
     * JVM, so prefer the {@link #Expression(String) constructor} for dynamically-generated literals.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr1 = Expression.of("CURRENT_DATE");
     * Expression expr2 = Expression.of("CURRENT_DATE");
     * // expr1 == expr2 (same instance due to caching)
     * 
     * Expression calc = Expression.of("price * 1.1");
     * // Reuse the same expression in multiple places
     * }</pre>
     *
     * @param literal the SQL expression string (must not be {@code null})
     * @return a cached or newly created Expression instance for the given literal
     * @throws IllegalArgumentException if {@code literal} is {@code null}
     */
    public static Expression of(final String literal) {
        if (literal == null) {
            throw new IllegalArgumentException("literal must not be null");
        }

        return cachedExpression.computeIfAbsent(literal, Expression::new);
    }

    /**
     * Creates an equality expression between a literal and a value.
     * This is useful for building dynamic SQL conditions programmatically.
     * If {@code value} is {@code null}, the result is rendered as {@code "literal IS NULL"} instead of {@code "literal = null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.equal("age", 25);
     * // Returns: "age = 25"
     *
     * String expr2 = Expression.equal("status", "active");
     * // Returns: "status = 'active'"
     *
     * String expr3 = Expression.equal("middle_name", null);
     * // Returns: "middle_name IS NULL"
     * }</pre>
     *
     * @param expr the left-hand side of the equality
     * @param value the right-hand side value; may be {@code null} (renders as {@code IS NULL})
     * @return a string representation of the equality expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String equal(final String expr, final Object value) { //NOSONAR
        return link(Operator.EQUAL, expr, value);
    }

    /**
     * Creates an equality expression between a literal and a value.
     * Alias for {@link #equal(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.eq("user_id", 123);
     * // Returns: "user_id = 123"
     * }</pre>
     *
     * @param expr the left-hand side of the equality
     * @param value the right-hand side value; may be {@code null} (renders as {@code IS NULL})
     * @return a string representation of the equality expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String eq(final String expr, final Object value) {
        return equal(expr, value);
    }

    /**
     * Creates a not-equal expression between a literal and a value.
     * If {@code value} is {@code null}, the result is rendered as {@code "literal IS NOT NULL"} instead of {@code "literal != null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.notEqual("status", "INACTIVE");
     * // Returns: "status != 'INACTIVE'"
     *
     * String expr2 = Expression.notEqual("count", 0);
     * // Returns: "count != 0"
     *
     * String expr3 = Expression.notEqual("email", null);
     * // Returns: "email IS NOT NULL"
     * }</pre>
     *
     * @param expr the left-hand side of the inequality
     * @param value the right-hand side value; may be {@code null} (renders as {@code IS NOT NULL})
     * @return a string representation of the not-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String notEqual(final String expr, final Object value) {
        return link(Operator.NOT_EQUAL, expr, value);
    }

    /**
     * Creates a not-equal expression between a literal and a value.
     * Alias for {@link #notEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ne("type", "guest");
     * // Returns: "type != 'guest'"
     * }</pre>
     *
     * @param expr the left-hand side of the inequality
     * @param value the right-hand side value; may be {@code null} (renders as {@code IS NOT NULL})
     * @return a string representation of the not-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String ne(final String expr, final Object value) {
        return notEqual(expr, value);
    }

    /**
     * Creates a greater-than expression between a literal and a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.greaterThan("salary", 50000);
     * // Returns: "salary > 50000"
     * 
     * String expr2 = Expression.greaterThan("created_date", "2024-01-01");
     * // Returns: "created_date > '2024-01-01'"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the greater-than expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String greaterThan(final String expr, final Object value) {
        return link(Operator.GREATER_THAN, expr, value);
    }

    /**
     * Creates a greater-than expression between a literal and a value.
     * Alias for {@link #greaterThan(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.gt("age", 18);
     * // Returns: "age > 18"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the greater-than expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String gt(final String expr, final Object value) {
        return greaterThan(expr, value);
    }

    /**
     * Creates a greater-than-or-equal expression between a literal and a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.greaterThanOrEqual("score", 60);
     * // Returns: "score >= 60"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the greater-than-or-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String greaterThanOrEqual(final String expr, final Object value) {
        return link(Operator.GREATER_THAN_OR_EQUAL, expr, value);
    }

    /**
     * Creates a greater-than-or-equal expression between a literal and a value.
     * Alias for {@link #greaterThanOrEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ge("quantity", 1);
     * // Returns: "quantity >= 1"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the greater-than-or-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String ge(final String expr, final Object value) {
        return greaterThanOrEqual(expr, value);
    }

    /**
     * Creates a less-than expression between a literal and a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lessThan("price", 100);
     * // Returns: "price < 100"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the less-than expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String lessThan(final String expr, final Object value) {
        return link(Operator.LESS_THAN, expr, value);
    }

    /**
     * Creates a less-than expression between a literal and a value.
     * Alias for {@link #lessThan(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lt("stock", 10);
     * // Returns: "stock < 10"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the less-than expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String lt(final String expr, final Object value) {
        return lessThan(expr, value);
    }

    /**
     * Creates a less-than-or-equal expression between a literal and a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lessThanOrEqual("discount", 50);
     * // Returns: "discount <= 50"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the less-than-or-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String lessThanOrEqual(final String expr, final Object value) {
        return link(Operator.LESS_THAN_OR_EQUAL, expr, value);
    }

    /**
     * Creates a less-than-or-equal expression between a literal and a value.
     * Alias for {@link #lessThanOrEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.le("temperature", 32);
     * // Returns: "temperature <= 32"
     * }</pre>
     *
     * @param expr the left-hand side of the comparison
     * @param value the right-hand side value; should not be {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the less-than-or-equal expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    @Beta
    public static String le(final String expr, final Object value) {
        return lessThanOrEqual(expr, value);
    }

    /**
     * Creates a BETWEEN expression for a literal with min and max values.
     * The BETWEEN operator is inclusive on both ends.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.between("age", 18, 65);
     * // Returns: "age BETWEEN 18 AND 65"
     * 
     * String expr2 = Expression.between("price", 10.0, 50.0);
     * // Returns: "price BETWEEN 10.0 AND 50.0"
     * }</pre>
     *
     * @param expr the expression to test
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a string representation of the BETWEEN expression
     * @throws IllegalArgumentException if {@code minValue} or {@code maxValue} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String between(final String expr, final Object minValue, final Object maxValue) {
        return link(Operator.BETWEEN, expr, minValue, maxValue);
    }

    // Removed: bt(String, Object, Object) - non-standard abbreviation.
    // Use between(String, Object, Object) instead.

    /**
     * Creates a NOT BETWEEN expression for a literal with min and max values.
     * A value satisfies {@code NOT BETWEEN min AND max} when it is strictly less than {@code min}
     * or strictly greater than {@code max}, so both ends of the range are excluded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.notBetween("age", 18, 65);
     * // Returns: "age NOT BETWEEN 18 AND 65"
     *
     * String expr2 = Expression.notBetween("price", 10.0, 50.0);
     * // Returns: "price NOT BETWEEN 10.0 AND 50.0"
     * }</pre>
     *
     * @param expr the expression to test
     * @param minValue the lower bound of the excluded range (inclusive)
     * @param maxValue the upper bound of the excluded range (inclusive)
     * @return a string representation of the NOT BETWEEN expression
     * @throws IllegalArgumentException if {@code min} or {@code max} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String notBetween(final String expr, final Object minValue, final Object maxValue) {
        return link(Operator.NOT_BETWEEN, expr, minValue, maxValue);
    }

    /**
     * Creates a LIKE expression for pattern matching.
     * Use SQL wildcards: % for any sequence of characters, _ for any single character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.like("name", "John%");
     * // Returns: "name LIKE 'John%'"
     * 
     * String expr2 = Expression.like("email", "%@gmail.com");
     * // Returns: "email LIKE '%@gmail.com'"
     * 
     * String expr3 = Expression.like("code", "A__");
     * // Returns: "code LIKE 'A__'" (matches 'A' followed by exactly 2 characters)
     * }</pre>
     *
     * @param expr the expression to match
     * @param value the pattern to match against (can include % and _ wildcards); should not be
     *              {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the LIKE expression
     */
    public static String like(final String expr, final String value) {
        return link(Operator.LIKE, expr, value);
    }

    /**
     * Creates a NOT LIKE expression for pattern matching.
     * Use SQL wildcards: % for any sequence of characters, _ for any single character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.notLike("name", "John%");
     * // Returns: "name NOT LIKE 'John%'"
     *
     * String expr2 = Expression.notLike("email", "%@gmail.com");
     * // Returns: "email NOT LIKE '%@gmail.com'"
     * }</pre>
     *
     * @param expr the expression to match
     * @param value the pattern to exclude (can include % and _ wildcards); should not be
     *              {@code null} — a {@code null} renders as the literal {@code null}
     * @return a string representation of the NOT LIKE expression
     */
    public static String notLike(final String expr, final String value) {
        return link(Operator.NOT_LIKE, expr, value);
    }

    /**
     * Creates an IS NULL expression for the specified literal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.isNull("email");          // Returns: "email IS NULL"
     * String expr2 = Expression.isNull("middle_name");   // Returns: "middle_name IS NULL"
     * }</pre>
     *
     * @param expr the expression to check for null
     * @return a string representation of the IS NULL expression
     */
    public static String isNull(final String expr) {
        return link2(Operator.IS, expr, NULL_KEYWORD);
    }

    /**
     * Creates an IS NOT NULL expression for the specified literal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.isNotNull("email");    // Returns: "email IS NOT NULL"
     * String expr2 = Expression.isNotNull("phone");   // Returns: "phone IS NOT NULL"
     * }</pre>
     *
     * @param expr the expression to check for not null
     * @return a string representation of the IS NOT NULL expression
     */
    public static String isNotNull(final String expr) {
        return link2(Operator.IS_NOT, expr, NULL_KEYWORD);
    }

    /**
     * Creates a framework-specific {@code IS BLANK} expression for the specified literal,
     * which the query engine interprets as a combined null-or-empty check.
     * This is not standard SQL; the generated string uses the token {@code "BLANK"}
     * as a special sentinel understood by this framework's SQL parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.isNullOrEmpty("description");   // Returns: "description IS BLANK"
     * String expr2 = Expression.isNullOrEmpty("address");      // Returns: "address IS BLANK"
     * }</pre>
     *
     * @param expr the column reference or expression to check
     * @return a framework-specific {@code IS BLANK} expression string
     */
    public static String isNullOrEmpty(final String expr) {
        return link2(Operator.IS, expr, BLANK_KEYWORD);
    }

    /**
     * Creates a framework-specific {@code IS NOT BLANK} expression for the specified literal,
     * which the query engine interprets as a combined not-null-and-not-empty check.
     * This is not standard SQL; the generated string uses the token {@code "BLANK"}
     * as a special sentinel understood by this framework's SQL parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.isNotNullAndNotEmpty("name");       // Returns: "name IS NOT BLANK"
     * String expr2 = Expression.isNotNullAndNotEmpty("comment");   // Returns: "comment IS NOT BLANK"
     * }</pre>
     *
     * @param expr the column reference or expression to check
     * @return a framework-specific {@code IS NOT BLANK} expression string
     */
    public static String isNotNullAndNotEmpty(final String expr) {
        return link2(Operator.IS_NOT, expr, BLANK_KEYWORD);
    }

    /**
     * Creates an AND expression combining multiple literals.
     * All conditions must be true for the AND expression to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.and("active = true", "age > 18", "status = 'APPROVED'");
     * // Returns: "active = true AND age > 18 AND status = 'APPROVED'"
     * 
     * String expr2 = Expression.and("verified = 1", "email IS NOT NULL");
     * // Returns: "verified = 1 AND email IS NOT NULL"
     * }</pre>
     *
     * @param exprs the expressions to combine with AND
     * @return a string representation of the AND expression
     */
    public static String and(final String... exprs) {
        return link2(Operator.AND, exprs);
    }

    /**
     * Creates an OR expression combining multiple literals.
     * At least one condition must be true for the OR expression to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.or("status = 'active'", "status = 'pending'", "priority = 1");
     * // Returns: "status = 'active' OR status = 'pending' OR priority = 1"
     * }</pre>
     *
     * @param exprs the expressions to combine with OR
     * @return a string representation of the OR expression
     */
    public static String or(final String... exprs) {
        return link2(Operator.OR, exprs);
    }

    /**
     * Creates an addition expression for the given objects.
     * Concatenates all values with the + operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.plus(Expression.of("price"), Expression.of("tax"), Expression.of("shipping"));
     * // Returns: "price + tax + shipping"
     *
     * String expr2 = Expression.plus(Expression.of("base_salary"), 5000, Expression.of("bonus"));
     * // Returns: "base_salary + 5000 + bonus"
     * }</pre>
     *
     * @param operands the values to add
     * @return a string representation of the addition expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String plus(final Object... operands) {
        return link(PLUS, operands);
    }

    /**
     * Creates a subtraction expression for the given objects.
     * Subtracts each subsequent value from the first.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.subtract(Expression.of("total"), Expression.of("discount"), Expression.of("tax_credit"));
     * // Returns: "total - discount - tax_credit"
     *
     * String expr2 = Expression.subtract(Expression.of("price"), 10);
     * // Returns: "price - 10"
     * }</pre>
     *
     * @param operands the values to subtract
     * @return a string representation of the subtraction expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String subtract(final Object... operands) {
        return link(MINUS, operands);
    }

    /**
     * Creates a subtraction expression for the given objects.
     * This is a deprecated alias for {@link #subtract(Object...)} and produces identical output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.minus(Expression.of("total"), Expression.of("discount"));
     * // Returns: "total - discount"
     *
     * String expr2 = Expression.minus(Expression.of("price"), 10);
     * // Returns: "price - 10"
     *
     * String none = Expression.minus();
     * // Returns: "" (no operands)
     *
     * Expression.minus(Expression.of("x"), Double.NaN);
     * // throws IllegalArgumentException (NaN has no portable SQL literal)
     * }</pre>
     *
     * @param objects the values to subtract
     * @return a string representation of the subtraction expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     * @deprecated Use {@link #subtract(Object...)} instead to avoid confusion with the SQL {@code MINUS} set operation.
     */
    @Deprecated
    public static String minus(final Object... objects) {
        return subtract(objects);
    }

    /**
     * Creates a multiplication expression for the given objects.
     * Multiplies all values together.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.multiply(Expression.of("price"), Expression.of("quantity"), Expression.of("tax_rate"));
     * // Returns: "price * quantity * tax_rate"
     *
     * String expr2 = Expression.multiply(Expression.of("hours"), 60);
     * // Returns: "hours * 60"
     * }</pre>
     *
     * @param operands the values to multiply
     * @return a string representation of the multiplication expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String multiply(final Object... operands) {
        return link(ASTERISK, operands);
    }

    /**
     * Creates a division expression for the given objects.
     * Divides the first value by each subsequent value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.divide(Expression.of("total"), Expression.of("count"));
     * // Returns: "total / count"
     *
     * String expr2 = Expression.divide(Expression.of("distance"), Expression.of("time"), 60);
     * // Returns: "distance / time / 60"
     * }</pre>
     *
     * @param objects the values to divide
     * @return a string representation of the division expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String divide(final Object... objects) {
        return link(SLASH, objects);
    }

    /**
     * Creates a modulus expression for the given objects.
     * Returns the remainder of division operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.modulus(Expression.of("value"), 10);
     * // Returns: "value % 10"
     *
     * String expr2 = Expression.modulus(Expression.of("id"), Expression.of("batch_size"));
     * // Returns: "id % batch_size"
     * }</pre>
     *
     * @param objects the values for modulus operation
     * @return a string representation of the modulus expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String modulus(final Object... objects) {
        return link(PERCENT, objects);
    }

    /**
     * Creates a left shift expression for the given objects.
     * Shifts bits to the left.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.leftShift(Expression.of("flags"), 2);
     * // Returns: "flags << 2"
     * }</pre>
     *
     * @param objects the values for left shift operation
     * @return a string representation of the left shift expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String leftShift(final Object... objects) {
        return link(LEFT_SHIFT, objects);
    }

    /**
     * Creates a right shift expression for the given objects.
     * Shifts bits to the right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.rightShift(Expression.of("value"), 4);
     * // Returns: "value >> 4"
     * }</pre>
     *
     * @param objects the values for right shift operation
     * @return a string representation of the right shift expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String rightShift(final Object... objects) {
        return link(RIGHT_SHIFT, objects);
    }

    /**
     * Creates a bitwise AND expression for the given objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.bitwiseAnd(Expression.of("permissions"), Expression.of("mask"));
     * // Returns: "permissions & mask"
     *
     * String expr2 = Expression.bitwiseAnd(Expression.of("flags"), 0xFF);
     * // Returns: "flags & 255"
     * }</pre>
     *
     * @param objects the values for bitwise AND operation
     * @return a string representation of the bitwise AND expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String bitwiseAnd(final Object... objects) {
        return link(AMPERSAND, objects);
    }

    /**
     * Creates a bitwise OR expression for the given objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.bitwiseOr(Expression.of("flags1"), Expression.of("flags2"));
     * // Returns: "flags1 | flags2"
     * }</pre>
     *
     * @param objects the values for bitwise OR operation
     * @return a string representation of the bitwise OR expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String bitwiseOr(final Object... objects) {
        return link(VERTICAL_BAR, objects);
    }

    /**
     * Creates a bitwise XOR expression for the given objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use Expression.of() for column references to avoid single-quote wrapping
     * String expr = Expression.bitwiseXor(Expression.of("value1"), Expression.of("value2"));
     * // Returns: "value1 ^ value2"
     * }</pre>
     *
     * @param objects the values for bitwise XOR operation
     * @return a string representation of the bitwise XOR expression
     * @throws IllegalArgumentException if any value is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    public static String bitwiseXor(final Object... objects) {
        return link(CIRCUMFLEX, objects);
    }

    /**
     * Links a literal with a value using the specified operator.
     *
     * <p>If {@code value} is {@code null}, this method substitutes a null-aware form:
     * {@link Operator#EQUAL} renders as {@code "literal IS NULL"}, while
     * {@link Operator#NOT_EQUAL} and {@link Operator#NOT_EQUAL_ANSI} render as
     * {@code "literal IS NOT NULL"}. For all other operators (or non-null values)
     * the value is rendered via {@link #renderValue(Object)}.</p>
     *
     * @param operator the operator to use
     * @param literal the left-hand side literal
     * @param value the right-hand side value; may be {@code null}
     * @return a string representation of the linked expression
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    static String link(final Operator operator, final String literal, final Object value) {
        if (value == null) {
            if (operator == Operator.EQUAL) {
                return isNull(literal);
            } else if (operator == Operator.NOT_EQUAL || operator == Operator.NOT_EQUAL_ANSI) {
                return isNotNull(literal);
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(SK._SPACE);
            sb.append(operator.sqlToken());
            sb.append(SK._SPACE);
            sb.append(renderValue(value));

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Renders a range expression of the form {@code "literal <op> min AND max"}.
     * Used by {@link #between(String, Object, Object)} (with {@link Operator#BETWEEN}); the
     * connector between {@code min} and {@code max} is always the literal {@code AND}.
     * Both {@code min} and {@code max} are rendered via {@link #renderValue(Object)}.
     *
     * @param operator the range operator (typically {@link Operator#BETWEEN})
     * @param literal the left-hand side literal
     * @param min the lower bound value
     * @param max the upper bound value
     * @return the rendered SQL fragment
     * @throws IllegalArgumentException if {@code min} or {@code max} is a {@code NaN} or infinite
     *             {@link Float}/{@link Double}
     */
    static String link(final Operator operator, final String literal, final Object min, final Object max) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(SK._SPACE);
            sb.append(operator.sqlToken());
            sb.append(SK._SPACE);
            sb.append(renderValue(min));
            sb.append(SK._SPACE);
            sb.append(SK.AND);
            sb.append(SK._SPACE);
            sb.append(renderValue(max));

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Renders an expression of the form {@code "literal <op> postfix"} with the postfix
     * appended verbatim (no quoting or escaping). Used to build {@code IS NULL},
     * {@code IS NOT NULL}, {@code IS BLANK}, and {@code IS NOT BLANK} expressions where
     * the right-hand side is a SQL keyword rather than a value.
     *
     * @param operator the operator whose {@link Operator#sqlToken() sqlToken} appears between the literal and the postfix
     * @param literal the left-hand side literal
     * @param operatorPostfix the literal keyword/token appended after the operator (emitted verbatim)
     * @return the rendered SQL fragment
     */
    static String link2(final Operator operator, final String literal, final String operatorPostfix) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(SK._SPACE);
            sb.append(operator.sqlToken());
            sb.append(SK._SPACE);
            sb.append(operatorPostfix);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins multiple literals using the specified operator's SQL token as the separator.
     * Each separator is surrounded by spaces (e.g. {@code " AND " }). If {@code literals}
     * contains a single element, that element is returned with no operator appended; an
     * empty array yields an empty string.
     *
     * @param operator the operator whose {@link Operator#sqlToken() sqlToken} is used as the separator
     * @param literals the literals to join
     * @return the joined string
     */
    static String link2(final Operator operator, final String... literals) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < literals.length; i++) {
                if (i > 0) {
                    sb.append(SK._SPACE);
                    sb.append(operator.sqlToken());
                    sb.append(SK._SPACE);
                }

                sb.append(literals[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the SQL representations of multiple objects using the given symbol.
     * Each object is rendered through {@link #renderValue(Object)}. The {@code linkedSymbol}
     * is automatically padded with surrounding spaces unless it is already
     * {@link com.landawn.abacus.util.SK#SPACE} or {@link com.landawn.abacus.util.SK#COMMA_SPACE}.
     *
     * @param linkedSymbol the symbol to use for linking (e.g. {@code "+"}, {@code "*"}, {@code "&"})
     * @param objects the objects to link
     * @return the joined SQL expression string
     * @throws IllegalArgumentException if any object is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     */
    static String link(String linkedSymbol, final Object... objects) {
        if (!(SPACE.equals(linkedSymbol) || COMMA_SPACE.equals(linkedSymbol))) {
            linkedSymbol = SK._SPACE + linkedSymbol + SK._SPACE;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < objects.length; i++) {
                if (i > 0) {
                    sb.append(linkedSymbol);
                }

                sb.append(renderValue(objects[i]));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Converts a value to its SQL representation.
     * This method performs SQL escaping and formatting:
     * <ul>
     *   <li>{@code null} values become the string {@code "null"}</li>
     *   <li>Strings are wrapped in single quotes and escaped via {@link AbstractCondition#escapeStringLiteral(String)}:
     *       embedded unescaped single and double quotes are backslash-escaped ({@code '} becomes {@code \'}, {@code "} becomes {@code \"});
     *       a backslash shields the character that follows it, so any existing {@code \x} pair — including an
     *       already-escaped quote such as {@code \'} — is copied verbatim rather than escaped again, plus a defensive
     *       guard that appends one extra backslash when the body would otherwise end in an unescaped trailing backslash</li>
     *   <li>{@link Number} and {@link Boolean} values are converted via {@code toString()} (no quoting);
     *       {@code NaN}/infinite {@link Float}/{@link Double} values are rejected</li>
     *   <li>{@link Expression} objects return their literal SQL text (or {@code "null"} if the literal is {@code null})</li>
     *   <li>{@link SubQuery} instances render their {@code toString()} wrapped in parentheses; other {@link Condition}s use their {@code toString()} verbatim</li>
     *   <li>Other objects are converted via {@link N#stringOf(Object)}, then quoted and escaped</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression.renderValue("text");                      // returns "'text'"
     * Expression.renderValue("O'Brien");                   // returns "'O\'Brien'" (single quote backslash-escaped)
     * Expression.renderValue("say \"hi\"");                // returns "'say \"hi\"'" (double quote backslash-escaped)
     * Expression.renderValue(123);                         // returns "123"
     * Expression.renderValue(45.67);                       // returns "45.67"
     * Expression.renderValue(null);                        // returns "null"
     * Expression.renderValue(true);                        // returns "true"
     * Expression.renderValue(false);                       // returns "false"
     * Expression.renderValue(new Expression("COUNT(*)"));  // returns "COUNT(*)" (the expression's literal)
     * Expression.renderValue(Double.NaN);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param value the value to render
     * @return the SQL representation of the value
     * @throws IllegalArgumentException if {@code value} is a {@link Float} or {@link Double} that is {@code NaN} or infinite
     *             (these have no portable SQL literal form; use {@link IsNaN}/{@link IsInfinite} instead)
     */
    public static String renderValue(final Object value) {
        if (value == null) {
            return NULL_STRING;
        }

        if (value instanceof String) {
            return (_SINGLE_QUOTE + AbstractCondition.escapeStringLiteral((String) value) + _SINGLE_QUOTE);
        } else if (value instanceof Number || value instanceof Boolean) {
            AbstractCondition.checkFiniteNumber(value);
            return value.toString();
        } else if (value instanceof Expression) {
            final String exprLiteral = ((Expression) value).literal();
            return exprLiteral != null ? exprLiteral : NULL_STRING;
        } else if (value instanceof Condition) {
            final String conditionStr = value.toString();

            if (value instanceof SubQuery) {
                return SK.PARENTHESIS_L + conditionStr + SK.PARENTHESIS_R;
            }

            return conditionStr;
        } else {
            return (_SINGLE_QUOTE + AbstractCondition.escapeStringLiteral(N.stringOf(value)) + _SINGLE_QUOTE);
        }
    }

    /**
     * Creates a COUNT function expression.
     * COUNT returns the number of rows that match the criteria.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.count("*");                      // Returns: "COUNT(*)"
     * String expr2 = Expression.count("id");                    // Returns: "COUNT(id)"
     * String expr3 = Expression.count("DISTINCT department");   // Returns: "COUNT(DISTINCT department)"
     * }</pre>
     *
     * @param expr the expression to count
     * @return a COUNT function string
     */
    public static String count(final String expr) {
        return function(COUNT, expr);
    }

    /**
     * Creates an AVG (average) function expression.
     * AVG returns the average value of a numeric column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.avg("salary");   // Returns: "AVG(salary)"
     * String expr2 = Expression.avg("age");     // Returns: "AVG(age)"
     * }</pre>
     *
     * @param expr the expression to average
     * @return an AVG function string
     */
    public static String avg(final String expr) {
        return function(AVG, expr);
    }

    /**
     * Creates a SUM function expression.
     * SUM returns the total sum of a numeric column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.sum("amount");              // Returns: "SUM(amount)"
     * String expr2 = Expression.sum("quantity * price");   // Returns: "SUM(quantity * price)"
     * }</pre>
     *
     * @param expr the expression to sum
     * @return a SUM function string
     */
    public static String sum(final String expr) {
        return function(SUM, expr);
    }

    /**
     * Creates a MIN function expression.
     * MIN returns the smallest value in a set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.min("price");           // Returns: "MIN(price)"
     * String expr2 = Expression.min("created_date");   // Returns: "MIN(created_date)"
     * }</pre>
     *
     * @param expr the expression to find minimum
     * @return a MIN function string
     */
    public static String min(final String expr) {
        return function(MIN, expr);
    }

    /**
     * Creates a MAX function expression.
     * MAX returns the largest value in a set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.max("score");         // Returns: "MAX(score)"
     * String expr2 = Expression.max("last_login");   // Returns: "MAX(last_login)"
     * }</pre>
     *
     * @param expr the expression to find maximum
     * @return a MAX function string
     */
    public static String max(final String expr) {
        return function(MAX, expr);
    }

    /**
     * Creates an ABS (absolute value) function expression.
     * ABS returns the absolute (positive) value of a number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.abs("balance");        // Returns: "ABS(balance)"
     * String expr2 = Expression.abs("temperature");   // Returns: "ABS(temperature)"
     * }</pre>
     *
     * @param expr the expression to get absolute value of
     * @return an ABS function string
     */
    public static String abs(final String expr) {
        return function(ABS, expr);
    }

    /**
     * Creates an ACOS (arc cosine) function expression.
     * ACOS returns the angle (in radians) whose cosine is the given number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.acos("0.5");             // Returns: "ACOS(0.5)"
     * String expr2 = Expression.acos("cosine_value");   // Returns: "ACOS(cosine_value)"
     * }</pre>
     *
     * @param expr the expression to calculate arc cosine of
     * @return an ACOS function string
     */
    public static String acos(final String expr) {
        return function(ACOS, expr);
    }

    /**
     * Creates an ASIN (arc sine) function expression.
     * ASIN returns the angle (in radians) whose sine is the given number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.asin("0.5");           // Returns: "ASIN(0.5)"
     * String expr2 = Expression.asin("sine_value");   // Returns: "ASIN(sine_value)"
     * }</pre>
     *
     * @param expr the expression to calculate arc sine of
     * @return an ASIN function string
     */
    public static String asin(final String expr) {
        return function(ASIN, expr);
    }

    /**
     * Creates an ATAN (arc tangent) function expression.
     * ATAN returns the angle (in radians) whose tangent is the given number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.atan("1");                // Returns: "ATAN(1)"
     * String expr2 = Expression.atan("tangent_value");   // Returns: "ATAN(tangent_value)"
     * }</pre>
     *
     * @param expr the expression to calculate arc tangent of
     * @return an ATAN function string
     */
    public static String atan(final String expr) {
        return function(ATAN, expr);
    }

    /**
     * Creates a CEIL (ceiling) function expression.
     * CEIL returns the smallest integer greater than or equal to a number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ceil("4.2");      // Returns: "CEIL(4.2)"
     * String expr2 = Expression.ceil("price");   // Returns: "CEIL(price)"
     * }</pre>
     *
     * @param expr the expression to round up
     * @return a CEIL function string
     */
    public static String ceil(final String expr) {
        return function(CEIL, expr);
    }

    /**
     * Creates a COS (cosine) function expression.
     * COS returns the cosine of an angle in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.cos("3.14159");   // Returns: "COS(3.14159)"
     * String expr2 = Expression.cos("angle");    // Returns: "COS(angle)"
     * }</pre>
     *
     * @param expr the expression to calculate cosine of
     * @return a COS function string
     */
    public static String cos(final String expr) {
        return function(COS, expr);
    }

    /**
     * Creates an EXP (exponential) function expression.
     * EXP returns e raised to the power of the given number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.exp("2");              // Returns: "EXP(2)"
     * String expr2 = Expression.exp("growth_rate");   // Returns: "EXP(growth_rate)"
     * }</pre>
     *
     * @param expr the expression to calculate exponential of
     * @return an EXP function string
     */
    public static String exp(final String expr) {
        return function(EXP, expr);
    }

    /**
     * Creates a FLOOR function expression.
     * FLOOR returns the largest integer less than or equal to a number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.floor("4.8");        // Returns: "FLOOR(4.8)"
     * String expr2 = Expression.floor("average");   // Returns: "FLOOR(average)"
     * }</pre>
     *
     * @param expr the expression to round down
     * @return a FLOOR function string
     */
    public static String floor(final String expr) {
        return function(FLOOR, expr);
    }

    /**
     * Creates a LOG function expression with specified base.
     * LOG returns the logarithm of a number to the specified base.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.log("10", "100");     // Returns: "LOG(10, 100)"
     * String expr2 = Expression.log("2", "value");   // Returns: "LOG(2, value)"
     * }</pre>
     *
     * @param base the logarithm base
     * @param value the value to calculate logarithm of
     * @return a LOG function string
     */
    public static String log(final String base, final String value) {
        return function(LOG, base, value);
    }

    /**
     * Creates an LN (natural logarithm) function expression.
     * LN returns the natural logarithm (base e) of a number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ln("10");       // Returns: "LN(10)"
     * String expr2 = Expression.ln("value");   // Returns: "LN(value)"
     * }</pre>
     *
     * @param expr the expression to calculate natural logarithm of
     * @return an LN function string
     */
    public static String ln(final String expr) {
        return function(LN, expr);
    }

    /**
     * Creates a MOD (modulo) function expression.
     * MOD returns the remainder of division.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.mod("10", "3");             // Returns: "MOD(10, 3)"
     * String expr2 = Expression.mod("id", "batch_size");   // Returns: "MOD(id, batch_size)"
     * }</pre>
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @return a MOD function string
     */
    public static String mod(final String dividend, final String divisor) {
        return function(MOD, dividend, divisor);
    }

    /**
     * Creates a POWER function expression.
     * POWER returns a number raised to the power of another number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.power("2", "10");             // Returns: "POWER(2, 10)"
     * String expr2 = Expression.power("base", "exponent");   // Returns: "POWER(base, exponent)"
     * }</pre>
     *
     * @param base the base
     * @param exponent the exponent
     * @return a POWER function string
     */
    public static String power(final String base, final String exponent) {
        return function(POWER, base, exponent);
    }

    /**
     * Creates a SIGN function expression.
     * SIGN returns -1, 0, or 1 depending on the sign of the number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.sign("-5");         // Returns: "SIGN(-5)"
     * String expr2 = Expression.sign("balance");   // Returns: "SIGN(balance)"
     * }</pre>
     *
     * @param expr the expression to get sign of
     * @return a SIGN function string
     */
    public static String sign(final String expr) {
        return function(SIGN, expr);
    }

    /**
     * Creates a SIN (sine) function expression.
     * SIN returns the sine of an angle in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.sin("1.5708");   // Returns: "SIN(1.5708)"
     * String expr2 = Expression.sin("angle");   // Returns: "SIN(angle)"
     * }</pre>
     *
     * @param expr the expression to calculate sine of
     * @return a SIN function string
     */
    public static String sin(final String expr) {
        return function(SIN, expr);
    }

    /**
     * Creates a SQRT (square root) function expression.
     * SQRT returns the square root of a number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.sqrt("16");      // Returns: "SQRT(16)"
     * String expr2 = Expression.sqrt("area");   // Returns: "SQRT(area)"
     * }</pre>
     *
     * @param expr the expression to calculate square root of
     * @return a SQRT function string
     */
    public static String sqrt(final String expr) {
        return function(SQRT, expr);
    }

    /**
     * Creates a TAN (tangent) function expression.
     * TAN returns the tangent of an angle in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.tan("0.7854");   // Returns: "TAN(0.7854)"
     * String expr2 = Expression.tan("angle");   // Returns: "TAN(angle)"
     * }</pre>
     *
     * @param expr the expression to calculate tangent of
     * @return a TAN function string
     */
    public static String tan(final String expr) {
        return function(TAN, expr);
    }

    /**
     * Creates a CONCAT function expression that concatenates two operands.
     * The two arguments are emitted verbatim inside {@code CONCAT(...)}; pass a column
     * reference as-is, and pre-quote any literal string values (e.g. {@code "' '"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.concat("firstName", "' '");
     * // Returns: "CONCAT(firstName, ' ')"
     *
     * String expr2 = Expression.concat("city", "', '");
     * // Returns: "CONCAT(city, ', ')"
     * }</pre>
     *
     * @param expr1 the first SQL expression (column reference or pre-quoted literal)
     * @param expr2 the second SQL expression (column reference or pre-quoted literal)
     * @return a CONCAT function string of the form {@code CONCAT(str1, str2)}
     */
    public static String concat(final String expr1, final String expr2) {
        return function(CONCAT, expr1, expr2);
    }

    /**
     * Creates a REPLACE function expression.
     * REPLACE substitutes occurrences of a substring within a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.replace("email", "'@'", "'_at_'");
     * // Returns: "REPLACE(email, '@', '_at_')"
     * 
     * String expr2 = Expression.replace("phone", "'-'", "''");
     * // Returns: "REPLACE(phone, '-', '')"
     * }</pre>
     *
     * @param expr the SQL expression to search in
     * @param oldString the string to search for
     * @param replacement the replacement string
     * @return a REPLACE function string
     */
    public static String replace(final String expr, final String oldString, final String replacement) {
        return function(REPLACE, expr, oldString, replacement);
    }

    /**
     * Creates a LENGTH function expression.
     * LENGTH returns the number of characters in a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.length("name");           // Returns: "LENGTH(name)"
     * String expr2 = Expression.length("description");   // Returns: "LENGTH(description)"
     * }</pre>
     *
     * @param expr the SQL expression whose length is returned
     * @return a LENGTH function string
     */
    public static String length(final String expr) {
        return function(LENGTH, expr);
    }

    /**
     * Creates a SUBSTR function expression starting from a position.
     * SUBSTR extracts a substring starting at the specified position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.substr("phone", 1);   // Returns: "SUBSTR(phone, 1)"
     * String expr2 = Expression.substr("code", 3);   // Returns: "SUBSTR(code, 3)"
     * }</pre>
     *
     * @param expr the SQL expression to extract from
     * @param fromIndex the starting position (1-based)
     * @return a SUBSTR function string
     */
    public static String substr(final String expr, final int fromIndex) {
        return function(SUBSTR, expr, fromIndex);
    }

    /**
     * Creates a SUBSTR function expression with start position and length.
     * SUBSTR extracts a substring of specified length starting at the given position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.substr("phone", 1, 3);   // Returns: "SUBSTR(phone, 1, 3)"
     * String expr2 = Expression.substr("zip", 1, 5);    // Returns: "SUBSTR(zip, 1, 5)"
     * }</pre>
     *
     * @param expr the SQL expression to extract from
     * @param fromIndex the starting position (1-based)
     * @param length the number of characters to extract
     * @return a SUBSTR function string
     */
    public static String substr(final String expr, final int fromIndex, final int length) {
        return function(SUBSTR, expr, fromIndex, length);
    }

    /**
     * Creates a TRIM function expression.
     * TRIM removes leading and trailing spaces from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.trim("input");        // Returns: "TRIM(input)"
     * String expr2 = Expression.trim("user_name");   // Returns: "TRIM(user_name)"
     * }</pre>
     *
     * @param expr the SQL expression to trim
     * @return a TRIM function string
     */
    public static String trim(final String expr) {
        return function(TRIM, expr);
    }

    /**
     * Creates an LTRIM (left trim) function expression.
     * LTRIM removes leading spaces from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ltrim("comment");    // Returns: "LTRIM(comment)"
     * String expr2 = Expression.ltrim("address");   // Returns: "LTRIM(address)"
     * }</pre>
     *
     * @param expr the SQL expression to left trim
     * @return an LTRIM function string
     */
    public static String ltrim(final String expr) {
        return function(LTRIM, expr);
    }

    /**
     * Creates an RTRIM (right trim) function expression.
     * RTRIM removes trailing spaces from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.rtrim("code");           // Returns: "RTRIM(code)"
     * String expr2 = Expression.rtrim("description");   // Returns: "RTRIM(description)"
     * }</pre>
     *
     * @param expr the SQL expression to right trim
     * @return an RTRIM function string
     */
    public static String rtrim(final String expr) {
        return function(RTRIM, expr);
    }

    /**
     * Creates an LPAD (left pad) function expression.
     * LPAD pads a string on the left to a specified length with a given string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lpad("id", 10, "'0'");     // Returns: "LPAD(id, 10, '0')"
     * String expr2 = Expression.lpad("code", 5, "' '");   // Returns: "LPAD(code, 5, ' ')"
     * }</pre>
     *
     * @param expr the SQL expression to pad
     * @param length the total length after padding
     * @param padExpr the SQL expression to pad with
     * @return an LPAD function string
     */
    public static String lpad(final String expr, final int length, final String padExpr) {
        return function(LPAD, expr, length, padExpr);
    }

    /**
     * Creates an RPAD (right pad) function expression.
     * RPAD pads a string on the right to a specified length with a given string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.rpad("name", 20, "' '");    // Returns: "RPAD(name, 20, ' ')"
     * String expr2 = Expression.rpad("code", 10, "'X'");   // Returns: "RPAD(code, 10, 'X')"
     * }</pre>
     *
     * @param expr the SQL expression to pad
     * @param length the total length after padding
     * @param padExpr the SQL expression to pad with
     * @return an RPAD function string
     */
    public static String rpad(final String expr, final int length, final String padExpr) {
        return function(RPAD, expr, length, padExpr);
    }

    /**
     * Creates a LOWER function expression.
     * LOWER converts all characters in a string to lowercase.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lower("email");      // Returns: "LOWER(email)"
     * String expr2 = Expression.lower("COUNTRY");   // Returns: "LOWER(COUNTRY)"
     * }</pre>
     *
     * @param expr the SQL expression to convert to lowercase
     * @return a LOWER function string
     */
    public static String lower(final String expr) {
        return function(LOWER, expr);
    }

    /**
     * Creates an UPPER function expression.
     * UPPER converts all characters in a string to uppercase.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.upper("name");            // Returns: "UPPER(name)"
     * String expr2 = Expression.upper("country_code");   // Returns: "UPPER(country_code)"
     * }</pre>
     *
     * @param expr the SQL expression to convert to uppercase
     * @return an UPPER function string
     */
    public static String upper(final String expr) {
        return function(UPPER, expr);
    }

    /**
     * Returns an empty list as expressions have no parameters.
     * Expressions are literal SQL strings and don't have bindable parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Object> params = Expression.of("price * quantity").parameters();   // returns []
     * boolean empty = params.isEmpty();                                                   // returns true
     * Expression.of("id = 5").parameters();                                            // returns [] (value is part of the literal, not a parameter)
     * }</pre>
     *
     * @return an empty immutable list
     */
    @Override
    public ImmutableList<Object> parameters() {
        return ImmutableList.empty();
    }

    /**
     * Renders a SQL function call of the form {@code "FUNC(arg1, arg2, ...)"}.
     * Each argument is converted via {@link N#stringOf(Object)} and emitted verbatim
     * (i.e. without quoting or escaping). String arguments that represent literal
     * values must therefore be pre-quoted by the caller (e.g. {@code "'foo'"}).
     *
     * @param functionName the function name, emitted verbatim as supplied
     * @param args the function arguments; emitted verbatim, comma-separated
     * @return the rendered function call string
     */
    private static String function(final String functionName, final Object... args) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(functionName);

            sb.append(SK._PARENTHESIS_L);

            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(N.stringOf(args[i]));
            }

            sb.append(SK._PARENTHESIS_R);

            return sb.toString();

        } finally {
            Objectory.recycle(sb);
        }
    }

    private static void registerSqlKeyword(final String keyword) {
        if (Strings.isNotEmpty(keyword)) {
            // Register the keyword as-is and its upper-case (canonical) form only. The lower-case form is
            // intentionally NOT registered: SQL keywords are conventionally upper-case, whereas a lower-case
            // token is treated as an identifier and converted by the naming policy. Registering lower-case
            // forms would wrongly suppress conversion of legitimate columns named like keywords
            // (e.g. "order", "count", "min", "rownum").
            SQL_KEY_WORDS.add(keyword);
            SQL_KEY_WORDS.add(keyword.toUpperCase(Locale.ROOT));
        }
    }

    private static boolean isSqlKeyword(final String word) {
        return SQL_KEY_WORDS.contains(word);
    }

    /**
     * Returns the string form of this expression, with the naming policy applied to any
     * identifiers (column or property names) that can be detected within the literal.
     * Function names, quoted strings, and numeric literals are left unchanged. Recognized SQL
     * keyword tokens are also left unchanged when written in their canonical upper-case form
     * (for example {@code CURRENT_DATE}); a lower-case token is treated as an identifier and
     * converted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression.of("firstName").toString(NamingPolicy.SNAKE_CASE);          // returns "first_name"
     * Expression.of("firstName = 'John'").toString(NamingPolicy.SNAKE_CASE); // returns "first_name = 'John'" (identifier converted, quoted literal kept)
     * Expression.of("price-tax").toString(NamingPolicy.CAMEL_CASE);          // returns "price-tax" (SQL subtraction preserved; each operand converted independently)
     * Expression.of("firstName").toString(NamingPolicy.NO_CHANGE);           // returns "firstName"
     * Expression.of("firstName").toString(null);                             // returns "firstName" (null defaults to NO_CHANGE)
     * Expression.of("").toString(NamingPolicy.NO_CHANGE);                    // returns "" (empty literal)
     * // an uninitialized instance (null literal, only possible via deserialization) returns "null"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to detected identifiers;
     *                     if {@code null}, {@link NamingPolicy#NO_CHANGE} is used
     * @return the expression string with identifiers converted according to the naming policy
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;

        if (literal == null) {
            return NULL_STRING;
        } else if (literal.isEmpty()) {
            return Strings.EMPTY;
        }

        if (literal.length() < 16 && literal.indexOf('-') < 0 && QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher(literal).matches()) {
            // Mirror the parse path below: only tokens starting with an ASCII letter are naming-policy
            // converted; a digit-leading token (e.g. "2faCode") passes through unchanged.
            // Hyphen-containing literals (e.g. "price-tax", SQL subtraction) are excluded even though the
            // simple-column pattern accepts '-': CAMEL_CASE/SNAKE_CASE conversion would swallow the '-',
            // so they take the parser path below, which converts each operand independently.
            if (!Strings.isAsciiAlpha(literal.charAt(0)) || isSqlKeyword(literal)) {
                return literal;
            }

            return effectiveNamingPolicy.convert(literal);
        }

        List<String> words = cachedParsedLiteral;

        if (words == null) {
            words = SqlParser.parse(literal);
            cachedParsedLiteral = words;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            String word = null;
            for (int i = 0, len = words.size(); i < len; i++) {
                word = words.get(i);

                if (word.isEmpty() || !Strings.isAsciiAlpha(word.charAt(0)) || SqlParser.isFunctionName(words, i) || isSqlKeyword(word)) {
                    sb.append(word);
                } else {
                    sb.append(effectiveNamingPolicy.convert(word));
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Expression("price * quantity").hashCode();                            // returns "price * quantity".hashCode()
     * Expression.of("a + b").hashCode() == Expression.of("a + b").hashCode();   // true (same literal)
     * }</pre>
     *
     * @return the hash code of the literal
     */
    @Override
    public int hashCode() {
        return (literal == null) ? 0 : literal.hashCode();
    }

    /**
     * Checks if this expression equals another object.
     * Two expressions are equal if they are both {@code Expression} instances with the same literal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Expression("a + b").equals(new Expression("a + b"));   // returns true (same literal)
     * Expression.of("a + b").equals(Expression.of("a + b"));     // returns true (cached, same instance)
     * new Expression("a + b").equals(new Expression("a - b"));   // returns false (different literal)
     * new Expression("a + b").equals("a + b");                   // returns false (not an Expression)
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return N.equals(literal, ((Expression) obj).literal);
    }
}
