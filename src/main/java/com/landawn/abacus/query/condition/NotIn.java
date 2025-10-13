/*
 * Copyright (C) 2020 HaiYang Li
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
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Represents a NOT IN condition in SQL queries.
 * This condition checks if a property value is NOT contained in a specified collection of values.
 * It's the logical opposite of the IN operator and is useful for exclusion-based filtering.
 * 
 * <p>The NOT IN operator is particularly useful for:
 * <ul>
 *   <li>Excluding records with specific status values</li>
 *   <li>Filtering out test or system data</li>
 *   <li>Implementing blacklist-based filtering</li>
 *   <li>Finding records that don't match any value in a list</li>
 * </ul>
 * 
 * <p>Important considerations:
 * <ul>
 *   <li>NULL handling: If the list contains NULL or the column has NULL values, 
 *       the behavior may be unexpected (typically returns no rows)</li>
 *   <li>Performance: For large lists, consider using NOT EXISTS or LEFT JOIN instead</li>
 *   <li>The values list is copied during construction to ensure immutability</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Exclude inactive statuses
 * List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
 * NotIn condition = new NotIn("status", inactiveStatuses);
 * // Results in: status NOT IN ('deleted', 'archived', 'suspended')
 * 
 * // Exclude specific department IDs
 * Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
 * NotIn deptCondition = new NotIn("department_id", excludedDepts);
 * // Results in: department_id NOT IN (10, 20, 30)
 * 
 * // Exclude test users
 * List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
 * NotIn emailCondition = new NotIn("email", testEmails);
 * }</pre>
 * 
 * @see In
 * @see NotInSubQuery
 */
public class NotIn extends AbstractCondition {

    // For Kryo
    final String propName;

    private List<?> values;

    // For Kryo
    NotIn() {
        propName = null;
    }

    /**
     * Constructs a NOT IN condition for the specified property and collection of values.
     * The condition will match records where the property value is not equal to any of the
     * provided values. A defensive copy of the values collection is made to ensure immutability.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Exclude specific product categories
     * List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
     * NotIn notIn = new NotIn("category", excludedCategories);
     * // Use in query: SELECT * FROM products WHERE category NOT IN ('discontinued', 'internal', 'test')
     * }</pre>
     * 
     * @param propName the property name to check
     * @param values the collection of values that the property should NOT match. 
     *               The collection is copied to ensure immutability.
     * @throws IllegalArgumentException if values is null or empty
     */
    public NotIn(final String propName, final Collection<?> values) {
        super(Operator.NOT_IN);

        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.propName = propName;
        this.values = new ArrayList<>(values);
    }

    /**
     * Gets the property name for this NOT IN condition.
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the collection of values that the property should NOT match.
     * Returns the internal list of values. Modifications to this list are discouraged
     * as conditions should be immutable.
     *
     * @return list of values to exclude
     */
    public List<?> getValues() { //NOSONAR
        return values;
    }

    /**
     * Sets new values for this NOT IN condition.
     * Note: Modifying conditions after creation is not recommended as they should be immutable.
     * Consider creating a new condition instead.
     *
     * @param values the new collection of values to exclude
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setValues(final List<?> values) {
        this.values = values;
    }

    /**
     * Gets the parameter values for this condition.
     * Returns the values that should be excluded when the query is executed.
     * These values will be bound to the prepared statement placeholders.
     *
     * @return list of parameter values, or empty list if values is null
     */
    @Override
    public List<Object> getParameters() {
        return values == null ? N.emptyList() : ImmutableList.wrap((List<Object>) values);
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * 
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<Object> parameters = condition.getParameters(); // e.g., [1, 2, 3, 4, 5]
     * condition.clearParameters(); // All parameters become null
     * List<Object> updatedParameters = condition.getParameters(); // Returns [null, null, null, null, null]
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
     * Creates a deep copy of this NOT IN condition.
     * The copy includes a new list containing the same values, ensuring complete
     * independence from the original condition.
     * 
     * <p>Example usage:
     * <pre>{@code
     * NotIn original = new NotIn("status", Arrays.asList("inactive", "deleted"));
     * NotIn copy = original.copy();
     * // copy is independent of original
     * }</pre>
     *
     * @param <T> the type of condition to return
     * @return a new instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final NotIn copy = super.copy();

        copy.values = new ArrayList<>(values);

        return (T) copy;
    }

    /**
     * Converts this NOT IN condition to its string representation using the specified naming policy.
     * The naming policy is applied to the property name to handle different naming conventions.
     * Values are formatted appropriately based on their types.
     * 
     * <p>Example output:
     * <pre>{@code
     * // With values ["A", "B", "C"] and snake_case naming:
     * // "property_name NOT IN (A, B, C)"
     * 
     * // With numeric values [1, 2, 3]:
     * // "property_name NOT IN (1, 2, 3)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return string representation of the NOT IN condition
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
     * Generates the hash code for this NOT IN condition.
     * The hash code is computed based on the property name, operator, and values list.
     *
     * @return hash code based on property name, operator, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Checks if this NOT IN condition is equal to another object.
     * Two NOT IN conditions are equal if they have the same property name,
     * operator, and values list.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final NotIn other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}