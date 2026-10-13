package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class IntersectTest extends TestBase {

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    void testConstructorWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM users WHERE plan = 'premium' AND active = true");
        Intersect intersect = new Intersect(subQuery);

        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
        assertNotNull(intersect.condition());
    }

    @Test
    void testConstructorWithNullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Intersect(null);
        });
    }

    @Test
    void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'active'");
        Intersect intersect = new Intersect(subQuery);

        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
        assertEquals(subQuery, intersect.condition());
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM table2");

        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        Intersect intersect3 = new Intersect(subQuery3);

        assertEquals(intersect1, intersect1);
        assertEquals(intersect1, intersect2);
        assertNotEquals(intersect1, intersect3);
        assertNotEquals(intersect1, null);
        assertNotEquals(intersect1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(intersect, "not an Intersect");
        assertNotEquals(intersect, new Union(subQuery));
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertNotEquals(intersect1, intersect2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1, intersect2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(null, intersect);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(intersect, intersect);
    }

    @Test
    void testFindCommonElements() {
        SubQuery activeUsers = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > ?", Arrays.asList("2023-01-01"));
        Intersect intersect = new Intersect(activeUsers);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders");
        Intersect intersect = new Intersect(subQuery);
        SubQuery retrieved = (SubQuery) intersect.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1.hashCode(), intersect2.hashCode());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(subQuery);
        List<Object> params = intersect.parameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, params.size());
    }

    @Test
    void testParametersWithMultipleValues() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table WHERE value BETWEEN ? AND ?", Arrays.asList(1, 2));
        Intersect intersect = new Intersect(subQuery);

        List<Object> params = intersect.parameters();

        assertEquals(Arrays.asList(1, 2), params);
    }

    @Test
    void testSetIntersection() {
        SubQuery onSale = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(onSale);
        // Raw SQL SubQuery has no parameters
        assertEquals(0, intersect.parameters().size());
    }

    @Test
    void testSubQueryAccessor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertSame(subQuery, intersect.subQuery());
    }

    @Test
    void testSubQueryAccessor_DefaultConstructor_ReturnsNull() {
        // Uninitialized (Kryo) instance: subQuery() must return null without a ClassCastException.
        Intersect intersect = new Intersect();
        assertNull(intersect.subQuery());
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString();
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT userId FROM userTable");
        Intersect intersect = new Intersect(subQuery);

        String result = intersect.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("INTERSECT"));
        // The SubQuery content might not be affected by naming policy,
        // but the Intersect operator should be present
    }

    @Test
    void testWithEmptyParametersSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        Intersect intersect = new Intersect(subQuery);

        List<Object> params = intersect.parameters();

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }
}
