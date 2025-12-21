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
 *       NOT IN may return unexpected results. In SQL, comparing any value with NULL using NOT IN
 *       evaluates to UNKNOWN, which behaves as false in WHERE clauses</li>
 *   <li>Performance: For large lists, consider using NOT EXISTS or LEFT JOIN instead</li>
 *   <li>The values list is copied during construction to ensure immutability</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
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
 * @see AbstractCondition
 */
public class NotIn extends AbstractCondition {

    /**
     * The property name to check.
     * This field stores the name of the column or property that will be compared
     * against the list of values. It's package-private for serialization frameworks.
     */
    final String propName;

    private List<?> values;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotIn instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotIn() {
        propName = null;
    }

    /**
     * Constructs a NOT IN condition for the specified property and collection of values.
     * The condition will match records where the property value is not equal to any of the
     * provided values. A defensive copy of the values collection is made to ensure immutability.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude specific product categories
     * List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
     * NotIn notIn = new NotIn("category", excludedCategories);
     * // Generates: category NOT IN ('discontinued', 'internal', 'test')
     *
     * // Exclude test users by ID
     * Set<Integer> testUserIds = new HashSet<>(Arrays.asList(1, 2, 999));
     * NotIn excludeUsers = new NotIn("user_id", testUserIds);
     * // Generates: user_id NOT IN (1, 2, 999)
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param values the collection of values that the property should NOT match.
     *               The collection is copied internally to ensure immutability.
     * @throws IllegalArgumentException if propName is null or values is null/empty
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
     * String propName = condition.getPropName();   // Returns "status"
     * }</pre>
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the collection of values that the property should NOT match.
     * Returns the internal list of values used in the NOT IN condition. These are the
     * values that will be excluded when the query is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> statuses = Arrays.asList("deleted", "archived", "suspended");
     * NotIn condition = new NotIn("status", statuses);
     * List<?> values = condition.getValues();   // Returns ["deleted", "archived", "suspended"]
     *
     * // Check how many values are being excluded
     * int count = condition.getValues().size();   // Returns 3
     *
     * // Inspect the values (useful for debugging)
     * System.out.println("Excluding values: " + condition.getValues());
     * }</pre>
     *
     * @return list of values to exclude (may be null if cleared)
     */
    public List<?> getValues() { //NOSONAR
        return values;
    }

    /**
     * Sets new values for this NOT IN condition.
     * This method allows replacing the collection of values to be excluded.
     * However, modifying conditions after creation is strongly discouraged as conditions should
     * be treated as immutable to ensure thread safety and predictable behavior.
     *
     * <p>Important notes:
     * <ul>
     *   <li>This method exists for backward compatibility only</li>
     *   <li>Using this method breaks the immutability contract of conditions</li>
     *   <li>Instead of modifying, create a new NotIn instance with the desired values</li>
     *   <li>Shared conditions modified this way can cause race conditions</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived"));
     *
     * // Not recommended - breaks immutability
     * condition.setValues(Arrays.asList("deleted", "archived", "suspended"));
     *
     * // Recommended approach - create a new condition
     * NotIn newCondition = new NotIn("status", Arrays.asList("deleted", "archived", "suspended"));
     * }</pre>
     *
     * @param values the new collection of values to exclude. Must not be null or empty.
     * @throws IllegalArgumentException if values is null or empty
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new NotIn instance instead of modifying existing conditions.
     */
    @Deprecated
    public void setValues(final List<?> values) {
        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.values = values;
    }

    /**
     * Gets the parameter values for this condition.
     * Returns the values that should be excluded when the query is executed.
     * These values will be bound to the prepared statement placeholders.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived", "suspended"));
     * List<Object> params = condition.getParameters();   // Returns ["deleted", "archived", "suspended"]
     *
     * // Check number of parameters
     * int paramCount = condition.getParameters().size();   // Returns 3
     *
     * // Use in query preparation
     * PreparedStatement stmt = connection.prepareStatement(sql);
     * List<Object> params = condition.getParameters();
     * for (int i = 0; i < params.size(); i++) {
     *     stmt.setObject(i + 1, params.get(i));
     * }
     * }</pre>
     *
     * @return an immutable list of parameter values, or empty list if values is null
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = new NotIn("status", Arrays.asList("deleted", "archived", "suspended"));
     * List<Object> parameters = condition.getParameters();          // Returns [deleted, archived, suspended]
     * condition.clearParameters();                                  // All parameters become null
     * List<Object> updatedParameters = condition.getParameters();   // Returns [null, null, null]
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * Generates the hash code for this NOT IN condition.
     * The hash code is computed based on the property name, operator, and values list,
     * ensuring consistent hashing for equivalent conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn c1 = new NotIn("status", Arrays.asList("deleted", "archived"));
     * NotIn c2 = new NotIn("status", Arrays.asList("deleted", "archived"));
     * assert c1.hashCode() == c2.hashCode();
     * }</pre>
     *
     * @return hash code based on property name, operator, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
    }

    /**
     * Checks if this NOT IN condition is equal to another object.
     * Two NOT IN conditions are equal if they have the same property name,
     * operator, and values list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn c1 = new NotIn("status", Arrays.asList("deleted", "archived"));
     * NotIn c2 = new NotIn("status", Arrays.asList("deleted", "archived"));
     * assert c1.equals(c2);   // true
     *
     * NotIn c3 = new NotIn("type", Arrays.asList("deleted", "archived"));
     * assert !c1.equals(c3);   // false - different property
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

        if (obj instanceof final NotIn other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}
