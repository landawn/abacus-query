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
public class MinusTest extends TestBase {
    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertNotNull(minus);
        assertEquals(Operator.MINUS, minus.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Minus minus = new Minus(subQuery);
        SubQuery retrieved = (SubQuery) minus.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testParameters() {
        SubQuery subQuery = Filters.subQuery("sales", List.of("product_id"), new Equal("region", "WEST"));
        Minus minus = new Minus(subQuery);
        List<Object> params = minus.parameters();
        assertEquals(1, params.size());
        assertEquals("WEST", params.get(0));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM inventory");
        Minus minus = new Minus(subQuery);
        String result = minus.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("MINUS"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertEquals(minus1.hashCode(), minus2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertEquals(minus, minus);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertEquals(minus1, minus2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Minus minus1 = new Minus(subQuery1);
        Minus minus2 = new Minus(subQuery2);
        assertNotEquals(minus1, minus2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertNotEquals(null, minus);
    }

    @Test
    public void testOracleStyle() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Minus minus = new Minus(customersWithOrders);
        assertNotNull(minus);
        assertEquals(Operator.MINUS, minus.operator());
    }

    @Test
    public void testFindUnsoldProducts() {
        SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales");
        Minus minus = new Minus(soldProducts);
        assertEquals(Operator.MINUS, minus.operator());
    }

    @Test
    public void testFindUnassignedEmployees() {
        SubQuery assignedEmployees = Filters.subQuery("project_assignments", List.of("employee_id"), new Equal("status", "ACTIVE"));
        Minus minus = new Minus(assignedEmployees);
        assertEquals(1, minus.parameters().size());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertEquals(Operator.MINUS, minus.operator());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM inventory");
        Minus minus = new Minus(subQuery);
        String result = minus.toString();
        assertTrue(result.contains("MINUS"));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Minus minus = new Minus(subQuery);
        assertNotEquals(minus, "not a Minus");
        assertNotEquals(minus, new Union(subQuery));
    }

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertNotNull(minus);
        Assertions.assertEquals(Operator.MINUS, minus.operator());
        Assertions.assertEquals(subQuery, minus.condition());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id"),
                Filters.and(Filters.eq("status", "cancelled"), Filters.gt("date", "2023-01-01")));
        Minus minus = Filters.minus(subQuery);

        Assertions.assertEquals(subQuery, minus.condition());
        Assertions.assertEquals(2, minus.parameters().size());
    }

    @Test
    public void testParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_records");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertTrue(minus.parameters().isEmpty());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        Minus minus1 = Filters.minus(subQuery1);
        Minus minus2 = Filters.minus(subQuery2);
        Minus minus3 = Filters.minus(subQuery3);

        Assertions.assertTrue(minus1.equals(minus1));
        Assertions.assertTrue(minus1.equals(minus2));
        Assertions.assertFalse(minus1.equals(minus3));
        Assertions.assertFalse(minus1.equals(null));
        Assertions.assertFalse(minus1.equals("not a Minus"));
    }

    @Test
    public void testPracticalExample() {
        // Find products that are in inventory but have never been sold
        SubQuery soldProducts = Filters.subQuery("SELECT DISTINCT product_id FROM sales");
        Minus minus = Filters.minus(soldProducts);

        // This would be used with: SELECT product_id FROM inventory MINUS ...
        Assertions.assertEquals(Operator.MINUS, minus.operator());
        Assertions.assertEquals(soldProducts, minus.condition());
    }

    @Test
    public void testWithParameterizedSubQuery() {
        // Find customers who haven't ordered in the last year
        SubQuery recentCustomers = Filters.subQuery("orders", Arrays.asList("customer_id"), Filters.gt("order_date", "2023-01-01"));
        Minus minus = Filters.minus(recentCustomers);

        Assertions.assertEquals(1, minus.parameters().size());
        Assertions.assertEquals("2023-01-01", minus.parameters().get(0));
    }
}
