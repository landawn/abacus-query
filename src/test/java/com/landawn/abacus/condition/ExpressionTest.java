package com.landawn.abacus.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class ExpressionTest extends TestBase {

    @Test
    public void testConstructor() {
        Expression expr = new Expression("price * 0.9");
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("price * 0.9", expr.getLiteral());
        Assertions.assertEquals(Operator.EMPTY, expr.getOperator());
    }

    @Test
    public void testOf() {
        Expression expr1 = Expression.of("CURRENT_TIMESTAMP");
        Expression expr2 = Expression.of("CURRENT_TIMESTAMP");

        Assertions.assertSame(expr1, expr2); // Should be cached
        Assertions.assertEquals("CURRENT_TIMESTAMP", expr1.getLiteral());
    }

    @Test
    public void testEqual() {
        String result = Expression.equal("age", 25);
        Assertions.assertEquals("age = 25", result);
    }

    @Test
    public void testEq() {
        String result = Expression.eq("status", "active");
        Assertions.assertEquals("status = 'active'", result);
    }

    @Test
    public void testNotEqual() {
        String result = Expression.notEqual("type", "temp");
        Assertions.assertEquals("type != 'temp'", result);
    }

    @Test
    public void testNe() {
        String result = Expression.ne("deleted", true);
        Assertions.assertEquals("deleted != true", result);
    }

    @Test
    public void testGreaterThan() {
        String result = Expression.greaterThan("salary", 50000);
        Assertions.assertEquals("salary > 50000", result);
    }

    @Test
    public void testGt() {
        String result = Expression.gt("score", 80.5);
        Assertions.assertEquals("score > 80.5", result);
    }

    @Test
    public void testGreaterEqual() {
        String result = Expression.greaterEqual("age", 18);
        Assertions.assertEquals("age >= 18", result);
    }

    @Test
    public void testGe() {
        String result = Expression.ge("count", 0);
        Assertions.assertEquals("count >= 0", result);
    }

    @Test
    public void testLessThan() {
        String result = Expression.lessThan("price", 100.0);
        Assertions.assertEquals("price < 100.0", result);
    }

    @Test
    public void testLt() {
        String result = Expression.lt("temperature", 0);
        Assertions.assertEquals("temperature < 0", result);
    }

    @Test
    public void testLessEqual() {
        String result = Expression.lessEqual("stock", 10);
        Assertions.assertEquals("stock <= 10", result);
    }

    @Test
    public void testLe() {
        String result = Expression.le("discount", 0.5);
        Assertions.assertEquals("discount <= 0.5", result);
    }

    @Test
    public void testBetween() {
        String result = Expression.between("age", 18, 65);
        Assertions.assertEquals("age BETWEEN (18, 65)", result);
    }

    @Test
    public void testBt() {
        String result = Expression.bt("price", 10.0, 50.0);
        Assertions.assertEquals("price BETWEEN (10.0, 50.0)", result);
    }

    @Test
    public void testLike() {
        String result = Expression.like("name", "John%");
        Assertions.assertEquals("name LIKE 'John%'", result);
    }

    @Test
    public void testIsNull() {
        String result = Expression.isNull("middleName");
        Assertions.assertEquals("middleName IS NULL", result);
    }

    @Test
    public void testIsNotNull() {
        String result = Expression.isNotNull("email");
        Assertions.assertEquals("email IS NOT NULL", result);
    }

    @Test
    public void testIsEmpty() {
        String result = Expression.isEmpty("description");
        Assertions.assertEquals("description IS BLANK", result);
    }

    @Test
    public void testIsNotEmpty() {
        String result = Expression.isNotEmpty("title");
        Assertions.assertEquals("title IS NOT BLANK", result);
    }

    @Test
    public void testAnd() {
        String result = Expression.and("active = true", "age > 18", "status = 'APPROVED'");
        Assertions.assertEquals("active = true AND age > 18 AND status = 'APPROVED'", result);
    }

    @Test
    public void testOr() {
        String result = Expression.or("type = 'A'", "type = 'B'");
        Assertions.assertEquals("type = 'A' OR type = 'B'", result);
    }

    @Test
    public void testPlus() {
        String result = Expression.plus(CF.expr("price"), CF.expr("tax"), CF.expr("shipping"));
        Assertions.assertEquals("price + tax + shipping", result);
    }

    @Test
    public void testMinus() {
        String result = Expression.minus(CF.expr("total"), CF.expr("discount"));
        Assertions.assertEquals("total - discount", result);
    }

    @Test
    public void testMulti() {
        String result = Expression.multi(CF.expr("quantity"), CF.expr("price"));
        Assertions.assertEquals("quantity * price", result);
    }

    @Test
    public void testDivision() {
        String result = Expression.division(CF.expr("total"), CF.expr("count"));
        Assertions.assertEquals("total / count", result);
    }

    @Test
    public void testModulus() {
        String result = Expression.modulus(CF.expr("value"), 10);
        Assertions.assertEquals("value % 10", result);
    }

    @Test
    public void testLShift() {
        String result = Expression.lShift(CF.expr("value"), 2);
        Assertions.assertEquals("value << 2", result);
    }

    @Test
    public void testRShift() {
        String result = Expression.rShift(CF.expr("value"), 2);
        Assertions.assertEquals("value >> 2", result);
    }

    @Test
    public void testBitwiseAnd() {
        String result = Expression.bitwiseAnd(CF.expr("flags"), 0xFF);
        Assertions.assertEquals("flags & 255", result);
    }

    @Test
    public void testBitwiseOr() {
        String result = Expression.bitwiseOr(CF.expr("flags"), 0x01);
        Assertions.assertEquals("flags | 1", result);
    }

    @Test
    public void testBitwiseXOr() {
        String result = Expression.bitwiseXOr(CF.expr("value"), CF.expr("mask"));
        Assertions.assertEquals("value ^ mask", result);
    }

    @Test
    public void testFormalize() {
        Assertions.assertEquals("'text'", Expression.formalize("text"));
        Assertions.assertEquals("123", Expression.formalize(123));
        Assertions.assertEquals("null", Expression.formalize(null));

        Expression expr = Expression.of("CURRENT_DATE");
        Assertions.assertEquals("CURRENT_DATE", Expression.formalize(expr));
    }

    @Test
    public void testCount() {
        String result = Expression.count("*");
        Assertions.assertEquals("COUNT(*)", result);
    }

    @Test
    public void testAverage() {
        String result = Expression.average("price");
        Assertions.assertEquals("AVG(price)", result);
    }

    @Test
    public void testSum() {
        String result = Expression.sum("amount");
        Assertions.assertEquals("SUM(amount)", result);
    }

    @Test
    public void testMin() {
        String result = Expression.min("value");
        Assertions.assertEquals("MIN(value)", result);
    }

    @Test
    public void testMax() {
        String result = Expression.max("score");
        Assertions.assertEquals("MAX(score)", result);
    }

    @Test
    public void testAbs() {
        String result = Expression.abs("difference");
        Assertions.assertEquals("ABS(difference)", result);
    }

    @Test
    public void testAcos() {
        String result = Expression.acos("value");
        Assertions.assertEquals("ACOS(value)", result);
    }

    @Test
    public void testAsin() {
        String result = Expression.asin("value");
        Assertions.assertEquals("ASIN(value)", result);
    }

    @Test
    public void testAtan() {
        String result = Expression.atan("value");
        Assertions.assertEquals("ATAN(value)", result);
    }

    @Test
    public void testCeil() {
        String result = Expression.ceil("value");
        Assertions.assertEquals("CEIL(value)", result);
    }

    @Test
    public void testCos() {
        String result = Expression.cos("angle");
        Assertions.assertEquals("COS(angle)", result);
    }

    @Test
    public void testExp() {
        String result = Expression.exp("value");
        Assertions.assertEquals("EXP(value)", result);
    }

    @Test
    public void testFloor() {
        String result = Expression.floor("value");
        Assertions.assertEquals("FLOOR(value)", result);
    }

    @Test
    public void testLog() {
        String result = Expression.log("10", "value");
        Assertions.assertEquals("LOG(10, value)", result);
    }

    @Test
    public void testLn() {
        String result = Expression.ln("value");
        Assertions.assertEquals("LN(value)", result);
    }

    @Test
    public void testMod() {
        String result = Expression.mod("dividend", "divisor");
        Assertions.assertEquals("MOD(dividend, divisor)", result);
    }

    @Test
    public void testPower() {
        String result = Expression.power("base", "exponent");
        Assertions.assertEquals("POWER(base, exponent)", result);
    }

    @Test
    public void testSign() {
        String result = Expression.sign("value");
        Assertions.assertEquals("SIGN(value)", result);
    }

    @Test
    public void testSin() {
        String result = Expression.sin("angle");
        Assertions.assertEquals("SIN(angle)", result);
    }

    @Test
    public void testSqrt() {
        String result = Expression.sqrt("value");
        Assertions.assertEquals("SQRT(value)", result);
    }

    @Test
    public void testTan() {
        String result = Expression.tan("angle");
        Assertions.assertEquals("TAN(angle)", result);
    }

    @Test
    public void testConcat() {
        String result = Expression.concat("firstName", "lastName");
        Assertions.assertEquals("CONCAT(firstName, lastName)", result);
    }

    @Test
    public void testReplace() {
        String result = Expression.replace("text", "'old'", "'new'");
        Assertions.assertEquals("REPLACE(text, 'old', 'new')", result);
    }

    @Test
    public void testStringLength() {
        String result = Expression.stringLength("name");
        Assertions.assertEquals("LENGTH(name)", result);
    }

    @Test
    public void testSubString() {
        String result = Expression.subString("text", 5);
        Assertions.assertEquals("SUBSTR(text, 5)", result);
    }

    @Test
    public void testSubStringWithLength() {
        String result = Expression.subString("text", 5, 10);
        Assertions.assertEquals("SUBSTR(text, 5, 10)", result);
    }

    @Test
    public void testTrim() {
        String result = Expression.trim("text");
        Assertions.assertEquals("TRIM(text)", result);
    }

    @Test
    public void testLTrim() {
        String result = Expression.lTrim("text");
        Assertions.assertEquals("LTRIM(text)", result);
    }

    @Test
    public void testRTrim() {
        String result = Expression.rTrim("text");
        Assertions.assertEquals("RTRIM(text)", result);
    }

    @Test
    public void testLPad() {
        String result = Expression.lPad("text", 10, "'*'");
        Assertions.assertEquals("LPAD(text, 10, '*')", result);
    }

    @Test
    public void testRPad() {
        String result = Expression.rPad("text", 10, "'*'");
        Assertions.assertEquals("RPAD(text, 10, '*')", result);
    }

    @Test
    public void testLower() {
        String result = Expression.lower("text");
        Assertions.assertEquals("LOWER(text)", result);
    }

    @Test
    public void testUpper() {
        String result = Expression.upper("text");
        Assertions.assertEquals("UPPER(text)", result);
    }

    @Test
    public void testGetParameters() {
        Expression expr = Expression.of("price * quantity");
        List<Object> params = expr.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        Expression expr = Expression.of("CURRENT_DATE");
        expr.clearParameters(); // Should do nothing

        Assertions.assertEquals("CURRENT_DATE", expr.getLiteral());
    }

    @Test
    public void testToString() {
        Expression expr = Expression.of("price > 100");
        Assertions.assertEquals("price > 100", expr.toString());
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Expression expr = Expression.of("firstName = 'John'");
        // Naming policy is ignored for expressions
        Assertions.assertEquals("first_name = 'John'", expr.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE));
    }

    @Test
    public void testEquals() {
        Expression expr1 = new Expression("price > 100");
        Expression expr2 = new Expression("price > 100");
        Expression expr3 = new Expression("price < 100");

        Assertions.assertEquals(expr1, expr1);
        Assertions.assertEquals(expr1, expr2);
        Assertions.assertNotEquals(expr1, expr3);
        Assertions.assertNotEquals(expr1, null);
        Assertions.assertNotEquals(expr1, "string");
    }

    @Test
    public void testHashCode() {
        Expression expr1 = new Expression("count > 0");
        Expression expr2 = new Expression("count > 0");

        Assertions.assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    public void testCopy() {
        Expression original = Expression.of("status = 'active'");
        Expression copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getLiteral(), copy.getLiteral());
    }

    @Test
    public void testExprClass() {
        Expression.Expr expr = new Expression.Expr("test expression");
        Assertions.assertEquals("test expression", expr.getLiteral());
        Assertions.assertTrue(expr instanceof Expression);
    }
}