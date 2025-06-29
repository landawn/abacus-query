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

package com.landawn.abacus.condition;

import java.util.Collection;
import java.util.Iterator;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Abstract base class for all condition implementations.
 * This class provides common functionality for conditions including logical operations
 * (AND, OR, NOT), cloning support, and utility methods for string representation.
 * 
 * <p>All concrete condition classes should extend this class and implement the
 * required abstract methods from the {@link Condition} interface.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutable operator storage</li>
 *   <li>Default implementations for logical operations (and, or, not)</li>
 *   <li>Cloneable support for creating deep copies</li>
 *   <li>Utility methods for parameter and property name formatting</li>
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
 * }</pre>
 * 
 * @see Condition
 * @see Operator
 */
public abstract class AbstractCondition implements Condition, Cloneable {

    /**
     * The operator for this condition.
     * This field is immutable once set in the constructor.
     */
    protected final Operator operator;

    // For Kryo
    AbstractCondition() {
        operator = null;
    }

    /**
     * Creates a new AbstractCondition with the specified operator.
     * 
     * @param operator the operator for this condition
     */
    protected AbstractCondition(final Operator operator) {
        this.operator = operator;
    }

    /**
     * Gets the operator for this condition.
     * 
     * @return the operator
     */
    @Override
    public Operator getOperator() {
        return operator;
    }

    /**
     * Creates a new AND condition combining this condition with another.
     * 
     * @param condition the condition to AND with this condition
     * @return a new And condition containing both conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c1 = CF.eq("status", "active");
     * Condition c2 = CF.gt("age", 18);
     * Condition combined = c1.and(c2); // status = 'active' AND age > 18
     * }</pre>
     */
    @Override
    public And and(final Condition condition) {
        return new And(this, condition);
    }

    /**
     * Creates a new OR condition combining this condition with another.
     * 
     * @param condition the condition to OR with this condition
     * @return a new Or condition containing both conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c1 = CF.eq("status", "premium");
     * Condition c2 = CF.eq("status", "vip");
     * Condition combined = c1.or(c2); // status = 'premium' OR status = 'vip'
     * }</pre>
     */
    @Override
    public Or or(final Condition condition) {
        return new Or(this, condition);
    }

    /**
     * Creates a new NOT condition that negates this condition.
     * 
     * @return a new Not condition wrapping this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition c = CF.eq("status", "inactive");
     * Condition negated = c.not(); // NOT (status = 'inactive')
     * }</pre>
     */
    @Override
    public Not not() {
        return new Not(this);
    }

    /**
     * Creates a shallow copy of this condition using object cloning.
     * Subclasses should override this method to provide deep copying
     * of their specific fields.
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
     * @param parameter the parameter value to convert
     * @param namingPolicy the naming policy to apply
     * @return the string representation of the parameter
     * 
     * <p>Example:</p>
     * <pre>{@code
     * parameter2String("John", policy);     // Returns: 'John'
     * parameter2String(123, policy);        // Returns: 123
     * parameter2String(null, policy);       // Returns: null
     * parameter2String(subCondition, policy); // Returns: subCondition.toString(policy)
     * }</pre>
     */
    protected static String parameter2String(final Object parameter, final NamingPolicy namingPolicy) {
        if (parameter == null) {
            return null;
        }

        if (parameter instanceof String) {
            return WD._QUOTATION_S + parameter.toString() + WD._QUOTATION_S;
        }

        if (parameter instanceof Condition) {
            return ((Condition) parameter).toString(namingPolicy);
        }

        return parameter.toString();
    }

    /**
     * Concatenates property names into a formatted string.
     * Handles different array sizes efficiently.
     * 
     * @param propNames the property names to concatenate
     * @return a formatted string of property names
     * 
     * <p>Example:</p>
     * <pre>{@code
     * concatPropNames("name");              // Returns: name
     * concatPropNames("city", "state");     // Returns: (city, state)
     * concatPropNames("a", "b", "c");       // Returns: (a, b, c)
     * }</pre>
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
                return WD.PARENTHESES_L + propNames[0] + WD.COMMA_SPACE + propNames[1] + WD.PARENTHESES_R;

            case 3:
                return WD.PARENTHESES_L + propNames[0] + WD.COMMA_SPACE + propNames[1] + WD.COMMA_SPACE + propNames[2] + WD.PARENTHESES_R;

            default:
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(WD._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(WD.COMMA_SPACE);
                        }

                        sb.append(propNames[i]);
                    }

                    sb.append(WD._PARENTHESES_R);

                    return sb.toString();

                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Concatenates property names from a collection into a formatted string.
     * Handles different collection sizes efficiently.
     * 
     * @param propNames the collection of property names to concatenate
     * @return a formatted string of property names
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> names = Arrays.asList("city", "state", "zip");
     * concatPropNames(names); // Returns: (city, state, zip)
     * }</pre>
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
                return WD.PARENTHESES_L + it.next() + WD.COMMA_SPACE + it.next() + WD.PARENTHESES_R;

            case 3:
                return WD.PARENTHESES_L + it.next() + WD.COMMA_SPACE + it.next() + WD.COMMA_SPACE + it.next() + WD.PARENTHESES_R;

            default:

                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(WD._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(WD.COMMA_SPACE);
                        }

                        sb.append(it.next());
                    }

                    sb.append(WD._PARENTHESES_R);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
        }
    }
}