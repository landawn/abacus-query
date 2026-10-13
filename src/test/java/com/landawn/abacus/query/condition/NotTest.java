package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotTest extends TestBase {

    @Test
    void testComplexNestedConditions() {
        // Test NOT with complex nested conditions
        Not notAndCondition = Filters.not(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.like("email", "%@company.com")));

        assertNotNull(notAndCondition);
        assertEquals(3, notAndCondition.parameters().size());
    }

    @Test
    void testConstructor() {
        Equal innerCondition = new Equal("status", "active");
        Not condition = new Not(innerCondition);

        assertEquals(Operator.NOT, condition.operator());
        assertSame(innerCondition, condition.condition());
    }

    @Test
    void testConstructor_NullCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Not(null));
    }

    @Test
    void testConstructorRejectsStructuralAndEmptyOperands() {
        Criteria criteria = Criteria.builder().where(Filters.eq("a", 1)).build();

        assertThrows(IllegalArgumentException.class, () -> Filters.not(criteria));
        assertThrows(IllegalArgumentException.class, () -> Filters.not(Filters.expr("")));
        assertThrows(IllegalArgumentException.class, () -> Filters.not(Filters.expr("   ")));
        assertThrows(IllegalArgumentException.class, () -> Filters.not(new And()));
    }

    @Test
    void testConstructorWithBetweenCondition() {
        Between between = Filters.between("age", 18, 65);
        Not notBetween = Filters.not(between);

        assertEquals(Operator.NOT, notBetween.operator());
        assertEquals(between, notBetween.condition());
    }

    @Test
    void testConstructorWithInCondition() {
        In inCondition = Filters.in("status", Arrays.asList("active", "pending"));
        Not notIn = Filters.not(inCondition);

        assertEquals(Operator.NOT, notIn.operator());
        assertEquals(inCondition, notIn.condition());
    }

    @Test
    void testConstructorWithLikeCondition() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notCondition = Filters.not(likeCondition);

        assertNotNull(notCondition);
        assertEquals(Operator.NOT, notCondition.operator());
        assertEquals(likeCondition, notCondition.condition());
    }

    @Test
    void testDoubleNegation() {
        Like likeCondition = Filters.like("name", "John%");
        Not notCondition = Filters.not(likeCondition);
        Not doubleNot = Filters.not(notCondition);

        assertEquals(notCondition, doubleNot.condition());
        assertEquals(Operator.NOT, doubleNot.operator());
    }

    @Test
    void testEmptyJunctionOperandNegatesItsBooleanIdentity() {
        // An initialized empty junction is a complete predicate (1 = 1 / 1 = 0), so NOT accepts it.
        assertEquals("NOT (1 = 1)", new Not(Filters.and()).toString());
        assertEquals("NOT (1 = 0)", Filters.not(Filters.or()).toString());
        assertEquals("NOT (1 = 1)", Filters.and().not().toString());
        assertTrue(new Not(Filters.and()).parameters().isEmpty());
    }

    @Test
    void testEquals() {
        Like likeCondition1 = Filters.like("name", "%test%");
        Like likeCondition2 = Filters.like("name", "%test%");
        Like likeCondition3 = Filters.like("name", "%demo%");

        Not not1 = Filters.not(likeCondition1);
        Not not2 = Filters.not(likeCondition2);
        Not not3 = Filters.not(likeCondition3);

        assertTrue(not1.equals(not1));
        assertTrue(not1.equals(not2));
        assertFalse(not1.equals(not3));
        assertFalse(not1.equals(null));
        assertFalse(not1.equals("not a Not"));
    }

    @Test
    void testEquals_DifferentClass() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentInnerConditions() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("b", 2));
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        Not cond1 = new Not(new Equal("a", 1));
        Not cond2 = new Not(new Equal("a", 1));
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        Not condition = new Not(new Equal("a", 1));
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        Not condition = new Not(new Equal("a", 1));
        assertEquals(condition, condition);
    }

    @Test
    void testGetCondition() {
        Equal innerCondition = new Equal("age", 25);
        Not condition = new Not(innerCondition);

        Equal retrieved = (Equal) condition.condition();
        assertSame(innerCondition, retrieved);
    }

    @Test
    void testGetCondition_ComplexCondition() {
        And innerAnd = new And(new Equal("a", 1), new Equal("b", 2));
        Not condition = new Not(innerAnd);

        And retrieved = (And) condition.condition();
        assertSame(innerAnd, retrieved);
    }

    @Test
    void testGetOperator() {
        Not condition = new Not(new Equal("field", "value"));
        assertEquals(Operator.NOT, condition.operator());
    }

    @Test
    void testHashCode() {
        Equal inner = new Equal("field", "value");
        Not cond1 = new Not(inner);
        Not cond2 = new Not(new Equal("field", "value"));

        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNestedCondition() {
        And innerAnd = new And(new Equal("status", "active"), new GreaterThan("age", 18));
        Not condition = new Not(innerAnd);

        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }

    @Test
    void testNullConditionMessageNamesConstructorParameter() {
        // the shared operand validator used to report the internal name 'cond'
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Not(null));
        assertTrue(e.getMessage().contains("condition"), e.getMessage());
        assertTrue(!e.getMessage().contains("'cond'"), e.getMessage());
    }

    @Test
    void testParameters() {
        Equal innerCondition = new Equal("name", "John");
        Not condition = new Not(innerCondition);

        List<Object> params = condition.parameters();
        assertEquals(1, params.size());
        assertEquals("John", params.get(0));
    }

    @Test
    void testParameters_ComplexCondition() {
        Between between = new Between("age", 18, 65);
        Not condition = new Not(between);

        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    void testParameters_NullCondition() {
        Not condition = new Not(new Equal("field", null));
        List<Object> params = condition.parameters();

        assertEquals(0, params.size());
    }

    @Test
    void testToString() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notCondition = Filters.not(likeCondition);

        String result = notCondition.toString();
        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("%test%"));
    }

    @Test
    void testToString_NoArgs() {
        Not condition = new Not(new Equal("status", "active"));
        String result = condition.toString();

        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("status"));
    }

    @Test
    void testToString_NoChange() {
        Equal innerCondition = new Equal("userName", "Bob");
        Not condition = new Not(innerCondition);

        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NOT"));
        assertTrue(result.contains("userName"));
    }

    @Test
    void testToString_SnakeCase() {
        Equal innerCondition = new Equal("firstName", "Charlie");
        Not condition = new Not(innerCondition);

        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("first_name"));
    }
}
