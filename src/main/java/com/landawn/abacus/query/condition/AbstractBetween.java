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

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Abstract base class for BETWEEN and NOT BETWEEN conditions in SQL queries.
 * This class provides all shared implementation for range-based conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>The only difference between {@link Between} and {@link NotBetween} is the operator
 * ({@code BETWEEN} vs {@code NOT BETWEEN}). All fields, getters, setters, and methods
 * for parameters, copying, string rendering, hashing, and equality are identical.</p>
 *
 * @see Between
 * @see NotBetween
 * @see AbstractCondition
 */
public abstract class AbstractBetween extends AbstractCondition {

    // For Kryo
    final String propName;

    private Object minValue;

    private Object maxValue;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractBetween() {
        propName = null;
    }

    /**
     * Creates a new BETWEEN or NOT BETWEEN condition.
     *
     * @param propName the property/column name (must not be null or empty)
     * @param operator the operator ({@link Operator#BETWEEN} or {@link Operator#NOT_BETWEEN})
     * @param minValue the minimum value (inclusive) (can be null, literal value, or subquery)
     * @param maxValue the maximum value (inclusive) (can be null, literal value, or subquery)
     * @throws IllegalArgumentException if propName is null or empty
     */
    protected AbstractBetween(final String propName, final Operator operator, final Object minValue, final Object maxValue) {
        super(operator);

        N.checkArgNotEmpty(propName, "propName");

        this.propName = propName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Gets the property name being checked.
     *
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the minimum value of the range.
     *
     * @param <T> the expected type of the minimum value
     * @return the minimum value (inclusive)
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets the minimum value of the range.
     *
     * @param minValue the new minimum value
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new instance instead.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value of the range.
     *
     * @param <T> the expected type of the maximum value
     * @return the maximum value (inclusive)
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets the maximum value of the range.
     *
     * @param maxValue the new maximum value
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Create a new instance instead.
     */
    @Deprecated
    public void setMaxValue(final Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the parameters for this condition.
     * Returns a list containing the minimum and maximum values.
     * If either value is a Condition (subquery), its parameters are included instead.
     *
     * @return a list containing [minValue, maxValue] or their parameters if they are Conditions
     */
    @Override
    public List<Object> getParameters() {
        final List<Object> parameters = new ArrayList<>();

        if (minValue instanceof Condition) {
            parameters.addAll(((Condition) minValue).getParameters());
        } else {
            parameters.add(minValue);
        }

        if (maxValue instanceof Condition) {
            parameters.addAll(((Condition) maxValue).getParameters());
        } else {
            parameters.add(maxValue);
        }

        return ImmutableList.wrap(parameters);
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * If min/max values are themselves conditions (like subqueries), their parameters are cleared.
     */
    @Override
    public void clearParameters() {
        if (minValue instanceof Condition) {
            ((Condition) minValue).clearParameters();
        } else {
            minValue = null;
        }

        if (maxValue instanceof Condition) {
            ((Condition) maxValue).clearParameters();
        } else {
            maxValue = null;
        }
    }

    /**
     * Creates a deep copy of this condition.
     * If minValue or maxValue are Conditions, they are also copied to ensure complete independence.
     *
     * @param <T> the type of condition to return
     * @return a new instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final AbstractBetween copy = super.copy();

        if (minValue instanceof Condition) {
            copy.minValue = ((Condition) minValue).copy();
        }

        if (maxValue instanceof Condition) {
            copy.maxValue = ((Condition) maxValue).copy();
        }

        return (T) copy;
    }

    /**
     * Converts this condition to its string representation.
     * The format is: propertyName BETWEEN/NOT BETWEEN minValue AND maxValue
     *
     * @param namingPolicy the naming policy to apply to the property name
     * @return a string representation
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;

        return effectiveNamingPolicy.convert(propName) + SK._SPACE + operator().toString() + SK._SPACE + parameter2String(minValue, effectiveNamingPolicy)
                + SK._SPACE + SK.AND + SK._SPACE + parameter2String(maxValue, effectiveNamingPolicy);
    }

    /**
     * Returns the hash code of this condition.
     *
     * @return hash code based on property name, operator, and range values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + ((minValue == null) ? 0 : minValue.hashCode());
        return (h * 31) + ((maxValue == null) ? 0 : maxValue.hashCode());
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the same property name,
     * operator, minValue, and maxValue.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final AbstractBetween other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}
