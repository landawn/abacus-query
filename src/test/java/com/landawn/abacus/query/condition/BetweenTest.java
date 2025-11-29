package com.landawn.abacus.query.condition;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class BetweenTest extends TestBase {

    @Test
    public void testConstructor() {
        Between between = Filters.between("age", 18, 65);

        Assertions.assertNotNull(between);
        Assertions.assertEquals("age", between.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) between.getMinValue());
        Assertions.assertEquals(65, (Integer) between.getMaxValue());
        Assertions.assertEquals(Operator.BETWEEN, between.getOperator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Double
        Between betweenDouble = Filters.between("price", 10.0, 50.0);
        Assertions.assertEquals(10.0, betweenDouble.getMinValue());
        Assertions.assertEquals(50.0, betweenDouble.getMaxValue());

        // Test with Date
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + 86400000);   // +1 day
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
    public void testSetMinValue() {
        Between between = Filters.between("range", 10, 20);
        Assertions.assertEquals(10, (Integer) between.getMinValue());

        between.setMinValue(5);
        Assertions.assertEquals(5, (Integer) between.getMinValue());
    }

    @Test
    public void testSetMaxValue() {
        Between between = Filters.between("range", 10, 20);
        Assertions.assertEquals(20, (Integer) between.getMaxValue());

        between.setMaxValue(30);
        Assertions.assertEquals(30, (Integer) between.getMaxValue());
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
    public void testClearParameters() {
        Between between = Filters.between("quantity", 10, 100);
        Assertions.assertEquals(10, (Integer) between.getMinValue());
        Assertions.assertEquals(100, (Integer) between.getMaxValue());

        between.clearParameters();

        Assertions.assertNull(between.getMinValue());
        Assertions.assertNull(between.getMaxValue());
    }

    @Test
    public void testClearParametersWithConditionValues() {
        In minIn = Filters.in("id", Arrays.asList(1, 2, 3));
        In maxIn = Filters.in("id", Arrays.asList(7, 8, 9));
        Between between = new Between("value", minIn, maxIn);

        between.clearParameters();

        // The subqueries' parameters should be cleared
        Assertions.assertTrue(between.getParameters().stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Between original = Filters.between("level", 1, 10);
        Between copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Integer) original.getMinValue(), copy.getMinValue());
        Assertions.assertEquals((Integer) original.getMaxValue(), copy.getMaxValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testCopyWithConditionValues() {
        SubQuery minQuery = Filters.subQuery("SELECT MIN(value) FROM table");
        SubQuery maxQuery = Filters.subQuery("SELECT MAX(value) FROM table");
        Between original = new Between("value", minQuery, maxQuery);

        Between copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertNotSame(original.getMinValue(), copy.getMinValue());
        Assertions.assertNotSame(original.getMaxValue(), copy.getMaxValue());
        Assertions.assertEquals((Object) original.getMinValue(), copy.getMinValue());
        Assertions.assertEquals((Object) original.getMaxValue(), copy.getMaxValue());
    }

    @Test
    public void testToString() {
        Between between = Filters.between("age", 18, 65);
        String result = between.toString();
        Assertions.assertEquals("age BETWEEN (18, 65)", result);
    }

    @Test
    public void testToStringWithStrings() {
        Between between = Filters.between("grade", "A", "C");
        String result = between.toString();
        Assertions.assertEquals("grade BETWEEN ('A', 'C')", result);
    }

    @Test
    public void testToStringWithNulls() {
        Between between = Filters.between("value", null, 100);
        String result = between.toString();
        Assertions.assertEquals("value BETWEEN (null, 100)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Between between = Filters.between("orderDate", "2023-01-01", "2023-12-31");
        String result = between.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("order_date BETWEEN ('2023-01-01', '2023-12-31')", result);
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
        Assertions.assertNotEquals(between1, between3);   // Different min
        Assertions.assertNotEquals(between1, between4);   // Different max
        Assertions.assertNotEquals(between1, between5);   // Different property
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

    @Test
    public void testBtAlias() {
        Between bt = Filters.bt("value", 1, 10);
        Assertions.assertEquals("value", bt.getPropName());
        Assertions.assertEquals(1, (Integer) bt.getMinValue());
        Assertions.assertEquals(10, (Integer) bt.getMaxValue());
    }

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