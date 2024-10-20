/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 */
public class In extends AbstractCondition {

    // For Kryo
    final String propName;

    private List<?> values;

    // For Kryo
    In() {
        propName = null;
    }

    /**
     *
     *
     * @param propName
     * @param values
     */
    public In(final String propName, final Collection<?> values) {
        super(Operator.IN);

        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.propName = propName;
        this.values = new ArrayList<>(values);
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
     * Gets the values.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<?> getValues() { //NOSONAR
        return values;
    }

    /**
     * Sets the values.
     *
     * @param values the new values
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setValues(final List<?> values) {
        N.checkArgNotEmpty(values, "'values' can't be null or empty");

        this.values = values;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return values == null ? N.emptyList() : (List<Object>) values;
    }

    /**
     * Clear parameters.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void clearParameters() {
        if (N.notEmpty(values)) {
            N.fill((List) values, null);
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
        final In copy = (In) super.copy();

        copy.values = new ArrayList<>(values);

        return (T) copy;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (namingPolicy == NamingPolicy.NO_CHANGE) {
            return propName + WD._SPACE + getOperator().toString()
                    + Joiner.with(WD.COMMA_SPACE, WD.SPACE_PARENTHESES_L, WD.PARENTHESES_R).reuseCachedBuffer().appendAll(values).toString();
        } else {
            return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString()
                    + Joiner.with(WD.COMMA_SPACE, WD.SPACE_PARENTHESES_L, WD.PARENTHESES_R).reuseCachedBuffer().appendAll(values).toString();
        }
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
        return (h * 31) + ((values == null) ? 0 : values.hashCode());
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

        if (obj instanceof final In other) {
            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}
