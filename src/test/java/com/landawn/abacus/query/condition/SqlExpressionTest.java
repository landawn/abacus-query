package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for SqlExpression.
 * Tests all public methods including constructors, factory methods, SQL functions, operators, and utilities.
 */
@Tag("2025")
public class SqlExpressionTest extends TestBase {
    @Test
    public void testComposingNullCustomRenderResultsPreservesLiteralNull() {
        final Condition condition = new Condition() {
            @Override
            public Operator operator() {
                return Operator.EQUAL;
            }

            @Override
            public com.landawn.abacus.util.ImmutableList<Object> parameters() {
                return com.landawn.abacus.util.ImmutableList.empty();
            }

            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                return null;
            }
        };

        Assertions.assertAll(() -> assertEquals("null + 1", SqlExpression.plus(condition, 1)),
                () -> assertEquals("value BETWEEN null AND 10", SqlExpression.between("value", condition, 10)),
                () -> assertThrows(IllegalArgumentException.class, () -> SqlExpression.plus(customNumber(null), 1)));
    }

    @Test
    public void testStandardQuotedBackslashDoesNotHideTrailingLineComment() {
        final String expr = "'a\\' -- trailing comment";

        Assertions.assertAll(() -> assertEquals(expr + "\n + 1", SqlExpression.plus(SqlExpression.of(expr), 1)),
                () -> assertEquals("COUNT(" + expr + "\n)", SqlExpression.count(expr)),
                () -> assertEquals("value BETWEEN " + expr + "\n AND 10", SqlExpression.between("value", SqlExpression.of(expr), 10)));
    }

    @Test
    public void testHelpersTerminateLineCommentsBeforeGeneratedSyntax() {
        final String expr = "price -- trailing comment";
        assertEquals(expr + "\n = 1", SqlExpression.equal(expr, 1));
        assertEquals(expr + "\n IS NULL", SqlExpression.isNull(expr));
        assertEquals("(" + expr + "\n IS NULL OR " + expr + "\n = '')", SqlExpression.isNullOrEmpty(expr));
        assertEquals("(" + expr + "\n IS NOT NULL AND " + expr + "\n <> '')", SqlExpression.isNotNullAndNotEmpty(expr));
        assertEquals("price BETWEEN " + expr + "\n AND 10", SqlExpression.between("price", SqlExpression.of(expr), 10));
        assertEquals("(" + expr + "\n) AND (active = 1)", SqlExpression.and(expr, "active = 1"));
        assertEquals(expr + "\n + 1", SqlExpression.plus(SqlExpression.of(expr), 1));
        assertEquals("COUNT(" + expr + "\n)", SqlExpression.count(expr));
        assertEquals("CONCAT(" + expr + "\n, 'x')", SqlExpression.concat(expr, "'x'"));
    }

    @Test
    public void testCommentBoundaryHandlingPreservesSafeFragments() {
        assertEquals("'-- text' + 1", SqlExpression.plus(SqlExpression.of("'-- text'"), 1));
        assertEquals("COUNT(\"name--suffix\")", SqlExpression.count("\"name--suffix\""));
        assertEquals("price -- comment\n = 1", SqlExpression.equal("price -- comment\n", 1));
        assertEquals("price /* comment */ = 1", SqlExpression.equal("price /* comment */", 1));
        assertEquals("price -- comment", SqlExpression.of("price -- comment").literal());
        assertEquals("price -- comment", SqlExpression.renderValue(SqlExpression.of("price -- comment")));
    }


    private static Number customNumber(final String literal) {
        return new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return 0;
            }

            @Override
            public double doubleValue() {
                return 0;
            }

            @Override
            public String toString() {
                return literal;
            }
        };
    }

    @Test
    public void testConstructorWithLiteral() {
        String literal = "CURRENT_TIMESTAMP";
        SqlExpression expr = new SqlExpression(literal);

        assertNotNull(expr);
        assertEquals(literal, expr.literal());
    }

    @Test
    public void testLiteral() {
        SqlExpression expr = new SqlExpression("price * 1.1");

        assertEquals("price * 1.1", expr.literal());
    }

    @Test
    public void testOfMethodDoesNotInternInstances() {
        SqlExpression expr1 = SqlExpression.of("CURRENT_DATE");
        SqlExpression expr2 = SqlExpression.of("CURRENT_DATE");

        assertNotSame(expr1, expr2, "Dynamic SQL text must not be retained in a global interning cache");
        assertEquals(expr1, expr2);
    }

    @Test
    public void testOfMethodDifferentLiterals() {
        SqlExpression expr1 = SqlExpression.of("literal1");
        SqlExpression expr2 = SqlExpression.of("literal2");

        assertNotSame(expr1, expr2);
    }

    // Comparison operators
    @Test
    public void testEqual() {
        String result = SqlExpression.equal("age", 25);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("25"));
    }

    @Test
    public void testEqualWithNull() {
        String result = SqlExpression.equal("middleName", null);
        assertEquals("middleName IS NULL", result);
    }

    @Test
    public void testEq() {
        String result = SqlExpression.eq("status", "active");

        assertTrue(result.contains("status"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("'active'"));
    }

    @Test
    public void testNotEqual() {
        String result = SqlExpression.notEqual("status", "inactive");

        assertTrue(result.contains("status"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("'inactive'"));
    }

    @Test
    public void testNotEqualWithNull() {
        String result = SqlExpression.notEqual("deleted", null);
        assertEquals("deleted IS NOT NULL", result);
    }

    @Test
    public void testNe() {
        String result = SqlExpression.ne("count", 0);

        assertTrue(result.contains("count"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("0"));
    }

    @Test
    public void testGreaterThan() {
        String result = SqlExpression.greaterThan("salary", 50000);

        assertTrue(result.contains("salary"));
        assertTrue(result.contains(">"));
        assertTrue(result.contains("50000"));
    }

    @Test
    public void testGt() {
        String result = SqlExpression.gt("age", 18);

        assertTrue(result.contains("age"));
        assertTrue(result.contains(">"));
        assertTrue(result.contains("18"));
    }

    @Test
    public void testGreaterThanOrEqual() {
        String result = SqlExpression.greaterThanOrEqual("score", 60);

        assertTrue(result.contains("score"));
        assertTrue(result.contains(">="));
        assertTrue(result.contains("60"));
    }

    @Test
    public void testGe() {
        String result = SqlExpression.ge("quantity", 1);

        assertTrue(result.contains("quantity"));
        assertTrue(result.contains(">="));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testLessThan() {
        String result = SqlExpression.lessThan("price", 100);

        assertTrue(result.contains("price"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testLt() {
        String result = SqlExpression.lt("stock", 10);

        assertTrue(result.contains("stock"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testLessThanOrEqual() {
        String result = SqlExpression.lessThanOrEqual("discount", 50);

        assertTrue(result.contains("discount"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("50"));
    }

    @Test
    public void testLe() {
        String result = SqlExpression.le("temperature", 32);

        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("32"));
    }

    @Test
    public void testBetween() {
        String result = SqlExpression.between("age", 18, 65);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testLike() {
        String result = SqlExpression.like("name", "John%");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("'John%'"));
    }

    @Test
    public void testNotBetween() {
        assertEquals("age NOT BETWEEN 18 AND 65", SqlExpression.notBetween("age", 18, 65));
        assertEquals("created NOT BETWEEN '2024-01-01' AND '2024-12-31'", SqlExpression.notBetween("created", "2024-01-01", "2024-12-31"));
    }

    @Test
    public void testNotLike() {
        String result = SqlExpression.notLike("name", "John%");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("NOT LIKE"));
        assertTrue(result.contains("'John%'"));
    }

    @Test
    public void testIsNull() {
        String result = SqlExpression.isNull("middleName");

        assertTrue(result.contains("middleName"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testIsNotNull() {
        String result = SqlExpression.isNotNull("email");

        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testIsNullOrEmptyUsesExecutableSql() {
        assertEquals("(description IS NULL OR description = '')", SqlExpression.isNullOrEmpty("description"));

        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNullOrEmpty(null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNullOrEmpty(""));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNullOrEmpty(" \t"));
    }

    @Test
    public void testIsNotNullAndNotEmptyUsesExecutableSql() {
        assertEquals("(name IS NOT NULL AND name <> '')", SqlExpression.isNotNullAndNotEmpty("name"));

        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNotNullAndNotEmpty(null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNotNullAndNotEmpty(""));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNotNullAndNotEmpty(" \t"));
    }

    // Composable operators
    @Test
    public void testAnd() {
        assertEquals("(active = true) AND (age > 18)", SqlExpression.and("active = true", "age > 18"));
    }

    @Test
    public void testOr() {
        assertEquals("(status = 'active') OR (status = 'pending')", SqlExpression.or("status = 'active'", "status = 'pending'"));

        assertEquals("(a = 1 OR b = 2) AND (c = 3)", SqlExpression.and("a = 1 OR b = 2", "c = 3"));
        assertEquals("(a = 1 AND b = 2) OR (c = 3)", SqlExpression.or("a = 1 AND b = 2", "c = 3"));
    }

    // Arithmetic operators
    @Test
    public void testPlus() {
        String result = SqlExpression.plus("price", "tax", "shipping");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("+"));
        assertTrue(result.contains("tax"));
        assertTrue(result.contains("shipping"));
    }

    @Test
    public void testMinus() {
        String result = SqlExpression.minus("total", "discount");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("-"));
        assertTrue(result.contains("discount"));
    }

    @Test
    public void testMulti() {
        String result = SqlExpression.multiply("price", "quantity");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
    }

    @Test
    public void testDivision() {
        String result = SqlExpression.divide("total", "count");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("/"));
        assertTrue(result.contains("count"));
    }

    @Test
    public void testModulus() {
        String result = SqlExpression.modulus("value", 10);

        assertTrue(result.contains("value"));
        assertTrue(result.contains("%"));
        assertTrue(result.contains("10"));
    }

    // Bitwise operators
    @Test
    public void testBitwiseAnd() {
        String result = SqlExpression.bitwiseAnd("flags", "mask");

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("&"));
        assertTrue(result.contains("mask"));
    }

    @Test
    public void testBitwiseOr() {
        String result = SqlExpression.bitwiseOr("flags1", "flags2");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("flags2"));
    }

    @Test
    public void testBitwiseXOr() {
        String result = SqlExpression.bitwiseXor("value1", "value2");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
    }

    @Test
    public void testLShift() {
        String result = SqlExpression.leftShift("flags", 2);

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShift() {
        String result = SqlExpression.rightShift("value", 4);

        assertTrue(result.contains("value"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("4"));
    }

    // Aggregate functions
    @Test
    public void testCount() {
        String result = SqlExpression.count("*");

        assertEquals("COUNT(*)", result);
    }

    @Test
    public void testAverage() {
        String result = SqlExpression.avg("salary");

        assertTrue(result.contains("AVG"));
        assertTrue(result.contains("salary"));
    }

    @Test
    public void testSum() {
        String result = SqlExpression.sum("amount");

        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("amount"));
    }

    @Test
    public void testMin() {
        String result = SqlExpression.min("price");

        assertTrue(result.contains("MIN"));
        assertTrue(result.contains("price"));
    }

    @Test
    public void testMax() {
        String result = SqlExpression.max("score");

        assertTrue(result.contains("MAX"));
        assertTrue(result.contains("score"));
    }

    // Mathematical functions
    @Test
    public void testAbs() {
        String result = SqlExpression.abs("balance");

        assertTrue(result.contains("ABS"));
        assertTrue(result.contains("balance"));
    }

    @Test
    public void testCeil() {
        String result = SqlExpression.ceil("price");

        assertTrue(result.contains("CEIL"));
        assertTrue(result.contains("price"));
    }

    @Test
    public void testFloor() {
        String result = SqlExpression.floor("average");

        assertTrue(result.contains("FLOOR"));
        assertTrue(result.contains("average"));
    }

    @Test
    public void testSqrt() {
        String result = SqlExpression.sqrt("area");

        assertTrue(result.contains("SQRT"));
        assertTrue(result.contains("area"));
    }

    @Test
    public void testPower() {
        String result = SqlExpression.power("base", "exponent");

        assertTrue(result.contains("POWER"));
        assertTrue(result.contains("base"));
        assertTrue(result.contains("exponent"));
    }

    @Test
    public void testMod() {
        String result = SqlExpression.mod("dividend", "divisor");

        assertTrue(result.contains("MOD"));
        assertTrue(result.contains("dividend"));
        assertTrue(result.contains("divisor"));
    }

    @Test
    public void testLog() {
        String result = SqlExpression.log("10", "100");

        assertTrue(result.contains("LOG"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testLn() {
        String result = SqlExpression.ln("value");

        assertTrue(result.contains("LN"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testExp() {
        String result = SqlExpression.exp("rate");

        assertTrue(result.contains("EXP"));
        assertTrue(result.contains("rate"));
    }

    @Test
    public void testSign() {
        String result = SqlExpression.sign("balance");

        assertTrue(result.contains("SIGN"));
        assertTrue(result.contains("balance"));
    }

    // Trigonometric functions
    @Test
    public void testSin() {
        String result = SqlExpression.sin("angle");

        assertTrue(result.contains("SIN"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testCos() {
        String result = SqlExpression.cos("angle");

        assertTrue(result.contains("COS"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testTan() {
        String result = SqlExpression.tan("angle");

        assertTrue(result.contains("TAN"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testAsin() {
        String result = SqlExpression.asin("value");

        assertTrue(result.contains("ASIN"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testAcos() {
        String result = SqlExpression.acos("value");

        assertTrue(result.contains("ACOS"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testAtan() {
        String result = SqlExpression.atan("value");

        assertTrue(result.contains("ATAN"));
        assertTrue(result.contains("value"));
    }

    // String functions
    @Test
    public void testConcat() {
        String result = SqlExpression.concat("firstName", "' '");

        assertTrue(result.contains("CONCAT"));
        assertTrue(result.contains("firstName"));
    }

    @Test
    public void testReplace() {
        String result = SqlExpression.replace("email", "'@'", "'_at_'");

        assertTrue(result.contains("REPLACE"));
        assertTrue(result.contains("email"));
    }

    @Test
    public void testStringLength() {
        String result = SqlExpression.length("name");

        assertTrue(result.contains("LENGTH"));
        assertTrue(result.contains("name"));
    }

    @Test
    public void testSubStringFromIndex() {
        String result = SqlExpression.substr("phone", 1);

        assertTrue(result.contains("SUBSTR"));
        assertTrue(result.contains("phone"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testSubStringWithLength() {
        String result = SqlExpression.substr("code", 1, 3);

        assertTrue(result.contains("SUBSTR"));
        assertTrue(result.contains("code"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testTrim() {
        String result = SqlExpression.trim("input");

        assertTrue(result.contains("TRIM"));
        assertTrue(result.contains("input"));
    }

    @Test
    public void testLTrim() {
        String result = SqlExpression.ltrim("comment");

        assertTrue(result.contains("LTRIM"));
        assertTrue(result.contains("comment"));
    }

    @Test
    public void testRTrim() {
        String result = SqlExpression.rtrim("code");

        assertTrue(result.contains("RTRIM"));
        assertTrue(result.contains("code"));
    }

    @Test
    public void testLPad() {
        String result = SqlExpression.lpad("id", 10, "'0'");

        assertTrue(result.contains("LPAD"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testRPad() {
        String result = SqlExpression.rpad("name", 20, "' '");

        assertTrue(result.contains("RPAD"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("20"));
    }

    @Test
    public void testLower() {
        String result = SqlExpression.lower("email");

        assertTrue(result.contains("LOWER"));
        assertTrue(result.contains("email"));
    }

    @Test
    public void testUpper() {
        String result = SqlExpression.upper("name");

        assertTrue(result.contains("UPPER"));
        assertTrue(result.contains("name"));
    }

    // Utility methods
    @Test
    public void testRenderValueString() {
        String result = SqlExpression.renderValue("text");

        assertEquals("'text'", result);
    }

    @Test
    public void testRenderValueNumber() {
        String result = SqlExpression.renderValue(123);

        assertEquals("123", result);
    }

    @Test
    public void testRenderValueBoolean() {
        String result = SqlExpression.renderValue(true);

        assertEquals("true", result);
    }

    @Test
    public void testRenderValueNull() {
        String result = SqlExpression.renderValue(null);

        assertEquals("null", result);
    }

    @Test
    public void testRenderValueExpression() {
        SqlExpression expr = new SqlExpression("column_name");
        String result = SqlExpression.renderValue(expr);

        assertEquals("column_name", result);
    }

    @Test
    public void testParameters() {
        SqlExpression expr = new SqlExpression("price * 1.1");

        List<Object> params = expr.parameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testToStringNoChange() {
        SqlExpression expr = new SqlExpression("userName = 'John'");

        String result = expr.toSql(NamingPolicy.NO_CHANGE);

        assertEquals("userName = 'John'", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SqlExpression expr = new SqlExpression("firstName");

        String result = expr.toSql(NamingPolicy.SNAKE_CASE);

        assertEquals("first_name", result);
    }

    @Test
    public void testConstructorWithNull() {
        assertThrows(IllegalArgumentException.class, () -> new SqlExpression(null));
    }

    @Test
    public void testToStringEmpty() {
        SqlExpression expr = new SqlExpression("");

        String result = expr.toSql(NamingPolicy.NO_CHANGE);

        assertEquals("", result);
    }

    @Test
    public void testHashCode() {
        SqlExpression expr1 = new SqlExpression("test");
        SqlExpression expr2 = new SqlExpression("test");

        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    public void testHashCodeEmptyLiteral() {
        SqlExpression expr = new SqlExpression("");

        assertEquals("".hashCode(), expr.hashCode());
    }

    @Test
    public void testEquals() {
        SqlExpression expr1 = new SqlExpression("test");
        SqlExpression expr2 = new SqlExpression("test");

        assertEquals(expr1, expr2);
    }

    @Test
    public void testEqualsSameInstance() {
        SqlExpression expr = new SqlExpression("test");

        assertEquals(expr, expr);
    }

    @Test
    public void testEqualsNull() {
        SqlExpression expr = new SqlExpression("test");

        assertNotEquals(expr, null);
    }

    @Test
    public void testEqualsDifferentType() {
        SqlExpression expr = new SqlExpression("test");

        assertNotEquals(expr, "not an expression");
    }

    @Test
    public void testEqualsDifferentLiteral() {
        SqlExpression expr1 = new SqlExpression("literal1");
        SqlExpression expr2 = new SqlExpression("literal2");

        assertNotEquals(expr1, expr2);
    }

    //    @Test
    //    public void testExprAlias() {
    //        SqlExpression.Expr expr = new SqlExpression.Expr("test");
    //
    //        assertNotNull(expr);
    //        assertEquals("test", expr.literal());
    //    }

    // Removed: testBt() - bt() method has been removed. Use between() instead.

    @Test
    public void testConcatWithTwoStrings() {
        String result = SqlExpression.concat("firstName", "lastName");

        assertTrue(result.contains("CONCAT"));
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("lastName"));
    }

    @Test
    public void testAndWithMultipleExpressions() {
        String result = SqlExpression.and("x > 0", "y > 0", "z > 0");

        assertTrue(result.contains("x > 0"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("y > 0"));
        assertTrue(result.contains("z > 0"));
    }

    @Test
    public void testOrWithMultipleExpressions() {
        String result = SqlExpression.or("status = 'A'", "status = 'B'", "status = 'C'");

        assertTrue(result.contains("status = 'A'"));
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status = 'B'"));
        assertTrue(result.contains("status = 'C'"));
    }

    @Test
    public void testPlusWithMultipleValues() {
        String result = SqlExpression.plus("a", "b", "c");

        assertTrue(result.contains("a"));
        assertTrue(result.contains("+"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testMinusWithMultipleValues() {
        String result = SqlExpression.minus("total", "tax", "discount");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("-"));
        assertTrue(result.contains("tax"));
        assertTrue(result.contains("discount"));
    }

    @Test
    public void testMultiWithMultipleValues() {
        String result = SqlExpression.multiply("price", "quantity", "rate");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("rate"));
    }

    @Test
    public void testDivisionWithMultipleValues() {
        String result = SqlExpression.divide("total", "count", "factor");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("/"));
        assertTrue(result.contains("count"));
        assertTrue(result.contains("factor"));
    }

    @Test
    public void testModulusWithMultipleValues() {
        String result = SqlExpression.modulus("value", "10", "3");

        assertTrue(result.contains("value"));
        assertTrue(result.contains("%"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testLShiftWithMultipleValues() {
        String result = SqlExpression.leftShift("flags", "2", "1");

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShiftWithMultipleValues() {
        String result = SqlExpression.rightShift("value", "4", "2");

        assertTrue(result.contains("value"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("4"));
    }

    @Test
    public void testBitwiseAndWithMultipleValues() {
        String result = SqlExpression.bitwiseAnd("flags1", "flags2", "mask");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("&"));
        assertTrue(result.contains("flags2"));
        assertTrue(result.contains("mask"));
    }

    @Test
    public void testBitwiseOrWithMultipleValues() {
        String result = SqlExpression.bitwiseOr("flags1", "flags2", "flags3");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("flags2"));
        assertTrue(result.contains("flags3"));
    }

    @Test
    public void testBitwiseXOrWithMultipleValues() {
        String result = SqlExpression.bitwiseXor("value1", "value2", "value3");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
        assertTrue(result.contains("value3"));
    }

    @Test
    public void testRenderValueCharSequence() {
        String result = SqlExpression.renderValue(new StringBuilder("test"));

        assertEquals("'test'", result);
    }

    @Test
    public void testRenderValueDouble() {
        String result = SqlExpression.renderValue(3.14);

        assertEquals("3.14", result);
    }

    @Test
    public void testRenderValueLong() {
        String result = SqlExpression.renderValue(999L);

        assertEquals("999", result);
    }

    @Test
    public void testOfMethodWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlExpression.of(null);
        });
    }

    @Test
    public void testEqualsWithEmptyLiterals() {
        SqlExpression expr1 = new SqlExpression("");
        SqlExpression expr2 = new SqlExpression("");

        assertEquals(expr1, expr2);
    }

    @Test
    public void testConstructor() {
        SqlExpression expr = new SqlExpression("price * 0.9");
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("price * 0.9", expr.literal());
        Assertions.assertEquals(Operator.EMPTY, expr.operator());
    }

    @Test
    public void testRenderValueRejectsNonNumericNumberText() {
        assertEquals("-6.02E23", SqlExpression.renderValue(customNumber("-6.02E23")));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(customNumber("1 UNION SELECT password FROM users")));
    }

    @Test
    public void testOf() {
        SqlExpression expr1 = SqlExpression.of("CURRENT_TIMESTAMP");
        SqlExpression expr2 = SqlExpression.of("CURRENT_TIMESTAMP");

        Assertions.assertNotSame(expr1, expr2);
        Assertions.assertEquals(expr1, expr2);
        Assertions.assertEquals("CURRENT_TIMESTAMP", expr1.literal());
    }

    @Test
    public void testRenderValue() {
        Assertions.assertEquals("'text'", SqlExpression.renderValue("text"));
        Assertions.assertEquals("123", SqlExpression.renderValue(123));
        Assertions.assertEquals("null", SqlExpression.renderValue(null));

        SqlExpression expr = SqlExpression.of("CURRENT_DATE");
        Assertions.assertEquals("CURRENT_DATE", SqlExpression.renderValue(expr));
    }

    @Test
    public void testRenderValueSubQueryCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Assertions.assertEquals("(SELECT id FROM users)", SqlExpression.renderValue(subQuery));
    }

    @Test
    public void testEqualWithSubQueryCondition() {
        String result = SqlExpression.equal("id", Filters.subQuery("SELECT id FROM users"));
        Assertions.assertEquals("id = (SELECT id FROM users)", result);
    }

    @Test
    public void testSubString() {
        String result = SqlExpression.substr("text", 5);
        Assertions.assertEquals("SUBSTR(text, 5)", result);
    }

    @Test
    public void testToString() {
        SqlExpression expr = SqlExpression.of("price > 100");
        Assertions.assertEquals("price > 100", expr.toString());
    }

    //    @Test
    //    public void testExprClass() {
    //        SqlExpression.Expr expr = new SqlExpression.Expr("test expression");
    //        Assertions.assertEquals("test expression", expr.literal());
    //        Assertions.assertTrue(expr instanceof SqlExpression);
    //    }

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        SqlExpression expr = new SqlExpression();

        Assertions.assertNull(expr.literal());
        Assertions.assertEquals("null", expr.toString());
        Assertions.assertEquals("null", SqlExpression.renderValue(expr));
    }

    @Test
    public void testRenderValue_ConditionWithoutSubQuery_Batch2() {
        String rendered = SqlExpression.renderValue(Filters.eq("id", 1));

        Assertions.assertEquals("id = 1", rendered);
    }

    @Test
    public void testLink_NotEqualAnsiWithNull_Batch2() {
        Assertions.assertEquals("deleted IS NOT NULL", SqlExpression.link(Operator.NOT_EQUAL_ANSI, "deleted", null));
    }

    @Test
    public void testLink_SpaceAndCommaSeparators_Batch2() {
        Assertions.assertEquals("a b", SqlExpression.link(" ", SqlExpression.of("a"), SqlExpression.of("b")));
        Assertions.assertEquals("a, b", SqlExpression.link(", ", SqlExpression.of("a"), SqlExpression.of("b")));
    }

    @Test
    public void testToString_FunctionNameWithNamingPolicy_Batch2() {
        SqlExpression expr = SqlExpression.of("SUM(totalAmount)");

        Assertions.assertEquals("SUM(total_amount)", expr.toSql(NamingPolicy.SNAKE_CASE));
        Assertions.assertEquals("SUM(totalAmount)", expr.toSql(null));
    }

    @Test
    public void testToStringDoesNotConvertSqlKeywordLiterals() {
        assertEquals("CURRENT_DATE", SqlExpression.of("CURRENT_DATE").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CURRENT_TIMESTAMP", SqlExpression.of("CURRENT_TIMESTAMP").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CURRENT_DATE = created_date", SqlExpression.of("CURRENT_DATE = createdDate").toSql(NamingPolicy.SNAKE_CASE));
    }

    /**
     * Lower-case identifiers that collide with a SQL keyword (e.g. a column literally named {@code order}
     * or {@code count}) must still be converted by the naming policy. Only the canonical upper-case keyword
     * form is preserved; the lower-case form is treated as an ordinary identifier.
     */
    @Test
    public void testToStringConvertsLowercaseKeywordLikeColumnNames() {
        assertEquals("ORDER", SqlExpression.of("order").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("COUNT", SqlExpression.of("count").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("ROWNUM", SqlExpression.of("rownum").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        // The upper-case keyword form is still preserved.
        assertEquals("CURRENT_DATE", SqlExpression.of("CURRENT_DATE").toSql(NamingPolicy.CAMEL_CASE));
    }

    /**
     * Regression (Pass 2): operator-to-comparison method mapping is one-to-one and
     * uses the SQL token that matches the method name. Catches potential copy/paste
     * defects where {@code greaterThanOrEqual} could accidentally emit {@code <=}, etc.
     */
    @Test
    public void testComparisonOperatorTokensAreCorrect_Pass2() {
        Assertions.assertEquals("age = 18", SqlExpression.equal("age", 18));
        Assertions.assertEquals("age != 18", SqlExpression.notEqual("age", 18));
        Assertions.assertEquals("age > 18", SqlExpression.greaterThan("age", 18));
        Assertions.assertEquals("age >= 18", SqlExpression.greaterThanOrEqual("age", 18));
        Assertions.assertEquals("age < 18", SqlExpression.lessThan("age", 18));
        Assertions.assertEquals("age <= 18", SqlExpression.lessThanOrEqual("age", 18));
    }

    /**
     * Regression (Pass 2): {@code SqlExpression.between(prop, min, max)} must emit
     * {@code prop BETWEEN min AND max} in that exact order regardless of value type,
     * not {@code prop BETWEEN max AND min}.
     */
    @Test
    public void testBetweenArgumentOrder_Pass2() {
        Assertions.assertEquals("age BETWEEN 18 AND 65", SqlExpression.between("age", 18, 65));
        Assertions.assertEquals("created BETWEEN '2024-01-01' AND '2024-12-31'", SqlExpression.between("created", "2024-01-01", "2024-12-31"));
    }

    // ---------------------------------------------------------------------
    // Third-pass review: SQL-escaping / injection-vector regression tests.
    // ---------------------------------------------------------------------

    /**
     * Regression (Pass 3): {@link SqlExpression#renderValue(Object)} must reject NaN / Infinity
     * because they have no portable SQL literal form (the previous behavior emitted a bare
     * {@code NaN} or {@code Infinity} token that most dialects reject).
     */
    @Test
    public void testRenderValue_RejectsNaNAndInfinity_Pass3() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(Double.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(Double.NEGATIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(Float.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SqlExpression.renderValue(Float.POSITIVE_INFINITY));
    }

    /** SQL-standard literal rendering preserves backslashes instead of changing the value. */
    @Test
    public void testRenderValue_TrailingBackslashIsPreservedForStandardSql() {
        String input = "x" + (char) 92; // x followed by one backslash
        String result = SqlExpression.renderValue(input);
        Assertions.assertEquals("'x\\'", result);
    }

    /**
     * Regression (Pass 3): non-numeric, non-string objects (Date, LocalDateTime, etc.) must be
     * wrapped in single quotes after going through {@code N.stringOf}, not concatenated bare
     * via {@code Object.toString()}.
     */
    @Test
    public void testRenderValue_DateLikeValuesAreQuoted_Pass3() {
        String dateResult = SqlExpression.renderValue(new java.util.Date(0L));
        Assertions.assertNotNull(dateResult);
        Assertions.assertTrue(dateResult.startsWith("'") && dateResult.endsWith("'"), "Date literal must be quoted, got: " + dateResult);

        String ldtResult = SqlExpression.renderValue(java.time.LocalDateTime.of(2024, 1, 1, 0, 0));
        Assertions.assertNotNull(ldtResult);
        Assertions.assertTrue(ldtResult.startsWith("'") && ldtResult.endsWith("'"), "LocalDateTime literal must be quoted, got: " + ldtResult);
        Assertions.assertTrue(ldtResult.contains("2024"));
    }

    /**
     * Regression: the short-literal fast path in {@code toSql(NamingPolicy)} must use a full-string
     * (anchored) match and not {@code Matcher.find()}. With {@code find()}, a short literal
     * containing a trailing line terminator such as {@code "col\n"} spuriously satisfies the
     * anchored pattern {@code ^[a-zA-Z0-9_-]+$} (because {@code $} matches before the final
     * {@code \n}), so the entire literal is wrongly treated as a single column identifier and
     * returned verbatim instead of being routed through the SQL parser (which collapses
     * whitespace runs into a single space token). With the correct {@code matches()} check the
     * literal goes through the parser path, so the newline is normalized to a space and the
     * output must not contain any line terminator.
     */
    @Test
    public void testToString_ShortLiteralWithNewlineGoesThroughParser() {
        SqlExpression expr = SqlExpression.of("col\n");

        String result = expr.toSql(NamingPolicy.NO_CHANGE);

        // Buggy (find()) path returned "col\n" verbatim; correct (matches()) path parses it
        // and collapses the whitespace run, so no line terminator may survive.
        Assertions.assertFalse(result.contains("\n"),
                "Short literal with trailing newline must be parsed (whitespace collapsed), got: " + result.replace("\n", "\\n"));
        Assertions.assertTrue(result.startsWith("col"), "Column token must be preserved, got: " + result);
    }

    /**
     * {@code toSql(NamingPolicy)} must return the value rendered for the <i>requested</i> policy.
     * A single expression must answer each naming policy correctly when callers alternate policies.
     * This guards against unsafe per-instance memoization.
     */
    @Test
    public void testToStringReturnsValuePerNamingPolicy() {
        SqlExpression expr = SqlExpression.of("firstName");

        for (int i = 0; i < 1000; i++) {
            assertEquals("firstName", expr.toSql(NamingPolicy.NO_CHANGE));
            assertEquals("first_name", expr.toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("FIRST_NAME", expr.toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("firstName", expr.toSql(null)); // null defaults to NO_CHANGE
        }
    }

    /**
     * {@code toSql(NamingPolicy)} must be thread-safe on a shared instance: every call must
     * return the value for its own policy. (Originally a regression test for a data race in the
     * since-removed single-slot toString cache; kept — with a lighter workload — to catch any future
     * reintroduction of unsafe per-instance memoization.)
     */
    @Test
    public void testToStringThreadSafeAcrossNamingPolicies() throws InterruptedException {
        final SqlExpression expr = SqlExpression.of("firstName");

        final NamingPolicy[] policies = { NamingPolicy.NO_CHANGE, NamingPolicy.SNAKE_CASE, NamingPolicy.SCREAMING_SNAKE_CASE };
        final String[] expected = { "firstName", "first_name", "FIRST_NAME" };

        final int threadCount = 12;
        final int iterations = 5_000;
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<String> firstError = new AtomicReference<>();
        final Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int idx = t % policies.length;
            final NamingPolicy policy = policies[idx];
            final String want = expected[idx];

            threads[t] = new Thread(() -> {
                try {
                    start.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int i = 0; i < iterations && firstError.get() == null; i++) {
                    final String got = expr.toSql(policy);

                    if (!want.equals(got)) {
                        firstError.compareAndSet(null, "policy=" + policy + " expected=" + want + " got=" + got);
                        return;
                    }
                }
            });
            threads[t].start();
        }

        start.countDown();

        for (final Thread thread : threads) {
            thread.join();
        }

        Assertions.assertNull(firstError.get(), "toSql(NamingPolicy) returned a value rendered for the wrong policy under concurrency: " + firstError.get());
    }

    @Test
    public void testToStringNamingPolicyPreservesHyphenInShortLiteral() {
        // "price-tax" is SQL subtraction. The short-literal fast path used to hand the whole
        // literal to NamingPolicy.convert, which swallowed the '-' (CAMEL_CASE -> "priceTax");
        // hyphen-containing literals must take the parser path, converting each operand independently.
        assertEquals("price-tax", SqlExpression.of("price-tax").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("unit_price-tax", SqlExpression.of("unitPrice-tax").toSql(NamingPolicy.SNAKE_CASE));

        // Consistent with the >= 16-char parse path, which always preserved the '-'.
        assertEquals("basePrice-salesTax", SqlExpression.of("base_price-sales_tax").toSql(NamingPolicy.CAMEL_CASE));

        // Hyphen-free short literals still use the fast path unchanged.
        assertEquals("first_name", SqlExpression.of("firstName").toSql(NamingPolicy.SNAKE_CASE));

        // The digit-leading pass-through guard is still intact.
        assertEquals("2faCode", SqlExpression.of("2faCode").toSql(NamingPolicy.SNAKE_CASE));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testVariadicHelpersTreatNullArrayAsNoOperands() {
        assertEquals("", SqlExpression.and((String[]) null));
        assertEquals("", SqlExpression.or((String[]) null));
        assertEquals("", SqlExpression.plus((Object[]) null));
        assertEquals("", SqlExpression.subtract((Object[]) null));
        assertEquals("", SqlExpression.minus((Object[]) null));
        assertEquals("", SqlExpression.multiply((Object[]) null));
        assertEquals("", SqlExpression.divide((Object[]) null));
        assertEquals("", SqlExpression.modulus((Object[]) null));
        assertEquals("", SqlExpression.leftShift((Object[]) null));
        assertEquals("", SqlExpression.rightShift((Object[]) null));
        assertEquals("", SqlExpression.bitwiseAnd((Object[]) null));
        assertEquals("", SqlExpression.bitwiseOr((Object[]) null));
        assertEquals("", SqlExpression.bitwiseXor((Object[]) null));
    }

    @Test
    public void testBooleanCombinatorsRejectNullOrBlankOperands() {
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.and("a = 1", null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.and("a = 1", ""));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.or("a = 1", " \t"));
        assertEquals("(a = 1)", SqlExpression.and("a = 1"));
        assertEquals("", SqlExpression.or());
    }

    @Test
    public void testNamingPolicyConvertsUnderscoreLeadingIdentifiers() {
        // Leading/trailing underscore runs are preserved and only the part between them is converted
        // (QueryUtil.convertIdentifier), even though NamingPolicy.convert itself drops such runs.
        assertEquals("_first_name", SqlExpression.of("_firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_FIRST_NAME = OTHER_VALUE", SqlExpression.of("_firstName = otherValue").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("first_name_ = other_value", SqlExpression.of("firstName_ = otherValue").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_firstName", SqlExpression.of("_firstName").toSql(NamingPolicy.CAMEL_CASE));
    }

    @Test
    public void testNamingPolicyKeepsUnderscoreOnlyPrefixedIdentifiers() {
        // "_1" must NOT collapse to "1" (which would turn "_1 = 1" into an always-true predicate),
        // and "__x" must keep both underscores.
        assertEquals("_1 = 1", SqlExpression.of("_1 = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_1", SqlExpression.of("_1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("__x", SqlExpression.of("__x").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("__x = 1", SqlExpression.of("__x = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("__X = 1", SqlExpression.of("__x = 1").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("__", SqlExpression.of("__").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testStaticHelpersRejectNullOrBlankExpr() {
        // the expr/column side must never render as the text "null"
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.equal(null, 1));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.notEqual(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.greaterThan("", 1));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.between(null, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.like(null, "a%"));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNull(null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNull(" "));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.isNotNull(""));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.count(null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.upper(""));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.concat("a", null));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.log(" ", "100"));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.lpad("id", 10, " "));

        // a null VALUE is still meaningful and renders the documented null-aware forms
        assertEquals("middle_name IS NULL", SqlExpression.equal("middle_name", null));
        assertEquals("email IS NOT NULL", SqlExpression.notEqual("email", null));
        // pre-quoted literal arguments (including the empty literal '') remain valid
        assertEquals("REPLACE(phone, '-', '')", SqlExpression.replace("phone", "'-'", "''"));
        assertEquals("COUNT(*)", SqlExpression.count("*"));
    }

    @Test
    public void testNamingPolicyDoesNotModifyPrefixedStringLiterals() {
        assertEquals("N'camelCase' = first_name", SqlExpression.of("N'camelCase' = firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_utf8mb4'camelCase' = OTHER_VALUE", SqlExpression.of("_utf8mb4'camelCase' = otherValue").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
    }

    @Test
    public void testNamingPolicyDoesNotRenameSqlVariables() {
        assertEquals("@firstName + @@session.sqlMode + column_name",
                SqlExpression.of("@firstName + @@session.sqlMode + columnName").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testNamingPolicyPreservesPostgreSqlCastOperator() {
        assertEquals("payload_data::jsonb", SqlExpression.of("payloadData::jsonb").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals(":payload::jsonb", SqlExpression.of(":payload::jsonb").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testRenderValue_UsesSqlStandardQuoteDoubling() {
        assertEquals("'O''Brien'", SqlExpression.renderValue("O'Brien"));
        assertEquals("'say \"hi\"'", SqlExpression.renderValue("say \"hi\""));
        assertEquals("'a\\''b'", SqlExpression.renderValue("a\\'b"));
    }

    @Test
    public void testNullValueRendersAsLiteralNullForNonEqualityOperators() {
        // Only EQUAL and NOT_EQUAL substitute the IS [NOT] NULL form for a null value;
        // every other operator renders it via renderValue(null) as the literal "null".
        assertEquals("name LIKE null", SqlExpression.like("name", null));
        assertEquals("age > null", SqlExpression.greaterThan("age", null));
        assertEquals("age BETWEEN null AND 65", SqlExpression.between("age", null, 65));
    }

    // Underscore-containing niladic keyword functions are indistinguishable from snake_case column names,
    // so each must be registered explicitly or a naming policy rewrites it into an identifier -- e.g.
    // CURRENT_ROLE became "currentRole" under CAMEL_CASE and the un-parseable "current-role" under KEBAB_CASE.
    @Test
    public void testNiladicKeywordFunctionsSurviveEveryNamingPolicy() {
        final String[] keywords = { "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME", "CURRENT_TIMESTAMP",
                "CURRENT_USER", "LOCALTIME", "LOCALTIMESTAMP", "SESSION_USER", "SYSTEM_USER", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP" };

        for (final String keyword : keywords) {
            for (final NamingPolicy policy : NamingPolicy.values()) {
                assertEquals(keyword, SqlExpression.of(keyword).toSql(policy), keyword + " must not be converted by " + policy);
            }
        }
    }

    // NULL comes from SK and was always preserved, but NAN/INFINITE/UNKNOWN were not registered, so the
    // builder path rendered "IS nan" / "IS NOT infinite" / "IS unknown" while Condition.toString() rendered
    // them in canonical case -- two spellings for the same condition.
    @Test
    public void testIsPredicateOperandsAreRecognizedAsKeywords() {
        for (final String keyword : new String[] { "NULL", "NAN", "INFINITE", "UNKNOWN" }) {
            for (final NamingPolicy policy : NamingPolicy.values()) {
                assertEquals(keyword, SqlExpression.of(keyword).toSql(policy), keyword + " must not be converted by " + policy);
            }
        }
    }

    @Test
    public void testRenderValueUsesConditionSqlInsteadOfDiagnosticToString() {
        final Condition condition = new Condition() {
            @Override
            public Operator operator() {
                return Operator.EQUAL;
            }

            @Override
            public com.landawn.abacus.util.ImmutableList<Object> parameters() {
                return com.landawn.abacus.util.ImmutableList.empty();
            }

            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                assertEquals(NamingPolicy.NO_CHANGE, namingPolicy);
                return "firstName = 'Ada'";
            }

            @Override
            public String toString() {
                return "Diagnostic condition";
            }
        };
        final SubQuery subQuery = new SubQuery("SELECT firstName FROM people") {
            @Override
            public String toString() {
                return "Diagnostic subquery";
            }
        };

        assertEquals("firstName = 'Ada'", SqlExpression.renderValue(condition));
        assertEquals("(SELECT firstName FROM people)", SqlExpression.renderValue(subQuery));
        assertEquals("firstName = (SELECT firstName FROM people)", SqlExpression.equal("firstName", subQuery));
    }

    /**
     * Pins the documented tokenizer behaviour: a MySQL-style {@code #} line comment (and the rest of the
     * line) is stripped from an expression literal under every naming policy, while the PostgreSQL JSON
     * operators, the MyBatis marker and a {@code FROM}/{@code JOIN} temp-table reference are preserved.
     */
    @Test
    public void testToSqlStripsHashLineCommentButKeepsHashOperators() {
        // preserved forms
        assertEquals("data #> '{a}'", Filters.expr("data #> '{a}'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("data #>> '{a}'", Filters.expr("data #>> '{a}'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("data #- '{a}'", Filters.expr("data #- '{a}'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("t.id = #{id}", Filters.expr("t.id = #{id}").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("id IN (SELECT id FROM #tmp)", Filters.expr("id IN (SELECT id FROM #tmp)").toSql(NamingPolicy.NO_CHANGE));

        // a bare '#' starts a line comment: it and the rest of the line are dropped (note the trailing space
        // left by the comment removal), so PostgreSQL's '#' bitwise-XOR operator cannot be used here
        assertEquals("x = 1 ", Filters.expr("x = 1 # c").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("flags ", Filters.expr("flags # 1 = 0").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("flags ", Filters.expr("flags # 1 = 0").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("", Filters.expr("#tmp.id = x.id").toSql(NamingPolicy.NO_CHANGE));

        Assertions.assertDoesNotThrow(() -> new Where(Filters.expr("#tmp.id = x.id")));
    }

    @Test
    public void testCommentOnlyExpressionsCannotBecomePredicatesOrScalarOperands() {
        for (final String literal : new String[] { "-- comment", "/* comment */", "# comment", " /* first */ -- second\n /* third */ ",
                "-- Keep comments\n/* only comment */" }) {
            final SqlExpression expression = SqlExpression.of(literal);
            assertThrows(IllegalArgumentException.class, () -> new Where(expression));
            assertThrows(IllegalArgumentException.class, () -> new On(expression));
            assertThrows(IllegalArgumentException.class, () -> new And(expression));
            assertThrows(IllegalArgumentException.class, () -> new Not(expression));
            assertThrows(IllegalArgumentException.class, () -> new Equal("value", expression));
            assertThrows(IllegalArgumentException.class, () -> new Between("value", expression, 10));
            assertThrows(IllegalArgumentException.class, () -> new In("value", java.util.List.of(expression)));
            assertThrows(IllegalArgumentException.class, () -> new SubQuery("records", "id", expression));
        }

        assertEquals("WHERE '-- comment' = '-- comment'", new Where(SqlExpression.of("'-- comment' = '-- comment'")).toString());
        assertEquals("WHERE value = 1", new Where(SqlExpression.of("/* comment */value = 1")).toString());
        assertEquals("value = '/* comment */'", new Equal("value", SqlExpression.of("'/* comment */'")).toString());
    }

    @Test
    public void testRejectedCommentOnlyPredicateLeavesBuilderUsable() {
        final com.landawn.abacus.query.SqlBuilder builder = com.landawn.abacus.query.Dsl.PSC.select("id").from("records");
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.expr("/* only comment */")));
        final com.landawn.abacus.query.SqlBuilder.SP result = builder.where(Filters.eq("id", 1)).build();
        assertEquals("SELECT id FROM records WHERE id = ?", result.query());
        assertEquals(List.of(1), result.parameters());
    }

    @Test
    public void testSqlServerTemporaryTablePredicatesRemainAccepted() {
        final com.landawn.abacus.query.Dsl sqlServer = com.landawn.abacus.query.Dsl.forDialect(com.landawn.abacus.query.Dsl.PSC.sqlDialect()
                .toBuilder().productInfo(com.landawn.abacus.query.SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        for (final String table : List.of("#tmp", "##tmp")) {
            final String predicate = table + ".id = 1";
            final String rawQuery = sqlServer.select("*").from(table).where(predicate).build().query();
            final String conditionQuery = sqlServer.select("*").from(table).where(Filters.expr(predicate)).build().query();
            assertEquals("SELECT * FROM " + table + " WHERE " + predicate, conditionQuery);
            assertEquals(rawQuery, conditionQuery);
        }

        // The retained forms use the builders' temporary-name characters, including '@' and non-ASCII letters.
        for (final String predicate : List.of("#@t.id = 1", "#übersicht.id = 1")) {
            Assertions.assertDoesNotThrow(() -> new Where(Filters.expr(predicate)));
            assertEquals("SELECT * FROM t WHERE " + predicate, sqlServer.select("*").from("t").where(Filters.expr(predicate)).build().query());
        }

        // A comment before the name cannot render on any builder: SQL Server rejects a temporary-table
        // expression containing a comment token, and other dialects read the '#' as a comment.
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("/* c */ #tmp.id = 1")));
        assertThrows(IllegalArgumentException.class, () -> sqlServer.select("*").from("#tmp").where("/* c */ #tmp.id = 1"));
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("#\u3000tmp.id = 1")));
    }

    @Test
    public void testUnterminatedMybatisMarkerIsConvertedLikeOrdinarySql() {
        assertEquals("foo_bar = #{ foo_bar AND bar_baz = 1", Filters.expr("fooBar = #{ fooBar AND barBaz = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("foo_bar = #{ fooBar } AND bar_baz = 1", Filters.expr("fooBar = #{ fooBar } AND barBaz = 1").toSql(NamingPolicy.SNAKE_CASE));
    }

    /**
     * Pins the documented rule that parameter placeholders inside an expression literal are left
     * unchanged by the naming policy, unlike the identifiers around them.
     */
    @Test
    public void testToSqlLeavesNamedPlaceholdersUnconverted() {
        assertEquals("firstName = :firstName", Filters.expr("first_name = :firstName").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("firstName = #{firstName}", Filters.expr("first_name = #{firstName}").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("firstName = ${firstName}", Filters.expr("first_name = ${firstName}").toSql(NamingPolicy.CAMEL_CASE));

        assertEquals("first_name = :firstName AND last_name = #{lastName}",
                Filters.expr("firstName = :firstName AND lastName = #{lastName}").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = ?", Filters.expr("firstName = ?").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = @firstName", Filters.expr("firstName = @firstName").toSql(NamingPolicy.SNAKE_CASE));

        assertEquals("first_name = #{ firstName }", Filters.expr("firstName = #{ firstName }").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = ${ firstName }", Filters.expr("firstName = ${ firstName }").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = #{firstName, jdbcType=VARCHAR}",
                Filters.expr("firstName = #{firstName, jdbcType=VARCHAR}").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = #{ firstName, jdbcType = VARCHAR } AND last_name = ${ lastName }",
                Filters.expr("firstName = #{ firstName, jdbcType = VARCHAR } AND lastName = ${ lastName }").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("FIRST_NAME = #{ firstName } + LAST_NAME",
                Filters.expr("firstName = #{ firstName } + lastName").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("SELECT id FROM records WHERE first_name = #{ firstName, jdbcType=VARCHAR }",
                com.landawn.abacus.query.Dsl.PSC.select("id").from("records")
                        .where(Filters.expr("firstName = #{ firstName, jdbcType=VARCHAR }")).build().query());
    }

    @Test
    public void testToSqlPreservesDelimitedSegmentOfQualifiedIdentifier() {
        assertEquals("t.\"firstName\" = 1", SqlExpression.of("t.\"firstName\" = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("T.\"firstName\" = 1", SqlExpression.of("t.\"firstName\" = 1").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("t.[firstName] = 1", SqlExpression.of("t.[firstName] = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.`firstName` = 1", SqlExpression.of("t.`firstName` = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.\"first Name\" = 1", SqlExpression.of("t.\"first Name\" = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.\"aB\"\"c\" = 1", SqlExpression.of("t.\"aB\"\"c\" = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("my_schema.t.\"firstName\" = last_name", SqlExpression.of("mySchema.t.\"firstName\" = lastName").toSql(NamingPolicy.SNAKE_CASE));
        // unchanged neighbours
        assertEquals("\"firstName\" = 1", SqlExpression.of("\"firstName\" = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.first_name = 1", SqlExpression.of("t.firstName = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("user_ids[1] = x", SqlExpression.of("userIds[1] = x").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testCompoundIntervalUnitsAndOracleFloatTypesAreKeywords() {
        assertEquals("DATE_ADD(createdAt, interval '1-2' YEAR_MONTH) > NOW()",
                SqlExpression.of("DATE_ADD(created_at, INTERVAL '1-2' YEAR_MONTH) > NOW()").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("YEAR_MONTH", SqlExpression.of("YEAR_MONTH").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CAST(amount AS BINARY_DOUBLE)", SqlExpression.of("CAST(amount AS BINARY_DOUBLE)").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CAST(amount AS BINARY_FLOAT)", SqlExpression.of("CAST(amount AS BINARY_FLOAT)").toSql(NamingPolicy.CAMEL_CASE));

        for (final String unit : new String[] { "DAY_HOUR", "DAY_MINUTE", "DAY_SECOND", "DAY_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND",
                "HOUR_MICROSECOND", "MINUTE_SECOND", "MINUTE_MICROSECOND", "SECOND_MICROSECOND", "YEAR_MONTH" }) {
            assertEquals(unit, SqlExpression.of(unit).toSql(NamingPolicy.CAMEL_CASE), unit);
            assertEquals(unit, SqlExpression.of(unit).toSql(NamingPolicy.KEBAB_CASE), unit);
            assertTrue(SqlExpression.of("EXTRACT(" + unit + " FROM ts)").toSql(NamingPolicy.CAMEL_CASE).contains(unit), unit);
        }

        // Lower-case forms are still identifiers converted by the naming policy (keywords are registered upper-case only).
        assertEquals("yearMonth", SqlExpression.of("year_month").toSql(NamingPolicy.CAMEL_CASE));
    }

    // Regression: a token with a glued subscript, array constructor or marker (ARRAY[1, 2, 3], myTags[:tagIndex],
    // log_${yearMonth}) was converted as one identifier: spaces became '_', bind markers and functions were renamed.
    @Test
    public void testGluedSubscriptOrArrayConstructorConvertsOnlyTheLeadingName() {
        assertEquals("id = ANY(ARRAY[1, 2, 3])", SqlExpression.of("id = ANY(ARRAY[1, 2, 3])").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("id = ANY(ARRAY[1, 2, 3])", SqlExpression.of("id = ANY(ARRAY[1, 2, 3])").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("tags && ARRAY[:tagA, :tagB]", SqlExpression.of("tags && ARRAY[:tagA, :tagB]").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("my_tags[:tagIndex] = 'x'", SqlExpression.of("myTags[:tagIndex] = 'x'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("my_tags[#{tagIndex}] = 'x'", SqlExpression.of("myTags[#{tagIndex}] = 'x'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("log_${yearMonth}.created_at > 0", SqlExpression.of("log_${yearMonth}.createdAt > 0").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("scores[idx + 1] > 5", SqlExpression.of("scores[idx + 1] > 5").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("scores[CURRENT_DATE - start_day] > 5", SqlExpression.of("scores[CURRENT_DATE - startDay] > 5").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("tags[array_length(tags, 1)] = 'x'", SqlExpression.of("tags[array_length(tags, 1)] = 'x'").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("payload_data['camelKey'] = 1", SqlExpression.of("payloadData['camelKey'] = 1").toSql(NamingPolicy.SNAKE_CASE));
        // The interior of a subscript or array constructor holds column references and is rendered like an expression.
        assertEquals("ARRAY[first_name, last_name]", SqlExpression.of("ARRAY[firstName, lastName]").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("user_ids[1] = x", SqlExpression.of("userIds[1] = x").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("col::text[] = y", SqlExpression.of("col::text[] = y").toSql(NamingPolicy.SNAKE_CASE));
        // An interior holding a '#' or comment is copied as written instead of being stripped as a comment.
        assertEquals("arr[idx # 2] = 1", SqlExpression.of("arr[idx # 2] = 1").toSql(NamingPolicy.SNAKE_CASE));
        // Prefixed literals, delimited qualified parts and a column after a subscript keep their existing rendering.
        assertEquals("N'camelCase' = first_name", SqlExpression.of("N'camelCase' = firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t.[colName] = y", SqlExpression.of("t.[colName] = y").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: a PostgreSQL cast glued to a column was treated as part of one identifier. unitPrice::numeric(10,2)
    // was skipped as a "function name", and the quoted cast type in orderStatus::"OrderStatus" was rewritten.
    @Test
    public void testGluedCastConvertsTheColumnAndKeepsTheType() {
        assertEquals("unit_price::numeric(10,2) > 5", SqlExpression.of("unitPrice::numeric(10,2) > 5").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("order_status::\"OrderStatus\" = 'NEW'", SqlExpression.of("orderStatus::\"OrderStatus\" = 'NEW'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("payload_data::jsonb", SqlExpression.of("payloadData::jsonb").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals(":payload::jsonb", SqlExpression.of(":payload::jsonb").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: the tokenizer ends a token at a closing delimiter, so the column in "T".firstName arrived as ".firstName"
    // and was never converted, although t."firstName" converts its unquoted qualifier.
    @Test
    public void testColumnAfterDelimitedQualifierIsConverted() {
        assertEquals("\"T\".first_name = 1", SqlExpression.of("\"T\".firstName = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("`t`.first_name = 1", SqlExpression.of("`t`.firstName = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("[t].first_name = 1", SqlExpression.of("[t].firstName = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("\"T\".FIRST_NAME = 1", SqlExpression.of("\"T\".firstName = 1").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        // A schema-qualified function keeps its name; its arguments are still converted.
        assertEquals("\"s\".myFunc(first_name)", SqlExpression.of("\"s\".myFunc(firstName)").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: the rule above also converted the unquoted part of a schema-qualified type or collation, so under
    // CAMEL_CASE id::"types".order_status became id::"types".orderStatus, a type the database does not have.
    @Test
    public void testQualifiedTypeOrCollationAfterDelimitedSchemaIsKept() {
        for (final String expr : new String[] { "id::\"types\".order_status", "id :: \"types\".order_status", "CAST(x AS \"types\".order_status)",
                "x::`types`.order_status", "x::[types].order_status", "x::\"db\".\"types\".order_status", "name collate \"public\".my_collation",
                "name collate [dbo].my_collation" }) {
            assertEquals(expr, SqlExpression.of(expr).toSql(NamingPolicy.CAMEL_CASE), expr);
        }

        // The column before the cast is still converted, the type after it is not.
        assertEquals("\"T\".firstName::\"types\".order_status", SqlExpression.of("\"T\".first_name::\"types\".order_status").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("\"T\".first_name::\"types\".orderStatus", SqlExpression.of("\"T\".firstName::\"types\".orderStatus").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(\"T\".first_name AS int)", SqlExpression.of("CAST(\"T\".firstName AS int)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("x::int = \"T\".first_name", SqlExpression.of("x::int = \"T\".firstName").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: a type or collation name not glued to its cast was converted like a column, so CAMEL_CASE turned
    // CAST(id AS order_status), id :: order_status and id::"types". order_status into ...orderStatus, a type the
    // database does not have.
    @Test
    public void testTypeAndCollationNamesInTypePositionsAreKept() {
        for (final String expr : new String[] { "CAST(id AS order_status)", "TRY_CAST(id AS order_status)", "SAFE_CAST(id AS order_status)",
                "cast (id as order_status)", "CAST(id AS types.order_status)", "CAST(id AS \"types\". order_status)", "CAST(id AS types .order_status)",
                "CAST(id AS numeric(10, 2))", "CAST(id AS double precision)", "CAST(id AS VARCHAR(10))", "CAST(CAST(id AS a_type) AS b_type)",
                "CAST((id + 1) AS order_status)", "CAST(id AS my_type ARRAY)", "id :: order_status", "id:: order_status", "id ::order_status",
                "id :: types.order_status", "id :: types. order_status", "id::types. order_status", "id::\"types\". order_status",
                "id::\"types\" . order_status", "id :: \"types\" .order_status", "id :: my_type[]", "CAST(id AS a_type)::b_type",
                "name collate my_collation", "name collate public. my_collation", "name collate \"public\" . my_collation" }) {
            assertEquals(expr, SqlExpression.of(expr).toSql(NamingPolicy.CAMEL_CASE), expr);
        }

        // Stripped comments collapse to one space, as elsewhere.
        assertEquals("CAST(? AS pg_catalog . \"line\")", SqlExpression.of("CAST(? AS /* schema */ pg_catalog /* dot */ . \"line\")").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CAST(created_at AS DATE)", SqlExpression.of("CAST(createdAt AS DATE)").toSql(NamingPolicy.SNAKE_CASE));

        // Columns around a type, an AS outside a CAST call and the words after a type's first word are still converted.
        assertEquals("CAST(first_name AS myType) = last_name", SqlExpression.of("CAST(firstName AS myType) = lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("COALESCE(CAST(first_name AS myType), last_name)",
                SqlExpression.of("COALESCE(CAST(firstName AS myType), lastName)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(first_name AS INT) AS my_alias", SqlExpression.of("CAST(firstName AS INT) AS myAlias").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("fn(first_name AS last_name)", SqlExpression.of("fn(firstName AS lastName)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name :: myType = last_name", SqlExpression.of("firstName :: myType = lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name :: types.myType = last_name", SqlExpression.of("firstName :: types.myType = lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name collate \"C\" ASC, last_name", SqlExpression.of("firstName collate \"C\" ASC, lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(#{p} AS myType) = last_name", SqlExpression.of("CAST(#{p} AS myType) = lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(arr[first_name] AS myType)", SqlExpression.of("CAST(arr[firstName] AS myType)").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: a part that glued a second cast (int:: in id :: int:: order_status) ended the name, so the second type was
    // converted; and every CAST clause after the type was copied as part of it, so the column in BigQuery's
    // AT TIME ZONE timeZone or Oracle's DEFAULT fallbackDate ON CONVERSION ERROR was no longer converted.
    @Test
    public void testChainedCastAndCastClauseAfterType() {
        for (final String expr : new String[] { "id :: int:: order_status", "id :: int::\"types\". order_status", "id :: int :: order_status",
                "id::int:: order_status", "CAST(x AS STRUCT<format STRING, first_name INT64>)", "CAST(x AS ARRAY<STRUCT<a_b INT64>>)" }) {
            assertEquals(expr, SqlExpression.of(expr).toSql(NamingPolicy.CAMEL_CASE), expr);
        }

        assertEquals("CAST(event_time AS STRING format 'YYYY' at time zone time_zone)",
                SqlExpression.of("CAST(eventTime AS STRING format 'YYYY' at time zone timeZone)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(event_time AS TIMESTAMP format fmt_col)", SqlExpression.of("CAST(eventTime AS TIMESTAMP format fmtCol)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(x AS DATE default fallback_date on conversion error, 'DD-MM-YYYY')",
                SqlExpression.of("CAST(x AS DATE default fallbackDate on conversion error, 'DD-MM-YYYY')").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(x AS DATE, fmt_col)", SqlExpression.of("CAST(x AS DATE, fmtCol)").toSql(NamingPolicy.SNAKE_CASE));
        // A ',' inside the type's own parentheses or angle brackets does not end it.
        assertEquals("CAST(x AS numericType(10, 2)) = last_name", SqlExpression.of("CAST(x AS numericType(10, 2)) = lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(x AS STRUCT<a INT64, firstName STRING>) = last_name",
                SqlExpression.of("CAST(x AS STRUCT<a INT64, firstName STRING>) = lastName").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: a schema named format or at was read as the FORMAT / AT TIME ZONE clause that ends a CAST type, so the type
    // after it was converted (CAST(id AS format . order_status) -> format . orderStatus under CAMEL_CASE).
    @Test
    public void testKeywordNamedSchemaInCastTypeIsNotAClause() {
        for (final String expr : new String[] { "CAST(id AS format . order_status)", "CAST(id AS at . order_status)", "CAST(id AS format. order_status)",
                "CAST(id AS format.order_status)", "CAST(id AS types. format)", "CAST(id AS types . at)", "CAST(id AS format)",
                "CAST(id AS \"format\" . order_status)" }) {
            assertEquals(expr, SqlExpression.of(expr).toSql(NamingPolicy.CAMEL_CASE), expr);
        }

        // Stripped comments collapse to one space.
        assertEquals("CAST(id AS at . order_status)", SqlExpression.of("CAST(id AS at /* c */ . order_status)").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("CAST(id AS format . order_status)", SqlExpression.of("CAST(id AS format /* c */ . /* d */ order_status)").toSql(NamingPolicy.CAMEL_CASE));

        // After a complete type name, qualified or not, the words still start a clause.
        assertEquals("CAST(event_time AS types . my_type at time zone time_zone)",
                SqlExpression.of("CAST(eventTime AS types . my_type at time zone timeZone)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("CAST(event_time AS format . my_type format fmt_col)",
                SqlExpression.of("CAST(eventTime AS format . my_type format fmtCol)").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: the tokenizer reads \' as an escaped quote, but renderValue emits standard literals ('C:\'). The quote
    // boundaries after such a literal were off by one: a later string value was converted as an identifier, and a "--"
    // inside a later string was dropped as a comment (truncating the SQL even under NO_CHANGE).
    @Test
    public void testEscapeDependentQuoteKeepsTheRestOfTheExpressionVerbatim() {
        final String truncated = SqlExpression.and(SqlExpression.eq("path", "C:\\"), SqlExpression.eq("note", "-- n/a"));
        assertEquals("(path = 'C:\\') AND (note = '-- n/a')", truncated);
        assertEquals("(path = 'C:\\') AND (note = '-- n/a')", SqlExpression.of(truncated).toSql(NamingPolicy.NO_CHANGE));
        assertEquals("(path = 'C:\\') AND (note = '-- n/a')", SqlExpression.of(truncated).toString());
        assertEquals("(path = 'C:\\') AND (note = '-- n/a')", SqlExpression.of(truncated).toSql(NamingPolicy.SNAKE_CASE));

        // Conversion stops at the escape-dependent literal; the string data after it is never rewritten.
        final String like = SqlExpression.and(SqlExpression.like("fileName", "%\\"), SqlExpression.eq("ownerName", "John Smith"));
        assertEquals("(file_name LIKE '%\\') AND (ownerName = 'John Smith')", SqlExpression.of(like).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("(FILE_NAME LIKE '%\\') AND (ownerName = 'John Smith')", SqlExpression.of(like).toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("file_name LIKE 'a\\_%' escape '\\' AND ownerName = 'John Smith'",
                SqlExpression.of("fileName LIKE 'a\\_%' escape '\\' AND ownerName = 'John Smith'").toSql(NamingPolicy.SNAKE_CASE));

        // Backslashes that both readings agree on are rendered normally: an escaped backslash, or a PostgreSQL E'...' string.
        assertEquals("first_name = 'a\\\\' AND last_name = 'b'", SqlExpression.of("firstName = 'a\\\\' AND lastName = 'b'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("x = E'It\\'s' AND first_name = 'x'", SqlExpression.of("x = E'It\\'s' AND firstName = 'x'").toSql(NamingPolicy.SNAKE_CASE));

        // The verbatim text starts exactly at the escape-dependent token, also after stripped comments.
        assertEquals("a_b = 'C:\\' AND firstName = 1", SqlExpression.of("aB /* x 'C:\\' */ = 'C:\\' AND firstName = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("a_b = 1 AND c_d = 'C:\\' AND eF = 1",
                SqlExpression.of("aB = 1 # note 'x\n AND cD = 'C:\\' AND eF = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = N'C:\\' AND lastName = 'x'", SqlExpression.of("firstName = N'C:\\' AND lastName = 'x'").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression guard for the glued-subscript fix: interiors are rendered recursively only to a fixed depth, so deeply
    // nested subscripts cannot blow the stack; deeper interiors are copied as written.
    @Test
    public void testDeeplyNestedGluedSubscriptsRenderWithBoundedDepth() {
        final StringBuilder expr = new StringBuilder("x");

        for (int i = 0; i < 12; i++) {
            expr.append("[aB");
        }

        expr.append("]".repeat(12));

        final String rendered = SqlExpression.of(expr.toString()).toSql(NamingPolicy.SNAKE_CASE);

        assertEquals("x" + "[a_b".repeat(8) + "[aB".repeat(4) + "]".repeat(12), rendered);
    }

    @Test
    public void testRenderValueUsesLocalWallClockTextForDateFamilyValues() {
        // Regression: java.util.Date-family values were rendered through N.stringOf as UTC instants
        // ('2020-01-02T08:00:00.000Z' for the local date 2020-01-02 in a UTC-8 JVM), shifting the inlined value.
        assertEquals("'2020-01-02'", SqlExpression.renderValue(java.sql.Date.valueOf("2020-01-02")));
        assertEquals("'03:04:05'", SqlExpression.renderValue(java.sql.Time.valueOf("03:04:05")));
        assertEquals("'2020-01-02 03:04:05.123'", SqlExpression.renderValue(java.sql.Timestamp.valueOf("2020-01-02 03:04:05.123")));
        assertEquals("'2020-01-02 03:04:05.0'",
                SqlExpression.renderValue(new java.util.Date(java.sql.Timestamp.valueOf("2020-01-02 03:04:05").getTime())));

        final java.util.Calendar calendar = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
        calendar.clear();
        calendar.set(2020, java.util.Calendar.JANUARY, 2, 3, 4, 5);
        // A Calendar is bound as new Timestamp(getTimeInMillis()), i.e. at its instant in the JVM zone, so it renders that way.
        assertEquals("'" + new java.sql.Timestamp(calendar.getTimeInMillis()) + "'", SqlExpression.renderValue(calendar));

        final java.util.TimeZone defaultZone = java.util.TimeZone.getDefault();

        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
            // 2020-01-02 03:04:05 in Tokyo is 2020-01-01 13:04:05 in New York; the calendar's own zone fields are not used.
            assertEquals("'2020-01-01 13:04:05.0'", SqlExpression.renderValue(calendar));
        } finally {
            java.util.TimeZone.setDefault(defaultZone);
        }

        assertEquals("d = '2020-01-02'", Filters.eq("d", java.sql.Date.valueOf("2020-01-02")).toString());
        assertEquals("'2020-01-02'", SqlExpression.renderValue(java.time.LocalDate.of(2020, 1, 2)));
    }

    @Test
    public void testCollationNameAfterCollateIsNotConverted() {
        // Regression: under CAMEL_CASE the collation name was converted like a column (utf8mb4_bin -> utf8mb4Bin).
        final String rendered = SqlExpression.of("first_name COLLATE utf8mb4_bin = 'x'").toSql(NamingPolicy.CAMEL_CASE);
        assertTrue(rendered.contains("firstName"), rendered);
        assertTrue(rendered.contains(" utf8mb4_bin "), rendered);

        assertTrue(SqlExpression.of("last_name COLLATE Latin1_General_CS_AS").toSql(NamingPolicy.CAMEL_CASE).endsWith(" Latin1_General_CS_AS"));
    }

    // Regression: any backslash before any quote character switched the rest of the expression to verbatim, although only a
    // backslash before the closing quote of its own region (\' in '...', \" in "...") makes the two readings disagree.
    // A JSON literal with \" left every later column unconverted.
    @Test
    public void testBackslashBeforeAnotherKindOfQuoteKeepsConverting() {
        final NamingPolicy snake = NamingPolicy.SNAKE_CASE;

        assertEquals("payload @> '{\"name\":\"say \\\"hi\\\"\"}' AND created_at > 1",
                SqlExpression.of("payload @> '{\"name\":\"say \\\"hi\\\"\"}' AND createdAt > 1").toSql(snake));
        assertEquals("payload @> '{\"a\":\"b\\\"c\"}' AND created_at > 1", SqlExpression.of("payload @> '{\"a\":\"b\\\"c\"}' AND createdAt > 1").toSql(snake));
        assertEquals("note = 'a \\\" b' AND created_at > 1", SqlExpression.of("note = 'a \\\" b' AND createdAt > 1").toSql(snake));
        assertEquals("\"it\\'s\" = 1 AND created_at > 1", SqlExpression.of("\"it\\'s\" = 1 AND createdAt > 1").toSql(snake));
        assertEquals("path = 'C:\\Temp\\\"x\"' AND created_at > 1", SqlExpression.of("path = 'C:\\Temp\\\"x\"' AND createdAt > 1").toSql(snake));
        // A value rendered by the library itself.
        assertEquals("(payload = '{\"name\":\"say \\\"hi\\\"\"}') AND (created_at > 1)",
                SqlExpression.of(SqlExpression.and(SqlExpression.eq("payload", "{\"name\":\"say \\\"hi\\\"\"}"), SqlExpression.gt("createdAt", 1))).toSql(snake));
        // An E'...' string inside a subscript honors backslash escapes under both readings, like a top-level one.
        assertEquals("arr[E'a\\'b' || first_name] = 1", SqlExpression.of("arr[E'a\\'b' || firstName] = 1").toSql(snake));

        // A backslash before the region's own closing quote still stops conversion at that token.
        assertEquals("\"col\\\"x\" = 1 AND aB = 1", SqlExpression.of("\"col\\\"x\" = 1 AND aB = 1").toSql(snake));
        assertEquals("first_name = '\\\\\\'' AND lastName = 1", SqlExpression.of("firstName = '\\\\\\'' AND lastName = 1").toSql(snake));
        assertEquals("first_name = 'it\\'s' AND lastName = 1", SqlExpression.of("firstName = 'it\\'s' AND lastName = 1").toSql(snake));
        assertEquals("a_b = `x\\` AND cD = 1", SqlExpression.of("aB = `x\\` AND cD = 1").toSql(snake));
    }

    // Regression: a function name with a glued marker (my_func_${ver}(x)) had its leading part converted, calling a different function.
    @Test
    public void testFunctionNameWithGluedMarkerIsKeptWhole() {
        assertEquals("my_func_${ver}(x) > 0", SqlExpression.of("my_func_${ver}(x) > 0").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("getValue_${v}(A_B)", SqlExpression.of("getValue_${v}(aB)").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("fn_${v}(a_b)", SqlExpression.of("fn_${v}(aB)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("calc_#{v}(a_b)", SqlExpression.of("calc_#{v}(aB)").toSql(NamingPolicy.SNAKE_CASE));
        // A ':' still separates a column from the cast type or slice function that follows it.
        assertEquals("unit_price::numeric(10,2) > 5", SqlExpression.of("unitPrice::numeric(10,2) > 5").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("arr[lo_idx:myFn(x_y)]", SqlExpression.of("arr[loIdx:myFn(xY)]").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: the column after a glued marker and a dot (a sharded table, log_${yearMonth}.createdAt) was copied unconverted.
    @Test
    public void testColumnAfterGluedMarkerAndDotIsConverted() {
        assertEquals("log_${yearMonth}.created_at = 1", SqlExpression.of("log_${yearMonth}.createdAt = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("order_${month}.create_time > 0", SqlExpression.of("order_${month}.createTime > 0").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("t_#{n}.first_name", SqlExpression.of("t_#{n}.firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("LOG_${yearMonth}.CREATED_AT::text", SqlExpression.of("log_${yearMonth}.createdAt::text").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("log_${yearMonth}.myFunc(created_at)", SqlExpression.of("log_${yearMonth}.myFunc(createdAt)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("log_${yearMonth}.\"createdAt\"", SqlExpression.of("log_${yearMonth}.\"createdAt\"").toSql(NamingPolicy.SNAKE_CASE));
    }

    // Regression: the upper bound of a slice (myTags[firstName:lastName]) was copied as if ":lastName" were a bind marker,
    // and only the first subscript of a chain (matrix[rowIdx][colIdx]) had its interior converted.
    @Test
    public void testSliceUpperBoundAndChainedSubscriptsAreConverted() {
        final NamingPolicy snake = NamingPolicy.SNAKE_CASE;

        assertEquals("my_tags[first_name:last_name] = 1", SqlExpression.of("myTags[firstName:lastName] = 1").toSql(snake));
        assertEquals("my_tags[first_name : last_name]", SqlExpression.of("myTags[firstName : lastName]").toSql(snake));
        assertEquals("my_tags[:tagIndex]", SqlExpression.of("myTags[:tagIndex]").toSql(snake));
        assertEquals("my_tags[first_name::int]", SqlExpression.of("myTags[firstName::int]").toSql(snake));
        // ParsedSql agrees that a ':' glued after a name is not a named parameter.
        assertTrue(com.landawn.abacus.query.ParsedSql.parse("SELECT * FROM t WHERE myTags[firstName:lastName] = 1").namedParameters().isEmpty());

        assertEquals("matrix[row_idx][col_idx] > 0", SqlExpression.of("matrix[rowIdx][colIdx] > 0").toSql(snake));
        assertEquals("m[a_b][c_d][e_f].field_name", SqlExpression.of("m[aB][cD][eF].fieldName").toSql(snake));
        assertEquals("ARRAY[a_b, 2][c_d]", SqlExpression.of("ARRAY[aB, 2][cD]").toSql(snake));
        assertEquals("arr[1].field_name", SqlExpression.of("arr[1].fieldName").toSql(snake));
        // Only a directly following bracket group continues the chain.
        assertEquals("matrix[row_idx] [colIdx]", SqlExpression.of("matrix[rowIdx] [colIdx]").toSql(snake));
        assertEquals("t.[col][aB]", SqlExpression.of("t.[col][aB]").toSql(snake));
    }

    // Regression: a subscript interior holding '#', "--" or "/*" only inside a string literal was copied unconverted.
    @Test
    public void testSubscriptInteriorWithCommentTokenInsideLiteralIsConverted() {
        final NamingPolicy snake = NamingPolicy.SNAKE_CASE;

        assertEquals("arr[first_name || '#'] = 1", SqlExpression.of("arr[firstName || '#'] = 1").toSql(snake));
        assertEquals("arr[first_name || '-- x'] = 1", SqlExpression.of("arr[firstName || '-- x'] = 1").toSql(snake));
        assertEquals("arr[first_name || '/* x */'] = 1", SqlExpression.of("arr[firstName || '/* x */'] = 1").toSql(snake));
        // Outside a literal the interior is still copied as written.
        assertEquals("arr[idx # 2] = 1", SqlExpression.of("arr[idx # 2] = 1").toSql(snake));
        assertEquals("arr[/* x */ aB] = 1", SqlExpression.of("arr[/* x */ aB] = 1").toSql(snake));
        assertEquals("arr['#' || aB # x] = 1", SqlExpression.of("arr['#' || aB # x] = 1").toSql(snake));
    }

    // Regression: the line break between two adjacent string literals was collapsed to a space ('a' 'b'), which PostgreSQL
    // rejects: the standard concatenates adjacent literals only across a line break.
    @Test
    public void testLineBreakBetweenAdjacentStringLiteralsIsKept() {
        assertEquals("'a'\n'b'", SqlExpression.of("'a'\n'b'").toString());
        assertEquals("msg = 'hello '\n'world'", SqlExpression.of("msg = 'hello '\n'world'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("'a'\n'b'", SqlExpression.of("'a' -- c\n'b'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("'a'\n'b'", SqlExpression.of("'a'\r'b'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("'a'\n'b'", SqlExpression.of("'a' \r\n  'b'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("'a'\n'b'\n'c'", SqlExpression.of("'a'\n'b'\n'c'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("N'a'\n'b'", SqlExpression.of("N'a'\n'b'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("arr['a'\n'b']", SqlExpression.of("arr['a'\n'b']").toSql(NamingPolicy.SNAKE_CASE));
        // Only a gap that held a line break, and only between two literals; a prefixed second literal is not a continuation.
        assertEquals("'a' 'b'\n'c'", SqlExpression.of("'a' 'b'\n'c'").toSql(NamingPolicy.NO_CHANGE));
        assertEquals("first_name = 'x' AND last_name = 'y'", SqlExpression.of("firstName = 'x'\nAND lastName = 'y'").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("'a' N'b'", SqlExpression.of("'a'\nN'b'").toSql(NamingPolicy.NO_CHANGE));
        // The second literal may start the verbatim text: the line break before it is still kept.
        assertEquals("'a'\n'C:\\' AND bC = 1", SqlExpression.of("'a'\n'C:\\' AND bC = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("msg = 'a'\n'b'", Filters.eq("msg", SqlExpression.of("'a' -- c\n'b'")).toString());
    }

    // Coverage: exact COLLATE rendering under every case-changing policy, the ARRAY keyword spelling, and multi-segment
    // names after a delimited qualifier.
    @Test
    public void testCollateArrayKeywordAndQualifiedSegmentsRenderExactly() {
        assertEquals("firstName collate utf8mb4_bin = 'x'", SqlExpression.of("first_name COLLATE utf8mb4_bin = 'x'").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("FirstName Collate utf8mb4_bin", SqlExpression.of("firstName COLLATE utf8mb4_bin").toSql(NamingPolicy.UPPER_CAMEL_CASE));
        assertEquals("LAST_NAME COLLATE Latin1_General_CS_AS", SqlExpression.of("lastName COLLATE Latin1_General_CS_AS").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("last_name collate \"de_DE\"", SqlExpression.of("lastName collate \"de_DE\"").toSql(NamingPolicy.SNAKE_CASE));

        assertEquals("Array[FIRST_NAME]", SqlExpression.of("Array[firstName]").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("Array[FirstName]", SqlExpression.of("Array[firstName]").toSql(NamingPolicy.UPPER_CAMEL_CASE));
        assertEquals("\"T\".first_name.sub_field", SqlExpression.of("\"T\".firstName.subField").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testRenderValueKeepsTimestampNanosAndRendersCalendarAtJvmZoneInstant() {
        // Covers full Timestamp nanosecond precision and a Calendar rendered at its instant in a fixed JVM zone (as it is bound).
        assertEquals("'2020-01-02 03:04:05.123456789'", SqlExpression.renderValue(java.sql.Timestamp.valueOf("2020-01-02 03:04:05.123456789")));

        final java.util.TimeZone defaultZone = java.util.TimeZone.getDefault();

        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

            final java.util.Calendar tokyo = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            tokyo.clear();
            tokyo.set(2020, java.util.Calendar.JANUARY, 2, 3, 4, 5);
            assertEquals("'2020-01-01 18:04:05.0'", SqlExpression.renderValue(tokyo));
        } finally {
            java.util.TimeZone.setDefault(defaultZone);
        }
    }

    @Test
    public void testNonAsciiSpaceGluedToIdentifierRendersAsPlainSpace() {
        // Regression: the tokenizer keeps U+3000 / NBSP inside a word, and leading-name-only conversion then emitted
        // "first_name\u3000", which databases read as one (unknown) identifier.
        assertEquals("first_name = last_name", SqlExpression.of("firstName\u3000= lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = 1 AND last_name = 2", SqlExpression.of("firstName\u00A0= 1 AND lastName\u3000\u3000= 2").toSql(NamingPolicy.SNAKE_CASE));
        // Several words glued by non-ASCII spaces: each word is still rendered (keywords kept, names converted).
        assertEquals("first_name AND last_name", SqlExpression.of("firstName\u3000AND\u3000lastName").toSql(NamingPolicy.SNAKE_CASE));
        // A function name keeps its spelling.
        assertEquals("myFunc (first_name)", SqlExpression.of("myFunc\u3000(firstName)").toSql(NamingPolicy.SNAKE_CASE));

        // Same rendering through a builder (shared token loop).
        assertEquals("SELECT id FROM account WHERE first_name = last_name",
                com.landawn.abacus.query.Dsl.PSC.select("id").from("account").where("firstName\u3000= lastName").build().query());
    }
    @Test
    public void testNonAsciiSpaceAnywhereInAWordIsASeparator() {
        // Regression: a non-ASCII space at the START of a token (after '=', ',', '(' or a number), after "::type", or between
        // several glued words kept a name unconverted, renamed a glued function, or converted a collation name; a long
        // chain of glued words overflowed the stack.
        assertEquals("first_name = last_name", SqlExpression.of("firstName\u3000=\u3000lastName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name = 1 AND last_name = 2", SqlExpression.of("firstName = 1\u3000AND\u3000lastName = 2").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("create_time::date = CURRENT_DATE", SqlExpression.of("createTime::date\u3000= CURRENT_DATE").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("x myFunc(last_name) > 0", SqlExpression.of("x\u3000myFunc(lastName) > 0").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("first_name LIKE upper(:p)", SqlExpression.of("firstName\u3000LIKE\u3000upper(:p)").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("firstName collate utf8mb4_bin = 'x'", SqlExpression.of("first_name\u3000COLLATE\u3000utf8mb4_bin = 'x'").toSql(NamingPolicy.CAMEL_CASE));
        assertEquals("a_b = 1", SqlExpression.of("aB\u00A0= 1").toSql(NamingPolicy.SNAKE_CASE));

        // Quoted text keeps its spaces.
        assertEquals("first_name = 'a\u3000b'", SqlExpression.of("firstName = 'a\u3000b'").toSql(NamingPolicy.SNAKE_CASE));

        // A split-off space never doubles an adjacent space, leads or trails the expression, and keeps a line break
        // between adjacent string literals.
        assertEquals("x = 1", SqlExpression.of("x\u3000 = 1").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("a b", SqlExpression.of("a\u3000/* c */\u3000b").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("'a'\n'b' = first_name", SqlExpression.of("'a'\u3000\n'b' = firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("SELECT id FROM t WHERE first_name = 1 ORDER BY id",
                com.landawn.abacus.query.Dsl.PSC.select("id").from("t").where("\u3000firstName = 1\u3000").orderBy("id").build().query());

        // A long run of glued words renders without recursion.
        final String longChain = "aB\u3000".repeat(5000) + "cD";
        assertEquals("a_b ".repeat(5000) + "c_d", SqlExpression.of(longChain).toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("SELECT id FROM t WHERE " + "a_b ".repeat(5000) + "c_d",
                com.landawn.abacus.query.Dsl.PSC.select("id").from("t").where(longChain).build().query());
    }

    @Test
    public void testSliceWithNumericLowerBoundConvertsTheUpperBoundColumn() {
        // Regression: inside a subscript, "2:tagCount" is one token starting with a digit, so the column after ':' was left
        // unconverted (HEAD converted it). A ':' glued to a number is not a bind marker.
        assertEquals("tags[2:tag_count] && x", SqlExpression.of("tags[2:tagCount] && x").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("scores[1:end_idx] > 0", SqlExpression.of("scores[1:endIdx] > 0").toSql(NamingPolicy.SNAKE_CASE));
        assertTrue(com.landawn.abacus.query.ParsedSql.parse("SELECT a FROM t WHERE scores[1:endIdx] > 0").namedParameters().isEmpty());
        // A leading ':' is still a bind marker.
        assertEquals("scores[:endIdx] > 0", SqlExpression.of("scores[:endIdx] > 0").toSql(NamingPolicy.SNAKE_CASE));
    }
}
