package com.landawn.abacus.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WhereTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        // Test with simple Equal condition
        Condition equalCondition = ConditionFactory.eq("status", "active");
        Where where1 = ConditionFactory.where(equalCondition);
        Assertions.assertNotNull(where1);
        Assertions.assertEquals(Operator.WHERE, where1.getOperator());
        Assertions.assertEquals(equalCondition, where1.getCondition());

        // Test with Like condition
        Condition likeCondition = ConditionFactory.like("name", "%John%");
        Where where2 = ConditionFactory.where(likeCondition);
        Assertions.assertNotNull(where2);
        Assertions.assertEquals(Operator.WHERE, where2.getOperator());
        Assertions.assertEquals(likeCondition, where2.getCondition());

        // Test with complex AND condition
        Condition andCondition = ConditionFactory.eq("age", 25).and(ConditionFactory.gt("salary", 50000));
        Where where3 = ConditionFactory.where(andCondition);
        Assertions.assertNotNull(where3);
        Assertions.assertEquals(Operator.WHERE, where3.getOperator());
        Assertions.assertEquals(andCondition, where3.getCondition());

        // Test with OR condition
        Condition orCondition = ConditionFactory.eq("department", "IT").or(ConditionFactory.eq("department", "HR"));
        Where where4 = ConditionFactory.where(orCondition);
        Assertions.assertNotNull(where4);
        Assertions.assertEquals(Operator.WHERE, where4.getOperator());
        Assertions.assertEquals(orCondition, where4.getCondition());
    }

    @Test
    public void testConstructorWithString() {
        // Test with string expression
        String condition = "age > 18";
        Where where = ConditionFactory.where(condition);
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertNotNull(where.getCondition());
    }

    @Test
    public void testToString() {
        Condition condition = ConditionFactory.eq("name", "John");
        Where where = ConditionFactory.where(condition);
        String str = where.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("WHERE"));
    }

    @Test
    public void testEquals() {
        Condition condition1 = ConditionFactory.eq("status", "active");
        Condition condition2 = ConditionFactory.eq("status", "active");
        Condition condition3 = ConditionFactory.eq("status", "inactive");

        Where where1 = ConditionFactory.where(condition1);
        Where where2 = ConditionFactory.where(condition2);
        Where where3 = ConditionFactory.where(condition3);

        Assertions.assertEquals(where1, where2);
        Assertions.assertNotEquals(where1, where3);
        Assertions.assertNotEquals(where1, null);
        Assertions.assertNotEquals(where1, "string");
    }

    @Test
    public void testHashCode() {
        Condition condition1 = ConditionFactory.eq("status", "active");
        Condition condition2 = ConditionFactory.eq("status", "active");

        Where where1 = ConditionFactory.where(condition1);
        Where where2 = ConditionFactory.where(condition2);

        Assertions.assertEquals(where1.hashCode(), where2.hashCode());
    }

    @Test
    public void testWithNullCondition() {
        // Test with null condition - should handle gracefully or throw exception
        Assertions.assertThrows(Exception.class, () -> {
            new Where(null);
        });
    }

    @Test
    public void testWithComplexNestedConditions() {
        // Test with deeply nested conditions
        Condition cond1 = ConditionFactory.eq("a", 1);
        Condition cond2 = ConditionFactory.gt("b", 2);
        Condition cond3 = ConditionFactory.lt("c", 3);
        Condition cond4 = ConditionFactory.like("d", "%test%");

        Condition complex = cond1.and(cond2).or(cond3.and(cond4));
        Where where = ConditionFactory.where(complex);

        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(complex, where.getCondition());
    }

    @Test
    public void testWithBetweenCondition() {
        Condition betweenCondition = ConditionFactory.between("age", 18, 65);
        Where where = ConditionFactory.where(betweenCondition);
        
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(betweenCondition, where.getCondition());
    }

    @Test
    public void testWithInCondition() {
        Condition inCondition = ConditionFactory.in("status", new String[]{"active", "pending", "approved"});
        Where where = ConditionFactory.where(inCondition);
        
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(inCondition, where.getCondition());
    }

    @Test
    public void testWithIsNullCondition() {
        Condition isNullCondition = ConditionFactory.isNull("deletedAt");
        Where where = ConditionFactory.where(isNullCondition);
        
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.getOperator());
        Assertions.assertEquals(isNullCondition, where.getCondition());
    }
}