package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Clause2025Test extends TestBase {

    private static class TestClause extends Clause {
        public TestClause(Operator operator, Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testConstructor() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        assertNotNull(clause);
        assertNotNull(clause.operator());
        assertNotNull(clause.getCondition());
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        Condition retrieved = clause.getCondition();

        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Condition condition = Filters.between("age", 18, 65);
        TestClause clause = new TestClause(Operator.WHERE, condition);

        java.util.List<Object> params = clause.getParameters();

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals(18, params.get(0));
        assertEquals(65, params.get(1));
    }

    @Test
    public void testClearParameters() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        assertFalse(clause.getParameters().isEmpty());

        clause.clearParameters();

        java.util.List<Object> params = clause.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testToString_WithNamingPolicy() {
        Condition condition = Filters.eq("userName", "John");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        String result = clause.toString(com.landawn.abacus.util.NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testToString_DefaultNamingPolicy() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.HAVING, condition);

        String result = clause.toString();

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testHashCode() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause1 = new TestClause(Operator.WHERE, condition);
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("test", "value"));

        assertEquals(clause1.hashCode(), clause2.hashCode());
    }

    @Test
    public void testEquals_SameInstance() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertEquals(clause, clause);
    }

    @Test
    public void testEquals_EqualClauses() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));

        assertEquals(clause1, clause2);
    }

    @Test
    public void testEquals_DifferentOperator() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.HAVING, Filters.eq("a", 1));

        assertNotEquals(clause1, clause2);
    }

    @Test
    public void testEquals_DifferentCondition() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("b", 2));

        assertNotEquals(clause1, clause2);
    }

    @Test
    public void testEquals_Null() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, null);
    }

    @Test
    public void testEquals_DifferentType() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, "not a clause");
    }

    @Test
    public void testGetOperator() {
        TestClause clause = new TestClause(Operator.GROUP_BY, Filters.eq("test", "value"));
        assertEquals(Operator.GROUP_BY, clause.operator());
    }

    @Test
    public void testWithComplexCondition() {
        Condition complex = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.isNotNull("email"));
        TestClause clause = new TestClause(Operator.WHERE, complex);

        java.util.List<Object> params = clause.getParameters();
        assertEquals(2, params.size());
    }

}

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
