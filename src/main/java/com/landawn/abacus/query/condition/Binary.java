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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Base class for binary conditions that compare a property with a value.
 * Binary conditions represent operations with two operands: a property name and a value.
 *
 * <p>This class is concrete and can be instantiated directly, but it also serves as the
 * foundation for all comparison operations in queries,
 * providing common functionality for storing the property name, operator, and value.
 * The value can be a literal (String, Number, Date, etc.) or another Condition (for subqueries).</p>
 *
 * <p>Common subclasses include:</p>
 * <ul>
 *   <li>{@link Equal} - property = value</li>
 *   <li>{@link NotEqual} - property != or &lt;&gt; value</li>
 *   <li>{@link GreaterThan} - property &gt; value</li>
 *   <li>{@link GreaterThanOrEqual} - property &gt;= value</li>
 *   <li>{@link LessThan} - property &lt; value</li>
 *   <li>{@link LessThanOrEqual} - property &lt;= value</li>
 *   <li>{@link Like} - property LIKE value</li>
 *   <li>{@link NotLike} - property NOT LIKE value</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple binary conditions
 * Binary eq = new Equal("name", "John");
 * Binary gt = new GreaterThan("age", 18);
 * 
 * // Binary condition with subquery
 * SubQuery avgSalary = Filters.subQuery("SELECT AVG(salary) FROM employees");
 * Binary aboveAvg = new GreaterThan("salary", avgSalary);
 * }</pre>
 *
 * <p><b>Note on {@code IN}/{@code NOT IN}:</b> although {@code Binary} accepts the membership
 * operators, prefer the dedicated {@link In}/{@link NotIn} condition classes (or the
 * {@code Filters.in}/{@code Filters.notIn} factories) for IN conditions — they expose the values
 * through {@code getValues()}, support multi-column row-value form, and are what
 * {@code instanceof}-based consumers expect. A {@code Binary} with {@code Operator.IN} renders
 * the same SQL but is a different type with different accessors.</p>
 *
 * @see ComposableCondition
 * @see Equal
 * @see GreaterThan
 * @see LessThan
 * @see Condition
 */
public class Binary extends ComposableCondition {

    /**
     * The operators valid for a binary {@code propName OP value} condition. This covers the operators
     * used by the concrete {@link Binary} subclasses ({@link Equal}, {@link NotEqual}, {@link GreaterThan},
     * {@link GreaterThanOrEqual}, {@link LessThan}, {@link LessThanOrEqual}, {@link Like}, {@link NotLike},
     * {@link Is}, {@link IsNot}) plus the ANSI not-equal token ({@link Operator#NOT_EQUAL_ANSI}) and the
     * membership operators ({@link Operator#IN}, {@link Operator#NOT_IN}) that {@code Binary} renders
     * directly when constructed via {@link com.landawn.abacus.query.Filters#binary}. Structural operators
     * (clauses, junctions, joins, set operations, quantifiers, {@code BETWEEN}/{@code EXISTS}) are rejected.
     */
    private static final Set<Operator> COMPARISON_OPERATORS = EnumSet.of(Operator.EQUAL, Operator.NOT_EQUAL, Operator.NOT_EQUAL_ANSI, Operator.GREATER_THAN,
            Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL, Operator.LIKE, Operator.NOT_LIKE, Operator.IS, Operator.IS_NOT,
            Operator.IN, Operator.NOT_IN);

    // For Kryo
    final String propName;

    private final Object propValue;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Binary instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Binary() {
        propName = null;
        propValue = null;
    }

