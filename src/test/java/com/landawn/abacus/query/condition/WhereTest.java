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
class Where2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleCondition() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    public void testConstructor_ComplexCondition() {
        And and = new And(new Equal("age", (Object) 25), new GreaterThan("salary", (Object) 50000));
        Where where = new Where(and);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("name", "John");
        Where where = new Where(condition);
        Condition retrieved = where.getCondition();
        assertNotNull(retrieved);
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        List<Object> params = where.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        And complexCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("balance", (Object) 1000)));
        Where where = new Where(complexCondition);
        List<Object> params = where.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testToString_Simple() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        String result = where.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testToString_Complex() {
        Or complexCondition = new Or(new And(new Equal("status", "active"), new GreaterThan("balance", (Object) 1000)), new Equal("vip", true));
        Where where = new Where(complexCondition);
        String result = where.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testHashCode() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1.hashCode(), where2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertEquals(where, where);
    }

    @Test
    public void testEquals_EqualObjects() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1, where2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "inactive");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertNotEquals(where1, where2);
    }

    @Test
    public void testEquals_Null() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotEquals(null, where);
    }

    @Test
    public void testWithLikeOperator() {
        Like condition = new Like("name", "%John%");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(1, (int) where.getParameters().size());
    }

    @Test
    public void testWithBetween() {
        Between condition = new Between("age", (Object) 18, (Object) 65);
        Where where = new Where(condition);
        assertEquals(2, (int) where.getParameters().size());
    }

    @Test
    public void testWithInOperator() {
        In condition = new In("status", Arrays.asList("active", "pending", "approved"));
        Where where = new Where(condition);
        assertEquals(3, (int) where.getParameters().size());
    }

    @Test
    public void testWithOrCondition() {
        Or orCondition = new Or(new Equal("type", "A"), new Equal("type", "B"));
        Where where = new Where(orCondition);
        assertEquals(2, (int) where.getParameters().size());
    }

    @Test
    public void testToString_NoArgs() {
        Where where = new Where(new Equal("name", "John"));
        String result = where.toString();

        assertTrue(result.contains("WHERE"));
        assertTrue(result.contains("name"));
    }

    @Test
    public void testGetOperator() {
        Where where = new Where(new Equal("id", 1));
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    public void testWithIsNull() {
        IsNull condition = new IsNull("deletedAt");
        Where where = new Where(condition);
        assertEquals(0, (int) where.getParameters().size());
    }

    @Test
    public void testEquals_DifferentClass() {
        Where where = new Where(new Equal("status", "active"));
        Having having = new Having(new Equal("status", "active"));
        assertNotEquals(where, (Object) having);
    }
}

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
        And andCondition = Filters.eq("age", 25).and(Filters.gt("salary", 50000));
        Where where3 = Filters.where(andCondition);
        Assertions.assertNotNull(where3);
        Assertions.assertEquals(Operator.WHERE, where3.operator());
        Assertions.assertEquals(andCondition, where3.getCondition());

        // Test with OR condition
        Or orCondition = Filters.eq("department", "IT").or(Filters.eq("department", "HR"));
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
        Equal cond1 = Filters.eq("a", 1);
        GreaterThan cond2 = Filters.gt("b", 2);
        LessThan cond3 = Filters.lt("c", 3);
        Like cond4 = Filters.like("d", "%test%");

        Or complex = cond1.and(cond2).or(cond3.and(cond4));
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
