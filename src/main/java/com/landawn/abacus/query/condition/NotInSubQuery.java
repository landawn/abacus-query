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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a NOT IN subquery condition used in SQL WHERE clauses.
 * This condition checks if a property value (or multiple property values) is NOT contained 
 * in the result set of a subquery.
 * 
 * <p>The NOT IN subquery is particularly useful for excluding records based on
 * dynamic criteria from another query. It supports both single-column and multi-column
 * comparisons, making it suitable for simple exclusions as well as composite key checks.</p>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>NULL handling: If the subquery returns any NULL values, NOT IN may produce unexpected results</li>
 *   <li>Performance: For large result sets, consider using NOT EXISTS instead</li>
 *   <li>Empty subquery results: If subquery returns no rows, all values pass the NOT IN check</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single property NOT IN subquery
 * SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
 * NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
 * // Generates: userId NOT IN (SELECT id FROM inactive_users)
 * 
 * // Multiple properties NOT IN subquery
 * List<String> props = Arrays.asList("firstName", "lastName");
 * SubQuery subQuery2 = Filters.subQuery("SELECT fname, lname FROM blacklist");
 * NotInSubQuery condition2 = new NotInSubQuery(props, subQuery2);
 * // Generates: (firstName, lastName) NOT IN (SELECT fname, lname FROM blacklist)
 * }</pre>
 * 
 * @see InSubQuery
 * @see NotIn
 * @see NotExists
 * @see SubQuery
 */
public class NotInSubQuery extends AbstractCondition {

    /**
     * The property name for single-column NOT IN conditions.
     * This field is used for serialization frameworks like Kryo.
     */
    final String propName;

    /**
     * The property names for multi-column NOT IN conditions.
     * This field is used for serialization frameworks like Kryo.
     */
    final Collection<String> propNames;

    private SubQuery subQuery;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NotInSubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NotInSubQuery() {
        propName = null;
        propNames = null;
    }

    /**
     * Creates a NOT IN subquery condition for a single property.
     * This checks if the property value is not present in the subquery results.
     *
     * <p>Use this constructor when comparing a single column against a subquery
     * that returns a single column of values. This is the most common use case
     * for NOT IN subqueries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude deleted items
     * SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
     * NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);
     * // Generates: itemId NOT IN (SELECT id FROM deleted_items)
     *
     * // Exclude users from specific departments
     * SubQuery deptQuery = Filters.subQuery("SELECT user_id FROM dept_users WHERE dept = 'HR'");
     * NotInSubQuery notHR = new NotInSubQuery("id", deptQuery);
     * // Generates: id NOT IN (SELECT user_id FROM dept_users WHERE dept = 'HR')
     * }</pre>
     *
     * @param propName the property/column name. Must not be null or empty.
     * @param subQuery the subquery that returns the values to check against. Must not be null.
     * @throws IllegalArgumentException if propName is null or empty, or if subQuery is null
     */
    public NotInSubQuery(final String propName, final SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotNull(subQuery, "subQuery");

        this.propName = propName;
        this.subQuery = subQuery;
        propNames = null;
    }

    /**
     * Creates a NOT IN subquery condition for multiple properties.
     * Used for composite key comparisons where multiple columns need to be
     * checked against a subquery returning multiple columns.
     *
     * <p>This constructor is useful for excluding records based on composite keys
     * or multiple related fields. The number and order of properties must match
     * the columns returned by the subquery.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude based on composite key
     * List<String> props = Arrays.asList("country", "city");
     * SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
     * NotInSubQuery condition = new NotInSubQuery(props, restricted);
     * // Generates: (country, city) NOT IN (SELECT country, city FROM restricted_locations)
     *
     * // Exclude duplicate entries
     * List<String> uniqueProps = Arrays.asList("firstName", "lastName", "email");
     * SubQuery existing = Filters.subQuery("SELECT fname, lname, email FROM existing_users");
     * NotInSubQuery noDupes = new NotInSubQuery(uniqueProps, existing);
     * // Generates: (firstName, lastName, email) NOT IN (SELECT fname, lname, email FROM existing_users)
     * }</pre>
     *
     * @param propNames collection of property names to check against the subquery results.
     *                  Must not be null or empty.
     * @param subQuery the subquery that returns the values to check against. Must not be null.
     *                 Must return the same number of columns as propNames.size().
     * @throws IllegalArgumentException if propNames is null/empty, if any element is null/empty, or if subQuery is null
     */
    public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotNull(subQuery, "subQuery");

