package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Union;
import com.landawn.abacus.query.condition.ConditionFactory.CF;

public class UnionTest extends TestBase {

    @Test
    public void testConstructorWithSubQuery() {
        SubQuery subQuery = CF.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = CF.union(subQuery);
        
        Assertions.assertNotNull(union);
        Assertions.assertEquals(Operator.UNION, union.getOperator());
        Assertions.assertEquals(subQuery, union.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = CF.subQuery("SELECT user_id FROM active_users");
        Union union = CF.union(subQuery);
        
        Assertions.assertEquals(subQuery, union.getCondition());
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM users");
        Union union = CF.union(subQuery);
        
        Assertions.assertEquals(Operator.UNION, union.getOperator());
    }

    @Test
    public void testInheritedMethods() {
        SubQuery subQuery = CF.subQuery("users", Arrays.asList("id", "name"), CF.eq("active", true));
        Union union = CF.union(subQuery);
        
        // Test inherited methods from Clause
        Assertions.assertNotNull(union.getParameters());
        Assertions.assertEquals(subQuery.getParameters(), union.getParameters());
    }

    @Test
    public void testWithComplexSubQuery() {
        SubQuery subQuery = CF.subQuery("orders", 
            Arrays.asList("customer_id", "total"), 
            CF.and(CF.gt("total", 1000), CF.eq("status", "completed")));
        Union union = CF.union(subQuery);
        
        Assertions.assertEquals(subQuery, union.getCondition());
        Assertions.assertEquals(2, union.getParameters().size());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM archived_users");
        Union union = CF.union(subQuery);
        
        String result = union.toString();
        Assertions.assertTrue(result.contains("UNION"));
        Assertions.assertTrue(result.contains("SELECT id FROM archived_users"));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = CF.subQuery("users", Arrays.asList("id"), CF.eq("deleted", false));
        Union original = CF.union(subQuery);
        
        Union copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM users");
        
        Union union1 = CF.union(subQuery1);
        Union union2 = CF.union(subQuery2);
        
        Assertions.assertEquals(union1.hashCode(), union2.hashCode());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery2 = CF.subQuery("SELECT id FROM users");
        SubQuery subQuery3 = CF.subQuery("SELECT id FROM customers");
        
        Union union1 = CF.union(subQuery1);
        Union union2 = CF.union(subQuery2);
        Union union3 = CF.union(subQuery3);
        
        Assertions.assertTrue(union1.equals(union1));
        Assertions.assertTrue(union1.equals(union2));
        Assertions.assertFalse(union1.equals(union3));
        Assertions.assertFalse(union1.equals(null));
        Assertions.assertFalse(union1.equals("not a union"));
    }
}