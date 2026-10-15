package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

public class UnionAllTest extends TestBase {

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = Filters.unionAll(subQuery);

        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
        assertEquals(subQuery, unionAll.condition());
    }

    @Test
    void testDifferenceFromUnion() {
        // UNION ALL keeps duplicates, unlike UNION
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders WHERE amount > 1000");
        UnionAll unionAll = Filters.unionAll(subQuery);

        // The operator should be UNION_ALL, not UNION
        assertEquals(Operator.UNION_ALL, unionAll.operator());
        assertNotEquals(Operator.UNION, unionAll.operator());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT * FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT * FROM table2");

        UnionAll unionAll1 = Filters.unionAll(subQuery1);
        UnionAll unionAll2 = Filters.unionAll(subQuery2);
        UnionAll unionAll3 = Filters.unionAll(subQuery3);

        assertTrue(unionAll1.equals(unionAll1));
        assertTrue(unionAll1.equals(unionAll2));
        assertFalse(unionAll1.equals(unionAll3));
        assertFalse(unionAll1.equals(null));
        assertFalse(unionAll1.equals("not a UnionAll"));
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(unionAll, "not a UnionAll");
        assertNotEquals(unionAll, new Union(subQuery));
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertNotEquals(unionAll1, unionAll2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1, unionAll2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(null, unionAll);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(unionAll, unionAll);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM orders");
        UnionAll unionAll = new UnionAll(subQuery);
        SubQuery retrieved = (SubQuery) unionAll.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1.hashCode(), unionAll2.hashCode());
    }

    @Test
    void testKeepsDuplicates() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("region", "EAST"));
        UnionAll unionAll = new UnionAll(subQuery);
        List<Object> params = unionAll.parameters();
        assertEquals(1, params.size());
        assertEquals("EAST", params.get(0));
    }

    @Test
    void testParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM backup_data");
        UnionAll unionAll = Filters.unionAll(subQuery);

        assertTrue(unionAll.parameters().isEmpty());
    }

    @Test
    void testPerformance() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name, 'active' as status FROM active_users");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    void testPracticalExample1() {
        // Combine current and archived transactions
        SubQuery currentTransactions = Filters.subQuery("SELECT * FROM transactions WHERE year = 2024");
        SubQuery archivedTransactions = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = Filters.unionAll(archivedTransactions);

        // Would be used like:
        // SELECT * FROM transactions WHERE year = 2024
        // UNION ALL
        // SELECT * FROM archived_transactions WHERE year = 2024
        assertEquals(Operator.UNION_ALL, unionAll.operator());
    }

    @Test
    void testPracticalExample2() {
        // Combine active and inactive users with status
        SubQuery activeUsers = Filters.subQuery("active_users", Arrays.asList("id", "name"), Strings.EMPTY);
        SubQuery inactiveUsers = Filters.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll allUsers = Filters.unionAll(inactiveUsers);

        assertEquals(inactiveUsers, allUsers.condition());
    }

    @Test
    void testPracticalExample3() {
        // Combine orders from multiple regions
        SubQuery eastRegion = Filters.subQuery("orders", Arrays.asList("*"), Filters.eq("region", "EAST"));
        SubQuery westRegion = Filters.subQuery("orders", Arrays.asList("*"), Filters.eq("region", "WEST"));
        UnionAll allOrders = Filters.unionAll(westRegion);

        // Keeps all orders, even duplicates
        assertEquals(1, allOrders.parameters().size());
        assertEquals("WEST", allOrders.parameters().get(0));
    }

    @Test
    void testSubQueryAccessor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertSame(subQuery, unionAll.subQuery());
    }

    @Test
    void testSubQueryAccessor_DefaultConstructor_ReturnsNull() {
        // Uninitialized (Kryo) instance: subQuery() must return null without a ClassCastException.
        UnionAll unionAll = new UnionAll();
        assertNull(unionAll.subQuery());
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toString();
        assertTrue(result.contains("UNION"));
    }

    @Test
    void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id", "customer_id", "total"),
                Filters.and(Filters.eq("region", "WEST"), Filters.gt("date", "2024-01-01")));
        UnionAll unionAll = Filters.unionAll(subQuery);

        assertEquals(subQuery, unionAll.condition());
        assertEquals(2, unionAll.parameters().size());
    }
}
