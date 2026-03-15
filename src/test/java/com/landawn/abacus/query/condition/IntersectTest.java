package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
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
class Intersect2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders");
        Intersect intersect = new Intersect(subQuery);
        SubQuery retrieved = intersect.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(subQuery);
        List<Object> params = intersect.getParameters();
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("activity", List.of("user_id"), new GreaterThan("last_login", "2024-01-01"));
        Intersect intersect = new Intersect(subQuery);
        assertFalse(intersect.getParameters().isEmpty());
        intersect.clearParameters();
        List<Object> params = intersect.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1.hashCode(), intersect2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(intersect, intersect);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        assertNotEquals(intersect1, intersect2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(null, intersect);
    }

    @Test
    public void testFindCommonElements() {
        SubQuery activeUsers = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > ?", "2023-01-01");
        Intersect intersect = new Intersect(activeUsers);
        assertNotNull(intersect);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    public void testSetIntersection() {
        SubQuery onSale = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect intersect = new Intersect(onSale);
        // Raw SQL SubQuery has no parameters
        assertEquals(0, (int) intersect.getParameters().size());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertEquals(Operator.INTERSECT, intersect.operator());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM assignments");
        Intersect intersect = new Intersect(subQuery);
        String result = intersect.toString();
        assertTrue(result.contains("INTERSECT"));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        Intersect intersect = new Intersect(subQuery);
        assertNotEquals(intersect, "not an Intersect");
        assertNotEquals(intersect, new Union(subQuery));
    }
}

public class IntersectTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'active'");
        Intersect intersect = new Intersect(subQuery);

        Assertions.assertNotNull(intersect);
        Assertions.assertEquals(Operator.INTERSECT, intersect.operator());
        Assertions.assertEquals(subQuery, intersect.getCondition());
    }

    @Test
    public void testConstructorWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM users WHERE plan = 'premium' AND active = true");
        Intersect intersect = new Intersect(subQuery);

        Assertions.assertNotNull(intersect);
        Assertions.assertEquals(Operator.INTERSECT, intersect.operator());
        Assertions.assertNotNull(intersect.getCondition());
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table WHERE value = ?");
        Intersect intersect = new Intersect(subQuery);

        List<Object> params = intersect.getParameters();

        Assertions.assertNotNull(params);
    }

    @Test
    public void testGetParametersWithMultipleValues() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table WHERE value BETWEEN ? AND ?");
        Intersect intersect = new Intersect(subQuery);

        List<Object> params = intersect.getParameters();

        Assertions.assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table WHERE status = ?", "active");
        Intersect intersect = new Intersect(subQuery);

        intersect.clearParameters();

        List<Object> params = intersect.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty() || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM orders WHERE order_date > '2023-01-01'");
        Intersect intersect = new Intersect(subQuery);

        String result = intersect.toString();

        Assertions.assertTrue(result.contains("INTERSECT"));
        Assertions.assertTrue(result.contains("SELECT customer_id FROM orders"));
        Assertions.assertTrue(result.contains("order_date > '2023-01-01'"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT userId FROM userTable");
        Intersect intersect = new Intersect(subQuery);

        String result = intersect.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("INTERSECT"));
        // The SubQuery content might not be affected by naming policy,
        // but the Intersect operator should be present
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM table2");

        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        Intersect intersect3 = new Intersect(subQuery3);

        Assertions.assertEquals(intersect1.hashCode(), intersect2.hashCode());
        Assertions.assertNotEquals(intersect1.hashCode(), intersect3.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT id FROM table2");

        Intersect intersect1 = new Intersect(subQuery1);
        Intersect intersect2 = new Intersect(subQuery2);
        Intersect intersect3 = new Intersect(subQuery3);

        Assertions.assertEquals(intersect1, intersect1);
        Assertions.assertEquals(intersect1, intersect2);
        Assertions.assertNotEquals(intersect1, intersect3);
        Assertions.assertNotEquals(intersect1, null);
        Assertions.assertNotEquals(intersect1, "string");
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Intersect intersect = new Intersect(subQuery);

        SubQuery retrieved = intersect.getCondition();

        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testConstructorWithNullSubQuery() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Intersect(null);
        });
    }

    @Test
    public void testWithEmptyParametersSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table");
        Intersect intersect = new Intersect(subQuery);

        List<Object> params = intersect.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }
}
