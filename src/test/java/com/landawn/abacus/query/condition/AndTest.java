package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class And2025Test extends TestBase {

    @Test
    public void testConstructor_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        assertEquals(2, (int) junction.getConditions().size());
        assertEquals(Operator.AND, junction.operator());
    }

    @Test
    public void testConstructor_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        List<Condition> conditions = Arrays.asList(cond1, cond2);

        And junction = new And(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testConstructor_SingleCondition() {
        Equal cond = new Equal("status", "active");
        And junction = new And(cond);
        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
    }

    @Test
    public void testConstructor_EmptyConditions() {
        And junction = new And();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        List<Condition> conditions = junction.getConditions();
        assertNotNull(conditions);
        assertEquals(2, (int) conditions.size());
    }

    @Test
    public void testGetParameters() {
        And junction = new And(new Equal("a", 1), new Equal("b", "test"));
        List<Object> params = junction.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals((Object) Integer.valueOf(1), params.get(0));
        assertEquals("test", params.get(1));
    }

    @Test
    public void testGetParameters_EmptyConditions() {
        And junction = new And();
        List<Object> params = junction.getParameters();
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testToString_NoChange() {
        And junction = new And(new Equal("a", 1), new Equal("b", 2));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testToString_EmptyConditions() {
        And junction = new And();
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertEquals("", result);
    }

    @Test
    public void testToString_SingleCondition() {
        And junction = new And(new Equal("status", "active"));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("status"));
        assertFalse(result.contains("AND"));
    }

    @Test
    public void testHashCode() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        And junction = new And(new Equal("a", 1));
        assertEquals(junction, junction);
    }

    @Test
    public void testEquals_EqualObjects() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1, j2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("b", 2));
        assertNotEquals(j1, j2);
    }

    @Test
    public void testEquals_Null() {
        And junction = new And(new Equal("a", 1));
        assertNotEquals(null, junction);
    }

    @Test
    public void testAndMethod() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And original = new And(cond1, cond2);
        And extended = original.and(cond3);

        assertEquals(Integer.valueOf(3), extended.getConditions().size());
        assertEquals(2, (int) original.getConditions().size());
    }

    @Test
    public void testAndMethod_NullCondition() {
        And junction = new And(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.and(null));
    }

    @Test
    public void testAndMethod_RejectsClauseCondition() {
        And junction = new And(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.and(Filters.where(Filters.equal("b", 2))));
    }

    @Test
    public void testNestedJunctions() {
        And inner = new And(new Equal("a", 1), new Equal("b", 2));
        And outer = new And(inner, new Equal("c", 3));

        assertEquals(2, (int) outer.getConditions().size());
        List<Object> params = outer.getParameters();
        assertEquals(3, (int) params.size());
    }

    @Test
    public void testOrMethod() {
        And and = new And(new Equal("a", 1));
        Equal cond = new Equal("b", 2);
        Or result = and.or(cond);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
        assertEquals(Operator.OR, result.operator());
    }

    @Test
    public void testNotMethod() {
        And and = new And(new Equal("a", 1), new Equal("b", 2));
        Not result = and.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        And innerCondition = result.getCondition();
        assertEquals(2, (int) innerCondition.getConditions().size());
    }

    @Test
    public void testToString_NoArgs() {
        And and = new And(new Equal("status", "active"), new Equal("verified", true));
        String result = and.toString();

        assertTrue(result.contains("AND"));
        assertTrue(result.contains("status"));
        assertTrue(result.contains("verified"));
    }

    @Test
    public void testConstructor_NullConditionInArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new And(new Equal("a", 1), null, new Equal("b", 2));
        });
    }

    @Test
    public void testGetOperator() {
        And and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.operator());
    }

    @Test
    public void testGetOperator_EmptyConstructor() {
        // Empty constructor doesn't set operator (for Kryo serialization)
        And and = new And();
        assertNull(and.operator());
    }

    @Test
    public void testConstructor_NullConditionInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            new And(conditions);
        });
    }

}

public class AndTest extends TestBase {

