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
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a BETWEEN condition in SQL queries.
 * The BETWEEN operator selects values within a given range, inclusive of both endpoints.
 * 
 * <p>The BETWEEN condition is a convenient way to filter data within a range.
 * It's equivalent to: property &gt;= minValue AND property &lt;= maxValue.
 * The condition evaluates to true if: minValue &lt;= propertyValue &lt;= maxValue.</p>
 * 
 * <p>BETWEEN can be used with various data types:</p>
 * <ul>
 *   <li>Numbers: {@code BETWEEN 1 AND 100}</li>
 *   <li>Dates: {@code BETWEEN '2023-01-01' AND '2023-12-31'}</li>
 *   <li>Strings: {@code BETWEEN 'A' AND 'M'} (alphabetical range)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Numeric range
 * Between ageRange = new Between("age", 18, 65);
 * 
 * // Date range for current year
 * Between yearRange = new Between("orderDate", 
 *     LocalDate.of(2024, 1, 1), 
 *     LocalDate.of(2024, 12, 31));
 * 
 * // Price range with subqueries
 * SubQuery minPrice = CF.subQuery("SELECT MIN(price) FROM products");
 * SubQuery maxPrice = CF.subQuery("SELECT MAX(price) FROM products");
 * Between priceRange = new Between("price", minPrice, maxPrice);
 * 
 * // String range (alphabetical)
 * Between nameRange = new Between("lastName", "A", "M");
 * }</pre>
 * 
 * @see AbstractCondition
 * @see NotBetween
 * @see Condition
 */
public class Between extends AbstractCondition {
    /**
     * The property name being checked.
     * This field stores the name of the column or property that will be tested against the range.
     * It's package-private for serialization frameworks.
     */
    final String propName;

    private Object minValue;

    private Object maxValue;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Between instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Between() {
        propName = null;
    }

    /**
     * Creates a new BETWEEN condition.
     * The condition checks if the property value falls within the specified range, inclusive.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if age is between 18 and 65 (inclusive)
     * Between ageRange = new Between("age", 18, 65);
     * 
     * // Check if salary is within a range
     * Between salaryRange = new Between("salary", 50000, 100000);
     * 
     * // Check if date is in current year
     * Between currentYear = new Between("createdDate",
     *     LocalDate.of(2024, 1, 1),
     *     LocalDate.of(2024, 12, 31));
     * 
     * // Use with subqueries for dynamic ranges
     * SubQuery avgMinus10 = CF.subQuery("SELECT AVG(score) - 10 FROM scores");
     * SubQuery avgPlus10 = CF.subQuery("SELECT AVG(score) + 10 FROM scores");
     * Between nearAverage = new Between("score", avgMinus10, avgPlus10);
     * }</pre>
     * 
     * @param propName the property/column name (must not be null or empty)
     * @param minValue the minimum value (inclusive), can be a literal or Condition
     * @param maxValue the maximum value (inclusive), can be a literal or Condition
     * @throws IllegalArgumentException if propName is null or empty
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * String prop = condition.getPropName(); // Returns "age"
     * }</pre>
     * 
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the minimum value of the range.
     * The returned value can be safely cast to its expected type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("price", 10.0, 50.0);
     * Double min = condition.getMinValue(); // Returns 10.0
     * 
     * Between dateRange = new Between("date", date1, date2);
     * LocalDate minDate = dateRange.getMinValue(); // Returns date1
     * }</pre>
     * 
     * @param <T> the expected type of the minimum value
     * @return the minimum value (inclusive)
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets the minimum value of the range.
     * This method should generally not be used as conditions should be immutable.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * // Not recommended: condition.setMinValue(21);
     * }</pre>
     * 
     * @param minValue the new minimum value
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value of the range.
     * The returned value can be safely cast to its expected type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("price", 10.0, 50.0);
     * Double max = condition.getMaxValue(); // Returns 50.0
     * 
     * Between dateRange = new Between("date", date1, date2);
     * LocalDate maxDate = dateRange.getMaxValue(); // Returns date2
     * }</pre>
     * 
     * @param <T> the expected type of the maximum value
     * @return the maximum value (inclusive)
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets the maximum value of the range.
     * This method should generally not be used as conditions should be immutable.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * // Not recommended: condition.setMaxValue(70);
     * }</pre>
     * 
     * @param maxValue the new maximum value
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("age", 18, 65);
     * List<Object> params = condition.getParameters(); // Returns [18, 65]
     * 
     * // With subqueries
     * Between subCondition = new Between("price", minSubQuery, maxSubQuery);
     * List<Object> subParams = subCondition.getParameters(); 
     * // Returns combined parameters from both subqueries
     * }</pre>
     * 
     * @return a list containing [minValue, maxValue] or their parameters if they are Conditions
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
     * Clears the min and max parameter values by setting them to null to free memory.
     * If either value is a nested Condition, delegates to that condition's clearParameters() method.
     *
     * <p>This method sets both minValue and maxValue fields to null unless they are Conditions,
     * in which case it recursively clears parameters in the nested conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between between = new Between("age", 18, 65);
     * between.clearParameters(); // Both minValue and maxValue become null
     * List<Object> parameters = between.getParameters(); // Returns [null, null]
     *
     * // With nested condition
     * Between withSubquery = new Between("id", new SubQuery("SELECT MIN(id)"), new SubQuery("SELECT MAX(id)"));
     * withSubquery.clearParameters(); // Delegates to both SubQuery.clearParameters()
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
     * Creates a deep copy of this BETWEEN condition.
     * If minValue or maxValue are Conditions, they are also copied to ensure complete independence.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between original = new Between("price", 10, 50);
     * Between copy = original.copy();
     * // copy is a new instance with the same values
     * 
     * // With subqueries
     * Between originalSub = new Between("score", minSubQuery, maxSubQuery);
     * Between copySub = originalSub.copy();
     * // Both subqueries are also deep copied
     * }</pre>
     * 
     * @param <T> the type of condition to return
     * @return a new Between instance with copied values
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
     * The format is: propertyName BETWEEN (minValue, maxValue)
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = new Between("orderDate", date1, date2);
     * String str = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "order_date BETWEEN ('2024-01-01', '2024-12-31')"
     * 
     * Between ageRange = new Between("age", 18, 65);
     * String str2 = ageRange.toString(NamingPolicy.NO_CHANGE);
     * // Returns: "age BETWEEN (18, 65)"
     * }</pre>
     * 
     * @param namingPolicy the naming policy to apply to the property name
     * @return a string representation like "propertyName BETWEEN (minValue, maxValue)"
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between b1 = new Between("age", 18, 65);
     * Between b2 = new Between("age", 18, 65);
     * boolean sameHash = b1.hashCode() == b2.hashCode(); // true
     * }</pre>
     * 
     * @return the hash code value
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
     * Checks if this BETWEEN condition is equal to another object.
     * Two BETWEEN conditions are equal if they have the same property name,
     * operator, minValue, and maxValue.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between b1 = new Between("age", 18, 65);
     * Between b2 = new Between("age", 18, 65);
     * boolean isEqual = b1.equals(b2); // Returns true
     * 
     * Between b3 = new Between("age", 21, 65);
     * boolean isDifferent = b1.equals(b3); // Returns false (different minValue)
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

        if (obj instanceof final Between other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}