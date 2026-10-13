package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class OperatorTest extends TestBase {

    @Test
    void testAllOperators() {
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

    @Test
    void testAllOperatorValues() {
        // Test that all operators have non-empty names
        for (Operator op : Operator.values()) {
            assertNotNull(op.sqlToken());
            assertNotNull(op.toString());
        }
    }

    @Test
    void testGetName() {
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
    void testGetOperator_Caching() {
        // First call initializes the cache
        Operator first = Operator.of("AND");
        // Second call should use cache
        Operator second = Operator.of("AND");
        assertEquals(first, second);
        assertEquals(Operator.AND, first);
    }

    @Test
    void testGetOperator_CaseInsensitive() {
        assertEquals(Operator.AND, Operator.of("and"));
        assertEquals(Operator.OR, Operator.of("or"));
        assertEquals(Operator.LIKE, Operator.of("like"));
        assertEquals(Operator.BETWEEN, Operator.of("between"));
        assertEquals(Operator.WHERE, Operator.of("where"));
    }

    @Test
    void testGetOperator_Clause() {
        assertEquals(Operator.WHERE, Operator.of("WHERE"));
        assertEquals(Operator.HAVING, Operator.of("HAVING"));
        assertEquals(Operator.GROUP_BY, Operator.of("GROUP BY"));
        assertEquals(Operator.ORDER_BY, Operator.of("ORDER BY"));
        assertEquals(Operator.LIMIT, Operator.of("LIMIT"));
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    void testGetOperator_Comparison() {
        assertEquals(Operator.EQUAL, Operator.of("="));
        assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
        assertEquals(Operator.GREATER_THAN, Operator.of(">"));
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Operator.of(">="));
        assertEquals(Operator.LESS_THAN, Operator.of("<"));
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Operator.of("<="));
    }

    @Test
    void testGetOperator_Composable() {
        assertEquals(Operator.AND, Operator.of("AND"));
        assertEquals(Operator.AND_OP, Operator.of("&&"));
        assertEquals(Operator.OR, Operator.of("OR"));
        assertEquals(Operator.OR_OP, Operator.of("||"));
        assertEquals(Operator.NOT, Operator.of("NOT"));
        assertEquals(Operator.NOT_OP, Operator.of("!"));
        assertEquals(Operator.XOR, Operator.of("XOR"));
    }

    @Test
    void testGetOperator_Empty() {
        assertEquals(Operator.EMPTY, Operator.of(""));
    }

    @Test
    void testGetOperator_ForUpdate() {
        assertEquals(Operator.FOR_UPDATE, Operator.of("FOR UPDATE"));
    }

    @Test
    void testGetOperator_Invalid() {
        assertNull(Operator.of("INVALID"));
        assertNull(Operator.of("UNKNOWN"));
    }

    @Test
    void testGetOperator_Join() {
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
    void testGetOperator_Null() {
        assertEquals(Operator.IS, Operator.of("IS"));
        assertEquals(Operator.IS_NOT, Operator.of("IS NOT"));
    }

    @Test
    void testGetOperator_NullInput() {
        // of(null) returns null (uniform "null for any non-match"), consistent with an unknown token
        assertNull(Operator.of(null));
    }

    @Test
    void testGetOperator_Offset() {
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    void testGetOperator_Pattern() {
        assertEquals(Operator.LIKE, Operator.of("LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
    }

    @Test
    void testGetOperator_Range() {
        assertEquals(Operator.BETWEEN, Operator.of("BETWEEN"));
        assertEquals(Operator.NOT_BETWEEN, Operator.of("NOT BETWEEN"));
        assertEquals(Operator.IN, Operator.of("IN"));
        assertEquals(Operator.NOT_IN, Operator.of("NOT IN"));
    }

    @Test
    void testGetOperator_SetOperations() {
        assertEquals(Operator.UNION, Operator.of("UNION"));
        assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
        assertEquals(Operator.INTERSECT, Operator.of("INTERSECT"));
        assertEquals(Operator.EXCEPT, Operator.of("EXCEPT"));
        assertEquals(Operator.MINUS, Operator.of("MINUS"));
    }

    @Test
    void testGetOperator_Subquery() {
        assertEquals(Operator.EXISTS, Operator.of("EXISTS"));
        assertEquals(Operator.NOT_EXISTS, Operator.of("NOT EXISTS"));
        assertEquals(Operator.ANY, Operator.of("ANY"));
        assertEquals(Operator.SOME, Operator.of("SOME"));
        assertEquals(Operator.ALL, Operator.of("ALL"));
    }

    @Test
    void testGetOperatorCaching() {
        // First call should cache the result
        Operator op1 = Operator.of("and");
        assertEquals(Operator.AND, op1);

        // Second call should return cached result
        Operator op2 = Operator.of("and");
        assertEquals(Operator.AND, op2);

        // Verify caching works for different cases
        Operator op3 = Operator.of("AND");
        assertEquals(Operator.AND, op3);
    }

    @Test
    void testGetOperatorCaseInsensitive() {
        assertEquals(Operator.AND, Operator.of("and"));
        assertEquals(Operator.OR, Operator.of("or"));
        assertEquals(Operator.LIKE, Operator.of("like"));
        assertEquals(Operator.BETWEEN, Operator.of("between"));
        assertEquals(Operator.NOT_IN, Operator.of("not in"));
    }

    @Test
    void testGetOperatorWithAlternativeSymbols() {
        assertEquals(Operator.AND_OP, Operator.of("&&"));
        assertEquals(Operator.OR_OP, Operator.of("||"));
        assertEquals(Operator.NOT_OP, Operator.of("!"));
    }

    @Test
    void testGetOperatorWithCompoundOperators() {
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
        assertEquals(Operator.NOT_BETWEEN, Operator.of("NOT BETWEEN"));
        assertEquals(Operator.IS_NOT, Operator.of("IS NOT"));
        assertEquals(Operator.LEFT_JOIN, Operator.of("LEFT JOIN"));
        assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
    }

    @Test
    void testGetOperatorWithExactMatch() {
        assertEquals(Operator.EQUAL, Operator.of("="));
        assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        assertEquals(Operator.AND, Operator.of("AND"));
        assertEquals(Operator.OR, Operator.of("OR"));
        assertEquals(Operator.LIKE, Operator.of("LIKE"));
    }

    @Test
    void testGetOperatorWithSymbols() {
        assertEquals(Operator.GREATER_THAN, Operator.of(">"));
        assertEquals(Operator.LESS_THAN, Operator.of("<"));
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Operator.of(">="));
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Operator.of("<="));
        assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
    }

    @Test
    void testGetOperatorWithUnknown() {
        assertNull(Operator.of("UNKNOWN_OPERATOR"));
        assertNull(Operator.of("???"));
    }

    @Test
    void testName() {
        assertEquals("EQUAL", Operator.EQUAL.name());
        assertEquals("NOT_EQUAL", Operator.NOT_EQUAL.name());
        assertEquals("AND", Operator.AND.name());
        assertEquals("OR", Operator.OR.name());
    }

    /**
     * Second-pass locking test: of() must accept both the sqlToken form and the enum name
     * form, and both must be case-insensitive. Multi-word tokens like "NOT LIKE" and
     * "GROUP BY" must work too.
     */
    @Test
    void testOfHandlesAllExpectedAliasFormsCaseInsensitive() {
        // sqlToken form
        assertEquals(Operator.EQUAL, Operator.of("="));
        assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
        assertEquals(Operator.LIKE, Operator.of("LIKE"));
        assertEquals(Operator.LIKE, Operator.of("like"));
        assertEquals(Operator.LIKE, Operator.of("LiKe"));
        // multi-word sqlToken
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.of("not like"));
        assertEquals(Operator.GROUP_BY, Operator.of("GROUP BY"));
        assertEquals(Operator.GROUP_BY, Operator.of("group by"));
        assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
        assertEquals(Operator.UNION_ALL, Operator.of("union all"));
        // enum-name form
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT_LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.of("not_like"));
        assertEquals(Operator.GROUP_BY, Operator.of("GROUP_BY"));
        // unknown returns null (not exception)
        assertNull(Operator.of("UNKNOWN_OP_XYZ"));
    }

    @Test
    void testOperatorUniqueness() {
        // Verify that operator names are unique (except for intentional duplicates)
        Operator[] operators = Operator.values();
        for (int i = 0; i < operators.length; i++) {
            for (int j = i + 1; j < operators.length; j++) {
                if (operators[i].sqlToken().equals(operators[j].sqlToken())) {
                    // Only EXCEPT and MINUS should have the same name
                    assertTrue(
                            (operators[i] == Operator.EXCEPT && operators[j] == Operator.MINUS)
                                    || (operators[i] == Operator.MINUS && operators[j] == Operator.EXCEPT),
                            "Unexpected duplicate operator names: " + operators[i] + " and " + operators[j]);
                }
            }
        }
    }

    @Test
    void testOrdinal() {
        assertNotNull(Operator.EQUAL.ordinal());
        assertNotNull(Operator.AND.ordinal());
    }

    @Test
    void testSpecificOperators() {
        // Comparison operators
        assertEquals("=", Operator.EQUAL.sqlToken());
        assertEquals("!=", Operator.NOT_EQUAL.sqlToken());
        assertEquals("<>", Operator.NOT_EQUAL_ANSI.sqlToken());
        assertEquals(">", Operator.GREATER_THAN.sqlToken());
        assertEquals(">=", Operator.GREATER_THAN_OR_EQUAL.sqlToken());
        assertEquals("<", Operator.LESS_THAN.sqlToken());
        assertEquals("<=", Operator.LESS_THAN_OR_EQUAL.sqlToken());

        // Composable operators
        assertEquals("AND", Operator.AND.sqlToken());
        assertEquals("OR", Operator.OR.sqlToken());
        assertEquals("NOT", Operator.NOT.sqlToken());
        assertEquals("XOR", Operator.XOR.sqlToken());

        // Range/Set operators
        assertEquals("BETWEEN", Operator.BETWEEN.sqlToken());
        assertEquals("NOT BETWEEN", Operator.NOT_BETWEEN.sqlToken());
        assertEquals("IN", Operator.IN.sqlToken());
        assertEquals("NOT IN", Operator.NOT_IN.sqlToken());
        assertEquals("LIKE", Operator.LIKE.sqlToken());
        assertEquals("NOT LIKE", Operator.NOT_LIKE.sqlToken());

        // Join operators
        assertEquals("JOIN", Operator.JOIN.sqlToken());
        assertEquals("LEFT JOIN", Operator.LEFT_JOIN.sqlToken());
        assertEquals("RIGHT JOIN", Operator.RIGHT_JOIN.sqlToken());
        assertEquals("FULL JOIN", Operator.FULL_JOIN.sqlToken());
        assertEquals("CROSS JOIN", Operator.CROSS_JOIN.sqlToken());
        assertEquals("INNER JOIN", Operator.INNER_JOIN.sqlToken());
        assertEquals("NATURAL JOIN", Operator.NATURAL_JOIN.sqlToken());

        // Clauses
        assertEquals("WHERE", Operator.WHERE.sqlToken());
        assertEquals("HAVING", Operator.HAVING.sqlToken());
        assertEquals("GROUP BY", Operator.GROUP_BY.sqlToken());
        assertEquals("ORDER BY", Operator.ORDER_BY.sqlToken());
        assertEquals("LIMIT", Operator.LIMIT.sqlToken());
        assertEquals("OFFSET", Operator.OFFSET.sqlToken());

        // Set operations
        assertEquals("UNION", Operator.UNION.sqlToken());
        assertEquals("UNION ALL", Operator.UNION_ALL.sqlToken());
        assertEquals("INTERSECT", Operator.INTERSECT.sqlToken());
        assertEquals("EXCEPT", Operator.EXCEPT.sqlToken());
        assertEquals("MINUS", Operator.MINUS.sqlToken());

        // Special operators
        assertEquals("", Operator.EMPTY.sqlToken());
        assertEquals("IS", Operator.IS.sqlToken());
        assertEquals("IS NOT", Operator.IS_NOT.sqlToken());
        assertEquals("EXISTS", Operator.EXISTS.sqlToken());
        assertEquals("ANY", Operator.ANY.sqlToken());
        assertEquals("SOME", Operator.SOME.sqlToken());
        assertEquals("ALL", Operator.ALL.sqlToken());
        assertEquals("ON", Operator.ON.sqlToken());
        assertEquals("USING", Operator.USING.sqlToken());
    }

    @Test
    void testToString() {
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
    void testValueOf() {
        assertEquals(Operator.EQUAL, Operator.valueOf("EQUAL"));
        assertEquals(Operator.NOT_EQUAL, Operator.valueOf("NOT_EQUAL"));
        assertEquals(Operator.AND, Operator.valueOf("AND"));
        assertEquals(Operator.OR, Operator.valueOf("OR"));
        assertEquals(Operator.WHERE, Operator.valueOf("WHERE"));
    }

    @Test
    void testValues() {
        Operator[] values = Operator.values();
        assertNotNull(values);
        assertTrue(values.length > 40);
    }
}
