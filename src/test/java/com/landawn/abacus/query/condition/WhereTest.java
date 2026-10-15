package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class WhereTest extends TestBase {

    @Test
    void testConstructor_ComplexCondition() {
        And and = new And(new Equal("age", 25), new GreaterThan("salary", 50000));
        Where where = new Where(and);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    void testConstructor_nullCondition_messageNamesCondition() {
        // every clause wrapper funnels through Clause's null check; the message must name a public parameter
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Where((com.landawn.abacus.query.condition.Condition) null));
        assertTrue(e.getMessage().contains("condition"), e.getMessage());
        assertTrue(!e.getMessage().contains("'cond'"), e.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> new Having((com.landawn.abacus.query.condition.Condition) null));
        assertTrue(e2.getMessage().contains("condition"), e2.getMessage());
    }

    @Test
    void testConstructor_SimpleCondition() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    void testConstructorRejectsQuantifiedSubQueryOperand() {
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.any(Filters.subQuery("SELECT id FROM t"))));
    }

    @Test
    void testConstructorWithCondition() {
        // Test with simple Equal condition
        Condition equalCondition = Filters.eq("status", "active");
        Where where1 = Filters.where(equalCondition);
        assertNotNull(where1);
        assertEquals(Operator.WHERE, where1.operator());
        assertEquals(equalCondition, where1.condition());

        // Test with Like condition
        Condition likeCondition = Filters.like("name", "%John%");
        Where where2 = Filters.where(likeCondition);
        assertNotNull(where2);
        assertEquals(Operator.WHERE, where2.operator());
        assertEquals(likeCondition, where2.condition());

        // Test with complex AND condition
        And andCondition = Filters.eq("age", 25).and(Filters.gt("salary", 50000));
        Where where3 = Filters.where(andCondition);
        assertNotNull(where3);
        assertEquals(Operator.WHERE, where3.operator());
        assertEquals(andCondition, where3.condition());

        // Test with OR condition
        Or orCondition = Filters.eq("department", "IT").or(Filters.eq("department", "HR"));
        Where where4 = Filters.where(orCondition);
        assertNotNull(where4);
        assertEquals(Operator.WHERE, where4.operator());
        assertEquals(orCondition, where4.condition());
    }

    @Test
    void testConstructorWithString() {
        // Test with string expression
        String condition = "age > 18";
        Where where = Filters.where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
        assertNotNull(where.condition());
    }

    @Test
    void testEmptyJunctionRendersBooleanIdentity() {
        // An initialized empty junction is a complete predicate, so WHERE accepts it and renders its identity.
        assertEquals("WHERE 1 = 1", new Where(Filters.and()).toString());
        assertEquals("WHERE 1 = 0", Filters.where(Filters.or()).toString());
        assertEquals("WHERE ((1 = 1) OR (id = 1))", new Where(Filters.or(Filters.and(), Filters.eq("id", 1))).toString());
        assertTrue(new Where(Filters.and()).parameters().isEmpty());

        // A blank SqlExpression and an uninitialized junction stay rejected.
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("  ")));
        assertThrows(IllegalArgumentException.class, () -> new Where(new And()));
    }

    @Test
    void testEquals() {
        Condition condition1 = Filters.eq("status", "active");
        Condition condition2 = Filters.eq("status", "active");
        Condition condition3 = Filters.eq("status", "inactive");

        Where where1 = Filters.where(condition1);
        Where where2 = Filters.where(condition2);
        Where where3 = Filters.where(condition3);

        assertEquals(where1, where2);
        assertNotEquals(where1, where3);
        assertNotEquals(where1, null);
        assertNotEquals(where1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        Where where = new Where(new Equal("status", "active"));
        Having having = new Having(new Equal("status", "active"));
        assertNotEquals(where, having);
    }

    @Test
    void testEquals_DifferentConditions() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "inactive");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertNotEquals(where1, where2);
    }

    @Test
    void testEquals_EqualObjects() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1, where2);
    }

    @Test
    void testEquals_Null() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotEquals(null, where);
    }

    @Test
    void testEquals_SameObject() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertEquals(where, where);
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("name", "John");
        Where where = new Where(condition);
        Condition retrieved = where.condition();
        assertNotNull(retrieved);
        assertEquals(condition, retrieved);
    }

    @Test
    void testGetOperator() {
        Where where = new Where(new Equal("id", 1));
        assertEquals(Operator.WHERE, where.operator());
    }

    @Test
    void testHashCode() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1.hashCode(), where2.hashCode());
    }

    @Test
    void testParameters() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        List<Object> params = where.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParameters_MultipleValues() {
        And complexCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("balance", 1000)));
        Where where = new Where(complexCondition);
        List<Object> params = where.parameters();
        assertEquals(2, params.size());
    }

    @Test
    void testToString() {
        Condition condition = Filters.eq("name", "John");
        Where where = Filters.where(condition);
        String str = where.toString();
        assertNotNull(str);
        assertTrue(str.contains("WHERE"));
    }

    @Test
    void testToString_Complex() {
        Or complexCondition = new Or(new And(new Equal("status", "active"), new GreaterThan("balance", 1000)), new Equal("vip", true));
        Where where = new Where(complexCondition);
        String result = where.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    void testToString_NoArgs() {
        Where where = new Where(new Equal("name", "John"));
        String result = where.toString();

        assertTrue(result.contains("WHERE"));
        assertTrue(result.contains("name"));
    }

    @Test
    void testToString_Simple() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        String result = where.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    void testWithBetween() {
        Between condition = new Between("age", 18, 65);
        Where where = new Where(condition);
        assertEquals(2, where.parameters().size());
    }

    @Test
    void testWithBetweenCondition() {
        Condition betweenCondition = Filters.between("age", 18, 65);
        Where where = Filters.where(betweenCondition);

        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
        assertEquals(betweenCondition, where.condition());
    }

    @Test
    void testWithComplexNestedConditions() {
        // Test with deeply nested conditions
        Equal cond1 = Filters.eq("a", 1);
        GreaterThan cond2 = Filters.gt("b", 2);
        LessThan cond3 = Filters.lt("c", 3);
        Like cond4 = Filters.like("d", "%test%");

        Or complex = cond1.and(cond2).or(cond3.and(cond4));
        Where where = Filters.where(complex);

        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
        assertEquals(complex, where.condition());
    }

    @Test
    void testWithInCondition() {
        Condition inCondition = Filters.in("status", new String[] { "active", "pending", "approved" });
        Where where = Filters.where(inCondition);

        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
        assertEquals(inCondition, where.condition());
    }

    @Test
    void testWithInOperator() {
        In condition = new In("status", Arrays.asList("active", "pending", "approved"));
        Where where = new Where(condition);
        assertEquals(3, where.parameters().size());
    }

    @Test
    void testWithIsNull() {
        IsNull condition = new IsNull("deletedAt");
        Where where = new Where(condition);
        assertEquals(0, where.parameters().size());
    }

    @Test
    void testWithIsNullCondition() {
        Condition isNullCondition = Filters.isNull("deletedAt");
        Where where = Filters.where(isNullCondition);

        assertNotNull(where);
        assertEquals(Operator.WHERE, where.operator());
        assertEquals(isNullCondition, where.condition());
    }

    @Test
    void testWithLikeOperator() {
        Like condition = new Like("name", "%John%");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(1, where.parameters().size());
    }

    @Test
    void testWithNullCondition() {
        // Test with null condition - should handle gracefully or throw exception
        assertThrows(Exception.class, () -> {
            new Where(null);
        });
    }

    @Test
    void testWithOrCondition() {
        Or orCondition = new Or(new Equal("type", "A"), new Equal("type", "B"));
        Where where = new Where(orCondition);
        assertEquals(2, where.parameters().size());
    }
}
