package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class AllTest extends TestBase {

    @Test
    void testAllOperatorBehaviors() {
        // Test > ALL (greater than maximum)
        SubQuery maxQuery = Filters.subQuery("SELECT MAX(price) FROM products WHERE category = 'Budget'");
        All greaterThanAll = Filters.all(maxQuery);
        // This would be used like: our_price > ALL (subquery)
        assertNotNull(greaterThanAll);

        // Test < ALL (less than minimum)
        SubQuery minQuery = Filters.subQuery("SELECT MIN(price) FROM products WHERE category = 'Premium'");
        All lessThanAll = Filters.all(minQuery);
        // This would be used like: our_price < ALL (subquery)
        assertNotNull(lessThanAll);

        // Test = ALL (equal to all - only if all values are same)
        SubQuery sameQuery = Filters.subQuery("SELECT DISTINCT status FROM orders WHERE date = '2023-01-01'");
        All equalToAll = Filters.all(sameQuery);
        // This would be used like: status = ALL (subquery)
        assertNotNull(equalToAll);

        // Test != ALL (different from all - equivalent to NOT IN)
        SubQuery excludeQuery = Filters.subQuery("SELECT id FROM excluded_items");
        All notEqualAll = Filters.all(excludeQuery);
        // This would be used like: item_id != ALL (subquery)
        assertNotNull(notEqualAll);
    }

    @Test
    void testComplexAllScenarios() {
        // Find products more expensive than all competitor products in a region
        SubQuery competitorPrices = Filters.subQuery("competitors", Arrays.asList("price"),
                Filters.and(Filters.eq("region", "Europe"), Filters.eq("product_type", "Premium"), Filters.eq("active", true)));
        All allCompetitorPrices = Filters.all(competitorPrices);

        var params = allCompetitorPrices.parameters();
        assertEquals(3, params.size());
        assertTrue(params.contains("Europe"));
        assertTrue(params.contains("Premium"));
        assertTrue(params.contains(true));

        // Find employees earning more than all managers in their department
        SubQuery managerSalaries = Filters.subQuery("employees", Arrays.asList("salary"),
                Filters.and(Filters.eq("is_manager", true), Filters.eq("department_id", SqlExpression.of("e.department_id"))));
        All allManagerSalaries = Filters.all(managerSalaries);

        assertNotNull(allManagerSalaries);
    }

    @Test
    void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All all = Filters.all(subQuery);

        assertNotNull(all);
        assertEquals(Operator.ALL, all.operator());
        assertEquals(subQuery, all.condition());
    }

    @Test
    void testConstructor_RejectsKnownMultiColumnProjection() {
        final SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary", "bonus"), (Condition) null);

        assertThrows(IllegalArgumentException.class, () -> new All(subQuery));
    }

    @Test
    void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.ALL, condition.operator());
    }

    @Test
    void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("department", "Sales");
        SubQuery subQuery = Filters.subQuery("employees", Arrays.asList("salary"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testEquals() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery3 = Filters.subQuery("SELECT value FROM table2");

        All all1 = Filters.all(subQuery1);
        All all2 = Filters.all(subQuery2);
        All all3 = Filters.all(subQuery3);

        assertEquals(all1, all1);
        assertEquals(all1, all2);
        assertNotEquals(all1, all3);
        assertNotEquals(all1, null);
        assertNotEquals(all1, "string");
    }

    @Test
    void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT price FROM products");
        SubQuery subQuery2 = Filters.subQuery("SELECT cost FROM items");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT salary FROM employees WHERE active = true");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE in_stock = true");
        All condition = new All(subQuery);
        SubQuery retrieved = (SubQuery) condition.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        All condition = new All(subQuery);
        assertEquals(Operator.ALL, condition.operator());
    }

    @Test
    void testGreaterThanAllScenario() {
        // price > ALL (SELECT price FROM competitor_products)
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products WHERE product_type = 'Premium'");
        All condition = new All(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
        assertTrue(sql.contains("competitor_products"));
    }

    @Test
    void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table1");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT value FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT value FROM table2");
        All cond1 = new All(subQuery1);
        All cond2 = new All(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    void testInheritance() {
        SubQuery subQuery = Filters.subQuery("SELECT value FROM table1");
        All condition = new All(subQuery);
        assertTrue(condition instanceof ComposableCell);
        assertTrue(condition instanceof ComposableCondition);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    void testLessThanAllScenario() {
        // cost < ALL (SELECT budget FROM departments)
        SubQuery subQuery = Filters.subQuery("SELECT budget FROM departments WHERE region = 'West'");
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testNullSubQueryMessageNamesSubQuery() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new All(null));
        assertTrue(e.getMessage().contains("subQuery"), e.getMessage());
    }

    @Test
    void testParameters() {
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), Filters.and(Filters.eq("category", "Premium"), Filters.eq("active", true)));
        All all = Filters.all(subQuery);

        var params = all.parameters();
        assertEquals(2, params.size());
        assertTrue(params.contains("Premium"));
        assertTrue(params.contains(true));
    }

    @Test
    void testParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        All condition = new All(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("category", "Premium");
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), whereCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("Premium", params.get(0));
    }

    @Test
    void testQuantifiedOperandsAcceptUnknownProjectionArity() {
        for (final SubQuery unknownArity : List.of(new SubQuery("SELECT id, tenant_id FROM accounts"), new SubQuery("accounts", "*", null))) {
            assertSame(unknownArity, new All(unknownArity).subQuery());
            assertSame(unknownArity, new Any(unknownArity).subQuery());
            assertSame(unknownArity, new Some(unknownArity).subQuery());
        }

        final SubQuery knownTwoColumns = new SubQuery("accounts", List.of("id", "tenantId"), null);
        assertThrows(IllegalArgumentException.class, () -> new All(knownTwoColumns));
        assertThrows(IllegalArgumentException.class, () -> new Any(knownTwoColumns));
        assertThrows(IllegalArgumentException.class, () -> new Some(knownTwoColumns));
    }

    @Test
    void testRejectsStandalonePredicateUse() {
        All condition = new All(Filters.subQuery("SELECT score FROM tests"));

        assertThrows(IllegalArgumentException.class, () -> new Where(condition));
        assertThrows(IllegalArgumentException.class, () -> condition.and(Filters.eq("active", true)));
    }

    @Test
    void testSalaryComparison() {
        // Find employees earning more than ALL managers
        SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        All condition = new All(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("ALL"));
    }

    @Test
    void testScoreComparison() {
        // score >= ALL (SELECT avg_score FROM class_statistics)
        Condition whereCondition = new Equal("year", 2024);
        SubQuery subQuery = Filters.subQuery("class_statistics", Arrays.asList("avg_score"), whereCondition);
        All condition = new All(subQuery);
        assertNotNull(condition);
    }

    @Test
    void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM competitor_products");
        All all = Filters.all(subQuery);

        String result = all.toString();
        assertEquals("ALL (SELECT price FROM competitor_products)", result);
    }

    @Test
    void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
        All condition = new All(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    void testToString_WithStructuredQuery() {
        Condition whereCondition = new GreaterThan("rating", 4);
        SubQuery subQuery = Filters.subQuery("reviews", Arrays.asList("score"), whereCondition);
        All condition = new All(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("reviews"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        SubQuery subQuery = Filters.subQuery("SELECT minPrice FROM priceRanges");
        All all = Filters.all(subQuery);

        String result = all.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("ALL"));
    }

    @Test
    void testWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("active", true), new Equal("category", "Electronics")));
        SubQuery subQuery = Filters.subQuery("products", Arrays.asList("price"), andCondition);
        All condition = new All(subQuery);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }
}
