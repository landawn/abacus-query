package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class ClauseTest extends TestBase {

    // Create a concrete Clause subclass for testing
    private static class TestClause extends Clause {
        public TestClause(Operator operator, Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testConstructor() {
        Equal eq = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, eq);

        Assertions.assertNotNull(clause);
        Assertions.assertEquals(Operator.WHERE, clause.operator());
        Assertions.assertEquals(eq, clause.getCondition());
    }

    @Test
    public void testInheritedMethodsFromCell() {
        GreaterThan gt = Filters.gt("price", 100);
        TestClause clause = new TestClause(Operator.WHERE, gt);

        // Test getCondition
        Condition retrieved = clause.getCondition();
        Assertions.assertEquals(gt, retrieved);

        // Test getParameters
        var params = clause.getParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(100, params.get(0));

        // Test clearParameters
        clause.clearParameters();
        Assertions.assertNull(clause.getCondition().getParameters().get(0));
    }

    @Test
    public void testCopy() {
        Between between = Filters.between("age", 18, 65);
        TestClause original = new TestClause(Operator.WHERE, between);

        TestClause copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.operator(), copy.operator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        Equal eq = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, eq);

        String result = clause.toString();
        Assertions.assertTrue(result.contains("WHERE"));
        Assertions.assertTrue(result.contains("status = 'active'"));
    }

    @Test
    public void testEquals() {
        Equal eq1 = Filters.eq("id", 1);
        Equal eq2 = Filters.eq("id", 1);

        TestClause clause1 = new TestClause(Operator.WHERE, eq1);
        TestClause clause2 = new TestClause(Operator.WHERE, eq2);
        TestClause clause3 = new TestClause(Operator.HAVING, eq1);

        Assertions.assertEquals(clause1, clause2);
        Assertions.assertNotEquals(clause1, clause3);
    }

    @Test
    public void testHashCode() {
        In in = Filters.in("type", Arrays.asList("A", "B", "C"));
        TestClause clause1 = new TestClause(Operator.WHERE, in);
        TestClause clause2 = new TestClause(Operator.WHERE, in);

        Assertions.assertEquals(clause1.hashCode(), clause2.hashCode());
    }
}