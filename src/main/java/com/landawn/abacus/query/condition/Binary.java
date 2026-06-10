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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 * @see ComposableCondition
 * @see Equal
 * @see GreaterThan
 * @see LessThan
 * @see Condition
 */
public class Binary extends ComposableCondition {

    // For Kryo
    final String propName;

    private Object propValue;

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
     * @param propName the property name to compare (must not be {@code null} or empty)
     * @param operator the comparison operator (must not be {@code null})
     * @param propValue the value to compare against; may be a literal value, {@code null}
     *                  (for equality operators, renders as {@code IS NULL} / {@code IS NOT NULL}),
     *                  or a {@link Condition} such as a {@link SubQuery}
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty, or if {@code propValue}
     *                                  is an empty {@link Collection} for an {@code IN}/{@code NOT_IN} operator
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");

        if (isCollectionOperator(operator) && propValue instanceof final Collection<?> values) {
            N.checkArgNotEmpty(values, "propValue");
        }

        this.propName = propName;
        this.propValue = propValue;
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
     * Gets the parameters for this condition.
     *
     * <ul>
     *   <li>If the value is {@code null} and the operator is {@code =}, {@code !=}, {@code <>}, {@code IS}, or
     *       {@code IS NOT}, an empty list is returned because the SQL is rendered as {@code IS NULL} /
     *       {@code IS NOT NULL} with no bind parameter.</li>
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
     * List<Object> p1 = eq.getParameters();   // [25]
     *
     * // Null value with = or != -> empty list (rendered as IS NULL / IS NOT NULL)
     * Binary nullEq = new Equal("name", (Object) null);
     * List<Object> p2 = nullEq.getParameters();   // [] (empty)
     *
     * // Subquery value -> the subquery's own parameters
     * SubQuery sub = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
     * Binary eqSub = new Equal("userId", sub);
     * List<Object> p3 = eqSub.getParameters();   // [true] (the subquery's params)
     * }</pre>
     *
     * @return an immutable list of parameter values; never {@code null}
     */
    @Override
    public ImmutableList<Object> getParameters() {
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
                    parameters.addAll(((Condition) value).getParameters());
                } else {
                    parameters.add(value);
                }
            }

            return ImmutableList.wrap(parameters);
        }

        if (propValue instanceof Condition) {
            return ((Condition) propValue).getParameters();
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
     * the output is {@code propertyName IS NOT NULL}.</p>
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
     * Two Binary conditions are equal if they have the same property name, operator, and value.
     * Comparison is based on these three fields only — the runtime class is not part of the
     * equality contract — so two instances of different concrete subclasses are considered equal
     * when their property name, operator, and value all match. In practice this rarely happens,
     * because different subclasses use different operators.
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

        if (obj instanceof final Binary other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(propValue, other.propValue);
        }

        return false;
    }
}
