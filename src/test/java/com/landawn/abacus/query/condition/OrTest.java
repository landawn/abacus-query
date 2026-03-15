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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Or2025Test extends TestBase {

    @Test
    public void testConstructor_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        assertEquals(2, (int) junction.getConditions().size());
        assertEquals(Operator.OR, junction.operator());
    }

    @Test
    public void testConstructor_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        List<Condition> conditions = Arrays.asList(cond1, cond2);

        Or junction = new Or(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testConstructor_SingleCondition() {
        Equal cond = new Equal("status", "active");
        Or junction = new Or(cond);
        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
    }

    @Test
    public void testConstructor_EmptyConditions() {
        Or junction = new Or();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        List<Condition> conditions = junction.getConditions();
        assertNotNull(conditions);
        assertEquals(2, (int) conditions.size());
    }

    @Test
    public void testGetParameters() {
        Or junction = new Or(new Equal("a", 1), new Equal("b", "test"));
        List<Object> params = junction.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals((Object) Integer.valueOf(1), params.get(0));
        assertEquals("test", params.get(1));
    }

    @Test
    public void testGetParameters_EmptyConditions() {
        Or junction = new Or();
        List<Object> params = junction.getParameters();
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        junction.clearParameters();
        assertNull(cond1.getPropValue());
        assertNull(cond2.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Or junction = new Or(new Equal("a", 1), new Equal("b", 2));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testToString_EmptyConditions() {
        Or junction = new Or();
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertEquals("", result);
    }

    @Test
    public void testToString_SingleCondition() {
        Or junction = new Or(new Equal("status", "active"));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("status"));
        assertFalse(result.contains("OR"));
    }

    @Test
    public void testHashCode() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("a", 1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Or junction = new Or(new Equal("a", 1));
        assertEquals(junction, junction);
    }

    @Test
    public void testEquals_EqualObjects() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("a", 1));
        assertEquals(j1, j2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("b", 2));
        assertNotEquals(j1, j2);
    }

    @Test
    public void testEquals_Null() {
        Or junction = new Or(new Equal("a", 1));
        assertNotEquals(null, junction);
    }

    @Test
    public void testOrMethod() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        Or original = new Or(cond1, cond2);
        Or extended = original.or(cond3);

        assertEquals(Integer.valueOf(3), extended.getConditions().size());
        assertEquals(2, (int) original.getConditions().size());
    }

    @Test
    public void testOrMethod_NullCondition() {
        Or junction = new Or(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.or(null));
    }

    @Test
    public void testOrMethod_RejectsClauseCondition() {
        Or junction = new Or(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.or(Filters.orderBy("b")));
    }

    @Test
    public void testNestedJunctions() {
        Or inner = new Or(new Equal("a", 1), new Equal("b", 2));
        Or outer = new Or(inner, new Equal("c", 3));

        assertEquals(2, (int) outer.getConditions().size());
        List<Object> params = outer.getParameters();
        assertEquals(3, (int) params.size());
    }

    @Test
    public void testAndMethod() {
        Or or = new Or(new Equal("a", 1));
        Equal cond = new Equal("b", 2);
        And result = or.and(cond);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
        assertEquals(Operator.AND, result.operator());
    }

    @Test
    public void testNotMethod() {
        Or or = new Or(new Equal("a", 1), new Equal("b", 2));
        Not result = or.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        Or innerCondition = result.getCondition();
        assertEquals(2, (int) innerCondition.getConditions().size());
    }

    @Test
    public void testToString_NoArgs() {
        Or or = new Or(new Equal("status", "active"), new Equal("status", "pending"));
        String result = or.toString();

        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status"));
    }

    @Test
    public void testConstructor_NullConditionInArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Or(new Equal("a", 1), null, new Equal("b", 2));
        });
    }

    @Test
    public void testGetOperator() {
        Or or = new Or(new Equal("a", 1));
        assertEquals(Operator.OR, or.operator());
    }

    @Test
    public void testGetOperator_EmptyConstructor() {
        // Empty constructor doesn't set operator (for Kryo serialization)
        Or or = new Or();
        assertNull(or.operator());
    }

    @Test
    public void testConstructor_NullConditionInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            new Or(conditions);
        });
    }

}

