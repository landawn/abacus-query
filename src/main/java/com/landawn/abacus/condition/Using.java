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

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;

/**
 *
 */
public class Using extends Cell {

    // For Kryo
    Using() {
    }

    /**
     *
     *
     * @param columnNames
     */
    @SafeVarargs
    public Using(final String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     *
     *
     * @param columnNames
     */
    public Using(final Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates the using condition.
     *
     * @param columnNames
     * @return
     */
    static Condition createUsingCondition(final String... columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames can't be null or empty");
        }

        return CF.expr(concatPropNames(columnNames));
    }

    /**
     * Creates the using condition.
     *
     * @param columnNames
     * @return
     */
    static Condition createUsingCondition(final Collection<String> columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames " + columnNames + " must has one or more than one column name. ");
        }

        return CF.expr(concatPropNames(columnNames));
    }
}
