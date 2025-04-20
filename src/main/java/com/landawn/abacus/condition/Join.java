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

import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 *
 */
public class Join extends AbstractCondition {

    private List<String> joinEntities;

    private Condition condition;

    // For Kryo
    Join() {
    }

    /**
     *
     *
     * @param joinEntity
     */
    public Join(final String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    protected Join(final Operator operator, final String joinEntity) {
        this(operator, joinEntity, null);
    }

    /**
     *
     *
     * @param joinEntity
     * @param condition
     */
    public Join(final String joinEntity, final Condition condition) {
        this(Operator.JOIN, joinEntity, condition);
    }

    protected Join(final Operator operator, final String joinEntity, final Condition condition) {
        this(operator, Array.asList(joinEntity), condition);
    }

    /**
     *
     *
     * @param joinEntities
     * @param condition
     */
    public Join(final Collection<String> joinEntities, final Condition condition) {
        this(Operator.JOIN, joinEntities, condition);
    }

    protected Join(final Operator operator, final Collection<String> joinEntities, final Condition condition) {
        super(operator);
        this.joinEntities = new ArrayList<>(joinEntities);
        this.condition = condition;
    }

    /**
     * Gets the join entities.
     *
     * @return
     */
    public List<String> getJoinEntities() {
        return joinEntities;
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
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final Join copy = super.copy();

        if (joinEntities != null) {
            copy.joinEntities = new ArrayList<>(joinEntities);
        }

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
        return getOperator().toString() + _SPACE + concatPropNames(joinEntities)
                + ((condition == null) ? Strings.EMPTY : (_SPACE + getCondition().toString(namingPolicy)));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + joinEntities.hashCode();

        if (condition != null) {
            h = (h * 31) + condition.hashCode();
        }

        return h;
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

        if (obj instanceof final Join other) {
            return N.equals(operator, other.operator) && N.equals(joinEntities, other.joinEntities) && N.equals(condition, other.condition);
        }

        return false;
    }
}
