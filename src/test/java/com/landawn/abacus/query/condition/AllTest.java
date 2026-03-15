package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class All2025Test extends TestBase {

    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ALL, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("department", "Sales");
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        All condition = new All(subQuery);
        assertEquals(Operator.ALL, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE in_stock = true");
        All condition = new All(subQuery);
        SubQuery retrieved = condition.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testGetParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("category", "Premium");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertEquals(1, (int) params.size());
        assertEquals("Premium", params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("rating", (Object) 4);
        SubQuery subQuery = Filters.subQuery("reviews", Arrays.asList("score"), whereCondition);
        All condition = new All(subQuery);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("reviews"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertTrue(condition instanceof ComposableCell);
        assertTrue(condition instanceof ComposableCondition);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testGreaterThanAllScenario() {
        // price > ALL (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products WHERE product_type = 'Premium'");
        All condition = new All(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
        assertTrue(sql.contains("competitor_products"));
    }

    @Test
    public void testLessThanAllScenario() {
        // cost < ALL (SELECT budget FROM departments)
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments WHERE region = 'West'");
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testSalaryComparison() {
        // Find employees earning more than ALL managers
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        All condition = new All(subQuery);
        String sql = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
    }

    @Test
    public void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("active", true), new Equal("category", "Electronics")));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testScoreComparison() {
        // score >= ALL (SELECT avg_score FROM class_statistics)
        Condition whereCondition = new Equal("year", (Object) 2024);
        SubQuery subQuery = Filters.subQuery("class_statistics", Arrays.asList("avg_score"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }
}

public class AllTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All all = Filters.all(subQuery);

        Assertions.assertNotNull(all);
        Assertions.assertEquals(Operator.ALL, all.operator());
        Assertions.assertEquals(subQuery, all.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        All all = Filters.all(subQuery);

        SubQuery retrieved = all.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        All all = Filters.all(subQuery);

        String result = all.toString();
        Assertions.assertEquals("ALL (SELECT price FROM competitor_products)", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT minPrice FROM priceRanges");
        All all = Filters.all(subQuery);

        String result = all.toString(NamingPolicy.SNAKE_CASE);
        Assertions.assertTrue(result.contains("ALL"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.and(Filters.eq("category", "Premium"), Filters.eq("active", true)));
        All all = Filters.all(subQuery);

        var params = all.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("Premium"));
        Assertions.assertTrue(params.contains(true));
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT value FROM table2");

        All all1 = Filters.all(subQuery1);
        All all2 = Filters.all(subQuery2);
        All all3 = Filters.all(subQuery3);

        Assertions.assertEquals(all1, all1);
        Assertions.assertEquals(all1, all2);
        Assertions.assertNotEquals(all1, all3);
        Assertions.assertNotEquals(all1, null);
        Assertions.assertNotEquals(all1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT score FROM exams");
        SubQuery subQuery2 = Filters.subQuery("SELECT score FROM exams");

        All all1 = Filters.all(subQuery1);
        All all2 = Filters.all(subQuery2);

        Assertions.assertEquals(all1.hashCode(), all2.hashCode());
    }

    @Test
    public void testAllOperatorBehaviors() {
        // Test > ALL (greater than maximum)
        SubQuery maxQuery = Filters.subQuery("SELECT MAX(price) FROM products WHERE category = 'Budget'");
        All greaterThanAll = Filters.all(maxQuery);
        // This would be used like: our_price > ALL (subquery)
        Assertions.assertNotNull(greaterThanAll);

        // Test < ALL (less than minimum)
        SubQuery minQuery = Filters.subQuery("SELECT MIN(price) FROM products WHERE category = 'Premium'");
        All lessThanAll = Filters.all(minQuery);
        // This would be used like: our_price < ALL (subquery)
        Assertions.assertNotNull(lessThanAll);

        // Test = ALL (equal to all - only if all values are same)
        SubQuery sameQuery = Filters.subQuery("SELECT DISTINCT status FROM orders WHERE date = '2023-01-01'");
        All equalToAll = Filters.all(sameQuery);
        // This would be used like: status = ALL (subquery)
        Assertions.assertNotNull(equalToAll);

        // Test != ALL (different from all - equivalent to NOT IN)
        SubQuery excludeQuery = Filters.subQuery("SELECT id FROM excluded_items");
        All notEqualAll = Filters.all(excludeQuery);
        // This would be used like: item_id != ALL (subquery)
        Assertions.assertNotNull(notEqualAll);
    }

    @Test
    public void testComplexAllScenarios() {
        // Find products more expensive than all competitor products in a region
        SubQuery competitorPrices = Filters.subQuery("competitors", Arrays.asList("price"),
                Filters.and(Filters.eq("region", "Europe"), Filters.eq("product_type", "Premium"), Filters.eq("active", true)));
        All allCompetitorPrices = Filters.all(competitorPrices);

        var params = allCompetitorPrices.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("Europe"));
        Assertions.assertTrue(params.contains("Premium"));
        Assertions.assertTrue(params.contains(true));

        // Find employees earning more than all managers in their department
        SubQuery managerSalaries = Filters.subQuery("employees", Arrays.asList("salary"),
                Filters.and(Filters.eq("is_manager", true), Filters.eq("department_id", Expression.of("e.department_id"))));
        All allManagerSalaries = Filters.all(managerSalaries);

        Assertions.assertNotNull(allManagerSalaries);
    }
}
