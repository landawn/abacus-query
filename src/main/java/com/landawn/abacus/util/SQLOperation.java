/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Enumeration representing SQL operation types.
 * This enum provides a comprehensive list of SQL operations including DML, DDL, DCL, and TCL commands.
 * It's primarily used internally for SQL parsing and operation identification.
 * 
 * <p>Supported operations include:</p>
 * <ul>
 *   <li>DML (Data Manipulation Language): SELECT, INSERT, UPDATE, DELETE, MERGE</li>
 *   <li>DDL (Data Definition Language): CREATE, DROP, ALTER, RENAME</li>
 *   <li>DCL (Data Control Language): USE</li>
 *   <li>TCL (Transaction Control Language): BEGIN_TRANSACTION, COMMIT, ROLLBACK</li>
 *   <li>Other: SHOW, DESCRIBE, CALL, UNKNOWN</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get operation from SQL statement
 * String sql = "SELECT * FROM users";
 * String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
 * SQLOperation op = SQLOperation.getOperation(firstWord);
 * 
 * if (op == SQLOperation.SELECT) {
 *     // Handle SELECT query
 * }
 * }</pre>
 * 
 * @see Internal
 */
@Internal
public enum SQLOperation {
    /**
     * SELECT operation for data retrieval.
     */
    SELECT(WD.SELECT),

    /**
     * INSERT operation for adding new records.
     */
    INSERT(WD.INSERT),

    /**
     * UPDATE operation for modifying existing records.
     */
    UPDATE(WD.UPDATE),

    /**
     * DELETE operation for removing records.
     */
    DELETE(WD.DELETE),

    /**
     * MERGE operation for upsert functionality.
     */
    MERGE(WD.MERGE),

    /**
     * CREATE operation for creating database objects.
     */
    CREATE(WD.CREATE),

    /**
     * DROP operation for removing database objects.
     */
    DROP(WD.DROP),

    /**
     * ALTER operation for modifying database object structure.
     */
    ALTER(WD.ALTER),

    /**
     * SHOW operation for displaying database information.
     */
    SHOW(WD.SHOW),

    /**
     * DESCRIBE operation for showing table structure.
     */
    DESCRIBE(WD.DESCRIBE),

    /**
     * USE operation for selecting a database.
     */
    USE(WD.USE),

    /**
     * RENAME operation for renaming database objects.
     */
    RENAME(WD.RENAME),

    /**
     * BEGIN_TRANSACTION operation for starting a transaction.
     */
    BEGIN_TRANSACTION(WD.BEGIN_TRANSACTION),

    /**
     * COMMIT operation for committing a transaction.
     */
    COMMIT(WD.COMMIT),

    /**
     * ROLLBACK operation for rolling back a transaction.
     */
    ROLLBACK(WD.ROLLBACK),

    /**
     * CALL operation for executing stored procedures.
     */
    CALL("CALL"),

    /**
     * UNKNOWN operation for unrecognized SQL commands.
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
     * Retrieves the SQLOperation enum value corresponding to the given operation name.
     * The lookup is case-sensitive and returns null if no matching operation is found.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * SQLOperation selectOp = SQLOperation.getOperation("SELECT"); // returns SELECT
     * SQLOperation unknownOp = SQLOperation.getOperation("TRUNCATE"); // returns null
     * }</pre>
     *
     * @param name the SQL operation name to look up
     * @return the corresponding SQLOperation enum value, or null if not found
     */
    public static SQLOperation getOperation(final String name) {
        return operationMap.get(name);
    }

    /**
     * Returns the string name of this SQL operation.
     * This is the same value used in SQL statements (e.g., "SELECT", "INSERT").
     *
     * @return the operation name as a string
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the string representation of this SQL operation.
     * This method returns the same value as {@link #getName()}.
     *
     * @return the operation name as a string
     */
    @Override
    public String toString() {
        return name;
    }
}