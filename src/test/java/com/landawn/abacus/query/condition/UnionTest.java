package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class UnionTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = Filters.union(subQuery);

        Assertions.assertNotNull(union);
        Assertions.assertEquals(Operator.UNION, union.getOperator());
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

        Assertions.assertEquals(Operator.UNION, union.getOperator());
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
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("deleted", false));
        Union original = Filters.union(subQuery);

        Union copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
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