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

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 */
public class Between extends AbstractCondition {
    // For Kryo
    final String propName;

    private Object minValue;

    private Object maxValue;

    // For Kryo
    Between() {
        propName = null;
    }

    /**
     *
     *
     * @param propName
     * @param minValue
     * @param maxValue
     */
    public Between(final String propName, final Object minValue, final Object maxValue) {
        super(Operator.BETWEEN);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Gets the prop name.
     *
     * @return
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the min value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets the min value.
     *
     * @param minValue the new min value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMinValue(final Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the max value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets the max value.
     *
     * @param maxValue the new max value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setMaxValue(final Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the parameters.
     *
     * @return
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

        return parameters;
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if (minValue instanceof Condition) {
            ((Condition) minValue).getParameters().clear();
        } else {
            minValue = null;
        }

        if (maxValue instanceof Condition) {
            ((Condition) maxValue).getParameters().clear();
        } else {
            maxValue = null;
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final Between copy = super.copy();

        if (minValue instanceof Condition) {
            copy.minValue = ((Condition) minValue).copy();
        }

        if (maxValue instanceof Condition) {
            copy.maxValue = ((Condition) maxValue).copy();
        }

        return (T) copy;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + parameter2String(minValue, namingPolicy)
                + WD.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + WD._PARENTHESES_R;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((minValue == null) ? 0 : minValue.hashCode());
        return (h * 31) + ((maxValue == null) ? 0 : maxValue.hashCode());
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Between other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}
