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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Minus2025Test extends TestBase {

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
        SubQuery retrieved = minus.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("sales", List.of("product_id"), new Equal("region", "WEST"));
        Minus minus = new Minus(subQuery);
        List<Object> params = minus.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("WEST", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("project_assignments", List.of("employee_id"), new Equal("active", "true"));
        Minus minus = new Minus(subQuery);
        assertFalse(minus.getParameters().isEmpty());
        minus.clearParameters();
        List<Object> params = minus.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM inventory");
        Minus minus = new Minus(subQuery);
        String result = minus.toString(NamingPolicy.NO_CHANGE);
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
        assertEquals(1, (int) minus.getParameters().size());
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
}

public class MinusTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertNotNull(minus);
        Assertions.assertEquals(Operator.MINUS, minus.operator());
        Assertions.assertEquals(subQuery, minus.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM inactive_customers");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertEquals(subQuery, minus.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM test");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertEquals(Operator.MINUS, minus.operator());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("customer_id"),
                Filters.and(Filters.eq("status", "cancelled"), Filters.gt("date", "2023-01-01")));
        Minus minus = Filters.minus(subQuery);

        Assertions.assertEquals(subQuery, minus.getCondition());
        Assertions.assertEquals(2, minus.getParameters().size());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("id"), Filters.eq("discontinued", true));
        Minus minus = Filters.minus(subQuery);

        Assertions.assertEquals(subQuery.getParameters(), minus.getParameters());
        Assertions.assertEquals(1, minus.getParameters().size());
        Assertions.assertEquals(true, minus.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithRawSqlSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_records");
        Minus minus = Filters.minus(subQuery);

        Assertions.assertTrue(minus.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.in("status", Arrays.asList("deleted", "banned")));
        Minus minus = Filters.minus(subQuery);

        minus.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(subQuery.getParameters().stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        Minus minus = Filters.minus(subQuery);

        String result = minus.toString();
        Assertions.assertTrue(result.contains("MINUS"));
        Assertions.assertTrue(result.contains("SELECT id FROM inactive_users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM test");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM other");

        Minus minus1 = Filters.minus(subQuery1);
        Minus minus2 = Filters.minus(subQuery2);
        Minus minus3 = Filters.minus(subQuery3);

        Assertions.assertEquals(minus1.hashCode(), minus2.hashCode());
        Assertions.assertNotEquals(minus1.hashCode(), minus3.hashCode());
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
        Assertions.assertEquals(soldProducts, minus.getCondition());
    }

    @Test
    public void testWithParameterizedSubQuery() {
        // Find customers who haven't ordered in the last year
        SubQuery recentCustomers = Filters.subQuery("orders", Arrays.asList("customer_id"), Filters.gt("order_date", "2023-01-01"));
        Minus minus = Filters.minus(recentCustomers);

        Assertions.assertEquals(1, minus.getParameters().size());
        Assertions.assertEquals("2023-01-01", minus.getParameters().get(0));
    }
}
