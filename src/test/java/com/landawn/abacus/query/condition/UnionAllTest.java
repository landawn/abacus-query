package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Strings;

public class UnionAllTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertNotNull(unionAll);
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
        Assertions.assertEquals(subQuery, unionAll.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertEquals(subQuery, unionAll.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM test");
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id", "customer_id", "total"),
                Filters.and(Filters.eq("region", "WEST"), Filters.gt("date", "2024-01-01")));
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertEquals(subQuery, unionAll.getCondition());
        Assertions.assertEquals(2, unionAll.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("sales", Arrays.asList("product_id", "quantity"), Filters.eq("store_id", 42));
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertEquals(subQuery.getParameters(), unionAll.getParameters());
        Assertions.assertEquals(1, unionAll.getParameters().size());
        Assertions.assertEquals(42, unionAll.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM backup_data");
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertTrue(unionAll.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("logs", Arrays.asList("timestamp", "message"), Filters.between("timestamp", "2024-01-01", "2024-12-31"));
        UnionAll unionAll = Filters.unionAll(subQuery);

        unionAll.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM historical_data");
        UnionAll unionAll = Filters.unionAll(subQuery);

        String result = unionAll.toString();
        Assertions.assertTrue(result.contains("UNION ALL"));
        Assertions.assertTrue(result.contains("SELECT * FROM historical_data"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("transactions", Arrays.asList("id", "amount"), Filters.eq("status", "completed"));
        UnionAll original = Filters.unionAll(subQuery);

        UnionAll copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT * FROM table2");

        UnionAll unionAll1 = Filters.unionAll(subQuery1);
        UnionAll unionAll2 = Filters.unionAll(subQuery2);
        UnionAll unionAll3 = Filters.unionAll(subQuery3);

        Assertions.assertEquals(unionAll1.hashCode(), unionAll2.hashCode());
        Assertions.assertNotEquals(unionAll1.hashCode(), unionAll3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT * FROM table2");

        UnionAll unionAll1 = Filters.unionAll(subQuery1);
        UnionAll unionAll2 = Filters.unionAll(subQuery2);
        UnionAll unionAll3 = Filters.unionAll(subQuery3);

        Assertions.assertTrue(unionAll1.equals(unionAll1));
        Assertions.assertTrue(unionAll1.equals(unionAll2));
        Assertions.assertFalse(unionAll1.equals(unionAll3));
        Assertions.assertFalse(unionAll1.equals(null));
        Assertions.assertFalse(unionAll1.equals("not a UnionAll"));
    }

    @Test
    public void testPracticalExample1() {
        // Combine current and archived transactions
        SubQuery currentTransactions = Filters.subQuery("SELECT * FROM transactions WHERE year = 2024");
        SubQuery archivedTransactions = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = Filters.unionAll(archivedTransactions);

        // Would be used like:
        // SELECT * FROM transactions WHERE year = 2024
        // UNION ALL
        // SELECT * FROM archived_transactions WHERE year = 2024
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testPracticalExample2() {
        // Combine active and inactive users with status
        SubQuery activeUsers = Filters.subQuery("active_users", Arrays.asList("id", "name"), Strings.EMPTY);
        SubQuery inactiveUsers = Filters.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll allUsers = Filters.unionAll(inactiveUsers);

        Assertions.assertEquals(inactiveUsers, allUsers.getCondition());
    }

    @Test
    public void testPracticalExample3() {
        // Combine orders from multiple regions
        SubQuery eastRegion = Filters.subQuery("orders", Arrays.asList("*"), Filters.eq("region", "EAST"));
        SubQuery westRegion = Filters.subQuery("orders", Arrays.asList("*"), Filters.eq("region", "WEST"));
        UnionAll allOrders = Filters.unionAll(westRegion);

        // Keeps all orders, even duplicates
        Assertions.assertEquals(1, allOrders.getParameters().size());
        Assertions.assertEquals("WEST", allOrders.getParameters().get(0));
    }

    @Test
    public void testDifferenceFromUnion() {
        // UNION ALL keeps duplicates, unlike UNION
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders WHERE amount > 1000");
        UnionAll unionAll = Filters.unionAll(subQuery);

        // The operator should be UNION_ALL, not UNION
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.getOperator());
        Assertions.assertNotEquals(Operator.UNION, unionAll.getOperator());
    }
}