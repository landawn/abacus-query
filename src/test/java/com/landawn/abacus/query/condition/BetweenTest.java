package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
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
class Between2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Between condition = new Between("age", 18, 65);
        assertEquals("age", condition.getPropName());
        assertEquals(Integer.valueOf(18), condition.getMinValue());
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between(null, 10, 20));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Between("", 10, 20));
    }

    @Test
    public void testGetPropName() {
        Between condition = new Between("salary", 50000, 100000);
        assertEquals("salary", condition.getPropName());
    }

    @Test
    public void testGetMinValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double min = condition.getMinValue();
        assertEquals(10.0, min);
    }

    @Test
    public void testGetMaxValue() {
        Between condition = new Between("price", 10.0, 50.0);
        Double max = condition.getMaxValue();
        assertEquals(50.0, max);
    }

    @Test
    public void testGetOperator() {
        Between condition = new Between("field", 1, 10);
        assertEquals(Operator.BETWEEN, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Between condition = new Between("age", 18, 65);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals(18, (int) params.get(0));
        assertEquals(65, (int) params.get(1));
    }

    @Test
    public void testGetParameters_WithNullValues() {
        Between condition = new Between("field", null, null);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
        assertNull(params.get(0));
        assertNull(params.get(1));
    }

    @Test
    public void testToString_NoChange() {
        Between condition = new Between("age", 18, 65);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("age"));
        assertTrue(result.contains("BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testToString_SnakeCase() {
        Between condition = new Between("userAge", 18, 65);
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_age"));
    }

    @Test
    public void testHashCode() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Between condition = new Between("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 65);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 18, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentMinValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 21, 65);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentMaxValue() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("age", 18, 70);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Between condition = new Between("age", 18, 65);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Between cond1 = new Between("age", 18, 65);
        Between cond2 = new Between("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Between cond1 = new Between("age", 18, 30);
        Between cond2 = new Between("age", 50, 65);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        Between condition = new Between("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testStringValues() {
        Between condition = new Between("name", "A", "M");
        assertEquals("A", condition.getMinValue());
        assertEquals("M", condition.getMaxValue());
    }

    @Test
    public void testNumericValues() {
        Between condition = new Between("score", 0, 100);
        assertEquals(Integer.valueOf(0), condition.getMinValue());
        assertEquals(Integer.valueOf(100), condition.getMaxValue());
    }

    @Test
    public void testToString_WithNullValues() {
        Between condition = new Between("value", null, null);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("value"));
        assertTrue(result.contains("BETWEEN"));
    }

    @Test
    public void testDoubleValues() {
        Between condition = new Between("price", 9.99, 99.99);
        assertEquals(9.99, (Double) condition.getMinValue());
        assertEquals(99.99, (Double) condition.getMaxValue());
    }
}

public class BetweenTest extends TestBase {

    @Test
    public void testConstructor() {
        Between between = Filters.between("age", 18, 65);

        Assertions.assertNotNull(between);
        Assertions.assertEquals("age", between.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) between.getMinValue());
        Assertions.assertEquals(65, (Integer) between.getMaxValue());
        Assertions.assertEquals(Operator.BETWEEN, between.operator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Double
        Between betweenDouble = Filters.between("price", 10.0, 50.0);
        Assertions.assertEquals(10.0, betweenDouble.getMinValue());
        Assertions.assertEquals(50.0, betweenDouble.getMaxValue());

        // Test with Date
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + 86400000); // +1 day
        Between betweenDate = Filters.between("createdDate", startDate, endDate);
        Assertions.assertEquals(startDate, betweenDate.getMinValue());
        Assertions.assertEquals(endDate, betweenDate.getMaxValue());

        // Test with String (alphabetical range)
        Between betweenString = Filters.between("lastName", "A", "M");
        Assertions.assertEquals("A", betweenString.getMinValue());
        Assertions.assertEquals("M", betweenString.getMaxValue());
    }

    @Test
    public void testConstructorWithNullValues() {
        Between between = Filters.between("value", null, null);
        Assertions.assertNull(between.getMinValue());
        Assertions.assertNull(between.getMaxValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Between("", 1, 10);
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Between(null, 1, 10);
        });
    }

    @Test
    public void testGetPropName() {
        Between between = Filters.between("temperature", -10, 40);
        Assertions.assertEquals("temperature", between.getPropName());
    }

    @Test
    public void testGetMinValue() {
        Between between = Filters.between("score", 0, 100);
        Integer min = between.getMinValue();
        Assertions.assertEquals(0, min);
    }

    @Test
    public void testGetMaxValue() {
        Between between = Filters.between("percentage", 0.0, 100.0);
        Double max = between.getMaxValue();
        Assertions.assertEquals(100.0, max);
    }

    @Test
    public void testGetParameters() {
        Between between = Filters.between("salary", 40000, 80000);
        List<Object> params = between.getParameters();

        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(40000, params.get(0));
        Assertions.assertEquals(80000, params.get(1));
    }

    @Test
    public void testGetParametersWithConditionValues() {
        SubQuery minQuery = Filters.subQuery("SELECT MIN(price) FROM products");
        SubQuery maxQuery = Filters.subQuery("SELECT MAX(price) FROM products");
        Between between = new Between("price", minQuery, maxQuery);

        List<Object> params = between.getParameters();
        // Should get parameters from both subqueries
        Assertions.assertNotNull(params);
    }

    @Test
    public void testToString() {
        Between between = Filters.between("age", 18, 65);
        String result = between.toString();
        Assertions.assertEquals("age BETWEEN 18 AND 65", result);
    }

    @Test
    public void testToStringWithStrings() {
        Between between = Filters.between("grade", "A", "C");
        String result = between.toString();
        Assertions.assertEquals("grade BETWEEN 'A' AND 'C'", result);
    }

    @Test
    public void testToStringWithNulls() {
        Between between = Filters.between("value", null, 100);
        String result = between.toString();
        Assertions.assertEquals("value BETWEEN null AND 100", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Between between = Filters.between("orderDate", "2023-01-01", "2023-12-31");
        String result = between.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertEquals("order_date BETWEEN '2023-01-01' AND '2023-12-31'", result);
    }

    @Test
    public void testEquals() {
        Between between1 = Filters.between("age", 18, 65);
        Between between2 = Filters.between("age", 18, 65);
        Between between3 = Filters.between("age", 21, 65);
        Between between4 = Filters.between("age", 18, 60);
        Between between5 = Filters.between("height", 18, 65);

        Assertions.assertEquals(between1, between1);
        Assertions.assertEquals(between1, between2);
        Assertions.assertNotEquals(between1, between3); // Different min
        Assertions.assertNotEquals(between1, between4); // Different max
        Assertions.assertNotEquals(between1, between5); // Different property
        Assertions.assertNotEquals(between1, null);
        Assertions.assertNotEquals(between1, "string");
    }

    @Test
    public void testHashCode() {
        Between between1 = Filters.between("price", 10.0, 50.0);
        Between between2 = Filters.between("price", 10.0, 50.0);

        Assertions.assertEquals(between1.hashCode(), between2.hashCode());
    }

    @Test
    public void testAnd() {
        Between between = Filters.between("age", 18, 65);
        Equal eq = Filters.eq("status", "active");

        And and = between.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(between));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        Between between = Filters.between("salary", 30000, 50000);
        GreaterThan gt = Filters.gt("experience", 10);

        Or or = between.or(gt);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Between between = Filters.between("score", 60, 100);

        Not not = between.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(between, not.getCondition());
    }

    @Test
    public void testBetweenWithSubQueries() {
        SubQuery minSalary = Filters.subQuery("SELECT MIN(salary) FROM employees WHERE department = 'IT'");
        SubQuery maxSalary = Filters.subQuery("SELECT MAX(salary) FROM employees WHERE department = 'IT'");

        Between between = new Between("salary", minSalary, maxSalary);

        String result = between.toString();
        Assertions.assertTrue(result.contains("BETWEEN"));
        Assertions.assertTrue(result.contains("SELECT MIN(salary)"));
        Assertions.assertTrue(result.contains("SELECT MAX(salary)"));
    }

    // Removed: testBtAlias() - bt() methods have been removed. Use between() instead.

    @Test
    public void testComplexBetweenScenarios() {
        // Date range
        Between dateRange = Filters.between("createdDate", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        // Price range with calculations
        Expression minPrice = Expression.of("base_price * 0.9");
        Expression maxPrice = Expression.of("base_price * 1.1");
        Between priceRange = new Between("offer_price", minPrice, maxPrice);

        // Complex condition with between
        And complexCondition = dateRange.and(priceRange).and(Filters.eq("status", "active")).and(Filters.between("quantity", 0, 5).not());

        Assertions.assertNotNull(complexCondition);
        Assertions.assertEquals(4, complexCondition.getConditions().size());
    }
}
