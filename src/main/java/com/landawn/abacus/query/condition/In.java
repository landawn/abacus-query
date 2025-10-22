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

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Represents an IN condition in SQL-like queries.
 * This class is used to check if a property value matches any value in a specified collection.
 * It's equivalent to multiple OR conditions but more concise and often more efficient.
 * The IN operator is one of the most commonly used SQL operators for filtering data.
 * 
 * <p>The IN condition is particularly useful for:
 * <ul>
 *   <li>Checking membership in a list of values</li>
 *   <li>Filtering by multiple possible values efficiently</li>
 *   <li>Replacing multiple OR conditions with cleaner syntax</li>
 *   <li>Working with results from subqueries (see {@link InSubQuery})</li>
 *   <li>Implementing dynamic filters based on user selections</li>
 * </ul>
 * 
 * <p>Performance considerations:
 * <ul>
 *   <li>Most databases optimize IN conditions well for small to medium lists</li>
 *   <li>For very large lists, consider using a temporary table and JOIN</li>
 *   <li>Some databases have limits on the number of values in an IN clause</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Check if status is one of several values
 * In statusCheck = new In("status", Arrays.asList("active", "pending", "approved"));
 * // Generates: status IN ('active', 'pending', 'approved')
 * 
 * // Check if user_id is in a list
 * In userFilter = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
 * // Generates: user_id IN (1, 2, 3, 5, 8)
 * 
 * // Filter by categories
 * Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
 * In categoryFilter = new In("category", categories);
 * // Generates: category IN ('electronics', 'computers')
 * }</pre>
 * 
 * @see NotIn
 * @see InSubQuery
 * @see AbstractCondition
 */
public class In extends AbstractCondition {

    /**
     * The property name to check.
     * This field stores the name of the column or property that will be compared
     * against the list of values. It's package-private for serialization frameworks.
     */
    final String propName;

    private List<?> values;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized In instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    In() {
        propName = null;
    }

    /**
     * Creates a new IN condition with the specified property name and collection of values.
     * The condition checks if the property value matches any value in the collection.
     * This is the primary constructor for creating IN conditions.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Filter by multiple categories
     * Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers", "phones"));
     * In categoryFilter = new In("category", categories);
     * // SQL: category IN ('electronics', 'computers', 'phones')
     * 
     * // Filter by specific IDs
     * List<Long> ids = Arrays.asList(101L, 102L, 103L);
     * In idFilter = new In("product_id", ids);
     * // SQL: product_id IN (101, 102, 103)
     * 
     * // Filter by enum values
     * List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
     * In priorityFilter = new In("priority", priorities);
     * // SQL: priority IN ('HIGH', 'CRITICAL')
     * }</pre>
     *
     * @param propName the property/column name (must not be null or empty)
     * @param values the collection of values to check against. Must not be null or empty.
     *               The collection is copied to prevent external modifications.
     * @throws IllegalArgumentException if propName is null or values is null/empty
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
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
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
        return values == null ? N.emptyList() : ImmutableList.wrap((List<Object>) values);
    }

    /**
     * Clears all values in the IN clause by setting them to null to free memory.
     *
     * <p>The values list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * In in = new In("status", Arrays.asList("active", "pending", "review"));
     * List<Object> parameters = in.getParameters(); // Returns [active, pending, review]
     * in.clearParameters(); // All values become null
     * List<Object> updatedParameters = in.getParameters(); // Returns [null, null, null]
     * }</pre>
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
        final StringBuilder sb = new StringBuilder();
        sb.append(namingPolicy.convert(propName)).append(SK._SPACE).append(getOperator().toString()).append(SK.SPACE_PARENTHESES_L);

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(SK.COMMA_SPACE);
            }
            sb.append(parameter2String(values.get(i), namingPolicy));
        }

        sb.append(SK._PARENTHESES_R);
        return sb.toString();
    }

    /**
     * Computes the hash code for this IN condition.
     *
     * @return the hash code based on property name, operator, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Checks if this IN condition is equal to another object.
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is an IN condition with the same property name, operator, and values
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