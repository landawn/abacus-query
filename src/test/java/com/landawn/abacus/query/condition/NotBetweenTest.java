package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class NotBetweenTest extends TestBase {

    @Test
    void testAnd() {
        NotBetween cond1 = new NotBetween("age", 18, 65);
        NotBetween cond2 = new NotBetween("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testConstructor() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        assertNotNull(notBetween);
        assertEquals("age", notBetween.propName());
        assertEquals(18, (Integer) notBetween.minValue());
        assertEquals(65, (Integer) notBetween.maxValue());
        assertEquals(Operator.NOT_BETWEEN, notBetween.operator());
    }

    @Test
    void testConstructor_BlankPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("   ", 1, 10));
    }

    @Test
    void testConstructor_DateRange() {
        NotBetween condition = new NotBetween("order_date", "2024-01-01", "2024-12-31");

        assertEquals("order_date", condition.propName());
        assertEquals("2024-01-01", condition.minValue());
        assertEquals("2024-12-31", condition.maxValue());
    }

    @Test
    void testConstructor_EmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("", 1, 10));
    }

    @Test
    void testConstructor_NullPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween(null, 1, 10));
    }

    @Test
    void testConstructor_NumericRange() {
        NotBetween condition = new NotBetween("price", 10.0, 100.0);

        assertEquals("price", condition.propName());
        assertEquals(10.0, (Double) condition.minValue());
        assertEquals(100.0, (Double) condition.maxValue());
    }

    @Test
    void testConstructor_RejectsQuantifiedBounds() {
        final SubQuery subQuery = Filters.subQuery("SELECT score FROM results");

        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", new All(subQuery), 100));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", 0, new Any(subQuery)));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", new Some(subQuery), 100));
    }

    @Test
    void testConstructor_RejectsStructuralConditionBounds() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", new Where(Filters.eq("active", true)), 100));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", 0, new On("a.id", "b.id")));
    }

    @Test
    void testConstructor_StringRange() {
        NotBetween condition = new NotBetween("grade", "A", "C");

        assertEquals("grade", condition.propName());
        assertEquals("A", condition.minValue());
        assertEquals("C", condition.maxValue());
    }

    @Test
    void testConstructor_ValidRange() {
        NotBetween condition = new NotBetween("age", 18, 65);

        assertEquals("age", condition.propName());
        assertEquals(Integer.valueOf(18), condition.minValue());
        assertEquals(Integer.valueOf(65), condition.maxValue());
        assertEquals(Operator.NOT_BETWEEN, condition.operator());
    }

    @Test
    void testConstructorRejectsNullValues() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", null, 100));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", 0, null));
    }

    @Test
    void testConstructorWithDates() {
        NotBetween notBetween = Filters.notBetween("orderDate", "2023-01-01", "2023-12-31");

        assertEquals("orderDate", notBetween.propName());
        assertEquals("2023-01-01", notBetween.minValue());
        assertEquals("2023-12-31", notBetween.maxValue());
    }

    @Test
    void testConstructorWithEmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notBetween("", 1, 10);
        });
    }

    @Test
    void testConstructorWithNullPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            Filters.notBetween(null, 1, 10);
        });
    }

    @Test
    void testEquals() {
        NotBetween notBetween1 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween2 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween3 = Filters.notBetween("age", 18, 70);
        NotBetween notBetween4 = Filters.notBetween("age", 20, 65);
        NotBetween notBetween5 = Filters.notBetween("height", 18, 65);

        assertTrue(notBetween1.equals(notBetween1));
        assertTrue(notBetween1.equals(notBetween2));
        assertFalse(notBetween1.equals(notBetween3));
        assertFalse(notBetween1.equals(notBetween4));
        assertFalse(notBetween1.equals(notBetween5));
        assertFalse(notBetween1.equals(null));
        assertFalse(notBetween1.equals("not a NotBetween"));
    }

    @Test
    void testEquals_DifferentMaxValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 70);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentMinValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentPropName() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("score", 18, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_DifferentType() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String other = "not a NotBetween";
        assertNotEquals(condition, other);
    }

    @Test
    void testEquals_EqualObjects() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1, c2);
    }

    @Test
    void testEquals_Null() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    void testFactoryRejectsNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Filters.notBetween("value", null, 100));
        assertThrows(IllegalArgumentException.class, () -> Filters.notBetween("value", 0, null));
    }

    @Test
    void testGetMaxValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(65), condition.maxValue());
    }

    @Test
    void testGetMinValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(18), condition.minValue());
    }

    @Test
    void testGetPropName() {
        NotBetween condition = new NotBetween("temperature", 36.0, 37.5);
        assertEquals("temperature", condition.propName());
    }

    @Test
    void testHashCode() {
        NotBetween notBetween1 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween2 = Filters.notBetween("age", 18, 65);
        NotBetween notBetween3 = Filters.notBetween("age", 18, 70);
        NotBetween notBetween4 = Filters.notBetween("height", 18, 65);

        assertEquals(notBetween1.hashCode(), notBetween2.hashCode());
        assertNotEquals(notBetween1.hashCode(), notBetween3.hashCode());
        assertNotEquals(notBetween1.hashCode(), notBetween4.hashCode());
    }

    @Test
    void testHashCode_Different() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCode_Equal() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testNot() {
        NotBetween condition = new NotBetween("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testOr() {
        NotBetween cond1 = new NotBetween("age", 18, 30);
        NotBetween cond2 = new NotBetween("age", 50, 65);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        NotBetween notBetween = Filters.notBetween("salary", 30000, 80000);

        List<Object> params = notBetween.parameters();
        assertEquals(2, params.size());
        assertEquals(30000, params.get(0));
        assertEquals(80000, params.get(1));
    }

    @Test
    void testParameters_Simple() {
        NotBetween condition = new NotBetween("age", 18, 65);
        List<Object> params = condition.parameters();

        assertEquals(2, params.size());
        assertEquals(Integer.valueOf(18), params.get(0));
        assertEquals(Integer.valueOf(65), params.get(1));
    }

    @Test
    void testParameters_WithConditionValues() {
        SqlExpression minExpr = new SqlExpression("MIN(age)");
        SqlExpression maxExpr = new SqlExpression("MAX(age)");
        NotBetween condition = new NotBetween("score", minExpr, maxExpr);

        List<Object> params = condition.parameters();
        assertNotNull(params);
    }

    @Test
    void testParametersWithConditionValues() {
        SqlExpression minExpr = Filters.expr("(SELECT MIN(salary) FROM employees)");
        SqlExpression maxExpr = Filters.expr("(SELECT AVG(salary) FROM employees)");
        NotBetween notBetween = Filters.notBetween("salary", minExpr, maxExpr);

        List<Object> params = notBetween.parameters();
        assertEquals(minExpr.parameters().size() + maxExpr.parameters().size(), params.size());
    }

    @Test
    void testPracticalExamples() {
        // Exclude normal working hours
        NotBetween notWorkHours = Filters.notBetween("hour", 9, 17);
        assertEquals(9, (Integer) notWorkHours.minValue());
        assertEquals(17, (Integer) notWorkHours.maxValue());

        // Exclude mid-range prices
        NotBetween extremePrices = Filters.notBetween("price", 100.0, 1000.0);
        assertEquals(100.0, extremePrices.minValue());
        assertEquals(1000.0, extremePrices.maxValue());
    }

    @Test
    void testPredicateBoundsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", Filters.eq("x", 1), 100));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("score", 0, Filters.exists(Filters.subQuery("SELECT 1"))));
    }

    @Test
    void testToString() {
        NotBetween notBetween = Filters.notBetween("age", 18, 65);

        String result = notBetween.toString();
        assertTrue(result.contains("age"));
        assertTrue(result.contains("NOT BETWEEN"));
        assertTrue(result.contains("18 AND 65"));
    }

    @Test
    void testToString_NoChange() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("NOT BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    void testToString_SnakeCase() {
        NotBetween condition = new NotBetween("maxAge", 18, 65);
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("max_age"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    void testToString_StringValues() {
        NotBetween condition = new NotBetween("grade", "A", "C");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("grade"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        NotBetween notBetween = Filters.notBetween("user_age", 18, 65);

        String result = notBetween.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("USER_AGE"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    void testUseCaseScenario_AbnormalTemperature() {
        // Exclude normal temperature range
        NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
        List<Object> params = abnormalTemp.parameters();

        assertEquals(2, params.size());
        assertEquals(36.0, (Double) params.get(0));
        assertEquals(37.5, (Double) params.get(1));
    }

    @Test
    void testUseCaseScenario_ExtremeValues() {
        // Find products with extreme prices (very cheap or very expensive)
        NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
        String sql = priceRange.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("price"));
        assertTrue(sql.contains("NOT BETWEEN"));
        assertEquals(2, priceRange.parameters().size());
    }

    @Test
    void testUseCaseScenario_OutsideBusinessHours() {
        // Find orders outside business hours (before 9 AM or after 5 PM)
        NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
        String sql = outsideHours.toSql(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("order_hour"));
        assertTrue(sql.contains("NOT BETWEEN"));
    }
}
