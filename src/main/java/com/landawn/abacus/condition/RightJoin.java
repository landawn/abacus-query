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

/**
 *
 */
public class RightJoin extends Join {

    // For Kryo
    RightJoin() {
    }

    /**
     *
     *
     * @param joinEntity
     */
    public RightJoin(final String joinEntity) {
        super(Operator.RIGHT_JOIN, joinEntity);
    }

    /**
     *
     *
     * @param joinEntity
     * @param condition
     */
    public RightJoin(final String joinEntity, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntity, condition);
    }

    /**
     *
     *
     * @param joinEntities
     * @param condition
     */
    public RightJoin(final Collection<String> joinEntities, final Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntities, condition);
    }
}
