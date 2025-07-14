package com.landawn.abacus.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class OperatorTest extends TestBase {

    @Test
    public void testGetName() {
        Assertions.assertEquals("=", Operator.EQUAL.getName());
        Assertions.assertEquals("!=", Operator.NOT_EQUAL.getName());
        Assertions.assertEquals("AND", Operator.AND.getName());
        Assertions.assertEquals("OR", Operator.OR.getName());
        Assertions.assertEquals("BETWEEN", Operator.BETWEEN.getName());
        Assertions.assertEquals("LIKE", Operator.LIKE.getName());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("=", Operator.EQUAL.toString());
        Assertions.assertEquals(">", Operator.GREATER_THAN.toString());
        Assertions.assertEquals("<=", Operator.LESS_EQUAL.toString());
        Assertions.assertEquals("IN", Operator.IN.toString());
        Assertions.assertEquals("NOT IN", Operator.NOT_IN.toString());
    }

    @Test
    public void testGetOperatorWithExactMatch() {
        Assertions.assertEquals(Operator.EQUAL, Operator.getOperator("="));
        Assertions.assertEquals(Operator.NOT_EQUAL, Operator.getOperator("!="));
        Assertions.assertEquals(Operator.AND, Operator.getOperator("AND"));
        Assertions.assertEquals(Operator.OR, Operator.getOperator("OR"));
        Assertions.assertEquals(Operator.LIKE, Operator.getOperator("LIKE"));
    }

    @Test
    public void testGetOperatorCaseInsensitive() {
        Assertions.assertEquals(Operator.AND, Operator.getOperator("and"));
        Assertions.assertEquals(Operator.OR, Operator.getOperator("or"));
        Assertions.assertEquals(Operator.LIKE, Operator.getOperator("like"));
        Assertions.assertEquals(Operator.BETWEEN, Operator.getOperator("between"));
        Assertions.assertEquals(Operator.NOT_IN, Operator.getOperator("not in"));
    }

    @Test
    public void testGetOperatorWithSymbols() {
        Assertions.assertEquals(Operator.GREATER_THAN, Operator.getOperator(">"));
        Assertions.assertEquals(Operator.LESS_THAN, Operator.getOperator("<"));
        Assertions.assertEquals(Operator.GREATER_EQUAL, Operator.getOperator(">="));
        Assertions.assertEquals(Operator.LESS_EQUAL, Operator.getOperator("<="));
        Assertions.assertEquals(Operator.NOT_EQUAL2, Operator.getOperator("<>"));
    }

    @Test
    public void testGetOperatorWithAlternativeSymbols() {
        Assertions.assertEquals(Operator.AND_OP, Operator.getOperator("&&"));
        Assertions.assertEquals(Operator.OR_OP, Operator.getOperator("||"));
        Assertions.assertEquals(Operator.NOT_OP, Operator.getOperator("!"));
    }

    @Test
    public void testGetOperatorWithCompoundOperators() {
        Assertions.assertEquals(Operator.NOT_LIKE, Operator.getOperator("NOT LIKE"));
        Assertions.assertEquals(Operator.NOT_BETWEEN, Operator.getOperator("NOT BETWEEN"));
        Assertions.assertEquals(Operator.IS_NOT, Operator.getOperator("IS NOT"));
        Assertions.assertEquals(Operator.LEFT_JOIN, Operator.getOperator("LEFT JOIN"));
        Assertions.assertEquals(Operator.UNION_ALL, Operator.getOperator("UNION ALL"));
    }

    @Test
    public void testGetOperatorWithUnknown() {
        Assertions.assertNull(Operator.getOperator("UNKNOWN_OPERATOR"));
        Assertions.assertNull(Operator.getOperator("???"));
    }

    @Test
    public void testGetOperatorCaching() {
        // First call should cache the result
        Operator op1 = Operator.getOperator("and");
        Assertions.assertEquals(Operator.AND, op1);

        // Second call should return cached result
        Operator op2 = Operator.getOperator("and");
        Assertions.assertEquals(Operator.AND, op2);

        // Verify caching works for different cases
        Operator op3 = Operator.getOperator("AND");
        Assertions.assertEquals(Operator.AND, op3);
    }

    @Test
    public void testAllOperatorValues() {
        // Test that all operators have non-empty names
        for (Operator op : Operator.values()) {
            Assertions.assertNotNull(op.getName());
            Assertions.assertNotNull(op.toString());
        }
    }

    @Test
    public void testSpecificOperators() {
        // Comparison operators
        Assertions.assertEquals("=", Operator.EQUAL.getName());
        Assertions.assertEquals("!=", Operator.NOT_EQUAL.getName());
        Assertions.assertEquals("<>", Operator.NOT_EQUAL2.getName());
        Assertions.assertEquals(">", Operator.GREATER_THAN.getName());
        Assertions.assertEquals(">=", Operator.GREATER_EQUAL.getName());
        Assertions.assertEquals("<", Operator.LESS_THAN.getName());
        Assertions.assertEquals("<=", Operator.LESS_EQUAL.getName());

        // Logical operators
        Assertions.assertEquals("AND", Operator.AND.getName());
        Assertions.assertEquals("OR", Operator.OR.getName());
        Assertions.assertEquals("NOT", Operator.NOT.getName());
        Assertions.assertEquals("XOR", Operator.XOR.getName());

        // Range/Set operators
        Assertions.assertEquals("BETWEEN", Operator.BETWEEN.getName());
        Assertions.assertEquals("NOT BETWEEN", Operator.NOT_BETWEEN.getName());
        Assertions.assertEquals("IN", Operator.IN.getName());
        Assertions.assertEquals("NOT IN", Operator.NOT_IN.getName());
        Assertions.assertEquals("LIKE", Operator.LIKE.getName());
        Assertions.assertEquals("NOT LIKE", Operator.NOT_LIKE.getName());

        // Join operators
        Assertions.assertEquals("JOIN", Operator.JOIN.getName());
        Assertions.assertEquals("LEFT JOIN", Operator.LEFT_JOIN.getName());
        Assertions.assertEquals("RIGHT JOIN", Operator.RIGHT_JOIN.getName());
        Assertions.assertEquals("FULL JOIN", Operator.FULL_JOIN.getName());
        Assertions.assertEquals("CROSS JOIN", Operator.CROSS_JOIN.getName());
        Assertions.assertEquals("INNER JOIN", Operator.INNER_JOIN.getName());
        Assertions.assertEquals("NATURAL JOIN", Operator.NATURAL_JOIN.getName());

        // Clauses
        Assertions.assertEquals("WHERE", Operator.WHERE.getName());
        Assertions.assertEquals("HAVING", Operator.HAVING.getName());
        Assertions.assertEquals("GROUP BY", Operator.GROUP_BY.getName());
        Assertions.assertEquals("ORDER BY", Operator.ORDER_BY.getName());
        Assertions.assertEquals("LIMIT", Operator.LIMIT.getName());
        Assertions.assertEquals("OFFSET", Operator.OFFSET.getName());

        // Set operations
        Assertions.assertEquals("UNION", Operator.UNION.getName());
        Assertions.assertEquals("UNION ALL", Operator.UNION_ALL.getName());
        Assertions.assertEquals("INTERSECT", Operator.INTERSECT.getName());
        Assertions.assertEquals("EXCEPT", Operator.EXCEPT.getName());
        Assertions.assertEquals("MINUS", Operator.MINUS.getName());

        // Special operators
        Assertions.assertEquals("", Operator.EMPTY.getName());
        Assertions.assertEquals("IS", Operator.IS.getName());
        Assertions.assertEquals("IS NOT", Operator.IS_NOT.getName());
        Assertions.assertEquals("EXISTS", Operator.EXISTS.getName());
        Assertions.assertEquals("ANY", Operator.ANY.getName());
        Assertions.assertEquals("SOME", Operator.SOME.getName());
        Assertions.assertEquals("ALL", Operator.ALL.getName());
        Assertions.assertEquals("ON", Operator.ON.getName());
        Assertions.assertEquals("USING", Operator.USING.getName());
    }

    @Test
    public void testOperatorUniqueness() {
        // Verify that operator names are unique (except for intentional duplicates)
        Operator[] operators = Operator.values();
        for (int i = 0; i < operators.length; i++) {
            for (int j = i + 1; j < operators.length; j++) {
                if (operators[i].getName().equals(operators[j].getName())) {
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