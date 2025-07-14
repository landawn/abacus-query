package com.landawn.abacus.condition;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class AllTest extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All all = ConditionFactory.all(subQuery);

        Assertions.assertNotNull(all);
        Assertions.assertEquals(Operator.ALL, all.getOperator());
        Assertions.assertEquals(subQuery, all.getCondition());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        All all = ConditionFactory.all(subQuery);

        SubQuery retrieved = all.getCondition();
        Assertions.assertEquals(subQuery, retrieved);
    }

    @Test
    public void testToString() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT price FROM competitor_products");
        All all = ConditionFactory.all(subQuery);

        String result = all.toString();
        Assertions.assertTrue(result.contains("ALL"));
        Assertions.assertTrue(result.contains("SELECT price FROM competitor_products"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT minPrice FROM priceRanges");
        All all = ConditionFactory.all(subQuery);

        String result = all.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("ALL"));
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("products", Arrays.asList("price"),
                ConditionFactory.and(ConditionFactory.eq("category", "Premium"), ConditionFactory.eq("active", true)));
        All all = ConditionFactory.all(subQuery);

        var params = all.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertTrue(params.contains("Premium"));
        Assertions.assertTrue(params.contains(true));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = ConditionFactory.subQuery("grades", Arrays.asList("score"), ConditionFactory.between("year", 2020, 2023));
        All all = ConditionFactory.all(subQuery);

        Assertions.assertEquals(2, all.getParameters().size());

        all.clearParameters();

        Assertions.assertTrue(all.getParameters().size() == 2 && all.getParameters().stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT threshold FROM limits WHERE active = true");
        All original = ConditionFactory.all(subQuery);

        All copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT value FROM table1");
        SubQuery subQuery3 = ConditionFactory.subQuery("SELECT value FROM table2");

        All all1 = ConditionFactory.all(subQuery1);
        All all2 = ConditionFactory.all(subQuery2);
        All all3 = ConditionFactory.all(subQuery3);

        Assertions.assertEquals(all1, all1);
        Assertions.assertEquals(all1, all2);
        Assertions.assertNotEquals(all1, all3);
        Assertions.assertNotEquals(all1, null);
        Assertions.assertNotEquals(all1, "string");
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT score FROM exams");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT score FROM exams");

        All all1 = ConditionFactory.all(subQuery1);
        All all2 = ConditionFactory.all(subQuery2);

        Assertions.assertEquals(all1.hashCode(), all2.hashCode());
    }

    @Test
    public void testAnd() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT min_salary FROM job_grades");
        All all = ConditionFactory.all(subQuery);
        Equal eq = ConditionFactory.eq("department", "Engineering");

        And and = all.and(eq);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(all));
        Assertions.assertTrue(and.getConditions().contains(eq));
    }

    @Test
    public void testOr() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT required_score FROM certifications");
        All all = ConditionFactory.all(subQuery);
        GreaterThan gt = ConditionFactory.gt("experience_years", 10);

        Or or = all.or(gt);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT banned_id FROM blacklist");
        All all = ConditionFactory.all(subQuery);

        Not not = all.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(all, not.getCondition());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = ConditionFactory.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = ConditionFactory.subQuery("SELECT id FROM table2");
        All all = ConditionFactory.all(subQuery1);

        Assertions.assertEquals(subQuery1, all.getCondition());

        all.setCondition(subQuery2);
        Assertions.assertEquals(subQuery2, all.getCondition());
    }

    @Test
    public void testAllOperatorBehaviors() {
        // Test > ALL (greater than maximum)
        SubQuery maxQuery = ConditionFactory.subQuery("SELECT MAX(price) FROM products WHERE category = 'Budget'");
        All greaterThanAll = ConditionFactory.all(maxQuery);
        // This would be used like: our_price > ALL (subquery)
        Assertions.assertNotNull(greaterThanAll);

        // Test < ALL (less than minimum)
        SubQuery minQuery = ConditionFactory.subQuery("SELECT MIN(price) FROM products WHERE category = 'Premium'");
        All lessThanAll = ConditionFactory.all(minQuery);
        // This would be used like: our_price < ALL (subquery)
        Assertions.assertNotNull(lessThanAll);

        // Test = ALL (equal to all - only if all values are same)
        SubQuery sameQuery = ConditionFactory.subQuery("SELECT DISTINCT status FROM orders WHERE date = '2023-01-01'");
        All equalToAll = ConditionFactory.all(sameQuery);
        // This would be used like: status = ALL (subquery)
        Assertions.assertNotNull(equalToAll);

        // Test != ALL (different from all - equivalent to NOT IN)
        SubQuery excludeQuery = ConditionFactory.subQuery("SELECT id FROM excluded_items");
        All notEqualAll = ConditionFactory.all(excludeQuery);
        // This would be used like: item_id != ALL (subquery)
        Assertions.assertNotNull(notEqualAll);
    }

    @Test
    public void testComplexAllScenarios() {
        // Find products more expensive than all competitor products in a region
        SubQuery competitorPrices = ConditionFactory.subQuery("competitors", Arrays.asList("price"), ConditionFactory
                .and(ConditionFactory.eq("region", "Europe"), ConditionFactory.eq("product_type", "Premium"), ConditionFactory.eq("active", true)));
        All allCompetitorPrices = ConditionFactory.all(competitorPrices);

        var params = allCompetitorPrices.getParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertTrue(params.contains("Europe"));
        Assertions.assertTrue(params.contains("Premium"));
        Assertions.assertTrue(params.contains(true));

        // Find employees earning more than all managers in their department
        SubQuery managerSalaries = ConditionFactory.subQuery("employees", Arrays.asList("salary"),
                ConditionFactory.and(ConditionFactory.eq("is_manager", true), ConditionFactory.eq("department_id", Expression.of("e.department_id"))));
        All allManagerSalaries = ConditionFactory.all(managerSalaries);

        Assertions.assertNotNull(allManagerSalaries);
    }
}