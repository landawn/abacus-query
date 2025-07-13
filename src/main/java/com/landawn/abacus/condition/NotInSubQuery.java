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
import com.landawn.abacus.util.SK;

/**
 * Represents a NOT IN subquery condition used in SQL WHERE clauses.
 * This condition checks if a property value (or multiple property values) is NOT contained 
 * in the result set of a subquery.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Single property NOT IN subquery
 * SubQuery subQuery = new SubQuery("SELECT id FROM inactive_users");
 * NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
 * // Generates: userId NOT IN (SELECT id FROM inactive_users)
 * 
 * // Multiple properties NOT IN subquery
 * List<String> props = Arrays.asList("firstName", "lastName");
 * SubQuery subQuery2 = new SubQuery("SELECT fname, lname FROM blacklist");
 * NotInSubQuery condition2 = new NotInSubQuery(props, subQuery2);
 * // Generates: (firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)
 * }</pre>
 */
public class NotInSubQuery extends AbstractCondition {

    // For Kryo
    final String propName;

    // For Kryo
    final Collection<String> propNames;

    private SubQuery subQuery;

    // For Kryo
    NotInSubQuery() {
        propName = null;
        propNames = null;
    }

    /**
     * Constructs a NOT IN subquery condition for a single property.
     * 
     * @param propName the property name to check against the subquery results
     * @param subQuery the subquery that returns the values to check against
     * @throws IllegalArgumentException if subQuery is null
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT id FROM deleted_items");
     * NotInSubQuery condition = new NotInSubQuery("itemId", subQuery);
     * }</pre>
     */
    public NotInSubQuery(final String propName, final SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propName = propName;
        this.subQuery = subQuery;
        propNames = null;
    }

    /**
     * Constructs a NOT IN subquery condition for multiple properties.
     * Used for composite key comparisons where multiple columns need to be
     * checked against a subquery returning multiple columns.
     * 
     * @param propNames collection of property names to check against the subquery results
     * @param subQuery the subquery that returns the values to check against
     * @throws IllegalArgumentException if propNames is empty or subQuery is null
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> props = Arrays.asList("country", "city");
     * SubQuery subQuery = new SubQuery("SELECT country, city FROM restricted_locations");
     * NotInSubQuery condition = new NotInSubQuery(props, subQuery);
     * }</pre>
     */
    public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotEmpty(propNames, "propNames");
        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propNames = propNames;
        this.subQuery = subQuery;
        propName = null;
    }

    /**
     * Gets the property name for single-property NOT IN conditions.
     * 
     * @return the property name, or null if this is a multi-property condition
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the property names for multi-property NOT IN conditions.
     * 
     * @return collection of property names, or null if this is a single-property condition
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    /**
     * Gets the subquery used in this NOT IN condition.
     * 
     * @return the subquery
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets a new subquery for this NOT IN condition.
     * 
     * @param subQuery the new subquery to set
     */
    public void setSubQuery(final SubQuery subQuery) {
        this.subQuery = subQuery;
    }

    /**
     * Gets the list of parameters from the subquery.
     * 
     * @return list of parameter values from the subquery
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
     * Creates a deep copy of this NOT IN subquery condition.
     * 
     * @param <T> the type of condition to return
     * @return a new instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final NotInSubQuery copy = super.copy();

        copy.subQuery = subQuery.copy();

        return (T) copy;
    }

    /**
     * Generates the hash code for this NOT IN subquery condition.
     * 
     * @return hash code based on property name(s), operator, and subquery
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (Strings.isNotEmpty(propName) ? N.hashCode(propName) : N.hashCode(propNames));
        h = (h * 31) + operator.hashCode();
        return (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());
    }

    /**
     * Checks if this NOT IN subquery condition is equal to another object.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final NotInSubQuery other) {
            return N.equals(propName, other.propName) && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                    && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    /**
     * Converts this NOT IN subquery condition to its string representation using the specified naming policy.
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return string representation of the NOT IN subquery condition
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * // Single property: "user_id NOT IN (SELECT id FROM inactive_users)"
     * // Multiple properties: "(first_name, last_name) NOT IN (SELECT fname, lname FROM blacklist)"
     * }</pre>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(propName)) {
            return namingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + SK.PARENTHESES_R;
        } else {
            final Function<String, String> func = namingPolicy::convert;

            return "(" + Strings.join(N.map(propNames, func), ", ") + ") " + getOperator().toString() + SK.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + SK.PARENTHESES_R;
        }
    }

}