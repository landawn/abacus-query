package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class UnionTest extends TestBase {
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
        SubQuery retrieved = (SubQuery) union.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("status", "active"));
        Union union = new Union(subQuery);
        List<Object> params = union.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toSql(NamingPolicy.NO_CHANGE);
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

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = Filters.union(subQuery);

        Assertions.assertNotNull(union);
        Assertions.assertEquals(Operator.UNION, union.operator());
        Assertions.assertEquals(subQuery, union.condition());
    }

    @Test
    public void testInheritedMethods() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.eq("active", true));
        Union union = Filters.union(subQuery);

        // Test inherited methods from Clause
        Assertions.assertNotNull(union.parameters());
        Assertions.assertEquals(subQuery.parameters(), union.parameters());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id", "total"),
                Filters.and(Filters.gt("total", 1000), Filters.eq("status", "completed")));
        Union union = Filters.union(subQuery);

        Assertions.assertEquals(subQuery, union.condition());
        Assertions.assertEquals(2, union.parameters().size());
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