    /**
     * Creates a new Binary condition.
     * This constructor initializes a binary condition with a property name, operator, and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom binary condition
     * Binary condition = new Binary("price", Operator.GREATER_THAN, 100.0);
     * // SQL: price > 100.0
     *
     * // With a subquery as value
     * SubQuery subQuery = Filters.subQuery("SELECT MIN(price) FROM products");
     * Binary minPrice = new Binary("price", Operator.GREATER_THAN_OR_EQUAL, subQuery);
     * // SQL: price >= (SELECT MIN(price) FROM products)
     * }</pre>
     * 
     * @param propName the property name to compare (must not be {@code null}, empty, or blank)
     * @param operator the comparison operator (must not be {@code null}); must be an operator valid for a
     *                 binary {@code propName OP value} condition, i.e. one of {@link Operator#EQUAL},
     *                 {@link Operator#NOT_EQUAL}, {@link Operator#NOT_EQUAL_ANSI}, {@link Operator#GREATER_THAN},
     *                 {@link Operator#GREATER_THAN_OR_EQUAL}, {@link Operator#LESS_THAN},
     *                 {@link Operator#LESS_THAN_OR_EQUAL}, {@link Operator#LIKE}, {@link Operator#NOT_LIKE},
     *                 {@link Operator#IS}, {@link Operator#IS_NOT}, {@link Operator#IN}, or {@link Operator#NOT_IN}
     * @param propValue the value to compare against; may be a literal value, {@code null}
     *                  (for equality operators, renders as {@code IS NULL} / {@code IS NOT NULL}),
     *                  or a {@link Condition} such as a {@link SubQuery}. For an {@code IN}/{@code NOT_IN}
     *                  operator, a {@link Collection} or array value is copied defensively and must be non-empty.
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank; if {@code operator}
     *                                  is not one of the operators listed above; or if, for an {@code IN}/{@code NOT_IN}
     *                                  operator, {@code propValue} is not a non-empty {@link Collection}, a non-empty
     *                                  array, or a {@link Condition}
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        checkPropName(propName);

        if (!COMPARISON_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException(
                    "Binary condition operator must be a comparison operator (one of " + COMPARISON_OPERATORS + "), but was: " + operator);
        }

        this.propName = propName;
        this.propValue = normalizePropValue(operator, propValue);
    }

    /**
     * Gets the property name being compared.
     * This is the left-hand side of the binary operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary eq = new Equal("age", 25);
     * String name = eq.getPropName();   // "age"
     *
     * Binary like = new Like("email", "%@example.com");
     * String likeName = like.getPropName();   // "email"
     * }</pre>
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the value being compared against.
     * The value can be a literal (String, Number, Date, etc.) or a {@link Condition} (typically a
     * {@link SubQuery}). The return type is inferred from the call site via an unchecked cast,
     * so the caller is responsible for ensuring the requested type is compatible with the stored
     * value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary eq = new Equal("age", 25);
     * Integer value = eq.getPropValue();   // 25
     *
     * Binary like = new Like("name", "%John%");
     * String pattern = like.getPropValue();   // "%John%"
     *
     * // With a subquery as value
     * SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
     * Binary gt = new GreaterThan("salary", subQuery);
     * SubQuery sub = gt.getPropValue();   // the SubQuery instance
     * }</pre>
     *
     * @param <T> the expected type of the value
     * @return the property value, cast to the requested type
     */
    @SuppressWarnings("unchecked")
    public <T> T getPropValue() {
        return (T) propValue;
    }

    /**
     * Returns the property value without an unchecked generic cast.
     *
     * @return the property value, which may be {@code null}
     */
    public Object propValue() {
        return propValue;
    }

    /**
     * Returns the property value cast by the supplied runtime type.
     *
     * @param <T> the requested value type
     * @param valueType the requested value type; must not be {@code null}
     * @return the property value cast to {@code valueType}, or {@code null} when the stored value is {@code null}
     * @throws NullPointerException if {@code valueType} is {@code null}
     * @throws ClassCastException if the stored value is not assignable to {@code valueType}
     */
    public <T> T getPropValue(final Class<T> valueType) {
        return java.util.Objects.requireNonNull(valueType, "valueType").cast(propValue);
    }

