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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

/**
 * Abstract base class for binary conditions that compare a property with a value.
 * Binary conditions represent operations with two operands: a property name and a value.
 *
 * <p>This class serves as the foundation for all comparison operations in queries,
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
     * 
     * // With a subquery as value
     * SubQuery subQuery = Filters.subQuery("SELECT MIN(price) FROM products");
     * Binary minPrice = new Binary("price", Operator.GREATER_THAN_OR_EQUAL, subQuery);
     * }</pre>
     * 
     * @param propName the property name to compare (must not be {@code null} or empty)
     * @param operator the comparison operator
     * @param propValue the value to compare against; may be a literal value, {@code null}
     *                  (for equality operators, renders as {@code IS NULL} / {@code IS NOT NULL}),
     *                  or a {@link Condition} such as a {@link SubQuery}
     * @throws IllegalArgumentException if {@code propName} is {@code null} or empty
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");

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
     * The value can be a literal value or a Condition (for subqueries).
     * The returned value can be safely cast to its expected type.
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
     *   <li>If the value is {@code null} and the operator is {@code =}, {@code !=}, {@code IS}, or {@code IS NOT},
     *       an empty list is returned because the SQL is rendered as {@code IS NULL} / {@code IS NOT NULL}
     *       with no bind parameter.</li>
     *   <li>If the value is a {@link Condition} (e.g., a subquery), the subquery's own parameters are returned.</li>
     *   <li>Otherwise, a single-element list containing the value is returned.</li>
     * </ul>
     *
     * @return an immutable list of parameter values; never {@code null}
     */
    @Override
    public ImmutableList<Object> getParameters() {
        if (propValue == null && (operator() == Operator.EQUAL || operator() == Operator.NOT_EQUAL || operator() == Operator.NOT_EQUAL_ANSI
                || operator() == Operator.IS || operator() == Operator.IS_NOT)) {
            return ImmutableList.empty();
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
     * @param namingPolicy the naming policy to apply to the property name;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a string representation of this condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        if (propValue == null) {
            if (operator() == Operator.EQUAL || operator() == Operator.IS) {
                return effectiveNamingPolicy.convert(propName) + SK._SPACE + SK.IS_NULL;
            } else if (operator() == Operator.NOT_EQUAL || operator() == Operator.NOT_EQUAL_ANSI || operator() == Operator.IS_NOT) {
                return effectiveNamingPolicy.convert(propName) + SK._SPACE + SK.IS_NOT_NULL;
            }
        }

        return effectiveNamingPolicy.convert(propName) + SK._SPACE + operator().toString() + SK._SPACE + formatParameter(propValue, effectiveNamingPolicy);
    }

    /**
     * Returns the hash code of this Binary condition.
     * The hash code is computed based on the property name, operator, and value.
     * 
     * @return hash code based on property name, operator, and value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((propValue == null) ? 0 : propValue.hashCode());
    }

    /**
     * Checks if this Binary condition is equal to another object.
     * Two Binary conditions are equal if they have the same property name, operator, and value.
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
