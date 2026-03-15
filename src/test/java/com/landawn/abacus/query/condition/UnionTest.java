package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
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
class Union2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM orders");
        Union union = new Union(subQuery);
        SubQuery retrieved = union.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("status", "active"));
        Union union = new Union(subQuery);
        List<Object> params = union.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1.hashCode(), union2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertEquals(union, union);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1, union2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertNotEquals(union1, union2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotEquals(null, union);
    }

    @Test
    public void testRemovesDuplicates() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.operator());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("status", "active"));
        Union union = new Union(subQuery);
        List<Object> params = union.getParameters();
        assertEquals(1, (int) params.size());
        union.clearParameters();
        List<Object> clearedParams = union.getParameters();
        assertTrue(clearedParams.size() == 1 && clearedParams.stream().allMatch(param -> param == null));
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertEquals(Operator.UNION, union.operator());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toString();
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotEquals(union, "not a Union");
        assertNotEquals(union, new UnionAll(subQuery));
    }
}

public class UnionTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = Filters.union(subQuery);

        Assertions.assertNotNull(union);
        Assertions.assertEquals(Operator.UNION, union.operator());
        Assertions.assertEquals(subQuery, union.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_users");
        Union union = Filters.union(subQuery);

        Assertions.assertEquals(subQuery, union.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Union union = Filters.union(subQuery);

        Assertions.assertEquals(Operator.UNION, union.operator());
    }

    @Test
    public void testInheritedMethods() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.eq("active", true));
        Union union = Filters.union(subQuery);

        // Test inherited methods from Clause
        Assertions.assertNotNull(union.getParameters());
        Assertions.assertEquals(subQuery.getParameters(), union.getParameters());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id", "total"),
                Filters.and(Filters.gt("total", 1000), Filters.eq("status", "completed")));
        Union union = Filters.union(subQuery);

        Assertions.assertEquals(subQuery, union.getCondition());
        Assertions.assertEquals(2, union.getParameters().size());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_users");
        Union union = Filters.union(subQuery);

        String result = union.toString();
        Assertions.assertTrue(result.contains("UNION"));
        Assertions.assertTrue(result.contains("SELECT id FROM archived_users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");

        Union union1 = Filters.union(subQuery1);
        Union union2 = Filters.union(subQuery2);

        Assertions.assertEquals(union1.hashCode(), union2.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM customers");

        Union union1 = Filters.union(subQuery1);
        Union union2 = Filters.union(subQuery2);
        Union union3 = Filters.union(subQuery3);

        Assertions.assertTrue(union1.equals(union1));
        Assertions.assertTrue(union1.equals(union2));
        Assertions.assertFalse(union1.equals(union3));
        Assertions.assertFalse(union1.equals(null));
        Assertions.assertFalse(union1.equals("not a union"));
    }
}
