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

package com.landawn.abacus.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

/**
 * Represents a NOT IN condition in SQL queries.
 * This condition checks if a property value is NOT contained in a specified collection of values.
 * 
 * <p>The NOT IN operator is useful for excluding rows where the column value matches any value 
 * in a given list. It's the opposite of the IN operator.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Exclude specific statuses
 * List<String> excludedStatuses = Arrays.asList("deleted", "archived", "inactive");
 * NotIn condition = new NotIn("status", excludedStatuses);
 * // Results in: status NOT IN ('deleted', 'archived', 'inactive')
 * 
 * // Exclude specific IDs
 * Set<Integer> excludedIds = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
 * NotIn condition2 = new NotIn("userId", excludedIds);
 * // Results in: userId NOT IN (1, 2, 3, 4, 5)
 * }</pre>
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
     * 
     * @param propName the property name to check
     * @param values the collection of values that the property should NOT match
     * @throws IllegalArgumentException if values is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> excludedTypes = Arrays.asList("temp", "draft", "test");
     * NotIn notIn = new NotIn("documentType", excludedTypes);
     * }</pre>
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
     *
     * @return list of values to exclude
     */
    public List<?> getValues() { //NOSONAR
        return values;
    }

    /**
     * Sets new values for this NOT IN condition.
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
     * Returns the values that should be excluded.
     *
     * @return list of parameter values
     */
    @Override
    public List<Object> getParameters() {
        return values == null ? N.emptyList() : (List<Object>) values;
    }

    /**
     * Clears all parameter values by setting them to null.
     * This is useful for releasing resources while maintaining the condition structure.
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
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return string representation of the NOT IN condition
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * // With values ["A", "B", "C"] and snake_case naming:
     * // "property_name NOT IN (A, B, C)"
     * }</pre>
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
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
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