    @Test
    public void testConstructorWithVarargs() {
        Equal eq1 = Filters.eq("status", "active");
        GreaterThan gt = Filters.gt("age", 18);

        And and = Filters.and(eq1, gt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.operator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(eq1));
        Assertions.assertTrue(and.getConditions().contains(gt));
    }

    @Test
    public void testConstructorWithCollection() {
        List<Condition> conditions = Arrays.asList(Filters.eq("department", "Sales"), Filters.ge("salary", 50000), Filters.lt("age", 65));

        And and = Filters.and(conditions);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(3, and.getConditions().size());
    }

    @Test
    public void testConstructorWithEmptyArray() {
        And and = Filters.and();

        Assertions.assertNotNull(and);
        Assertions.assertEquals(0, and.getConditions().size());
    }

    @Test
    public void testConstructorWithNullCollection() {
        And and = Filters.and((Collection<Condition>) null);

        assertTrue(and.getConditions().isEmpty());
    }

    @Test
    public void testAndMethod() {
        And and = Filters.and(Filters.eq("status", "active"));
        NotEqual ne = Filters.ne("type", "temp");

        And extended = and.and(ne);

        Assertions.assertNotNull(extended);
        Assertions.assertNotSame(and, extended);
        Assertions.assertEquals(2, extended.getConditions().size());
        Assertions.assertEquals(1, and.getConditions().size()); // Original unchanged
    }

    @Test
    public void testAndMethodThrowsException() {
        And and = Filters.and();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            and.and(null);
        });
    }

    @Test
    public void testToString() {
        And and = Filters.and(Filters.eq("name", "John"), Filters.gt("age", 25));

        String result = and.toString();
        Assertions.assertTrue(result.contains("name = 'John'"));
        Assertions.assertTrue(result.contains("AND"));
        Assertions.assertTrue(result.contains("age > 25"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        And and = Filters.and(Filters.eq("firstName", "Jane"), Filters.le("yearOfBirth", 2000));

        String result = and.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("first_name = 'Jane'"));
        Assertions.assertTrue(result.contains("year_of_birth <= 2000"));
    }

    @Test
    public void testGetParameters() {
        And and = Filters.and(Filters.eq("status", "active"), Filters.between("age", 18, 65), Filters.like("name", "John%"));

        List<Object> params = and.getParameters();

        Assertions.assertEquals(4, params.size());
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(18));
        Assertions.assertTrue(params.contains(65));
        Assertions.assertTrue(params.contains("John%"));
    }

    @Test
    public void testEquals() {
        And and1 = Filters.and(Filters.eq("name", "Test"), Filters.gt("value", 10));

        And and2 = Filters.and(Filters.eq("name", "Test"), Filters.gt("value", 10));

        And and3 = Filters.and(Filters.eq("name", "Test"));

        Assertions.assertEquals(and1, and2);
        Assertions.assertNotEquals(and1, and3);
        Assertions.assertNotEquals(and1, null);
        Assertions.assertNotEquals(and1, "string");
    }

    @Test
    public void testHashCode() {
        And and1 = Filters.and(Filters.eq("id", 1), Filters.ne("deleted", true));

        And and2 = Filters.and(Filters.eq("id", 1), Filters.ne("deleted", true));

        Assertions.assertEquals(and1.hashCode(), and2.hashCode());
    }

    @Test
    public void testOr() {
        And and = Filters.and(Filters.eq("type", "A"), Filters.eq("status", "active"));

        Or or = and.or(Filters.eq("priority", "high"));

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.operator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        And and = Filters.and(Filters.eq("available", true), Filters.gt("stock", 0));

        Not not = and.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.operator());
        Assertions.assertEquals(and, not.getCondition());
    }

    @Test
    public void testComplexNestedAnd() {
        And nested1 = Filters.and(Filters.eq("a", 1), Filters.eq("b", 2));

        And nested2 = Filters.and(Filters.eq("c", 3), Filters.eq("d", 4));

        And complex = Filters.and(nested1, nested2);

        Assertions.assertEquals(2, complex.getConditions().size());
        List<Object> params = complex.getParameters();
        Assertions.assertEquals(4, params.size());
    }
}
