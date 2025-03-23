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

import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 */
public class Binary extends AbstractCondition {

    // For Kryo
    final String propName;

    private Object propValue;

    // For Kryo
    Binary() {
        propName = null;
    }

    /**
     *
     *
     * @param propName
     * @param operator
     * @param propValue
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.propValue = propValue;
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
     * Gets the prop value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getPropValue() {
        return (T) propValue;
    }

    /**
     * Sets the prop value.
     *
     * @param propValue the new prop value
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setPropValue(final Object propValue) {
        this.propValue = propValue;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        if (propValue instanceof Condition) {
            return ((Condition) propValue).getParameters();
        } else {
            return N.asList(propValue);
        }
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if (propValue instanceof Condition) {
            ((Condition) propValue).clearParameters();
        } else {
            propValue = null;
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
        final Binary copy = super.copy();

        if (propValue instanceof Condition) {
            copy.propValue = ((Condition) propValue).copy();
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
        return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD._SPACE + parameter2String(propValue, namingPolicy);
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
        return (h * 31) + ((propValue == null) ? 0 : propValue.hashCode());
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

        if (obj instanceof final Binary other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(propValue, other.propValue);
        }

        return false;
    }
}
