/*
 * Copyright (c) 2025, Haiyang Li.
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SqlOperation2025Test extends TestBase {

    @Test
    public void testOf_ValidOperations() {
        assertEquals(SqlOperation.SELECT, SqlOperation.of("SELECT"));
        assertEquals(SqlOperation.INSERT, SqlOperation.of("INSERT"));
        assertEquals(SqlOperation.UPDATE, SqlOperation.of("UPDATE"));
        assertEquals(SqlOperation.DELETE, SqlOperation.of("DELETE"));
        assertEquals(SqlOperation.MERGE, SqlOperation.of("MERGE"));
        assertEquals(SqlOperation.CREATE, SqlOperation.of("CREATE"));
        assertEquals(SqlOperation.DROP, SqlOperation.of("DROP"));
        assertEquals(SqlOperation.ALTER, SqlOperation.of("ALTER"));
        assertEquals(SqlOperation.SHOW, SqlOperation.of("SHOW"));
        assertEquals(SqlOperation.DESCRIBE, SqlOperation.of("DESCRIBE"));
        assertEquals(SqlOperation.USE, SqlOperation.of("USE"));
        assertEquals(SqlOperation.RENAME, SqlOperation.of("RENAME"));
        assertEquals(SqlOperation.BEGIN_TRANSACTION, SqlOperation.of("BEGIN TRANSACTION"));
        assertEquals(SqlOperation.BEGIN_TRANSACTION, SqlOperation.of("BEGIN_TRANSACTION"));
        assertEquals(SqlOperation.COMMIT, SqlOperation.of("COMMIT"));
        assertEquals(SqlOperation.ROLLBACK, SqlOperation.of("ROLLBACK"));
        assertEquals(SqlOperation.CALL, SqlOperation.of("CALL"));
        assertEquals(SqlOperation.UNKNOWN, SqlOperation.of("UNKNOWN"));
    }

    @Test
    public void testOf_InvalidOperation() {
        assertNull(SqlOperation.of("INVALID"));
        assertNull(SqlOperation.of("TRUNCATE"));
        assertNull(SqlOperation.of(""));
        assertEquals(SqlOperation.SELECT, SqlOperation.of("select"));
        assertEquals(SqlOperation.SELECT, SqlOperation.of("Select"));
    }

    @Test
    public void testOf_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.of(null));
    }

    @Test
    public void testSqlText() {
        assertEquals("SELECT", SqlOperation.SELECT.sqlToken());
        assertEquals("INSERT", SqlOperation.INSERT.sqlToken());
        assertEquals("UPDATE", SqlOperation.UPDATE.sqlToken());
        assertEquals("DELETE", SqlOperation.DELETE.sqlToken());
        assertEquals("MERGE", SqlOperation.MERGE.sqlToken());
        assertEquals("CREATE", SqlOperation.CREATE.sqlToken());
        assertEquals("DROP", SqlOperation.DROP.sqlToken());
        assertEquals("ALTER", SqlOperation.ALTER.sqlToken());
        assertEquals("SHOW", SqlOperation.SHOW.sqlToken());
        assertEquals("DESCRIBE", SqlOperation.DESCRIBE.sqlToken());
        assertEquals("USE", SqlOperation.USE.sqlToken());
        assertEquals("RENAME", SqlOperation.RENAME.sqlToken());
        assertEquals("BEGIN TRANSACTION", SqlOperation.BEGIN_TRANSACTION.sqlToken());
        assertEquals("COMMIT", SqlOperation.COMMIT.sqlToken());
        assertEquals("ROLLBACK", SqlOperation.ROLLBACK.sqlToken());
        assertEquals("CALL", SqlOperation.CALL.sqlToken());
        assertEquals("UNKNOWN", SqlOperation.UNKNOWN.sqlToken());
    }

    @Test
    public void testToString() {
        assertEquals("SELECT", SqlOperation.SELECT.toString());
        assertEquals("INSERT", SqlOperation.INSERT.toString());
        assertEquals("UPDATE", SqlOperation.UPDATE.toString());
        assertEquals("DELETE", SqlOperation.DELETE.toString());
        assertEquals("MERGE", SqlOperation.MERGE.toString());
        assertEquals("CREATE", SqlOperation.CREATE.toString());
        assertEquals("DROP", SqlOperation.DROP.toString());
        assertEquals("ALTER", SqlOperation.ALTER.toString());
        assertEquals("SHOW", SqlOperation.SHOW.toString());
        assertEquals("DESCRIBE", SqlOperation.DESCRIBE.sqlToken());
        assertEquals("USE", SqlOperation.USE.toString());
        assertEquals("RENAME", SqlOperation.RENAME.toString());
        assertEquals("BEGIN TRANSACTION", SqlOperation.BEGIN_TRANSACTION.toString());
        assertEquals("COMMIT", SqlOperation.COMMIT.toString());
        assertEquals("ROLLBACK", SqlOperation.ROLLBACK.toString());
        assertEquals("CALL", SqlOperation.CALL.toString());
        assertEquals("UNKNOWN", SqlOperation.UNKNOWN.toString());
    }

    @Test
    public void testValues() {
        SqlOperation[] values = SqlOperation.values();
        assertNotNull(values);
        assertEquals(17, values.length);
    }

    @Test
    public void testValueOf() {
        assertEquals(SqlOperation.SELECT, SqlOperation.valueOf("SELECT"));
        assertEquals(SqlOperation.INSERT, SqlOperation.valueOf("INSERT"));
        assertEquals(SqlOperation.UPDATE, SqlOperation.valueOf("UPDATE"));
        assertEquals(SqlOperation.DELETE, SqlOperation.valueOf("DELETE"));
        assertEquals(SqlOperation.UNKNOWN, SqlOperation.valueOf("UNKNOWN"));
    }

    @Test
    public void testToStringMatchesSqlText() {
        for (SqlOperation op : SqlOperation.values()) {
            assertEquals(op.sqlToken(), op.toString());
        }
    }
}
