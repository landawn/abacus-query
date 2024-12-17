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

import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;
import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * This class is used to join multiple conditions. like {@code And}, {@code Or}. But must not put clause(where, order by
 * ...) into it. Those clauses are joined by {@code Criteria}.
 *
 * @see com.landawn.abacus.condition.Criteria
 */
public class Junction extends AbstractCondition {

    List<Condition> conditionList;

    // For Kryo
    Junction() {
    }

    /**
     *
     *
     * @param operator
     * @param conditions
     */
    @SafeVarargs
    public Junction(final Operator operator, final Condition... conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions);
    }

    /**
     *
     *
     * @param operator
     * @param conditions
     */
    public Junction(final Operator operator, final Collection<? extends Condition> conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions); // NOSONAR
    }

    /**
     * Gets the conditions.
     *
     * @return
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     *
     * @param conditions
     */
    @SafeVarargs
    public final void set(final Condition... conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     *
     * @param conditions
     */
    public void set(final Collection<? extends Condition> conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     *
     * @param conditions
     */
    @SafeVarargs
    public final void add(final Condition... conditions) {
        conditionList.addAll(Arrays.asList(conditions));
    }

    /**
     *
     * @param conditions
     */
    public void add(final Collection<? extends Condition> conditions) {
        conditionList.addAll(conditions);
    }

    /**
     *
     * @param conditions
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    @SafeVarargs
    public final void remove(final Condition... conditions) {
        for (final Condition cond : conditions) {
            conditionList.remove(cond);
        }
    }

    /**
     *
     * @param conditions
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void remove(final Collection<? extends Condition> conditions) {
        conditionList.removeAll(conditions);
    }

    /**
     * Clear.
     */
    public void clear() {
        conditionList.clear();
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        final List<Object> parameters = new ArrayList<>();

        for (final Condition condition : conditionList) {
            parameters.addAll(condition.getParameters());
        }

        return parameters;
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : conditionList) {
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
        final Junction result = super.copy();

        result.conditionList = new ArrayList<>();

        for (final Condition cond : conditionList) {
            result.conditionList.add(cond.copy());
        }

        return (T) result;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (N.isEmpty(conditionList)) {
            return Strings.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(_PARENTHESES_L);

            for (int i = 0; i < conditionList.size(); i++) {
                if (i > 0) {
                    sb.append(_SPACE);
                    sb.append(getOperator().toString());
                    sb.append(_SPACE);
                }

                sb.append(_PARENTHESES_L);
                sb.append(conditionList.get(i).toString(namingPolicy));
                sb.append(_PARENTHESES_R);
            }

            sb.append(_PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
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
        h = (h * 31) + operator.hashCode();
        return (h * 31) + conditionList.hashCode();
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

        if (obj instanceof final Junction other) {
            return N.equals(operator, other.operator) && N.equals(conditionList, other.conditionList);

        }

        return false;
    }
}
