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

import java.util.Collection;
import java.util.Iterator;

import com.landawn.abacus.query.SK;
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
 * <p>Usage example:</p>
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
 * Condition combined = c1.and(c2); // Inherited method
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

    // For Kryo
    AbstractCondition() {
        operator = null;
    }

    /**
     * Creates a new AbstractCondition with the specified operator.
     * The operator is immutable once set and defines the behavior of this condition.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // In a subclass constructor
     * public Equal(String propName, Object propValue) {
     *     super(Operator.EQUAL); // Sets the operator
     *     this.propName = propName;
     *     this.propValue = propValue;
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
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.eq("status", "active");
     * Operator op = condition.getOperator(); // Returns Operator.EQUAL
     * 
     * Condition andCondition = CF.and(c1, c2);
     * Operator andOp = andCondition.getOperator(); // Returns Operator.AND
     * }</pre>
     * 
     * @return the operator for this condition
     */
    @Override
    public Operator getOperator() {
        return operator;
    }

    /**
     * Creates a new AND condition combining this condition with another.
     * Both conditions must be true for the AND condition to be true.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c1 = CF.eq("status", "active");
     * Condition c2 = CF.gt("age", 18);
     * And combined = c1.and(c2); 
     * // Results in: status = 'active' AND age > 18
     * 
     * // Can be chained
     * Condition c3 = CF.lt("age", 65);
     * And allConditions = c1.and(c2).and(c3);
     * // Results in: status = 'active' AND age > 18 AND age < 65
     * }</pre>
     * 
     * @param condition the condition to AND with this condition
     * @return a new And condition containing both conditions
     */
    @Override
    public And and(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        return new And(this, condition);
    }

    /**
     * Creates a new OR condition combining this condition with another.
     * At least one condition must be true for the OR condition to be true.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c1 = CF.eq("status", "premium");
     * Condition c2 = CF.eq("status", "vip");
     * Or combined = c1.or(c2);
     * // Results in: status = 'premium' OR status = 'vip'
     * 
     * // Can be chained
     * Condition c3 = CF.eq("status", "gold");
     * Or anyStatus = c1.or(c2).or(c3);
     * // Results in: status = 'premium' OR status = 'vip' OR status = 'gold'
     * }</pre>
     * 
     * @param condition the condition to OR with this condition
     * @return a new Or condition containing both conditions
     */
    @Override
    public Or or(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        return new Or(this, condition);
    }

    /**
     * Creates a new NOT condition that negates this condition.
     * The NOT condition is true when this condition is false, and vice versa.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c = CF.eq("status", "inactive");
     * Not negated = c.not();
     * // Results in: NOT (status = 'inactive')
     * 
     * // Double negation
     * Not doubleNegated = negated.not();
     * // Results in: NOT (NOT (status = 'inactive'))
     * 
     * // Complex negation
     * Condition complex = CF.and(
     *     CF.eq("type", "guest"),
     *     CF.lt("visits", 3)
     * );
     * Not negatedComplex = complex.not();
     * // Results in: NOT (type = 'guest' AND visits < 3)
     * }</pre>
     * 
     * @return a new Not condition wrapping this condition
     */
    @Override
    public Not not() {
        return new Not(this);
    }

    /**
     * Creates a shallow copy of this condition using object cloning.
     * Subclasses should override this method to provide deep copying
     * of their specific fields to ensure complete independence between copies.
     * 
     * <p>Example implementation in a subclass:</p>
     * <pre>{@code
     * @Override
     * public <T extends Condition> T copy() {
     *     Binary copy = super.copy(); // Shallow copy
     *     // Deep copy any mutable fields
     *     if (propValue instanceof Condition) {
     *         copy.propValue = ((Condition) propValue).copy();
     *     }
     *     return (T) copy;
     * }
     * }</pre>
     * 
     * @param <T> the type of condition to return
     * @return a copy of this condition
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        AbstractCondition copy = null;

        try {
            copy = (AbstractCondition) super.clone();
        } catch (final CloneNotSupportedException e) {
            // ignore, won't happen.
        }

        return (T) copy;
    }

    /**
     * Returns a string representation of this condition using the default naming policy.
     * This method delegates to {@link #toString(NamingPolicy)} with {@link NamingPolicy#NO_CHANGE}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.eq("userName", "John");
     * String str = condition.toString();
     * // Returns: "userName = 'John'"
     * }</pre>
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
     * <p>Example:</p>
     * <pre>{@code
     * parameter2String("John", policy);     // Returns: 'John'
     * parameter2String(123, policy);        // Returns: 123
     * parameter2String(null, policy);       // Returns: null
     * parameter2String(subCondition, policy); // Returns: subCondition.toString(policy)
     * }</pre>
     * 
     * @param parameter the parameter value to convert
     * @param namingPolicy the naming policy to apply
     * @return the string representation of the parameter
     */
    protected static String parameter2String(final Object parameter, final NamingPolicy namingPolicy) {
        if (parameter == null) {
            return null;
        }

        if (parameter instanceof String) {
            return SK._QUOTATION_S + parameter.toString() + SK._QUOTATION_S;
        }

        if (parameter instanceof Condition) {
            if (parameter.equals(IsNull.NULL)) {
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
     * <p>Example:</p>
     * <pre>{@code
     * concatPropNames("name");              // Returns: name
     * concatPropNames("city", "state");     // Returns: (city, state)
     * concatPropNames("a", "b", "c");       // Returns: (a, b, c)
     * }</pre>
     * 
     * @param propNames the property names to concatenate
     * @return a formatted string of property names
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
                return SK.PARENTHESES_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.PARENTHESES_R;

            case 3:
                return SK.PARENTHESES_L + propNames[0] + SK.COMMA_SPACE + propNames[1] + SK.COMMA_SPACE + propNames[2] + SK.PARENTHESES_R;

            default:
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(propNames[i]);
                    }

                    sb.append(SK._PARENTHESES_R);

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
     * <p>Example:</p>
     * <pre>{@code
     * List<String> names = Arrays.asList("city", "state", "zip");
     * concatPropNames(names); // Returns: (city, state, zip)
     * 
     * Set<String> single = Collections.singleton("id");
     * concatPropNames(single); // Returns: id
     * }</pre>
     * 
     * @param propNames the collection of property names to concatenate
     * @return a formatted string of property names
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
                return SK.PARENTHESES_L + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESES_R;

            case 3:
                return SK.PARENTHESES_L + it.next() + SK.COMMA_SPACE + it.next() + SK.COMMA_SPACE + it.next() + SK.PARENTHESES_R;

            default:

                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(SK._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(it.next());
                    }

                    sb.append(SK._PARENTHESES_R);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
        }
    }
}