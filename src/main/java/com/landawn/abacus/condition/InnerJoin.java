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

import java.util.Collection;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class InnerJoin extends Join {

    private static final long serialVersionUID = -3913405148778272631L;

    // For Kryo
    InnerJoin() {
    }

    public InnerJoin(String joinEntity) {
        super(Operator.INNER_JOIN, joinEntity);
    }

    public InnerJoin(String joinEntity, Condition condition) {
        super(Operator.INNER_JOIN, joinEntity, condition);
    }

    public InnerJoin(Collection<String> joinEntities, Condition condition) {
        super(Operator.INNER_JOIN, joinEntities, condition);
    }
}