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

import static com.landawn.abacus.query.SK.COMMA_SPACE;
import static com.landawn.abacus.query.SK.SPACE;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for all condition implementations.
 * This class provides common functionality for conditions including logical operations
 * (AND, OR, NOT), cloning support, and utility methods for string representation.
 * 
 * <p>AbstractCondition serves as the foundation for the condition hierarchy, implementing
 * the {@link Condition} interface and providing default implementations for common operations.
 * All concrete condition classes should extend this class to inherit standard behavior
 * and ensure consistency across the framework.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutable operator storage</li>
 *   <li>Default implementations for logical operations (and, or, not)</li>
 *   <li>Cloneable support for creating deep copies</li>
 *   <li>Utility methods for parameter and property name formatting</li>
 *   <li>Standard toString() implementation</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Concrete implementations extend this class
 * public class Equal extends Binary {
 *     public Equal(String propName, Object propValue) {
 *         super(propName, Operator.EQUAL, propValue);
 *     }
 * }
 * 
 * // Using logical operations inherited from AbstractCondition
 * Condition c1 = new Equal("status", "active");
 * Condition c2 = new GreaterThan("age", 18);
 * Condition combined = c1.and(c2);   // Inherited method
 * }</pre>
 * 
 * @see Condition
 * @see Operator
 */
public abstract class AbstractCondition implements Condition, Cloneable {

    /**
     * The operator for this condition.
     * This field is immutable once set in the constructor and defines
     * the type of operation this condition represents.
     */
    protected final Operator operator;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized AbstractCondition instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    AbstractCondition() {
        operator = null;
    }

    /**
     * Creates a new AbstractCondition with the specified operator.
     * The operator is immutable once set and defines the behavior of this condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // In a subclass constructor
     * abstract class CustomCondition extends LogicalCondition {
     *     CustomCondition() {
     *         super(Operator.EQUAL);   // Sets the operator
     *     }
     * }
     * }</pre>
     *
     * @param operator the operator for this condition (must not be null)
     */
    protected AbstractCondition(final Operator operator) {
        this.operator = N.requireNonNull(operator, "operator");
    }

    /**
     * Gets the operator for this condition.
     * The operator defines the type of operation (e.g., EQUAL, GREATER_THAN, AND, OR).
     *
     * @return the operator for this condition
     */
    @Override
    public Operator operator() {
        return operator;
    }

    /**
     * Creates a shallow copy of this condition using object cloning.
     * Subclasses should override this method to provide deep copying
     * of their specific fields to ensure complete independence between copies.
     *
     * @param <T> the type of condition to return
     * @return a shallow copy of this condition
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        try {
            return (T) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError("CloneNotSupportedException should never happen since AbstractCondition implements Cloneable", e);
        }
    }

    /**
     * Returns a string representation of this condition using the default naming policy.
     * This method delegates to {@link #toString(NamingPolicy)} with {@link NamingPolicy#NO_CHANGE}.
     *
     * @return a string representation of this condition
     */
    @Override
    public String toString() {
        return toString(NamingPolicy.NO_CHANGE);
    }

    /**
     * Converts a parameter value to its string representation for use in condition strings.
     * Handles special cases like strings (adds quotes), conditions (recursive toString),
     * and null values.
     *
     * <p>This utility method is used internally by condition implementations to format
     * parameter values consistently across the framework.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>Strings are wrapped in single quotes: 'value'</li>
     *   <li>Numbers are returned as-is: 123</li>
     *   <li>null returns null</li>
     *   <li>Conditions use recursive toString with naming policy</li>
     *   <li>Other objects use their toString() method</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * formatParameter("John", NamingPolicy.NO_CHANGE);         // Returns: 'John'
     * formatParameter(123, NamingPolicy.NO_CHANGE);            // Returns: 123
     * formatParameter(null, NamingPolicy.NO_CHANGE);           // Returns: null
     * formatParameter(subCondition, NamingPolicy.NO_CHANGE);   // Returns: subCondition.toString(policy)
     * }</pre>
     *
     * @param parameter the parameter value to convert
     * @param namingPolicy the naming policy to apply to property names within conditions
     * @return the string representation of the parameter, or null if parameter is null
     */
    protected static String formatParameter(final Object parameter, final NamingPolicy namingPolicy) {
        if (parameter == null) {
            return null;
        }

        if (parameter instanceof String) {
            return SK._SINGLE_QUOTE + parameter.toString() + SK._SINGLE_QUOTE;
        }

        if (parameter instanceof Condition) {
            if (parameter == IsNull.NULL || parameter == IsNaN.NAN || parameter == IsInfinite.INFINITE) { //NOSONAR
                return parameter.toString();
            } else {
                return ((Condition) parameter).toString(namingPolicy);
            }
        }

        return parameter.toString();
    }

