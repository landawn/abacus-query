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
import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a BETWEEN condition in SQL queries.
 * The BETWEEN operator selects values within a given range, inclusive of both endpoints.
 * 
 * <p>The condition evaluates to true if: minValue <= propertyValue <= maxValue</p>
 * 
 * <p>BETWEEN can be used with various data types:</p>
 * <ul>
 *   <li>Numbers: {@code BETWEEN 1 AND 100}</li>
 *   <li>Dates: {@code BETWEEN '2023-01-01' AND '2023-12-31'}</li>
 *   <li>Strings: {@code BETWEEN 'A' AND 'M'} (alphabetical range)</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Numeric range
 * Between ageRange = new Between("age", 18, 65);
 * 
 * // Date range
 * Between dateRange = new Between("orderDate", 
 *     LocalDate.of(2023, 1, 1), 
 *     LocalDate.of(2023, 12, 31));
 * 
 * // Price range with subqueries
 * SubQuery minPrice = new SubQuery("SELECT MIN(price) FROM products");
 * SubQuery maxPrice = new SubQuery("SELECT MAX(price) FROM products");
 * Between priceRange = new Between("price", minPrice, maxPrice);
 * 
 * // String range (alphabetical)
 * Between nameRange = new Between("lastName", "A", "M");
 * }</pre>
 * 
 * @see AbstractCondition
 * @see NotBetween
 * @see GreaterEqual
 * @see LessEqual
 */
public class Between extends AbstractCondition {
    // For Kryo
    final String propName;

    private Object minValue;

    private Object maxValue;

    // For Kryo
    Between() {
        propName = null;
    }

    /**
     * Creates a new BETWEEN condition.
     * 
     * @param propName the property name to check
     * @param minValue the minimum value (inclusive), can be a literal or Condition
     * @param maxValue the maximum value (inclusive), can be a literal or Condition
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Check if salary is between 50000 and 100000
     * Between salaryRange = new Between("salary", 50000, 100000);
     * 
     * // Check if date is in current year
     * Between currentYear = new Between("createdDate",
     *     LocalDate.of(2023, 1, 1),
     *     LocalDate.of(2023, 12, 31));
     * }</pre>
     */
    public Between(final String propName, final Object minValue, final Object maxValue) {
        super(Operator.BETWEEN);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Gets the property name being checked.
     * 
     * @return the property name
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * String prop = condition.getPropName(); // Returns "age"
     * }</pre>
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the minimum value of the range.
     * 
     * @param <T> the expected type of the minimum value
     * @return the minimum value (inclusive)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between condition = new Between("price", 10.0, 50.0);
     * Double min = condition.getMinValue(); // Returns 10.0
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets the minimum value of the range.
     * 
     * @param minValue the new minimum value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value of the range.
     * 
     * @param <T> the expected type of the maximum value
     * @return the maximum value (inclusive)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between condition = new Between("price", 10.0, 50.0);
     * Double max = condition.getMaxValue(); // Returns 50.0
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets the maximum value of the range.
     * 
     * @param maxValue the new maximum value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMaxValue(final Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the parameters for this BETWEEN condition.
     * Returns a list containing the minimum and maximum values.
     * If either value is a Condition (subquery), its parameters are included instead.
     * 
     * @return a list containing [minValue, maxValue] or their parameters if they are Conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * List<Object> params = condition.getParameters(); // Returns [18, 65]
     * }</pre>
     */
    @Override
    public List<Object> getParameters() {
        final List<Object> parameters = new ArrayList<>();

        if (minValue instanceof Condition) {
            parameters.addAll(((Condition) minValue).getParameters());
        } else {
            parameters.add(minValue);
        }

        if (maxValue instanceof Condition) {
            parameters.addAll(((Condition) maxValue).getParameters());
        } else {
            parameters.add(maxValue);
        }

        return parameters;
    }

    /**
     * Clears the parameters of this BETWEEN condition.
     * If minValue or maxValue are Conditions, their parameters are cleared.
     * Otherwise, they are set to null.
     */
    @Override
    public void clearParameters() {
        if (minValue instanceof Condition) {
            ((Condition) minValue).getParameters().clear();
        } else {
            minValue = null;
        }

        if (maxValue instanceof Condition) {
            ((Condition) maxValue).getParameters().clear();
        } else {
            maxValue = null;
        }
    }

    /**
     * Creates a deep copy of this BETWEEN condition.
     * If minValue or maxValue are Conditions, they are also copied.
     * 
     * @param <T> the type of condition to return
     * @return a new Between instance with copied values
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between original = new Between("price", 10, 50);
     * Between copy = original.copy();
     * // copy is a new instance with the same values
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final Between copy = super.copy();

        if (minValue instanceof Condition) {
            copy.minValue = ((Condition) minValue).copy();
        }

        if (maxValue instanceof Condition) {
            copy.maxValue = ((Condition) maxValue).copy();
        }

        return (T) copy;
    }

    /**
     * Returns a string representation of this BETWEEN condition using the specified naming policy.
     * 
     * @param namingPolicy the naming policy to apply to the property name
     * @return a string representation like "propertyName BETWEEN (minValue, maxValue)"
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between condition = new Between("orderDate", date1, date2);
     * String str = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "order_date BETWEEN ('2023-01-01', '2023-12-31')"
     * }</pre>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L + parameter2String(minValue, namingPolicy)
                + SK.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + SK._PARENTHESES_R;
    }

    /**
     * Returns the hash code of this BETWEEN condition.
     * The hash code is computed based on property name, operator, minValue, and maxValue.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((minValue == null) ? 0 : minValue.hashCode());
        return (h * 31) + ((maxValue == null) ? 0 : maxValue.hashCode());
    }

    /**
     * Checks if this BETWEEN condition is equal to another object.
     * Two BETWEEN conditions are equal if they have the same property name,
     * operator, minValue, and maxValue.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Between b1 = new Between("age", 18, 65);
     * Between b2 = new Between("age", 18, 65);
     * boolean isEqual = b1.equals(b2); // Returns true
     * }</pre>
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Between other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}