package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.UnionAll;
import com.landawn.abacus.query.condition.Filters.CF;
import com.landawn.abacus.util.Strings;

public class UnionAllTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertNotNull(unionAll);
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
        Assertions.assertEquals(subQuery, unionAll.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = CF.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertEquals(subQuery, unionAll.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM test");
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = CF.subQuery("orders", Arrays.asList("id", "customer_id", "total"), CF.and(CF.eq("region", "WEST"), CF.gt("date", "2024-01-01")));
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertEquals(subQuery, unionAll.getCondition());
        Assertions.assertEquals(2, unionAll.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = CF.subQuery("sales", Arrays.asList("product_id", "quantity"), CF.eq("store_id", 42));
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertEquals(subQuery.getParameters(), unionAll.getParameters());
        Assertions.assertEquals(1, unionAll.getParameters().size());
        Assertions.assertEquals(42, unionAll.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM backup_data");
        UnionAll unionAll = CF.unionAll(subQuery);

        Assertions.assertTrue(unionAll.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = CF.subQuery("logs", Arrays.asList("timestamp", "message"), CF.between("timestamp", "2024-01-01", "2024-12-31"));
        UnionAll unionAll = CF.unionAll(subQuery);

        unionAll.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = CF.subQuery("SELECT * FROM historical_data");
        UnionAll unionAll = CF.unionAll(subQuery);

        String result = unionAll.toString();
        Assertions.assertTrue(result.contains("UNION ALL"));
        Assertions.assertTrue(result.contains("SELECT * FROM historical_data"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = CF.subQuery("transactions", Arrays.asList("id", "amount"), CF.eq("status", "completed"));
        UnionAll original = CF.unionAll(subQuery);

        UnionAll copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = CF.subQuery("SELECT * FROM table1");
        SubQuery subQuery2 = CF.subQuery("SELECT * FROM table1");
        SubQuery subQuery3 = CF.subQuery("SELECT * FROM table2");

        UnionAll unionAll1 = CF.unionAll(subQuery1);
        UnionAll unionAll2 = CF.unionAll(subQuery2);
        UnionAll unionAll3 = CF.unionAll(subQuery3);

        Assertions.assertEquals(unionAll1.hashCode(), unionAll2.hashCode());
        Assertions.assertNotEquals(unionAll1.hashCode(), unionAll3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = CF.subQuery("SELECT * FROM table1");
        SubQuery subQuery2 = CF.subQuery("SELECT * FROM table1");
        SubQuery subQuery3 = CF.subQuery("SELECT * FROM table2");

        UnionAll unionAll1 = CF.unionAll(subQuery1);
        UnionAll unionAll2 = CF.unionAll(subQuery2);
        UnionAll unionAll3 = CF.unionAll(subQuery3);

        Assertions.assertTrue(unionAll1.equals(unionAll1));
        Assertions.assertTrue(unionAll1.equals(unionAll2));
        Assertions.assertFalse(unionAll1.equals(unionAll3));
        Assertions.assertFalse(unionAll1.equals(null));
        Assertions.assertFalse(unionAll1.equals("not a UnionAll"));
    }

    @Test
    public void testPracticalExample1() {
        // Combine current and archived transactions
        SubQuery currentTransactions = CF.subQuery("SELECT * FROM transactions WHERE year = 2024");
        SubQuery archivedTransactions = CF.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = CF.unionAll(archivedTransactions);

        // Would be used like:
        // SELECT * FROM transactions WHERE year = 2024
        // UNION ALL
        // SELECT * FROM archived_transactions WHERE year = 2024
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testPracticalExample2() {
        // Combine active and inactive users with status
        SubQuery activeUsers = CF.subQuery("active_users", Arrays.asList("id", "name"), Strings.EMPTY);
        SubQuery inactiveUsers = CF.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll allUsers = CF.unionAll(inactiveUsers);

        Assertions.assertEquals(inactiveUsers, allUsers.getCondition());
    }

    @Test
    public void testPracticalExample3() {
        // Combine orders from multiple regions
        SubQuery eastRegion = CF.subQuery("orders", Arrays.asList("*"), CF.eq("region", "EAST"));
        SubQuery westRegion = CF.subQuery("orders", Arrays.asList("*"), CF.eq("region", "WEST"));
        UnionAll allOrders = CF.unionAll(westRegion);

        // Keeps all orders, even duplicates
        Assertions.assertEquals(1, allOrders.getParameters().size());
        Assertions.assertEquals("WEST", allOrders.getParameters().get(0));
    }

    @Test
    public void testDifferenceFromUnion() {
        // UNION ALL keeps duplicates, unlike UNION
        SubQuery subQuery = CF.subQuery("SELECT customer_id FROM orders WHERE amount > 1000");
        UnionAll unionAll = CF.unionAll(subQuery);

        // The operator should be UNION_ALL, not UNION
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
        Assertions.assertNotEquals(Operator.UNION, unionAll.getOperator());
    }
}