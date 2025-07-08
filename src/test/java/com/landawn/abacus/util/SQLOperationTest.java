package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SQLOperationTest extends TestBase {

    @Test
    public void testGetOperation() {
        // Test all valid operations
        assertEquals(SQLOperation.SELECT, SQLOperation.getOperation("SELECT"));
        assertEquals(SQLOperation.INSERT, SQLOperation.getOperation("INSERT"));
        assertEquals(SQLOperation.UPDATE, SQLOperation.getOperation("UPDATE"));
        assertEquals(SQLOperation.DELETE, SQLOperation.getOperation("DELETE"));
        assertEquals(SQLOperation.MERGE, SQLOperation.getOperation("MERGE"));
        assertEquals(SQLOperation.CREATE, SQLOperation.getOperation("CREATE"));
        assertEquals(SQLOperation.DROP, SQLOperation.getOperation("DROP"));
        assertEquals(SQLOperation.ALTER, SQLOperation.getOperation("ALTER"));
        assertEquals(SQLOperation.SHOW, SQLOperation.getOperation("SHOW"));
        assertEquals(SQLOperation.DESCRIBE, SQLOperation.getOperation("DESCRIBE"));
        assertEquals(SQLOperation.USE, SQLOperation.getOperation("USE"));
        assertEquals(SQLOperation.RENAME, SQLOperation.getOperation("RENAME"));
        assertEquals(SQLOperation.BEGIN_TRANSACTION, SQLOperation.getOperation("BEGIN_TRANSACTION"));
        assertEquals(SQLOperation.COMMIT, SQLOperation.getOperation("COMMIT"));
        assertEquals(SQLOperation.ROLLBACK, SQLOperation.getOperation("ROLLBACK"));
        assertEquals(SQLOperation.CALL, SQLOperation.getOperation("CALL"));
        assertEquals(SQLOperation.UNKNOWN, SQLOperation.getOperation("UNKNOWN"));
        
        // Test case sensitivity (should be case-sensitive)
        assertNull(SQLOperation.getOperation("select"));
        assertNull(SQLOperation.getOperation("Select"));
        assertNull(SQLOperation.getOperation("SELECT "));
        
        // Test non-existent operations
        assertNull(SQLOperation.getOperation("TRUNCATE"));
        assertNull(SQLOperation.getOperation("EXPLAIN"));
        assertNull(SQLOperation.getOperation(""));
        assertNull(SQLOperation.getOperation(null));
    }

    @Test
    public void testGetName() {
        // Test getName() for all operations
        assertEquals("SELECT", SQLOperation.SELECT.getName());
        assertEquals("INSERT", SQLOperation.INSERT.getName());
        assertEquals("UPDATE", SQLOperation.UPDATE.getName());
        assertEquals("DELETE", SQLOperation.DELETE.getName());
        assertEquals("MERGE", SQLOperation.MERGE.getName());
        assertEquals("CREATE", SQLOperation.CREATE.getName());
        assertEquals("DROP", SQLOperation.DROP.getName());
        assertEquals("ALTER", SQLOperation.ALTER.getName());
        assertEquals("SHOW", SQLOperation.SHOW.getName());
        assertEquals("DESCRIBE", SQLOperation.DESCRIBE.getName());
        assertEquals("USE", SQLOperation.USE.getName());
        assertEquals("RENAME", SQLOperation.RENAME.getName());
        assertEquals("BEGIN_TRANSACTION", SQLOperation.BEGIN_TRANSACTION.getName());
        assertEquals("COMMIT", SQLOperation.COMMIT.getName());
        assertEquals("ROLLBACK", SQLOperation.ROLLBACK.getName());
        assertEquals("CALL", SQLOperation.CALL.getName());
        assertEquals("UNKNOWN", SQLOperation.UNKNOWN.getName());
    }

    @Test
    public void testToString() {
        // Test toString() returns the same as getName()
        for (SQLOperation op : SQLOperation.values()) {
            assertEquals(op.getName(), op.toString());
        }
        
        // Test specific cases
        assertEquals("SELECT", SQLOperation.SELECT.toString());
        assertEquals("INSERT", SQLOperation.INSERT.toString());
        assertEquals("UPDATE", SQLOperation.UPDATE.toString());
        assertEquals("DELETE", SQLOperation.DELETE.toString());
    }

    @Test
    public void testEnumValues() {
        // Test that we have all expected values
        SQLOperation[] values = SQLOperation.values();
        assertEquals(17, values.length);
        
        // Verify all operations are present
        boolean hasSelect = false, hasInsert = false, hasUpdate = false, hasDelete = false;
        boolean hasMerge = false, hasCreate = false, hasDrop = false, hasAlter = false;
        boolean hasShow = false, hasDescribe = false, hasUse = false, hasRename = false;
        boolean hasBeginTransaction = false, hasCommit = false, hasRollback = false;
        boolean hasCall = false, hasUnknown = false;
        
        for (SQLOperation op : values) {
            switch (op) {
                case SELECT: hasSelect = true; break;
                case INSERT: hasInsert = true; break;
                case UPDATE: hasUpdate = true; break;
                case DELETE: hasDelete = true; break;
                case MERGE: hasMerge = true; break;
                case CREATE: hasCreate = true; break;
                case DROP: hasDrop = true; break;
                case ALTER: hasAlter = true; break;
                case SHOW: hasShow = true; break;
                case DESCRIBE: hasDescribe = true; break;
                case USE: hasUse = true; break;
                case RENAME: hasRename = true; break;
                case BEGIN_TRANSACTION: hasBeginTransaction = true; break;
                case COMMIT: hasCommit = true; break;
                case ROLLBACK: hasRollback = true; break;
                case CALL: hasCall = true; break;
                case UNKNOWN: hasUnknown = true; break;
            }
        }
        
        assertTrue(hasSelect);
        assertTrue(hasInsert);
        assertTrue(hasUpdate);
        assertTrue(hasDelete);
        assertTrue(hasMerge);
        assertTrue(hasCreate);
        assertTrue(hasDrop);
        assertTrue(hasAlter);
        assertTrue(hasShow);
        assertTrue(hasDescribe);
        assertTrue(hasUse);
        assertTrue(hasRename);
        assertTrue(hasBeginTransaction);
        assertTrue(hasCommit);
        assertTrue(hasRollback);
        assertTrue(hasCall);
        assertTrue(hasUnknown);
    }

    @Test
    public void testValueOf() {
        // Test valueOf for all valid enum names
        assertEquals(SQLOperation.SELECT, SQLOperation.valueOf("SELECT"));
        assertEquals(SQLOperation.INSERT, SQLOperation.valueOf("INSERT"));
        assertEquals(SQLOperation.UPDATE, SQLOperation.valueOf("UPDATE"));
        assertEquals(SQLOperation.DELETE, SQLOperation.valueOf("DELETE"));
        assertEquals(SQLOperation.MERGE, SQLOperation.valueOf("MERGE"));
        assertEquals(SQLOperation.CREATE, SQLOperation.valueOf("CREATE"));
        assertEquals(SQLOperation.DROP, SQLOperation.valueOf("DROP"));
        assertEquals(SQLOperation.ALTER, SQLOperation.valueOf("ALTER"));
        assertEquals(SQLOperation.SHOW, SQLOperation.valueOf("SHOW"));
        assertEquals(SQLOperation.DESCRIBE, SQLOperation.valueOf("DESCRIBE"));
        assertEquals(SQLOperation.USE, SQLOperation.valueOf("USE"));
        assertEquals(SQLOperation.RENAME, SQLOperation.valueOf("RENAME"));
        assertEquals(SQLOperation.BEGIN_TRANSACTION, SQLOperation.valueOf("BEGIN_TRANSACTION"));
        assertEquals(SQLOperation.COMMIT, SQLOperation.valueOf("COMMIT"));
        assertEquals(SQLOperation.ROLLBACK, SQLOperation.valueOf("ROLLBACK"));
        assertEquals(SQLOperation.CALL, SQLOperation.valueOf("CALL"));
        assertEquals(SQLOperation.UNKNOWN, SQLOperation.valueOf("UNKNOWN"));
        
        // Test invalid enum name
        assertThrows(IllegalArgumentException.class, () -> SQLOperation.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> SQLOperation.valueOf("select")); // Case sensitive
    }

    @Test
    public void testOrdinal() {
        // Test ordinal values are consistent
        assertEquals(0, SQLOperation.SELECT.ordinal());
        assertEquals(1, SQLOperation.INSERT.ordinal());
        assertEquals(2, SQLOperation.UPDATE.ordinal());
        assertEquals(3, SQLOperation.DELETE.ordinal());
        assertEquals(4, SQLOperation.MERGE.ordinal());
        assertEquals(5, SQLOperation.CREATE.ordinal());
        assertEquals(6, SQLOperation.DROP.ordinal());
        assertEquals(7, SQLOperation.ALTER.ordinal());
        assertEquals(8, SQLOperation.SHOW.ordinal());
        assertEquals(9, SQLOperation.DESCRIBE.ordinal());
        assertEquals(10, SQLOperation.USE.ordinal());
        assertEquals(11, SQLOperation.RENAME.ordinal());
        assertEquals(12, SQLOperation.BEGIN_TRANSACTION.ordinal());
        assertEquals(13, SQLOperation.COMMIT.ordinal());
        assertEquals(14, SQLOperation.ROLLBACK.ordinal());
        assertEquals(15, SQLOperation.CALL.ordinal());
        assertEquals(16, SQLOperation.UNKNOWN.ordinal());
    }

    @Test
    public void testEnumComparison() {
        // Test that enum instances are singletons
        SQLOperation select1 = SQLOperation.SELECT;
        SQLOperation select2 = SQLOperation.SELECT;
        assertSame(select1, select2);
        
        // Test different operations are not the same
        assertNotSame(SQLOperation.SELECT, SQLOperation.INSERT);
        assertNotSame(SQLOperation.UPDATE, SQLOperation.DELETE);
    }

    @Test
    public void testName() {
        // Test name() method returns the enum constant name
        assertEquals("SELECT", SQLOperation.SELECT.name());
        assertEquals("INSERT", SQLOperation.INSERT.name());
        assertEquals("UPDATE", SQLOperation.UPDATE.name());
        assertEquals("DELETE", SQLOperation.DELETE.name());
        assertEquals("MERGE", SQLOperation.MERGE.name());
        assertEquals("CREATE", SQLOperation.CREATE.name());
        assertEquals("DROP", SQLOperation.DROP.name());
        assertEquals("ALTER", SQLOperation.ALTER.name());
        assertEquals("SHOW", SQLOperation.SHOW.name());
        assertEquals("DESCRIBE", SQLOperation.DESCRIBE.name());
        assertEquals("USE", SQLOperation.USE.name());
        assertEquals("RENAME", SQLOperation.RENAME.name());
        assertEquals("BEGIN_TRANSACTION", SQLOperation.BEGIN_TRANSACTION.name());
        assertEquals("COMMIT", SQLOperation.COMMIT.name());
        assertEquals("ROLLBACK", SQLOperation.ROLLBACK.name());
        assertEquals("CALL", SQLOperation.CALL.name());
        assertEquals("UNKNOWN", SQLOperation.UNKNOWN.name());
    }

    @Test
    public void testOperationMapConsistency() {
        // Test that all enum values can be retrieved by their name through getOperation
        for (SQLOperation op : SQLOperation.values()) {
            assertEquals(op, SQLOperation.getOperation(op.getName()));
        }
        
        // Test that getName() returns the expected value used in getOperation()
        assertEquals("SELECT", SQLOperation.SELECT.getName());
        assertEquals("INSERT", SQLOperation.INSERT.getName());
        assertEquals("UPDATE", SQLOperation.UPDATE.getName());
        assertEquals("DELETE", SQLOperation.DELETE.getName());
        assertEquals("MERGE", SQLOperation.MERGE.getName());
        assertEquals("CREATE", SQLOperation.CREATE.getName());
        assertEquals("DROP", SQLOperation.DROP.getName());
        assertEquals("ALTER", SQLOperation.ALTER.getName());
        assertEquals("SHOW", SQLOperation.SHOW.getName());
        assertEquals("DESCRIBE", SQLOperation.DESCRIBE.getName());
        assertEquals("USE", SQLOperation.USE.getName());
        assertEquals("RENAME", SQLOperation.RENAME.getName());
        assertEquals("BEGIN_TRANSACTION", SQLOperation.BEGIN_TRANSACTION.getName());
        assertEquals("COMMIT", SQLOperation.COMMIT.getName());
        assertEquals("ROLLBACK", SQLOperation.ROLLBACK.getName());
        assertEquals("CALL", SQLOperation.CALL.getName());
        assertEquals("UNKNOWN", SQLOperation.UNKNOWN.getName());
    }
}