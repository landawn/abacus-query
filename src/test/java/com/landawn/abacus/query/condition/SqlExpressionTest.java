package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    public void testOfMethodCaching() {
        SqlExpression expr1 = SqlExpression.of("CURRENT_DATE");
        SqlExpression expr2 = SqlExpression.of("CURRENT_DATE");

        assertSame(expr1, expr2, "Should return cached instance");
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
    public void testIsEmpty() {
        String result = SqlExpression.isNullOrEmpty("description");

        assertTrue(result.contains("description"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("BLANK"));
    }

    @Test
    public void testIsNotEmpty() {
        String result = SqlExpression.isNotNullAndNotEmpty("name");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("BLANK"));
    }

    // Composable operators
    @Test
    public void testAnd() {
        String result = SqlExpression.and("active = true", "age > 18");

        assertTrue(result.contains("active = true"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("age > 18"));
    }

    @Test
    public void testOr() {
        String result = SqlExpression.or("status = 'active'", "status = 'pending'");

        assertTrue(result.contains("status = 'active'"));
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status = 'pending'"));
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

        Assertions.assertSame(expr1, expr2); // Should be cached
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

    /**
     * Regression (Pass 3): a string ending in a single backslash must not produce
     * {@code 'x\'} (where the closing quote is consumed as an escape under MySQL-style
     * parsing). The escape helper must keep the literal balanced.
     */
    @Test
    public void testRenderValue_TrailingBackslashStaysBalanced_Pass3() {
        String input = "x" + (char) 92; // x followed by one backslash
        String result = SqlExpression.renderValue(input);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("'") && result.endsWith("'"), "Output must be quoted, got: " + result);
        String body = result.substring(1, result.length() - 1);
        int trailing = 0;
        for (int i = body.length() - 1; i >= 0 && body.charAt(i) == '\\'; i--) {
            trailing++;
        }
        Assertions.assertEquals(0, trailing % 2, "Trailing backslash count must be even so closing quote is not escaped, got body: " + body);
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
     * {@link SqlExpression#of} interns instances, so the same object is reused across calls and must answer
     * each policy correctly even when callers alternate policies on it. (Originally guarded the
     * since-removed single-slot toString cache; kept because the contract must hold regardless of any
     * internal memoization.)
     */
    @Test
    public void testToStringReturnsValuePerNamingPolicy() {
        SqlExpression expr = SqlExpression.of("firstName");
        assertSame(expr, SqlExpression.of("firstName"), "SqlExpression.of must intern instances");

        for (int i = 0; i < 1000; i++) {
            assertEquals("firstName", expr.toSql(NamingPolicy.NO_CHANGE));
            assertEquals("first_name", expr.toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("FIRST_NAME", expr.toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("firstName", expr.toSql(null)); // null defaults to NO_CHANGE
        }
    }

    /**
     * {@code toSql(NamingPolicy)} must be thread-safe on a shared interned instance: every call must
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
    public void testNamingPolicyConvertsUnderscoreLeadingIdentifiers() {
        assertEquals("_first_name", SqlExpression.of("_firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_FIRST_NAME = OTHER_VALUE", SqlExpression.of("_firstName = otherValue").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
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

}
