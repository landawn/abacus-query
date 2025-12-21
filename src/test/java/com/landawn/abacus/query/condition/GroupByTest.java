package com.landawn.abacus.query.condition;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.NamingPolicy;

public class GroupByTest extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Condition condition = Filters.expr("YEAR(order_date)");
        GroupBy groupBy = new GroupBy(condition);

        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(Operator.GROUP_BY, groupBy.getOperator());
        Assertions.assertEquals(condition, groupBy.getCondition());
    }

    @Test
    public void testConstructorWithVarArgs() {
        GroupBy groupBy = new GroupBy("department", "location");

        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(Operator.GROUP_BY, groupBy.getOperator());
        String result = groupBy.toString();
        Assertions.assertTrue(result.contains("department"));
        Assertions.assertTrue(result.contains("location"));
    }

    @Test
    public void testConstructorWithSinglePropName() {
        GroupBy groupBy = new GroupBy("category");

        Assertions.assertNotNull(groupBy);
        String result = groupBy.toString();
        Assertions.assertTrue(result.contains("GROUP BY"));
        Assertions.assertTrue(result.contains("category"));
    }

    @Test
    public void testConstructorWithPropNameAndDirection() {
        GroupBy groupBy = new GroupBy("sales_amount", SortDirection.DESC);

        Assertions.assertNotNull(groupBy);
        String result = groupBy.toString();
        Assertions.assertTrue(result.contains("sales_amount"));
        Assertions.assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithCollectionAndDirection() {
        List<String> columns = Arrays.asList("department", "location");
        GroupBy groupBy = new GroupBy(columns, SortDirection.DESC);

        Assertions.assertNotNull(groupBy);
        String result = groupBy.toString();
        Assertions.assertTrue(result.contains("department"));
        Assertions.assertTrue(result.contains("location"));
        Assertions.assertTrue(result.contains("DESC"));
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);
        orders.put("hire_date", SortDirection.ASC);

        GroupBy groupBy = new GroupBy(orders);

        Assertions.assertNotNull(groupBy);
        String result = groupBy.toString();
        Assertions.assertTrue(result.contains("department"));
        Assertions.assertTrue(result.contains("ASC"));
        Assertions.assertTrue(result.contains("salary"));
        Assertions.assertTrue(result.contains("DESC"));
        Assertions.assertTrue(result.contains("hire_date"));
    }

    @Test
    public void testGetParameters() {
        GroupBy groupBy = new GroupBy("category", "subcategory");
        List<Object> params = groupBy.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        GroupBy groupBy = new GroupBy("department");
        groupBy.clearParameters();
        // Should not throw exception
        List<Object> params = groupBy.getParameters();
        Assertions.assertNotNull(params);
    }

    @Test
    public void testCopy() {
        GroupBy original = new GroupBy("department", SortDirection.DESC);
        GroupBy copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertEquals(original.toString(), copy.toString());
    }

    @Test
    public void testToString() {
        GroupBy groupBy = new GroupBy("product_category");
        String result = groupBy.toString();

        Assertions.assertTrue(result.contains("GROUP BY"));
        Assertions.assertTrue(result.contains("product_category"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        GroupBy groupBy = new GroupBy("productCategory");
        String result = groupBy.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        Assertions.assertTrue(result.contains("GROUP BY"));
        Assertions.assertTrue(result.contains("PRODUCT_CATEGORY"));
    }

    @Test
    public void testHashCode() {
        GroupBy groupBy1 = new GroupBy("department");
        GroupBy groupBy2 = new GroupBy("department");
        GroupBy groupBy3 = new GroupBy("location");

        Assertions.assertEquals(groupBy1.hashCode(), groupBy2.hashCode());
        Assertions.assertNotEquals(groupBy1.hashCode(), groupBy3.hashCode());
    }

    @Test
    public void testEquals() {
        GroupBy groupBy1 = new GroupBy("department");
        GroupBy groupBy2 = new GroupBy("department");
        GroupBy groupBy3 = new GroupBy("location");

        Assertions.assertEquals(groupBy1, groupBy1);
        Assertions.assertEquals(groupBy1, groupBy2);
        Assertions.assertNotEquals(groupBy1, groupBy3);
        Assertions.assertNotEquals(groupBy1, null);
        Assertions.assertNotEquals(groupBy1, "string");
    }

    @Test
    public void testEmptyPropNames() {
        assertThrows(IllegalArgumentException.class, () -> new GroupBy(new String[0]));
    }

    @Test
    public void testMultiplePropNamesWithSortDirection() {
        GroupBy groupBy = new GroupBy(Arrays.asList("col1", "col2", "col3"), SortDirection.ASC);
        String result = groupBy.toString();

        Assertions.assertTrue(result.contains("col1"));
        Assertions.assertTrue(result.contains("col2"));
        Assertions.assertTrue(result.contains("col3"));
        Assertions.assertTrue(result.contains("ASC"));
    }
}