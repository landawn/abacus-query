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

import static com.landawn.abacus.query.SK.ABS;
import static com.landawn.abacus.query.SK.ACOS;
import static com.landawn.abacus.query.SK.AMPERSAND;
import static com.landawn.abacus.query.SK.ASIN;
import static com.landawn.abacus.query.SK.ASTERISK;
import static com.landawn.abacus.query.SK.ATAN;
import static com.landawn.abacus.query.SK.AVG;
import static com.landawn.abacus.query.SK.CEIL;
import static com.landawn.abacus.query.SK.CIRCUMFLEX;
import static com.landawn.abacus.query.SK.COMMA_SPACE;
import static com.landawn.abacus.query.SK.CONCAT;
import static com.landawn.abacus.query.SK.COS;
import static com.landawn.abacus.query.SK.COUNT;
import static com.landawn.abacus.query.SK.EXP;
import static com.landawn.abacus.query.SK.FLOOR;
import static com.landawn.abacus.query.SK.LENGTH;
import static com.landawn.abacus.query.SK.LN;
import static com.landawn.abacus.query.SK.LOG;
import static com.landawn.abacus.query.SK.LOWER;
import static com.landawn.abacus.query.SK.LPAD;
import static com.landawn.abacus.query.SK.LTRIM;
import static com.landawn.abacus.query.SK.MAX;
import static com.landawn.abacus.query.SK.MIN;
import static com.landawn.abacus.query.SK.MINUS;
import static com.landawn.abacus.query.SK.MOD;
import static com.landawn.abacus.query.SK.PERCENT;
import static com.landawn.abacus.query.SK.PLUS;
import static com.landawn.abacus.query.SK.POWER;
import static com.landawn.abacus.query.SK.REPLACE;
import static com.landawn.abacus.query.SK.RPAD;
import static com.landawn.abacus.query.SK.RTRIM;
import static com.landawn.abacus.query.SK.SIGN;
import static com.landawn.abacus.query.SK.SIN;
import static com.landawn.abacus.query.SK.SLASH;
import static com.landawn.abacus.query.SK.SPACE;
import static com.landawn.abacus.query.SK.SQRT;
import static com.landawn.abacus.query.SK.SUBSTR;
import static com.landawn.abacus.query.SK.SUM;
import static com.landawn.abacus.query.SK.TAN;
import static com.landawn.abacus.query.SK.TRIM;
import static com.landawn.abacus.query.SK.UPPER;
import static com.landawn.abacus.query.SK.VERTICALBAR;
import static com.landawn.abacus.query.SK._QUOTATION_S;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.SK;
import com.landawn.abacus.query.SQLParser;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
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
 * <p>The class provides numerous static helper methods for creating common SQL expressions
 * and functions, including arithmetic operations, string functions, mathematical functions,
 * and comparison operations. These methods help build type-safe SQL expressions.</p>
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
 * String avgPrice = Expression.average("price");
 * 
 * // Complex expressions
 * String complex = Expression.plus("base_price", "tax", "shipping");
 * }</pre>
 *
 * @see AbstractCondition
 * @see Filters#expr(String)
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

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Expression instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Expression() {
        literal = null;
    }

    /**
     * Constructs a new Expression with the specified SQL literal.
     * The literal can contain any valid SQL expression, including functions, operators,
     * column references, and complex expressions.
     *
     * <p>Note: For frequently used expressions, consider using {@link #of(String)} instead,
     * which provides caching for better performance and memory efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr1 = new Expression("CURRENT_TIMESTAMP");
     * Expression expr2 = new Expression("price * quantity");
     * Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END");
     * Expression expr4 = new Expression("COALESCE(middle_name, '')");
     * }</pre>
     *
     * @param literal the SQL expression as a string. Can contain any valid SQL.
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
     * share the same instance, improving memory efficiency and performance.
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
     * @param literal the SQL expression string
     * @return a cached or new Expression instance
     */
    public static Expression of(final String literal) {
        return cachedExpression.computeIfAbsent(literal, Expression::new);
    }

    /**
     * Creates an equality expression between a literal and a value.
     * This is useful for building dynamic SQL conditions programmatically.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.equal("age", 25);
     * // Returns: "age = 25"
     * 
     * String expr2 = Expression.equal("status", "active");
     * // Returns: "status = 'active'"
     * }</pre>
     *
     * @param literal the left-hand side of the equality
     * @param value the right-hand side value
     * @return a string representation of the equality expression
     */
    public static String equal(final String literal, final Object value) { //NOSONAR
        return link(Operator.EQUAL, literal, value);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.notEqual("status", "INACTIVE");
     * // Returns: "status != 'INACTIVE'"
     * 
     * String expr2 = Expression.notEqual("count", 0);
     * // Returns: "count != 0"
     * }</pre>
     *
     * @param literal the left-hand side of the inequality
     * @param value the right-hand side value
     * @return a string representation of the not-equal expression
     */
    public static String notEqual(final String literal, final Object value) {
        return link(Operator.NOT_EQUAL, literal, value);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.greaterThan("salary", 50000);
     * // Returns: "salary > 50000"
     * 
     * String expr2 = Expression.greaterThan("created_date", "2024-01-01");
     * // Returns: "created_date > '2024-01-01'"
     * }</pre>
     *
     * @param literal the left-hand side of the comparison
     * @param value the right-hand side value
     * @return a string representation of the greater-than expression
     */
    public static String greaterThan(final String literal, final Object value) {
        return link(Operator.GREATER_THAN, literal, value);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.greaterEqual("score", 60);
     * // Returns: "score >= 60"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.ge("quantity", 1);
     * // Returns: "quantity >= 1"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lessThan("price", 100);
     * // Returns: "price < 100"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lt("stock", 10);
     * // Returns: "stock < 10"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lessEqual("discount", 50);
     * // Returns: "discount <= 50"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.le("temperature", 32);
     * // Returns: "temperature <= 32"
     * }</pre>
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
     * The BETWEEN operator is inclusive on both ends.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.between("age", 18, 65);
     * // Returns: "age BETWEEN (18, 65)"
     * 
     * String expr2 = Expression.between("price", 10.0, 50.0);
     * // Returns: "price BETWEEN (10.0, 50.0)"
     * }</pre>
     *
     * @param literal the literal to test
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a string representation of the BETWEEN expression
     */
    public static String between(final String literal, final Object min, final Object max) {
        return link(Operator.BETWEEN, literal, min, max);
    }

    /**
     * Creates a BETWEEN expression for a literal with min and max values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.bt("score", 60, 100);
     * // Returns: "score BETWEEN (60, 100)"
     * }</pre>
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
     * @param literal the literal to match
     * @param value the pattern to match against (can include % and _ wildcards)
     * @return a string representation of the LIKE expression
     */
    public static String like(final String literal, final String value) {
        return link(Operator.LIKE, literal, value);
    }

    /**
     * Creates an IS NULL expression for the specified literal.
     *
     * @param literal the literal to check for null
     * @return a string representation of the IS NULL expression
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
     * @param literals the literals to combine with AND
     * @return a string representation of the AND expression
     */
    public static String and(final String... literals) {
        return link2(Operator.AND, literals);
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
     * @param literals the literals to combine with OR
     * @return a string representation of the OR expression
     */
    public static String or(final String... literals) {
        return link2(Operator.OR, literals);
    }

    /**
     * Creates an addition expression for the given objects.
     * Concatenates all values with the + operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.plus("price", "tax", "shipping");
     * // Returns: "price + tax + shipping"
     * 
     * String expr2 = Expression.plus("base_salary", 5000, "bonus");
     * // Returns: "base_salary + 5000 + bonus"
     * }</pre>
     *
     * @param objects the values to add
     * @return a string representation of the addition expression
     */
    public static String plus(final Object... objects) {
        return link(PLUS, objects);
    }

    /**
     * Creates a subtraction expression for the given objects.
     * Subtracts each subsequent value from the first.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.minus("total", "discount", "tax_credit");
     * // Returns: "total - discount - tax_credit"
     * 
     * String expr2 = Expression.minus("price", 10);
     * // Returns: "price - 10"
     * }</pre>
     *
     * @param objects the values to subtract
     * @return a string representation of the subtraction expression
     */
    public static String minus(final Object... objects) {
        return link(MINUS, objects);
    }

    /**
     * Creates a multiplication expression for the given objects.
     * Multiplies all values together.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.multi("price", "quantity", "tax_rate");
     * // Returns: "price * quantity * tax_rate"
     * 
     * String expr2 = Expression.multi("hours", 60);
     * // Returns: "hours * 60"
     * }</pre>
     *
     * @param objects the values to multiply
     * @return a string representation of the multiplication expression
     */
    public static String multi(final Object... objects) {
        return link(ASTERISK, objects);
    }

    /**
     * Creates a division expression for the given objects.
     * Divides the first value by each subsequent value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.division("total", "count");
     * // Returns: "total / count"
     * 
     * String expr2 = Expression.division("distance", "time", 60);
     * // Returns: "distance / time / 60"
     * }</pre>
     *
     * @param objects the values to divide
     * @return a string representation of the division expression
     */
    public static String division(final Object... objects) {
        return link(SLASH, objects);
    }

    /**
     * Creates a modulus expression for the given objects.
     * Returns the remainder of division operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.modulus("value", 10);
     * // Returns: "value % 10"
     * 
     * String expr2 = Expression.modulus("id", "batch_size");
     * // Returns: "id % batch_size"
     * }</pre>
     *
     * @param objects the values for modulus operation
     * @return a string representation of the modulus expression
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
     * String expr = Expression.lShift("flags", 2);
     * // Returns: "flags << 2"
     * }</pre>
     *
     * @param objects the values for left shift operation
     * @return a string representation of the left shift expression
     */
    public static String lShift(final Object... objects) {
        return link(LEFT_SHIFT, objects);
    }

    /**
     * Creates a right shift expression for the given objects.
     * Shifts bits to the right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.rShift("value", 4);
     * // Returns: "value >> 4"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.bitwiseAnd("permissions", "mask");
     * // Returns: "permissions & mask"
     * 
     * String expr2 = Expression.bitwiseAnd("flags", 0xFF);
     * // Returns: "flags & 255"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.bitwiseOr("flags1", "flags2");
     * // Returns: "flags1 | flags2"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.bitwiseXOr("value1", "value2");
     * // Returns: "value1 ^ value2"
     * }</pre>
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
            sb.append(SK._SPACE);
            sb.append(operator.getName());
            sb.append(SK._SPACE);
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
            sb.append(SK._SPACE);
            sb.append(operator.getName());
            sb.append(SK._SPACE);
            sb.append(SK._PARENTHESES_L);
            sb.append(formalize(min));
            sb.append(SK.COMMA_SPACE);
            sb.append(formalize(max));
            sb.append(SK._PARENTHESES_R);

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
            sb.append(SK._SPACE);
            sb.append(operator.getName());
            sb.append(SK._SPACE);
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
                    sb.append(SK._SPACE);
                    sb.append(operator.getName());
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
     * Links multiple objects with the specified symbol.
     *
     * @param linkedSymbol the symbol to use for linking
     * @param objects the objects to link
     * @return a string representation of the linked expression
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

                sb.append(formalize(objects[i]));
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
     *   <li>Strings are quoted with single quotes and special characters are escaped</li>
     *   <li>null values become the string "null"</li>
     *   <li>Numbers and booleans are converted to their string representation</li>
     *   <li>Expression objects return their literal SQL text</li>
     *   <li>Other objects are converted to strings, quoted, and escaped</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression.formalize("text");      // Returns: "'text'"
     * Expression.formalize("O'Brien");   // Returns: "'O\'Brien'" (escaped quote)
     * Expression.formalize(123);         // Returns: "123"
     * Expression.formalize(45.67);       // Returns: "45.67"
     * Expression.formalize(null);        // Returns: "null"
     * Expression.formalize(true);        // Returns: "true"
     * Expression.formalize(false);       // Returns: "false"
     * Expression expr = new Expression("COUNT(*)");
     * Expression.formalize(expr);   // Returns: "COUNT(*)" (the expression's literal)
     * }</pre>
     *
     * @param value the value to formalize
     * @return the SQL representation of the value
     */
    public static String formalize(final Object value) {
        if (value == null) {
            return NULL_STRING;
        }

        if (value instanceof String) {
            return (_QUOTATION_S + Strings.quoteEscaped((String) value) + _QUOTATION_S);
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Expression) {
            return ((Expression) value).getLiteral();
        } else {
            return (_QUOTATION_S + Strings.quoteEscaped(N.stringOf(value)) + _QUOTATION_S);
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
     * @param expression the expression to count
     * @return a COUNT function string
     */
    public static String count(final String expression) {
        return function(COUNT, expression);
    }

    /**
     * Creates an AVERAGE function expression.
     * AVG returns the average value of a numeric column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.average("salary");   // Returns: "AVG(salary)"
     * String expr2 = Expression.average("age");     // Returns: "AVG(age)"
     * }</pre>
     *
     * @param expression the expression to average
     * @return an AVG function string
     */
    public static String average(final String expression) {
        return function(AVG, expression);
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
     * @param expression the expression to sum
     * @return a SUM function string
     */
    public static String sum(final String expression) {
        return function(SUM, expression);
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
     * @param expression the expression to find minimum
     * @return a MIN function string
     */
    public static String min(final String expression) {
        return function(MIN, expression);
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
     * @param expression the expression to find maximum
     * @return a MAX function string
     */
    public static String max(final String expression) {
        return function(MAX, expression);
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
     * @param expression the expression to get absolute value of
     * @return an ABS function string
     */
    public static String abs(final String expression) {
        return function(ABS, expression);
    }

    /**
     * Creates an ACOS (arc cosine) function expression.
     * ACOS returns the arc cosine of a number in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.acos("0.5");             // Returns: "ACOS(0.5)"
     * String expr2 = Expression.acos("cosine_value");   // Returns: "ACOS(cosine_value)"
     * }</pre>
     *
     * @param expression the expression to calculate arc cosine of
     * @return an ACOS function string
     */
    public static String acos(final String expression) {
        return function(ACOS, expression);
    }

    /**
     * Creates an ASIN (arc sine) function expression.
     * ASIN returns the arc sine of a number in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.asin("0.5");           // Returns: "ASIN(0.5)"
     * String expr2 = Expression.asin("sine_value");   // Returns: "ASIN(sine_value)"
     * }</pre>
     *
     * @param expression the expression to calculate arc sine of
     * @return an ASIN function string
     */
    public static String asin(final String expression) {
        return function(ASIN, expression);
    }

    /**
     * Creates an ATAN (arc tangent) function expression.
     * ATAN returns the arc tangent of a number in radians.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.atan("1");                // Returns: "ATAN(1)"
     * String expr2 = Expression.atan("tangent_value");   // Returns: "ATAN(tangent_value)"
     * }</pre>
     *
     * @param expression the expression to calculate arc tangent of
     * @return an ATAN function string
     */
    public static String atan(final String expression) {
        return function(ATAN, expression);
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
     * @param expression the expression to round up
     * @return a CEIL function string
     */
    public static String ceil(final String expression) {
        return function(CEIL, expression);
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
     * @param expression the expression to calculate cosine of
     * @return a COS function string
     */
    public static String cos(final String expression) {
        return function(COS, expression);
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
     * @param expression the expression to calculate exponential of
     * @return an EXP function string
     */
    public static String exp(final String expression) {
        return function(EXP, expression);
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
     * @param expression the expression to round down
     * @return a FLOOR function string
     */
    public static String floor(final String expression) {
        return function(FLOOR, expression);
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
     * @param expression the expression to calculate natural logarithm of
     * @return an LN function string
     */
    public static String ln(final String expression) {
        return function(LN, expression);
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
     * @param expression the expression to get sign of
     * @return a SIGN function string
     */
    public static String sign(final String expression) {
        return function(SIGN, expression);
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
     * @param expression the expression to calculate sine of
     * @return a SIN function string
     */
    public static String sin(final String expression) {
        return function(SIN, expression);
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
     * @param expression the expression to calculate square root of
     * @return a SQRT function string
     */
    public static String sqrt(final String expression) {
        return function(SQRT, expression);
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
     * @param expression the expression to calculate tangent of
     * @return a TAN function string
     */
    public static String tan(final String expression) {
        return function(TAN, expression);
    }

    /**
     * Creates a CONCAT function expression to concatenate two strings.
     * CONCAT joins two or more strings into one.
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
     * @param str1 the first string
     * @param str2 the second string
     * @return a CONCAT function string
     */
    public static String concat(final String str1, final String str2) {
        return function(CONCAT, str1, str2);
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
     * @param str the string to search in
     * @param oldString the string to search for
     * @param replacement the replacement string
     * @return a REPLACE function string
     */
    public static String replace(final String str, final String oldString, final String replacement) {
        return function(REPLACE, str, oldString, replacement);
    }

    /**
     * Creates a LENGTH function expression.
     * LENGTH returns the number of characters in a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.stringLength("name");           // Returns: "LENGTH(name)"
     * String expr2 = Expression.stringLength("description");   // Returns: "LENGTH(description)"
     * }</pre>
     *
     * @param str the string to get length of
     * @return a LENGTH function string
     */
    public static String stringLength(final String str) {
        return function(LENGTH, str);
    }

    /**
     * Creates a SUBSTR function expression starting from a position.
     * SUBSTR extracts a substring starting at the specified position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.subString("phone", 1);   // Returns: "SUBSTR(phone, 1)"
     * String expr2 = Expression.subString("code", 3);   // Returns: "SUBSTR(code, 3)"
     * }</pre>
     *
     * @param str the string to extract from
     * @param fromIndex the starting position (1-based)
     * @return a SUBSTR function string
     */
    public static String subString(final String str, final int fromIndex) {
        return function(SUBSTR, str, fromIndex);
    }

    /**
     * Creates a SUBSTR function expression with start position and length.
     * SUBSTR extracts a substring of specified length starting at the given position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.subString("phone", 1, 3);   // Returns: "SUBSTR(phone, 1, 3)"
     * String expr2 = Expression.subString("zip", 1, 5);    // Returns: "SUBSTR(zip, 1, 5)"
     * }</pre>
     *
     * @param str the string to extract from
     * @param fromIndex the starting position (1-based)
     * @param length the number of characters to extract
     * @return a SUBSTR function string
     */
    public static String subString(final String str, final int fromIndex, final int length) {
        return function(SUBSTR, str, fromIndex, length);
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
     * @param str the string to trim
     * @return a TRIM function string
     */
    public static String trim(final String str) {
        return function(TRIM, str);
    }

    /**
     * Creates an LTRIM (left trim) function expression.
     * LTRIM removes leading spaces from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lTrim("comment");    // Returns: "LTRIM(comment)"
     * String expr2 = Expression.lTrim("address");   // Returns: "LTRIM(address)"
     * }</pre>
     *
     * @param str the string to left trim
     * @return an LTRIM function string
     */
    public static String lTrim(final String str) {
        return function(LTRIM, str);
    }

    /**
     * Creates an RTRIM (right trim) function expression.
     * RTRIM removes trailing spaces from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.rTrim("code");           // Returns: "RTRIM(code)"
     * String expr2 = Expression.rTrim("description");   // Returns: "RTRIM(description)"
     * }</pre>
     *
     * @param str the string to right trim
     * @return an RTRIM function string
     */
    public static String rTrim(final String str) {
        return function(RTRIM, str);
    }

    /**
     * Creates an LPAD (left pad) function expression.
     * LPAD pads a string on the left to a specified length with a given string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.lPad("id", 10, "'0'");     // Returns: "LPAD(id, 10, '0')"
     * String expr2 = Expression.lPad("code", 5, "' '");   // Returns: "LPAD(code, 5, ' ')"
     * }</pre>
     *
     * @param str the string to pad
     * @param length the total length after padding
     * @param padStr the string to pad with
     * @return an LPAD function string
     */
    public static String lPad(final String str, final int length, final String padStr) {
        return function(LPAD, str, length, padStr);
    }

    /**
     * Creates an RPAD (right pad) function expression.
     * RPAD pads a string on the right to a specified length with a given string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String expr = Expression.rPad("name", 20, "' '");    // Returns: "RPAD(name, 20, ' ')"
     * String expr2 = Expression.rPad("code", 10, "'X'");   // Returns: "RPAD(code, 10, 'X')"
     * }</pre>
     *
     * @param str the string to pad
     * @param length the total length after padding
     * @param padStr the string to pad with
     * @return an RPAD function string
     */
    public static String rPad(final String str, final int length, final String padStr) {
        return function(RPAD, str, length, padStr);
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
     * @param str the string to convert to lowercase
     * @return a LOWER function string
     */
    public static String lower(final String str) {
        return function(LOWER, str);
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
     * @param str the string to convert to uppercase
     * @return an UPPER function string
     */
    public static String upper(final String str) {
        return function(UPPER, str);
    }

    /**
     * Returns an empty list as expressions have no parameters.
     * Expressions are literal SQL strings and don't have bindable parameters.
     *
     * @return an empty list
     */
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * No-op method as Expression has no parameters to clear.
     *
     * <p>Expressions are literal SQL strings and don't have bindable parameters,
     * so this method does nothing. It exists to satisfy the interface contract.</p>
     *
     */
    @Override
    public void clearParameters() {
        // No parameters to clear for Expression
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

            sb.append(SK._PARENTHESES_L);

            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(N.stringOf(args[i]));
            }

            sb.append(SK._PARENTHESES_R);

            return sb.toString();

        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Returns the literal string of this expression.
     * The naming policy may be applied to property names within the expression
     * if they can be identified as simple column names.
     *
     * @param namingPolicy the naming policy to apply
     * @return the literal string of this expression with applied naming policy
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (literal == null) {
            return NULL_STRING;
        } else if (literal.isEmpty()) {
            return Strings.EMPTY;
        }

        if (literal.length() < 16 && QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher(literal).find()) {
            return namingPolicy.convert(literal);
        }

        final List<String> words = SQLParser.parse(literal);
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            String word = null;
            for (int i = 0, len = words.size(); i < len; i++) {
                word = words.get(i);

                if (word.isEmpty() || !Strings.isAsciiAlpha(word.charAt(0)) || SQLParser.isFunctionName(words, len, i)) {
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
     * @return {@code true} if the objects are equal
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression.Expr expr = new Expression.Expr("price * quantity");
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Expression.Expr expr = new Expression.Expr("price * quantity");
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
