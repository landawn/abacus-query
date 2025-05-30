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
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class Or extends Junction {

    // For Kryo
    Or() {
    }

    /**
     *
     *
     * @param condition
     */
    public Or(final Condition... condition) {
        super(Operator.OR, condition);
    }

    /**
     *
     *
     * @param conditions
     */
    public Or(final Collection<? extends Condition> conditions) {
        super(Operator.OR, conditions);
    }

    /**
     *
     *
     * @param condition
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        final List<Condition> condList = new ArrayList<>(conditionList.size() + 1);

        condList.addAll(conditionList);
        condList.add(condition);

        return new Or(condList);
    }
}