        this.propNames = copyAndValidatePropNames(propNames);
        this.subQuery = subQuery;
        propName = null;
    }

    /**
     * Gets the property name for single-property NOT IN conditions.
     * Returns null if this is a multi-property condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
     * NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
     * String propName = condition.getPropName();
     * // Returns: "userId"
     *
     * // For multi-property conditions, getPropName() returns null
     * NotInSubQuery multiProp = new NotInSubQuery(Arrays.asList("country", "city"), subQuery);
     * String name = multiProp.getPropName();
     * // Returns: null
     * }</pre>
     *
     * @return the property name, or null if this is a multi-property condition
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the property names for multi-property NOT IN conditions.
     * Returns null if this is a single-property condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
     * NotInSubQuery condition = new NotInSubQuery(Arrays.asList("country", "city"), restricted);
     * Collection<String> propNames = condition.getPropNames();
     * // Returns: ["country", "city"]
     *
     * // For single-property conditions, getPropNames() returns null
     * NotInSubQuery singleProp = new NotInSubQuery("userId", restricted);
     * Collection<String> names = singleProp.getPropNames();
     * // Returns: null
     * }</pre>
     *
     * @return collection of property names, or null if this is a single-property condition
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    private static Collection<String> copyAndValidatePropNames(final Collection<String> propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        final List<String> copy = new ArrayList<>(propNames.size());

        for (final String propName : propNames) {
            N.checkArgNotEmpty(propName, "Property name in propNames");
            copy.add(propName);
        }

        return Collections.unmodifiableList(copy);
    }

    /**
     * Gets the subquery used in this NOT IN condition.
     * The subquery defines the set of values to exclude from the results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
     * NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);
     * SubQuery retrieved = condition.getSubQuery();
     * // Returns the SubQuery: "SELECT id FROM deleted_items"
     * }</pre>
     *
     * @return the subquery
     */
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets a new subquery for this NOT IN condition.
     * This method allows replacing the subquery after construction.
     * However, modifying conditions after creation is strongly discouraged as conditions should
     * be treated as immutable to ensure thread safety and predictable behavior.
     *
     * <p>Important notes:
     * <ul>
     *   <li>This method exists for backward compatibility only</li>
     *   <li>Using this method breaks the immutability contract of conditions</li>
     *   <li>Instead of modifying, create a new NotInSubQuery instance with the desired subquery</li>
     *   <li>Shared conditions modified this way can cause race conditions</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery originalQuery = Filters.subQuery("SELECT id FROM blocked_users");
     * NotInSubQuery condition = new NotInSubQuery("userId", originalQuery);
     *
     * // Replace subquery (deprecated - prefer creating a new NotInSubQuery instance instead)
     * SubQuery newQuery = Filters.subQuery("SELECT id FROM suspended_users");
     * condition.setSubQuery(newQuery);
     *
     * // Preferred approach: create a new NotInSubQuery instance
     * NotInSubQuery updatedCondition = new NotInSubQuery("userId", newQuery);
     * }</pre>
     *
     * @param subQuery the new subquery to set. Must not be null.
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new NotInSubQuery instance instead of modifying existing conditions.
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
        return subQuery == null ? N.emptyList() : subQuery.getParameters();
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * This method delegates to the wrapped subquery's clearParameters method.
     *
     */
    @Override
    public void clearParameters() {
        if (subQuery != null) {
            subQuery.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this NOT IN subquery condition.
     * The copy includes a deep copy of the subquery to ensure complete independence.
     *
     * @param <T> the type of condition to return
     * @return a new instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final NotInSubQuery copy = super.copy();

        if (subQuery != null) {
            copy.subQuery = subQuery.copy();
        }

        return (T) copy;
    }

    /**
     * Generates the hash code for this NOT IN subquery condition.
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
     * Checks if this NOT IN subquery condition is equal to another object.
     * Two NotInSubQuery conditions are equal if they have the same property name(s),
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

        if (obj instanceof final NotInSubQuery other) {
            return N.equals(propName, other.propName) && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                    && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    /**
     * Converts this NOT IN subquery condition to its string representation using the specified naming policy.
     * The output format depends on whether this is a single or multi-property condition.
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return string representation of the NOT IN subquery condition
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;

        if (Strings.isNotEmpty(propName)) {
            return effectiveNamingPolicy.convert(propName) + SK._SPACE + getOperator().toString() + SK.SPACE_PARENTHESES_L
                    + subQuery.toString(effectiveNamingPolicy) + SK.PARENTHESES_R;
        } else {
            final Function<String, String> converter = effectiveNamingPolicy::convert;

            return "(" + Strings.join(N.map(propNames, converter), ", ") + ") " + getOperator().toString() + SK.SPACE_PARENTHESES_L
                    + subQuery.toString(effectiveNamingPolicy) + SK.PARENTHESES_R;
        }
    }

}
