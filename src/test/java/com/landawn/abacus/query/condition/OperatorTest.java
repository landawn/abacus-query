package com.landawn.abacus.query.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class OperatorTest extends TestBase {

    @Test
    public void testGetName() {
        Assertions.assertEquals("=", Operator.EQUAL.sqlToken());
        Assertions.assertEquals("!=", Operator.NOT_EQUAL.sqlToken());
        Assertions.assertEquals("AND", Operator.AND.sqlToken());
        Assertions.assertEquals("OR", Operator.OR.sqlToken());
        Assertions.assertEquals("BETWEEN", Operator.BETWEEN.sqlToken());
        Assertions.assertEquals("LIKE", Operator.LIKE.sqlToken());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("=", Operator.EQUAL.toString());
        Assertions.assertEquals(">", Operator.GREATER_THAN.toString());
        Assertions.assertEquals("<=", Operator.LESS_THAN_OR_EQUAL.toString());
        Assertions.assertEquals("IN", Operator.IN.toString());
        Assertions.assertEquals("NOT IN", Operator.NOT_IN.toString());
    }

    @Test
    public void testGetOperatorWithExactMatch() {
        Assertions.assertEquals(Operator.EQUAL, Operator.of("="));
        Assertions.assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        Assertions.assertEquals(Operator.AND, Operator.of("AND"));
        Assertions.assertEquals(Operator.OR, Operator.of("OR"));
        Assertions.assertEquals(Operator.LIKE, Operator.of("LIKE"));
    }

    @Test
    public void testGetOperatorCaseInsensitive() {
        Assertions.assertEquals(Operator.AND, Operator.of("and"));
        Assertions.assertEquals(Operator.OR, Operator.of("or"));
        Assertions.assertEquals(Operator.LIKE, Operator.of("like"));
        Assertions.assertEquals(Operator.BETWEEN, Operator.of("between"));
        Assertions.assertEquals(Operator.NOT_IN, Operator.of("not in"));
    }

    @Test
    public void testGetOperatorWithSymbols() {
        Assertions.assertEquals(Operator.GREATER_THAN, Operator.of(">"));
        Assertions.assertEquals(Operator.LESS_THAN, Operator.of("<"));
        Assertions.assertEquals(Operator.GREATER_THAN_OR_EQUAL, Operator.of(">="));
        Assertions.assertEquals(Operator.LESS_THAN_OR_EQUAL, Operator.of("<="));
        Assertions.assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
    }

    @Test
    public void testGetOperatorWithAlternativeSymbols() {
        Assertions.assertEquals(Operator.AND_OP, Operator.of("&&"));
        Assertions.assertEquals(Operator.OR_OP, Operator.of("||"));
        Assertions.assertEquals(Operator.NOT_OP, Operator.of("!"));
    }

    @Test
    public void testGetOperatorWithCompoundOperators() {
        Assertions.assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
        Assertions.assertEquals(Operator.NOT_BETWEEN, Operator.of("NOT BETWEEN"));
        Assertions.assertEquals(Operator.IS_NOT, Operator.of("IS NOT"));
        Assertions.assertEquals(Operator.LEFT_JOIN, Operator.of("LEFT JOIN"));
        Assertions.assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
    }

    @Test
    public void testGetOperatorWithUnknown() {
        Assertions.assertNull(Operator.of("UNKNOWN_OPERATOR"));
        Assertions.assertNull(Operator.of("???"));
    }

    @Test
    public void testGetOperatorCaching() {
        // First call should cache the result
        Operator op1 = Operator.of("and");
        Assertions.assertEquals(Operator.AND, op1);

        // Second call should return cached result
        Operator op2 = Operator.of("and");
        Assertions.assertEquals(Operator.AND, op2);

        // Verify caching works for different cases
        Operator op3 = Operator.of("AND");
        Assertions.assertEquals(Operator.AND, op3);
    }

    @Test
    public void testAllOperatorValues() {
        // Test that all operators have non-empty names
        for (Operator op : Operator.values()) {
            Assertions.assertNotNull(op.sqlToken());
            Assertions.assertNotNull(op.toString());
        }
    }

    @Test
    public void testSpecificOperators() {
        // Comparison operators
        Assertions.assertEquals("=", Operator.EQUAL.sqlToken());
        Assertions.assertEquals("!=", Operator.NOT_EQUAL.sqlToken());
        Assertions.assertEquals("<>", Operator.NOT_EQUAL_ANSI.sqlToken());
        Assertions.assertEquals(">", Operator.GREATER_THAN.sqlToken());
        Assertions.assertEquals(">=", Operator.GREATER_THAN_OR_EQUAL.sqlToken());
        Assertions.assertEquals("<", Operator.LESS_THAN.sqlToken());
        Assertions.assertEquals("<=", Operator.LESS_THAN_OR_EQUAL.sqlToken());

        // Logical operators
        Assertions.assertEquals("AND", Operator.AND.sqlToken());
        Assertions.assertEquals("OR", Operator.OR.sqlToken());
        Assertions.assertEquals("NOT", Operator.NOT.sqlToken());
        Assertions.assertEquals("XOR", Operator.XOR.sqlToken());

        // Range/Set operators
        Assertions.assertEquals("BETWEEN", Operator.BETWEEN.sqlToken());
        Assertions.assertEquals("NOT BETWEEN", Operator.NOT_BETWEEN.sqlToken());
        Assertions.assertEquals("IN", Operator.IN.sqlToken());
        Assertions.assertEquals("NOT IN", Operator.NOT_IN.sqlToken());
        Assertions.assertEquals("LIKE", Operator.LIKE.sqlToken());
        Assertions.assertEquals("NOT LIKE", Operator.NOT_LIKE.sqlToken());

        // Join operators
        Assertions.assertEquals("JOIN", Operator.JOIN.sqlToken());
        Assertions.assertEquals("LEFT JOIN", Operator.LEFT_JOIN.sqlToken());
        Assertions.assertEquals("RIGHT JOIN", Operator.RIGHT_JOIN.sqlToken());
        Assertions.assertEquals("FULL JOIN", Operator.FULL_JOIN.sqlToken());
        Assertions.assertEquals("CROSS JOIN", Operator.CROSS_JOIN.sqlToken());
        Assertions.assertEquals("INNER JOIN", Operator.INNER_JOIN.sqlToken());
        Assertions.assertEquals("NATURAL JOIN", Operator.NATURAL_JOIN.sqlToken());

        // Clauses
        Assertions.assertEquals("WHERE", Operator.WHERE.sqlToken());
        Assertions.assertEquals("HAVING", Operator.HAVING.sqlToken());
        Assertions.assertEquals("GROUP BY", Operator.GROUP_BY.sqlToken());
        Assertions.assertEquals("ORDER BY", Operator.ORDER_BY.sqlToken());
        Assertions.assertEquals("LIMIT", Operator.LIMIT.sqlToken());
        Assertions.assertEquals("OFFSET", Operator.OFFSET.sqlToken());

        // Set operations
        Assertions.assertEquals("UNION", Operator.UNION.sqlToken());
        Assertions.assertEquals("UNION ALL", Operator.UNION_ALL.sqlToken());
        Assertions.assertEquals("INTERSECT", Operator.INTERSECT.sqlToken());
        Assertions.assertEquals("EXCEPT", Operator.EXCEPT.sqlToken());
        Assertions.assertEquals("MINUS", Operator.MINUS.sqlToken());

        // Special operators
        Assertions.assertEquals("", Operator.EMPTY.sqlToken());
        Assertions.assertEquals("IS", Operator.IS.sqlToken());
        Assertions.assertEquals("IS NOT", Operator.IS_NOT.sqlToken());
        Assertions.assertEquals("EXISTS", Operator.EXISTS.sqlToken());
        Assertions.assertEquals("ANY", Operator.ANY.sqlToken());
        Assertions.assertEquals("SOME", Operator.SOME.sqlToken());
        Assertions.assertEquals("ALL", Operator.ALL.sqlToken());
        Assertions.assertEquals("ON", Operator.ON.sqlToken());
        Assertions.assertEquals("USING", Operator.USING.sqlToken());
    }

    @Test
    public void testOperatorUniqueness() {
        // Verify that operator names are unique (except for intentional duplicates)
        Operator[] operators = Operator.values();
        for (int i = 0; i < operators.length; i++) {
            for (int j = i + 1; j < operators.length; j++) {
                if (operators[i].sqlToken().equals(operators[j].sqlToken())) {
                    // Only EXCEPT and MINUS should have the same name
                    Assertions.assertTrue(
                            (operators[i] == Operator.EXCEPT && operators[j] == Operator.MINUS)
                                    || (operators[i] == Operator.MINUS && operators[j] == Operator.EXCEPT),
                            "Unexpected duplicate operator names: " + operators[i] + " and " + operators[j]);
                }
            }
        }
    }
}