public class OrTest extends TestBase {

    @Test
    public void testConstructorWithVarArgs() {
        Equal eq1 = Filters.eq("status", "active");
        Equal eq2 = Filters.eq("status", "pending");
        Equal eq3 = Filters.eq("status", "review");

        Or or = Filters.or(eq1, eq2, eq3);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.operator());
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testConstructorWithEmptyVarArgs() {
        Or or = Filters.or();

        Assertions.assertNotNull(or);
        Assertions.assertEquals(0, or.getConditions().size());
    }

    @Test
    public void testConstructorWithCollection() {
        List<Condition> conditions = Arrays.asList(Filters.like("name", "John%"), Filters.like("name", "Jane%"));

        Or or = Filters.or(conditions);

        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testOrMethodWithSingleCondition() {
        Or or = Filters.or(Filters.eq("type", "A"));
        Or result = or.or(Filters.eq("type", "B"));

        Assertions.assertNotSame(or, result);
        Assertions.assertEquals(2, result.getConditions().size());
    }

    @Test
    public void testOrMethodChaining() {
        Or or = Filters.or(Filters.eq("type", "A")).or(Filters.eq("type", "B")).or(Filters.eq("type", "C"));

        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testGetConditionList() {
        Equal eq1 = Filters.eq("status", "active");
        Equal eq2 = Filters.eq("status", "pending");

        Or or = Filters.or(eq1, eq2);
        List<Condition> conditions = or.getConditions();

        Assertions.assertEquals(2, conditions.size());
        Assertions.assertTrue(conditions.contains(eq1));
        Assertions.assertTrue(conditions.contains(eq2));
    }

    @Test
    public void testGetParameters() {
        Or or = Filters.or(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.like("name", "%John%"));

        List<Object> params = or.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(18));
        Assertions.assertTrue(params.contains("%John%"));
    }

    @Test
    public void testClearParameters() {
        Or or = Filters.or(Filters.eq("status", "active"), Filters.in("id", Arrays.asList(1, 2, 3)));

        or.clearParameters();

        List<Object> params = or.getParameters();
        Assertions.assertTrue(params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        Or or = Filters.or(Filters.eq("city", "New York"), Filters.eq("city", "Los Angeles"), Filters.eq("city", "Chicago"));

        String result = or.toString();
        Assertions.assertTrue(result.contains("OR"));
        Assertions.assertTrue(result.contains("city"));
        Assertions.assertTrue(result.contains("New York"));
        Assertions.assertTrue(result.contains("Los Angeles"));
        Assertions.assertTrue(result.contains("Chicago"));
    }

    @Test
    public void testHashCode() {
        Or or1 = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));
        Or or2 = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));

        Assertions.assertEquals(or1.hashCode(), or2.hashCode());
    }

    @Test
    public void testEquals() {
        Or or1 = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));
        Or or2 = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));
        Or or3 = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "inactive"));

        Assertions.assertTrue(or1.equals(or1));
        Assertions.assertTrue(or1.equals(or2));
        Assertions.assertFalse(or1.equals(or3));
        Assertions.assertFalse(or1.equals(null));
        Assertions.assertFalse(or1.equals("not an Or"));
    }

    @Test
    public void testComplexOrConditions() {
        Or or = Filters.or(Filters.and(Filters.eq("category", "electronics"), Filters.gt("price", 100)),
                Filters.and(Filters.eq("category", "books"), Filters.gt("price", 50)), Filters.eq("featured", true));

        Assertions.assertEquals(3, or.getConditions().size());
        Assertions.assertTrue(or.getParameters().size() >= 5);
    }

    @Test
    public void testAndMethod() {
        Or or = Filters.or(Filters.eq("status", "active"));
        And and = or.and(Filters.eq("type", "user"));
        Assertions.assertEquals(Operator.AND, and.operator());
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testNotMethod() {
        Or or = Filters.or(Filters.eq("status", "active"));
        Not not = or.not();
        Assertions.assertEquals(Operator.NOT, not.operator());
        Assertions.assertEquals(or, not.getCondition());
    }
}