    /**
     * Gets the parameters for this condition.
     *
     * <ul>
     *   <li>If the value is {@code null} and the operator is {@code =}, {@code !=}, {@code <>}, {@code IS}, or
     *       {@code IS NOT}, an empty list is returned because the SQL is rendered as {@code IS NULL} /
     *       {@code IS NOT NULL} with no bind parameter.</li>
     *   <li>If the value is {@code null} with any other operator (e.g. {@code LIKE}), a single-element list
     *       containing {@code null} is returned, mirroring the rendered {@code null} literal
     *       (see {@link #toString(NamingPolicy)}).</li>
     *   <li>If the operator is {@code null} (only possible for an uninitialized instance), an empty list
     *       is returned.</li>
     *   <li>If the operator is {@code IN} or {@code NOT IN} and the value is a {@link Collection}, each element is
     *       added as a parameter; any element that is itself a {@link Condition} has its own parameters spliced in.</li>
     *   <li>If the value is a {@link Condition} (e.g., a subquery), the subquery's own parameters are returned.</li>
     *   <li>Otherwise, a single-element list containing the value is returned.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Literal value -> single-element list
     * Binary eq = new Equal("age", 25);
     * List<Object> p1 = eq.parameters();   // [25]
     *
     * // Null value with = or != -> empty list (rendered as IS NULL / IS NOT NULL)
     * Binary nullEq = new Equal("name", (Object) null);
     * List<Object> p2 = nullEq.parameters();   // [] (empty)
     *
     * // Subquery value -> the subquery's own parameters
     * SubQuery sub = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
     * Binary eqSub = new Equal("userId", sub);
     * List<Object> p3 = eqSub.parameters();   // [true] (the subquery's params)
     * }</pre>
     *
     * @return an immutable list of parameter values; never {@code null}
     */
    @Override
    public ImmutableList<Object> parameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    private ImmutableList<Object> computeParameters() {
        final Operator op = operator();

        if (propValue == null
                && (op == Operator.EQUAL || op == Operator.NOT_EQUAL || op == Operator.NOT_EQUAL_ANSI || op == Operator.IS || op == Operator.IS_NOT)) {
            return ImmutableList.empty();
        }

        if (op == null) {
            return ImmutableList.empty();
        }

        if (isCollectionOperator(op) && propValue instanceof final Collection<?> values) {
            final List<Object> parameters = new ArrayList<>(values.size());

            for (final Object value : values) {
                if (value instanceof Condition) {
                    parameters.addAll(((Condition) value).parameters());
                } else {
                    parameters.add(value);
                }
            }

            return ImmutableList.wrap(parameters);
        }

        if (propValue instanceof Condition) {
            return ((Condition) propValue).parameters();
        } else {
            return ImmutableList.of(propValue);
        }
    }

