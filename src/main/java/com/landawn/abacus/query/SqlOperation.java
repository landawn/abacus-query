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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.SK;

/**
 * Enumeration representing SQL operation types.
 * This enum provides a comprehensive list of SQL operations including DML, DDL, DCL, and TCL commands.
 * It's primarily used internally for SQL parsing and operation identification.
 * 
 * <p>Supported operations include:</p>
 * <ul>
 *   <li>DML (Data Manipulation Language): SELECT, INSERT, UPDATE, DELETE, MERGE</li>
 *   <li>DDL (Data Definition Language): CREATE, DROP, ALTER, RENAME</li>
 *   <li>Utility: USE</li>
 *   <li>TCL (Transaction Control Language): BEGIN_TRANSACTION, COMMIT, ROLLBACK</li>
 *   <li>Other: SHOW, DESCRIBE, CALL, UNKNOWN</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get operation from SQL statement
 * String sql = "SELECT * FROM users";
 * String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
 * SqlOperation op = SqlOperation.of(firstWord);
 *
 * if (op == SqlOperation.SELECT) {
 *     // Handle SELECT query
 * }
 * }</pre>
 * 
 * @see SqlParser
 */
@Internal
public enum SqlOperation {
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
    CALL(SK.CALL),

    /**
     * UNKNOWN operation for unrecognized SQL commands.
     */
    UNKNOWN("UNKNOWN");

    private final String sqlToken;

    SqlOperation(final String sqlToken) {
        this.sqlToken = sqlToken;
    }

    private static final Map<String, SqlOperation> operationMap;

    static {
        final SqlOperation[] values = SqlOperation.values();
        final Map<String, SqlOperation> tempMap = new HashMap<>();

        for (final SqlOperation value : values) {
            tempMap.put(value.sqlToken, value);
            tempMap.put(value.sqlToken.toLowerCase(Locale.ROOT), value);
            tempMap.put(value.name(), value);
            tempMap.put(value.name().toLowerCase(Locale.ROOT), value);
        }

        operationMap = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Retrieves the {@code SqlOperation} enum value corresponding to the given operation name.
     * The lookup is case-insensitive and matches against both the SQL token representation
     * and the enum constant name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlOperation selectOp = SqlOperation.of("SELECT");      // returns SELECT
     * SqlOperation insertOp = SqlOperation.of("INSERT");      // returns INSERT
     * SqlOperation mergeOp = SqlOperation.of("MERGE");        // returns MERGE
     * SqlOperation unknownOp = SqlOperation.of("TRUNCATE");   // returns null (not supported)
     * }</pre>
     *
     * @param name the SQL operation name to look up (case-insensitive)
     * @return the corresponding {@code SqlOperation} enum value, or {@code null} if no matching operation is found
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public static SqlOperation of(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("SqlOperation name cannot be null");
        }

        SqlOperation value = operationMap.get(name);

        if (value == null) {
            return operationMap.get(name.toLowerCase(Locale.ROOT));
        }

        return value;
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
     * SqlOperation op = SqlOperation.SELECT;
     * String sqlKeyword = op.sqlToken();   // Returns "SELECT"
     *
     * SqlOperation txOp = SqlOperation.BEGIN_TRANSACTION;
     * String txText = txOp.sqlToken();   // Returns "BEGIN TRANSACTION"
     * }</pre>
     *
     * @return the SQL keyword string representation of this operation, never {@code null}
     */
    public String sqlToken() {
        return sqlToken;
    }

    /**
     * Returns the string representation of this SQL operation.
     * This method returns the same value as {@link #sqlToken()}.
     *
     * @return the SQL keyword string representation of this operation
     */
    @Override
    public String toString() {
        return sqlToken;
    }
}
