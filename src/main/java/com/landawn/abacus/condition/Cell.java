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
public class Cell extends AbstractCondition {

    private Condition condition;

    // For Kryo
    Cell() {
    }

    /**
     *
     *
     * @param operator
     * @param condition
     */
    public Cell(final Operator operator, final Condition condition) {
        super(operator);
        this.condition = condition;
    }

    /**
     * Gets the condition.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Condition> T getCondition() {
        return (T) condition;
    }

    /**
     * Sets the condition.
     *
     * @param condition the new condition
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
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
        final Cell copy = super.copy();

        if (condition != null) {
            copy.condition = condition.copy();
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
        return getOperator().toString() + ((condition == null) ? Strings.EMPTY : WD._SPACE + condition.toString(namingPolicy));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((condition == null) ? 0 : condition.hashCode());
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

        if (obj instanceof final Cell other) {
            return N.equals(operator, other.operator) && N.equals(condition, other.condition);
        }

        return false;
    }
}
