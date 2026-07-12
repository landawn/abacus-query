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
 * Comprehensive test class for Expression.
 * Tests all public methods including constructors, factory methods, SQL functions, operators, and utilities.
 */
@Tag("2025")
public class ExpressionTest extends TestBase {
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
    public void testEqualWithNull() {
        String result = Expression.equal("middleName", null);
        assertEquals("middleName IS NULL", result);
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
    public void testNotEqualWithNull() {
        String result = Expression.notEqual("deleted", null);
        assertEquals("deleted IS NOT NULL", result);
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
    public void testGreaterThanOrEqual() {
        String result = Expression.greaterThanOrEqual("score", 60);

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
    public void testLessThanOrEqual() {
        String result = Expression.lessThanOrEqual("discount", 50);

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
    public void testNotBetween() {
        assertEquals("age NOT BETWEEN 18 AND 65", Expression.notBetween("age", 18, 65));
        assertEquals("created NOT BETWEEN '2024-01-01' AND '2024-12-31'", Expression.notBetween("created", "2024-01-01", "2024-12-31"));
    }

    @Test
    public void testNotLike() {
        String result = Expression.notLike("name", "John%");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("NOT LIKE"));
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
        String result = Expression.isNullOrEmpty("description");

        assertTrue(result.contains("description"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("BLANK"));
    }

    @Test
    public void testIsNotEmpty() {
        String result = Expression.isNotNullAndNotEmpty("name");

        assertTrue(result.contains("name"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("BLANK"));
    }

    // Composable operators
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
        String result = Expression.multiply("price", "quantity");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
    }

    @Test
    public void testDivision() {
        String result = Expression.divide("total", "count");

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
        String result = Expression.bitwiseXor("value1", "value2");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
    }

    @Test
    public void testLShift() {
        String result = Expression.leftShift("flags", 2);

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShift() {
        String result = Expression.rightShift("value", 4);

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
        String result = Expression.avg("salary");

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
        String result = Expression.length("name");

        assertTrue(result.contains("LENGTH"));
        assertTrue(result.contains("name"));
    }

    @Test
    public void testSubStringFromIndex() {
        String result = Expression.substr("phone", 1);

        assertTrue(result.contains("SUBSTR"));
        assertTrue(result.contains("phone"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testSubStringWithLength() {
        String result = Expression.substr("code", 1, 3);

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
        String result = Expression.ltrim("comment");

        assertTrue(result.contains("LTRIM"));
        assertTrue(result.contains("comment"));
    }

    @Test
    public void testRTrim() {
        String result = Expression.rtrim("code");

        assertTrue(result.contains("RTRIM"));
        assertTrue(result.contains("code"));
    }

    @Test
    public void testLPad() {
        String result = Expression.lpad("id", 10, "'0'");

        assertTrue(result.contains("LPAD"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("10"));
    }

    @Test
    public void testRPad() {
        String result = Expression.rpad("name", 20, "' '");

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
        String result = Expression.normalize("text");

        assertEquals("'text'", result);
    }

    @Test
    public void testFormalizeNumber() {
        String result = Expression.normalize(123);

        assertEquals("123", result);
    }

    @Test
    public void testFormalizeBoolean() {
        String result = Expression.normalize(true);

        assertEquals("true", result);
    }

    @Test
    public void testFormalizeNull() {
        String result = Expression.normalize(null);

        assertEquals("null", result);
    }

    @Test
    public void testFormalizeExpression() {
        Expression expr = new Expression("column_name");
        String result = Expression.normalize(expr);

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
    public void testConstructorWithNull() {
        assertThrows(IllegalArgumentException.class, () -> new Expression(null));
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
    public void testHashCodeEmptyLiteral() {
        Expression expr = new Expression("");

        assertEquals("".hashCode(), expr.hashCode());
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

    //    @Test
    //    public void testExprAlias() {
    //        Expression.Expr expr = new Expression.Expr("test");
    //
    //        assertNotNull(expr);
    //        assertEquals("test", expr.getLiteral());
    //    }

    // Removed: testBt() - bt() method has been removed. Use between() instead.

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
        String result = Expression.multiply("price", "quantity", "rate");

        assertTrue(result.contains("price"));
        assertTrue(result.contains("*"));
        assertTrue(result.contains("quantity"));
        assertTrue(result.contains("rate"));
    }

    @Test
    public void testDivisionWithMultipleValues() {
        String result = Expression.divide("total", "count", "factor");

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
        String result = Expression.leftShift("flags", "2", "1");

        assertTrue(result.contains("flags"));
        assertTrue(result.contains("<<"));
        assertTrue(result.contains("2"));
    }

    @Test
    public void testRShiftWithMultipleValues() {
        String result = Expression.rightShift("value", "4", "2");

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
        String result = Expression.bitwiseXor("value1", "value2", "value3");

        assertTrue(result.contains("value1"));
        assertTrue(result.contains("^"));
        assertTrue(result.contains("value2"));
        assertTrue(result.contains("value3"));
    }

    @Test
    public void testFormalizeCharSequence() {
        String result = Expression.normalize(new StringBuilder("test"));

        assertEquals("'test'", result);
    }

    @Test
    public void testFormalizeDouble() {
        String result = Expression.normalize(3.14);

        assertEquals("3.14", result);
    }

    @Test
    public void testFormalizeLong() {
        String result = Expression.normalize(999L);

        assertEquals("999", result);
    }

    @Test
    public void testOfMethodWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Expression.of(null);
        });
    }

    @Test
    public void testEqualsWithEmptyLiterals() {
        Expression expr1 = new Expression("");
        Expression expr2 = new Expression("");

        assertEquals(expr1, expr2);
    }

    @Test
    public void testConstructor() {
        Expression expr = new Expression("price * 0.9");
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("price * 0.9", expr.getLiteral());
        Assertions.assertEquals(Operator.EMPTY, expr.operator());
    }

    @Test
    public void testOf() {
        Expression expr1 = Expression.of("CURRENT_TIMESTAMP");
        Expression expr2 = Expression.of("CURRENT_TIMESTAMP");

        Assertions.assertSame(expr1, expr2); // Should be cached
        Assertions.assertEquals("CURRENT_TIMESTAMP", expr1.getLiteral());
    }

    @Test
    public void testFormalize() {
        Assertions.assertEquals("'text'", Expression.normalize("text"));
        Assertions.assertEquals("123", Expression.normalize(123));
        Assertions.assertEquals("null", Expression.normalize(null));

        Expression expr = Expression.of("CURRENT_DATE");
        Assertions.assertEquals("CURRENT_DATE", Expression.normalize(expr));
    }

    @Test
    public void testNormalizeSubQueryCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Assertions.assertEquals("(SELECT id FROM users)", Expression.normalize(subQuery));
    }

    @Test
    public void testEqualWithSubQueryCondition() {
        String result = Expression.equal("id", Filters.subQuery("SELECT id FROM users"));
        Assertions.assertEquals("id = (SELECT id FROM users)", result);
    }

    @Test
    public void testSubString() {
        String result = Expression.substr("text", 5);
        Assertions.assertEquals("SUBSTR(text, 5)", result);
    }

    @Test
    public void testToString() {
        Expression expr = Expression.of("price > 100");
        Assertions.assertEquals("price > 100", expr.toString());
    }

    //    @Test
    //    public void testExprClass() {
    //        Expression.Expr expr = new Expression.Expr("test expression");
    //        Assertions.assertEquals("test expression", expr.getLiteral());
    //        Assertions.assertTrue(expr instanceof Expression);
    //    }

    @Test
    public void testDefaultConstructor_EmptyState_Batch2() {
        Expression expr = new Expression();

        Assertions.assertNull(expr.getLiteral());
        Assertions.assertEquals("null", expr.toString());
        Assertions.assertEquals("null", Expression.normalize(expr));
    }

    @Test
    public void testNormalize_ConditionWithoutSubQuery_Batch2() {
        String normalized = Expression.normalize(Filters.eq("id", 1));

        Assertions.assertEquals("id = 1", normalized);
    }

    @Test
    public void testLink_NotEqualAnsiWithNull_Batch2() {
        Assertions.assertEquals("deleted IS NOT NULL", Expression.link(Operator.NOT_EQUAL_ANSI, "deleted", null));
    }

    @Test
    public void testLink_SpaceAndCommaSeparators_Batch2() {
        Assertions.assertEquals("a b", Expression.link(" ", Expression.of("a"), Expression.of("b")));
        Assertions.assertEquals("a, b", Expression.link(", ", Expression.of("a"), Expression.of("b")));
    }

    @Test
    public void testToString_FunctionNameWithNamingPolicy_Batch2() {
        Expression expr = Expression.of("SUM(totalAmount)");

        Assertions.assertEquals("SUM(total_amount)", expr.toString(NamingPolicy.SNAKE_CASE));
        Assertions.assertEquals("SUM(totalAmount)", expr.toString(null));
    }

    @Test
    public void testToStringDoesNotConvertSqlKeywordLiterals() {
        assertEquals("CURRENT_DATE", Expression.of("CURRENT_DATE").toString(NamingPolicy.CAMEL_CASE));
        assertEquals("CURRENT_TIMESTAMP", Expression.of("CURRENT_TIMESTAMP").toString(NamingPolicy.CAMEL_CASE));
        assertEquals("CURRENT_DATE = created_date", Expression.of("CURRENT_DATE = createdDate").toString(NamingPolicy.SNAKE_CASE));
    }

    /**
     * Lower-case identifiers that collide with a SQL keyword (e.g. a column literally named {@code order}
     * or {@code count}) must still be converted by the naming policy. Only the canonical upper-case keyword
     * form is preserved; the lower-case form is treated as an ordinary identifier.
     */
    @Test
    public void testToStringConvertsLowercaseKeywordLikeColumnNames() {
        assertEquals("ORDER", Expression.of("order").toString(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("COUNT", Expression.of("count").toString(NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("ROWNUM", Expression.of("rownum").toString(NamingPolicy.SCREAMING_SNAKE_CASE));
        // The upper-case keyword form is still preserved.
        assertEquals("CURRENT_DATE", Expression.of("CURRENT_DATE").toString(NamingPolicy.CAMEL_CASE));
    }

    /**
     * Regression (Pass 2): operator-to-comparison method mapping is one-to-one and
     * uses the SQL token that matches the method name. Catches potential copy/paste
     * defects where {@code greaterThanOrEqual} could accidentally emit {@code <=}, etc.
     */
    @Test
    public void testComparisonOperatorTokensAreCorrect_Pass2() {
        Assertions.assertEquals("age = 18", Expression.equal("age", 18));
        Assertions.assertEquals("age != 18", Expression.notEqual("age", 18));
        Assertions.assertEquals("age > 18", Expression.greaterThan("age", 18));
        Assertions.assertEquals("age >= 18", Expression.greaterThanOrEqual("age", 18));
        Assertions.assertEquals("age < 18", Expression.lessThan("age", 18));
        Assertions.assertEquals("age <= 18", Expression.lessThanOrEqual("age", 18));
    }

    /**
     * Regression (Pass 2): {@code Expression.between(prop, min, max)} must emit
     * {@code prop BETWEEN min AND max} in that exact order regardless of value type,
     * not {@code prop BETWEEN max AND min}.
     */
    @Test
    public void testBetweenArgumentOrder_Pass2() {
        Assertions.assertEquals("age BETWEEN 18 AND 65", Expression.between("age", 18, 65));
        Assertions.assertEquals("created BETWEEN '2024-01-01' AND '2024-12-31'", Expression.between("created", "2024-01-01", "2024-12-31"));
    }

    // ---------------------------------------------------------------------
    // Third-pass review: SQL-escaping / injection-vector regression tests.
    // ---------------------------------------------------------------------

    /**
     * Regression (Pass 3): {@link Expression#normalize(Object)} must reject NaN / Infinity
     * because they have no portable SQL literal form (the previous behavior emitted a bare
     * {@code NaN} or {@code Infinity} token that most dialects reject).
     */
    @Test
    public void testNormalize_RejectsNaNAndInfinity_Pass3() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Expression.normalize(Double.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Expression.normalize(Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Expression.normalize(Double.NEGATIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Expression.normalize(Float.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Expression.normalize(Float.POSITIVE_INFINITY));
    }

    /**
     * Regression (Pass 3): a string ending in a single backslash must not produce
     * {@code 'x\'} (where the closing quote is consumed as an escape under MySQL-style
     * parsing). The escape helper must keep the literal balanced.
     */
    @Test
    public void testNormalize_TrailingBackslashStaysBalanced_Pass3() {
        String input = "x" + (char) 92; // x followed by one backslash
        String result = Expression.normalize(input);
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
    public void testNormalize_DateLikeValuesAreQuoted_Pass3() {
        String dateResult = Expression.normalize(new java.util.Date(0L));
        Assertions.assertNotNull(dateResult);
        Assertions.assertTrue(dateResult.startsWith("'") && dateResult.endsWith("'"), "Date literal must be quoted, got: " + dateResult);

        String ldtResult = Expression.normalize(java.time.LocalDateTime.of(2024, 1, 1, 0, 0));
        Assertions.assertNotNull(ldtResult);
        Assertions.assertTrue(ldtResult.startsWith("'") && ldtResult.endsWith("'"), "LocalDateTime literal must be quoted, got: " + ldtResult);
        Assertions.assertTrue(ldtResult.contains("2024"));
    }

    /**
     * Regression: the short-literal fast path in {@code toString(NamingPolicy)} must use a full-string
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
        Expression expr = Expression.of("col\n");

        String result = expr.toString(NamingPolicy.NO_CHANGE);

        // Buggy (find()) path returned "col\n" verbatim; correct (matches()) path parses it
        // and collapses the whitespace run, so no line terminator may survive.
        Assertions.assertFalse(result.contains("\n"),
                "Short literal with trailing newline must be parsed (whitespace collapsed), got: " + result.replace("\n", "\\n"));
        Assertions.assertTrue(result.startsWith("col"), "Column token must be preserved, got: " + result);
    }

    /**
     * {@code toString(NamingPolicy)} must return the value rendered for the <i>requested</i> policy.
     * {@link Expression#of} interns instances, so the same object is reused across calls and must answer
     * each policy correctly even when callers alternate policies on it. (Originally guarded the
     * since-removed single-slot toString cache; kept because the contract must hold regardless of any
     * internal memoization.)
     */
    @Test
    public void testToStringReturnsValuePerNamingPolicy() {
        Expression expr = Expression.of("firstName");
        assertSame(expr, Expression.of("firstName"), "Expression.of must intern instances");

        for (int i = 0; i < 1000; i++) {
            assertEquals("firstName", expr.toString(NamingPolicy.NO_CHANGE));
            assertEquals("first_name", expr.toString(NamingPolicy.SNAKE_CASE));
            assertEquals("FIRST_NAME", expr.toString(NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("firstName", expr.toString(null)); // null defaults to NO_CHANGE
        }
    }

    /**
     * {@code toString(NamingPolicy)} must be thread-safe on a shared interned instance: every call must
     * return the value for its own policy. (Originally a regression test for a data race in the
     * since-removed single-slot toString cache; kept — with a lighter workload — to catch any future
     * reintroduction of unsafe per-instance memoization.)
     */
    @Test
    public void testToStringThreadSafeAcrossNamingPolicies() throws InterruptedException {
        final Expression expr = Expression.of("firstName");

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
                    final String got = expr.toString(policy);

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

        Assertions.assertNull(firstError.get(), "toString(NamingPolicy) returned a value rendered for the wrong policy under concurrency: " + firstError.get());
    }

    @Test
    public void testToStringNamingPolicyPreservesHyphenInShortLiteral() {
        // "price-tax" is SQL subtraction. The short-literal fast path used to hand the whole
        // literal to NamingPolicy.convert, which swallowed the '-' (CAMEL_CASE -> "priceTax");
        // hyphen-containing literals must take the parser path, converting each operand independently.
        assertEquals("price-tax", Expression.of("price-tax").toString(NamingPolicy.CAMEL_CASE));
        assertEquals("unit_price-tax", Expression.of("unitPrice-tax").toString(NamingPolicy.SNAKE_CASE));

        // Consistent with the >= 16-char parse path, which always preserved the '-'.
        assertEquals("basePrice-salesTax", Expression.of("base_price-sales_tax").toString(NamingPolicy.CAMEL_CASE));

        // Hyphen-free short literals still use the fast path unchanged.
        assertEquals("first_name", Expression.of("firstName").toString(NamingPolicy.SNAKE_CASE));

        // The digit-leading pass-through guard is still intact.
        assertEquals("2faCode", Expression.of("2faCode").toString(NamingPolicy.SNAKE_CASE));
    }
}
