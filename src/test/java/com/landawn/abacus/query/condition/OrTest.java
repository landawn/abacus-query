package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.Filters.CF;

public class OrTest extends TestBase {

    @Test
    public void testConstructorWithVarArgs() {
        Equal eq1 = CF.eq("status", "active");
        Equal eq2 = CF.eq("status", "pending");
        Equal eq3 = CF.eq("status", "review");

        Or or = CF.or(eq1, eq2, eq3);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testConstructorWithEmptyVarArgs() {
        Or or = CF.or();

        Assertions.assertNotNull(or);
        Assertions.assertEquals(0, or.getConditions().size());
    }

    @Test
    public void testConstructorWithCollection() {
        List<Condition> conditions = Arrays.asList(CF.like("name", "John%"), CF.like("name", "Jane%"));

        Or or = CF.or(conditions);

        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testOrMethodWithSingleCondition() {
        Or or = CF.or(CF.eq("type", "A"));
        Or result = or.or(CF.eq("type", "B"));

        Assertions.assertNotSame(or, result);
        Assertions.assertEquals(2, result.getConditions().size());
    }

    @Test
    public void testOrMethodChaining() {
        Or or = CF.or(CF.eq("type", "A")).or(CF.eq("type", "B")).or(CF.eq("type", "C"));

        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testGetConditionList() {
        Equal eq1 = CF.eq("status", "active");
        Equal eq2 = CF.eq("status", "pending");

        Or or = CF.or(eq1, eq2);
        List<Condition> conditions = or.getConditions();

        Assertions.assertEquals(2, conditions.size());
        Assertions.assertTrue(conditions.contains(eq1));
        Assertions.assertTrue(conditions.contains(eq2));
    }

    @Test
    public void testGetParameters() {
        Or or = CF.or(CF.eq("status", "active"), CF.gt("age", 18), CF.like("name", "%John%"));

        List<Object> params = or.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("active"));
        Assertions.assertTrue(params.contains(18));
        Assertions.assertTrue(params.contains("%John%"));
    }

    @Test
    public void testClearParameters() {
        Or or = CF.or(CF.eq("status", "active"), CF.in("id", Arrays.asList(1, 2, 3)));

        or.clearParameters();

        List<Object> params = or.getParameters();
        Assertions.assertTrue(params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testToString() {
        Or or = CF.or(CF.eq("city", "New York"), CF.eq("city", "Los Angeles"), CF.eq("city", "Chicago"));

        String result = or.toString();
        Assertions.assertTrue(result.contains("OR"));
        Assertions.assertTrue(result.contains("city"));
        Assertions.assertTrue(result.contains("New York"));
        Assertions.assertTrue(result.contains("Los Angeles"));
        Assertions.assertTrue(result.contains("Chicago"));
    }

    @Test
    public void testCopy() {
        Or original = CF.or(CF.eq("status", "active"), CF.gt("age", 18));

        Or copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());
    }

    @Test
    public void testHashCode() {
        Or or1 = CF.or(CF.eq("status", "active"), CF.eq("status", "pending"));
        Or or2 = CF.or(CF.eq("status", "active"), CF.eq("status", "pending"));

        Assertions.assertEquals(or1.hashCode(), or2.hashCode());
    }

    @Test
    public void testEquals() {
        Or or1 = CF.or(CF.eq("status", "active"), CF.eq("status", "pending"));
        Or or2 = CF.or(CF.eq("status", "active"), CF.eq("status", "pending"));
        Or or3 = CF.or(CF.eq("status", "active"), CF.eq("status", "inactive"));

        Assertions.assertTrue(or1.equals(or1));
        Assertions.assertTrue(or1.equals(or2));
        Assertions.assertFalse(or1.equals(or3));
        Assertions.assertFalse(or1.equals(null));
        Assertions.assertFalse(or1.equals("not an Or"));
    }

    @Test
    public void testComplexOrConditions() {
        Or or = CF.or(CF.and(CF.eq("category", "electronics"), CF.gt("price", 100)), CF.and(CF.eq("category", "books"), CF.gt("price", 50)),
                CF.eq("featured", true));

        Assertions.assertEquals(3, or.getConditions().size());
        Assertions.assertTrue(or.getParameters().size() >= 5);
    }

    @Test
    public void testAddMethod() {
        Or or = CF.or();
        or.add(CF.eq("status", "active"));
        or.add(CF.eq("status", "pending"));

        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAndMethod() {
        Or or = CF.or(CF.eq("status", "active"));
        or.and(CF.eq("type", "user"));
    }

    @Test
    public void testNotMethod() {
        Or or = CF.or(CF.eq("status", "active"));
        or.not();
    }
}