    /**
     * Concatenates property names into a formatted string.
     * Handles different array sizes efficiently, adding parentheses for multiple names.
     *
     * <p>This utility method is used internally for formatting multiple property names
     * in conditions like GROUP BY or ORDER BY.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>Single name: returned as-is without parentheses</li>
     *   <li>Multiple names: enclosed in parentheses and comma-separated</li>
     *   <li>Empty array: returns empty string</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * concatPropNames("name");            // Returns: name
     * concatPropNames("city", "state");   // Returns: (city, state)
     * concatPropNames("a", "b", "c");     // Returns: (a, b, c)
     * concatPropNames();                  // Returns: ""
     * }</pre>
     *
     * @param propNames the property names to concatenate (varargs, can be empty)
     * @return a formatted string of property names, empty string if no names provided
     */
    protected static String concatPropNames(final String... propNames) {
        if (N.isEmpty(propNames)) {
            return Strings.EMPTY;
        }

        final int size = propNames.length;

        switch (size) {
            case 1:
                return propNames[0];

            case 2:
                return SK.PARENTHESIS_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.PARENTHESIS_R;

            case 3:
                return SK.PARENTHESIS_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.COMMA_SPACE + propNames[2] + SK.PARENTHESIS_R;

            default:
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESIS_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(propNames[i]);
                    }

                    sb.append(SK._PARENTHESIS_R);

                    return sb.toString();

                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Concatenates property names from a collection into a formatted string.
     * Handles different collection sizes efficiently, adding parentheses for multiple names.
     *
     * <p>This utility method is used internally for formatting multiple property names
     * from collections in conditions like IN or GROUP BY.</p>
     *
     * <p>Formatting rules:</p>
     * <ul>
     *   <li>Single name: returned as-is without parentheses</li>
     *   <li>Multiple names: enclosed in parentheses and comma-separated</li>
     *   <li>Empty collection: returns empty string</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("city", "state", "zip");
     * concatPropNames(names);   // Returns: (city, state, zip)
     *
     * Set<String> single = Collections.singleton("id");
     * concatPropNames(single);   // Returns: id
     *
     * List<String> empty = Collections.emptyList();
     * concatPropNames(empty);   // Returns: ""
     * }</pre>
     *
     * @param propNames the collection of property names to concatenate (can be empty)
     * @return a formatted string of property names, empty string if collection is empty
     */
    protected static String concatPropNames(final Collection<String> propNames) {
        if (N.isEmpty(propNames)) {
            return Strings.EMPTY;
        }

        final Iterator<String> it = propNames.iterator();
        final int size = propNames.size();

        switch (size) {
            case 1:
                return it.next();

            case 2:
                return SK.PARENTHESIS_L + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESIS_R;

            case 3:
                return SK.PARENTHESIS_L + it.next() + SK.COMMA_SPACE + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESIS_R;

            default:

                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESIS_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(it.next());
                    }

                    sb.append(SK._PARENTHESIS_R);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Creates a comma-separated string of property names for ordering.
     * This is an internal helper method used by OrderBy and GroupBy constructors.
     *
     * <p>This method is package-private and not intended for direct use by application code.
     * Use the public OrderBy constructors instead.</p>
     *
     * @param propNames array of property names. Must not be null or empty.
     * @return formatted string for ORDER BY clause
     * @throws IllegalArgumentException if propNames is null, empty, or contains null/empty elements
     */
    static String createSortExpression(final String... propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                if (Strings.isEmpty(propName)) {
                    throw new IllegalArgumentException("Property name cannot be null or empty");
                }

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates an ordering condition for a single property with direction.
     * This is an internal helper method used by OrderBy and GroupBy constructors.
     *
     * <p>This method is package-private and not intended for direct use by application code.
     * Use the public OrderBy constructors instead.</p>
     *
     * @param propName the property name
     * @param direction the sort direction
     * @return formatted string for ORDER BY clause
     * @throws IllegalArgumentException if propName is null/empty or direction is null
     */
    static String createSortExpression(final String propName, final SortDirection direction) {
        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        return propName + SPACE + direction;
    }

    /**
     * Creates an ordering condition for multiple properties with the same direction.
     * This is an internal helper method used by OrderBy and GroupBy constructors.
     *
     * <p>This method is package-private and not intended for direct use by application code.
     * Use the public OrderBy constructors instead.</p>
     *
     * @param propNames collection of property names
     * @param direction the sort direction
     * @return formatted string for ORDER BY clause
     * @throws IllegalArgumentException if propNames is null/empty, direction is null, or propNames contains null/empty elements
     */
    static String createSortExpression(final Collection<String> propNames, final SortDirection direction) {
        N.checkArgNotEmpty(propNames, "propNames");

        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final String propName : propNames) {
                if (Strings.isEmpty(propName)) {
                    throw new IllegalArgumentException("Property name in collection cannot be null or empty");
                }

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
                sb.append(SPACE);
                sb.append(direction);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates an ordering condition from a map of properties and their directions.
     * This is an internal helper method used by OrderBy and GroupBy constructors.
     *
     * <p>This method is package-private and not intended for direct use by application code.
     * Use the public OrderBy constructors instead.</p>
     *
     * @param orders map of property names to sort directions
     * @return formatted string for ORDER BY clause
     * @throws IllegalArgumentException if orders is null/empty, or contains null/empty keys or null values
     */
    static String createSortExpression(final Map<String, SortDirection> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders cannot be null or empty");
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
                final String propName = entry.getKey();
                final SortDirection direction = entry.getValue();

                if (Strings.isEmpty(propName)) {
                    throw new IllegalArgumentException("Property name in orders cannot be null or empty");
                }
                if (direction == null) {
                    throw new IllegalArgumentException("SortDirection in orders cannot be null");
                }

                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
                sb.append(SPACE);
                sb.append(direction);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }
}
