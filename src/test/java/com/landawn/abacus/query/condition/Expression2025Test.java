package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for Expression.
 * Tests all public methods including constructors, factory methods, SQL functions, operators, and utilities.
 */
@Tag("2025")
public class Expression2025Test extends TestBase {

    @Test
    public void testConstructorWithLiteral() {
        String literal = "CURRENT_TIMESTAMP";
        Expression expr = new Expression(literal);

        assertNotNull(expr);
        assertEquals(literal, expr.getLiteral());
    }

    @Test
    public void testGetLiteral() {
        Expression expr = new Expression("price * 1.1");

        assertEquals("price * 1.1", expr.getLiteral());
    }

    @Test
    public void testOfMethodCaching() {
        Expression expr1 = Expression.of("CURRENT_DATE");
        Expression expr2 = Expression.of("CURRENT_DATE");

        assertSame(expr1, expr2, "Should return cached instance");
    }

    @Test
    public void testOfMethodDifferentLiterals() {
        Expression expr1 = Expression.of("literal1");
        Expression expr2 = Expression.of("literal2");

        assertNotSame(expr1, expr2);
    }

    // Comparison operators
    @Test
    public void testEqual() {
        String result = Expression.equal("age", 25);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("25"));
    }

    @Test
    public void testEq() {
        String result = Expression.eq("status", "active");

        assertTrue(result.contains("status"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("'active'"));
    }

    @Test
    public void testNotEqual() {
        String result = Expression.notEqual("status", "inactive");

        assertTrue(result.contains("status"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("'inactive'"));
    }

    @Test
    public void testNe() {
        String result = Expression.ne("count", 0);

        assertTrue(result.contains("count"));
        assertTrue(result.contains("!="));
        assertTrue(result.contains("0"));
    }

    @Test
    public void testGreaterThan() {
        String result = Expression.greaterThan("salary", 50000);

        assertTrue(result.contains("salary"));
        assertTrue(result.contains(">"));
        assertTrue(result.contains("50000"));
    }

    @Test
    public void testGt() {
        String result = Expression.gt("age", 18);

        assertTrue(result.contains("age"));
        assertTrue(result.contains(">"));
        assertTrue(result.contains("18"));
    }

    @Test
    public void testGreaterEqual() {
        String result = Expression.greaterEqual("score", 60);

        assertTrue(result.contains("score"));
        assertTrue(result.contains(">="));
        assertTrue(result.contains("60"));
    }

    @Test
    public void testGe() {
        String result = Expression.ge("quantity", 1);

        assertTrue(result.contains("quantity"));
        assertTrue(result.contains(">="));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testLessThan() {
        String result = Expression.lessThan("price", 100);

        assertTrue(result.contains("price"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testLt() {
        String result = Expression.lt("stock", 10);

        assertTrue(result.contains("stock"));
        assertTrue(result.contains("<"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testLessEqual() {
        String result = Expression.lessEqual("discount", 50);

        assertTrue(result.contains("discount"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("50"));
    }

    @Test
    public void testLe() {
        String result = Expression.le("temperature", 32);

        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("<="));
        assertTrue(result.contains("32"));
    }

    @Test
    public void testBetween() {
        String result = Expression.between("age", 18, 65);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testLike() {
        String result = Expression.like("name", "John%");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("LIKE"));
        assertTrue(result.contains("'John%'"));
    }

    @Test
    public void testIsNull() {
        String result = Expression.isNull("middleName");

        assertTrue(result.contains("middleName"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testIsNotNull() {
        String result = Expression.isNotNull("email");

        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testIsEmpty() {
        String result = Expression.isEmpty("description");

        assertTrue(result.contains("description"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("BLANK"));
    }

    @Test
    public void testIsNotEmpty() {
        String result = Expression.isNotEmpty("name");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("BLANK"));
    }

    // Logical operators
    @Test
    public void testAnd() {
        String result = Expression.and("active = true", "age > 18");

        assertTrue(result.contains("active = true"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("age > 18"));
    }

    @Test
    public void testOr() {
        String result = Expression.or("status = 'active'", "status = 'pending'");

        assertTrue(result.contains("status = 'active'"));
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status = 'pending'"));
    }

    // Arithmetic operators
    @Test
    public void testPlus() {
        String result = Expression.plus("price", "tax", "shipping");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("+"));
        assertTrue(result.contains("tax"));
        assertTrue(result.contains("shipping"));
    }

    @Test
    public void testMinus() {
        String result = Expression.minus("total", "discount");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("-"));
        assertTrue(result.contains("discount"));
    }

    @Test
    public void testMulti() {
        String result = Expression.multi("price", "quantity");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
    }

    @Test
    public void testDivision() {
        String result = Expression.division("total", "count");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("/"));
        assertTrue(result.contains("count"));
    }

    @Test
    public void testModulus() {
        String result = Expression.modulus("value", 10);

        assertTrue(result.contains("value"));
        assertTrue(result.contains("%"));
        assertTrue(result.contains("10"));
    }

    // Bitwise operators
    @Test
    public void testBitwiseAnd() {
        String result = Expression.bitwiseAnd("flags", "mask");

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("&"));
        assertTrue(result.contains("mask"));
    }

    @Test
    public void testBitwiseOr() {
        String result = Expression.bitwiseOr("flags1", "flags2");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("flags2"));
    }

    @Test
    public void testBitwiseXOr() {
        String result = Expression.bitwiseXOr("value1", "value2");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
    }

    @Test
    public void testLShift() {
        String result = Expression.lShift("flags", 2);

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShift() {
        String result = Expression.rShift("value", 4);

        assertTrue(result.contains("value"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("4"));
    }

    // Aggregate functions
    @Test
    public void testCount() {
        String result = Expression.count("*");

        assertEquals("COUNT(*)", result);
    }

    @Test
    public void testAverage() {
        String result = Expression.average("salary");

        assertTrue(result.contains("AVG"));
        assertTrue(result.contains("salary"));
    }

    @Test
    public void testSum() {
        String result = Expression.sum("amount");

        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("amount"));
    }

    @Test
    public void testMin() {
        String result = Expression.min("price");

        assertTrue(result.contains("MIN"));
        assertTrue(result.contains("price"));
    }

    @Test
    public void testMax() {
        String result = Expression.max("score");

        assertTrue(result.contains("MAX"));
        assertTrue(result.contains("score"));
    }

    // Mathematical functions
    @Test
    public void testAbs() {
        String result = Expression.abs("balance");

        assertTrue(result.contains("ABS"));
        assertTrue(result.contains("balance"));
    }

    @Test
    public void testCeil() {
        String result = Expression.ceil("price");

        assertTrue(result.contains("CEIL"));
        assertTrue(result.contains("price"));
    }

    @Test
    public void testFloor() {
        String result = Expression.floor("average");

        assertTrue(result.contains("FLOOR"));
        assertTrue(result.contains("average"));
    }

    @Test
    public void testSqrt() {
        String result = Expression.sqrt("area");

        assertTrue(result.contains("SQRT"));
        assertTrue(result.contains("area"));
    }

    @Test
    public void testPower() {
        String result = Expression.power("base", "exponent");

        assertTrue(result.contains("POWER"));
        assertTrue(result.contains("base"));
        assertTrue(result.contains("exponent"));
    }

    @Test
    public void testMod() {
        String result = Expression.mod("dividend", "divisor");

        assertTrue(result.contains("MOD"));
        assertTrue(result.contains("dividend"));
        assertTrue(result.contains("divisor"));
    }

    @Test
    public void testLog() {
        String result = Expression.log("10", "100");

        assertTrue(result.contains("LOG"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testLn() {
        String result = Expression.ln("value");

        assertTrue(result.contains("LN"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testExp() {
        String result = Expression.exp("rate");

        assertTrue(result.contains("EXP"));
        assertTrue(result.contains("rate"));
    }

    @Test
    public void testSign() {
        String result = Expression.sign("balance");

        assertTrue(result.contains("SIGN"));
        assertTrue(result.contains("balance"));
    }

    // Trigonometric functions
    @Test
    public void testSin() {
        String result = Expression.sin("angle");

        assertTrue(result.contains("SIN"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testCos() {
        String result = Expression.cos("angle");

        assertTrue(result.contains("COS"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testTan() {
        String result = Expression.tan("angle");

        assertTrue(result.contains("TAN"));
        assertTrue(result.contains("angle"));
    }

    @Test
    public void testAsin() {
        String result = Expression.asin("value");

        assertTrue(result.contains("ASIN"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testAcos() {
        String result = Expression.acos("value");

        assertTrue(result.contains("ACOS"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testAtan() {
        String result = Expression.atan("value");

        assertTrue(result.contains("ATAN"));
        assertTrue(result.contains("value"));
    }

    // String functions
    @Test
    public void testConcat() {
        String result = Expression.concat("firstName", "' '");

        assertTrue(result.contains("CONCAT"));
        assertTrue(result.contains("firstName"));
    }

    @Test
    public void testReplace() {
        String result = Expression.replace("email", "'@'", "'_at_'");

        assertTrue(result.contains("REPLACE"));
        assertTrue(result.contains("email"));
    }

    @Test
    public void testStringLength() {
        String result = Expression.stringLength("name");

        assertTrue(result.contains("LENGTH"));
        assertTrue(result.contains("name"));
    }

    @Test
    public void testSubStringFromIndex() {
        String result = Expression.subString("phone", 1);

        assertTrue(result.contains("SUBSTR"));
        assertTrue(result.contains("phone"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testSubStringWithLength() {
        String result = Expression.subString("code", 1, 3);

        assertTrue(result.contains("SUBSTR"));
        assertTrue(result.contains("code"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testTrim() {
        String result = Expression.trim("input");

        assertTrue(result.contains("TRIM"));
        assertTrue(result.contains("input"));
    }

    @Test
    public void testLTrim() {
        String result = Expression.lTrim("comment");

        assertTrue(result.contains("LTRIM"));
        assertTrue(result.contains("comment"));
    }

    @Test
    public void testRTrim() {
        String result = Expression.rTrim("code");

        assertTrue(result.contains("RTRIM"));
        assertTrue(result.contains("code"));
    }

    @Test
    public void testLPad() {
        String result = Expression.lPad("id", 10, "'0'");

        assertTrue(result.contains("LPAD"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testRPad() {
        String result = Expression.rPad("name", 20, "' '");

        assertTrue(result.contains("RPAD"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("20"));
    }

    @Test
    public void testLower() {
        String result = Expression.lower("email");

        assertTrue(result.contains("LOWER"));
        assertTrue(result.contains("email"));
    }

    @Test
    public void testUpper() {
        String result = Expression.upper("name");

        assertTrue(result.contains("UPPER"));
        assertTrue(result.contains("name"));
    }

    // Utility methods
    @Test
    public void testFormalizeString() {
        String result = Expression.formalize("text");

        assertEquals("'text'", result);
    }

    @Test
    public void testFormalizeNumber() {
        String result = Expression.formalize(123);

        assertEquals("123", result);
    }

    @Test
    public void testFormalizeBoolean() {
        String result = Expression.formalize(true);

        assertEquals("true", result);
    }

    @Test
    public void testFormalizeNull() {
        String result = Expression.formalize(null);

        assertEquals("null", result);
    }

    @Test
    public void testFormalizeExpression() {
        Expression expr = new Expression("column_name");
        String result = Expression.formalize(expr);

        assertEquals("column_name", result);
    }

    @Test
    public void testGetParameters() {
        Expression expr = new Expression("price * 1.1");

        List<Object> params = expr.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testClearParameters() {
        Expression expr = new Expression("CURRENT_TIMESTAMP");

        // Should not throw exception
        assertDoesNotThrow(() -> expr.clearParameters());
    }

    @Test
    public void testToStringNoChange() {
        Expression expr = new Expression("userName = 'John'");

        String result = expr.toString(NamingPolicy.NO_CHANGE);

        assertEquals("userName = 'John'", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Expression expr = new Expression("firstName");

        String result = expr.toString(NamingPolicy.SNAKE_CASE);

        assertEquals("first_name", result);
    }

    @Test
    public void testToStringNull() {
        Expression expr = new Expression(null);

        String result = expr.toString(NamingPolicy.NO_CHANGE);

        assertEquals("null", result);
    }

    @Test
    public void testToStringEmpty() {
        Expression expr = new Expression("");

        String result = expr.toString(NamingPolicy.NO_CHANGE);

        assertEquals("", result);
    }

    @Test
    public void testHashCode() {
        Expression expr1 = new Expression("test");
        Expression expr2 = new Expression("test");

        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    public void testHashCodeNull() {
        Expression expr = new Expression(null);

        assertEquals(0, expr.hashCode());
    }

    @Test
    public void testEquals() {
        Expression expr1 = new Expression("test");
        Expression expr2 = new Expression("test");

        assertEquals(expr1, expr2);
    }

    @Test
    public void testEqualsSameInstance() {
        Expression expr = new Expression("test");

        assertEquals(expr, expr);
    }

    @Test
    public void testEqualsNull() {
        Expression expr = new Expression("test");

        assertNotEquals(expr, null);
    }

    @Test
    public void testEqualsDifferentType() {
        Expression expr = new Expression("test");

        assertNotEquals(expr, "not an expression");
    }

    @Test
    public void testEqualsDifferentLiteral() {
        Expression expr1 = new Expression("literal1");
        Expression expr2 = new Expression("literal2");

        assertNotEquals(expr1, expr2);
    }

    @Test
    public void testExprAlias() {
        Expression.Expr expr = new Expression.Expr("test");

        assertNotNull(expr);
        assertEquals("test", expr.getLiteral());
    }

    @Test
    public void testBt() {
        String result = Expression.bt("score", 0, 100);

        assertTrue(result.contains("score"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("0"));
        assertTrue(result.contains("100"));
    }

    @Test
    public void testConcatWithTwoStrings() {
        String result = Expression.concat("firstName", "lastName");

        assertTrue(result.contains("CONCAT"));
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("lastName"));
    }

    @Test
    public void testAndWithMultipleExpressions() {
        String result = Expression.and("x > 0", "y > 0", "z > 0");

        assertTrue(result.contains("x > 0"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("y > 0"));
        assertTrue(result.contains("z > 0"));
    }

    @Test
    public void testOrWithMultipleExpressions() {
        String result = Expression.or("status = 'A'", "status = 'B'", "status = 'C'");

        assertTrue(result.contains("status = 'A'"));
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status = 'B'"));
        assertTrue(result.contains("status = 'C'"));
    }

    @Test
    public void testPlusWithMultipleValues() {
        String result = Expression.plus("a", "b", "c");

        assertTrue(result.contains("a"));
        assertTrue(result.contains("+"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testMinusWithMultipleValues() {
        String result = Expression.minus("total", "tax", "discount");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("-"));
        assertTrue(result.contains("tax"));
        assertTrue(result.contains("discount"));
    }

    @Test
    public void testMultiWithMultipleValues() {
        String result = Expression.multi("price", "quantity", "rate");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("rate"));
    }

    @Test
    public void testDivisionWithMultipleValues() {
        String result = Expression.division("total", "count", "factor");

        assertTrue(result.contains("total"));
        assertTrue(result.contains("/"));
        assertTrue(result.contains("count"));
        assertTrue(result.contains("factor"));
    }

    @Test
    public void testModulusWithMultipleValues() {
        String result = Expression.modulus("value", "10", "3");

        assertTrue(result.contains("value"));
        assertTrue(result.contains("%"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testLShiftWithMultipleValues() {
        String result = Expression.lShift("flags", "2", "1");

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShiftWithMultipleValues() {
        String result = Expression.rShift("value", "4", "2");

        assertTrue(result.contains("value"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("4"));
    }

    @Test
    public void testBitwiseAndWithMultipleValues() {
        String result = Expression.bitwiseAnd("flags1", "flags2", "mask");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("&"));
        assertTrue(result.contains("flags2"));
        assertTrue(result.contains("mask"));
    }

    @Test
    public void testBitwiseOrWithMultipleValues() {
        String result = Expression.bitwiseOr("flags1", "flags2", "flags3");

        assertTrue(result.contains("flags1"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("flags2"));
        assertTrue(result.contains("flags3"));
    }

    @Test
    public void testBitwiseXOrWithMultipleValues() {
        String result = Expression.bitwiseXOr("value1", "value2", "value3");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
        assertTrue(result.contains("value3"));
    }

    @Test
    public void testFormalizeCharSequence() {
        String result = Expression.formalize(new StringBuilder("test"));

        assertEquals("'test'", result);
    }

    @Test
    public void testFormalizeDouble() {
        String result = Expression.formalize(3.14);

        assertEquals("3.14", result);
    }

    @Test
    public void testFormalizeLong() {
        String result = Expression.formalize(999L);

        assertEquals("999", result);
    }

    @Test
    public void testOfMethodWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Expression.of(null);
        });
    }

    @Test
    public void testCopy() {
        Expression original = new Expression("test_expr");

        Expression copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getLiteral(), copy.getLiteral());
    }

    @Test
    public void testEqualsWithNullLiterals() {
        Expression expr1 = new Expression(null);
        Expression expr2 = new Expression(null);

        assertEquals(expr1, expr2);
    }
}
