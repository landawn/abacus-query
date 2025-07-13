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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

/**
 * Represents an IN condition in SQL-like queries.
 * This class is used to check if a property value matches any value in a specified collection.
 * It's equivalent to multiple OR conditions but more concise and often more efficient.
 * 
 * <p>The IN condition is commonly used for:
 * <ul>
 *   <li>Checking membership in a list of values</li>
 *   <li>Filtering by multiple possible values</li>
 *   <li>Replacing multiple OR conditions</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if status is one of several values
 * In condition = new In("status", Arrays.asList("active", "pending", "approved"));
 * // This would generate: status IN ('active', 'pending', 'approved')
 * 
 * // Check if user_id is in a list
 * In userCondition = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
 * // This would generate: user_id IN (1, 2, 3, 5, 8)
 * }</pre>
 * 
 * @see NotIn
 * @see InSubQuery
 */
public class In extends AbstractCondition {

    /**
     * The property name to check.
     * This field is used for serialization frameworks like Kryo.
     */
    final String propName;

    private List<?> values;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    In() {
        propName = null;
    }

    /**
     * Creates a new IN condition with the specified property name and collection of values.
     * The condition checks if the property value matches any value in the collection.
     *
     * @param propName the name of the property to check. Must not be null.
     * @param values the collection of values to check against. Must not be null or empty.
     * 
     * <p>Example:
     * <pre>{@code
     * // Filter by multiple categories
     * Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers", "phones"));
     * In categoryFilter = new In("category", categories);
     * // Generates: category IN ('electronics', 'computers', 'phones')
     * 
     * // Filter by specific IDs
     * List<Long> ids = Arrays.asList(101L, 102L, 103L);
     * In idFilter = new In("product_id", ids);
     * // Generates: product_id IN (101, 102, 103)
     * }</pre>
     */
    public In(final String propName, final Collection<?> values) {
        super(Operator.IN);

        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.propName = propName;
        this.values = new ArrayList<>(values);
    }

    /**
     * Gets the property name being checked.
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the collection of values to check against.
     *
     * @return the list of values
     */
    public List<?> getValues() { //NOSONAR
        return values;
    }

    /**
     * Sets new values for this IN condition.
     *
     * @param values the new collection of values. Must not be null or empty.
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setValues(final List<?> values) {
        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.values = values;
    }

    /**
     * Gets all parameter values from this IN condition.
     * The returned list contains all the values that the property is being checked against.
     *
     * @return the list of values as parameters, or an empty list if no values are set
     */
    @Override
    public List<Object> getParameters() {
        return values == null ? N.emptyList() : (List<Object>) values;
    }

    /**
     * Clears all parameter values in this condition.
     * This sets all values in the list to null to release resources.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void clearParameters() {
        if (N.notEmpty(values)) {
            N.fill((List) values, null);
        }
    }

    /**
     * Creates a deep copy of this IN condition.
     *
     * @param <T> the type of the condition
     * @return a new IN instance with a copy of all values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final In copy = super.copy();

        copy.values = new ArrayList<>(values);

        return (T) copy;
    }

    /**
     * Converts this IN condition to its string representation according to the specified naming policy.
     * The output format is: propName IN (value1, value2, ..., valueN)
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return the string representation, e.g., "status IN ('active', 'pending')"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        //noinspection resource
        return Joiner.with(SK.COMMA_SPACE, namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L, SK.PARENTHESES_R)
                .reuseCachedBuffer()
                .appendAll(values)
                .toString();
    }

    /**
     * Computes the hash code for this IN condition.
     *
     * @return the hash code based on property name, operator, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Checks if this IN condition is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the object is an IN condition with the same property name, operator, and values
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final In other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}