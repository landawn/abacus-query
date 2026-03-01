package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

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
    public void testCopy() {
        Or original = Filters.or(Filters.eq("status", "active"), Filters.gt("age", 18));

        Or copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());
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
    public void testAddMethod() {
        Or or = Filters.or();
        or.add(Filters.eq("status", "active"));
        or.add(Filters.eq("status", "pending"));

        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAndMethod() {
        Or or = Filters.or(Filters.eq("status", "active"));
        or.and(Filters.eq("type", "user"));
    }

    @Test
    public void testNotMethod() {
        Or or = Filters.or(Filters.eq("status", "active"));
        or.not();
    }
}