/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.annotation.Internal;

/**
 * The Enum SQLOperation.
 *
 */
@Internal
public enum SQLOperation {
    /**
     * Field SELECT.
     */
    SELECT(WD.SELECT),
    /**
     * Field INSERT.
     */
    INSERT(WD.INSERT),
    /**
     * Field UPDATE.
     */
    UPDATE(WD.UPDATE),
    /**
     * Field DELETE.
     */
    DELETE(WD.DELETE),
    /**
     * Field MERGE.
     */
    MERGE(WD.MERGE),
    /**
     * Field CREATE.
     */
    CREATE(WD.CREATE),
    /**
     * Field DROP.
     */
    DROP(WD.DROP),
    /**
     * Field ALTER.
     */
    ALTER(WD.ALTER),
    /**
     * Field SHOW.
     */
    SHOW(WD.SHOW),
    /**
     * Field DESCRIBE.
     */
    DESCRIBE(WD.DESCRIBE),
    /**
     * Field USE.
     */
    USE(WD.USE),
    /**
     * Field RENAME.
     */
    RENAME(WD.RENAME),
    /**
     * Field BEGIN_TRANSACTION.
     */
    BEGIN_TRANSACTION(WD.BEGIN_TRANSACTION),
    /**
     * Field COMMIT.
     */
    COMMIT(WD.COMMIT),
    /**
     * Field ROLLBACK.
     */
    ROLLBACK(WD.ROLLBACK),
    /**
     * Field CALL.
     */
    CALL("CALL"),
    /**
     * Field UNKNOWN.
     */
    UNKNOWN("UNKNOWN");

    private final String name;

    SQLOperation(final String name) {
        this.name = name;
    }

    private static final Map<String, SQLOperation> operationMap = new HashMap<>();

    static {
        final SQLOperation[] values = SQLOperation.values();

        for (final SQLOperation value : values) {
            operationMap.put(value.name, value);
        }
    }

    /**
     * Gets the operation.
     *
     * @param name
     * @return
     */
    public static SQLOperation getOperation(final String name) {
        return operationMap.get(name);
    }

    /**
     * Gets the name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return name;
    }
}
