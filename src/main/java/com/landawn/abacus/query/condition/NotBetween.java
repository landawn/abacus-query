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

package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a NOT BETWEEN condition in SQL queries.
 * This condition checks if a value is NOT within a specified range.
 * It's the logical opposite of the BETWEEN operator and is useful for excluding ranges of values.
 *
 * <p>The NOT BETWEEN condition is equivalent to: property &lt; minValue OR property &gt; maxValue.
 * The condition evaluates to true if the property value falls outside the specified range,
 * excluding the boundaries themselves.</p>
 *
 * <p>Common use cases include:
 * <ul>
 *   <li>Excluding values within a specific range</li>
 *   <li>Finding outliers or extreme values</li>
 *   <li>Filtering out normal operating ranges to find anomalies</li>
 *   <li>Implementing "outside business hours" logic</li>
 * </ul>
 *
 * <p>Important notes:</p>
 * <ul>
 *   <li>The range is inclusive in BETWEEN, so NOT BETWEEN excludes both boundaries</li>
 *   <li>Works with numbers, strings, dates, and other comparable types</li>
 *   <li>Can use expressions or subqueries as range boundaries</li>
 *   <li>NULL values: if the column value or either boundary is NULL, the result is NULL (not true)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude normal temperature range
 * NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
 * // SQL: temperature NOT BETWEEN 36.0 AND 37.5
 *
 * // Find orders outside business hours (before 9 AM or after 5 PM)
 * NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
 * // SQL: order_hour NOT BETWEEN 9 AND 17
 *
 * // Exclude mid-range salaries
 * NotBetween salaryRange = new NotBetween("salary", 50000, 100000);
 * // SQL: salary NOT BETWEEN 50000 AND 100000
 *
 * // Using with date strings
 * NotBetween dateRange = new NotBetween("order_date", "2024-01-01", "2024-12-31");
 * // SQL: order_date NOT BETWEEN '2024-01-01' AND '2024-12-31'
 * }</pre>
 * 
 * @see AbstractCondition
 * @see Between
 * @see Condition
 */
public class NotBetween extends AbstractCondition {
    // For Kryo
    final String propName;

    private Object minValue;

    private Object maxValue;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotBetween instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotBetween() {
        propName = null;
    }

    /**
     * Constructs a NOT BETWEEN condition for the specified property and range.
     * The condition will match values that are less than minValue OR greater than maxValue.
     * Both boundaries are excluded from the match (opposite of BETWEEN's inclusive behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find products with extreme prices (very cheap or very expensive)
     * NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
     * // SQL: price NOT BETWEEN 10.0 AND 1000.0
     * // Matches: price < 10.0 OR price > 1000.0
     *
     * // Find events outside regular working days
     * NotBetween workdays = new NotBetween("day_of_week", 2, 6);  // Monday = 2, Friday = 6
     * // SQL: day_of_week NOT BETWEEN 2 AND 6
     * // Matches: Sunday (1) and Saturday (7)
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param minValue the minimum value of the range to exclude (can be null, literal value, or subquery)
     * @param maxValue the maximum value of the range to exclude (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
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
     * Returns the name of the column or field being tested against the range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("age", 18, 65);
     * String prop = condition.getPropName(); // Returns "age"
     * }</pre>
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the minimum value of the range to exclude.
     * Values less than this will match the condition (since they fall outside the range).
     * The type parameter allows the value to be cast to the expected type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("temperature", 36.0, 37.5);
     * Double min = condition.getMinValue(); // Returns 36.0
     * }</pre>
     *
     * @param <T> the type of the minimum value
     * @return the minimum value of the excluded range
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets a new minimum value for the range.
     * Note: Modifying conditions after creation is not recommended as they should be immutable.
     * This method exists for backward compatibility but should be avoided in new code.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("age", 18, 65);
     * condition.setMinValue(21); // Not recommended - creates mutable state
     * }</pre>
     *
     * @param minValue the new minimum value to set
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new NotBetween instance instead.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value of the range to exclude.
     * Values greater than this will match the condition (since they fall outside the range).
     * The type parameter allows the value to be cast to the expected type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("temperature", 36.0, 37.5);
     * Double max = condition.getMaxValue(); // Returns 37.5
     * }</pre>
     *
     * @param <T> the type of the maximum value
     * @return the maximum value of the excluded range
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets a new maximum value for the range.
     * Note: Modifying conditions after creation is not recommended as they should be immutable.
     * This method exists for backward compatibility but should be avoided in new code.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("age", 18, 65);
     * condition.setMaxValue(70); // Not recommended - creates mutable state
     * }</pre>
     *
     * @param maxValue the new maximum value to set
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new NotBetween instance instead.
     */
    @Deprecated
    public void setMaxValue(final Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the list of parameters for this condition.
     * If min/max values are themselves conditions (like subqueries), their parameters are included.
     * Otherwise, the values themselves are returned as parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("age", 18, 65);
     * List<Object> params = condition.getParameters(); // Returns [18, 65]
     * }</pre>
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

        return ImmutableList.wrap(parameters);
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     *
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = new NotBetween("age", 18, 65);
     * List<Object> parameters = condition.getParameters(); // Returns [18, 65]
     * condition.clearParameters(); // All parameters become null
     * List<Object> updatedParameters = condition.getParameters(); // Returns [null, null]
     * }</pre>
     */
    @Override
    public void clearParameters() {
        if (minValue instanceof Condition) {
            ((Condition) minValue).clearParameters();
        } else {
            minValue = null;
        }

        if (maxValue instanceof Condition) {
            ((Condition) maxValue).clearParameters();
        } else {
            maxValue = null;
        }
    }

    /**
     * Creates a deep copy of this NOT BETWEEN condition.
     * If min/max values are conditions themselves (like expressions or subqueries),
     * they are also copied to ensure complete independence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween original = new NotBetween("score", 60, 80);
     * NotBetween copy = original.copy();
     * // copy is independent of original
     * }</pre>
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
     * The naming policy is applied to the property name to handle different naming conventions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With numeric values: "age NOT BETWEEN (18, 65)"
     * // With string values: "grade NOT BETWEEN ('A', 'C')"
     * // With date values: "order_date NOT BETWEEN ('2024-01-01', '2024-12-31')"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return string representation of the NOT BETWEEN condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L + parameter2String(minValue, namingPolicy)
                + SK.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + SK._PARENTHESES_R;
    }

    /**
     * Generates the hash code for this NOT BETWEEN condition.
     * The hash code is computed based on the property name, operator, and both range values.
     * Two NotBetween instances with the same property, operator, and range values will have
     * the same hash code, ensuring correct behavior in hash-based collections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween c1 = new NotBetween("age", 18, 65);
     * NotBetween c2 = new NotBetween("age", 18, 65);
     * assert c1.hashCode() == c2.hashCode(); // true
     * }</pre>
     *
     * @return hash code based on property name, operator, and range values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + ((minValue == null) ? 0 : minValue.hashCode());
        return (h * 31) + ((maxValue == null) ? 0 : maxValue.hashCode());
    }

    /**
     * Checks if this NOT BETWEEN condition is equal to another object.
     * Two NOT BETWEEN conditions are equal if they have the same property name,
     * operator, and range boundaries (both min and max values).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween c1 = new NotBetween("age", 18, 65);
     * NotBetween c2 = new NotBetween("age", 18, 65);
     * assert c1.equals(c2); // true
     *
     * NotBetween c3 = new NotBetween("age", 20, 65);
     * assert !c1.equals(c3); // false - different minValue
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

        if (obj instanceof final NotBetween other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}