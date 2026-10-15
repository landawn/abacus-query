package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

public class OperatorTest extends TestBase {

    @ParameterizedTest(name = "[{index}] unknown or untrimmed token: {0}")
    @NullSource
    @ValueSource(strings = { "INVALID", "UNKNOWN", "UNKNOWN_OPERATOR", "???", "UNKNOWN_OP_XYZ", " ", " AND", "AND ", "\tAND", "NOT  IN" })
    void testOfReturnsNullForUnknownOrUntrimmedTokens(String token) {
        assertNull(Operator.of(token));
    }

    @ParameterizedTest(name = "[{index}] SQL token and lookup aliases: {0}")
    @EnumSource(Operator.class)
    void testSqlTokenAndLookupAliases(Operator operator) {
        // An exhaustive switch keeps expectations independent of the implementation
        // and requires an explicit SQL token when a new operator is introduced.
        String expectedToken = switch (operator) {
            case EQUAL -> "=";
            case NOT_EQUAL -> "!=";
            case NOT_EQUAL_ANSI -> "<>";
            case NOT -> "NOT";
            case NOT_OP -> "!";
            case XOR -> "XOR";
            case LIKE -> "LIKE";
            case NOT_LIKE -> "NOT LIKE";
            case AND -> "AND";
            case AND_OP -> "&&";
            case OR -> "OR";
            case OR_OP -> "||";
            case GREATER_THAN -> ">";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_OR_EQUAL -> "<=";
            case BETWEEN -> "BETWEEN";
            case NOT_BETWEEN -> "NOT BETWEEN";
            case IS -> "IS";
            case IS_NOT -> "IS NOT";
            case EXISTS -> "EXISTS";
            case NOT_EXISTS -> "NOT EXISTS";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
            case ANY -> "ANY";
            case SOME -> "SOME";
            case ALL -> "ALL";
            case ON -> "ON";
            case USING -> "USING";
            case JOIN -> "JOIN";
            case LEFT_JOIN -> "LEFT JOIN";
            case RIGHT_JOIN -> "RIGHT JOIN";
            case FULL_JOIN -> "FULL JOIN";
            case CROSS_JOIN -> "CROSS JOIN";
            case INNER_JOIN -> "INNER JOIN";
            case NATURAL_JOIN -> "NATURAL JOIN";
            case WHERE -> "WHERE";
            case HAVING -> "HAVING";
            case GROUP_BY -> "GROUP BY";
            case ORDER_BY -> "ORDER BY";
            case LIMIT -> "LIMIT";
            case OFFSET -> "OFFSET";
            case FOR_UPDATE -> "FOR UPDATE";
            case UNION -> "UNION";
            case UNION_ALL -> "UNION ALL";
            case INTERSECT -> "INTERSECT";
            case EXCEPT -> "EXCEPT";
            case MINUS -> "MINUS";
            case EMPTY -> "";
        };

        assertAll(() -> assertEquals(expectedToken, operator.sqlToken()), () -> assertEquals(expectedToken, operator.toString()),
                () -> assertSame(operator, Operator.of(expectedToken)), () -> assertSame(operator, Operator.of(expectedToken.toLowerCase(Locale.ROOT))),
                () -> assertSame(operator, Operator.of(mixedCase(expectedToken))), () -> assertSame(operator, Operator.of(operator.name())),
                () -> assertSame(operator, Operator.of(operator.name().toLowerCase(Locale.ROOT))),
                () -> assertSame(operator, Operator.of(mixedCase(operator.name()))));
        assertSame(operator, Operator.of(expectedToken), "repeated lookup");
    }

    @Test
    void testSqlTokensAreUnique() {
        Set<String> tokens = new HashSet<>();
        for (Operator operator : Operator.values()) {
            assertTrue(tokens.add(operator.sqlToken()), () -> "Duplicate SQL token for " + operator.name());
        }
    }

    private static String mixedCase(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            result.append(i % 2 == 0 ? Character.toUpperCase(value.charAt(i)) : Character.toLowerCase(value.charAt(i)));
        }
        return result.toString();
    }
}
