package com.landawn.abacus.query.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AllTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All all = Filters.all(subQuery);

        Assertions.assertNotNull(all);
        Assertions.assertEquals(Operator.ALL, all.getOperator());
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
        Assertions.assertTrue(result.contains("ALL"));
        Assertions.assertTrue(result.contains("SELECT price FROM competitor_products"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT minPrice FROM priceRanges");
        All all = Filters.all(subQuery);

        String result = all.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("grades", Arrays.asList("score"), Filters.between("year", 2020, 2023));
        All all = Filters.all(subQuery);

        Assertions.assertEquals(2, all.getParameters().size());

        all.clearParameters();

        Assertions.assertTrue(all.getParameters().size() == 2 && all.getParameters().stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT threshold FROM limits WHERE active = true");
        All original = Filters.all(subQuery);

        All copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
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
    public void testAnd() {
        SubQuery subQuery = Filters.subQuery("SELECT min_salary FROM job_grades");
        All all = Filters.all(subQuery);
        Equal eq = Filters.eq("department", "Engineering");

        And and = all.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(all));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        SubQuery subQuery = Filters.subQuery("SELECT required_score FROM certifications");
        All all = Filters.all(subQuery);
        GreaterThan gt = Filters.gt("experience_years", 10);

        Or or = all.or(gt);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = Filters.subQuery("SELECT banned_id FROM blacklist");
        All all = Filters.all(subQuery);

        Not not = all.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(all, not.getCondition());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        All all = Filters.all(subQuery1);

        Assertions.assertEquals(subQuery1, all.getCondition());

        all.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, all.getCondition());
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