package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AndTest extends TestBase {

    @Test
    void testAndMethod() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And original = new And(cond1, cond2);
        And extended = original.and(cond3);

        assertEquals(Integer.valueOf(3), extended.conditions().size());
        assertEquals(2, original.conditions().size());
    }

    @Test
    void testAndMethod_NullCondition() {
        And junction = new And(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.and(null));
    }

    @Test
    void testAndMethod_RejectsClauseCondition() {
        And junction = new And(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.and(Filters.where(Filters.equal("b", 2))));
    }

    @Test
    void testAndMethodThrowsException() {
        And and = Filters.and();

        assertThrows(IllegalArgumentException.class, () -> {
            and.and(null);
        });
    }

    @Test
    void testComplexNestedAnd() {
        And nested1 = Filters.and(Filters.eq("a", 1), Filters.eq("b", 2));

        And nested2 = Filters.and(Filters.eq("c", 3), Filters.eq("d", 4));

        And complex = Filters.and(nested1, nested2);

        assertEquals(2, complex.conditions().size());
        List<Object> params = complex.parameters();
        assertEquals(4, params.size());
    }

    @Test
    void testConditions() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        List<Condition> conditions = junction.conditions();
        assertNotNull(conditions);
        assertEquals(2, conditions.size());
    }

    @Test
    void testConstructor_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        List<Condition> conditions = Arrays.asList(cond1, cond2);

        And junction = new And(conditions);
        assertEquals(2, junction.conditions().size());
    }

    @Test
    void testConstructor_EmptyConditions() {
        And junction = new And();
        assertEquals(Integer.valueOf(0), junction.conditions().size());
    }

    @Test
    void testConstructor_NullConditionInArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new And(new Equal("a", 1), null, new Equal("b", 2));
        });
    }

    @Test
    void testConstructor_NullConditionInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            new And(conditions);
        });
    }

    @Test
    void testConstructor_SingleCondition() {
        Equal cond = new Equal("status", "active");
        And junction = new And(cond);
        assertEquals((Object) Integer.valueOf(1), junction.conditions().size());
    }

    @Test
    void testConstructor_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        assertEquals(2, junction.conditions().size());
        assertEquals(Operator.AND, junction.operator());
    }

    @Test
    void testConstructorRejectsClauseOperand() {
        assertThrows(IllegalArgumentException.class, () -> new And(Filters.where(Filters.eq("a", 1))));
    }

    @Test
    void testConstructorRejectsOnConnectorOperand() {
        assertThrows(IllegalArgumentException.class, () -> new And(Filters.on("a.id", "b.a_id"), Filters.eq("active", true)));
    }

    @Test
    void testConstructorWithCollection() {
        List<Condition> conditions = Arrays.asList(Filters.eq("department", "Sales"), Filters.ge("salary", 50000), Filters.lt("age", 65));

        And and = Filters.and(conditions);

        assertNotNull(and);
        assertEquals(3, and.conditions().size());
    }

    @Test
    void testConstructorWithEmptyArray() {
        And and = Filters.and();

        assertNotNull(and);
        assertEquals(0, and.conditions().size());
    }

    @Test
    void testConstructorWithNullCollection() {
        And and = Filters.and((Collection<Condition>) null);

        assertTrue(and.conditions().isEmpty());
    }

    @Test
    void testConstructorWithVarargs() {
        Equal eq1 = Filters.eq("status", "active");
        GreaterThan gt = Filters.gt("age", 18);

        And and = Filters.and(eq1, gt);

        assertNotNull(and);
        assertEquals(Operator.AND, and.operator());
        assertEquals(2, and.conditions().size());
        assertTrue(and.conditions().contains(eq1));
        assertTrue(and.conditions().contains(gt));
    }

    @Test
    void testEquals() {
        And and1 = Filters.and(Filters.eq("name", "Test"), Filters.gt("value", 10));

        And and2 = Filters.and(Filters.eq("name", "Test"), Filters.gt("value", 10));

        And and3 = Filters.and(Filters.eq("name", "Test"));

        assertEquals(and1, and2);
        assertNotEquals(and1, and3);
        assertNotEquals(and1, null);
        assertNotEquals(and1, "string");
    }

    @Test
    void testEquals_DifferentConditions() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("b", 2));
        assertNotEquals(j1, j2);
    }

    @Test
    void testEquals_EqualObjects() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1, j2);
    }

    @Test
    void testEquals_Null() {
        And junction = new And(new Equal("a", 1));
        assertNotEquals(null, junction);
    }

    @Test
    void testEquals_SameObject() {
        And junction = new And(new Equal("a", 1));
        assertEquals(junction, junction);
    }

    @Test
    void testGetOperator() {
        And and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.operator());
    }

    @Test
    void testGetOperator_EmptyConstructor() {
        // Empty constructor doesn't set operator (for Kryo serialization)
        And and = new And();
        assertNull(and.operator());
    }

    @Test
    void testHashCode() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    void testNestedJunctions() {
        And inner = new And(new Equal("a", 1), new Equal("b", 2));
        And outer = new And(inner, new Equal("c", 3));

        assertEquals(2, outer.conditions().size());
        List<Object> params = outer.parameters();
        assertEquals(3, params.size());
    }

    @Test
    void testNot() {
        And and = Filters.and(Filters.eq("available", true), Filters.gt("stock", 0));

        Not not = and.not();

        assertNotNull(not);
        assertEquals(Operator.NOT, not.operator());
        assertEquals(and, not.condition());
    }

    @Test
    void testNotMethod() {
        And and = new And(new Equal("a", 1), new Equal("b", 2));
        Not result = and.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        And innerCondition = (And) result.condition();
        assertEquals(2, innerCondition.conditions().size());
    }

    @Test
    void testOr() {
        And and = Filters.and(Filters.eq("type", "A"), Filters.eq("status", "active"));

        Or or = and.or(Filters.eq("priority", "high"));

        assertNotNull(or);
        assertEquals(Operator.OR, or.operator());
        assertEquals(2, or.conditions().size());
    }

    @Test
    void testOrMethod() {
        And and = new And(new Equal("a", 1));
        Equal cond = new Equal("b", 2);
        Or result = and.or(cond);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
        assertEquals(Operator.OR, result.operator());
    }

    @Test
    void testParameters() {
        And junction = new And(new Equal("a", 1), new Equal("b", "test"));
        List<Object> params = junction.parameters();
        assertEquals(2, params.size());
        assertEquals(Integer.valueOf(1), params.get(0));
        assertEquals("test", params.get(1));
    }

    @Test
    void testParameters_EmptyConditions() {
        And junction = new And();
        List<Object> params = junction.parameters();
        assertEquals(0, params.size());
    }

    @Test
    void testToString() {
        And and = Filters.and(Filters.eq("name", "John"), Filters.gt("age", 25));

        String result = and.toString();
        assertTrue(result.contains("name = 'John'"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("age > 25"));
    }

    @Test
    void testToString_EmptyConditions() {
        And junction = Filters.and();
        String result = junction.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("1 = 1", result);
    }

    @Test
    void testToString_NoArgs() {
        And and = new And(new Equal("status", "active"), new Equal("verified", true));
        String result = and.toString();

        assertTrue(result.contains("AND"));
        assertTrue(result.contains("status"));
        assertTrue(result.contains("verified"));
    }

    @Test
    void testToString_NoChange() {
        And junction = new And(new Equal("a", 1), new Equal("b", 2));
        String result = junction.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    void testToString_SingleCondition() {
        And junction = new And(new Equal("status", "active"));
        String result = junction.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("status"));
        assertFalse(result.contains("AND"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        And and = Filters.and(Filters.eq("firstName", "Jane"), Filters.le("yearOfBirth", 2000));

        String result = and.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("first_name = 'Jane'"));
        assertTrue(result.contains("year_of_birth <= 2000"));
    }
}
