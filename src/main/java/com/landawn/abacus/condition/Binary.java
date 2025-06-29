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

import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Abstract base class for binary conditions that compare a property with a value.
 * Binary conditions represent operations with two operands: a property name and a value.
 * 
 * <p>Common subclasses include:</p>
 * <ul>
 *   <li>{@link Equal} - property = value</li>
 *   <li>{@link NotEqual} - property != value</li>
 *   <li>{@link GreaterThan} - property > value</li>
 *   <li>{@link GreaterEqual} - property >= value</li>
 *   <li>{@link LessThan} - property < value</li>
 *   <li>{@link LessEqual} - property <= value</li>
 *   <li>{@link Like} - property LIKE value</li>
 * </ul>
 * 
 * <p>The value can be a literal value or another Condition (for subqueries).</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Simple binary conditions
 * Binary eq = new Equal("name", "John");
 * Binary gt = new GreaterThan("age", 18);
 * 
 * // Binary condition with subquery
 * Binary in = new In("department_id", subQuery);
 * }</pre>
 * 
 * @see AbstractCondition
 * @see Condition
 */
public class Binary extends AbstractCondition {

    // For Kryo
    final String propName;

    private Object propValue;

    // For Kryo
    Binary() {
        propName = null;
    }

    /**
     * Creates a new Binary condition.
     * 
     * @param propName the property name to compare
     * @param operator the comparison operator
     * @param propValue the value to compare against (can be a literal or Condition)
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary condition = new Binary("price", Operator.GREATER_THAN, 100.0);
     * }</pre>
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.propValue = propValue;
    }

    /**
     * Gets the property name being compared.
     * 
     * @return the property name
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary condition = new Equal("userName", "John");
     * String prop = condition.getPropName(); // Returns "userName"
     * }</pre>
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the value being compared against.
     * The value can be a literal value or a Condition (for subqueries).
     * 
     * @param <T> the expected type of the value
     * @return the property value, cast to the requested type
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary condition = new Equal("age", 25);
     * Integer age = condition.getPropValue(); // Returns 25
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T getPropValue() {
        return (T) propValue;
    }

    /**
     * Sets the value being compared against.
     * 
     * @param propValue the new property value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setPropValue(final Object propValue) {
        this.propValue = propValue;
    }

    /**
     * Gets the parameters for this condition.
     * If the value is a Condition (subquery), returns its parameters.
     * Otherwise, returns a list containing the single value.
     * 
     * @return a list of parameter values
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary condition = new Equal("status", "active");
     * List<Object> params = condition.getParameters(); // Returns ["active"]
     * }</pre>
     */
    @Override
    public List<Object> getParameters() {
        if (propValue instanceof Condition) {
            return ((Condition) propValue).getParameters();
        } else {
            return N.asList(propValue);
        }
    }

    /**
     * Clears the parameters of this condition.
     * If the value is a Condition, clears its parameters.
     * Otherwise, sets the value to null.
     */
    @Override
    public void clearParameters() {
        if (propValue instanceof Condition) {
            ((Condition) propValue).clearParameters();
        } else {
            propValue = null;
        }
    }

    /**
     * Creates a deep copy of this Binary condition.
     * If the value is a Condition, it is also copied.
     * 
     * @param <T> the type of condition to return
     * @return a new Binary instance with copied values
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary original = new Equal("name", "John");
     * Binary copy = original.copy();
     * // copy is a new instance with the same values
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final Binary copy = super.copy();

        if (propValue instanceof Condition) {
            copy.propValue = ((Condition) propValue).copy();
        }

        return (T) copy;
    }

    /**
     * Returns a string representation of this Binary condition using the specified naming policy.
     * 
     * @param namingPolicy the naming policy to apply to the property name
     * @return a string representation of this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary condition = new Equal("userName", "John");
     * String str = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "user_name = 'John'"
     * }</pre>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD._SPACE + parameter2String(propValue, namingPolicy);
    }

    /**
     * Returns the hash code of this Binary condition.
     * The hash code is computed based on the property name, operator, and value.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        return (h * 31) + ((propValue == null) ? 0 : propValue.hashCode());
    }

    /**
     * Checks if this Binary condition is equal to another object.
     * Two Binary conditions are equal if they have the same property name, operator, and value.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Binary c1 = new Equal("name", "John");
     * Binary c2 = new Equal("name", "John");
     * boolean isEqual = c1.equals(c2); // Returns true
     * }</pre>
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