/*
 * Copyright (C) 2021 HaiYang Li
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

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Represents a NOT BETWEEN condition in SQL queries.
 * This condition checks if a value is NOT within a specified range (exclusive of the range).
 * 
 * <p>The NOT BETWEEN operator selects values outside a given range. The values can be
 * numbers, text, dates, or even other conditions/expressions.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Exclude ages between 18 and 65
 * NotBetween condition1 = new NotBetween("age", 18, 65);
 * // Results in: age NOT BETWEEN 18 AND 65
 * 
 * // Exclude dates in a range
 * NotBetween condition2 = new NotBetween("orderDate", "2023-01-01", "2023-12-31");
 * // Results in: orderDate NOT BETWEEN '2023-01-01' AND '2023-12-31'
 * 
 * // Using with subqueries or expressions as bounds
 * Expression minExpr = new Expression("(SELECT MIN(salary) FROM employees)");
 * Expression maxExpr = new Expression("(SELECT AVG(salary) FROM employees)");
 * NotBetween condition3 = new NotBetween("salary", minExpr, maxExpr);
 * }</pre>
 */
public class NotBetween extends AbstractCondition {
    // For Kryo
    final String propName;

    private Object minValue;

    private Object maxValue;

    // For Kryo
    NotBetween() {
        propName = null;
    }

    /**
     * Constructs a NOT BETWEEN condition for the specified property and range.
     *
     * @param propName the property name to check
     * @param minValue the minimum value of the range (inclusive in BETWEEN, so excluded here)
     * @param maxValue the maximum value of the range (inclusive in BETWEEN, so excluded here)
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Exclude normal working hours
     * NotBetween notWorkHours = new NotBetween("hour", 9, 17);
     * 
     * // Exclude mid-range prices
     * NotBetween extremePrices = new NotBetween("price", 100, 1000);
     * }</pre>
     */
    public NotBetween(final String propName, final Object minValue, final Object maxValue) {
        super(Operator.NOT_BETWEEN);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Gets the property name for this NOT BETWEEN condition.
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the minimum value of the range to exclude.
     *
     * @param <T> the type of the minimum value
     * @return the minimum value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets a new minimum value for the range.
     *
     * @param minValue the new minimum value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value of the range to exclude.
     *
     * @param <T> the type of the maximum value
     * @return the maximum value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets a new maximum value for the range.
     *
     * @param maxValue the new maximum value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMaxValue(final Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the list of parameters for this condition.
     * If min/max values are themselves conditions, their parameters are included.
     *
     * @return list containing the min and max values or their parameters
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
     * Clears all parameters by setting values to null or clearing nested conditions.
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
     * Creates a deep copy of this NOT BETWEEN condition.
     * If min/max values are conditions themselves, they are also copied.
     *
     * @param <T> the type of condition to return
     * @return a new instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final NotBetween copy = super.copy();

        if (minValue instanceof Condition) {
            copy.minValue = ((Condition) minValue).copy();
        }

        if (maxValue instanceof Condition) {
            copy.maxValue = ((Condition) maxValue).copy();
        }

        return (T) copy;
    }

    /**
     * Converts this NOT BETWEEN condition to its string representation.
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return string representation of the NOT BETWEEN condition
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * // "age NOT BETWEEN (18, 65)"
     * // "price NOT BETWEEN (100.00, 1000.00)"
     * }</pre>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + parameter2String(minValue, namingPolicy)
                + WD.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + WD._PARENTHESES_R;
    }

    /**
     * Generates the hash code for this NOT BETWEEN condition.
     *
     * @return hash code based on property name, operator, and range values
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
     * Checks if this NOT BETWEEN condition is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final NotBetween other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}