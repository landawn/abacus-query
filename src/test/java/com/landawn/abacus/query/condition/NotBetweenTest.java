package com.landawn.abacus.query.condition;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotBetweenTest extends TestBase {

    @Test
    public void testConstructor() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        Assertions.assertNotNull(notBetween);
        Assertions.assertEquals("age", notBetween.getPropName());
        Assertions.assertEquals(18, (Integer) notBetween.getMinValue());
        Assertions.assertEquals(65, (Integer) notBetween.getMaxValue());
        Assertions.assertEquals(Operator.NOT_BETWEEN, notBetween.getOperator());
    }

    @Test
    public void testConstructorWithDates() {
        NotBetween notBetween = Filters.notBetween("orderDate", "2023-01-01", "2023-12-31");

        Assertions.assertEquals("orderDate", notBetween.getPropName());
        Assertions.assertEquals("2023-01-01", notBetween.getMinValue());
        Assertions.assertEquals("2023-12-31", notBetween.getMaxValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notBetween("", 1, 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Filters.notBetween(null, 1, 10);
        });
    }

    @Test
    public void testSetMinValue() {
        NotBetween notBetween = Filters.notBetween("price", 100, 1000);
        notBetween.setMinValue(200);

        Assertions.assertEquals(200, (Integer) notBetween.getMinValue());
    }

    @Test
    public void testSetMaxValue() {
        NotBetween notBetween = Filters.notBetween("price", 100, 1000);
        notBetween.setMaxValue(2000);

        Assertions.assertEquals(2000, (Integer) notBetween.getMaxValue());
    }

    @Test
    public void testGetParameters() {
        NotBetween notBetween = Filters.notBetween("salary", 30000, 80000);

        List<Object> params = notBetween.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(30000, params.get(0));
        Assertions.assertEquals(80000, params.get(1));
    }

    @Test
    public void testGetParametersWithConditionValues() {
        Expression minExpr = Filters.expr("(SELECT MIN(salary) FROM employees)");
        Expression maxExpr = Filters.expr("(SELECT AVG(salary) FROM employees)");
        NotBetween notBetween = Filters.notBetween("salary", minExpr, maxExpr);

        List<Object> params = notBetween.getParameters();
        Assertions.assertEquals(minExpr.getParameters().size() + maxExpr.getParameters().size(), params.size());
    }

    @Test
    public void testClearParameters() {
        NotBetween notBetween = Filters.notBetween("age", 20, 40);

        notBetween.clearParameters();

        Assertions.assertNull(notBetween.getMinValue());
        Assertions.assertNull(notBetween.getMaxValue());
    }

    @Test
    public void testClearParametersWithConditionValues() {
        SubQuery minSubQuery = Filters.subQuery("SELECT MIN(price) FROM products");
        SubQuery maxSubQuery = Filters.subQuery("SELECT MAX(price) FROM products");
        NotBetween notBetween = Filters.notBetween("price", minSubQuery, maxSubQuery);

        notBetween.clearParameters();

        // Verify subquery parameters are cleared
        Assertions.assertTrue(minSubQuery.getParameters().isEmpty());
        Assertions.assertTrue(maxSubQuery.getParameters().isEmpty());
    }

    @Test
    public void testCopy() {
        NotBetween original = Filters.notBetween("temperature", -10, 40);

        NotBetween copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Integer) original.getMinValue(), copy.getMinValue());
        Assertions.assertEquals((Integer) original.getMaxValue(), copy.getMaxValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testCopyWithConditionValues() {
        Expression minExpr = Filters.expr("MIN_VALUE");
        Expression maxExpr = Filters.expr("MAX_VALUE");
        NotBetween original = Filters.notBetween("value", minExpr, maxExpr);

        NotBetween copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertNotSame(original.getMinValue(), copy.getMinValue());
        Assertions.assertNotSame(original.getMaxValue(), copy.getMaxValue());
        Assertions.assertEquals((Object) original.getMinValue(), copy.getMinValue());
        Assertions.assertEquals((Object) original.getMaxValue(), copy.getMaxValue());
    }

    @Test
    public void testToString() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        String result = notBetween.toString();
        Assertions.assertTrue(result.contains("age"));
        Assertions.assertTrue(result.contains("NOT BETWEEN"));
        Assertions.assertTrue(result.contains("(18, 65)"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        NotBetween notBetween = Filters.notBetween("user_age", 18, 65);

        String result = notBetween.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        Assertions.assertTrue(result.contains("USER_AGE"));
        Assertions.assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    public void testHashCode() {
        NotBetween notBetween1 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween2 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween3 = Filters.notBetween("age", 18, 70);
        NotBetween notBetween4 = Filters.notBetween("height", 18, 65);

        Assertions.assertEquals(notBetween1.hashCode(), notBetween2.hashCode());
        Assertions.assertNotEquals(notBetween1.hashCode(), notBetween3.hashCode());
        Assertions.assertNotEquals(notBetween1.hashCode(), notBetween4.hashCode());
    }

    @Test
    public void testEquals() {
        NotBetween notBetween1 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween2 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween3 = Filters.notBetween("age", 18, 70);
        NotBetween notBetween4 = Filters.notBetween("age", 20, 65);
        NotBetween notBetween5 = Filters.notBetween("height", 18, 65);

        Assertions.assertTrue(notBetween1.equals(notBetween1));
        Assertions.assertTrue(notBetween1.equals(notBetween2));
        Assertions.assertFalse(notBetween1.equals(notBetween3));
        Assertions.assertFalse(notBetween1.equals(notBetween4));
        Assertions.assertFalse(notBetween1.equals(notBetween5));
        Assertions.assertFalse(notBetween1.equals(null));
        Assertions.assertFalse(notBetween1.equals("not a NotBetween"));
    }

    @Test
    public void testPracticalExamples() {
        // Exclude normal working hours
        NotBetween notWorkHours = Filters.notBetween("hour", 9, 17);
        Assertions.assertEquals(9, (Integer) notWorkHours.getMinValue());
        Assertions.assertEquals(17, (Integer) notWorkHours.getMaxValue());

        // Exclude mid-range prices
        NotBetween extremePrices = Filters.notBetween("price", 100.0, 1000.0);
        Assertions.assertEquals(100.0, extremePrices.getMinValue());
        Assertions.assertEquals(1000.0, extremePrices.getMaxValue());
    }

    @Test
    public void testWithNullValues() {
        NotBetween notBetween = Filters.notBetween("value", null, null);

        Assertions.assertNull(notBetween.getMinValue());
        Assertions.assertNull(notBetween.getMaxValue());

        List<Object> params = notBetween.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertNull(params.get(0));
        Assertions.assertNull(params.get(1));
    }
}