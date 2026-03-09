package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.SK;

public class SqlOperationTest extends TestBase {

    @Test
    public void testGetOperation() {
        // Test all valid operations
        assertEquals(SqlOperation.SELECT, SqlOperation.valueOf("SELECT"));
        assertEquals(SqlOperation.INSERT, SqlOperation.valueOf("INSERT"));
        assertEquals(SqlOperation.UPDATE, SqlOperation.valueOf("UPDATE"));
        assertEquals(SqlOperation.DELETE, SqlOperation.valueOf("DELETE"));
        assertEquals(SqlOperation.MERGE, SqlOperation.valueOf("MERGE"));
        assertEquals(SqlOperation.CREATE, SqlOperation.valueOf("CREATE"));
        assertEquals(SqlOperation.DROP, SqlOperation.valueOf("DROP"));
        assertEquals(SqlOperation.ALTER, SqlOperation.valueOf("ALTER"));
        assertEquals(SqlOperation.SHOW, SqlOperation.valueOf("SHOW"));
        assertEquals(SqlOperation.DESCRIBE, SqlOperation.valueOf("DESCRIBE"));
        assertEquals(SqlOperation.USE, SqlOperation.valueOf("USE"));
        assertEquals(SqlOperation.RENAME, SqlOperation.valueOf("RENAME"));
        assertEquals(SqlOperation.BEGIN_TRANSACTION, SqlOperation.valueOf("BEGIN_TRANSACTION"));
        assertEquals(SqlOperation.COMMIT, SqlOperation.valueOf("COMMIT"));
        assertEquals(SqlOperation.ROLLBACK, SqlOperation.valueOf("ROLLBACK"));
        assertEquals(SqlOperation.CALL, SqlOperation.valueOf("CALL"));
        assertEquals(SqlOperation.UNKNOWN, SqlOperation.valueOf("UNKNOWN"));

        // Test case sensitivity (should be case-sensitive)
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("select"));
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("Select"));
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("SELECT "));

        // Test non-existent operations
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("TRUNCATE"));
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("EXPLAIN"));
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf(""));
        assertThrows(NullPointerException.class, () -> SqlOperation.valueOf(null));
    }

    @Test
    public void testGetName() {
        // Test getName() for all operations
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
        assertEquals(SK.BEGIN_TRANSACTION, SqlOperation.BEGIN_TRANSACTION.sqlToken());
        assertEquals("COMMIT", SqlOperation.COMMIT.sqlToken());
        assertEquals("ROLLBACK", SqlOperation.ROLLBACK.sqlToken());
        assertEquals("CALL", SqlOperation.CALL.sqlToken());
        assertEquals("UNKNOWN", SqlOperation.UNKNOWN.sqlToken());
    }

    @Test
    public void testToString() {
        // Test toString() returns the same as getName()
        for (SqlOperation op : SqlOperation.values()) {
            assertEquals(op.sqlToken(), op.toString());
        }

        // Test specific cases
        assertEquals("SELECT", SqlOperation.SELECT.toString());
        assertEquals("INSERT", SqlOperation.INSERT.toString());
        assertEquals("UPDATE", SqlOperation.UPDATE.toString());
        assertEquals("DELETE", SqlOperation.DELETE.toString());
    }

    @Test
    public void testEnumValues() {
        // Test that we have all expected values
        SqlOperation[] values = SqlOperation.values();
        assertEquals(17, values.length);

        // Verify all operations are present
        boolean hasSelect = false, hasInsert = false, hasUpdate = false, hasDelete = false;
        boolean hasMerge = false, hasCreate = false, hasDrop = false, hasAlter = false;
        boolean hasShow = false, hasDescribe = false, hasUse = false, hasRename = false;
        boolean hasBeginTransaction = false, hasCommit = false, hasRollback = false;
        boolean hasCall = false, hasUnknown = false;

        for (SqlOperation op : values) {
            switch (op) {
                case SELECT:
                    hasSelect = true;
                    break;
                case INSERT:
                    hasInsert = true;
                    break;
                case UPDATE:
                    hasUpdate = true;
                    break;
                case DELETE:
                    hasDelete = true;
                    break;
                case MERGE:
                    hasMerge = true;
                    break;
                case CREATE:
                    hasCreate = true;
                    break;
                case DROP:
                    hasDrop = true;
                    break;
                case ALTER:
                    hasAlter = true;
                    break;
                case SHOW:
                    hasShow = true;
                    break;
                case DESCRIBE:
                    hasDescribe = true;
                    break;
                case USE:
                    hasUse = true;
                    break;
                case RENAME:
                    hasRename = true;
                    break;
                case BEGIN_TRANSACTION:
                    hasBeginTransaction = true;
                    break;
                case COMMIT:
                    hasCommit = true;
                    break;
                case ROLLBACK:
                    hasRollback = true;
                    break;
                case CALL:
                    hasCall = true;
                    break;
                case UNKNOWN:
                    hasUnknown = true;
                    break;
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
        assertEquals(SqlOperation.SELECT, SqlOperation.valueOf("SELECT"));
        assertEquals(SqlOperation.INSERT, SqlOperation.valueOf("INSERT"));
        assertEquals(SqlOperation.UPDATE, SqlOperation.valueOf("UPDATE"));
        assertEquals(SqlOperation.DELETE, SqlOperation.valueOf("DELETE"));
        assertEquals(SqlOperation.MERGE, SqlOperation.valueOf("MERGE"));
        assertEquals(SqlOperation.CREATE, SqlOperation.valueOf("CREATE"));
        assertEquals(SqlOperation.DROP, SqlOperation.valueOf("DROP"));
        assertEquals(SqlOperation.ALTER, SqlOperation.valueOf("ALTER"));
        assertEquals(SqlOperation.SHOW, SqlOperation.valueOf("SHOW"));
        assertEquals(SqlOperation.DESCRIBE, SqlOperation.valueOf("DESCRIBE"));
        assertEquals(SqlOperation.USE, SqlOperation.valueOf("USE"));
        assertEquals(SqlOperation.RENAME, SqlOperation.valueOf("RENAME"));
        assertEquals(SqlOperation.BEGIN_TRANSACTION, SqlOperation.valueOf("BEGIN_TRANSACTION"));
        assertEquals(SqlOperation.COMMIT, SqlOperation.valueOf("COMMIT"));
        assertEquals(SqlOperation.ROLLBACK, SqlOperation.valueOf("ROLLBACK"));
        assertEquals(SqlOperation.CALL, SqlOperation.valueOf("CALL"));
        assertEquals(SqlOperation.UNKNOWN, SqlOperation.valueOf("UNKNOWN"));

        // Test invalid enum name
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> SqlOperation.valueOf("select")); // Case sensitive
    }

    @Test
    public void testOrdinal() {
        // Test ordinal values are consistent
        assertEquals(0, SqlOperation.SELECT.ordinal());
        assertEquals(1, SqlOperation.INSERT.ordinal());
        assertEquals(2, SqlOperation.UPDATE.ordinal());
        assertEquals(3, SqlOperation.DELETE.ordinal());
        assertEquals(4, SqlOperation.MERGE.ordinal());
        assertEquals(5, SqlOperation.CREATE.ordinal());
        assertEquals(6, SqlOperation.DROP.ordinal());
        assertEquals(7, SqlOperation.ALTER.ordinal());
        assertEquals(8, SqlOperation.SHOW.ordinal());
        assertEquals(9, SqlOperation.DESCRIBE.ordinal());
        assertEquals(10, SqlOperation.USE.ordinal());
        assertEquals(11, SqlOperation.RENAME.ordinal());
        assertEquals(12, SqlOperation.BEGIN_TRANSACTION.ordinal());
        assertEquals(13, SqlOperation.COMMIT.ordinal());
        assertEquals(14, SqlOperation.ROLLBACK.ordinal());
        assertEquals(15, SqlOperation.CALL.ordinal());
        assertEquals(16, SqlOperation.UNKNOWN.ordinal());
    }

    @Test
    public void testEnumComparison() {
        // Test that enum instances are singletons
        SqlOperation select1 = SqlOperation.SELECT;
        SqlOperation select2 = SqlOperation.SELECT;
        assertSame(select1, select2);

        // Test different operations are not the same
        assertNotSame(SqlOperation.SELECT, SqlOperation.INSERT);
        assertNotSame(SqlOperation.UPDATE, SqlOperation.DELETE);
    }

    @Test
    public void testName() {
        // Test name() method returns the enum constant name
        assertEquals("SELECT", SqlOperation.SELECT.name());
        assertEquals("INSERT", SqlOperation.INSERT.name());
        assertEquals("UPDATE", SqlOperation.UPDATE.name());
        assertEquals("DELETE", SqlOperation.DELETE.name());
        assertEquals("MERGE", SqlOperation.MERGE.name());
        assertEquals("CREATE", SqlOperation.CREATE.name());
        assertEquals("DROP", SqlOperation.DROP.name());
        assertEquals("ALTER", SqlOperation.ALTER.name());
        assertEquals("SHOW", SqlOperation.SHOW.name());
        assertEquals("DESCRIBE", SqlOperation.DESCRIBE.name());
        assertEquals("USE", SqlOperation.USE.name());
        assertEquals("RENAME", SqlOperation.RENAME.name());
        assertEquals("BEGIN_TRANSACTION", SqlOperation.BEGIN_TRANSACTION.name());
        assertEquals("COMMIT", SqlOperation.COMMIT.name());
        assertEquals("ROLLBACK", SqlOperation.ROLLBACK.name());
        assertEquals("CALL", SqlOperation.CALL.name());
        assertEquals("UNKNOWN", SqlOperation.UNKNOWN.name());
    }

    @Test
    public void testOperationMapConsistency() {
        // Test that all enum values can be retrieved by their name through getOperation
        for (SqlOperation op : SqlOperation.values()) {
            assertEquals(op, SqlOperation.of(op.sqlToken()));
        }

        // Test that getName() returns the expected value used in getOperation()
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
        assertEquals(SK.BEGIN_TRANSACTION, SqlOperation.BEGIN_TRANSACTION.sqlToken());
        assertEquals("COMMIT", SqlOperation.COMMIT.sqlToken());
        assertEquals("ROLLBACK", SqlOperation.ROLLBACK.sqlToken());
        assertEquals("CALL", SqlOperation.CALL.sqlToken());
        assertEquals("UNKNOWN", SqlOperation.UNKNOWN.sqlToken());
    }
}