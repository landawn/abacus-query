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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Represents an IN condition with a subquery in SQL-like queries.
 * This class is used to check if a property value (or multiple property values) exists 
 * in the result set of a subquery.
 * 
 * <p>This condition supports two forms:
 * <ul>
 *   <li>Single column: {@code column IN (SELECT ... FROM ...)}</li>
 *   <li>Multiple columns: {@code (column1, column2) IN (SELECT col1, col2 FROM ...)}</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Single column IN subquery
 * SubQuery activeUsers = new SubQuery("SELECT user_id FROM users WHERE status = 'active'");
 * InSubQuery condition = new InSubQuery("user_id", activeUsers);
 * // Generates: user_id IN (SELECT user_id FROM users WHERE status = 'active')
 * 
 * // Multiple columns IN subquery
 * SubQuery locations = new SubQuery("SELECT city, state FROM allowed_locations");
 * InSubQuery multiColumn = new InSubQuery(Arrays.asList("city", "state"), locations);
 * // Generates: (city, state) IN (SELECT city, state FROM allowed_locations)
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
     * This constructor should not be used directly in application code.
     */
    InSubQuery() {
        propName = null;
        propNames = null;
    }

    /**
     * Creates an IN subquery condition for a single property.
     * Checks if the property value exists in the result set of the subquery.
     *
     * @param propName the name of the property to check. Must not be null.
     * @param subQuery the subquery that returns the values to check against. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * SubQuery managerIds = new SubQuery("SELECT id FROM employees WHERE role = 'manager'");
     * InSubQuery condition = new InSubQuery("manager_id", managerIds);
     * // Generates: manager_id IN (SELECT id FROM employees WHERE role = 'manager')
     * }</pre>
     */
    public InSubQuery(final String propName, final SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propName = propName;
        this.subQuery = subQuery;
        propNames = null;
    }

    /**
     * Creates an IN subquery condition for multiple properties.
     * Checks if the combination of property values exists in the result set of the subquery.
     *
     * @param propNames the names of the properties to check. Must not be null or empty.
     * @param subQuery the subquery that returns the value combinations to check against. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> columns = Arrays.asList("department_id", "location_id");
     * SubQuery validCombos = new SubQuery("SELECT dept_id, loc_id FROM valid_assignments");
     * InSubQuery condition = new InSubQuery(columns, validCombos);
     * // Generates: (department_id, location_id) IN (SELECT dept_id, loc_id FROM valid_assignments)
     * }</pre>
     */
    public InSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotEmpty(propNames, "propNames");
        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propNames = propNames;
        this.subQuery = subQuery;
        propName = null;
    }

    /**
     * Gets the property name for single-column IN conditions.
     *
     * @return the property name, or null if this is a multi-column condition
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the property names for multi-column IN conditions.
     *
     * @return the collection of property names, or null if this is a single-column condition
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    /**
     * Gets the subquery used in this IN condition.
     *
     * @return the subquery
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets a new subquery for this IN condition.
     *
     * @param subQuery the new subquery to set. Must not be null.
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setSubQuery(final SubQuery subQuery) {
        this.subQuery = subQuery;
    }

    /**
     * Gets all parameters from the subquery.
     *
     * @return the list of parameters from the subquery
     */
    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }

    /**
     * Clears all parameters from the subquery.
     */
    @Override
    public void clearParameters() {
        subQuery.clearParameters();
    }

    /**
     * Creates a deep copy of this InSubQuery condition.
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
     * Computes the hash code for this InSubQuery condition.
     *
     * @return the hash code based on the property name(s), operator, and subquery
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (Strings.isNotEmpty(propName) ? N.hashCode(propName) : N.hashCode(propNames));
        h = (h * 31) + operator.hashCode();
        return (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());
    }

    /**
     * Checks if this InSubQuery condition is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the object is an InSubQuery with the same property name(s), operator, and subquery
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
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return the string representation, e.g., "user_id IN (SELECT ...)" or "(city, state) IN (SELECT ...)"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(propName)) {
            return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + WD.PARENTHESES_R;
        } else {
            final Function<String, String> func = namingPolicy::convert;

            return "(" + Strings.join(N.map(propNames, func), ", ") + ") " + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + WD.PARENTHESES_R;
        }
    }
}