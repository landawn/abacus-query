package com.landawn.abacus.query.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class WhereTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        // Test with simple Equal condition
        Condition equalCondition = Filters.eq("status", "active");
        Where where1 = Filters.where(equalCondition);
        Assertions.assertNotNull(where1);
        Assertions.assertEquals(Operator.WHERE, where1.operator());
        Assertions.assertEquals(equalCondition, where1.getCondition());

        // Test with Like condition
        Condition likeCondition = Filters.like("name", "%John%");
        Where where2 = Filters.where(likeCondition);
        Assertions.assertNotNull(where2);
        Assertions.assertEquals(Operator.WHERE, where2.operator());
        Assertions.assertEquals(likeCondition, where2.getCondition());

        // Test with complex AND condition
        Condition andCondition = Filters.eq("age", 25).and(Filters.gt("salary", 50000));
        Where where3 = Filters.where(andCondition);
        Assertions.assertNotNull(where3);
        Assertions.assertEquals(Operator.WHERE, where3.operator());
        Assertions.assertEquals(andCondition, where3.getCondition());

        // Test with OR condition
        Condition orCondition = Filters.eq("department", "IT").or(Filters.eq("department", "HR"));
        Where where4 = Filters.where(orCondition);
        Assertions.assertNotNull(where4);
        Assertions.assertEquals(Operator.WHERE, where4.operator());
        Assertions.assertEquals(orCondition, where4.getCondition());
    }

    @Test
    public void testConstructorWithString() {
        // Test with string expression
        String condition = "age > 18";
        Where where = Filters.where(condition);
        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertNotNull(where.getCondition());
    }

    @Test
    public void testToString() {
        Condition condition = Filters.eq("name", "John");
        Where where = Filters.where(condition);
        String str = where.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("WHERE"));
    }

    @Test
    public void testEquals() {
        Condition condition1 = Filters.eq("status", "active");
        Condition condition2 = Filters.eq("status", "active");
        Condition condition3 = Filters.eq("status", "inactive");

        Where where1 = Filters.where(condition1);
        Where where2 = Filters.where(condition2);
        Where where3 = Filters.where(condition3);

        Assertions.assertEquals(where1, where2);
        Assertions.assertNotEquals(where1, where3);
        Assertions.assertNotEquals(where1, null);
        Assertions.assertNotEquals(where1, "string");
    }

    @Test
    public void testHashCode() {
        Condition condition1 = Filters.eq("status", "active");
        Condition condition2 = Filters.eq("status", "active");

        Where where1 = Filters.where(condition1);
        Where where2 = Filters.where(condition2);

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
        Condition cond1 = Filters.eq("a", 1);
        Condition cond2 = Filters.gt("b", 2);
        Condition cond3 = Filters.lt("c", 3);
        Condition cond4 = Filters.like("d", "%test%");

        Condition complex = cond1.and(cond2).or(cond3.and(cond4));
        Where where = Filters.where(complex);

        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertEquals(complex, where.getCondition());
    }

    @Test
    public void testWithBetweenCondition() {
        Condition betweenCondition = Filters.between("age", 18, 65);
        Where where = Filters.where(betweenCondition);

        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertEquals(betweenCondition, where.getCondition());
    }

    @Test
    public void testWithInCondition() {
        Condition inCondition = Filters.in("status", new String[] { "active", "pending", "approved" });
        Where where = Filters.where(inCondition);

        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertEquals(inCondition, where.getCondition());
    }

    @Test
    public void testWithIsNullCondition() {
        Condition isNullCondition = Filters.isNull("deletedAt");
        Where where = Filters.where(isNullCondition);

        Assertions.assertNotNull(where);
        Assertions.assertEquals(Operator.WHERE, where.operator());
        Assertions.assertEquals(isNullCondition, where.getCondition());
    }
}