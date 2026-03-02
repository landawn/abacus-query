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
public class SQLOperation2025Test extends TestBase {

    @Test
    public void testOf_ValidOperations() {
        assertEquals(SQLOperation.SELECT, SQLOperation.of("SELECT"));
        assertEquals(SQLOperation.INSERT, SQLOperation.of("INSERT"));
        assertEquals(SQLOperation.UPDATE, SQLOperation.of("UPDATE"));
        assertEquals(SQLOperation.DELETE, SQLOperation.of("DELETE"));
        assertEquals(SQLOperation.MERGE, SQLOperation.of("MERGE"));
        assertEquals(SQLOperation.CREATE, SQLOperation.of("CREATE"));
        assertEquals(SQLOperation.DROP, SQLOperation.of("DROP"));
        assertEquals(SQLOperation.ALTER, SQLOperation.of("ALTER"));
        assertEquals(SQLOperation.SHOW, SQLOperation.of("SHOW"));
        assertEquals(SQLOperation.DESCRIBE, SQLOperation.of("DESCRIBE"));
        assertEquals(SQLOperation.USE, SQLOperation.of("USE"));
        assertEquals(SQLOperation.RENAME, SQLOperation.of("RENAME"));
        assertEquals(SQLOperation.BEGIN_TRANSACTION, SQLOperation.of("BEGIN TRANSACTION"));
        assertEquals(SQLOperation.BEGIN_TRANSACTION, SQLOperation.of("BEGIN_TRANSACTION"));
        assertEquals(SQLOperation.COMMIT, SQLOperation.of("COMMIT"));
        assertEquals(SQLOperation.ROLLBACK, SQLOperation.of("ROLLBACK"));
        assertEquals(SQLOperation.CALL, SQLOperation.of("CALL"));
        assertEquals(SQLOperation.UNKNOWN, SQLOperation.of("UNKNOWN"));
    }

    @Test
    public void testOf_InvalidOperation() {
        assertNull(SQLOperation.of("INVALID"));
        assertNull(SQLOperation.of("TRUNCATE"));
        assertNull(SQLOperation.of(""));
        assertEquals(SQLOperation.SELECT, SQLOperation.of("select"));
        assertEquals(SQLOperation.SELECT, SQLOperation.of("Select"));
    }

    @Test
    public void testOf_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> SQLOperation.of(null));
    }

    @Test
    public void testSqlText() {
        assertEquals("SELECT", SQLOperation.SELECT.sqlToken());
        assertEquals("INSERT", SQLOperation.INSERT.sqlToken());
        assertEquals("UPDATE", SQLOperation.UPDATE.sqlToken());
        assertEquals("DELETE", SQLOperation.DELETE.sqlToken());
        assertEquals("MERGE", SQLOperation.MERGE.sqlToken());
        assertEquals("CREATE", SQLOperation.CREATE.sqlToken());
        assertEquals("DROP", SQLOperation.DROP.sqlToken());
        assertEquals("ALTER", SQLOperation.ALTER.sqlToken());
        assertEquals("SHOW", SQLOperation.SHOW.sqlToken());
        assertEquals("DESCRIBE", SQLOperation.DESCRIBE.sqlToken());
        assertEquals("USE", SQLOperation.USE.sqlToken());
        assertEquals("RENAME", SQLOperation.RENAME.sqlToken());
        assertEquals("BEGIN TRANSACTION", SQLOperation.BEGIN_TRANSACTION.sqlToken());
        assertEquals("COMMIT", SQLOperation.COMMIT.sqlToken());
        assertEquals("ROLLBACK", SQLOperation.ROLLBACK.sqlToken());
        assertEquals("CALL", SQLOperation.CALL.sqlToken());
        assertEquals("UNKNOWN", SQLOperation.UNKNOWN.sqlToken());
    }

    @Test
    public void testToString() {
        assertEquals("SELECT", SQLOperation.SELECT.toString());
        assertEquals("INSERT", SQLOperation.INSERT.toString());
        assertEquals("UPDATE", SQLOperation.UPDATE.toString());
        assertEquals("DELETE", SQLOperation.DELETE.toString());
        assertEquals("MERGE", SQLOperation.MERGE.toString());
        assertEquals("CREATE", SQLOperation.CREATE.toString());
        assertEquals("DROP", SQLOperation.DROP.toString());
        assertEquals("ALTER", SQLOperation.ALTER.toString());
        assertEquals("SHOW", SQLOperation.SHOW.toString());
        assertEquals("DESCRIBE", SQLOperation.DESCRIBE.sqlToken());
        assertEquals("USE", SQLOperation.USE.toString());
        assertEquals("RENAME", SQLOperation.RENAME.toString());
        assertEquals("BEGIN TRANSACTION", SQLOperation.BEGIN_TRANSACTION.toString());
        assertEquals("COMMIT", SQLOperation.COMMIT.toString());
        assertEquals("ROLLBACK", SQLOperation.ROLLBACK.toString());
        assertEquals("CALL", SQLOperation.CALL.toString());
        assertEquals("UNKNOWN", SQLOperation.UNKNOWN.toString());
    }

    @Test
    public void testValues() {
        SQLOperation[] values = SQLOperation.values();
        assertNotNull(values);
        assertEquals(17, values.length);
    }

    @Test
    public void testValueOf() {
        assertEquals(SQLOperation.SELECT, SQLOperation.valueOf("SELECT"));
        assertEquals(SQLOperation.INSERT, SQLOperation.valueOf("INSERT"));
        assertEquals(SQLOperation.UPDATE, SQLOperation.valueOf("UPDATE"));
        assertEquals(SQLOperation.DELETE, SQLOperation.valueOf("DELETE"));
        assertEquals(SQLOperation.UNKNOWN, SQLOperation.valueOf("UNKNOWN"));
    }

    @Test
    public void testToStringMatchesSqlText() {
        for (SQLOperation op : SQLOperation.values()) {
            assertEquals(op.sqlToken(), op.toString());
        }
    }
}
