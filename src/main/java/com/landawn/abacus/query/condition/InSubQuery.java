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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents an IN condition with a subquery in SQL-like queries.
 * This class is used to check if a property value (or multiple property values) exists 
 * in the result set of a subquery. It's commonly used for filtering records based on
 * values from another table or complex query result.
 * 
 * <p>The IN subquery condition is particularly useful when:
 * <ul>
 *   <li>Filtering based on dynamic result sets from other tables</li>
 *   <li>Avoiding joins when only checking existence</li>
 *   <li>Working with correlated or uncorrelated subqueries</li>
 *   <li>Checking multiple column combinations against subquery results</li>
 * </ul>
 * 
 * <p>This condition supports two forms:
 * <ul>
 *   <li>Single column: {@code column IN (SELECT ... FROM ...)}</li>
 *   <li>Multiple columns: {@code (column1, column2) IN (SELECT col1, col2 FROM ...)}</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single column IN subquery - find orders from premium customers
 * SubQuery premiumCustomers = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
 * InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);
 * // Generates: customer_id IN (SELECT customer_id FROM customers WHERE status = 'premium')
 * 
 * // Multiple columns IN subquery - find employees in specific department/location combinations
 * SubQuery validAssignments = Filters.subQuery("SELECT dept_id, location_id FROM allowed_assignments");
 * InSubQuery multiColumn = new InSubQuery(Arrays.asList("department_id", "location_id"), validAssignments);
 * // Generates: (department_id, location_id) IN (SELECT dept_id, location_id FROM allowed_assignments)
 * }</pre>
 * 
 * @see NotInSubQuery
 * @see In
 * @see SubQuery
 */
public class InSubQuery extends AbstractCondition {
    /**
     * The property name for single-column IN conditions.
     * This field is used for serialization frameworks like Kryo.
     */
    final String propName;

    /**
     * The property names for multi-column IN conditions.
     * This field is used for serialization frameworks like Kryo.
     */
    final Collection<String> propNames;

    private SubQuery subQuery;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized InSubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    InSubQuery() {
        propName = null;
        propNames = null;
    }

    /**
     * Creates an IN subquery condition for a single property.
     * Use this constructor when checking if a single column value exists in the subquery result.
     * The subquery should return a single column of compatible type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all products in active categories
     * SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
     * InSubQuery condition = new InSubQuery("category_id", activeCategories);
     * // Generates: category_id IN (SELECT category_id FROM categories WHERE active = true)
     *
     * // Find employees in departments with high budgets
     * SubQuery richDepts = Filters.subQuery("SELECT dept_id FROM departments WHERE budget > 1000000");
     * InSubQuery condition2 = new InSubQuery("department_id", richDepts);
     * // Generates: department_id IN (SELECT dept_id FROM departments WHERE budget > 1000000)
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param subQuery the subquery that returns the values to check against. Must not be null.
     * @throws IllegalArgumentException if propName is null or empty, or if subQuery is null
     */
    public InSubQuery(final String propName, final SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotNull(subQuery, "subQuery");

        this.propName = propName;
        this.subQuery = subQuery;
        propNames = null;
    }

    /**
     * Creates an IN subquery condition for multiple properties.
     * Use this constructor for composite key checks or when multiple columns need to match
     * the subquery results. The subquery must return the same number of columns in the same order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find employees assigned to valid department/location combinations
     * List<String> columns = Arrays.asList("dept_id", "loc_id");
     * SubQuery validCombinations = Filters.subQuery(
     *     "SELECT department_id, location_id FROM dept_locations WHERE active = 'Y'"
     * );
     * InSubQuery condition = new InSubQuery(columns, validCombinations);
     * // Generates: (dept_id, loc_id) IN (SELECT department_id, location_id FROM dept_locations WHERE active = 'Y')
     * }</pre>
     *
     * @param propNames the names of the properties to check. Must not be null or empty.
     *                  The order must match the column order in the subquery.
     * @param subQuery the subquery that returns the value combinations to check against. Must not be null.
     *                 Must return the same number of columns as propNames.size().
     * @throws IllegalArgumentException if propNames is null or empty, or if subQuery is null
     */
    public InSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotEmpty(propNames, "propNames");
        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = propNames;
        this.subQuery = subQuery;
        propName = null;
    }

    /**
     * Gets the property name for single-column IN conditions.
     * Returns null if this is a multi-column condition.
     *
     * @return the property name, or null if this is a multi-column condition
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the property names for multi-column IN conditions.
     * Returns null if this is a single-column condition.
     *
     * @return collection of property names, or null if this is a single-column condition
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    /**
     * Gets the subquery used in this IN condition.
     * The subquery defines the set of values to check against in the IN clause.
     *
     * @return the subquery
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets a new subquery for this IN condition.
     * This method allows replacing the subquery after construction.
     * However, modifying conditions after creation is strongly discouraged as conditions should
     * be treated as immutable to ensure thread safety and predictable behavior.
     *
     * <p>Important notes:
     * <ul>
     *   <li>This method exists for backward compatibility only</li>
     *   <li>Using this method breaks the immutability contract of conditions</li>
     *   <li>Instead of modifying, create a new InSubQuery instance with the desired subquery</li>
     *   <li>Shared conditions modified this way can cause race conditions</li>
     * </ul>
     *
     * @param subQuery the new subquery to set. Must not be null.
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new InSubQuery instance instead of modifying existing conditions.
     */
    @Deprecated
    public void setSubQuery(final SubQuery subQuery) {
        N.checkArgNotNull(subQuery, "subQuery");

        this.subQuery = subQuery;
    }

    /**
     * Gets the list of parameters from the subquery.
     * These are the parameter values that will be bound to the prepared statement placeholders
     * when the query is executed.
     *
     * @return list of parameter values from the subquery
     */
    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * 
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     * 
     */
    @Override
    public void clearParameters() {
        subQuery.clearParameters();
    }

    /**
     * Creates a deep copy of this InSubQuery condition.
     * The copy includes a deep copy of the subquery, ensuring complete independence
     * from the original condition.
     *
     * @param <T> the type of the condition
     * @return a new InSubQuery instance with a copy of the subquery
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final InSubQuery copy = super.copy();

        copy.subQuery = subQuery.copy();

        return (T) copy;
    }

    /**
     * Generates the hash code for this InSubQuery condition.
     * The hash code is based on the property name(s), operator, and subquery,
     * ensuring consistent hashing for equivalent conditions.
     *
     * @return hash code based on property name(s), operator, and subquery
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (Strings.isNotEmpty(propName) ? N.hashCode(propName) : N.hashCode(propNames));
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());
    }

    /**
     * Checks if this InSubQuery condition is equal to another object.
     * Two InSubQuery conditions are equal if they have the same property name(s),
     * operator, and subquery.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final InSubQuery other) {
            return N.equals(propName, other.propName) && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                    && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    /**
     * Converts this InSubQuery condition to its string representation according to the specified naming policy.
     * The naming policy is applied to the property names to handle different naming conventions
     * (e.g., camelCase to snake_case).
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return the string representation of the IN subquery condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(propName)) {
            return namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + SK.PARENTHESES_R;
        } else {
            final Function<String, String> converter = namingPolicy::convert;

            return "(" + Strings.join(N.map(propNames, converter), ", ") + ") " + getOperator().toString() + SK.SPACE_PARENTHESES_L
                    + subQuery.toString(namingPolicy) + SK.PARENTHESES_R;
        }
    }
}
