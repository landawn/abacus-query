package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

public class ClauseTest extends TestBase {

    @Test
    void testConstructor() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        assertNotNull(clause);
        assertNotNull(clause.operator());
        assertNotNull(clause.condition());
    }

    @Test
    void testConstructor_NullOperatorThrowsIllegalArgumentException() {
        // A null operator is an IllegalArgumentException (previously threw NullPointerException).
        assertThrows(IllegalArgumentException.class, () -> new TestClause(null, Filters.eq("test", "value")));
    }

    @Test
    void testConstructorRejectsClauseLikeConnectorAndEmptyExpressions() {
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("WHERE a = 1")));
        assertThrows(IllegalArgumentException.class, () -> new Having(Filters.expr("HAVING COUNT(*) > 1")));
        assertThrows(IllegalArgumentException.class, () -> new GroupBy(Filters.expr("GROUP BY name")));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(Filters.expr("ORDER BY name")));
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("ON a.id = b.id")));
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("")));
        assertThrows(IllegalArgumentException.class, () -> new Where(new And()));
    }

    @Test
    void testConstructorRejectsJoinOperatorsAndInvalidSetOperands() {
        final Condition predicate = Filters.eq("id", 1);

        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.JOIN, predicate));
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.LEFT_JOIN, predicate));
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.UNION, predicate));
        assertDoesNotThrow(() -> new TestClause(Operator.UNION, Filters.subQuery("SELECT id FROM archived_users")));
    }

    @Test
    void testConstructorRejectsNestedClauses() {
        assertThrows(IllegalArgumentException.class, () -> new Where(new OrderBy("name")));
        assertThrows(IllegalArgumentException.class, () -> new Having(new Where(Filters.eq("a", 1))));
    }

    @Test
    void testConstructorRejectsNonClauseOperator() {
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.EQUAL, Filters.eq("id", 1)));
    }

    @Test
    void testConstructorRejectsOtherWrappedNonPredicateComponents() {
        final SubQuery subQuery = new SubQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.WHERE, new TestComposableWrapper(new OrderBy("name"))));
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.WHERE, new TestComposableWrapper(new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.WHERE, new TestComposableWrapper(new SqlExpression(" "))));
        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.WHERE, new Equal()));
        assertDoesNotThrow(() -> new TestClause(Operator.WHERE, new TestComposableWrapper(new Equal("id", 1))));
    }

    @Test
    void testConstructorRejectsWrappedOnConnector() {
        final Condition wrappedOn = new TestComposableWrapper(new On("orders.customer_id", "customers.id"));

        assertThrows(IllegalArgumentException.class, () -> new TestClause(Operator.WHERE, wrappedOn));
    }

    @Test
    void testEquals() {
        Equal eq1 = Filters.eq("id", 1);
        Equal eq2 = Filters.eq("id", 1);

        TestClause clause1 = new TestClause(Operator.WHERE, eq1);
        TestClause clause2 = new TestClause(Operator.WHERE, eq2);
        TestClause clause3 = new TestClause(Operator.HAVING, eq1);

        assertEquals(clause1, clause2);
        assertNotEquals(clause1, clause3);
    }

    @Test
    void testEquals_DifferentCondition() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("b", 2));

        assertNotEquals(clause1, clause2);
    }

    @Test
    void testEquals_DifferentOperator() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.HAVING, Filters.eq("a", 1));

        assertNotEquals(clause1, clause2);
    }

    @Test
    void testEquals_DifferentType() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, "not a clause");
    }

    @Test
    void testEquals_EqualClauses() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));

        assertEquals(clause1, clause2);
    }

    @Test
    void testEquals_Null() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, null);
    }

    @Test
    void testEquals_SameInstance() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertEquals(clause, clause);
    }

    @Test
    void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        Condition retrieved = clause.condition();

        assertEquals(condition, retrieved);
    }

    @Test
    void testGetOperator() {
        TestClause clause = new TestClause(Operator.GROUP_BY, Filters.eq("test", "value"));
        assertEquals(Operator.GROUP_BY, clause.operator());
    }

    @Test
    void testHashCode() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause1 = new TestClause(Operator.WHERE, condition);
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("test", "value"));

        assertEquals(clause1.hashCode(), clause2.hashCode());
    }

    @Test
    void testInheritedMethodsFromCell() {
        GreaterThan gt = Filters.gt("price", 100);
        TestClause clause = new TestClause(Operator.WHERE, gt);

        // Test condition
        Condition retrieved = clause.condition();
        assertEquals(gt, retrieved);

        // Test parameters
        var params = clause.parameters();
        assertEquals(1, params.size());
        assertEquals(100, params.get(0));
    }

    @Test
    void testParameters() {
        Condition condition = Filters.between("age", 18, 65);
        TestClause clause = new TestClause(Operator.WHERE, condition);

        java.util.List<Object> params = clause.parameters();

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals(18, params.get(0));
        assertEquals(65, params.get(1));
    }

    @Test
    void testToString() {
        Equal eq = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, eq);

        String result = clause.toString();
        assertTrue(result.contains("WHERE"));
        assertTrue(result.contains("status = 'active'"));
    }

    @Test
    void testToString_DefaultNamingPolicy() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.HAVING, condition);

        String result = clause.toString();

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    void testToString_WithNamingPolicy() {
        Condition condition = Filters.eq("userName", "John");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        String result = clause.toSql(com.landawn.abacus.util.NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    void testWithComplexCondition() {
        Condition complex = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.isNotNull("email"));
        TestClause clause = new TestClause(Operator.WHERE, complex);

        java.util.List<Object> params = clause.parameters();
        assertEquals(2, params.size());
    }

    private static class TestClause extends Clause {
        public TestClause(Operator operator, Condition condition) {
            super(operator, condition);
        }
    }

    private static final class TestComposableWrapper extends ComposableCell {
        TestComposableWrapper(final Condition condition) {
            super(Operator.NOT, condition);
        }
    }
}
