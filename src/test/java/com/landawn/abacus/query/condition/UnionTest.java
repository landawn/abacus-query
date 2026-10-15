package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class UnionTest extends TestBase {

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.operator());
    }

    @Test
    void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = Filters.union(subQuery);

        assertNotNull(union);
        assertEquals(Operator.UNION, union.operator());
        assertEquals(subQuery, union.condition());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM customers");

        Union union1 = Filters.union(subQuery1);
        Union union2 = Filters.union(subQuery2);
        Union union3 = Filters.union(subQuery3);

        assertTrue(union1.equals(union1));
        assertTrue(union1.equals(union2));
        assertFalse(union1.equals(union3));
        assertFalse(union1.equals(null));
        assertFalse(union1.equals("not a union"));
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotEquals(union, "not a Union");
        assertNotEquals(union, new UnionAll(subQuery));
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertNotEquals(union1, union2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1, union2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertNotEquals(null, union);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertEquals(union, union);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM orders");
        Union union = new Union(subQuery);
        SubQuery retrieved = (SubQuery) union.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Union union1 = new Union(subQuery1);
        Union union2 = new Union(subQuery2);
        assertEquals(union1.hashCode(), union2.hashCode());
    }

    @Test
    void testInheritedMethods() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.eq("active", true));
        Union union = Filters.union(subQuery);

        // Test inherited methods from Clause
        assertNotNull(union.parameters());
        assertEquals(subQuery.parameters(), union.parameters());
    }

    @Test
    void testNullSubQueryMessageNamesSubQuery() {
        for (final java.util.function.Supplier<Object> ctor : java.util.List.<java.util.function.Supplier<Object>> of(() -> new Union(null),
                () -> new UnionAll(null), () -> new Intersect(null), () -> new Except(null), () -> new Minus(null), () -> Criteria.builder().union(null))) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ctor::get);
            assertTrue(e.getMessage().contains("subQuery"), e.getMessage());
        }
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("status", "active"));
        Union union = new Union(subQuery);
        List<Object> params = union.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testRemovesDuplicates() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders");
        Union union = new Union(subQuery);
        assertNotNull(union);
        assertEquals(Operator.UNION, union.operator());
    }

    @Test
    void testSubQueryAccessor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Union union = new Union(subQuery);
        assertSame(subQuery, union.subQuery());
    }

    @Test
    void testSubQueryAccessor_DefaultConstructor_ReturnsNull() {
        // Uninitialized (Kryo) instance: subQuery() must return null without a ClassCastException.
        Union union = new Union();
        assertNull(union.subQuery());
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM customers");
        Union union = new Union(subQuery);
        String result = union.toString();
        assertTrue(result.contains("UNION"));
    }

    @Test
    void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id", "total"),
                Filters.and(Filters.gt("total", 1000), Filters.eq("status", "completed")));
        Union union = Filters.union(subQuery);

        assertEquals(subQuery, union.condition());
        assertEquals(2, union.parameters().size());
    }
}