    /**
     * Converts this Binary condition to its string representation using the specified naming policy.
     *
     * <p>Normally the format is: {@code propertyName OPERATOR value}.
     * When the value is {@code null} and the operator is {@code =} or {@code IS}, the output is
     * {@code propertyName IS NULL}; when the operator is {@code !=}, {@code <>}, or {@code IS NOT},
     * the output is {@code propertyName IS NOT NULL}. Only those operators collapse to
     * {@code IS [NOT] NULL}: a {@code null} value with any other operator renders as the bare
     * literal {@code null} (e.g. {@code new Like("name", null)} renders {@code "name LIKE null"}),
     * and {@link #parameters()} correspondingly returns a single-element list containing {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values are single-quoted; numbers are unquoted
     * Binary eq = new Equal("name", "John");
     * String s1 = eq.toString(NamingPolicy.NO_CHANGE);   // "name = 'John'"
     *
     * Binary gt = new GreaterThan("age", 18);
     * String s2 = gt.toString(NamingPolicy.NO_CHANGE);   // "age > 18"
     *
     * // Null value with = renders as IS NULL; with != renders as IS NOT NULL
     * Binary nullEq = new Equal("deletedAt", (Object) null);
     * String s3 = nullEq.toString(NamingPolicy.NO_CHANGE);   // "deletedAt IS NULL"
     *
     * Binary nullNe = new NotEqual("deletedAt", (Object) null);
     * String s4 = nullNe.toString(NamingPolicy.NO_CHANGE);   // "deletedAt IS NOT NULL"
     *
     * // Subquery values are parenthesized; a null naming policy uses NO_CHANGE
     * Binary sub = new Equal("userId", Filters.subQuery("SELECT id FROM users"));
     * String s5 = sub.toString(null);   // "userId = (SELECT id FROM users)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a string representation of this condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();

        if (propValue == null) {
            if (op == Operator.EQUAL || op == Operator.IS) {
                return effectiveNamingPolicy.convert(propName) + SK._SPACE + SK.IS_NULL;
            } else if (op == Operator.NOT_EQUAL || op == Operator.NOT_EQUAL_ANSI || op == Operator.IS_NOT) {
                return effectiveNamingPolicy.convert(propName) + SK._SPACE + SK.IS_NOT_NULL;
            }
        }

        final String opStr = op == null ? Strings.NULL : op.toString();

        if (isCollectionOperator(op) && propValue instanceof final Collection<?> values) {
            return effectiveNamingPolicy.convert(propName) + SK._SPACE + opStr + SK._SPACE + formatCollection(values, effectiveNamingPolicy);
        }

        return effectiveNamingPolicy.convert(propName) + SK._SPACE + opStr + SK._SPACE + formatParameter(propValue, effectiveNamingPolicy);
    }

    private static boolean isCollectionOperator(final Operator op) {
        return op == Operator.IN || op == Operator.NOT_IN;
    }

    private static Object normalizePropValue(final Operator op, final Object propValue) {
        if (!isCollectionOperator(op)) {
            return propValue;
        }

        if (propValue instanceof Condition) {
            return propValue;
        }

        // The copies are wrapped unmodifiable so getPropValue() cannot leak a mutable view of the
        // internal membership list (mutation would silently desync the memoized parameters/hashCode).
        if (propValue instanceof final Collection<?> values) {
            N.checkArgNotEmpty(values, "propValue");
            return Collections.unmodifiableList(new ArrayList<>(values));
        }

        if (propValue != null && propValue.getClass().isArray()) {
            final int len = Array.getLength(propValue);

            if (len == 0) {
                throw new IllegalArgumentException("propValue must not be empty");
            }

            final List<Object> values = new ArrayList<>(len);

            for (int i = 0; i < len; i++) {
                values.add(Array.get(propValue, i));
            }

            return Collections.unmodifiableList(values);
        }

        throw new IllegalArgumentException("IN/NOT IN operator requires a non-empty collection, array, or condition value");
    }

    private static String formatCollection(final Collection<?> values, final NamingPolicy namingPolicy) {
        final StringBuilder sb = new StringBuilder();
        sb.append(SK._PARENTHESIS_L);

        int i = 0;
        for (final Object value : values) {
            if (i++ > 0) {
                sb.append(SK.COMMA_SPACE);
            }

            sb.append(formatParameter(value, namingPolicy));
        }

        sb.append(SK._PARENTHESIS_R);
        return sb.toString();
    }

    /**
     * Returns the hash code of this Binary condition.
     * The hash code is computed based on the property name, operator, and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Equal property/operator/value -> equal hash codes
     * Binary a = new Equal("age", 25);
     * Binary b = new Equal("age", 25);
     * boolean same = a.hashCode() == b.hashCode();   // true
     *
     * // Different value -> different hash codes
     * Binary c = new Equal("age", 30);
     * boolean diff = a.hashCode() == c.hashCode();   // false
     * }</pre>
     *
     * @return hash code based on property name, operator, and value
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + N.deepHashCode(propValue);

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this Binary condition is equal to another object.
     * Two conditions are equal only if they are of the exact same runtime class and have the same
     * property name, operator, and value. The runtime class is part of the equality contract, so an
     * instance of one concrete subclass is never equal to an instance of a different subclass (or to
     * a raw {@code Binary}), even when their property name, operator, and value all match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary a = new Equal("age", 25);
     * Binary b = new Equal("age", 25);
     * boolean eq = a.equals(b);   // true (same prop, operator, value)
     *
     * // Different value or property -> not equal
     * boolean neValue = a.equals(new Equal("age", 30));   // false
     * boolean neProp = a.equals(new Equal("name", 25));   // false
     *
     * // Non-Binary object -> not equal
     * boolean neType = a.equals("age");   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Binary other = (Binary) obj;
        return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(propValue, other.propValue);
    }
}
