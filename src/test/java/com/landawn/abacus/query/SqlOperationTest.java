package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.SK;

@Tag("2025")
class SqlOperation2025Test extends TestBase {

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

class SqlOperationJavadocExamples extends TestBase {

    @Test
    public void testSqlOperation_classLevelExample() {
        String sql = "SELECT * FROM users";
        String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
        SqlOperation op = SqlOperation.of(firstWord);
        assertEquals(SqlOperation.SELECT, op);
    }

    @Test
    public void testSqlOperation_of() {
        SqlOperation selectOp = SqlOperation.of("SELECT");
        assertEquals(SqlOperation.SELECT, selectOp);
        SqlOperation insertOp = SqlOperation.of("INSERT");
        assertEquals(SqlOperation.INSERT, insertOp);
        SqlOperation mergeOp = SqlOperation.of("MERGE");
        assertEquals(SqlOperation.MERGE, mergeOp);
        SqlOperation unknownOp = SqlOperation.of("TRUNCATE");
        assertNull(unknownOp);
    }

    @Test
    public void testSqlOperation_sqlToken() {
        SqlOperation op = SqlOperation.SELECT;
        String sqlKeyword = op.sqlToken();
        assertEquals("SELECT", sqlKeyword);
        SqlOperation txOp = SqlOperation.BEGIN_TRANSACTION;
        String txText = txOp.sqlToken();
        assertEquals("BEGIN TRANSACTION", txText);
    }
}
