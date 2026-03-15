package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Operator2025Test extends TestBase {

    @Test
    public void testGetOperator_Comparison() {
        assertEquals(Operator.EQUAL, Operator.of("="));
        assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
        assertEquals(Operator.GREATER_THAN, Operator.of(">"));
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Operator.of(">="));
        assertEquals(Operator.LESS_THAN, Operator.of("<"));
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Operator.of("<="));
    }

    @Test
    public void testGetOperator_Composable() {
        assertEquals(Operator.AND, Operator.of("AND"));
        assertEquals(Operator.AND_OP, Operator.of("&&"));
        assertEquals(Operator.OR, Operator.of("OR"));
        assertEquals(Operator.OR_OP, Operator.of("||"));
        assertEquals(Operator.NOT, Operator.of("NOT"));
        assertEquals(Operator.NOT_OP, Operator.of("!"));
        assertEquals(Operator.XOR, Operator.of("XOR"));
    }

    @Test
    public void testGetOperator_Range() {
        assertEquals(Operator.BETWEEN, Operator.of("BETWEEN"));
        assertEquals(Operator.NOT_BETWEEN, Operator.of("NOT BETWEEN"));
        assertEquals(Operator.IN, Operator.of("IN"));
        assertEquals(Operator.NOT_IN, Operator.of("NOT IN"));
    }

    @Test
    public void testGetOperator_Pattern() {
        assertEquals(Operator.LIKE, Operator.of("LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
    }

    @Test
    public void testGetOperator_Null() {
        assertEquals(Operator.IS, Operator.of("IS"));
        assertEquals(Operator.IS_NOT, Operator.of("IS NOT"));
    }

    @Test
    public void testGetOperator_Subquery() {
        assertEquals(Operator.EXISTS, Operator.of("EXISTS"));
        assertEquals(Operator.NOT_EXISTS, Operator.of("NOT EXISTS"));
        assertEquals(Operator.ANY, Operator.of("ANY"));
        assertEquals(Operator.SOME, Operator.of("SOME"));
        assertEquals(Operator.ALL, Operator.of("ALL"));
    }

    @Test
    public void testGetOperator_Join() {
        assertEquals(Operator.JOIN, Operator.of("JOIN"));
        assertEquals(Operator.LEFT_JOIN, Operator.of("LEFT JOIN"));
        assertEquals(Operator.RIGHT_JOIN, Operator.of("RIGHT JOIN"));
        assertEquals(Operator.FULL_JOIN, Operator.of("FULL JOIN"));
        assertEquals(Operator.CROSS_JOIN, Operator.of("CROSS JOIN"));
        assertEquals(Operator.INNER_JOIN, Operator.of("INNER JOIN"));
        assertEquals(Operator.NATURAL_JOIN, Operator.of("NATURAL JOIN"));
        assertEquals(Operator.ON, Operator.of("ON"));
        assertEquals(Operator.USING, Operator.of("USING"));
    }

    @Test
    public void testGetOperator_Clause() {
        assertEquals(Operator.WHERE, Operator.of("WHERE"));
        assertEquals(Operator.HAVING, Operator.of("HAVING"));
        assertEquals(Operator.GROUP_BY, Operator.of("GROUP BY"));
        assertEquals(Operator.ORDER_BY, Operator.of("ORDER BY"));
        assertEquals(Operator.LIMIT, Operator.of("LIMIT"));
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    public void testGetOperator_SetOperations() {
        assertEquals(Operator.UNION, Operator.of("UNION"));
        assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
        assertEquals(Operator.INTERSECT, Operator.of("INTERSECT"));
        assertEquals(Operator.EXCEPT, Operator.of("EXCEPT"));
        assertEquals(Operator.MINUS, Operator.of("MINUS"));
    }

    @Test
    public void testGetOperator_CaseInsensitive() {
        assertEquals(Operator.AND, Operator.of("and"));
        assertEquals(Operator.OR, Operator.of("or"));
        assertEquals(Operator.LIKE, Operator.of("like"));
        assertEquals(Operator.BETWEEN, Operator.of("between"));
        assertEquals(Operator.WHERE, Operator.of("where"));
    }

    @Test
    public void testGetOperator_Invalid() {
        assertNull(Operator.of("INVALID"));
        assertNull(Operator.of("UNKNOWN"));
    }

    @Test
    public void testGetName() {
        assertEquals("=", Operator.EQUAL.sqlToken());
        assertEquals("!=", Operator.NOT_EQUAL.sqlToken());
        assertEquals("AND", Operator.AND.sqlToken());
        assertEquals("OR", Operator.OR.sqlToken());
        assertEquals("LIKE", Operator.LIKE.sqlToken());
        assertEquals("BETWEEN", Operator.BETWEEN.sqlToken());
        assertEquals("WHERE", Operator.WHERE.sqlToken());
        assertEquals("LEFT JOIN", Operator.LEFT_JOIN.sqlToken());
    }

    @Test
    public void testToString() {
        assertEquals("=", Operator.EQUAL.toString());
        assertEquals("!=", Operator.NOT_EQUAL.toString());
        assertEquals("AND", Operator.AND.toString());
        assertEquals("OR", Operator.OR.toString());
        assertEquals("LIKE", Operator.LIKE.toString());
        assertEquals("BETWEEN", Operator.BETWEEN.toString());
        assertEquals("WHERE", Operator.WHERE.toString());
        assertEquals("LEFT JOIN", Operator.LEFT_JOIN.toString());
    }

    @Test
    public void testValues() {
        Operator[] values = Operator.values();
        assertNotNull(values);
        assertTrue(values.length > 40);
    }

    @Test
    public void testValueOf() {
        assertEquals(Operator.EQUAL, Operator.valueOf("EQUAL"));
        assertEquals(Operator.NOT_EQUAL, Operator.valueOf("NOT_EQUAL"));
        assertEquals(Operator.AND, Operator.valueOf("AND"));
        assertEquals(Operator.OR, Operator.valueOf("OR"));
        assertEquals(Operator.WHERE, Operator.valueOf("WHERE"));
    }

    @Test
    public void testGetOperator_Empty() {
        assertEquals(Operator.EMPTY, Operator.of(""));
    }

    @Test
    public void testOrdinal() {
        assertNotNull(Operator.EQUAL.ordinal());
        assertNotNull(Operator.AND.ordinal());
    }

    @Test
    public void testName() {
        assertEquals("EQUAL", Operator.EQUAL.name());
        assertEquals("NOT_EQUAL", Operator.NOT_EQUAL.name());
        assertEquals("AND", Operator.AND.name());
        assertEquals("OR", Operator.OR.name());
    }

    @Test
    public void testGetOperator_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> Operator.of(null));
    }

    @Test
    public void testGetOperator_Caching() {
        // First call initializes the cache
        Operator first = Operator.of("AND");
        // Second call should use cache
        Operator second = Operator.of("AND");
        assertEquals(first, second);
        assertEquals(Operator.AND, first);
    }

    @Test
    public void testGetOperator_ForUpdate() {
        assertEquals(Operator.FOR_UPDATE, Operator.of("FOR UPDATE"));
    }

    @Test
    public void testGetOperator_Offset() {
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    public void testAllOperators() {
        // Test all comparison operators
        assertNotNull(Operator.EQUAL);
        assertNotNull(Operator.NOT_EQUAL);
        assertNotNull(Operator.NOT_EQUAL_ANSI);
        assertNotNull(Operator.GREATER_THAN);
        assertNotNull(Operator.GREATER_THAN_OR_EQUAL);
        assertNotNull(Operator.LESS_THAN);
        assertNotNull(Operator.LESS_THAN_OR_EQUAL);

        // Test all composable operators
        assertNotNull(Operator.AND);
        assertNotNull(Operator.AND_OP);
        assertNotNull(Operator.OR);
        assertNotNull(Operator.OR_OP);
        assertNotNull(Operator.NOT);
        assertNotNull(Operator.NOT_OP);
        assertNotNull(Operator.XOR);

        // Test all join types
        assertNotNull(Operator.JOIN);
        assertNotNull(Operator.LEFT_JOIN);
        assertNotNull(Operator.RIGHT_JOIN);
        assertNotNull(Operator.FULL_JOIN);
        assertNotNull(Operator.CROSS_JOIN);
        assertNotNull(Operator.INNER_JOIN);
        assertNotNull(Operator.NATURAL_JOIN);

        // Test clauses
        assertNotNull(Operator.WHERE);
        assertNotNull(Operator.HAVING);
        assertNotNull(Operator.GROUP_BY);
        assertNotNull(Operator.ORDER_BY);
        assertNotNull(Operator.LIMIT);
        assertNotNull(Operator.OFFSET);

        // Test set operations
        assertNotNull(Operator.UNION);
        assertNotNull(Operator.UNION_ALL);
        assertNotNull(Operator.INTERSECT);
        assertNotNull(Operator.EXCEPT);
        assertNotNull(Operator.MINUS);

        // Test others
        assertNotNull(Operator.BETWEEN);
        assertNotNull(Operator.NOT_BETWEEN);
        assertNotNull(Operator.IN);
        assertNotNull(Operator.NOT_IN);
        assertNotNull(Operator.LIKE);
        assertNotNull(Operator.NOT_LIKE);
        assertNotNull(Operator.IS);
        assertNotNull(Operator.IS_NOT);
        assertNotNull(Operator.EXISTS);
        assertNotNull(Operator.NOT_EXISTS);
        assertNotNull(Operator.ANY);
        assertNotNull(Operator.SOME);
        assertNotNull(Operator.ALL);
        assertNotNull(Operator.ON);
        assertNotNull(Operator.USING);
        assertNotNull(Operator.FOR_UPDATE);
        assertNotNull(Operator.EMPTY);
    }
}

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

        // Composable operators
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
