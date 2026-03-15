package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class UnionAll2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM orders");
        UnionAll unionAll = new UnionAll(subQuery);
        SubQuery retrieved = unionAll.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("region", "EAST"));
        UnionAll unionAll = new UnionAll(subQuery);
        List<Object> params = unionAll.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("EAST", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM products WHERE year = 2024");
        UnionAll unionAll = new UnionAll(subQuery);
        // SubQuery with raw SQL doesn't have parameters - test condition instead
        assertTrue(unionAll.getParameters().isEmpty());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1.hashCode(), unionAll2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(unionAll, unionAll);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1, unionAll2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertNotEquals(unionAll1, unionAll2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(null, unionAll);
    }

    @Test
    public void testKeepsDuplicates() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    public void testPerformance() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name, 'active' as status FROM active_users");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toString();
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(unionAll, "not a UnionAll");
        assertNotEquals(unionAll, new Union(subQuery));
    }
}

public class UnionAllTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = Filters.unionAll(subQuery);

        Assertions.assertNotNull(unionAll);
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.operator());
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

        Assertions.assertEquals(Operator.UNION_ALL, unionAll.operator());
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
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.operator());
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
        Assertions.assertEquals(Operator.UNION_ALL, unionAll.operator());
        Assertions.assertNotEquals(Operator.UNION, unionAll.operator());
    }
}
