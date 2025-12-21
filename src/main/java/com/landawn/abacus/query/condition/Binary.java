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

import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

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
 *   <li>{@link GreaterEqual} - property &gt;= value</li>
 *   <li>{@link LessThan} - property &lt; value</li>
 *   <li>{@link LessEqual} - property &lt;= value</li>
 *   <li>{@link Like} - property LIKE value</li>
 *   <li>{@link In} - property IN (values)</li>
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
 * @see AbstractCondition
 * @see Equal
 * @see GreaterThan
 * @see LessThan
 * @see Condition
 */
public class Binary extends AbstractCondition {

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
     * Binary minPrice = new Binary("price", Operator.GREATER_EQUAL, subQuery);
     * }</pre>
     * 
     * @param propName the property name to compare (must not be null or empty)
     * @param operator the comparison operator
     * @param propValue the value to compare against (can be a literal or Condition)
     * @throws IllegalArgumentException if propName is null or empty
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
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
     * Binary condition = new Equal("userName", "John");
     * String prop = condition.getPropName();   // Returns "userName"
     * 
     * Binary ageCheck = new GreaterThan("age", 18);
     * String ageProp = ageCheck.getPropName();   // Returns "age"
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
     * Binary condition = new Equal("age", 25);
     * Integer age = condition.getPropValue();   // Returns 25
     * 
     * Binary strCondition = new Like("name", "%John%");
     * String pattern = strCondition.getPropValue();   // Returns "%John%"
     * 
     * // For subquery conditions
     * Binary subCondition = new In("id", subQuery);
     * SubQuery sq = subCondition.getPropValue();   // Returns the SubQuery
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
     * Sets the value being compared against.
     * This method should generally not be used as conditions should be immutable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = new Equal("status", "active");
     * // Not recommended: condition.setPropValue("inactive");
     * }</pre>
     *
     * @param propValue the new property value
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Instead of modifying an existing condition, create a new condition with the desired value.
     *             For example, use {@code new Equal(propName, newValue)} instead of
     *             {@code existingCondition.setPropValue(newValue)}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = new Equal("status", "active");
     * List<Object> params = condition.getParameters();   // Returns ["active"]
     *
     * Binary numCondition = new GreaterThan("age", 18);
     * List<Object> numParams = numCondition.getParameters();   // Returns [18]
     *
     * // For subquery conditions
     * Binary inSubquery = new InSubQuery("id", subQuery);
     * List<Object> subParams = inSubquery.getParameters();   // Returns subquery's parameters
     * }</pre>
     *
     * @return a list of parameter values
     */
    @Override
    public List<Object> getParameters() {
        if (propValue instanceof Condition) {
            return ((Condition) propValue).getParameters();
        } else {
            return ImmutableList.of(propValue);
        }
    }

    /**
     * Clears the parameter value by setting it to null to free memory.
     * If the value is a nested Condition, delegates to that condition's clearParameters() method.
     *
     * <p>This method sets the propValue field to null unless it's a Condition,
     * in which case it recursively clears parameters in the nested condition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary eq = new Binary("age", Operator.EQUAL, 25);
     * eq.clearParameters();   // propValue becomes null
     *
     * // With nested condition
     * Binary withSubquery = new Binary("id", Operator.IN, new SubQuery("SELECT id FROM users"));
     * withSubquery.clearParameters();   // Delegates to SubQuery.clearParameters()
     * }</pre>
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
     * If the value is a Condition, it is also copied to ensure complete independence.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary original = new Equal("name", "John");
     * Binary copy = original.copy();
     * // copy is a new instance with the same values
     * 
     * // For conditions with subqueries
     * Binary originalSub = new InSubQuery("id", subQuery);
     * Binary copySub = originalSub.copy();
     * // The subquery is also deep copied
     * }</pre>
     * 
     * @param <T> the type of condition to return
     * @return a new Binary instance with copied values
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
     * The format is: propertyName OPERATOR value
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = new Equal("userName", "John");
     * String str1 = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "user_name = 'John'"
     * 
     * String str2 = condition.toString(NamingPolicy.LOWER_CAMEL_CASE);
     * // Returns: "userName = 'John'"
     * 
     * Binary numCondition = new GreaterThan("age", 18);
     * String str3 = numCondition.toString(NamingPolicy.NO_CHANGE);
     * // Returns: "age > 18"
     * }</pre>
     * 
     * @param namingPolicy the naming policy to apply to the property name
     * @return a string representation of this condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK._SPACE + parameter2String(propValue, namingPolicy);
    }

    /**
     * Returns the hash code of this Binary condition.
     * The hash code is computed based on the property name, operator, and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary c1 = new Equal("name", "John");
     * Binary c2 = new Equal("name", "John");
     * boolean sameHash = c1.hashCode() == c2.hashCode();   // true
     * }</pre>
     * 
     * @return the hash code value
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary c1 = new Equal("name", "John");
     * Binary c2 = new Equal("name", "John");
     * boolean isEqual = c1.equals(c2);   // Returns true
     * 
     * Binary c3 = new Equal("name", "Jane");
     * boolean isDifferent = c1.equals(c3);   // Returns false
     * 
     * Binary c4 = new GreaterThan("name", "John");
     * boolean diffOperator = c1.equals(c4);   // Returns false
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