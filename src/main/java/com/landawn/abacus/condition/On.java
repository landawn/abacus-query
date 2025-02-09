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

import java.util.Map;

import com.landawn.abacus.condition.ConditionFactory.CF;

/**
 *
 */
public class On extends Cell {

    // For Kryo
    On() {
    }

    /**
     *
     *
     * @param condition
     */
    public On(final Condition condition) {
        super(Operator.ON, condition);
    }

    /**
     *
     *
     * @param propName
     * @param anoPropName
     */
    public On(final String propName, final String anoPropName) {
        this(createOnCondition(propName, anoPropName));
    }

    /**
     *
     *
     * @param propNamePair
     */
    public On(final Map<String, String> propNamePair) {
        this(createOnCondition(propNamePair));
    }

    /**
     * Creates the on condition.
     *
     * @param propName
     * @param anoPropName
     * @return
     */
    static Condition createOnCondition(final String propName, final String anoPropName) {
        return new Equal(propName, CF.expr(anoPropName));
    }

    /**
     * Creates the on condition.
     *
     * @param propNamePair
     * @return
     */
    static Condition createOnCondition(final Map<String, String> propNamePair) {
        if (propNamePair.size() == 1) {
            final Map.Entry<String, String> entry = propNamePair.entrySet().iterator().next();

            return createOnCondition(entry.getKey(), entry.getValue());
        } else {
            final And and = CF.and();

            for (final Map.Entry<String, String> entry : propNamePair.entrySet()) {
                and.add(createOnCondition(entry.getKey(), entry.getValue()));
            }

            return and;
        }
    }
}
