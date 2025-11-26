package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class IntersectTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'active'");
        Intersect intersect = new Intersect(subQuery);

        Assertions.assertNotNull(intersect);
        Assertions.assertEquals(Operator.INTERSECT, intersect.getOperator());
        Assertions.assertEquals(subQuery, intersect.getCondition());
    }

    @Test
    public void testConstructorWithComplexSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM users WHERE plan = 'premium' AND active = true");
        Intersect intersect = new Intersect(subQuery);

        Assertions.assertNotNull(intersect);
        Assertions.assertEquals(Operator.INTERSECT, intersect.getOperator());
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
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM products WHERE price > ?");
        Intersect original = new Intersect(subQuery);
        Intersect copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals(original.toString(), copy.toString());
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

        String result = intersect.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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
        Assertions.assertThrows(NullPointerException.class, () -> {
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