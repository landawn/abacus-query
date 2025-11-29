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

package com.landawn.abacus.query;

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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get operation from SQL statement
 * String sql = "SELECT * FROM users";
 * String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
 * SQLOperation op = SQLOperation.of(firstWord);
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
    SELECT(SK.SELECT),

    /**
     * INSERT operation for adding new records.
     */
    INSERT(SK.INSERT),

    /**
     * UPDATE operation for modifying existing records.
     */
    UPDATE(SK.UPDATE),

    /**
     * DELETE operation for removing records.
     */
    DELETE(SK.DELETE),

    /**
     * MERGE operation for upsert functionality.
     */
    MERGE(SK.MERGE),

    /**
     * CREATE operation for creating database objects.
     */
    CREATE(SK.CREATE),

    /**
     * DROP operation for removing database objects.
     */
    DROP(SK.DROP),

    /**
     * ALTER operation for modifying database object structure.
     */
    ALTER(SK.ALTER),

    /**
     * SHOW operation for displaying database information.
     */
    SHOW(SK.SHOW),

    /**
     * DESCRIBE operation for showing table structure.
     */
    DESCRIBE(SK.DESCRIBE),

    /**
     * USE operation for selecting a database.
     */
    USE(SK.USE),

    /**
     * RENAME operation for renaming database objects.
     */
    RENAME(SK.RENAME),

    /**
     * BEGIN_TRANSACTION operation for starting a transaction.
     */
    BEGIN_TRANSACTION(SK.BEGIN_TRANSACTION),

    /**
     * COMMIT operation for committing a transaction.
     */
    COMMIT(SK.COMMIT),

    /**
     * ROLLBACK operation for rolling back a transaction.
     */
    ROLLBACK(SK.ROLLBACK),

    /**
     * CALL operation for executing stored procedures.
     */
    CALL("CALL"),

    /**
     * UNKNOWN operation for unrecognized SQL commands.
     */
    UNKNOWN("UNKNOWN");

    private final String sqlText;

    SQLOperation(final String name) {
        this.sqlText = name;
    }

    private static final Map<String, SQLOperation> operationMap = new HashMap<>();

    static {
        final SQLOperation[] values = SQLOperation.values();

        for (final SQLOperation value : values) {
            operationMap.put(value.sqlText, value);
            operationMap.put(value.name(), value);
        }
    }

    /**
     * Retrieves the SQLOperation enum value corresponding to the given operation name.
     * The lookup is case-sensitive and matches against both the SQL text representation
     * and the enum constant name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLOperation selectOp = SQLOperation.of("SELECT");      // returns SELECT
     * SQLOperation insertOp = SQLOperation.of("INSERT");      // returns INSERT
     * SQLOperation mergeOp = SQLOperation.of("MERGE");        // returns MERGE
     * SQLOperation unknownOp = SQLOperation.of("TRUNCATE");   // returns null (not supported)
     * }</pre>
     *
     * @param name the SQL operation name to look up (case-sensitive)
     * @return the corresponding SQLOperation enum value, or {@code null} if no matching operation is found
     */
    public static SQLOperation of(final String name) {
        return operationMap.get(name);
    }

    /**
     * Returns the SQL text representation of this operation.
     *
     * <p>This method returns the canonical SQL keyword associated with this operation.
     * For standard SQL operations, this will be the standard SQL keyword (e.g., "SELECT", "INSERT").
     * For composite operations like BEGIN_TRANSACTION, it returns the full command text
     * (e.g., "BEGIN TRANSACTION").</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLOperation op = SQLOperation.SELECT;
     * String sqlKeyword = op.sqlText();   // Returns "SELECT"
     *
     * SQLOperation txOp = SQLOperation.BEGIN_TRANSACTION;
     * String txText = txOp.sqlText();   // Returns "BEGIN TRANSACTION"
     * }</pre>
     *
     * @return the SQL keyword string representation of this operation, never {@code null}
     */
    public String sqlText() {
        return sqlText;
    }

    /**
     * Returns the string representation of this SQL operation.
     * This method returns the same value as {@link #sqlText()}.
     *
     * @return the operation name as a string
     */
    @Override
    public String toString() {
        return sqlText;
    }
}
