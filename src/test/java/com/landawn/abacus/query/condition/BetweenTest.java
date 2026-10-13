package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class BetweenTest extends TestBase {

    @Test
    void testAnd() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testBetweenWithSubQueries() {
        SubQuery minSalary = Filters.subQuery("SELECT MIN(salary) FROM employees WHERE department = 'IT'");
        SubQuery maxSalary = Filters.subQuery("SELECT MAX(salary) FROM employees WHERE department = 'IT'");

        Between between = new Between("salary", minSalary, maxSalary);

        String result = between.toString();
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("SELECT MIN(salary)"));
        assertTrue(result.contains("SELECT MAX(salary)"));
    }

    // Removed: testBtAlias() - bt() methods have been removed. Use between() instead.

    @Test
    void testComplexBetweenScenarios() {
        // Date range
        Between dateRange = Filters.between("createdDate", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        // Price range with calculations
        SqlExpression minPrice = SqlExpression.of("base_price * 0.9");
        SqlExpression maxPrice = SqlExpression.of("base_price * 1.1");
        Between priceRange = new Between("offer_price", minPrice, maxPrice);

        // Complex condition with between
        And complexCondition = dateRange.and(priceRange).and(Filters.eq("status", "active")).and(Filters.between("quantity", 0, 5).not());

        assertNotNull(complexCondition);
        assertEquals(4, complexCondition.conditions().size());
    }

    @Test
    void testConstructor() {
        Between condition = new Between("age", 18, 65);
        assertEquals("age", condition.propName());
        assertEquals(Integer.valueOf(18), condition.minValue());
        assertEquals(Integer.valueOf(65), condition.maxValue());
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    void testConstructor_BlankPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between("   ", 10, 20));
    }

    @Test
    void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between("", 10, 20));
    }

    @Test
    void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between(null, 10, 20));
    }

    @Test
    void testConstructorRejectsNullBounds() {
        assertThrows(IllegalArgumentException.class, () -> new Between("field", null, 10));
        assertThrows(IllegalArgumentException.class, () -> new Between("field", 1, null));
    }

    @Test
    void testConstructorWithDifferentTypes() {
        // Test with Double
        Between betweenDouble = Filters.between("price", 10.0, 50.0);
        assertEquals(10.0, betweenDouble.minValue());
        assertEquals(50.0, betweenDouble.maxValue());

        // Test with Date
        Date startDate = new Date(1_700_000_000_000L);
        Date endDate = new Date(startDate.getTime() + 86_400_000L); // +1 day
        Between betweenDate = Filters.between("createdDate", startDate, endDate);
        assertEquals(startDate, betweenDate.minValue());
        assertEquals(endDate, betweenDate.maxValue());

        // Test with String (alphabetical range)
        Between betweenString = Filters.between("lastName", "A", "M");
        assertEquals("A", betweenString.minValue());
        assertEquals("M", betweenString.maxValue());
    }

    @Test
    void testConstructorWithEmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Between("", 1, 10);
        });
    }

    @Test
    void testConstructorWithNullPropName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Between(null, 1, 10);
        });
    }

    @Test
    void testDoubleValues() {
        Between condition = new Between("price", 9.99, 99.99);
        assertEquals(9.99, (Double) condition.minValue());
        assertEquals(99.99, (Double) condition.maxValue());
    }

    @Test
    void testEquals() {
        Between between1 = Filters.between("age", 18, 65);
        Between between2 = Filters.between("age", 18, 65);
        Between between3 = Filters.between("age", 21, 65);
        Between between4 = Filters.between("age", 18, 60);
        Between between5 = Filters.between("height", 18, 65);

        assertEquals(between1, between1);
        assertEquals(between1, between2);
        assertNotEquals(between1, between3); // Different min
        assertNotEquals(between1, between4); // Different max
        assertNotEquals(between1, between5); // Different property
        assertNotEquals(between1, null);
        assertNotEquals(between1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentMaxValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 70);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentMinValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_DifferentPropName() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 18, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        Between condition = new Between("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    void testFactoryRejectsNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Filters.between("value", null, 100));
        assertThrows(IllegalArgumentException.class, () -> Filters.between("value", 0, null));
    }

    @Test
    void testGetMaxValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double max = condition.maxValue();
        assertEquals(50.0, max);
    }

    @Test
    void testGetMinValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double min = condition.minValue();
        assertEquals(10.0, min);
    }

    @Test
    void testGetOperator() {
        Between condition = new Between("field", 1, 10);
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    void testGetPropName() {
        Between condition = new Between("salary", 50000, 100000);
        assertEquals("salary", condition.propName());
    }

    @Test
    void testHashCode() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testNot() {
        Between condition = new Between("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    void testNullBoundsFailBeforeRendering() {
        assertThrows(IllegalArgumentException.class, () -> Filters.between("value", null, 100));
    }

    @Test
    void testNumericValues() {
        Between condition = new Between("score", 0, 100);
        assertEquals(Integer.valueOf(0), condition.minValue());
        assertEquals(Integer.valueOf(100), condition.maxValue());
    }

    @Test
    void testOr() {
        Between cond1 = new Between("age", 18, 30);
        Between cond2 = new Between("age", 50, 65);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testParameters() {
        Between condition = new Between("age", 18, 65);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    void testParametersWithConditionValues() {
        SubQuery minQuery = Filters.subQuery("SELECT MIN(price) FROM products");
        SubQuery maxQuery = Filters.subQuery("SELECT MAX(price) FROM products");
        Between between = new Between("price", minQuery, maxQuery);

        List<Object> params = between.parameters();
        // Should get parameters from both subqueries
        assertNotNull(params);
    }

    @Test
    void testPredicateBoundsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Between("value", Filters.eq("x", 1), 100));
        assertThrows(IllegalArgumentException.class, () -> new Between("value", 0, Filters.exists(Filters.subQuery("SELECT 1"))));
    }

    @Test
    void testStringValues() {
        Between condition = new Between("name", "A", "M");
        assertEquals("A", condition.minValue());
        assertEquals("M", condition.maxValue());
    }

    @Test
    void testToString() {
        Between between = Filters.between("age", 18, 65);
        String result = between.toString();
        assertEquals("age BETWEEN 18 AND 65", result);
    }

    @Test
    void testToString_NoChange() {
        Between condition = new Between("age", 18, 65);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    void testToString_SnakeCase() {
        Between condition = new Between("userAge", 18, 65);
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_age"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Between between = Filters.between("orderDate", "2023-01-01", "2023-12-31");
        String result = between.toSql(NamingPolicy.SNAKE_CASE);
        assertEquals("order_date BETWEEN '2023-01-01' AND '2023-12-31'", result);
    }

    @Test
    void testToStringWithStrings() {
        Between between = Filters.between("grade", "A", "C");
        String result = between.toString();
        assertEquals("grade BETWEEN 'A' AND 'C'", result);
    }
}
