package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class NotBetween2025Test extends TestBase {

    @Test
    public void testConstructor_ValidRange() {
        NotBetween condition = new NotBetween("age", 18, 65);

        assertEquals("age", condition.getPropName());
        assertEquals(Integer.valueOf(18), condition.getMinValue());
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
        assertEquals(Operator.NOT_BETWEEN, condition.operator());
    }

    @Test
    public void testConstructor_NullPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween(null, 1, 10));
    }

    @Test
    public void testConstructor_EmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("", 1, 10));
    }

    @Test
    public void testConstructor_NumericRange() {
        NotBetween condition = new NotBetween("price", 10.0, 100.0);

        assertEquals("price", condition.getPropName());
        assertEquals(10.0, (Double) condition.getMinValue());
        assertEquals(100.0, (Double) condition.getMaxValue());
    }

    @Test
    public void testConstructor_StringRange() {
        NotBetween condition = new NotBetween("grade", "A", "C");

        assertEquals("grade", condition.getPropName());
        assertEquals("A", condition.getMinValue());
        assertEquals("C", condition.getMaxValue());
    }

    @Test
    public void testConstructor_DateRange() {
        NotBetween condition = new NotBetween("order_date", "2024-01-01", "2024-12-31");

        assertEquals("order_date", condition.getPropName());
        assertEquals("2024-01-01", condition.getMinValue());
        assertEquals("2024-12-31", condition.getMaxValue());
    }

    @Test
    public void testConstructor_NullValues() {
        NotBetween condition = new NotBetween("score", null, null);

        assertEquals("score", condition.getPropName());
        assertNull(condition.getMinValue());
        assertNull(condition.getMaxValue());
    }

    @Test
    public void testGetPropName() {
        NotBetween condition = new NotBetween("temperature", 36.0, 37.5);
        assertEquals("temperature", condition.getPropName());
    }

    @Test
    public void testGetMinValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(18), condition.getMinValue());
    }

    @Test
    public void testGetMaxValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
    }

    @Test
    public void testGetParameters_Simple() {
        NotBetween condition = new NotBetween("age", 18, 65);
        List<Object> params = condition.getParameters();

        assertEquals(2, params.size());
        assertEquals(Integer.valueOf(18), params.get(0));
        assertEquals(Integer.valueOf(65), params.get(1));
    }

    @Test
    public void testGetParameters_WithConditionValues() {
        Expression minExpr = new Expression("MIN(age)");
        Expression maxExpr = new Expression("MAX(age)");
        NotBetween condition = new NotBetween("score", minExpr, maxExpr);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_NullValues() {
        NotBetween condition = new NotBetween("score", null, null);
        List<Object> params = condition.getParameters();

        assertEquals(2, params.size());
        assertNull(params.get(0));
        assertNull(params.get(1));
    }

    @Test
    public void testToString_NoChange() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("NOT BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testToString_StringValues() {
        NotBetween condition = new NotBetween("grade", "A", "C");
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("grade"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotBetween condition = new NotBetween("maxAge", 18, 65);
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("max_age"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    public void testHashCode_Equal() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("score", 18, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentMinValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentMaxValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 70);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentType() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String other = "not a NotBetween";
        assertNotEquals(condition, other);
    }

    @Test
    public void testUseCaseScenario_ExtremeValues() {
        // Find products with extreme prices (very cheap or very expensive)
        NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
        String sql = priceRange.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("price"));
        assertTrue(sql.contains("NOT BETWEEN"));
        assertEquals(2, priceRange.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_OutsideBusinessHours() {
        // Find orders outside business hours (before 9 AM or after 5 PM)
        NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
        String sql = outsideHours.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("order_hour"));
        assertTrue(sql.contains("NOT BETWEEN"));
    }

    @Test
    public void testUseCaseScenario_AbnormalTemperature() {
        // Exclude normal temperature range
        NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
        List<Object> params = abnormalTemp.getParameters();

        assertEquals(2, params.size());
        assertEquals(36.0, (Double) params.get(0));
        assertEquals(37.5, (Double) params.get(1));
    }

    @Test
    public void testAnd() {
        NotBetween cond1 = new NotBetween("age", 18, 65);
        NotBetween cond2 = new NotBetween("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotBetween cond1 = new NotBetween("age", 18, 30);
        NotBetween cond2 = new NotBetween("age", 50, 65);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotBetween condition = new NotBetween("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}

public class NotBetweenTest extends TestBase {

    @Test
    public void testConstructor() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        Assertions.assertNotNull(notBetween);
        Assertions.assertEquals("age", notBetween.getPropName());
        Assertions.assertEquals(18, (Integer) notBetween.getMinValue());
        Assertions.assertEquals(65, (Integer) notBetween.getMaxValue());
        Assertions.assertEquals(Operator.NOT_BETWEEN, notBetween.operator());
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
    public void testToString() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        String result = notBetween.toString();
        Assertions.assertTrue(result.contains("age"));
        Assertions.assertTrue(result.contains("NOT BETWEEN"));
        Assertions.assertTrue(result.contains("18 AND 65"));
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
