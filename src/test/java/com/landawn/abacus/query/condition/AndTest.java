package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class AndTest extends TestBase {

    @Test
    public void testConstructorWithVarargs() {
        Equal eq1 = Filters.eq("status", "active");
        GreaterThan gt = Filters.gt("age", 18);

        And and = Filters.and(eq1, gt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.getOperator());
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
        Assertions.assertThrows(NullPointerException.class, () -> {
            Filters.and((Collection<Condition>) null);
        });
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

        String result = and.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
    public void testClearParameters() {
        And and = Filters.and(Filters.eq("id", 100), Filters.ne("status", "deleted"));

        and.clearParameters();

        List<Object> params = and.getParameters();
        Assertions.assertEquals(2, params.size());
    }

    @Test
    public void testCopy() {
        And original = Filters.and(Filters.eq("active", true), Filters.gt("score", 80));

        And copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getConditions().size(), copy.getConditions().size());
        Assertions.assertNotSame(original.getConditions(), copy.getConditions());
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
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        And and = Filters.and(Filters.eq("available", true), Filters.gt("stock", 0));

        Not not = and.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.getOperator());
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